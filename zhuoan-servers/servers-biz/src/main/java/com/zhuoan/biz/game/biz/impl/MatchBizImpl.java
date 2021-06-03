

package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.MatchBiz;
import com.zhuoan.biz.game.dao.MatchDao;
import javax.annotation.Resource;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class MatchBizImpl implements MatchBiz {
    @Resource
    private MatchDao matchDao;

    public MatchBizImpl() {
    }

    public JSONArray getMatchSettingByType(int type, String createTime) {
        return this.matchDao.getMatchSettingByType(type, createTime);
    }

    public JSONObject getMatchSettingById(long matchId, long gameId) {
        return this.matchDao.getMatchSettingById(matchId, gameId);
    }

    public void updateMatchSettingById(JSONObject matchSetting) {
        this.matchDao.updateMatchSettingById(matchSetting);
    }

    public JSONObject getMatchInfoByMatchId(long matchId, int isFull, int isEnd) {
        return this.matchDao.getMatchInfoByMatchId(matchId, isFull, isEnd);
    }

    public void addOrUpdateMatchInfo(JSONObject obj) {
        this.matchDao.addOrUpdateMatchInfo(obj);
    }

    public void updateMatchInfoByMatchNum(String matchNum, int isFull) {
        this.matchDao.updateMatchInfoByMatchNum(matchNum, isFull);
    }

    public JSONArray getRobotList(int count) {
        return this.matchDao.getRobotList(count);
    }

    public void updateUserCoinsAndScoreByAccount(String account, int coins, int score, int roomCard, double yb) {
        this.matchDao.updateUserCoinsAndScoreByAccount(account, coins, score, roomCard, yb);
    }

    public JSONObject getUserWinningRecord(String account, int gameId) {
        return this.matchDao.getUserWinningRecord(account, gameId);
    }

    public void addOrUpdateUserWinningRecord(JSONObject winningRecord) {
        this.matchDao.addOrUpdateUserWinningRecord(winningRecord);
    }

    public void updateRobotStatus(String account, int status) {
        this.matchDao.updateRobotStatus(account, status);
    }

    public JSONArray getUnFullMatchInfo() {
        return this.matchDao.getUnFullMatchInfo();
    }

    public JSONArray getMatchSection(String platform) {
        return this.matchDao.getMatchSection(platform);
    }
}
