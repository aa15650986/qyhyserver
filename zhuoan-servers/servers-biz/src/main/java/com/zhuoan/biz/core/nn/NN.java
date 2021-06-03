
package com.zhuoan.biz.core.nn;

import com.zhuoan.biz.model.nn.NNPackerCompare;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang.ArrayUtils;

public class NN {
    public static final int SPECIALTYPE_SHUNZINIU = 13;
    public static final int SPECIALTYPE_SIHUANIU = 19;
    public static final int SPECIALTYPE_WUHUANIU = 20;
    public static final int SPECIALTYPE_TONGHUANIU = 23;
    public static final int SPECIALTYPE_HULUNIU = 30;
    public static final int SPECIALTYPE_ZHADANNIU = 40;
    public static final int SPECIALTYPE_WUXIAONIU = 50;
    public static final int SPECIALTYPE_TONGHUASHUNZINIU = 60;
    private static int paiIndex = 0;
    public static final int RANDOM = 400;
    public static final int RANDOM_TONGHUASHUNZINIU = 1;
    public static final int RANDOM_WUXIAONIU = 3;
    public static final int RANDOM_ZHADANNIU = 4;
    public static final int RANDOM_HULUNIU = 5;
    public static final int RANDOM_TONGHUANIU = 6;
    public static final int RANDOM_WUHUANIU = 10;
    public static final int RANDOM_SHUNZINIU = 10;

    public NN() {
    }

    private static NNPacker[] initPai(String feature) {
        NNPacker[] p = new NNPacker[52];
        if ("1".equals(feature)) {
            p = new NNPacker[54];
            p[52] = new NNPacker(NNNum.BM, NNColor.JOKER);
            p[53] = new NNPacker(NNNum.P_A, NNColor.JOKER);
        }

        p[0] = new NNPacker(NNNum.P_A, NNColor.HEITAO);
        p[1] = new NNPacker(NNNum.P_2, NNColor.HEITAO);
        p[2] = new NNPacker(NNNum.P_3, NNColor.HEITAO);
        p[3] = new NNPacker(NNNum.P_4, NNColor.HEITAO);
        p[4] = new NNPacker(NNNum.P_5, NNColor.HEITAO);
        p[5] = new NNPacker(NNNum.P_6, NNColor.HEITAO);
        p[6] = new NNPacker(NNNum.P_7, NNColor.HEITAO);
        p[7] = new NNPacker(NNNum.P_8, NNColor.HEITAO);
        p[8] = new NNPacker(NNNum.P_9, NNColor.HEITAO);
        p[9] = new NNPacker(NNNum.P_10, NNColor.HEITAO);
        p[10] = new NNPacker(NNNum.P_J, NNColor.HEITAO);
        p[11] = new NNPacker(NNNum.P_Q, NNColor.HEITAO);
        p[12] = new NNPacker(NNNum.P_K, NNColor.HEITAO);
        p[13] = new NNPacker(NNNum.P_A, NNColor.HONGTAO);
        p[14] = new NNPacker(NNNum.P_2, NNColor.HONGTAO);
        p[15] = new NNPacker(NNNum.P_3, NNColor.HONGTAO);
        p[16] = new NNPacker(NNNum.P_4, NNColor.HONGTAO);
        p[17] = new NNPacker(NNNum.P_5, NNColor.HONGTAO);
        p[18] = new NNPacker(NNNum.P_6, NNColor.HONGTAO);
        p[19] = new NNPacker(NNNum.P_7, NNColor.HONGTAO);
        p[20] = new NNPacker(NNNum.P_8, NNColor.HONGTAO);
        p[21] = new NNPacker(NNNum.P_9, NNColor.HONGTAO);
        p[22] = new NNPacker(NNNum.P_10, NNColor.HONGTAO);
        p[23] = new NNPacker(NNNum.P_J, NNColor.HONGTAO);
        p[24] = new NNPacker(NNNum.P_Q, NNColor.HONGTAO);
        p[25] = new NNPacker(NNNum.P_K, NNColor.HONGTAO);
        p[26] = new NNPacker(NNNum.P_A, NNColor.MEIHAU);
        p[27] = new NNPacker(NNNum.P_2, NNColor.MEIHAU);
        p[28] = new NNPacker(NNNum.P_3, NNColor.MEIHAU);
        p[29] = new NNPacker(NNNum.P_4, NNColor.MEIHAU);
        p[30] = new NNPacker(NNNum.P_5, NNColor.MEIHAU);
        p[31] = new NNPacker(NNNum.P_6, NNColor.MEIHAU);
        p[32] = new NNPacker(NNNum.P_7, NNColor.MEIHAU);
        p[33] = new NNPacker(NNNum.P_8, NNColor.MEIHAU);
        p[34] = new NNPacker(NNNum.P_9, NNColor.MEIHAU);
        p[35] = new NNPacker(NNNum.P_10, NNColor.MEIHAU);
        p[36] = new NNPacker(NNNum.P_J, NNColor.MEIHAU);
        p[37] = new NNPacker(NNNum.P_Q, NNColor.MEIHAU);
        p[38] = new NNPacker(NNNum.P_K, NNColor.MEIHAU);
        p[39] = new NNPacker(NNNum.P_A, NNColor.FANGKUAI);
        p[40] = new NNPacker(NNNum.P_2, NNColor.FANGKUAI);
        p[41] = new NNPacker(NNNum.P_3, NNColor.FANGKUAI);
        p[42] = new NNPacker(NNNum.P_4, NNColor.FANGKUAI);
        p[43] = new NNPacker(NNNum.P_5, NNColor.FANGKUAI);
        p[44] = new NNPacker(NNNum.P_6, NNColor.FANGKUAI);
        p[45] = new NNPacker(NNNum.P_7, NNColor.FANGKUAI);
        p[46] = new NNPacker(NNNum.P_8, NNColor.FANGKUAI);
        p[47] = new NNPacker(NNNum.P_9, NNColor.FANGKUAI);
        p[48] = new NNPacker(NNNum.P_10, NNColor.FANGKUAI);
        p[49] = new NNPacker(NNNum.P_J, NNColor.FANGKUAI);
        p[50] = new NNPacker(NNNum.P_Q, NNColor.FANGKUAI);
        p[51] = new NNPacker(NNNum.P_K, NNColor.FANGKUAI);
        return p;
    }

    public static NNPacker[] xiPai(String feature) {
        NNPacker[] pais = initPai(feature);
        NNPacker[] newPais = new NNPacker[pais.length];
        List<Integer> list = new ArrayList();

        int i;
        for(i = 0; i < pais.length; ++i) {
            list.add(i);
        }

        Collections.shuffle(list);

        for(i = 0; i < list.size(); ++i) {
            newPais[i] = pais[(Integer)list.get(i)];
        }

        return newPais;
    }

    private static int[] randomPai(String feature) {
        int[] nums = new int[52];
        if ("1".equals(feature)) {
            nums = new int[54];
        }

        Random rd = new Random();

        int num;
        for(int i = 0; i < nums.length; ++i) {
            do {
                if ("1".equals(feature)) {
                    num = rd.nextInt(54);
                } else {
                    num = rd.nextInt(52);
                }

                if (!ArrayUtils.contains(nums, num)) {
                    nums[i] = num;
                    break;
                }
            } while(num != 0 || ArrayUtils.indexOf(nums, num) != i);
        }

        return nums;
    }

    public static List<NNPacker[]> faPai(NNPacker[] pais, int userCount) {
        List<NNPacker[]> userPacketList = new ArrayList();
        paiIndex = 0;

        for(int i = 0; i < userCount; ++i) {
            NNPacker[] pai = new NNPacker[5];

            for(int j = 0; j < 5; ++j) {
                pai[j] = pais[paiIndex];
                ++paiIndex;
            }

            userPacketList.add(pai);
        }

        return userPacketList;
    }

    public static void main(String[] args) {
        jokerTest(0, 500000);
        jokerTest(1, 500);
        jokerTest(2, 500);
        isWin();
    }

    private static void isWin() {
        List<Integer> types = new ArrayList();
        types.add(30);
        types.add(19);
        types.add(20);
        types.add(50);
        types.add(40);
        types.add(23);
        types.add(13);
        types.add(60);
        NNPacker[] p = new NNPacker[]{new NNPacker(NNNum.P_6, NNColor.HONGTAO), new NNPacker(NNNum.P_2, NNColor.FANGKUAI), new NNPacker(NNNum.P_3, NNColor.HONGTAO), new NNPacker(NNNum.P_4, NNColor.HEITAO), new NNPacker(NNNum.P_5, NNColor.FANGKUAI)};
        NNUserPacket up = new NNUserPacket(p, true, types);
        NNPacker[] p1 = new NNPacker[]{new NNPacker(NNNum.P_10, NNColor.FANGKUAI), new NNPacker(NNNum.P_6, NNColor.FANGKUAI), new NNPacker(NNNum.P_K, NNColor.FANGKUAI), new NNPacker(NNNum.P_4, NNColor.FANGKUAI), new NNPacker(NNNum.P_10, NNColor.HEITAO)};
        NNUserPacket up1 = new NNUserPacket(p1, types);
        NNUserPacket u = NNPackerCompare.getWin(up1, up);
        System.out.println(up.isWin());
        System.out.println(u.getType());
    }

    private static void jokerTest(int num, int count) {
        List<Integer> types = new ArrayList();
        types.add(30);
        types.add(20);
        types.add(50);
        types.add(40);
        types.add(23);
        types.add(13);
        types.add(60);
        System.out.println("N大于0 的会 牛几 ，大于10 特殊牌");

        for(int i = 0; i < count; ++i) {
            NNPacker[] p1 = xiPai((String)null);
            NNPacker[] p;
            NNUserPacket nn;
            if (1 == num) {
                p = new NNPacker[]{new NNPacker(NNNum.BM, NNColor.JOKER), p1[1], p1[2], p1[3], p1[4]};
                nn = new NNUserPacket(p, true, types);
                System.out.println("牌值：小鬼 " + p[1].getNum().getNum() + " " + p[2].getNum().getNum() + " " + p[3].getNum().getNum() + " " + p[4].getNum().getNum() + "    牛:" + nn.getType());
            } else if (2 == num) {
                p = new NNPacker[]{new NNPacker(NNNum.BM, NNColor.JOKER), new NNPacker(NNNum.P_A, NNColor.JOKER), p1[2], p1[3], p1[4]};
                nn = new NNUserPacket(p, true, types);
                System.out.println("牌值：小鬼 大鬼 " + p[2].getNum().getNum() + " " + p[3].getNum().getNum() + " " + p[4].getNum().getNum() + "    牛:" + nn.getType());
            } else {
                p = new NNPacker[]{p1[0], p1[1], p1[2], p1[3], p1[4]};
                nn = new NNUserPacket(p, true, types);
                String s = nn.getType() == 13 ? "顺子牛" : (nn.getType() == 19 ? "四花牛" : (nn.getType() == 20 ? "五花牛" : (nn.getType() == 23 ? "同花牛" : (nn.getType() == 30 ? "葫芦牛" : (nn.getType() == 40 ? "炸弹牛" : (nn.getType() == 50 ? "五小牛" : (nn.getType() == 60 ? "同花顺牛" : "")))))));
                if (!"".equals(s)) {
                    System.out.println("牌值：" + huaSe(p[0].getColor().getColor()) + p[0].getNum().getNum() + " " + huaSe(p[1].getColor().getColor()) + p[1].getNum().getNum() + " " + huaSe(p[2].getColor().getColor()) + p[2].getNum().getNum() + " " + huaSe(p[3].getColor().getColor()) + p[3].getNum().getNum() + " " + huaSe(p[4].getColor().getColor()) + p[4].getNum().getNum() + "    牛:" + s);
                }
            }
        }

    }

    private static String huaSe(int color) {
        if (1 == color) {
            return "方块";
        } else if (2 == color) {
            return "梅花";
        } else if (3 == color) {
            return "红桃";
        } else if (4 == color) {
            return "黑桃";
        } else {
            return 5 == color ? "鬼牌" : "";
        }
    }
}
