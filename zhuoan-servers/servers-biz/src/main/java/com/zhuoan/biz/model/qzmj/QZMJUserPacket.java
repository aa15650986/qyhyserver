
package com.zhuoan.biz.model.qzmj;

import com.zhuoan.constant.QZMJConstant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class QZMJUserPacket {
    private List<Integer> myPai = new ArrayList();
    private List<Integer> huaList = new ArrayList();
    private List<DontMovePai> historyPai = new ArrayList();
    private int status;
    private double score;
    private int fan;
    private int youJin;
    private int youJinIng;
    private int huType;
    private int pingHuTimes;
    private int ziMoTimes;
    private int sanJinDaoTimes;
    private int youJinTimes;
    private int shuangYouTimes;
    private int sanYouTimes;
    private int tianHuTimes;
    private int qiangGangHuTimes;
    private int isTrustee;
    private int cancelTrusteeTime = 0;
    private double compensateScore = 0.0D;
    private int timeLeft = 0;
    private int isJin = 0;
    
    
    public int getIsJin() {
		return isJin;
	}

	public void setIsJin(int isJin) {
		this.isJin = isJin;
	}

	public QZMJUserPacket() {
    }

    public int getTimeLeft() {
        return this.timeLeft;
    }

    public void setTimeLeft(int timeLeft) {
        this.timeLeft = timeLeft;
    }

    public List<Integer> getMyPai() {
        return this.myPai;
    }

    public void setMyPai(List<Integer> myPai) {
        this.myPai = myPai;
    }

    public List<Integer> getHuaList() {
        return this.huaList;
    }

    public void setHuaList(List<Integer> huaList) {
        this.huaList = huaList;
    }

    public List<DontMovePai> getHistoryPai() {
        return this.historyPai;
    }

    public void setHistoryPai(List<DontMovePai> historyPai) {
        this.historyPai = historyPai;
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

    public int getFan() {
        return this.fan;
    }

    public void setFan(int fan) {
        this.fan = fan;
    }

    public int getYouJin() {
        return this.youJin;
    }

    public void setYouJin(int youJin) {
        this.youJin = youJin;
    }

    public int getYouJinIng() {
        return this.youJinIng;
    }

    public void setYouJinIng(int youJinIng) {
        this.youJinIng = youJinIng;
    }

    public int getHuType() {
        return this.huType;
    }

    public void setHuType(int huType) {
        this.huType = huType;
    }

    public int getPingHuTimes() {
        return this.pingHuTimes;
    }

    public void setPingHuTimes(int pingHuTimes) {
        this.pingHuTimes = pingHuTimes;
    }

    public int getZiMoTimes() {
        return this.ziMoTimes;
    }

    public void setZiMoTimes(int ziMoTimes) {
        this.ziMoTimes = ziMoTimes;
    }

    public int getSanJinDaoTimes() {
        return this.sanJinDaoTimes;
    }

    public void setSanJinDaoTimes(int sanJinDaoTimes) {
        this.sanJinDaoTimes = sanJinDaoTimes;
    }

    public int getYouJinTimes() {
        return this.youJinTimes;
    }

    public void setYouJinTimes(int youJinTimes) {
        this.youJinTimes = youJinTimes;
    }

    public int getShuangYouTimes() {
        return this.shuangYouTimes;
    }

    public void setShuangYouTimes(int shuangYouTimes) {
        this.shuangYouTimes = shuangYouTimes;
    }

    public int getSanYouTimes() {
        return this.sanYouTimes;
    }

    public void setSanYouTimes(int sanYouTimes) {
        this.sanYouTimes = sanYouTimes;
    }

    public int getTianHuTimes() {
        return this.tianHuTimes;
    }

    public void setTianHuTimes(int tianHuTimes) {
        this.tianHuTimes = tianHuTimes;
    }

    public int getQiangGangHuTimes() {
        return this.qiangGangHuTimes;
    }

    public void setQiangGangHuTimes(int qiangGangHuTimes) {
        this.qiangGangHuTimes = qiangGangHuTimes;
    }

    public int getIsTrustee() {
        return this.isTrustee;
    }

    public void setIsTrustee(int isTrustee) {
        this.isTrustee = isTrustee;
    }

    public int getCancelTrusteeTime() {
        return this.cancelTrusteeTime;
    }

    public void setCancelTrusteeTime(int cancelTrusteeTime) {
        this.cancelTrusteeTime = cancelTrusteeTime;
    }

    public double getCompensateScore() {
        return (new BigDecimal(this.compensateScore)).setScale(2, 4).doubleValue();
    }

    public void setCompensateScore(double compensateScore) {
        this.compensateScore = (new BigDecimal(compensateScore)).setScale(2, 4).doubleValue();
    }

    public void initUserPacket() {
        this.fan = 0;
        this.youJin = 0;
        this.youJinIng = 0;
        this.score = 0.0D;
        this.huaList = new ArrayList();
        this.historyPai = new ArrayList();
        this.myPai.clear();
        this.isTrustee = 0;
        this.cancelTrusteeTime = 0;
    }

    public void setMyPai(int[] pais) {
        if (this.myPai != null && this.myPai.size() > 0) {
            this.myPai.clear();
        }

        for(int i = 0; i < pais.length; ++i) {
            this.myPai.add(pais[i]);
        }

    }

    public int getPlayerJinCount(int jinPai) {
        int jinCount = 0;
        Iterator var3 = this.myPai.iterator();

        while(var3.hasNext()) {
            Integer p = (Integer)var3.next();
            if (p == jinPai) {
                ++jinCount;
            }
        }

        return jinCount;
    }

    public boolean removeMyPai(Integer oldPai) {
        return this.myPai.remove(oldPai);
    }

    public boolean addMyPai(int newpai) {
        return this.myPai.add(newpai);
    }

    public int buGangIndex(int pai) {
        List<DontMovePai> history = new ArrayList();
        if (this.historyPai != null && this.historyPai.size() > 0) {
            Iterator var3 = this.historyPai.iterator();

            label33:
            while(true) {
                DontMovePai dontMovePai;
                do {
                    if (!var3.hasNext()) {
                        break label33;
                    }

                    dontMovePai = (DontMovePai)var3.next();
                } while(dontMovePai.getType() != 2 && dontMovePai.getType() != 5);

                history.add(dontMovePai);
            }
        }

        for(int i = 0; i < history.size(); ++i) {
            if (((DontMovePai)history.get(i)).getFoucsPai() == pai) {
                return i;
            }
        }

        return 0;
    }

    public void addHistoryPai(int type, int[] pai, int foucsPai) {
        this.historyPai.add(new DontMovePai(type, pai, foucsPai));
    }

    public List<DontMovePai> getPengList() {
        return this.dontMovePaiList(2);
    }

    public List<DontMovePai> getGangAnList() {
        return this.dontMovePaiList(3);
    }

    public List<DontMovePai> getGangMingList() {
        return this.dontMovePaiList(4);
    }

    public List<DontMovePai> getGangBuList() {
        return this.dontMovePaiList(5);
    }

    public List<DontMovePai> dontMovePaiList(int type) {
        List<DontMovePai> back = new ArrayList();
        if (this.historyPai != null && this.historyPai.size() > 0) {
            Iterator var3 = this.historyPai.iterator();

            while(var3.hasNext()) {
                DontMovePai dontMovePai = (DontMovePai)var3.next();
                if (dontMovePai.getType() == type) {
                    back.add(dontMovePai);
                }
            }
        }

        return back;
    }

    public List<Integer> getHistoryList() {
        List<Integer> paiList = new ArrayList();

        for(int i = 1; i <= 5; ++i) {
            List<DontMovePai> pais = this.dontMovePaiList(i);
            if (pais != null) {
                for(int j = 0; j < pais.size(); ++j) {
                    int[] p = ((DontMovePai)pais.get(j)).getPai();

                    for(int k = 0; k < p.length; ++k) {
                        paiList.add(p[k]);
                    }
                }
            }
        }

        return paiList;
    }

    public List<Integer> getGangValue() {
        List<Integer> back = new ArrayList();
        if (this.historyPai != null && this.historyPai.size() > 0) {
            Iterator var2 = this.historyPai.iterator();

            while(true) {
                DontMovePai dontMovePai;
                do {
                    if (!var2.hasNext()) {
                        return back;
                    }

                    dontMovePai = (DontMovePai)var2.next();
                } while(dontMovePai.getType() != 3 && dontMovePai.getType() != 4 && dontMovePai.getType() != 5);

                back.add(dontMovePai.getFoucsPai());
            }
        } else {
            return back;
        }
    }

    public String getFanDetail(List<Integer> pais, QZMJGameRoom room, String account) {
        if (room.getGid() == 12) {
            return this.getFanDetailNA();
        } else {
            StringBuffer fanDetail = new StringBuffer();
            int jinPai = this.getFanShuOnJinPai(room.getJin());
            if (jinPai > 0) {
                fanDetail.append("金牌 ");
                fanDetail.append(jinPai);
                fanDetail.append("番   ");
            }

            int huaPai = this.getFanShuOnHuaPai();
            if (huaPai > 0) {
                fanDetail.append("花牌 ");
                fanDetail.append(huaPai);
                fanDetail.append("番   ");
            }

            int mingGang = 0;
            int anGang = 0;
            int keZi = 0;
            List<DontMovePai> pengList = this.getPengList();
            List<DontMovePai> anGList = this.getGangAnList();
            List<DontMovePai> mGList = this.getGangMingList();
            mGList.addAll(this.getGangBuList());
            Iterator var13 = anGList.iterator();

            DontMovePai dontMovePai;
            while(var13.hasNext()) {
                dontMovePai = (DontMovePai)var13.next();
                anGang += 3;
                if (QZMJConstant.ZI_PAI.contains(dontMovePai.getFoucsPai())) {
                    ++anGang;
                }
            }

            if (anGang > 0) {
                fanDetail.append("暗杠 ");
                fanDetail.append(anGang);
                fanDetail.append("番   ");
            }

            var13 = mGList.iterator();

            while(var13.hasNext()) {
                dontMovePai = (DontMovePai)var13.next();
                mingGang += 2;
                if (QZMJConstant.ZI_PAI.contains(dontMovePai.getFoucsPai())) {
                    ++mingGang;
                }
            }

            if (mingGang > 0) {
                fanDetail.append("明杠 ");
                fanDetail.append(mingGang);
                fanDetail.append("番   ");
            }

            var13 = pengList.iterator();

            while(var13.hasNext()) {
                dontMovePai = (DontMovePai)var13.next();
                if (QZMJConstant.ZI_PAI.contains(dontMovePai.getFoucsPai())) {
                    ++keZi;
                }
            }

            keZi += this.getFanShuOnHandPai(pais, room, account);
            if (keZi > 0) {
                fanDetail.append("刻子 ");
                fanDetail.append(keZi);
                fanDetail.append("番   ");
            }

            return fanDetail.toString();
        }
    }

    public String getFanDetailNA() {
        StringBuffer fanDetail = new StringBuffer();
        int gang = this.getNanAnFanShuOnGang();
        if (gang > 0) {
            fanDetail.append("杠 ");
            fanDetail.append(gang);
            fanDetail.append("番   ");
        }

        int hua = this.getNanAnFanShuOnHuaPai();
        if (hua > 0) {
            fanDetail.append("花牌 ");
            fanDetail.append(hua);
            fanDetail.append("番   ");
        }

        return String.valueOf(fanDetail);
    }

    private int getFanShuOnJinPai(int jin) {
        return this.getPlayerJinCount(jin) * 1;
    }

    private int getFanShuOnHuaPai() {
        if (this.huaList.size() >= 4) {
            int flower = 0;
            int season = 0;
            Iterator var3 = this.huaList.iterator();

            while(var3.hasNext()) {
                int hua = (Integer)var3.next();
                if (hua < 55) {
                    ++season;
                } else {
                    ++flower;
                }
            }

            if (flower == 4 && season == 4) {
                return this.huaList.size() * 1 * 2;
            }

            if (flower == 4) {
                return this.huaList.size() * 1 + 4;
            }

            if (season == 4) {
                return this.huaList.size() * 1 + 4;
            }
        }

        return this.huaList.size() * 1;
    }

    private int getFanShuOnHandPai(List<Integer> myPai, QZMJGameRoom room, String account) {
        int ke = 0;
        int zi = 0;
        List<Integer> pais = new ArrayList(myPai);
        List<Integer> paiList = new ArrayList(myPai);
        Collections.sort(paiList);
        List<Integer> fs = new ArrayList();

        for(Integer i = 0; i < pais.size(); i = i + 1) {
            if (room.getJin() != (Integer)pais.get(i)) {
                for(Integer j = 0; j < paiList.size(); j = j + 1) {
                    if (((Integer)pais.get(i)).equals(paiList.get(j))) {
                        fs.add(pais.get(i));
                    }
                }

                if (3 <= fs.size()) {
                    paiList.remove(pais.get(i));
                    paiList.remove(pais.get(i));
                    paiList.remove(pais.get(i));
                    if (4 != fs.size() && account.equals(room.getWinner())) {
                        if (room.getHuType() != 1 || !((Integer)pais.get(i)).equals(room.getLastPai())) {
                            ++ke;
                        }
                    } else {
                        ++ke;
                    }

                    if (QZMJConstant.ZI_PAI.contains(pais.get(i))) {
                        ++zi;
                    }
                }

                fs.clear();
            }
        }

        int fan = ke * 1 + zi * 1;
        return fan;
    }

    private int getFanShuOnZhuoPai() {
        int angang = 0;
        int minggang = 0;
        int zi = 0;
        List<DontMovePai> pengList = this.getPengList();
        List<DontMovePai> anGList = this.getGangAnList();
        List<DontMovePai> mGList = this.getGangMingList();
        mGList.addAll(this.getGangBuList());
        Iterator var7 = mGList.iterator();

        DontMovePai dontMovePai;
        while(var7.hasNext()) {
            dontMovePai = (DontMovePai)var7.next();
            ++minggang;
            if (QZMJConstant.ZI_PAI.contains(dontMovePai.getFoucsPai())) {
                ++zi;
            }
        }

        var7 = anGList.iterator();

        while(var7.hasNext()) {
            dontMovePai = (DontMovePai)var7.next();
            ++angang;
            if (QZMJConstant.ZI_PAI.contains(dontMovePai.getFoucsPai())) {
                ++zi;
            }
        }

        var7 = pengList.iterator();

        while(var7.hasNext()) {
            dontMovePai = (DontMovePai)var7.next();
            if (QZMJConstant.ZI_PAI.contains(dontMovePai.getFoucsPai())) {
                ++zi;
            }
        }

        return angang * 3 + minggang * 2 + zi * 1;
    }

    public int getTotalFanShu(List<Integer> pais, QZMJGameRoom game, String account) {
        return this.getFanShuOnHandPai(pais, game, account) + this.getFanShuOnZhuoPai() + this.getFanShuOnHuaPai() + this.getFanShuOnJinPai(game.getJin());
    }

    public void addHuTimes(int huType) {
        switch(huType) {
            case 1:
                ++this.pingHuTimes;
                break;
            case 2:
                ++this.ziMoTimes;
                break;
            case 3:
                ++this.sanJinDaoTimes;
                break;
            case 4:
                ++this.youJinTimes;
                break;
            case 5:
                ++this.shuangYouTimes;
                break;
            case 6:
                ++this.sanYouTimes;
            case 7:
            default:
                break;
            case 8:
                ++this.tianHuTimes;
                break;
            case 9:
                ++this.qiangGangHuTimes;
        }

    }

    public int getNanAnTotalFanShu() {
        return this.getNanAnFanShuOnGang() + this.getNanAnFanShuOnHuaPai();
    }

    private int getNanAnFanShuOnGang() {
        List<DontMovePai> anGList = this.getGangAnList();
        List<DontMovePai> mGList = this.getGangMingList();
        List<DontMovePai> bGList = this.getGangBuList();
        return anGList.size() * 2 + mGList.size() * 1 + bGList.size() * 1;
    }

    private int getNanAnFanShuOnHuaPai() {
        if (this.getHuaList().size() == 8) {
            return 2;
        } else {
            return this.getHuaList().size() >= 4 && this.getHuaList().size() < 8 ? 1 : 0;
        }
    }
}
