
package com.zhuoan.biz.model.pdk;

import java.util.List;

public class PDKGameFlow {
    private int index;
    List<String> myCard;
    private int cardType;
    private String account;
    private String name;
    private String headimg;

    public PDKGameFlow() {
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccount() {
        return this.account;
    }

    public String getHeadimg() {
        return this.headimg;
    }

    public void setHeadimg(String headimg) {
        this.headimg = headimg;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public List<String> getMyCard() {
        return this.myCard;
    }

    public void setMyCard(List<String> myCard) {
        this.myCard = myCard;
    }

    public int getCardType() {
        return this.cardType;
    }

    public void setCardType(int cardType) {
        this.cardType = cardType;
    }
}
