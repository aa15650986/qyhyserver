
package com.zhuoan.biz.game.dao.impl;

import com.zhuoan.biz.game.dao.PropsDao;
import com.zhuoan.dao.DBJsonUtil;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.util.Dto;
import com.zhuoan.util.TimeUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class PropsDaoImpl implements PropsDao {
    public PropsDaoImpl() {
    }

    public JSONArray getPropsInfoByPlatform(String platform) {
        String sql = "select id,game_id,props_type,props_name,props_price,cost_type,duration,status,platform,description,img_url,type_name from za_props_info where platform=? and status=1";
        return DBUtil.getObjectListBySQL(sql, new Object[]{platform});
    }

    public JSONObject getPropsInfoById(long propsId) {
        String sql = "select id,game_id,props_type,props_name,props_price,cost_type,duration,status,platform,description,img_url,type_name from za_props_info where id=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{propsId});
    }

    public JSONObject getUserPropsByType(String account, int propsType) {
        String sql = "select id,user_account,game_id,props_type,props_name,end_time,status,props_count from za_user_props where user_account=? and props_type=?";
        JSONObject userProps = DBUtil.getObjectBySQL(sql, new Object[]{account, propsType});
        return !Dto.isObjNull(userProps) ? TimeUtil.transTimeStamp(userProps, "yyyy-MM-dd HH:mm:ss", "end_time") : userProps;
    }

    public void addOrUpdateUserProps(JSONObject userProps) {
        DBJsonUtil.saveOrUpdate(userProps, "za_user_props");
    }

    public void updateUserPropsCount(String account, int propsType, int sum) {
        String sql = "update za_user_props set props_count=props_count-? where user_account=? and props_type=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{sum, account, propsType});
    }

    public JSONArray getUserPropsByAccount(String account) {
        String sql = "select id,user_account,game_id,props_type,props_name,end_time,status,props_count from za_user_props where user_account=?";
        JSONArray userProps = DBUtil.getObjectListBySQL(sql, new Object[]{account});
        return !Dto.isNull(userProps) ? TimeUtil.transTimestamp(userProps, "end_time", "yyyy-MM-dd HH:mm:ss") : userProps;
    }
}
