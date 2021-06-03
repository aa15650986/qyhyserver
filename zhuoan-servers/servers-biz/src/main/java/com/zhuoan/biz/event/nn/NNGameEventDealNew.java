package com.zhuoan.biz.event.nn;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.nn.NN;
import com.zhuoan.biz.core.nn.NNColor;
import com.zhuoan.biz.core.nn.NNNum;
import com.zhuoan.biz.core.nn.NNPacker;
import com.zhuoan.biz.core.nn.NNServer;
import com.zhuoan.biz.core.nn.NNUserPacket;
import com.zhuoan.biz.core.pdk.PDKColor;
import com.zhuoan.biz.core.pdk.PDKNum;
import com.zhuoan.biz.event.BaseEventDeal;
import com.zhuoan.biz.event.FundEventDeal;
import com.zhuoan.biz.event.circle.CircleBaseEventDeal;
import com.zhuoan.biz.game.biz.GameCircleService;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.nn.NNGameRoomNew;
import com.zhuoan.biz.model.nn.NNPackerCompare;
import com.zhuoan.biz.model.pdk.PDKPacker;
import com.zhuoan.biz.model.qzmj.QZMJGameRoom;
import com.zhuoan.biz.model.sss.SSSGameRoomNew;
import com.zhuoan.biz.model.sss.SSSUserPacket;
import com.zhuoan.biz.robot.RobotEventDeal;
import com.zhuoan.constant.CircleConstant;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.DaoTypeConstant;
import com.zhuoan.constant.NNConstant;
import com.zhuoan.constant.SSSConstant;
import com.zhuoan.constant.event.CommonEventConstant;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.TeaService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.AppBeanUtil;
import com.zhuoan.util.DateUtils;
import com.zhuoan.util.Dto;
import com.zhuoan.util.thread.ThreadPoolHelper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class NNGameEventDealNew {

	private final static Logger logger = LoggerFactory.getLogger(NNGameEventDealNew.class);
	@Resource
	private GameTimerNN gameTimerNN;
	@Resource
	private RoomBiz roomBiz;
	@Resource
	private Destination daoQueueDestination;
	@Resource
	private TeaService teaService;
	@Resource
	private ProducerService producerService;
	@Resource
	private RedisInfoService redisInfoService;
	@Resource
	private UserBiz userBiz;
	@Resource
	private FundEventDeal fundEventDeal;
	@Resource
	private GameCircleService gameCircleService;
	@Resource
	private BaseEventDeal baseEventDeal;
	@Resource
	private CircleBaseEventDeal circleBaseEventDeal;

	public void createRoom(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		JSONObject roomData = obtainRoomData(account, roomNo);
		// 数据不为空
		if (!Dto.isObjNull(roomData)) {
			JSONObject result = new JSONObject();
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			result.put("data", roomData);
			// 通知自己
			CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_NN");
		}
	}

	public void joinRoom(SocketIOClient client, Object data) {
		// 进入房间通知自己
		createRoom(client, data);
		JSONObject joinData = JSONObject.fromObject(data);
		// 非重连通知其他玩家
		if (joinData.containsKey("isReconnect") && joinData.getInt("isReconnect") == 0) {
			String account = joinData.getString(CommonConstant.DATA_KEY_ACCOUNT);
			NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap
					.get(joinData.getString(CommonConstant.DATA_KEY_ROOM_NO));
			Playerinfo player = room.getPlayerMap().get(account);
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
			// 通知玩家
			CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), obj.toString(), "playerEnterPush_NN");
		}
	}

	public JSONObject obtainRoomData(String account, String roomNo) {
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		JSONObject obj = new JSONObject();
		obj.put("playerCount", room.getPlayerCount());
		obj.put("gameStatus", room.getGameStatus());
		obj.put("room_no", room.getRoomNo());
		obj.put("roomType", room.getRoomType());
		obj.put("game_count", room.getGameCount());
		obj.put("di", room.getScore());
		obj.put("banker_type", room.getBankerType());
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
			obj.put("clubCode", room.getClubCode());
		}
		// 元宝场
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB || room.getRoomType() == CommonConstant.ROOM_TYPE_FREE) {
			StringBuffer roomInfo = new StringBuffer();
			roomInfo.append("底注:");
			roomInfo.append((int) room.getScore());
			roomInfo.append(" 进:");
			roomInfo.append((int) room.getEnterScore());
			roomInfo.append(" 出:");
			roomInfo.append((int) room.getLeaveScore());
			if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_ZZ) {
				roomInfo.append("\nJ以下庄家赢");
			}
			obj.put("roominfo", roomInfo.toString());
			obj.put("roominfo2", room.getWfType());
		}
		// 房卡场
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_DK
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
			StringBuffer roomInfo = new StringBuffer();
			roomInfo.append(room.getWfType());
			obj.put("roominfo", String.valueOf(roomInfo));
		}
		if (Dto.stringIsNULL(room.getBanker())
				|| (room.getGameStatus() <= NNConstant.NN_GAME_STATUS_DZ
						&& room.getBankerType() != NNConstant.NN_BANKER_TYPE_ZZ)
				|| room.getBankerType() == NNConstant.NN_BANKER_TYPE_TB
				|| room.getUserPacketMap().get(room.getBanker()) == null) {
			obj.put("zhuang", CommonConstant.NO_BANKER_INDEX);
			obj.put("qzScore", 0);
		} else {
			obj.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
			obj.put("qzScore", room.getUserPacketMap().get(room.getBanker()).getQzTimes());
		}
		if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_MP) {
			obj.put("qzType", NNConstant.NN_QZ_TYPE);
		}
		if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_TB) {
			obj.put("wanfaType", NNConstant.NN_GAME_TYPE);
		}
		obj.put("game_index", room.getGameIndex());

		obj.put("showTimer", CommonConstant.GLOBAL_NO);
		if (room.getTimeLeft() > NNConstant.NN_TIMER_INIT) {
			obj.put("showTimer", CommonConstant.GLOBAL_YES);
		}
		obj.put("timer", room.getTimeLeft());
		obj.put("myIndex", room.getPlayerMap().get(account).getMyIndex());
		obj.put("qzTimes", room.getQzTimes(room.getPlayerMap().get(account).getScore()));
		obj.put("baseNum", room.getBaseNumTimes(account));
		obj.put("users", room.getAllPlayer());
		obj.put("qiangzhuang", room.getQZResult());
		obj.put("xiazhu", room.getXiaZhuResult());
		obj.put("gameData", room.getGameData(account));
		if (room.getJieSanTime() > 0) {
			obj.put("jiesan", CommonConstant.GLOBAL_YES);
			obj.put("jiesanData", room.getJieSanData());
		}
		if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_ZJS) {
			obj.put("jiesuanData", room.getFinalSummary());
		}
		if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_ZZ
				&& room.getGameStatus() == NNConstant.NN_GAME_STATUS_TO_BE_BANKER) {
			obj.put("bankerMinScore", room.getMinBankerScore());
			obj.put("bankerIsUse", CommonConstant.GLOBAL_NO);
			if (room.getPlayerMap().get(account).getScore() >= room.getMinBankerScore()) {
				obj.put("bankerIsUse", CommonConstant.GLOBAL_YES);
			}
		}
		obj.put("startIndex", getStartIndex(roomNo));
		obj.put("tuizhu", room.getTuiZhuTimes());
		obj.put("reconnectLpMsg", room.getReconnectLpMsg());
		obj.put("circleCode",room.getClubCode());
		
		return obj;
	}

	public void gameBeBanker(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		// 不满足准备条件直接忽略
		if (!CommonConstant.checkEvent(postData, NNConstant.NN_GAME_STATUS_TO_BE_BANKER, client)) {
			return;
		}
		// 房间号
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		// 玩家账号
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		// 庄家已经确定
		if (!Dto.stringIsNULL(room.getBanker())) {
			return;
		}
		// 元宝不足无法上庄
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB || room.getRoomType() == CommonConstant.ROOM_TYPE_JB
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_FREE) {
			if (room.getPlayerMap().get(account).getScore() < room.getMinBankerScore()) {
				return;
			}
		}
		// 设置庄家
		room.setBanker(account);
		// 设置游戏状态
		room.setGameStatus(NNConstant.NN_GAME_STATUS_READY);
		changeGameStatus(room);
	}

	public void gameReady(SocketIOClient client, Object data) {
		
		JSONObject postData = JSONObject.fromObject(data);
		// 不满足准备条件直接忽略
		if (!CommonConstant.checkEvent(postData, NNConstant.NN_GAME_STATUS_INIT, client)
				&& !CommonConstant.checkEvent(postData, NNConstant.NN_GAME_STATUS_READY, client)
				&& !CommonConstant.checkEvent(postData, NNConstant.NN_GAME_STATUS_JS, client)) {
			return;
		}
		// 房间号
		final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room) {
			return;
		}
		// 玩家账号
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		NNUserPacket up = room.getUserPacketMap().get(account);
		Playerinfo playerinfo = room.getPlayerMap().get(account);
		if (null == up || null == playerinfo)
			return;
		// 元宝不足无法准备
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB || room.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
			if (playerinfo.getScore() < room.getLeaveScore()) {
				// 清出房间
				postData.put("notSendToMe", CommonConstant.GLOBAL_YES);
				postData.put("notSend", CommonConstant.GLOBAL_YES);
				exitRoom(client, postData);
				JSONObject result = new JSONObject();
				result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
				if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB) {
					result.put(CommonConstant.RESULT_KEY_MSG, "元宝不足");
				}
				if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
					result.put(CommonConstant.RESULT_KEY_MSG, "金币不足");
				}
				if (room.getRoomType() == CommonConstant.ROOM_TYPE_FREE) {
					result.put(CommonConstant.RESULT_KEY_MSG, "体力值不足");
				}
				CommonConstant.sendMsgEventToSingle(client, result.toString(), "tipMsgPush");
				return;
			}

		} else if (room.getRoomType() == CommonConstant.ROOM_TYPE_COMPETITIVE) {
			if (playerinfo.getRoomCardNum() < room.getLeaveScore()) {
				postData.put("notSend", CommonConstant.GLOBAL_YES);
				postData.put("notSendToMe", CommonConstant.GLOBAL_YES);
				exitRoom(client, postData);
				JSONObject result = new JSONObject();
				result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
				result.put(CommonConstant.RESULT_KEY_MSG, "钻石不足,无法参赛");
				CommonConstant.sendMsgEventToSingle(client, result.toString(), "tipMsgPush");
				return;
			}
		}
		// 亲友圈
		else if (room.getRoomType() == CommonConstant.ROOM_TYPE_FREE
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
			double leaveScore = room.getLeaveScore();// 离场验证
			if (leaveScore > Dto.add(playerinfo.getScore(), playerinfo.getSourceScore())) {
				if (room.getRoomType() == CommonConstant.ROOM_TYPE_INNING && room.getGameIndex() > 0) {// 局数场 ，有玩过的
					if (playerinfo.isCollapse()) {
						for (String userAccount : room.getPlayerMap().keySet()) {
							JSONObject result = new JSONObject();
							if (account.equals(userAccount)) {
								result.put("type", CommonConstant.SHOW_MSG_TYPE_SMALL);
								result.put(CommonConstant.RESULT_KEY_MSG, "体力不足  已破产");
							} else {
								result.put("type", CommonConstant.SHOW_MSG_TYPE_SMALL);
								result.put(CommonConstant.RESULT_KEY_MSG,
										"【" + room.getPlayerMap().get(account).getName() + "】体力不足  已破产");
								//CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(userAccount).getUuid(), room.getPlayerMap().get(account).getMyIndex(), "exitRoomPush_NN");
							}
							SocketIOClient c = GameMain.server
									.getClient(room.getPlayerMap().get(userAccount).getUuid());
							CommonConstant.sendMsgEventToSingle(c, result.toString(), "tipMsgPush");
						}
						userPumpingFee(room);
						RoomManage.gameRoomMap.get(roomNo).setGameStatus(NNConstant.NN_GAME_STATUS_ZJS);
						RoomManage.gameRoomMap.get(roomNo).setNeedFinalSummary(true);
						RoomManage.gameRoomMap.get(roomNo).setGameIndex(room.getGameCount());
						changeGameStatus(room);
						finalSummaryRoom(roomNo);
						return;
					}

					/*
					 * // 强制结算 start userPumpingFee(room);// 亲友圈抽水
					 * room.setGameStatus(NNConstant.NN_GAME_STATUS_ZJS); changeGameStatus(room); //
					 * 强制结算 end for (String userAccount : room.getPlayerMap().keySet()) { JSONObject
					 * result = new JSONObject(); result.put("type",
					 * CommonConstant.SHOW_MSG_TYPE_SMALL); if (account.equals(userAccount)) {
					 * result.put(CommonConstant.RESULT_KEY_MSG, "体力不足  已破产"); } else {
					 * result.put(CommonConstant.RESULT_KEY_MSG, "【" +
					 * room.getPlayerMap().get(account).getName() + "】体力不足  已破产"); } SocketIOClient
					 * c =
					 * GameMain.server.getClient(room.getPlayerMap().get(userAccount).getUuid());
					 * CommonConstant.sendMsgEventToSingle(c, result.toString(), "tipMsgPush"); }
					 */
					//return;
				}
				postData.put("notSend", CommonConstant.GLOBAL_YES);
				postData.put("notSendToMe", CommonConstant.GLOBAL_YES);
				exitRoom(client, postData);
				JSONObject result = new JSONObject();
				result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
				result.put(CommonConstant.RESULT_KEY_MSG, "体力不足");
				CommonConstant.sendMsgEventToSingle(client, result.toString(), "tipMsgPush");
				return;
			}
			// 自由场 房费AA
			if (room.getRoomType() == CommonConstant.ROOM_TYPE_FREE
					&& CommonConstant.ROOM_PAY_TYPE_AA.equals(String.valueOf(room.getPayType()))) {
				JSONObject user = DBUtil.getObjectBySQL("SELECT z.`roomcard` FROM  za_users z  WHERE z.`account`= ?",
						new Object[] { account });
				if (user == null || user.getDouble("roomcard") < room.getSinglePayNum()) {
					postData.put("notSend", CommonConstant.GLOBAL_YES);
					postData.put("notSendToMe", CommonConstant.GLOBAL_YES);
					exitRoom(client, postData);
					JSONObject result = new JSONObject();
					result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
					result.put(CommonConstant.RESULT_KEY_MSG, "钻石不足");
					CommonConstant.sendMsgEventToSingle(client, result.toString(), "tipMsgPush");
					//return;
				}
			}
		}
		// 设置玩家准备状态
		up.setStatus(NNConstant.NN_USER_STATUS_READY);
		// 设置房间准备状态
		room.getUserPacketMap().put(account, up);
		if (room.getGameStatus() != NNConstant.NN_GAME_STATUS_READY) {
			room.setGameStatus(NNConstant.NN_GAME_STATUS_READY);
		}
		int minStartCount = NNConstant.NN_MIN_START_COUNT;
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
			if (!Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("mustFull")) {
				minStartCount = room.getPlayerCount();
			}
		}
		// 当前准备人数大于最低开始人数开始游戏

		if (room.getNowReadyCount() >= 2 && room.getGameIndex()<1) {
			// 当前准备玩家超过两人 通知房主显示提前开始的按钮
			List<UUID> list = new ArrayList<UUID>();
			ConcurrentHashMap<String, Playerinfo> userMap = room.getPlayerMap();
			String fangzhu = "";
			int minIndex = 99;
			for (String userAccount : userMap.keySet()) {
				if (userMap.get(userAccount).getMyIndex() < minIndex) {
					fangzhu = userAccount;
					minIndex = userMap.get(userAccount).getMyIndex();
				}
			}
			JSONObject tqks = new JSONObject();
			tqks.put("index", minIndex);
			tqks.put("account", fangzhu);
			CommonConstant.sendMsgEventToSingle(userMap.get(fangzhu).getUuid(), tqks.toString(), "tqks");
		}
		if (room.getNowReadyCount() == minStartCount) {
			logger.error("准备人数：" + room.getNowReadyCount());
			logger.error("最低开始人数：" + minStartCount);
			room.setTimeLeft(NNConstant.NN_TIMER_READY);
			ThreadPoolHelper.executorService.submit(new Runnable() {
				@Override
				public void run() {
					gameTimerNN.gameOverTime(roomNo, NNConstant.NN_GAME_STATUS_READY, 0);
				}
			});
		}
		// 房间内所有玩家都已经完成准备且人数大于最低开始人数通知开始游戏,否则通知玩家准备
		if (room.getPlayerMap().size() <= room.getPlayerCount() && room.isAllReady()
				&& room.getPlayerMap().size() >= minStartCount) {
			logger.error("房间内所有玩家都已经完成准备且人数大于最低开始人数通知开始游戏");
			// 亲友圈扣除房卡
			if (room.getRoomType() == CommonConstant.ROOM_TYPE_FREE
					|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
				List<String> userIds = new ArrayList<>();
				List<String> accountList = new ArrayList<>();
				for (String accountS : room.getUserPacketMap().keySet()) {
					if (room.getUserPacketMap().containsKey(accountS)
							&& room.getUserPacketMap().get(accountS) != null) {
						if (room.getPlayerMap().get(accountS).getPlayTimes() == 0
								|| room.getRoomType() == CommonConstant.ROOM_TYPE_FREE) {
							userIds.add(String.valueOf(room.getPlayerMap().get(accountS).getId()));
							accountList.add(accountS);
						}
					}
				}
				if (userIds != null && userIds.size() > 0) {
					double eachPerson = (double) room.getSinglePayNum();
					boolean b = gameCircleService.deductRoomCard(String.valueOf(room.getGid()), room.getRoomNo(),
							room.getCircleId(), String.valueOf(room.getPayType()), eachPerson, userIds,
							room.getCreateRoom());
					if (!b) {
						List<UUID> allUUIDList = new ArrayList<>();
						for (String player : accountList) {
							allUUIDList.add(room.getPlayerMap().get(player).getUuid());
							room.getUserPacketMap().get(player).setStatus(NNConstant.NN_GAME_STATUS_INIT);
						}
						for (String player : accountList) {
							postData.put(CommonConstant.DATA_KEY_ACCOUNT, player);
							postData.put("notSend", CommonConstant.GLOBAL_YES);
							exitRoom(GameMain.server.getClient(room.getPlayerMap().get(player).getUuid()), postData);
						}
						JSONObject result = new JSONObject();
						result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
						result.put(CommonConstant.RESULT_KEY_MSG, "基金不足请充值!");
						CommonConstant.sendMsgEventToAll(allUUIDList, result.toString(), "tipMsgPush");
						return;
					}
				}
			}
			logger.error("开始游戏");
			RoomManage.gameRoomMap.put(roomNo, room);
			startGame(room);// 全部准备开始游戏
		} else {
			logger.error("否则通知玩家准备");
			JSONObject result = new JSONObject();
			result.put("index", playerinfo.getMyIndex());
			result.put("showTimer", CommonConstant.GLOBAL_NO);
			if (room.getTimeLeft() >= 0) {
				result.put("showTimer", CommonConstant.GLOBAL_YES);
			}
			result.put("timer", room.getTimeLeft());
			RoomManage.gameRoomMap.put(roomNo, room);
			CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "playerReadyPush_NN");
		}
	}

	public void gameStart(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		// 不满足准备条件直接忽略
		if (!CommonConstant.checkEvent(postData, NNConstant.NN_GAME_STATUS_INIT, client)
				&& !CommonConstant.checkEvent(postData, NNConstant.NN_GAME_STATUS_READY, client)) {
			return;
		}
		// 房间号
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		room.setTimeLeft(NNConstant.NN_TIMER_READY);
		// 玩家账号
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		int type = postData.getInt("type");
		String eventName = "gameStartPush_NN";
		// 只有房卡场和俱乐部有提前开始的功能
		if (room.getRoomType() != CommonConstant.ROOM_TYPE_FK && room.getRoomType() != CommonConstant.ROOM_TYPE_CLUB
				&& room.getRoomType() != CommonConstant.ROOM_TYPE_INNING) {
			sendStartResultToSingle(client, eventName, CommonConstant.GLOBAL_NO, "当前房间不支持");
			return;
		}
		// 不是房主无法提前开始
		if (!account.equals(room.getOwner())) {
			sendStartResultToSingle(client, eventName, CommonConstant.GLOBAL_NO, "当前没有开始权限");
			return;
		}
		// 已经玩过无法提前开始
		if (room.getGameIndex() != 0) {
			sendStartResultToSingle(client, eventName, CommonConstant.GLOBAL_NO, "游戏已开局，无法提前开始");
			return;
		}
		int readyCount = room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_READY
				? room.getNowReadyCount()
				: room.getNowReadyCount() + 1;
		// 实时准备人数不足
		if (readyCount < NNConstant.NN_MIN_START_COUNT) {
			sendStartResultToSingle(client, eventName, CommonConstant.GLOBAL_NO, "当前准备人数不足");
			return;
		}
		// 需要提前退出的人
		List<String> outList = new ArrayList<>();
		for (String player : room.getUserPacketMap().keySet()) {
			// 不是房主且未准备
			if (!account.equals(player)
					&& room.getUserPacketMap().get(player).getStatus() != NNConstant.NN_USER_STATUS_READY) {
				outList.add(player);
			}
		}
		// 第一次点开始游戏有人需要退出
		if (type == NNConstant.START_GAME_TYPE_UNSURE && outList.size() > 0) {
			sendStartResultToSingle(client, eventName, CommonConstant.GLOBAL_YES, "是否开始");
			return;
		}
		if (!Dto.isObjNull(room.getSetting())) {
			room.getSetting().remove("mustFull");
		}
		// 房主未准备直接准备
		if (room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_READY) {
			gameReady(client, data);
		}
		if (outList.size() > 0) {
			// 退出房间
			for (String player : outList) {
				if (room.getUserPacketMap().get(player).getStatus() != NNConstant.NN_USER_STATUS_READY) {
					SocketIOClient playerClient = GameMain.server.getClient(room.getPlayerMap().get(player).getUuid());
					JSONObject exitData = new JSONObject();
					exitData.put(CommonConstant.DATA_KEY_ROOM_NO, roomNo);
					exitData.put(CommonConstant.DATA_KEY_ACCOUNT, player);
					exitData.put("notSend", CommonConstant.GLOBAL_YES);
					exitData.put("notSendToMe", CommonConstant.GLOBAL_YES);
					exitRoom(playerClient, exitData);
					// 通知玩家
					JSONObject result = new JSONObject();
					result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
					result.put(CommonConstant.RESULT_KEY_MSG, "已被房主踢出房间");
					CommonConstant.sendMsgEventToSingle(playerClient, result.toString(), "tipMsgPush");
				}
			}
		} else {
			startGame(room);
		}

	}

	private void sendStartResultToSingle(SocketIOClient client, String eventName, int code, String msg) {
		JSONObject result = new JSONObject();
		result.put(CommonConstant.RESULT_KEY_CODE, code);
		result.put(CommonConstant.RESULT_KEY_MSG, msg);
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), eventName);
	}

	public void startGameNn(NNGameRoomNew room) {
		final String roomNo = room.getRoomNo();
		// 开启定庄
		ThreadPoolHelper.executorService.submit(new Runnable() {
			@Override
			public void run() {
				gameTimerNN.chooseZhuangNnTime(roomNo);
			}
		});

	}

	// 明牌抢庄
	public void startGameMp(NNGameRoomNew room) {
		logger.error("开始明牌抢庄游戏");
		logger.error("1.洗牌发牌");
		shuffleAndFp(room);
		logger.error("2.设置房间状态");
		logger.error("3.设置玩家状态");
		logger.error("4.返回客户端");
		room.setGameStatus(NNConstant.NN_GAME_STATUS_QZ);
		for (String account : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
				if (room.getUserPacketMap().get(account).getStatus() > NNConstant.NN_USER_STATUS_INIT) {
					room.getUserPacketMap().get(account).setStatus(NNConstant.NN_USER_STATUS_READY);
				}
			}
		}
		changeGameStatus(room);
		int gameEventTime = room.getTimeLeft();
		if (gameEventTime > 0) {// 配牌倒计时
			ThreadPoolHelper.executorService.submit(new Runnable() {
				@Override
				public void run() {
					gameTimerNN.gameOverTime(room.getRoomNo(), NNConstant.NN_GAME_EVENT_QZ, gameEventTime);
				}
			});
		}

	}

	public static NNPacker[] shuffle(NNGameRoomNew room) {
		List<NNPacker> pai = new ArrayList<NNPacker>();
		NNPacker[] nnPackers = new NNPacker[52];
		if (NNConstant.NN_FEATURE_LAIZI.equals(room.getFeature())) {
			nnPackers = new NNPacker[54];
		}
		for (int i = 0; i < NNColor.values().length; i++) {// 花色
			NNColor color = NNColor.values()[i];
			for (NNNum num : NNNum.values()) {// 牌值
				if (color.equals(NNColor.JOKER) || num.equals(NNNum.BM) || num.equals(NNNum.P_BIG)
						|| num.equals(NNNum.P_SMALL)) {
					continue;
				}
				pai.add(new NNPacker(num, color));
			}
		}
		// 如果房间为赖子玩法 发鬼牌
		if (NNConstant.NN_FEATURE_LAIZI.equals(room.getFeature())) {
			pai.add(new NNPacker(NNNum.BM, NNColor.JOKER));
			pai.add(new NNPacker(NNNum.P_A, NNColor.JOKER));
		}
		Collections.shuffle(pai);
		for (int i = 0; i < pai.size(); i++) {
			nnPackers[i] = pai.get(i);
		}
		return nnPackers;
	}

	public void startGameZyqz(NNGameRoomNew room) {

		logger.error("开始自由抢庄游戏");
		shuffleAndFp(room);
		room.setGameStatus(NNConstant.NN_GAME_STATUS_QZ);
		for (String account : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
				if (room.getUserPacketMap().get(account).getStatus() > NNConstant.NN_USER_STATUS_INIT) {
					room.getUserPacketMap().get(account).setStatus(NNConstant.NN_USER_STATUS_READY);
				}
			}
		}
		changeGameStatus(room);
		int gameEventTime = room.getTimeLeft();
		if (gameEventTime > 0) {// 配牌倒计时
			ThreadPoolHelper.executorService.submit(new Runnable() {
				@Override
				public void run() {
					gameTimerNN.gameOverTime(room.getRoomNo(), NNConstant.NN_GAME_EVENT_QZ, gameEventTime);
				}
			});
		}
	}

	public void shuffleAndFp(NNGameRoomNew room) {
		NNPacker[] nnPackers = shuffle(room);
		String roomNo = room.getRoomNo();
		ConcurrentMap<String, NNUserPacket> map = room.getUserPacketMap();
		// Map<String, Playerinfo> map2 = room.getPlayerMap();
		int userCount = map.keySet().size();
		// 获取对应人数的五张牌组
		List<NNPacker[]> listPackers = NN.faPai(nnPackers, userCount);
		for (String account : map.keySet()) {
			NNUserPacket np = map.get(account);
			np.setPs(listPackers.get(0));
			listPackers.remove(0);
			map.put(account, np);
		}
		room.setUserPacketMap(map);
		RoomManage.gameRoomMap.put(roomNo, room);
	}

	public void startGame(NNGameRoomNew room) {
		// 更新房间信息
		// start
		if (null == room) {
			return;
		}
		logger.error("============开始游戏=======");
		// 非准备或初始阶段无法开始开始游戏
		if (room.getGameStatus() != NNConstant.NN_GAME_STATUS_READY) {
			return;
		}
		if (room.getGameIndex() > room.getGameCount()) {
			logger.error("局数超过强制结算");
			compulsorySettlement(room.getRoomNo());// 局数超过 强制结算
			return;
		}
		// 插入游戏记录
		redisInfoService.insertSummary(room.getRoomNo(), "_NN");
		// 初始化房间信息
		room.initGame();
		// 更新游戏局数
		JSONObject roomInfo = new JSONObject();
		roomInfo.put("roomId", room.getId());
		producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INDEX, roomInfo));
		if (room.getFee() > 0 && room.getRoomType() != CommonConstant.ROOM_TYPE_FREE
				&& room.getRoomType() != CommonConstant.ROOM_TYPE_FREE
				&& room.getRoomType() != CommonConstant.ROOM_TYPE_INNING
				&& room.getRoomType() != CommonConstant.ROOM_TYPE_TEA) {
			JSONArray array = new JSONArray();
			Map<String, Double> map = new HashMap<String, Double>();
			for (String account : room.getPlayerMap().keySet()) {
				NNUserPacket up = room.getUserPacketMap().get(account);
				Playerinfo playerinfo = room.getPlayerMap().get(account);
				if (null == up || null == playerinfo)
					continue;
				// 中途加入不抽水
				if (NNConstant.NN_USER_STATUS_INIT == up.getStatus())
					continue;
				// 更新实体类数据
				playerinfo.setScore(Dto.sub(playerinfo.getScore(), room.getFee()));
				// 负数清零
				if (playerinfo.getScore() < 0D) {
					playerinfo.setScore(0D);
				}
				array.add(playerinfo.getId());
				map.put(room.getPlayerMap().get(account).getOpenId(), -room.getFee());
			}
			// 抽水
			producerService.sendMessage(daoQueueDestination,
					new PumpDao(DaoTypeConstant.PUMP, room.getJsonObject(array)));
			if (room.isFund()) {
				fundEventDeal.addBalChangeRecord(map, "牛牛游戏抽水");
			}
		}

		// end

		int type = room.getBankerType();
		logger.error("游戏类型：" + type);
		switch (type) {
		case 10:
			// 霸王庄
			break;
		case 50:
			// 自由抢庄
			startGameZyqz(room);
			break;
		case 30:
			startGameMp(room);
			// 明牌抢庄
			break;
		default:
			break;
		}
	}

	public void startGameTb(NNGameRoomNew room) {
		final String roomNo = room.getRoomNo();
		shuffleAndFp(room);
		// 设置房间状态(下注)
		room.setGameStatus(NNConstant.NN_GAME_STATUS_XZ);
		changeGameStatus(room);
		for (String account : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
				if (room.getUserPacketMap().get(account).getStatus() > NNConstant.NN_USER_STATUS_INIT) {
					room.getUserPacketMap().get(account).setStatus(NNConstant.NN_USER_STATUS_XZ);
					// 通比模式默认下一个底注
					room.getUserPacketMap().get(account).setXzTimes((int) room.getScore());
					// 通知玩家
					JSONObject result = new JSONObject();
					result.put("index", room.getPlayerMap().get(account).getMyIndex());
					result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
					result.put("value", room.getUserPacketMap().get(account).getXzTimes());
					result.put("name", room.getUserPacketMap().get(account).getXzTimes() + "倍");
					CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "gameXiaZhuPush_NN");
				}
			}
		}
		// 设置游戏状态(亮牌)
		room.setGameStatus(NNConstant.NN_GAME_STATUS_LP);
		// 设置倒计时
		room.setTimeLeft(NNConstant.NN_TIMER_SHOW);
		// 开启亮牌定时器
		ThreadPoolHelper.executorService.submit(new Runnable() {
			@Override
			public void run() {
				gameTimerNN.gameOverTime(roomNo, NNConstant.NN_GAME_STATUS_LP, 0);
			}
		});
		// 通知前端状态改变
		changeGameStatus(room);
	}

	public void startGameGn(NNGameRoomNew room) {
		final String roomNo = room.getRoomNo();
		shuffleAndFp(room);
		// 设置房间状态(下注)
		room.setGameStatus(NNConstant.NN_GAME_STATUS_XZ);
		changeGameStatus(room);
		for (String account : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
				if (room.getUserPacketMap().get(account).getStatus() > NNConstant.NN_USER_STATUS_INIT) {
					room.getUserPacketMap().get(account).setStatus(NNConstant.NN_USER_STATUS_XZ);
					// 斗公牛模式默认下一个底注
					room.getUserPacketMap().get(account).setXzTimes((int) room.getScore());
					// 通知玩家
					JSONObject result = new JSONObject();
					result.put("index", room.getPlayerMap().get(account).getMyIndex());
					result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
					result.put("value", room.getUserPacketMap().get(account).getXzTimes());
					result.put("name", room.getUserPacketMap().get(account).getXzTimes() + "倍");
					CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "gameXiaZhuPush_NN");
				}
			}
		}
		// 设置游戏状态(亮牌)
		room.setGameStatus(NNConstant.NN_GAME_STATUS_LP);
		// 设置倒计时
		room.setTimeLeft(NNConstant.NN_TIMER_SHOW);
		// 开启亮牌定时器
		ThreadPoolHelper.executorService.submit(new Runnable() {
			@Override
			public void run() {
				gameTimerNN.gameOverTime(roomNo, NNConstant.NN_GAME_STATUS_LP, 0);
			}
		});
		// 通知前端状态改变
		changeGameStatus(room);
	}

	public void gameQiangZhuang(SocketIOClient client, Object data) {
		logger.error("抢庄事件");
		JSONObject postData = JSONObject.fromObject(data);
		// 非抢庄阶段收到抢庄消息不作处理
		if (!CommonConstant.checkEvent(postData, NNConstant.NN_GAME_STATUS_QZ, client)) {
			return;
		}
		final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		if (postData.containsKey(NNConstant.DATA_KEY_VALUE)) {
			// 设置玩家抢庄状态及抢庄倍数
			if (!Dto.stringIsNULL(account) && room.getUserPacketMap().containsKey(account)
					&& room.getUserPacketMap().get(account) != null) {

				// 非抢庄、明牌抢庄抢庄消息不做处理
				if (room.getBankerType() != NNConstant.NN_BANKER_TYPE_MP
						&& room.getBankerType() != NNConstant.NN_BANKER_TYPE_QZ) {
					return;
				}
				// 不是准备状态的玩家抢庄消息不作处理(包括中途加入和已经抢过庄的)
				if (room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_READY) {
					return;
				}
				// int maxTimes =
				// getMaxTimes(room.getQzTimes(room.getPlayerMap().get(account).getScore()));
				if (postData.getInt(NNConstant.DATA_KEY_VALUE) < 0 || postData.getInt(NNConstant.DATA_KEY_VALUE) > 15) {
					logger.error("====抢庄倍数出了问题==");
					return;
				}
				// 设置为玩家抢庄状态，抢庄倍数
				room.getUserPacketMap().get(account).setStatus(NNConstant.NN_USER_STATUS_QZ);
				room.getUserPacketMap().get(account).setQzTimes(postData.getInt(NNConstant.DATA_KEY_VALUE));
				if(room.getPlayerMap().get(account).getScore()<500) {
					room.getUserPacketMap().get(account).setQzTimes(0);
				}
				if (NNConstant.NN_BANKER_TYPE_QZ == room.getBankerType()) {// 自由抢庄 设置无人庄
					room.setBanker("-1");
				}
				// 所有人都完成抢庄
				if (room.isAllQZ()) {
					logger.error("所有人完成抢庄");
					gameDingZhuang(room, account);
					JSONArray gameProcessQZ = new JSONArray();
					for (String uuid : room.getUserPacketMap().keySet()) {
						if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
							if (room.getUserPacketMap().get(uuid).getStatus() > NNConstant.NN_USER_STATUS_INIT) {
								JSONObject userQZ = new JSONObject();
								userQZ.put("account", uuid);
								userQZ.put("name", room.getPlayerMap().get(uuid).getName());
								userQZ.put("qzTimes", room.getUserPacketMap().get(uuid).getQzTimes());
								userQZ.put("banker", room.getPlayerMap().get(room.getBanker()).getName());
								gameProcessQZ.add(userQZ);
							}
						}
					}
					room.getGameProcess().put("qiangzhuang", gameProcessQZ);

				} else {
					JSONObject result = new JSONObject();
					result.put("index", room.getPlayerMap().get(account).getMyIndex());
					result.put("value", room.getUserPacketMap().get(account).getQzTimes());
					result.put("type", 0);
					CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "qiangZhuangPush_NN");
				}
			}
		}
	}

	public void gameDingZhuang(NNGameRoomNew room, String lastAccount) {
		// 非抢庄阶段不作处理
		if (room.getGameStatus() != NNConstant.NN_GAME_STATUS_QZ) {
			return;
		}
		// 所有抢庄玩家
		List<String> qzList = new ArrayList<>();
		// 所有参与玩家
		List<String> allList = new ArrayList<>();
		// 最大抢庄倍数
		int maxBs = 1;
		// 随机庄家
		if (room.getSjBanker() == 1) {
			for (String account : room.getUserPacketMap().keySet()) {
				if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
					// 中途加入除外
					if (room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_QZ) {
						// 所有有参与抢庄的玩家
						if (room.getUserPacketMap().get(account).getQzTimes() > 0) {
							qzList.add(account);
						}
						allList.add(account);
					}
				}
			}
		} else {// 最高倍数为庄家
			for (String account : room.getUserPacketMap().keySet()) {
				if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
					// 中途加入除外
					if (room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_QZ) {
						// 抢庄倍数大于最大倍数
						if (room.getUserPacketMap().get(account).getQzTimes() >= maxBs) {
							if (room.getUserPacketMap().get(account).getQzTimes() > maxBs) {
								qzList.clear();
							}
							qzList.add(account);
							maxBs = room.getUserPacketMap().get(account).getQzTimes();
						}
						allList.add(account);
					}
				}
			}
		}
		// 庄家下标
		int bankerIndex = 0;
		// 只有一个玩家抢庄
		if (qzList.size() == 1) {
			room.setBanker(qzList.get(bankerIndex));
			room.setGameStatus(NNConstant.NN_GAME_STATUS_DZ);
		} else if (qzList.size() > 1) {
			// 多个玩家抢庄
			bankerIndex = RandomUtils.nextInt(qzList.size());
			room.setBanker(qzList.get(bankerIndex));
			room.setGameStatus(NNConstant.NN_GAME_STATUS_DZ);
		} else {// 无人抢庄
			if (room.getQzNoBanker() == NNConstant.NN_QZ_NO_BANKER_SJ) {
				// 随机庄家
				bankerIndex = RandomUtils.nextInt(allList.size());
				room.setBanker(allList.get(bankerIndex));
				room.setGameStatus(NNConstant.NN_GAME_STATUS_DZ);
				for (String account : room.getUserPacketMap().keySet()) {
					// 无人抢庄 则默认所有人抢庄 需要所有人随机
					qzList.add(account);
				}
			} else if (room.getQzNoBanker() == NNConstant.NN_QZ_NO_BANKER_JS) {
				// 解散房间
				// TODO: 2018/4/18 解散房间
				// 解散房间不需要后续通知玩家庄家已经确定
				return;
			} else if (room.getQzNoBanker() == NNConstant.NN_QZ_NO_BANKER_CK) {
				logger.error("重新开");
				// 重新开局
				// 20190624 重新开局局数-1
				room.setGameIndex(room.getGameIndex() - 1);
				// 重置游戏状态
				room.setGameStatus(NNConstant.NN_GAME_STATUS_READY);
				// 初始化倒计时
				room.setTimeLeft(NNConstant.NN_TIMER_INIT);
				// 重置玩家状态
				for (String account : room.getUserPacketMap().keySet()) {
					if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
						room.getUserPacketMap().get(account).setStatus(NNConstant.NN_USER_STATUS_INIT);
					}
				}
				JSONObject result = new JSONObject();
				result.put("type", CommonConstant.SHOW_MSG_TYPE_NORMAL);
				result.put(CommonConstant.RESULT_KEY_MSG, "无人抢庄重新开局");
				CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "tipMsgPush");

				// 通知玩家
				changeGameStatus(room);
				// 重新开局不需要后续通知玩家庄家已经确定
				return;
			}
		}
		// 通知玩家
		JSONObject result = new JSONObject();
		result.put("index", room.getPlayerMap().get(lastAccount).getMyIndex());
		result.put("value", room.getUserPacketMap().get(lastAccount).getQzTimes());
		result.put("type", 1);
		result.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
		result.put("qzScore", room.getUserPacketMap().get(room.getBanker()).getQzTimes());
		result.put("gameStatus", room.getGameStatus());
		List<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < qzList.size(); i++) {
			list.add(room.getPlayerMap().get(qzList.get(i)).getMyIndex());
		}
		result.put("maxZhuang", list);
		CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "qiangZhuangPush_NN");

		// 多人抢庄才进行休眠
		int sjCount = qzList.size();
		if (room.getQzNoBanker() == NNConstant.NN_QZ_NO_BANKER_SJ && sjCount == 0) {
			sjCount = allList.size();
		}
		// 多人抢庄才进行休眠

		final int sleepTime;
		if (sjCount > 1) {
			logger.error("多人最大倍数 进行三秒定庄");
			sleepTime = NNConstant.NN_TIMER_DZ * 1000;
		} else {
			sleepTime = 0;
		}
		final String roomNo = room.getRoomNo();
		ThreadPoolHelper.executorService.submit(new Runnable() {
			@Override
			public void run() {
				gameTimerNN.gameOverTime(roomNo, NNConstant.NN_GAME_STATUS_DZ, sleepTime);
			}
		});

	}

	public void gameXiaZhu(SocketIOClient client, Object data) {

		// 非下注阶段收到下注消息不作处理
		JSONObject postData = JSONObject.fromObject(data);
		if (!CommonConstant.checkEvent(postData, NNConstant.NN_GAME_STATUS_XZ, client)) {
			return;
		}
		final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		String isTuizhu = "0";
		if (postData.containsKey("tuizhu")) {
			isTuizhu = postData.getString("tuizhu");
		}
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (postData.containsKey(NNConstant.DATA_KEY_MONEY)) {
			// 设置玩家下注状态及下注倍数
			if (!Dto.stringIsNULL(account) && room.getUserPacketMap().containsKey(account)
					&& room.getUserPacketMap().get(account) != null) {
				if (account.equals(room.getBanker())) {
					return;
				}
				if (NNConstant.NN_BANKER_TYPE_MP == room.getBankerType()
						|| NNConstant.NN_BANKER_TYPE_QZ == room.getBankerType()) {
					if (room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_QZ) {
						return;
					}
				} else if (NNConstant.NN_BANKER_TYPE_ZZ == room.getBankerType()
						|| NNConstant.NN_BANKER_TYPE_FZ == room.getBankerType()) {
					if (room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_READY) {
						return;
					}
				} else if (NNConstant.NN_BANKER_TYPE_TB == room.getBankerType()
						|| NNConstant.NN_BANKER_TYPE_GN == room.getBankerType()) {
					return;
				}
				int maxTimes = getMaxTimes(room.getBaseNumTimes(account));
				if (postData.getInt(NNConstant.DATA_KEY_MONEY) < 0
						|| postData.getInt(NNConstant.DATA_KEY_MONEY) > maxTimes) {
					return;
				}
				logger.error("玩家" + account + "下注");
				room.getUserPacketMap().get(account).setStatus(NNConstant.NN_USER_STATUS_XZ);
				room.getUserPacketMap().get(account).setXzTimes(postData.getInt(NNConstant.DATA_KEY_MONEY));
				// 通知玩家
				JSONObject result = new JSONObject();
				result.put("index", room.getPlayerMap().get(account).getMyIndex());
				result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
				result.put("value", room.getUserPacketMap().get(account).getXzTimes());
				result.put("name", room.getUserPacketMap().get(account).getXzTimes() + "倍");
				result.put("isTuiZhu", isTuizhu);
				CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "gameXiaZhuPush_NN");
				// 所有人都完成下注
				if (room.isAllXiaZhu()) {
					// 明牌抢庄已发牌 在抢庄的时候发牌
					// if (NNConstant.NN_BANKER_TYPE_MP != room.getBankerType()) {
					// shuffleAndFp(room);
					// }
					// 设置游戏状态
					room.setGameStatus(NNConstant.NN_GAME_STATUS_LP);
					room.setTimeLeft(NNConstant.NN_TIMER_SHOW);
					ThreadPoolHelper.executorService.submit(new Runnable() {
						@Override
						public void run() {
							gameTimerNN.gameOverTime(roomNo, NNConstant.NN_GAME_STATUS_LP, 0);
						}
					});

					// 存放游戏记录
					JSONArray gameProcessXZ = new JSONArray();
					for (String uuid : room.getUserPacketMap().keySet()) {
						if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
							if (room.getUserPacketMap().get(uuid).getStatus() > NNConstant.NN_USER_STATUS_INIT
									&& !room.getBanker().equals(uuid)) {
								JSONObject userXZ = new JSONObject();
								userXZ.put("account", uuid);
								userXZ.put("name", room.getPlayerMap().get(uuid).getName());
								userXZ.put("xzTimes", room.getUserPacketMap().get(uuid).getXzTimes());
								gameProcessXZ.add(userXZ);
							}
						}
					}
					room.getGameProcess().put("xiaZhu", gameProcessXZ);
					// 通知玩家
					changeGameStatus(room);
				}
			}
		}
	}

	public int getMaxTimes(JSONArray array) {
		int maxTimes = 0;
		for (int i = 0; i < array.size(); i++) {
			JSONObject baseNum = array.getJSONObject(i);
			if (baseNum.getInt("isuse") == CommonConstant.GLOBAL_YES && baseNum.getInt("val") > maxTimes) {
				maxTimes = baseNum.getInt("val");
			}
		}
		return maxTimes;
	}

	public void showPai(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		// 非亮牌阶段收到亮牌消息不作处理
		if (!CommonConstant.checkEvent(postData, NNConstant.NN_GAME_STATUS_LP, client)) {
			return;
		}
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		// 设置玩家亮牌状态
		if (!Dto.stringIsNULL(account) && room.getUserPacketMap().containsKey(account)
				&& room.getUserPacketMap().get(account) != null) {

			if (NNConstant.NN_BANKER_TYPE_NN == room.getBankerType()) {
				if (room.getBanker().equals(account)) {
					if (NNConstant.NN_USER_STATUS_READY != room.getUserPacketMap().get(account).getStatus())
						return;
				} else if (NNConstant.NN_USER_STATUS_XZ != room.getUserPacketMap().get(account).getStatus()) {
					return;
				}
			} else if (NNConstant.NN_BANKER_TYPE_QZ == room.getBankerType()
					|| NNConstant.NN_BANKER_TYPE_NN == room.getBankerType()) {
				if (room.getBanker().equals(account)) {// 庄家 处理抢庄
					if (NNConstant.NN_USER_STATUS_QZ != room.getUserPacketMap().get(account).getStatus())
						return;
				} else if (NNConstant.NN_USER_STATUS_XZ != room.getUserPacketMap().get(account).getStatus()) {
					return;
				}
			} else if (!account.equals(room.getBanker()) || room.getBankerType() == NNConstant.NN_BANKER_TYPE_TB) {
				if (room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_XZ) {
					return;
				}
			} else if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_MP) {
				if (room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_QZ) {
					return;
				}
			} else if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_ZZ
					|| room.getBankerType() == NNConstant.NN_BANKER_TYPE_FZ) {
				if (room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_READY) {
					return;
				}
			}
			// 配牛
			if (room.getBankerType() != NNConstant.NN_BANKER_TYPE_TB) {
				peiNiu(roomNo, account);
			} else {
				NNUserPacket winner = new NNUserPacket(room.getUserPacketMap().get(account).getPs(), false,
						room.getSpecialType());
				// 设置牌型
				room.getUserPacketMap().get(account).setType(winner.getType());
			}
			// 设置玩家状态
			room.getUserPacketMap().get(account).setStatus(NNConstant.NN_USER_STATUS_LP);
			// 所有人都完成亮牌
			if (!room.isAllShowPai()) {
				// 通知玩家
				JSONObject result = new JSONObject();
				result.put("index", room.getPlayerMap().get(account).getMyIndex());
				result.put("pai", room.getUserPacketMap().get(account).getSortPai());
				result.put("paiType", room.getUserPacketMap().get(account).getType());
				room.getUserPacketMap().get(account).setLP(true);
				CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "showPaiPush_NN");
			} else {
				JSONObject result = new JSONObject();
				result.put("index", room.getPlayerMap().get(account).getMyIndex());
				result.put("pai", room.getUserPacketMap().get(account).getSortPai());
				result.put("paiType", room.getUserPacketMap().get(account).getType());
				room.getUserPacketMap().get(account).setLP(true);
				CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "showPaiPush_NN");
				if (NNConstant.NN_BANKER_TYPE_NN == room.getBankerType()) {// 牛牛上庄记录本局的最大牛
					setMaxNNUser(room);
				}
				showFinish(roomNo);
			}
		}
	}

	private void setMaxNNUser(NNGameRoomNew room) {
		NNUserPacket userpacket;
		Map<String, Integer> map = new HashMap<>();
		int min = 99;
		int[] newP;
		for (String account : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_LP)
				continue;
			userpacket = new NNUserPacket(room.getUserPacketMap().get(account).getPs(), new ArrayList<Integer>());
			if (userpacket != null && userpacket.getType() == 10) {
				newP = room.getUserPacketMap().get(account).getMyPai();
				Arrays.sort(newP);
				map.put(account, newP[0]);
				if (min > newP[0])
					min = newP[0];
			}
		}
		List<String> NNUser = new ArrayList<>();
		for (String account : map.keySet()) {
			if (min == map.get(account))
				NNUser.add(account);
		}
		room.setNNUser(NNUser);
	}

	public void showFinish(final String roomNo) {
		long summaryTimes = redisInfoService.summaryTimes(roomNo, "_NN");
		if (summaryTimes > 1) {
			return;
		}
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		// 存放游戏记录
		JSONArray gameProcessLP = new JSONArray();
		for (String uuid : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
				if (room.getUserPacketMap().get(uuid).getStatus() > NNConstant.NN_USER_STATUS_INIT) {
					JSONObject userLP = new JSONObject();
					userLP.put("account", uuid);
					userLP.put("name", room.getPlayerMap().get(uuid).getName());
					userLP.put("pai", room.getUserPacketMap().get(uuid).getMingPai());
					gameProcessLP.add(userLP);
				}
			}
		}
		// 结算玩家输赢数据
		gameJieSuan(room);
		// 如果不是最后一句 正常结算 如果是最后一局 执行别的操作

		// 设置房间状态
		room.setGameStatus(NNConstant.NN_GAME_STATUS_JS);
		// 初始化倒计时room.getTimeLeft
		// room.setTimeLeft(NNConstant.NN_TIMER_INIT);
		room.setTimeLeft(8);
		room.getGameProcess().put("showPai", gameProcessLP);
		// 设置玩家状态
		for (String uuid : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
				if (room.getUserPacketMap().get(uuid).getStatus() == NNConstant.NN_USER_STATUS_LP) {
					room.getUserPacketMap().get(uuid).setStatus(NNConstant.NN_USER_STATUS_JS);
					// 顺便把用户推注设置为否

				}
			}
		}
		// 通知玩家
		changeGameStatus(room);

		if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_DK
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
			if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_JS && room.getGameIndex() == room.getGameCount()) {
				room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_FINISH);
				if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
					room.setOpen(false);
				}
				room.setGameStatus(NNConstant.NN_GAME_STATUS_ZJS);
				changeGameStatus(room);
				finalSummaryRoom(roomNo);
			}
		}
		if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_ZZ) {
			if (room.getPlayerMap().get(room.getBanker()).getScore() < room.getMinBankerScore()) {
				// 庄家设为空
				room.setBanker(null);
				// 设置游戏状态
				room.setGameStatus(NNConstant.NN_GAME_STATUS_TO_BE_BANKER);
				// 初始化倒计时
				room.setTimeLeft(NNConstant.NN_TIMER_INIT);
				// 重置玩家状态
				for (String uuid : room.getUserPacketMap().keySet()) {
					if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
						room.getUserPacketMap().get(uuid).setStatus(NNConstant.NN_USER_STATUS_INIT);
						room.getUserPacketMap().get(uuid).setTuiZhu(false);
					}
				}
			}
		}
		// 结算后 开始准备定时器
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_FREE) {
			ThreadPoolHelper.executorService.submit(new Runnable() {
				@Override
				public void run() {
					gameTimerNN.gameOverTime(roomNo, NNConstant.NN_GAME_STATUS_JS, 3000);
				}
			});
			JSONObject result = new JSONObject();
			result.put("showTimer", CommonConstant.GLOBAL_YES);
			result.put("timer", "5");
			CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "nnTimerPush_NN");
		} else if (room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
			int time = redisInfoService.getAppTimeSetting(room.getPlatform(), room.getGid(),
					"NNConstant.NN_TIMER_READY_INNING");
			if (-99 == time) {
				time = NNConstant.NN_TIMER_READY_INNING;
			}

			ThreadPoolHelper.executorService.submit(new Runnable() {

				@Override
				public void run() {
					gameTimerNN.gameOverTime(roomNo, NNConstant.NN_GAME_STATUS_JS, 3000);
				}
			});

			JSONObject result = new JSONObject();
			result.put("showTimer", CommonConstant.GLOBAL_YES);
			result.put("timer", time);
			CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "nnTimerPush_NN");
		}
	}

	public void gameJieSuan(NNGameRoomNew room) {
		if (room.getGameStatus() != NNConstant.NN_GAME_STATUS_LP) {
			return;
		}
		if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_TB) {// 通比
			niuNiuTongBi(room);
		} else if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_GN) {// 斗公牛
			niuNiuGongNiu(room);
		} else {
			// 通杀
			boolean tongSha = true;
			// 通赔
			boolean tongPei = true;
			for (String account : room.getUserPacketMap().keySet()) {
				if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
					NNUserPacket bankerUp = room.getUserPacketMap().get(room.getBanker());
					// 有参与的玩家
					if (room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_LP) {
						// 不是庄家
						if (!account.equals(room.getBanker())) {
							// 计算输赢
							NNUserPacket banker = new NNUserPacket(
									room.getUserPacketMap().get(room.getBanker()).getPs(), true, room.getSpecialType());
							NNUserPacket userpacket = new NNUserPacket(room.getUserPacketMap().get(account).getPs(),
									room.getSpecialType());
							NNUserPacket winner = NNPackerCompare.getWin(userpacket, banker);
							// 庄家抢庄倍数
							int qzTimes = room.getUserPacketMap().get(room.getBanker()).getQzTimes();
							if (qzTimes <= 0) {
								qzTimes = 1;
							}
							// 坐庄模式闲家没牛十点以下直接输
							if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_ZZ) {
								if (room.getUserPacketMap().get(account).getType() == 0) {
									NNPacker[] ps = room.getUserPacketMap().get(account).getPs();
									boolean allMin = true;
									for (NNPacker p : ps) {
										if (p.getNum().getNum() > 10) {
											allMin = false;
											break;
										}
									}
									if (allMin) {
										userpacket.setWin(false);
									}
								}
							}
							// 输赢分数 下注倍数*倍率*底注*抢庄倍数
							int xzTimes = room.getUserPacketMap().get(account).getXzTimes();
							if (xzTimes <= 0)
								xzTimes = 1;

							double totalScore = xzTimes * room.getRatio().get(winner.getType()) * room.getScore()
									* qzTimes;
							// 闲家赢
							if (userpacket.isWin()) {
								// 设置闲家当局输赢
								room.getUserPacketMap().get(account)
										.setScore(Dto.add(room.getUserPacketMap().get(account).getScore(), totalScore));
								room.getUserPacketMap().get(account).setNowScore(room.getUserPacketMap().get(account).getNowScore()+totalScore);
								// 设置庄家当局输赢
								room.getUserPacketMap().get(room.getBanker())
										.setScore(Dto.sub(bankerUp.getScore(), totalScore));
								room.getUserPacketMap().get(room.getBanker()).setNowScore(room.getUserPacketMap().get(room.getBanker()).getNowScore()-totalScore);
								// 闲家当前分数
								double oldScoreXJ = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap()
										.get(account).getScore();
								RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account)
										.setScore(Dto.add(oldScoreXJ, totalScore));
								// 庄家家当前分数
								double oldScoreZJ = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap()
										.get(room.getBanker()).getScore();
								RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(room.getBanker())
										.setScore(Dto.sub(oldScoreZJ, totalScore));
								tongSha = false;
							} else { // 庄家赢
								// 设置闲家当局输赢
								room.getUserPacketMap().get(account)
										.setScore(Dto.sub(room.getUserPacketMap().get(account).getScore(), totalScore));
								room.getUserPacketMap().get(account).setNowScore(room.getUserPacketMap().get(account).getNowScore()-totalScore);
								// 设置庄家当局输赢
								room.getUserPacketMap().get(room.getBanker())
										.setScore(Dto.add(bankerUp.getScore(), totalScore));
								room.getUserPacketMap().get(room.getBanker()).setNowScore(room.getUserPacketMap().get(room.getBanker()).getNowScore()+totalScore);
								// 闲家当前分数
								double oldScoreXJ = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap()
										.get(account).getScore();
								RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account)
										.setScore(Dto.sub(oldScoreXJ, totalScore));
								// 庄家家当前分数
								double oldScoreZJ = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap()
										.get(room.getBanker()).getScore();
								RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(room.getBanker())
										.setScore(Dto.add(oldScoreZJ, totalScore));
								tongPei = false;
							}
						}
						// 负数清零
						if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB
								|| room.getRoomType() == CommonConstant.ROOM_TYPE_JB
								|| room.getRoomType() == CommonConstant.ROOM_TYPE_FREE) {
							if (RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account)
									.getScore() < 0) {
								RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setScore(0);
							}
						}
					}
				}

			}
			// 通杀
			if (tongSha) {
				room.setTongSha(1);
			}
			// 通赔
			if (tongPei) {
				room.setTongSha(-1);
			}
		}
		// 记录玩家上局下注倍数
		updateUserLastXzTime(room.getRoomNo());
		// 房卡场结算
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_DK
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
			room.setNeedFinalSummary(true);
			roomCardSummary(room.getRoomNo());
			// 更新房卡
			updateRoomCard(room.getRoomNo());
		}
		// 更新数据库
		updateUserScore(room.getRoomNo());
		// 竞技场
		updateCompetitiveUserScore(room.getRoomNo());
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB) {
			saveUserDeduction(room.getRoomNo());
		}
		// 金币场不插入战绩
		if (room.getRoomType() != CommonConstant.ROOM_TYPE_JB
				&& room.getRoomType() != CommonConstant.ROOM_TYPE_COMPETITIVE) {
			saveGameLog(room.getRoomNo());
		}
	}

	private void updateUserLastXzTime(String roomNo) {
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		for (String account : room.getUserPacketMap().keySet()) {
			// 不为庄家且上局赢,而且未选择推注筹码才有推注筹码
			// 不是庄家 分数>0 下注分数不等于 上把的下注分数*推注
			if (!account.equals(room.getBanker()) && room.getUserPacketMap().get(account).getScore() > 0
					&& room.getUserPacketMap().get(account).getXzTimes() != room.getTuiZhuTimes()) {
				room.getUserPacketMap().get(account).setLastXzTimes(room.getUserPacketMap().get(account).getXzTimes());
				room.getUserPacketMap().get(account).setTuiZhu(true);
			} else {
				room.getUserPacketMap().get(account).setLastXzTimes(0);
			}
		}
	}

	public void roomCardSummary(String roomNo) {
		logger.error("NN总结算");
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (room == null) {
			return;
		}
		for (String account : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
				// 有参与的玩家
				if (room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_LP) {
					NNUserPacket up = room.getUserPacketMap().get(account);
					// 无牛次数+1
					if (up.getType() == 0) {
						up.setWuNiuTimes(up.getWuNiuTimes() + 1);
					}
					// 牛牛次数+1
					if (up.getType() == 10) {
						up.setNiuNiuTimes(up.getNiuNiuTimes() + 1);
					}
					// 胜利次数+1
					if (up.getScore() > 0) {
						up.setWinTimes(up.getWinTimes() + 1);
					}
					if (account.equals(room.getBanker())) {
						// 通杀次数+1
						if (room.getTongSha() == 1) {
							up.setTongShaTimes(up.getTongShaTimes() + 1);
						}
						// 通赔次数+1
						if (room.getTongSha() == -1) {
							up.setTongPeiTimes(up.getTongPeiTimes() + 1);
						}
					}
				}
			}
		}
	}

	public void updateCompetitiveUserScore(String roomNo) {
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_COMPETITIVE) {
			JSONArray array = new JSONArray();
			JSONArray userIds = new JSONArray();
			for (String uuid : room.getUserPacketMap().keySet()) {
				if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
					if (room.getUserPacketMap().get(uuid).getStatus() > NNConstant.NN_USER_STATUS_INIT) {
						room.getPlayerMap().get(uuid).setRoomCardNum(
								room.getPlayerMap().get(uuid).getRoomCardNum() - room.getSinglePayNum());
						if (room.getPlayerMap().get(uuid).getRoomCardNum() < 0) {
							room.getPlayerMap().get(uuid).setRoomCardNum(0);
						}
						JSONObject obj = new JSONObject();
						obj.put("total", 2);
						obj.put("fen", room.getUserPacketMap().get(uuid).getScore());
						obj.put("id", room.getPlayerMap().get(uuid).getId());
						array.add(obj);
						userIds.add(room.getPlayerMap().get(uuid).getId());
						JSONObject object = new JSONObject();
						object.put("userId", room.getPlayerMap().get(uuid).getId());
						object.put("score", room.getUserPacketMap().get(uuid).getScore());
						object.put("type", 2);
						producerService.sendMessage(daoQueueDestination,
								new PumpDao(DaoTypeConstant.ADD_OR_UPDATE_USER_COINS_REC, object));
					}
				}
			}
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("array", array);
			jsonObject.put("updateType", "score");
			// 更新玩家分数
			if (array.size() > 0 && userIds.size() > 0 && room.getRoomType() != CommonConstant.ROOM_TYPE_FREE
					&& room.getRoomType() != CommonConstant.ROOM_TYPE_INNING) {
				producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_SCORE, jsonObject));
				producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.PUMP,
						room.getRoomCardChangeObject(userIds, room.getSinglePayNum())));
			}
		}
	}

	private void userPumpingFee(NNGameRoomNew room) {
		if (room.isAgreeClose()) {// 全部同意解散

		} else {
			if (room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
				if (CircleConstant.CUTTYPE_BIG_WINNER.equals(room.getCutType())
						|| CircleConstant.CUTTYPE_WINNER.equals(room.getCutType())) {
					if (room.getGameIndex() != room.getGameCount()) {
						return;
					}
				}
			}
		}
		Map<String, Double> pumpInfo = new HashMap<>();
		// 取大赢家分数
		Double maxScore = 0D;
		String maxAccount = "";
		for (String account : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().get(account).getScore() >= maxScore) {
				maxScore = room.getUserPacketMap().get(account).getScore();
				maxAccount = account;
			}
		}
		/*
		 * if (room.getRoomType() == CommonConstant.ROOM_TYPE_INNING &&
		 * (CircleConstant.CUTTYPE_BIG_WINNER.equals(room.getCutType()) ||
		 * CircleConstant.CUTTYPE_WINNER.equals(room.getCutType()))) { for (String
		 * account : room.getPlayerMap().keySet()) { double sub =
		 * Dto.sub(room.getPlayerMap().get(account).getScore(),
		 * room.getPlayerMap().get(account).getSourceScore()); if (sub > maxScore) {
		 * maxScore = sub; } } } else { for (String account :
		 * room.getUserPacketMap().keySet()) { if
		 * (room.getUserPacketMap().get(account).getScore() >= maxScore) { maxScore =
		 * room.getUserPacketMap().get(account).getScore(); } } }
		 */
		for (String account : room.getPlayerMap().keySet()) {
			// 有参与的玩家
			if (room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_LP) {
				if (CircleConstant.CUTTYPE_BIG_WINNER.equals(room.getCutType())) {
					// 局数场统计所有局数输赢
					if (room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
						// 大赢家消耗
						/*
						 * if (room.getPlayerMap().get(account).getScore() -
						 * room.getPlayerMap().get(account).getSourceScore() == maxScore) {
						 */
						if (maxAccount.equals(account)) {
							RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setScore(Dto
									.sub(room.getPlayerMap().get(account).getScore(), room.getPump() * maxScore / 100));
							// 原始分数需要扣除抽水差值
							RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account)
									.setSourceScore(Dto.sub(room.getPlayerMap().get(account).getSourceScore(),
											room.getPump() * maxScore / 100));
							pumpInfo.put(String.valueOf(room.getPlayerMap().get(account).getId()),
									-room.getPump() * maxScore / 100);
						}
					} else {
						// 大赢家消耗
						if (room.getUserPacketMap().get(account).getScore() == maxScore) {
							RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setScore(Dto
									.sub(room.getPlayerMap().get(account).getScore(), room.getPump() * maxScore / 100));
							// 原始分数需要扣除抽水差值
							RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account)
									.setSourceScore(Dto.sub(room.getPlayerMap().get(account).getSourceScore(),
											room.getPump() * maxScore / 100));
							pumpInfo.put(String.valueOf(room.getPlayerMap().get(account).getId()),
									-room.getPump() * maxScore / 100);
						}
					}
				} else if (CircleConstant.CUTTYPE_WINNER.equals(room.getCutType())) {
					// 局数场统计所有局数输赢
					if (room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
						// 大赢家消耗
						//如果赢的钱大于0  则抽水
						if(room.getUserPacketMap().get(account).getScore()>0) {
							
							RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setScore(Dto
									.sub(room.getPlayerMap().get(account).getScore(), room.getPump() * room.getUserPacketMap().get(account).getScore() / 100));
							// 原始分数需要扣除抽水差值
							RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account)
									.setSourceScore(Dto.sub(room.getPlayerMap().get(account).getSourceScore(),
											room.getPump() * room.getUserPacketMap().get(account).getScore() / 100));
							pumpInfo.put(String.valueOf(room.getPlayerMap().get(account).getId()),
									-room.getPump() * room.getUserPacketMap().get(account).getScore() / 100);
						}
					} else {
						// 赢家消耗
						if (room.getUserPacketMap().get(account) != null
								&& room.getUserPacketMap().get(account).getScore() > 0) {
							RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account)
									.setScore(Dto.sub(room.getPlayerMap().get(account).getScore(),
											room.getPump() * room.getUserPacketMap().get(account).getScore() / 100));
							RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account)
									.setSourceScore(Dto.sub(room.getPlayerMap().get(account).getSourceScore(),
											room.getPump() * room.getUserPacketMap().get(account).getScore() / 100));
							pumpInfo.put(String.valueOf(room.getPlayerMap().get(account).getId()),
									-room.getPump() * room.getUserPacketMap().get(account).getScore() / 100);
						}
					}
				} else if (CircleConstant.CUTTYPE_DI.equals(room.getCutType())) {// 底注消耗
					RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setScore(Dto
							.sub(room.getPlayerMap().get(account).getScore(), room.getScore() * room.getPump() / 100));
					RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setSourceScore(Dto.sub(
							room.getPlayerMap().get(account).getSourceScore(), room.getScore() * room.getPump() / 100));
					pumpInfo.put(String.valueOf(room.getPlayerMap().get(account).getId()),
							-room.getScore() * room.getPump() / 100);
				}
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
			// 亲友圈玩家反水
			gameCircleService.circleUserPumping(object);
		}
	}

	public void updateUserScore(String roomNo) {

		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		double leaveScore = room.getLeaveScore();// 离场验证
		if (room == null)
			return;
		JSONArray array = new JSONArray();
		Map<String, Double> map = new HashMap<String, Double>();
		JSONObject pumpInfo = new JSONObject();
		for (String account : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
				// 有参与的玩家
				if (room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_LP) {
					// 元宝输赢情况
					if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB
							|| room.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
						double total = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account)
								.getScore();
						long userId = room.getPlayerMap().get(account).getId();
						double sum = room.getUserPacketMap().get(account).getNowScore();
						array.add(obtainUserScoreData(total, sum, userId));
						map.put(room.getPlayerMap().get(account).getOpenId(), sum);
					}
					// 亲友圈输赢
					else if (room.getRoomType() == CommonConstant.ROOM_TYPE_FREE
							|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
						pumpInfo.put(String.valueOf(room.getPlayerMap().get(account).getId()),
								room.getUserPacketMap().get(account).getNowScore());
					}
				}
			}
			if (CommonConstant.ROOM_TYPE_INNING == room.getRoomType()
					&& leaveScore > Dto.add(room.getPlayerMap().get(account).getScore(),
							room.getPlayerMap().get(account).getSourceScore())) {
				room.getPlayerMap().get(account).setCollapse(true);// 玩家破产
			}
		}
		if (room.getPlayerMap().keySet().size() <2) { //房间剩余人数为1 
			room.setGameIndex(room.getGameCount());
		}
		if (pumpInfo.size() > 0) {
			JSONObject object = new JSONObject();
			object.put("circleId", room.getCircleId());
			object.put("roomNo", room.getRoomNo());
			object.put("gameId", room.getGid());
			object.put("pumpInfo", pumpInfo);
			object.put("cutType", room.getCutType());
			object.put("changeType", CircleConstant.PROP_BILL_CHANGE_TYPE_GAME);
			// 亲友圈玩家输赢
			gameCircleService.circleUserPumping(object);

			// 亲友圈抽水
			userPumpingFee(room);
		}

		if (array.size() > 0) {
			// 更新玩家分数
			producerService.sendMessage(daoQueueDestination,
					new PumpDao(DaoTypeConstant.UPDATE_SCORE, room.getPumpObject(array)));
			if (room.isFund()) {
				fundEventDeal.addBalChangeRecord(map, "牛牛游戏输赢");
			}
		}

		if (room.getGameIndex() < room.getGameCount()) {
			return;
		}
		insertStatis(roomNo);
	}

	public void updateRoomCard(String roomNo) {
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		JSONArray array = new JSONArray();
		int roomCardCount = 0;
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
			for (String account : room.getUserPacketMap().keySet()) {
				if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
					// 房主支付
					if (CommonConstant.ROOM_PAY_TYPE_FANGZHU.equals(room.getPayType())) {
						if (account.equals(room.getOwner())) {
							// 参与第一局需要扣房卡
							if (room.getPlayerMap().get(account).getPlayTimes() == 1) {
								roomCardCount = room.getPlayerCount() * room.getSinglePayNum();
								array.add(room.getPlayerMap().get(account).getId());
							}
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
		if (array.size() > 0 && room.getRoomType() != CommonConstant.ROOM_TYPE_FREE
				&& room.getRoomType() != CommonConstant.ROOM_TYPE_INNING) {
			producerService.sendMessage(daoQueueDestination,
					new PumpDao(DaoTypeConstant.PUMP, room.getRoomCardChangeObject(array, roomCardCount)));
		}
	}

	public void saveUserDeduction(String roomNo) {
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (room == null) {
			return;
		}
		JSONArray userDeductionData = new JSONArray();
		for (String account : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
				if (room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_LP) {
					// 用户游戏记录
					JSONObject object = new JSONObject();
					object.put("id", room.getPlayerMap().get(account).getId());
					object.put("gid", room.getGid());
					object.put("roomNo", room.getRoomNo());
					object.put("type", room.getRoomType());
					object.put("fen", room.getUserPacketMap().get(account).getNowScore());
					object.put("old",
							Dto.sub(RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore(),
									room.getUserPacketMap().get(account).getScore()));
					if (RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore() < 0) {
						object.put("new", 0);
					} else {
						object.put("new",
								RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore());
					}
					userDeductionData.add(object);
				}
			}
		}
		// 玩家输赢记录
		producerService.sendMessage(daoQueueDestination,
				new PumpDao(DaoTypeConstant.USER_DEDUCTION, new JSONObject().element("user", userDeductionData)));
	}

	public JSONObject obtainUserScoreData(double total, double sum, long id) {
		JSONObject obj = new JSONObject();
		obj.put("id", id);
		obj.put("total", total);
		obj.put("fen", sum);
		return obj;
	}

	public void saveGameLog(String roomNo) {
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room || CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE == room.getIsClose()
				&& NNConstant.NN_GAME_STATUS_JS != room.getGameStatus()
				&& NNConstant.NN_GAME_STATUS_ZJS != room.getGameStatus())
			return;

		JSONArray gameLogResults = new JSONArray();
		JSONArray gameResult = new JSONArray();
		JSONArray array = new JSONArray();
		// 存放游戏记录
		JSONArray gameProcessJS = new JSONArray();
		for (String account : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
				// 有参与的玩家
				if (room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_LP) {
					JSONObject userJS = new JSONObject();
					userJS.put("account", account);
					userJS.put("name", room.getPlayerMap().get(account).getName());
					userJS.put("sum", room.getUserPacketMap().get(account).getScore());
					userJS.put("pai", room.getUserPacketMap().get(account).getSortPai());
					userJS.put("paiType", room.getUserPacketMap().get(account).getType());
					userJS.put("old",
							Dto.sub(RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore(),
									room.getUserPacketMap().get(account).getScore()));
					if (RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore() < 0) {
						userJS.put("new", 0);
					} else {
						userJS.put("new",
								RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore());
					}
					gameProcessJS.add(userJS);
					double total = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore();
					double sum = room.getUserPacketMap().get(account).getNowScore();
					long userId = room.getPlayerMap().get(account).getId();
					array.add(obtainUserScoreData(total, sum, userId));
					// 战绩记录
					JSONObject gameLogResult = new JSONObject();
					gameLogResult.put("account", account);
					gameLogResult.put("name", room.getPlayerMap().get(account).getName());
					gameLogResult.put("headimg", room.getPlayerMap().get(account).getHeadimg());
					if (!Dto.stringIsNULL(room.getBanker()) && room.getPlayerMap().containsKey(room.getBanker())
							&& room.getPlayerMap().get(room.getBanker()) != null) {
						gameLogResult.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
					} else {
						gameLogResult.put("zhuang", CommonConstant.NO_BANKER_INDEX);
					}
					gameLogResult.put("myIndex", room.getPlayerMap().get(account).getMyIndex());
					gameLogResult.put("myPai", room.getUserPacketMap().get(account).getMyPai());
					gameLogResult.put("mingPai", room.getUserPacketMap().get(account).getSortPai());
					gameLogResult.put("score", room.getUserPacketMap().get(account).getNowScore());
					gameLogResult.put("totalScore",
							RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore());
					gameLogResult.put("win", CommonConstant.GLOBAL_YES);
					if (room.getUserPacketMap().get(account).getScore() < 0) {
						gameLogResult.put("win", CommonConstant.GLOBAL_NO);
					}
					gameLogResult.put("zhuangTongsha", room.getTongSha());
					gameLogResults.add(gameLogResult);
					// 用户战绩
					JSONObject userResult = new JSONObject();
					userResult.put("zhuang", room.getBanker());
					userResult.put("isWinner", CommonConstant.GLOBAL_NO);
					if (room.getUserPacketMap().get(account).getScore() > 0) {
						userResult.put("isWinner", CommonConstant.GLOBAL_YES);
					}
					userResult.put("score", room.getUserPacketMap().get(account).getNowScore());
					userResult.put("totalScore",
							RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore());
					userResult.put("player", room.getPlayerMap().get(account).getName());
					userResult.put("account", account);
					gameResult.add(userResult);
				}
			}
		}
		room.getGameProcess().put("JieSuan", gameProcessJS);
		logger.info(room.getRoomNo() + "---" + String.valueOf(room.getGameProcess()));
		// 战绩信息
		JSONObject gameLogObj = room.obtainGameLog(gameLogResults.toString(), room.getGameProcess().toString());
		producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_GAME_LOG, gameLogObj));
		JSONObject o = new JSONObject();
		JSONArray userGameLogs = room.obtainUserGameLog(gameLogObj.getLong("id"), array);
		o.element("array", userGameLogs);
		o.element("object",
				new JSONObject().element("gamelog_id", gameLogObj.getLong("id"))
						.element("room_type", room.getRoomType()).element("room_id", room.getId())
						.element("room_no", room.getRoomNo()).element("game_index", room.getGameNewIndex())
						.element("result", gameResult.toString()));
		producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_USER_GAME_LOG, o));
	}

	public void exitRoom(SocketIOClient client, Object data) {
		logger.error("退出房间");
		JSONObject postData = JSONObject.fromObject(data);
		if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
			return;
		}
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		if (!Dto.stringIsNULL(account) && room.getUserPacketMap().containsKey(account)
				&& room.getUserPacketMap().get(account) != null) {
			boolean canExit = false;
			// 金币场、元宝场
			if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB || room.getRoomType() == CommonConstant.ROOM_TYPE_YB
					|| room.getRoomType() == CommonConstant.ROOM_TYPE_COMPETITIVE
					|| room.getRoomType() == CommonConstant.ROOM_TYPE_FREE) {
				// 未参与游戏可以自由退出
				if (room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_INIT) {
					canExit = true;
				} else if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_INIT
						|| room.getGameStatus() == NNConstant.NN_GAME_STATUS_READY
						|| room.getGameStatus() == NNConstant.NN_GAME_STATUS_JS) {// 初始及准备阶段可以退出
					canExit = true;
				}
			} else if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK
					|| room.getRoomType() == CommonConstant.ROOM_TYPE_DK
					|| room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB
					|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
				if (room.getPlayerMap().get(account).getPlayTimes() == 0) {
					if (CommonConstant.ROOM_PAY_TYPE_AA.equals(room.getPayType()) || !room.getOwner().equals(account)
							|| room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
						canExit = true;
					} else if (room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
						canExit = true;
					}
				}
				if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_ZJS) {
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
				room.getVisitPlayerMap().remove(account); // 观战人员直接退出
				room.getUserPacketMap().remove(account);
				roomInfo.put("player_number", room.getPlayerMap().size());
				producerService.sendMessage(daoQueueDestination,
						new PumpDao(DaoTypeConstant.UPDATE_QUIT_USER_ROOM, roomInfo));
// 退出房间更新数据 end
				// 组织数据，通知玩家
				JSONObject result = new JSONObject();
				result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
				result.put("type", 1);
				result.put("index", player.getMyIndex());
				if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_READY
						&& room.getNowReadyCount() < NNConstant.NN_MIN_START_COUNT) {
					// 重置房间倒计时
					room.setTimeLeft(NNConstant.NN_TIMER_INIT);
				}
				if (room.getTimeLeft() > 0) {
					result.put("showTimer", CommonConstant.GLOBAL_YES);
				} else {
					result.put("showTimer", CommonConstant.GLOBAL_NO);
				}
				result.put("timer", room.getTimeLeft());
				result.put("startIndex", getStartIndex(roomNo));
				if (!postData.containsKey("notSend")) {
					CommonConstant.sendMsgEventToAll(allUUIDList, result.toString(), "exitRoomPush_NN");
				}
				if (postData.containsKey("notSendToMe")) {
					CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "exitRoomPush_NN");
				}
				// 坐庄模式
				if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_ZZ) {
					// 房主退出且房间内有其他玩家
					if (account.equals(room.getBanker()) && room.getUserPacketMap().size() > 0) {
						// 庄家设为空
						room.setBanker(null);
						// 设置游戏状态
						room.setGameStatus(NNConstant.NN_GAME_STATUS_TO_BE_BANKER);
						// 初始化倒计时
						room.setTimeLeft(NNConstant.NN_TIMER_INIT);
						// 重置玩家状态
						for (String uuid : room.getUserPacketMap().keySet()) {
							if (room.getUserPacketMap().containsKey(uuid)
									&& room.getUserPacketMap().get(uuid) != null) {
								room.getUserPacketMap().get(uuid).setStatus(NNConstant.NN_USER_STATUS_INIT);
							}
						}
						changeGameStatus(room);
						return;
					}
				}
				int minStartCount = NNConstant.NN_MIN_START_COUNT;
				if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK
						|| room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB
						|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
					if (!Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("mustFull")) {
						minStartCount = room.getPlayerCount();
					}
				}
				// 房间内所有玩家都已经完成准备且人数大于两人通知开始游戏
				if (room.getPlayerMap().size() <= room.getPlayerCount() && room.isAllReady()
						&& room.getPlayerMap().size() >= minStartCount) {
					startGame(room);
				}
				// 所有人都退出清除房间数据
				logger.error("NN type:" + room.getRoomType());
				if (room.getPlayerMap().size() == 0 && room.getRoomType() == CommonConstant.ROOM_TYPE_FK) {
					redisInfoService.delSummary(roomNo, "_NN");
					if (CommonConstant.ROOM_TYPE_FREE == room.getRoomType()
							|| CommonConstant.ROOM_TYPE_INNING == room.getRoomType()) {
						roomInfo.put("status", 0);
					} else {
						roomInfo.put("status", room.getIsClose());
					}
					roomInfo.put("game_index", 0);
					// 亲友圈房间 复制房间 初始化
					if (CommonConstant.ROOM_TYPE_FREE == room.getRoomType()
							|| CommonConstant.ROOM_TYPE_INNING == room.getRoomType()) {
						copyGameRoom(roomNo, null);
					}
					// RoomManage.gameRoomMap.remove(roomNo);
				}
				// 机器人退出
				if (room.isRobot() && room.getRobotList().contains(account)) {
					AppBeanUtil.getBean(RobotEventDeal.class).robotExit(account);
				}
			} else {
				// 组织数据，通知玩家
				JSONObject result = new JSONObject();
				result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
				result.put(CommonConstant.RESULT_KEY_MSG, "游戏中无法退出");
				result.put("showTimer", CommonConstant.GLOBAL_YES);
				result.put("timer", room.getTimeLeft());
				result.put("type", 1);
				CommonConstant.sendMsgEventToSingle(client, result.toString(), "exitRoomPush_NN");
			}
		}
	}

	private void copyGameRoom(String roomNo, Playerinfo playerinfo) {
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		room.initGame();
		room.setGameIndex(0);
		room.setGameNewIndex(0);
		room.getSummaryData().clear();
		room.setGameStatus(NNConstant.NN_USER_STATUS_INIT);
		room.setJieSanTime(0);
		room.setTimeLeft(0);
		room.setFinalSummaryData(new JSONArray());
		room.setUserPacketMap(new ConcurrentHashMap<>());
		room.setPlayerMap(new ConcurrentHashMap<>());
		room.setVisitPlayerMap(new ConcurrentHashMap<>());
		room.setOpen(true);
		for (int i = 0; i < room.getUserIdList().size(); i++) {
			room.getUserIdList().set(i, 0L);
			room.addIndexList(i);
		}
//处理清空房间，重新生成
		baseEventDeal.reloadClearRoom(roomNo);

	}

	public void changeGameStatus(NNGameRoomNew room) {
		for (String account : room.getPlayerMap().keySet()) {
			if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
				JSONObject obj = new JSONObject();
				obj.put("tuizhu", room.getTuiZhuTimes());
				obj.put("room_no", room.getRoomNo());
				obj.put("gameStatus", room.getGameStatus());
				obj.put("gameBankerType", room.getBankerType());
				String wanfa = "";
				switch (room.getBankerType()) {
				case 10:
					wanfa = "霸王庄";
					break;
				case 50:
					// 自由抢庄
					wanfa = "自由抢庄";
					break;
				case 30:
					wanfa = "明牌抢庄";
					break;
				default:
					break;
				}
				obj.put("roominfo", wanfa);
				// 无庄家
				if (Dto.stringIsNULL(room.getBanker())
						|| (room.getGameStatus() <= NNConstant.NN_GAME_STATUS_DZ
								&& room.getBankerType() != NNConstant.NN_BANKER_TYPE_ZZ)
						|| NNConstant.NN_BANKER_TYPE_TB == room.getBankerType()
						|| NNConstant.NN_BANKER_TYPE_GN == room.getBankerType()
						|| room.getPlayerMap().get(room.getBanker()) == null) {
					obj.put("zhuang", CommonConstant.NO_BANKER_INDEX);
					obj.put("qzScore", 0);
				} else {
					obj.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
					obj.put("qzScore", room.getUserPacketMap().get(room.getBanker()).getQzTimes());
				}
				obj.put("game_index", room.getGameIndex());
				obj.put("game_count", room.getGameCount());
				obj.put("showTimer", CommonConstant.GLOBAL_YES);
				if (room.getTimeLeft() == NNConstant.NN_TIMER_INIT) {
					obj.put("showTimer", CommonConstant.GLOBAL_NO);
				}
				// 倒计时
				obj.put("timer", room.getTimeLeft());
				obj.put("qzTimes", room.getQzTimes(
						RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore()));
				obj.put("baseNum", room.getBaseNumTimes(account));
				obj.put("users", room.getAllPlayer());
				obj.put("gameData", room.getGameData(account));

				if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK
						|| room.getRoomType() == CommonConstant.ROOM_TYPE_DK
						|| room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB
						|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
					if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_ZJS) {

						obj.put("jiesuanData", room.getFinalSummary());
					}
					if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_JS
							&& room.getGameIndex() == room.getGameCount()) {
						obj.put("jiesuanData", room.getFinalSummary());
					}
				}

				obj.put("isBanker", CommonConstant.GLOBAL_NO);
				if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_ZZ) {
					if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_TO_BE_BANKER
							|| (room.getGameStatus() == NNConstant.NN_GAME_STATUS_JS && room.getPlayerMap()
									.get(room.getBanker()).getScore() < room.getMinBankerScore())) {
						obj.put("isBanker", CommonConstant.GLOBAL_YES);
						obj.put("bankerMinScore", room.getMinBankerScore());
						obj.put("bankerIsUse", CommonConstant.GLOBAL_YES);
						if (room.getPlayerMap().get(account).getScore() < room.getMinBankerScore()) {
							obj.put("bankerIsUse", CommonConstant.GLOBAL_NO);
						}
					}
				}
				UUID uuid = room.getPlayerMap().get(account).getUuid();
				if (uuid != null) {
					logger.error("返回客户端事件");
					CommonConstant.sendMsgEventToSingle(uuid, obj.toString(), "changeGameStatusPush_NN");
				}
			}
		}
		if (room.isRobot()) {
			for (String robotAccount : room.getRobotList()) {
				int delayTime = RandomUtils.nextInt(3) + 2;
				if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_JS
						|| room.getGameStatus() == NNConstant.NN_GAME_STATUS_READY) {
					AppBeanUtil.getBean(RobotEventDeal.class).changeRobotActionDetail(robotAccount,
							NNConstant.NN_GAME_EVENT_READY, delayTime);
				}
				if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_QZ) {
					AppBeanUtil.getBean(RobotEventDeal.class).changeRobotActionDetail(robotAccount,
							NNConstant.NN_GAME_EVENT_QZ, delayTime);
				}
				if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_XZ) {
					AppBeanUtil.getBean(RobotEventDeal.class).changeRobotActionDetail(robotAccount,
							NNConstant.NN_GAME_EVENT_XZ, delayTime);
				}
				if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_LP) {
					AppBeanUtil.getBean(RobotEventDeal.class).changeRobotActionDetail(robotAccount,
							NNConstant.NN_GAME_EVENT_LP, delayTime);
				}
			}
		}
	}

	public void closeRoom(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
			return;
		}
		final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		final NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
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
						gameTimerNN.closeRoomOverTime(roomNo, room.getJieSanTime());
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
				String[] names = { room.getPlayerMap().get(account).getName() };
				result.put("names", names);
				CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush_NN");
				return;
			}
			if (type == CommonConstant.CLOSE_ROOM_AGREE) {
				// 全部同意解散
				if (room.isAgreeClose()) {
					// 未玩完一局不需要强制结算
					if (!room.isNeedFinalSummary()) {
						// 所有玩家
						List<UUID> uuidList = room.getAllUUIDList();
						copyGameRoom(roomNo, null);
						// 移除房间
						RoomManage.gameRoomMap.remove(roomNo);
						// 通知玩家
						result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
						result.put(CommonConstant.RESULT_KEY_MSG, "房间已被解散");
						CommonConstant.sendMsgEventToAll(uuidList, result.toString(), "tipMsgPush");
						return;
					}
					userPumpingFee(room);// 亲友圈抽水
					if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
						room.setOpen(false);
					}
					room.setGameStatus(NNConstant.NN_GAME_STATUS_ZJS);
					changeGameStatus(room);
				} else {// 刷新数据
					room.getPlayerMap().get(account).setIsCloseRoom(CommonConstant.CLOSE_ROOM_AGREE);
					result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
					result.put("data", room.getJieSanData());
					CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush_NN");
				}
			}
		}
	}

	public void reconnectGame(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!postData.containsKey(CommonConstant.DATA_KEY_ROOM_NO)
				|| !postData.containsKey(CommonConstant.DATA_KEY_ACCOUNT) || !postData.containsKey("uuid")) {
			return;
		}
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		JSONObject userInfo = userBiz.getUserByAccount(account);
		// uuid不匹配
		if (!userInfo.containsKey("uuid") || Dto.stringIsNULL(userInfo.getString("uuid"))
				|| !userInfo.getString("uuid").equals(postData.getString("uuid"))) {
			return;
		}
		JSONObject result = new JSONObject();
		if (client == null) {
			return;
		}
		// 房间不存在
		if (!RoomManage.gameRoomMap.containsKey(roomNo) || RoomManage.gameRoomMap.get(roomNo) == null) {
			result.put("type", 0);
			CommonConstant.sendMsgEventToSingle(client, result.toString(), "reconnectGamePush_NN");
			return;
		}
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		// 不在当前房间内
		if (Dto.stringIsNULL(account) || !room.getPlayerMap().containsKey(account)
				|| room.getPlayerMap().get(account) == null) {
			result.put("type", 0);
			CommonConstant.sendMsgEventToSingle(client, result.toString(), "reconnectGamePush_NN");
			return;
		}
		// 刷新uuid
		room.getPlayerMap().get(account).setUuid(client.getSessionId());
		// 组织数据，通知玩家
		result.put("type", 1);
		result.put("data", obtainRoomData(account, roomNo));
		// 通知玩家
		CommonConstant.sendMsgEventToSingle(client, result.toString(), "reconnectGamePush_NN");
	}

	public void peiNiu(String roomNo, String uuid) {
		NNGameRoomNew room = ((NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo));
		NNUserPacket packet = room.getUserPacketMap().get(uuid);
		if (uuid.equals(room.getBanker())) {
			NNUserPacket zhuang = new NNUserPacket(room.getUserPacketMap().get(uuid).getPs(), true,
					room.getSpecialType());
			packet.setType(zhuang.getType());
			packet.setWin(zhuang.isWin());
		} else {
			NNPacker[] ups = room.getUserPacketMap().get(uuid).getPs();
			// 有发牌的玩家
			if (ups != null && ups.length > 0 && ups[0] != null) {
				NNUserPacket zhuang = new NNUserPacket(room.getUserPacketMap().get(room.getBanker()).getPs(), true,
						room.getSpecialType());
				NNUserPacket userpacket = new NNUserPacket(ups, room.getSpecialType());
				NNPackerCompare.getWin(userpacket, zhuang);
				packet.setType(userpacket.getType());
				packet.setWin(userpacket.isWin());
			}
		}
	}

	private void niuNiuTongBi(NNGameRoomNew room) {

		List<String> list = new ArrayList<>();
		for (String account : room.getUserPacketMap().keySet()) {
			// 用户不存在或者 不处于亮牌
			if (room.getUserPacketMap().get(account) == null || room.getPlayerMap().get(account) == null
					|| room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_LP)
				continue;
			list.add(account);
		}
		NNUserPacket bankerPacket;// 庄
		NNUserPacket playerPacket;// 闲
		double totalScore;
		double di = room.getScore();// 底分
		Double winRatio;// 赔率
		for (int i = 0; i < list.size(); i++) {
			totalScore = 0D;
			bankerPacket = new NNUserPacket(room.getUserPacketMap().get(list.get(i)).getPs(), true,
					room.getSpecialType());
			for (int j = 0; j < list.size(); j++) {
				if (list.get(i).equals(list.get(j)))
					continue;
				playerPacket = new NNUserPacket(room.getUserPacketMap().get(list.get(j)).getPs(),
						room.getSpecialType());
				// 计算玩家输赢
				NNPackerCompare.getWin(bankerPacket, playerPacket);

				if (bankerPacket.isWin()) // 庄赢
					winRatio = (double) room.getRatio().get(bankerPacket.getType());
				else
					winRatio = -(double) room.getRatio().get(playerPacket.getType());
				if (winRatio == null || 0D == winRatio)
					winRatio = 1D;
				totalScore = Dto.add(totalScore, Dto.mul(di, winRatio));
			}
			// 更改玩家分数
			room.getUserPacketMap().get(list.get(i)).setScore(Dto.add(bankerPacket.getScore(), totalScore));
			room.getPlayerMap().get(list.get(i))
					.setScore(Dto.add(room.getPlayerMap().get(list.get(i)).getScore(), totalScore));
			if (totalScore > 0D)
				bankerPacket.setWin(true);
			else
				bankerPacket.setWin(false);
		}

	}

	private void niuNiuGongNiu(NNGameRoomNew room) {

		String winUUID = null;
		for (String win : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().containsKey(win) && room.getUserPacketMap().get(win) != null) {
				if (room.getUserPacketMap().get(win).getStatus() == NNConstant.NN_USER_STATUS_LP) {
					NNUserPacket winPacket = room.getUserPacketMap().get(win);
					NNUserPacket winner = new NNUserPacket(winPacket.getPs(), true, room.getSpecialType());
					// 设置牌型
					winPacket.setType(winner.getType());

					boolean isWin = true;
					for (String uuid : room.getUserPacketMap().keySet()) {
						if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
							if (!uuid.equals(win)
									&& room.getUserPacketMap().get(uuid).getStatus() == NNConstant.NN_USER_STATUS_LP) {
								NNUserPacket userpacket = new NNUserPacket(room.getUserPacketMap().get(uuid).getPs(),
										room.getSpecialType());
								// 计算玩家输赢
								NNPackerCompare.getWin(winner, userpacket);
								// 输给其他玩家
								if (!winner.isWin()) {
									isWin = false;
									break;
								}
							}
						}
					}
					// 通杀
					if (isWin) {
						double totalScore = 0D;
						for (String account : room.getUserPacketMap().keySet()) {
							if (room.getUserPacketMap().containsKey(account)
									&& room.getUserPacketMap().get(account) != null) {
								if (!account.equals(win) && room.getUserPacketMap().get(account)
										.getStatus() == NNConstant.NN_USER_STATUS_LP) {
									totalScore = Dto.add(totalScore, room.getScore());
								}
							}
						}
						// 设置当局输赢
						room.getUserPacketMap().get(win).setScore(totalScore);
						// 设置当前分数
						double oldScore = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(win)
								.getScore();
						RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(win)
								.setScore(Dto.add(oldScore, totalScore));
						winUUID = win;
						winPacket.setWin(true);
					} else {
						// 设置当局输赢
						room.getUserPacketMap().get(win).setScore(-room.getScore());
						// 设置当前分数
						double oldScore = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(win)
								.getScore();
						RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(win)
								.setScore(Dto.sub(oldScore, room.getScore()));
						winPacket.setWin(false);
					}
				}
			}
		}
		if (!Dto.stringIsNULL(winUUID) && room.getUserPacketMap().containsKey(winUUID)
				&& room.getUserPacketMap().get(winUUID) != null) {
			int winType = room.getUserPacketMap().get(winUUID).getType();
			if (room.getRatio().containsKey(winType) && room.getRatio().get(winType) != null) {
				// 赢家赔率
				int winRatio = room.getRatio().get(winType);
				if (winRatio > 1) {
					for (String account : room.getUserPacketMap().keySet()) {
						// 需要额外增加的分数
						double extraScore = room.getUserPacketMap().get(account).getScore() * (winRatio - 1);
						// 更改玩家分数
						room.getUserPacketMap().get(account)
								.setScore(Dto.add(room.getUserPacketMap().get(account).getScore(), extraScore));
						room.getPlayerMap().get(account)
								.setScore(Dto.add(room.getPlayerMap().get(account).getScore(), extraScore));
					}
				}
			}
		}
	}

	private int getStartIndex(String roomNo) {
		if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
			NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
			// 房卡场或俱乐部
			if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB
					|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
				// 房主在房间内返回房主的下标
				if (!Dto.stringIsNULL(room.getOwner()) && room.getPlayerMap().containsKey(room.getOwner())
						&& room.getPlayerMap().get(room.getOwner()) != null) {
					return room.getPlayerMap().get(room.getOwner()).getMyIndex();
				}
			}
		}
		return CommonConstant.NO_BANKER_INDEX;
	}

	private void removeRoom(String roomNo) {
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		// 房间解散
		redisInfoService.delSummary(roomNo, "_NN");
		JSONObject roomInfo = new JSONObject();
		roomInfo.put("room_no", room.getRoomNo());
		roomInfo.put("status", CommonConstant.CLOSE_ROOM_TYPE_OVER);
		roomInfo.put("game_index", 0);
		producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
		logger.error("清除数组信息");
		RoomManage.gameRoomMap.remove(roomNo);
	}

	public void forcedRoom(Object data) {
		logger.error("牛牛解散2");
		JSONObject postData = JSONObject.fromObject(data);
		if (!postData.containsKey(CommonConstant.DATA_KEY_ROOM_NO))
			return;
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		JSONObject object = new JSONObject();
		object.put("type", CommonEventConstant.TIP_MSG_PUSH_TYPE_CPM);
		object.put(CommonConstant.RESULT_KEY_MSG, "管理员解散房间");
		logger.error("返回客户端");
		CommonConstant.sendMsgEventAll(room.getAllUUIDList(), object, CommonEventConstant.TIP_MSG_PUSH);
		room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE);
		ConcurrentHashMap<String, Playerinfo> map = room.getPlayerMap();
		for (String account : room.getUserPacketMap().keySet()) {
			if (map.containsKey(account)) {
				map.get(account).setIsCloseRoom(CommonConstant.CLOSE_ROOM_AGREE);
			} else {
			}
		}
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
			room.setOpen(false);
		}
		room.setGameStatus(0);

		// 更新数据库
		updateUserScore(room.getRoomNo());

		changeGameStatus(room);
		// 房间处理
		removeRoom(roomNo);

	}

	public void compulsorySettlement(String roomNo) {
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (room == null)
			return;
		room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE);
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
			room.setOpen(false);
		}
		room.setGameStatus(NNConstant.NN_GAME_STATUS_ZJS);
		// 更新数据库
		updateUserScore(room.getRoomNo());
		changeGameStatus(room);
		room.setNeedFinalSummary(true);
		finalSummaryRoom(room.getRoomNo());// 总结算房间处理
	}

	private void finalSummaryRoom(String roomNo) {
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room) {
			return;
		}

		int roomType = room.getRoomType();
		// 总结算 房间处理
		if (room.isNeedFinalSummary()) {
			// 最后一局 或解散房间
			if (room.getGameIndex() >= room.getGameCount()
					|| CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE == room.getIsClose()
					|| CommonConstant.CLOSE_ROOM_TYPE_FINISH == room.getIsClose()
					|| CommonConstant.CLOSE_ROOM_TYPE_OVER == room.getIsClose()) {
				if (CommonConstant.ROOM_TYPE_FK == roomType || CommonConstant.ROOM_TYPE_TEA == roomType) {// 房卡场 茶楼
					removeRoom(roomNo);// 移除房间
					return;
				} else if (CommonConstant.ROOM_TYPE_INNING == roomType) {// 局数场
					copyGameRoom(roomNo, null);// 处理清空房间，重新生成
					return;
				}
			}
		}
	}

	public void insertStatis(String roomNo) {
		NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room) {
			return;
		}

		double maxScore = 0D;// 大赢家

		for (String account : room.getUserPacketMap().keySet()) {
			double sub = room.getUserPacketMap().get(account).getScore();
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
		for (String account : room.getUserPacketMap().keySet()) {
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			NNUserPacket up = room.getUserPacketMap().get(account);
			if (null == playerinfo || null == up)
				continue;
			// 有参与的玩家
			if (!playerinfo.isCollapse() && SSSConstant.SSS_USER_STATUS_INIT == up.getStatus())
				continue;
			JSONObject jsonObject = new JSONObject();
			int isBigWinner = 0;
			if (maxScore == room.getUserPacketMap().get(account).getScore() && maxScore > 0D) {
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
				jsonObject.element("score", room.getUserPacketMap().get(account).getScore());
			} else {
				jsonObject.element("score", Dto.add(room.getUserPacketMap().get(account).getScore(),
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
}
