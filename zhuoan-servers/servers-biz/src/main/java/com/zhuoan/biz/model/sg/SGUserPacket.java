

package com.zhuoan.biz.model.sg;

import com.zhuoan.biz.core.sg.SGColor;
import com.zhuoan.biz.core.sg.SGNum;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SGUserPacket {
    private String account;
    private List<SGCard> myCard;
    private int status = 0;
    private int timeLeft = 0;
    private double score;
    private int zhuangNum = 0;
    private int robMultiple;
    private int bet;
    private int cardType;
    private int cardCount;

    public SGUserPacket(String account) {
        this.account = account;
    }

    public List<SGCard> getMyCard() {
        return this.myCard;
    }

    public void setMyCard(List<SGCard> myCard) {
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
        return (new BigDecimal(this.score)).setScale(2, 4).doubleValue();
    }

    public void setScore(double score) {
        this.score = (new BigDecimal(score)).setScale(2, 4).doubleValue();
    }

    public int getZhuangNum() {
        return this.zhuangNum;
    }

    public void setZhuangNum(int zhuangNum) {
        this.zhuangNum = zhuangNum;
    }

    public int getRobMultiple() {
        return this.robMultiple;
    }

    public void setRobMultiple(int robMultiple) {
        this.robMultiple = robMultiple;
    }

    public int getBet() {
        return this.bet;
    }

    public void setBet(int bet) {
        this.bet = bet;
    }

    public int getCardType() {
        return this.cardType;
    }

    public void setCardType(int cardType) {
        this.cardType = cardType;
    }

    public int getCardCount() {
        return this.cardCount;
    }

    public void setCardCount(int cardCount) {
        this.cardCount = cardCount;
    }

    public String getAccount() {
        return this.account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void initUserPacket() {
        this.myCard = new ArrayList();
        this.score = 0.0D;
        this.robMultiple = 1;
        this.bet = 1;
        this.cardType = 0;
        this.cardCount = 0;
    }

    public List<String> getMyPai() {
        List<String> myPai = new ArrayList();
        List<SGCard> myCard = this.myCard;
        if (myCard != null && myCard.size() != 0) {
            StringBuilder sb = new StringBuilder();
            Iterator var4 = myCard.iterator();

            while(var4.hasNext()) {
                SGCard card = (SGCard)var4.next();
                sb.delete(0, sb.length());
                sb.append(card.getColor().getColor()).append("-").append(card.getNum().getNum());
                myPai.add(sb.toString());
            }

            return myPai;
        } else {
            return myPai;
        }
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

    public void addZhuangNum() {
        int zhuangNum = this.zhuangNum;
        if (zhuangNum < 0) {
            zhuangNum = 0;
        }

        ++zhuangNum;
        this.zhuangNum = zhuangNum;
    }

    public void initMyCard(List<SGCard> cards) {
        this.myCard = cards;
        if (this.myCard != null && this.myCard.size() == 3) {
            Map<String, Integer> map = this.getCardTypeCount(new ArrayList(this.myCard));
            this.cardType = map.get("cardType") != null ? (Integer)map.get("cardType") : 0;
            this.cardCount = map.get("cardCount") != null ? (Integer)map.get("cardCount") : 0;
        }

    }

    public void addMyCard(SGCard newCard) {
        if (newCard != null) {
            this.myCard.add(newCard);
            if (this.myCard.size() == 3) {
                Map<String, Integer> map = this.getCardTypeCount(new ArrayList(this.myCard));
                this.cardType = map.get("cardType") != null ? (Integer)map.get("cardType") : 0;
                this.cardCount = map.get("cardCount") != null ? (Integer)map.get("cardCount") : 0;
            }
        }

    }

    private Map<String, Integer> getCardTypeCount(List<SGCard> myCard) {
        Map<String, Integer> map = new HashMap();
        if (myCard != null && myCard.size() == 3) {
            int cardType = 0;
            SGCard sgCard1 = (SGCard)myCard.get(0);
            SGCard sgCard2 = (SGCard)myCard.get(1);
            SGCard sgCard3 = (SGCard)myCard.get(2);
            if (sgCard1 != null && sgCard2 != null && sgCard3 != null) {
                int jokerNum = 0;
                if (SGColor.JOKER.equals(sgCard1.getColor())) {
                    ++jokerNum;
                }

                if (SGColor.JOKER.equals(sgCard2.getColor())) {
                    ++jokerNum;
                }

                if (SGColor.JOKER.equals(sgCard3.getColor())) {
                    ++jokerNum;
                }

                if (jokerNum >= 3) {
                    return map;
                } else {
                    int c1 = sgCard1.getNum().getNum();
                    int c2 = sgCard2.getNum().getNum();
                    int c3 = sgCard3.getNum().getNum();
                    int cardCount;
                    if (jokerNum == 0) {
                        if (c1 == c2 && c2 == c3) {
                            if (SGNum.P_3.getNum() == c1) {
                                cardType = 100;
                            } else if (SGNum.P_10.getNum() < c1) {
                                cardType = 90;
                            } else if (SGNum.P_10.getNum() >= c1) {
                                cardType = 80;
                            }

                            cardCount = c1;
                        } else if (SGNum.P_10.getNum() >= c1 && SGNum.P_10.getNum() >= c2 && SGNum.P_10.getNum() >= c3) {
                            if (c1 >= SGNum.P_10.getNum()) {
                                c1 = 0;
                            }

                            if (c2 >= SGNum.P_10.getNum()) {
                                c2 = 0;
                            }

                            if (c3 >= SGNum.P_10.getNum()) {
                                c3 = 0;
                            }

                            cardCount = (c1 + c2 + c3) % 10;
                        } else {
                            cardType = 10;
                            if (SGNum.P_10.getNum() < c1 && SGNum.P_10.getNum() < c2 || SGNum.P_10.getNum() < c1 && SGNum.P_10.getNum() < c3 || SGNum.P_10.getNum() < c2 && SGNum.P_10.getNum() < c3) {
                                cardType = 20;
                            }

                            if (SGNum.P_10.getNum() < c1 && SGNum.P_10.getNum() < c2 && SGNum.P_10.getNum() < c3) {
                                cardType = 70;
                            }

                            if (70 == cardType) {
                                if (SGNum.P_K.getNum() != c1 && SGNum.P_K.getNum() != c2 && SGNum.P_K.getNum() != c3) {
                                    if (SGNum.P_Q.getNum() != c1 && SGNum.P_Q.getNum() != c2 && SGNum.P_Q.getNum() != c3) {
                                        cardCount = SGNum.P_J.getNum();
                                    } else {
                                        cardCount = SGNum.P_Q.getNum();
                                    }
                                } else {
                                    cardCount = SGNum.P_K.getNum();
                                }
                            } else {
                                if (c1 >= SGNum.P_10.getNum()) {
                                    c1 = 0;
                                }

                                if (c2 >= SGNum.P_10.getNum()) {
                                    c2 = 0;
                                }

                                if (c3 >= SGNum.P_10.getNum()) {
                                    c3 = 0;
                                }

                                cardCount = (c1 + c2 + c3) % 10;
                            }
                        }
                    } else if (jokerNum == 1) {
                        cardCount = SGNum.P_9.getNum();
                        if (c1 == c2 || c1 == c3 || c2 == c3) {
                            cardType = 80;
                            if (SGNum.P_BLACK_JOKER.getNum() > c1) {
                                cardCount = c1;
                            } else {
                                cardCount = c2;
                            }

                            if (SGNum.P_3.getNum() == cardCount) {
                                cardType = 100;
                            }

                            if (SGNum.P_10.getNum() < cardCount) {
                                cardType = 90;
                            }
                        }
                    } else {
                        cardType = 80;
                        if (SGNum.P_BLACK_JOKER.getNum() > c1) {
                            cardCount = c1;
                        } else if (SGNum.P_BLACK_JOKER.getNum() > c2) {
                            cardCount = c2;
                        } else {
                            cardCount = c3;
                        }

                        if (SGNum.P_3.getNum() == cardCount) {
                            cardType = 100;
                        }

                        if (SGNum.P_10.getNum() < cardCount) {
                            cardType = 90;
                        }
                    }

                    map.put("cardType", Integer.valueOf(cardType));
                    map.put("cardCount", cardCount);
                    return map;
                }
            } else {
                return map;
            }
        } else {
            return map;
        }
    }
}
