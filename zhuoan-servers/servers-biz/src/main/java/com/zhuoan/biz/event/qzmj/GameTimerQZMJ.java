
package com.zhuoan.biz.event.qzmj;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.qzmj.QZMJGameRoom;
import com.zhuoan.biz.model.qzmj.QZMJUserPacket;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.QZMJConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.annotation.Resource;
import javax.jms.Destination;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GameTimerQZMJ {
    private static final Logger logger = LoggerFactory.getLogger(GameTimerQZMJ.class);
    @Resource
    private Destination qzmjQueueDestination;
    @Resource
    private ProducerService producerService;
    @Resource
    private QZMJGameEventDeal qzmjGameEventDeal;

    public GameTimerQZMJ() {
    }

    public void gameStart(String roomNo, int firstStatus) {
        if (firstStatus == 2) {
            this.doDice(roomNo);
        } else if (firstStatus == 1) {
            this.checkIp(roomNo);
        }

    }

    private void changeStartStatus(String roomNo, int startStatus) {
        if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
            QZMJGameRoom room = (QZMJGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (room.getGameStatus() == 2) {
                room.setStartStatus(startStatus);
                if (startStatus != -1) {
                    this.sendChangeData(roomNo, startStatus);
                }
            }
        }

    }

    private void sendChangeData(String roomNo, int startStatus) {
        QZMJGameRoom room = (QZMJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (startStatus == 4) {
            List<String> cliTagList = new ArrayList();
            int playerCount = 0;

            for(String next = room.getBanker(); playerCount < room.getPlayerCount(); ++playerCount) {
                QZMJUserPacket up = (QZMJUserPacket)room.getUserPacketMap().get(next);
                if (QZMJConstant.hasHuaPai(up.getMyPai().toArray())) {
                    cliTagList.add(next);
                }

                next = room.getNextPlayer(next);
            }

            Iterator var20 = cliTagList.iterator();

            while(true) {
                String clientTag;
                JSONArray huavals;
                int buhuaType;
                do {
                    if (!var20.hasNext()) {
                        return;
                    }

                    clientTag = (String)var20.next();
                    if (!clientTag.equals(cliTagList.get(0))) {
                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException var16) {
                            var16.printStackTrace();
                        }
                    }

                    huavals = new JSONArray();
                    buhuaType = 10;

                    while(buhuaType == 10) {
                        JSONObject data = QZMJGameEventDeal.autoBuHua(roomNo, clientTag);
                        if (data != null) {
                            buhuaType = data.getInt("type");
                            huavals.add(data);
                        }
                    }
                } while(buhuaType == 10);

                JSONArray otherBuHua = new JSONArray();

                JSONObject buhua;
                for(int i = 0; i < huavals.size(); ++i) {
                    JSONObject obj = huavals.getJSONObject(i);
                    buhua = new JSONObject();
                    buhua.put("huaValue", obj.get("huaValue"));
                    otherBuHua.add(buhua);
                }

                Iterator var23 = room.getPlayerMap().keySet().iterator();

                while(var23.hasNext()) {
                    String uuid = (String)var23.next();
                    if (room.getPlayerMap().containsKey(uuid) && room.getPlayerMap().get(uuid) != null) {
                        buhua = new JSONObject();
                        buhua.put("index", ((Playerinfo)room.getPlayerMap().get(clientTag)).getMyIndex());
                        buhua.put("zpaishu", room.getPai().length - room.getIndex());
                        if (clientTag.equals(uuid)) {
                            buhua.put("huavals", huavals);
                        } else {
                            buhua.put("huavals", otherBuHua);
                        }

                        JSONObject buhuaData = new JSONObject();
                        buhuaData.put("type", startStatus);
                        buhuaData.put("buhua", buhua);
                        CommonConstant.sendMsgEventToSingle(((Playerinfo)room.getPlayerMap().get(uuid)).getUuid(), String.valueOf(buhuaData), "gameKaiJuPush");
                    }
                }
            }
        } else {
            Iterator var17 = room.getPlayerMap().keySet().iterator();

            while(var17.hasNext()) {
                String uuid = (String)var17.next();
                if (room.getPlayerMap().containsKey(uuid) && room.getPlayerMap().get(uuid) != null) {
                    QZMJUserPacket up = (QZMJUserPacket)room.getUserPacketMap().get(uuid);
                    JSONObject result = new JSONObject();
                    result.put("type", startStatus);
                    if (startStatus == 3) {
                        result.put("zpaishu", room.getPai().length - room.getIndex());
                    } else if (startStatus == 5) {
                        result.put("myPai", up.getMyPai().toArray());
                        result.put("huaValue", up.getHuaList().toArray());
                    }

                    CommonConstant.sendMsgEventToSingle(((Playerinfo)room.getPlayerMap().get(uuid)).getUuid(), String.valueOf(result), "gameKaiJuPush");
                }
            }

        }
    }

    private void checkIp(String roomNo) {
        this.changeStartStatus(roomNo, 1);

        try {
            Thread.sleep(3000L);
        } catch (InterruptedException var3) {
            var3.printStackTrace();
        }

        this.doDice(roomNo);
    }

    private void doDice(String roomNo) {
        this.changeStartStatus(roomNo, 2);

        try {
            Thread.sleep(1000L);
        } catch (InterruptedException var3) {
            var3.printStackTrace();
        }

        this.faPai(roomNo);
    }

    private void faPai(String roomNo) {
        this.changeStartStatus(roomNo, 3);

        try {
            Thread.sleep(250L);
        } catch (InterruptedException var3) {
            var3.printStackTrace();
        }

        if (((GameRoom)RoomManage.gameRoomMap.get(roomNo)).getGid() != 3 && ((GameRoom)RoomManage.gameRoomMap.get(roomNo)).getGid() != 13) {
            if (((GameRoom)RoomManage.gameRoomMap.get(roomNo)).getGid() == 12) {
                this.dingJin(roomNo);
            }
        } else {
            this.buHua(roomNo);
        }

    }

    private void buHua(String roomNo) {
        this.changeStartStatus(roomNo, 4);

        try {
            Thread.sleep(750L);
        } catch (InterruptedException var3) {
            var3.printStackTrace();
        }

        if (((GameRoom)RoomManage.gameRoomMap.get(roomNo)).getGid() != 3 && ((GameRoom)RoomManage.gameRoomMap.get(roomNo)).getGid() != 13) {
            if (((GameRoom)RoomManage.gameRoomMap.get(roomNo)).getGid() == 12) {
                this.moPai(roomNo);
            }
        } else {
            this.dingJin(roomNo);
        }

    }

    private void dingJin(String roomNo) {
        this.changeStartStatus(roomNo, 5);

        try {
            Thread.sleep(750L);
        } catch (InterruptedException var3) {
            var3.printStackTrace();
        }

        if (((GameRoom)RoomManage.gameRoomMap.get(roomNo)).getGid() != 3 && ((GameRoom)RoomManage.gameRoomMap.get(roomNo)).getGid() != 13) {
            if (((GameRoom)RoomManage.gameRoomMap.get(roomNo)).getGid() == 12) {
                this.buHua(roomNo);
            }
        } else {
            this.moPai(roomNo);
        }

    }

    private void moPai(String roomNo) {
        this.changeStartStatus(roomNo, -1);
        this.qzmjGameEventDeal.autoMoPai(roomNo, ((GameRoom)RoomManage.gameRoomMap.get(roomNo)).getBanker());
    }

    public void closeRoomOverTime(String roomNo, int timeLeft) {
        for(int i = timeLeft; i >= 0 && RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null; --i) {
            QZMJGameRoom room = (QZMJGameRoom)RoomManage.gameRoomMap.get(roomNo);
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
                    this.producerService.sendMessage(this.qzmjQueueDestination, new Messages(client, data, 3, 5));
                }
            }

            try {
                Thread.sleep(1000L);
            } catch (Exception var10) {
                logger.error("", var10);
            }
        }

    }

    public void gameEventOverTime(String roomNo, String nextAccount, int timeLeft, int type, int askNum) {
        for(int i = timeLeft; i >= 0; --i) {
            QZMJGameRoom room = (QZMJGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (null == room) {
                return;
            }

            if (room.getAskNum() != askNum) {
                return;
            }

            if (nextAccount.equals(room.getThisAccount()) && room.getNextAskType() != 2) {
                return;
            }

            if (2 != room.getGameStatus()) {
                return;
            }

            room.setTimeLeft(i);
            JSONObject autoEventData;
            if (i == timeLeft - 3 && ((QZMJUserPacket)room.getUserPacketMap().get(nextAccount)).getIsTrustee() == 1) {
                autoEventData = this.getAutoEventData(roomNo, nextAccount, type);
                this.producerService.sendMessage(this.qzmjQueueDestination, new Messages((SocketIOClient)null, autoEventData, 3, 3));
                return;
            }

            if (i == 0) {
                autoEventData = this.getAutoEventData(roomNo, nextAccount, type);
                this.producerService.sendMessage(this.qzmjQueueDestination, new Messages((SocketIOClient)null, autoEventData, 3, 3));
                JSONObject trusteeData = this.getTrusteeData(roomNo, nextAccount);
                this.producerService.sendMessage(this.qzmjQueueDestination, new Messages((SocketIOClient)null, trusteeData, 3, 14));
                return;
            }

            try {
                Thread.sleep(1000L);
            } catch (Exception var10) {
                logger.error("", var10);
            }
        }

    }

    private JSONObject getAutoEventData(String roomNo, String nextAccount, int type) {
        JSONObject data = new JSONObject();
        data.put("room_no", roomNo);
        data.put("account", nextAccount);
        data.put("type", type);
        return data;
    }

    private JSONObject getTrusteeData(String roomNo, String nextAccount) {
        JSONObject data = new JSONObject();
        data.put("room_no", roomNo);
        data.put("account", nextAccount);
        data.put("type", 1);
        return data;
    }

    public void cpOverTime(String roomNo, String nextAccount, int timeLeft, int askNum) {
        for(int i = timeLeft; i >= 0; --i) {
            QZMJGameRoom room = (QZMJGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (null == room) {
                return;
            }

            if (room.getAskNum() != askNum) {
                return;
            }

            if (room.getGameStatus() != 2) {
                return;
            }

            room.setTimeLeft(i);
            JSONObject cpData;
            if (i == timeLeft - 3 && ((QZMJUserPacket)room.getUserPacketMap().get(nextAccount)).getIsTrustee() == 1) {
                cpData = this.getCpData(roomNo, nextAccount);
                this.producerService.sendMessage(this.qzmjQueueDestination, new Messages((SocketIOClient)null, cpData, 3, 2));
                return;
            }

            if (i == 0) {
                cpData = this.getCpData(roomNo, nextAccount);
                this.producerService.sendMessage(this.qzmjQueueDestination, new Messages((SocketIOClient)null, cpData, 3, 2));
                JSONObject trusteeData = this.getTrusteeData(roomNo, nextAccount);
                this.producerService.sendMessage(this.qzmjQueueDestination, new Messages((SocketIOClient)null, trusteeData, 3, 14));
                return;
            }

            try {
                Thread.sleep(1000L);
            } catch (Exception var9) {
                logger.error("", var9);
            }
        }

    }

    private JSONObject getCpData(String roomNo, String nextAccount) {
        JSONObject data = new JSONObject();
        data.put("room_no", roomNo);
        data.put("account", nextAccount);
        QZMJGameRoom room = (QZMJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        data.put("pai", ((QZMJUserPacket)room.getUserPacketMap().get(nextAccount)).getMyPai().get(((QZMJUserPacket)room.getUserPacketMap().get(nextAccount)).getMyPai().size() - 1));
        return data;
    }

    public void readyOverTime(String roomNo, String account, int timeLeft) {
        for(int i = timeLeft; i >= 0; --i) {
            QZMJGameRoom room = (QZMJGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (null == room) {
                return;
            }

            if (1 != room.getGameStatus() && 3 != room.getGameStatus()) {
                break;
            }

            QZMJUserPacket up = (QZMJUserPacket)room.getUserPacketMap().get(account);
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
                data.put("account", account);
                data.put("room_no", roomNo);
                data.put("init", 1);
                int sorts = 6;
                int roomType = room.getRoomType();
                if ((0 == roomType || 9 == roomType || 10 == roomType) && playerinfo.getPlayTimes() > 0) {
                    sorts = 16;
                }

                this.producerService.sendMessage(this.qzmjQueueDestination, new Messages((SocketIOClient)null, data, 3, sorts));
            }

            try {
                Thread.sleep(1000L);
            } catch (Exception var11) {
                logger.error("泉州麻将准备倒计时 [{}]", var11);
            }
        }

    }

    private void sendReadyTimer(int time, List<UUID> uuidList) {
        JSONObject result = new JSONObject();
        result.put("readyTimerPush", time);
        CommonConstant.sendMsgEventToAll(uuidList, String.valueOf(result), "readyTimerPush");
    }
}
