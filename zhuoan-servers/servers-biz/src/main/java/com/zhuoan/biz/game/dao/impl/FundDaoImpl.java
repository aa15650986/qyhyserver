
package com.zhuoan.biz.game.dao.impl;

import com.zhuoan.biz.game.dao.FundDao;
import com.zhuoan.dao.DBUtil;
import java.util.UUID;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class FundDaoImpl implements FundDao {
    public FundDaoImpl() {
    }

    public void updateUserScoreByOpenId(String openId, double score) {
        String sql = "update za_users set yuanbao=? where openid=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{score, openId});
    }

    public JSONObject getSysUsers() {
        String sql = "select account,openid,uuid from za_users where isown=1 and status=0 limit 0,1";
        return DBUtil.getObjectBySQL(sql, new Object[0]);
    }

    public void updateSysUserStatusByAccount(String account, int status) {
        String sql = "update za_users set status=? where account=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{status, account});
    }

    public JSONObject getVersionInfo() {
        String sql = "select platform from za_version where id=1";
        return DBUtil.getObjectBySQL(sql, new Object[0]);
    }

    public JSONObject getUserInfoByOpenId(String openId) {
        String sql = "select id from za_users where openid=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{openId});
    }

    public JSONObject getUserInfoByAccount(String account) {
        String sql = "select id from za_users account=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{account});
    }

    public void insertUserInfo(String account, JSONObject obj, String platform) {
        String sql = "insert into za_users(account,name,headimg,tel,sex,openid,uuid,platform,yuanbao,isown,status) values(?,?,?,?,?,?,?,?,?,?,?)";
        DBUtil.executeUpdateBySQL(sql, new Object[]{account, obj.getString("nick_name"), obj.getString("img_url"), obj.getString("user_tel"), "男", obj.getString("chain_add"), UUID.randomUUID().toString(), platform, 0, 1, 0});
    }
}
