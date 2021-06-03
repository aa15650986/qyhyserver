
package com.zhuoan.biz.model;

import com.zhuoan.biz.model.qzmj.QZMJGameRoom;
import com.zhuoan.biz.model.sss.SSSGameRoomNew;
import com.zhuoan.biz.model.zjh.ZJHGameRoomNew;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.util.Dto;
import com.zhuoan.util.TimeUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class GameRoom implements Serializable, Cloneable {
    private String roomNo;
    private int roomType;
    private double fee;
    private double enterScore;
    private double leaveScore;
    private JSONObject roomInfo;
    private int gid;
    private int gameStatus;
    private int gameIndex = 0;
    private int gameNewIndex = 0;
    private int gameCount;
    private boolean isHalfwayIn = true;
    private int readyOvertime;
    private boolean robot;
    private List<String> robotList = new ArrayList();
    private int visit = 0;
    private int playerCount;
    private int score;
    private boolean isOpen;
    private String payType;
    private ConcurrentHashMap<String, Playerinfo> playerMap = new ConcurrentHashMap();
    private ConcurrentHashMap<String, Playerinfo> visitPlayerMap = new ConcurrentHashMap();
    private JSONObject setting;
    private String wfType;
    private String createTime;
    private JSONObject gameProcess = new JSONObject();
    private String ip;
    private int port;
    private int timeLeft;
    private List<Long> userIdList;
    private List<Integer> indexList = new ArrayList();
    private String owner;
    private String createRoom;
    private String banker;
    private int jieSanTime = 0;
    private long id = 0L;
    private int singlePayNum = 0;
    private JSONArray finalSummaryData = new JSONArray();
    private double minBankerScore;
    private JSONObject summaryData = new JSONObject();
    private boolean needFinalSummary = false;
    private int isClose = 0;
    private int lastIndex;
    private boolean isFund = false;
    private String matchNum;
    private int totalNum;
    private JSONObject winStreakObj = new JSONObject();
    private String currencyType;
    private String platform;
    private String clubCode;
    private boolean isCost = false;
    private long parUserId = 0L;
    private String circleId;
    private String cutType;
    private String cutFee;
    private double pump;
    private List<Integer> cutList;
    //最小开始人数
    private int minPlayer = 2;
    private String isAllStart;
    private boolean isRemoveRoom = false;
    private int isPlayerDismiss = 0;
    private boolean isVirtual = false;
    private boolean RobotRoom = false;
    
    

    public boolean isRobotRoom() {
		return RobotRoom;
	}

	public void setRobotRoom(boolean robotRoom) {
		RobotRoom = robotRoom;
	}

	public GameRoom() {
    }

    public boolean isVirtual() {
        return this.isVirtual;
    }

    public void setVirtual(boolean virtual) {
        this.isVirtual = virtual;
    }

    public int getIsPlayerDismiss() {
        return this.isPlayerDismiss;
    }

    public void setIsPlayerDismiss(int isPlayerDismiss) {
        this.isPlayerDismiss = isPlayerDismiss;
    }

    public boolean isRemoveRoom() {
        return this.isRemoveRoom;
    }

    public void setRemoveRoom(boolean removeRoom) {
        this.isRemoveRoom = removeRoom;
    }

    public String getIsAllStart() {
        return this.isAllStart;
    }

    public void setIsAllStart(String isAllStart) {
        this.isAllStart = isAllStart;
    }

    public int getMinPlayer() {
        return this.minPlayer;
    }

    public void setMinPlayer(int minPlayer) {
        this.minPlayer = minPlayer;
    }

    public int getGameNewIndex() {
        return this.gameNewIndex;
    }

    public void setGameNewIndex(int gameNewIndex) {
        this.gameNewIndex = gameNewIndex;
    }

    public List<Integer> getCutList() {
        return this.cutList;
    }

    public void setCutList(List<Integer> cutList) {
        this.cutList = cutList;
    }

    public double getPump() {
        return this.pump;
    }

    public void setPump(double pump) {
        this.pump = pump;
    }

    public long getParUserId() {
        return this.parUserId;
    }

    public void setParUserId(long parUserId) {
        this.parUserId = parUserId;
    }

    public boolean isCost() {
        return this.isCost;
    }

    public void setCost(boolean cost) {
        this.isCost = cost;
    }

    public int getLastIndex() {
        return this.lastIndex;
    }

    public void setLastIndex(int lastIndex) {
        this.lastIndex = lastIndex;
    }

    public int getIsClose() {
        return this.isClose;
    }

    public void setIsClose(int isClose) {
        this.isClose = isClose;
    }

    public boolean isNeedFinalSummary() {
        return this.needFinalSummary;
    }

    public void setNeedFinalSummary(boolean needFinalSummary) {
        this.needFinalSummary = needFinalSummary;
    }

    public JSONObject getSummaryData() {
        return this.summaryData;
    }

    public void setSummaryData(JSONObject summaryData) {
        this.summaryData = summaryData;
    }

    public String getRoomNo() {
        return this.roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public int getRoomType() {
        return this.roomType;
    }

    public void setRoomType(int roomType) {
        this.roomType = roomType;
    }

    public double getFee() {
        return this.fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    public double getEnterScore() {
        return this.enterScore;
    }

    public void setEnterScore(double enterScore) {
        this.enterScore = enterScore;
    }

    public double getLeaveScore() {
        return this.leaveScore;
    }

    public void setLeaveScore(double leaveScore) {
        this.leaveScore = leaveScore;
    }

    public JSONObject getRoomInfo() {
        return this.roomInfo;
    }

    public void setRoomInfo(JSONObject roomInfo) {
        this.roomInfo = roomInfo;
    }

    public int getGid() {
        return this.gid;
    }

    public void setGid(int gid) {
        this.gid = gid;
    }

    public int getGameStatus() {
        return this.gameStatus;
    }

    public void setGameStatus(int gameStatus) {
        this.gameStatus = gameStatus;
    }

    public int getGameIndex() {
        return this.gameIndex;
    }

    public void setGameIndex(int gameIndex) {
        this.gameIndex = gameIndex;
    }

    public int getGameCount() {
        return this.gameCount;
    }

    public void setGameCount(int gameCount) {
        this.gameCount = gameCount;
    }

    public boolean isHalfwayIn() {
        return this.isHalfwayIn;
    }

    public void setHalfwayIn(boolean halfwayIn) {
        this.isHalfwayIn = halfwayIn;
    }

    public int getReadyOvertime() {
        return this.readyOvertime;
    }

    public void setReadyOvertime(int readyOvertime) {
        this.readyOvertime = readyOvertime;
    }

    public boolean isRobot() {
        return this.robot;
    }

    public void setRobot(boolean robot) {
        this.robot = robot;
    }

    public List<String> getRobotList() {
        return this.robotList;
    }

    public void setRobotList(List<String> robotList) {
        this.robotList = robotList;
    }

    public int getVisit() {
        return this.visit;
    }

    public void setVisit(int visit) {
        this.visit = visit;
    }

    public int getPlayerCount() {
        return this.playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public int getScore() {
        return this.score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isOpen() {
        return this.isOpen;
    }

    public void setOpen(boolean open) {
        this.isOpen = open;
    }

    public String getPayType() {
        return this.payType;
    }

    public void setPayType(String payType) {
        this.payType = payType;
    }

    public ConcurrentHashMap<String, Playerinfo> getPlayerMap() {
        return this.playerMap;
    }

    public void setVisitPlayerMap(ConcurrentHashMap<String, Playerinfo> visitPlayerMap) {
        this.visitPlayerMap = visitPlayerMap;
    }

    public ConcurrentHashMap<String, Playerinfo> getVisitPlayerMap() {
        return this.visitPlayerMap;
    }

    public void setPlayerMap(ConcurrentHashMap<String, Playerinfo> playerMap) {
        this.playerMap = playerMap;
    }

    public JSONObject getSetting() {
        return this.setting;
    }

    public void setSetting(JSONObject setting) {
        this.setting = setting;
    }

    public String getWfType() {
        return this.wfType;
    }

    public void setWfType(String wfType) {
        this.wfType = wfType;
    }

    public String getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public JSONObject getGameProcess() {
        return this.gameProcess;
    }

    public void setGameProcess(JSONObject gameProcess) {
        this.gameProcess = gameProcess;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTimeLeft() {
        return this.timeLeft;
    }

    public void setTimeLeft(int timeLeft) {
        this.timeLeft = timeLeft;
    }

    public List<Long> getUserIdList() {
        return this.userIdList;
    }

    public void setUserIdList(List<Long> userIdList) {
        this.userIdList = userIdList;
    }

    public synchronized boolean addUserIdMyIndex(int myIndex, long usersId) {
        if (null != this.userIdList && this.userIdList.size() != 0) {
            if (0L != (Long)this.userIdList.get(myIndex)) {
                return false;
            } else {
                this.userIdList.set(myIndex, usersId);
                return true;
            }
        } else {
            return false;
        }
    }

    public List<Integer> getIndexList() {
        return this.indexList;
    }

    public void setIndexList(List<Integer> indexList) {
        this.indexList = indexList;
    }

    public synchronized void addIndexList(int index) {
        if (!this.indexList.contains(index)) {
            this.indexList.add(index);
        }

    }

    public synchronized boolean removeIndexList(Integer index) {
        return this.indexList.remove(index);
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getBanker() {
        if (null == this.banker) {
            this.banker = "";
        }

        return this.banker;
    }

    public void setBanker(String banker) {
        this.banker = banker;
    }

    public int getJieSanTime() {
        return this.jieSanTime;
    }

    public void setJieSanTime(int jieSanTime) {
        this.jieSanTime = jieSanTime;
    }

    public long getId() {
        if (0L >= this.id) {
            JSONObject roomInfo = DBUtil.getObjectBySQL("select id from za_gamerooms where room_no=? order by id desc", new Object[]{this.roomNo});
            if (null != roomInfo && roomInfo.containsKey("id")) {
                this.id = roomInfo.getLong("id");
            }
        }

        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getSinglePayNum() {
        return this.singlePayNum;
    }

    public void setSinglePayNum(int singlePayNum) {
        this.singlePayNum = singlePayNum;
    }

    public JSONArray getFinalSummaryData() {
        return this.finalSummaryData;
    }

    public void setFinalSummaryData(JSONArray finalSummaryData) {
        this.finalSummaryData = finalSummaryData;
    }

   
    

    public double getMinBankerScore() {
        return this.minBankerScore;
    }

    public void setMinBankerScore(double minBankerScore) {
        this.minBankerScore = minBankerScore;
    }

    public boolean isFund() {
        return this.isFund;
    }

    public void setFund(boolean fund) {
        this.isFund = fund;
    }

    public String getMatchNum() {
        return this.matchNum;
    }

    public void setMatchNum(String matchNum) {
        this.matchNum = matchNum;
    }

    public int getTotalNum() {
        return this.totalNum;
    }

    public void setTotalNum(int totalNum) {
        this.totalNum = totalNum;
    }

    public JSONObject getWinStreakObj() {
        return this.winStreakObj;
    }

    public void setWinStreakObj(JSONObject winStreakObj) {
        this.winStreakObj = winStreakObj;
    }

    public String getCurrencyType() {
        return this.currencyType;
    }

    public void setCurrencyType(String currencyType) {
        this.currencyType = currencyType;
    }

    public String getPlatform() {
        return this.platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getClubCode() {
        return this.clubCode;
    }

    public void setClubCode(String clubCode) {
        this.clubCode = clubCode;
    }

    public String getCircleId() {
        return this.circleId;
    }

    public void setCircleId(String circleId) {
        this.circleId = circleId;
    }

    public String getCutType() {
        return this.cutType;
    }

    public void setCutType(String cutType) {
        this.cutType = cutType;
    }

    public String getCutFee() {
        return this.cutFee;
    }

    public void setCutFee(String cutFee) {
        this.cutFee = cutFee;
    }

    public String getCreateRoom() {
        return this.createRoom;
    }

    public void setCreateRoom(String createRoom) {
        this.createRoom = createRoom;
    }

    public List<UUID> getAllUUIDList() {
        List<UUID> uuidList = new ArrayList();
        Iterator var2 = this.getPlayerMap().keySet().iterator();

        String account;
        Playerinfo playerinfo;
        while(var2.hasNext()) {
            account = (String)var2.next();
            playerinfo = (Playerinfo)this.getPlayerMap().get(account);
            if (null != playerinfo && null != playerinfo.getUuid()) {
                uuidList.add(playerinfo.getUuid());
            }
        }

        var2 = this.getVisitPlayerMap().keySet().iterator();

        while(var2.hasNext()) {
            account = (String)var2.next();
            playerinfo = (Playerinfo)this.getVisitPlayerMap().get(account);
            if (null != playerinfo && null != playerinfo.getUuid()) {
                uuidList.add(playerinfo.getUuid());
            }
        }

        return uuidList;
    }

    public List<UUID> getAllUUIDList(String uuid) {
        List<UUID> uuidList = new ArrayList();
        Iterator var3 = this.getPlayerMap().keySet().iterator();

        String account;
        Playerinfo playerinfo;
        while(var3.hasNext()) {
            account = (String)var3.next();
            playerinfo = (Playerinfo)this.getPlayerMap().get(account);
            if (null != playerinfo && null != playerinfo.getUuid() && !uuid.equals(account)) {
                uuidList.add(playerinfo.getUuid());
            }
        }

        var3 = this.getVisitPlayerMap().keySet().iterator();

        while(var3.hasNext()) {
            account = (String)var3.next();
            playerinfo = (Playerinfo)this.getVisitPlayerMap().get(account);
            if (null != playerinfo && null != playerinfo.getUuid() && !uuid.equals(account)) {
                uuidList.add(playerinfo.getUuid());
            }
        }

        return uuidList;
    }

    public String getUpdateType() {
        switch(this.getRoomType()) {
            case 0:
                return "roomcard";
            case 1:
                return "coins";
            case 2:
                return "roomcard";
            case 3:
                return "yuanbao";
            case 4:
                return "roomcard";
            case 5:
            default:
                return "";
            case 6:
                return "roomcard";
            case 7:
                return "yuanbao";
        }
    }

    public JSONObject getJsonObject(JSONArray array) {
        JSONObject objectDao = new JSONObject();
        objectDao.put("array", array);
        objectDao.put("roomNo", this.getRoomNo());
        objectDao.put("gId", this.getGid());
        objectDao.put("fee", this.getFee());
        objectDao.put("updateType", this.getCurrencyType());
        return objectDao;
    }

    public JSONObject getRoomCardChangeObject(JSONArray array, int roomCardCount) {
        JSONObject obj = new JSONObject();
        obj.put("array", array);
        obj.put("roomNo", this.getRoomNo());
        obj.put("gId", this.getGid());
        obj.put("fee", roomCardCount);
        obj.put("updateType", this.getCurrencyType());
        return obj;
    }

    public JSONObject getPumpObject(JSONArray array) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("array", array);
        jsonObject.put("updateType", this.getCurrencyType());
        return jsonObject;
    }

    public JSONArray obtainUserGameLog(long gameLogId, JSONArray users) {
        JSONArray userGameLogs = new JSONArray();
        double maxScore = 0.0D;

        int i;
        for(i = 0; i < users.size(); ++i) {
            if (users.getJSONObject(i).getDouble("fen") > maxScore) {
                maxScore = users.getJSONObject(i).getDouble("fen");
            }
        }

        for(i = 0; i < users.size(); ++i) {
            long userId = users.getJSONObject(i).getLong("id");
            JSONObject userGameLog = new JSONObject();
            userGameLog.put("gid", this.getGid());
            userGameLog.put("room_id", this.getId());
            userGameLog.put("room_no", this.getRoomNo());
            userGameLog.put("game_index", this.getGameNewIndex());
            userGameLog.put("user_id", userId);
            userGameLog.put("gamelog_id", gameLogId);
            userGameLog.put("createtime", TimeUtil.getNowDate());
            userGameLog.put("account", users.getJSONObject(i).getDouble("fen"));
            int isBigWinner = 0;
            if (maxScore == users.getJSONObject(i).getDouble("fen")) {
                isBigWinner = 1;
            }

            userGameLog.put("is_big_winner", Integer.valueOf(isBigWinner));
            userGameLog.put("fee", this.getFee());
            userGameLog.put("room_type", this.getRoomType());
            if (null != this.getClubCode() && !"".equals(this.getClubCode())) {
                userGameLog.put("club_code", this.getClubCode());
            }

            userGameLogs.add(userGameLog);
        }

        return userGameLogs;
    }

    public JSONObject obtainGameLog(String result, String gameProcess) {
        JSONObject gamelog = new JSONObject();
        StringBuffer id = new StringBuffer();
        id.append(System.currentTimeMillis());
        id.append(this.getRoomNo());
        gamelog.put("id", Long.valueOf(id.toString()));
        gamelog.put("gid", this.getGid());
        gamelog.put("room_id", this.getId());
        gamelog.put("room_no", this.getRoomNo());
        gamelog.put("game_index", this.getGameNewIndex());
        JSONObject baseInfo = this.getRoomInfo();
        if (4 == this.getGid()) {
            SSSGameRoomNew sssGameRoomNew = (SSSGameRoomNew)this;
            baseInfo.element("maPai", sssGameRoomNew.getMaPai()).element("maPaiType", sssGameRoomNew.getMaPaiType()).element("maPaiIndex", sssGameRoomNew.getMaPaiIndex()).element("maPaiAccount", sssGameRoomNew.getMaPaiAccount());
        }

        gamelog.put("base_info", baseInfo);
        gamelog.put("result", result);
        if (this.getGid() != 3 && this.getGid() != 12 && this.getGid() != 13) {
            if (this.getGid() == 6 && this instanceof ZJHGameRoomNew) {
                ZJHGameRoomNew zjhGameRoomNew = (ZJHGameRoomNew)this;
                StringBuffer roominfo = new StringBuffer();
                roominfo.append(this.getWfType()).append(" ").append(this.getPlayerCount()).append("人 ").append(this.getGameCount()).append("局");
                baseInfo.put("roominfo", String.valueOf(roominfo));
                baseInfo.put("game_count", this.getGameCount());
                baseInfo.put("zhuang", zjhGameRoomNew.getPlayerIndex(this.getBanker()));
                baseInfo.put("gameNum", zjhGameRoomNew.getTotalGameNum());
                baseInfo.put("currentScore", this.getScore());
                baseInfo.put("totalScore", 0);
                int score = !Dto.isObjNull(this.getSetting()) && this.getSetting().containsKey("boom_reward") ? this.getSetting().getInt("boom_reward") : 0;
                if (score > 0) {
                    baseInfo.put("boom_reward", "豹子奖励：" + score);
                }

                baseInfo.put("users", ((ZJHGameRoomNew)this).getAllPlayer());
                gamelog.put("base_info", baseInfo);
                gamelog.put("result", this.getSummaryData());
            }
        } else {
            QZMJGameRoom qzmjGameRoom = (QZMJGameRoom)this;
            baseInfo.put("game_count", this.getGameCount());
            baseInfo.put("zhuang", qzmjGameRoom.getPlayerIndex(this.getBanker()));
            baseInfo.put("jin", qzmjGameRoom.getJin());
            baseInfo.put("users", qzmjGameRoom.getAllPlayer());
            gamelog.put("base_info", baseInfo);
            gamelog.put("result", this.getSummaryData());
        }

        gamelog.put("action_records", gameProcess);
        String nowTime = TimeUtil.getNowDate();
        String visitCode = Dto.getEntNumCode(8);
        gamelog.put("visitcode", visitCode);
        gamelog.put("finishtime", nowTime);
        gamelog.put("createtime", nowTime);
        gamelog.put("status", 1);
        gamelog.put("roomtype", this.getRoomType());
        return gamelog;
    }

    public void addGameIndex() {
        int gameIndex = this.gameIndex;
        if (gameIndex < 0) {
            gameIndex = 0;
        }

        ++gameIndex;
        this.gameIndex = gameIndex;
    }

    public void addGameNewIndex() {
        int gameNewIndex = this.gameNewIndex;
        if (gameNewIndex < 0) {
            gameNewIndex = 0;
        }

        ++gameNewIndex;
        this.gameNewIndex = gameNewIndex;
    }

    public double maxScore(ArrayList<Double> score) {
        if (score.size() > 0) {
            double bigWinner = 0.0D;

            for(int i = 0; i < score.size(); ++i) {
                if ((Double)score.get(i) > bigWinner) {
                    bigWinner = (Double)score.get(i);
                }
            }

            return bigWinner;
        } else {
            return 0.0D;
        }
    }

    public Object clone() {
        GameRoom model = null;

        try {
            model = (GameRoom)super.clone();
        } catch (CloneNotSupportedException var3) {
            var3.printStackTrace();
        }

        return model;
    }

	@Override
	public String toString() {
		return "GameRoom [roomNo=" + roomNo + ", roomType=" + roomType + ", fee=" + fee + ", enterScore=" + enterScore
				+ ", leaveScore=" + leaveScore + ", roomInfo=" + roomInfo + ", gid=" + gid + ", gameStatus="
				+ gameStatus + ", gameIndex=" + gameIndex + ", gameNewIndex=" + gameNewIndex + ", gameCount="
				+ gameCount + ", isHalfwayIn=" + isHalfwayIn + ", readyOvertime=" + readyOvertime + ", robot=" + robot
				+ ", robotList=" + robotList + ", visit=" + visit + ", playerCount=" + playerCount + ", score=" + score
				+ ", isOpen=" + isOpen + ", payType=" + payType + ", playerMap=" + playerMap + ", visitPlayerMap="
				+ visitPlayerMap + ", setting=" + setting + ", wfType=" + wfType + ", createTime=" + createTime
				+ ", gameProcess=" + gameProcess + ", ip=" + ip + ", port=" + port + ", timeLeft=" + timeLeft
				+ ", userIdList=" + userIdList + ", indexList=" + indexList + ", owner=" + owner + ", createRoom="
				+ createRoom + ", banker=" + banker + ", jieSanTime=" + jieSanTime + ", id=" + id + ", singlePayNum="
				+ singlePayNum + ", finalSummaryData=" + finalSummaryData + ",  minBankerScore="
				+ minBankerScore + ", summaryData=" + summaryData + ", needFinalSummary=" + needFinalSummary
				+ ", isClose=" + isClose + ", lastIndex=" + lastIndex + ", isFund=" + isFund + ", matchNum=" + matchNum
				+ ", totalNum=" + totalNum + ", winStreakObj=" + winStreakObj + ", currencyType=" + currencyType
				+ ", platform=" + platform + ", clubCode=" + clubCode + ", isCost=" + isCost + ", parUserId="
				+ parUserId + ", circleId=" + circleId + ", cutType=" + cutType + ", cutFee=" + cutFee + ", pump="
				+ pump + ", cutList=" + cutList + ", minPlayer=" + minPlayer + ", isAllStart=" + isAllStart
				+ ", isRemoveRoom=" + isRemoveRoom + ", isPlayerDismiss=" + isPlayerDismiss + ", isVirtual=" + isVirtual
				+ "]";
	}
    
    
}
