
package com.zhuoan.biz.game.biz;

import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public interface GameLogBiz {
    int addOrUpdateGameLog(JSONObject var1);

    long getGameLogId(long var1, int var3);

    void addUserGameLog(JSONArray var1, JSONObject var2);

    void updateGamelogs(String var1, int var2, JSONArray var3);

    JSONArray getUserGameLogList(String var1, int var2, int var3, String var4);

    JSONArray getUserGameLogsByUserId(long var1, int var3, int var4);

    JSONArray getUserGameRoomByRoomType(long var1, int var3, int var4);

    JSONArray getUserGameLogsByUserId(long var1, int var3, int var4, List<String> var5, String var6, String var7);

    JSONObject pageUserGameLogsByUserId(Long var1, Integer var2, String var3, Object[] var4, int[] var5, String var6, int var7, int var8);

    JSONArray getRecordInfoByUser(int[] var1, String var2, Integer var3, String var4, String var5);

    JSONArray getUserGameLogList(Object var1, Object var2);

    JSONObject getRoomcardSpend(Object var1, Object var2);

    JSONObject getRoomcardAllSpend(Object var1, Object var2, String var3, List var4);

    JSONObject getGameAllCount(Object var1, int[] var2, String var3, List var4, String var5);

    JSONObject getUserGameLogsByUserIdAndRoomNo(long var1, String var3, String var4);

    JSONObject getRecordInfo(long var1, String var3);
}
