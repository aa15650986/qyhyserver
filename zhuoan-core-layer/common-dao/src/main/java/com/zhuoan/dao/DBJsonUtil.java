
package com.zhuoan.dao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.sf.json.JSONObject;

public class DBJsonUtil {
    public DBJsonUtil() {
    }

    public static int saveOrUpdate(JSONObject jsonObject, String tablename) {
        if (jsonObject.has("id")) {
            return jsonObject.getLong("id") < 0L ? add(jsonObject, tablename) : update(jsonObject, tablename);
        } else {
            return add(jsonObject, tablename);
        }
    }

    public static long saveOrUpdateGetId(JSONObject jsonObject, String tablename) {
        if (jsonObject.has("id")) {
            return jsonObject.getLong("id") < 0L ? addGetId(jsonObject, tablename) : (long)update(jsonObject, tablename);
        } else {
            return addGetId(jsonObject, tablename);
        }
    }

    public static long addGetId(JSONObject jsonObject, String tablename) {
        String sqlHead = "insert into " + tablename + "(";
        String sqlTail = ") values (";
        List<Object> paramList = new ArrayList();
        Iterator iterator = jsonObject.keys();

        String sql;
        while(iterator.hasNext()) {
            sql = (String)iterator.next();
            Object value = jsonObject.get(sql);
            if (sql == "id") {
            }

            sqlHead = sqlHead + sql + ",";
            sqlTail = sqlTail + "?,";
            paramList.add(value);
        }

        sqlHead = sqlHead.substring(0, sqlHead.length() - 1);
        sqlTail = sqlTail.substring(0, sqlTail.length() - 1);
        sql = sqlHead + sqlTail + ")";
        Object[] params = new Object[paramList.size()];

        for(int i = 0; i < paramList.size(); ++i) {
            params[i] = paramList.get(i);
        }

        return DBUtil.executeUpdateGetKeyBySQL(sql, params);
    }

    public static int add(JSONObject jsonObject, String tablename) {
        String sqlHead = "insert into " + tablename + "(";
        String sqlTail = ") values (";
        List<Object> paramList = new ArrayList();
        Iterator iterator = jsonObject.keys();

        String sql;
        while(iterator.hasNext()) {
            sql = (String)iterator.next();
            Object value = jsonObject.get(sql);
            if (sql == "id") {
            }

            sqlHead = sqlHead + sql + ",";
            sqlTail = sqlTail + "?,";
            paramList.add(value);
        }

        sqlHead = sqlHead.substring(0, sqlHead.length() - 1);
        sqlTail = sqlTail.substring(0, sqlTail.length() - 1);
        sql = sqlHead + sqlTail + ")";
        Object[] params = new Object[paramList.size()];

        for(int i = 0; i < paramList.size(); ++i) {
            params[i] = paramList.get(i);
        }

        return DBUtil.executeUpdateBySQL(sql, params);
    }

    public static int update(JSONObject jsonObject, String tablename) {
        String sql = "update " + tablename + " set ";
        List<Object> paramList = new ArrayList();
        Object paramTail = null;
        Iterator iterator = jsonObject.keys();

        while(iterator.hasNext()) {
            String key = (String)iterator.next();
            Object value = jsonObject.get(key);
            if ("id".equals(key)) {
                paramTail = value;
            } else {
                sql = sql + key + "=?,";
                paramList.add(value);
            }
        }

        sql = sql.substring(0, sql.length() - 1);
        sql = sql + " where id=?";
        if (null == paramTail) {
            return 0;
        } else {
            paramList.add(paramTail);
            Object[] params = new Object[paramList.size()];

            for(int i = 0; i < paramList.size(); ++i) {
                params[i] = paramList.get(i);
            }

            return DBUtil.executeUpdateBySQL(sql, params);
        }
    }

    public static int update(JSONObject jsonObject, String tablename, String keyname) {
        String sql = "update " + tablename + " set ";
        List<Object> paramList = new ArrayList();
        Object paramTail = null;
        Iterator iterator = jsonObject.keys();

        while(iterator.hasNext()) {
            String key = (String)iterator.next();
            Object value = jsonObject.get(key);
            if (key == keyname) {
                paramTail = value;
            } else {
                sql = sql + key + "=?,";
                paramList.add(value);
            }
        }

        sql = sql.substring(0, sql.length() - 1);
        sql = sql + " where " + keyname + "=?";
        if (paramTail == null) {
            return 0;
        } else {
            paramList.add(paramTail);
            Object[] params = new Object[paramList.size()];

            for(int i = 0; i < paramList.size(); ++i) {
                params[i] = paramList.get(i);
            }

            return DBUtil.executeUpdateBySQL(sql, params);
        }
    }

    public static int update(JSONObject jsonObject, String tableName, String[] keyNames) {
        if (null != keyNames && keyNames.length != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("update ").append(tableName).append(" set ");
            List paramList = new ArrayList();
            Iterator iterator = jsonObject.keys();

            while(iterator.hasNext()) {
                String key = (String)iterator.next();
                Object value = jsonObject.get(key);
                boolean b = true;

                for(int i = 0; i < keyNames.length; ++i) {
                    if (key == keyNames[i]) {
                        paramList.add(value);
                        b = false;
                        break;
                    }
                }

                if (b) {
                    sb.append(key).append("=?,");
                    paramList.add(value);
                }
            }

            sb.deleteCharAt(sb.length() - 1);
            sb.append(" where 1=1 ");

            for(int i = 0; i < keyNames.length; ++i) {
                sb.append(" AND ").append(keyNames[i]).append("= ? ");
            }

            return DBUtil.executeUpdateBySQL(sb.toString(), paramList.toArray());
        } else {
            return -1;
        }
    }
}
