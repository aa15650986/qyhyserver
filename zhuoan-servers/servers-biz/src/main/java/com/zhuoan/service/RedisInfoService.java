

package com.zhuoan.service;

import com.corundumstudio.socketio.SocketIOClient;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public interface RedisInfoService {
    JSONArray getGameSetting(int var1, String var2, int var3);

    void delGameSettingAll();

    SocketIOClient getSocketIOClientByUid(String var1);

    boolean isOnline(String var1);

    JSONObject getSysGlobalByKey(String var1, String var2);

    JSONObject getGlobalByKey(String var1);

    void delSysGlobalByKey(String var1, String var2);

    void delSysGlobal();

    JSONObject getGameCircleInfoByid(String var1);

    void delGameCircleInfoByid(String var1);

    JSONObject getGameInfoById(int var1);

    void delGameInfoById(int var1);

    void delGameInfoAll();

    long summaryTimes(String var1, String var2);

    void insertSummary(String var1, String var2);

    void delSummary(String var1, String var2);

    void delSummaryAll();

    int getAppTimeSetting(String var1, int var2, String var3);

    void delAppTimeSettingAll();

    List<String> getTeaInfoAllCode();

    void delTeaInfoAllCode();

    boolean verifyTeaCode(String var1);

    boolean teaRoomOpen(String var1);

    void delTeaRoomOpen(String var1);

    void subExistCreateRoom(String var1);

    void addExistCreateRoom(String var1, int var2);

    boolean isExistCreateRoom(String var1, boolean var2);

    void delExistCreateRoom(String var1);

    JSONObject getSysInfo(String var1);

    boolean idempotent(String var1);

    void delTeaSys();

    boolean getStartStatus(String var1, String var2);

    JSONArray getGameSssSpecialSetting();

    void delGameSssSpecialSetting();

    boolean getHasControl(String var1);

    void delHasControl();

    long createCircle(String var1);

    String getCreateCircle(String var1);

    void delCreateCircle(String var1);

    void delAllCreateCircle();

    Map<String, String> getSokcetInfo();

    void delSokcetInfo();

    JSONObject getAttachGameLog(Object var1);

    long readMessageRepeatCheck(long var1);

    void delReadMessageRepeatCheck(long var1);

    void delAllReadMessageRepeatCheck();
}
