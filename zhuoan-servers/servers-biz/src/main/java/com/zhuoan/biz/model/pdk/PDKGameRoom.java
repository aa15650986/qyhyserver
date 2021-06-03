
package com.zhuoan.biz.model.pdk;

import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PDKGameRoom extends GameRoom implements Serializable {
    private int multiple = 1;
    private ConcurrentMap<String, PDKUserPacket> userPacketMap = new ConcurrentHashMap();
    private String playerCardAccount = "-1";
    private List<PDKPacker> lastCard;
    private int lastCardType;
    private String lastOperateAccount;
    private int lastOperateIndex;
    private boolean isLastPlayCard;
    private int lastPlayIndex;
    private int focusIndex;
    private String focusAccount;
    private boolean isPlay;
    private List<Map<Integer, List<PDKPacker>>> cradGroup;
    private String winner = "-1";
    private int cardProcessNum;
    private int waysType;
    private int payRules;
    private int passCard;
    private int settleType;
    private int threeType;
    private int fourType;
    private int bombsScore;
    private String heiTaoAccount;
    private int heiTaoAccountIndex;
    private int playTime = 15;
    private List<PDKGameFlow> pdkGameFlowList = new ArrayList();

    public PDKGameRoom() {
    }

    public int getPlayTime() {
        return this.playTime;
    }

    public void setPlayTime(int playTime) {
        this.playTime = playTime;
    }

    public String getPlayerCardAccount() {
        return this.playerCardAccount;
    }

    public void setPlayerCardAccount(String playerCardAccount) {
        this.playerCardAccount = playerCardAccount;
    }

    public int getWaysType() {
        return this.waysType;
    }

    public void setWaysType(int waysType) {
        this.waysType = waysType;
    }

    public int getPayRules() {
        return this.payRules;
    }

    public void setPayRules(int payRules) {
        this.payRules = payRules;
    }

    public int getPassCard() {
        return this.passCard;
    }

    public void setPassCard(int passCard) {
        this.passCard = passCard;
    }

    public int getSettleType() {
        return this.settleType;
    }

    public void setSettleType(int settleType) {
        this.settleType = settleType;
    }

    public int getThreeType() {
        return this.threeType;
    }

    public void setThreeType(int threeType) {
        this.threeType = threeType;
    }

    public int getFourType() {
        return this.fourType;
    }

    public void setFourType(int fourType) {
        this.fourType = fourType;
    }

    public int getBombsScore() {
        return this.bombsScore;
    }

    public void setBombsScore(int bombsScore) {
        this.bombsScore = bombsScore;
    }

    public String getHeiTaoAccount() {
        return this.heiTaoAccount;
    }

    public void setHeiTaoAccount(String heiTaoAccount) {
        this.heiTaoAccount = heiTaoAccount;
    }

    public int getHeiTaoAccountIndex() {
        return this.heiTaoAccountIndex;
    }

    public void setHeiTaoAccountIndex(int heiTaoAccountIndex) {
        this.heiTaoAccountIndex = heiTaoAccountIndex;
    }

    public int getMultiple() {
        return this.multiple;
    }

    public void setMultiple(int multiple) {
        this.multiple = multiple;
    }

    public ConcurrentMap<String, PDKUserPacket> getUserPacketMap() {
        return this.userPacketMap;
    }

    public void setUserPacketMap(ConcurrentMap<String, PDKUserPacket> userPacketMap) {
        this.userPacketMap = userPacketMap;
    }

    public List<PDKPacker> getLastCard() {
        return this.lastCard;
    }

    public void setLastCard(List<PDKPacker> lastCard) {
        this.lastCard = lastCard;
    }

    public int getLastCardType() {
        return this.lastCardType;
    }

    public void setLastCardType(int lastCardType) {
        this.lastCardType = lastCardType;
    }

    public String getLastOperateAccount() {
        return this.lastOperateAccount;
    }

    public void setLastOperateAccount(String lastOperateAccount) {
        this.lastOperateAccount = lastOperateAccount;
    }

    public int getLastOperateIndex() {
        return this.lastOperateIndex;
    }

    public void setLastOperateIndex(int lastOperateIndex) {
        this.lastOperateIndex = lastOperateIndex;
    }

    public boolean isLastPlayCard() {
        return this.isLastPlayCard;
    }

    public void setLastPlayCard(boolean lastPlayCard) {
        this.isLastPlayCard = lastPlayCard;
    }

    public int getLastPlayIndex() {
        return this.lastPlayIndex;
    }

    public void setLastPlayIndex(int lastPlayIndex) {
        this.lastPlayIndex = lastPlayIndex;
    }

    public int getFocusIndex() {
        return this.focusIndex;
    }

    public void setFocusIndex(int focusIndex) {
        this.focusIndex = focusIndex;
    }

    public String getFocusAccount() {
        return this.focusAccount;
    }

    public void setFocusAccount(String focusAccount) {
        this.focusAccount = focusAccount;
    }

    public boolean isPlay() {
        return this.isPlay;
    }

    public void setPlay(boolean play) {
        this.isPlay = play;
    }

    public String getWinner() {
        return this.winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public int getCardProcessNum() {
        return this.cardProcessNum;
    }

    public void setCardProcessNum(int cardProcessNum) {
        this.cardProcessNum = cardProcessNum;
    }

    public List<Map<Integer, List<PDKPacker>>> getCradGroup() {
        return this.cradGroup;
    }

    public void setCradGroup(List<Map<Integer, List<PDKPacker>>> cradGroup) {
        this.cradGroup = cradGroup;
    }

    public List<PDKGameFlow> getPdkGameFlowList() {
        return this.pdkGameFlowList;
    }

    public void setPdkGameFlowList(List<PDKGameFlow> pdkGameFlowList) {
        this.pdkGameFlowList = pdkGameFlowList;
    }

    public void initGame() {
        this.multiple = 1;
        Iterator var1 = this.getUserPacketMap().keySet().iterator();

        while(var1.hasNext()) {
            String account = (String)var1.next();
            PDKUserPacket up = (PDKUserPacket)this.getUserPacketMap().get(account);
            Playerinfo playerinfo = (Playerinfo)this.getPlayerMap().get(account);
            if (null != up && null != playerinfo) {
                up.initUserPacket();
                playerinfo.addPlayTimes();
            }
        }

        this.lastCard = new ArrayList();
        this.lastCardType = 0;
        this.lastOperateAccount = "-1";
        this.lastOperateIndex = -1;
        this.isLastPlayCard = false;
        this.lastPlayIndex = -1;
        this.focusIndex = -1;
        this.focusAccount = "-1";
        this.isPlay = false;
        this.getSummaryData().clear();
        this.addGameIndex();
        this.addGameNewIndex();
        this.setIsClose(0);
        this.cardProcessNum = 0;
        this.heiTaoAccount = "-1";
        this.heiTaoAccountIndex = -1;
        this.cradGroup = new ArrayList();
        this.pdkGameFlowList = new ArrayList();
    }

    public int getNowReadyCount() {
        int readyCount = 0;
        Iterator var2 = this.getUserPacketMap().keySet().iterator();

        while(var2.hasNext()) {
            String account = (String)var2.next();
            PDKUserPacket up = (PDKUserPacket)this.userPacketMap.get(account);
            if (null != up && 1 == up.getStatus()) {
                ++readyCount;
            }
        }

        return readyCount;
    }

    public List<String> getLastCardList() {
        List<String> card = new ArrayList();
        List<PDKPacker> myCard = this.lastCard;
        if (myCard != null && myCard.size() != 0) {
            StringBuilder sb = new StringBuilder();
            Iterator var4 = myCard.iterator();

            while(var4.hasNext()) {
                PDKPacker packer = (PDKPacker)var4.next();
                sb.delete(0, sb.length());
                sb.append(packer.getColor().getColor()).append("-").append(packer.getNum().getNum());
                card.add(sb.toString());
            }

            return card;
        } else {
            return card;
        }
    }

    public void addPDKGameFlowList(int index, int cardType, List<String> myCard, String account, String headimg, String name) {
        PDKGameFlow pdkGameFlow = new PDKGameFlow();
        pdkGameFlow.setIndex(index);
        pdkGameFlow.setCardType(cardType);
        pdkGameFlow.setMyCard(myCard);
        if (null != account) {
            pdkGameFlow.setAccount(account);
        }

        if (null != headimg) {
            pdkGameFlow.setHeadimg(headimg);
        }

        if (null != name) {
            pdkGameFlow.setName(name);
        }

        this.pdkGameFlowList.add(pdkGameFlow);
    }

    public boolean isAgreeClose() {
        Iterator var1 = this.userPacketMap.keySet().iterator();

        String account;
        do {
            if (!var1.hasNext()) {
                return true;
            }

            account = (String)var1.next();
        } while(!this.userPacketMap.containsKey(account) || this.userPacketMap.get(account) == null || 1 == ((Playerinfo)this.getPlayerMap().get(account)).getIsCloseRoom());

        return false;
    }

    public void andCardProcessNum() {
        int cardProcessNum = this.cardProcessNum;
        if (cardProcessNum < 0) {
            cardProcessNum = 0;
        }

        ++cardProcessNum;
        this.cardProcessNum = cardProcessNum;
    }

    public boolean allNoClose() {
        Iterator var1 = this.getPlayerMap().keySet().iterator();

        String account;
        do {
            if (!var1.hasNext()) {
                return true;
            }

            account = (String)var1.next();
        } while(!this.getUserPacketMap().containsKey(account) || this.getUserPacketMap().get(account) == null || ((Playerinfo)this.getPlayerMap().get(account)).getIsCloseRoom() == 0);

        return false;
    }
}
