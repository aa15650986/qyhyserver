
package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.PublicBiz;
import com.zhuoan.biz.game.dao.GameDao;
import com.zhuoan.util.Dto;
import com.zhuoan.util.TimeUtil;
import javax.annotation.Resource;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class PublicBizImpl implements PublicBiz {
    @Resource
    GameDao gameDao;

    public PublicBizImpl() {
    }

    public JSONArray getRoomSetting(int gid, String platform, int flag) {
        return this.gameDao.getRoomSetting(gid, platform, flag);
    }

    public JSONObject getSysBaseSet() {
        return this.gameDao.getSysBaseSet();
    }

    public JSONObject getAPPGameSetting() {
        return this.gameDao.getAPPGameSetting();
    }

    public JSONArray getAppObjRec(Long userId, int doType, String gid, String roomid, String roomNo) {
        return this.gameDao.getAppObjRec(userId, doType, gid, roomid, roomNo);
    }

    public void addAppObjRec(JSONObject object) {
        this.gameDao.addAppObjRec(object);
    }

    public JSONArray getNoticeByPlatform(String platform, int type) {
        return this.gameDao.getNoticeByPlatform(platform, type);
    }

    public JSONArray getGoldSetting(JSONObject obj) {
        return this.gameDao.getGoldSetting(obj);
    }

    public JSONObject getUserSignInfo(String platform, long userId) {
        return this.gameDao.getUserSignInfo(platform, userId);
    }

    public int addOrUpdateUserSign(JSONObject obj) {
        return this.gameDao.addOrUpdateUserSign(obj);
    }

    public JSONObject getSignRewardInfoByPlatform(String platform) {
        return this.gameDao.getSignRewardInfoByPlatform(platform);
    }

    public JSONArray getArenaInfo() {
        return this.gameDao.getArenaInfo();
    }

    public void addOrUpdateUserCoinsRec(long userId, int score, int type) {
        String nowDate = TimeUtil.getNowDate("yyyy-MM-dd");
        JSONObject userCoinsRec = this.gameDao.getUserCoinsRecById(userId, type, nowDate + " 00:00:00", nowDate + " 23:59:59");
        JSONObject obj = new JSONObject();
        obj.put("user_id", userId);
        obj.put("type", type);
        obj.put("createTime", TimeUtil.getNowDate());
        if (!Dto.isObjNull(userCoinsRec)) {
            obj.put("id", userCoinsRec.getLong("id"));
            if (score > 0) {
                obj.put("win", userCoinsRec.getInt("win") + 1);
            }

            if (score < 0) {
                obj.put("lose", userCoinsRec.getInt("lose") + 1);
            }

            obj.put("coins", userCoinsRec.getInt("coins") + score);
        } else {
            obj.put("coins", score);
            if (score > 0) {
                obj.put("win", 1);
                obj.put("lose", 0);
            }

            if (score < 0) {
                obj.put("win", 0);
                obj.put("lose", 1);
            }
        }

        this.gameDao.addOrUpdateUserCoinsRec(obj);
    }

    public JSONObject getUserGameInfo(String account) {
        return this.gameDao.getUserGameInfo(account);
    }

    public void addOrUpdateUserGameInfo(JSONObject obj) {
        this.gameDao.addOrUpdateUserGameInfo(obj);
    }

    public void addUserWelfareRec(String account, double sum, int type, int gameId) {
        JSONObject userInfo = this.gameDao.getUserByAccount(account);
        if (!Dto.isObjNull(userInfo)) {
            JSONObject obj = new JSONObject();
            obj.put("userid", userInfo.getLong("id"));
            obj.put("gid", gameId);
            obj.put("type", type);
            obj.put("sum", sum);
            obj.put("doType", 4);
            obj.put("creataTime", TimeUtil.getNowDate());
            if (type + 1 == 3 && userInfo.containsKey("score")) {
                obj.put("pocketOld", userInfo.getInt("score"));
                obj.put("memo", "实物券变动");
            } else if (type + 1 == 4 && userInfo.containsKey("yuanbao")) {
                obj.put("pocketOld", userInfo.getDouble("yuanbao"));
                obj.put("memo", "红包券变动");
            } else if (type + 1 == 1 && userInfo.containsKey("yuanbao")) {
                obj.put("pocketOld", userInfo.getDouble("yuanbao"));
                obj.put("memo", "钻石变动");
            }

            obj.put("pocketChange", sum);
            obj.put("operatorType", 3);
            if (userInfo.containsKey("platform")) {
                obj.put("platform", userInfo.getString("platform"));
            }

            this.gameDao.addUserWelfareRec(obj);
        }

    }

    public JSONObject getAppSettingInfo(String platform) {
        return this.gameDao.getAppSettingInfo(platform);
    }
}
