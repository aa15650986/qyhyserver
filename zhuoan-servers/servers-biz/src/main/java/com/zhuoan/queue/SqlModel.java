package com.zhuoan.queue;

import java.io.Serializable;
import java.util.Arrays;
import net.sf.json.JSONArray;

public class SqlModel implements Serializable {
    private static final long serialVersionUID = -5236848137376023025L;
    public static final int GETOBJECTBYSQL = 1;
    public static final int GETOBJECTLISTBYSQL = 2;
    public static final int EXECUTEUPDATEBYSQL = 3;
    public static final int PUMP = 4;
    public static final int SAVELOGS_NN = 5;
    public static final int SAVELOGS_SSS = 6;
    public static final int SAVELOGS_ZJH = 7;
    public static final int SAVELOGS_BDX = 8;
    public String sql;
    public Object[] params;
    public int type;
    JSONArray userIds;
    String roomNo;
    int gid;
    double fee;
    String type1;

    public SqlModel(String sql, Object[] params, int type) {
        this.sql = sql;
        this.params = params;
        this.type = type;
    }

    public SqlModel(int type, JSONArray userIds, String roomNo, int gid, double fee, String type1) {
        this.type = type;
        this.userIds = userIds;
        this.roomNo = roomNo;
        this.gid = gid;
        this.fee = fee;
        this.type1 = type1;
    }

    public String getSql() {
        return this.sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public Object[] getParams() {
        return this.params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public JSONArray getUserIds() {
        return this.userIds;
    }

    public void setUserIds(JSONArray userIds) {
        this.userIds = userIds;
    }

    public String getRoomNo() {
        return this.roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public int getGid() {
        return this.gid;
    }

    public void setGid(int gid) {
        this.gid = gid;
    }

    public double getFee() {
        return this.fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    public String getType1() {
        return this.type1;
    }

    public void setType1(String type1) {
        this.type1 = type1;
    }

    public String toString() {
        return "SqlModel{sql='" + this.sql + '\'' + ", params=" + Arrays.toString(this.params) + ", type=" + this.type + ", userIds=" + this.userIds + ", roomNo='" + this.roomNo + '\'' + ", gid=" + this.gid + ", fee=" + this.fee + ", type1='" + this.type1 + '\'' + '}';
    }
}
