
package com.zhuoan.biz.robot;

import org.springframework.stereotype.Component;

@Component
public class RobotInfo {
    private String robotAccount;
    private String playRoomNo;
    private int playGameId;
    private int actionType;
    private int delayTime;
    private int outTimes;
    private double maxOutScore;
    private double minOutScore;
    private int totalScore;
    private int maxWinScore = 100;
    private int maxLoseScore = -100;
    private String robotUUID;

    public RobotInfo() {
    }

    public String getRobotUUID() {
        return this.robotUUID;
    }

    public void setRobotUUID(String robotUUID) {
        this.robotUUID = robotUUID;
    }

    public String getRobotAccount() {
        return this.robotAccount;
    }

    public void setRobotAccount(String robotAccount) {
        this.robotAccount = robotAccount;
    }

    public String getPlayRoomNo() {
        return this.playRoomNo;
    }

    public void setPlayRoomNo(String playRoomNo) {
        this.playRoomNo = playRoomNo;
    }

    public int getPlayGameId() {
        return this.playGameId;
    }

    public void setPlayGameId(int playGameId) {
        this.playGameId = playGameId;
    }

    public int getActionType() {
        return this.actionType;
    }

    public void setActionType(int actionType) {
        this.actionType = actionType;
    }

    public int getDelayTime() {
        return this.delayTime;
    }

    public void setDelayTime(int delayTime) {
        this.delayTime = delayTime;
    }

    public int getOutTimes() {
        return this.outTimes;
    }

    public void setOutTimes(int outTimes) {
        this.outTimes = outTimes;
    }

    public double getMaxOutScore() {
        return this.maxOutScore;
    }

    public void setMaxOutScore(double maxOutScore) {
        this.maxOutScore = maxOutScore;
    }

    public double getMinOutScore() {
        return this.minOutScore;
    }

    public void setMinOutScore(double minOutScore) {
        this.minOutScore = minOutScore;
    }

    public int getTotalScore() {
        return this.totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getMaxWinScore() {
        return this.maxWinScore;
    }

    public void setMaxWinScore(int maxWinScore) {
        this.maxWinScore = maxWinScore;
    }

    public int getMaxLoseScore() {
        return this.maxLoseScore;
    }

    public void setMaxLoseScore(int maxLoseScore) {
        this.maxLoseScore = maxLoseScore;
    }

    public void subDelayTime() {
        --this.delayTime;
    }

    public void subOutTimes() {
        --this.outTimes;
    }

    public void addTotalScore(int score) {
        this.totalScore += score;
    }
}
