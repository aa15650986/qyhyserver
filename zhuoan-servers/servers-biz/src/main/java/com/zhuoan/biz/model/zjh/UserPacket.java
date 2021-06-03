
package com.zhuoan.biz.model.zjh;

import com.zhuoan.biz.core.zjh.ZhaJinHuaCore;
import java.math.BigDecimal;
import java.util.Arrays;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class UserPacket {
    private Integer[] pai;
    public int type;
    private boolean win = false;
    private boolean isBanker = false;
    private int status = 0;
    private double score;
    public boolean isGenDaoDi = false;
    public boolean isShow = false;
    private JSONArray bipaiList = new JSONArray();
    private int winTimes;
    private int luck;
    public boolean isDo = false;
    public double allScore;
    
    
    
    

    public double getAllScore() {
		return allScore;
	}

	public void setAllScore(double allScore) {
		this.allScore = allScore;
	}

	public UserPacket() {
    }

    public Integer[] getPai() {
        return this.pai;
    }

    public void setPai(Integer[] pai) {
        this.pai = pai;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isWin() {
        return this.win;
    }

    public void setWin(boolean win) {
        this.win = win;
    }

    public boolean isBanker() {
        return this.isBanker;
    }

    public void setBanker(boolean isBanker) {
        this.isBanker = isBanker;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public double getScore() {
        return (new BigDecimal(this.score)).setScale(2, 4).doubleValue();
    }

    public void setScore(double score) {
        this.score = (new BigDecimal(score)).setScale(2, 4).doubleValue();
    }

    public JSONArray getBipaiList() {
        return this.bipaiList;
    }

    public void setBipaiList(JSONArray bipaiList) {
        this.bipaiList = bipaiList;
    }

    public int getWinTimes() {
        return this.winTimes;
    }

    public void setWinTimes(int winTimes) {
        this.winTimes = winTimes;
    }

    public int getLuck() {
        return this.luck;
    }

    public void setLuck(int luck) {
        this.luck = luck;
    }

    public void initUserPacket() {
        this.type = 0;
        this.win = false;
        this.score = 0.0D;
        this.isGenDaoDi = false;
        this.isShow = false;
        this.bipaiList = new JSONArray();
    }

    public void addBiPaiList(int index, Integer[] bipai) {
        JSONObject obj = new JSONObject();
        obj.put("index", index);
        obj.put("pai", bipai);
        obj.put("paiType", ZhaJinHuaCore.getPaiType(Arrays.asList(bipai)));
        this.bipaiList.add(obj);
    }
}
