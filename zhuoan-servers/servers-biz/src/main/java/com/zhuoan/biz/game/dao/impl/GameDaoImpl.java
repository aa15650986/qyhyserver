
package com.zhuoan.biz.game.dao.impl;

import com.zhuoan.biz.game.biz.CircleBiz;
import com.zhuoan.biz.game.dao.GameDao;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.dao.DBJsonUtil;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.util.DateUtils;
import com.zhuoan.util.Dto;
import com.zhuoan.util.TimeUtil;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class GameDaoImpl implements GameDao {
    @Resource
    private CircleBiz circleBiz;

    public GameDaoImpl() {
    }

    public long insertGameRoom(JSONObject obj) {
        return DBJsonUtil.addGetId(obj, "za_gamerooms");
    }

    public JSONObject getUserByID(long id) {
        String sql = "select id,account,name,password,tel,sex,headimg,area,lv,roomcard,coins,score,createtime,ip,logintime,openid,unionid,uuid,status,isAuthentication,memo,vip,safe,luck,safeprice,yuanbao,operatorMark,isManag,Losevalue,wholecost,sign,isown,platform,pumpVal,clubIds,parUserId from za_users where id=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{id});
    }

    public JSONObject getUserByAccount(String account) {
        String sql = "select id,account,name,password,tel,sex,headimg,area,lv,roomcard,coins,score,createtime,ip,logintime,openid,unionid,uuid,status,isAuthentication,memo,vip,safe,luck,safeprice,yuanbao,operatorMark,isManag,Losevalue,wholecost,sign,isown,platform,gulidId,pumpVal,Identification,clubIds,parUserId from za_users where account=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{account});
    }

    public JSONObject getUserInfoByTel(String tel) {
        String sql = "select id,gulidId,password,tel,roomcard,coins,score,yuanbao,parUserId,parUsers from za_users where tel=? and status=1";
        return DBUtil.getObjectBySQL(sql, new Object[]{tel});
    }

    public int updateUserInfo(JSONObject userInfo) {
        return DBJsonUtil.saveOrUpdate(userInfo, "za_users");
    }

    public int deleteUserInfoById(long id) {
        String sql = "delete from za_users where id=?";
        return DBUtil.executeUpdateBySQL(sql, new Object[]{id});
    }

    public boolean updateUserBalance(JSONArray data, String types) {
        String sql = "update za_users SET " + types + " = CASE id  $ END WHERE id IN (/)";
        String z = "";
        String d = "";

        for(int i = 0; i < data.size(); ++i) {
            JSONObject uuu = data.getJSONObject(i);
            if (uuu.getDouble("total") <= 0.0D) {
                z = z + " WHEN " + uuu.getLong("id") + " THEN 0";
            } else {
                z = z + " WHEN " + uuu.getLong("id") + " THEN " + types + "+" + uuu.getDouble("fen");
            }

            d = d + uuu.getLong("id") + ",";
        }

        DBUtil.executeUpdateBySQL(sql.replace("$", z).replace("/", d.substring(0, d.length() - 1)), new Object[0]);
        return false;
    }

    public void insertUserdeduction(JSONObject obj) {
        StringBuffer sqlx = new StringBuffer();
        sqlx.append("insert into za_userdeduction(userid,gid,roomNo,type,sum,creataTime,pocketNew,pocketOld,pocketChange,operatorType,memo) values $");
        String ve = "";
        String te = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date());
        JSONArray jsoaArray = obj.getJSONArray("user");

        for(int i = 0; i < jsoaArray.size(); ++i) {
            JSONObject uuu = jsoaArray.getJSONObject(i);
            ve = ve + "(" + uuu.getLong("id") + "," + uuu.getInt("gid") + ",'" + uuu.getString("roomNo") + "'," + uuu.getInt("type") + "," + uuu.getDouble("fen") + ",'" + te + "'," + uuu.getDouble("new") + "," + uuu.getDouble("old") + "," + uuu.getDouble("fen") + "," + 20 + ",'游戏记录'),";
        }

        DBUtil.executeUpdateBySQL(sqlx.toString().replace("$", ve.substring(0, ve.length() - 1)), new Object[0]);
    }

    public JSONArray getYbUpdateLog() {
        String sql = "select id,account,yuanbao,status from za_yb_update where status=0";
        JSONArray objectListBySQL = DBUtil.getObjectListBySQL(sql, new Object[0]);
        return objectListBySQL;
    }

    public void delYbUpdateLog(long id) {
        String sql = "update za_yb_update set status=-1 where id=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{id});
    }

    public JSONObject getGongHui(JSONObject userInfo) {
        String sql = "select id,code,name,isUse,platform from guild where id=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{userInfo.getLong("gulidId")});
    }

    public JSONObject getGameInfoByID(long id) {
        String sql = "select id,name,logo,type,gameType,status,setting,isUse,clearTime from za_games where id=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{id});
    }

    public JSONObject getRoomInfoByRno(Object roomNo) {
        String sql = "select * from za_gamerooms where room_no=? order by id desc";
        return DBUtil.getObjectBySQL(sql, new Object[]{roomNo});
    }

    public int updateRoomInfoByRid(JSONObject roominfo) {
        return DBJsonUtil.update(roominfo, "za_gamerooms");
    }

    public JSONObject getRoomInfoSeting(int gameID, String optkey) {
        String sql = "select id,game_id,opt_key,opt_name,opt_val,is_mul,is_use,createTime,memo,sort,is_open from za_gamesetting where game_id=? and opt_key=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{gameID, optkey});
    }

    public boolean updateGameRoomUserId(String roomNo) {
        JSONObject roomInfo = this.getRoomInfoByRno(roomNo);
        if (!Dto.isObjNull(roomInfo)) {
            StringBuffer sql = new StringBuffer("update za_gamerooms set ");
            List<Integer> paramList = new ArrayList();
            int maxCount = 10;

            int i;
            for(i = 0; i < maxCount; ++i) {
                if (roomInfo.getInt("user_id" + i) == 0) {
                    sql.append("user_id" + i + "=? ");
                    if (i != maxCount - 1) {
                        sql.append(",");
                    }

                    paramList.add(-1);
                }
            }

            if (paramList.size() > 0) {
                sql.append("where id=?");
                paramList.add(roomInfo.getInt("id"));
                i = DBUtil.executeUpdateBySQL(sql.toString(), paramList.toArray());
                if (i > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    public JSONObject getRoomInfoByRnoNotUse(String roomNo) {
        String sql = "select id,server_id,game_id,room_no,roomtype,base_info,createtime,game_count,game_index,game_score,game_coins,user_id0,user_icon0,user_name0,user_score0,user_id1,user_icon1,user_name1,user_score1,user_id2,user_icon2,user_name2,user_score2,user_id3,user_icon3,user_name3,user_score3,user_id4,user_icon4,user_name4,user_score4,user_id5,user_icon5,user_name5,user_score5,ip,port,status,paytype,level,fangzhu,user_id6,user_icon6,user_name6,user_score6,user_id7,user_icon7,user_name7,user_score7,user_id8,user_icon8,user_name8,user_score8,user_id9,user_icon9,user_name9,user_score9,stoptime,open from za_gamerooms where room_no=? and status<0 order by id desc";
        return DBUtil.getObjectBySQL(sql, new Object[]{roomNo});
    }

    public JSONObject getGameSetting() {
        String sql = "select id,isXipai,xipaiObj,xipaiLayer,xipaiCount,bangObj,bangData,pumpData,bangCount from app_game_setting";
        return DBUtil.getObjectBySQL(sql, new Object[0]);
    }

    public JSONArray getRobotArray(int count, double minScore, double maxScore) {
    	System.out.println(1111111);
        String sql = "select account,uuid from za_users where openid='0' and isuse=0 and area='AI' and yuanbao>=? ";
        if (maxScore > minScore) {
            sql = sql + " and yuanbao<" + maxScore;
        }

        sql = sql + " limit ?,?";
        System.out.println("sql"+sql);
        JSONArray robotArray = DBUtil.getObjectListBySQL(sql, new Object[]{0, 0, count});

        if (robotArray.size()<count) {
			System.out.println("没有人机可用，恢复人机使用情况");
			String resetSql = "update za_users set isuse=0 where area = 'AI'";
	        DBUtil.executeUpdateBySQL(resetSql, new Object[]{});
	        return   getRobotArray( count,  minScore,  maxScore);
	        
		}
        
        for(int i = 0; i < robotArray.size(); ++i) {
            sql = "update za_users set isuse=1 where account=?";
            DBUtil.executeUpdateBySQL(sql, new Object[]{robotArray.getJSONObject(i).getString("account")});
        }

        return robotArray;
    }

    public boolean pump(JSONArray userIds, String roomNo, int gid, double fee, String type) {
        String sql = "select id from za_gamerooms where room_no=?";
        long roomId;
        JSONObject roominfo;
        if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
            if (((GameRoom)RoomManage.gameRoomMap.get(roomNo)).getId() == 0L) {
                roominfo = DBUtil.getObjectBySQL(sql, new Object[]{roomNo});
                roomId = roominfo.getLong("id");
                ((GameRoom)RoomManage.gameRoomMap.get(roomNo)).setId(roomId);
            } else {
                roomId = ((GameRoom)RoomManage.gameRoomMap.get(roomNo)).getId();
            }
        } else {
            roominfo = DBUtil.getObjectBySQL(sql, new Object[]{roomNo});
            roomId = roominfo.getLong("id");
        }

        JSONArray users = JSONArray.fromObject(userIds);
        int userCount = users.size();
        Object[] params = new Object[userCount];
        sql = "select id,platform,roomcard,coins,yuanbao from za_users where id in(";

        for(int i = 0; i < userCount; ++i) {
            params[i] = users.getString(i);
            sql = sql + "?";
            if (i < userCount - 1) {
                sql = sql + ",";
            } else {
                sql = sql + ")";
            }
        }

        JSONArray objectListBySQL = DBUtil.getObjectListBySQL(sql, params);
        String platform = "";
        int type1 = 0;
        String nowTime = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date());
        if (type.equals("roomcard")) {
            type1 = 0;
        } else if (type.equals("coins")) {
            type1 = 1;
        } else if (type.equals("yuanbao")) {
            type1 = 3;
        }

        sql = "UPDATE za_users SET " + type + "=(" + type + " - CASE id";
        String sqlString2 = " END )WHERE $ IN (";
        String addSql = "insert into za_userdeduction (userid,roomid,roomNo,gid,type,sum,doType,creataTime,memo,platform,pocketNew,pocketOld,pocketChange,operatorType) values ";

        for(int i = 0; i < objectListBySQL.size(); ++i) {
            JSONObject user = objectListBySQL.getJSONObject(i);
            long uid = user.getLong("id");
            if (user.containsKey("platform") && !Dto.stringIsNULL(user.getString("platform"))) {
                platform = user.getString("platform");
            }

            double pocketOld = 0.0D;
            double pocketNew = 0.0D;
            if (type.equals("yuanbao")) {
                if (user.getDouble(type) < Double.parseDouble(String.valueOf(fee))) {
                    fee = user.getDouble("yuanbao");
                }

                pocketOld = user.getDouble("yuanbao");
                pocketNew = Dto.sub(pocketOld, fee);
            } else if (type.equals("roomcard")) {
                if ((double)user.getInt(type) < Double.parseDouble(String.valueOf(fee))) {
                    fee = (double)user.getInt("roomcard");
                }

                pocketOld = user.getDouble("roomcard");
                pocketNew = Dto.sub(pocketOld, fee);
            } else if (type.equals("coins")) {
                if (user.getDouble(type) < Double.parseDouble(String.valueOf(fee))) {
                    fee = user.getDouble("coins");
                }

                pocketOld = user.getDouble("coins");
                pocketNew = Dto.sub(pocketOld, fee);
            }

            String memo = "";
            if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
                if (((GameRoom)RoomManage.gameRoomMap.get(roomNo)).getRoomType() == 0) {
                    memo = "房卡场扣房卡";
                } else if (((GameRoom)RoomManage.gameRoomMap.get(roomNo)).getRoomType() == 3) {
                    memo = "元宝场抽水";
                }
            }

            sql = sql + " WHEN " + uid + " THEN " + fee;
            addSql = addSql + "(" + uid + "," + roomId + ",'" + roomNo + "'," + gid + "," + type1 + "," + -fee + "," + 2 + ",'" + nowTime + "','" + memo + "','" + platform + "'," + pocketNew + "," + pocketOld + "," + fee + "," + 10 + ")";
            if (i == objectListBySQL.size() - 1) {
                sqlString2 = sqlString2 + uid + ")";
            } else {
                sqlString2 = sqlString2 + uid + ",";
                addSql = addSql + ",";
            }
        }

        sql = sql + sqlString2.replace("$", "id");
        Object[] objects = new Object[0];
        DBUtil.executeUpdateBySQL(sql, objects);
        if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null && ((GameRoom)RoomManage.gameRoomMap.get(roomNo)).getRoomType() != 1) {
            DBUtil.executeUpdateBySQL(addSql, objects);
        }

        return false;
    }

    public void updateUserRoomCard(int roomCard, long userId) {
        String sql = "update za_users set roomcard=roomcard+? where id=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{roomCard, userId});
    }

    public void deductionRoomCardLog(Object[] data) {
        String sql1 = "insert into za_userdeduction(userid,roomid,gid,roomNo,type,sum,creataTime) values(?,?,?,?,?,?,?)";
        DBUtil.executeUpdateBySQL(sql1, data);
    }

    public int addOrUpdateGameLog(JSONObject gamelog) {
        int gameLogId = 0;
        DBJsonUtil.add(gamelog, "za_gamelogs");
        return gameLogId;
    }

    public long getGameLogId(long room_id, int game_index) {
        String sql = "select id from za_gamelogs where room_id=? and game_index=?";
        JSONObject result = DBUtil.getObjectBySQL(sql, new Object[]{room_id, game_index});
        return result != null && result.has("id") ? result.getLong("id") : -1L;
    }

    public int addUserGameLog(JSONObject usergamelog) {
        return DBJsonUtil.add(usergamelog, "za_usergamelogs");
    }

    public JSONObject getSysUser(String adminCode, String adminPass, String memo) {
        String sql = "select id from sys_admin where adminCode=? and adminPass=? and memo=?";
        JSONObject sysUserInfo = DBUtil.getObjectBySQL(sql, new Object[]{adminCode, adminPass, memo});
        return sysUserInfo;
    }

    public JSONArray getUserGameLogsByUserId(long userId, int gameId, int roomType) {
        String sql = "SELECT id,room_no,createtime,result,gamelog_id FROM `za_usergamelogs` where user_id=? and gid=? and room_type=? ORDER BY id DESC LIMIT 0,20";
        return TimeUtil.transTimestamp(DBUtil.getObjectListBySQL(sql, new Object[]{userId, gameId, roomType}), "createtime", "yyyy-MM-dd HH:mm:ss");
    }

    public JSONObject pageUserGameLogsByUserId(Long userId, Integer gameId, String circleCode, Object[] userIds, int[] roomType, String time, int pageIndex, int pageSize) {
        StringBuilder sb = new StringBuilder();
        sb.append(" za_user_game_statis z WHERE  (DATEDIFF(z.`create_time`,?)=0)");
        List list = new ArrayList();
        list.add(time);
        if (null != circleCode) {
            sb.append(" AND z.`circle_code`= ? ");
            list.add(circleCode);
        }

        if (null != gameId) {
            sb.append(" AND z.`za_games_id`= ? ");
            list.add(gameId);
        }

        if (null != userId && userId > 0L) {
            sb.append("   AND z.`user_id` = ? ");
            list.add(userId);
        }

        int i;
        if (userIds != null) {
            sb.append(" AND z.`user_id` in(");

            for(i = 0; i < userIds.length; ++i) {
                sb.append("?");
                list.add(userIds[i]);
                if (i != userIds.length - 1) {
                    sb.append(",");
                }
            }

            sb.append(") ");
        }

        if (null != roomType && roomType.length > 0) {
            sb.append(" AND z.`room_type` in(");

            for(i = 0; i < roomType.length; ++i) {
                sb.append("?");
                list.add(roomType[i]);
                if (i != roomType.length - 1) {
                    sb.append(",");
                }
            }

            sb.append(") ");
        }

        sb.append(" GROUP BY z.`room_no`,z.`room_id` ORDER BY z.`create_time` DESC");
        return DBUtil.getObjectPageBySQL("z.`game_sum`,z.`za_games_id` as gid,z.`room_type`,z.`room_id`,z.`room_no`,z.`circle_code`,z.`create_time`", sb.toString(), list.toArray(), pageIndex, pageSize);
    }

    public JSONObject getSysBaseSet() {
        String sql = "SELECT appShareFrequency,appShareCircle,appShareFriend,cardname,coinsname,yuanbaoname,appShareObj FROM sys_base_set WHERE id=1";
        return DBUtil.getObjectBySQL(sql, new Object[0]);
    }

    public JSONObject getAPPGameSetting() {
        String sql = "SELECT isXipai,xipaiLayer,xipaiCount,xipaiObj,bangData FROM app_game_setting WHERE id=1";
        return DBUtil.getObjectBySQL(sql, new Object[0]);
    }

    public JSONArray getAppObjRec(Long userId, int doType, String gid, String roomid, String roomNo) {
        String sql = "SELECT id FROM za_userdeduction WHERE userid=? AND gid=? AND roomid=? AND roomNo=? AND doType=?";
        return DBUtil.getObjectListBySQL(sql, new Object[]{userId, gid, roomid, roomNo, doType});
    }

    public void addAppObjRec(JSONObject object) {
        String sql = "insert into za_userdeduction(userid,gid,roomNo,doType,roomid,creataTime,pocketNew,pocketOld,pocketChange,operatorType,memo,sum,platform,type) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        DBUtil.executeUpdateBySQL(sql, new Object[]{object.getLong("userId"), object.getInt("gameId"), object.getString("room_no"), 1, object.getLong("room_id"), (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date()), object.getDouble("new"), object.getDouble("old"), object.getDouble("change"), 30, "洗牌", object.getDouble("change"), object.getString("platform"), object.getInt("type")});
    }

    public void updateUserPump(String account, String type, double sum) {
        StringBuffer sb = new StringBuffer();
        sb.append("update za_users set pumpVal=pumpVal+1,");
        sb.append(type);
        sb.append("=");
        sb.append(type);
        sb.append("+? where account=?");
        DBUtil.executeUpdateBySQL(String.valueOf(sb), new Object[]{sum, account});
    }

    public void updateGamelogs(String roomNo, int gid, JSONArray jsonArray) {
        String sql = "select id from za_gamelogs where room_no=? and gid=? ORDER BY game_index DESC limit 1";
        JSONObject objectBySQL = DBUtil.getObjectBySQL(sql, new Object[]{roomNo, gid});
        if (!Dto.isObjNull(objectBySQL)) {
            sql = "update za_gamelogs set jiesuan=? where id=?";
            DBUtil.executeUpdateBySQL(sql, new Object[]{jsonArray.toString(), objectBySQL.getLong("id")});
        }

    }

    public JSONArray getUserGameLogList(String account, int gid, int num, String createTime) {
        String sql = "select id from za_users where account=?";
        JSONObject objectBySQL = DBUtil.getObjectBySQL(sql, new Object[]{account});
        if (!Dto.isObjNull(objectBySQL)) {
            if (Dto.stringIsNULL(createTime)) {
                sql = " select id,gid,user_id,gamelog_id,result,createtime,room_no from za_usergamelogs where gid=? and user_id=? GROUP BY gamelog_id  order by id desc LIMIT ?";
                return TimeUtil.transTimestamp(DBUtil.getObjectListBySQL(sql, new Object[]{gid, objectBySQL.getLong("id"), num}), "createtime", "yyyy-MM-dd hh:mm:ss");
            } else {
                sql = " select id,gid,user_id,gamelog_id,result,createtime,room_no from za_usergamelogs where gid=? and user_id=? and createtime<? GROUP BY gamelog_id  order by id desc LIMIT ?";
                return TimeUtil.transTimestamp(DBUtil.getObjectListBySQL(sql, new Object[]{gid, objectBySQL.getLong("id"), createTime, num}), "createtime", "yyyy-MM-dd hh:mm:ss");
            }
        } else {
            return null;
        }
    }

    public boolean reDaikaiGameRoom(String roomNo) {
        try {
            String sql = "select server_id,game_id,base_info,ip,port,game_count,paytype,stoptime,fangzhu from za_gamerooms where room_no=? order by id desc";
            JSONObject roomInfo = DBUtil.getObjectBySQL(sql, new Object[]{roomNo});
            if (roomInfo.containsKey("stoptime") && !Dto.stringIsNULL(roomInfo.getString("stoptime")) && roomInfo.getInt("roomtype") == 2 && roomInfo.getInt("status") < 0) {
                TimeUtil.transTimeStamp(roomInfo, "yyyy-MM-dd HH:mm:ss", "stoptime");
                String stoptime = roomInfo.getString("stoptime");
                String newtime = DateUtils.getTimestamp().toString();
                System.out.println("进入代开房间重开方法stoptime：" + stoptime + ":newtime" + newtime);
                boolean result = TimeUtil.isLatter(newtime, stoptime);
                if (!result) {
                    sql = "insert into za_gamerooms(roomtype,server_id,game_id,room_no,base_info,createtime,ip,port,status,game_count,paytype,stoptime,fangzhu) values(?,?,?,?,?,?,?,?,?,?,?,?,?)";
                    Object[] params = new Object[]{2, roomInfo.getInt("server_id"), roomInfo.getInt("game_id"), roomNo, roomInfo.getString("base_info"), new Date(), roomInfo.getString("ip"), roomInfo.getInt("port"), 0, roomInfo.getInt("game_count"), roomInfo.getInt("paytype"), stoptime, roomInfo.getInt("fangzhu")};
                    DBUtil.executeUpdateBySQL(sql, params);
                }
            }

            return true;
        } catch (Exception var9) {
            return false;
        }
    }

    public JSONArray getRoomSetting(int gid, String platform, int flag) {
        String sql = "select id,game_id,opt_key,opt_name,opt_val,is_mul,is_use,createTime,memo,sort,is_open from za_gamesetting where is_use=? and is_open=0 and game_id=? and memo=? order by sort";
        JSONArray gameSetting = DBUtil.getObjectListBySQL(sql, new Object[]{flag, gid, platform});
        return gameSetting;
    }

    public JSONArray getNoticeByPlatform(String platform, int type) {
        String sql = "SELECT id,title,con,image,strcreateTime,endcreateTime,showType,createTime FROM za_message WHERE `status`=1 AND type=? AND platform=? ORDER BY createTime DESC";
        return DBUtil.getObjectListBySQL(sql, new Object[]{type, platform});
    }

    public JSONArray getGoldSetting(JSONObject obj) {
        String sql = "SELECT di,`option`,online,memo FROM za_gamegoldsetting WHERE game_id=? AND platform=?";
        return DBUtil.getObjectListBySQL(sql, new Object[]{obj.getInt("gameId"), obj.getString("platform")});
    }

    public JSONObject getUserSignInfo(String platform, long userId) {
        String sql = "SELECT id,userID,singnum,createtime,platform from sys_sign where userID=? AND platform=?";
        JSONObject signInfo = DBUtil.getObjectBySQL(sql, new Object[]{userId, platform});
        return Dto.isObjNull(signInfo) ? signInfo : TimeUtil.transTimeStamp(signInfo, "yyyy-MM-dd HH:mm:ss", "createtime");
    }

    public int addOrUpdateUserSign(JSONObject obj) {
        return DBJsonUtil.saveOrUpdate(obj, "sys_sign");
    }

    public JSONObject getSignRewardInfoByPlatform(String platform) {
        String sql = "SELECT signin_base,signin_min,signin_max from operator_appsetting where platform=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{platform});
    }

    public void updateRobotStatus(String robotAccount, int status) {
        String sql = "update za_users set status=? where account=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{status, robotAccount});
    }

    public JSONArray getArenaInfo() {
        String sql = "select endTime,startTime,day,hour,isOpen,description,explanation,status,name,id,gid,memo from za_arena where status=1";
        return DBUtil.getObjectListBySQL(sql, new Object[0]);
    }

    public JSONObject getUserCoinsRecById(long userId, int type, String startTime, String endTime) {
        String sql = "SELECT id,win,lose,coins FROM za_coins_rec WHERE user_id=? AND type=? AND createTime>=? AND createTime<=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{userId, type, startTime, endTime});
    }

    public int addOrUpdateUserCoinsRec(JSONObject obj) {
        return DBJsonUtil.saveOrUpdate(obj, "za_coins_rec");
    }

    public JSONObject getUserGameInfo(String account) {
        String sql = "SELECT id,account,update_time,treasure_history,shuffle_times FROM za_user_game_info WHERE account=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{account});
    }

    public void addOrUpdateUserGameInfo(JSONObject obj) {
        DBJsonUtil.saveOrUpdate(obj, "za_user_game_info");
    }

    public void addUserTicketRec(JSONObject ticketRec) {
        DBJsonUtil.add(ticketRec, "za_user_ticket_rec");
    }

    public void addUserWelfareRec(JSONObject obj) {
        DBJsonUtil.add(obj, "za_userdeduction");
    }

    public JSONArray getUserGameRoomByRoomType(long userId, int gameId, int roomType) {
        String sql = "SELECT id,room_no,game_count,createtime FROM za_gamerooms where game_id=? and roomtype=? and (user_id0=? or user_id1=? or user_id2=? or user_id3=? or user_id4=? or user_id5=? or user_id6=? or user_id7=? or user_id8=? or user_id9=?) order by id desc LIMIT 20";
        JSONArray roomList = DBUtil.getObjectListBySQL(sql, new Object[]{gameId, roomType, userId, userId, userId, userId, userId, userId, userId, userId, userId, userId});
        return !Dto.isNull(roomList) ? TimeUtil.transTimestamp(roomList, "createtime", "yyyy-MM-dd HH:mm:ss") : roomList;
    }

    public JSONArray getUserGameLogsByUserId(long userId, int gameId, int roomType, List<String> roomList, String clubCode, String roomId) {
        String sql = "SELECT id,room_no,room_id,result,gamelog_id,game_index FROM `za_usergamelogs` where gid=? and room_type=? ";
        if (null != roomId) {
            sql = sql + "and room_id='" + roomId + "'";
        }

        if (roomList.size() > 0) {
            sql = sql + " and room_no in(";

            for(int i = 0; i < roomList.size(); ++i) {
                sql = sql + (String)roomList.get(i);
                if (i < roomList.size() - 1) {
                    sql = sql + ",";
                }
            }

            sql = sql + ")";
        }

        if (!Dto.stringIsNULL(clubCode)) {
            sql = sql + " and club_code='" + clubCode + "'";
        }

        sql = sql + " GROUP BY gamelog_id ORDER BY id ASC";
        return DBUtil.getObjectListBySQL(sql, new Object[]{gameId, roomType});
    }

    public void increaseRoomIndexByRoomNo(Object roomId) {
        JSONObject object = DBUtil.getObjectBySQL("SELECT `status` FROM za_gamerooms  WHERE `id`= ? ", new Object[]{roomId});
        if (null != object && object.containsKey("status")) {
            StringBuilder sb = new StringBuilder();
            List list = new ArrayList();
            sb.append("update za_gamerooms set `status`=?,game_index = game_index+1");
            list.add(1);
            if (0 == object.getInt("status")) {
                sb.append(",createtime=?");
                list.add(TimeUtil.getNowDate());
            }

            sb.append(" WHERE id= ?");
            list.add(roomId);
            DBUtil.executeUpdateBySQL(sb.toString(), list.toArray());
        }

    }

    public JSONObject getAppSettingInfo(String platform) {
        String sql = "select signin_prop,signin_base,signin_max,signin_min from operator_appsetting where platform=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{platform});
    }

    public JSONObject getUserProxyInfoById(long id) {
        String sql = "select za_user_id,player_power_id from player_info where za_user_id=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{id});
    }

    public JSONArray getRoomListByClubCode(String clubCode, int gid, int pageIndex, int pageSize) {
        String sql = "SELECT id,room_no,game_count,createtime FROM za_gamerooms where game_id=? and level=? and (user_id0>0 or user_id1>0 or user_id2>0 or user_id3>0 or user_id4>0 or user_id5>0 or user_id6>0 or user_id7>0 or user_id8>0 or user_id9>0) order by id desc LIMIT ?,?";
        JSONArray roomList = DBUtil.getObjectListBySQL(sql, new Object[]{gid, clubCode, pageIndex, pageSize});
        return !Dto.isNull(roomList) ? TimeUtil.transTimestamp(roomList, "createtime", "yyyy-MM-dd HH:mm:ss") : roomList;
    }

    public JSONArray getRecordInfoByUser(int[] roomType, String roomId, Integer gameId, String roomNo, String circleCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT `createtime` AS createTime,id,room_no,result,gamelog_id,game_index,room_type FROM `za_usergamelogs` where gid=?  and room_no = ? ");
        List list = new ArrayList();
        list.add(gameId);
        list.add(roomNo);
        if (null != gameId) {
            sb.append(" and gid=? ");
            list.add(gameId);
        }

        if (null != circleCode) {
            sb.append(" and club_code=? ");
            list.add(circleCode);
        }

        if (null != roomId) {
            sb.append("and room_id = ? ");
            list.add(roomId);
        }

        if (null != roomType && roomType.length > 0) {
            sb.append(" AND `room_type` in(");

            for(int i = 0; i < roomType.length; ++i) {
                sb.append("?");
                list.add(roomType[i]);
                if (i != roomType.length - 1) {
                    sb.append(",");
                }
            }

            sb.append(") ");
        }

        sb.append(" GROUP BY gamelog_id ORDER BY id ASC");
        return DBUtil.getObjectListBySQL(sb.toString(), list.toArray());
    }

    public JSONArray getUserGameLogList(Object rommId, Object roomNo) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT z.*,f.`account`,f.`name`,f.`headimg`,IF(g.`cut_score`IS NULL,0,g.`cut_score`)AS cutScore,IF(g.`cut_type`IS NULL,0,g.`cut_type`)AS cutType FROM(SELECT z.`gid`,z.`room_id`,z.`room_no`");
        sb.append(",z.`game_index`,z.`user_id`,z.`gamelog_id`,z.`createtime` AS createTime,z.`room_type`,z.`club_code`,SUM(z.`account`)AS score,COUNT(z.`id`)AS gameSum FROM za_usergamelogs z  WHERE 1=1 ");
        List list = new ArrayList();
        if (null != rommId) {
            sb.append(" AND z.`room_id`= ?  ");
            list.add(rommId);
        }

        if (null != roomNo) {
            sb.append(" AND z.`room_no`= ?  ");
            list.add(roomNo);
        }

        sb.append("GROUP BY z.`user_id`,z.`room_id`,z.`room_no`)z LEFT JOIN za_users f ON z.`user_id`=f.`id`LEFT JOIN za_user_game_statis g ON g.`room_no`=z.`room_no`AND g.`room_id`=z.`room_id`AND g.`user_id`=z.`user_id`");
        return DBUtil.getObjectListBySQL(sb.toString(), list.toArray());
    }

    public JSONObject getUserGameCardLog(long userId, String roomNo, int gameId, String gameCount) {
        StringBuilder sb = new StringBuilder("SELECT a.`game_index`, c.`action_records`,a.createtime FROM za_usergamelogs a LEFT JOIN za_users b ON a.`user_id` = b.`id` LEFT JOIN za_gamelogs c ON a.`gamelog_id` = c.`id` WHERE 1=1 ");
        List<Object> list = new ArrayList();
        if (roomNo != null && !"".equals(roomNo)) {
            sb.append(" AND a.room_no = ? ");
            list.add(roomNo);
        }

        if (gameCount != null && !"".equals(gameCount)) {
            sb.append(" AND a.game_index = ? ");
            list.add(gameCount);
        }

        sb.append(" ORDER BY a.`game_index` DESC LIMIT 1 ");
        return DBUtil.getObjectBySQL(sb.toString(), list.toArray());
    }

    public JSONObject getRoomcardSpend(Object rommId, Object roomNo) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT SUM(z.`pocketChange`)AS roomCard FROM za_userdeduction z WHERE 1=1 ");
        List list = new ArrayList();
        if (null != rommId) {
            sb.append(" AND z.`roomid`= ?  ");
            list.add(rommId);
        }

        if (null != roomNo) {
            sb.append(" AND z.`roomNo`= ?  ");
            list.add(roomNo);
        }

        return DBUtil.getObjectBySQL(sb.toString(), list.toArray());
    }

    public JSONObject getRoomcardAllSpend(Object clubId, Object clubType, String time, List userIds) {
        JSONObject object = new JSONObject();
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT SUM(z.`pocketChange`)AS roomCardAll FROM za_userdeduction z WHERE z.gid is not null AND(DATEDIFF(z.`creataTime`,?)=0)");
        List list = new ArrayList();
        list.add(time);
        if (null != clubId) {
            sb.append(" AND z.`club_id`= ?  ");
            list.add(clubId);
        }

        if (null != clubType) {
            sb.append(" AND z.`club_type`= ?  ");
            list.add(clubType);
        }

        double roomCardAll = 0.0D;
        JSONObject All1 = DBUtil.getObjectBySQL(sb.toString(), list.toArray());
        if (All1.containsKey("roomCardAll")) {
            roomCardAll = All1.getDouble("roomCardAll");
        }

        if ("1".equals(clubType + "")) {
            sb = new StringBuilder();
            sb.append("SELECT SUM(z.`fund_change_count`) AS roomCardAll FROM game_circle_fund_bill z WHERE 1=1 AND(DATEDIFF(z.`gmt_create`,?)=0)");
            list = new ArrayList();
            list.add(time);
            if (null != clubId) {
                sb.append(" AND z.`circle_id`= ?  ");
                list.add(clubId);
            }

            sb.append(" AND z.`operator_type`= ?  ");
            list.add("3");
            if (null != userIds && userIds.size() > 0) {
                sb.append(" AND z.`user_id` in(");

                for(int i = 0; i < userIds.size(); ++i) {
                    sb.append("?");
                    list.add(userIds.get(i));
                    if (i != userIds.size() - 1) {
                        sb.append(",");
                    }
                }

                sb.append(") ");
            }

            JSONObject All2 = DBUtil.getObjectBySQL(sb.toString(), list.toArray());
            if (All2.containsKey("roomCardAll")) {
                roomCardAll = Dto.add(roomCardAll, All2.getDouble("roomCardAll"));
            }
        }

        object.element("roomCardAll", roomCardAll);
        return object;
    }

    public JSONObject getGameAllCount(Object clubCode, int[] roomType, String time, List userIds, String platform) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT COUNT(*)AS gameAllCount FROM(SELECT id FROM za_user_game_statis z WHERE 1=1 AND(DATEDIFF(z.`create_time`,?)=0) ");
        String type = this.circleBiz.getSysValue(platform, "calculation_type");
        List list = new ArrayList();
        list.add(time);
        if (null != clubCode) {
            sb.append(" AND z.`circle_code`= ?  ");
            list.add(clubCode);
        }

        int i;
        if (null != roomType && roomType.length > 0) {
            sb.append(" AND z.`room_type` in(");

            for(i = 0; i < roomType.length; ++i) {
                sb.append("?");
                list.add(roomType[i]);
                if (i != roomType.length - 1) {
                    sb.append(",");
                }
            }

            sb.append(") ");
        }

        if (null != userIds && userIds.size() > 0) {
            sb.append(" AND z.`user_id` in(");

            for(i = 0; i < userIds.size(); ++i) {
                sb.append("?");
                list.add(userIds.get(i));
                if (i != userIds.size() - 1) {
                    sb.append(",");
                }
            }

            sb.append(") ");
        }

        String temp = " GROUP BY z.`sole`";
        if ("2".equals(type)) {
            temp = "";
        }

        sb.append(temp);
        sb.append(" )f");
        return DBUtil.getObjectBySQL(sb.toString(), list.toArray());
    }

    public JSONObject getRecordInfo(long userId, String time) {
        String sb = " SELECT b.`headimg`, b.`roomcard`, b.`name`, b.`account`  , IF(f.`totalNumber` IS NULL, 0, f.`totalNumber`) AS totalNumber  , IF(f.`totalScore` IS NULL, 0, f.`totalScore`) AS totalScore  , IF(f.`bigWinnerCount` IS NULL, 0, f.`bigWinnerCount`) AS bigWinnerCount  , IF(f.`bigWinnerScore` IS NULL, 0, f.`bigWinnerScore`) AS bigWinnerScore  FROM za_users b LEFT JOIN ( SELECT COUNT(*) AS totalNumber, l.`user_id`, SUM(l.`score`) AS totalScore  , SUM(IF(l.`is_big_winner` = 0, 0, 1)) AS bigWinnerCount  , SUM(IF(l.`is_big_winner` = 0, 0, l.`score`)) AS bigWinnerScore  FROM za_user_game_statis l  WHERE l.`circle_code` IS NULL  AND l.`user_id` = ?  AND (DATEDIFF(l.`create_time`,?)=0)  GROUP BY l.`user_id` ) f  ON b.`id` = f.`user_id` WHERE b.`id` = ? ";
        return DBUtil.getObjectBySQL(sb, new Object[]{userId, time, userId});
    }

	@Override
	public int updateRoomcardByAccount(String account, int zuanshi) {
		String sql = "update za_users set roomcard = roomcard-? where account =?";
		return DBUtil.executeUpdateBySQL(sql, new Object[]{zuanshi,account});
	}

	@Override
	public JSONObject getGameBgSetByUserId(long userId) {
		String sql = "SELECT * FROM za_bg WHERE user_id =?";
		return DBUtil.getObjectBySQL(sql, new Object[] {userId});
	}

	@Override
	public int insertUserGameSet(JSONObject zaBg) {
		return DBJsonUtil.add(zaBg, "za_bg");
	}

	@Override
	public int updateUserGameSetByGameid(int userId, int gameId,int bgid) {
		String sql1 = "";
		if (gameId==3) {
			sql1 = "mjbg=?";
		}else if (gameId == 4) {
			sql1 = "sssbg=?";
		}else if (gameId == 21) {
			sql1="pdkbg=?";
		}
		String sql = "update za_bg set "+sql1+" where user_id =?";
		return DBUtil.executeUpdateBySQL(sql, new Object[]{bgid,userId});
	}

	@Override
	public int updateUserNameByUserId(long userId, String name) {
		String sql = "update za_users set name=? where id=?";
		return DBUtil.executeUpdateBySQL(sql, new Object[]{name,userId});
		
	}

	@Override
	public int updateUserHeadimgByUserId(long userId, String headImg) {
		String sql = "update za_users set headimg=? where id =?";
		return DBUtil.executeUpdateBySQL(sql, new Object[]{headImg,userId});
	}

	@Override
	public JSONObject getHeadimgByHeadId(long id) {
		String sql = "select address from za_sys_headimg where id=?";
		return DBUtil.getObjectBySQL(sql, new Object[] {id});
	}
}
