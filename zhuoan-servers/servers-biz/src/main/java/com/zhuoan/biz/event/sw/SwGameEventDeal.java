

package com.zhuoan.biz.event.sw;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.game.biz.PublicBiz;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.sw.SwGameRoom;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.util.Dto;
import com.zhuoan.util.TimeUtil;
import com.zhuoan.util.thread.ThreadPoolHelper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.*;

@Component
public class SwGameEventDeal {
    @Resource
    GameTimerSw gameTimerSw;
    @Resource
    private UserBiz userBiz;
    @Resource
    private PublicBiz publicBiz;
    @Resource
    private Destination daoQueueDestination;
    @Resource
    private ProducerService producerService;
    @Resource
    private RedisInfoService redisInfoService;

    public SwGameEventDeal() {
    }

    public void createRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String account = postData.getString("account");
        String roomNo = postData.getString("room_no");
        if (!postData.containsKey("is_join") && RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
            ((GameRoom) RoomManage.gameRoomMap.get(roomNo)).setTimeLeft(90);
        }

        JSONObject roomData = this.obtainRoomData(roomNo, account);
        if (!Dto.isObjNull(roomData)) {
            JSONObject result = new JSONObject();
            result.put("code", 1);
            result.put("data", roomData);
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "enterRoomPush_SW");
            if (!postData.containsKey("is_join")) {
                this.beginStartTimer(roomNo, account);
            }
        }

    }

    public void joinRoom(SocketIOClient client, Object data) {
        JSONObject joinData = JSONObject.fromObject(data);
        joinData.put("is_join", 1);
        this.createRoom(client, joinData);
        if (joinData.containsKey("isReconnect") && joinData.getInt("isReconnect") == 0) {
            String account = joinData.getString("account");
            SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(joinData.getString("room_no"));
            JSONObject result = new JSONObject();
            JSONObject user = this.obtainPlayerInfo(room.getRoomNo(), account);
            if (user.getInt("index") <= 18) {
                result.put("user", user);
            }

            result.put("playerCount", room.getPlayerMap().size());
            CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), String.valueOf(result), "playerEnterPush_SW");
        }

    }

    public void gameHide(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (CommonConstant.checkEvent(postData, 0, client) || CommonConstant.checkEvent(postData, 1, client) || CommonConstant.checkEvent(postData, 4, client)) {
            final String roomNo = postData.getString("room_no");
            SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
            String account = postData.getString("account");
            if (account.equals(room.getBanker())) {
                room.setGameStatus(6);
                room.setTimeLeft(60);
                this.changeGameStatus(roomNo);
                ThreadPoolHelper.executorService.submit(new Runnable() {
                    public void run() {
                        SwGameEventDeal.this.gameTimerSw.gameOverTime(roomNo, 60, 6);
                    }
                });
            }
        }
    }

    public void gameStart(SocketIOClient client, Object data) {
    }

    public void gameBet(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (CommonConstant.checkEvent(postData, 2, client)) {
            String roomNo = postData.getString("room_no");
            SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
            String account = postData.getString("account");
            if (!account.equals(room.getBanker())) {
                if (postData.containsKey("place") && postData.containsKey("value")) {
                    int place = postData.getInt("place");
                    int value = postData.getInt("value");
                    if (this.obtainBetList().contains(place)) {
                        if (value > 0 && value <= this.obtainMaxTimes(room.getBaseNum())) {
                            JSONObject result = new JSONObject();
                            if ((double) (value * room.getScore()) + room.getFee() > ((Playerinfo) room.getPlayerMap().get(account)).getScore()) {
                                result.put("code", 0);
                                result.put("msg", "余额不足");
                                CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "gameBetPush_SW");
                            } else {
                                boolean canBet = true;
                                int playerBetMax = 10;
                                if (room.getSetting().containsKey("playerBetMax")) {
                                    playerBetMax = room.getSetting().getInt("playerBetMax");
                                }

                                if (this.obtainTotalBetByAccountAndPlace(roomNo, account, place) + value > playerBetMax) {
                                    canBet = false;
                                }

                                if (room.getSingleMax() > 0 && this.obtainTotalBetByPlace(roomNo, place) + value > room.getSingleMax()) {
                                    canBet = false;
                                }

                                if ((double) (this.obtainTotalBetByPlace(roomNo, place) + value) * room.getRatio() * (double) room.getScore() > ((Playerinfo) room.getPlayerMap().get(room.getBanker())).getScore()) {
                                    canBet = false;
                                }

                                if (!canBet) {
                                    result.put("code", 0);
                                    result.put("msg", "已达下注上限");
                                    CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "gameBetPush_SW");
                                } else {
                                    this.addBetRecord(roomNo, account, place, value);
                                    this.changeUserScore(roomNo, account, (double) (-value * room.getScore()));
                                    result.put("code", 1);
                                    result.put("index", ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
                                    result.put("value", value);
                                    result.put("place", place);
                                    result.put("myScore", this.obtainTotalBetByAccountAndPlace(roomNo, account, place));
                                    result.put("totalScore", this.obtainTotalBetByPlace(roomNo, place));
                                    result.put("scoreLeft", ((Playerinfo) room.getPlayerMap().get(account)).getScore());
                                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "gameBetPush_SW");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void gameBeBanker(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (CommonConstant.checkEvent(postData, 5, client)) {
            String roomNo = postData.getString("room_no");
            SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
            String account = postData.getString("account");
            if (Dto.stringIsNULL(room.getBanker())) {
                if (room.getRoomType() != 3 && room.getRoomType() != 1 || ((Playerinfo) room.getPlayerMap().get(account)).getScore() >= room.getMinBankerScore()) {
                    room.setBanker(account);
                    if (((Playerinfo) room.getPlayerMap().get(account)).getMyIndex() < room.getUserIdList().size()) {
                        room.getUserIdList().set(((Playerinfo) room.getPlayerMap().get(account)).getMyIndex(), 0L);
                    }

                    ((Playerinfo) room.getPlayerMap().get(account)).setMyIndex(0);
                    room.getUserIdList().set(0, ((Playerinfo) room.getPlayerMap().get(account)).getId());
                    room.setGameStatus(1);
                    room.setTimeLeft(90);
                    this.changeGameStatus(roomNo);
                    this.beginStartTimer(roomNo, account);
                }
            }
        }
    }

    public void getUndoInfo(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (CommonConstant.checkEvent(postData, 2, client)) {
            String roomNo = postData.getString("room_no");
            SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
            String account = postData.getString("account");
            if (!account.equals(room.getBanker())) {
                JSONObject result = new JSONObject();
                Set<Integer> betArray = this.obtainMyBetArray(roomNo, account);
                if (betArray.size() == 0) {
                    result.put("code", 0);
                    result.put("msg", "当前未下注");
                } else {
                    result.put("code", 1);
                    result.put("betArray", betArray);
                }

                CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getUndoInfoPush_SW");
            }
        }
    }

    public void gameUndo(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (CommonConstant.checkEvent(postData, 2, client)) {
            String roomNo = postData.getString("room_no");
            SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
            String account = postData.getString("account");
            if (!account.equals(room.getBanker())) {
                int place = postData.getInt("place");
                if (this.obtainBetList().contains(place)) {
                    JSONObject result = new JSONObject();
                    double betScore = (double) this.obtainTotalBetByAccountAndPlace(roomNo, account, place);
                    if (betScore > 0.0D) {
                        this.removeBetRecord(roomNo, account, place);
                        this.changeUserScore(roomNo, account, betScore * (double) room.getScore());
                        result.put("code", 1);
                        result.put("place", place);
                        result.put("index", ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
                        result.put("myScore", this.obtainTotalBetByAccountAndPlace(roomNo, account, place));
                        result.put("totalScore", this.obtainTotalBetByPlace(roomNo, place));
                        result.put("scoreLeft", ((Playerinfo) room.getPlayerMap().get(account)).getScore());
                        result.put("betArray", this.obtainMyBetArray(roomNo, account));
                        CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "gameUndoPush_SW");
                    } else {
                        result.put("code", 0);
                        result.put("msg", "该子当前未下注");
                        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "gameUndoPush_SW");
                    }

                }
            }
        }
    }

    public void exitRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (CommonConstant.checkEvent(postData, -1, client)) {
            String roomNo = postData.getString("room_no");
            SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
            String account = postData.getString("account");
            boolean canExit = false;
            if (room.getRoomType() == 1 || room.getRoomType() == 3) {
                if (!account.equals(room.getBanker()) && Dto.isObjNull(this.obtainLastBetRecord(roomNo, account))) {
                    canExit = true;
                } else if (room.getGameStatus() == 0 || room.getGameStatus() == 1 || room.getGameStatus() == 4) {
                    canExit = true;
                }
            }

            Playerinfo player = (Playerinfo) room.getPlayerMap().get(account);
            if (canExit) {
                List<UUID> allUUIDList = room.getAllUUIDList();

                for (int i = 0; i < room.getUserIdList().size(); ++i) {
                    if ((Long) room.getUserIdList().get(i) == ((Playerinfo) room.getPlayerMap().get(account)).getId()) {
                        room.getUserIdList().set(i, 0L);
                        room.addIndexList(((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
                        break;
                    }
                }

                JSONObject roomInfo = new JSONObject();
                roomInfo.put("roomNo", room.getRoomNo());
                roomInfo.put("roomId", room.getId());
                roomInfo.put("userIndex", ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
                room.getPlayerMap().remove(account);
                room.getVisitPlayerMap().remove(account);
                roomInfo.put("player_number", room.getPlayerMap().size());
                this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("update_quit_user_room", roomInfo));
                JSONObject result = new JSONObject();
                result.put("code", 1);
                result.put("type", 1);
                result.put("index", player.getMyIndex());
                result.put("playerCount", room.getPlayerMap().size());
                if (!postData.containsKey("notSend")) {
                    CommonConstant.sendMsgEventToAll(allUUIDList, result.toString(), "exitRoomPush");
                }

                if (postData.containsKey("notSendToMe")) {
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "exitRoomPush");
                }

                if (room.getPlayerMap().size() == 0) {
                    this.redisInfoService.delSummary(roomNo, "_SW");
                    roomInfo.put("status", room.getIsClose());
                    roomInfo.put("game_index", room.getGameIndex());
                    if (8 != room.getRoomType() && 9 != room.getRoomType()) {
                        RoomManage.gameRoomMap.remove(room.getRoomNo());
                    }
                }

                if (account.equals(room.getBanker()) && room.getPlayerMap().size() > 0) {
                    room.setBanker((String) null);
                    this.choiceBanker(roomNo);
                }
            } else {
                JSONObject result = new JSONObject();
                result.put("code", 0);
                result.put("msg", "游戏已开始无法退出");
                result.put("type", 1);
                CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "exitRoomPush_SW");
            }

        }
    }

    public void gameChangeSeat(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (CommonConstant.checkEvent(postData, -1, client)) {
            if (postData.containsKey("index")) {
                int index = postData.getInt("index");
                String roomNo = postData.getString("room_no");
                SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
                String account = postData.getString("account");
                JSONObject result = new JSONObject();
                if (client != null && account.equals(room.getBanker())) {
                    result.put("code", 0);
                    result.put("msg", "庄家无法换座");
                    CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "gameChangeSeatPush_SW");
                } else if (room.getGameStatus() == 2) {
                    result.put("code", 0);
                    result.put("msg", "当前无法换座");
                    CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "gameChangeSeatPush_SW");
                } else if (!Dto.stringIsNULL(this.obtainUserAccountByIndex(roomNo, index))) {
                    result.put("code", 0);
                    result.put("msg", "该座位已被使用");
                    CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "gameChangeSeatPush_SW");
                } else {
                    result.put("code", 1);
                    result.put("lastIndex", ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
                    if (((Playerinfo) room.getPlayerMap().get(account)).getMyIndex() < room.getUserIdList().size()) {
                        room.getUserIdList().set(((Playerinfo) room.getPlayerMap().get(account)).getMyIndex(), 0L);
                    }

                    ((Playerinfo) room.getPlayerMap().get(account)).setMyIndex(index);
                    if (index >= 0 && index <= 18) {
                        room.getUserIdList().set(index, ((Playerinfo) room.getPlayerMap().get(account)).getId());
                    }

                    result.put("user", this.obtainPlayerInfo(roomNo, account));
                    Iterator var9 = room.getPlayerMap().keySet().iterator();

                    while (var9.hasNext()) {
                        String uuid = (String) var9.next();
                        if (room.getPlayerMap().containsKey(uuid) && room.getPlayerMap().get(uuid) != null) {
                            result.put("myIndex", ((Playerinfo) room.getPlayerMap().get(uuid)).getMyIndex());
                            CommonConstant.sendMsgEventToSingle(((Playerinfo) room.getPlayerMap().get(uuid)).getUuid(), String.valueOf(result), "gameChangeSeatPush_SW");
                        }
                    }

                }
            }
        }
    }

    public void reconnectGame(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (postData.containsKey("room_no") && postData.containsKey("account") && postData.containsKey("uuid")) {
            String roomNo = postData.getString("room_no");
            String account = postData.getString("account");
            JSONObject userInfo = this.userBiz.getUserByAccount(account);
            if (userInfo.containsKey("uuid") && !Dto.stringIsNULL(userInfo.getString("uuid")) && userInfo.getString("uuid").equals(postData.getString("uuid"))) {
                JSONObject result = new JSONObject();
                if (client != null) {
                    if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
                        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
                        if (!Dto.stringIsNULL(account) && room.getPlayerMap().containsKey(account) && room.getPlayerMap().get(account) != null) {
                            ((Playerinfo) room.getPlayerMap().get(account)).setUuid(client.getSessionId());
                            result.put("type", 1);
                            result.put("data", this.obtainRoomData(roomNo, account));
                            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush_SW");
                        } else {
                            result.put("type", 0);
                            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush_SW");
                        }
                    } else {
                        result.put("type", 0);
                        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush_SW");
                    }
                }
            }
        }
    }

    public void getHistory(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (CommonConstant.checkEvent(postData, -1, client)) {
            String roomNo = postData.getString("room_no");
            SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
            JSONObject result = new JSONObject();
            if (!Dto.stringIsNULL(room.getBanker())) {
                JSONObject bankerInfo = this.publicBiz.getUserGameInfo(room.getBanker());
                if (!Dto.isObjNull(bankerInfo)) {
                    result.put("array", bankerInfo.getJSONArray("treasure_history"));
                } else {
                    result.put("array", new JSONArray());
                }
            } else {
                result.put("array", new JSONArray());
            }

            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getHistoryPush_SW");
        }
    }

    public void getAllUser(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (CommonConstant.checkEvent(postData, -1, client)) {
            String roomNo = postData.getString("room_no");
            SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
            int pageIndex = postData.getInt("pageIndex");
            JSONObject result = new JSONObject();
            JSONArray array = new JSONArray();
            Iterator var9 = room.getPlayerMap().keySet().iterator();

            while (var9.hasNext()) {
                String account = (String) var9.next();
                if (room.getPlayerMap().containsKey(account) && room.getPlayerMap().get(account) != null) {
                    JSONObject obj = new JSONObject();
                    obj.put("name", ((Playerinfo) room.getPlayerMap().get(account)).getName());
                    obj.put("score", ((Playerinfo) room.getPlayerMap().get(account)).getScore());
                    obj.put("headimg", ((Playerinfo) room.getPlayerMap().get(account)).getRealHeadimg());
                    obj.put("isBanker", 0);
                    if (account.equals(room.getBanker())) {
                        obj.put("isBanker", 1);
                    }

                    array.add(obj);
                }
            }

            JSONArray users = new JSONArray();
            int beginIndex = pageIndex * 30;
            int endIndex = (pageIndex + 1) * 30;
            int i;
            if (array.size() > endIndex) {
                for (i = beginIndex; i < endIndex; ++i) {
                    users.add(array.getJSONObject(i));
                }
            } else if (array.size() > beginIndex) {
                for (i = beginIndex; i < array.size(); ++i) {
                    users.add(array.getJSONObject(i));
                }
            }

            result.put("array", users);
            result.put("pageIndex", pageIndex);
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getAllUsersPush_SW");
        }
    }

    public void betFinish(final String roomNo) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room.getGameStatus() == 2) {
            room.setGameStatus(3);
            boolean isBet = this.isBet(roomNo, room);
            if (room.getFee() > 0.0D && isBet) {
                JSONArray array = new JSONArray();
                Iterator var5 = room.getPlayerMap().keySet().iterator();

                label36:
                while (true) {
                    String account;
                    do {
                        do {
                            do {
                                if (!var5.hasNext()) {
                                    this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("pump", room.getJsonObject(array)));
                                    break label36;
                                }

                                account = (String) var5.next();
                            } while (!room.getPlayerMap().containsKey(account));
                        } while (room.getPlayerMap().get(account) == null);
                    } while (Dto.isObjNull(this.obtainLastBetRecord(roomNo, account)) && !account.equals(room.getBanker()));

                    this.changeUserScore(roomNo, account, -room.getFee());
                    array.add(((Playerinfo) room.getPlayerMap().get(account)).getId());
                }
            }

            room.setTimeLeft(10);
            this.changeGameStatus(roomNo);
            ThreadPoolHelper.executorService.submit(new Runnable() {
                public void run() {
                    SwGameEventDeal.this.gameTimerSw.gameOverTime(roomNo, 10, 3);
                }
            });
        }
    }

    private boolean isBet(String roomNo, SwGameRoom room) {
        Iterator var3 = room.getPlayerMap().keySet().iterator();

        String account;
        do {
            if (!var3.hasNext()) {
                return false;
            }

            account = (String) var3.next();
        } while (!room.getPlayerMap().containsKey(account) || room.getPlayerMap().get(account) == null || Dto.isObjNull(this.obtainLastBetRecord(roomNo, account)));

        return true;
    }

    public void summary(final String roomNo) {
    }

    public void updateBankerTreasureHistory(String banker, int treasure) {
        JSONObject bankerInfo = this.publicBiz.getUserGameInfo(banker);
        JSONObject obj = new JSONObject();
        JSONArray treasureHistory;
        if (!Dto.isObjNull(bankerInfo)) {
            obj.put("id", bankerInfo.getLong("id"));
            treasureHistory = bankerInfo.getJSONArray("treasure_history");
            treasureHistory.add(0, treasure);
            if (treasureHistory.size() > 30) {
                for (int i = 0; i < treasureHistory.size(); ++i) {
                    if (i >= 30) {
                        treasureHistory.remove(i);
                    }
                }
            }

            obj.put("treasure_history", treasureHistory);
            obj.put("update_time", TimeUtil.getNowDate());
        } else {
            treasureHistory = new JSONArray();
            treasureHistory.add(treasure);
            obj.put("treasure_history", treasureHistory);
            obj.put("account", banker);
            obj.put("update_time", TimeUtil.getNowDate());
        }

        this.publicBiz.addOrUpdateUserGameInfo(obj);
    }

    public void updateUserScore(String roomNo) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room != null) {
            JSONArray array = new JSONArray();

            for (int i = 0; i < room.getSummaryArray().size(); ++i) {
                String account = room.getSummaryArray().getJSONObject(i).getString("account");
                JSONObject obj = new JSONObject();
                obj.put("id", ((Playerinfo) room.getPlayerMap().get(account)).getId());
                obj.put("total", ((Playerinfo) room.getPlayerMap().get(account)).getScore());
                obj.put("fen", room.getSummaryArray().getJSONObject(i).getDouble("score"));
                array.add(obj);
            }

            if (array.size() > 0) {
                this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("update_score", room.getPumpObject(array)));
            }

        }
    }

    public void saveUserDeduction(String roomNo) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room != null) {
            JSONArray userDeductionData = new JSONArray();

            for (int i = 0; i < room.getSummaryArray().size(); ++i) {
                String account = room.getSummaryArray().getJSONObject(i).getString("account");
                JSONObject object = new JSONObject();
                object.put("id", ((Playerinfo) room.getPlayerMap().get(account)).getId());
                object.put("gid", room.getGid());
                object.put("roomNo", room.getRoomNo());
                object.put("type", room.getRoomType());
                object.put("fen", room.getSummaryArray().getJSONObject(i).getDouble("score"));
                object.put("old", Dto.sub(((Playerinfo) room.getPlayerMap().get(account)).getScore(), object.getDouble("fen")));
                if (((Playerinfo) room.getPlayerMap().get(account)).getScore() < 0.0D) {
                    object.put("new", 0);
                } else {
                    object.put("new", ((Playerinfo) room.getPlayerMap().get(account)).getScore());
                }

                userDeductionData.add(object);
            }

            this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("user_deduction", (new JSONObject()).element("user", userDeductionData)));
        }
    }

    public void addUserGameLog(String roomNo, int treasure) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        Iterator var5 = room.getPlayerMap().keySet().iterator();

        while (true) {
            String account;
            do {
                do {
                    do {
                        if (!var5.hasNext()) {
                            JSONObject gameLogObj = room.obtainGameLog(String.valueOf(array), String.valueOf(room.getBetArray()));
                            this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("insert_game_log", gameLogObj));

                            for (int i = 0; i < array.size(); ++i) {
                                JSONArray result = new JSONArray();
                                result.add((new JSONObject()).element("player", "藏宝人").element("score", ((Playerinfo) room.getPlayerMap().get(room.getBanker())).getName()).element("banker", room.getBanker()));
                                result.add((new JSONObject()).element("player", "藏宝").element("score", this.obtainTreasureName(array.getJSONObject(i).getInt("treasure"))));
                                result.add((new JSONObject()).element("player", "赔率").element("score", room.getRatio()));
                                result.add((new JSONObject()).element("player", "总注").element("score", array.getJSONObject(i).getDouble("totalBet")));
                                result.add((new JSONObject()).element("player", "押中").element("score", array.getJSONObject(i).getDouble("winBet")));
                                result.add((new JSONObject()).element("player", "输赢").element("score", array.getJSONObject(i).getDouble("sum")));
                                JSONObject userGameLog = new JSONObject();
                                userGameLog.put("gid", room.getGid());
                                userGameLog.put("room_id", room.getId());
                                userGameLog.put("room_no", roomNo);
                                userGameLog.put("game_index", room.getGameIndex());
                                userGameLog.put("user_id", array.getJSONObject(i).getLong("id"));
                                userGameLog.put("gamelog_id", gameLogObj.getLong("id"));
                                userGameLog.put("result", result);
                                userGameLog.put("createtime", TimeUtil.getNowDate());
                                userGameLog.put("account", array.getJSONObject(i).getDouble("sum"));
                                userGameLog.put("fee", room.getFee());
                                this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("insert_user_game_log", userGameLog));
                            }

                            return;
                        }

                        account = (String) var5.next();
                    } while (!room.getPlayerMap().containsKey(account));
                } while (room.getPlayerMap().get(account) == null);
            } while (Dto.isObjNull(this.obtainLastBetRecord(roomNo, account)) && !account.equals(room.getBanker()));

            JSONObject obj = new JSONObject();
            obj.put("treasure", treasure);
            if (account.equals(room.getBanker())) {
                obj.put("totalBet", this.obtainTotalBet(roomNo) * room.getScore());
                obj.put("winBet", this.obtainTotalBetByPlace(roomNo, treasure) * room.getScore());
            } else {
                obj.put("totalBet", this.obtainTotalBetByAccount(roomNo, account) * room.getScore());
                obj.put("winBet", this.obtainTotalBetByAccountAndPlace(roomNo, account, treasure) * room.getScore());
            }

            obj.put("sum", this.obtainPlayerScoreByIndex(roomNo, account));
            obj.put("id", ((Playerinfo) room.getPlayerMap().get(account)).getId());
            array.add(obj);
        }
    }

    public void hideOverTime(String roomNo) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        room.setGameStatus(1);
        this.changeGameStatus(roomNo);
    }

    public void choiceBanker(String roomNo) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (!Dto.stringIsNULL(room.getBanker())) {
            JSONObject obj = new JSONObject();
            obj.put("account", room.getBanker());
            obj.put("room_no", roomNo);
            obj.put("index", room.getLastIndex());
            room.setLastIndex(room.getLastIndex() + 1);
            this.gameChangeSeat((SocketIOClient) null, obj);
            room.setBanker((String) null);
        }

        room.getBetArray().clear();
        room.getSummaryArray().clear();
        room.setGameStatus(5);
        room.setTimeLeft(0);
        this.changeGameStatus(roomNo);
    }

    public void addBetRecord(String roomNo, String account, int place, int value) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONObject betRecord = new JSONObject();
        betRecord.put("account", account);
        betRecord.put("index", ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
        betRecord.put("place", place);
        betRecord.put("value", value);
        room.getBetArray().add(betRecord);
    }

    public void removeBetRecord(String roomNo, String account, int place) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray betArray = room.getBetArray();

        for (int i = betArray.size() - 1; i >= 0; --i) {
            if (betArray.getJSONObject(i).getString("account").equals(account) && betArray.getJSONObject(i).getInt("place") == place) {
                betArray.remove(i);
            }
        }

    }

    public void changeUserScore(String roomNo, String account, double score) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (!Dto.stringIsNULL(account) && room.getPlayerMap().containsKey(account) && room.getPlayerMap().get(account) != null) {
            double oldScore = ((Playerinfo) room.getPlayerMap().get(account)).getScore();
            double newScore = Dto.add(oldScore, score);
            if (newScore < 0.0D) {
                newScore = 0.0D;
            }

            ((Playerinfo) room.getPlayerMap().get(account)).setScore(newScore);
        }

    }

    public void changeGameStatus(String roomNo) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        Iterator var3 = room.getPlayerMap().keySet().iterator();

        while (var3.hasNext()) {
            String account = (String) var3.next();
            if (room.getPlayerMap().containsKey(account) && room.getPlayerMap().get(account) != null) {
                JSONObject obj = new JSONObject();
                obj.put("gameStatus", room.getGameStatus());
                obj.put("game_index", room.getGameIndex());
                obj.put("treasure", this.obtainTreasure(roomNo));
                obj.put("showTimer", 0);
                if (room.getTimeLeft() > 0) {
                    obj.put("showTimer", 1);
                    obj.put("time", room.getTimeLeft());
                }

                obj.put("users", this.obtainAllPlayer(roomNo, account));
                obj.put("myScore", this.obtainMyScore(roomNo, account));
                obj.put("totalScore", this.obtainTotalScore(roomNo));
                obj.put("baseNum", room.getBaseNum());
                obj.put("bankerIndex", this.obtainBankerIndex(roomNo));
                obj.put("bankerBtn", this.obtainBankerBtnStatus(roomNo, account));
                obj.put("bankerScore", room.getMinBankerScore());
                obj.put("summaryData", this.obtainSummaryData(roomNo, account));
                obj.put("winArray", this.obtainWinIndex(roomNo));
                obj.put("myIndex", ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
                UUID uuid = ((Playerinfo) room.getPlayerMap().get(account)).getUuid();
                if (uuid != null) {
                    CommonConstant.sendMsgEventToSingle(uuid, String.valueOf(obj), "changeGameStatusPush_SW");
                }
            }
        }

    }

    public Set<Integer> obtainMyBetArray(String roomNo, String account) {
        Set<Integer> set = new HashSet();
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray betArray = room.getBetArray();

        for (int i = 0; i < betArray.size(); ++i) {
            JSONObject betRecord = betArray.getJSONObject(i);
            if (betRecord.getString("account").equals(account)) {
                set.add(betRecord.getInt("place"));
            }
        }

        return set;
    }

    public JSONArray obtainMyScore(String roomNo, String account) {
        JSONArray array = new JSONArray();
        Iterator var4 = this.obtainBetList().iterator();

        while (var4.hasNext()) {
            Integer integer = (Integer) var4.next();
            JSONObject obj = new JSONObject();
            obj.put("place", integer);
            obj.put("score", this.obtainTotalBetByAccountAndPlace(roomNo, account, integer));
            array.add(obj);
        }

        return array;
    }

    public JSONArray obtainTotalScore(String roomNo) {
        JSONArray array = new JSONArray();
        Iterator var3 = this.obtainBetList().iterator();

        while (var3.hasNext()) {
            Integer integer = (Integer) var3.next();
            JSONObject obj = new JSONObject();
            obj.put("place", integer);
            obj.put("score", this.obtainTotalBetByPlace(roomNo, integer));
            array.add(obj);
        }

        return array;
    }

    public JSONObject obtainPlayerInfo(String roomNo, String account) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        Playerinfo player = (Playerinfo) room.getPlayerMap().get(account);
        JSONObject obj = new JSONObject();
        obj.put("account", player.getAccount());
        obj.put("name", player.getName());
        obj.put("headimg", player.getRealHeadimg());
        obj.put("sex", player.getSex());
        obj.put("ip", player.getIp());
        obj.put("vip", player.getVip());
        obj.put("location", player.getLocation());
        obj.put("area", player.getArea());
        obj.put("score", player.getScore());
        obj.put("index", player.getMyIndex());
        obj.put("userOnlineStatus", player.getStatus());
        obj.put("ghName", player.getGhName());
        obj.put("introduction", player.getSignature());
        return obj;
    }

    public JSONObject obtainRoomData(String roomNo, String account) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONObject obj = new JSONObject();
        obj.put("playerCount", room.getPlayerCount());
        obj.put("gameStatus", room.getGameStatus());
        obj.put("room_no", roomNo);
        obj.put("roomType", room.getRoomType());
        obj.put("game_count", room.getGameCount());
        obj.put("myIndex", ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
        obj.put("playerCount", room.getPlayerMap().size());
        obj.put("di", room.getScore());
        StringBuffer roomInfo = new StringBuffer();
        roomInfo.append(room.getWfType());
        roomInfo.append(" 1赔");
        roomInfo.append(room.getRatio());
        roomInfo.append(" ");
        roomInfo.append(room.getRoomNo());
        roomInfo.append("\n单子下注上限:");
        if (room.getSingleMax() == 0) {
            roomInfo.append("不限");
        } else {
            roomInfo.append(room.getSingleMax());
        }

        roomInfo.append("\n底注:");
        roomInfo.append(room.getScore());
        obj.put("roominfo", String.valueOf(roomInfo));
        obj.put("bankerIndex", this.obtainBankerIndex(roomNo));
        obj.put("bankerBtn", this.obtainBankerBtnStatus(roomNo, account));
        obj.put("myScore", this.obtainMyScore(roomNo, account));
        obj.put("totalScore", this.obtainTotalScore(roomNo));
        obj.put("bankerScore", room.getMinBankerScore());
        obj.put("game_index", ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
        obj.put("treasure", this.obtainTreasure(roomNo));
        obj.put("showTimer", 0);
        if (room.getTimeLeft() > 0) {
            obj.put("showTimer", 1);
            obj.put("time", room.getTimeLeft());
        }

        obj.put("users", this.obtainAllPlayer(roomNo, account));
        obj.put("baseNum", room.getBaseNum());
        obj.put("betResult", room.getBetArray());
        obj.put("summaryData", this.obtainSummaryData(roomNo, account));
        obj.put("playerCount", room.getPlayerMap().size());
        return obj;
    }

    public JSONObject obtainSummaryData(String roomNo, String account) {
        JSONObject summaryData = new JSONObject();
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room.getGameStatus() == 4) {
            summaryData.put("index", ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
            if (!account.equals(room.getBanker()) && this.obtainTotalBetByAccount(roomNo, account) <= 0) {
                summaryData.put("isWinner", 0);
                summaryData.put("sum", 0);
                summaryData.put("winNum", 0);
            } else {
                int winBetNum = this.obtainTotalBetByAccountAndPlace(roomNo, account, room.getTreasure());
                double score = this.obtainPlayerScoreByIndex(roomNo, account);
                summaryData.put("isWinner", -1);
                if (score > 0.0D) {
                    summaryData.put("isWinner", 1);
                }

                summaryData.put("sum", score);
                summaryData.put("winNum", (double) winBetNum * room.getRatio() * (double) room.getScore());
            }
        }

        return summaryData;
    }

    public double obtainPlayerScoreByIndex(String roomNo, String account) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray summaryArray = room.getSummaryArray();

        for (int i = 0; i < summaryArray.size(); ++i) {
            if (summaryArray.getJSONObject(i).getString("account").equals(account)) {
                return summaryArray.getJSONObject(i).getDouble("score");
            }
        }

        return 0.0D;
    }

    public int obtainBankerBtnStatus(String roomNo, String account) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (!Dto.stringIsNULL(room.getBanker()) && room.getPlayerMap().containsKey(room.getBanker()) && room.getPlayerMap().get(room.getBanker()) != null && ((Playerinfo) room.getPlayerMap().get(room.getBanker())).getScore() > room.getMinBankerScore()) {
            return 0;
        } else {
            return !Dto.stringIsNULL(account) && room.getPlayerMap().containsKey(account) && room.getPlayerMap().get(account) != null && ((Playerinfo) room.getPlayerMap().get(account)).getScore() > room.getMinBankerScore() ? 1 : 0;
        }
    }

    public int obtainBankerIndex(String roomNo) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        return !Dto.stringIsNULL(room.getBanker()) && room.getPlayerMap().containsKey(room.getBanker()) && room.getPlayerMap().get(room.getBanker()) != null && ((Playerinfo) room.getPlayerMap().get(room.getBanker())).getScore() > room.getMinBankerScore() ? ((Playerinfo) room.getPlayerMap().get(room.getBanker())).getMyIndex() : -1;
    }

    public int obtainTreasure(String roomNo) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        return room.getGameStatus() >= 3 ? room.getTreasure() : 0;
    }

    public JSONArray obtainAllPlayer(String roomNo, String me) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        Iterator var5 = room.getPlayerMap().keySet().iterator();

        while (true) {
            String account;
            Playerinfo player;
            do {
                do {
                    if (!var5.hasNext()) {
                        return array;
                    }

                    account = (String) var5.next();
                    player = (Playerinfo) room.getPlayerMap().get(account);
                } while (player == null);
            } while (!account.equals(me) && player.getMyIndex() > 18);

            JSONObject obj = new JSONObject();
            obj.put("account", player.getAccount());
            obj.put("name", player.getName());
            obj.put("headimg", player.getRealHeadimg());
            obj.put("sex", player.getSex());
            obj.put("ip", player.getIp());
            obj.put("vip", player.getVip());
            obj.put("location", player.getLocation());
            obj.put("area", player.getArea());
            obj.put("score", player.getScore());
            obj.put("index", player.getMyIndex());
            obj.put("userOnlineStatus", player.getStatus());
            obj.put("ghName", player.getGhName());
            obj.put("introduction", player.getSignature());
            array.add(obj);
        }
    }

    public int obtainTotalBetByAccountAndPlace(String roomNo, String account, int place) {
        int totalBet = 0;
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray betArray = room.getBetArray();

        for (int i = 0; i < betArray.size(); ++i) {
            JSONObject betRecord = betArray.getJSONObject(i);
            if (betRecord.getString("account").equals(account) && betRecord.getInt("place") == place) {
                totalBet += betRecord.getInt("value");
            }
        }

        return totalBet;
    }

    public int obtainTotalBetByPlace(String roomNo, int place) {
        int totalBet = 0;
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray betArray = room.getBetArray();

        for (int i = 0; i < betArray.size(); ++i) {
            JSONObject betRecord = betArray.getJSONObject(i);
            if (betRecord.getInt("place") == place) {
                totalBet += betRecord.getInt("value");
            }
        }

        return totalBet;
    }

    public int obtainTotalBet(String roomNo) {
        int totalBet = 0;
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray betArray = room.getBetArray();

        for (int i = 0; i < betArray.size(); ++i) {
            totalBet += betArray.getJSONObject(i).getInt("value");
        }

        return totalBet;
    }

    public int obtainTotalBetByAccount(String roomNo, String account) {
        int totalBet = 0;
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray betArray = room.getBetArray();

        for (int i = 0; i < betArray.size(); ++i) {
            JSONObject betRecord = betArray.getJSONObject(i);
            if (betRecord.getString("account").equals(account)) {
                totalBet += betRecord.getInt("value");
            }
        }

        return totalBet;
    }

    public JSONObject obtainLastBetRecord(String roomNo, String account) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray betArray = room.getBetArray();

        for (int i = betArray.size() - 1; i >= 0; --i) {
            if (betArray.getJSONObject(i).getString("account").equals(account)) {
                return betArray.getJSONObject(i);
            }
        }

        return null;
    }

    public String obtainUserAccountByIndex(String roomNo, int index) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        Iterator var4 = room.getPlayerMap().keySet().iterator();

        String account;
        do {
            if (!var4.hasNext()) {
                return null;
            }

            account = (String) var4.next();
        } while (!room.getPlayerMap().containsKey(account) || room.getPlayerMap().get(account) == null || ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex() != index);

        return account;
    }

    public int obtainMaxTimes(JSONArray array) {
        int maxTimes = 0;

        for (int i = 0; i < array.size(); ++i) {
            JSONObject baseNum = array.getJSONObject(i);
            if (baseNum.getInt("val") > maxTimes) {
                maxTimes = baseNum.getInt("val");
            }
        }

        return maxTimes;
    }

    public JSONArray obtainWinIndex(String roomNo) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray winArray = new JSONArray();
        if (room.getGameStatus() == 4) {
            JSONArray betArray = room.getBetArray();

            for (int i = betArray.size() - 1; i >= 0; --i) {
                if (betArray.getJSONObject(i).getInt("place") == room.getTreasure() && !winArray.contains(betArray.getJSONObject(i).getInt("index"))) {
                    winArray.add(betArray.getJSONObject(i).getInt("index"));
                }
            }
        }

        return winArray;
    }

    public String obtainTreasureName(int treasure) {
        switch (treasure) {
            case 1:
                return "車";
            case 2:
                return "馬";
            case 3:
                return "包";
            case 4:
                return "象";
            case 5:
                return "士";
            case 6:
                return "將";
            case 7:
                return "俥";
            case 8:
                return "傌";
            case 9:
                return "炮";
            case 10:
                return "相";
            case 11:
                return "仕";
            case 12:
                return "帥";
            default:
                return null;
        }
    }

    public List<Integer> obtainBetList() {
        List<Integer> betList = new ArrayList();
        betList.add(1);
        betList.add(2);
        betList.add(3);
        betList.add(4);
        betList.add(5);
        betList.add(6);
        betList.add(7);
        betList.add(8);
        betList.add(9);
        betList.add(10);
        betList.add(11);
        betList.add(12);
        return betList;
    }

    private void beginStartTimer(final String roomNo, final String account) {
        ThreadPoolHelper.executorService.submit(new Runnable() {
            public void run() {
                SwGameEventDeal.this.gameTimerSw.startOverTime(roomNo, account, 90);
            }
        });
    }
}
