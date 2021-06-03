package com.zhuoan.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBUtil {
    private static final Logger logger = LoggerFactory.getLogger(DBUtil.class);

    public DBUtil() {
    }

    public static JSONObject getObjectPageBySQL(String field, String conditions, Object[] params, Integer page, Integer limitNum) {
        JSONObject object = new JSONObject();
        if (page == null || page < 1) {
            page = 1;
        }

        if (limitNum == null || limitNum < 1) {
            limitNum = 10;
        }

        Connection conn = DBConnectionPool.getConnection();
        JSONArray jsonArray = new JSONArray();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int totalCount = 0;

        int columCount;
        try {
            String sql = "select " + field + " FROM " + conditions + " LIMIT ?,?";
            pstmt = conn.prepareStatement(sql);
            if (params != null && params.length > 0) {
                for(columCount = 0; columCount < params.length; ++columCount) {
                    if (!(params[columCount] instanceof JSONObject) && !(params[columCount] instanceof JSONArray)) {
                        pstmt.setObject(columCount + 1, params[columCount]);
                    } else {
                        pstmt.setString(columCount + 1, params[columCount].toString());
                    }
                }
            }

            pstmt.setObject(params.length + 1, (page - 1) * limitNum);
            pstmt.setObject(params.length + 2, limitNum);
            rs = pstmt.executeQuery();
            columCount = rs.getMetaData().getColumnCount();

            int i;
            while(rs.next()) {
                JSONObject o = new JSONObject();

                for(i = 0; i < columCount; ++i) {
                    o.put(rs.getMetaData().getColumnLabel(i + 1), rs.getObject(i + 1));
                }

                jsonArray.add(o);
            }

            sql = "select count(*) FROM (" + "select " + field + " FROM " + conditions + ")f";
            pstmt = conn.prepareStatement(sql);
            if (params != null && params.length > 0) {
                for(i = 0; i < params.length; ++i) {
                    if (!(params[i] instanceof JSONObject) && !(params[i] instanceof JSONArray)) {
                        pstmt.setObject(i + 1, params[i]);
                    } else {
                        pstmt.setString(i + 1, params[i].toString());
                    }
                }
            }

            rs = pstmt.executeQuery();
            columCount = rs.getMetaData().getColumnCount();

            while(rs.next()) {
                for(i = 0; i < columCount; ++i) {
                    totalCount = rs.getInt(i + 1);
                }
            }
        } catch (SQLException var18) {
            logger.error("SQL执行出错", var18);
        } finally {
            close(conn, pstmt, rs);
        }

        int totalPage = 0;
        if (totalCount > 0) {
            columCount = totalCount / limitNum;
            if (totalCount % limitNum > 0) {
                ++columCount;
            }

            totalPage = columCount;
        }

        object.element("list", jsonArray).element("pageIndex", page).element("pageSize", limitNum).element("totalPage", totalPage).element("totalCount", totalCount);
        return object;
    }

    public static JSONObject getObjectLimitPageBySQL(String field, String conditions, Object[] params, Integer page, Integer limitNum, Integer limitPage) {
        JSONObject object = new JSONObject();
        if (page == null || page < 1) {
            page = 1;
        }

        if (limitNum == null || limitNum < 1) {
            limitNum = 10;
        }

        Connection conn = DBConnectionPool.getConnection();
        JSONArray jsonArray = new JSONArray();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        int totalCount = 0;

        int columCount;
        try {
            String sql = "select " + field + " FROM " + conditions + " LIMIT ?,?";
            pstmt = conn.prepareStatement(sql);
            if (params != null && params.length > 0) {
                for(columCount = 0; columCount < params.length; ++columCount) {
                    if (!(params[columCount] instanceof JSONObject) && !(params[columCount] instanceof JSONArray)) {
                        pstmt.setObject(columCount + 1, params[columCount]);
                    } else {
                        pstmt.setString(columCount + 1, params[columCount].toString());
                    }
                }
            }

            pstmt.setObject(params.length + 1, (page - 1) * limitNum);
            pstmt.setObject(params.length + 2, limitNum);
            rs = pstmt.executeQuery();
            columCount = rs.getMetaData().getColumnCount();

            int i;
            while(rs.next()) {
                JSONObject o = new JSONObject();

                for(i = 0; i < columCount; ++i) {
                    o.put(rs.getMetaData().getColumnLabel(i + 1), rs.getObject(i + 1));
                }

                jsonArray.add(o);
            }

            sql = "select count(*) FROM (" + "select " + field + " FROM " + conditions + ")f";
            pstmt = conn.prepareStatement(sql);
            if (params != null && params.length > 0) {
                for(i = 0; i < params.length; ++i) {
                    if (!(params[i] instanceof JSONObject) && !(params[i] instanceof JSONArray)) {
                        pstmt.setObject(i + 1, params[i]);
                    } else {
                        pstmt.setString(i + 1, params[i].toString());
                    }
                }
            }

            rs = pstmt.executeQuery();
            columCount = rs.getMetaData().getColumnCount();

            while(rs.next()) {
                for(i = 0; i < columCount; ++i) {
                    totalCount = rs.getInt(i + 1);
                    if (limitPage != -1 && limitPage * limitNum < totalCount) {
                        totalCount = limitPage * limitNum;
                    }
                }
            }
        } catch (SQLException var19) {
            logger.error("SQL执行出错", var19);
        } finally {
            close(conn, pstmt, rs);
        }

        int totalPage = 0;
        if (totalCount > 0) {
            columCount = totalCount / limitNum;
            if (totalCount % limitNum > 0) {
                ++columCount;
            }

            totalPage = columCount;
        }

        object.element("list", jsonArray).element("pageIndex", page).element("pageSize", limitNum).element("totalPage", totalPage).element("totalCount", totalCount);
        return object;
    }

    public static JSONArray getObjectListBySQL(String sql, Object[] params) {
        Connection conn = DBConnectionPool.getConnection();
        JSONArray jsonArray = new JSONArray();
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = conn.prepareStatement(sql);
            int columCount;
            if (params != null && params.length > 0) {
                for(columCount = 0; columCount < params.length; ++columCount) {
                    pstmt.setObject(columCount + 1, params[columCount]);
                }
            }

            rs = pstmt.executeQuery();
            columCount = rs.getMetaData().getColumnCount();

            while(rs.next()) {
                JSONObject jsonObject = new JSONObject();

                for(int i = 0; i < columCount; ++i) {
                    jsonObject.put(rs.getMetaData().getColumnLabel(i + 1), rs.getObject(i + 1));
                }

                jsonArray.add(jsonObject);
            }

            JSONArray var14 = jsonArray;
            return var14;
        } catch (SQLException var12) {
            logger.error("SQL执行出错", var12);
        } finally {
            close(conn, pstmt, rs);
        }

        return jsonArray;
    }

    public static JSONObject getObjectBySQL(String sql, Object[] params) {
        Connection conn = DBConnectionPool.getConnection();
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = conn.prepareStatement(sql);
            int columCount;
            if (params != null && params.length > 0) {
                for(columCount = 0; columCount < params.length; ++columCount) {
                    pstmt.setObject(columCount + 1, params[columCount]);
                }
            }

            rs = pstmt.executeQuery();
            columCount = rs.getMetaData().getColumnCount();
            if (!rs.next()) {
                return null;
            } else {
                JSONObject jsonObject = new JSONObject();

                for(int i = 0; i < columCount; ++i) {
                    jsonObject.put(rs.getMetaData().getColumnLabel(i + 1), rs.getString(i + 1));
                }

                JSONObject var13 = jsonObject;
                return var13;
            }
        } catch (SQLException var11) {
            logger.error("SQL执行出错", var11);
            return null;
        } finally {
            close(conn, pstmt, rs);
        }
    }

    public static int executeUpdateBySQL(String sql, Object[] params) {
        Connection conn = DBConnectionPool.getConnection();
        PreparedStatement pstmt = null;

        try {
            pstmt = conn.prepareStatement(sql);
            int i;
            if (params != null && params.length > 0) {
                for(i = 0; i < params.length; ++i) {
                    if (!(params[i] instanceof JSONObject) && !(params[i] instanceof JSONArray)) {
                        pstmt.setObject(i + 1, params[i]);
                    } else {
                        pstmt.setString(i + 1, params[i].toString());
                    }
                }
            }

            i = pstmt.executeUpdate();
            return i;
        } catch (SQLException var8) {
            logger.error("SQL执行出错", var8);
        } finally {
            close(conn, pstmt, (ResultSet)null);
        }

        return 0;
    }

    public static long executeUpdateGetKeyBySQL(String sql, Object[] params) {
        Connection conn = DBConnectionPool.getConnection();
        PreparedStatement pstmt = null;

        try {
            pstmt = conn.prepareStatement(sql, 1);
            if (params != null && params.length > 0) {
                for(int i = 0; i < params.length; ++i) {
                    if (!(params[i] instanceof JSONObject) && !(params[i] instanceof JSONArray)) {
                        pstmt.setObject(i + 1, params[i]);
                    } else {
                        pstmt.setString(i + 1, params[i].toString());
                    }
                }
            }

            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            long var5;
            if (rs.next()) {
                var5 = rs.getLong(1);
                return var5;
            }

            var5 = -1L;
            return var5;
        } catch (SQLException var10) {
            logger.error("SQL执行出错", var10);
        } finally {
            close(conn, pstmt, (ResultSet)null);
        }

        return 0L;
    }

    private static void close(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }

            if (pstmt != null) {
                pstmt.close();
            }

            if (conn != null) {
                DBConnectionPool.closeConnection(conn);
            }
        } catch (SQLException var4) {
            logger.error("释放资源出错", var4);
        }

    }
}
