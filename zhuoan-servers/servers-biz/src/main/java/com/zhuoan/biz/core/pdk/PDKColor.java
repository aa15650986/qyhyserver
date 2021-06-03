
package com.zhuoan.biz.core.pdk;

public enum PDKColor {
    HEITAO(4),
    HONGTAO(3),
    MEIHAU(2),
    FANGKUAI(1);

    private int color;

    public int getColor() {
        return this.color;
    }

    private PDKColor(int i) {
        this.color = i;
    }

    public static PDKColor getColor(int c) {
        PDKColor[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            PDKColor color = var1[var3];
            if (c == color.getColor()) {
                return color;
            }
        }

        return null;
    }
}
