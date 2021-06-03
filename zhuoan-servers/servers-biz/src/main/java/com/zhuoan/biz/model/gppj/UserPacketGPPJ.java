
package com.zhuoan.biz.model.gppj;

import java.math.BigDecimal;

public class UserPacketGPPJ {
    private int status;
    private int bankerTimes;
    private int xzTimes;
    private double score;
    private int paiType;
    private String[] pai;

    public UserPacketGPPJ() {
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getBankerTimes() {
        return this.bankerTimes;
    }

    public void setBankerTimes(int bankerTimes) {
        this.bankerTimes = bankerTimes;
    }

    public int getXzTimes() {
        return this.xzTimes;
    }

    public void setXzTimes(int xzTimes) {
        this.xzTimes = xzTimes;
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

    public String[] getPai() {
        return this.pai;
    }

    public void setPai(String[] pai) {
        this.pai = pai;
    }
}
