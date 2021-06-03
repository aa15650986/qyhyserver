
package com.zhuoan.biz.model.pdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PDKUserPacket {
    private String account;
    private List<PDKPacker> myCard;
    private int status = 0;
    private int timeLeft = 0;
    private double score;
    private int winNum = 0;
    private boolean isTrustee;
    private int cancelTrusteeTime;
    private boolean isSpring;
    private List<List<PDKPacker>> bombList;

    public PDKUserPacket(String account) {
        this.account = account;
    }

    public String getAccount() {
        return this.account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public List<PDKPacker> getMyCard() {
        return this.myCard;
    }

    public void setMyCard(List<PDKPacker> myCard) {
        this.myCard = myCard;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getTimeLeft() {
        return this.timeLeft;
    }

    public void setTimeLeft(int timeLeft) {
        this.timeLeft = timeLeft;
    }

    public double getScore() {
        return this.score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getWinNum() {
        return this.winNum;
    }

    public void setWinNum(int winNum) {
        this.winNum = winNum;
    }

    public boolean isTrustee() {
        return this.isTrustee;
    }

    public void setTrustee(boolean trustee) {
        this.isTrustee = trustee;
    }

    public int getCancelTrusteeTime() {
        return this.cancelTrusteeTime;
    }

    public void setCancelTrusteeTime(int cancelTrusteeTime) {
        this.cancelTrusteeTime = cancelTrusteeTime;
    }

    public boolean isSpring() {
        return this.isSpring;
    }

    public void setSpring(boolean spring) {
        this.isSpring = spring;
    }

    public List<List<PDKPacker>> getBombList() {
        return this.bombList;
    }

    public void setBombList(List<List<PDKPacker>> bombList) {
        this.bombList = bombList;
    }

    public void initUserPacket() {
        this.myCard = new ArrayList();
        this.score = 0.0D;
        this.isTrustee = false;
        this.cancelTrusteeTime = 0;
        this.isSpring = true;
        this.bombList = new ArrayList();
    }

    public void initMyCard(List<PDKPacker> cards) {
        Collections.sort(cards, PDKPacker.desc);
        this.myCard = cards;
    }

    public void removeMyCard(List<PDKPacker> oldCards) {
        if (oldCards != null && oldCards.size() > 0) {
            List<PDKPacker> cards = this.myCard;

            for(int i = 0; i < oldCards.size(); ++i) {
                for(int j = 0; j < cards.size(); ++j) {
                    if (((PDKPacker)cards.get(j)).getColor().getColor() == ((PDKPacker)oldCards.get(i)).getColor().getColor() && ((PDKPacker)cards.get(j)).getNum().getNum() == ((PDKPacker)oldCards.get(i)).getNum().getNum()) {
                        cards.remove(j);
                        break;
                    }
                }
            }

            this.myCard = cards;
        }

    }

    public List<String> getMyPai() {
        List<String> myPai = new ArrayList();
        List<PDKPacker> myCard = this.myCard;
        if (null != myCard && myCard.size() != 0) {
            StringBuilder sb = new StringBuilder();
            Iterator var4 = myCard.iterator();

            while(var4.hasNext()) {
                PDKPacker card = (PDKPacker)var4.next();
                sb.delete(0, sb.length());
                sb.append(card.getColor().getColor()).append("-").append(card.getNum().getNum());
                myPai.add(sb.toString());
            }

            return myPai;
        } else {
            return myPai;
        }
    }

    public int getCardNum() {
        return this.myCard == null ? 0 : this.myCard.size();
    }

    public List<String> getMyBMPai() {
        List<String> myPai = new ArrayList();
        int cardNum = this.myCard == null ? 0 : this.myCard.size();
        if (cardNum == 0) {
            return myPai;
        } else {
            String card = "0-0";

            for(int i = 0; i < cardNum; ++i) {
                myPai.add(card);
            }

            return myPai;
        }
    }

    public void addWinNum() {
        int winNum = this.winNum;
        if (winNum < 0) {
            winNum = 0;
        }

        ++winNum;
        this.winNum = winNum;
    }
}
