
package com.zhuoan.biz.game.biz;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public interface RoomBiz {
    long insertGameRoom(JSONObject var1);

    JSONObject getGameInfoByID(long var1);

    JSONObject getRoomInfoByRno(String var1);

    boolean updateGameRoom(JSONObject var1);

    boolean closeGameRoom(String var1);

    JSONObject getRoomInfoSeting(int var1, String var2);

    boolean stopJoin(String var1);

    JSONObject getRoomInfoByRnoNotUse(String var1);

    JSONObject getGameSetting();

    JSONArray getRobotArray(int var1, double var2, double var4);

    boolean pump(JSONArray var1, String var2, int var3, double var4, String var6);

    void settlementRoomNo(String var1);

    void updateRobotStatus(String var1, int var2);

    void increaseRoomIndexByRoomNo(Object var1);

    JSONArray getRoomListByClubCode(String var1, int var2, int var3, int var4);

    void updateRoomQuitUser(JSONObject var1);

    long reloadCreateRoom(JSONObject var1);

    void updateZaGamerooms(JSONObject var1);
}
