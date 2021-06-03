
package com.zhuoan.util;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateUtils {
    private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);
    public static final SimpleDateFormat yyyyMMdd = new SimpleDateFormat("yyyyMMdd");
    public static final SimpleDateFormat yyyy_MM_dd = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat yyyy_MM_dd_wz = new SimpleDateFormat("yyyy年MM月dd日");
    public static final SimpleDateFormat MM_dd_wz = new SimpleDateFormat("MM月dd日");
    public static final SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss");
    public static final SimpleDateFormat short_time_sdf = new SimpleDateFormat("HH:mm");
    public static final SimpleDateFormat time_sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    public static final SimpleDateFormat datetime_sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final DateFormat timedate_sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static final DateFormat MM_dd = new SimpleDateFormat("MM-dd");
    private static final long DAY_IN_MILLIS = 86400000L;
    private static final long HOUR_IN_MILLIS = 3600000L;
    private static final long MINUTE_IN_MILLIS = 60000L;
    private static final long SECOND_IN_MILLIS = 1000L;

    public DateUtils() {
    }

    public static SimpleDateFormat getSimpleDateFormat(String format) {
        SimpleDateFormat dateformat = new SimpleDateFormat(format);
        return dateformat;
    }

    public static Date getDate() {
        return new Date();
    }

    public static Date getDate(Timestamp time) {
        return time == null ? null : time;
    }

    public static Date getDate(Calendar calendar) {
        if (calendar == null) {
            return null;
        } else {
            Date date = calendar.getTime();
            return date;
        }
    }

    public static Date getDate(String time) {
        if (!isNotNull(time)) {
            return null;
        } else {
            String[] format = new String[]{"yyyyMMdd", "yyyy-MM-dd", "yyyy年MM月dd日", "yyyyMMddHHmmss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm"};
            Date date = null;
            String[] var3 = format;
            int var4 = format.length;
            int var5 = 0;

            while(var5 < var4) {
                String str = var3[var5];

                try {
                    date = getSimpleDateFormat(str).parse(time);
                    break;
                } catch (Exception var8) {
                    ++var5;
                }
            }

            return date;
        }
    }

    public static Date getDate(String time, String format) {
        Date date = null;
        if (!isNotNull(time)) {
            return null;
        } else if (!isNotNull(format)) {
            return getDate(time);
        } else {
            try {
                date = getSimpleDateFormat(format).parse(time);
            } catch (ParseException var4) {
                logger.error("", var4);
            }

            return date;
        }
    }

    public static Date getDate(long time) {
        return new Date(time);
    }

    public static Timestamp getTimestamp() {
        return new Timestamp((new Date()).getTime());
    }

    public static Timestamp getTimestamp(Date date) {
        return date == null ? null : new Timestamp(date.getTime());
    }

    public static Timestamp getTimestamp(Calendar calendar) {
        if (calendar == null) {
            return null;
        } else {
            Date date = calendar.getTime();
            return getTimestamp(date);
        }
    }

    public static Timestamp getTimestamp(String time, String format) {
        return !isNotNull(time) ? null : getTimestamp(getDate(time, format));
    }

    public static Timestamp getTimestamp(long time) {
        return new Timestamp(time);
    }

    public static Calendar getCalendar() {
        return Calendar.getInstance();
    }

    public static Calendar getCalendar(Date date) {
        if (date == null) {
            return null;
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return calendar;
        }
    }

    public static Calendar getCalendar(Timestamp time) {
        if (time == null) {
            return null;
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(time);
            return calendar;
        }
    }

    public static Calendar getCalendar(String time, String format) {
        return !isNotNull(time) ? null : getCalendar(getDate(time, format));
    }

    public static Calendar getCalendar(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return calendar;
    }

    public static String getStringTime(String format) {
        return !isNotNull(format) ? null : getStringTime(getDate(), format);
    }

    public static String getStringTime(Date date, SimpleDateFormat date_sdf) {
        return date == null ? null : date_sdf.format(date);
    }

    public static String getStringTime(Date date, String format) {
        return date != null && isNotNull(format) ? getSimpleDateFormat(format).format(date) : null;
    }

    public static String getStringTime(Calendar calendar, SimpleDateFormat date_sdf) {
        return calendar == null ? null : date_sdf.format(calendar);
    }

    public static String getStringTime(Calendar calendar, String format) {
        return calendar != null && isNotNull(format) ? getSimpleDateFormat(format).format(calendar) : null;
    }

    public static String getStringTime(Timestamp time, String format) {
        return time != null && isNotNull(format) ? getSimpleDateFormat(format).format(time) : null;
    }

    public static String getStringTime(Timestamp time, SimpleDateFormat date_sdf) {
        return time == null ? null : date_sdf.format(time);
    }

    public static String getStringTime(long time, String format) {
        return getStringTime(getDate(time), format);
    }

    public static String getStringTime(long time, SimpleDateFormat date_sdf) {
        return getStringTime(getDate(time), date_sdf);
    }

    public static long getLongTime() {
        return getDate().getTime();
    }

    public static long getLongTime(String format) {
        try {
            Date t = new Date();
            SimpleDateFormat sf = new SimpleDateFormat(format);
            String bizdate = sf.format(t);
            t = sf.parse(bizdate);
            return t.getTime();
        } catch (Exception var4) {
            return 0L;
        }
    }

    public static long getLongTime(Date date) {
        return date == null ? -1L : date.getTime();
    }

    public static long getLongTime(Timestamp date) {
        return date == null ? -1L : date.getTime();
    }

    public static long getLongTime(Calendar calendar) {
        return calendar == null ? -1L : calendar.getTimeInMillis();
    }

    public static long getLongTime(String time, String format) {
        return !isNotNull(time) ? -1L : getDate(time, format).getTime();
    }

    public static JSONObject transTimestamp(JSONObject obj, String key, String format) {
        if (!isNotNull(format)) {
            format = "yyyy-MM-dd HH:mm:ss";
        }

        if (isNotNull(obj, key)) {
            Date nowDate = new Date();
            nowDate.setTime(obj.getLong(key));
            obj.put(key, getSimpleDateFormat(format).format(nowDate));
        }

        return obj;
    }

    public static JSONObject transTimestamp(JSONObject obj, String[] keys, String format) {
        if (!isNotNull(format)) {
            format = "yyyy-MM-dd HH:mm:ss";
        }

        if (keys != null && keys.length > 0) {
            String[] var3 = keys;
            int var4 = keys.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                String key = var3[var5];
                if (isNotNull(obj, key)) {
                    Date nowDate = new Date();
                    nowDate.setTime(obj.getLong(key));
                    obj.put(key, getSimpleDateFormat(format).format(nowDate));
                }
            }
        }

        return obj;
    }

    public static JSONArray transTimestamp(JSONArray array, String key, String format) {
        if (!isNotNull(format)) {
            format = "yyyy-MM-dd HH:mm:ss";
        }

        if (array != null && array.size() > 0) {
            for(int i = 0; i < array.size(); ++i) {
                JSONObject tmpobj = array.getJSONObject(i);
                if (!isNotNull(tmpobj, key)) {
                    JSONObject time = tmpobj.getJSONObject(key);
                    Date nowDate = new Date();
                    nowDate.setTime(time.getLong("time"));
                    tmpobj.element(key, getSimpleDateFormat(format).format(nowDate));
                }
            }
        }

        return array;
    }

    public static JSONArray transTimestamp(JSONArray array, String[] keys, String format) {
        if (!isNotNull(format)) {
            format = "yyyy-MM-dd HH:mm:ss";
        }

        SimpleDateFormat sdf = new SimpleDateFormat(format);

        for(int i = 0; i < array.size(); ++i) {
            JSONObject tmpobj = array.getJSONObject(i);
            String[] var6 = keys;
            int var7 = keys.length;

            for(int var8 = 0; var8 < var7; ++var8) {
                String key = var6[var8];
                if (!"null".equals(tmpobj.getString(key))) {
                    JSONObject time = tmpobj.getJSONObject(key);
                    Date nowDate = new Date();
                    nowDate.setTime(time.getLong("time"));
                    tmpobj.element(key, sdf.format(nowDate));
                }
            }
        }

        return array;
    }

    public static int comparison(Date date1, Date date2) {
        if (date1 != null && date2 != null) {
            if (date1.getTime() > date2.getTime()) {
                return 1;
            } else {
                return date1.getTime() == date2.getTime() ? 2 : 3;
            }
        } else {
            return -1;
        }
    }

    public static int comparison(Timestamp date1, Timestamp date2) {
        if (date1 != null && date2 != null) {
            if (date1.getTime() > date2.getTime()) {
                return 1;
            } else {
                return date1.getTime() == date2.getTime() ? 2 : 3;
            }
        } else {
            return -1;
        }
    }

    public static int comparison(Calendar date1, Calendar date2) {
        if (date1 != null && date2 != null) {
            if (getLongTime(date1) > getLongTime(date2)) {
                return 1;
            } else {
                return getLongTime(date1) == getLongTime(date2) ? 2 : 3;
            }
        } else {
            return -1;
        }
    }

    public static int comparison(String time1, String time2, String format1, String format2) {
        if (!isNotNull(new String[]{time1, time2, format1})) {
            return -1;
        } else {
            if (!isNotNull(format2)) {
                format2 = format1;
            }

            return comparison(getDate(time1, format2), getDate(time1, format2));
        }
    }

    public static Date addAndSubtract(Date time, int year, int month, int days, int hour, int min, int second) {
        if (time == null) {
            return null;
        } else {
            Calendar c = addAndSubtract(getCalendar(time), year, month, days, hour, min, second);
            return getDate(c);
        }
    }

    public static Timestamp addAndSubtract(Timestamp time, int year, int month, int days, int hour, int min, int second) {
        if (time == null) {
            return null;
        } else {
            Calendar c = addAndSubtract(getCalendar(time), year, month, days, hour, min, second);
            return getTimestamp(getDate(c));
        }
    }

    public static Calendar addAndSubtract(Calendar calendar, int year, int month, int days, int hour, int min, int second) {
        if (calendar == null) {
            return null;
        } else {
            calendar.add(1, year);
            calendar.add(2, month);
            calendar.add(5, days);
            calendar.add(10, days);
            calendar.add(12, days);
            calendar.add(13, days);
            return calendar;
        }
    }

    public static String addAndSubtract(String time, String format, int year, int month, int days, int hour, int min, int second) {
        Calendar c = addAndSubtract(getCalendar(time, format), year, month, days, hour, min, second);
        return getStringTime(c, format);
    }

    public static long getTimeDistance(Date date1, Date date2, int backtype) {
        if (date1 != null && date2 != null) {
            long date1m = date1.getTime();
            long date2m = date2.getTime();
            long back = 0L;
            back = date1m - date2m;
            switch(backtype) {
                case 3:
                    back /= 86400000L;
                    break;
                case 4:
                    back /= 3600000L;
                    break;
                case 5:
                    back /= 60000L;
                    break;
                case 6:
                    back /= 1000L;
                case 7:
            }

            return back;
        } else {
            return -1L;
        }
    }

    public static long getTimeDistance(Date date1, Date date2, String backtype) {
        int type = 5;
        if (!isNotNull(backtype)) {
            type = getbacktype(backtype);
        }

        return getTimeDistance(date1, date2, type);
    }

    public static long getTimeDistance(Timestamp date1, Timestamp date2, int backtype) {
        return getTimeDistance(date1, date2, backtype);
    }

    public static long getTimeDistance(Timestamp date1, Timestamp date2, String backtype) {
        int type = 5;
        if (!isNotNull(backtype)) {
            type = getbacktype(backtype);
        }

        return getTimeDistance(date1, date2, type);
    }

    public static long getTimeDistance(Calendar calendar1, Calendar calendar2, int backtype) {
        return calendar1 != null && calendar2 != null ? getTimeDistance(calendar1.getTime(), calendar2.getTime(), backtype) : -1L;
    }

    public static long getTimeDistance(Calendar calendar1, Calendar calendar2, String backtype) {
        int type = 5;
        if (!isNotNull(backtype)) {
            type = getbacktype(backtype);
        }

        return getTimeDistance(calendar1, calendar2, type);
    }

    public static long getTimeDistance(String time1, String time2, String format1, String format2, int backtype) {
        if (!isNotNull(new String[]{time1, time2, format1})) {
            return -1L;
        } else {
            if (!isNotNull(format2)) {
                format2 = format1;
            }

            return getTimeDistance(getDate(time1, format1), getDate(time2, format2), backtype);
        }
    }

    public static long getTimeDistance(String time1, String time2, String format1, String format2, String backtype) {
        int type = 5;
        if (!isNotNull(backtype)) {
            type = getbacktype(backtype);
        }

        return getTimeDistance(time1, time2, format1, format2, type);
    }

    public static String getWeek(Date date) {
        if (date == null) {
            return null;
        } else {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return getWeek(cal);
        }
    }

    public static String getWeek(Timestamp date) {
        if (date == null) {
            return null;
        } else {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return getWeek(cal);
        }
    }

    public static String getWeek(Calendar calendar) {
        String[] weekDays = new String[]{"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        int w = calendar.get(7) - 1;
        if (w < 0) {
            w = 0;
        }

        return weekDays[w];
    }

    public static String getWeek(String time, String format) {
        return getWeek(getCalendar(time, format));
    }

    public static int getSingleDate(Date date, int backtype) {
        if (date == null) {
            return -1;
        } else {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(date);
            return getSingleDate((Calendar)calendar, backtype);
        }
    }

    public static int getSingleDate(Date date, String backtype) {
        return getSingleDate(date, getbacktype(backtype));
    }

    public static int getSingleDate(Timestamp date, int backtype) {
        if (date == null) {
            return -1;
        } else {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(date);
            return getSingleDate((Calendar)calendar, backtype);
        }
    }

    public static int getSingleDate(Timestamp date, String backtype) {
        return getSingleDate(date, getbacktype(backtype));
    }

    public static int getSingleDate(Calendar calendar, int backtype) {
        if (calendar == null) {
            return -1;
        } else {
            int back = 0;
            switch(backtype) {
                case 1:
                    back = calendar.get(1);
                    break;
                case 2:
                    back = calendar.get(2);
                    break;
                case 3:
                    back = calendar.get(5);
                    break;
                case 4:
                    back = calendar.get(10);
                    break;
                case 5:
                    back = calendar.get(12);
                    break;
                case 6:
                    back = calendar.get(13);
            }

            return back;
        }
    }

    public static int getSingleDate(Calendar calendar, String backtype) {
        return getSingleDate(calendar, getbacktype(backtype));
    }

    public static int getSingleDate(String time, String format, int backtype) {
        return getSingleDate(getCalendar(time, format), backtype);
    }

    public static int getSingleDate(String time, String format, String backtype) {
        return getSingleDate(time, format, getbacktype(backtype));
    }

    public static Date getWeekFirstDay(Date time) {
        if (time == null) {
            return null;
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(time);
            return getDate(getWeekFirstDay(calendar));
        }
    }

    public static Timestamp getWeekFirstDay(Timestamp time) {
        if (time == null) {
            return null;
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(time);
            return getTimestamp(getWeekFirstDay(calendar));
        }
    }

    public static Calendar getWeekFirstDay(Calendar calendar) {
        if (calendar == null) {
            return null;
        } else {
            int dayWeek = calendar.get(7);
            if (1 == dayWeek) {
                calendar.add(5, -1);
            }

            calendar.setFirstDayOfWeek(2);
            int day = calendar.get(7);
            calendar.add(5, calendar.getFirstDayOfWeek() - day);
            return calendar;
        }
    }

    public static String getWeekFirstDay(String time, String format) {
        return getStringTime(getWeekFirstDay(getCalendar(time, format)), format);
    }

    public static boolean isLegalDate(String sDate, String pattern) {
        int legalLen = 10;
        if (null != sDate && sDate.length() == legalLen) {
            SimpleDateFormat formatter = new SimpleDateFormat(pattern);

            try {
                Date date = formatter.parse(sDate);
                return sDate.equals(formatter.format(date));
            } catch (Exception var5) {
                return false;
            }
        } else {
            return false;
        }
    }

    private static boolean isNotNull(String str) {
        return str != null && !str.trim().isEmpty() && !str.equals("null");
    }

    private static boolean isNotNull(String[] strs) {
        if (strs != null && strs.length > 0) {
            String[] var1 = strs;
            int var2 = strs.length;

            for(int var3 = 0; var3 < var2; ++var3) {
                String str = var1[var3];
                if (!isNotNull(str)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isNotNull(JSONObject obj, String key) {
        return obj != null && !obj.containsKey(key) && obj.get(key) != null && !obj.getString(key).equals("null");
    }

    public static int getbacktype(String str) {
        int back = 1;
        if (!str.equals("yyyy") && !str.equals("YEAR") && !str.equals("year") && !str.equals("nian")) {
            if (!str.equals("MM") && !str.equals("MONTH") && !str.equals("month") && !str.equals("yue")) {
                if (!str.equals("dd") && !str.equals("Day") && !str.equals("day") && !str.equals("tian") && !str.equals("ri")) {
                    if (!str.equals("HH") && !str.equals("HOUR") && !str.equals("hour") && !str.equals("shi") && !str.equals("xiaoshi")) {
                        if (!str.equals("mm") && !str.equals("MINUTE") && !str.equals("minute") && !str.equals("fen") && !str.equals("fengzhong")) {
                            if (!str.equals("ss") && !str.equals("SECOND") && !str.equals("second") && !str.equals("miao")) {
                                if (str.equals("hs") || str.equals("MILLI") || str.equals("milli") || str.equals("MILLISECOND") || str.equals("millisecond") || str.equals("haomiao")) {
                                    back = 7;
                                }
                            } else {
                                back = 6;
                            }
                        } else {
                            back = 5;
                        }
                    } else {
                        back = 4;
                    }
                } else {
                    back = 3;
                }
            } else {
                back = 2;
            }
        } else {
            back = 1;
        }

        return back;
    }

    public static String searchTime(Object time) {
        String t = "";
        if (null != time && isLegalDate(time.toString(), "yyyy-MM-dd")) {
            t = time.toString();
        } else {
            t = getToday();
        }

        return t;
    }

    public static String getToday() {
        Calendar cal = Calendar.getInstance();
        cal.add(5, 0);
        return (new SimpleDateFormat("yyyy-MM-dd")).format(cal.getTime());
    }

    public static String getTodayTime() {
        Calendar cal = Calendar.getInstance();
        cal.add(5, 0);
        return (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(cal.getTime());
    }

    public static String getYesterday() {
        Calendar cal = Calendar.getInstance();
        cal.add(5, -1);
        return (new SimpleDateFormat("yyyy-MM-dd")).format(cal.getTime());
    }
}
