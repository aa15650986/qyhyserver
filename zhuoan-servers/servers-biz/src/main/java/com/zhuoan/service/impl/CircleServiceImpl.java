
package com.zhuoan.service.impl;

import com.zhuoan.dao.DBUtil;
import com.zhuoan.service.CircleService;
import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CircleServiceImpl implements CircleService {
    private final Logger log = LoggerFactory.getLogger(CircleServiceImpl.class);

    public CircleServiceImpl() {
    }

    public Map<String, String> verifyJoinRoom(Object userId, Object circleId) {
        Map map = new HashMap();
        if (null != userId && null != circleId) {
            JSONObject member = DBUtil.getObjectBySQL("SELECT g.`is_use`,g.`user_hp`,z.`account`,z.`roomcard`,z.`account` FROM game_circle_member g LEFT JOIN za_users z ON g.`user_id`=z.`id`WHERE g.`circle_id`=?AND g.`user_id`=?AND g.`is_delete`=?", new Object[]{circleId, userId, "N"});
            if (null != member && member.containsKey("user_hp") && member.containsKey("is_use") && member.containsKey("account") && member.containsKey("roomcard")) {
                if ("N".equals(member.getString("is_use"))) {
                    map.put("type", "2");
                    return map;
                } else {
                    map.put("type", "0");
                    map.put("userHp", String.valueOf(member.getDouble("user_hp")));
                    map.put("roomcard", String.valueOf(member.getInt("roomcard")));
                    map.put("account", member.getString("account"));
                    return map;
                }
            } else {
                map.put("type", "1");
                return map;
            }
        } else {
            map.put("type", "1");
            return map;
        }
    }
}
