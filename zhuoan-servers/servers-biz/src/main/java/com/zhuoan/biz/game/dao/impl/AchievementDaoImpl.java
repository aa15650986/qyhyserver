
package com.zhuoan.biz.game.dao.impl;

import com.zhuoan.biz.game.dao.AchievementDao;
import com.zhuoan.dao.DBJsonUtil;
import com.zhuoan.dao.DBUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class AchievementDaoImpl implements AchievementDao {
    public AchievementDaoImpl() {
    }

    public JSONArray getAchievementInfoByGameId(int gameId, String platform) {
        String sql = "select id,game_id,achievement_name,achievement_level,min_score,reward,reward_type,platform from za_achievement_info where game_id=? and platform=? order by achievement_level";
        return DBUtil.getObjectListBySQL(sql, new Object[]{gameId, platform});
    }

    public JSONObject getAchievementInfoById(long id) {
        String sql = "select id,game_id,achievement_name,achievement_level,min_score,reward,reward_type,platform from za_achievement_info where id=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{id});
    }

    public JSONArray getUserAchievementByAccount(String account) {
        String sql = "select id,user_account,user_img,user_name,user_sign,game_id,achievement_score,reward_level,achievement_id,achievement_name,reward_array,draw_array from za_user_achievement where user_account=?";
        return DBUtil.getObjectListBySQL(sql, new Object[]{account});
    }

    public JSONObject getUserAchievementByAccountAndGameId(String account, int gameId) {
        String sql = "select id,user_account,user_img,user_name,user_sign,game_id,achievement_score,reward_level,achievement_id,achievement_name,reward_array,draw_array from za_user_achievement where user_account=? and game_id=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{account, gameId});
    }

    public void addOrUpdateUserAchievement(JSONObject userAchievement) {
        DBJsonUtil.saveOrUpdate(userAchievement, "za_user_achievement");
    }

    public JSONArray getAchievementRank(int limit, int gameId) {
        String sql = "select user_account,user_img,user_name,user_sign,achievement_score,achievement_name from za_user_achievement where game_id=? order by achievement_score DESC limit 0,?";
        return DBUtil.getObjectListBySQL(sql, new Object[]{gameId, limit});
    }
}
