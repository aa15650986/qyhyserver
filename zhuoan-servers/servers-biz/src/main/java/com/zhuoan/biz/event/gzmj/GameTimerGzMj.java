
package com.zhuoan.biz.event.gzmj;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.gzmj.GzMjGameRoom;
import com.zhuoan.biz.model.gzmj.UserPacketGzMj;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Resource;
import javax.jms.Destination;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GameTimerGzMj {
    private static final Logger logger = LoggerFactory.getLogger(GameTimerGzMj.class);
    @Resource
    private Destination gzmjQueueDestination;
    @Resource
    private ProducerService producerService;

    public GameTimerGzMj() {
    }

    public void gameOverTime(String roomNo, int gameStatus, int timeLeft) {
        int userStatus = 0;
        switch(gameStatus) {
            case 1:
                userStatus = 1;
                break;
            case 2:
                userStatus = 2;
                break;
            case 3:
                userStatus = 3;
        }

        for(int i = timeLeft; i >= 0 && RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null; --i) {
            GzMjGameRoom room = (GzMjGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (room.getGameStatus() != gameStatus && room.getGameStatus() != 1) {
                break;
            }

            room.setTimeLeft(i);
            if (i == 0) {
                List<String> autoAccountList = new ArrayList();
                Iterator var8 = room.getUserPacketMap().keySet().iterator();

                while(var8.hasNext()) {
                    String account = (String)var8.next();
                    if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null && ((UserPacketGzMj)room.getUserPacketMap().get(account)).getStatus() != userStatus) {
                        autoAccountList.add(account);
                    }
                }

                int messageSort = 0;
                Iterator var15 = autoAccountList.iterator();

                while(var15.hasNext()) {
                    String account = (String)var15.next();
                    JSONObject data = new JSONObject();
                    data.put("room_no", room.getRoomNo());
                    data.put("account", account);
                    if (gameStatus == 1 && room.getReadyOvertime() == 2) {
                        messageSort = 9;
                    }

                    if (gameStatus == 2) {
                        messageSort = 3;
                        data.put("betTimes", 0);
                    }

                    if (gameStatus == 3) {
                        messageSort = 4;
                        data.put("lackType", 10);
                    }

                    if (messageSort > 0) {
                        SocketIOClient client = GameMain.server.getClient(((Playerinfo)room.getPlayerMap().get(account)).getUuid());
                        this.producerService.sendMessage(this.gzmjQueueDestination, new Messages(client, data, 20, messageSort));
                    }
                }
            }

            try {
                Thread.sleep(1000L);
            } catch (Exception var13) {
                logger.error("", var13);
            }
        }

    }

    public void closeRoomOverTime(String roomNo, int timeLeft) {
        for(int i = timeLeft; i >= 0 && RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null; --i) {
            GzMjGameRoom room = (GzMjGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (room.getJieSanTime() == 0) {
                break;
            }

            room.setJieSanTime(i);
            if (i == 0) {
                List<String> autoAccountList = new ArrayList();
                Iterator var6 = room.getUserPacketMap().keySet().iterator();

                String account;
                while(var6.hasNext()) {
                    account = (String)var6.next();
                    if (room.getPlayerMap().containsKey(account) && room.getPlayerMap().get(account) != null && ((Playerinfo)room.getPlayerMap().get(account)).getIsCloseRoom() == 0) {
                        autoAccountList.add(account);
                    }
                }

                var6 = autoAccountList.iterator();

                while(var6.hasNext()) {
                    account = (String)var6.next();
                    JSONObject data = new JSONObject();
                    data.put("room_no", room.getRoomNo());
                    data.put("account", account);
                    data.put("type", 1);
                    SocketIOClient client = GameMain.server.getClient(((Playerinfo)room.getPlayerMap().get(account)).getUuid());
                    this.producerService.sendMessage(this.gzmjQueueDestination, new Messages(client, data, 20, 8));
                }
            }

            try {
                Thread.sleep(1000L);
            } catch (Exception var10) {
                logger.error("", var10);
            }
        }

    }
}
