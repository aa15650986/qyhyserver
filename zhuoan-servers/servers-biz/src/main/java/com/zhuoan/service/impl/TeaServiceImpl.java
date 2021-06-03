package com.zhuoan.service.impl;

import com.zhuoan.biz.game.dao.util.BaseSqlUtil;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.DaoTypeConstant;
import com.zhuoan.constant.SSSConstant;
import com.zhuoan.constant.TeaConstant;
import com.zhuoan.dao.DBJsonUtil;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.TeaService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.util.DateUtils;
import com.zhuoan.util.MathDelUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Service
public class TeaServiceImpl implements TeaService {

    private final Logger log = LoggerFactory.getLogger(TeaServiceImpl.class);
    @Resource
    private RedisInfoService redisInfoService;
    @Resource
    private ProducerService producerService;
    @Resource
    private Destination daoQueueDestination;

    @Override
    public String getTeaCode() {
        List<String> array = redisInfoService.getTeaInfoAllCode();
        String code = "";
        boolean b = true;
        while (b) {
            code = MathDelUtil.getRandomStr(5);
            if (!array.contains(code) && !redisInfoService.verifyTeaCode(code)) {
                b = false;
            }
        }
        if (code.length() == 5) {
            JSONObject object = DBUtil.getObjectBySQL("SELECT id  FROM tea_info where tea_code=?", new Object[]{code});
            if (null == object) {
//                log.info(new StringBuilder().append("生成茶楼的编号:").append(code).append(" 原有编号:").append(array.toString()).toString());
                redisInfoService.delTeaInfoAllCode();
                return code;
            } else {
                return getTeaCode();
            }
        } else {
            code = "00000000000000000000";
        }

        return code;
    }

    @Override
    public void applyJoinTea(JSONObject teaMember) {
        if (!teaMember.containsKey("tea_info_id") || !teaMember.containsKey("tea_info_code") || !teaMember.containsKey("users_id")
                || !teaMember.containsKey("powern") || !teaMember.containsKey("audit") || !teaMember.containsKey("platform")) {
            log.info("申请加入茶楼失败 MQ,缺少参数");
            return;
        }
        JSONObject o = DBUtil.getObjectBySQL("SELECT id,audit,is_deleted FROM tea_member z WHERE z.`tea_info_id`= ? AND z.`users_id`= ? "
                , new Object[]{teaMember.get("tea_info_id"), teaMember.get("users_id")});
        if (null == o) {
            DBJsonUtil.add(teaMember, "tea_member");
            return;
        }
        if (TeaConstant.IS_DELETE_N == o.getInt("is_deleted") && TeaConstant.AUDIT_Y == o.getInt("audit")) {
            //已在茶楼里并且审核通过，没有被删除,不处理
            return;
        }
        teaMember.element("id", o.get("id")).element("is_deleted", TeaConstant.IS_DELETE_N);
        DBJsonUtil.saveOrUpdate(teaMember, "tea_member");
    }

    @Override
    public String getRoomGameInfo(JSONObject baseInfo) {
        StringBuilder sb = new StringBuilder();
        if (null != baseInfo) {

            if (baseInfo.containsKey("gameId")) {//游戏id
                if (CommonConstant.GAME_ID_SSS == baseInfo.getInt("gameId")) {
                    sb.append("游戏:罗松  ");
                }
            }

            if (baseInfo.containsKey("player")) {//人数
                sb.append("人数:").append(baseInfo.get("player")).append("人");
                if (baseInfo.containsKey("teaIsFree")) {//是否自由房
                    if (SSSConstant.TEA_IS_FREE_Y.equals(baseInfo.getString("teaIsFree"))) {
                        sb.append("自由房");
                    }
                }
                sb.append("  ");
            }

            if (baseInfo.containsKey("turn")) {//局数
                JSONObject turn = baseInfo.getJSONObject("turn");
                if (turn.containsKey("turn")) {
                    sb.append("局数:").append(turn.get("turn")).append("局");
                }
                sb.append("  ");
            }

            if (baseInfo.containsKey("jiama")) {//马牌
                switch (baseInfo.getInt("jiama")) {
                    case SSSConstant.SSS_MP_TYPE_ALL_W:
                        sb.append("马牌:无");
                        break;
                    case SSSConstant.SSS_MP_TYPE_ALL_HT:
                        sb.append("马牌:黑桃随机");
                        break;
                    case SSSConstant.SSS_MP_TYPE_HT_A:
                        sb.append("马牌:黑桃A");
                        break;
                    case SSSConstant.SSS_MP_TYPE_HT_3:
                        sb.append("马牌:黑桃3");
                        break;
                    case SSSConstant.SSS_MP_TYPE_HT_5:
                        sb.append("马牌:黑桃5");
                        break;
                    case SSSConstant.SSS_MP_TYPE_ALL:
                        sb.append("马牌:全部随机");
                        break;
                    default:
                        break;
                }
                sb.append("  ");
            }

            if (baseInfo.containsKey("feature")) {//特色
                switch (baseInfo.getString("feature")) {
                    case SSSConstant.SSS_FEATURE_LAIZI_ONE:
                        sb.append("特色:一癞");
                        break;
                    case SSSConstant.SSS_FEATURE_LAIZI_TWO:
                        sb.append("特色:两癞");
                        break;
                    default:
                        break;
                }
                sb.append("  ");
            }

            if (baseInfo.containsKey("teaPlay")) {//特殊牌
                switch (baseInfo.getString("teaPlay")) {
                    case SSSConstant.TEA_PLAY_Y:
                        sb.append("特殊牌:有,");
                        String teaPlayMultiple = "一倍分";
                        if (baseInfo.containsKey("teaPlayMultiple")) {
                            if (2 == baseInfo.getInt("teaPlayMultiple")) {
                                teaPlayMultiple = "两倍分";
                            }
                        }
                        sb.append(teaPlayMultiple);
                        break;
                    case SSSConstant.TEA_PLAY_N:
                        sb.append("特殊牌:无");
                        break;
                    default:
                        break;
                }
                sb.append("  ");
            }

            if (baseInfo.containsKey("fire")) {//打枪
                if (SSSConstant.TEA_FIRE_Y.equals(baseInfo.getString("fire"))) {
                    sb.append("打枪/全垒打得分翻倍");
                } else if (SSSConstant.TEA_FIRE_N.equals(baseInfo.getString("fire"))) {
                    sb.append("打枪/全垒打固定吃分");
                }
                sb.append("  ");
            }

            if (baseInfo.containsKey("pattern")) {//模式
                switch (baseInfo.getString("pattern")) {
                    case SSSConstant.TEA_PATTERN_0:
                        sb.append("模式:不自动配牌");
                        break;
                    case SSSConstant.TEA_PATTERN_1:
                        sb.append("模式:1分钟自动配牌");
                        break;
                    case SSSConstant.TEA_PATTERN_2:
                        sb.append("模式:2分钟自动配牌");
                        break;
                    default:
                        break;
                }
                sb.append("  ");
            }
        }
        return sb.toString();
    }

    @Override
    public void importMemberTea(JSONObject jsonObject) {
        if (!jsonObject.containsKey("teaInfoCode") || !jsonObject.containsKey("teaInfoCodeIds")) {
            log.info("批量导入茶楼成员失败 MQ,缺少参数");
            return;
        }
        String teaInfoCode = jsonObject.getString("teaInfoCode");
        JSONObject placeTea = DBUtil.getObjectBySQL("SELECT id,platform FROM tea_info z WHERE z.`tea_code` = ? AND z.`audit` = ? AND z.`is_deleted` = ?"
                , new Object[]{teaInfoCode, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
        if (null == placeTea) {
            return;
        }
        String teaInfoCodeIds = jsonObject.getString("teaInfoCodeIds");
        List list = new ArrayList(Arrays.asList(teaInfoCodeIds.split(",")));
        if (null == list || list.size() == 0) {
            return;
        }
        list.remove(teaInfoCode);
        if (null == list || list.size() == 0) {
            return;
        }
        //找出需要导入的所有茶客
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT f.`users_id`,t.`is_deleted`,t.`powern`,t.`audit`,t.`id` AS memberId FROM(");
        sb.append("SELECT z.`users_id`FROM tea_member z WHERE 1 = 1 ");
        for (int i = 0; i < list.size(); i++) {

            if (0 == i) {
                sb.append(" AND z.`tea_info_code`IN(");
            }
            sb.append("?");
            if (i != list.size() - 1) {
                sb.append(",");
            }
            if (i == list.size() - 1) {
                sb.append(")");
            }
        }
        sb.append(" AND z.`audit` = ? AND z.`is_deleted` = ?  GROUP BY z.`users_id`");
        list.add(TeaConstant.AUDIT_Y);
        list.add(TeaConstant.IS_DELETE_N);
        sb.append(")f LEFT JOIN(SELECT z.`users_id`,z.`is_deleted`,z.`powern`,z.`audit`,z.`id` FROM tea_member z WHERE 1=1 AND z.`tea_info_code`= ? GROUP BY z.`users_id`)t ON f.`users_id`=t.`users_id`");
        list.add(teaInfoCode);
        JSONArray memberArray = DBUtil.getObjectListBySQL(sb.toString(), list.toArray());
        if (null == memberArray || memberArray.size() == 0) {
            return;
        }

        List updateList = new ArrayList();//需要更新的茶客 存的是memberId
        List addList = new ArrayList();//需要新增的茶客
        for (int i = 0; i < memberArray.size(); i++) {
            JSONObject o = memberArray.getJSONObject(i);
            if (null == o || !o.containsKey("users_id")) continue;

            if (o.containsKey("is_deleted") && o.containsKey("powern") && o.containsKey("audit") && o.containsKey("memberId")) {
                if (TeaConstant.IS_DELETE_Y == o.getInt("is_deleted")) {//已删除的茶客 更新
                    updateList.add(o.get("memberId"));
                    continue;
                }
                if (TeaConstant.TEA_MEMBER_POWERN_LZ == o.getInt("powern")) {//楼主 不处理
                    continue;
                }
                if (TeaConstant.AUDIT_Y != o.getInt("audit")) {//审核不用 更新
                    updateList.add(o.get("memberId"));
                    continue;
                }

            }
            addList.add(o.get("users_id"));
        }
        //需要更新的处理
        for (int i = 0; i < updateList.size(); i++) {
            JSONObject object = new JSONObject();
            object
                    .element("id", updateList.get(i))
                    .element("powern", TeaConstant.TEA_MEMBER_POWERN_PTCY)//普通成员
                    .element("audit", TeaConstant.AUDIT_Y)
                    .element("is_deleted", TeaConstant.IS_DELETE_N);
            DBJsonUtil.update(object, "tea_member", "id");
        }

        //需要插入的处理
        List<Object[]> insertList = new ArrayList<>();
        for (int i = 0; i < addList.size(); i++) {
            //插入前验证是否存在
            JSONObject m = DBUtil.getObjectBySQL("SELECT id FROM tea_member WHERE tea_info_code = ? AND users_id = ? "
                    , new Object[]{teaInfoCode, addList.get(i)});
            if (null != m) continue;

            Object[] o = new Object[]{placeTea.get("id"), teaInfoCode, addList.get(i), TeaConstant.AUDIT_Y, placeTea.get("platform")};
            insertList.add(o);
        }

        if (insertList != null && insertList.size() > 0) {//批量插入
            BaseSqlUtil.insertData("tea_member", new String[]{"tea_info_id", "tea_info_code", "users_id", "audit", "platform"}, insertList);
        }

    }

    @Override
    public int getTeaMemberPowern(Object usersId, String teaInfoCode) {
        JSONObject teaMember = DBUtil.getObjectBySQL("SELECT z.`powern` FROM tea_member z WHERE z.`tea_info_code`=? AND z.`users_id` = ? AND z.`audit` = ? AND z.`is_deleted` = ?"
                , new Object[]{teaInfoCode, usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});

        if (null == teaMember || !teaMember.containsKey("powern")) {
            return -1;
        }
        return teaMember.getInt("powern");
    }

    @Override
    public JSONObject getTeaMemberLimitPage(String teaInfoId, String usersAccountOrName, Integer page, Integer limitNum) {

        String attribute = "t.is_use isUse,t.`score_lock`AS scoreLock,t.`tea_money` AS memberTeaMoney,t.`id` AS memberId,z.`headimg` AS memberHeadimg,z.`name` AS memberName,z.`account` AS memberAccount,t.`powern` AS memberPowern,t.`users_id`";
        StringBuilder sb = new StringBuilder();
        sb.append(" tea_member t LEFT JOIN za_users z ON t.`users_id`=z.`id`WHERE 1=1 ");
        List list = new ArrayList();
        if (null != usersAccountOrName && !"".equals(usersAccountOrName)) {
            sb
                    .append(" AND CONCAT (z.`account`, IFNULL(z.`name`, ''))LIKE '%")
                    .append(usersAccountOrName)
                    .append("%'");
        }
        sb.append(" AND t.`tea_info_id`= ? AND t.`audit`= ? AND t.`is_deleted`= ? ");
        list.add(teaInfoId);
        list.add(TeaConstant.AUDIT_Y);
        list.add(TeaConstant.IS_DELETE_N);

        return DBUtil.getObjectPageBySQL(attribute, sb.toString(), list.toArray(), page, limitNum);
    }

    @Override
    public void insertTeaMemberMsg(JSONObject object) {
        JSONObject obj = new JSONObject();
        JSONObject memberInfo = DBUtil.getObjectBySQL("SELECT * FROM tea_member t WHERE t.`id`= ?", new Object[]{object.get("memberId")});
        int type = object.getInt("type");
        if (null == memberInfo) {
            log.info("插入茶楼消息通知 参数错误");
            return;
        }
        if (TeaConstant.TEA_MEMBER_MSG_TYPE_YQ == type && !object.containsKey("invitesTeaInfo")) {
            return;
        }
        if (TeaConstant.TEA_MEMBER_MSG_TYPE_YQ == type) {
            if (object.getLong("invitesTeaInfo") <= 0D) {
                log.info("插入茶楼消息通知 参数错误");
                return;
            } else {
                obj.element("invites_tea_info", object.getLong("invitesTeaInfo"));
            }

        }
        obj
                .element("tea_member_id", memberInfo.get("id"))
                .element("users_id", memberInfo.get("users_id"))
                .element("tea_info_id", memberInfo.get("tea_info_id"))
                .element("tea_info_code", memberInfo.get("tea_info_code"))
                .element("platform", memberInfo.get("platform"))
                .element("test", object.get("test"))
                .element("type", type)
        ;

        DBJsonUtil.add(obj, "tea_member_msg");
    }

    @Override
    public JSONObject getTeaMumberRecord(long userId, String teaInfoCode, int[] rootType, String time) {
        if (userId < 1L || null == teaInfoCode) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        List list = new JSONArray();
        sb
                .append("SELECT COUNT(*)gameCount,SUM(z.`score`)allScore,SUM(IF(z.`score`>0,1,0))AS winCount,SUM(IF(z.`score`=0,1,0))AS tiesCount,SUM(IF(z.`score`<0,1,0))AS loseCount,SUM(IF(z.`is_big_winner`=1,1,0))AS bigWinCount ")
                .append(" FROM za_user_game_statis z WHERE z.`user_id`= ? AND z.`circle_code`= ? ")
        ;
        list.add(userId);
        list.add(teaInfoCode);
        if (null != rootType && rootType.length > 0) {
            sb.append(" AND z.`room_type` in(");
            for (int i = 0; i < rootType.length; i++) {
                sb.append("?");
                list.add(rootType[i]);
                if (i != rootType.length - 1) {
                    sb.append(",");
                }
            }
            sb.append(") ");
        }
        if (null != time && !"".equals(time)) {
            sb.append(" AND(DATEDIFF(z.`create_time`,?)=0)");
            list.add(time);
        }
        sb.append(" GROUP BY z.`user_id`");
        return DBUtil.getObjectBySQL(sb.toString(), list.toArray());
    }

    @Override
    public JSONObject pageTeaMumberRecord(String teaInfoCode, String usersAccountOrName, int[] rootType, String time, int sequence, Integer page, Integer limitNum) {
        List list = new ArrayList();
        StringBuilder sb = new StringBuilder();
        sb.append("(SELECT f.`name`AS memberName,f.`headimg`AS memberHeadimg,f.`account`AS memberAccount,SUM(z.`score`)AS memberScore,COUNT(*)AS memberCount");
        sb.append(",SUM(IF(z.`score`>0,1,0))AS winCount,SUM(IF(z.`score`=0,1,0))AS tiesCount,SUM(IF(z.`score`<0,1,0))AS loseCount,SUM(IF(z.`is_big_winner`=1,1,0))AS bigWinCount");
        sb.append(",z.`create_time`AS createTime,z.id gameRecordId,z.tick_status tickStatus FROM za_user_game_statis z LEFT JOIN za_users f ON z.`user_id`=f.`id`WHERE 1=1 ");
        if (null != time && !"".equals(time)) {
            sb.append(" AND (DATEDIFF(z.`create_time`,?)=0)");
            list.add(time);
        }
        if (null != rootType && rootType.length > 0) {
            sb.append(" AND z.`room_type` in(");
            for (int i = 0; i < rootType.length; i++) {
                sb.append("?");
                list.add(rootType[i]);
                if (i != rootType.length - 1) {
                    sb.append(",");
                }
            }
            sb.append(") ");
        }
        if (null != teaInfoCode) {
            sb.append(" AND z.`circle_code`= ?");
            list.add(teaInfoCode);
        }

        if (null != usersAccountOrName && !"".equals(usersAccountOrName)) {
            sb
                    .append(" AND CONCAT (f.`account`, IFNULL(f.`name`, ''))LIKE '%")
                    .append(usersAccountOrName)
                    .append("%'");
        }

        sb.append("  GROUP BY z.`user_id`)f ");
        //战榜排序
        if (sequence == TeaConstant.BATTLE_LIST_SEQUENCE_MIN) {
            sb.append(" ORDER BY f.`memberScore`");
        } else {
            sb.append(" ORDER BY f.`memberScore` DESC");
        }
        return DBUtil.getObjectPageBySQL("*", sb.toString(), list.toArray(), page, limitNum);
    }

    @Override
    public JSONObject getMemberRecord(String teaInfoCode, String usersId, int[] rootType, String time) {

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT GROUP_CONCAT('$',f.`memberId`,'$',f.`memberScore`,'|',f.`memberBigWinCount`)AS ids ");
        sb.append("FROM(SELECT*FROM(SELECT z.`user_id`AS memberId,SUM(z.`score`)AS memberScore,SUM(IF(z.`is_big_winner`=1,1,0))AS memberBigWinCount FROM za_user_game_statis z WHERE 1=1 ");
        List list = new ArrayList();
        if (null != time && !"".equals(time)) {
            sb.append(" AND (DATEDIFF(z.`create_time`,?)=0)");
            list.add(time);
        }
        if (null != rootType && rootType.length > 0) {
            sb.append(" AND z.`room_type` in(");
            for (int i = 0; i < rootType.length; i++) {
                sb.append("?");
                list.add(rootType[i]);
                if (i != rootType.length - 1) {
                    sb.append(",");
                }
            }
            sb.append(") ");
        }
        if (null != teaInfoCode) {
            sb.append(" AND z.`circle_code`= ?");
            list.add(teaInfoCode);
        }

        sb.append(" GROUP BY z.`user_id`)f ORDER BY f.`memberScore`DESC)f");
        JSONObject object = DBUtil.getObjectBySQL(sb.toString(), list.toArray());

        String memberRanking = "无";
        String memberScore = "0";
        String memberBigWinCount = "0";
        if (null != object && object.containsKey("ids")) {
            String ids = object.getString("ids");
            String[] memberList = ids.split(",");
            if (null != memberList && memberList.length > 0) {
                String memberId = "$" + usersId + "$";
                for (int i = 0; i < memberList.length; i++) {
                    String str = memberList[i];
                    if (str.contains(memberId)) {
                        str.replace(memberId, "");
                        String[] s = str.split("|");
                        memberRanking = "" + (i + 1);
                        memberScore = s[0];
                        memberBigWinCount = s[1];
                        break;
                    }
                }
            }
        }
        JSONObject result = new JSONObject();
        result
                .element("memberRanking", memberRanking)
                .element("memberScore", memberScore)
                .element("memberBigWinCount", memberBigWinCount)
        ;

        return result;
    }

    @Override
    public JSONObject pageTeaMumberBigWinRecord(String teaInfoCode, String usersAccountOrName, int[] rootType, String time, Integer page, Integer limitNum) {
        List list = new ArrayList();
        StringBuilder sb = new StringBuilder();
        sb.append("(SELECT f.`name`AS memberName,f.`account`AS memberAccount,f.`headimg`AS memberHeadimg");
        sb.append(",SUM(z.`is_big_winner`)AS bigWinCount FROM za_user_game_statis z LEFT JOIN za_users f ON z.`user_id`=f.`id`WHERE z.`is_big_winner`=1 ");
        if (null != time && !"".equals(time)) {
            sb.append(" AND (DATEDIFF(z.`create_time`,?)=0)");
            list.add(time);
        }
        if (null != rootType && rootType.length > 0) {
            sb.append(" AND z.`room_type` in(");
            for (int i = 0; i < rootType.length; i++) {
                sb.append("?");
                list.add(rootType[i]);
                if (i != rootType.length - 1) {
                    sb.append(",");
                }
            }
            sb.append(") ");
        }
        if (null != teaInfoCode) {
            sb.append(" AND z.`circle_code`= ?");
            list.add(teaInfoCode);
        }

        if (null != usersAccountOrName && !"".equals(usersAccountOrName)) {
            sb
                    .append(" AND CONCAT (f.`account`, IFNULL(f.`name`, ''))LIKE '%")
                    .append(usersAccountOrName)
                    .append("%'");
        }

        sb.append("  GROUP BY z.`user_id`)f ORDER BY f.`bigWinCount`DESC");
        return DBUtil.getObjectPageBySQL("*", sb.toString(), list.toArray(), page, limitNum);
    }

    @Override
    public JSONObject getMemberBigWinRecord(String teaInfoCode, String usersId, int[] rootType, String time) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT GROUP_CONCAT('$',f.`memberId`,'$',f.`bigWinCount`)AS ids FROM(SELECT*FROM(SELECT t.`id`AS memberId,t.`name`AS memberName,t.`account`AS memberAccount");
        sb.append(",t.`headimg`AS memberHeadimg,SUM(z.`is_big_winner`)AS bigWinCount FROM za_user_game_statis z LEFT JOIN za_users t ON z.`user_id`=t.`id`WHERE z.`is_big_winner`=1 ");
        List list = new ArrayList();
        if (null != time && !"".equals(time)) {
            sb.append(" AND (DATEDIFF(z.`create_time`,?)=0)");
            list.add(time);
        }
        if (null != rootType && rootType.length > 0) {
            sb.append(" AND z.`room_type` in(");
            for (int i = 0; i < rootType.length; i++) {
                sb.append("?");
                list.add(rootType[i]);
                if (i != rootType.length - 1) {
                    sb.append(",");
                }
            }
            sb.append(") ");
        }
        if (null != teaInfoCode) {
            sb.append(" AND z.`circle_code`= ?");
            list.add(teaInfoCode);
        }

        sb.append(" GROUP BY z.`user_id`)f ORDER BY f.`bigWinCount`DESC)f");
        JSONObject object = DBUtil.getObjectBySQL(sb.toString(), list.toArray());
        String memberRanking = "无";
        String memberBigWinCount = "0";
        if (null != object && object.containsKey("ids")) {
            String ids = object.getString("ids");
            String[] memberList = ids.split(",");
            if (null != memberList && memberList.length > 0) {
                String memberId = "$" + usersId + "$";
                for (int i = 0; i < memberList.length; i++) {
                    String str = memberList[i];
                    if (str.contains(memberId)) {
                        str = str.replace(memberId, "");
                        memberRanking = "" + (i + 1);
                        memberBigWinCount = str;
                        break;
                    }
                }
            }
        }
        JSONObject result = new JSONObject();
        result
                .element("memberRanking", memberRanking)
                .element("memberBigWinCount", memberBigWinCount)
        ;

        return result;
    }

    @Override
    public void generationTeaRoom(JSONObject object) {
        if (!object.containsKey("teaInfoCode") || !object.containsKey("used") || !object.containsKey("unused")) {
            return;
        }
        String teaInfoCode = object.getString("teaInfoCode");
        int used = object.getInt("used");//已使用房间个数
        int unused = object.getInt("unused");//未使用房间个数

        //房间数量验证
        if (unused >= TeaConstant.MIN_ROOT) {
            redisInfoService.delExistCreateRoom(teaInfoCode);
            return;
        }

        //验证是否允许开房间 start
        JSONObject teaInfo = DBUtil.getObjectBySQL("SELECT t.`type`,t.`base_info`,t.`id`,t.`users_id`,z.`account`,z.`platform`  FROM tea_info t LEFT JOIN za_users z  ON t.`users_id`=z.`id` WHERE t.`audit`= ? AND t.`is_deleted`= ? AND t.`tea_code`= ?"
                , new Object[]{TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N, teaInfoCode});
        if (null == teaInfo || !teaInfo.containsKey("type") || !teaInfo.containsKey("base_info") || !teaInfo.containsKey("id") || !teaInfo.containsKey("users_id") || !teaInfo.containsKey("account") || !teaInfo.containsKey("platform")
                || TeaConstant.TEA_INFO_TYPE_DY == teaInfo.getInt("type")) {
            redisInfoService.delExistCreateRoom(teaInfoCode);
            return;
        }
        //验证是否允许开房间 end
        //需要生成的房间数量
        int roomCount;
        if (used + unused >= TeaConstant.MIN_ROOT) {
            roomCount = TeaConstant.MIN_ROOT_COUNT - unused;
        } else {
            roomCount = TeaConstant.MIN_ROOT - used - unused;
        }
        if (roomCount < 1) {
            redisInfoService.delExistCreateRoom(teaInfoCode);
            return;
        }
        log.info("茶楼号：[{}] 茶楼自动生成房间数量：[{}]", teaInfoCode, roomCount);

        object = new JSONObject();
        redisInfoService.addExistCreateRoom(teaInfoCode, roomCount);

        for (int i = 0; i < roomCount; i++) {
            object
                    .element("base_info", teaInfo.getJSONObject("base_info"))
                    .element("teaInfoCode", teaInfoCode)
                    .element("circleId", teaInfo.get("id"))
                    .element("platform", teaInfo.get("platform"))
                    .element("account", teaInfo.get("account"))
                    .element("users_id", teaInfo.get("users_id"))
            ;
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.TEA_GAME_ROMM_CREATE, object));
        }
    }

    @Override
    public JSONObject getTeaRoomcardSpend(Object rommId, Object roomNo, Object[] payType, Object usersId) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT SUM(z.`tea_money`)AS recordInfo,z.`pay_type` AS payType FROM tea_member_money_record z WHERE 1=1 ");
        List list = new ArrayList();
        if (null != rommId) {
            sb.append(" AND z.`za_gamerooms_id`= ?  ");
            list.add(rommId);
        }
        if (null != roomNo) {
            sb.append(" AND z.`room_no`= ?  ");
            list.add(roomNo);
        }
        if (null != usersId) {
            sb.append(" AND z.`users_id`= ?  ");
            list.add(usersId);
        }

        if (null != payType && payType.length > 0) {
            sb.append(" AND z.`pay_type` in(");
            for (int i = 0; i < payType.length; i++) {
                sb.append("?");
                list.add(payType[i]);
                if (i != payType.length - 1) {
                    sb.append(",");
                }
            }
            sb.append(") ");
        }

        JSONObject object = DBUtil.getObjectBySQL(sb.toString(), list.toArray());
        return null != object ? object : new JSONObject();
    }

    @Override
    public JSONArray getTeaRoomcardAllSpend(Object teaInfoId, String time, Object[] payType, Object usersId) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT SUM(z.`tea_money`)AS teaMoneyAll,z.`pay_type` AS payType FROM tea_member_money_record z WHERE 1=1 AND(DATEDIFF(z.`create_time`,?)=0)");
        List list = new ArrayList();
        list.add(time);
        if (null != teaInfoId) {
            sb.append(" AND z.`tea_info_id`= ?  ");
            list.add(teaInfoId);
        }
        sb.append(" AND z.`source_type`= ?  ");
        list.add(TeaConstant.TEA_MEMBER_MONEY_RECORD_SOURCE_TYPE_XH);
        if (null != usersId) {
            sb.append(" AND z.`users_id`= ?  ");
            list.add(usersId);
        }

        if (null != payType && payType.length > 0) {
            sb.append(" AND z.`pay_type` in(");
            for (int i = 0; i < payType.length; i++) {
                sb.append("?");
                list.add(payType[i]);
                if (i != payType.length - 1) {
                    sb.append(",");
                }
            }
            sb.append(") ");
        }

        sb.append(" GROUP BY z.`pay_type` ");
        return DBUtil.getObjectListBySQL(sb.toString(), list.toArray());
    }


    @Override
    public int verifyJoinRoom(int roomCard, int playerCount, String payType, Object teaInfoCode, Object usersId) {
        JSONObject teaMember = DBUtil.getObjectBySQL("SELECT z.is_use useStatus,z.`score_lock`,z.`score_all`,z.`score_time`,t.`proxy_money`,z.`tea_money`,z.`id`,z.`powern`,f.`account`FROM tea_member z LEFT JOIN za_users f ON z.`users_id`=f.`id` LEFT JOIN tea_info t ON t.`id`=z.`tea_info_id` WHERE z.`tea_info_code`= ? AND z.`users_id`= ? AND z.`audit`=? AND z.`is_deleted`= ?"
                , new Object[]{teaInfoCode, usersId, TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
        if (null == teaMember || !teaMember.containsKey("account") || !teaMember.containsKey("tea_money")
                || !teaMember.containsKey("proxy_money")) {
            return TeaConstant.JOIN_ROOM_TYPE_NO;
        }
        //黑名单控制
        if (!teaMember.containsKey("useStatus") || TeaConstant.IS_USE_NO.equals(teaMember.getString("useStatus"))) {
            return TeaConstant.JOIN_ROOM_TYPE_BLACK_LIST;
        }
        //得分锁控制 start
        if (teaMember.containsKey("score_lock") && teaMember.containsKey("score_all") && teaMember.containsKey("score_time")) {
            if (0 < teaMember.getInt("score_lock")) {
                int scoreLock = teaMember.getInt("score_lock");
                int scoreAll = teaMember.getInt("score_all");
                String scoreTime = teaMember.getString("score_time");//最后得分时间
                long t1 = DateUtils.getLongTime(scoreTime, "yyyy-MM-dd");
                long todayTime = DateUtils.getLongTime("yyyy-MM-dd");//当日时间
                if (todayTime == t1) {//当日 限制
                    if (-scoreAll >= scoreLock) {
                        return TeaConstant.JOIN_ROOM_TYPE_SCORE_LOCK_NO;
                    }
                }
            }

        }
        //得分锁控制 end

        int proxyMoney = teaMember.getInt("proxy_money");//茶楼余额
        int teaMoney = teaMember.getInt("tea_money");//当前用户余额
        if (CommonConstant.ROOM_PAY_TYPE_TEA.equals(payType)) {//茶壶支付
            if (proxyMoney < roomCard * playerCount) {
                return TeaConstant.JOIN_ROOM_TYPE_NO_1;
            }
        } else if (CommonConstant.ROOM_PAY_TYPE_TEA_AA.equals(payType)) {//AA
            if (teaMoney < roomCard) {
                return TeaConstant.JOIN_ROOM_TYPE_NO_1;
            }
        } else {
            return TeaConstant.JOIN_ROOM_TYPE_NO;
        }
        return TeaConstant.JOIN_ROOM_TYPE_YES;
    }

    @Override
    public int deductTeaMoney(String roomNo, Object usersId, String teInfoId, String payType, int eachPerson) {
        List teaLIist = new ArrayList();
        teaLIist.add(teInfoId);
        teaLIist.add(TeaConstant.AUDIT_Y);
        teaLIist.add(TeaConstant.IS_DELETE_N);
        String sql = "SELECT t.`platform`,t.`tea_code`,t.`proxy_money`,t.`type` FROM tea_info t  WHERE t.`id`= ? AND t.`audit`= ? AND t.`is_deleted`= ? ";
        if (CommonConstant.ROOM_PAY_TYPE_TEA_AA.equals(payType)) {//AA
            sql = "SELECT t.`platform`,t.`tea_code`,z.`id` AS memberId,z.`tea_money`,t.`proxy_money`,t.`type` FROM tea_info t LEFT JOIN tea_member z ON t.`id`=z.`tea_info_id` WHERE t.`id`= ? AND t.`audit`= ? AND t.`is_deleted`= ?  AND z.`users_id`= ? AND z.`audit`= ? AND z.`is_deleted`= ?";
            teaLIist.add(usersId);
            teaLIist.add(TeaConstant.AUDIT_Y);
            teaLIist.add(TeaConstant.IS_DELETE_N);
        }
        JSONObject teaInfo = DBUtil.getObjectBySQL(sql, teaLIist.toArray());
        if (null == teaInfo || !teaInfo.containsKey("proxy_money") || !teaInfo.containsKey("type")) {
            return TeaConstant.START_ROOM_NO;
        }
        if (TeaConstant.TEA_INFO_TYPE_DY == teaInfo.getInt("type")) {
            return TeaConstant.START_ROOM_NO_1;//茶楼打烊
        }
        int proxyMoney = teaInfo.getInt("proxy_money");//茶楼余额
        int teaMoney = teaInfo.containsKey("tea_money") ? teaInfo.getInt("tea_money") : 0;//当前用户余额

        if (CommonConstant.ROOM_PAY_TYPE_TEA.equals(payType)) {//茶壶支付
            if (proxyMoney < eachPerson) {
                return TeaConstant.START_ROOM_NO_3;
            }
        } else if (CommonConstant.ROOM_PAY_TYPE_TEA_AA.equals(payType)) {//AA
            if (!teaInfo.containsKey("tea_money") || !teaInfo.containsKey("memberId")) {
                return TeaConstant.START_ROOM_NO_2;//你已经被提出茶楼
            }
            if (teaMoney < eachPerson) {
                return TeaConstant.START_ROOM_NO_3;
            }
        } else {
            return TeaConstant.START_ROOM_NO;
        }

        //更新茶叶
        StringBuilder sb = new StringBuilder();
        List list = new ArrayList();
        list.add(eachPerson);
        int oldMoney;//旧

        if (CommonConstant.ROOM_PAY_TYPE_TEA.equals(payType)) {//茶壶支付
            sb.append("UPDATE tea_info SET `proxy_money`= `proxy_money` - ? WHERE id = ?");
            list.add(teInfoId);
            oldMoney = proxyMoney;
        } else {
            sb.append("UPDATE tea_member SET `tea_money`= `tea_money` - ? WHERE id = ?");
            list.add(teaInfo.get("memberId"));
            oldMoney = teaMoney;
        }
        int newMoney = oldMoney - eachPerson;//新

        int i = DBUtil.executeUpdateBySQL(sb.toString(), list.toArray());
        if (i > 0) {
            //添加记录
            JSONObject object = new JSONObject();
            JSONObject gamerooms = DBUtil.getObjectBySQL("SELECT `id`,`game_id` FROM za_gamerooms WHERE room_no=? ORDER BY id DESC", new Object[]{roomNo});
            object
                    .element("tea_info_id", teInfoId)
                    .element("tea_info_code", teaInfo.get("tea_code"))
                    .element("za_gamerooms_id", gamerooms.get("id"))
                    .element("gid", gamerooms.get("game_id"))
                    .element("room_no", roomNo)
                    .element("tea_money", -eachPerson)
                    .element("old_money", oldMoney)
                    .element("new_money", newMoney)
                    .element("pay_type", payType)
                    .element("source_type", TeaConstant.TEA_MEMBER_MONEY_RECORD_SOURCE_TYPE_XH)
                    .element("platform", teaInfo.get("platform"))
            ;
            if (CommonConstant.ROOM_PAY_TYPE_TEA_AA.equals(payType)) {//AA
                object.element("tea_member_id", teaInfo.get("memberId"))
                        .element("users_id", usersId);
            }
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_TEA_MEMBER_MONEY_RECORD, object));
            return TeaConstant.START_ROOM_YES;
        }
        return TeaConstant.START_ROOM_NO_3;
    }

    @Override
    public void insertTeaMemberMoneyRecord(JSONObject object) {
        DBJsonUtil.add(object, "tea_member_money_record");
    }

    @Override
    public JSONObject getMoneyRecordLimitPage(String teaInfoId, int type, String time, Integer page, Integer limitNum) {
        List list = new ArrayList();
        StringBuilder sb = new StringBuilder();
        sb.append("tea_member_money_record t LEFT JOIN za_users z ON t.`users_id`=z.`id`WHERE  ");
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

        sb.append(" ORDER BY t.`create_time`DESC");

        return DBUtil.getObjectPageBySQL("z.`name`,t.`room_no` AS roomNo,t.`tea_money`AS teaMoney,t.`pay_type`AS payType,t.`source_type`AS sourceType,t.`create_time`AS createTime"
                , sb.toString(), list.toArray(), page, limitNum);
    }

    @Override
    public JSONObject getSysValue(String platform) {
        return redisInfoService.getSysInfo(platform);
    }


    @Override
    public int updateTeaRecordStatiTick(String gameRecordId, String tickStatus, String teaCode) {
        //勾选战榜操作
        String sql = "update za_user_game_statis a set a.tick_status = ? where id = ? and  circle_code = ?";
        return DBUtil.executeUpdateBySQL(sql, new Object[]{tickStatus, gameRecordId, teaCode});
    }

    @Override
    public void updateTeaMemberScoreALl(JSONArray array, String teaInfoId) {
        try {
            if (null == array || array.size() == 0 || null == teaInfoId) return;

            for (int i = 0; i < array.size(); i++) {
                JSONObject teaObject = array.getJSONObject(i);
                if (null == teaObject || !teaObject.containsKey("user_id"))
                    continue;
                JSONObject teaMember = DBUtil.getObjectBySQL("SELECT t.`id`,t.`score_all`,t.`score_lock`,t.`score_time`FROM tea_member t WHERE t.`tea_info_id`=? AND t.`users_id`=? AND t.`audit`=? AND t.`is_deleted`=?"
                        , new Object[]{teaInfoId, teaObject.get("user_id"), TeaConstant.AUDIT_Y, TeaConstant.IS_DELETE_N});
                if (null == teaMember || !teaMember.containsKey("score_lock"))
                    continue;

                String scoreTime = teaMember.getString("score_time");//最后得分时间
                long t1 = DateUtils.getLongTime(scoreTime, "yyyy-MM-dd");
                long todayTime = DateUtils.getLongTime("yyyy-MM-dd");//当日时间
                double score = teaObject.getDouble("score");
                if (0D == score) continue;

                if (todayTime == t1) {//相同时间
                    DBUtil.executeUpdateBySQL("UPDATE tea_member SET `score_time`=?,`score_all`= `score_all` + ? WHERE id = ?"
                            , new Object[]{DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"), score, teaMember.get("id")});
                } else {
                    DBUtil.executeUpdateBySQL("UPDATE tea_member SET `score_time`=?,`score_all`= ? WHERE id = ?"
                            , new Object[]{DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"), score, teaMember.get("id")});
                }
            }

        } catch (Exception e) {
            log.info("茶楼分数锁保存报错[{}]", e);
        }
    }

}
