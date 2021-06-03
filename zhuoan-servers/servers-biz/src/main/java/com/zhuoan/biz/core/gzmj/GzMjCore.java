
package com.zhuoan.biz.core.gzmj;

import com.zhuoan.biz.model.gzmj.GzMjDeskCard;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.sf.json.JSONObject;

public class GzMjCore {
    private static final int JIANG_SIZE = 2;
    private static final int KE_SIZE = 3;
    private static final int MAX_HU_CARD_SIZE = 14;
    private static final List<Integer> ALL_CARD = Arrays.asList(11, 12, 13, 14, 15, 16, 17, 18, 19, 21, 22, 23, 24, 25, 26, 27, 28, 29, 31, 32, 33, 34, 35, 36, 37, 38, 39);
    private static final List<Integer> TONG_CARD = Arrays.asList(11, 12, 13, 14, 15, 16, 17, 18, 19);
    private static final List<Integer> WAND_CARD = Arrays.asList(21, 22, 23, 24, 25, 26, 27, 28, 29);
    private static final List<Integer> TIAO_CARD = Arrays.asList(31, 32, 33, 34, 35, 36, 37, 38, 39);
    private static final Map<Integer, List<Integer>> CARD_MAP = new HashMap();

    public GzMjCore() {
    }

    public static int getHuType(List<Integer> handCards, List<GzMjDeskCard> deskCards, Integer newCard) {
        ArrayList<Integer> cardsTemp = new ArrayList(handCards);
        cardsTemp.add(newCard);
        if (deskCards.size() == 1 && isAllPair(cardsTemp) && handCards.contains(newCard)) {
            return 9;
        } else if (cardsTemp.size() == 14 && isAllPair(cardsTemp)) {
            boolean allColor = isAllColor(cardsTemp, deskCards);
            boolean containsFour = containsFour(cardsTemp);
            if (allColor && containsFour) {
                return 8;
            } else if (allColor) {
                return 6;
            } else {
                return containsFour ? 7 : 3;
            }
        } else {
            Map<Integer, Integer> countMap = getCountMap(cardsTemp);
            List<Integer> jiangList = new ArrayList();
            Iterator var6 = countMap.keySet().iterator();

            Integer jiang;
            while(var6.hasNext()) {
                jiang = (Integer)var6.next();
                if ((Integer)countMap.get(jiang) >= 2) {
                    jiangList.add(jiang);
                }
            }

            var6 = jiangList.iterator();

            ArrayList cardsTemp1;
            do {
                if (!var6.hasNext()) {
                    return 0;
                }

                jiang = (Integer)var6.next();
                cardsTemp1 = new ArrayList(cardsTemp);
                cardsTemp1.remove(jiang);
                cardsTemp1.remove(jiang);
            } while(!canHu(cardsTemp1));

            boolean allColor = isAllColor(cardsTemp, deskCards);
            boolean allPeng = isAllPeng(cardsTemp1);
            if (allColor && allPeng) {
                return 5;
            } else if (allColor) {
                return 4;
            } else if (allPeng) {
                return 2;
            } else {
                return 1;
            }
        }
    }

    private static boolean canHu(List<Integer> cards) {
        if (cards.size() == 0) {
            return true;
        } else {
            ArrayList<Integer> cardsTemp = new ArrayList(cards);
            Collections.sort(cards);
            Integer curCard = (Integer)cards.get(0);
            List<Integer> sameCards = new ArrayList();
            Iterator var4 = cards.iterator();

            Integer nextTwo;
            while(var4.hasNext()) {
                nextTwo = (Integer)var4.next();
                if (Objects.equals(nextTwo, curCard)) {
                    sameCards.add(nextTwo);
                }
            }

            if (sameCards.size() < 3) {
                Integer nextOne = curCard + 1;
                nextTwo = curCard + 2;
                if (cardsTemp.contains(nextOne) && cardsTemp.contains(nextTwo)) {
                    cardsTemp.remove(curCard);
                    cardsTemp.remove(nextOne);
                    cardsTemp.remove(nextTwo);
                    return canHu(cardsTemp);
                } else {
                    return false;
                }
            } else {
                for(int i = 0; i < 3; ++i) {
                    cardsTemp.remove(curCard);
                }

                return canHu(cardsTemp);
            }
        }
    }

    public static JSONObject getTingList(List<Integer> handCards, List<GzMjDeskCard> deskCards) {
        JSONObject tingMap = new JSONObject();
        Iterator var3 = handCards.iterator();

        while(var3.hasNext()) {
            Integer card = (Integer)var3.next();
            ArrayList<Integer> cardsTemp = new ArrayList(handCards);
            cardsTemp.remove(card);
            List<JSONObject> tingList = new ArrayList();
            Iterator var7 = ALL_CARD.iterator();

            while(var7.hasNext()) {
                Integer newCard = (Integer)var7.next();
                int huType = getHuType(cardsTemp, deskCards, newCard);
                if (huType != 0) {
                    JSONObject object = new JSONObject();
                    object.put(newCard, huType);
                    tingList.add(object);
                }
            }

            if (tingList.size() > 0) {
                tingMap.put(card, tingList);
            }
        }

        return tingMap;
    }

    private static Map<Integer, Integer> getCountMap(List<Integer> handCards) {
        Map<Integer, Integer> countMap = new HashMap();
        Iterator var2 = handCards.iterator();

        while(var2.hasNext()) {
            Integer handCard = (Integer)var2.next();
            if (countMap.containsKey(handCard)) {
                countMap.put(handCard, (Integer)countMap.get(handCard) + 1);
            } else {
                countMap.put(handCard, 1);
            }
        }

        return countMap;
    }

    private static boolean isAllPeng(List<Integer> handCards) {
        Map<Integer, Integer> countMap = getCountMap(handCards);
        List<Integer> notThreeList = new ArrayList();
        Iterator var3 = countMap.keySet().iterator();

        while(var3.hasNext()) {
            Integer card = (Integer)var3.next();
            if ((Integer)countMap.get(card) != 3) {
                notThreeList.add(card);
            }
        }

        return notThreeList.size() == 0;
    }

    private static boolean isAllPair(List<Integer> handCards) {
        Map<Integer, Integer> countMap = getCountMap(handCards);
        List<Integer> singleList = new ArrayList();
        Iterator var3 = countMap.keySet().iterator();

        while(var3.hasNext()) {
            Integer card = (Integer)var3.next();
            if ((Integer)countMap.get(card) % 2 != 0) {
                singleList.add(card);
            }
        }

        return singleList.size() == 0;
    }

    private static boolean containsFour(List<Integer> handCards) {
        Map<Integer, Integer> countMap = getCountMap(handCards);
        List<Integer> fourList = new ArrayList();
        Iterator var3 = countMap.keySet().iterator();

        while(var3.hasNext()) {
            Integer card = (Integer)var3.next();
            if ((Integer)countMap.get(card) == 4) {
                fourList.add(card);
            }
        }

        return fourList.size() == 0;
    }

    private static boolean isAllColor(List<Integer> handCards, List<GzMjDeskCard> deskCards) {
        List<Integer> allCards = new ArrayList();
        allCards.addAll(handCards);
        Iterator var3 = deskCards.iterator();

        while(var3.hasNext()) {
            GzMjDeskCard deskCard = (GzMjDeskCard)var3.next();
            allCards.addAll(deskCard.getCardList());
        }

        int color = getCardColor((Integer)allCards.get(0));
        Iterator var7 = allCards.iterator();

        Integer card;
        do {
            if (!var7.hasNext()) {
                return true;
            }

            card = (Integer)var7.next();
        } while(getCardColor(card) == color);

        return false;
    }

    public static List<Integer> getActionList(List<Integer> handCards, List<GzMjDeskCard> deskCards, int newCard, boolean isOut) {
        List<Integer> handCardsTemp = new ArrayList(handCards);
        List<Integer> actionList = new ArrayList();
        int cardCount = 0;
        Iterator var7 = handCardsTemp.iterator();

        while(var7.hasNext()) {
            Integer handCard = (Integer)var7.next();
            if (handCard == newCard) {
                ++cardCount;
            }
        }

        if (cardCount >= 2 && isOut) {
            actionList.add(1);
        }

        if (cardCount >= 3) {
            actionList.add(isOut ? 3 : 2);
        }

        if (!isOut) {
            var7 = deskCards.iterator();

            while(var7.hasNext()) {
                GzMjDeskCard deskCard = (GzMjDeskCard)var7.next();
                if (deskCard.getCardList().contains(newCard)) {
                    actionList.add(4);
                }
            }
        }

        if (getHuType(handCardsTemp, deskCards, newCard) != 0) {
            actionList.add(5);
        }

        return actionList;
    }

    public static int getNextCard(int card) {
        int color = getCardColor(card);
        List<Integer> cardList = (List)CARD_MAP.get(color);
        if (cardList != null) {
            for(int i = 0; i < cardList.size(); ++i) {
                if ((Integer)cardList.get(i) == card) {
                    return i + 1 < cardList.size() ? (Integer)cardList.get(i + 1) : (Integer)cardList.get(0);
                }
            }
        }

        return -1;
    }

    public static int getCardColor(int card) {
        return card / 10 * 10;
    }

    public static List<Integer> getAllCard(boolean hasTwo) {
        List<Integer> allCard = new ArrayList();
        allCard.addAll(WAND_CARD);
        allCard.addAll(WAND_CARD);
        allCard.addAll(WAND_CARD);
        allCard.addAll(WAND_CARD);
        allCard.addAll(TONG_CARD);
        allCard.addAll(TONG_CARD);
        allCard.addAll(TONG_CARD);
        allCard.addAll(TONG_CARD);
        if (!hasTwo) {
            allCard.addAll(TIAO_CARD);
            allCard.addAll(TIAO_CARD);
            allCard.addAll(TIAO_CARD);
            allCard.addAll(TIAO_CARD);
        }

        Collections.shuffle(allCard);
        return allCard;
    }

    public static void main(String[] args) {
        List<Integer> allCard = getAllCard(true);
        System.out.println(allCard.subList(0, 13));
        System.out.println(allCard.subList(13, 26));
        System.out.println(allCard.subList(26, allCard.size()));
    }

    static {
        CARD_MAP.put(10, TONG_CARD);
        CARD_MAP.put(20, WAND_CARD);
        CARD_MAP.put(30, TIAO_CARD);
    }
}
