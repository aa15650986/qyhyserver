package com.zhuoan.biz.core.nn;

import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.nn.NNGameRoomNew;
import com.zhuoan.constant.NNConstant;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NNServer {

    private static NNPacker[] group;//组合 获取概率出现特色牌
    private static boolean isGroup;//结束获取 组合 获取概率出现特色牌

    /**
     * 洗牌
     *
     * @param roomNo
     */
    public static void xiPai(String roomNo) {

        NNGameRoomNew room = ((NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo));
        String feature = null;
        JSONObject baseInfo = room.getRoomInfo();
        if (baseInfo != null && baseInfo.containsKey("feature")) {
            feature = baseInfo.getString("feature");
        }
        NNPacker[] pai = NN.xiPai(feature);
        room.setPai(pai);
    }

    /**
     * 特殊牌 发牌 不包括癞子牌
     *
     * @param roomNo
     */
    public static void specialFaPai(String roomNo) {
        NNGameRoomNew room = ((NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo));
        List<String> uuidList = new ArrayList<>();
        for (String uuid : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
                if (room.getUserPacketMap().get(uuid).getStatus() > NNConstant.NN_USER_STATUS_INIT) {
                    uuidList.add(uuid);
                }
            }
        }
        // 发牌
        getRandomSpecial(uuidList, roomNo);
    }

    /**
     * 获取概率出现特色牌 不包括癞子牌
     *
     * @param uuidList
     * @return
     */
    private static void getRandomSpecial(List<String> uuidList, String roomNo) {
        if (uuidList == null || uuidList.size() == 0) return;
        NNGameRoomNew room = ((NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo));
        NNPacker[] paiAll = room.getPai();//所有的牌
        List<NNPacker> list = new ArrayList<>();
        for (NNPacker n : paiAll) {
            list.add(n);
        }

        NNPacker[] pai;
        List<String> userList = new ArrayList<>();//没有概率的玩家
        Collections.shuffle(uuidList);//洗人  防止最后一个人概率低
        for (int i = 0; i < uuidList.size(); i++) {
            pai = new NNPacker[5];
            int type = 0;
            if (isRandomSpecial(NN.RANDOM_TONGHUASHUNZINIU, NN.RANDOM)) {//同花顺牛
                type = NN.SPECIALTYPE_TONGHUASHUNZINIU;
            } else if (isRandomSpecial(NN.RANDOM_WUXIAONIU, NN.RANDOM)) {//五小牛
                type = NN.SPECIALTYPE_WUXIAONIU;
            } else if (isRandomSpecial(NN.RANDOM_ZHADANNIU, NN.RANDOM)) {//炸弹牛
                type = NN.SPECIALTYPE_ZHADANNIU;
            } else if (isRandomSpecial(NN.RANDOM_HULUNIU, NN.RANDOM)) {//葫芦牛
                type = NN.SPECIALTYPE_HULUNIU;
            } else if (isRandomSpecial(NN.RANDOM_TONGHUANIU, NN.RANDOM)) {//同花牛
                type = NN.SPECIALTYPE_TONGHUANIU;
            } else if (isRandomSpecial(NN.RANDOM_WUHUANIU, NN.RANDOM)) {//五花牛
                type = NN.SPECIALTYPE_WUHUANIU;
            } else if (isRandomSpecial(NN.RANDOM_SHUNZINIU, NN.RANDOM)) { //顺子牛
                type = NN.SPECIALTYPE_SHUNZINIU;
            }

            if (type > 0) {
                group = new NNPacker[5];
                isGroup = false;
                submit(list, 0, 0, 5, new NNPacker[5], type);//获取特殊牌 一个组合
                pai = group;
            }

            if (pai[0] != null && pai[1] != null && pai[2] != null && pai[3] != null && pai[4] != null) {
                for (NNPacker n : pai) {
                    list.remove(n);//移除已发的牌
                }
                room.getUserPacketMap().get(uuidList.get(i)).setPs(pai);//保存牌
            } else {
                userList.add(uuidList.get(i));//没有概率的玩家
            }
        }

        for (String account : userList) {//没有概率的玩家发牌
            pai = new NNPacker[5];
            for (int i = 0; i < pai.length; i++) {
                pai[i] = list.get(0);
                list.remove(0);//移除已发的牌
            }
            room.getUserPacketMap().get(account).setPs(pai);//保存牌
        }

    }


    /**
     * 获取概率出现特色牌
     *
     * @param a
     * @param c
     * @param i
     * @param n
     * @param b
     * @param type
     */
    private static void submit(List<NNPacker> a, int c, int i, int n, NNPacker[] b, int type) {
        if (isGroup) return;//结束获取组合
        for (int j = c; j < a.size() - (n - 1); j++) {
            b[i] = a.get(j);
            if (n == 1) {
                NNPacker[] m = new NNPacker[5];
                m[0] = b[0];
                m[1] = b[1];
                m[2] = b[2];
                m[3] = b[3];
                m[4] = b[4];
                NNUserPacket nnUserPacket = new NNUserPacket();
                nnUserPacket.setPs(m);
                boolean is = false;
                switch (type) {
                    case NN.SPECIALTYPE_TONGHUASHUNZINIU://同花顺牛
                        is = nnUserPacket.isTongHuaShunZi();
                        break;
                    case NN.SPECIALTYPE_WUXIAONIU://五小牛
                        is = nnUserPacket.isWuXiaoNiu();
                        break;
                    case NN.SPECIALTYPE_ZHADANNIU://炸弹牛
                        is = nnUserPacket.isSiZha();
                        break;
                    case NN.SPECIALTYPE_HULUNIU://葫芦牛
                        is = nnUserPacket.isHuLuNiu();
                        break;
                    case NN.SPECIALTYPE_TONGHUANIU://同花牛
                        is = nnUserPacket.isTongHua();
                        break;
                    case NN.SPECIALTYPE_WUHUANIU://五花牛
                        is = nnUserPacket.isWuHuaNiu();
                        break;
                    case NN.SPECIALTYPE_SHUNZINIU://顺子牛
                        is = nnUserPacket.isShunZi();
                        break;
                    default:
                }

                if (is) {
                    group = m;
                    isGroup = true;//获取一次就 退出循环
                }
            } else {
                n--;
                i++;
                submit(a, j + 1, i, n, b, type);//递归调用
                n++;//还原n,i的值
                i--;
            }
        }
    }

    public static void main(String[] args) {
        List<NNPacker> a = new ArrayList<>();

        a.add(new NNPacker(NNNum.P_A, NNColor.HEITAO));
        a.add(new NNPacker(NNNum.P_2, NNColor.HEITAO));
        a.add(new NNPacker(NNNum.P_3, NNColor.HEITAO));
        a.add(new NNPacker(NNNum.P_4, NNColor.HEITAO));
        a.add(new NNPacker(NNNum.P_5, NNColor.HEITAO));
        a.add(new NNPacker(NNNum.P_6, NNColor.HEITAO));
        a.add(new NNPacker(NNNum.P_7, NNColor.HEITAO));
        a.add(new NNPacker(NNNum.P_8, NNColor.HEITAO));
        a.add(new NNPacker(NNNum.P_9, NNColor.HEITAO));
        a.add(new NNPacker(NNNum.P_10, NNColor.HEITAO));
        a.add(new NNPacker(NNNum.P_J, NNColor.HEITAO));
        a.add(new NNPacker(NNNum.P_Q, NNColor.HEITAO));
        a.add(new NNPacker(NNNum.P_K, NNColor.HEITAO));
        // 红桃
        a.add(new NNPacker(NNNum.P_A, NNColor.HONGTAO));
        a.add(new NNPacker(NNNum.P_2, NNColor.HONGTAO));
        a.add(new NNPacker(NNNum.P_3, NNColor.HONGTAO));
        a.add(new NNPacker(NNNum.P_4, NNColor.HONGTAO));
        a.add(new NNPacker(NNNum.P_5, NNColor.HONGTAO));
        a.add(new NNPacker(NNNum.P_6, NNColor.HONGTAO));
        a.add(new NNPacker(NNNum.P_7, NNColor.HONGTAO));
        a.add(new NNPacker(NNNum.P_8, NNColor.HONGTAO));
        a.add(new NNPacker(NNNum.P_9, NNColor.HONGTAO));
        a.add(new NNPacker(NNNum.P_10, NNColor.HONGTAO));
        a.add(new NNPacker(NNNum.P_J, NNColor.HONGTAO));
        a.add(new NNPacker(NNNum.P_Q, NNColor.HONGTAO));
        a.add(new NNPacker(NNNum.P_K, NNColor.HONGTAO));
        // 梅花
        a.add(new NNPacker(NNNum.P_A, NNColor.MEIHAU));
        a.add(new NNPacker(NNNum.P_2, NNColor.MEIHAU));
        a.add(new NNPacker(NNNum.P_3, NNColor.MEIHAU));
        a.add(new NNPacker(NNNum.P_4, NNColor.MEIHAU));
        a.add(new NNPacker(NNNum.P_5, NNColor.MEIHAU));
        a.add(new NNPacker(NNNum.P_6, NNColor.MEIHAU));
        a.add(new NNPacker(NNNum.P_7, NNColor.MEIHAU));
        a.add(new NNPacker(NNNum.P_8, NNColor.MEIHAU));
        a.add(new NNPacker(NNNum.P_9, NNColor.MEIHAU));
        a.add(new NNPacker(NNNum.P_10, NNColor.MEIHAU));
        a.add(new NNPacker(NNNum.P_J, NNColor.MEIHAU));
        a.add(new NNPacker(NNNum.P_Q, NNColor.MEIHAU));
        a.add(new NNPacker(NNNum.P_K, NNColor.MEIHAU));
        // 方块
        a.add(new NNPacker(NNNum.P_A, NNColor.FANGKUAI));
        a.add(new NNPacker(NNNum.P_2, NNColor.FANGKUAI));
        a.add(new NNPacker(NNNum.P_3, NNColor.FANGKUAI));
        a.add(new NNPacker(NNNum.P_4, NNColor.FANGKUAI));
        a.add(new NNPacker(NNNum.P_5, NNColor.FANGKUAI));
        a.add(new NNPacker(NNNum.P_6, NNColor.FANGKUAI));
        a.add(new NNPacker(NNNum.P_7, NNColor.FANGKUAI));
        a.add(new NNPacker(NNNum.P_8, NNColor.FANGKUAI));
        a.add(new NNPacker(NNNum.P_9, NNColor.FANGKUAI));
        a.add(new NNPacker(NNNum.P_10, NNColor.FANGKUAI));
        a.add(new NNPacker(NNNum.P_J, NNColor.FANGKUAI));
        a.add(new NNPacker(NNNum.P_Q, NNColor.FANGKUAI));
        a.add(new NNPacker(NNNum.P_K, NNColor.FANGKUAI));

        Collections.shuffle(a);
        submit(a, 0, 0, 5, new NNPacker[5], NN.SPECIALTYPE_TONGHUASHUNZINIU);
        System.out.println(group.toString());
    }

    /**
     * 概率 是否 出现特色牌
     *
     * @param random 几率
     * @param points 几分 比如  1/100  random=1  points 100
     * @return
     */
    private static boolean isRandomSpecial(int random, int points) {
        if (random < 0 || points < random) return false;
        int b = (int) (Math.random() * points) + 1;
        return b <= random;
    }

    /**
     * 发牌
     *
     * @param roomNo
     */
    public static void faPai(String roomNo) {

        NNGameRoomNew room = ((NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo));
        // 返回玩家手牌
        List<NNPacker[]> userPackets = NN.faPai(room.getPai(), room.getPlayerCount());
        List<String> uuidList = new ArrayList<>();
        for (String uuid : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
                if (room.getUserPacketMap().get(uuid).getStatus() > NNConstant.NN_USER_STATUS_INIT) {
                    uuidList.add(uuid);
                }
            }
        }
        // 遍历玩家列表
        for (int i = 0; i < uuidList.size(); i++) {
            ((NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(uuidList.get(i)).setPs(userPackets.get(i));
        }
    }

}
