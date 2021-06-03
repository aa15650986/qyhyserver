

package com.zhuoan.biz.game.dao;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public interface MatchDao {
    JSONArray getMatchSettingByType(int var1, String var2);

    JSONObject getMatchSettingById(long var1, long var3);

    void updateMatchSettingById(JSONObject var1);

    JSONObject getMatchInfoByMatchId(long var1, int var3, int var4);

    void addOrUpdateMatchInfo(JSONObject var1);

    void updateMatchInfoByMatchNum(String var1, int var2);

    JSONArray getRobotList(int var1);

    void updateUserCoinsAndScoreByAccount(String var1, int var2, int var3, int var4, double var5);

    JSONObject getUserWinningRecord(String var1, int var2);

    void addOrUpdateUserWinningRecord(JSONObject var1);

    void updateRobotStatus(String var1, int var2);

    JSONArray getUnFullMatchInfo();

    JSONArray getMatchSection(String var1);
}
