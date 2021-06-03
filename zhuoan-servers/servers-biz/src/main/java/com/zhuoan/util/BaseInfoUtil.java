
package com.zhuoan.util;

import com.zhuoan.service.RedisInfoService;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class BaseInfoUtil {
    public BaseInfoUtil() {
    }

    public static int getCircleRoomCostByBaseInfo(JSONObject baseInfo, String payType) {
        try {
            if (baseInfo.containsKey("player") && baseInfo.containsKey("turn")) {
                int player = baseInfo.getInt("player");
                JSONObject turn = baseInfo.getJSONObject("turn");
                if (turn.containsKey("AANum")) {
                    int aaNum = turn.getInt("AANum");
                    if (turn.containsKey("increase")) {
                        aaNum += player > 4 ? player * turn.getInt("increase") : 4 * turn.getInt("increase");
                    }

                    return "2".equals(payType) ? aaNum : player * aaNum;
                }
            }
        } catch (Exception var5) {
        }

        return -1;
    }

    public static boolean checkBaseInfo(JSONObject baseInfo, int gameId) {
        RedisInfoService redisInfoService = (RedisInfoService)SpringTool.getBean(RedisInfoService.class);
        JSONObject object = redisInfoService.getGameInfoById(gameId);
        if (!object.containsKey("isUse") && !object.containsKey("setting")) {
            return false;
        } else {
            String isUse = object.getString("isUse");
            if (!"1".equals(isUse)) {
                return false;
            } else {
                JSONObject setting = object.getJSONObject("setting");
                if (setting.containsKey("openRoomType") && setting.getJSONArray("openRoomType").contains(baseInfo.getInt("roomType"))) {
                	if (21 == gameId || 10 == gameId || 3 == gameId || gameId ==1 || 6==gameId) {
                        return true;
                    }

                    if (baseInfo.containsKey("type") && setting.containsKey("openType") && setting.getJSONArray("openType").contains(baseInfo.getInt("type")) && (checkScoreRatio(baseInfo, setting) || gameId == 14) && checkMaxTimes(baseInfo, setting) && checkPlayerNum(baseInfo, setting)) {
                        return true;
                    }
                }

                return false;
            }
        }
    }

    public static String getSystemInfo(String key) {
        RedisInfoService redisInfoService = (RedisInfoService)SpringTool.getBean(RedisInfoService.class);
        JSONObject systemInfo = redisInfoService.getGlobalByKey(key);
        if (systemInfo == null) {
            return null;
        } else {
            return systemInfo.containsKey("global_value") ? systemInfo.getString("global_value") : null;
        }
    }

    private static boolean checkScoreRatio(JSONObject baseInfo, JSONObject gameInfo) {
        if (!gameInfo.containsKey("scoreRatio")) {
            return true;
        } else {
            int type = baseInfo.getInt("type");
            int ratio = 0;
            JSONArray array = gameInfo.getJSONArray("scoreRatio");
            int i;
            if (baseInfo.getInt("roomType") == 3) {
                if (!baseInfo.containsKey("yuanbao") || !baseInfo.containsKey("leaveYB") || !baseInfo.containsKey("enterYB")) {
                    return false;
                }

                for(i = 0; i < array.size(); ++i) {
                    if (array.getJSONObject(i).containsKey("type") && array.getJSONObject(i).getInt("type") == type) {
                        ratio = array.getJSONObject(i).getInt("ybRatio");
                        break;
                    }
                }

                if (ratio == 0 || baseInfo.getDouble("yuanbao") <= 0.0D || baseInfo.getDouble("yuanbao") * (double)ratio > baseInfo.getDouble("leaveYB") || baseInfo.getDouble("yuanbao") * (double)ratio > baseInfo.getDouble("enterYB")) {
                    return false;
                }
            } else if (baseInfo.getInt("roomType") == 1) {
                if (!baseInfo.containsKey("di") || !baseInfo.containsKey("goldCoinEnter") || !baseInfo.containsKey("goldCoinLeave")) {
                    return false;
                }

                for(i = 0; i < array.size(); ++i) {
                    if (array.getJSONObject(i).containsKey("type") && array.getJSONObject(i).getInt("type") == type) {
                        ratio = array.getJSONObject(i).getInt("jbRatio");
                        break;
                    }
                }

                if (ratio == 0 || baseInfo.getDouble("di") <= 0.0D || baseInfo.getDouble("di") * (double)ratio > baseInfo.getDouble("goldCoinLeave") || baseInfo.getDouble("di") * (double)ratio > baseInfo.getDouble("goldCoinEnter")) {
                    return false;
                }
            }

            return true;
        }
    }

    private static boolean checkMaxTimes(JSONObject baseInfo, JSONObject gameInfo) {
        JSONArray array;
        int i;
        if (baseInfo.containsKey("baseNum") && gameInfo.containsKey("maxXzTimes")) {
            array = baseInfo.getJSONArray("baseNum");

            for(i = 0; i < array.size(); ++i) {
                JSONObject obj = array.getJSONObject(i);
                if (obj.getInt("val") < 0 || obj.getInt("val") > gameInfo.getInt("maxXzTimes")) {
                    return false;
                }
            }
        }

        if (baseInfo.containsKey("qzTimes") && gameInfo.containsKey("maxQzTimes")) {
            array = baseInfo.getJSONArray("qzTimes");

            for(i = 0; i < array.size(); ++i) {
                if (array.getInt(i) < 0 || array.getInt(i) > gameInfo.getInt("maxQzTimes")) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean checkPlayerNum(JSONObject baseInfo, JSONObject gameInfo) {
        int playerNum = baseInfo.getInt("player");
        if (2 > playerNum) {
            return false;
        } else {
            return !baseInfo.containsKey("maxPlayer") || playerNum <= gameInfo.getInt("maxplayer");
        }
    }
}
