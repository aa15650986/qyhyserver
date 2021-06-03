
package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.RobotBiz;
import com.zhuoan.biz.game.dao.RobotDao;
import java.util.List;
import javax.annotation.Resource;
import net.sf.json.JSONArray;
import org.springframework.stereotype.Service;

@Service
public class RobotBizImpl implements RobotBiz {
    @Resource
    private RobotDao robotDao;

    public RobotBizImpl() {
    }

    public JSONArray getFreeRobotByCount(int count) {
        return this.robotDao.getFreeRobotByCount(count);
    }

    public int batchUpdateRobotStatus(List<Long> idList, int status) {
        if (idList.size() == 0) {
            return -1;
        } else {
            return status != 0 & status != 1 ? -2 : this.robotDao.batchUpdateRobotStatus(idList, status);
        }
    }
}
