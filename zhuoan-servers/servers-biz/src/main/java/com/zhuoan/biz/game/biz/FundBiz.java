
package com.zhuoan.biz.game.biz;

import net.sf.json.JSONObject;

public interface FundBiz {
    void updateUserScoreByOpenId(String var1, double var2);

    JSONObject getSysUsers();

    void updateSysUserStatusByAccount(String var1, int var2);

    JSONObject getVersionInfo();

    JSONObject getUserInfoByOpenId(String var1);

    JSONObject getUserInfoByAccount(String var1);

    void insertUserInfo(String var1, JSONObject var2, String var3);
}
