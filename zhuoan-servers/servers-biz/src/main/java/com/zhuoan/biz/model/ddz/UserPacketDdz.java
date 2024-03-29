package com.zhuoan.biz.model.ddz;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 13:38 2018/6/27
 * @Modified By:
 **/
public class UserPacketDdz implements Serializable {
    /**
     * 游戏状态
     */
    private int status;
    /**
     * 分数
     */
    private double score;
    /**
     * 手牌
     */
    private List<String> myPai = new ArrayList<>();
    /**
     * 叫地主次数
     */
    private int callNum;
    /**
     * 胜利次数
     */
    private int winNum;
    /**
     * 地主次数
     */
    private int landlordNum;
    /**
     * 是否托管
     */
    private int isTrustee;

    /**
     * 记牌器结束时间
     */
    private String jpqEndTime;
    /**
     * 连胜次数
     */
    private int winStreakTime;
    /**
     * 加倍数
     */
    private int doubleTime;
    /**
     * 加倍卡数
     */
    private int doubleCardNum = -1;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public double getScore() {
        return new BigDecimal(score)
                .setScale(2, BigDecimal.ROUND_HALF_UP)
                .doubleValue();
    }

    public void setScore(double score) {
        this.score = new BigDecimal(score)
                .setScale(2, BigDecimal.ROUND_HALF_UP)
                .doubleValue();
    }

    public List<String> getMyPai() {
        return myPai;
    }

    public void setMyPai(List<String> myPai) {
        this.myPai = myPai;
    }

    public int getCallNum() {
        return callNum;
    }

    public void setCallNum(int callNum) {
        this.callNum = callNum;
    }

    public int getWinNum() {
        return winNum;
    }

    public void setWinNum(int winNum) {
        this.winNum = winNum;
    }

    public int getLandlordNum() {
        return landlordNum;
    }

    public void setLandlordNum(int landlordNum) {
        this.landlordNum = landlordNum;
    }

    public int getIsTrustee() {
        return isTrustee;
    }

    public void setIsTrustee(int isTrustee) {
        this.isTrustee = isTrustee;
    }

    public String getJpqEndTime() {
        return jpqEndTime;
    }

    public void setJpqEndTime(String jpqEndTime) {
        this.jpqEndTime = jpqEndTime;
    }

    public int getWinStreakTime() {
        return winStreakTime;
    }

    public void setWinStreakTime(int winStreakTime) {
        this.winStreakTime = winStreakTime;
    }

    public int getDoubleTime() {
        return doubleTime;
    }

    public void setDoubleTime(int doubleTime) {
        this.doubleTime = doubleTime;
    }

    public int getDoubleCardNum() {
        return doubleCardNum;
    }

    public void setDoubleCardNum(int doubleCardNum) {
        this.doubleCardNum = doubleCardNum;
    }
}
