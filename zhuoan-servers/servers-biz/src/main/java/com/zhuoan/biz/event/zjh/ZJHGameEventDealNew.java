
package com.zhuoan.biz.event.zjh;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.zjh.ZhaJinHuaCore;
import com.zhuoan.biz.event.BaseEventDeal;
import com.zhuoan.biz.game.biz.GameCircleService;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.nn.NNGameRoomNew;
import com.zhuoan.biz.model.sss.SSSGameRoomNew;
import com.zhuoan.biz.model.sss.SSSUserPacket;
import com.zhuoan.biz.model.zjh.UserPacket;
import com.zhuoan.biz.model.zjh.ZJHGameRoomNew;
import com.zhuoan.biz.robot.RobotEventDeal;
import com.zhuoan.constant.CircleConstant;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.DaoTypeConstant;
import com.zhuoan.constant.NNConstant;
import com.zhuoan.constant.SSSConstant;
import com.zhuoan.constant.TeaConstant;
import com.zhuoan.constant.ZJHConstant;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Resource;
import javax.jms.Destination;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Component;

@Component
public class ZJHGameEventDealNew {
	private static final Logger logger = LoggerFactory.getLogger(ZJHGameEventDealNew.class);
	@Resource
	private GameTimerZJH gameTimerZJH;
	@Resource
	private RoomBiz roomBiz;
	@Resource
	private Destination daoQueueDestination;
	@Resource
	private ProducerService producerService;
	@Resource
	private RedisInfoService redisInfoService;
	@Resource
	private UserBiz userBiz;
	@Resource
	private GameCircleService gameCircleService;
	@Resource
	private BaseEventDeal baseEventDeal;
	@Resource
	private TeaService teaService;

	public ZJHGameEventDealNew() {
	}

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
			CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_ZJH");
		}
	}

	/**
	 * 加入房间
	 * 
	 * @param client
	 * @param data
	 */
	public void joinRoom(SocketIOClient client, Object data) {
		JSONObject joinData = JSONObject.fromObject(data);
		final String account = joinData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		final String roomNo = joinData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
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
		// 通知别人
		if (CommonConstant.GLOBAL_NO == isReconnect) {
			UserPacket up = room.getUserPacketMap().get(account);
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
			// 通知玩家
			CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), obj.toString(), "playerEnterPush_ZJH");
		}
	}

	public void gameReady(SocketIOClient client, Object data) {
		System.out.println("玩家准备数据："+data);
		JSONObject postData = JSONObject.fromObject(data);
		// 不满足准备条件直接忽略/
		if (!CommonConstant.checkEvent(postData, ZJHConstant.ZJH_GAME_STATUS_INIT, client)
				&& !CommonConstant.checkEvent(postData, ZJHConstant.ZJH_GAME_STATUS_READY, client)
				&& !CommonConstant.checkEvent(postData, ZJHConstant.ZJH_GAME_STATUS_SUMMARY, client)) {
			return;
		}
		// 房间号
		final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		int roomType = room.getRoomType();
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		logger.warn("玩家" + account + "准备");
		UserPacket up = room.getUserPacketMap().get(account);
		Playerinfo playerinfo = room.getPlayerMap().get(account);
		if (null == up || null == playerinfo || CommonConstant.ROOM_VISIT_INDEX == playerinfo.getMyIndex())
			return;
		if (ZJHConstant.ZJH_USER_STATUS_READY == up.getStatus()) {
			JSONObject result = new JSONObject();
			result.put("type", CommonConstant.SHOW_MSG_TYPE_SMALL);
			result.put(CommonConstant.RESULT_KEY_MSG, "您已准备");
			CommonConstant.sendMsgEventToSingle(client, result.toString(), "tipMsgPush");
			return;
		}

		if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB || room.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
			// 元宝场 金币场
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
					result.put(CommonConstant.RESULT_KEY_MSG, "钻石不足");
				}
				CommonConstant.sendMsgEventToSingle(client, result.toString(), "tipMsgPush");
				return;
			}
		} else if (CommonConstant.ROOM_TYPE_INNING == room.getRoomType()) {
			// 亲友圈
			if (playerinfo.isCollapse()) {
				JSONObject result = new JSONObject();
				result.put("type", CommonEventConstant.TIP_MSG_PUSH_TYPE_CPM);
				result.put(CommonConstant.RESULT_KEY_MSG, "您已破产");
				CommonConstant.sendMsgEventToSingle(client, result.toString(), CommonEventConstant.TIP_MSG_PUSH);
				return;
			}
		} else if (room.getRoomType() == CommonConstant.ROOM_TYPE_FREE
				&& CommonConstant.ROOM_PAY_TYPE_AA.equals(String.valueOf(room.getPayType()))) {
			// 自由场 房费AA
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
		up.setStatus(ZJHConstant.ZJH_USER_STATUS_READY);
		// 设置房间准备状态
		if (room.getGameStatus() != ZJHConstant.ZJH_GAME_STATUS_READY) {
			room.setGameStatus(ZJHConstant.ZJH_GAME_STATUS_READY);
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
							room.getUserPacketMap().get(s).setStatus(ZJHConstant.ZJH_GAME_STATUS_INIT);
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
		// 统一倒计时时间
		if (room.getNowReadyCount() == minStartPlayer) {
			if (CommonConstant.ROOM_TYPE_YB == roomType || CommonConstant.ROOM_TYPE_FREE == roomType
					|| CommonConstant.ROOM_TYPE_INNING == roomType && room.getGameIndex() == 0) {
				room.setTimeLeft(ZJHConstant.ZJH_TIMER_READY);
				room.setReadyOvertime(CommonConstant.READY_OVERTIME_OUT);
				ThreadPoolHelper.executorService.submit(new Runnable() {
					@Override
					public void run() {
						gameTimerZJH.gameOverTime(roomNo, ZJHConstant.ZJH_GAME_STATUS_READY, account,
								ZJHConstant.ZJH_TIMER_READY);
					}
				});
			}

			result.put("showTimer", CommonConstant.GLOBAL_YES);
			result.put("timer", room.getTimeLeft());
			result.put("userStatus", up.getStatus());
		}
		CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "playerReadyPush_ZJH");
		// 统一倒计时时间 end
	}

	// 开始游戏
	public void startGame(String roomNo) {
		/*
		 * 0.如果房间不是ready状态则不能开始游戏 1.局数超过强制结算 2.初始化房间信息 4.更新游戏局数 5.根据房间设置开始游戏
		 */
		ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room) {
			return;
		}
		// 非准备或初始阶段无法开始开始游戏
		if (room.getGameStatus() != ZJHConstant.ZJH_GAME_STATUS_READY) {
			return;
		}
		if (room.getGameIndex() > room.getGameCount()) {
			// compulsorySettlement(room.getRoomNo());// 局数超过 强制结算
			return;
		}
		redisInfoService.insertSummary(room.getRoomNo(), "_ZJH");
		// 初始化房间信息
		room.initGame();
		// 更新游戏局数
		JSONObject roomInfo = new JSONObject();
		roomInfo.put("roomId", room.getId());
		producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INDEX, roomInfo));
		startGameCommon(roomNo);
	}

	// 游戏内容阶段
	public void startGameCommon(String roomNo) {
		final ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		shuffleAndFp(room);
		// 设置游戏状态 配牌
		room.setGameStatus(ZJHConstant.ZJH_GAME_STATUS_GAME);
		for (String acc : room.getUserPacketMap().keySet()) {
			room.getUserPacketMap().get(acc).isDo = false;
		}
		// 设置玩家的手牌
		JSONArray gameProcessFP = new JSONArray();
		String a = "";
		int difen = room.getScore() * room.getUserPacketMap().keySet().size();
		room.setTotalScore(difen);
		for (String uuid : room.getUserPacketMap().keySet()) {
			room.getUserPacketMap().get(uuid).setScore(1);
			// 设置要下注的人 第一局由游戏玩家下标决定
			UserPacket up = room.getUserPacketMap().get(uuid);
			Playerinfo playerinfo = room.getPlayerMap().get(uuid);
			if (playerinfo.getMyIndex() == 0) {
				a = room.getGameIndex() == 1 ? playerinfo.getAccount() : room.getBanker();
			}
			if (null == up || null == playerinfo)
				continue;
			// 存放游戏记录
			JSONObject userPai = new JSONObject();
			userPai.put("account", uuid);
			userPai.put("name", playerinfo.getName());
			userPai.put("pai", up.getPai());
			gameProcessFP.add(userPai);

		}
		final String xzAccount = a;
		room.setFocus(xzAccount);
		room.getGameProcess().put("fapai", gameProcessFP);
		// 设置下注时间
		final int gameEventTime = room.getXzTimer();
		changeGameStatus(room);
		room.setTimeLeft(gameEventTime);
		if (gameEventTime > 0) {
			// 下注倒计时
			ThreadPoolHelper.executorService.submit(new Runnable() {
				@Override
				public void run() {
					gameTimerZJH.gameOverTime(room.getRoomNo(), ZJHConstant.ZJH_GAME_STATUS_GAME, xzAccount,
							room.getXzTimer());
				}
			});
		}

	}

	// 游戏开始（点击了游戏开始按钮）
	public void gameStart(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!CommonConstant.checkEvent(postData, ZJHConstant.ZJH_GAME_STATUS_INIT, client)
				&& !CommonConstant.checkEvent(postData, ZJHConstant.ZJH_GAME_STATUS_READY, client)) {
			return;
		}
		// 房间号
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		// 玩家账号
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		int type = postData.getInt("type");
		String eventName = "gameStartPush_ZJH";
		// 只有房卡场和俱乐部和亲友圈有提前开始的功能
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

		int readyCount = room.getUserPacketMap().get(account).getStatus() == ZJHConstant.ZJH_USER_STATUS_READY
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
					&& room.getUserPacketMap().get(player).getStatus() != ZJHConstant.ZJH_USER_STATUS_READY) {
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
		if (type == ZJHConstant.START_GAME_TYPE_UNSURE && outList.size() > 0) {
			sendStartResultToSingle(client, eventName, CommonConstant.GLOBAL_YES, "是否开始");
			return;
		}
		if (!Dto.isObjNull(room.getSetting())) {
			room.getSetting().remove("mustFull");
		}
		// 房主未准备直接准备
		if (room.getUserPacketMap().get(account).getStatus() != ZJHConstant.ZJH_USER_STATUS_READY) {
			gameReady(client, data);
		}
		if (outList.size() > 0) {
			// 退出房间
			for (String player : outList) {
				if (room.getUserPacketMap().get(player).getStatus() != ZJHConstant.ZJH_USER_STATUS_READY) {
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

	public void shuffleAndFp(ZJHGameRoomNew room) {
		room.xiPai();
		room.faPai();
	}

	public void gameEvent(SocketIOClient client, Object data) {
		/*
		 * 游戏内容阶段 判断玩家做了什么操作 最后判断是否全部完成亮牌 是否要更改游戏状态
		 */
		JSONObject postData = JSONObject.fromObject(data);
		String roomNo = postData.getString("roomNo");
		ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		String account = postData.getString("uid");
		int index = 0;
		if (postData.containsKey("index")) {
			index = postData.getInt("index");
		}

		// 标注玩家已操作
		int type = postData.getInt("type");
		if (type != 4) {
			room.getUserPacketMap().get(account).isDo = true;
		}

		Double score = 0.0;
		if (postData.containsKey("score")) {
			score = postData.getDouble("score");
		}
		int value = 0;
		if (postData.containsKey("value")) {
			value = postData.getInt("value");
		}

		if (type == 1) {
			// 跟到底
			genDaoDi(room, account, value, client);
		} else if (type == 2) {
			// 弃牌
			giveUp(room, account);
		} else if (type == 3) {
			compare(room, account, index);
			// 找人比牌
		} else if (type == 4) {
			// 看牌
			look(room, account);
		} else if (type == 6 || type == 5) {
			// 下注
			xiaZhu(room, account, score, type, null);
		}

	}

	public void genDaoDi(ZJHGameRoomNew room, String account, int value, SocketIOClient client) {
		JSONObject result = new JSONObject();
		if (value == 1) {
			((UserPacket) room.getUserPacketMap().get(account)).isGenDaoDi = true;
		} else {
			((UserPacket) room.getUserPacketMap().get(account)).isGenDaoDi = false;
		}

		result.put("code", 1);
		result.put("value", value);
		result.put("index", room.getPlayerIndex(account));
		result.put("type", 1);
		CommonConstant.sendMsgEventToSingle(client, result.toString(), "gameActionPush_ZJH");
		room.getProcessList().add(result);
		if (room.getFocus().equals(account) && ((UserPacket) room.getUserPacketMap().get(account)).isGenDaoDi) {
			this.xiaZhu(room, account, room.getCurrentScore(), 5, null);
		}

	}

	public void look(ZJHGameRoomNew room, String account) {
		// 玩家状态改为3
		room.getUserPacketMap().get(account).setStatus(ZJHConstant.ZJH_USER_STATUS_KP);
		// 给此玩家发送自己的牌
		for (String u : room.getUserPacketMap().keySet()) {
			JSONObject result = new JSONObject();
			result.put("index", room.getPlayerIndex(account));
			if (u.equals(account)) {
				result.put("pai", room.getUserPacketMap().get(u).getPai());
				result.put("type", room.getUserPacketMap().get(u).getType());
			}
			CommonConstant.sendMsgEventToSingle(((Playerinfo) room.getPlayerMap().get(u)).getUuid(), result.toString(),
					"gameShowPaiPush_ZJH");
		}
	}

	public boolean xiaZhu(final ZJHGameRoomNew room, String account, double score, int type, JSONObject json) {

		JSONObject result = new JSONObject();
		// 如果类型为6 房间的当前积分为下注积分？
		if (type == 6) {
			room.setCurrentScore(score);
		}
		// 如果玩家状态为已看牌 积分*2
		if (((UserPacket) room.getUserPacketMap().get(account)).getStatus() == 3) {
			score *= 2.0D;
		}

		// 如果类型为比牌 且房间设置里包含 double_compare 字段为1 则积分也*2
		if (type == 3 && !Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("double_compare")
				&& room.getSetting().getInt("double_compare") == 1) {
			score *= 2.0D;
		}

		// 元宝场？的元宝判断 目前用不到
		if ((room.getRoomType() == 3 || room.getRoomType() == 1 || room.getRoomType() == 8)
				&& ((Playerinfo) room.getPlayerMap().get(account)).getScore() < score) {
			result.put("code", 0);
			result.put("msg", "元宝不足");
			CommonConstant.sendMsgEventToSingle(((Playerinfo) room.getPlayerMap().get(account)).getUuid(),
					result.toString(), "gameActionPush_ZJH");
			return false;
		} else {
			// 下注人的下标
			int myIndex = room.getPlayerIndex(account);
			// 获取下一个操作的玩家
			final String nextPlayer = room.getNextOperationPlayer(account);
			// 给下注list添加玩家下注数据 (下标，积分 json格式)
			room.addXiazhuList(myIndex, score);
			// 更改玩家下注积分信息
			room.addScoreChange(account, score);
			// 添加已下注玩家信息 一轮一轮计算 如果集合中包含了下一位要下注的玩家 则证明下注完了一轮
			// 清空集合 并且房间下注轮数+1
			room.addXzPlayer(myIndex, room.getPlayerIndex(nextPlayer));
			// 默认游戏下注未结束
			int isGameOver = 0;
			// 判断是否有其他人要操作 如果仅剩最后两个人 则直接进行房间结算
			int i = 0;
			for (String u : room.getUserPacketMap().keySet()) {
				if (room.getUserPacketMap().get(u).getStatus() != 4 & room.getUserPacketMap().get(u).getStatus() != 7) {
					i++;
				}
			}
			if (i == 1) {
				this.compelCompare(room);
				isGameOver = 1;
			}

			// 如果房间下注轮数已达到最大轮数 或者已下注总注数大于设置的最大注数
			if (room.getGameNum() > room.getTotalGameNum() || room.getTotalScore() >= room.getMaxScore()) {
				// 全部玩家开始比牌
				this.compelCompare(room);
				// 设置下注状态为结束
				isGameOver = 1;
			}
			// 如果类型不为3时
			if (type != 3 && isGameOver == 0) {
				// 从房间设置中读出eventTime为30
				/*
				 * final int eventTime = !Dto.isObjNull(room.getSetting()) &&
				 * room.getSetting().containsKey("eventTime") ?
				 * room.getSetting().getInt("eventTime") : 60;
				 */
				int eventTime = room.getXzTimer();
				// 开始给下一位玩家进行倒计时操作 如果玩家未操作则自动操作
				ThreadPoolHelper.executorService.submit(new Runnable() {
					public void run() {
						ZJHGameEventDealNew.this.gameTimerZJH.gameOverTime(room.getRoomNo(),
								ZJHConstant.ZJH_GAME_STATUS_GAME, nextPlayer, eventTime);
					}
				});
			}
			// 如果类型为3 且 下注未结束
			if (type == 3 && isGameOver != 1) {
				if (type == 3) {
					// 添加返回给客户端的信息
					result.put("code", 1);
					// 房间游戏状态
					result.put("gameStatus", room.getGameStatus());
					// 下注玩家的下标
					result.put("index", ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
					// 下一位要下注玩家的下标
					result.put("nextNum", room.getPlayerIndex(nextPlayer));
					// 当前下注轮数
					result.put("gameNum", room.getGameNum());
					// 当前下注的注数
					result.put("currentScore", room.getCurrentScore());
					// 房间下注的总注数
					result.put("totalScore", room.getTotalScore());
					// 总轮数
					result.put("totalGameNum", room.getTotalGameNum());
					// 房间最大下注筹码数
					result.put("maxScore", room.getMaxScore());
					// 我一共下了多少注
					result.put("myScore", ((UserPacket) room.getUserPacketMap().get(account)).getScore());
					// 这轮我下的注数
					result.put("score", score);
					// 玩家的总分数
					result.put("realScore", ((Playerinfo) room.getPlayerMap().get(account)).getScore());
					// 类型 （未知）
					result.put("type", type);
					// 是否结束了下注环节
					result.put("isGameover", Integer.valueOf(isGameOver));
					// 把此信息添加到房间游戏过程
					room.getProcessList().add(result);
				}
			} else {

				// 获取房间所有玩家的迭代器
				Iterator var10 = room.getUserPacketMap().keySet().iterator();
				while (var10.hasNext()) {
					// 获取玩家的account
					String uuid = (String) var10.next();
					if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
						// code
						result.put("code", 1);
						result.put("gameStatus", room.getGameStatus());
						result.put("index", ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
						result.put("nextNum", room.getPlayerIndex(nextPlayer));
						result.put("gameNum", room.getGameNum());
						result.put("currentScore", room.getCurrentScore());
						result.put("totalScore", room.getTotalScore());
						// 总轮数
						result.put("totalGameNum", room.getTotalGameNum());
						// 房间最大下注筹码数
						result.put("maxScore", room.getMaxScore());
						result.put("myScore", ((UserPacket) room.getUserPacketMap().get(account)).getScore());
						result.put("score", score);
						result.put("realScore", ((Playerinfo) room.getPlayerMap().get(account)).getScore());
						result.put("type", type);
						result.put("isGameover", Integer.valueOf(isGameOver));
						result.put("xzTimer", room.getXzTimer());
						result.put("xzTimeBegin", System.currentTimeMillis());
						result.put("summeryTimer", 10);
						if (json != null) {
							result.put("winIndex", json.get("winIndex"));
							result.put("otherIndex", json.get("otherIndex"));
						}
						if (room.getUserPacketMap().get(account).getStatus() == 3) {
							// 玩家已经看牌 告诉所有客户端他需要扔两份筹码出去
							result.put("fanbei", 1);
						}
						if (isGameOver == 1) {
							// 把每个玩家的牌型信息存进去 最后比牌的时候发送给客户端
							for (String u : room.getPlayerMap().keySet()) {
								((UserPacket) room.getUserPacketMap().get(uuid)).addBiPaiList(
										room.getPlayerMap().get(u).getMyIndex(),
										room.getUserPacketMap().get(u).getPai());
							}
							// 游戏结算数据
							result.put("jiesuan", this.obtainSummaryData(room));
							// 所有玩家的牌型
							result.put("showPai", ((UserPacket) room.getUserPacketMap().get(uuid)).getBipaiList());
							result.put("type", 7);
							// 把数据添加到房间的结算数据中
							room.setSummaryData(result);
						}

						if (account.equals(uuid)) {
							// 添加游戏过程 保证只添加了一次 并且改变游戏状态
							room.getProcessList().add(result);

						}
						// 给每个玩家发送信息
						CommonConstant.sendMsgEventToSingle(((Playerinfo) room.getPlayerMap().get(uuid)).getUuid(),
								result.toString(), "gameActionPush_ZJH");
					}
				}
				// 如果游戏下注环节结束 并且房间的类型不为1
				if (isGameOver == 1 && room.getRoomType() != 1) {
					// 保存游戏日志（DAO层操作）
					if (room.getGameIndex() != room.getGameCount()) {
						room.setTimeLeft(10);
						room.setGameStatus(ZJHConstant.ZJH_GAME_STATUS_SUMMARY);
						if (room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
							ThreadPoolHelper.executorService.submit(new Runnable() {
								@Override
								public void run() {
									gameTimerZJH.readyOverTime(room.getRoomNo(), ZJHConstant.ZJH_GAME_STATUS_SUMMARY,
											10);
								}
							});

						}
					}

					this.saveGameLogs(room.getRoomNo());
				}
				// 判断是否为最后一局 是的话发送总结算数据给每个人
				this.sendFinalSummaryToPlayer(isGameOver, room);

			}

			return type != 3 || isGameOver != 1;
		}
	}

	private void finalSummaryRoom(String roomNo) {
		ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room) {
			return;
		}
		insertStatis(roomNo);
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
		ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		double maxScore = 0D;// 大赢家
		for (String account : room.getUserPacketMap().keySet()) {
			double sub = room.getUserPacketMap().get(account).getAllScore();
			if (sub > maxScore) {
				maxScore = sub;
			}
		}
		if (maxScore <= 0D && room.getGameIndex() <= 1) {
			return;
		}
		String time = DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss");
		JSONArray arr = new JSONArray();
		Iterator var7 = room.getUserPacketMap().keySet().iterator();
		while (var7.hasNext()) {
			String account = (String) var7.next();
			int isBigWinner = 0;
			if (maxScore == room.getUserPacketMap().get(account).getAllScore() && maxScore > 0D) {
				isBigWinner = 1;
			}
			JSONObject jsonObject = new JSONObject();
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			jsonObject.element("game_sum", room.getGameCount()).element("account", account)
					.element("name", playerinfo.getName()).element("headimg", playerinfo.getHeadimg())
					.element("cut_type", room.getCutType()).element("cut_score", playerinfo.getCutScore())
					.element("za_games_id", room.getGid()).element("room_type", room.getRoomType())
					.element("user_id", playerinfo.getId()).element("room_no", room.getRoomNo())
					.element("room_id", room.getId()).element("circle_code", room.getClubCode())
					.element("create_time", time).element("is_big_winner", isBigWinner);
			if (CircleConstant.CUTTYPE_WU.equals(room.getCutType())) {
				jsonObject.element("score", room.getUserPacketMap().get(account).getAllScore());
			} else {
				jsonObject.element("score", Dto.add(room.getUserPacketMap().get(account).getAllScore(),
						room.getPlayerMap().get(account).getCutScore()));
			}
			arr.add(jsonObject);
		}
		JSONObject object = new JSONObject();
		object.element("array", arr);
		this.producerService.sendMessage(this.daoQueueDestination,
				new PumpDao(DaoTypeConstant.INSERT_ZA_USER_GAME_STATIS, object));

	}

	public void compare(final ZJHGameRoomNew room, String account, int index) {
		/*
		 * 单独比牌 需要知道操作者 以及被操作者 然后拿两个人的牌开始进行相比 返回所有客户端 通知谁输谁赢 判断除了被比牌的两个人外是否还有其他人要操作
		 * 如果仅剩最后两个人 则直接进行房间结算
		 */

		JSONObject result = new JSONObject();
		// 获取当前房间下注的筹码数
		double score = room.getCurrentScore();
		int winIndex = 0;
		UserPacket up1 = room.getUserPacketMap().get(account);
		String account2 = "";
		for (String acc : room.getPlayerMap().keySet()) {
			if (room.getPlayerMap().get(acc).getMyIndex() == index) {
				account2 = acc;
			}
		}
		UserPacket up2 = room.getUserPacketMap().get(account2);
		if (up2.getStatus() == 4 || up2.getStatus() == 7) {
			xiaZhu(room, account, score, 5, null);
		}
		int type1 = up1.getType();
		int type2 = up2.getType();
		if (up2 != null && type1 > type2) {
			// 玩家比牌胜利 被比牌玩家状态设置为结算状态
			room.getUserPacketMap().get(account2).setStatus(7);
			winIndex = room.getPlayerMap().get(account).getMyIndex();
		} else if (type1 == type2) {
			// 类型一样 比较大小
			if (ZhaJinHuaCore.compareDaXiao(Arrays.asList(up1.getPai()), Arrays.asList(up2.getPai())) == 1) {
				// 同款牌型 但是牌面大小胜利
				room.getUserPacketMap().get(account2).setStatus(7);
				winIndex = room.getPlayerMap().get(account).getMyIndex();
			} else {
				room.getUserPacketMap().get(account).setStatus(7);
				winIndex = room.getPlayerMap().get(account2).getMyIndex();
			}
		} else {
			// 玩家比牌失败
			room.getUserPacketMap().get(account).setStatus(7);
			winIndex = room.getPlayerMap().get(account2).getMyIndex();
		}
		result.put("winIndex", winIndex);
		result.put("otherIndex", room.getPlayerMap().get(account2).getMyIndex());
		xiaZhu(room, account, score, 5, result);
	}

	public void sendFinalSummaryToPlayer(int isGameOver, ZJHGameRoomNew room) {
		if ((room.getRoomType() == 0 || room.getRoomType() == 6 || room.getRoomType() == 9) && isGameOver == 1
				&& room.getGameIndex() == room.getGameCount()) {
			if (room.getRoomType() == 6) {
				room.setOpen(false);
				room.setNeedFinalSummary(true);
			}
			room.setGameStatus(4);
			room.setIsClose(-2);
			JSONObject result = new JSONObject();
			result.put("code", 1);
			result.put("type", 8);
			result.put("isClose", 0);
			result.put("jiesuanData", room.obtainFinalSummaryData());

			CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "gameActionPush_ZJH");
			finalSummaryRoom(room.getRoomNo());
		}

	}

	// 获得结算数据
	public JSONArray obtainSummaryData(ZJHGameRoomNew room) {
		JSONArray summaryData = new JSONArray();
		// 房间内的所有玩家
		Iterator var3 = room.getUserPacketMap().keySet().iterator();

		while (var3.hasNext()) {
			// 每一个玩家
			String account = (String) var3.next();
			if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null
					&& ((UserPacket) room.getUserPacketMap().get(account)).getStatus() > 0) {
				JSONObject playerData = new JSONObject();

				playerData.put("index", room.getPlayerIndex(account)); // 下标
				playerData.put("headimg", ((Playerinfo) room.getPlayerMap().get(account)).getHeadimg()); // 头像
				playerData.put("account", account); // id
				playerData.put("name", ((Playerinfo) room.getPlayerMap().get(account)).getName()); // 名字
				if (((UserPacket) room.getUserPacketMap().get(account)).getStatus() == 6) {
					playerData.put("win", 1); // 状态为6 赢家
					// 积分为房间已下注的总数减去玩家下注的总数
					playerData.put("score", Dto.sub(room.getTotalScore(),
							((UserPacket) room.getUserPacketMap().get(account)).getScore()));
				} else {
					playerData.put("win", 0);// 状态不为6 输家
					playerData.put("score", -((UserPacket) room.getUserPacketMap().get(account)).getScore());
				}

				// 玩家的总积分
				playerData.put("totalScore", ((Playerinfo) room.getPlayerMap().get(account)).getScore()); // 总分数
				summaryData.add(playerData);
			}
		}

		return summaryData;
	}

	// 弃牌
	public void giveUp(final ZJHGameRoomNew room, String account) {
		JSONObject result = new JSONObject();
		int isGameOver = 0;
		// 更改玩家状态为弃牌状态
		room.getUserPacketMap().get(account).setStatus(4);
		// 判断是否只剩下一个没有弃牌的玩家 如果是 则该玩家为本局的赢家
		String acc = "";
		int i = 0;
		for (String up : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().get(up).getStatus() == 4 || room.getUserPacketMap().get(up).getStatus() == 7) {
				continue;
			}

			acc = up;
			i++;
		}
		if (i == 1) {
			// 只剩下一个玩家不是结算状态或者弃牌状态 默认赢家
			room.getUserPacketMap().get(acc).setStatus(6);
			compelCompare(room);
			isGameOver = 1;
		}
		// 获取自己的下标
		int myIndex = room.getPlayerIndex(account);
		// 下一位操作的玩家
		final String nextPlayer = room.getNextOperationPlayer(account);
		// 添加下注玩家 判断是否到下一轮了
		room.addXzPlayer(myIndex, room.getPlayerIndex(nextPlayer));

		if (room.getGameNum() == room.getTotalGameNum() || room.getTotalScore() >= room.getMaxScore()) {
			// 全部玩家开始比牌
			this.compelCompare(room);
			// 设置下注状态为结束
			isGameOver = 1;
		}
		// 如果类型不为3时
		if (isGameOver == 0) {
			// 游戏未结束 开始给下一位玩家进行倒计时
			ThreadPoolHelper.executorService.submit(new Runnable() {
				public void run() {
					ZJHGameEventDealNew.this.gameTimerZJH.gameOverTime(room.getRoomNo(),
							ZJHConstant.ZJH_GAME_STATUS_GAME, nextPlayer, room.getXzTimer());
				}
			});
		}
		if (isGameOver != 1) {
			Iterator var10 = room.getUserPacketMap().keySet().iterator();
			while (var10.hasNext()) {
				String uuid = (String) var10.next();
				// 添加返回给客户端的信息
				result.put("code", 1);
				// 房间游戏状态
				result.put("gameStatus", room.getGameStatus());
				// 弃牌玩家的下标
				result.put("index", ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
				// 下一位要下注玩家的下标
				result.put("nextNum", room.getPlayerIndex(nextPlayer));
				// 当前下注轮数
				result.put("gameNum", room.getGameNum());
				// 当前下注的注数
				result.put("currentScore", room.getCurrentScore());
				// 房间下注的总注数
				result.put("totalScore", room.getTotalScore());
				// 总轮数
				result.put("totalGameNum", room.getTotalGameNum());
				// 房间最大下注筹码数
				result.put("maxScore", room.getMaxScore());
				// 我一共下了多少注
				result.put("myScore", ((UserPacket) room.getUserPacketMap().get(account)).getScore());
				// 这轮我下的注数
				result.put("score", 0);
				// 玩家的总分数
				result.put("realScore", ((Playerinfo) room.getPlayerMap().get(account)).getScore());
				// 类型 （未知）
				result.put("type", 2);
				// 是否结束了下注环节
				result.put("isGameover", Integer.valueOf(isGameOver));
				result.put("xzTimer", room.getXzTimer());
				result.put("xzTimeBegin", System.currentTimeMillis());
				result.put("summeryTimer", 10);
				// 把此信息添加到房间游戏过程
				room.getProcessList().add(result);
				CommonConstant.sendMsgEventToSingle(((Playerinfo) room.getPlayerMap().get(uuid)).getUuid(),
						result.toString(), "gameActionPush_ZJH");
			}
		} else {
			// 获取房间所有玩家的迭代器
			Iterator var10 = room.getUserPacketMap().keySet().iterator();
			while (var10.hasNext()) {
				// 获取玩家的uuid
				String uuid = (String) var10.next();
				if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {

					// code
					result.put("code", 1);
					result.put("gameStatus", room.getGameStatus());
					result.put("index", ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
					result.put("nextNum", room.getPlayerIndex(nextPlayer));
					result.put("gameNum", room.getGameNum());
					result.put("currentScore", room.getCurrentScore());
					result.put("totalScore", room.getTotalScore());
					// 总轮数
					result.put("totalGameNum", room.getTotalGameNum());
					// 房间最大下注筹码数
					result.put("maxScore", room.getMaxScore());
					result.put("myScore", ((UserPacket) room.getUserPacketMap().get(account)).getScore());
					result.put("score", 0);
					result.put("realScore", ((Playerinfo) room.getPlayerMap().get(account)).getScore());
					result.put("type", 2);
					result.put("isGameover", Integer.valueOf(isGameOver));
					if (isGameOver == 1) {
						// 把每个玩家的牌型信息存进去 最后比牌的时候发送给客户端
						for (String u : room.getPlayerMap().keySet()) {
							((UserPacket) room.getUserPacketMap().get(uuid)).addBiPaiList(
									room.getPlayerMap().get(u).getMyIndex(), room.getUserPacketMap().get(u).getPai());
						}
						// 游戏结算数据
						result.put("jiesuan", this.obtainSummaryData(room));
						// 所有玩家的牌型
						result.put("showPai", ((UserPacket) room.getUserPacketMap().get(uuid)).getBipaiList());
						result.put("type", 7);
						// 把数据添加到房间的结算数据中
						room.setSummaryData(result);
					}
					result.put("xzTimer", room.getXzTimer());
					result.put("xzTimeBegin", System.currentTimeMillis());
					result.put("summeryTimer", 10);
					if (account.equals(uuid)) {
						// 添加游戏过程 保证只添加了一次 并且改变游戏状态
						room.getProcessList().add(result);
						room.setTimeLeft(10);

						room.setGameStatus(ZJHConstant.ZJH_GAME_STATUS_SUMMARY);
						if (room.getGameIndex() != room.getGameCount()) {
							if (room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
								ThreadPoolHelper.executorService.submit(new Runnable() {
									@Override
									public void run() {
										gameTimerZJH.readyOverTime(room.getRoomNo(),
												ZJHConstant.ZJH_GAME_STATUS_SUMMARY, 10);
									}
								});

							}
						}
					}
					// 给每个玩家发送信息
					CommonConstant.sendMsgEventToSingle(((Playerinfo) room.getPlayerMap().get(uuid)).getUuid(),
							result.toString(), "gameActionPush_ZJH");
				}
			}
			// 如果游戏下注环节结束 并且房间的类型不为1
			if (isGameOver == 1 && room.getRoomType() != 1) {
				// 保存游戏日志（DAO层操作）
				this.saveGameLogs(room.getRoomNo());
			}
			// 判断是否为最后一局 是的话发送总结算数据给每个人
			this.sendFinalSummaryToPlayer(isGameOver, room);
		}

	}

	// 强制比牌
	public void compelCompare(ZJHGameRoomNew room) {
		// 除了强制比牌应该还有一个两个玩家之间的比牌
		Set<String> accountSet = room.getUserPacketMap().keySet();
		String maxAccount = "";
		int maxType = 0;
		for (String account : accountSet) {
			if (room.getUserPacketMap().get(account).getStatus() == 4
					|| room.getUserPacketMap().get(account).getStatus() == 7) {
				// 如果玩家为弃牌状态或者已经结算状态 直接判为输家 进行下一个玩家的判断
				room.getUserPacketMap().get(account).setStatus(5);
				continue;
			}
			room.getUserPacketMap().get(account).setStatus(5);
			if (room.getUserPacketMap().get(account).getType() > maxType) {
				maxAccount = account;
				maxType = room.getUserPacketMap().get(account).getType();
			} else if (room.getUserPacketMap().get(account).getType() == maxType) {
				// 如果两人类型一样 拿当前玩家和上一个最大的玩家比牌 如果返回1 则最大玩家变更
				int result = ZhaJinHuaCore.compareDaXiao(Arrays.asList(room.getUserPacketMap().get(account).getPai()),
						Arrays.asList(room.getUserPacketMap().get(maxAccount).getPai()));
				if (result == 1) {
					maxAccount = account;
				}
			}
		}
		// 设置为大赢家
		room.getUserPacketMap().get(maxAccount).setStatus(6);
		// 进行结算
		summary(room);
	}

	// 1V1比牌
	public void comPareEVE(String account1, String account2) {
	}

	public void summary(ZJHGameRoomNew room) {
		// 数据计算
		long summaryTimes = this.redisInfoService.summaryTimes(room.getRoomNo(), "_ZJH");
		if (summaryTimes <= 1L) {
			room.setGameStatus(3);
			room.setTimeLeft(0);
			Iterator var4 = room.getUserPacketMap().keySet().iterator();

			while (true) {
				String uuid;
				do {
					do {
						do {
							do {
								// 如果迭代器中没有下一位玩家了
								if (!var4.hasNext()) {
									// 更新玩家积分
									this.updateUserScore(room.getRoomNo());
									// 如果房间类型为3
									if (room.getRoomType() == 3) {
										this.saveUserDeductionData(room.getRoomNo());
									}
									// 如果为069
									if (room.getRoomType() == 0 || room.getRoomType() == 6 || room.getRoomType() == 9) {
										// 设置房间需要进行总结算
										room.setNeedFinalSummary(true);
										// 进行房卡结算
										this.roomCardSummary(room.getRoomNo());
										// 更新房卡
										this.updateRoomCard(room.getRoomNo());
									}
									return;
								}
								// 玩家的acount
								uuid = (String) var4.next();
							} while (!room.getUserPacketMap().containsKey(uuid));
						} while (room.getUserPacketMap().get(uuid) == null);
					} while (((UserPacket) room.getUserPacketMap().get(uuid)).getStatus() <= 0);

					// 如果玩家的状态为6 为大赢家
					if (((UserPacket) room.getUserPacketMap().get(uuid)).getStatus() == 6) {
						// 玩家的牌是否为豹子
						if (((UserPacket) room.getUserPacketMap().get(uuid)).getType() == ZhaJinHuaCore.TYPE_BAOZI) {
							int score = !Dto.isObjNull(room.getSetting())
									&& room.getSetting().containsKey("boom_reward")
											? room.getSetting().getInt("boom_reward")
											: 0;
							if (score > 0) {
								Iterator var7 = room.getUserPacketMap().keySet().iterator();

								while (var7.hasNext()) {
									String account = (String) var7.next();
									if (!account.equals(uuid)
											&& ((UserPacket) room.getUserPacketMap().get(account)).getStatus() != 0) {
										((Playerinfo) room.getPlayerMap().get(account))
												.setScore(((Playerinfo) room.getPlayerMap().get(account)).getScore()
														- (double) score);
										((UserPacket) room.getUserPacketMap().get(account))
												.setScore(((UserPacket) room.getUserPacketMap().get(account)).getScore()
														+ (double) score);
										room.setTotalScore(room.getTotalScore() + (double) score);
									}
								}
							}
						}
						// 获取玩家的player对象
						Playerinfo player = (Playerinfo) ((GameRoom) RoomManage.gameRoomMap.get(room.getRoomNo()))
								.getPlayerMap().get(uuid);
						// 获取对象的当前积分
						double oldScore = player.getScore();
						// 给房间里的palyer对象更新积分
						((Playerinfo) ((GameRoom) RoomManage.gameRoomMap.get(room.getRoomNo())).getPlayerMap()
								.get(uuid)).setScore(Dto.add(oldScore, room.getTotalScore()));
						// 给玩家在这个房间内的游戏增加总积分
						room.getUserPacketMap().get(uuid).setAllScore(Dto.add(
								room.getUserPacketMap().get(uuid).getAllScore(),
								room.getTotalScore() - ((UserPacket) room.getUserPacketMap().get(uuid)).getScore()));
						// 设置为下一轮的庄
						room.setBanker(uuid);
					} else {
						room.getUserPacketMap().get(uuid)
								.setAllScore(Dto.sub(room.getUserPacketMap().get(uuid).getAllScore(),
										((UserPacket) room.getUserPacketMap().get(uuid)).getScore()));
					}
				} while (room.getRoomType() != 3 && room.getRoomType() != 1 && room.getRoomType() != 8);

				if (((Playerinfo) ((GameRoom) RoomManage.gameRoomMap.get(room.getRoomNo())).getPlayerMap().get(uuid))
						.getScore() < 0.0D) {
					((Playerinfo) ((GameRoom) RoomManage.gameRoomMap.get(room.getRoomNo())).getPlayerMap().get(uuid))
							.setScore(0.0D);
				}
			}
		}

	}

	public void roomCardSummary(String roomNo) {
		ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (room != null) {
			Iterator var3 = room.getUserPacketMap().keySet().iterator();

			while (var3.hasNext()) {
				String account = (String) var3.next();
				if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null
						&& ((UserPacket) room.getUserPacketMap().get(account)).getStatus() > 0) {
					UserPacket up = (UserPacket) room.getUserPacketMap().get(account);
					if (up.getStatus() == 6) {
						up.setWinTimes(up.getWinTimes() + 1);
					}
				}
			}

		}
	}

	public void updateUserScore(String roomNo) {
		ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (room != null) {
			JSONArray array = new JSONArray();
			JSONObject pumpInfo = new JSONObject();
			Iterator var5 = room.getUserPacketMap().keySet().iterator();

			while (true) {
				String account;
				do {
					while (true) {
						do {
							do {
								do {
									if (!var5.hasNext()) {
										if (pumpInfo.size() > 0) {
											JSONObject object = new JSONObject();
											object.put("circleId", room.getCircleId());
											object.put("roomNo", room.getRoomNo());
											object.put("gameId", room.getGid());
											object.put("pumpInfo", pumpInfo);
											object.put("cutType", room.getCutType());
											object.put("changeType", "2");
											this.gameCircleService.circleUserPumping(object);
											this.userPumpingFee(room);
										}

										if (array.size() > 0) {
											this.producerService.sendMessage(this.daoQueueDestination,
													new PumpDao("update_score", room.getPumpObject(array)));
										}

										return;
									}

									account = (String) var5.next();
								} while (!room.getUserPacketMap().containsKey(account));
							} while (room.getUserPacketMap().get(account) == null);
						} while (((UserPacket) room.getUserPacketMap().get(account)).getStatus() <= 0);

						JSONObject obj = new JSONObject();
						if (room.getRoomType() != 3 && room.getRoomType() != 1) {
							break;
						}

						obj.put("total", ((Playerinfo) room.getPlayerMap().get(account)).getScore());
						if (((UserPacket) room.getUserPacketMap().get(account)).getStatus() == 6) {
							obj.put("fen", Dto.sub(room.getTotalScore(),
									((UserPacket) room.getUserPacketMap().get(account)).getScore()));
						} else {
							obj.put("fen", -((UserPacket) room.getUserPacketMap().get(account)).getScore());
						}

						obj.put("id", ((Playerinfo) room.getPlayerMap().get(account)).getId());
						array.add(obj);
					}
				} while (room.getRoomType() != 8 && room.getRoomType() != 9);

				if (((UserPacket) room.getUserPacketMap().get(account)).getStatus() == 6) {
					pumpInfo.put(String.valueOf(((Playerinfo) room.getPlayerMap().get(account)).getId()), Dto
							.sub(room.getTotalScore(), ((UserPacket) room.getUserPacketMap().get(account)).getScore()));
				} else {
					pumpInfo.put(String.valueOf(((Playerinfo) room.getPlayerMap().get(account)).getId()),
							-((UserPacket) room.getUserPacketMap().get(account)).getScore());
				}
			}
		}
	}

	private void userPumpingFee(ZJHGameRoomNew room) {
		if (room.isAgreeClose() || room.getRoomType() != 9
				|| !"1".equals(room.getCutType()) && !"2".equals(room.getCutType())
				|| room.getGameIndex() == room.getGameCount()) {
			Map<String, Double> pumpInfo = new HashMap();
			double maxScore = 0.0D;
			Iterator var5;
			String account;
			double winScore;
			if ("1".equals(room.getCutType()) || "2".equals(room.getCutType())) {
				if (room.getRoomType() == 9) {
					var5 = room.getPlayerMap().keySet().iterator();

					while (var5.hasNext()) {
						account = (String) var5.next();
						winScore = Dto.sub(((Playerinfo) room.getPlayerMap().get(account)).getScore(),
								((Playerinfo) room.getPlayerMap().get(account)).getSourceScore());
						if (winScore > maxScore) {
							maxScore = winScore;
						}
					}
				} else {
					var5 = room.getUserPacketMap().keySet().iterator();

					while (var5.hasNext()) {
						account = (String) var5.next();
						if (((UserPacket) room.getUserPacketMap().get(account)).getStatus() == 6) {
							winScore = Dto.sub(room.getTotalScore(),
									((UserPacket) room.getUserPacketMap().get(account)).getScore());
							if (winScore > maxScore) {
								maxScore = winScore;
							}
						}
					}
				}
			}

			var5 = room.getPlayerMap().keySet().iterator();

			while (var5.hasNext()) {
				account = (String) var5.next();
				if (((UserPacket) room.getUserPacketMap().get(account)).getStatus() > 0) {
					if ("1".equals(room.getCutType())) {
						if (room.getRoomType() == 9) {
							if (((Playerinfo) room.getPlayerMap().get(account)).getScore()
									- ((Playerinfo) room.getPlayerMap().get(account)).getSourceScore() == maxScore) {
								((Playerinfo) ((GameRoom) RoomManage.gameRoomMap.get(room.getRoomNo())).getPlayerMap()
										.get(account)).setScore(
												Dto.sub(((Playerinfo) room.getPlayerMap().get(account)).getScore(),
														room.getPump() * maxScore / 100.0D));
								((Playerinfo) ((GameRoom) RoomManage.gameRoomMap.get(room.getRoomNo())).getPlayerMap()
										.get(account))
												.setSourceScore(Dto.sub(((Playerinfo) room.getPlayerMap().get(account))
														.getSourceScore(), room.getPump() * maxScore / 100.0D));
								pumpInfo.put(String.valueOf(((Playerinfo) room.getPlayerMap().get(account)).getId()),
										-room.getPump() * maxScore / 100.0D);
							}
						} else {
							winScore = Dto.sub(room.getTotalScore(),
									((UserPacket) room.getUserPacketMap().get(account)).getScore());
							if (((UserPacket) room.getUserPacketMap().get(account)).getStatus() == 6
									&& winScore == maxScore) {
								((Playerinfo) ((GameRoom) RoomManage.gameRoomMap.get(room.getRoomNo())).getPlayerMap()
										.get(account)).setScore(
												Dto.sub(((Playerinfo) room.getPlayerMap().get(account)).getScore(),
														room.getPump() * winScore / 100.0D));
								((Playerinfo) ((GameRoom) RoomManage.gameRoomMap.get(room.getRoomNo())).getPlayerMap()
										.get(account))
												.setSourceScore(Dto.sub(((Playerinfo) room.getPlayerMap().get(account))
														.getSourceScore(), room.getPump() * winScore / 100.0D));
								pumpInfo.put(String.valueOf(((Playerinfo) room.getPlayerMap().get(account)).getId()),
										-room.getPump() * winScore / 100.0D);
							}
						}
					} else if ("2".equals(room.getCutType())) {
						if (room.getRoomType() == 9) {
							if (((Playerinfo) room.getPlayerMap().get(account)).getScore()
									- ((Playerinfo) room.getPlayerMap().get(account)).getSourceScore() > 0.0D) {
								winScore = ((Playerinfo) room.getPlayerMap().get(account)).getScore()
										- ((Playerinfo) room.getPlayerMap().get(account)).getSourceScore();
								((Playerinfo) ((GameRoom) RoomManage.gameRoomMap.get(room.getRoomNo())).getPlayerMap()
										.get(account)).setScore(
												Dto.sub(((Playerinfo) room.getPlayerMap().get(account)).getScore(),
														room.getPump() * winScore / 100.0D));
								((Playerinfo) ((GameRoom) RoomManage.gameRoomMap.get(room.getRoomNo())).getPlayerMap()
										.get(account))
												.setSourceScore(Dto.sub(((Playerinfo) room.getPlayerMap().get(account))
														.getSourceScore(), room.getPump() * winScore / 100.0D));
								pumpInfo.put(String.valueOf(((Playerinfo) room.getPlayerMap().get(account)).getId()),
										-room.getPump() * winScore / 100.0D);
							}
						} else if (((UserPacket) room.getUserPacketMap().get(account)).getStatus() == 6) {
							winScore = Dto.sub(room.getTotalScore(),
									((UserPacket) room.getUserPacketMap().get(account)).getScore());
							if (winScore > 0.0D && room.getUserPacketMap().get(account) != null
									&& ((UserPacket) room.getUserPacketMap().get(account)).getScore() > 0.0D) {
								((Playerinfo) ((GameRoom) RoomManage.gameRoomMap.get(room.getRoomNo())).getPlayerMap()
										.get(account)).setScore(
												Dto.sub(((Playerinfo) room.getPlayerMap().get(account)).getScore(),
														room.getPump() * winScore / 100.0D));
								((Playerinfo) ((GameRoom) RoomManage.gameRoomMap.get(room.getRoomNo())).getPlayerMap()
										.get(account))
												.setSourceScore(Dto.sub(((Playerinfo) room.getPlayerMap().get(account))
														.getSourceScore(), room.getPump() * winScore / 100.0D));
								pumpInfo.put(String.valueOf(((Playerinfo) room.getPlayerMap().get(account)).getId()),
										-room.getPump() * winScore / 100.0D);
							}
						}
					} else if ("3".equals(room.getCutType())) {
						((Playerinfo) ((GameRoom) RoomManage.gameRoomMap.get(room.getRoomNo())).getPlayerMap()
								.get(account))
										.setScore(Dto.sub(((Playerinfo) room.getPlayerMap().get(account)).getScore(),
												(double) room.getScore() * room.getPump() / 100.0D));
						((Playerinfo) ((GameRoom) RoomManage.gameRoomMap.get(room.getRoomNo())).getPlayerMap()
								.get(account)).setSourceScore(
										Dto.sub(((Playerinfo) room.getPlayerMap().get(account)).getSourceScore(),
												(double) room.getScore() * room.getPump() / 100.0D));
						pumpInfo.put(String.valueOf(((Playerinfo) room.getPlayerMap().get(account)).getId()),
								(double) (-room.getScore()) * room.getPump() / 100.0D);
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
				object.put("changeType", "3");
				this.gameCircleService.circleUserPumping(object);
			}

		}
	}

	public void updateRoomCard(String roomNo) {
		ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		JSONArray array = new JSONArray();
		int roomCardCount = 0;
		if (room.getRoomType() == 0 || room.getRoomType() == 6 || room.getRoomType() == 9) {
			Iterator var5 = room.getUserPacketMap().keySet().iterator();

			while (var5.hasNext()) {
				String account = (String) var5.next();
				if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
					if ("0".equals(room.getPayType()) && account.equals(room.getOwner())
							&& ((Playerinfo) room.getPlayerMap().get(account)).getPlayTimes() == 1) {
						array.add(((Playerinfo) room.getPlayerMap().get(account)).getId());
						roomCardCount = room.getPlayerCount() * room.getSinglePayNum();
					}

					if ("1".equals(room.getPayType())
							&& ((Playerinfo) room.getPlayerMap().get(account)).getPlayTimes() == 1) {
						array.add(((Playerinfo) room.getPlayerMap().get(account)).getId());
						roomCardCount = room.getSinglePayNum();
					}
				}
			}
		}

		if (array.size() > 0 && room.getRoomType() != 8 && room.getRoomType() != 9) {
			this.producerService.sendMessage(this.daoQueueDestination,
					new PumpDao("pump", room.getRoomCardChangeObject(array, roomCardCount)));
		}

	}

	public void saveUserDeductionData(String roomNo) {
		ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		JSONArray userDeductionData = new JSONArray();
		Iterator var4;
		if (room.getOutUserData().size() > 0) {
			var4 = room.getOutUserData().iterator();

			while (var4.hasNext()) {
				JSONObject outUserData = (JSONObject) var4.next();
				userDeductionData.add(outUserData.getJSONObject("userDeduction"));
			}
		}

		var4 = room.getUserPacketMap().keySet().iterator();

		while (var4.hasNext()) {
			String account = (String) var4.next();
			if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null
					&& ((UserPacket) room.getUserPacketMap().get(account)).getStatus() > 0) {
				JSONObject object = this.obtainUserDeductionData(account, room);
				userDeductionData.add(object);
			}
		}

		this.producerService.sendMessage(this.daoQueueDestination,
				new PumpDao("user_deduction", (new JSONObject()).element("user", userDeductionData)));
	}

	public void saveGameLogs(String roomNo) {
		ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null != room && (-1 != room.getIsClose() || 3 == room.getGameStatus() || 4 == room.getGameStatus())) {
			JSONArray array = new JSONArray();
			JSONArray gameLogResults = new JSONArray();
			JSONArray gameResult = new JSONArray();
			JSONArray gameProcessJS = new JSONArray();
			Iterator var7;
			JSONObject o;
			if (room.getOutUserData().size() > 0) {
				var7 = room.getOutUserData().iterator();

				while (var7.hasNext()) {
					o = (JSONObject) var7.next();
					gameProcessJS.add(o.getJSONObject("userSummary"));
					gameLogResults.add(o.getJSONObject("gameLog"));
					gameResult.add(o.getJSONObject("userGameLog"));
					array.add(o.getJSONObject("obj"));
				}
			}

			var7 = room.getUserPacketMap().keySet().iterator();
			while (var7.hasNext()) {
				String account = (String) var7.next();
				if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null
						&& ((UserPacket) room.getUserPacketMap().get(account)).getStatus() > 0) {
					JSONObject userJS = this.obtainUserSummaryData(account, room);
					gameProcessJS.add(userJS);
					JSONObject obj = new JSONObject();
					obj.put("total", ((Playerinfo) room.getPlayerMap().get(account)).getScore());
					if (((UserPacket) room.getUserPacketMap().get(account)).getStatus() == 6) {
						obj.put("fen", Dto.sub(room.getTotalScore(),
								((UserPacket) room.getUserPacketMap().get(account)).getScore()));
					} else {
						obj.put("fen", -((UserPacket) room.getUserPacketMap().get(account)).getScore());
					}

					obj.put("id", ((Playerinfo) room.getPlayerMap().get(account)).getId());
					array.add(obj);
					JSONObject gameLogResult = this.obtainGameLogData(account, room);
					gameLogResults.add(gameLogResult);
					JSONObject userResult = this.obtainUserResult(account, room);
					gameResult.add(userResult);
				}

			}
			room.getGameProcess().put("xiazhu", room.getXiaZhuList());
			room.getGameProcess().put("JieSuan", gameProcessJS);
			logger.info(room.getRoomNo() + "---" + room.getGameProcess());
			JSONObject gameLogObj = room.obtainGameLog(String.valueOf(gameLogResults),
					String.valueOf(room.getProcessList()));
			this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("insert_game_log", gameLogObj));
			o = new JSONObject();
			JSONArray userGameLogs = room.obtainUserGameLog(gameLogObj.getLong("id"), array);
			o.element("array", userGameLogs);

			o.element("object",
					(new JSONObject()).element("gamelog_id", gameLogObj.getLong("id"))
							.element("room_type", room.getRoomType()).element("room_id", room.getId())
							.element("room_no", room.getRoomNo()).element("game_index", room.getGameNewIndex())
							.element("result", gameResult.toString()));
			this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("insert_user_game_log", o));

		}
	}

	public JSONObject obtainUserResult(String account, ZJHGameRoomNew room) {
		JSONObject userResult = new JSONObject();
		userResult.put("zhuang", room.getBanker());
		if (((UserPacket) room.getUserPacketMap().get(account)).getStatus() == 6) {
			userResult.put("isWinner", 1);
			userResult.put("score",
					Dto.sub(room.getTotalScore(), ((UserPacket) room.getUserPacketMap().get(account)).getScore()));
		} else {
			userResult.put("isWinner", 0);
			userResult.put("score", -((UserPacket) room.getUserPacketMap().get(account)).getScore());
		}

		userResult.put("totalScore", ((Playerinfo) room.getPlayerMap().get(account)).getScore());
		userResult.put("player", ((Playerinfo) room.getPlayerMap().get(account)).getName());
		userResult.put("account", account);
		return userResult;
	}

	public JSONObject obtainGameLogData(String account, ZJHGameRoomNew room) {
		JSONObject gameLogResult = new JSONObject();
		gameLogResult.put("account", account);
		gameLogResult.put("name", ((Playerinfo) room.getPlayerMap().get(account)).getName());
		gameLogResult.put("headimg", ((Playerinfo) room.getPlayerMap().get(account)).getHeadimg());
		gameLogResult.put("zhuang", ((Playerinfo) room.getPlayerMap().get(room.getBanker())).getMyIndex());
		gameLogResult.put("myIndex", ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
		gameLogResult.put("myPai", ((UserPacket) room.getUserPacketMap().get(account)).getPai());
		gameLogResult.put("totalScore", ((Playerinfo) room.getPlayerMap().get(account)).getScore());
		if (((UserPacket) room.getUserPacketMap().get(account)).getStatus() == 6) {
			gameLogResult.put("win", 1);
			gameLogResult.put("score",
					Dto.sub(room.getTotalScore(), ((UserPacket) room.getUserPacketMap().get(account)).getScore()));
		} else {
			gameLogResult.put("score", -((UserPacket) room.getUserPacketMap().get(account)).getScore());
			gameLogResult.put("win", 0);
		}

		return gameLogResult;
	}

	public JSONObject obtainUserDeductionData(String account, ZJHGameRoomNew room) {
		JSONObject object = new JSONObject();
		object.put("id", ((Playerinfo) room.getPlayerMap().get(account)).getId());
		object.put("gid", room.getGid());
		object.put("roomNo", room.getRoomNo());
		object.put("type", room.getRoomType());
		object.put("new", ((Playerinfo) room.getPlayerMap().get(account)).getScore());
		if (((UserPacket) room.getUserPacketMap().get(account)).getStatus() == 6) {
			object.put("fen",
					Dto.sub(room.getTotalScore(), ((UserPacket) room.getUserPacketMap().get(account)).getScore()));
			object.put("old",
					Dto.sub(((Playerinfo) room.getPlayerMap().get(account)).getScore(), object.getDouble("fen")));
		} else {
			object.put("fen", -((UserPacket) room.getUserPacketMap().get(account)).getScore());
			object.put("old",
					Dto.sub(((Playerinfo) room.getPlayerMap().get(account)).getScore(), object.getDouble("fen")));
		}

		return object;
	}

	public JSONObject obtainUserSummaryData(String account, ZJHGameRoomNew room) {
		JSONObject userJS = new JSONObject();
		userJS.put("account", account);
		userJS.put("name", ((Playerinfo) room.getPlayerMap().get(account)).getName());
		userJS.put("pai", ((UserPacket) room.getUserPacketMap().get(account)).getPai());
		userJS.put("paiType", ((UserPacket) room.getUserPacketMap().get(account)).getType());
		userJS.put("new", ((Playerinfo) room.getPlayerMap().get(account)).getScore());
		if (((UserPacket) room.getUserPacketMap().get(account)).getStatus() == 6) {
			userJS.put("sum",
					Dto.sub(room.getTotalScore(), ((UserPacket) room.getUserPacketMap().get(account)).getScore()));
			userJS.put("old",
					Dto.sub(((Playerinfo) room.getPlayerMap().get(account)).getScore(), userJS.getDouble("sum")));
		} else {
			userJS.put("sum", -((UserPacket) room.getUserPacketMap().get(account)).getScore());
			userJS.put("old",
					Dto.sub(((Playerinfo) room.getPlayerMap().get(account)).getScore(), userJS.getDouble("sum")));
		}

		return userJS;
	}

	public void exitRoom(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (CommonConstant.checkEvent(postData, -1, client)) {
			String roomNo = postData.getString("room_no");
			ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
			String account = postData.getString("account");
			if (!Dto.stringIsNULL(account) && room.getUserPacketMap().containsKey(account)
					&& room.getUserPacketMap().get(account) != null) {
				boolean canExit = false;
				JSONObject obj;
				JSONObject roomInfo;
				if (room.getRoomType() != 1 && room.getRoomType() != 3 && room.getRoomType() != 8) {
					if (room.getRoomType() == 0 || room.getRoomType() == 6 || room.getRoomType() == 9) {
						if (((Playerinfo) room.getPlayerMap().get(account)).getPlayTimes() == 0) {
							if (!"1".equals(room.getPayType()) && room.getOwner().equals(account)) {
								if (room.getRoomType() == 9) {
									canExit = true;
								}
							} else {
								canExit = true;
							}
						}

						if (room.getGameStatus() == 4) {
							canExit = true;
						}
					}
				} else if (((UserPacket) room.getUserPacketMap().get(account)).getStatus() == 0) {
					canExit = true;
				} else if (room.getGameStatus() != 0 && room.getGameStatus() != 1 && room.getGameStatus() != 3) {
					if (((UserPacket) room.getUserPacketMap().get(account)).getStatus() == 4) {
						canExit = true;
						JSONArray array = new JSONArray();
						obj = new JSONObject();
						obj.put("total", ((Playerinfo) room.getPlayerMap().get(account)).getScore());
						if (((UserPacket) room.getUserPacketMap().get(account)).getStatus() == 6) {
							obj.put("fen", Dto.sub(room.getTotalScore(),
									((UserPacket) room.getUserPacketMap().get(account)).getScore()));
						} else {
							obj.put("fen", -((UserPacket) room.getUserPacketMap().get(account)).getScore());
						}

						obj.put("id", ((Playerinfo) room.getPlayerMap().get(account)).getId());
						array.add(obj);
						this.producerService.sendMessage(this.daoQueueDestination,
								new PumpDao("update_score", room.getPumpObject(array)));
						roomInfo = new JSONObject();
						roomInfo.put("userSummary", this.obtainUserSummaryData(account, room));
						roomInfo.put("gameLog", this.obtainGameLogData(account, room));
						roomInfo.put("userDeduction", this.obtainUserDeductionData(account, room));
						roomInfo.put("userGameLog", this.obtainUserResult(account, room));
						roomInfo.put("obj", obj);
						room.getOutUserData().add(roomInfo);
					}
				} else {
					canExit = true;
				}

				Playerinfo player = (Playerinfo) room.getPlayerMap().get(account);
				if (canExit) {
					List<UUID> allUUIDList = room.getAllUUIDList();
					if (room.getBanker().equals(account)) {
						String newBanker = room.getNextPlayer(account);
						room.setBanker(newBanker);
					}

					for (int i = 0; i < room.getUserIdList().size(); ++i) {
						if ((Long) room.getUserIdList().get(i) == ((Playerinfo) room.getPlayerMap().get(account))
								.getId()) {
							room.getUserIdList().set(i, 0L);
							room.addIndexList(((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
							break;
						}
					}

					roomInfo = new JSONObject();
					roomInfo.put("roomNo", room.getRoomNo());
					roomInfo.put("roomId", room.getId());
					roomInfo.put("userIndex", ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
					room.getPlayerMap().remove(account);
					room.getVisitPlayerMap().remove(account);
					room.getUserPacketMap().remove(account);
					roomInfo.put("player_number", room.getPlayerMap().size());
					this.producerService.sendMessage(this.daoQueueDestination,
							new PumpDao("update_quit_user_room", roomInfo));
					JSONObject result = new JSONObject();
					result.put("code", 1);
					result.put("type", 1);
					result.put("index", player.getMyIndex());
					if (room.getGameStatus() == 1 && room.getNowReadyCount() < 2) {
						room.setTimeLeft(0);
					}

					if (room.getTimeLeft() > 0) {
						result.put("showTimer", 1);
					} else {
						result.put("showTimer", 0);
					}

					result.put("timer", room.getTimeLeft());
					if (!postData.containsKey("notSend")) {
						CommonConstant.sendMsgEventToAll(allUUIDList, result.toString(), "exitRoomPush_ZJH");
					}

					if (postData.containsKey("notSendToMe")) {
						CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "exitRoomPush_ZJH");
					}

					int minStartCount = !Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("mustFull")
							? room.getPlayerCount()
							: 2;
					if (room.getPlayerMap().size() <= room.getPlayerCount() && room.isAllReady()
							&& room.getPlayerMap().size() >= minStartCount) {
						this.startGame(room.getRoomNo());
					}

					if (room.getPlayerMap().size() == 0) {
						this.redisInfoService.delSummary(room.getRoomNo(), "_ZJH");
						if (8 != room.getRoomType() && 9 != room.getRoomType()) {
							roomInfo.put("status", room.getIsClose());
						} else {
							roomInfo.put("status", 0);
						}

						roomInfo.put("game_index", 0);
						if (10 == room.getRoomType() || 9 == room.getRoomType()) {
							this.copyGameRoom(roomNo, (Playerinfo) null);
						} else {
							RoomManage.gameRoomMap.remove(roomNo);
						}
					}
				} else {
					obj = new JSONObject();
					obj.put("code", 0);
					obj.put("msg", "游戏中无法退出");
					obj.put("showTimer", 0);
					obj.put("timer", room.getTimeLeft());
					obj.put("type", 1);
					CommonConstant.sendMsgEventToSingle(client, obj.toString(), "exitRoomPush_ZJH");
				}
			}

		}
	}

	private void copyGameRoom(String roomNo, Playerinfo playerinfo) {
		ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		room.initGame();
		room.setGameIndex(0);
		room.setGameNewIndex(0);
		room.getSummaryData().clear();
		room.setGameStatus(0);
		room.setJieSanTime(0);
		room.setTimeLeft(0);
		room.setFinalSummaryData(new JSONArray());
		room.setUserPacketMap(new ConcurrentHashMap());
		room.setPlayerMap(new ConcurrentHashMap());
		room.setVisitPlayerMap(new ConcurrentHashMap());
		room.setOpen(true);

		for (int i = 0; i < room.getUserIdList().size(); ++i) {
			room.getUserIdList().set(i, 0L);
			room.addIndexList(i);
		}

		this.baseEventDeal.reloadClearRoom(roomNo);
	}

	public void closeRoom(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (CommonConstant.checkEvent(postData, -1, client)) {
			final String roomNo = postData.getString("room_no");
			ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
			String account = postData.getString("account");
			if (postData.containsKey("type")) {
				JSONObject result = new JSONObject();
				int type = postData.getInt("type");
				if (type == 1 && room.getJieSanTime() == 0) {
					final int closeTime = !Dto.isObjNull(room.getSetting())
							&& room.getSetting().containsKey("closeTime") ? room.getSetting().getInt("closeTime") : 60;
					room.setJieSanTime(closeTime);
					ThreadPoolHelper.executorService.submit(new Runnable() {
						public void run() {
							ZJHGameEventDealNew.this.gameTimerZJH.closeRoomOverTime(roomNo, closeTime);
						}
					});
				}

				((Playerinfo) room.getPlayerMap().get(account)).setIsCloseRoom(type);
				if (type == -1) {
					room.setJieSanTime(0);
					Iterator var12 = room.getUserPacketMap().keySet().iterator();

					while (var12.hasNext()) {
						String uuid = (String) var12.next();
						if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
							((Playerinfo) room.getPlayerMap().get(uuid)).setIsCloseRoom(0);
						}
					}

					result.put("code", 0);
					String[] names = new String[] { ((Playerinfo) room.getPlayerMap().get(account)).getName() };
					result.put("names", names);
					CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush_ZJH");
					return;
				}

				if (type == 1) {
					if (room.isAgreeClose()) {
						if (!room.isNeedFinalSummary()) {
							List<UUID> uuidList = room.getAllUUIDList();
							this.copyGameRoom(roomNo, (Playerinfo) null);
							RoomManage.gameRoomMap.remove(roomNo);
							result.put("type", 2);
							result.put("msg", "房间已解散");
							CommonConstant.sendMsgEventToAll(uuidList, result.toString(), "tipMsgPush");
							return;
						}

						this.userPumpingFee(room);
						room.setGameStatus(4);
						if (room.getRoomType() == 6) {
							room.setOpen(false);
						}

						result.put("code", 1);
						result.put("type", 8);
						result.put("isClose", 1);
						result.put("jiesuanData", room.obtainFinalSummaryData());
						CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(),
								"gameActionPush_ZJH");
					} else {
						((Playerinfo) room.getPlayerMap().get(account)).setIsCloseRoom(1);
						result.put("code", 1);
						result.put("data", room.getJieSanData());
						CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush_ZJH");
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
			if (userInfo.containsKey("uuid") && !Dto.stringIsNULL(userInfo.getString("uuid"))
					&& userInfo.getString("uuid").equals(postData.getString("uuid"))) {
				JSONObject result = new JSONObject();
				if (client != null) {
					if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
						ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
						if (!Dto.stringIsNULL(account) && room.getPlayerMap().containsKey(account)
								&& room.getPlayerMap().get(account) != null) {
							((Playerinfo) room.getPlayerMap().get(account)).setUuid(client.getSessionId());
							((Playerinfo) room.getPlayerMap().get(account)).setStatus(1);
							result.put("type", 1);
							result.put("data", this.obtainRoomData(roomNo, account));
							CommonConstant.sendMsgEventToSingle(client, result.toString(), "reconnectGamePush_ZJH");
							JSONObject obj = new JSONObject();
							obj.put("index", ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
							CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(obj),
									"userReconnectPush");
						} else {
							result.put("type", 0);
							CommonConstant.sendMsgEventToSingle(client, result.toString(), "reconnectGamePush_ZJH");
						}
					} else {
						result.put("type", 0);
						CommonConstant.sendMsgEventToSingle(client, result.toString(), "reconnectGamePush_ZJH");
					}
				}
			}
		}
	}

	public JSONObject obtainStartData(ZJHGameRoomNew room, String player) {
		JSONObject object = new JSONObject();
		object.put("gameStatus", room.getGameStatus());
		object.put("zhuang", room.getPlayerIndex(room.getBanker()));
		object.put("game_index", room.getGameIndex());
		object.put("nextNum", room.getPlayerIndex(player));
		object.put("gameNum", room.getGameNum());
		object.put("currentScore", room.getCurrentScore());
		object.put("totalScore", room.getTotalScore());
		object.put("users", room.getAllPlayer());
		return object;
	}

	public JSONObject obtainRoomData(String roomNo, String account) {
		ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		JSONObject obj = new JSONObject();
		obj.put("playerCount", room.getPlayerCount());
		obj.put("room_no", room.getRoomNo());
		obj.put("roomType", room.getRoomType());
		obj.put("game_count", room.getGameCount());
		obj.put("xzTimer", room.getXzTimer());
		if (room.getRoomType() == 6) {
			obj.put("clubCode", room.getClubCode());
		}

		StringBuffer roominfo;
		if (room.getRoomType() == 3 || room.getRoomType() == 8) {
			roominfo = new StringBuffer();
			roominfo.append("底注:");
			roominfo.append(room.getScore());
			roominfo.append("进:");
			roominfo.append((int) room.getEnterScore());
			roominfo.append(" 出:");
			roominfo.append((int) room.getLeaveScore());
			StringBuffer diInfo = new StringBuffer();
			diInfo.append("底注:");
			diInfo.append(room.getScore());
			StringBuffer roominfo3 = new StringBuffer();
			roominfo3.append("进:");
			roominfo3.append((int) room.getEnterScore());
			roominfo3.append(" 出:");
			roominfo3.append((int) room.getLeaveScore());
			obj.put("diInfo", diInfo.toString());
			obj.put("roominfo3", roominfo3.toString());
			obj.put("roominfo", roominfo.toString());
			obj.put("roominfo2", room.getWfType());
		}

		if (room.getRoomType() == 0 || room.getRoomType() == 6 || room.getRoomType() == 9) {
			roominfo = new StringBuffer();
			roominfo.append(room.getWfType());
			roominfo.append(" ");
			roominfo.append(room.getPlayerCount());
			roominfo.append("人 ");
			roominfo.append(room.getGameCount());
			roominfo.append("局");
			obj.put("roominfo", roominfo.toString());
		}

		obj.put("baseNum", room.getBaseNum());
		obj.put("totalGameNum", room.getTotalGameNum());
		obj.put("dizhu", room.getScore());
		obj.put("wanfa", room.getGameType());
		obj.put("gameStatus", room.getGameStatus());
		if (!Dto.stringIsNULL(room.getBanker()) && room.getPlayerMap().containsKey(room.getBanker())
				&& room.getPlayerMap().get(room.getBanker()) != null) {
			obj.put("zhuang", ((Playerinfo) room.getPlayerMap().get(room.getBanker())).getMyIndex());
		} else {
			obj.put("zhuang", -1);
		}

		obj.put("game_index", room.getGameIndex());
		obj.put("showTimer", 0);
		if (room.getTimeLeft() > 0) {
			obj.put("showTimer", 1);
		}

		obj.put("timer", room.getTimeLeft());
		if (room.getGameStatus() == 2) {
			obj.put("timer", room.getXzTimer());
		}

		obj.put("myIndex", ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex());
		if (((UserPacket) room.getUserPacketMap().get(account)).getStatus() == 3
				|| ((UserPacket) room.getUserPacketMap().get(account)).getStatus() == 4) {
			obj.put("mypai", ((UserPacket) room.getUserPacketMap().get(account)).getPai());
			obj.put("paiType", ((UserPacket) room.getUserPacketMap().get(account)).getType());
		}

		obj.put("users", room.getAllPlayer());
		obj.put("isGendaodi", ((UserPacket) room.getUserPacketMap().get(account)).isGenDaoDi);
		obj.put("nextNum", room.getPlayerIndex(room.getFocus()));
		obj.put("gameNum", room.getGameNum());
		obj.put("currentScore", room.getCurrentScore());
		obj.put("totalScore", room.getTotalScore());
		obj.put("xiazhuList", room.getXiaZhuList());
		obj.put("myScore", room.getPlayerScore());
		obj.put("isGameover", 1);
		if (room.getProgressIndex().length > 1) {
			obj.put("isGameover", 0);
		}

		if (room.getGameStatus() == 3) {
			// 把每个玩家的牌型信息存进去 最后比牌的时候发送给客户端
			for (String uuid : room.getPlayerMap().keySet()) {
				for (String u : room.getPlayerMap().keySet()) {
					((UserPacket) room.getUserPacketMap().get(uuid)).addBiPaiList(
							room.getPlayerMap().get(u).getMyIndex(), room.getUserPacketMap().get(u).getPai());
				}
			}
			obj.put("jiesuan", this.obtainSummaryData(room));
			obj.put("showPai", ((UserPacket) room.getUserPacketMap().get(account)).getBipaiList());
		} else {
			obj.put("jiesuan", new JSONArray());
			obj.put("showPai", new JSONArray());
		}

		if (room.getGameStatus() == 4) {
			obj.put("jiesuanData", room.getFinalSummaryData());
		}

		if (room.getJieSanTime() > 0) {
			obj.put("jiesan", 1);
			obj.put("jiesanData", room.getJieSanData());
		}

		int score = !Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("boom_reward")
				? room.getSetting().getInt("boom_reward")
				: 0;
		if (score > 0) {
			obj.put("boom_reward", "豹子奖励：" + score);
		}

		return obj;
	}

	private void sendStartResultToSingle(SocketIOClient client, String eventName, int code, String msg) {
		JSONObject result = new JSONObject();
		result.put(CommonConstant.RESULT_KEY_CODE, code);
		result.put(CommonConstant.RESULT_KEY_MSG, msg);
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), eventName);
	}

	public void changeGameStatus(ZJHGameRoomNew room) {
		if (null == room)
			return;
		JSONObject obj = new JSONObject();
		obj.put("gameStatus", room.getGameStatus());
		obj.put("users", room.getAllPlayer());
		// 下注的玩家
		obj.put("foucs", room.getFocus());
		// 判断当前哪位玩家开始下注 index
		if ("".equals(obj.getString("foucs")) || null == obj.getString("foucs")) {
			obj.put("index", 0);
		} else {
			obj.put("index", room.getPlayerMap().get(room.getFocus()).getMyIndex());
		}
		// 开启倒计时
		obj.put("xzTimer", room.getXzTimer());
		obj.put("xzTimeBegin", new Date().getTime());
		obj.put("timeLeft", room.getTimeLeft());
		obj.put("game_index", room.getGameNewIndex());
		obj.put("showTimer", CommonConstant.GLOBAL_NO);
		if (room.getTimeLeft() > ZJHConstant.ZJH_TIMER_INIT) {
			obj.put("showTimer", CommonConstant.GLOBAL_YES);
		}
		if (room.getGameStatus() == ZJHConstant.ZJH_GAME_STATUS_SUMMARY) {
			obj.put("showTimer", CommonConstant.GLOBAL_NO);
		}
		// obj.put("gameData", room.getgame);
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_DK
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING
				|| CommonConstant.ROOM_TYPE_TEA == room.getRoomType()) {
			if (room.getGameStatus() == ZJHConstant.ZJH_GAME_STATUS_FINAL_SUMMARY) {
				obj.put("jiesuanData", room.obtainFinalSummaryData());// 结算通知
			}
			if (room.getGameStatus() == ZJHConstant.ZJH_GAME_STATUS_SUMMARY
					&& room.getGameIndex() >= room.getGameCount()) {
				obj.put("jiesuanData", room.obtainFinalSummaryData());
			}
		}
		// 显示当前下注轮数 roomData.gameNum;
		obj.put("gameNum", room.getGameNum());
		// 显示当前下注筹码数 roomData.currentScore
		obj.put("currentScore", room.getCurrentScore());

		// 显示当前下注总筹码数 totalScore
		obj.put("totalScore", room.getTotalScore());
		// 总轮数
		obj.put("totalGameNum", room.getTotalGameNum());
		// 房间最大下注筹码数
		obj.put("maxScore", room.getMaxScore());
		// 显示当前各玩家筹码数 usersinfo
		obj.put("dizhu", room.getScore());
		// 显示当前各玩家牌面
		// 给自己发自己的牌显示
		for (String account : room.getPlayerMap().keySet()) {
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (null == playerinfo)
				continue;
			obj.put("roomNo", room.getRoomNo());

			CommonConstant.sendMsgEventToSingle(playerinfo.getUuid(), obj.toString(), "changeGameStatusPush_ZJH");
		}

		for (String account : room.getVisitPlayerMap().keySet()) {// 观战玩家
			Playerinfo playerinfo = room.getVisitPlayerMap().get(account);
			if (null == playerinfo)
				continue;

			obj.put("myPai", "");
			obj.put("myPaiType", "0");
			obj.put("roomNo", room.getRoomNo());

			CommonConstant.sendMsgEventToSingle(playerinfo.getUuid(), obj.toString(), "changeGameStatusPush_ZJH");
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

	public void forcedRoom(Object data) {
		logger.error("炸金花解散");
		JSONObject postData = JSONObject.fromObject(data);
		if (!postData.containsKey(CommonConstant.DATA_KEY_ROOM_NO))
			return;
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
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

	private void removeRoom(String roomNo) {
		ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		// 房间解散
		redisInfoService.delSummary(roomNo, "_ZJH");
		JSONObject roomInfo = new JSONObject();
		roomInfo.put("room_no", room.getRoomNo());
		roomInfo.put("status", CommonConstant.CLOSE_ROOM_TYPE_OVER);
		roomInfo.put("game_index", 0);
		producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
		logger.error("清除数组信息");
		RoomManage.gameRoomMap.remove(roomNo);
	}

}
