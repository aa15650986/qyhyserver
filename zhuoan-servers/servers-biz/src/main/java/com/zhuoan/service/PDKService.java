
package com.zhuoan.service;

import com.zhuoan.biz.model.pdk.PDKPacker;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public interface PDKService {
    JSONObject obtainRoomData(String var1, String var2);

    JSONArray obtainAllPlayer(String var1);

    List<String> obtainAllPlayerAccount(String var1, boolean var2);

    JSONObject obtainPlayerInfo(String var1, String var2);

    List<JSONObject> getGameData(String var1, String var2);

    JSONArray getDissolveRoomInfo(String var1);

    void exitRoomByPlayer(String var1, long var2, String var4);

    void settleAccounts(String var1);

    void dissolveRoomInform(String var1, List<UUID> var2);

    void removeRoom(String var1);

    void clearRoomInfo(String var1);

    void updateRoomCard(String var1);

    boolean isAllReady(String var1);

    void startGame(String var1);

    JSONObject playRoomData(String var1, String var2);

    List<PDKPacker> getSortCardList(List<String> var1, int var2);

    boolean checkPlayCard(String var1, String var2, int var3, List<PDKPacker> var4);

    boolean verifyCardType(List<PDKPacker> var1, int var2, int var3, int var4, boolean var5);

    String nextPlayerAccount(String var1, String var2);

    boolean isLastCard(int var1, int var2, int var3, int var4, int var5);

    List<Map<Integer, List<PDKPacker>>> getCradGroupt(String var1);

    boolean verifyHeiTao3(int var1, int var2, int var3);

    boolean isAgreeClose(String var1);

    Map<Integer, List<PDKPacker>> getSameValList(List<PDKPacker> var1, int var2);

    List<List<PDKPacker>> getStraightList(List<PDKPacker> var1, int var2);

    List<PDKPacker> getSingleList(List<PDKPacker> var1);

    int getCardType(List<PDKPacker> var1);

    List<PDKPacker> getCardSortNumList(List<PDKPacker> var1, int var2);

    List<String> getCardList(List<PDKPacker> var1);

    List<PDKPacker> shuffle(int var1);

    void shuffleDeing(String var1, boolean var2);
}
