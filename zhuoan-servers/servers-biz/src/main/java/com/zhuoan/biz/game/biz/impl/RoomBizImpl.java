
package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.game.dao.GameDao;
import com.zhuoan.biz.game.dao.util.BaseSqlUtil;
import com.zhuoan.dao.DBJsonUtil;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.util.Dto;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Resource;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class RoomBizImpl implements RoomBiz {
    @Resource
    private GameDao gameDao;

    public RoomBizImpl() {
    }

    public long insertGameRoom(JSONObject obj) {
        return this.gameDao.insertGameRoom(obj);
    }

    public JSONObject getGameInfoByID(long id) {
        return this.gameDao.getGameInfoByID(id);
    }

    public JSONObject getRoomInfoByRno(String roomNo) {
        return this.gameDao.getRoomInfoByRno(roomNo);
    }

    public boolean updateGameRoom(JSONObject room) {
        String roomNo = room.containsKey("room_no_old") ? room.getString("room_no_old") : room.getString("room_no");
        JSONObject roomInfo = this.gameDao.getRoomInfoByRno(roomNo);
        if (!Dto.isObjNull(roomInfo)) {
            if (room.containsKey("room_no_old")) {
                room.remove("room_no_old");
            }

            if (room.containsKey("is_id") && "1".equals(room.getString("is_id"))) {
                room.remove("is_id");
                BaseSqlUtil.updateDataByMore("za_gamerooms", new String[]{"status"}, new String[]{"-1"}, (String[])null, (Object[])null, " where `id` =" + roomInfo.getLong("id"));
            } else {
                room.put("id", roomInfo.getLong("id"));
            }
        }

        int i = 0;
        if (room.containsKey("id")) {
            i = this.gameDao.updateRoomInfoByRid(room);
        } else {
            this.gameDao.insertGameRoom(room);
        }

        return i > 0;
    }

    public boolean closeGameRoom(String roomNo) {
        JSONObject roomInfo = this.gameDao.getRoomInfoByRno(roomNo);
        JSONObject room = new JSONObject();
        room.put("id", roomInfo.getLong("id"));
        room.put("status", -2);
        int i = this.gameDao.updateRoomInfoByRid(room);
        return i > 0;
    }

    public JSONObject getRoomInfoSeting(int gameID, String optkey) {
        return this.gameDao.getRoomInfoSeting(gameID, optkey);
    }

    public boolean stopJoin(String roomNo) {
        return this.gameDao.updateGameRoomUserId(roomNo);
    }

    public JSONObject getRoomInfoByRnoNotUse(String roomNo) {
        return this.gameDao.getRoomInfoByRnoNotUse(roomNo);
    }

    public JSONObject getGameSetting() {
        return this.gameDao.getGameSetting();
    }

    public JSONArray getRobotArray(int count, double minScore, double maxScore) {
    	System.out.println("??????");
        return this.gameDao.getRobotArray(count, minScore, maxScore);
    }

    public boolean pump(JSONArray userIds, String roomNo, int gid, double fee, String type) {
        return this.gameDao.pump(userIds, roomNo, gid, fee, type);
    }

    public void settlementRoomNo(String roomNo) {
        JSONObject roomInfo = this.getRoomInfoByRno(roomNo);
        if (!Dto.isObjNull(roomInfo) && roomInfo.getInt("roomtype") == 0) {
            if (roomInfo.getInt("game_index") == 1) {
                if (roomInfo.containsKey("paytype") && roomInfo.getInt("paytype") == 1) {
                    List<Long> idList = new ArrayList();
                    int count = 10;

                    String sql4;
                    for(int i = 0; i < count; ++i) {
                        sql4 = "user_id" + i;
                        if (roomInfo.getLong(sql4) > 0L) {
                            idList.add(roomInfo.getLong(sql4));
                        }
                    }

                    JSONObject base_info = roomInfo.getJSONObject("base_info");
                    sql4 = "UPDATE za_users SET roomcard=roomcard- CASE id";
                    String sqlString2 = " END WHERE id IN (";
                    String addSql = "insert into za_userdeduction (userid,roomid,roomNo,gid,type,sum,creataTime) values ";
                    int temp = 0;

                    for(Iterator var10 = idList.iterator(); var10.hasNext(); ++temp) {
                        Long userid = (Long)var10.next();
                        sql4 = sql4 + " WHEN " + userid + " THEN " + base_info.getJSONObject("turn").getInt("AANum");
                        addSql = addSql + "(" + userid + "," + roomInfo.getLong("id") + ",'" + roomNo + "'," + roomInfo.getInt("game_id") + "," + 0 + "," + -base_info.getJSONObject("turn").getInt("AANum") + ",'" + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date()) + "')";
                        if (temp == idList.size() - 1) {
                            sqlString2 = sqlString2 + userid + ")";
                        } else {
                            sqlString2 = sqlString2 + userid + ",";
                            addSql = addSql + ",";
                        }
                    }

                    sql4 = sql4 + sqlString2;
                    DBUtil.executeUpdateBySQL(sql4, new Object[0]);
                    DBUtil.executeUpdateBySQL(addSql, new Object[0]);
                } else {
                    JSONObject base_info = roomInfo.getJSONObject("base_info");
                    int roomcard = base_info.getJSONObject("turn").getInt("roomcard");
                    if (!base_info.getJSONObject("turn").containsKey("noAANum") && base_info.containsKey("player") && base_info.getJSONObject("turn").containsKey("AANum")) {
                        roomcard = base_info.getJSONObject("turn").getInt("AANum") * base_info.getInt("player");
                    }

                    this.gameDao.updateUserRoomCard(-roomcard, roomInfo.getLong("user_id0"));
                    this.gameDao.deductionRoomCardLog(new Object[]{roomInfo.getLong("user_id0"), roomInfo.getInt("id"), roomInfo.getInt("game_id"), roomNo, 0, roomcard, (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date())});
                }
            }
        } else if (!Dto.isObjNull(roomInfo) && roomInfo.getInt("roomtype") == 2 && roomInfo.getInt("game_count") <= roomInfo.getInt("game_index")) {
            this.gameDao.reDaikaiGameRoom(roomNo);
        }

    }

    public void updateRobotStatus(String robotAccount, int status) {
        this.gameDao.updateRobotStatus(robotAccount, status);
    }

    public void increaseRoomIndexByRoomNo(Object roomId) {
        this.gameDao.increaseRoomIndexByRoomNo(roomId);
    }

    public JSONArray getRoomListByClubCode(String clubCode, int gid, int pageIndex, int pageSize) {
        return this.gameDao.getRoomListByClubCode(clubCode, gid, pageIndex, pageSize);
    }

    public void updateRoomQuitUser(JSONObject object) {
        if (object.containsKey("roomNo") && object.containsKey("userIndex")) {
            JSONObject room = new JSONObject();
            room.element("player_number", object.get("player_number")).element("room_no", object.get("roomNo")).element("user_id" + object.get("userIndex"), 0).element("user_icon" + object.get("userIndex"), "").element("user_name" + object.get("userIndex"), "");
            long roomId = object.containsKey("roomId") ? object.getLong("roomId") : 0L;
            if (roomId <= 0L) {
                String sql = "select id from za_gamerooms where room_no= ?  order by id desc";
                JSONObject gameRoom = DBUtil.getObjectBySQL(sql, new Object[]{object.get("roomNo")});
                if (null == gameRoom || !gameRoom.containsKey("id")) {
                    return;
                }

                roomId = gameRoom.getLong("id");
            }

            if (roomId > 0L) {
                room.element("id", roomId);
                this.gameDao.updateRoomInfoByRid(room);
            }

        }
    }

    public long reloadCreateRoom(JSONObject room) {
        long roomId = 0L;
        if (room.containsKey("room_id")) {
            roomId = room.getLong("room_id");
        }

        if (0L >= roomId) {
            if (!room.containsKey("room_no_old")) {
                return -1L;
            }

            JSONObject roomInfo = this.gameDao.getRoomInfoByRno(room.get("room_no_old"));
            if (null == roomInfo || !roomInfo.containsKey("id")) {
                return -1L;
            }

            roomId = roomInfo.getLong("id");
        }

        room.remove("room_no_old");
        room.remove("room_id");
        if (0L < roomId) {
            DBUtil.executeUpdateBySQL("UPDATE za_gamerooms SET `status` = ? WHERE `id` = ? ", new Object[]{2, roomId});
        }

        return DBJsonUtil.addGetId(room, "za_gamerooms");
    }

    public void updateZaGamerooms(JSONObject room) {
        if (room.containsKey("id") && 0L < room.getLong("id")) {
            DBJsonUtil.update(room, "za_gamerooms");
        }
    }
}
