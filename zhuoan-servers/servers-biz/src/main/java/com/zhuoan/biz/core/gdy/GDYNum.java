package com.zhuoan.biz.core.gdy;

public enum GDYNum {
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
    P_A(14),
    P_2(15),
    P_BLACK_JOKER(16),
    P_RED_JOKER(17),
    P_OTHER_CARD(18);

    private int num;

    public int getNum() {
        return this.num;
    }

    private GDYNum(int i) {
        this.num = i;
    }

    public static GDYNum getGDYNum(int n) {
        GDYNum[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            GDYNum num = var1[var3];
            if (n == num.getNum()) {
                return num;
            }
        }

        return null;
    }
}
