package com.zhuoan.service.impl;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.pdk.PDKColor;
import com.zhuoan.biz.core.pdk.PDKNum;
import com.zhuoan.biz.event.BaseEventDeal;
import com.zhuoan.biz.event.pdk.PDKGameTimer;
import com.zhuoan.biz.game.biz.GameCircleService;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.pdk.PDKGameRoom;
import com.zhuoan.biz.model.pdk.PDKPacker;
import com.zhuoan.biz.model.pdk.PDKUserPacket;
import com.zhuoan.constant.*;
import com.zhuoan.constant.event.CommonEventConstant;
import com.zhuoan.constant.event.enmu.PDKEventEnum;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.PDKService;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.TeaService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.BaseInfoUtil;
import com.zhuoan.util.DateUtils;
import com.zhuoan.util.Dto;
import com.zhuoan.util.thread.ThreadPoolHelper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PDKServiceImpl implements PDKService {

	private final Logger log = LoggerFactory.getLogger(PDKServiceImpl.class);

	@Resource
	private ProducerService producerService;
	@Resource
	private Destination daoQueueDestination;
	@Resource
	private RedisInfoService redisInfoService;
	@Resource
	private RoomBiz roomBiz;
	@Resource
	private PDKGameTimer pdkGameTimer;
	@Resource
	private BaseEventDeal baseEventDeal;
	@Resource
	private TeaService teaService;
	@Resource
	private GameCircleService gameCircleService;
	@Resource
	private Destination pdkQueueDestination;
	@Resource
	private UserBiz userBiz;

	@Override
	public JSONObject obtainRoomData(String roomNo, String account) {
		JSONObject obj = new JSONObject();
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		PDKUserPacket up = room.getUserPacketMap().get(account);
		// 获取pdk背景设置 如果没有 则为1
		Playerinfo playerinfo = room.getPlayerMap().get(account);
		JSONObject userObject = userBiz.getUserByAccount(account);
		JSONObject jb = userBiz.getUserBgSetByUserId(userObject.getLong("id"));
		String bg = "0";
		if (Dto.isNull(jb)) {
			// 没有数据 存进去一个
			JSONObject object = new JSONObject();
			object.put("user_id", userObject.getLong("id"));
			object.put("pdkbg", 0);
			object.put("sssbg", 0);
			object.put("mjbg", 0);
			userBiz.addUserGameSet(object);
		} else {
			bg = jb.getString("pdkbg");
		}

		if (null == room || null == up || null == playerinfo)
			return obj;
		obj.put("bg", bg);
		obj.put("playerCount", room.getPlayerCount());
		obj.put("gid", room.getId());// 游戏id
		obj.put("roomNo", roomNo);// 房间号
		obj.put("gameIndex", room.getGameNewIndex());// 当前局数
		obj.put("gameCount", room.getGameCount());// 游戏总局数
		obj.put("gameStatus", room.getGameStatus());// 游戏状态
		obj.put("roomType", room.getRoomType());// 房间类型
		obj.put("myIndex", playerinfo.getMyIndex());// 玩家座位号
		obj.put("time", room.getTimeLeft());// 房间倒计时
		obj.put("playerTime", room.getPlayTime());// 出牌时间
		obj.put("paytype", room.getPayType());// 房费

		obj.put("multiple", room.getMultiple());// 当前房间倍数
		obj.put("focusIndex", room.getFocusIndex());// 当前操作玩家index
		obj.put("isPlay", room.isPlay());// 当前操作玩家index 是否能出牌
		obj.put("lastCardList", room.getLastCardList());// 上一次出牌 牌型
		obj.put("lastCardType", room.getLastCardType());// 上一次出牌 类型
		obj.put("lastOperateIndex", room.getLastOperateIndex());// 上一个出牌玩家index
		obj.put("isLastPlayCard", room.isLastPlayCard());// 上家是否出牌
		obj.put("lastPlayIndex", room.getLastPlayIndex());// 上家坐标
		obj.put("heiTaoAccount", room.getHeiTaoAccount());// 黑桃3玩家
		obj.put("heiTaoAccountIndex", room.getHeiTaoAccountIndex());// 黑桃3玩家下标
		obj.put("waysType", room.getWaysType());// 玩法选择
		obj.put("payRules", room.getPayRules());// 先出规则
		obj.put("passCard", room.getPassCard());// 是否反春
		obj.put("settleType", room.getSettleType());// 结算类型
		obj.put("threeType", room.getThreeType());// 三张带几
		obj.put("fourType", room.getFourType());// 四张带几
		obj.put("bombsScore", room.getBombsScore());// 炸弹倍数
		obj.put("score", room.getScore());// 底分
		obj.put("cardProcessNum", room.getCardProcessNum());// 出牌流程次数
		obj.put("users", obtainAllPlayer(roomNo));// 玩家基本信息
		obj.put("gameData", getGameData(roomNo, account));// 游戏信息
		StringBuilder roomInfo = new StringBuilder();
		roomInfo.append(room.getWaysType()).append("张 ").append(room.getPlayerCount()).append("人 ");
		if (PDKConstant.PAY_RULES_3 == room.getPayRules()) {
			roomInfo.append("黑桃3出 ");
		} else if (PDKConstant.PAY_RULES_1 == room.getPayRules()) {
			roomInfo.append("赢家出 ");
		} else if (PDKConstant.PAY_RULES_0 == room.getPayRules()) {
			roomInfo.append("轮庄出 ");
		}
		roomInfo.append(room.getBombsScore()).append("倍炸弹 ");
		if (PDKConstant.PASS_CARD_1 == room.getPassCard()) {
			roomInfo.append("反春 ");
		} else {
			roomInfo.append("不反春 ");
		}
		if (PDKConstant.SETTLE_TYPE_1 == room.getSettleType()) {
			roomInfo.append("按张数 ");
		} else {
			roomInfo.append("大小头").append(room.getSettleType()).append("/").append(room.getSettleType() / 2)
					.append(" ");
		}
		roomInfo.append("底注：").append(room.getScore());

		obj.put("roomInfo", roomInfo.toString());// 房间基本信息
		if (null != room.getSummaryData() && room.getSummaryData().containsKey("array")) {// 结算
			obj.put("summaryData", room.getSummaryData());
		}

		obj.put("isClose", room.getIsClose());// -1 解散房间
		if (room.getIsClose() == CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE) {
			JSONObject jieSan = new JSONObject();
			jieSan.put("array", getDissolveRoomInfo(roomNo));
			jieSan.put("jieSanTime", room.getJieSanTime());// 解散时间
			obj.put("jieSan", jieSan);
		}
		return obj;
	}

	@Override
	public JSONArray obtainAllPlayer(String roomNo) {
		JSONArray array = new JSONArray();
		for (String account : obtainAllPlayerAccount(roomNo, true)) {
			JSONObject playerInfo = obtainPlayerInfo(roomNo, account);
			if (!Dto.isObjNull(playerInfo)) {
				array.add(playerInfo);
			}
		}
		return array;
	}

	@Override
	public List<String> obtainAllPlayerAccount(String roomNo, boolean isAll) {
		List<String> accountList = new ArrayList<>();
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return accountList;
		for (String account : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null
					&& room.getPlayerMap().containsKey(account) && room.getPlayerMap().get(account) != null) {
				if (!isAll && room.getUserPacketMap().get(account).getStatus() == PDKConstant.USER_STATUS_INIT)
					continue;
				accountList.add(account);
			}
		}
		return accountList;
	}

	@Override
	public JSONObject obtainPlayerInfo(String roomNo, String account) {
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		JSONObject obj = new JSONObject();
		Playerinfo playerinfo = room.getPlayerMap().get(account);
		if (null == playerinfo)
			return obj;
		obj.put("location", playerinfo.getLocation());// 玩家坐标
		obj.put("account", playerinfo.getAccount());// 玩家id
		obj.put("name", playerinfo.getName());// 昵称
		obj.put("headimg", playerinfo.getRealHeadimg());// 头像
		obj.put("sex", playerinfo.getSex());// 性别
		obj.put("ip", playerinfo.getIp());
		obj.put("vip", playerinfo.getVip());
		obj.put("area", playerinfo.getArea());// 地区
		obj.put("score", Dto.add(playerinfo.getScore(), playerinfo.getSourceScore()));// 玩家分数
		obj.put("index", playerinfo.getMyIndex());// 座位号
		obj.put("userOnlineStatus", playerinfo.getStatus());// 在线状态
		PDKUserPacket up = room.getUserPacketMap().get(account);
		if (null != up) {
			obj.put("userStatus", up.getStatus());// 玩家游戏状态
			obj.put("time", up.getTimeLeft());// 玩家倒计时
		}
		return obj;
	}

	@Override
	public List<JSONObject> getGameData(String roomNo, String account) {
		List<JSONObject> data = new ArrayList<>();
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		int gameStatus = room.getGameStatus();// 游戏状态

		boolean isCardVisible = false;// 牌是否可见
		if (PDKConstant.GAME_STATUS_SETTLE_ACCOUNTS == gameStatus
				|| PDKConstant.GAME_STATUS_SUMMARY_ACCOUNT == gameStatus) {// 结算 总结算
			isCardVisible = true;
		}
		for (String s : room.getPlayerMap().keySet()) {
			PDKUserPacket up = room.getUserPacketMap().get(s);
			Playerinfo playerinfo = room.getPlayerMap().get(s);
			if (up == null || playerinfo == null)
				continue;

			JSONObject obj = new JSONObject();

			if (isCardVisible || account.equals(s))
				obj.put("pai", up.getMyPai());// 牌
			else
				obj.put("pai", up.getMyBMPai());

			obj.put("cardNum", up.getCardNum());// 牌数
			obj.put("index", playerinfo.getMyIndex());// 下标
			if (PDKConstant.GAME_STATUS_SETTLE_ACCOUNTS == gameStatus
					|| PDKConstant.GAME_STATUS_SUMMARY_ACCOUNT == gameStatus) { // 结算 总结算
				obj.put("trusteeStatus", false);
			} else {
				obj.put("trusteeStatus", up.isTrustee());// 是否托管
			}
			if (PDKConstant.GAME_STATUS_SETTLE_ACCOUNTS == gameStatus) {// 结算
				obj.put("sum", up.getScore());
				obj.put("scoreLeft", playerinfo.getScore());
			}
			data.add(obj);
		}
		return data;
	}

	@Override
	public JSONArray getDissolveRoomInfo(String roomNo) {
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		JSONArray array = new JSONArray();
		for (String s : room.getUserPacketMap().keySet()) {
			PDKUserPacket up = room.getUserPacketMap().get(s);
			Playerinfo player = room.getPlayerMap().get(s);
			if (up == null || player == null)
				continue;
			JSONObject obj = new JSONObject();
			obj.put("index", player.getMyIndex());
			obj.put("name", player.getName());
			obj.put("type", player.getIsCloseRoom());
			array.add(obj);
		}
		return array;
	}

	@Override
	public void exitRoomByPlayer(String roomNo, long userId, String account) {
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
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
		room.getVisitPlayerMap().remove(account); // 观战人员直接退出
		room.getUserPacketMap().remove(account);
		roomInfo.put("player_number", room.getPlayerMap().size());
		producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_QUIT_USER_ROOM, roomInfo));
// 退出房间更新数据 end
	}

	@Override
	public void settleAccounts(String roomNo) {
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		// 房间处于结算状态
		if (null == room || PDKConstant.GAME_STATUS_SETTLE_ACCOUNTS != room.getGameStatus()
				&& CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE != room.getIsClose()) {
			return;
		}
		int roomType = room.getRoomType();// 房间类型
		// 未玩完一局不需要强制结算
		if (CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE == room.getIsClose()) {
			if (CommonConstant.ROOM_TYPE_FREE == roomType
					|| (CommonConstant.ROOM_TYPE_INNING == roomType && room.getGameNewIndex() <= 1
							&& (null == room.getSummaryData() || !room.getSummaryData().containsKey("array")))) {// 清除房间里所有人
				dissolveRoomInform(roomNo, room.getAllUUIDList());// 通知玩家
				if (room.isRemoveRoom()) {
					removeRoom(roomNo);// 移除房间
				} else {
					clearRoomInfo(roomNo);// 处理清空房间，重新生成
				}
				return;
			} else if ((CommonConstant.ROOM_TYPE_FK == roomType || CommonConstant.ROOM_TYPE_TEA == roomType)
					&& room.getGameNewIndex() <= 1
					&& (room.getSummaryData() == null || !room.getSummaryData().containsKey("array"))) {// 清除房间
				dissolveRoomInform(roomNo, room.getAllUUIDList());// 通知玩家
				removeRoom(roomNo);// 移除房间
				return;
			}
		}
		long summaryTimes = redisInfoService.summaryTimes(roomNo, "_PDK");
		if (summaryTimes > 1)
			return;
		redisInfoService.delSummary(roomNo, "_PDK");
		if (CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE != room.getIsClose() && !"-1".equals(room.getWinner())) {// 解散房间
																												// 分数不变
			String winner = room.getWinner();// 赢家

			List<Integer> settleList = new ArrayList<>();
			// 大小头
			if (PDKConstant.SETTLE_TYPE_1 != room.getSettleType()) {// 不是按张数来
				for (String account : room.getUserPacketMap().keySet()) {
					if (account.equals(winner))
						continue;// 非赢家
					PDKUserPacket up = room.getUserPacketMap().get(account);
					settleList.add(up.getCardNum());
				}
				Collections.sort(settleList, Collections.reverseOrder());// 降序
				if (settleList.size() == 0) {
					log.info("新跑得快大小头结算错误");
					return;
				}
			}
			int multiple = room.getMultiple() * room.getScore();//保底分数=倍数*底
            Map<String, Integer> scoreMap = new HashMap<>();
            for (String account : room.getUserPacketMap().keySet()) {
                if (account.equals(winner)) continue;//非赢家
                PDKUserPacket up = room.getUserPacketMap().get(account);
                int cardNum = up.getCardNum();
                if (PDKConstant.SETTLE_TYPE_1 != room.getSettleType()) {//不是按张数来
                    if (cardNum == settleList.get(0)) {
                        cardNum = room.getSettleType();
                    } else {
                        cardNum = room.getSettleType() / 2;
                    }
                }
                if (up.isSpring()) {//关牌
                    cardNum *= 2;//春天两倍
                }

                scoreMap.put(account, multiple * cardNum);
            }
            //输家扣分
            double winnerScore = 0D;
            for (String account : scoreMap.keySet()) {
                double score = scoreMap.get(account);
                PDKUserPacket up = room.getUserPacketMap().get(account);
                Playerinfo playerinfo = room.getPlayerMap().get(account);
                up.setScore(-score);
                playerinfo.setScore(Dto.sub(playerinfo.getScore(), score));
                winnerScore = Dto.add(winnerScore, score);
            }
            //赢家加分
            PDKUserPacket up = room.getUserPacketMap().get(winner);
            Playerinfo playerinfo = room.getPlayerMap().get(winner);
            up.setScore(winnerScore);
            playerinfo.setScore(Dto.add(playerinfo.getScore(), winnerScore));
        }
        saveGameLog(roomNo);//保存战绩
        setSummaryData(roomNo);//保存结算数据
		// 总结算
		if (room.isNeedFinalSummary()) {
			// 最后一局 或解散房间
			if (room.getGameIndex() >= room.getGameCount()
					|| CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE == room.getIsClose()) {
				room.setGameStatus(PDKConstant.GAME_STATUS_SUMMARY_ACCOUNT);
				setFinalSummaryData(roomNo);// 保存总结算数据
			}
		}

		// 数据库操作 玩家 房卡扣除 亲友圈房卡扣除不走这里
		// 第一局
		if (CommonConstant.ROOM_TYPE_FK == roomType) {
			if (1 == room.getGameIndex() && room.getSummaryData() != null
					&& room.getSummaryData().containsKey("array")) {
				updateRoomCard(roomNo);// 扣除房卡
			}
		}

		// 数据库操作 结算 玩家 分数扣除 抽水
		if (CommonConstant.ROOM_TYPE_FK == roomType || CommonConstant.ROOM_TYPE_FREE == roomType
				|| CommonConstant.ROOM_TYPE_INNING == roomType || CommonConstant.ROOM_TYPE_TEA == roomType) { // 自由场
																												// 或房卡场
																												// 茶楼
			updateUserScore(roomNo);
		}

		room.setTimeLeft(PDKConstant.TIMER_INIT);
		// 总结算 房间处理
		if (room.isNeedFinalSummary()) {
			if (room.getGameIndex() >= room.getGameCount()
					|| CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE == room.getIsClose()) {
				if (CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE == room.getIsClose())
					room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_FINISH);// 解散结束
				else
					room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_OVER);// 游戏结束
			}
		}
		// 通知玩家
		for (String account : room.getUserPacketMap().keySet()) {
			PDKUserPacket up = room.getUserPacketMap().get(account);
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (null == up || null == playerinfo)
				continue;
			SocketIOClient client = GameMain.server.getClient(playerinfo.getUuid());
			JSONObject object = new JSONObject();
			object.put("lastCardList", room.getLastCardList());// 上一次出牌
			object.put("lastCardType", room.getLastCardType());// 上一次出牌 类型
			object.put("lastOperateIndex", room.getLastOperateIndex());// 上一个出牌玩家index
			object.put("gameStatus", room.getGameStatus());
			object.put("isClose", room.getIsClose());
			object.put("summaryData", room.getSummaryData());
			object.put("finalSummaryData", room.getFinalSummaryData());
			CommonConstant.sendMsgEvent(client, object, PDKEventEnum.PDK_SETTLE_ACCOUNTS);// 结算回调
		}

		// 总结算 房间处理
		if (room.isNeedFinalSummary()) {
			// 最后一局 或解散房间
			if (room.getGameIndex() >= room.getGameCount()
					|| CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE == room.getIsClose()
					|| CommonConstant.CLOSE_ROOM_TYPE_FINISH == room.getIsClose()
					|| CommonConstant.CLOSE_ROOM_TYPE_OVER == room.getIsClose()) {
				if (room.isRemoveRoom()) {
					removeRoom(roomNo);// 移除房间
					return;
				} else if (CommonConstant.ROOM_TYPE_FK == roomType || CommonConstant.ROOM_TYPE_TEA == roomType) {// 房卡场
																													// 茶楼
					removeRoom(roomNo);// 移除房间
					return;
				} else if (CommonConstant.ROOM_TYPE_INNING == roomType) {// 局数场
					clearRoomInfo(roomNo);// 处理清空房间，重新生成
					return;
				}
			}
		}

		// 倒计时通知玩家
		if (CommonConstant.ROOM_TYPE_FREE == roomType) {// 自由场
			clearRoomInfo(roomNo);// 处理清空房间，重新生成
			return;
		} else if (CommonConstant.ROOM_TYPE_INNING == roomType || CommonConstant.ROOM_TYPE_FK == roomType
				|| CommonConstant.ROOM_TYPE_TEA == roomType) {// 局数场 房卡场 茶楼
			room.setGameStatus(PDKConstant.GAME_STATUS_READY);

			int time = redisInfoService.getAppTimeSetting(room.getPlatform(), room.getGid(),
					"PDKConstant.TIMER_READY_INNING");
			if (-99 == time) {
				time = PDKConstant.TIMER_READY_INNING;
			}
			final int overTime = time;

			for (final String account : room.getUserPacketMap().keySet()) {
				PDKUserPacket up = room.getUserPacketMap().get(account);
				Playerinfo playerinfo = room.getPlayerMap().get(account);
				if (up == null || playerinfo == null)
					continue;
				up.setStatus(PDKConstant.USER_STATUS_INIT);// 初始
				up.setTimeLeft(overTime);// 存入倒计时变化
				JSONObject object = new JSONObject();
				object.put("index", playerinfo.getMyIndex());
				object.put("time", overTime);
				CommonConstant.sendMsgEventToSingle(playerinfo.getUuid(), object, PDKEventEnum.PDK_READY_TIMER); // 通知不是准备玩家
				if (overTime >= 0) {
					// 倒计时 定时
					ThreadPoolHelper.executorService.submit(new Runnable() {
						@Override
						public void run() {
							pdkGameTimer.gameReadyOverTime(roomNo, account, overTime);
						}
					});
				}

			}
		}
	}

	private void saveGameLog(String roomNo) {
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room || CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE == room.getIsClose()
				&& PDKConstant.GAME_STATUS_SETTLE_ACCOUNTS != room.getGameStatus()
				&& PDKConstant.GAME_STATUS_SUMMARY_ACCOUNT != room.getGameStatus())
			return;

		JSONArray gameResult = new JSONArray();
		JSONArray array = new JSONArray();
		// 存放游戏记录
		for (String account : room.getUserPacketMap().keySet()) {
			PDKUserPacket up = room.getUserPacketMap().get(account);
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (up == null || playerinfo == null)
				continue;
			if (PDKConstant.USER_STATUS_INIT == up.getStatus() || PDKConstant.USER_STATUS_READY == up.getStatus())
				continue;
			JSONObject obj = new JSONObject();
			obj.put("id", playerinfo.getId());
			obj.put("total", playerinfo.getScore());
			obj.put("fen", up.getScore());
			array.add(obj);
			// 用户战绩
			JSONObject userResult = new JSONObject();
			userResult.put("account", account);
			userResult.put("isWinner", CommonConstant.GLOBAL_NO);
			if (up.getScore() > 0D) {
				userResult.put("isWinner", CommonConstant.GLOBAL_YES);
			}
			userResult.put("score", up.getScore());
			userResult.put("totalScore", playerinfo.getScore());
			userResult.put("player", playerinfo.getName());
			gameResult.add(userResult);

		}

		// 战绩信息
		JSONObject gameLogObj = room.obtainGameLog(gameResult.toString(),
				JSONArray.fromObject(room.getPdkGameFlowList()).toString());
		JSONObject o = new JSONObject();
		JSONArray userGameLogs = room.obtainUserGameLog(gameLogObj.getLong("id"), array);
		o.element("array", userGameLogs);
		o.element("object",
				new JSONObject().element("gamelog_id", gameLogObj.getLong("id"))
						.element("room_type", room.getRoomType()).element("room_id", room.getId())
						.element("room_no", room.getRoomNo()).element("game_index", room.getGameNewIndex())
						.element("result", gameResult.toString()));
		producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_USER_GAME_LOG, o));
		// 保存游戏记录
		producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_GAME_LOG, gameLogObj));

	}

	@Override
	public void dissolveRoomInform(String roomNo, List<UUID> uuidList) {
		// 通过玩家
		JSONObject object = new JSONObject();
		object.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		object.put("type", PDKConstant.CLOSE_ROOM_CONSENT_ALL);
		CommonConstant.sendMsgEventAll(uuidList, object, PDKEventEnum.PDK_CLOSE_ROOM.getEventPush());// 通知玩家解散房间
		redisInfoService.delSummary(roomNo, "_PDK");
	}

	@Override
	public void removeRoom(String roomNo) {
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		// 房间解散
		redisInfoService.delSummary(roomNo, "_PDK");
		JSONObject roomInfo = new JSONObject();
		roomInfo.put("room_no", room.getRoomNo());
		roomInfo.put("status", CommonConstant.CLOSE_ROOM_TYPE_OVER);
		roomInfo.put("game_index", 0);
		producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
		RoomManage.gameRoomMap.remove(roomNo);
	}

	@Override
	public void clearRoomInfo(String roomNo) {
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (room == null)
			return;
		room.initGame();
		room.setGameIndex(0);
		room.setGameNewIndex(0);
		room.getSummaryData().clear();
		room.setGameStatus(PDKConstant.GAME_STATUS_INIT);
		room.setJieSanTime(0);
		room.getFinalSummaryData().clear();
		room.setUserPacketMap(new ConcurrentHashMap<>());
		room.setPlayerMap(new ConcurrentHashMap<>());
		room.setVisitPlayerMap(new ConcurrentHashMap<>());
		for (int i = 0; i < room.getUserIdList().size(); i++) {
			room.getUserIdList().set(i, 0L);
			room.addIndexList(i);
		}
		room.setWinner("-1");
		// 处理清空房间，重新生成
		baseEventDeal.reloadClearRoom(roomNo);
	}

	@Override
	public void updateRoomCard(String roomNo) {
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		String payType = room.getPayType();// 房间支付类型
		JSONArray array = new JSONArray();
		int roomCardCount = 0;
		for (String account : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
				// 房主支付
				if (CommonConstant.ROOM_PAY_TYPE_FANGZHU.equals(payType)) {
					if (account.equals(room.getOwner())) {
						// 参与第一局需要扣房卡
						if (room.getPlayerMap().get(account).getPlayTimes() == 1) {
							roomCardCount = room.getPlayerCount() * room.getSinglePayNum();
							array.add(room.getPlayerMap().get(account).getId());
						}
					}
				}
				// 房费AA
				if (CommonConstant.ROOM_PAY_TYPE_AA.equals(payType)) {
					// 参与第一局需要扣房卡
					if (room.getPlayerMap().get(account).getPlayTimes() == 1) {
						array.add(room.getPlayerMap().get(account).getId());
						roomCardCount = room.getSinglePayNum();
					}
				}
			}
		}
		if (array.size() > 0) {
			producerService.sendMessage(daoQueueDestination,
					new PumpDao(DaoTypeConstant.PUMP, room.getRoomCardChangeObject(array, roomCardCount)));
		}
	}

	@Override
	public boolean isAllReady(String roomNo) {
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return false;
		// 人数验证
		if (room.getPlayerMap().size() != room.getUserPacketMap().size()) {
			return false;
		}

		int count = 0;
		for (String account : room.getUserPacketMap().keySet()) {
			PDKUserPacket up = room.getUserPacketMap().get(account);
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (up == null || playerinfo == null)
				return false;
			if (PDKConstant.USER_STATUS_READY != up.getStatus())
				return false;
			count++;
		}
		if (count < 2) {// 最小开始人数
			return false;
		}
		return true;
	}

	@Override
	public void startGame(String roomNo) {
		redisInfoService.insertSummary(roomNo, "_PDK");
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room || !isAllReady(roomNo)) {
			return;
		}

		if (PDKConstant.GAME_STATUS_READY != room.getGameStatus()) {
			return;
		}
		int roomType = room.getRoomType();// 房间类型
		if (CommonConstant.ROOM_TYPE_FREE == roomType || CommonConstant.ROOM_TYPE_INNING == roomType) {
			if (!circleFKDeduct(roomNo))
				return;// 亲友圈扣除房卡
		}

		// 初始化房间信息
		room.initGame();
		// 添加房间局数
		JSONObject roomInfo = new JSONObject();
		roomInfo.put("roomId", room.getId());
		producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INDEX, roomInfo));

		boolean isHeiTao3 = verifyHeiTao3(room.getCardProcessNum(), room.getPayRules(), room.getGameIndex());// 是否必带黑桃3

		// 洗牌发牌
		shuffleDeing(roomNo, isHeiTao3);

		// 测试 start1111111111111

//        List<PDKPacker> packerList = new ArrayList<>();
//        PDKUserPacket pdkUserPacket = room.getUserPacketMap().get("888888");
//        if (null != pdkUserPacket && pdkUserPacket.getMyCard().contains(new PDKPacker(PDKColor.HEITAO, PDKNum.P_3))) {
//            packerList.add(new PDKPacker(PDKColor.HEITAO, PDKNum.P_3));//黑桃3
//        }
//        packerList.add(new PDKPacker(PDKColor.MEIHAU, PDKNum.P_3));
//        packerList.add(new PDKPacker(PDKColor.MEIHAU, PDKNum.P_4));
//        packerList.add(new PDKPacker(PDKColor.FANGKUAI, PDKNum.P_4));
//        packerList.add(new PDKPacker(PDKColor.HONGTAO, PDKNum.P_5));
//        packerList.add(new PDKPacker(PDKColor.HEITAO, PDKNum.P_5));
//        packerList.add(new PDKPacker(PDKColor.MEIHAU, PDKNum.P_A));
//        packerList.add(new PDKPacker(PDKColor.HEITAO, PDKNum.P_A));
//        packerList.add(new PDKPacker(PDKColor.HONGTAO, PDKNum.P_A));
//        packerList.add(new PDKPacker(PDKColor.HONGTAO, PDKNum.P_2));
//        pdkUserPacket.initMyCard(packerList);
//
//        packerList = new ArrayList<>();
//        pdkUserPacket = room.getUserPacketMap().get("666666");
//        if (null != pdkUserPacket && pdkUserPacket.getMyCard().contains(new PDKPacker(PDKColor.HEITAO, PDKNum.P_3))) {
//            packerList.add(new PDKPacker(PDKColor.HEITAO, PDKNum.P_3));//黑桃3
//        }
//        packerList.add(new PDKPacker(PDKColor.MEIHAU, PDKNum.P_4));
//        packerList.add(new PDKPacker(PDKColor.FANGKUAI, PDKNum.P_4));
//        packerList.add(new PDKPacker(PDKColor.HONGTAO, PDKNum.P_5));
//        packerList.add(new PDKPacker(PDKColor.MEIHAU, PDKNum.P_5));
//        packerList.add(new PDKPacker(PDKColor.FANGKUAI, PDKNum.P_6));
//        packerList.add(new PDKPacker(PDKColor.MEIHAU, PDKNum.P_A));
//        packerList.add(new PDKPacker(PDKColor.HEITAO, PDKNum.P_A));
//        packerList.add(new PDKPacker(PDKColor.HONGTAO, PDKNum.P_7));
//        packerList.add(new PDKPacker(PDKColor.HONGTAO, PDKNum.P_2));
//
//        pdkUserPacket.initMyCard(packerList);

		// 测试 end1111111111111

		// 测试 start22222222222222

//        List<PDKPacker> packerList = new ArrayList<>();
//        PDKUserPacket pdkUserPacket = room.getUserPacketMap().get("16803801");
//        String[] a = new String[]{"4-14", "1-14", "1-14", "3-15", "4-10", "4-9", "3-8", "2-7", "1-7", "2-7", "1-6", "3-6", "2-6", "1-8", "4-8", "3-8"};
//        for (int i = 0; i < a.length; i++) {
//            packerList.add(new PDKPacker(a[i]));
//        }
//        pdkUserPacket.initMyCard(packerList);
//
//        packerList = new ArrayList<>();
//        pdkUserPacket = room.getUserPacketMap().get("11062664");
//        a = new String[]{"2-15", "4-14", "4-14", "3-12", "2-12", "4-11", "3-10", "1-5", "3-5", "2-5", "4-4", "1-4", "3-4", "4-3", "2-3", "1-3"};
//        for (int i = 0; i < a.length; i++) {
//            packerList.add(new PDKPacker(a[i]));
//        }
//        pdkUserPacket.initMyCard(packerList);

		// 测试 end22222222222222

		// 先出玩家
		String focusAccount = "-1";
		int focusIndex = -1;

		// 获取每个玩家的牌
		for (String account : room.getUserPacketMap().keySet()) {
			PDKUserPacket up = room.getUserPacketMap().get(account);
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (null == up || null == playerinfo || 0 == up.getMyCard().size())
				continue;
			List<PDKPacker> playerCard = up.getMyCard();
			for (PDKPacker pdkPacker : playerCard) {
				if (PDKColor.HEITAO.equals(pdkPacker.getColor()) && PDKNum.P_3.equals(pdkPacker.getNum())) {
					room.setHeiTaoAccount(account);// 黑桃3玩家
					room.setHeiTaoAccountIndex(room.getPlayerMap().get(account).getMyIndex());
					if (isHeiTao3) {// 第一局 和 黑桃3先出
						focusAccount = account;
						focusIndex = room.getPlayerMap().get(account).getMyIndex();
						room.setPlayerCardAccount(account);// 记录本局出牌玩家
					}

				}
			}
		}
		// 决定谁先出牌
		if ("-1".equals(focusAccount)) {
			if (PDKConstant.PAY_RULES_1 == room.getPayRules()) {// 赢家出牌
				focusAccount = room.getWinner();
			} else if (PDKConstant.PAY_RULES_0 == room.getPayRules()) {// 轮庄
				focusAccount = nextPlayerAccount(roomNo, room.getPlayerCardAccount());
			}
			if (!"-1".equals(focusAccount)) {
				focusIndex = room.getPlayerMap().get(focusAccount).getMyIndex();
				room.setPlayerCardAccount(focusAccount);// 更新本局出牌玩家
			}
		}
		if ("-1".equals(focusAccount)) {
			log.info("新跑得快获取出牌玩家错误");
			return;
		}

		// 开始游戏通知前端
		room.setGameStatus(PDKConstant.GAME_STATUS_START);
		room.setLastOperateAccount(focusAccount);// 上一个出牌玩家
		room.setLastOperateIndex(focusIndex);// 上一个出牌玩家
		room.setFocusAccount(focusAccount);// 当前操作玩家
		room.setFocusIndex(focusIndex);// 当前操作玩家index
		room.setLastCard(new ArrayList<>());// 上一次出牌
		room.setLastCardType(PDKConstant.CARD_TYPE);// 牌类型
		room.setPlay(true);// 当前操作玩家index 能出牌
		room.setTimeLeft(room.getPlayTime());// 第一次出牌时间
		room.getUserPacketMap().get(focusAccount).setStatus(PDKConstant.USER_STATUS_PLAY);// 出牌
		room.setCradGroup(getCradGroupt(roomNo));// 当前操作玩家 可出牌组合

		for (String account : room.getPlayerMap().keySet()) {
			PDKUserPacket up = room.getUserPacketMap().get(account);
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (null == up || null == playerinfo)
				continue;
			if (account.equals(room.getFocusAccount())) {
				up.setStatus(PDKConstant.USER_STATUS_PLAY);// 出牌
			} else {
				up.setStatus(PDKConstant.USER_STATUS_AWAIT_PLAY);// 等待出牌
			}

			// 出牌倒计时 通知所有玩家
			JSONObject object = new JSONObject();
			object.put("data", playRoomData(roomNo, account));// 出牌 信息
			object.put("code", CommonConstant.GLOBAL_YES);
			SocketIOClient client = GameMain.server.getClient(playerinfo.getUuid());
			CommonConstant.sendMsgEvent(client, object, PDKEventEnum.PDK_GAME.getEventPush());// 出牌通知玩家
			// 添加游戏流程
			room.addPDKGameFlowList(playerinfo.getMyIndex(), PDKConstant.CARD_TYPE_INIT, up.getMyPai(), account,
					playerinfo.getHeadimg(), playerinfo.getName());
		}

		// 出牌倒计时
		ThreadPoolHelper.executorService.submit(new Runnable() {
			@Override
			public void run() {
				pdkGameTimer.gamePlayOverTime(roomNo, room.getTimeLeft(), room.getFocusAccount(),
						room.getCardProcessNum());
			}
		});
	}

	@Override
	public JSONObject playRoomData(String roomNo, String account) {
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		JSONObject obj = new JSONObject();
		obj.put("gameStatus", room.getGameStatus());// 游戏状态
		obj.put("gameIndex", room.getGameNewIndex());// 当前局数
		obj.put("cardProcessNum", room.getCardProcessNum());// 出牌流程次数

		obj.put("time", room.getTimeLeft());// 房间倒计时
		obj.put("playTime", room.getPlayTime());// 出牌时间
		obj.put("focusIndex", room.getFocusIndex());// 当前操作玩家index
		obj.put("isPlay", room.isPlay());// 当前操作玩家index 是否能出牌
		obj.put("lastCardList", room.getLastCardList());// 上一次出牌
		obj.put("lastCardType", room.getLastCardType());// 上一次出牌 类型
		obj.put("lastOperateIndex", room.getLastOperateIndex());// 上一个出牌玩家index
		obj.put("isLastPlayCard", room.isLastPlayCard());// 上家是否出牌
		obj.put("lastPlayIndex", room.getLastPlayIndex());// 上家坐标
		obj.put("multiple", room.getMultiple());// 倍数
		obj.put("gameData", getGameData(roomNo, account));// 游戏信息
		return obj;
	}

	@Override
	public List<PDKPacker> getSortCardList(List<String> paiList, int cardType) {
		List<PDKPacker> packerList = new ArrayList<>();
		if (null == paiList || paiList.size() == 0) {
			return packerList;
		}
		try {
			for (String s : paiList) {
				packerList.add(new PDKPacker(s));
			}
			return getCardSortNumList(packerList, cardType);
		} catch (Exception e) {
			log.info("新跑得快返回排序后的牌 [{}]", e);
			return packerList;
		}
	}

	@Override
	public boolean checkPlayCard(String roomNo, String account, int cardType, List<PDKPacker> cardList) {
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		PDKUserPacket up = room.getUserPacketMap().get(account);
		if (null == room || null == up)
			return false;
		if (PDKConstant.CARD_TYPE == cardType) {
			return true;// 不出 不用验证
		}

		List<PDKPacker> myCard = up.getMyCard();
		if (null == myCard || myCard.size() == 0 || null == cardList || cardList.size() < 1) {
			return false;// 牌数验证
		}
		// 牌值验证
		List<PDKPacker> card = new ArrayList<>(myCard);
		card.removeAll(cardList);
		if (card.size() != (myCard.size() - cardList.size())) {
			return false;
		}

		String lastOperateAccount = room.getLastOperateAccount();// 上一个出牌玩家
		// 排序
		cardList = getCardSortNumList(cardList, cardType);
		// 最后一手牌验证
		boolean isLastCard = isLastCard(cardList.size(), myCard.size(), cardType, room.getThreeType(),
				room.getFourType());
		// 牌类型验证
		if (!verifyCardType(cardList, cardType, room.getThreeType(), room.getFourType(), isLastCard)) {
			return false;
		}
		if (lastOperateAccount.equals(account))
			return true;// 同个人可以不用验证 上一次出牌

		// 牌的合理性验证
		List<PDKPacker> last = getCardSortNumList(new ArrayList<>(room.getLastCard()), room.getLastCardType());// 上一次出牌
		int lastType = room.getLastCardType();// 上一次出牌 类型

		if (PDKConstant.CARD_TYPE_BOMB != lastType) {// 不是炸弹
			if (PDKConstant.CARD_TYPE_BOMB == cardType)
				return true;// 出的是炸弹
			if (cardType != lastType)
				return false;// 类型要相同
		} else {// 炸弹
			if (PDKConstant.CARD_TYPE_BOMB != cardType)
				return false;// 出的不是炸弹
		}

		// 出牌和上一次出牌数量验证 start
		// 单牌 对牌 三条 单顺 双顺 三顺 || PDKConstant.CARD_TYPE_HREE == cardType ||
		// PDKConstant.CARD_TYPE_THREE_STRAIGHT == cardType
		if (PDKConstant.CARD_TYPE_SINGLE == cardType || PDKConstant.CARD_TYPE_PAIR == cardType
				|| PDKConstant.CARD_TYPE_ONE_STRAIGHT == cardType || PDKConstant.CARD_TYPE_TWO_STRAIGHT == cardType) {
			if (last.size() != cardList.size())
				return false;
		}

		if (PDKConstant.CARD_TYPE_THREE_WITH == cardType) {// 三带几 3-5
			if (isLastCard) {// 最后一手牌
				if (3 > cardList.size() || 5 < cardList.size())
					return false;
			} else {
				if (last.size() != cardList.size())
					return false;
			}
		}
		if (PDKConstant.CARD_TYPE_AIRCRAFT == lastType) {// 飞机 张数 10 15 两种可能
			if (isLastCard) {// 最后一手牌
				if (10 == last.size()) {
					if (6 > cardList.size() || cardList.size() > 10) {// 范围6-10
						return false;
					}
				} else if (15 == last.size()) {
					if (9 > cardList.size() || cardList.size() > 15) {// 范围9-15
						return false;
					}
					// 需要考虑的是9 张和 10
					if (9 == cardList.size() || 10 == cardList.size()) {
						if (!cardList.get(0).getNum().equals(cardList.get(1).getNum())
								|| !cardList.get(0).getNum().equals(cardList.get(2).getNum())
								|| !cardList.get(3).getNum().equals(cardList.get(4).getNum())
								|| !cardList.get(3).getNum().equals(cardList.get(5).getNum())
								|| !cardList.get(6).getNum().equals(cardList.get(7).getNum())
								|| !cardList.get(6).getNum().equals(cardList.get(8).getNum())) {
							return false;
						}
					}
				}
			} else {
				if (last.size() != cardList.size())
					return false;
			}
		}

		if (PDKConstant.CARD_TYPE_FOUR_WITH == cardType) {// 四带几 5-7
			if (!isLastCard) {// 非最后一手牌
				if (5 > cardList.size() || 7 < cardList.size())
					return false;
			} else {
				if (last.size() != cardList.size())
					return false;
			}
		}
		if (PDKConstant.CARD_TYPE_BOMB == cardType) {// 炸弹
			if (3 == cardList.size()) {// 3A
				for (PDKPacker packer : cardList) {
					if (!PDKNum.P_A.equals(packer.getNum())) {
						return false;
					}
				}
			} else {
				if (last.size() != cardList.size())
					return false;
			}
		}
		// 出牌和上一次出牌数量验证 end

		if (last.size() > 0 && last.get(0).getNum().getNum() >= cardList.get(0).getNum().getNum()) {
			return false;
		}

		return true;
	}

	@Override
	public boolean verifyCardType(List<PDKPacker> cardList, int cardType, int threeType, int fourType,
			boolean isLastCard) {
		switch (cardType) {
		case PDKConstant.CARD_TYPE_SINGLE:// 单牌
			if (1 == cardList.size())
				return true;
			return false;
		case PDKConstant.CARD_TYPE_PAIR:// 对子
			if (2 == cardList.size())
				if (cardList.get(0).getNum().equals(cardList.get(1).getNum()))
					return true;
			return false;
//            case PDKConstant.CARD_TYPE_HREE://3条不带
//                if (3 == cardList.size())
//                    if (cardList.get(0).getNum().equals(cardList.get(1).getNum())
//                            && cardList.get(0).getNum().equals(cardList.get(2).getNum()))
//                        return true;
//                return false;
		case PDKConstant.CARD_TYPE_THREE_WITH:// 3条带几
			if (!isLastCard) {
				if (PDKConstant.THREE_TYPE_12 == threeType) {// 1/2张
					if (4 != cardList.size() && 5 != cardList.size()) {
						return false;
					}
				} else if (PDKConstant.THREE_TYPE_2 == threeType) {// 2张
					if (5 != cardList.size()) {
						return false;
					}
				} else if (PDKConstant.THREE_TYPE_1 == threeType) {// 1张
					if (4 != cardList.size()) {
						return false;
					}
				}
			}
			if (cardList.get(0).getNum().equals(cardList.get(1).getNum())
					&& cardList.get(0).getNum().equals(cardList.get(2).getNum()))
				return true;
			return false;
		case PDKConstant.CARD_TYPE_BOMB:// 炸弹
			if (3 == cardList.size()// 3A
					&& PDKNum.P_A.equals(cardList.get(0).getNum()) && PDKNum.P_A.equals(cardList.get(1).getNum())
					&& PDKNum.P_A.equals(cardList.get(2).getNum())) {
				return true;
			}
			if (4 == cardList.size() && cardList.get(0).getNum().equals(cardList.get(1).getNum())
					&& cardList.get(0).getNum().equals(cardList.get(2).getNum())
					&& cardList.get(0).getNum().equals(cardList.get(3).getNum())) {
				return true;
			}
			return false;
		case PDKConstant.CARD_TYPE_FOUR_WITH:// 四带几
			// 3A只能当炸弹
//                if (PDKNum.P_A.equals(cardList.get(0).getNum())
//                        && PDKNum.P_A.equals(cardList.get(1).getNum())
//                        && PDKNum.P_A.equals(cardList.get(2).getNum())) {
//                    if (!isLastCard) {
//                        if (PDKConstant.FOUR_TYPE_123 == fourType) {//1/2/3张
//                            if (4 == cardList.size() || 5 == cardList.size() || 6 == cardList.size()) {
//                                return true;
//                            }
//                        } else if (PDKConstant.FOUR_TYPE_12 == fourType) {//1/2张
//                            if (4 == cardList.size() || 5 == cardList.size()) {
//                                return true;
//                            }
//                        } else if (PDKConstant.FOUR_TYPE_1 == fourType) {//1张
//                            if (4 == cardList.size()) {
//                                return true;
//                            }
//                        } else if (PDKConstant.FOUR_TYPE_2 == fourType) {//2张
//                            if (5 == cardList.size()) {
//                                return true;
//                            }
//                        } else if (PDKConstant.FOUR_TYPE_3 == fourType) {//3张
//                            if (6 == cardList.size()) {
//                                return true;
//                            }
//                        }
//                    } else if (cardList.get(0).getNum().equals(cardList.get(1).getNum())
//                            && cardList.get(0).getNum().equals(cardList.get(2).getNum()))
//                        return true;
//
//                }

			if (cardList.get(0).getNum().equals(cardList.get(1).getNum())
					&& cardList.get(0).getNum().equals(cardList.get(2).getNum())
					&& cardList.get(0).getNum().equals(cardList.get(3).getNum())) {
				if (!isLastCard) {
					if (PDKConstant.FOUR_TYPE_123 == fourType) {// 1/2/3张
						if (5 == cardList.size() || 6 == cardList.size() || 7 == cardList.size()) {
							return true;
						}
					} else if (PDKConstant.FOUR_TYPE_12 == fourType) {// 1/2张
						if (5 == cardList.size() || 6 == cardList.size()) {
							return true;
						}
					} else if (PDKConstant.FOUR_TYPE_1 == fourType) {// 1张
						if (5 == cardList.size()) {
							return true;
						}
					} else if (PDKConstant.FOUR_TYPE_2 == fourType) {// 2张
						if (6 == cardList.size()) {
							return true;
						}
					} else if (PDKConstant.FOUR_TYPE_3 == fourType) {// 3张
						if (7 == cardList.size()) {
							return true;
						}
					}
				} else if (cardList.get(0).getNum().equals(cardList.get(1).getNum())
						&& cardList.get(0).getNum().equals(cardList.get(2).getNum())
						&& cardList.get(0).getNum().equals(cardList.get(3).getNum()))
					return true;
			}
			return false;
		case PDKConstant.CARD_TYPE_ONE_STRAIGHT:// 单顺
			if (4 < cardList.size()) {
				if (PDKNum.P_2.equals(cardList.get(0).getNum())) {// 不能到2
					return false;
				}
				boolean b = true;
				for (int u = cardList.size() - 1; u >= 0; u--) {
					if (u - 1 < 0) {
						continue;
					}
					if (cardList.get(u).getNum().getNum() != cardList.get(u - 1).getNum().getNum() - 1) {
						b = false;
						break;
					}
				}
				if (b)
					return true;

			}
			return false;
		case PDKConstant.CARD_TYPE_TWO_STRAIGHT:// 双顺
			if (0 == cardList.size() % 2 && 4 <= cardList.size()) {
				boolean b = true;
				for (int u = cardList.size() - 1; u >= 0; u--) {
					if (u - 1 < 0) {
						continue;
					}
					if (0 == u % 2) {// 隔1张的牌
						if (cardList.get(u).getNum().getNum() != cardList.get(u - 1).getNum().getNum() - 1) {
							b = false;
							break;
						}
					} else {// 相邻的牌
						if (!cardList.get(u).getNum().equals(cardList.get(u - 1).getNum())) {
							b = false;
							break;
						}
					}
				}
				if (b)
					return true;
			}
			return false;
//            case PDKConstant.CARD_TYPE_THREE_STRAIGHT://三顺
//                if (0 == cardList.size() % 3 && 6 <= cardList.size()) {
//                    boolean b = true;
//                    for (int u = cardList.size() - 1; u >= 0; u--) {
//                        if (u - 1 < 0) {
//                            continue;
//                        }
//                        if (0 == u % 3) {//隔2张的牌
//                            if (cardList.get(u).getNum().getNum() != cardList.get(u - 1).getNum().getNum() - 1) {
//                                b = false;
//                                break;
//                            }
//                        } else {//相邻的牌
//                            if (!cardList.get(u).getNum().equals(cardList.get(u - 1).getNum())) {
//                                b = false;
//                                break;
//                            }
//                        }
//                    }
//                    if (b) return true;
//                }
//                return false;
		case PDKConstant.CARD_TYPE_AIRCRAFT:// 飞机|2个或3个连续的三带二。如888999＋3457、888999+3355、888999+3367
			if (!isLastCard) {
				if (0 != cardList.size() % 5) {
					return false;
				}
			}
			if (6 > cardList.size() || 16 < cardList.size()) {
				return false;
			}

			if (11 > cardList.size()) {
				if (!cardList.get(0).getNum().equals(cardList.get(1).getNum())
						|| !cardList.get(0).getNum().equals(cardList.get(2).getNum())
						|| cardList.get(0).getNum().getNum() != cardList.get(3).getNum().getNum() + 1
						|| !cardList.get(3).getNum().equals(cardList.get(4).getNum())
						|| !cardList.get(3).getNum().equals(cardList.get(5).getNum())) {
					return false;
				} else {
					return true;
				}
			}
			if (16 > cardList.size()) {
				if (!cardList.get(0).getNum().equals(cardList.get(1).getNum())
						|| !cardList.get(0).getNum().equals(cardList.get(2).getNum())
						|| cardList.get(0).getNum().getNum() != cardList.get(3).getNum().getNum() + 1
						|| !cardList.get(3).getNum().equals(cardList.get(4).getNum())
						|| !cardList.get(3).getNum().equals(cardList.get(5).getNum())
						|| cardList.get(3).getNum().getNum() != cardList.get(6).getNum().getNum() + 1
						|| !cardList.get(6).getNum().equals(cardList.get(7).getNum())
						|| !cardList.get(6).getNum().equals(cardList.get(8).getNum())) {
					return false;
				} else {
					return true;
				}
			}
			if (16 == cardList.size()) {
				if (!cardList.get(0).getNum().equals(cardList.get(1).getNum())
						|| !cardList.get(0).getNum().equals(cardList.get(2).getNum())
						|| cardList.get(0).getNum().getNum() != cardList.get(3).getNum().getNum() + 1
						|| !cardList.get(3).getNum().equals(cardList.get(4).getNum())
						|| !cardList.get(3).getNum().equals(cardList.get(5).getNum())
						|| cardList.get(3).getNum().getNum() != cardList.get(6).getNum().getNum() + 1
						|| !cardList.get(6).getNum().equals(cardList.get(7).getNum())
						|| !cardList.get(6).getNum().equals(cardList.get(8).getNum())
						|| cardList.get(6).getNum().getNum() != cardList.get(9).getNum().getNum() + 1
						|| !cardList.get(9).getNum().equals(cardList.get(10).getNum())
						|| !cardList.get(9).getNum().equals(cardList.get(11).getNum())) {
					return false;
				} else {
					return true;
				}
			}
			return false;
		default:
			return false;
		}

	}

	@Override
	public String nextPlayerAccount(String roomNo, String account) {
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		Map<Long, String> map = new HashMap<>();
		for (String s : room.getPlayerMap().keySet()) {
			map.put(room.getPlayerMap().get(s).getId(), s);
		}

		List<Long> userIdList = new ArrayList<>();
		for (int i = 0; i < room.getUserIdList().size(); i++) {
			if (room.getUserIdList().get(i) <= 0L)
				continue;
			PDKUserPacket up = room.getUserPacketMap().get(map.get(room.getUserIdList().get(i)));
			if (null == up || PDKConstant.USER_STATUS_INIT == up.getStatus()
					|| PDKConstant.USER_STATUS_READY == up.getStatus())
				continue;
			userIdList.add(room.getUserIdList().get(i));
		}

		for (int i = 0; i < userIdList.size(); i++) {
			if (userIdList.get(i) == room.getPlayerMap().get(account).getId()) {
				if (i + 1 == userIdList.size())
					return map.get(userIdList.get(0));
				else
					return map.get(userIdList.get(i + 1));
			}
		}
		return "-1";
	}

	@Override
	public boolean isLastCard(int cardCount, int myCardCount, int cardType, int threeType, int fourType) {
		if (cardCount != myCardCount) {
			return false;
		}
		if (PDKConstant.CARD_TYPE_THREE_WITH == cardType) {// 三带几
			if (PDKConstant.THREE_TYPE_1 == threeType) {// 1张
				if (3 == myCardCount) {
					return true;
				}
			} else {
				if (3 == myCardCount || 4 == myCardCount) {
					return true;
				}
			}
		} else if (PDKConstant.CARD_TYPE_FOUR_WITH == cardType) {// 四带几 3A
			if (PDKConstant.FOUR_TYPE_1 == fourType) {// 1张
				if (5 == myCardCount) {
					return true;
				}
			} else {
				if (4 == myCardCount || 5 == myCardCount || 6 == myCardCount) {
					return true;
				}
			}
		} else if (PDKConstant.CARD_TYPE_AIRCRAFT == cardType) {// 飞机
			if (6 <= myCardCount && 16 >= myCardCount) {
				return true;
			}
		}

		return false;
	}

	@Override
	public List<Map<Integer, List<PDKPacker>>> getCradGroupt(String roomNo) {
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room) {
			return null;
		}
		String focusAccount = room.getFocusAccount();// 当前操作玩家
		// 获取当前操作玩家的牌
		List<PDKPacker> focusCardList = new ArrayList<>(room.getUserPacketMap().get(focusAccount).getMyCard());
//        log.info("新跑得快当前操作玩家{}的牌型{}", focusAccount, focusCardList);

		List<Map<Integer, List<PDKPacker>>> list = new ArrayList<>();

		// 4张相同的牌(3A)
		Map<Integer, List<PDKPacker>> fourMap = getSameValList(focusCardList, 4);
//        log.info("新跑得快的炸弹组合{}", fourMap);
		// 3张相同的牌
		Map<Integer, List<PDKPacker>> threeMap = getSameValList(focusCardList, 3);
//        log.info("新跑得快的三条组合{}", threeMap);
		// 2张相同的牌
		Map<Integer, List<PDKPacker>> twoMap = getSameValList(focusCardList, 2);
//        log.info("新跑得快的对子组合{}", twoMap);
		// 顺子 3-14
		List<List<PDKPacker>> straightList = new ArrayList<>();
		for (int i = 12; i > 4; i--) {
			straightList.addAll(getStraightList(focusCardList, i));
		}
//        log.info("新跑得快的顺子组合{}", straightList);

		String lastOperateAccount = room.getLastOperateAccount();// 上一个出牌玩家
		int lastCardType = room.getLastCardType();// 上一次出牌 类型
		int lastCardCount = room.getLastCard().size();// 上次出牌牌数
		int focusCardCount = focusCardList.size();// 玩家手上的牌数

		PDKPacker isPacker = null;// 是否包含那张牌
		Integer cardVal = null;// 大于牌值
		boolean isHeiTao3 = verifyHeiTao3(room.getCardProcessNum(), room.getPayRules(), room.getGameIndex());// 是否必带黑桃3
		if (0 == room.getCardProcessNum() && isHeiTao3) {// 必须是第一首出牌
			isPacker = new PDKPacker(PDKColor.HEITAO, PDKNum.P_3);
			lastCardType = PDKConstant.CARD_TYPE;
		} else {
			if (!focusAccount.equals(lastOperateAccount)) {// 如果不是同一个人 必出
				List<PDKPacker> lastCard = room.getLastCard();// 上一次出牌
				if (null != lastCard && lastCard.size() > 0) {
					cardVal = lastCard.get(0).getNum().getNum();
				}
			} else {
				lastCardType = PDKConstant.CARD_TYPE;
			}
		}

		if (PDKConstant.CARD_TYPE == lastCardType || PDKConstant.CARD_TYPE_AIRCRAFT == lastCardType) {// 未出牌或飞机
			if (null != threeMap && threeMap.size() > 1) {// 飞机
				for (Integer key : threeMap.keySet()) {
					if (null != threeMap.get(key + 1)) {// 有相邻的
						Map<Integer, List<PDKPacker>> mP = new HashMap<>();
						List<PDKPacker> lP = new ArrayList<>();
						// 移除必出的牌 重新获取 牌的优先级 start
						List<PDKPacker> newList = new ArrayList<>(focusCardList);
						newList.removeAll(threeMap.get(key));
						newList.removeAll(threeMap.get(key + 1));
						List<PDKPacker> aP = new ArrayList<>(getSingleList(newList));
						// 移除必出的牌 重新获取 牌的优先级 start
						lP.addAll(threeMap.get(key));
						lP.addAll(threeMap.get(key + 1));
						aP.removeAll(threeMap.get(key));
						aP.removeAll(threeMap.get(key + 1));
						for (int c = 0; c < 4; c++) {// 4张
							if (aP.size() > 0) {
								lP.add(aP.get(0));
								aP.remove(aP.get(0));
							}
						}
						// 包含牌型验证 start
						if (null != isPacker && !lP.contains(isPacker)) {
							continue;
						}
						// 包含牌型验证 end
						// 大于牌值验证 start
						if (null != cardVal && cardVal >= lP.get(0).getNum().getNum()) {
							continue;
						}
						// 大于牌值验证 end
						mP.put(PDKConstant.CARD_TYPE_AIRCRAFT, lP);
						if (PDKConstant.CARD_TYPE_AIRCRAFT == lastCardType && focusCardCount >= lastCardCount
								&& lP.size() != lastCardCount) {
							mP = null;
						}

						if (null != mP)
							list.add(mP);

						if (null != threeMap.get(key + 2)) {// 3相邻
							Map<Integer, List<PDKPacker>> mP2 = new HashMap<>();
							List<PDKPacker> lP2 = new ArrayList<>();
							// 移除必出的牌 重新获取 牌的优先级 start
							List<PDKPacker> newList2 = new ArrayList<>(focusCardList);
							newList.removeAll(threeMap.get(key));
							newList.removeAll(threeMap.get(key + 1));
							newList.removeAll(threeMap.get(key + 2));
							List<PDKPacker> aP2 = new ArrayList<>(getSingleList(newList2));
							// 移除必出的牌 重新获取 牌的优先级 end

							lP2.addAll(threeMap.get(key));
							lP2.addAll(threeMap.get(key + 1));
							lP2.addAll(threeMap.get(key + 2));
							aP2.removeAll(threeMap.get(key));
							aP2.removeAll(threeMap.get(key + 1));
							aP2.removeAll(threeMap.get(key + 2));
							for (int c = 0; c < 6; c++) {// 4张
								if (aP2.size() > 0) {
									lP2.add(aP2.get(0));
									aP2.remove(aP2.get(0));
								}
							}
							// 包含牌型验证 start
							if (null != isPacker && !lP2.contains(isPacker)) {
								continue;
							}
							// 包含牌型验证 end
							// 大于牌值验证 start
							if (null != cardVal && cardVal >= lP2.get(0).getNum().getNum()) {
								continue;
							}
							// 大于牌值验证 end
							mP2.put(PDKConstant.CARD_TYPE_AIRCRAFT, lP2);

							if (PDKConstant.CARD_TYPE_AIRCRAFT == lastCardType && focusCardCount >= lastCardCount
									&& lP2.size() != lastCardCount) {
								mP2 = null;
							}
							if (null != mP2)
								list.add(mP2);

							if (null != threeMap.get(key + 3)) {// 4相邻
								Map<Integer, List<PDKPacker>> mP3 = new HashMap<>();
								List<PDKPacker> aP3 = new ArrayList<>(focusCardList);
								// 包含牌型验证 start
								if (null != isPacker && !aP3.contains(isPacker)) {
									continue;
								}
								// 包含牌型验证 end
								// 大于牌值验证 start
								if (null != cardVal && cardVal >= aP3.get(0).getNum().getNum()) {
									continue;
								}
								// 大于牌值验证 end
								mP3.put(PDKConstant.CARD_TYPE_AIRCRAFT, aP3);

								if (PDKConstant.CARD_TYPE_AIRCRAFT == lastCardType && focusCardCount >= lastCardCount) {
									mP3 = null;
								}
								if (null != mP3)
									list.add(mP3);

							}
						}
					}
				}
			}
		}

		if (PDKConstant.CARD_TYPE == lastCardType || PDKConstant.CARD_TYPE_ONE_STRAIGHT == lastCardType) {// 未出牌或顺子

			for (List<PDKPacker> straight : straightList) {
				Map<Integer, List<PDKPacker>> mP = new HashMap<>();
				// 包含牌型验证 start
				if (null != isPacker && !straight.contains(isPacker)) {
					continue;
				}
				// 包含牌型验证 end
				// 大于牌值验证 start
				if (null != cardVal && cardVal >= straight.get(0).getNum().getNum()) {
					continue;
				}
				// 大于牌值验证 end
				mP.put(PDKConstant.CARD_TYPE_ONE_STRAIGHT, straight);

				if (PDKConstant.CARD_TYPE_ONE_STRAIGHT == lastCardType && straight.size() != lastCardCount) {
					mP = null;
				}
				if (null != mP)
					list.add(mP);

			}
		}

		if (PDKConstant.CARD_TYPE == lastCardType || PDKConstant.CARD_TYPE_FOUR_WITH == lastCardType) {// 未出牌或四带几
			if (null != fourMap) {
				for (Integer key : fourMap.keySet()) {
					Map<Integer, List<PDKPacker>> mP = new HashMap<>();
					List<PDKPacker> lP = new ArrayList<>();
					// 移除必出的牌 重新获取 牌的优先级 start
					List<PDKPacker> newList = new ArrayList<>(focusCardList);
					newList.removeAll(fourMap.get(key));
					List<PDKPacker> aP = new ArrayList<>(getSingleList(newList));
					// 移除必出的牌 重新获取 牌的优先级 end
					lP.addAll(fourMap.get(key));
					// 3A只能当炸弹
					if (PDKNum.P_A.equals(lP.get(0).getNum())) {
						continue;
					}
					aP.removeAll(fourMap.get(key));
					int count = 3;
					if (PDKConstant.FOUR_TYPE_1 == room.getFourType()) {
						count = 1;
					} else if (PDKConstant.FOUR_TYPE_2 == room.getFourType()) {
						count = 2;
					}
					if (aP.size() == 0) {// 没有牌带 直接炸弹
						// 包含牌型验证 start
						if (null != isPacker && !lP.contains(isPacker)) {
							continue;
						}
						// 包含牌型验证 end
						// 大于牌值验证 start
						if (null != cardVal && cardVal >= lP.get(0).getNum().getNum()) {
							continue;
						}
						// 大于牌值验证 end
						mP.put(PDKConstant.CARD_TYPE_BOMB, lP);
						list.add(mP);
					}
					for (int c = 0; c < count; c++) {
						if (aP.size() > 0) {
							lP.add(aP.get(0));
							aP.remove(aP.get(0));
						}
					}
					// 包含牌型验证 start
					if (null != isPacker && !lP.contains(isPacker)) {
						continue;
					}
					// 包含牌型验证 end
					// 大于牌值验证 start
					if (null != cardVal && cardVal >= lP.get(0).getNum().getNum()) {
						continue;
					}
					// 大于牌值验证 end
					mP.put(PDKConstant.CARD_TYPE_FOUR_WITH, lP);

					if (PDKConstant.CARD_TYPE_FOUR_WITH == lastCardType && focusCardCount >= lastCardCount
							&& lP.size() != lastCardCount) {
						if (PDKNum.P_A.equals(lP.get(0).getNum())) {// 3A 少一张
							if (lP.size() + 1 != lastCardCount) {
								mP = null;
							}
						} else {
							if (lP.size() != lastCardCount) {
								mP = null;
							}
						}
					}
					if (null != mP)
						list.add(mP);

				}
			}
		}

		if (PDKConstant.CARD_TYPE == lastCardType || PDKConstant.CARD_TYPE_THREE_WITH == lastCardType) {// 未出牌或三带几
			if (null != threeMap) {
				for (Integer key : threeMap.keySet()) {
					Map<Integer, List<PDKPacker>> mP = new HashMap<>();
					List<PDKPacker> lP = new ArrayList<>();
					// 移除必出的牌 重新获取 牌的优先级 start
					List<PDKPacker> newList = new ArrayList<>(focusCardList);
					newList.removeAll(threeMap.get(key));
					List<PDKPacker> aP = new ArrayList<>(getSingleList(newList));
					// 移除必出的牌 重新获取 牌的优先级 end
					lP.addAll(threeMap.get(key));
					aP.removeAll(threeMap.get(key));
					int count = 2;
					if (PDKConstant.THREE_TYPE_1 == room.getThreeType()) {
						count = 1;
					}
					for (int c = 0; c < count; c++) {
						if (aP.size() > 0) {
							lP.add(aP.get(0));
							aP.remove(aP.get(0));
						}
					}
					// 包含牌型验证 start
					if (null != isPacker && !lP.contains(isPacker)) {
						continue;
					}
					// 包含牌型验证 end
					// 大于牌值验证 start
					if (null != cardVal && cardVal >= lP.get(0).getNum().getNum()) {
						continue;
					}
					// 大于牌值验证 end
					mP.put(PDKConstant.CARD_TYPE_THREE_WITH, lP);

					if (PDKConstant.CARD_TYPE_THREE_WITH == lastCardType && focusCardCount >= lastCardCount
							&& lP.size() != lastCardCount) {
						mP = null;
					}
					if (null != mP)
						list.add(mP);

				}
			}
		}

		if (PDKConstant.CARD_TYPE == lastCardType || PDKConstant.CARD_TYPE_TWO_STRAIGHT == lastCardType) {// 未出牌或双顺

			if (null != twoMap) {
				int isCount = 0;
				if (PDKConstant.CARD_TYPE_TWO_STRAIGHT == lastCardType) {
					List<PDKPacker> lastCard = room.getLastCard();// 上一次出牌
					if (null != lastCard && lastCard.size() > 0) {
						isCount = lastCard.size() / 2;
					}
				}

				for (Integer key : twoMap.keySet()) {
					for (int f = 8; f > 1; f--) {
						if (isCount > 0 && f != isCount)
							continue;

						boolean b = true;
						int val = 0;
						for (int t = 1; t < f; t++) {
							if (null == twoMap.get(key + t)) {// 有相邻的
								b = false;
								break;
							}
							val++;
						}
						if (b) {
							Map<Integer, List<PDKPacker>> mP = new HashMap<>();
							List<PDKPacker> lP = new ArrayList<>();
							for (int t = val; t >= 0; t--) {
								lP.addAll(twoMap.get(key + t));
							}
							// 包含牌型验证 start
							if (null != isPacker && !lP.contains(isPacker)) {
								continue;
							}
							// 包含牌型验证 end

							// 大于牌值验证 start
							if (null != cardVal && cardVal >= lP.get(0).getNum().getNum()) {
								continue;
							}
							// 大于牌值验证 end
							mP.put(PDKConstant.CARD_TYPE_TWO_STRAIGHT, lP);

							if (PDKConstant.CARD_TYPE_TWO_STRAIGHT == lastCardType && lP.size() != lastCardCount) {
								mP = null;
							}
							if (null != mP)
								list.add(mP);
						}

					}

				}
			}
		}

//        if (PDKConstant.CARD_TYPE == lastCardType || PDKConstant.CARD_TYPE_THREE_STRAIGHT == lastCardType) {//未出牌或三顺
//            if (null != threeMap) {
//                int isCount = 0;
//                if (PDKConstant.CARD_TYPE_THREE_STRAIGHT == lastCardType) {
//                    List<PDKPacker> lastCard = room.getLastCard();//上一次出牌
//                    if (null != lastCard && lastCard.size() > 0) {
//                        isCount = lastCard.size() / 3;
//                    }
//                }
//
//                for (Integer key : threeMap.keySet()) {
//                    for (int f = 4; f > 1; f--) {
//                        if (isCount > 0 && f != isCount) continue;
//
//                        boolean b = true;
//                        int val = 0;
//                        for (int t = 1; t < f; t++) {
//                            if (null == threeMap.get(key + t)) {//有相邻的
//                                b = false;
//                                break;
//                            }
//                            val++;
//                        }
//                        if (b) {
//                            Map<Integer, List<PDKPacker>> mP = new HashMap<>();
//                            List<PDKPacker> lP = new ArrayList<>();
//                            for (int t = val; t >= 0; t--) {
//                                lP.addAll(threeMap.get(key + t));
//                            }
//                            // 包含牌型验证 start
//                            if (null != isPacker && !lP.contains(isPacker)) {
//                                continue;
//                            }
//                            // 包含牌型验证 end
//                            // 大于牌值验证 start
//                            if (null != cardVal && cardVal >= lP.get(0).getNum().getNum()) {
//                                continue;
//                            }
//                            // 大于牌值验证 end
//                            mP.put(PDKConstant.CARD_TYPE_THREE_STRAIGHT, lP);
//
//                            if (PDKConstant.CARD_TYPE_THREE_STRAIGHT == lastCardType && lP.size() != lastCardCount) {
//                                mP = null;
//                            }
//                            if (null != mP) list.add(mP);
//                        }
//                    }
//                }
//            }
//
//        }

		if (PDKConstant.CARD_TYPE == lastCardType || PDKConstant.CARD_TYPE_PAIR == lastCardType) {// 未出牌或对牌
			if (null != twoMap) {
				for (Integer key : twoMap.keySet()) {
					Map<Integer, List<PDKPacker>> mP = new HashMap<>();
					List<PDKPacker> lP = new ArrayList<>();
					lP.addAll(twoMap.get(key));
					// 包含牌型验证 start
					if (null != isPacker && !lP.contains(isPacker)) {
						continue;
					}
					// 包含牌型验证 end
					// 大于牌值验证 start
					if (null != cardVal && cardVal >= lP.get(0).getNum().getNum()) {
						continue;
					}
					// 大于牌值验证 end
					mP.put(PDKConstant.CARD_TYPE_PAIR, lP);
					list.add(mP);
				}
			}
		}

//        if (PDKConstant.CARD_TYPE == lastCardType || PDKConstant.CARD_TYPE_HREE == lastCardType) {//未出牌或单牌
//            if (null != threeMap) {
//                for (Integer key : threeMap.keySet()) {
//                    Map<Integer, List<PDKPacker>> mP = new HashMap<>();
//                    List<PDKPacker> lP = new ArrayList<>();
//                    lP.addAll(threeMap.get(key));
//                    // 包含牌型验证 start
//                    if (null != isPacker && !lP.contains(isPacker)) {
//                        continue;
//                    }
//                    // 包含牌型验证 end
//                    // 大于牌值验证 start
//                    if (null != cardVal && cardVal >= lP.get(0).getNum().getNum()) {
//                        continue;
//                    }
//                    // 大于牌值验证 end
//                    mP.put(PDKConstant.CARD_TYPE_HREE, lP);
//                    list.add(mP);
//                }
//            }
//        }

		if (PDKConstant.CARD_TYPE == lastCardType || PDKConstant.CARD_TYPE_SINGLE == lastCardType) {// 未出牌或单牌
			List<PDKPacker> oneList = getSingleList(focusCardList);
//            log.info("新跑得快的单牌排序{}", oneList);
			if (0 == room.getCardProcessNum() && isHeiTao3) {// 黑桃3先出
				oneList = new ArrayList<>();
				oneList.add(new PDKPacker(PDKColor.HEITAO, PDKNum.P_3));
			}

			// 必压
			String nextPlayerAccount = nextPlayerAccount(roomNo, focusAccount);// 下家
			if (room.getUserPacketMap().get(nextPlayerAccount).getMyCard().size() == 1) {// 下家只剩一张牌
				oneList = new ArrayList<>();
				oneList.add(focusCardList.get(0));// 取第一张
				if (focusCardList.size() > 1 && focusCardList.get(0).getNum().equals(focusCardList.get(1).getNum())) {
					oneList.add(focusCardList.get(1));// 取第二张
					if (focusCardList.size() > 2
							&& focusCardList.get(1).getNum().equals(focusCardList.get(2).getNum())) {
						oneList.add(focusCardList.get(2));// 取第三张
						if (focusCardList.size() > 3
								&& focusCardList.get(2).getNum().equals(focusCardList.get(3).getNum())) {
							oneList.add(focusCardList.get(3));// 取第四张
						}
					}
				}

			}

			for (PDKPacker packer : oneList) {
				Map<Integer, List<PDKPacker>> mP = new HashMap<>();
				List<PDKPacker> lP = new ArrayList<>();
				lP.add(packer);
				// 包含牌型验证 start
				if (null != isPacker && !lP.contains(isPacker)) {
					continue;
				}
				// 包含牌型验证 end
				// 大于牌值验证 start
				if (null != cardVal && cardVal >= lP.get(0).getNum().getNum()) {
					continue;
				}
				// 大于牌值验证 end
				mP.put(PDKConstant.CARD_TYPE_SINGLE, lP);
				list.add(mP);
			}
		}

		if (null != fourMap) {
			for (Integer key : fourMap.keySet()) {
				Map<Integer, List<PDKPacker>> mP = new HashMap<>();
				List<PDKPacker> lP = new ArrayList<>();
				lP.addAll(fourMap.get(key));
				// 包含牌型验证 start
				if (null != isPacker && !lP.contains(isPacker)) {
					continue;
				}
				// 包含牌型验证 end
				// 大于牌值验证 start
				if (PDKConstant.CARD_TYPE_BOMB == lastCardType && null != cardVal
						&& cardVal >= lP.get(0).getNum().getNum()) {
					continue;
				}
				// 大于牌值验证 end
				mP.put(PDKConstant.CARD_TYPE_BOMB, lP);
				list.add(mP);
			}
		}

//        log.info("新跑得快当前操作玩家可以出的组合{}", list);
		return list;
	}

	@Override
	public Map<Integer, List<PDKPacker>> getSameValList(List<PDKPacker> cardList, int number) {
		if (null == cardList || 1 >= number || cardList.size() < number) {
			return null;
		}
		Map<Integer, List<PDKPacker>> cardMap = null;// 满足条件组合
		Map<Integer, List<PDKPacker>> map = new HashMap<>();// 所有牌组合
		for (PDKPacker packer : cardList) {

			List<PDKPacker> card = new ArrayList<>();
			int val = packer.getNum().getNum();
			if (null != map.get(val)) {
				card = map.get(val);
			}
			card.add(packer);
			map.put(val, card);
		}
		for (Integer i : map.keySet()) {
			if (number <= map.get(i).size()) {
				List<PDKPacker> card = new ArrayList<>();
				for (int j = 0; j < number; j++) {
					card.add(map.get(i).get(j));
				}
				if (null == cardMap)
					cardMap = new HashMap<>();
				cardMap.put(i, card);
			}
			if (4 == number && PDKNum.P_A.getNum() == i && map.get(i).size() == 3) {// 3A也是炸弹
				if (null == cardMap)
					cardMap = new HashMap<>();
				cardMap.put(i, map.get(i));
			}
		}

		return cardMap;
	}

	@Override
	public List<List<PDKPacker>> getStraightList(List<PDKPacker> cardList, int number) {
		List<List<PDKPacker>> list = new ArrayList<>();
		if (null == cardList || 5 > number || cardList.size() < number) {
			return list;
		}

		Map<Integer, PDKPacker> map = new HashMap<>();// 所有牌组合
		for (PDKPacker packer : cardList) {
			if (!PDKNum.P_2.equals(packer.getNum())) {// 顺子不能到2
				map.put(packer.getNum().getNum(), packer);
			}
		}
		if (cardList.contains(new PDKPacker(PDKColor.HEITAO, PDKNum.P_3))) {// 如果有黑桃3优先选择
			map.put(PDKNum.P_3.getNum(), new PDKPacker(PDKColor.HEITAO, PDKNum.P_3));
		}

		// 从3开始 3 - 10
		for (int i = 3; i <= 10; i++) {
			List<PDKPacker> packerList = new ArrayList<>();
			boolean b = true;
			for (int j = 0; j < number; j++) {
				if (null == map.get(i + j)) {
					b = false;
					break;
				}
				packerList.add(map.get(i + j));
			}
			if (b) {
				Collections.sort(packerList, PDKPacker.desc);// 排序
				list.add(packerList);
			}
		}
		return list;
	}

	@Override
	public List<PDKPacker> getSingleList(List<PDKPacker> cardList) {
		List<PDKPacker> list = new ArrayList<>();

		List<PDKPacker> bigList = new ArrayList<>(cardList);
		if (null == bigList || 0 == bigList.size()) {
			return list;
		}
		if (1 == bigList.size()) {
			return bigList;
		}
		Collections.sort(bigList, PDKPacker.asc);// 排序

		// 4张相同的牌(3A)
		Map<Integer, List<PDKPacker>> fourMap = getSameValList(cardList, 4);
		// 3张相同的牌
		Map<Integer, List<PDKPacker>> threeMap = getSameValList(cardList, 3);
		// 2张相同的牌
		Map<Integer, List<PDKPacker>> twoMap = getSameValList(cardList, 2);
		// 顺子 3-14
		List<List<PDKPacker>> straightList = new ArrayList<>();
		for (int i = 12; i > 4; i--) {
			straightList.addAll(getStraightList(cardList, i));
		}

		List<Map<String, Integer>> mapList = new ArrayList<>();// 下标 k +优先级 v
		for (int f = 0; f < bigList.size(); f++) {
			int index = 0;

			// 4张相同的牌(3A)
			if (null != fourMap && null != fourMap.get(bigList.get(f).getNum().getNum())) {
				index += 100000;
			}
			// 3张相同的牌
			if (null != threeMap && null != threeMap.get(bigList.get(f).getNum().getNum())) {
				index += 10000;
			}
			// Q 以上也算大牌
			if (PDKNum.P_Q.getNum() < bigList.get(f).getNum().getNum()) {
				index += 1000;
			}

			// 顺子 5条
			if (null != straightList && straightList.size() > 0) {
				for (List<PDKPacker> sList : straightList) {
					if (sList.contains(bigList.get(f))) {
						index += 100;
					}
				}
			}
			// 2张相同的牌 10
			if (null != twoMap && null != twoMap.get(bigList.get(f).getNum().getNum())) {
				index += 10;
			}

			Map<String, Integer> map = new HashMap<>();
			map.put("k", f);
			map.put("v", index);
			mapList.add(map);
		}

		Collections.sort(mapList, PDKPacker.singleDesc);// 排序
		for (int i = 0; i < mapList.size(); i++) {
			list.add(bigList.get(mapList.get(i).get("k")));
		}
		return list;
	}

	@Override
	public int getCardType(List<PDKPacker> cardList) {
		if (null == cardList || 0 == cardList.size()) {// 未出牌
			return PDKConstant.CARD_TYPE;
		}
		if (1 == cardList.size()) {// 单牌
			return PDKConstant.CARD_TYPE_SINGLE;
		}
		if (2 == cardList.size()) {// 对子
			return PDKConstant.CARD_TYPE_PAIR;
		}
		if (3 == cardList.size()) {// 三条
			if (PDKNum.P_A.equals(cardList.get(0).getNum())) {
				return PDKConstant.CARD_TYPE_BOMB; // 3条A
			}
			return PDKConstant.CARD_TYPE_THREE_WITH;
//            return PDKConstant.CARD_TYPE_HREE;
		}
		if (4 == cardList.size()) {// 4条 三带1(最后一手牌) 2对 炸弹
			Set<Integer> set = new HashSet<>();
			for (PDKPacker packer : cardList) {
				set.add(packer.getNum().getNum());
			}
			if (1 == set.size()) {// 炸弹
				return PDKConstant.CARD_TYPE_BOMB;
			}
			if (2 == set.size()) {// 2对
				if (cardList.get(0).getNum().equals(cardList.get(1).getNum())
						&& cardList.get(2).getNum().equals(cardList.get(3).getNum())) {
					return PDKConstant.CARD_TYPE_TWO_STRAIGHT;
				}
			}
			return PDKConstant.CARD_TYPE_THREE_WITH;// 三带1
		}
		if (5 == cardList.size()) {// 单顺 三带2 四带1(最后一手牌)
			Set<Integer> set = new HashSet<>();
			for (PDKPacker packer : cardList) {
				set.add(packer.getNum().getNum());
			}
			if (5 == set.size()) {// 单顺
				return PDKConstant.CARD_TYPE_ONE_STRAIGHT;
			}
			if (2 == set.size() && cardList.get(0).getNum().equals(cardList.get(3).getNum())
					&& !cardList.contains(new PDKPacker(PDKColor.HEITAO, PDKNum.P_3))) {// 四带1(最后一手牌) 排除三带对
				return PDKConstant.CARD_TYPE_FOUR_WITH;
			}
			return PDKConstant.CARD_TYPE_THREE_WITH;
		}
		if (6 == cardList.size()) {// 单顺 双顺 三顺 四带2(最后一手牌)
			Set<Integer> set = new HashSet<>();
			for (PDKPacker packer : cardList) {
				set.add(packer.getNum().getNum());
			}
			if (6 == set.size()) {// 单顺
				return PDKConstant.CARD_TYPE_ONE_STRAIGHT;
			}
			if (2 == set.size() && cardList.get(0).getNum().getNum() == cardList.get(3).getNum().getNum() + 1) {// 三顺
																												// 排除四带对子
//                return PDKConstant.CARD_TYPE_THREE_STRAIGHT;
				return PDKConstant.CARD_TYPE_AIRCRAFT;// 没有三顺 都当成飞机(最后一手牌)
			}

			if (3 == set.size()) {// 双顺
//                3A只能当炸弹
//                if (PDKNum.P_A.equals(cardList.get(0).getNum())
//                        && PDKNum.P_A.equals(cardList.get(1).getNum()) && PDKNum.P_A.equals(cardList.get(2).getNum())) {
//                    return PDKConstant.CARD_TYPE_FOUR_WITH;//3A 一对 1单
//                }
				if (cardList.get(0).getNum().equals(cardList.get(1).getNum())
						&& cardList.get(1).getNum().getNum() - 1 == cardList.get(3).getNum().getNum()
						&& cardList.get(2).getNum().equals(cardList.get(3).getNum())
						&& cardList.get(3).getNum().getNum() - 1 == cardList.get(5).getNum().getNum()
						&& cardList.get(4).getNum().equals(cardList.get(5).getNum())) {
					return PDKConstant.CARD_TYPE_TWO_STRAIGHT;
				}
			}
			return PDKConstant.CARD_TYPE_FOUR_WITH;
		}

		if (7 == cardList.size()) {// 单顺 四带3(最后一手牌) 飞机(最后一手牌)
			Set<Integer> set = new HashSet<>();
			for (PDKPacker packer : cardList) {
				set.add(packer.getNum().getNum());
			}
			if (7 == set.size()) {// 单顺
				return PDKConstant.CARD_TYPE_ONE_STRAIGHT;
			}
			if (cardList.get(0).getNum().equals(cardList.get(1).getNum())
					&& cardList.get(0).getNum().equals(cardList.get(2).getNum())
					&& cardList.get(3).getNum().equals(cardList.get(4).getNum())
					&& cardList.get(3).getNum().equals(cardList.get(5).getNum())) {// 飞机(最后一手牌)
				return PDKConstant.CARD_TYPE_AIRCRAFT;
			}
			return PDKConstant.CARD_TYPE_FOUR_WITH;
		}
		if (8 <= cardList.size()) {// 单顺 飞机(最后一手牌)
			Set<Integer> set = new HashSet<>();
			for (PDKPacker packer : cardList) {
				set.add(packer.getNum().getNum());
			}
			if (cardList.size() == set.size()) {// 单顺
				return PDKConstant.CARD_TYPE_ONE_STRAIGHT;
			}
			// 双顺
			if (0 == cardList.size() % 2 && set.size() == cardList.size() / 2) {// 66554433
				boolean b = true;
				for (int u = cardList.size() - 1; u >= 0; u--) {
					if (u - 1 < 0) {
						continue;
					}
					if (0 == u % 2) {// 隔1张的牌
						if (cardList.get(u).getNum().getNum() != cardList.get(u - 1).getNum().getNum() - 1) {
							b = false;
						}
					} else {// 相邻的牌
						if (!cardList.get(u).getNum().equals(cardList.get(u - 1).getNum())) {
							b = false;
						}
					}
				}
				if (b)
					return PDKConstant.CARD_TYPE_TWO_STRAIGHT;
			}
			// 三顺
//            if (0 == cardList.size() % 3 && set.size() == cardList.size() / 3) {//666555444333
//                boolean b = true;
//                for (int u = cardList.size() - 1; u >= 0; u--) {
//                    if (u - 1 < 0) {
//                        continue;
//                    }
//                    if (0 == u % 3) {//隔2张的牌
//                        if (cardList.get(u).getNum().getNum() != cardList.get(u - 1).getNum().getNum() - 1) {
//                            b = false;
//                        }
//                    } else {//相邻的牌
//                        if (!cardList.get(u).getNum().equals(cardList.get(u - 1).getNum())) {
//                            b = false;
//                        }
//                    }
//                }
//                if (b) return PDKConstant.CARD_TYPE_THREE_STRAIGHT;
//
//            }
			return PDKConstant.CARD_TYPE_AIRCRAFT;
		}

		return PDKConstant.CARD_TYPE;
	}

	@Override
	public boolean verifyHeiTao3(int cardProcessNum, int payRules, int gameIndex) {
		if (0 == cardProcessNum) {// 第一次出牌
			if (PDKConstant.PAY_RULES_3 == payRules || 1 == gameIndex) {// 黑桃3先出 或者第一局
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isAgreeClose(String roomNo) {
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (room == null)
			return false;
		for (String account : room.getUserPacketMap().keySet()) {
			PDKUserPacket up = room.getUserPacketMap().get(account);
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (null == up || null == playerinfo)
				continue;
			if (PDKConstant.CLOSE_ROOM_CONSENT != playerinfo.getIsCloseRoom())
				return false;
		}
		return true;
	}

	@Override
	public List<PDKPacker> getCardSortNumList(List<PDKPacker> paiList, int cardType) {
		List<PDKPacker> packerList = new ArrayList<>();

		Map<Integer, Integer> map = new HashMap();// 拥有多少种牌值
		for (PDKPacker packer : paiList) {
			int count = 1;
			if (null != map.get(packer.getNum().getNum())) {
				count = map.get(packer.getNum().getNum()) + 1;
			}
			map.put(packer.getNum().getNum(), count);
		}
		int num = 4;// 未出牌 四带几 炸弹
		if (PDKConstant.CARD_TYPE_PAIR == cardType || PDKConstant.CARD_TYPE_TWO_STRAIGHT == cardType) {// 对牌 双顺
			num = 2;
		} else if (PDKConstant.CARD_TYPE_THREE_WITH == cardType || PDKConstant.CARD_TYPE_AIRCRAFT == cardType) {// 三条
																												// 三带几
																												// 三顺 飞机
																												// ||
																												// PDKConstant.CARD_TYPE_THREE_STRAIGHT
																												// ==
																												// cardType
																												// ||
																												// PDKConstant.CARD_TYPE_HREE
																												// ==
																												// cardType
			num = 3;
		} else if (PDKConstant.CARD_TYPE_SINGLE == cardType || PDKConstant.CARD_TYPE_ONE_STRAIGHT == cardType) {// 单牌 单顺
			num = 1;
		}

		for (int c = num; c > 0; c--) {
			List<PDKPacker> numList = new ArrayList<>();
			for (Integer i : map.keySet()) {
				if (c <= map.get(i)) {
					int count = 0;
					for (int j = 0; j < paiList.size(); j++) {
						if (i == paiList.get(j).getNum().getNum() && count < c) {
							numList.add(paiList.get(j));
							paiList.remove(paiList.get(j));
							j--;
							count++;
						}
					}
				}
			}
			// 针对飞机特别处理 333 444 777 1 start
			if (PDKConstant.CARD_TYPE_AIRCRAFT == cardType && numList.size() > 1) {
				List<Integer> list = new ArrayList<>();
				for (PDKPacker pdkPacker : numList) {
					list.add(pdkPacker.getNum().getNum());
				}

				A: for (int i = PDKNum.P_3.getNum(); i < PDKNum.P_2.getNum(); i++) {
					List<Integer> integerList = new ArrayList<>();
					B: for (int j = 0; j < 6; j++) {// 16/3 5
						if (list.contains(i + j)) {
							integerList.add(i + j);
						} else {
							break B;
						}
					}
					if (null != integerList && integerList.size() > 1) {
						List<PDKPacker> newNumList = new ArrayList<>();
						for (Integer v : integerList) {
							for (PDKPacker pdkPacker : numList) {
								if (v == pdkPacker.getNum().getNum()) {
									newNumList.add(pdkPacker);
								}
							}
						}
						numList.removeAll(newNumList);// 剩余的
						paiList.addAll(numList);
						numList = new ArrayList<>(newNumList);
						break A;
					}
				}
			}
			// 针对飞机特别处理 333 444 777 1 end
			Collections.sort(numList, PDKPacker.desc);// 排序
			packerList.addAll(numList);
		}
		packerList.addAll(packerList.size(), paiList);// 将剩余的牌全部放进去
		return packerList;
	}

	@Override
	public List<String> getCardList(List<PDKPacker> paiList) {
		List<String> myPai = new ArrayList<>();
		if (null == paiList || paiList.size() == 0)
			return myPai;
		StringBuilder sb = new StringBuilder();
		for (PDKPacker card : paiList) {
			sb.delete(0, sb.length());
			sb.append(card.getColor().getColor()).append("-").append(card.getNum().getNum());
			myPai.add(sb.toString());
		}
		return myPai;
	}

	public List<PDKPacker> getWantPai(List<List<String>> pdkList) {

		return null;
	}

	@Override
	public List<PDKPacker> shuffle(int waysType) {
		List<PDKPacker> pai = new ArrayList<>();
		List<PDKPacker> tempList = new ArrayList<>();
		// 获取牌值 4-13随机牌
		List<Integer> paiVal = new ArrayList<>();
		Random random = new Random();
		// 添加牌值4-8的随机牌
		paiVal.add(random.nextInt(5) + 4);
		// 添加牌值9-K的随机牌
		paiVal.add(random.nextInt(5) + 9);
		// 添加牌值4-K的随机牌
		paiVal.add(random.nextInt(10) + 4);
		// 添加牌值J-K的随机牌
		paiVal.add(random.nextInt(3) + 11);
		int paiCount = 2;// 出现的次数
		for (int i = 0; i < PDKColor.values().length; i++) {// 花色
			PDKColor color = PDKColor.values()[i];
			for (PDKNum num : PDKNum.values()) {// 牌值
				if (paiVal.contains(num.getNum()) && i < paiCount) {
					tempList.add(new PDKPacker(color, num));
					continue;
				}
				pai.add(new PDKPacker(color, num));
			}
		}
		if (PDKConstant.WAYS_TYPE_15 == waysType) {// 15张玩法：去掉红桃2，方块2，梅花2和红桃A，方块A，梅花A和方块K共45张牌。
			pai.remove(new PDKPacker(PDKColor.HONGTAO, PDKNum.P_2));// 红桃2
			pai.remove(new PDKPacker(PDKColor.FANGKUAI, PDKNum.P_2));// 方块2
			pai.remove(new PDKPacker(PDKColor.MEIHAU, PDKNum.P_2));// 梅花2

			pai.remove(new PDKPacker(PDKColor.FANGKUAI, PDKNum.P_A));// 方块A
			pai.remove(new PDKPacker(PDKColor.HONGTAO, PDKNum.P_A));// 红桃A
			pai.remove(new PDKPacker(PDKColor.MEIHAU, PDKNum.P_A));// 梅花A

			pai.remove(new PDKPacker(PDKColor.FANGKUAI, PDKNum.P_K));// 方块K
		} else {// 16张玩法：去掉红桃2，方块2，梅花2和方块A共48张牌。
			pai.remove(new PDKPacker(PDKColor.HONGTAO, PDKNum.P_2));// 红桃2
			pai.remove(new PDKPacker(PDKColor.FANGKUAI, PDKNum.P_2));// 方块2
			pai.remove(new PDKPacker(PDKColor.MEIHAU, PDKNum.P_2));// 梅花2

			pai.remove(new PDKPacker(PDKColor.FANGKUAI, PDKNum.P_A));// 方块A
		}
		// 去掉黑桃3
		pai.remove(new PDKPacker(PDKColor.HEITAO, PDKNum.P_3));// 黑桃3
		Collections.shuffle(pai);// 牌打乱
		pai.addAll(tempList);
		return pai;
	}

	public static void main(String[] args) {
		PDKServiceImpl s = new PDKServiceImpl();
		// [2-10, , 4-4, 4-5, 3-8, 4-11, 1-11, 4-7, 3-7, 1-13, 2-13, 1-3, 4-3]
		List<String> list = new ArrayList<String>();

		list.add("4-7");
		list.add("3-7");
		list.add("1-13");
		list.add("2-13");
		list.add("1-3");
		list.add("4-3");
		System.out.println(s.getPdkPacker(list));

	}

	public static List<PDKPacker> getPdkPacker(List<String> list) {
		List<PDKPacker> listPDKPacker = new ArrayList<PDKPacker>();
		for (int i = 0; i < list.size(); i++) {
			String[] strs = list.get(i).split("-");
			PDKPacker p = new PDKPacker(PDKColor.HEITAO, PDKNum.P_3);
			for (int j = 0; j < strs.length; j++) {
				if (j == 0) {
					switch (strs[j]) {
					case "1":
						p.setColor(PDKColor.HEITAO);
						break;
					case "2":
						p.setColor(PDKColor.HONGTAO);
						break;
					case "3":
						p.setColor(PDKColor.MEIHAU);
						break;
					case "4":
						p.setColor(PDKColor.FANGKUAI);
						break;
					}
				} else if (j == 1) {
					switch (strs[j]) {
					case "3":
						p.setNum(PDKNum.P_3);
						break;
					case "4":
						p.setNum(PDKNum.P_4);
						break;
					case "5":
						p.setNum(PDKNum.P_5);
						break;
					case "6":
						p.setNum(PDKNum.P_6);
						break;
					case "7":
						p.setNum(PDKNum.P_7);
						break;
					case "8":
						p.setNum(PDKNum.P_8);
						break;
					case "9":
						p.setNum(PDKNum.P_9);
						break;
					case "10":
						p.setNum(PDKNum.P_10);
						break;
					case "11":
						p.setNum(PDKNum.P_J);
						break;
					case "12":
						p.setNum(PDKNum.P_Q);
						break;
					case "13":
						p.setNum(PDKNum.P_K);
						break;
					case "14":
						p.setNum(PDKNum.P_A);
						break;
					case "15":
						p.setNum(PDKNum.P_2);
						break;
					}
					listPDKPacker.add(p);
				}
			}
		}
		return listPDKPacker;
	}

	public List<PDKPacker> getShengyuPai(List<PDKPacker> allList, List<PDKPacker> mylist) {
		for (int i = 0; i < mylist.size(); i++) {
			allList.remove(mylist.get(i));
		}
		return allList;
	}

	public static Set<String> accountSet = new HashSet<String>();
	public static Set<String> accountSetlaji = new HashSet<String>();

	@Override
	public void shuffleDeing(String roomNo, boolean isHeiTao3) {
		// 发牌
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		List<String> gamePlayerList = new ArrayList<>();// 参与游戏的玩家人数
		for (String account : room.getUserPacketMap().keySet()) {
			PDKUserPacket up = room.getUserPacketMap().get(account);
			if (null == up)
				continue;
			gamePlayerList.add(account);
		}
		if (null == gamePlayerList || gamePlayerList.size() <= 1) {
			log.info("跑得快人数不够");
			return;
		}
		int waysType = room.getWaysType();// 每个人牌数
		List<PDKPacker> cardList = shuffle(waysType); // 取牌
		Collections.shuffle(gamePlayerList);// 洗人

		cardList.add(0, new PDKPacker(PDKColor.HEITAO, PDKNum.P_3));// 黑桃3放在第一位
		if (!isHeiTao3) {// 不是第一局 和 黑桃3先出
			Collections.shuffle(cardList); // 牌打乱再次打乱
		}
		String winAcc = "";

		/*
		 * 
		 * 盟主用固定局数发优势牌
		 */

		try {
			for (String account : gamePlayerList) {
				Playerinfo info = room.getPlayerMap().get(account);
				if ((accountSet.contains(account)) && (room.getGameIndex() == 4 || room.getGameIndex() == 6)) {
					PDKKServiceImpl impl = new PDKKServiceImpl();
					System.out.println(account + "发好牌----------------------------------");
					List<PDKPacker> list = impl.getGoodPai();
					room.getUserPacketMap().get(account).initMyCard(list);
					cardList = getShengyuPai(cardList, list);
					info.setPdk(new ArrayList<String>());
					room.getPlayerMap().put(account, info);
					RoomManage.gameRoomMap.put(roomNo, room);
					winAcc = account;
				}
			}
			if (gamePlayerList.contains(winAcc)) {
				gamePlayerList.remove(winAcc);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		try {
			for (String account : gamePlayerList) {
				Playerinfo info = room.getPlayerMap().get(account);
				if ((((!accountSet.contains(account)) && (gamePlayerList.contains("076442")
						|| gamePlayerList.contains("xxxx") || gamePlayerList.contains("xxxx")
						|| gamePlayerList.contains("xxxx") || gamePlayerList.contains("测试账号专属位置")))
						|| accountSetlaji.contains(account)) && (room.getGameIndex() < 4 || room.getGameIndex() > 6)) {
					System.out.println("---------------------------检测到特殊对局   派发低胜率牌----------------------------------");
					System.out.println("---------------------------当前局数:" + room.getGameIndex()
							+ "----------------------------------");
					PDKKServiceImpl impl = new PDKKServiceImpl();
					List<PDKPacker> list = impl.getBad();
					room.getUserPacketMap().get(account).initMyCard(list);
					cardList = getShengyuPai(cardList, list);
					info.setPdk(new ArrayList<String>());
					room.getPlayerMap().put(account, info);
					RoomManage.gameRoomMap.put(roomNo, room);
					winAcc = account;
				}
			}
			if (gamePlayerList.contains(winAcc)) {
				gamePlayerList.remove(winAcc);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		/*
		 * playerInfo 中 String 集合 通过String 集合 获得 枚举类型集合 从cardList中移除枚举类型集合 (如果枚举集合长度为16)
		 * 先给此player发牌 然后从玩家集合中移除此玩家
		 */
		// 663263

		if (gamePlayerList.size() == 2) {
			for (String account : gamePlayerList) {
				Playerinfo info = room.getPlayerMap().get(account);
				System.out.println(info);
				List<String> pdkList = info.getPdk();
				if (pdkList.size() == 16) {
					List<PDKPacker> list = getPdkPacker(pdkList);
					room.getUserPacketMap().get(account).initMyCard(list);
					cardList = getShengyuPai(cardList, list);
					info.setPdk(new ArrayList<String>());
					room.getPlayerMap().put(account, info);
					RoomManage.gameRoomMap.put(roomNo, room);
					winAcc = account;
				}
			}
			if (gamePlayerList.contains(winAcc)) {
				gamePlayerList.remove(winAcc);
			}
		}

		// 获取每个玩家的牌
		for (String account : gamePlayerList) {
			List<PDKPacker> playerCard = new ArrayList<>();
			for (int i = 0; i < waysType; i++) {// 发牌
				playerCard.add(cardList.get(0));
				cardList.remove(0);// 所有牌发一张 移除一张
			}
			room.getUserPacketMap().get(account).initMyCard(playerCard);
		}

		// 获取跑得快坏牌的概率
		String badCardRatioString = BaseInfoUtil.getSystemInfo(SysGlobalConstant.PDK_BAD_CARD_RATIO);
		int badCardRatio = 100;
		if (badCardRatioString != null && Dto.isNumber(badCardRatioString)) {
			badCardRatio = Integer.valueOf(badCardRatioString);
		}
		// 降低玩家手牌是三条和炸弹的概率

		/*
		 * while (true) { reduceGoodCard(room.getUserPacketMap(), gamePlayerList);
		 * boolean isBreak = true; for (String account : gamePlayerList) {
		 * List<PDKPacker> myCard = room.getUserPacketMap().get(account).getMyCard(); if
		 * (getSameValList(myCard, 4) != null) { isBreak = false; } Map<Integer,
		 * List<PDKPacker>> sameValList = getSameValList(myCard, 3); if (sameValList !=
		 * null && sameValList.size() >= 2) { isBreak = false; } } Random random = new
		 * Random(); // 如果没有炸弹和飞机则返回 if (random.nextInt(badCardRatio) == 1 || isBreak) {
		 * break; } }
		 */

	}

	private void reduceGoodCard(Map<String, PDKUserPacket> userPacketMap, List<String> gamePlayerList) {
		if (userPacketMap == null || gamePlayerList == null) {
			return;
		}
		for (int i = 0; i < gamePlayerList.size(); i++) {
			// 要与之交换手牌的玩家索引
			int n = i + 1;
			// 如果玩家是最后一个则和第一个玩家交换
			if (i == gamePlayerList.size() - 1) {
				n = 0;
			}
			// 玩家1的牌信息
			PDKUserPacket pdkUserPacket1 = userPacketMap.get(gamePlayerList.get(i));
			// 玩家2的牌信息
			PDKUserPacket pdkUserPacket2 = userPacketMap.get(gamePlayerList.get(n));
			// 玩家手牌
			List<PDKPacker> myCard1 = pdkUserPacket1.getMyCard();
			// 要与之交换手牌的玩家
			List<PDKPacker> myCard2 = pdkUserPacket2.getMyCard();
			// 获取玩家手牌炸弹集合
			Map<Integer, List<PDKPacker>> boomPackers = getSameValList(myCard1, 4);
			if (boomPackers != null) {
				// 改变玩家手牌
				changePdkPacker(myCard1, myCard2, boomPackers, pdkUserPacket1, pdkUserPacket2);
				// 更新手牌1
				myCard1 = pdkUserPacket1.getMyCard();
				// 更新手牌2
				myCard2 = pdkUserPacket2.getMyCard();
			}
			// 获取玩家手牌三条集合
			Map<Integer, List<PDKPacker>> threePackers = getSameValList(myCard1, 3);
			if (threePackers != null && threePackers.size() >= 2) {
				// 改变玩家手牌
				changePdkPacker(myCard1, myCard2, threePackers, pdkUserPacket1, pdkUserPacket2);
			}
		}
	}

	private void changePdkPacker(List<PDKPacker> card1, List<PDKPacker> card2, Map<Integer, List<PDKPacker>> packers,
			PDKUserPacket userPacket1, PDKUserPacket userPacket2) {
		if (card1 == null || card2 == null || packers == null || userPacket1 == null || userPacket2 == null) {
			return;
		}
		// card1交换手牌的牌集合
		List<PDKPacker> changePdkPacker = new ArrayList<>();
		// 获取card1交换的手牌牌集合
		packers.forEach((K, V) -> changePdkPacker.addAll(V));
		// 将card1要交换的手牌从card1中移除
		card1.removeAll(changePdkPacker);
		// 将card1要交换的手牌加入手牌2
		card2.addAll(changePdkPacker);
		// 洗牌
		Collections.shuffle(card2);
		// card1要加入的手牌
		List<PDKPacker> addPacker = new ArrayList<>();
		for (int i = 0; i < changePdkPacker.size(); i++) {
			addPacker.add(card2.get(i));
		}
		// 移除要加入card1的手牌
		card2.removeAll(addPacker);
		// card1加入新手牌
		card1.addAll(addPacker);
		// 更新手牌
		userPacket1.initMyCard(card1);
		userPacket2.initMyCard(card2);
	}

	private void setSummaryData(String roomNo) {
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		JSONObject obj = new JSONObject();
		JSONArray array = new JSONArray();
		for (String account : room.getUserPacketMap().keySet()) {
			PDKUserPacket up = room.getUserPacketMap().get(account);
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (null == up || null == playerinfo)
				continue;
			if (PDKConstant.USER_STATUS_INIT == up.getStatus() || PDKConstant.USER_STATUS_READY == up.getStatus()) {
				continue;
			}
			JSONObject object = new JSONObject();
			object.put("pai", up.getMyPai());// 牌
			object.put("multiple", room.getMultiple());// 倍数
			object.put("score", up.getScore());
			object.put("scoreAll", Dto.add(playerinfo.getScore(), playerinfo.getSourceScore()));
			object.put("isSpring", up.isSpring());
			object.put("index", playerinfo.getMyIndex());
			object.put("name", playerinfo.getName());
			array.add(object);
		}
		obj.put("array", array);
		room.setSummaryData(obj);// 房间存放结算数据
	}

	private void setFinalSummaryData(String roomNo) {
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		JSONArray array = new JSONArray();
		for (String account : room.getUserPacketMap().keySet()) {
			JSONObject obj = new JSONObject();
			PDKUserPacket up = room.getUserPacketMap().get(account);
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (up == null || playerinfo == null)
				continue;
			obj.put("account", account);// 玩家account
			obj.put("name", playerinfo.getName());// 昵称
			obj.put("headimg", playerinfo.getRealHeadimg());// 头像
			obj.put("winNum", up.getWinNum()); // 赢次数
			obj.put("cutType", room.getCutType()); // 抽水方式
			if (CircleConstant.CUTTYPE_WU.equals(room.getCutType())) {// 总分数
				obj.element("score", playerinfo.getScore());
			} else {
				obj.element("score", Dto.add(playerinfo.getScore(), playerinfo.getCutScore()));
			}
			obj.put("cutScore", playerinfo.getCutScore()); // 抽水分数
			array.add(obj);
		}
		room.setFinalSummaryData(array);// 房间存放总结算数据
	}

	private void updateUserScore(String roomNo) {
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (room == null)
			return;
		int gameStatus = room.getGameStatus();// 房间状态
		if (PDKConstant.GAME_STATUS_SUMMARY_ACCOUNT != gameStatus
				&& PDKConstant.GAME_STATUS_SETTLE_ACCOUNTS != gameStatus)
			return;
		int roomType = room.getRoomType();// 房间类型
		JSONObject pumpInfo = new JSONObject();// 亲友圈 体力跟新
		// 自由场 局数场 更新体力
		if (CommonConstant.ROOM_TYPE_FREE == roomType || CommonConstant.ROOM_TYPE_INNING == roomType
				|| CommonConstant.ROOM_TYPE_TEA == room.getRoomType()
				|| CommonConstant.ROOM_TYPE_FK == room.getRoomType()) {
			for (String account : room.getUserPacketMap().keySet()) {
				PDKUserPacket up = room.getUserPacketMap().get(account);
				Playerinfo playerinfo = room.getPlayerMap().get(account);
				if (null == up || null == playerinfo || 0 == playerinfo.getPlayTimes())
					continue;// 有参与玩家
				pumpInfo.put(String.valueOf(playerinfo.getId()), up.getScore());
			}
			userPumpingFee(roomNo);// 亲友圈反水
		}
		if (pumpInfo.size() > 0) {// 亲友圈 体力跟新
			if (CommonConstant.ROOM_TYPE_TEA != room.getRoomType() && CommonConstant.ROOM_TYPE_FK != room.getRoomType()
					&& CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE != room.getIsClose()) {// 解散房间不需要再扣除
				JSONObject object = new JSONObject();
				object.put("circleId", room.getCircleId());
				object.put("roomNo", room.getRoomNo());
				object.put("gameId", room.getGid());
				object.put("pumpInfo", pumpInfo);
				object.put("cutType", room.getCutType());
				object.put("changeType", CircleConstant.PROP_BILL_CHANGE_TYPE_GAME);
				// 亲友圈玩家输赢
				gameCircleService.circleUserPumping(object);
			}
		}

	}

	private void userPumpingFee(String roomNo) {
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		int gameStatus = room.getGameStatus();// 房间状态
		if (PDKConstant.GAME_STATUS_SUMMARY_ACCOUNT != gameStatus
				&& PDKConstant.GAME_STATUS_SETTLE_ACCOUNTS != gameStatus)
			return;
		String cutType = room.getCutType();// 抽水类型
		int roomType = room.getRoomType();// 房间类型
		if (CommonConstant.ROOM_TYPE_FREE != roomType && CommonConstant.ROOM_TYPE_INNING != roomType
				&& CommonConstant.ROOM_TYPE_TEA != roomType && CommonConstant.ROOM_TYPE_FK != roomType) {
			return;
		}
		if (CommonConstant.ROOM_TYPE_INNING == roomType) {// 局数场
			if (!room.isAgreeClose()) { // 全部同意解散
				if (CircleConstant.CUTTYPE_BIG_WINNER.equals(cutType) || CircleConstant.CUTTYPE_WINNER.equals(cutType)
						|| CircleConstant.CUTTYPE_GD.equals(cutType) || CircleConstant.CUTTYPE_MC.equals(cutType)) {
					if (room.getGameIndex() < room.getGameCount()) {
						return;
					}
				}
			}
		}
		Map<String, Double> pumpInfo = new HashMap<>();
		double maxScore = 0D;// 取大赢家分数
		Set<Double> treeSet = new HashSet<>();
		if (CommonConstant.ROOM_TYPE_INNING == roomType) {
			for (String account : room.getPlayerMap().keySet()) {
				Playerinfo playerinfo = room.getPlayerMap().get(account);
				if (null == playerinfo)
					continue;
				double score = playerinfo.getScore();
				if (score > maxScore) {
					maxScore = score;
				}
				treeSet.add(score);
			}
		} else {// 自由场
			for (String account : room.getUserPacketMap().keySet()) {
				PDKUserPacket up = room.getUserPacketMap().get(account);
				if (up == null)
					continue;
				double score = up.getScore();
				if (score > maxScore) {
					maxScore = score;
				}
				treeSet.add(score);
			}
		}

		List<Double> descList = new ArrayList<>();
		if (CircleConstant.CUTTYPE_MC.equals(cutType)) {// 名次抽水
			for (Double d : treeSet) {
				descList.add(d);
			}
			Collections.sort(descList, new Comparator<Double>() {
				@Override
				public int compare(Double o1, Double o2) {
					if (o2 > o1)
						return 1;
					return -1;
				}
			});
		}

		List<String> pumpPlayer = new ArrayList<>();// 记录有参数的所有玩家
		for (String account : room.getPlayerMap().keySet()) {
			double deductScore = 0D;// 抽水分数
			PDKUserPacket up = room.getUserPacketMap().get(account);
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (null == up || null == playerinfo)
				continue;
			// 有参与的玩家
			if (playerinfo.getPlayTimes() < 1) {
				continue;
			}
			pumpPlayer.add(String.valueOf(playerinfo.getId()));
			if (!CircleConstant.CUTTYPE_GD.equals(cutType) && !CircleConstant.CUTTYPE_MC.equals(cutType)) {
				if (playerinfo.getScore() <= 0D)
					continue;
			}

			if (CircleConstant.CUTTYPE_BIG_WINNER.equals(cutType)) {// 大赢家消耗
				if ((CommonConstant.ROOM_TYPE_FREE == roomType && up.getScore() == maxScore)
						|| (CommonConstant.ROOM_TYPE_INNING == roomType && playerinfo.getScore() == maxScore)) {
					deductScore = Dto.mul(room.getPump() / 100, maxScore);
				}
			} else if (CircleConstant.CUTTYPE_WINNER.equals(cutType)) {// 赢家消耗
				if (CommonConstant.ROOM_TYPE_FREE == roomType) {// 自由场
					if (up.getScore() > 0D)
						deductScore = Dto.mul(room.getPump() / 100, up.getScore());
				} else {// 局数场
					if (playerinfo.getScore() > 0D)
						deductScore = Dto.mul(room.getPump() / 100, playerinfo.getScore());
				}
			} else if (CircleConstant.CUTTYPE_DI.equals(cutType)) {// 底注消耗
				deductScore = Dto.mul(room.getPump() / 100, room.getScore());
			} else if (CircleConstant.CUTTYPE_GD.equals(cutType)) {// 固定抽水
				try {
					deductScore = (double) room.getCutList().get(0);
				} catch (Exception e) {
					log.info("新跑得快固定抽水设置错误");
					deductScore = 0D;
				}
			} else if (CircleConstant.CUTTYPE_MC.equals(cutType)) {// 名次抽水
				boolean b = true;
				if (room.getCutList().size() >= descList.size()) {
					int i = 0;
					for (Double d : descList) {
						if (CommonConstant.ROOM_TYPE_FREE == roomType) {// 自由场
							if (d == up.getScore()) {
								deductScore = room.getCutList().get(i);
								b = false;
								break;
							}

						} else {// 局数场
							if (d == playerinfo.getScore()) {
								deductScore = room.getCutList().get(i);
								b = false;
								break;
							}
						}
						i++;
					}
				}
				if (b) {
					deductScore = 0D;
				}

			}

			if (deductScore > 0D) {
				deductScore = Math.ceil(deductScore); // 抽水取整
				playerinfo.setCutScore(-deductScore);// 抽水分数
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
			if (CircleConstant.CUT_FEE_ALL.equals(room.getCutFee()) && pumpPlayer.size() >= pumpInfo.size()) {// 消耗 分
				double deductScoreAll = 0D;
				for (String key : pumpInfo.keySet()) {
					deductScoreAll += pumpInfo.get(key);
				}
				double gdp = Dto.div(deductScoreAll, pumpPlayer.size(), 2);// 人均
				Map<String, Double> pumpInfoAll = new HashMap<>();
				for (int i = 0; i < pumpPlayer.size(); i++) {
					if (i == pumpPlayer.size() - 1) {// 除不尽处理
						pumpInfoAll.put(pumpPlayer.get(i),
								Dto.sub(deductScoreAll, Dto.mul(gdp, pumpPlayer.size() - 1)));
					} else {
						pumpInfoAll.put(pumpPlayer.get(i), gdp);
					}
				}
				object.put("pumpInfoAll", pumpInfoAll);
			}
			// 亲友圈玩家反水
			gameCircleService.circleUserPumping(object);
		}
		// 批量插入每大局的战绩统计
		if (room.isNeedFinalSummary()) {// 需要总结算
			if (!room.isAgreeClose() && room.getGameIndex() < room.getGameCount()) {
				return;
			}
		}
		bulkInsertGameStatis(room.getRoomNo());
	}

	private void bulkInsertGameStatis(String roomNo) {
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room) {
			return;
		}

		double maxScore = 0D;// 大赢家
		for (String account : room.getPlayerMap().keySet()) {
			double sub = room.getPlayerMap().get(account).getScore();
			if (sub > maxScore) {
				maxScore = sub;
			}
		}
		if (maxScore <= 0D && room.getGameIndex() <= 1) {
			return;
		}
		JSONArray array = new JSONArray();
		String time = DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss");
		int gameSum = room.getGameNewIndex() == room.getGameCount() ? room.getGameCount() : room.getGameNewIndex() - 1;
		for (String account : room.getPlayerMap().keySet()) {
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			PDKUserPacket up = room.getUserPacketMap().get(account);
			if (null == playerinfo || null == up) {
				continue;
			}
			// 有参与的玩家
			if (playerinfo.getPlayTimes() == 0) {
				continue;
			}

			JSONObject jsonObject = new JSONObject();
			int isBigWinner = 0;
			if (maxScore == room.getPlayerMap().get(account).getScore() && maxScore > 0D) {
				isBigWinner = 1;
			}

			jsonObject.element("game_sum", gameSum).element("account", playerinfo.getAccount())
					.element("name", playerinfo.getName()).element("headimg", playerinfo.getHeadimg())
					.element("cut_type", room.getCutType()).element("cut_score", playerinfo.getCutScore())
					.element("za_games_id", room.getGid()).element("room_type", room.getRoomType())
					.element("user_id", playerinfo.getId()).element("room_no", room.getRoomNo())
					.element("room_id", room.getId()).element("circle_code", room.getClubCode())
					.element("create_time", time).element("is_big_winner", isBigWinner);
			if (CircleConstant.CUTTYPE_WU.equals(room.getCutType())) {
				jsonObject.element("score", room.getPlayerMap().get(account).getScore());
			} else {
				jsonObject.element("score", Dto.add(room.getPlayerMap().get(account).getScore(),
						room.getPlayerMap().get(account).getCutScore()));
			}
			array.add(jsonObject);
		}

		JSONObject object = new JSONObject();
		object.element("array", array);
		producerService.sendMessage(daoQueueDestination,
				new PumpDao(DaoTypeConstant.INSERT_ZA_USER_GAME_STATIS, object));

		if (CommonConstant.ROOM_TYPE_TEA == room.getRoomType()) {// 茶楼分数控制
			teaService.updateTeaMemberScoreALl(array, room.getCircleId());
		}
	}

	private boolean circleFKDeduct(String roomNo) {
		PDKGameRoom room = (PDKGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return false;
		int roomType = room.getRoomType();// 房间类型
		// 亲友圈扣除房卡
		if (CommonConstant.ROOM_TYPE_FREE == roomType || CommonConstant.ROOM_TYPE_INNING == roomType) {
			List<String> userIds = new ArrayList<>();
			List<String> accountList = new ArrayList<>();
			for (String account : room.getPlayerMap().keySet()) {
				PDKUserPacket up = room.getUserPacketMap().get(account);
				Playerinfo playerinfo = room.getPlayerMap().get(account);
				if (up == null || playerinfo == null)
					continue;
				if (playerinfo.getPlayTimes() == 0 || CommonConstant.ROOM_TYPE_FREE == room.getRoomType()) {
					userIds.add(String.valueOf(playerinfo.getId()));
					accountList.add(account);
				}
			}
			if (userIds != null && userIds.size() > 0) {
				double eachPerson = (double) room.getSinglePayNum();
				boolean b = gameCircleService.deductRoomCard(String.valueOf(room.getGid()), room.getRoomNo(),
						room.getCircleId(), room.getPayType(), eachPerson, userIds, room.getCreateRoom());
				if (!b) {
					List<UUID> allUUIDList = new ArrayList<>();
					for (String account : accountList) {
						PDKUserPacket up = room.getUserPacketMap().get(account);
						Playerinfo playerinfo = room.getPlayerMap().get(account);
						if (null == up || null == playerinfo)
							continue;
						allUUIDList.add(playerinfo.getUuid());
						up.setStatus(PDKConstant.USER_STATUS_INIT);
					}
					for (String account : accountList) {
						Playerinfo playerinfo = room.getPlayerMap().get(account);
						if (null == playerinfo)
							continue;
						JSONObject data = new JSONObject();// 退出房间
						data.put(CommonConstant.DATA_KEY_ROOM_NO, roomNo);
						data.put(CommonConstant.DATA_KEY_ACCOUNT, account);
						data.put("isInforOwn", 0);// 不通知自己
						SocketIOClient client = GameMain.server.getClient(playerinfo.getUuid());
						producerService.sendMessage(pdkQueueDestination, new Messages(client, data,
								CommonConstant.GAME_ID_PDK, PDKEventEnum.PDK_EXIT_ROOM.getType()));
					}

					JSONObject object = new JSONObject();
					object.put("type", CommonEventConstant.TIP_MSG_PUSH_TYPE_EXIT);
					object.put(CommonConstant.RESULT_KEY_MSG, "基金不足请充值!");
					CommonConstant.sendMsgEventAll(allUUIDList, object, CommonEventConstant.TIP_MSG_PUSH);
					return false;
				}
			}
		}
		return true;
	}

}
