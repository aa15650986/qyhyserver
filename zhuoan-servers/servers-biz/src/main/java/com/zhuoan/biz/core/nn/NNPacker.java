
package com.zhuoan.biz.core.nn;


public class NNPacker {
    private NNNum num;
    private NNColor color;

    public NNPacker(String num) {
        this.num = NNNum.valueOf(num);
    }

    public NNPacker(NNNum num, NNColor color) {
        this.num = num;
        this.color = color;
    }

    public NNPacker() {
    }

    public NNNum getNum() {
        return this.num;
    }

    public void setNum(NNNum num) {
        this.num = num;
    }

    public NNColor getColor() {
        return this.color;
    }

    public void setColor(NNColor color) {
        this.color = color;
    }

    public int compare(NNPacker p) {
        int compare = this.num.getNum() == p.num.getNum() ? 0 : (this.num.getNum() > p.num.getNum() ? 1 : -1);
        if (compare != 0) {
            return compare;
        } else {
            compare = this.getColor().getColor() == p.getColor().getColor() ? 0 : (this.getColor().getColor() > p.getColor().getColor() ? 1 : -1);
            return compare;
        }
    }

    public int compareNum(NNPacker p) {
        int t = this.getNum().getNum();
        int p1 = p.getNum().getNum();
        if (t == p1) {
            return 0;
        } else {
            return t > p1 ? 1 : -1;
        }
    }

    public static NNPacker[] sort(NNPacker[] ps) {
        for(int i = 0; i < ps.length; ++i) {
            int k = i;

            for(int j = i - 1; j >= 0 && ps[k] != null && ps[k].compare(ps[j]) < 0; --j) {
                exchange(ps, k, j);
                --k;
            }
        }

        return ps;
    }

    public String toString() {
        String c = NNColor.HEITAO.equals(this.color) ? "黑桃" : (NNColor.HONGTAO.equals(this.color) ? "红桃" : (NNColor.MEIHAU.equals(this.color) ? "梅花" : (NNColor.FANGKUAI.equals(this.color) ? "方块" : "未知")));
        String n = NNNum.P_2.equals(this.num) ? "2" : (NNNum.P_A.equals(this.num) ? "A" : (NNNum.P_K.equals(this.num) ? "K" : (NNNum.P_Q.equals(this.num) ? "Q" : (NNNum.P_J.equals(this.num) ? "J" : (NNNum.P_10.equals(this.num) ? "10" : (NNNum.P_9.equals(this.num) ? "9" : (NNNum.P_8.equals(this.num) ? "8" : (NNNum.P_7.equals(this.num) ? "7" : (NNNum.P_6.equals(this.num) ? "6" : (NNNum.P_5.equals(this.num) ? "5" : (NNNum.P_4.equals(this.num) ? "4" : (NNNum.P_3.equals(this.num) ? "3" : "未知"))))))))))));
        return c + n;
    }

	private static void exchange(NNPacker[] ps, int i, int j) {
        NNPacker p = ps[i];
        ps[i] = ps[j];
        ps[j] = p;
    }
}
