
package com.zhuoan.biz.event.bdx;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.event.FundEventDeal;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.bdx.BDXGameRoomNew;
import com.zhuoan.biz.model.bdx.UserPackerBDX;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.*;

@Component
public class BDXGameEventDealNew {
    @Resource
    private UserBiz userBiz;
    @Resource
    private Destination daoQueueDestination;
    @Resource
    private ProducerService producerService;
    @Resource
    private FundEventDeal fundEventDeal;
    @Resource
    private RedisInfoService redisInfoService;

    public BDXGameEventDealNew() {
    }

    public void createRoom(SocketIOClient client, Object data) {}

    public void joinRoom(SocketIOClient client, Object data) {}

    public void startGame(BDXGameRoomNew room) {
        room.initGame();
        room.setGameStatus(2);
    }

    public void xiaZhu(SocketIOClient client, Object data) {}

    public void gameEvent(SocketIOClient client, Object data) {}

    public void exitRoom(SocketIOClient client, Object data) {}

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
                        BDXGameRoomNew room = (BDXGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
                        if (!Dto.stringIsNULL(account) && room.getPlayerMap().containsKey(account) && room.getPlayerMap().get(account) != null) {
                            ((Playerinfo) room.getPlayerMap().get(account)).setUuid(client.getSessionId());
                            result.put("type", 1);
                            result.put("data", this.obtainRoomData(roomNo, account));
                            CommonConstant.sendMsgEventToSingle(client, result.toString(), "reconnectGamePush_BDX");
                        } else {
                            result.put("type", 0);
                            CommonConstant.sendMsgEventToSingle(client, result.toString(), "reconnectGamePush_BDX");
                        }
                    } else {
                        result.put("type", 0);
                        CommonConstant.sendMsgEventToSingle(client, result.toString(), "reconnectGamePush_BDX");
                    }
                }
            }
        }
    }

    public void summary(BDXGameRoomNew room, String giveUpAccount) {
        long summaryTimes = this.redisInfoService.summaryTimes(room.getRoomNo(), "_DBX");
        if (summaryTimes <= 1L) {
            room.setGameStatus(3);
            double sum = ((UserPackerBDX) room.getUserPacketMap().get(giveUpAccount)).getValue();
            List<Integer> pai = obtainPai();
            Iterator var8 = room.getUserPacketMap().keySet().iterator();

            while (var8.hasNext()) {
                String account = (String) var8.next();
                if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                    ((UserPackerBDX) room.getUserPacketMap().get(account)).setStatus(0);
                    double oldScore = ((Playerinfo) room.getPlayerMap().get(account)).getScore();
                    if (account.equals(giveUpAccount)) {
                        ((UserPackerBDX) room.getUserPacketMap().get(account)).setScore(-sum);
                        ((UserPackerBDX) room.getUserPacketMap().get(account)).setPai(new int[]{(Integer) pai.get(1)});
                        ((UserPackerBDX) room.getUserPacketMap().get(account)).setIsWin(0);
                        ((Playerinfo) room.getPlayerMap().get(account)).setScore(Dto.sub(oldScore, sum));
                    } else {
                        ((UserPackerBDX) room.getUserPacketMap().get(account)).setScore(sum);
                        ((UserPackerBDX) room.getUserPacketMap().get(account)).setPai(new int[]{(Integer) pai.get(0)});
                        ((UserPackerBDX) room.getUserPacketMap().get(account)).setIsWin(1);
                        ((Playerinfo) room.getPlayerMap().get(account)).setScore(Dto.add(oldScore, sum));
                    }
                }
            }

            JSONArray array = new JSONArray();
            JSONArray userDeductionData = new JSONArray();
            JSONArray gameLogResults = new JSONArray();
            JSONArray gameResult = new JSONArray();
            JSONArray gameProcessJS = new JSONArray();
            Iterator var13 = room.getUserPacketMap().keySet().iterator();

            while (var13.hasNext()) {
                String account = (String) var13.next();
                if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                    JSONObject userJS = new JSONObject();
                    userJS.put("account", account);
                    userJS.put("name", ((Playerinfo) room.getPlayerMap().get(account)).getName());
                    userJS.put("sum", ((UserPackerBDX) room.getUserPacketMap().get(account)).getScore());
                    gameProcessJS.add(userJS);
                    JSONObject obj = new JSONObject();
                    obj.put("total", ((Playerinfo) room.getPlayerMap().get(account)).getScore());
                    obj.put("fen", ((UserPackerBDX) room.getUserPacketMap().get(account)).getScore());
                    obj.put("id", ((Playerinfo) room.getPlayerMap().get(account)).getId());
                    array.add(obj);
                    JSONObject object = new JSONObject();
                    object.put("id", ((Playerinfo) room.getPlayerMap().get(account)).getId());
                    object.put("gid", room.getGid());
                    object.put("roomNo", room.getRoomNo());
                    object.put("type", room.getRoomType());
                    object.put("fen", ((UserPackerBDX) room.getUserPacketMap().get(account)).getScore());
                    object.put("old", Dto.sub(((Playerinfo) room.getPlayerMap().get(account)).getScore(), ((UserPackerBDX) room.getUserPacketMap().get(account)).getScore()));
                    object.put("new", ((Playerinfo) room.getPlayerMap().get(account)).getScore());
                    userDeductionData.add(object);
                    JSONObject gameLogResult = new JSONObject();
                    gameLogResult.put("account", account);
                    gameLogResult.put("name", ((Playerinfo) room.getPlayerMap().get(account)).getName());
                    gameLogResult.put("headimg", ((Playerinfo) room.getPlayerMap().get(account)).getHeadimg());
                    gameLogResult.put("myIndex", ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
                    gameLogResult.put("score", ((UserPackerBDX) room.getUserPacketMap().get(account)).getScore());
                    gameLogResult.put("totalScore", ((Playerinfo) room.getPlayerMap().get(account)).getScore());
                    gameLogResult.put("win", 1);
                    if (((UserPackerBDX) room.getUserPacketMap().get(account)).getStatus() < 0) {
                        gameLogResult.put("win", 0);
                    }

                    gameLogResults.add(gameLogResult);
                    JSONObject userResult = new JSONObject();
                    userResult.put("zhuang", room.getBanker());
                    userResult.put("isWinner", 0);
                    if (((UserPackerBDX) room.getUserPacketMap().get(account)).getScore() > 0.0D) {
                        userResult.put("isWinner", 1);
                    }

                    userResult.put("score", ((UserPackerBDX) room.getUserPacketMap().get(account)).getScore());
                    userResult.put("totalScore", ((Playerinfo) room.getPlayerMap().get(account)).getScore());
                    userResult.put("player", ((Playerinfo) room.getPlayerMap().get(account)).getName());
                    gameResult.add(userResult);
                }
            }

            room.getGameProcess().put("JieSuan", gameProcessJS);
            this.userBiz.updateUserBalance(array, room.getCurrencyType());
            this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("user_deduction", (new JSONObject()).element("user", userDeductionData)));
            JSONObject gameLogObj = room.obtainGameLog(gameLogResults.toString(), room.getGameProcess().toString());
            this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("insert_game_log", gameLogObj));
            JSONObject o = new JSONObject();
            JSONArray userGameLogs = room.obtainUserGameLog(gameLogObj.getLong("id"), array);
            o.element("array", userGameLogs);
            o.element("object", (new JSONObject()).element("gamelog_id", gameLogObj.getLong("id")).element("room_type", room.getRoomType()).element("room_id", room.getId()).element("room_no", room.getRoomNo()).element("game_index", room.getGameNewIndex()).element("result", gameResult.toString()));
            this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("insert_user_game_log", o));
        }
    }

    public static List<Integer> obtainPai() {
        int pai1 = RandomUtils.nextInt(13) + 1;
        int pai2 = RandomUtils.nextInt(13) + 1;
        int color1 = RandomUtils.nextInt(4);
        int color2 = RandomUtils.nextInt(4);
        List<Integer> list = new ArrayList();
        if (pai1 == pai2 && color1 == color2) {
            return obtainPai();
        } else if (pai1 > pai2) {
            list.add(pai1 + color1 * 20);
            list.add(pai2 + color2 * 20);
            return list;
        } else {
            list.add(pai2 + color2 * 20);
            list.add(pai1 + color1 * 20);
            return list;
        }
    }

    public JSONObject obtainRoomData(String roomNo, String account) {
        JSONObject roomData = new JSONObject();
        BDXGameRoomNew room = (BDXGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        roomData.put("playerCount", room.getPlayerCount());
        roomData.put("gameStatus", room.getGameStatus());
        roomData.put("myIndex", ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
        roomData.put("room_no", room.getRoomNo());
        roomData.put("roomType", room.getRoomType());
        roomData.put("users", room.getAllPlayer());
        return roomData;
    }
}
