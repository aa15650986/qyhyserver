package com.zhuoan.biz.event.gzmj;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.gzmj.GzMjCore;
import com.zhuoan.biz.event.BaseEventDeal;
import com.zhuoan.biz.game.biz.GameCircleService;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.gzmj.GzMjDeskCard;
import com.zhuoan.biz.model.gzmj.GzMjGameRoom;
import com.zhuoan.biz.model.gzmj.GzMjSummaryDetail;
import com.zhuoan.biz.model.gzmj.UserPacketGzMj;
import com.zhuoan.constant.CircleConstant;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.DaoTypeConstant;
import com.zhuoan.constant.GzMjConstant;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.util.Dto;
import com.zhuoan.util.thread.ThreadPoolHelper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GzMjGameEventDeal {

    @Resource
    private UserBiz userBiz;
    @Resource
    private Destination daoQueueDestination;
    @Resource
    private ProducerService producerService;
    @Resource
    private BaseEventDeal baseEventDeal;
    @Resource
    private GameTimerGzMj gameTimerGzMj;
    @Resource
    private GameCircleService gameCircleService;


    public void createOrJoinRoom(SocketIOClient client, Object data) {}

    public void sitDown(SocketIOClient client, Object data) {}

    public void gameReady(SocketIOClient client, Object data) {}

    public void gameBet(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, GzMjConstant.GZMJ_GAME_STATUS_BET, client)) {
            return;
        }
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        // 估卖倍数
        Integer betTimes = postData.getInt("betTimes");
        GzMjGameRoom room = (GzMjGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 验证玩家状态，已经估卖过的玩家无法重复操作
        if (room.getUserPacketMap().get(account).getStatus() == GzMjConstant.GZMJ_USER_STATUS_BET) {
            return;
        }
        // 当前玩家可选估卖倍数
        List<JSONObject> betList = getBetList(room, account);
        // 不估卖无需进行参数验证
        boolean isRight = betTimes == 0;
        for (JSONObject object : betList) {
            // 当前筹码可选才可进行估卖
            if (object.containsKey("isUse") && object.getInt("isUse") == CommonConstant.GLOBAL_YES &&
                    object.containsKey("value") && object.getInt("value") == betTimes) {
                isRight = true;
                break;
            }
        }
        // 筹码不正确无法进行估卖
        if (!isRight) {
            return;
        }
        // 设置玩家状态
        room.getUserPacketMap().get(account).setStatus(GzMjConstant.GZMJ_USER_STATUS_BET);
        // 设置估卖倍数
        room.getUserPacketMap().get(account).setBetTime(betTimes);

        JSONObject result = new JSONObject();
        result.put("index", room.getPlayerMap().get(account).getMyIndex());
        result.put("betTimes", room.getUserPacketMap().get(account).getBetTime());
        result.put("showTimer", room.getTimeLeft() > 0 ? CommonConstant.GLOBAL_YES : CommonConstant.GLOBAL_NO);
        result.put("timer", room.getTimeLeft());
        CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "gameBetPush_GZMJ");

        if (checkAllAndUserStatus(room, GzMjConstant.GZMJ_USER_STATUS_BET)) {
            startGame(room);
        }
    }

    public void gameChooseLack(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, GzMjConstant.GZMJ_GAME_STATUS_CHOOSE_LACK, client)) {
            return;
        }
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        // 缺牌类型
        int lackType = postData.getInt("lackType");
        if (lackType != GzMjConstant.GZMJ_CARD_COLOR_TONG && lackType != GzMjConstant.GZMJ_CARD_COLOR_WANG && lackType != GzMjConstant.GZMJ_CARD_COLOR_TIAO) {
            return;
        }
        GzMjGameRoom room = (GzMjGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 如已经定缺无法重复操作
        if (room.getUserPacketMap().get(account).getStatus() == GzMjConstant.GZMJ_USER_STATUS_CHOOSE_LACK ||
                room.getUserPacketMap().get(account).getUserLack() != 0) {
            return;
        }
        // 设置玩家状态
        room.getUserPacketMap().get(account).setStatus(GzMjConstant.GZMJ_USER_STATUS_CHOOSE_LACK);
        // 设置玩家缺牌类型
        room.getUserPacketMap().get(account).setUserLack(lackType);

        JSONObject result = new JSONObject();
        result.put("index", room.getPlayerMap().get(account).getMyIndex());
        result.put("lackType", lackType);
        result.put("showTimer", room.getTimeLeft() > 0 ? CommonConstant.GLOBAL_YES : CommonConstant.GLOBAL_NO);
        result.put("timer", room.getTimeLeft());
        CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "gameChooseLackPush_GZMJ");

        if (checkAllAndUserStatus(room, GzMjConstant.GZMJ_USER_STATUS_CHOOSE_LACK)) {
            // 设置房间状态
            room.setGameStatus(GzMjConstant.GZMJ_GAME_STATUS_GAME);
            // 通知玩家
            changeGameStatusInform(room.getRoomNo());
        }
    }

    public void gameReconnect(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!postData.containsKey(CommonConstant.DATA_KEY_ROOM_NO) ||
                !postData.containsKey(CommonConstant.DATA_KEY_ACCOUNT) ||
                !postData.containsKey("uuid")) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        JSONObject userInfo = userBiz.getUserByAccount(account);
        // uuid不匹配
        if (!userInfo.containsKey("uuid") || Dto.stringIsNULL(userInfo.getString("uuid")) ||
                !userInfo.getString("uuid").equals(postData.getString("uuid"))) {
            return;
        }
        JSONObject result = new JSONObject();
        if (client == null) {
            return;
        }
        // 房间不存在
        if (!RoomManage.gameRoomMap.containsKey(roomNo) || RoomManage.gameRoomMap.get(roomNo) == null) {
            result.put("code", 0);
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "gameReconnectPush_GZMJ");
            return;
        }
        GzMjGameRoom room = (GzMjGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 不在当前房间内
        if (Dto.stringIsNULL(account) || !room.getPlayerMap().containsKey(account) || room.getPlayerMap().get(account) == null) {
            result.put("code", 0);
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "gameReconnectPush_GZMJ");
            return;
        }
        // 刷新uuid
        room.getPlayerMap().get(account).setUuid(client.getSessionId());
        // 组织数据，通知玩家
        result.put("code", 1);
        result.put("roomData", getRoomData(roomNo, account));
        // 通知玩家
        CommonConstant.sendMsgEventToSingle(client, result.toString(), "gameReconnectPush_GZMJ");
    }

    public void gameOutCard(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, GzMjConstant.GZMJ_GAME_STATUS_GAME, client)) {
            return;
        }
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        // 牌
        Integer card = postData.getInt("card");
        GzMjGameRoom room = (GzMjGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (!room.getUserPacketMap().get(account).getHandCardList().contains(card)) {
            return;
        }
        // 移除手牌
        room.getUserPacketMap().get(account).getHandCardList().remove(card);
        // 上个出牌玩家
        room.setLastAccount(account);
        // 上一张牌
        room.setLastCard(card);
        room.getUserPacketMap().get(account).getOutCardList().add(card);
        room.setFocusIndex(room.getPlayerMap().get(account).getMyIndex());
        // 清空询问记录
        room.getActionMap().clear();
        // 是否有玩家有事件
        boolean hasAction = false;
        // 设置玩家操作类型
        for (String player : room.getUserPacketMap().keySet()) {
            if (!StringUtils.equals(account, player)) {
                if (GzMjCore.getCardColor(card) == room.getUserPacketMap().get(player).getUserLack()) {
                    room.getUserPacketMap().get(player).setActionList(new ArrayList<Integer>());
                    continue;
                }
                List<Integer> actionList = GzMjCore.getActionList(room.getUserPacketMap().get(player).getHandCardList()
                        , room.getUserPacketMap().get(player).getDeskCardList(), card, true);
                room.getUserPacketMap().get(player).setActionList(actionList);
                if (actionList.size() > 0) {
                    hasAction = true;
                }
            } else {
                room.getUserPacketMap().get(player).setActionList(new ArrayList<Integer>());
            }
        }
        // 没有玩家有事件则下家摸牌
        if (!hasAction) {
            String nextPlayer = getNextPlayer(room, account);
            moPai(room, nextPlayer, new ArrayList<Integer>(), GzMjConstant.GZMJ_CHANGE_TYPE_MP);
        } else {
            focusChangeInform(roomNo, GzMjConstant.GZMJ_CHANGE_TYPE_HPG, account, new ArrayList<Integer>());
        }
    }

    public void gameEvent(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, GzMjConstant.GZMJ_GAME_STATUS_GAME, client)) {
            return;
        }
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        GzMjGameRoom room = (GzMjGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 事件类型
        int actionType = postData.getInt("actionType");
        // 事件不符合且不为过则直接返回
        if (!getActionList(room, account).contains(actionType) && actionType != GzMjConstant.GZMJ_ACTION_TYPE_PASS) {
            return;
        }
        if (actionType != GzMjConstant.GZMJ_ACTION_TYPE_PASS) {
            room.getActionMap().put(account, actionType);
        }
        // 清空玩家事件询问
        room.getUserPacketMap().get(account).getActionList().clear();
        // 全部询问完成
        if (checkAllFinishAction(room)) {
            allFinishActionDeal(room, account);
        }
    }

    public void exitRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        GzMjGameRoom room = (GzMjGameRoom) RoomManage.gameRoomMap.get(roomNo);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (!Dto.stringIsNULL(account)) {
            boolean canExit = false;
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                // 金币场、元宝场
                if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB || room.getRoomType() == CommonConstant.ROOM_TYPE_YB ||
                        room.getRoomType() == CommonConstant.ROOM_TYPE_COMPETITIVE || room.getRoomType() == CommonConstant.ROOM_TYPE_FREE) {
                    // 未参与游戏可以自由退出
                    if (room.getUserPacketMap().get(account).getStatus() == GzMjConstant.GZMJ_USER_STATUS_INIT) {
                        canExit = true;
                    } else if (room.getGameStatus() != GzMjConstant.GZMJ_GAME_STATUS_BET && room.getGameStatus() != GzMjConstant.GZMJ_GAME_STATUS_CHOOSE_LACK
                            && room.getGameStatus() != GzMjConstant.GZMJ_GAME_STATUS_GAME) {
                        // 初始及准备阶段可以退出
                        canExit = true;
                    }
                } else if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_DK
                        || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB || room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
                    if (room.getPlayerMap().get(account).getPlayTimes() == 0) {
                        if (CommonConstant.ROOM_PAY_TYPE_AA.equals(room.getPayType()) || !room.getOwner().equals(account) || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
                            canExit = true;
                        } else if (room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
                            canExit = true;
                        }
                    }
                    if (room.getGameStatus() == GzMjConstant.GZMJ_GAME_STATUS_FINAL_SUMMARY) {
                        canExit = true;
                    }
                }
            } else {
                canExit = true;
            }
            Playerinfo player = room.getPlayerMap().get(account);
            if (canExit) {
                List<UUID> allUUIDList = room.getAllUUIDList();
                // 退出房间更新数据 start
// 移除数据
                for (int i = 0; i < room.getUserIdList().size(); i++) {
                    if (room.getUserIdList().get(i) == room.getPlayerMap().get(account).getId()) {
                        room.getUserIdList().set(i, 0L);
                        room.addIndexList(room.getPlayerMap().get(account).getMyIndex());
                        break;
                    }
                }
                JSONObject roomInfo = new JSONObject();
                roomInfo.put("roomNo", room.getRoomNo());
                roomInfo.put("roomId", room.getId());
                roomInfo.put("userIndex", room.getPlayerMap().get(account).getMyIndex());
                room.getPlayerMap().remove(account);
                room.getVisitPlayerMap().remove(account); //观战人员直接退出
                room.getUserPacketMap().remove(account);
                roomInfo.put("player_number", room.getPlayerMap().size());
                producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_QUIT_USER_ROOM, roomInfo));
// 退出房间更新数据 end
                // 组织数据，通知玩家
                JSONObject result = new JSONObject();
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                result.put("type", 1);
                result.put("index", player.getMyIndex());
                result.put("showTimer", CommonConstant.GLOBAL_NO);
                result.put("timer", room.getTimeLeft());
                if (!postData.containsKey("notSend")) {
                    CommonConstant.sendMsgEventToAll(allUUIDList, result.toString(), "exitRoomPush");
                }
                if (postData.containsKey("notSendToMe")) {
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "exitRoomPush");
                }

                // 房间内所有玩家都已经完成准备且人数大于两人通知开始游戏
                if (checkAllAndUserStatus(room, GzMjConstant.GZMJ_USER_STATUS_READY)) {
                    startGame(room);
                }
                // 所有人都退出清除房间数据
                if (room.getPlayerMap().size() == 0) {
                    if (CommonConstant.ROOM_TYPE_FREE == room.getRoomType() || CommonConstant.ROOM_TYPE_INNING == room.getRoomType()) {
                        roomInfo.put("status", 0);
                    } else {
                        roomInfo.put("status", room.getIsClose());
                    }
                    roomInfo.put("game_index", 0);
                    //亲友圈房间 复制房间 初始化
                    if (CommonConstant.ROOM_TYPE_FREE == room.getRoomType() || CommonConstant.ROOM_TYPE_INNING == room.getRoomType()) {
                        copyGameRoom(roomNo, null);
                    }
                    RoomManage.gameRoomMap.remove(roomNo);
                }

            } else {
                // 组织数据，通知玩家
                JSONObject result = new JSONObject();
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                result.put(CommonConstant.RESULT_KEY_MSG, "游戏中无法退出");
                result.put("showTimer", CommonConstant.GLOBAL_NO);
                result.put("timer", room.getTimeLeft());
                result.put("type", 1);
                CommonConstant.sendMsgEventToSingle(client, result.toString(), "exitRoomPush");
            }
        }
    }

    public void closeRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        GzMjGameRoom room = (GzMjGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room.getRoomType() != CommonConstant.ROOM_TYPE_FK && room.getRoomType() != CommonConstant.ROOM_TYPE_DK
                && room.getRoomType() != CommonConstant.ROOM_TYPE_CLUB && room.getRoomType() != CommonConstant.ROOM_TYPE_INNING) {
            return;
        }
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (postData.containsKey("type")) {
            JSONObject result = new JSONObject();
            int type = postData.getInt("type");
            // 有人发起解散设置解散时间
            if (type == CommonConstant.CLOSE_ROOM_AGREE && room.getJieSanTime() == 0) {
                room.setJieSanTime(60);
                ThreadPoolHelper.executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        gameTimerGzMj.closeRoomOverTime(roomNo, 60);
                    }
                });
            }
            // 设置解散状态
            room.getPlayerMap().get(account).setIsCloseRoom(type);
            // 有人拒绝解散
            if (type == CommonConstant.CLOSE_ROOM_DISAGREE) {
                // 重置解散
                room.setJieSanTime(0);
                // 设置玩家为未确认状态
                for (String uuid : room.getUserPacketMap().keySet()) {
                    if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
                        room.getPlayerMap().get(uuid).setIsCloseRoom(CommonConstant.CLOSE_ROOM_UNSURE);
                    }
                }
                // 通知玩家
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                String[] names = {room.getPlayerMap().get(account).getName()};
                result.put("names", names);
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush_GZMJ");
                return;
            }
            if (type == CommonConstant.CLOSE_ROOM_AGREE) {
                // 全部同意解散
                if (isAgreeClose(room)) {
                    // 未玩完一局不需要强制结算
                    if (room.getGameIndex() <= 1 && room.getGameStatus() != GzMjConstant.GZMJ_GAME_STATUS_SUMMARY) {
                        // 所有玩家
                        List<UUID> uuidList = room.getAllUUIDList();
                        copyGameRoom(roomNo, null);
                        // 移除房间
                        RoomManage.gameRoomMap.remove(roomNo);
                        // 通知玩家
                        result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
                        result.put(CommonConstant.RESULT_KEY_MSG, "成功解散房间");
                        CommonConstant.sendMsgEventToAll(uuidList, result.toString(), "tipMsgPush");
                        return;
                    }
                    userPumpingFee(roomNo);// 亲友圈抽水
                    if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
                        room.setOpen(false);
                    }
                    room.setFinish(true);
                    room.setGameStatus(GzMjConstant.GZMJ_GAME_STATUS_FINAL_SUMMARY);
                    changeGameStatusInform(room.getRoomNo());
                } else {
                    // 刷新数据
                    room.getPlayerMap().get(account).setIsCloseRoom(CommonConstant.CLOSE_ROOM_AGREE);
                    result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                    result.put("data", getCloseRoomData(room));
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush");
                }
            }
        }
    }

    private boolean isAgreeClose(GzMjGameRoom room) {
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                if (room.getPlayerMap().get(account).getIsCloseRoom() != CommonConstant.CLOSE_ROOM_AGREE) {
                    return false;
                }
            }
        }
        return true;
    }

    private JSONArray getCloseRoomData(GzMjGameRoom room) {
        JSONArray array = new JSONArray();
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                JSONObject obj = new JSONObject();
                obj.put("index", room.getPlayerMap().get(account).getMyIndex());
                obj.put("result", room.getPlayerMap().get(account).getIsCloseRoom());
                obj.put("name", room.getPlayerMap().get(account).getName());
                obj.put("jiesanTimer", room.getJieSanTime());
                array.add(obj);
            }
        }
        return array;
    }

    private void userPumpingFee(String roomNo) {
        GzMjGameRoom room = (GzMjGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room == null) return;
        int gameStatus = room.getGameStatus();//房间状态
        if (GzMjConstant.GZMJ_GAME_STATUS_SUMMARY != gameStatus && GzMjConstant.GZMJ_GAME_STATUS_FINAL_SUMMARY != gameStatus)
            return;
        String cutType = room.getCutType();//抽水类型
        int roomType = room.getRoomType();//房间类型
        if (CommonConstant.ROOM_TYPE_FREE != roomType && CommonConstant.ROOM_TYPE_INNING != roomType
                && CommonConstant.ROOM_TYPE_TEA != roomType && CommonConstant.ROOM_TYPE_FK != roomType) {
            return;
        }
        if (CommonConstant.ROOM_TYPE_INNING == roomType) {//局数场
            if (!isAgreeClose(room)) { //全部同意解散
                if (CircleConstant.CUTTYPE_BIG_WINNER.equals(cutType) || CircleConstant.CUTTYPE_WINNER.equals(cutType)
                        || CircleConstant.CUTTYPE_GD.equals(cutType) || CircleConstant.CUTTYPE_MC.equals(cutType)) {
                    if (room.getGameIndex() != room.getGameCount()) {
                        return;
                    }
                }
            }
        }
        Map<String, Double> pumpInfo = new HashMap<>();
        UserPacketGzMj up;
        Playerinfo playerinfo;
        double maxScore = 0D;// 取大赢家分数

        if (CircleConstant.CUTTYPE_BIG_WINNER.equals(cutType)) {
            if (CommonConstant.ROOM_TYPE_INNING == roomType) {
                for (String account : room.getPlayerMap().keySet()) {
                    playerinfo = room.getPlayerMap().get(account);
                    if (playerinfo == null) continue;
                    if (playerinfo.getScore() > maxScore) maxScore = playerinfo.getScore();
                }
            } else {//自由场
                for (String account : room.getUserPacketMap().keySet()) {
                    up = room.getUserPacketMap().get(account);
                    if (up == null) continue;
                    if (up.getScore() > maxScore) maxScore = up.getScore();
                }
            }
        }

        for (String account : room.getPlayerMap().keySet()) {
            double deductScore = 0D;//抽水分数
            up = room.getUserPacketMap().get(account);
            playerinfo = room.getPlayerMap().get(account);
            if (up == null || playerinfo == null || playerinfo.getScore() < 0D) continue;
            // 有参与的玩家
            if (GzMjConstant.GZMJ_USER_STATUS_INIT == up.getStatus() || GzMjConstant.GZMJ_USER_STATUS_READY == up.getStatus())
                continue;
            if (CircleConstant.CUTTYPE_BIG_WINNER.equals(cutType)) {//大赢家消耗
                if ((CommonConstant.ROOM_TYPE_FREE == roomType && up.getScore() == maxScore)
                        || (CommonConstant.ROOM_TYPE_INNING == roomType && playerinfo.getScore() == maxScore)) {
                    deductScore = Dto.mul(room.getPump() / 100, maxScore);
                }
            } else if (CircleConstant.CUTTYPE_WINNER.equals(cutType)) {//赢家消耗
                if (CommonConstant.ROOM_TYPE_FREE == roomType) {//自由场
                    if (up.getScore() > 0D) deductScore = Dto.mul(room.getPump() / 100, up.getScore());
                } else {//局数场
                    if (playerinfo.getScore() > 0D)
                        deductScore = Dto.mul(room.getPump() / 100, playerinfo.getScore());
                }
            } else if (CircleConstant.CUTTYPE_DI.equals(cutType)) {//底注消耗
                deductScore = Dto.mul(room.getPump() / 100, room.getScore());
            }

            if (deductScore > 0D) {
                deductScore = Math.ceil(deductScore); //抽水取整
                up.setScore(Dto.sub(up.getScore(), deductScore));
                playerinfo.setScore(Dto.sub(playerinfo.getScore(), deductScore));
                pumpInfo.put(String.valueOf(playerinfo.getId()), -deductScore);
            }
        }
        if (pumpInfo.size() > 0) {
            JSONObject object = new JSONObject();
            object.put("circleId", room.getCircleId());
            object.put("roomNo", room.getRoomNo());
            object.put("gameId", room.getGid());
            object.put("pumpInfo", pumpInfo);
            object.put("cutType", room.getCutType());
            object.put("changeType", CircleConstant.PROP_BILL_CHANGE_TYPE_FEE);
            //亲友圈玩家反水
            gameCircleService.circleUserPumping(object);
        }
    }

    private void copyGameRoom(String roomNo, Playerinfo playerinfo) {
        GzMjGameRoom room = (GzMjGameRoom) RoomManage.gameRoomMap.get(roomNo).clone();
        room.setGameIndex(0);
        room.setGameNewIndex(0);
        room.setGameStatus(GzMjConstant.GZMJ_GAME_STATUS_INIT);
        room.setJieSanTime(0);
        room.setTimeLeft(0);
        room.setFinalSummaryData(new JSONArray());
        room.setUserPacketMap(new ConcurrentHashMap<>());
        room.setPlayerMap(new ConcurrentHashMap<>());
        room.setVisitPlayerMap(new ConcurrentHashMap<>());
        room.setOpen(true);
        for (int i = 0; i < room.getUserIdList().size(); i++) {
            if (room.getUserIdList().get(i) > 0L) {
                room.getUserIdList().set(i, 0L);
                room.addIndexList(i);
            }
        }

//处理清空房间，重新生成
        baseEventDeal.reloadClearRoom(roomNo);
    }

    private boolean checkAllFinishAction(GzMjGameRoom room) {
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().get(account).getActionList().size() > 0) {
                return false;
            }
        }
        return true;
    }

    private void allFinishActionDeal(GzMjGameRoom room, String account) {
        // 如无事件则过
        if (room.getActionMap().size() == 0) {
            guo(room, account);
        } else {
            List<String> huList = new ArrayList<>();
            int actionType = -1;
            String actionPlayer = null;
            for (String player : room.getActionMap().keySet()) {
                if (room.getActionMap().get(player) == GzMjConstant.GZMJ_ACTION_TYPE_HU) {
                    huList.add(player);
                } else {
                    actionType = room.getActionMap().get(player);
                    actionPlayer = player;
                }
            }
            // 有人胡则胡，否则碰杠
            if (huList.size() > 0) {
                hu(room, huList);
            } else if (StringUtils.isNotBlank(actionPlayer)) {
                switch (actionType) {
                    case GzMjConstant.GZMJ_ACTION_TYPE_PENG:
                        peng(room, actionPlayer);
                        break;
                    case GzMjConstant.GZMJ_ACTION_TYPE_AG:
                        anGang(room, actionPlayer);
                        break;
                    case GzMjConstant.GZMJ_ACTION_TYPE_MG:
                        mingGang(room, actionPlayer);
                        break;
                    case GzMjConstant.GZMJ_ACTION_TYPE_BG:
                        buGang(room, actionPlayer);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void peng(GzMjGameRoom room, String account) {
        GzMjDeskCard gzMjDeskCard = new GzMjDeskCard();
        gzMjDeskCard.setCardList(new ArrayList<>(Arrays.asList(room.getLastCard(), room.getLastCard(), room.getLastCard())));
        gzMjDeskCard.setCardType(GzMjConstant.GZMJ_DESK_CARD_TYPE_PENG);
        gzMjDeskCard.setFrom(room.getLastAccount());
        // 添加桌面牌
        room.getUserPacketMap().get(account).getDeskCardList().add(gzMjDeskCard);
        // 移除手牌
        room.getUserPacketMap().get(account).getHandCardList().remove(room.getLastCard());
        room.getUserPacketMap().get(account).getHandCardList().remove(room.getLastCard());
        // 移除玩家已出牌记录
        room.getUserPacketMap().get(room.getLastAccount()).getOutCardList().remove(room.getLastCard());
        // 设置上个操作玩家
        room.setLastAccount(account);
        room.setFocusIndex(room.getPlayerMap().get(account).getMyIndex());
        // 重置摸牌
        room.setCurrentCard(0);
        ArrayList<Integer> cardList = new ArrayList<>();
        cardList.add(room.getLastCard());
        cardList.add(room.getLastCard());
        cardList.add(room.getLastCard());
        // 碰结果通知
        focusChangeInform(room.getRoomNo(), GzMjConstant.GZMJ_CHANGE_TYPE_PENG_RESULT, account, cardList);
    }

    private void anGang(GzMjGameRoom room, String account) {
        GzMjDeskCard gzMjDeskCard = new GzMjDeskCard();
        gzMjDeskCard.setCardList(Arrays.asList(room.getCurrentCard(), room.getCurrentCard(), room.getCurrentCard(), room.getCurrentCard()));
        gzMjDeskCard.setCardType(GzMjConstant.GZMJ_DESK_CARD_TYPE_AG);
        gzMjDeskCard.setFrom(account);
        // 添加桌面牌
        room.getUserPacketMap().get(account).getDeskCardList().add(gzMjDeskCard);
        // 移除手牌
        room.getUserPacketMap().get(account).getHandCardList().remove(room.getCurrentCard());
        room.getUserPacketMap().get(account).getHandCardList().remove(room.getCurrentCard());
        room.getUserPacketMap().get(account).getHandCardList().remove(room.getCurrentCard());
        room.getUserPacketMap().get(account).getHandCardList().remove(room.getCurrentCard());
        // 设置上个操作玩家
        room.setLastAccount(account);
        room.setFocusIndex(room.getPlayerMap().get(account).getMyIndex());
        ArrayList<Integer> cardList = new ArrayList<>();
        cardList.add(room.getCurrentCard());
        cardList.add(room.getCurrentCard());
        cardList.add(room.getCurrentCard());
        cardList.add(room.getCurrentCard());
        // 杠摸牌
        moPai(room, account, cardList, GzMjConstant.GZMJ_CHANGE_TYPE_AG_RESULT);
    }

    private void mingGang(GzMjGameRoom room, String account) {
        GzMjDeskCard gzMjDeskCard = new GzMjDeskCard();
        gzMjDeskCard.setCardList(Arrays.asList(room.getLastCard(), room.getLastCard(), room.getLastCard(), room.getLastCard()));
        gzMjDeskCard.setCardType(GzMjConstant.GZMJ_DESK_CARD_TYPE_MG);
        gzMjDeskCard.setFrom(room.getLastAccount());
        // 添加桌面牌
        room.getUserPacketMap().get(account).getDeskCardList().add(gzMjDeskCard);
        // 移除手牌
        room.getUserPacketMap().get(account).getHandCardList().remove(room.getLastCard());
        room.getUserPacketMap().get(account).getHandCardList().remove(room.getLastCard());
        room.getUserPacketMap().get(account).getHandCardList().remove(room.getLastCard());
        // 移除玩家已出牌记录
        room.getUserPacketMap().get(room.getLastAccount()).getOutCardList().remove(room.getLastCard());
        // 设置上个操作玩家
        room.setLastAccount(account);
        room.setFocusIndex(room.getPlayerMap().get(account).getMyIndex());
        ArrayList<Integer> cardList = new ArrayList<>();
        cardList.add(room.getLastCard());
        cardList.add(room.getLastCard());
        cardList.add(room.getLastCard());
        cardList.add(room.getLastCard());
        // 杠摸牌
        moPai(room, account, cardList, GzMjConstant.GZMJ_CHANGE_TYPE_MG_RESULT);
    }

    private void buGang(GzMjGameRoom room, String account) {
        for (GzMjDeskCard gzMjDeskCard : room.getUserPacketMap().get(account).getDeskCardList()) {
            if (gzMjDeskCard.getCardList().contains(room.getCurrentCard())) {
                gzMjDeskCard.getCardList().add(room.getCurrentCard());
                gzMjDeskCard.setCardType(GzMjConstant.GZMJ_DESK_CARD_TYPE_BG);
                gzMjDeskCard.setFrom(account);
            }
        }
        room.setLastAccount(account);
        room.setFocusIndex(room.getPlayerMap().get(account).getMyIndex());
        // 移除手牌
        room.getUserPacketMap().get(account).getHandCardList().remove(room.getCurrentCard());
        ArrayList<Integer> cardList = new ArrayList<>();
        cardList.add(room.getCurrentCard());
        cardList.add(room.getCurrentCard());
        cardList.add(room.getCurrentCard());
        cardList.add(room.getCurrentCard());
        // 杠摸牌
        moPai(room, account, cardList, GzMjConstant.GZMJ_CHANGE_TYPE_BG_RESULT);
    }

    private void hu(GzMjGameRoom room, List<String> huList) {
        summaryHuScore(room, huList);
        // 计算鸡分
        summaryChookScore(room);
        // 计算杠分
        summaryGangScore(room);
        // 计算估卖
        summaryBetScore(room, huList);
        // 计算玩家最大分数
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().get(account).getScore() >= room.getUserPacketMap().get(account).getMaxScore()) {
                room.getUserPacketMap().get(account).setMaxScore(room.getUserPacketMap().get(account).getScore());
            }
        }
        // 通知玩家
        room.setGameStatus(GzMjConstant.GZMJ_GAME_STATUS_SUMMARY);
        if (room.getGameCount() == room.getGameIndex()) {
            room.setGameStatus(GzMjConstant.GZMJ_GAME_STATUS_FINAL_SUMMARY);
        }
        changeGameStatusInform(room.getRoomNo());
    }

    private void summaryHuScore(GzMjGameRoom room, List<String> huList) {
        Integer card = room.getLastCard();
        // 是否是自摸
        boolean isZiMo = false;
        // 自摸
        if (huList.size() == 1 && StringUtils.equals(room.getLastMoAccount(), huList.get(0))) {
            card = room.getCurrentCard();
            isZiMo = true;
        }
        room.setHuCard(card);
        // 计算胡分
        for (String huAccount : huList) {
            List<Integer> handCardList = new ArrayList<>(room.getUserPacketMap().get(huAccount).getHandCardList());
            // 自摸需要移除手牌后计算
            if (isZiMo) {
                handCardList.remove(card);
            }
            // 胡牌类型
            int huType = GzMjCore.getHuType(handCardList, room.getUserPacketMap().get(huAccount).getDeskCardList(), card);
            int score = getHuScore(huType);
            for (String account : room.getUserPacketMap().keySet()) {
                if (StringUtils.equals(huAccount, account)) {
                    room.getUserPacketMap().get(account).setHuType(huType);
                    room.getUserPacketMap().get(account).setWinMode(isZiMo ? GzMjConstant.GZMJ_WIN_MODE_ZM : GzMjConstant.GZMJ_WIN_MODE_JP);
                    addUserSummaryDetail(room, account, score * (room.getUserPacketMap().size() - 1), "胡分", GzMjConstant.GZMJ_SUMMARY_DETAIL_TYPE_HU);
                } else if (isZiMo) {
                    addUserSummaryDetail(room, account, -score, "胡分", GzMjConstant.GZMJ_SUMMARY_DETAIL_TYPE_HU);
                } else if (StringUtils.equals(room.getLastAccount(), account)) {
                    room.getUserPacketMap().get(account).setWinMode(GzMjConstant.GZMJ_WIN_MODE_DP);
                    addUserSummaryDetail(room, account, -score, "胡分", GzMjConstant.GZMJ_SUMMARY_DETAIL_TYPE_HU);
                }
            }
        }
        // 增加点炮次数
        if (!isZiMo) {
            room.getUserPacketMap().get(room.getLastAccount()).setDpTime(room.getUserPacketMap().get(room.getLastAccount()).getDpTime() + 1);
        }
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().get(account).getHuType() != GzMjConstant.GZMJ_HU_TYPE_NONE) {
                // 增加胡牌次数
                room.getUserPacketMap().get(account).setHuTime(room.getUserPacketMap().get(account).getHuTime() + 1);
                // 增加大胡次数
                if (room.getUserPacketMap().get(account).getHuType() != GzMjConstant.GZMJ_HU_TYPE_PH) {
                    room.getUserPacketMap().get(account).setMultipleTime(room.getUserPacketMap().get(account).getMultipleTime() + 1);
                }
            }
        }
    }

    private void summaryChookScore(GzMjGameRoom room) {
        for (String account : room.getUserPacketMap().keySet()) {
            Map<Integer, JSONObject> chookMap = getInitChookMap(room);
            // 手牌
            for (Integer handCard : room.getUserPacketMap().get(account).getHandCardList()) {
                if (chookMap.containsKey(handCard)) {
                    chookMap.put(handCard, addChookNum(chookMap.get(handCard), 1));
                }
            }
            // 桌面上的牌
            for (GzMjDeskCard gzMjDeskCard : room.getUserPacketMap().get(account).getDeskCardList()) {
                if (gzMjDeskCard.getCardList().size() > 0 && chookMap.containsKey(gzMjDeskCard.getCardList().get(0))) {
                    chookMap.put(gzMjDeskCard.getCardList().get(0), addChookNum(chookMap.get(gzMjDeskCard.getCardList().get(0)), gzMjDeskCard.getCardList().size()));
                }
            }
            // 出的牌
            for (Integer outCard : room.getUserPacketMap().get(account).getOutCardList()) {
                if (chookMap.containsKey(outCard)) {
                    chookMap.put(outCard, addChookNum(chookMap.get(outCard), 1));
                }
            }
            for (Integer chook : chookMap.keySet()) {
                if (chookMap.get(chook).getInt("count") == 0) {
                    continue;
                }
                for (String other : room.getUserPacketMap().keySet()) {
                    if (StringUtils.equals(other, account)) {
                        addUserSummaryDetail(room, account,
                                chookMap.get(chook).getInt("count") * (room.getUserPacketMap().size() - 1),
                                chookMap.get(chook).getString("name"), GzMjConstant.GZMJ_SUMMARY_DETAIL_TYPE_JI);
                    } else {
                        addUserSummaryDetail(room, other, -chookMap.get(chook).getInt("count"), chookMap.get(chook).getString("name"), GzMjConstant.GZMJ_SUMMARY_DETAIL_TYPE_JI);
                    }
                }
            }
        }
    }

    private void summaryGangScore(GzMjGameRoom room) {
        for (String account : room.getUserPacketMap().keySet()) {
            // 桌面上的牌
            for (GzMjDeskCard gzMjDeskCard : room.getUserPacketMap().get(account).getDeskCardList()) {
                if (gzMjDeskCard.getCardType() == GzMjConstant.GZMJ_DESK_CARD_TYPE_AG) {
                    // 暗杠多家输赢
                    for (String other : room.getUserPacketMap().keySet()) {
                        if (StringUtils.equals(other, account)) {
                            addUserSummaryDetail(room, account, 3 * (room.getUserPacketMap().size() - 1), "闷豆", GzMjConstant.GZMJ_SUMMARY_DETAIL_TYPE_GANG);
                        } else {
                            addUserSummaryDetail(room, other, -3, "闷豆", GzMjConstant.GZMJ_SUMMARY_DETAIL_TYPE_GANG);
                        }
                    }
                } else if (gzMjDeskCard.getCardType() == GzMjConstant.GZMJ_DESK_CARD_TYPE_MG) {
                    // 明杠只有一家输赢
                    addUserSummaryDetail(room, account, 3, "点杠", GzMjConstant.GZMJ_SUMMARY_DETAIL_TYPE_GANG);
                    addUserSummaryDetail(room, gzMjDeskCard.getFrom(), -3, "点杠", GzMjConstant.GZMJ_SUMMARY_DETAIL_TYPE_GANG);
                } else if (gzMjDeskCard.getCardType() == GzMjConstant.GZMJ_DESK_CARD_TYPE_BG) {
                    // 补杠多家输赢
                    for (String other : room.getUserPacketMap().keySet()) {
                        if (StringUtils.equals(other, account)) {
                            addUserSummaryDetail(room, account, 3 * (room.getUserPacketMap().size() - 1), "转弯豆", GzMjConstant.GZMJ_SUMMARY_DETAIL_TYPE_GANG);
                        } else {
                            addUserSummaryDetail(room, other, -3, "转弯豆", GzMjConstant.GZMJ_SUMMARY_DETAIL_TYPE_GANG);
                        }
                    }
                }
            }
        }
    }

    private void summaryBetScore(GzMjGameRoom room, List<String> huList) {
        for (String huAccount : huList) {
            int betTime = room.getUserPacketMap().get(huAccount).getBetTime();
            for (String account : room.getUserPacketMap().keySet()) {
                if (!StringUtils.equals(huAccount, account)) {
                    int otherBetTime = room.getUserPacketMap().get(account).getBetTime();
                    addUserSummaryDetail(room, huAccount, betTime + otherBetTime, "估卖", GzMjConstant.GZMJ_SUMMARY_DETAIL_TYPE_BET);
                    addUserSummaryDetail(room, account, -(betTime + otherBetTime), "估卖", GzMjConstant.GZMJ_SUMMARY_DETAIL_TYPE_BET);
                }
            }
        }
    }

    private void addUserSummaryDetail(GzMjGameRoom room, String account, int score, String detailName, int type) {
        room.getUserPacketMap().get(account).setScore(Dto.add(room.getUserPacketMap().get(account).getScore(), score));
        room.getPlayerMap().get(account).setScore(Dto.add(room.getPlayerMap().get(account).getScore(), score));
        // 设置胡分
        if (type == GzMjConstant.GZMJ_SUMMARY_DETAIL_TYPE_HU) {
            room.getUserPacketMap().get(account).setHuScore(Dto.add(room.getUserPacketMap().get(account).getHuScore(), score));
        }
        // 设置鸡分
        if (type == GzMjConstant.GZMJ_SUMMARY_DETAIL_TYPE_JI) {
            room.getUserPacketMap().get(account).setChookScore(Dto.add(room.getUserPacketMap().get(account).getChookScore(), score));
        }
        // 设置杠分
        if (type == GzMjConstant.GZMJ_SUMMARY_DETAIL_TYPE_GANG) {
            room.getUserPacketMap().get(account).setGangScore(Dto.add(room.getUserPacketMap().get(account).getGangScore(), score));
        }
        // 合并相同类型分数
        boolean containsDetail = false;
        for (GzMjSummaryDetail gzMjSummaryDetail : room.getUserPacketMap().get(account).getDetailList()) {
            if (StringUtils.equals(detailName, gzMjSummaryDetail.getDetailName())) {
                gzMjSummaryDetail.setDetailScore(String.valueOf(Integer.parseInt(gzMjSummaryDetail.getDetailScore()) + score));
                containsDetail = true;
                break;
            }
        }

        if (!containsDetail) {
            // 设置积分
            GzMjSummaryDetail gzMjSummaryDetail = new GzMjSummaryDetail();
            gzMjSummaryDetail.setDetailName(detailName);
            gzMjSummaryDetail.setDetailScore(String.valueOf(score));
            room.getUserPacketMap().get(account).getDetailList().add(gzMjSummaryDetail);
        }
    }

    private JSONObject addChookNum(JSONObject chookInfo, int addCount) {
        chookInfo.put("name", chookInfo.getString("name"));
        chookInfo.put("count", chookInfo.getInt("count") + addCount);
        return chookInfo;
    }

    private Map<Integer, JSONObject> getInitChookMap(GzMjGameRoom room) {
        Map<Integer, JSONObject> chookMap = new HashMap<>();
        // 满堂鸡
        chookMap.put(31, new JSONObject().element("name", "满堂鸡").element("count", 0));
        // 乌骨鸡
        if (room.isHasWgChock()) {
            chookMap.put(18, new JSONObject().element("name", "乌骨鸡").element("count", 0));
        }
        // 翻牌鸡
        chookMap.put(getChookCard(room), new JSONObject().element("name", "翻牌鸡").element("count", 0));
        // 星期鸡
        if (room.isHasWgChock()) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(new Date());
            // 计算当前星期
            int week = instance.get(Calendar.DAY_OF_WEEK);
            Map<Integer, List<Integer>> weekChook = new HashMap<>();
            weekChook.put(1, new ArrayList<>(Arrays.asList(17, 27, 37)));
            weekChook.put(2, new ArrayList<>(Arrays.asList(11, 21, 31)));
            weekChook.put(3, new ArrayList<>(Arrays.asList(12, 22, 32)));
            weekChook.put(4, new ArrayList<>(Arrays.asList(13, 23, 33)));
            weekChook.put(5, new ArrayList<>(Arrays.asList(14, 24, 34)));
            weekChook.put(6, new ArrayList<>(Arrays.asList(15, 25, 35)));
            weekChook.put(7, new ArrayList<>(Arrays.asList(16, 26, 36)));
            if (weekChook.get(week) != null) {
                for (Integer chook : weekChook.get(week)) {
                    // 如果已是其他类型鸡牌则不展示
                    if (chookMap.containsKey(chook)) {
                        continue;
                    }
                    chookMap.put(chook, new JSONObject().element("name", "星期鸡").element("count", 0));
                }
            }
        }
        return chookMap;
    }

    private int getHuScore(int huType) {
        switch (huType) {
            case GzMjConstant.GZMJ_HU_TYPE_PH:
                return 1;
            case GzMjConstant.GZMJ_HU_TYPE_DDZ:
                return 5;
            case GzMjConstant.GZMJ_HU_TYPE_XQD:
                return 10;
            case GzMjConstant.GZMJ_HU_TYPE_QYS:
                return 10;
            case GzMjConstant.GZMJ_HU_TYPE_QDD:
                return 15;
            case GzMjConstant.GZMJ_HU_TYPE_QQD:
                return 20;
            case GzMjConstant.GZMJ_HU_TYPE_LQD:
                return 20;
            case GzMjConstant.GZMJ_HU_TYPE_QLQD:
                return 40;
            case GzMjConstant.GZMJ_HU_TYPE_DL:
                return 5;
            default:
                return 0;
        }
    }

    private void guo(GzMjGameRoom room, String account) {
        // 点完过之后上一张牌置0，防止重复出两次
        room.setLastCard(0);
        // 轮到自己时过则出牌，否则下家摸牌
        if (!StringUtils.equals(account, room.getLastAccount())) {
            String nextPlayer = getNextPlayer(room, room.getLastAccount());
            // 3n+1张时摸牌
            if (StringUtils.isNotBlank(nextPlayer) && room.getUserPacketMap().get(nextPlayer) != null
                    && room.getUserPacketMap().get(nextPlayer).getHandCardList().size() % 3 == 1) {
                moPai(room, nextPlayer, new ArrayList<Integer>(), GzMjConstant.GZMJ_CHANGE_TYPE_MP);
            }
        }
    }

    private void moPai(GzMjGameRoom room, String nextPlayer, List<Integer> cardList, int changeType) {
        // 流局
        if (room.getCards().size() == 0) {
            room.setGameStatus(GzMjConstant.GZMJ_GAME_STATUS_SUMMARY);
            changeGameStatusInform(room.getRoomNo());
            return;
        }
        if (StringUtils.isNotBlank(nextPlayer)) {
            Integer newCard = room.getCards().get(0);
            if (GzMjCore.getCardColor(newCard) == room.getUserPacketMap().get(nextPlayer).getUserLack()) {
                room.getUserPacketMap().get(nextPlayer).setActionList(new ArrayList<Integer>());
            } else {
                // 计算新摸牌的事件
                List<Integer> actionList = GzMjCore.getActionList(room.getUserPacketMap().get(nextPlayer).getHandCardList()
                        , room.getUserPacketMap().get(nextPlayer).getDeskCardList(), newCard, false);
                room.getUserPacketMap().get(nextPlayer).setActionList(actionList);
            }
            // 移除牌
            room.getCards().remove(0);
            room.setCurrentCard(newCard);
            // 添加到玩家手牌
            room.getUserPacketMap().get(nextPlayer).getHandCardList().add(newCard);
            room.setFocusIndex(room.getPlayerMap().get(nextPlayer).getMyIndex());
            // 设置上个摸牌玩家
            room.setLastMoAccount(nextPlayer);
        }
        focusChangeInform(room.getRoomNo(), changeType, nextPlayer, cardList);
    }

    private void focusChangeInform(String roomNo, int changeType, String myself, List<Integer> cardList) {
        GzMjGameRoom room = (GzMjGameRoom) RoomManage.gameRoomMap.get(roomNo);
        room.setChangeType(changeType);
        for (String account : room.getPlayerMap().keySet()) {
            JSONObject result = new JSONObject();
            result.put("changeType", changeType);
            // 上一个操作玩家
            result.put("lastIndex", StringUtils.isBlank(room.getLastAccount()) ? -1 : room.getPlayerMap().get(room.getLastAccount()).getMyIndex());
            // 上个玩家出的牌
            result.put("lastCard", room.getLastCard());
            // 有点鸡肋
            result.put("focusIndex", room.getFocusIndex());
            // 收到此通知的玩家可选操作 胡碰杠过 等
            result.put("actionList", getActionList(room, account));
            // 剩余牌数
            result.put("leftCardNum", room.getCards().size());
            // 以下通知出牌时有
            if (StringUtils.equals(myself, account)) {
                // 当前牌，摸牌时摸到的牌
                result.put("currentCard", room.getCurrentCard());
                // 听牌
                result.put("tingTip", getTingTip(room, myself));
                // 缺牌
                result.put("lackCardList", getLackCardList(room, myself));
            }
            // 碰 杠的牌
            result.put("cardList", cardList);
            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(account).getUuid(), String.valueOf(result), "focusChangePush_GZMJ");
        }
    }

    private void changeGameStatusInform(String roomNo) {
        GzMjGameRoom room = (GzMjGameRoom) RoomManage.gameRoomMap.get(roomNo);
        for (String account : room.getPlayerMap().keySet()) {
            JSONObject result = new JSONObject();
            // 游戏状态
            result.put("gameStatus", room.getGameStatus());
            // 游戏局数
            result.put("gameIndex", room.getGameNewIndex());
            // 庄家座位号
            result.put("bankerIndex", getBankerIndex(room));
            // 当前操作玩家座位号
            result.put("focusIndex", room.getFocusIndex());
            // 总牌数
            result.put("totalCardNum", room.getTotalCardNum());
            // 剩余牌数
            result.put("leftCardNum", room.getCards().size());
            // 是否展示定时器
            result.put("showTimer", room.getTimeLeft() > 0 ? CommonConstant.GLOBAL_YES : CommonConstant.GLOBAL_NO);
            // 倒计时时间
            result.put("timer", room.getTimeLeft() > 0);
            // 估卖结果
            result.put("betResult", getBetResult(room));
            // 可供选择的估卖倍数
            result.put("betList", getBetList(room, account));
            // 最佳缺牌
            result.put("minLack", getMinLack(room, account));
            // 缺牌结果
            result.put("lackResult", getLackResult(room));
            // 缺牌集合(此类牌必须先出)
            result.put("lackCardList", getLackCardList(room, account));
            // 听牌提示
            result.put("tingTip", getTingTip(room, account));
            // 玩家手牌
            result.put("handCardList", getHandCardList(room, account));
            // 玩家是否可操作
            result.put("actionList", getActionList(room, account));
            // 翻牌
            result.put("flipCard", getFlipCard(room));
            // 翻牌鸡
            result.put("chookCard", getChookCard(room));
            // 结算数据
            result.put("summaryData", getSummaryDataList(room, account));
            // 总结算数据
            result.put("finalSummaryData", getFinalSummaryDataList(room));
            result.put("isLiuJu", room.getCards().size() == 0 ? CommonConstant.GLOBAL_YES : CommonConstant.GLOBAL_NO);
            result.put("isFinish", room.isFinish() ? CommonConstant.GLOBAL_YES : CommonConstant.GLOBAL_NO);
            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(account).getUuid(), String.valueOf(result), "changeGameStatusPush_GZMJ");
        }
    }

    private void startGame(GzMjGameRoom room) {
        final String roomNo = room.getRoomNo();
        initRoom(room);
        if (room.isHasBet() && room.getGameStatus() != GzMjConstant.GZMJ_GAME_STATUS_BET) {
            // 估卖
            room.setGameStatus(GzMjConstant.GZMJ_GAME_STATUS_BET);
            // 开启定时器
            ThreadPoolHelper.executorService.submit(new Runnable() {
                @Override
                public void run() {
                    gameTimerGzMj.gameOverTime(roomNo, GzMjConstant.GZMJ_GAME_STATUS_BET, GzMjConstant.GZMJ_TIMER_BET);
                }
            });
            changeGameStatusInform(room.getRoomNo());
        } else if (!checkChooseLack(room)) {
            // 发牌
            shuffleAndDeal(room);
            // 定缺
            room.setGameStatus(GzMjConstant.GZMJ_GAME_STATUS_CHOOSE_LACK);
            // 开启定时器
            ThreadPoolHelper.executorService.submit(new Runnable() {
                @Override
                public void run() {
                    gameTimerGzMj.gameOverTime(roomNo, GzMjConstant.GZMJ_GAME_STATUS_CHOOSE_LACK, GzMjConstant.GZMJ_TIMER_CHOOSE_LACK);
                }
            });
            changeGameStatusInform(room.getRoomNo());
        } else {
            // 发牌
            shuffleAndDeal(room);
            // 设置房间状态
            room.setGameStatus(GzMjConstant.GZMJ_GAME_STATUS_GAME);
            // 通知玩家
            changeGameStatusInform(room.getRoomNo());
        }
    }

    private void initRoom(GzMjGameRoom room) {
        if (room.getGameStatus() != GzMjConstant.GZMJ_GAME_STATUS_READY && room.getGameStatus() != GzMjConstant.GZMJ_GAME_STATUS_INIT) {
            return;
        }
        // 确定庄家
        if (room.getLastHuAccount().size() == 0) {
            // 无人胡且无庄家则随机庄家
            if (StringUtils.isBlank(room.getBanker())) {
                for (String account : room.getUserPacketMap().keySet()) {
                    room.setBanker(account);
                    break;
                }
            }
        } else if (room.getLastHuAccount().size() == 1) {
            // 单人胡则胡的人坐庄
            room.setBanker(room.getLastHuAccount().get(0));
        } else {
            // 多人胡上个出牌坐庄
            room.setBanker(room.getLastAccount());
        }
        room.addGameIndex();
        room.addGameNewIndex();
        room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_INIT);
        room.setHuCard(0);
        // 设置上个摸牌玩家
        room.setLastMoAccount(room.getBanker());
        // 设置focusIndex
        room.setFocusIndex(room.getPlayerMap().get(room.getBanker()).getMyIndex());
        // 设置changeType
        room.setChangeType(GzMjConstant.GZMJ_CHANGE_TYPE_MP);
        // 初始化玩家信息
        for (String account : room.getUserPacketMap().keySet()) {
            room.getUserPacketMap().get(account).setStatus(GzMjConstant.GZMJ_USER_STATUS_GAME);
            room.getUserPacketMap().get(account).setScore(0D);
            room.getUserPacketMap().get(account).setHuScore(0D);
            room.getUserPacketMap().get(account).setChookScore(0D);
            room.getUserPacketMap().get(account).setGangScore(0D);
            room.getUserPacketMap().get(account).setBetTime(-1);
            room.getUserPacketMap().get(account).setUserLack(0);
            room.getUserPacketMap().get(account).setOutCardList(new ArrayList<>());
            room.getUserPacketMap().get(account).setDeskCardList(new ArrayList<>());
            room.getPlayerMap().get(account).addPlayTimes();//添加游戏局数
            room.getUserPacketMap().get(account).setDetailList(new ArrayList<>());
            room.getUserPacketMap().get(account).setWinMode(0);
            room.getUserPacketMap().get(account).setHuType(0);
        }
    }

    private void shuffleAndDeal(GzMjGameRoom room) {
        // 所有牌
        List<Integer> allCard = GzMjCore.getAllCard(room.isHasTwo());
        int cardIndex = 0;
        // 玩家初始手牌
        for (String account : room.getUserPacketMap().keySet()) {
            int cardCount = 13;
            // 庄家多摸一张牌
            if (StringUtils.equals(account, room.getBanker())) {
                cardCount = 14;
            }
            List<Integer> allCardTemp = new ArrayList<>(allCard);
            room.getUserPacketMap().get(account).setHandCardList(allCardTemp.subList(cardIndex, cardIndex + cardCount));
            cardIndex += cardCount;
        }
        List<Integer> allCardTemp = new ArrayList<>(allCard);
        // 设置房间牌
        room.setCards(allCardTemp.subList(cardIndex + 1, allCardTemp.size()));
    }


    private boolean checkChooseLack(GzMjGameRoom room) {
        // 两房或者房间满人不需要定缺
        return room.isHasTwo() || room.getPlayerCount() == GzMjConstant.GZMJ_MAX_PLAY_COUNT;
    }


    private boolean checkAllAndUserStatus(GzMjGameRoom room, int userStatus) {
        // 房间是否已经满人
        if (room.getUserPacketMap().size() != room.getPlayerCount()) {
            return false;
        }
        for (String account : room.getUserPacketMap().keySet()) {
            // 如有用户状态不符合，则直接返回false
            if (room.getUserPacketMap().get(account).getStatus() != userStatus) {
                return false;
            }
        }
        return true;
    }

    private JSONObject getRoomData(String roomNo, String account) {
        GzMjGameRoom room = (GzMjGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONObject roomData = new JSONObject();
        roomData.put("room_no", roomNo);
        roomData.put("myIndex", room.getPlayerMap().get(account).getMyIndex());
        roomData.put("roomType", room.getRoomType());
        roomData.put("game_count", room.getGameCount());
        roomData.put("di", room.getScore());
        roomData.put("clubCode", room.getCircleId());
        roomData.put("roomInfo", new JSONObject());
        // 游戏状态
        roomData.put("gameStatus", room.getGameStatus());
        // 游戏局数
        roomData.put("gameIndex", room.getGameNewIndex());
        // 庄家座位号
        roomData.put("bankerIndex", getBankerIndex(room));
        // 当前操作玩家座位号
        roomData.put("focusIndex", room.getFocusIndex());
        // 上一次操作玩家座位号
        if (!Dto.stringIsNULL(room.getLastAccount()) && !Dto.isNull(room.getPlayerMap().get(room.getLastAccount()))) {
            roomData.put("lastIndex", room.getPlayerMap().get(room.getLastAccount()).getMyIndex());
        }
        // 总牌数
        roomData.put("totalCardNum", room.getTotalCardNum());
        // 剩余牌数
        roomData.put("leftCardNum", room.getCards().size());
        // 是否展示定时器
        roomData.put("showTimer", room.getTimeLeft() > 0 ? CommonConstant.GLOBAL_YES : CommonConstant.GLOBAL_NO);
        // 倒计时时间
        roomData.put("timer", room.getTimeLeft() > 0);
        // 估卖结果
        roomData.put("betResult", getBetResult(room));
        // 可供选择的估卖倍数
        roomData.put("betList", getBetList(room, account));
        // 最佳缺牌
        roomData.put("minLack", getMinLack(room, account));
        // 缺牌结果
        roomData.put("lackResult", getLackResult(room));
        // 缺牌集合(此类牌必须先出)
        roomData.put("lackCardList", getLackCardList(room, account));
        // 听牌提示
        roomData.put("tingTip", getTingTip(room, account));
        // 玩家手牌
        roomData.put("handCardList", getHandCardList(room, account));
        // 玩家是否可操作
        roomData.put("actionList", getActionList(room, account));
        // 翻牌
        roomData.put("flipCard", getFlipCard(room));
        // 翻牌鸡
        roomData.put("chookCard", getChookCard(room));
        // 结算数据
        roomData.put("summaryData", getSummaryDataList(room, account));
        // 总结算数据
        roomData.put("finalSummaryData", getFinalSummaryDataList(room));
        roomData.put("users", getAllPlayer(room));
        // 当前牌，摸牌时摸到的牌
        roomData.put("lastCard", room.getLastCard());
        roomData.put("changeType", room.getChangeType());
        return roomData;
    }


    public JSONArray getAllPlayer(GzMjGameRoom room) {
        JSONArray array = new JSONArray();

        for (String account : room.getUserPacketMap().keySet()) {
            Playerinfo player = room.getPlayerMap().get(account);
            if (player != null) {
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
                obj.put("userStatus", room.getUserPacketMap().get(account).getStatus());
                array.add(obj);
            }
        }
        return array;
    }


    private String getNextPlayer(GzMjGameRoom room, String account) {
        int index = room.getPlayerMap().get(account).getMyIndex();
        // 两人局，坐对面
        int next = room.getPlayerCount() == 2 ? (index + 2) % 4 : (index + 1) % room.getPlayerCount();
        for (String player : room.getPlayerMap().keySet()) {
            if (room.getPlayerMap().get(player).getMyIndex() == next) {
                return player;
            }
        }
        return null;
    }


    private int getFlipCard(GzMjGameRoom room) {
        return room.getCards().size() > 0 ? room.getCards().get(0) : -1;
    }

    private int getChookCard(GzMjGameRoom room) {
        // 翻牌鸡
        int flipCard = getFlipCard(room);
        // 未选择本鸡时进行计算
        return room.isHasSelfChock() ? flipCard : GzMjCore.getNextCard(flipCard);
    }


    private List<Integer> getActionList(GzMjGameRoom room, String account) {
        if (!room.getUserPacketMap().containsKey(account) || room.getUserPacketMap().get(account) == null) {
            return new ArrayList<>();
        }
        return room.getUserPacketMap().get(account).getActionList();
    }


    private List<Integer> getLackCardList(GzMjGameRoom room, String account) {
        List<Integer> lackCardList = new ArrayList<>();
        if (room.getUserPacketMap().get(account) != null) {
            // 手牌
            List<Integer> handCardList = room.getUserPacketMap().get(account).getHandCardList();
            for (Integer handCard : handCardList) {
                // 缺牌
                if (GzMjCore.getCardColor(handCard) == room.getUserPacketMap().get(account).getUserLack()) {
                    lackCardList.add(handCard);
                }
            }
        }

        return lackCardList;
    }


    private int getMinLack(GzMjGameRoom room, String account) {
        if (!room.getUserPacketMap().containsKey(account) || room.getUserPacketMap().get(account) == null) {
            return 0;
        }
        // 手牌
        List<Integer> handCardList = room.getUserPacketMap().get(account).getHandCardList();
        // 最小张数，初始值给14为最大手牌数
        int minCount = 14;
        // 最小花色数量
        int minColor = 0;
        // 统计花色
        Map<Integer, Integer> countMap = new HashMap<>();
        countMap.put(GzMjConstant.GZMJ_CARD_COLOR_TONG, 0);
        countMap.put(GzMjConstant.GZMJ_CARD_COLOR_WANG, 0);
        countMap.put(GzMjConstant.GZMJ_CARD_COLOR_TIAO, 0);
        for (Integer handCard : handCardList) {
            int cardColor = GzMjCore.getCardColor(handCard);
            if (countMap.containsKey(cardColor)) {
                countMap.put(cardColor, countMap.get(cardColor) + 1);
            } else {
                countMap.put(cardColor, 1);
            }
        }
        for (Integer color : countMap.keySet()) {
            // 去最少花色
            if (countMap.get(color) < minCount) {
                minColor = color;
                minCount = countMap.get(minColor);
            }
        }
        return minColor;
    }


    private int getBankerIndex(GzMjGameRoom room) {
        return StringUtils.isNotBlank(room.getBanker()) && room.getPlayerMap().get(room.getBanker()) != null ?
                room.getPlayerMap().get(room.getBanker()).getMyIndex() : -1;
    }

    private List<JSONObject> getBetList(GzMjGameRoom room, String myself) {
        List<JSONObject> betList = new ArrayList<>();
        if (room.isHasBet()) {
            for (JSONObject betInfo : room.getBetList()) {
                betInfo.put("isUse", CommonConstant.GLOBAL_YES);
                betList.add(betInfo);
            }
        }
        return betList;
    }

    private JSONObject getBetResult(GzMjGameRoom room) {
        JSONObject betResult = new JSONObject();
        for (String account : room.getUserPacketMap().keySet()) {
            // 玩家座位号，估卖结果
            betResult.put(room.getPlayerMap().get(account).getMyIndex(), room.getUserPacketMap().get(account).getBetTime());
        }
        return betResult;
    }


    private JSONObject getLackResult(GzMjGameRoom room) {
        JSONObject lackResult = new JSONObject();
        for (String account : room.getUserPacketMap().keySet()) {
            // 玩家座位号，定缺结果
            lackResult.put(room.getPlayerMap().get(account).getMyIndex(), room.getUserPacketMap().get(account).getUserLack());
        }
        return lackResult;
    }


    private JSONObject getHandCardList(GzMjGameRoom room, String myself) {
        JSONObject handCardListResult = new JSONObject();
        for (String account : room.getUserPacketMap().keySet()) {
            // 获取玩家自己的手牌或准备、结算、总结算阶段手牌可见
            if (StringUtils.equals(myself, account)
                    || room.getGameStatus() == GzMjConstant.GZMJ_GAME_STATUS_READY
                    || room.getGameStatus() == GzMjConstant.GZMJ_GAME_STATUS_SUMMARY
                    || room.getGameStatus() == GzMjConstant.GZMJ_GAME_STATUS_FINAL_SUMMARY) {
                handCardListResult.put(room.getPlayerMap().get(account).getMyIndex(), room.getUserPacketMap().get(account).getHandCardList());
            } else {
                // 手牌不可见状态时传[0,0,0.....]
                List<Integer> handCardList = new ArrayList<>();
                for (int i = 0; i < room.getUserPacketMap().get(account).getHandCardList().size(); i++) {
                    handCardList.add(0);
                }
                handCardListResult.put(room.getPlayerMap().get(account).getMyIndex(), handCardList);
            }
        }
        return handCardListResult;
    }


    private JSONObject getOutCardList(GzMjGameRoom room) {
        JSONObject outCardListResult = new JSONObject();
        for (String account : room.getUserPacketMap().keySet()) {
            // 玩家座位号、玩家已出的牌
            outCardListResult.put(room.getPlayerMap().get(account).getMyIndex(), room.getUserPacketMap().get(account).getOutCardList());
        }
        return outCardListResult;
    }


    private JSONObject getDeskCardList(GzMjGameRoom room) {
        JSONObject deskCardListResult = new JSONObject();
        for (String account : room.getUserPacketMap().keySet()) {
            // 玩家座位号、玩家已经碰杠的牌
            deskCardListResult.put(room.getPlayerMap().get(account).getMyIndex(), deskCard2Json(room.getUserPacketMap().get(account).getDeskCardList()));
        }
        return deskCardListResult;
    }


    private List<JSONObject> getSummaryDataList(GzMjGameRoom room, String myself) {
        List<JSONObject> summaryDataList = new ArrayList<>();
        for (String account : room.getUserPacketMap().keySet()) {
            JSONObject summaryData = new JSONObject();
            if (room.getGameStatus() == GzMjConstant.GZMJ_GAME_STATUS_SUMMARY || room.getGameStatus() == GzMjConstant.GZMJ_GAME_STATUS_FINAL_SUMMARY
                    || StringUtils.equals(account, myself)) {
                summaryData.put("handCard", room.getUserPacketMap().get(account).getHandCardList());
            } else {
                List<Integer> handCandList = new ArrayList<>();
                for (int i = 0; i < room.getUserPacketMap().get(account).getHandCardList().size(); i++) {
                    handCandList.add(0);
                }
                summaryData.put("handCard", handCandList);
            }
            summaryData.put("outCard", room.getUserPacketMap().get(account).getOutCardList());
            summaryData.put("deskCard", deskCard2Json(room.getUserPacketMap().get(account).getDeskCardList()));
            summaryData.put("index", room.getPlayerMap().get(account).getMyIndex());
            if (room.getGameStatus() != GzMjConstant.GZMJ_GAME_STATUS_SUMMARY && room.getGameStatus() != GzMjConstant.GZMJ_GAME_STATUS_FINAL_SUMMARY) {
                summaryDataList.add(summaryData);
                continue;
            }
            summaryData.put("account", account);
            summaryData.put("name", room.getPlayerMap().get(account).getName());
            summaryData.put("headimg", room.getPlayerMap().get(account).getHeadimg());
            summaryData.put("isBanker", StringUtils.equals(room.getBanker(), account) ? CommonConstant.GLOBAL_YES : CommonConstant.GLOBAL_NO);
            summaryData.put("winMode", room.getUserPacketMap().get(account).getWinMode());
            summaryData.put("huType", room.getUserPacketMap().get(account).getHuType());
            summaryData.put("score", room.getUserPacketMap().get(account).getScore());
            summaryData.put("gangScore", room.getUserPacketMap().get(account).getGangScore());
            summaryData.put("chookScore", room.getUserPacketMap().get(account).getChookScore());
            summaryData.put("huScore", room.getUserPacketMap().get(account).getHuScore());
            summaryData.put("chookCard", room.getUserPacketMap().get(account).getChookCardList());
            summaryData.put("detail", summaryDetail2Json(room.getUserPacketMap().get(account).getDetailList()));
            summaryData.put("huCard", room.getHuCard());
            summaryDataList.add(summaryData);
        }
        return summaryDataList;
    }


    private List<JSONObject> getFinalSummaryDataList(GzMjGameRoom room) {
        List<JSONObject> finalSummaryDataList = new ArrayList<>();
        if (room.getGameStatus() != GzMjConstant.GZMJ_GAME_STATUS_FINAL_SUMMARY) {
            return finalSummaryDataList;
        }
        for (String account : room.getUserPacketMap().keySet()) {
            JSONObject finalSummaryData = new JSONObject();
            finalSummaryData.put("account", account);
            finalSummaryData.put("index", room.getPlayerMap().get(account).getMyIndex());
            finalSummaryData.put("name", room.getPlayerMap().get(account).getName());
            finalSummaryData.put("headimg", room.getPlayerMap().get(account).getHeadimg());
            finalSummaryData.put("isOwner", StringUtils.equals(room.getOwner(), account) ? CommonConstant.GLOBAL_YES : CommonConstant.GLOBAL_NO);
            finalSummaryData.put("score", room.getPlayerMap().get(account).getScore());
            finalSummaryData.put("huTime", room.getUserPacketMap().get(account).getHuTime());
            finalSummaryData.put("dpTime", room.getUserPacketMap().get(account).getDpTime());
            finalSummaryData.put("multipleTime", room.getUserPacketMap().get(account).getMultipleTime());
            finalSummaryData.put("maxScore", room.getUserPacketMap().get(account).getMaxScore());
            finalSummaryData.put("isFinish", room.isFinish() ? CommonConstant.GLOBAL_YES : CommonConstant.GLOBAL_NO);
            finalSummaryDataList.add(finalSummaryData);
        }
        return finalSummaryDataList;
    }


    private JSONObject getTingTip(GzMjGameRoom room, String myself) {
        if (!room.getUserPacketMap().containsKey(myself) || room.getUserPacketMap().get(myself) == null) {
            return new JSONObject();
        }
        return GzMjCore.getTingList(room.getUserPacketMap().get(myself).getHandCardList(), room.getUserPacketMap().get(myself).getDeskCardList());
    }


    private List<JSONObject> deskCard2Json(List<GzMjDeskCard> deskCards) {
        List<JSONObject> deskCardList = new ArrayList<>();
        // 遍历玩家所有已经碰杠的牌
        for (GzMjDeskCard gzMjDeskCard : deskCards) {
            JSONObject deskCard = new JSONObject();
            deskCard.put("cardType", gzMjDeskCard.getCardType());
            deskCard.put("cardList", gzMjDeskCard.getCardList());
            deskCard.put("from", gzMjDeskCard.getFrom());
            deskCardList.add(deskCard);
        }
        return deskCardList;
    }

    private List<JSONObject> summaryDetail2Json(List<GzMjSummaryDetail> summaryDetails) {
        List<JSONObject> deskCardList = new ArrayList<>();
        // 遍历玩家结算数据
        for (GzMjSummaryDetail gzMjSummaryDetail : summaryDetails) {
            JSONObject summaryDetail = new JSONObject();
            summaryDetail.put("detailName", gzMjSummaryDetail.getDetailName());
            summaryDetail.put("detailCardList", gzMjSummaryDetail.getDetailCardList());
            summaryDetail.put("detailScore", gzMjSummaryDetail.getDetailScore());
            deskCardList.add(summaryDetail);
        }
        return deskCardList;
    }
}
