
package com.zhuoan.biz.game.dao;

import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public interface GameDao {
    long insertGameRoom(JSONObject var1);

    JSONObject getUserByID(long var1);

    JSONObject getUserByAccount(String var1);

    JSONObject getUserInfoByTel(String var1);

    int updateUserInfo(JSONObject var1);

    int deleteUserInfoById(long var1);

    boolean updateUserBalance(JSONArray var1, String var2);

    void insertUserdeduction(JSONObject var1);

    JSONArray getYbUpdateLog();

    void delYbUpdateLog(long var1);

    JSONObject getGongHui(JSONObject var1);

    JSONObject getGameInfoByID(long var1);

    JSONObject getRoomInfoByRno(Object var1);

    int updateRoomInfoByRid(JSONObject var1);

    JSONObject getRoomInfoSeting(int var1, String var2);

    boolean updateGameRoomUserId(String var1);

    JSONObject getRoomInfoByRnoNotUse(String var1);

    JSONObject getGameSetting();

    JSONArray getRobotArray(int var1, double var2, double var4);

    boolean pump(JSONArray var1, String var2, int var3, double var4, String var6);

    void updateUserRoomCard(int var1, long var2);

    void deductionRoomCardLog(Object[] var1);

    int addOrUpdateGameLog(JSONObject var1);

    long getGameLogId(long var1, int var3);

    int addUserGameLog(JSONObject var1);

    void updateGamelogs(String var1, int var2, JSONArray var3);

    JSONArray getUserGameLogList(String var1, int var2, int var3, String var4);

    boolean reDaikaiGameRoom(String var1);

    JSONArray getRoomSetting(int var1, String var2, int var3);

    JSONObject getSysUser(String var1, String var2, String var3);

    JSONArray getUserGameLogsByUserId(long var1, int var3, int var4);

    JSONObject pageUserGameLogsByUserId(Long var1, Integer var2, String var3, Object[] var4, int[] var5, String var6, int var7, int var8);

    JSONObject getSysBaseSet();

    JSONObject getAPPGameSetting();

    JSONArray getAppObjRec(Long var1, int var2, String var3, String var4, String var5);

    void addAppObjRec(JSONObject var1);

    void updateUserPump(String var1, String var2, double var3);

    JSONArray getNoticeByPlatform(String var1, int var2);

    JSONArray getGoldSetting(JSONObject var1);

    JSONObject getUserSignInfo(String var1, long var2);

    int addOrUpdateUserSign(JSONObject var1);

    JSONObject getSignRewardInfoByPlatform(String var1);

    void updateRobotStatus(String var1, int var2);

    JSONArray getArenaInfo();

    JSONObject getUserCoinsRecById(long var1, int var3, String var4, String var5);

    int addOrUpdateUserCoinsRec(JSONObject var1);

    JSONObject getUserGameInfo(String var1);

    void addOrUpdateUserGameInfo(JSONObject var1);

    void addUserTicketRec(JSONObject var1);

    void addUserWelfareRec(JSONObject var1);

    JSONArray getUserGameRoomByRoomType(long var1, int var3, int var4);

    JSONArray getUserGameLogsByUserId(long var1, int var3, int var4, List<String> var5, String var6, String var7);

    void increaseRoomIndexByRoomNo(Object var1);

    JSONObject getAppSettingInfo(String var1);

    JSONObject getUserProxyInfoById(long var1);

    JSONArray getRoomListByClubCode(String var1, int var2, int var3, int var4);

    JSONArray getRecordInfoByUser(int[] var1, String var2, Integer var3, String var4, String var5);

    JSONArray getUserGameLogList(Object var1, Object var2);

    JSONObject getUserGameCardLog(long var1, String var3, int var4, String var5);

    JSONObject getRoomcardSpend(Object var1, Object var2);

    JSONObject getRoomcardAllSpend(Object var1, Object var2, String var3, List var4);

    JSONObject getGameAllCount(Object var1, int[] var2, String var3, List var4, String var5);

    JSONObject getRecordInfo(long var1, String var3);
    
    int updateRoomcardByAccount(String var1,int var2);
    
    JSONObject getGameBgSetByUserId(long var);
    
    int insertUserGameSet(JSONObject var);
    
    int updateUserGameSetByGameid(int var1,int var2,int var3);
    
    int updateUserNameByUserId(long var1,String var2);

    int updateUserHeadimgByUserId(long var1,String var2);
    
    JSONObject getHeadimgByHeadId(long var);
}
