
package com.zhuoan.biz.game.biz;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public interface UserBiz {
    JSONObject getUserByID(long var1);

    JSONObject getUserByAccount(String var1);

    JSONObject checkUUID(String var1, String var2);

    boolean updateUserBalance(JSONArray var1, String var2);

    void insertUserdeduction(JSONObject var1);

    JSONArray refreshUserBalance();

    JSONObject getGongHui(JSONObject var1);

    JSONObject getSysUser(String var1, String var2, String var3);

    void updateUserPump(String var1, String var2, double var3);

    void addUserTicketRec(JSONObject var1);

    JSONObject getUserInfoByTel(String var1);

    int updateUserInfo(JSONObject var1);

    int deleteUserInfoById(long var1);

    JSONObject getUserProxyInfoById(long var1);
    
    int updateRoomcardByAccount(String var1,int var2);
    
    JSONObject getUserBgSetByUserId(long var);
    
    int addUserGameSet(JSONObject var);
    
    int updateGameSetByGameId(int var1,int var2,int var3);
    
    int updateUserNameByUserId(long var1,String var2);
    
    int updateUserHeadimgByUserId(long var1,String var2);
    
    JSONObject findHeadimgById(long var);
}
