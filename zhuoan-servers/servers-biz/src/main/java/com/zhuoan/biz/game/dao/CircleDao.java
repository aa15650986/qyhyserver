//
// Source code recreated from a .class file by IntelliJ IDEA

package com.zhuoan.biz.game.dao;

import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public interface CircleDao {
    Integer getCountCircleByUser(long var1, String var3);

    Integer getCountCircleByAccount(String var1, String var2);

    JSONObject getCircleInfoByCode(String var1);

    JSONObject getCircleMemberByCode(String var1);

    long saveOrUpdateAuditRecord(JSONObject var1);

    int saveOrUpdateFundBill(JSONObject var1);

    int saveOrUpdateCircleInfo(JSONObject var1);

    long insertCircleInfoReturnId(JSONObject var1);

    long saveOrUpdateCircleMember(JSONObject var1);

    int saveOrUpdateCircleMsg(JSONObject var1);

    int saveOrUpdateStstis(JSONObject var1);

    int saveOrUpdateUserInfo(JSONObject var1);

    int saveOrUpdateUserPropBill(JSONObject var1);

    int saveOrUpdateUserSensitive(JSONObject var1);

    JSONObject getCircleInfoById(long var1);

    JSONObject getCircleMemberByUserId(Long var1, Long var2, String var3);

    JSONObject getCircleMemberByUserAccount(String var1, Long var2, String var3);

    int updateCircleMemberCreateUser(int var1, int var2, long var3, long var5, long var7, String var9);

    JSONArray queryMbrExamList(String var1, String var2, String var3);

    JSONObject getZaUserInfoById(long var1);

    JSONArray getPartner(long var1, long var3);

    JSONObject checkMemberExist(long var1, long var3);

    JSONObject selectByCircleIdAndUsercode(long var1, String var3);

    void addPartner(JSONObject var1);

    JSONObject getUserPropBill(long var1, long var3, Object[] var5, String var6, int var7, int var8);

    JSONArray getYestodayAndTodayPropBill(long var1, long var3, String var5, String var6, String var7, String var8);

    JSONObject getMemberStatistics(int var1, long var2, long var4, int var6, int var7);

    JSONObject getAllMemberStatistics(int var1, long var2, int var4, int var5);

    JSONArray getYesterdayGameLog(long var1, String var3, int var4, int var5);

    JSONObject getOperatorSystemsettingByPlatform(String var1);

    JSONObject getTodayHpChange(long var1, long var3, String var5, String var6);

    JSONObject getUserTotalPlay(long var1, long var3, long var5);

    JSONObject getYestodayHpFee(long var1, long var3, String var5);

    JSONObject getYestodayPalyNum(long var1, String var3, int var4, int var5);

    JSONObject getMyCircleStatistics(long var1, long var3, int var5);

    JSONObject getUserByAccount(String var1, long var2, int var4, String var5);

    void transferProfitBalanceToHp(JSONObject var1);

    JSONObject getMemberStatisticsPage(long var1, String var3, int var4, int var5, int var6);

    JSONObject getAllMemberStatisticsPage(long var1, int var3, int var4, int var5);

    JSONObject getOneMemberStatisticsPage(long var1, int var3, long var4, int var6, int var7);

    JSONObject getYestodayPaly(long var1, long var3, int var5, String var6);

    JSONObject getAllYestodayPaly(long var1, long var3, int var5);

    JSONObject getSuperUserDetailMessageByCode(String var1);

    JSONObject getMemberInfoPage(String var1, String var2, String var3, String var4, String var5, List var6, int[] var7, long var8, String var10, String var11, String var12, int var13, int var14);

    JSONObject getMemberInfo(String var1, String var2, long var3, String var5);

    JSONArray queryCircleList(Long var1, String var2);

    int getCountCircleMemberByCircle(long var1, String var3);

    JSONArray queryMsgExamList(String var1, String var2, String var3);

    JSONObject getLowerMemberCount(String var1, String var2, String var3);

    JSONObject getSuperUserByCode(String var1);

    JSONArray getSysGlobalEvent(JSONObject var1);

    int saveOrUpdateZaDeduction(JSONObject var1);

    JSONObject getSeqMemberIdsByUserCode(String var1);

    JSONObject getUpCircleMemberByUserId(Long var1, Long var2, String var3);

    JSONArray getCircleMemberByCircleId(Long var1);

    JSONObject getCircleMemberInfo(Object var1, Object var2);

    JSONArray getLowerMemberList(Long var1, long var2, String var4);

    void resetPartner(long[] var1);

    JSONArray queryMsgPartnerExamList(String var1, String var2, String var3);

    void updateMessage(long var1);

    JSONObject queryMasterMemberInfo(long var1);
    
    int saveOrUpdateZaBank(JSONObject var1);
    
    int updateBankBalance(int var1,String var2,int var3);
    
    JSONObject getLuckyDrawById(int id);
    
    JSONObject getBoos(int circleId);
}
