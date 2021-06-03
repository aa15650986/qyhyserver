

package com.zhuoan.biz.model.gdy;

import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GDYGameRoom extends GameRoom implements Serializable {
    private List<GDYPacker> leftCard;
    private int leftCardNumber;
    private int totalCardNumber;
    private ConcurrentMap<String, GDYUserPacket> userPacketMap = new ConcurrentHashMap();
    private List<Integer> roomBombList;
    private int multiple = 1;
    private List<GDYPacker> lastCard;
    private int lastCardType;
    private String lastOperateAccount;
    private int lastOperateIndex;
    private boolean isLastPlayCard;
    private int lastPlayIndex;
    private int focusIndex;
    private String focusAccount;
    private boolean isPlay;
    private boolean isLiuJu;
    private int liuJuNum = 0;
    private Map<String, String> fillCard = new HashMap();
    private String winner;
    private int cardProcessNum;

    public GDYGameRoom() {
    }

    public List<GDYPacker> getLeftCard() {
        return this.leftCard;
    }

    public void setLeftCard(List<GDYPacker> leftCard) {
        this.leftCard = leftCard;
    }

    public int getLeftCardNumber() {
        return this.leftCard == null ? 0 : this.leftCard.size();
    }

    public void setLeftCardNumber(int leftCardNumber) {
        this.leftCardNumber = leftCardNumber;
    }

    public int getTotalCardNumber() {
        return this.totalCardNumber;
    }

    public void setTotalCardNumber(int totalCardNumber) {
        this.totalCardNumber = totalCardNumber;
    }

    public ConcurrentMap<String, GDYUserPacket> getUserPacketMap() {
        return this.userPacketMap;
    }

    public void setUserPacketMap(ConcurrentMap<String, GDYUserPacket> userPacketMap) {
        this.userPacketMap = userPacketMap;
    }

    public List<GDYPacker> getLastCard() {
        return this.lastCard;
    }

    public void setLastCard(List<GDYPacker> lastCard) {
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

    public List<Integer> getRoomBombList() {
        return this.roomBombList;
    }

    public void setRoomBombList(List<Integer> roomBombList) {
        this.roomBombList = roomBombList;
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

    public boolean isLiuJu() {
        return this.isLiuJu;
    }

    public void setLiuJu(boolean liuJu) {
        this.isLiuJu = liuJu;
    }

    public int getLastPlayIndex() {
        return this.lastPlayIndex;
    }

    public void setLastPlayIndex(int lastPlayIndex) {
        this.lastPlayIndex = lastPlayIndex;
    }

    public int getMultiple() {
        return this.multiple;
    }

    public void setMultiple(int multiple) {
        this.multiple = multiple;
    }

    public Map<String, String> getFillCard() {
        return this.fillCard;
    }

    public void setFillCard(Map<String, String> fillCard) {
        this.fillCard = fillCard;
    }

    public String getWinner() {
        return this.winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public int getLiuJuNum() {
        return this.liuJuNum;
    }

    public void setLiuJuNum(int liuJuNum) {
        this.liuJuNum = liuJuNum;
    }

    public int getCardProcessNum() {
        return this.cardProcessNum;
    }

    public void setCardProcessNum(int cardProcessNum) {
        this.cardProcessNum = cardProcessNum;
    }

    public void initGame() {
        this.leftCard = new ArrayList();
        this.totalCardNumber = 0;
        Iterator var1 = this.getUserPacketMap().keySet().iterator();

        while(var1.hasNext()) {
            String uuid = (String)var1.next();
            if (this.getUserPacketMap().containsKey(uuid) && this.getUserPacketMap().get(uuid) != null) {
                ((GDYUserPacket)this.getUserPacketMap().get(uuid)).initUserPacket();
                ((Playerinfo)this.getPlayerMap().get(uuid)).addPlayTimes();
            }
        }

        this.roomBombList = new ArrayList();
        this.lastCard = new ArrayList();
        this.lastCardType = 0;
        this.lastOperateAccount = "-1";
        this.lastOperateIndex = -1;
        this.focusIndex = -1;
        this.focusAccount = "-1";
        this.isPlay = false;
        this.isLastPlayCard = false;
        this.lastPlayIndex = -1;
        this.isLiuJu = false;
        this.multiple = 1;
        this.fillCard = new HashMap();
        this.winner = "-1";
        this.getSummaryData().clear();
        this.addGameIndex();
        this.addGameNewIndex();
        this.setIsClose(0);
        this.cardProcessNum = 0;
    }

    public List<String> getLastCardList() {
        List<String> card = new ArrayList();
        List<GDYPacker> myCard = this.lastCard;
        if (myCard != null && myCard.size() != 0) {
            StringBuilder sb = new StringBuilder();
            Iterator var4 = myCard.iterator();

            while(var4.hasNext()) {
                GDYPacker packer = (GDYPacker)var4.next();
                sb.delete(0, sb.length());
                sb.append(packer.getColor().getColor()).append("-").append(packer.getNum().getNum());
                card.add(sb.toString());
            }

            return card;
        } else {
            return card;
        }
    }

    public List<String> getLeftCardList() {
        List<String> card = new ArrayList();
        List<GDYPacker> myCard = this.leftCard;
        if (myCard != null && myCard.size() != 0) {
            StringBuilder sb = new StringBuilder();
            Iterator var4 = myCard.iterator();

            while(var4.hasNext()) {
                GDYPacker packer = (GDYPacker)var4.next();
                sb.delete(0, sb.length());
                sb.append(packer.getColor().getColor()).append("-").append(packer.getNum().getNum());
                card.add(sb.toString());
            }

            return card;
        } else {
            return card;
        }
    }

    public int getNowReadyCount() {
        int readyCount = 0;
        Iterator var3 = this.getUserPacketMap().keySet().iterator();

        while(var3.hasNext()) {
            String account = (String)var3.next();
            GDYUserPacket up = (GDYUserPacket)this.userPacketMap.get(account);
            if (up != null && 1 == up.getStatus()) {
                ++readyCount;
            }
        }

        return readyCount;
    }

    public boolean isAgreeClose() {
        Iterator var1 = this.userPacketMap.keySet().iterator();

        String account;
        do {
            if (!var1.hasNext()) {
                return true;
            }

            account = (String)var1.next();
        } while(!this.userPacketMap.containsKey(account) || this.userPacketMap.get(account) == null || ((Playerinfo)this.getPlayerMap().get(account)).getIsCloseRoom() == 1);

        return false;
    }

    public void addRoomBombList(int num) {
        if (num >= 3) {
            List<Integer> list = this.roomBombList;
            if (list == null) {
                list = new ArrayList();
            }

            ((List)list).add(num);
            this.roomBombList = (List)list;
        }
    }

    public GDYPacker removeLeftCard() {
        if (this.leftCard.size() > 0) {
            GDYPacker packer = (GDYPacker)this.leftCard.get(0);
            this.leftCard.remove(0);
            return packer;
        } else {
            return null;
        }
    }

    public void andLiuJuNum() {
        int liuJuNum = this.liuJuNum;
        if (liuJuNum < 0) {
            liuJuNum = 0;
        }

        ++liuJuNum;
        this.liuJuNum = liuJuNum;
    }

    public void andCardProcessNum() {
        int cardProcessNum = this.cardProcessNum;
        if (cardProcessNum < 0) {
            cardProcessNum = 0;
        }

        ++cardProcessNum;
        this.cardProcessNum = cardProcessNum;
    }
}
