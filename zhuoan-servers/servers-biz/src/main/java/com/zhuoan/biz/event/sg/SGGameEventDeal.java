package com.zhuoan.biz.event.sg;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.game.biz.GameCircleService;
import com.zhuoan.biz.game.dao.util.BaseSqlUtil;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.sg.SGGameRoom;
import com.zhuoan.biz.model.sg.SGUserPacket;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.SGService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.Dto;
import com.zhuoan.util.JsonUtil;
import com.zhuoan.util.thread.ThreadPoolHelper;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.jms.Destination;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SGGameEventDeal {
    private static final Logger log = LoggerFactory.getLogger(SGGameEventDeal.class);
    @Resource
    private Destination daoQueueDestination;
    @Resource
    private ProducerService producerService;
    @Resource
    private SGGameTimer sgGameTimer;
    @Resource
    private SGService sgService;
    @Resource
    private GameCircleService gameCircleService;
    @Resource
    private RedisInfoService redisInfoService;

    public SGGameEventDeal() {
    }

    public void test(SocketIOClient client, JSONObject object) {
        String eventName = "testPush_SG";
        log.info("三公测试接收到客户端数据：" + object.toString());
        if (object == null) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数", eventName);
        } else {
            CommonConstant.sendMsgEventYes(client, "成功", eventName);
        }

    }

    public void ready(SocketIOClient client, JSONObject object) {}

    public void robZhuang(SocketIOClient client, JSONObject object) {}

    public void bet(SocketIOClient client, JSONObject object) {}

    public void showCard(SocketIOClient client, JSONObject object) {}

    public void exitRoom(SocketIOClient client, JSONObject object) {}

    public void closeRoom(SocketIOClient client, JSONObject object) {
        String eventName = "closeRoomPush_SG";
        if (!JsonUtil.isNullVal(object, new String[]{"room_no", "account", "type"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[room_no,account,type]", eventName);
        } else {
            int type = object.getInt("type");
            if (1 != type && -1 != type && 100 != type) {
                log.info("三公解散房间type错误");
            } else {
                String account = object.getString("account");
                final String roomNo = object.getString("room_no");
                SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
                if (room != null) {
                    if (100 == type && 0 != room.getJieSanTime()) {
                        CommonConstant.sendMsgEventNo(client, "已经提出解散房间申请", (String)null, eventName);
                    }

                    if (1 != type && -1 != type || 0 != room.getJieSanTime()) {
                        SGUserPacket up = (SGUserPacket)room.getUserPacketMap().get(account);
                        Playerinfo player = (Playerinfo)room.getPlayerMap().get(account);
                        if (up != null && player != null) {
                            if (100 == type) {
                                type = 1;
                            }

                            player.setIsCloseRoom(type);
                            object = new JSONObject();
                            if (-1 == type) {
                                room.setJieSanTime(0);
                                room.setIsClose(1);
                                Iterator var10 = room.getUserPacketMap().keySet().iterator();

                                while(var10.hasNext()) {
                                    String s = (String)var10.next();
                                    player = (Playerinfo)room.getPlayerMap().get(s);
                                    if (player != null) {
                                        player.setIsCloseRoom(0);
                                    }
                                }

                                object.put("code", 1);
                                object.put("msg", ((Playerinfo)room.getPlayerMap().get(account)).getName() + " 拒绝解散房间");
                                object.put("type", type);
                                CommonConstant.sendMsgEventAll(room.getAllUUIDList(), object, eventName);
                            } else {
                                if (1 == type) {
                                    if (this.sgService.isAgreeClose(roomNo)) {
                                        this.redisInfoService.insertSummary(roomNo, "_SG");
                                        room.setIsClose(-1);
                                        room.setGameStatus(5);
                                        this.sgService.settleAccounts(roomNo);
                                        return;
                                    }

                                    if (0 == room.getJieSanTime()) {
                                        room.setJieSanTime(60);
                                        room.setIsClose(-1);
                                        ThreadPoolHelper.executorService.submit(new Runnable() {
                                            public void run() {
                                                SGGameEventDeal.this.sgGameTimer.closeRoomOverTime(roomNo, 60);
                                            }
                                        });
                                    }

                                    object.put("code", 1);
                                    object.put("type", type);
                                    object.put("time", room.getJieSanTime());
                                    object.put("array", this.sgService.getDissolveRoomInfo(roomNo));
                                    CommonConstant.sendMsgEventAll(room.getAllUUIDList(), object, eventName);
                                }

                            }
                        }
                    }
                }
            }
        }
    }

    public void earlyStart(SocketIOClient client, JSONObject object) {
        String eventName = "earlyStartPush_SG";
        if (!JsonUtil.isNullVal(object, new String[]{"room_no", "account"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[room_no,account]", eventName);
        } else {
            String account = object.getString("account");
            String roomNo = object.getString("room_no");
            SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (room != null) {
                if (0 != room.getGameStatus() && 5 != room.getGameStatus() && 1 != room.getGameStatus()) {
                    CommonConstant.sendMsgEventNo(client, "请稍后再试", (String)null, eventName);
                } else {
                    SGUserPacket up = (SGUserPacket)room.getUserPacketMap().get(account);
                    Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                    if (up != null && playerinfo != null) {
                        int startType = room.getStartType();
                        if (1 == startType || 3 == startType) {
                            up.setStatus(1);
                            object.put("index", playerinfo.getMyIndex());
                            object.put("code", 1);
                            CommonConstant.sendMsgEventAll(room.getAllUUIDList(), object, "readyPush_SG");
                            if (this.sgService.getNowReadyCount(roomNo) < 2) {
                                CommonConstant.sendMsgEventNo(client, "必须至少有两人准备才能开始游戏", (String)null, eventName);
                            } else if (!this.sgService.isAllReady(roomNo)) {
                                CommonConstant.sendMsgEventNo(client, "玩家没有全部准备", (String)null, eventName);
                            } else {
                                CommonConstant.sendMsgEventYes(client, "开始游戏", eventName);
                                this.sgService.startGame(roomNo);
                            }
                        }
                    }
                }
            }
        }
    }

    public void reconnect(SocketIOClient client, JSONObject object) {
        String eventName = "reconnectPush_SG";
        if (!JsonUtil.isNullVal(object, new String[]{"room_no", "account", "uuid"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[room_no,account,uuid]", eventName);
        } else {
            String account = object.getString("account");
            String uuid = object.getString("uuid");
            String roomNo = object.getString("room_no");
            if (!"".equals(account) && !"".equals(uuid) && !"".equals(roomNo)) {
                JSONObject users = BaseSqlUtil.getObjectByOneConditions("uuid", "za_users", "account", account);
                if (users != null && users.containsKey("uuid") && uuid.equals(users.getString("uuid"))) {
                    GameRoom room = (GameRoom)RoomManage.gameRoomMap.get(roomNo);
                    if (room == null) {
                        CommonConstant.sendMsgEventNo(client, "房间不存在", (String)null, eventName);
                    } else if (room.getPlayerMap().containsKey(account) && room.getPlayerMap().get(account) != null) {
                        ((Playerinfo)room.getPlayerMap().get(account)).setUuid(client.getSessionId());
                        object = new JSONObject();
                        object.put("code", 1);
                        JSONObject roomData = this.sgService.obtainRoomData(roomNo, account);
                        object.put("data", roomData);
                        CommonConstant.sendMsgEvent(client, object, eventName);
                    } else {
                        CommonConstant.sendMsgEventNo(client, "不在当前房间内", (String)null, eventName);
                    }
                }
            }
        }
    }

    public void createRoom(SocketIOClient client, JSONObject data) {
        String eventName = "enterRoomPush_SG";
        String account = data.getString("account");
        String roomNo = data.getString("room_no");
        JSONObject roomData = this.sgService.obtainRoomData(roomNo, account);
        if (Dto.isObjNull(roomData)) {
            CommonConstant.sendMsgEventNo(client, "创建房间失败", (String)null, eventName);
        } else {
            data = new JSONObject();
            data.put("code", 1);
            data.put("data", roomData);
            CommonConstant.sendMsgEvent(client, data, eventName);
        }

    }

    public void joinRoom(SocketIOClient client, JSONObject data) {
        final String roomNo = data.getString("room_no");
        final String account = data.getString("account");
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        SGUserPacket up = (SGUserPacket)room.getUserPacketMap().get(account);
        if (room != null && up != null) {
            int roomType = room.getRoomType();
            if (1 == room.getGameStatus() && (9 == roomType && room.getGameIndex() > 0 || this.sgService.getNowReadyCount(roomNo) >= 2)) {
                up.setTimeLeft(10);
                ThreadPoolHelper.executorService.submit(new Runnable() {
                    public void run() {
                        SGGameEventDeal.this.sgGameTimer.gameReadyOverTime(roomNo, account, 10);
                    }
                });
            }

            this.createRoom(client, data);
            JSONObject obj = this.sgService.obtainPlayerInfo(roomNo, account);
            CommonConstant.sendMsgEventAll(room.getAllUUIDList(account), obj, "playerEnterPush_SG");
        }
    }
}
