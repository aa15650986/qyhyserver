
package com.zhuoan.biz.event.gppj;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.gppj.GPPJGameRoom;
import com.zhuoan.biz.model.gppj.UserPacketGPPJ;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Resource;
import javax.jms.Destination;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GameTimerGPPJ {
    private static final Logger logger = LoggerFactory.getLogger(GameTimerGPPJ.class);
    @Resource
    private Destination gppjQueueDestination;
    @Resource
    private ProducerService producerService;
    @Resource
    private GPPJGameEventDeal gppjGameEventDeal;

    public GameTimerGPPJ() {
    }

    public void gameOverTime(String roomNo, int gameStatus, int userStatus, int timeLeft, int sleepType) {
        if (sleepType == 1) {
            try {
                Thread.sleep(2500L);
                this.gppjGameEventDeal.changeGameStatus(roomNo);
            } catch (Exception var16) {
                logger.error("", var16);
            }
        }

        for(int i = timeLeft; i >= 0 && RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null; --i) {
            GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (room.getGameStatus() != gameStatus || gameStatus == 1 && this.gppjGameEventDeal.obtainNowReadyCount(roomNo) < 2) {
                break;
            }

            room.setTimeLeft(i);
            JSONObject result = new JSONObject();
            result.put("time", i);
            CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "timePush_GPPJ");
            if (i == 0) {
                List<String> autoAccountList = new ArrayList();
                if (gameStatus == 8) {
                    autoAccountList.add(room.getBanker());
                } else {
                    Iterator var10 = room.getUserPacketMap().keySet().iterator();

                    label108:
                    while(true) {
                        String account;
                        do {
                            do {
                                do {
                                    if (!var10.hasNext()) {
                                        break label108;
                                    }

                                    account = (String)var10.next();
                                } while(!room.getUserPacketMap().containsKey(account));
                            } while(room.getUserPacketMap().get(account) == null);
                        } while(gameStatus != 1 && ((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() == 0);

                        if (((UserPacketGPPJ)room.getUserPacketMap().get(account)).getStatus() != userStatus) {
                            autoAccountList.add(account);
                        }
                    }
                }

                int messageSort = 0;
                Iterator var18 = autoAccountList.iterator();

                while(var18.hasNext()) {
                    String account = (String)var18.next();
                    JSONObject data = new JSONObject();
                    data.put("room_no", roomNo);
                    data.put("account", account);
                    if (gameStatus == 2) {
                        messageSort = 3;
                        data.put("cutPlace", 1);
                    }

                    if (gameStatus == 3) {
                        messageSort = 4;
                        data.put("qzTimes", 0);
                    }

                    if (gameStatus == 4) {
                        messageSort = 5;
                        JSONArray baseNum = JSONArray.fromObject(room.getBaseNum());
                        data.put("xzTimes", baseNum.getJSONObject(0).getInt("val"));
                    }

                    if (gameStatus == 5) {
                        messageSort = 6;
                    }

                    if (gameStatus == 6) {
                        messageSort = 4;
                        data.put("qzTimes", 0);
                    }

                    if (gameStatus == 8) {
                        messageSort = 10;
                        data.put("type", 0);
                    }

                    if (messageSort > 0) {
                        SocketIOClient client = GameMain.server.getClient(((Playerinfo)room.getPlayerMap().get(account)).getUuid());
                        this.producerService.sendMessage(this.gppjQueueDestination, new Messages(client, data, 5, messageSort));
                    }
                }
            }

            try {
                Thread.sleep(1000L);
            } catch (Exception var15) {
                logger.error("", var15);
            }
        }

    }

    public void closeRoomOverTime(String roomNo, int timeLeft) {
        for(int i = timeLeft; i >= 0 && RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null; --i) {
            GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
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
                    if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null && ((Playerinfo)room.getPlayerMap().get(account)).getIsCloseRoom() == 0) {
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
                    this.producerService.sendMessage(this.gppjQueueDestination, new Messages(client, data, 5, 9));
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
