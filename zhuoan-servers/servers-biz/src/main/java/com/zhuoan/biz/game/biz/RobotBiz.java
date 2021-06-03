
package com.zhuoan.biz.game.biz;

import java.util.List;
import net.sf.json.JSONArray;

public interface RobotBiz {
    JSONArray getFreeRobotByCount(int var1);

    int batchUpdateRobotStatus(List<Long> var1, int var2);
}
