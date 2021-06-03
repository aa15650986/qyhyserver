

package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.GameCircleService;
import com.zhuoan.biz.game.dao.util.BaseSqlUtil;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.util.Dto;
import com.zhuoan.util.RegexUtil;
import com.zhuoan.util.TimeUtil;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import jodd.util.ArraysUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GameCircleServiceImpl implements GameCircleService {
    private static final Logger log = LoggerFactory.getLogger(GameCircleServiceImpl.class);
    @Resource
    private RedisInfoService redisInfoService;

    public GameCircleServiceImpl() {
    }

    public JSONObject queryGameCircleInfoById(String circleId) {
        return this.redisInfoService.getGameCircleInfoByid(circleId);
    }

    public JSONObject getCircleMemberInfoById(String circleId, String userId) {
        return circleId != null && Dto.isNumeric(circleId) && userId != null && Dto.isNumeric(userId) ? BaseSqlUtil.getObjectByConditions("*", "game_circle_member", new String[]{"id", "user_id"}, new Object[]{circleId, userId}) : null;
    }

    public boolean deductRoomCard(String gameId, String roomNo, String circleId, String paytype, double eachPerson, List<String> userIds, String createRoom) {
        if (circleId != null && Dto.isNumeric(circleId) && createRoom != null && Dto.isNumeric(createRoom)) {
            if (userIds != null && userIds.size() != 0) {
                JSONObject circle = BaseSqlUtil.getObjectByOneConditions("fee_type,fund_count,platform", "game_circle_info", "id", circleId);
                if (circle != null && circle.get("fee_type") != null && circle.get("fund_count") != null) {
                    JSONObject gamerooms = DBUtil.getObjectBySQL("SELECT id FROM za_gamerooms WHERE room_no=? ORDER BY id DESC", new Object[]{roomNo});
                    Object roomId = null;
                    if (null != gamerooms && gamerooms.containsKey("id")) {
                        roomId = gamerooms.get("id");
                    }

                    if (!"1".equals(paytype)) {
                        double roomcardCount = eachPerson * (double)userIds.size();
                        if ("0".equals(paytype)) {
                            JSONObject user = BaseSqlUtil.getObjectByOneConditions("roomcard,id", "za_users", "id", createRoom);
                            if (user != null && user.get("roomcard") != null) {
                                double roomcard = user.getDouble("roomcard");
                                if (roomcard < roomcardCount) {
                                    return false;
                                } else {
                                    double roomcardN = roomcard - roomcardCount;
                                    int b = DBUtil.executeUpdateBySQL("update za_users set roomcard = roomcard - ? where id=?", new Object[]{roomcardCount, user.get("id")});
                                    if (b > 0) {
                                        BaseSqlUtil.insertData("za_userdeduction", new String[]{"club_id", "club_type", "userid", "roomid", "roomNo", "gid", "`type`", "`sum`", "creataTime", "pocketNew", "pocketOld", "pocketChange", "operatorType", "platform", "memo"}, new Object[]{circleId, "1", createRoom, roomId, roomNo, gameId, "0", -roomcardCount, TimeUtil.getNowDate(), roomcardN, roomcard, -roomcardCount, "40", circle.getString("platform"), "亲友圈开房消耗房卡"});
                                    }

                                    return b > 0;
                                }
                            } else {
                                return false;
                            }
                        } else if ("2".equals(paytype)) {
                            double fundCount = circle.getDouble("fund_count");
                            if (fundCount < roomcardCount) {
                                return false;
                            } else {
                                int b = DBUtil.executeUpdateBySQL("update game_circle_info set fund_count = fund_count - ? where id=?", new Object[]{roomcardCount, circleId});
                                this.redisInfoService.delGameCircleInfoByid(circleId);
                                if (b > 0) {
                                    BaseSqlUtil.insertData("game_circle_fund_bill", new String[]{"game_id", "circle_id", "user_id", "room_no", "operator_type", "fund_change_count", "fund_old", "platform", "create_user", "memo"}, new Object[]{gameId, circleId, createRoom, roomNo, "3", -roomcardCount, fundCount, circle.getString("platform"), createRoom, "房间号 " + roomNo});
                                }

                                return b > 0;
                            }
                        } else {
                            return false;
                        }
                    } else {
                        List params = new ArrayList();
                        StringBuilder sb = new StringBuilder();
                        sb.append("select id,`roomcard`,`platform` from za_users where id in(");

                        for(int i = 0; i < userIds.size(); ++i) {
                            params.add(userIds.get(i));
                            sb.append("?");
                            if (i < userIds.size() - 1) {
                                sb.append(",");
                            } else {
                                sb.append(")");
                            }
                        }

                        JSONArray userList = DBUtil.getObjectListBySQL(sb.toString(), params.toArray());
                        if (userList != null && userList.size() == userIds.size()) {
                            StringBuilder sbU = new StringBuilder();
                            sbU.append("UPDATE za_users SET `roomcard`= `roomcard` - CASE id");
                            List param = new ArrayList();
                            StringBuilder sbU2 = new StringBuilder();
                            List<Object[]> insertList = new ArrayList();

                            for(int i = 0; i < userList.size(); ++i) {
                                JSONObject user = userList.getJSONObject(i);
                                if (user == null) {
                                    return false;
                                }

                                if (user.get("roomcard") == null || (new BigDecimal(String.valueOf(user.get("roomcard")))).compareTo(new BigDecimal(String.valueOf(eachPerson))) == -1) {
                                    return false;
                                }

                                sbU.append(" WHEN ").append(user.get("id")).append(" THEN ").append(eachPerson);
                                param.add(user.get("id"));
                                sbU2.append("?");
                                if (i < userList.size() - 1) {
                                    sbU2.append(",");
                                } else {
                                    sbU2.append(")");
                                }

                                BigDecimal roomcardN = (new BigDecimal(String.valueOf(user.get("roomcard")))).subtract(new BigDecimal(eachPerson));
                                insertList.add(new Object[]{circleId, "1", user.get("id"), roomId, roomNo, gameId, "0", -eachPerson, TimeUtil.getNowDate(), roomcardN, user.get("roomcard"), -eachPerson, "40", user.getString("platform"), "亲友圈开房消耗房卡"});
                            }

                            sbU.append(" END WHERE id IN (").append(sbU2);
                            DBUtil.executeUpdateBySQL(sbU.toString(), param.toArray());
                            BaseSqlUtil.insertData("za_userdeduction", new String[]{"club_id", "club_type", "userid", "roomid", "roomNo", "gid", "`type`", "`sum`", "creataTime", "pocketNew", "pocketOld", "pocketChange", "operatorType", "platform", "memo"}, insertList);
                            return true;
                        } else {
                            return false;
                        }
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void userPumping(String circleId, String roomNo, String gameId, JSONObject pumpInfo, String changeType) {
        if (circleId != null && Dto.isNumeric(circleId)) {
            List userIdList = new ArrayList();
            Iterator var7 = pumpInfo.keySet().iterator();

            while(var7.hasNext()) {
                Object key = var7.next();
                userIdList.add(key);
            }

            if (userIdList != null && userIdList.size() != 0) {
                int userCount = userIdList.size();
                List params = new ArrayList();
                StringBuilder sb = new StringBuilder();
                sb.append("select id,user_hp,platform,user_id from game_circle_member where user_id in(");

                for(int i = 0; i < userCount; ++i) {
                    params.add(userIdList.get(i));
                    sb.append("?");
                    if (i < userCount - 1) {
                        sb.append(",");
                    } else {
                        sb.append(")");
                    }
                }

                sb.append(" and circle_id = ? and is_delete= ?");
                params.add(circleId);
                params.add("N");
                JSONArray memberList = DBUtil.getObjectListBySQL(sb.toString(), params.toArray());
                if (memberList != null && memberList.size() != 0) {
                    List<Object[]> insertList = new ArrayList();
                    StringBuilder sbU = new StringBuilder();
                    sbU.append("UPDATE game_circle_member SET `user_hp` =  CASE id");
                    StringBuilder sbU2 = new StringBuilder();

                    for(int i = 0; i < memberList.size(); ++i) {
                        JSONObject member = memberList.getJSONObject(i);
                        sbU.append(" WHEN ").append(member.get("id")).append(" THEN `user_hp`+").append(pumpInfo.getDouble(member.getString("user_id")));
                        sbU2.append(member.get("id"));
                        if (i < userCount - 1) {
                            sbU2.append(",");
                        } else {
                            sbU2.append(")");
                        }

                        String memo = "2".equals(changeType) ? "游戏记录(" + roomNo + ")" : ("3".equals(changeType) ? "消耗扣除(" + roomNo + ")" : "");
                        insertList.add(new Object[]{member.getString("user_id"), circleId, gameId, roomNo, pumpInfo.get(member.getString("user_id")), member.get("user_hp"), changeType, member.getString("platform"), memo});
                    }

                    sbU.append(" END WHERE id IN (").append(sbU2);
                    DBUtil.executeUpdateBySQL(sbU.toString(), (Object[])null);
                    BaseSqlUtil.insertData("game_circle_prop_bill", new String[]{"user_id", "circle_id", "game_id", "room_no", "change_count", "old_count", "change_type", "platform", "memo"}, insertList);
                }
            }
        }
    }

    public void circleCentFee(String circleId, String roomNo, String gameId, JSONObject pumpInfo, String cutType) {
        if (circleId != null && Dto.isNumeric(circleId)) {
            List userIdList = new ArrayList();
            Iterator var7 = pumpInfo.keySet().iterator();

            while(var7.hasNext()) {
                Object key = var7.next();
                userIdList.add(key);
            }

            if (userIdList != null && userIdList.size() != 0) {
                List params = new ArrayList();
                StringBuilder sb = new StringBuilder();
                sb.append("select id,user_id,seq_member_ids,circle_id,user_role,profit_ratio from game_circle_member where user_id in(");

                for(int i = 0; i < userIdList.size(); ++i) {
                    params.add(userIdList.get(i));
                    sb.append("?");
                    if (i < userIdList.size() - 1) {
                        sb.append(",");
                    } else {
                        sb.append(")");
                    }
                }

                sb.append(" and circle_id = ? and is_delete = ?");
                params.add(circleId);
                params.add("N");
                JSONArray memberList = DBUtil.getObjectListBySQL(sb.toString(), params.toArray());
                if (memberList != null && memberList.size() != 0) {
                    List<Object[]> insertList = new ArrayList();
                    Map<Long, Double> updateMember = new HashMap();

                    for(int i = 0; i < memberList.size(); ++i) {
                        JSONObject member = memberList.getJSONObject(i);
                        JSONArray memberListSeq = new JSONArray();
                        String memberUserId = member.containsKey("id") ? member.getString("id") : null;
                        String memberRole = member.containsKey("user_role") ? member.getString("user_role") : "3";
                        int memberProfitRatio = member.containsKey("profit_ratio") ? member.getInt("profit_ratio") : 0;
                        if (member.containsKey("seq_member_ids") && !"".equals(member.getString("seq_member_ids"))) {
                            String seqMemberIds = member.getString("seq_member_ids");
                            String[] memberIds = seqMemberIds.split("\\$");
                            StringBuilder seq = new StringBuilder();
                            seq.append("select id,profit_ratio,user_id,profit_balance,platform from game_circle_member where id in(");
                            params = new ArrayList();
                            if ("2".equals(memberRole) && memberProfitRatio > 0 && memberUserId != null) {
                                memberIds = ArraysUtil.insert(memberIds, memberUserId, memberIds.length);
                            }

                            for(int j = 0; j < memberIds.length; ++j) {
                                if (RegexUtil.isPlusInt(memberIds[j])) {
                                    params.add(memberIds[j]);
                                    seq.append("?");
                                    if (j < memberIds.length - 1) {
                                        seq.append(",");
                                    } else {
                                        seq.append(")");
                                    }
                                }
                            }

                            memberListSeq = DBUtil.getObjectListBySQL(seq.toString(), params.toArray());
                        }

                        if (memberListSeq == null || memberListSeq.size() == 0) {
                            params = new ArrayList();
                            params.add(member.get("circle_id"));
                            params.add("1");
                            params.add("N");
                            memberListSeq = DBUtil.getObjectListBySQL("select id,profit_ratio,user_id,profit_balance,platform from game_circle_member WHERE circle_id = ? and user_role = ? and is_delete= ? ", params.toArray());
                        }

                        double fee = pumpInfo.getDouble(member.getString("user_id"));
                        fee = fee < 0.0D ? fee * -1.0D : fee;
                        int profitRatio = 0;

                        for(int j = memberListSeq.size() - 1; j > -1; --j) {
                            JSONObject memberSeq = memberListSeq.getJSONObject(j);
                            if (profitRatio < memberSeq.getInt("profit_ratio")) {
                                profitRatio = memberSeq.getInt("profit_ratio");
                            }

                            if (profitRatio > 0) {
                                double balance;
                                if (memberListSeq.size() == 1) {
                                    balance = fee;
                                } else if (j == memberListSeq.size() - 1) {
                                    balance = fee * (double)profitRatio / 100.0D;
                                } else {
                                    if (memberListSeq.getJSONObject(j + 1).getInt("profit_ratio") >= profitRatio) {
                                        continue;
                                    }

                                    int feeRatio = profitRatio - memberListSeq.getJSONObject(j + 1).getInt("profit_ratio");
                                    balance = fee * (double)feeRatio / 100.0D;
                                }

                                if (balance > 0.0D) {
                                    long memberId = memberSeq.getLong("id");
                                    if (updateMember.get(memberId) != null) {
                                        updateMember.put(memberSeq.getLong("id"), (Double)updateMember.get(memberId) + balance);
                                    } else {
                                        updateMember.put(memberSeq.getLong("id"), balance);
                                    }

                                    insertList.add(new Object[]{memberSeq.get("user_id"), member.get("user_id"), circleId, gameId, roomNo, balance, memberSeq.get("profit_balance"), "5", memberSeq.getString("platform"), "分润"});
                                }
                            }
                        }
                    }

                    BaseSqlUtil.insertData("game_circle_prop_bill", new String[]{"user_id", "fenrun_id", "circle_id", "game_id", "room_no", "change_count", "old_count", "change_type", "platform", "memo"}, insertList);
                    if (insertList != null && insertList.size() > 0) {
                        StringBuilder sbU = new StringBuilder();
                        sbU.append("UPDATE game_circle_member SET ");
                        StringBuilder sbU2 = new StringBuilder();
                        sbU2.append(" profit_balance = CASE id ");
                        StringBuilder sbU3 = new StringBuilder();
                        sbU3.append(" profit_balance_all = CASE id ");
                        StringBuilder sbU4 = new StringBuilder();
                        Iterator var38 = updateMember.keySet().iterator();

                        while(var38.hasNext()) {
                            Long id = (Long)var38.next();
                            sbU2.append(" WHEN ").append(id).append(" THEN profit_balance +").append(updateMember.get(id));
                            sbU3.append(" WHEN ").append(id).append(" THEN profit_balance_all +").append(updateMember.get(id));
                            sbU4.append(id).append(",");
                        }

                        sbU4.delete(sbU4.length() - 1, sbU4.length());
                        sbU.append(sbU2).append(" END ,").append(sbU3).append(" END ").append("  WHERE id IN (").append(sbU4).append(")");
                        DBUtil.executeUpdateBySQL(sbU.toString(), (Object[])null);
                    }

                }
            }
        }
    }

    public void circleUserPumping(JSONObject object) {
        try {
            log.info("亲友圈 扣除玩家相应数据并添加记录");
            this.userPumping(object.getString("circleId"), object.getString("roomNo"), object.getString("gameId"), object.getJSONObject("pumpInfo"), object.getString("changeType"));
            if ("3".equals(object.getString("changeType"))) {
                this.circleCentFee(object.getString("circleId"), object.getString("roomNo"), object.getString("gameId"), object.containsKey("pumpInfoAll") ? object.getJSONObject("pumpInfoAll") : object.getJSONObject("pumpInfo"), object.containsKey("cutType") ? object.getString("cutType") : null);
            }
        } catch (Exception var3) {
            log.info("亲友圈 扣除玩家相应数据并添加记录[{}]", var3);
        }

    }
}
