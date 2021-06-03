
package com.zhuoan.service;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public interface TeaService {
    String getTeaCode();

    void applyJoinTea(JSONObject var1);

    String getRoomGameInfo(JSONObject var1);

    void importMemberTea(JSONObject var1);

    int getTeaMemberPowern(Object var1, String var2);

    JSONObject getTeaMemberLimitPage(String var1, String var2, Integer var3, Integer var4);

    void insertTeaMemberMsg(JSONObject var1);

    JSONObject getTeaMumberRecord(long var1, String var3, int[] var4, String var5);

    JSONObject pageTeaMumberRecord(String var1, String var2, int[] var3, String var4, int var5, Integer var6, Integer var7);

    JSONObject getMemberRecord(String var1, String var2, int[] var3, String var4);

    JSONObject pageTeaMumberBigWinRecord(String var1, String var2, int[] var3, String var4, Integer var5, Integer var6);

    JSONObject getMemberBigWinRecord(String var1, String var2, int[] var3, String var4);

    void generationTeaRoom(JSONObject var1);

    JSONObject getTeaRoomcardSpend(Object var1, Object var2, Object[] var3, Object var4);

    JSONArray getTeaRoomcardAllSpend(Object var1, String var2, Object[] var3, Object var4);

    int verifyJoinRoom(int var1, int var2, String var3, Object var4, Object var5);

    int deductTeaMoney(String var1, Object var2, String var3, String var4, int var5);

    void insertTeaMemberMoneyRecord(JSONObject var1);

    JSONObject getMoneyRecordLimitPage(String var1, int var2, String var3, Integer var4, Integer var5);

    JSONObject getSysValue(String var1);

    int updateTeaRecordStatiTick(String var1, String var2, String var3);

    void updateTeaMemberScoreALl(JSONArray var1, String var2);
}
