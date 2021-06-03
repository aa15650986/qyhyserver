
package com.zhuoan.biz.game.dao.impl;

import com.zhuoan.biz.game.dao.SysSettingDao;
import com.zhuoan.dao.DBUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class SysSettingDaoImpl implements SysSettingDao {
    public SysSettingDaoImpl() {
    }

    public JSONObject queryByKey(String platform, String key) {
        String sql = "select * from sys_global where  `key`= ? and platform = ?";
        return DBUtil.getObjectBySQL(sql, new Object[]{key, platform});
    }

    public JSONArray queryAll(String platform) {
        String sql = "select * from sys_global where platform=?";
        return DBUtil.getObjectListBySQL(sql, new Object[]{platform});
    }
}
