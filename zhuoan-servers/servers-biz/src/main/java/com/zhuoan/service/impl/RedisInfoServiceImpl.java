

package com.zhuoan.service.impl;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.game.dao.util.BaseSqlUtil;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.service.socketio.impl.GameMain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Resource;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RedisInfoServiceImpl implements RedisInfoService {
    private Logger log = LoggerFactory.getLogger(RedisInfoServiceImpl.class);
    @Resource
    private RedisService redisService;

    public RedisInfoServiceImpl() {
    }

    public JSONArray getGameSetting(int gid, String platform, int flag) {
        if (gid != 0 && platform != null && !"".equals(platform) && flag != 0) {
            String key = "za_gamesetting" + ":" + platform + ":" + gid + ":" + flag;
            boolean b = true;

            try {
                Object o = this.redisService.queryValueByKey(key);
                if (o != null) {
                    return JSONArray.fromObject(o);
                }
            } catch (Exception var8) {
                b = false;
                this.log.error("请启动REmote DIctionary Server");
            }

            String sql = "SELECT * FROM(SELECT z.`game_id`,z.`opt_key`,IF(f.`opt_name`IS NULL,z.`opt_name`,f.`opt_name`) opt_name,IF(f.`opt_val`IS NULL,z.`opt_val`,f.`opt_val`) opt_val,IF(f.`is_mul`IS NULL,z.`is_mul`,f.`is_mul`) is_mul,IF(f.`is_use`IS NULL,z.`is_use`,f.`is_use`) is_use,f.`memo`,IF(f.`sort`IS NULL,z.`sort`,f.`sort`) sort,IF(f.`is_open`IS NULL,z.`is_open`,f.`is_open`) is_open,IF(f.`id_hidden`IS NULL,z.`id_hidden`,f.`id_hidden`) id_hidden FROM za_gamesetting_all z LEFT JOIN(SELECT * FROM za_gamesetting WHERE `memo`= ? )f ON z.`game_id`=f.`game_id`AND z.`is_use`=f.`is_use`AND z.`opt_key`=f.`opt_key`WHERE z.`game_id`=? AND z.`is_use`=? )f WHERE f.`is_open` = 0 ORDER BY f.`sort`";
            JSONArray gameSetting = DBUtil.getObjectListBySQL(sql, new Object[]{platform, gid, flag});
            if (gameSetting != null && gameSetting.size() > 0) {
                if (b) {
                    this.redisService.insertKey(key, String.valueOf(gameSetting), 1800L);
                }

                return gameSetting;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public void delGameSettingAll() {
        String key = "za_gamesetting" + ":*";
        this.redisService.deleteByLikeKey(key);
    }

    public SocketIOClient getSocketIOClientByUid(String userId) {
        String sessionId = (String)RoomManage.channelMap.get(userId);
        if (null != sessionId && !"".equals(sessionId)) {
            UUID uuid = UUID.fromString(sessionId);
            if (uuid == null) {
                return null;
            } else {
                SocketIOClient client = GameMain.server.getClient(uuid);
                return client != null ? client : null;
            }
        } else {
            return null;
        }
    }

    public boolean isOnline(String userId) {
        String sessionId = (String)RoomManage.channelMap.get(userId);
        return null != sessionId && !"".equals(sessionId);
    }

    public JSONObject getSysGlobalByKey(String globalKey, String platform) {
        if (platform != null && !"".equals(platform) && platform != globalKey && !"".equals(globalKey)) {
            String key = "sys_global" + ":" + platform + ":" + globalKey;
            boolean b = true;

            try {
                Object o = this.redisService.queryValueByKey(key);
                if (o != null) {
                    return JSONObject.fromObject(o);
                }
            } catch (Exception var6) {
                b = false;
                this.log.error("请启动REmote DIctionary Server");
            }

            JSONObject object = BaseSqlUtil.getObjectByConditions("global_key,global_value,value_memo,memo", "sys_global", new String[]{"global_key", "platform", "is_delete"}, new Object[]{globalKey, platform, false});
            if (object != null && object.get("global_value") != null) {
                if (b) {
                    this.redisService.insertKey(key, String.valueOf(object), 1800L);
                }

                return object;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public JSONObject getGlobalByKey(String globalKey) {
        String key = "sys_global" + ":" + globalKey;
        boolean b = true;

        try {
            Object o = this.redisService.queryValueByKey(key);
            if (o != null) {
                return JSONObject.fromObject(o);
            }
        } catch (Exception var5) {
            b = false;
            this.log.error("请启动REmote DIctionary Server");
        }

        JSONObject object = BaseSqlUtil.getObjectByConditions("global_key,global_value,value_memo,memo", "sys_global", new String[]{"global_key", "is_delete"}, new Object[]{globalKey, false});
        if (object != null && object.get("global_value") != null) {
            if (b) {
                this.redisService.insertKey(key, String.valueOf(object), 1800L);
            }

            return object;
        } else {
            return null;
        }
    }

    public void delSysGlobalByKey(String globalKey, String platform) {
        String key = "sys_global" + ":" + platform + ":" + globalKey;
        this.redisService.deleteByKey(key);
    }

    public void delSysGlobal() {
        this.redisService.deleteByLikeKey("sys_global");
    }

    public JSONObject getGameCircleInfoByid(String circleId) {
        if (circleId != null && !"".equals(circleId)) {
            String key = "game_circle_info" + ":" + circleId;
            boolean b = true;

            try {
                Object o = this.redisService.queryValueByKey(key);
                if (o != null) {
                    return JSONObject.fromObject(o);
                }
            } catch (Exception var5) {
                b = false;
                this.log.error("请启动REmote DIctionary Server");
            }

            JSONObject object = BaseSqlUtil.getObjectByOneConditions("*", "game_circle_info", "id", circleId);
            if (object == null) {
                return null;
            } else {
                if (b) {
                    this.redisService.insertKey(key, String.valueOf(object), 600L);
                }

                return object;
            }
        } else {
            return null;
        }
    }

    public void delGameCircleInfoByid(String circleId) {
        if (circleId != null && !"".equals(circleId)) {
            String key = "game_circle_info" + ":" + circleId;
            this.redisService.deleteByKey(key);
        }
    }

    public JSONObject getGameInfoById(int gameId) {
        String key = "game_info_by_id" + ":" + gameId;
        boolean b = true;

        try {
            Object o = this.redisService.queryValueByKey(key);
            if (o != null) {
                return JSONObject.fromObject(o);
            }
        } catch (Exception var5) {
            b = false;
            this.log.error("请启动REmote DIctionary Server");
        }

        JSONObject object = DBUtil.getObjectBySQL("select id,name,logo,type,gameType,status,setting,isUse,clearTime from za_games where id=?", new Object[]{gameId});
        if (object == null) {
            return null;
        } else {
            if (b) {
                this.redisService.insertKey(key, String.valueOf(object), 1800L);
            }

            return object;
        }
    }

    public void delGameInfoById(int gameId) {
        String key = "game_info_by_id" + ":" + gameId;
        this.redisService.deleteByKey(key);
    }

    public void delGameInfoAll() {
        String key = "game_info_by_id" + ":*";
        this.redisService.deleteByLikeKey(key);
    }

    public long summaryTimes(String roomNo, String name) {
        String key = "settle_accounts" + name + ":" + roomNo;
        return this.redisService.incr(key, 1L);
    }

    public void insertSummary(String roomNo, String name) {
        String key = "settle_accounts" + name + ":" + roomNo;
        this.redisService.insertKey(key, "0", 60L);
    }

    public void delSummary(String roomNo, String name) {
        String key = "settle_accounts" + name + ":" + roomNo;
        this.redisService.deleteByKey(key);
    }

    public void delSummaryAll() {
        String key = "settle_accounts" + "_*";
        this.redisService.deleteByLikeKey(key);
    }

    public int getAppTimeSetting(String platform, int gid, String k) {
        int time = -99;
        String key = "app_time_setting" + ":" + platform + ":" + gid + ":" + k;
        boolean b = true;
        try {
            Object o = this.redisService.queryValueByKey(key);
            if (o != null) {
                time = Integer.valueOf(o.toString());
                return time;
            }
        } catch (Exception var8) {
            b = false;
            this.log.error("请启动REmote DIctionary Server");
        }

        JSONObject object = DBUtil.getObjectBySQL("select `v` from `app_time_setting` where `k` = ? and `game_id` = ? and `platform` = ? and `id_deleted` = 0", new Object[]{k, gid, platform});
        if (object != null) {
            time = object.getInt("v");
        }

        if (b) {
            this.redisService.insertKey(key, String.valueOf(time), 1800L);
        }

        return time;
    }

    public void delAppTimeSettingAll() {
        String key = "app_time_setting" + "*";
        this.redisService.deleteByLikeKey(key);
    }

    public List<String> getTeaInfoAllCode() {
        String key = "tea_info_code";
        boolean b = true;

        try {
            Object o = this.redisService.queryValueByKey(key);
            if (o != null) {
                return com.alibaba.fastjson.JSONArray.parseArray(o.toString(), String.class);
            }
        } catch (Exception var5) {
            b = false;
            this.log.error("请启动REmote DIctionary Server");
        }

        JSONObject object = DBUtil.getObjectBySQL("SELECT GROUP_CONCAT(tea_code) as teaCode FROM tea_info", (Object[])null);
        if (object != null && object.containsKey("teaCode")) {
            List<String> list = Arrays.asList(object.getString("teaCode").split(","));
            if (b) {
                this.redisService.insertKey(key, String.valueOf(list), 60L);
            }

            return list;
        } else {
            return new ArrayList();
        }
    }

    public void delTeaInfoAllCode() {
        this.redisService.deleteByLikeKey("tea_info_code");
    }

    public boolean verifyTeaCode(String code) {
        String key = "tea_info_code_set" + ":" + code;
        if (null != this.redisService.queryValueByKey(key)) {
            return true;
        } else {
            this.redisService.insertKey(key, code, 60L);
            return false;
        }
    }

    public boolean teaRoomOpen(String teaInfoCode) {
        if (null != teaInfoCode && !"".equals(teaInfoCode)) {
            String key = "tea_room_open" + ":" + teaInfoCode;
            boolean b = true;

            try {
                Object o = this.redisService.queryValueByKey(key);
                if (o != null) {
                    return "0".equals(o.toString());
                }
            } catch (Exception var6) {
                b = false;
                this.log.error("请启动REmote DIctionary Server");
            }

            JSONObject object = DBUtil.getObjectBySQL("SELECT t.`type`  FROM tea_info t  WHERE t.`audit`= ? AND t.`is_deleted`= ? AND t.`tea_code`= ?", new Object[]{1, 0, teaInfoCode});
            if (object != null && object.containsKey("type")) {
                String type = "1";
                if (0 == object.getInt("type")) {
                    type = "0";
                }

                if (b) {
                    this.redisService.insertKey(key, type, 600L);
                }

                return "0".equals(type);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void delTeaRoomOpen(String teaInfoCode) {
        if (null != teaInfoCode || "".equals(teaInfoCode)) {
            teaInfoCode = "*";
        }

        String key = "tea_room_open" + ":" + teaInfoCode;
        this.redisService.deleteByLikeKey(key);
    }

    public void subExistCreateRoom(String teaInfoCode) {
        String key = "tea_exist_create_room" + ":" + teaInfoCode;
        Object o = this.redisService.queryValueByKey(key);
        if (null != o) {
            int i = Integer.valueOf(o.toString());
            --i;
            if (i < 0) {
                i = 0;
            }

            this.redisService.insertKey(key, String.valueOf(i), 60L);
        }
    }

    public void addExistCreateRoom(String teaInfoCode, int count) {
        if (count > 1) {
            String key = "tea_exist_create_room" + ":" + teaInfoCode;
            this.redisService.insertKey(key, String.valueOf(count), 60L);
        }
    }

    public boolean isExistCreateRoom(String teaInfoCode, boolean isInsert) {
        String key = "tea_exist_create_room" + ":" + teaInfoCode;
        Object o = this.redisService.queryValueByKey(key);
        if (null != o && !"0".equals(o.toString())) {
            return true;
        } else {
            if (isInsert) {
                this.redisService.insertKey(key, "1", 60L);
            }

            return false;
        }
    }

    public void delExistCreateRoom(String teaInfoCode) {
        StringBuilder key = (new StringBuilder()).append("tea_exist_create_room").append(":");
        if (null == teaInfoCode) {
            this.redisService.deleteByKey(key.append("*").toString());
        } else {
            this.redisService.deleteByKey(key.append(teaInfoCode).toString());
        }

    }

    public void delTeaSys() {
        this.redisService.deleteByKey("system_info");
    }

    public JSONObject getSysInfo(String platform) {
        String key = "system_info";
        boolean b = true;

        try {
            Object o = this.redisService.queryValueByKey(key);
            if (o != null) {
                return JSONObject.fromObject(o);
            }
        } catch (Exception var5) {
            b = false;
            this.log.error("请启动REmote DIctionary Server");
        }

        JSONObject object = DBUtil.getObjectBySQL("select * from operator_systemsetting where platform = ? limit 1", new Object[]{platform});
        if (object == null) {
            return null;
        } else {
            if (b) {
                this.redisService.insertKey(key, String.valueOf(object), 1800L);
            }

            return object;
        }
    }

    public boolean getStartStatus(String platform, String account) {
        if (null == platform) {
            platform = "HYQP";
        }

        JSONObject sysInfo = this.getSysInfo(platform);
        boolean b = !sysInfo.containsKey("is_start_game") ? true : 1 == sysInfo.getInt("is_start_game");
        if (null != account && !b) {
            try {
                JSONObject object = DBUtil.getObjectBySQL("SELECT * FROM za_users_test z WHERE z.`account`=? AND z.`is_control`=1 AND z.`is_deleted`=0", new Object[]{account});
                if (null != object) {
                    b = true;
                }
            } catch (Exception var6) {
                this.log.info("za_users_test未设置");
            }
        }

        return b;
    }

    public boolean idempotent(String key) {
        try {
            Object o = this.redisService.queryValueByKey(key);
            if (null != o) {
                return false;
            } else {
                this.redisService.insertKey(key, "1", 300L);
                return true;
            }
        } catch (Exception var3) {
            this.log.info("幂等性 验证失败 redis 宕机");
            return true;
        }
    }

    public JSONArray getGameSssSpecialSetting() {
        String key = "za_game_sss_special_setting";
        boolean b = true;

        try {
            Object o = this.redisService.queryValueByKey(key);
            if (null != o) {
                return JSONArray.fromObject(o);
            }
        } catch (Exception var5) {
            b = false;
            this.log.error("请启动REmote DIctionary Server");
        }

        String sql = "SELECT * FROM za_game_sss_special_setting z WHERE z.`id_deleted`=? AND z.`is_use`=? AND z.`multiple`>? ORDER BY z.`multiple`DESC";
        JSONArray gameSssSpecialSettin = DBUtil.getObjectListBySQL(sql, new Object[]{0, 1, 0});
        if (null != gameSssSpecialSettin && gameSssSpecialSettin.size() > 0) {
            if (b) {
                this.redisService.insertKey(key, String.valueOf(gameSssSpecialSettin), 1800L);
            }

            return gameSssSpecialSettin;
        } else {
            return null;
        }
    }

    public void delGameSssSpecialSetting() {
        this.redisService.deleteByLikeKey("za_game_sss_special_setting");
    }

    public boolean getHasControl(String platform) {
        if (null == platform) {
            platform = "HYQP";
        }

        String key = "operator_systemsetting";
        boolean b = true;

        JSONObject operatorSystemsetting;
        try {
            Object o = this.redisService.queryValueByKey(key);
            operatorSystemsetting = JSONObject.fromObject(o);
            if (null != o && null != operatorSystemsetting) {
                if (operatorSystemsetting.containsKey("has_control") && "1".equals(operatorSystemsetting.getString("has_control"))) {
                    return true;
                }

                if (operatorSystemsetting.containsKey("has_con") && "1".equals(operatorSystemsetting.getString("has_con"))) {
                    return true;
                }

                return false;
            }
        } catch (Exception var6) {
            b = false;
            this.log.error("请启动REmote DIctionary Server");
        }

        String sql = "SELECT * FROM operator_systemsetting z WHERE z.`platform`=? ";
        operatorSystemsetting = DBUtil.getObjectBySQL(sql, new Object[]{platform});
        if (null != operatorSystemsetting) {
            if (b) {
                this.redisService.insertKey(key, String.valueOf(operatorSystemsetting), 86400L);
            }

            if (operatorSystemsetting.containsKey("has_control") && "1".equals(operatorSystemsetting.getString("has_control"))) {
                return true;
            }

            if (operatorSystemsetting.containsKey("has_con") && "1".equals(operatorSystemsetting.getString("has_con"))) {
                return true;
            }
        }

        return false;
    }

    public void delHasControl() {
        this.redisService.deleteByLikeKey("operator_systemsetting");
    }

    public long createCircle(String uid) {
        return this.redisService.incr("add_circle:" + uid, 1L);
    }

    public String getCreateCircle(String uid) {
        return (String)this.redisService.queryValueByKey("add_circle:" + uid);
    }

    public void delCreateCircle(String uid) {
        this.redisService.deleteByLikeKey("add_circle:" + uid);
    }

    public void delAllCreateCircle() {
        this.redisService.deleteByLikeKey("add_circle");
    }

    public Map<String, String> getSokcetInfo() {
        HashMap map = new HashMap();

        try {
            String key = "sys_socket";
            JSONArray data = null;
            Object s = this.redisService.queryValueByKey(key);
            if (null != s) {
                data = JSONArray.fromObject(s);
            }

            if (null == data) {
                data = DBUtil.getObjectListBySQL("SELECT * FROM sys_socket s WHERE s.`is_deleted`=0", new Object[0]);
                if (null == data || data.size() == 0) {
                    return map;
                }

                this.redisService.insertKey(key, String.valueOf(data), 600L);
            }

            int i = (int)((double)data.size() * Math.random());
            if (data.getJSONObject(i).containsKey("socket_ip") && data.getJSONObject(i).containsKey("socket_port") && data.getJSONObject(i).containsKey("headimg_url") && data.getJSONObject(i).containsKey("headimg_url")) {
                map.put("socketIp", data.getJSONObject(i).getString("socket_ip"));
                map.put("socketPort", data.getJSONObject(i).getString("socket_port"));
                map.put("headimgUrl", data.getJSONObject(i).getString("headimg_url"));
                map.put("payUrl", data.getJSONObject(i).getString("pay_url"));
                System.out.println("=================================================");
                System.out.println("SOCKET:"+map);
                System.out.println("=================================================");
            }
        } catch (Exception var6) {
            this.log.info("server获取sys_socket异常{}", var6);
        }

        return map;
    }

    public void delSokcetInfo() {
        String key = "sys_socket";
        this.redisService.deleteByLikeKey(key);
    }

    public JSONObject getAttachGameLog(Object sole) {
        if (null == sole) {
            return null;
        } else {
            String key = "za_game_log_attach" + ":" + sole;
            boolean b = true;

            try {
                Object o = this.redisService.queryValueByKey(key);
                if (o != null) {
                    return JSONObject.fromObject(o);
                }
            } catch (Exception var10) {
                b = false;
                this.log.error("请启动REmote DIctionary Server");
            }

            JSONObject object = DBUtil.getObjectBySQL("select * from za_game_log_attach where sole_id = ? ", new Object[]{sole});
            if (null != object && object.containsKey("extra_info")) {
                JSONArray extraInfo = object.getJSONArray("extra_info");
                object = new JSONObject();
                if (null != extraInfo && extraInfo.size() > 0) {
                    for(int i = 0; i < extraInfo.size(); ++i) {
                        JSONObject o = extraInfo.getJSONObject(i);
                        if (null != o) {
                            Iterator var8 = o.keySet().iterator();

                            while(var8.hasNext()) {
                                Object str = var8.next();
                                object.element(String.valueOf(str), o.get(str));
                            }
                        }
                    }
                }

                if (b) {
                    this.redisService.insertKey(key, String.valueOf(object), 86400L);
                }

                return object;
            } else {
                return null;
            }
        }
    }

    public long readMessageRepeatCheck(long userId) {
        return this.redisService.incr("read_message_circle_check:" + userId, 1L);
    }

    public void delReadMessageRepeatCheck(long userId) {
        this.redisService.deleteByLikeKey("read_message_circle_check:" + userId);
    }

    public void delAllReadMessageRepeatCheck() {
        this.redisService.deleteByLikeKey("read_message_circle_check");
    }
}
