
package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.AchievementBiz;
import com.zhuoan.biz.game.dao.AchievementDao;
import com.zhuoan.biz.game.dao.GameDao;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.util.Dto;
import javax.annotation.Resource;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class AchievementBizImpl implements AchievementBiz {
    @Resource
    private RedisService redisService;
    @Resource
    private AchievementDao achievementDao;
    @Resource
    private GameDao gameDao;

    public AchievementBizImpl() {
    }

    public JSONArray getAchievementInfoByGameId(int gameId, String platform) {
        StringBuffer key = new StringBuffer("achievement_info_");
        key.append(platform);
        key.append("_");
        key.append(gameId);

        JSONArray achievementInfo;
        try {
            Object object = this.redisService.queryValueByKey(String.valueOf(key));
            if (object != null) {
                achievementInfo = JSONArray.fromObject(object);
            } else {
                achievementInfo = this.achievementDao.getAchievementInfoByGameId(gameId, platform);
                this.redisService.insertKey(String.valueOf(key), String.valueOf(achievementInfo), (Long)null);
            }
        } catch (Exception var6) {
            achievementInfo = this.achievementDao.getAchievementInfoByGameId(gameId, platform);
        }

        return achievementInfo;
    }

    public JSONObject getAchievementInfoById(long id) {
        return this.achievementDao.getAchievementInfoById(id);
    }

    public JSONArray getUserAchievementByAccount(String account) {
        return this.achievementDao.getUserAchievementByAccount(account);
    }

    public JSONObject getUserAchievementByAccountAndGameId(String account, int gameId) {
        return this.achievementDao.getUserAchievementByAccountAndGameId(account, gameId);
    }

    public JSONObject addOrUpdateUserAchievement(String account, int gameId, int achievementScore) {
        JSONObject levelUp = new JSONObject();
        JSONObject userInfo = this.gameDao.getUserByAccount(account);
        if (!Dto.isObjNull(userInfo)) {
            JSONObject userAchievement = this.achievementDao.getUserAchievementByAccountAndGameId(account, gameId);
            JSONObject obj = new JSONObject();
            if (!Dto.isObjNull(userAchievement)) {
                obj.put("id", userAchievement.getLong("id"));
                achievementScore = (int)((long)achievementScore + userAchievement.getLong("achievement_score"));
            }

            if (userInfo.containsKey("name")) {
                obj.put("user_name", userInfo.getString("name"));
            }

            if (userInfo.containsKey("headimg")) {
                obj.put("user_img", "http://game.88huhu.cn/zagame" + userInfo.getString("headimg"));
            }

            if (userInfo.containsKey("sign")) {
                obj.put("user_sign", userInfo.getString("sign"));
            }

            obj.put("user_account", account);
            obj.put("game_id", gameId);
            obj.put("achievement_score", achievementScore);
            JSONArray achievementInfo = this.getAchievementInfoByGameId(gameId, userInfo.getString("platform"));

            for(int i = 0; i < achievementInfo.size(); ++i) {
                JSONObject achievement = achievementInfo.getJSONObject(i);
                if (achievement.getInt("min_score") == achievementScore) {
                    obj.put("achievement_id", achievement.getLong("id"));
                    obj.put("achievement_name", achievement.getString("achievement_name"));
                    JSONArray rewardArray;
                    if (!Dto.isObjNull(userAchievement)) {
                        rewardArray = userAchievement.getJSONArray("reward_array");
                        rewardArray.add(achievement.getLong("id"));
                        obj.put("reward_array", rewardArray);
                    } else {
                        rewardArray = new JSONArray();
                        rewardArray.add(achievement.getLong("id"));
                        obj.put("reward_array", rewardArray);
                        obj.put("draw_array", new JSONArray());
                    }

                    levelUp = achievement;
                    break;
                }
            }

            this.achievementDao.addOrUpdateUserAchievement(obj);
        }

        return levelUp;
    }

    public JSONArray getAchievementRank(int limit, int gameId) {
        return this.achievementDao.getAchievementRank(limit, gameId);
    }

    public void updateUserAchievement(JSONObject userAchievement) {
        this.achievementDao.addOrUpdateUserAchievement(userAchievement);
    }
}
