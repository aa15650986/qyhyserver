

package com.zhuoan.biz.model.sg;

import com.zhuoan.biz.core.sg.SGColor;
import com.zhuoan.biz.core.sg.SGNum;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SGCard {
    private SGColor color;
    private SGNum num;
    public static final Comparator<SGCard> cardDesc = new Comparator<SGCard>() {
        int num1;
        int num2;

        public int compare(SGCard o1, SGCard o2) {
            this.num1 = o1.getNum().getNum();
            if (SGNum.P_BLACK_JOKER.getNum() == this.num1 || SGNum.P_RED_JOKER.getNum() == this.num1) {
                this.num1 = 0;
            }

            this.num2 = o2.getNum().getNum();
            if (SGNum.P_BLACK_JOKER.getNum() == this.num2 || SGNum.P_RED_JOKER.getNum() == this.num2) {
                this.num2 = 0;
            }

            if (this.num1 > this.num2) {
                return -1;
            } else {
                return this.num1 == this.num2 && o1.getColor().getColor() < o2.getColor().getColor() ? -1 : 1;
            }
        }
    };
    public static final Comparator<SGCard> cardAsc = new Comparator<SGCard>() {
        int num1;
        int num2;

        public int compare(SGCard o2, SGCard o1) {
            this.num1 = o1.getNum().getNum();
            if (SGNum.P_BLACK_JOKER.getNum() == this.num1 || SGNum.P_RED_JOKER.getNum() == this.num1) {
                this.num1 = 0;
            }

            this.num2 = o2.getNum().getNum();
            if (SGNum.P_BLACK_JOKER.getNum() == this.num2 || SGNum.P_RED_JOKER.getNum() == this.num2) {
                this.num2 = 0;
            }

            if (this.num1 > this.num2) {
                return -1;
            } else {
                return this.num1 == this.num2 && o1.getColor().getColor() < o2.getColor().getColor() ? -1 : 1;
            }
        }
    };
    public static final Comparator<Integer> intDesc = new Comparator<Integer>() {
        public int compare(Integer o1, Integer o2) {
            return o1 > o2 ? -1 : 1;
        }
    };
    public static final Comparator<Integer> intAsc = new Comparator<Integer>() {
        public int compare(Integer o2, Integer o1) {
            return o1 > o2 ? -1 : 1;
        }
    };
    public static final Comparator<SGUserPacket> upDesc = new Comparator<SGUserPacket>() {
        int type1;
        int type2;
        List<SGCard> sgCard1;
        List<SGCard> sgCard2;

        public int compare(SGUserPacket o1, SGUserPacket o2) {
            this.type1 = o1.getCardType();
            if (20 == this.type1 || 10 == this.type1) {
                this.type1 = 0;
            }

            this.type2 = o2.getCardType();
            if (20 == this.type2 || 10 == this.type2) {
                this.type2 = 0;
            }

            if (this.type1 > this.type2) {
                return -1;
            } else {
                if (this.type1 == this.type2 && 0 != this.type1) {
                    if (o1.getCardCount() > o2.getCardCount()) {
                        return -1;
                    }

                    if (o1.getCardCount() == o2.getCardCount()) {
                        this.sgCard1 = o1.getMyCard();
                        this.sgCard2 = o1.getMyCard();
                        Collections.sort(this.sgCard1, SGCard.cardDesc);
                        Collections.sort(this.sgCard2, SGCard.cardDesc);
                        if (((SGCard)this.sgCard1.get(0)).getColor().getColor() > ((SGCard)this.sgCard2.get(0)).getColor().getColor()) {
                            return -1;
                        }
                    }
                } else if (this.type1 == this.type2 && 0 == this.type1) {
                    if (o1.getCardCount() > o2.getCardCount()) {
                        return -1;
                    }

                    if (o1.getCardCount() == o2.getCardCount()) {
                        if (o1.getCardType() > o2.getCardType()) {
                            return -1;
                        }

                        if (o1.getCardType() == o2.getCardType()) {
                            this.sgCard1 = o1.getMyCard();
                            this.sgCard2 = o1.getMyCard();
                            Collections.sort(this.sgCard1, SGCard.cardDesc);
                            Collections.sort(this.sgCard2, SGCard.cardDesc);
                            int num1 = ((SGCard)this.sgCard1.get(0)).num.getNum();
                            if (num1 > SGNum.P_K.getNum()) {
                                num1 = 0;
                            }

                            int num2 = ((SGCard)this.sgCard2.get(0)).num.getNum();
                            if (num2 > SGNum.P_K.getNum()) {
                                num2 = 0;
                            }

                            if (num1 > num2) {
                                return -1;
                            }

                            if (num1 == num2) {
                                num1 = ((SGCard)this.sgCard1.get(1)).num.getNum();
                                if (num1 > SGNum.P_K.getNum()) {
                                    num1 = 0;
                                }

                                num2 = ((SGCard)this.sgCard2.get(1)).num.getNum();
                                if (num2 > SGNum.P_K.getNum()) {
                                    num2 = 0;
                                }

                                if (num1 > num2) {
                                    return -1;
                                }

                                if (num1 == num2) {
                                    num1 = ((SGCard)this.sgCard1.get(2)).num.getNum();
                                    if (num1 > SGNum.P_K.getNum()) {
                                        num1 = 0;
                                    }

                                    num2 = ((SGCard)this.sgCard2.get(2)).num.getNum();
                                    if (num2 > SGNum.P_K.getNum()) {
                                        num2 = 0;
                                    }

                                    if (num1 > num2) {
                                        return -1;
                                    }
                                }
                            }
                        }
                    }
                }

                return 1;
            }
        }
    };

    public SGCard(SGColor color, SGNum num) {
        this.color = color;
        this.num = num;
    }

    public SGCard(SGCard sgCard) {
        this.color = sgCard.getColor();
        this.num = sgCard.getNum();
    }

    public SGCard() {
    }

    public SGNum getNum() {
        return this.num;
    }

    public SGColor getColor() {
        return this.color;
    }

    public static void main(String[] args) {
        List<SGUserPacket> upList = new ArrayList();
        SGUserPacket up = new SGUserPacket("1");
        List<SGCard> myCard = new ArrayList();
        myCard.add(new SGCard(SGColor.FANGKUAI, SGNum.P_3));
        myCard.add(new SGCard(SGColor.MEIHAU, SGNum.P_8));
        myCard.add(new SGCard(SGColor.HEITAO, SGNum.P_K));
        up.initMyCard(myCard);
        upList.add(up);
        up = new SGUserPacket("2");
        myCard = new ArrayList();
        myCard.add(new SGCard(SGColor.MEIHAU, SGNum.P_Q));
        myCard.add(new SGCard(SGColor.HEITAO, SGNum.P_Q));
        myCard.add(new SGCard(SGColor.MEIHAU, SGNum.P_4));
        up.initMyCard(myCard);
        upList.add(up);
        up = new SGUserPacket("3");
        myCard = new ArrayList();
        myCard.add(new SGCard(SGColor.MEIHAU, SGNum.P_3));
        myCard.add(new SGCard(SGColor.FANGKUAI, SGNum.P_10));
        myCard.add(new SGCard(SGColor.MEIHAU, SGNum.P_A));
        up.initMyCard(myCard);
        upList.add(up);
        Collections.sort(upList, upDesc);

        for(int i = 0; i < upList.size(); ++i) {
            up = (SGUserPacket)upList.get(i);
            System.out.println(up.getAccount() + "===" + up.getCardType() + "----点数" + up.getCardCount());
        }

    }
}
