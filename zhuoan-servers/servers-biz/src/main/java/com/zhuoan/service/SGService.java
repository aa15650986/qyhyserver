
package com.zhuoan.service;

import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public interface SGService {
    JSONObject obtainRoomData(String var1, String var2);

    JSONArray obtainAllPlayer(String var1);

    JSONObject obtainPlayerInfo(String var1, String var2);

    JSONArray getDissolveRoomInfo(String var1);

    JSONObject getGameData(String var1, String var2);

    boolean isStart(String var1, String var2);

    void exitRoomByPlayer(String var1, long var2, String var4);

    void removeRoom(String var1);

    boolean isAllReady(String var1);

    int getNowReadyCount(String var1);

    void startGame(String var1);

    boolean isAgreeClose(String var1);

    void settleAccounts(String var1);

    boolean isAllRobZhuang(String var1);

    List<Integer> getAnteList(String var1, String var2);

    void confirmZhuang(String var1);

    void gameBet(String var1);

    boolean isAllBet(String var1);

    void gameShowCard(String var1);

    boolean isAllShowCard(String var1);

    void clearRoomInfo(String var1, boolean var2);
}
