
package com.zhuoan.biz.model.gzmj;

import com.zhuoan.biz.model.GameRoom;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.sf.json.JSONObject;

public class GzMjGameRoom extends GameRoom implements Serializable {
    private int bankerTime;
    private int totalCardNum;
    private String lastAccount = null;
    private Integer lastCard = -1;
    private Integer currentCard;
    private Integer focusIndex;
    private boolean hasBet;
    private boolean hasTwo;
    private boolean hasDl;
    private boolean hasMtChock;
    private boolean hasWgChock;
    private boolean hasWeekChock;
    private boolean hasSelfChock;
    private List<Integer> cards = new ArrayList();
    private List<JSONObject> betList = new ArrayList();
    private ConcurrentHashMap<String, UserPacketGzMj> userPacketMap = new ConcurrentHashMap();
    private ConcurrentHashMap<String, Integer> actionMap = new ConcurrentHashMap();
    private List<String> lastHuAccount = new ArrayList();
    private int changeType;
    private boolean finish = false;
    private int huCard;
    private String lastMoAccount;

    public GzMjGameRoom() {
    }

    public int getBankerTime() {
        return this.bankerTime;
    }

    public void setBankerTime(int bankerTime) {
        this.bankerTime = bankerTime;
    }

    public int getTotalCardNum() {
        return this.totalCardNum;
    }

    public void setTotalCardNum(int totalCardNum) {
        this.totalCardNum = totalCardNum;
    }

    public String getLastAccount() {
        return this.lastAccount;
    }

    public void setLastAccount(String lastAccount) {
        this.lastAccount = lastAccount;
    }

    public Integer getLastCard() {
        return this.lastCard;
    }

    public void setLastCard(Integer lastCard) {
        this.lastCard = lastCard;
    }

    public Integer getCurrentCard() {
        return this.currentCard;
    }

    public void setCurrentCard(Integer currentCard) {
        this.currentCard = currentCard;
    }

    public Integer getFocusIndex() {
        return this.focusIndex;
    }

    public void setFocusIndex(Integer focusIndex) {
        this.focusIndex = focusIndex;
    }

    public boolean isHasBet() {
        return this.hasBet;
    }

    public void setHasBet(boolean hasBet) {
        this.hasBet = hasBet;
    }

    public boolean isHasTwo() {
        return this.hasTwo;
    }

    public void setHasTwo(boolean hasTwo) {
        this.hasTwo = hasTwo;
    }

    public boolean isHasDl() {
        return this.hasDl;
    }

    public void setHasDl(boolean hasDl) {
        this.hasDl = hasDl;
    }

    public boolean isHasMtChock() {
        return this.hasMtChock;
    }

    public void setHasMtChock(boolean hasMtChock) {
        this.hasMtChock = hasMtChock;
    }

    public boolean isHasWgChock() {
        return this.hasWgChock;
    }

    public void setHasWgChock(boolean hasWgChock) {
        this.hasWgChock = hasWgChock;
    }

    public boolean isHasWeekChock() {
        return this.hasWeekChock;
    }

    public void setHasWeekChock(boolean hasWeekChock) {
        this.hasWeekChock = hasWeekChock;
    }

    public boolean isHasSelfChock() {
        return this.hasSelfChock;
    }

    public void setHasSelfChock(boolean hasSelfChock) {
        this.hasSelfChock = hasSelfChock;
    }

    public List<Integer> getCards() {
        return this.cards;
    }

    public void setCards(List<Integer> cards) {
        this.cards = cards;
    }

    public List<JSONObject> getBetList() {
        return this.betList;
    }

    public void setBetList(List<JSONObject> betList) {
        this.betList = betList;
    }

    public ConcurrentHashMap<String, UserPacketGzMj> getUserPacketMap() {
        return this.userPacketMap;
    }

    public void setUserPacketMap(ConcurrentHashMap<String, UserPacketGzMj> userPacketMap) {
        this.userPacketMap = userPacketMap;
    }

    public ConcurrentHashMap<String, Integer> getActionMap() {
        return this.actionMap;
    }

    public void setActionMap(ConcurrentHashMap<String, Integer> actionMap) {
        this.actionMap = actionMap;
    }

    public List<String> getLastHuAccount() {
        return this.lastHuAccount;
    }

    public void setLastHuAccount(List<String> lastHuAccount) {
        this.lastHuAccount = lastHuAccount;
    }

    public int getChangeType() {
        return this.changeType;
    }

    public void setChangeType(int changeType) {
        this.changeType = changeType;
    }

    public boolean isFinish() {
        return this.finish;
    }

    public void setFinish(boolean finish) {
        this.finish = finish;
    }

    public int getHuCard() {
        return this.huCard;
    }

    public void setHuCard(int huCard) {
        this.huCard = huCard;
    }

    public String getLastMoAccount() {
        return this.lastMoAccount;
    }

    public void setLastMoAccount(String lastMoAccount) {
        this.lastMoAccount = lastMoAccount;
    }
}
