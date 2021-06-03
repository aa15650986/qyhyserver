
package com.zhuoan.biz.game.biz;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public interface PublicBiz {
    JSONArray getRoomSetting(int var1, String var2, int var3);

    JSONObject getSysBaseSet();

    JSONObject getAPPGameSetting();

    JSONArray getAppObjRec(Long var1, int var2, String var3, String var4, String var5);

    void addAppObjRec(JSONObject var1);

    JSONArray getNoticeByPlatform(String var1, int var2);

    JSONArray getGoldSetting(JSONObject var1);

    JSONObject getUserSignInfo(String var1, long var2);

    int addOrUpdateUserSign(JSONObject var1);

    JSONObject getSignRewardInfoByPlatform(String var1);

    JSONArray getArenaInfo();

    void addOrUpdateUserCoinsRec(long var1, int var3, int var4);

    JSONObject getUserGameInfo(String var1);

    void addOrUpdateUserGameInfo(JSONObject var1);

    void addUserWelfareRec(String var1, double var2, int var4, int var5);

    JSONObject getAppSettingInfo(String var1);
}
