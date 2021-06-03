
package com.zhuoan.biz.core.sss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSSLaiZIForMula {
    private static final Logger log = LoggerFactory.getLogger(SSSLaiZIForMula.class);
    private static ArrayList<ArrayList<Integer>> flushList;
    private static ArrayList<ArrayList<Integer>> sameFlowerList;

    public SSSLaiZIForMula() {
    }

    public static ArrayList<String> flushBrandByLaiZi(ArrayList<String> player) {
        ArrayList<String> jokerList = new ArrayList();
        Map<Integer, String> paiMap = new HashMap();
        Set<Integer> paiSet = new HashSet();
        Iterator var6 = player.iterator();

        while(true) {
            while(var6.hasNext()) {
                String s = (String)var6.next();
                if (!"5-0".equals(s) && !"5-1".equals(s)) {
                    String[] val = s.split("-");
                    int key = Integer.valueOf(val[1]);
                    String vlave = val[0];
                    if (paiMap.get(key) != null) {
                        vlave = (String)paiMap.get(key) + "," + vlave;
                    }

                    paiMap.put(key, vlave);
                    if (key == 1) {
                        key = 14;
                    }

                    paiSet.add(key);
                } else {
                    jokerList.add(s);
                }
            }

            if (jokerList != null && jokerList.size() != 0) {
                List<Integer> paiList = new ArrayList();
                Iterator var10 = paiSet.iterator();

                while(var10.hasNext()) {
                    Integer pai = (Integer)var10.next();
                    paiList.add(pai);
                }

                Collections.sort(paiList, Collections.reverseOrder());
                ArrayList pai;
                ArrayList arrayList;
                if (paiList != null && paiList.size() > 4 && 14 == (Integer)paiList.get(0)) {
                    if (13 == (Integer)paiList.get(1) && 12 == (Integer)paiList.get(2) && 11 == (Integer)paiList.get(3) && 10 == (Integer)paiList.get(4)) {
                        arrayList = new ArrayList();
                        arrayList.add(getFlushpai("1", paiMap));
                        arrayList.add(getFlushpai("13", paiMap));
                        arrayList.add(getFlushpai("12", paiMap));
                        arrayList.add(getFlushpai("11", paiMap));
                        arrayList.add(getFlushpai("10", paiMap));
                        return arrayList;
                    }

                    if (5 == (Integer)paiList.get(paiList.size() - 4) && 4 == (Integer)paiList.get(paiList.size() - 3) && 3 == (Integer)paiList.get(paiList.size() - 2) && 2 == (Integer)paiList.get(paiList.size() - 1)) {
                        boolean b = true;
                        if (paiList.size() > 8) {
                            if (jokerList.size() == 1 && paiList.size() > 8) {
                                if ((Integer)paiList.get(3) > 9) {
                                    b = false;
                                }
                            } else if (jokerList.size() == 2 && (Integer)paiList.get(2) > 9) {
                                b = false;
                            }
                        }

                        if (b) {
                            pai = new ArrayList();
                            pai.add(getFlushpai("1", paiMap));
                            pai.add(getFlushpai("5", paiMap));
                            pai.add(getFlushpai("4", paiMap));
                            pai.add(getFlushpai("3", paiMap));
                            pai.add(getFlushpai("2", paiMap));
                            return pai;
                        }
                    }
                }

                if (jokerList.size() > 0 && paiList.size() > 3) {
                    flushList = new ArrayList();
                    arrayList = new ArrayList() {
                        {
                            this.add(0);
                            this.add(0);
                            this.add(0);
                            this.add(0);
                        }
                    };
                    flushSubmit(paiList, 0, 0, 4, arrayList);
                }

                arrayList = getShunZiMax(jokerList, paiMap);
                if (arrayList != null && arrayList.size() == 5 && !arrayList.contains("0-0")) {
                    return arrayList;
                }

                if (jokerList.size() == 2 && paiList.size() > 2) {
                    flushList = new ArrayList();
                    pai = new ArrayList() {
                        {
                            this.add(0);
                            this.add(0);
                            this.add(0);
                        }
                    };
                    flushSubmit(paiList, 0, 0, 3, pai);
                }

                arrayList = getShunZiMax(jokerList, paiMap);
                if (arrayList != null && arrayList.size() == 5 && !arrayList.contains("0-0")) {
                    return arrayList;
                }

                return null;
            }

            return null;
        }
    }

    private static ArrayList<String> getShunZiMax(ArrayList<String> jokerList, Map<Integer, String> paiMap) {
        if (flushList == null) {
            return null;
        } else {
            int a4 = 0;
            int sum = 0;

            for(int i = 0; i < flushList.size(); ++i) {
                ArrayList<Integer> intList = (ArrayList)flushList.get(i);
                if (intList != null && intList.size() >= 3) {
                    int a1 = (Integer)intList.get(0);
                    int a2 = (Integer)intList.get(1);
                    int a3 = (Integer)intList.get(2);
                    if (intList.size() == 4 && jokerList.size() > 0) {
                        a4 = (Integer)intList.get(3);
                        if (14 == a1 && a2 + a3 + a4 < 13) {
                            if (a2 - a3 > 2 || a3 - a4 > 2 || a4 - 1 > 2) {
                                continue;
                            }

                            sum = a2 - 1;
                        } else {
                            if (a1 - a2 > 2 || a2 - a3 > 2 || a3 - a4 > 2) {
                                continue;
                            }

                            sum = a1 - a4;
                        }
                    } else if (intList.size() == 3 && jokerList.size() == 2) {
                        a4 = 0;
                        if (14 == a1 && a2 + a3 + a4 < 13) {
                            if (a2 - a3 > 3 || a3 - 1 > 3) {
                                continue;
                            }

                            sum = a2 - 1;
                        } else {
                            if (a1 - a2 > 3 || a2 - a3 > 3) {
                                continue;
                            }

                            sum = a1 - a3;
                        }
                    }

                    if (2 == sum || 3 == sum || 4 == sum) {
                        ArrayList<String> arrayList = flushArithmetics(a1, a2, a3, a4, sum, jokerList, paiMap);
                        if (arrayList != null && arrayList.size() == 5 && !arrayList.contains("0-0")) {
                            return arrayList;
                        }
                    }
                }
            }

            return null;
        }
    }

    private static ArrayList<String> flushArithmetics(int a1, int a2, int a3, int a4, int sum, ArrayList<String> jokerList, Map<Integer, String> paiMap) {
        if (a1 != 0 && a2 != 0 && a3 != 0 && (2 == sum || 3 == sum || 4 == sum) && jokerList != null && paiMap != null) {
            ArrayList<String> arrayList = new ArrayList();
            if (0 != a4) {
                if (jokerList.size() != 1) {
                    return null;
                }

                if (3 == sum) {
                    if (14 != a1) {
                        arrayList.add(jokerList.get(0));
                    }

                    arrayList.add(String.valueOf(a1));
                    if (14 == a1 && a2 < 5) {
                        arrayList.add(jokerList.get(0));
                    }

                    arrayList.add(String.valueOf(a2));
                    arrayList.add(String.valueOf(a3));
                    arrayList.add(String.valueOf(a4));
                    if (14 == a1 && a2 > 4) {
                        arrayList.add(jokerList.get(0));
                    }
                } else if (4 == sum) {
                    arrayList.add(String.valueOf(a1));
                    arrayList.add(String.valueOf(a2));
                    arrayList.add(String.valueOf(a3));
                    arrayList.add(String.valueOf(a4));
                    int count = a2 - a3 == 2 ? 2 : (a3 - a4 == 2 ? 3 : 1);
                    if (a2 < 6) {
                        count = 4;
                    }

                    arrayList.add(count, jokerList.get(0));
                }
            } else {
                if (jokerList.size() != 2) {
                    return null;
                }

                String joker0 = jokerList.contains("5-0") ? "5-0" : (String)jokerList.get(0);
                String joker1 = jokerList.contains("5-1") ? "5-1" : (String)jokerList.get(1);
                if (2 == sum) {
                    if (13 == a1) {
                        arrayList.add(joker1);
                    }

                    if (13 > a1) {
                        arrayList.add(joker1);
                        arrayList.add(joker0);
                    }

                    arrayList.add(String.valueOf(a1));
                    arrayList.add(String.valueOf(a2));
                    arrayList.add(String.valueOf(a3));
                    if (14 == a1) {
                        arrayList.add(joker1);
                        arrayList.add(joker0);
                    }

                    if (13 == a1) {
                        arrayList.add(joker0);
                    }
                } else {
                    int count;
                    if (3 == sum) {
                        if (14 != a1) {
                            arrayList.add(joker1);
                        }

                        arrayList.add(String.valueOf(a1));
                        arrayList.add(String.valueOf(a2));
                        arrayList.add(String.valueOf(a3));
                        count = a2 - a3 == 2 ? 2 : 1;
                        arrayList.add(count, joker0);
                        if (14 == a1) {
                            arrayList.add(count, joker1);
                        }
                    } else if (4 == sum) {
                        arrayList.add(String.valueOf(a1));
                        arrayList.add(String.valueOf(a2));
                        arrayList.add(String.valueOf(a3));
                        count = a2 - a3 == 3 ? 2 : 1;
                        arrayList.add(count, joker0);
                        arrayList.add(count, joker1);
                    }
                }
            }

            if (arrayList != null && arrayList.size() == 5) {
                ArrayList<String> pai = new ArrayList();
                pai.add(getFlushpai((String)arrayList.get(0), paiMap));
                pai.add(getFlushpai((String)arrayList.get(1), paiMap));
                pai.add(getFlushpai((String)arrayList.get(2), paiMap));
                pai.add(getFlushpai((String)arrayList.get(3), paiMap));
                pai.add(getFlushpai((String)arrayList.get(4), paiMap));
                return pai;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private static String getFlushpai(String str, Map<Integer, String> paiMap) {
        if (!"5-0".equals(str) && !"5-1".equals(str)) {
            int key = Integer.valueOf(str);
            if (14 == key) {
                key = 1;
            }

            if (paiMap.get(key) != null) {
                String[] value = ((String)paiMap.get(key)).split(",");
                return value[0] + "-" + key;
            } else {
                log.info("十三水 癞子玩法 顺子配牌 key：" + key + " paiMap:" + paiMap.toString());
                return "0-0";
            }
        } else {
            return str;
        }
    }

    public static ArrayList<String> sameFlowerByLaiZi(ArrayList<String> player) {
        if (player.size() < 5) {
            return null;
        } else {
            List<Integer> heiTao = new ArrayList();
            List<Integer> hongTao = new ArrayList();
            List<Integer> meiHua = new ArrayList();
            List<Integer> fangKuai = new ArrayList();
            ArrayList<String> jokerList = new ArrayList();
            Iterator var7 = player.iterator();

            while(true) {
                while(var7.hasNext()) {
                    String s = (String)var7.next();
                    if (!"5-0".equals(s) && !"5-1".equals(s)) {
                        int num;
                        if (s.contains("1-")) {
                            num = Integer.valueOf(s.replace("1-", ""));
                            if (1 == num) {
                                num = 14;
                            }

                            heiTao.add(num);
                        } else if (s.contains("2-")) {
                            num = Integer.valueOf(s.replace("2-", ""));
                            if (1 == num) {
                                num = 14;
                            }

                            hongTao.add(num);
                        } else if (s.contains("3-")) {
                            num = Integer.valueOf(s.replace("3-", ""));
                            if (1 == num) {
                                num = 14;
                            }

                            meiHua.add(num);
                        } else if (s.contains("4-")) {
                            num = Integer.valueOf(s.replace("4-", ""));
                            if (1 == num) {
                                num = 14;
                            }

                            fangKuai.add(num);
                        }
                    } else {
                        jokerList.add(s);
                    }
                }

                ArrayList<String> arrayList = null;
                if (jokerList.size() == 1 && heiTao.size() > 3 || jokerList.size() == 2 && heiTao.size() > 2) {
                    arrayList = tonghuaCombination(heiTao, jokerList, "1-");
                }

                if (arrayList != null && arrayList.size() == 5) {
                    return arrayList;
                }

                if (jokerList.size() == 1 && hongTao.size() > 3 || jokerList.size() == 2 && hongTao.size() > 2) {
                    arrayList = tonghuaCombination(hongTao, jokerList, "2-");
                }

                if (arrayList != null && arrayList.size() == 5) {
                    return arrayList;
                }

                if (jokerList.size() == 1 && meiHua.size() > 3 || jokerList.size() == 2 && meiHua.size() > 2) {
                    arrayList = tonghuaCombination(meiHua, jokerList, "3-");
                }

                if (arrayList != null && arrayList.size() == 5) {
                    return arrayList;
                }

                if (jokerList.size() == 1 && fangKuai.size() > 3 || jokerList.size() == 2 && fangKuai.size() > 2) {
                    arrayList = tonghuaCombination(fangKuai, jokerList, "4-");
                }

                if (arrayList != null && arrayList.size() == 5) {
                    return arrayList;
                }

                return null;
            }
        }
    }

    private static ArrayList<String> tonghuaCombination(List<Integer> brandList, ArrayList<String> jokerList, String type) {
        ArrayList<String> arrayList = new ArrayList();
        Collections.sort(brandList, Collections.reverseOrder());
        int count;
        if (jokerList.size() == 1) {
            if (brandList.size() > 3) {
                if (14 == (Integer)brandList.get(0)) {
                    arrayList.add(type + 1);
                } else {
                    arrayList.add(type + brandList.get(0));
                }

                arrayList.add(type + brandList.get(1));
                arrayList.add(type + brandList.get(2));
                arrayList.add(type + brandList.get(3));
                count = 14 == (Integer)brandList.get(3) ? 4 : (14 == (Integer)brandList.get(2) ? 3 : (14 == (Integer)brandList.get(1) ? 2 : (14 == (Integer)brandList.get(0) ? 1 : 0)));
                arrayList.add(count, jokerList.get(0));
            }
        } else if (jokerList.size() == 2 && brandList.size() > 2) {
            if (14 == (Integer)brandList.get(0)) {
                arrayList.add(type + 1);
            } else {
                arrayList.add(type + brandList.get(0));
            }

            arrayList.add(type + brandList.get(1));
            arrayList.add(type + brandList.get(2));
            count = 14 == (Integer)brandList.get(2) ? 3 : (14 == (Integer)brandList.get(1) ? 2 : (14 == (Integer)brandList.get(0) ? 1 : 0));
            arrayList.add(count, jokerList.get(0));
            arrayList.add(count, jokerList.get(1));
        }

        return arrayList;
    }

    private static ArrayList<ArrayList<String>> sameFlowerMaxBrandLaiZi(List<Integer> brandList, ArrayList<String> jokerList, String type) {
        ArrayList<ArrayList<String>> arrayList = new ArrayList();
        if (1 == jokerList.size()) {
            brandList.add(14);
        } else if (2 == jokerList.size()) {
            brandList.add(14);
            brandList.add(14);
        }

        Collections.sort(brandList, Collections.reverseOrder());
        sameFlowerList = new ArrayList();
        ArrayList<Integer> d = new ArrayList() {
            {
                this.add(0);
                this.add(0);
                this.add(0);
                this.add(0);
                this.add(0);
            }
        };
        sameFlowerSubmit(brandList, 0, 0, 5, d);

        for(int i = 0; sameFlowerList != null && i < sameFlowerList.size(); ++i) {
            ArrayList<String> strList = new ArrayList();
            ArrayList<Integer> intList = (ArrayList)sameFlowerList.get(i);

            for(int j = 0; j < intList.size(); ++j) {
                if (j == 0 && 1 == jokerList.size()) {
                    strList.add(jokerList.get(j));
                } else if (j == 1 && 2 == jokerList.size()) {
                    strList.add(jokerList.get(j));
                } else {
                    strList.add(type + ((Integer)intList.get(j) == 14 ? 1 : (Integer)intList.get(j)));
                }
            }

            arrayList.add(strList);
        }

        return arrayList;
    }

    private static void sameFlowerSubmit(List<Integer> a, int c, int i, int n, ArrayList<Integer> b) {
        for(int j = c; j < a.size() - (n - 1); ++j) {
            b.set(i, a.get(j));
            if (n == 1) {
                ArrayList<Integer> d = new ArrayList();
                d.addAll(b);
                sameFlowerList.add(d);
            } else {
                --n;
                ++i;
                sameFlowerSubmit(a, j + 1, i, n, b);
                ++n;
                --i;
            }
        }

    }

    private static void flushSubmit(List<Integer> a, int c, int i, int n, ArrayList<Integer> b) {
        for(int j = c; j < a.size() - (n - 1); ++j) {
            b.set(i, a.get(j));
            if (n == 1) {
                ArrayList<Integer> d = new ArrayList();
                d.addAll(b);
                flushList.add(d);
            } else {
                --n;
                ++i;
                flushSubmit(a, j + 1, i, n, b);
                ++n;
                --i;
            }
        }

    }

    public static void main(String[] args) {
        laiZiTest(1, 13, 1000, 1);
    }

    private static void laiZiTest(int num, int paiCount, int count, int type) {
        for(int k = 0; k < count; ++k) {
            List<String> pai = new ArrayList();

            for(int i = 0; i < num; ++i) {
                String[] numbers = new String[]{"2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "1"};
                String[] colors = new String[]{"1-", "2-", "3-", "4-"};
                String[] var9 = numbers;
                int var10 = numbers.length;

                for(int var11 = 0; var11 < var10; ++var11) {
                    String number = var9[var11];
                    String[] var13 = colors;
                    int var14 = colors.length;

                    for(int var15 = 0; var15 < var14; ++var15) {
                        String color = var13[var15];
                        String poker = color.concat(number);
                        pai.add(poker);
                    }
                }

                pai.add("5-0");
                pai.add("5-1");
            }

            Collections.shuffle(pai);
            ArrayList<String> player = new ArrayList();

            for(int i = 0; i < paiCount; ++i) {
                player.add(pai.get(i));
            }

            ArrayList<String> arrayList = null;
            String log = "";
            if (1 == type) {
                arrayList = flushBrandByLaiZi(player);
                log = "癞子 顺子最大配牌：";
            } else if (2 == type) {
                arrayList = sameFlowerByLaiZi(player);
                log = "癞子 同花最大配牌：";
            }

            System.out.println("当前的牌：" + player.toString());
            System.out.println("配出的牌");
            if (arrayList != null && arrayList.size() > 0) {
                System.out.println(log + arrayList.toString());
            } else {
                System.out.println("无法配牌");
            }
        }

    }
}
