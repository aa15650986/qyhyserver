
package com.zhuoan.biz.event.gdy;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.gdy.GDYGameRoom;
import com.zhuoan.biz.model.gdy.GDYUserPacket;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.GDYService;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import javax.annotation.Resource;
import javax.jms.Destination;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class GDYGameTimer {
    @Resource
    private Destination gdyQueueDestination;
    @Resource
    private ProducerService producerService;
    @Resource
    private GDYService gdyService;
    @Resource
    private Destination daoQueueDestination;
    @Resource
    private RedisInfoService redisInfoService;

    public GDYGameTimer() {
    }

    public void closeRoomOverTime(String roomNo, int timeLeft) {
        for(int i = timeLeft; i >= 0; --i) {
            GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (room == null || 0 == room.getJieSanTime()) {
                return;
            }

            room.setJieSanTime(i);
            if (i == 0) {
                room.setGameStatus(6);
                this.gdyService.settleAccounts(roomNo);
            }

            try {
                Thread.sleep(1000L);
            } catch (Exception var6) {
                System.out.println("干瞪眼房间解散倒计时" + var6);
            }
        }

    }

    public void gameSetZhuangOverTime(String roomNo) {
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);

        try {
            Thread.sleep((long)(room.getTimeLeft() * 1000));
        } catch (Exception var4) {
            System.out.println("干瞪眼定庄倒计时" + var4);
        }

        this.gdyService.startGame(roomNo);
    }

    public void gamePlayOverTime(String roomNo, int timeLeft, String account, int processNum) {
        int i = timeLeft;

        while(i >= 0) {
            GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (room != null && 5 == room.getGameStatus()) {
                int cardProcessNum = room.getCardProcessNum();
                if (processNum != cardProcessNum) {
                    return;
                }

                String focusAccount = room.getFocusAccount();
                GDYUserPacket up = (GDYUserPacket)room.getUserPacketMap().get(account);
                Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                if (up != null && playerinfo != null) {
                    if (!account.equals(focusAccount) && 4 != up.getStatus()) {
                        return;
                    }

                    room.setTimeLeft(i);
                    if (i == 0) {
                        boolean isPlay = room.isPlay();
                        if (isPlay) {
                            up.setTrustee(true);
                        }

                        JSONObject data = new JSONObject();
                        data.put("room_no", roomNo);
                        data.put("account", account);
                        data.put("cardType", 0);
                        SocketIOClient client = GameMain.server.getClient(playerinfo.getUuid());
                        this.producerService.sendMessage(this.gdyQueueDestination, new Messages(client, data, 18, 4));
                    }

                    try {
                        Thread.sleep(1000L);
                    } catch (Exception var14) {
                        System.out.println("干瞪眼出牌倒计时" + var14);
                    }

                    --i;
                    continue;
                }

                return;
            }

            return;
        }

    }

    public void gameReadyOverTime(String roomNo, String account, int timeLeft) {
        for(int i = timeLeft; i >= 0; --i) {
            GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (room == null) {
                return;
            }

            int gameStatus = room.getGameStatus();
            if (1 != gameStatus) {
                return;
            }

            GDYUserPacket up = (GDYUserPacket)room.getUserPacketMap().get(account);
            Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
            if (up == null || playerinfo == null) {
                return;
            }

            if (1 == up.getStatus()) {
                return;
            }

            up.setTimeLeft(i);
            if (i == 0) {
                JSONObject data = new JSONObject();
                data.put("room_no", roomNo);
                data.put("account", account);
                int sorts = 8;
                int roomType = room.getRoomType();
                if ((0 == roomType || 9 == roomType) && playerinfo.getPlayTimes() > 0) {
                    sorts = 1;
                }

                SocketIOClient client = GameMain.server.getClient(playerinfo.getUuid());
                this.producerService.sendMessage(this.gdyQueueDestination, new Messages(client, data, 18, sorts));
            }

            try {
                Thread.sleep(1000L);
            } catch (Exception var13) {
                System.out.println("干瞪眼准备倒计时" + var13);
            }
        }

    }
}
