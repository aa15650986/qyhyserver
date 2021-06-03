package com.zhuoan.biz.event.sss;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.sss.SSSComputeCards;
import com.zhuoan.biz.core.sss.SSSOrdinaryCards;
import com.zhuoan.biz.core.sss.SSSSpecialCardSort;
import com.zhuoan.biz.event.BaseEventDeal;
import com.zhuoan.biz.event.FundEventDeal;
import com.zhuoan.biz.game.biz.GameCircleService;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.game.biz.ZaNewSignBiz;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.sss.SSSGameRoomNew;
import com.zhuoan.biz.model.sss.SSSUserPacket;
import com.zhuoan.biz.robot.RobotEventDeal;
import com.zhuoan.constant.*;
import com.zhuoan.constant.event.CommonEventConstant;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.SSSService;
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

@Component
public class SSSGameEventDealNew {

	private final static Logger log = LoggerFactory.getLogger(SSSGameEventDealNew.class);
	@Resource
	private RoomBiz roomBiz;
	@Resource
	private GameTimerSSS gameTimerSSS;
	@Resource
	private Destination daoQueueDestination;
	@Resource
	private ProducerService producerService;
	@Resource
	private RedisInfoService redisInfoService;
	@Resource
	private UserBiz userBiz;
	@Resource
	private TeaService teaService;
	@Resource
	private FundEventDeal fundEventDeal;
	@Resource
	private GameCircleService gameCircleService;
	@Resource
	private BaseEventDeal baseEventDeal;
	@Resource
	private SSSService sssService;
	@Resource
	private ZaNewSignBiz zaNewSignBiz;

	public void createRoom(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		JSONObject roomData = obtainRoomData(roomNo, account);
		// 数据不为空
		if (!Dto.isObjNull(roomData)) {
			JSONObject result = new JSONObject();
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			result.put("data", roomData);
			// 通知自己
			CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_SSS");
		}
	}

	public void joinRoom(SocketIOClient client, Object data) {
		System.out.println("=================加入房间================");
		JSONObject joinData = JSONObject.fromObject(data);
		final String account = joinData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		final String roomNo = joinData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		int isReconnect = CommonConstant.GLOBAL_NO;
		if (joinData.containsKey("isReconnect")) {
			isReconnect = joinData.getInt("isReconnect");
		}

		int roomType = room.getRoomType();
		// 茶楼 处理
		if (CommonConstant.ROOM_TYPE_TEA == roomType && room.getGameIndex() == 0) {
			if (CommonConstant.GLOBAL_NO == isReconnect) {
				gameReady(client, data);// 玩家准备
			}
			return;
		}

		// 进入房间通知自己
		createRoom(client, data);

		// 非重连通知其他玩家
		if (CommonConstant.GLOBAL_NO == isReconnect) {
			SSSUserPacket up = room.getUserPacketMap().get(account);
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (null == up || null == playerinfo)
				return;
			JSONObject obj = new JSONObject();
			obj.put("account", playerinfo.getAccount());
			obj.put("name", playerinfo.getName());
			obj.put("headimg", playerinfo.getRealHeadimg());
			obj.put("sex", playerinfo.getSex());
			obj.put("ip", playerinfo.getIp());
			obj.put("vip", playerinfo.getVip());
			obj.put("location", playerinfo.getLocation());
			obj.put("area", playerinfo.getArea());
			obj.put("score", playerinfo.getScore() + playerinfo.getSourceScore());// 玩家分数
			obj.put("index", playerinfo.getMyIndex());
			obj.put("userOnlineStatus", playerinfo.getStatus());
			obj.put("ghName", playerinfo.getGhName());
			obj.put("introduction", playerinfo.getSignature());
			obj.put("userStatus", room.getUserPacketMap().get(account).getStatus());
			obj.put("userTimeLeft", room.getUserPacketMap().get(account).getTimeLeft());
			// 通知玩家
			CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), obj.toString(), "playerEnterPush_SSS");
		}
	}

	public void gameStart(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		// 不满足准备条件直接忽略
		if (!CommonConstant.checkEvent(postData, SSSConstant.SSS_GAME_STATUS_INIT, client)
				&& !CommonConstant.checkEvent(postData, SSSConstant.SSS_GAME_STATUS_READY, client)) {
			return;
		}
		// 房间号
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		// 玩家账号
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		int type = postData.getInt("type");
		String eventName = "gameStartPush_SSS";
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

		int readyCount = room.getUserPacketMap().get(account).getStatus() == SSSConstant.SSS_USER_STATUS_READY
				? room.getNowReadyCount()
				: room.getNowReadyCount() + 1;
		// 实时准备人数不足
		if (readyCount < room.getMinPlayer()) {
			sendStartResultToSingle(client, eventName, CommonConstant.GLOBAL_NO, "当前准备人数不足");
			return;
		}
		// 需要提前退出的人
		List<String> outList = new ArrayList<>();
		for (String player : room.getUserPacketMap().keySet()) {
			// 不是房主且未准备
			if (!account.equals(player)
					&& room.getUserPacketMap().get(player).getStatus() != SSSConstant.SSS_USER_STATUS_READY) {
				outList.add(player);
			}
		}

		if (CommonConstant.IS_ALL_START_Y.equals(room.getIsAllStart())) {// 满人开
			if (room.getUserPacketMap().size() != room.getPlayerCount()) {
				sendStartResultToSingle(client, eventName, CommonConstant.GLOBAL_NO, "必须人满才能开始");
				return;
			}
			if (readyCount != room.getPlayerCount()) {
				sendStartResultToSingle(client, eventName, CommonConstant.GLOBAL_NO, "需要全部准备才能开始");
				return;
			}
		}
		// 第一次点开始游戏有人需要退出
		if (type == SSSConstant.START_GAME_TYPE_UNSURE && outList.size() > 0) {
			sendStartResultToSingle(client, eventName, CommonConstant.GLOBAL_YES, "是否开始");
			return;
		}
		if (!Dto.isObjNull(room.getSetting())) {
			room.getSetting().remove("mustFull");
		}
		// 房主未准备直接准备
		if (room.getUserPacketMap().get(account).getStatus() != SSSConstant.SSS_USER_STATUS_READY) {
			gameReady(client, data);
		}
		if (outList.size() > 0) {
			// 退出房间
			for (String player : outList) {
				if (room.getUserPacketMap().get(player).getStatus() != SSSConstant.SSS_USER_STATUS_READY) {
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
					result.put(CommonConstant.RESULT_KEY_MSG, "已被房主踢出");
					CommonConstant.sendMsgEventToSingle(playerClient, result.toString(), "tipMsgPush");
				}

			}
		} else {
			startGame(room.getRoomNo());
		}
	}

	private void sendStartResultToSingle(SocketIOClient client, String eventName, int code, String msg) {
		JSONObject result = new JSONObject();
		result.put(CommonConstant.RESULT_KEY_CODE, code);
		result.put(CommonConstant.RESULT_KEY_MSG, msg);
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), eventName);
	}

	public void gameReady(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		// 不满足准备条件直接忽略/
		if (!CommonConstant.checkEvent(postData, SSSConstant.SSS_GAME_STATUS_INIT, client)
				&& !CommonConstant.checkEvent(postData, SSSConstant.SSS_GAME_STATUS_READY, client)
				&& !CommonConstant.checkEvent(postData, SSSConstant.SSS_GAME_STATUS_SUMMARY, client)) {
			return;
		}
		// 房间号
		final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		SSSGameRoomNew room = null;
		room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		int roomType = room.getRoomType();
		// 玩家账号
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		SSSUserPacket up = room.getUserPacketMap().get(account);
		Playerinfo playerinfo = room.getPlayerMap().get(account);
		List<String> listsss = new ArrayList<String>();
		try {
			JSONArray jsonArray = postData.getJSONArray("sss");
			listsss = JSONArray.toList(jsonArray);
		} catch (Exception e) {
		}
		if (listsss.size() > 0) {
			System.out.println("接收到选的牌为：" + listsss);
			playerinfo.setSss(listsss);
			ConcurrentHashMap<String, Playerinfo> playerMap = room.getPlayerMap();
			playerMap.put(account, playerinfo);
			room.setPlayerMap(playerMap);
			RoomManage.gameRoomMap.put(roomNo, room);

		}
		if (null == up || null == playerinfo || CommonConstant.ROOM_VISIT_INDEX == playerinfo.getMyIndex())
			return;
		if (SSSConstant.SSS_USER_STATUS_READY == up.getStatus()) {
			JSONObject result = new JSONObject();
			result.put("type", CommonConstant.SHOW_MSG_TYPE_SMALL);
			result.put(CommonConstant.RESULT_KEY_MSG, "您已准备");
			CommonConstant.sendMsgEventToSingle(client, result.toString(), "tipMsgPush");
			return;
		}

		// 元宝不足无法准备
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB || room.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
			if (Dto.sub(room.getPlayerMap().get(account).getScore(),
					room.getPlayerMap().get(account).getSourceScore()) < room.getLeaveScore()) {
				postData.put("notSend", CommonConstant.GLOBAL_YES);
				postData.put("notSendToMe", CommonConstant.GLOBAL_YES);
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
					result.put(CommonConstant.RESULT_KEY_MSG, "能量值不足");
				}
				CommonConstant.sendMsgEventToSingle(client, result.toString(), "tipMsgPush");
				return;
			}
		} // 亲友圈
		else if (CommonConstant.ROOM_TYPE_INNING == room.getRoomType()) {
			if (playerinfo.isCollapse()) {
				JSONObject result = new JSONObject();
				result.put("type", CommonEventConstant.TIP_MSG_PUSH_TYPE_CPM);
				result.put(CommonConstant.RESULT_KEY_MSG, "您已破产");
				CommonConstant.sendMsgEventToSingle(client, result.toString(), CommonEventConstant.TIP_MSG_PUSH);
				return;
			}
			// 自由场 房费AA
		} else if (room.getRoomType() == CommonConstant.ROOM_TYPE_FREE
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
				return;
			}
		}

		// 设置玩家准备状态
		up.setStatus(SSSConstant.SSS_USER_STATUS_READY);
		// 设置房间准备状态
		if (room.getGameStatus() != SSSConstant.SSS_GAME_STATUS_READY) {
			room.setGameStatus(SSSConstant.SSS_GAME_STATUS_READY);
		}

		int minStartPlayer = room.getMinPlayer();
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
			if (CommonConstant.IS_ALL_START_Y.equals(room.getIsAllStart())) {// 满人开始
				minStartPlayer = room.getPlayerCount();
			}
		} else if (room.getRoomType() == CommonConstant.ROOM_TYPE_TEA) {// 茶楼满人开始
			minStartPlayer = room.getPlayerCount();
		}
		// 房间内所有玩家都已经完成准备且人数大于最低开始人数通知开始游戏,否则通知玩家准备

		if (room.getPlayerMap().size() <= room.getPlayerCount() && room.isAllReady()
				&& room.getUserPacketMap().size() >= minStartPlayer) {
			// 亲友圈扣除房卡
			if (CommonConstant.ROOM_TYPE_FREE == room.getRoomType()
					|| CommonConstant.ROOM_TYPE_INNING == room.getRoomType()) {
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
							room.getCircleId(), room.getPayType(), eachPerson, userIds, room.getCreateRoom());
					if (!b) {
						List<UUID> allUUIDList = new ArrayList<>();
						for (String s : accountList) {
							allUUIDList.add(room.getPlayerMap().get(s).getUuid());
							room.getUserPacketMap().get(s).setStatus(SSSConstant.SSS_GAME_STATUS_INIT);
						}
						for (String s : accountList) {
							postData.put(CommonConstant.DATA_KEY_ACCOUNT, s);
							postData.put("notSend", CommonConstant.GLOBAL_YES);
							exitRoom(GameMain.server.getClient(room.getPlayerMap().get(s).getUuid()), postData);
						}
						JSONObject result = new JSONObject();
						result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
						result.put(CommonConstant.RESULT_KEY_MSG, "基金不足请充值!");
						CommonConstant.sendMsgEventToAll(allUUIDList, result.toString(), "tipMsgPush");
						return;
					}
				}
			}
			// 茶楼房卡扣除
			else if (CommonConstant.ROOM_TYPE_TEA == room.getRoomType()) {
				// 满人开始 只扣除第一局
				if (0 == room.getGameIndex()) {

					if (CommonConstant.ROOM_PAY_TYPE_TEA.equals(room.getPayType())) {// 茶壶支付
						int deductType = teaService.deductTeaMoney(roomNo, null, room.getCircleId(), room.getPayType(),
								room.getSinglePayNum() * room.getPlayerCount());
						if (CommonConstant.ROOM_PAY_TYPE_TEA.equals(room.getPayType())) {// 茶壶支付
							if (TeaConstant.START_ROOM_NO == deductType || TeaConstant.START_ROOM_NO_1 == deductType
									|| TeaConstant.START_ROOM_NO_3 == deductType) {
								String msg = "";
								if (TeaConstant.START_ROOM_NO == deductType) {// 0茶楼不存在
									msg = "请刷新后再试";
								} else if (TeaConstant.START_ROOM_NO_1 == deductType) {// 1茶楼已经打烊
									msg = "该茶楼已经打烊";
								} else if (TeaConstant.START_ROOM_NO_3 == deductType) {// 3余额不足
									msg = "该茶楼茶壶不足";
								}
								JSONObject result = new JSONObject();
								result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
								result.put(CommonConstant.RESULT_KEY_MSG, msg);
								CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(),
										"tipMsgPush");
								return;
							}
						}
					} else if (CommonConstant.ROOM_PAY_TYPE_TEA_AA.equals(room.getPayType())) {
						Map<Long, String> map = new HashMap<>();
						for (String accountS : room.getUserPacketMap().keySet()) {
							if (room.getUserPacketMap().containsKey(accountS)
									&& room.getUserPacketMap().get(accountS) != null) {
								if (room.getPlayerMap().get(accountS).getPlayTimes() == 0) {
									map.put(room.getPlayerMap().get(accountS).getId(), accountS);
								}
							}
						}
						if (null != map && map.size() > 0) {
							for (Long usersId : map.keySet()) {
								int deductType = teaService.deductTeaMoney(roomNo, usersId, room.getCircleId(),
										room.getPayType(), room.getSinglePayNum());
								if (TeaConstant.START_ROOM_YES != deductType) {
									postData.put(CommonConstant.DATA_KEY_ACCOUNT, map.get(usersId));
									postData.put("notSend", CommonConstant.GLOBAL_YES);
									exitRoom(GameMain.server
											.getClient(room.getPlayerMap().get(map.get(usersId)).getUuid()), postData);
									return;
								}
							}
						}
					} else {
						return;
					}

					// 通知所有玩家
					for (String uuid : room.getUserPacketMap().keySet()) {
						SocketIOClient playerClient = GameMain.server
								.getClient(room.getPlayerMap().get(uuid).getUuid());
						JSONObject object = new JSONObject();
						object.element(CommonConstant.DATA_KEY_ACCOUNT, uuid).element(CommonConstant.DATA_KEY_ROOM_NO,
								roomNo);
						createRoom(playerClient, object);
					}

				}
			}
			startGame(room.getRoomNo());
			return;
		}
		JSONObject result = new JSONObject();
		result.put("index", room.getPlayerMap().get(account).getMyIndex());
		result.put("showTimer", CommonConstant.GLOBAL_NO);
		result.put("timer", room.getTimeLeft());
		// 统一倒计时时间 start
		if (room.getNowReadyCount() == minStartPlayer) {
			if (CommonConstant.ROOM_TYPE_YB == roomType || CommonConstant.ROOM_TYPE_FREE == roomType
					|| CommonConstant.ROOM_TYPE_INNING == roomType && room.getGameIndex() == 0) {
				room.setTimeLeft(SSSConstant.SSS_TIMER_READY);
				room.setReadyOvertime(CommonConstant.READY_OVERTIME_OUT);
				ThreadPoolHelper.executorService.submit(new Runnable() {
					@Override
					public void run() {
						gameTimerSSS.gameOverTime(roomNo, SSSConstant.SSS_GAME_STATUS_READY,
								SSSConstant.SSS_TIMER_READY);
					}
				});
			}

			result.put("showTimer", CommonConstant.GLOBAL_YES);
			result.put("timer", room.getTimeLeft());
		}
		CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "playerReadyPush_SSS");
		// 统一倒计时时间 end

	}

	public void startGame(String roomNo) {
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room) {
			return;
		}
		System.out.println("============开始游戏=======");
		// 非准备或初始阶段无法开始开始游戏
		if (room.getGameStatus() != SSSConstant.SSS_GAME_STATUS_READY) {
			return;
		}
		if (room.getGameIndex() > room.getGameCount()) {
			System.out.println("局数超过强制结算");
			compulsorySettlement(room.getRoomNo());// 局数超过 强制结算
			return;
		}
		redisInfoService.insertSummary(room.getRoomNo(), "_SSS");
		// 初始化房间信息
		room.initGame();
		// 更新游戏局数
		JSONObject roomInfo = new JSONObject();
		roomInfo.put("roomId", room.getId());
		producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INDEX, roomInfo));

		if (room.getBankerType() == SSSConstant.SSS_BANKER_TYPE_BWZ
				|| room.getBankerType() == SSSConstant.SSS_BANKER_TYPE_HB) {
			startGameCommon(room.getRoomNo());
		} else if (room.getBankerType() == SSSConstant.SSS_BANKER_TYPE_ZZ) {
			// 设置房间状态
			room.setGameStatus(SSSConstant.SSS_GAME_STATUS_XZ);
			// 听通知玩家
			changeGameStatus(room);
			// 设置倒计时时间
			final int gameEventTime;
			if (!Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("XZTime")) {
				gameEventTime = room.getSetting().getInt("XZTime");
			} else {
				gameEventTime = SSSConstant.SSS_TIMER_GAME_XZ;
			}
			room.setTimeLeft(gameEventTime);
			// 改变状态，通知玩家
			changeGameStatus(room);
			ThreadPoolHelper.executorService.submit(new Runnable() {
				@Override
				public void run() {
					gameTimerSSS.gameOverTime(room.getRoomNo(), SSSConstant.SSS_GAME_STATUS_XZ, gameEventTime);
				}
			});
		}
		if (room.getFee() > 0 && room.getRoomType() != CommonConstant.ROOM_TYPE_FREE
				&& room.getRoomType() != CommonConstant.ROOM_TYPE_FREE
				&& room.getRoomType() != CommonConstant.ROOM_TYPE_INNING
				&& room.getRoomType() != CommonConstant.ROOM_TYPE_TEA) {
			JSONArray array = new JSONArray();
			Map<String, Double> map = new HashMap<String, Double>();
			for (String account : room.getPlayerMap().keySet()) {
				SSSUserPacket up = room.getUserPacketMap().get(account);
				Playerinfo playerinfo = room.getPlayerMap().get(account);
				if (null == up || null == playerinfo)
					continue;
				// 中途加入不抽水
				if (SSSConstant.SSS_USER_STATUS_INIT == up.getStatus())
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
				fundEventDeal.addBalChangeRecord(map, "十三水游戏抽水");
			}
		}
	}

	public void startGameCommon(String roomNo) {
		final SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		shuffleAndFp(room);// 洗牌发牌

		// 设置房间状态(配牌)
		room.setGameStatus(SSSConstant.SSS_GAME_STATUS_GAME_EVENT);
		// 设置玩家手牌
		JSONArray gameProcessFP = new JSONArray();
		for (String uuid : room.getUserPacketMap().keySet()) {
			SSSUserPacket up = room.getUserPacketMap().get(uuid);
			Playerinfo playerinfo = room.getPlayerMap().get(uuid);
			if (null == up || null == playerinfo)
				continue;

			// 存放游戏记录
			JSONObject userPai = new JSONObject();
			userPai.put("account", uuid);
			userPai.put("name", playerinfo.getName());
			userPai.put("pai", up.getMyPai());
			gameProcessFP.add(userPai);
		}
		room.getGameProcess().put("faPai", gameProcessFP);
		// 设置倒计时时间
		final int gameEventTime = room.getPattern();

		room.setTimeLeft(gameEventTime);
		// 改变状态，通知玩家
		changeGameStatus(room);
		if (gameEventTime > 0) {// 配牌倒计时
			ThreadPoolHelper.executorService.submit(new Runnable() {
				@Override
				public void run() {
					gameTimerSSS.gameOverTime(room.getRoomNo(), SSSConstant.SSS_GAME_STATUS_GAME_EVENT, gameEventTime);
				}
			});
		}

	}

	public void shuffleAndFp(SSSGameRoomNew room) {
		String feature = room.getFeature();// 十三水特色玩法 特色 0 无 1 癞子玩法 2 清一色 11 一癞 12 两癞
		String flowerColor = null;
		String sameColor = null;
		JSONObject baseInfo = room.getRoomInfo();

		if (baseInfo != null && baseInfo.containsKey(SSSConstant.SSS_DATA_KET_SAME_COLOR)) {
			sameColor = baseInfo.getString(SSSConstant.SSS_DATA_KET_SAME_COLOR);
			if (baseInfo.getString(SSSConstant.SSS_DATA_KET_SAME_COLOR).equals(SSSConstant.SSS_SAME_COLOR_YES)) {
				if (baseInfo.containsKey(SSSConstant.SSS_DATA_KET_FLOWER_COLOR)) {
					flowerColor = baseInfo.getString(SSSConstant.SSS_DATA_KET_FLOWER_COLOR);
				}
			}
		}
		int num = 0;
		for (String account : room.getUserPacketMap().keySet()) {
			if (room.getPlayerMap().get(account).isCollapse())
				continue; // 破产玩家不考虑进去
			num++;
		}

		// 洗牌
		room.shufflePai(num, room.getColor(), feature, sameColor, flowerColor, room.getHardGhostNumber());
		// 发牌
//        sssService.faPai(room.getRoomNo());
		sssService.faPaiNew(room.getRoomNo());

		// changeRobotCard(room);

	}

//    private void changeRobotCard(SSSGameRoomNew room) {
//        int maxLose = 0;
//        int maxWin = 0;
//        String maxWinAccount = null;
//        String maxLoseAccount = null;
//        for (String robotAccount : room.getRobotList()) {
//            if (RobotEventDeal.robots.containsKey(robotAccount) && RobotEventDeal.robots.get(robotAccount) != null) {
//                RobotInfo robot = RobotEventDeal.robots.get(robotAccount);
//                if (robot.getTotalScore() > maxWin) {
//                    maxWin = robot.getTotalScore();
//                    maxWinAccount = robotAccount;
//                } else if (robot.getTotalScore() < maxLose) {
//                    maxLose = robot.getTotalScore();
//                    maxLoseAccount = robotAccount;
//                }
//            }
//        }
//        int playerCount = 0;
//        for (String account : room.getPlayerMap().keySet()) {
//            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
//                if (room.getUserPacketMap().get(account).getStatus() > SSSConstant.SSS_USER_STATUS_INIT) {
//                    playerCount++;
//                }
//            }
//        }
//        Set<Object> set = redisService.sGet("card_library_" + playerCount);
//        List<JSONArray> winList = new ArrayList<>();
//        List<JSONArray> loseList = new ArrayList<>();
//        if (set.size() > 0) {
//            List<Object> list = new ArrayList<>(set);
//            int libraryIndex = RandomUtils.nextInt(set.size());
//            for (Object o : JSONArray.fromObject(list.get(libraryIndex))) {
//                if (JSONObject.fromObject(o).getInt("score") > 0) {
//                    winList.add(JSONObject.fromObject(o).getJSONArray("card"));
//                } else if (JSONObject.fromObject(o).getInt("score") < 0) {
//                    loseList.add(JSONObject.fromObject(o).getJSONArray("card"));
//                }
//            }
//            if (!Dto.stringIsNULL(maxLoseAccount)) {
//                int loseFlag = RandomUtils.nextInt(-RobotEventDeal.robots.get(maxLoseAccount).getMaxLoseScore());
//                if (-loseFlag > maxLose) {
//                    changeRobotCard(room, maxLoseAccount, winList, loseList);
//                }
//            } else if (!Dto.stringIsNULL(maxWinAccount)) {
//                int winFlag = RandomUtils.nextInt(RobotEventDeal.robots.get(maxWinAccount).getMaxWinScore());
//                if (winFlag < maxWin) {
//                    changeRobotCard(room, maxWinAccount, loseList, winList);
//                }
//            }
//        }
//    }
//
//    private void changeRobotCard(SSSGameRoomNew room, String maxAccount, List<JSONArray> list1, List<JSONArray> list2) {
//        List<JSONArray> allList = new ArrayList<>();
//        int index = RandomUtils.nextInt(list1.size());
//        if (index < list1.size()) {
//            JSONArray arr = list1.get(index);
//            list1.remove(index);
//            allList.addAll(list1);
//            allList.addAll(list2);
//            Collections.shuffle(allList);
//            int cardIndex = 0;
//            for (String account : room.getPlayerMap().keySet()) {
//                if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
//                    if (room.getUserPacketMap().get(account).getStatus() > SSSConstant.SSS_USER_STATUS_INIT) {
//                        if (account.equals(maxAccount)) {
//                            String[] p = new String[arr.size()];
//                            for (int i = 0; i < p.length; i++) {
//                                p[i] = arr.getString(i);
//                            }
//                            // 设置玩家手牌
//                            room.getUserPacketMap().get(account).setPai(p);
//                            // 设置玩家牌型
//                            room.getUserPacketMap().get(account).setPaiType(SSSSpecialCards.isSpecialCards(p, room.getSetting()));
//                        } else {
//                            String[] p = new String[allList.get(cardIndex).size()];
//                            for (int i = 0; i < p.length; i++) {
//                                p[i] = allList.get(cardIndex).getString(i);
//                            }
//                            // 设置玩家手牌
//                            room.getUserPacketMap().get(account).setPai(SSSGameRoomNew.sortPaiDesc(p));
//                            // 设置玩家牌型
//                            room.getUserPacketMap().get(account).setPaiType(SSSSpecialCards.isSpecialCards(p, room.getSetting()));
//                            cardIndex++;
//                        }
//                    }
//                }
//            }
//        }
//    }

	public void gameXiaZhu(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		// 不满足准备条件直接忽略
		if (!CommonConstant.checkEvent(postData, SSSConstant.SSS_GAME_STATUS_XZ, client)) {
			return;
		}
		// 房间号
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		// 玩家账号
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		if (room.getUserPacketMap().get(account).getStatus() != SSSConstant.SSS_USER_STATUS_READY) {
			return;
		}
		// 庄家不能下注
		if (account.equals(room.getBanker())) {
			return;
		}
		JSONArray array = room.getBaseNumTimes(room.getPlayerMap().get(account).getScore());
		int maxTimes = 1;
		for (Object o : array) {
			JSONObject baseNum = JSONObject.fromObject(o);
			if (baseNum.getInt("isuse") == CommonConstant.GLOBAL_YES && baseNum.getInt("val") > maxTimes) {
				maxTimes = baseNum.getInt("val");
			}
		}
		if (postData.getInt("money") < 0 && postData.getInt("money") > maxTimes) {
			return;
		}
		// 设置玩家下注状态
		room.getUserPacketMap().get(account).setStatus(SSSConstant.SSS_USER_STATUS_XZ);
		// 下注分数
		room.getUserPacketMap().get(account).setXzTimes(postData.getInt("money"));
		JSONObject result = new JSONObject();
		result.put("index", room.getPlayerMap().get(account).getMyIndex());
		result.put("value", room.getUserPacketMap().get(account).getXzTimes());
		CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "gameXiaZhuPush_SSS");
		if (room.isAllXiaZhu()) {
			startGameCommon(roomNo);
		}
	}

	public void gameBeBanker(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		// 不满足准备条件直接忽略
		if (!CommonConstant.checkEvent(postData, SSSConstant.SSS_GAME_STATUS_TO_BE_BANKER, client)) {
			return;
		}
		// 房间号
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
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
		room.setGameStatus(SSSConstant.SSS_GAME_STATUS_READY);
		changeGameStatus(room);
	}

	public void gameEvent(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!CommonConstant.checkEvent(postData, SSSConstant.SSS_GAME_STATUS_GAME_EVENT, client)) {
			return;
		}
		final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room) {
			return;
		}
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		SSSUserPacket up = room.getUserPacketMap().get(account);
		log.debug(up.toString());
		if (null == up) {
			return;
		}
		int status = room.getUserPacketMap().get(account).getStatus();
		// 牌为空或 没有在准备状态
		if (null == up.getPai() || SSSConstant.SSS_GAME_STATUS_READY != status) {
			return;
		}
		if (postData.containsKey(SSSConstant.SSS_DATA_KET_TYPE)) {
			// 配牌类型
			int type = postData.getInt(SSSConstant.SSS_DATA_KET_TYPE);
			switch (type) {
			case SSSConstant.SSS_GAME_ACTION_TYPE_AUTO:// 配牌类型-自动
				if (room.getUserPacketMap().get(account).getPaiType() > 0) {// 特色牌处理
					String sameColor = room.getRoomInfo().containsKey(SSSConstant.SSS_DATA_KET_SAME_COLOR)
							? room.getRoomInfo().getString(SSSConstant.SSS_DATA_KET_SAME_COLOR)
							: null;
					int specialType = sssService.isSpecialCards(up.getPai(), sameColor);
					String[] specialPai = SSSSpecialCardSort.CardSort(up.getPai(), specialType, room);
					room.getUserPacketMap().get(account).setPai(specialPai);
					// 设置特殊牌型分数
					room.getUserPacketMap().get(account).setPaiScore(sssService.score(specialType));
					// 设置玩家头中尾手牌
					room.changePlayerPai(specialPai, account);
				} else {
					String[] auto = SSSOrdinaryCards.sort(up.getPai(), room);
					if (!room.getRoomInfo().containsKey(SSSConstant.SSS_DATA_KET_SAME_COLOR)
							|| SSSConstant.SSS_SAME_COLOR_NO
									.equals(room.getRoomInfo().getString(SSSConstant.SSS_DATA_KET_SAME_COLOR))) {
						auto = checkBestPai(auto);
					}
					room.getUserPacketMap().get(account).setPai(auto);
					room.changePlayerPai(auto, account);
				}
				break;
			case SSSConstant.SSS_GAME_ACTION_TYPE_COMMON:// 配牌类型-手动
				JSONArray myPai = postData.getJSONArray(SSSConstant.SSS_DATA_KET_MY_PAI);
				// 数据不正确直接返回
				for (int i = 0; i < up.getMyPai().length; i++) {
					if (!myPai.contains((up.getMyPai())[i])) {
						return;
					}
				}
				JSONArray actionResult = SSSComputeCards.judge(up.togetMyPai(myPai), room);
				if ("倒水".equals(actionResult.get(0))) {
					String[] best = SSSOrdinaryCards.sort(up.getPai(), room);
					if (!room.getRoomInfo().containsKey(SSSConstant.SSS_DATA_KET_SAME_COLOR)
							|| SSSConstant.SSS_SAME_COLOR_NO
									.equals(room.getRoomInfo().getString(SSSConstant.SSS_DATA_KET_SAME_COLOR))) {
						best = checkBestPai(best);
					}
					up.setPai(best);
					room.changePlayerPai(best, account);
				} else {
					String[] str = new String[13];
					for (int i = 0; i < myPai.size(); i++) {
						if (myPai.getInt(i) < 20) {
							String a = "1-" + myPai.getString(i);
							str[i] = a;
						} else if (myPai.getInt(i) < 40) {
							String a = "2-" + (myPai.getInt(i) - 20);
							str[i] = a;
						} else if (myPai.getInt(i) < 60) {
							String a = "3-" + (myPai.getInt(i) - 40);
							str[i] = a;
						} else if (myPai.getInt(i) < 80) {
							String a = "4-" + (myPai.getInt(i) - 60);
							str[i] = a;
						} else {
							String a = "5-" + (myPai.getInt(i) - 80);
							str[i] = a;
						}
					}
					// 头道排序
					str = SSSGameRoomNew.AppointSort(str, 0, 2);
					// 中道排序
					str = SSSGameRoomNew.AppointSort(str, 3, 7);
					// 尾道排序
					str = SSSGameRoomNew.AppointSort(str, 8, 12);
					// 设置玩家手牌
					room.getUserPacketMap().get(account).setPai(str);
					// 设置玩家头中尾手牌
					room.changePlayerPai(str, account);
					// 设置玩家牌型
					room.getUserPacketMap().get(account).setPaiType(0);
				}
				break;
			case SSSConstant.SSS_GAME_ACTION_TYPE_SPECIAL:// 配牌类型-特殊牌
				String sameColor = room.getRoomInfo().containsKey(SSSConstant.SSS_DATA_KET_SAME_COLOR)
						? room.getRoomInfo().getString(SSSConstant.SSS_DATA_KET_SAME_COLOR)
						: null;
				int specialType = sssService.isSpecialCards(up.getPai(), sameColor);
				String[] specialPai = SSSSpecialCardSort.CardSort(up.getPai(), specialType, room);
				room.getUserPacketMap().get(account).setPai(specialPai);
				room.getUserPacketMap().get(account).setPaiType(specialType);
				// 设置特殊牌型分数
				room.getUserPacketMap().get(account).setPaiScore(sssService.score(specialType));
				// 设置玩家头中尾手牌
				room.changePlayerPai(specialPai, account);
				break;
			default:
				break;
			}
			room.getUserPacketMap().get(account).setStatus(SSSConstant.SSS_USER_STATUS_GAME_EVENT);
			// 所有人都完成操作切换游戏阶段
			if (room.isAllFinish()) {
				allFinishDeal(roomNo);
			} else {
				JSONObject result = new JSONObject();
				result.put("index", room.getPlayerMap().get(account).getMyIndex());
				result.put("showTimer", CommonConstant.GLOBAL_NO);
				if (room.getTimeLeft() > SSSConstant.SSS_TIMER_INIT) {
					result.put("showTimer", CommonConstant.GLOBAL_YES);
				}
				result.put("timer", room.getTimeLeft());
				CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "gameActionPush_SSS");
			}
		}
	}

	public void allFinishDeal(final String roomNo) {
		long summaryTimes = redisInfoService.summaryTimes(roomNo, "_SSS");
		if (summaryTimes > 1) {
			return;
		}
		final SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		final int roomType = room.getRoomType();
		// 设置为比牌状态
		room.setGameStatus(SSSConstant.SSS_GAME_STATUS_COMPARE);
		// 根据庄家类型进行结算
		switch (room.getBankerType()) {
		case SSSConstant.SSS_BANKER_TYPE_HB:
			gameSummaryHb(roomNo);
			break;
		case SSSConstant.SSS_BANKER_TYPE_BWZ:
			gameSummaryBwzOrZZ(roomNo);
			break;
		case SSSConstant.SSS_BANKER_TYPE_ZZ:
			gameSummaryBwzOrZZ(roomNo);
			break;
		default:
			break;
		}
		if (CommonConstant.ROOM_TYPE_FK == roomType || CommonConstant.ROOM_TYPE_DK == roomType
				|| CommonConstant.ROOM_TYPE_CLUB == roomType || CommonConstant.ROOM_TYPE_INNING == roomType
				|| CommonConstant.ROOM_TYPE_TEA == roomType) {
			room.setNeedFinalSummary(true);
			roomCardSummary(roomNo);
		}
		// 改变状态通知玩家
		if (SSSConstant.SSS_SPEED_MODE_NO.equals(room.getSpeedMode())) {
			changeGameStatus(room);
		}
		ThreadPoolHelper.executorService.submit(new Runnable() {
			@Override
			public void run() {
				int compareTime = SSSConstant.SSS_COMPARE_TIME_BASE;
				compareTime += room.obtainNotSpecialCount() * 3 * SSSConstant.SSS_COMPARE_TIME_SHOW;
				compareTime += room.getDqArray().size() * SSSConstant.SSS_COMPARE_TIME_DQ;
				compareTime += room.getSwat() * SSSConstant.SSS_COMPARE_TIME_SWAT;
				// 如果是极速模式,增加配牌打枪和全垒打时间
				if (SSSConstant.SSS_SPEED_MODE_YES.equals(room.getSpeedMode())) {
					compareTime = 1;
				}
				System.out.println("时间："+compareTime);
				room.setCompareTimer(compareTime);
				for (int i = 1; i <= compareTime; i++) {
					try {
						room.setCompareTimer(i);
						if (SSSConstant.SSS_SPEED_MODE_NO.equals(room.getSpeedMode())) {
							Thread.sleep(100);
						}
						if (i == compareTime) {
							// 更新玩家余额
							for (String account : room.getUserPacketMap().keySet()) {
								if (room.getUserPacketMap().containsKey(account)
										&& room.getUserPacketMap().get(account) != null) {
									if (room.getUserPacketMap().get(account)
											.getStatus() != SSSConstant.SSS_USER_STATUS_INIT) {
										double sum = room.getUserPacketMap().get(account).getScore();
										double oldScore = RoomManage.gameRoomMap.get(roomNo).getPlayerMap().get(account)
												.getScore();
										double newScore = Dto.add(sum, oldScore);
										Playerinfo playerinfo = RoomManage.gameRoomMap.get(roomNo).getPlayerMap()
												.get(account);
										playerinfo.setScore(newScore);
									}
								}
							}
							if (room.getGameStatus() != SSSConstant.SSS_GAME_STATUS_FINAL_SUMMARY) {
								room.setGameStatus(SSSConstant.SSS_GAME_STATUS_SUMMARY);
							}
							// 初始化倒计时
							room.setTimeLeft(SSSConstant.SSS_TIMER_INIT);
							// 更新数据库
							updateUserScore(room.getRoomNo());

							if (CommonConstant.ROOM_TYPE_YB == roomType) {
								saveUserDeduction(room);
							}
							if (CommonConstant.ROOM_TYPE_FK == roomType || CommonConstant.ROOM_TYPE_DK == roomType
									|| CommonConstant.ROOM_TYPE_CLUB == roomType
									|| CommonConstant.ROOM_TYPE_INNING == roomType) {
								updateRoomCard(room.getRoomNo());// 扣除房卡
							}

							saveGameLog(room.getRoomNo());
							// 改变状态，通知玩家
							changeGameStatus(room);

							// 坐庄模式元宝不足
							if (room.getBankerType() == SSSConstant.SSS_BANKER_TYPE_ZZ) {
								if (CommonConstant.ROOM_TYPE_FK != roomType
										&& CommonConstant.ROOM_TYPE_INNING != roomType
										&& CommonConstant.ROOM_TYPE_TEA != roomType) {
									if (room.getPlayerMap().get(room.getBanker()).getScore() < room
											.getMinBankerScore()) {
										// 庄家设为空
										room.setBanker(null);
										// 设置游戏状态
										room.setGameStatus(SSSConstant.SSS_GAME_STATUS_TO_BE_BANKER);
										// 初始化倒计时
										room.setTimeLeft(SSSConstant.SSS_TIMER_INIT);
										// 重置玩家状态
										for (String uuid : room.getUserPacketMap().keySet()) {
											if (room.getUserPacketMap().containsKey(uuid)
													&& room.getUserPacketMap().get(uuid) != null) {
												room.getUserPacketMap().get(uuid).setStatus(SSSConstant.SSS_TIMER_INIT);
											}
										}
									}
								}
							}
							for (String account : room.getUserPacketMap().keySet()) {
								if (room.getUserPacketMap().containsKey(account)
										&& room.getUserPacketMap().get(account) != null) {
									if (room.getUserPacketMap().get(account)
											.getStatus() != SSSConstant.SSS_USER_STATUS_INIT) {
										if (room.getRobotList().contains(account)) {
											RobotEventDeal.robots.get(account).addTotalScore(
													(int) room.getUserPacketMap().get(account).getScore());
										}
									}
								}
							}
						}
					} catch (Exception e) {
						log.error("", e);
					}
				}

				if (room.getGameIndex() >= room.getGameCount()
						|| SSSConstant.SSS_GAME_STATUS_FINAL_SUMMARY == room.getGameStatus()) {
					room.setNeedFinalSummary(true);
					finalSummaryRoom(room.getRoomNo());// 总结算房间处理
				}
				// 结算
				if (SSSConstant.SSS_GAME_STATUS_SUMMARY != room.getGameStatus())
					return;

				// 结算后 开始准备定时器 //局数场 茶楼 房卡场
				if ((CommonConstant.ROOM_TYPE_INNING == roomType || CommonConstant.ROOM_TYPE_TEA == roomType
						|| CommonConstant.ROOM_TYPE_FK == roomType) && room.getGameIndex() < room.getGameCount()) {
					int time = SSSConstant.SSS_TIMER_READY_INNING;
					// 判断是否是极速模式
					if (SSSConstant.SSS_SPEED_MODE_NO.equals(room.getSpeedMode())) {
						time = redisInfoService.getAppTimeSetting(room.getPlatform(), room.getGid(),
								"SSSConstant.SSS_TIMER_READY_INNING");
					} else if (SSSConstant.SSS_SPEED_MODE_YES.equals(room.getSpeedMode())) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						time = redisInfoService.getAppTimeSetting(room.getPlatform(), room.getGid(),
								"SSSConstant.SSS_SPEED_TIMER_READY");
					}
					room.setGameStatus(SSSConstant.SSS_GAME_STATUS_READY);
					if (-99 == time) {
						time = SSSConstant.SSS_TIMER_READY_INNING;
					}
					final int overTime = time;
					if (overTime >= 0) {
//                    统一倒计时时间 start
						ThreadPoolHelper.executorService.submit(new Runnable() {
							@Override
							public void run() {
								gameTimerSSS.gameOverTime(roomNo, SSSConstant.SSS_GAME_STATUS_SUMMARY, 30);
							}
						});
					}
					JSONObject result = new JSONObject();
					result.put("showTimer", CommonConstant.GLOBAL_YES);
					result.put("timer", 30);
					CommonConstant.sendMsgEventAll(room.getAllUUIDList(), result, "sssTimerPush_SSS");
//                    统一倒计时时间 end
				}
			}
		});
	}

	public void roomCardSummary(String roomNo) {
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (room == null) {
			return;
		}
		for (String account : room.getPlayerMap().keySet()) {
			SSSUserPacket up = room.getUserPacketMap().get(account);
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (null == up || null == playerinfo)
				continue;
			// 有参与的玩家
			if (SSSConstant.SSS_USER_STATUS_GAME_EVENT != up.getStatus())
				continue;
			// 胜利次数+1
			if (up.getScore() > 0) {
				up.setWinTimes(up.getWinTimes() + 1);
			}
			// 全垒打次数+1
			if (up.getSwat() == CommonConstant.GLOBAL_YES) {
				up.setSwatTimes(up.getSwatTimes() + 1);
			}
			// 特殊牌次数+1
			if (up.getPaiType() > 0) {
				up.setSpecialTimes(up.getSpecialTimes() + 1);
			}
			// 普通牌次数+1
			if (up.getPaiType() == 0) {
				up.setOrdinaryTimes(up.getOrdinaryTimes() + 1);
			}
			for (int i = 0; i < room.getDqArray().size(); i++) {
				JSONArray dq = room.getDqArray().getJSONArray(i);
				// 打枪次数+1
				if (dq.getInt(0) == room.getPlayerMap().get(account).getMyIndex()) {
					up.setDqTimes(up.getDqTimes() + 1);
				}
				// 被打枪次数+1
				if (dq.getInt(1) == room.getPlayerMap().get(account).getMyIndex()) {
					up.setBdqTimes(up.getBdqTimes() + 1);
				}
			}
		}
	}

	public void cardEnd(SocketIOClient client, Object data) {

	}

	public String[] checkBestPai(String[] myPai) {
		JSONArray midPai = new JSONArray();
		for (int i = 3; i < 8; i++) {
			midPai.add(myPai[i]);
		}
		JSONArray footPai = new JSONArray();
		for (int i = 8; i < 13; i++) {
			footPai.add(myPai[i]);
		}
		int mid = SSSComputeCards.isSameFlower(midPai);
		int mid1 = SSSComputeCards.isFlushByFlower(midPai);
		int foot = SSSComputeCards.isSameFlower(footPai);
		int foot1 = SSSComputeCards.isFlushByFlower(footPai);
		// 都是同花且不是同花顺
		if (mid == 5 && foot == 5 && mid1 == -1 && foot1 == -1) {
			int result = SSSComputeCards.compareSameFlower(midPai, footPai);
			if (result == 1) {
				String[] pai = new String[myPai.length];
				for (int i = 0; i < 3; i++) {
					pai[i] = myPai[i];
				}
				for (int i = 3; i < 8; i++) {
					pai[i] = footPai.getString(i - 3);
				}
				for (int i = 8; i < 13; i++) {
					pai[i] = midPai.getString(i - 8);
				}
				return pai;
			}
		}
		return myPai;
	}

	public void updateUserScore(String roomNo) {
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		double leaveScore = room.getLeaveScore();// 离场验证
		JSONArray array = new JSONArray();
		Map<String, Double> map = new HashMap<String, Double>();
		JSONObject pumpInfo = new JSONObject();
		// 存放游戏记录
		for (String uuid : room.getUserPacketMap().keySet()) {
			Playerinfo playerinfo = room.getPlayerMap().get(uuid);
			SSSUserPacket up = room.getUserPacketMap().get(uuid);
			if (null == playerinfo || null == up)
				continue;

			if (playerinfo.isCollapse())
				continue; // 破产玩家不考虑进去
			if (SSSConstant.SSS_USER_STATUS_INIT == up.getStatus())
				continue;
			// 有参与的玩家
			// 元宝输赢情况
			JSONObject obj = new JSONObject();
			if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB
					|| room.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
				obj.put("total", playerinfo.getScore());
				obj.put("fen", up.getScore());
				obj.put("id", playerinfo.getId());
				array.add(obj);
				map.put(playerinfo.getOpenId(), up.getScore());
			}
			// 亲友圈输赢
			else if (room.getRoomType() == CommonConstant.ROOM_TYPE_FREE
					|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING
					|| CommonConstant.ROOM_TYPE_TEA == room.getRoomType()
					|| CommonConstant.ROOM_TYPE_FK == room.getRoomType()) {
				pumpInfo.put(String.valueOf(playerinfo.getId()), up.getScore());
			}
			if (CommonConstant.ROOM_TYPE_INNING == room.getRoomType()
					&& leaveScore > Dto.add(playerinfo.getScore(), playerinfo.getSourceScore())) {
				playerinfo.setCollapse(true);// 玩家破产
			}
		}

		if (room.getMinPlayer() > room.getCollapseNum()) {// 破产人数太多无法继续游戏
			room.setGameIndex(room.getGameCount());
		}

		if (pumpInfo.size() > 0) {
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

			// 亲友圈抽水
			userPumpingFee(room.getRoomNo());

		}
		// 更新玩家分数
		if (array.size() > 0 && CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE != room.getIsClose()) {
			producerService.sendMessage(daoQueueDestination,
					new PumpDao(DaoTypeConstant.UPDATE_SCORE, room.getPumpObject(array)));
		}
	}

	private void userPumpingFee(String roomNo) {
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		int gameStatus = room.getGameStatus();// 房间状态
		if (SSSConstant.SSS_GAME_STATUS_SUMMARY != gameStatus
				&& SSSConstant.SSS_GAME_STATUS_FINAL_SUMMARY != gameStatus)
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
			for (String account : room.getUserPacketMap().keySet()) {
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
				SSSUserPacket up = room.getUserPacketMap().get(account);
				if (null == up)
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
		for (String account : room.getUserPacketMap().keySet()) {
			double deductScore = 0D;// 抽水分数
			SSSUserPacket up = room.getUserPacketMap().get(account);
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
					log.info("十三水固定抽水设置错误");
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

	public void updateRoomCard(String roomNo) {
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		JSONArray array = new JSONArray();
		int roomType = room.getRoomType();
		int roomCardCount = 0;
		if (CommonConstant.ROOM_TYPE_FK == roomType || CommonConstant.ROOM_TYPE_CLUB == roomType
				|| CommonConstant.ROOM_TYPE_INNING == roomType) {
			for (String account : room.getPlayerMap().keySet()) {
				SSSUserPacket up = room.getUserPacketMap().get(account);
				Playerinfo playerinfo = room.getPlayerMap().get(account);
				if (null == up || null == playerinfo)
					continue;

				// 房主支付
				if (CommonConstant.ROOM_PAY_TYPE_FANGZHU.equals(room.getPayType())) {
					if (account.equals(room.getOwner())) {
						// 参与第一局需要扣房卡
						if (playerinfo.getPlayTimes() == 1) {
							array.add(playerinfo.getId());
							roomCardCount = room.getPlayerCount() * room.getSinglePayNum();
						}
					}
				}
				// 房费AA
				else if (CommonConstant.ROOM_PAY_TYPE_AA.equals(room.getPayType())) {
					// 参与第一局需要扣房卡
					if (playerinfo.getPlayTimes() == 1) {
						array.add(playerinfo.getId());
						roomCardCount = room.getSinglePayNum();
					}
				}
			}
		}
		if (array.size() > 0 && CommonConstant.ROOM_TYPE_FREE != roomType
				&& CommonConstant.ROOM_TYPE_INNING != roomType) {
			producerService.sendMessage(daoQueueDestination,
					new PumpDao(DaoTypeConstant.PUMP, room.getRoomCardChangeObject(array, roomCardCount)));
		}
	}

	public void saveUserDeduction(SSSGameRoomNew room) {
		JSONArray userDeductionData = new JSONArray();
		for (String uuid : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
				// 有参与的玩家
				if (room.getUserPacketMap().get(uuid).getStatus() > SSSConstant.SSS_USER_STATUS_INIT) {
					// 用户游戏记录
					JSONObject object = new JSONObject();
					object.put("id", room.getPlayerMap().get(uuid).getId());
					object.put("gid", room.getGid());
					object.put("roomNo", room.getRoomNo());
					object.put("type", room.getRoomType());
					object.put("fen", room.getUserPacketMap().get(uuid).getScore());
					object.put("old", Dto.sub(room.getPlayerMap().get(uuid).getScore(),
							room.getUserPacketMap().get(uuid).getScore()));
					if (room.getPlayerMap().get(uuid).getScore() < 0D) {
						object.put("new", 0);
					} else {
						object.put("new", room.getPlayerMap().get(uuid).getScore());
					}
					userDeductionData.add(object);
				}
			}
		}
		// 玩家输赢记录
		producerService.sendMessage(daoQueueDestination,
				new PumpDao(DaoTypeConstant.USER_DEDUCTION, new JSONObject().element("user", userDeductionData)));
	}

	public void saveGameLog(String roomNo) {
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room || CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE == room.getIsClose()
				&& SSSConstant.SSS_GAME_STATUS_SUMMARY != room.getGameStatus()
				&& SSSConstant.SSS_GAME_STATUS_FINAL_SUMMARY != room.getGameStatus())
			return;

		JSONArray array = new JSONArray();
		JSONArray gameLogResults = new JSONArray();
		JSONArray gameResult = new JSONArray();
		// 存放游戏记录
		JSONArray gameProcessJS = new JSONArray();
		for (String uuid : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
				// 有参与的玩家
				if (room.getUserPacketMap().get(uuid).getStatus() > SSSConstant.SSS_USER_STATUS_INIT) {
					JSONObject userJS = new JSONObject();
					userJS.put("account", uuid);
					userJS.put("name", room.getPlayerMap().get(uuid).getName());
					userJS.put("sum", room.getUserPacketMap().get(uuid).getScore());
					userJS.put("pai", room.getUserPacketMap().get(uuid).getMyPai());
					userJS.put("paiType", room.getUserPacketMap().get(uuid).getPaiType());
					userJS.put("old", Dto.sub(room.getPlayerMap().get(uuid).getScore(),
							room.getUserPacketMap().get(uuid).getScore()));
					userJS.put("createTime", DateUtils.getTodayTime());
					if (room.getPlayerMap().get(uuid).getScore() < 0D) {
						userJS.put("new", 0);
					} else {
						userJS.put("new", room.getPlayerMap().get(uuid).getScore());
					}
					gameProcessJS.add(userJS);

					JSONObject obj = new JSONObject();
					obj.put("fen", room.getUserPacketMap().get(uuid).getScore());
					obj.put("id", room.getPlayerMap().get(uuid).getId());
					array.add(obj);
					// 战绩记录
					JSONObject gameLogResult = new JSONObject();
					gameLogResult.put("account", uuid);
					gameLogResult.put("name", room.getPlayerMap().get(uuid).getName());
					gameLogResult.put("headimg", room.getPlayerMap().get(uuid).getHeadimg());
					if (room.getPlayerMap().get(room.getBanker()) == null) {
						gameLogResult.put("zhuang", CommonConstant.NO_BANKER_INDEX);
					} else {
						gameLogResult.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
					}
					gameLogResult.put("myIndex", room.getPlayerMap().get(uuid).getMyIndex());
					gameLogResult.put("myPai", room.getUserPacketMap().get(uuid).getMyPai());
					gameLogResult.put("score", room.getUserPacketMap().get(uuid).getScore());
					gameLogResult.put("totalScore", room.getPlayerMap().get(uuid).getScore());
					gameLogResult.put("win", CommonConstant.GLOBAL_YES);
					if (room.getUserPacketMap().get(uuid).getScore() < 0) {
						gameLogResult.put("win", CommonConstant.GLOBAL_NO);
					}
					gameLogResults.add(gameLogResult);
					// 用户战绩
					JSONObject userResult = new JSONObject();
					userResult.put("zhuang", room.getBanker());
					userResult.put("isWinner", CommonConstant.GLOBAL_NO);
					if (room.getUserPacketMap().get(uuid).getScore() > 0) {
						userResult.put("isWinner", CommonConstant.GLOBAL_YES);
					}
					userResult.put("score", room.getUserPacketMap().get(uuid).getScore());
					userResult.put("totalScore", room.getPlayerMap().get(uuid).getScore());
					userResult.put("player", room.getPlayerMap().get(uuid).getName());
					userResult.put("account", uuid);
					gameResult.add(userResult);
				}
			}
		}
		room.getGameProcess().put("JieSuan", gameProcessJS);
		room.getGameRecords().add(gameProcessJS);

		log.info("[{}]---[{}]", room.getRoomNo(), room.getGameProcess());
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
		System.out.println("十三水退出房间");
		String eventName = "exitRoomPush_SSS";
		JSONObject postData = JSONObject.fromObject(data);

		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room) {
			JSONObject result = new JSONObject();
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			result.put("type", 2);
			CommonConstant.sendMsgEvent(client, result, eventName);
			return;
		}
		SSSUserPacket up = room.getUserPacketMap().get(account);
		Playerinfo playerinfo = room.getPlayerMap().get(account);

		if (null == up || null == playerinfo) {
			room.getPlayerMap().remove(account);
			room.getVisitPlayerMap().remove(account); // 观战人员直接退出
			room.getUserPacketMap().remove(account);
			JSONObject result = new JSONObject();
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			result.put("type", 2);
			CommonConstant.sendMsgEvent(client, result, eventName);
			return;
		}

		int roomType = room.getRoomType();

		boolean canExit = false;
		// 金币场、元宝场
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB || room.getRoomType() == CommonConstant.ROOM_TYPE_YB
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_COMPETITIVE
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_FREE) {
			// 未参与游戏可以自由退出
			if (room.getUserPacketMap().get(account).getStatus() == SSSConstant.SSS_USER_STATUS_INIT) {
				canExit = true;
			} else if (room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_INIT
					|| room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_READY
					|| room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_SUMMARY) {// 初始及准备阶段可以退出
				canExit = true;
			}
		} else if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_DK
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
			if (playerinfo.isCollapse()) {// 破产玩家不能退出
			} else {
				// 房卡场没玩过可以退出
				if (room.getPlayerMap().get(account).getPlayTimes() == 0) {
					if (CommonConstant.ROOM_PAY_TYPE_AA.equals(room.getPayType()) || !room.getOwner().equals(account)
							|| room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
						canExit = true;
					} else if (room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
						canExit = true;
					}
				}
				if (room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_FINAL_SUMMARY) {
					canExit = true;
				}
			}
		} else if (CommonConstant.ROOM_TYPE_TEA == room.getRoomType()) {
			canExit = false;// 茶楼不运行退出
		}
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
			result.put("index", playerinfo.getMyIndex());
			int minStartPlayer = room.getMinPlayer();
			if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB
					|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING
					|| room.getRoomType() == CommonConstant.ROOM_TYPE_TEA) {
				if (CommonConstant.IS_ALL_START_Y.equals(room.getIsAllStart())) {// 满人开始
					minStartPlayer = room.getPlayerCount();
				}
			}
			if (room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_READY && room.getNowReadyCount() < minStartPlayer) {
				// 重置房间倒计时
				room.setTimeLeft(SSSConstant.SSS_TIMER_INIT);
			}
			if (room.getTimeLeft() > SSSConstant.SSS_TIMER_INIT
					&& room.getGameStatus() != SSSConstant.SSS_GAME_STATUS_COMPARE) {
				result.put("showTimer", CommonConstant.GLOBAL_YES);
			} else {
				result.put("showTimer", CommonConstant.GLOBAL_NO);
			}
			result.put("timer", room.getTimeLeft());
			result.put("startIndex", getStartIndex(roomNo));
			if (!postData.containsKey("notSend")) {
				CommonConstant.sendMsgEventToAll(allUUIDList, result.toString(), "exitRoomPush");
			}
			if (postData.containsKey("notSendToMe")) {
				CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "exitRoomPush");
			}

			// 坐庄模式
			if (room.getGameStatus() != SSSConstant.SSS_GAME_STATUS_FINAL_SUMMARY) {
				if (room.getBankerType() == SSSConstant.SSS_BANKER_TYPE_ZZ) {
					// 房主退出且房间内有其他玩家
					if (account.equals(room.getBanker()) && room.getUserPacketMap().size() > 0) {
						// 庄家设为空
						room.setBanker(null);
						// 设置游戏状态
						room.setGameStatus(SSSConstant.SSS_GAME_STATUS_TO_BE_BANKER);
						// 初始化倒计时
						room.setTimeLeft(SSSConstant.SSS_TIMER_INIT);
						// 重置玩家状态
						for (String uuid : room.getUserPacketMap().keySet()) {
							if (room.getUserPacketMap().containsKey(uuid)
									&& room.getUserPacketMap().get(uuid) != null) {
								room.getUserPacketMap().get(uuid).setStatus(SSSConstant.SSS_USER_STATUS_INIT);
							}
						}
						changeGameStatus(room);
						return;
					}
				}
			}

			// 房间内所有玩家都已经完成准备且人数大于最低开始人数通知开始游戏
			if (room.getPlayerMap().size() <= room.getPlayerCount() && room.isAllReady()
					&& room.getPlayerMap().size() >= minStartPlayer) {
				startGame(room.getRoomNo());
			}
			// 所有人都退出清除房间数据
			System.out.println("人数剩余：" + room.getPlayerMap());
			System.out.println(roomType);
			if (room.getPlayerMap().size() == 0 && CommonConstant.ROOM_TYPE_FK == roomType) {
				System.out.println("移除房间");
				removeRoom(roomNo);// 移除房间
				return;
			}

		} else {
			// 组织数据，通知玩家
			JSONObject result = new JSONObject();
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			result.put(CommonConstant.RESULT_KEY_MSG, "游戏中无法退出");
			result.put("showTimer", CommonConstant.GLOBAL_YES);
			if (room.getTimeLeft() == 0) {
				result.put("showTimer", CommonConstant.GLOBAL_NO);
			}
			result.put("timer", room.getTimeLeft());
			result.put("type", 1);
			CommonConstant.sendMsgEventToSingle(client, result.toString(), eventName);
		}
	}

	public void closeRoom(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
			return;
		}

		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		int roomType = room.getRoomType();
		if (room.allNoClose()) {// 第一个提交解散
			String circleCode = room.getClubCode();// 俱乐部
			if (CommonConstant.ROOM_TYPE_INNING == roomType || CommonConstant.ROOM_TYPE_FREE == roomType) {// 俱乐部
				JSONObject object = DBUtil.getObjectBySQL(
						"SELECT g.`id`,f.`circle_code`,z.`account`FROM game_circle_member g LEFT JOIN game_circle_info f ON g.`circle_id`=f.`id`LEFT JOIN za_users z ON g.`user_id`=z.`id`WHERE f.`circle_code`= ? AND z.`account`=? AND g.`is_delete`='N'AND(g.`user_role`=1 OR g.`is_admin`=1)",
						new Object[] { circleCode, account });
				if (null == object || !object.containsKey("id") || !object.containsKey("circle_code")) {
					JSONObject result = new JSONObject();
					result.put("type", CommonConstant.SHOW_MSG_TYPE_NORMAL);
					result.put(CommonConstant.RESULT_KEY_MSG, "只有管理员才能发起解散");
					CommonConstant.sendMsgEvent(client, result, "tipMsgPush");
					return;
				}
			} else if (CommonConstant.ROOM_TYPE_TEA == roomType) {// 茶楼
				// 楼客权限 验证 start
				JSONObject teaMember = DBUtil.getObjectBySQL(
						"SELECT t.`powern`FROM za_users z LEFT JOIN tea_member t ON z.`id`=t.`users_id`WHERE z.`account`= ? AND t.`tea_info_code` = ? AND t.`audit`= ? AND t.`is_deleted`= ?",
						new Object[] { account, circleCode, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N });
				if (null == teaMember || !teaMember.containsKey("powern")
						|| TeaConstant.TEA_MEMBER_POWERN_PTCY == teaMember.getInt("powern")) {
					JSONObject result = new JSONObject();
					result.put("type", CommonConstant.SHOW_MSG_TYPE_NORMAL);
					result.put(CommonConstant.RESULT_KEY_MSG, "只有管理员才能发起解散");
					CommonConstant.sendMsgEvent(client, result, "tipMsgPush");
					return;
				}
				// 楼客权限 验证 end
			}
		}

		SSSUserPacket up = room.getUserPacketMap().get(account);
		Playerinfo playerinfo = room.getPlayerMap().get(account);
		if (null == up || null == playerinfo || CommonConstant.ROOM_VISIT_INDEX == playerinfo.getMyIndex())
			return;
		if (postData.containsKey("type")) {
			JSONObject result = new JSONObject();
			int type = postData.getInt("type");
			playerinfo.setIsCloseRoom(type);
			// 有人发起解散设置解散时间
			if (type == CommonConstant.CLOSE_ROOM_AGREE && room.getJieSanTime() == 0) {
				room.setJieSanTime(60);
				ThreadPoolHelper.executorService.submit(new Runnable() {
					@Override
					public void run() {
						gameTimerSSS.closeRoomOverTime(roomNo, room.getJieSanTime());
					}
				});
			}

			// 有人拒绝解散
			if (type == CommonConstant.CLOSE_ROOM_DISAGREE) {
				// 重置解散
				room.setJieSanTime(0);
				room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_GAME);// 游戏中
				// 设置玩家为未确认状态
				for (String uuid : room.getUserPacketMap().keySet()) {
					if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
						room.getPlayerMap().get(uuid).setIsCloseRoom(CommonConstant.CLOSE_ROOM_UNSURE);
					}
				}
				// 通知玩家
				result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
				String[] names = { playerinfo.getName() };
				result.put("names", names);
				CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush_SSS");
				return;
			}
			if (type == CommonConstant.CLOSE_ROOM_AGREE) {
				// 全部同意解散
				if (room.isAgreeClose()) {
					room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE);
					// 未玩完一局不需要强制结算
					if (!room.isNeedFinalSummary()) {
						// 所有玩家
						List<UUID> uuidList = room.getAllUUIDList();
						room.setNeedFinalSummary(true);
						// 通知玩家
						result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
						result.put(CommonConstant.RESULT_KEY_MSG, "解散房间成功");
						CommonConstant.sendMsgEventToAll(uuidList, result.toString(), "tipMsgPush");
						finalSummaryRoom(roomNo);// 总结算房间处理
						return;
					}
					compulsorySettlement(room.getRoomNo());// 局数超过 强制结算
					return;
				} else {// 刷新数据
					room.getPlayerMap().get(account).setIsCloseRoom(CommonConstant.CLOSE_ROOM_AGREE);
					result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
					result.put("data", room.getJieSanData());
					CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush_SSS");
				}
			}
		}
	}

	public void reconnectGame(SocketIOClient client, Object data) {
		System.out.println("从新连接=============");
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
			result.put("type", CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEventToSingle(client, result.toString(), "reconnectGamePush_SSS");
			return;
		}
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		// 不在当前房间内
		if (Dto.stringIsNULL(account)
				|| !room.getPlayerMap().containsKey(account) && !room.getVisitPlayerMap().containsKey(account)) {
			result.put("type", CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEventToSingle(client, result.toString(), "reconnectGamePush_SSS");
			return;
		}
		// 刷新uuid
		if (room.getPlayerMap().containsKey(account)) {
			room.getPlayerMap().get(account).setUuid(client.getSessionId());
		}
		if (room.getVisitPlayerMap().containsKey(account)) {
			room.getVisitPlayerMap().get(account).setUuid(client.getSessionId());
		}

		// 组织数据，通知玩家
		result.put("type", 1);
		result.put("data", obtainRoomData(roomNo, account));
		// 通知玩家
		CommonConstant.sendMsgEventToSingle(client, result.toString(), "reconnectGamePush_SSS");
	}

	public void changeGameStatus(SSSGameRoomNew room) {
		if (null == room)
			return;
		JSONObject obj = new JSONObject();
		obj.put("gameStatus", room.getGameStatus());
		obj.put("users", room.getAllPlayer());
		if (room.getBankerType() == SSSConstant.SSS_BANKER_TYPE_BWZ
				|| room.getBankerType() == SSSConstant.SSS_BANKER_TYPE_ZZ) {
			if (!Dto.stringIsNULL(room.getBanker()) && room.getPlayerMap().get(room.getBanker()) != null) {
				obj.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
			} else {
				obj.put("zhuang", CommonConstant.NO_BANKER_INDEX);
			}
		} else if (room.getBankerType() == SSSConstant.SSS_BANKER_TYPE_HB) {
			obj.put("zhuang", CommonConstant.NO_BANKER_INDEX);
		}
		obj.put("game_index", room.getGameNewIndex());
		obj.put("showTimer", CommonConstant.GLOBAL_NO);
		if (room.getTimeLeft() > SSSConstant.SSS_TIMER_INIT) {
			obj.put("showTimer", CommonConstant.GLOBAL_YES);
		}
		if (room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_COMPARE) {
			obj.put("bipaiTimer", 0);
			obj.put("showTimer", CommonConstant.GLOBAL_NO);
		}
		obj.put("timer", room.getTimeLeft());
		if (room.getMaPaiType() > 0 && !Dto.stringIsNULL(room.getMaPai())) {
			String[] ma = room.getMaPai().split("-");
			obj.put("mapai", Integer.valueOf(ma[1]) + 20 * (Integer.valueOf(ma[0]) - 1));
		} else {
			obj.put("mapai", 0);
		}
		obj.put("maPaiAccount", room.getMaPaiAccount());// 得到马牌的玩家
		obj.put("maPaiIndex", room.getMaPaiIndex());// 得到马牌的玩家

		obj.put("gameData", room.obtainGameData());
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_DK
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING
				|| CommonConstant.ROOM_TYPE_TEA == room.getRoomType()) {
			if (room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_FINAL_SUMMARY) {
				obj.put("jiesuanData", room.obtainFinalSummaryData());// 结算通知
			}
			if (room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_SUMMARY
					&& room.getGameIndex() >= room.getGameCount()) {
				obj.put("jiesuanData", room.obtainFinalSummaryData());
			}
		}
		// 坐庄模式
		obj.put("isBanker", CommonConstant.GLOBAL_NO);

		for (String account : room.getPlayerMap().keySet()) {
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (null == playerinfo)
				continue;

			if (room.getBankerType() == SSSConstant.SSS_BANKER_TYPE_ZZ) {
				// 上庄阶段或结算阶段庄家分数不足
				if (room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_TO_BE_BANKER
						|| (room.getRoomType() != CommonConstant.ROOM_TYPE_FK
								&& room.getRoomType() != CommonConstant.ROOM_TYPE_INNING
								&& room.getRoomType() != CommonConstant.ROOM_TYPE_TEA
								&& room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_SUMMARY
								&& room.getPlayerMap().get(room.getBanker()).getScore() < room.getMinBankerScore())) {
					obj.put("isBanker", CommonConstant.GLOBAL_YES);
					obj.put("bankerMinScore", room.getMinBankerScore());
					obj.put("bankerIsUse", CommonConstant.GLOBAL_YES);
					if (room.getPlayerMap().get(account).getScore() < room.getMinBankerScore()) {
						obj.put("bankerIsUse", CommonConstant.GLOBAL_NO);
					}
				}
				if (room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_XZ) {
					obj.put("baseNum", room.getBaseNumTimes(room.getPlayerMap().get(account).getScore()));
				}
			}
			SSSUserPacket up = room.getUserPacketMap().get(account);
			if (null != up) {
				obj.put("myPai", up.getMyPai());
				obj.put("myPaiType", up.getPaiType());
			}
			obj.put("roomNo", room.getRoomNo());
			// 判断是否是切牌模式
			if (SSSConstant.SSS_CUT_CARD_MODE_YES.equals(room.getCutCardMode())) {
				obj.put("cutCardAccount", room.getCutCardAccount());
			}
			CommonConstant.sendMsgEventToSingle(playerinfo.getUuid(), obj.toString(), "changeGameStatusPush_SSS");
		}

		for (String account : room.getVisitPlayerMap().keySet()) {// 观战玩家
			Playerinfo playerinfo = room.getVisitPlayerMap().get(account);
			if (null == playerinfo)
				continue;

			if (room.getBankerType() == SSSConstant.SSS_BANKER_TYPE_ZZ) {
				// 上庄阶段或结算阶段庄家分数不足
				if (room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_TO_BE_BANKER
						|| (room.getRoomType() != CommonConstant.ROOM_TYPE_FK
								&& room.getRoomType() != CommonConstant.ROOM_TYPE_INNING
								&& room.getRoomType() != CommonConstant.ROOM_TYPE_TEA
								&& room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_SUMMARY
								&& room.getPlayerMap().get(room.getBanker()).getScore() < room.getMinBankerScore())) {
					obj.put("isBanker", CommonConstant.GLOBAL_YES);
					obj.put("bankerMinScore", room.getMinBankerScore());
					obj.put("bankerIsUse", CommonConstant.GLOBAL_NO);
				}
				if (room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_XZ) {
					obj.put("baseNum", room.getBaseNumTimes(0D));
				}
			}
			obj.put("myPai", "");
			obj.put("myPaiType", "0");
			obj.put("roomNo", room.getRoomNo());
			// 判断是否是切牌模式
			if (SSSConstant.SSS_CUT_CARD_MODE_YES.equals(room.getCutCardMode())) {
				obj.put("cutCardAccount", room.getCutCardAccount());
			}
			CommonConstant.sendMsgEventToSingle(playerinfo.getUuid(), obj.toString(), "changeGameStatusPush_SSS");
		}

		if (room.isRobot()) {
			for (String robotAccount : room.getRobotList()) {
				int delayTime = RandomUtils.nextInt(10) + 5;
				if (room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_SUMMARY) {
					/*
					 * Random r = new Random(); int sleepTime = r.nextInt(20)+1; try {
					 * Thread.sleep(sleepTime); } catch (InterruptedException e) { // TODO
					 * Auto-generated catch block e.printStackTrace(); }
					 */
					AppBeanUtil.getBean(RobotEventDeal.class).changeRobotActionDetail(robotAccount,
							SSSConstant.SSS_GAME_EVENT_READY, delayTime);
				}
				if (room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_GAME_EVENT) {
					AppBeanUtil.getBean(RobotEventDeal.class).changeRobotActionDetail(robotAccount,
							SSSConstant.SSS_GAME_EVENT_EVENT, delayTime);
				}
			}
		}
	}

	public JSONObject obtainRoomData(String roomNo, String account) {
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		//Playerinfo playerinfo = room.getPlayerMap().get(account);
		// 获取pdk背景设置 如果没有 则为1
		//System.out.println(playerinfo.getId());
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
			bg = jb.getString("sssbg");
		}

		if (null == room)
			return null;
		JSONObject roomData = new JSONObject();
		roomData.put("ruanguiCount", room.getRuanguiCount());
		roomData.put("bg", bg);
		roomData.put("playerCount", room.getPlayerCount());
		roomData.put("gameStatus", room.getGameStatus());
		roomData.put("speedMode", room.getSpeedMode());
		roomData.put("cutCardAccount", room.getCutCardAccount());
		roomData.put("cutCardMode", room.getCutCardMode());
		roomData.put("room_no", room.getRoomNo());
		roomData.put("roomNo", room.getRoomNo());
		roomData.put("roomType", room.getRoomType());
		roomData.put("game_count", room.getGameCount());
		roomData.put("di", room.getScore());
		roomData.put("hardGhostNumber", room.getHardGhostNumber());
		roomData.put("clubCode", room.getClubCode());
		if (room.getMaPaiType() > 0 && !Dto.stringIsNULL(room.getMaPai())) {
			String[] ma = room.getMaPai().split("-");
			roomData.put("mapai", Integer.valueOf(ma[1]) + 20 * (Integer.valueOf(ma[0]) - 1));
		} else {
			roomData.put("mapai", 0);
		}
		roomData.put("maPaiAccount", room.getMaPaiAccount());// 得到马牌的玩家
		roomData.put("maPaiIndex", room.getMaPaiIndex());// 得到马牌的玩家
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB || room.getRoomType() == CommonConstant.ROOM_TYPE_FREE) {
			StringBuffer roomInfo = new StringBuffer();
			roomInfo.append("房号:");
			roomInfo.append(room.getRoomNo());
			roomInfo.append(" 最低");
			roomInfo.append(room.getMinPlayer());
			roomInfo.append("人 ");
			StringBuffer roomInfo22 = new StringBuffer();
			if (room.getColor() == 0) {
				roomInfo.append("不加色 ");
				roomInfo22.append("不加色 ");
			}
			if (room.getColor() == 1) {
				roomInfo.append("加一色 ");
				roomInfo22.append("加一色 ");
			}
			if (room.getColor() == 2) {
				roomInfo.append("加两色 ");
				roomInfo22.append("加一色 ");
			}
			roomData.put(SSSConstant.SSS_DATA_KET_SAME_COLOR, SSSConstant.SSS_SAME_COLOR_NO);
			roomData.put(SSSConstant.SSS_DATA_KET_FEATURE, SSSConstant.SSS_FEATURE_WU);
			if (room.getRoomInfo().containsKey(SSSConstant.SSS_DATA_KET_SAME_COLOR)) {
				if (SSSConstant.SSS_SAME_COLOR_YES
						.equals(room.getRoomInfo().getString(SSSConstant.SSS_DATA_KET_SAME_COLOR))) {
					roomInfo.append("清一色 ");
					roomData.put(SSSConstant.SSS_DATA_KET_SAME_COLOR, SSSConstant.SSS_SAME_COLOR_YES);
					if (room.getRoomInfo().containsKey(SSSConstant.SSS_DATA_KET_FLOWER_COLOR)) {
						String flowerColor = room.getRoomInfo().getString(SSSConstant.SSS_DATA_KET_FLOWER_COLOR);
						if (SSSConstant.SSS_FLOWER_COLOR_HEITAO.equals(flowerColor)) {
							roomInfo.append("黑桃 ");
							roomData.put(SSSConstant.SSS_DATA_KET_FLOWER_COLOR, SSSConstant.SSS_FLOWER_COLOR_HEITAO);
						} else if (SSSConstant.SSS_FLOWER_COLOR_TAOHUA.equals(flowerColor)) {
							roomInfo.append("红桃 ");
							roomData.put(SSSConstant.SSS_DATA_KET_FLOWER_COLOR, SSSConstant.SSS_FLOWER_COLOR_TAOHUA);
						} else if (SSSConstant.SSS_FLOWER_COLOR_MEIHUA.equals(flowerColor)) {
							roomInfo.append("梅花 ");
							roomData.put(SSSConstant.SSS_DATA_KET_FLOWER_COLOR, SSSConstant.SSS_FLOWER_COLOR_MEIHUA);
						} else if (SSSConstant.SSS_FLOWER_COLOR_FANGKUAI.equals(flowerColor)) {
							roomInfo.append("方块 ");
							roomData.put(SSSConstant.SSS_DATA_KET_FLOWER_COLOR, SSSConstant.SSS_FLOWER_COLOR_FANGKUAI);
						} else {
							roomInfo.append("黑桃 ");
							roomData.put(SSSConstant.SSS_DATA_KET_FLOWER_COLOR, SSSConstant.SSS_FLOWER_COLOR_HEITAO);
						}
					}
				}
			}
			if (room.getRoomInfo().containsKey(SSSConstant.SSS_DATA_KET_FEATURE)) {
				if (SSSConstant.SSS_FEATURE_LAIZI
						.equals(room.getRoomInfo().getString(SSSConstant.SSS_DATA_KET_FEATURE))) {
					roomInfo.append("癞子牌玩法 ");
					roomData.put(SSSConstant.SSS_DATA_KET_FEATURE, SSSConstant.SSS_FEATURE_LAIZI);
				}
			}
			roomInfo.append(room.getWfType());
			roomData.put("roominfo", roomInfo.toString());
			roomData.put("roominfo22",roomInfo22.toString());
			StringBuffer roomInfo2 = new StringBuffer();
			roomInfo2.append("底注:");
			roomInfo2.append(room.getScore());
			roomInfo2.append(" 进:");
			roomInfo2.append((int) room.getEnterScore());
			roomInfo2.append(" 出:");
			roomInfo2.append((int) room.getLeaveScore());
			roomData.put("roominfo2", roomInfo2.toString());
		}
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_DK
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_TEA) {
			StringBuffer roomInfo = new StringBuffer();
			StringBuffer roomInfo22 = new StringBuffer();
			roomInfo.append(room.getWfType()).append(" ");
			if (room.getScore() > 0D) {// 底分
				roomInfo.append("底分:").append(Dto.zerolearing(room.getScore()));
			}
			// 人数
			roomInfo.append(" 玩家人数：").append(room.getPlayerCount());

			if (room.getColor() == 1) {
				roomInfo.append(" 加一色");
				roomInfo22.append(" 加一色");
			} else if (room.getColor() == 2) {
				roomInfo.append(" 加两色");
				roomInfo22.append(" 加两色");
			}

			if (SSSConstant.SSS_FEATURE_LAIZI.equals(room.getFireScore())
					|| SSSConstant.SSS_FEATURE_LAIZI_TWO.equals(room.getFireScore())) {
				roomInfo.append(" 鬼牌2张");
				roomInfo22.append(" 鬼牌2张");
			} else if (SSSConstant.SSS_FEATURE_LAIZI_ONE.equals(room.getFireScore())) {
				roomInfo.append(" 鬼牌1张");
				roomInfo22.append(" 鬼牌2张");
			}

			if (CircleConstant.CUTTYPE_GD.equals(room.getCutType())) {
				roomInfo.append(" 固定消耗");
			} else if (CircleConstant.CUTTYPE_MC.equals(room.getCutType())) {
				roomInfo.append(" 名次消耗");
			}

			if (SSSConstant.TEA_FIRE_Y.equals(room.getFire())) {
				roomInfo.append(" 打枪/全垒打得分翻倍");
			} else if (SSSConstant.TEA_FIRE_N.equals(room.getFire())) {
				roomInfo.append(" 打枪/全垒打固定吃分");
			}

			if (room.getPattern() > 0) {
				roomInfo.append(" 配牌时间：").append(room.getPattern()).append("秒");
			} else {
				roomInfo.append(" 不限时");
			}

			if (SSSConstant.SSS_SPEED_MODE_YES.equals(room.getSpeedMode())) {
				roomInfo.append(" 极速模式 ");
			}

			if (CommonConstant.IS_ALL_START_Y.equals(room.getIsAllStart())) {
				roomInfo.append(" 满人开");
			}

			if (room.getHardGhostNumber() > 0) {
				roomInfo.append(" 硬鬼  ").append(room.getHardGhostNumber()).append(" 张");
				roomInfo22.append(" 硬鬼  ").append(room.getHardGhostNumber()).append(" 张");
			}

			if (room.getRoomInfo().containsKey(SSSConstant.SSS_DATA_KET_SAME_COLOR)) {
				if (SSSConstant.SSS_SAME_COLOR_YES
						.equals(room.getRoomInfo().getString(SSSConstant.SSS_DATA_KET_SAME_COLOR))) {
					roomInfo.append(" 清一色");
					roomData.put(SSSConstant.SSS_DATA_KET_SAME_COLOR, SSSConstant.SSS_SAME_COLOR_YES);
					if (room.getRoomInfo().containsKey(SSSConstant.SSS_DATA_KET_FLOWER_COLOR)) {
						String flowerColor = room.getRoomInfo().getString(SSSConstant.SSS_DATA_KET_FLOWER_COLOR);
						if (SSSConstant.SSS_FLOWER_COLOR_HEITAO.equals(flowerColor)) {
							roomInfo.append(" 黑桃");
							roomData.put(SSSConstant.SSS_DATA_KET_FLOWER_COLOR, SSSConstant.SSS_FLOWER_COLOR_HEITAO);
						} else if (SSSConstant.SSS_FLOWER_COLOR_TAOHUA.equals(flowerColor)) {
							roomInfo.append(" 红桃");
							roomData.put(SSSConstant.SSS_DATA_KET_FLOWER_COLOR, SSSConstant.SSS_FLOWER_COLOR_TAOHUA);
						} else if (SSSConstant.SSS_FLOWER_COLOR_MEIHUA.equals(flowerColor)) {
							roomInfo.append(" 梅花");
							roomData.put(SSSConstant.SSS_DATA_KET_FLOWER_COLOR, SSSConstant.SSS_FLOWER_COLOR_MEIHUA);
						} else if (SSSConstant.SSS_FLOWER_COLOR_FANGKUAI.equals(flowerColor)) {
							roomInfo.append(" 方块");
							roomData.put(SSSConstant.SSS_DATA_KET_FLOWER_COLOR, SSSConstant.SSS_FLOWER_COLOR_FANGKUAI);
						} else {
							roomInfo.append(" 黑桃");
							roomData.put(SSSConstant.SSS_DATA_KET_FLOWER_COLOR, SSSConstant.SSS_FLOWER_COLOR_HEITAO);
						}
					}
				}
			}

			roomData.put("roominfo", roomInfo.toString());
			roomData.put("roominfo22", roomInfo22.toString());
			
		}
		roomData.put("zhuang", CommonConstant.NO_BANKER_INDEX);
		if (!Dto.stringIsNULL(room.getBanker()) && room.getBankerType() != SSSConstant.SSS_BANKER_TYPE_HB
				&& room.getPlayerMap().get(room.getBanker()) != null) {
			roomData.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
		}
		roomData.put("game_index", room.getGameNewIndex());
		roomData.put("showTimer", CommonConstant.GLOBAL_NO);
		if (room.getTimeLeft() > SSSConstant.SSS_TIMER_INIT) {
			roomData.put("showTimer", CommonConstant.GLOBAL_YES);
		}
		if (room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_COMPARE) {
			roomData.put("showTimer", CommonConstant.GLOBAL_NO);
			roomData.put("bipaiTimer", room.getCompareTimer() * 100);
		}
		roomData.put("timer", room.getTimeLeft());
		int myIndex = CommonConstant.ROOM_VISIT_INDEX;
		if (room.getPlayerMap().containsKey(account)) {
			myIndex = room.getPlayerMap().get(account).getMyIndex();
		}
		roomData.put("myIndex", myIndex);
		roomData.put("users", room.getAllPlayer());

		SSSUserPacket up = room.getUserPacketMap().get(account);
		if (null != up) {
			roomData.put("myPai", up.getMyPai());
			roomData.put("myPaiType", up.getPaiType());
		}

		roomData.put("gameData", room.obtainGameData());
		if (room.getJieSanTime() > 0) {
			roomData.put("jiesan", CommonConstant.GLOBAL_YES);
			roomData.put("jiesanData", room.getJieSanData());
		}
		if (room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_FINAL_SUMMARY) {
			roomData.put("jiesuanData", room.obtainFinalSummaryData());
		}
		if (room.getBankerType() == SSSConstant.SSS_BANKER_TYPE_ZZ) {
			if (room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_TO_BE_BANKER) {
				roomData.put("bankerMinScore", room.getMinBankerScore());
				roomData.put("bankerIsUse", CommonConstant.GLOBAL_NO);
				if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK
						|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING
						|| room.getRoomType() == CommonConstant.ROOM_TYPE_TEA
						|| room.getPlayerMap().containsKey(account)
								&& room.getPlayerMap().get(account).getScore() >= room.getMinBankerScore()) {
					roomData.put("bankerIsUse", CommonConstant.GLOBAL_YES);
				}
			}
			roomData.put("baseNum", room.getBaseNumTimes(
					room.getPlayerMap().containsKey(account) ? room.getPlayerMap().get(account).getScore() : 0D));
			roomData.put("xiazhu", room.obtainXzResult());
		}
		roomData.put("startIndex", getStartIndex(roomNo));
		return roomData;
	}

	public void gameSummaryHb(String roomNo) {
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		// 切牌玩家
		String cutCardAccount = "-1";
		int minScore = 1;
		if (null == room)
			return;
		// 获取所有参与的玩家
		List<String> gameList = new ArrayList<String>();
		for (String account : room.getUserPacketMap().keySet()) {
			SSSUserPacket up = room.getUserPacketMap().get(account);
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (null == up || null == playerinfo)
				continue;
			if (SSSConstant.SSS_USER_STATUS_INIT != up.getStatus()) {
				gameList.add(account);
			}
		}
		// 是否全垒打
		boolean isSwat = false;
		for (String account : gameList) {
			SSSUserPacket up = room.getUserPacketMap().get(account);
			// 赢几个人
			int winPlayer = 0;
			int special = up.getPaiType();
			if (special > 0) {
				break;
			}
			for (String other : gameList) {
				if (!other.equals(account)) {
					SSSUserPacket otherPlayer = room.getUserPacketMap().get(other);
					int otherSpecial = otherPlayer.getPaiType();
					if (otherSpecial > 0) {
						break;
					} else {
						// 比牌结果
						JSONObject compareResult = SSSComputeCards.compare(up.getPai(), otherPlayer.getPai(), room);
						// 自己的牌型及分数
						JSONArray mySingleResult = JSONArray.fromObject(compareResult.getJSONArray("result").get(0));
						// 其他玩家的牌型及分数
						JSONArray otherSingleResult = JSONArray.fromObject(compareResult.getJSONArray("result").get(1));
						int winTime = 0;
						for (int i = 0; i < mySingleResult.size(); i++) {
							if (mySingleResult.getJSONObject(i).getInt("score") > otherSingleResult.getJSONObject(i)
									.getInt("score")) {
								winTime++;
							}
						}
						// 三道全赢打枪
						if (winTime == 3) {
							winPlayer++;
						} else {
							break;
						}
					}
				}
			}
			int minSwatCount = SSSConstant.SSS_MIN_SWAT_COUNT;
			if (!Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("qld")) {
				minSwatCount = room.getSetting().getInt("qld");
			}
			if (gameList.size() >= minSwatCount && winPlayer == gameList.size() - 1) {
				room.setSwat(CommonConstant.GLOBAL_YES);
				isSwat = true;
				room.getUserPacketMap().get(account).setSwat(CommonConstant.GLOBAL_YES);
				break;
			}
		}
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (String account : gameList) {
			// 当局总计输赢
			int sumScoreAll = 0;
			// 自己
			SSSUserPacket up = room.getUserPacketMap().get(account);
			// 自己是否是特殊牌
			int special = up.getPaiType();
			// 单局结果
			JSONArray myResult = new JSONArray();
			// 头道
			myResult.add(new JSONObject().element("pai", up.getHeadPai()).element("score", 0).element("type", 0));
			// 中道
			myResult.add(new JSONObject().element("pai", up.getMidPai()).element("score", 0).element("type", 0));
			// 尾道
			myResult.add(new JSONObject().element("pai", up.getFootPai()).element("score", 0).element("type", 0));
			List<Integer> allWinList = new ArrayList<Integer>();
			for (String other : gameList) {
				// 单个玩家输赢
				int sumScoreSingle = 0;
				if (!other.equals(account)) {
					// 其他玩家
					SSSUserPacket otherPlayer = room.getUserPacketMap().get(other);
					int otherSpecial = otherPlayer.getPaiType();
					if (special > 0 && otherSpecial > 0) {
						if (up.getPaiScore() > otherPlayer.getPaiScore()) {
							sumScoreSingle += up.getPaiScore();
						} else if (up.getPaiScore() < otherPlayer.getPaiScore()) {
							sumScoreSingle -= otherPlayer.getPaiScore();
						}
					} else if (special > 0 && otherSpecial == 0) {
						sumScoreSingle += up.getPaiScore();
					} else if (special == 0 && otherSpecial > 0) {
						sumScoreSingle -= otherPlayer.getPaiScore();
					} else {
						// 比牌结果
						JSONObject compareResult = SSSComputeCards.compare(up.getPai(), otherPlayer.getPai(), room);
						// 自己的牌型及分数
						JSONArray mySingleResult = JSONArray.fromObject(compareResult.getJSONArray("result").get(0));
						// 其他玩家的牌型及分数
						JSONArray otherSingleResult = JSONArray.fromObject(compareResult.getJSONArray("result").get(1));
						int winTime = 0;
						for (int i = 0; i < myResult.size(); i++) {
							// 增加相应的分数
							myResult.getJSONObject(i).put("score", myResult.getJSONObject(i).getInt("score")
									+ mySingleResult.getJSONObject(i).getInt("score"));
							// 设置没道对应的牌型
							myResult.getJSONObject(i).put("type", mySingleResult.getJSONObject(i).getInt("type"));
							if (mySingleResult.getJSONObject(i).getInt("score") > otherSingleResult.getJSONObject(i)
									.getInt("score")) {
								winTime++;
							} else if (mySingleResult.getJSONObject(i).getInt("score") < otherSingleResult
									.getJSONObject(i).getInt("score")) {
								winTime--;
							}
						}
						sumScoreSingle = compareResult.getInt("A");
						if (room.getMaPaiType() > 0 && !Dto.stringIsNULL(room.getMaPai())) {
							// 马牌处理
							sumScoreSingle = maPaiScore(sumScoreSingle, account, room.getMaPaiAccount());// 自己
							sumScoreSingle = maPaiScore(sumScoreSingle, other, room.getMaPaiAccount());// 其他人
						}
						// 三道全赢打枪
						if (winTime == 3) {
							// 打枪得分处理
							sumScoreSingle = fireScore(sumScoreSingle, room.getFire(), room.getFireScore());
							allWinList.add(room.getPlayerMap().get(other).getMyIndex());
							JSONArray dq = new JSONArray();
							dq.add(room.getPlayerMap().get(account).getMyIndex());
							dq.add(room.getPlayerMap().get(other).getMyIndex());
							if (!room.getDqArray().contains(dq)) {
								room.getDqArray().add(dq);
							}
						}
						// 三道全输被打枪
						if (winTime == -3) {
							// 打枪得分处理
							sumScoreSingle = fireScore(sumScoreSingle, room.getFire(), room.getFireScore());
						}
						// 全垒打只对全垒打的人翻倍
						if (isSwat) {
							// 自己或者他人全垒打
							if (room.getUserPacketMap().get(account).getSwat() == CommonConstant.GLOBAL_YES
									|| room.getUserPacketMap().get(other).getSwat() == CommonConstant.GLOBAL_YES) {
								// 打枪得分处理 全垒打
								sumScoreSingle = fireScore(sumScoreSingle, room.getFire(), room.getFireScore() * 3);
							}
						}
					}
					sumScoreAll += sumScoreSingle;
				}
			
			}
			// 获取大输家
			if (sumScoreAll < minScore) {
				cutCardAccount = account;
				minScore = sumScoreAll;
				// 如果相同则随机选取输家
			} else if (sumScoreAll == minScore) {
				Random random = new Random();
				int temp = random.nextInt(2);
				if (temp > 0) {
					cutCardAccount = account;
				}
			}
			// 设置玩家当局输赢
			room.getUserPacketMap().get(account).setScore(sumScoreAll * room.getScore());
			// 设置头道输赢情况
			room.getUserPacketMap().get(account).setHeadResult(myResult.getJSONObject(0));
			// 设置中道输赢情况
			room.getUserPacketMap().get(account).setMidResult(myResult.getJSONObject(1));
			// 设置尾道输赢情况
			room.getUserPacketMap().get(account).setFootResult(myResult.getJSONObject(2));
		}
		// 设置切牌玩家
		room.setCutCardAccount(cutCardAccount);
	}

	private int maPaiScore(int score, String account, String maPaiAccount) {
		// 马牌翻倍
		if (account.equals(maPaiAccount)) {
			score *= 2;
		}
		return score;
	}

	private int fireScore(int score, String fire, int fireScore) {
		if (SSSConstant.FIRE_N.equals(fire)) {// 全垒打固定吃分
			if (score < 0) {
				score -= fireScore;
			} else {
				score += fireScore;
			}
		} else {// 全垒打翻倍
			score *= 2;
		}
		return score;
	}

	private double fireScore(double score, String fire, int fireScore) {
		if (SSSConstant.FIRE_N.equals(fire)) {// 全垒打固定吃分
			if (score < 0D) {
				score = Dto.sub(score, fireScore);
			} else {
				score = Dto.add(score, fireScore);
			}
		} else {// 全垒打翻倍
			score = Dto.mul(score, 2);
		}
		return score;
	}

	public void gameSummaryBwzOrZZ(String roomNo) {
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		// 获取所有参与的玩家
		List<String> gameList = new ArrayList<String>();
		for (String account : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
				if (room.getUserPacketMap().get(account).getStatus() != SSSConstant.SSS_USER_STATUS_INIT) {
					gameList.add(account);
				}
			}
		}
		// 是否全垒打
		boolean isSwat = true;
		int minSwatCount = SSSConstant.SSS_MIN_SWAT_COUNT;
		if (!Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("qld")) {
			minSwatCount = room.getSetting().getInt("qld");
		}
		if (gameList.size() < minSwatCount || room.getBankerType() == SSSConstant.SSS_BANKER_TYPE_ZZ) {
			isSwat = false;
		}
		// 庄家
		SSSUserPacket up = room.getUserPacketMap().get(room.getBanker());
		// 庄家输赢
		double sumScoreBanker = 0;
		// 庄家是否是特殊牌
		int special = up.getPaiType();
		// 单局结果
		JSONArray bankerResult = new JSONArray();
		// 头道
		bankerResult.add(new JSONObject().element("pai", up.getHeadPai()).element("score", 0).element("type", 0));
		// 中道
		bankerResult.add(new JSONObject().element("pai", up.getMidPai()).element("score", 0).element("type", 0));
		// 尾道
		bankerResult.add(new JSONObject().element("pai", up.getFootPai()).element("score", 0).element("type", 0));
		for (String account : gameList) {
			if (!account.equals(room.getBanker())) {
				// 闲家
				SSSUserPacket otherPlayer = room.getUserPacketMap().get(account);
				// 闲家输赢
				int sumScoreOther = 0;
				// 闲家是否是特殊牌
				int otherSpecial = otherPlayer.getPaiType();
				// 单局结果
				JSONArray otherResult = new JSONArray();
				// 头道
				otherResult.add(new JSONObject().element("pai", otherPlayer.getHeadPai()).element("score", 0)
						.element("type", 0));
				// 中道
				otherResult.add(new JSONObject().element("pai", otherPlayer.getMidPai()).element("score", 0)
						.element("type", 0));
				// 尾道
				otherResult.add(new JSONObject().element("pai", otherPlayer.getFootPai()).element("score", 0)
						.element("type", 0));
				if (special > 0 && otherSpecial > 0) {
					if (up.getPaiScore() > otherPlayer.getPaiScore()) {
						sumScoreBanker += up.getPaiScore();
						sumScoreOther -= up.getPaiScore();
					} else if (up.getPaiScore() < otherPlayer.getPaiScore()) {
						sumScoreBanker -= otherPlayer.getPaiScore();
						sumScoreOther += otherPlayer.getPaiScore();
					}
					isSwat = false;
				} else if (special > 0 && otherSpecial == 0) {
					sumScoreBanker += up.getPaiScore();
					sumScoreOther -= up.getPaiScore();
					isSwat = false;
				} else if (special == 0 && otherSpecial > 0) {
					sumScoreBanker -= otherPlayer.getPaiScore();
					sumScoreOther += otherPlayer.getPaiScore();
					isSwat = false;
				} else {
					// 比牌结果
					JSONObject compareResult = SSSComputeCards.compare(up.getPai(), otherPlayer.getPai(), room);
					// 自己的牌型及分数
					JSONArray bankerSingleResult = JSONArray.fromObject(compareResult.getJSONArray("result").get(0));
					// 其他玩家的牌型及分数
					JSONArray otherSingleResult = JSONArray.fromObject(compareResult.getJSONArray("result").get(1));
					int winTime = 0;
					for (int i = 0; i < otherResult.size(); i++) {
						// 增加相应的分数
						bankerResult.getJSONObject(i).put("score", bankerResult.getJSONObject(i).getInt("score")
								+ bankerSingleResult.getJSONObject(i).getInt("score"));
						otherResult.getJSONObject(i).put("score", otherResult.getJSONObject(i).getInt("score")
								+ otherSingleResult.getJSONObject(i).getInt("score"));
						// 设置没道对应的牌型
						otherResult.getJSONObject(i).put("type", otherSingleResult.getJSONObject(i).getInt("type"));
						bankerResult.getJSONObject(i).put("type", bankerSingleResult.getJSONObject(i).getInt("type"));
						if (bankerSingleResult.getJSONObject(i).getInt("score") > otherSingleResult.getJSONObject(i)
								.getInt("score")) {
							winTime++;
						} else if (bankerSingleResult.getJSONObject(i).getInt("score") < otherSingleResult
								.getJSONObject(i).getInt("score")) {
							winTime--;
						}
					}
					sumScoreOther = compareResult.getInt("B");
					if (room.getBankerType() == SSSConstant.SSS_BANKER_TYPE_ZZ) {
						sumScoreOther = compareResult.getInt("B") * room.getUserPacketMap().get(account).getXzTimes();
					}
					if (room.getMaPaiType() > 0 && !Dto.stringIsNULL(room.getMaPai())) {
						// 马牌处理
						sumScoreOther = maPaiScore(sumScoreOther, room.getBanker(), room.getMaPaiAccount());// 庄家
						sumScoreOther = maPaiScore(sumScoreOther, account, room.getMaPaiAccount());// 其他人
					}

					// 三道全赢打枪
					if (winTime == 3) {
						// 打枪得分处理
						sumScoreOther = fireScore(sumScoreOther, room.getFire(), room.getFireScore());
						JSONArray dq = new JSONArray();
						dq.add(room.getPlayerMap().get(room.getBanker()).getMyIndex());
						dq.add(room.getPlayerMap().get(account).getMyIndex());
						if (!room.getDqArray().contains(dq)) {
							room.getDqArray().add(dq);
						}
					} else {
						isSwat = false;
					}
					// 三道全输被打枪
					if (winTime == -3) {
						// 打枪得分处理
						sumScoreOther = fireScore(sumScoreOther, room.getFire(), room.getFireScore());
						JSONArray dq = new JSONArray();
						dq.add(room.getPlayerMap().get(account).getMyIndex());
						dq.add(room.getPlayerMap().get(room.getBanker()).getMyIndex());
						if (!room.getDqArray().contains(dq)) {
							room.getDqArray().add(dq);
						}
					}
					sumScoreBanker -= sumScoreOther;
				}
				// 设置闲家家当局输赢
				room.getUserPacketMap().get(account).setScore(sumScoreOther * room.getScore());
				// 设置头道输赢情况
				room.getUserPacketMap().get(account).setHeadResult(otherResult.getJSONObject(0));
				// 设置中道输赢情况
				room.getUserPacketMap().get(account).setMidResult(otherResult.getJSONObject(1));
				// 设置尾道输赢情况
				room.getUserPacketMap().get(account).setFootResult(otherResult.getJSONObject(2));
			}
		}
		// 设置闲家家当局输赢
		room.getUserPacketMap().get(room.getBanker()).setScore(sumScoreBanker * room.getScore());
		// 设置头道输赢情况
		room.getUserPacketMap().get(room.getBanker()).setHeadResult(bankerResult.getJSONObject(0));
		// 设置中道输赢情况
		room.getUserPacketMap().get(room.getBanker()).setMidResult(bankerResult.getJSONObject(1));
		// 设置尾道输赢情况
		room.getUserPacketMap().get(room.getBanker()).setFootResult(bankerResult.getJSONObject(2));
		// 全垒打翻倍
		if (isSwat) {
			room.setSwat(CommonConstant.GLOBAL_YES);
			for (String account : room.getUserPacketMap().keySet()) {
				if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
					double nowScore = room.getUserPacketMap().get(account).getScore();

					// 打枪得分处理 全垒打
					fireScore(nowScore, room.getFire(), room.getFireScore() * 3);
					room.getUserPacketMap().get(account).setScore(nowScore);

					if (account.equals(room.getBanker())) {
						room.getUserPacketMap().get(account).setSwat(CommonConstant.GLOBAL_YES);
					}
				}
			}
		}
	}

	private int getStartIndex(String roomNo) {
		GameRoom room = RoomManage.gameRoomMap.get(roomNo);
		if (null != room) {
			int roomType = room.getRoomType();
			// 房卡场或俱乐部
			if (CommonConstant.ROOM_TYPE_FK == roomType || CommonConstant.ROOM_TYPE_CLUB == roomType
					|| CommonConstant.ROOM_TYPE_INNING == roomType) {
				if (null != room.getOwner() && !"-1".equals(room.getOwner())) {
					Playerinfo playerinfo = room.getPlayerMap().get(room.getOwner());
					if (null != playerinfo && CommonConstant.ROOM_VISIT_INDEX != playerinfo.getMyIndex()
							&& !playerinfo.isCollapse()) {
						return playerinfo.getMyIndex();
					}
				}
			}
		}
		return CommonConstant.NO_BANKER_INDEX;
	}

	private void bulkInsertGameStatis(String roomNo) {
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room) {
			return;
		}

		double maxScore = 0D;// 大赢家

		for (String account : room.getUserPacketMap().keySet()) {
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
		for (String account : room.getUserPacketMap().keySet()) {
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			SSSUserPacket up = room.getUserPacketMap().get(account);
			if (null == playerinfo || null == up)
				continue;
			// 有参与的玩家
			if (!playerinfo.isCollapse() && SSSConstant.SSS_USER_STATUS_INIT == up.getStatus())
				continue;
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

	private void finalSummaryRoom(String roomNo) {
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
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
					clearRoomInfo(roomNo);// 处理清空房间，重新生成
					return;
				}
			}
		}
	}

	private void removeRoom(String roomNo) {
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		// 房间解散
		redisInfoService.delSummary(roomNo, "_SSS");
		JSONObject roomInfo = new JSONObject();
		roomInfo.put("room_no", room.getRoomNo());
		roomInfo.put("status", CommonConstant.CLOSE_ROOM_TYPE_OVER);
		roomInfo.put("game_index", 0);
		producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
		System.out.println("清除数组信息");
		RoomManage.gameRoomMap.remove(roomNo);
	}

	private void clearRoomInfo(String roomNo) {
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (room == null)
			return;
		room.initGame();
		room.clearGameRecord();
		room.setGameIndex(0);
		room.setGameNewIndex(0);
		room.getSummaryData().clear();
		room.setGameStatus(SSSConstant.SSS_GAME_STATUS_INIT);
		room.setJieSanTime(0);
		room.setTimeLeft(0);
		room.getFinalSummaryData().clear();
		room.setUserPacketMap(new ConcurrentHashMap<>());
		room.setPlayerMap(new ConcurrentHashMap<>());
		room.setVisitPlayerMap(new ConcurrentHashMap<>());
		for (int i = 0; i < room.getUserIdList().size(); i++) {
			room.getUserIdList().set(i, 0L);
			room.addIndexList(i);
		}

		// 处理清空房间，重新生成
		baseEventDeal.reloadClearRoom(roomNo);
	}

	public void compulsorySettlement(String roomNo) {
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (room == null)
			return;
		room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE);
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
			room.setOpen(false);
		}
		room.setGameStatus(SSSConstant.SSS_GAME_STATUS_FINAL_SUMMARY);

		// 更新数据库
		updateUserScore(room.getRoomNo());

		changeGameStatus(room);
		room.setNeedFinalSummary(true);
		finalSummaryRoom(room.getRoomNo());// 总结算房间处理
	}

	public void forcedRoom(Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!postData.containsKey(CommonConstant.DATA_KEY_ROOM_NO))
			return;
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		// 给房间里每人通知管理员解散房间
		JSONObject object = new JSONObject();
		object.put("type", CommonEventConstant.TIP_MSG_PUSH_TYPE_CPM);
		object.put(CommonConstant.RESULT_KEY_MSG, "管理员解散房间");
		CommonConstant.sendMsgEventAll(room.getAllUUIDList(), object, CommonEventConstant.TIP_MSG_PUSH);

		room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE);
		ConcurrentHashMap<String, Playerinfo> map = room.getPlayerMap();
		for (String account : room.getUserPacketMap().keySet()) {
			System.out.println(account + "存在：？" + map.contains(account));
			if (map.containsKey(account)) {
				map.get(account).setIsCloseRoom(CommonConstant.CLOSE_ROOM_AGREE);
			} else {
				System.out.println(account + "不存在房间对象中");
			}
		}
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
			room.setOpen(false);
		}
		room.setGameStatus(SSSConstant.SSS_GAME_STATUS_FINAL_SUMMARY);

		// 更新数据库
		updateUserScore(room.getRoomNo());

		changeGameStatus(room);
		// 房间处理
		removeRoom(roomNo);

	}

	public void checkMyCard(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!postData.containsKey(CommonConstant.DATA_KEY_ROOM_NO)
				|| !postData.containsKey(CommonConstant.DATA_KEY_ACCOUNT) || !postData.containsKey("uuid")) {
			return;
		}
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room) {
			return;
		}
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		JSONObject userInfo = userBiz.getUserByAccount(account);
		// 检验是不是本人操作
		if (!userInfo.containsKey("uuid") || Dto.stringIsNULL(userInfo.getString("uuid"))
				|| !userInfo.getString("uuid").equals(postData.getString("uuid"))) {
			return;
		}
		// 游戏不在配牌阶段
		if (SSSConstant.SSS_GAME_STATUS_GAME_EVENT != room.getGameStatus()) {
			return;
		}
		JSONObject result = new JSONObject();
		result.put("headPai", room.getUserPacketMap().get(account).getHeadPai());
		result.put("midPai", room.getUserPacketMap().get(account).getMidPai());
		result.put("footPai", room.getUserPacketMap().get(account).getFootPai());
		CommonConstant.sendMsgEventToSingle(client, result.toString(), "checkMypaiPush_SSS");
	}

	public void handReview(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		String eventName = "handleReviewEventPush";
		if (!postData.containsKey(CommonConstant.DATA_KEY_ROOM_NO)
				|| !postData.containsKey(CommonConstant.DATA_KEY_ACCOUNT)
				|| !postData.containsKey(CommonConstant.USERS_ID)) {
			return;
		}
		JSONObject result = new JSONObject();

		// 账号
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		// 房间号
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		// 房间信息
		SSSGameRoomNew gameRoom = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		// 是否在此房间玩过游戏
		boolean isGame = false;
		// 游戏房间不存在
		if (gameRoom == null) {
			return;
		}

		// 获取房间里的玩家(未包括观战)
		Playerinfo playerinfo = gameRoom.getPlayerMap().get(account);
		// 判断是否是游戏中的玩家
		if (playerinfo == null) {
			CommonConstant.sendMsgEventNo(client, "权限不足,请参加游戏后重试", null, eventName);
			return;
		}

		// 获取游戏记录
		List<JSONArray> arrays = gameRoom.getGameRecords();

		// 判断是否有游戏记录
		if (arrays.size() == 0) {
			CommonConstant.sendMsgEventNo(client, "暂无数据", null, eventName);
			return;
		}

		// 游戏时间
		String gameTime = DateUtils.getTodayTime();
		// 获取局数
		int gameCount = postData.containsKey("gameCount") ? postData.getInt("gameCount") : arrays.size();

		SSSUserPacket up = new SSSUserPacket();

		// 判断局数是否符合规范
		if (gameCount > arrays.size() || gameCount <= 0) {
			CommonConstant.sendMsgEventNo(client, "请刷新后再试", null, eventName);
			return;
		}

		// 获取该小局的数据
		JSONArray array = arrays.get(gameCount - 1);

		for (int i = 0; i < array.size(); i++) {
			JSONObject temp = array.getJSONObject(i);
			// 判断该玩家是否在该房间玩过游戏
			if (temp.containsKey("account") && account.equals(temp.getString("account"))) {
				isGame = true;
			}
			// 检测数据是否符合格式
			if (!temp.containsKey("pai") || !Dto.isJsonArray(temp.getString("pai"))) {
				CommonConstant.sendMsgEventNo(client, "请刷新后再试", null, eventName);
				return;
			}
			JSONArray pai = temp.getJSONArray("pai");
			// 添加玩家头道,中道,尾道的牌类型集合
			temp.put("paiTypeList", SSSComputeCards.judge(up.togetMyPai(pai), gameRoom));
			// 获取结算时间
			if (temp.containsKey("createTime")) {
				gameTime = temp.getString("createTime");
			}
		}

		// 该玩家是否在该房间玩过游戏
		if (!isGame) {
			CommonConstant.sendMsgEventNo(client, "未在该房间玩过游戏", null, eventName);
			return;
		}

		result.element("list", sordCardByScore(array));
		result.element("gameTime", gameTime);
		result.element("gameIndex", arrays.indexOf(array) + 1);
		log.info(result.toString());
		CommonConstant.sendMsgEventToSingle(client, result.toString(), eventName);
	}

	private JSONArray sordCardByScore(JSONArray array) {
		if (array == null || array.size() == 0) {
			return array;
		}
		for (int i = 0; i < array.size(); i++) {
			for (int j = 0; j < array.size() - 1 - i; j++) {
				if (array.getJSONObject(j).getInt("sum") < array.getJSONObject(j + 1).getInt("sum")) {
					JSONObject temp = array.getJSONObject(j);
					array.set(j, array.getJSONObject(j + 1));
					array.set(j + 1, temp);
				}
			}
		}
		return array;
	}

	public void relist(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!postData.containsKey(CommonConstant.DATA_KEY_ROOM_NO)
				|| !postData.containsKey(CommonConstant.DATA_KEY_ACCOUNT)
				|| !postData.containsKey(CommonConstant.USERS_ID)) {
			return;
		}

		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (room == null) {
			return;
		}
		SSSUserPacket sssUserPacket = room.getUserPacketMap().get(account);
		if (sssUserPacket == null) {
			return;
		}
		Playerinfo playerinfo = room.getPlayerMap().get(account);
		if (playerinfo == null) {
			return;
		}
		int myIndex = playerinfo.getMyIndex();
		// 重摆时改变玩家状态
		sssUserPacket.setStatus(SSSConstant.SSS_GAME_STATUS_READY);
		JSONObject result = new JSONObject();
		result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		result.element("myIndex", myIndex);
		CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "sss_relistEvent_push");
	}

	public void cutCard(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!postData.containsKey(CommonConstant.DATA_KEY_ROOM_NO)
				|| !postData.containsKey(CommonConstant.DATA_KEY_ACCOUNT) || !postData.containsKey("type")) {
			return;
		}
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		String type = postData.getString("type");

		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (room == null) {
			return;
		}
		// 切牌玩家账号
		String cutCardAccount = room.getCutCardAccount();
		// 判断切牌玩家是否是最大输家
		if (!cutCardAccount.equals(account)) {
			return;
		}

		JSONObject result = new JSONObject();
		result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
		// 准备切牌 重置倒计时
		if ("1".equals(type)) {
			if (SSSConstant.SSS_GAME_STATUS_READY != room.getGameStatus()
					&& SSSConstant.SSS_GAME_STATUS_SUMMARY != room.getGameStatus()) {
				return;
			}
			int cutTime = redisInfoService.getAppTimeSetting(room.getPlatform(), room.getGid(),
					"SSSConstant.SSS_CUT_CARD_TIMER");
			if (cutTime <= 0) {
				cutTime = SSSConstant.SSS_CUT_CARD_TIME;
			}
			result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			result.element("cutTime", cutTime);
			result.element("cutCardAccount", cutCardAccount);
			result.element("type", type);
			// 重置小局倒计时
			room.setResetTime(true);
			// 开始切牌
		} else if ("2".equals(type) && postData.containsKey("cutRatio")) {
			result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			result.element("cutRatio", postData.getString("cutRatio"));
			result.element("cutCardAccount", cutCardAccount);
			result.element("type", type);
		}
		CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "sss_cutCardEvent_push");
	}

	public void interactive(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		System.out.println(postData);
		String account = postData.getString("account");
		System.out.println("account:" + account);
		// 检测钻石够不够 够的话-10 不够返回失败
		JSONObject user = userBiz.getUserByAccount(account);
		String zuanshi = user.getString("roomcard");
		try {
			postData.put("account", room.getPlayerMap().get(postData.get("account")).getMyIndex());
			postData.put("target_account", room.getPlayerMap().get(postData.get("target_account")).getMyIndex());
			if (Long.parseLong(zuanshi) > 10) {
				// 修改钻石
				int result = zaNewSignBiz.updateUserRoomcardBySign(-10, user.getInt("id"));
				if (result > 0) {
					CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), postData.toString(),
							"interactiveEventPush");
				} else {
					// 修改钻石失败
					postData.put("code", 0);
					postData.put("errorMsg", "扣除钻石失败！错误码：400");
					CommonConstant.sendMsgEventToSingle(client, postData.toString(), "interactiveEventPush");
				}
			} else {
				// 钻石不足
				postData.put("code", 0);
				postData.put("errorMsg", "钻石不足");
				CommonConstant.sendMsgEventToSingle(client, postData.toString(), "interactiveEventPush");
			}

		} catch (Exception e) {
			// TODO: handle exception
		}
	}
}
