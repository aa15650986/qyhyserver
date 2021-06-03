
package com.zhuoan.biz.model.dao;

import java.io.Serializable;
import java.util.UUID;
import net.sf.json.JSONObject;

public class PumpDao implements Serializable {
    private static final long serialVersionUID = -6962256051875959108L;
    private String daoType;
    private JSONObject objectDao;
    private String idempotentUUID;

    public PumpDao() {
    }

    public PumpDao(String daoType, JSONObject objectDao) {
        this.daoType = daoType;
        this.objectDao = objectDao;
        this.idempotentUUID = UUID.randomUUID().toString();
    }

    public String getDaoType() {
        return this.daoType;
    }

    public void setDaoType(String daoType) {
        this.daoType = daoType;
    }

    public JSONObject getObjectDao() {
        return this.objectDao;
    }

    public String getIdempotentUUID() {
        return this.idempotentUUID;
    }

    public void setIdempotentUUID(String idempotentUUID) {
        this.idempotentUUID = idempotentUUID;
    }

    public void setObjectDao(JSONObject objectDao) {
        this.objectDao = objectDao;
    }

    public String toString() {
        return "PumpDao{daoType='" + this.daoType + '\'' + ", objectDao=" + this.objectDao + ", idempotentUUID=" + this.idempotentUUID + '}';
    }
}
