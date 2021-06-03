
package com.zhuoan.biz.game.dao.impl;

import com.zhuoan.biz.game.dao.CircleDao;
import com.zhuoan.constant.CircleConstant;
import com.zhuoan.dao.DBJsonUtil;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.util.TimeUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Resource;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CircleDaoImpl implements CircleDao {
    private static final Logger log = LoggerFactory.getLogger(CircleDaoImpl.class);
    @Resource
    private RedisInfoService redisInfoService;

    public CircleDaoImpl() {
    }

    public long saveOrUpdateAuditRecord(JSONObject jsonObject) {
        return DBJsonUtil.saveOrUpdateGetId(jsonObject, "game_circle_audit_record");
    }

    public int saveOrUpdateFundBill(JSONObject jsonObject) {
        return DBJsonUtil.saveOrUpdate(jsonObject, "game_circle_fund_bill");
    }

    public int saveOrUpdateCircleInfo(JSONObject jsonObject) {
        if (jsonObject.get("id") != null) {
            this.redisInfoService.delGameCircleInfoByid(String.valueOf(jsonObject.get("id")));
        }

        return DBJsonUtil.saveOrUpdate(jsonObject, "game_circle_info");
    }

    public long insertCircleInfoReturnId(JSONObject jsonObject) {
        if (jsonObject.get("id") != null) {
            this.redisInfoService.delGameCircleInfoByid(String.valueOf(jsonObject.get("id")));
        }

        return DBJsonUtil.addGetId(jsonObject, "game_circle_info");
    }

    public long saveOrUpdateCircleMember(JSONObject jsonObject) {
        return DBJsonUtil.saveOrUpdateGetId(jsonObject, "game_circle_member");
    }

    public int saveOrUpdateCircleMsg(JSONObject jsonObject) {
        return DBJsonUtil.saveOrUpdate(jsonObject, "game_circle_message");
    }

    public int saveOrUpdateStstis(JSONObject jsonObject) {
        return DBJsonUtil.saveOrUpdate(jsonObject, "game_circle_statistics");
    }

    public int saveOrUpdateUserInfo(JSONObject jsonObject) {
        return DBJsonUtil.saveOrUpdate(jsonObject, "game_user_info");
    }

    public int saveOrUpdateUserPropBill(JSONObject jsonObject) {
        return DBJsonUtil.saveOrUpdate(jsonObject, "game_circle_prop_bill");
    }

    public int saveOrUpdateUserSensitive(JSONObject jsonObject) {
        return DBJsonUtil.saveOrUpdate(jsonObject, "game_user_sensitive_info");
    }

    public JSONObject getCircleInfoById(long circleId) {
        String sql = "select * from game_circle_info where id=? and is_delete='N'";
        return DBUtil.getObjectBySQL(sql, new Object[]{circleId});
    }

    public JSONObject getCircleMemberByUserId(Long userId, Long circleId, String platform) {
        String sql = "select a.*,b.`name`,b.account,b.headimg,b.area,c.circle_code,c.circle_name,c.circle_notice,c.fee_type,c.create_room_role,c.fund_prepaid_role,c.fund_bill_role,c.fund_count from game_circle_member a join za_users b on a.user_id=b.id join game_circle_info c on a.circle_id=c.id where a.user_id=? and a.platform=? and a.is_delete='N' and a.circle_id=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{userId, platform, circleId});
    }

    public JSONObject getCircleMemberByUserAccount(String account, Long circleId, String platform) {
        String sql = "select a.*,b.`name`,b.account,b.headimg,b.area,c.circle_code, c.circle_name,c.circle_notice,c.fee_type,c.create_room_role, c.fund_prepaid_role,c.fund_bill_role,c.fund_count from game_circle_member a join za_users b on a.user_id=b.id join game_circle_info c on a.circle_id=c.id  where b.account=?  and a.platform=? and a.is_delete='N' and a.circle_id=? ";
        return DBUtil.getObjectBySQL(sql, new Object[]{account, platform, circleId});
    }

    public int updateCircleMemberCreateUser(int userRole, int isAdmin, long modifyUser, long userId, long circleId, String platform) {
        String sql = "UPDATE game_circle_member SET user_role =? ,is_admin=? ,modify_user=? ,gmt_modified=? where user_id=? and circle_id=? and platform=? and is_delete = 'N'";
        return DBUtil.executeUpdateBySQL(sql, new Object[]{userRole, isAdmin, modifyUser, TimeUtil.getNowDate(), userId, circleId, platform});
    }

    public JSONArray queryCircleList(Long userId, String platform) {
        String sql = "SELECT b.`id`,a.id circle_id,b.`account`,b.`name`,c.`user_code`,a.`circle_name`,b.`headimg`, c.`user_role`,c.`superior_user_code`  FROM game_circle_info a  JOIN za_users b ON a.`create_user`=b.`id`  LEFT JOIN game_circle_member c ON  a.id=c.`circle_id`  WHERE  c.is_delete='N' and a.is_delete='N' AND a.circle_status = 2 AND c.`user_id`=? AND a.`platform`=?";
        return DBUtil.getObjectListBySQL(sql, new Object[]{userId, platform});
    }

    public int getCountCircleMemberByCircle(long circleId, String platform) {
        String sql = "select count(id)  from game_circle_member where circle_id = ? and platform=? and is_delete='N'";
        return ((JSONObject)Objects.requireNonNull(DBUtil.getObjectBySQL(sql, new Object[]{circleId, platform}))).getInt("count(id)");
    }

    public Integer getCountCircleByUser(long userId, String platform) {
        String sql = "select count(id)  from game_circle_info where create_user = ? and platform=? and is_delete = 'N'";
        return ((JSONObject)Objects.requireNonNull(DBUtil.getObjectBySQL(sql, new Object[]{userId, platform}))).getInt("count(id)");
    }

    public Integer getCountCircleByAccount(String account, String platform) {
        String sql = "select count(a.id) c  from game_circle_info a left join za_users b on a.create_user=b.id             where b.account = ? and a.platform=? and a.is_delete = 'N' ";
        return ((JSONObject)Objects.requireNonNull(DBUtil.getObjectBySQL(sql, new Object[]{account, platform}))).getInt("c");
    }

    public JSONObject getCircleInfoByCode(String code) {
        String sql = "select * from game_circle_info where circle_code=? and is_delete = 'N'";
        return DBUtil.getObjectBySQL(sql, new Object[]{code});
    }

    public JSONObject getCircleMemberByCode(String code) {
        String sql = "select id, circle_id, user_id, user_code, user_hp, superior_user_code, user_role, profit_ratio, profit_balance, is_admin, is_use, platform, create_user, modify_user, gmt_create, gmt_modified from game_circle_member where user_code=? and is_delete = 'N'";
        return DBUtil.getObjectBySQL(sql, new Object[]{code});
    }

    public JSONObject getZaUserInfoById(long id) {
        String sql = "select * from za_users where id=? ";
        return DBUtil.getObjectBySQL(sql, new Object[]{id});
    }

    public JSONArray getPartner(long circleId, long userId) {
        String sql = "SELECT c.id,c.user_role,b.name,b.headimg,b.id as userId,c.user_code FROM game_circle_member c LEFT JOIN za_users b ON c.user_id=b.id WHERE c.superior_user_code IN(SELECT a.user_code FROM game_circle_member a WHERE  a.circle_id =? AND a.user_id =?)";
        return DBUtil.getObjectListBySQL(sql, new Object[]{circleId, userId});
    }

    public JSONObject checkMemberExist(long userId, long circleId) {
        String sql = "select id,user_id,user_role,user_code,user_hp,profit_ratio,is_admin,profit_balance,platform,superior_user_code from game_circle_member where user_id = ? and circle_id=? and is_delete='N' ";
        return DBUtil.getObjectBySQL(sql, new Object[]{userId, circleId});
    }

    public JSONObject selectByCircleIdAndUsercode(long circleId, String userCode) {
        String sql = "select * from game_circle_member where  circle_id=? and user_code=? and is_delete='N' ";
        JSONObject sysUserInfo = DBUtil.getObjectBySQL(sql, new Object[]{circleId, userCode});
        return sysUserInfo;
    }

    public void addPartner(JSONObject operatorUser) {
        DBJsonUtil.saveOrUpdate(operatorUser, "game_circle_member");
    }

    public JSONObject getUserPropBill(long circleId, long userId, Object[] changeType, String platform, int pageIndex, int pageSize) {
        StringBuilder sb = new StringBuilder();
        sb.append("game_circle_prop_bill where circle_id=? and user_id = ?  and platform=? and TO_DAYS(NOW()) - TO_DAYS(create_time) < 4 ");
        List list = new ArrayList();
        list.add(circleId);
        list.add(userId);
        list.add(platform);
        if (null != changeType && changeType.length > 0) {
            sb.append(" AND change_type in(");

            for(int i = 0; i < changeType.length; ++i) {
                sb.append("?");
                list.add(changeType[i]);
                if (i != changeType.length - 1) {
                    sb.append(",");
                }
            }

            sb.append(") ");
        }

        sb.append(" ORDER BY create_time DESC");
        return DBUtil.getObjectLimitPageBySQL("*", sb.toString(), list.toArray(), pageIndex, pageSize, CircleConstant.MAX_PAGE_COUNT);
    }

    public JSONArray getYestodayAndTodayPropBill(long circleId, long userId, String changeType1, String changeType2, String changeType3, String platform) {
        String sql = "select sum(change_count)changeCount,DATE_FORMAt(create_time,'%Y-%m-%d')createTime FROM game_circle_prop_bill where circle_id=? and user_id = ? and (change_type=? or change_type=? or change_type=?) and platform=? and TO_DAYS(NOW()) - TO_DAYS(create_time) <= 1 group by DATE_FORMAt(create_time,'%Y-%m-%d')";
        return DBUtil.getObjectListBySQL(sql, new Object[]{circleId, userId, changeType1, changeType2, changeType3, platform});
    }

    public JSONObject getMemberStatistics(int userRole, long circleId, long userId, int pageIndex, int pageSize) {
        String sql = "select a.free_count,a.inning_count,e.user_name,b.user_id,d.circle_code,e.account,b.is_admin from game_circle_member b LEFT JOIN game_circle_info d on b.circle_id=d.id LEFT JOIN game_circle_statistics a ON a.user_code = b.user_code left join za_users e on b.user_id=e.id where b.is_delete='N' and b.user_role=? and b.superior_user_code IN(SELECT c.user_code FROM game_circle_member c WHERE c.circle_id = ? AND c.user_id = ?)";
        return DBUtil.getObjectPageBySQL("b.superior_user_code,a.free_count,a.inning_count,e.name as user_name,b.user_id,d.circle_code,e.account,b.is_admin", "game_circle_member b LEFT JOIN game_circle_info d on b.circle_id=d.id LEFT JOIN game_circle_statistics a ON a.user_code = b.user_code left join za_users e on b.user_id=e.id where b.is_delete='N' and b.user_role=? and b.superior_user_code IN(SELECT c.user_code FROM game_circle_member c WHERE c.circle_id = ? AND c.user_id = ? )", new Object[]{userRole, circleId, userId}, pageIndex, pageSize);
    }

    public JSONObject getAllMemberStatistics(int userRole, long circleId, int pageIndex, int pageSize) {
        return DBUtil.getObjectPageBySQL("b.superior_user_code,a.free_count,a.inning_count,e.name as user_name,b.user_id,d.circle_code,e.account,b.is_admin", "game_circle_member b LEFT JOIN game_circle_info d on b.circle_id=d.id LEFT JOIN game_circle_statistics a ON a.user_code = b.user_code left join za_users e on b.user_id=e.id where b.is_delete='N' and b.user_role=? and b.circle_id = ? ", new Object[]{userRole, circleId}, pageIndex, pageSize);
    }

    public JSONArray getYesterdayGameLog(long userId, String clubCode, int roomType1, int roomType2) {
        String sql = "select user_id,account,room_no,room_type,game_index from za_usergamelogs a  where TO_DAYS(NOW()) - TO_DAYS(createtime) = 1 and user_id=? and club_code=? and (room_type=? or room_type=?)";
        return DBUtil.getObjectListBySQL(sql, new Object[]{userId, clubCode, roomType1, roomType2});
    }

    public JSONObject getOperatorSystemsettingByPlatform(String platform) {
        String sql = "select * from operator_systemsetting where platform =? ORDER BY id DESC LIMIT 1";
        return DBUtil.getObjectBySQL(sql, new Object[]{platform});
    }

    public JSONObject getTodayHpChange(long userId, long circleId, String changeType1, String changeType2) {
        String sql = "SELECT IF(SUM(change_count) IS NULL,0.0,SUM(change_count)) change_count FROM game_circle_prop_bill WHERE date(create_time)= curdate() AND user_id=? AND circle_id=? AND (change_type=? or change_type=?)";
        return DBUtil.getObjectBySQL(sql, new Object[]{userId, circleId, changeType1, changeType2});
    }

    public JSONObject getUserTotalPlay(long userId, long circleId, long memberId) {
        String sql = "SELECT IF(SUM(free_count)+SUM(inning_count) IS NULL,0,SUM(free_count)+SUM(inning_count)) count  FROM game_circle_statistics WHERE  user_id=? AND circle_id=? AND member_id=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{userId, circleId, memberId});
    }

    public JSONObject getYestodayHpFee(long userId, long circleId, String changeType) {
        String sql = "SELECT sum(change_count)change_count from game_circle_prop_bill where TO_DAYS(NOW()) - TO_DAYS(create_time) = 1 and user_id=? AND circle_id=? and change_type=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{userId, circleId, changeType});
    }

    public void transferProfitBalanceToHp(JSONObject jsonObject) {
        DBJsonUtil.saveOrUpdate(jsonObject, "game_circle_member");
    }

    public JSONObject getMemberStatisticsPage(long circleId, String userCode, int userRole, int pageIndex, int pageSize) {
        String sql = "SELECT a.*,b.name,b.account,c.superior_user_code FROM game_circle_statistics a LEFT JOIN za_users b ON a.user_id=b.id AND a.del=0 WHERE a.del=0 AND a.circle_id=?  AND a.user_id in(SELECT user_id FROM game_circle_member WHERE superior_user_code=? AND user_role=?)";
        return DBUtil.getObjectPageBySQL("c.superior_user_code,c.is_admin,c.user_code,c.id member_id , c.circle_id,c.user_id,IF(a.free_count IS NULL,0,a.free_count)free_count,IF(a.inning_count IS NULL,0,a.inning_count)inning_count, IF(a.free_deduction IS NULL,0,a.free_deduction)free_deduction,IF(a.inning_deduction IS NULL,0,a.inning_deduction)inning_deduction,IF(a.big_win IS NULL,0,a.big_win)big_win,b.name user_name,b.account id", "game_circle_member c LEFT JOIN game_circle_statistics a ON  c.user_id=a.user_id AND a.circle_id = c.circle_id AND TO_DAYS(NOW()) = TO_DAYS(a.create_time) AND c.is_delete = 'N' LEFT JOIN za_users b ON c.user_id=b.id WHERE c.circle_id=? AND c.superior_user_code=? AND c.user_role=? AND c.is_delete = 'N'", new Object[]{circleId, userCode, userRole}, pageIndex, pageSize);
    }

    public JSONObject getAllMemberStatisticsPage(long circleId, int userRole, int pageIndex, int pageSize) {
        return DBUtil.getObjectPageBySQL("c.superior_user_code,c.user_code,c.is_admin, c.id member_id , c.circle_id,c.user_id,IF(a.free_count IS NULL,0,a.free_count)free_count,IF(a.inning_count IS NULL,0,a.inning_count)inning_count, IF(a.free_deduction IS NULL,0,a.free_deduction)free_deduction,IF(a.inning_deduction IS NULL,0,a.inning_deduction)inning_deduction,IF(a.big_win IS NULL,0,a.big_win)big_win,b.name user_name,b.account id", "game_circle_member c LEFT JOIN game_circle_statistics a ON  c.user_id=a.user_id AND a.circle_id = c.circle_id AND TO_DAYS(NOW()) = TO_DAYS(a.create_time) AND c.is_delete = 'N' LEFT JOIN za_users b ON c.user_id=b.id  WHERE c.circle_id=? AND c.user_role=? AND c.is_delete = 'N'", new Object[]{circleId, userRole}, pageIndex, pageSize);
    }

    public JSONObject getOneMemberStatisticsPage(long circleId, int userRole, long userAccount, int pageIndex, int pageSize) {
        String sql = "SELECT a.*,c.superior_user_code,c.user_code,b.NAME FROM game_circle_statistics a LEFT JOIN za_users b ON a.user_id = b.id LEFT JOIN game_circle_member c ON a.user_id = c.user_id AND a.circle_id = c.circle_id AND c.is_delete = 'N' WHERE a.circle_id=10 AND c.user_role=3 AND  a.user_id IN(SELECT id FROM za_users  WHERE account=10229535)";
        return DBUtil.getObjectPageBySQL("c.superior_user_code,c.user_code,c.is_admin, c.id member_id , c.circle_id,c.user_id,IF(a.free_count IS NULL,0,a.free_count)free_count,IF(a.inning_count IS NULL,0,a.inning_count)inning_count, IF(a.free_deduction IS NULL,0,a.free_deduction)free_deduction,IF(a.inning_deduction IS NULL,0,a.inning_deduction)inning_deduction,IF(a.big_win IS NULL,0,a.big_win)big_win,b.name user_name,b.account id", "game_circle_member c LEFT JOIN game_circle_statistics a ON  c.user_id=a.user_id AND a.circle_id = c.circle_id AND TO_DAYS(NOW()) = TO_DAYS(a.create_time) AND c.is_delete = 'N' LEFT JOIN za_users b ON c.user_id=b.id  WHERE c.circle_id=? AND c.user_role=? AND c.user_id IN(SELECT id FROM za_users  WHERE account=?) AND c.is_delete = 'N'", new Object[]{circleId, userRole, userAccount}, pageIndex, pageSize);
    }

    public JSONObject getYestodayPaly(long circleId, long circleId1, int userRole, String userCode) {
        String sql = "SELECT IF(SUM(free_count)+SUM(inning_count) IS NULL,0,SUM(free_count)+SUM(inning_count)) count FROM game_circle_statistics  WHERE circle_id=? AND user_id in(SELECT user_id FROM game_circle_member WHERE circle_id=? AND user_role=? AND superior_user_code=? AND is_delete='N') AND TO_DAYS(NOW()) - TO_DAYS(create_time) < 1";
        return DBUtil.getObjectBySQL(sql, new Object[]{circleId, circleId1, userRole, userCode});
    }

    public JSONObject getAllYestodayPaly(long circleId, long circleId1, int userRole) {
        String sql = "SELECT IF(SUM(free_count)+SUM(inning_count) IS NULL,0,SUM(free_count)+SUM(inning_count)) count FROM game_circle_statistics  WHERE circle_id=? AND user_id in(SELECT user_id FROM game_circle_member WHERE circle_id=? AND user_role=? AND is_delete='N') AND TO_DAYS(NOW()) - TO_DAYS(create_time) < 1";
        return DBUtil.getObjectBySQL(sql, new Object[]{circleId, circleId1, userRole});
    }

    public JSONObject getYestodayPalyNum(long userId, String clubCode, int roomType1, int roomType2) {
        String sql = "SELECT COUNT(b.room_no)total FROM za_usergamelogs b WHERE  user_id in(SELECT user_id FROM game_circle_member WHERE superior_user_code IN ( SELECT user_code FROM game_circle_member  WHERE  user_id=? )) AND club_code =? AND (room_type = ? OR room_type = ?) AND TO_DAYS(NOW()) - TO_DAYS(createtime) = 1";
        return DBUtil.getObjectBySQL(sql, new Object[]{userId, clubCode, roomType1, roomType2});
    }

    public JSONObject getSuperUserDetailMessageByCode(String userCode) {
        String sql = "select a.*,b.name,b.account from game_circle_member a join za_users b on a.user_id = b.id where a.user_code=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{userCode});
    }

    public JSONObject getMemberInfoPage(String circleId, String userRole, String userCode, String type, String circleCode, List userIds, int[] roomType, long userAccount, String condition, String time, String orderByTotalScore, int pageIndex, int pageSize) {
        StringBuilder sb = new StringBuilder();
        sb.append("(SELECT c.is_use,c.superior_user_code,c.id,b.headimg,c.user_hp,c.profit_ratio,c.profit_balance_all,c.`user_role`,c.`is_admin`,c.`user_id`,b.`name`,b.`account`,IF(f.`totalNumber`IS NULL,0,f.`totalNumber`)AS totalNumber,IF(f.`totalScore`IS NULL,0,f.`totalScore`)AS totalScore,IF(f.`bigWinnerCount`IS NULL,0,f.`bigWinnerCount`)AS bigWinnerCount,IF(f.`bigWinnerScore`IS NULL,0,f.`bigWinnerScore`)AS bigWinnerScore FROM game_circle_member c LEFT JOIN za_users b ON c.`user_id`=b.`id`LEFT JOIN(SELECT COUNT(*)AS totalNumber,l.`user_id`,SUM(l.`score`)AS totalScore,SUM(IF(l.`is_big_winner`=0,0,1))AS bigWinnerCount,SUM(IF(l.`is_big_winner`=0,0,l.`score`))AS bigWinnerScore FROM za_user_game_statis l WHERE ").append("l.`circle_code`= ? AND (DATEDIFF(l.`create_time`,?)=0)  ");
        List list = new ArrayList();
        list.add(circleCode);
        list.add(time);
        int i;
        if (null != roomType && roomType.length > 0) {
            sb.append(" AND l.`room_type` in(");

            for(i = 0; i < roomType.length; ++i) {
                sb.append("?");
                list.add(roomType[i]);
                if (i != roomType.length - 1) {
                    sb.append(",");
                }
            }

            sb.append(") ");
        }

        sb.append("GROUP BY l.`user_id`)f ON c.`user_id`=f.`user_id`WHERE c.`circle_id`= ? AND c.`is_delete`='N'");
        list.add(circleId);
        if (userAccount > 0L) {
            sb.append(" AND b.`account`= ? ");
            list.add(userAccount);
        }

        if ("1".equals(condition)) {
            sb.append(" AND  c.superior_user_code = ? ");
            list.add(userCode);
        } else if ("2".equals(condition)) {
            sb.append(" AND  c.superior_user_code != ? ");
            list.add(userCode);
        }

        if ("2".equals(type) && !"2".equals(userRole)) {
            sb.append(" AND c.`user_role` != 2 ");
            if (null != userIds && userIds.size() > 0) {
                sb.append(" AND c.`user_id` not in(");

                for(i = 0; i < userIds.size(); ++i) {
                    sb.append("?");
                    list.add(userIds.get(i));
                    if (i != userIds.size() - 1) {
                        sb.append(",");
                    }
                }

                sb.append(") ");
            }
        } else if (null != userIds && userIds.size() > 0) {
            sb.append(" AND c.`user_id` in(");

            for(i = 0; i < userIds.size(); ++i) {
                sb.append("?");
                list.add(userIds.get(i));
                if (i != userIds.size() - 1) {
                    sb.append(",");
                }
            }

            sb.append(") ");
        }

        if ("1".equals(type)) {
            sb.append(")f ORDER BY f.profit_ratio ").append(orderByTotalScore).append(" ,f.id ");
        } else {
            sb.append(")f ORDER BY f.`totalScore` ").append(orderByTotalScore).append(" ,f.id ");
        }

        return DBUtil.getObjectPageBySQL("*", sb.toString(), list.toArray(), pageIndex, pageSize);
    }

    public JSONObject getMemberInfo(String circleId, String circleCode, long userId, String time) {
        String sql = "SELECT b.`headimg`,c.`user_hp`,b.`name`,b.`account`,IF(f.`totalNumber`IS NULL,0,f.`totalNumber`)AS totalNumber,IF(f.`totalScore`IS NULL,0,f.`totalScore`)AS totalScore,IF(f.`bigWinnerCount`IS NULL,0,f.`bigWinnerCount`)AS bigWinnerCount,IF(f.`bigWinnerScore`IS NULL,0,f.`bigWinnerScore`)AS bigWinnerScore FROM game_circle_member c LEFT JOIN za_users b ON c.`user_id`=b.`id`LEFT JOIN(SELECT COUNT(*)AS totalNumber,l.`user_id`,SUM(l.`score`)AS totalScore,SUM(IF(l.`is_big_winner`=0,0,1))AS bigWinnerCount,SUM(IF(l.`is_big_winner`=0,0,l.`score`))AS bigWinnerScore FROM za_user_game_statis l WHERE l.`circle_code`= ? AND l.`user_id`= ?  AND(DATEDIFF(l.`create_time`,?)=0)GROUP BY l.`user_id`)f ON c.`user_id`=f.`user_id`WHERE c.`circle_id`= ? AND c.`is_delete`= ? AND c.`user_id`= ? ";
        System.out.println(sql);
        System.out.println(circleCode+"   "+userId+"   "+time+"   "+circleId+"  "+ "N"+ "  "+ userId);
        return DBUtil.getObjectBySQL(sql, new Object[]{circleCode, userId, time, circleId, "N", userId});
    }

    public JSONArray queryMbrExamList(String circleId, String auditType, String platform) {
        String sql = "select * from game_circle_audit_record where circle_id=? and audit_type=? and platform=? and is_agree != 'Y' and is_delete='N'";
        return DBUtil.getObjectListBySQL(sql, new Object[]{circleId, auditType, platform});
    }

    public JSONObject getMyCircleStatistics(long userId, long circleId, int userRole) {
        String sql = "SELECT a.superior_user_code, b.free_count,b.inning_count,c.account,a.is_admin,c.name as user_name FROM game_circle_member a LEFT JOIN za_users c ON a.user_id=c.id LEFT JOIN game_circle_statistics b ON a.user_code=b.user_code  WHERE c.account=? AND a.circle_id=? and a.user_role=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{userId, circleId, userRole});
    }

    public JSONObject getUserByAccount(String account, long circleId, int userRole, String platform) {
        String sql = "SELECT b.id,a.superior_user_code FROM game_circle_member a LEFT JOIN za_users b ON a.user_id = b.id WHERE b.account =? AND a.circle_id=? AND a.user_role=? AND a.platform=? and a.is_delete='N'";
        return DBUtil.getObjectBySQL(sql, new Object[]{account, circleId, userRole, platform});
    }

    public JSONArray queryMsgExamList(String circleId, String userId, String platform) {
        String sql = "select a.id as msgId,b.id as recordId,b.superior_user_code,a.user_id,audit_type,DATE_FORMAT(b.gmt_create, '%Y-%m-%d %H:%i:%s') as gmt_create,msg_title,msg_content from game_circle_message a join game_circle_audit_record b on a.msg_pid=b.id join game_circle_member c on a.circle_id=c.circle_id where a.platform=? and a.is_read='N' and a.is_delete='N' and a.circle_id=? and c.user_id=? and c.is_delete='N' and (c.user_role = '1' or c.is_admin='1') and b.`audit_type` != 6 and b.`audit_type` != 8";
        return DBUtil.getObjectListBySQL(sql, new Object[]{platform, circleId, userId});
    }

    public JSONObject getLowerMemberCount(String userId, String circleId, String platform) {
        String sql = "select count(1) as total from game_circle_member where superior_user_code in(select user_code from game_circle_member where user_id=?) and circle_id=? and platform=? and is_delete='N'";
        return DBUtil.getObjectBySQL(sql, new Object[]{userId, circleId, platform});
    }

    public JSONObject getSuperUserByCode(String userCode) {
        String sql = "select * from game_circle_member where user_code=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{userCode});
    }

    public JSONArray getSysGlobalEvent(JSONObject object) {
        String sql = "select * from sys_global where platform=? and global_key in ('" + object.getString("globalKey").replaceAll(",", "','") + "')";
        return DBUtil.getObjectListBySQL(sql, new Object[]{object.getString("platform")});
    }

    public int saveOrUpdateZaDeduction(JSONObject jsonObject) {
        return DBJsonUtil.saveOrUpdate(jsonObject, "za_userdeduction");
    }

    public JSONObject getSeqMemberIdsByUserCode(String userCode) {
        String sql = "select * from game_circle_member where user_code=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{userCode});
    }

    public JSONObject getUpCircleMemberByUserId(Long adminId, Long circleId, String platform) {
        String sql = "select * from game_circle_member a left join za_users b on a.user_id = b.id where a.user_code = (SELECT superior_user_code from game_circle_member where user_id = ? and circle_id = ? and platform = ? and is_delete='N')";
        return DBUtil.getObjectBySQL(sql, new Object[]{adminId, circleId, platform});
    }

    public JSONArray getCircleMemberByCircleId(Long circleInfoId) {
        String sql = "SELECT a.`id`, a.`user_id`, IFNULL(b.lowerCount, 0) AS lowerCount    FROM game_circle_member a    JOIN (      SELECT superior_user_code AS user_code,is_delete, COUNT(*) AS lowerCount      FROM game_circle_member where is_delete = 'N'      GROUP BY superior_user_code      HAVING superior_user_code IS NOT NULL    ) b    ON a.`user_code` = b.user_code    WHERE a.`circle_id` = ?    AND b.`is_delete` = 'N'";
        return DBUtil.getObjectListBySQL(sql, new Object[]{circleInfoId});
    }

    public JSONObject getCircleMemberInfo(Object circleId, Object userId) {
        String sql = "SELECT c.`account` AS upAccount, c.`name` AS upName, a.`profit_ratio` AS profitRatio, a.`profit_balance_all` AS profitBalanceAll  FROM game_circle_member a LEFT JOIN game_circle_member b ON a.`superior_user_code` = b.`user_code` LEFT JOIN za_users c ON b.`user_id` = c.`id`  WHERE a.`circle_id` = ? AND a.`is_delete` = 'N' AND a.user_id = ? ";
        return DBUtil.getObjectBySQL(sql, new Object[]{circleId, userId});
    }

    public JSONArray getLowerMemberList(Long userMemberId, long circleId, String platform) {
        String sql = "SELECT * FROM game_circle_member WHERE seq_member_ids LIKE CONCAT('%',?,'%')  AND circle_id = ? AND platform = ? AND is_delete = 'N' ";
        return DBUtil.getObjectListBySQL(sql, new Object[]{userMemberId, circleId, platform});
    }

    public void resetPartner(long[] lowerIdList) {
        StringBuilder sb = new StringBuilder("UPDATE game_circle_member SET profit_ratio = 0 WHERE id IN (-1");
        long[] var3 = lowerIdList;
        int var4 = lowerIdList.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            long aLowerIdList = var3[var5];
            sb.append(",").append(aLowerIdList);
        }

        sb.append(")");
        DBUtil.executeUpdateBySQL(sb.toString(), new Object[0]);
    }

    public JSONArray queryMsgPartnerExamList(String circleId, String userId, String platform) {
        String sql = "select a.id as msgId,b.id as recordId,b.superior_user_code,a.user_id,audit_type,DATE_FORMAT(b.gmt_create, '%Y-%m-%d %H:%i:%s') as gmt_create,msg_title,msg_content from game_circle_message a join game_circle_audit_record b on a.msg_pid=b.id join game_circle_member c on a.circle_id=c.circle_id and a.user_id = c.user_id where a.platform=? and a.is_read='N' and a.is_delete='N' and a.circle_id=? and a.user_id=? and c.is_delete='N' and c.user_role = '2' and b.`audit_type` in (?,?)";
        return DBUtil.getObjectListBySQL(sql, new Object[]{platform, circleId, userId, "8", "6"});
    }

    public void updateMessage(long msgId) {
        String sql = "UPDATE game_circle_message SET is_read = 'Y' WHERE id = ? ";
        DBUtil.executeUpdateBySQL(sql, new Object[]{msgId});
    }

    public JSONObject queryMasterMemberInfo(long circleId) {
        String sql = "SELECT * FROM game_circle_info a LEFT JOIN game_circle_member b ON a.create_user = b.`user_id` WHERE b.`circle_id` = ?   AND b.`user_role` = '1'   AND a.`is_delete` = 'N'   AND b.`is_delete` = 'N'";
        return DBUtil.getObjectBySQL(sql, new Object[]{circleId});
    }

	@Override
	public int saveOrUpdateZaBank(JSONObject jsonObject) {
		 return DBJsonUtil.saveOrUpdate(jsonObject, "za_bank");
	}

	@Override
	public int updateBankBalance(int circleId, String userId, int balance) {
		String sql = "UPDATE za_bank set money = money+ ? WHERE circle_id = ? AND user_id = ?";
		return DBUtil.executeUpdateBySQL(sql, new Object[] {balance,circleId,userId});
	}

	@Override
	public JSONObject getLuckyDrawById(int id) {
		String sql = "SELECT * FROM  za_luckydraw where id = ?";
		return DBUtil.getObjectBySQL(sql, new Object[] {id});
	}

	@Override
	public JSONObject getBoos(int circleId) {
		String sql = "SELECT * FROM game_circle_member where circle_id = ? and user_role = 1";
		return DBUtil.getObjectBySQL(sql, new Object[] {circleId});
	}
	
	
}
