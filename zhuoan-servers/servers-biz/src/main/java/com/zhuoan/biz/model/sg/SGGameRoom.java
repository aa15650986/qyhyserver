
package com.zhuoan.biz.model.sg;

import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SGGameRoom extends GameRoom implements Serializable {
    private ConcurrentMap<String, SGUserPacket> userPacketMap = new ConcurrentHashMap();
    private String winner;
    private int playType;
    private int specialType;
    private boolean isLaiZi;
    private int bolus;
    private List<Integer> robList;
    private int robMultiple;
    private int startType;
    private int startTypeNum;
    private int bet;
    private String doubleName;
    private Map<Integer, Integer> doubleMap;
    private String ante;
    private List<Integer> anteList;
    private List<SGCard> leftCard;
    private Map<String, Integer> playerWinScore;

    public SGGameRoom() {
    }

    public ConcurrentMap<String, SGUserPacket> getUserPacketMap() {
        return this.userPacketMap;
    }

    public void setUserPacketMap(ConcurrentMap<String, SGUserPacket> userPacketMap) {
        this.userPacketMap = userPacketMap;
    }

    public String getWinner() {
        return this.winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public int getPlayType() {
        return this.playType;
    }

    public void setPlayType(int playType) {
        this.playType = playType;
    }

    public int getSpecialType() {
        return this.specialType;
    }

    public void setSpecialType(int specialType) {
        this.specialType = specialType;
    }

    public boolean isLaiZi() {
        return this.isLaiZi;
    }

    public void setLaiZi(boolean laiZi) {
        this.isLaiZi = laiZi;
    }

    public int getBolus() {
        return this.bolus;
    }

    public void setBolus(int bolus) {
        this.bolus = bolus;
    }

    public List<Integer> getRobList() {
        return this.robList;
    }

    public void setRobList(List<Integer> robList) {
        this.robList = robList;
    }

    public int getRobMultiple() {
        return this.robMultiple;
    }

    public void setRobMultiple(int robMultiple) {
        this.robMultiple = robMultiple;
    }

    public int getStartType() {
        return this.startType;
    }

    public void setStartType(int startType) {
        this.startType = startType;
    }

    public int getStartTypeNum() {
        return this.startTypeNum;
    }

    public void setStartTypeNum(int startTypeNum) {
        this.startTypeNum = startTypeNum;
    }

    public int getBet() {
        return this.bet;
    }

    public void setBet(int bet) {
        this.bet = bet;
    }

    public String getDoubleName() {
        return this.doubleName;
    }

    public void setDoubleName(String doubleName) {
        this.doubleName = doubleName;
    }

    public Map<Integer, Integer> getDoubleMap() {
        return this.doubleMap;
    }

    public void setDoubleMap(Map<Integer, Integer> doubleMap) {
        this.doubleMap = doubleMap;
    }

    public String getAnte() {
        return this.ante;
    }

    public void setAnte(String ante) {
        this.ante = ante;
    }

    public List<Integer> getAnteList() {
        return this.anteList;
    }

    public void setAnteList(List<Integer> anteList) {
        Collections.sort(anteList, SGCard.intAsc);
        this.anteList = anteList;
    }

    public List<SGCard> getLeftCard() {
        return this.leftCard;
    }

    public void setLeftCard(List<SGCard> leftCard) {
        this.leftCard = leftCard;
    }

    public Map<String, Integer> getPlayerWinScore() {
        return this.playerWinScore;
    }

    public void setPlayerWinScore(Map<String, Integer> playerWinScore) {
        this.playerWinScore = playerWinScore;
    }

    public void initGame() {
        Iterator var1 = this.userPacketMap.keySet().iterator();

        while(var1.hasNext()) {
            String account = (String)var1.next();
            SGUserPacket up = (SGUserPacket)this.userPacketMap.get(account);
            if (null != up) {
                up.initUserPacket();
                ((Playerinfo)this.getPlayerMap().get(account)).addPlayTimes();
            }
        }

        this.winner = "-1";
        this.robMultiple = 1;
        this.getSummaryData().clear();
        this.addGameIndex();
        this.addGameNewIndex();
        this.setIsClose(0);
        this.leftCard = new ArrayList();
    }

    public int getNowReadyCount() {
        int readyCount = 0;
        Iterator var3 = this.getUserPacketMap().keySet().iterator();

        while(var3.hasNext()) {
            String account = (String)var3.next();
            SGUserPacket up = (SGUserPacket)this.userPacketMap.get(account);
            if (up != null && 1 == up.getStatus()) {
                ++readyCount;
            }
        }

        return readyCount;
    }
}
