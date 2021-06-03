
package com.zhuoan.biz.model.zjh;

import com.zhuoan.biz.core.zjh.ZhaJinHuaCore;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.util.Dto;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.stereotype.Component;

@Component
public class ZJHGameRoomNew extends GameRoom implements Serializable {
    private int xzTimer;
    private JSONArray baseNum;
    private int[] pai;
    private int paiIndex;
    private double currentScore;
    private double totalScore;
    private double maxScore;
    private String focus;
    private int gameNum;
    private int totalGameNum;
    private ConcurrentHashMap<String, UserPacket> userPacketMap = new ConcurrentHashMap();
    private List<Integer> yiXiaZhu = new ArrayList();
    private JSONArray xiaZhuList = new JSONArray();
    private int gameType;
    private List<JSONObject> outUserData = new ArrayList();
    private List<JSONObject> processList = new ArrayList();
    private int genzhu;
    private int xzTime;
    private Set<String> giveupUsers = new HashSet<String>();
    
    
    
 

	public Set<String> getGiveupUsers() {
		return giveupUsers;
	}

	public void setGiveupUsers(Set<String> giveupUsers) {
		this.giveupUsers = giveupUsers;
	}

	public int getXzTime() {
		return xzTime;
	}

	public void setXzTime(int xzTime) {
		this.xzTime = xzTime;
	}

	public int getGenzhu() {
		return genzhu;
	}

	public void setGenzhu(int genzhu) {
		this.genzhu = genzhu;
	}

	public ZJHGameRoomNew() {
    }

    public List<JSONObject> getOutUserData() {
        return this.outUserData;
    }

    public void setOutUserData(List<JSONObject> outUserData) {
        this.outUserData = outUserData;
    }

    public int getGameType() {
        return this.gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getXzTimer() {
        return this.xzTimer;
    }

    public void setXzTimer(int xzTimer) {
        this.xzTimer = xzTimer;
    }

    public JSONArray getBaseNum() {
        return this.baseNum;
    }

    public void setBaseNum(JSONArray baseNum) {
        this.baseNum = baseNum;
    }

    public int[] getPai() {
        return this.pai;
    }

    public void setPai(int[] pai) {
        this.pai = pai;
    }

    public int getPaiIndex() {
        return this.paiIndex;
    }

    public void setPaiIndex(int paiIndex) {
        this.paiIndex = paiIndex;
    }

    public double getCurrentScore() {
        return this.currentScore;
    }

    public void setCurrentScore(double currentScore) {
        this.currentScore = currentScore;
    }

    public double getTotalScore() {
        return this.totalScore;
    }

    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }

    public double getMaxScore() {
        return this.maxScore;
    }

    public void setMaxScore(double maxScore) {
        this.maxScore = maxScore;
    }

    public String getFocus() {
        return this.focus;
    }

    public void setFocus(String focus) {
        this.focus = focus;
    }

    public int getGameNum() {
        return this.gameNum;
    }

    public void setGameNum(int gameNum) {
        this.gameNum = gameNum;
    }

    public int getTotalGameNum() {
        return this.totalGameNum;
    }

    public void setTotalGameNum(int totalGameNum) {
        this.totalGameNum = totalGameNum;
    }

    public ConcurrentHashMap<String, UserPacket> getUserPacketMap() {
        return this.userPacketMap;
    }

    public void setUserPacketMap(ConcurrentHashMap<String, UserPacket> userPacketMap) {
        this.userPacketMap = userPacketMap;
    }

    public List<Integer> getYiXiaZhu() {
        return this.yiXiaZhu;
    }

    public void setYiXiaZhu(List<Integer> yiXiaZhu) {
        this.yiXiaZhu = yiXiaZhu;
    }

    public JSONArray getXiaZhuList() {
        return this.xiaZhuList;
    }

    public void setXiaZhuList(JSONArray xiaZhuList) {
        this.xiaZhuList = xiaZhuList;
    }

    public List<JSONObject> getProcessList() {
        return this.processList;
    }

    public void setProcessList(List<JSONObject> processList) {
        this.processList = processList;
    }

    public JSONArray getAllPlayer() {
        JSONArray array = new JSONArray();
        Iterator var2 = this.getPlayerMap().keySet().iterator();

        while(var2.hasNext()) {
            String uuid = (String)var2.next();
            Playerinfo player = (Playerinfo)this.getPlayerMap().get(uuid);
            if (player != null) {
                JSONObject obj = new JSONObject();
                obj.put("account", player.getAccount());
                obj.put("name", player.getName());
                obj.put("headimg", player.getRealHeadimg());
                obj.put("sex", player.getSex());
                obj.put("ip", player.getIp());
                obj.put("vip", player.getVip());
                obj.put("location", player.getLocation());
                obj.put("area", player.getArea());
                obj.put("score", player.getScore());
                obj.put("index", player.getMyIndex());
                obj.put("userOnlineStatus", player.getStatus());
                obj.put("ghName", player.getGhName());
                obj.put("introduction", player.getSignature());
                obj.put("userStatus", ((UserPacket)this.userPacketMap.get(uuid)).getStatus());
                array.add(obj);
            }
        }

        return array;
    }

    public void initGame() {
        this.addGameIndex();
        this.addGameNewIndex();
        this.setIsClose(0);
        this.totalScore = 0.0D;
        this.paiIndex = 0;
        this.currentScore = (double)this.getScore();
        this.gameNum = 1;
        this.setTimeLeft(0);
        this.yiXiaZhu.clear();
        this.xiaZhuList.clear();
        this.outUserData.clear();
        this.processList.clear();
        Iterator var1 = this.getUserPacketMap().keySet().iterator();

        while(var1.hasNext()) {
            String account = (String)var1.next();
            if (this.getUserPacketMap().containsKey(account) && this.getUserPacketMap().get(account) != null) {
                ((UserPacket)this.getUserPacketMap().get(account)).initUserPacket();
                ((Playerinfo)this.getPlayerMap().get(account)).addPlayTimes();
            }
        }

    }

    public void xiPai() {
        int[] pais = ZhaJinHuaCore.PAIS;
        if (this.getGameType() == 2) {
            pais = ZhaJinHuaCore.PAIS_JQ;
        }

        int[] indexs = this.randomPai(pais.length);
        this.pai = new int[pais.length];

        for(int i = 0; i < indexs.length; ++i) {
            this.pai[i] = pais[indexs[i]];
        }

    }

    private int[] randomPai(int paiCount) {
        int[] nums = new int[paiCount];

        int num;
        for(int i = 0; i < nums.length; ++i) {
            do {
                num = RandomUtils.nextInt(paiCount);
                if (!ArrayUtils.contains(nums, num)) {
                    nums[i] = num;
                    break;
                }
            } while(num != 0 || ArrayUtils.indexOf(nums, num) != i);
        }

        return nums;
    }

    public void faPai() {
        Iterator var1 = this.getUserPacketMap().keySet().iterator();

        while(true) {
            String account;
            do {
                do {
                    if (!var1.hasNext()) {
                        return;
                    }

                    account = (String)var1.next();
                } while(!this.getUserPacketMap().containsKey(account));
            } while(this.getUserPacketMap().get(account) == null);

            Integer[] myPai = new Integer[3];

            for(int i = 0; i < 3; ++i) {
                myPai[i] = this.pai[this.paiIndex];
                ++this.paiIndex;
            }

            ((UserPacket)this.getUserPacketMap().get(account)).setPai(myPai);
            ((UserPacket)this.getUserPacketMap().get(account)).setType(ZhaJinHuaCore.getPaiType(Arrays.asList(myPai)));
            ((UserPacket)this.getUserPacketMap().get(account)).setStatus(2);
            
        }
    }

    public int getPlayerIndex(String account) {
        return account != null && this.getPlayerMap().containsKey(account) && this.getPlayerMap().get(account) != null ? ((Playerinfo)this.getPlayerMap().get(account)).getMyIndex() : -1;
    }

    public void addXiazhuList(int myIndex, double score) {
        JSONObject obj = new JSONObject();
        obj.put("index", myIndex);
        obj.put("score", score);
        this.getXiaZhuList().add(obj);
    }

    public void addScoreChange(String account, double score) {
    	//房间当前已下注积分修改
        this.totalScore = Dto.add(score, this.totalScore);
        //玩家下注前已下注的积分？
        double oldScore = ((UserPacket)this.getUserPacketMap().get(account)).getScore();
        //添加玩家下注的积分
        ((UserPacket)this.getUserPacketMap().get(account)).setScore(Dto.add(oldScore, score));
        //获取玩家信息
        Playerinfo playerinfo = (Playerinfo)this.getPlayerMap().get(account);
        //玩家总积分需要减去下注的积分
        ((Playerinfo)this.getPlayerMap().get(account)).setScore(Dto.sub(playerinfo.getScore(), score));
        if ((this.getRoomType() == 3 || this.getRoomType() == 1) && ((Playerinfo)this.getPlayerMap().get(account)).getScore() < 0.0D) {
            ((Playerinfo)this.getPlayerMap().get(account)).setScore(0.0D);
        }

    }

    public Integer[] getProgressIndex() {
        List<Integer> indexList = new ArrayList();
        Iterator var2 = this.userPacketMap.keySet().iterator();

        while(true) {
            String uuid;
            int status;
            do {
                do {
                    do {
                        if (!var2.hasNext()) {
                            return (Integer[])indexList.toArray(new Integer[indexList.size()]);
                        }

                        uuid = (String)var2.next();
                    } while(!this.getUserPacketMap().containsKey(uuid));
                } while(this.getUserPacketMap().get(uuid) == null);

                status = ((UserPacket)this.getUserPacketMap().get(uuid)).getStatus();
            } while(status != 2 && status != 3);

            indexList.add(((Playerinfo)this.getPlayerMap().get(uuid)).getMyIndex());
        }
    }

    //添加下注玩家
    public void addXzPlayer(int myIndex, int nextIndex) {
        this.getYiXiaZhu().add(myIndex);
        if (this.getYiXiaZhu().contains(nextIndex)) {
            this.getYiXiaZhu().clear();
            this.setGameNum(this.getGameNum() + 1);
        }

    }

    //获取下一个玩家
    public String getNextPlayer(String account) {
        if (this.getPlayerMap().get(account) != null) {
            int playerCount = 7;
            //当前玩家的下标
            int index = ((Playerinfo)this.getPlayerMap().get(account)).getMyIndex();
            //下一位玩家的下标
            int next = index + 1;

            for(Object player = null; player == null && index != next; ++next) {
            	//如果下一个下标为7  则改为0
                if (next >= playerCount) {
                    next = 0;
                }

                Iterator var6 = this.getPlayerMap().keySet().iterator();

                while(var6.hasNext()) {
                    String uuid = (String)var6.next();
                    if (this.getPlayerMap().containsKey(account) && this.getPlayerMap().get(account) != null && next == ((Playerinfo)this.getPlayerMap().get(uuid)).getMyIndex()) {
                        return uuid;
                    }
                }
            }
        }

        return this.getBanker();
    }

    //获取下一个操作的玩家
    public String getNextOperationPlayer(String uuid) {
    	//下一个玩家的account
        uuid = this.getNextPlayer(uuid);
        //玩家人数
        int count = this.getUserPacketMap().size();

        while(((UserPacket)this.getUserPacketMap().get(uuid)).getStatus() != 2 && ((UserPacket)this.getUserPacketMap().get(uuid)).getStatus() != 3 ) {
            uuid = this.getNextPlayer(uuid);
            --count;
            if (count <= 1) {
                break;
            }
        }

        this.setFocus(uuid);
        return uuid;
    }

    //是否全部准备
    public boolean isAllReady() {
        Iterator var1 = this.getUserPacketMap().keySet().iterator();

        String account;
        do {
            if (!var1.hasNext()) {
                return true;
            }

            account = (String)var1.next();
        } while(!this.getUserPacketMap().containsKey(account) || this.getUserPacketMap().get(account) == null || ((UserPacket)this.getUserPacketMap().get(account)).getStatus() == 1);

        return false;
    }

    public int getNowReadyCount() {
        int readyCount = 0;
        Iterator var2 = this.getUserPacketMap().keySet().iterator();

        while(var2.hasNext()) {
            String account = (String)var2.next();
            if (this.getUserPacketMap().containsKey(account) && this.getUserPacketMap().get(account) != null && ((UserPacket)this.getUserPacketMap().get(account)).getStatus() == 1) {
                ++readyCount;
            }
        }

        return readyCount;
    }

    //获取玩家分数
    public JSONArray getPlayerScore() {
        JSONArray array = new JSONArray();
        Iterator var2 = this.getUserPacketMap().keySet().iterator();

        while(var2.hasNext()) {
            String account = (String)var2.next();
            if (this.getUserPacketMap().containsKey(account) && this.getUserPacketMap().get(account) != null && ((UserPacket)this.getUserPacketMap().get(account)).getStatus() > 0) {
                JSONObject obj = new JSONObject();
                obj.put("index", this.getPlayerIndex(account));
                obj.put("myScore", ((UserPacket)this.getUserPacketMap().get(account)).getScore());
                array.add(obj);
            }
        }

        return array;
    }

    public JSONArray obtainFinalSummaryData() {
        if (this.getFinalSummaryData().size() > 0) {
            return this.getFinalSummaryData();
        } else {
            JSONArray array = new JSONArray();
            double circleMax = 0.0D;
            Iterator var4 = this.userPacketMap.keySet().iterator();

            String account;
            while(var4.hasNext()) {
                account = (String)var4.next();
                if (9 == this.getRoomType()) {
                    double sub = Dto.sub(((Playerinfo)this.getPlayerMap().get(account)).getScore(), ((Playerinfo)this.getPlayerMap().get(account)).getSourceScore());
                    if (sub > circleMax) {
                        circleMax = sub;
                    }
                }
            }

            var4 = this.getUserPacketMap().keySet().iterator();

            while(var4.hasNext()) {
                account = (String)var4.next();
                if (this.getUserPacketMap().containsKey(account) && this.getUserPacketMap().get(account) != null && ((UserPacket)this.getUserPacketMap().get(account)).getStatus() > 0) {
                    JSONObject obj = new JSONObject();
                    obj.put("name", ((Playerinfo)this.getPlayerMap().get(account)).getName());
                    obj.put("account", account);
                    obj.put("headimg", ((Playerinfo)this.getPlayerMap().get(account)).getRealHeadimg());
                    obj.put("isWinner", 0);
                    if (9 == this.getRoomType()) {
                        double sub = Dto.sub(((Playerinfo)this.getPlayerMap().get(account)).getScore(), ((Playerinfo)this.getPlayerMap().get(account)).getSourceScore());
                        obj.put("score", sub);
                        if (circleMax == sub) {
                            obj.put("isWinner", 1);
                        }
                    } else {
                        obj.put("score", ((Playerinfo)this.getPlayerMap().get(account)).getScore());
                        if (((Playerinfo)this.getPlayerMap().get(account)).getScore() > 0.0D) {
                            obj.put("isWinner", 1);
                        }
                    }

                    obj.put("isFangzhu", 0);
                    if (account.equals(this.getOwner())) {
                        obj.put("isFangzhu", 1);
                    }

                    obj.put("winTimes", ((UserPacket)this.getUserPacketMap().get(account)).getWinTimes());
                    obj.put("winScore", this.getUserPacketMap().get(account).getAllScore());
                    array.add(obj);
                }
            }

            this.setFinalSummaryData(array);
            return array;
        }
    }

    public boolean isAgreeClose() {
        Iterator var1 = this.userPacketMap.keySet().iterator();

        String account;
        do {
            if (!var1.hasNext()) {
                return true;
            }

            account = (String)var1.next();
        } while(!this.getUserPacketMap().containsKey(account) || this.getUserPacketMap().get(account) == null || ((Playerinfo)this.getPlayerMap().get(account)).getIsCloseRoom() == 1);

        return false;
    }

    //获取结算数据
    public JSONArray getJieSanData() {
        JSONArray array = new JSONArray();
        Iterator var2 = this.getUserPacketMap().keySet().iterator();

        while(var2.hasNext()) {
            String account = (String)var2.next();
            if (this.getUserPacketMap().containsKey(account) && this.getUserPacketMap().get(account) != null) {
                JSONObject obj = new JSONObject();
                obj.put("index", ((Playerinfo)this.getPlayerMap().get(account)).getMyIndex());
                obj.put("name", ((Playerinfo)this.getPlayerMap().get(account)).getName());
                obj.put("result", ((Playerinfo)this.getPlayerMap().get(account)).getIsCloseRoom());
                obj.put("jiesanTimer", this.getJieSanTime());
                array.add(obj);
            }
        }

        return array;
    }
}
