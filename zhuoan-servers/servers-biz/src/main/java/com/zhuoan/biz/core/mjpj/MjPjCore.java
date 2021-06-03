
package com.zhuoan.biz.core.mjpj;

import java.util.*;

public class MjPjCore {
    private static final List<String> ALL_CARD = Arrays.asList("4-7-17-2", "4-7-17-2", "3-6-16-2", "3-6-16-2", "3-5-15-8", "3-5-15-8", "3-4-14-4", "3-4-14-4", "2-3-13-6", "2-3-13-6", "2-3-12-4", "2-3-12-4", "4-3-11-10", "4-3-11-10", "3-2-10-6", "3-2-10-6", "3-2-9-7", "3-2-9-7", "4-2-8-10", "4-2-8-10", "3-2-7-1", "3-2-7-1", "3-1-6-9", "3-1-6-9", "2-1-5-8", "2-1-5-8", "2-1-4-7", "2-1-4-7", "3-1-3-5", "3-1-3-5", "1-1-2-6", "1-1-1-3");
    private static final List<Integer> LONG_CARD_LIST = Arrays.asList(13, 12, 11);
    private static final List<Integer> SHORT_CARD_LIST = Arrays.asList(10, 9, 8, 7);
    private static final List<Integer> VARIED_CARD_LIST = Arrays.asList(6, 5, 4, 3);
    private static final int CARD_TYPE_EMPEROR = 28;
    private static final int CARD_TYPE_KING_NINE = 27;
    private static final int CARD_TYPE_DOUBLE_HEAVEN = 26;
    private static final int CARD_TYPE_HEAVEN = 11;
    private static final int CARD_TYPE_EARTH = 10;
    private static final int CARD_TYPE_0 = 0;
    private static final int CARD_VALUE_17 = 17;
    private static final int CARD_VALUE_16 = 16;
    private static final int CARD_VALUE_15 = 15;
    private static final int CARD_VALUE_6 = 6;
    private static final int CARD_VALUE_5 = 5;
    private static final int CARD_VALUE_2 = 2;
    private static final int CARD_VALUE_1 = 1;
    private static final int CARD_SIZE = 2;
    private static Map<Integer, String> cardTypeNameMap = initCardTypeName();
    public static final int COMPARE_RESULT_WIN = 1;
    public static final int COMPARE_RESULT_LOSE = -1;

    public MjPjCore() {
    }

    private static Map<Integer, String> initCardTypeName() {
        Map<Integer, String> cardTypeName = new HashMap(29);
        cardTypeName.put(0, "闲  十");
        cardTypeName.put(1, "一  点");
        cardTypeName.put(2, "二  点");
        cardTypeName.put(3, "三  点");
        cardTypeName.put(4, "四  点");
        cardTypeName.put(5, "五  点");
        cardTypeName.put(6, "六  点");
        cardTypeName.put(7, "七  点");
        cardTypeName.put(8, "八  点");
        cardTypeName.put(9, "九  点");
        cardTypeName.put(10, "地  杠");
        cardTypeName.put(11, "天  杠");
        cardTypeName.put(12, "对五筒");
        cardTypeName.put(13, "对七条");
        cardTypeName.put(14, "对八条");
        cardTypeName.put(15, "对九筒");
        cardTypeName.put(16, "对一筒");
        cardTypeName.put(17, "对红中");
        cardTypeName.put(18, "对七筒");
        cardTypeName.put(19, "对六筒");
        cardTypeName.put(20, "对北风");
        cardTypeName.put(21, "对四条");
        cardTypeName.put(22, "对六条");
        cardTypeName.put(23, "对  鹅");
        cardTypeName.put(24, "对  人");
        cardTypeName.put(25, "对  地");
        cardTypeName.put(26, "对  天");
        cardTypeName.put(27, "天王九");
        cardTypeName.put(28, "皇  帝");
        return cardTypeName;
    }

    public static String[] shuffleCard() {
        List<String> initCard = new ArrayList(ALL_CARD);
        Collections.shuffle(initCard);
        String[] shuffleCard = new String[initCard.size()];

        for (int i = 0; i < initCard.size(); ++i) {
            shuffleCard[i] = (String) initCard.get(i);
        }

        return shuffleCard;
    }

    private static List<Integer> getValueList(String[] cards) {
        List<Integer> valueList = new ArrayList();
        String[] var2 = cards;
        int var3 = cards.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            String card = var2[var4];
            valueList.add(getCardValue(card));
        }

        return valueList;
    }

    public static int getCardValue(String card) {
        return Integer.parseInt(card.split("-")[2]);
    }

    public static int[] getCardValue(String[] cards) {
        int[] cardValues = new int[cards.length];

        for (int i = 0; i < cardValues.length; ++i) {
            cardValues[i] = getCardValue(cards[i]);
        }

        return cardValues;
    }

    private static List<Integer> getNumList(String[] cards) {
        List<Integer> numList = new ArrayList();
        String[] var2 = cards;
        int var3 = cards.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            String card = var2[var4];
            numList.add(Integer.parseInt(card.split("-")[3]));
        }

        return numList;
    }

    public static int getCardType(String[] cards) {
        List<Integer> valueList = getValueList(cards);
        if (valueList.contains(1) && valueList.contains(2)) {
            return 28;
        } else if (valueList.contains(17) && valueList.contains(6)) {
            return 27;
        } else if (((Integer) valueList.get(0)).equals(valueList.get(1))) {
            return (Integer) valueList.get(0) + 9;
        } else if (valueList.contains(17) && (valueList.contains(15) || valueList.contains(5))) {
            return 11;
        } else if (!valueList.contains(16) || !valueList.contains(15) && !valueList.contains(5)) {
            List<Integer> numList = getNumList(cards);
            return ((Integer) numList.get(0) + (Integer) numList.get(1)) % 10;
        } else {
            return 10;
        }
    }

    private static int getCardTypeForCompare(String[] cards) {
        List<Integer> valueList = getValueList(cards);
        if (((Integer) valueList.get(0)).equals(valueList.get(1))) {
            if (LONG_CARD_LIST.contains(valueList.get(0))) {
                return (Integer) LONG_CARD_LIST.get(0) + 9;
            }

            if (SHORT_CARD_LIST.contains(valueList.get(0))) {
                return (Integer) SHORT_CARD_LIST.get(0) + 9;
            }
        }

        int cardType = getCardType(cards);
        return cardType == 26 ? 27 : cardType;
    }

    public static int compareCard(String[] myCard, String[] bankCard) {
        int myCardType = getCardTypeForCompare(myCard);
        int otherCardType = getCardTypeForCompare(bankCard);
        if (myCardType == otherCardType && myCardType < 10) {
            if (myCardType == 0) {
                return -1;
            }

            List<Integer> myValueList = getValueListForCompare(myCard);
            List<Integer> otherValueList = getValueListForCompare(bankCard);
            if (myValueList.size() == 2 && otherValueList.size() == 2) {
                return Integer.compare((Integer) myValueList.get(1), (Integer) otherValueList.get(1));
            }
        }

        return Integer.compare(myCardType, otherCardType);
    }

    private static List<Integer> getValueListForCompare(String[] cards) {
        List<Integer> valueList = new ArrayList();
        String[] var2 = cards;
        int var3 = cards.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            String card = var2[var4];
            int cardValue = getCardValue(card);
            if (LONG_CARD_LIST.contains(cardValue)) {
                cardValue = (Integer) LONG_CARD_LIST.get(0);
            }

            if (SHORT_CARD_LIST.contains(cardValue)) {
                cardValue = (Integer) SHORT_CARD_LIST.get(0);
            }

            if (VARIED_CARD_LIST.contains(cardValue)) {
                cardValue = (Integer) VARIED_CARD_LIST.get(0);
            }

            valueList.add(cardValue);
        }

        Collections.sort(valueList);
        return valueList;
    }

    private static List<String> getCardNameList(String[] cards) {
        List<String> nameList = new ArrayList();
        String[] var2 = cards;
        int var3 = cards.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            String card = var2[var4];
            if ("4-7-17-2".equals(card)) {
                nameList.add("白板");
            }

            if ("3-6-16-2".equals(card)) {
                nameList.add("二筒");
            }

            if ("3-5-15-8".equals(card)) {
                nameList.add("八筒");
            }

            if ("3-4-14-4".equals(card)) {
                nameList.add("四筒");
            }

            if ("2-3-13-6".equals(card)) {
                nameList.add("六条");
            }

            if ("2-3-12-4".equals(card)) {
                nameList.add("四条");
            }

            if ("4-3-11-10".equals(card)) {
                nameList.add("北风");
            }

            if ("3-2-10-6".equals(card)) {
                nameList.add("六筒");
            }

            if ("3-2-9-7".equals(card)) {
                nameList.add("七筒");
            }

            if ("4-2-8-10".equals(card)) {
                nameList.add("红中");
            }

            if ("3-2-7-1".equals(card)) {
                nameList.add("一筒");
            }

            if ("3-1-6-9".equals(card)) {
                nameList.add("九筒");
            }

            if ("2-1-5-8".equals(card)) {
                nameList.add("八条");
            }

            if ("2-1-4-7".equals(card)) {
                nameList.add("七条");
            }

            if ("3-1-3-5".equals(card)) {
                nameList.add("五筒");
            }

            if ("1-1-2-6".equals(card)) {
                nameList.add("六万");
            }

            if ("1-1-1-3".equals(card)) {
                nameList.add("三万");
            }
        }

        return nameList;
    }

    public static void main(String[] args) {
        ArrayList<String> allCard = new ArrayList(ALL_CARD);
        ArrayList<String[]> allCards = new ArrayList();
        ArrayList<String> temp0 = new ArrayList();

        int i;
        int m;
        for (i = 0; i < allCard.size(); ++i) {
            if (!temp0.contains(allCard.get(i))) {
                ArrayList<String> temp1 = new ArrayList();

                for (m = i + 1; m < allCard.size(); ++m) {
                    if (!temp1.contains(allCard.get(m))) {
                        String[] cards = new String[]{(String) allCard.get(i), (String) allCard.get(m)};
                        allCards.add(cards);
                        temp1.add(allCard.get(m));
                    }
                }

                temp0.add(allCard.get(i));
            }
        }

        for (i = 0; i < allCards.size(); ++i) {
            for (int j = i + 1; j < allCards.size(); ++j) {
                j = compareCard((String[]) allCards.get(i), (String[]) allCards.get(j));
                System.out.println("闲家手牌:" + getCardNameList((String[]) allCards.get(i)) + "(" + (String) cardTypeNameMap.get(getCardType((String[]) allCards.get(i))) + ")   与   庄家手牌:" + getCardNameList((String[]) allCards.get(j)) + "(" + (String) cardTypeNameMap.get(getCardType((String[]) allCards.get(j))) + ")   比牌结果为:" + (j == 1 ? "胜" : "负"));
            }
        }

    }
}
