package com.zhuoan.biz.event.qzmj;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.qzmj.MaJiangCore;
import com.zhuoan.biz.event.BaseEventDeal;
import com.zhuoan.biz.game.biz.GameCircleService;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.qzmj.DontMovePai;
import com.zhuoan.biz.model.qzmj.KaiJuModel;
import com.zhuoan.biz.model.qzmj.QZMJGameRoom;
import com.zhuoan.biz.model.qzmj.QZMJUserPacket;
import com.zhuoan.biz.robot.RobotEventDeal;
import com.zhuoan.constant.*;
import com.zhuoan.constant.event.CommonEventConstant;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.TeaService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.AppBeanUtil;
import com.zhuoan.util.DateUtils;
import com.zhuoan.util.Dto;
import com.zhuoan.util.JsonUtil;
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
public class QZMJGameEventDeal {

	private final static Logger logger = LoggerFactory.getLogger(QZMJGameEventDeal.class);
	@Resource
	private GameTimerQZMJ gameTimerQZMJ;
	@Resource
	private Destination daoQueueDestination;
	@Resource
	private ProducerService producerService;
	@Resource
	private UserBiz userBiz;
	@Resource
	private GameCircleService gameCircleService;
	@Resource
	private RedisInfoService redisInfoService;
	@Resource
	private BaseEventDeal baseEventDeal;
	@Resource
	private TeaService teaService;

	public void createRoom(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		JSONObject roomData = obtainEnterData(roomNo, account);
		// 数据不为空
		if (!Dto.isObjNull(roomData)) {
			JSONObject result = new JSONObject();
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			result.put("data", roomData);
			// 通知自己
			CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush");
		}
	}

	public void joinRoom(SocketIOClient client, Object data) {
		JSONObject joinData = JSONObject.fromObject(data);
		String account = joinData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap
				.get(joinData.getString(CommonConstant.DATA_KEY_ROOM_NO));
		if (null == room)
			return;
		int isReconnect = CommonConstant.GLOBAL_NO;
		if (joinData.containsKey("isReconnect")) {
			isReconnect = joinData.getInt("isReconnect");
		}
		// 茶楼 处理
		if (CommonConstant.ROOM_TYPE_TEA == room.getRoomType() && room.getGameIndex() == 0) {
			if (CommonConstant.GLOBAL_NO == isReconnect) {
				gameReady(client, data);// 玩家准备
			}
			return;
		}

		// 进入房间通知自己
		createRoom(client, data);

		// 非重连通知其他玩家
		if (CommonConstant.GLOBAL_NO == isReconnect) {
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
			obj.put("score", Dto.add(player.getScore(), player.getSourceScore()));
			obj.put("index", player.getMyIndex());
			obj.put("userOnlineStatus", player.getStatus());
			obj.put("ghName", player.getGhName());
			obj.put("introduction", player.getSignature());
			obj.put("userStatus", room.getUserPacketMap().get(account).getStatus());
			obj.put("isTrustee", room.getUserPacketMap().get(account).getIsTrustee());
			// 通知玩家
			CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), obj.toString(), "playerEnterPush");

		}
	}

	public void loadFinish(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		// 不满足准备条件直接忽略
		if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
			return;
		}
		// 房间号
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		if (!postData.containsKey("type")) {
			return;
		}
		int type = postData.getInt("type");
		if (type == QZMJConstant.GAME_READY_TYPE_RECONNECT) {// 重连准备
			if (room.getRoomType() != CommonConstant.ROOM_TYPE_FK && room.getRoomType() != CommonConstant.ROOM_TYPE_DK
					&& room.getRoomType() != CommonConstant.ROOM_TYPE_CLUB
					&& room.getRoomType() != CommonConstant.ROOM_TYPE_FREE
					&& room.getRoomType() != CommonConstant.ROOM_TYPE_INNING) {
				reconnectGame(client, data);
			} else if (room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_INIT
					|| room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_READY) {
				gameReady(client, data);
			} else {
				reconnectGame(client, data);
			}
		} else if (type == QZMJConstant.GAME_READY_TYPE_READY) {// 准备
			gameReady(client, data);
		} else if (type == QZMJConstant.GAME_READY_TYPE_RECONNECT_NOT_READY) {// 重连不准备
			if (room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_INIT
					|| room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_READY) {
			} else {
				reconnectGame(client, data);
			}
		}
	}

	public void gameReady(SocketIOClient client, Object data) {
		String eventName = "gameReadyPush";
		JSONObject object = JSONObject.fromObject(data);
		if (!JsonUtil.isNullVal(object,
				new String[] { CommonConstant.DATA_KEY_ROOM_NO, CommonConstant.DATA_KEY_ACCOUNT })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[room_no,account]", eventName);
			return;
		}
		String account = object.getString(CommonConstant.DATA_KEY_ACCOUNT);// 玩家id
		final String roomNo = object.getString(CommonConstant.DATA_KEY_ROOM_NO);// 房间号
		int init = 0;// 是否初始化
		if (object.containsKey("init")) {
			init = object.getInt("init");
		}

		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		int gameStatus = room.getGameStatus();
		if (QZMJConstant.QZ_GAME_STATUS_INIT != gameStatus && QZMJConstant.QZ_GAME_STATUS_READY != gameStatus
				&& QZMJConstant.QZ_GAME_STATUS_SUMMARY != gameStatus) {
			CommonConstant.sendMsgEventNo(client, "请稍后再试", null, eventName);
			return;
		}

		QZMJUserPacket up = room.getUserPacketMap().get(account);
		Playerinfo playerinfo = room.getPlayerMap().get(account);
		if (up == null || playerinfo == null)
			return;
		if (QZMJConstant.QZ_USER_STATUS_READY == up.getStatus()) {// 玩家已经处于准备
			return;
		}
		int roomType = room.getRoomType();// 房间类型
		if (CommonConstant.ROOM_TYPE_FK == roomType && playerinfo.getPlayTimes() == 0
				|| CommonConstant.ROOM_TYPE_YB == roomType) {// 房卡 元宝
			String updateType = room.getCurrencyType();
			// 获取用户信息
			JSONObject user = DBUtil.getObjectBySQL(
					"SELECT z.`roomcard`,z.`yuanbao` FROM  za_users z  WHERE z.`account`= ?", new Object[] { account });
			if (null == user || !user.containsKey(updateType) || user.getDouble(updateType) < room.getEnterScore()) {
				String msg = "钻石不足";
				if ("yuanbao".equals(updateType))
					msg = "元宝不足";
				object = new JSONObject();
				object.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
				object.put(CommonConstant.RESULT_KEY_MSG, msg);
				object.put("type", CommonEventConstant.TIP_MSG_PUSH_TYPE_EXIT);
				CommonConstant.sendMsgEvent(client, object, CommonEventConstant.TIP_MSG_PUSH);
				// 移除房间用户数据
				exitRoomByPlayer(roomNo, playerinfo.getId(), account);
				return;
			}
		}
		// 亲友圈
		else if (room.getRoomType() == CommonConstant.ROOM_TYPE_FREE
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
			double leaveScore = room.getLeaveScore();// 离场验证
			if (leaveScore > Dto.add(playerinfo.getScore(), playerinfo.getSourceScore())) {
				if (room.getRoomType() == CommonConstant.ROOM_TYPE_INNING && room.getGameIndex() > 0) {// 局数场 ，有玩过的
					// 强制结算 start
					room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE);
					room.setGameStatus(QZMJConstant.QZ_USER_STATUS_SUMMARY);
					room.setGameIndex(room.getGameCount());

					// 强制结算 end
					for (String userAccount : room.getPlayerMap().keySet()) {
						JSONObject result = new JSONObject();
						result.put("type", CommonConstant.SHOW_MSG_TYPE_SMALL);
						if (account.equals(userAccount)) {
							result.put(CommonConstant.RESULT_KEY_MSG, "能量不足");
						} else {
							result.put(CommonConstant.RESULT_KEY_MSG,
									"【" + room.getPlayerMap().get(account).getName() + "】能量不足");
						}
						SocketIOClient c = GameMain.server.getClient(room.getPlayerMap().get(userAccount).getUuid());
						CommonConstant.sendMsgEventToSingle(c, result.toString(), "tipMsgPush");
					}

					compulsorySettlement(roomNo);// 强制结算
					return;
				}
				object.put("notSend", CommonConstant.GLOBAL_YES);
				object.put("notSendToMe", CommonConstant.GLOBAL_YES);
				exitRoom(client, object);
				JSONObject result = new JSONObject();
				result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
				result.put(CommonConstant.RESULT_KEY_MSG, "能量不足");
				CommonConstant.sendMsgEventToSingle(client, result.toString(), "tipMsgPush");
				return;
			}
			// 自由场 房费AA
			if (room.getRoomType() == CommonConstant.ROOM_TYPE_FREE
					&& CommonConstant.ROOM_PAY_TYPE_AA.equals(String.valueOf(room.getPayType()))) {
				JSONObject user = DBUtil.getObjectBySQL("SELECT z.`roomcard` FROM  za_users z  WHERE z.`account`= ?",
						new Object[] { account });
				if (user == null || user.getDouble("roomcard") < room.getSinglePayNum()) {
					object.put("notSend", CommonConstant.GLOBAL_YES);
					object.put("notSendToMe", CommonConstant.GLOBAL_YES);
					exitRoom(client, object);
					JSONObject result = new JSONObject();
					result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
					result.put(CommonConstant.RESULT_KEY_MSG, "钻石不足");
					CommonConstant.sendMsgEventToSingle(client, result.toString(), "tipMsgPush");
					return;
				}
			}
		}
		// 设置玩家准备状态
		room.getUserPacketMap().get(account).setStatus(QZMJConstant.QZ_USER_STATUS_READY);
		// 设置房间准备状态
		if (room.getGameStatus() != QZMJConstant.QZ_GAME_STATUS_READY) {
			room.setGameStatus(QZMJConstant.QZ_GAME_STATUS_READY);
		}

		int minStartPlayer = room.getPlayerCount();// 麻将只支持满人开

		// 房间内所有玩家都已经完成准备且人数已满通知开始游戏,否则通知玩家准备
		if (room.getPlayerMap().size() <= room.getPlayerCount() && room.isAllReady()
				&& room.getUserPacketMap().size() >= minStartPlayer) {
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
							room.getUserPacketMap().get(player).setStatus(QZMJConstant.QZ_USER_STATUS_INIT);
						}
						for (String player : accountList) {
							object.put(CommonConstant.DATA_KEY_ACCOUNT, player);
							object.put("notSend", CommonConstant.GLOBAL_YES);
							exitRoom(GameMain.server.getClient(room.getPlayerMap().get(player).getUuid()), object);
						}
						JSONObject result = new JSONObject();
						result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
						result.put(CommonConstant.RESULT_KEY_MSG, "基金不足请充值!");
						CommonConstant.sendMsgEventToAll(allUUIDList, result.toString(), "tipMsgPush");
						return;
					}
				}
			}
//茶楼房卡扣除
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
									JSONObject postData = new JSONObject();
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
						object = new JSONObject();
						object.element(CommonConstant.DATA_KEY_ACCOUNT, uuid).element(CommonConstant.DATA_KEY_ROOM_NO,
								roomNo);
						createRoom(playerClient, object);
					}

				}
			}
			startGame(roomNo);// 全部准备开始游戏
			return;
		}
		JSONObject result = new JSONObject();
		JSONArray array = new JSONArray();
		for (String uuid : room.getUserPacketMap().keySet()) {
			playerinfo = room.getPlayerMap().get(uuid);
			if (playerinfo == null)
				continue;

			JSONObject obj = new JSONObject();
			obj.put("index", playerinfo.getMyIndex());
			obj.put("score", Dto.add(playerinfo.getScore(), playerinfo.getSourceScore()));
			if (QZMJConstant.QZ_USER_STATUS_READY == room.getUserPacketMap().get(uuid).getStatus()) {
				obj.put("ready", CommonConstant.GLOBAL_YES);
			} else {
				obj.put("ready", CommonConstant.GLOBAL_NO);
			}
			array.add(obj);

		}
		result.put("data", array);
		result.put("init", init);
		CommonConstant.sendMsgEventAll(room.getAllUUIDList(), result, eventName);
	}

	private void finalSummaryRoom(String roomNo, boolean isCloseRoom) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;

		int roomType = room.getRoomType();
		if (isCloseRoom) {
			removeRoom(roomNo);// 移除房间
			return;
		}
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

	private void clearRoomInfo(String roomNo) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (room == null)
			return;
		room.initGame();
		room.setGameIndex(0);
		room.setGameNewIndex(0);
		room.getSummaryData().clear();
		room.setGameStatus(QZMJConstant.QZ_GAME_STATUS_INIT);
		room.setJieSanTime(0);
		room.setTimeLeft(0);
		room.getFinalSummaryData().clear();
		room.setUserPacketMap(new ConcurrentHashMap<>());
		room.setPlayerMap(new ConcurrentHashMap<>());
		room.setVisitPlayerMap(new ConcurrentHashMap<>());
		room.setOpen(true);
		room.setBankerTimes(1);
		for (int i = 0; i < room.getUserIdList().size(); i++) {
			if (room.getUserIdList().get(i) > 0L) {
				room.getUserIdList().set(i, 0L);
				room.addIndexList(i);
			}
		}

		// 处理清空房间，重新生成
		baseEventDeal.reloadClearRoom(roomNo);
	}

	private void removeRoom(String roomNo) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		// 房间解散
		redisInfoService.delSummary(roomNo, "_QZMJ");
		JSONObject roomInfo = new JSONObject();
		roomInfo.put("room_no", room.getRoomNo());
		roomInfo.put("status", CommonConstant.CLOSE_ROOM_TYPE_OVER);
		roomInfo.put("game_index", 0);
		producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
		RoomManage.gameRoomMap.remove(roomNo);
	}

	public void gameChuPai(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		// 不满足准备条件直接忽略
		if (!CommonConstant.checkEvent(postData, QZMJConstant.QZ_GAME_STATUS_ING, client)) {
			return;
		}
		if (!postData.containsKey("pai"))
			return;
		// 房间号
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;

		room.setTooNextAskAccount(null);// 清空 过记录
		// 玩家账号
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		int paiSize = 3;
		int paiLeft = 2;
		if (room.getUserPacketMap().get(account).getMyPai().size() % paiSize != paiLeft) {
			return;
		}
		// 出牌
		int pai = postData.getInt("pai");
		if (room.getGid() == CommonConstant.GAME_ID_ZZC) {// 南安麻将
			// 牌局中剩余牌数（包含其他玩家手牌）
			List<Integer> leftList = new ArrayList<Integer>(Dto.arrayToList(room.getPai(), room.getIndex()));
			for (String player : room.getPlayerMap().keySet()) {
				if (room.getUserPacketMap().containsKey(player) && room.getUserPacketMap().get(player) != null) {
					if (!account.equals(player)) {
						leftList.addAll(room.getUserPacketMap().get(player).getMyPai());
					}
				}
			}
			JSONArray tingTip = MaJiangCore.tingPaiTip(room.getUserPacketMap().get(account).getMyPai(), room.getJin(),
					leftList);
			List<Integer> compensateList = MaJiangCore.getCompensateList(
					room.getUserPacketMap().get(account).getMyPai(), getOutList(roomNo), room.getJin(), tingTip);
			// 获取要出的牌
			if (compensateList.contains(pai)) {
				room.setCompensateAccount(account);
			} else {
				// 清空违规玩家
				room.setCompensateAccount(null);
				if (room.getCompensateMap().containsValue(account)) {
					// 该玩家有违规出牌记录需要清空
					for (String uuid : room.getCompensateMap().keySet()) {
						// 需要赔给其他人
						if (account.equals(room.getCompensateMap().get(uuid))) {
							boolean isCompensate = false;
							// 其他人游金不清空
							if (room.getUserPacketMap().get(uuid).getYouJinIng() > 0) {
								isCompensate = true;
							}
							// 导致他人游金不清空
							if (!isCompensate) {
								room.getCompensateMap().remove(account);
							}
						}
					}
				}
			}
		}

		// type： 0:无操作 ,1：抓 2：暗杠(抓杠)询问事件 3：自摸(抓糊)询问事件 4：吃 询问事件 5：碰 询问事件 6：杠 出牌 7：糊 询问事件
		// 8：结算事件
		Object[] outResult = chuPai(roomNo, pai, account);
		// 出牌返回
		detailDataByChuPai(outResult, roomNo);
		room.setChuPaiJieGuo(outResult);
		if (room.getChuPaiJieGuo() != null && room.getChuPaiJieGuo()[0] != null) {
			String thisAskAccount = String.valueOf(room.getChuPaiJieGuo()[0]);
			if (!Dto.stringIsNULL(thisAskAccount) && room.getPlayerMap().containsKey(thisAskAccount)
					&& room.getPlayerMap().get(thisAskAccount) != null) {
				room.getChuPaiJieGuo()[0] = room.getPlayerMap().get(thisAskAccount).getAccount();
			}
		}
	}

	public void gameEvent(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		// 不满足准备条件直接忽略
		if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
			return;
		}
		if (!postData.containsKey("type"))
			return;
		// 房间号
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		// 玩家账号
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);

		int type = -postData.getInt("type");
		if (room.getGid() == CommonConstant.GAME_ID_ZZC) {
			if (type == -4 || type == -5 || type == -6 || type == -7) {
				if (!Dto.stringIsNULL(room.getCompensateAccount())) {
					room.getCompensateMap().put(account, room.getCompensateAccount());
				}
			}
			if (type == -1) {
				room.getCompensateMap().remove(account);
			}
		}
		if (-1 != type)
			room.setTooNextAskAccount(null);// 清空 过记录
		switch (type) {
		case -1:
			// 过
			Object[] jieguo = guo(roomNo, account);
			detailDataByGuo(jieguo, roomNo);
			break;
		case -2:
			// 暗杠
			int[] gang = gang(roomNo, account, -type);

			if (gang[0] == 1) {
				// 抓杠
				detailDataByChiGangPengHu(roomNo, -9, account, gang);
			} else if (gang[0] == 2) {
				// 明杠
				detailDataByChiGangPengHu(roomNo, -6, account, gang);
			} else if (gang[0] == 3) {
				// 暗杠
				detailDataByChiGangPengHu(roomNo, -2, account, gang);
			}

			break;
		case -3:
			// 自摸询问
			JSONObject d = new JSONObject();
			d.put("type", new int[] { 3 });
			CommonConstant.sendMsgEventToSingle(client, String.valueOf(d), "gameActionPush");
			break;
		case -4:
			// 吃
			if (postData.containsKey("chivalue")) {
				JSONArray array = postData.getJSONArray("chivalue");
				int[] chiValue = new int[array.size()];
				for (int i = 0; i < array.size(); i++) {
					chiValue[i] = array.getInt(i);
				}
				if (chi(roomNo, chiValue, account)) {
					detailDataByChiGangPengHu(roomNo, -4, account, chiValue);
				}
			}
			break;
		case -5:
			// 碰
			if (peng(roomNo, account)) {
				detailDataByChiGangPengHu(roomNo, -5, account, null);
			}
			break;
		case -6:
			// 明杠
			int[] mgang = gang(roomNo, account, -type);

			if (mgang[0] == 1) {
				// 抓杠
				detailDataByChiGangPengHu(roomNo, -9, account, mgang);
			} else if (mgang[0] == 2) {
				// 明杠
				detailDataByChiGangPengHu(roomNo, -6, account, mgang);
			} else if (mgang[0] == 3) {
				// 暗杠
				detailDataByChiGangPengHu(roomNo, -2, account, mgang);
			}
			break;
		case -7:
			// 胡
			int hu = hu(roomNo, account, 7);
			if (hu > 0) {
				detailDataByChiGangPengHu(roomNo, -7, account, null);
				// 结算事件，返回结算处理结果
				sendSummaryData(roomNo, false, false);
			}
			break;
		case -8:
			// 结算事件，返回结算处理结果
			// sendSummaryData(roomNo);
			break;
		case -9:
			// 抓明杠
			int[] bgang = gang(roomNo, account, -type);

			if (bgang[0] == 1) {
				// 抓杠
				detailDataByChiGangPengHu(roomNo, -9, account, bgang);
			} else if (bgang[0] == 2) {
				// 明杠
				detailDataByChiGangPengHu(roomNo, -6, account, bgang);
			} else if (bgang[0] == 3) {
				// 暗杠
				detailDataByChiGangPengHu(roomNo, -2, account, bgang);
			} else if (bgang[0] == -1) {
				// 有玩家可以抢杠胡
				int myIndex = bgang[1];
				for (String uuid : room.getPlayerMap().keySet()) {
					if (room.getPlayerMap().containsKey(uuid) && room.getPlayerMap().get(uuid) != null) {
						// 询问玩家是否抢杠
						if (room.getPlayerMap().get(uuid).getMyIndex() == myIndex) {
							JSONObject qgobj = new JSONObject();
							qgobj.put("type", new int[] { 3 });
							qgobj.put("huType", QZMJConstant.HU_TYPE_QGH);
							qgobj.put(QZMJConstant.value, new int[] { room.getLastMoPai() });
							CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(),
									String.valueOf(qgobj), "gameChupaiPush");
							break;
						}
					}
				}
			}
			break;
		case -10:
			// 补花
			buHua(roomNo, account);
			break;
		case -11:
			// 自摸
			if (room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_ING) {
				int zimo = hu(roomNo, account, 3);
				if (zimo > 0) {
					detailDataByChiGangPengHu(roomNo, -3, account, null);
					// 结算事件，返回结算处理结果
					sendSummaryData(roomNo, false, false);
				}
				break;
			}
		default:
			break;
		}
	}

	public void gangChupaiEvent(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		// 不满足准备条件直接忽略
		if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
			return;
		}
		// 房间号
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		room.setTooNextAskAccount(null);// 清空 过记录
		// 玩家账号
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		// 玩家抓牌
		JSONArray mjieguo = moPai(roomNo, account);
		detailDataByZhuaPai(mjieguo, room, null);
	}

	public void gameTrustee(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		// 不满足准备条件直接忽略
		if (!CommonConstant.checkEvent(postData, QZMJConstant.QZ_GAME_STATUS_ING, client)) {
			return;
		}
		// 房间号
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		// 玩家账号
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		if (!postData.containsKey("type")) {
			return;
		}
		// 改变玩家托管状态
		int trustee = postData.getInt("type");
		if (trustee == CommonConstant.GLOBAL_NO
				&& room.getUserPacketMap().get(account).getCancelTrusteeTime() >= QZMJConstant.MAX_CANCEL_TIME) {
			JSONObject obj = new JSONObject();
			obj.put("type", CommonConstant.SHOW_MSG_TYPE_NORMAL);
			obj.put("msg", "已达单局取消托管上限");
			CommonConstant.sendMsgEventToSingle(client, String.valueOf(obj), "tipMsgPush");
			return;
		}
		room.getUserPacketMap().get(account).setIsTrustee(trustee);
		// 取消托管增加取消次数
		if (trustee == CommonConstant.GLOBAL_NO) {
			room.getUserPacketMap().get(account)
					.setCancelTrusteeTime(room.getUserPacketMap().get(account).getCancelTrusteeTime() + 1);
		}
		JSONObject result = new JSONObject();
		result.put("index", room.getPlayerMap().get(account).getMyIndex());
		result.put("type", trustee);
		CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "gameTrusteePush");
	}

	public void exitRoom(SocketIOClient client, Object data) {
		String eventName = "exitRoomPush";
		JSONObject postData = JSONObject.fromObject(data);

		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room) {
			JSONObject result = new JSONObject();
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			result.put("type", 2);
			CommonConstant.sendMsgEvent(client, result, eventName);
			return;
		}

		QZMJUserPacket up = room.getUserPacketMap().get(account);
		Playerinfo player = room.getPlayerMap().get(account);
		if (null == up || null == player) {
			JSONObject result = new JSONObject();
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			result.put("type", 2);
			CommonConstant.sendMsgEvent(client, result, eventName);
			return;
		}
		int roomType = room.getRoomType();
		if (!Dto.stringIsNULL(account) && room.getUserPacketMap().containsKey(account)
				&& room.getUserPacketMap().get(account) != null) {
			boolean canExit = false;
			// 金币场、元宝场
			if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB || room.getRoomType() == CommonConstant.ROOM_TYPE_YB
					|| room.getRoomType() == CommonConstant.ROOM_TYPE_COMPETITIVE
					|| room.getRoomType() == CommonConstant.ROOM_TYPE_FREE) {
				// 未参与游戏可以自由退出
				if (room.getUserPacketMap().get(account).getStatus() == QZMJConstant.QZ_USER_STATUS_INIT) {
					canExit = true;
				} else if (room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_INIT
						|| room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_READY
						|| room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_SUMMARY) {// 初始及准备阶段可以退出
					canExit = true;
				}
			} else if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK
					|| room.getRoomType() == CommonConstant.ROOM_TYPE_DK
					|| room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB
					|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
				// 房卡场没玩过可以退出
				if (room.getPlayerMap().get(account).getPlayTimes() == 0) {
					if (CommonConstant.ROOM_PAY_TYPE_AA.equals(room.getPayType()) || !room.getOwner().equals(account)
							|| room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
						canExit = true;
					} else if (room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
						canExit = true;
					}
				}
				if (room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_FINAL_SUMMARY) {
					canExit = true;
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
				result.put("index", player.getMyIndex());

				int minStartPlayer = room.getPlayerCount();// 麻将只支持满人开

				if (room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_READY
						&& room.getNowReadyCount() < minStartPlayer) {
					// 重置房间倒计时
					room.setTimeLeft(QZMJConstant.QZMJ_TIMER_INIT);
				}
				if (room.getTimeLeft() > QZMJConstant.QZMJ_TIMER_INIT) {
					result.put("showTimer", CommonConstant.GLOBAL_YES);
				} else {
					result.put("showTimer", CommonConstant.GLOBAL_NO);
				}
				result.put("timer", room.getTimeLeft());
				if (!postData.containsKey("notSend")) {
					CommonConstant.sendMsgEventToAll(allUUIDList, result.toString(), eventName);
				}
				if (postData.containsKey("notSendToMe")) {
					CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), eventName);
				}

				// 房间内所有玩家都已经完成准备且人数大于最低开始人数通知开始游戏
				if (room.getPlayerMap().size() <= room.getPlayerCount() && room.isAllReady()
						&& room.getPlayerMap().size() >= minStartPlayer) {
					startGame(room.getRoomNo());
				}
				// 所有人都退出清除房间数据
				if (room.getPlayerMap().size() == 0 && CommonConstant.ROOM_TYPE_FK == roomType) {
					removeRoom(roomNo);// 移除房间
					return;
				}
			} else {
				// 组织数据，通知玩家
				JSONObject result = new JSONObject();
				result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
				result.put(CommonConstant.RESULT_KEY_MSG, "游戏中无法退出");
				result.put("showTimer", CommonConstant.GLOBAL_NO);
				if (room.getTimeLeft() == 0) {
					result.put("showTimer", CommonConstant.GLOBAL_NO);
				}
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
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		int roomType = room.getRoomType();
		if (CommonConstant.DISSMISS_ROLE_MSG == room.getIsPlayerDismiss() && room.allNoClose()) {// 第一个提交解散
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
		if (postData.containsKey("type")) {
			JSONObject result = new JSONObject();
			int type = postData.getInt("type");
			// 有人发起解散设置解散时间
			if (type == CommonConstant.CLOSE_ROOM_AGREE && room.getJieSanTime() == 0) {
				int jieSanTime = redisInfoService.getAppTimeSetting(room.getPlatform(), room.getGid(),
						"QZMJConstant.QZMJ_TIMER_CLOSE_ROOM");
				if (jieSanTime < 0) {
					jieSanTime = QZMJConstant.QZMJ_TIMER_CLOSE_ROOM;
				}
				final int overTime = jieSanTime;

				room.setJieSanTime(overTime);
				ThreadPoolHelper.executorService.submit(new Runnable() {
					@Override
					public void run() {
						gameTimerQZMJ.closeRoomOverTime(roomNo, overTime);
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
				CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush");
				return;
			}
			if (type == CommonConstant.CLOSE_ROOM_AGREE) {
				// 全部同意解散
				if (room.isAgreeClose()) {
					room.setNeedFinalSummary(true);
					room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE);
					if (room.getGameIndex() <= 1 && Dto.isObjNull(room.getSummaryData())) {// 未玩一局
						// 所有玩家
						List<UUID> uuidList = room.getAllUUIDList();
						// 通知玩家
						result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
						result.put(CommonConstant.RESULT_KEY_MSG, "成功解散房间");
						CommonConstant.sendMsgEventToAll(uuidList, result.toString(), "tipMsgPush");
						finalSummaryRoom(room.getRoomNo(), false);// 总结算房间处理
						return;
					}
					compulsorySettlement(room.getRoomNo());// 局数超过 强制结算
					return;
				} else {
					// 刷新数据
					room.getPlayerMap().get(account).setIsCloseRoom(CommonConstant.CLOSE_ROOM_AGREE);
					result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
					result.put("data", room.getCloseRoomData());
					CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush");
				}
			}
		}
	}

	public void compulsorySettlement(String roomNo) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (room == null)
			return;
		room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE);
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
			room.setOpen(false);
		}
		if (room.getGameIndex() <= 1 && Dto.isObjNull(room.getSummaryData())) {// 未玩一局
		} else {
			room.setGameStatus(QZMJConstant.QZ_GAME_STATUS_SUMMARY);
			room.setGameIndex(room.getGameCount());
			// 结算事件，返回结算处理结果
			sendSummaryData(roomNo, false, true);
		}
	}

	public JSONObject obtainIngReconnectData(String roomNo, String account) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return null;
		Playerinfo player = room.getPlayerMap().get(account);
		QZMJUserPacket qzmjUserPacket = room.getUserPacketMap().get(account);
		// 返回给玩家当前牌局信息（基础信息）
		JSONObject result = new JSONObject();
		// 返回庄家的位置
		result.put(QZMJConstant.zhuang, room.getPlayerIndex(room.getBanker()));
		result.put("lianzhuang", room.getBankerTimes());
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB || room.getRoomType() == CommonConstant.ROOM_TYPE_FREE) {
			result.put("lianzhuang", 0);
		}
		// 返回玩家的状态
		result.put(QZMJConstant.status, room.getUserPacketMap().get(account).getStatus());
		// 返回游戏的底分
		result.put(QZMJConstant.soure, room.getScore());
		// 返回金
		result.put(QZMJConstant.jin, room.getJin());
		result.put(QZMJConstant.myIndex, player.getMyIndex());
		result.put(QZMJConstant.myPai, room.getUserPacketMap().get(account).getMyPai().toArray());
		// 返回骰子点数
		result.put("dice", room.getDice());
		result.put("jiesan", 0);
		int paishu = 0;
		int shengyu = 0;
		if (room.getPai() != null) {
			shengyu = room.getPai().length - room.getIndex();
			paishu = room.getPai().length;
		}
		// 返回剩余牌数
		result.put(QZMJConstant.zpaishu, shengyu);
		// 返回总牌数
		result.put(QZMJConstant.pai, paishu);

		// 当前游戏状态判断
		String thisUUID = room.getThisAccount();
		// 返回的type
		int[] backType = new int[] { 0 };
		int[] value = new int[] {};
		int focus = -1;
		int lastFoucs = -1;
		int lastPoint = -1;
		// 获取过记录
		JSONObject tooObject = room.getTooNextAskAccount();
		if (tooObject != null && tooObject.containsKey(QZMJConstant.foucs)) {
			if (tooObject.getInt(QZMJConstant.foucs) == room.getPlayerIndex(account)) {
				JSONArray backTypeList = tooObject.getJSONArray(QZMJConstant.type);
				if (backTypeList != null && backTypeList.size() > 0) {
					backType = new int[backTypeList.size()];
					for (int i = 0; i < backTypeList.size(); i++) {
						backType[i] = backTypeList.getInt(i);
					}
				}
				focus = tooObject.getInt(QZMJConstant.foucs);
				lastFoucs = tooObject.getInt(QZMJConstant.lastFoucs);
				result.put("actData", tooObject);
			}

		} else {
			// 获取最后一次操作记录 start
			KaiJuModel kaijujl = room.getLastKaiJuValue(-1);

			int jltype = -1;
			if (kaijujl != null) {
				jltype = kaijujl.getType();

				if (room.getLastZhuoPaiValue() != null) {

					lastPoint = room.getLastZhuoPaiValue().getIndex();
				}
				focus = room.getPlayerIndex(thisUUID);
				lastFoucs = room.getLastFocus();

				// 三种情况：1.自己摸牌 2.别人出牌 3.自己操作完成后（吃碰杠）

				// 最后一次操作人
				if (kaijujl.getIndex() == room.getPlayerIndex(account)) {

					if (jltype == 2 || jltype == 4 || jltype == 5 || jltype == 6 || jltype == 9) {

						backType = new int[] { -jltype };
						focus = player.getMyIndex();
						lastFoucs = room.getLastFocus();

					} else if (room.getStartStatus() == -1 && (jltype == 1 || jltype == 11)) {
						// 开局流程走完，且抓完牌（补花）->出牌

						int pai = room.getLastMoPai();
						// 执行摸牌处理方法，判断是否触发自摸，杠事件
						JSONObject actData = moPaiDeal(roomNo, account, pai);
						if (actData != null) {
							JSONArray array = actData.getJSONArray("type");
							backType = new int[array.size()];
							for (int i = 0; i < array.size(); i++) {
								backType[i] = array.getInt(i);
							}
							result.put("actData", actData);
							if (actData.containsKey("tingTip")) {

								result.put("tingTip", actData.get("tingTip"));
								// 去掉重复数据
								actData.remove("tingTip");
							}
							if (actData.containsKey("compensateList")) {
								result.put("compensateList", actData.get("compensateList"));
								// 去掉重复数据
								actData.remove("compensateList");
							}
						}
						if (pai > 0) {

							value = new int[] { pai };
							List<Integer> myPai = new ArrayList<Integer>(qzmjUserPacket.getMyPai());
							myPai.remove((Integer) room.getLastMoPai());
							result.put(QZMJConstant.myPai, myPai.toArray());
							focus = player.getMyIndex();
							lastFoucs = room.getLastFocus();
						}
					}

				} else if (jltype == 0) {
					// 出牌
					if (player != null && room.getChuPaiJieGuo() != null) {
						if (player.getAccount().equals(room.getChuPaiJieGuo()[0])) {
							JSONObject actData = chuPaiDeal(room.getChuPaiJieGuo(), roomNo, account);
							if (actData != null) {
								focus = actData.getInt("focus");
								JSONArray array = actData.getJSONArray("type");
								backType = new int[array.size()];
								for (int i = 0; i < array.size(); i++) {
									backType[i] = array.getInt(i);
								}
							}
							result.put("actData", actData);
						}
					}

				}
			}
			// 获取最后一次操作记录 end
		}
		// 事件类型,当前正在进行的操作(1表示出牌 2~9表示事件询问请求,-2~-9表示事件返回结果后,等待出牌)
		result.put(QZMJConstant.type, backType);
		Playerinfo p = room.getPlayerMap().get(account);
		// type是1或者负数的时候出牌
		if (focus == p.getMyIndex() && (backType == null || (backType != null && backType[0] < 0))) {

			// 牌局中剩余牌数（包含其他玩家手牌）
			List<Integer> shengyuList = new ArrayList<Integer>(Dto.arrayToList(room.getPai(), room.getIndex()));
			for (String cliTag : room.getPlayerMap().keySet()) {
				if (room.getUserPacketMap().containsKey(cliTag) && room.getUserPacketMap().get(cliTag) != null) {
					if (!account.equals(cliTag)) {
						shengyuList.addAll(room.getUserPacketMap().get(cliTag).getMyPai());
					}
				}
			}

			// 出牌提示
			JSONArray tingTip = MaJiangCore.tingPaiTip(room.getUserPacketMap().get(account).getMyPai(), room.getJin(),
					shengyuList);
			result.put("tingTip", tingTip);
			result.put("compensateList", MaJiangCore.getCompensateList(room.getUserPacketMap().get(account).getMyPai(),
					getOutList(roomNo), room.getJin(), tingTip));
		}
		// 当前正在操作的人
		result.put(QZMJConstant.foucs, focus);
		result.put(QZMJConstant.foucsIndex, room.getFocusIndex());
		// 当type是2~9的询问事件时才需要
		result.put(QZMJConstant.lastFoucs, lastFoucs);
		// type为1或者-2 -6 -9时表示新摸的牌,如果是询问事件(2~9)就表示询问事件的牌
		result.put(QZMJConstant.value, value);
		// 上一个出牌的人
		result.put(QZMJConstant.nowPoint, room.getNowPoint());
		// 上上个出牌的人
		result.put(QZMJConstant.lastPoint, lastPoint);

		// 各个玩家信息
		JSONArray userInfos = new JSONArray();
		for (String cliTag : room.getPlayerMap().keySet()) {
			QZMJUserPacket up = room.getUserPacketMap().get(cliTag);
			Playerinfo playerinfo = room.getPlayerMap().get(cliTag);
			if (null == up || null == playerinfo)
				continue;

			JSONObject user = new JSONObject();
			user.put("chupai", room.getChuPaiJiLu(room.getPlayerIndex(cliTag)).toString());
			user.put("hua", up.getHuaList().size());
			List<Integer> huaValue = up.getHuaList();
			user.put("huaValue", JSONArray.fromObject(huaValue));
			List<DontMovePai> dmPai = up.getHistoryPai();
			user.put("mingpai", JSONArray.fromObject(dmPai));
			user.put("paicount", up.getMyPai().size());
			user.put(QZMJConstant.myIndex, room.getPlayerMap().get(cliTag).getMyIndex());
			user.put("index", playerinfo.getMyIndex());
			user.put("name", playerinfo.getName());
			user.put("headimg", playerinfo.getRealHeadimg());
			user.put("sex", playerinfo.getSex());
			user.put("ip", playerinfo.getIp());
			user.put("location", playerinfo.getLocation());
			user.put("ghName", playerinfo.getGhName());
			user.put("score", Dto.add(playerinfo.getScore(), playerinfo.getSourceScore()));
			user.put("status", playerinfo.getStatus());
			user.put("isTrustee", up.getIsTrustee());
			int yjtype = up.getYouJinIng();
			// 光游时直接返回游金类型
			if (cliTag.equals(account) || room.isGuangYou) {
				user.put("youjinType", yjtype);
			} else {
				// 暗游时，只有双游以上才通知其他人
				if (yjtype > 1) {
					// 通知其他玩家正在双游、三游
					user.put("youjinType", yjtype);
				} else {
					user.put("youjinType", 0);
				}
			}

			userInfos.add(user);

		}
		result.put("userInfos", userInfos);
		// 返回开局状态
		result.put("kjtype", room.getStartStatus());
		result.put("room_index", room.getGameNewIndex());
		return result;
	}

	public JSONObject obtainSummaryObject(String roomNo, boolean isAllSummary, boolean forced) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return null;
		if (!isAllSummary && !Dto.isObjNull(room.getSummaryData())) {
			return room.getSummaryData();
		}
		String winner = room.getWinner();
		JSONArray array = new JSONArray();
		// 结算时赢家排在第一个
		if (null != winner && room.getPlayerMap().containsKey(winner)) {

			JSONObject data = new JSONObject();
			Playerinfo player = room.getPlayerMap().get(winner);
			QZMJUserPacket up = room.getUserPacketMap().get(winner);
			// 确定赢家
			data.put("isWinner", 1);
			data.put("huType", room.getHuType());
			data.put("jin", room.getJin());
			data.put("huTimes", QZMJConstant.getHuTimes(room.getHuType(), room.getYouJinScore()));

			// 获取玩家吃杠碰的牌
			List<Integer> paiList = up.getHistoryList();
			// 手牌排序
			int huPai = up.getMyPai().remove(up.getMyPai().size() - 1);
			Collections.sort(up.getMyPai());
			up.getMyPai().add(huPai);
			paiList.addAll(up.getMyPai());
			data.put("myPai", paiList.toArray());
			data.put("fan", up.getFan());
			data.put("fanDetail", up.getFanDetail(up.getMyPai(), room, winner));
			data.put("score", up.getScore());
			data.put("player", player.getName());
			data.put("headimg", player.getRealHeadimg());
			data.put("hua", up.getHuaList().size());
			data.put("myIndex", player.getMyIndex());
			data.put("huaValue", JSONArray.fromObject(up.getHuaList()));
			data.put("gangValue", JSONArray.fromObject(up.getGangValue()));
			data.put("isCompensate", CommonConstant.GLOBAL_NO);
			// 判断玩家是否是庄家
			if (winner.equals(room.getBanker())) {
				data.put("zhuang", 1);
				data.put("bankerTimes", room.getBankerTimes());// 连庄次数
			} else {
				data.put("zhuang", 0);
			}
			data.put("difen", room.getUserScore(winner));

			array.add(data);
		}

		// 其他玩家结算
		for (String uuid : room.getPlayerMap().keySet()) {
			if (room.getPlayerMap().containsKey(uuid) && room.getPlayerMap().get(uuid) != null) {
				Playerinfo player = room.getPlayerMap().get(uuid);
				QZMJUserPacket userPacketQZMJ = room.getUserPacketMap().get(uuid);

				if (!uuid.equals(winner)) {
					JSONObject data = new JSONObject();
					data.put("isWinner", 0);
					data.put("huType", room.getHuType());
					data.put("jin", room.getJin());
					data.put("huTimes", QZMJConstant.getHuTimes(room.getHuType(), room.getYouJinScore()));

					// 获取玩家吃杠碰的牌
					List<Integer> paiList = userPacketQZMJ.getHistoryList();
					// 手牌排序
					Collections.sort(userPacketQZMJ.getMyPai());
					paiList.addAll(userPacketQZMJ.getMyPai());
					data.put("myPai", paiList.toArray());
					data.put("fan", userPacketQZMJ.getFan());
					data.put("fanDetail", userPacketQZMJ.getFanDetail(userPacketQZMJ.getMyPai(), room, uuid));
					data.put("score", userPacketQZMJ.getScore());
					data.put("player", player.getName());
					data.put("headimg", player.getRealHeadimg());
					data.put("hua", userPacketQZMJ.getHuaList().size());
					data.put("myIndex", player.getMyIndex());
					data.put("huaValue", JSONArray.fromObject(userPacketQZMJ.getHuaList()));
					data.put("gangValue", JSONArray.fromObject(userPacketQZMJ.getGangValue()));
					data.put("isCompensate", CommonConstant.GLOBAL_NO);
					if (uuid.equals(room.getCompensateMap().get(winner))) {
						data.put("isCompensate", CommonConstant.GLOBAL_YES);
					}
					// 判断玩家是否是庄家
					if (uuid.equals(room.getBanker())) {
						data.put("zhuang", 1);
						data.put("bankerTimes", room.getBankerTimes());// 连庄次数
					} else {
						data.put("zhuang", 0);
					}
					data.put("difen", room.getUserScore(uuid));
					array.add(data);
				}
			}
		}
		// 返回数据
		JSONObject backObj = new JSONObject();
		// 结算类型, 0：正常结算 1：最后一局结算 2:解散结算
		backObj.put("type", 0);
		backObj.put("data", array);
		backObj.put("isLiuju", CommonConstant.GLOBAL_NO);
		if (CommonConstant.ROOM_TYPE_FREE != room.getRoomType()) {
			if (room.getGameCount() <= room.getGameIndex()
					|| room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_FINAL_SUMMARY) {
				room.setGameStatus(QZMJConstant.QZ_GAME_STATUS_FINAL_SUMMARY);
				// 保存结算汇总数据
				JSONArray jiesuanArray = obtainFinalSummaryArray(room);
				backObj.put("type", 1);
				backObj.put("data1", jiesuanArray);
				room.setNeedFinalSummary(true);
				if (forced) {
					backObj.put("type", 2);
				}
			}
		}

		backObj.put("fanMultiple", room.getFanMultiple());// 番倍
		backObj.put("score", room.getScore());// 低分
		backObj.put("zhuangScore", room.getZhuangScore());// 庄分
		backObj.put("xianScore", room.getXianScore());// 闲分
		backObj.put("startTime", room.getStartTime());// 游戏开始时间
		backObj.put("roomNo", room.getRoomNo());// 房间号
		room.setSummaryData(backObj);
		return backObj;
	}

	public void reconnectGame(SocketIOClient client, Object data) {
		if (null == client) {
			return;
		}
		JSONObject postData = JSONObject.fromObject(data);
		if (!postData.containsKey(CommonConstant.DATA_KEY_ROOM_NO)
				|| !postData.containsKey(CommonConstant.DATA_KEY_ACCOUNT)) {
			return;
		}
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		JSONObject result = new JSONObject();

		// 房间不存在
		if (!RoomManage.gameRoomMap.containsKey(roomNo) || RoomManage.gameRoomMap.get(roomNo) == null) {
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush");
			return;
		}
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		// 不在当前房间内
		if (Dto.stringIsNULL(account) || !room.getPlayerMap().containsKey(account)
				|| room.getPlayerMap().get(account) == null) {
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush");
			return;
		}
		// 刷新uuid
		room.getPlayerMap().get(account).setUuid(client.getSessionId());
		// 组织数据，通知玩家
		if (room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_INIT
				|| room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_READY) {
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			result.put("gameStatus", room.getGameStatus());
			result.put("users", room.getAllPlayer());
			result.put("showTimer", CommonConstant.GLOBAL_NO);
		} else if (room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_ING) {
			result = obtainIngReconnectData(roomNo, account);// 游戏中重连数据
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			result.put("gameStatus", room.getGameStatus());
			result.put("showTimer", CommonConstant.GLOBAL_NO);
			if (room.getTimeLeft() > 0) {
				result.put("showTimer", CommonConstant.GLOBAL_YES);
				result.put("time", room.getTimeLeft());
			}
		} else if (room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_SUMMARY
				|| room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_FINAL_SUMMARY) {
			result = obtainSummaryObject(roomNo, false, false);
			result.put("gameStatus", room.getGameStatus());
			result.put("users", room.getAllPlayer());
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			result.put("showTimer", CommonConstant.GLOBAL_NO);
		}
		if (room.getJieSanTime() > 0 && room.getGameStatus() != QZMJConstant.QZ_GAME_STATUS_FINAL_SUMMARY) {
			result.put("jiesan", CommonConstant.GLOBAL_YES);
			result.put("jiesanData", room.getCloseRoomData());
		}
		// 俱乐部
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
			result.put("clubCode", room.getClubCode());
		}
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush");
	}

	public JSONObject moPaiDeal(String roomNo, String account, int pai) {

		QZMJGameRoom game = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == game)
			return null;
		QZMJUserPacket up = game.getUserPacketMap().get(account);
		if (null == up)
			return null;
		JSONArray array = new JSONArray();
		int backType = 1;
		// 三游（或者光游时的双游）不能自摸，只能杠上摸
		if ((game.getYjType() > 2 && up.getYouJinIng() < 3)
				|| (game.isGuangYou && game.hasYouJinType(2) && up.getYouJinIng() < 2)) {
			game.setNextAskType(QZMJConstant.ASK_TYPE_HU_BY_MYSELF);
		} else {
			// 判断是否自摸
			if (checkIsHuByMySelf(roomNo, pai)) {
				backType = 3;
				// 双金必游金 start
				if (getBackTypeJin(up.getMyPai(), up.getYouJinIng(), game.getHasJinNoPingHu(), game.getJin())) {
					backType = 1;
				}
				// 双金必游金 end
			}
		}

		// 判断是否触发杠事件
		int[] gang = checkIsAnGang(roomNo, pai);
		if (gang[0] > 0) {
			// 有杠
			if (gang[0] == 1) {
				// 补杠
				backType = 9;
			} else if (gang[0] == 3) {
				// 暗杠
				backType = 2;
			}
			JSONObject obj = new JSONObject();
			obj.put("type", backType);
			obj.put("uuid", String.valueOf(account));
			obj.put("pai", pai);
			obj.put("gangValue", gang[1]);
			array.add(obj);
		}

		JSONObject obj = new JSONObject();
		obj.put("type", backType);
		obj.put("pai", pai);
		if (game.getLastMoAccount() != null) {
			// 开局补花后掉线，LastMoUUID为null
			obj.put("uuid", String.valueOf(game.getLastMoAccount()));
		} else {
			obj.put("uuid", "");
		}
		array.add(obj);

		return detailDataBymoPaiDeal(array, game, null);
	}

	public JSONObject detailDataBymoPaiDeal(JSONArray mjieguo, QZMJGameRoom game, JSONObject buhuaData) {

		// 解析结果数据
		String zhuaID = null;// 抓牌的人
		int type = 0;// 抓牌人触发的事件
		int mopai = 0;// 摸的牌
		int gangType = 0;
		int gangValue = 0;

		if (mjieguo.size() > 0) {

			if (!mjieguo.getJSONObject(0).getString("uuid").equals("")) {

				zhuaID = mjieguo.getJSONObject(0).getString("uuid");
				mopai = mjieguo.getJSONObject(0).getInt("pai");

				// 事件优先级：流局->补花->胡->杠
				if (mjieguo.size() == 1) { // 没有杠事件

					type = mjieguo.getJSONObject(0).getInt("type");

				} else if (mjieguo.getJSONObject(0).containsKey("gangValue")) { // 包含杠事件

					// 杠事件
					gangType = mjieguo.getJSONObject(0).getInt("type");
					gangValue = mjieguo.getJSONObject(0).getInt("gangValue");
					// 普通事件
					type = mjieguo.getJSONObject(1).getInt("type");
				}
			}
		}

		int[] gangvalue = null;// 杠
		if (gangType == 2) {
			gangvalue = new int[] { gangValue, gangValue, gangValue, gangValue };
		}
		if (gangType == 9) {
			gangvalue = new int[] { gangValue, gangValue, gangValue, gangValue };
		}
		Integer lastType = null;
		Object lastValue = null;
		Object lastAnValue = null;
		// 获取之前事件
		KaiJuModel jilu = game.getLastValue();
		if (jilu != null) {
			if (jilu.getType() == 2) {// 暗杠
				lastType = jilu.getType();
				lastAnValue = new int[] { jilu.getValues()[0] };
			} else if (jilu.getType() == 9) {// 抓杠
				lastType = 2;
				lastValue = new int[] { jilu.getValues()[0] };
			} else if (jilu.getType() == 1) {// 出牌
				lastType = 1;
				lastValue = jilu.getValues();
			}
		}

		JSONObject back = new JSONObject();

		if (gangType == 9) {

			// 获取抓杠位置
			int zgindex = game.getUserPacketMap().get(zhuaID).buGangIndex(mopai);
			back.put(QZMJConstant.zgindex, zgindex);
		}
		back.put(QZMJConstant.foucs, game.getPlayerIndex(zhuaID));
		back.put(QZMJConstant.foucsIndex, game.getFocusIndex());
		back.put(QZMJConstant.nowPoint, game.getNowPoint());
		back.put(QZMJConstant.lastType, lastType);
		back.put(QZMJConstant.lastValue, lastValue);
		// 返回事件询问类型
		if (type == 3 && gangType > 0) {

			back.put(QZMJConstant.type, new int[] { type, gangType });
		} else if (type == 1 && gangType > 0) {
			back.put(QZMJConstant.type, new int[] { gangType });
		} else {
			back.put(QZMJConstant.type, new int[] { type });
		}
		if (!back.containsKey(QZMJConstant.lastValue)) {
			back.put(QZMJConstant.lastValue, lastAnValue);
		}
		if (type == 3) { // 胡牌类型
			QZMJUserPacket up = game.getUserPacketMap().get(zhuaID);
			if (up.getMyPai().contains(game.getJin())) {

				int huType = MaJiangCore.huPaiHasJin(up.getMyPai(), 0, game.getJin(), game.getHasJinNoPingHu());
				// 满足双、三游条件
				int canYouJin = 0;
				// 当玩家有游金时判断是几游
				if (huType == QZMJConstant.HU_TYPE_YJ) {

					if (up.getYouJin() == 1) {
						huType = QZMJConstant.HU_TYPE_YJ;
						if (mopai == game.getJin() || MaJiangCore.shuangSanYouPanDing(up.getMyPai(), 0, game.getJin(),
								game.getHasJinNoPingHu())) { // 可以开始双游
							canYouJin = 2;
						}
					} else if (up.getYouJin() == 2) {
						huType = QZMJConstant.HU_TYPE_SHY;
						if (mopai == game.getJin() || MaJiangCore.shuangSanYouPanDing(up.getMyPai(), 0, game.getJin(),
								game.getHasJinNoPingHu())) { // 可以开始三游
							canYouJin = 3;
						}
					} else if (up.getYouJin() == 3) {
						huType = QZMJConstant.HU_TYPE_SY;
					} else {
						// 三金倒
						if (QZMJConstant.HAS_JIN_NO_PING_HU_2 != game.getHasJinNoPingHu()// 不能三金倒
								&& up.getPlayerJinCount(game.getJin()) == 3) {
							huType = QZMJConstant.HU_TYPE_SJD;
						} else {
							huType = QZMJConstant.HU_TYPE_ZM;
						}
					}
				}

				// 判断是否是天胡
				if (huType == QZMJConstant.HU_TYPE_ZM) {

					List<KaiJuModel> paiJuList = game.getKaiJuList();
					int moPaiConut = 0; // 摸牌次数
					if (paiJuList != null && paiJuList.size() < 10) {

						for (KaiJuModel kaiJu : paiJuList) {
							if (kaiJu.getType() == 1) {
								moPaiConut++;
							}
						}
						if (moPaiConut <= 1) {
							huType = QZMJConstant.HU_TYPE_TH;
						}
					}
				}

				back.put("huType", huType);
				back.put("youjin", canYouJin);

				// 是否三金倒
				if (QZMJConstant.HU_TYPE_SJD != huType && 3 == up.getPlayerJinCount(game.getJin())) {
					if (QZMJConstant.HAS_JIN_NO_PING_HU_2 != game.getHasJinNoPingHu()) {// 不能三金倒
						back.put("huType2", QZMJConstant.HU_TYPE_SJD);
					}
				}

			} else {
				back.put("huType", QZMJConstant.HU_TYPE_ZM);
				back.put("youjin", 0);
			}

		}

		if (type == 1 || type == 3) { // 出牌或者胡

			// 牌局中剩余牌数（包含其他玩家手牌）
			List<Integer> shengyuList = new ArrayList<Integer>(Dto.arrayToList(game.getPai(), game.getIndex()));
			for (String cliTag : game.getPlayerMap().keySet()) {
				if (game.getUserPacketMap().containsKey(cliTag) && game.getUserPacketMap().get(cliTag) != null) {
					if (!zhuaID.equals(cliTag)) {
						shengyuList.addAll(game.getUserPacketMap().get(cliTag).getMyPai());
					}
				}
			}

			// 出牌提示
			if (game.getPlayerMap().get(zhuaID) != null) {

				JSONArray tingTip = MaJiangCore.tingPaiTip(game.getUserPacketMap().get(zhuaID).getMyPai(),
						game.getJin(), shengyuList);
				back.put("tingTip", tingTip);
				back.put("compensateList", MaJiangCore.getCompensateList(game.getUserPacketMap().get(zhuaID).getMyPai(),
						getOutList(game.getRoomNo()), game.getJin(), tingTip));
			} else {
				back.put("tingTip", new JSONArray());
				back.put("compensateList", new JSONArray());
			}
		}
		back.put(QZMJConstant.gangvalue, gangvalue);
		back.put(QZMJConstant.value, new int[] { mopai });

		return back;
	}

	public JSONObject chuPaiDeal(Object[] jieguo, String roomNo, String thisAskAccount) {

		QZMJGameRoom game = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == game)
			return null;
		int type = Integer.valueOf(String.valueOf(jieguo[1]));
		// 最后出的牌
		int pai = game.getLastPai();
		// 上次出牌的人

		String chupai = game.getLastAccount();
		System.out.println("上次出牌的人：" + chupai);
		// 获取总牌数
		int zpaishu = game.getPai().length - game.getIndex();
		// 获取lastType
		int lastType = 1;
		// 获取lastValue
		int[] lastValue = new int[] { pai };
		int lastFoucs = game.getLastFocus();
		JSONObject result = new JSONObject();
		result.put(QZMJConstant.zpaishu, zpaishu);
		result.put(QZMJConstant.foucs, game.getPlayerIndex(thisAskAccount));
		result.put(QZMJConstant.foucsIndex, game.getPlayerIndex(chupai));
		result.put(QZMJConstant.nowPoint, game.getNowPoint());
		result.put(QZMJConstant.value, null);
		result.put(QZMJConstant.lastType, lastType);
		result.put(QZMJConstant.lastValue, lastValue);
		result.put(QZMJConstant.lastFoucs, lastFoucs);
		result.put(QZMJConstant.type, new int[] { type });
		for (String account2 : game.getPlayerMap().keySet()) {
			result.put("paipai--" + account2, game.getUserPacketMap().get(chupai).getMyPai().toArray());
		}

		if (type == 7) {
			// 胡 询问事件
			result.put(QZMJConstant.huvalue, jieguo[2]);
		} else if (type == 6) {
			// 杠 询问事件
			result.put(QZMJConstant.gangvalue, new Object[] { pai, pai, pai, pai });
		} else if (type == 5) {
			// 碰 询问事件
			result.put(QZMJConstant.pengvalue, new int[] { pai, pai, pai });
		} else if (type == 4) {
			// 吃 询问事件
			result.put(QZMJConstant.chivalue, jieguo[2]);
		} else if (type == 10) {
			// 触发多事件询问
			JSONArray array = JSONArray.fromObject(jieguo[2]);
			int[] types = new int[array.size()];
			for (int i = 0; i < array.size(); i++) {
				JSONObject obj = array.getJSONObject(i);
				if (obj.getInt("type") == 7) {
					result.put(QZMJConstant.huvalue, pai);
				} else if (obj.getInt("type") == 6) {
					result.put(QZMJConstant.gangvalue, new Object[] { pai, pai, pai, pai });
				} else if (obj.getInt("type") == 5) {
					result.put(QZMJConstant.pengvalue, new int[] { pai, pai, pai });
				} else if (obj.getInt("type") == 4) {
					result.put(QZMJConstant.chivalue, obj.get("value"));
				}
				types[i] = obj.getInt("type");
			}
			result.put(QZMJConstant.type, types);
		}
		return result;
	}

	public void sendSummaryData(String roomNo, boolean isCloseRoom, boolean forced) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		// 游戏结束
		if (room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_SUMMARY) {
			JSONObject backObj;
			if (room.getGameIndex() >= room.getGameCount()) {
				backObj = obtainSummaryObject(roomNo, true, forced);
			} else {
				backObj = obtainSummaryObject(roomNo, false, forced);
			}

			CommonConstant.sendMsgEventAll(room.getAllUUIDList(), backObj, "gameJieSuanPush");
			String winner = room.getWinner();
			// 判断是否连庄
			if (null != winner && winner.equals(room.getBanker())) {
				room.addBankTimes();
			} else {
				// 设置庄家，换下家当庄
				room.setBanker(room.getNextPlayer(room.getBanker()));
				room.setBankerTimes(1);
			}
			// 保存结算记录
			room.addKaijuList(-1, 8, new int[] {});

			summaryDeal(roomNo, isCloseRoom);// 结算处理
		}

	}

	private void summaryDeal(String roomNo, boolean isCloseRoom) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;

		updateUserScore(roomNo);
		int roomType = room.getRoomType();
		saveGameLog(roomNo);
		if (CommonConstant.ROOM_TYPE_YB == roomType) {
			saveUserDeduction(roomNo);
		}
		if (CommonConstant.ROOM_TYPE_FK == roomType || CommonConstant.ROOM_TYPE_DK == roomType
				|| CommonConstant.ROOM_TYPE_CLUB == roomType) {
			updateRoomCard(roomNo);
		}
		if (QZMJConstant.QZ_GAME_STATUS_FINAL_SUMMARY == room.getGameStatus()) {
			room.setNeedFinalSummary(true);
			finalSummaryRoom(room.getRoomNo(), isCloseRoom);// 总结算房间处理
			return;
		}
		// 小局倒计时

		int time = redisInfoService.getAppTimeSetting(room.getPlatform(), room.getGid(),
				"QZMJConstant.QZMJ_TIMER_READY_INNING");
		if (-99 == time) {
			time = QZMJConstant.QZMJ_TIMER_READY_INNING;
		}
		final int overTime = time;

		for (final String account : room.getUserPacketMap().keySet()) {
			QZMJUserPacket up = room.getUserPacketMap().get(account);
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (up == null || playerinfo == null)
				continue;
			up.setStatus(QZMJConstant.QZ_USER_STATUS_INIT);// 初始
			up.setTimeLeft(overTime);// 存入倒计时变化
			JSONObject object = new JSONObject();
			object.put("index", playerinfo.getMyIndex());
			object.put("time", overTime);
			CommonConstant.sendMsgEventToSingle(playerinfo.getUuid(), object, "qzmj_readyTimer_push"); // 通知不是准备玩家

			if (overTime >= 0) {// 小于 不自动
				// 倒计时 定时
				ThreadPoolHelper.executorService.submit(new Runnable() {
					@Override
					public void run() {
						gameTimerQZMJ.readyOverTime(roomNo, account, overTime);
					}
				});
			}

		}
	}

	public void startGame(final String roomNo) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;

		if (room.getGameIndex() > room.getGameCount()) {
			compulsorySettlement(room.getRoomNo());// 局数超过 强制结算
			return;
		}
		// 确定庄家
		if (room.choiceBanker()) {
			room.initGame();
			// 更新游戏局数
			if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB
					|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
				JSONObject roomInfo = new JSONObject();
				roomInfo.put("roomId", room.getId());
				producerService.sendMessage(daoQueueDestination,
						new PumpDao(DaoTypeConstant.UPDATE_ROOM_INDEX, roomInfo));
			}
			RoomManage.gameRoomMap.get(roomNo).setGameStatus(QZMJConstant.QZ_GAME_STATUS_ING);
			// 摇骰子
			room.obtainDice();
			// 洗牌
			room.shufflePai();
			// 开金
			room.choiceJin();
			// 发牌
			room.faPai();

			// 保存游戏发牌记录
			saveGameFaPaiRecords(roomNo);
			// 消耗元宝
			if (room.getFee() > 0 && room.getRoomType() != CommonConstant.ROOM_TYPE_FK
					&& room.getRoomType() != CommonConstant.ROOM_TYPE_DK
					&& room.getRoomType() != CommonConstant.ROOM_TYPE_CLUB
					&& room.getRoomType() != CommonConstant.ROOM_TYPE_FREE
					&& room.getRoomType() != CommonConstant.ROOM_TYPE_INNING) {
				JSONArray array = new JSONArray();
				for (String account : room.getPlayerMap().keySet()) {
					if (room.getPlayerMap().containsKey(account) && room.getPlayerMap().get(account) != null) {
						// 中途加入不抽水
						if (room.getUserPacketMap().get(account).getStatus() > QZMJConstant.QZ_USER_STATUS_INIT) {
							// 更新实体类数据
							Playerinfo playerinfo = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap()
									.get(account);
							RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account)
									.setScore(Dto.sub(playerinfo.getScore(), room.getFee()));
							// 负数清零
							if (RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account)
									.getScore() < 0) {
								RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setScore(0);
							}
							array.add(playerinfo.getId());
						}
					}
				}
				// 抽水
				System.out.println("泉州麻将抽水==============================");
				producerService.sendMessage(daoQueueDestination,
						new PumpDao(DaoTypeConstant.PUMP, room.getJsonObject(array)));
			}
			// 俱乐部 扣除房卡
			else if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
				logger.info("俱乐部-------开始游戏扣除房卡");
			}
			// 通知玩家
			for (String account : room.getPlayerMap().keySet()) {
				if (room.getPlayerMap().containsKey(account) && room.getPlayerMap().get(account) != null) {
					room.getUserPacketMap().get(account).setStatus(QZMJConstant.QZ_USER_STATUS_GAME);
					JSONObject result = new JSONObject();
					// 返回骰子点数
					result.put("dice", room.getDice());
					// 返回庄家的位置
					result.put(QZMJConstant.zhuang, room.getPlayerIndex(room.getBanker()));
					// 返回连庄数
					result.put("lianzhuang", room.getBankerTimes());
					if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB
							|| room.getRoomType() == CommonConstant.ROOM_TYPE_FREE) {
						result.put("lianzhuang", 0);
					}
					// 返回焦点的位置
					result.put(QZMJConstant.foucs, room.getPlayerIndex(room.getBanker()));
					result.put(QZMJConstant.foucsIndex, room.getFocusIndex());
					result.put(QZMJConstant.nowPoint, room.getNowPoint());
					result.put(QZMJConstant.lastFoucs, -1);
					// 返回总牌数
					result.put(QZMJConstant.pai, room.getPai().length);
					result.put(QZMJConstant.zpaishu, QZMJConstant.PAI_COUNT);
					// 返回游戏的底分
					result.put(QZMJConstant.soure, room.getScore());
					// 返回金
					result.put(QZMJConstant.jin, room.getJin());
					Object[] myPai = room.getUserPacketMap().get(account).getMyPai().toArray();
					// 返回发给我的牌
					result.put(QZMJConstant.myPai, myPai);
					// 返回我对手的牌
					String duishou = "";
					for (String account2 : room.getPlayerMap().keySet()) {
						if (!account.equals(account2)) {
							duishou = account2;
							break;
						}
					}
					Object[] duishouPai = room.getUserPacketMap().get(duishou).getMyPai().toArray();
					result.put("duishouPai", duishouPai);
					// 返回发给我的位置
					result.put(QZMJConstant.myIndex, room.getPlayerMap().get(account).getMyIndex());
					result.put("game_index", room.getGameNewIndex());
					result.put("users", room.getAllPlayer());
					CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(account).getUuid(),
							String.valueOf(result), "gameStartPush");
				}
			}
			final int startStatus;
			if (room.getGameIndex() > 1 || (room.getRoomType() != CommonConstant.ROOM_TYPE_FK
					&& room.getRoomType() != CommonConstant.ROOM_TYPE_DK
					&& room.getRoomType() != CommonConstant.ROOM_TYPE_CLUB
					&& room.getRoomType() != CommonConstant.ROOM_TYPE_INNING
					&& room.getRoomType() != CommonConstant.ROOM_TYPE_TEA)) {
				startStatus = 2;
			} else {
				startStatus = 1;
			}
			ThreadPoolHelper.executorService.submit(new Runnable() {
				@Override
				public void run() {
					gameTimerQZMJ.gameStart(roomNo, startStatus);
				}
			});
		}
	}

	public void saveGameFaPaiRecords(String roomNo) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		// 记录发牌记录
		for (String account : room.getUserPacketMap().keySet()) {
			QZMJUserPacket up = room.getUserPacketMap().get(account);
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (null == up || null == playerinfo || null == up.getMyPai() || up.getMyPai().size() == 0)
				continue;
			room.addKaijuList(playerinfo.getMyIndex(), 10, up.getMyPai().stream().mapToInt(Integer::valueOf).toArray());
		}
	}

	public JSONArray obtainFinalSummaryArray(QZMJGameRoom room) {
		JSONArray fianlSummaryArray = new JSONArray();
		String gameId = room.getRoomNo();
		Set<String> uuids = room.getPlayerMap().keySet();
		// 大赢家分数
		int dayinjia = 0;
		for (String account : uuids) {
			if (room.getPlayerMap().containsKey(account) && room.getPlayerMap().get(account) != null) {
				if (room.getPlayerMap().get(account).getScore() >= dayinjia) {
					dayinjia = (int) room.getPlayerMap().get(account).getScore();
				}
			}
		}
		// 设置玩家信息

		for (String account : uuids) {
			Playerinfo player = room.getPlayerMap().get(account);
			JSONObject obj = new JSONObject();
			obj.element("cut_type", room.getCutType());
			obj.element("cut_score", player.getCutScore());
			obj.put("name", player.getName());
			obj.put("account", player.getAccount());
			obj.put("headimg", player.getRealHeadimg());
			int score = (int) player.getScore();
			obj.put("score", score);

			if (account.equals(room.getOwner())) {

				obj.put("isFangzhu", 1);
			} else {
				obj.put("isFangzhu", 0);
			}
			// 设置大赢家
			if (player.getScore() == dayinjia) {
				obj.put("isWinner", 1);
			} else {
				obj.put("isWinner", 0);
			}

			// 获取胡牌类型次数
			JSONArray huTimes = getHuTypeTimes(gameId, account);

			obj.put("huTypeTimes", huTimes);
			obj.put("compensateScore", room.getUserPacketMap().get(account).getCompensateScore());

			fianlSummaryArray.add(obj);
		}
		return fianlSummaryArray;
	}

	public JSONArray getHuTypeTimes(String roomNo, String account) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return null;
		QZMJUserPacket up = room.getUserPacketMap().get(account);
		// 统计牌局信息
		JSONArray array = new JSONArray();
		if (up.getPingHuTimes() > 0) {
			JSONObject obj = new JSONObject();
			obj.put("name", "平胡次数 ");
			obj.put("val", up.getPingHuTimes());
			array.add(obj);
		}
		if (up.getZiMoTimes() > 0) {
			JSONObject obj = new JSONObject();
			obj.put("name", "自摸次数 ");
			obj.put("val", up.getZiMoTimes());
			array.add(obj);
		}
		if (up.getSanJinDaoTimes() > 0) {
			JSONObject obj = new JSONObject();
			obj.put("name", "三金倒次数 ");
			obj.put("val", up.getSanJinDaoTimes());
			array.add(obj);
		}
		if (up.getYouJinTimes() > 0) {
			JSONObject obj = new JSONObject();
			obj.put("name", "游金次数 ");
			obj.put("val", up.getYouJinTimes());
			array.add(obj);
		}
		if (up.getShuangYouTimes() > 0) {
			JSONObject obj = new JSONObject();
			obj.put("name", "双游次数 ");
			obj.put("val", up.getShuangYouTimes());
			array.add(obj);
		}
		if (up.getSanYouTimes() > 0) {
			JSONObject obj = new JSONObject();
			obj.put("name", "三游次数 ");
			obj.put("val", up.getSanYouTimes());
			array.add(obj);
		}
		if (up.getTianHuTimes() > 0) {
			JSONObject obj = new JSONObject();
			obj.put("name", "天胡次数 ");
			obj.put("val", up.getTianHuTimes());
			array.add(obj);
		}
		if (up.getQiangGangHuTimes() > 0) {
			JSONObject obj = new JSONObject();
			obj.put("name", "抢杠胡次数 ");
			obj.put("val", up.getQiangGangHuTimes());
			array.add(obj);
		}
		if (room.getPlayerMap().get(account).getRewardScore() != 0) {
			JSONObject obj = new JSONObject();
			obj.put("name", "奖励分 ");
			obj.put("val", room.getPlayerMap().get(account).getRewardScore());
			array.add(obj);
		}
		return array;
	}

	public JSONObject obtainEnterData(String roomNo, String account) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		// 获取pdk背景设置 如果没有 则为1
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
			bg = jb.getString("mjbg");
		}

		if (null == room)
			return null;
		JSONObject obj = new JSONObject();
		obj.put("bg", bg);
		obj.put("playerCount", room.getPlayerCount());
		obj.put("room_no", roomNo);
		obj.put("fanMultiple", room.getFanMultiple());// 番倍
		obj.put("score", room.getScore());// 低分
		obj.put("zhuangScore", room.getZhuangScore());// 庄分
		obj.put("xianScore", room.getXianScore());// 闲分
		obj.put("game_count", room.getGameCount());
		obj.put("game_index", room.getGameNewIndex());
		obj.put("gameStatus", room.getGameStatus());
		obj.put("youjin", room.getYouJinScore());
		obj.put("users", room.getAllPlayer());
		obj.put("myIndex", room.getPlayerMap().get(account).getMyIndex());
		obj.put("gid", room.getGid());
		obj.put("roomType", room.getRoomType());
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
			obj.put("clubCode", room.getClubCode());
		}
		StringBuffer roomInfo = new StringBuffer();
		roomInfo.append(room.getPlayerCount());
		roomInfo.append("人 ");

		if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB || room.getRoomType() == CommonConstant.ROOM_TYPE_YB
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_FREE
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
			roomInfo.append("    庄:");
			roomInfo.append(room.getZhuangScore());
			roomInfo.append(" 闲:");
			roomInfo.append(room.getXianScore());
			StringBuffer roomInfo2 = new StringBuffer();
			roomInfo2.append("入场:");
			roomInfo2.append((int) room.getEnterScore());
			roomInfo2.append("  离场:");
			roomInfo2.append((int) room.getLeaveScore());
			obj.put("roominfo2", String.valueOf(roomInfo2));
		}
		obj.put("roominfo", String.valueOf(roomInfo));
		JSONArray timeArray = new JSONArray();
		timeArray.add(0);
		timeArray.add(room.getPlayTime());
		obj.put("timeArray", timeArray);
		return obj;
	}

	public int hu(String roomNo, String account, int type) {

		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return 0;
		// 胡牌类型
		int huType = 0;
		QZMJUserPacket up = room.getUserPacketMap().get(account);
		// 设置赢家
		room.setWinner(account);
		// 获取我的牌
		List<Integer> myPai = up.getMyPai();
		int pai;
		// 自摸
		if (type == 3) {
			// 获取上次抓的牌
			pai = room.getLastMoPai();
			huType = MaJiangCore.huPaiType(myPai, pai, room.getJin(), 1, room.getHasJinNoPingHu());
		} else {
			// 获取上次出的牌
			pai = room.getLastPai();
			huType = MaJiangCore.huPaiType(myPai, pai, room.getJin(), 2, room.getHasJinNoPingHu());
		}
		if (huType > 0) {
			// 胡类型
			if (huType == QZMJConstant.HU_TYPE_YJ) {
				if (up.getYouJin() == 1) {
					room.setHuType(QZMJConstant.HU_TYPE_YJ);
				} else if (up.getYouJin() == 2) {
					room.setHuType(QZMJConstant.HU_TYPE_SHY);
				} else if (up.getYouJin() == 3) {
					room.setHuType(QZMJConstant.HU_TYPE_SY);
				} else {
					// 三金倒
					if (QZMJConstant.HAS_JIN_NO_PING_HU_2 != room.getHasJinNoPingHu()// 不能三金倒
							&& up.getPlayerJinCount(room.getJin()) == 3) {
						room.setHuType(QZMJConstant.HU_TYPE_SJD);
					} else {
						room.setHuType(QZMJConstant.HU_TYPE_ZM);
					}
				}
			} else {
				room.setHuType(huType);
			}
			// 判断是否是天胡
			if (huType == QZMJConstant.HU_TYPE_ZM) {
				List<KaiJuModel> paiJuList = room.getKaiJuList();
				// 摸牌次数
				int moPaiConut = 0;
				for (KaiJuModel kaiJu : paiJuList) {
					if (kaiJu.getType() == 1) {
						moPaiConut++;
					}
				}
				if (moPaiConut <= 1) {
					room.setHuType(QZMJConstant.HU_TYPE_TH);
				}
			}
			if (myPai.size() % 3 == 1) {
				// 完整的手牌
				myPai.add(pai);
			} else {
				// 胡的牌要放在最后一张
				for (int i = myPai.size() - 1; i >= 0; i--) {
					if (myPai.get(i).equals(pai)) {
						// 移除胡的牌
						myPai.remove(i);
						// 添加到末尾
						myPai.add(pai);
						break;
					}
				}
			}
			if (room.getGid() == CommonConstant.GAME_ID_QZMJ || room.getGid() == CommonConstant.GAME_ID_ZZC) {
				summary(roomNo, account);
			} else if (room.getGid() == CommonConstant.GAME_ID_NAMJ) {
				summaryNA(roomNo, account);
			}
			room.getUserPacketMap().get(account).addHuTimes(room.getHuType());
		}

		return huType;
	}

	public void summary(String roomNo, String winAccount) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		int roomType = room.getRoomType();

		// 计算番
		List<String> loserList = new ArrayList<>();// 输家列表
		for (String account : room.getUserPacketMap().keySet()) {
			QZMJUserPacket up = room.getUserPacketMap().get(account);
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (up == null || playerinfo == null)
				continue;
			up.setFan(up.getTotalFanShu(up.getMyPai(), room, account));
			if (!winAccount.equals(account))
				loserList.add(account);
		}

		boolean summaryLose = true;
		String compensateAccount = null;

		if (room.getGid() == CommonConstant.GAME_ID_ZZC) {// 支支长
			if (room.getCompensateMap().containsKey(winAccount)) {
				compensateAccount = room.getCompensateMap().get(winAccount);
				summaryLose = false;
			}
		}
		int gameCount = room.getGameCount();
		// 番倍
		int fanMultiple = room.getFanMultiple();

		if (summaryLose) {
			// 其他未胡玩家结算
			int fan1;
			int fan2;
			for (int i = 0; i < loserList.size(); i++) {
				fan1 = room.getUserPacketMap().get(loserList.get(i)).getFan();
				for (int j = i + 1; j < loserList.size(); j++) {
					fan2 = room.getUserPacketMap().get(loserList.get(j)).getFan();
					room.getUserPacketMap().get(loserList.get(i)).setScore(
							(fan1 - fan2) * fanMultiple + room.getUserPacketMap().get(loserList.get(i)).getScore());
					room.getUserPacketMap().get(loserList.get(j)).setScore(
							(fan2 - fan1) * fanMultiple + room.getUserPacketMap().get(loserList.get(j)).getScore());
				}
			}
		}

		// 赢家的总番数
		int winTotalFan = room.getUserPacketMap().get(winAccount).getFan() * fanMultiple;

		// 奖励分数
		int rewardScore = 0;
		if (room.getHuType() == QZMJConstant.HU_TYPE_YJ) {
			rewardScore = room.getYjRewardScore();
		} else if (room.getHuType() == QZMJConstant.HU_TYPE_SHY) {
			rewardScore = room.getShyRewardScore();
		} else if (room.getHuType() == QZMJConstant.HU_TYPE_SY) {
			rewardScore = room.getSyRewardScore();
		}

		double loserScore;// 输家分数
		for (String account : loserList) {
			QZMJUserPacket up = room.getUserPacketMap().get(account);
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (null == up || null == playerinfo)
				continue;
			int difen;
			if (winAccount.equals(room.getBanker())) {// 赢家是庄家
				difen = room.getUserScore(winAccount);
			} else {
				difen = room.getUserScore(account);
			}

			// 赢家（底注+番数）x胡法倍数x倍数
			int winScore = QZMJGameRoom.jiSuanScore(room.getHuType(), difen, winTotalFan, room.getYouJinScore());

			loserScore = Dto.sub(winScore, up.getScore());
			double score = 0D;
			// 元宝场 1课 控制 分数
			if (CommonConstant.ROOM_TYPE_YB == roomType || QZMJConstant.QZMJ_KE_GAME_COUNT == gameCount) {
				double playerScore = Dto.add(playerinfo.getSourceScore(), playerinfo.getScore());
				if (playerScore < loserScore) {
					score = Dto.sub(playerScore, loserScore);
					loserScore = playerScore;
				}
			}
			// 输家分数
			up.setScore(-loserScore);
			playerinfo.setScore(Dto.sub(playerinfo.getScore(), loserScore));

			// 赢家分数
			room.getUserPacketMap().get(winAccount)
					.setScore(Dto.add(room.getUserPacketMap().get(winAccount).getScore(), Dto.add(winScore, score)));
			room.getPlayerMap().get(winAccount)
					.setScore(Dto.add(room.getPlayerMap().get(winAccount).getScore(), Dto.add(winScore, score)));

			if (rewardScore > 0) {
				playerinfo.setRewardScore(Dto.sub(playerinfo.getRewardScore(), rewardScore));
				room.getPlayerMap().get(winAccount)
						.setRewardScore(Dto.add(room.getPlayerMap().get(winAccount).getRewardScore(), rewardScore));
			}
		}

		if (room.getGid() == CommonConstant.GAME_ID_ZZC && !summaryLose && !Dto.stringIsNULL(compensateAccount)) {// 支支长
			if (room.getUserPacketMap().containsKey(compensateAccount)
					&& room.getUserPacketMap().get(compensateAccount) != null) {
				for (String account : room.getUserPacketMap().keySet()) {
					double sum = room.getUserPacketMap().get(account).getScore();
					if (sum > 0D) {
						room.getUserPacketMap().get(account)
								.setCompensateScore(room.getUserPacketMap().get(account).getCompensateScore() + sum);
					} else {
						room.getUserPacketMap().get(compensateAccount).setCompensateScore(
								room.getUserPacketMap().get(compensateAccount).getCompensateScore() + sum);
					}
					room.getPlayerMap().get(account)
							.setScore(Dto.sub(room.getPlayerMap().get(account).getScore(), sum));
				}
			}
		}

		room.setGameStatus(QZMJConstant.QZ_GAME_STATUS_SUMMARY);

		// 标识牌局是否结束
		boolean isFinish = false;
		// 倒课玩家数
		int loseCount = 0;
		if (QZMJConstant.QZMJ_KE_GAME_COUNT == room.getGameCount()) {// 1课玩法
			for (String account : room.getPlayerMap().keySet()) {
				Playerinfo playerinfo = room.getPlayerMap().get(account);
				if (null == playerinfo)
					continue;
				if (Dto.add(playerinfo.getScore(), playerinfo.getSourceScore()) <= 0) {
					isFinish = true;// 没有分数 直接总结算
					// break;
					loseCount++;
				}
			}
		}
		if (isFinish) {
			room.setGameIndex(room.getGameCount());
			// 速三奖励
			if (room.getPlayerCount() == 4 && loseCount == 3) {
				for (String account : room.getPlayerMap().keySet()) {
					Playerinfo playerinfo = room.getPlayerMap().get(account);
					if (null == playerinfo)
						continue;
					int ssRewardScore = Dto.add(playerinfo.getScore(), playerinfo.getSourceScore()) <= 0
							? -room.getSsRewardScore()
							: 3 * room.getSsRewardScore();
					room.getPlayerMap().get(account)
							.setRewardScore(Dto.add(room.getPlayerMap().get(account).getRewardScore(), ssRewardScore));
				}
			}
		}
	}

	public void summaryNA(String roomNo, String account) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		// 计算番
		for (String uuid : room.getPlayerMap().keySet()) {
			if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
				int fan = room.getUserPacketMap().get(uuid).getNanAnTotalFanShu();
				room.getUserPacketMap().get(uuid).setFan(fan);
			}
		}
		// 赢家的总番数
		int winnerTotalFan = room.getUserPacketMap().get(account).getFan();

		// 赢家结算 account 为赢家的uuid
		for (String uuid : room.getPlayerMap().keySet()) {
			if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
				if (!uuid.equals(account)) {
					Playerinfo p = room.getPlayerMap().get(uuid);
					QZMJUserPacket up = room.getUserPacketMap().get(uuid);
					int difen = 0;
					if (room.getHuType() == QZMJConstant.HU_TYPE_ZM || room.getHuType() == QZMJConstant.HU_TYPE_QGH) {
						difen = QZMJConstant.SCORE_TYPE_NA_ZM;
					} else if (room.getHuType() == QZMJConstant.HU_TYPE_YJ) {
						difen = QZMJConstant.SCORE_TYPE_NA_YJ;
					} else if (room.getHuType() == QZMJConstant.HU_TYPE_SHY) {
						difen = QZMJConstant.SCORE_TYPE_NA_SHY;
					} else if (room.getHuType() == QZMJConstant.HU_TYPE_SY) {
						difen = QZMJConstant.SCORE_TYPE_NA_SY;
					}
					if (account.equals(room.getBanker())) {
						// 庄赢（双倍底分）
						difen = difen * 2;
					} else if (uuid.equals(room.getBanker())) {
						// 连庄底分加倍
						difen = difen * 2;
					}
					// 计分
					int score = difen + winnerTotalFan;

					// 更新未胡的玩家分数
					p.setScore(Dto.sub(p.getScore(), score));
					up.setScore(Dto.sub(up.getScore(), score));
					// 更新胡的玩家分数
					room.getPlayerMap().get(account)
							.setScore(Dto.add(room.getPlayerMap().get(account).getScore(), score));
					room.getUserPacketMap().get(account)
							.setScore(Dto.add(room.getUserPacketMap().get(account).getScore(), score));
				}
			}
		}
		room.setGameStatus(QZMJConstant.QZ_GAME_STATUS_SUMMARY);
	}

	public void updateUserScore(String roomNo) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (room == null)
			return;
		JSONArray array = new JSONArray();
		Map<String, Double> map = new HashMap<String, Double>();
		JSONObject pumpInfo = new JSONObject();
		// 存放游戏记录
		for (String uuid : room.getPlayerMap().keySet()) {
			Playerinfo playerinfo = room.getPlayerMap().get(uuid);
			QZMJUserPacket up = room.getUserPacketMap().get(uuid);
			if (null == playerinfo || null == up)
				continue;

			// 有参与的玩家
			if (playerinfo.getPlayTimes() > 0) {
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
			}
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
		if (array.size() > 0) {
			producerService.sendMessage(daoQueueDestination,
					new PumpDao(DaoTypeConstant.UPDATE_SCORE, room.getPumpObject(array)));
		}
	}

	private void userPumpingFee(String roomNo) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (room == null)
			return;
		int gameStatus = room.getGameStatus();// 房间状态
		if (QZMJConstant.QZ_GAME_STATUS_SUMMARY != gameStatus
				&& QZMJConstant.QZ_GAME_STATUS_FINAL_SUMMARY != gameStatus)
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
			for (String account : room.getPlayerMap().keySet()) {
				QZMJUserPacket up = room.getUserPacketMap().get(account);
				if (null == up)
					continue;
				treeSet.add(up.getScore());
				if (up.getScore() > maxScore) {
					maxScore = up.getScore();
				}
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
			QZMJUserPacket up = room.getUserPacketMap().get(account);
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
					System.out.println("固定消耗抽水========================");
					deductScore = (double) room.getCutList().get(0);
				} catch (Exception e) {
					logger.info("泉州麻将固定抽水设置错误");
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
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
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
		JSONArray attachLogArray = new JSONArray();
		String time = DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss");
		int gameSum = room.getGameNewIndex() == room.getGameCount() ? room.getGameCount() : room.getGameNewIndex() - 1;
		for (String account : room.getPlayerMap().keySet()) {
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			QZMJUserPacket up = room.getUserPacketMap().get(account);
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

			// 战绩信息 平胡次数,游金次数等信息
			JSONObject attachInfo = new JSONObject();
			attachInfo.element("rewardScore", playerinfo.getRewardScore()).element("pingHuTimes", up.getPingHuTimes())
					.element("ziMoTimes", up.getZiMoTimes()).element("sanJinDaoTimes", up.getSanJinDaoTimes())
					.element("youJinTimes", up.getYouJinTimes()).element("shuangYouTimes", up.getShuangYouTimes())
					.element("sanYouTimes", up.getSanYouTimes()).element("tianHuTimes", up.getTianHuTimes())
					.element("qiangGangHuTimes", up.getQiangGangHuTimes());
			JSONObject userInfo = new JSONObject();
			userInfo.element(playerinfo.getAccount(), attachInfo);
			jsonObject.element("game_sum", gameSum).element("account", playerinfo.getAccount())
					.element("name", playerinfo.getName()).element("headimg", playerinfo.getHeadimg())
					.element("cut_type", room.getCutType()).element("cut_score", playerinfo.getCutScore())
					.element("za_games_id", room.getGid()).element("room_type", room.getRoomType())
					.element("user_id", playerinfo.getId()).element("room_no", room.getRoomNo())
					.element("room_id", room.getId()).element("circle_code", room.getClubCode())
					.element("create_time", time).element("is_big_winner", isBigWinner);
			if (CircleConstant.CUTTYPE_WU.equals(room.getCutType())) {
				double score = Dto.add(room.getPlayerMap().get(account).getScore(),
						room.getPlayerMap().get(account).getRewardScore());
				jsonObject.element("score", score);
			} else {
				double score = Dto.add(
						Dto.add(room.getPlayerMap().get(account).getScore(),
								room.getPlayerMap().get(account).getCutScore()),
						room.getPlayerMap().get(account).getRewardScore());
				jsonObject.element("score", score);
			}
			array.add(jsonObject);
			attachLogArray.add(userInfo);
		}

		JSONObject object = new JSONObject();
		object.element("array", array);
		// 附加信息
		JSONObject attachInfo = new JSONObject();
		attachInfo.element("extraInfo", attachLogArray);
		attachInfo.element("gameId", room.getGid());
		object.element("attachInfo", attachInfo);
		producerService.sendMessage(daoQueueDestination,
				new PumpDao(DaoTypeConstant.INSERT_ZA_USER_GAME_STATIS, object));
		if (CommonConstant.ROOM_TYPE_TEA == room.getRoomType()) {// 茶楼分数控制
			teaService.updateTeaMemberScoreALl(array, room.getCircleId());
		}
		// 局数场统计奖励分数
		if (CommonConstant.ROOM_TYPE_INNING == room.getRoomType()) {
			JSONObject pumpInfo = new JSONObject();
			// 存放游戏记录
			for (String uuid : room.getPlayerMap().keySet()) {
				Playerinfo playerinfo = room.getPlayerMap().get(uuid);
				if (playerinfo.getRewardScore() != 0) {
					pumpInfo.put(String.valueOf(playerinfo.getId()), playerinfo.getRewardScore());
				}
			}
			if (pumpInfo.size() > 0) {
				JSONObject rewardObject = new JSONObject();
				rewardObject.put("circleId", room.getCircleId());
				rewardObject.put("roomNo", room.getRoomNo());
				rewardObject.put("gameId", room.getGid());
				rewardObject.put("pumpInfo", pumpInfo);
				rewardObject.put("cutType", room.getCutType());
				rewardObject.put("changeType", CircleConstant.PROP_BILL_CHANGE_TYPE_GAME);
				// 亲友圈玩家输赢
				gameCircleService.circleUserPumping(rewardObject);
			}
		}
	}

	public JSONObject obtainUserScoreData(double total, double sum, long id) {
		JSONObject obj = new JSONObject();
		obj.put("total", total);
		obj.put("fen", sum);
		obj.put("id", id);
		return obj;
	}

	public void saveGameLog(String roomNo) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room || CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE == room.getIsClose()
				&& QZMJConstant.QZ_GAME_STATUS_SUMMARY != room.getGameStatus()
				&& QZMJConstant.QZ_GAME_STATUS_FINAL_SUMMARY != room.getGameStatus())
			return;

		JSONArray gameLogResults = new JSONArray();
		JSONArray gameResult = new JSONArray();
		JSONArray array = new JSONArray();
		for (String account : room.getUserPacketMap().keySet()) {
			QZMJUserPacket up = room.getUserPacketMap().get(account);
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (up == null || playerinfo == null)
				continue;
			if (QZMJConstant.QZ_USER_STATUS_INIT == up.getStatus()
					|| QZMJConstant.QZ_USER_STATUS_READY == up.getStatus())
				continue;

			double total = playerinfo.getScore();
			double sum = up.getScore();
			long userId = playerinfo.getId();
			array.add(obtainUserScoreData(total, sum, userId));
			// 战绩记录
			JSONObject gameLogResult = new JSONObject();
			gameLogResult.put("account", account);
			gameLogResult.put("name", playerinfo.getName());
			gameLogResult.put("headimg", playerinfo.getHeadimg());
			if (!Dto.stringIsNULL(room.getBanker()) && room.getPlayerMap().containsKey(room.getBanker())
					&& room.getPlayerMap().get(room.getBanker()) != null) {
				gameLogResult.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
			} else {
				gameLogResult.put("zhuang", CommonConstant.NO_BANKER_INDEX);
			}
			gameLogResult.put("myIndex", playerinfo.getMyIndex());
			gameLogResult.put("score", up.getScore());
			gameLogResult.put("totalScore", playerinfo.getScore());
			gameLogResult.put("win", CommonConstant.GLOBAL_YES);
			if (up.getScore() < 0D) {
				gameLogResult.put("win", CommonConstant.GLOBAL_NO);
			}
			gameLogResults.add(gameLogResult);
			// 用户战绩
			JSONObject userResult = new JSONObject();
			userResult.put("zhuang", room.getBanker());
			userResult.put("isWinner", CommonConstant.GLOBAL_NO);
			if (up.getScore() > 0D) {
				userResult.put("isWinner", CommonConstant.GLOBAL_YES);
			}
			userResult.put("score", up.getScore());
			userResult.put("totalScore", playerinfo.getScore());
			userResult.put("player", playerinfo.getName());
			userResult.put("account", account);
			gameResult.add(userResult);
		}
		// 战绩信息
		JSONObject gameLogObj = room.obtainGameLog(gameLogResults.toString(),
				JSONArray.fromObject(room.getKaiJuList()).toString());
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

	public void updateRoomCard(String roomNo) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		JSONArray array = new JSONArray();
		int roomCardCount = 0;
		if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB
				|| room.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
			for (String account : room.getUserPacketMap().keySet()) {
				if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
					// 房主支付
					if (CommonConstant.ROOM_PAY_TYPE_FANGZHU.equals(room.getPayType())) {
						if (account.equals(room.getOwner())) {
							// 参与第一局需要扣房卡
							if (room.getPlayerMap().get(account).getPlayTimes() == 1) {
								array.add(room.getPlayerMap().get(account).getId());
								roomCardCount = room.getPlayerCount() * room.getSinglePayNum();
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
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		JSONArray userDeductionData = new JSONArray();
		for (String account : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
				// 用户游戏记录
				JSONObject object = new JSONObject();
				object.put("id", room.getPlayerMap().get(account).getId());
				object.put("roomNo", room.getRoomNo());
				object.put("gid", room.getGid());
				object.put("type", room.getRoomType());
				object.put("fen", room.getUserPacketMap().get(account).getScore());
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
		// 玩家输赢记录
		producerService.sendMessage(daoQueueDestination,
				new PumpDao(DaoTypeConstant.USER_DEDUCTION, new JSONObject().element("user", userDeductionData)));
	}

	public int[] gang(String roomNo, String account, int gangType) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return null;
		int[] back = null;
		QZMJUserPacket up = room.getUserPacketMap().get(account);
		// 获取上次出的牌
		int pai = 0;
		// 获取我的牌
		List<Integer> myPai = up.getMyPai();
		// 获取最后一次摸排的人
		if (gangType == 2 || gangType == 9) {
			// 获取我碰过的排
			pai = room.getLastMoPai();
			List<DontMovePai> penghistory = up.getPengList();
			back = MaJiangCore.isGang(myPai, pai, 1, penghistory);
		} else if (gangType == 6) {
			// 明杠
			pai = room.getLastPai();
			back = MaJiangCore.isGang(myPai, pai, 2, null);
		}
		int type = back[0];
		// 杠
		if (type > 0) {

			pai = back[1];
			// 处理杠事件
			if (type == 1) {
				// 抓，补杠
				List<Integer> indexList = new ArrayList<Integer>();
				// 取得当前的牌是否能有人胡牌
				for (String player : room.getUserPacketMap().keySet()) {
					if (MaJiangCore.isHu(room.getUserPacketMap().get(player).getMyPai(), pai, room.getJin(),
							room.getHasJinNoPingHu())) {
						indexList.add(room.getPlayerIndex(player));
					}
				}

				// 有玩家可以抢杠
				if (indexList.size() > 0) {

					back = new int[indexList.size() + 1];
					back[0] = -1;
					Collections.sort(indexList);
					for (int i = 0; i < indexList.size(); i++) {
						back[i + 1] = indexList.get(i);
					}

				} else {
					// 1.从手牌中减去1张牌
					up.removeMyPai(pai);
					// 2.记录到不可动的排
					// 获取抓杠位置
					int zgindex = up.buGangIndex(pai);
					// 将碰的牌组转化成杠牌组
					for (DontMovePai dmpai : up.getPengList()) {
						int focusPai = dmpai.getFoucsPai();
						if (focusPai == pai) {
							dmpai.updateDontMovePai(5, pai, focusPai);
						}
					}
					// 3.记录到牌局记录
					room.addKaijuList(room.getPlayerIndex(account), 9, new int[] { pai, pai, pai, pai, zgindex });
				}

			} else if (type == 3) {
				// 暗杠
				// 1.从手牌中减去4张牌
				up.removeMyPai(pai);
				up.removeMyPai(pai);
				up.removeMyPai(pai);
				up.removeMyPai(pai);
				// 2.记录到不可动的排
				up.addHistoryPai(3, new int[] { pai, pai, pai, pai }, pai);
				// 3.记录到牌局记录
				room.addKaijuList(room.getPlayerIndex(account), 2, new int[] { pai, pai, pai, pai });
			} else if (type == 2) {
				// 明杠
				// 1.从手牌中减去3张牌
				up.removeMyPai(pai);
				up.removeMyPai(pai);
				up.removeMyPai(pai);
				// 2.记录到不可动的排
				up.addHistoryPai(4, new int[] { pai, pai, pai, pai }, pai);
				// 3.记录到牌局记录
				room.addKaijuList(room.getPlayerIndex(account), 6, new int[] { pai, pai, pai, pai });
				// 4.给玩家计分
			}

			// 4.定位ThisUUID
			room.setThisType(QZMJConstant.THIS_TYPE_ZUA);
			room.setThisAccount(account);
		}

		return back;
	}

	public boolean peng(String roomNo, String account) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return false;
		// 获取上次出的牌
		int pai = room.getLastPai();
		QZMJUserPacket up = room.getUserPacketMap().get(account);
		// 获取我的牌
		List<Integer> myPai = up.getMyPai();
		int[] back = MaJiangCore.isPeng(myPai, pai);
		if (back[0] > 0) {
			// 处理碰事件
			// 1.从手牌中减去2张牌
			up.removeMyPai(pai);
			up.removeMyPai(pai);
			// 2.记录到不可动的排
			up.addHistoryPai(2, new int[] { pai, pai, pai }, pai);
			// 3.记录到牌局记录
			room.addKaijuList(room.getPlayerIndex(account), 5, new int[] { pai, pai, pai });
			// 4.定位ThisUUID
			room.setThisType(QZMJConstant.THIS_TYPE_CHU);
			room.setThisAccount(account);
			return true;
		}

		return false;
	}

	public boolean chi(String roomNo, int[] chiValue, String account) {

		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return false;
		// 获取上次出的牌
		int pai = room.getLastPai();
		QZMJUserPacket up = room.getUserPacketMap().get(account);
		// 获取我的牌
		List<Integer> myPai = up.getMyPai();
		List<int[]> back = MaJiangCore.isChi(myPai, pai, room.getJin());
		//
		boolean isChi = false;
		if (back != null && back.size() > 0) {
			for (int[] a : back) {
				if ((a[0] == chiValue[0] && a[1] == chiValue[1]) || (a[0] == chiValue[1] && a[1] == chiValue[0])) {
					isChi = true;
					break;
				}
			}
		}
		// 处理吃事件
		if (isChi) {
			// 1.从手牌中减去2张牌
			up.removeMyPai(chiValue[0]);
			up.removeMyPai(chiValue[1]);
			// 2.记录到不可动的排
			up.addHistoryPai(1, new int[] { chiValue[0], chiValue[1], pai }, pai);
			// 3.记录到牌局记录
			room.addKaijuList(room.getPlayerIndex(account), 4, new int[] { chiValue[0], chiValue[1], pai });
			// 4.定位ThisUUID
			room.setThisType(QZMJConstant.THIS_TYPE_CHU);
			room.setThisAccount(account);
			return isChi;
		}

		return false;
	}

	public Object[] guo(String roomNo, String operateAccount) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return null;
		Object[] obj = null;
		// 获取下次询问的事件
		int nextAskType = room.getNextAskType();
		// 获取上次抓牌的人
		String lastMoAccount = room.getLastMoAccount();
		if ((nextAskType == QZMJConstant.ASK_TYPE_HU_BY_MYSELF || nextAskType == QZMJConstant.ASK_TYPE_GANG_AN)
				&& lastMoAccount.equals(operateAccount)) {
			// 获取上次摸到的牌
			int newPai = room.getLastMoPai();
			// 出牌
			obj = new Object[] { lastMoAccount, QZMJConstant.THIS_TYPE_ZUA, newPai };
		} else {
			// 获取上次出的牌
			int pai = room.getLastPai();
			// 获取上次出牌的玩家
			String chuPai = room.getLastAccount();
			// 出牌玩家的下家
			String xiajiaUUID = room.getNextPlayer(chuPai);
			String ask = null;
			JSONArray askArray = new JSONArray();
			// 设置询问事件
			nextAskType = room.getNextAskType();
			// 1.检查有无胡
			if (nextAskType == QZMJConstant.ASK_TYPE_HU_OTHER) {
				// 设置询问事件
				List<String> askHu = checkIsHuByOtherSelf(roomNo, pai);
				// 有胡
				if (askHu.size() > 0) {
					// 一家有胡
					if (askHu.size() == 1) {
						askArray.add(obtainAskResult(askHu.get(0).toString(), 7, pai));
					} else {
						// 多人有胡,返回第一个玩家
						room.setNextAskType(QZMJConstant.ASK_TYPE_HU_OTHER);
						// 下一个询问玩家
						room.setNextAskAccount(room.getNextPlayer(askHu.get(0)));
						return new Object[] { askHu.get(0), 7, pai };
					}
				}
			}
			nextAskType = room.getNextAskType();
			if (nextAskType == QZMJConstant.ASK_TYPE_GANG_MING || askArray.size() > 0) {
				// 2.检查有无杠
				ask = checkIsMingGang(roomNo, pai);
				// 有杠
				if (ask != null) {
					// 同时出现胡和杠事件
					if (askArray.size() > 0) {
						if (askArray.getJSONObject(0).get("uuid").equals(ask.toString())) {
							askArray.add(obtainAskResult(ask, 6, pai));
						} else {
							// 其他玩家有杠
							String uuid = askArray.getJSONObject(0).getString("uuid");
							room.setNextAskType(QZMJConstant.ASK_TYPE_HU_OTHER);
							// 下一个询问玩家
							room.setNextAskAccount(room.getNextPlayer(uuid));
							return new Object[] { uuid, 7, pai };
						}
					} else {
						askArray.add(obtainAskResult(ask, 6, pai));
					}
				}
			}
			nextAskType = room.getNextAskType();
			if (nextAskType == QZMJConstant.ASK_TYPE_PENG || askArray.size() > 0) {
				// 3.检查有无碰
				ask = checkIsPeng(roomNo, pai);
				// 有碰
				if (ask != null) {
					// 同时出现胡和碰事件
					if (askArray.size() > 0) {
						if (askArray.getJSONObject(0).get("uuid").equals(ask.toString())) {
							askArray.add(obtainAskResult(ask, 5, pai));
						} else {
							// 其他玩家有碰
							String uuid = askArray.getJSONObject(0).getString("uuid");
							room.setNextAskType(QZMJConstant.ASK_TYPE_HU_OTHER);
							// 下一个询问玩家
							room.setNextAskAccount(room.getNextPlayer(uuid));
							return new Object[] { uuid, 7, pai };
						}

					} else {
						askArray.add(obtainAskResult(ask, 5, pai));
					}
				}
			}

			nextAskType = room.getNextAskType();
			if (nextAskType == QZMJConstant.ASK_TYPE_CHI || (askArray.size() > 0 && xiajiaUUID.equals(operateAccount)
					&& nextAskType != QZMJConstant.ASK_TYPE_FINISH)) {
				// 4.检查有无吃
				Object[] chiback = checkIsChi(roomNo, pai, xiajiaUUID);
				// 有吃
				if (chiback != null && chiback.length == 2) {
					ask = (String) chiback[0];
					// 同时出现胡和吃碰事件
					if (askArray.size() > 0) {
						if (askArray.getJSONObject(0).get("uuid").equals(ask.toString())) {
							askArray.add(obtainAskResult(ask, 4, JSONArray.fromObject(chiback[1])));
						} else { // 其他玩家有胡杠碰事件
							room.setNextAskType(QZMJConstant.ASK_TYPE_CHI);
							room.setNextAskAccount(ask);
						}

					} else {

						return new Object[] { ask, 4, chiback[1] };
					}
				}
			}

			// 解决多事件询问问题
			if (askArray.size() > 0) {
				String uuid = askArray.getJSONObject(0).getString("uuid");
				return new Object[] { uuid, 10, askArray };
			}
			// 设置下一个焦点人
			room.setNextThisUUID();
			obj = new Object[] { null, QZMJConstant.THIS_TYPE_ZUA, null };
			return obj;
		}
		return obj;
	}

	public void buHua(String roomNo, String mPaier) {

		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		Playerinfo player = room.getPlayerMap().get(mPaier);
		QZMJUserPacket up = room.getUserPacketMap().get(mPaier);
		// 1、获取花的数量
		List<Integer> paiList = up.getMyPai();
		List<Integer> huaList = new ArrayList<Integer>();
		for (Integer pai : paiList) {
			// 如果是花牌
			if (QZMJConstant.isHuaPai(pai)) {
				huaList.add(pai);
			}
		}
		int huaCount = huaList.size();
		if (huaCount > 0) {
			room.getUserPacketMap().get(mPaier).getHuaList().addAll(huaList);
			// 2、移除花牌
			for (Integer p : huaList) {
				room.getUserPacketMap().get(mPaier).removeMyPai(p);
			}
			// 返回类型
			int backType = 1;
			// 新补的牌
			int buPai = -1;
			// 3、开始摸牌（补花）
			int[] pais = new int[huaCount];
			for (int i = 0; i < huaCount; i++) {

				int index = room.getIndex();
				int[] pai = room.getPai();
				int newpai = -1;
				if (index + QZMJConstant.LEFT_PAI_COUNT < pai.length) {
					newpai = pai[index];
					buPai = newpai;
					pais[i] = newpai;
					// 重置牌的位数
					room.setIndex(index + 1);
					// 摸牌
					room.getUserPacketMap().get(mPaier).addMyPai(newpai);
					// 摸牌时补花，设置新摸的牌为最后一张摸的牌
					if (room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_ING) {
						room.setLastMoPai(newpai);
					}

				} else {
					// 流局
					backType = 999;
				}
			}
			if (pais.length > 0) {
				// 记录补花记录
				// 0.出牌 1：抓 2.暗杠 3：自摸 4.吃 5.碰 6.明杠 7.胡 11补花
				room.addKaijuList(room.getPlayerIndex(mPaier), 11, pais);
			}
			// 4、摸牌时补花，判断是否有触发事件
			boolean isChuPai = false;

			// 摸牌次数
			int moPaiConut = room.getActionTimes(room.getPlayerIndex(mPaier), 1);
			// 补花后需要出牌
			if (!QZMJConstant.hasHuaPai(pais) && moPaiConut > 0) {

				isChuPai = true;

				JSONObject buhuaData = new JSONObject();
				buhuaData.put("huaCount", up.getHuaList().size());
				buhuaData.put("huaValue", huaList.toArray());
				buhuaData.put(QZMJConstant.lastValue, pais);
				JSONArray array = new JSONArray();
				// 判断是否胡
				boolean back = checkIsHuByMySelf(roomNo, buPai);
				if (back) {
					backType = 3;
					int yjtype = room.getUserPacketMap().get(mPaier).getYouJinIng();
					// 游金中
					if (yjtype > 0) {
						List<Integer> myPais = room.getUserPacketMap().get(mPaier).getMyPai();
						if (myPais.contains(room.getJin())) {

							int result = MaJiangCore.huPaiHasJin(myPais, 0, room.getJin(), room.getHasJinNoPingHu());
							if (result == QZMJConstant.HU_TYPE_YJ) {
								// 游金成功
								room.getUserPacketMap().get(mPaier).setYouJin(yjtype);

							} else {
								room.getUserPacketMap().get(mPaier).setYouJinIng(0);
								room.getUserPacketMap().get(mPaier).setYouJin(0);
							}
						} else {
							room.getUserPacketMap().get(mPaier).setYouJinIng(0);
							room.getUserPacketMap().get(mPaier).setYouJin(0);
						}
					}

					// 双金必游金 start
					if (getBackTypeJin(up.getMyPai(), up.getYouJinIng(), room.getHasJinNoPingHu(), room.getJin())) {
						backType = 1;
					}
					// 双金必游金 end

				} else {
					room.setNextAskType(QZMJConstant.ASK_TYPE_HU_BY_MYSELF);
				}

				// 判断是否有杠
				int[] gang = checkIsAnGang(roomNo, buPai);

				// 有杠
				if (gang[0] > 0) {
					int gangType = 0;
					// 补杠
					if (gang[0] == 1) {
						gangType = 9;
					} else if (gang[0] == 3) {
						// 暗杠
						gangType = 2;
					}
					JSONObject obj = new JSONObject();
					obj.put("type", gangType);
					obj.put("uuid", String.valueOf(room.getLastMoAccount()));
					obj.put("pai", buPai);
					obj.put("gangValue", gang[1]);
					array.add(obj);
				}

				JSONObject obj = new JSONObject();
				obj.put("type", backType);
				obj.put("uuid", String.valueOf(room.getLastMoAccount()));
				obj.put("pai", buPai);
				array.add(obj);
				detailDataByZhuaPai(array, room, buhuaData);
			}

			// 5、返回数据
			for (String uuid : room.getPlayerMap().keySet()) {
				if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
					// 当玩家是摸牌时补花，不返回补花事件（会与摸牌事件冲突）
					if (uuid.equals(mPaier) && !isChuPai) {
						JSONObject data = new JSONObject();
						data.put(QZMJConstant.zpaishu, room.getPai().length - room.getIndex());
						if (QZMJConstant.hasHuaPai(pais)) {
							data.put(QZMJConstant.type, 10);
						} else {
							data.put(QZMJConstant.type, 1);
						}
						data.put(QZMJConstant.lastType, -10);
						data.put(QZMJConstant.foucs, player.getMyIndex());
						data.put(QZMJConstant.foucsIndex, room.getFocusIndex());
						data.put(QZMJConstant.nowPoint, room.getNowPoint());
						data.put("huaCount", up.getHuaList().size());
						data.put("huaValue", huaList.toArray());
						data.put(QZMJConstant.lastValue, pais);
						CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(),
								String.valueOf(data), "gameActReultPush");
					} else if (!uuid.equals(mPaier)) {
						// 通知其他玩家
						JSONObject data = new JSONObject();
						data.put(QZMJConstant.zpaishu, room.getPai().length - room.getIndex());
						data.put(QZMJConstant.type, 1);
						data.put(QZMJConstant.lastType, -10);
						data.put(QZMJConstant.foucs, player.getMyIndex());
						data.put(QZMJConstant.foucsIndex, room.getFocusIndex());
						data.put(QZMJConstant.nowPoint, room.getNowPoint());
						data.put("huaCount", up.getHuaList().size());
						data.put("huaValue", huaList.toArray());
						CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(),
								String.valueOf(data), "gameActReultPush");
					}
				}
			}

		} else if (up.getHuaList().size() > 0) {
			// 最后一张补的花
			int hua = up.getHuaList().get(up.getHuaList().size() - 1);
			// 最后一张摸到的牌
			int pai = up.getMyPai().get(up.getMyPai().size() - 1);

			JSONObject data = new JSONObject();
			data.put(QZMJConstant.zpaishu, room.getPai().length - room.getIndex());
			data.put(QZMJConstant.type, 1);
			data.put(QZMJConstant.lastType, -10);
			data.put(QZMJConstant.foucs, player.getMyIndex());
			data.put(QZMJConstant.foucsIndex, room.getFocusIndex());
			data.put(QZMJConstant.nowPoint, room.getNowPoint());
			data.put(QZMJConstant.lastValue, new int[pai]);
			data.put("huaCount", up.getHuaList().size());
			data.put("huaValue", new int[hua]);
			CommonConstant.sendMsgEventToSingle(player.getUuid(), String.valueOf(data), "gameActReultPush");
		}

	}

	public static JSONObject autoBuHua(String roomNo, String mPaier) {

		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return null;
		QZMJUserPacket up = room.getUserPacketMap().get(mPaier);

		// 1、获取花的数量
		List<Integer> paiList = up.getMyPai();
		List<Integer> huaList = new ArrayList<Integer>();
		for (Integer pai : paiList) {
			// 如果是花牌
			if (QZMJConstant.isHuaPai(pai)) {
				huaList.add(pai);
			}
		}
		int huaCount = huaList.size();
		if (huaCount > 0) {
			room.getUserPacketMap().get(mPaier).getHuaList().addAll(huaList);
			// 2、移除花牌
			for (Integer p : huaList) {
				room.getUserPacketMap().get(mPaier).removeMyPai(p);
			}
			// 3、开始摸牌（补花）
			int[] pais = new int[huaCount];
			for (int i = 0; i < huaCount; i++) {
				changePai(roomNo, mPaier);
				int index = room.getIndex();
				int[] pai = room.getPai();
				int newpai = -1;
				if (index + QZMJConstant.LEFT_PAI_COUNT < pai.length) {
					newpai = pai[index];
					pais[i] = newpai;
					// 重置牌的位数
					room.setIndex(index + 1);
					// 摸牌
					room.getUserPacketMap().get(mPaier).addMyPai(newpai);
					// 摸牌时补花，设置新摸的牌为最后一张摸的牌
					if (room.getGameStatus() == room.getGameStatus()) {
						room.setLastMoPai(newpai);
					}
				}
			}
			if (pais.length > 0) {
				// 记录补花记录
				// 0.出牌 1：抓 2.暗杠 3：自摸 4.吃 5.碰 6.明杠 7.胡 11补花
				room.addKaijuList(room.getPlayerIndex(mPaier), 11, pais);
			}
			// 4、返回数据
			JSONObject data = new JSONObject();
			if (QZMJConstant.hasHuaPai(pais)) {
				data.put(QZMJConstant.type, 10);
			} else {
				data.put(QZMJConstant.type, 1);
			}
			data.put("huaCount", up.getHuaList().size());
			data.put("huaValue", huaList.toArray());
			data.put(QZMJConstant.lastValue, pais);
			return data;
		}

		return null;
	}

	public void buHuaByMoPai(String roomNo, String mPaier) {

		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		JSONArray huavals = new JSONArray();
		int buhuaType = 10;
		int buhuaCount = 0;
		// 需要补花
		while (buhuaType == 10) {
			JSONObject data = autoBuHua(roomNo, mPaier);
			if (data != null) {
				buhuaType = data.getInt("type");
				huavals.add(data);
			}
			buhuaCount++;
		}
		// 通知玩家补花结果
		if (buhuaType != 10) {
			// 通知其他玩家的补花结果
			JSONArray otherBuHua = new JSONArray();
			for (int i = 0; i < huavals.size(); i++) {
				JSONObject obj = huavals.getJSONObject(i);
				JSONObject obuhua = new JSONObject();
				obuhua.put("huaValue", obj.get("huaValue"));
				otherBuHua.add(obuhua);
			}

			for (String uuid : room.getPlayerMap().keySet()) {
				if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
					JSONObject buhua = new JSONObject();
					buhua.put("index", room.getPlayerMap().get(mPaier).getMyIndex());
					buhua.put("zpaishu", room.getPai().length - room.getIndex());

					if (mPaier.equals(uuid)) {
						buhua.put("huavals", huavals);
					} else {
						buhua.put("huavals", otherBuHua);
					}
					CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(), String.valueOf(buhua),
							"gameBuHuaPush");
				}
			}
		}

		// 补一次花延迟800ms，让补花动画播完
//        try {
//            Thread.sleep(800 * buhuaCount);
//        } catch (InterruptedException e1) {
//            e1.printStackTrace();
//        }

		// 判断是否触发事件
		JSONArray array = new JSONArray();
		// 返回类型
		int backType = 1;
		int buPai = room.getLastMoPai();
		if (room.getIndex() + QZMJConstant.LEFT_PAI_COUNT < room.getPai().length) {
			// 判断是否胡
			boolean back = checkIsHuByMySelf(roomNo, buPai);
			if (back) {
				backType = 3;
				int yjtype = room.getUserPacketMap().get(mPaier).getYouJinIng();
				// 游金中
				if (yjtype > 0) {
					List<Integer> myPais = room.getUserPacketMap().get(mPaier).getMyPai();
					if (myPais.contains(room.getJin())) {
						int result = MaJiangCore.huPaiHasJin(myPais, 0, room.getJin(), room.getHasJinNoPingHu());
						if (result == QZMJConstant.HU_TYPE_YJ) {
							// 游金成功
							room.getUserPacketMap().get(mPaier).setYouJin(yjtype);
						} else {
							room.getUserPacketMap().get(mPaier).setYouJinIng(0);
							room.getUserPacketMap().get(mPaier).setYouJin(0);
						}
					} else {
						room.getUserPacketMap().get(mPaier).setYouJinIng(0);
						room.getUserPacketMap().get(mPaier).setYouJin(0);
					}
				}
				// 双金必游金 start
				if (getBackTypeJin(room.getUserPacketMap().get(mPaier).getMyPai(),
						room.getUserPacketMap().get(mPaier).getYouJinIng(), room.getHasJinNoPingHu(), room.getJin())) {
					backType = 1;
				}
				// 双金必游金 end
			} else {
				room.setNextAskType(QZMJConstant.ASK_TYPE_HU_BY_MYSELF);
			}
			// 判断是否有杠
			int[] gang = checkIsAnGang(roomNo, buPai);

			if (gang[0] > 0) {
				// 有杠
				int gangType = 0;
				if (gang[0] == 1) {
					// 补杠
					gangType = 9;
				} else if (gang[0] == 3) {
					// 暗杠
					gangType = 2;
				}
				JSONObject obj = new JSONObject();
				obj.put("type", gangType);
				obj.put("uuid", String.valueOf(room.getLastMoAccount()));
				obj.put("pai", buPai);
				obj.put("gangValue", gang[1]);
				array.add(obj);
			}
		} else {
			backType = 999;
		}
		JSONObject obj = new JSONObject();
		obj.put("type", backType);
		obj.put("uuid", String.valueOf(room.getLastMoAccount()));
		obj.put("pai", buPai);
		array.add(obj);
		detailDataByZhuaPai(array, room, null);
	}

	public JSONArray autoMoPai(String roomNo, String account) {
		JSONArray mjieguo = moPai(roomNo, account);
		detailDataByZhuaPai(mjieguo, (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo), null);
		return null;
	}

	public JSONArray moPai(String roomNo, String moPaiUUID) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return null;
		JSONArray array = new JSONArray();
		int backType = -1;
		int newpai = -1;
		newpai = mopai(roomNo, moPaiUUID);
		String uuid = room.getLastMoAccount();
		// 流局
		if (newpai < 0) {
			backType = 999;
		} else if (QZMJConstant.isHuaPai(newpai)) {
			// 判断是否抓到花牌
			backType = 10;
		} else {
			backType = 1;
			boolean back = false;
			int type = room.getNextAskType();

			if ((room.getYjType() > 2 && room.getUserPacketMap().get(uuid).getYouJinIng() < 3)) {
				// 三游不能自摸，只能杠上摸
				room.setNextAskType(QZMJConstant.ASK_TYPE_HU_BY_MYSELF);
			} else if ((room.isGuangYou && room.hasYouJinType(2)
					&& room.getUserPacketMap().get(uuid).getYouJinIng() < 2)) {
				// 光游时的双游不能自摸，只能杠上摸
				room.setNextAskType(QZMJConstant.ASK_TYPE_HU_BY_MYSELF);
			} else {
				// 判断是否自摸
				if (type == QZMJConstant.THIS_TYPE_ZUA) {
					back = checkIsHuByMySelf(roomNo, newpai);
					if (back) {
						backType = 3;
						int yjtype = room.getUserPacketMap().get(uuid).getYouJinIng();
						List<Integer> myPais = room.getUserPacketMap().get(uuid).getMyPai();
						// 游金中
						if (yjtype > 0) {
							if (myPais.contains(room.getJin())) {
								int result = MaJiangCore.huPaiHasJin(myPais, 0, room.getJin(),
										room.getHasJinNoPingHu());
								if (result == QZMJConstant.HU_TYPE_YJ) {
									// 游金成功
									room.getUserPacketMap().get(uuid).setYouJin(yjtype);
								} else {
									room.getUserPacketMap().get(uuid).setYouJinIng(0);
									room.getUserPacketMap().get(uuid).setYouJin(0);
								}
							} else {
								room.getUserPacketMap().get(uuid).setYouJinIng(0);
								room.getUserPacketMap().get(uuid).setYouJin(0);
							}
						}
						// 双金必游金 start
						if (getBackTypeJin(myPais, yjtype, room.getHasJinNoPingHu(), room.getJin())) {
							backType = 1;
						}
						// 双金必游金 end

					}
				}
			}
			// 判断是否触发杠事件
			int[] gang = checkIsAnGang(roomNo, newpai);
			if (gang[0] > 0) {
				// 有杠
				int gangType = 0;
				if (gang[0] == 1) {
					// 补杠
					gangType = 9;
				} else if (gang[0] == 3) {
					// 暗杠
					gangType = 2;
				}
				JSONObject obj = new JSONObject();
				obj.put("type", gangType);
				obj.put("uuid", uuid);
				obj.put("pai", newpai);
				System.out.println("======返回对手牌");
				String duishou = "";
				for (String account2 : room.getPlayerMap().keySet()) {
					if (!uuid.equals(account2)) {
						duishou = account2;
						break;
					}
				}
				Object[] duishouPai = room.getUserPacketMap().get(duishou).getMyPai().toArray();

				obj.put("duishouPai", duishouPai);
				obj.put("gangValue", gang[1]);

				array.add(obj);
			}
		}
		JSONObject obj = new JSONObject();
		obj.put("type", backType);
		obj.put("uuid", String.valueOf(room.getLastMoAccount()));
		obj.put("pai", newpai);
		array.add(obj);

		return array;
	}

	private boolean getBackTypeJin(List<Integer> myPai, int youJinIng, int hasJinNoPingHu, int jin) {
		if (0 == youJinIng && QZMJConstant.HAS_JIN_NO_PING_HU_2 == hasJinNoPingHu) {// 双金必游金
			int jinCout = 0;// 金牌数量
			for (Integer i : myPai) {
				if (i == jin) {
					jinCout++;
				}
			}

			if (jinCout >= 2) {
				return true;
			}
		}
		return false;
	}

	public int mopai(String roomNo, String moPaiUUID) {

		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return -1;
		int newpai = -1;
		// 获取本次应该出牌的人
		changePai(roomNo, moPaiUUID);
		int index = room.getIndex();
		int[] pai = room.getPai();

		if (index < pai.length - QZMJConstant.LEFT_PAI_COUNT) {

			// 获取摸牌人
			String mopai = moPaiUUID;
			int paiCount = room.getUserPacketMap().get(mopai).getMyPai().size();
			if (paiCount <= room.getPaiCount()) {
				newpai = pai[index];
				// 重置牌的位数
				room.setIndex(index + 1);
				// 摸牌
				room.getUserPacketMap().get(mopai).addMyPai(newpai);
				// 设置询问人
				room.setNextAskAccount(mopai);
				// 设置询问事件
				room.setNextAskType(QZMJConstant.THIS_TYPE_ZUA);
				// 设置最新被摸的牌
				room.setLastMoPai(newpai);
				// 设置最新摸牌的人
				room.setLastMoAccount(mopai);
				// 1.抓牌（出牌） 2.暗杠 3：自摸 4.吃 5.碰 6.明杠 7.胡
				// 记录摸牌记录
				room.addKaijuList(room.getPlayerIndex(mopai), 1, new int[] { newpai });
			} else {
				return room.getLastMoPai();
			}
		}

		return newpai;
	}

	private static void changePai(String roomNo, String moPaiUUID) {
		try {
			QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
			if (null == room)
				return;
			QZMJUserPacket up = room.getUserPacketMap().get(moPaiUUID);
			int index = room.getIndex();
			int[] pai = room.getPai();
			// 当前的牌
			int curPai = pai[index];
			// 是否有杠
			List<DontMovePai> penghistory = up.getPengList();
			int[] back = MaJiangCore.isGang(up.getMyPai(), curPai, 1, penghistory);
			// 牌下标
			int changeIndex = QZMJConstant.ALL_PAI.length - QZMJConstant.LEFT_PAI_COUNT;
			// 有杠
			while (back[0] > 0 && changeIndex < QZMJConstant.ALL_PAI.length) {
				boolean hu = false;
				// 取得当前的牌是否能有人胡牌
				for (String player : room.getUserPacketMap().keySet()) {
					if (!player.equals(moPaiUUID) && MaJiangCore.isHu(room.getUserPacketMap().get(player).getMyPai(),
							curPai, room.getJin(), room.getHasJinNoPingHu())) {
						hu = true;
						break;
					}
				}
				if (hu) {
					// 取非金牌
					int newPai = pai[changeIndex];
					while (newPai == room.getJin()) {
						changeIndex++;
						newPai = pai[changeIndex];
					}
					// 交换
					pai[index] = newPai;
					pai[changeIndex] = curPai;
					changeIndex++;
					room.setPai(pai);
					// 重新赋值
					curPai = pai[index];
					back = MaJiangCore.isGang(up.getMyPai(), curPai, 1, penghistory);
				} else {
					back = new int[] { 0 };
				}
			}

		} catch (Exception e) {
			logger.error("", e);
		}
	}

	public Object[] chuPai(String roomNo, int pai, String chupaiId) {
		Object[] obj = null;
		// 出牌
		boolean back = chupai(roomNo, pai, chupaiId);
		// 出牌成功
		if (back) {
			// 通知前端出牌事件
			QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
			if (null == room)
				return null;
			// 当前游金状态
			int yjtype = room.getUserPacketMap().get(chupaiId).getYouJinIng();
			// 获取出牌玩家的下家uuid
			String xiajiaUUID = room.getNextPlayer(chupaiId);
			// 判断是否可以游金
			List<Integer> myPais = room.getUserPacketMap().get(chupaiId).getMyPai();
			// 开始游金
			if (myPais.contains(room.getJin())) {
				List<Integer> paiList = new ArrayList<Integer>(myPais);
				JSONObject result = MaJiangCore.youJinAndTingPaiPanDing(paiList, room.getJin(), null);
				if (result != null && result.getInt("type") == 1) {
					// 设置当前游金状态
					if (yjtype == 0) {
						room.getUserPacketMap().get(chupaiId).setYouJinIng(1);
						if (room.getUserPacketMap().get(chupaiId).getYouJinIng() > room.getYjType()) {
							room.setYjType(room.getUserPacketMap().get(chupaiId).getYouJinIng());
							room.setYjAccount(chupaiId);
						}
					} else if (yjtype > 0) {
						// 打出金牌
						if (room.getJin() == pai) {
							if (yjtype == 1) {
								room.getUserPacketMap().get(chupaiId).setYouJinIng(2);
							} else if (yjtype == 2) {
								room.getUserPacketMap().get(chupaiId).setYouJinIng(3);
							}

							if (room.getUserPacketMap().get(chupaiId).getYouJinIng() > room.getYjType()) {
								room.setYjType(room.getUserPacketMap().get(chupaiId).getYouJinIng());
								room.setYjAccount(chupaiId);
							}
						} else { // 单游
							room.getUserPacketMap().get(chupaiId).setYouJinIng(1);
							room.getUserPacketMap().get(chupaiId).setYouJin(0);
							if (room.getUserPacketMap().equals(chupaiId)) {
								room.setYjType(1);
							}
						}
					}
				} else {
					room.getUserPacketMap().get(chupaiId).setYouJinIng(0);
					room.getUserPacketMap().get(chupaiId).setYouJin(0);
				}
			} else {
				room.getUserPacketMap().get(chupaiId).setYouJinIng(0);
				room.getUserPacketMap().get(chupaiId).setYouJin(0);
			}

			/// 通知前台展示玩家出的牌
			// 总牌数
			int zpaishu = room.getPai().length - room.getIndex();
			// 获取之前事件
			int foucs = room.getPlayerIndex(room.getLastAccount());
			// 出牌玩家当前游金状态
			yjtype = room.getUserPacketMap().get(chupaiId).getYouJinIng();
			for (String uuid : room.getPlayerMap().keySet()) {
				if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
					JSONObject backObj = new JSONObject();
					backObj.put(QZMJConstant.zpaishu, zpaishu);
					backObj.put(QZMJConstant.foucs, foucs);
					backObj.put(QZMJConstant.foucsIndex, room.getFocusIndex());
					backObj.put(QZMJConstant.nowPoint, room.getNowPoint());
					backObj.put(QZMJConstant.lastValue, pai);
					String duishou = "";
					for (String account2 : room.getPlayerMap().keySet()) {
						if (!uuid.equals(account2)) {
							duishou = account2;
							break;
						}
					}
					Object[] duishouPai = room.getUserPacketMap().get(duishou).getMyPai().toArray();
					backObj.put("duishouPai", duishouPai);

					// 出牌牌面展示
					backObj.put(QZMJConstant.type, new int[] { 11 });
					// 光游时直接返回游金类型
					if (uuid.equals(chupaiId) || room.isGuangYou) {
						backObj.put("youjin", yjtype);
					} else {
						// 暗游时，只有双游以上才通知其他人
						if (yjtype > 1) {
							// 通知其他玩家正在双游、三游
							backObj.put("youjin", yjtype);
						} else {
							backObj.put("youjin", 0);
						}
					}
					CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(),
							String.valueOf(backObj), "gameChupaiPush");
				}
			}

			// 延迟1000ms播放出牌动画
//            try {
//                Thread.sleep(600);
//            } catch (InterruptedException e1) {
//                e1.printStackTrace();
//            }

			// 是否可以吃碰杠胡
			boolean isCanChiPengGangHu = true;

			if (room.getYjType() >= 2 || (room.isGuangYou && room.hasYouJinType(1))) {

				isCanChiPengGangHu = false;
			}

			// 判断是否有玩家在双游，三游
			if (isCanChiPengGangHu) {

				JSONArray askArray = new JSONArray();

				// 1.检查有无胡
				String ask = null;
				List<String> askHu = checkIsHuByOtherSelf(roomNo, pai);
				// 有胡
				if (askHu.size() > 0) {

					// 下家当前游金状态
					int youjining = room.getUserPacketMap().get(xiajiaUUID).getYouJinIng();
					// 一家有胡
					if (askHu.size() == 1) {
						if (!askHu.get(0).equals(xiajiaUUID) || youjining <= 0) {
							askArray.add(obtainAskResult(askHu.get(0), 7, pai));
						}
					} else {
						// 多人有胡,返回第一个玩家
						room.setNextAskType(QZMJConstant.ASK_TYPE_HU_OTHER);
						// 当上家出的牌下家可以胡，且下家处于游金状态，则直接过。
						if (askHu.get(0).equals(xiajiaUUID) && youjining > 0) {
							// 下一个询问玩家
							room.setNextAskAccount(room.getNextPlayer(askHu.get(1)));
							return new Object[] { askHu.get(1), 7, pai };
						}
						// 下一个询问玩家
						room.setNextAskAccount(room.getNextPlayer(askHu.get(0)));
						return new Object[] { askHu.get(0), 7, pai };
					}
				}
				// 从下家开始询问
				room.setNextAskAccount(xiajiaUUID);
				// 2.检查有无杠
				ask = checkIsMingGang(roomNo, pai);
				// 有杠
				if (ask != null) {
					// 同时出现胡和杠事件
					if (askArray.size() > 0) {
						if (askArray.getJSONObject(0).get("uuid").equals(ask.toString())) {
							askArray.add(obtainAskResult(ask, 6, pai));
						} else { // 其他玩家有杠
							String uuid = askArray.getJSONObject(0).getString("uuid");
							room.setNextAskType(QZMJConstant.ASK_TYPE_HU_OTHER);
							// 下一个询问玩家
							room.setNextAskAccount(room.getNextPlayer(uuid));
							return new Object[] { uuid, 7, pai };
						}

					} else {
						askArray.add(obtainAskResult(ask, 6, pai));
					}
				}

				// 从下家开始询问
				room.setNextAskAccount(xiajiaUUID);
				// 3.检查有无碰
				ask = checkIsPeng(roomNo, pai);
				if (ask != null) {// 有碰
					// 同时出现胡和碰事件
					if (askArray.size() > 0) {
						if (askArray.getJSONObject(0).get("uuid").equals(ask.toString())) {
							askArray.add(obtainAskResult(ask, 5, pai));
						} else {
							// 其他玩家有碰
							String uuid = askArray.getJSONObject(0).getString("uuid");
							room.setNextAskType(QZMJConstant.ASK_TYPE_HU_OTHER);
							// 下一个询问玩家
							room.setNextAskAccount(room.getNextPlayer(uuid));
							return new Object[] { uuid, 7, pai };
						}
					} else {
						askArray.add(obtainAskResult(ask, 5, pai));
					}
				}

				// 从下家开始询问
				room.setNextAskAccount(xiajiaUUID);

				// 4.检查有无吃
				Object[] chiback = checkIsChi(roomNo, pai, xiajiaUUID);
				// 有吃
				if (chiback != null && chiback.length == 2) {
					ask = (String) chiback[0];
					// 同时出现胡和吃碰事件
					if (askArray.size() > 0) {
						if (askArray.getJSONObject(0).get("uuid").equals(ask.toString())) {
							askArray.add(obtainAskResult(ask, 4, JSONArray.fromObject(chiback[1])));
						} else { // 其他玩家有胡杠碰事件
							room.setNextAskType(QZMJConstant.ASK_TYPE_CHI);
							room.setNextAskAccount(ask);
						}
					} else {
						return new Object[] { ask, 4, chiback[1] };
					}
				}
				// 解决多事件询问问题
				if (askArray.size() > 0) {
					String uuid = askArray.getJSONObject(0).getString("uuid");
					return new Object[] { uuid, 10, askArray };
				}
				// 获取上次thisUUID，thisType
				if (room.getPlayerMap().get(room.getThisAccount()) == null) {
					room.setThisAccount(chupaiId);
				}
				// 设置下一个焦点人
				room.setNextThisUUID();
				obj = new Object[] { null, QZMJConstant.THIS_TYPE_ZUA, null };
				return obj;
			} else {
				// 获取上次thisUUID，thisType
				if (room.getPlayerMap().get(room.getThisAccount()) == null) {
					room.setThisAccount(chupaiId);
				}
				// 设置下一个焦点人
				room.setNextThisUUID();
				obj = new Object[] { null, QZMJConstant.THIS_TYPE_ZUA, null };
				return obj;
			}
		}
		return obj;
	}

	public boolean chupai(String roomNo, int oldPai, String chupaiId) {

		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return false;
		boolean back = room.getUserPacketMap().get(chupaiId).removeMyPai(oldPai);
		if (back) {
			// 下次需要判断的人
			String nextAsk = room.getNextPlayer(chupaiId);
			// 设置下次询问的人
			room.setNextAskAccount(nextAsk);
			// 设置下次询问的事件
			room.setNextAskType(QZMJConstant.ASK_TYPE_HU_OTHER);
			// 设置上次出的牌
			room.setLastPai(oldPai);
			// 设置出牌人
			room.setLastAccount(chupaiId);
			// 记录出牌记录
			room.addKaijuList(room.getPlayerIndex(chupaiId), 0, new int[] { oldPai });
			return true;
		}

		return false;
	}

	public JSONObject obtainAskResult(String askAccount, int type, Object obj) {
		JSONObject askResult = new JSONObject();
		askResult.put("uuid", String.valueOf(askAccount));
		askResult.put("type", type);
		askResult.put("value", obj);
		return askResult;
	}

	public boolean detailDataByZhuaPai(JSONArray mjieguo, QZMJGameRoom room, JSONObject buhuaData) {
		// 抓牌的人
		String zhuaID = null;
		// 抓牌人触发的事件
		int type = 0;
		// 摸的牌
		int mopai = 0;
		int gangType = 0;
		int gangValue = 0;

		if (mjieguo.size() > 0) {
			zhuaID = mjieguo.getJSONObject(0).getString("uuid");
			mopai = mjieguo.getJSONObject(0).getInt("pai");
			// 事件优先级：流局->补花->胡->杠
			if (mjieguo.size() == 1) {
				// 没有杠事件
				type = mjieguo.getJSONObject(0).getInt("type");
			} else if (mjieguo.getJSONObject(0).containsKey("gangValue")) {
				// 包含杠事件
				gangType = mjieguo.getJSONObject(0).getInt("type");
				gangValue = mjieguo.getJSONObject(0).getInt("gangValue");
				// 普通事件
				type = mjieguo.getJSONObject(1).getInt("type");
			}
		}
		// 流局，结束当局游戏
		if (type == 999) {
			liuJu(room.getRoomNo());
		} else {
			boolean hasHua = false;
			if (type == 10) {
				hasHua = true;
				gangType = 0;
			}
			// 本次暗杠
			int[] anvalue = null;
			// 本次抓杠
			int[] zhuagangvalue = null;
			if (gangType == 2) {
				anvalue = new int[] { gangValue, gangValue, gangValue, gangValue };
			}
			if (gangType == 9) {
				zhuagangvalue = new int[] { gangValue, gangValue, gangValue, gangValue };
				anvalue = new int[] { gangValue, gangValue, gangValue, gangValue };
			}
			// 总牌数
			int zpaishu = room.getPai().length - room.getIndex();
			// 本次焦点人
			int foucs = room.getPlayerIndex(zhuaID);
			int lastFoucs = room.getPlayerIndex(room.getLastAccount());
			Integer lastType = null;
			Object lastValue = null;
			Object lastAnValue = null;
			// 获取之前事件
			KaiJuModel jilu = room.getLastValue();
			if (jilu != null) {
				if (jilu.getType() == 2) {
					// 暗杠
					lastType = 2;
					lastAnValue = new int[] { jilu.getValues()[0] };
				} else if (jilu.getType() == 9) {
					// 抓杠
					lastType = 9;
					lastValue = new int[] { jilu.getValues()[0] };
				} else if (jilu.getType() == 6) {
					// 明杠
					lastType = 6;
					lastValue = new int[] { jilu.getValues()[0] };
				} else if (jilu.getType() == 1) {
					// 出牌
					lastType = 1;
					lastValue = jilu.getValues();
				}
			}
			// 通知所有的玩家
			String next = zhuaID;
			do {
				SocketIOClient client = GameMain.server.getClient(room.getPlayerMap().get(next).getUuid());
				JSONObject back = new JSONObject();
				back.put(QZMJConstant.zpaishu, zpaishu);
				back.put(QZMJConstant.foucs, foucs);
				back.put(QZMJConstant.foucsIndex, room.getFocusIndex());
				back.put(QZMJConstant.nowPoint, room.getNowPoint());
				back.put(QZMJConstant.lastFoucs, lastFoucs);
				// 获取抓杠位置
				int zgindex = room.getUserPacketMap().get(zhuaID).buGangIndex(mopai);
				back.put(QZMJConstant.zgindex, zgindex);
				back.put(QZMJConstant.lastType, lastType);
				back.put(QZMJConstant.lastValue, lastValue);
				if (next.equals(zhuaID)) {
					// 返回事件询问类型
					if (type == 3 && gangType > 0) {
						back.put(QZMJConstant.type, new int[] { type, gangType });
					} else if (type == 1 && gangType > 0) {
						back.put(QZMJConstant.type, new int[] { gangType });
					} else {
						back.put(QZMJConstant.type, new int[] { type });
					}
					if (!back.containsKey(QZMJConstant.lastValue)) {
						back.put(QZMJConstant.lastValue, lastAnValue);
					}
					// 胡牌类型
					if (type == 3) {
						QZMJUserPacket up = room.getUserPacketMap().get(zhuaID);
						if (up.getMyPai().contains(room.getJin())) {

							int huType = MaJiangCore.huPaiHasJin(up.getMyPai(), 0, room.getJin(),
									room.getHasJinNoPingHu());
							// 满足双、三游条件
							int canYouJin = 0;
							// 当玩家有游金时判断是几游
							if (huType == QZMJConstant.HU_TYPE_YJ) {

								if (up.getYouJin() == 1) {
									huType = QZMJConstant.HU_TYPE_YJ;
									if (mopai == room.getJin() || MaJiangCore.shuangSanYouPanDing(up.getMyPai(), 0,
											room.getJin(), room.getHasJinNoPingHu())) {
										// 可以开始双游
										canYouJin = 2;
									}
								} else if (up.getYouJin() == 2) {
									huType = QZMJConstant.HU_TYPE_SHY;
									if (mopai == room.getJin() || MaJiangCore.shuangSanYouPanDing(up.getMyPai(), 0,
											room.getJin(), room.getHasJinNoPingHu())) {
										// 可以开始三游
										canYouJin = 3;
									}
								} else if (up.getYouJin() == 3) {
									huType = QZMJConstant.HU_TYPE_SY;
								} else {
									// 三金倒
									if (QZMJConstant.HAS_JIN_NO_PING_HU_2 != room.getHasJinNoPingHu()// 不能三金倒
											&& up.getPlayerJinCount(room.getJin()) == 3) {
										huType = QZMJConstant.HU_TYPE_SJD;
									} else {
										huType = QZMJConstant.HU_TYPE_ZM;
									}
								}
							}

							// 判断是否是天胡
							if (huType == QZMJConstant.HU_TYPE_ZM) {

								List<KaiJuModel> paiJuList = room.getKaiJuList();
								// 摸牌次数
								int moPaiConut = 0;
								if (paiJuList != null && paiJuList.size() < 10) {
									for (KaiJuModel kaiJu : paiJuList) {
										if (kaiJu.getType() == 1) {
											moPaiConut++;
										}
									}
									if (moPaiConut <= 1) {
										huType = QZMJConstant.HU_TYPE_TH;
									}
								}
							}

							back.put("huType", huType);
							back.put("youjin", canYouJin);

						} else {
							back.put("huType", QZMJConstant.HU_TYPE_ZM);
							back.put("youjin", 0);

							String duishou = "";

							for (String account2 : room.getPlayerMap().keySet()) {
								if (!zhuaID.equals(account2)) {
									duishou = account2;
									break;
								}
							}
							Object[] duishouPai = room.getUserPacketMap().get(duishou).getMyPai().toArray();
							back.put("duishouPai", duishouPai);
						}
					}
					// 出牌或者胡
					if (type == 1 || type == 3) {
						// 牌局中剩余牌数（包含其他玩家手牌）
						List<Integer> shengyuList = new ArrayList<Integer>(
								Dto.arrayToList(room.getPai(), room.getIndex()));
						for (String uuid : room.getPlayerMap().keySet()) {
							if (room.getUserPacketMap().containsKey(uuid)
									&& room.getUserPacketMap().get(uuid) != null) {
								if (!zhuaID.equals(uuid)) {
									shengyuList.addAll(room.getUserPacketMap().get(uuid).getMyPai());
								}
							}
						}

						// 出牌提示
						JSONArray tingTip = MaJiangCore.tingPaiTip(room.getUserPacketMap().get(zhuaID).getMyPai(),
								room.getJin(), shengyuList);
						back.put("tingTip", tingTip);
						back.put("compensateList",
								MaJiangCore.getCompensateList(room.getUserPacketMap().get(zhuaID).getMyPai(),
										getOutList(room.getRoomNo()), room.getJin(), tingTip));
					}
					back.put(QZMJConstant.gangvalue, anvalue);
					back.put(QZMJConstant.value, new int[] { mopai });

					// 补花触发事件
					if (buhuaData != null) {
						back.put("lastType", -10);
						back.put("huaCount", buhuaData.get("huaCount"));
						back.put("huaValue", buhuaData.get("huaValue"));
						back.put("lastValue", buhuaData.get("lastValue"));
					}
					if (anvalue != null && anvalue.length > 0) {
						sendGameEventResult(room, next, back, 1);
					} else {
						CommonConstant.sendMsgEventToSingle(client, String.valueOf(back), "gameChupaiPush");
						if (room.isRobot() && room.getRobotList().contains(next) && !hasHua) {
							int delayTime = RandomUtils.nextInt(3) + 2;
							AppBeanUtil.getBean(RobotEventDeal.class).changeRobotActionDetail(next,
									QZMJConstant.QZMJ_GAME_EVENT_CP, delayTime);
						} else if (!hasHua) {
							beginGameEventTimer(room.getRoomNo(), next, 0, QZMJConstant.QZ_MJ_TIMER_TYPE_CP);
						}
					}

				} else {
					back.put(QZMJConstant.gangvalue, zhuagangvalue);
					back.put(QZMJConstant.type, new int[] { 1 });
					back.put(QZMJConstant.value, null);
					back.put(QZMJConstant.lastValue, lastValue);
					CommonConstant.sendMsgEventToSingle(client, String.valueOf(back), "gameChupaiPush");
				}
				next = room.getNextPlayer(next);
			} while (!zhuaID.equals(next));

			// 摸牌时触发补花
			if (hasHua) {
				buHuaByMoPai(room.getRoomNo(), zhuaID);
			}
		}
		return true;
	}

	public boolean detailDataByChuPai(Object[] jieguo, String roomNo) {
		if (Dto.isNull(jieguo)) {
			return false;
		}
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room) {
			return false;
		}
		String thisask = (String) jieguo[0];
		int type = Integer.valueOf(String.valueOf(jieguo[1]));
		// 最后出的牌
		int pai = room.getLastPai();
		// 上次出牌的人
		String chupai = room.getLastAccount();
		// 获取总牌数
		int zpaishu = room.getPai().length - room.getIndex();
		// 获取lastType
		int lastType = 1;
		// 获取lastValue
		int[] lastValue = new int[] { pai };
		int lastFoucs = room.getLastFocus();
		// 下家抓牌事件，并通知所有人
		if (type == 1) {
			JSONArray mjieguo = moPai(roomNo, room.getThisAccount());
			detailDataByZhuaPai(mjieguo, room, null);
		} else {
			JSONObject result = new JSONObject();
			result.put(QZMJConstant.zpaishu, zpaishu);
			result.put(QZMJConstant.value, null);
			result.put(QZMJConstant.lastType, lastType);
			result.put(QZMJConstant.lastValue, lastValue);
			result.put(QZMJConstant.lastFoucs, lastFoucs);
			result.put(QZMJConstant.foucs, room.getPlayerIndex(thisask));
			result.put(QZMJConstant.foucsIndex, room.getPlayerIndex(chupai));
			result.put(QZMJConstant.nowPoint, room.getNowPoint());
			result.put(QZMJConstant.type, new int[] { type });

			if (type == 7) {
				// 胡 询问事件
				result.put(QZMJConstant.huvalue, jieguo[2]);
			} else if (type == 6) {
				// 杠 询问事件
				result.put(QZMJConstant.gangvalue, new Object[] { pai, pai, pai, pai });
			} else if (type == 5) {
				// 碰 询问事件
				result.put(QZMJConstant.pengvalue, new int[] { pai, pai, pai });
			} else if (type == 4) {
				// 吃 询问事件
				result.put(QZMJConstant.chivalue, jieguo[2]);
			} else if (type == 10) {
				// 触发多事件询问
				JSONArray array = JSONArray.fromObject(jieguo[2]);
				int[] types = new int[array.size()];
				for (int i = 0; i < array.size(); i++) {
					JSONObject obj = array.getJSONObject(i);
					if (obj.getInt("type") == 7) {
						result.put(QZMJConstant.huvalue, pai);
					} else if (obj.getInt("type") == 6) {
						result.put(QZMJConstant.gangvalue, new Object[] { pai, pai, pai, pai });
					} else if (obj.getInt("type") == 5) {
						result.put(QZMJConstant.pengvalue, new int[] { pai, pai, pai });
					} else if (obj.getInt("type") == 4) {
						result.put(QZMJConstant.chivalue, obj.get("value"));
					}
					types[i] = obj.getInt("type");
				}
				result.put(QZMJConstant.type, types);
			}
			sendGameEventResult(room, thisask, result, 1);
		}
		return true;
	}

	public void detailDataByGuo(Object[] jieguo, String roomNo) {

		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		String thisask = (String) jieguo[0];
		// 1.抓/出 2.暗杠 9.抓明杠 7.胡 6.杠 5.碰 4.吃
		int type = Integer.valueOf(String.valueOf(jieguo[1]));
		// 最后出的牌
		int pai = room.getLastPai();
		// 获取总牌数
		int zpaishu = room.getPai().length - room.getIndex();
		// 获取lastType
		int lastType = 1;
		// 获取lastValue
		int[] lastValue = new int[] { pai };
		// 组织数据
		JSONObject obj = new JSONObject();
		obj.put(QZMJConstant.zpaishu, zpaishu);
		obj.put(QZMJConstant.value, null);
		obj.put(QZMJConstant.lastType, lastType);
		obj.put(QZMJConstant.lastValue, lastValue);
		obj.put(QZMJConstant.lastFoucs, room.getPlayerIndex(room.getLastAccount()));
		obj.put(QZMJConstant.foucs, room.getPlayerIndex(thisask));
		obj.put(QZMJConstant.foucsIndex, room.getFocusIndex());
		obj.put(QZMJConstant.nowPoint, room.getNowPoint());
		obj.put(QZMJConstant.type, new int[] { type });
		if (1 == type)
			room.setTooNextAskAccount(null);// 清空 过记录
		if (type == 7) {
			// 胡 询问事件
			JSONObject ishu = obj;
			ishu.put(QZMJConstant.huvalue, jieguo[2]);
			sendGameEventResult(room, thisask, ishu, type);
		} else if (type == 6) {
			// 杠 询问事件
			JSONObject isgang = obj;
			isgang.put(QZMJConstant.gangvalue, new Object[] { jieguo[2], jieguo[2], jieguo[2], jieguo[2] });
			sendGameEventResult(room, thisask, isgang, 1);
		} else if (type == 5) {
			// 碰 询问事件
			JSONObject ispeng = obj;
			ispeng.put(QZMJConstant.pengvalue, new Object[] { jieguo[2], jieguo[2], jieguo[2] });
			sendGameEventResult(room, thisask, ispeng, 1);
		} else if (type == 4) {
			// 吃 询问事件
			JSONObject ischi = obj;
			ischi.put(QZMJConstant.chivalue, jieguo[2]);
			sendGameEventResult(room, thisask, ischi, 1);
		} else if (type == 2) {
			// 询问暗杠
			JSONObject ischi = obj;
			sendGameEventResult(room, thisask, ischi, 1);
		} else if (type == 9) {
			// 询问抓杠
			JSONObject ischi = obj;
			ischi.put(QZMJConstant.gangvalue, new int[] { (Integer) jieguo[3] });
			// 获取抓杠位置
			int zgindex = room.getUserPacketMap().get(thisask).buGangIndex((Integer) jieguo[2]);
			ischi.put(QZMJConstant.zgindex, zgindex);
			sendGameEventResult(room, thisask, ischi, 1);
		} else if (type == 10) {
			// 触发多事件询问
			JSONObject result = obj;
			JSONArray array = JSONArray.fromObject(jieguo[2]);
			int[] types = new int[array.size()];
			for (int i = 0; i < array.size(); i++) {
				JSONObject data = array.getJSONObject(i);
				if (data.getInt("type") == 7) {
					result.put(QZMJConstant.huvalue, pai);
				} else if (data.getInt("type") == 6) {
					result.put(QZMJConstant.gangvalue, new Object[] { pai, pai, pai, pai });
				} else if (data.getInt("type") == 5) {
					result.put(QZMJConstant.pengvalue, new int[] { pai, pai, pai });
				} else if (data.getInt("type") == 4) {
					result.put(QZMJConstant.chivalue, data.get("value"));
				}
				types[i] = data.getInt("type");
			}
			result.put(QZMJConstant.type, types);
			sendGameEventResult(room, thisask, result, 1);
		} else if (type == 1) {
			// 出牌或下家抓牌
			if (thisask != null && jieguo[2] != null) {
				// 出牌
				JSONObject chupai = obj;

				// 牌局中剩余牌数（包含其他玩家手牌）
				List<Integer> shengyuList = new ArrayList<Integer>(Dto.arrayToList(room.getPai(), room.getIndex()));
				for (String uuid : room.getPlayerMap().keySet()) {
					if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
						if (!thisask.equals(uuid)) {
							shengyuList.addAll(room.getUserPacketMap().get(uuid).getMyPai());
						}
					}
				}

				if (room.isRobot() && room.getRobotList().contains(thisask)) {
					int delayTime = RandomUtils.nextInt(3) + 2;
					AppBeanUtil.getBean(RobotEventDeal.class).changeRobotActionDetail(thisask,
							QZMJConstant.QZMJ_GAME_EVENT_CP, delayTime);
				}
				// 出牌提示
				JSONArray tingTip = MaJiangCore.tingPaiTip(room.getUserPacketMap().get(thisask).getMyPai(),
						room.getJin(), shengyuList);
				chupai.put("tingTip", tingTip);
				chupai.put("compensateList", MaJiangCore.getCompensateList(
						room.getUserPacketMap().get(thisask).getMyPai(), getOutList(roomNo), room.getJin(), tingTip));
				beginGameEventTimer(roomNo, thisask, 0, QZMJConstant.QZ_MJ_TIMER_TYPE_CP);
				CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(thisask).getUuid(), String.valueOf(chupai),
						"gameActionPush");
			} else {
				// 下家抓牌事件，并通知所有人
				JSONArray mjieguo = moPai(roomNo, room.getThisAccount());
				detailDataByZhuaPai(mjieguo, room, null);
			}
		}
	}

	private void sendGameEventResult(QZMJGameRoom room, String thisAsk, JSONObject result, int type) {
		if (room.isRobot() && room.getRobotList().contains(thisAsk)) {
			int delayTime = RandomUtils.nextInt(3) + 2;
			if (result.getJSONArray("type").getInt(0) == 3) {
				AppBeanUtil.getBean(RobotEventDeal.class).changeRobotActionDetail(thisAsk,
						QZMJConstant.QZMJ_GAME_EVENT_ROBOT_ZM, delayTime);
			} else if (result.getJSONArray("type").getInt(0) == 7) {
				AppBeanUtil.getBean(RobotEventDeal.class).changeRobotActionDetail(thisAsk,
						QZMJConstant.QZMJ_GAME_EVENT_ROBOT_HU, delayTime);
			} else {
				AppBeanUtil.getBean(RobotEventDeal.class).changeRobotActionDetail(thisAsk,
						QZMJConstant.QZMJ_GAME_EVENT_ROBOT_GUO, delayTime);
			}
		} else {
			beginGameEventTimer(room.getRoomNo(), thisAsk, type, QZMJConstant.QZ_MJ_TIMER_TYPE_EVENT);
			if (room.getUserPacketMap().get(thisAsk).getIsTrustee() != CommonConstant.GLOBAL_YES) {
				CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(thisAsk).getUuid(), String.valueOf(result),
						"gameActionPush");
				room.setTooNextAskAccount(result);// 过事件 后下家操作
			}
		}
	}

	public void detailDataByChiGangPengHu(String roomNo, int type, String clientId, int[] value) {
		// -2：暗杠结果事件 -3：自摸结果事件 -4：吃结果事件 -5：碰结果事件 -6：明杠结果事件 -7：胡 结果事件 -9：抓明杠
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		Set<String> uuids = room.getPlayerMap().keySet();
		int lastpai = room.getLastPai();
		int lastmopai = room.getLastMoPai();
		JSONObject back = new JSONObject();
		back.put(QZMJConstant.foucs, room.getPlayerIndex(clientId));
		back.put(QZMJConstant.foucsIndex, room.getFocusIndex());
		back.put(QZMJConstant.nowPoint, room.getNowPoint());
		back.put(QZMJConstant.zpaishu, room.getPai().length - room.getIndex());
		int lastFoucs = room.getPlayerIndex(room.getLastAccount());
		back.put(QZMJConstant.lastFoucs, lastFoucs);
		switch (type) {
		case -2:
			// -2：暗杠结果事件
			JSONObject objAnGang = back;
			objAnGang.put(QZMJConstant.type, 1);
			objAnGang.put(QZMJConstant.lastType, -2);
			objAnGang.put(QZMJConstant.lastGangvalue, new int[] { value[1], value[1], value[1], value[1] });
			CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(objAnGang), "gameActReultPush");
			beginGameEventTimer(room.getRoomNo(), clientId, 0, QZMJConstant.QZ_MJ_TIMER_TYPE_CP);
			break;
		case -3:
			// -3：自摸结果事件
			JSONObject objZiMo = new JSONObject();
			objZiMo.put(QZMJConstant.type, 1);
			objZiMo.put(QZMJConstant.lastType, -3);
			objZiMo.put(QZMJConstant.lastValue, new int[] { lastmopai });
			CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(clientId).getUuid(), String.valueOf(objZiMo),
					"gameActReultPush");
			break;
		case -4:
			// -4：吃结果事件
			JSONObject objchi = back;
			objchi.put(QZMJConstant.type, 1);
			objchi.put(QZMJConstant.lastType, -4);
			objchi.put(QZMJConstant.lastValue, new int[] { lastpai });
			objchi.put(QZMJConstant.lastChiValue, value);

			// 牌局中剩余牌数（包含其他玩家手牌）
			List<Integer> shengyuList = new ArrayList<Integer>(Dto.arrayToList(room.getPai(), room.getIndex()));
			for (String uuid : room.getPlayerMap().keySet()) {
				if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
					if (!clientId.equals(uuid)) {
						shengyuList.addAll(room.getUserPacketMap().get(uuid).getMyPai());
					}
				}
			}

			for (String uuid : room.getPlayerMap().keySet()) {
				if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
					// 听牌提示
					if (uuid.equals(clientId)) {
						// 出牌提示
						JSONArray tingTip = MaJiangCore.tingPaiTip(room.getUserPacketMap().get(clientId).getMyPai(),
								room.getJin(), shengyuList);
						objchi.put("tingTip", tingTip);
						objchi.put("compensateList",
								MaJiangCore.getCompensateList(room.getUserPacketMap().get(clientId).getMyPai(),
										getOutList(roomNo), room.getJin(), tingTip));
					}
				}
				CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(), String.valueOf(objchi),
						"gameActReultPush");
			}
			beginGameEventTimer(room.getRoomNo(), clientId, 0, QZMJConstant.QZ_MJ_TIMER_TYPE_CP);
			break;
		case -5:
			// -5：碰结果事件
			JSONObject objpeng = back;
			objpeng.put(QZMJConstant.type, 1);
			objpeng.put(QZMJConstant.lastPengvalue, new int[] { lastpai, lastpai, lastpai });
			objpeng.put(QZMJConstant.lastType, -5);
			objpeng.put(QZMJConstant.lastValue, new int[] { lastpai });
			// 牌局中剩余牌数（包含其他玩家手牌）
			List<Integer> shengyuList1 = new ArrayList<Integer>(Dto.arrayToList(room.getPai(), room.getIndex()));
			for (String uuid : room.getPlayerMap().keySet()) {
				if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
					if (!clientId.equals(uuid)) {
						shengyuList1.addAll(room.getUserPacketMap().get(uuid).getMyPai());
					}
				}
			}
			// 通知所有玩家碰
			for (String uuid : uuids) {
				if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
					// 听牌提示
					if (uuid.equals(clientId)) {
						// 出牌提示
						JSONArray tingTip = MaJiangCore.tingPaiTip(room.getUserPacketMap().get(clientId).getMyPai(),
								room.getJin(), shengyuList1);
						objpeng.put("tingTip", tingTip);
						objpeng.put("compensateList",
								MaJiangCore.getCompensateList(room.getUserPacketMap().get(clientId).getMyPai(),
										getOutList(roomNo), room.getJin(), tingTip));
					}
					CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(),
							String.valueOf(objpeng), "gameActReultPush");
				}
			}
			beginGameEventTimer(room.getRoomNo(), clientId, 0, QZMJConstant.QZ_MJ_TIMER_TYPE_CP);
			break;
		case -6:
			// -6：明杠结果事件
			JSONObject objMingGang = back;
			objMingGang.put(QZMJConstant.type, 1);
			objMingGang.put(QZMJConstant.lastType, -6);
			objMingGang.put(QZMJConstant.lastGangvalue, new int[] { value[1], value[1], value[1], value[1] });
			objMingGang.put(QZMJConstant.lastValue, new int[] { lastpai });
			CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(objMingGang), "gameActReultPush");
			beginGameEventTimer(room.getRoomNo(), clientId, 0, QZMJConstant.QZ_MJ_TIMER_TYPE_CP);
			break;
		case -7:
			// -7：胡 结果事件
			JSONObject objHu = new JSONObject();
			objHu.put(QZMJConstant.type, 1);
			objHu.put(QZMJConstant.lastType, -7);
			objHu.put(QZMJConstant.lastValue, new int[] { lastpai });
			CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(clientId).getUuid(), String.valueOf(objHu),
					"gameActReultPush");
			break;
		case -9:
			// -9：抓明杠 结果事件
			JSONObject objZhuangGang = back;
			objZhuangGang.put(QZMJConstant.type, 1);
			objZhuangGang.put(QZMJConstant.lastType, -9);
			objZhuangGang.put(QZMJConstant.lastGangvalue, new int[] { value[1], value[1], value[1], value[1] });
			// 获取抓杠位置
			int zgindex = room.getUserPacketMap().get(clientId).buGangIndex(value[1]);
			objZhuangGang.put(QZMJConstant.lastzgindex, zgindex);
			CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(objZhuangGang), "gameActReultPush");
			beginGameEventTimer(room.getRoomNo(), clientId, 0, QZMJConstant.QZ_MJ_TIMER_TYPE_CP);
			break;
		default:
			break;
		}
	}

	public void liuJu(String roomNo) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		if (room.getIndex() >= room.getPai().length - QZMJConstant.LEFT_PAI_COUNT && room.getGameStatus() > 0) {
			room.setGameStatus(QZMJConstant.QZ_GAME_STATUS_SUMMARY);
			JSONArray array = new JSONArray();
			for (String account : room.getPlayerMap().keySet()) {
				if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
					JSONObject data = new JSONObject();
					QZMJUserPacket up = room.getUserPacketMap().get(account);
					Playerinfo player = room.getPlayerMap().get(account);
					// 获取玩家吃杠碰的牌
					List<Integer> paiList = up.getHistoryList();
					// 手牌排序
					Collections.sort(up.getMyPai());
					paiList.addAll(up.getMyPai());
					data.put("myPai", paiList.toArray());
					data.put("isWinner", 0);
					data.put("fan", 0);
					data.put("fanDetail", up.getFanDetail(up.getMyPai(), room, account));
					data.put("score", up.getScore());
					data.put("player", player.getName());
					data.put("headimg", player.getRealHeadimg());
					data.put("hua", up.getHuaList().size());
					data.put("myIndex", player.getMyIndex());
					data.put("huaValue", JSONArray.fromObject(up.getHuaList()));
					data.put("gangValue", JSONArray.fromObject(up.getGangValue()));
					// 判断玩家是否是庄家
					if (account.equals(room.getBanker())) {
						data.put("zhuang", 1);
						data.put("bankerTimes", room.getBankerTimes());// 连庄次数
					} else {
						data.put("zhuang", 0);
					}
					data.put("difen", room.getUserScore(account));

					array.add(data);
					if (room.isRobot() && room.getRobotList().contains(account)) {
						int delayTime = RandomUtils.nextInt(3) + 2;
						AppBeanUtil.getBean(RobotEventDeal.class).changeRobotActionDetail(account,
								QZMJConstant.QZMJ_GAME_EVENT_LOAD_FINISH, delayTime);
					}
				}
			}
			// 返回数据
			JSONObject result = new JSONObject();
			result.put("type", 0);
			result.put("isLiuju", CommonConstant.GLOBAL_YES);
			result.put("data", array);
			// 流局算连庄
			room.addBankTimes();
			// 保存结算记录
			room.addKaijuList(-1, 999, new int[] {});
			if (CommonConstant.ROOM_TYPE_FREE != room.getRoomType()) {
				if (room.getGameCount() <= room.getGameIndex()
						|| room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_FINAL_SUMMARY) {
					room.setGameStatus(QZMJConstant.QZ_GAME_STATUS_FINAL_SUMMARY);
					// 保存结算汇总数据
					JSONArray jiesuanArray = obtainFinalSummaryArray(room);
					result.put("type", 1);
					result.put("data1", jiesuanArray);
					room.setNeedFinalSummary(true);
				}
			}
			result.put("fanMultiple", room.getFanMultiple());// 番倍
			result.put("score", room.getScore());// 低分
			result.put("zhuangScore", room.getZhuangScore());// 庄分
			result.put("xianScore", room.getXianScore());// 闲分
			result.put("time", room.getXianScore());//
			result.put("roomNo", room.getRoomNo());//
			room.setSummaryData(result);
			CommonConstant.sendMsgEventAll(room.getAllUUIDList(), result, "gameLiuJuPush");

			summaryDeal(roomNo, false);// 结算处理
		}
	}

	public boolean checkIsHuByMySelf(String roomNo, int oldPai) {

		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return false;
		String account = room.getThisAccount();
		if (null == account) {
			return false;
		}
		QZMJUserPacket up = room.getUserPacketMap().get(account);
		if (null == up) {
			return false;
		}
		// 获取我的手牌
		List<Integer> myPai = up.getMyPai();
		// 自摸
		room.setNextAskType(QZMJConstant.ASK_TYPE_HU_BY_MYSELF);
		room.setNextAskAccount(account);
		return MaJiangCore.isHu(myPai, 0, room.getJin(), room.getHasJinNoPingHu());
	}

	public List<String> checkIsHuByOtherSelf(String roomNo, int oldPai) {

		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return null;
		List<String> uuidList = new ArrayList<String>();
		if (!room.isNotChiHu) {
			// 获取本次事件询问人
			String nextAsk = room.getNextAskAccount();

			// 轮到出牌的玩家，一轮询问结束，进入下一个事件询问（杠事件）
			if (nextAsk.equals(room.getLastAccount())) {
				// 跳过出牌的玩家
				nextAsk = room.getNextPlayer(nextAsk);
				// 设置下一次询问的事件
				room.setNextAskType(QZMJConstant.ASK_TYPE_GANG_MING);
				// 设置下一次询问的人
				room.setNextAskAccount(nextAsk);
			}

			int jinPai = room.getJin();

			while (room.getNextAskType() == QZMJConstant.ASK_TYPE_HU_OTHER) {

				if (room.getUserPacketMap().get(nextAsk).getIsTrustee() == CommonConstant.GLOBAL_NO) {// 询问未托管玩家
					// 获取玩家的手牌
					List<Integer> mypai = room.getUserPacketMap().get(nextAsk).getMyPai();
					int hasJinNoPingHu = room.getHasJinNoPingHu();// 有金玩法
					boolean isAllowHu = true; // 玩家是否可以平胡
					if (QZMJConstant.HAS_JIN_NO_PING_HU_1 == hasJinNoPingHu
							|| QZMJConstant.HAS_JIN_NO_PING_HU_2 == hasJinNoPingHu) {
						if (mypai.contains(jinPai)) {// 玩家有金情况
							isAllowHu = false;
						}
					}

					int youjining = room.getUserPacketMap().get(nextAsk).getYouJinIng();
					// 玩家在游金或金数大于2，不能平胡
					if (youjining > 0) {
						isAllowHu = false;
					} else if (room.getUserPacketMap().get(nextAsk).getPlayerJinCount(jinPai) >= 2) {
						isAllowHu = false;
					}

					if (isAllowHu) {
						boolean back = MaJiangCore.isHu(mypai, oldPai, jinPai, room.getHasJinNoPingHu());
						if (back) {
							uuidList.add(nextAsk);
						}
					}

				}
				// 设置下一个玩家
				nextAsk = room.getNextPlayer(nextAsk);
				// 一轮询问结束，进入下一个事件询问（杠事件）
				// 轮到出牌的玩家
				if (nextAsk.equals(room.getLastAccount())) {
					// 跳过出牌的玩家
					nextAsk = room.getNextPlayer(nextAsk);
					// 设置下一次询问的事件
					room.setNextAskType(QZMJConstant.ASK_TYPE_GANG_MING);
					// 设置下一次询问的人
					room.setNextAskAccount(nextAsk);
				} else {
					// 换下个人询问
					room.setNextAskType(QZMJConstant.ASK_TYPE_HU_OTHER);
					room.setNextAskAccount(nextAsk);
				}

			}
		}

		return uuidList;
	}

	public int[] checkIsAnGang(String roomNo, int newPai) {

		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return null;
		String account = room.getThisAccount();
		if (account != null && RoomManage.gameRoomMap.get(roomNo).getPlayerMap().get(account) != null) {
			// 自摸
			room.setNextAskType(QZMJConstant.ASK_TYPE_GANG_AN);
			room.setNextAskAccount(account);
			// 获取我的手牌
			List<Integer> myPai = room.getUserPacketMap().get(account).getMyPai();
			List<DontMovePai> penghistory = room.getUserPacketMap().get(account).getPengList();
			return MaJiangCore.isGang(myPai, newPai, 1, penghistory);
		}

		return new int[] { 0 };
	}

	public String checkIsMingGang(String roomNo, int oldPai) {

		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return null;
		// 本次需要判断的人
		String askAccount = room.getNextAskAccount();
		// 下次需要判断的人
		String nextAsk = room.getNextPlayer(askAccount);
		// 判断下次询问的人是否是出牌人如果是出牌人进行下一次事件
		if (nextAsk.equals(room.getLastAccount())) {
			nextAsk = room.getNextPlayer(nextAsk);
			// 设置下一次询问的事件
			room.setNextAskType(QZMJConstant.ASK_TYPE_PENG);
			// 设置下一次询问的人
			room.setNextAskAccount(nextAsk);
		} else {
			// 设置下一次询问的事件
			room.setNextAskType(QZMJConstant.ASK_TYPE_GANG_MING);
			// 设置下一次询问的人
			room.setNextAskAccount(nextAsk);
		}
		// 获取我的手牌
		if (room.getUserPacketMap().get(askAccount).getIsTrustee() == CommonConstant.GLOBAL_NO) {// 询问未托管玩家
			List<Integer> myPai = room.getUserPacketMap().get(askAccount).getMyPai();
			int[] back = MaJiangCore.isGang(myPai, oldPai, 2, null);
			if (back[0] > 0) {
				return askAccount;
			}
		}
		if (room.getNextAskType() == QZMJConstant.ASK_TYPE_GANG_MING) {
			return checkIsMingGang(roomNo, oldPai);
		}

		return null;
	}

	public String checkIsPeng(String roomNo, int oldPai) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return null;
		if (oldPai == room.getJin()) {
			return null;
		}
		String askAccount = room.getNextAskAccount();
		String nextAsk = room.getNextPlayer(askAccount);
		if (nextAsk.equals(room.getLastAccount())) {
			nextAsk = room.getNextPlayer(nextAsk);
			// 设置下一次询问的事件
			room.setNextAskType(QZMJConstant.ASK_TYPE_CHI);
			// 设置下一次询问的人
			room.setNextAskAccount(nextAsk);
		} else {
			// 设置下一次询问的事件
			room.setNextAskType(QZMJConstant.ASK_TYPE_PENG);
			// 设置下一次询问的人
			room.setNextAskAccount(nextAsk);
		}
		// 获取我的手牌
		if (room.getUserPacketMap().get(askAccount).getIsTrustee() == CommonConstant.GLOBAL_NO) {// 询问未托管玩家
			List<Integer> myPai = room.getUserPacketMap().get(askAccount).getMyPai();
			int[] back = MaJiangCore.isPeng(myPai, oldPai);
			if (back[0] == 1) {
				return askAccount;
			}
		}
		if (room.getNextAskType() == QZMJConstant.ASK_TYPE_PENG) {
			return checkIsPeng(roomNo, oldPai);
		}

		return null;
	}

	public Object[] checkIsChi(String roomNo, int oldPai, String nextAccount) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return null;
		if (!room.isNotChiHu) {
			String nextAsk = room.getNextAskAccount();
			// 只有出牌玩家的下家才可以吃
			if (nextAsk.equals(nextAccount)) {
				// 获取我的手牌
				if (room.getUserPacketMap().get(nextAsk).getIsTrustee() == CommonConstant.GLOBAL_NO) {// 询问未托管玩家
					List<Integer> myPai = room.getUserPacketMap().get(nextAsk).getMyPai();
					// 设置下一次询问 完成
					room.setNextAskType(QZMJConstant.ASK_TYPE_FINISH);
					List<int[]> back = MaJiangCore.isChi(myPai, oldPai, room.getJin());
					if (back != null && back.size() > 0) {
						return new Object[] { nextAsk, back };
					}
				}
			}
		}

		return null;
	}

	public void setNextAsk(String roomNo, String nextAsk, int thisAskType, int nextAskType) {
		// 判断下次询问的人是否是出牌人如果是出牌人进行下一次事件
		if (nextAsk.equals(((QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo)).getLastAccount())) {
			nextAsk = ((QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo)).getNextPlayer(nextAsk);
			// 设置下一次询问的事件
			((QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo)).setNextAskType(nextAskType);
			// 设置下一次询问的人
			((QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo)).setNextAskAccount(nextAsk);
		} else {
			// 设置下一次询问的事件
			((QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo)).setNextAskType(thisAskType);
			// 设置下一次询问的人
			((QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo)).setNextAskAccount(nextAsk);
		}
	}

	private void beginGameEventTimer(final String roomNo, final String nextPlayerAccount, final int type,
			final int timerType) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		room.andAskNum();// 添加询问次数
		if (CommonConstant.ROOM_TYPE_YB == room.getRoomType() || CommonConstant.ROOM_TYPE_FK == room.getRoomType()
				|| CommonConstant.ROOM_TYPE_FREE == room.getRoomType()
				|| CommonConstant.ROOM_TYPE_INNING == room.getRoomType()
				|| CommonConstant.ROOM_TYPE_TEA == room.getRoomType()) {
			if (room.getPlayTime() <= 0) {
				return;
			}
			final int timeLeft = room.getPlayTime();

			final int askNum = room.getAskNum();// 询问次数
			ThreadPoolHelper.executorService.submit(new Runnable() {
				@Override
				public void run() {
					if (timerType == QZMJConstant.QZ_MJ_TIMER_TYPE_EVENT) {
						gameTimerQZMJ.gameEventOverTime(roomNo, nextPlayerAccount, timeLeft, type, askNum);
					} else if (timerType == QZMJConstant.QZMJ_GAME_EVENT_CP) {
						gameTimerQZMJ.cpOverTime(roomNo, nextPlayerAccount, timeLeft, askNum);
					}
				}
			});
		}
	}

	private boolean isFullAndReady(String roomNo) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return false;
		if (room.getUserPacketMap().size() == room.getPlayerCount()) {
			int readyCount = 0;
			for (String account : room.getUserPacketMap().keySet()) {
				if (room.getUserPacketMap().get(account).getStatus() == QZMJConstant.QZ_USER_STATUS_READY) {
					readyCount++;
				}
			}
			if (readyCount == 1) {
				return true;
			}
		}
		return false;
	}

	private List<Integer> getOutList(String roomNo) {

		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return null;
		List<Integer> outList = new ArrayList<>();
		List<Integer> leftList = new ArrayList<>(Dto.arrayToList(room.getPai(), room.getIndex()));
		for (String account : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
				leftList.addAll(room.getUserPacketMap().get(account).getMyPai());
			}
		}
		List<Integer> allList = new ArrayList<>(Dto.arrayToList(QZMJConstant.ALL_PAI, 0));
		// 去除玩家手里的牌及牌堆里的牌
		for (Integer pai : leftList) {
			if (allList.contains(pai)) {
				allList.remove(pai);
			}
		}
		// 去除花牌
		for (Integer pai : allList) {
			if (pai < 50) {
				outList.add(pai);
			}
		}

		return outList;
	}

	private void exitRoomByPlayer(String roomNo, long userId, String account) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (room == null)
			return;
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

	public void forcedRoom(Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!postData.containsKey(CommonConstant.DATA_KEY_ROOM_NO))
			return;
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_DISSOLVE);
		// 给房间里每人通知管理员解散房间
		JSONObject object = new JSONObject();
		int type = CommonEventConstant.TIP_MSG_PUSH_TYPE_CPM;
		if (room.getGameIndex() <= 1 && Dto.isObjNull(room.getSummaryData())) {// 未玩一局
			type = CommonEventConstant.TIP_MSG_PUSH_TYPE_EXIT;
		}
		object.put("type", type);
		object.put(CommonConstant.RESULT_KEY_MSG, "管理员解散房间");
		CommonConstant.sendMsgEventAll(room.getAllUUIDList(), object, CommonEventConstant.TIP_MSG_PUSH);

		if (type == CommonEventConstant.TIP_MSG_PUSH_TYPE_CPM) {
			room.setGameStatus(QZMJConstant.QZ_GAME_STATUS_SUMMARY);
			room.setGameIndex(room.getGameCount());
			// 结算事件，返回结算处理结果
			sendSummaryData(roomNo, true, true);
		} else {
			room.setNeedFinalSummary(true);
			finalSummaryRoom(room.getRoomNo(), true);// 总结算房间处理
		}

	}
}
