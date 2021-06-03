
package com.zhuoan.biz.event.gdy;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.gdy.GDYColor;
import com.zhuoan.biz.game.dao.util.BaseSqlUtil;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.gdy.GDYGameRoom;
import com.zhuoan.biz.model.gdy.GDYPacker;
import com.zhuoan.biz.model.gdy.GDYUserPacket;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.GDYService;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.Dto;
import com.zhuoan.util.JsonUtil;
import com.zhuoan.util.thread.ThreadPoolHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Resource;
import javax.jms.Destination;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GDYGameEventDeal {
    private static final Logger log = LoggerFactory.getLogger(GDYGameEventDeal.class);
    @Resource
    private Destination gdyQueueDestination;
    @Resource
    private ProducerService producerService;
    @Resource
    private GDYGameTimer gdyGameTimer;
    @Resource
    private GDYService gdyService;
    @Resource
    private RedisInfoService redisInfoService;

    public GDYGameEventDeal() {
    }

    public void gdyTest(SocketIOClient client, JSONObject object) {
        log.info("干瞪眼测试接收到客户端数据：" + object.toString());
        if (object == null) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数", "gdyTestPush");
        } else {
            CommonConstant.sendMsgEventYes(client, "成功", "gdyTestPush");
        }

    }

    public void gameReady(SocketIOClient client, JSONObject object) {}

    public void gameDouble(SocketIOClient client, JSONObject object) {}

    public void gamePrompt(SocketIOClient client, JSONObject object) {}

    public void gameEvent(SocketIOClient client, JSONObject object) {}

    public void gameTrustee(SocketIOClient client, JSONObject object) {}

    public void reconnectGame(SocketIOClient client, JSONObject object) {}

    public void gameContinue(SocketIOClient client, JSONObject object) {}

    public void exitRoom(SocketIOClient client, JSONObject object) {
        String eventName = "exitRoomPush_GDY";
        if (!JsonUtil.isNullVal(object, new String[]{"room_no", "account"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[room_no,account]", eventName);
        } else {
            String account = object.getString("account");
            String roomNo = object.getString("room_no");
            GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (room == null) {
                CommonConstant.sendMsgEventYes(client, "退出成功", eventName);
            } else {
                GDYUserPacket up = (GDYUserPacket)room.getUserPacketMap().get(account);
                Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                if (up != null && playerinfo != null) {
                    object = new JSONObject();
                    object.put("index", playerinfo.getMyIndex());
                    object.put("code", 1);
                    int gameStatus = room.getGameStatus();
                    int roomType = room.getRoomType();
                    if (playerinfo.getPlayTimes() > 0) {
                        if (0 != gameStatus && 1 != gameStatus && 6 != gameStatus && 7 != gameStatus) {
                            CommonConstant.sendMsgEventNo(client, "游戏中无法退出", (String)null, eventName);
                            return;
                        }

                        if (0 == roomType || 9 == roomType) {
                            CommonConstant.sendMsgEventNo(client, "游戏中无法退出", (String)null, eventName);
                            return;
                        }

                        if (up.getStatus() == 7) {
                            CommonConstant.sendMsgEventNo(client, "破产玩家中途无法退出", (String)null, eventName);
                            return;
                        }
                    }

                    this.gdyService.exitRoomByPlayer(roomNo, playerinfo.getId(), account);
                    if (room.getPlayerMap().size() == 0 && 0 == roomType) {
                        this.gdyService.removeRoom(roomNo);
                    }

                    if (!"1".equals(room.getIsAllStart())) {
                        object.put("startIndex", this.gdyService.getStartIndex(roomNo));
                    }

                    List<UUID> allUUIDList = new ArrayList();
                    String isInforOwn = !object.containsKey("isInforOwn") ? "1" : object.getString("isInforOwn");
                    Iterator var13 = room.getPlayerMap().keySet().iterator();

                    while(true) {
                        String s;
                        do {
                            if (!var13.hasNext()) {
                                CommonConstant.sendMsgEventAll(allUUIDList, object, eventName);
                                int playerCount = room.getPlayerCount();
                                if (playerCount < 2) {
                                    playerCount = 2;
                                }

                                if (this.gdyService.isAllReady(roomNo) && room.getNowReadyCount() >= 2) {
                                    if (0 == roomType && room.getPlayerMap().size() != playerCount) {
                                        return;
                                    }

                                    if (9 == roomType && "1".equals(room.getIsAllStart()) && room.getPlayerMap().size() != playerCount) {
                                        return;
                                    }

                                    this.gdyService.gameSetZhuang(roomNo);
                                }

                                return;
                            }

                            s = (String)var13.next();
                        } while("0".equals(isInforOwn) && account.equals(s));

                        allUUIDList.add(((Playerinfo)room.getPlayerMap().get(s)).getUuid());
                    }
                } else {
                    CommonConstant.sendMsgEventYes(client, "退出成功", eventName);
                }
            }
        }
    }

    public void closeRoom(SocketIOClient client, JSONObject object) {
        String eventName = "closeRoomPush_GDY";
        if (!JsonUtil.isNullVal(object, new String[]{"room_no", "account", "type"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[room_no,account,type]", eventName);
        } else {
            int type = object.getInt("type");
            if (1 != type && -1 != type) {
                log.info("干瞪眼解散房间type错误");
            } else {
                String account = object.getString("account");
                final String roomNo = object.getString("room_no");
                GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
                if (room != null) {
                    GDYUserPacket up = (GDYUserPacket)room.getUserPacketMap().get(account);
                    Playerinfo player = (Playerinfo)room.getPlayerMap().get(account);
                    if (up != null && player != null) {
                        player.setIsCloseRoom(type);
                        if (up.getStatus() == 7) {
                            CommonConstant.sendMsgEventNo(client, "破产玩家不能发起解散", "破产玩家不能发起解散", eventName);
                        } else if (-1 == type) {
                            room.setJieSanTime(0);
                            room.setIsClose(1);
                            Iterator var10 = room.getUserPacketMap().keySet().iterator();

                            while(var10.hasNext()) {
                                String s = (String)var10.next();
                                up = (GDYUserPacket)room.getUserPacketMap().get(s);
                                if (up != null) {
                                    player.setIsCloseRoom(0);
                                }
                            }

                            object = new JSONObject();
                            object.put("code", 1);
                            object.put("msg", ((Playerinfo)room.getPlayerMap().get(account)).getName() + " 拒绝解散房间");
                            object.put("type", type);
                            CommonConstant.sendMsgEventAll(room.getAllUUIDList(), object, eventName);
                        } else {
                            if (1 == type) {
                                if (room.isAgreeClose()) {
                                    this.redisInfoService.insertSummary(roomNo, "_GDY");
                                    room.setIsClose(-1);
                                    room.setGameStatus(6);
                                    this.gdyService.settleAccounts(roomNo);
                                    return;
                                }

                                if (0 == room.getJieSanTime()) {
                                    room.setJieSanTime(60);
                                    room.setIsClose(-1);
                                    ThreadPoolHelper.executorService.submit(new Runnable() {
                                        public void run() {
                                            GDYGameEventDeal.this.gdyGameTimer.closeRoomOverTime(roomNo, 60);
                                        }
                                    });
                                }

                                object = new JSONObject();
                                object.put("code", 1);
                                object.put("type", type);
                                object.put("time", room.getJieSanTime());
                                object.put("array", this.gdyService.getDissolveRoomInfo(roomNo));
                                CommonConstant.sendMsgEventAll(room.getAllUUIDList(), object, eventName);
                            }

                        }
                    }
                }
            }
        }
    }

    public void earlyStartGame(SocketIOClient client, JSONObject object) {
        String eventName = "earlyStartGamePush_GDY";
        if (!JsonUtil.isNullVal(object, new String[]{"room_no", "account"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[room_no,account]", eventName);
        } else {
            String account = object.getString("account");
            String roomNo = object.getString("room_no");
            GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (room != null) {
                if (0 != room.getGameStatus() && 6 != room.getGameStatus() && 1 != room.getGameStatus()) {
                    CommonConstant.sendMsgEventNo(client, "请稍后再试", (String)null, eventName);
                } else {
                    GDYUserPacket up = (GDYUserPacket)room.getUserPacketMap().get(account);
                    Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                    if (up != null && playerinfo != null) {
                        if ("1".equals(room.getIsAllStart())) {
                            CommonConstant.sendMsgEventNo(client, "人数未满无法提前开始", (String)null, eventName);
                        } else {
                            up.setStatus(1);
                            object.put("index", playerinfo.getMyIndex());
                            object.put("code", 1);
                            CommonConstant.sendMsgEventAll(room.getAllUUIDList(), object, "gameReadyPush_GDY");
                            if (room.getNowReadyCount() < 2) {
                                CommonConstant.sendMsgEventNo(client, "至少要两个人准备才能开始", (String)null, eventName);
                            } else if (!this.gdyService.isAllReady(roomNo)) {
                                CommonConstant.sendMsgEventNo(client, "玩家没有全部准备", (String)null, eventName);
                            } else {
                                this.gdyService.gameSetZhuang(roomNo);
                            }
                        }
                    }
                }
            }
        }
    }

    public void createRoom(SocketIOClient client, JSONObject data) {
        String account = data.getString("account");
        String roomNo = data.getString("room_no");
        JSONObject roomData = this.gdyService.obtainRoomData(roomNo, account);
        if (Dto.isObjNull(roomData)) {
            CommonConstant.sendMsgEventNo(client, "创建房间失败", (String)null, "enterRoomPush_GDY");
        } else {
            data = new JSONObject();
            data.put("code", 1);
            data.put("data", roomData);
            CommonConstant.sendMsgEvent(client, data, "enterRoomPush_GDY");
        }

    }

    public void joinRoom(SocketIOClient client, JSONObject data) {
        final String roomNo = data.getString("room_no");
        final String account = data.getString("account");
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
        GDYUserPacket up = (GDYUserPacket)room.getUserPacketMap().get(account);
        int roomType = room.getRoomType();
        if (1 == room.getGameStatus() && (9 == roomType && room.getGameIndex() > 0 || room.getNowReadyCount() >= 2)) {
            up.setTimeLeft(15);
            ThreadPoolHelper.executorService.submit(new Runnable() {
                public void run() {
                    GDYGameEventDeal.this.gdyGameTimer.gameReadyOverTime(roomNo, account, 15);
                }
            });
        }

        this.createRoom(client, data);
        JSONObject obj = this.gdyService.obtainPlayerInfo(roomNo, account);
        CommonConstant.sendMsgEventAll(room.getAllUUIDList(account), obj, "playerEnterPush_GDY");
    }
}
