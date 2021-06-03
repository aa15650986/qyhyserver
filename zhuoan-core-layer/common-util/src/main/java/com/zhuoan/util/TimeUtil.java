
package com.zhuoan.util;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class TimeUtil {
    private static SimpleDateFormat ymdhms = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static SimpleDateFormat ymd = new SimpleDateFormat("yyyy-MM-dd");
    private static Calendar calendar = Calendar.getInstance();
    public static String timeType1 = "yyyy-MM-dd HH:mm:ss";
    public static String timeType2 = "yyyy-MM-dd";

    public TimeUtil() {
    }

    public static String getNowDate() {
        String time = ymdhms.format(new Date());
        return time;
    }

    public static String getNowDateymd() {
        String time = ymd.format(new Date());
        return time;
    }

    public static String getNowDate(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        String time = sdf.format(new Date());
        return time;
    }

    public static String getMonAndDay() {
        String time = ymdhms.format(new Date());
        Timestamp timestamp = Timestamp.valueOf(time);
        calendar.setTime(timestamp);
        int month = calendar.get(2) + 1;
        int day = calendar.get(5);
        return month + "月" + day + "日";
    }

    public static String StrTsf(String timestr) throws ParseException {
        Date date = ymd.parse(timestr);
        return ymdhms.format(date);
    }

    public static Date StrTsfToDate(String timestr, String format) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date date = sdf.parse(timestr);
        return date;
    }

    public static String StrTsfToString(String timestr, String format) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date date = sdf.parse(timestr);
        String time = sdf.format(date);
        return time;
    }

    public static String transTimestamp(Timestamp time, String fromat) {
        if (null != time) {
            Date date = new Date(time.getTime());
            SimpleDateFormat sdf = new SimpleDateFormat(fromat);
            return sdf.format(date);
        } else {
            return null;
        }
    }

    public static JSONArray transTimestamp(JSONArray array, String key) {
        if (key != null && !"".equals(key) && array != null && array.size() != 0) {
            Date nowDate = new Date();

            for(int i = 0; i < array.size(); ++i) {
                JSONObject tmpobj = array.getJSONObject(i);
                if (tmpobj.get(key) != null) {
                    JSONObject time = tmpobj.getJSONObject(key);
                    if (time != null && time.containsKey("time")) {
                        nowDate.setTime(time.getLong("time"));
                        tmpobj.element(key, ymdhms.format(nowDate));
                    }
                }
            }

            return array;
        } else {
            return array;
        }
    }

    public static JSONArray transTimestamp(JSONArray array, String[] keys) {
        if (keys != null && keys.length != 0 && array != null && array.size() != 0) {
            Date nowDate = new Date();

            for(int i = 0; i < array.size(); ++i) {
                JSONObject tmpobj = array.getJSONObject(i);
                String[] var6 = keys;
                int var7 = keys.length;

                for(int var8 = 0; var8 < var7; ++var8) {
                    String key = var6[var8];
                    if (tmpobj.get(key) != null) {
                        JSONObject time = tmpobj.getJSONObject(key);
                        if (time != null && time.containsKey("time")) {
                            nowDate.setTime(time.getLong("time"));
                            tmpobj.element(key, ymdhms.format(nowDate));
                        }
                    }
                }
            }

            return array;
        } else {
            return array;
        }
    }

    public static JSONArray transTimestamp(JSONArray array, String key, String format) {
        if (key != null && !"".equals(key) && array != null && array.size() != 0) {
            Date nowDate = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat(format);

            for(int i = 0; i < array.size(); ++i) {
                JSONObject tmpobj = array.getJSONObject(i);
                if (tmpobj.get(key) != null) {
                    JSONObject time = tmpobj.getJSONObject(key);
                    nowDate.setTime(time.getLong("time"));
                    tmpobj.element(key, sdf.format(nowDate));
                }
            }

            return array;
        } else {
            return array;
        }
    }

    public static JSONArray transTimestamp(JSONArray array, String[] keys, String format) {
        if (keys != null && keys.length != 0 && array != null && array.size() != 0) {
            Date nowDate = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat(format);

            for(int i = 0; i < array.size(); ++i) {
                JSONObject tmpobj = array.getJSONObject(i);
                String[] var8 = keys;
                int var9 = keys.length;

                for(int var10 = 0; var10 < var9; ++var10) {
                    String key = var8[var10];
                    if (tmpobj.get(key) != null) {
                        JSONObject time = tmpobj.getJSONObject(key);
                        nowDate.setTime(time.getLong("time"));
                        tmpobj.element(key, sdf.format(nowDate));
                    }
                }
            }

            return array;
        } else {
            return array;
        }
    }

    public static JSONArray transTimestampToMd(JSONArray array, String key) {
        if (key != null && !"".equals(key) && array != null && array.size() != 0) {
            Date nowDate = new Date();

            for(int i = 0; i < array.size(); ++i) {
                JSONObject tmpobj = array.getJSONObject(i);
                if (tmpobj.get(key) != null) {
                    JSONObject time = tmpobj.getJSONObject(key);
                    nowDate.setTime(time.getLong("time"));
                    calendar.setTime(nowDate);
                    int month = calendar.get(2) + 1;
                    int day = calendar.get(5);
                    tmpobj.element(key, month + "月" + day + "日");
                }
            }

            return array;
        } else {
            return array;
        }
    }

    public static JSONObject transTimeStamp(JSONObject oData, String sSdfFormat, String sKey) {
        if (!"null".equals(oData.getString(sKey)) && oData.containsKey(sKey)) {
            SimpleDateFormat format = new SimpleDateFormat(sSdfFormat);
            Date nowDate = new Date();
            JSONObject time = oData.getJSONObject(sKey);
            nowDate.setTime(time.getLong("time"));
            oData.element(sKey, format.format(nowDate));
        }

        return oData;
    }

    public static JSONObject transTimeStamp(JSONObject oData, String sSdfFormat, String[] sKeys) {
        SimpleDateFormat format = new SimpleDateFormat(sSdfFormat);
        Date nowDate = new Date();
        String[] var5 = sKeys;
        int var6 = sKeys.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            String sKey = var5[var7];
            if (!"null".equals(oData.getString(sKey))) {
                JSONObject time = oData.getJSONObject(sKey);
                nowDate.setTime(time.getLong("time"));
                oData.element(sKey, format.format(nowDate));
            }
        }

        return oData;
    }

    public static boolean isLatter(String startTime, String endTime) {
        calendar.setTime(Timestamp.valueOf(startTime));
        long start = calendar.getTimeInMillis();
        calendar.setTime(Timestamp.valueOf(endTime));
        long end = calendar.getTimeInMillis();
        return start > end;
    }

    public static String getMonOrSat(boolean isMon) {
        dayCount(new Date());
        if (isMon) {
            return ymd.format(calendar.getTime());
        } else {
            calendar.add(5, 6);
            return ymd.format(calendar.getTime());
        }
    }

    private static Calendar dayCount(Date time) {
        calendar.setTime(time);
        int dayWeek = calendar.get(7);
        if (1 == dayWeek) {
            calendar.add(5, -1);
        }

        calendar.setFirstDayOfWeek(2);
        int day = calendar.get(7);
        calendar.add(5, calendar.getFirstDayOfWeek() - day);
        return calendar;
    }

    public static JSONArray transArrayToWeekOfDate(JSONArray array, String key, String newKey) {
        for(int i = 0; i < array.size(); ++i) {
            JSONObject tmpobj = array.getJSONObject(i);
            if (!"null".equals(tmpobj.getString(key))) {
                JSONObject time = tmpobj.getJSONObject(key);
                Date nowDate = new Date();
                nowDate.setTime(time.getLong("time"));
                String day = getWeekOfDate(nowDate);
                tmpobj.element(newKey, day);
            }
        }

        return array;
    }

    public static String getWeekOfDate(Date dt) {
        String[] weekDays = new String[]{"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        int w = cal.get(7) - 1;
        if (w < 0) {
            w = 0;
        }

        return weekDays[w];
    }

    public static String getDaysBetweenTwoTime(String time1, String time2, Long Time) {
        Timestamp start = Timestamp.valueOf(time1);
        Timestamp end = Timestamp.valueOf(time2);
        Long tempString = Math.abs(start.getTime() - end.getTime()) / Time;
        return String.valueOf(tempString);
    }

    public static String addYearBaseOnNowTime(String Time, int addCount) {
        calendar.setTime(Timestamp.valueOf(Time));
        calendar.add(1, addCount);
        return ymdhms.format(calendar.getTime());
    }

    public static String addTimeBaseOnNowTime(String Time, int addCount) {
        calendar.setTime(Timestamp.valueOf(Time));
        calendar.add(2, addCount);
        return ymdhms.format(calendar.getTime());
    }

    public static String addDaysBaseOnNowTime(String Time, int addCount, String format) {
        calendar.setTime(Timestamp.valueOf(Time));
        calendar.set(5, calendar.get(5) + addCount);
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(calendar.getTime());
    }

    public static String addHoursBaseOnNowTime(String Time, int addCount, String format) {
        calendar.setTime(Timestamp.valueOf(Time));
        calendar.add(11, addCount);
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(calendar.getTime());
    }

    public static String addSecondBaseOnNowTime(String Time, int addCount) {
        calendar.setTime(Timestamp.valueOf(Time));
        calendar.add(13, addCount);
        return ymdhms.format(calendar.getTime());
    }

    public static int getMonthDays(String time) {
        calendar.setTime(Timestamp.valueOf(time));
        return calendar.getActualMaximum(5);
    }

    public static String getPastDate(int past) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(6, calendar.get(6) - past);
        Date today = calendar.getTime();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        return format.format(today);
    }

    public static String getFirstDay(String thisday, int count) {
        calendar.setTime(Timestamp.valueOf(thisday));
        calendar.add(2, count);
        calendar.set(5, 1);
        String firstDay = ymd.format(calendar.getTime());
        return firstDay;
    }

    public static String getLastDay(String thisday) {
        calendar.setTime(Timestamp.valueOf(thisday));
        calendar.add(2, 1);
        calendar.set(5, 0);
        String lastDay = ymd.format(calendar.getTime());
        return lastDay;
    }
}
