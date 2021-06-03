
package com.zhuoan.biz.event.sg;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.sg.SGGameRoom;
import com.zhuoan.biz.model.sg.SGUserPacket;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.SGService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import javax.annotation.Resource;
import javax.jms.Destination;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class SGGameTimer {
    @Resource
    private Destination sgQueueDestination;
    @Resource
    private ProducerService producerService;
    @Resource
    private Destination daoQueueDestination;
    @Resource
    private SGService sgService;

    public SGGameTimer() {
    }

    public void gameReadyOverTime(String roomNo, String account, int timeLeft) {
        for(int i = timeLeft; i >= 0; --i) {
            SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (room == null) {
                return;
            }

            int gameStatus = room.getGameStatus();
            if (1 != gameStatus) {
                return;
            }

            SGUserPacket up = (SGUserPacket)room.getUserPacketMap().get(account);
            Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
            if (up == null || playerinfo == null) {
                return;
            }

            if (1 == up.getStatus()) {
                up.setTimeLeft(0);
                return;
            }

            up.setTimeLeft(i);
            if (i == 0) {
                JSONObject data = new JSONObject();
                data.put("room_no", roomNo);
                data.put("account", account);
                int sorts = 5;
                int roomType = room.getRoomType();
                if ((0 == roomType || 9 == roomType) && playerinfo.getPlayTimes() > 0) {
                    sorts = 1;
                }

                SocketIOClient client = GameMain.server.getClient(playerinfo.getUuid());
                this.producerService.sendMessage(this.sgQueueDestination, new Messages(client, data, 19, sorts));
            }

            try {
                Thread.sleep(1000L);
            } catch (Exception var13) {
                System.out.println("三公准备倒计时" + var13);
            }
        }

    }

    public void closeRoomOverTime(String roomNo, int timeLeft) {
        for(int i = timeLeft; i >= 0; --i) {
            SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (room == null || 0 == room.getJieSanTime()) {
                return;
            }

            room.setJieSanTime(i);
            if (i == 0) {
                room.setGameStatus(5);
                this.sgService.settleAccounts(roomNo);
            }

            try {
                Thread.sleep(1000L);
            } catch (Exception var6) {
                System.out.println("三公房间解散倒计时" + var6);
            }
        }

    }

    public void robZhuangOverTime(String roomNo, String account, int timeLeft) {
        int i = timeLeft;

        while(i >= 0) {
            SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (room != null && 2 == room.getGameStatus() && 1 == room.getPlayType()) {
                SGUserPacket up = (SGUserPacket)room.getUserPacketMap().get(account);
                Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                if (up != null && playerinfo != null) {
                    if (0 != up.getStatus() && 2 != up.getStatus()) {
                        room.setTimeLeft(i);
                        if (i == 0) {
                            JSONObject data = new JSONObject();
                            data.put("room_no", roomNo);
                            data.put("account", account);
                            data.put("robMultiple", 0);
                            SocketIOClient client = GameMain.server.getClient(playerinfo.getUuid());
                            this.producerService.sendMessage(this.sgQueueDestination, new Messages(client, data, 19, 2));
                        }

                        try {
                            Thread.sleep(1000L);
                        } catch (Exception var10) {
                            System.out.println("三公抢庄倒计时" + var10);
                        }

                        --i;
                        continue;
                    }

                    return;
                }

                return;
            }

            return;
        }

    }

    public void confirmZhuangOverTime(String roomNo) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room != null && 1 == room.getPlayType() && 2 == room.getGameStatus()) {
            try {
                Thread.sleep((long)(room.getTimeLeft() * 1000));
            } catch (Exception var4) {
                System.out.println("三公定庄倒计时" + var4);
            }

            room.setTimeLeft(0);
            room.setGameStatus(3);
            this.sgService.gameBet(roomNo);
        }
    }

    public void gameBetOverTime(String roomNo, String account, int timeLeft) {
        int i = timeLeft;

        while(i >= 0) {
            SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (room != null && 3 == room.getGameStatus()) {
                SGUserPacket up = (SGUserPacket)room.getUserPacketMap().get(account);
                Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                if (up != null && playerinfo != null) {
                    if (0 != up.getStatus() && 3 != up.getStatus()) {
                        if (1 == room.getPlayType() && room.getBanker().equals(account)) {
                            return;
                        }

                        room.setTimeLeft(i);
                        if (i == 0) {
                            JSONObject data = new JSONObject();
                            data.put("room_no", roomNo);
                            data.put("account", account);
                            data.put("bet", this.sgService.getAnteList(roomNo, account).get(0));
                            SocketIOClient client = GameMain.server.getClient(playerinfo.getUuid());
                            this.producerService.sendMessage(this.sgQueueDestination, new Messages(client, data, 19, 3));
                        }

                        try {
                            Thread.sleep(1000L);
                        } catch (Exception var10) {
                            System.out.println("三公下注倒计时" + var10);
                        }

                        --i;
                        continue;
                    }

                    return;
                }

                return;
            }

            return;
        }

    }

    public void showCardOverTime(String roomNo, String account, int timeLeft) {
        int i = timeLeft;

        while(i >= 0) {
            SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (room != null && 4 == room.getGameStatus()) {
                SGUserPacket up = (SGUserPacket)room.getUserPacketMap().get(account);
                Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                if (up != null && playerinfo != null) {
                    if (0 != up.getStatus() && 4 != up.getStatus()) {
                        room.setTimeLeft(i);
                        if (i == 0) {
                            JSONObject data = new JSONObject();
                            data.put("room_no", roomNo);
                            data.put("account", account);
                            SocketIOClient client = GameMain.server.getClient(playerinfo.getUuid());
                            this.producerService.sendMessage(this.sgQueueDestination, new Messages(client, data, 19, 4));
                        }

                        try {
                            Thread.sleep(1000L);
                        } catch (Exception var10) {
                            System.out.println("三公亮牌倒计时" + var10);
                        }

                        --i;
                        continue;
                    }

                    return;
                }

                return;
            }

            return;
        }

    }
}
