
package com.zhuoan.biz.model.pdk;

import com.zhuoan.biz.core.pdk.PDKColor;
import com.zhuoan.biz.core.pdk.PDKNum;

import java.util.Comparator;
import java.util.Map;

public class PDKPacker {
    private PDKColor color;
    private PDKNum num;
    public static final Comparator<PDKPacker> desc = new Comparator<PDKPacker>() {
        public int compare(PDKPacker o1, PDKPacker o2) {
            if (o1.getNum().getNum() > o2.getNum().getNum()) {
                return -1;
            } else {
                return o1.getNum().getNum() == o2.getNum().getNum() && o1.getColor().getColor() > o2.getColor().getColor() ? -1 : 1;
            }
        }
    };
    public static final Comparator<PDKPacker> asc = new Comparator<PDKPacker>() {
        public int compare(PDKPacker o2, PDKPacker o1) {
            if (o1.getNum().getNum() > o2.getNum().getNum()) {
                return -1;
            } else {
                return o1.getNum().getNum() == o2.getNum().getNum() && o1.getColor().getColor() > o2.getColor().getColor() ? -1 : 1;
            }
        }
    };
    public static final Comparator<Map<String, Integer>> singleDesc = new Comparator<Map<String, Integer>>() {
        public int compare(Map<String, Integer> o1, Map<String, Integer> o2) {
            return (Integer) o1.get("v") < (Integer) o2.get("v") ? -1 : 1;
        }
    };

    public PDKPacker(String colorNum) {
        try {
            String[] pai = colorNum.split("-");
            int c = Integer.valueOf(pai[0]);
            int n = Integer.valueOf(pai[1]);
            this.color = PDKColor.getColor(c);
            this.num = PDKNum.getNum(n);
        } catch (Exception var5) {
        }

    }

    public PDKPacker(PDKColor color, PDKNum num) {
        this.color = color;
        this.num = num;
    }

    public PDKColor getColor() {
        return this.color;
    }

    public void setColor(PDKColor color) {
        this.color = color;
    }

    public PDKNum getNum() {
        return this.num;
    }

    public void setNum(PDKNum num) {
        this.num = num;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            PDKPacker person = (PDKPacker) o;
            return !this.color.equals(person.color) ? false : this.num.equals(person.num);
        } else {
            return false;
        }
    }

    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + this.color.hashCode();
        hash = hash * 31 + this.num.hashCode();
        return hash;
    }

    public String toString() {
        String c = PDKColor.HEITAO.equals(this.color) ? "黑桃" : (PDKColor.HONGTAO.equals(this.color) ? "红桃" : (PDKColor.MEIHAU.equals(this.color) ? "梅花" : (PDKColor.FANGKUAI.equals(this.color) ? "方块" : "未知")));
        String n = PDKNum.P_2.equals(this.num) ? "2" : (PDKNum.P_A.equals(this.num) ? "A" : (PDKNum.P_K.equals(this.num) ? "K" : (PDKNum.P_Q.equals(this.num) ? "Q" : (PDKNum.P_J.equals(this.num) ? "J" : (PDKNum.P_10.equals(this.num) ? "10" : (PDKNum.P_9.equals(this.num) ? "9" : (PDKNum.P_8.equals(this.num) ? "8" : (PDKNum.P_7.equals(this.num) ? "7" : (PDKNum.P_6.equals(this.num) ? "6" : (PDKNum.P_5.equals(this.num) ? "5" : (PDKNum.P_4.equals(this.num) ? "4" : (PDKNum.P_3.equals(this.num) ? "3" : "未知"))))))))))));
        return c + n;
    }
}
