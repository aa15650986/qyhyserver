
package com.zhuoan.biz.model.gppj;

import com.zhuoan.biz.model.GameRoom;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.sf.json.JSONArray;

public class GPPJGameRoom extends GameRoom implements Serializable {
    private int bankerType;
    private JSONArray baseNum = new JSONArray();
    private JSONArray qzTimes = new JSONArray();
    private ConcurrentMap<String, UserPacketGPPJ> userPacketMap = new ConcurrentHashMap();
    private String[] pai;
    private JSONArray dice = new JSONArray();
    private int cutIndex;
    private int cutPlace;
    private int[] leftPai;
    private int multiple = -1;
    private JSONArray usedArray = new JSONArray();
    private String[] leftArray;

    public GPPJGameRoom() {
    }

    public int getBankerType() {
        return this.bankerType;
    }

    public void setBankerType(int bankerType) {
        this.bankerType = bankerType;
    }

    public JSONArray getBaseNum() {
        return this.baseNum;
    }

    public void setBaseNum(JSONArray baseNum) {
        this.baseNum = baseNum;
    }

    public JSONArray getQzTimes() {
        return this.qzTimes;
    }

    public void setQzTimes(JSONArray qzTimes) {
        this.qzTimes = qzTimes;
    }

    public ConcurrentMap<String, UserPacketGPPJ> getUserPacketMap() {
        return this.userPacketMap;
    }

    public void setUserPacketMap(ConcurrentMap<String, UserPacketGPPJ> userPacketMap) {
        this.userPacketMap = userPacketMap;
    }

    public String[] getPai() {
        return this.pai;
    }

    public void setPai(String[] pai) {
        this.pai = pai;
    }

    public JSONArray getDice() {
        return this.dice;
    }

    public void setDice(JSONArray dice) {
        this.dice = dice;
    }

    public int getCutIndex() {
        return this.cutIndex;
    }

    public void setCutIndex(int cutIndex) {
        this.cutIndex = cutIndex;
    }

    public int getCutPlace() {
        return this.cutPlace;
    }

    public void setCutPlace(int cutPlace) {
        this.cutPlace = cutPlace;
    }

    public int[] getLeftPai() {
        return this.leftPai;
    }

    public void setLeftPai(int[] leftPai) {
        this.leftPai = leftPai;
    }

    public int getMultiple() {
        return this.multiple;
    }

    public void setMultiple(int multiple) {
        this.multiple = multiple;
    }

    public JSONArray getUsedArray() {
        return this.usedArray;
    }

    public void setUsedArray(JSONArray usedArray) {
        this.usedArray = usedArray;
    }

    public String[] getLeftArray() {
        return this.leftArray;
    }

    public void setLeftArray(String[] leftArray) {
        this.leftArray = leftArray;
    }
}
