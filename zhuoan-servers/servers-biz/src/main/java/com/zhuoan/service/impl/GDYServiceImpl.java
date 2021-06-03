//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.zhuoan.service.impl;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.gdy.GDYColor;
import com.zhuoan.biz.core.gdy.GDYNum;
import com.zhuoan.biz.event.BaseEventDeal;
import com.zhuoan.biz.event.gdy.GDYGameTimer;
import com.zhuoan.biz.game.biz.GameCircleService;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.gdy.GDYGameRoom;
import com.zhuoan.biz.model.gdy.GDYPacker;
import com.zhuoan.biz.model.gdy.GDYUserPacket;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.GDYService;
import com.zhuoan.service.RedisInfoService;
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
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Resource;
import javax.jms.Destination;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GDYServiceImpl implements GDYService {
    private final Logger log = LoggerFactory.getLogger(GDYServiceImpl.class);
    @Resource
    private GDYGameTimer gdyGameTimer;
    @Resource
    private ProducerService producerService;
    @Resource
    private Destination daoQueueDestination;
    @Resource
    private Destination gdyQueueDestination;
    @Resource
    private RedisInfoService redisInfoService;
    @Resource
    private GameCircleService gameCircleService;
    @Resource
    private RoomBiz roomBiz;
    @Resource
    private BaseEventDeal baseEventDeal;

    public GDYServiceImpl() {
    }

    public String nextPlayerAccount(String roomNo, String account) {
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
        Map<Long, String> map = new HashMap();
        Iterator var5 = room.getPlayerMap().keySet().iterator();

        while(var5.hasNext()) {
            String s = (String)var5.next();
            map.put(((Playerinfo)room.getPlayerMap().get(s)).getId(), s);
        }

        List<Long> userIdList = new ArrayList();

        int i;
        for(i = 0; i < room.getUserIdList().size(); ++i) {
            if ((Long)room.getUserIdList().get(i) > 0L) {
                GDYUserPacket up = (GDYUserPacket)room.getUserPacketMap().get(map.get(room.getUserIdList().get(i)));
                if (up != null && 0 != up.getStatus() && 1 != up.getStatus()) {
                    userIdList.add(room.getUserIdList().get(i));
                }
            }
        }

        for(i = 0; i < userIdList.size(); ++i) {
            if ((Long)userIdList.get(i) == ((Playerinfo)room.getPlayerMap().get(account)).getId()) {
                if (i + 1 == userIdList.size()) {
                    return (String)map.get(userIdList.get(0));
                }

                return (String)map.get(userIdList.get(i + 1));
            }
        }

        return null;
    }

    public List<GDYPacker> getSortCardList(List<String> paiList, boolean desc) {
        List<GDYPacker> packerList = new ArrayList();
        if (paiList != null && paiList.size() != 0) {
            try {
                Iterator var10 = paiList.iterator();

                while(var10.hasNext()) {
                    String s = (String)var10.next();
                    String[] pai = s.split("-");
                    int c = Integer.valueOf(pai[0]);
                    int n = Integer.valueOf(pai[1]);
                    GDYColor color = GDYColor.getGDYColor(c);
                    GDYNum num = GDYNum.getGDYNum(n);
                    GDYPacker packer = new GDYPacker(color, num);
                    packerList.add(packer);
                }
            } catch (Exception var12) {
                this.log.info("干瞪眼返回排序后的牌" + var12);
            }

            if (desc) {
                Collections.sort(packerList, GDYPacker.desc);
            } else {
                Collections.sort(packerList, GDYPacker.asc);
            }

            return packerList;
        } else {
            return packerList;
        }
    }

    public boolean checkPlayCard(String roomNo, String account, int cardType, List<GDYPacker> cardList, List<GDYPacker> jokerList) {return false;}

    public void shuffleDeing(String roomNo, boolean otherCard) {
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
        List<String> gamePlayerList = new ArrayList();
        ConcurrentMap<String, GDYUserPacket> userPacketMap = room.getUserPacketMap();
        Iterator var7 = userPacketMap.keySet().iterator();

        String banker;
        while(var7.hasNext()) {
            banker = (String)var7.next();
            GDYUserPacket up = (GDYUserPacket)userPacketMap.get(banker);
            if (up != null && up.getStatus() == 1) {
                gamePlayerList.add(banker);
            }
        }

        List<GDYPacker> cardList = this.shuffle();
        room.setTotalCardNumber(cardList.size());
        banker = room.getBanker();
        Map<String, List<GDYPacker>> listMap = new HashMap();

        ArrayList playerCard;
        Iterator var11;
        String account;
        for(var11 = gamePlayerList.iterator(); var11.hasNext(); listMap.put(account, playerCard)) {
            account = (String)var11.next();
            playerCard = new ArrayList();

            for(int i = 0; i < 5; ++i) {
                playerCard.add(cardList.get(0));
                cardList.remove(0);
            }

            if (banker.equals(account)) {
                playerCard.add(cardList.get(0));
                cardList.remove(0);
            }
        }

        var11 = listMap.keySet().iterator();

        while(var11.hasNext()) {
            account = (String)var11.next();
            ((GDYUserPacket)userPacketMap.get(account)).initMyCard((List)listMap.get(account));
        }

        int number = gamePlayerList.size();
        if (number < 2) {
            number = 2;
        }

        if (otherCard) {
            cardList.add(new GDYPacker(GDYColor.JOKER, GDYNum.P_RED_JOKER));
            if (number > 5) {
                cardList.add(new GDYPacker(GDYColor.JOKER, GDYNum.P_BLACK_JOKER));
            }

            if (number > 6) {
                cardList.add(new GDYPacker(GDYColor.JOKER, GDYNum.P_RED_JOKER));
            }

            Collections.shuffle(cardList);
        }

        room.setLeftCard(cardList);
    }

    private List<GDYPacker> shuffle() {
        List<GDYPacker> pai = new ArrayList();
        GDYColor[] var3 = GDYColor.values();
        int var4 = var3.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            GDYColor color = var3[var5];
            if (color.getColor() != GDYColor.JOKER.getColor()) {
                GDYNum[] var7 = GDYNum.values();
                int var8 = var7.length;

                for(int var9 = 0; var9 < var8; ++var9) {
                    GDYNum num = var7[var9];
                    if (num.getNum() <= GDYNum.P_2.getNum()) {
                        GDYPacker packer = new GDYPacker(color, num);
                        pai.add(packer);
                    }
                }
            }
        }

        pai.add(new GDYPacker(GDYColor.JOKER, GDYNum.P_RED_JOKER));
        pai.add(new GDYPacker(GDYColor.JOKER, GDYNum.P_BLACK_JOKER));
        Collections.shuffle(pai);
        return pai;
    }

    public void sendReadyTimerToAll(List<UUID> uuidList, int index, int time) {
        JSONObject result = new JSONObject();
        result.put("index", index);
        result.put("time", time);
        CommonConstant.sendMsgEventAll(uuidList, result, "readyTimerPush_GDY");
    }

    public int getStartIndex(String roomNo) {
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room == null) {
            return -1;
        } else {
            int roomType = room.getRoomType();
            if (room.getPlayerMap().get(room.getOwner()) != null) {
                return ((Playerinfo)room.getPlayerMap().get(room.getOwner())).getMyIndex();
            } else if (room.getPlayerMap().get(room.getBanker()) != null) {
                return ((Playerinfo)room.getPlayerMap().get(room.getBanker())).getMyIndex();
            } else {
                Iterator var4 = room.getPlayerMap().keySet().iterator();
                if (var4.hasNext()) {
                    String account = (String)var4.next();
                    return ((Playerinfo)room.getPlayerMap().get(account)).getMyIndex();
                } else {
                    return -1;
                }
            }
        }
    }

    public void exitRoomByPlayer(String roomNo, long userId, String account) {
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);

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

    public void clearRoomInfo(String roomNo) {
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room != null) {
            room.initGame();
            room.setGameIndex(0);
            room.setGameNewIndex(0);
            room.getSummaryData().clear();
            room.setGameStatus(0);
            room.setJieSanTime(0);
            room.getFinalSummaryData().clear();
            room.setUserPacketMap(new ConcurrentHashMap());
            room.setPlayerMap(new ConcurrentHashMap());
            room.setVisitPlayerMap(new ConcurrentHashMap());

            for(int i = 0; i < room.getUserIdList().size(); ++i) {
                room.getUserIdList().set(i, 0L);
                room.addIndexList(i);
            }

            this.baseEventDeal.reloadClearRoom(roomNo);
        }
    }

    private boolean circleFKDeduct(String roomNo) {return false;}

    public void gameSetZhuang(final String roomNo) {
        if (this.isAllReady(roomNo)) {
            GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
            int roomType = room.getRoomType();
            if (8 != roomType && 9 != roomType || this.circleFKDeduct(roomNo)) {
                room.setGameStatus(2);
                int timeLeft = 0;
                if (room.getGameIndex() == 0) {
                    timeLeft = 0;
                }

                room.setTimeLeft(timeLeft);
                List<String> zhuangList = new ArrayList();
                if (room.getLastOperateAccount() != null && !"-1".equals(room.getLastOperateAccount())) {
                    zhuangList.add(room.getLastOperateAccount());
                } else {
                    Iterator var6 = room.getUserPacketMap().keySet().iterator();

                    while(var6.hasNext()) {
                        String account = (String)var6.next();
                        if (((GDYUserPacket)room.getUserPacketMap().get(account)).getStatus() == 1) {
                            zhuangList.add(account);
                        }
                    }
                }

                if (zhuangList != null && zhuangList.size() != 0) {
                    if (zhuangList.size() == 1) {
                        room.setBanker((String)zhuangList.get(0));
                    } else {
                        int bankerIndex = RandomUtils.nextInt(zhuangList.size());
                        room.setBanker((String)zhuangList.get(bankerIndex));
                    }

                    JSONObject result = new JSONObject();
                    result.put("gameStatus", room.getGameStatus());
                    result.put("time", room.getTimeLeft());
                    result.put("zhuang", ((Playerinfo)room.getPlayerMap().get(room.getBanker())).getMyIndex());
                    CommonConstant.sendMsgEventAll(room.getAllUUIDList(), result, "changeGameStatusPush_GDY");
                    ThreadPoolHelper.executorService.submit(new Runnable() {
                        public void run() {
                            GDYServiceImpl.this.gdyGameTimer.gameSetZhuangOverTime(roomNo);
                        }
                    });
                }
            }
        }
    }

    public void startGame(final String roomNo) {
        this.redisInfoService.insertSummary(roomNo, "_GDY");
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (null != room) {
            if (2 == room.getGameStatus()) {
                room.initGame();
                int roomType = room.getRoomType();
                JSONObject roomInfo = new JSONObject();
                roomInfo.put("roomId", room.getId());
                this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("update_room_index", roomInfo));
                if (room.getFee() > 0.0D && 8 != roomType && 9 != roomType) {
                    JSONArray array = new JSONArray();
                    Map<String, Double> map = new HashMap();
                    Iterator var8 = room.getPlayerMap().keySet().iterator();

                    while(var8.hasNext()) {
                        String account = (String)var8.next();
                        Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                        if (playerinfo != null && playerinfo.getStatus() == 1) {
                            playerinfo.setScore(Dto.sub(playerinfo.getScore(), room.getFee()));
                            if (playerinfo.getScore() < 0.0D) {
                                playerinfo.setScore(0.0D);
                            }

                            map.put(((Playerinfo)room.getPlayerMap().get(account)).getOpenId(), -room.getFee());
                            array.add(playerinfo.getId());
                        }
                    }

                    this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("pump", room.getJsonObject(array)));
                }

                this.shuffleDeing(roomNo, true);
                room.setGameStatus(5);
                int bankerIndex = ((Playerinfo)room.getPlayerMap().get(room.getBanker())).getMyIndex();
                final String banker = room.getBanker();
                room.setLastOperateAccount(banker);
                room.setLastOperateIndex(bankerIndex);
                room.setFocusAccount(banker);
                room.setFocusIndex(bankerIndex);
                room.setLastCard(new ArrayList());
                room.setLastCardType(0);
                room.setPlay(true);
                room.setTimeLeft(10);
                Iterator var17 = room.getPlayerMap().keySet().iterator();

                while(var17.hasNext()) {
                    String account = (String)var17.next();
                    GDYUserPacket up = (GDYUserPacket)room.getUserPacketMap().get(account);
                    Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                    if (up != null && playerinfo != null) {
                        if (account.equals(banker)) {
                            up.setStatus(4);
                        } else {
                            up.setStatus(3);
                        }

                        JSONObject object = new JSONObject();
                        object.put("data", this.playRoomData(roomNo, account));
                        object.put("code", 1);
                        SocketIOClient client = GameMain.server.getClient(playerinfo.getUuid());
                        CommonConstant.sendMsgEvent(client, object, "gameEventPush_GDY");
                    }
                }

                final int processNum = room.getCardProcessNum();
                ThreadPoolHelper.executorService.submit(new Runnable() {
                    public void run() {
                        GDYServiceImpl.this.gdyGameTimer.gamePlayOverTime(roomNo, 10, banker, processNum);
                    }
                });
            }
        }
    }

    public JSONArray obtainAllPlayer(String roomNo) {
        JSONArray array = new JSONArray();
        Iterator var3 = this.obtainAllPlayerAccount(roomNo, true).iterator();

        while(var3.hasNext()) {
            String account = (String)var3.next();
            JSONObject playerInfo = this.obtainPlayerInfo(roomNo, account);
            if (!Dto.isObjNull(playerInfo)) {
                array.add(playerInfo);
            }
        }

        return array;
    }

    public List<String> obtainAllPlayerAccount(String roomNo, boolean isAll) {
        List<String> accountList = new ArrayList();
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room == null) {
            return accountList;
        } else {
            Iterator var5 = room.getUserPacketMap().keySet().iterator();

            while(true) {
                String account;
                do {
                    do {
                        do {
                            do {
                                do {
                                    if (!var5.hasNext()) {
                                        return accountList;
                                    }

                                    account = (String)var5.next();
                                } while(!room.getUserPacketMap().containsKey(account));
                            } while(room.getUserPacketMap().get(account) == null);
                        } while(!room.getPlayerMap().containsKey(account));
                    } while(room.getPlayerMap().get(account) == null);
                } while(!isAll && ((GDYUserPacket)room.getUserPacketMap().get(account)).getStatus() == 0);

                accountList.add(account);
            }
        }
    }

    public JSONObject obtainPlayerInfo(String roomNo, String account) {
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
        JSONObject obj = new JSONObject();
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
            GDYUserPacket up = (GDYUserPacket)room.getUserPacketMap().get(account);
            if (up != null) {
                obj.put("userStatus", up.getStatus());
                obj.put("timeLeft", up.getTimeLeft());
            }

            return obj;
        }
    }

    public JSONObject playRoomData(String roomNo, String account) {
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
        JSONObject obj = new JSONObject();
        obj.put("gameStatus", room.getGameStatus());
        obj.put("gameIndex", room.getGameNewIndex());
        int time = room.getTimeLeft();
        if (!room.isPlay() && room.getFocusIndex() != ((Playerinfo)room.getPlayerMap().get(account)).getMyIndex()) {
            time += 7;
        }

        if (time < 0) {
            time = 0;
        }

        obj.put("time", time);
        obj.put("leftCardNumber", room.getLeftCardNumber());
        obj.put("totalCardNumber", room.getTotalCardNumber());
        obj.put("focusIndex", room.getFocusIndex());
        obj.put("isPlay", room.isPlay());
        obj.put("lastCardList", room.getLastCardList());
        obj.put("lastCardType", room.getLastCardType());
        obj.put("lastOperateIndex", room.getLastOperateIndex());
        obj.put("isLastPlayCard", room.isLastPlayCard());
        obj.put("lastPlayIndex", room.getLastPlayIndex());
        obj.put("multiple", room.getMultiple());
        Map<String, String> fillCard = room.getFillCard();
        if (fillCard.get(account) != null) {
            obj.put("fillCard", fillCard.get(account));
        }

        obj.put("gameData", this.getGameData(roomNo, account));
        return obj;
    }

    public JSONObject obtainRoomData(String roomNo, String account) {
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
        int roomType = room.getRoomType();
        JSONObject obj = new JSONObject();
        obj.put("playerCount", room.getPlayerCount());
        obj.put("roomNo", roomNo);
        obj.put("gameIndex", room.getGameNewIndex());
        obj.put("gameCount", room.getGameCount());
        obj.put("gameStatus", room.getGameStatus());
        obj.put("roomType", room.getRoomType());
        obj.put("myIndex", ((Playerinfo)room.getPlayerMap().get(account)).getMyIndex());
        int time = room.getTimeLeft();
        if (5 == room.getGameStatus() && !room.isPlay() && room.getFocusIndex() != ((Playerinfo)room.getPlayerMap().get(account)).getMyIndex()) {
            time += 7;
        }

        if (time < 0) {
            time = 0;
        }

        obj.put("time", time);
        if (room.getClubCode() != null) {
            obj.put("clubCode", room.getClubCode());
        }

        obj.put("users", this.obtainAllPlayer(roomNo));
        obj.put("leftCardNumber", room.getLeftCardNumber());
        obj.put("totalCardNumber", room.getTotalCardNumber());
        obj.put("myPai", ((GDYUserPacket)room.getUserPacketMap().get(account)).getMyPai());
        obj.put("cardNum", ((GDYUserPacket)room.getUserPacketMap().get(account)).getCardNum());
        obj.put("focusIndex", room.getFocusIndex());
        obj.put("isPlay", room.isPlay());
        obj.put("lastCardList", room.getLastCardList());
        obj.put("lastCardType", room.getLastCardType());
        obj.put("lastOperateIndex", room.getLastOperateIndex());
        obj.put("isLastPlayCard", room.isLastPlayCard());
        obj.put("lastPlayIndex", room.getLastPlayIndex());
        obj.put("multiple", room.getMultiple());
        obj.put("zhuang", -1);
        if (room.getBanker() != null && room.getPlayerMap().get(room.getBanker()) != null) {
            obj.put("zhuang", ((Playerinfo)room.getPlayerMap().get(room.getBanker())).getMyIndex());
        }

        if (room.getSummaryData() != null && room.getSummaryData().containsKey("array")) {
            obj.put("summaryData", room.getSummaryData());
        }

        obj.put("isLiuJu", room.isLiuJu());
        obj.put("isEnd", 0);
        if (room.getFinalSummaryData() != null && room.getFinalSummaryData().size() > 0) {
            obj.put("isEnd", 1);
            obj.put("endData", room.getFinalSummaryData());
            obj.put("liuJuNum", room.getLiuJuNum());
        }

        if (0 != roomType && 8 != roomType && !"1".equals(room.getIsAllStart())) {
            obj.put("startIndex", this.getStartIndex(roomNo));
        } else {
            obj.put("startIndex", -1);
        }

        obj.put("gameData", this.getGameData(roomNo, account));
        obj.put("ante", room.getScore());
        obj.put("isClose", room.getIsClose());
        if (room.getIsClose() == -1) {
            JSONObject jieSan = new JSONObject();
            jieSan.put("array", this.getDissolveRoomInfo(roomNo));
            jieSan.put("time", room.getJieSanTime());
            obj.put("jieSan", jieSan);
        }

        return obj;
    }

    public void setSummaryData(String roomNo) {
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
        JSONObject obj = new JSONObject();
        JSONObject array = new JSONObject();
        Iterator var8 = room.getUserPacketMap().keySet().iterator();

        while(var8.hasNext()) {
            String account = (String)var8.next();
            GDYUserPacket up = (GDYUserPacket)room.getUserPacketMap().get(account);
            Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
            if (up != null && playerinfo != null && 0 != up.getStatus() && 1 != up.getStatus()) {
                JSONObject object = new JSONObject();
                object.put("score", up.getScore());
                object.put("name", playerinfo.getName());
                object.put("isWin", 0);
                if (up.getScore() > 0.0D) {
                    object.put("isWin", 1);
                }

                if (room.isLiuJu()) {
                    object.put("isSpring", false);
                } else {
                    object.put("isSpring", up.isSpring());
                }

                object.put("multiple", up.getMultiple());
                object.put("headimg", playerinfo.getRealHeadimg());
                array.put(((Playerinfo)room.getPlayerMap().get(account)).getMyIndex(), object);
            }
        }

        obj.put("array", array);
        room.setSummaryData(obj);
    }

    public void setFinalSummaryData(String roomNo) {
        JSONArray array = new JSONArray();
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
        Iterator var7 = room.getUserPacketMap().keySet().iterator();

        while(var7.hasNext()) {
            String account = (String)var7.next();
            JSONObject obj = new JSONObject();
            GDYUserPacket up = (GDYUserPacket)room.getUserPacketMap().get(account);
            Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
            if (up != null && playerinfo != null) {
                obj.put("account", account);
                obj.put("name", playerinfo.getName());
                obj.put("headimg", playerinfo.getRealHeadimg());
                obj.put("score", playerinfo.getScore());
                obj.put("winNum", up.getWinNum());
                array.add(obj);
            }
        }

        room.setFinalSummaryData(array);
    }

    public JSONObject getGameData(String roomNo, String account) {
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
        int gameStatus = room.getGameStatus();
        boolean isCardVisible = false;
        if (6 == gameStatus || 7 == gameStatus) {
            isCardVisible = true;
        }

        JSONObject data = new JSONObject();
        Iterator var10 = room.getPlayerMap().keySet().iterator();

        while(true) {
            GDYUserPacket up;
            Playerinfo playerinfo;
            String s;
            do {
                do {
                    if (!var10.hasNext()) {
                        if (6 == gameStatus) {
                            data.put("leftCard", room.getLeftCardList());
                        }

                        return data;
                    }

                    s = (String)var10.next();
                    up = (GDYUserPacket)room.getUserPacketMap().get(s);
                    playerinfo = (Playerinfo)room.getPlayerMap().get(s);
                } while(up == null);
            } while(playerinfo == null);

            JSONObject obj = new JSONObject();
            if (!isCardVisible && !account.equals(s)) {
                obj.put("pai", up.getMyBMPai());
            } else {
                obj.put("pai", up.getMyPai());
            }

            obj.put("cardNum", up.getCardNum());
            obj.put("index", playerinfo.getMyIndex());
            if (6 != gameStatus && 7 != gameStatus) {
                obj.put("trusteeStatus", up.isTrustee());
            } else {
                obj.put("trusteeStatus", false);
            }

            if (6 == gameStatus) {
                obj.put("sum", up.getScore());
                obj.put("scoreLeft", playerinfo.getScore());
            }

            data.put(playerinfo.getMyIndex(), obj);
        }
    }

    private String cardReplace(GDYPacker p) {
        return p.getColor().getColor() + "-" + p.getNum().getNum();
    }

    public List<List<String>> obtainAllCard(List<GDYPacker> lastCard, List<GDYPacker> myPai) {return null;}

    public boolean checkIsPlay(String roomNo) {
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
        String focusAccount = room.getFocusAccount();
        String lastOperateAccount = room.getLastOperateAccount();
        if (focusAccount.equals(lastOperateAccount)) {
            return true;
        } else {
            int lastCardType = room.getLastCardType();
            List<GDYPacker> lastCard = room.getLastCard();
            GDYUserPacket packet = (GDYUserPacket)room.getUserPacketMap().get(focusAccount);
            List<GDYPacker> myCard = packet.getMyCard();
            if (1 < lastCardType && myCard.size() == 1) {
                return false;
            } else {
                Map<Integer, Integer> card = new HashMap();
                List<GDYPacker> joker = new ArrayList();
                Iterator var11 = myCard.iterator();

                while(var11.hasNext()) {
                    GDYPacker packer = (GDYPacker)var11.next();
                    if (packer.getColor().getColor() == GDYColor.JOKER.getColor()) {
                        joker.add(packer);
                    } else {
                        card.put(packer.getNum().getNum(), card.get(packer.getNum().getNum()) == null ? 1 : (Integer)card.get(packer.getNum().getNum()) + 1);
                    }
                }

                if (card.isEmpty()) {
                    return false;
                } else {
                    Integer m;
                    if (lastCardType == 1) {
                        if (joker.size() >= 2) {
                            return true;
                        }

                        var11 = card.keySet().iterator();

                        while(var11.hasNext()) {
                            m = (Integer)var11.next();
                            if ((Integer)card.get(m) + joker.size() >= 3) {
                                return true;
                            }

                            if (((GDYPacker)lastCard.get(0)).getNum().getNum() < GDYNum.P_2.getNum()) {
                                if (m == GDYNum.P_2.getNum()) {
                                    return true;
                                }

                                if (((GDYPacker)lastCard.get(0)).getNum().getNum() == m - 1) {
                                    return true;
                                }
                            }
                        }
                    } else if (lastCardType == 2) {
                        if (joker.size() >= 2) {
                            return true;
                        }

                        var11 = card.keySet().iterator();

                        while(var11.hasNext()) {
                            m = (Integer)var11.next();
                            if ((Integer)card.get(m) + joker.size() >= 3) {
                                return true;
                            }

                            if (((GDYPacker)lastCard.get(0)).getNum().getNum() < GDYNum.P_2.getNum()) {
                                if (m == GDYNum.P_2.getNum()) {
                                    if ((Integer)card.get(m) >= 2) {
                                        return true;
                                    }

                                    if (joker.size() >= 1) {
                                        return true;
                                    }
                                } else if (((GDYPacker)lastCard.get(0)).getNum().getNum() == m - 1) {
                                    if ((Integer)card.get(m) >= 2) {
                                        return true;
                                    }

                                    if (joker.size() >= 1) {
                                        return true;
                                    }
                                }
                            }
                        }
                    } else {
                        if (lastCardType == 3) {
                            if (joker.size() >= 2) {
                                return true;
                            }

                            var11 = card.keySet().iterator();

                            do {
                                if (!var11.hasNext()) {
                                    int minNum = ((GDYPacker)lastCard.get(lastCard.size() - 1)).getNum().getNum();
                                    int b;
                                    if (joker.size() == 0) {
                                        for(b = 1; b <= lastCard.size(); ++b) {
                                            if (card.get(minNum + b) == null) {
                                                return false;
                                            }
                                        }

                                        return true;
                                    }

                                    b = 0;

                                    for(int i = 1; i <= lastCard.size(); ++i) {
                                        if (card.get(minNum + i) == null) {
                                            ++b;
                                        }

                                        if (b >= 2) {
                                            return false;
                                        }
                                    }

                                    return true;
                                }

                                m = (Integer)var11.next();
                            } while((Integer)card.get(m) + joker.size() < 3);

                            return true;
                        }

                        if (lastCardType == 4) {
                            if (joker.size() > lastCard.size()) {
                                return true;
                            }

                            var11 = card.keySet().iterator();

                            while(var11.hasNext()) {
                                m = (Integer)var11.next();
                                if ((Integer)card.get(m) + joker.size() > lastCard.size()) {
                                    return true;
                                }

                                if ((Integer)card.get(m) + joker.size() == lastCard.size() && ((GDYPacker)lastCard.get(0)).getNum().getNum() < GDYNum.P_2.getNum()) {
                                    if (m == GDYNum.P_2.getNum()) {
                                        return true;
                                    }

                                    if (((GDYPacker)lastCard.get(0)).getNum().getNum() < m) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }

                    return false;
                }
            }
        }
    }

    public void dissolveRoomInform(String roomNo, List<UUID> uuidList) {
        JSONObject object = new JSONObject();
        object.put("code", 1);
        object.put("type", 2);
        CommonConstant.sendMsgEventAll(uuidList, object, "closeRoomPush_GDY");
        this.redisInfoService.delSummary(roomNo, "_GDY");
    }

    public void settleAccounts(final String roomNo) {}

    private void saveGameLog(String roomNo) {
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (null != room && (-1 != room.getIsClose() || 6 == room.getGameStatus() || 7 == room.getGameStatus())) {
            JSONArray gameResult = new JSONArray();
            JSONArray array = new JSONArray();
            Iterator var8 = room.getUserPacketMap().keySet().iterator();

            JSONObject userResult;
            while(var8.hasNext()) {
                String account = (String)var8.next();
                GDYUserPacket up = (GDYUserPacket)room.getUserPacketMap().get(account);
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

    public void updateRoomCard(String roomNo) {
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
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

    private void updateUserScore(String roomNo) {
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room != null) {
            int gameStatus = room.getGameStatus();
            if (7 == gameStatus || 6 == gameStatus) {
                int roomType = room.getRoomType();
                JSONObject pumpInfo = new JSONObject();
                if (8 == roomType || 9 == roomType) {
                    Iterator var8 = room.getUserPacketMap().keySet().iterator();

                    while(var8.hasNext()) {
                        String account = (String)var8.next();
                        GDYUserPacket up = (GDYUserPacket)room.getUserPacketMap().get(account);
                        Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                        if (up != null && playerinfo != null && up.getScore() != 0.0D) {
                            pumpInfo.put(String.valueOf(playerinfo.getId()), up.getScore());
                        }
                    }

                    this.userPumpingFee(roomNo);
                }

                if (pumpInfo.size() > 0) {
                    JSONObject object = new JSONObject();
                    object.put("circleId", room.getCircleId());
                    object.put("roomNo", room.getRoomNo());
                    object.put("gameId", room.getGid());
                    object.put("pumpInfo", pumpInfo);
                    object.put("cutType", room.getCutType());
                    object.put("changeType", "2");
                    this.gameCircleService.circleUserPumping(object);
                }

            }
        }
    }

    private void userPumpingFee(String roomNo) {
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room != null) {
            int gameStatus = room.getGameStatus();
            if (7 == gameStatus || 6 == gameStatus) {
                String cutType = room.getCutType();
                int roomType = room.getRoomType();
                if (8 == roomType || 9 == roomType || 10 == roomType || 0 == roomType) {
                    if (9 != roomType || room.isAgreeClose() || !"1".equals(cutType) && !"2".equals(cutType) && !"4".equals(cutType) && !"5".equals(cutType) || room.getGameIndex() >= room.getGameCount()) {
                        Map<String, Double> pumpInfo = new HashMap();
                        double maxScore = 0.0D;
                        GDYUserPacket up;
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
                                    up = (GDYUserPacket)room.getUserPacketMap().get(account);
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
                                                up = (GDYUserPacket)room.getUserPacketMap().get(account);
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

    public void removeRoom(String roomNo) {
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (null != room) {
            this.redisInfoService.delSummary(roomNo, "_GDY");
            JSONObject roomInfo = new JSONObject();
            roomInfo.put("room_no", room.getRoomNo());
            roomInfo.put("status", 2);
            roomInfo.put("game_index", 0);
            this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("update_room_info", roomInfo));
            RoomManage.gameRoomMap.remove(roomNo);
        }
    }

    public JSONArray getDissolveRoomInfo(String roomNo) {
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        Iterator var7 = room.getUserPacketMap().keySet().iterator();

        while(var7.hasNext()) {
            String s = (String)var7.next();
            GDYUserPacket up = (GDYUserPacket)room.getUserPacketMap().get(s);
            Playerinfo player = (Playerinfo)room.getPlayerMap().get(s);
            if (up != null && player != null) {
                JSONObject obj = new JSONObject();
                obj.put("index", player.getMyIndex());
                obj.put("name", player.getName());
                obj.put("type", player.getIsCloseRoom());
                array.add(obj);
            }
        }

        return array;
    }

    public boolean isAllReady(String roomNo) {
        GDYGameRoom room = (GDYGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if (room == null) {
            return false;
        } else {
            Iterator var5 = room.getUserPacketMap().keySet().iterator();

            GDYUserPacket up;
            do {
                if (!var5.hasNext()) {
                    return true;
                }

                String account = (String)var5.next();
                up = (GDYUserPacket)room.getUserPacketMap().get(account);
                Playerinfo playerinfo = (Playerinfo)room.getPlayerMap().get(account);
                if (up == null || playerinfo == null) {
                    return false;
                }
            } while(1 == up.getStatus());

            return false;
        }
    }

    private List<List<String>> transformStringList(List<List<GDYPacker>> resLists) {
        List<List<String>> cards = new ArrayList();
        StringBuilder sb = new StringBuilder();
        Iterator var5 = resLists.iterator();

        while(var5.hasNext()) {
            List<GDYPacker> resList = (List)var5.next();
            List<String> card = new ArrayList();
            Iterator var7 = resList.iterator();

            while(var7.hasNext()) {
                GDYPacker packer = (GDYPacker)var7.next();
                sb.delete(0, sb.length());
                sb.append(packer.getColor().getColor()).append("-").append(packer.getNum().getNum());
                card.add(sb.toString());
            }

            cards.add(card);
        }

        return cards;
    }

    private List<List<GDYPacker>> obtainBoomCard(List<GDYPacker> lastCard, List<GDYPacker> myPai) {
        List<List<GDYPacker>> resLists = new ArrayList();

        for(int i = 0; i < this.getBoomPai(myPai).size(); ++i) {
            List<GDYPacker> packers = (List)this.getBoomPai(myPai).get(i);
            if (packers.size() > lastCard.size()) {
                resLists.add(packers);
            }

            if (packers.size() == lastCard.size() && ((GDYPacker)packers.get(0)).getNum().getNum() > ((GDYPacker)lastCard.get(0)).getNum().getNum()) {
                resLists.add(packers);
            }
        }

        return resLists;
    }

    private List<List<GDYPacker>> obtainFlushCard(List<GDYPacker> lastCard, List<GDYPacker> myPai) {
        List<List<GDYPacker>> resLists = new ArrayList();
        List<GDYPacker> kingCard = this.getKingCard(myPai);
        List<GDYNum> list = new ArrayList();
        Iterator var7 = lastCard.iterator();

        while(var7.hasNext()) {
            GDYPacker packer = (GDYPacker)var7.next();
            list.add(this.getNextNum(packer.getNum()));
        }

        if (GDYNum.P_A.equals(list.get(list.size() - 1))) {
            return resLists;
        } else {
            List<GDYNum> packerNum = this.getCardNum(myPai);
            List<GDYNum> sameList = new ArrayList(list);
            sameList.retainAll(packerNum);
            if (sameList.size() + kingCard.size() >= list.size()) {
                List<GDYPacker> resList = new ArrayList();
                Iterator var9 = myPai.iterator();

                label56:
                while(var9.hasNext()) {
                    GDYPacker aMyPai = (GDYPacker)var9.next();
                    Iterator var11 = sameList.iterator();

                    while(true) {
                        while(true) {
                            GDYNum aPackerNum;
                            do {
                                if (!var11.hasNext()) {
                                    continue label56;
                                }

                                aPackerNum = (GDYNum)var11.next();
                            } while(!aPackerNum.equals(aMyPai.getNum()));

                            if (resList.size() > 0 && !((GDYPacker)resList.get(resList.size() - 1)).getNum().equals(aMyPai.getNum())) {
                                resList.add(aMyPai);
                            } else if (resList.size() == 0) {
                                resList.add(aMyPai);
                            }
                        }
                    }
                }

                if (resList.size() < list.size()) {
                    list.removeAll(this.getCardNum(resList));
                    var9 = list.iterator();

                    while(var9.hasNext()) {
                        GDYNum num = (GDYNum)var9.next();
                        resList.add(new GDYPacker(GDYColor.JOKER, num));
                    }
                }

                resLists.add(this.sortByNum(resList));
            }

            return resLists;
        }
    }

    private List<List<GDYPacker>> obtainTwoCard(List<GDYPacker> lastCard, List<GDYPacker> myPai) {
        List<List<GDYPacker>> resLists = new ArrayList();
        List<GDYPacker> kingCard = this.getKingCard(myPai);
        GDYPacker gdyPacker = (GDYPacker)lastCard.get(0);
        if (GDYNum.P_2.equals(gdyPacker.getNum())) {
            return resLists;
        } else {
            GDYNum gdyNum = this.getNextNum(gdyPacker.getNum());
            if (gdyNum == null) {
                return resLists;
            } else {
                int nextCount = 0;
                GDYPacker nextPai = new GDYPacker();
                GDYPacker maxPai = new GDYPacker();
                int maxCount = 0;

                ArrayList resList;
                for(int i = 0; i < myPai.size(); ++i) {
                    GDYPacker packer = (GDYPacker)myPai.get(i);
                    if (GDYNum.P_2.getNum() == packer.getNum().getNum()) {
                        maxPai = packer;
                        ++maxCount;
                    } else if (gdyNum.getNum() == packer.getNum().getNum()) {
                        nextPai = packer;
                        ++nextCount;
                    }

                    if ((nextCount == 2 || maxCount == 2) && ((GDYPacker)myPai.get(i - 1)).getNum() == packer.getNum()) {
                        resList = new ArrayList();
                        resList.add(myPai.get(i - 1));
                        resList.add(packer);
                        resLists.add(resList);
                    }
                }

                if (kingCard.size() > 0) {
                    if (nextCount >= 1) {
                        resList = new ArrayList();
                        resList.add(nextPai);
                        resList.add(new GDYPacker(GDYColor.JOKER, nextPai.getNum()));
                        resLists.add(resList);
                    }

                    if (maxCount >= 1) {
                        resList = new ArrayList();
                        resList.add(maxPai);
                        resList.add(new GDYPacker(GDYColor.JOKER, maxPai.getNum()));
                        resLists.add(resList);
                    }
                }

                return resLists;
            }
        }
    }

    private List<List<GDYPacker>> obtainOneCard(List<GDYPacker> lastCard, List<GDYPacker> myPai) {
        GDYPacker gdyPacker = (GDYPacker)lastCard.get(0);
        List<List<GDYPacker>> resLists = new ArrayList();
        if (GDYNum.P_2.equals(gdyPacker.getNum())) {
            return resLists;
        } else {
            GDYNum gdyNum = this.getNextNum(gdyPacker.getNum());
            if (gdyNum == null) {
                return resLists;
            } else {
                Iterator var7 = myPai.iterator();

                while(var7.hasNext()) {
                    GDYPacker packer = (GDYPacker)var7.next();
                    ArrayList resList;
                    if (GDYNum.P_2.getNum() == packer.getNum().getNum()) {
                        resList = new ArrayList();
                        resList.add(packer);
                        resLists.add(resList);
                    } else if (gdyNum.getNum() == packer.getNum().getNum()) {
                        resList = new ArrayList();
                        resList.add(packer);
                        resLists.add(resList);
                    }
                }

                return resLists;
            }
        }
    }

    private List<GDYPacker> sortByNum(List<GDYPacker> resList) {
        int i;
        GDYPacker gdyPacker2;
        for(i = 1; i < resList.size(); ++i) {
            for(int j = 0; j < resList.size() - i; ++j) {
                if (((GDYPacker)resList.get(j)).getNum().getNum() > ((GDYPacker)resList.get(j + 1)).getNum().getNum()) {
                    gdyPacker2 = (GDYPacker)resList.get(j);
                    resList.set(j, resList.get(j + 1));
                    resList.set(j + 1, gdyPacker2);
                }
            }
        }

        for(i = 0; i < resList.size() - 1; ++i) {
            GDYPacker gdyPacker1 = (GDYPacker)resList.get(i);
            gdyPacker2 = (GDYPacker)resList.get(i + 1);
            if (gdyPacker1.getNum().equals(gdyPacker2.getNum()) && gdyPacker1.getColor().getColor() > gdyPacker2.getColor().getColor()) {
                resList.set(i + 1, gdyPacker1);
                resList.set(i, gdyPacker2);
            }
        }

        return resList;
    }

    private List<GDYPacker> getMaxCard(List<GDYPacker> myPai) {
        List<GDYPacker> packers = new ArrayList();
        Iterator var3 = myPai.iterator();

        while(var3.hasNext()) {
            GDYPacker gdyPacker = (GDYPacker)var3.next();
            if (GDYNum.P_2.equals(gdyPacker.getNum())) {
                packers.add(gdyPacker);
            }
        }

        return packers;
    }

    private List<GDYNum> getCardNum(List<GDYPacker> lastCard) {
        List<GDYNum> list = new ArrayList();
        Iterator var3 = lastCard.iterator();

        while(var3.hasNext()) {
            GDYPacker gdyPacker = (GDYPacker)var3.next();
            list.add(gdyPacker.getNum());
        }

        return list;
    }

    private GDYNum getNextNum(GDYNum num) {
        if (num.getNum() > GDYNum.P_A.getNum()) {
            return null;
        } else {
            GDYNum[] values = GDYNum.values();

            for(int i = 0; i < values.length - 1; ++i) {
                if (values[i].getNum() == num.getNum()) {
                    return values[i + 1];
                }
            }

            return null;
        }
    }

    private int getCardType(List<GDYPacker> lastCard) {
        if (lastCard.size() == 1) {
            return 1;
        } else if (lastCard.size() == 2) {
            return 2;
        } else {
            return ((GDYPacker)lastCard.get(0)).getNum().equals(((GDYPacker)lastCard.get(1)).getNum()) ? 4 : 3;
        }
    }

    public List<List<GDYPacker>> getBoomPai(List<GDYPacker> myPai) {
        List<List<GDYPacker>> lists = new ArrayList();
        List<List<GDYPacker>> myPaiList = this.getListByNum(myPai);
        List<GDYPacker> kingCard = this.getKingCard(myPai);
        Iterator var5 = myPaiList.iterator();

        while(true) {
            while(var5.hasNext()) {
                List<GDYPacker> list = (List)var5.next();
                if (list.size() >= 3 && kingCard.size() == 0) {
                    lists.add(list);
                } else if (kingCard.size() > 0 && list.size() + kingCard.size() >= 3 && list.size() > 0) {
                    for(int j = 0; j < kingCard.size(); ++j) {
                        list.add(new GDYPacker(GDYColor.JOKER, ((GDYPacker)list.get(0)).getNum()));
                    }

                    lists.add(list);
                }
            }

            return lists;
        }
    }

    private List<List<GDYPacker>> getListByNum(List<GDYPacker> myPai) {
        List<List<GDYPacker>> lists = new ArrayList();

        for(int i = 0; i < 13; ++i) {
            lists.add(new ArrayList());
        }

        Iterator var8 = myPai.iterator();

        while(var8.hasNext()) {
            GDYPacker gdyPacker = (GDYPacker)var8.next();
            GDYNum num = gdyPacker.getNum();
            GDYNum[] values = GDYNum.values();

            for(int i = 0; i < 13; ++i) {
                if (values[i].equals(num)) {
                    ((List)lists.get(i)).add(gdyPacker);
                }
            }
        }

        return lists;
    }

    private List<List<GDYPacker>> getTwoPai(List<GDYPacker> myPai) {
        List<List<GDYPacker>> lists = new ArrayList();
        List<GDYPacker> kingCard = this.getKingCard(myPai);

        for(int i = 0; i < myPai.size() - 1; ++i) {
            List<GDYPacker> list = new ArrayList();
            if (((GDYPacker)myPai.get(i)).getNum().equals(((GDYPacker)myPai.get(i + 1)).getNum()) && !((GDYPacker)myPai.get(i)).getColor().equals(GDYColor.JOKER)) {
                list.add(myPai.get(i));
                list.add(myPai.get(i + 1));
                lists.add(list);
            }
        }

        if (kingCard.size() > 0) {
            Iterator var7 = this.getOnePai(myPai).iterator();

            while(var7.hasNext()) {
                List<GDYPacker> gdyPackers = (List)var7.next();
                gdyPackers.add(new GDYPacker(GDYColor.JOKER, ((GDYPacker)gdyPackers.get(0)).getNum()));
                lists.add(gdyPackers);
            }
        }

        return lists;
    }

    private List<GDYPacker> getKingCard(List<GDYPacker> myPai) {
        List<GDYPacker> list = new ArrayList();
        Iterator var3 = myPai.iterator();

        while(true) {
            GDYPacker gdyPacker;
            do {
                if (!var3.hasNext()) {
                    return list;
                }

                gdyPacker = (GDYPacker)var3.next();
            } while(!GDYNum.P_BLACK_JOKER.equals(gdyPacker.getNum()) && !GDYNum.P_RED_JOKER.equals(gdyPacker.getNum()) && !GDYNum.P_OTHER_CARD.equals(gdyPacker.getNum()));

            list.add(gdyPacker);
        }
    }

    private List<List<GDYPacker>> getOnePai(List<GDYPacker> myPai) {
        List<List<GDYPacker>> lists = new ArrayList();
        Iterator var4 = myPai.iterator();

        while(var4.hasNext()) {
            GDYPacker gdyPacker = (GDYPacker)var4.next();
            if (!GDYNum.P_BLACK_JOKER.equals(gdyPacker.getNum()) && !GDYNum.P_RED_JOKER.equals(gdyPacker.getNum()) && !GDYNum.P_OTHER_CARD.equals(gdyPacker.getNum())) {
                List<GDYPacker> list = new ArrayList();
                list.add(gdyPacker);
                lists.add(list);
            }
        }

        return lists;
    }

    public static void main(String[] args) {
        Map<String, String> a = new HashMap();
        String s = (String)a.get((Object)null);
        System.out.println(s);
    }

    public static void tipTest() {
        List<GDYPacker> lastCard = new ArrayList();
        List<GDYPacker> myPai = new ArrayList();
        GDYPacker gdyPacker = new GDYPacker(GDYColor.FANGKUAI, GDYNum.P_2);
        myPai.add(gdyPacker);
        gdyPacker = new GDYPacker(GDYColor.HEITAO, GDYNum.P_2);
        myPai.add(gdyPacker);
        gdyPacker = new GDYPacker(GDYColor.HONGTAO, GDYNum.P_4);
        myPai.add(gdyPacker);
        gdyPacker = new GDYPacker(GDYColor.HEITAO, GDYNum.P_4);
        myPai.add(gdyPacker);
        gdyPacker = new GDYPacker(GDYColor.FANGKUAI, GDYNum.P_5);
        myPai.add(gdyPacker);
        gdyPacker = new GDYPacker(GDYColor.FANGKUAI, GDYNum.P_7);
        myPai.add(gdyPacker);
        gdyPacker = new GDYPacker(GDYColor.FANGKUAI, GDYNum.P_7);
        myPai.add(gdyPacker);
        gdyPacker = new GDYPacker(GDYColor.FANGKUAI, GDYNum.P_7);
        myPai.add(gdyPacker);
        gdyPacker = new GDYPacker(GDYColor.FANGKUAI, GDYNum.P_9);
        myPai.add(gdyPacker);

        for(int i = 0; i < 2; ++i) {
            gdyPacker = new GDYPacker(GDYColor.JOKER, GDYNum.P_OTHER_CARD);
            myPai.add(gdyPacker);
        }

        gdyPacker = new GDYPacker(GDYColor.FANGKUAI, GDYNum.P_3);
        lastCard.add(gdyPacker);
        gdyPacker = new GDYPacker(GDYColor.FANGKUAI, GDYNum.P_4);
        lastCard.add(gdyPacker);
        gdyPacker = new GDYPacker(GDYColor.FANGKUAI, GDYNum.P_5);
        lastCard.add(gdyPacker);
        new GDYPacker(GDYColor.FANGKUAI, GDYNum.P_6);
        GDYServiceImpl gdyService = new GDYServiceImpl();
        System.out.println(gdyService.obtainAllCard(new ArrayList(), myPai));
    }

    private void shuffleTest(int number, boolean otherCard) {
        List<GDYPacker> pai = this.shuffle();
        System.out.println("牌的数量：" + pai.size());
        Iterator var4 = pai.iterator();

        while(var4.hasNext()) {
            GDYPacker packer = (GDYPacker)var4.next();
            System.out.println(packer.getColor().getColor() + "-" + packer.getNum().getNum());
        }

        System.out.println("获取五张牌");
        System.out.println("未排序前");
        List<GDYPacker> list = new ArrayList();
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < 5; ++i) {
            sb.append(((GDYPacker)pai.get(i)).getColor().getColor()).append("-").append(((GDYPacker)pai.get(i)).getNum().getNum()).append("||");
            list.add(pai.get(i));
        }

        System.out.println(sb.toString());
        System.out.println("排序完");
        GDYUserPacket packet = new GDYUserPacket();
        packet.initMyCard(list);
        sb = new StringBuilder();
        Iterator var7 = packet.getMyCard().iterator();

        GDYPacker packer;
        while(var7.hasNext()) {
            packer = (GDYPacker)var7.next();
            sb.append(packer.getColor().getColor()).append("-").append(packer.getNum().getNum()).append("||");
        }

        System.out.println(sb.toString());
        System.out.println("加一张牌前");
        sb = new StringBuilder();
        var7 = packet.getMyCard().iterator();

        while(var7.hasNext()) {
            packer = (GDYPacker)var7.next();
            sb.append(packer.getColor().getColor()).append("-").append(packer.getNum().getNum()).append("||");
        }

        sb.append(((GDYPacker)pai.get(6)).getColor().getColor()).append("-").append(((GDYPacker)pai.get(6)).getNum().getNum()).append("||");
        System.out.println(sb.toString());
        System.out.println("加一张牌排序完");
        sb = new StringBuilder();
        packet.addMyCard((GDYPacker)pai.get(6));
        var7 = packet.getMyCard().iterator();

        while(var7.hasNext()) {
            packer = (GDYPacker)var7.next();
            sb.append(packer.getColor().getColor()).append("-").append(packer.getNum().getNum()).append("||");
        }

        System.out.println(sb.toString());
    }
}
