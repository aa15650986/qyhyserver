

package com.zhuoan.biz.core.sg;

public enum SGNum {
    P_A(1),
    P_2(2),
    P_3(3),
    P_4(4),
    P_5(5),
    P_6(6),
    P_7(7),
    P_8(8),
    P_9(9),
    P_10(10),
    P_J(11),
    P_Q(12),
    P_K(13),
    P_BLACK_JOKER(14),
    P_RED_JOKER(15);

    private int num;

    public int getNum() {
        return this.num;
    }

    private SGNum(int i) {
        this.num = i;
    }

    public static SGNum getGDYNum(int n) {
        SGNum[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            SGNum num = var1[var3];
            if (n == num.getNum()) {
                return num;
            }
        }

        return null;
    }
}
