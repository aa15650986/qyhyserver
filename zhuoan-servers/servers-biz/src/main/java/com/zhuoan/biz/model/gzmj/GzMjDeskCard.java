
package com.zhuoan.biz.model.gzmj;

import java.util.ArrayList;
import java.util.List;

public class GzMjDeskCard {
    private int cardType;
    private List<Integer> cardList = new ArrayList();
    private String from;

    public GzMjDeskCard() {
    }

    public int getCardType() {
        return this.cardType;
    }

    public void setCardType(int cardType) {
        this.cardType = cardType;
    }

    public List<Integer> getCardList() {
        return this.cardList;
    }

    public void setCardList(List<Integer> cardList) {
        this.cardList = cardList;
    }

    public String getFrom() {
        return this.from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
