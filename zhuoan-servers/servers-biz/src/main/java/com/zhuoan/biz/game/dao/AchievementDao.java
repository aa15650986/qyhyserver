

package com.zhuoan.biz.game.dao;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public interface AchievementDao {
    JSONArray getAchievementInfoByGameId(int var1, String var2);

    JSONObject getAchievementInfoById(long var1);

    JSONArray getUserAchievementByAccount(String var1);

    JSONObject getUserAchievementByAccountAndGameId(String var1, int var2);

    void addOrUpdateUserAchievement(JSONObject var1);

    JSONArray getAchievementRank(int var1, int var2);
}
