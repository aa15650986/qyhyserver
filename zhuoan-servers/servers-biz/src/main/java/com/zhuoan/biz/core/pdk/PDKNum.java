package com.zhuoan.biz.core.pdk;

public enum PDKNum {
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
    ;

    private int num;

    public int getNum() {
        return num;
    }

    PDKNum(int i) {
        this.num = i;
    }

    public static PDKNum getNum(int n) {
        for (PDKNum num : values()) {
            if (n == num.getNum()) {
                return num;
            }
        }
        return null;
    }
}
