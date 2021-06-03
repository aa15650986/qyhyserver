
package com.zhuoan.biz.model.gdy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class GDYUserPacket {
    private List<GDYPacker> myCard;
    private int cardNum;
    private int status = 0;
    private int timeLeft;
    private double score;
    private int winNum = 0;
    private int zhuangNum = 0;
    private boolean isTrustee;
    private int cancelTrusteeTime;
    private int multiple;
    private boolean isSpring;
    private List<Integer> bombList;

    public GDYUserPacket() {
    }

    public List<GDYPacker> getMyCard() {
        return this.myCard;
    }

    public void setMyPai(List<GDYPacker> myCard) {
        this.myCard = myCard;
    }

    public int getCardNum() {
        return this.myCard == null ? 0 : this.myCard.size();
    }

    public void setCardNum(int cardNum) {
        this.cardNum = cardNum;
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
        return (new BigDecimal(this.score)).setScale(2, 4).doubleValue();
    }

    public void setScore(double score) {
        this.score = (new BigDecimal(score)).setScale(2, 4).doubleValue();
    }

    public boolean isTrustee() {
        return this.isTrustee;
    }

    public void setTrustee(boolean trustee) {
        this.isTrustee = trustee;
    }

    public int getMultiple() {
        return this.multiple;
    }

    public void setMultiple(int multiple) {
        this.multiple = multiple;
    }

    public boolean isSpring() {
        return this.isSpring;
    }

    public void setSpring(boolean isSpring) {
        this.isSpring = isSpring;
    }

    public int getWinNum() {
        return this.winNum;
    }

    public void setWinNum(int winNum) {
        this.winNum = winNum;
    }

    public int getZhuangNum() {
        return this.zhuangNum;
    }

    public void setZhuangNum(int zhuangNum) {
        this.zhuangNum = zhuangNum;
    }

    public int getCancelTrusteeTime() {
        return this.cancelTrusteeTime;
    }

    public void setCancelTrusteeTime(int cancelTrusteeTime) {
        this.cancelTrusteeTime = cancelTrusteeTime;
    }

    public List<Integer> getBombList() {
        return this.bombList;
    }

    public void setBombList(List<Integer> bombList) {
        this.bombList = bombList;
    }

    public void addBombList(int num) {
        if (num >= 3) {
            List<Integer> list = this.bombList;
            if (list == null) {
                list = new ArrayList();
            }

            ((List)list).add(num);
            this.bombList = (List)list;
        }
    }

    public void initUserPacket() {
        this.myCard = new ArrayList();
        this.score = 0.0D;
        this.isTrustee = false;
        this.cancelTrusteeTime = 0;
        this.multiple = 1;
        this.isSpring = true;
        this.bombList = new ArrayList();
    }

    public void initMyCard(List<GDYPacker> cards) {
        Collections.sort(cards, GDYPacker.desc);
        this.myCard = cards;
    }

    public void addMyCard(GDYPacker newCard) {
        if (newCard != null) {
            this.myCard.add(newCard);
            Collections.sort(this.myCard, GDYPacker.desc);
        }

    }

    public void removeMyCard(List<GDYPacker> oldCards) {
        if (oldCards != null && oldCards.size() > 0) {
            List<GDYPacker> cards = this.myCard;

            for(int i = 0; i < oldCards.size(); ++i) {
                for(int j = 0; j < cards.size(); ++j) {
                    if (((GDYPacker)cards.get(j)).getColor().getColor() == ((GDYPacker)oldCards.get(i)).getColor().getColor() && ((GDYPacker)cards.get(j)).getNum().getNum() == ((GDYPacker)oldCards.get(i)).getNum().getNum()) {
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
        List<GDYPacker> myCard = this.myCard;
        if (myCard != null && myCard.size() != 0) {
            StringBuilder sb = new StringBuilder();
            Iterator var4 = myCard.iterator();

            while(var4.hasNext()) {
                GDYPacker packer = (GDYPacker)var4.next();
                sb.delete(0, sb.length());
                sb.append(packer.getColor().getColor()).append("-").append(packer.getNum().getNum());
                myPai.add(sb.toString());
            }

            return myPai;
        } else {
            return myPai;
        }
    }

    public List<String> getMyBMPai() {
        List<String> myPai = new ArrayList();
        int cardNum = this.getCardNum();
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
