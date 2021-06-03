
package com.zhuoan.biz.core.gppj;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GPPJCore {
    public static List<String> ALL_PAi = Arrays.asList("6-6-21-12", "1-1-20-2", "4-4-19-8", "1-3-18-4", "5-5-17-10", "3-3-16-6", "2-2-15-4", "6-6-21-12", "1-1-20-2", "4-4-19-8", "1-3-18-4", "5-5-17-10", "3-3-16-6", "2-2-15-4", "5-6-14-11", "4-6-13-10", "1-6-12-7", "1-5-11-6", "5-6-14-11", "4-6-13-10", "1-6-12-7", "1-5-11-6", "4-5-10-9", "3-6-9-9", "3-5-8-8", "2-6-7-8", "3-4-6-7", "2-5-5-7", "1-4-4-5", "2-3-3-5", "2-4-2-6", "1-2-1-3");

    public GPPJCore() {
    }

    public static String[] ShufflePai() {
        List<String> initPai = ALL_PAi;
        Collections.shuffle(initPai);
        String[] shufflePai = new String[initPai.size()];

        for(int i = 0; i < initPai.size(); ++i) {
            shufflePai[i] = (String)initPai.get(i);
        }

        return shufflePai;
    }

    public static int getPaiValue(String pai) {
        String[] values = pai.split("-");
        return Integer.valueOf(values[2]);
    }

    public static int[] getPaiValue(String[] pai) {
        int[] newPai = new int[pai.length];

        for(int i = 0; i < newPai.length; ++i) {
            newPai[i] = getPaiValue(pai[i]);
        }

        return newPai;
    }

    public static int getPaiNum(String pai) {
        String[] values = pai.split("-");
        return Integer.valueOf(values[3]);
    }

    public static int getPaiType(String[] myPai) {
        if (myPai.length == 2) {
            String pai1 = myPai[0];
            String pai2 = myPai[1];
            int value1 = getPaiValue(pai1);
            int value2 = getPaiValue(pai2);
            int num1 = getPaiNum(pai1);
            int num2 = getPaiNum(pai2);
            if (value1 + value2 == 3) {
                return 38;
            } else if (value1 + value2 == 42) {
                return 37;
            } else if (value1 == 20 && value2 == 20) {
                return 36;
            } else if (value1 == 19 && value2 == 19) {
                return 35;
            } else if (value1 == 18 && value2 == 18) {
                return 34;
            } else if (value1 == 17 && value2 == 17) {
                return 33;
            } else if (value1 == 16 && value2 == 16) {
                return 32;
            } else if (value1 == 15 && value2 == 15) {
                return 31;
            } else if (value1 == 14 && value2 == 14) {
                return 30;
            } else if (value1 == 13 && value2 == 13) {
                return 29;
            } else if (value1 == 12 && value2 == 12) {
                return 28;
            } else if (value1 == 11 && value2 == 11) {
                return 27;
            } else if (value1 == 10 && value2 == 9) {
                return 26;
            } else if (value1 == 8 && value2 == 7) {
                return 25;
            } else if (value1 == 6 && value2 == 5) {
                return 24;
            } else if (value1 == 4 && value2 == 3) {
                return 23;
            } else if (value1 == 21 && value2 == 10) {
                return 22;
            } else if (value1 == 21 && value2 == 9) {
                return 21;
            } else if (value1 == 20 && value2 == 10) {
                return 20;
            } else if (value1 == 20 && value2 == 9) {
                return 19;
            } else if (value1 == 21 && value2 == 19) {
                return 18;
            } else if (value1 == 21 && value2 == 8) {
                return 17;
            } else if (value1 == 21 && value2 == 7) {
                return 16;
            } else if (value1 == 20 && value2 == 19) {
                return 15;
            } else if (value1 == 20 && value2 == 8) {
                return 14;
            } else if (value1 == 20 && value2 == 7) {
                return 13;
            } else if (value1 == 21 && value2 == 5) {
                return 12;
            } else {
                return value1 == 20 && value2 == 12 ? 11 : (num1 + num2) % 10;
            }
        } else {
            return -1;
        }
    }

    public static int comparePai(String[] myPai, String[] otherPai) {
        int myPaiType = getPaiType(myPai);
        int otherPaiType = getPaiType(otherPai);
        if (myPaiType == 17 && otherPaiType == 16) {
            return 0;
        } else if (myPaiType == 16 && otherPaiType == 17) {
            return 0;
        } else if (myPaiType == 14 && otherPaiType == 13) {
            return 0;
        } else if (myPaiType == 13 && otherPaiType == 14) {
            return 0;
        } else if (myPaiType > otherPaiType) {
            return 1;
        } else if (myPaiType < otherPaiType) {
            return -1;
        } else if (myPaiType == otherPaiType && myPaiType < 11) {
            if (getMaxValue(myPai) > getMaxValue(otherPai)) {
                return 1;
            } else {
                return getMaxValue(myPai) < getMaxValue(otherPai) ? -1 : 0;
            }
        } else {
            return 0;
        }
    }

    public static int getMaxValue(String[] myPai) {
        int maxValue = 0;

        for(int i = 0; i < myPai.length; ++i) {
            if (getPaiValue(myPai[i]) > maxValue) {
                maxValue = getPaiValue(myPai[i]);
            }
        }

        return maxValue;
    }
}
