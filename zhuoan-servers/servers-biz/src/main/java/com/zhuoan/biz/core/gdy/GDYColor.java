package com.zhuoan.biz.core.gdy;

public enum GDYColor {
    HEITAO(4),
    HONGTAO(3),
    MEIHAU(2),
    FANGKUAI(1),
    JOKER(0);

    private int color;

    public int getColor() {
        return this.color;
    }

    private GDYColor(int i) {
        this.color = i;
    }

    public static GDYColor getGDYColor(int c) {
        GDYColor[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            GDYColor color = var1[var3];
            if (c == color.getColor()) {
                return color;
            }
        }

        return null;
    }
}
