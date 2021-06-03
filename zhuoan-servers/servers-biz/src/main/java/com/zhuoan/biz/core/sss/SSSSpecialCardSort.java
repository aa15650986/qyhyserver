package com.zhuoan.biz.core.sss;

import com.zhuoan.biz.model.sss.SSSGameRoomNew;
import com.zhuoan.service.impl.SSSServiceImpl;
import com.zhuoan.util.LogUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class SSSSpecialCardSort {
    public SSSSpecialCardSort() {
    }

    public static String[] CardSort(String[] car, int carType, SSSGameRoomNew room) {
        switch(carType) {
            case 0:
                return SSSOrdinaryCards.sort(car, room);
            case 1:
                return threeFlower(car, room);
            case 2:
                return threeFlush(car, room);
            case 3:
                return sixPairs(car, room);
            case 4:
                return fiveThree(car, room);
            case 5:
                return fourThree(car, room);
            case 6:
                return twoGourd(car, room);
            case 7:
                return oneColor(car);
            case 8:
                return allSmall(car);
            case 9:
                return allBig(car);
            case 10:
                return threeBomb(car, room);
            case 11:
                return threeFlushByFlower(car, room);
            case 12:
                return twelfth(car);
            case 13:
                return thirteen(car);
            case 14:
                return sameThirteen(car);
            case 15:
                return eightXian(car);
            case 16:
                return sevenStars(car);
            case 17:
                return sixDaSun(car);
            case 18:
                return threeEmFiveSo(car);
            default:
                return SSSOrdinaryCards.sort(car, room);
        }
    }

    private static String[] threeEmFiveSo(String[] car) {
        ArrayList<String> player = new ArrayList();
        String[] var2 = car;
        int var3 = car.length;

        int num;
        for(num = 0; num < var3; ++num) {
            String string = var2[num];
            player.add(string);
        }

        ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
        String[] cars = new String[13];
        num = 0;
        Iterator var11 = set.iterator();

        while(var11.hasNext()) {
            ArrayList<Integer> list = (ArrayList)var11.next();
            ++num;
            if (list.size() == 3) {
                cars[0] = list.get(0) + "-" + num;
                cars[1] = list.get(1) + "-" + num;
                cars[2] = list.get(2) + "-" + num;
            } else if (list.size() == 5) {
                if (cars[3] == null) {
                    cars[3] = list.get(0) + "-" + num;
                    cars[4] = list.get(1) + "-" + num;
                    cars[5] = list.get(2) + "-" + num;
                    cars[6] = list.get(3) + "-" + num;
                    cars[7] = list.get(4) + "-" + num;
                } else {
                    cars[8] = list.get(0) + "-" + num;
                    cars[9] = list.get(1) + "-" + num;
                    cars[10] = list.get(2) + "-" + num;
                    cars[11] = list.get(3) + "-" + num;
                    cars[12] = list.get(4) + "-" + num;
                }
            }
        }

        String[] var12 = cars;
        int var13 = cars.length;

        for(int var7 = 0; var7 < var13; ++var7) {
            String pai = var12[var7];
            if (pai == null) {
                LogUtil.print("三皇五帝排牌出错：" + player);
                break;
            }
        }

        return cars;
    }

    private static String[] sixDaSun(String[] car) {
        ArrayList<String> player = new ArrayList();
        String[] var2 = car;
        int var3 = car.length;

        int num;
        for(num = 0; num < var3; ++num) {
            String string = var2[num];
            player.add(string);
        }

        ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
        String[] cars = new String[13];
        num = 0;
        int i = 6;
        Iterator var6 = set.iterator();

        while(true) {
            int j;
            while(var6.hasNext()) {
                ArrayList<Integer> list = (ArrayList)var6.next();
                ++num;
                if (list.size() >= 6) {
                    cars[0] = list.get(0) + "-" + num;
                    cars[1] = list.get(1) + "-" + num;
                    cars[2] = list.get(2) + "-" + num;
                    cars[3] = list.get(3) + "-" + num;
                    cars[4] = list.get(4) + "-" + num;
                    cars[5] = list.get(5) + "-" + num;
                } else if (list.size() != 0) {
                    for(j = 0; j < list.size(); ++j) {
                        cars[i] = list.get(j) + "-" + num;
                        ++i;
                    }
                }
            }

            String[] var13 = cars;
            int var14 = cars.length;

            for(j = 0; j < var14; ++j) {
                String pai = var13[j];
                if (pai == null) {
                    LogUtil.print("六六大顺排牌出错：" + player);
                    break;
                }
            }

            return cars;
        }
    }

    private static String[] sevenStars(String[] car) {
        ArrayList<String> player = new ArrayList();
        String[] var2 = car;
        int var3 = car.length;

        int num;
        for(num = 0; num < var3; ++num) {
            String string = var2[num];
            player.add(string);
        }

        ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
        String[] cars = new String[13];
        num = 0;
        int i = 7;
        Iterator var6 = set.iterator();

        while(true) {
            int j;
            while(var6.hasNext()) {
                ArrayList<Integer> list = (ArrayList)var6.next();
                ++num;
                if (list.size() >= 7) {
                    cars[0] = list.get(0) + "-" + num;
                    cars[1] = list.get(1) + "-" + num;
                    cars[2] = list.get(2) + "-" + num;
                    cars[3] = list.get(3) + "-" + num;
                    cars[4] = list.get(4) + "-" + num;
                    cars[5] = list.get(5) + "-" + num;
                    cars[6] = list.get(6) + "-" + num;
                } else if (list.size() != 0) {
                    for(j = 0; j < list.size(); ++j) {
                        cars[i] = list.get(j) + "-" + num;
                        ++i;
                    }
                }
            }

            String[] var13 = cars;
            int var14 = cars.length;

            for(j = 0; j < var14; ++j) {
                String pai = var13[j];
                if (pai == null) {
                    LogUtil.print("七星连珠排牌出错：" + player);
                    break;
                }
            }

            return cars;
        }
    }

    private static String[] eightXian(String[] car) {
        ArrayList<String> player = new ArrayList();
        String[] var2 = car;
        int var3 = car.length;

        int num;
        for(num = 0; num < var3; ++num) {
            String string = var2[num];
            player.add(string);
        }

        ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
        String[] cars = new String[13];
        num = 0;
        int i = 8;
        Iterator var6 = set.iterator();

        while(true) {
            int j;
            while(var6.hasNext()) {
                ArrayList<Integer> list = (ArrayList)var6.next();
                ++num;
                if (list.size() >= 8) {
                    cars[0] = list.get(0) + "-" + num;
                    cars[1] = list.get(1) + "-" + num;
                    cars[2] = list.get(2) + "-" + num;
                    cars[3] = list.get(3) + "-" + num;
                    cars[4] = list.get(4) + "-" + num;
                    cars[5] = list.get(5) + "-" + num;
                    cars[6] = list.get(6) + "-" + num;
                    cars[7] = list.get(7) + "-" + num;
                } else if (list.size() != 0) {
                    for(j = 0; j < list.size(); ++j) {
                        cars[i] = list.get(j) + "-" + num;
                        ++i;
                    }
                }
            }

            String[] var13 = cars;
            int var14 = cars.length;

            for(j = 0; j < var14; ++j) {
                String pai = var13[j];
                if (pai == null) {
                    LogUtil.print("八仙过海排牌出错：" + player);
                    break;
                }
            }

            return cars;
        }
    }

    public static String[] sameThirteen(String[] car) {
        for(int i = 0; i < car.length - 1; ++i) {
            for(int j = i + 1; j < car.length; ++j) {
                if (SSSGameRoomNew.getValue(car[i]) > SSSGameRoomNew.getValue(car[j])) {
                    String o = car[i];
                    String o1 = car[j];
                    car[i] = o1;
                    car[j] = o;
                }
            }
        }

        return car;
    }

    public static String[] thirteen(String[] car) {
        for(int i = 0; i < car.length - 1; ++i) {
            for(int j = i + 1; j < car.length; ++j) {
                if (SSSGameRoomNew.getValue(car[i]) > SSSGameRoomNew.getValue(car[j])) {
                    String o = car[i];
                    String o1 = car[j];
                    car[i] = o1;
                    car[j] = o;
                }
            }
        }

        return car;
    }

    public static String[] twelfth(String[] car) {
        for(int i = 0; i < car.length - 1; ++i) {
            for(int j = i + 1; j < car.length; ++j) {
                if (SSSGameRoomNew.getValue(car[i]) > SSSGameRoomNew.getValue(car[j])) {
                    String o = car[i];
                    String o1 = car[j];
                    car[i] = o1;
                    car[j] = o;
                }
            }
        }

        return car;
    }

    public static String[] threeFlushByFlower(String[] car, SSSGameRoomNew room) {
        return SSSOrdinaryCards.sort(car, room);
    }

    public static String[] threeBomb(String[] car, SSSGameRoomNew room) {
        return SSSOrdinaryCards.sort(car, room);
    }

    public static String[] allBig(String[] car) {
        for(int i = 0; i < car.length - 1; ++i) {
            for(int j = i + 1; j < car.length; ++j) {
                if (SSSGameRoomNew.getValue(car[i]) > SSSGameRoomNew.getValue(car[j])) {
                    String o = car[i];
                    String o1 = car[j];
                    car[i] = o1;
                    car[j] = o;
                }
            }
        }

        return car;
    }

    public static String[] allSmall(String[] car) {
        for(int i = 0; i < car.length - 1; ++i) {
            for(int j = i + 1; j < car.length; ++j) {
                if (SSSGameRoomNew.getValue(car[i]) > SSSGameRoomNew.getValue(car[j])) {
                    String o = car[i];
                    String o1 = car[j];
                    car[i] = o1;
                    car[j] = o;
                }
            }
        }

        return car;
    }

    public static String[] oneColor(String[] car) {
        for(int i = 0; i < car.length - 1; ++i) {
            for(int j = i + 1; j < car.length; ++j) {
                if (SSSGameRoomNew.getValue(car[i]) > SSSGameRoomNew.getValue(car[j])) {
                    String o = car[i];
                    String o1 = car[j];
                    car[i] = o1;
                    car[j] = o;
                }
            }
        }

        return car;
    }

    public static String[] twoGourd(String[] car, SSSGameRoomNew roomNew) {
        return SSSOrdinaryCards.sort(car, roomNew);
    }

    public static String[] fourThree(String[] car, SSSGameRoomNew roomNew) {
        return SSSOrdinaryCards.sort(car, roomNew);
    }

    public static String[] fiveThree(String[] car, SSSGameRoomNew roomNew) {
        return SSSOrdinaryCards.sort(car, roomNew);
    }

    public static String[] sixPairs(String[] car, SSSGameRoomNew roomNew) {
        ArrayList<String> player = new ArrayList();
        String[] var3 = car;
        int var4 = car.length;

        int num;
        for(num = 0; num < var4; ++num) {
            String string = var3[num];
            player.add(string);
        }

        ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
        String[] cars = new String[13];
        num = 0;
        int c = 0;
        Iterator var7 = set.iterator();

        while(true) {
            ArrayList list;
            int i;
            label73:
            do {
                while(var7.hasNext()) {
                    list = (ArrayList)var7.next();
                    ++num;
                    if (list.size() != 1 && list.size() != 3 && list.size() != 5 && list.size() != 9) {
                        continue label73;
                    }

                    for(i = 0; i < list.size(); ++i) {
                        if (cars[c] == null) {
                            cars[c] = list.get(i) + "-" + num;
                        }

                        ++c;
                    }
                }

                Boolean is = false;
                String[] var16 = cars;
                i = cars.length;

                for(int var10 = 0; var10 < i; ++var10) {
                    String pai = var16[var10];
                    if (pai == null) {
                        is = true;
                        LogUtil.print("六对半排牌出错：" + player);
                        break;
                    }
                }

                if (is) {
                    return SSSOrdinaryCards.sort(car, roomNew);
                }

                return cars;
            } while(list.size() != 2 && list.size() != 4 && list.size() != 6 && list.size() != 8);

            for(i = 0; i < list.size(); ++i) {
                if (cars[c] == null) {
                    cars[c] = list.get(i) + "-" + num;
                }

                ++c;
            }
        }
    }

    public static String[] threeFlush(String[] car, SSSGameRoomNew roomNew) {
        ArrayList<String> player = new ArrayList();
        String[] cars = car;
        int var4 = car.length;

        int i;
        for(i = 0; i < var4; ++i) {
            String string = cars[i];
            player.add(string);
        }

        cars = new String[13];
        ArrayList<ArrayList<String>> flu = SSSOrdinaryCards.flush(player);

        int j;
        for(i = 0; i < flu.size(); ++i) {
            ArrayList<String> five1 = (ArrayList)flu.get(i);

            for(j = 1; j < flu.size(); ++j) {
                ArrayList<String> player1 = new ArrayList(player);
                ArrayList<String> five2 = (ArrayList)flu.get(j);
                if (five1 != five2) {
                    int one;
                    for(one = 0; one < five1.size(); ++one) {
                        player1.remove(five1.get(one));
                        player1.remove(five2.get(one));
                    }

                    if (player1.size() == 3) {
                        one = SSSGameRoomNew.getValue((String)player1.get(0));
                        int two = SSSGameRoomNew.getValue((String)player1.get(1));
                        int three = SSSGameRoomNew.getValue((String)player1.get(2));
                        ArrayList<Integer> ps = new ArrayList();
                        ps.add(one);
                        ps.add(two);
                        ps.add(three);
                        Collections.sort(ps);
                        if ((Integer)ps.get(0) + 1 == (Integer)ps.get(1) && (Integer)ps.get(1) + 1 == (Integer)ps.get(2) && (Integer)ps.get(2) - 2 == (Integer)ps.get(0)) {
                            if (SSSGameRoomNew.getValue((String)five1.get(4)) > SSSGameRoomNew.getValue((String)five2.get(4))) {
                                if (SSSGameRoomNew.getValue((String)five1.get(0)) != 1 && SSSGameRoomNew.getValue((String)five2.get(0)) != 1 || SSSGameRoomNew.getValue((String)five1.get(0)) == 1 && SSSGameRoomNew.getValue((String)five2.get(0)) != 1) {
                                    cars[12] = (String)five1.get(4);
                                    cars[11] = (String)five1.get(3);
                                    cars[10] = (String)five1.get(2);
                                    cars[9] = (String)five1.get(1);
                                    cars[8] = (String)five1.get(0);
                                    cars[7] = (String)five2.get(4);
                                    cars[6] = (String)five2.get(3);
                                    cars[5] = (String)five2.get(2);
                                    cars[4] = (String)five2.get(1);
                                    cars[3] = (String)five2.get(0);
                                    cars[2] = (String)player1.get(2);
                                    cars[1] = (String)player1.get(1);
                                    cars[0] = (String)player1.get(0);
                                } else if (SSSGameRoomNew.getValue((String)five1.get(0)) != 1 && SSSGameRoomNew.getValue((String)five2.get(0)) == 1) {
                                    cars[12] = (String)five2.get(4);
                                    cars[11] = (String)five2.get(3);
                                    cars[10] = (String)five2.get(2);
                                    cars[9] = (String)five2.get(1);
                                    cars[8] = (String)five2.get(0);
                                    cars[7] = (String)five1.get(4);
                                    cars[6] = (String)five1.get(3);
                                    cars[5] = (String)five1.get(2);
                                    cars[4] = (String)five1.get(1);
                                    cars[3] = (String)five1.get(0);
                                    cars[2] = (String)player1.get(2);
                                    cars[1] = (String)player1.get(1);
                                    cars[0] = (String)player1.get(0);
                                }
                            } else if ((SSSGameRoomNew.getValue((String)five1.get(0)) == 1 || SSSGameRoomNew.getValue((String)five2.get(0)) == 1) && (SSSGameRoomNew.getValue((String)five1.get(0)) != 1 || SSSGameRoomNew.getValue((String)five2.get(0)) == 1)) {
                                if (SSSGameRoomNew.getValue((String)five1.get(0)) != 1 && SSSGameRoomNew.getValue((String)five2.get(0)) == 1) {
                                    cars[12] = (String)five1.get(4);
                                    cars[11] = (String)five1.get(3);
                                    cars[10] = (String)five1.get(2);
                                    cars[9] = (String)five1.get(1);
                                    cars[8] = (String)five1.get(0);
                                    cars[7] = (String)five2.get(4);
                                    cars[6] = (String)five2.get(3);
                                    cars[5] = (String)five2.get(2);
                                    cars[4] = (String)five2.get(1);
                                    cars[3] = (String)five2.get(0);
                                    cars[2] = (String)player1.get(2);
                                    cars[1] = (String)player1.get(1);
                                    cars[0] = (String)player1.get(0);
                                }
                            } else {
                                cars[12] = (String)five2.get(4);
                                cars[11] = (String)five2.get(3);
                                cars[10] = (String)five2.get(2);
                                cars[9] = (String)five2.get(1);
                                cars[8] = (String)five2.get(0);
                                cars[7] = (String)five1.get(4);
                                cars[6] = (String)five1.get(3);
                                cars[5] = (String)five1.get(2);
                                cars[4] = (String)five1.get(1);
                                cars[3] = (String)five1.get(0);
                                cars[2] = (String)player1.get(2);
                                cars[1] = (String)player1.get(1);
                                cars[0] = (String)player1.get(0);
                            }
                        } else if ((Integer)ps.get(0) == 1 && (Integer)ps.get(1) == 12 && (Integer)ps.get(2) == 13) {
                            if ((SSSGameRoomNew.getValue((String)five1.get(0)) == 1 || SSSGameRoomNew.getValue((String)five2.get(0)) == 1) && (SSSGameRoomNew.getValue((String)five1.get(0)) != 1 || SSSGameRoomNew.getValue((String)five2.get(0)) == 1)) {
                                if (SSSGameRoomNew.getValue((String)five1.get(0)) != 1 && SSSGameRoomNew.getValue((String)five2.get(0)) == 1) {
                                    cars[12] = (String)five2.get(4);
                                    cars[11] = (String)five2.get(3);
                                    cars[10] = (String)five2.get(2);
                                    cars[9] = (String)five2.get(1);
                                    cars[8] = (String)five2.get(0);
                                    cars[7] = (String)five1.get(4);
                                    cars[6] = (String)five1.get(3);
                                    cars[5] = (String)five1.get(2);
                                    cars[4] = (String)five1.get(1);
                                    cars[3] = (String)five1.get(0);
                                    cars[2] = (String)player1.get(2);
                                    cars[1] = (String)player1.get(1);
                                    cars[0] = (String)player1.get(0);
                                }
                            } else {
                                cars[12] = (String)five1.get(4);
                                cars[11] = (String)five1.get(3);
                                cars[10] = (String)five1.get(2);
                                cars[9] = (String)five1.get(1);
                                cars[8] = (String)five1.get(0);
                                cars[7] = (String)five2.get(4);
                                cars[6] = (String)five2.get(3);
                                cars[5] = (String)five2.get(2);
                                cars[4] = (String)five2.get(1);
                                cars[3] = (String)five2.get(0);
                                cars[2] = (String)player1.get(2);
                                cars[1] = (String)player1.get(1);
                                cars[0] = (String)player1.get(0);
                            }
                        }
                    }
                }
            }
        }

        Boolean is = false;
        String[] var17 = cars;
        j = cars.length;

        for(int var18 = 0; var18 < j; ++var18) {
            String pai = var17[var18];
            if (pai == null) {
                is = true;
                LogUtil.print("三顺子排牌出错：" + player);
                break;
            }
        }

        if (is) {
            return SSSOrdinaryCards.sort(car, roomNew);
        } else {
            return cars;
        }
    }

    public static String[] threeFlower(String[] car, SSSGameRoomNew roomNew) {
        ArrayList<String> player = new ArrayList();
        String[] var3 = car;
        int var4 = car.length;

        int flower;
        for(flower = 0; flower < var4; ++flower) {
            String string = var3[flower];
            player.add(string);
        }

        ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByFlower(player);
        String[] cars = new String[13];
        flower = 0;
        Iterator var13 = set.iterator();

        while(true) {
            while(var13.hasNext()) {
                ArrayList<Integer> list = (ArrayList)var13.next();
                ++flower;
                if (list.size() == 3) {
                    cars[0] = flower + "-" + list.get(0);
                    cars[1] = flower + "-" + list.get(1);
                    cars[2] = flower + "-" + list.get(2);
                } else if (list.size() == 5) {
                    if (cars[3] == null) {
                        cars[3] = flower + "-" + list.get(0);
                        cars[4] = flower + "-" + list.get(1);
                        cars[5] = flower + "-" + list.get(2);
                        cars[6] = flower + "-" + list.get(3);
                        cars[7] = flower + "-" + list.get(4);
                    } else {
                        cars[8] = flower + "-" + list.get(0);
                        cars[9] = flower + "-" + list.get(1);
                        cars[10] = flower + "-" + list.get(2);
                        cars[11] = flower + "-" + list.get(3);
                        cars[12] = flower + "-" + list.get(4);
                    }
                } else if (list.size() == 8) {
                    if (cars[3] == null && cars[0] == null) {
                        cars[0] = flower + "-" + list.get(0);
                        cars[1] = flower + "-" + list.get(1);
                        cars[2] = flower + "-" + list.get(2);
                        cars[3] = flower + "-" + list.get(3);
                        cars[4] = flower + "-" + list.get(4);
                        cars[5] = flower + "-" + list.get(5);
                        cars[6] = flower + "-" + list.get(6);
                        cars[7] = flower + "-" + list.get(7);
                    } else if (cars[0] == null && cars[8] == null) {
                        cars[0] = flower + "-" + list.get(0);
                        cars[1] = flower + "-" + list.get(1);
                        cars[2] = flower + "-" + list.get(2);
                        cars[8] = flower + "-" + list.get(3);
                        cars[9] = flower + "-" + list.get(4);
                        cars[10] = flower + "-" + list.get(5);
                        cars[11] = flower + "-" + list.get(6);
                        cars[12] = flower + "-" + list.get(7);
                    }
                } else if (list.size() == 10) {
                    cars[3] = flower + "-" + list.get(0);
                    cars[4] = flower + "-" + list.get(1);
                    cars[5] = flower + "-" + list.get(2);
                    cars[6] = flower + "-" + list.get(3);
                    cars[7] = flower + "-" + list.get(4);
                    cars[8] = flower + "-" + list.get(5);
                    cars[9] = flower + "-" + list.get(6);
                    cars[10] = flower + "-" + list.get(7);
                    cars[11] = flower + "-" + list.get(8);
                    cars[12] = flower + "-" + list.get(9);
                } else if (list.size() == 13) {
                    cars[0] = flower + "-" + list.get(0);
                    cars[1] = flower + "-" + list.get(1);
                    cars[2] = flower + "-" + list.get(2);
                    cars[3] = flower + "-" + list.get(3);
                    cars[4] = flower + "-" + list.get(4);
                    cars[5] = flower + "-" + list.get(5);
                    cars[6] = flower + "-" + list.get(6);
                    cars[7] = flower + "-" + list.get(7);
                    cars[8] = flower + "-" + list.get(8);
                    cars[9] = flower + "-" + list.get(9);
                    cars[10] = flower + "-" + list.get(10);
                    cars[11] = flower + "-" + list.get(11);
                    cars[12] = flower + "-" + list.get(12);
                }
            }

            Boolean is = false;
            String[] var15 = cars;
            int var8 = cars.length;

            for(int var9 = 0; var9 < var8; ++var9) {
                String pai = var15[var9];
                if (pai == null) {
                    is = true;
                    LogUtil.print("三同花排牌出错：" + player);
                    break;
                }
            }

            if (is) {
                return SSSOrdinaryCards.sort(car, roomNew);
            }

            return cars;
        }
    }
}
