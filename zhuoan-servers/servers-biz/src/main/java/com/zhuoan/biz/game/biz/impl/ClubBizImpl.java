
package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.ClubBiz;
import com.zhuoan.biz.game.dao.ClubDao;
import com.zhuoan.util.Dto;
import com.zhuoan.util.TimeUtil;
import javax.annotation.Resource;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class ClubBizImpl implements ClubBiz {
    @Resource
    private ClubDao clubDao;

    public ClubBizImpl() {
    }

    public JSONObject getUserClubByAccount(String account) {
        return this.clubDao.getUserClubByAccount(account);
    }

    public JSONObject getClubByCode(String clubCode) {
        return this.clubDao.getClubByCode(clubCode);
    }

    public JSONObject getClubById(long id) {
        return this.clubDao.getClubById(id);
    }

    public JSONArray getClubMember(long clubId) {
        return this.clubDao.getClubMember(clubId);
    }

    public JSONObject getUserByAccountAndUuid(String account, String uuid) {
        return this.clubDao.getUserByAccountAndUuid(account, uuid);
    }

    public void updateClubInfo(JSONObject clubInfo) {
        this.clubDao.updateClubInfo(clubInfo);
    }

    public void updateUserClubIds(long userId, String clubIds) {
        this.clubDao.updateUserClubIds(userId, clubIds);
    }

    public boolean clubPump(String clubCode, double sum, long roomId, String roomNo, int gid) {
        JSONObject clubInfo = this.clubDao.getClubByCode(clubCode);
        if (!Dto.isObjNull(clubInfo)) {
            if (clubInfo.getDouble("balance") < sum) {
                return false;
            } else {
                this.clubDao.updateClubBalance(clubInfo.getLong("id"), sum);
                this.clubDao.addClubPumpRec(clubInfo.getLong("leaderId"), roomId, roomNo, gid, clubInfo.getInt("balance_type") - 1, -sum, TimeUtil.getNowDate(), clubInfo.getString("platform"), clubInfo.getDouble("balance") - sum, clubInfo.getDouble("balance"));
                return true;
            }
        } else {
            return false;
        }
    }

    public void updateUserTopClub(String account, long clubId) {
        this.clubDao.updateUserTopClub(account, clubId);
    }

    public JSONArray getClubInviteRec(int status, long clubId) {
        return this.clubDao.getClubInviteRec(status, clubId);
    }

    public void updateClubInviteRecStatus(int status, long clubInviteRecId) {
        this.clubDao.updateClubInviteRecStatus(status, clubInviteRecId);
    }

    public void updateUserClub(long userId, String clubIds) {
        this.clubDao.updateUserClub(userId, clubIds);
    }

    public void addClubInviteRec(long userId, long clubId, long parId, String memo, int status) {
        this.clubDao.addClubInviteRec(userId, clubId, parId, memo, status);
    }

    public boolean updatePropAndWriteRec(Double sum, Long playerId, int type, int doType, String platform, String des, int operatorType) {
        return this.clubDao.updatePropAndWriteRec(sum, playerId, type, doType, platform, des, operatorType);
    }

    public boolean addClub(JSONObject club) {
        return this.clubDao.addClub(club);
    }

    public JSONObject getSysClubSetting(String platform) {
        return this.clubDao.getSysClubSetting(platform);
    }

    public int countClubByLeader(String leaderId, String platform) {
        return this.clubDao.countClubByLeader(leaderId, platform);
    }

    public void updateClubBalance(long clubId, double sum) {
        this.clubDao.updateClubBalance(clubId, sum);
    }
}
