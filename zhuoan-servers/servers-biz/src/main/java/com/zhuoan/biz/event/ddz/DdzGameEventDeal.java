package com.zhuoan.biz.event.ddz;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.ddz.DdzCore;
import com.zhuoan.biz.event.match.MatchEventDeal;
import com.zhuoan.biz.game.biz.AchievementBiz;
import com.zhuoan.biz.game.biz.PropsBiz;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.ddz.DdzGameRoom;
import com.zhuoan.biz.model.ddz.UserPacketDdz;
import com.zhuoan.biz.robot.RobotEventDeal;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.DaoTypeConstant;
import com.zhuoan.constant.DdzConstant;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.AppBeanUtil;
import com.zhuoan.util.Dto;
import com.zhuoan.util.TimeUtil;
import com.zhuoan.util.thread.ThreadPoolHelper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.*;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 13:47 2018/6/27
 * @Modified By:
 **/
@Component
public class DdzGameEventDeal {

    private final static Logger logger = LoggerFactory.getLogger(DdzGameEventDeal.class);
    @Resource
    private UserBiz userBiz;
    @Resource
    private RoomBiz roomBiz;
    @Resource
    private AchievementBiz achievementBiz;
    @Resource
    private PropsBiz propsBiz;
    @Resource
    private Destination daoQueueDestination;
    @Resource
    private ProducerService producerService;
    @Resource
    private RedisService redisService;
    @Resource
    private RedisInfoService redisInfoService;
    @Resource
    private GameTimerDdz gameTimerDdz;
    @Resource
    private MatchEventDeal matchEventDeal;

    public void createRoom(SocketIOClient client, Object data) {}


    public void joinRoom(SocketIOClient client, Object data) {}


    public void gameReady(SocketIOClient client, Object data) {}

    private void beginReadyTimer(final String roomNo, DdzGameRoom room) {}


    public void gameBeLandlord(SocketIOClient client, Object data) {}


    public void gameDouble(SocketIOClient client, Object data) {}


    private void sendDoubleResult(SocketIOClient client, String eventName, String msg) {}


    public void gameEvent(SocketIOClient client, Object data) {}


    private void matchContinue(final String roomNo) {}


    public void gamePrompt(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, DdzConstant.DDZ_GAME_STATUS_GAME_IN, client)) {
            return;
        }
        // 房间号
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        // 当前不是该玩家操作
        if (room.getPlayerMap().get(account).getMyIndex() != room.getFocusIndex()) {
            return;
        }
        JSONArray proList = new JSONArray();
        List<String> myPai = room.getUserPacketMap().get(account).getMyPai();
        // 还没出过牌或轮到自己出牌依次提示单牌、对子、三张、炸弹
        if (room.getLastCard().size() == 0 || account.equals(room.getLastOperateAccount())) {
            proList.addAll(changeFormat(DdzCore.obtainAllCard(new ArrayList<String>(), myPai)));
        } else {
            proList.addAll(changeFormat(DdzCore.obtainAllCard(room.getLastCard(), myPai)));
        }
        if (proList.size() > 0) {
            JSONObject result = new JSONObject();
            result.put("proList", proList);
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "gamePromptPush_DDZ");
        } else {
            postData.put("type", DdzConstant.DDZ_GAME_EVENT_TYPE_NO);
            postData.put("paiList", proList);
            gameEvent(null, postData);
        }
    }


    public void gameTrustee(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, DdzConstant.DDZ_GAME_STATUS_GAME_IN, client)) {
            return;
        }
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (!postData.containsKey(DdzConstant.DDZ_DATA_KEY_TYPE)) {
            return;
        }
        // 改变玩家托管状态
        int trustee = postData.getInt(DdzConstant.DDZ_DATA_KEY_TYPE);
        room.getUserPacketMap().get(account).setIsTrustee(trustee);
        JSONObject result = new JSONObject();
        result.put("num", room.getPlayerMap().get(account).getMyIndex());
        result.put("type", trustee);
        CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "gameTrusteePush_DDZ");
        // 自己出牌托管需要出牌
        if (trustee == CommonConstant.GLOBAL_YES && room.getPlayerMap().get(account).getMyIndex() == room.getFocusIndex()) {
            gameAutoPlay(null, postData);
        }
    }


    public void gameAutoPlay(SocketIOClient client, Object data) {
        if (client != null) {
            return;
        }
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, DdzConstant.DDZ_GAME_STATUS_GAME_IN, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        List<String> lastCard = room.getLastCard();
        if (room.getLastCard().size() == 0 || account.equals(room.getLastOperateAccount())) {
            lastCard.clear();
        }
        List<List<String>> allCard = DdzCore.obtainAllCard(lastCard, room.getUserPacketMap().get(account).getMyPai());
        if (room.getSetting().containsKey("trustee_pass") && lastCard.size() != 0) {
            postData.put(DdzConstant.DDZ_DATA_KEY_PAI_LIST, new ArrayList<String>());
            postData.put(DdzConstant.DDZ_DATA_KEY_TYPE, DdzConstant.DDZ_GAME_EVENT_TYPE_NO);
        } else if (allCard.size() > 0) {
            postData.put(DdzConstant.DDZ_DATA_KEY_PAI_LIST, allCard.get(0));
            postData.put(DdzConstant.DDZ_DATA_KEY_TYPE, DdzConstant.DDZ_GAME_EVENT_TYPE_YES);
        } else {
            postData.put(DdzConstant.DDZ_DATA_KEY_PAI_LIST, new ArrayList<String>());
            postData.put(DdzConstant.DDZ_DATA_KEY_TYPE, DdzConstant.DDZ_GAME_EVENT_TYPE_NO);
        }
        gameEvent(client, postData);
    }


    public void getOutInfo(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        boolean canExit = false;
        // 金币场、元宝场
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB || room.getRoomType() == CommonConstant.ROOM_TYPE_YB) {
            // 未参与游戏可以自由退出
            if (room.getUserPacketMap().get(account).getStatus() == DdzConstant.DDZ_USER_STATUS_INIT) {
                canExit = true;
            } else if (room.getGameStatus() == DdzConstant.DDZ_GAME_STATUS_INIT ||
                    room.getGameStatus() == DdzConstant.DDZ_GAME_STATUS_READY ||
                    room.getGameStatus() == DdzConstant.DDZ_GAME_STATUS_SUMMARY) {// 初始及准备阶段可以退出
                canExit = true;
            }
        }
        JSONObject result = new JSONObject();
        if (canExit) {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            if (!Dto.isObjNull(room.getWinStreakObj())) {
                JSONObject winStreakObj = room.getWinStreakObj();
                // 次数
                int time = winStreakObj.getInt("time");
                // 是否需要连胜
                int mustWin = winStreakObj.getInt("mustWin");
                int winTime = time - room.getUserPacketMap().get(account).getWinStreakTime();
                // 已经达到次数
                if (winTime <= 0) {
                    result.put("content", "当前可拆红包,确定退出");
                } else if (mustWin == CommonConstant.GLOBAL_YES) {
                    result.put("content", "再连胜" + winTime + "场可拆红包,确定退出");
                } else {
                    result.put("content", "再赢" + winTime + "场可拆红包,确定退出");
                }
            } else {
                result.put("content", "确定退出");
            }
        } else {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG, "正在游戏中,无法退出");
        }
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getOutInfoPush_DDZ");
    }


    public void exitRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        boolean canExit = false;
        // 金币场、元宝场
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB || room.getRoomType() == CommonConstant.ROOM_TYPE_YB) {
            // 未参与游戏可以自由退出
            if (room.getUserPacketMap().get(account).getStatus() == DdzConstant.DDZ_USER_STATUS_INIT) {
                canExit = true;
            } else if (room.getGameStatus() == DdzConstant.DDZ_GAME_STATUS_INIT ||
                    room.getGameStatus() == DdzConstant.DDZ_GAME_STATUS_READY ||
                    room.getGameStatus() == DdzConstant.DDZ_GAME_STATUS_SUMMARY) {// 初始及准备阶段可以退出
                canExit = true;
            }
        } else if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_DK ||
                room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
            // 总结算之后可以退出房间
            if (room.getGameStatus() == DdzConstant.DDZ_GAME_STATUS_FINAL_SUMMARY) {
                canExit = true;
            }
            if (room.getGameStatus() == DdzConstant.DDZ_GAME_STATUS_INIT || room.getGameStatus() == DdzConstant.DDZ_GAME_STATUS_READY) {
                if (room.getPlayerMap().get(account).getPlayTimes() == 0) {
                    if (CommonConstant.ROOM_PAY_TYPE_AA.equals(room.getPayType()) || !room.getOwner().equals(account)) {
                        canExit = true;
                    }
                }
            }
        } else if (room.getRoomType() == CommonConstant.ROOM_TYPE_MATCH) {
            Map<Object, Object> allPlayerInfo = redisService.hmget("player_info_" + room.getMatchNum());
            if (allPlayerInfo == null || !allPlayerInfo.containsKey(account)) {
                canExit = true;
            }
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
            if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB && room.getUserPacketMap().get(account).getWinStreakTime() >= 0) {
                redisService.hset("win_time_info_" + room.getScore(), account, String.valueOf(room.getUserPacketMap().get(account).getWinStreakTime()));
            }

            // 组织数据，通知玩家
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            result.put("type", 1);
            result.put("index", player.getMyIndex());
            if (!postData.containsKey("notSend")) {
                CommonConstant.sendMsgEventToAll(allUUIDList, result.toString(), "exitRoomPush_DDZ");
            }
            if (postData.containsKey("notSendToMe")) {
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "exitRoomPush_DDZ");
            }

            boolean isRobot = false;
            if (room.getRobotList().contains(account)) {
                isRobot = true;
            }
            // 所有人都退出清除房间数据
            if (room.getPlayerMap().size() == 0) {
                redisInfoService.delSummary(roomNo, "_DDZ");
                redisService.deleteByKey("startTimes_ddz:" + room.getRoomNo());
                roomInfo.put("status", room.getIsClose());
                roomInfo.put("game_index", room.getGameIndex());
                //亲友圈房间不清除
                if (CommonConstant.ROOM_TYPE_FREE == room.getRoomType() || CommonConstant.ROOM_TYPE_INNING == room.getRoomType()) {
                } else {
                    RoomManage.gameRoomMap.remove(room.getRoomNo());
                }
            } else if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
                for (String uuid : obtainAllPlayerAccount(roomNo)) {
                    if (!room.getRobotList().contains(uuid)) {
                        AppBeanUtil.getBean(RobotEventDeal.class).robotJoin(roomNo);
                        break;
                    }
                }
            }
            if (isRobot) {
                roomBiz.updateRobotStatus(account, 0);
            }

        } else {
            // 组织数据，通知玩家
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG, "当前无法退出,请发起解散");
            result.put("type", 1);
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "exitRoomPush_DDZ");
        }
    }


    public void closeRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (postData.containsKey("type")) {
            JSONObject result = new JSONObject();
            int type = postData.getInt("type");
            // 有人发起解散设置解散时间
            if (type == CommonConstant.CLOSE_ROOM_AGREE && room.getJieSanTime() == 0) {
                // 有人发起解散设置解散时间
                final int closeTime;
                if (room.getSetting().containsKey("closeTime")) {
                    closeTime = room.getSetting().getInt("closeTime");
                } else {
                    closeTime = 60;
                }
                room.setJieSanTime(closeTime);
                ThreadPoolHelper.executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        gameTimerDdz.closeRoomOverTime(roomNo, closeTime);
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
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "closeRoomPush_DDZ");
                return;
            }
            if (type == CommonConstant.CLOSE_ROOM_AGREE) {
                // 全部同意解散
                if (isAllAgreeClose(roomNo)) {
                    // 未玩完一局不需要强制结算
                    if (!room.isNeedFinalSummary()) {
                        // 所有玩家
                        List<UUID> uuidList = room.getAllUUIDList();
                        // 移除房间
                        RoomManage.gameRoomMap.remove(roomNo);
                        // 通知玩家
                        result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
                        result.put(CommonConstant.RESULT_KEY_MSG, "解散成功");
                        CommonConstant.sendMsgEventToAll(uuidList, String.valueOf(result), "tipMsgPush");
                        return;
                    }
                    if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
                        room.setOpen(false);
                    }
                    room.setGameStatus(DdzConstant.DDZ_GAME_STATUS_FINAL_SUMMARY);
                    changeGameStatus(roomNo);
                } else {// 刷新数据
                    room.getPlayerMap().get(account).setIsCloseRoom(CommonConstant.CLOSE_ROOM_AGREE);
                    result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                    result.put("data", obtainCloseData(roomNo));
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "closeRoomPush_DDZ");
                }
            }
        }
    }


    public void gameContinue(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, DdzConstant.DDZ_GAME_STATUS_SUMMARY, client) &&
                !CommonConstant.checkEvent(postData, DdzConstant.DDZ_GAME_STATUS_READY, client)) {
            return;
        }
        // 房间号
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        // 设置状态
        // 设置房间准备状态
        if (room.getRoomType() != CommonConstant.ROOM_TYPE_MATCH && room.getGameStatus() != DdzConstant.DDZ_GAME_STATUS_READY) {
            room.setGameStatus(DdzConstant.DDZ_GAME_STATUS_READY);
            for (String player : obtainAllPlayerAccount(roomNo)) {
                room.getUserPacketMap().get(player).setStatus(DdzConstant.DDZ_USER_STATUS_INIT);
            }
        }
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_MATCH && room.getGameCount() > room.getGameIndex()) {
            gameReady(client, postData);
        }
        // 比赛场获取分数
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_MATCH) {
            room.getPlayerMap().get(account).setScore(getUserScoreByAccount(room.getMatchNum(), account));
        }
        JSONObject result = new JSONObject();
        // 组织数据，通知玩家
        result.put("type", 1);
        result.put("data", obtainRoomData(roomNo, account));
        // 通知玩家
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush_DDZ");
    }


    public void reconnectGame(SocketIOClient client, Object data) {
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
            result.put("type", 0);
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush_DDZ");
            return;
        }
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 不在当前房间内
        if (Dto.stringIsNULL(account) || !room.getPlayerMap().containsKey(account) || room.getPlayerMap().get(account) == null) {
            result.put("type", 0);
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush_DDZ");
            return;
        }
        // 刷新uuid
        room.getPlayerMap().get(account).setUuid(client.getSessionId());
        // 刷新比赛场
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_MATCH) {
            matchEventDeal.changePlayerInfo(room.getMatchNum(), String.valueOf(client.getSessionId()),
                    null, account, 0, 0, room.getUserPacketMap().get(account).getMyPai().size(), 0);
        }
        // 组织数据，通知玩家
        result.put("type", 1);
        result.put("data", obtainRoomData(roomNo, account));
        // 通知玩家
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush_DDZ");
    }

    private void changeGameStatus(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONObject result = new JSONObject();
        result.put("gameStatus", room.getGameStatus());
        result.put("users", obtainAllPlayer(roomNo));
        if (room.getGameStatus() == DdzConstant.DDZ_GAME_STATUS_FINAL_SUMMARY) {
            result.put("endData", obtainFinalSummaryData(roomNo));
        }
        CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "changeGameStatusPush_DDZ");
    }

    private List<String> changeFormat(List<List<String>> list) {
        List<String> newList = new ArrayList<>();
        for (List<String> stringList : list) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < stringList.size(); i++) {
                sb.append(stringList.get(i));
                if (i != stringList.size() - 1) {
                    sb.append(",");
                }
            }
            newList.add(String.valueOf(sb));
        }
        return newList;
    }

    private void startGame(String roomNo) {
        String startTimesKey = "startTimes_ddz:" + roomNo;
        long startTimes = redisService.incr(startTimesKey, 1);
        if (startTimes > 1) {
            return;
        }
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);

        // 设置房间状态
        room.setGameStatus(DdzConstant.DDZ_GAME_STATUS_CHOICE_LANDLORD);
        // 初始化房间信息
        initRoom(roomNo);
        // 更新游戏局数
        roomBiz.increaseRoomIndexByRoomNo(room.getId());

        // 清空结算防重
        redisInfoService.insertSummary(roomNo, "_PDK");
        // 设置手牌
        List<List<String>> cardList = DdzCore.shuffleAndDeal();
        int cardIndex = 0;
        List<String> accountList = obtainAllPlayerAccount(roomNo);
        for (String account : accountList) {
            // 比赛场准备失败
            if (room.getRoomType() == CommonConstant.ROOM_TYPE_MATCH && room.getUserPacketMap().get(account).getStatus() != DdzConstant.DDZ_USER_STATUS_READY) {
                room.getUserPacketMap().get(account).setStatus(DdzConstant.DDZ_USER_STATUS_READY);
            }
            room.getUserPacketMap().get(account).setMyPai(cardList.get(cardIndex));
            cardIndex++;
        }
        if (room.getFee() > 0) {
            JSONArray array = new JSONArray();
            for (String account : obtainAllPlayerAccount(roomNo)) {
                // 更新实体类数据
                Playerinfo playerinfo = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account);
                RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setScore(Dto.sub(playerinfo.getScore(), room.getFee()));
                // 负数清零
                if (RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore() < 0) {
                    RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setScore(0);
                }
                array.add(playerinfo.getId());
            }
            // 抽水
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.PUMP, room.getJsonObject(array)));
        }
        // 设置地主牌
        room.setLandlordCard(cardList.get(cardIndex));
        // 随机产生叫地主玩家
        room.setFocusIndex(RandomUtils.nextInt(DdzConstant.DDZ_PLAYER_NUMBER));
        // 开始叫地主定时器
        beginRobTimer(roomNo, room.getFocusIndex(), DdzConstant.DDZ_BE_LANDLORD_TYPE_CALL);
        // 通知玩家
        for (String account : accountList) {
            JSONObject result = new JSONObject();
            result.put("room_no", roomNo);
            result.put("myPai", room.getUserPacketMap().get(account).getMyPai());
            result.put("number", room.getFocusIndex());
            result.put("multiple", room.getMultiple());
            result.put("gameIndex", room.getGameIndex());
            result.put("leftArray", getLeftArray(roomNo, account));
            result.put("users", obtainAllPlayer(roomNo));
            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(account).getUuid(), String.valueOf(result), "gameStartPush_DDZ");
        }
    }

    private void initRoom(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 清空地主信息
        room.setLandlordAccount(null);
        // 清空上一次出牌
        room.getLastCard().clear();
        // 清空地主牌
        room.getLandlordCard().clear();
        // 清空叫、抢地主记录
        room.getOperateRecord().clear();
        // 初始化倍数
        room.setMultiple(1);
        // 增加游戏局数
        if (room.getReShuffleTime() == 0) {
            room.addGameIndex();
            room.addGameNewIndex();
        }
        room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_INIT);
        // 初始化玩家
        for (String account : obtainAllPlayerAccount(roomNo)) {
            room.getUserPacketMap().get(account).setScore(0);
            room.getUserPacketMap().get(account).setDoubleTime(0);
        }
    }


    private void summary(String roomNo, String winAccount) {
        // 结算防重
        long summaryTimes = redisInfoService.summaryTimes(roomNo, "_PDK");
        if (summaryTimes > 1) {
            return;
        }
        // 重置开始游戏防重
        redisService.insertKey("startTimes_ddz:" + roomNo, "0", null);
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 农民输赢分数=倍数*底
        double farmerScore = Dto.mul(room.getMultiple(), room.getScore());
        if (winAccount.equals(room.getLandlordAccount())) {
            farmerScore = Dto.sub(0, farmerScore);
        }
        // 春天翻倍
        if (isSpring(roomNo) == CommonConstant.GLOBAL_YES) {
            farmerScore *= 2;
        }
        // 如果有托管包赔取出当前所有托管农民
        List<String> trusteeFarmer = new ArrayList<>();
        if (!Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("trustee_lose") &&
                room.getSetting().getInt("trustee_lose") == CommonConstant.GLOBAL_YES) {
            for (String account : obtainAllPlayerAccount(roomNo)) {
                if (!account.equals(room.getLandlordAccount()) && room.getUserPacketMap().get(account).getIsTrustee() == CommonConstant.GLOBAL_YES) {
                    trusteeFarmer.add(account);
                }
            }
        }
        // 各个玩家结算数据
        Map<String, Double> sumMap = new HashMap<>();
        for (String account : obtainAllPlayerAccount(roomNo)) {
            if (!account.equals(room.getLandlordAccount())) {
                double singleFarmScore = farmerScore;
                // 单个农民输赢等于农民基础底分 * 农民加倍倍数 * 地主加倍倍数
                if (room.getUserPacketMap().get(account).getDoubleTime() > 0 &&
                        room.getUserPacketMap().get(room.getLandlordAccount()).getDoubleTime() > 0) {
                    singleFarmScore = singleFarmScore * room.getUserPacketMap().get(account).getDoubleTime()
                            * room.getUserPacketMap().get(room.getLandlordAccount()).getDoubleTime();
                }
                sumMap.put(account, singleFarmScore);
                // 地主输赢为农民积分的相反数，进行累加
                if (!sumMap.containsKey(room.getLandlordAccount())) {
                    sumMap.put(room.getLandlordAccount(), -singleFarmScore);
                } else {
                    sumMap.put(room.getLandlordAccount(), Dto.sub(sumMap.get(room.getLandlordAccount()), singleFarmScore));
                }
            }
        }
        // 优先更改地主输赢积分，用于后续判断
        room.getUserPacketMap().get(room.getLandlordAccount()).setScore(sumMap.get(room.getLandlordAccount()));
        for (String account : sumMap.keySet()) {
            if (!account.equals(room.getLandlordAccount())) {
                if (trusteeFarmer.size() == 1) {

                    if (trusteeFarmer.contains(account)) {
                        if (sumMap.get(account) > 0) {
                            room.getUserPacketMap().get(account).setScore(0);
                        } else {
                            room.getUserPacketMap().get(account).setScore(-sumMap.get(room.getLandlordAccount()));
                        }
                    } else {
                        if (sumMap.get(account) > 0) {
                            room.getUserPacketMap().get(account).setScore(-sumMap.get(room.getLandlordAccount()));
                        } else {
                            room.getUserPacketMap().get(account).setScore(0);
                        }
                    }
                } else {
                    room.getUserPacketMap().get(account).setScore(sumMap.get(account));
                }
            }
            // 设置为玩家分数
            double oldScore = room.getPlayerMap().get(account).getScore();
            room.getPlayerMap().get(account).setScore(Dto.add(room.getUserPacketMap().get(account).getScore(), oldScore));
            // 重置托管状态
            room.getUserPacketMap().get(account).setIsTrustee(CommonConstant.GLOBAL_NO);
        }

        room.setGameStatus(DdzConstant.DDZ_GAME_STATUS_SUMMARY);
        // 重置重新发牌次数
        room.setReShuffleTime(0);
        // 房卡场
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_DK
                || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
            for (String account : obtainAllPlayerAccount(roomNo)) {
                // 游戏局数+1
                room.getPlayerMap().get(account).addPlayTimes();//添加游戏局数
                if (room.getUserPacketMap().get(account).getScore() > 0) {
                    // 胜利局数+1
                    room.getUserPacketMap().get(account).setWinNum(room.getUserPacketMap().get(account).getWinNum() + 1);
                }
                if (account.equals(room.getLandlordAccount())) {
                    // 地主局数+1
                    room.getUserPacketMap().get(account).setLandlordNum(room.getUserPacketMap().get(account).getLandlordNum() + 1);
                }
            }
            // 是否需要总结算
            room.setNeedFinalSummary(true);
            // 局数到了之后触发总结算
            if (room.getGameIndex() == room.getGameCount()) {
                if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
                    room.setOpen(false);
                }
                room.setGameStatus(DdzConstant.DDZ_GAME_STATUS_FINAL_SUMMARY);

            }
            // 扣房卡
            updateRoomCard(roomNo);
            // 存战绩
            saveGameLog(roomNo);
        } else if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
            // 更新玩家分数
            updateUserScore(roomNo);
            // 更新玩家连胜奖励
            updateUserStreakWinReward(roomNo, room);
        } else if (room.getRoomType() == CommonConstant.ROOM_TYPE_MATCH) {
            if (room.getGameIndex() == room.getGameCount()) {
                // 玩家游戏详情
                List<JSONObject> userDetails = new ArrayList<>();
                String firstAccount = getFirstAccountInRoom(roomNo);
                for (String player : obtainAllPlayerAccount(roomNo)) {
                    JSONObject userDetail = new JSONObject();
                    userDetail.put("account", player);
                    userDetail.put("score", room.getUserPacketMap().get(player).getScore());
                    userDetail.put("round", 0);
                    userDetail.put("card", room.getUserPacketMap().get(player).getMyPai().size());
                    if (!room.getRobotList().contains(player)) {
                        userDetail.put("round", 1);
                    }
                    userDetail.put("win", player.equals(firstAccount) ? 10 : 1);
                    userDetails.add(userDetail);
                }
                matchEventDeal.userFinish(room.getMatchNum(), userDetails);
            }
        }
        // 更新成就信息
        updateUserAchievement(roomNo);
    }


    private void updateUserStreakWinReward(String roomNo, DdzGameRoom room) {
        if (!Dto.isObjNull(room.getWinStreakObj())) {
            JSONObject winStreakObj = room.getWinStreakObj();
            // 次数
            int time = winStreakObj.getInt("time");
            // 是否需要连胜
            int mustWin = winStreakObj.getInt("mustWin");
            // 设置连胜次数
            for (String player : obtainAllPlayerAccount(roomNo)) {
                // 达到条件之后不再更新次数
                if (room.getUserPacketMap().get(player).getWinStreakTime() == time) {
                    continue;
                }
                // 胜利局数+1 如果需要连胜失败了直接算0
                if (room.getUserPacketMap().get(player).getScore() > 0) {
                    room.getUserPacketMap().get(player).setWinStreakTime(room.getUserPacketMap().get(player).getWinStreakTime() + 1);
                } else if (mustWin == CommonConstant.GLOBAL_YES) {
                    room.getUserPacketMap().get(player).setWinStreakTime(0);
                }
            }
        }
    }

    private void updateUserAchievement(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room.getSetting().containsKey("isAchievement") && room.getSetting().getInt("isAchievement") == CommonConstant.GLOBAL_YES) {
            for (String account : obtainAllPlayerAccount(roomNo)) {
                if (room.getUserPacketMap().get(account).getScore() > 0) {
                    JSONObject levelUp = achievementBiz.addOrUpdateUserAchievement(account, CommonConstant.GAME_ID_DDZ, 1);
                    if (!Dto.isObjNull(levelUp)) {
                        int reward = levelUp.getInt("reward");
                        if (reward > 0) {
                            JSONObject result = new JSONObject();
                            result.put("type", CommonConstant.SHOW_MSG_TYPE_SMALL);
                            String msg = "已晋升为" + levelUp.getString("achievement_name");
                            result.put(CommonConstant.RESULT_KEY_MSG, msg);
                            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(account).getUuid(), String.valueOf(result), "tipMsgPush");
                        }
                    }
                }
            }
        }
    }


    private void updateUserInfo(DdzGameRoom room, String account, double reward, String updateType) {
        JSONArray array = new JSONArray();
        JSONObject obj = getScoreChangeObj(room, account, reward);
        array.add(obj);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("array", array);
        jsonObject.put("updateType", updateType);
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_SCORE, jsonObject));
    }


    public void updateRoomCard(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        int roomCardCount = 0;
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
            for (String account : obtainAllPlayerAccount(roomNo)) {
                if (room.getUserPacketMap().get(account).getStatus() > DdzConstant.DDZ_USER_STATUS_INIT) {
                    // 房主支付
                    if (CommonConstant.ROOM_PAY_TYPE_FANGZHU.equals(room.getPayType())) {
                        if (account.equals(room.getOwner()) && room.getPlayerMap().get(account).getPlayTimes() == 1) {
                            // 参与第一局需要扣房卡
                            roomCardCount = room.getPlayerCount() * room.getSinglePayNum();
                            array.add(room.getPlayerMap().get(room.getOwner()).getId());
                        }
                    }
                    // 房费AA
                    if (CommonConstant.ROOM_PAY_TYPE_AA.equals(room.getPayType())) {
                        // 参与第一局需要扣房卡
                        if (room.getPlayerMap().get(account).getPlayTimes() == 1) {
                            array.add(room.getPlayerMap().get(account).getId());
                            roomCardCount = room.getSinglePayNum();
                        }
                    }

                }
            }
        } else if (room.getRoomType() == CommonConstant.ROOM_TYPE_DK && room.getGameIndex() == 1) {
            JSONObject userInfo = userBiz.getUserByAccount(room.getOwner());
            if (!Dto.isObjNull(userInfo)) {
                roomCardCount = room.getPlayerCount() * room.getSinglePayNum();
                array.add(userInfo.getLong("id"));
            }
        }
        if (array.size() > 0) {
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.PUMP, room.getRoomCardChangeObject(array, roomCardCount)));
        }
    }

    public void updateUserScore(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB || room.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
            JSONArray array = new JSONArray();
            // 存放游戏记录
            for (String uuid : obtainAllPlayerAccount(roomNo)) {
                // 有参与的玩家
                if (room.getUserPacketMap().get(uuid).getStatus() > DdzConstant.DDZ_USER_STATUS_INIT) {
                    // 元宝输赢情况
                    JSONObject obj = getScoreChangeObj(room, uuid, room.getUserPacketMap().get(uuid).getScore());
                    array.add(obj);
                }
            }
            // 更新玩家分数
            if (array.size() > 0) {
                producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_SCORE, room.getPumpObject(array)));
            }
        }
    }

    private JSONObject getScoreChangeObj(DdzGameRoom room, String uuid, double sum) {
        JSONObject obj = new JSONObject();
        obj.put("total", room.getPlayerMap().get(uuid).getScore());
        obj.put("fen", sum);
        obj.put("id", room.getPlayerMap().get(uuid).getId());
        return obj;
    }

    public void saveGameLog(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (null == room
                || CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE == room.getIsClose()
                && DdzConstant.DDZ_GAME_STATUS_SUMMARY != room.getGameStatus() && DdzConstant.DDZ_GAME_STATUS_FINAL_SUMMARY != room.getGameStatus()
        ) return;

        JSONArray gameLogResults = new JSONArray();
        JSONArray gameResult = new JSONArray();
        JSONArray array = new JSONArray();
        // 存放游戏记录
        for (String account : obtainAllPlayerAccount(roomNo)) {
            // 有参与的玩家
            if (room.getUserPacketMap().get(account).getStatus() > DdzConstant.DDZ_USER_STATUS_INIT) {
                JSONObject obj = new JSONObject();
                obj.put("id", room.getPlayerMap().get(account).getId());
                obj.put("total", room.getPlayerMap().get(account).getScore());
                obj.put("fen", room.getUserPacketMap().get(account).getScore());
                array.add(obj);
                // 战绩记录
                JSONObject gameLogResult = new JSONObject();
                gameLogResult.put("account", account);
                gameLogResult.put("name", room.getPlayerMap().get(account).getName());
                gameLogResult.put("headimg", room.getPlayerMap().get(account).getHeadimg());
                gameLogResult.put("score", room.getUserPacketMap().get(account).getScore());
                gameLogResult.put("totalScore", room.getPlayerMap().get(account).getScore());
                gameLogResult.put("win", CommonConstant.GLOBAL_YES);
                if (room.getUserPacketMap().get(account).getScore() < 0) {
                    gameLogResult.put("win", CommonConstant.GLOBAL_NO);
                }
                gameLogResults.add(gameLogResult);
                // 用户战绩
                JSONObject userResult = new JSONObject();
                userResult.put("account", account);
                userResult.put("isWinner", CommonConstant.GLOBAL_NO);
                if (room.getUserPacketMap().get(account).getScore() > 0) {
                    userResult.put("isWinner", CommonConstant.GLOBAL_YES);
                }
                userResult.put("score", room.getUserPacketMap().get(account).getScore());
                userResult.put("totalScore", RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore());
                userResult.put("player", room.getPlayerMap().get(account).getName());
                gameResult.add(userResult);
            }
        }

        // 战绩信息
        JSONObject gameLogObj = room.obtainGameLog(String.valueOf(gameLogResults), String.valueOf(room.getOperateRecord()));
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_GAME_LOG, gameLogObj));
        JSONObject o = new JSONObject();
        JSONArray userGameLogs = room.obtainUserGameLog(gameLogObj.getLong("id"), array);
        o.element("array", userGameLogs);
        o.element("object", new JSONObject()
                .element("gamelog_id", gameLogObj.getLong("id"))
                .element("room_type", room.getRoomType())
                .element("room_id", room.getId())
                .element("room_no", room.getRoomNo())
                .element("game_index", room.getGameNewIndex())
                .element("result", gameResult.toString())
        );
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_USER_GAME_LOG, o));
    }

    private boolean determineLandlord(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        String landlordAccount = null;
        // 获取最后一个选是的玩家
        for (int i = room.getOperateRecord().size() - 1; i >= 0; i--) {
            if (room.getOperateRecord().get(i).containsKey("isChoice") && room.getOperateRecord().get(i).getInt("isChoice") == CommonConstant.GLOBAL_YES) {
                landlordAccount = room.getOperateRecord().get(i).getString("account");
                break;
            }
        }
        // 无人叫地主选默认
        if (!Dto.stringIsNULL(landlordAccount)) {
            room.setLandlordAccount(landlordAccount);
        } else {
            int maxShuffleTime = !Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("max_shuffle_time")
                    ? room.getSetting().getInt("max_shuffle_time") : DdzConstant.DDZ_MAX_RE_SHUFFLE_TIME;
            // 重新开局
            if (room.getSetting().containsKey("re_shuffle") && room.getSetting().getInt("re_shuffle") == CommonConstant.GLOBAL_YES &&
                    room.getReShuffleTime() < maxShuffleTime) {
                room.setReShuffleTime(room.getReShuffleTime() + 1);
                room.setFocusIndex(CommonConstant.NO_BANKER_INDEX);
                return false;
            }
            room.setLandlordAccount(room.getOperateRecord().get(0).getString("account"));
        }
        // 设置上个出牌玩家
        room.setLastOperateAccount(room.getLandlordAccount());
        // 设置焦点
        room.setFocusIndex(room.getPlayerMap().get(room.getLandlordAccount()).getMyIndex());
        // 添加地主牌
        List<String> landlordPai = room.getUserPacketMap().get(room.getLandlordAccount()).getMyPai();
        landlordPai.addAll(room.getLandlordCard());
        // 排序
        DdzCore.sortCard(landlordPai);
        room.getUserPacketMap().get(room.getLandlordAccount()).setMyPai(landlordPai);

        if (!Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("is_double")) {
            room.setGameStatus(DdzConstant.DDZ_GAME_STATUS_DOUBLE);
        } else {
            room.setGameStatus(DdzConstant.DDZ_GAME_STATUS_GAME_IN);
        }
        return true;
    }

    private void beginEventTimer(final String roomNo, final String nextPlayerAccount) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        final int timeLeft;
        if (room.getSetting().containsKey("eventTime")) {
            timeLeft = room.getSetting().getInt("eventTime");
        } else {
            timeLeft = 20;
        }
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                gameTimerDdz.gameEventOverTime(roomNo, nextPlayerAccount, timeLeft);
            }
        });
        // 机器人出牌
        if (room.isRobot() && room.getRobotList().contains(nextPlayerAccount)) {
            int delayTime = RandomUtils.nextInt(3) + 2;
            AppBeanUtil.getBean(RobotEventDeal.class).changeRobotActionDetail(nextPlayerAccount, DdzConstant.DDZ_GAME_EVENT_GAME_IN, delayTime);
        }
    }

    private void beginDoubleTimer(final String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        final int timeLeft;
        if (room.getSetting().containsKey("doubleTime")) {
            timeLeft = room.getSetting().getInt("doubleTime");
        } else {
            timeLeft = 10;
        }
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                gameTimerDdz.doubleOverTime(roomNo, timeLeft);
            }
        });
        // 机器人出牌
        if (room.isRobot()) {
            for (String account : room.getRobotList()) {
                int delayTime = RandomUtils.nextInt(3) + 2;
                AppBeanUtil.getBean(RobotEventDeal.class).changeRobotActionDetail(account, DdzConstant.DDZ_GAME_EVENT_GAME_DOUBLE, delayTime);
            }
        }
    }

    private void beginRobTimer(final String roomNo, final int focus, final int type) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        final int timeLeft;
        if (room.getSetting().containsKey("robTime")) {
            timeLeft = room.getSetting().getInt("robTime");
        } else {
            timeLeft = 15;
        }
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                gameTimerDdz.gameRobOverTime(roomNo, focus, type, timeLeft);
            }
        });
        // 机器人出牌
        if (room.isRobot()) {
            for (String robotAccount : obtainAllPlayerAccount(roomNo)) {
                if (room.getPlayerMap().get(robotAccount).getMyIndex() == focus && room.getRobotList().contains(robotAccount)) {
                    int delayTime = RandomUtils.nextInt(3) + 2;
                    if (type == DdzConstant.DDZ_BE_LANDLORD_TYPE_CALL) {
                        AppBeanUtil.getBean(RobotEventDeal.class).changeRobotActionDetail(robotAccount, DdzConstant.DDZ_GAME_EVENT_ROBOT_CALL, delayTime);
                    } else if (type == DdzConstant.DDZ_BE_LANDLORD_TYPE_ROB) {
                        AppBeanUtil.getBean(RobotEventDeal.class).changeRobotActionDetail(robotAccount, DdzConstant.DDZ_GAME_EVENT_ROBOT_ROB, delayTime);
                    }
                    break;
                }
            }
        }
    }

    private boolean isAllReady(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        for (String account : obtainAllPlayerAccount(roomNo)) {
            if (room.getUserPacketMap().get(account).getStatus() != DdzConstant.DDZ_USER_STATUS_READY) {
                return false;
            }
        }
        return true;
    }


    private boolean isAllChoice(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        List<JSONObject> operateRecord = room.getOperateRecord();
        // 3条操作记录
        if (operateRecord.size() == DdzConstant.DDZ_PLAYER_NUMBER) {
            int choiceNum = 0;
            for (JSONObject record : operateRecord) {
                if (record.getInt("isChoice") == CommonConstant.GLOBAL_YES) {
                    choiceNum++;
                }
            }
            // 只有一个人叫或没人叫地主已经完成
            if (choiceNum <= 1) {
                return true;
            }
        }
        // 4条操作记录
        if (operateRecord.size() == DdzConstant.DDZ_PLAYER_NUMBER + 1) {
            return true;
        }
        return false;
    }

    private boolean isAllAgreeClose(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        for (String account : obtainAllPlayerAccount(roomNo)) {
            if (room.getPlayerMap().get(account).getIsCloseRoom() != CommonConstant.CLOSE_ROOM_AGREE) {
                return false;
            }
        }
        return true;
    }


    private int isSpring(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        List<JSONObject> operateRecord = room.getOperateRecord();
        // 地主出牌次数
        int landlordCardCount = 0;
        // 农民出牌次数
        int farmerCardCount = 0;
        for (JSONObject object : operateRecord) {
            if (object.containsKey("cardList") && object.getJSONArray("cardList").size() > 0) {
                if (object.getString("account").equals(room.getLandlordAccount())) {
                    landlordCardCount++;
                } else {
                    farmerCardCount++;
                }
            }
        }
        if (landlordCardCount == 1 || farmerCardCount == 0) {
            return CommonConstant.GLOBAL_YES;
        }
        return CommonConstant.GLOBAL_NO;
    }


    private JSONArray obtainFinalSummaryData(String roomNo) {
        JSONArray array = new JSONArray();
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        for (String account : obtainAllPlayerAccount(roomNo)) {
            JSONObject obj = new JSONObject();
            obj.put("account", account);
            obj.put("name", room.getPlayerMap().get(account).getName());
            obj.put("score", room.getPlayerMap().get(account).getScore());
            obj.put("callNum", room.getUserPacketMap().get(account).getCallNum());
            obj.put("winNum", room.getUserPacketMap().get(account).getWinNum());
            obj.put("landlordNum", room.getUserPacketMap().get(account).getLandlordNum());
            obj.put("headimg", room.getPlayerMap().get(account).getRealHeadimg());
            array.add(obj);
        }
        return array;
    }


    private JSONObject obtainSummaryData(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONObject obj = new JSONObject();
        obj.put("isSpring", isSpring(roomNo));
        obj.put("landlordWin", CommonConstant.GLOBAL_NO);
        if (!Dto.stringIsNULL(room.getWinner()) && room.getWinner().equals(room.getLandlordCard())) {
            obj.put("landlordWin", CommonConstant.GLOBAL_YES);
        }
        JSONObject array = new JSONObject();
        for (String account : obtainAllPlayerAccount(roomNo)) {
            JSONObject object = new JSONObject();
            object.put("score", room.getUserPacketMap().get(account).getScore());
            object.put("name", room.getPlayerMap().get(account).getName());
            object.put("scoreLeft", room.getPlayerMap().get(account).getScore());
            object.put("isWin", CommonConstant.GLOBAL_NO);
            if (room.getUserPacketMap().get(account).getScore() > 0) {
                object.put("isWin", CommonConstant.GLOBAL_YES);
            }
            object.put("isLandlord", CommonConstant.GLOBAL_NO);
            if (account.equals(room.getLandlordAccount())) {
                object.put("isLandlord", CommonConstant.GLOBAL_YES);
            }
            object.put("myPai", room.getUserPacketMap().get(account).getMyPai());
            object.put("isDouble", room.getUserPacketMap().get(account).getDoubleTime());
            array.put(room.getPlayerMap().get(account).getMyIndex(), object);
        }
        obj.put("array", array);
        return obj;
    }


    private JSONObject obtainRoomData(String roomNo, String account) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONObject obj = new JSONObject();
        obj.put("playerCount", room.getPlayerCount());
        obj.put("room_no", roomNo);
        obj.put("gameIndex", room.getGameIndex());
        obj.put("gameCount", room.getGameCount());
        obj.put("gameStatus", room.getGameStatus());
        obj.put("note", obtainReconnectNote(roomNo));
        obj.put("roomType", room.getRoomType());
        obj.put("myIndex", room.getPlayerMap().get(account).getMyIndex());
        obj.put("landlord", obtainLandlordIndex(roomNo));
        obj.put("time", room.getTimeLeft());
        obj.put("users", obtainAllPlayer(roomNo));
        obj.put("cardList", obtainLandlordCard(roomNo));
        obj.put("multiple", room.getMultiple());
        obj.put("myPai", room.getUserPacketMap().get(account).getMyPai());
        obj.put("focus", room.getFocusIndex());
        obj.put("map", obtainOperateRecord(roomNo));
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
            obj.put("clubCode", room.getClubCode());
        }
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_MATCH) {
            obj.put("myRank", room.getPlayerMap().get(account).getMyRank());
            obj.put("totalPlayer", room.getTotalNum());
            // 比赛场等待晋级恢复 20180919 wqm
            if (room.getGameStatus() == DdzConstant.DDZ_GAME_STATUS_SUMMARY) {
                Object object = redisService.queryValueByKey("match_info_" + room.getMatchNum());
                if (object != null) {
                    JSONObject matchInfo = JSONObject.fromObject(object);
                    obj.put("rankArray", matchInfo.getJSONArray("promotion"));
                    obj.put("isPromotion", CommonConstant.GLOBAL_NO);
                    obj.put("rankIndex", matchInfo.getInt("cur_round"));
                    obj.put("gameStatus", DdzConstant.DDZ_GAME_STATUS_WAIT_PROMOTION);
                }
            }
            // ------end------
        }
        if (room.getGameStatus() == DdzConstant.DDZ_GAME_STATUS_SUMMARY) {
            obj.put("summaryData", obtainSummaryData(roomNo));
        }
        // 是否总结算
        obj.put("isEnd", CommonConstant.GLOBAL_NO);
        if (room.getGameStatus() == DdzConstant.DDZ_GAME_STATUS_FINAL_SUMMARY) {
            obj.put("isEnd", CommonConstant.GLOBAL_NO);
            obj.put("endData", obtainFinalSummaryData(roomNo));
        }
        // 是否解散房间
        obj.put("isClose", CommonConstant.GLOBAL_NO);
        if (room.getJieSanTime() > 0) {
            obj.put("isClose", CommonConstant.GLOBAL_YES);
            obj.put("closeData", obtainCloseData(roomNo));
        }
        JSONArray timeArray = new JSONArray();
        if (room.getSetting().containsKey("readyTime")) {
            timeArray.add(room.getSetting().getInt("readyTime"));
        } else {
            timeArray.add(15);
        }
        if (room.getSetting().containsKey("robTime")) {
            timeArray.add(room.getSetting().getInt("robTime"));
            timeArray.add(room.getSetting().getInt("robTime"));
        } else {
            timeArray.add(15);
            timeArray.add(15);
        }
        if (room.getSetting().containsKey("eventTime")) {
            timeArray.add(room.getSetting().getInt("eventTime"));
        } else {
            timeArray.add(20);
        }
        if (room.getSetting().containsKey("doubleTime")) {
            timeArray.add(room.getSetting().getInt("doubleTime"));
        } else {
            timeArray.add(10);
        }
        obj.put("timeArray", timeArray);
        obj.put("leftArray", getLeftArray(roomNo, account));
        obj.put("isDraw", CommonConstant.GLOBAL_NO);
        if (!Dto.isObjNull(room.getWinStreakObj())) {
            int left = room.getWinStreakObj().getInt("time") - room.getUserPacketMap().get(account).getWinStreakTime();
            if (left <= 0) {
                obj.put("drawInfo", "可抽奖");
                if (room.getGameStatus() == DdzConstant.DDZ_GAME_STATUS_SUMMARY) {
                    obj.put("isDraw", CommonConstant.GLOBAL_YES);
                }
            } else {
                obj.put("drawInfo", "还剩" + left + "局");
            }
        }
        // 当前加倍卡数量
        obj.put("doubleCardNum", getUserDoubleCardNum(roomNo, account));
        // 需要消耗的加倍卡数量
        obj.put("costNum", getCurDoubleCardCost(roomNo, account));
        obj.put("doubleTip", getDoubleTip(roomNo));
        return obj;
    }

    private JSONObject obtainOperateRecord(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONObject obj = new JSONObject();
        for (String account : obtainAllPlayerAccount(roomNo)) {
            obj.put(room.getPlayerMap().get(account).getMyIndex(), obtainLastOperateRecordByAccount(roomNo, account));
        }
        return obj;
    }


    private JSONObject obtainLastOperateRecordByAccount(String roomNo, String account) {
        JSONObject obj = new JSONObject();
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        for (int i = room.getOperateRecord().size() - 1; i >= 0; i--) {
            if (account.equals(room.getOperateRecord().get(i).getString("account"))) {
                return room.getOperateRecord().get(i);
            }
        }
        obj.put("lastNote", 0);
        return obj;
    }

    private JSONArray obtainAllPlayer(String roomNo) {
        JSONArray array = new JSONArray();
        for (String account : obtainAllPlayerAccount(roomNo)) {
            JSONObject playerInfo = obtainPlayerInfo(roomNo, account);
            if (!Dto.isObjNull(playerInfo)) {
                array.add(playerInfo);
            }
        }
        return array;
    }

    private JSONObject obtainPlayerInfo(String roomNo, String account) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        Playerinfo player = room.getPlayerMap().get(account);
        JSONObject obj = new JSONObject();
        if (player != null) {
            UserPacketDdz up = room.getUserPacketMap().get(account);
            obj.put("account", player.getAccount());
            obj.put("name", player.getName());
            obj.put("headimg", player.getRealHeadimg());
            obj.put("sex", player.getSex());
            obj.put("ip", player.getIp());
            obj.put("vip", player.getVip());
            if (room.isRobot() && room.getRobotList().contains(account) && Dto.stringIsNULL(player.getLocation())) {
                int jwIndex = RandomUtils.nextInt(CommonConstant.jwdList.size());
                if (jwIndex > CommonConstant.jwdList.size() - 1) {
                    player.setLocation(CommonConstant.jwdList.get(0));
                } else {
                    player.setLocation(CommonConstant.jwdList.get(jwIndex));
                }
            }
            obj.put("location", player.getLocation());
            obj.put("area", player.getArea());
            obj.put("score", player.getScore());
            obj.put("index", player.getMyIndex());
            obj.put("userOnlineStatus", player.getStatus());
            obj.put("ghName", player.getGhName());
            obj.put("introduction", player.getSignature());
            obj.put("userStatus", up.getStatus());
            obj.put("cardNum", up.getMyPai().size());
            obj.put("trusteeStatus", up.getIsTrustee());
            obj.put("isDouble", up.getDoubleTime());
        }
        return obj;
    }

    private List<String> obtainLandlordCard(String roomNo) {
        List<String> landlordCard = new ArrayList<>();
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room.getGameStatus() == DdzConstant.DDZ_GAME_STATUS_GAME_IN || room.getGameStatus() == DdzConstant.DDZ_GAME_STATUS_DOUBLE) {
            return room.getLandlordCard();
        }
        return landlordCard;
    }

    private int obtainLandlordIndex(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room.getGameStatus() == DdzConstant.DDZ_GAME_STATUS_GAME_IN || room.getGameStatus() == DdzConstant.DDZ_GAME_STATUS_DOUBLE) {
            if (!Dto.stringIsNULL(room.getLandlordAccount()) && room.getPlayerMap().containsKey(room.getLandlordAccount()) &&
                    room.getPlayerMap().get(room.getLandlordAccount()) != null) {
                return room.getPlayerMap().get(room.getLandlordAccount()).getMyIndex();
            }
        }
        return CommonConstant.NO_BANKER_INDEX;
    }

    private int obtainReconnectNote(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        switch (room.getGameStatus()) {
            case DdzConstant.DDZ_GAME_STATUS_INIT:
                return DdzConstant.DDZ_RECONNECT_NODE_READY;
            case DdzConstant.DDZ_GAME_STATUS_READY:
                return DdzConstant.DDZ_RECONNECT_NODE_READY;
            case DdzConstant.DDZ_GAME_STATUS_CHOICE_LANDLORD:
                // 有人叫地主之后返回抢地主
                for (JSONObject obj : room.getOperateRecord()) {
                    if (obj.getInt("isChoice") == CommonConstant.GLOBAL_YES) {
                        return DdzConstant.DDZ_RECONNECT_NODE_ROB;
                    }
                }
                return DdzConstant.DDZ_RECONNECT_NODE_CALL;
            case DdzConstant.DDZ_GAME_STATUS_GAME_IN:
                return DdzConstant.DDZ_RECONNECT_NODE_IN;
            case DdzConstant.DDZ_GAME_STATUS_SUMMARY:
                return DdzConstant.DDZ_RECONNECT_NODE_SUMMARY;
            case DdzConstant.DDZ_GAME_STATUS_FINAL_SUMMARY:
                return DdzConstant.DDZ_RECONNECT_NODE_FINAL_SUMMARY;
            case DdzConstant.DDZ_GAME_STATUS_DOUBLE:
                return DdzConstant.DDZ_RECONNECT_DOUBLE;
            default:
                return DdzConstant.DDZ_RECONNECT_NODE_READY;
        }
    }

    private List<String> obtainAllPlayerAccount(String roomNo) {
        List<String> accountList = new ArrayList<>();
        if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
            DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
            for (String account : room.getUserPacketMap().keySet()) {
                if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null &&
                        room.getPlayerMap().containsKey(account) && room.getPlayerMap().get(account) != null) {
                    accountList.add(account);
                }
            }
        }
        return accountList;
    }

    private int obtainNextPlayerIndex(String roomNo, String account) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        int playerIndex = room.getPlayerMap().get(account).getMyIndex();
        playerIndex++;
        return playerIndex % DdzConstant.DDZ_PLAYER_NUMBER;
    }

    private String obtainNextPlayerAccount(String roomNo, String account) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        int playerIndex = room.getPlayerMap().get(account).getMyIndex();
        playerIndex++;
        for (String next : obtainAllPlayerAccount(roomNo)) {
            if (room.getPlayerMap().get(next).getMyIndex() == playerIndex % DdzConstant.DDZ_PLAYER_NUMBER) {
                return next;
            }
        }
        return null;
    }

    private int obtainNextDoLandlordIndex(String roomNo, String account) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        int playerIndex = room.getPlayerMap().get(account).getMyIndex();
        playerIndex++;
        int nextIndex = playerIndex % DdzConstant.DDZ_PLAYER_NUMBER;
        if (!isAllChoice(roomNo)) {
            // 多人抢地主才需要判断
            for (String player : obtainAllPlayerAccount(roomNo)) {
                // 获取下家账号
                if (room.getPlayerMap().get(player).getMyIndex() == nextIndex) {
                    // 最后一条操作记录
                    JSONObject lastRecord = obtainLastOperateRecordByAccount(roomNo, player);
                    // 下家不抢返回下下家
                    if (lastRecord.containsKey("account") && player.equals(lastRecord.getString("account")) &&
                            lastRecord.containsKey("isChoice") && lastRecord.getInt("isChoice") == CommonConstant.GLOBAL_NO) {
                        return obtainNextDoLandlordIndex(roomNo, player);
                    }
                }
            }
        }
        return nextIndex;
    }

    public JSONArray obtainCloseData(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray closeData = new JSONArray();
        for (String account : obtainAllPlayerAccount(roomNo)) {
            JSONObject obj = new JSONObject();
            obj.put("jiesanTime", room.getJieSanTime());
            obj.put("index", room.getPlayerMap().get(account).getMyIndex());
            obj.put("name", room.getPlayerMap().get(account).getName());
            obj.put("result", room.getPlayerMap().get(account).getIsCloseRoom());
            closeData.add(obj);
        }
        return closeData;
    }

    private String obtainPaiTypeName(int paiType) {
        switch (paiType) {
            case DdzConstant.DDZ_CARD_TYPE_SINGLE:
                return "c1";
            case DdzConstant.DDZ_CARD_TYPE_PAIRS:
                return "c2";
            case DdzConstant.DDZ_CARD_TYPE_THREE:
                return "c3";
            case DdzConstant.DDZ_CARD_TYPE_THREE_WITH_SINGLE:
                return "c31";
            case DdzConstant.DDZ_CARD_TYPE_THREE_WITH_PARIS:
                return "c32";
            case DdzConstant.DDZ_CARD_TYPE_BOMB:
                return "c4";
            case DdzConstant.DDZ_CARD_TYPE_BOMB_WITH_SINGLE:
                return "c41";
            case DdzConstant.DDZ_CARD_TYPE_BOMB_WITH_PARIS:
                return "c42";
            case DdzConstant.DDZ_CARD_TYPE_STRAIGHT:
                return "c123";
            case DdzConstant.DDZ_CARD_TYPE_DOUBLE_STRAIGHT:
                return "c1122";
            case DdzConstant.DDZ_CARD_TYPE_PLANE:
                return "c111222";
            case DdzConstant.DDZ_CARD_TYPE_PLANE_WITH_SINGLE:
                return "c11122234";
            case DdzConstant.DDZ_CARD_TYPE_PLANE_WITH_DOUBLE:
                return "c1112223344";
            default:
                return "c0";
        }
    }


    private int getUserScoreByAccount(String matchNum, String account) {
        Object robotInfo = redisService.hget("robot_info_" + matchNum, account);
        if (!Dto.isNull(robotInfo)) {
            JSONObject robot = JSONObject.fromObject(robotInfo);
            if (robot.containsKey("score")) {
                return robot.getInt("score");
            }
        }
        Object plyerInfo = redisService.hget("player_info_" + matchNum, account);
        if (!Dto.isNull(plyerInfo)) {
            JSONObject player = JSONObject.fromObject(plyerInfo);
            if (player.containsKey("score")) {
                return player.getInt("score");
            }
        }
        return 0;
    }

    private List<Integer> getLeftArray(String roomNo, String account) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (Dto.stringIsNULL(room.getUserPacketMap().get(account).getJpqEndTime())) {
            JSONObject userProps = propsBiz.getUserPropsByType(account, CommonConstant.PROPS_TYPE_JPQ);
            // 没有记录设置当前时间之前的时间
            if (Dto.isObjNull(userProps)) {
                room.getUserPacketMap().get(account).setJpqEndTime(TimeUtil.addYearBaseOnNowTime(TimeUtil.getNowDate(), -1));
            } else {
                room.getUserPacketMap().get(account).setJpqEndTime(userProps.getString("end_time"));
            }
        }
        List<Integer> leftArray = new ArrayList<>();
        // 结束时间必须在当前时间之后
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB && room.getSetting().containsKey("props_jpq")) {
            if (TimeUtil.isLatter(room.getUserPacketMap().get(account).getJpqEndTime(), TimeUtil.getNowDate())) {
                // 初始化
                for (int i = 0; i < 15; i++) {
                    leftArray.add(0);
                }

                // 取所有手牌
                List<String> allCard = new ArrayList<>();
                for (String player : obtainAllPlayerAccount(roomNo)) {
                    if (!player.equals(account)) {
                        allCard.addAll(room.getUserPacketMap().get(player).getMyPai());
                    }
                }
                if (room.getGameStatus() == DdzConstant.DDZ_GAME_STATUS_CHOICE_LANDLORD) {
                    allCard.addAll(room.getLandlordCard());
                }
                // 取剩余手牌数
                for (String card : allCard) {
                    int cardValue = DdzCore.obtainCardValue(card);
                    int cardCount = leftArray.get(cardValue - 3) + 1;
                    leftArray.set(cardValue - 3, cardCount);
                }
            }
        }
        return leftArray;
    }

    private String getFirstAccountInRoom(String roomNo) {
        try {
            if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
                // 房间实体对象
                final DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
                // 当前房间内的所有的所有玩家
                List<String> allPlayer = obtainAllPlayerAccount(roomNo);
                if (allPlayer.size() > 0) {
                    // 排序
                    Collections.sort(allPlayer, new Comparator<String>() {
                        @Override
                        public int compare(String o1, String o2) {
                            double score1 = room.getPlayerMap().get(o1).getScore();
                            double score2 = room.getPlayerMap().get(o2).getScore();
                            if (score1 == score2) {
                                int size1 = room.getUserPacketMap().get(o1).getMyPai().size();
                                int size2 = room.getUserPacketMap().get(o2).getMyPai().size();
                                return size2 - size1;
                            }
                            return (int) (score2 - score1);
                        }
                    });
                    // 取第一个玩家
                    return allPlayer.get(0);
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        return null;
    }


    private int getUserDoubleCardNum(String roomNo, String account) {
        if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
            DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                // 未从数据库查询过
                if (room.getUserPacketMap().get(account).getDoubleCardNum() == -1) {
                    JSONObject userProps = propsBiz.getUserPropsByType(account, CommonConstant.PROPS_TYPE_DOUBLE_CARD);
                    // 赋值
                    if (!Dto.isObjNull(userProps)) {
                        room.getUserPacketMap().get(account).setDoubleCardNum(userProps.getInt("props_count"));
                    }
                }
                // 负数清0
                if (room.getUserPacketMap().get(account).getDoubleCardNum() < 0) {
                    room.getUserPacketMap().get(account).setDoubleCardNum(0);
                }
                return room.getUserPacketMap().get(account).getDoubleCardNum();
            }
        }
        return 0;
    }


    private int getCurDoubleCardCost(String roomNo, String account) {
        if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
            DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
            // 比赛场判断免费加倍次数及付费加倍次数 -1表示当前无法加倍
            if (room.getRoomType() == CommonConstant.ROOM_TYPE_MATCH && !Dto.stringIsNULL(room.getMatchNum())) {
                String key = "match_info_" + room.getMatchNum();
                try {
                    Object object = redisService.queryValueByKey(key);
                    if (object != null) {
                        // 场次信息
                        JSONObject matchInfo = JSONObject.fromObject(object);
                        // 免费次数
                        int freeTime = matchInfo.getInt("free_double_time");
                        // 付费次数
                        int payTime = matchInfo.getInt("pay_double_time");
                        // 玩家加倍信息
                        Object o = redisService.hget("double_info_" + room.getMatchNum(), account);
                        // 未加倍或加倍次数小于免费次数不需要消耗加倍卡
                        if (o == null || Integer.parseInt(String.valueOf(o)) < freeTime) {
                            return 0;
                        }
                        // 加倍次数小于免费次数+付费次数消耗1张加倍卡
                        if (Integer.parseInt(String.valueOf(o)) < freeTime + payTime) {
                            return 1;
                        }
                    }
                    return -1;
                } catch (Exception e) {
                    logger.error("请启动REmote DIctionary Server");
                    return -1;
                }
            }
        }
        return 1;
    }

    private String getDoubleTip(String roomNo) {
        if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
            DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
            // 比赛场判断免费加倍次数及付费加倍次数 -1表示当前无法加倍
            if (room.getRoomType() == CommonConstant.ROOM_TYPE_MATCH && !Dto.stringIsNULL(room.getMatchNum())) {
                String key = "match_info_" + room.getMatchNum();
                try {
                    Object object = redisService.queryValueByKey(key);
                    if (object != null) {
                        // 场次信息
                        JSONObject matchInfo = JSONObject.fromObject(object);
                        // 免费次数
                        int freeTime = matchInfo.getInt("free_double_time");
                        // 付费次数
                        int payTime = matchInfo.getInt("pay_double_time");
                        return "每场比赛有" + freeTime + "次免费加倍，" + payTime + "次使用加倍卡的机会";
                    }
                } catch (Exception e) {
                    logger.error("请启动REmote DIctionary Server");
                    return "";
                }
            }
        }
        return "";
    }
}
