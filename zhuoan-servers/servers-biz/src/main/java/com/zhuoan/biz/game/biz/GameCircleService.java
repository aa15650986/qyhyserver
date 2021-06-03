
package com.zhuoan.biz.game.biz;

import java.util.List;
import net.sf.json.JSONObject;

public interface GameCircleService {
    JSONObject queryGameCircleInfoById(String var1);

    JSONObject getCircleMemberInfoById(String var1, String var2);

    boolean deductRoomCard(String var1, String var2, String var3, String var4, double var5, List<String> var7, String var8);

    void userPumping(String var1, String var2, String var3, JSONObject var4, String var5);

    void circleCentFee(String var1, String var2, String var3, JSONObject var4, String var5);

    void circleUserPumping(JSONObject var1);
}
