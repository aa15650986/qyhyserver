
package com.zhuoan.util;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dto {
    private static final Logger logger = LoggerFactory.getLogger(Dto.class);

    public Dto() {
    }

    public static boolean stringIsNULL(String values) {
        return values == null || "".equals(values) || "null".equals(values) || "undefined".equals(values);
    }

    public static String getEntNumCode(int length) {
        String val = "";
        Random random = new Random();

        for(int i = 0; i < length; ++i) {
            String charOrNum = random.nextInt(2) % 2 == 0 ? "char" : "num";
            if ("char".equalsIgnoreCase(charOrNum)) {
                int choice = 65;
                val = val + (char)(choice + random.nextInt(26));
            } else if ("num".equalsIgnoreCase(charOrNum)) {
                val = val + String.valueOf(random.nextInt(10));
            }
        }

        return val;
    }

    public static boolean isNull(Object obj) {
        return obj == null || String.valueOf(obj).equals("null") || String.valueOf(obj).equals("");
    }

    public static boolean isObjNull(JSONObject obj) {
        return null == obj || obj.isEmpty() || String.valueOf(obj).equals("null") || String.valueOf(obj).equals("");
    }

    public static boolean isNull(JSONObject obj, String keyname) {
        return !obj.containsKey(keyname) || obj.get(keyname) == null || obj.get(keyname).equals("") || obj.get(keyname).equals("null");
    }

    public static String string_UTF_8(String values) throws UnsupportedEncodingException {
        if (!stringIsNULL(values)) {
            values = new String(values.getBytes("iso8859-1"), "utf-8");
        }

        return values;
    }

    public static JSONObject string_JSONObject(JSONObject obj) {
        if (!isNull(obj)) {
            Iterator keys = obj.keys();

            while(keys.hasNext()) {
                String key = keys.next().toString();
                String value = obj.optString(key);
                if (stringIsNULL(value)) {
                    obj.element(key, "");
                }
            }
        }

        return obj;
    }

    public static JSONArray string_JSONArray(JSONArray array) {
        for(int i = 0; i < array.size(); ++i) {
            JSONObject obj = array.getJSONObject(i);
            if (!isNull(obj)) {
                Iterator keys = obj.keys();

                while(keys.hasNext()) {
                    String key = keys.next().toString();
                    String value = obj.optString(key);
                    if (stringIsNULL(value)) {
                        obj.element(key, "");
                    }
                }
            }
        }

        return array;
    }

    public static JSONArray string_JSONArray2(JSONArray array, Object objs) {
        for(int i = 0; i < array.size(); ++i) {
            JSONObject obj = array.getJSONObject(i);
            if (!isNull(obj)) {
                Iterator keys = obj.keys();

                while(keys.hasNext()) {
                    String key = keys.next().toString();
                    String value = obj.optString(key);
                    if (stringIsNULL(value)) {
                        obj.element(key, objs);
                    }
                }
            }
        }

        return array;
    }

    public static void writeLog(String msg) {
        logger.info(msg);
    }

    public static void writeLog(String[] msgs) {
        logger.info(getString(msgs));
    }

    public static void writeLog(Object[] msgs) {
        logger.info(getString(msgs));
    }

    public static String getString(String[] strs) {
        StringBuffer sb = new StringBuffer();
        String[] var2 = strs;
        int var3 = strs.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            String str = var2[var4];
            sb.append(str);
        }

        return sb.toString();
    }

    public static String getString(Object[] strs) {
        StringBuffer sb = new StringBuffer();
        Object[] var2 = strs;
        int var3 = strs.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            Object str = var2[var4];
            sb.append(str + ",");
        }

        return sb.toString();
    }

    public static String getJSON(JSONArray arr) {
        String e = "";
        if (arr.size() != 0) {
            for(int i = 0; i < arr.size(); ++i) {
                e = e + arr.get(i) + ",";
            }

            e.subSequence(0, e.length() - 1);
        }

        return e;
    }

    public static void ERROR() {
        int i = 8 / 0;
    }

    public static double add(double a1, double b1) {
        BigDecimal a2 = new BigDecimal(Double.toString(a1));
        BigDecimal b2 = new BigDecimal(Double.toString(b1));
        return a2.add(b2).doubleValue();
    }

    public static double sub(double a1, double b1) {
        BigDecimal a2 = new BigDecimal(Double.toString(a1));
        BigDecimal b2 = new BigDecimal(Double.toString(b1));
        return a2.subtract(b2).doubleValue();
    }

    public static double mul(double a1, double b1) {
        BigDecimal a2 = new BigDecimal(Double.toString(a1));
        BigDecimal b2 = new BigDecimal(Double.toString(b1));
        return a2.multiply(b2).doubleValue();
    }

    public static double mul2(double a1, double b1) {
        BigDecimal a2 = new BigDecimal(Double.toString(a1));
        BigDecimal b2 = new BigDecimal(Double.toString(b1));
        String b = String.format("%.2f", a2.multiply(b2).doubleValue());
        return Double.valueOf(b);
    }

    public static double div(double a1, double b1, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("error");
        } else {
            BigDecimal a2 = new BigDecimal(Double.toString(a1));
            BigDecimal b2 = new BigDecimal(Double.toString(b1));
            return a2.divide(b2, scale, 4).doubleValue();
        }
    }

    public static List<Integer> arrayToList(int[] array, int index) {
        if (index < 0 || index >= array.length) {
            index = 0;
        }

        List<Integer> list = new ArrayList();

        for(int i = index; i < array.length; ++i) {
            list.add(array[i]);
        }

        return list;
    }

    public static boolean isNumeric(String string) {
        Pattern pattern = Pattern.compile("[1-9]\\d*");
        return pattern.matcher(string).matches();
    }

    public static boolean isNumber(Object o) {
        Pattern pattern = Pattern.compile("[0-9]*");
        return pattern.matcher(String.valueOf(o)).matches();
    }

    public static String zerolearing(double str) {
        String s = str + "";
        if (s.indexOf(".") > 0) {
            s = s.replaceAll("0+?$", "");
            s = s.replaceAll("[.]$", "");
        }

        return s;
    }

    public static boolean isJsonObj(String str) {
        try {
            JSONObject.fromObject(str);
            return true;
        } catch (Exception var2) {
            return false;
        }
    }

    public static boolean isJsonArray(String str) {
        try {
            JSONArray.fromObject(str);
            return true;
        } catch (Exception var2) {
            return false;
        }
    }
}
