

package com.zhuoan.biz.model.sw;

import com.zhuoan.biz.model.GameRoom;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONArray;

public class SwGameRoom extends GameRoom implements Serializable {
    private JSONArray baseNum = new JSONArray();
    private int treasure = 0;
    private JSONArray betArray = new JSONArray();
    private JSONArray summaryArray = new JSONArray();
    private double ratio;
    private int singleMax = 0;
    private Map<String, Integer> userUnBetTime = new HashMap();

    public SwGameRoom() {
    }

    public JSONArray getBaseNum() {
        return this.baseNum;
    }

    public void setBaseNum(JSONArray baseNum) {
        this.baseNum = baseNum;
    }

    public int getTreasure() {
        return this.treasure;
    }

    public void setTreasure(int treasure) {
        this.treasure = treasure;
    }

    public JSONArray getBetArray() {
        return this.betArray;
    }

    public void setBetArray(JSONArray betArray) {
        this.betArray = betArray;
    }

    public JSONArray getSummaryArray() {
        return this.summaryArray;
    }

    public void setSummaryArray(JSONArray summaryArray) {
        this.summaryArray = summaryArray;
    }

    public double getRatio() {
        return this.ratio;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }

    public int getSingleMax() {
        return this.singleMax;
    }

    public void setSingleMax(int singleMax) {
        this.singleMax = singleMax;
    }

    public Map<String, Integer> getUserUnBetTime() {
        return this.userUnBetTime;
    }

    public void setUserUnBetTime(Map<String, Integer> userUnBetTime) {
        this.userUnBetTime = userUnBetTime;
    }
}
