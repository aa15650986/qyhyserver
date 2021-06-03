
package com.zhuoan.biz.core.sg;

public enum SGColor {
    JOKER(0),
    HEITAO(1),
    HONGTAO(2),
    MEIHAU(3),
    FANGKUAI(4);

    private int color;

    public int getColor() {
        return this.color;
    }

    private SGColor(int i) {
        this.color = i;
    }

    public static SGColor getGDYColor(int c) {
        SGColor[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            SGColor color = var1[var3];
            if (c == color.getColor()) {
                return color;
            }
        }

        return null;
    }
}
