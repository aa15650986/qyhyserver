package com.zhuoan.biz.event.circle;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.event.BaseEventDeal;
import com.zhuoan.biz.event.nn.NNGameEventDealNew;
import com.zhuoan.biz.event.pdk.PDKGameEventDeal;
import com.zhuoan.biz.event.qzmj.QZMJGameEventDeal;
import com.zhuoan.biz.event.sss.SSSGameEventDealNew;
import com.zhuoan.biz.event.zjh.ZJHGameEventDealNew;
import com.zhuoan.biz.game.biz.CircleBiz;
import com.zhuoan.biz.game.biz.GameCircleService;
import com.zhuoan.biz.game.biz.GameLogBiz;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.game.biz.ZaNewSignBiz;
import com.zhuoan.biz.game.dao.util.BaseSqlUtil;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.sss.SSSGameRoomNew;
import com.zhuoan.constant.*;
import com.zhuoan.constant.event.CircleBaseEventConstant;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.service.CircleService;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.util.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.*;

@Component
public class CircleBaseEventDeal {

	private final static Logger log = LoggerFactory.getLogger(CircleBaseEventDeal.class);
	@Resource
	private CircleBiz circleBiz;
	@Resource
	private GameLogBiz gameLogBiz;
	@Resource
	private RedisInfoService redisInfoService;
	@Resource
	private BaseEventDeal baseEventDeal;
	@Resource
	private Destination daoQueueDestination;
	@Resource
	private ProducerService producerService;
	@Resource
	private GameCircleService gameCircleService;
	@Resource
	private SSSGameEventDealNew sssGameEventDealNew;
	@Resource
	private QZMJGameEventDeal qzmjGameEventDeal;
	@Resource
	private PDKGameEventDeal pdkGameEventDeal;
	@Resource
	private UserBiz userBiz;
	@Resource
	private CircleService circleService;
	@Resource
	private ZaNewSignBiz zaNewSignBiz;

	@Resource
	private NNGameEventDealNew nnGameEventDealNew;
	@Resource
	private ZJHGameEventDealNew zjhGameEventDealNew;

	private static ClientObservable circleListObservable = new ClientObservable("circleList");

	public void getCircleBaseTest(SocketIOClient client, JSONObject object) {
	}

	public void createModifyCircleEvent(SocketIOClient client, JSONObject object) {
		if (!JsonUtil.isNullVal(object, new String[] { CommonConstant.USERS_ID, "type", "platform" })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,type,platform]",
					CircleBaseEventConstant.CREATE_MODIFY_CIRCLE_EVENT_PUSH);
			return;
		}
		String uid = object.getString(CommonConstant.USERS_ID);
		if (object.containsKey("circleName") && object.getString("circleName").length() > 8) {
			object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO)
					.element(CommonConstant.RESULT_KEY_MSG, "俱乐部的名字不能超过八个字！！！");
		} else {
			String type = object.getString("type");
			// 创建俱乐部
			if ("1".equals(type)) {
				if (object.get("circleName") == null) {
					return;
				}
				// 防重验证
				long createCircle = redisInfoService.createCircle(uid);
				if (createCircle > 1L) {
					CommonConstant.sendMsgEventNo(client, "牌友圈创建中,请稍后再试", "重复提交",
							CircleBaseEventConstant.CREATE_MODIFY_CIRCLE_EVENT_PUSH);
					return;
				}
				object = circleBiz.addGameCircleInfo(object);
				redisInfoService.delCreateCircle(uid);
			} else if ("2".equals(type)) {
				if (object.get("circleId") == null) {
					return;
				}
				// 修改俱乐部
				object = circleBiz.updateGameCircleInfo(object);
				redisInfoService.delGameCircleInfoByid(object.getString("circleId"));
			}
		}
		CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CREATE_MODIFY_CIRCLE_EVENT_PUSH);
	}

	public void dismissCircleEvent(SocketIOClient client, JSONObject object) {
		if (!JsonUtil.isNullVal(object, new String[] { CommonConstant.USERS_ID, "circleId", "platform" })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,circleId]",
					CircleBaseEventConstant.DISMISS_CIRCLE_EVENT_PUSH);
			return;
		}
		object = circleBiz.dismissCircle(object);
		redisInfoService.delGameCircleInfoByid(object.getString("circleId"));
		CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.DISMISS_CIRCLE_EVENT_PUSH);
	}

	public void mgrSettingCircleEvent(SocketIOClient client, JSONObject object) {
		if (!JsonUtil.isNullVal(object,
				new String[] { CommonConstant.USERS_ID, "circleId", "platform", "adminId", "type" })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,circleId,platform,adminId,type]",
					CircleBaseEventConstant.MGR_SETTING_CIRCLE_EVENT_PUSH);
			return;
		}
		String type = object.getString("type");
		if ("1".equals(type)) {// 管理员设置
			if (object.get("isAdmin") == null) {
				CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[isAdmin]",
						CircleBaseEventConstant.MGR_SETTING_CIRCLE_EVENT_PUSH);
			}
			object = circleBiz.mgrSettingCircle(object);
		} else if ("2".equals(type)) {// 成员合伙比例调整
			if (object.get("profitRatio") == null) {
				CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[profitRatio]",
						CircleBaseEventConstant.MGR_SETTING_CIRCLE_EVENT_PUSH);
			}
			object = circleBiz.updateProfitRatio(object);
		}
		CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.MGR_SETTING_CIRCLE_EVENT_PUSH);
	}

	public void transferCircleEvent(SocketIOClient client, JSONObject object) {
		if (!JsonUtil.isNullVal(object,
				new String[] { CommonConstant.USERS_ID, "circleId", "platform", "newCreateAccount" })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,circleId,platform,newCreateAccount]",
					CircleBaseEventConstant.TRANSFER_CIRCLE_EVENT_PUSH);
			return;
		}
		object = circleBiz.transferCircle(object);
		redisInfoService.delGameCircleInfoByid(object.getString("circleId"));
		CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.TRANSFER_CIRCLE_EVENT_PUSH);
	}

	public void circleMbrAddExitExamEvent(SocketIOClient client, JSONObject object) {
		// 参数校验
		if (!JsonUtil.isNullVal(object, new String[] { CommonConstant.USERS_ID, "circle_id", "type", "platform",
				"is_agree", "recordId", "msgId", "user_id" })) {
			CommonConstant.sendMsgEventNo(client, "系统错误",
					"缺少参数[user_id、circle_id、type、platform、is_agree、recordId、msgId]",
					CircleBaseEventConstant.CIRCLE_MBR_ADD_EXIT_EXAM_EVENT_PUSH);
			return;
		}

		String type = object.getString("type");
		String isAgree = object.getString("is_agree");
		String userId = object.getString("user_id");
		String rstMsg = "";
		int code = CommonConstant.GLOBAL_YES;
		String msg = "";
		if ("join".equals(type)) {
			long n = circleBiz.userJoinExamCircle(object);
			if (n <= 0) {
				rstMsg = "申请加入俱乐部审核失败";
				code = CommonConstant.GLOBAL_NO;
			} else {
				if ("Y".equals(isAgree)) {
					msg = "恭喜您，成功申请加入俱乐部";
				} else {
					msg = "很遗憾通知您已被拒绝加入俱乐部";
				}
				rstMsg = "审核完成";
			}
		} else if ("exit".equals(type)) {
			long n = circleBiz.userExitExamCircle(object);
			if (n == -1) {
				rstMsg = "申请退出俱乐部审核失败";
				code = CommonConstant.GLOBAL_NO;
			} else if (n == -2) {
				rstMsg = "当前玩家佣金/能量值不为0，不允许审核通过";
				code = CommonConstant.GLOBAL_NO;
			} else if (n == -3) {
				rstMsg = "当前玩家推荐过玩家，不允许审核通过";
				code = CommonConstant.GLOBAL_NO;
			} else {
				if ("Y".equals(isAgree)) {
					msg = "恭喜您，成功申请退出俱乐部";
				} else {
					msg = "很遗憾通知您已被拒绝退出俱乐部";
				}
				rstMsg = "审核完成";
			}
		} else if ("extract".equals(type)) {
			// 提取佣金审核
			long n = circleBiz.userExtractBalance(object);
			if (n <= 0) {
				rstMsg = "提取佣金审核失败";
				code = CommonConstant.GLOBAL_NO;
			} else {
				if ("Y".equals(isAgree)) {
					msg = "通过佣金提取";
				} else {
					msg = "拒绝佣金提取";
				}
				rstMsg = "审核完成";
			}
		}

		object.element(CommonConstant.RESULT_KEY_MSG, rstMsg);
		object.element(CommonConstant.RESULT_KEY_CODE, code);

		CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CIRCLE_MBR_ADD_EXIT_EXAM_EVENT_PUSH);

		// 成员审核加入、退出发送消息提醒
		SocketIOClient superClient = redisInfoService.getSocketIOClientByUid(userId);
		if (null != superClient) {
			JSONObject rstObj = new JSONObject();
			rstObj.put("type", "2");
			rstObj.put("data", msg);
			CommonConstant.sendMsgEvent(superClient, rstObj, CircleBaseEventConstant.CIRCLE_MSG_REMIND_EVENT_PUSH);
		}

		circleListObservable.notifyObservers(userId);
	}

	public void circleBlacklistEvent(SocketIOClient client, JSONObject object) {
		String eventName = CircleBaseEventConstant.CIRCLE_BLACKLIST_EVENT_PUSH;
		if (!JsonUtil.isNullVal(object, new String[] { CommonConstant.USERS_ID, "circle_id", "user_id", "type" })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,circle_id,user_id,type]", eventName);
			return;
		}
		String usersId = object.getString(CommonConstant.USERS_ID);// 操作者
		String circleId = object.getString("circle_id");// 俱乐部id

		String modifyUserId = object.getString("user_id");// 被操作者id
		String type = object.getString("type");

		if (!"1".equals(type) && !"2".equals(type)) {
			CommonConstant.sendMsgEventNo(client, "操作类型错误", "1 启用2 禁用 ", eventName);
			return;
		}

		type = "1".equals(type) ? CircleConstant.MEMBER_NOT_USE : CircleConstant.MEMBER_IS_USE;

		JSONObject circleMember = DBUtil.getObjectBySQL(
				"SELECT * FROM game_circle_member g WHERE g.`circle_id`=? AND g.`user_id`=? AND g.`is_delete`=?",
				new Object[] { circleId, usersId, CircleConstant.IS_DELETE_N });
		// 操作者不存在
		if (null == circleMember || !circleMember.containsKey("user_role") || !circleMember.containsKey("is_admin")) {
			CommonConstant.sendMsgEventNo(client, "请刷新后再试", null, eventName);
			return;
		}

		// 用户权限 //管理员权限
		String userRole = circleMember.getString("user_role");
		String adminRole = circleMember.getString("is_admin");
		// 判断是否有管理员,合伙人或操作者权限
		if (!CircleConstant.MEMBER_ROLE_QZ.equals(userRole) && !CircleConstant.MEMBER_IS_MGR.equals(adminRole)) {
			CommonConstant.sendMsgEventNo(client, "权限不够", null, eventName);
			return;
		}
		JSONObject modifyCircleMember = DBUtil.getObjectBySQL(
				"SELECT * FROM game_circle_member g WHERE g.`circle_id`=? AND g.`user_id`=? AND g.`is_delete`=?",
				new Object[] { circleId, modifyUserId, CircleConstant.IS_DELETE_N });
		if (null == modifyCircleMember || !modifyCircleMember.containsKey("is_use")
				|| !modifyCircleMember.containsKey("id")) {
			CommonConstant.sendMsgEventNo(client, "请刷新后再试", null, eventName);
			return;
		}
		if (type.equals(modifyCircleMember.getString("is_use"))) {
			CommonConstant.sendMsgEventYes(client, "操作成功", eventName);
			return;
		}

		int i = DBUtil.executeUpdateBySQL("UPDATE game_circle_member SET `is_use`=? WHERE id = ?",
				new Object[] { type, modifyCircleMember.get("id") });
		if (i > 0) {
			CommonConstant.sendMsgEventYes(client, "操作成功", eventName);
			return;
		}
		CommonConstant.sendMsgEventNo(client, "操作失败", null, eventName);
	}

	public void circleFundSavePayEvent(SocketIOClient client, JSONObject object) {

		// 参数校验
		if (!object.containsKey("circle_id") || !object.containsKey("platform") || !object.containsKey("change_count")
				|| !object.containsKey("operator_type")) {
			object.element(CommonConstant.RESULT_KEY_MSG, "缺少参数[circle_id、platform、change_count、operator_type]");
			object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CIRCLE_FUND_SAVE_PAY_EVENT_PUSH);
			return;
		}

		String circleId = object.getString("circle_id");
		String platform = object.getString("platform");
		String chgCount = object.getString("change_count");
		String type = object.getString("operator_type");
		if (Dto.stringIsNULL(circleId) || Dto.stringIsNULL(platform) || Dto.stringIsNULL(chgCount)
				|| Dto.stringIsNULL(type)) {
			object.element(CommonConstant.RESULT_KEY_MSG, "参数值存在空");
			object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CIRCLE_FUND_SAVE_PAY_EVENT_PUSH);
			return;
		}

		if ("0".equals(chgCount)) {
			object.element(CommonConstant.RESULT_KEY_MSG, "基金不允许操作0");
			object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CIRCLE_FUND_SAVE_PAY_EVENT_PUSH);
		}

		String rstMsg = "";
		int code = CommonConstant.GLOBAL_YES;

		int n = circleBiz.userFundSavePay(object);
		if (n == -1) {
			rstMsg = "基金操作失败";
			code = CommonConstant.GLOBAL_NO;
		} else if (n == -2) {
			rstMsg = "房卡不足";
			code = CommonConstant.GLOBAL_NO;
		} else {
			rstMsg = "恭喜您，基金操作成功";
		}

		object.element(CommonConstant.RESULT_KEY_MSG, rstMsg);
		object.element(CommonConstant.RESULT_KEY_CODE, code);

		CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CIRCLE_FUND_SAVE_PAY_EVENT_PUSH);
	}

	public void circleFundBillDetailEvent(SocketIOClient client, JSONObject object) {
		String eventName = CircleBaseEventConstant.CIRCLE_FUND_BILL_DETAIL_EVENT_PUSH;
		if (!JsonUtil.isNullVal(object, new String[] { CommonConstant.USERS_ID, "circleId", "type" })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,circleId,type 1 游戏消耗  2 转入转出]", eventName);
			return;
		}

		String circleId = object.getString("circleId");
		String time = DateUtils.searchTime(object.get("time"));// 日期搜索
		int pageIndex = !object.containsKey("pageIndex") ? 1 : object.getInt("pageIndex");// 第几页
		int pageSize = !object.containsKey("pageSize") ? 6 : object.getInt("pageSize");// 一页几条

		int type = object.getInt("type");
		JSONObject jsonObject = circleBiz.queryFundBillDetail(circleId, time, type, pageIndex, pageSize);
		JSONArray dataList = JSONArray.fromObject(jsonObject.get("list"));

		TimeUtil.transTimestamp(dataList, new String[] { "gmt_create" }, "yyyy-MM-dd HH:mm:ss");
		jsonObject.put("list", dataList);
		StringBuilder sb = new StringBuilder();
		sb.append(
				"SELECT SUM(`fund_change_count`)AS fundCountAll FROM game_circle_fund_bill g WHERE g.`circle_id`= ? ");
		List list = new ArrayList();
		list.add(circleId);
		if (null != time) {
			sb.append(" AND DATEDIFF(g.`gmt_create`,?)=0");
			list.add(time);
		}
		if (1 == type) {// 游戏
			sb.append(" AND g.`operator_type` = 3 ");
		} else if (2 == type) {// 操作
			sb.append(" AND g.`operator_type` in (1,2) ");
		}

		JSONObject fundCountAll = DBUtil.getObjectBySQL(sb.toString(), list.toArray());
		object.element("msg", "成功获取账单信息").element("code", CommonConstant.GLOBAL_YES).element("time", time)
				.element("data", jsonObject)
				.element("fundCountAll",
						fundCountAll.containsKey("fundCountAll") ? fundCountAll.get("fundCountAll") : 0)
				.element("pageIndex", jsonObject.containsKey("pageIndex") ? jsonObject.getInt("pageIndex") : 1)
				.element("pageSize", jsonObject.containsKey("pageSize") ? jsonObject.getInt("pageSize") : 6)
				.element("totalPage", jsonObject.containsKey("totalPage") ? jsonObject.getInt("totalPage") : 0)
				.element("totalCount", jsonObject.containsKey("totalCount") ? jsonObject.getInt("totalCount") : 0);
		log.info("circleFundBillDetailEvent [{}]", object);
		CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CIRCLE_FUND_BILL_DETAIL_EVENT_PUSH);
	}

	public void circleMessageInfoEvent(SocketIOClient client, JSONObject object) {
		if (object.get(CommonConstant.USERS_ID) == null)
			return;

		JSONArray jsonArray = circleBiz.queryMessageList(object);

		object.element(CommonConstant.RESULT_KEY_MSG, "success");
		object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		object.element("data", jsonArray);
		CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CIRCLE_MESSAGE_INFO_EVENT_PUSH);
	}

	public void circleMessageBlanceReminderEvent(SocketIOClient client, JSONObject object) {
		if (object.get(CommonConstant.USERS_ID) == null)
			return;
		String account = object.getString(CommonConstant.USERS_ID);
		CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CIRCLE_MESSAGE_BLANCE_REMINDER_EVENT_PUSH);
	}

	boolean checkPower(String userRole, String isAdmin) {
		// 定义1是圈主2是合伙人3是玩家
		if (userRole.equals(CircleConstant.MEMBER_ROLE_QZ) || userRole.equals(CircleConstant.MEMBER_ROLE_HHR)
				|| isAdmin.equals(CircleConstant.MEMBER_IS_MGR)) {
			return true;
		}
		return false;
	}

	private boolean isOnCircleGame(String circleId, String account) {
		// 用来判断玩家是否在房间内:false表示不在房间
		if (null == account || null == circleId)
			return true;
		boolean b = false;
		for (String roomNo : RoomManage.gameRoomMap.keySet()) {
			GameRoom room = RoomManage.gameRoomMap.get(roomNo);
			if (null == room)
				continue;
			if (CommonConstant.ROOM_TYPE_FREE != room.getRoomType()
					&& CommonConstant.ROOM_TYPE_INNING != room.getRoomType())
				continue;
			if (!circleId.equals(room.getCircleId()))
				continue;
			if (room.getPlayerMap().containsKey(account)) {
				b = true;
				break;
			}
		}
		return b;
	}

	public void circlePartnerListEvent(SocketIOClient client, JSONObject object) {
		String eventName = CircleBaseEventConstant.CIRCLE_PARTNER_LIST_EVENT_PUSH;
		if (!JsonUtil.isNullVal(object,
				new String[] { "circleId", CommonConstant.USERS_ID, CommonConstant.DATA_KEY_PLATFORM })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "参数问题：circleId,uid,platform", eventName);
			return;
		}
		String time = DateUtils.searchTime(object.get("time"));// 日期搜索

		int pageIndex = !object.containsKey("pageIndex") ? 1 : object.getInt("pageIndex");// 第几页
		int pageSize = !object.containsKey("pageSize") ? 6 : object.getInt("pageSize");// 一页几条
		Object userAccount = object.get("partnerUserAccount"); // 搜索成员account
		String orderByTotalScore = "DESC";// 总得分降序
		if (object.containsKey("orderByTotalScore")) {
			if ("ASC".equals(object.getString("orderByTotalScore"))) {
				orderByTotalScore = "ASC";
			}
		}

		// 条件搜索 0:全部搜索 1:直属搜索 2:非直属
		String condition = object.containsKey("condition") ? object.getString("condition")
				: CircleConstant.LIST_CONDITION_ALL;
		if (!CircleConstant.LIST_CONDITION_ALL.equals(condition)
				&& !CircleConstant.LIST_CONDITION_LOWER_YES.equals(condition)
				&& !CircleConstant.LIST_CONDITION_LOWER_NO.equals(condition)) {
			CommonConstant.sendMsgEventNo(client, "condition类型错误", null, eventName);
			return;
		}

		Object circleId = object.get("circleId");
		Object userId = object.get(CommonConstant.USERS_ID);
		String platform = object.getString(CommonConstant.DATA_KEY_PLATFORM);
		// 用户验证
		JSONObject circleMember = DBUtil.getObjectBySQL(
				"SELECT z.*,f.`circle_code` FROM game_circle_member z LEFT JOIN game_circle_info f ON z.`circle_id`=f.`id` WHERE z.`user_id`= ? AND z.`circle_id`= ? AND z.`is_delete`= ? ",
				new Object[] { userId, circleId, CircleConstant.IS_DELETE_N });
		if (null == circleMember || !circleMember.containsKey("user_role") || !circleMember.containsKey("is_admin")
				|| !circleMember.containsKey("circle_code") || !circleMember.containsKey("id")
				|| !circleMember.containsKey("user_code")) {
			CommonConstant.sendMsgEventNo(client, "用户不存在", null, eventName);
			return;
		}
		// 最后返回的结果集
		String userRole = circleMember.getString("user_role");// 用户角色（字典）1:圈主 2:合伙人 3:玩家
		// 获取上级用户ID
		Object superUserId = circleMember.get("id");
		// 获取上级编码
		String userCode = circleMember.getString("user_code");
		String adminRole = circleMember.getString("is_admin");

		// 判断是否是创建者或合伙人
		if (CircleConstant.MEMBER_ROLE_WJ.equals(userRole) && CircleConstant.MEMBER_NOT_MGR.equals(adminRole)) {
			CommonConstant.sendMsgEventNo(client, "权限不够", "您不是创建者或合伙人或管理员,无法查看该列表", eventName);
			return;
		}
		// 合伙人 只查看自己的下级 下下级。。。
		if (CircleConstant.MEMBER_ROLE_HHR.equals(userRole)) {
		} else {
			superUserId = null;
		}

		// 管理员 查看的直属下级是创建者的直属下级
		if (CircleConstant.MEMBER_IS_MGR.equals(adminRole)) {
			JSONObject masterMemberInfo = circleBiz.getMasterMemberInfo(Long.valueOf((String) circleId));
			userCode = masterMemberInfo == null ? userCode
					: masterMemberInfo.containsKey("user_code") ? masterMemberInfo.getString("user_code") : userCode;
		}

		JSONObject circleMemberInfoList = circleBiz.getMemberPage(2, circleId, userAccount, superUserId, condition,
				userCode, orderByTotalScore, circleMember.getString("circle_code"), time, new int[] { 8, 9 }, pageIndex,
				pageSize);
		// 俱乐部成员下级人数表
		JSONArray memberList = new JSONArray();
		if (circleMemberInfoList.containsKey("list")) {
			JSONArray newData = circleMemberInfoList.getJSONArray("list");
			System.out.println(newData);
			for (int i = 0; i < newData.size(); i++) {
				JSONObject member = newData.getJSONObject(i);
				if (null == member)
					continue;
				boolean isUnder = true;
				// 是否直属
				String user_id = member.getString("user_id");
				// 权限
				String memberUserRole = member.containsKey("user_role") ? member.getString("user_role")
						: CircleConstant.MEMBER_ROLE_WJ;
				if (userId.equals(user_id)) {
					isUnder = false;
				}
				member.element("isUnder", isUnder);

				// 获取上级编码
				String memberSuperUserCode = member.containsKey("superior_user_code")
						? member.getString("superior_user_code")
						: null;
				boolean isSuperiorRelation = userCode.equals(memberSuperUserCode);
				member.element("isSuperiorRelation", isSuperiorRelation);

				// 下级人数
				JSONObject lowerObject = DBUtil.getObjectBySQL(
						"SELECT COUNT(*)AS lowerCount FROM game_circle_member g WHERE g.`is_delete`= ? AND g.`seq_member_ids`LIKE '%$"
								+ member.get("id") + "$%'",
						new Object[] { CircleConstant.IS_DELETE_N });
				int lowerCount = 0;
				if (null != lowerObject && lowerObject.containsKey("lowerCount")) {
					lowerCount = lowerObject.getInt("lowerCount");
				}
				member.element("lowerCount", lowerCount);
				System.out.println("=============" + member.getString("name"));
				member.element("user_name", member.getString("name"));// 前端需要 暂时提供

				// 当日下级总局数
				int gameCount = 0;
				List userIds = new ArrayList();
				if (CircleConstant.MEMBER_ROLE_HHR.equals(memberUserRole)) {
					JSONArray userArrays = DBUtil.getObjectListBySQL(
							"SELECT user_id FROM game_circle_member WHERE seq_member_ids LIKE CONCAT('%$',?,'$%') and is_delete = 'N' OR id = ?",
							new Object[] { member.getInt("id"), member.getInt("id") });
					if (userArrays != null) {
						for (int z = 0; z < userArrays.size(); z++) {
							JSONObject temp = userArrays.getJSONObject(z);
							if (temp != null && temp.containsKey("user_id")) {
								userIds.add(temp.getLong("user_id"));
							}
						}
					}
				}
				if (userIds.size() != 0) {
					// 总局数
					JSONObject gameAllCount = gameLogBiz.getGameAllCount(circleMember.getString("circle_code"),
							new int[] { 8, 9 }, time, userIds, platform);
					if (gameAllCount != null && gameAllCount.containsKey("gameAllCount")) {
						gameCount = gameAllCount.getInt("gameAllCount");
					}
				}
				member.element("gameCount", gameCount);
				memberList.add(member);
			}
		}

		JSONObject result = new JSONObject();
		// 合伙人总分成
		JSONObject memberInfo = circleBiz.getCircleMemberSuper(circleId, userId);

		if (null != memberInfo && memberInfo.containsKey("profitBalanceAll")) {
			memberInfo.element("profitBalanceAll", Dto.zerolearing(memberInfo.getDouble("profitBalanceAll")));
		}
		result.element("data", memberList).element("memberInfo", memberInfo)
				.element("pageIndex",
						circleMemberInfoList.containsKey("pageIndex") ? circleMemberInfoList.getInt("pageIndex") : 1)
				.element("totalPage",
						circleMemberInfoList.containsKey("totalPage") ? circleMemberInfoList.getInt("totalPage") : 0)
				.element("pageSize",
						circleMemberInfoList.containsKey("pageSize") ? circleMemberInfoList.getInt("pageSize") : 6)
				.element("totalCount",
						circleMemberInfoList.containsKey("totalCount") ? circleMemberInfoList.getInt("totalCount") : 0);
		result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		result.element(CommonConstant.RESULT_KEY_MSG, "操作成功！");
		log.info("circlePartnerListEvent result:{}", result);

		CommonConstant.sendMsgEvent(client, result, eventName);
	}

	public void circlePartnerAddEvent(SocketIOClient client, JSONObject object) {
		if (object.get("circleId") == null || object.get("operatorUserCode") == null)
			return;
		long userId = object.getLong(CommonConstant.USERS_ID);
		long circleId = object.getLong("circleId");
		String operatorUserCode = object.getString("operatorUserCode");
		// 查找出操作者
		JSONObject member = circleBiz.checkMemberExist(userId, circleId);
		// 获取修改体力的权限
		if (member == null || !member.containsKey("user_role")) {
			return;
		}
		// 获取操作者管理员权限
		String adminRole = member.containsKey("is_admin") ? member.getString("is_admin")
				: CircleConstant.MEMBER_NOT_MGR;
		// 判断是否是管理员,如果是管理员操作人直接改为圈主
		if (CircleConstant.MEMBER_IS_MGR.equals(adminRole)) {
			member = circleBiz.getMasterMemberInfo(circleId);
		}
		if (member == null)
			return;
		JSONObject result = new JSONObject();
		boolean havePower = checkPower(member.getString("user_role"), member.getString("is_admin"));
		// 判断权限
		if (!havePower) {
			result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			result.element(CommonConstant.RESULT_KEY_MSG, "您没权限！");
			result.element(CommonConstant.RESULT_KEY_ERROR, "当前玩家权限不足");
		} else {
			// 查找出被操作者
			JSONObject operatorUser = circleBiz.selectByCircleIdAndUsercode(circleId, operatorUserCode);
			if (operatorUser == null || operatorUser.get("superior_user_code") == null) {
				return;
			}

			String superiorUserCode = operatorUser.getString("superior_user_code");
			String userCode = member.getString("user_code");

			// 被操作者的user_role等于2，就不能设置为合伙人
			if (operatorUser.getString("user_role").equals(CircleConstant.MEMBER_ROLE_HHR)) {
				result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
				result.element(CommonConstant.RESULT_KEY_MSG, "该玩家已经是合伙人！");
				result.element(CommonConstant.RESULT_KEY_ERROR, "该玩家已经是合伙人");
				// 被操作者的superiorUserCode不等于空且不等操作者的userCode,就不能设置为合伙人
			} else if (superiorUserCode != null && !superiorUserCode.equals("") && !superiorUserCode.equals(userCode)) {
				result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
				result.element(CommonConstant.RESULT_KEY_MSG, "该玩家不是你的邀请对象！");
				result.element(CommonConstant.RESULT_KEY_ERROR, "该玩家不是你的下级");
			} else {
				// 被操作者的用户信息
				JSONObject operatorUserInfo = userBiz.getUserByID(operatorUser.getLong("user_id"));
				if (null == operatorUserInfo || !operatorUserInfo.containsKey("account")) {
					CommonConstant.sendMsgEventNo(client, "用户不存在", null,
							CircleBaseEventConstant.CIRCLE_PARTNER_ADD_EVENT_PUSH);
					return;
				}
				// 判断是否在房间内
				boolean isOnGame = isOnCircleGame(object.getString("circleId"), operatorUserInfo.getString("account"));
				if (isOnGame) {
					result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
					result.element(CommonConstant.RESULT_KEY_MSG, "该玩家在游戏房间内，无法操作！");
					result.element(CommonConstant.RESULT_KEY_ERROR, "该玩家在游戏房间内，无法操作");
				} else {

					if (CircleConstant.MEMBER_IS_MGR.equals(operatorUser.getString("is_admin"))) {
						result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO)
								.element(CommonConstant.RESULT_KEY_MSG, "身份冲突,请先取消管理员");
					} else {
						operatorUser.element("superior_user_code", userCode);
						operatorUser.element("profit_ratio", 0);
						operatorUser.element("user_role", CircleConstant.MEMBER_ROLE_HHR);
						circleBiz.addPartner(operatorUser);
						result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
						result.element(CommonConstant.RESULT_KEY_MSG, "添加合伙人成功！");
						log.info("result", result);
					}
				}
			}
		}
		log.info("circlePartnerAddEvent result:{}", result);
		CommonConstant.sendMsgEvent(client, result, CircleBaseEventConstant.CIRCLE_PARTNER_ADD_EVENT_PUSH);
	}

	public void circleMemberListEvent(SocketIOClient client, JSONObject object) {
		if (object.get("circleId") == null || object.get("pageIndex") == null || object.get("pageSize") == null
				|| object.get("platform") == null || object.get("userRole") == null
				|| !object.getString("userRole").equals(CircleConstant.MEMBER_ROLE_WJ))
			return;
		Long userId = object.getLong(CommonConstant.USERS_ID);
		Long circleId = object.getLong("circleId");
		// 查找出操作者
		JSONObject user = circleBiz.checkMemberExist(userId, circleId);
		if (user == null || user.get("user_role") == null) {
			return;
		}
		// 最后返回的结果集
		JSONObject result = new JSONObject();
		boolean havePower = checkPower(user.getString("user_role"), user.getString("is_admin"));
		// 判断权限
		if (!havePower) {
			result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			result.element(CommonConstant.RESULT_KEY_MSG, "您没权限！");
			result.element(CommonConstant.RESULT_KEY_ERROR, "当前玩家权限不足");
			CommonConstant.sendMsgEvent(client, result, CircleBaseEventConstant.CIRCLE_MEMBER_LIST_EVENT_PUSH);
		} else {
			int user_role = object.getInt("userRole");
			long userAccount = 0;
			// 查单人
			if (object.containsKey("memberUserAccount")) {
				userAccount = object.getLong("memberUserAccount");
			}
			// 圈主或者管理员查看全部人
			boolean readAll = false;
			if (user.getString("user_role").equals(CircleConstant.MEMBER_ROLE_QZ)
					|| user.getString("is_admin").equals(CircleConstant.MEMBER_IS_MGR)) {
				readAll = true;
			}
			int pageIndex = object.get("pageIndex") == null ? 1 : object.getInt("pageIndex");
			int pageSize = object.get("pageSize") == null ? 6 : object.getInt("pageSize");
			JSONObject memberStatisticsList = circleBiz.getMemberStatisticsPage(readAll, circleId,
					object.getString("userCode"), user_role, userAccount, pageIndex, pageSize);
			JSONArray memberList = JSONArray.fromObject(memberStatisticsList.get("list"));
			// 查找单人
			if (userAccount != 0) {
				// 不是圈主或者管理员查看
				if (!user.getString("user_role").equals(CircleConstant.MEMBER_ROLE_QZ)
						&& !user.getString("is_admin").equals(CircleConstant.MEMBER_IS_MGR)) {
					if (memberList.size() != 0) {
						JSONObject member = JSONObject.fromObject(memberList.get(0));
						if (member == null
								|| !user.getString("user_code").equals(member.getString("superior_user_code"))) {
							result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
							result.element(CommonConstant.RESULT_KEY_MSG, "请输入正确的id！");
							result.element(CommonConstant.RESULT_KEY_ERROR, "请输入正确的id！");
							log.info("result:{}", result);
							CommonConstant.sendMsgEvent(client, result,
									CircleBaseEventConstant.CIRCLE_MEMBER_LIST_EVENT_PUSH);
							return;
						}
					}
				}
			}
			// 昨日玩的总局数
			JSONObject total = circleBiz.getYestodayPaly(readAll, circleId, user_role, object.getString("userCode"));
			// 共几页
			int totalPage = memberStatisticsList.getInt("totalPage");
			// 共几条数据
			int totalCount = memberStatisticsList.getInt("totalCount");
			if (memberList.size() == 0) {
				result.element("data", "");
				result.element("totalPage", totalPage);
				result.element("totalCount", totalCount);
				result.element("yestodayPlayTotal", 0);
				result.element("pageIndex", object.get("pageIndex"));
				result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
				result.element(CommonConstant.RESULT_KEY_MSG, "操作成功！");
			} else {
				JSONArray newData = circleBiz.getMemberArray(memberList, user, circleId);
				result.element("data", newData);
				result.element("totalPage", totalPage);
				result.element("totalCount", totalCount);
				result.element("yestodayPlayTotal", total.get("count"));
				result.element("pageIndex", object.get("pageIndex"));
				result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
				result.element(CommonConstant.RESULT_KEY_MSG, "操作成功！");
			}
			log.info("circleMemberListEvent result:{}", result);
			CommonConstant.sendMsgEvent(client, result, CircleBaseEventConstant.CIRCLE_MEMBER_LIST_EVENT_PUSH);
		}
	}

	public void circleModifyStrenthEvent(SocketIOClient client, JSONObject object) {
		String eventName = CircleBaseEventConstant.CIRCLE_MODIFY_STRENTH_EVENT_PUSH;
		if (!JsonUtil.isNullVal(object,
				new String[] { "circleId", "changeCount", CommonConstant.USERS_ID, "operatorUserCode", "platform" }))
			return;
		long userId = object.getLong(CommonConstant.USERS_ID);
		long circleId = object.getLong("circleId");
		String operatorUserCode = object.getString("operatorUserCode");
		// 被操作者
		JSONObject operatorUser = circleBiz.selectByCircleIdAndUsercode(circleId, operatorUserCode);
		// 被操作者的用户信息
		JSONObject operatorUserInfo = userBiz.getUserByID(operatorUser.getLong("user_id"));
		if (null == operatorUserInfo || !operatorUserInfo.containsKey("account")) {
			CommonConstant.sendMsgEventNo(client, "用户不存在", null, eventName);
			return;
		}

		// 获取被操作者的昵称
		String operatorName = operatorUserInfo.containsKey("name") ? operatorUserInfo.getString("name") : "";
		// 操作者
		JSONObject user = circleBiz.checkMemberExist(userId, circleId);
		if (null == user || null == operatorUser || null == user.get("user_code")
				|| null == operatorUser.get("superior_user_code") || null == user.getString("is_admin")
				|| null == operatorUser.getString("is_admin") || null == user.getString("user_role")) {
			return;
		}
		// 操作者的用户信息
		JSONObject userInfo = userBiz.getUserByID(userId);
		// 获取操作者的昵称
		String userName = userInfo.containsKey("name") ? userInfo.getString("name") : "";
		// 被操作者的直属上级
//        JSONObject superUser = circleBiz.getSuperUserDetailMessageByCode(operatorUser.getString("superior_user_code"));
//        if (null == superUser.getString("user_role")) {
//            return;
//        }
		JSONObject result = new JSONObject();
		// 成员权限
		String userRole = user.containsKey("user_role") ? user.getString("user_role") : CircleConstant.MEMBER_ROLE_WJ;
		// 判断权限
		boolean canOperate = false;
		// 判断是否是直属下级且是合伙人
		if (!user.getString("user_code").equals(operatorUser.getString("superior_user_code"))) {
			// 操作人是管理员，能操作除管理员外的所有人
			if ("1".equals(user.getString("is_admin"))
					&& CircleConstant.MEMBER_NOT_MGR.equals(operatorUser.getString("is_admin"))) {
				canOperate = true;
			}
			// 操作人是群主，能操作非合伙人下面的成员。
			if ("1".equals(userRole)) {
				canOperate = true;
			}
			// 操作人是合伙人,能越级操作合伙人下面的成员
			if (CircleConstant.MEMBER_ROLE_HHR.equals(userRole)) {
				// 获取操作者的memberID
				String memberId = user.getString("id");
				// 获取被操作者的上级ID集合
				String seqMemberIds = operatorUser.containsKey("seq_member_ids")
						? operatorUser.getString("seq_member_ids")
						: "";
				// 判断操作者是否在被操作者的上级ID中
				if (seqMemberIds.contains(memberId)) {
					canOperate = true;
				}
			}
		} else {
			canOperate = true;
		}

		if (!canOperate) {
			result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			result.element(CommonConstant.RESULT_KEY_MSG, "您没权限！");
			result.element(CommonConstant.RESULT_KEY_ERROR, "当前玩家权限不足");
			CommonConstant.sendMsgEvent(client, result, eventName);
			return;
		} else {
			// 判断是否在房间内
			boolean isOnGame = isOnCircleGame(object.getString("circleId"), operatorUserInfo.getString("account"));
			if (isOnGame) {
				result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
				result.element(CommonConstant.RESULT_KEY_MSG, "该玩家在房间内，无法操作！");
				result.element(CommonConstant.RESULT_KEY_ERROR, "该玩家在房间内，无法操作");
			} else {
				// 增减的体力值
				double changeHp = object.getDouble("changeCount");
				if (changeHp == 0D) {
					result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
					result.element(CommonConstant.RESULT_KEY_MSG, "不能填0！");
					result.element(CommonConstant.RESULT_KEY_ERROR, "操作金额为0");
				} else {
					// 被操作者的体力值
					double operatorUserHp = operatorUser.getDouble("user_hp");
					// 操作者的体力值
					double myHp = user.getDouble("user_hp");
					// 先判断是增加体力还是减少
					if (changeHp > 0D) {
						// 给被操作者增加体力值,自己减少体力值
						// 判断自己的体力值是否足够减
						if (myHp < changeHp) {
							result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
							result.element(CommonConstant.RESULT_KEY_MSG, "您的能量值不够！");
							result.element(CommonConstant.RESULT_KEY_ERROR, "操作玩家的能量值不够");
						} else {
							// 更新操作者的体力
							int i = DBUtil.executeUpdateBySQL(
									"UPDATE game_circle_member SET user_hp = user_hp - ? WHERE id = ? ",
									new Object[] { changeHp, user.getString("id") });
							if (i > 0) {
								// 更新被操作者的体力
								DBUtil.executeUpdateBySQL(
										"UPDATE game_circle_member SET user_hp = user_hp + ? WHERE id = ? ",
										new Object[] { changeHp, operatorUser.getString("id") });
								// 往道具表添加数据(赠送者和接收者都要添加一条数据)
								circleBiz.savePropBill(user.get("user_id"), circleId, -changeHp, myHp,
										CircleConstant.PROP_BILL_CHANGE_TYPE_GIVE, object.getString("platform"),
										user.get("user_id"), "赠送" + operatorName, operatorUser.get("user_id"));
								circleBiz.savePropBill(operatorUser.get("user_id"), circleId, changeHp, operatorUserHp,
										CircleConstant.PROP_BILL_CHANGE_TYPE_GIVE, object.getString("platform"),
										user.get("user_id"), userName + "赠送", operatorUser.get("user_id"));
								result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
								result.element(CommonConstant.RESULT_KEY_MSG, "操作成功！");
							} else {
								result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
								result.element(CommonConstant.RESULT_KEY_MSG, "操作失败！");
							}

						}
					} else {
						// 给被操作者减少体力值，自己增加体力值
						// 判断被操作者的体力值是否够减
						if (operatorUserHp < -changeHp) {
							result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
							result.element(CommonConstant.RESULT_KEY_MSG, "对方的能量值不够！");
							result.element(CommonConstant.RESULT_KEY_ERROR, "被操作玩家的能量值不够减");
						} else {
							// 更新操作者的体力
							int i = DBUtil.executeUpdateBySQL(
									"UPDATE game_circle_member SET user_hp = user_hp + ? WHERE id = ? ",
									new Object[] { Math.abs(changeHp), user.getString("id") });
							if (i > 0) {
								// 更新被操作者的体力
								DBUtil.executeUpdateBySQL(
										"UPDATE game_circle_member SET user_hp = user_hp - ? WHERE id = ? ",
										new Object[] { Math.abs(changeHp), operatorUser.getString("id") });
								// 保存体力日志
								circleBiz.savePropBill(user.get("user_id"), circleId, -changeHp, myHp,
										CircleConstant.PROP_BILL_CHANGE_TYPE_GIVE, object.getString("platform"),
										user.get("user_id"), "扣除" + operatorName, operatorUser.get("user_id"));
								circleBiz.savePropBill(operatorUser.get("user_id"), circleId, changeHp, operatorUserHp,
										CircleConstant.PROP_BILL_CHANGE_TYPE_GIVE, object.getString("platform"),
										user.get("user_id"), userName + "扣除", operatorUser.get("user_id"));
								result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
								result.element(CommonConstant.RESULT_KEY_MSG, "操作成功！");
							} else {
								result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
								result.element(CommonConstant.RESULT_KEY_MSG, "操作失败！");
							}

						}
					}
				}
			}
		}
		log.info("circleModifyStrenthEvent result:{}", result);
		CommonConstant.sendMsgEvent(client, result, eventName);
	}

	public void circleStrenthLogEvent(SocketIOClient client, JSONObject object) {
		String eventName = CircleBaseEventConstant.CIRCLE_STRENTH_LOG_EVENT_PUSH;
		if (!JsonUtil.isNullVal(object, new String[] { "circleId", CommonConstant.USERS_ID })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid]", eventName);
			return;
		}

		Object userId = object.get(CommonConstant.USERS_ID);// 用户id
		Object circleId = object.get("circleId");// 俱乐部id
		String time = null;// 日期搜索
//        String time = DateUtils.searchTime(object.get("time"));//日期搜索
		int pageIndex = !object.containsKey("pageIndex") ? 1 : object.getInt("pageIndex");// 第几页
		int pageSize = !object.containsKey("pageSize") ? 6 : object.getInt("pageSize");// 一页几条
		// 体力日志类别搜索
		String condition = object.containsKey("hpLogType") ? object.getString("hpLogType") : null;
		// 日志类别
		Object[] changeType = new Object[] { CircleConstant.PROP_BILL_CHANGE_TYPE_GIVE,
				CircleConstant.PROP_BILL_CHANGE_TYPE_GAME, CircleConstant.PROP_BILL_CHANGE_TYPE_WITHDRAW,
				CircleConstant.PROP_BILL_CHANGE_TYPE_FEE, CircleConstant.PROP_BILL_CHANGE_TYPE_SAVE,
				CircleConstant.PROP_BILL_CHANGE_TYPE_WITHDRAWBANK, CircleConstant.PROP_BILL_CHANGE_TYPE_LUCKY_ADD,
				CircleConstant.PROP_BILL_CHANGE_TYPE_LUCKY_SUB };
		if (condition != null) {
			if (CircleConstant.HP_LOG_TYPE_GAME.equals(condition)) {
				changeType = new Object[] { CircleConstant.PROP_BILL_CHANGE_TYPE_GAME };
			}
			if (CircleConstant.HP_LOG_TYPE_OTHER.equals(condition)) {
				changeType = new Object[] { CircleConstant.PROP_BILL_CHANGE_TYPE_GIVE,
						CircleConstant.PROP_BILL_CHANGE_TYPE_WITHDRAW, CircleConstant.PROP_BILL_CHANGE_TYPE_FEE,
						CircleConstant.PROP_BILL_CHANGE_TYPE_SAVE, CircleConstant.PROP_BILL_CHANGE_TYPE_WITHDRAWBANK,
						CircleConstant.PROP_BILL_CHANGE_TYPE_LUCKY_ADD,
						CircleConstant.PROP_BILL_CHANGE_TYPE_LUCKY_SUB };
			}
		}
		JSONObject memberPropBillPage = circleBiz.getMemberPropBillPage(circleId, userId, changeType, time, pageIndex,
				pageSize);
		JSONArray data = new JSONArray();
		if (memberPropBillPage.containsKey("list")) {
			data = memberPropBillPage.getJSONArray("list");
			for (int i = 0; i < data.size(); i++) {
				JSONObject o = data.getJSONObject(i);
				if (null == o)
					continue;
				TimeUtil.transTimeStamp(o, "yyyy-MM-dd HH:mm:ss", "create_time");
			}
		}
		changeType = new Object[] { CircleConstant.PROP_BILL_CHANGE_TYPE_FEE,
				CircleConstant.PROP_BILL_CHANGE_TYPE_GAME };

		JSONArray memberPropBillAll = circleBiz.getMemberPropBillAll(circleId, userId, changeType);// 今日和昨日体力总和
		String todayHp = "0";// 今日
		String yestodayHp = "0";// 昨日
		if (null != memberPropBillAll && memberPropBillAll.size() > 0) {
			String today = DateUtils.getToday();
			String yesterday = DateUtils.getYesterday();
			for (int i = 0; i < memberPropBillAll.size(); i++) {
				JSONObject jsonObject = memberPropBillAll.getJSONObject(i);
				if (null == jsonObject || !jsonObject.containsKey("changeCount")
						|| !jsonObject.containsKey("createTime"))
					continue;
				if (today.equals(jsonObject.getString("createTime"))) {
					todayHp = Dto.zerolearing(jsonObject.getDouble("changeCount"));
				} else if (yesterday.equals(jsonObject.getString("createTime"))) {
					yestodayHp = Dto.zerolearing(jsonObject.getDouble("changeCount"));
				}
			}
		}

		JSONObject result = new JSONObject();
		result.element("yestodayHp", yestodayHp).element("todayHp", todayHp).element("data", data).element("time", time)
				.element("pageIndex",
						memberPropBillPage.containsKey("pageIndex") ? memberPropBillPage.getInt("pageIndex") : 1)
				.element("pageSize",
						memberPropBillPage.containsKey("pageSize") ? memberPropBillPage.getInt("pageSize") : 6)
				.element("totalPage",
						memberPropBillPage.containsKey("totalPage") ? memberPropBillPage.getInt("totalPage") : 0)
				.element("totalCount",
						memberPropBillPage.containsKey("totalCount") ? memberPropBillPage.getInt("totalCount") : 0)
				.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES)
				.element(CommonConstant.RESULT_KEY_MSG, "操作成功！");
		log.info("circleStrenthLogEvent result:{}", result);
		CommonConstant.sendMsgEvent(client, result, eventName);
	}

	public void circlePartnerShareEvent(SocketIOClient client, JSONObject object) {
		if (object.get(CommonConstant.USERS_ID) == null)
			return;
		Long userId = object.getLong(CommonConstant.USERS_ID);
		Long circleId = object.getLong("circleId");

		CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CIRCLE_PARTNER_SHARE_EVENT_PUSH);
	}

	public void circleGeneralJoinExitEvent(SocketIOClient client, JSONObject object) {

		// 参数校验
		if (!object.containsKey("superior_user_code") || !object.containsKey("type")
				|| !object.containsKey("platform")) {
			object.element(CommonConstant.RESULT_KEY_MSG, "缺少参数[superior_user_code、type、platform]");
			object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CIRCLE_GENERAL_JOIN_EXIT_EVENT_PUSH);
			return;
		}

		String superiorUserCode = object.getString("superior_user_code");
		String type = object.getString("type");
		String platform = object.getString("platform");

		if (Dto.stringIsNULL(superiorUserCode) || Dto.stringIsNULL(type) || Dto.stringIsNULL(platform)) {
			object.element(CommonConstant.RESULT_KEY_MSG, "参数值存在空");
			object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CIRCLE_GENERAL_JOIN_EXIT_EVENT_PUSH);
			return;
		}

		String rstMsg = "", msg = "";
		int code = CommonConstant.GLOBAL_YES;

		if ("join".equals(type) && object.containsKey("userCode")) {
			int n = circleBiz.userJoinCircle(object);
			msg = "有新成员申请加入俱乐部";
			if (n == -1) {
				rstMsg = "申请加入俱乐部失败";
				code = CommonConstant.GLOBAL_NO;
			} else if (n == -2) {
				rstMsg = "您已提交申请，请耐心等待管理员的审核";
				code = CommonConstant.GLOBAL_NO;
			} else {
				rstMsg = "恭喜您，成功申请加入俱乐部，在审核中";
			}
		} else if ("exit".equals(type) && object.containsKey("circle_id")) {
			// 参数校验
			if (!object.containsKey("circle_id") || !object.containsKey(CommonConstant.USERS_ID)) {
				object.element(CommonConstant.RESULT_KEY_MSG, "缺少参数[circle_id,uid]");
				object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
				CommonConstant.sendMsgEvent(client, object,
						CircleBaseEventConstant.CIRCLE_GENERAL_JOIN_EXIT_EVENT_PUSH);
				return;
			}
			String circleId = object.getString("circle_id");
			String userId = object.getString(CommonConstant.USERS_ID);

			JSONObject exitKey = redisInfoService.getSysGlobalByKey(SysGlobalConstant.GAME_CIRCLE_MEMBER_EXIT,
					platform);
			boolean isExit = false;// 不需要审核
			if (null != exitKey && exitKey.containsKey("global_value")) {
				if ("1".equals(exitKey.getString("global_value"))) {
					isExit = true;// 需要
				}
			}
			long n;
			if (isExit) {
				n = circleBiz.userExitCircle(object);
			} else {
				n = circleBiz.exitCircle(userId, circleId, platform);
			}

			msg = "有新成员申请退出俱乐部";
			if (n == -1) {
				if (isExit) {
					rstMsg = "您已提交申请退出俱乐部，请耐心等待管理员的审核";
				} else {
					rstMsg = "您已退出俱乐部";
				}

				code = CommonConstant.GLOBAL_NO;
			} else if (n == -2) {
				rstMsg = "请清空消耗/能量值后再申请退出";
				code = CommonConstant.GLOBAL_NO;
			} else if (n == -3) {
				rstMsg = "您为俱乐部推荐过玩家，不允许申请退出";
				code = CommonConstant.GLOBAL_NO;
			} else {
				if (isExit) {
					rstMsg = "恭喜您，成功申请退出俱乐部，在审核中";
				} else {
					rstMsg = "正在退出俱乐部";
				}
			}
		} else {
			object.element(CommonConstant.RESULT_KEY_MSG, "缺少参数或参数值为空");
			object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CIRCLE_GENERAL_JOIN_EXIT_EVENT_PUSH);
		}

		object.element(CommonConstant.RESULT_KEY_MSG, rstMsg);
		object.element(CommonConstant.RESULT_KEY_CODE, code);
		CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CIRCLE_GENERAL_JOIN_EXIT_EVENT_PUSH);

		JSONObject memObj = null;
		if ("join".equals(type)) {
			memObj = circleBiz.getSuperUserByCode(object.getString("userCode"));
		} else if ("exit".equals(type)) {
			memObj = circleBiz.selectByCircleIdAndUsercode(object.getLong("circle_id"), "superiorUserCode");
		}

		if (null != memObj && memObj.containsKey("user_id") && CommonConstant.GLOBAL_YES == code) {
			// 成员审核加入、退出发送消息提醒
			SocketIOClient superClient = redisInfoService.getSocketIOClientByUid(memObj.getString("user_id"));
			if (null != superClient) {
				JSONObject rstObj = new JSONObject();
				rstObj.put("type", "1");
				rstObj.put("data", msg);
				CommonConstant.sendMsgEvent(superClient, rstObj, CircleBaseEventConstant.CIRCLE_MSG_REMIND_EVENT_PUSH);
			}
		}
	}

	public void circleListEvent(SocketIOClient client, JSONObject object) {
		if (!JsonUtil.isNullVal(object, new String[] { CommonConstant.USERS_ID, "platform" })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,platform]",
					CircleBaseEventConstant.CIRCLE_LIST_EVENT_PUSH);
			return;
		}
		long userId = object.getLong(CommonConstant.USERS_ID);
		// 玩家俱乐部下线
//        redisInfoService.delCircleLineInfoByUserId(Long.toString(userId));
		String platform = object.getString("platform");
		JSONArray circleList = circleBiz.queryCircleList(userId, platform);
		JSONObject resultObj = new JSONObject();
		resultObj.element("circles", circleList);
		CommonConstant.sendMsgEvent(client, resultObj, CircleBaseEventConstant.CIRCLE_LIST_EVENT_PUSH);
	}

	public void subscribeCircleListEvent(final SocketIOClient client, final JSONObject object) {
		if (client == null)
			return;

		this.circleListEvent(client, object);

		if (circleListObservable.contains(client)) {
			return;
		}

		final String userId = object.containsKey(CommonConstant.USERS_ID) ? object.getString(CommonConstant.USERS_ID)
				: null;

		final CircleBaseEventDeal deal = this;
		Observer observer = new Observer() {
			@Override
			public void update(Observable o, Object key) {
				if (key == null || String.valueOf(key).equals(userId)) {
					deal.circleListEvent(client, object);
				}

			}
		};

		circleListObservable.addObserverByClient(client, observer);
	}

	public void unSubscribeCircleListEvent(SocketIOClient client) {
		if (null == client) {
			return;
		}
		circleListObservable.deleteObserverByClient(client);
	}

	public void circleMbrExamListEvent(SocketIOClient client, JSONObject object) {

		String circleId = object.getString("circle_id");
		String platform = object.getString("platform");
		String auditType = object.getString("audit_type");

		JSONArray examList = circleBiz.queryMbrExamList(circleId, auditType, platform);

		JSONObject resultObj = new JSONObject();

		resultObj.element(CommonConstant.RESULT_KEY_MSG, "获取审核列表数据成功");
		resultObj.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		resultObj.element("data", examList);

		CommonConstant.sendMsgEvent(client, resultObj, CircleBaseEventConstant.CIRCLE_MBR_EXAM_LIST_EVENT_PUSH);
	}

	public void circleRecordEvent(SocketIOClient client, JSONObject object) {
		System.out.println("查询战绩");
		String eventName = CircleBaseEventConstant.CIRCLE_RECORD_EVENT_PUSH;

		if (!JsonUtil.isNullVal(object, new String[] { CommonConstant.USERS_ID, "gameId", "circleCode" })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,gameId,circleCode]", eventName);
			return;
		}
		String circleCode = object.getString("circleCode");
		long userId = object.getLong(CommonConstant.USERS_ID);
		String sql = "SELECT f.id,f.`user_role`,f.`is_admin`,f.`circle_id` FROM game_circle_info g LEFT JOIN game_circle_member f ON g.`id`=f.`circle_id`WHERE g.`circle_code`= ? AND f.`user_id`= ? AND g.`is_delete`='N' AND f.`is_delete`='N'";
		JSONObject gameCircleMember = DBUtil.getObjectBySQL(sql, new Object[] { circleCode, userId });

		if (null == gameCircleMember || !gameCircleMember.containsKey("user_role")
				|| !gameCircleMember.containsKey("circle_id")) {
			CommonConstant.sendMsgEventNo(client, "用户不存在", null, eventName);
			return;
		}
		// 成员ID
		String memberId = gameCircleMember.containsKey("id") ? gameCircleMember.getString("id") : "";
		String circleId = gameCircleMember.getString("circle_id");
		String userRole = gameCircleMember.getString("user_role");// 用户角色（字典）1:圈主 2:合伙人 3:玩家
		String isAdmin = gameCircleMember.getString("is_admin");// 是否管理员 1:是 0:不是
		int pageIndex = !object.containsKey("pageIndex") ? 1 : object.getInt("pageIndex");// 第几页
		int pageSize = !object.containsKey("pageSize") ? 6 : object.getInt("pageSize");// 一页几条
		boolean havePower = checkPower(userRole, isAdmin);

		long playerUserId = userId;// 需要查询的玩家
		if (havePower) {
			userId = 0L;
			if (object.containsKey("userId") && object.getInt("userId") > 0) {
				String account = object.getString("userId");
				JSONObject users = DBUtil.getObjectBySQL("SELECT id FROM za_users where account= ? ",
						new Object[] { account });
				if (null != users && users.containsKey("id")) {
					userId = users.getLong("id");
				} else {
					JSONObject o = new JSONObject();
					o.element(CommonConstant.RESULT_KEY_CODE, 2);
					o.element(CommonConstant.RESULT_KEY_MSG, "该玩家不存在,请核对id是否正确");
					o.element("account", account);
					CommonConstant.sendMsgEvent(client, o, eventName);
					return;
				}
				JSONObject member = DBUtil.getObjectBySQL(
						"SELECT id FROM game_circle_member where user_id= ?  and circle_id = ? and is_delete = ? ",
						new Object[] { userId, circleId, "N" });
				if (null == member) {
					JSONObject o = new JSONObject();
					o.element(CommonConstant.RESULT_KEY_CODE, 2);
					o.element(CommonConstant.RESULT_KEY_MSG, "该玩家还未加入此俱乐部");
					o.element("account", account);
					CommonConstant.sendMsgEvent(client, o, eventName);
					return;
				}
				playerUserId = userId;// 需要查询的玩家
			}
		}

		Integer gameId = object.containsKey("gameId") ? object.getInt("gameId") : null;
		String time = DateUtils.searchTime(object.get("time"));// 日期搜索

		// 用户集合搜索
		Object[] userIds = null;

		// 合伙人只能查看下级玩家战绩
		if (CircleConstant.MEMBER_ROLE_HHR.equals(userRole)) {
			JSONArray userArray = DBUtil.getObjectListBySQL(
					"SELECT user_id FROM game_circle_member WHERE seq_member_ids LIKE CONCAT('%$',?,'$%') AND is_delete = ? OR id = ?",
					new Object[] { memberId, CircleConstant.IS_DELETE_N, memberId });
			userIds = new Object[userArray.size()];
			for (int i = 0; i < userArray.size(); i++) {
				JSONObject temp = JSONObject.fromObject(userArray.getJSONObject(i));
				userIds[i] = temp.getString("user_id");
			}
		}

		JSONArray userGameLogs = new JSONArray();
		JSONObject logs = gameLogBiz.pageUserGameLogsByUserId(userId, gameId, circleCode, userIds, new int[] { 8, 9 },
				time, pageIndex, pageSize);
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
			System.out.println("游戏日志："+gameList);
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
				System.out.println("头像地址：" + CommonConstant.getServerDomain() + userObject.get("headimg"));
				JSONObject temp = new JSONObject().element("createTime", userObject.get("create_time"))
						.element("cutScore", userObject.get("cut_score"))
						.element("headimg", CommonConstant.getServerDomain() + userObject.get("headimg"))
						.element("cutType", userObject.get("cut_type")).element("player", userObject.get("name"))
						.element("score", userObject.get("score")).element("account", account)
						.element("gameId", userObject.get("za_games_id"));
				if (null != attachGameLog && attachGameLog.containsKey(account)) {
					temp.element("extraInfo", attachGameLog.getString(account));
				}
				userResult.add(temp);
			}
			userRes.element("playermap", userResult).element("gameSum", o.get("game_sum"))
					.element("room_no", o.get("room_no")).element("room_id", o.get("room_id"))
					.element("createTime", o.get("create_time")).element("room_type", o.get("room_type"));

			result.add(userRes);
		}

		JSONObject back = new JSONObject();
		back.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		back.element("data", result);
		back.element("gid", gameId);

		JSONObject memberInfo = circleBiz.getMemberInfo(circleId, circleCode, playerUserId, time);
		String nickName = "";// 昵称
		String userHp = "";// 体力
		String headimg = "";// 头像
		String totalNumber = "";// 总场次
		String totalScore = "";// 总得分
		String bigWinnerCount = "";// 大赢家次数
		String bigWinnerScore = "";// 大赢家得分
		System.out.println("头像地址：" + CommonConstant.getServerDomain() + memberInfo.getString("headimg"));
		if (null != memberInfo) {
			nickName = memberInfo.getString("name");// 昵称
			userHp = Dto.zerolearing(memberInfo.getDouble("user_hp"));// 体力
			headimg = CommonConstant.getServerDomain() + memberInfo.getString("headimg");// 头像
			totalNumber = memberInfo.getString("totalNumber");// 总场次
			totalScore = memberInfo.getString("totalScore");// 总得分
			bigWinnerCount = memberInfo.getString("bigWinnerCount");// 大赢家次数
			bigWinnerScore = memberInfo.getString("bigWinnerScore");// 大赢家得分
		}
		back.element("nickName", nickName).element("userHp", userHp).element("headimg", headimg)
				.element("totalNumber", totalNumber).element("totalScore", totalScore)
				.element("bigWinnerCount", bigWinnerCount).element("bigWinnerScore", bigWinnerScore)
				.element("pageIndex", logs.containsKey("pageIndex") ? logs.getInt("pageIndex") : 1)
				.element("pageSize", logs.containsKey("pageSize") ? logs.getInt("pageSize") : 6)
				.element("totalPage", logs.containsKey("totalPage") ? logs.getInt("totalPage") : 0)
				.element("totalCount", logs.containsKey("totalCount") ? logs.getInt("totalCount") : 0);
		;
		log.info("circleRecordEvent [{}]", back);
		System.out.println("返回客户端：" + back);
		CommonConstant.sendMsgEvent(client, back, eventName);
	}

	public void circleExamMsgInfoEvent(SocketIOClient client, JSONObject object) {

		// 参数校验
		if (!object.containsKey("circle_id") || !object.containsKey("platform")) {
			object.element(CommonConstant.RESULT_KEY_MSG, "缺少参数[circle_id、platform]");
			object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CIRCLE_EXAM_MSG_INFO_EVENT_PUSH);
			return;
		}

		String circleId = object.getString("circle_id");
		String platform = object.getString("platform");
		String userId = object.getString(CommonConstant.USERS_ID);

		if (Dto.stringIsNULL(circleId) || Dto.stringIsNULL(platform)) {
			object.element(CommonConstant.RESULT_KEY_MSG, "参数值存在空");
			object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CIRCLE_EXAM_MSG_INFO_EVENT_PUSH);
			return;
		}

		JSONArray examList = circleBiz.queryMsgExamList(circleId, userId, platform);

		JSONObject resultObj = new JSONObject();

		resultObj.element(CommonConstant.RESULT_KEY_MSG, "获取审核消息列表数据成功");
		resultObj.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		resultObj.element("data", examList);

		CommonConstant.sendMsgEvent(client, resultObj, CircleBaseEventConstant.CIRCLE_EXAM_MSG_INFO_EVENT_PUSH);
	}

	public void addCircleMemberEvent(SocketIOClient client, JSONObject object) {

		JSONObject resultObj = new JSONObject();
		resultObj.element(CommonConstant.RESULT_KEY_MSG, "添加用户成功");
		resultObj.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);

		// 参数校验
		if (!object.containsKey("circle_id") || !object.containsKey("platform") || !object.containsKey("account")
				|| !object.containsKey("superior_user_code")) {
			resultObj.element(CommonConstant.RESULT_KEY_MSG, "缺少参数[circle_id、platform、account、superior_user_code]");
			resultObj.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEvent(client, resultObj, CircleBaseEventConstant.ADD_CIRCLE_MEMBER_EVENT_PUSH);
			return;
		}

		String circleId = object.getString("circle_id");
		String platform = object.getString("platform");
		String account = object.getString("account");
		String superiorUserCode = object.getString("superior_user_code");

		if (Dto.stringIsNULL(circleId) || Dto.stringIsNULL(platform) || Dto.stringIsNULL(account)
				|| Dto.stringIsNULL(superiorUserCode)) {
			resultObj.element(CommonConstant.RESULT_KEY_MSG, "参数值存在空");
			resultObj.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEvent(client, resultObj, CircleBaseEventConstant.ADD_CIRCLE_MEMBER_EVENT_PUSH);
			return;
		}

		long rst = circleBiz.addCircleMember(object);

		if (rst == -1) {
			resultObj.element(CommonConstant.RESULT_KEY_MSG, "用户account[" + object.getString("account") + "]不存在");
			resultObj.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
		} else if (rst == -2) {
			resultObj.element(CommonConstant.RESULT_KEY_MSG, "用户account[" + object.getString("account") + "]已添加");
			resultObj.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
		}

		CommonConstant.sendMsgEvent(client, resultObj, CircleBaseEventConstant.ADD_CIRCLE_MEMBER_EVENT_PUSH);
	}

	public void historyHpChangeByOperateEvent(SocketIOClient client, JSONObject object) {
		if (object.get("circleId") == null || object.get("pageIndex") == null)
			return;
		JSONObject result = new JSONObject();
		// 查询人的id
		String userId = object.getString(CommonConstant.USERS_ID);
		String circleId = object.getString("circleId");
		String changeType = object.get("changeType") == null ? CircleConstant.PROP_BILL_CHANGE_TYPE_GIVE
				: object.getString("changeType");
		int page = object.get("pageIndex") == null ? 1 : object.getInt("pageIndex");
		int limitNum = object.get("pageSize") == null ? 10 : object.getInt("pageSize");
		List list = new ArrayList();
		StringBuilder sb = new StringBuilder();
		// 如果changeType不等于空的话，就是查询分润记录(4)。等于空就是默认查操作记录。只查前三天的记录
		if (object.get("changeType") != null
				&& object.getString("changeType").equals(CircleConstant.PROP_BILL_CHANGE_TYPE_FEE)) {
			sb.append(" game_circle_prop_bill a LEFT JOIN za_users b on a.fenrun_id=b.id WHERE 1=1 "
					+ "and a.user_id=? AND a.circle_id=? AND a.change_type in('4','5') and TO_DAYS(NOW()) - TO_DAYS(a.create_time) < 4 ORDER BY a.id DESC ");
			list.add(userId);
			list.add(circleId);
			// list.add(changeType);
			String attribute = "a.id,a.user_id,a.circle_id,a.change_count,a.create_time,a.memo,a.change_type,b.name as user_name,b.account";
			JSONObject jsonObject = DBUtil.getObjectLimitPageBySQL(attribute, sb.toString(), list.toArray(), page,
					limitNum, CircleConstant.MAX_PAGE_COUNT);
			JSONArray dataList = JSONArray.fromObject(jsonObject.get("list"));
			TimeUtil.transTimestamp(dataList, "create_time", "yyyy-MM-dd HH:mm:ss");
			result.element("totalPage", jsonObject.get("totalPage"));
			result.element("totalCount", jsonObject.get("totalCount"));
			JSONObject jsonObject1 = circleBiz.checkMemberExist(object.getLong(CommonConstant.USERS_ID),
					object.getLong("circleId"));
			result.element("totalBalance", Dto.zerolearing(jsonObject1.getDouble("profit_balance")));
			result.element("data", dataList);
			JSONObject yestodayHpFee = circleBiz.getYestodayHpFee(Integer.parseInt(userId), Integer.parseInt(circleId),
					CircleConstant.PROP_BILL_CHANGE_TYPE_CENTFEE);
			if (yestodayHpFee == null || yestodayHpFee.get("change_count") == null) {
				result.element("yestodayHpFee", "");
			} else {
				result.element("yestodayHpFee", Dto.zerolearing(yestodayHpFee.getDouble("change_count")));
			}

		} else {
			// 被查询人的id,只查前三天的记录
			String targetUserId = object.getString("targetUserId");
			sb.append(
					" game_circle_prop_bill WHERE 1=1 AND superior_user_id=? and user_id=? AND circle_id=? AND change_type=? and TO_DAYS(NOW()) - TO_DAYS(create_time) < 4 ORDER BY id DESC ");
			list.add(userId);
			list.add(targetUserId);
			list.add(circleId);
			list.add(changeType);
			JSONObject jsonObject = DBUtil.getObjectPageBySQL("*", sb.toString(), list.toArray(), page, limitNum);
			// 查询总数
			JSONArray jsonArray = DBUtil.getObjectListBySQL(
					"select * from  game_circle_prop_bill WHERE 1=1 AND superior_user_id=? and user_id=? AND circle_id=? AND change_type=?",
					list.toArray());
			JSONArray dataList = JSONArray.fromObject(jsonObject.get("list"));
			TimeUtil.transTimestamp(dataList, "create_time", "yyyy-MM-dd HH:mm:ss");
			TimeUtil.transTimestamp(jsonArray, "create_time", "yyyy-MM-dd HH:mm:ss");
			// 获取历史总增加和历史总减少
			// 历史增加
			double historyAdd = 0.0;
			// 历史减少
			double historyDeduct = 0.0;
			// 今日时间
			String todayTime = DateUtils.getToday();
			// 统计当日的历史总增加和总减少
			if (jsonArray.size() != 0) {
				for (int i = 0; i < jsonArray.size(); i++) {
					JSONObject detailList = JSONObject.fromObject(jsonArray.get(i));
					double changeCount = detailList.getDouble("change_count");
					String createTime = detailList.getString("create_time");
					// 如果不是今日则跳过
					if (!createTime.contains(todayTime)) {
						continue;
					}
					if (changeCount >= 0) {
						historyAdd += changeCount;
					} else {
						historyDeduct += changeCount;
					}
				}
			}
			result.element("historyAdd", Dto.zerolearing(historyAdd));
			result.element("historyDeduct", Dto.zerolearing(historyDeduct));
			result.element("data", dataList);
			result.element("totalPage", jsonObject.get("totalPage"));
			result.element("totalCount", jsonObject.get("totalCount"));
		}
		result.element("pageIndex", object.get("pageIndex"));
		result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		result.element(CommonConstant.RESULT_KEY_MSG, "操作成功！");
		log.info("historyHpChangeByOperateEvent result:{}", result);
		CommonConstant.sendMsgEvent(client, result, CircleBaseEventConstant.HISTORY_HP_CHANGE_BY_OPERATE_EVENT_PUSH);
	}

	public void getMemberDetailInfo(SocketIOClient client, JSONObject object) {

		// 参数校验
		if (!JsonUtil.isNullVal(object, new String[] { "platform", "circle_id", "user_id" })) {
			object.element(CommonConstant.RESULT_KEY_MSG, "缺少参数[platform、user_id、circle_id]");
			object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.MEMBER_DETAIL_INFO_EVENT_PUSH);
			return;
		}

		String platform = object.getString("platform");
		String userId = object.getString("user_id");
		String circleId = object.getString("circle_id");

		if (Dto.stringIsNULL(userId) || Dto.stringIsNULL(platform) || Dto.stringIsNULL(circleId)) {
			object.element(CommonConstant.RESULT_KEY_MSG, "参数值存在空");
			object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.MEMBER_DETAIL_INFO_EVENT_PUSH);
			return;
		}
		JSONObject circle = circleBiz.getCircleInfoById(object.getLong("circle_id"));
		if (Dto.isObjNull(circle)) {
			object.element(CommonConstant.RESULT_KEY_MSG, "该俱乐部已经解散!");
			object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.MEMBER_DETAIL_INFO_EVENT_PUSH);
			return;
		}
		// 查看人的信息
		JSONObject reader = circleBiz.checkMemberExist(object.getLong("uid"), object.getLong("circle_id"));
		// 获取成员信息
		JSONObject jsonObj = circleBiz.getCircleMemberByUserId(object);

		JSONObject resultObj = new JSONObject();

		String memberId = jsonObj != null ? jsonObj.containsKey("id") ? jsonObj.getString("id") : "" : "";
		// 获取下级成员总数
		JSONObject lowerObject = DBUtil.getObjectBySQL(
				"SELECT COUNT(*)AS lowerCount FROM game_circle_member g WHERE g.`is_delete`= ? AND g.`seq_member_ids`LIKE '%$"
						+ memberId + "$%'",
				new Object[] { CircleConstant.IS_DELETE_N });
		int lowerCount = 0;
		if (null != lowerObject && lowerObject.containsKey("lowerCount")) {
			lowerCount = lowerObject.getInt("lowerCount");
		}
		resultObj.put("total", lowerCount);
		JSONObject circleInfo = gameCircleService.queryGameCircleInfoById(circleId);
		if (null != jsonObj && circleInfo != null && reader != null) {
			// 玩家玩的总数（不包含当日）
			JSONObject userTotalPlay = circleBiz.getUserTotalPlay(jsonObj.getLong("user_id"),
					object.getLong("circle_id"), jsonObj.getLong("id"));
			jsonObj.element("userTotalPlay", userTotalPlay.get("count"));
			if (jsonObj.get("superior_user_code") != null) {
				String superiorUserCode = jsonObj.getString("superior_user_code");
				// 直属上级玩家的信息
				JSONObject superUser = circleBiz.getSuperUserDetailMessageByCode(superiorUserCode);
				if (null != superUser) {
					// 直属上级玩家的user_code不等于查看人的user_code
					if (!superUser.getString("user_code").equals(reader.getString("user_code"))) {
						// 如果查看人是管理员，可以操作非合伙人下的玩家，但是不能操作其他管理员
						if ("1".equals(reader.getString("is_admin")) && !"2".equals(superUser.getString("user_role"))
								&& !"1".equals(jsonObj.getString("is_admin"))) {
							// canOperate表示能能否操作该成员
							jsonObj.element("canOperate", true);
							// 如果查看人是群主,可以操作非合伙人下的玩家且可以操作所有管理员
						} else if ("1".equals(reader.getString("user_role"))
								&& !"2".equals(superUser.getString("user_role"))) {
							jsonObj.element("canOperate", true);
						} else {
							jsonObj.element("canOperate", false);
						}
					} else {
						jsonObj.element("canOperate", true);
					}
					// 圈主查看
					if (CircleConstant.MEMBER_ROLE_QZ.equals(reader.getString("user_role"))
							|| CircleConstant.MEMBER_IS_MGR.equals(reader.getString("is_admin"))) {
						jsonObj.element("superUserName", superUser.getString("name"));
						jsonObj.element("superUserId", superUser.getString("account"));
						jsonObj.element("profitRatio", superUser.getString("profit_ratio"));
						// 合伙人查看
					} else if (CircleConstant.MEMBER_ROLE_HHR.equals(reader.getString("user_role"))) {
						jsonObj.element("superUserName", superUser.getString("name"));
						jsonObj.element("superUserId", superUser.getString("account"));
					}
				}
			}
			resultObj.element(CommonConstant.RESULT_KEY_MSG, "获取成员详细信息成功");
			resultObj.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
			resultObj.element("data", jsonObj);

			resultObj.element("fundCount", circleInfo.get("fund_count"));
		} else {
			resultObj.element(CommonConstant.RESULT_KEY_MSG, "获取成员详细信息失败");
			resultObj.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
		}

		CommonConstant.sendMsgEvent(client, resultObj, CircleBaseEventConstant.MEMBER_DETAIL_INFO_EVENT_PUSH);
	}

	public void circleRoomListEvent(SocketIOClient client, JSONObject object) {
		if (!JsonUtil.isNullVal(object, "circleCode"))
			return;// 俱乐部编号
		String clubCode = object.getString("circleCode");
		JSONArray allRoom = new JSONArray();
		GameRoom room;
		JSONObject obj;
		Set scoreSet = new HashSet<>();// 底注
		Set enterScoreSet = new HashSet<>();// 入场
		Set leaveScoreSet = new HashSet<>();// 离场
		for (String roomNo : RoomManage.gameRoomMap.keySet()) {
			if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
				room = RoomManage.gameRoomMap.get(roomNo);
				if (room == null)
					continue;
				// 是否开发
				if (!room.isOpen())
					continue;
				// 俱乐部
				if (!clubCode.equals(room.getClubCode()))
					continue;
				// 俱乐部自由场/局数场
				if (room.getRoomType() != CommonConstant.ROOM_TYPE_FREE
						&& room.getRoomType() != CommonConstant.ROOM_TYPE_INNING)
					continue;
				if (JsonUtil.isNullVal(object, "roomType") && room.getRoomType() != object.getInt("roomType"))
					continue;
				// 选择游戏
				if (JsonUtil.isNullVal(object, "gid") && object.getInt("gid") != -1
						&& room.getGid() != object.getInt("gid"))
					continue;
				scoreSet.add(room.getScore());// 底注
				enterScoreSet.add(room.getEnterScore());// 入场
				leaveScoreSet.add(room.getLeaveScore());// 离场

				// 如果是十三水则选择是否有鬼牌
				if (CommonConstant.GAME_ID_SSS == room.getGid()) {
					// 获取十三水房间
					SSSGameRoomNew sssGameRoom = (SSSGameRoomNew) RoomManage.gameRoomMap.get(room.getRoomNo());
					// 选择鬼牌
					if (object.containsKey("isGhostCard")) {
						if (SSSConstant.ROOM_TYPE_GHOST_CARD_YES.equals(object.getString("isGhostCard"))
								&& sssGameRoom.getHardGhostNumber() <= 0) {
							continue;
						}
						if (SSSConstant.ROOM_TYPE_GHOST_CARD_NO.equals(object.getString("isGhostCard"))
								&& sssGameRoom.getHardGhostNumber() > 0) {
							continue;
						}
					}
				}

				// 是否人未满房间
				if (JsonUtil.isNullVal(object, "type") && object.getInt("type") == 1
						&& room.getPlayerMap().size() < room.getPlayerCount())
					continue;
				// 底注
				if (JsonUtil.isNullVal(object, "score") && room.getScore() != object.getDouble("score"))
					continue;
				// 入场
				if (JsonUtil.isNullVal(object, "enterScore") && room.getEnterScore() != object.getDouble("enterScore"))
					continue;
				// 出场
				if (JsonUtil.isNullVal(object, "leaveScore") && room.getLeaveScore() != object.getDouble("leaveScore"))
					continue;

				obj = new JSONObject();
				obj.element("roomType", room.getRoomType());// 8 自由场 9局数场
				obj.element("room_no", room.getRoomNo());// 房间号
				obj.element("gid", room.getGid());// 游戏
				obj.element("base_info", room.getRoomInfo());// 游戏信息
				obj.element("player", room.getPlayerCount());// 房间人数
				obj.element("gameIndex", room.getGameNewIndex());// 当前局数
				obj.element("gameCount", room.getGameCount());// 游戏总局数
				obj.element("visit", room.getVisit());// 是否观战模式
				// 非观战用户
				obj.element("renshu", room.getPlayerMap().size());// 玩家人数
				obj.element("userList", room.getPlayerMap());// 房间用户信息

				allRoom.add(obj);
			}
		}
		object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		object.element("array", allRoom);
		object.element("gid", object.get("gid"));
		object.element("scoreType", JSONArrayUtil.toJSONArray(scoreSet));
		object.element("enterScoreType", JSONArrayUtil.toJSONArray(enterScoreSet));
		object.element("leaveScoreType", JSONArrayUtil.toJSONArray(leaveScoreSet));
		CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CIRCLE_ROOM_LIST_EVENT_PUSH);
	}

	public void circleDelMemberEvent(SocketIOClient client, JSONObject object) {

		// 参数校验
		if (!object.containsKey("platform") || !object.containsKey("user_id") || !object.containsKey("circle_id")
				|| !object.containsKey("type")) {
			object.element(CommonConstant.RESULT_KEY_MSG, "缺少参数[platform、user_id、circle_id、type]");
			object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CIRCLE_DEL_MEMBER_EVENT_PUSH);
			return;
		}

		String platform = object.getString("platform");
		String userId = object.getString("user_id");
		String circleId = object.getString("circle_id");
		String type = object.getString("type");

		if (Dto.stringIsNULL(userId) || Dto.stringIsNULL(platform) || Dto.stringIsNULL(circleId)
				|| Dto.stringIsNULL(type)) {
			object.element(CommonConstant.RESULT_KEY_MSG, "参数值存在空");
			object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CIRCLE_DEL_MEMBER_EVENT_PUSH);
			return;
		}

		JSONObject rstObj = new JSONObject();
		rstObj.put("data", object);

		long rst = circleBiz.circleDelMember(object);

		if (rst == -1) {
			rstObj.element(CommonConstant.RESULT_KEY_MSG, "删除失败");
			rstObj.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
		} else if (rst == -2) {
			rstObj.element(CommonConstant.RESULT_KEY_MSG, "当前玩家推荐过玩家，不允许删除");
			rstObj.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
		} else if (rst == -3) {
			rstObj.element(CommonConstant.RESULT_KEY_MSG, "当前玩家存在未提取的佣金，不允许删除");
			rstObj.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
		} else if (rst == -4) {
			rstObj.element(CommonConstant.RESULT_KEY_MSG, "当前玩家能量要为零，不允许删除");
			rstObj.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
		} else if (rst == -5) {
			rstObj.element(CommonConstant.RESULT_KEY_MSG, "无操作权限");
			rstObj.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
		} else {
			rstObj.element(CommonConstant.RESULT_KEY_MSG, "删除成功");
			rstObj.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		}

		CommonConstant.sendMsgEvent(client, rstObj, CircleBaseEventConstant.CIRCLE_DEL_MEMBER_EVENT_PUSH);
	}

	public void getSysGlobalEvent(SocketIOClient client, JSONObject object) {
		// 参数校验
		if (!object.containsKey("platform") || !object.containsKey("globalKey")) {
			object.element(CommonConstant.RESULT_KEY_MSG, "缺少参数[platform、globalKey]");
			object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CIRCLE_SYS_GLOBAL_EVENT_PUSH);
			return;
		}

		String platform = object.getString("platform");
		String globalKey = object.getString("globalKey");

		if (Dto.stringIsNULL(globalKey) || Dto.stringIsNULL(platform)) {
			object.element(CommonConstant.RESULT_KEY_MSG, "参数值存在空");
			object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.CIRCLE_SYS_GLOBAL_EVENT_PUSH);
			return;
		}

		JSONArray globalList = circleBiz.getSysGlobalEvent(object);

		JSONObject resultObj = new JSONObject();

		resultObj.element(CommonConstant.RESULT_KEY_MSG, "全局参数获取成功");
		resultObj.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		resultObj.element("data", globalList);

		CommonConstant.sendMsgEvent(client, resultObj, CircleBaseEventConstant.CIRCLE_SYS_GLOBAL_EVENT_PUSH);
	}

	public void circleGetProfitBalanceToHpEvent(SocketIOClient client, JSONObject object) {
		if (object.get("circleId") == null || object.get("profitBalance") == null || object.get("platform") == null)
			return;
		Long userId = object.getLong(CommonConstant.USERS_ID);
		Long circleId = object.getLong("circleId");
		String platform = object.getString("platform");
		JSONObject result = new JSONObject();
		// 查找出操作者
		JSONObject user = circleBiz.checkMemberExist(userId, circleId);
		if (user == null) {
			return;
		}
		// 要提取的数额
		double profitBalance = object.getDouble("profitBalance");
		double myHp = user.getDouble("user_hp");
		// 判断提取的数额是否大于现有的数额
		if (profitBalance > user.getDouble("profit_balance")) {
			result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
			result.element(CommonConstant.RESULT_KEY_MSG, "请输入正确的数额！");
			result.element(CommonConstant.RESULT_KEY_ERROR, "操作玩家提取数额大于拥有的数额，失败");
		} else {
			String userRole = user.containsKey("user_role") ? user.getString("user_role")
					: CircleConstant.MEMBER_ROLE_WJ;
			// 提取佣金是否需要审核
			String isBalanceReview = circleBiz.getSysValue(platform, SysGlobalConstant.GAME_CIRCLE_BALANCE_IS_REVIEW);
			// 如果是合伙人提取佣金需要审核
			if (CircleConstant.MEMBER_ROLE_HHR.equals(userRole)
					&& CircleConstant.CIRCLE_BALANCE_REVIEW_YES.equals(isBalanceReview)) {
				JSONObject updateMember = new JSONObject();
				updateMember.put("id", user.getString("id"));
				updateMember.element("profit_balance", Dto.sub(user.getDouble("profit_balance"), profitBalance));
				// 修改佣金
				circleBiz.transferProfitBalanceToHp(updateMember);
				// 添加体力日志
				JSONObject insertHpLog = new JSONObject();
				insertHpLog.element("user_id", userId);
				insertHpLog.element("circle_id", circleId);
				insertHpLog.element("change_count", profitBalance);
				insertHpLog.element("old_count", myHp);
				insertHpLog.element("change_type", CircleConstant.PROP_BILL_CHANGE_TYPE_WITHDRAW_REVIEW);
				insertHpLog.element("platform", object.getString("platform"));
				insertHpLog.element("create_time", TimeUtil.getNowDate());
				insertHpLog.element("is_delete", 0);
				insertHpLog.element("fenrun_id", userId);
				insertHpLog.element("superior_user_id", userId);
				insertHpLog.element("memo", CircleConstant.PROP_BILL_CHANGE_EVENT_PROFIT_BALANCE_REVIEW);
				insertHpLog.element("junior_user_id", userId);
				circleBiz.saveOrUpdateUserPropBill(insertHpLog);
				// 添加消息审核信息
				if (circleBiz.addBalanceReviewMessage(circleId, userId, platform, profitBalance) >= 1) {
					result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
					result.element(CommonConstant.RESULT_KEY_MSG, "请等待管理员审核！");
				} else {
					result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
					result.element(CommonConstant.RESULT_KEY_MSG, "操作失败！");
				}
			} else {
				user.element("profit_balance", Dto.sub(user.getDouble("profit_balance"), profitBalance));
				user.element("user_hp", Dto.add(myHp, profitBalance));
				circleBiz.transferProfitBalanceToHp(user);
				JSONObject jsonObject = new JSONObject();
				jsonObject.element("user_id", userId);
				jsonObject.element("circle_id", circleId);
				jsonObject.element("change_count", profitBalance);
				jsonObject.element("old_count", myHp);
				jsonObject.element("change_type", CircleConstant.PROP_BILL_CHANGE_TYPE_WITHDRAW);
				jsonObject.element("platform", object.getString("platform"));
				jsonObject.element("create_time", TimeUtil.getNowDate());
				jsonObject.element("is_delete", 0);
				jsonObject.element("fenrun_id", userId);
				jsonObject.element("superior_user_id", userId);
				jsonObject.element("memo", CircleConstant.PROP_BILL_CHANGE_EVENT_PROFIT_BALANCE_TO_HP);
				jsonObject.element("junior_user_id", userId);
				circleBiz.saveOrUpdateUserPropBill(jsonObject);
				result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
				result.element(CommonConstant.RESULT_KEY_MSG, "操作成功！");
			}
		}
		log.info("circleGetProfitBalanceToHpEvent result:{}", result);
		CommonConstant.sendMsgEvent(client, result, CircleBaseEventConstant.CIRCLE_GET_PROFIT_BALANCE_TO_HP_EVENT_PUSH);
	}

	public void getGameCircleInfoEvent(SocketIOClient client, JSONObject object) {
		if (object.get("circleId") == null)
			return;// 亲友id
		String circleId = object.getString("circleId");
		JSONObject gameCircleInfo = redisInfoService.getGameCircleInfoByid(circleId);
		if (gameCircleInfo == null)
			object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
		else
			object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);

		object.element("data", gameCircleInfo);
		log.info("亲友圈id: [{}]信息：[{}]", circleId, gameCircleInfo);
		CommonConstant.sendMsgEvent(client, object, CircleBaseEventConstant.GET_GAME_CIRCLE_INFO_EVENT_PUSH);
	}

	public void circleRecordInfoEvent(SocketIOClient client, JSONObject object) {
		if (!JsonUtil.isNullVal(object, new String[] { "roomNo", "roomId" })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[roomNo,roomId]",
					CircleBaseEventConstant.CIRCLE_RECORD_INFO_EVENT_PUSH);
			return;
		}
		// 房间号
		String roomNo = object.getString("roomNo");
		long roomId = object.getLong("roomId");

		String sql = "SELECT `create_time` AS createTime,`id`,`room_no`,`result`,`gamelog_id`,`game_index`,`room_type` FROM `za_users_game_logs_result` where `room_no` = ? and `room_id` = ? ORDER BY `game_index` ASC ";
		JSONArray array = DBUtil.getObjectListBySQL(sql, new Object[] { roomNo, roomId });
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
		log.info(result.toString());
		CommonConstant.sendMsgEvent(client, result, CircleBaseEventConstant.CIRCLE_RECORD_INFO_EVENT_PUSH);
	}

	public void quickJoinRoomCircleEvent(SocketIOClient client, JSONObject object) {
		if (!JsonUtil.isNullVal(object, new String[] { CommonConstant.USERS_ID, "gid", "clubCode", "createRoomType" }))
			return;
		JSONObject baseInfo = object.getJSONObject("base_info");
		String userId = object.getString(CommonConstant.USERS_ID);// 用户Id
		JSONObject user = BaseSqlUtil.getObjectByOneConditions("*", "za_users", "id", userId);

		String payType = baseInfo.getString("paytype");
		// 开房需要的房卡
		int cost = BaseEventDeal.getRoomCardPayInfo(baseInfo);
		baseInfo.put("createRoom", userId);// 开房者
		// 俱乐部验证
		String clubCode = object.getString("clubCode");// 亲友圈账号
		JSONObject circleInfo = BaseSqlUtil.getObjectByConditions("*", "game_circle_info",
				new String[] { "circle_code", "circle_status", "is_delete" },
				new Object[] { clubCode, CircleConstant.CIRCLE_INFO_AUDIT, CircleConstant.IS_DELETE_N });
		if (CommonConstant.ROOM_PAY_TYPE_FANGZHU.equals(payType)) {// 0.桌费由开桌者支付
			double userBalance = user.getDouble("roomcard");
			if (cost == -1 || cost > userBalance) {
				CommonConstant.sendMsgEventNo(client, "余额不足，无法创建房间", null,
						CircleBaseEventConstant.QUICK_JOIN_ROOM_CIRCLE__EVENT_PUSH);
				return;
			}

		} else if (CommonConstant.ROOM_PAY_TYPE_FUND.equals(payType)) {// 1:桌费从基金中支付
			double circleBalance = circleInfo.getDouble("fund_count");
			if (cost == -1 || cost > circleBalance) {
				CommonConstant.sendMsgEventNo(client, "余额不足，请充值基金", null,
						CircleBaseEventConstant.QUICK_JOIN_ROOM_CIRCLE__EVENT_PUSH);
				return;
			}
		}
		object.put("circleId", circleInfo.getString("id"));
		log.warn("---baseInfo:"+object.getJSONObject("base_info"));
		if (!BaseInfoUtil.checkBaseInfo(object.getJSONObject("base_info"), object.getInt("gid"))) {
			CommonConstant.sendMsgEventNo(client, "敬请期待", null, "enterRoomPush_NN");
			return;
		}

		// 俱乐部 房间 附加参数 start
		JSONObject o = new JSONObject();
		if (object.containsKey(CommonConstant.USERS_ID))
			o.put(CommonConstant.USERS_ID, object.getString(CommonConstant.USERS_ID));
		if (object.containsKey("gid"))
			o.put("gid", object.getInt("gid"));
		if (object.containsKey("port"))
			o.put("port", object.getInt("port"));
		if (object.containsKey("ip"))
			o.put("ip", object.getString("ip"));
		if (object.containsKey("match_num"))
			o.put("match_num", object.getString("match_num"));
		if (object.containsKey("clubCode"))
			o.put("clubCode", object.getString("clubCode"));
		if (object.containsKey("circleId"))
			o.put("circleId", object.getString("circleId"));
		if (object.containsKey("location"))
			o.put("location", object.getString("location"));
		if (object.containsKey("createRoomType"))
			o.put("createRoomType", object.getString("createRoomType"));
		if (object.containsKey("isAllStart"))
			o.put("isAllStart", object.getString("isAllStart"));
		if (object.containsKey("fire"))
			o.put("fire", object.getString("fire"));
		baseInfo.put("gameRoomOther", o);
		// 俱乐部 房间 附加参数 end

		// 创建房间
		baseEventDeal.createRoomBase(client, object, user);
	}

	public void removeRoomCircleEvent(SocketIOClient client, JSONObject object) {
		System.out.println("解散房间请求");
		String eventName = CircleBaseEventConstant.REMOVE_ROOM_CIRCLE_EVENT_PUSH;

		String roomNo = object.getString(CommonConstant.ROOM_NO);
		GameRoom room = RoomManage.gameRoomMap.get(roomNo);
		// 获取游戏ID
		int gid = room.getGid();
		if (null == room) {
			CommonConstant.sendMsgEventNo(client, "房间不存在", null, eventName);
			return;
		}

		// 房间处于初始状态和准备状态才能解散
		if (room.getGameStatus() != 0 && room.getGameIndex() > 0) {
			// 十三水房间解散处理 start
			if (CommonConstant.GAME_ID_SSS == gid) {
				JSONObject data = new JSONObject();
				data.put(CommonConstant.DATA_KEY_ROOM_NO, roomNo);
				sssGameEventDealNew.forcedRoom(data);
				// 通知解散者
				CommonConstant.sendMsgEventYes(client, "解散成功", eventName);
				return;
			}
			// 十三水房间解散处理 end

			// 泉州麻将房间解散处理 start
			if (CommonConstant.GAME_ID_QZMJ == gid) {
				JSONObject data = new JSONObject();
				data.put(CommonConstant.DATA_KEY_ROOM_NO, roomNo);
				qzmjGameEventDeal.forcedRoom(data);
				// 通知解散者
				CommonConstant.sendMsgEventYes(client, "解散成功", eventName);
				return;
			}
			// 泉州麻将房间解散处理 end

			// 跑得快房间解散处理 start
			if (CommonConstant.GAME_ID_PDK == gid) {
				JSONObject data = new JSONObject();
				data.put(CommonConstant.DATA_KEY_ROOM_NO, roomNo);
				pdkGameEventDeal.gameForcedRoomEvent(data);
				// 通知解散者
				CommonConstant.sendMsgEventYes(client, "解散成功", eventName);
				return;
			}
			// 跑得快房间解散处理 end

			// NN解散 start
			if (CommonConstant.GAME_ID_NN == gid) {
				System.out.println("nn解散");
				JSONObject data = new JSONObject();
				data.put(CommonConstant.DATA_KEY_ROOM_NO, roomNo);
				nnGameEventDealNew.forcedRoom(data);
				// 通知解散者
				CommonConstant.sendMsgEventYes(client, "解散成功", eventName);
				return;
			}
			//炸金花解散
			if (CommonConstant.GAME_ID_ZJH == gid) {
				System.out.println("炸金花解散");
				JSONObject data = new JSONObject();
				data.put(CommonConstant.DATA_KEY_ROOM_NO, roomNo);
				zjhGameEventDealNew.forcedRoom(data);
				// 通知解散者
				CommonConstant.sendMsgEventYes(client, "解散成功", eventName);
				return;
			}
			// NN解散 end
			CommonConstant.sendMsgEventNo(client, "游戏中的房间不能解散", null, eventName);
			return;
		}
		// 通知房间内玩家
		List<UUID> uuidList = room.getAllUUIDList();
		// 移除房间
		RoomManage.gameRoomMap.remove(roomNo);
		// 通知玩家
		JSONObject result = new JSONObject();
		result.element("type", CommonConstant.SHOW_MSG_TYPE_BIG);
		result.element(CommonConstant.RESULT_KEY_MSG, "房间已解散");
		CommonConstant.sendMsgEventAll(uuidList, result, "tipMsgPush");
		// 更新数据库房间
		JSONObject roomInfo = new JSONObject();
		roomInfo.put("room_no", room.getRoomNo());
		roomInfo.put("status", CommonConstant.CLOSE_ROOM_TYPE_FINISH);
		producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
		// 通知解散者
		CommonConstant.sendMsgEventYes(client, "解散成功", eventName);
	}

	public void circleMemberInfoListEvent(SocketIOClient client, JSONObject object) {
		String eventName = CircleBaseEventConstant.CIRCLE_MEMBER_INFO_LIST_EVENT_PUSH;
		if (!JsonUtil.isNullVal(object,
				new String[] { "circleId", CommonConstant.USERS_ID, CommonConstant.DATA_KEY_PLATFORM })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "参数问题：circleId,uid,platform", eventName);
			return;
		}
		String time = DateUtils.searchTime(object.get("time"));// 日期搜索

		int pageIndex = !object.containsKey("pageIndex") ? 1 : object.getInt("pageIndex");// 第几页
		int pageSize = !object.containsKey("pageSize") ? 6 : object.getInt("pageSize");// 一页几条
		Object userAccount = object.get("memberUserAccount"); // 搜索成员account
		String platform = object.getString("platform");
		String orderByTotalScore = "DESC";// 总得分降序
		if (object.containsKey("orderByTotalScore")) {
			if ("ASC".equals(object.getString("orderByTotalScore"))) {
				orderByTotalScore = "ASC";
			}
		}

		// 条件搜索 0:全部搜索 1:直属搜索 2:非直属
		String condition = object.containsKey("condition") ? object.getString("condition")
				: CircleConstant.LIST_CONDITION_ALL;
		if (!CircleConstant.LIST_CONDITION_ALL.equals(condition)
				&& !CircleConstant.LIST_CONDITION_LOWER_YES.equals(condition)
				&& !CircleConstant.LIST_CONDITION_LOWER_NO.equals(condition)) {
			CommonConstant.sendMsgEventNo(client, "condition类型错误", null, eventName);
			return;
		}

		Object circleId = object.get("circleId");
		Object userId = object.get(CommonConstant.USERS_ID);
		// 用户验证
		JSONObject circleMember = DBUtil.getObjectBySQL(
				"SELECT z.*,f.`circle_code` FROM game_circle_member z LEFT JOIN game_circle_info f ON z.`circle_id`=f.`id` WHERE z.`user_id`= ? AND z.`circle_id`= ? AND z.`is_delete`= ? ",
				new Object[] { userId, circleId, CircleConstant.IS_DELETE_N });
		if (null == circleMember || !circleMember.containsKey("user_role") || !circleMember.containsKey("is_admin")
				|| !circleMember.containsKey("circle_code") || !circleMember.containsKey("id")
				|| !circleMember.containsKey("user_code")) {
			CommonConstant.sendMsgEventNo(client, "用户不存在", null, eventName);
			return;
		}
		// 最后返回的结果集
		String userRole = circleMember.getString("user_role");// 用户角色（字典）1:圈主 2:合伙人 3:玩家
		// 获取上级用户ID
		Object superUserId = circleMember.get("id");
		// 获取上级编码
		String userCode = circleMember.getString("user_code");
		String adminRole = circleMember.getString("is_admin");
		boolean isAdminRole = CircleConstant.MEMBER_ROLE_WJ.equalsIgnoreCase(userRole)
				&& CircleConstant.MEMBER_IS_MGR.equals(adminRole);

		// 判断是否是创建者或合伙人
		if (CircleConstant.MEMBER_ROLE_WJ.equals(userRole) && CircleConstant.MEMBER_NOT_MGR.equals(adminRole)) {
			CommonConstant.sendMsgEventNo(client, "权限不够", "您不是创建者或合伙人或管理员,无法查看该列表", eventName);
			return;
		}
		// 合伙人 只查看自己的下级 下下级。。。
		if (CircleConstant.MEMBER_ROLE_HHR.equals(userRole)) {
		} else {
			superUserId = null;
		}

		// 管理员 查看的直属下级是创建者的直属下级
		if (isAdminRole) {
			JSONObject masterMemberInfo = circleBiz.getMasterMemberInfo(Long.valueOf((String) circleId));
			userCode = masterMemberInfo == null ? userCode
					: masterMemberInfo.containsKey("user_code") ? masterMemberInfo.getString("user_code") : userCode;
		}

		JSONObject circleMemberInfoList = circleBiz.getMemberPage(1, circleId, userAccount, superUserId, condition,
				userCode, orderByTotalScore, circleMember.getString("circle_code"), time, new int[] { 8, 9 }, pageIndex,
				pageSize);

		JSONArray memberList = new JSONArray();
		if (circleMemberInfoList.containsKey("list")) {
			JSONArray newData = circleMemberInfoList.getJSONArray("list");
			for (Object aMemberList : newData) {
				JSONObject member = JSONObject.fromObject(aMemberList);
				String memberSuperUserCode = member.containsKey("superior_user_code")
						? member.getString("superior_user_code")
						: null;
				boolean isUnder = true;
				// 是否直属标记
				boolean isSuperiorRelation = userCode.equals(memberSuperUserCode);
				String user_id = member.getString("user_id");
				// 获取管理员权限
				String memberAdminRole = member.containsKey("is_admin") ? member.getString("is_admin")
						: CircleConstant.MEMBER_NOT_MGR;
				boolean isOnline = redisInfoService.isOnline(user_id);
				// 获取成员权限
				String memberUserRole = member.containsKey("user_role") ? member.getString("user_role")
						: CircleConstant.MEMBER_ROLE_WJ;
				// 判断操作者是否是管理员
				if (isAdminRole && CircleConstant.MEMBER_IS_MGR.equals(memberAdminRole)) {
					isUnder = false;
				}
				if (user_id.equals(userId.toString())) {
					isUnder = false;
				}
				if (CircleConstant.MEMBER_ROLE_QZ.equals(memberUserRole)) {
					isUnder = false;
				}
				member.element("isOnline", isOnline);
				member.element("isUnder", isUnder);
				member.element("isSuperiorRelation", isSuperiorRelation);
				member.element("user_name", member.getString("name"));// 前端需要 暂时提供
				memberList.add(member);
			}
		}
		List userIds = new ArrayList();
		if (CircleConstant.MEMBER_ROLE_HHR.equals(userRole)) {
			JSONArray userArrays = DBUtil.getObjectListBySQL(
					"SELECT user_id FROM game_circle_member WHERE seq_member_ids LIKE CONCAT('%$',?,'$%') and is_delete = 'N' OR id = ?",
					new Object[] { circleMember.getLong("id"), circleMember.getLong("id") });
			if (userArrays != null) {
				for (int i = 0; i < userArrays.size(); i++) {
					JSONObject temp = userArrays.getJSONObject(i);
					if (temp != null && temp.containsKey("user_id")) {
						userIds.add(temp.get("user_id"));
					}
				}
			}
		}
		// 总房卡消耗
		JSONObject roomCardAll = gameLogBiz.getRoomcardAllSpend(circleId, ZAUserdeductionConstant.CLUB_TYPE_QYQ, time,
				null);
		// 总局数
		JSONObject gameAllCount = gameLogBiz.getGameAllCount(circleMember.getString("circle_code"), new int[] { 8, 9 },
				time, userIds, platform);
		JSONObject result = new JSONObject();
		result.element("time", time).element("data", memberList)
				.element("roomCardAll",
						roomCardAll.containsKey("roomCardAll") ? Dto.zerolearing(roomCardAll.getDouble("roomCardAll"))
								: "0")
				.element("gameAllCount", /*
											 * gameAllCount.containsKey("gameAllCount") ?
											 * Dto.zerolearing(gameAllCount.getDouble("gameAllCount")) :
											 */ "0")
				.element("pageIndex",
						circleMemberInfoList.containsKey("pageIndex") ? circleMemberInfoList.getInt("pageIndex") : 1)
				.element("pageSize",
						circleMemberInfoList.containsKey("pageSize") ? circleMemberInfoList.getInt("pageSize") : 6)
				.element("totalPage",
						circleMemberInfoList.containsKey("totalPage") ? circleMemberInfoList.getInt("totalPage") : 0)
				.element("totalCount",
						circleMemberInfoList.containsKey("totalCount") ? circleMemberInfoList.getInt("totalCount") : 0);
		result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		result.element(CommonConstant.RESULT_KEY_MSG, "操作成功！");
		try {
			JSONArray data = (JSONArray) result.get("data");
			int allhp = 0;
			for (int i = 0; i < data.size(); i++) {
				JSONObject jsonObject = (JSONObject) data.get(i);
				Double hp = Double.parseDouble(jsonObject.get("user_hp").toString());
				allhp += hp;
			}
			result.element("allhp", allhp);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		log.info("circleMemberInfoListEvent result:{}", result);
		CommonConstant.sendMsgEvent(client, result, eventName);

	}

	public void offlineCircleEvent(SocketIOClient client, JSONObject object) {
		String eventName = CircleBaseEventConstant.OFFLINE_CIRCLE_EVENT_PUSH;
		if (!JsonUtil.isNullVal(object, new String[] { CommonConstant.USERS_ID })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "参数问题：uid", eventName);
			return;
		}
		CommonConstant.sendMsgEventYes(client, "退出成功", eventName);
	}

	public void quickEntryRoomCircleEvent(SocketIOClient client, JSONObject object) {
		String eventName = CircleBaseEventConstant.QUICK_ENTRY_ROOM_CIRCLE_EVENT_PUSH;

		if (!JsonUtil.isNullVal(object, new String[] { CommonConstant.USERS_ID, "circleId" })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "参数问题：uid,circleId", eventName);
			return;
		}
		String usersId = object.getString(CommonConstant.USERS_ID);
		String circleId = object.getString("circleId");
		Integer gid = object.containsKey("gid") ? object.getInt("gid") : null;
		// 俱乐部用户加入房间校验 start
		Map<String, String> map = circleService.verifyJoinRoom(usersId, circleId);

		if (null == map || !map.containsKey("type")) {
			CommonConstant.sendMsgEventNo(client, "请稍后再试", null, eventName);
			return;
		}
		int type = Integer.valueOf(map.get("type"));
		if (2 != type && 0 != type) {
			CommonConstant.sendMsgEventNo(client, "请稍后再试", null, eventName);
			return;
		}
		if (2 == type) {
			CommonConstant.sendMsgEventNo(client, CircleConstant.MEMBER_NOT_USE_MSG, null, eventName);
			return;
		}
		// 俱乐部用户加入房间校验 end
		Map<String, Integer> roomMap = new HashMap<>();

		String account = map.get("account");
		// 游戏停服中
		if (!redisInfoService.getStartStatus(null, account)) {
			CommonConstant.sendMsgEventNo(client, CommonConstant.CLOSING_DOWN_MSG, null, eventName);
			return;
		}

		for (String room_No : RoomManage.gameRoomMap.keySet()) {
			GameRoom room = RoomManage.gameRoomMap.get(room_No);
			if (null == room)
				continue;
			// 如果在房间里排除
			for (String room_account : room.getPlayerMap().keySet()) {
				if (room_account.equals(account)) {
					CommonConstant.sendMsgEventNo(client, "您已经在茶楼房间中...", null, eventName);
					return;
				}
			}
		}

		// 用户体力是否充足
		boolean isEnoughHp = true;

		for (String roomNo : RoomManage.gameRoomMap.keySet()) {
			if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
				GameRoom room = RoomManage.gameRoomMap.get(roomNo);
				if (room == null)
					continue;
				if (null != gid && gid != room.getGid()) {
					continue;
				}
				if (room.getGameStatus() == CommonConstant.CLOSE_ROOM_TYPE_GAME)
					// 是否开发
					if (!room.isOpen())
						continue;
				// 俱乐部自由场/局数场
				if (room.getRoomType() != CommonConstant.ROOM_TYPE_FREE
						&& room.getRoomType() != CommonConstant.ROOM_TYPE_INNING)
					continue;
				// 俱乐部
				if (!circleId.equals(room.getCircleId()))
					continue;

				// 是否人未满房间
				if (room.getPlayerMap().size() >= room.getPlayerCount())
					continue;

				// 入场
				if (Double.valueOf(map.get("userHp")) < room.getEnterScore()) {
					isEnoughHp = false;
					continue;
				}

				if (CommonConstant.ROOM_PAY_TYPE_AA.equals(String.valueOf(room.getPayType()))// AA支付
						&& Integer.valueOf(map.get("roomcard")) < room.getSinglePayNum()) {
					isEnoughHp = false;
					continue;
				}

				// 如果在房间里排除
				if (room.getPlayerMap().containsKey(account) || room.getVisitPlayerMap().containsKey(account)) {
					break;
				}

				roomMap.put(roomNo, room.getPlayerMap().size());
			}
		}

		if (roomMap.size() == 0) {
			// 玩家是否有充足的体力
			if (!isEnoughHp) {
				CommonConstant.sendMsgEventNo(client, "能量不足", null, eventName);
				return;
			}
			CommonConstant.sendMsgEventNo(client, "当前没有房间可以加入,请稍后再试", null, eventName);
			return;
		}

		String roomNo = "";
		int i = 0;
		for (String s : roomMap.keySet()) {
			if (i <= roomMap.get(s)) {
				roomNo = s;
				i = roomMap.get(s);
			}
		}
		if ("".equals(roomNo)) {
			CommonConstant.sendMsgEventNo(client, "当前没有房间可以加入,请稍后再试", null, eventName);
			return;
		}
		CommonConstant.sendMsgEventYes(client, "房间匹配中，请稍候...", eventName);

		log.info("account:[{}]在亲友圈快速开始找到房间:[{}]", account, roomNo);
		JSONObject postData = new JSONObject();
		postData.put(CommonConstant.DATA_KEY_ROOM_NO, roomNo);
		postData.put(CommonConstant.DATA_KEY_ACCOUNT, account);
		postData.put("uuid", object.get(CommonConstant.USERS_TOKEN));
		if (object.containsKey("location")) {
			postData.put("location", object.get("location"));
		}
		baseEventDeal.joinRoomBase(client, postData);
	}

	public void resetPartnerEvent(SocketIOClient client, JSONObject object) {
		String eventName = CircleBaseEventConstant.RESET_PARTNER_EVENT_PUSH;
		if (!JsonUtil.isNullVal(object,
				new String[] { CommonConstant.USERS_ID, "circleId", "operatedId", "platform" })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "参数问题：uid,circleId,operatedId,platform", eventName);
			return;
		}

		// 用户ID
		long userId = object.getLong(CommonConstant.USERS_ID);
		// 俱乐部ID
		long circleId = object.getLong("circleId");
		// 被操作者ID
		long operatedId = object.getLong("operatedId");
		// 平台号
		String platform = object.getString("platform");

		// 操作者成员信息
		JSONObject userMember = circleBiz.getCircleMemberByUserId(userId, circleId, platform);
		// 被操作者成员信息
		JSONObject operatedMember = circleBiz.getCircleMemberByUserId(operatedId, circleId, platform);

		if (userMember == null) {
			CommonConstant.sendMsgEventNo(client, "请刷新后重试", "操作者不存在", eventName);
			return;
		}

		if (operatedMember == null) {
			CommonConstant.sendMsgEventNo(client, "该用户不存在,请刷新后重试", "被操作者不存在", eventName);
			return;
		}

		// 获取操作者管理权限
		String adminRole = userMember.containsKey("is_admin") ? userMember.getString("is_admin")
				: CircleConstant.MEMBER_NOT_MGR;
		// 获取操作者的成员权限
		String userRole = userMember.containsKey("user_role") ? userMember.getString("user_role")
				: CircleConstant.MEMBER_ROLE_WJ;

		// 判断是否是管理员
		if (CircleConstant.MEMBER_ROLE_WJ.equals(userRole) && CircleConstant.MEMBER_IS_MGR.equals(adminRole)) {
			userMember = circleBiz.getMasterMemberInfo(circleId);
		}

		// 判断是否操作者和被操作者是否是上下级关系
		if (!circleBiz.isSuperiorRelation(userMember, operatedMember)) {
			CommonConstant.sendMsgEventNo(client, "用户权限不够,请重试", "俩个成员不是上下级关系或俩个成员不全是合伙人", eventName);
			return;
		}

		// 重置合伙人
		try {
			Long operatedMemberId = operatedMember.containsKey("id") ? operatedMember.getLong("id") : -1;
			circleBiz.resetPartner(operatedMemberId, circleId, userId, operatedId, platform);
		} catch (Exception e) {
			log.info("===========重置合伙人失败=============");
		}

		CommonConstant.sendMsgEventYes(client, "合伙人重置成功", eventName);
	}

	public void readMessageEvent(SocketIOClient client, JSONObject object) {
		String eventName = CircleBaseEventConstant.READ_MESSAGE_EVENT_PUSH;
		if (!JsonUtil.isNullVal(object, new String[] { CommonConstant.USERS_ID, "msgId" })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "参数问题：uid,circleId,msgId", eventName);
			return;
		}
		// 消息ID
		long msgId = object.getLong("msgId");
		// 用户ID
		long userId = object.getLong(CommonConstant.USERS_ID);
		// 平台号
		String platform = object.getString("platform");
		JSONObject gameCircleMessage = DBUtil.getObjectBySQL(
				"SELECT g.`is_read`,g.`msg_pid`,g.`message_type` FROM game_circle_message g WHERE g.`id`=?",
				new Object[] { msgId });
		if (null == gameCircleMessage || !gameCircleMessage.containsKey("is_read")) {
			CommonConstant.sendMsgEventNo(client, "请刷新后再试", "找不到消息id", eventName);
			return;
		}
		// 审核消息记录ID
		long msgPid = gameCircleMessage.containsKey("msg_pid") ? gameCircleMessage.getLong("msg_pid") : -1;
		// 消息类型
		String messageType = gameCircleMessage.containsKey("message_type") ? gameCircleMessage.getString("message_type")
				: null;
		// 是否进行佣金审核信息的处理
		if (msgPid > 0 && CircleConstant.MESSAGE_TYPE_READ_BALANCE_REVIEW.equals(messageType)) {
			// 防重验证
			long repeatCheck = redisInfoService.readMessageRepeatCheck(userId);
			if (repeatCheck > 1L) {
				CommonConstant.sendMsgEventNo(client, "请稍后重试", "重复提交",
						CircleBaseEventConstant.CREATE_MODIFY_CIRCLE_EVENT_PUSH);
				return;
			}
			// 提取佣金信息处理
			int result = circleBiz.extractBalanceMessageHandle(msgPid, platform);
			// 删除防重缓存
			redisInfoService.delReadMessageRepeatCheck(userId);
			if (result < 0) {
				CommonConstant.sendMsgEventNo(client, "读取消息失败", "提取佣金失败", eventName);
				return;
			}
		}
		if (!CircleConstant.MESSAGE_IS_READ.equals(gameCircleMessage.getString("is_read"))) {
			circleBiz.updateMessage(msgId);
		}
		CommonConstant.sendMsgEventYes(client, "读取消息成功", eventName);

	}

	public void circleVisitJoinRoomEvent(SocketIOClient client, JSONObject object) {
		String eventName = CircleBaseEventConstant.CIRCLE_VISIT_JOIN_ROOM_EVENT_PUSH;
		if (!JsonUtil.isNullVal(object,
				new String[] { CommonConstant.USERS_ID, CommonConstant.DATA_KEY_ROOM_NO, "circleId" })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "参数问题：uid,room_no,circleId", eventName);
			return;
		}

		long usersId = object.getLong(CommonConstant.USERS_ID); // 玩家id
		String roomNo = object.getString(CommonConstant.DATA_KEY_ROOM_NO); // 房间号
		String circleId = object.getString("circleId");
		JSONObject o = DBUtil.getObjectBySQL(
				"SELECT z.`sex`,z.`headimg`,z.`name`,z.`account`,f.`circle_code` FROM game_circle_member g LEFT JOIN game_circle_info f ON g.`circle_id`=f.`id`LEFT JOIN za_users z ON g.`user_id`=z.`id`WHERE g.`circle_id`= ? AND g.`user_id`=? AND g.`is_delete`='N'",
				new Object[] { circleId, usersId });
		if (null == o || !o.containsKey("circle_code") || !o.containsKey("account")) {
			CommonConstant.sendMsgEventNo(client, "请刷新后再试", "用户不存在", eventName);
			return;
		}
		String account = o.getString("account");
		for (String room_No : RoomManage.gameRoomMap.keySet()) {
			GameRoom room = RoomManage.gameRoomMap.get(room_No);
			if (null == room)
				continue;
			// 如果在房间里排除
			for (String roomAccount : room.getPlayerMap().keySet()) {
				if (roomAccount.equals(account)) {
					String msg = "您已经在房间中...";
					if (CommonConstant.ROOM_TYPE_TEA == room.getRoomType()) {
						msg = "您已经在茶楼房间中...";
					}
					CommonConstant.sendMsgEventNo(client, msg, null, eventName);
					return;
				}
			}
		}
		GameRoom room = RoomManage.gameRoomMap.get(roomNo);
		if (null == room) {
			CommonConstant.sendMsgEventNo(client, "房间已经不存在了", null, eventName);
			return;
		}
		if (CommonConstant.ROOM_VISIT_YES != room.getVisit()) {
			CommonConstant.sendMsgEventNo(client, "该房间未开启观战模式", null, eventName);
			return;
		}

		if (null == room.getVisitPlayerMap().get(account)) {
			Playerinfo playerinfo = new Playerinfo();
			playerinfo.setUuid(client.getSessionId());
			playerinfo.setId(usersId);
			playerinfo.setAccount(account);
			playerinfo.setName(o.getString("name"));
			playerinfo.setHeadimg(o.getString("headimg"));
			playerinfo.setMyIndex(CommonConstant.ROOM_VISIT_INDEX);// 观战下标
			playerinfo.setCollapse(true);// 观战用户都处于破产
			room.getVisitPlayerMap().put(account, playerinfo);
		}
		// 通知玩家
		int gid = room.getGid();
		if (CommonConstant.GAME_ID_SSS == gid) {// 十三水
			// 进入房间通知自己
			sssGameEventDealNew.createRoom(client, object);
			return;
		}

	}

	public void circleVisitJoinGameEvent(SocketIOClient client, JSONObject object) {
		String eventName = CircleBaseEventConstant.CIRCLE_VISIT_JOIN_GAME_EVENT_PUSH;
		if (!JsonUtil.isNullVal(object, new String[] { CommonConstant.USERS_ID, CommonConstant.USERS_TOKEN,
				CommonConstant.DATA_KEY_ROOM_NO, CommonConstant.DATA_KEY_ACCOUNT, "seatIndex" })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "参数问题：uid,token,room_no,account,seatIndex", eventName);
			return;
		}
		String usersId = object.getString(CommonConstant.USERS_ID); // 玩家account
		String account = object.getString(CommonConstant.DATA_KEY_ACCOUNT); // 玩家account
		String roomNo = object.getString(CommonConstant.DATA_KEY_ROOM_NO); // 房间号
		// 需要入座的座位号
		int seatIndex = object.getInt("seatIndex");
		GameRoom room = RoomManage.gameRoomMap.get(roomNo);
		if (null == room) {
			CommonConstant.sendMsgEventNo(client, "房间已经不存在了", null, eventName);
			return;
		}
		Playerinfo playerinfo = room.getPlayerMap().get(account);
		if (null != playerinfo) {
			CommonConstant.sendMsgEventNo(client, "请刷新后再试", "你已经在房间里", eventName);
			return;
		}

		playerinfo = room.getVisitPlayerMap().get(account);
		if (null == playerinfo) {
			CommonConstant.sendMsgEventNo(client, "请刷新后再试", "你已经不在房间里", eventName);
			return;
		}

		// 座位号
		if (0 > seatIndex || room.getPlayerCount() <= seatIndex || 0L != room.getUserIdList().get(seatIndex)) {
			CommonConstant.sendMsgEventNo(client, "该座位已经有人", null, eventName);
			return;
		}

		// 玩家必须处于观战视角
		if (CommonConstant.ROOM_VISIT_INDEX != playerinfo.getMyIndex()) {
			CommonConstant.sendMsgEventNo(client, "请刷新后再试", "你已经入座", eventName);
			return;
		}
		// 俱乐部用户加入房间校验 start
		Map<String, String> map = circleService.verifyJoinRoom(usersId, room.getCircleId());

		if (null == map || !map.containsKey("type")) {
			CommonConstant.sendMsgEventNo(client, "请稍后再试", null, eventName);
			return;
		}
		int type = Integer.valueOf(map.get("type"));
		if (2 != type && 0 != type) {
			CommonConstant.sendMsgEventNo(client, "请稍后再试", null, eventName);
			return;
		}
		if (2 == type) {
			CommonConstant.sendMsgEventNo(client, CircleConstant.MEMBER_NOT_USE_MSG, null, eventName);
			return;
		}

		double userHp = Double.valueOf(map.get("userHp"));
		if (userHp < room.getEnterScore()) {
			CommonConstant.sendMsgEventNo(client, "能量不足", null, eventName);
			return;
		}

		if (CommonConstant.ROOM_PAY_TYPE_AA.equals(String.valueOf(room.getPayType()))// AA支付
				&& Integer.valueOf(map.get("roomcard")) < room.getSinglePayNum()) {
			CommonConstant.sendMsgEventNo(client, "房卡不足", null, eventName);
			return;
		}
		// 俱乐部用户加入房间校验 end
		JSONObject postData = new JSONObject();
		postData.put(CommonConstant.DATA_KEY_ROOM_NO, roomNo);
		postData.put(CommonConstant.DATA_KEY_ACCOUNT, account);
		postData.put("uuid", object.get(CommonConstant.USERS_TOKEN));
		postData.put("seatIndex", seatIndex);
		if (object.containsKey("location")) {
			postData.put("location", object.get("location"));
		}
		baseEventDeal.joinRoomBase(client, postData);
	}

	public void circleVisitListEvent(SocketIOClient client, JSONObject object) {
		String eventName = CircleBaseEventConstant.CIRCLE_VISIT_LIST_EVENT_PUSH;
		if (!JsonUtil.isNullVal(object, new String[] { CommonConstant.USERS_ID, CommonConstant.DATA_KEY_ROOM_NO,
				CommonConstant.DATA_KEY_ACCOUNT })) {
			CommonConstant.sendMsgEventNo(client, "系统错误", "参数问题：uid,room_no,account", eventName);
			return;
		}
		String account = object.getString(CommonConstant.DATA_KEY_ACCOUNT); // 玩家account
		String roomNo = object.getString(CommonConstant.DATA_KEY_ROOM_NO); // 房间号
		GameRoom room = RoomManage.gameRoomMap.get(roomNo);
		if (null == room) {
			CommonConstant.sendMsgEventNo(client, "房间已经不存在了", null, eventName);
			return;
		}
		Playerinfo playerinfo = null;
		if (room.getVisitPlayerMap().containsKey(account)) {
			playerinfo = room.getVisitPlayerMap().get(account);
		} else if (room.getPlayerMap().containsKey(account)) {
			playerinfo = room.getPlayerMap().get(account);
		}

		if (null == playerinfo) {
			CommonConstant.sendMsgEventNo(client, "请刷新后再试", "你已经不在房间里", eventName);
			return;
		}
		JSONObject result = new JSONObject();
		JSONArray array = new JSONArray();
		for (String a : room.getVisitPlayerMap().keySet()) {
			playerinfo = room.getVisitPlayerMap().get(a);
			if (null == playerinfo || CommonConstant.ROOM_VISIT_INDEX != playerinfo.getMyIndex())
				continue;
			JSONObject o = new JSONObject();
			o.put("account", playerinfo.getAccount());// 玩家account
			o.put("name", playerinfo.getName());// 昵称
			o.put("headimg", playerinfo.getRealHeadimg());// 头像
			array.add(o);
		}

		result.element("data", array);
		result.element("visitCount", array.size());
		result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
		CommonConstant.sendMsgEvent(client, result, eventName);
	}

	public void goBank(SocketIOClient client, JSONObject object) {
		String event = CircleBaseEventConstant.GO_BANK_EVENT_PUSH;
		String password = object.getString("password");
		System.out.println(password);
		int circleId = object.getInt("circle_id");
		System.out.println(circleId);
		String account = object.getString("uid"); // userId
		System.out.println(account);
		JSONObject circleBank = circleBiz.getBankMsgByCircle(password, circleId, account);
		JSONObject jsb = circleBiz.getCircleMemberByUserId(account, circleId);
		int userRole = jsb.getInt("user_role");
		boolean isHeHuoRen = userRole == 3 ? false : true;
		JSONObject data = new JSONObject();
		if (null == circleBank) {
			// 首次进入 保存密码数据
			JSONObject obj2 = new JSONObject();
			obj2.put("circle_id", circleId);
			obj2.put("user_id", account);
			obj2.put("password", password);
			obj2.put("money", 0);
			circleBiz.insertZaBank(obj2);
			data.put("code", 1);
			data.put("msg", "首次登录！密码已保存！");
		} else {
			if (password.equals(circleBank.getString("password"))) {
				data.put("code", 1);
				data.put("msg", "成功登录银行！");
			} else {
				data.put("code", 0);
				data.put("msg", "密码错误！忘记密码请联系管理员！");
			}
		}
		data.put("balance", circleBank == null ? 0 : circleBank.get("money"));
		data.put("isHeHuoRen", isHeHuoRen);
		System.out.println("data:" + data);
		CommonConstant.sendMsgEvent(client, data, event);
		System.out.println(circleBank);
	}

	public void operationMoneyEvent(SocketIOClient client, JSONObject object) {
		String event = CircleBaseEventConstant.OPERATION_MONEY_EVENT_PUSH;
		int circleId = object.getInt("circleId");
		String userId = object.getString("uid");
		int type = object.getInt("type");
		int money = type == 1 ? object.getInt("money") : -object.getInt("money");
		double hp = object.getDouble("hp");
		JSONObject jsb = circleBiz.getCircleMemberByUserId(userId, circleId);
		JSONObject data = new JSONObject();
		double serverHp = jsb.getDouble("user_hp");
		if (hp != serverHp) {
			data.put("code", 0);
			data.put("msg", "系统异常！请联系上级管理员！code=13");
		}
		int result = circleBiz.operationMoney(circleId, userId, money);

		if (result == 1) {
			// 取款或者存款 应该去修改成员的余额表
			int i = DBUtil.executeUpdateBySQL(
					"UPDATE game_circle_member SET user_hp = user_hp - ? WHERE user_id = ? AND circle_id = ?",
					new Object[] { money, userId, circleId });
			if (i > 0) {
				System.out.println("添加日志");
				circleBiz.savePropBill(userId, circleId, -money, hp, type == 1 ? "7" : "8", "HYQP", null,
						type == 1 ? "银行存款" : "银行取款", null);
				JSONObject circleBank = circleBiz.getBankMsgByCircle(null, circleId, userId);
				String balance = (String) circleBank.get("money");
				data.put("bankBalance", balance);
				data.put("code", 1);
				data.put("msg", "操作成功");
			} else {
				data.put("code", 0);
				data.put("msg", "系统异常！请联系上级管理员！code=12");
			}

		} else {
			data.put("code", 0);
			data.put("msg", "系统异常！请联系上级管理员！code=11");
		}
		System.out.println("data:" + data);
		CommonConstant.sendMsgEvent(client, data, event);
	}

	public void luckyDraw(SocketIOClient client, JSONObject object) {
		String event = CircleBaseEventConstant.LUCKY_DRAW_EVENT_PUHS;
		int userId = object.getInt("uid");
		int circleId = object.getInt("circleId");
		JSONObject user = userBiz.getUserByID(userId);
		JSONObject data = new JSONObject();
		if (Integer.parseInt(user.getString("roomcard")) < 200) {
			data.put("code", 0);
			data.put("errorMsg", "钻石不足！");
			CommonConstant.sendMsgEvent(client, data, event);
			return;
		}
		doRoomCardByLucky(-200, userId);
		int id = 1; // 可拓展 多个抽奖 目前就一个抽奖
		JSONObject luckyDraw = circleBiz.getLuckyDrawById(id);
		String chance = luckyDraw.getString("chance");
		String[] chances = chance.split(",");
		Random random = new Random();
		int count = random.nextInt(100) + 1;
		int luckyId = 0;
		for (int i = 0; i < chances.length; i++) {
			if (count <= Integer.parseInt(chances[i])) {
				luckyId = i;
				break;
			}
		}
		// 根据奖品 进行发奖
		if (doLucky(luckyId, userId, circleId)) {
			if (luckyId <= 3) {
				luckyId += 2;
			} else if (luckyId == 4) {
				luckyId = 0;
			} else if (luckyId == 5) {
				luckyId = 1;
			}
			data.put("luckyId", luckyId);
			data.put("count", chances.length);
			data.put("code", 1);
		} else {
			data.put("code", 0);
			data.put("errorMsg", "未知异常，请联系管理员，错误码：300");
		}
		System.out.println("返回客户端:" + data);
		System.out.println("事件：" + event);
		CommonConstant.sendMsgEvent(client, data, event);
		System.out.println("====");
	}

	public boolean doLucky(int luckyId, int userId, int circleId) {
		System.out.println("奖品：" + luckyId);
		// 2999 / 100体力/ 1体力/ 50钻石/100钻/ 空奖 / 0,1,5,20,50,100
		boolean flag = false;
		if (luckyId == 1 || luckyId == 2) {
			flag = doHpByLucky(luckyId == 1 ? 50 : 1, userId, circleId);
		}
		if (luckyId == 3 || luckyId == 4) {
			flag = doRoomCardByLucky(luckyId == 3 ? 50 : 100, userId);
		}
		if (luckyId == 5) {
			flag = true;
		}
		return flag;
	}

	public boolean doHpByLucky(int count, int userId, int circleId) {
		JSONObject jsb = circleBiz.getCircleMemberByUserId(userId + "", circleId);
		JSONObject data = new JSONObject();
		double usreHp = jsb.getDouble("user_hp");
		int i = 0;
		int j = 0;
		try {
			i = DBUtil.executeUpdateBySQL(
					"UPDATE game_circle_member SET user_hp = user_hp + ? WHERE user_id = ? AND circle_id = ?",
					new Object[] { count, userId, circleId });
			JSONObject boos = circleBiz.getBoos(circleId);
			int boosId = boos.getInt("user_id");
			System.out.println("盟主id：" + boosId);
			JSONObject user = userBiz.getUserByID(userId);
			j = DBUtil.executeUpdateBySQL(
					"UPDATE game_circle_member SET user_hp = user_hp - ? WHERE user_id = ? AND circle_id = ?",
					new Object[] { count, userId, circleId });
			circleBiz.savePropBill(userId, circleId, count, usreHp, "9", "HYQP", null, "转盘奖励", null);
			circleBiz.savePropBill(boosId, circleId, -count, boos.getDouble("user_hp"), "10", "HYQP", boosId,
					user.get("name") + "抽奖扣除(" + user.getString("account") + ")", userId);
		} catch (Exception e) {
			log.error(e + "!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		}
		if (i > 0 && j > 0) {
			return true;
		}
		return false;
	}

	public boolean doRoomCardByLucky(int count, int userId) {
		int result = zaNewSignBiz.updateUserRoomcardBySign(count, userId);
		return result > 0 ? true : false;
	}
}
