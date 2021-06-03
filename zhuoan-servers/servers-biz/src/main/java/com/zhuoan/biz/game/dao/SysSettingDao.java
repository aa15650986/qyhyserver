
package com.zhuoan.biz.game.dao;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public interface SysSettingDao {
    JSONObject queryByKey(String var1, String var2);

    JSONArray queryAll(String var1);
}
