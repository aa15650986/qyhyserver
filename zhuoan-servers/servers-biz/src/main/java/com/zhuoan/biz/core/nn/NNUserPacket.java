package com.zhuoan.biz.core.nn;

import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.NNConstant;
import org.apache.commons.lang.math.RandomUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

/**
 *
 */
public class NNUserPacket implements Serializable {

    /**
     * 手里的5张牌
     */
    private NNPacker[] ps = new NNPacker[5];
    /**
     * 牌的类型  0:无牛，1~9:牛一~牛9，10:牛牛
     */
    private int type;
    /**
     * 是否赢了
     */
    private boolean win = false;
    /**
     * 是否是庄家
     */
    private boolean isBanker = false;
    /**
     * 玩家游戏状态
     */
    private int status = NNConstant.NN_USER_STATUS_INIT;
    /**
     * 分数
     */
    private double score = 0;
    
    private double nowScore = 0;
    /**
     * 明牌抢庄
     */
    private int[] mingPai;
    /**
     * 抢庄倍数
     */
    private int qzTimes = 0;



    private int luck;
    /**
     * 下注倍数
     */
    private int xzTimes;
    /**
     * 上局下注倍数
     */
    private int lastXzTimes;

    /**
     * 牌局统计数据
     */
    private int tongShaTimes = 0;
    private int tongPeiTimes = 0;
    private int niuNiuTimes = 0;
    private int wuNiuTimes = 0;
    private int winTimes = 0;

    
    private boolean isTuiZhu = false;
    
    private boolean isLP = false;
    
    
    
    public boolean isLP() {
		return isLP;
	}

	public void setLP(boolean isLP) {
		this.isLP = isLP;
	}

	public boolean isTuiZhu() {
		return isTuiZhu;
	}

	public void setTuiZhu(boolean isTuiZhu) {
		this.isTuiZhu = isTuiZhu;
	}

	public NNPacker[] getPs() {
        return ps;
    }

    public void setPs(NNPacker[] ps) {
        this.ps = ps;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isWin() {
        return win;
    }

    public void setWin(boolean win) {
        this.win = win;
    }

    public boolean isBanker() {
        return isBanker;
    }

    public void setBanker(boolean banker) {
        isBanker = banker;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public double getScore() {
        return new BigDecimal(score)
            .setScale(2, BigDecimal.ROUND_HALF_UP)
            .doubleValue();
    }

    public void setScore(double score) {
        this.score = new BigDecimal(score)
            .setScale(2, BigDecimal.ROUND_HALF_UP)
            .doubleValue();
    }
    

    public double getNowScore() {
    	  return new BigDecimal(nowScore)
    	            .setScale(2, BigDecimal.ROUND_HALF_UP)
    	            .doubleValue();
	}

	public void setNowScore(double nowScore) {
		 this.nowScore = new BigDecimal(nowScore)
		            .setScale(2, BigDecimal.ROUND_HALF_UP)
		            .doubleValue();
	}

	public int[] getMingPai() {
        return mingPai;
    }

    public void setMingPai(int[] mingPai) {
        this.mingPai = mingPai;
    }

    public int getQzTimes() {
        return qzTimes;
    }

    public void setQzTimes(int qzTimes) {
        this.qzTimes = qzTimes;
    }

    public int getLuck() {
        return luck;
    }

    public void setLuck(int luck) {
        this.luck = luck;
    }

    public int getXzTimes() {
        return xzTimes;
    }

    public void setXzTimes(int xzTimes) {
        this.xzTimes = xzTimes;
    }

    public int getLastXzTimes() {
        return lastXzTimes;
    }

    public void setLastXzTimes(int lastXzTimes) {
        this.lastXzTimes = lastXzTimes;
    }

    public int getTongShaTimes() {
        return tongShaTimes;
    }

    public void setTongShaTimes(int tongShaTimes) {
        this.tongShaTimes = tongShaTimes;
    }

    public int getTongPeiTimes() {
        return tongPeiTimes;
    }

    public void setTongPeiTimes(int tongPeiTimes) {
        this.tongPeiTimes = tongPeiTimes;
    }

    public int getNiuNiuTimes() {
        return niuNiuTimes;
    }

    public void setNiuNiuTimes(int niuNiuTimes) {
        this.niuNiuTimes = niuNiuTimes;
    }

    public int getWuNiuTimes() {
        return wuNiuTimes;
    }

    public void setWuNiuTimes(int wuNiuTimes) {
        this.wuNiuTimes = wuNiuTimes;
    }

    public int getWinTimes() {
        return winTimes;
    }

    public void setWinTimes(int winTimes) {
        this.winTimes = winTimes;
    }

    /**
     * 保存明牌抢庄的牌组
     */
    public void saveMingPai() {

        int[] mypai = getMyPai();
        Arrays.sort(mypai);
        int paiIndex = RandomUtils.nextInt(4);
        // 隐藏的牌放到最后一张
        if (paiIndex < 4) {
            int temp = mypai[paiIndex];
            mypai[paiIndex] = mypai[4];
            mypai[4] = temp;
        }
        this.mingPai = mypai;
    }

    /**
     * 初始化牌局信息
     */
    public void initUserPacket() {

        win = false;
        //score = 0;
        nowScore = 0;
        xzTimes = 0;
        qzTimes = 0;
    }

    public NNUserPacket() {
    }

    /**
     * 判断是否是四炸
     * 炸弹：5张牌有4张是一样的
     *
     * @return
     */
    public boolean isSiZha() {
        NNPacker[] newPs = NNPacker.sort(this.ps);
        int m1 = newPs[0].getNum().getNum();
        int m2 = newPs[1].getNum().getNum();
        int m3 = newPs[2].getNum().getNum();
        int m4 = newPs[3].getNum().getNum();
        int m5 = newPs[4].getNum().getNum();
        if (m1 == m2 && m2 == m3 && m3 == m4 || m2 == m3 && m3 == m4 && m4 == m5) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断是否是葫芦牛
     *
     * @return
     */
    public boolean isHuLuNiu() {

        NNPacker[] newPs = NNPacker.sort(this.ps);
        //3张牌值一样的牌，配两张牌值一样的牌（三带一对）即为“葫芦牛”。如：3、3、3、5、5，即为“葫芦牛”
        int m1 = newPs[0].getNum().getNum();
        int m2 = newPs[1].getNum().getNum();
        int m3 = newPs[2].getNum().getNum();
        int m4 = newPs[3].getNum().getNum();
        int m5 = newPs[4].getNum().getNum();
        if (m1 == m2 && m2 == m3 && m4 == m5 || m1 == m2 && m3 == m4 && m4 == m5) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断是否是五花牛
     * 5张牌的牌值均大于10即为“五花牛”。如：J、J、Q、Q、K即为“五花牛”
     *
     * @return
     */
    public boolean isWuHuaNiu() {
        NNPacker[] newPs = NNPacker.sort(this.ps);
        //如果数组最小值是大于10，那么就是五花
        int min = newPs[0].getNum().getNum();
        if (min > 10) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断是否是四花牛
     * 四花牛：5张牌中有4张牌大于10，另一张等于10
     *
     * @return
     */
    public boolean isSiHuaNiu() {
        NNPacker[] newPs = NNPacker.sort(this.ps);
        //如果数组最小值等于10，其他大于10，那么就是四花
        int min = newPs[0].getNum().getNum();
        int secondMin = newPs[1].getNum().getNum();
        if (min == 10 && secondMin > 10) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断是否是五小牛
     * 五小牛：5张牌点数加起来小于等于10
     *
     * @return
     */
    public boolean isWuXiaoNiu() {

        int sum = 0;
        for (NNPacker packer : ps) {
            sum += packer.getNum().getNum();
        }
        if (sum <= 10) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 判断是牛几
     *
     * @return
     */
    public int isNiuNum() {
        int[] n = new int[5];
        for (int i = 0; i < 5; i++) {
            if (ps[i].getNum().getNum() > 10) {
                n[i] = 10;
            } else {
                n[i] = ps[i].getNum().getNum();
            }
        }
        Map<String, Boolean> map = isHasNiu(n);
        if (map.get("isNiuNiu")) {
            return 10;
        }
        if (map.get("isNiuNum")) {
            int num = 0;
            for (int i : n) {
                num += i;
            }
            return num % 10;
        } else {
            return 0;
        }
    }

    /**
     * 判断是否有牛
     *
     * @param i
     * @return
     */
    private Map<String, Boolean> isHasNiu(int[] i) {

        // 是否有牛
        boolean isNiuNum = false;
        // 是否是牛牛
        boolean isNiuNiu = false;
        for (int m = 0; m <= 2; m++) {
            for (int n = m + 1; n <= 3; n++) {
                for (int z = n + 1; z <= 4; z++) {
                    if ((i[m] + i[n] + i[z]) % 10 == 0) {
                        isNiuNum = true;
                        int num = 0;
                        for (int x = 0; x <= 4; x++) {
                            if (x != m && x != n && x != z) {
                                num += i[x];
                            }
                        }
                        if (num % 10 == 0) {
                            isNiuNiu = true;
                        }
                    }
                }
            }
        }
        Map<String, Boolean> result = new HashMap<String, Boolean>();
        result.put("isNiuNum", isNiuNum);
        result.put("isNiuNiu", isNiuNiu);
        return result;
    }

    public NNUserPacket(NNPacker[] ps, List<Integer> types) {
        this(ps, false, types);
    }

    /**
     * 构造方法
     * 同花顺牛（*10）>五小牛（*9）>炸弹牛（*8）>葫芦牛（*7）>同花牛（*6）>五花牛（*5）>顺子牛（*5）
     *
     * @param ps       牌
     * @param isBanker 是否是庄
     */
    public NNUserPacket(NNPacker[] ps, boolean isBanker, List<Integer> types) {

        this.ps = ps;
        this.isBanker = isBanker;
        int jokerNum = 0;//鬼牌数量
        for (int i = 0; i < ps.length; i++) {
            if (NNColor.JOKER.getColor() == ps[i].getColor().getColor())
                jokerNum++;
        }
        if (jokerNum > 0) {//包含大小鬼
            // 癞子玩法
            this.type = isLalZiNiuNum(jokerNum);
            return;
        }

        if (types.contains(NN.SPECIALTYPE_TONGHUASHUNZINIU) && isTongHuaShunZi()) {//同花顺牛
            this.type = NN.SPECIALTYPE_TONGHUASHUNZINIU;
            return;
        }
        if (types.contains(NN.SPECIALTYPE_WUXIAONIU) && isWuXiaoNiu()) { // 五小牛
            this.type = NN.SPECIALTYPE_WUXIAONIU;
            return;
        }
        if (types.contains(NN.SPECIALTYPE_ZHADANNIU) && isSiZha()) { // 炸弹
            this.type = NN.SPECIALTYPE_ZHADANNIU;
            return;
        }
        if (types.contains(NN.SPECIALTYPE_HULUNIU) && isHuLuNiu()) {// 葫芦牛
            this.type = NN.SPECIALTYPE_HULUNIU;
            return;
        }
        if (types.contains(NN.SPECIALTYPE_TONGHUANIU) && isTongHua()) {//同花牛
            this.type = NN.SPECIALTYPE_TONGHUANIU;
            return;
        }
        if (types.contains(NN.SPECIALTYPE_WUHUANIU) && isWuHuaNiu()) { // 五花牛
            this.type = NN.SPECIALTYPE_WUHUANIU;
            return;
        }
        if (types.contains(NN.SPECIALTYPE_SIHUANIU) && isSiHuaNiu()) { // 四花牛
            this.type = NN.SPECIALTYPE_SIHUANIU;
            return;
        }
        if (types.contains(NN.SPECIALTYPE_SHUNZINIU) && isShunZi()) {//顺子牛
            this.type = NN.SPECIALTYPE_SHUNZINIU;
            return;
        }
        // 普通牌型
        this.type = isNiuNum();

    }

    /**
     * 癞子 判断是牛几
     * (只支持最多两种鬼牌算法)
     *
     * @return
     */
    public int isLalZiNiuNum(int jokerNum) {
        if (0 == jokerNum) return isNiuNum();// 普通牌型
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < ps.length; i++) {
            if (NNColor.JOKER.getColor() != ps[i].getColor().getColor())
                list.add(ps[i].getNum().getNum() > 10 ? 10 : ps[i].getNum().getNum());//牌值大于10等于10
        }
        if (1 == jokerNum) {//1张鬼牌
            int max = 0;
            int sum;
            //三张配 组合 牛牛
            if (4 == list.size()) {
                if ((list.get(0) + list.get(1) + list.get(2)) % 10 == 0) return 10;
                if ((list.get(0) + list.get(1) + list.get(3)) % 10 == 0) return 10;
                if ((list.get(0) + list.get(2) + list.get(3)) % 10 == 0) return 10;
                if ((list.get(1) + list.get(2) + list.get(3)) % 10 == 0) return 10;
            }
            for (int i = 0; i < list.size(); i++) {
                for (int j = i + 1; j < list.size(); j++) {
                    sum = list.get(i) + list.get(j);
                    if (sum == 20 || sum == 10) return 10;
                    else if (sum > 10) sum = sum % 10;
                    if (max < sum) max = sum;
                }
            }
            return max;
        }
        if (jokerNum == 2) {//2张鬼牌以上
            int[] n;
            Map<String, Boolean> map;
            int max = 0;
            int b;
            List<Integer> jokerList;
            for (int i = 1; i < 11; i++) {//大王小王只能当同一个牌值使用 10种情况
                jokerList = new ArrayList<>();
                jokerList.addAll(list);
                jokerList.add(i);
                jokerList.add(i);
                n = new int[jokerList.size()];
                for (int j = 0; j < jokerList.size(); j++) {
                    n[j] = jokerList.get(j);
                }
                map = isHasNiu(n);//判断是否有牛
                if (map.get("isNiuNiu")) return 10;
                if (map.get("isNiuNum")) {
                    int num = 0;
                    for (int k : n) {
                        num += k;
                    }
                    b = num % 10;
                    if (max < b) max = b;
                }
            }
            return max;
        }
        return 0;
    }

    /**
     * 判断是否是同花牛
     * 55张牌的花色一样的牛牛即为“同花牛”
     *
     * @return
     */
    public boolean isTongHua() {
        NNPacker[] newPs = NNPacker.sort(this.ps);
        NNColor n1 = newPs[0].getColor();
        NNColor n2 = newPs[1].getColor();
        NNColor n3 = newPs[2].getColor();
        NNColor n4 = newPs[3].getColor();
        NNColor n5 = newPs[4].getColor();
        if (n1.getColor() == n2.getColor() && n2.getColor() == n3.getColor() && n3.getColor() == n4.getColor() && n4.getColor() == n5.getColor())
            return true;
        return false;
    }

    /**
     * 判断是否是顺子牛
     * 5张数值连续数字的牌组成的牛牛即为“顺子牛”，不论花色
     *
     * @return
     */
    public boolean isShunZi() {
        NNPacker[] newPs = NNPacker.sort(this.ps);

        int m1 = newPs[0].getNum().getNum();
        int m2 = newPs[1].getNum().getNum();
        int m3 = newPs[2].getNum().getNum();
        int m4 = newPs[3].getNum().getNum();
        int m5 = newPs[4].getNum().getNum();
        // 10、J、Q、K、A
        if (m1 == NNNum.P_A.getNum() && m2 == NNNum.P_10.getNum() && m3 == NNNum.P_J.getNum() && m4 == NNNum.P_Q.getNum() && m5 == NNNum.P_K.getNum()) {
            return true;
        } else {
            if ((m1 + 1) == m2 && (m2 + 1) == m3 && (m3 + 1) == m4 && (m4 + 1) == m5)
                return true;
        }
        return false;
    }

    /**
     * 判断是否是同花顺牛
     * 在达到“顺子牛”条件的基础上，5张牌的花色也必须相同
     *
     * @return
     */
    public boolean isTongHuaShunZi() {
        if (isTongHua()) {
            if (isShunZi()) return true;
        }
        return false;
    }

    /**
     * 获取玩家手牌
     *
     * @return
     */
    public int[] getMyPai() {
        int[] pais = new int[ps.length];
        if (ps[0] != null) {

            for (int i = 0; i < pais.length; i++) {
                int num = 0;
                if (ps[i].getColor().equals(NNColor.HONGTAO)) {
                    num = 20;
                } else if (ps[i].getColor().equals(NNColor.MEIHAU)) {
                    num = 40;
                } else if (ps[i].getColor().equals(NNColor.FANGKUAI)) {
                    num = 60;
                } else if (ps[i].getColor().equals(NNColor.JOKER)) {
                    num = 80;
                }
                pais[i] = ps[i].getNum().getNum() + num;
            }
        }
        return pais;
    }


    /**
     * 经过排序整理过的牌
     *
     * @return
     */
    public int[] getSortPai() {

        int[] pais = getMyPai();
        // 取得牌的数值
        for (int i = 0; i < pais.length; i++) {
            int number = pais[i] % 20;
            if (number > 10) {
                pais[i] = 10;
            } else {
                pais[i] = number;
            }
        }
        int[] pindex = new int[pais.length];
        boolean isSort = false;
        for (int m = 0; m <= 2; m++) {
            for (int n = m + 1; n <= 3; n++) {
                for (int z = n + 1; z <= 4; z++) {
                    // 判断是否有牛
                    if ((pais[m] + pais[n] + pais[z]) % 10 == 0) {

                        pindex[0] = m;
                        pindex[1] = n;
                        pindex[2] = z;

                        pais[m] = 0;
                        pais[n] = 0;
                        pais[z] = 0;

                        int paiindex = 3;
                        for (int i = 0; i < pais.length; i++) {
                            if (pais[i] > 0) {
                                pindex[paiindex] = i;
                                paiindex++;
                            }
                        }

                        // 排序完成，跳出循环
                        m = n = z = 5;
                        isSort = true;
                    }
                }
            }
        }

        int[] myPai = getMyPai();
        if (isSort) {
            int[] newPais = new int[myPai.length];
            for (int i = 0; i < pindex.length; i++) {
                int ii = pindex[i];
                newPais[i] = myPai[ii];
            }
            return newPais;
        }
        return myPai;
    }


    /**
     * 玩家手动配牛，判断是否有牛
     *
     * @param pais
     * @return
     */
    public boolean peiNiu(int[] pais) {

        if (pais.length > 0) {

            int sum = 0;
            for (int p : pais) {
                int val = p % 20;
                if (val > 10) {
                    val = 10;
                }
                sum += val;
            }

            if (sum % 10 == 0) {

                return true;
            }
        }
        return false;
    }

}
