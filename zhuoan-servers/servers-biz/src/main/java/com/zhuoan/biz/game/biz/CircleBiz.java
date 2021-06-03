
package com.zhuoan.biz.game.biz;

import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public interface CircleBiz {
    JSONObject addGameCircleInfo(JSONObject var1);

    JSONObject updateGameCircleInfo(JSONObject var1);

    String getCircleCode(int var1, int var2);

    int userJoinCircle(JSONObject var1);

    int userExitCircle(JSONObject var1);

    long exitCircle(String var1, String var2, String var3);

    long userJoinExamCircle(JSONObject var1);

    long userExitExamCircle(JSONObject var1);

    int userBlackCircle(JSONObject var1);

    JSONObject queryFundBillDetail(String var1, String var2, int var3, int var4, int var5);

    JSONArray queryMessageList(JSONObject var1);

    int userFundSavePay(JSONObject var1);

    JSONObject dismissCircle(JSONObject var1);

    JSONObject transferCircle(JSONObject var1);

    JSONObject mgrSettingCircle(JSONObject var1);

    JSONArray queryMbrExamList(String var1, String var2, String var3);

    int saveOrUpdateUserPropBill(JSONObject var1);

    void savePropBill(Object var1, long var2, double var4, double var6, String var8, String var9, Object var10, String var11, Object var12);

    JSONObject getZaUserInfoById(long var1);

    JSONObject getCircleInfoById(long var1);

    JSONArray getPartner(long var1, long var3, String var5);

    void addPartner(JSONObject var1);

    JSONObject checkMemberExist(long var1, long var3);

    JSONObject selectByCircleIdAndUsercode(long var1, String var3);

    JSONObject getUserPropBill(long var1, long var3, Object[] var5, String var6, int var7, int var8);

    JSONArray getYestodayAndTodayPropBill(long var1, long var3, String var5, String var6, String var7, String var8);

    JSONObject getMemberStatistics(boolean var1, int var2, long var3, long var5, int var7, int var8);

    JSONArray getYesterdayGameLog(long var1, String var3, int var4, int var5);

    JSONObject getOperatorSystemsettingByPlatform(String var1);

    JSONObject getTodayHpChange(long var1, long var3, String var5, String var6);

    JSONObject getUserTotalPlay(long var1, long var3, long var5);

    JSONObject getYestodayHpFee(long var1, long var3, String var5);

    JSONObject getYestodayPalyNum(long var1, String var3, int var4, int var5);

    JSONObject getMyCircleStatistics(long var1, long var3, int var5);

    JSONObject getUserByAccount(String var1, long var2, int var4, String var5);

    void transferProfitBalanceToHp(JSONObject var1);

    JSONObject getMemberStatisticsPage(boolean var1, long var2, String var4, int var5, long var6, int var8, int var9);

    JSONObject getYestodayPaly(boolean var1, long var2, int var4, String var5);

    JSONObject getSuperUserDetailMessageByCode(String var1);

    JSONObject getMemberInfoPage(String var1, String var2, String var3, String var4, String var5, List var6, int[] var7, long var8, String var10, String var11, String var12, int var13, int var14);

    JSONObject getMemberInfo(String var1, String var2, long var3, String var5);

    JSONArray queryCircleList(Long var1, String var2);

    JSONObject updateProfitRatio(JSONObject var1);

    JSONArray queryMsgExamList(String var1, String var2, String var3);

    long addCircleMember(JSONObject var1);

    JSONObject getCircleMemberByUserId(JSONObject var1);

    JSONObject getCircleMemberByUserId(long var1, long var3, String var5);

    JSONObject getLowerMemberCount(JSONObject var1);

    long circleDelMember(JSONObject var1);

    JSONObject getSuperUserByCode(String var1);

    String getSysValue(String var1, String var2);

    JSONArray getSysGlobalEvent(JSONObject var1);

    int getHpRole(long var1);

    JSONArray getMemberArray(JSONArray var1, JSONObject var2, long var3);

    void insertZaUserGameStatis(JSONObject var1);

    JSONArray getCircleMemberLowerCount(String var1);

    JSONObject getCircleMemberSuper(Object var1, Object var2);

    boolean isSuperiorRelation(JSONObject var1, JSONObject var2);

    void resetPartner(Long var1, long var2, long var4, long var6, String var8);

    void updateMessage(long var1);

    JSONObject getMasterMemberInfo(long var1);

    JSONObject getMemberPage(int var1, Object var2, Object var3, Object var4, String var5, String var6, String var7, String var8, String var9, int[] var10, int var11, int var12);

    JSONObject getMemberPropBillPage(Object var1, Object var2, Object[] var3, String var4, int var5, int var6);

    JSONArray getMemberPropBillAll(Object var1, Object var2, Object[] var3);

    int addBalanceReviewMessage(long var1, long var3, String var5, double var6);

    long userExtractBalance(JSONObject var1);

    int extractBalanceMessageHandle(long var1, String var3);
    
    JSONObject getBankMsgByCircle(String var1,int var2,String var3);
    
    void insertZaBank(JSONObject var1);
    
    int operationMoney(int var1,String var2,int var3);
    
    JSONObject getCircleMemberByUserId(String var1,int var2);
    
    JSONObject getLuckyDrawById(int id);
    
    JSONObject getBoos(int circleId);
}
