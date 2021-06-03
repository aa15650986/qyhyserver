
package com.zhuoan.biz.model.sss;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class SSSUserPacket implements Serializable {
    private String[] pai;
    private int status = 0;
    private int swat = 0;
    private double score;
    private int paiType;
    private int paiScore;
    private JSONObject headResult = new JSONObject();
    private JSONObject midResult = new JSONObject();
    private JSONObject footResult = new JSONObject();
    private int[] headPai;
    private int[] midPai;
    private int[] footPai;
    private int winTimes;
    private int dqTimes;
    private int bdqTimes;
    private int swatTimes;
    private int specialTimes;
    private int ordinaryTimes;
    private int xzTimes = 0;
    private int luck;
    private int timeLeft = 0;

    public SSSUserPacket() {
    }

    public String[] getPai() {
        return this.pai;
    }

    public void setPai(String[] pai) {
        this.pai = pai;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getSwat() {
        return this.swat;
    }

    public void setSwat(int swat) {
        this.swat = swat;
    }

    public double getScore() {
        return (new BigDecimal(this.score)).setScale(2, 4).doubleValue();
    }

    public void setScore(double score) {
        this.score = (new BigDecimal(score)).setScale(2, 4).doubleValue();
    }

    public int getPaiType() {
        return this.paiType;
    }

    public void setPaiType(int paiType) {
        this.paiType = paiType;
    }

    public int getPaiScore() {
        return this.paiScore;
    }

    public void setPaiScore(int paiScore) {
        this.paiScore = paiScore;
    }

    public JSONObject getHeadResult() {
        return this.headResult;
    }

    public void setHeadResult(JSONObject headResult) {
        this.headResult = headResult;
    }

    public JSONObject getMidResult() {
        return this.midResult;
    }

    public void setMidResult(JSONObject midResult) {
        this.midResult = midResult;
    }

    public JSONObject getFootResult() {
        return this.footResult;
    }

    public void setFootResult(JSONObject footResult) {
        this.footResult = footResult;
    }

    public int[] getHeadPai() {
        return this.headPai;
    }

    public void setHeadPai(int[] headPai) {
        this.headPai = headPai;
    }

    public int[] getMidPai() {
        return this.midPai;
    }

    public void setMidPai(int[] midPai) {
        this.midPai = midPai;
    }

    public int[] getFootPai() {
        return this.footPai;
    }

    public void setFootPai(int[] footPai) {
        this.footPai = footPai;
    }

    public int getWinTimes() {
        return this.winTimes;
    }

    public void setWinTimes(int winTimes) {
        this.winTimes = winTimes;
    }

    public int getDqTimes() {
        return this.dqTimes;
    }

    public void setDqTimes(int dqTimes) {
        this.dqTimes = dqTimes;
    }

    public int getBdqTimes() {
        return this.bdqTimes;
    }

    public void setBdqTimes(int bdqTimes) {
        this.bdqTimes = bdqTimes;
    }

    public int getSwatTimes() {
        return this.swatTimes;
    }

    public void setSwatTimes(int swatTimes) {
        this.swatTimes = swatTimes;
    }

    public int getSpecialTimes() {
        return this.specialTimes;
    }

    public void setSpecialTimes(int specialTimes) {
        this.specialTimes = specialTimes;
    }

    public int getOrdinaryTimes() {
        return this.ordinaryTimes;
    }

    public void setOrdinaryTimes(int ordinaryTimes) {
        this.ordinaryTimes = ordinaryTimes;
    }

    public int getXzTimes() {
        return this.xzTimes;
    }

    public void setXzTimes(int xzTimes) {
        this.xzTimes = xzTimes;
    }

    public int getLuck() {
        return this.luck;
    }

    public void setLuck(int luck) {
        this.luck = luck;
    }

    public int getTimeLeft() {
        return this.timeLeft;
    }

    public void setTimeLeft(int timeLeft) {
        this.timeLeft = timeLeft;
    }

    public int[] getMyPai() {
        int p = 0;
        if (null != this.pai) {
            p = this.pai.length;
        }

        int[] pais = new int[p];

        for(int i = 0; i < pais.length; ++i) {
            String[] val = this.pai[i].split("-");
            int num = 0;
            if (val[0].equals("2")) {
                num = 20;
            } else if (val[0].equals("3")) {
                num = 40;
            } else if (val[0].equals("4")) {
                num = 60;
            } else if (val[0].equals("5")) {
                num = 80;
            }

            pais[i] = Integer.valueOf(val[1]) + num;
        }

        return pais;
    }

    public JSONArray togetMyPai(JSONArray p) {
        JSONArray pais = new JSONArray();

        for(int i = 0; i < p.size(); ++i) {
            String a;
            if (p.getInt(i) < 20) {
                a = "1-" + p.getString(i);
                pais.add(a);
            } else if (p.getInt(i) > 20 && p.getInt(i) < 40) {
                a = "2-" + (p.getInt(i) - 20);
                pais.add(a);
            } else if (p.getInt(i) > 40 && p.getInt(i) < 60) {
                a = "3-" + (p.getInt(i) - 40);
                pais.add(a);
            } else if (p.getInt(i) > 60 && p.getInt(i) < 80) {
                a = "4-" + (p.getInt(i) - 60);
                pais.add(a);
            } else if (p.getInt(i) >= 80) {
                a = "5-" + (p.getInt(i) - 80);
                pais.add(a);
            }
        }

        return pais;
    }

    public void initUserPacket() {
        this.score = 0.0D;
        this.paiScore = 0;
        this.paiType = 0;
        this.swat = 0;
        this.xzTimes = 0;
        this.headResult.clear();
        this.midResult.clear();
        this.footResult.clear();
        this.pai = new String[0];
    }

	@Override
	public String toString() {
		return "SSSUserPacket [pai=" + Arrays.toString(pai) + ", status=" + status + ", swat=" + swat + ", score="
				+ score + ", paiType=" + paiType + ", paiScore=" + paiScore + ", headResult=" + headResult
				+ ", midResult=" + midResult + ", footResult=" + footResult + ", headPai=" + Arrays.toString(headPai)
				+ ", midPai=" + Arrays.toString(midPai) + ", footPai=" + Arrays.toString(footPai) + ", winTimes="
				+ winTimes + ", dqTimes=" + dqTimes + ", bdqTimes=" + bdqTimes + ", swatTimes=" + swatTimes
				+ ", specialTimes=" + specialTimes + ", ordinaryTimes=" + ordinaryTimes + ", xzTimes=" + xzTimes
				+ ", luck=" + luck + ", timeLeft=" + timeLeft + "]";
	}
    
    
}
