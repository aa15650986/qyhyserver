//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.zhuoan.biz.game.dao;

import java.util.List;
import net.sf.json.JSONArray;

public interface RobotDao {
    JSONArray getFreeRobotByCount(int var1);

    int batchUpdateRobotStatus(List<Long> var1, int var2);
}
