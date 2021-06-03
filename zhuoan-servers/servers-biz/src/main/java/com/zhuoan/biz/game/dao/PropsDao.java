
package com.zhuoan.biz.game.dao;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public interface PropsDao {
    JSONArray getPropsInfoByPlatform(String var1);

    JSONObject getPropsInfoById(long var1);

    JSONObject getUserPropsByType(String var1, int var2);

    void addOrUpdateUserProps(JSONObject var1);

    void updateUserPropsCount(String var1, int var2, int var3);

    JSONArray getUserPropsByAccount(String var1);
}
