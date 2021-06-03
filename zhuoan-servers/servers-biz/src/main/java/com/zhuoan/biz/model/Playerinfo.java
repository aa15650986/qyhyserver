

package com.zhuoan.biz.model;

import com.zhuoan.constant.CommonConstant;
import com.zhuoan.service.socketio.impl.GameMain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Playerinfo implements Serializable {
    private UUID uuid;
    private long id;
    private String account;
    private String name;
    private String headimg;
    private String ip;
    private String location;
    private String area;
    private String sex;
    private int status;
    private double score = 0.0D;
    private int myIndex;
    private String signature;
    private int vip;
    private int luck = -1;
    private boolean isTipMsg = true;
    private String ghName = "该玩家未加入工会";
    private int roomCardNum = 0;
    private String openId;
    private long assetsId;
    private int myRank;
    private String platform;
    private double sourceScore = 0.0D;
    //是否破产
    private boolean isCollapse = false;
    private double cutScore = 0.0D;
    private int isCloseRoom = 0;
    private int playTimes = 0;
    private double rewardScore = 0.0D;
    private List<String> sss = new ArrayList<>();
    private List<String> pdk = new ArrayList<>();
    

	public List<String> getPdk() {
		return pdk;
	}

	public void setPdk(List<String> pdk) {
		this.pdk = pdk;
	}

	public List<String> getSss() {
		return sss;
	}

	public void setSss(List<String> sss) {
		this.sss = sss;
	}

	public Playerinfo() {
    }

    public double getRewardScore() {
        return (new BigDecimal(this.rewardScore)).setScale(2, 4).doubleValue();
    }

    public void setRewardScore(double rewardScore) {
        this.rewardScore = (new BigDecimal(rewardScore)).setScale(2, 4).doubleValue();
    }

    public void addPlayTimes() {
        ++this.playTimes;
    }

    public int getPlayTimes() {
        return this.playTimes;
    }

    public void setPlayTimes(int playTimes) {
        this.playTimes = playTimes;
    }

    public int getIsCloseRoom() {
        return this.isCloseRoom;
    }

    public void setIsCloseRoom(int isCloseRoom) {
        this.isCloseRoom = isCloseRoom;
    }

    public double getCutScore() {
        return (new BigDecimal(this.cutScore)).setScale(2, 4).doubleValue();
    }

    public void setCutScore(double cutScore) {
        this.cutScore = (new BigDecimal(cutScore)).setScale(2, 4).doubleValue();
    }

    public boolean isCollapse() {
        return this.isCollapse;
    }

    public void setCollapse(boolean collapse) {
        this.isCollapse = collapse;
    }

    public int getRoomCardNum() {
        return this.roomCardNum;
    }

    public void setRoomCardNum(int roomCardNum) {
        this.roomCardNum = roomCardNum;
    }

    public String getGhName() {
        return this.ghName;
    }

    public void setGhName(String ghName) {
        this.ghName = ghName;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getLocation() {
        return this.location;
    }

    public void setLocation(String location) {
        if (location != null && location.indexOf("失败") == -1 && !location.equals("null")) {
            this.location = location;
        } else {
            this.location = "0,0";
        }

    }

    public String getArea() {
        return this.area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAccount() {
        return this.account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHeadimg() {
        return this.headimg;
    }

    public void setHeadimg(String headimg) {
        this.headimg = headimg;
    }

    public String getSex() {
        return this.sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public double getScore() {
        return (new BigDecimal(this.score)).setScale(2, 4).doubleValue();
    }

    public void setScore(double score) {
        this.score = (new BigDecimal(score)).setScale(2, 4).doubleValue();
    }

    public int getMyIndex() {
        return this.myIndex;
    }

    public void setMyIndex(int myIndex) {
        this.myIndex = myIndex;
    }

    public String getSignature() {
        return this.signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public int getVip() {
        return this.vip;
    }

    public void setVip(int vip) {
        this.vip = vip;
    }

    public int getLuck() {
        return this.luck;
    }

    public void setLuck(int luck) {
        this.luck = luck;
    }

    public boolean isTipMsg() {
        return this.isTipMsg;
    }

    public void setTipMsg(boolean isTipMsg) {
        this.isTipMsg = isTipMsg;
    }

    public String getOpenId() {
        return this.openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public long getAssetsId() {
        return this.assetsId;
    }

    public void setAssetsId(long assetsId) {
        this.assetsId = assetsId;
    }

    public int getMyRank() {
        return this.myRank;
    }

    public void setMyRank(int myRank) {
        this.myRank = myRank;
    }

    public String getPlatform() {
        return this.platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public double getSourceScore() {
        return (new BigDecimal(this.sourceScore)).setScale(2, 4).doubleValue();
    }

    public void setSourceScore(double sourceScore) {
        this.sourceScore = (new BigDecimal(sourceScore)).setScale(2, 4).doubleValue();
    }
   
    public String getRealHeadimg() {
        return "http://"+GameMain.propertiesUtil.get("ip")+"/zagame" + this.getHeadimg();
    }


    
}
