package com.zhuoan.biz.core.sss;

import com.zhuoan.service.impl.SSSServiceImpl;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

class SSSKingCards {
    SSSKingCards() {
    }

    static ArrayList<ArrayList<String>> oneHasKing(ArrayList<String> player) {
        ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
        ArrayList<ArrayList<String>> tempList = new ArrayList();
        String kingPai = (String)getKingPaiList(player).get(0);

        for(int i = 0; i < set.size(); ++i) {
            if (((ArrayList)set.get(i)).size() == 1) {
                ArrayList<String> temp = new ArrayList();
                temp.add(kingPai);
                temp.add(((ArrayList)set.get(i)).get(0) + "-" + (i + 1));
                tempList.add(temp);
            }
        }

        return tempList;
    }

    static ArrayList<ArrayList<String>> threeHasKing(ArrayList<String> player) {
        List<String> kingPaiList = getKingPaiList(player);
        int kingPaiCount = kingPaiList.size();
        System.out.println("赖子数量："+kingPaiCount);
        ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
        ArrayList<ArrayList<String>> tempList = new ArrayList();
        ArrayList temp;
        int i;
        if (kingPaiCount == 2) {
            for(i = 0; i < set.size(); ++i) {
                if (((ArrayList)set.get(i)).size() == 1) {
                    temp = new ArrayList(kingPaiList);
                    temp.add(((ArrayList)set.get(i)).get(0) + "-" + (i + 1));
                    tempList.add(temp);
                }
            }
        }

        if (kingPaiCount == 1) {
        	System.out.println(set);
            for(i = 0; i < set.size(); ++i) {
                if (((ArrayList)set.get(i)).size() == 2) {
                    temp = new ArrayList(kingPaiList);

                    for(int j = 0; j < 2; ++j) {
                        temp.add(((ArrayList)set.get(i)).get(j) + "-" + (i + 1));
                    }

                    tempList.add(temp);
                }
            }
        }
System.out.println(tempList);
        return tempList;
    }

    static ArrayList<ArrayList<String>> flushHasKing(ArrayList<String> player) {
        ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
        ArrayList<ArrayList<String>> tempList = new ArrayList();
        TreeSet<Integer> allSet = new TreeSet();
        Iterator var4 = player.iterator();

        while(var4.hasNext()) {
            String list = (String)var4.next();
            allSet.add(Integer.parseInt(list.split("-")[1]));
        }

        ArrayList<Integer> allList = new ArrayList(allSet);
        return allList.size() < 5 ? tempList : null;
    }

    static ArrayList<ArrayList<String>> sameFlowerHasKing(ArrayList<String> player) {
        return null;
    }

    static ArrayList<ArrayList<String>> gourdHasKing(ArrayList<String> player) {
        List<String> kingPaiList = getKingPaiList(player);
        ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
        ArrayList<ArrayList<String>> tempList = new ArrayList();

        for(int i = 0; i < set.size(); ++i) {
            if (((ArrayList)set.get(i)).size() > 1) {
                ArrayList temp;
                int j;
                if (i == 0) {
                    for(j = set.size() - 1; j > i; --j) {
                        if (((ArrayList)set.get(j)).size() > 1) {
                            temp = new ArrayList(kingPaiList);
                            temp.add(((ArrayList)set.get(i)).get(0) + "-" + (i + 1));
                            temp.add(((ArrayList)set.get(i)).get(1) + "-" + (i + 1));
                            temp.add(((ArrayList)set.get(j)).get(0) + "-" + (j + 1));
                            temp.add(((ArrayList)set.get(j)).get(1) + "-" + (j + 1));
                            tempList.add(temp);
                        }
                    }
                } else {
                    for(j = i + 1; j < set.size(); ++j) {
                        if (((ArrayList)set.get(j)).size() > 1) {
                            temp = new ArrayList(kingPaiList);
                            temp.add(((ArrayList)set.get(i)).get(0) + "-" + (i + 1));
                            temp.add(((ArrayList)set.get(i)).get(1) + "-" + (i + 1));
                            temp.add(((ArrayList)set.get(j)).get(0) + "-" + (j + 1));
                            temp.add(((ArrayList)set.get(j)).get(1) + "-" + (j + 1));
                            tempList.add(temp);
                        }
                    }
                }
            }
        }

        return tempList;
    }

    static ArrayList<ArrayList<String>> bombHasKing(ArrayList<String> player) {
        List<String> kingPaiList = getKingPaiList(player);
        int kingCount = kingPaiList.size();
        ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
        ArrayList<ArrayList<String>> tempList = new ArrayList();

        for(int i = 0; i < set.size(); ++i) {
            ArrayList<Integer> list = (ArrayList)set.get(i);
            if (list.size() + kingCount == 4) {
                ArrayList<String> temp = new ArrayList();

                for(int j = 0; j < ((ArrayList)set.get(i)).size(); ++j) {
                    temp.add(list.get(j) + "-" + (i + 1));
                }

                temp.addAll(kingPaiList);
                tempList.add(temp);
            }
        }

        return tempList;
    }

    static ArrayList<ArrayList<String>> flushByFlowerHasKing(ArrayList<String> player) {
        ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByFlower(player);
        ArrayList<ArrayList<String>> tempList = new ArrayList();
        List<String> kingPaiList = getKingPaiList(player);
        int kingCount = kingPaiList.size();

        for(int i = 0; i < set.size(); ++i) {
            ArrayList<Integer> list = (ArrayList)set.get(i);
            if (kingCount + list.size() >= 5 && list.size() > 0) {
                int specialCount = 0;
                ArrayList<String> temp = new ArrayList();
                Iterator var9 = list.iterator();

                while(var9.hasNext()) {
                    Integer aList = (Integer)var9.next();
                    String card = i + 1 + "-" + aList;
                    if (!temp.contains(card)) {
                        if (aList == 1) {
                            temp.add(card);
                            ++specialCount;
                        } else if (aList >= 10 && aList <= 13) {
                            temp.add(card);
                            ++specialCount;
                        }
                    }
                }

                int j;
                int needKingNumTotal;
                if (specialCount + kingCount >= 5) {
                    j = 5 - specialCount;
                    if (j > 0) {
                        for(needKingNumTotal = 0; needKingNumTotal < j; ++needKingNumTotal) {
                            temp.add(kingPaiList.get(needKingNumTotal));
                        }
                    }

                    if (temp.size() == 5) {
                        tempList.add(temp);
                    }
                }

                for(j = 0; j < list.size() - 1; ++j) {
                    needKingNumTotal = 0;
                    kingCount = kingPaiList.size();
                    temp = new ArrayList();
                    temp.add(i + 1 + "-" + list.get(j));

                    int n;
                    for(n = j; n < list.size() - 1; ++n) {
                        int needKingNum = (Integer)list.get(n + 1) - (Integer)list.get(n) - 1;
                        if (!((Integer)list.get(n)).equals(list.get(n + 1))) {
                            if (kingCount < needKingNum) {
                                break;
                            }

                            needKingNumTotal += needKingNum;
                            kingCount -= needKingNum;
                            temp.add(i + 1 + "-" + list.get(n + 1));
                            if (temp.size() + needKingNumTotal == 5) {
                                break;
                            }

                            if (temp.size() + needKingNumTotal + kingCount >= 5) {
                                needKingNumTotal = 5 - temp.size();
                                break;
                            }
                        }
                    }

                    if (needKingNumTotal <= kingPaiList.size() && temp.size() + needKingNumTotal == 5) {
                        for(n = 0; n < needKingNumTotal; ++n) {
                            temp.add(kingPaiList.get(n));
                        }

                        if (temp.size() == 5) {
                            tempList.add(temp);
                        }
                    }
                }
            }
        }

        return tempList;
    }

    static ArrayList<ArrayList<String>> bothHasKing(ArrayList<String> player) {
        List<String> kingPaiList = getKingPaiList(player);
        int kingCount = kingPaiList.size();
        ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
        ArrayList<ArrayList<String>> tempList = new ArrayList();

        for(int i = 0; i < set.size(); ++i) {
            ArrayList<Integer> list = (ArrayList)set.get(i);
            ArrayList temp;
            int k;
            if (kingCount >= 5) {
                temp = new ArrayList();

                for(k = 0; k < 5; ++k) {
                    temp.add(kingPaiList.get(k));
                }

                tempList.add(temp);
                //
            } else if (list.size() >= 5) {
                temp = new ArrayList();

                for(k = 0; k < 5; ++k) {
                    temp.add(list.get(k) + "-" + (i + 1));
                }

                tempList.add(temp);
            } else if (list.size() + kingCount >= 5) {
                temp = new ArrayList();
                Iterator var8 = list.iterator();

                while(var8.hasNext()) {
                    Integer aList = (Integer)var8.next();
                    temp.add(aList + "-" + (i + 1));
                }

                for(k = 0; k < 5 - list.size(); ++k) {
                    temp.add(kingPaiList.get(k));
                }

                tempList.add(temp);
            }
        }

        return tempList;
    }

    private static List<String> getKingPaiList(List<String> player) {
        List<String> kingPaiList = new ArrayList();
        Iterator var2 = player.iterator();

        while(var2.hasNext()) {
            String s = (String)var2.next();
            if ("5".equals(s.split("-")[0])) {
                kingPaiList.add(s);
            }
        }

        return kingPaiList;
    }
}
