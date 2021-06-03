

package com.zhuoan.biz.game.dao.impl;

import com.zhuoan.biz.game.dao.RobotDao;
import com.zhuoan.dao.DBUtil;
import java.util.List;
import net.sf.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RobotDaoImpl implements RobotDao {
    private static final Logger log = LoggerFactory.getLogger(RobotDaoImpl.class);

    public RobotDaoImpl() {
    }

    public JSONArray getFreeRobotByCount(int count) {
        try {
            String sql = "SELECT id,`name`,head_img,score_hp,`status` FROM `robot_info` WHERE `status`=0 LIMIT ?";
            return DBUtil.getObjectListBySQL(sql, new Object[]{count});
        } catch (Exception var3) {
            log.info("添加虚拟房间缺少robot_info{}", var3);
            return new JSONArray();
        }
    }

    public int batchUpdateRobotStatus(List<Long> idList, int status) {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE robot_info SET `status`=? WHERE id IN(");

        for(int i = 0; i < idList.size(); ++i) {
            sb.append(idList.get(i));
            if (i < idList.size() - 1) {
                sb.append(",");
            }
        }

        sb.append(")");
        return DBUtil.executeUpdateBySQL(String.valueOf(sb), new Object[]{status});
    }
}
