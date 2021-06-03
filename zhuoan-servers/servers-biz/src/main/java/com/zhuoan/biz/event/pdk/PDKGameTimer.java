
package com.zhuoan.biz.event.pdk;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.pdk.PDKGameRoom;
import com.zhuoan.biz.model.pdk.PDKUserPacket;
import com.zhuoan.constant.event.enmu.PDKEventEnum;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.PDKService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import javax.annotation.Resource;
import javax.jms.Destination;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PDKGameTimer {
    private static final Logger log = LoggerFactory.getLogger(PDKGameTimer.class);
    @Resource
    private Destination pdkQueueDestination;
    @Resource
    private ProducerService producerService;
    @Resource
    private PDKService PDKService;

    public PDKGameTimer() {
    }

    public void gamePlayOverTime(String roomNo, int timeLeft, String account, int processNum) {
        int i = timeLeft;

        while(i >= 0) {
            PDKGameRoom room = (PDKGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (room != null && 3 == room.getGameStatus()) {
                int cardProcessNum = room.getCardProcessNum();
                if (processNum != cardProcessNum) {
                    return;
                }

                String focusAccount = room.getFocusAccount();
                PDKUserPacket up = (PDKUserPacket)room.getUserPacketMap().get(account);
                Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                if (up != null && playerinfo != null) {
                    if (!account.equals(focusAccount) && 4 != up.getStatus()) {
                        return;
                    }

                    room.setTimeLeft(i);
                    if (i == 0 || up.isTrustee() && i == timeLeft - 3) {
                        boolean isPlay = room.isPlay();
                        if (isPlay) {
                            up.setTrustee(true);
                        }

                        JSONObject data = new JSONObject();
                        data.put("room_no", roomNo);
                        data.put("account", account);
                        SocketIOClient client = GameMain.server.getClient(playerinfo.getUuid());
                        this.producerService.sendMessage(this.pdkQueueDestination, new Messages(client, data, 21, PDKEventEnum.PDK_GAME.getType()));
                    }

                    try {
                        Thread.sleep(1000L);
                    } catch (Exception var14) {
                        log.info("新跑得快出牌倒计时 [{}]", var14);
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
            PDKGameRoom room = (PDKGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (null == room) {
                return;
            }

            int gameStatus = room.getGameStatus();
            if (1 != gameStatus) {
                return;
            }

            PDKUserPacket up = (PDKUserPacket)room.getUserPacketMap().get(account);
            Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
            if (null == up || null == playerinfo) {
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
                int sorts = PDKEventEnum.PDK_EXIT_ROOM.getType();
                int roomType = room.getRoomType();
                if ((0 == roomType || 9 == roomType || 10 == roomType) && playerinfo.getPlayTimes() > 0) {
                    sorts = PDKEventEnum.PDK_GAME_READY.getType();
                }

                SocketIOClient client = GameMain.server.getClient(playerinfo.getUuid());
                this.producerService.sendMessage(this.pdkQueueDestination, new Messages(client, data, 21, sorts));
            }

            try {
                Thread.sleep(1000L);
            } catch (Exception var13) {
                log.info("新跑得快准备倒计时 [{}]", var13);
            }
        }

    }

    public void closeRoomOverTime(String roomNo, int timeLeft) {
        for(int i = timeLeft; i >= 0; --i) {
            PDKGameRoom room = (PDKGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (room == null || 0 == room.getJieSanTime()) {
                return;
            }

            room.setJieSanTime(i);
            if (i == 0) {
                room.setGameStatus(5);
                this.PDKService.settleAccounts(roomNo);
            }

            try {
                Thread.sleep(1000L);
            } catch (Exception var6) {
                log.info("新跑得快房间解散倒计时", var6);
            }
        }

    }
}
