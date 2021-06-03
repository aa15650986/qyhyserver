
package com.zhuoan.biz.game.biz;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public interface ClubBiz {
    JSONObject getUserClubByAccount(String var1);

    JSONObject getClubByCode(String var1);

    JSONObject getClubById(long var1);

    JSONArray getClubMember(long var1);

    JSONObject getUserByAccountAndUuid(String var1, String var2);

    void updateClubInfo(JSONObject var1);

    void updateUserClubIds(long var1, String var3);

    boolean clubPump(String var1, double var2, long var4, String var6, int var7);

    void updateUserTopClub(String var1, long var2);

    JSONArray getClubInviteRec(int var1, long var2);

    void updateClubInviteRecStatus(int var1, long var2);

    void updateUserClub(long var1, String var3);

    void addClubInviteRec(long var1, long var3, long var5, String var7, int var8);

    boolean updatePropAndWriteRec(Double var1, Long var2, int var3, int var4, String var5, String var6, int var7);

    boolean addClub(JSONObject var1);

    JSONObject getSysClubSetting(String var1);

    int countClubByLeader(String var1, String var2);

    void updateClubBalance(long var1, double var3);
}
