package com.zhuoan.biz.core.qzmj;

import com.zhuoan.biz.model.qzmj.DontMovePai;
import com.zhuoan.constant.QZMJConstant;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;

import java.util.*;

public class MaJiangCore {

    private static int yunsuanCount = 0;

    public static void main(String[] args) {

        long yunxingCount = 1000000000;
        int huCount = 0;
        for (int i = 0; i < yunxingCount; i++) {

            if (testHu()) {
                huCount++;
                System.out.println(i);
            }
        }
        System.out.println("运行次数：" + yunxingCount + "，胡次数：" + huCount);
    }

    private static boolean testHu() {


        int[] indexs = randomPai(QZMJConstant.ALL_PAI.length);
        Integer[] pai = new Integer[QZMJConstant.ALL_PAI.length];

        for (int i = 0; i < indexs.length; i++) {

            pai[i] = QZMJConstant.ALL_PAI[indexs[i]];
        }

        Integer[] myPai = Arrays.copyOfRange(pai, 0, 14);

        return isHu(Arrays.asList(myPai), 0, 11, 2);
    }

    private static int[] randomPai(int paiCount) {

        int[] nums = new int[paiCount];
        Random rd = new Random();
        for (int i = 0; i < nums.length; i++) {
            while (true) {
                int num = rd.nextInt(paiCount);
                // 开局庄家手牌不带花
                if (i < QZMJConstant.LEFT_PAI_COUNT) {
                    if (!QZMJConstant.isHuaPai(QZMJConstant.ALL_PAI[num]) && !ArrayUtils.contains(nums, num)) {
                        nums[i] = num;
                        break;
                    }
                } else if (!ArrayUtils.contains(nums, num)) {
                    nums[i] = num;
                    break;
                } else if (num == 0) {
                    if (ArrayUtils.indexOf(nums, num) == i) {
                        break;
                    }
                }
            }
        }
        return nums;
    }

    public static int[] isGang(List<Integer> myPai, Integer otherPai, int type, List<DontMovePai> penghistory) {


        int paiCount = tongPaiCount(myPai, otherPai);

        if (type == 1) {

            if (paiCount == 4) {  // 暗杠
                return new int[]{3, otherPai, otherPai, otherPai, otherPai};
            } else { // 补杠
                for (DontMovePai p : penghistory) {
                    if (otherPai.equals(p.getFoucsPai())) {
                        return new int[]{1, otherPai, otherPai, otherPai};
                    }
                }
            }

            // 判断手牌是否可以杠
            for (int pai : myPai) {
                // 暗杠
                if (tongPaiCount(myPai, pai) == 4) {
                    return new int[]{3, pai, pai, pai, pai};
                }
                // 补杠
                for (DontMovePai p : penghistory) {
                    if (pai == p.getFoucsPai()) {
                        return new int[]{1, pai, pai, pai};
                    }
                }
            }
        } else {
            // 明杠
            if (paiCount == 3) {
                return new int[]{2, otherPai, otherPai, otherPai};
            } else if (penghistory != null) {
                // 特殊情况
                for (DontMovePai p : penghistory) {
                    if (otherPai.equals(p.getFoucsPai())) {
                        return new int[]{1, otherPai, otherPai, otherPai};
                    }
                }
            }
        }

        return new int[]{0};
    }


    public static int tongPaiCount(List<Integer> myPai, Integer otherPai) {

        List<Integer> paiList = new ArrayList<Integer>();
        for (int pai : myPai) {
            if (otherPai.equals(pai)) {
                paiList.add(pai);
            }
        }
        return paiList.size();
    }

    public static int[] isPeng(List<Integer> myPai, Integer otherPai) {

        List<Integer> pengList = new ArrayList<Integer>();
        for (int pai : myPai) {
            if (otherPai.equals(pai)) {
                pengList.add(pai);
            }
        }
        if (pengList.size() >= 2) {
            return new int[]{1, pengList.get(0), pengList.get(1)};
        }
        return new int[]{0};
    }

    public static List<int[]> isChi(List<Integer> myPai, Integer otherPai, int jin) {
        if (jin == otherPai) {
            return null;
        }
        //1.查询otherPai是否属于可吃的牌
        List<Integer> zhupai = new ArrayList<>();
        if (QZMJConstant.TONG_PAI.contains(otherPai)) {
            zhupai = QZMJConstant.TONG_PAI;
        } else if (QZMJConstant.TIAO_PAI.contains(otherPai)) {
            zhupai = QZMJConstant.TIAO_PAI;
        } else if (QZMJConstant.WANG_PAI.contains(otherPai)) {
            zhupai = QZMJConstant.WANG_PAI;
        }

        if (zhupai != null && zhupai.size() > 0) {
            //获取牌的位置
            int index = zhupai.indexOf(otherPai);
            int maxLength = zhupai.size();
            //获取组合
            int a = index - 2 >= 0 ? zhupai.get(index - 2) : -1;
            int b = index - 1 >= 0 ? zhupai.get(index - 1) : -1;
            int e = index + 1 < maxLength ? zhupai.get(index + 1) : -1;
            int f = index + 2 < maxLength ? zhupai.get(index + 2) : -1;
            List<int[]> chiList = new ArrayList<int[]>();
            if (myPai.contains(a) && myPai.contains(b)) {
                // 金牌不提示吃
                if (a != jin && b != jin) {
                    chiList.add(new int[]{a, b});
                }
            }
            if (myPai.contains(b) && myPai.contains(e)) {
                // 金牌不提示吃
                if (e != jin && b != jin) {
                    chiList.add(new int[]{b, e});
                }
            }
            if (myPai.contains(e) && myPai.contains(f)) {
                // 金牌不提示吃
                if (e != jin && f != jin) {
                    chiList.add(new int[]{e, f});
                }
            }
            return chiList;
        }
        return null;
    }

    public static boolean isHu(List<Integer> myPai, Integer otherPai, Integer jinPai, int hasJinNoPingHu) {
        if (null != otherPai && otherPai == jinPai) {//打出的金牌不能胡
            return false;
        }

        List<Integer> paiList = new ArrayList<Integer>(myPai);
        List<Integer> jinList = new ArrayList<Integer>();

        for (int i : paiList) {

            if (jinPai.equals(i)) {

                jinList.add(i);
            }
        }

        paiList.removeAll(jinList);


        // 三金倒（自己摸牌时）
        if (otherPai.equals(0)) {
            if (QZMJConstant.HAS_JIN_NO_PING_HU_2 != hasJinNoPingHu && jinList.size() >= 3) {
                return true;
            }
        }

        boolean isCanhu = isCanHU(paiList, jinList, otherPai);
        return isCanhu;
    }

    public static boolean isCanHU(List<Integer> paiList, List<Integer> jinList, int pai) {

        List<Integer> pais = new ArrayList<Integer>(paiList);
        if (pai != 0) {
            pais.add(pai);
        }

        // 不满足n%3==2，则不能胡牌
        if ((pais.size() + jinList.size()) % 3 != 2) {
            return false;
        }

        //只有两张牌
        if (pais.size() == 2) {
            return pais.get(0).equals(pais.get(1));
        }

        //牌序
        Collections.sort(pais);

        //依据牌的顺序从左到右依次分出将牌
        for (Integer i = 0; i < pais.size(); i++) {

            List<Integer> paiT = new ArrayList<Integer>(pais);
            List<Integer> jinPaiList = new ArrayList<Integer>(jinList);
            List<Integer> ds = new ArrayList<Integer>();

            for (Integer j = 0; j < pais.size(); j++) {

                if (pais.get(i).equals(pais.get(j))) {
                    ds.add(pais.get(i));
                }
            }

            //判断是否能做将牌
            if (ds.size() >= 2) {

                //移除两张将牌
                paiT.remove(pais.get(i));
                paiT.remove(pais.get(i));

                //避免重复运算 将光标移到其他牌上（跳一步）
                i += 1;

                if (HuPaiPanDin(paiT, jinPaiList)) {
                    return true;
                }
            }
        }

        if (jinList.size() > 0) {

            //依据牌的顺序从左到右依次分出将牌
            for (Integer i = 0; i < pais.size(); i++) {

                List<Integer> paiT = new ArrayList<Integer>(pais);
                List<Integer> jinPaiList = new ArrayList<Integer>(jinList);
                if (jinList.size() > 0 && i < pais.size()) {

                    //移除一张牌
                    paiT.remove(pais.get(i));
                    //一张金牌做将牌
                    jinPaiList.remove(jinPaiList.get(0));

                    if (HuPaiPanDin(paiT, jinPaiList)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    private static boolean HuPaiPanDin(List<Integer> paiList, List<Integer> jinList) {

        yunsuanCount++;

        List<Integer> paiNewList = new ArrayList<Integer>(paiList);
        List<Integer> jinNewList = new ArrayList<Integer>(jinList);

        if (paiList.size() == 0) {
            return true;
        }

        List<Integer> fs = new ArrayList<Integer>();
        for (Integer i = 0; i < paiList.size(); i++) {

            if (paiList.get(i).equals(paiList.get(0))) {
                fs.add(paiList.get(0));
            }
        }

        //组成刻子
        if (fs.size() == 3) {

            paiList.remove(paiList.get(0));
            paiList.remove(paiList.get(0));
            paiList.remove(paiList.get(0));

            return HuPaiPanDin(paiList, jinList);

        } else { //组成顺子（排除字牌）

            Integer p1 = paiList.get(0) + 1;
            Integer p2 = paiList.get(0) + 2;

            if (!QZMJConstant.ZI_PAI.contains(paiList.get(0))) {
                if (paiList.contains(p1) && paiList.contains(p2)) {

                    paiList.remove(paiList.get(0));
                    paiList.remove(p1);
                    paiList.remove(p2);

                    if (HuPaiPanDin(paiList, jinList)) {
                        return true;
                    }
                }
            }

            /**
             * 20171009 lhp
             * 用金代替其他牌（进入一个新的判断流程）
             */
            if (paiNewList.size() > 0 && jinNewList.size() > 0) {

                fs.clear();
                for (Integer i = 0; i < paiNewList.size(); i++) {

                    if (paiNewList.get(i).equals(paiNewList.get(0))) {
                        fs.add(paiNewList.get(0));
                    }
                }

                if (fs.size() == 2 && jinNewList.size() >= 1) {
                    // 组成刻子,带一个金
                    List<Integer> pList = new ArrayList<Integer>(paiNewList);
                    pList.remove(pList.get(0));
                    pList.remove(pList.get(0));

                    // 移除金牌
                    jinNewList.remove(jinNewList.get(0));

                    return HuPaiPanDin(pList, jinNewList);

                } else if (!QZMJConstant.ZI_PAI.contains(paiNewList.get(0))) {
                    // 用金代替其他牌，组成顺子（不能包含字牌）

                    // 当顺子的牌不能被10整除，且要是同类型的牌
                    if (p1 % 10 == 0 || paiNewList.get(0) / 10 != p1 / 10) {
                        p1 = p1 - 3;
                    }

                    if (p2 % 10 == 0 || paiNewList.get(0) / 10 != p2 / 10) {
                        p2 = p2 - 3;
                    }

                    if (!paiNewList.contains(p1) && paiNewList.contains(p2) && jinNewList.size() >= 1) {

                        // 带一个金
                        List<Integer> pList = new ArrayList<Integer>(paiNewList);
                        pList.remove(pList.get(0));
                        pList.remove(p2);

                        // 移除金牌
                        jinNewList.remove(jinNewList.get(0));

                        return HuPaiPanDin(pList, jinNewList);

                    } else if (paiNewList.contains(p1) && !paiNewList.contains(p2) && jinNewList.size() >= 1) {
                        // 带一个金
                        List<Integer> pList = new ArrayList<Integer>(paiNewList);
                        pList.remove(pList.get(0));
                        pList.remove(p1);

                        // 移除金牌
                        jinNewList.remove(jinNewList.get(0));

                        return HuPaiPanDin(pList, jinNewList);

                    } else if (jinNewList.size() >= 2) {
                        // 带两个金
                        List<Integer> pList = new ArrayList<Integer>(paiNewList);
                        pList.remove(pList.get(0));
                        // 移除金牌
                        jinNewList.remove(jinNewList.get(0));
                        // 移除金牌
                        jinNewList.remove(jinNewList.get(0));
                        return HuPaiPanDin(pList, jinNewList);
                    }
                }
            }

            return false;
        }
    }

    public static List<Integer> isTingPai(List<Integer> myPai, Integer jinPai) {

        List<Integer> paiList = new ArrayList<Integer>(myPai);
        List<Integer> jinList = new ArrayList<Integer>();

        for (int i : paiList) {

            if (jinPai.equals(i)) {

                jinList.add(i);
            }
        }

        paiList.removeAll(jinList);

        List<Integer> result = new ArrayList<Integer>();

        for (int i : QZMJConstant.ALL_CAN_HU_PAI) {

            if (isCanHU(paiList, jinList, i)) {
                result.add(i);
            }
        }
        return result;
    }

    public static JSONArray tingPaiTip(List<Integer> pais, Integer jinPai, List<Integer> shengyuList) {

        // 方法开始执行时间
        long startTime = System.currentTimeMillis();
        yunsuanCount = 0;
        JSONArray array = new JSONArray();
        try {

            // 遍历玩家手牌
            for (Integer pai : pais) {

                List<Integer> paiList = new ArrayList<Integer>(pais);
                // 模拟打牌
                paiList.remove(pai);
                JSONObject result = youJinAndTingPaiPanDing(paiList, jinPai, shengyuList);
                if (result != null) {

                    // 如果听的牌没有包含金牌，则计算剩余金牌的数量
                    if (result.getInt("type") == 0) {

                        JSONArray vals = result.getJSONArray("values");
                        boolean hasJin = false;
                        for (int i = 0; i < vals.size(); i++) {
                            if (jinPai.equals(vals.getJSONObject(i).getInt("val"))) {
                                hasJin = true;
                                break;
                            }
                        }
                        // 计算可听剩余金牌的数量
                        if (!hasJin) {
                            // 金牌最多三张
                            int count = -1;
                            for (Integer p : shengyuList) {
                                if (jinPai.equals(p)) {
                                    count++;
                                }
                            }
                            if (count >= 0) {

                                JSONObject value = new JSONObject();
                                value.put("val", jinPai);
                                value.put("count", count);
                                vals.add(value);

                                result.put("values", vals);
                            }
                        }
                    }

                    result.put("pai", pai);
                    array.add(result);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
        return array;
    }

    public static JSONObject youJinAndTingPaiPanDing(List<Integer> paiList, Integer jin, List<Integer> shengyuList) {

        //牌序
        Collections.sort(paiList);

        int type = 0;

        // 获取所有可以听的牌
        List<Integer> valList = isTingPai(paiList, jin);

        // 所有牌都可以听，则说明可游金
        if (valList.size() == QZMJConstant.ALL_CAN_HU_PAI.length) {
            type = 1;
        }

        if (valList.size() > 0) {

            JSONObject obj = new JSONObject();
            obj.put("type", type);
            if (type == 0 && shengyuList != null) {
                JSONArray values = new JSONArray();
                for (Integer pai : valList) {
                    int count = 0;
                    for (Integer p : shengyuList) {
                        if (pai.equals(p)) {
                            count++;
                        }
                    }
                    // 金牌数量-1
                    if (pai.equals(jin)) {
                        count = count - 1;
                    }
                    JSONObject value = new JSONObject();
                    value.put("val", pai);
                    value.put("count", count);
                    values.add(value);
                }
                obj.put("values", values);
            }
            return obj;
        }
        return null;
    }

    public static int huPaiType(List<Integer> myPai, Integer otherPai, Integer jinPai, int huType, int hasJinNoPingHu) {

        // 自摸且带金，判断是否可以游金
        if (myPai.contains(jinPai) && huType == 1) {

            return huPaiHasJin(myPai, 0, jinPai, hasJinNoPingHu);

        } else {

            // 自摸
            if (huType == 1) {

                if (isHu(myPai, 0, jinPai, hasJinNoPingHu)) {
                    return QZMJConstant.HU_TYPE_ZM;
                }
            } else {
                // 平胡

                if (isHu(myPai, otherPai, jinPai, hasJinNoPingHu)) {
                    return QZMJConstant.HU_TYPE_PH;
                }
            }
        }

        return 0;
    }

    public static int huPaiHasJin(List<Integer> pais, Integer pai, Integer jin, int hasJinNoPingHu) {
        try {
            List<Integer> paiList = new ArrayList<Integer>(pais);

            //牌序
            Collections.sort(paiList);

            // 获取金牌
            List<Integer> jinList = new ArrayList<Integer>();
            for (int i : paiList) {
                if (jin.equals(i)) {
                    jinList.add(i);
                }
            }

            // 移除金牌
            paiList.removeAll(jinList);

            // 有金牌
            if (jinList.size() > 0) {

                if (isCanYouJin(paiList, jinList, pai)) {
                    // 游金
                    return QZMJConstant.HU_TYPE_YJ;
                }
            }

            // 三金倒（自己摸牌时）
            if (pai == 0) {
                if (QZMJConstant.HAS_JIN_NO_PING_HU_2 != hasJinNoPingHu && jinList.size() >= 3) {
                    // 三金倒
                    return QZMJConstant.HU_TYPE_SJD;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }

        return QZMJConstant.HU_TYPE_ZM;
    }

    public static boolean isCanYouJin(List<Integer> paiList, List<Integer> jinList, int pai) {


        List<Integer> pais = new ArrayList<Integer>(paiList);
        if (pai != 0) {
            pais.add(pai);
        }
        //只有两张将牌且带金
        if ((pais.size() == 0 && jinList.size() == 2) || (pais.size() == 1 && jinList.size() == 1)) {
            return true;
        }

        //牌序
        Collections.sort(pais);

        //依据牌的顺序从左到右依次分出将牌
        for (Integer i = 0; i < pais.size(); i++) {

            List<Integer> paiT = new ArrayList<Integer>(pais);
            List<Integer> jinPaiList = new ArrayList<Integer>(jinList);
            List<Integer> ds = new ArrayList<Integer>();

            for (Integer j = 0; j < pais.size(); j++) {

                if (pais.get(i).equals(pais.get(j))) {
                    ds.add(pais.get(i));
                }
            }

            //判断是否能做将牌
            if (ds.size() >= 2) {

                //移除两张将牌
                paiT.remove(pais.get(i));
                paiT.remove(pais.get(i));

                //避免重复运算 将光标移到其他牌上（跳一步）
                i += 1;

                if (HuPaiPanDin(paiT, jinPaiList)) {
                    // 剩下最后一张牌为将牌
                    if (jinPaiList.size() > 0 || paiT.size() % 3 == 1) {
                        return true;
                    }
                }
            }
        }

        if (jinList.size() > 0) {

            //依据牌的顺序从左到右依次分出将牌
            for (Integer i = 0; i < pais.size(); i++) {

                List<Integer> paiT = new ArrayList<Integer>(pais);
                List<Integer> jinPaiList = new ArrayList<Integer>(jinList);
                if (jinList.size() > 0 && i < pais.size()) {

                    //移除一张牌
                    paiT.remove(pais.get(i));
                    //一张金牌做将牌
                    jinPaiList.remove(jinPaiList.get(0));

                    if (HuPaiPanDin(paiT, jinPaiList)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean shuangSanYouPanDing(List<Integer> pais, Integer pai, Integer jin, int hasJinNoPingHu) {

        List<Integer> paiList = new ArrayList<Integer>(pais);

        //牌序
        Collections.sort(paiList);

        // 获取金牌
        List<Integer> jinList = new ArrayList<Integer>();
        for (int i : paiList) {
            if (jin.equals(i)) {
                jinList.add(i);
            }
        }

        // 金牌做对子，可以双、三游
        if (jinList.size() >= 2) {

            paiList = new ArrayList<Integer>(pais);
            // 移除两张金牌
            paiList.remove(jin);
            paiList.remove(jin);

            paiList.add(0);
            paiList.add(0);

            if (isHu(paiList, pai, jin, hasJinNoPingHu)) {
                return true;
            }
        }

        return false;
    }

    public static List<Integer> getCompensateList(List<Integer> myPai, List<Integer> outList, int jin, JSONArray tingList) {
        List<Integer> compensateList = new ArrayList<>();
        try {
            // 所有正确的手牌
            List<Integer> legalList = new ArrayList<>();
            // 有听打无听
            if (tingList.size() > 0) {
                // 所有听的牌
                List<Integer> allTing = new ArrayList<>();
                for (int i = 0; i < tingList.size(); i++) {
                    allTing.add(tingList.getJSONObject(i).getInt("pai"));
                }
                List<Integer> legalTing = getTingPaiList(tingList, jin, allTing);
                legalList.addAll(legalTing);
            } else {
                List<Integer> singleList = getAllSingle(myPai, jin);
                // 有单牌优先出单牌
                if (singleList.size() > 0) {
                    // 计算每个单牌桌面上的牌数
                    List<Integer> moreList = getMoreList(outList, singleList);
                    // 单牌见2先出
                    if (moreList.size() > 0) {
                        // 有字优先打字
                        List<Integer> moreZiList = new ArrayList<>();
                        for (Integer pai : moreList) {
                            if (QZMJConstant.ZI_PAI.contains(pai)) {
                                moreZiList.add(pai);
                            }
                        }
                        if (moreZiList.size() > 0) {
                            legalList.addAll(moreZiList);
                        } else {
                            legalList.addAll(moreList);
                        }
                    } else {
                        legalList.addAll(singleList);
                    }
                } else {
                    // 去除所有的刻 顺子
                    List<Integer> wipeList = wipeOffPai(myPai, jin);
                    // 去除刻 顺子之后的单牌
                    List<Integer> allSingle = getAllSingle(wipeList, jin);
                    // 有有单牌优先出单牌
                    if (allSingle.size() > 0) {
                        legalList.addAll(allSingle);
                    } else {
                        List<Integer> doubleList = getDoubleList(wipeList);
                        // 不失牌出牌
                        List<Integer> bestPai = getBestPai(wipeList, doubleList);
                        if (bestPai.size() > 0) {
                            legalList.addAll(bestPai);
                        } else {
                            List<Integer> moreList = getMoreList(outList, doubleList);
                            // 有双雀必须留双雀
                            if (doubleList.size() <= QZMJConstant.CARD_SIZE_FOUR && wipeList.size() != doubleList.size()) {
                                for (int i = 0; i < wipeList.size(); i++) {
                                    // 所有不是雀的牌，或者死雀多于2可以拆死雀
                                    if (!doubleList.contains(wipeList.get(i))) {
                                        legalList.add(wipeList.get(i));
                                    } else if (moreList.size() > 1 && moreList.contains(wipeList.get(i))) {
                                        legalList.add(wipeList.get(i));
                                    }
                                }
                            } else {
                                legalList.addAll(wipeList);
                            }
                        }
                    }
                }
            }
            for (Integer pai : myPai) {
                if (!legalList.contains(pai)) {
                    compensateList.add(pai);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return compensateList;
    }

    private static List<Integer> getTingPaiList(JSONArray tingList, int jin, List<Integer> allTing) {
        List<Integer> result = new ArrayList<>();
        // 加入判断，不能听无张,如果都听无张，那就都可以打
        for (int i = 0; i < tingList.size(); i++) {
            JSONObject tingPai = tingList.getJSONObject(i);
            // 如果听的牌不等于金且count!=0,则加入legalList
            boolean isNone = true;
            if (tingPai.containsKey("values")) {
                JSONArray needPai = tingPai.getJSONArray("values");
                for (int j = 0; j < needPai.size(); j++) {
                    JSONObject temp = needPai.getJSONObject(j);
                    if (temp.getInt("val") != jin && temp.getInt("count") > 0) {
                        isNone = false;
                        break;
                    }

                }
            }

            if (!isNone) {
                result.add(tingPai.getInt("pai"));
            }
        }
        if (result.size() == 0) {
            result.addAll(allTing);
        }
        return result;
    }

    private static List<Integer> getMoreList(List<Integer> outList, List<Integer> curList) {
        List<Integer> moreList = new ArrayList<>();
        // 统计当前手牌中没张牌桌面上的数量
        Map<Integer, Integer> outCountMap = getOutCount(curList, outList);
        for (Integer pai : outCountMap.keySet()) {
            // 已出牌数大于阈值的优先打
            if (outCountMap.get(pai) >= QZMJConstant.OUT_CARD_THRESHOLD) {
                moreList.add(pai);
            }
        }
        return moreList;
    }

    private static List<Integer> wipeOffPai(List<Integer> myPai, Integer jin) {
        List<Integer> copyPai = new ArrayList<>(myPai);
        // 去除金
        List<Integer> jinList = new ArrayList<>();
        for (int i = 0; i < copyPai.size(); i++) {
            if (copyPai.get(i).equals(jin)) {
                jinList.add(copyPai.get(i));
            }
        }
        copyPai.removeAll(jinList);
        // 手牌排序
        Collections.sort(copyPai);
        // 去除刻、顺子之后的手牌
        List<Integer> newPaiList = new ArrayList<>(copyPai);
        // 优先去除所有刻子
        for (int i = 0; i < copyPai.size(); i++) {
            List<Integer> threeList = new ArrayList<>();
            for (int j = 0; j < copyPai.size(); j++) {
                if (copyPai.get(i).equals(copyPai.get(j))) {
                    threeList.add(copyPai.get(j));
                }
            }
            // 去掉所有刻
            if (threeList.size() >= QZMJConstant.CARD_SIZE_THREE) {
                for (int j = 0; j < QZMJConstant.CARD_SIZE_THREE; j++) {
                    newPaiList.remove(threeList.get(j));
                }
                return wipeOffPai(newPaiList, jin);
            }
        }
        // 去除顺子
        for (int i = 0; i < copyPai.size(); i++) {
            if (!QZMJConstant.ZI_PAI.contains(copyPai.get(i)) && newPaiList.contains(copyPai.get(i))) {
                Integer nextOne = copyPai.get(i) + 1;
                Integer nextTwo = copyPai.get(i) + 2;
                Integer nextThree = copyPai.get(i) + 3;
                if (newPaiList.contains(nextOne) && newPaiList.contains(nextTwo)) {
                    newPaiList.remove(nextOne);
                    newPaiList.remove(nextTwo);
                    // 计算当前这张牌的牌数
                    List<Integer> doubleList = new ArrayList<>();
                    for (int j = 0; j < newPaiList.size(); j++) {
                        if (copyPai.get(i).equals(newPaiList.get(j))) {
                            doubleList.add(newPaiList.get(j));
                        }
                    }
                    // 如果当前牌是对子且有其他的牌可以补足
                    if (newPaiList.contains(nextThree) && doubleList.size() >= QZMJConstant.CARD_SIZE_TWO) {
                        newPaiList.remove(nextThree);
                    } else {
                        newPaiList.remove(copyPai.get(i));
                    }
                }
            }
        }
        return newPaiList;
    }

    private static List<Integer> getDoubleList(List<Integer> myPai) {
        List<Integer> allDoubleList = new ArrayList<>();
        for (int i = 0; i < myPai.size(); i++) {
            List<Integer> doubleList = new ArrayList<>();
            // 取所有相同的牌
            for (int j = 0; j < myPai.size(); j++) {
                if (myPai.get(i).equals(myPai.get(j))) {
                    doubleList.add(myPai.get(j));
                }
            }
            if (doubleList.size() == QZMJConstant.CARD_SIZE_TWO) {
                // 防止重复添加
                if (!allDoubleList.contains(doubleList.get(0))) {
                    allDoubleList.addAll(doubleList);
                }
            }
        }

        return allDoubleList;
    }

    private static Map<Integer, Integer> getOutCount(List<Integer> curList, List<Integer> outList) {
        // curList中已出牌数
        Map<Integer, Integer> outCountMap = new HashMap<>();
        // 所有已经出过的牌
        Map<Integer, Integer> countMap = new HashMap<>();
        for (Integer pai : outList) {
            // 已经存在+1,不存在存初值1
            if (countMap.containsKey(pai) && countMap.get(pai) != null) {
                countMap.put(pai, countMap.get(pai) + 1);
            } else {
                countMap.put(pai, 1);
            }
        }
        for (Integer pai : curList) {
            if (countMap.containsKey(pai) && countMap.get(pai) != null) {
                outCountMap.put(pai, countMap.get(pai));
            } else {
                outCountMap.put(pai, 0);
            }
        }
        return outCountMap;
    }

    private static List<Integer> getAllSingle(List<Integer> myPai, int jin) {
        List<Integer> copyPai = new ArrayList<>(myPai);
        // 去除金
        List<Integer> jinList = new ArrayList<>();
        for (int i = 0; i < copyPai.size(); i++) {
            if (copyPai.get(i).equals(jin)) {
                jinList.add(copyPai.get(i));
            }
        }
        copyPai.removeAll(jinList);
        List<Integer> singleList = new ArrayList<>();
        // 取手牌中的所有单牌
        for (Integer pai : copyPai) {
            if (checkSingle(copyPai, pai, jin)) {
                singleList.add(pai);
            }
        }
        return singleList;
    }

    private static boolean checkSingle(List<Integer> myPai, int pai, int jin) {
        int num = 0;
        for (Integer p : myPai) {
            // 字牌牌只计算相同的张数,万条筒取相邻的两张牌
            if (QZMJConstant.ZI_PAI.contains(pai)) {
                if (Math.abs(p - pai) == 0) {
                    num++;
                }
            } else if (p / 10 == pai / 10 && Math.abs(p - pai) <= 2) {
                // 两张牌相同或者可以吃
                if (p - pai == 0) {
                    num++;
                } else if (checkChiWithOutJin(p, pai, jin)) {
                    num++;
                }
            }
        }
        return num <= 1;
    }

    private static boolean checkChiWithOutJin(int pai1, int pai2, int jin) {
        List<Integer> paiList = new ArrayList<>();
        // 万条筒可以吃
        if (QZMJConstant.TONG_PAI.contains(pai1)) {
            paiList = QZMJConstant.TONG_PAI;
        } else if (QZMJConstant.TIAO_PAI.contains(pai1)) {
            paiList = QZMJConstant.TIAO_PAI;
        } else if (QZMJConstant.WANG_PAI.contains(pai1)) {
            paiList = QZMJConstant.WANG_PAI;
        }
        // 不是同一张牌
        if (pai1 != pai2) {
            // 同一类型的牌
            if (paiList.size() > 0 && paiList.contains(pai1) && paiList.contains(pai2)) {
                if (Math.abs(pai1 - pai2) <= 2) {
                    // 和金不是同一类型的牌
                    if (!paiList.contains(jin)) {
                        return true;
                    }
                    // 和金相同类型的添加一张金进行计算（需要排序）
                    List<Integer> list = new ArrayList<>();
                    list.add(pai1);
                    list.add(pai2);
                    list.add(jin);
                    Collections.sort(list);
                    // 金在第一张 且金为7万 7条 7筒  最后一张为9万 9条 9筒
                    if (list.get(0) == jin && jin % 10 == 7 && list.get(2) % 10 == 9) {
                        return false;
                    }
                    // 金在中间
                    if (list.get(1) == jin) {
                        return false;
                    }
                    // 金在第三张 且金为3万 3条 3筒  第一张为1万 1条 1筒
                    if (list.get(2) == jin && jin % 10 == 3 && list.get(0) % 10 == 1) {
                        return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private static List<Integer> getBestPai(List<Integer> myPai, List<Integer> doubleList) {
        List<Integer> bestPai = new ArrayList<>();
        Collections.sort(myPai);
        for (int i = 0; i < myPai.size(); i++) {
            // 当前手牌
            int pai = myPai.get(i);
            // 当前手牌+1,+2,+3
            int nextOne = myPai.get(i) + 1;
            int nextTwo = myPai.get(i) + 2;
            int nextThree = myPai.get(i) + 3;
            // 当前手牌-1,-2,-3
            int lastOne = myPai.get(i) - 1;
            int lastTwo = myPai.get(i) - 2;
            int lastThree = myPai.get(i) - 3;
            // 手牌中同时有x x+2 x+3且都是同类型的手牌 例如1,3,4
            if (pai / 10 == nextTwo / 10 && pai / 10 == nextThree / 10
                    && myPai.contains(nextTwo) && myPai.contains(nextThree)) {
                // 手牌中没有x-1则优先出x
                if (myPai.contains(lastOne) && pai / 10 == lastOne / 10) {
                    // 1 2 4 5的情况1 2都可以出 其余情况不限制
                    if (lastOne % 10 == 1) {
                        if (!doubleList.contains(pai) && !bestPai.contains(pai)) {
                            bestPai.add(pai);
                        }
                        if (!doubleList.contains(lastOne) && !bestPai.contains(lastOne)) {
                            bestPai.add(lastOne);
                        }
                    }
                } else {
                    // 不是对子
                    if (!doubleList.contains(pai) && !bestPai.contains(pai)) {
                        bestPai.add(pai);
                    }
                }
            }
            // 手牌中同时有x x-2 x-3且都是同类型的手牌 例如2,3,5
            if (pai / 10 == lastTwo / 10 && pai / 10 == lastThree / 10
                    && myPai.contains(lastTwo) && myPai.contains(lastThree)) {
                // 手牌中没有x+1则优先出x
                if (myPai.contains(nextOne) && pai / 10 == nextOne / 10) {
                    // 5 6 8 9的情况8 9都可以出 其余情况不限制
                    if (nextOne % 10 == 9) {
                        if (!doubleList.contains(pai) && !bestPai.contains(pai)) {
                            bestPai.add(pai);
                        }
                        if (!doubleList.contains(nextOne) && !bestPai.contains(nextOne)) {
                            bestPai.add(nextOne);
                        }
                    }
                } else {
                    // 不是对子
                    if (!doubleList.contains(pai) && !bestPai.contains(pai)) {
                        bestPai.add(pai);
                    }
                }
            }
        }
        return bestPai;
    }
}
