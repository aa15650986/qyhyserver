
package com.zhuoan.biz.model.gdy;

import com.zhuoan.biz.core.gdy.GDYColor;
import com.zhuoan.biz.core.gdy.GDYNum;
import java.util.Comparator;

public class GDYPacker {
    private GDYColor color;
    private GDYNum num;
    public static final Comparator<GDYPacker> desc = new Comparator<GDYPacker>() {
        public int compare(GDYPacker o1, GDYPacker o2) {
            if (o1.getNum().getNum() > o2.getNum().getNum()) {
                return -1;
            } else {
                return o1.getNum().getNum() == o2.getNum().getNum() && o1.getColor().getColor() < o2.getColor().getColor() ? -1 : 1;
            }
        }
    };
    public static final Comparator<GDYPacker> asc = new Comparator<GDYPacker>() {
        public int compare(GDYPacker o2, GDYPacker o1) {
            if (o1.getNum().getNum() > o2.getNum().getNum()) {
                return -1;
            } else {
                return o1.getNum().getNum() == o2.getNum().getNum() && o1.getColor().getColor() < o2.getColor().getColor() ? -1 : 1;
            }
        }
    };

    public GDYPacker(GDYColor color, GDYNum num) {
        this.color = color;
        this.num = num;
    }

    public GDYPacker() {
    }

    public GDYNum getNum() {
        return this.num;
    }

    public GDYColor getColor() {
        return this.color;
    }
}
