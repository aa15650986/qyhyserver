
package com.zhuoan.biz.game.biz;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public interface AchievementBiz {
    JSONArray getAchievementInfoByGameId(int var1, String var2);

    JSONObject getAchievementInfoById(long var1);

    JSONArray getUserAchievementByAccount(String var1);

    JSONObject getUserAchievementByAccountAndGameId(String var1, int var2);

    JSONObject addOrUpdateUserAchievement(String var1, int var2, int var3);

    JSONArray getAchievementRank(int var1, int var2);

    void updateUserAchievement(JSONObject var1);
}
