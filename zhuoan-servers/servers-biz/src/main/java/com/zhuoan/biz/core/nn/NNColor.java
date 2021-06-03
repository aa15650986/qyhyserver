package com.zhuoan.biz.core.nn;

public enum NNColor {
    JOKER(5),
    HEITAO(4),
    HONGTAO(3),
    MEIHAU(2),
    FANGKUAI(1);

    private int color;

    public int getColor() {
        return this.color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    private NNColor(int i) {
        this.color = i;
    }

}
