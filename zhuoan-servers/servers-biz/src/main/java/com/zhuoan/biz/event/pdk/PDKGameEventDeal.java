package com.zhuoan.biz.event.pdk;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.pdk.PDKColor;
import com.zhuoan.biz.core.pdk.PDKNum;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.game.dao.util.BaseSqlUtil;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.pdk.PDKGameRoom;
import com.zhuoan.biz.model.pdk.PDKPacker;
import com.zhuoan.biz.model.pdk.PDKUserPacket;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.PDKConstant;
import com.zhuoan.constant.TeaConstant;
import com.zhuoan.constant.event.CommonEventConstant;
import com.zhuoan.constant.event.enmu.PDKEventEnum;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.service.PDKService;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.TeaService;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.Dto;
import com.zhuoan.util.JsonUtil;
import com.zhuoan.util.thread.ThreadPoolHelper;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PDKGameEventDeal {
    private final static Logger log = LoggerFactory.getLogger(PDKGameEventDeal.class);
    @Resource
    private PDKGameTimer pdkGameTimer;
    @Resource
    private PDKService pdkService;
    @Resource
    private RedisInfoService redisInfoService;
    @Resource
    private TeaService teaService;
    

    public void pdkTest(SocketIOClient client, JSONObject object, String eventName) {
        log.info("新跑得快测试接收到客户端数据：" + object.toString());
        if (object == null) CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数", eventName);
        else CommonConstant.sendMsgEventYes(client, "成功", eventName);
    }


    public void gameReadyEvent(SocketIOClient client, JSONObject object, String eventName) {
    	System.out.println("玩家准备==============开始游戏");
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.DATA_KEY_ROOM_NO, CommonConstant.DATA_KEY_ACCOUNT})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[room_no,account]", eventName);
            return;
        }
        String account = object.getString(CommonConstant.DATA_KEY_ACCOUNT);//玩家id
        String roomNo = object.getString(CommonConstant.DATA_KEY_ROOM_NO);//房间号
        PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
        Playerinfo info = room.getPlayerMap().get(account);
        List<String> listpdk = new ArrayList<>();
        try {
			JSONArray jsonArray = object.getJSONArray("pdk");
			listpdk = JSONArray.toList(jsonArray);
		} catch (Exception e) {
		}
        if (listpdk.size()>0) {
			info.setPdk(listpdk);
			ConcurrentHashMap<String, Playerinfo> plMap = room.getPlayerMap();
			plMap.put(account, info);
			room.setPlayerMap(plMap);
			RoomManage.gameRoomMap.put(roomNo, room);
		}
        
        if (null == room) return;
        //房间必须处于 初始状态或结算 准备状态
        if (PDKConstant.GAME_STATUS_INIT != room.getGameStatus() && PDKConstant.GAME_STATUS_READY != room.getGameStatus()) {
            CommonConstant.sendMsgEventNo(client, "请稍后再试", null, eventName);
            return;
        }

        PDKUserPacket up = room.getUserPacketMap().get(account);
        Playerinfo playerinfo = room.getPlayerMap().get(account);
        if (null == up || null == playerinfo) return;
        if (PDKConstant.USER_STATUS_READY == up.getStatus()) {//玩家已经处于准备
            CommonConstant.sendMsgEventNo(client, "您已经准备", null, eventName);
            return;
        }

        int roomType = room.getRoomType();//房间类型
        if (CommonConstant.ROOM_TYPE_FK == roomType && playerinfo.getPlayTimes() == 0) {//房卡
            String updateType = room.getCurrencyType();
            // 获取用户信息
            JSONObject user = DBUtil.getObjectBySQL("SELECT z.`roomcard`,z.`yuanbao` FROM  za_users z  WHERE z.`account`= ?"
                    , new Object[]{account});
            if (user == null || !user.containsKey(updateType) || user.getDouble(updateType) < room.getEnterScore()) {
                String msg = "钻石不足";
                if ("yuanbao".equals(updateType)) msg = "元宝不足";
                object = new JSONObject();
                object.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                object.put(CommonConstant.RESULT_KEY_MSG, msg);
                object.put("type", CommonEventConstant.TIP_MSG_PUSH_TYPE_EXIT);
                CommonConstant.sendMsgEvent(client, object, CommonEventConstant.TIP_MSG_PUSH);
                // 移除房间用户数据
                pdkService.exitRoomByPlayer(roomNo, playerinfo.getId(), account);
                return;
            }
        } else if (CommonConstant.ROOM_TYPE_FREE == roomType || CommonConstant.ROOM_TYPE_INNING == roomType) { //亲友圈
            double leaveScore = room.getLeaveScore();//离场验证
            if (leaveScore > Dto.add(playerinfo.getScore(), playerinfo.getSourceScore())) {
                if (room.getRoomType() == CommonConstant.ROOM_TYPE_INNING && playerinfo.getPlayTimes() > 0) {//局数场 ，有玩过的
                    for (String userAccount : room.getPlayerMap().keySet()) {
                        JSONObject result = new JSONObject();
                        result.put("type", CommonConstant.SHOW_MSG_TYPE_SMALL);
                        if (account.equals(userAccount)) {
                            result.put(CommonConstant.RESULT_KEY_MSG, "能量不足");
                        } else {
                            result.put(CommonConstant.RESULT_KEY_MSG, "【" + room.getPlayerMap().get(account).getName() + "】能量不足");
                        }
                        CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(userAccount).getUuid(), result.toString(), CommonEventConstant.TIP_MSG_PUSH);
                    }

                    //强制结算 start
                    redisInfoService.insertSummary(roomNo, "_PDK");
                    room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE);
                    room.setGameStatus(PDKConstant.GAME_STATUS_SETTLE_ACCOUNTS);
                    room.setGameIndex(room.getGameCount());
                    pdkService.settleAccounts(roomNo);
                    //强制结算 end
                    return;
                }

                JSONObject result = new JSONObject();
                result.put("type", CommonEventConstant.TIP_MSG_PUSH_TYPE_EXIT);
                result.put(CommonConstant.RESULT_KEY_MSG, "能量不足");
                CommonConstant.sendMsgEventToSingle(client, result.toString(), CommonEventConstant.TIP_MSG_PUSH);
                return;
            }
            //自由场 房费AA
            if (room.getRoomType() == CommonConstant.ROOM_TYPE_FREE && CommonConstant.ROOM_PAY_TYPE_AA.equals(String.valueOf(room.getPayType()))) {
                JSONObject user = DBUtil.getObjectBySQL("SELECT z.`roomcard` FROM  za_users z  WHERE z.`account`= ?"
                        , new Object[]{account});
                if (user == null || user.getDouble("roomcard") < room.getSinglePayNum()) {
                    JSONObject result = new JSONObject();
                    result.put("type", CommonEventConstant.TIP_MSG_PUSH_TYPE_EXIT);
                    result.put(CommonConstant.RESULT_KEY_MSG, "钻石不足");
                    CommonConstant.sendMsgEventToSingle(client, result.toString(), CommonEventConstant.TIP_MSG_PUSH);
                    return;
                }
            }
        }

        //玩家状态
        up.setStatus(PDKConstant.USER_STATUS_READY);
        //房间状态
        room.setGameStatus(PDKConstant.GAME_STATUS_READY);

        // 房间内所有玩家都已经完成准备且 人满开始游戏
        int minStartPlayer = room.getPlayerCount();//人满开始
        if (room.getPlayerMap().size() <= room.getPlayerCount() && pdkService.isAllReady(roomNo) && room.getNowReadyCount() >= minStartPlayer) {
            //茶楼房卡扣除
            if (CommonConstant.ROOM_TYPE_TEA == room.getRoomType()) {
                //满人开始 只扣除第一局
                if (0 == room.getGameIndex()) {
                    if (CommonConstant.ROOM_PAY_TYPE_TEA.equals(room.getPayType())) {//茶壶支付
                        int deductType = teaService.deductTeaMoney(roomNo, null, room.getCircleId(), room.getPayType(), room.getSinglePayNum() * room.getPlayerCount());
                        if (CommonConstant.ROOM_PAY_TYPE_TEA.equals(room.getPayType())) {//茶壶支付
                            if (TeaConstant.START_ROOM_NO == deductType || TeaConstant.START_ROOM_NO_1 == deductType || TeaConstant.START_ROOM_NO_3 == deductType) {
                                String msg = "";
                                if (TeaConstant.START_ROOM_NO == deductType) {//0茶楼不存在
                                    msg = "请刷新后再试";
                                } else if (TeaConstant.START_ROOM_NO_1 == deductType) {//1茶楼已经打烊
                                    msg = "该茶楼已经打烊";
                                } else if (TeaConstant.START_ROOM_NO_3 == deductType) {//3余额不足
                                    msg = "该茶楼茶壶不足";
                                }
                                JSONObject result = new JSONObject();
                                result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
                                result.put(CommonConstant.RESULT_KEY_MSG, msg);
                                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "tipMsgPush");
                                return;
                            }
                        }
                    } else if (CommonConstant.ROOM_PAY_TYPE_TEA_AA.equals(room.getPayType())) {
                        Map<Long, String> map = new HashMap<>();
                        for (String accountS : room.getUserPacketMap().keySet()) {
                            if (room.getUserPacketMap().containsKey(accountS) && room.getUserPacketMap().get(accountS) != null) {
                                if (room.getPlayerMap().get(accountS).getPlayTimes() == 0) {
                                    map.put(room.getPlayerMap().get(accountS).getId(), accountS);
                                }
                            }
                        }
                        if (null != map && map.size() > 0) {
                            for (Long usersId : map.keySet()) {
                                int deductType = teaService.deductTeaMoney(roomNo, usersId, room.getCircleId(), room.getPayType(), room.getSinglePayNum());
                                if (TeaConstant.START_ROOM_YES != deductType) {
                                    JSONObject postData = new JSONObject();
                                    postData.put(CommonConstant.DATA_KEY_ACCOUNT, map.get(usersId));
                                    postData.put("notSend", CommonConstant.GLOBAL_YES);
                                    exitRoomeEvent(GameMain.server.getClient(room.getPlayerMap().get(map.get(usersId)).getUuid()), postData, PDKEventEnum.PDK_EXIT_ROOM.getEventPush());
                                    return;
                                }
                            }
                        }
                    } else {
                        return;
                    }

                    //通知所有玩家
                    for (String uuid : room.getUserPacketMap().keySet()) {
                        SocketIOClient playerClient = GameMain.server.getClient(room.getPlayerMap().get(uuid).getUuid());
                        object = new JSONObject();
                        object
                                .element(CommonConstant.DATA_KEY_ACCOUNT, uuid)
                                .element(CommonConstant.DATA_KEY_ROOM_NO, roomNo)
                        ;
                        createRoom(playerClient, object);
                    }

                } else {
                    //通知所有人 该玩家准备
                    object = new JSONObject();
                    object.put("index", playerinfo.getMyIndex());
                    object.put("code", CommonConstant.GLOBAL_YES);
                    CommonConstant.sendMsgEventAll(room.getAllUUIDList(), object, eventName);
                }
            }
            //开始游戏
            pdkService.startGame(roomNo);
            return;
        }

        //通知所有人 该玩家准备
        object = new JSONObject();
        object.put("index", playerinfo.getMyIndex());
        object.put("code", CommonConstant.GLOBAL_YES);
        CommonConstant.sendMsgEventAll(room.getAllUUIDList(), object, eventName);
    }


    public void gameEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.DATA_KEY_ROOM_NO, CommonConstant.DATA_KEY_ACCOUNT})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[room_no,account]", eventName);
            return;
        }
        String account = object.getString(CommonConstant.DATA_KEY_ACCOUNT);//玩家id
        String roomNo = object.getString(CommonConstant.DATA_KEY_ROOM_NO);//房间号
        PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (null == room || PDKConstant.GAME_STATUS_START != room.getGameStatus()) {
            return;
        }

        PDKUserPacket up = room.getUserPacketMap().get(account);
        Playerinfo playerinfo = room.getPlayerMap().get(account);
        if (null == up || null == playerinfo) {
            return;
        }
        String focusAccount = room.getFocusAccount();//当前操作玩家
        if (!account.equals(focusAccount)) {
            return;//不是当前操作玩家
        }

        int cardType = PDKConstant.CARD_TYPE;//出牌类型
        List<PDKPacker> cardList = new ArrayList<>();
        if (object.containsKey("cardList")) {
            cardList = pdkService.getSortCardList(object.getJSONArray("cardList"), cardType);//出牌排序
            String lastOperateAccount = room.getLastOperateAccount();//上一个出牌玩家
            cardType = pdkService.getCardType(cardList);
            if (!lastOperateAccount.equals(account) && PDKConstant.CARD_TYPE_BOMB != cardType) {//不是炸弹或者是自己 一定要更上一次出牌牌型一样
                cardType = room.getLastCardType();//上一次出牌牌型
            }
            cardList = pdkService.getCardSortNumList(cardList, cardType);//按牌型 排序
        }

        if (PDKConstant.CARD_TYPE == cardType) {//不出牌
            //能出牌的玩家强制出牌
            if (room.isPlay()) {
                List<Map<Integer, List<PDKPacker>>> cradGroup = room.getCradGroup();
                if (null != cradGroup && cradGroup.size() > 0) {
                    Map<Integer, List<PDKPacker>> map = cradGroup.get(0);
                    for (Integer i : map.keySet()) {
                        cardType = i;
                        cardList = map.get(i);
                    }
                }
            }
        }

        final String nextPlayerAccount = pdkService.nextPlayerAccount(roomNo, account);//下家
        if (PDKConstant.CARD_TYPE != cardType) {//正常出牌 验证
            if (pdkService.verifyHeiTao3(room.getCardProcessNum(), room.getPayRules(), room.getGameIndex())) {//校验是否黑桃3先出
                if (!cardList.contains(new PDKPacker(PDKColor.HEITAO, PDKNum.P_3))) {
                    CommonConstant.sendMsgEventNo(client, "黑桃3先出", null, eventName);
                    return;
                }
            }
            //验证出牌是否符合
            if (!pdkService.checkPlayCard(roomNo, account, cardType, new ArrayList<>(cardList))) {
                log.info("上一次牌型{} cardType[{}] cardList[{}]", room.getLastOperateAccount(), cardType, room.getLastCardList());
                log.info("跑得快请选择合理的牌型 cardType[{}] cardList[{}]", cardType, cardList);
                CommonConstant.sendMsgEventNo(client, "请选择合理的牌型", null, eventName);
                return;
            }

            //单牌 必压
            if (PDKConstant.CARD_TYPE_SINGLE == cardType
                    && room.getUserPacketMap().get(nextPlayerAccount).getMyCard().size() == 1) {//下家只剩一张牌
                List<PDKPacker> myCard = up.getMyCard();//当前玩家的牌
                if (1 != cardList.size() || !cardList.get(0).getNum().equals(myCard.get(0).getNum())) {
                    CommonConstant.sendMsgEventNo(client, "请选择合理的牌型", null, eventName);
                    return;
                }
            }
        }

        int multiple = room.getMultiple();
        List<List<PDKPacker>> bombList = up.getBombList();
        if (PDKConstant.CARD_TYPE_BOMB == cardType) {//记录炸弹
            if (null == bombList) bombList = new ArrayList<>();
            bombList.add(cardList);
            multiple *= room.getBombsScore();//炸弹倍数
            room.setMultiple(multiple);//倍数
        }
        up.removeMyCard(cardList);//移除出的牌
        room.andCardProcessNum();//添加出牌流程次数
        //添加游戏流程
        room.addPDKGameFlowList(playerinfo.getMyIndex(), cardType, pdkService.getCardList(cardList), null, null, null);
        if (0 == up.getCardNum()) { //当前操作玩家 没有牌触发结算
            room.setWinner(account);//存放赢家
            up.addWinNum();//添加胜利次数
            up.setSpring(false);
            room.setLastCard(cardList);//上一次出牌
            room.setLastCardType(cardType);//上一次出牌 类型
            room.setLastOperateAccount(account);//上一个出牌玩家
            room.setLastOperateIndex(playerinfo.getMyIndex());////上一个出牌坐标

            //结算
            room.setGameStatus(PDKConstant.GAME_STATUS_SETTLE_ACCOUNTS);//结算
            room.setTimeLeft(PDKConstant.TIMER_INIT);
            pdkService.settleAccounts(roomNo);
            return;
            //局数达到触发总结算
        }

        //房间信息更新 start
        //玩家关牌修改
        up.setStatus(PDKConstant.USER_STATUS_AWAIT_PLAY);//等待出牌

        room.getUserPacketMap().get(nextPlayerAccount).setStatus(PDKConstant.USER_STATUS_PLAY);//出牌

        room.setFocusAccount(nextPlayerAccount);//当前操作玩家
        room.setFocusIndex(room.getPlayerMap().get(nextPlayerAccount).getMyIndex());//当前操作玩家 下标
        if (null != cardList && cardList.size() > 0) {//不能出牌
//            玩家关牌修改
            if (up.isSpring()) {
                if (PDKConstant.PASS_CARD_1 == room.getPassCard()) {
                    String lastOperateAccount = room.getLastOperateAccount();//上一次出牌玩家
                    if (account.equals(lastOperateAccount)) {
                        if (null != room.getLastCardList() && room.getLastCard().size() > 0) {
                            up.setSpring(false);
                        }
                    } else {
                        up.setSpring(false);
                    }
                } else {
                    up.setSpring(false);
                }
            }

            room.setLastCard(cardList);//上一次出牌
            room.setLastCardType(cardType);//上一次出牌 类型
            room.setLastOperateAccount(account);//上一个出牌玩家
            room.setLastOperateIndex(playerinfo.getMyIndex());////上一个出牌坐标
            room.setLastPlayCard(true);//上家 是否出牌
        } else {
            room.setLastPlayCard(false);//上家 是否出牌
        }
//        log.info("新跑得快当前玩家出牌{}出牌类型{} 剩余的牌{}", cardList, cardType, up.getMyCard());
        room.setLastPlayIndex(playerinfo.getMyIndex());//上家坐标


        //更新当前操作玩家牌组合
        List<Map<Integer, List<PDKPacker>>> focusCradGroupt = pdkService.getCradGroupt(roomNo);
        if (null != focusCradGroupt && focusCradGroupt.size() > 0) {
            room.setPlay(true);//当前操作玩家index 是否能出牌  true 能 flase 要不起
        } else {
            room.setPlay(false);
        }
        room.setCradGroup(focusCradGroupt);
        if (room.isPlay()) {
            room.setTimeLeft(room.getPlayTime());//出牌时间
        } else {
            room.setTimeLeft(PDKConstant.TIMER_NO_PLAY);//要不起时间
        }

        //房间信息更新 end

        //通知玩家继续游戏
        for (String s : room.getPlayerMap().keySet()) {
            up = room.getUserPacketMap().get(s);
            playerinfo = room.getPlayerMap().get(s);
            if (null == up || null == playerinfo) continue;

            //出牌倒计时 通知所有玩家
            object = new JSONObject();
            object.put("data", pdkService.playRoomData(roomNo, s));//出牌 信息
            object.put("code", CommonConstant.GLOBAL_YES);
            client = GameMain.server.getClient(playerinfo.getUuid());
            CommonConstant.sendMsgEvent(client, object, PDKEventEnum.PDK_GAME.getEventPush());//出牌通知玩家
        }

        if (room.getPlayTime() <= 0) {
            return;
        }

        final int timeLeft = room.getTimeLeft();
        final int processNum = room.getCardProcessNum();
        //出牌倒计时
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                pdkGameTimer.gamePlayOverTime(roomNo, timeLeft, nextPlayerAccount, processNum);
            }
        });


    }


    public void reconnectGameEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.DATA_KEY_ROOM_NO, CommonConstant.DATA_KEY_ACCOUNT, "uuid"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[room_no,account,uuid]", eventName);
            return;
        }
        String account = object.getString(CommonConstant.DATA_KEY_ACCOUNT);//玩家id
        String uuid = object.getString("uuid");
        String roomNo = object.getString(CommonConstant.DATA_KEY_ROOM_NO);//房间号
        if ("".equals(account) || "".equals(uuid) || "".equals(roomNo)) {
            return;
        }
        JSONObject users = BaseSqlUtil.getObjectByOneConditions("uuid", "za_users", "account", account);
        if (users == null || !users.containsKey("uuid") || !uuid.equals(users.getString("uuid"))) {
            return;
        }

        GameRoom room = RoomManage.gameRoomMap.get(roomNo);
        if (room == null) {
            CommonConstant.sendMsgEventNo(client, "房间不存在", null, eventName);
            return;
        }
        if (!room.getPlayerMap().containsKey(account) || room.getPlayerMap().get(account) == null) {
            CommonConstant.sendMsgEventNo(client, "不在当前房间内", null, eventName);
            return;
        }
        // 刷新uuid
        room.getPlayerMap().get(account).setUuid(client.getSessionId());
        object = new JSONObject();
        object.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        JSONObject roomData = pdkService.obtainRoomData(roomNo, account);
        object.put("data", roomData);
        // 通知自己
        CommonConstant.sendMsgEvent(client, object, eventName);
    }


    public void exitRoomeEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.DATA_KEY_ROOM_NO, CommonConstant.DATA_KEY_ACCOUNT})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[room_no,account]", eventName);
            return;
        }

        String account = object.getString(CommonConstant.DATA_KEY_ACCOUNT);//玩家id
        String roomNo = object.getString(CommonConstant.DATA_KEY_ROOM_NO);//房间号
        PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room == null) {
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            result.put("type", 2);
            CommonConstant.sendMsgEvent(client, result, eventName);
            return;
        }
        PDKUserPacket up = room.getUserPacketMap().get(account);
        Playerinfo player = room.getPlayerMap().get(account);
        if (up == null || player == null) {
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            result.put("type", 2);
            CommonConstant.sendMsgEvent(client, result, eventName);
            return;
        }

        int gameStatus = room.getGameStatus();//游戏状态
        int roomType = room.getRoomType();//房间类型
        if (player.getPlayTimes() > 0) {
            if (PDKConstant.GAME_STATUS_INIT != gameStatus && PDKConstant.GAME_STATUS_READY != gameStatus
                    && PDKConstant.GAME_STATUS_SETTLE_ACCOUNTS != gameStatus && PDKConstant.GAME_STATUS_SUMMARY_ACCOUNT != gameStatus) {
                CommonConstant.sendMsgEventNo(client, "游戏中无法退出", null, eventName);
                return;
            }

            // 房卡和局数场 有参与的玩家 不允许退出房间
            if (CommonConstant.ROOM_TYPE_FK == roomType || CommonConstant.ROOM_TYPE_INNING == roomType) {
                CommonConstant.sendMsgEventNo(client, "游戏中无法退出", null, eventName);
                return;
            }

        }
        Map<String, Playerinfo> allPlayerMap = new HashMap<>(room.getPlayerMap());//所有玩家

        // 移除房间用户数据
        pdkService.exitRoomByPlayer(roomNo, player.getId(), account);
        // 所有人都退出清除房间数据 需要清楚的房间类型 房卡场
        if (room.getPlayerMap().size() == 0 && CommonConstant.ROOM_TYPE_FK == roomType) {
            pdkService.removeRoom(roomNo);//移除房间
        } else {
            String isInforOwn = !object.containsKey("isInforOwn") ? "1" : object.getString("isInforOwn");//是否通知自己
            object = new JSONObject();
            object.put("index", player.getMyIndex());
            object.put("code", CommonConstant.GLOBAL_YES);
            for (String s : allPlayerMap.keySet()) {
                if ("0".equals(isInforOwn) && account.equals(s)) continue;
                CommonConstant.sendMsgEventToSingle(allPlayerMap.get(s).getUuid(), object, eventName);//通知所有人退出
            }

        }
    }

    public void closeRoomEvent(SocketIOClient client, JSONObject object, String eventName) {
    	System.out.println(object);
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.DATA_KEY_ROOM_NO, CommonConstant.DATA_KEY_ACCOUNT, "type"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[room_no,account,type]", eventName);
            return;
        }
        int type = object.getInt("type");//玩家id
        if (PDKConstant.CLOSE_ROOM_CONSENT != type && PDKConstant.CLOSE_ROOM_TURN != type && PDKConstant.CLOSE_ROOM_CONSENT_FIRST != type) {
            log.info("新跑得快解散房间type错误");
            return;
        }

        String account = object.getString(CommonConstant.DATA_KEY_ACCOUNT);//玩家id
        final String roomNo = object.getString(CommonConstant.DATA_KEY_ROOM_NO);//房间号
        PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (null == room) return;
        int roomType = room.getRoomType();
        if (room.allNoClose()) {//第一个提交解散
            String circleCode = room.getClubCode();//俱乐部
            if (CommonConstant.ROOM_TYPE_INNING == roomType || CommonConstant.ROOM_TYPE_FREE == roomType) {//俱乐部
                JSONObject member = DBUtil.getObjectBySQL("SELECT g.`id`,f.`circle_code`,z.`account`FROM game_circle_member g LEFT JOIN game_circle_info f ON g.`circle_id`=f.`id`LEFT JOIN za_users z ON g.`user_id`=z.`id`WHERE f.`circle_code`= ? AND z.`account`=? AND g.`is_delete`='N'AND(g.`user_role`=1 OR g.`is_admin`=1)"
                        , new Object[]{circleCode, account});
                if (null == member || !member.containsKey("id") || !member.containsKey("circle_code")) {
                    JSONObject result = new JSONObject();
                    result.put("type", CommonConstant.SHOW_MSG_TYPE_NORMAL);
                    result.put(CommonConstant.RESULT_KEY_MSG, "只有管理员才能发起解散");
                    CommonConstant.sendMsgEvent(client, result, "tipMsgPush");
                    return;
                }
            } else if (CommonConstant.ROOM_TYPE_TEA == roomType) {//茶楼
                //楼客权限 验证  start
                JSONObject teaMember = DBUtil.getObjectBySQL("SELECT t.`powern`FROM za_users z LEFT JOIN tea_member t ON z.`id`=t.`users_id`WHERE z.`account`= ? AND t.`tea_info_code` = ? AND t.`audit`= ? AND t.`is_deleted`= ?"
                        , new Object[]{account, circleCode, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
                if (null == teaMember || !teaMember.containsKey("powern") || TeaConstant.TEA_MEMBER_POWERN_PTCY == teaMember.getInt("powern")) {
                    JSONObject result = new JSONObject();
                    result.put("type", CommonConstant.SHOW_MSG_TYPE_NORMAL);
                    result.put(CommonConstant.RESULT_KEY_MSG, "只有管理员才能发起解散");
                    CommonConstant.sendMsgEvent(client, result, "tipMsgPush");
                    return;
                }
                //楼客权限 验证  end
            }
        }
        if (PDKConstant.CLOSE_ROOM_CONSENT_FIRST == type && 0 != room.getJieSanTime()) {
            CommonConstant.sendMsgEventNo(client, "已经提出解散房间申请", null, eventName);
            return;
        }

        if (PDKConstant.CLOSE_ROOM_CONSENT == type || PDKConstant.CLOSE_ROOM_TURN == type) {
            if (0 == room.getJieSanTime()) return;
        }

        PDKUserPacket up = room.getUserPacketMap().get(account);
        Playerinfo player = room.getPlayerMap().get(account);
        if (null == up || null == player) return;
        if (PDKConstant.CLOSE_ROOM_CONSENT_FIRST == type) type = PDKConstant.CLOSE_ROOM_CONSENT;
        player.setIsCloseRoom(type);
        object = new JSONObject();
        if (PDKConstant.CLOSE_ROOM_TURN == type) {//拒绝
            // 重置解散
            room.setJieSanTime(0);
            room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_GAME);//游戏中
            for (String s : room.getUserPacketMap().keySet()) {
                player = room.getPlayerMap().get(s);
                if (null == player) continue;
                player.setIsCloseRoom(PDKConstant.CLOSE_ROOM);
            }

            object.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            object.put("msg", new StringBuilder().append(room.getPlayerMap().get(account).getName()).append(" 拒绝解散房间").toString());
            object.put("type", type);
            CommonConstant.sendMsgEventAll(room.getAllUUIDList(), object, eventName);
            return;
        }

        if (PDKConstant.CLOSE_ROOM_CONSENT == type) {//同意
            //全部同意
            if (pdkService.isAgreeClose(roomNo)) {
                redisInfoService.insertSummary(roomNo, "_PDK");
                room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE);
                room.setGameStatus(PDKConstant.GAME_STATUS_SETTLE_ACCOUNTS);
                room.setGameIndex(room.getGameCount());
                //结算
                pdkService.settleAccounts(roomNo);
                return;
            }
            // 解散时间 开启定时器
            if (0 == room.getJieSanTime()) {
                room.setJieSanTime(PDKConstant.TIMER_CLOSE_ROOM);
                room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE);
                ThreadPoolHelper.executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        pdkGameTimer.closeRoomOverTime(roomNo, PDKConstant.TIMER_CLOSE_ROOM);
                    }
                });
            }

            object.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            object.put("type", type);
            object.put("jieSanTime", room.getJieSanTime());//解散时间
            object.put("array", pdkService.getDissolveRoomInfo(roomNo));
            CommonConstant.sendMsgEventAll(room.getAllUUIDList(), object, eventName);
        }
    }


    public void gameTrusteeEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.DATA_KEY_ROOM_NO, CommonConstant.DATA_KEY_ACCOUNT, "isTrustee"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[room_no,account,isTrustee]", eventName);
            return;
        }
        String account = object.getString(CommonConstant.DATA_KEY_ACCOUNT);//玩家id
        String roomNo = object.getString(CommonConstant.DATA_KEY_ROOM_NO);//房间号
        boolean isTrustee = object.getBoolean("isTrustee");//托管类型 ture 托管  false 取消
        PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room == null) return;
        if (PDKConstant.GAME_STATUS_START != room.getGameStatus()) {
            CommonConstant.sendMsgEventNo(client, "游戏未开始", null, eventName);
            return;
        }
        PDKUserPacket up = room.getUserPacketMap().get(account);
        Playerinfo player = room.getPlayerMap().get(account);
        if (null == up || null == player) return;
        if (isTrustee == up.isTrustee()) return;
        up.setTrustee(isTrustee);
        up.setCancelTrusteeTime(up.getCancelTrusteeTime() + 1);//添加取消托管次数
        object.put("index", player.getMyIndex());
        CommonConstant.sendMsgEventAll(room.getAllUUIDList(), object, eventName);
    }


    public void gameTipEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.DATA_KEY_ROOM_NO, CommonConstant.DATA_KEY_ACCOUNT})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[room_no,account,count]", eventName);
            return;
        }
        String account = object.getString(CommonConstant.DATA_KEY_ACCOUNT);//玩家id
        String roomNo = object.getString(CommonConstant.DATA_KEY_ROOM_NO);//房间号
        int count = object.getInt("count");
        PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room == null) return;
        if (PDKConstant.GAME_STATUS_START != room.getGameStatus()) {
            CommonConstant.sendMsgEventNo(client, "游戏未开始", null, eventName);
            return;
        }
        PDKUserPacket up = room.getUserPacketMap().get(account);
        Playerinfo player = room.getPlayerMap().get(account);
        if (null == up || null == player) return;
        List<Map<Integer, List<PDKPacker>>> cradGroup = room.getCradGroup();
        if (null == cradGroup || cradGroup.size() == 0) {
            CommonConstant.sendMsgEventNo(client, "当前玩家要不起", null, eventName);
            return;
        }
        count = count % cradGroup.size();

        object = new JSONObject();
        object.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        JSONObject obj = new JSONObject();
        if (null != cradGroup.get(count)) {
            Map<Integer, List<PDKPacker>> map = cradGroup.get(count);
            for (Integer i : map.keySet()) {
                obj.element("cardType", i);
                obj.element("cardList", pdkService.getCardList(map.get(i)));
            }
        }
        object.put("data", obj);
        object.put("count", count);
        CommonConstant.sendMsgEvent(client, object, eventName);
    }

    public void gameForcedRoomEvent(Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!postData.containsKey(CommonConstant.DATA_KEY_ROOM_NO)) return;
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (null == room) return;
        //给房间里每人通知管理员解散房间
        JSONObject object = new JSONObject();
        object.put("type", CommonEventConstant.TIP_MSG_PUSH_TYPE_CPM);
        object.put(CommonConstant.RESULT_KEY_MSG, "管理员解散房间");
        CommonConstant.sendMsgEventAll(room.getAllUUIDList(), object, CommonEventConstant.TIP_MSG_PUSH);

        room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE);
        room.setGameStatus(PDKConstant.GAME_STATUS_SETTLE_ACCOUNTS);
        room.setGameIndex(room.getGameCount());
        room.setRemoveRoom(true);//删除房间
        //结算
        pdkService.settleAccounts(roomNo);
    }

    public void createRoom(SocketIOClient client, JSONObject data) {
        String account = data.getString(CommonConstant.DATA_KEY_ACCOUNT);//用户
        String roomNo = data.getString(CommonConstant.DATA_KEY_ROOM_NO);//房间号
        JSONObject roomData = pdkService.obtainRoomData(roomNo, account);

        if (Dto.isObjNull(roomData)) {
            // 通知自己
            CommonConstant.sendMsgEventNo(client, "加入房间失败", null, PDKEventEnum.PDK_ENTER_ROOM);
        } else {
            data = new JSONObject();
            data.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            data.put("data", roomData);
           
            // 通知自己
            CommonConstant.sendMsgEvent(client, data, PDKEventEnum.PDK_ENTER_ROOM);
        }
    }


    public void joinRoom(SocketIOClient client, JSONObject data) {
        final String roomNo = data.getString(CommonConstant.DATA_KEY_ROOM_NO);//房间号
        final String account = data.getString(CommonConstant.DATA_KEY_ACCOUNT);
        PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (null == room) return;
        PDKUserPacket up = room.getUserPacketMap().get(account);
        int roomType = room.getRoomType();
        int isReconnect = CommonConstant.GLOBAL_NO;
        if (data.containsKey("isReconnect")) {
            isReconnect = data.getInt("isReconnect");
        }
        //茶楼 处理
        if (CommonConstant.ROOM_TYPE_TEA == roomType && room.getGameIndex() == 0) {
            if (CommonConstant.GLOBAL_NO == isReconnect) {
                gameReadyEvent(client, data, PDKEventEnum.PDK_GAME_READY.getEventPush());//玩家准备
            }
            return;
        }
        //游戏处理准备阶段 准备人数达到 进入准备倒计时
        if (PDKConstant.GAME_STATUS_READY == room.getGameStatus()) {
            //局数场
            if (CommonConstant.ROOM_TYPE_INNING == roomType && room.getGameIndex() > 0 || room.getNowReadyCount() >= PDKConstant.MIN_START_COUNT) {
                //准备倒计时处理
                up.setTimeLeft(PDKConstant.TIMER_READY);
                ThreadPoolHelper.executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        pdkGameTimer.gameReadyOverTime(roomNo, account, PDKConstant.TIMER_READY);
                    }
                });
            }

        }

        // 进入房间通知自己
        createRoom(client, data);
        // 非重连通知其他玩家
        if (CommonConstant.GLOBAL_NO == isReconnect) {
            JSONObject obj = pdkService.obtainPlayerInfo(roomNo, account);
            // 通知其他玩家
            CommonConstant.sendMsgEventAll(room.getAllUUIDList(account), obj, PDKEventEnum.PDK_PLAYER_ENTER);
        }
    }

}
