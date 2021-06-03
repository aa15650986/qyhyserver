
package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.GameLogBiz;
import com.zhuoan.biz.game.dao.GameDao;
import com.zhuoan.biz.game.dao.util.BaseSqlUtil;
import com.zhuoan.dao.DBJsonUtil;
import com.zhuoan.service.cache.RedisService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Resource;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class GameLogBizImpl implements GameLogBiz {
    @Resource
    private GameDao gameDao;
    @Resource
    private RedisService redisService;

    public GameLogBizImpl() {
    }

    public int addOrUpdateGameLog(JSONObject gamelog) {
        return this.gameDao.addOrUpdateGameLog(gamelog);
    }

    public long getGameLogId(long room_id, int game_index) {
        return this.gameDao.getGameLogId(room_id, game_index);
    }

    public void addUserGameLog(JSONArray data, JSONObject usergamelogResult) {
        if (null != data && data.size() != 0 && null != usergamelogResult) {
            ArrayList<String> columnList = new ArrayList();
            JSONObject jsonObject = data.getJSONObject(0);
            Iterator it = jsonObject.keys();

            while(it.hasNext()) {
                String key = (String)it.next();
                columnList.add(key);
            }

            List<Object[]> insertList = new ArrayList();

            for(int i = 0; i < data.size(); ++i) {
                JSONObject o = data.getJSONObject(i);
                if (o != null) {
                    ArrayList list = new ArrayList();
                    boolean b = true;

                    for(int j = 0; j < columnList.size(); ++j) {
                        if (null == o.get((String)columnList.get(j))) {
                            b = false;
                            break;
                        }

                        list.add(o.get((String)columnList.get(j)));
                    }

                    if (b) {
                        insertList.add(list.toArray(new Object[]{list.size()}));
                    }
                }
            }

            if (columnList != null && columnList.size() > 0) {
                BaseSqlUtil.insertData("za_usergamelogs", (String[])columnList.toArray(new String[columnList.size()]), insertList);
                DBJsonUtil.add(usergamelogResult, "za_users_game_logs_result");
            }

        }
    }

    public void updateGamelogs(String roomNo, int gid, JSONArray jsonArray) {
        this.gameDao.updateGamelogs(roomNo, gid, jsonArray);
    }

    public JSONArray getUserGameLogList(String account, int gid, int num, String createTime) {
        return this.gameDao.getUserGameLogList(account, gid, num, createTime);
    }

    public JSONArray getUserGameLogsByUserId(long userId, int gameId, int roomType) {
        Object userGameLogList = this.redisService.hget("user_game_log_list:" + gameId + roomType, String.valueOf(userId));
        if (userGameLogList != null) {
            return JSONArray.fromObject(userGameLogList);
        } else {
            JSONArray userGameLogsByUserId = this.gameDao.getUserGameLogsByUserId(userId, gameId, roomType);
            this.redisService.hset("user_game_log_list:" + gameId + roomType, String.valueOf(userId), String.valueOf(userGameLogsByUserId));
            return userGameLogsByUserId;
        }
    }

    public JSONArray getUserGameRoomByRoomType(long userId, int gameId, int roomType) {
        return this.gameDao.getUserGameRoomByRoomType(userId, gameId, roomType);
    }

    public JSONArray getUserGameLogsByUserId(long userId, int gameId, int roomType, List<String> roomList, String clubCode, String roomId) {
        return this.gameDao.getUserGameLogsByUserId(userId, gameId, roomType, roomList, clubCode, roomId);
    }

    public JSONObject pageUserGameLogsByUserId(Long userId, Integer gameId, String circleCode, Object[] userIds, int[] roomType, String time, int pageIndex, int pageSize) {
        return this.gameDao.pageUserGameLogsByUserId(userId, gameId, circleCode, userIds, roomType, time, pageIndex, pageSize);
    }

    public JSONArray getRecordInfoByUser(int[] roomType, String roomId, Integer gameId, String roomNo, String circleCode) {
        return this.gameDao.getRecordInfoByUser(roomType, roomId, gameId, roomNo, circleCode);
    }

    public JSONArray getUserGameLogList(Object rommId, Object roomNo) {
        return this.gameDao.getUserGameLogList(rommId, roomNo);
    }

    public JSONObject getRoomcardSpend(Object rommId, Object roomNo) {
        return this.gameDao.getRoomcardSpend(rommId, roomNo);
    }

    public JSONObject getRoomcardAllSpend(Object clubId, Object clubType, String time, List userIds) {
        return this.gameDao.getRoomcardAllSpend(clubId, clubType, time, userIds);
    }

    public JSONObject getGameAllCount(Object clubCode, int[] roomType, String time, List userIds, String platform) {
        return this.gameDao.getGameAllCount(clubCode, roomType, time, userIds, platform);
    }

    public JSONObject getUserGameLogsByUserIdAndRoomNo(long userId, String roomNo, String gameCount) {
        return this.gameDao.getUserGameCardLog(userId, roomNo, 4, gameCount);
    }

    public JSONObject getRecordInfo(long userId, String time) {
        return this.gameDao.getRecordInfo(userId, time);
    }
}
