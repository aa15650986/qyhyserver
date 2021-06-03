
package com.zhuoan.biz.event.ddz;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.ddz.DdzCore;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.ddz.DdzGameRoom;
import com.zhuoan.biz.model.ddz.UserPacketDdz;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class GameTimerDdz {
    private static final Logger logger = LoggerFactory.getLogger(GameTimerDdz.class);
    public static final int TIMER_TYPE_ROB = 2;
    public static final int TIMER_TYPE_DOUBLE = 3;
    public static final int TIMER_TYPE_EVENT = 4;
    @Resource
    private Destination ddzQueueDestination;
    @Resource
    private ProducerService producerService;
    @Resource
    private Destination matchDealQueueDestination;
    @Resource
    private RedisService redisService;

    public GameTimerDdz() {
    }

    public void gameReadyOverTime(String roomNo, String outAccount, int timeLeft) {
        for (int i = timeLeft; i >= 0 && RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null; --i) {
            DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
            if (room.getGameStatus() != 1) {
                this.sendReadyTimerToAll(outAccount, 0, room);
                break;
            }

            if (!room.getPlayerMap().containsKey(outAccount) || room.getPlayerMap().get(outAccount) == null) {
                this.sendReadyTimerToAll(outAccount, 0, room);
                break;
            }

            if (room.getPlayerMap().size() != 3) {
                this.sendReadyTimerToAll(outAccount, 0, room);
                break;
            }

            if (((UserPacketDdz) room.getUserPacketMap().get(outAccount)).getStatus() == 1) {
                this.sendReadyTimerToAll(outAccount, 0, room);
                break;
            }

            this.sendReadyTimerToAll(outAccount, i, room);
            if (i == 0) {
                JSONObject data = new JSONObject();
                data.put("room_no", roomNo);
                data.put("account", outAccount);
                this.producerService.sendMessage(this.ddzQueueDestination, new Messages((SocketIOClient) null, data, 2, 7));
            }

            try {
                Thread.sleep(1000L);
            } catch (Exception var7) {
                logger.error("", var7);
            }
        }

    }

    private void sendReadyTimerToAll(String outAccount, int time, DdzGameRoom room) {
        JSONObject result = new JSONObject();
        result.put("index", ((Playerinfo) room.getPlayerMap().get(outAccount)).getMyIndex());
        result.put("time", time);
        CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "gameTimer_DDZ");
    }

    public void gameEventOverTime(String roomNo, String nextAccount, int timeLeft) {
        for (int i = timeLeft; i >= 0 && RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null; --i) {
            DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
            if (room.getFocusIndex() != ((Playerinfo) room.getPlayerMap().get(nextAccount)).getMyIndex()) {
                break;
            }

            room.setTimeLeft(i);
            if (i == timeLeft && room.getSetting().containsKey("auto_last") && room.getSetting().getInt("auto_last") == 1) {
                List<String> lastCard = room.getLastCard();
                if (room.getLastCard().size() == 0 || nextAccount.equals(room.getLastOperateAccount())) {
                    lastCard.clear();
                }

                if (DdzCore.checkCard(lastCard, ((UserPacketDdz) room.getUserPacketMap().get(nextAccount)).getMyPai())) {
                    int cardType = DdzCore.obtainCardType(((UserPacketDdz) room.getUserPacketMap().get(nextAccount)).getMyPai());
                    if (cardType != 7 && cardType != 8) {
                        JSONObject data = new JSONObject();
                        data.put("room_no", roomNo);
                        data.put("account", nextAccount);
                        data.put("paiList", ((UserPacketDdz) room.getUserPacketMap().get(nextAccount)).getMyPai());
                        data.put("type", 1);
                        this.producerService.sendMessage(this.ddzQueueDestination, new Messages((SocketIOClient) null, data, 2, 3));
                        break;
                    }
                }
            }

            JSONObject data;
            if (i == timeLeft && ((UserPacketDdz) room.getUserPacketMap().get(nextAccount)).getIsTrustee() == 1) {
                data = new JSONObject();
                data.put("room_no", roomNo);
                data.put("account", nextAccount);
                this.producerService.sendMessage(this.ddzQueueDestination, new Messages((SocketIOClient) null, data, 2, 10));
                break;
            }

            if (i == 0 && room.getRoomType() != 0 && room.getRoomType() != 2 && room.getRoomType() != 6) {
                data = new JSONObject();
                data.put("room_no", roomNo);
                data.put("account", nextAccount);
                data.put("type", 1);
                this.producerService.sendMessage(this.ddzQueueDestination, new Messages((SocketIOClient) null, data, 2, 9));
                break;
            }

            try {
                Thread.sleep(1000L);
            } catch (Exception var9) {
                logger.error("", var9);
            }
        }

    }

    public void gameRobOverTime(String roomNo, int focus, int type, int timeLeft) {
        for (int i = timeLeft; i >= 0 && RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null; --i) {
            DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
            if (room.getFocusIndex() != focus) {
                break;
            }

            room.setTimeLeft(i);
            if (i == 0) {
                Iterator var7 = room.getPlayerMap().keySet().iterator();

                String account;
                do {
                    if (!var7.hasNext()) {
                        return;
                    }

                    account = (String) var7.next();
                } while (((Playerinfo) room.getPlayerMap().get(account)).getMyIndex() != focus);

                JSONObject data = new JSONObject();
                data.put("room_no", roomNo);
                data.put("account", account);
                data.put("type", type);
                data.put("isChoice", 0);
                this.producerService.sendMessage(this.ddzQueueDestination, new Messages((SocketIOClient) null, data, 2, 2));
                break;
            }

            try {
                Thread.sleep(1000L);
            } catch (Exception var10) {
                logger.error("", var10);
            }
        }

    }

    public void closeRoomOverTime(String roomNo, int timeLeft) {
        for (int i = timeLeft; i >= 0 && RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null; --i) {
            DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
            if (room.getJieSanTime() == 0) {
                break;
            }

            room.setJieSanTime(i);
            if (i == 0) {
                List<String> autoAccountList = new ArrayList();
                Iterator var6 = room.getUserPacketMap().keySet().iterator();

                String account;
                while (var6.hasNext()) {
                    account = (String) var6.next();
                    if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null && ((Playerinfo) room.getPlayerMap().get(account)).getIsCloseRoom() == 0) {
                        autoAccountList.add(account);
                    }
                }

                var6 = autoAccountList.iterator();

                while (var6.hasNext()) {
                    account = (String) var6.next();
                    JSONObject data = new JSONObject();
                    data.put("room_no", room.getRoomNo());
                    data.put("account", account);
                    data.put("type", 1);
                    SocketIOClient client = GameMain.server.getClient(((Playerinfo) room.getPlayerMap().get(account)).getUuid());
                    this.producerService.sendMessage(this.ddzQueueDestination, new Messages(client, data, 2, 8));
                }
            }

            try {
                Thread.sleep(1000L);
            } catch (Exception var10) {
                logger.error("", var10);
            }
        }

    }

    public void doubleOverTime(String roomNo, int timeLeft) {
        for (int i = timeLeft; i >= 0 && RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null; --i) {
            DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
            if (room.getGameStatus() != 10) {
                break;
            }

            room.setTimeLeft(i);
            if (i == 0) {
                List<String> autoAccountList = new ArrayList();
                Iterator var6 = room.getUserPacketMap().keySet().iterator();

                String account;
                while (var6.hasNext()) {
                    account = (String) var6.next();
                    if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null && ((UserPacketDdz) room.getUserPacketMap().get(account)).getDoubleTime() < 1) {
                        autoAccountList.add(account);
                    }
                }

                var6 = autoAccountList.iterator();

                while (var6.hasNext()) {
                    account = (String) var6.next();
                    JSONObject data = new JSONObject();
                    data.put("room_no", room.getRoomNo());
                    data.put("account", account);
                    data.put("type", 0);
                    SocketIOClient client = GameMain.server.getClient(((Playerinfo) room.getPlayerMap().get(account)).getUuid());
                    this.producerService.sendMessage(this.ddzQueueDestination, new Messages(client, data, 2, 14));
                }

                return;
            }

            try {
                Thread.sleep(1000L);
            } catch (Exception var10) {
                logger.error("", var10);
            }
        }

    }

    public void checkTimer() {
        String key = "room_map";
        Map<Object, Object> roomMap = this.redisService.hmget(key);
        if (roomMap != null && roomMap.size() > 0) {
            Iterator var3 = roomMap.keySet().iterator();

            while (var3.hasNext()) {
                Object roomNo = var3.next();
                Object roomObj = this.redisService.hget("room_map", String.valueOf(roomNo));
                if (roomObj != null) {
                    JSONObject roomInfo = JSONObject.fromObject(roomObj);
                    int timeLeft = roomInfo.getInt("timeLeft") - 1;
                    if (timeLeft <= 8) {
                        this.doOverTimeDeal(roomNo, roomInfo);
                    } else {
                        if (RoomManage.gameRoomMap.containsKey(String.valueOf(roomNo)) && RoomManage.gameRoomMap.get(String.valueOf(roomNo)) != null) {
                            ((GameRoom) RoomManage.gameRoomMap.get(String.valueOf(roomNo))).setTimeLeft(timeLeft);
                        }

                        this.redisService.hset(key, String.valueOf(roomNo), String.valueOf(roomInfo.element("timeLeft", timeLeft)));
                    }
                }
            }
        }

    }

    public void doOverTimeDeal(Object roomNo, JSONObject roomInfo) {
        switch (roomInfo.getInt("timerType")) {
            case 2:
                this.gameRobOverTime(String.valueOf(roomNo), roomInfo.getInt("focus"), roomInfo.getInt("type"), 0);
                break;
            case 3:
                this.doubleOverTime(String.valueOf(roomNo), 0);
                break;
            case 4:
                this.gameEventOverTime(String.valueOf(roomNo), roomInfo.getString("nextPlayerAccount"), 0);
        }

    }
}
