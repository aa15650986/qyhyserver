
package com.zhuoan.biz.model.bdx;

import java.math.BigDecimal;

public class UserPackerBDX {
    private int status;
    private double value;
    private double score;
    private int[] pai;
    private int isWin;

    public UserPackerBDX() {
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public double getValue() {
        return this.value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getScore() {
        return (new BigDecimal(this.score)).setScale(2, 4).doubleValue();
    }

    public void setScore(double score) {
        this.score = (new BigDecimal(score)).setScale(2, 4).doubleValue();
    }

    public int[] getPai() {
        return this.pai;
    }

    public void setPai(int[] pai) {
        this.pai = pai;
    }

    public int getIsWin() {
        return this.isWin;
    }

    public void setIsWin(int isWin) {
        this.isWin = isWin;
    }
}
