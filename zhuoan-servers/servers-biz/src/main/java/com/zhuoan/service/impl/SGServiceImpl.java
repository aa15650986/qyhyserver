
package com.zhuoan.service.impl;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.sg.SGColor;
import com.zhuoan.biz.core.sg.SGNum;
import com.zhuoan.biz.event.BaseEventDeal;
import com.zhuoan.biz.event.sg.SGGameTimer;
import com.zhuoan.biz.game.biz.GameCircleService;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.sg.SGCard;
import com.zhuoan.biz.model.sg.SGGameRoom;
import com.zhuoan.biz.model.sg.SGUserPacket;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.SGService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.Dto;
import com.zhuoan.util.thread.ThreadPoolHelper;
import java.util.ArrayList;
import java.util.Collections;
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
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SGServiceImpl implements SGService {
    private final Logger log = LoggerFactory.getLogger(SGServiceImpl.class);
    @Resource
    private SGGameTimer sgGameTimer;
    @Resource
    private ProducerService producerService;
    @Resource
    private Destination daoQueueDestination;
    @Resource
    private Destination sgQueueDestination;
    @Resource
    private RedisInfoService redisInfoService;
    @Resource
    private GameCircleService gameCircleService;
    @Resource
    private RoomBiz roomBiz;
    @Resource
    private BaseEventDeal baseEventDeal;

    public SGServiceImpl() {
    }

    public JSONObject obtainRoomData(String roomNo, String account) {
        JSONObject obj = new JSONObject();
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        SGUserPacket up = (SGUserPacket)room.getUserPacketMap().get(account);
        Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
        if (room != null && up != null && playerinfo != null) {
            int roomType = room.getRoomType();
            obj.put("playerCount", room.getPlayerCount());
            obj.put("roomNo", roomNo);
            obj.put("gameIndex", room.getGameNewIndex());
            obj.put("gameCount", room.getGameCount());
            obj.put("gameStatus", room.getGameStatus());
            obj.put("roomType", roomType);
            obj.put("myIndex", playerinfo.getMyIndex());
            obj.put("roomTimeLeft", room.getTimeLeft());
            obj.put("playType", room.getPlayType());
            obj.put("ante", room.getAnte());
            if (room.getClubCode() != null) {
                obj.put("clubCode", room.getClubCode());
            }

            obj.put("users", this.obtainAllPlayer(roomNo));
            obj.put("zhuang", -1);
            int playType = room.getPlayType();
            if (1 == playType && room.getBanker() != null && room.getPlayerMap().get(room.getBanker()) != null) {
                obj.put("zhuang", ((Playerinfo)room.getPlayerMap().get(room.getBanker())).getMyIndex());
                obj.put("robMultiple", room.getRobMultiple());
            }

            int startType = room.getStartType();
            obj.put("startType", startType);
            obj.put("startTypeNum", room.getStartTypeNum());
            obj.put("isStart", this.isStart(roomNo, account));
            obj.put("gameData", this.getGameData(roomNo, account));
            if (room.getSummaryData() != null && room.getSummaryData().containsKey("array")) {
                obj.put("summaryData", room.getSummaryData());
            }

            obj.put("isEnd", 0);
            if (room.getFinalSummaryData() != null && room.getFinalSummaryData().size() > 0) {
                obj.put("isEnd", 1);
                obj.put("endData", room.getFinalSummaryData());
            }

            obj.put("isClose", room.getIsClose());
            if (room.getIsClose() == -1) {
                JSONObject jieSan = new JSONObject();
                jieSan.put("array", this.getDissolveRoomInfo(roomNo));
                jieSan.put("time", room.getJieSanTime());
                obj.put("jieSan", jieSan);
            }

            return obj;
        } else {
            return obj;
        }
    }

    public JSONArray obtainAllPlayer(String roomNo) {
        JSONArray array = new JSONArray();
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room == null) {
            return array;
        } else {
            Iterator var6 = room.getUserPacketMap().keySet().iterator();

            while(var6.hasNext()) {
                String account = (String)var6.next();
                SGUserPacket up = (SGUserPacket)room.getUserPacketMap().get(account);
                if (up != null) {
                    JSONObject playerInfo = this.obtainPlayerInfo(roomNo, account);
                    if (!Dto.isObjNull(playerInfo)) {
                        array.add(playerInfo);
                    }
                }
            }

            return array;
        }
    }

    public JSONObject obtainPlayerInfo(String roomNo, String account) {
        JSONObject obj = new JSONObject();
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room == null) {
            return obj;
        } else {
            Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
            if (playerinfo == null) {
                return obj;
            } else {
                obj.put("location", playerinfo.getLocation());
                obj.put("account", playerinfo.getAccount());
                obj.put("name", playerinfo.getName());
                obj.put("headimg", playerinfo.getRealHeadimg());
                obj.put("sex", playerinfo.getSex());
                obj.put("ip", playerinfo.getIp());
                obj.put("vip", playerinfo.getVip());
                obj.put("area", playerinfo.getArea());
                obj.put("score", playerinfo.getScore() + playerinfo.getSourceScore());
                obj.put("index", playerinfo.getMyIndex());
                obj.put("userOnlineStatus", playerinfo.getStatus());
                SGUserPacket up = (SGUserPacket)room.getUserPacketMap().get(account);
                if (up != null) {
                    obj.put("userStatus", up.getStatus());
                    obj.put("userTimeLeft", up.getTimeLeft());
                }

                return obj;
            }
        }
    }

    public JSONArray getDissolveRoomInfo(String roomNo) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        Iterator var7 = room.getUserPacketMap().keySet().iterator();

        while(var7.hasNext()) {
            String account = (String)var7.next();
            SGUserPacket up = (SGUserPacket)room.getUserPacketMap().get(account);
            Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
            if (up != null && playerinfo != null) {
                JSONObject obj = new JSONObject();
                obj.put("name", playerinfo.getName());
                obj.put("type", playerinfo.getIsCloseRoom());
                obj.put("index", playerinfo.getMyIndex());
                array.add(obj);
            }
        }

        return array;
    }

    public JSONObject getGameData(String roomNo, String account) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        int gameStatus = room.getGameStatus();
        boolean isCardVisible = false;
        if (5 == gameStatus || 6 == gameStatus) {
            isCardVisible = true;
        }

        JSONObject data = new JSONObject();
        int palyType = room.getPlayType();
        String banker = room.getBanker();
        Iterator var18 = room.getPlayerMap().keySet().iterator();

        while(true) {
            SGUserPacket up;
            Playerinfo playerinfo;
            String s;
            do {
                do {
                    if (!var18.hasNext()) {
                        return data;
                    }

                    s = (String)var18.next();
                    up = (SGUserPacket)room.getUserPacketMap().get(s);
                    playerinfo = (Playerinfo)room.getPlayerMap().get(s);
                } while(up == null);
            } while(playerinfo == null);

            JSONObject obj = new JSONObject();
            int cardType;
            if (!isCardVisible && !account.equals(s)) {
                obj.put("paiList", up.getMyBMPai());
            } else {
                cardType = up.getCardCount();
                obj.put("paiList", up.getMyPai());
                obj.put("cardType", up.getCardType());
                obj.put("cardCount", cardType);
                obj.put("double", room.getDoubleMap().get(cardType) != null ? room.getDoubleMap().get(cardType) : 1);
            }

            obj.put("index", playerinfo.getMyIndex());
            int bet = up.getBet();
            if (1 == palyType && banker.equals(s)) {
                bet = 0;
            }

            obj.put("bet", bet);
            obj.put("robMultiple", up.getRobMultiple());
            obj.put("userStatus", up.getStatus());
            obj.put("userTimeLeft", up.getTimeLeft());
            boolean isRob = false;
            boolean isAnte = false;
            boolean isShowCard = false;
            if (account.equals(s)) {
                if (1 == palyType && 2 == gameStatus && 2 != up.getStatus() && 0 != up.getStatus()) {
                    List<Integer> robList = room.getRobList();
                    obj.put("robList", robList);
                    if (robList != null && robList.size() > 0) {
                        isRob = true;
                    }
                }

                List<Integer> anteList = this.getAnteList(roomNo, account);
                if (anteList != null && anteList.size() > 0) {
                    isAnte = true;
                }

                data.put("anteList", anteList);
                if (3 == up.getStatus()) {
                    cardType = up.getCardType();
                    obj.put("cardType", cardType);
                    obj.put("cardCount", up.getCardCount());
                    obj.put("double", room.getDoubleMap().get(cardType) != null ? room.getDoubleMap().get(cardType) : 1);
                    isShowCard = true;
                }
            }

            obj.put("isRob", isRob);
            obj.put("isAnte", isAnte);
            obj.put("isShowCard", isShowCard);
            data.put(playerinfo.getMyIndex(), obj);
        }
    }

    public boolean isStart(String roomNo, String account) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room != null && account != null) {
            if (!room.getPlayerMap().containsKey(account)) {
                return false;
            } else {
                int roomType = room.getRoomType();
                if ((9 == roomType || 0 == roomType) && room.getGameIndex() != 0) {
                    return false;
                } else {
                    int startType = room.getStartType();
                    if (1 == startType || 3 == startType) {
                        String owner = room.getOwner();
                        if (3 == startType) {
                            if (account.equals(owner)) {
                                return true;
                            }
                        } else {
                            if (account.equals(owner)) {
                                return true;
                            }

                            Map<Long, String> map = new HashMap();
                            Iterator var8 = room.getPlayerMap().keySet().iterator();

                            while(var8.hasNext()) {
                                String s = (String)var8.next();
                                map.put(((Playerinfo)room.getPlayerMap().get(s)).getId(), s);
                            }

                            for(int i = 0; i < room.getUserIdList().size(); ++i) {
                                if ((Long)room.getUserIdList().get(i) > 0L) {
                                    if (account.equals(map.get(room.getUserIdList().get(i)))) {
                                        return true;
                                    }
                                    break;
                                }
                            }
                        }
                    }

                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public void exitRoomByPlayer(String roomNo, long userId, String account) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room != null) {
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
        }
    }

    public void removeRoom(String roomNo) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (null != room) {
            this.redisInfoService.delSummary(roomNo, "_SG");
            JSONObject roomInfo = new JSONObject();
            roomInfo.put("room_no", room.getRoomNo());
            roomInfo.put("status", 2);
            roomInfo.put("game_index", 0);
            this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("update_room_info", roomInfo));
            RoomManage.gameRoomMap.remove(roomNo);
        }
    }

    public boolean isAllReady(String roomNo) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room == null) {
            return false;
        } else {
            Iterator var5 = room.getUserPacketMap().keySet().iterator();

            SGUserPacket up;
            do {
                if (!var5.hasNext()) {
                    return true;
                }

                String account = (String)var5.next();
                up = (SGUserPacket)room.getUserPacketMap().get(account);
                Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                if (up == null || playerinfo == null) {
                    return false;
                }
            } while(1 == up.getStatus());

            return false;
        }
    }

    public int getNowReadyCount(String roomNo) {
        int readyCount = 0;
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room == null) {
            return readyCount;
        } else {
            Iterator var5 = room.getUserPacketMap().keySet().iterator();

            while(var5.hasNext()) {
                String account = (String)var5.next();
                SGUserPacket up = (SGUserPacket)room.getUserPacketMap().get(account);
                if (up != null && 1 == up.getStatus()) {
                    ++readyCount;
                }
            }

            return readyCount;
        }
    }

    private boolean circleFKDeduct(String roomNo) {return false;}

    public void startGame(String roomNo) {
        if (this.isAllReady(roomNo)) {
            SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (room != null) {
                int roomType = room.getRoomType();
                if (8 != roomType && 9 != roomType || this.circleFKDeduct(roomNo)) {
                    room.initGame();
                    JSONObject roomInfo = new JSONObject();
                    roomInfo.put("roomId", room.getId());
                    this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("update_room_index", roomInfo));
                    this.redisInfoService.insertSummary(roomNo, "_SG");
                    int playType = room.getPlayType();
                    if (1 == playType) {
                        this.shuffleDeingMPQZ(roomNo);
                    } else if (2 == playType) {
                        room.setGameStatus(3);
                        this.gameBet(roomNo);
                    }
                }
            }
        }
    }

    private void shuffleDeingMPQZ(final String roomNo) {}

    private void shuffleDeingDCX(final String roomNo) {}

    private List<SGCard> getSGCard() {
        List<SGCard> card = new ArrayList();
        SGColor[] var3 = SGColor.values();
        int var4 = var3.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            SGColor color = var3[var5];
            if (color.getColor() != SGColor.JOKER.getColor()) {
                SGNum[] var7 = SGNum.values();
                int var8 = var7.length;

                for(int var9 = 0; var9 < var8; ++var9) {
                    SGNum num = var7[var9];
                    if (num.getNum() <= SGNum.P_K.getNum()) {
                        SGCard sgCard = new SGCard(color, num);
                        card.add(sgCard);
                    }
                }
            }
        }

        Collections.shuffle(card);
        return card;
    }

    public boolean isAgreeClose(String roomNo) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room == null) {
            return false;
        } else {
            Iterator var5 = room.getUserPacketMap().keySet().iterator();

            SGUserPacket up;
            Playerinfo playerinfo;
            do {
                if (!var5.hasNext()) {
                    return true;
                }

                String account = (String)var5.next();
                up = (SGUserPacket)room.getUserPacketMap().get(account);
                playerinfo = (Playerinfo)room.getPlayerMap().get(account);
            } while(null == up || null == playerinfo || 1 == playerinfo.getIsCloseRoom());

            return false;
        }
    }

    public void settleAccounts(final String roomNo) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room != null && 5 == room.getGameStatus()) {
            long summaryTimes = this.redisInfoService.summaryTimes(roomNo, "_SG");
            if (summaryTimes <= 1L) {
                this.redisInfoService.delSummary(roomNo, "_SG");
                room.setGameStatus(5);
                int roomType = room.getRoomType();
                if (-1 == room.getIsClose()) {
                    if (8 == roomType || 3 == roomType || 9 == roomType && room.getGameIndex() <= 1 && (room.getSummaryData() == null || !room.getSummaryData().containsKey("array"))) {
                        this.dissolveRoomInform(roomNo, room.getAllUUIDList());
                        this.clearRoomInfo(roomNo, true);
                        return;
                    }

                    if (0 == roomType && room.getGameIndex() <= 1 && (room.getSummaryData() == null || !room.getSummaryData().containsKey("array"))) {
                        this.dissolveRoomInform(roomNo, room.getAllUUIDList());
                        this.removeRoom(roomNo);
                        return;
                    }
                }

                if (-1 != room.getIsClose()) {
                    this.scorePoints(roomNo);
                }

                this.saveGameLog(roomNo);
                this.setSummaryData(roomNo);
                if (room.isNeedFinalSummary() && (room.getGameIndex() >= room.getGameCount() || -1 == room.getIsClose())) {
                    room.setGameStatus(6);
                    this.setFinalSummaryData(roomNo);
                }

                if ((!room.isNeedFinalSummary() || room.getGameIndex() == 1 && (room.getSummaryData() == null || !room.getSummaryData().containsKey("array"))) && 0 == roomType) {
                    this.updateRoomCard(roomNo);
                }

                if (8 == roomType || 9 == roomType || 3 == roomType) {
                    this.updateUserScore(roomNo);
                }

                room.setTimeLeft(0);
                if (room.isNeedFinalSummary() && (room.getGameIndex() >= room.getGameCount() || -1 == room.getIsClose())) {
                    if (-1 == room.getIsClose()) {
                        room.setIsClose(-2);
                    } else {
                        room.setIsClose(2);
                    }
                }

                Iterator var9 = room.getUserPacketMap().keySet().iterator();

                Playerinfo playerinfo;
                SGUserPacket up;
                while(var9.hasNext()) {
                    String account = (String)var9.next();
                    up = (SGUserPacket)room.getUserPacketMap().get(account);
                    playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                    if (up != null && playerinfo != null) {
                        SocketIOClient client = GameMain.server.getClient(playerinfo.getUuid());
                        CommonConstant.sendMsgEvent(client, this.obtainRoomData(roomNo, account), "settleAccountsPush_SG");
                    }
                }

                if (room.isNeedFinalSummary() && (room.getGameIndex() >= room.getGameCount() || -1 == room.getIsClose() || -2 == room.getIsClose() || 2 == room.getIsClose())) {
                    if (0 == roomType) {
                        this.removeRoom(roomNo);
                        return;
                    }

                    if (9 == roomType) {
                        this.clearRoomInfo(roomNo, true);
                        return;
                    }
                }

                if (8 != roomType && 3 != roomType) {
                    if (9 == roomType) {
                        room.setGameStatus(1);
                        int time = this.redisInfoService.getAppTimeSetting(room.getPlatform(), room.getGid(), "SGConstant.TIMER_READY_INNING");
                        if (time < 0) {
                            time = 8;
                        }

                        final int overTime = time;
                        Iterator var11 = room.getUserPacketMap().keySet().iterator();

                        while(var11.hasNext()) {
                            final String account = (String)var11.next();
                            up = (SGUserPacket)room.getUserPacketMap().get(account);
                            playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                            if (up != null && playerinfo != null) {
                                up.setStatus(0);
                                JSONObject object = new JSONObject();
                                object.put("index", playerinfo.getMyIndex());
                                object.put("time", overTime);
                                CommonConstant.sendMsgEventAll(room.getAllUUIDList(), object, "readyTimerPush_SG");
                                ThreadPoolHelper.executorService.submit(new Runnable() {
                                    public void run() {
                                        SGServiceImpl.this.sgGameTimer.gameReadyOverTime(roomNo, account, overTime);
                                    }
                                });
                            }
                        }
                    }

                } else {
                    this.clearRoomInfo(roomNo, false);
                }
            }
        }
    }

    private void dissolveRoomInform(String roomNo, List<UUID> uuidList) {
        JSONObject object = new JSONObject();
        object.put("code", 1);
        object.put("type", 2);
        CommonConstant.sendMsgEventAll(uuidList, object, "closeRoomPush_SG");
        this.redisInfoService.delSummary(roomNo, "_SG");
    }

    public boolean isAllRobZhuang(String roomNo) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room == null && 2 != room.getGameStatus() && 1 != room.getPlayType()) {
            return false;
        } else {
            Iterator var5 = room.getUserPacketMap().keySet().iterator();

            SGUserPacket up;
            Playerinfo playerinfo;
            do {
                if (!var5.hasNext()) {
                    return true;
                }

                String account = (String)var5.next();
                up = (SGUserPacket)room.getUserPacketMap().get(account);
                playerinfo = (Playerinfo)room.getPlayerMap().get(account);
            } while(up == null || playerinfo == null || 0 == up.getStatus() || 2 == up.getStatus());

            return false;
        }
    }

    public List<Integer> getAnteList(String roomNo, String account) {
        List<Integer> anteList = new ArrayList();
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room == null) {
            return anteList;
        } else {
            SGUserPacket up = (SGUserPacket)room.getUserPacketMap().get(account);
            Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
            if (up != null && playerinfo != null) {
                if (room.getBanker() != null && room.getBanker().equals(account)) {
                    return anteList;
                } else {
                    int palyType = room.getPlayType();
                    if (3 == room.getGameStatus() && 3 != up.getStatus() && 0 != up.getStatus()) {
                        anteList = new ArrayList(room.getAnteList());
                        int bolus;
                        int winScore;
                        int bolusScore;
                        if (1 == palyType) {
                            bolus = room.getBolus();
                            if (room.getPlayerWinScore() != null) {
                                winScore = room.getPlayerWinScore().get(account) != null ? (Integer)room.getPlayerWinScore().get(account) : 0;
                                if (bolus > 1 && winScore > 0) {
                                    winScore += (Integer)anteList.get(anteList.size() - 1);
                                    bolusScore = (Integer)anteList.get(anteList.size() - 1) * bolus;
                                    if (bolusScore < winScore) {
                                        winScore = bolusScore;
                                    }

                                    anteList.add(winScore);
                                }
                            }
                        } else if (2 == palyType) {
                            bolus = room.getRoomType();
                            winScore = room.getBet();
                            if (3 == bolus || 8 == bolus || 9 == bolus) {
                                bolusScore = (int)playerinfo.getScore() + (int)playerinfo.getSourceScore();
                                if (bolusScore < room.getBet()) {
                                    winScore = bolusScore;
                                }
                            }

                            anteList.add(winScore);
                        }
                    }

                    return anteList;
                }
            } else {
                return anteList;
            }
        }
    }

    public void confirmZhuang(final String roomNo) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room != null && this.isAllRobZhuang(roomNo)) {
            int playType = room.getPlayType();
            if (1 == playType) {
                room.setTimeLeft(1);
                int maxBs = 0;
                Iterator var6 = room.getUserPacketMap().keySet().iterator();

                SGUserPacket up;
                while(var6.hasNext()) {
                    String account = (String)var6.next();
                    up = (SGUserPacket)room.getUserPacketMap().get(account);
                    if (up != null && 2 == up.getStatus() && maxBs < up.getRobMultiple()) {
                        maxBs = up.getRobMultiple();
                    }
                }

                List<String> zhuangList = new ArrayList();
                Iterator var11 = room.getUserPacketMap().keySet().iterator();

                while(var11.hasNext()) {
                    String account = (String)var11.next();
                    up = (SGUserPacket)room.getUserPacketMap().get(account);
                    if (up != null && 2 == up.getStatus() && maxBs == up.getRobMultiple()) {
                        zhuangList.add(account);
                    }
                }

                if (zhuangList.size() == 0) {
                    return;
                }

                int bankerIndex = RandomUtils.nextInt(zhuangList.size());
                room.setBanker((String)zhuangList.get(bankerIndex));
                int robMultiple = ((SGUserPacket)room.getUserPacketMap().get(zhuangList.get(bankerIndex))).getRobMultiple();
                if (robMultiple <= 0) {
                    robMultiple = 1;
                }

                room.setRobMultiple(robMultiple);
                JSONObject result = new JSONObject();
                result.put("time", room.getTimeLeft());
                result.put("zhuang", ((Playerinfo)room.getPlayerMap().get(room.getBanker())).getMyIndex());
                result.put("robMultiple", robMultiple);
                CommonConstant.sendMsgEventAll(room.getAllUUIDList(), result, "confirmZhuangPush_SG");
                room.setGameStatus(2);
                ThreadPoolHelper.executorService.submit(new Runnable() {
                    public void run() {
                        SGServiceImpl.this.sgGameTimer.confirmZhuangOverTime(roomNo);
                    }
                });
            }

        }
    }

    public void gameBet(final String roomNo) {}

    public boolean isAllBet(String roomNo) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room == null && 3 != room.getGameStatus()) {
            return false;
        } else {
            int playType = room.getPlayType();
            Iterator var6 = room.getUserPacketMap().keySet().iterator();

            SGUserPacket up;
            String account;
            do {
                Playerinfo playerinfo;
                do {
                    do {
                        do {
                            if (!var6.hasNext()) {
                                return true;
                            }

                            account = (String)var6.next();
                            up = (SGUserPacket)room.getUserPacketMap().get(account);
                            playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                        } while(up == null);
                    } while(playerinfo == null);
                } while(1 == playType && room.getBanker().equals(account));
            } while(0 == up.getStatus() || 3 == up.getStatus());

            return false;
        }
    }

    public void gameShowCard(final String roomNo) {}

    public boolean isAllShowCard(String roomNo) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room == null && 4 != room.getGameStatus()) {
            return false;
        } else {
            Iterator var5 = room.getUserPacketMap().keySet().iterator();

            SGUserPacket up;
            Playerinfo playerinfo;
            do {
                if (!var5.hasNext()) {
                    return true;
                }

                String account = (String)var5.next();
                up = (SGUserPacket)room.getUserPacketMap().get(account);
                playerinfo = (Playerinfo)room.getPlayerMap().get(account);
            } while(up == null || playerinfo == null || 0 == up.getStatus() || 4 == up.getStatus());

            return false;
        }
    }

    public void clearRoomInfo(String roomNo, boolean isClearUser) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room != null) {
            room.initGame();
            room.setGameIndex(0);
            room.setGameNewIndex(0);
            room.getSummaryData().clear();
            room.setGameStatus(0);
            room.setJieSanTime(0);
            room.getFinalSummaryData().clear();
            if (isClearUser) {
                room.setUserPacketMap(new ConcurrentHashMap());
                room.setPlayerMap(new ConcurrentHashMap());
                room.setVisitPlayerMap(new ConcurrentHashMap());
                room.setPlayerWinScore(new HashMap());

                for(int i = 0; i < room.getUserIdList().size(); ++i) {
                    room.getUserIdList().set(i, 0L);
                    room.addIndexList(i);
                }

                this.baseEventDeal.reloadClearRoom(roomNo);
            }
        }
    }

    private void saveGameLog(String roomNo) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (null != room && (-1 != room.getIsClose() || 5 == room.getGameStatus() || 6 == room.getGameStatus())) {
            JSONArray gameResult = new JSONArray();
            JSONArray array = new JSONArray();
            Iterator var8 = room.getUserPacketMap().keySet().iterator();

            JSONObject userResult;
            while(var8.hasNext()) {
                String account = (String)var8.next();
                SGUserPacket up = (SGUserPacket)room.getUserPacketMap().get(account);
                Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                if (up != null && playerinfo != null && up.getStatus() != 0) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", playerinfo.getId());
                    obj.put("total", playerinfo.getScore());
                    obj.put("fen", up.getScore());
                    array.add(obj);
                    userResult = new JSONObject();
                    userResult.put("account", account);
                    userResult.put("isWinner", 0);
                    if (up.getScore() > 0.0D) {
                        userResult.put("isWinner", 1);
                    }

                    userResult.put("score", up.getScore());
                    userResult.put("totalScore", playerinfo.getScore());
                    userResult.put("player", playerinfo.getName());
                    gameResult.add(userResult);
                }
            }

            long id = Long.valueOf(System.currentTimeMillis() + roomNo);
            userResult = new JSONObject();
            JSONArray userGameLogs = room.obtainUserGameLog(id, array);
            userResult.element("array", userGameLogs);
            userResult.element("object", (new JSONObject()).element("gamelog_id", id).element("room_type", room.getRoomType()).element("room_id", room.getId()).element("room_no", room.getRoomNo()).element("game_index", room.getGameNewIndex()).element("result", gameResult.toString()));
            this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("insert_user_game_log", userResult));
        }
    }

    private void setSummaryData(String roomNo) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        JSONObject obj = new JSONObject();
        JSONObject array = new JSONObject();
        Iterator var8 = room.getUserPacketMap().keySet().iterator();

        while(var8.hasNext()) {
            String account = (String)var8.next();
            SGUserPacket up = (SGUserPacket)room.getUserPacketMap().get(account);
            Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
            if (up != null && playerinfo != null && 0 != up.getStatus() && 1 != up.getStatus()) {
                JSONObject object = new JSONObject();
                object.put("score", up.getScore());
                object.put("index", playerinfo.getMyIndex());
                array.put(((Playerinfo)room.getPlayerMap().get(account)).getMyIndex(), object);
            }
        }

        obj.put("array", array);
        room.setSummaryData(obj);
    }

    private void setFinalSummaryData(String roomNo) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        Iterator var7 = room.getUserPacketMap().keySet().iterator();

        while(var7.hasNext()) {
            String account = (String)var7.next();
            JSONObject obj = new JSONObject();
            SGUserPacket up = (SGUserPacket)room.getUserPacketMap().get(account);
            Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
            if (up != null && playerinfo != null) {
                obj.put("account", account);
                obj.put("name", playerinfo.getName());
                obj.put("headimg", playerinfo.getRealHeadimg());
                obj.put("score", playerinfo.getScore());
                array.add(obj);
            }
        }

        room.setFinalSummaryData(array);
    }

    private void updateUserScore(String roomNo) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room != null) {
            int gameStatus = room.getGameStatus();
            if (6 == gameStatus || 5 == gameStatus) {
                int roomType = room.getRoomType();
                if (8 == roomType || 9 == roomType || 3 == roomType) {
                    JSONObject pumpInfo = new JSONObject();
                    JSONArray userDeductionData = new JSONArray();
                    Iterator var9 = room.getUserPacketMap().keySet().iterator();

                    while(true) {
                        SGUserPacket up;
                        Playerinfo playerinfo;
                        String account;
                        do {
                            do {
                                do {
                                    if (!var9.hasNext()) {
                                        if (pumpInfo.size() > 0) {
                                            if (10 != room.getRoomType() && 0 != room.getRoomType() && -1 != room.getIsClose()) {
                                                JSONObject object = new JSONObject();
                                                object.put("circleId", room.getCircleId());
                                                object.put("roomNo", room.getRoomNo());
                                                object.put("gameId", room.getGid());
                                                object.put("pumpInfo", pumpInfo);
                                                object.put("cutType", room.getCutType());
                                                object.put("changeType", "2");
                                                this.gameCircleService.circleUserPumping(object);
                                            }

                                            this.userPumpingFee(roomNo);
                                        }

                                        if (3 == roomType) {
                                            this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("user_deduction", (new JSONObject()).element("user", userDeductionData)));
                                        }

                                        return;
                                    }

                                    account = (String)var9.next();
                                    up = (SGUserPacket)room.getUserPacketMap().get(account);
                                    playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                                } while(up == null);
                            } while(playerinfo == null);
                        } while(up.getScore() == 0.0D);

                        if (8 == roomType || 9 == roomType || 10 == roomType || 0 == roomType) {
                            pumpInfo.put(String.valueOf(playerinfo.getId()), up.getScore());
                        }

                        if (3 == roomType) {
                            JSONObject object = new JSONObject();
                            object.put("id", ((Playerinfo)room.getPlayerMap().get(account)).getId());
                            object.put("gid", room.getGid());
                            object.put("roomNo", room.getRoomNo());
                            object.put("type", room.getRoomType());
                            object.put("fen", ((SGUserPacket)room.getUserPacketMap().get(account)).getScore());
                            object.put("old", Dto.sub(((Playerinfo)((GameRoom)RoomManage.gameRoomMap.get(room.getRoomNo())).getPlayerMap().get(account)).getScore(), ((SGUserPacket)room.getUserPacketMap().get(account)).getScore()));
                            if (((Playerinfo)((GameRoom)RoomManage.gameRoomMap.get(room.getRoomNo())).getPlayerMap().get(account)).getScore() < 0.0D) {
                                object.put("new", 0);
                            } else {
                                object.put("new", ((Playerinfo)((GameRoom)RoomManage.gameRoomMap.get(room.getRoomNo())).getPlayerMap().get(account)).getScore());
                            }

                            userDeductionData.add(object);
                        }
                    }
                }
            }
        }
    }

    private void userPumpingFee(String roomNo) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room != null) {
            int gameStatus = room.getGameStatus();
            if (6 == gameStatus || 5 == gameStatus) {
                String cutType = room.getCutType();
                int roomType = room.getRoomType();
                if (8 == roomType || 9 == roomType || 10 == roomType || 0 == roomType) {
                    if (9 != roomType || this.isAgreeClose(roomNo) || !"1".equals(cutType) && !"2".equals(cutType) && !"4".equals(cutType) && !"5".equals(cutType) || room.getGameIndex() >= room.getGameCount()) {
                        Map<String, Double> pumpInfo = new HashMap();
                        double maxScore = 0.0D;
                        SGUserPacket up;
                        Playerinfo playerinfo;
                        Iterator var11;
                        String account;
                        if ("1".equals(cutType)) {
                            if (9 == roomType) {
                                var11 = room.getPlayerMap().keySet().iterator();

                                while(var11.hasNext()) {
                                    account = (String)var11.next();
                                    playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                                    if (playerinfo != null && playerinfo.getScore() > maxScore) {
                                        maxScore = playerinfo.getScore();
                                    }
                                }
                            } else {
                                var11 = room.getUserPacketMap().keySet().iterator();

                                while(var11.hasNext()) {
                                    account = (String)var11.next();
                                    up = (SGUserPacket)room.getUserPacketMap().get(account);
                                    if (up != null && up.getScore() > maxScore) {
                                        maxScore = up.getScore();
                                    }
                                }
                            }
                        }

                        var11 = room.getPlayerMap().keySet().iterator();

                        while(true) {
                            double deductScore;
                            do {
                                do {
                                    do {
                                        do {
                                            do {
                                                if (!var11.hasNext()) {
                                                    if (pumpInfo.size() > 0) {
                                                        JSONObject object = new JSONObject();
                                                        object.put("circleId", room.getCircleId());
                                                        object.put("roomNo", room.getRoomNo());
                                                        object.put("gameId", room.getGid());
                                                        object.put("pumpInfo", pumpInfo);
                                                        object.put("cutType", room.getCutType());
                                                        object.put("changeType", "3");
                                                        this.gameCircleService.circleUserPumping(object);
                                                    }

                                                    return;
                                                }

                                                account = (String)var11.next();
                                                deductScore = 0.0D;
                                                up = (SGUserPacket)room.getUserPacketMap().get(account);
                                                playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                                            } while(up == null);
                                        } while(playerinfo == null);
                                    } while(playerinfo.getScore() < 0.0D);
                                } while(0 == up.getStatus());
                            } while(1 == up.getStatus());

                            if (!"1".equals(cutType)) {
                                if ("2".equals(cutType)) {
                                    if (8 == roomType) {
                                        if (up.getScore() > 0.0D) {
                                            deductScore = Dto.mul(room.getPump() / 100.0D, up.getScore());
                                        }
                                    } else if (playerinfo.getScore() > 0.0D) {
                                        deductScore = Dto.mul(room.getPump() / 100.0D, playerinfo.getScore());
                                    }
                                } else if ("3".equals(cutType)) {
                                    deductScore = Dto.mul(room.getPump() / 100.0D, (double)room.getScore());
                                }
                            } else if (8 == roomType && up.getScore() == maxScore || 9 == roomType && playerinfo.getScore() == maxScore) {
                                deductScore = Dto.mul(room.getPump() / 100.0D, maxScore);
                            }

                            if (deductScore > 0.0D) {
                                deductScore = Math.ceil(deductScore);
                                up.setScore(Dto.sub(up.getScore(), deductScore));
                                playerinfo.setScore(Dto.sub(playerinfo.getScore(), deductScore));
                                pumpInfo.put(String.valueOf(playerinfo.getId()), -deductScore);
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateRoomCard(String roomNo) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        String payType = room.getPayType();
        JSONArray array = new JSONArray();
        int roomCardCount = 0;
        Iterator var6 = room.getUserPacketMap().keySet().iterator();

        while(var6.hasNext()) {
            String account = (String)var6.next();
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                if ("0".equals(payType) && account.equals(room.getOwner()) && ((Playerinfo)room.getPlayerMap().get(account)).getPlayTimes() == 1) {
                    roomCardCount = room.getPlayerCount() * room.getSinglePayNum();
                    array.add(((Playerinfo)room.getPlayerMap().get(account)).getId());
                }

                if ("1".equals(payType) && ((Playerinfo)room.getPlayerMap().get(account)).getPlayTimes() == 1) {
                    array.add(((Playerinfo)room.getPlayerMap().get(account)).getId());
                    roomCardCount = room.getSinglePayNum();
                }
            }
        }

        if (array.size() > 0) {
            this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("pump", room.getRoomCardChangeObject(array, roomCardCount)));
        }

    }

    private void scorePoints(String roomNo) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room != null) {
            List<SGUserPacket> upList = new ArrayList();
            Iterator var5 = room.getPlayerMap().keySet().iterator();

            while(var5.hasNext()) {
                String account = (String)var5.next();
                SGUserPacket up = (SGUserPacket)room.getUserPacketMap().get(account);
                if (up != null && 0 != up.getStatus() && 1 != up.getStatus()) {
                    upList.add(up);
                }
            }

            Collections.sort(upList, SGCard.upDesc);
            int playType = room.getPlayType();
            if (1 == playType) {
                this.mpqzScorePoints(roomNo, upList);
            } else if (2 == playType) {
                this.dcxScorePoints(roomNo, upList);
            }

        }
    }

    private void mpqzScorePoints(String roomNo, List<SGUserPacket> upList) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room != null || 1 == room.getPlayType()) {
            int roomType = room.getRoomType();
            String banker = room.getBanker();
            Map<Integer, Integer> doubleMap = room.getDoubleMap();
            int robMultiple = room.getRobMultiple();
            double scoreBanker = 0.0D;
            boolean isWin = true;
            List<String> winUserList = new ArrayList();
            List<String> loserUserList = new ArrayList();
            int bolus = room.getBolus();
            List<Integer> anteList = room.getAnteList();
            int bolusScore = (Integer)anteList.get(anteList.size() - 1) * bolus;
            Map<String, Integer> playerWinScore = new HashMap();

            for(int i = 0; i < upList.size(); ++i) {
                SGUserPacket up = (SGUserPacket)upList.get(i);
                if (banker.equals(up.getAccount())) {
                    isWin = false;
                } else {
                    int bet = up.getBet();
                    int cardMultiple;
                    int cardType;
                    int cardCount;
                    if (isWin) {
                        cardType = up.getCardType();
                        if (20 < cardType) {
                            cardMultiple = doubleMap.get(cardType) != null ? (Integer)doubleMap.get(cardType) : 1;
                        } else {
                            cardCount = up.getCardCount();
                            cardMultiple = doubleMap.get(cardCount) != null ? (Integer)doubleMap.get(cardCount) : 1;
                        }

                        winUserList.add(up.getAccount());
                    } else {
                        cardType = ((SGUserPacket)room.getUserPacketMap().get(banker)).getCardType();
                        if (20 < cardType) {
                            cardMultiple = doubleMap.get(cardType) != null ? (Integer)doubleMap.get(cardType) : 1;
                        } else {
                            cardCount = ((SGUserPacket)room.getUserPacketMap().get(banker)).getCardCount();
                            cardMultiple = doubleMap.get(cardCount) != null ? (Integer)doubleMap.get(cardCount) : 1;
                        }

                        loserUserList.add(up.getAccount());
                    }

                    double score = Dto.mul((double)bet, Dto.mul((double)robMultiple, (double)cardMultiple));
                    if (8 == roomType || 9 == roomType || 3 == roomType) {
                        double playerScore = ((Playerinfo)room.getPlayerMap().get(up.getAccount())).getScore();
                        if (3 != roomType) {
                            playerScore += ((Playerinfo)room.getPlayerMap().get(up.getAccount())).getSourceScore();
                        }

                        if (playerScore < score) {
                            score = playerScore;
                        }
                    }

                    if (!isWin) {
                        ((SGUserPacket)room.getUserPacketMap().get(up.getAccount())).setScore(-score);
                        ((Playerinfo)room.getPlayerMap().get(up.getAccount())).setScore(Dto.add(((Playerinfo)room.getPlayerMap().get(up.getAccount())).getScore(), -score));
                        scoreBanker = Dto.add(scoreBanker, score);
                    } else {
                        ((SGUserPacket)room.getUserPacketMap().get(up.getAccount())).setScore(score);
                        ((Playerinfo)room.getPlayerMap().get(up.getAccount())).setScore(Dto.add(((Playerinfo)room.getPlayerMap().get(up.getAccount())).getScore(), score));
                        if (bolusScore > 0 && (room.getPlayerWinScore() == null || room.getPlayerWinScore().get(up.getAccount()) == null)) {
                            playerWinScore.put(up.getAccount(), (int)score);
                        }

                        scoreBanker = Dto.add(scoreBanker, -score);
                    }
                }
            }

            room.setPlayerWinScore(playerWinScore);
            if (8 == roomType || 9 == roomType || 3 == roomType) {
                double playerScore = ((Playerinfo)room.getPlayerMap().get(banker)).getScore();
                if (3 != roomType) {
                    playerScore += ((Playerinfo)room.getPlayerMap().get(banker)).getSourceScore();
                }

                double moreScore;
                double meanScore;
                String account;
                double s;
                int i;
                if (playerScore < scoreBanker && scoreBanker > 0.0D) {
                    scoreBanker = playerScore;
                    moreScore = playerScore - playerScore;
                    meanScore = Dto.div(moreScore, (double)loserUserList.size(), 0);

                    for(i = 0; i < loserUserList.size(); ++i) {
                        account = (String)loserUserList.get(i);
                        if (moreScore < meanScore) {
                            s = moreScore;
                        } else {
                            s = meanScore;
                        }

                        moreScore -= meanScore;
                        ((SGUserPacket)room.getUserPacketMap().get(account)).setScore(Dto.add(((SGUserPacket)room.getUserPacketMap().get(account)).getScore(), s));
                        ((Playerinfo)room.getPlayerMap().get(account)).setScore(Dto.add(((Playerinfo)room.getPlayerMap().get(account)).getScore(), s));
                        if (moreScore <= 0.0D) {
                            break;
                        }
                    }
                } else if (playerScore < -scoreBanker && scoreBanker < 0.0D) {
                    scoreBanker = -playerScore;
                    moreScore = -scoreBanker - playerScore;
                    meanScore = Dto.div(moreScore, (double)winUserList.size(), 0);

                    for(i = 0; i < winUserList.size(); ++i) {
                        account = (String)winUserList.get(i);
                        if (moreScore < meanScore) {
                            s = moreScore;
                        } else {
                            s = meanScore;
                        }

                        moreScore -= meanScore;
                        ((SGUserPacket)room.getUserPacketMap().get(account)).setScore(Dto.add(((SGUserPacket)room.getUserPacketMap().get(account)).getScore(), -s));
                        ((Playerinfo)room.getPlayerMap().get(account)).setScore(Dto.add(((Playerinfo)room.getPlayerMap().get(account)).getScore(), -s));
                        if (moreScore <= 0.0D) {
                            break;
                        }
                    }
                }
            }

            ((SGUserPacket)room.getUserPacketMap().get(banker)).setScore(scoreBanker);
            ((Playerinfo)room.getPlayerMap().get(banker)).setScore(Dto.add(((Playerinfo)room.getPlayerMap().get(banker)).getScore(), scoreBanker));
        }
    }

    private void dcxScorePoints(String roomNo, List<SGUserPacket> upList) {
        SGGameRoom room = (SGGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room != null || 2 == room.getPlayType()) {
            Map<String, Integer> userBet = new HashMap();
            Map<String, Integer> userWinBet = new HashMap();

            SGUserPacket up;
            for(int i = 0; i < upList.size(); ++i) {
                up = (SGUserPacket)upList.get(i);
                userBet.put(up.getAccount(), up.getBet());
                userWinBet.put(up.getAccount(), up.getBet());
            }

            for(int i = 0; i < upList.size(); ++i) {
                SGUserPacket up1 = (SGUserPacket)upList.get(i);
                int bet1 = (Integer)userBet.get(up1.getAccount());
                int bet3 = bet1 * 2;

                for(int j = upList.size() - 1; j >= 0; --j) {
                    SGUserPacket up2 = (SGUserPacket)upList.get(j);
                    if (up1.getAccount().equals(up2.getAccount())) {
                        break;
                    }

                    int bet2 = (Integer)userWinBet.get(up2.getAccount());
                    if (bet2 > 0) {
                        if ((Integer)userWinBet.get(up1.getAccount()) >= bet3) {
                            break;
                        }

                        if (bet1 <= bet2) {
                            userWinBet.put(up1.getAccount(), bet3);
                            userWinBet.put(up2.getAccount(), bet2 - bet1);
                            break;
                        }

                        userWinBet.put(up1.getAccount(), (Integer)userWinBet.get(up1.getAccount()) + bet2);
                        userWinBet.put(up2.getAccount(), 0);
                        bet1 -= bet2;
                    }
                }
            }

            Iterator var15 = userWinBet.keySet().iterator();

            while(var15.hasNext()) {
                String account = (String)var15.next();
                up = (SGUserPacket)room.getUserPacketMap().get(account);
                Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                double score = (double)((Integer)userWinBet.get(account) - (Integer)userBet.get(account));
                if (score != 0.0D) {
                    up.setScore(score);
                    playerinfo.setScore(Dto.add(playerinfo.getScore(), score));
                }
            }

        }
    }
}
