package com.zhuoan.queue;

import com.corundumstudio.socketio.SocketIOClient;
import java.io.Serializable;
import java.util.UUID;

public class Messages implements Serializable {
    private static final long serialVersionUID = 7021328701898559635L;
    private UUID sessionId;
    private Object dataObject;
    private int gid;
    private int sorts;
    private String idempotentUUID;

    public Messages(SocketIOClient client, Object dataObject, int gid, int sorts) {
        if (client == null) {
            this.sessionId = UUID.randomUUID();
        } else {
            this.sessionId = client.getSessionId();
        }

        this.dataObject = dataObject;
        this.gid = gid;
        this.sorts = sorts;
        this.idempotentUUID = UUID.randomUUID().toString();
    }

    public String getIdempotentUUID() {
        return this.idempotentUUID;
    }

    public void setIdempotentUUID(String idempotentUUID) {
        this.idempotentUUID = idempotentUUID;
    }

    public UUID getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public Object getDataObject() {
        return this.dataObject;
    }

    public void setDataObject(Object dataObject) {
        this.dataObject = dataObject;
    }

    public int getGid() {
        return this.gid;
    }

    public void setGid(int gid) {
        this.gid = gid;
    }

    public int getSorts() {
        return this.sorts;
    }

    public void setSorts(int sorts) {
        this.sorts = sorts;
    }
}
