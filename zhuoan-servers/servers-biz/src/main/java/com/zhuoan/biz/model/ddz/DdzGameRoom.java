

package com.zhuoan.biz.model.ddz;

import com.zhuoan.biz.model.GameRoom;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.sf.json.JSONObject;

public class DdzGameRoom extends GameRoom implements Serializable {
    private int multiple = 1;
    private List<String> landlordCard = new ArrayList();
    private List<String> lastCard = new ArrayList();
    private String lastOperateAccount = null;
    private String landlordAccount;
    private int focusIndex;
    private ConcurrentMap<String, UserPacketDdz> userPacketMap = new ConcurrentHashMap();
    private List<JSONObject> operateRecord = new ArrayList();
    private String winner;
    private int maxMultiple;
    private int reShuffleTime;

    public DdzGameRoom() {
    }

    public int getReShuffleTime() {
        return this.reShuffleTime;
    }

    public void setReShuffleTime(int reShuffleTime) {
        this.reShuffleTime = reShuffleTime;
    }

    public int getMultiple() {
        return this.multiple;
    }

    public void setMultiple(int multiple) {
        if (this.maxMultiple != 0 && multiple > this.maxMultiple) {
            this.multiple = this.maxMultiple;
        } else {
            this.multiple = multiple;
        }

    }

    public List<String> getLandlordCard() {
        return this.landlordCard;
    }

    public void setLandlordCard(List<String> landlordCard) {
        this.landlordCard = landlordCard;
    }

    public List<String> getLastCard() {
        return this.lastCard;
    }

    public void setLastCard(List<String> lastCard) {
        this.lastCard = lastCard;
    }

    public String getLandlordAccount() {
        return this.landlordAccount;
    }

    public void setLandlordAccount(String landlordAccount) {
        this.landlordAccount = landlordAccount;
    }

    public int getFocusIndex() {
        return this.focusIndex;
    }

    public void setFocusIndex(int focusIndex) {
        this.focusIndex = focusIndex;
    }

    public ConcurrentMap<String, UserPacketDdz> getUserPacketMap() {
        return this.userPacketMap;
    }

    public void setUserPacketMap(ConcurrentMap<String, UserPacketDdz> userPacketMap) {
        this.userPacketMap = userPacketMap;
    }

    public String getLastOperateAccount() {
        return this.lastOperateAccount;
    }

    public void setLastOperateAccount(String lastOperateAccount) {
        this.lastOperateAccount = lastOperateAccount;
    }

    public String getWinner() {
        return this.winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public List<JSONObject> getOperateRecord() {
        return this.operateRecord;
    }

    public void setOperateRecord(List<JSONObject> operateRecord) {
        this.operateRecord = operateRecord;
    }

    public int getMaxMultiple() {
        return this.maxMultiple;
    }

    public void setMaxMultiple(int maxMultiple) {
        this.maxMultiple = maxMultiple;
    }
}
