
package com.zhuoan.service;

import com.zhuoan.biz.model.gdy.GDYPacker;
import java.util.List;
import java.util.UUID;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public interface GDYService {
    String nextPlayerAccount(String var1, String var2);

    List<GDYPacker> getSortCardList(List<String> var1, boolean var2);

    boolean checkPlayCard(String var1, String var2, int var3, List<GDYPacker> var4, List<GDYPacker> var5);

    void shuffleDeing(String var1, boolean var2);

    void sendReadyTimerToAll(List<UUID> var1, int var2, int var3);

    int getStartIndex(String var1);

    void exitRoomByPlayer(String var1, long var2, String var4);

    void clearRoomInfo(String var1);

    void dissolveRoomInform(String var1, List<UUID> var2);

    void gameSetZhuang(String var1);

    void startGame(String var1);

    JSONArray obtainAllPlayer(String var1);

    List<String> obtainAllPlayerAccount(String var1, boolean var2);

    JSONObject obtainPlayerInfo(String var1, String var2);

    JSONObject playRoomData(String var1, String var2);

    JSONObject obtainRoomData(String var1, String var2);

    void setSummaryData(String var1);

    void setFinalSummaryData(String var1);

    JSONObject getGameData(String var1, String var2);

    List<List<String>> obtainAllCard(List<GDYPacker> var1, List<GDYPacker> var2);

    boolean checkIsPlay(String var1);

    void settleAccounts(String var1);

    void updateRoomCard(String var1);

    void removeRoom(String var1);

    JSONArray getDissolveRoomInfo(String var1);

    boolean isAllReady(String var1);
}
