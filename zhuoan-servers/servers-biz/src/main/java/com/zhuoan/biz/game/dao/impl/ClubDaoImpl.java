
package com.zhuoan.biz.game.dao.impl;

import com.zhuoan.biz.game.dao.ClubDao;
import com.zhuoan.dao.DBJsonUtil;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.util.Dto;
import com.zhuoan.util.TimeUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class ClubDaoImpl implements ClubDao {
    public ClubDaoImpl() {
    }

    public JSONObject getUserClubByAccount(String account) {
        String sql = "select id,account,platform,clubIds,top_club from za_users where account=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{account});
    }

    public JSONObject getClubByCode(String clubCode) {
        String sql = "select id,clubCode,platform,notice,setting,leaderId,clubName,quick_setting,balance,balance_type,payType from club where clubCode=? and isUse=1";
        return DBUtil.getObjectBySQL(sql, new Object[]{clubCode});
    }

    public JSONObject getClubById(long id) {
        String sql = "select id,clubCode,platform,notice,setting,leaderId,clubName,quick_setting,balance,balance_type,payType from club where id=? and isUse=1";
        return DBUtil.getObjectBySQL(sql, new Object[]{id});
    }

    public JSONArray getClubMember(long clubId) {
        String sql = "select id,account,platform,clubIds,top_club,name,headimg from za_users where clubIds like ?";
        return DBUtil.getObjectListBySQL(sql, new Object[]{"%$" + clubId + "$%"});
    }

    public JSONObject getUserByAccountAndUuid(String account, String uuid) {
        String sql = "select id,clubIds,roomcard,yuanbao from za_users where account=? and uuid=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{account, uuid});
    }

    public void updateClubInfo(JSONObject clubInfo) {
        DBJsonUtil.update(clubInfo, "club");
    }

    public void updateUserClubIds(long userId, String clubIds) {
        String sql = "update za_users set clubIds=? where id=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{clubIds, userId});
    }

    public void updateClubBalance(long clubId, double sum) {
        String sql = "update club set balance=balance-? where id=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{sum, clubId});
    }

    public void addClubPumpRec(long userId, long roomId, String roomNo, int gid, int type, double sum, String createTime, String platform, double pocketNew, double pocketOld) {
        String sql = "insert into za_userdeduction (userid,roomid,roomNo,gid,type,sum,doType,creataTime,memo,platform,pocketNew,pocketOld,pocketChange,operatorType) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        DBUtil.executeUpdateBySQL(sql, new Object[]{userId, roomId, roomNo, gid, type, sum, 2, createTime, "俱乐部抽水", platform, pocketNew, pocketOld, sum, 10});
    }

    public void updateUserTopClub(String account, long clubId) {
        String sql = "update za_users set top_club=? where account=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{clubId, account});
    }

    public JSONArray getClubInviteRec(int status, long clubId) {
        String sql = "SELECT a.id,a.clubId,a.userId,b.account,b.`name`,b.headimg FROM `club_invite_rec` a LEFT JOIN za_users b ON a.userId=b.id where a.status=? and a.clubId=? ORDER BY a.id DESC";
        return DBUtil.getObjectListBySQL(sql, new Object[]{status, clubId});
    }

    public void updateClubInviteRecStatus(int status, long clubInviteRecId) {
        String sql = "update club_invite_rec set status=?,modifyTime=? where id=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{status, TimeUtil.getNowDate(), clubInviteRecId});
    }

    public void updateUserClub(long userId, String clubIds) {
        String sql = "update za_users set clubIds=? where id=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{clubIds, userId});
    }

    public void addClubInviteRec(long userId, long clubId, long parId, String memo, int status) {
        String sql = "insert into club_invite_rec(clubId,userId,parId,description,status,createTime,modifyTime) values(?,?,?,?,?,?,?)";
        DBUtil.executeUpdateBySQL(sql, new Object[]{clubId, userId, parId, memo, status, TimeUtil.getNowDate(), TimeUtil.getNowDate()});
    }

    public boolean updatePropAndWriteRec(Double sum, Long playerId, int type, int doType, String platform, String des, int operatorType) {
        String updateSql = "";
        if (type == 1) {
            updateSql = "update za_users set roomcard=roomcard+? where id=?";
        } else if (type == 4) {
            updateSql = "update za_users set yuanbao=yuanbao+? where id=?";
        }

        if (!Dto.stringIsNULL(updateSql)) {
            int updateLine = DBUtil.executeUpdateBySQL(updateSql, new Object[]{sum, playerId});
            if (updateLine > 0) {
                JSONObject obj = new JSONObject();
                obj.put("userid", playerId);
                obj.put("type", type - 1);
                obj.put("sum", sum);
                obj.put("doType", doType);
                obj.put("creataTime", TimeUtil.getNowDate());
                obj.put("memo", des);
                obj.put("pocketChange", sum);
                obj.put("operatorType", operatorType);
                obj.put("platform", platform);
                int addLine = DBJsonUtil.add(obj, "za_userdeduction");
                return addLine > 0;
            }
        }

        return false;
    }

    public boolean addClub(JSONObject club) {
        int addLine = DBJsonUtil.add(club, "club");
        return addLine > 0;
    }

    public JSONObject getSysClubSetting(String platform) {
        String sql = "select has_club, club_count, club_mini_bal, club_bal_type from operator_systemsetting where platform=? ORDER BY id LIMIT 1";
        return DBUtil.getObjectBySQL(sql, new Object[]{platform});
    }

    public int countClubByLeader(String leaderId, String platform) {
        String sql = "select COUNT(id) as clubCount from club where leaderId=? and platform=? and isUse=1";
        JSONObject countInfo = DBUtil.getObjectBySQL(sql, new Object[]{leaderId, platform});
        return !Dto.isObjNull(countInfo) && countInfo.containsKey("clubCount") ? countInfo.getInt("clubCount") : 0;
    }
}
