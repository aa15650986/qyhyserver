package com.zhuoan.biz.event;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.nn.NNUserPacket;
import com.zhuoan.biz.event.bdx.BDXGameEventDealNew;
import com.zhuoan.biz.event.ddz.DdzGameEventDeal;
import com.zhuoan.biz.event.gdy.GDYGameEventDeal;
import com.zhuoan.biz.event.gppj.GPPJGameEventDeal;
import com.zhuoan.biz.event.gzmj.GzMjGameEventDeal;
import com.zhuoan.biz.event.match.MatchEventDeal;
import com.zhuoan.biz.event.nn.NNGameEventDealNew;
import com.zhuoan.biz.event.pdk.PDKGameEventDeal;
import com.zhuoan.biz.event.qzmj.QZMJGameEventDeal;
import com.zhuoan.biz.event.sg.SGGameEventDeal;
import com.zhuoan.biz.event.sss.SSSGameEventDealNew;
import com.zhuoan.biz.event.sw.SwGameEventDeal;
import com.zhuoan.biz.event.zjh.ZJHGameEventDealNew;
import com.zhuoan.biz.game.biz.*;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.bdx.BDXGameRoomNew;
import com.zhuoan.biz.model.bdx.UserPackerBDX;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.ddz.DdzGameRoom;
import com.zhuoan.biz.model.ddz.UserPacketDdz;
import com.zhuoan.biz.model.gdy.GDYGameRoom;
import com.zhuoan.biz.model.gdy.GDYUserPacket;
import com.zhuoan.biz.model.gppj.GPPJGameRoom;
import com.zhuoan.biz.model.gppj.UserPacketGPPJ;
import com.zhuoan.biz.model.gzmj.GzMjGameRoom;
import com.zhuoan.biz.model.nn.NNGameRoomNew;
import com.zhuoan.biz.model.pdk.PDKGameRoom;
import com.zhuoan.biz.model.pdk.PDKUserPacket;
import com.zhuoan.biz.model.qzmj.QZMJGameRoom;
import com.zhuoan.biz.model.qzmj.QZMJUserPacket;
import com.zhuoan.biz.model.sg.SGGameRoom;
import com.zhuoan.biz.model.sg.SGUserPacket;
import com.zhuoan.biz.model.sss.SSSGameRoomNew;
import com.zhuoan.biz.model.sss.SSSUserPacket;
import com.zhuoan.biz.model.sw.SwGameRoom;
import com.zhuoan.biz.model.zjh.ZJHGameRoomNew;
import com.zhuoan.biz.robot.RobotEventDeal;
import com.zhuoan.constant.*;
import com.zhuoan.constant.event.CircleBaseEventConstant;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.service.CircleService;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.TeaService;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import static net.sf.json.JSONObject.fromObject;

@Component
public class BaseEventDeal {

	public static String noticeContentGame = "";
	private final static Logger logger = LoggerFactory.getLogger(BaseEventDeal.class);
	@Resource
	private UserBiz userBiz;
	@Resource
	private RoomBiz roomBiz;
	@Resource
	private PublicBiz publicBiz;
	@Resource
	private GameLogBiz gameLogBiz;
	@Resource
	private AchievementBiz achievementBiz;
	@Resource
	private PropsBiz propsBiz;
	@Resource
	private ClubBiz clubBiz;
	@Resource
	private RobotBiz robotBiz;
	@Resource
	private NNGameEventDealNew nnGameEventDealNew;
	@Resource
	private SSSGameEventDealNew sssGameEventDealNew;
	@Resource
	private ZJHGameEventDealNew zjhGameEventDealNew;
	@Resource
	private BDXGameEventDealNew bdxGameEventDealNew;
	@Resource
	private QZMJGameEventDeal qzmjGameEventDeal;
	@Resource
	private GPPJGameEventDeal gppjGameEventDeal;
	@Resource
	private SwGameEventDeal swGameEventDeal;
	@Resource
	private DdzGameEventDeal ddzGameEventDeal;
	@Resource
	private PDKGameEventDeal pdkGameEventDeal;
	@Resource
	private Destination daoQueueDestination;
	@Resource
	private ProducerService producerService;
	@Resource
	private RedisService redisService;
	@Resource
	private FundEventDeal fundEventDeal;
	@Resource
	private MatchEventDeal matchEventDeal;
	@Resource
	private RedisInfoService redisInfoService;
	@Resource
	private GDYGameEventDeal gdyGameEventDeal;
	@Resource
	private SGGameEventDeal sgGameEventDeal;
	@Resource
	private GzMjGameEventDeal gzMjGameEventDeal;
	@Resource
	private TeaService teaService;
	@Resource
	private CircleService circleService;
	@Resource
	private ZaNewSignBiz zaNewSignBiz;
	@Resource
	private RobotEventDeal robotEventDeal;

	public void createRoomBase(SocketIOClient client, Object data) {
		// 检查是否能加入房间
		JSONObject postData = fromObject(data);
		// 玩家账号
		String account = postData.getString("account");

		// 如果加入房间类型不是茶楼 验证该玩家是否在茶楼房间里 start
		for (String room_No : RoomManage.gameRoomMap.keySet()) {
			GameRoom gameRoom = RoomManage.gameRoomMap.get(room_No);
			if (null == gameRoom)
				continue;
			// 如果在房间里排除
			if (gameRoom.getPlayerMap().containsKey(account) && null != gameRoom.getPlayerMap().get(account)) {
				String msg = "您已经在房间中...";
				if (CommonConstant.ROOM_TYPE_TEA == gameRoom.getRoomType()) {
					msg = "您已经在茶楼房间中...";
				}
				CommonConstant.sendMsgEventNo(client, msg, null, "enterRoomPush_NN");
				return;
			}
		}
		// 如果加入房间类型不是茶楼 验证该玩家是否在茶楼房间里 end

		// 获取用户信息
		JSONObject userInfo = userBiz.getUserByAccount(account);
		if (Dto.isObjNull(userInfo)) {
			// 用户不存在
			CommonConstant.sendMsgEventNo(client, "用户不存在", null, "enterRoomPush_NN");
			return;
		}
		if (!userInfo.containsKey("uuid") || Dto.stringIsNULL(userInfo.getString("uuid"))
				|| !userInfo.getString("uuid").equals(postData.getString("uuid"))) {
			return;
		}
		// 添加工会信息
		if (!Dto.isObjNull(userInfo) && userInfo.containsKey("gulidId") && userInfo.getInt("gulidId") > 0
				&& userInfo.containsKey("unionid") && userInfo.containsKey("platform")) {
			JSONObject ghInfo = userBiz.getGongHui(userInfo);
			if (!Dto.isObjNull(ghInfo) && ghInfo.containsKey("name")) {
				userInfo.put("ghName", ghInfo.getString("name"));
			}
		}
		// 房间信息
		JSONObject baseInfo = postData.getJSONObject("base_info");
		int roomType = baseInfo.getInt("roomType");// 房间类型
		// 元宝场
		if (roomType == CommonConstant.ROOM_TYPE_YB && userInfo.containsKey("yuanbao")) {
			double minScore = obtainBankMinScore(postData);
			if (minScore == -1 || userInfo.getDouble("yuanbao") < minScore) {
				// 元宝不足
				CommonConstant.sendMsgEventNo(client, "元宝不足", null, "enterRoomPush_NN");
				return;
			}
		}
		// 金币场
		else if (roomType == CommonConstant.ROOM_TYPE_JB && userInfo.containsKey("coins")) {
			if (userInfo.getDouble("coins") < baseInfo.getDouble("goldCoinEnter")) {
				// 金币不足
				CommonConstant.sendMsgEventNo(client, "金币不足", null, "enterRoomPush_NN");
				return;
			} else if (baseInfo.getDouble("goldCoinEnter") != baseInfo.getDouble("goldCoinLeave")
					&& userInfo.getDouble("coins") > baseInfo.getDouble("goldCoinLeave")) {
				// 金币过多
				CommonConstant.sendMsgEventNo(client, "金币超该场次限制", null, "enterRoomPush_NN");
				return;
			}
		}
		// 房卡场
		else if (roomType == CommonConstant.ROOM_TYPE_FK
				|| baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_DK) {
			String updateType = baseInfo.containsKey("updateYb") ? "yuanbao" : "roomcard";
			if (userInfo.containsKey(updateType)) {
				int roomCard = getRoomCardPayInfo(baseInfo);
				if (userInfo.getInt(updateType) < roomCard) {
					String msg = "房卡不足";
					if ("yuanbao".equals(updateType)) {
						msg = "元宝不足";
					}
					CommonConstant.sendMsgEventNo(client, msg, null, "enterRoomPush_NN");
					return;
				}
			}
		}
		// 竞技场
		else if (roomType == CommonConstant.ROOM_TYPE_COMPETITIVE && userInfo.containsKey("roomcard")
				&& userInfo.getDouble("roomcard") < baseInfo.getDouble("goldCoinEnter")) {
			// 金币不足
			CommonConstant.sendMsgEventNo(client, "房卡不足", null, "enterRoomPush_NN");
			return;
		}
		// 好友局
		else if (roomType == CommonConstant.ROOM_TYPE_FRIEND) {
			if (!userInfo.containsKey("parUserId")) {
				JSONObject userProxyInfo = userBiz.getUserProxyInfoById(userInfo.getLong("id"));
				if (Dto.isObjNull(userProxyInfo) || !userProxyInfo.containsKey("player_power_id")
						|| userProxyInfo.getInt("player_power_id") == 0) {
					CommonConstant.sendMsgEventNo(client, "当前未绑定代理关系", null, "enterRoomPush_NN");
					return;
				}
			}
			if (userInfo.containsKey("yuanbao")) {
				int roomCard = getRoomCardPayInfo(baseInfo);
				if (userInfo.getInt("yuanbao") < roomCard) {
					CommonConstant.sendMsgEventNo(client, "牛豆不足", null, "enterRoomPush_NN");
					return;
				}
			}
		}

		if (!BaseInfoUtil.checkBaseInfo(postData.getJSONObject("base_info"), postData.getInt("gid"))) {
			System.out.println("???????????????");
			CommonConstant.sendMsgEventNo(client, "敬请期待", null, "enterRoomPush_NN");
			return;
		}
		// 创建房间
		createRoomBase(client, postData, userInfo);
	}

	public void createRoomBase(SocketIOClient client, JSONObject postData, JSONObject userInfo) {
		JSONObject baseInfo = postData.getJSONObject("base_info");
		if (baseInfo.containsKey("roomType")) {
			int roomType = baseInfo.getInt("roomType");
			if (CommonConstant.ROOM_TYPE_DK != roomType && CommonConstant.ROOM_TYPE_FREE != roomType
					&& CommonConstant.ROOM_TYPE_INNING != roomType && CommonConstant.ROOM_TYPE_TEA != roomType) {// 这些开房者不一定在房间里
				for (String roomNum : RoomManage.gameRoomMap.keySet()) {
					if (RoomManage.gameRoomMap.containsKey(roomNum) && RoomManage.gameRoomMap.get(roomNum) != null
							&& null != userInfo && userInfo.containsKey("account")) {
						if (RoomManage.gameRoomMap.get(roomNum).getPlayerMap()
								.containsKey(userInfo.getString("account"))) {
							return;
						}
					}
				}
			}
		}

		// 添加房间信息
		String roomNo = randomRoomNo();
		logger.info("创建房间--------------------------房间号{}", roomNo);
		// 获取房间实体对象 对应的游戏房间
		GameRoom gameRoom = createRoomByGameId(postData.getInt("gid"), baseInfo, userInfo);

		// 设置房间属性
		gameRoom.setRoomType(baseInfo.getInt("roomType"));
		gameRoom.setGid(postData.getInt("gid"));
		if (postData.containsKey("port")) {
			gameRoom.setPort(postData.getInt("port"));
		}
		if (postData.containsKey("ip")) {
			gameRoom.setIp(postData.getString("ip"));
		}
		if (postData.containsKey("match_num") && gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_MATCH) {
			gameRoom.setMatchNum(postData.getString("match_num"));
		}
		if (postData.containsKey("clubCode")) {
			gameRoom.setClubCode(postData.getString("clubCode"));
		}
		if (postData.containsKey("teaInfoCode")) {
			gameRoom.setClubCode(postData.getString("teaInfoCode"));
		}
		if (postData.containsKey("circleId")) {
			gameRoom.setCircleId(postData.getString("circleId"));
		}
		// 解散房间权限 默认是只能管理员解散房间
		int isPlayerDismiss = CommonConstant.DISSMISS_ROLE_MSG;
		if (baseInfo.containsKey("isPlayerDismiss")) {
			isPlayerDismiss = baseInfo.getInt("isPlayerDismiss");
		}
		gameRoom.setIsPlayerDismiss(isPlayerDismiss);

		// 抽水模式
		String cutType = CircleConstant.CUTTYPE_WU;
		if (baseInfo.containsKey("cutType")) {
			cutType = baseInfo.getString("cutType");
			// 固定抽水
			List<Integer> cutList = new ArrayList<>();
			if (baseInfo.containsKey("cutList")) {
				String cutIds = baseInfo.getString("cutList");
				String[] cutStr = cutIds.split(",");
				for (int i = 0; i < cutStr.length; i++) {
					cutList.add(Integer.valueOf(cutStr[i]));
				}
			}
			gameRoom.setCutList(cutList);
		}
		gameRoom.setCutType(cutType);
		// 消耗分
		String cutFee = CircleConstant.CUT_FEE_WU;
		if (baseInfo.containsKey("cutFee")) {
			cutFee = baseInfo.getString("cutFee");
		}
		gameRoom.setCutFee(cutFee);

		if (baseInfo.containsKey("pump")) {
			gameRoom.setPump(baseInfo.getDouble("pump"));
		}
		gameRoom.setRoomNo(roomNo);
		gameRoom.setRoomInfo(baseInfo);
		gameRoom.setCreateTime(String.valueOf(new Date()));
		// 坐庄最小分数
		gameRoom.setMinBankerScore(obtainBankMinScore(postData));

		if (baseInfo.containsKey("updateYb") && baseInfo.getInt("updateYb") == CommonConstant.GLOBAL_YES) {
			gameRoom.setCurrencyType("yuanbao");
		} else {
			gameRoom.setCurrencyType(gameRoom.getUpdateType());
		}
		// 支付类型
		String paytype = CommonConstant.ROOM_PAY_TYPE_FANGZHU;
		if (baseInfo.containsKey("paytype")) {
			paytype = baseInfo.getString("paytype");
		}
		gameRoom.setPayType(paytype);

		int gameCount = 99999;// 局数
		if (baseInfo.containsKey("turn")) {
			JSONObject turn = baseInfo.getJSONObject("turn");
			if (turn.containsKey("turn")) {
				gameCount = turn.getInt("turn");
			}
		}
		gameRoom.setGameCount(gameCount);
		int roomType = gameRoom.getRoomType();
		int singlePayNum = getRoomCardPayInfo(baseInfo);
		gameRoom.setSinglePayNum(singlePayNum);
		if (CommonConstant.ROOM_TYPE_FK == roomType || CommonConstant.ROOM_TYPE_DK == roomType
				|| CommonConstant.ROOM_TYPE_CLUB == roomType || CommonConstant.ROOM_TYPE_FRIEND == roomType) {
			if (baseInfo.containsKey("turn")) {
				JSONObject turn = baseInfo.getJSONObject("turn");
				// 单个玩家需要扣除的房卡
				if (turn.containsKey("AANum")) {
					if (CommonConstant.ROOM_PAY_TYPE_AA.equals(gameRoom.getPayType())) {
						gameRoom.setEnterScore(singlePayNum);
					}
				}
			}
		}

		if (baseInfo.containsKey("createRoom")) {// 开房者
			gameRoom.setCreateRoom(baseInfo.getString("createRoom"));
		}
		// 底分
		if (baseInfo.containsKey("di")) {
			gameRoom.setScore(baseInfo.getInt("di"));
		} else if (gameRoom.getGid() == CommonConstant.GAME_ID_NAMJ
				|| gameRoom.getGid() == CommonConstant.GAME_ID_ZZC) {
			gameRoom.setScore(5);
		} else if (gameRoom.getGid() == CommonConstant.GAME_ID_DDZ) {
			if (baseInfo.containsKey("baseNum")) {
				gameRoom.setScore(baseInfo.getInt("baseNum"));
			}
		} else {
			gameRoom.setScore(1);
		}
		// 机器人
		if (baseInfo.containsKey("robot") && baseInfo.getInt("robot") == CommonConstant.GLOBAL_YES) {
			// 机器人
			gameRoom.setRobot(true);
		} else {
			gameRoom.setRobot(false);
		}
		// 设置金币场准入金币
		if (baseInfo.containsKey("goldCoinEnter")) {
			gameRoom.setEnterScore(baseInfo.getInt("goldCoinEnter"));
		}
		// 设置金币场准入金币
		if (baseInfo.containsKey("goldCoinLeave")) {
			gameRoom.setLeaveScore(baseInfo.getInt("goldCoinLeave"));
		}
		// 元宝模式
		if (baseInfo.containsKey("yuanbao")) {
			// 底分
			gameRoom.setScore(baseInfo.getInt("yuanbao"));
		}
		// 元宝模式
		if (baseInfo.containsKey("enterYB")) {
			// 俱乐部入场 和离场
			if (CommonConstant.ROOM_TYPE_FREE == roomType || CommonConstant.ROOM_TYPE_INNING == roomType) {
				gameRoom.setEnterScore(baseInfo.getDouble("enterYB"));
			}
		}
		// 元宝模式
		if (baseInfo.containsKey("leaveYB")) {
			// 俱乐部入场 和离场
			if (CommonConstant.ROOM_TYPE_FREE == roomType || CommonConstant.ROOM_TYPE_INNING == roomType) {
				gameRoom.setLeaveScore(baseInfo.getDouble("leaveYB"));
			}

		}
		// 亲友圈
		if (CommonConstant.ROOM_TYPE_FREE == roomType || CommonConstant.ROOM_TYPE_INNING == roomType) {
			// 一课玩法默认底分为5分
			if (gameRoom.getGameCount() != QZMJConstant.QZMJ_KE_GAME_COUNT && baseInfo.containsKey("yuanbao")) {
				gameRoom.setScore(baseInfo.getInt("yuanbao"));
			}
		}
		if (CommonConstant.ROOM_TYPE_CLUB == roomType || CommonConstant.ROOM_TYPE_FREE == roomType
				|| CommonConstant.ROOM_TYPE_INNING == roomType || CommonConstant.ROOM_TYPE_TEA == roomType) {
			gameRoom.setOpen(true);
		} else if (baseInfo.containsKey("open") && baseInfo.getInt("open") == 1
				&& postData.getInt("gid") != CommonConstant.GAME_ID_BDX) {
			gameRoom.setOpen(true);
		} else {
			gameRoom.setOpen(false);
		}
		// 是否允许玩家中途加入
		if (baseInfo.containsKey("halfway") && baseInfo.getInt("halfway") == CommonConstant.GLOBAL_YES) {
			gameRoom.setHalfwayIn(false);
		}
		// 准备超时（0：不处理 1：自动准备 2：踢出房间）
		if (baseInfo.containsKey("readyovertime")) {
			if (baseInfo.getInt("readyovertime") == CommonConstant.READY_OVERTIME_NOTHING) {
				gameRoom.setReadyOvertime(CommonConstant.READY_OVERTIME_NOTHING);
			} else if (baseInfo.getInt("readyovertime") == CommonConstant.READY_OVERTIME_AUTO) {
				gameRoom.setReadyOvertime(CommonConstant.READY_OVERTIME_AUTO);
			} else if (baseInfo.getInt("readyovertime") == CommonConstant.READY_OVERTIME_OUT) {
				gameRoom.setReadyOvertime(CommonConstant.READY_OVERTIME_OUT);
			}
		} else if (baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_YB
				|| baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_JB
				|| baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_COMPETITIVE) {
			gameRoom.setReadyOvertime(CommonConstant.READY_OVERTIME_OUT);
		} else if (gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_FK
				|| gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_DK
				|| gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_CLUB
				|| gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_FRIEND) {
			gameRoom.setReadyOvertime(CommonConstant.READY_OVERTIME_NOTHING);
		} else if (baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_FREE) {
			gameRoom.setReadyOvertime(CommonConstant.READY_OVERTIME_OUT);
		}

		if (userInfo.containsKey("platform")) {
			gameRoom.setPlatform(userInfo.getString("platform"));
		}

		// 好友局只有不同上级代理无法加入 20190103 wqm
		if (CommonConstant.ROOM_TYPE_FRIEND == roomType) {
			if (userInfo.containsKey("parUserId")) {
				gameRoom.setParUserId(userInfo.getLong("parUserId"));
			}
		}

		// 金币、元宝扣服务费
		if (CommonConstant.ROOM_TYPE_YB == roomType) {
			/* 获取房间设置，插入缓存 */
			JSONObject gameSetting = getGameSetting(gameRoom);

			JSONObject gameInfo = redisInfoService.getGameInfoById(postData.getInt("gid")).getJSONObject("setting");

			JSONObject roomFee = gameSetting.getJSONObject("pumpData");
			double fee;
			// 服务费：费率x底注
			if (baseInfo.containsKey("custFee")) {
				// 自定义费率
				if (gameInfo.containsKey("custFee")) {
					fee = gameInfo.getDouble("custFee") * gameRoom.getScore();
				} else {
					fee = baseInfo.getDouble("custFee") * gameRoom.getScore();
				}
			} else {
				// 统一费率
				fee = roomFee.getDouble("proportion") * gameRoom.getScore();
			}
			double maxFee = roomFee.getDouble("max");
			double minFee = roomFee.getDouble("min");
			if (fee > maxFee) {
				fee = maxFee;
			} else if (fee < minFee) {
				fee = minFee;
			}
			fee = new BigDecimal(fee).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
			gameRoom.setFee(fee);
		} else if (baseInfo.containsKey("fee")) {
			gameRoom.setFee(baseInfo.getDouble("fee"));
		}

		int playerNum = baseInfo.getInt("player");
		CopyOnWriteArrayList<Long> idList = new CopyOnWriteArrayList<>();
		for (int i = 0; i < playerNum; i++) {
			// 开房者需要进入房间的
			if (i == 0 && CommonConstant.ROOM_TYPE_DK != roomType && CommonConstant.ROOM_TYPE_FREE != roomType
					&& CommonConstant.ROOM_TYPE_INNING != roomType && CommonConstant.ROOM_TYPE_TEA != roomType) {
				idList.add(userInfo.containsKey("id") ? userInfo.getLong("id") : 0L);
			} else if (playerNum == 2 && i == 1
					&& (CommonConstant.GAME_ID_QZMJ == gameRoom.getGid()
							|| CommonConstant.GAME_ID_NAMJ == gameRoom.getGid()
							|| CommonConstant.GAME_ID_ZZC == gameRoom.getGid()
							|| CommonConstant.GAME_ID_GZMJ == gameRoom.getGid())) {
				// 麻将差异化，两人场坐对面
				idList.add(-1L);
				idList.add(0L);
				gameRoom.addIndexList(i + 1);// 预留座位号
			} else {
				idList.add(0L);
				gameRoom.addIndexList(i);// 预留座位号
			}
		}
		gameRoom.setUserIdList(idList);
		// 玩家人数
		System.out.println("玩家人数：" + playerNum);
		gameRoom.setPlayerCount(playerNum);
		gameRoom.setLastIndex(playerNum);
		// 保存房间信息
		RoomManage.gameRoomMap.put(roomNo, gameRoom);
		long roomId = 0L;// 房间id
		// 代开房间
		if (CommonConstant.ROOM_TYPE_DK == roomType) {
			// 插入数据库
			Playerinfo playerinfo = new Playerinfo();
			roomId = addGameRoom(gameRoom, playerinfo);
			JSONObject result = new JSONObject();
			result.put("type", 0);
			result.put("array", obtainProxyRoomList(userInfo.getString("account")));
			CommonConstant.sendMsgEvent(client, result, "getProxyRoomListPush");
		}
		// 亲友圈开房
		else if (CommonConstant.ROOM_TYPE_FREE == roomType || CommonConstant.ROOM_TYPE_INNING == roomType) {
			// 插入数据库
			Playerinfo playerinfo = new Playerinfo();
			roomId = addGameRoom(gameRoom, playerinfo);
			CommonConstant.sendMsgEventYes(client, "创建房间成功",
					CircleBaseEventConstant.QUICK_JOIN_ROOM_CIRCLE__EVENT_PUSH);
		}
		// 茶楼开房
		else if (CommonConstant.ROOM_TYPE_TEA == roomType) {
			// 插入数据库
			Playerinfo playerinfo = new Playerinfo();
			roomId = addGameRoom(gameRoom, playerinfo);
		} else {
			// 获取用户信息
			JSONObject obtainPlayerInfoData = new JSONObject();
			obtainPlayerInfoData.put("userInfo", userInfo);
			obtainPlayerInfoData.put("myIndex", 0);
			obtainPlayerInfoData.put("gid", postData.getInt("gid"));
			if (client != null) {
				obtainPlayerInfoData.put("uuid", String.valueOf(client.getSessionId()));
			} else {
				obtainPlayerInfoData.put("uuid", String.valueOf(UUID.randomUUID()));
			}
			obtainPlayerInfoData.put("room_type", gameRoom.getRoomType());
			if (postData.containsKey("location")) {
				obtainPlayerInfoData.put("location", postData.getString("location"));
			}
			obtainPlayerInfoData.put("gameCount", gameRoom.getGameCount());
			obtainPlayerInfoData.put("roomScore", gameRoom.getScore());
			obtainPlayerInfoData.put("roomNo", gameRoom.getRoomNo());
			Playerinfo playerinfo = obtainPlayerInfo(obtainPlayerInfoData);

			gameRoom.getPlayerMap().put(playerinfo.getAccount(), playerinfo);
			// 通知玩家
			informUser(gameRoom, playerinfo, client);
			// 组织数据，插入数据库
			roomId = addGameRoom(gameRoom, playerinfo);
			// 开启机器人
			if (gameRoom.isRobot() && gameRoom.getRoomType() != CommonConstant.ROOM_TYPE_MATCH) {
				System.out.println("开启机器人");
				AppBeanUtil.getBean(RobotEventDeal.class).robotJoin(roomNo);
			}
			// 是否是资金盘
			if (userInfo.containsKey("platform")
					&& CommonConstant.fundPlatformList.contains(userInfo.getString("platform"))) {
				gameRoom.setFund(true);
				if (gameRoom.getGid() == CommonConstant.GAME_ID_BDX) {
					fundEventDeal.joinSysUser(roomNo);
				}
			}

		}

		if (0L < roomId) {
			gameRoom.setId(roomId);
		}
	}

	public GameRoom createRoomByGameId(int gameId, JSONObject baseInfo, JSONObject userInfo) {
		System.out.println("baseInfo    " + baseInfo);
		GameRoom gameRoom;
		switch (gameId) {
		case CommonConstant.GAME_ID_NN:// 牛牛
			gameRoom = new NNGameRoomNew();
			createRoomNN((NNGameRoomNew) gameRoom, baseInfo, userInfo.getString("account"));
			break;
		case CommonConstant.GAME_ID_SSS:// 十三水
			gameRoom = new SSSGameRoomNew();
			createRoomSSS((SSSGameRoomNew) gameRoom, baseInfo, userInfo.getString("account"));
			break;
		case CommonConstant.GAME_ID_ZJH:// 炸金花
			gameRoom = new ZJHGameRoomNew();
			createRoomZJH((ZJHGameRoomNew) gameRoom, baseInfo, userInfo.getString("account"));
			break;
		case CommonConstant.GAME_ID_BDX:
			gameRoom = new BDXGameRoomNew();
			((BDXGameRoomNew) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new UserPackerBDX());
			break;
		case CommonConstant.GAME_ID_QZMJ:// 泉州麻将
			gameRoom = new QZMJGameRoom();
			createRoomQZMJ((QZMJGameRoom) gameRoom, baseInfo, userInfo.getString("account"), gameId);
			break;
		case CommonConstant.GAME_ID_ZZC:
			gameRoom = new QZMJGameRoom();
			if (baseInfo.getInt("roomType") != CommonConstant.ROOM_TYPE_DK) {
				((QZMJGameRoom) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new QZMJUserPacket());
			}
			createRoomQZMJ((QZMJGameRoom) gameRoom, baseInfo, userInfo.getString("account"), gameId);
			break;
		case CommonConstant.GAME_ID_NAMJ:
			gameRoom = new QZMJGameRoom();
			((QZMJGameRoom) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new QZMJUserPacket());
			createRoomNAMJ((QZMJGameRoom) gameRoom, baseInfo, userInfo.getString("account"));
			break;
		case CommonConstant.GAME_ID_GP_PJ:
			gameRoom = new GPPJGameRoom();
			((GPPJGameRoom) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new UserPacketGPPJ());
			createRoomGPPJ((GPPJGameRoom) gameRoom, baseInfo, userInfo.getString("account"), gameId);
			break;
		case CommonConstant.GAME_ID_MJ_PJ:
			gameRoom = new GPPJGameRoom();
			((GPPJGameRoom) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new UserPacketGPPJ());
			createRoomGPPJ((GPPJGameRoom) gameRoom, baseInfo, userInfo.getString("account"), gameId);
			break;
		case CommonConstant.GAME_ID_SW:
			gameRoom = new SwGameRoom();
			createRoomSw((SwGameRoom) gameRoom, baseInfo, userInfo.getString("account"));
			break;
		case CommonConstant.GAME_ID_DDZ:
			gameRoom = new DdzGameRoom();
			createRoomDdz((DdzGameRoom) gameRoom, baseInfo, userInfo.getString("account"));
			break;
		case CommonConstant.GAME_ID_PDK: // 新跑得快
			gameRoom = new PDKGameRoom();
			createRoomPdk((PDKGameRoom) gameRoom, baseInfo, userInfo.getString("account"), gameId);
			break;
		case CommonConstant.GAME_ID_GDY:// 干瞪眼
			gameRoom = new GDYGameRoom();
			createRoomGdy((GDYGameRoom) gameRoom, baseInfo, userInfo.getString("account"), gameId);
			break;
		case CommonConstant.GAME_ID_SG:// 三公
			gameRoom = new SGGameRoom();
			createRoomSG((SGGameRoom) gameRoom, baseInfo, userInfo.getString("account"), gameId);
			break;
		case CommonConstant.GAME_ID_GZMJ:// 贵州麻将
			gameRoom = new GzMjGameRoom();
			createRoomGzMj((GzMjGameRoom) gameRoom, baseInfo, userInfo.getString("account"));
			break;
		default:
			gameRoom = new GameRoom();
			break;
		}

		if (RoomManage.gameConfigMap.containsKey("setting")) {
			gameRoom.setSetting(JSONObject.fromObject(RoomManage.gameConfigMap.get("setting")));
		}
		return gameRoom;
	}

	public void informUser(GameRoom gameRoom, Playerinfo playerinfo, SocketIOClient client) {
		JSONObject object = new JSONObject();
		object.put(CommonConstant.DATA_KEY_ACCOUNT, playerinfo.getAccount());
		object.put(CommonConstant.DATA_KEY_ROOM_NO, gameRoom.getRoomNo());
		switch (gameRoom.getGid()) {
		case CommonConstant.GAME_ID_NN:
			nnGameEventDealNew.createRoom(client, object);
			break;
		case CommonConstant.GAME_ID_SSS:
			sssGameEventDealNew.createRoom(client, object);
			break;
		case CommonConstant.GAME_ID_ZJH:
			zjhGameEventDealNew.createRoom(client, object);
			break;
		case CommonConstant.GAME_ID_BDX:
			bdxGameEventDealNew.createRoom(client, object);
			break;
		case CommonConstant.GAME_ID_QZMJ:
			qzmjGameEventDeal.createRoom(client, object);
			break;
		case CommonConstant.GAME_ID_NAMJ:
			qzmjGameEventDeal.createRoom(client, object);
			break;
		case CommonConstant.GAME_ID_ZZC:
			qzmjGameEventDeal.createRoom(client, object);
			break;
		case CommonConstant.GAME_ID_GP_PJ:
			gppjGameEventDeal.createRoom(client, object);
			break;
		case CommonConstant.GAME_ID_MJ_PJ:
			gppjGameEventDeal.createRoom(client, object);
			break;
		case CommonConstant.GAME_ID_SW:
			swGameEventDeal.createRoom(client, object);
			break;
		case CommonConstant.GAME_ID_DDZ:
			ddzGameEventDeal.createRoom(client, object);
			break;
		case CommonConstant.GAME_ID_PDK:
			pdkGameEventDeal.createRoom(client, object);
			break;
		case CommonConstant.GAME_ID_GDY:// 干瞪眼
			gdyGameEventDeal.createRoom(client, object);
			break;
		case CommonConstant.GAME_ID_SG:// 三公
			sgGameEventDeal.createRoom(client, object);
			break;
		case CommonConstant.GAME_ID_GZMJ:
			gzMjGameEventDeal.createOrJoinRoom(client, object);
			break;
		default:
			break;
		}
	}

	public long addGameRoom(GameRoom gameRoom, Playerinfo playerinfo) {
		JSONObject obj = new JSONObject();
		obj.put("player_number", gameRoom.getPlayerMap().size());
		obj.put("game_id", gameRoom.getGid());
		obj.put("platform", gameRoom.getPlatform());
		obj.put("room_no", gameRoom.getRoomNo());
		obj.put("roomtype", gameRoom.getRoomType());
		obj.put("base_info", gameRoom.getRoomInfo());
		obj.put("createtime", TimeUtil.getNowDate());
		obj.put("game_count", gameRoom.getGameCount());
		if (gameRoom.getPlatform() != null)
			obj.put("platform", gameRoom.getPlatform());
		if (playerinfo != null) {
			obj.put("user_id0", playerinfo.getId());
			obj.put("user_icon0", playerinfo.getHeadimg());
			obj.put("user_name0", playerinfo.getName());
		}
		obj.put("ip", gameRoom.getIp());
		obj.put("port", gameRoom.getPort());
		obj.put("status", 0);
		obj.put("level", gameRoom.getClubCode());
		if (gameRoom.isOpen()) {
			obj.put("open", CommonConstant.GLOBAL_YES);
		} else {
			obj.put("open", CommonConstant.GLOBAL_NO);
		}

		return roomBiz.insertGameRoom(obj);
	}

	public static String randomRoomNo() {
		String roomNo = MathDelUtil.getRandomStr(6);
		if (RoomManage.gameRoomMap.containsKey(roomNo)) {
			return randomRoomNo();
		}
		return roomNo;
	}

	private JSONObject getGameSetting(GameRoom gameRoom) {
		JSONObject gameSetting;
		try {
			Object object = redisService.queryValueByKey(CacheKeyConstant.APP_GAME_SETTING);
			if (object != null) {
				gameSetting = JSONObject.fromObject(redisService.queryValueByKey(CacheKeyConstant.APP_GAME_SETTING));
			} else {
				gameSetting = roomBiz.getGameSetting();
				redisService.insertKey(CacheKeyConstant.APP_GAME_SETTING, String.valueOf(gameSetting), 300L);
			}
		} catch (Exception e) {
			logger.error("请启动REmote DIctionary Server");
			gameSetting = roomBiz.getGameSetting();
		}
		return gameSetting;
	}

	public void joinRoomBase(SocketIOClient client, Object data) {
		
		System.out.println("执行joinRoomBase1");
		JSONObject postData = fromObject(data);
		System.out.println(postData);
		if (!JsonUtil.isNullVal(postData,
				new String[] { CommonConstant.DATA_KEY_ACCOUNT, CommonConstant.DATA_KEY_ROOM_NO })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "参数问题：account,room_no", "enterRoomPush_NN");
			System.out.println("812");
			return;
		}
		// 玩家账号
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		// 房间号
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);

		GameRoom room = RoomManage.gameRoomMap.get(roomNo);
		if (null == room) {
			CommonConstant.sendMsgEventNo(client, "房间不存在", null, "enterRoomPush_NN");
			System.out.println("823");
			return;
		}
		// 如果在房间里排除
		boolean inRoom = true;
		boolean visitPlayer = false;// 用户是否属于观战用户
		if (room.getPlayerMap().containsKey(account) && null != room.getPlayerMap().get(account)) {
			inRoom = false;
		}
		// 观战玩家
		else if (room.getVisitPlayerMap().containsKey(account) && null != room.getVisitPlayerMap().get(account)) {
			visitPlayer = true;// 观战用户
		}

		if (inRoom) {
			// 游戏停服中
			if (!redisInfoService.getStartStatus(null, account)) {
				CommonConstant.sendMsgEventNo(client, CommonConstant.CLOSING_DOWN_MSG, null, "enterRoomPush_NN");
				System.out.println("841");
				return;
			}
		}

		int roomType = room.getRoomType();// 房间类型
		// 如果加入房间类型不是茶楼 验证该玩家是否在茶楼房间里 start
		if (CommonConstant.ROOM_TYPE_TEA != roomType) {
			for (String room_No : RoomManage.gameRoomMap.keySet()) {
				GameRoom gameRoom = RoomManage.gameRoomMap.get(room_No);
				if (null == gameRoom)
					continue;
				// 如果在房间里排除
				if (gameRoom.getPlayerMap().containsKey(account) && null != gameRoom.getPlayerMap().get(account)
						&& CommonConstant.ROOM_TYPE_TEA == gameRoom.getRoomType()) {
					CommonConstant.sendMsgEventNo(client, "您已经在茶楼房间中...", null, "enterRoomPush_NN");
					System.out.println("853");
					return;
				}

			}
		}
		// 如果加入房间类型不是茶楼 验证该玩家是否在茶楼房间里 end

		// 获取用户信息
		JSONObject userInfo = userBiz.getUserByAccount(account);
		if (Dto.isObjNull(userInfo)) {
			CommonConstant.sendMsgEventNo(client, "用户不存在", null, "enterRoomPush_NN");
			System.out.println(857);
			return;
		}

		if (null != client && CommonConstant.ROOM_TYPE_MATCH != roomType) {
			if (!userInfo.containsKey("uuid") || !postData.containsKey("uuid")
					|| Dto.stringIsNULL(userInfo.getString("uuid"))
					|| !userInfo.getString("uuid").equals(postData.getString("uuid"))) {
				CommonConstant.sendMsgEventNo(client, "请刷新后再试", null, "enterRoomPush_NN");
				System.out.println(878);
				return;
			}
		}

		// 房卡 俱乐部 好友局
		if (CommonConstant.ROOM_TYPE_FK == roomType || CommonConstant.ROOM_TYPE_CLUB == roomType
				|| CommonConstant.ROOM_TYPE_FRIEND == room.getRoomType()) {
			if (!Dto.stringIsNULL(room.getPlatform()) && userInfo.containsKey("platform")
					&& !room.getPlatform().equals(userInfo.getString("platform"))) {
				CommonConstant.sendMsgEventNo(client, "房间不存在", null, "enterRoomPush_NN");
				System.out.println(888);
				return;
			}
		}

		if (CommonConstant.ROOM_TYPE_FRIEND == roomType && userInfo.getLong("id") != room.getParUserId()) {
			if (!userInfo.containsKey("parUserId") || room.getParUserId() != userInfo.getLong("parUserId")) {
				CommonConstant.sendMsgEventNo(client, "该房间不允许加入", null, "enterRoomPush_NN");
				System.out.println(896);
				return;
			}
		}

		// 添加工会信息
		if (!Dto.isObjNull(userInfo) && userInfo.containsKey("gulidId") && userInfo.containsKey("unionid")
				&& userInfo.containsKey("platform")) {
			JSONObject ghInfo = userBiz.getGongHui(userInfo);
			if (!Dto.isObjNull(ghInfo) && ghInfo.containsKey("name")) {
				userInfo.put("ghName", ghInfo.getString("name"));
			}
		}
		// 重连不需要再次检查玩家积分
		if ((!room.getPlayerMap().containsKey(account) || null == room.getPlayerMap().get(account)) && !visitPlayer
				|| postData.containsKey("seatIndex")// 观战坐下
		) {
			// 房卡场禁止中途加入提示 wqm 2018/09/12
			if (!room.isHalfwayIn() && room.getGameIndex() > 0) {
				CommonConstant.sendMsgEventNo(client, "已开局", null, "enterRoomPush_NN");
				System.out.println(917);
				return;
			}
			// 俱乐部
			if (CommonConstant.ROOM_TYPE_CLUB == roomType) {
				String clubIds = !userInfo.containsKey("clubIds") ? null : userInfo.getString("clubIds");
				if (Dto.stringIsNULL(clubIds) || !postData.containsKey("clubId")
						|| !new ArrayList<>(Arrays.asList(clubIds.substring(1, clubIds.length()).split("\\$")))
								.contains(postData.getString("clubId"))) {
					CommonConstant.sendMsgEventNo(client, "房间不存在", null, "enterRoomPush_NN");
					System.out.println(927);
					return;
				}

				JSONObject clubInfo = clubBiz.getClubById(postData.getLong("clubId"));
				if (CommonConstant.ROOM_PAY_TYPE_AA.equals(clubInfo.getString("payType"))) {
					String updateType = room.getCurrencyType();
					if (!userInfo.containsKey(updateType) || userInfo.getDouble(updateType) < room.getEnterScore()) {
						CommonConstant.sendMsgEventNo(client, "房卡不足", null, "enterRoomPush_NN");
						System.out.println(928);
						return;
					}
				}

			}
			// 元宝场
			else if (CommonConstant.ROOM_TYPE_YB == roomType && userInfo.containsKey("yuanbao")
					&& userInfo.getDouble("yuanbao") < room.getEnterScore()) {
				// 元宝不足
				CommonConstant.sendMsgEventNo(client, "元宝不足", null, "enterRoomPush_NN");
				System.out.println(947);
				return;
			}
			// 金币场
			else if (CommonConstant.ROOM_TYPE_JB == roomType && userInfo.containsKey("coins")) {
				if (userInfo.getDouble("coins") < room.getEnterScore()) {
					// 金币不足
					CommonConstant.sendMsgEventNo(client, "金币不足", null, "enterRoomPush_NN");
					System.out.println(10);
					return;
				} else if (room.getEnterScore() != room.getLeaveScore()
						&& userInfo.getDouble("coins") > room.getLeaveScore()) {
					// 金币过多
					CommonConstant.sendMsgEventNo(client, "金币超出该场次限定", null, "enterRoomPush_NN");
					System.out.println(11);
					return;
				}
			}
			// 房卡场
			else if (CommonConstant.ROOM_TYPE_FK == roomType) {
				String updateType = room.getCurrencyType();
				if (!userInfo.containsKey(updateType) || userInfo.getDouble(updateType) < room.getEnterScore()) {
					String msg = "房卡不足";
					if ("yuanbao".equals(updateType)) {
						msg = "元宝不足";
					}
					CommonConstant.sendMsgEventNo(client, msg, null, "enterRoomPush_NN");
					System.out.println(12);
					return;
				}
			}
			// 竞技场
			else if (CommonConstant.ROOM_TYPE_COMPETITIVE == roomType && userInfo.containsKey("roomcard")
					&& userInfo.getDouble("roomcard") < room.getEnterScore()) {
				// 金币不足
				CommonConstant.sendMsgEventNo(client, "房卡不足", null, "enterRoomPush_NN");
				System.out.println(13);
				return;
			}
			// 好友局
			else if (CommonConstant.ROOM_TYPE_FRIEND == roomType) {
				if (!userInfo.containsKey("yuanbao") || userInfo.getDouble("yuanbao") < room.getEnterScore()) {
					CommonConstant.sendMsgEventNo(client, "房卡不足", null, "enterRoomPush_NN");
					System.out.println(14);
					return;
				}
			}
			// 亲友圈
			else if (CommonConstant.ROOM_TYPE_FREE == roomType || CommonConstant.ROOM_TYPE_INNING == roomType) {
				// 俱乐部用户加入房间校验 start
				Map<String, String> map = circleService.verifyJoinRoom(userInfo.get("id"), room.getCircleId());

				if (null == map || !map.containsKey("type")) {
					CommonConstant.sendMsgEventNo(client, "请稍后再试", null, "enterRoomPush_NN");
					System.out.println(1001);
					return;
				}
				int type = Integer.valueOf(map.get("type"));
				System.out.println(type);
				if (2 != type && 0 != type) {
					CommonConstant.sendMsgEventNo(client, "请稍后再试", null, "enterRoomPush_NN");
					System.out.println(1007);
					return;
				}
				if (2 == type) {
					CommonConstant.sendMsgEventNo(client, CircleConstant.MEMBER_NOT_USE_MSG, null, "enterRoomPush_NN");
					System.out.println(1012);
					return;
				}
				System.out.println("获取体力");
				double userHp = Double.valueOf(map.get("userHp"));
				if (userHp < room.getEnterScore()) {
					CommonConstant.sendMsgEventNo(client, "能量不足", null, "enterRoomPush_NN");
					return;
				}

				if (CommonConstant.ROOM_PAY_TYPE_AA.equals(String.valueOf(room.getPayType()))// AA支付
						&& Integer.valueOf(map.get("roomcard")) < room.getSinglePayNum()) {
					CommonConstant.sendMsgEventNo(client, "房卡不足", null, "enterRoomPush_NN");
					return;
				}
				// 俱乐部用户加入房间校验 end
				// 存放体力
				userInfo.element("user_hp", userHp);
			}
			// 茶楼
			else if (CommonConstant.ROOM_TYPE_TEA == roomType) {
				// 茶楼余额 验证 start
				int joinRoomType = teaService.verifyJoinRoom(room.getSinglePayNum(), room.getPlayerCount(),
						room.getPayType(), room.getClubCode(), userInfo.get("id"));
				if (TeaConstant.JOIN_ROOM_TYPE_NO == joinRoomType) {
					CommonConstant.sendMsgEventNo(client, "您已经退出该茶楼", null, "enterRoomPush_NN");
					return;
				}
				if (TeaConstant.JOIN_ROOM_TYPE_BLACK_LIST == joinRoomType) {
					CommonConstant.sendMsgEventNo(client, TeaConstant.NOT_USE_MSG, null, "enterRoomPush_NN");
					return;
				}
				if (TeaConstant.JOIN_ROOM_TYPE_NO_1 == joinRoomType) {
					String msg = "茶楼茶壶不足";
					if (CommonConstant.ROOM_PAY_TYPE_TEA_AA.equals(room.getPayType())) {// 4:茶叶AA"
						msg = "您的茶叶不足";
					}
					CommonConstant.sendMsgEventNo(client, msg, null, "enterRoomPush_NN");
					return;
				}
				// 茶楼余额 验证 end
			}
		}
		System.out.println("33333333333333333333333333333");
		userInfo.element("visitPlayer", visitPlayer);// 是否是观战用户

		String joinRoomBaseKey = new StringBuilder().append("joinRoomBase:").append(roomNo).toString();
		try {
			long count = redisService.incr(joinRoomBaseKey, 1);
			if (1L < count) {
				CommonConstant.sendMsgEventNo(client, "房间已满,请稍后再试", null, "enterRoomPush_NN");
				return;
			}
			redisService.expire(joinRoomBaseKey, 10);
		} catch (Exception e) {
		}
		joinRoomBase(client, postData, userInfo);
		redisService.deleteByKey(joinRoomBaseKey);
	}

	public void joinRoomBase(SocketIOClient client, JSONObject postData, JSONObject userInfo) {
		System.out.println("执行joinRoomBase");
		boolean visitPlayer = false;// 是否是观战用户
		if (userInfo.containsKey("visitPlayer")) {// 观战用户重连
			visitPlayer = userInfo.getBoolean("visitPlayer");
		}
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		String account = userInfo.getString("account");
		GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
		//判断是否是人机房 如果是的话  禁止真人进入房间
		
		System.out.println(1);
		if (!visitPlayer) {
			System.out.println(2);
			if (gameRoom.isRobotRoom()) {
				System.out.println(3);
				if (!robotEventDeal.isRobot(account)) {
					CommonConstant.sendMsgEventNo(client, "请稍后再试", null, "enterRoomPush_NN");
					System.out.println("11111111");
					return;
				}
			}
		}
		
		
		if (null == gameRoom) {
			CommonConstant.sendMsgEventNo(client, "请稍后再试", null, "enterRoomPush_NN");
			return;
		}
		
		// 判断是否在其他房间
		for (String room_No : RoomManage.gameRoomMap.keySet()) {
			GameRoom room = RoomManage.gameRoomMap.get(room_No);
			if (null == room)
				continue;
			// 如果在房间里排除
			if (!room_No.equals(roomNo) && room.getPlayerMap().containsKey(account)
					&& null != room.getPlayerMap().get(account)) {
				CommonConstant.sendMsgEventNo(client, "请稍后再试", null, "enterRoomPush_NN");
				return;
			}
		}

		Playerinfo playerinfo;
		JSONObject joinData = new JSONObject();
		joinData.put(CommonConstant.DATA_KEY_ACCOUNT, account);
		joinData.put(CommonConstant.DATA_KEY_ROOM_NO, roomNo);
		

		if (gameRoom.getVisitPlayerMap().containsKey(account) && !postData.containsKey("seatIndex")) {// 观战用户重连 观战玩家坐下
			playerinfo = gameRoom.getVisitPlayerMap().get(account);
			if (null != client) {
				playerinfo.setUuid(client.getSessionId());
			} else {
				playerinfo.setUuid(UUID.randomUUID());
			}
			joinData.put("isReconnect", CommonConstant.GLOBAL_YES);
		} else if (gameRoom.getPlayerMap().containsKey(account)) {// 重连用户 观战玩家坐下
			playerinfo = gameRoom.getPlayerMap().get(account);
			if (null != client) {
				playerinfo.setUuid(client.getSessionId());
			} else {
				playerinfo.setUuid(UUID.randomUUID());
			}
			joinData.put("isReconnect", CommonConstant.GLOBAL_YES);
		} else {
			Integer myIndex = -1;
			// 观战玩家坐下
			if (postData.containsKey("seatIndex")) {
				Integer seatIndex = postData.getInt("seatIndex");
				if (0 > seatIndex || gameRoom.getPlayerCount() <= seatIndex
						|| 0L != gameRoom.getUserIdList().get(seatIndex)
						|| !gameRoom.getIndexList().contains(seatIndex)) {
					CommonConstant.sendMsgEventNo(client, "该座位已经有人", null, "enterRoomPush_NN");
					return;
				}
				myIndex = seatIndex;
				if (!gameRoom.removeIndexList(myIndex)) {// 移除预留座位号
					CommonConstant.sendMsgEventNo(client, "房间已满,请稍后再试", null, "enterRoomPush_NN");
					return;
				}
			}
			if (-1 == myIndex) {
				try {
					myIndex = gameRoom.getIndexList().get(0);
					if (!gameRoom.removeIndexList(myIndex)) {// 移除预留座位号
						CommonConstant.sendMsgEventNo(client, "房间已满,请稍后再试", null, "enterRoomPush_NN");
						return;
					}
				} catch (Exception e) {
				}
			}
			if (myIndex < 0) {
				CommonConstant.sendMsgEventNo(client, "房间已满,请稍后再试", null, "enterRoomPush_NN");
				return;
			}

			try {
				// 亲友圈取第一个进来的人当房主
				if (CommonConstant.GAME_ID_SG != gameRoom.getGid()) {// 三公 不改变房主
					if (gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_FREE
							|| gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_INNING) {
						boolean b = false;// 房主是否存在
						String owner = gameRoom.getOwner();// 房主
						for (String player : gameRoom.getPlayerMap().keySet()) {
							if (owner.equals(player)) {
								b = true;
								break;
							}
						}
						if (!b) {
							if (gameRoom.getPlayerMap().size() == 0) {
								owner = userInfo.getString("account");
							} else {
								for (String player : gameRoom.getPlayerMap().keySet()) {
									owner = player;
									break;
								}
							}

						}

						if (!b) {
							gameRoom.setOwner(owner);
							gameRoom.setBanker(owner);
						}
					}
				}

				if (gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_MATCH && userInfo.containsKey("openid")
						&& "0".equals(userInfo.getString("openid"))) {
					gameRoom.getRobotList().add(userInfo.getString("account"));
					AppBeanUtil.getBean(RobotEventDeal.class).addRobotInfo(userInfo.getString("account"), roomNo,
							gameRoom.getGid());
				}
				// 获取用户信息
				JSONObject obtainPlayerInfoData = new JSONObject();
				obtainPlayerInfoData.put("userInfo", userInfo);
				obtainPlayerInfoData.put("myIndex", myIndex);
				if (client != null) {
					obtainPlayerInfoData.put("uuid", String.valueOf(client.getSessionId()));
				} else {
					obtainPlayerInfoData.put("uuid", String.valueOf(UUID.randomUUID()));
				}
				obtainPlayerInfoData.put("room_type", gameRoom.getRoomType());
				if (postData.containsKey("location")) {
					obtainPlayerInfoData.put("location", postData.getString("location"));
				}
				if (postData.containsKey("my_rank")) {
					obtainPlayerInfoData.put("my_rank", postData.getInt("my_rank"));
				}
				obtainPlayerInfoData.put("gid", gameRoom.getGid());
				obtainPlayerInfoData.put("gameCount", gameRoom.getGameCount());
				obtainPlayerInfoData.put("roomScore", gameRoom.getScore());
				obtainPlayerInfoData.put("roomNo", gameRoom.getRoomNo());
				playerinfo = obtainPlayerInfo(obtainPlayerInfoData);

				// 设置房间属性
				if (!gameRoom.addUserIdMyIndex(myIndex, userInfo.getLong("id"))) {// 添加玩家下标
					CommonConstant.sendMsgEventNo(client, "房间已满,请稍后再试", null, "enterRoomPush_NN");
					gameRoom.addIndexList(myIndex);// 座位号还原
					return;
				}
				gameRoom.getPlayerMap().put(playerinfo.getAccount(), playerinfo);
				gameRoom.getVisitPlayerMap().remove(account);// 移除观战列表
				visitPlayer = false;// 非观战
			} catch (Exception e) {
				CommonConstant.sendMsgEventNo(client, "房间已满,请稍后再试", null, "enterRoomPush_NN");
				gameRoom.addIndexList(myIndex);// 座位号还原
				return;
			}

			// 更新数据库
			if (myIndex < 10 && gameRoom.getRoomType() != CommonConstant.ROOM_TYPE_MATCH) {
				JSONObject roomInfo = new JSONObject();
				roomInfo.put("player_number", gameRoom.getPlayerMap().size());
				roomInfo.put("room_no", gameRoom.getRoomNo());
				roomInfo.put("user_id" + myIndex, playerinfo.getId());
				roomInfo.put("user_icon" + myIndex, playerinfo.getHeadimg());
				roomInfo.put("user_name" + myIndex, playerinfo.getName());
				// 更新房间信息
				producerService.sendMessage(daoQueueDestination,
						new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
			}
			joinData.put("isReconnect", CommonConstant.GLOBAL_NO);
		}

		// 通知玩家
		switch (gameRoom.getGid()) {
		case CommonConstant.GAME_ID_NN:
			// 重连不需要重新设置用户牌局信息 非观战用户
			if (!visitPlayer
					&& !((NNGameRoomNew) gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
				((NNGameRoomNew) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new NNUserPacket());
			}
			nnGameEventDealNew.joinRoom(client, joinData);
			break;
		case CommonConstant.GAME_ID_SSS:
			// 重连不需要重新设置用户牌局信息
			if (!visitPlayer
					&& !((SSSGameRoomNew) gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
				((SSSGameRoomNew) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new SSSUserPacket());
			}
			sssGameEventDealNew.joinRoom(client, joinData);
			break;
		case CommonConstant.GAME_ID_ZJH:
			// 重连不需要重新设置用户牌局信息
			if (!visitPlayer
					&& !((ZJHGameRoomNew) gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
				((ZJHGameRoomNew) gameRoom).getUserPacketMap().put(userInfo.getString("account"),
						new com.zhuoan.biz.model.zjh.UserPacket());
			}
			zjhGameEventDealNew.joinRoom(client, joinData);
			break;
		case CommonConstant.GAME_ID_BDX:
			// 重连不需要重新设置用户牌局信息
			if (!visitPlayer
					&& !((BDXGameRoomNew) gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
				((BDXGameRoomNew) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new UserPackerBDX());
			}
			bdxGameEventDealNew.joinRoom(client, joinData);
			break;
		case CommonConstant.GAME_ID_QZMJ:
			// 重连不需要重新设置用户牌局信息
			if (!visitPlayer
					&& !((QZMJGameRoom) gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
				((QZMJGameRoom) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new QZMJUserPacket());
			}
			qzmjGameEventDeal.joinRoom(client, joinData);
			break;
		case CommonConstant.GAME_ID_ZZC:
			// 重连不需要重新设置用户牌局信息
			if (!visitPlayer
					&& !((QZMJGameRoom) gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
				((QZMJGameRoom) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new QZMJUserPacket());
			}
			qzmjGameEventDeal.joinRoom(client, joinData);
			break;
		case CommonConstant.GAME_ID_NAMJ:
			// 重连不需要重新设置用户牌局信息
			if (!visitPlayer
					&& !((QZMJGameRoom) gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
				((QZMJGameRoom) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new QZMJUserPacket());
			}
			qzmjGameEventDeal.joinRoom(client, joinData);
			break;
		case CommonConstant.GAME_ID_GP_PJ:
			// 重连不需要重新设置用户牌局信息
			if (!visitPlayer
					&& !((GPPJGameRoom) gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
				((GPPJGameRoom) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new UserPacketGPPJ());
			}
			gppjGameEventDeal.joinRoom(client, joinData);
			break;
		case CommonConstant.GAME_ID_MJ_PJ:
			// 重连不需要重新设置用户牌局信息
			if (!visitPlayer
					&& !((GPPJGameRoom) gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
				((GPPJGameRoom) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new UserPacketGPPJ());
			}
			gppjGameEventDeal.joinRoom(client, joinData);
			break;
		case CommonConstant.GAME_ID_SW:
			swGameEventDeal.joinRoom(client, joinData);
			break;
		case CommonConstant.GAME_ID_DDZ:
			// 重连不需要重新设置用户牌局信息
			if (!visitPlayer
					&& !((DdzGameRoom) gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
				UserPacketDdz up = new UserPacketDdz();
				// 设置连胜局数
				Object winTimeInfo = redisService.hget("win_time_info_" + gameRoom.getScore(),
						userInfo.getString("account"));
				if (!Dto.isNull(winTimeInfo)) {
					up.setWinStreakTime(Integer.parseInt(String.valueOf(winTimeInfo)));
				}
				((DdzGameRoom) gameRoom).getUserPacketMap().put(userInfo.getString("account"), up);
			}
			ddzGameEventDeal.joinRoom(client, joinData);
			break;
		case CommonConstant.GAME_ID_PDK:
			// 重连不需要重新设置用户牌局信息
			if (!visitPlayer
					&& !((PDKGameRoom) gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
				((PDKGameRoom) gameRoom).getUserPacketMap().put(account, new PDKUserPacket(account));
			}
			pdkGameEventDeal.joinRoom(client, joinData);
			break;
		case CommonConstant.GAME_ID_GDY:// 干瞪眼
			// 重连不需要重新设置用户牌局信息
			if (!visitPlayer
					&& !((GDYGameRoom) gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
				((GDYGameRoom) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new GDYUserPacket());
			}
			gdyGameEventDeal.joinRoom(client, joinData);
			break;
		case CommonConstant.GAME_ID_SG:// 三公
			// 重连不需要重新设置用户牌局信息
			if (!visitPlayer
					&& !((SGGameRoom) gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
				((SGGameRoom) gameRoom).getUserPacketMap().put(account, new SGUserPacket(account));
			}
			sgGameEventDeal.joinRoom(client, joinData);
			break;
		case CommonConstant.GAME_ID_GZMJ:
			gzMjGameEventDeal.createOrJoinRoom(client, joinData);
			break;
		default:
			break;
		}
	}

	public Playerinfo obtainPlayerInfo(JSONObject data) {
		JSONObject userInfo = data.getJSONObject("userInfo");
		int myIndex = data.getInt("myIndex");
		UUID uuid = UUID.fromString(data.getString("uuid"));
		int roomType = data.getInt("room_type");
		int gameId = data.getInt("gid");// 游戏id
		String roomNo = data.getString("roomNo");
		Playerinfo playerinfo = new Playerinfo();
		playerinfo.setId(userInfo.getLong("id"));
		playerinfo.setAccount(userInfo.getString("account"));
		if (RoomManage.gameConfigMap.containsKey(playerinfo.getAccount())) {
			userInfo.putAll(JSONObject.fromObject(RoomManage.gameConfigMap.get(playerinfo.getAccount())));
		}
		playerinfo.setName(userInfo.getString("name"));
		playerinfo.setUuid(uuid);
		playerinfo.setMyIndex(myIndex);
		if (roomType == CommonConstant.ROOM_TYPE_JB) {
			// 金币模式
			playerinfo.setScore(userInfo.getDouble("coins"));
		} else if (roomType == CommonConstant.ROOM_TYPE_YB) { // 元宝模式
			if (userInfo.containsKey("yuanbao")) {
				double yuanbao = userInfo.getDouble("yuanbao");
				// 干瞪眼 三公 泉州麻将
				if (CommonConstant.GAME_ID_GDY != gameId && CommonConstant.GAME_ID_SG != gameId
						&& CommonConstant.GAME_ID_QZMJ != gameId)
					playerinfo.setScore(yuanbao);

				playerinfo.setSourceScore(yuanbao);
			}

		} else if (roomType == CommonConstant.ROOM_TYPE_COMPETITIVE) {
			// 竞技场
			playerinfo.setScore(userInfo.getDouble("score"));
		} else if (roomType == CommonConstant.ROOM_TYPE_FREE || roomType == CommonConstant.ROOM_TYPE_INNING) {
			if (userInfo.containsKey("user_hp")) {
				double userHp = userInfo.getDouble("user_hp");
				// 干瞪眼 三公 泉州麻将 十三水 新跑得快 陆续修改其他游戏
				if (CommonConstant.GAME_ID_GDY != gameId && CommonConstant.GAME_ID_SG != gameId
						&& CommonConstant.GAME_ID_QZMJ != gameId && CommonConstant.GAME_ID_SSS != gameId
						&& CommonConstant.GAME_ID_PDK != gameId) {
					playerinfo.setScore(userHp);
				} else {
					playerinfo.setSourceScore(userHp);
				}
			}
		} else {
			// 房卡模式 茶楼
			playerinfo.setScore(0);
		}

		// 麻将一课
		if (data.containsKey("gameCount") && data.containsKey("roomScore")) {
			int gameCount = data.getInt("gameCount");
			if (QZMJConstant.QZMJ_KE_GAME_COUNT == gameCount) {
				if (CommonConstant.GAME_ID_QZMJ == gameId) {// 泉州麻将
					QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
					playerinfo.setSourceScore(room.getKeScoreVal());
				} else if (CommonConstant.GAME_ID_QZMJ == gameId) {// 南安麻将
					playerinfo.setSourceScore(15D);
				}
			}
		}

		playerinfo.setHeadimg(userInfo.containsKey("headimg") ? userInfo.getString("headimg") : "");
		playerinfo.setSex(userInfo.containsKey("sex") ? userInfo.getString("sex") : "");
		if (userInfo.containsKey("ip")) {
			playerinfo.setIp(userInfo.getString("ip"));
		} else {
			playerinfo.setIp("");
		}
		if (userInfo.containsKey("sign")) {
			playerinfo.setSignature(userInfo.getString("sign"));
		} else {
			playerinfo.setSignature("");
		}
		if (userInfo.containsKey("ghName") && !Dto.stringIsNULL(userInfo.getString("ghName"))) {
			playerinfo.setGhName(userInfo.getString("ghName"));
		}
		if (userInfo.containsKey("roomcard")) {
			playerinfo.setRoomCardNum(userInfo.getInt("roomcard"));
		}
		if (userInfo.containsKey("area")) {
			playerinfo.setArea(userInfo.getString("area"));
		} else {
			playerinfo.setArea("");
		}
		// 平台标识
		if (userInfo.containsKey("platform")) {
			playerinfo.setPlatform(userInfo.getString("platform"));
		} else {
			playerinfo.setArea("");
		}
		if (userInfo.containsKey("lv")) {
			int vip = userInfo.getInt("lv");
			if (vip > 1) {
				playerinfo.setVip(vip - 1);
			} else {
				playerinfo.setVip(0);
			}
		}
		playerinfo.setStatus(Constant.ONLINE_STATUS_YES);
		// 保存用户坐标
		if (data.containsKey("location")) {
			playerinfo.setLocation(data.getString("location"));
		}
		if (data.containsKey("my_rank")) {
			playerinfo.setMyRank(data.getInt("my_rank"));
		}
//        if (userInfo.containsKey("luck")) {
//            playerinfo.setLuck(userInfo.getInt("luck"));
//        }
		if (userInfo.containsKey("Identification")) {
			int luck = -1;
			try {
				JSONObject identification = userInfo.getJSONObject("Identification");
				luck = identification.getInt(String.valueOf(gameId));
				if (luck > 57)
					luck = 57;
			} catch (Exception e) {
			}
			playerinfo.setLuck(luck);
		}
		if (userInfo.containsKey("openid")) {
			playerinfo.setOpenId(userInfo.getString("openid"));
		}

		return playerinfo;
	}

	public void createRoomNN(NNGameRoomNew room, JSONObject baseInfo, String account) {
		room.setBankerType(baseInfo.getInt("playing"));
		// 玩法
		String wanFa = "";
		switch (room.getBankerType()) {
		case 10:
			wanFa = "霸王庄";
			break;
		case NNConstant.NN_BANKER_TYPE_LZ:
			wanFa = "轮庄";
			break;
		case 50:
			wanFa = "自由抢庄";
			break;
		case 30:
			wanFa = "明牌抢庄";
			break;
		case NNConstant.NN_BANKER_TYPE_NN:
			wanFa = "牛牛上庄";
			break;
		case NNConstant.NN_BANKER_TYPE_TB:
			wanFa = "通比牛牛";
			break;
		case NNConstant.NN_BANKER_TYPE_ZZ:
			wanFa = "坐庄模式";
			break;
		case NNConstant.NN_BANKER_TYPE_GN:
			wanFa = "斗公牛";
			break;
		default:
			break;
		}
		room.setWfType(wanFa);
		// 庄家
		room.setBanker(account);
		// 房主
		room.setOwner(account);
		JSONObject setting = redisInfoService.getGameInfoById(CommonConstant.GAME_ID_NN).getJSONObject("setting");
		// 设置基本牌型倍数
		if (baseInfo.containsKey("special")) {
			List<Integer> specialType = new ArrayList<>();
			JSONArray types = baseInfo.getJSONArray("special");
			for (int i = 0; i < types.size(); i++) {
				int type = types.getJSONObject(i).getInt("type");
				int value = types.getJSONObject(i).getInt("value");
				specialType.add(type);
				// 设置特殊牌型的倍率
				room.ratio.put(type, value);
			}
			room.setSpecialType(specialType);
		}
		room.setSetting(setting);
		// 抢庄是否要加倍
		if (baseInfo.containsKey("qzTimes")) {
			room.qzTimes = baseInfo.getJSONArray("qzTimes");
		} else {
			room.qzTimes.add(1);
			room.qzTimes.add(2);
			room.qzTimes.add(3);
		}
		// 抢庄是否是随机庄（随机、最高倍数为庄）
		if (baseInfo.containsKey("qzsjzhuang")) {
			room.setSjBanker(baseInfo.getInt("qzsjzhuang"));
		} else {
			room.setSjBanker(0);
		}
		// 没人抢庄
		if (baseInfo.containsKey("qznozhuang") && baseInfo.getInt("qznozhuang") == NNConstant.NN_QZ_NO_BANKER_CK) {
			// 无人抢庄，重新发牌
			room.setQzNoBanker(NNConstant.NN_QZ_NO_BANKER_CK);
		} else if (baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_YB) {
			// 无人抢庄，房间自动解散
			room.setQzNoBanker(NNConstant.NN_QZ_NO_BANKER_JS);
		} else if (NNConstant.NN_BANKER_TYPE_QZ == room.getBankerType()// 自由抢庄 牛牛上庄
				|| NNConstant.NN_BANKER_TYPE_NN == room.getBankerType()) {
			// 无人抢庄，随机庄
			room.setQzNoBanker(NNConstant.NN_QZ_NO_BANKER_SJ);
		} else {
			// 无人抢庄，随机庄
			room.setQzNoBanker(NNConstant.NN_QZ_NO_BANKER_SJ);
		}
		// 通比和斗公牛/牛牛上庄 游戏筹码取底注
		if (NNConstant.NN_BANKER_TYPE_TB == room.getBankerType()
				|| NNConstant.NN_BANKER_TYPE_GN == room.getBankerType()) {
			JSONArray array = new JSONArray();
			JSONObject obj = new JSONObject();
			obj.put("val", (int) baseInfo.getDouble("yuanbao"));
			obj.put("name", (int) baseInfo.getDouble("yuanbao"));
			array.add(obj);
			room.setBaseNum(array);
		} else {
			if (baseInfo.containsKey("baseNum")) {
				// 设置基础倍率
				room.setBaseNum(baseInfo.getJSONArray("baseNum"));
			}
		}
		// 是否推注
		if (baseInfo.containsKey("tzTimes")) {
			room.setTuiZhuTimes(baseInfo.getInt("tzTimes"));
		} else {
			room.setTuiZhuTimes(0);
		}
		// 癞子
		String feature = NNConstant.NN_FEATURE_WU;
		if (baseInfo.containsKey("feature")) {
			feature = baseInfo.getString("feature");
		}
		room.setFeature(feature);
		int roomType = baseInfo.getInt("roomType");
		// 开房者不进入房间
		if (CommonConstant.ROOM_TYPE_DK != roomType && CommonConstant.ROOM_TYPE_FREE != roomType
				&& CommonConstant.ROOM_TYPE_INNING != roomType && CommonConstant.ROOM_TYPE_TEA != roomType) {
			room.getUserPacketMap().put(account, new NNUserPacket());
		}
		room.setSetting(setting);
	}

	public void createRoomSSS(SSSGameRoomNew room, JSONObject baseInfo, String account) {
		int bankerType = SSSConstant.SSS_BANKER_TYPE_HB;
		if (baseInfo.containsKey("type")) {
			bankerType = baseInfo.getInt("type");
		}
		room.setBankerType(bankerType);
		// 玩法
		String wanFa = "";
		switch (bankerType) {
		case SSSConstant.SSS_BANKER_TYPE_BWZ:
			wanFa = "霸王庄";
			break;
		case SSSConstant.SSS_BANKER_TYPE_HB:
			wanFa = "通比";
			break;
		case SSSConstant.SSS_BANKER_TYPE_ZZ:
			wanFa = "坐庄模式";
			break;
		default:
			break;
		}
		room.setWfType(wanFa);
		// 庄家
		room.setBanker(account);
		// 房主
		room.setOwner(account);

		// 加色
		if (baseInfo.containsKey("color")) {
			room.setColor(baseInfo.getInt("color"));
		}
		// 马牌
		if (baseInfo.containsKey("jiama")) {
			room.setMaPaiType(baseInfo.getInt("jiama"));
			switch (room.getMaPaiType()) {
			case SSSConstant.SSS_MP_TYPE_ALL_HT:
				room.setMaPai("1-" + (RandomUtils.nextInt(13) + 1));
				break;
			case SSSConstant.SSS_MP_TYPE_HT_A:
				room.setMaPai("1-1");
				break;
			case SSSConstant.SSS_MP_TYPE_HT_3:
				room.setMaPai("1-3");
				break;
			case SSSConstant.SSS_MP_TYPE_HT_5:
				room.setMaPai("1-5");
				break;
			case SSSConstant.SSS_MP_TYPE_HT_10:
				room.setMaPai("1-10");
				break;
			case SSSConstant.SSS_MP_TYPE_ALL:
				int num = RandomUtils.nextInt(13) + 1;
				int color = RandomUtils.nextInt(4) + 1;
				room.setMaPai(color + "-" + num);
				break;
			default:
				room.setMaPaiType(SSSConstant.SSS_MP_TYPE_ALL_W);
				room.setMaPai("0-0");
				break;
			}
		}
		if (baseInfo.containsKey("baseNum")) {
			// 设置基础倍率
			room.setBaseNum(String.valueOf(baseInfo.getJSONArray("baseNum")));
		}
		/* 获取游戏信息设置,插入缓存 */
		JSONObject setting = redisInfoService.getGameInfoById(CommonConstant.GAME_ID_SSS).getJSONObject("setting");

		room.setSetting(setting);
		int roomType = baseInfo.getInt("roomType");
//是否满人开始
		String isAllStart = CommonConstant.IS_ALL_START_N;
		if (baseInfo.containsKey("isAllStart")) {
			isAllStart = baseInfo.getString("isAllStart");
		}
		room.setIsAllStart(isAllStart);
//全垒打得分翻倍
		String fire = SSSConstant.FIRE_Y;
		if (baseInfo.containsKey("fire")) {
			fire = baseInfo.getString("fire");
		}
		room.setFire(fire);
//全垒打固定得分
		int fireScore = SSSConstant.FIRE_SCORE;
		if (setting.containsKey("fireScore")) {
			fireScore = setting.getInt("fireScore");
		}
		room.setFireScore(fireScore);
		// 配牌倒计时
		int pattern = SSSConstant.SSS_TIMER_GAME_EVENT;
		if (baseInfo.containsKey("pattern") && baseInfo.getInt("pattern") >= 0) {
			pattern = baseInfo.getInt("pattern") * 60;
		}
		room.setPattern(pattern);

		// 癞子
		String feature = SSSConstant.SSS_FEATURE_WU;
		if (baseInfo.containsKey("feature")) {
			feature = baseInfo.getString("feature");
		}
		room.setFeature(feature);

		// 硬鬼数量
		int hardGhostNumber = 0;
		if (baseInfo.containsKey("hardGhostNumber") && Dto.isNumber(baseInfo.getString("hardGhostNumber"))) {
			hardGhostNumber = baseInfo.getInt("hardGhostNumber");
			if (hardGhostNumber > 9) {
				hardGhostNumber = 9;
			} else if (hardGhostNumber < 0) {
				hardGhostNumber = 0;
			}
		}
		// 设置硬鬼数量
		room.setHardGhostNumber(hardGhostNumber);

		// 极速模式
		String speedMode = SSSConstant.SSS_SPEED_MODE_NO;
		if (baseInfo.containsKey("speedMode")) {
			speedMode = baseInfo.getString("speedMode");
		}
		room.setSpeedMode(speedMode);
		//软鬼数量
		int ruanguiCount = SSSConstant.SSS_RUANGUI_COUNT_NO;
		if (baseInfo.containsKey("ruanguiCount")) {
			ruanguiCount=baseInfo.getInt("ruanguiCount");
		}
		room.setRuanguiCount(ruanguiCount);

		// 切牌
		//String cutCardMode = SSSConstant.SSS_CUT_CARD_MODE_NO;
		String cutCardMode = SSSConstant.SSS_CUT_CARD_MODE_YES;
		// if (baseInfo.containsKey("cutCardMode")) {
		// cutCardMode = baseInfo.getString("cutCardMode");
		// }
		room.setCutCardMode(cutCardMode);

		// 开房者不进入房间
		if (CommonConstant.ROOM_TYPE_DK != roomType && CommonConstant.ROOM_TYPE_FREE != roomType
				&& CommonConstant.ROOM_TYPE_INNING != roomType && CommonConstant.ROOM_TYPE_TEA != roomType) {
			room.getUserPacketMap().put(account, new SSSUserPacket());
		}

		// 房间是否支持观战
		room.setVisit(CommonConstant.ROOM_VISIT_YES);

		// 获取是否观战信息
		if (baseInfo.containsKey("visit") && Dto.isNumber(baseInfo.get("visit"))) {
			room.setVisit(baseInfo.getInt("visit"));
		}

	}

	public void createRoomGPPJ(GPPJGameRoom room, JSONObject baseInfo, String account, int gameId) {
		room.setBankerType(baseInfo.getInt("type"));
		// 玩法
		String wanFa = "";
		switch (baseInfo.getInt("type")) {
		case GPPJConstant.BANKER_TYPE_OWNER:
			wanFa = "房主坐庄";
			break;
		case GPPJConstant.BANKER_TYPE_LOOK:
			wanFa = "看牌抢庄";
			break;
		case GPPJConstant.BANKER_TYPE_COMPARE:
			wanFa = "通比";
			break;
		case GPPJConstant.BANKER_TYPE_TURN:
			wanFa = "轮庄";
			break;
		case GPPJConstant.BANKER_TYPE_ROB:
			wanFa = "抢庄";
			break;
		default:
			break;
		}
		room.setWfType(wanFa);
		// 庄家
		room.setBanker(account);
		// 房主
		room.setOwner(account);
		JSONObject setting = redisInfoService.getGameInfoById(gameId).getJSONObject("setting");
		room.setSetting(setting);
		// 下注倍数
		if (baseInfo.containsKey("baseNum")) {
			room.setBaseNum(baseInfo.getJSONArray("baseNum"));
		} else {
			room.setBaseNum(setting.getJSONArray("xzTimes"));
		}
		// 抢庄倍数
		room.setQzTimes(setting.getJSONArray("qzTimes"));
		// 倍数
		if (baseInfo.containsKey("multiple")) {
			room.setMultiple(baseInfo.getInt("multiple"));
		}
	}

	public void createRoomQZMJ(QZMJGameRoom room, JSONObject baseInfo, String account, int gameId) {
		if (gameId == CommonConstant.GAME_ID_QZMJ) {
			room.setWfType("泉州麻将");
		} else if (gameId == CommonConstant.GAME_ID_ZZC) {
			room.setWfType("支支长");
		}
		if (baseInfo.containsKey("type")) {
			room.setYouJinScore(baseInfo.getInt("type"));
		}
		room.setPaiCount(QZMJConstant.HAND_PAI_COUNT);

		// 光游
		boolean isGuangYou = false;
		if (baseInfo.getJSONObject("turn").containsKey("guangyou")) {
			isGuangYou = true;
		}
		room.setGuangYou(isGuangYou);

		// 有金不平胡
		int hasJinNoPingHu = QZMJConstant.HAS_JIN_NO_PING_HU;
		if (baseInfo.containsKey("hasjinnopinghu")) {
			hasJinNoPingHu = baseInfo.getInt("hasjinnopinghu");
		}
		room.setHasJinNoPingHu(hasJinNoPingHu);

		// 是否没有吃、胡
		boolean isNotChiHu = false;
		if (baseInfo.containsKey("isNotChiHu") && 1 == baseInfo.getInt("isNotChiHu")) {
			isNotChiHu = true;
		}
		room.setNotChiHu(isNotChiHu);

		// 一课 课 数
		int keScoreVal = 100;
		// 连庄分数
		int bankerScore = 5;
		if (baseInfo.containsKey("di")) {
			keScoreVal *= baseInfo.getInt("di");
			bankerScore *= baseInfo.getInt("di");
		}
		room.setKeScoreVal(keScoreVal);
		room.setBankerScore(bankerScore);

		// 庄闲
		int zhuangScore = 2;
		int xianScore = 1;
		if (baseInfo.containsKey("zhuangXian")) {
			String zhuangXian = baseInfo.getString("zhuangXian");
			String[] str = zhuangXian.split("/");
			if (null != str && str.length == 2) {
				try {
					zhuangScore = Integer.valueOf(str[0]);
					xianScore = Integer.valueOf(str[1]);
				} catch (Exception e) {
					logger.info("麻将房间庄闲配置错误");
				}
			}
		}
		room.setZhuangScore(zhuangScore);
		room.setXianScore(xianScore);

		// 番倍
		int fanMultiple = 1;
		if (baseInfo.containsKey("fanMultiple")) {
			fanMultiple = baseInfo.getInt("fanMultiple");
		}
		room.setFanMultiple(fanMultiple);

		// 设置游金奖励分数
		if (baseInfo.containsKey("yjRewardScore")) {
			room.setYjRewardScore(baseInfo.getInt("yjRewardScore"));
		} else {
			room.setYjRewardScore(0);
		}
		// 设置双游奖励分数
		if (baseInfo.containsKey("shyRewardScore")) {
			room.setShyRewardScore(baseInfo.getInt("shyRewardScore"));
		} else {
			room.setShyRewardScore(0);
		}
		// 设置三游奖励分数
		if (baseInfo.containsKey("syRewardScore")) {
			room.setSyRewardScore(baseInfo.getInt("syRewardScore"));
		} else {
			room.setSyRewardScore(0);
		}
		// 设置速三奖励分数
		if (baseInfo.containsKey("ssRewardScore")) {
			room.setSsRewardScore(baseInfo.getInt("ssRewardScore"));
		} else {
			room.setSsRewardScore(0);
		}

		// 出牌时间
		int playTime = 10;
		if (baseInfo.containsKey("playTime")) {
			playTime = baseInfo.getInt("playTime");
		}
		room.setPlayTime(playTime);

		JSONObject setting = redisInfoService.getGameInfoById(gameId).getJSONObject("setting");
		// 设置房间信息
		room.setSetting(setting);
		// 庄家
		room.setBanker(account);
		// 房主
		room.setOwner(account);
		int roomType = baseInfo.getInt("roomType");
		// 开房者不进入房间
		if (CommonConstant.ROOM_TYPE_DK != roomType && CommonConstant.ROOM_TYPE_FREE != roomType
				&& CommonConstant.ROOM_TYPE_INNING != roomType && CommonConstant.ROOM_TYPE_TEA != roomType) {
			room.getUserPacketMap().put(account, new QZMJUserPacket());
		}
		if (room.getRoomType() != CommonConstant.ROOM_TYPE_FREE) {// 自由场
			room.setNeedFinalSummary(true);// 需要总结算
		}
	}

	public void createRoomNAMJ(QZMJGameRoom room, JSONObject baseInfo, String account) {
		room.setWfType("南安麻将");
		if (baseInfo.containsKey("type")) {
			room.setYouJinScore(baseInfo.getInt("type"));
		}
		room.setPaiCount(QZMJConstant.HAND_PAI_COUNT);
		// 光游
		room.isGuangYou = false;
		// 有金不平胡
		room.hasJinNoPingHu = QZMJConstant.HAS_JIN_NO_PING_HU;
		// 没有吃，平胡
		room.isNotChiHu = true;

		JSONObject setting = redisInfoService.getGameInfoById(CommonConstant.GAME_ID_NAMJ).getJSONObject("setting");
		// 设置房间信息
		room.setSetting(setting);
		// 庄家
		room.setBanker(account);
		// 房主
		room.setOwner(account);
		int roomType = baseInfo.getInt("roomType");
		// 开房者不进入房间
		if (CommonConstant.ROOM_TYPE_DK != roomType && CommonConstant.ROOM_TYPE_FREE != roomType
				&& CommonConstant.ROOM_TYPE_INNING != roomType && CommonConstant.ROOM_TYPE_TEA != roomType) {
			room.getUserPacketMap().put(account, new QZMJUserPacket());
		}
	}

	public void createRoomSw(SwGameRoom room, JSONObject baseInfo, String account) {
		room.setWfType("水蛙");
		// 庄家
		room.setBanker(account);
		// 房主
		room.setOwner(account);
		// 设置赔率
		switch (baseInfo.getInt("type")) {
		case SwConstant.SW_TYPE_TEN:
			room.setRatio(10);
			break;
		case SwConstant.SW_TYPE_TEN_POINT_FIVE:
			room.setRatio(10.5);
			break;
		case SwConstant.SW_TYPE_TEN_ELEVEN:
			room.setRatio(11);
			break;
		case SwConstant.SW_TYPE_TEN_ELEVEN_POINT_FIVE:
			room.setRatio(11.5);
			break;
		case SwConstant.SW_TYPE_TEN_TWELVE:
			room.setRatio(12);
			break;
		default:
			room.setRatio(10);
			break;
		}
		// 下注倍数
		JSONObject setting = redisInfoService.getGameInfoById(CommonConstant.GAME_ID_SW).getJSONObject("setting");
		// 设置房间信息
		room.setSetting(setting);
		if (baseInfo.containsKey("singleMax") && baseInfo.getInt("singleMax") >= 0) {
			room.setSingleMax(baseInfo.getInt("singleMax"));
		} else if (baseInfo.containsKey("yuanbao")) {
			room.setSingleMax(baseInfo.getInt("yuanbao") * 180);
		}
		if (baseInfo.containsKey("baseNum")) {
			room.setBaseNum(baseInfo.getJSONArray("baseNum"));
		} else {
			room.setBaseNum(setting.getJSONArray("xzTimes"));
		}
	}

	public void createRoomDdz(DdzGameRoom room, JSONObject baseInfo, String account) {
		room.setWfType("斗地主");
		// 庄家
		room.setBanker(account);
		// 房主
		room.setOwner(account);
		// 添加牌局信息
		if (baseInfo.getInt("roomType") != CommonConstant.ROOM_TYPE_DK) {
			UserPacketDdz up = new UserPacketDdz();
			if (baseInfo.containsKey("di")) {
				Object winTimeInfo = redisService.hget("win_time_info_" + baseInfo.getDouble("di"), account);
				if (!Dto.isNull(winTimeInfo)) {
					up.setWinStreakTime(Integer.parseInt(String.valueOf(winTimeInfo)));
				}
			}
			room.getUserPacketMap().put(account, up);
		}
		JSONObject setting = redisInfoService.getGameInfoById(CommonConstant.GAME_ID_DDZ).getJSONObject("setting");
		if (baseInfo.containsKey("win_streak")) {
			JSONArray winStreakArray = setting.getJSONArray("win_streak_array");
			for (Object winStreak : winStreakArray) {
				JSONObject winStreakObj = JSONObject.fromObject(winStreak);
				if (baseInfo.getInt("di") == winStreakObj.getInt("di")) {
					room.setWinStreakObj(winStreakObj);
					break;
				}
			}
		}
		// 设置房间信息
		room.setSetting(setting);
		// 最大倍数
		if (baseInfo.containsKey("multiple")) {
			room.setMaxMultiple(baseInfo.getInt("multiple"));
		}
	}

	public void createRoomPdk(PDKGameRoom room, JSONObject baseInfo, String account, int gameId) {
		room.setWfType("跑得快");
		JSONObject setting = redisInfoService.getGameInfoById(gameId).getJSONObject("setting");
		room.setSetting(setting); // 设置房间信息
		room.setOwner(account); // 房主
		room.setGameStatus(GDYConstant.GDY_GAME_STATUS_INIT);
		int roomType = baseInfo.getInt("roomType");

		// 是否需要总结算 房卡 俱乐部局数场
		if (CommonConstant.ROOM_TYPE_FK == roomType || CommonConstant.ROOM_TYPE_INNING == roomType
				|| CommonConstant.ROOM_TYPE_TEA == roomType)
			room.setNeedFinalSummary(true);
		else
			room.setNeedFinalSummary(false);
		// 开房者不进入房间
		if (CommonConstant.ROOM_TYPE_DK != roomType && CommonConstant.ROOM_TYPE_FREE != roomType
				&& CommonConstant.ROOM_TYPE_INNING != roomType && CommonConstant.ROOM_TYPE_TEA != roomType) {
			room.getUserPacketMap().put(account, new PDKUserPacket(account));
		}
		// 玩法选择
		int waysType = PDKConstant.WAYS_TYPE_16;
		if (baseInfo.containsKey("waysType")) {
			waysType = baseInfo.getInt("waysType");
		}
		room.setWaysType(waysType);
		// 先出规则
		int payRules = PDKConstant.PAY_RULES_3;
		if (baseInfo.containsKey("payRules")) {
			payRules = baseInfo.getInt("payRules");
		}
		room.setPayRules(payRules);
		// 是否反春
		int passCard = PDKConstant.PASS_CARD_0;
		if (baseInfo.containsKey("passCard")) {
			passCard = baseInfo.getInt("passCard");
		}
		room.setPassCard(passCard);
		// 结算
		int settleType = PDKConstant.SETTLE_TYPE_1;
		if (baseInfo.containsKey("settleType")) {
			settleType = baseInfo.getInt("settleType");
		}
		room.setSettleType(settleType);
		// 三张带2
		room.setThreeType(PDKConstant.THREE_TYPE_2);
		// 四张带3
		room.setFourType(PDKConstant.FOUR_TYPE_3);
		// 炸弹倍数
		int bombsScore = PDKConstant.BOMBS_SCORE_2;
		if (baseInfo.containsKey("bombsScore")) {
			bombsScore = baseInfo.getInt("bombsScore");
		}
		if (baseInfo.containsKey("yuanbao")) {
			// 底分
			room.setScore(baseInfo.getInt("yuanbao"));
		}
		// 出牌时间
		int playTime = PDKConstant.TIMER_PLAY;
		if (baseInfo.containsKey("playTime")) {
			playTime = baseInfo.getInt("playTime");
		}
		room.setPlayTime(playTime);
		room.setBombsScore(bombsScore);
	}

	public void createRoomGdy(GDYGameRoom room, JSONObject baseInfo, String account, int gameId) {
		room.setWfType("干瞪眼");// 游戏信息
		JSONObject setting = redisInfoService.getGameInfoById(gameId).getJSONObject("setting");
		room.setSetting(setting); // 设置房间信息
		room.setOwner(account); // 房主
		room.setGameStatus(GDYConstant.GDY_GAME_STATUS_INIT);
		int roomType = baseInfo.getInt("roomType");

		// 是否需要总结算 房卡 俱乐部局数场
		if (CommonConstant.ROOM_TYPE_FK == roomType || CommonConstant.ROOM_TYPE_INNING == roomType)
			room.setNeedFinalSummary(true);
		else
			room.setNeedFinalSummary(false);
		// 开房者不进入房间
		if (CommonConstant.ROOM_TYPE_DK != roomType && CommonConstant.ROOM_TYPE_FREE != roomType
				&& CommonConstant.ROOM_TYPE_INNING != roomType && CommonConstant.ROOM_TYPE_TEA != roomType) {
			room.getUserPacketMap().put(account, new GDYUserPacket());
		}

	}

	public void createRoomSG(SGGameRoom room, JSONObject baseInfo, String account, int gameId) {
		room.setWfType("三公");// 游戏信息
		JSONObject setting = redisInfoService.getGameInfoById(gameId).getJSONObject("setting");
		room.setSetting(setting); // 设置房间信息
		room.setOwner(account); // 房主
		room.setGameStatus(SGConstant.GAME_STATUS_INIT);
		int roomType = baseInfo.getInt("roomType");

		int palyType = SGConstant.PLAY_TYPE_MPQZ;// 明牌抢庄
		if (baseInfo.containsKey("type"))// 玩法 1 明牌抢庄 2 大吃小
			palyType = baseInfo.getInt("type");
		room.setPlayType(palyType);

		int specialType = SGConstant.SPECIAL_TYPE_PT;// 特殊玩法
		if (baseInfo.containsKey("special"))// 特殊玩法类型 1 普通玩法 2 疯狂玩法
			specialType = baseInfo.getInt("special");
		room.setSpecialType(specialType);

		boolean isLaiZi = false;
		if (baseInfo.containsKey("isLaiZi"))// 是否疯狂王癞
			isLaiZi = baseInfo.getBoolean("isLaiZi");
		room.setLaiZi(isLaiZi);

		int bolus = 1;
		if (baseInfo.containsKey("bolus"))// 推注
			bolus = baseInfo.getInt("bolus");
		room.setBolus(bolus);

		// 抢庄倍数列表 明牌抢庄
		if (SGConstant.PLAY_TYPE_MPQZ == palyType && baseInfo.containsKey("rob")) {
			List<Integer> robList = new ArrayList<>();
			int rob = baseInfo.getInt("rob");
			if (rob < 1)
				rob = 1;
			for (int i = 1; i <= rob; i++) {
				robList.add(i);
			}
			room.setRobList(robList);
		}

		room.setStartType(SGConstant.START_TYPE_ZB);// 游戏开始 类型
		if (baseInfo.containsKey("start")) {// 游戏开始 类型 1 首位开始 2 准备开始 3 房主开始 4 满几人开始
			JSONObject object = baseInfo.getJSONObject("start");
			if (object.containsKey("type"))
				room.setStartType(object.getInt("type"));// 游戏开始 类型
			if (object.containsKey("value"))
				room.setStartTypeNum(object.getInt("value"));// 游戏开始 人数
		}

		room.setDoubleName(SGConstant.DOUBLE_TYPE);// 翻倍规则
		if (baseInfo.containsKey("double")) {
			Map<Integer, Integer> doubleMap = new HashMap<>();
			JSONArray array = baseInfo.getJSONArray("double");
			JSONObject object;
			for (int i = 0; i < array.size(); i++) {
				object = array.getJSONObject(i);
				if (object == null)
					continue;
				doubleMap.put(object.getInt("type"), object.getInt("value"));
			}
			room.setDoubleMap(doubleMap);
			if (doubleMap.get(SGConstant.CARD_TYPE_ZZ) != null && doubleMap.get(SGConstant.CARD_TYPE_DSG) != null
					&& doubleMap.get(SGConstant.CARD_TYPE_ZZ) != doubleMap.get(SGConstant.CARD_TYPE_DSG)) {
				room.setDoubleName(SGConstant.DOUBLE_TYPE_ZZ);// 翻倍规则
			} else
				room.setDoubleName(SGConstant.DOUBLE_TYPE_BZ);
		}

		// 底注
		List<Integer> anteList = new ArrayList<>();
		if (SGConstant.PLAY_TYPE_MPQZ == palyType) {// 明牌抢庄
			JSONArray array;
			if (SGConstant.SPECIAL_TYPE_PT == specialType)// 普通
				array = baseInfo.getJSONArray("robScore");
			else
				array = baseInfo.getJSONArray("folieRobScore");

			JSONObject object;
			StringBuffer ante = new StringBuffer();
			for (int i = 0; i < array.size(); i++) {
				object = array.getJSONObject(i);
				if (object == null)
					continue;
				anteList.add(object.getInt("val"));
				if (ante.length() > 0)
					ante.append("/");
				ante.append(object.getString("name"));
			}
			room.setAnte(ante.toString());
			room.setAnteList(anteList);
		} else if (SGConstant.PLAY_TYPE_DCX == palyType) {// 大吃小
			int score;
			if (SGConstant.SPECIAL_TYPE_PT == specialType)// 普通
				score = baseInfo.getInt("score");
			else
				score = baseInfo.getInt("folieScore");

			room.setAnte(String.valueOf(score));
			anteList.add(score);
			room.setAnteList(anteList);
		}

		if (SGConstant.PLAY_TYPE_DCX == palyType && baseInfo.containsKey("bet")) { // 下注封顶 大吃小
			if (SGConstant.SPECIAL_TYPE_PT == specialType)// 普通
				room.setBet(baseInfo.getInt("bet"));
			else
				room.setBet(baseInfo.getInt("folieBet"));
		}

		// 是否需要总结算 房卡 俱乐部局数场
		if (CommonConstant.ROOM_TYPE_FK == roomType || CommonConstant.ROOM_TYPE_INNING == roomType)
			room.setNeedFinalSummary(true);
		else
			room.setNeedFinalSummary(false);
		// 开房者不进入房间
		if (CommonConstant.ROOM_TYPE_DK != roomType && CommonConstant.ROOM_TYPE_FREE != roomType
				&& CommonConstant.ROOM_TYPE_INNING != roomType && CommonConstant.ROOM_TYPE_TEA != roomType) {
			room.getUserPacketMap().put(account, new SGUserPacket(account));
		}

	}

	public void createRoomGzMj(GzMjGameRoom room, JSONObject baseInfo, String account) {
		room.setWfType("贵州麻将");
		// 房主
		room.setOwner(account);
		// 底分
		if (baseInfo.containsKey("di")) {
			room.setScore(baseInfo.getInt("di"));
		} else {
			room.setScore(1);
		}
		// 鸡牌
		if (baseInfo.containsKey("chook") && baseInfo.getJSONArray("chook") != null) {
			JSONArray chook = baseInfo.getJSONArray("chook");
			room.setHasMtChock(chook.contains(GzMjConstant.GZMJ_ROOM_SETTING_CHOOK_MT));
			room.setHasWgChock(chook.contains(GzMjConstant.GZMJ_ROOM_SETTING_CHOOK_WG));
			room.setHasWeekChock(chook.contains(GzMjConstant.GZMJ_ROOM_SETTING_CHOOK_XQ));
			room.setHasSelfChock(chook.contains(GzMjConstant.GZMJ_ROOM_SETTING_CHOOK_BJ));
		}
		// 玩法
		if (baseInfo.containsKey("special") && baseInfo.getJSONArray("special") != null) {
			JSONArray special = baseInfo.getJSONArray("special");
			room.setHasTwo(special.contains(String.valueOf(GzMjConstant.GZMJ_ROOM_SETTING_SPECIAL_TWO)));
			room.setHasDl(special.contains(String.valueOf(GzMjConstant.GZMJ_ROOM_SETTING_SPECIAL_DL)));
			room.setHasBet(special.contains(String.valueOf(GzMjConstant.GZMJ_ROOM_SETTING_SPECIAL_BET)));
		}
		// 估卖
		if (room.isHasBet()) {
			for (int i = 1; i <= 4; i++) {
				JSONObject obj = new JSONObject();
				obj.put("value", i);
				obj.put("name", "卖" + i);
				room.getBetList().add(obj);
			}
		}
	}

	public void createRoomZJH(ZJHGameRoomNew room, JSONObject baseInfo, String account) {
		// 玩法
		String wanFa = "";
		switch (baseInfo.getInt("type")) {
		case ZJHConstant.ZJH_GAME_TYPE_CLASSIC:
			wanFa = "经典模式";
			break;
		case ZJHConstant.ZJH_GAME_TYPE_MEN:
			wanFa = "必闷三圈";
			break;
		case ZJHConstant.ZJH_GAME_TYPE_HIGH:
			wanFa = "激情模式";
			break;
		default:
			break;
		}

		room.setWfType(wanFa);
		// 玩法类型 经典模式、必闷三圈、激情模式
		room.setGameType(baseInfo.getInt("type"));
		// 倒计时及满人可配 20180830 wqm
		JSONObject setting = redisInfoService.getGameInfoById(CommonConstant.GAME_ID_ZJH).getJSONObject("setting");
		logger.warn("房间设置："+setting);
		room.setSetting(setting);
		if (baseInfo.containsKey("eventTime")) {
			// 设置下注时间
			room.setXzTimer(baseInfo.getInt("eventTime"));
		} else {
			// 设置下注时间
			room.setXzTimer(ZJHConstant.ZJH_TIMER_XZ);
		}
		System.out.println("开放时设置下注时间为："+room.getXzTimer());
		// 庄家
		room.setBanker(account);
		// 房主
		room.setOwner(account);
		if (baseInfo.containsKey("di")) {
			room.setScore(baseInfo.getInt("di"));
		} else {
			room.setScore(1);
		}
		// 元宝模式
		if (baseInfo.containsKey("yuanbao")) {
			// 底分
			room.setScore(baseInfo.getInt("yuanbao"));
		}
		// 倍数
		if (baseInfo.containsKey("baseNum")) {
			JSONArray baseNum = new JSONArray();
			for (int i = 0; i < baseInfo.getJSONArray("baseNum").size(); i++) {
				baseNum.add(baseInfo.getJSONArray("baseNum").getJSONObject(i).getInt("val"));
			}
			room.setBaseNum(baseNum);
		} else {
			JSONArray baseNum = new JSONArray();
			for (int i = 1; i <= 5; i++) {
				if (room.getScore() % 1 == 0) {
					baseNum.add(String.valueOf((int) room.getScore() * i));
				} else {
					baseNum.add(String.valueOf(room.getScore() * i));
				}
			}
			room.setBaseNum(baseNum);
		}
		// 下注上限
		if (baseInfo.containsKey("maxcoins")) {
			room.setMaxScore(baseInfo.getDouble("maxcoins"));
		} else {
			room.setMaxScore(100000000);
		}
		// 下注轮数上限
		if (baseInfo.containsKey("gameNum")) {
			room.setTotalGameNum(baseInfo.getInt("gameNum"));
		} else {
			room.setTotalGameNum(15);
		}
		room.setCurrentScore(room.getScore());
		int roomType = baseInfo.getInt("roomType");
		// 开房者不进入房间
		if (CommonConstant.ROOM_TYPE_DK != roomType && CommonConstant.ROOM_TYPE_FREE != roomType
				&& CommonConstant.ROOM_TYPE_INNING != roomType && CommonConstant.ROOM_TYPE_TEA != roomType) {
			room.getUserPacketMap().put(account, new com.zhuoan.biz.model.zjh.UserPacket());
		}
	}

	public void getGameSetting(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		int gid = postData.getInt("gid");
		String platform = postData.getString("platform");
		int flag = 1;
		if (postData.containsKey("flag")) {
			flag = postData.getInt("flag");
		}
		/* 查询房间设置,插入缓存 */
		JSONArray gameSetting = redisInfoService.getGameSetting(gid, platform, flag);

		if (!Dto.isNull(gameSetting)) {
			JSONArray array = new JSONArray();
			for (int i = 0; i < gameSetting.size(); i++) {
				array.add(gameSetting.getJSONObject(i));
			}
			JSONObject result = new JSONObject();
			result.put("data", array);
			result.put("flag", flag);
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);

			System.out.println("result===" + result);
			CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getGameSettingPush");
			return;
		}
		CommonConstant.sendMsgEventNo(client, "敬请期待", null, "getGameSettingPush");
	}

	public void checkUser(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (postData.containsKey("gid") && postData.getInt("gid") > 0) {
			int gameId = postData.getInt("gid");
			JSONObject gameInfo = redisInfoService.getGameInfoById(gameId);
			if (Dto.isObjNull(gameInfo) || !gameInfo.containsKey("status") || gameInfo.getInt("status") != 1) {
				CommonConstant.sendMsgEventNo(client, "该游戏正在维护中", null, "enterRoomPush_NN");
				return;
			}
		}
		if (postData.containsKey("account")) {
			String account = postData.getString("account");
			// 遍历房间列表
			for (String roomNo : RoomManage.gameRoomMap.keySet()) {
				if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
					GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
					if (!Dto.stringIsNULL(account)) {
						if (gameRoom.getPlayerMap().containsKey(account)
								&& gameRoom.getPlayerMap().get(account) != null) {
							postData.put(CommonConstant.DATA_KEY_ROOM_NO, gameRoom.getRoomNo());
							postData.put("myIndex", gameRoom.getPlayerMap().get(account).getMyIndex());
							joinRoomBase(client, postData);
							return;
						}
						// 观战玩家
						else if (gameRoom.getVisitPlayerMap().containsKey(account)
								&& gameRoom.getVisitPlayerMap().get(account) != null) {
							postData.put(CommonConstant.DATA_KEY_ROOM_NO, gameRoom.getRoomNo());
							postData.put("myIndex", gameRoom.getVisitPlayerMap().get(account).getMyIndex());
							joinRoomBase(client, postData);
							return;
						}

					}
				}
			}
			// 该玩家当前所在场次
			Object playerSignUpInfo = redisService.hget("player_sign_up_info" + MatchConstant.MATCH_TYPE_COUNT,
					account);
			// 更新uuid
			if (playerSignUpInfo != null) {
				matchEventDeal.changePlayerInfo(JSONObject.fromObject(playerSignUpInfo).getString("match_num"),
						String.valueOf(client.getSessionId()), null, account, 0, 0, 0, 0);
			}
			playerSignUpInfo = redisService.hget("player_sign_up_info" + MatchConstant.MATCH_TYPE_TIME, account);
			if (playerSignUpInfo != null) {
				matchEventDeal.changePlayerInfo(JSONObject.fromObject(playerSignUpInfo).getString("match_num"),
						String.valueOf(client.getSessionId()), null, account, 0, 0, 0, 0);
			}
			JSONObject result = new JSONObject();
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "checkUserPush");
		}
	}

	public void getUserInfo(SocketIOClient client, Object data) {
		JSONObject postdata = JSONObject.fromObject(data);
		if (!postdata.containsKey("account")) {
			return;
		}
		JSONObject result = new JSONObject();
		String account = postdata.getString(CommonConstant.DATA_KEY_ACCOUNT);
		JSONObject userInfo = userBiz.getUserByAccount(account);
		if (!Dto.isObjNull(userInfo)) {
			if (userInfo.containsKey("platform")
					&& CommonConstant.fundPlatformList.contains(userInfo.getString("platform"))) {
				fundEventDeal.getAndUpdateUserMoney(userInfo.getString("openid"));
				userInfo = userBiz.getUserByAccount(account);
			}
			String headimg = userInfo.getString("headimg");
			userInfo.put("headimg", "http://"+GameMain.propertiesUtil.get("ip")+"/zagame"+headimg);
			userInfo.remove("uuid");
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			result.put("user", userInfo);
		} else {
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
		}
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getUserInfoPush");
	}

	public void getAllRoomList(SocketIOClient client, Object data) {
		JSONObject fromObject = JSONObject.fromObject(data);
		JSONObject result = new JSONObject();
		int type = 0;
		if (fromObject.containsKey("type")) {
			type = fromObject.getInt("type");
		}
		JSONArray allRoom = new JSONArray();
		for (String roomNo : RoomManage.gameRoomMap.keySet()) {
			if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
				GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
				if (fromObject.get("gid") != null && !"-1".equals(fromObject.getString("gid"))
						&& gameRoom.getGid() != fromObject.getInt("gid"))
					continue;

				if (gameRoom.getGid() != CommonConstant.GAME_ID_BDX
						&& gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_YB && gameRoom.isOpen()) {

					JSONObject obj = new JSONObject();
					obj.put("room_no", gameRoom.getRoomNo());
					obj.put("gid", gameRoom.getGid());
					obj.put("base_info", gameRoom.getRoomInfo());
					obj.put("fytype", gameRoom.getWfType());
					obj.put("iszs", 0);
					obj.put("player", gameRoom.getPlayerCount());
					obj.put("renshu", gameRoom.getPlayerMap().size());
					if (gameRoom.getGid() == CommonConstant.GAME_ID_SW && gameRoom instanceof SwGameRoom) {
						double ratio = ((SwGameRoom) gameRoom).getRatio();
						obj.put("ratio", "1赔" + ratio);
					}
					if (type == 0 || (type == 1 && gameRoom.getPlayerMap().size() < gameRoom.getPlayerCount())) {
						allRoom.add(obj);
					}
				}
			}
		}
		Collections.sort(allRoom, new Comparator() {
			@Override
			public int compare(Object o1, Object o2) {
				return JSONObject.fromObject(o1).getInt("renshu") - JSONObject.fromObject(o2).getInt("renshu");
			}
		});
		result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		result.element("array", allRoom);
		result.element("gid", fromObject.get("gid"));
		result.element("sType", fromObject.get("sType"));
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getAllRoomListPush");
	}

	public void getUserGameLogs(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (postData.containsKey("id") && postData.containsKey("gid")) {
			long userId = postData.getLong("id");
			int gameId = postData.getInt("gid");
			int roomType = 3;
			if (postData.containsKey("room_type")) {
				roomType = postData.getInt("room_type");
			}
			JSONArray userGameLogs = gameLogBiz.getUserGameLogsByUserId(userId, gameId, roomType);
			JSONObject back = new JSONObject();
			JSONArray result = new JSONArray();
			if (userGameLogs.size() > 0) {
				for (int i = 0; i < userGameLogs.size(); i++) {
					JSONObject userGameLog = userGameLogs.getJSONObject(i);
					JSONObject obj = new JSONObject();
					obj.put("room_no", userGameLog.getString("room_no"));
					obj.put("createTime", userGameLog.getString("createtime"));
					obj.put("glog_id", userGameLog.getString("gamelog_id"));
					obj.put("id", userGameLog.getString("id"));
					JSONArray userResult = new JSONArray();
					for (int j = 0; j < userGameLog.getJSONArray("result").size(); j++) {
						JSONObject object = userGameLog.getJSONArray("result").getJSONObject(j);
						userResult.add(new JSONObject().element("player", object.getString("player"))
								.element("score", object.getString("score"))
								.element("account", object.getString("account")));
					}
					obj.put("playermap", userResult);
					result.add(obj);
				}
			}
			if (result.size() == 0) {
				back.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			} else {
				back.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
				back.put("data", result);
				back.put("gid", gameId);
			}
			CommonConstant.sendMsgEventToSingle(client, String.valueOf(back), "getGameLogsListPush");
		}
	}

	public void dissolveRoom(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (postData.containsKey("adminCode") && postData.containsKey("adminPass") && postData.containsKey("memo")) {
			String adminCode = postData.getString("adminCode");
			String adminPass = postData.getString("adminPass");
			String memo = postData.getString("memo");
			if (postData.containsKey("room_no")) {
				String roomNo = postData.getString("room_no");
				if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
					JSONObject sysUser = userBiz.getSysUser(adminCode, adminPass, memo);
					if (!Dto.isObjNull(sysUser)) {
						JSONObject result = new JSONObject();
						result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
						result.put(CommonConstant.RESULT_KEY_MSG, "房间已解散");
						CommonConstant.sendMsgEventToAll(RoomManage.gameRoomMap.get(roomNo).getAllUUIDList(),
								String.valueOf(result), "tipMsgPush");
						RoomManage.gameRoomMap.remove(roomNo);
					}
				}
			}
		}
	}

	public void sendNotice(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (postData.containsKey("adminCode") && postData.containsKey("adminPass") && postData.containsKey("memo")) {
			String adminCode = postData.getString("adminCode");
			String adminPass = postData.getString("adminPass");
			String memo = postData.getString("memo");
			if (postData.containsKey("content") && postData.containsKey("type")) {
				String content = postData.getString("content");
				int type = postData.getInt("type");
				// 通知所有
				JSONObject sysUser = userBiz.getSysUser(adminCode, adminPass, memo);
				if (!Dto.isObjNull(sysUser)) {
					switch (type) {
					case CommonConstant.NOTICE_TYPE_MALL:
						// TODO: 2018/5/10 大厅滚动公告设置
						break;
					case CommonConstant.NOTICE_TYPE_GAME:
						BaseEventDeal.noticeContentGame = content;
						for (String roomNo : RoomManage.gameRoomMap.keySet()) {
							if (RoomManage.gameRoomMap.containsKey(roomNo)
									&& RoomManage.gameRoomMap.get(roomNo) != null) {
								sendNoticeToPlayerByRoomNo(roomNo, content, type);
							}
						}
						break;
					default:
						break;
					}
				}
			}
		}
	}

	public void getNotice(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		int type = postData.getInt("type");
		JSONObject result = new JSONObject();
		result.put("type", type);
		result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		switch (type) {
		case CommonConstant.NOTICE_TYPE_MALL:
			if (!postData.containsKey("platform")) {
				return;
			} else {
				String platform = postData.getString("platform");
				JSONArray noticeArray = getNoticeInfoByPlatform(platform, CommonConstant.NOTICE_DATABASE_TYPE_ROLL);
				if (!Dto.isNull(noticeArray) && noticeArray.size() > 0) {
					JSONObject noticeInfo = noticeArray.getJSONObject(0);
					result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
					result.put("content", noticeInfo.getString("con"));
				} else {
					result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
				}
			}
			break;
		case CommonConstant.NOTICE_TYPE_GAME:
			if (Dto.stringIsNULL(BaseEventDeal.noticeContentGame)) {
				result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			} else {
				result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
				result.put("content", BaseEventDeal.noticeContentGame);
			}
			break;
		case CommonConstant.NOTICE_TYPE_ALERT:
			String platform = postData.getString("platform");
			JSONArray noticeArray = getNoticeInfoByPlatform(platform, CommonConstant.NOTICE_DATABASE_TYPE_ALERT);
			if (!Dto.isNull(noticeArray) && noticeArray.size() > 0) {
				for (int i = 0; i < noticeArray.size(); i++) {
					if (noticeArray.getJSONObject(i).containsKey("image")) {
						noticeArray.getJSONObject(i).put("image",
								CommonConstant.getResourceDomain() + noticeArray.getJSONObject(i).getString("image"));
					}
				}
				result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
				result.put("data", noticeArray);
			} else {
				result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			}
			break;
		default:
			break;
		}
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getMessagePush");
	}

	private JSONArray getNoticeInfoByPlatform(String platform, int type) {
		JSONArray noticeInfo;
		StringBuffer sb = new StringBuffer();
		sb.append("notice_");
		sb.append(platform);
		sb.append("_");
		sb.append(type);
		try {
			Object object = redisService.queryValueByKey(String.valueOf(sb));
			if (object != null) {
				noticeInfo = JSONArray.fromObject(redisService.queryValueByKey(String.valueOf(sb)));
			} else {
				noticeInfo = publicBiz.getNoticeByPlatform(platform, type);
				redisService.insertKey(String.valueOf(sb), String.valueOf(noticeInfo), 300L);
			}
		} catch (Exception e) {
			logger.error("请启动REmote DIctionary Server");
			noticeInfo = publicBiz.getNoticeByPlatform(platform, type);
		}
		return noticeInfo;
	}

	public void sendNoticeToPlayerByRoomNo(String roomNo, String content, int type) {
		if (!Dto.stringIsNULL(content)) {
			JSONObject result = new JSONObject();
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			result.put("content", content);
			result.put("type", type);
			CommonConstant.sendMsgEventToAll(RoomManage.gameRoomMap.get(roomNo).getAllUUIDList(),
					String.valueOf(result), "getMessagePush");
		}
	}

	public void getShuffleInfo(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
			return;
		}
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		String gameId = postData.getString("gid");
		GameRoom room = RoomManage.gameRoomMap.get(roomNo);
		JSONObject result = new JSONObject();
		result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
		JSONObject sysSet = publicBiz.getSysBaseSet();
		JSONObject set = publicBiz.getAPPGameSetting();
		if (!Dto.isObjNull(set) && set.getString("isXipai").equals("1") && !set.getString("xipaiLayer").equals("0")
				&& !Dto.stringIsNULL(set.getString("xipaiCount"))) {
			// 总洗牌次数
			result.element("allCount", set.getInt("xipaiLayer"));
			if (set.getString("xipaiObj").equals("1")) {
				result.element("obj", sysSet.getString("cardname"));
			} else if (set.getString("xipaiObj").equals("2")) {
				result.element("obj", sysSet.getString("coinsname"));
			} else if (set.getString("xipaiObj").equals("3")) {
				result.element("obj", sysSet.getString("yuanbaoname"));
			}
			int size = 0;
			if (size < set.getInt("xipaiLayer")) {
				String[] count = set.getString("xipaiCount").substring(1, set.getString("xipaiCount").length() - 1)
						.split("\\$");
				if (count.length > size) {
					result.element("cur", size + 1);
					// 扣除数额
					result.element("sum", count[size]);
					result.element(CommonConstant.RESULT_KEY_MSG, "可以洗牌");
					result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
				} else {
					result.element("cur", size + 1);
					result.element(CommonConstant.RESULT_KEY_MSG, "扣除次数已达上限");
				}
			} else {
				result.element("cur", size + 1);
				result.element(CommonConstant.RESULT_KEY_MSG, "扣除次数已达上限");
			}
		} else {
			result.element(CommonConstant.RESULT_KEY_MSG, "无此功能");
		}
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "xipaiMessaPush");
	}

	public void doShuffle(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
			return;
		}
		JSONObject result = new JSONObject();
		result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
		result.element(CommonConstant.RESULT_KEY_MSG, "操作失败，稍后重试");
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		String gameId = postData.getString("gid");
		// 当前洗牌次数
		int currentTime = postData.getInt("cur");
		// 总次数
		int totalTime = postData.getInt("allCount");
		double sum = postData.getDouble("sum");
		// 当前洗牌消耗元宝
		GameRoom room = RoomManage.gameRoomMap.get(roomNo);
		// 洗牌次数大于总次数
		if (currentTime <= totalTime) {
			if (sum <= room.getPlayerMap().get(account).getScore()) {
				double oldScore = room.getPlayerMap().get(account).getScore();
				room.getPlayerMap().get(account).setScore(Dto.sub(oldScore, sum));
				// 更新玩家分数
				JSONObject obj = new JSONObject();
				obj.put("account", account);
				obj.put("updateType", room.getCurrencyType());
				obj.put("sum", -sum);
				producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_USER_INFO, obj));
				// 插入洗牌数据

				JSONObject object = new JSONObject();
				object.put("userId", room.getPlayerMap().get(account).getId());
				object.put("platform", room.getPlayerMap().get(account).getPlatform());
				object.put("gameId", gameId);
				object.put("room_id", room.getId());
				object.put("room_no", roomNo);
				object.put("new", room.getPlayerMap().get(account).getScore());
				object.put("old", oldScore);
				object.put("change", -sum);
				object.put("type", room.getRoomType());
				producerService.sendMessage(daoQueueDestination,
						new PumpDao(DaoTypeConstant.INSERT_APP_OBJ_REC, object));
				// 通知玩家
				result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
				result.element("index", room.getPlayerMap().get(account).getMyIndex());
				result.element("score", room.getPlayerMap().get(account).getScore());
				CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "xipaiFunPush");
				return;
			} else {
				result.element(CommonConstant.RESULT_KEY_MSG, "余额不足");
			}
		} else {
			result.element(CommonConstant.RESULT_KEY_MSG, "洗牌次数已达上限");
		}
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "xipaiFunPush");
	}

	public void sendMessage(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
			return;
		}
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		int type = postData.getInt("type");
		String content = postData.getString("data");
		GameRoom room = RoomManage.gameRoomMap.get(roomNo);
		// 敏感词替代
		String backData = SensitivewordFilter.replaceSensitiveWord(content, 1, "*");
		JSONObject result = new JSONObject();
		result.put("user", room.getPlayerMap().get(account).getMyIndex());
		result.put("type", type);
		result.put("data", backData);
		CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "sendMsgEventPush");
	}

	public void sendVoice(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
			return;
		}
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		GameRoom room = RoomManage.gameRoomMap.get(roomNo);
		// 游戏语音通知
		int gameId = postData.getInt("gid");
		switch (gameId) {
		case CommonConstant.GAME_ID_NN:
			CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), String.valueOf(postData),
					"voiceCallGamePush_NN");
			break;
		case CommonConstant.GAME_ID_SSS:
			CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), String.valueOf(postData),
					"voiceCallGamePush_SSS");
			break;
		case CommonConstant.GAME_ID_ZJH:
			CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), String.valueOf(postData),
					"voiceCallGamePush_ZJH");
			break;
		case CommonConstant.GAME_ID_QZMJ:
			CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), String.valueOf(postData),
					"voiceCallGamePush");
			break;
		case CommonConstant.GAME_ID_ZZC:
			CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), String.valueOf(postData),
					"voiceCallGamePush");
			break;
		case CommonConstant.GAME_ID_NAMJ:
			CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), String.valueOf(postData),
					"voiceCallGamePush");
			break;
		case CommonConstant.GAME_ID_GP_PJ:
			CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), String.valueOf(postData),
					"voiceCallGamePush_GPPJ");
			break;
		default:
			CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), String.valueOf(postData),
					"voiceCallGamePush");
			break;
		}
	}

	public void getRoomGid(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!postData.containsKey(CommonConstant.DATA_KEY_ROOM_NO)) {
			return;
		}
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		JSONObject result = new JSONObject();
		if (!RoomManage.gameRoomMap.containsKey(roomNo) || RoomManage.gameRoomMap.get(roomNo) == null) {
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			result.put(CommonConstant.RESULT_KEY_MSG, "房间不存在");
			CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getRoomGidPush");
			return;
		}
		GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
		if (gameRoom.getPlayerMap().size() >= gameRoom.getPlayerCount()) {
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			result.put(CommonConstant.RESULT_KEY_MSG, "房间已满");
			CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getRoomGidPush");
			return;
		}
		result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getRoomGidPush");
		result.put("gid", gameRoom.getGid());
	}

	public void getRoomCardPayInfo(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (postData.containsKey("base_info")) {
			JSONObject baseInfo = postData.getJSONObject("base_info");
			if (!baseInfo.containsKey("player") || !baseInfo.containsKey("paytype") || !baseInfo.containsKey("turn")) {
				return;
			}
			int roomCard = getRoomCardPayInfo(baseInfo);
			JSONObject result = new JSONObject();
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			String payType = baseInfo.getString("paytype");
			if (!CommonConstant.ROOM_PAY_TYPE_TEA_AA.equals(payType)
					&& !CommonConstant.ROOM_PAY_TYPE_AA.equals(payType)) {
				roomCard = roomCard * baseInfo.getInt("player");
			}
			result.put("roomcard", roomCard);
			CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getRoomCardPayInfoPush");
		}
	}

	public static int getRoomCardPayInfo(JSONObject baseInfo) {
		int roomCard = 0;
		if (baseInfo.containsKey("player")) {
			JSONObject turn = baseInfo.getJSONObject("turn");
			if (turn.containsKey("AANum")) {
				roomCard = turn.getInt("AANum");
				if (baseInfo.containsKey("roomType")) {
					if (CommonConstant.ROOM_TYPE_FREE == baseInfo.getInt("roomType")) {
						int t = turn.containsKey("turn") ? turn.getInt("turn") : 10;
						if (t > 900)
							t = 10;
						roomCard = roomCard / t;
						if (roomCard <= 0)
							roomCard = 1;
					}
				}
			}
		}
		return roomCard;
	}

	public void joinCoinRoom(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		int gameId = postData.getInt("gid");
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		if (Dto.stringIsNULL(account)) {
			return;
		}
		JSONObject option = new JSONObject();
		if (postData.containsKey("option")) {
			option = postData.getJSONObject("option");
		} else {
			JSONObject object = new JSONObject();
			object.put("gameId", gameId);
			object.put("platform", postData.getString("platform"));
			JSONArray array = getGoldSettingByGameIdAndPlatform(object);
			JSONObject userInfo = userBiz.getUserByAccount(account);
			if (!Dto.isObjNull(userInfo) && userInfo.containsKey("coins")) {
				int userCoins = userInfo.getInt("coins");
				// 取符合场次要求的场次信息
				for (int i = array.size() - 1; i >= 0; i--) {
					JSONObject obj = array.getJSONObject(i).getJSONObject("option");
					if (obj.getInt("goldCoinEnter") < userCoins && obj.getInt("goldCoinLeave") >= userCoins) {
						option = obj;
						break;
					} else if (obj.getInt("goldCoinEnter") == obj.getInt("goldCoinLeave")
							&& obj.getInt("goldCoinEnter") < userCoins) {
						option = obj;
						break;
					}
				}
				if (Dto.isObjNull(option) && array.size() > 0) {
					option = array.getJSONObject(0).getJSONObject("option");
				}
			}
		}
		postData.put("base_info", option);
		if (Dto.isObjNull(option)) {
			return;
		}
		// 比赛场判断
		Object playerSignUpInfo = redisService.hget("player_sign_up_info" + MatchConstant.MATCH_TYPE_COUNT, account);
		if (playerSignUpInfo != null) {
			CommonConstant.sendMsgEventNo(client, "已经报名比赛场", null, "enterRoomPush_NN");
			return;
		}
		List<String> roomNoList = new ArrayList<String>();
		for (String roomNo : RoomManage.gameRoomMap.keySet()) {
			if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
				GameRoom room = RoomManage.gameRoomMap.get(roomNo);
				if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB && room.getGid() == gameId
						&& room.getScore() == option.getDouble("di") && !room.getPlayerMap().containsKey(account)
						&& room.getPlayerMap().size() < room.getPlayerCount()) {
					roomNoList.add(roomNo);
				}
			}
		}
		if (roomNoList.size() <= 0) {
			createRoomBase(client, postData);
		} else {
			// 随机加入
			Collections.shuffle(roomNoList);
			postData.put("room_no", roomNoList.get(0));
			joinRoomBase(client, postData);
		}
	}

	private JSONArray getGoldSettingByGameIdAndPlatform(JSONObject obj) {
		JSONArray goldSettings;
		StringBuffer sb = new StringBuffer();
		sb.append("gold_setting_");
		sb.append(obj.getString("platform"));
		sb.append("_");
		sb.append(obj.getInt("gameId"));
		try {
			Object object = redisService.queryValueByKey(String.valueOf(sb));
			if (object != null) {
				goldSettings = JSONArray.fromObject(redisService.queryValueByKey(String.valueOf(sb)));
			} else {
				goldSettings = publicBiz.getGoldSetting(obj);
				redisService.insertKey(String.valueOf(sb), String.valueOf(goldSettings), 300L);
			}
		} catch (Exception e) {
			goldSettings = publicBiz.getGoldSetting(obj);
			logger.error("请启动REmote DIctionary Server");
		}
		return goldSettings;
	}

	public void getCoinSetting(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		int gameId = postData.getInt("gid");
		String platform = postData.getString("platform");
		JSONObject obj = new JSONObject();
		obj.put("gameId", gameId);
		obj.put("platform", platform);
		JSONArray goldSettings = getGoldSettingByGameIdAndPlatform(obj);
		for (int i = 0; i < goldSettings.size(); i++) {
			JSONObject goldSetting = goldSettings.getJSONObject(i);
			goldSetting.put("online", goldSetting.getInt("online") + RandomUtils.nextInt(goldSetting.getInt("online")));
			goldSetting.put("enter", goldSetting.getJSONObject("option").getInt("goldCoinEnter"));
			goldSetting.put("leave", goldSetting.getJSONObject("option").getInt("goldCoinLeave"));
		}
		JSONObject result = new JSONObject();
		result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		result.put("data", goldSettings);
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getGameGoldSettingPush");
	}

	public void checkSignIn(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		long userId = postData.getLong("userId");
		String platform = postData.getString("platform");
		int type = CommonConstant.SIGN_INFO_EVENT_TYPE_HALL;
		if (postData.containsKey("type")) {
			type = postData.getInt("type");
		}
		// 当前日期
		String nowTime = TimeUtil.getNowDateymd() + " 00:00:00";
		JSONObject signInfo = publicBiz.getUserSignInfo(platform, userId);
		JSONObject result = new JSONObject();
		int minReward = getCoinsSignMinReward(platform);
		int maxReward = getCoinsSignMaxReward(platform);
		int baseReward = getCoinsSignBaseReward(platform);
		if (Dto.isObjNull(signInfo)) {
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			result.put("reward", baseReward + minReward);
			result.put("days", 0);
			result.put("isSign", CommonConstant.GLOBAL_NO);
		} else {
			if (!TimeUtil.isLatter(signInfo.getString("createtime"), nowTime)) {
				result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
				result.put("isSign", CommonConstant.GLOBAL_NO);
				String yesterday = TimeUtil.addDaysBaseOnNowTime(nowTime, -1, "yyyy-MM-dd HH:mm:ss");
				if (TimeUtil.isLatter(signInfo.getString("createtime"), yesterday)) {
					int signDay = signInfo.getInt("singnum") + 1;
					// 一周签到模式
					if (CommonConstant.weekSignPlatformList.contains(platform) && signDay >= 8) {
						signDay = 1;
					}
					int reward = baseReward + signDay * minReward;
					if (reward > maxReward) {
						reward = maxReward;
					}
					result.put("reward", reward);
					result.put("days", signDay - 1);
				} else {
					result.put("reward", baseReward + minReward);
					result.put("days", 0);
				}
			} else {
				// 已经签到过了判断是否是用户主动请求
				if (type == CommonConstant.SIGN_INFO_EVENT_TYPE_CLICK) {
					result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
					result.put("days", signInfo.getInt("singnum") - 1);
					result.put("isSign", CommonConstant.GLOBAL_YES);
				} else {
					result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
				}
			}
		}
		// 签到奖励数组
		JSONArray array = new JSONArray();
		for (int i = 1; i <= 7; i++) {
			int reward = baseReward + minReward * i;
			if (reward > maxReward) {
				reward = maxReward;
			}
			array.add(reward);
		}
		result.put("array", array);
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "checkSignInPush");
	}

	public int getCoinsSignMinReward(String platform) {
		int minReward = CommonConstant.COINS_SIGN_MIN;
		JSONObject signRewardInfo = getCoinsSignRewardInfo(platform);
		if (!Dto.isObjNull(signRewardInfo) && signRewardInfo.containsKey("signin_min")) {
			return signRewardInfo.getInt("signin_min");
		}
		return minReward;
	}

	private JSONObject getCoinsSignRewardInfo(String platform) {
		JSONObject signRewardInfo;
		StringBuffer sb = new StringBuffer();
		sb.append("sign_reward_info_");
		sb.append(platform);
		try {
			Object object = redisService.queryValueByKey(String.valueOf(sb));
			if (object != null) {
				signRewardInfo = JSONObject.fromObject(redisService.queryValueByKey(String.valueOf(sb)));
			} else {
				signRewardInfo = publicBiz.getAppSettingInfo(platform);
				redisService.insertKey(String.valueOf(sb), String.valueOf(signRewardInfo), 300L);
			}
		} catch (Exception e) {
			signRewardInfo = publicBiz.getAppSettingInfo(platform);
			logger.error("请启动REmote DIctionary Server");
		}
		return signRewardInfo;
	}

	public int getCoinsSignMaxReward(String platform) {
		int maxReward = CommonConstant.COINS_SIGN_MAX;
		JSONObject signRewardInfo = getCoinsSignRewardInfo(platform);
		if (!Dto.isObjNull(signRewardInfo) && signRewardInfo.containsKey("signin_max")) {
			return signRewardInfo.getInt("signin_max");
		}
		return maxReward;
	}

	private int getCoinsSignBaseReward(String platform) {
		int maxReward = CommonConstant.COINS_SIGN_BASE;
		JSONObject signRewardInfo = getCoinsSignRewardInfo(platform);
		if (!Dto.isObjNull(signRewardInfo) && signRewardInfo.containsKey("signin_base")) {
			return signRewardInfo.getInt("signin_base");
		}
		return maxReward;
	}

	private String getCoinsSignRewardType(String platform) {
		String rewardType = null;
		JSONObject signRewardInfo = getCoinsSignRewardInfo(platform);
		if (!Dto.isObjNull(signRewardInfo) && signRewardInfo.containsKey("signin_prop")) {
			switch (signRewardInfo.getInt("signin_prop")) {
			case CommonConstant.CURRENCY_TYPE_ROOM_CARD:
				rewardType = "roomcard";
				break;
			case CommonConstant.CURRENCY_TYPE_COINS:
				rewardType = "coins";
				break;
			default:
				break;
			}
		}
		return rewardType;
	}

	public void doUserSignIn(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		long userId = postData.getLong("userId");
		String platform = postData.getString("platform");
		String account = postData.getString("account");
		// 当前日期
		String nowTime = TimeUtil.getNowDate();
		// 签到信息
		JSONObject signInfo = publicBiz.getUserSignInfo(platform, userId);
		JSONObject object = new JSONObject();
		JSONObject result = new JSONObject();
		int reward = getCoinsSignBaseReward(platform) + getCoinsSignMinReward(platform);
		if (Dto.isObjNull(signInfo)) {
			object.put("singnum", 1);
			object.put("createtime", nowTime);
			object.put("userID", userId);
			object.put("platform", platform);
		} else {
			object.put("id", signInfo.getLong("id"));
			String today = TimeUtil.getNowDateymd() + " 00:00:00";
			String yesterday = TimeUtil.addDaysBaseOnNowTime(today, -1, "yyyy-MM-dd HH:mm:ss");
			// 今日已签到
			if (TimeUtil.isLatter(signInfo.getString("createtime"), today)) {
				result.put(CommonConstant.RESULT_KEY_CODE, -1);
				result.put(CommonConstant.RESULT_KEY_MSG, "今日已签到");
				CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "userSignInPush");
				return;
			}
			if (TimeUtil.isLatter(signInfo.getString("createtime"), yesterday)) {
				int signDay = signInfo.getInt("singnum") + 1;
				// 一周签到模式
				if (CommonConstant.weekSignPlatformList.contains(platform) && signDay >= 8) {
					signDay = 1;
				}
				object.put("singnum", signDay);
				reward = getCoinsSignBaseReward(platform) + (signDay) * getCoinsSignMinReward(platform);
				int maxReward = getCoinsSignMaxReward(platform);
				if (reward > maxReward) {
					reward = maxReward;
				}
			} else {
				object.put("singnum", 1);
			}
			object.put("createtime", nowTime);
		}
		if (!Dto.isObjNull(object)) {
			int back = publicBiz.addOrUpdateUserSign(object);
			JSONObject userInfo = userBiz.getUserByAccount(account);
			String rewardType = getCoinsSignRewardType(platform);
			if (back > 0 && !Dto.isObjNull(userInfo) && !Dto.stringIsNULL(rewardType)) {
				result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
				result.put("newScore", userInfo.getInt(rewardType) + reward);
				result.put("days", object.getInt("singnum"));
				JSONObject obj = new JSONObject();
				obj.put("account", account);
				obj.put("updateType", rewardType);
				obj.put("sum", reward);
				producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_USER_INFO, obj));
			} else {
				result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			}
		} else {
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
		}
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "userSignInPush");
	}

	public double obtainBankMinScore(JSONObject postData) {
		if (!postData.containsKey("gid") || !postData.containsKey("base_info")) {
			return -1;
		}
		JSONObject baseInfo = postData.getJSONObject("base_info");
		if (baseInfo.containsKey("roomType")) {
			if (baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_FK
					|| baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_CLUB
					|| baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_FRIEND) {
				return 0;
			}
		}
		if (!baseInfo.containsKey("enterYB") || !baseInfo.containsKey("player") || !baseInfo.containsKey("yuanbao")) {
			return -1;
		}
		int gameId = postData.getInt("gid");
		double minScore = baseInfo.getDouble("enterYB");
		int noBankerNum = baseInfo.getInt("player") - 1;
		int maxNum = 1;
		if (baseInfo.containsKey("baseNum")) {
			JSONArray baseNum = baseInfo.getJSONArray("baseNum");
			for (int i = 0; i < baseNum.size(); i++) {
				if (baseNum.getJSONObject(i).getInt("val") > maxNum) {
					maxNum = baseNum.getJSONObject(i).getInt("val");
				}
			}
		}
		if (gameId == CommonConstant.GAME_ID_NN && baseInfo.getInt("playing") == NNConstant.NN_BANKER_TYPE_ZZ) {
			minScore = noBankerNum * maxNum * baseInfo.getDouble("yuanbao") * 5;
		}
		if (gameId == CommonConstant.GAME_ID_SSS && baseInfo.getInt("type") == SSSConstant.SSS_BANKER_TYPE_ZZ) {
			minScore = noBankerNum * maxNum * baseInfo.getDouble("yuanbao") * SSSConstant.SSS_XZ_BASE_NUM;
		}
		if (gameId == CommonConstant.GAME_ID_SW) {
			double ratio = 1;
			if (baseInfo.getInt("type") == SwConstant.SW_TYPE_TEN) {
				ratio = 10;
			} else if (baseInfo.getInt("type") == SwConstant.SW_TYPE_TEN_POINT_FIVE) {
				ratio = 10.5;
			} else if (baseInfo.getInt("type") == SwConstant.SW_TYPE_TEN_ELEVEN) {
				ratio = 11;
			} else if (baseInfo.getInt("type") == SwConstant.SW_TYPE_TEN_ELEVEN_POINT_FIVE) {
				ratio = 11.5;
			} else if (baseInfo.getInt("type") == SwConstant.SW_TYPE_TEN_TWELVE) {
				ratio = 12;
			}
			minScore = noBankerNum * 3 * baseInfo.getDouble("yuanbao") * ratio;
		}
		return minScore;
	}

	public void getRoomAndPlayerCount(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		int gameId = postData.getInt("game_id");
		int roomCount = 0;
		int playerCount = 0;
		for (String roomNo : RoomManage.gameRoomMap.keySet()) {
			if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
				GameRoom room = RoomManage.gameRoomMap.get(roomNo);
				if (room.getGid() == gameId) {
					roomCount++;
					playerCount += room.getPlayerMap().size();
				}
			}
		}
		JSONObject result = new JSONObject();
		result.put("roomCount", roomCount);
		result.put("playerCount", playerCount);
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getRoomAndPlayerCountPush");
	}

	public void getCompetitiveInfo(SocketIOClient client, Object data) {
		JSONObject result = new JSONObject();
		JSONArray arenaArray = publicBiz.getArenaInfo();
		if (arenaArray.size() > 0) {
			for (int i = 0; i < arenaArray.size(); i++) {
				JSONObject arena = arenaArray.getJSONObject(i);
				if (arena.containsKey("endTime") && arena.containsKey("startTime")) {
					TimeUtil.transTimeStamp(arena, "yyyy-MM-dd HH:mm:ss", "endTime");
					TimeUtil.transTimeStamp(arena, "yyyy-MM-dd HH:mm:ss", "startTime");
				}
			}
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			result.put("data", arenaArray);
		} else {
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			result.put("msg", "暂无场次信息");
		}
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getArenaPush");
	}

	public void joinCompetitiveRoom(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		int gameId = postData.getInt("gid");
		String platform = postData.getString("platform");
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		if (Dto.stringIsNULL(account)) {
			return;
		}
		for (String roomNo : RoomManage.gameRoomMap.keySet()) {
			if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
				GameRoom room = RoomManage.gameRoomMap.get(roomNo);
				if (room.getRoomType() == CommonConstant.ROOM_TYPE_COMPETITIVE && room.getGid() == gameId
						&& !room.getPlayerMap().containsKey(account)
						&& room.getPlayerMap().size() < room.getPlayerCount()) {
					postData.put("room_no", roomNo);
					joinRoomBase(client, postData);
					return;
				}
			}
		}
		JSONObject object = new JSONObject();
		object.put("gameId", gameId);
		object.put("platform", platform);
		JSONArray array = getGoldSettingByGameIdAndPlatform(object);
		if (array.size() > 0) {
			postData.put("base_info", array.getJSONObject(0).getJSONObject("option"));
			createRoomBase(client, postData);
		}
	}

	public void gameCheckIp(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
			return;
		}
		JSONObject result = new JSONObject();
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
		result.put("index", gameRoom.getPlayerMap().get(account).getMyIndex());
		result.put("ipStatus", CommonConstant.GLOBAL_YES);
		for (String uuid : gameRoom.getPlayerMap().keySet()) {
			if (gameRoom.getPlayerMap().containsKey(uuid) && gameRoom.getPlayerMap().get(uuid) != null) {
				if (!uuid.equals(account)) {
					if (gameRoom.getPlayerMap().get(account).getIp()
							.equals(gameRoom.getPlayerMap().get(uuid).getIp())) {
						result.put("ipStatus", CommonConstant.GLOBAL_NO);
						break;
					}
				}
			}
		}
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "gameCheckIpPush");
	}

	public void getProxyRoomList(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!postData.containsKey(CommonConstant.DATA_KEY_ACCOUNT)
				|| Dto.stringIsNULL(postData.getString(CommonConstant.DATA_KEY_ACCOUNT))) {
			return;
		}
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		JSONObject result = new JSONObject();
		result.put("type", 1);
		result.put("array", obtainProxyRoomList(account));
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getProxyRoomListPush");
	}

	public void dissolveProxyRoom(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!postData.containsKey(CommonConstant.DATA_KEY_ACCOUNT)
				|| Dto.stringIsNULL(postData.getString(CommonConstant.DATA_KEY_ACCOUNT))) {
			return;
		}
		if (!postData.containsKey(CommonConstant.DATA_KEY_ROOM_NO)
				|| Dto.stringIsNULL(postData.getString(CommonConstant.DATA_KEY_ROOM_NO))) {
			return;
		}
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		JSONObject result = new JSONObject();
		if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
			if (RoomManage.gameRoomMap.get(roomNo).getRoomType() == CommonConstant.ROOM_TYPE_DK
					&& account.equals(RoomManage.gameRoomMap.get(roomNo).getOwner())
					&& RoomManage.gameRoomMap.get(roomNo).getPlayerMap().size() == 0) {
				RoomManage.gameRoomMap.remove(roomNo);
			} else {
				result.put(CommonConstant.RESULT_KEY_MSG, "当前房间不可解散");
			}
		} else {
			result.put(CommonConstant.RESULT_KEY_MSG, "房间不存在");
		}
		result.put("type", 2);
		result.put("array", obtainProxyRoomList(account));
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getProxyRoomListPush");
	}

	private List<JSONObject> obtainProxyRoomList(String account) {
		List<JSONObject> proxyRoomList = new ArrayList<>();
		for (String roomNo : RoomManage.gameRoomMap.keySet()) {
			GameRoom room = RoomManage.gameRoomMap.get(roomNo);
			if (room.getRoomType() == CommonConstant.ROOM_TYPE_DK && account.equals(room.getOwner())) {
				JSONObject obj = new JSONObject();
				if (room.getGameIndex() == 0) {
					obj.put("status", 0);
				} else if (room.getGameIndex() < room.getGameCount()) {
					obj.put("status", 1);
				} else {
					obj.put("status", -2);
				}
				obj.put("game_id", room.getGid());
				obj.put("room_no", room.getRoomNo());
				obj.put("playerCount", room.getPlayerCount());
				List<String> playerList = new ArrayList<>();
				for (String player : room.getPlayerMap().keySet()) {
					if (room.getPlayerMap().containsKey(player) && room.getPlayerMap().get(player) != null) {
						playerList.add(room.getPlayerMap().get(player).getName());
					}
				}
				obj.put("users", playerList);
				proxyRoomList.add(obj);
			}
		}
		return proxyRoomList;
	}

	public void getUserAchievementInfo(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		// 所要获取的游戏id
		JSONArray idList = postData.getJSONArray("id_list");
		String platform = postData.getString("platform");
		// 用户成就信息
		JSONArray userAchievements = achievementBiz.getUserAchievementByAccount(account);
		// 组织数据，通知前端
		JSONObject result = new JSONObject();
		result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		// 成就详情
		JSONArray achievementArray = new JSONArray();
		for (int i = 0; i < idList.size(); i++) {
			int gameId = idList.getInt(i);
			JSONObject userAchievement = getAchievementByGameId(userAchievements, gameId);
			JSONObject achievement = new JSONObject();
			achievement.put("game_id", gameId);
			// 有存在取数据库，不存在取默认
			if (!Dto.isObjNull(userAchievement)) {
				achievement.put("score", userAchievement.getInt("achievement_score"));
				achievement.put("name", userAchievement.getString("achievement_name"));
			} else {
				achievement.put("score", 0);
				JSONArray achievementInfo = achievementBiz.getAchievementInfoByGameId(gameId, platform);
				if (achievementInfo.size() > 0) {
					achievement.put("name", achievementInfo.getJSONObject(0).getString("achievement_name"));
				} else {
					achievement.put("name", "未设置");
				}
			}
			achievementArray.add(achievement);
		}
		result.put("data", achievementArray);
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getUserAchievementInfoPush");
	}

	private JSONObject getAchievementByGameId(JSONArray userAchievements, int gameId) {
		for (Object obj : userAchievements) {
			JSONObject userAchievement = JSONObject.fromObject(obj);
			if (userAchievement.containsKey("game_id") && userAchievement.getInt("game_id") == gameId) {
				return userAchievement;
			}
		}
		return null;
	}

	public void getPropsInfo(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		int type = postData.getInt("type");
		String platform = postData.getString("platform");
		JSONArray props = propsBiz.getPropsInfoByPlatform(platform);
		JSONObject result = new JSONObject();
		result.put("data", props);
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getPropsInfoPush");
	}

	public void userPurchase(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		String account = postData.getString("account");
		String uuid = postData.getString("uuid");
		long propsId = postData.getLong("props_id");
		// 当前时间
		String nowTime = TimeUtil.getNowDate();
		// 道具信息
		JSONObject propsInfo = propsBiz.getPropsInfoById(propsId);
		// 通知前端
		JSONObject result = new JSONObject();
		if (!Dto.isObjNull(propsInfo)) {
			JSONObject userInfo = userBiz.getUserByAccount(account);
			// 支付类型
			String costType = propsInfo.getString("cost_type");
			// 价格
			int propsPrice = propsInfo.getInt("props_price");
			// 持续时间(小时)
			int duration = propsInfo.getInt("duration");
			// 道具类型
			int propsType = propsInfo.getInt("props_type");
			if (!Dto.isObjNull(userInfo) && userInfo.containsKey(costType) && userInfo.getInt(costType) > propsPrice) {
				if (!Dto.stringIsNULL(uuid) && uuid.equals(userInfo.getString("uuid"))) {
					JSONObject userProps = propsBiz.getUserPropsByType(account, propsType);
					JSONObject props = new JSONObject();
					String endTime;
					if (Dto.isObjNull(userProps)) {
						endTime = TimeUtil.addHoursBaseOnNowTime(nowTime, duration, "yyyy-MM-dd HH:mm:ss");
						props.put("user_account", account);
						props.put("game_id", propsInfo.getInt("game_id"));
						props.put("props_type", propsInfo.getInt("props_type"));

						props.put("props_name", propsInfo.getString("type_name"));
						props.put("end_time", endTime);
						// type为偶数需要更新数量
						if (propsInfo.getInt("props_type") % 2 == 0) {
							props.put("props_count", duration);
						}
					} else {
						// 当前是否过期
						if (TimeUtil.isLatter(nowTime, userProps.getString("end_time"))) {
							endTime = TimeUtil.addHoursBaseOnNowTime(nowTime, duration, "yyyy-MM-dd HH:mm:ss");
						} else {
							endTime = TimeUtil.addHoursBaseOnNowTime(userProps.getString("end_time"), duration,
									"yyyy-MM-dd HH:mm:ss");
						}
						props.put("id", userProps.getString("id"));
						props.put("end_time", endTime);
						// type为偶数需要更新数量
						if (propsInfo.getInt("props_type") % 2 == 0) {
							props.put("props_count", userProps.getInt("props_count") + duration);
						}
					}
					propsBiz.addOrUpdateUserProps(props);
					// 扣除玩家金币房卡
					JSONArray array = new JSONArray();
					JSONObject obj = new JSONObject();
					obj.put("id", userInfo.getLong("id"));
					obj.put("total", userInfo.getInt(costType));
					obj.put("fen", -propsPrice);
					array.add(obj);
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("array", array);
					jsonObject.put("updateType", costType);
					producerService.sendMessage(daoQueueDestination,
							new PumpDao(DaoTypeConstant.UPDATE_SCORE, jsonObject));
					result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
					result.put(CommonConstant.RESULT_KEY_MSG, "购买成功");
					if (costType.equals("roomcard")) {
						// 添加记录
						publicBiz.addUserWelfareRec(account, -propsPrice, CommonConstant.CURRENCY_TYPE_ROOM_CARD - 1,
								propsInfo.getInt("game_id"));
						result.put("roomcard", userInfo.getInt("roomcard") - propsPrice);
					} else {
						result.put("roomcard", userInfo.getInt("roomcard"));
					}
					if (costType.equals("coins")) {
						result.put("coins", userInfo.getInt("coins") - propsPrice);
					} else {
						result.put("coins", userInfo.getInt("coins"));
					}
					result.put("end_time", endTime);
					result.put("account", account);
					if (propsType == CommonConstant.PROPS_TYPE_DOUBLE_CARD) {
						result.put("props_count", props.getInt("props_count"));
					}
					if (postData.containsKey("room_no")) {
						String roomNo = postData.getString("room_no");
						if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
							GameRoom room = RoomManage.gameRoomMap.get(roomNo);
							if (room.getPlayerMap().containsKey(account) && room.getPlayerMap().get(account) != null) {
								if (room instanceof DdzGameRoom) {
									if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
										room.getPlayerMap().get(account).setScore(result.getInt("coins"));
									}
									// 更改对应的道具类型
									switch (propsType) {
									case CommonConstant.PROPS_TYPE_JPQ:
										((DdzGameRoom) room).getUserPacketMap().get(account).setJpqEndTime(endTime);
										break;
									case CommonConstant.PROPS_TYPE_DOUBLE_CARD:
										((DdzGameRoom) room).getUserPacketMap().get(account)
												.setDoubleCardNum(props.getInt("props_count"));
										break;
									default:
										break;
									}
									CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account),
											String.valueOf(result), "userPurchasePush");
								}
							}
						}
					}
				} else {
					result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
					result.put(CommonConstant.RESULT_KEY_MSG, "信息不正确");
				}
			} else {
				result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
				result.put(CommonConstant.RESULT_KEY_MSG, "余额不足");
			}
		} else {
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			result.put(CommonConstant.RESULT_KEY_MSG, "道具不存在");
		}
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "userPurchasePush");
	}

	public void getBackpackInfo(SocketIOClient client, Object data) {
		// 页面传递数据
		JSONObject postData = JSONObject.fromObject(data);
		// 玩家账号
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		// 平台号
		String platform = postData.getString("platform");
		// 道具详情
		List<JSONObject> propsList = new ArrayList<>();
		// 当前时间
		String nowDate = TimeUtil.getNowDate();
		// 查询当前用户道具
		JSONArray userProps = propsBiz.getUserPropsByAccount(account);
		if (!Dto.isNull(userProps) && userProps.size() > 0) {
			for (Object obj : userProps) {
				JSONObject userProp = JSONObject.fromObject(obj);
				// 按数量计算且当前数量大于0
				if (userProp.getInt("props_type") % 2 == 0 && userProp.getInt("props_count") > 0) {
					JSONObject propsInfo = new JSONObject();
					propsInfo.put("type", userProp.getInt("props_type"));
					propsInfo.put("name", userProp.getString("props_name"));
					propsInfo.put("count", userProp.getInt("props_count"));
					propsList.add(propsInfo);
				} else if (TimeUtil.isLatter(userProp.getString("end_time"), nowDate)) {
					// 按时间计算且当前时间未过期
					JSONObject propsInfo = new JSONObject();
					propsInfo.put("type", userProp.getInt("props_type"));
					propsInfo.put("name", userProp.getString("props_name"));
					propsInfo.put("endTime", userProp.getString("end_time"));
					propsList.add(propsInfo);
				}
			}
		}
		JSONObject result = new JSONObject();
		if (propsList.size() > 0) {
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			result.put("propsList", propsList);
		} else {
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
		}
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getBackpackInfoPush");
	}

	public void getAchievementRank(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		int gameId = postData.getInt("gameId");
		int limit = postData.getInt("limit");
		JSONArray achievementRank = achievementBiz.getAchievementRank(limit, gameId);
		JSONObject result = new JSONObject();
		result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		result.put("data", achievementRank);
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getAchievementRankPush");
	}

	public void getDrawInfo(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
			return;
		}
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		GameRoom room = RoomManage.gameRoomMap.get(roomNo);
		JSONObject result = new JSONObject();
		if (!Dto.isObjNull(room.getWinStreakObj())) {
			JSONObject winStreakObj = room.getWinStreakObj();
			// 次数
			int time = winStreakObj.getInt("time");
			// 是否需要连胜
			int mustWin = winStreakObj.getInt("mustWin");
			int winStreakTime = 0;
			if (room instanceof DdzGameRoom) {
				winStreakTime = ((DdzGameRoom) room).getUserPacketMap().get(account).getWinStreakTime();
			}
			if (winStreakTime >= time) {
				result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
				result.put("cardNum", 6);
			} else {
				result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
				if (mustWin == CommonConstant.GLOBAL_YES) {
					result.put(CommonConstant.RESULT_KEY_MSG, "再连胜" + (time - winStreakTime) + "场可拆红包");
				} else {
					result.put(CommonConstant.RESULT_KEY_MSG, "再赢" + (time - winStreakTime) + "场可拆红包");
				}
			}
		} else {
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			result.put(CommonConstant.RESULT_KEY_MSG, "当前房间不可拆红包");
		}
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getDrawInfoPush");
	}

	public void gameDraw(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
			return;
		}
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		int num = postData.getInt("num");
		GameRoom room = RoomManage.gameRoomMap.get(roomNo);
		int winStreakTime = 0;
		if (room instanceof DdzGameRoom) {
			winStreakTime = ((DdzGameRoom) room).getUserPacketMap().get(account).getWinStreakTime();
		}
		if (!Dto.isObjNull(room.getWinStreakObj())) {
			JSONObject winStreakObj = room.getWinStreakObj();
			// 次数
			int time = winStreakObj.getInt("time");
			if (winStreakTime >= time) {
				List<JSONObject> rewardList = room.getWinStreakObj().getJSONArray("rewardArr");
				if (num >= 0 && num < rewardList.size()) {
					Collections.shuffle(rewardList);
					JSONObject rewardObj = rewardList.get(num);
					double reward = rewardObj.getDouble("val");
					// 奖励类别
					String rewardType = null;
					// 奖励详情
					String rewardDetail = "谢谢参与";
					// 初始化抽奖次数
					if (room instanceof DdzGameRoom) {
						((DdzGameRoom) room).getUserPacketMap().get(account).setWinStreakTime(0);
					}
					if (reward > 0) {
						if (rewardObj.getInt("type") == CommonConstant.CURRENCY_TYPE_ROOM_CARD) {
							rewardType = "roomcard";
							rewardDetail = reward + "钻石";
						} else if (rewardObj.getInt("type") == CommonConstant.CURRENCY_TYPE_COINS) {
							rewardType = "coins";
							rewardDetail = reward + "金币";
							// 更新金币
							if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
								double newScore = Dto.add(room.getPlayerMap().get(account).getScore(), reward);
								room.getPlayerMap().get(account).setScore(newScore);
							}
						} else if (rewardObj.getInt("type") == CommonConstant.CURRENCY_TYPE_SCORE) {
							rewardType = "score";
							rewardDetail = reward + "实物券";
						} else if (rewardObj.getInt("type") == CommonConstant.CURRENCY_TYPE_YB) {
							rewardType = "yuanbao";
							rewardDetail = reward + "红包券";
						}
					}
					if (!Dto.stringIsNULL(rewardType)) {
						// 更新奖励
						JSONArray array = new JSONArray();
						JSONObject obj = new JSONObject();
						obj.put("total", room.getPlayerMap().get(account).getScore());
						obj.put("fen", reward);
						obj.put("id", room.getPlayerMap().get(account).getId());
						array.add(obj);
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("array", array);
						jsonObject.put("updateType", rewardType);
						producerService.sendMessage(daoQueueDestination,
								new PumpDao(DaoTypeConstant.UPDATE_SCORE, jsonObject));
						// 红包券和实物券添加记录
						if (rewardObj.getInt("type") == CommonConstant.CURRENCY_TYPE_SCORE
								|| rewardObj.getInt("type") == CommonConstant.CURRENCY_TYPE_YB) {
							publicBiz.addUserWelfareRec(account, reward, rewardObj.getInt("type") - 1, room.getGid());
						}
					}
					// 通知前端
					JSONObject result = new JSONObject();
					result.put("num", num);
					result.put("reward", rewardDetail);
					result.put("rewardList", rewardList);
					result.put("drawInfo", "还剩" + time + "局");
					result.put("nowScore", room.getPlayerMap().get(account).getScore());
					result.put("index", room.getPlayerMap().get(account).getMyIndex());
					CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "gameDrawPush");
				}
			}
		}
	}

	public void getAchievementDetail(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		String platform = postData.getString("platform");
		int gameId = postData.getInt("game_id");
		JSONObject result = new JSONObject();
		// 所有成就
		JSONArray achievementArray = achievementBiz.getAchievementInfoByGameId(gameId, platform);
		// 玩家当前成就
		JSONObject userAchievement = achievementBiz.getUserAchievementByAccountAndGameId(account, gameId);
		JSONArray array = new JSONArray();
		for (Object obj : achievementArray) {
			JSONObject achievement = JSONObject.fromObject(obj);
			// 可以领取 已经领取 不能领
			if (!Dto.isObjNull(userAchievement)
					&& userAchievement.getJSONArray("reward_array").contains(achievement.getLong("id"))) {
				achievement.put("status", 1);
			} else if (!Dto.isObjNull(userAchievement)
					&& userAchievement.getJSONArray("draw_array").contains(achievement.getLong("id"))) {
				achievement.put("status", 0);
			} else {
				achievement.put("status", 2);
			}
			array.add(achievement);
		}
		result.put("achievement_array", array);
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getAchievementDetailPush");
	}

	public void drawAchievementReward(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		int userId = postData.getInt("user_id");
		JSONObject result = new JSONObject();
		// 成就详情
		int achievementId = postData.getInt("achievement_id");
		JSONObject achievementInfo = achievementBiz.getAchievementInfoById(achievementId);
		if (!Dto.isObjNull(achievementInfo)) {
			// 用户成就
			JSONObject userAchievement = achievementBiz.getUserAchievementByAccountAndGameId(account,
					achievementInfo.getInt("game_id"));
			if (!Dto.isObjNull(userAchievement)
					&& userAchievement.getJSONArray("reward_array").contains(achievementId)) {
				double reward = achievementInfo.getDouble("reward");
				String rewardType = null;
				if (achievementInfo.getInt("reward_type") == CommonConstant.CURRENCY_TYPE_COINS) {
					rewardType = "coins";
				}
				if (!Dto.stringIsNULL(rewardType)) {
					// 更新成就信息
					JSONObject newUserAchievement = new JSONObject();
					newUserAchievement.put("id", userAchievement.getLong("id"));
					JSONArray rewardArray = new JSONArray();
					JSONArray drawArray = userAchievement.getJSONArray("draw_array");
					for (int i = 0; i < userAchievement.getJSONArray("reward_array").size(); i++) {
						if (userAchievement.getJSONArray("reward_array").getInt(i) == achievementId) {
							drawArray.add(achievementId);
						} else {
							rewardArray.add(userAchievement.getJSONArray("reward_array").getInt(i));
						}
					}
					newUserAchievement.put("reward_array", rewardArray);
					newUserAchievement.put("draw_array", drawArray);
					achievementBiz.updateUserAchievement(newUserAchievement);
					// 更新奖励
					JSONArray array = new JSONArray();
					JSONObject obj = new JSONObject();
					obj.put("total", 1);
					obj.put("fen", reward);
					obj.put("id", userId);
					array.add(obj);
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("array", array);
					jsonObject.put("updateType", rewardType);
					producerService.sendMessage(daoQueueDestination,
							new PumpDao(DaoTypeConstant.UPDATE_SCORE, jsonObject));
					// 通知前端
					result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
					result.put(CommonConstant.RESULT_KEY_MSG, "领取成功");
					// 刷新成就
					JSONArray achievementArray = achievementBiz.getAchievementInfoByGameId(
							achievementInfo.getInt("game_id"), achievementInfo.getString("platform"));
					JSONArray arr = new JSONArray();
					for (Object o : achievementArray) {
						JSONObject achievement = JSONObject.fromObject(o);
						// 可以领取 已经领取 不能领
						if (rewardArray.contains(achievement.getLong("id"))) {
							achievement.put("status", 1);
						} else if (drawArray.contains(achievement.getLong("id"))) {
							achievement.put("status", 0);
						} else {
							achievement.put("status", 2);
						}
						arr.add(achievement);
					}
					result.put("achievement_array", arr);
					CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "drawAchievementRewardPush");
					return;
				}
			}
		}
		result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
		result.put(CommonConstant.RESULT_KEY_MSG, "领取失败");
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "drawAchievementRewardPush");
	}

	public void changeRoomBase(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
			return;
		}
		// 玩家账号
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		// 房间号
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		// 找到当前房间对象
		GameRoom nowRoom = RoomManage.gameRoomMap.get(roomNo);
		// 第一步、完成换房间需要先判断是否有空房间，
		// 若有继续第二步，若无直接返回无空房间信息
		List temp = haveEmptyRoom(nowRoom);
		if (temp != null && temp.size() > 0) {
			// 第二步、玩家退出房间
			JSONObject exitData = fromObject(data);

			exitData.put("notSend", 1);
			exitData.put("notSendToMe", 1);

			switch (nowRoom.getGid()) {
			case CommonConstant.GAME_ID_NN:
				nnGameEventDealNew.exitRoom(null, exitData);
				break;
			case CommonConstant.GAME_ID_SSS:
				sssGameEventDealNew.exitRoom(null, exitData);
				break;
			case CommonConstant.GAME_ID_ZJH:
				zjhGameEventDealNew.exitRoom(null, exitData);
				break;
			case CommonConstant.GAME_ID_SW:
				swGameEventDeal.exitRoom(null, exitData);
				break;
			default:
				break;
			}
			if (!nowRoom.getPlayerMap().containsKey(account)) {
				// 第三步、玩家加入相应房间
				JSONObject joinData = new JSONObject();
				// 随机取出一个房间号加入
				int index = RandomUtils.nextInt(temp.size());
				joinData.put("account", account);
				joinData.put("room_no", temp.get(index));
				JSONObject userInfo = userBiz.getUserByAccount(account);
				joinData.put("uuid", Dto.isObjNull(userInfo) ? "" : userInfo.getString("uuid"));
				if (joinData.containsKey("location")) {
					joinData.put("location", postData.get("location"));
				}
				joinRoomBase(client, joinData);
			}

		} else {
			// 通知玩家
			JSONObject result = new JSONObject();
			result.put("type", CommonConstant.SHOW_MSG_TYPE_NORMAL);
			result.put(CommonConstant.RESULT_KEY_MSG, "当前无法换桌");
			CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "tipMsgPush");
		}
	}

	private List<String> haveEmptyRoom(GameRoom room) {
		// 根据游戏id查找对应的房间数
		// 然后进行遍历
		// 最后返回查询结果
		Map<String, GameRoom> map = RoomManage.gameRoomMap;
		List<String> list = new ArrayList<>();

		for (String roomNum : map.keySet()) {
			GameRoom gameRoom = map.get(roomNum);
			// 判断房间状态
			if (room.getGid() == gameRoom.getGid() && room.getRoomType() == gameRoom.getRoomType()
					&& gameRoom.getPlayerMap().size() < gameRoom.getPlayerCount() && !roomNum.equals(room.getRoomNo())
					&& room.getScore() == gameRoom.getScore() && room.getWfType() == gameRoom.getWfType()) {
				list.add(roomNum);
			}
		}
		return list;
	}

	public void getRoomCardGameLogList(SocketIOClient client, Object data) {
		String eventName = "getRoomCardGameLogListPush";
		JSONObject object = JSONObject.fromObject(data);
		// 用户id
		long userId = object.getLong("user_id");
		// 房间类别
		int roomType = object.getInt("roomType");
		Integer gameId = object.containsKey("gid") ? object.getInt("gid") : null;
		// 俱乐部编号
		int pageIndex = !object.containsKey("pageIndex") ? 1 : object.getInt("pageIndex");// 第几页
		int pageSize = !object.containsKey("pageSize") ? 6 : object.getInt("pageSize");// 一页几条

		String time = DateUtils.searchTime(object.get("time"));// 日期搜索
		JSONArray userGameLogs = new JSONArray();
		JSONObject logs = gameLogBiz.pageUserGameLogsByUserId(userId, gameId, null, null, new int[] { roomType }, time,
				pageIndex, pageSize);
		if (logs.containsKey("list"))
			userGameLogs = logs.getJSONArray("list");
		JSONArray result = new JSONArray();
		for (int i = 0; i < userGameLogs.size(); i++) {
			JSONObject userRes = new JSONObject();
			JSONObject o = userGameLogs.getJSONObject(i);
			if (null == o)
				continue;
			TimeUtil.transTimeStamp(o, "yyyy-MM-dd HH:mm:ss", "create_time");
			JSONArray gameList = DBUtil.getObjectListBySQL(
					"SELECT * FROM za_user_game_statis z WHERE z.`room_id`= ? AND z.`room_no`= ?",
					new Object[] { o.get("room_id"), o.get("room_no") });
			if (null == gameList || gameList.size() == 0)
				continue;
			JSONArray userResult = new JSONArray();
			Collections.sort(gameList, new Comparator<JSONObject>() {
				@Override
				public int compare(JSONObject o1, JSONObject o2) {
					double do1 = Dto.sub(o1.containsKey("score") ? o1.getDouble("score") : 0D,
							o1.containsKey("cut_score") ? o1.getDouble("cut_score") : 0D);
					double do2 = Dto.sub(o2.containsKey("score") ? o2.getDouble("score") : 0D,
							o2.containsKey("cut_score") ? o2.getDouble("cut_score") : 0D);
					if (do1 > do2)
						return -1;
					return 1;
				}
			});

			JSONObject attachGameLog = null;
			JSONObject gameObject = gameList.getJSONObject(0);
			if (gameObject.containsKey("za_games_id")
					&& CommonConstant.GAME_ID_QZMJ == gameObject.getInt("za_games_id")) {// 目前只支持泉州麻将
				if (gameObject.containsKey("sole")) {
					attachGameLog = redisInfoService.getAttachGameLog(gameObject.get("sole"));
				}
			}

			for (int j = 0; j < gameList.size(); j++) {
				JSONObject userObject = gameList.getJSONObject(j);
				TimeUtil.transTimeStamp(userObject, "yyyy-MM-dd HH:mm:ss", "create_time");
				String account = userObject.getString("account");
				JSONObject temp = new JSONObject().element("createTime", userObject.get("create_time"))
						.element("cutScore", userObject.get("cut_score")).element("cutType", userObject.get("cut_type"))
						.element("player", userObject.get("name")).element("score", userObject.get("score"))
						.element("account", account).element("headimg", userObject.get("headimg"))
						.element("gameId", userObject.get("za_games_id"));
				if (null != attachGameLog && attachGameLog.containsKey(account)) {
					temp.element("extraInfo", attachGameLog.getString(account));
				}
				userResult.add(temp);
			}
			userRes.element("playermap", userResult).element("room_no", o.get("room_no"))
					.element("room_id", o.get("room_id")).element("createTime", o.get("create_time"))
					.element("room_type", o.get("room_type"));

			result.add(userRes);
		}

		JSONObject userRecordInfo = gameLogBiz.getRecordInfo(userId, time);
		String nickName = "";// 昵称
		String roomCard = "";// 房卡
		String headimg = "";// 头像
		String totalNumber = "";// 总场次
		String totalScore = "";// 总得分
		String bigWinnerCount = "";// 大赢家次数
		String bigWinnerScore = "";// 大赢家得分

		if (null != userRecordInfo) {
			nickName = userRecordInfo.getString("name");// 昵称
			roomCard = Dto.zerolearing(userRecordInfo.getDouble("roomcard"));// 房卡
			headimg = CommonConstant.getServerDomain() + userRecordInfo.getString("headimg");// 头像
			totalNumber = userRecordInfo.getString("totalNumber");// 总场次
			totalScore = userRecordInfo.getString("totalScore");// 总得分
			bigWinnerCount = userRecordInfo.getString("bigWinnerCount");// 大赢家次数
			bigWinnerScore = userRecordInfo.getString("bigWinnerScore");// 大赢家得分
		}

		JSONObject back = new JSONObject();
		back.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		back.element("data", result).element("gid", gameId).element("nickName", nickName).element("roomCard", roomCard)
				.element("headimg", headimg).element("totalNumber", totalNumber).element("totalScore", totalScore)
				.element("bigWinnerCount", bigWinnerCount).element("bigWinnerScore", bigWinnerScore)
				.element("pageIndex", logs.containsKey("pageIndex") ? logs.getInt("pageIndex") : 1)
				.element("pageSize", logs.containsKey("pageSize") ? logs.getInt("pageSize") : 6)
				.element("totalPage", logs.containsKey("totalPage") ? logs.getInt("totalPage") : 0)
				.element("totalCount", logs.containsKey("totalCount") ? logs.getInt("totalCount") : 0);
		logger.info(back.toString());
		CommonConstant.sendMsgEvent(client, back, eventName);
	}

	public void getClubGameLogList(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		// 用户id
		long userId = postData.getLong("user_id");
		// 游戏id
		int gameId = postData.getInt("gid");
		String roomId = postData.containsKey("roomId") ? postData.getString("roomId") : null;
		// 房间类别
		int roomType = postData.getInt("roomType");
		// 俱乐部编号
		String clubCode = postData.containsKey("clubCode") ? postData.getString("clubCode") : null;
		// 分页
		int pageIndex = postData.containsKey("pageIndex") ? postData.getInt("pageIndex") : 0;
		// 俱乐部信息
		if (Dto.stringIsNULL(clubCode)) {
			return;
		}
		int pageStart = pageIndex * CommonConstant.GAME_LOG_SIZE_PER_PAGE;
		List<String> list = new ArrayList<>();
		JSONObject clubInfo = clubBiz.getClubByCode(clubCode);
		List<JSONObject> allLogs = new ArrayList<>();
		if (!Dto.isObjNull(clubInfo)) {
			if (userId == clubInfo.getLong("leaderId")) {
				// 去除重复房间
				JSONArray newRoomList = roomBiz.getRoomListByClubCode(clubCode, gameId, pageStart,
						CommonConstant.GAME_LOG_SIZE_PER_PAGE);
				if (newRoomList.size() > 0) {
					for (int i = 0; i < newRoomList.size(); i++) {
						list.add(newRoomList.getJSONObject(i).getString("room_no"));
					}
					// 用户战绩
					JSONArray userGameLogsByUserId = gameLogBiz.getUserGameLogsByUserId(userId, gameId, roomType, list,
							clubCode, roomId);
					// 汇总
					List<JSONObject> summaryLogs = summaryLogs(newRoomList, userGameLogsByUserId, userId);
					allLogs.addAll(summaryLogs);
				}
			}
		}
		JSONObject result = new JSONObject();
		if (allLogs.size() > 0 || pageIndex > 0) {
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			result.put("pageIndex", pageIndex);
			result.put("gid", gameId);
			result.put("data", allLogs);
		} else {
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
		}
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getClubGameLogListPush");
	}

	public void getRoomCardGameLogDetail(SocketIOClient client, Object data) {
		JSONObject object = JSONObject.fromObject(data);
		if (!JsonUtil.isNullVal(object, new String[] { "roomId", "room_no" })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[roomId,room_no]", "getRoomCardGameLogDetailPush");
			return;
		}
		// 房间号
		String roomNo = object.getString("room_no");
		long roomId = object.getLong("roomId");
		// 用户战绩
		String sql = "SELECT `create_time` AS createTime,`id`,`room_no`,`result`,`gamelog_id`,`game_index`,`room_type` FROM `za_users_game_logs_result` where `room_no` = ? and `room_id` = ? ORDER BY `game_index` ASC ";
		JSONArray array = DBUtil.getObjectListBySQL(sql, new Object[] { roomNo, roomId });
		// 用户战绩
		for (int i = 0; i < array.size(); i++) {
			JSONObject o = array.getJSONObject(i);
			if (null == o)
				continue;
			if (o.containsKey("gamelog_id")) {// 防止数字过长传输过程中丢失，转成string
				o.element("gamelog_id", o.getString("gamelog_id"));
			}
			TimeUtil.transTimeStamp(o, "yyyy-MM-dd HH:mm:ss", "createTime");
		}
		JSONObject result = new JSONObject();
		result.element("list", array);
		result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		CommonConstant.sendMsgEvent(client, result, "getRoomCardGameLogDetailPush");

	}

	private List<JSONObject> summaryLogs(JSONArray roomList, JSONArray gameLogList, long userId) {
		List<JSONObject> summaryList = new ArrayList<>();
		// 遍历所有有效房间
		for (int i = 0; i < roomList.size(); i++) {
			JSONObject roomObj = roomList.getJSONObject(i);
			// 取当前房间的战绩记录
			List<JSONObject> gameLogByRoomNo = getGameLogByRoomNo(gameLogList, roomObj.getString("room_no"));
			// 有战绩记录
			if (gameLogByRoomNo.size() > 0) {
				// result字段添加account防止报错
				try {
					JSONObject obj = new JSONObject();
					obj.put("createtime", roomObj.getString("createtime"));
					obj.put("room_no", roomObj.getString("room_no"));
					obj.put("room_id", roomObj.get("id"));
					obj.put("roomindex", roomObj.getString("id"));
					obj.put("userId", userId);
					obj.put("result", getTotalSumList(gameLogByRoomNo));
					summaryList.add(obj);
				} catch (Exception e) {
					logger.error("", e);
				}
			}
		}
		return summaryList;
	}

	private List<JSONObject> getTotalSumList(List<JSONObject> gameLogList) {
		List<JSONObject> totalSum = new ArrayList<>();
		if (gameLogList.size() > 0) {
			String roomNo = gameLogList.get(0).getString("room_no");
			// 玩家输赢统计
			Map<String, Integer> sumMap = new HashMap<>();
			// 玩家昵称统计
			Map<String, String> playerMap = new HashMap<>();
			// 玩家昵称
			Map<String, String> accountMap = new HashMap<>();
			for (JSONObject object : gameLogList) {
				// 确保与当前房间相同
				if (roomNo.equals(object.getString("room_no"))) {
					// 输赢结果
					JSONArray result = object.getJSONArray("result");
					// 统计
					for (int i = 0; i < result.size(); i++) {
						JSONObject sum = result.getJSONObject(i);
						// 已有结果累计输赢，否则添加当局结果
						if (sumMap.containsKey(sum.getString("account"))
								&& playerMap.containsKey(sum.getString("account"))) {
							sumMap.put(sum.getString("account"),
									sumMap.get(sum.getString("account")) + sum.getInt("score"));
						} else {
							sumMap.put(sum.getString("account"), sum.getInt("score"));
							playerMap.put(sum.getString("account"), sum.getString("player"));
						}
						accountMap.put(sum.getString("account"), sum.getString("account"));
					}
				}
			}
			// 添加结果
			if (sumMap.size() > 0 && playerMap.size() > 0) {
				for (String account : sumMap.keySet()) {
					if (playerMap.containsKey(account)) {
						totalSum.add(new JSONObject().element("score", sumMap.get(account))
								.element("player", playerMap.get(account)).element("account", accountMap.get(account)));
					}
				}
			}
			Collections.sort(totalSum, new Comparator<JSONObject>() {
				@Override
				public int compare(JSONObject o1, JSONObject o2) {
					return o2.getInt("score") - o1.getInt("score");
				}
			});
		}
		return totalSum;
	}

	private List<JSONObject> getGameLogByRoomNo(JSONArray gameLogList, String roomNo) {
		List<JSONObject> gameLog = new ArrayList<>();
		for (int i = 0; i < gameLogList.size(); i++) {
			JSONObject obj = gameLogList.getJSONObject(i);
			if (obj.getString("room_no").equals(roomNo)) {
				gameLog.add(obj);
			}
		}
		return gameLog;
	}

	public void checkUserOnlineStatus(SocketIOClient client) {
		if (client.has(CommonConstant.DATA_KEY_ACCOUNT)) {
			// 获取玩家账号
			String account = client.get(CommonConstant.DATA_KEY_ACCOUNT);
			// 存入缓存
			redisService.hset("online_player_list", account, TimeUtil.getNowDate());
			for (String roomNo : RoomManage.gameRoomMap.keySet()) {
				// 如果当前玩家是离线状态则刷新，防止断掉心跳包没有触发重连
				if (RoomManage.gameRoomMap.get(roomNo).getPlayerMap().containsKey(account)
						&& RoomManage.gameRoomMap.get(roomNo).getPlayerMap().get(account) != null
						&& RoomManage.gameRoomMap.get(roomNo).getPlayerMap().get(account)
								.getStatus() == CommonConstant.GLOBAL_NO) {
					// 设置状态
					RoomManage.gameRoomMap.get(roomNo).getPlayerMap().get(account).setStatus(CommonConstant.GLOBAL_YES);
					JSONObject result = new JSONObject();
					result.put("index", RoomManage.gameRoomMap.get(roomNo).getPlayerMap().get(account).getMyIndex());
					// 通知玩家
					CommonConstant.sendMsgEventToAll(RoomManage.gameRoomMap.get(roomNo).getAllUUIDList(account),
							String.valueOf(result), "userReconnectPush");
					break;
				}
			}
		}
	}

	// @Scheduled(cron = "0/3 * * * * ?")
	public void checkUserOnlineStatus() {
		// 所有玩家的心跳集合
		Map<Object, Object> onlinePlayerList = redisService.hmget("online_player_list");
		if (onlinePlayerList != null && onlinePlayerList.size() > 0) {
			for (Object account : onlinePlayerList.keySet()) {
				// 上一次心跳时间
				String lastPingTime = String.valueOf(onlinePlayerList.get(account));
				String difference = TimeUtil.getDaysBetweenTwoTime(lastPingTime, TimeUtil.getNowDate(), 1000L);
				// 超过3秒没有ping视为断线
				if (Integer.parseInt(difference) > 3) {
					for (String roomNo : RoomManage.gameRoomMap.keySet()) {
						if (RoomManage.gameRoomMap.get(roomNo).getPlayerMap().containsKey(account)
								&& RoomManage.gameRoomMap.get(roomNo).getPlayerMap().get(account) != null) {
							RoomManage.gameRoomMap.get(roomNo).getPlayerMap().get(account)
									.setStatus(CommonConstant.GLOBAL_NO);
							// 通知玩家
							JSONObject result = new JSONObject();
							result.put("index",
									RoomManage.gameRoomMap.get(roomNo).getPlayerMap().get(account).getMyIndex());
							CommonConstant.sendMsgEventToAll(RoomManage.gameRoomMap.get(roomNo).getAllUUIDList(),
									String.valueOf(result), "userDisconnectPush");
							// 删除缓存，防止多次通知
							redisService.hdel("online_player_list", account);
							break;
						}
					}
				}
			}
		}
	}

	public void checkBindStatus(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		JSONObject result = new JSONObject();
		if (redisService.sHasKey(CacheKeyConstant.BIND_SET, account)) {
			result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		} else {
			JSONObject userInfo = userBiz.getUserByAccount(account);
			if (!Dto.isObjNull(userInfo) && userInfo.containsKey("tel") && !Dto.stringIsNULL(userInfo.getString("tel"))
					&& Pattern.matches(RegexConstant.REGEX_MOBILE, userInfo.getString("tel"))) {
				redisService.sSet(CacheKeyConstant.BIND_SET, account);
				result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			} else {
				result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			}
		}
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "checkBindStatusPush");
	}

	public void userBind(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
//        String uuid = postData.getString("uuid");
		String tel = postData.getString("tel");
		String password = postData.getString("password");
		String authCode = postData.getString("authCode");
		String eventName = "userBindPush";// 回调事件名
		if (!Pattern.matches(RegexConstant.REGEX_MOBILE, tel)) {
			CommonConstant.sendMsgEventNo(client, "请输入正确的手机号", null, eventName);
			return;
		}
		Object verificationInfo = redisService.hget(CacheKeyConstant.VERIFICATION_MAP, tel);
		if (verificationInfo == null) {
			CommonConstant.sendMsgEventNo(client, "请重新获取验证码", null, eventName);
			return;
		}
		if (!String.valueOf(verificationInfo).equals(authCode)) {
			CommonConstant.sendMsgEventNo(client, "请输入正确的验证码", null, eventName);
			return;
		}
		JSONObject userInfo = userBiz.getUserByAccount(account);
		// 用户是否存在
		if (userInfo == null || !userInfo.containsKey("platform")) {
			CommonConstant.sendMsgEventNo(client, "用户信息不存在", null, eventName);
			return;
		}
		// 不可重复绑定
		if (Pattern.matches(RegexConstant.REGEX_MOBILE, String.valueOf(userInfo.get("tel")))) {
			CommonConstant.sendMsgEventNo(client, "不可重复绑定", null, eventName);
			return;
		}
		JSONObject userInfoByTel = DBUtil.getObjectBySQL(
				"select id from za_users where tel=? and status=1 AND `platform`= ? ",
				new Object[] { tel, userInfo.getString("platform") });
		// 有用户同步信息，没用注册
		if (userInfoByTel != null) {
			CommonConstant.sendMsgEventNo(client, "该手机已经绑定其他账号", null, eventName);
			return;
		}
		JSONObject updateInfo = new JSONObject();
		updateInfo.put("id", userInfo.getLong("id"));
		updateInfo.put("tel", tel);
		updateInfo.put("password", password);
		userBiz.updateUserInfo(updateInfo);
		CommonConstant.sendMsgEventYes(client, "绑定成功", eventName);
	}

	public void addVirtualRoom(int gameId, int score, String platform, String circleCode, JSONObject conditions) {
		int ratio = 100;
		JSONObject baseInfo = new JSONObject();
		// 亲友圈
		baseInfo.put("roomType", CommonConstant.ROOM_TYPE_INNING + "");
		baseInfo.put("open", "1");
		baseInfo.put("yuanbao", score);
		baseInfo.put("score", score);
		int leaveYB = conditions.containsKey("leaveYB") ? conditions.getInt("leaveYB") : 0;
		int enterYB = conditions.containsKey("enterYB") ? conditions.getInt("enterYB") : score * ratio;
		// 离场
		baseInfo.put("leaveYB", leaveYB);
		// 入场
		baseInfo.put("enterYB", enterYB);
		// 获取房间设置
		JSONArray gameSetting = redisInfoService.getGameSetting(gameId, platform, 9);
		if (gameSetting != null) {
			GameRoom gameRoom = gameId == CommonConstant.GAME_ID_QZMJ ? new QZMJGameRoom()
					: gameId == CommonConstant.GAME_ID_SSS ? new SSSGameRoomNew()
							: gameId == CommonConstant.GAME_ID_PDK ? new PDKGameRoom() : null;
			if (gameRoom != null) {
				// 玩家人数
				int playerCount = gameId == CommonConstant.GAME_ID_SSS ? 6 : 2;
				// 游戏局数
				int gameCount = 0;
				// 十三水封装创建房间参数
				if (gameId == CommonConstant.GAME_ID_SSS) {
					gameCount = 10;
					// 初始化房间配置
					JSONObject turnObj = new JSONObject().element("roomcard", "7").element("turn", "10")
							.element("AANum", "7");
					baseInfo.put("player", playerCount);
					baseInfo.put("type", "0");
					baseInfo.put("roomType", "9");
					baseInfo.put("isAllStart", CommonConstant.IS_ALL_START_Y);
					baseInfo.put("turn", turnObj);
					baseInfo.put("paytype", CommonConstant.ROOM_PAY_TYPE_FUND);
					baseInfo.put("color", "0");
					baseInfo.put("jiama", SSSConstant.SSS_MP_TYPE_ALL_W);
					baseInfo.put("fire", SSSConstant.TEA_FIRE_Y);
					baseInfo.put("pattern", SSSConstant.TEA_PATTERN_1);
					baseInfo.put("cutType", CircleConstant.CUTTYPE_WU);
					baseInfo.put("pump", 1);
					baseInfo.put("cutList", "1");
				}
				if (gameId == CommonConstant.GAME_ID_QZMJ) {
					baseInfo.put("player", playerCount);
					baseInfo.put("roomType", "9");
					baseInfo.put("turn",
							new JSONObject().element("roomcard", "11").element("turn", "50").element("AANum", "11"));
					baseInfo.put("zhuangXian", "15/10");
					baseInfo.put("type", "4");
					baseInfo.put("isNotChiHu", 0);
					baseInfo.put("hasjinnopinghu", 2);
					baseInfo.put("playTime", "30");
					baseInfo.put("paytype", "2");
					baseInfo.put("cutType", "4");
					baseInfo.put("isPlayerDismiss", "0");
					baseInfo.put("pump", 1);
					baseInfo.put("cutList", "1");
					baseInfo.put("fanMultiple", score + "");
					baseInfo.put("keScoreVal", score * 100);
					gameCount = 50;
				}
				if (gameId == CommonConstant.GAME_ID_PDK) {
					baseInfo.put("player", playerCount);
					baseInfo.put("roomType", "9");
					baseInfo.put("waysType", "16");
					baseInfo.put("turn",
							new JSONObject().element("roomcard", "9").element("turn", "8").element("AANum", "9"));
					baseInfo.put("payRules", "1");
					baseInfo.put("passCard", "1");
					baseInfo.put("settleType", "1");
					baseInfo.put("paytype", "2");
					baseInfo.put("bombsScore", "2");
					baseInfo.put("playTime", "30");
					baseInfo.put("cutType", "4");
					baseInfo.put("pump", 1);
					baseInfo.put("cutList", "1");
					gameCount = 8;
				}
				if (gameCount == 0) {
					return;
				}
				// 更新房间配置信息
				conditions.forEach(baseInfo::put);
				// 局数信息
				String turn = conditions.containsKey("turn") ? conditions.getString("turn") : "";
				// 更新局数
				if (Dto.isJsonObj(conditions.getString("turn"))) {
					JSONObject turnObj = JSONObject.fromObject(turn);
					if (turnObj.containsKey("turn")) {
						gameCount = turnObj.getInt("turn");
					}
				}
				// 更新房间人数
				if (conditions.containsKey("player")) {
					playerCount = conditions.getInt("player");
				}
				// 查询牌友圈id，否则加入提示请稍后再试
				String sql = "SELECT id FROM `game_circle_info` WHERE circle_code=? AND is_delete='N' AND circle_status=2";
				JSONObject circleInfo = DBUtil.getObjectBySQL(sql, new Object[] { circleCode });
				if (Dto.isObjNull(circleInfo)) {
					return;
				}
				// 查询空闲机器人
				JSONArray freeRobotList = robotBiz.getFreeRobotByCount(playerCount);
				if (freeRobotList.size() != playerCount) {
					return;
				}
				List<Long> idList = new ArrayList<>();
				String roomNo = randomRoomNo();
				gameRoom.setRoomType(baseInfo.get("roomType") != null ? baseInfo.getInt("roomType")
						: CommonConstant.ROOM_TYPE_INNING);
				gameRoom.setRoomNo(roomNo);
				gameRoom.setGid(gameId);
				gameRoom.setOpen(true);
				gameRoom.setRoomInfo(baseInfo);
				gameRoom.setPlayerCount(playerCount);
				gameRoom.setGameIndex(0);
				gameRoom.setGameNewIndex(0);
				gameRoom.setGameCount(gameCount);
				gameRoom.setVirtual(true);
				gameRoom.setScore(score);
				ConcurrentHashMap<String, Playerinfo> playerinfoConcurrentHashMap = new ConcurrentHashMap<>();
				for (int i = 0; i < playerCount; i++) {
					Playerinfo playerinfo1 = new Playerinfo();
					playerinfo1.setName(freeRobotList.getJSONObject(i).getString("name"));
					// 判断是否是麻将课玩法且是一课玩法
					if (CommonConstant.GAME_ID_QZMJ == gameId && gameCount == 50) {
						QZMJGameRoom temp = (QZMJGameRoom) gameRoom;
						playerinfo1.setScore(0);
						playerinfo1.setSourceScore(enterYB);
						temp.setKeScoreVal(enterYB);
					} else {
						playerinfo1.setScore(RandomUtils.nextInt(1000 * score));
						playerinfo1.setSourceScore(freeRobotList.getJSONObject(i).getInt("score_hp") + score * 20);
					}
					playerinfo1.setHeadimg(freeRobotList.getJSONObject(i).getString("head_img"));
					playerinfo1.setId(freeRobotList.getJSONObject(i).getLong("id"));
					playerinfoConcurrentHashMap.put(String.valueOf(i), playerinfo1);
					idList.add(freeRobotList.getJSONObject(i).getLong("id"));
				}
				gameRoom.setClubCode(circleCode);
				gameRoom.setCircleId(circleInfo.getString("id"));
				gameRoom.setVisit(CommonConstant.ROOM_VISIT_NO);
				gameRoom.setPlayerMap(playerinfoConcurrentHashMap);
				gameRoom.setUserIdList(new CopyOnWriteArrayList<>());
				// 更新状态
				int result = robotBiz.batchUpdateRobotStatus(idList, 1);
				if (result < 0) {
					return;
				}
				if (CommonConstant.GAME_ID_SSS == gameId) {
					// 鬼牌数量
					int hardGhostNumber = conditions.containsKey("hardGhostNumber")
							&& Dto.isNumber(conditions.getString("hardGhostNumber"))
									? conditions.getInt("hardGhostNumber")
									: 0;
					// 鬼牌校验
					if (hardGhostNumber < 0 || hardGhostNumber > 9) {
						hardGhostNumber = 0;
					}
					SSSGameRoomNew sssGameRoomNew = (SSSGameRoomNew) gameRoom;
					sssGameRoomNew.setHardGhostNumber(hardGhostNumber);
					createRoomSSS(sssGameRoomNew, baseInfo, "-1");
				}
				if (CommonConstant.GAME_ID_PDK == gameId) {
					createRoomPdk((PDKGameRoom) gameRoom, baseInfo, "-1", CommonConstant.GAME_ID_PDK);
				}
				if (CommonConstant.GAME_ID_QZMJ == gameId) {
					int temp = baseInfo.containsKey("hasjinnopinghu")
							&& Dto.isNumber(baseInfo.getString("hasjinnopinghu"))
									? Integer.valueOf(baseInfo.getString("hasjinnopinghu"))
									: 0;
					baseInfo.put("hasjinnopinghu", temp);
					baseInfo.put("isNotChiHu", baseInfo.getInt("isNotChiHu"));
					baseInfo.put("di", score);
					createRoomQZMJ((QZMJGameRoom) gameRoom, baseInfo, "-1", CommonConstant.GAME_ID_QZMJ);
				}
				RoomManage.gameRoomMap.put(roomNo, gameRoom);
				Playerinfo playerinfo = new Playerinfo();
				playerinfo.setName("虚拟房间");
				gameRoom.setPlatform(platform);
				addGameRoom(gameRoom, playerinfo);
			}

		}
	}

	@Scheduled(cron = "0/20 * * * * ?")
	public void refreshVirtualRoom() {
		for (String roomNo : RoomManage.gameRoomMap.keySet()) {
			GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
			// 课分
			int keScoreVal = 0;
			if (CommonConstant.GAME_ID_QZMJ == gameRoom.getGid() && 50 == gameRoom.getGameCount()) {
				QZMJGameRoom temp = (QZMJGameRoom) gameRoom;
				keScoreVal = temp.getKeScoreVal();
			}
			if (gameRoom.isVirtual()) {
				int flag = RandomUtils.nextInt(30);
				if (flag < 10) {
					// 结束的局数
					int gameCount = gameRoom.getGameCount();
					if (gameCount == 50 && gameRoom.getGid() == CommonConstant.GAME_ID_QZMJ) {
						gameCount = RandomUtils.nextInt(7) + 4;
					}
					if (gameCount <= gameRoom.getGameNewIndex()) {
						// 随机退出人数
						int outCount = RandomUtils.nextInt(gameRoom.getPlayerCount());
						if (outCount > 0 && outCount < gameRoom.getPlayerCount()) {
							JSONArray freeRobotList = robotBiz.getFreeRobotByCount(outCount);
							if (freeRobotList.size() > 0) {
								List<Long> outIdList = new ArrayList<>();
								List<Long> inIdList = new ArrayList<>();
								int index = 0;
								for (String account : gameRoom.getPlayerMap().keySet()) {
									Playerinfo playerinfo1 = gameRoom.getPlayerMap().get(account);
									if (index < freeRobotList.size()) {
										outIdList.add(playerinfo1.getId());
										inIdList.add(freeRobotList.getJSONObject(index).getLong("id"));
										playerinfo1.setName(freeRobotList.getJSONObject(index).getString("name"));
										playerinfo1.setScore(RandomUtils.nextInt(100 * gameRoom.getScore()));
										playerinfo1.setSourceScore(freeRobotList.getJSONObject(index).getInt("score_hp")
												+ gameRoom.getScore() * 20);
										playerinfo1
												.setHeadimg(freeRobotList.getJSONObject(index).getString("head_img"));
										playerinfo1.setId(freeRobotList.getJSONObject(index).getLong("id"));
										gameRoom.getPlayerMap().put(account, playerinfo1);
										index++;
									}
									// 麻将课玩法
									if (gameRoom.getGameCount() == 50
											&& gameRoom.getGid() == CommonConstant.GAME_ID_QZMJ) {
										playerinfo1.setScore(0);
										playerinfo1.setSourceScore(keScoreVal);
									}
								}
								robotBiz.batchUpdateRobotStatus(inIdList, 1);
								robotBiz.batchUpdateRobotStatus(outIdList, 0);
							}
						}
						gameRoom.setGameIndex(0);
						gameRoom.setGameNewIndex(0);
					} else {
						gameRoom.setGameIndex(gameRoom.getGameIndex() + 1);
						gameRoom.setGameNewIndex(gameRoom.getGameNewIndex() + 1);
						int index = 0;
						// 赢家分数
						int winnerScoreTotal = 0;
						// 赢家数量
						int winnerCount = RandomUtils.nextInt(gameRoom.getPlayerCount() - 1) + 1;
						// 麻将课玩法第一局不扣分
						if (gameRoom.getGameCount() == 50 && gameRoom.getGid() == CommonConstant.GAME_ID_QZMJ
								&& gameRoom.getGameNewIndex() == 1) {
							return;
						}
						for (String account : gameRoom.getPlayerMap().keySet()) {
							Playerinfo playerinfo1 = gameRoom.getPlayerMap().get(account);
							int score = RandomUtils.nextInt(6);
							int symbol = RandomUtils.nextInt(2);
							if (symbol % 2 == 0) {
								score = -score;
							}
							gameRoom.getPlayerMap().get(account).setScore(
									gameRoom.getPlayerMap().get(account).getScore() + score * gameRoom.getScore());
							// 麻将课玩法
							if (gameRoom.getGameCount() == 50 && gameRoom.getGid() == CommonConstant.GAME_ID_QZMJ) {
								playerinfo1.setSourceScore(keScoreVal);
								if (index < winnerCount) {
									int winScore = RandomUtils.nextInt(99) + 1;
									if (winScore + winnerScoreTotal > 100) {
										winScore = winScore / 10;
									}
									winScore = winScore * gameRoom.getScore();
									playerinfo1.setScore(winScore);
									winnerScoreTotal += winScore;
								} else {
									if (index == gameRoom.getPlayerMap().size() - 1) {
										playerinfo1.setScore(-winnerScoreTotal);
									} else {
										int winScore = -RandomUtils.nextInt(winnerScoreTotal / gameRoom.getScore())
												* gameRoom.getScore();
										playerinfo1.setScore(winScore);
										winnerScoreTotal += winScore;
									}
								}
							}
							index++;
						}
					}
				}
			}
		}
	}

	public void reloadClearRoom(String roomNo) {
		GameRoom room = RoomManage.gameRoomMap.get(roomNo);
		if (null == room)
			return;
		JSONObject obj = new JSONObject();
		obj.put("player_number", 0);
		obj.put("room_id", room.getId());
		obj.put("game_id", room.getGid());
		obj.put("room_no_old", roomNo);
		obj.put("room_no", room.getRoomNo());
		obj.put("roomtype", room.getRoomType());
		obj.put("base_info", room.getRoomInfo());
		obj.put("createtime", TimeUtil.getNowDate());
		obj.put("game_count", room.getGameCount());
		if (room.getPlatform() != null)
			obj.put("platform", room.getPlatform());
		obj.put("ip", room.getIp());
		obj.put("port", room.getPort());
		obj.put("status", 0);
		obj.put("level", room.getClubCode());

		if (room.isOpen()) {
			obj.put("open", CommonConstant.GLOBAL_YES);
		} else {
			obj.put("open", CommonConstant.GLOBAL_NO);
		}
//添加新的房间
		room.setId(0L);
//重新生成房间处理
		
		long roomId = roomBiz.reloadCreateRoom(obj);
		if (0L >= roomId) {
			JSONObject roomInfo = DBUtil.getObjectBySQL("select id from za_gamerooms where room_no=? order by id desc",
					new Object[] { roomNo });
			if (null != roomInfo && roomInfo.containsKey("id")) {
				roomId = roomInfo.getLong("id");
			}
		}

		if (0L < roomId) {
			room.setId(roomId);
		}
		
		if (room.isRobot()) {
			robotEventDeal.robotJoin(roomNo);
		}
		
	}

	public void updateZaGamerooms(String roomNo) {
		GameRoom room = RoomManage.gameRoomMap.get(roomNo);
		if (null == room || 0L >= room.getId())
			return;
		JSONObject obj = new JSONObject();
		obj.put("id", room.getId());
		obj.put("player_number", room.getPlayerMap().size());
		obj.put("createtime", TimeUtil.getNowDate());
		obj.put("game_index", room.getGameNewIndex());
		int status = room.getGameStatus();
		if (0 < status)
			status = 1;
		obj.put("status", status);
		for (String account : room.getPlayerMap().keySet()) {
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (null == playerinfo || playerinfo.getMyIndex() > 9)
				continue;
			obj.put("user_id" + playerinfo.getMyIndex(), playerinfo.getId());
			obj.put("user_icon" + playerinfo.getMyIndex(), playerinfo.getHeadimg());
			obj.put("user_name" + playerinfo.getMyIndex(), playerinfo.getName());
		}
		for (int i = 0; i < 10; i++) {
			if (!obj.containsKey("user_id" + i)) {
				obj.put("user_id" + i, 0);
				obj.put("user_icon" + i, "");
				obj.put("user_name" + i, "");
			}
		}
		// 同步更新房间信息
		producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ZA_GAMEROOMS, obj));
	}

	public void refreshLocation(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		String location = postData.getString("location");
		// 验证数据是否正确
		if (StringUtils.isBlank(account) || StringUtils.isBlank(roomNo) || StringUtils.isBlank(location)) {
			return;
		}
		// 判断房间是否存在
		if (!RoomManage.gameRoomMap.containsKey(roomNo) || RoomManage.gameRoomMap.get(roomNo) == null) {
			return;
		}
		// 判断用户是否存在
		GameRoom room = RoomManage.gameRoomMap.get(roomNo);
		if (!room.getPlayerMap().containsKey(account) || room.getPlayerMap().get(account) == null) {
			return;
		}
		// 更新玩家经纬度信息
		RoomManage.gameRoomMap.get(roomNo).getPlayerMap().get(account).setLocation(location);
	}

	public void getUserLocation(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
		String index = postData.getString("index");
		// 验证数据是否正确
		if (StringUtils.isBlank(roomNo)) {
			return;
		}
		// 判断房间是否存在
		if (!RoomManage.gameRoomMap.containsKey(roomNo) || RoomManage.gameRoomMap.get(roomNo) == null) {
			return;
		}
		JSONObject result = new JSONObject();
		result.put("index", index);
		// 判断用户是否存在
		GameRoom room = RoomManage.gameRoomMap.get(roomNo);
		if (room instanceof QZMJGameRoom) {
			result.put("users", ((QZMJGameRoom) room).getAllPlayer());
		}
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getUserLocationPush");
	}

	//检查用户签到信息
	public void getSignMessageEvent(SocketIOClient client, Object data) {
		/*
		 * 需要信息：userId 
		 * 检查此刻时间   获得用户签到时间
		 * 进行比较  看是否是今日已经签过到了
		 * 如果签过到了 返回
		 * 如果没有签过到   通知客户端弹出签到窗口进行签到
		 * 返回信息：  是否签到   已经签到天数
		 */
		JSONObject returnData = new JSONObject();
		JSONObject postData = JSONObject.fromObject(data);
		int userId = postData.getInt("uid");
		JSONObject userSign = zaNewSignBiz.getZaNewSignMsgByUserId(userId);
		JSONObject signSetting = zaNewSignBiz.getSignSetting();
		System.out.println("signSetting"+signSetting);
		List<Integer> list = new ArrayList<Integer>();
		list.add(signSetting.getInt("one"));
		list.add(signSetting.getInt("two"));
		list.add(signSetting.getInt("three"));
		list.add(signSetting.getInt("four"));
		list.add(signSetting.getInt("five"));
		list.add(signSetting.getInt("six"));
		list.add(signSetting.getInt("seven"));
		if (Dto.isNull(userSign)) {
			//如果为null 则证明之前没有签过到  需要在表中创建一条数据
			JSONObject newUserSign = new JSONObject();
			newUserSign.put("user_id",userId);
			newUserSign.put("sign_days_week",7);
			newUserSign.put("create_time","2000-01-01 00:00:00");
			newUserSign.put("all_roomcard",0);
			newUserSign.put("sign_all_days",0);
			int result = zaNewSignBiz.insertZaNewSign(newUserSign);
			if (result > 0) {
				returnData.put("code", 1);
				returnData.put("isSignIn", 0); //0为可以签到  
				returnData.put("signDays", 0);
				returnData.put("signSetting", list);
				System.out.println("1111111111"+returnData.toString());
				CommonConstant.sendMsgEventToSingle(client, String.valueOf(returnData), "getSignMessageEventPush");
			}else {
				returnData.put("code", 0);
				returnData.put("errorMsg", "首次签到获取信息异常，请联系管理员  错误码：100");
				CommonConstant.sendMsgEventToSingle(client, String.valueOf(returnData), "getSignMessageEventPush");
			}
		}else {
			//如果不为空 则之前签过到  需要获取签到时间 以及天数  先判断今天是否签到
			String signTime = userSign.getString("create_time");
			returnData.put("code", 1);
			returnData.put("signDays", userSign.get("sign_days_week"));
			returnData.put("signSetting", list);
			if (!isOneDay(signTime)) {
				//不是一天  没有签过
				returnData.put("isSignIn", 0);
			}else {
				returnData.put("isSignIn", 1);
				returnData.put("msg", "今日已经签过到了~");
			}
			System.out.println("22222"+returnData.toString());
			CommonConstant.sendMsgEventToSingle(client, String.valueOf(returnData), "getSignMessageEventPush");
		}
		
	}
	
	public boolean isOneDay(String time) {
		Calendar c1 = Calendar.getInstance();   
        Calendar c2 = Calendar.getInstance();   
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");//注意月份是MM
        try {
			Date date = simpleDateFormat.parse(time);
			c1.setTime(date);
			Date date2 = new Date();
			c2.setTime(date2);
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return (c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR))   
                && (c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH))   
                && (c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH));   
	}

	//用户执行签到情况
	public void UserSignEvent(SocketIOClient client, Object data) {
		//用户点击一下 接收用户id 
		//接收是第几天签到 以及钻石数目
		//天数加1 更改时间
		//签到总钻石增加 总天数+1 如果本周天数不为7 也+1 为7则改为1
		JSONObject postData = JSONObject.fromObject(data);
		int userId = postData.getInt("uid");
		int days = postData.getInt("days");
		int count = postData.getInt("count");
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String time = simpleDateFormat.format(new Date());
		JSONObject oldSign = zaNewSignBiz.getZaNewSignMsgByUserId(userId);
		oldSign.put("sign_days_week", days);
		oldSign.put("create_time", time);
		oldSign.put("all_roomcard", oldSign.getInt("all_roomcard")+count);
		oldSign.put("sign_all_days", oldSign.getInt("sign_all_days")+1);
		int result = zaNewSignBiz.insertZaNewSign(oldSign);
		if(result>0) {
			result = zaNewSignBiz.updateUserRoomcardBySign(count,userId);
		}
		JSONObject resultData = new JSONObject();
		int code = result>0?1:0;
		String errorMsg = code==1?"":"签到失败！请联系管理员补签！错误码：101";
		resultData.put("code", code);
		resultData.put("errorMsg", errorMsg);
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(resultData), "UserSignEventPush");
	}
	
	public void updateGameBgSet(SocketIOClient client, Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		System.out.println(postData);
		int gameId = postData.getInt("gameId");
		int bgId = postData.getInt("bg");
		int userId = postData.getInt("uid");
		int result = userBiz.updateGameSetByGameId(userId, gameId, bgId);
		System.out.println("更换背景结果");
	}
	
	public void updateUseNameOrHead(SocketIOClient client,Object data) {
		JSONObject postData = JSONObject.fromObject(data);
		String name = postData.getString("name");
		long headId = postData.getInt("headId");
		long userId=postData.getLong("uid");
		int result = 0;
		if (Dto.isNull(name) || "".equals(name)) {
			//更换头像
			JSONObject headAdd = userBiz.findHeadimgById(headId);
			String headimg = headAdd.getString("address");
			result = userBiz.updateUserHeadimgByUserId(userId, headimg);
			System.out.println("更换头像："+result);
		}else {
			result = userBiz.updateUserNameByUserId(userId, name);
			System.out.println("更换名字："+result);
		}
		JSONObject resultData = new JSONObject();
		resultData.put("code", result);
		CommonConstant.sendMsgEventToSingle(client, String.valueOf(resultData), "updateUserMsgEventPush");
	}

}
