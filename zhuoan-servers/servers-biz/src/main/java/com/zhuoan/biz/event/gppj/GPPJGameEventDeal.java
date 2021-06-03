package com.zhuoan.biz.event.gppj;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.gppj.GPPJCore;
import com.zhuoan.biz.core.mjpj.MjPjCore;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.gppj.GPPJGameRoom;
import com.zhuoan.biz.model.gppj.UserPacketGPPJ;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.util.Dto;
import com.zhuoan.util.thread.ThreadPoolHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Resource;
import javax.jms.Destination;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GPPJGameEventDeal {
    private static final Logger logger = LoggerFactory.getLogger(GPPJGameEventDeal.class);
    @Resource
    private RoomBiz roomBiz;
    @Resource
    private UserBiz userBiz;
    @Resource
    private Destination daoQueueDestination;
    @Resource
    private ProducerService producerService;
    @Resource
    private RedisInfoService redisInfoService;
    @Resource
    private GameTimerGPPJ gameTimerGPPJ;

    public GPPJGameEventDeal() {
    }

    public void createRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String account = postData.getString("account");
        String roomNo = postData.getString("room_no");
        JSONObject roomData = this.obtainRoomData(roomNo, account);
        if (!Dto.isObjNull(roomData)) {
            JSONObject result = new JSONObject();
            result.put("code", 1);
            result.put("data", roomData);
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "enterRoomPush_GPPJ");
        }

    }

    public void joinRoom(SocketIOClient client, Object data) {
        this.createRoom(client, data);
        JSONObject joinData = JSONObject.fromObject(data);
        if (joinData.containsKey("isReconnect") && joinData.getInt("isReconnect") == 0) {
            String account = joinData.getString("account");
            GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(joinData.getString("room_no"));
            Playerinfo player = (Playerinfo)room.getPlayerMap().get(account);
            JSONObject obj = new JSONObject();
            obj.put("account", player.getAccount());
            obj.put("name", player.getName());
            obj.put("headimg", player.getRealHeadimg());
            obj.put("sex", player.getSex());
            obj.put("ip", player.getIp());
            obj.put("vip", player.getVip());
            obj.put("location", player.getLocation());
            obj.put("area", player.getArea());
            obj.put("score", player.getScore());
            obj.put("index", player.getMyIndex());
            obj.put("userOnlineStatus", player.getStatus());
            obj.put("ghName", player.getGhName());
            obj.put("introduction", player.getSignature());
            obj.put("userStatus", ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus());
            CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), String.valueOf(obj), "playerEnterPush_GPPJ");
        }

    }

    public void gameReady(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (CommonConstant.checkEvent(postData, 0, client) || CommonConstant.checkEvent(postData, 1, client) || CommonConstant.checkEvent(postData, 6, client)) {
            String roomNo = postData.getString("room_no");
            GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
            String account = postData.getString("account");
            if (((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() != 1) {
                ((UserPacketGPPJ)room.getUserPacketMap().get(account)).setStatus(1);
                if (room.getGameStatus() != 1) {
                    room.setGameStatus(1);
                }

                if (this.isAllReady(roomNo) && room.getPlayerMap().size() >= 2) {
                    this.initRoom(roomNo);
                    room.setGameStatus(2);
                    if (room.getGid() == 17 && room.getUsedArray().size() > 0) {
                        this.startGame(roomNo);
                    } else {
                        this.startCutTimer(roomNo, 0);
                    }

                    this.changeGameStatus(roomNo);
                } else {
                    JSONObject result = new JSONObject();
                    result.put("index", ((Playerinfo)room.getPlayerMap().get(account)).getMyIndex());
                    result.put("startBtnIndex", this.obtainBankerIndex(roomNo));
                    result.put("showStartBtn", this.obtainStartBtnStatus(roomNo));
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "playerReadyPush_GPPJ");
                }

            }
        }
    }

    public void gameStart(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (CommonConstant.checkEvent(postData, 1, client)) {
            String roomNo = postData.getString("room_no");
            GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
            String account = postData.getString("account");
            if (((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() != 1) {
                if (account.equals(room.getBanker())) {
                    if (postData.containsKey("type")) {
                        int type = postData.getInt("type");
                        if (type == 1 && this.obtainNowReadyCount(roomNo) < room.getPlayerCount() - 1) {
                            JSONObject result = new JSONObject();
                            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "gameStartPush_GPPJ");
                        } else {
                            ((UserPacketGPPJ)room.getUserPacketMap().get(account)).setStatus(1);
                            this.initRoom(roomNo);
                            room.setGameStatus(2);
                            if (room.getGid() == 17 && room.getUsedArray().size() > 0) {
                                this.startGame(roomNo);
                            } else {
                                this.startCutTimer(roomNo, 0);
                            }

                            this.changeGameStatus(roomNo);
                        }
                    }
                }
            }
        }
    }

    public void gameCut(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (CommonConstant.checkEvent(postData, 2, client)) {
            String roomNo = postData.getString("room_no");
            String account = postData.getString("account");
            GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() == 1) {
                if (postData.containsKey("cutPlace")) {
                    room.setCutPlace(postData.getInt("cutPlace"));
                    room.setCutIndex(((Playerinfo)room.getPlayerMap().get(account)).getMyIndex());
                    this.startGame(roomNo);
                    Iterator var7 = room.getUserPacketMap().keySet().iterator();

                    while(var7.hasNext()) {
                        String uuid = (String)var7.next();
                        if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
                            JSONObject result = new JSONObject();
                            result.put("dice", room.getDice());
                            result.put("paiIndex", this.obtainPaiIndex(roomNo));
                            result.put("paiArray", this.obtainPlayerIndex(roomNo));
                            int[] myPai = new int[]{0, 0};
                            if (room.getBankerType() == 1 && ((UserPacketGPPJ)room.getUserPacketMap().get(uuid)).getStatus() != 0) {
                                if (room.getGid() == 5) {
                                    myPai[0] = GPPJCore.getPaiValue(((UserPacketGPPJ)room.getUserPacketMap().get(uuid)).getPai()[1]);
                                } else if (room.getGid() == 17) {
                                    myPai[0] = MjPjCore.getCardValue(((UserPacketGPPJ)room.getUserPacketMap().get(uuid)).getPai()[1]);
                                }
                            }

                            result.put("myPai", myPai);
                            CommonConstant.sendMsgEventToSingle(((Playerinfo)room.getPlayerMap().get(uuid)).getUuid(), String.valueOf(result), "gameCutPush_GPPJ");
                        }
                    }
                }

            }
        }
    }

    public void gameQz(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (CommonConstant.checkEvent(postData, 3, client) || CommonConstant.checkEvent(postData, 6, client)) {
            String roomNo = postData.getString("room_no");
            GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
            String account = postData.getString("account");
            if (postData.containsKey("qzTimes")) {
                if (room.getBankerType() != 1 && room.getBankerType() != 4) {
                    return;
                }

                if (((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() != 1 && ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() != 2 && (room.getBankerType() == 1 || room.getGameIndex() == 1)) {
                    return;
                }

                if (room.getBankerType() == 4 && room.getGameIndex() != 1 && room.getGameIndex() % (room.getGameCount() / room.getPlayerCount()) != 0) {
                    return;
                }

                int maxTimes = this.obtainMaxTimes(this.obtainQzBtn(roomNo, account));
                if (postData.getInt("qzTimes") < 0 || postData.getInt("qzTimes") > maxTimes) {
                    return;
                }

                ((UserPacketGPPJ)room.getUserPacketMap().get(account)).setStatus(3);
                ((UserPacketGPPJ)room.getUserPacketMap().get(account)).setBankerTimes(postData.getInt("qzTimes"));
                if (this.isAllQz(roomNo)) {
                    this.chooseBanker(roomNo);
                    if (room.getBankerType() != 1 && room.getGameIndex() != 1) {
                        room.setGameStatus(8);
                        this.startReshuffleTimer(roomNo, 0);
                    } else {
                        room.setGameStatus(4);
                        this.startBetTimer(roomNo, 0);
                    }

                    this.changeGameStatus(roomNo);
                } else {
                    JSONObject result = new JSONObject();
                    result.put("index", ((Playerinfo)room.getPlayerMap().get(account)).getMyIndex());
                    result.put("qzTimes", ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getBankerTimes());
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "gameQzPush_GPPJ");
                }
            }

        }
    }

    public void gameXz(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (CommonConstant.checkEvent(postData, 4, client)) {
            String roomNo = postData.getString("room_no");
            String account = postData.getString("account");
            GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (postData.containsKey("xzTimes")) {
                if (account.equals(room.getBanker())) {
                    return;
                }

                if ((room.getBankerType() == 0 || room.getBankerType() == 3) && ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() != 1 && ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() != 2) {
                    return;
                }

                if (room.getBankerType() == 1 && ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() != 3) {
                    return;
                }

                if (room.getBankerType() == 4 && ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() != 3 && ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() != 1 && ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() != 2) {
                    return;
                }

                if (room.getBankerType() == 2) {
                    return;
                }

                int maxTimes = this.obtainMaxTimes(this.obtainXzBtn(roomNo, account));
                if (postData.getInt("xzTimes") <= 0 || postData.getInt("xzTimes") > maxTimes) {
                    return;
                }

                ((UserPacketGPPJ)room.getUserPacketMap().get(account)).setStatus(4);
                ((UserPacketGPPJ)room.getUserPacketMap().get(account)).setXzTimes(postData.getInt("xzTimes"));
                JSONObject result = new JSONObject();
                result.put("code", 1);
                result.put("index", ((Playerinfo)room.getPlayerMap().get(account)).getMyIndex());
                result.put("xzTimes", ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getXzTimes());
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "gameXzPush_GPPJ");
                if (this.isAllXz(roomNo)) {
                    room.setGameStatus(5);
                    if ((room.getBankerType() == 0 || room.getBankerType() == 1 || room.getBankerType() == 3 || room.getBankerType() == 4) && !Dto.stringIsNULL(room.getBanker()) && room.getUserPacketMap().get(room.getBanker()) != null) {
                        ((UserPacketGPPJ)room.getUserPacketMap().get(room.getBanker())).setStatus(4);
                    }

                    this.startShowTimer(roomNo, 0);
                    this.changeGameStatus(roomNo);
                }
            }

        }
    }

    public void gameShow(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (CommonConstant.checkEvent(postData, 5, client)) {
            String roomNo = postData.getString("room_no");
            GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
            String account = postData.getString("account");
            if (room.getBankerType() != 2 || ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() == 1 || ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() == 2) {
                if (room.getBankerType() != 0 && room.getBankerType() != 1 && room.getBankerType() != 3 && room.getBankerType() != 4 || ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() == 4) {
                    ((UserPacketGPPJ)room.getUserPacketMap().get(account)).setStatus(5);
                    if (this.isAllShow(roomNo)) {
                        this.showFinish(roomNo);
                    } else {
                        JSONObject result = new JSONObject();
                        result.put("index", ((Playerinfo)room.getPlayerMap().get(account)).getMyIndex());
                        if (room.getGid() == 5) {
                            result.put("pai", GPPJCore.getPaiValue(((UserPacketGPPJ)room.getUserPacketMap().get(account)).getPai()));
                        } else if (room.getGid() == 17) {
                            result.put("pai", MjPjCore.getCardValue(((UserPacketGPPJ)room.getUserPacketMap().get(account)).getPai()));
                        }

                        result.put("paiType", ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getPaiType());
                        CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "gameShowPush_GPPJ");
                    }

                }
            }
        }
    }

    public void reShuffle(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (CommonConstant.checkEvent(postData, 8, client)) {
            String roomNo = postData.getString("room_no");
            GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
            String account = postData.getString("account");
            int type = postData.getInt("type");
            if (account.equals(room.getBanker())) {
                Iterator var8 = room.getUserPacketMap().keySet().iterator();

                while(var8.hasNext()) {
                    String player = (String)var8.next();
                    ((UserPacketGPPJ)room.getUserPacketMap().get(player)).setStatus(0);
                }

                room.setGameStatus(0);
                if (type == 1) {
                    room.setLeftArray(new String[0]);
                    room.setUsedArray(new JSONArray());
                }

                JSONObject result = new JSONObject();
                result.put("type", type);
                result.put("startBtnIndex", this.obtainBankerIndex(roomNo));
                result.put("showStartBtn", this.obtainStartBtnStatus(roomNo));
                result.put("banker", this.obtainBankerIndex(roomNo));
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "reShufflePush_GPPJ");
            }
        }
    }

    public void reconnectGame(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (postData.containsKey("room_no") && postData.containsKey("account") && postData.containsKey("uuid")) {
            String roomNo = postData.getString("room_no");
            String account = postData.getString("account");
            JSONObject userInfo = this.userBiz.getUserByAccount(account);
            if (userInfo.containsKey("uuid") && !Dto.stringIsNULL(userInfo.getString("uuid")) && userInfo.getString("uuid").equals(postData.getString("uuid"))) {
                JSONObject result = new JSONObject();
                if (client != null) {
                    if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
                        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
                        if (!Dto.stringIsNULL(account) && room.getPlayerMap().containsKey(account) && room.getPlayerMap().get(account) != null) {
                            ((Playerinfo)room.getPlayerMap().get(account)).setUuid(client.getSessionId());
                            result.put("type", 1);
                            result.put("data", this.obtainRoomData(roomNo, account));
                            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush_GPPJ");
                        } else {
                            result.put("type", 0);
                            CommonConstant.sendMsgEventToSingle(client, result.toString(), "reconnectGamePush_GPPJ");
                        }
                    } else {
                        result.put("type", 0);
                        CommonConstant.sendMsgEventToSingle(client, result.toString(), "reconnectGamePush_GPPJ");
                    }
                }
            }
        }
    }

    public void exitRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (CommonConstant.checkEvent(postData, -1, client)) {
            String roomNo = postData.getString("room_no");
            GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
            String account = postData.getString("account");
            boolean canExit = false;
            if (room.getRoomType() != 1 && room.getRoomType() != 3) {
                if (room.getRoomType() == 0 || room.getRoomType() == 6 || room.getRoomType() == 7) {
                    if (room.getGameStatus() == 7) {
                        canExit = true;
                    }

                    if (!room.getOwner().equals(account) && !room.getBanker().equals(account) && ((Playerinfo)room.getPlayerMap().get(account)).getPlayTimes() == 0) {
                        canExit = true;
                    }
                }
            } else if (((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() == 0) {
                canExit = true;
            } else if (room.getGameStatus() == 0 || room.getGameStatus() == 1 || room.getGameStatus() == 6) {
                canExit = true;
            }

            Playerinfo player = (Playerinfo)room.getPlayerMap().get(account);
            if (canExit) {
                List<UUID> allUUIDList = room.getAllUUIDList();

                for(int i = 0; i < room.getUserIdList().size(); ++i) {
                    if ((Long)room.getUserIdList().get(i) == ((Playerinfo)room.getPlayerMap().get(account)).getId()) {
                        room.getUserIdList().set(i, 0L);
                        room.addIndexList(((Playerinfo)room.getPlayerMap().get(account)).getMyIndex());
                        break;
                    }
                }

                JSONObject roomInfo = new JSONObject();
                roomInfo.put("roomNo", room.getRoomNo());
                roomInfo.put("roomId", room.getId());
                roomInfo.put("userIndex", ((Playerinfo)room.getPlayerMap().get(account)).getMyIndex());
                room.getPlayerMap().remove(account);
                room.getVisitPlayerMap().remove(account);
                room.getUserPacketMap().remove(account);
                roomInfo.put("player_number", room.getPlayerMap().size());
                this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("update_quit_user_room", roomInfo));
                JSONObject result = new JSONObject();
                result.put("code", 1);
                result.put("type", 1);
                result.put("index", player.getMyIndex());
                result.put("startBtnIndex", this.obtainBankerIndex(roomNo));
                result.put("showStartBtn", this.obtainStartBtnStatus(roomNo));
                if (!postData.containsKey("notSend")) {
                    CommonConstant.sendMsgEventToAll(allUUIDList, result.toString(), "exitRoomPush_GPPJ");
                }

                if (postData.containsKey("notSendToMe")) {
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "exitRoomPush_GPPJ");
                }

                if (this.isAllReady(roomNo) && room.getPlayerMap().size() >= 2) {
                    this.startGame(roomNo);
                }

                if (room.getPlayerMap().size() == 0) {
                    this.redisInfoService.delSummary(roomNo, "_GP_PJ");
                    roomInfo.put("status", room.getIsClose());
                    roomInfo.put("game_index", room.getGameIndex());
                    if (8 != room.getRoomType() && 9 != room.getRoomType()) {
                        RoomManage.gameRoomMap.remove(room.getRoomNo());
                    }
                }
            } else {
                JSONObject result = new JSONObject();
                result.put("code", 0);
                result.put("msg", "当前无法退出,请发起解散");
                result.put("type", 1);
                CommonConstant.sendMsgEventToSingle(client, result.toString(), "exitRoomPush_GPPJ");
            }

        }
    }

    public void closeRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (CommonConstant.checkEvent(postData, -1, client)) {
            final String roomNo = postData.getString("room_no");
            GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
            String account = postData.getString("account");
            if (postData.containsKey("type")) {
                JSONObject result = new JSONObject();
                int type = postData.getInt("type");
                if (type == 1 && room.getJieSanTime() == 0) {
                    final int closeTime = !Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("close_time") ? room.getSetting().getInt("close_time") : 60;
                    room.setJieSanTime(closeTime);
                    ThreadPoolHelper.executorService.submit(new Runnable() {
                        public void run() {
                            GPPJGameEventDeal.this.gameTimerGPPJ.closeRoomOverTime(roomNo, closeTime);
                        }
                    });
                }

                ((Playerinfo)room.getPlayerMap().get(account)).setIsCloseRoom(type);
                if (type == -1) {
                    room.setJieSanTime(0);
                    Iterator var12 = room.getUserPacketMap().keySet().iterator();

                    while(var12.hasNext()) {
                        String uuid = (String)var12.next();
                        if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
                            ((Playerinfo)room.getPlayerMap().get(uuid)).setIsCloseRoom(0);
                        }
                    }

                    result.put("code", 0);
                    String[] names = new String[]{((Playerinfo)room.getPlayerMap().get(account)).getName()};
                    result.put("names", names);
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush_GPPJ");
                    return;
                }

                if (type == 1) {
                    if (this.isAllAgreeClose(roomNo)) {
                        if (!room.isNeedFinalSummary()) {
                            List<UUID> uuidList = room.getAllUUIDList();
                            RoomManage.gameRoomMap.remove(roomNo);
                            result.put("type", 2);
                            result.put("msg", "解散成功");
                            CommonConstant.sendMsgEventToAll(uuidList, result.toString(), "tipMsgPush");
                            return;
                        }

                        if (room.getRoomType() == 6) {
                            room.setOpen(false);
                        }

                        room.setGameStatus(7);
                        this.changeGameStatus(roomNo);
                    } else {
                        ((Playerinfo)room.getPlayerMap().get(account)).setIsCloseRoom(1);
                        result.put("code", 1);
                        result.put("data", this.obtainCloseData(roomNo));
                        CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush_GPPJ");
                    }
                }
            }

        }
    }

    public void showFinish(String roomNo) {
        long summaryTimes = this.redisInfoService.summaryTimes(roomNo, "_GP_PJ");
        if (summaryTimes <= 1L) {
            GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
            room.setGameStatus(6);
            if (room.getGid() == 5) {
                switch(room.getBankerType()) {
                    case 0:
                        this.summaryBanker(roomNo);
                        break;
                    case 1:
                        this.summaryBanker(roomNo);
                        break;
                    case 2:
                        this.summaryCompare(roomNo);
                        break;
                    case 3:
                        this.summaryBanker(roomNo);
                        break;
                    case 4:
                        this.summaryBanker(roomNo);
                }
            } else if (room.getGid() == 17) {
                this.summaryBankerMj(roomNo);
            }

            this.changeGameStatus(roomNo);
            if (room.getRoomType() == 0 || room.getRoomType() == 6 || room.getRoomType() == 7) {
                room.setNeedFinalSummary(true);
                if (room.getGameStatus() == 6 && room.getGameIndex() == room.getGameCount()) {
                    room.setIsClose(-2);
                    if (room.getRoomType() == 6) {
                        room.setOpen(false);
                    }

                    room.setGameStatus(7);
                } else if (room.getGameIndex() > 1 && room.getGameIndex() % (room.getGameCount() / room.getPlayerCount()) == 0) {
                    if (room.getBankerType() == 3) {
                        room.setGameStatus(8);
                        String currentBanker = !Dto.stringIsNULL(room.getBanker()) ? room.getBanker() : room.getOwner();
                        room.setBanker(this.obtainNextBanker(roomNo, ((Playerinfo)room.getPlayerMap().get(currentBanker)).getMyIndex()));
                        this.startReshuffleTimer(roomNo, 0);
                    }

                    if (room.getBankerType() == 4) {
                        this.startRobTimer(roomNo, 0);
                    }
                }

                this.updateRoomCard(roomNo);
                this.saveGameLog(roomNo);
            }

        }
    }

    public void updateRoomCard(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        int roomCardCount = 0;
        if (room.getRoomType() == 0 || room.getRoomType() == 6 || room.getRoomType() == 7) {
            Iterator var5 = room.getUserPacketMap().keySet().iterator();

            while(var5.hasNext()) {
                String account = (String)var5.next();
                if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null && ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() > 0) {
                    if ("0".equals(room.getPayType()) && account.equals(room.getOwner()) && ((Playerinfo)room.getPlayerMap().get(account)).getPlayTimes() == 1) {
                        roomCardCount = room.getPlayerCount() * room.getSinglePayNum();
                        array.add(((Playerinfo)room.getPlayerMap().get(room.getOwner())).getId());
                    }

                    if ("1".equals(room.getPayType()) && ((Playerinfo)room.getPlayerMap().get(account)).getPlayTimes() == 1) {
                        array.add(((Playerinfo)room.getPlayerMap().get(account)).getId());
                        roomCardCount = room.getSinglePayNum();
                    }
                }
            }
        }

        if (array.size() > 0) {
            this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("pump", room.getRoomCardChangeObject(array, roomCardCount)));
        }

    }

    public void saveGameLog(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (null != room && (-1 != room.getIsClose() || 6 == room.getGameStatus() || 7 == room.getGameStatus())) {
            JSONArray gameLogResults = new JSONArray();
            JSONArray gameResult = new JSONArray();
            JSONArray array = new JSONArray();
            Iterator var6 = room.getUserPacketMap().keySet().iterator();

            while(true) {
                String account;
                do {
                    do {
                        do {
                            if (!var6.hasNext()) {
                                JSONObject gameLogObj = room.obtainGameLog(gameLogResults.toString(), String.valueOf(room.getGameProcess()));
                                this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("insert_game_log", gameLogObj));
                                JSONObject o = new JSONObject();
                                JSONArray userGameLogs = room.obtainUserGameLog(gameLogObj.getLong("id"), array);
                                o.element("array", userGameLogs);
                                o.element("object", (new JSONObject()).element("gamelog_id", gameLogObj.getLong("id")).element("room_type", room.getRoomType()).element("room_id", room.getId()).element("room_no", room.getRoomNo()).element("game_index", room.getGameNewIndex()).element("result", gameResult.toString()));
                                this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("insert_user_game_log", o));
                                return;
                            }

                            account = (String)var6.next();
                        } while(!room.getUserPacketMap().containsKey(account));
                    } while(room.getUserPacketMap().get(account) == null);
                } while(((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() != 5);

                JSONObject obj = new JSONObject();
                obj.put("id", ((Playerinfo)room.getPlayerMap().get(account)).getId());
                obj.put("total", ((Playerinfo)room.getPlayerMap().get(account)).getScore());
                obj.put("fen", ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getScore());
                array.add(obj);
                JSONObject gameLogResult = new JSONObject();
                gameLogResult.put("account", account);
                gameLogResult.put("name", ((Playerinfo)room.getPlayerMap().get(account)).getName());
                gameLogResult.put("headimg", ((Playerinfo)room.getPlayerMap().get(account)).getHeadimg());
                if (!Dto.stringIsNULL(room.getBanker()) && room.getPlayerMap().containsKey(room.getBanker()) && room.getPlayerMap().get(room.getBanker()) != null) {
                    gameLogResult.put("zhuang", ((Playerinfo)room.getPlayerMap().get(room.getBanker())).getMyIndex());
                } else {
                    gameLogResult.put("zhuang", -1);
                }

                gameLogResult.put("myIndex", ((Playerinfo)room.getPlayerMap().get(account)).getMyIndex());
                gameLogResult.put("myPai", ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getPai());
                gameLogResult.put("score", ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getScore());
                gameLogResult.put("totalScore", ((Playerinfo)room.getPlayerMap().get(account)).getScore());
                gameLogResult.put("win", 1);
                if (((UserPacketGPPJ)room.getUserPacketMap().get(account)).getScore() < 0.0D) {
                    gameLogResult.put("win", 0);
                }

                gameLogResults.add(gameLogResult);
                JSONObject userResult = new JSONObject();
                userResult.put("account", account);
                userResult.put("zhuang", room.getBanker());
                userResult.put("isWinner", 0);
                if (((UserPacketGPPJ)room.getUserPacketMap().get(account)).getScore() > 0.0D) {
                    userResult.put("isWinner", 1);
                }

                userResult.put("score", ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getScore());
                userResult.put("totalScore", ((Playerinfo)((GameRoom)RoomManage.gameRoomMap.get(room.getRoomNo())).getPlayerMap().get(account)).getScore());
                userResult.put("player", ((Playerinfo)room.getPlayerMap().get(account)).getName());
                gameResult.add(userResult);
            }
        }
    }

    public void changeGameStatus(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        Iterator var3 = room.getPlayerMap().keySet().iterator();

        while(true) {
            String account;
            do {
                do {
                    if (!var3.hasNext()) {
                        return;
                    }

                    account = (String)var3.next();
                } while(!room.getUserPacketMap().containsKey(account));
            } while(room.getUserPacketMap().get(account) == null);

            JSONObject obj = new JSONObject();
            obj.put("gameStatus", room.getGameStatus());
            obj.put("banker", this.obtainBankerIndex(roomNo));
            obj.put("game_index", room.getGameIndex());
            if (room.getGameIndex() == 0) {
                obj.put("game_index", 1);
            }

            obj.put("dice", room.getDice());
            obj.put("cutIndex", room.getCutIndex());
            obj.put("cutPlace", room.getCutPlace());
            obj.put("leftPai", this.obtainLeftPai(roomNo));
            obj.put("paiIndex", this.obtainPaiIndex(roomNo));
            obj.put("paiArray", this.obtainPlayerIndex(roomNo));
            obj.put("bankerTimes", this.obtainBankerTimes(roomNo));
            obj.put("btnType", this.obtainBtnType(roomNo, account));
            obj.put("qzTimes", this.obtainQzBtn(roomNo, account));
            obj.put("xzTimes", this.obtainXzBtn(roomNo, account));
            obj.put("users", this.obtainAllPlayer(roomNo));
            obj.put("gameData", this.obtainGameData(roomNo, account));
            obj.put("showStartBtn", this.obtainStartBtnStatus(roomNo));
            obj.put("startBtnIndex", this.obtainBankerIndex(roomNo));
            obj.put("usedArray", room.getUsedArray());
            obj.put("needDeal", 0);
            if (room.getUsedArray().size() > 0) {
                if (room.getBankerType() == 1 && room.getGameStatus() == 3) {
                    obj.put("needDeal", 1);
                }

                if ((room.getBankerType() == 0 || room.getBankerType() == 3) && room.getGameStatus() == 4) {
                    obj.put("needDeal", 1);
                }

                if (room.getBankerType() == 4 && (room.getGameStatus() == 4 || room.getGameStatus() == 3)) {
                    obj.put("needDeal", 1);
                }
            }

            if (room.getRoomType() == 0 || room.getRoomType() == 6 || room.getRoomType() == 7) {
                if (room.getGameStatus() == 7) {
                    obj.put("summaryData", this.obtainFinalSummaryData(roomNo));
                }

                if (room.getGameStatus() == 6 && room.getGameIndex() == room.getGameCount()) {
                    obj.put("summaryData", this.obtainFinalSummaryData(roomNo));
                }
            }

            if (room.getBankerType() == 4) {
                if (room.getGameIndex() == 1 && room.getGameStatus() == 4) {
                    obj.put("qzList", this.obtainQzList(roomNo));
                }

                if (room.getGameStatus() == 8) {
                    obj.put("qzList", this.obtainQzList(roomNo));
                }
            }

            UUID uuid = ((Playerinfo)room.getPlayerMap().get(account)).getUuid();
            if (uuid != null) {
                CommonConstant.sendMsgEventToSingle(uuid, String.valueOf(obj), "changeGameStatusPush_GPPJ");
            }
        }
    }

    public void initRoom(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        room.addGameIndex();
        room.addGameNewIndex();
        room.setIsClose(0);
        this.roomBiz.increaseRoomIndexByRoomNo(room.getId());
        double minScore = 0.0D;
        String minAccount = "";
        Iterator var6 = room.getUserPacketMap().keySet().iterator();

        String account;
        while(var6.hasNext()) {
            account = (String)var6.next();
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                if (((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() != 1) {
                    ((UserPacketGPPJ)room.getUserPacketMap().get(account)).setStatus(0);
                } else if (((UserPacketGPPJ)room.getUserPacketMap().get(account)).getScore() < minScore) {
                    minScore = ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getScore();
                    minAccount = account;
                }
            }
        }

        if (Dto.stringIsNULL(minAccount) && minScore == 0.0D) {
            if (!Dto.stringIsNULL(room.getOwner()) && room.getPlayerMap().containsKey(room.getOwner()) && room.getPlayerMap().get(room.getOwner()) != null) {
                minAccount = room.getOwner();
            } else {
                var6 = room.getPlayerMap().keySet().iterator();

                while(var6.hasNext()) {
                    account = (String)var6.next();
                    if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                        minAccount = account;
                        break;
                    }
                }
            }
        }

        room.setCutIndex(((Playerinfo)room.getPlayerMap().get(minAccount)).getMyIndex());
        var6 = room.getUserPacketMap().keySet().iterator();

        while(var6.hasNext()) {
            account = (String)var6.next();
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                ((UserPacketGPPJ)room.getUserPacketMap().get(account)).setScore(0.0D);
                ((UserPacketGPPJ)room.getUserPacketMap().get(account)).setXzTimes(0);
                ((UserPacketGPPJ)room.getUserPacketMap().get(account)).setBankerTimes(0);
                if (((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() == 1) {
                    ((Playerinfo)room.getPlayerMap().get(account)).addPlayTimes();
                }
            }
        }

        this.redisInfoService.insertSummary(roomNo, "_GP_PJ");
        if (room.getLeftArray() == null || room.getLeftArray().length < room.getPlayerMap().size() * 2) {
            room.getUsedArray().clear();
        }

    }

    public void startGame(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        String[] pai;
        if (room.getGid() == 5) {
            pai = GPPJCore.ShufflePai();
        } else if (room.getLeftArray() != null && room.getLeftArray().length >= room.getPlayerMap().size() * 2) {
            pai = room.getLeftArray();
        } else {
            pai = MjPjCore.shuffleCard();
        }

        JSONArray dice = this.dice(roomNo);
        room.setDice(dice);
        this.faPai(roomNo, pai);
        int sleepType = room.getUsedArray().size() > 0 ? 0 : 1;
        if (room.getBankerType() == 0 || room.getBankerType() == 3) {
            room.setGameStatus(4);
            this.startBetTimer(roomNo, sleepType);
        }

        if (room.getBankerType() == 1) {
            room.setGameStatus(3);
            this.startRobTimer(roomNo, sleepType);
        }

        if (room.getBankerType() == 4) {
            if (room.getGameIndex() == 1) {
                room.setGameStatus(3);
                this.startRobTimer(roomNo, sleepType);
            } else {
                room.setGameStatus(4);
                this.startBetTimer(roomNo, sleepType);
            }
        }

        if (room.getBankerType() == 2) {
            room.setGameStatus(5);
            this.startShowTimer(roomNo, sleepType);
        }

    }

    public void chooseBanker(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        List<String> qzList = new ArrayList();
        List<String> allList = new ArrayList();
        Iterator var5 = room.getUserPacketMap().keySet().iterator();

        while(var5.hasNext()) {
            String account = (String)var5.next();
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null && ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() > 0) {
                allList.add(account);
                if (((UserPacketGPPJ)room.getUserPacketMap().get(account)).getBankerTimes() > 0) {
                    qzList.add(account);
                }
            }
        }

        int bankerIndex;
        if (qzList.size() == 0) {
            bankerIndex = RandomUtils.nextInt(allList.size());
            room.setBanker((String)allList.get(bankerIndex));
        } else {
            bankerIndex = RandomUtils.nextInt(qzList.size());
            room.setBanker((String)qzList.get(bankerIndex));
        }

    }

    public JSONArray dice(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        JSONArray dice = new JSONArray();
        if (room != null) {
            int diceNum = !Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("dice_num") ? room.getSetting().getInt("dice_num") : 3;

            for(int i = 0; i < diceNum; ++i) {
                dice.add(RandomUtils.nextInt(6) + 1);
            }
        }

        return dice;
    }

    public void faPai(String roomNo, String[] pai) {
        int paiIndex = 0;
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        Iterator var5 = room.getUserPacketMap().keySet().iterator();

        while(true) {
            String account;
            do {
                do {
                    do {
                        if (!var5.hasNext()) {
                            int[] left = new int[pai.length - paiIndex];
                            String[] leftArray = new String[pai.length - paiIndex];

                            for(int i = 0; i < left.length; ++i) {
                                if (room.getGid() == 5) {
                                    left[i] = GPPJCore.getPaiValue(pai[i + paiIndex]);
                                } else if (room.getGid() == 17) {
                                    left[i] = MjPjCore.getCardValue(pai[i + paiIndex]);
                                    leftArray[i] = pai[i + paiIndex];
                                }
                            }

                            room.setLeftArray(leftArray);
                            room.setLeftPai(left);
                            return;
                        }

                        account = (String)var5.next();
                    } while(!room.getUserPacketMap().containsKey(account));
                } while(room.getUserPacketMap().get(account) == null);
            } while(((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() <= 0);

            String[] userPai = new String[2];

            for(int i = 0; i < userPai.length; ++i) {
                userPai[i] = pai[paiIndex];
                ++paiIndex;
            }

            ((UserPacketGPPJ)room.getUserPacketMap().get(account)).setPai(userPai);
            if (room.getGid() == 5) {
                ((UserPacketGPPJ)room.getUserPacketMap().get(account)).setPaiType(GPPJCore.getPaiType(userPai));
            } else if (room.getGid() == 17) {
                ((UserPacketGPPJ)room.getUserPacketMap().get(account)).setPaiType(MjPjCore.getCardType(userPai));
            }
        }
    }

    public void summaryBankerMj(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (!Dto.stringIsNULL(room.getBanker()) && room.getUserPacketMap().containsKey(room.getBanker()) && room.getUserPacketMap().get(room.getBanker()) != null) {
            UserPacketGPPJ banker = (UserPacketGPPJ)room.getUserPacketMap().get(room.getBanker());
            Iterator var4 = room.getUserPacketMap().keySet().iterator();

            while(var4.hasNext()) {
                String account = (String)var4.next();
                if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null && ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() > 0) {
                    if (!account.equals(room.getBanker())) {
                        UserPacketGPPJ userPacketGPPJ = (UserPacketGPPJ)room.getUserPacketMap().get(account);
                        int compareResult = MjPjCore.compareCard(userPacketGPPJ.getPai(), banker.getPai());
                        double sum;
                        if (compareResult == 1) {
                            sum = (double)(room.getScore() * ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getXzTimes());
                        } else {
                            sum = (double)(-room.getScore() * ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getXzTimes());
                        }

                        if (sum != 0.0D) {
                            banker.setScore(Dto.sub(banker.getScore(), sum));
                            userPacketGPPJ.setScore(Dto.add(userPacketGPPJ.getScore(), sum));
                            double bankerScoreOld = ((Playerinfo)room.getPlayerMap().get(room.getBanker())).getScore();
                            ((Playerinfo)room.getPlayerMap().get(room.getBanker())).setScore(Dto.sub(bankerScoreOld, sum));
                            double otherScoreOld = ((Playerinfo)room.getPlayerMap().get(account)).getScore();
                            ((Playerinfo)room.getPlayerMap().get(account)).setScore(Dto.add(otherScoreOld, sum));
                        }
                    }

                    room.getUsedArray().addAll(JSONArray.fromObject(MjPjCore.getCardValue(((UserPacketGPPJ)room.getUserPacketMap().get(account)).getPai())));
                }
            }

            Collections.sort(room.getUsedArray());
        }
    }

    public void summaryBanker(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (!Dto.stringIsNULL(room.getBanker()) && room.getUserPacketMap().containsKey(room.getBanker()) && room.getUserPacketMap().get(room.getBanker()) != null) {
            UserPacketGPPJ banker = (UserPacketGPPJ)room.getUserPacketMap().get(room.getBanker());
            Iterator var4 = room.getUserPacketMap().keySet().iterator();

            while(true) {
                String account;
                do {
                    do {
                        do {
                            do {
                                if (!var4.hasNext()) {
                                    return;
                                }

                                account = (String)var4.next();
                            } while(!room.getUserPacketMap().containsKey(account));
                        } while(room.getUserPacketMap().get(account) == null);
                    } while(((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() <= 0);
                } while(account.equals(room.getBanker()));

                UserPacketGPPJ other = (UserPacketGPPJ)room.getUserPacketMap().get(account);
                double sum;
                if (GPPJCore.comparePai(other.getPai(), banker.getPai()) == 1) {
                    sum = (double)(room.getScore() * ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getXzTimes());
                } else {
                    sum = (double)(-room.getScore() * ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getXzTimes());
                }

                if (room.getMultiple() == 0) {
                    if (banker.getPaiType() != 38 && other.getPaiType() != 38) {
                        if (banker.getPaiType() >= 27 || other.getPaiType() >= 27) {
                            sum *= 2.0D;
                        }
                    } else {
                        sum *= 4.0D;
                    }
                }

                banker.setScore(Dto.sub(banker.getScore(), sum));
                other.setScore(Dto.add(other.getScore(), sum));
                double bankerScoreOld = ((Playerinfo)room.getPlayerMap().get(room.getBanker())).getScore();
                ((Playerinfo)room.getPlayerMap().get(room.getBanker())).setScore(Dto.sub(bankerScoreOld, sum));
                double otherScoreOld = ((Playerinfo)room.getPlayerMap().get(account)).getScore();
                ((Playerinfo)room.getPlayerMap().get(account)).setScore(Dto.add(otherScoreOld, sum));
            }
        }
    }

    public void summaryCompare(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room.getRoomType() == 0 || room.getRoomType() == 6 || room.getRoomType() == 7) {
            room.setScore(10);
        }

        List<String> gameList = new ArrayList();
        Iterator var4 = room.getUserPacketMap().keySet().iterator();

        String account;
        while(var4.hasNext()) {
            account = (String)var4.next();
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null && ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() > 0) {
                gameList.add(account);
            }
        }

        var4 = gameList.iterator();

        while(var4.hasNext()) {
            account = (String)var4.next();
            UserPacketGPPJ me = (UserPacketGPPJ)room.getUserPacketMap().get(account);
            double sum = 0.0D;
            Iterator var9 = gameList.iterator();

            while(var9.hasNext()) {
                String uuid = (String)var9.next();
                UserPacketGPPJ other = (UserPacketGPPJ)room.getUserPacketMap().get(uuid);
                if (GPPJCore.comparePai(me.getPai(), other.getPai()) == 1) {
                    sum += (double)room.getScore();
                } else if (GPPJCore.comparePai(me.getPai(), other.getPai()) == -1) {
                    sum -= (double)room.getScore();
                }
            }

            me.setScore(Dto.add(me.getScore(), sum));
            double otherScoreOld = ((Playerinfo)room.getPlayerMap().get(account)).getScore();
            ((Playerinfo)room.getPlayerMap().get(account)).setScore(Dto.add(otherScoreOld, sum));
        }

    }

    public int obtainNowReadyCount(String roomNo) {
        int readyCount = 0;
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        Iterator var4 = room.getUserPacketMap().keySet().iterator();

        while(var4.hasNext()) {
            String account = (String)var4.next();
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null && ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() == 1) {
                ++readyCount;
            }
        }

        return readyCount;
    }

    public boolean isAllReady(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        Iterator var3 = room.getUserPacketMap().keySet().iterator();

        String account;
        do {
            if (!var3.hasNext()) {
                return true;
            }

            account = (String)var3.next();
        } while(!room.getUserPacketMap().containsKey(account) || room.getUserPacketMap().get(account) == null || ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() == 1);

        return false;
    }

    public boolean isAllQz(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        Iterator var3 = room.getUserPacketMap().keySet().iterator();

        String account;
        do {
            if (!var3.hasNext()) {
                return true;
            }

            account = (String)var3.next();
        } while(!room.getUserPacketMap().containsKey(account) || room.getUserPacketMap().get(account) == null || ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() == 0 || ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() == 3);

        return false;
    }

    public boolean isAllXz(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        Iterator var3 = room.getUserPacketMap().keySet().iterator();

        String account;
        do {
            if (!var3.hasNext()) {
                return true;
            }

            account = (String)var3.next();
        } while(!room.getUserPacketMap().containsKey(account) || room.getUserPacketMap().get(account) == null || account.equals(room.getBanker()) || ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() == 0 || ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() == 4);

        return false;
    }

    public boolean isAllShow(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        Iterator var3 = room.getUserPacketMap().keySet().iterator();

        String account;
        do {
            if (!var3.hasNext()) {
                return true;
            }

            account = (String)var3.next();
        } while(!room.getUserPacketMap().containsKey(account) || room.getUserPacketMap().get(account) == null || ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() == 0 || ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() == 5);

        return false;
    }

    public boolean isAllAgreeClose(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        Iterator var3 = room.getUserPacketMap().keySet().iterator();

        String account;
        do {
            if (!var3.hasNext()) {
                return true;
            }

            account = (String)var3.next();
        } while(!room.getUserPacketMap().containsKey(account) || room.getUserPacketMap().get(account) == null || ((Playerinfo)room.getPlayerMap().get(account)).getIsCloseRoom() == 1);

        return false;
    }

    public int obtainMaxTimes(JSONArray array) {
        int maxTimes = 0;

        for(int i = 0; i < array.size(); ++i) {
            JSONObject baseNum = array.getJSONObject(i);
            if (baseNum.getInt("isUse") == 1 && baseNum.getInt("val") > maxTimes) {
                maxTimes = baseNum.getInt("val");
            }
        }

        return maxTimes;
    }

    public JSONObject obtainRoomData(String roomNo, String account) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        JSONObject obj = new JSONObject();
        obj.put("playerCount", room.getPlayerCount());
        obj.put("gameStatus", room.getGameStatus());
        obj.put("room_no", room.getRoomNo());
        obj.put("roomType", room.getRoomType());
        obj.put("game_count", room.getGameCount());
        obj.put("player_count", room.getPlayerCount());
        obj.put("di", room.getScore());
        if (room.getRoomType() == 6) {
            obj.put("clubCode", room.getClubCode());
        }

        if (room.getRoomType() == 0 || room.getRoomType() == 6 || room.getRoomType() == 7) {
            StringBuffer roomInfo = new StringBuffer();
            roomInfo.append(room.getWfType());
            roomInfo.append("   ");
            roomInfo.append(room.getPlayerCount());
            roomInfo.append("人");
            obj.put("roomInfo", String.valueOf(roomInfo));
        }

        obj.put("banker", this.obtainBankerIndex(roomNo));
        obj.put("bankerType", room.getBankerType());
        obj.put("ownerIndex", this.obtainOwnerIndex(roomNo));
        obj.put("game_index", room.getGameIndex());
        if (room.getGameIndex() == 0) {
            obj.put("game_index", 1);
        }

        obj.put("myIndex", ((Playerinfo)room.getPlayerMap().get(account)).getMyIndex());
        obj.put("dice", room.getDice());
        obj.put("cutIndex", room.getCutIndex());
        obj.put("cutPlace", room.getCutPlace());
        obj.put("paiIndex", this.obtainPaiIndex(roomNo));
        obj.put("paiArray", this.obtainPlayerIndex(roomNo));
        obj.put("leftPai", this.obtainLeftPai(roomNo));
        obj.put("bankerTimes", this.obtainBankerTimes(roomNo));
        obj.put("btnType", this.obtainBtnType(roomNo, account));
        obj.put("qzTimes", this.obtainQzBtn(roomNo, account));
        obj.put("xzTimes", this.obtainXzBtn(roomNo, account));
        obj.put("users", this.obtainAllPlayer(roomNo));
        obj.put("qzJoin", this.obtainQzResult(roomNo));
        obj.put("xzJoin", this.obtainXzResult(roomNo));
        obj.put("gameData", this.obtainGameData(roomNo, account));
        obj.put("showStartBtn", this.obtainStartBtnStatus(roomNo));
        obj.put("startBtnIndex", this.obtainBankerIndex(roomNo));
        obj.put("isClose", 0);
        obj.put("usedArray", room.getUsedArray());
        if (room.getJieSanTime() > 0 && room.getGameStatus() != 7) {
            obj.put("isClose", 1);
            obj.put("closeData", this.obtainCloseData(roomNo));
        }

        if ((room.getRoomType() == 0 || room.getRoomType() == 6 || room.getRoomType() == 7) && room.getGameStatus() == 7) {
            obj.put("summaryData", this.obtainFinalSummaryData(roomNo));
        }

        int[] myPai = new int[]{0, 0};
        if (room.getBankerType() == 1 && ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getPai() != null) {
            if (room.getGid() == 5) {
                myPai[0] = GPPJCore.getPaiValue(((UserPacketGPPJ)room.getUserPacketMap().get(account)).getPai()[1]);
            } else if (room.getGid() == 17) {
                myPai[0] = MjPjCore.getCardValue(((UserPacketGPPJ)room.getUserPacketMap().get(account)).getPai()[1]);
            }
        }

        obj.put("myPai", myPai);
        return obj;
    }

    public JSONArray obtainCloseData(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        JSONArray closeData = new JSONArray();
        Iterator var4 = room.getUserPacketMap().keySet().iterator();

        while(var4.hasNext()) {
            String account = (String)var4.next();
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                JSONObject obj = new JSONObject();
                obj.put("index", ((Playerinfo)room.getPlayerMap().get(account)).getMyIndex());
                obj.put("name", ((Playerinfo)room.getPlayerMap().get(account)).getName());
                obj.put("jiesanTimer", room.getJieSanTime());
                obj.put("result", ((Playerinfo)room.getPlayerMap().get(account)).getIsCloseRoom());
                closeData.add(obj);
            }
        }

        return closeData;
    }

    public JSONObject obtainGameData(String roomNo, String account) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        JSONObject gameData = new JSONObject();
        Iterator var5 = room.getPlayerMap().keySet().iterator();

        while(true) {
            String uuid;
            do {
                do {
                    do {
                        if (!var5.hasNext()) {
                            return gameData;
                        }

                        uuid = (String)var5.next();
                    } while(!room.getUserPacketMap().containsKey(uuid));
                } while(room.getUserPacketMap().get(uuid) == null);
            } while(((UserPacketGPPJ)room.getUserPacketMap().get(uuid)).getStatus() <= 0);

            JSONObject obj = new JSONObject();
            obj.put("index", ((Playerinfo)room.getPlayerMap().get(uuid)).getMyIndex());
            if (room.getGameStatus() == 3 || room.getGameStatus() == 4) {
                if (room.getBankerType() == 1 && uuid.equals(account)) {
                    String[] myPai = ((UserPacketGPPJ)room.getUserPacketMap().get(uuid)).getPai();
                    if (myPai != null && myPai.length == 2) {
                        if (room.getGid() == 5) {
                            obj.put("pai", new int[]{GPPJCore.getPaiValue(myPai[1]), 0});
                        } else if (room.getGid() == 17) {
                            obj.put("pai", new int[]{MjPjCore.getCardValue(myPai[1]), 0});
                        } else {
                            obj.put("pai", new int[0]);
                        }
                    } else {
                        obj.put("pai", new int[0]);
                    }
                } else {
                    obj.put("pai", new int[]{0, 0});
                }
            }

            if (room.getGameStatus() == 5) {
                if (!uuid.equals(account) && ((UserPacketGPPJ)room.getUserPacketMap().get(uuid)).getStatus() != 5) {
                    obj.put("pai", new int[]{0, 0});
                } else {
                    if (room.getGid() == 5) {
                        obj.put("pai", GPPJCore.getPaiValue(((UserPacketGPPJ)room.getUserPacketMap().get(uuid)).getPai()));
                    } else if (room.getGid() == 17) {
                        obj.put("pai", MjPjCore.getCardValue(((UserPacketGPPJ)room.getUserPacketMap().get(uuid)).getPai()));
                    }

                    obj.put("paiType", ((UserPacketGPPJ)room.getUserPacketMap().get(uuid)).getPaiType());
                }
            }

            if (room.getGameStatus() == 6) {
                if (room.getGid() == 5) {
                    obj.put("pai", GPPJCore.getPaiValue(((UserPacketGPPJ)room.getUserPacketMap().get(uuid)).getPai()));
                } else if (room.getGid() == 17) {
                    obj.put("pai", MjPjCore.getCardValue(((UserPacketGPPJ)room.getUserPacketMap().get(uuid)).getPai()));
                }

                obj.put("paiType", ((UserPacketGPPJ)room.getUserPacketMap().get(uuid)).getPaiType());
                obj.put("sum", ((UserPacketGPPJ)room.getUserPacketMap().get(uuid)).getScore());
                obj.put("scoreLeft", ((Playerinfo)room.getPlayerMap().get(uuid)).getScore());
            }

            gameData.put(((Playerinfo)room.getPlayerMap().get(uuid)).getMyIndex(), obj);
        }
    }

    public JSONArray obtainQzResult(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        JSONArray qzResult = new JSONArray();
        Iterator var4 = room.getUserPacketMap().keySet().iterator();

        while(var4.hasNext()) {
            String account = (String)var4.next();
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                UserPacketGPPJ up = (UserPacketGPPJ)room.getUserPacketMap().get(account);
                if (up.getStatus() == 3) {
                    JSONObject obj = new JSONObject();
                    obj.put("index", ((Playerinfo)room.getPlayerMap().get(account)).getMyIndex());
                    obj.put("qzTimes", up.getBankerTimes());
                    qzResult.add(obj);
                }
            }
        }

        return qzResult;
    }

    public JSONArray obtainXzResult(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        JSONArray xzResult = new JSONArray();
        Iterator var4 = room.getUserPacketMap().keySet().iterator();

        while(var4.hasNext()) {
            String account = (String)var4.next();
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                UserPacketGPPJ up = (UserPacketGPPJ)room.getUserPacketMap().get(account);
                JSONObject obj = new JSONObject();
                obj.put("index", ((Playerinfo)room.getPlayerMap().get(account)).getMyIndex());
                obj.put("xzTimes", up.getXzTimes());
                xzResult.add(obj);
            }
        }

        return xzResult;
    }

    public JSONArray obtainAllPlayer(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        Iterator var4 = room.getPlayerMap().keySet().iterator();

        while(var4.hasNext()) {
            String account = (String)var4.next();
            Playerinfo player = (Playerinfo)room.getPlayerMap().get(account);
            if (player != null) {
                UserPacketGPPJ up = (UserPacketGPPJ)room.getUserPacketMap().get(account);
                JSONObject obj = new JSONObject();
                obj.put("account", player.getAccount());
                obj.put("name", player.getName());
                obj.put("headimg", player.getRealHeadimg());
                obj.put("sex", player.getSex());
                obj.put("ip", player.getIp());
                obj.put("vip", player.getVip());
                obj.put("location", player.getLocation());
                obj.put("area", player.getArea());
                obj.put("score", player.getScore());
                obj.put("index", player.getMyIndex());
                obj.put("userOnlineStatus", player.getStatus());
                obj.put("ghName", player.getGhName());
                obj.put("introduction", player.getSignature());
                obj.put("userStatus", up.getStatus());
                array.add(obj);
            }
        }

        return array;
    }

    public JSONArray obtainQzBtn(String roomNo, String account) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        JSONArray qzTimes = room.getQzTimes();

        for(int i = 0; i < qzTimes.size(); ++i) {
            JSONObject obj = new JSONObject();
            int value = qzTimes.getInt(i);
            obj.put("name", String.valueOf((new StringBuffer()).append(value).append("倍")));
            obj.put("val", value);
            obj.put("isUse", 0);
            if (room.getRoomType() == 0 || room.getRoomType() == 6 || room.getRoomType() == 7) {
                obj.put("isUse", 1);
            }

            array.add(obj);
        }

        return array;
    }

    public JSONArray obtainXzBtn(String roomNo, String account) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        JSONArray xzTimes = room.getBaseNum();

        for(int i = 0; i < xzTimes.size(); ++i) {
            JSONObject obj = xzTimes.getJSONObject(i);
            obj.put("isUse", 0);
            if (room.getRoomType() == 0 || room.getRoomType() == 6 || room.getRoomType() == 7) {
                obj.put("isUse", 1);
            }

            array.add(obj);
        }

        return array;
    }

    public int obtainBtnType(String roomNo, String account) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if ((room.getGameStatus() == 0 || room.getGameStatus() == 1 || room.getGameStatus() == 6 || room.getGameStatus() == 8) && !Dto.stringIsNULL(account) && room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
            if (room.getGameIndex() > 1 && room.getGameIndex() % (room.getGameCount() / room.getPlayerCount()) == 0) {
                if (room.getBankerType() == 4) {
                    if (room.getGameStatus() == 8) {
                        return account.equals(room.getBanker()) ? 3 : 0;
                    }

                    if (room.getGameStatus() == 6) {
                        return ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() != 3 ? 2 : 0;
                    }
                }

                if (room.getBankerType() == 3) {
                    String currentBanker = !Dto.stringIsNULL(room.getBanker()) ? room.getBanker() : room.getOwner();
                    String nextBanker = this.obtainNextBanker(roomNo, ((Playerinfo)room.getPlayerMap().get(currentBanker)).getMyIndex());
                    if (account.equals(nextBanker)) {
                        return 3;
                    }

                    return 0;
                }
            }

            return 1;
        } else {
            return 0;
        }
    }

    public int obtainStartBtnStatus(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room.getGameStatus() == 0 || room.getGameStatus() == 1 || room.getGameStatus() == 6) {
            Iterator var3 = room.getUserPacketMap().keySet().iterator();

            while(var3.hasNext()) {
                String account = (String)var3.next();
                if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null && ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() == 1) {
                    return 1;
                }
            }
        }

        return 0;
    }

    public int[] obtainLeftPai(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room.getLeftPai() == null) {
            return new int[0];
        } else if (room.getGameStatus() != 5 && room.getGameStatus() != 6 && room.getGameStatus() != 7) {
            int[] leftPai = new int[room.getLeftPai().length];

            for(int i = 0; i < leftPai.length; ++i) {
                leftPai[i] = 0;
            }

            return leftPai;
        } else {
            return room.getLeftPai();
        }
    }

    public JSONArray obtainFinalSummaryData(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room.getFinalSummaryData().size() > 0) {
            return room.getFinalSummaryData();
        } else {
            JSONArray array = new JSONArray();
            Iterator var4 = room.getUserPacketMap().keySet().iterator();

            while(var4.hasNext()) {
                String account = (String)var4.next();
                if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null && ((Playerinfo)room.getPlayerMap().get(account)).getPlayTimes() > 0) {
                    JSONObject obj = new JSONObject();
                    obj.put("name", ((Playerinfo)room.getPlayerMap().get(account)).getName());
                    obj.put("account", account);
                    obj.put("headimg", ((Playerinfo)room.getPlayerMap().get(account)).getRealHeadimg());
                    obj.put("score", ((Playerinfo)room.getPlayerMap().get(account)).getScore());
                    obj.put("isOwner", 0);
                    if (account.equals(room.getOwner())) {
                        obj.put("isOwner", 1);
                    }

                    obj.put("isWinner", 0);
                    if (((Playerinfo)room.getPlayerMap().get(account)).getScore() > 0.0D) {
                        obj.put("isWinner", 1);
                    }

                    array.add(obj);
                }
            }

            room.setFinalSummaryData(array);
            return array;
        }
    }

    public JSONArray obtainPaiIndex(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        int index = room.getCutPlace();
        JSONArray array = new JSONArray();
        Iterator var5 = room.getUserPacketMap().keySet().iterator();

        while(var5.hasNext()) {
            String account = (String)var5.next();
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null && ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() > 0) {
                array.add(index);
                ++index;
                if (index > 16) {
                    index = 1;
                }
            }
        }

        return array;
    }

    private List<Integer> obtainQzList(String roomNo) {
        List<Integer> qzList = new ArrayList();
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room != null) {
            Iterator var4 = room.getUserPacketMap().keySet().iterator();

            while(var4.hasNext()) {
                String player = (String)var4.next();
                if (room.getUserPacketMap().containsKey(player) && room.getUserPacketMap().get(player) != null && ((UserPacketGPPJ)room.getUserPacketMap().get(player)).getStatus() > 0 && ((UserPacketGPPJ)room.getUserPacketMap().get(player)).getBankerTimes() > 0) {
                    qzList.add(((Playerinfo)room.getPlayerMap().get(player)).getMyIndex());
                }
            }
        }

        return qzList;
    }

    public JSONArray obtainPlayerIndex(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        Iterator var4 = room.getUserPacketMap().keySet().iterator();

        while(var4.hasNext()) {
            String account = (String)var4.next();
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null && ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() > 0) {
                array.add(((Playerinfo)room.getPlayerMap().get(account)).getMyIndex());
            }
        }

        return array;
    }

    public int obtainBankerTimes(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        return !Dto.stringIsNULL(room.getBanker()) && room.getUserPacketMap().containsKey(room.getBanker()) && room.getUserPacketMap().get(room.getBanker()) != null ? ((UserPacketGPPJ)room.getUserPacketMap().get(room.getBanker())).getBankerTimes() : -1;
    }

    public int obtainBankerIndex(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room.getBankerType() == 0 && !Dto.stringIsNULL(room.getOwner()) && room.getPlayerMap().containsKey(room.getOwner()) && room.getPlayerMap().get(room.getOwner()) != null) {
            return ((Playerinfo)room.getPlayerMap().get(room.getOwner())).getMyIndex();
        } else {
            return (room.getBankerType() == 1 || room.getBankerType() == 3 || room.getBankerType() == 4) && !Dto.stringIsNULL(room.getBanker()) && room.getPlayerMap().containsKey(room.getBanker()) && room.getPlayerMap().get(room.getBanker()) != null ? ((Playerinfo)room.getPlayerMap().get(room.getBanker())).getMyIndex() : -1;
        }
    }

    public int obtainOwnerIndex(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        return !Dto.stringIsNULL(room.getOwner()) && room.getPlayerMap().containsKey(room.getOwner()) && room.getPlayerMap().get(room.getOwner()) != null ? ((Playerinfo)room.getPlayerMap().get(room.getOwner())).getMyIndex() : -1;
    }

    private String obtainNextBanker(String roomNo, int currentBankerIndex) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        int newBankerIndex = (currentBankerIndex + 1) % room.getPlayerMap().size();
        Iterator var5 = room.getPlayerMap().keySet().iterator();

        String account;
        do {
            if (!var5.hasNext()) {
                return room.getBanker();
            }

            account = (String)var5.next();
        } while(((Playerinfo)room.getPlayerMap().get(account)).getMyIndex() != newBankerIndex);

        if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null && ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() > 0) {
            return account;
        } else {
            return this.obtainNextBanker(roomNo, newBankerIndex);
        }
    }

    private void startCutTimer(final String roomNo, final int sleepType) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        final int timeLeft = !Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("cutTime") ? room.getSetting().getInt("cutTime") : 10;
        ThreadPoolHelper.executorService.submit(new Runnable() {
            public void run() {
                GPPJGameEventDeal.this.gameTimerGPPJ.gameOverTime(roomNo, 2, 2, timeLeft, sleepType);
            }
        });
    }

    private void startRobTimer(final String roomNo, final int sleepType) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        final int gameStatus = room.getGameStatus();
        final int timeLeft = !Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("robTime") ? room.getSetting().getInt("robTime") : 10;
        ThreadPoolHelper.executorService.submit(new Runnable() {
            public void run() {
                GPPJGameEventDeal.this.gameTimerGPPJ.gameOverTime(roomNo, gameStatus, 3, timeLeft, sleepType);
            }
        });
    }

    private void startBetTimer(final String roomNo, final int sleepType) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        final int timeLeft = !Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("betTime") ? room.getSetting().getInt("betTime") : 10;
        ThreadPoolHelper.executorService.submit(new Runnable() {
            public void run() {
                GPPJGameEventDeal.this.gameTimerGPPJ.gameOverTime(roomNo, 4, 4, timeLeft, sleepType);
            }
        });
    }

    private void startShowTimer(final String roomNo, final int sleepType) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        final int timeLeft = !Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("showTime") ? room.getSetting().getInt("showTime") : 60;
        ThreadPoolHelper.executorService.submit(new Runnable() {
            public void run() {
                GPPJGameEventDeal.this.gameTimerGPPJ.gameOverTime(roomNo, 5, 5, timeLeft, sleepType);
            }
        });
    }

    private void startReshuffleTimer(final String roomNo, final int sleepType) {
        GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        final int timeLeft = !Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("reshuffleTime") ? room.getSetting().getInt("reshuffleTime") : 60;
        ThreadPoolHelper.executorService.submit(new Runnable() {
            public void run() {
                GPPJGameEventDeal.this.gameTimerGPPJ.gameOverTime(roomNo, 8, 0, timeLeft, sleepType);
            }
        });
    }
}
