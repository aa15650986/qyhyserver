package com.zhuoan.biz.event.tea;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.event.BaseEventDeal;
import com.zhuoan.biz.game.biz.GameLogBiz;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.bdx.BDXGameRoomNew;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.ddz.DdzGameRoom;
import com.zhuoan.biz.model.gdy.GDYGameRoom;
import com.zhuoan.biz.model.gppj.GPPJGameRoom;
import com.zhuoan.biz.model.gzmj.GzMjGameRoom;
import com.zhuoan.biz.model.nn.NNGameRoomNew;
import com.zhuoan.biz.model.qzmj.QZMJGameRoom;
import com.zhuoan.biz.model.sg.SGGameRoom;
import com.zhuoan.biz.model.sss.SSSGameRoomNew;
import com.zhuoan.biz.model.zjh.ZJHGameRoomNew;
import com.zhuoan.constant.*;
import com.zhuoan.constant.event.enmu.PDKEventEnum;
import com.zhuoan.dao.DBJsonUtil;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.TeaService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.util.DateUtils;
import com.zhuoan.util.Dto;
import com.zhuoan.util.JsonUtil;
import com.zhuoan.util.TimeUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.*;


@Component
public class TeaEventDeal {
    private final static Logger log = LoggerFactory.getLogger(TeaEventDeal.class);

    @Resource
    private Destination daoQueueDestination;
    @Resource
    private ProducerService producerService;
    @Resource
    private TeaService teaService;
    @Resource
    private RedisInfoService redisInfoService;
    @Resource
    private GameLogBiz gameLogBiz;
    @Resource
    private BaseEventDeal baseEventDeal;
    @Resource
    private Destination sssQueueDestination;
    @Resource
    private Destination pdkQueueDestination;
    @Resource
    private Destination qzmjQueueDestination;

    public void teaTest(SocketIOClient client, JSONObject object, String eventName) {
        log.info("茶楼测试接收到客户端数据：" + object.toString());
        if (object == null) CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数", eventName);
        else CommonConstant.sendMsgEventYes(client, "成功", eventName);
    }


    public void listEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid]", eventName);
            return;
        }
        long usersId = object.getLong(CommonConstant.USERS_ID);//用户id

        JSONObject user = DBUtil.getObjectBySQL("SELECT id FROM za_users z WHERE z.`id`=? ", new Object[]{usersId});
        if (null == user) {
            CommonConstant.sendMsgEventNo(client, "用户不存在", null, eventName);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT f.`id`AS teaInfoId,f.`tea_code` as teaInfoCode,f.`tea_name`AS teaName,f.`type`AS teaType,z.`name`AS teaUserName,z.`headimg` AS teaHeadimg,t.`powern` AS memberPowern FROM tea_info f LEFT JOIN ")
                .append("tea_member t ON t.`tea_info_id`=f.`id`LEFT JOIN za_users z ON f.`users_id`=z.`id`WHERE t.`users_id`= ? AND t.`is_deleted`= ?  AND f.`audit`= ?")
                .append(" AND t.`audit`= ? AND f.`is_deleted` = ? ");
        JSONArray array = DBUtil.getObjectListBySQL(sb.toString(), new Object[]{usersId, TeaConstant.IS_DELETE_N, TeaConstant.AUDIT_Y, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
        for (int i = 0; i < array.size(); i++) {
            JSONObject o = array.getJSONObject(i);
            if (null == o) continue;
            if (o.containsKey("teaHeadimg")) {
                o.element("teaHeadimg", CommonConstant.getServerDomain() + o.get("teaHeadimg"));
            }
        }

        JSONObject result = new JSONObject();
        result.element("data", array);
        result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        CommonConstant.sendMsgEvent(client, result, eventName);

    }


    public void creatorEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaName", "gid", "base_info"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaName,gid,base_info]", eventName);
            return;
        }

        String teaName = object.getString("teaName");//茶楼名称
        if (teaName.length() <= 0) {
            CommonConstant.sendMsgEventNo(client, "请取个名称", null, eventName);
            return;
        }
        if (teaName.length() > 20) {
            CommonConstant.sendMsgEventNo(client, "名称过长", null, eventName);
            return;
        }

        int gid = object.getInt("gid");//游戏id
        JSONObject baseInfo = object.getJSONObject("base_info");//房间信息
        baseInfo.element("gid", gid);
        long usersId = object.getLong(CommonConstant.USERS_ID);//用户id
        JSONObject user = DBUtil.getObjectBySQL("SELECT z.`id`,z.`platform` FROM za_users z WHERE z.`id`= ? "
                , new Object[]{usersId});
        if (null == user) {
            CommonConstant.sendMsgEventNo(client, "请刷新后再试", null, eventName);
            return;
        }
//        代理验证 start
        //茶楼系统参数
        JSONObject teaSysValue = teaService.getSysValue(user.containsKey("platform") ? user.getString("platform") : "HYQP");
        //代理权限
        System.out.println("============"+teaSysValue);
        int teaProxy = !teaSysValue.containsKey(CacheKeyConstant.TEA_PROXY) ? TeaConstant.IS_TEA_PROXY : teaSysValue.getInt(CacheKeyConstant.TEA_PROXY);
        //判断是否需要代理
        if (teaProxy == TeaConstant.TEA_PROXY_YES) {
            JSONObject userTemp = DBUtil.getObjectBySQL("SELECT z.`id`,z.`platform`,t.`level_type`FROM za_users z LEFT JOIN t_proxy_info t ON z.`id`=t.`users_id`WHERE z.`id`= ? AND t.`is_deleted`= 0 AND t.`audit`= 1"
                    , new Object[]{usersId});
            if (null == userTemp || !userTemp.containsKey("level_type")) {
                CommonConstant.sendMsgEventNo(client, "请先成为代理，才可创建茶楼", null, eventName);
                return;
            }
        }
        //        代理验证 end
        //创建茶楼的数量
        int teaCount = !teaSysValue.containsKey(CacheKeyConstant.TEA_COUNT) ? TeaConstant.TEA_NUMBER : teaSysValue.getInt(CacheKeyConstant.TEA_COUNT);
        JSONObject teaInfo = DBUtil.getObjectBySQL("SELECT COUNT(*)AS number FROM tea_info f WHERE f.`users_id` = ? AND f.`audit`= ? AND f.`is_deleted`= ? GROUP BY f.`users_id`"
                , new Object[]{usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
        if (null != teaInfo && teaCount <= teaInfo.getInt("number")) {
            CommonConstant.sendMsgEventNo(client, "您创建的茶楼已经达到最大值:" + teaCount, null, eventName);
            return;
        }
        teaInfo = new JSONObject();
        teaInfo
                .element("tea_code", teaService.getTeaCode())
                .element("users_id", usersId)
                .element("tea_name", teaName)
                .element("base_info", baseInfo)
                .element("audit", TeaConstant.AUDIT_Y)
                .element("platform", user.get("platform"));

        long teaInfoId = DBJsonUtil.addGetId(teaInfo, "tea_info");
        if (teaInfoId > 0L) {
            JSONObject teaMember = new JSONObject();
            teaMember
                    .element("tea_info_id", teaInfoId)
                    .element("tea_info_code", teaInfo.get("tea_code"))
                    .element("users_id", usersId)
                    .element("powern", TeaConstant.TEA_MEMBER_POWERN_LZ)//楼主
                    .element("audit", TeaConstant.AUDIT_Y)
                    .element("platform", user.get("platform"));
            int memberAdd = DBJsonUtil.add(teaMember, "tea_member");
            if (memberAdd > 0) {
                CommonConstant.sendMsgEventYes(client, "操作成功", eventName);
                return;
            }
            DBJsonUtil.update(new JSONObject().element("id", teaInfoId).element("is_deleted", TeaConstant.IS_DELETE_Y).element("memo", "创建失败删除")
                    , "tea_info");//删除失败的
        }
        CommonConstant.sendMsgEventNo(client, "操作失败", null, eventName);

    }


    public void joinEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode]", eventName);
            return;
        }
        long usersId = object.getLong(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//茶楼编号

        JSONObject teaInfo = DBUtil.getObjectBySQL("SELECT id,platform FROM tea_info z WHERE  z.`tea_code`= ? AND z.`is_deleted`= ? AND z.`audit` = ?"
                , new Object[]{teaInfoCode, TeaConstant.IS_DELETE_N, TeaConstant.AUDIT_Y});
        if (null == teaInfo) {
            CommonConstant.sendMsgEventNo(client, "茶楼号不存在", null, eventName);
            return;
        }
        JSONObject teaMember = DBUtil.getObjectBySQL("SELECT id,audit FROM tea_member z WHERE z.`users_id`= ? AND z.`tea_info_code`= ? AND z.`is_deleted`= ?"
                , new Object[]{usersId, teaInfoCode, TeaConstant.IS_DELETE_N});
        if (null != teaMember) {
            if (TeaConstant.AUDIT_N == teaMember.getInt("audit")) {
                CommonConstant.sendMsgEventNo(client, "你已经申请了,请联系楼主", null, eventName);
                return;
            }
            if (TeaConstant.AUDIT_Y == teaMember.getInt("audit")) {
                CommonConstant.sendMsgEventNo(client, "您已经加入该茶楼", null, eventName);
                return;
            }
        }

        teaMember = new JSONObject();
        teaMember
                .element("tea_info_id", teaInfo.get("id"))
                .element("tea_info_code", teaInfoCode)
                .element("users_id", usersId)
                .element("powern", TeaConstant.TEA_MEMBER_POWERN_PTCY)//楼主
                .element("audit", TeaConstant.AUDIT_N)
                .element("platform", teaInfo.get("platform"));
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.APPLY_JOIN_TEA, teaMember));
        CommonConstant.sendMsgEventYes(client, "申请成功", eventName);
    }

    public void modifyNameEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaName", "teaInfoCode"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaName,teaInfoCode]", eventName);
            return;
        }
        long usersId = object.getLong(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号
        String teaName = object.getString("teaName");//茶楼名称
        if (teaName.length() <= 0) {
            CommonConstant.sendMsgEventNo(client, "请取个名称", null, eventName);
            return;
        }
        if (teaName.length() > 20) {
            CommonConstant.sendMsgEventNo(client, "名称过长", null, eventName);
            return;
        }

        JSONObject teaMember = DBUtil.getObjectBySQL("SELECT z.`powern`,z.`tea_info_id` FROM tea_member z WHERE z.`tea_info_code`=? AND z.`users_id` = ? AND z.`audit` = ? AND z.`is_deleted` = ?"
                , new Object[]{teaInfoCode, usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
        if (null == teaMember) {
            CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
            return;
        }
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY == teaMember.getInt("powern")) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }

        if (teaMember.containsKey("tea_info_id")) {
            long teaInfoId = teaMember.getLong("tea_info_id");
            if (teaInfoId > 0L) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.element("id", teaInfoId).element("tea_name", teaName);
                int i = DBJsonUtil.update(jsonObject, "tea_info", "id");
                if (i > 0) {
                    JSONObject result = new JSONObject();
                    result
                            .element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES)
                            .element(CommonConstant.RESULT_KEY_MSG, "操作成功")
                            .element("teaName", teaName)
                    ;
                    CommonConstant.sendMsgEvent(client, result, eventName);
                    return;
                }
            }
        }

        CommonConstant.sendMsgEventNo(client, "操作失败", null, eventName);

    }


    public void modifyPlayEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "base_info", "teaInfoCode", "gid"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,base_info,teaInfoCode,gid]", eventName);
            return;
        }
        long usersId = object.getLong(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号
        int gid = object.getInt("gid");//游戏id
        JSONObject baseInfo = object.getJSONObject("base_info");//房间信息
        baseInfo.element("gid", gid);

        JSONObject teaMember = DBUtil.getObjectBySQL("SELECT z.`powern`,z.`tea_info_id`,t.`type` FROM tea_member z LEFT JOIN tea_info t ON z.`tea_info_id`=t.`id` WHERE z.`tea_info_code`=? AND z.`users_id` = ? AND z.`audit` = ? AND z.`is_deleted` = ? AND t.`audit` = ? AND t.`is_deleted` = ?"
                , new Object[]{teaInfoCode, usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});

        if (null == teaMember || !teaMember.containsKey("type")) {
            CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
            return;
        }
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY == teaMember.getInt("powern")) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }

        if (TeaConstant.TEA_INFO_TYPE_KQ == teaMember.getInt("type")) {
            CommonConstant.sendMsgEventNo(client, "请先打烊茶楼", null, eventName);
            return;
        }

        if (teaMember.containsKey("tea_info_id")) {
            long teaInfoId = teaMember.getLong("tea_info_id");
            if (teaInfoId > 0L) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.element("id", teaInfoId).element("base_info", baseInfo);
                int i = DBJsonUtil.update(jsonObject, "tea_info", "id");
                if (i > 0) {
                    JSONObject result = new JSONObject();
                    result
                            .element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES)
                            .element(CommonConstant.RESULT_KEY_MSG, "操作成功")
                            .element("gameInfo", teaService.getRoomGameInfo(baseInfo))
                    ;
                    CommonConstant.sendMsgEvent(client, result, eventName);
                    return;
                }
            }
        }

        CommonConstant.sendMsgEventNo(client, "操作失败", null, eventName);
    }


    public void addMemberEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode", "addUsersAccount"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode,addUsersAccount]", eventName);
            return;
        }
        long usersId = object.getLong(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号
        String addUsersAccount = object.getString("addUsersAccount");// 添加成员的account

        //验证  邀请玩家
        JSONObject addUser = DBUtil.getObjectBySQL("SELECT z.`id`,z.`platform` FROM za_users z WHERE z.`account`= ? ", new Object[]{addUsersAccount});
        if (null == addUser) {
            CommonConstant.sendMsgEventNo(client, "请输入正确的玩家ID", null, eventName);
            return;
        }

        if (usersId == addUser.getLong("id")) {
            CommonConstant.sendMsgEventNo(client, "不能邀请自己", null, eventName);
            return;
        }
        JSONObject addMember = DBUtil.getObjectBySQL("SELECT z.`id`,z.`audit`,z.`is_deleted` FROM tea_member z WHERE z.`tea_info_code`= ? AND z.`users_id` = ?"
                , new Object[]{teaInfoCode, addUser.get("id")});
        if (null != addMember && addMember.containsKey("is_deleted") && addMember.containsKey("audit")) {
            if (TeaConstant.IS_DELETE_N == addMember.getInt("is_deleted")) {//未删除
                if (TeaConstant.AUDIT_Y == addMember.getInt("audit")) {
                    CommonConstant.sendMsgEventNo(client, "该玩家已经在该茶楼", null, eventName);
                    return;
                }
            }
        }

        //验证  操作玩家
        JSONObject teaMember = DBUtil.getObjectBySQL("SELECT z.`powern`,z.`tea_info_id` FROM tea_member z WHERE z.`tea_info_code`=? AND z.`users_id` = ? AND z.`audit` = ? AND z.`is_deleted` = ?"
                , new Object[]{teaInfoCode, usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});

        if (null == teaMember) {
            CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
            return;
        }
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY == teaMember.getInt("powern")) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }

        //保存玩家
        if (null == addMember) {
            addMember = new JSONObject();
            addMember
                    .element("tea_info_id", teaMember.get("tea_info_id"))
                    .element("tea_info_code", teaInfoCode)
                    .element("users_id", addUser.get("id"))
                    .element("platform", addUser.get("platform"));
        }
        addMember
                .element("powern", TeaConstant.TEA_MEMBER_POWERN_PTCY)//普通成员
                .element("audit", TeaConstant.AUDIT_Y)
                .element("is_deleted", TeaConstant.IS_DELETE_N);
        int i = DBJsonUtil.saveOrUpdate(addMember, "tea_member");
        if (i > 0) {
            CommonConstant.sendMsgEventYes(client, "操作成功", eventName);
            return;
        }
        CommonConstant.sendMsgEventNo(client, "操作失败", null, eventName);
    }


    public void importMemberListEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode]", eventName);
            return;
        }
        String usersId = object.getString(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//当前所在的茶楼号

        JSONObject user = DBUtil.getObjectBySQL("SELECT id FROM za_users z WHERE z.`id`=? ", new Object[]{usersId});
        if (null == user) {
            CommonConstant.sendMsgEventNo(client, "用户不存在", null, eventName);
            return;
        }
        //楼客权限 验证  start
        int powern = teaService.getTeaMemberPowern(usersId, teaInfoCode);//获取权限
        if (-1 == powern) {
            CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
            return;
        }
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY == powern) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }
        //楼客权限 验证  end
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT (SELECT COUNT(*)FROM tea_member g WHERE g.`tea_info_id`=f.`id`)AS number,f.`id`AS teaInfoId,f.`tea_code`AS teaInfoCode,f.`tea_name`AS teaName,f.`type`AS teaType,z.`name`AS teaUserName,z.`headimg`AS teaHeadimg FROM tea_member t LEFT JOIN tea_info f ON t.`tea_info_id`=f.`id`LEFT JOIN za_users z ON f.`users_id`=z.`id`WHERE ")
                .append("t.`users_id`=? AND t.`powern`IN(?,?)AND t.`audit`= ? AND t.`is_deleted`= ? AND t.`tea_info_code`!= ?")
                .append(" AND f.`audit`= ? AND f.`is_deleted`= ? GROUP BY f.`tea_code`");
        JSONArray array = DBUtil.getObjectListBySQL(sb.toString()
                , new Object[]{usersId, TeaConstant.TEA_MEMBER_POWERN_LZ, TeaConstant.TEA_MEMBER_POWERN_GLY, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N, teaInfoCode, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
        for (int i = 0; i < array.size(); i++) {
            JSONObject o = array.getJSONObject(i);
            if (null == o) continue;
            if (o.containsKey("teaHeadimg")) {
                o.element("teaHeadimg", CommonConstant.getServerDomain() + o.get("teaHeadimg"));
            }
        }

        JSONObject result = new JSONObject();
        result.element("data", array);
        result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        CommonConstant.sendMsgEvent(client, result, eventName);
    }


    public void importMemberEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode", "teaInfoCodeIds"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode,teaInfoCodeIds]", eventName);
            return;
        }
        String usersId = object.getString(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//当前所在的茶楼号
        String teaInfoCodeIds = object.getString("teaInfoCodeIds");//导入的茶楼号 用逗号隔开
        //楼客权限 验证  start
        int powern = teaService.getTeaMemberPowern(usersId, teaInfoCode);//获取权限
        if (-1 == powern) {
            CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
            return;
        }
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY == powern) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }
        //楼客权限 验证  end
        JSONObject o = new JSONObject();
        o
                .element("teaInfoCode", teaInfoCode)
                .element("teaInfoCodeIds", teaInfoCodeIds);
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.IMPORT_MEMBER_TEA, o));

        CommonConstant.sendMsgEventYes(client, "操作成功", eventName);
    }


    public void closesEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode", "teaInfoType"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode,teaInfoType]", eventName);
            return;
        }

        String usersId = object.getString(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号
        int teaInfoType = object.getInt("teaInfoType");


        if (TeaConstant.TEA_INFO_TYPE_DY != teaInfoType && TeaConstant.TEA_INFO_TYPE_KQ != teaInfoType) {
            CommonConstant.sendMsgEventNo(client, "teaInfoType类型错误", null, eventName);
            return;
        }

        JSONObject teaMember = DBUtil.getObjectBySQL("SELECT z.`powern`,f.`type` FROM tea_member z LEFT JOIN tea_info f ON z.`tea_info_id`=f.`id`AND z.`tea_info_code`=f.`tea_code` WHERE z.`tea_info_code`=? AND z.`users_id` = ? AND z.`audit` = ? AND z.`is_deleted` = ? AND f.`audit` = ? AND f.`is_deleted` = ?"
                , new Object[]{teaInfoCode, usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
        if (null == teaMember) {
            CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
            return;
        }
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY == teaMember.getInt("powern")) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }
        if (teaInfoType == teaMember.getInt("type")) {
            CommonConstant.sendMsgEventYes(client, "请刷新后再试", eventName);
            return;
        }


        JSONObject o = new JSONObject();
        o.element("tea_code", teaInfoCode).element("type", teaInfoType);
        int i = DBJsonUtil.update(o, "tea_info", "tea_code");
        if (i > 0) {

            JSONObject result = new JSONObject();
            result
                    .element("teaInfoType", teaInfoType)
                    .element(CommonConstant.RESULT_KEY_MSG, "操作成功")
                    .element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES)
            ;
            CommonConstant.sendMsgEvent(client, result, eventName);

            redisInfoService.delTeaRoomOpen(teaInfoCode);//清redis 缓存茶楼房间是否开放玩家娱乐

            List<String> roomList = new ArrayList<>();
            for (String roomNo : RoomManage.gameRoomMap.keySet()) {
                if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
                    GameRoom room = RoomManage.gameRoomMap.get(roomNo);
                    if (room == null) continue;

                    //俱乐部
                    if (!teaInfoCode.equals(room.getClubCode())) continue;
                    //茶楼
                    if (room.getRoomType() != CommonConstant.ROOM_TYPE_TEA)
                        continue;
                    if (room.getGameIndex() > 1) {//满人房间
                        continue;
                    }
                    if (room.getGameStatus() != 0 && room.getGameStatus() != 1) {
                        continue;
                    }
                    roomList.add(roomNo);
                }
            }
            //解散房间 start
            for (int j = 0; j < roomList.size(); j++) {
                o = new JSONObject();
                o
                        .element(CommonConstant.USERS_ID, usersId)
                        .element("roomNo", roomList.get(j))
                        .element("teaInfoCode", teaInfoCode)
                ;
                removeRoomEvent(null, o, null);
            }
            //解散房间 end
            return;
        }
        CommonConstant.sendMsgEventNo(client, "操作失败", null, eventName);
    }

    public void removeEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode]", eventName);
            return;
        }

        String usersId = object.getString(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号

        JSONObject teaMember = DBUtil.getObjectBySQL("SELECT z.`powern`,f.`type`,f.`proxy_money` FROM tea_member z LEFT JOIN tea_info f ON z.`tea_info_id`=f.`id`AND z.`tea_info_code`=f.`tea_code` WHERE z.`tea_info_code`=? AND z.`users_id` = ? AND z.`audit` = ? AND z.`is_deleted` = ? AND f.`audit` = ? AND f.`is_deleted` = ?"
                , new Object[]{teaInfoCode, usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
        if (null == teaMember || !teaMember.containsKey("proxy_money")) {
            CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
            return;
        }
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY == teaMember.getInt("powern")) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }

        if (TeaConstant.TEA_INFO_TYPE_KQ == teaMember.getInt("type")) {
            CommonConstant.sendMsgEventNo(client, "请先打烊再销毁茶楼", null, eventName);
            return;
        }

        if (teaMember.getInt("proxy_money") > 0) {
            CommonConstant.sendMsgEventNo(client, "销毁茶楼茶壶必须全部提取出来", null, eventName);
            return;
        }
        JSONObject o = new JSONObject();
        o
                .element("tea_code", teaInfoCode)
                .element("is_deleted", TeaConstant.IS_DELETE_Y)
        ;
        int i = DBJsonUtil.update(o, "tea_info", "tea_code");
        if (i > 0) {
            CommonConstant.sendMsgEventYes(client, "操作成功", eventName);
            return;
        }
        CommonConstant.sendMsgEventNo(client, "操作失败", null, eventName);
    }


    public void memberListEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode]", eventName);
            return;
        }
        String usersId = object.getString(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//当前所在的茶楼号
        String usersAccountOrName = object.containsKey("usersAccountOrName") ? object.getString("usersAccountOrName") : null;//楼客搜索
        int page = object.get("pageIndex") == null ? 1 : object.getInt("pageIndex");
        int limitNum = object.get("pageSize") == null ? 6 : object.getInt("pageSize");

        JSONObject teaMember = DBUtil.getObjectBySQL("SELECT z.`powern`,z.`tea_info_id` FROM tea_member z WHERE z.`tea_info_code`=? AND z.`users_id` = ? AND z.`audit` = ? AND z.`is_deleted` = ?"
                , new Object[]{teaInfoCode, usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});

        if (null == teaMember || !teaMember.containsKey("tea_info_id")) {
            CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
            return;
        }
        int powern = teaMember.getInt("powern");
        JSONObject memberPage = teaService.getTeaMemberLimitPage(teaMember.getString("tea_info_id"), usersAccountOrName, page, limitNum);
        JSONArray memberList = new JSONArray();

        if (memberPage.containsKey("list")) {
            JSONArray newData = memberPage.getJSONArray("list");
            for (int i = 0; i < newData.size(); i++) {
                JSONObject member = newData.getJSONObject(i);
                boolean isUnder = true;
                String user_id = member.getString("users_id");
                if (TeaConstant.TEA_MEMBER_POWERN_PTCY == powern || usersId.equals(user_id)) {//普通成员or自己
                    isUnder = false;
                }
                boolean isOnline = redisInfoService.isOnline(user_id);

                member
                        .element("memberHeadimg", CommonConstant.getServerDomain() + member.get("memberHeadimg"))
                        .element("isOnline", isOnline)
                        .element("isUnder", isUnder);
                memberList.add(member);
            }
        }
        int auditMemberCount = 0;//待审核成员数量
        int quitMemberMsgCount = 0;//成员退出消息
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY != powern) {
            JSONObject memberCount = DBUtil.getObjectBySQL("SELECT COUNT(*)AS`count`,(SELECT COUNT(*)FROM tea_member_msg t WHERE t.`tea_info_id`=? AND t.`type`=? AND t.`is_deleted`=? AND t.`is_read`=?)AS quitCount FROM tea_member t WHERE t.`tea_info_id`=? AND t.`audit`=? AND t.`is_deleted`=?"
                    , new Object[]{teaMember.getString("tea_info_id"), TeaConstant.TEA_MEMBER_MSG_TYPE_TC, TeaConstant.IS_DELETE_N, TeaConstant.TEA_MEMBER_MSG_READ_N, teaMember.getString("tea_info_id"), TeaConstant.AUDIT_N, TeaConstant.IS_DELETE_N});
            if (null != memberCount && memberCount.containsKey("count") && memberCount.containsKey("quitCount")) {
                auditMemberCount = memberCount.getInt("count");
                quitMemberMsgCount = memberCount.getInt("quitCount");
            }
        }

        JSONObject result = new JSONObject();
        result
                .element("auditMemberCount", auditMemberCount)
                .element("quitMemberMsgCount", quitMemberMsgCount)
                .element("data", memberList)
                .element("pageIndex", memberPage.containsKey("pageIndex") ? memberPage.getInt("pageIndex") : 1)
                .element("pageSize", memberPage.containsKey("pageSize") ? memberPage.getInt("pageSize") : 6)
                .element("totalPage", memberPage.containsKey("totalPage") ? memberPage.getInt("totalPage") : 0)
                .element("totalCount", memberPage.containsKey("totalCount") ? memberPage.getInt("totalCount") : 0);
        result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        CommonConstant.sendMsgEvent(client, result, eventName);
    }


    public void auditMemberEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode", "audit", "memberId"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode,audit,memberId]", eventName);
            return;
        }
        String usersId = object.getString(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号
        String memberId = object.getString("memberId");//茶楼成员id
        int audit = object.getInt("audit");//审核
        if (TeaConstant.AUDIT_BTG != audit && TeaConstant.AUDIT_Y != audit && -1 != audit && -2 != audit) {
            CommonConstant.sendMsgEventNo(client, "审核类型错误", null, eventName);
            return;
        }

        //楼客权限 验证  start
        int powern = teaService.getTeaMemberPowern(usersId, teaInfoCode);//获取权限
        if (-1 == powern) {
            CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
            return;
        }
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY == powern) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }
        //楼客权限 验证  end
        if (-1 == audit || -2 == audit) {//全部清除or全部通过
            int auditAll = TeaConstant.AUDIT_Y;
            if (-1 == audit) {
                auditAll = TeaConstant.AUDIT_BTG;
            }
            int iAll = DBUtil.executeUpdateBySQL("UPDATE tea_member SET audit = ? WHERE tea_info_code = ? AND audit = ? AND is_deleted = ?"
                    , new Object[]{auditAll, teaInfoCode, TeaConstant.AUDIT_N, TeaConstant.IS_DELETE_N});

            if (iAll > 0) {
                CommonConstant.sendMsgEventYes(client, "操作成功", eventName);
                return;
            }
            CommonConstant.sendMsgEventNo(client, "操作失败", null, eventName);
            return;
        }

        JSONObject member = DBUtil.getObjectBySQL("SELECT t.`audit`,t.`is_deleted` FROM tea_member t WHERE t.`id` = ? AND t.`tea_info_code` = ?", new Object[]{memberId, teaInfoCode});
        if (null == member) {
            CommonConstant.sendMsgEventNo(client, "请刷新后再试", "请输入正确的memberId", eventName);
            return;
        }

        if (TeaConstant.AUDIT_Y == member.getInt("audit") && TeaConstant.IS_DELETE_N == member.getInt("is_deleted")) {
            CommonConstant.sendMsgEventNo(client, "该成员已经审核通过", null, eventName);
            return;
        }
        JSONObject o = new JSONObject();
        o
                .element("id", memberId)
                .element("audit", audit)
                .element("is_deleted", TeaConstant.IS_DELETE_N);
        int i = DBJsonUtil.update(o, "tea_member", "id");
        if (i > 0) {
            CommonConstant.sendMsgEventYes(client, "操作成功", eventName);
            return;
        }

        CommonConstant.sendMsgEventNo(client, "操作失败", null, eventName);

    }


    public void auditMemberListEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode]", eventName);
            return;
        }
        String usersId = object.getString(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号

        //楼客权限 验证  start
        int powern = teaService.getTeaMemberPowern(usersId, teaInfoCode);//获取权限
        if (-1 == powern) {
            CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
            return;
        }
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY == powern) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }
        //楼客权限 验证  end

        JSONArray memberList = DBUtil.getObjectListBySQL("SELECT z.`headimg`AS memberHeadimg,z.`name`AS memberName,z.`account`AS memberAccount,t.`id`AS memberId,t.`users_id` AS usersId, t.`powern` AS memberPowern  FROM tea_member t LEFT JOIN za_users z ON t.`users_id`=z.`id`WHERE t.`tea_info_code`= ? AND t.`audit`= ? AND t.`is_deleted`= ?  LIMIT 0,50"
                , new Object[]{teaInfoCode, TeaConstant.AUDIT_N, TeaConstant.IS_DELETE_N});
        JSONObject result = new JSONObject();
        for (int i = 0; i < memberList.size(); i++) {
            JSONObject member = memberList.getJSONObject(i);
            member
                    .element("memberHeadimg", CommonConstant.getServerDomain() + member.get("memberHeadimg"));
        }

        result.element("data", memberList);
        result.element("isAllButton", memberList == null ? 0 : memberList.size() == 0 ? 0 : 1);
        result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        CommonConstant.sendMsgEvent(client, result, eventName);
    }


    public void exitInfoListEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode]", eventName);
            return;
        }
        String usersId = object.getString(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号

        //楼客权限 验证  start
        int powern = teaService.getTeaMemberPowern(usersId, teaInfoCode);//获取权限
        if (-1 == powern) {
            CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
            return;
        }
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY == powern) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }


        //楼客权限 验证  end

        JSONArray teaMemberMsgArray = DBUtil.getObjectListBySQL("SELECT t.`id` AS memberMsgId,z.`account`AS memberAccount,z.`headimg`AS memberHeadimg,z.`name`AS memberName,f.`tea_name`AS teaName,f.`tea_code`AS teaInfoCode,f.`id`AS teaInfoId,t.`is_read`AS isRead,t.`create_time`AS createTime FROM tea_member_msg t LEFT JOIN za_users z ON t.`users_id`=z.`id`LEFT JOIN tea_info f ON t.`tea_info_id`=f.`id`WHERE t.`tea_info_code`= ? AND t.`type`= ? AND t.`is_deleted`= ? ORDER BY t.`create_time`DESC LIMIT 0,50"
                , new Object[]{teaInfoCode, TeaConstant.TEA_MEMBER_MSG_TYPE_TC, TeaConstant.IS_DELETE_N});

        JSONObject result = new JSONObject();
        for (int i = 0; i < teaMemberMsgArray.size(); i++) {
            JSONObject msg = teaMemberMsgArray.getJSONObject(i);
            TimeUtil.transTimeStamp(msg, "yyyy-MM-dd HH:mm:ss", "createTime");
            msg
                    .element("memberHeadimg", CommonConstant.getServerDomain() + msg.get("memberHeadimg"));
        }

        result.element("data", teaMemberMsgArray);
        result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        CommonConstant.sendMsgEvent(client, result, eventName);
    }


    public void adminListEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode]", eventName);
            return;
        }
        String usersId = object.getString(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号

        //楼客权限 验证  start
        int powern = teaService.getTeaMemberPowern(usersId, teaInfoCode);//获取权限
        if (-1 == powern) {
            CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
            return;
        }
        if (TeaConstant.TEA_MEMBER_POWERN_LZ != powern) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }
        //楼客权限 验证  end

        JSONArray teaMemberMsgArray = DBUtil.getObjectListBySQL("SELECT t.`users_id` AS usersId,z.`account`AS memberAccount,z.`headimg`AS memberHeadimg,z.`name`AS memberName,t.`id`AS memberId FROM tea_member t LEFT JOIN za_users z ON t.`users_id`=z.`id`WHERE t.`tea_info_code`=? AND t.`is_deleted`=? AND t.`powern`= ? AND t.`audit`=?"
                , new Object[]{teaInfoCode, TeaConstant.IS_DELETE_N, TeaConstant.TEA_MEMBER_POWERN_GLY, TeaConstant.AUDIT_Y});

        JSONObject result = new JSONObject();
        for (int i = 0; i < teaMemberMsgArray.size(); i++) {
            JSONObject msg = teaMemberMsgArray.getJSONObject(i);
            boolean isOnline = redisInfoService.isOnline(msg.getString("usersId"));
            msg
                    .element("memberHeadimg", CommonConstant.getServerDomain() + msg.get("memberHeadimg"))
                    .element("isOnline", isOnline)
            ;
        }

        result.element("data", teaMemberMsgArray);
        result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        CommonConstant.sendMsgEvent(client, result, eventName);
    }


    public void modifyAdminEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode", "type", "updateUsersAccount"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode,type]", eventName);
            return;
        }

        String usersId = object.getString(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号

        String type = object.getString("type");//操作类型
        String updateUsersAccount = object.getString("updateUsersAccount");
        if (!"0".equals(type) && !"1".equals(type)) {
            CommonConstant.sendMsgEventNo(client, "操作类型错误", "type 0 删除管理员 1 添加管理员", eventName);
            return;
        }
        JSONObject teaMember = DBUtil.getObjectBySQL("SELECT t.`id`,z.`account`,t.`powern` FROM tea_member t LEFT JOIN za_users z ON z.`id`=t.`users_id`WHERE z.`account`= ? AND t.`audit`= ? AND t.`is_deleted`= ? AND t.`tea_info_code`= ?"
                , new Object[]{updateUsersAccount, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N, teaInfoCode});
        if (null == teaMember || !teaMember.containsKey("id")) {
            CommonConstant.sendMsgEventNo(client, "该玩家还有没有加入", null, eventName);
            return;
        }
        if (!teaMember.containsKey("account")) {
            CommonConstant.sendMsgEventNo(client, "请输入正确的玩家ID", null, eventName);
            return;
        }

        if (TeaConstant.TEA_MEMBER_POWERN_LZ == teaMember.getInt("powern")) {
            CommonConstant.sendMsgEventNo(client, "你不能操作自己", null, eventName);
            return;
        }

        //楼客权限 验证  start
        int powern = teaService.getTeaMemberPowern(usersId, teaInfoCode);//获取权限
        if (-1 == powern) {
            CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
            return;
        }
        if (TeaConstant.TEA_MEMBER_POWERN_LZ != powern) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }
        //楼客权限 验证  end

        //更新数据
        powern = TeaConstant.TEA_MEMBER_POWERN_PTCY;
        if ("1".equals(type)) {
            powern = TeaConstant.TEA_MEMBER_POWERN_GLY;
        }
        if (powern == teaMember.getInt("powern")) {
            CommonConstant.sendMsgEventNo(client, "请刷新后再试", null, eventName);
            return;
        }
        JSONObject obj = new JSONObject();
        obj
                .element("powern", powern)
                .element("id", teaMember.get("id"))
        ;
        int i = DBJsonUtil.update(obj, "tea_member");
        if (i > 0) {
            CommonConstant.sendMsgEventYes(client, "操作成功", eventName);
            return;
        }
        CommonConstant.sendMsgEventNo(client, "操作失败", null, eventName);
    }


    public void myRecordListEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode]", eventName);
            return;
        }
        long usersId = object.getLong(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号
        //楼客权限 验证  start
        JSONObject teaMember = DBUtil.getObjectBySQL("SELECT z.`id`,z.`powern`,z.`tea_info_id` FROM tea_member z WHERE z.`tea_info_code`=? AND z.`users_id` = ? AND z.`audit` = ? AND z.`is_deleted` = ?"
                , new Object[]{teaInfoCode, usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
        if (null == teaMember) {
            CommonConstant.sendMsgEventNo(client, "您已经退出该茶楼", null, eventName);
            return;
        }

        //楼客权限 验证  end


        String time = DateUtils.searchTime(object.get("time"));//日期搜索
        int pageIndex = !object.containsKey("pageIndex") ? 1 : object.getInt("pageIndex");//第几页
        int pageSize = !object.containsKey("pageSize") ? 6 : object.getInt("pageSize");//一页几条

        Integer gameId = object.containsKey("gameId") ? object.getInt("gameId") : null;
        JSONObject logs = gameLogBiz.pageUserGameLogsByUserId(usersId, gameId, teaInfoCode, null, TeaConstant.ROOM_TYPE, time, pageIndex, pageSize);

        JSONArray gameLosArray = new JSONArray();
        if (logs.containsKey("list")) {
            JSONArray array = logs.getJSONArray("list");
            for (int i = 0; i < array.size(); i++) {
                JSONObject o = array.getJSONObject(i);
                if (null == o) continue;
                TimeUtil.transTimeStamp(o, "yyyy-MM-dd HH:mm:ss", "create_time");
                JSONObject userRes = new JSONObject();
                JSONArray gameList = DBUtil.getObjectListBySQL("SELECT * FROM za_user_game_statis z WHERE z.`room_id`= ? AND z.`room_no`= ?"
                        , new Object[]{o.get("room_id"), o.get("room_no")});
                if (null == gameList || gameList.size() == 0) continue;
                Collections.sort(gameList, new Comparator<JSONObject>() {
                    @Override
                    public int compare(JSONObject o1, JSONObject o2) {
                        double do1 = Dto.sub(o1.containsKey("score") ? o1.getDouble("score") : 0D, o1.containsKey("cut_score") ? o1.getDouble("cut_score") : 0D);
                        double do2 = Dto.sub(o2.containsKey("score") ? o2.getDouble("score") : 0D, o2.containsKey("cut_score") ? o2.getDouble("cut_score") : 0D);
                        if (do1 > do2)
                            return -1;
                        return 1;
                    }
                });

                JSONObject attachGameLog = null;
                JSONObject gameObject = gameList.getJSONObject(0);
                if (gameObject.containsKey("za_games_id") && CommonConstant.GAME_ID_QZMJ == gameObject.getInt("za_games_id")) {//目前只支持泉州麻将
                    if (gameObject.containsKey("sole")) {
                        attachGameLog = redisInfoService.getAttachGameLog(gameObject.get("sole"));
                    }
                }

                //房卡消耗获取
                JSONObject roomCard = teaService.getTeaRoomcardSpend(o.get("room_id"), o.get("room_no"), new Object[]{CommonConstant.ROOM_PAY_TYPE_TEA_AA}, usersId);
                JSONArray userResult = new JSONArray();
                for (int j = 0; j < gameList.size(); j++) {
                    JSONObject userObject = gameList.getJSONObject(j);
                    TimeUtil.transTimeStamp(userObject, "yyyy-MM-dd HH:mm:ss", "create_time");
                    String account = userObject.getString("account");
                    JSONObject temp = new JSONObject()
                            .element("createTime", userObject.get("create_time"))
                            .element("memberHeadimg", CommonConstant.getServerDomain() + userObject.get("headimg"))
                            .element("memberName", userObject.get("name"))
                            .element("memberScore", userObject.get("score"))
                            .element("memberAccount", account)
                            .element("gameId", userObject.get("za_games_id"));
                    if (null != attachGameLog && attachGameLog.containsKey(account)) {
                        temp.element("extraInfo", attachGameLog.getString(account));
                    }
                    userResult.add(temp);
                }
                userRes.
                        element("memberList", userResult)
                        .element("gameMumber", gameList.size())//人数
                        .element("gameSum", o.get("game_sum"))//小局
                        .element("gid", o.get("gid"))//小局
                        .element("roomNo", o.get("room_no"))
                        .element("roomId", o.get("room_id"))
                        .element("createTime", o.get("create_time"))
                        .element("roomType", o.get("room_type"));

                StringBuilder recordInfo = new StringBuilder();
                if (roomCard.containsKey("payType")) {
                    if (CommonConstant.ROOM_PAY_TYPE_TEA.equals(roomCard.getString("payType"))) {//茶壶
                        recordInfo.append(" 茶壶支付:").append(roomCard.get("recordInfo"));
                    } else if (CommonConstant.ROOM_PAY_TYPE_TEA_AA.equals(roomCard.getString("payType"))) {//AA
                        recordInfo.append(" 茶叶AA:").append(roomCard.get("recordInfo"));
                    }
                }
                userRes.element("recordInfo", recordInfo.toString());
                gameLosArray.add(userRes);
            }
        }
        //茶客战绩统计
        JSONObject memberRecord = teaService.getTeaMumberRecord(usersId, teaInfoCode, TeaConstant.ROOM_TYPE, time);
        if (null == memberRecord) memberRecord = new JSONObject();

        //总茶壶消耗
        JSONArray recordInfoArray = teaService.getTeaRoomcardAllSpend(teaMember.get("tea_info_id"), time, new Object[]{CommonConstant.ROOM_PAY_TYPE_TEA_AA}, usersId);

        String proxyMoneyALl = "0";
        String teaMoneyALl = "0";
        if (null != recordInfoArray && recordInfoArray.size() > 0) {
            for (int i = 0; i < recordInfoArray.size(); i++) {
                JSONObject record = recordInfoArray.getJSONObject(i);
                if (null == record) {
                    continue;
                }
                if (record.containsKey("payType")) {
                    if (CommonConstant.ROOM_PAY_TYPE_TEA.equals(record.getString("payType"))) {//茶壶
                        proxyMoneyALl = record.getString("teaMoneyAll");
                    } else if (CommonConstant.ROOM_PAY_TYPE_TEA_AA.equals(record.getString("payType"))) {//AA
                        teaMoneyALl = record.getString("teaMoneyAll");
                    }
                }
            }
        }
        JSONObject result = new JSONObject();
        result
                .element("teaMoneyALl", teaMoneyALl)
                .element("time", time)
                .element("data", gameLosArray)
                .element("pageIndex", logs.containsKey("pageIndex") ? logs.getInt("pageIndex") : 1)
                .element("pageSize", logs.containsKey("pageSize") ? logs.getInt("pageSize") : 6)
                .element("totalPage", logs.containsKey("totalPage") ? logs.getInt("totalPage") : 0)
                .element("totalCount", logs.containsKey("totalCount") ? logs.getInt("totalCount") : 0)
                .element("gameCount", memberRecord.containsKey("gameCount") ? memberRecord.get("gameCount") : 0)//总场次
                .element("allScore", memberRecord.containsKey("allScore") ? memberRecord.get("allScore") : 0)//总积分
                .element("winCount", memberRecord.containsKey("winCount") ? memberRecord.get("winCount") : 0)//赢局
                .element("tiesCount", memberRecord.containsKey("tiesCount") ? memberRecord.get("tiesCount") : 0)//平局
                .element("loseCount", memberRecord.containsKey("loseCount") ? memberRecord.get("loseCount") : 0)//输局
                .element("bigWinCount", memberRecord.containsKey("bigWinCount") ? memberRecord.get("bigWinCount") : 0)//大赢家
        ;
        result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        log.info("myRecordListEvent [{}]", result);
        CommonConstant.sendMsgEvent(client, result, eventName);
    }


    public void recordListEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode]", eventName);
            return;
        }
        long usersId = object.getLong(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号

        //楼客权限 验证  start
        JSONObject teaMember = DBUtil.getObjectBySQL("SELECT z.`id`,z.`powern`,z.`tea_info_id` FROM tea_member z WHERE z.`tea_info_code`=? AND z.`users_id` = ? AND z.`audit` = ? AND z.`is_deleted` = ?"
                , new Object[]{teaInfoCode, usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
        if (null == teaMember) {
            CommonConstant.sendMsgEventNo(client, "您已经退出该茶楼", null, eventName);
            return;
        }

        int powern = teaMember.getInt("powern");//获取权限
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY == powern) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }
        //楼客权限 验证  end
        Long userId = null;
        if (object.containsKey("usersAccount")) {//查询
            JSONObject user = DBUtil.getObjectBySQL("SELECT z.`id` FROM za_users z WHERE z.`account`=? "
                    , new Object[]{object.get("usersAccount")});
            if (null == user || !user.containsKey("id")) {
                CommonConstant.sendMsgEventNo(client, "请输入正确的玩家id", null, eventName);
                return;
            }
            userId = user.getLong("id");
        }

        String time = DateUtils.searchTime(object.get("time"));//日期搜索
        int pageIndex = !object.containsKey("pageIndex") ? 1 : object.getInt("pageIndex");//第几页
        int pageSize = !object.containsKey("pageSize") ? 6 : object.getInt("pageSize");//一页几条
        Integer gameId = object.containsKey("gameId") ? object.getInt("gameId") : null;
        JSONObject logs = gameLogBiz.pageUserGameLogsByUserId(userId, gameId, teaInfoCode, null, TeaConstant.ROOM_TYPE, time, pageIndex, pageSize);

        JSONArray gameLosArray = new JSONArray();
        if (logs.containsKey("list")) {
            JSONArray array = logs.getJSONArray("list");
            for (int i = 0; i < array.size(); i++) {
                JSONObject o = array.getJSONObject(i);
                if (null == o) continue;
                TimeUtil.transTimeStamp(o, "yyyy-MM-dd HH:mm:ss", "create_time");
                JSONObject userRes = new JSONObject();
                JSONArray gameList = DBUtil.getObjectListBySQL("SELECT * FROM za_user_game_statis z WHERE z.`room_id`= ? AND z.`room_no`= ?"
                        , new Object[]{o.get("room_id"), o.get("room_no")});
                if (null == gameList || gameList.size() == 0) continue;
                Collections.sort(gameList, new Comparator<JSONObject>() {
                    @Override
                    public int compare(JSONObject o1, JSONObject o2) {
                        double do1 = Dto.sub(o1.containsKey("score") ? o1.getDouble("score") : 0D, o1.containsKey("cut_score") ? o1.getDouble("cut_score") : 0D);
                        double do2 = Dto.sub(o2.containsKey("score") ? o2.getDouble("score") : 0D, o2.containsKey("cut_score") ? o2.getDouble("cut_score") : 0D);
                        if (do1 > do2)
                            return -1;
                        return 1;
                    }
                });

                JSONObject attachGameLog = null;
                JSONObject gameObject = gameList.getJSONObject(0);
                if (gameObject.containsKey("za_games_id") && CommonConstant.GAME_ID_QZMJ == gameObject.getInt("za_games_id")) {//目前只支持泉州麻将
                    if (gameObject.containsKey("sole")) {
                        attachGameLog = redisInfoService.getAttachGameLog(gameObject.get("sole"));
                    }
                }

                //房卡消耗获取
                JSONObject roomCard = teaService.getTeaRoomcardSpend(o.get("room_id"), o.get("room_no"), null, null);
                JSONArray userResult = new JSONArray();
                for (int j = 0; j < gameList.size(); j++) {
                    JSONObject userObject = gameList.getJSONObject(j);
                    TimeUtil.transTimeStamp(userObject, "yyyy-MM-dd HH:mm:ss", "create_time");
                    String account = userObject.getString("account");
                    JSONObject temp = new JSONObject()
                            .element("createTime", userObject.get("create_time"))
                            .element("memberHeadimg", CommonConstant.getServerDomain() + userObject.get("headimg"))
                            .element("memberName", userObject.get("name"))
                            .element("memberScore", userObject.get("score"))
                            .element("memberAccount", account)
                            .element("gameId", userObject.get("za_games_id"));
                    if (null != attachGameLog && attachGameLog.containsKey(account)) {
                        temp.element("extraInfo", attachGameLog.getString(account));
                    }
                    userResult.add(temp);
                }
                userRes
                        .element("time", time)
                        .element("memberList", userResult)
                        .element("gameMumber", gameList.size())//人数
                        .element("gameSum", o.get("game_sum"))//小局
                        .element("gid", o.get("gid"))//小局
                        .element("roomNo", o.get("room_no"))
                        .element("roomId", o.get("room_id"))
                        .element("createTime", o.get("create_time"))
                        .element("roomType", o.get("room_type"))
                ;
                StringBuilder recordInfo = new StringBuilder();
                if (roomCard.containsKey("payType")) {
                    if (CommonConstant.ROOM_PAY_TYPE_TEA.equals(roomCard.getString("payType"))) {//茶壶
                        recordInfo.append(" 茶壶支付:").append(roomCard.get("recordInfo"));
                    } else if (CommonConstant.ROOM_PAY_TYPE_TEA_AA.equals(roomCard.getString("payType"))) {//AA
                        recordInfo.append(" 茶叶AA:").append(roomCard.get("recordInfo"));
                    }
                }
                userRes.element("recordInfo", recordInfo.toString());

                gameLosArray.add(userRes);
            }
        }
        //总茶壶消耗
        JSONArray recordInfoArray = teaService.getTeaRoomcardAllSpend(teaMember.get("tea_info_id"), time, null, null);

        String proxyMoneyALl = "0";
        String teaMoneyALl = "0";
        if (null != recordInfoArray && recordInfoArray.size() > 0) {
            for (int i = 0; i < recordInfoArray.size(); i++) {
                JSONObject record = recordInfoArray.getJSONObject(i);
                if (null == record) {
                    continue;
                }
                if (record.containsKey("payType")) {
                    if (CommonConstant.ROOM_PAY_TYPE_TEA.equals(record.getString("payType"))) {//茶壶
                        proxyMoneyALl = record.getString("teaMoneyAll");
                    } else if (CommonConstant.ROOM_PAY_TYPE_TEA_AA.equals(record.getString("payType"))) {//AA
                        teaMoneyALl = record.getString("teaMoneyAll");
                    }
                }
            }
        }

        JSONObject result = new JSONObject();
        result
                .element("data", gameLosArray)
                .element("proxyMoneyALl", proxyMoneyALl)//茶壶总
                .element("teaMoneyALl", teaMoneyALl)//茶叶总
                .element("pageIndex", logs.containsKey("pageIndex") ? logs.getInt("pageIndex") : 1)
                .element("pageSize", logs.containsKey("pageSize") ? logs.getInt("pageSize") : 6)
                .element("totalPage", logs.containsKey("totalPage") ? logs.getInt("totalPage") : 0)
                .element("totalCount", logs.containsKey("totalCount") ? logs.getInt("totalCount") : 0)
        ;
        result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        CommonConstant.sendMsgEvent(client, result, eventName);
    }


    public void recordStatisListEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode]", eventName);
            return;
        }
        String usersId = object.getString(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//当前所在的茶楼号

        JSONObject teaInfo = DBUtil.getObjectBySQL("SELECT t.`id`,t.`is_record` FROM tea_info t WHERE t.`tea_code`= ? AND t.`audit`= ? AND t.`is_deleted`= ?"
                , new Object[]{teaInfoCode, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
        if (null == teaInfo || !teaInfo.containsKey("is_record")) {
            CommonConstant.sendMsgEventNo(client, "该茶楼不存在", null, eventName);
            return;
        }
        boolean isUnder = false;//是否有  允许所有玩家查看按钮
        if (TeaConstant.TEA_INFO_RECORD_N == teaInfo.getInt("is_record")) {
            //楼客权限 验证  start
            int powern = teaService.getTeaMemberPowern(usersId, teaInfoCode);//获取权限
            if (-1 == powern) {
                CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
                return;
            }
            if (TeaConstant.TEA_MEMBER_POWERN_PTCY == powern) {
                CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
                return;
            }
            //楼客权限 验证  end
            isUnder = true;
        }

        String usersAccountOrName = object.containsKey("usersAccountOrName") ? object.getString("usersAccountOrName") : null;//楼客搜索
        String time = DateUtils.searchTime(object.get("time"));//日期搜索
        int sequence = object.containsKey("sequence") ? object.getInt("sequence") : TeaConstant.BATTLE_LIST_SEQUENCE_MAX;//战榜列表排序
        int page = object.get("pageIndex") == null ? 1 : object.getInt("pageIndex");
        int limitNum = object.get("pageSize") == null ? 6 : object.getInt("pageSize");

        JSONObject memberPage = teaService.pageTeaMumberRecord(teaInfoCode, usersAccountOrName, TeaConstant.ROOM_TYPE, time, sequence, page, limitNum);

        JSONArray memberList = new JSONArray();
        if (memberPage.containsKey("list")) {
            JSONArray newData = memberPage.getJSONArray("list");
            for (int i = 0; i < newData.size(); i++) {
                JSONObject member = newData.getJSONObject(i);
                TimeUtil.transTimeStamp(member, "yyyy-MM-dd HH:mm:ss", "createTime");
                member
                        .element("memberHeadimg", CommonConstant.getServerDomain() + member.get("memberHeadimg"))
                ;
                memberList.add(member);
            }
        }

        JSONObject result = new JSONObject();
        //我的得分排名处理 start
        JSONObject o = teaService.getMemberRecord(teaInfoCode, usersId, TeaConstant.ROOM_TYPE, time);
        result.putAll(o);
        //我的得分排名处理
        result
                .element("time", time)
                .element("data", memberList)
                .element("isUnder", isUnder)
                .element("pageIndex", memberPage.containsKey("pageIndex") ? memberPage.getInt("pageIndex") : 1)
                .element("pageSize", memberPage.containsKey("pageSize") ? memberPage.getInt("pageSize") : 6)
                .element("totalPage", memberPage.containsKey("totalPage") ? memberPage.getInt("totalPage") : 0)
                .element("totalCount", memberPage.containsKey("totalCount") ? memberPage.getInt("totalCount") : 0)
        ;
        result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        CommonConstant.sendMsgEvent(client, result, eventName);
    }


    public void bigWinnerListEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode]", eventName);
            return;
        }
        String usersId = object.getString(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//当前所在的茶楼号

        JSONObject teaInfo = DBUtil.getObjectBySQL("SELECT t.`id` FROM tea_info t WHERE t.`tea_code`= ? AND t.`audit`= ? AND t.`is_deleted`= ?"
                , new Object[]{teaInfoCode, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
        if (null == teaInfo) {
            CommonConstant.sendMsgEventNo(client, "该茶楼不存在", null, eventName);
            return;
        }

        String usersAccountOrName = object.containsKey("usersAccountOrName") ? object.getString("usersAccountOrName") : null;//楼客搜索
        String time = DateUtils.searchTime(object.get("time"));//日期搜索
        int page = object.get("pageIndex") == null ? 1 : object.getInt("pageIndex");
        int limitNum = object.get("pageSize") == null ? 6 : object.getInt("pageSize");

        JSONObject memberPage = teaService.pageTeaMumberBigWinRecord(teaInfoCode, usersAccountOrName, TeaConstant.ROOM_TYPE, time, page, limitNum);

        JSONArray memberList = new JSONArray();
        if (memberPage.containsKey("list")) {
            JSONArray newData = memberPage.getJSONArray("list");
            for (int i = 0; i < newData.size(); i++) {
                JSONObject member = newData.getJSONObject(i);
                member
                        .element("memberHeadimg", CommonConstant.getServerDomain() + member.get("memberHeadimg"))
                        .element("bigWinCount", member.get("bigWinCount"))
                ;
                memberList.add(member);
            }
        }

        JSONObject result = new JSONObject();
        //我的得分排名处理 start
        JSONObject o = teaService.getMemberBigWinRecord(teaInfoCode, usersId, TeaConstant.ROOM_TYPE, time);
        result.putAll(o);
        //我的得分排名处理 end
        result
                .element("time", time)
                .element("data", memberList)
                .element("pageIndex", memberPage.containsKey("pageIndex") ? memberPage.getInt("pageIndex") : 1)
                .element("pageSize", memberPage.containsKey("pageSize") ? memberPage.getInt("pageSize") : 6)
                .element("totalPage", memberPage.containsKey("totalPage") ? memberPage.getInt("totalPage") : 0)
                .element("totalCount", memberPage.containsKey("totalCount") ? memberPage.getInt("totalCount") : 0)
        ;
        result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        log.info("20.茶楼大赢家列表 [{}]", result);
        CommonConstant.sendMsgEvent(client, result, eventName);
    }

    public void quickJoinEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode", "uuid", CommonConstant.DATA_KEY_ACCOUNT})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode,uuid,account]", eventName);
            return;
        }
        String usersId = object.getString(CommonConstant.USERS_ID);//用户id
        String account = object.getString(CommonConstant.DATA_KEY_ACCOUNT);//用户id
        //游戏停服中
        if (!redisInfoService.getStartStatus(null, account)) {
            CommonConstant.sendMsgEventNo(client, CommonConstant.CLOSING_DOWN_MSG, null, eventName);
            return;
        }
        String teaInfoCode = object.getString("teaInfoCode");//当前所在的茶楼号
        Integer gid = object.containsKey("gid") ? object.getInt("gid") : null;
        for (String room_No : RoomManage.gameRoomMap.keySet()) {
            GameRoom room = RoomManage.gameRoomMap.get(room_No);
            if (null == room) continue;
            //如果在房间里排除
            if (room.getPlayerMap().containsKey(account) && null != room.getPlayerMap().get(account)) {
                CommonConstant.sendMsgEventYes(client, "您已经在房间中...", eventName);
                return;
            }
        }
        boolean b = false;
        String roomNo = "";
        for (String room_No : RoomManage.gameRoomMap.keySet()) {

            GameRoom room = RoomManage.gameRoomMap.get(room_No);
            if (null == room) continue;
            if (null != gid && gid != room.getGid()) {
                continue;
            }
            //茶楼
            if (CommonConstant.ROOM_TYPE_TEA != room.getRoomType())
                continue;
            //茶楼号
            if (!teaInfoCode.equals(room.getClubCode())) continue;
            //是否人未满房间
            if (room.getPlayerMap().size() >= room.getPlayerCount()) continue;

            //茶楼余额 验证  start
            int joinRoomType = teaService.verifyJoinRoom(room.getSinglePayNum(), room.getPlayerCount(), room.getPayType(), teaInfoCode, usersId);
            if (TeaConstant.JOIN_ROOM_TYPE_NO == joinRoomType) {
                CommonConstant.sendMsgEventNo(client, "您已经退出该茶楼", null, eventName);
                return;
            }
            //黑名单校验
            if (TeaConstant.JOIN_ROOM_TYPE_BLACK_LIST == joinRoomType) {
                CommonConstant.sendMsgEventNo(client, TeaConstant.NOT_USE_MSG, null, eventName);
                return;
            }
            if (TeaConstant.JOIN_ROOM_TYPE_SCORE_LOCK_NO == joinRoomType) {
                CommonConstant.sendMsgEventNo(client, "您当日已达到负分限制，无法继续游戏，请先联系管理员", null, eventName);
                return;
            }

            if (TeaConstant.JOIN_ROOM_TYPE_NO_1 == joinRoomType) {
                String msg = "茶楼茶壶不足";
                if (CommonConstant.ROOM_PAY_TYPE_TEA_AA.equals(room.getPayType())) {//4:茶叶AA"
                    msg = "您的茶叶不足";
                }
                CommonConstant.sendMsgEventNo(client, msg, null, eventName);
                return;
            }
            //茶楼余额 验证  end

            b = true;//找到房间
            roomNo = room_No;
            break;

        }

        if (!b || "".equals(roomNo)) {
            CommonConstant.sendMsgEventNo(client, "当前没有房间可以加入,请稍后再试", null, eventName);
            return;
        }

        log.info("account:[{}]在茶楼快速加入找到房间：[{}]", account, roomNo);
        JSONObject postData = new JSONObject();
        postData.put(CommonConstant.DATA_KEY_ROOM_NO, roomNo);
        postData.put(CommonConstant.DATA_KEY_ACCOUNT, account);
        postData.put("uuid", object.get("uuid"));
        if (object.containsKey("location")) {
            postData.put("location", object.get("location"));
        }
        baseEventDeal.joinRoomBase(client, postData);

        CommonConstant.sendMsgEventYes(client, "房间匹配中,请稍候...", eventName);
    }


    public void infoEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode]", eventName);
            return;
        }
        String usersId = object.getString(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号
        //楼客权限 验证  start
        JSONObject teaInfo = DBUtil.getObjectBySQL("SELECT t.`tea_name`,t.`tea_code`,t.`id`,t.`is_record`,f.`powern` FROM tea_info t LEFT JOIN tea_member f on t.`id`=f.`tea_info_id` WHERE t.`tea_code`= ? AND t.`audit`= ? AND t.`is_deleted`= ? AND f.`users_id`= ? AND f.`audit` = ? AND f.`is_deleted`= ?"
                , new Object[]{teaInfoCode, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N, usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
        if (null == teaInfo || !teaInfo.containsKey("is_record") || !teaInfo.containsKey("powern")) {
            CommonConstant.sendMsgEventNo(client, "该茶楼不存在", null, eventName);
            return;
        }
        int powern = teaInfo.getInt("powern");//获取权限
        if (-1 == powern) {
            CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
            return;
        }
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY == powern) {
            JSONObject result = new JSONObject();
            result.element("msg", "您没有权限");
            result.element("teaName", teaInfo.get("tea_name"));
            result.element("teaCode", teaInfo.get("tea_code"));
            result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            CommonConstant.sendMsgEvent(client, result, eventName);
            return;
        }
        //楼客权限 验证  end
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT z.`proxy_money`AS proxyMoney,z.`tea_code`AS teaInfoCode,z.`type`AS teaInfoType,z.`tea_name`AS teaName,z.`base_info`,f.`powern`AS memberPowern,")
                .append("(SELECT COUNT(*)FROM tea_member t WHERE t.`tea_info_id`=? AND t.`audit`=? AND t.`is_deleted`=?)AS number FROM tea_info z LEFT JOIN ")
                .append("tea_member f ON z.`id`=f.`tea_info_id`WHERE f.`users_id`=? AND z.`id`=? AND z.`audit`=? AND z.`is_deleted`=? AND f.`audit`=? AND f.`is_deleted`=? ");
        teaInfo = DBUtil.getObjectBySQL(sb.toString()
                , new Object[]{teaInfo.get("id"), TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N, usersId, teaInfo.get("id"), TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});

        if (null != teaInfo && teaInfo.containsKey("base_info")) {
            JSONObject baseInfo = teaInfo.getJSONObject("base_info");//房间信息
            teaInfo.element("gameInfo", teaService.getRoomGameInfo(baseInfo));
        }

        JSONObject result = new JSONObject();
        result.element("data", teaInfo);
        result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        CommonConstant.sendMsgEvent(client, result, eventName);
    }

    public void exitMemberEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode]", eventName);
            return;
        }
        String usersId = object.getString(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号
        //楼客权限 验证  start
        JSONObject teaMember = DBUtil.getObjectBySQL("SELECT z.`id`,z.`powern`,z.`tea_info_id` FROM tea_member z WHERE z.`tea_info_code`=? AND z.`users_id` = ? AND z.`audit` = ? AND z.`is_deleted` = ?"
                , new Object[]{teaInfoCode, usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
        if (null == teaMember) {
            CommonConstant.sendMsgEventNo(client, "您已经退出该茶楼", null, eventName);
            return;
        }
        int powern = teaMember.getInt("powern");//获取权限
        if (TeaConstant.TEA_MEMBER_POWERN_LZ == powern) {
            CommonConstant.sendMsgEventNo(client, "茶楼不能没有主人", null, eventName);
            return;
        }

        if (TeaConstant.TEA_MEMBER_POWERN_GLY == powern) {
            CommonConstant.sendMsgEventNo(client, "请先解除管理员身份", null, eventName);
            return;
        }
        //楼客权限 验证  end
        JSONObject o = new JSONObject();
        o
                .element("is_deleted", TeaConstant.IS_DELETE_Y)
                .element("users_id", usersId)
                .element("tea_info_code", teaInfoCode);
        int i = DBJsonUtil.update(o, "tea_member", new String[]{"users_id", "tea_info_code"});
        if (i > 0) {
            //成员退出记录

            CommonConstant.sendMsgEventYes(client, "操作成功", eventName);

            JSONObject msg = new JSONObject();
            msg
                    .element("memberId", teaMember.get("id"))
                    .element("type", TeaConstant.TEA_MEMBER_MSG_TYPE_TC);
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_TEA_MEMBER_MSG, msg));//茶楼消息通知
            return;
        }
        CommonConstant.sendMsgEventNo(client, "操作失败", null, eventName);
    }


    public void modifyInfoRecordEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaRecordType", "teaInfoCode"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaRecordType,teaInfoCode]", eventName);
            return;
        }
        long usersId = object.getLong(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号
        int teaRecordType = object.getInt("teaRecordType");//普通成员站榜查看权限
        if (TeaConstant.TEA_INFO_RECORD_N != teaRecordType && TeaConstant.TEA_INFO_RECORD_Y != teaRecordType) {
            CommonConstant.sendMsgEventNo(client, "请刷新后再试", "teaRecordType参数错误", eventName);
            return;
        }
        //楼客权限 验证  start
        int powern = teaService.getTeaMemberPowern(usersId, teaInfoCode);//获取权限
        if (-1 == powern) {
            CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
            return;
        }
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY == powern) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }
        //楼客权限 验证  end
        JSONObject teaInfo = DBUtil.getObjectBySQL("SELECT t.`id`,t.`is_record` FROM tea_info t WHERE t.`tea_code`= ? AND t.`audit`= ? AND t.`is_deleted`= ? "
                , new Object[]{teaInfoCode, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});

        if (null != teaInfo && teaInfo.containsKey("is_record") && teaInfo.containsKey("id")) {
            int isRecord = teaInfo.getInt("is_record");
            if (teaRecordType == isRecord) {
                CommonConstant.sendMsgEventNo(client, "请刷新后再试", "已经操作成功", eventName);
                return;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.element("id", teaInfo.get("id")).element("is_record", teaRecordType);
            int i = DBJsonUtil.update(jsonObject, "tea_info", "id");
            if (i > 0) {
                CommonConstant.sendMsgEventYes(client, "操作成功", eventName);
                return;
            }
        }
        CommonConstant.sendMsgEventNo(client, "操作失败", null, eventName);
    }


    public void roomListEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, CommonConstant.DATA_KEY_ACCOUNT, "teaInfoCode"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,account,teaInfoCode]", eventName);
            return;
        }
        String account = object.getString("account");//用户id
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号

        JSONArray allRoom = new JSONArray();
        GameRoom room;
        JSONObject obj;
        int used = 0;//已使用房间个数
        int unused = 0;//未使用房间个数

        for (String roomNo : RoomManage.gameRoomMap.keySet()) {
            if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
                room = RoomManage.gameRoomMap.get(roomNo);
                if (room == null) continue;
                //是否开发
                if (!room.isOpen()) continue;
                //俱乐部
                if (!teaInfoCode.equals(room.getClubCode())) continue;
                //茶楼
                if (room.getRoomType() != CommonConstant.ROOM_TYPE_TEA)
                    continue;

                obj = new JSONObject();
                int isJoinRoom = 0;//是否在该房间里
                for (String uuid : room.getPlayerMap().keySet()) {
                    if (account.equals(uuid)) {
                        isJoinRoom = 1;
                        break;
                    }
                }
                obj.element("isJoinRoom", isJoinRoom);//是否在该房间里
                obj.element("roomNo", room.getRoomNo());//房间号
                obj.element("gid", room.getGid());//游戏id
                obj.element("base_info", room.getRoomInfo());//游戏信息
                obj.element("playerCount", room.getPlayerCount());//房间人数
                obj.element("joinNumber", room.getPlayerMap().size());//玩家人数
                obj.element("gameIndex", room.getGameIndex());//当前局数
                obj.element("gameCount", room.getGameCount());//游戏总局数
                obj.element("userList", room.getPlayerMap());//房间用户信息
                allRoom.add(obj);

                if (room.getPlayerMap().size() == room.getPlayerCount() || room.getGameIndex() > 1) {//满人房间
                    used++;
                } else {
                    unused++;
                }
            }
        }
        object.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        object.element("data", allRoom);
        CommonConstant.sendMsgEvent(client, object, eventName);
        int roomCount = used + unused;//总的房间数量
        //是否添加房间
        if (roomCount < TeaConstant.MIN_ROOT
                || (roomCount >= TeaConstant.MIN_ROOT && unused < TeaConstant.MIN_ROOT_COUNT)) {
            if (!redisInfoService.teaRoomOpen(teaInfoCode)) {//茶楼是否开启
                return;
            }
            if (redisInfoService.isExistCreateRoom(teaInfoCode, true)) {//是否已经在创建中。。。
                return;
            }
            JSONObject addTeaRoom = new JSONObject();
            addTeaRoom
                    .element("used", used)
                    .element("unused", unused)
                    .element("teaInfoCode", teaInfoCode)
            ;
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.ADD_TEA_ROOM, addTeaRoom));
        }
        //是否添加房间
    }


    public void removeRoomEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, CommonConstant.ROOM_NO, "teaInfoCode"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,roomNo,teaInfoCode]", eventName);
            return;
        }
        long usersId = object.getLong(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号
        String roomNo = object.getString(CommonConstant.ROOM_NO);//房间号
        GameRoom room = RoomManage.gameRoomMap.get(roomNo);
        if (null == room || !teaInfoCode.equals(room.getClubCode())) {
            CommonConstant.sendMsgEventNo(client, "房间不存在", null, eventName);
            return;
        }
        //楼客权限 验证  start
        int powern = teaService.getTeaMemberPowern(usersId, teaInfoCode);//获取权限
        if (-1 == powern) {
            CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
            return;
        }
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY == powern) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }
        //楼客权限 验证  end

        //房间处于初始状态和准备状态才能解散
        if (room.getGameStatus() != 0 && room.getGameIndex() > 0) {
            JSONObject data = new JSONObject();
            data.put(CommonConstant.DATA_KEY_ROOM_NO, roomNo);
            //十三水房间解散处理 start
            if (CommonConstant.GAME_ID_SSS == room.getGid()) {
                producerService.sendMessage(sssQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_SSS, SSSConstant.SSS_GAME_EVENT_FORCED_ROOM));
                //通知解散者
                CommonConstant.sendMsgEventYes(client, "解散成功", eventName);
                return;
            }
            //十三水房间解散处理 end
            // 跑得快房间解散处理 start
            if (CommonConstant.GAME_ID_PDK == room.getGid()) {
                producerService.sendMessage(pdkQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_PDK, PDKEventEnum.PDK_GAME_FORCED_ROOM_EVENT.getType()));
                //通知解散者
                CommonConstant.sendMsgEventYes(client, "解散成功", eventName);
                return;
            }
            //跑得快房间解散处理 end
            // 泉州麻将房间解散处理 start
            if (CommonConstant.GAME_ID_QZMJ == room.getGid()) {
                producerService.sendMessage(qzmjQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_QZMJ, QZMJConstant.QZMJ_GAME_EVENT_FORCED_ROOM));
                //通知解散者
                CommonConstant.sendMsgEventYes(client, "解散成功", eventName);
                return;
            }
            //泉州麻将房间解散处理 end
            CommonConstant.sendMsgEventNo(client, "游戏中的房间不能解散", null, eventName);
            return;
        }
        //通知房间内玩家
        List<UUID> uuidList = room.getAllUUIDList();
        // 移除房间
        RoomManage.gameRoomMap.remove(roomNo);
        // 通知玩家
        JSONObject result = new JSONObject();
        result.element("type", CommonConstant.SHOW_MSG_TYPE_BIG);
        result.element(CommonConstant.RESULT_KEY_MSG, "房间已解散");
        CommonConstant.sendMsgEventAll(uuidList, result, "tipMsgPush");
        //更新数据库房间
        JSONObject roomInfo = new JSONObject();
        roomInfo.put("room_no", room.getRoomNo());
        roomInfo.put("status", CommonConstant.CLOSE_ROOM_TYPE_FINISH);
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
        //通知解散者
        CommonConstant.sendMsgEventYes(client, "解散成功", eventName);
    }


    public void delMemberEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode", "delUsersAccount"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode,delUsersAccount]", eventName);
            return;
        }
        long usersId = object.getLong(CommonConstant.USERS_ID);//用户id
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号
        String delUsersAccount = object.getString("delUsersAccount");//删除成员的account

        //验证  邀请玩家
        JSONObject addUser = DBUtil.getObjectBySQL("SELECT z.`id`,z.`platform` FROM za_users z WHERE z.`account`= ? ", new Object[]{delUsersAccount});
        if (null == addUser) {
            CommonConstant.sendMsgEventNo(client, "请输入正确的玩家ID", null, eventName);
            return;
        }

        if (usersId == addUser.getLong("id")) {
            CommonConstant.sendMsgEventNo(client, "不能删除自己", null, eventName);
            return;
        }
        JSONObject delMember = DBUtil.getObjectBySQL("SELECT z.`powern`,z.`id`,z.`audit`,z.`is_deleted` FROM tea_member z WHERE z.`tea_info_code`= ? AND z.`users_id` = ?"
                , new Object[]{teaInfoCode, addUser.get("id")});
        if (null == delMember) {
            CommonConstant.sendMsgEventNo(client, "该玩家不在茶楼里", null, eventName);
            return;
        }

        if (TeaConstant.IS_DELETE_Y == delMember.getInt("is_deleted")) {//未删除
            CommonConstant.sendMsgEventNo(client, "该玩家已经删除", null, eventName);
            return;
        }

        if (TeaConstant.TEA_MEMBER_POWERN_LZ == delMember.getInt("powern")) {//楼主不能删除
            CommonConstant.sendMsgEventNo(client, "楼主不能删除", null, eventName);
            return;
        }

        //验证  操作玩家
        JSONObject teaMember = DBUtil.getObjectBySQL("SELECT z.`powern`,z.`tea_info_id` FROM tea_member z WHERE z.`tea_info_code`=? AND z.`users_id` = ? AND z.`audit` = ? AND z.`is_deleted` = ?"
                , new Object[]{teaInfoCode, usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});

        if (null == teaMember) {
            CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
            return;
        }
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY == teaMember.getInt("powern")) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }

        //保存玩家
        JSONObject updateMember = new JSONObject();
        updateMember
                .element("is_deleted", TeaConstant.IS_DELETE_Y)
                .element("id", delMember.get("id"))
        ;
        int i = DBJsonUtil.update(updateMember, "tea_member");
        if (i > 0) {
            CommonConstant.sendMsgEventYes(client, "操作成功", eventName);
            return;
        }
        CommonConstant.sendMsgEventNo(client, "操作失败", null, eventName);

    }

    public void joinRoomEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, CommonConstant.DATA_KEY_ACCOUNT, "teaInfoCode", "roomNo"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,account,teaInfoCode,roomNo]", eventName);
            return;
        }
        String usersId = object.getString(CommonConstant.USERS_ID);//用户id
        String account = object.getString(CommonConstant.DATA_KEY_ACCOUNT);//用户account
        //游戏停服中
        if (!redisInfoService.getStartStatus(null, account)) {
            CommonConstant.sendMsgEventNo(client, CommonConstant.CLOSING_DOWN_MSG, null, eventName);
            return;
        }
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号
        String roomNo = object.getString("roomNo");//房间号

        GameRoom room = RoomManage.gameRoomMap.get(roomNo);
        if (null == room || !teaInfoCode.equals(room.getClubCode()) || CommonConstant.ROOM_TYPE_TEA != room.getRoomType()) {
            CommonConstant.sendMsgEventNo(client, "房间不存在", null, eventName);
            return;
        }
        if (room.getPlayerCount() <= room.getPlayerMap().size()) {
            CommonConstant.sendMsgEventNo(client, "房间人已满", null, eventName);
            return;
        }

        //茶楼余额 验证  start
        int joinRoomType = teaService.verifyJoinRoom(room.getSinglePayNum(), room.getPlayerCount(), room.getPayType(), teaInfoCode, usersId);
        if (TeaConstant.JOIN_ROOM_TYPE_NO == joinRoomType) {
            CommonConstant.sendMsgEventNo(client, "您已经退出该茶楼", null, eventName);
            return;
        }
        //黑名单
        if (TeaConstant.JOIN_ROOM_TYPE_BLACK_LIST == joinRoomType) {
            CommonConstant.sendMsgEventNo(client, TeaConstant.NOT_USE_MSG, null, eventName);
            return;
        }
        if (TeaConstant.JOIN_ROOM_TYPE_SCORE_LOCK_NO == joinRoomType) {
            CommonConstant.sendMsgEventNo(client, "您当日已达到负分限制，无法继续游戏，请先联系管理员", null, eventName);
            return;
        }

        if (TeaConstant.JOIN_ROOM_TYPE_NO_1 == joinRoomType) {
            String msg = "茶楼茶壶不足";
            if (CommonConstant.ROOM_PAY_TYPE_TEA_AA.equals(room.getPayType())) {//4:茶叶AA"
                msg = "您的茶叶不足";
            }
            CommonConstant.sendMsgEventNo(client, msg, null, eventName);
            return;
        }
        //茶楼余额 验证  end


        //验证用户是否有在其他房间里
        for (String room_No : RoomManage.gameRoomMap.keySet()) {
            GameRoom gameRoom = RoomManage.gameRoomMap.get(room_No);
            if (null == gameRoom || CommonConstant.ROOM_TYPE_TEA != gameRoom.getRoomType()) continue;
            if (!gameRoom.getPlayerMap().containsKey(account) || null == gameRoom.getPlayerMap().get(account)) continue;
            if (!roomNo.equals(room_No)) {
                if (0 == gameRoom.getGameIndex() && room.getPlayerCount() > room.getPlayerMap().size()) {
                    removeRoomAccount(room_No, account);//移除房间用户
                    break;
                } else {
                    CommonConstant.sendMsgEventNo(client, "游戏已经开始", null, eventName);
                    return;
                }

            } else {
                CommonConstant.sendMsgEventNo(client, "你已经在该房间", null, eventName);
                return;
            }
        }

        JSONObject postData = new JSONObject();
        postData.put(CommonConstant.DATA_KEY_ROOM_NO, roomNo);
        postData.put(CommonConstant.DATA_KEY_ACCOUNT, account);
        postData.put("uuid", object.get("uuid"));
        if (object.containsKey("location")) {
            postData.put("location", object.get("location"));
        }
        baseEventDeal.joinRoomBase(client, postData);
        CommonConstant.sendMsgEventYes(client, "加入成功", eventName);
    }

    public void quitRoomEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.DATA_KEY_ACCOUNT, "teaInfoCode", "roomNo"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode,roomNo]", eventName);
            return;
        }
        String account = object.getString(CommonConstant.DATA_KEY_ACCOUNT);//用户account
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号
        String roomNo = object.getString("roomNo");//房间号

        GameRoom room = RoomManage.gameRoomMap.get(roomNo);
        if (room == null || !teaInfoCode.equals(room.getClubCode())) {
            CommonConstant.sendMsgEventNo(client, "房间不存在", null, eventName);
            return;
        }

        if (room.getGameIndex() != 0 && room.getGameStatus() != 0) {
            CommonConstant.sendMsgEventNo(client, "游戏已经开始", null, eventName);
            return;
        }
        if (room.getPlayerCount() <= room.getPlayerMap().size()) {
            CommonConstant.sendMsgEventNo(client, "游戏已经开始", null, eventName);
            return;
        }
        removeRoomAccount(roomNo, account);
        CommonConstant.sendMsgEventYes(client, "退出成功", eventName);
    }


    public void recordInfoEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "roomId", "teaInfoCode", "roomNo"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,roomId,teaInfoCode,roomNo]", eventName);
            return;
        }
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号
        String usersId = object.getString(CommonConstant.USERS_ID);
        //楼客权限 验证  start
        int powern = teaService.getTeaMemberPowern(usersId, teaInfoCode);//获取权限
        if (-1 == powern) {
            CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
            return;
        }
        //楼客权限 验证  end

        //房间号
        String roomNo = object.getString("roomNo");
        long roomId = object.getLong("roomId");
        //用户战绩
        String sql = "SELECT `create_time` AS createTime,`id`,`room_no`,`result`,`gamelog_id`,`game_index`,`room_type` FROM `za_users_game_logs_result` where `room_no` = ? and `room_id` = ? ORDER BY `game_index` ASC ";
        JSONArray array = DBUtil.getObjectListBySQL(sql, new Object[]{roomNo, roomId});
        for (int i = 0; i < array.size(); i++) {
            JSONObject o = array.getJSONObject(i);
            if (null == o) continue;
            if (o.containsKey("gamelog_id")) {//防止数字过长传输过程中丢失，转成string
                o.element("gamelog_id", o.getString("gamelog_id"));
            }
            TimeUtil.transTimeStamp(o, "yyyy-MM-dd HH:mm:ss", "createTime");
        }
        JSONObject result = new JSONObject();
        result.element("list", array);
        result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        CommonConstant.sendMsgEvent(client, result, eventName);
    }

    public void modifyProxyMoneyEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode", "type", "modifyMoney"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode,type,modifyMoney]", eventName);
            return;
        }
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号
        String usersId = object.getString(CommonConstant.USERS_ID);
        String type = object.getString("type");
        //楼客权限 验证  start
        JSONObject teaInfo = DBUtil.getObjectBySQL("SELECT z.`id` AS memberId,z.`powern`,t.`proxy_money`,t.`id`,t.`tea_code`,t.`users_id`,t.`platform` FROM tea_member z LEFT JOIN tea_info t ON z.`tea_info_id`=t.`id` WHERE z.`tea_info_code`=? AND z.`users_id` = ? AND z.`audit` = ? AND z.`is_deleted` = ? AND t.`audit` = ? AND t.`is_deleted` = ?"
                , new Object[]{teaInfoCode, usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});

        int powern;
        if (null == teaInfo || !teaInfo.containsKey("powern") || !teaInfo.containsKey("proxy_money")) {
            powern = -1;
        } else {
            powern = teaInfo.getInt("powern");
        }
        if (-1 == powern) {
            CommonConstant.sendMsgEventNo(client, "请刷新后再试", null, eventName);
            return;
        }
        if (TeaConstant.TEA_MEMBER_POWERN_LZ != powern) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }
        //楼客权限 验证  end
        if (!"0".equals(type) && !"1".equals(type)) {
            CommonConstant.sendMsgEventNo(client, "修改类型错误", "0提取1存入", eventName);
            return;
        }
        int modifyMoney = object.getInt("modifyMoney");//修改的金额
        if (modifyMoney < 1) {
            CommonConstant.sendMsgEventNo(client, "请输入正确的金额", null, eventName);
            return;
        }
//        JSONObject proxyInfo = DBUtil.getObjectBySQL("SELECT t.`id` FROM t_proxy_info t WHERE t.`users_id`= ? AND t.`audit`= ? AND t.`is_deleted`= ?"
//                , new Object[]{usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
//        if (null == proxyInfo || !proxyInfo.containsKey("id")) {
//            CommonConstant.sendMsgEventNo(client, "请先成为代理,才能操作", null, eventName);
//            return;
//        }
//        int money = proxyInfo.getInt("money");

        JSONObject user = DBUtil.getObjectBySQL("SELECT t.`id`,t.`roomcard` FROM za_users t WHERE t.`id`= ? "
                , new Object[]{usersId});
        if (null == user || !user.containsKey("roomcard")) {
            CommonConstant.sendMsgEventNo(client, "用户不存在", null, eventName);
            return;
        }
        int roomcard = user.getInt("roomcard");

        int proxyMoney = teaInfo.getInt("proxy_money");
        int newMoney = 0;
        if ("0".equals(type)) {//提取
            if (proxyMoney < modifyMoney) {
                CommonConstant.sendMsgEventNo(client, "茶壶不足", null, eventName);
                return;
            }
            newMoney = proxyMoney - modifyMoney;
            DBUtil.executeUpdateBySQL("UPDATE tea_info SET `proxy_money`= `proxy_money` - ? WHERE id = ?", new Object[]{modifyMoney, teaInfo.get("id")});
            JSONObject teaInfoRecord = new JSONObject();
            teaInfoRecord
                    .element("tea_info_id", teaInfo.get("id"))
                    .element("tea_info_code", teaInfoCode)
                    .element("tea_member_id", teaInfo.get("memberId"))
                    .element("users_id", teaInfo.get("users_id"))
                    .element("tea_money", -modifyMoney)
                    .element("old_money", proxyMoney)
                    .element("new_money", proxyMoney - modifyMoney)
                    .element("source_type", TeaConstant.TEA_MEMBER_MONEY_RECORD_SOURCE_TYPE_GLY_TEA_PROXY)
                    .element("platform", teaInfo.get("platform"))
                    .element("operation_member_id", teaInfo.get("memberId"))
            ;
            DBJsonUtil.add(teaInfoRecord, "tea_member_money_record");
            DBUtil.executeUpdateBySQL("UPDATE za_users SET `roomcard`= `roomcard` + ? WHERE id = ?", new Object[]{modifyMoney, user.get("id")});
            JSONObject userdeduction = new JSONObject();
            userdeduction
                    .element("club_id", teaInfo.get("id"))
                    .element("club_type", ZAUserdeductionConstant.CLUB_TYPE_CL)
                    .element("userid", teaInfo.get("users_id"))
                    .element("`type`", ZAUserdeductionConstant.TYPE_ROOMCARD)
                    .element("`sum`", modifyMoney)
                    .element("pocketNew", modifyMoney + roomcard)
                    .element("pocketOld", roomcard)
                    .element("pocketChange", modifyMoney)
                    .element("operatorType", ZAUserdeductionConstant.OPERATORTYPE_TEA_PAY)
                    .element("platform", teaInfo.get("platform"))

            ;
            DBJsonUtil.add(userdeduction, "za_userdeduction");
        } else {//存入
            if (roomcard < modifyMoney) {
                CommonConstant.sendMsgEventNo(client, "房卡不足", null, eventName);
                return;
            }
            newMoney = proxyMoney + modifyMoney;

            DBUtil.executeUpdateBySQL("UPDATE za_users SET `roomcard`= `roomcard` - ? WHERE id = ?", new Object[]{modifyMoney, user.get("id")});
            JSONObject userdeduction = new JSONObject();
            userdeduction
                    .element("club_id", teaInfo.get("id"))
                    .element("club_type", ZAUserdeductionConstant.CLUB_TYPE_CL)
                    .element("userid", teaInfo.get("users_id"))
                    .element("`type`", ZAUserdeductionConstant.TYPE_ROOMCARD)
                    .element("`sum`", -modifyMoney)
                    .element("pocketNew", modifyMoney - roomcard)
                    .element("pocketOld", roomcard)
                    .element("pocketChange", -modifyMoney)
                    .element("operatorType", ZAUserdeductionConstant.OPERATORTYPE_TEA_PAY)
                    .element("platform", teaInfo.get("platform"))
                    .element("memo", ZAUserdeductionConstant.ZA_USERDEDUCTION_MEMO_TEA_MONEY)
            ;
            DBJsonUtil.add(userdeduction, "za_userdeduction");
            DBUtil.executeUpdateBySQL("UPDATE tea_info SET `proxy_money`= `proxy_money` + ? WHERE id = ?", new Object[]{modifyMoney, teaInfo.get("id")});
            JSONObject teaInfoRecord = new JSONObject();
            teaInfoRecord
                    .element("tea_info_id", teaInfo.get("id"))
                    .element("tea_info_code", teaInfoCode)
                    .element("tea_member_id", teaInfo.get("memberId"))
                    .element("users_id", teaInfo.get("users_id"))
                    .element("tea_money", modifyMoney)
                    .element("old_money", proxyMoney)
                    .element("new_money", proxyMoney + modifyMoney)
                    .element("source_type", TeaConstant.TEA_MEMBER_MONEY_RECORD_SOURCE_TYPE_GLY_TEA_PROXY)
                    .element("platform", teaInfo.get("platform"))
                    .element("operation_member_id", teaInfo.get("memberId"))
            ;
            DBJsonUtil.add(teaInfoRecord, "tea_member_money_record");

        }


        JSONObject result = new JSONObject();
        result
                .element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES)
                .element(CommonConstant.RESULT_KEY_MSG, "操作成功")
                .element("newMoney", newMoney)
        ;
        CommonConstant.sendMsgEvent(client, result, eventName);
    }


    public void modifyTeaMoneyEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode", "type", "memberId", "modifyMoney"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode,type,memberId,modifyMoney]", eventName);
            return;
        }
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号
        String usersId = object.getString(CommonConstant.USERS_ID);
        String type = object.getString("type");
        String memberId = object.getString("memberId");
        if (!"0".equals(type) && !"1".equals(type)) {
            CommonConstant.sendMsgEventNo(client, "修改类型错误", "0添加1扣除", eventName);
            return;
        }
        int modifyMoney = object.getInt("modifyMoney");//修改的金额
        if (modifyMoney < 1) {
            CommonConstant.sendMsgEventNo(client, "请输入正确的金额", null, eventName);
            return;
        }
        JSONObject member = DBUtil.getObjectBySQL("SELECT z.`users_id`,z.`tea_money`,z.`id` FROM tea_member z  WHERE z.`tea_info_code`=? AND z.`id` = ? AND z.`audit` = ? AND z.`is_deleted` = ?"
                , new Object[]{teaInfoCode, memberId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
        if (null == member || !member.containsKey("tea_money")) {
            CommonConstant.sendMsgEventNo(client, "该楼客不存在请刷新后再试", null, eventName);
            return;
        }
        int teaMoney = member.getInt("tea_money");
        if ("1".equals(type)) {//减少
            if (modifyMoney > teaMoney) {
                CommonConstant.sendMsgEventNo(client, "茶叶不足", null, eventName);
                return;
            }
        }
        //楼客权限 验证  start
        JSONObject teaInfo = DBUtil.getObjectBySQL("SELECT z.`id` AS memberId,z.`powern`,t.`proxy_money`,t.`id`,t.`tea_code`,t.`users_id`,t.`platform` FROM tea_member z LEFT JOIN tea_info t ON z.`tea_info_id`=t.`id` WHERE z.`tea_info_code`=? AND z.`users_id` = ? AND z.`audit` = ? AND z.`is_deleted` = ? AND t.`audit` = ? AND t.`is_deleted` = ?"
                , new Object[]{teaInfoCode, usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});

        int powern;
        if (null == teaInfo || !teaInfo.containsKey("powern") || !teaInfo.containsKey("proxy_money")) {
            powern = -1;
        } else {
            powern = teaInfo.getInt("powern");
        }

        if (-1 == powern) {
            CommonConstant.sendMsgEventNo(client, "请刷新后再试", null, eventName);
            return;
        }
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY == powern) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }
        //楼客权限 验证  end
        int proxyMoney = teaInfo.getInt("proxy_money");//茶壶

        if ("0".equals(type)) {//添加
            if (proxyMoney < modifyMoney) {
                CommonConstant.sendMsgEventNo(client, "茶壶不足", null, eventName);
                return;
            }

            DBUtil.executeUpdateBySQL("UPDATE tea_info SET `proxy_money`= `proxy_money` - ? WHERE id = ?", new Object[]{modifyMoney, teaInfo.get("id")});
            JSONObject teaInfoRecord = new JSONObject();
            teaInfoRecord
                    .element("tea_info_id", teaInfo.get("id"))
                    .element("tea_info_code", teaInfo.get("tea_code"))
                    .element("tea_member_id", teaInfo.get("memberId"))
                    .element("users_id", teaInfo.get("users_id"))
                    .element("tea_money", -modifyMoney)
                    .element("old_money", proxyMoney)
                    .element("new_money", proxyMoney - modifyMoney)
                    .element("source_type", TeaConstant.TEA_MEMBER_MONEY_RECORD_SOURCE_TYPE_GLY_TEA)
                    .element("platform", teaInfo.get("platform"))
                    .element("operation_member_id", memberId)
            ;
            DBJsonUtil.add(teaInfoRecord, "tea_member_money_record");
            DBUtil.executeUpdateBySQL("UPDATE tea_member SET `tea_money`= `tea_money` + ? WHERE id = ?", new Object[]{modifyMoney, memberId});
            JSONObject memberRecord = new JSONObject();
            memberRecord
                    .element("tea_info_id", teaInfo.get("id"))
                    .element("tea_info_code", teaInfo.get("tea_code"))
                    .element("tea_member_id", memberId)
                    .element("users_id", member.get("users_id"))
                    .element("tea_money", modifyMoney)
                    .element("old_money", teaMoney)
                    .element("new_money", teaMoney + modifyMoney)
                    .element("source_type", TeaConstant.TEA_MEMBER_MONEY_RECORD_SOURCE_TYPE_GLY_MEMBER)
                    .element("platform", teaInfo.get("platform"))
                    .element("operation_member_id", teaInfo.get("memberId"))
            ;
            DBJsonUtil.add(memberRecord, "tea_member_money_record");
        } else {//扣除
            DBUtil.executeUpdateBySQL("UPDATE tea_member SET `tea_money`= `tea_money` - ? WHERE id = ?", new Object[]{modifyMoney, memberId});
            JSONObject memberRecord = new JSONObject();
            memberRecord
                    .element("tea_info_id", teaInfo.get("id"))
                    .element("tea_info_code", teaInfo.get("tea_code"))
                    .element("tea_member_id", memberId)
                    .element("users_id", member.get("users_id"))
                    .element("tea_money", -modifyMoney)
                    .element("old_money", teaMoney)
                    .element("new_money", teaMoney - modifyMoney)
                    .element("source_type", TeaConstant.TEA_MEMBER_MONEY_RECORD_SOURCE_TYPE_GLY_MEMBER)
                    .element("platform", teaInfo.get("platform"))
                    .element("operation_member_id", teaInfo.get("memberId"))
            ;
            DBJsonUtil.add(memberRecord, "tea_member_money_record");
            DBUtil.executeUpdateBySQL("UPDATE tea_info SET `proxy_money`= `proxy_money` + ? WHERE id = ?", new Object[]{modifyMoney, teaInfo.get("id")});
            JSONObject teaInfoRecord = new JSONObject();
            teaInfoRecord
                    .element("tea_info_id", teaInfo.get("id"))
                    .element("tea_info_code", teaInfo.get("tea_code"))
                    .element("tea_member_id", teaInfo.get("memberId"))
                    .element("users_id", teaInfo.get("users_id"))
                    .element("tea_money", modifyMoney)
                    .element("old_money", proxyMoney)
                    .element("new_money", proxyMoney + modifyMoney)
                    .element("source_type", TeaConstant.TEA_MEMBER_MONEY_RECORD_SOURCE_TYPE_GLY_TEA)
                    .element("platform", teaInfo.get("platform"))
                    .element("operation_member_id", memberId)
            ;
            DBJsonUtil.add(teaInfoRecord, "tea_member_money_record");
        }

        CommonConstant.sendMsgEventYes(client, "操作成功", eventName);
    }

    public void getMemberInfoEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode]", eventName);
            return;
        }
        String teaInfoCode = object.getString("teaInfoCode");//茶楼号
        String usersId = object.getString(CommonConstant.USERS_ID);
        JSONObject teaInfo = DBUtil.getObjectBySQL("SELECT t.`id`,t.`tea_name` AS teaName,t.`tea_code` AS teaCode,f.`tea_money`AS teaMoney,f.`powern` AS memberPowern,f.`score_lock` AS scoreLock FROM tea_info t LEFT JOIN tea_member f ON t.`id`=f.`tea_info_id`WHERE t.`tea_code`=? AND f.`users_id`=? AND t.`audit`=? AND t.`is_deleted`=? AND f.`audit`=? AND f.`is_deleted`=?"
                , new Object[]{teaInfoCode, usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});

        if (null == teaInfo || !teaInfo.containsKey("teaName") || !teaInfo.containsKey("teaMoney")) {
            CommonConstant.sendMsgEventYes(client, "请刷新后再重试", eventName);
            return;
        }
        JSONObject result = new JSONObject();
        int memberPowern = teaInfo.getInt("memberPowern");//权限
        int auditMemberCount = 0;//待审核成员数量
        int quitMemberMsgCount = 0;//成员退出消息
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY != memberPowern) {
            JSONObject memberCount = DBUtil.getObjectBySQL("SELECT COUNT(*)AS`count`,(SELECT COUNT(*)FROM tea_member_msg t WHERE t.`tea_info_id`=? AND t.`type`=? AND t.`is_deleted`=? AND t.`is_read`=?)AS quitCount FROM tea_member t WHERE t.`tea_info_id`=? AND t.`audit`=? AND t.`is_deleted`=?"
                    , new Object[]{teaInfo.get("id"), TeaConstant.TEA_MEMBER_MSG_TYPE_TC, TeaConstant.IS_DELETE_N, TeaConstant.TEA_MEMBER_MSG_READ_N, teaInfo.get("id"), TeaConstant.AUDIT_N, TeaConstant.IS_DELETE_N});
            if (null != memberCount && memberCount.containsKey("count") && memberCount.containsKey("quitCount")) {
                auditMemberCount = memberCount.getInt("count");
                quitMemberMsgCount = memberCount.getInt("quitCount");
            }
        }
        teaInfo
                .element("auditMemberCount", auditMemberCount)
                .element("quitMemberMsgCount", quitMemberMsgCount)
        ;
        result.element("data", teaInfo);
        result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        CommonConstant.sendMsgEvent(client, result, eventName);
    }

    public void memberMsgReadEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoId", "type"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoId,memberMsgId,type]", eventName);
            return;
        }
        String teaInfoId = object.getString("teaInfoId");//茶楼id
        String usersId = object.getString(CommonConstant.USERS_ID);
        String type = object.getString("type");
        if (!"0".equals(type) && !"1".equals(type)) {
            CommonConstant.sendMsgEventNo(client, "操作类型错误", "0单条,1全部", eventName);
        }
        //楼客权限 验证  start
        JSONObject teaMember = DBUtil.getObjectBySQL("SELECT z.`id`,z.`powern`,z.`tea_info_id` FROM tea_member z WHERE z.`tea_info_id`=? AND z.`users_id` = ? AND z.`audit` = ? AND z.`is_deleted` = ?"
                , new Object[]{teaInfoId, usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
        if (null == teaMember || !teaMember.containsKey("powern")) {
            CommonConstant.sendMsgEventNo(client, "您已经退出该茶楼", null, eventName);
            return;
        }

        int powern = teaMember.getInt("powern");//获取权限
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY == powern) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }
        //楼客权限 验证  end

        if ("0".equals(type)) {
            if (!object.containsKey("memberMsgId")) {
                CommonConstant.sendMsgEventNo(client, "系统错误", "缺少 memberMsgId", eventName);
            }
            DBUtil.executeUpdateBySQL("UPDATE tea_member_msg SET `is_read`= ? WHERE id = ? AND `is_read` = ?  AND `tea_info_id` = ?"
                    , new Object[]{TeaConstant.TEA_MEMBER_MSG_READ_Y, object.get("memberMsgId"), TeaConstant.TEA_MEMBER_MSG_READ_N, teaInfoId});
        } else {//全部
            DBUtil.executeUpdateBySQL("UPDATE tea_member_msg SET `is_read`= ? WHERE `is_read` = ?AND `tea_info_id` = ?"
                    , new Object[]{TeaConstant.TEA_MEMBER_MSG_READ_Y, TeaConstant.TEA_MEMBER_MSG_READ_N, teaInfoId});
        }
        CommonConstant.sendMsgEventYes(client, "操作成功", eventName);
    }


    public void moneyRecordListEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoId", "type"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoId,type 1 游戏消耗  2 转入转出]", eventName);
            return;
        }
        String usersId = object.getString(CommonConstant.USERS_ID);
        String teaInfoId = object.getString("teaInfoId");//当前所在的茶楼id
        //楼客权限 验证  start
        JSONObject teaMember = DBUtil.getObjectBySQL("SELECT z.`powern` FROM tea_member z WHERE z.`tea_info_id`=? AND z.`users_id` = ? AND z.`audit` = ? AND z.`is_deleted` = ?"
                , new Object[]{teaInfoId, usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
        if (null == teaMember || !teaMember.containsKey("powern")) {
            CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
            return;
        }
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY == teaMember.getInt("powern")) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }
        //楼客权限 验证  end
        int page = object.get("pageIndex") == null ? 1 : object.getInt("pageIndex");
        int limitNum = object.get("pageSize") == null ? 6 : object.getInt("pageSize");
        String time = DateUtils.searchTime(object.get("time"));//日期搜索
        int type = object.getInt("type");
        JSONObject moneyRecordPage = teaService.getMoneyRecordLimitPage(teaInfoId, type, time, page, limitNum);
        JSONArray moneyRecord = new JSONArray();

        if (moneyRecordPage.containsKey("list")) {
            JSONArray newData = moneyRecordPage.getJSONArray("list");
            for (int i = 0; i < newData.size(); i++) {
                JSONObject money = newData.getJSONObject(i);
                TimeUtil.transTimeStamp(money, "yyyy-MM-dd HH:mm:ss", "createTime");
                if (TeaConstant.TEA_MEMBER_MONEY_RECORD_SOURCE_TYPE_XH == money.getInt("sourceType")) {
                    money.element("remark", "游戏消耗");
                    money.element("operationInfo", "房间号 " + money.get("roomNo"));
                } else if (TeaConstant.TEA_MEMBER_MONEY_RECORD_SOURCE_TYPE_GLY_TEA == money.getInt("sourceType")) {
                    money.element("remark", "茶壶操作");
                    money.element("operationInfo", money.get("name"));
                } else if (TeaConstant.TEA_MEMBER_MONEY_RECORD_SOURCE_TYPE_GLY_MEMBER == money.getInt("sourceType")) {
                    money.element("remark", "茶叶操作");
                    money.element("operationInfo", money.get("name"));
                } else if (TeaConstant.TEA_MEMBER_MONEY_RECORD_SOURCE_TYPE_GLY_TEA_PROXY == money.getInt("sourceType")) {
                    money.element("remark", "茶壶操作");
                    money.element("operationInfo", money.get("name"));
                }
                moneyRecord.add(money);
            }
        }

        JSONObject result = new JSONObject();
        List list = new ArrayList();
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT SUM(t.`tea_money`)AS teaMoneyAll FROM tea_member_money_record t LEFT JOIN za_users z ON t.`users_id`=z.`id`WHERE  ");
        if (null != time && !"".equals(time)) {
            sb.append("  (DATEDIFF(t.`create_time`,?)=0)");
            list.add(time);
        }
        sb.append(" AND t.`tea_info_id`= ?");
        list.add(teaInfoId);
        if (1 == type) {//游戏
            sb.append(" AND t.`source_type` = 0 AND t.`pay_type` = 3");
        } else if (2 == type) {//操作
            sb.append(" AND t.`source_type` in (1,3) ");
        }
        JSONObject teaMoneyAll = DBUtil.getObjectBySQL(sb.toString(), list.toArray());

        result
                .element("teaMoneyAll", teaMoneyAll.containsKey("teaMoneyAll") ? teaMoneyAll.getInt("teaMoneyAll") : 0)
                .element("data", moneyRecord)
                .element("time", time)
                .element("pageIndex", moneyRecordPage.containsKey("pageIndex") ? moneyRecordPage.getInt("pageIndex") : 1)
                .element("pageSize", moneyRecordPage.containsKey("pageSize") ? moneyRecordPage.getInt("pageSize") : 6)
                .element("totalPage", moneyRecordPage.containsKey("totalPage") ? moneyRecordPage.getInt("totalPage") : 0)
                .element("totalCount", moneyRecordPage.containsKey("totalCount") ? moneyRecordPage.getInt("totalCount") : 0);
        result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        CommonConstant.sendMsgEvent(client, result, eventName);
    }


    public void recordTickEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoCode", "gameRecordId", "tickStatus"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoCode,gameRecordId,tickStatus]", eventName);
            return;
        }

        //用户ID
        String usersId = object.getString(CommonConstant.USERS_ID);
        //战榜ID
        String gameRecordId = object.getString("gameRecordId");
        //勾选状态
        String tickStatus = object.getString("tickStatus");
        //茶楼号
        String teaInfoCode = object.getString("teaInfoCode");

        JSONObject teaInfo = DBUtil.getObjectBySQL("SELECT t.`id`,t.`users_id` FROM tea_info t WHERE t.`tea_code`= ? AND t.`audit`= ? AND t.`is_deleted`= ?"
                , new Object[]{teaInfoCode, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
        if (null == teaInfo || !teaInfo.containsKey("users_id")) {
            CommonConstant.sendMsgEventNo(client, "该茶楼不存在", null, eventName);
            return;
        }

        JSONObject teaMember = DBUtil.getObjectBySQL("SELECT t.`powern` FROM tea_member t WHERE t.`tea_info_code` = ? AND t.`audit`= ? AND t.`is_deleted`= ? and t.`users_id` = ? "
                , new Object[]{teaInfoCode, TeaConstant.AUDIT_Y, TeaConstant.AUDIT_N, usersId});
        //判断是否存在
        if (teaMember == null || !teaMember.containsKey("powern")) {
            CommonConstant.sendMsgEventNo(client, "请刷新后重试", null, eventName);
            return;
        }
        //判断是否是群主或管理员
        if (TeaConstant.TEA_MEMBER_POWERN_LZ != teaMember.getInt("powern") && TeaConstant.TEA_MEMBER_POWERN_GLY != teaMember.getInt("powern")) {
            CommonConstant.sendMsgEventNo(client, "您权限不足", null, eventName);
            return;
        }

        //战榜勾选操作
        try {
            teaService.updateTeaRecordStatiTick(gameRecordId, tickStatus, teaInfoCode);
        } catch (Exception e) {
            log.info("战榜勾选操作数据库失败", e.getMessage());
            CommonConstant.sendMsgEventNo(client, "操作失败,请重试", null, eventName);
            return;
        }
        CommonConstant.sendMsgEventYes(client, "操作成功", eventName);

    }

    public void modifyMemberScoreLockEvent(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{CommonConstant.USERS_ID, "teaInfoId", "teaMemberId", "scoreLock"})) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "缺少参数[uid,teaInfoId,teaMemberId,scoreLock]", eventName);
            return;
        }
        long usersId = object.getLong(CommonConstant.USERS_ID);//用户id
        String teaInfoId = object.getString("teaInfoId");//茶楼id
        String teaMemberId = object.getString("teaMemberId");
        int scoreLock = object.getInt("scoreLock");//分数锁
        if (scoreLock < 0) {
            CommonConstant.sendMsgEventNo(client, "请输入正确的分数锁", null, eventName);
            return;
        }

        //验证  操作玩家
        JSONObject teaMember = DBUtil.getObjectBySQL("SELECT z.`powern`,z.`tea_info_id` FROM tea_member z WHERE z.`tea_info_id`=? AND z.`users_id` = ? AND z.`audit` = ? AND z.`is_deleted` = ?"
                , new Object[]{teaInfoId, usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});

        if (null == teaMember) {
            CommonConstant.sendMsgEventNo(client, "您已被请出该茶楼", null, eventName);
            return;
        }
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY == teaMember.getInt("powern")) {
            CommonConstant.sendMsgEventNo(client, "您没有权限", null, eventName);
            return;
        }

        //保存玩家
        int i = DBUtil.executeUpdateBySQL("UPDATE tea_member SET `score_lock`=? WHERE id = ? AND `tea_info_id`= ?"
                , new Object[]{scoreLock, teaMemberId, teaInfoId});
        if (i > 0) {
            JSONObject result = new JSONObject();
            result.element("scoreLock", scoreLock);
            result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            result.element(CommonConstant.RESULT_KEY_MSG, "操作成功");
            CommonConstant.sendMsgEvent(client, result, eventName);
            return;
        }
        CommonConstant.sendMsgEventNo(client, "操作失败", null, eventName);

    }

    private void removeRoomAccount(String roomNo, String account) {
        GameRoom room = RoomManage.gameRoomMap.get(roomNo);
        if (null == room) return;

        // 退出房间更新数据 start
// 移除数据
        for (int i = 0; i < room.getUserIdList().size(); i++) {
            if (null != room.getPlayerMap().get(account) && room.getUserIdList().get(i) == room.getPlayerMap().get(account).getId()) {
                room.getUserIdList().set(i, 0L);
                room.addIndexList(room.getPlayerMap().get(account).getMyIndex());
                break;
            }
        }

// 退出房间更新数据 end

        switch (room.getGid()) {
            case CommonConstant.GAME_ID_NN:
                ((NNGameRoomNew) room).getUserPacketMap().remove(account);
                break;
            case CommonConstant.GAME_ID_SSS:
                ((SSSGameRoomNew) room).getUserPacketMap().remove(account);
                break;
            case CommonConstant.GAME_ID_ZJH:
                ((ZJHGameRoomNew) room).getUserPacketMap().remove(account);
                break;
            case CommonConstant.GAME_ID_BDX:
                ((BDXGameRoomNew) room).getUserPacketMap().remove(account);
                break;
            case CommonConstant.GAME_ID_QZMJ:
                ((QZMJGameRoom) room).getUserPacketMap().remove(account);
                break;
            case CommonConstant.GAME_ID_ZZC:
                ((QZMJGameRoom) room).getUserPacketMap().remove(account);
                break;
            case CommonConstant.GAME_ID_NAMJ:
                ((QZMJGameRoom) room).getUserPacketMap().remove(account);
                break;
            case CommonConstant.GAME_ID_GP_PJ:
                ((GPPJGameRoom) room).getUserPacketMap().remove(account);
                break;
            case CommonConstant.GAME_ID_MJ_PJ:
                ((GPPJGameRoom) room).getUserPacketMap().remove(account);
                break;
            case CommonConstant.GAME_ID_DDZ:
                ((DdzGameRoom) room).getUserPacketMap().remove(account);
                break;
            case CommonConstant.GAME_ID_GDY://干瞪眼
                ((GDYGameRoom) room).getUserPacketMap().remove(account);
                break;
            case CommonConstant.GAME_ID_SG://三公
                ((SGGameRoom) room).getUserPacketMap().remove(account);
                break;
            case CommonConstant.GAME_ID_GZMJ:
                ((GzMjGameRoom) room).getUserPacketMap().remove(account);
                break;
            default:
                break;
        }
        JSONObject roomInfo = new JSONObject();
        roomInfo.put("roomNo", room.getRoomNo());
        roomInfo.put("roomId", room.getId());
        roomInfo.put("userIndex", room.getPlayerMap().get(account).getMyIndex());
        room.getPlayerMap().remove(account);
        room.getVisitPlayerMap().remove(account); //观战人员直接退出
        roomInfo.put("player_number", room.getPlayerMap().size());
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_QUIT_USER_ROOM, roomInfo));
    }


    public void createRoomTeaEvent(SocketIOClient client, JSONObject object) {
        if (!JsonUtil.isNullVal(object, new String[]{"teaInfoCode", "base_info", "platform", "account", "users_id"})) {
            return;
        }

        JSONObject baseInfo = object.getJSONObject("base_info");
        //房间配置验证
        if (baseInfo == null || !baseInfo.containsKey("paytype") || !baseInfo.containsKey("roomType") || !baseInfo.containsKey("gid")
                || CommonConstant.ROOM_TYPE_TEA != baseInfo.getInt("roomType")) {
            return;
        }
        object
                .element("gid", baseInfo.get("gid"))
        ;
        String teaInfoCode = object.getString("teaInfoCode");
        if (!redisInfoService.teaRoomOpen(teaInfoCode)) {
            return;
        }
        if (!redisInfoService.isExistCreateRoom(teaInfoCode, false)) {
            return;
        } else {
            redisInfoService.subExistCreateRoom(teaInfoCode);
        }

        JSONObject userInfo = new JSONObject();
        userInfo
                .element("platform", object.get("platform"))
                .element("account", object.get("account"))
                .element("id", object.get("users_id"))
        ;
        // 创建房间
        baseEventDeal.createRoomBase(client, object, userInfo);

    }

    public void modifyMemberUseStatus(SocketIOClient client, JSONObject object, String eventName) {
        if (!JsonUtil.isNullVal(object, new String[]{"teaId", CommonConstant.DATA_KEY_PLATFORM, CommonConstant.USERS_ID, "operatedId", "useStatus"})) {
            return;
        }

        String teaId = object.getString("teaId");
        String userId = object.getString(CommonConstant.USERS_ID);
        String operatedId = object.getString("operatedId");
        String platform = object.getString(CommonConstant.DATA_KEY_PLATFORM);
        String useStatus = object.getString("useStatus");

        //参数是否符合规范
        if (!TeaConstant.IS_USE_NO.equals(useStatus) && !TeaConstant.IS_USE_YES.equals(useStatus)) {
            return;
        }

        //获取被操作成员信息
        JSONObject operatedMemberInfo = DBUtil.getObjectBySQL("SELECT id,powern FROM tea_member WHERE is_deleted = ? AND users_id = ? AND tea_info_id = ? AND platform = ?", new Object[]{TeaConstant.IS_DELETE_N, operatedId, teaId, platform});

        if (operatedMemberInfo == null) {
            CommonConstant.sendMsgEventNo(client, "成员不在该茶楼", null, eventName);
            return;
        }

        //获取操作成员信息
        JSONObject memberInfo = DBUtil.getObjectBySQL("SELECT id,powern FROM tea_member WHERE is_deleted = ? AND tea_info_id = ? AND platform = ? AND users_id = ?", new Object[]{TeaConstant.IS_DELETE_N, teaId, platform, userId});
        if (memberInfo == null) {
            CommonConstant.sendMsgEventNo(client, "请稍后重试", null, eventName);
            return;
        }

        //被操作成员的权限
        int operatedPower = operatedMemberInfo.containsKey("powern") ? operatedMemberInfo.getInt("powern") : TeaConstant.TEA_MEMBER_POWERN_PTCY;
        //操作成员的权限
        int userPower = memberInfo.containsKey("powern") ? memberInfo.getInt("powern") : TeaConstant.TEA_MEMBER_POWERN_PTCY;

        //判断操作成员的权限
        if (TeaConstant.TEA_MEMBER_POWERN_PTCY == userPower) {
            CommonConstant.sendMsgEventNo(client, "权限不足", "只能管理员操作,权限不足", eventName);
            return;
        }
        //被操作者是管理员
        if (TeaConstant.TEA_MEMBER_POWERN_GLY == operatedPower && TeaConstant.TEA_MEMBER_POWERN_LZ != userPower) {
            CommonConstant.sendMsgEventNo(client, "权限不足", "管理员只能由创建者禁止,权限不足", eventName);
            return;
        }

        try {
            DBUtil.executeUpdateBySQL("UPDATE tea_member SET is_use = ? WHERE id = ?", new Object[]{useStatus, operatedMemberInfo.getString("id")});
        } catch (Exception e) {
            CommonConstant.sendMsgEventNo(client, "系统错误", "数据库操作错误", eventName);
            return;
        }

        CommonConstant.sendMsgEventYes(client, "操作成功", eventName);
    }
}
