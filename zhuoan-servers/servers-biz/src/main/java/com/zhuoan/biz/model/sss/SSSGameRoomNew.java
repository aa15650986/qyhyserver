package com.zhuoan.biz.model.sss;


import com.zhuoan.biz.core.sss.SSSComputeCards;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.SSSConstant;
import com.zhuoan.service.impl.QTHServiceImpl;
import com.zhuoan.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;


public class SSSGameRoomNew extends GameRoom implements Serializable {


	
	
	
    private int color;

    private String maPai;

    private String maPaiAccount = "-1";


    private int maPaiIndex = -1;


    private int bankerType;

    private int swat = 0;

    private ConcurrentHashMap<String, SSSUserPacket> userPacketMap = new ConcurrentHashMap<>();

    private int maPaiType = 0;

    private List<String> pai;

    private int compareTimer = 0;

    private JSONArray dqArray = new JSONArray();

    private String baseNum;


    private String fire;

    private int fireScore;

    private int pattern;

    private String feature;

    private List<JSONArray> gameRecords = new ArrayList<>();

    private String speedMode = SSSConstant.SSS_SPEED_MODE_NO;

    private QTHServiceImpl impl;
    
    private String cutCardAccount = "-1";

    private String cutCardMode = SSSConstant.SSS_CUT_CARD_MODE_NO;

    private int hardGhostNumber = 0;

    private boolean isResetTime = false;
    
    private int ruanguiCount = 0;
    

    public QTHServiceImpl getImpl() {
		return impl;
	}

	public void setImpl(QTHServiceImpl impl) {
		this.impl = impl;
	}

	public int getRuanguiCount() {
		return ruanguiCount;
	}

	public void setRuanguiCount(int ruanguiCount) {
		this.ruanguiCount = ruanguiCount;
	}

	public boolean isResetTime() {
        return isResetTime;
    }

    public void setResetTime(boolean resetTime) {
        isResetTime = resetTime;
    }

    public int getHardGhostNumber() {
        return hardGhostNumber;
    }

    public void setHardGhostNumber(int hardGhostNumber) {
        this.hardGhostNumber = hardGhostNumber;
    }

    public String getCutCardMode() {
        return cutCardMode;
    }

    public void setCutCardMode(String cutCardMode) {
        this.cutCardMode = cutCardMode;
    }

    public String getCutCardAccount() {
        return cutCardAccount;
    }

    public void setCutCardAccount(String cutCardAccount) {
        this.cutCardAccount = cutCardAccount;
    }

    public String getSpeedMode() {
        return speedMode;
    }

    public void setSpeedMode(String speedMode) {
        this.speedMode = speedMode;
    }

    public List<JSONArray> getGameRecords() {
        return gameRecords;
    }

    public void setGameRecords(List<JSONArray> gameRecords) {
        this.gameRecords = gameRecords;
    }

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public int getPattern() {
        return pattern;
    }

    public void setPattern(int pattern) {
        this.pattern = pattern;
    }

    public int getFireScore() {
        return fireScore;
    }

    public void setFireScore(int fireScore) {
        this.fireScore = fireScore;
    }

    public String getFire() {
        return fire;
    }

    public void setFire(String fire) {
        this.fire = fire;
    }

    public JSONArray getDqArray() {
        return dqArray;
    }

    public void setDqArray(JSONArray dqArray) {
        this.dqArray = dqArray;
    }

    public int getCompareTimer() {
        return compareTimer;
    }

    public void setCompareTimer(int compareTimer) {
        this.compareTimer = compareTimer;
    }

    public int getSwat() {
        return swat;
    }

    public void setSwat(int swat) {
        this.swat = swat;
    }

    public List<String> getPai() {
        return pai;
    }

    public void setPai(List<String> pai) {
        this.pai = pai;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getMaPai() {
        return maPai;
    }

    public String getMaPaiAccount() {
        return maPaiAccount;
    }

    public void setMaPaiAccount(String maPaiAccount) {
        this.maPaiAccount = maPaiAccount;
    }

    public int getMaPaiIndex() {
        return maPaiIndex;
    }

    public void setMaPaiIndex(int maPaiIndex) {
        this.maPaiIndex = maPaiIndex;
    }

    public void setMaPai(String maPai) {
        this.maPai = maPai;
    }

    public int getBankerType() {
        return bankerType;
    }

    public void setBankerType(int bankerType) {
        this.bankerType = bankerType;
    }

    public ConcurrentHashMap<String, SSSUserPacket> getUserPacketMap() {
        return userPacketMap;
    }

    public void setUserPacketMap(ConcurrentHashMap<String, SSSUserPacket> userPacketMap) {
        this.userPacketMap = userPacketMap;
    }

    public int getMaPaiType() {
        return maPaiType;
    }

    public void setMaPaiType(int maPaiType) {
        this.maPaiType = maPaiType;
    }

    public String getBaseNum() {
        return baseNum;
    }

    public void setBaseNum(String baseNum) {
        this.baseNum = baseNum;
    }


    public void clearGameRecord() {
        gameRecords = new ArrayList<>();
    }


    public JSONArray getAllPlayer() {
        JSONArray array = new JSONArray();

        for (String uuid : getUserPacketMap().keySet()) {
            Playerinfo player = getPlayerMap().get(uuid);
            //玩家不存在
            if (null == player) continue;

            JSONObject obj = new JSONObject();
            obj.put("account", player.getAccount());
            obj.put("isCollapse", player.isCollapse());//玩家是否破产
            obj.put("name", player.getName());
            obj.put("headimg", player.getRealHeadimg());
            obj.put("sex", player.getSex());
            obj.put("ip", player.getIp());
            obj.put("vip", player.getVip());
            obj.put("location", player.getLocation());
            obj.put("area", player.getArea());
            obj.put("score", Dto.add(player.getScore(), player.getSourceScore()));//玩家分数
            obj.put("index", player.getMyIndex());
            obj.put("userOnlineStatus", player.getStatus());
            obj.put("ghName", player.getGhName());
            obj.put("introduction", player.getSignature());
            obj.put("userStatus", userPacketMap.get(uuid).getStatus());
            obj.put("userTimeLeft", userPacketMap.get(uuid).getTimeLeft());
            array.add(obj);
        }
        return array;
    }


    public JSONObject obtainGameData() {
        JSONObject gameData = new JSONObject();
        gameData.put("showIndex", obtainShowIndex());
        gameData.put("dq", getDqArray());
        gameData.put("data", obtainData());
        return gameData;
    }


    public JSONArray obtainData() {
        JSONArray data = new JSONArray();
        if (getGameStatus() > SSSConstant.SSS_GAME_STATUS_GAME_EVENT) {
            // 获取所有参与玩家得分情况
            for (String account : getUserPacketMap().keySet()) {
                if (getUserPacketMap().containsKey(account) && getUserPacketMap().get(account) != null) {
                    if (getUserPacketMap().get(account).getStatus() != SSSConstant.SSS_USER_STATUS_INIT) {
                        JSONObject userData = new JSONObject();
                        userData.put("index", getPlayerMap().get(account).getMyIndex());
                        userData.put("paiType", getUserPacketMap().get(account).getPaiType());
                        userData.put("havema", 0);
                        JSONArray userResult = new JSONArray();
                        userResult.add(getUserPacketMap().get(account).getHeadResult());
                        userResult.add(getUserPacketMap().get(account).getMidResult());
                        userResult.add(getUserPacketMap().get(account).getFootResult());
                        userData.put("result", userResult);
                        userData.put("sum", getUserPacketMap().get(account).getScore());
                        userData.put("account", account);
                        double scoreLeft;
                        if (getGameStatus() == SSSConstant.SSS_GAME_STATUS_COMPARE && getRoomType() != CommonConstant.ROOM_TYPE_FK
                                && getRoomType() != CommonConstant.ROOM_TYPE_COMPETITIVE && getRoomType() != CommonConstant.ROOM_TYPE_DK
                                && getRoomType() != CommonConstant.ROOM_TYPE_CLUB) {
                            scoreLeft = Dto.add(getPlayerMap().get(account).getScore(), getUserPacketMap().get(account).getScore());
                            scoreLeft = Dto.add(scoreLeft, getPlayerMap().get(account).getSourceScore());
                        } else {
                            scoreLeft = getPlayerMap().get(account).getScore();
                        }
                        userData.put("scoreLeft", scoreLeft);
                        userData.put("isQld", getUserPacketMap().get(account).getSwat());
                        data.add(userData);
                    }
                }
            }
        }
        return data;
    }


    public JSONArray obtainShowIndex() {
        JSONArray showIndex = new JSONArray();
        if (getGameStatus() == SSSConstant.SSS_GAME_STATUS_XZ) {
            return showIndex;
        }
        if (getGameStatus() > SSSConstant.SSS_GAME_STATUS_GAME_EVENT && getGameStatus() != SSSConstant.SSS_GAME_STATUS_FINAL_SUMMARY) {
            Map<String, Double> headMap = new HashMap<String, Double>();
            Map<String, Double> midMap = new HashMap<String, Double>();
            Map<String, Double> footMap = new HashMap<String, Double>();
            // 获取所有参与玩家得分情况
            for (String account : getPlayerMap().keySet()) {
                if (getUserPacketMap().containsKey(account) && getUserPacketMap().get(account) != null) {
                    if (getUserPacketMap().get(account).getStatus() != SSSConstant.SSS_USER_STATUS_INIT) {
                        if (getUserPacketMap().get(account).getPaiType() == 0) {
                            headMap.put(account, getUserPacketMap().get(account).getHeadResult().getDouble("score"));
                            midMap.put(account, getUserPacketMap().get(account).getMidResult().getDouble("score"));
                            footMap.put(account, getUserPacketMap().get(account).getFootResult().getDouble("score"));
                        }
                    }
                }
            }
            showIndex.add(sortUserScore(headMap));
            showIndex.add(sortUserScore(midMap));
            showIndex.add(sortUserScore(footMap));
        }
        return showIndex;
    }


    public JSONArray sortUserScore(Map<String, Double> map) {
        Set<Map.Entry<String, Double>> entry = map.entrySet();
        LinkedList<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(entry);
        Collections.sort(list, new Comparator<Entry<String, Double>>() {
            @Override
            public int compare(Entry<String, Double> o1,
                               Entry<String, Double> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        JSONArray array = new JSONArray();
        for (Entry<String, Double> e : list) {
            array.add(getPlayerMap().get(e.getKey()).getMyIndex());
        }

        return array;
    }


    public void initGame() {
        addGameIndex();
        addGameNewIndex();
        // 清空游戏记录
        getGameProcess().clear();
        // 清空切牌玩家
        cutCardAccount = "-1";
        // 清空比牌时间
        compareTimer = 0;
        // 清空打枪记录
        getDqArray().clear();
        // 全垒打清零
        swat = 0;
        // 霸王庄换庄
        if (getBankerType() == SSSConstant.SSS_BANKER_TYPE_BWZ) {
            if (!userPacketMap.containsKey(getBanker()) || userPacketMap.get(getBanker()) == null) {
                // 换庄
                for (String newBanker : getUserPacketMap().keySet()) {
                    if (getUserPacketMap().containsKey(newBanker) && getUserPacketMap().get(newBanker) != null) {
                        setBanker(newBanker);
                    }
                }
            }
        }
        // 初始化用户信息
        for (String uuid : getUserPacketMap().keySet()) {
            Playerinfo playerinfo = getPlayerMap().get(uuid);
            SSSUserPacket player = getUserPacketMap().get(uuid);
            if (null == playerinfo || null == player) continue;

            player.initUserPacket();
            playerinfo.setCutScore(0D);//抽水分数
            playerinfo.addPlayTimes();//添加游戏局数
            if (playerinfo.isCollapse()) player.setStatus(0);//破产玩家准备处于初始状态

        }
        getSummaryData().clear();
        setIsClose(CommonConstant.CLOSE_ROOM_TYPE_INIT);
        //马牌玩家
        setMaPaiAccount("-1");
        setMaPaiIndex(-1);
    }


    public int obtainNotSpecialCount() {
        int notSpecialCount = 0;
        for (String account : getUserPacketMap().keySet()) {
            if (getUserPacketMap().containsKey(account) && getUserPacketMap().get(account) != null) {
                if (getUserPacketMap().get(account).getStatus() != SSSConstant.SSS_USER_STATUS_INIT) {
                    if (getUserPacketMap().get(account).getPaiType() == 0) {
                        notSpecialCount++;
                    }
                }
            }
        }
        return notSpecialCount;
    }



    public void shufflePai(int perNum, int mode, String feature, String sameColor, String flowerColor, int hardGhostNumber) {

        // 定义一个花色数组 1黑桃，2红桃，3梅花，4方块
        String[] colors = {"1-", "2-", "3-", "4-"};
        //判断花色
        if (SSSConstant.SSS_SAME_COLOR_YES.equals(sameColor)) {//清一色
            String color;
            if (SSSConstant.SSS_FLOWER_COLOR_HEITAO.equals(flowerColor)) {
                color = "1-";
            } else if (SSSConstant.SSS_FLOWER_COLOR_TAOHUA.equals(flowerColor)) {
                color = "2-";
            } else if (SSSConstant.SSS_FLOWER_COLOR_MEIHUA.equals(flowerColor)) {
                color = "3-";
            } else if (SSSConstant.SSS_FLOWER_COLOR_FANGKUAI.equals(flowerColor)) {
                color = "4-";
            } else {
                Random random = new Random();
                int i = random.nextInt(4) + 1;
                color = i + "-";
            }
            colors = new String[]{color, color, color, color};
        }
        // 定义一个点数数组
        String[] numbers = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12",
                "13", "1"};

        List<String> pai = new ArrayList<>();
        // 黑桃
        List<String> oneFlower = new ArrayList<>();
        // 红心
        List<String> twoFlower = new ArrayList<>();
        //梅花
        List<String> threeFlower = new ArrayList<>();
        //方块
        List<String> FourFlower = new ArrayList<>();
        for (String number : numbers) {
            for (String color : colors) {
                String poker = color.concat(number);
                pai.add(poker);
                if ("1-".equals(color)) {
                    oneFlower.add(poker);
                } else if ("2-".equals(color)) {
                    twoFlower.add(poker);
                } else if ("3-".equals(color)) {
                    threeFlower.add(poker);
                } else if ("4-".equals(color)) {
                    FourFlower.add(poker);
                }
            }
        }
        if (perNum < 5 && mode > 0) {
            if (mode == 1) {
                pai.addAll(oneFlower);
            } else if (mode == 2) {
                pai.addAll(oneFlower);
                pai.addAll(twoFlower);
            }
        } else if (perNum == 5) {
            pai.addAll(oneFlower);
            if (mode == 1) {
                pai.addAll(twoFlower);
            } else if (mode == 2) {
                pai.addAll(twoFlower);
                pai.addAll(threeFlower);
            }
        } else if (perNum == 6) {
            pai.addAll(oneFlower);
            pai.addAll(twoFlower);
            if (mode == 1) {
                pai.addAll(threeFlower);
            } else if (mode == 2) {
                pai.addAll(threeFlower);
                pai.addAll(FourFlower);
            }
        } else if (perNum == 7) {
            pai.addAll(oneFlower);
            pai.addAll(twoFlower);
            pai.addAll(threeFlower);
            if (mode == 1) {
                pai.addAll(FourFlower);
            } else if (mode == 2) {
                pai.addAll(FourFlower);
                pai.addAll(oneFlower);
            }
        } else if (perNum == 8) {
            pai.addAll(oneFlower);
            pai.addAll(twoFlower);
            pai.addAll(threeFlower);
            pai.addAll(FourFlower);
            if (mode == 1) {
                pai.addAll(oneFlower);
            } else if (mode == 2) {
                pai.addAll(oneFlower);
                pai.addAll(twoFlower);
            }
        }
        //癞子玩法加癞子
        if (SSSConstant.SSS_FEATURE_LAIZI.equals(feature)) {
            //大小王
            List<String> kingPai = new ArrayList<>();
            kingPai.add("5-0");//小王
            kingPai.add("5-1");//大王
            int i = (perNum - 1) / 4 + 1;
            for (int j = 0; j < i; j++) {
                pai.addAll(kingPai);
            }
        } else if (SSSConstant.SSS_FEATURE_LAIZI_ONE.equals(feature)) {
            pai.add("5-1");//大王
        } else if (SSSConstant.SSS_FEATURE_LAIZI_TWO.equals(feature)) {
            pai.add("5-0");//小王
            pai.add("5-1");//大王
        }
        // 硬鬼数量
        if (hardGhostNumber > 0) {
            for (int i = 0; i < hardGhostNumber; i++) {
                if (i % 2 == 0) {
                    pai.add("5-1");//大王
                } else {
                    pai.add("5-0");//小王
                }
            }
        }
        // 洗牌
        Collections.shuffle(pai);
        setPai(pai);
    }


    public static String[] AppointSort(String[] list, int i1, int j1) {
        //如果是癞子则直接返回不进行排序
        boolean isLaiZi = false;
        for (String s : list) {
            if ("5".equals(s.split("-")[0])) {
                isLaiZi = true;
                break;
            }
        }
        if (isLaiZi) return list;
        String[] a = new String[j1 - i1 + 1];
        int h = i1;
        for (int i = 0; i < a.length; i++) {
            if (i1 <= j1) {
                a[i] = list[h];
                h++;
            }
        }
        JSONArray arr = new JSONArray();
        for (int i = 0; i < a.length; i++) {
            arr.add(getValue(a[i]));
        }
        // 判断是否按对子以上排序 start
        JSONArray aa = JSONArray.fromObject(a);
        boolean isPair = true;
        if (5 == SSSComputeCards.isSameFlower(aa)) {
            isPair = false;
        }
        // 判断是否按对子以上排序 end
        JSONArray array = new JSONArray();
        JSONArray array1 = new JSONArray();
        JSONArray array2 = new JSONArray();
        JSONArray array3 = new JSONArray();
        JSONArray array4 = new JSONArray();

        for (int i = 1; i < 14; i++) {
            int count = 0;
            for (int t = 0; t < arr.size(); t++) {
                if (arr.getInt(t) == i) {
                    if (isPair) {
                        count++;
                    }
                }
            }
            if (count == 1) {
                array1.add(i);
            }
            if (count == 2) {
                array2.add(i);
            }
            if (count == 3) {
                array3.add(i);
            }
            if (count == 4) {
                array4.add(i);
            }
        }
        array.add(array1);
        array.add(array2);
        array.add(array3);
        array.add(array4);

        JSONArray array22 = new JSONArray();
        for (int i = array.size() - 1; i >= 0; i--) {
            JSONArray jsona = array.getJSONArray(i);
            for (int ii = 0; ii < jsona.size() - 1; ii++) {
                for (int jj = ii + 1; jj < jsona.size(); jj++) {
                    int a1 = jsona.getInt(ii) == 1 ? 14 : jsona.getInt(ii);
                    int a2 = jsona.getInt(jj) == 1 ? 14 : jsona.getInt(jj);
                    // 这里是从大到小排序，如果是从小到大排序，只需将“<”换成“>”
                    if (a1 > a2) {
                        int temp = jsona.getInt(ii);
                        jsona.element(ii, jsona.getInt(jj));
                        jsona.element(jj, temp);

                    }
                }
            }
            for (int j = jsona.size() - 1; j >= 0; j--) {
                for (int j2 = 0; j2 < a.length; j2++) {
                    if (jsona.getInt(j) == getValue(a[j2])) {
                        array22.add(a[j2]);

                    }
                }
            }
        }
        for (int i = 0; i < array22.size(); i++) {
            if (i1 <= j1) {
                list[i1] = array22.getString(i);
                i1++;
            }

        }
        return list;
    }

    public static int getValue(String card) {
        int i = Integer.parseInt(card.substring(2, card.length()));
        return i;
    }


    public int getValueWithColor(String card) {
        String[] cards = card.split("-");
        int value = Integer.valueOf(cards[1]);
        if (cards[0].equals("2")) {
            return value + 20;
        } else if (cards[0].equals("3")) {
            return value + 40;
        } else if (cards[0].equals("4")) {
            return value + 60;
        } else if (cards[0].equals("5")) {
            return value + 80;
        }
        return value;
    }


    public void changePlayerPai(String[] myPai, String account) {
        int[] headPai = new int[3];
        for (int i = 0; i < 3; i++) {
            headPai[i] = getValueWithColor(myPai[i]);
        }
        getUserPacketMap().get(account).setHeadPai(headPai);
        int[] midPai = new int[5];
        for (int i = 3; i < 8; i++) {
            midPai[i - 3] = getValueWithColor(myPai[i]);
        }
        getUserPacketMap().get(account).setMidPai(midPai);
        int[] footPai = new int[5];
        for (int i = 8; i < 13; i++) {
            footPai[i - 8] = getValueWithColor(myPai[i]);
        }
        getUserPacketMap().get(account).setFootPai(footPai);
    }


    public int getNowReadyCount() {
        int readyCount = 0;
        for (String account : getPlayerMap().keySet()) {
            Playerinfo playerinfo = getPlayerMap().get(account);
            SSSUserPacket up = getUserPacketMap().get(account);
            if (null == playerinfo || null == up || playerinfo.isCollapse()) continue;//破产玩家不考虑进去
            if (SSSConstant.SSS_USER_STATUS_READY == up.getStatus()) {
                readyCount++;
            }
        }
        return readyCount;
    }


    public boolean isAllReady() {
        for (String account : getPlayerMap().keySet()) {
            Playerinfo playerinfo = getPlayerMap().get(account);
            SSSUserPacket up = getUserPacketMap().get(account);
            if (null == playerinfo || null == up) continue;
            if (playerinfo.isCollapse()) continue; //破产玩家不考虑进去
            if (SSSConstant.SSS_USER_STATUS_READY != up.getStatus()) {
                return false;
            }
        }
        return true;
    }

    public boolean isAllFinish() {
        for (String account : getPlayerMap().keySet()) {
            Playerinfo playerinfo = getPlayerMap().get(account);
            SSSUserPacket up = getUserPacketMap().get(account);
            if (null == playerinfo || null == up) continue;
            if (playerinfo.isCollapse()) continue; //破产玩家不考虑进去

            if (SSSConstant.SSS_USER_STATUS_INIT != up.getStatus() && SSSConstant.SSS_USER_STATUS_GAME_EVENT != up.getStatus()) {
                return false;
            }
        }
        return true;
    }


    public static String[] sortPaiDesc(String[] list) {

        for (int i = 0; i < list.length; i++) {
            if (getValue(list[i]) == 1 && !"5".equals(list[i].split("-")[0])) {
                list[i] = list[i].replace("-1", "-14");
            } else if ("5".equals(list[i].split("-")[0])) {
                if (getValue(list[i]) == 0) {
                    list[i] = list[i].replace("-0", "-15");
                } else {
                    list[i] = list[i].replace("-1", "-16");
                }
            }
        }


        for (int i = 0; i < list.length - 1; i++) {
            for (int j = i + 1; j < list.length; j++) {
                if (getValue(list[i]) > getValue(list[j])) {
                    String o = list[i];
                    String o1 = list[j];
                    list[i] = o1;
                    list[j] = o;
                }
            }
        }

        for (int i = 0; i < list.length; i++) {
            if (getValue(list[i]) == 14) {
                list[i] = list[i].replace("-14", "-1");
            }
            if ("5".equals(list[i].split("-")[0])) {
                if (getValue(list[i]) == 15) {
                    list[i] = list[i].replace("-15", "-0");
                } else {
                    list[i] = list[i].replace("-16", "-1");
                }
            }
        }
        
        String[] dd = new String[13];
        int p = 0;
        //倒序
        for (int i = list.length - 1; i > -1; i--) {
            dd[p] = list[i];
            p++;
        }
        return dd;
    }

    public JSONArray obtainFinalSummaryData() {

        JSONArray array = new JSONArray();
        double max = 0D;
        for (String account : userPacketMap.keySet()) {
            if (getPlayerMap().get(account).getScore() > max) max = getPlayerMap().get(account).getScore();
        }

        for (String account : getPlayerMap().keySet()) {
            SSSUserPacket player = getUserPacketMap().get(account);
            Playerinfo playerinfo = getPlayerMap().get(account);
            if (null == player || null == playerinfo) continue;
            if (!playerinfo.isCollapse() && SSSConstant.SSS_USER_STATUS_INIT == player.getStatus())
                continue;

            JSONObject obj = new JSONObject();
            obj.put("name", playerinfo.getName());
            obj.put("account", account);
            obj.put("headimg", playerinfo.getRealHeadimg());

            obj.put("isFangzhu", CommonConstant.GLOBAL_NO);
            if (account.equals(getOwner())) {
                obj.put("isFangzhu", CommonConstant.GLOBAL_YES);
            }
            int isWinner = CommonConstant.GLOBAL_NO;

            double score = playerinfo.getScore();
            if (score == max && score > 0D) {
                isWinner = CommonConstant.GLOBAL_YES;
            }
            obj.put("isWinner", isWinner);
            obj.put("score", score);
            obj.put("cutType", getCutType());//消耗类型
            obj.put("cutScore", playerinfo.getCutScore());

            obj.put("winTimes", player.getWinTimes());
            obj.put("dqTimes", player.getDqTimes());
            obj.put("bdqTimes", player.getBdqTimes());
            obj.put("qldTimes", player.getSwatTimes());
            obj.put("specialTimes", player.getSpecialTimes());
            obj.put("ordinaryTimes", player.getOrdinaryTimes());
            array.add(obj);
        }
        Collections.sort(array, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject o1, JSONObject o2) {
                if (o1.getDouble("score") > o2.getDouble("score")) return -1;
                return 1;
            }
        });
        return array;
    }

    public int getCollapseNum() {
        int count = 0;
        for (String account : getPlayerMap().keySet()) {
            Playerinfo playerinfo = getPlayerMap().get(account);
            if (null == playerinfo) continue;
            if (playerinfo.isCollapse()) continue;
            count++;
        }
        return count;
    }


    public boolean isAgreeClose() {
        for (String account : getPlayerMap().keySet()) {
            Playerinfo playerinfo = getPlayerMap().get(account);
            if (null == playerinfo) continue;
            if (CommonConstant.CLOSE_ROOM_AGREE != playerinfo.getIsCloseRoom()) {
                return false;
            }
        }
        return true;
    }


    public boolean allNoClose() {
        for (String account : getPlayerMap().keySet()) {
            Playerinfo playerinfo = getPlayerMap().get(account);
            if (null == playerinfo) continue;
            if (CommonConstant.CLOSE_ROOM_UNSURE != playerinfo.getIsCloseRoom()) {
                return false;
            }
        }
        return true;
    }


    public JSONArray getJieSanData() {
        JSONArray array = new JSONArray();
        for (String account : getPlayerMap().keySet()) {
            Playerinfo playerinfo = getPlayerMap().get(account);
            if (null == playerinfo) continue;

            JSONObject obj = new JSONObject();
            obj.put("index", playerinfo.getMyIndex());
            obj.put("name", playerinfo.getName());
            obj.put("result", playerinfo.getIsCloseRoom());
            obj.put("jiesanTimer", getJieSanTime());
            array.add(obj);
        }
        return array;
    }

    public JSONArray getBaseNumTimes(double yuanbao) {

        // 最大下注倍数
        JSONArray baseNums = new JSONArray();
        JSONArray array = JSONArray.fromObject(getBaseNum());
        for (int i = 0; i < array.size(); i++) {
            int val = array.getJSONObject(i).getInt("val");
            JSONObject obj = new JSONObject();
            obj.put("name", new StringBuffer().append(String.valueOf(val)).append("倍").toString());
            obj.put("val", val);
            if (yuanbao >= val * SSSConstant.SSS_XZ_BASE_NUM || getRoomType() == CommonConstant.ROOM_TYPE_FK
                    || getRoomType() == CommonConstant.ROOM_TYPE_DK || getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
                obj.put("isuse", CommonConstant.GLOBAL_YES);
            } else {
                obj.put("isuse", CommonConstant.GLOBAL_NO);
            }
            baseNums.add(obj);
        }

        return JSONArray.fromObject(baseNums);
    }


    public boolean isAllXiaZhu() {
        for (String account : getPlayerMap().keySet()) {
            SSSUserPacket up = getUserPacketMap().get(account);
            if (null == up) continue;
            if (!account.equals(getBanker())) {
                if (SSSConstant.SSS_USER_STATUS_INIT != up.getStatus() && SSSConstant.SSS_USER_STATUS_XZ != up.getStatus()) {
                    return false;
                }
            }
        }
        return true;
    }


    public JSONArray obtainXzResult() {
        JSONArray array = new JSONArray();
        for (String account : getPlayerMap().keySet()) {
            SSSUserPacket up = getUserPacketMap().get(account);
            Playerinfo playerinfo = getPlayerMap().get(account);
            if (null == up || null == playerinfo) continue;

            if (up.getXzTimes() > 0) {
                JSONObject obj = new JSONObject();
                obj.put("index", playerinfo.getMyIndex());
                obj.put("value", up.getXzTimes());
                obj.put("result", CommonConstant.GLOBAL_YES);
                array.add(obj);
            }
        }
        return array;
    }

	@Override
	public String toString() {
		return "SSSGameRoomNew [color=" + color + ", maPai=" + maPai + ", maPaiAccount=" + maPaiAccount
				+ ", maPaiIndex=" + maPaiIndex + ", bankerType=" + bankerType + ", swat=" + swat + ", userPacketMap="
				+ userPacketMap + ", maPaiType=" + maPaiType + ", pai=" + pai + ", compareTimer=" + compareTimer
				+ ", dqArray=" + dqArray + ", baseNum=" + baseNum + ", fire=" + fire + ", fireScore=" + fireScore
				+ ", pattern=" + pattern + ", feature=" + feature + ", gameRecords=" + gameRecords + ", speedMode="
				+ speedMode + ", impl=" + impl + ", cutCardAccount=" + cutCardAccount + ", cutCardMode=" + cutCardMode
				+ ", hardGhostNumber=" + hardGhostNumber + ", isResetTime=" + isResetTime + ", ruanguiCount="
				+ ruanguiCount + "]"+super.toString();
	}

    
    
	/*
	 * @Override public String toString() {
	 * 
	 * return "局数限制："+getGameCount()+"========当前："+getGameIndex();
	 * 
	 * }
	 */

	
    
    
}
