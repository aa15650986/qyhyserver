
package com.zhuoan.util;

import net.sf.json.JSONObject;

public class JsonUtil {
    public JsonUtil() {
    }

    public static boolean isNullVal(JSONObject object, String[] key) {
        try {
            if (object != null && key != null && key.length != 0) {
                String[] var2 = key;
                int var3 = key.length;

                for(int var4 = 0; var4 < var3; ++var4) {
                    String s = var2[var4];
                    if (!object.containsKey(s) || object.get(s) == null || "null".equals(object.getString(s)) || "".equals(object.getString(s))) {
                        return false;
                    }
                }

                return true;
            } else {
                return false;
            }
        } catch (Exception var6) {
            return false;
        }
    }

    public static boolean isNullVal(JSONObject object, String key) {
        try {
            if (object != null && key != null) {
                return object.containsKey(key) && object.get(key) != null && !"null".equals(object.getString(key)) && !"".equals(object.getString(key));
            } else {
                return false;
            }
        } catch (Exception var3) {
            return false;
        }
    }
}
