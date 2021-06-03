
package com.zhuoan.biz.model.gzmj;

import java.util.ArrayList;
import java.util.List;

public class UserPacketGzMj {
    private int status = 0;
    private double score = 0.0D;
    private double gangScore = 0.0D;
    private double chookScore = 0.0D;
    private double huScore = 0.0D;
    private int winMode;
    private int huType;
    private int huTime = 0;
    private int dpTime = 0;
    private int multipleTime = 0;
    private double maxScore = 0.0D;
    private int betTime = -1;
    private int userLack;
    private List<Integer> handCardList = new ArrayList();
    private List<Integer> outCardList = new ArrayList();
    private List<GzMjDeskCard> deskCardList = new ArrayList();
    private List<Integer> chookCardList = new ArrayList();
    private List<GzMjSummaryDetail> detailList = new ArrayList();
    private List<Integer> actionList = new ArrayList();

    public UserPacketGzMj() {
    }

    public int getWinMode() {
        return this.winMode;
    }

    public void setWinMode(int winMode) {
        this.winMode = winMode;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public double getScore() {
        return this.score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getGangScore() {
        return this.gangScore;
    }

    public void setGangScore(double gangScore) {
        this.gangScore = gangScore;
    }

    public double getChookScore() {
        return this.chookScore;
    }

    public void setChookScore(double chookScore) {
        this.chookScore = chookScore;
    }

    public double getHuScore() {
        return this.huScore;
    }

    public void setHuScore(double huScore) {
        this.huScore = huScore;
    }

    public int getHuType() {
        return this.huType;
    }

    public void setHuType(int huType) {
        this.huType = huType;
    }

    public int getHuTime() {
        return this.huTime;
    }

    public void setHuTime(int huTime) {
        this.huTime = huTime;
    }

    public int getDpTime() {
        return this.dpTime;
    }

    public void setDpTime(int dpTime) {
        this.dpTime = dpTime;
    }

    public int getMultipleTime() {
        return this.multipleTime;
    }

    public void setMultipleTime(int multipleTime) {
        this.multipleTime = multipleTime;
    }

    public double getMaxScore() {
        return this.maxScore;
    }

    public void setMaxScore(double maxScore) {
        this.maxScore = maxScore;
    }

    public List<Integer> getHandCardList() {
        return this.handCardList;
    }

    public void setHandCardList(List<Integer> handCardList) {
        this.handCardList = handCardList;
    }

    public List<Integer> getOutCardList() {
        return this.outCardList;
    }

    public void setOutCardList(List<Integer> outCardList) {
        this.outCardList = outCardList;
    }

    public List<Integer> getChookCardList() {
        return this.chookCardList;
    }

    public void setChookCardList(List<Integer> chookCardList) {
        this.chookCardList = chookCardList;
    }

    public List<GzMjSummaryDetail> getDetailList() {
        return this.detailList;
    }

    public void setDetailList(List<GzMjSummaryDetail> detailList) {
        this.detailList = detailList;
    }

    public int getBetTime() {
        return this.betTime;
    }

    public void setBetTime(int betTime) {
        this.betTime = betTime;
    }

    public int getUserLack() {
        return this.userLack;
    }

    public void setUserLack(int userLack) {
        this.userLack = userLack;
    }

    public List<GzMjDeskCard> getDeskCardList() {
        return this.deskCardList;
    }

    public void setDeskCardList(List<GzMjDeskCard> deskCardList) {
        this.deskCardList = deskCardList;
    }

    public List<Integer> getActionList() {
        return this.actionList;
    }

    public void setActionList(List<Integer> actionList) {
        this.actionList = actionList;
    }
}
