

package com.zhuoan.biz.event.club;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.event.BaseEventDeal;
import com.zhuoan.biz.event.ddz.DdzGameEventDeal;
import com.zhuoan.biz.event.gppj.GPPJGameEventDeal;
import com.zhuoan.biz.event.nn.NNGameEventDealNew;
import com.zhuoan.biz.event.qzmj.QZMJGameEventDeal;
import com.zhuoan.biz.event.sss.SSSGameEventDealNew;
import com.zhuoan.biz.event.zjh.ZJHGameEventDealNew;
import com.zhuoan.biz.game.biz.ClubBiz;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.util.Dto;
import com.zhuoan.util.MathDelUtil;
import com.zhuoan.util.TimeUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Resource;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class ClubEventDeal {
    @Resource
    private ClubBiz clubBiz;
    @Resource
    private UserBiz userBiz;
    @Resource
    private RedisService redisService;
    @Resource
    private RoomBiz roomBiz;
    @Resource
    private BaseEventDeal baseEventDeal;
    private String GLOBAL_CLUB_NAME = "俱乐部";
    @Resource
    private NNGameEventDealNew nnGameEventDealNew;
    @Resource
    private DdzGameEventDeal ddzGameEventDeal;
    @Resource
    private QZMJGameEventDeal qzmjGameEventDeal;
    @Resource
    private SSSGameEventDealNew sssGameEventDealNew;
    @Resource
    private GPPJGameEventDeal gppjGameEventDeal;
    @Resource
    private ZJHGameEventDealNew zjhGameEventDealNew;

    public ClubEventDeal() {
    }

    public void getMyClubList(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (postData.containsKey("account")) {
            JSONObject result = new JSONObject();
            String account = postData.getString("account");
            JSONObject userClub = this.clubBiz.getUserClubByAccount(account);
            if (!Dto.isObjNull(userClub) && userClub.containsKey("clubIds") && !Dto.stringIsNULL(userClub.getString("clubIds"))) {
                result.put("code", 1);
                List<JSONObject> clubList = this.getUserClubListInfo(userClub);
                result.put("clubList", clubList);
            } else {
                result.put("code", 0);
                result.put("msg", "未加入" + this.GLOBAL_CLUB_NAME);
            }

            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getMyClubListPush");
        }
    }

    private List<JSONObject> getUserClubListInfo(JSONObject userClub) {
        List<JSONObject> clubList = new ArrayList();
        String[] clubIds = userClub.getString("clubIds").substring(1, userClub.getString("clubIds").length() - 1).split("\\$");

        for(int i = 0; i < clubIds.length; ++i) {
            JSONObject clubInfo = this.clubBiz.getClubById(Long.valueOf(clubIds[i]));
            if (!Dto.isObjNull(clubInfo)) {
                JSONObject leaderInfo = this.userBiz.getUserByID(clubInfo.getLong("leaderId"));
                JSONObject obj = new JSONObject();
                obj.put("clubId", clubInfo.getLong("id"));
                obj.put("clubCode", clubInfo.getString("clubCode"));
                obj.put("clubName", clubInfo.getString("clubName"));
                obj.put("payType", clubInfo.getInt("payType"));
                obj.put("imgUrl", CommonConstant.getServerDomain() + leaderInfo.getString("headimg"));
                obj.put("isTop", userClub.containsKey("top_club") && Long.valueOf(clubIds[i]) == userClub.getLong("top_club") ? 1 : 0);
                clubList.add(obj);
            }
        }

        Collections.sort(clubList, new Comparator<JSONObject>() {
            public int compare(JSONObject o1, JSONObject o2) {
                return o2.getInt("isTop") - o1.getInt("isTop");
            }
        });
        return clubList;
    }

    public void getClubMembers(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (postData.containsKey("clubCode")) {
            JSONObject result = new JSONObject();
            String clubCode = postData.getString("clubCode");
            JSONObject clubInfo = this.clubBiz.getClubByCode(clubCode);
            List<JSONObject> members = new ArrayList();
            if (!Dto.isObjNull(clubInfo)) {
                JSONArray memberArray = this.clubBiz.getClubMember(clubInfo.getLong("id"));

                for(int i = 0; i < memberArray.size(); ++i) {
                    JSONObject memberObj = memberArray.getJSONObject(i);
                    JSONObject member = new JSONObject();
                    member.put("account", memberObj.getString("account"));
                    member.put("name", memberObj.getString("name"));
                    member.put("img", CommonConstant.getServerDomain() + memberObj.getString("headimg"));
                    member.put("isLeader", clubInfo.getLong("leaderId") == memberObj.getLong("id") ? 1 : 0);
                    members.add(member);
                }
            }

            result.put("members", members);
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getClubMembersPush");
        }
    }

    public void getClubSetting(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (postData.containsKey("clubCode") && postData.containsKey("account")) {
            JSONObject result = new JSONObject();
            String clubCode = postData.getString("clubCode");
            String account = postData.getString("account");
            JSONObject clubInfo = this.clubBiz.getClubByCode(clubCode);
            if (!Dto.isObjNull(clubInfo)) {
                JSONObject leaderInfo = this.userBiz.getUserByID(clubInfo.getLong("leaderId"));
                if (!Dto.isObjNull(leaderInfo)) {
                    result.put("leader_img", CommonConstant.getServerDomain() + leaderInfo.getString("headimg"));
                    result.put("leader_name", leaderInfo.getString("name"));
                    result.put("clubName", clubInfo.getString("clubName"));
                    result.put("clubCode", clubInfo.getString("clubCode"));
                    result.put("balance", clubInfo.getDouble("balance"));
                    JSONArray memberArray = this.clubBiz.getClubMember(clubInfo.getLong("id"));
                    result.put("memberCount", memberArray == null ? 0 : memberArray.size());
                    result.put("notice", clubInfo.containsKey("notice") ? clubInfo.getString("notice") : "");
                    String setting = "";
                    if (clubInfo.containsKey("setting")) {
                        JSONObject clubSetting = JSONObject.fromObject(clubInfo.getString("setting"));
                        Iterator var12 = clubSetting.keySet().iterator();

                        while(var12.hasNext()) {
                            Object obj = var12.next();
                            String gameName = this.getGameNameById(Integer.parseInt(String.valueOf(obj)));
                            if (!Dto.stringIsNULL(gameName)) {
                                setting = setting + gameName;
                                setting = setting + " ： ";
                                setting = setting + clubSetting.get(obj);
                                setting = setting + "\n";
                            }
                        }
                    }

                    result.put("setting", setting);
                    result.put("isLeader", leaderInfo.getString("account").equals(account) ? 1 : 0);
                    CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getClubSettingPush");
                }
            }

        }
    }

    public void changeClubSetting(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String clubCode = postData.containsKey("clubCode") ? postData.getString("clubCode") : "";
        String leader = postData.getString("leader");
        String uuid = postData.getString("uuid");
        String notice = postData.containsKey("notice") ? postData.getString("notice") : "";
        String setting = postData.containsKey("setting") ? postData.getString("setting") : "";
        String quickSetting = postData.containsKey("quick_setting") ? postData.getString("quick_setting") : "";
        int gameId = postData.containsKey("gid") ? postData.getInt("gid") : 0;
        String eventName = "changeClubSettingPush";
        JSONObject leaderInfo = this.clubBiz.getUserByAccountAndUuid(leader, uuid);
        if (Dto.isObjNull(leaderInfo)) {
            this.sendPromptToSingle(client, 0, "账号已在其他地方登录", eventName);
        } else {
            JSONObject clubInfo = this.clubBiz.getClubByCode(clubCode);
            if (!Dto.isObjNull(clubInfo) && clubInfo.getLong("leaderId") == leaderInfo.getLong("id")) {
                JSONObject newClubInfo = new JSONObject();
                newClubInfo.put("id", clubInfo.getLong("id"));
                if (!Dto.stringIsNULL(notice)) {
                    newClubInfo.put("notice", notice);
                }

                if (!Dto.stringIsNULL(setting) && !Dto.stringIsNULL(quickSetting)) {
                    JSONObject settingObj = clubInfo.containsKey("setting") ? clubInfo.getJSONObject("setting") : new JSONObject();
                    JSONObject quickSettingObj = clubInfo.containsKey("quick_setting") ? clubInfo.getJSONObject("quick_setting") : new JSONObject();
                    newClubInfo.put("setting", settingObj.element(String.valueOf(gameId), setting));
                    newClubInfo.put("quick_setting", quickSettingObj.element(String.valueOf(gameId), quickSetting));
                }

                this.clubBiz.updateClubInfo(newClubInfo);
                this.sendPromptToSingle(client, 1, "修改成功", eventName);
            } else {
                this.sendPromptToSingle(client, 0, "无修改权限", eventName);
            }
        }
    }

    public void exitClub(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String clubCode = postData.getString("clubCode");
        String account = postData.getString("account");
        String uuid = postData.getString("uuid");
        String eventName = "exitClubPush";
        JSONObject userInfo = this.clubBiz.getUserByAccountAndUuid(account, uuid);
        if (Dto.isObjNull(userInfo)) {
            this.sendPromptToSingle(client, 0, "账号已在其他地方登录", eventName);
        } else if (userInfo.containsKey("clubIds") && !Dto.stringIsNULL(userInfo.getString("clubIds"))) {
            JSONObject clubInfo = this.clubBiz.getClubByCode(clubCode);
            if (Dto.isObjNull(clubInfo)) {
                this.sendPromptToSingle(client, 0, this.GLOBAL_CLUB_NAME + "不存在", eventName);
            } else {
                List<String> clubIdList = this.getUserClubList(userInfo);
                String clubId = clubInfo.getString("id");
                if (!clubIdList.contains(clubId)) {
                    this.sendPromptToSingle(client, 0, "未加入该" + this.GLOBAL_CLUB_NAME, eventName);
                } else {
                    clubIdList.remove(clubId);
                    StringBuffer newClubIds = new StringBuffer();
                    if (clubIdList.size() > 0) {
                        newClubIds.append("$");

                        for(int i = 0; i < clubIdList.size(); ++i) {
                            newClubIds.append((String)clubIdList.get(i));
                            newClubIds.append("$");
                        }
                    }

                    this.clubBiz.updateUserClubIds(userInfo.getLong("id"), String.valueOf(newClubIds));
                    this.sendPromptToSingle(client, 1, "退出成功", eventName);
                }
            }
        } else {
            this.sendPromptToSingle(client, 0, "未加入" + this.GLOBAL_CLUB_NAME, eventName);
        }
    }

    public void toTop(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String clubCode = postData.getString("clubCode");
        String account = postData.getString("account");
        String uuid = postData.getString("uuid");
        int top = postData.getInt("top");
        String eventName = "toTopPush";
        JSONObject userInfo = this.clubBiz.getUserByAccountAndUuid(account, uuid);
        if (Dto.isObjNull(userInfo)) {
            this.sendPromptToSingle(client, 0, "账号已在其他地方登录", eventName);
        } else if (userInfo.containsKey("clubIds") && !Dto.stringIsNULL(userInfo.getString("clubIds"))) {
            JSONObject clubInfo = this.clubBiz.getClubByCode(clubCode);
            if (Dto.isObjNull(clubInfo)) {
                this.sendPromptToSingle(client, 0, this.GLOBAL_CLUB_NAME + "不存在", eventName);
            } else {
                long topClubId = top == 1 ? clubInfo.getLong("id") : 0L;
                this.clubBiz.updateUserTopClub(account, topClubId);
                JSONObject userClub = this.clubBiz.getUserClubByAccount(account);
                JSONObject result = new JSONObject();
                result.put("code", 1);
                result.put("msg", "修改成功");
                if (!Dto.isObjNull(userClub) && userClub.containsKey("clubIds") && !Dto.stringIsNULL(userClub.getString("clubIds"))) {
                    result.put("code", 1);
                    List<JSONObject> clubList = this.getUserClubListInfo(userClub);
                    result.put("clubList", clubList);
                }

                CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), eventName);
            }
        } else {
            this.sendPromptToSingle(client, 0, "未加入" + this.GLOBAL_CLUB_NAME, eventName);
        }
    }

    public void refreshClubInfo(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String clubCode = postData.getString("clubCode");
        long gid = postData.get("gid") != null ? postData.getLong("gid") : 0L;
        JSONObject result = new JSONObject();
        int onlineNum = 1;
        Iterator var9 = RoomManage.gameRoomMap.keySet().iterator();

        while(var9.hasNext()) {
            String roomNo = (String)var9.next();
            if (clubCode.equals(((GameRoom)RoomManage.gameRoomMap.get(roomNo)).getClubCode())) {
                onlineNum += ((GameRoom)RoomManage.gameRoomMap.get(roomNo)).getPlayerMap().size();
            }
        }

        result.put("onlineNum", onlineNum);
        result.put("gid", gid);
        result.put("clubCode", clubCode);
        List<JSONObject> roomList = new ArrayList();
        Iterator var18 = RoomManage.gameRoomMap.keySet().iterator();

        while(true) {
            String roomNo;
            GameRoom room;
            do {
                do {
                    do {
                        do {
                            if (!var18.hasNext()) {
                                result.put("roomList", roomList);
                                CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "refreshClubInfoPush");
                                return;
                            }

                            roomNo = (String)var18.next();
                            room = (GameRoom)RoomManage.gameRoomMap.get(roomNo);
                        } while(Dto.stringIsNULL(clubCode));
                    } while(!clubCode.equals(room.getClubCode()));
                } while(!room.isOpen());
            } while(gid != 0L && (long)room.getGid() != gid);

            JSONObject roomObj = new JSONObject();
            roomObj.put("room_no", roomNo);
            roomObj.put("game", room.getGid());
            roomObj.put("di", room.getScore());
            roomObj.put("detail", room.getWfType());
            roomObj.put("isFull", 0);
            if (room.getPlayerMap().size() >= room.getPlayerCount()) {
                roomObj.put("isFull", 1);
            }

            roomObj.put("curCount", room.getPlayerMap().size());
            roomObj.put("totalCount", room.getPlayerCount());
            roomObj.put("curIndex", room.getGameIndex());
            roomObj.put("totalIndex", room.getGameCount());
            List<JSONObject> users = new ArrayList();
            Iterator var15 = room.getPlayerMap().keySet().iterator();

            while(var15.hasNext()) {
                String player = (String)var15.next();
                users.add((new JSONObject()).element("headimg", ((Playerinfo)room.getPlayerMap().get(player)).getRealHeadimg()));
            }

            roomObj.put("users", users);
            roomList.add(roomObj);
        }
    }

    public void quickJoinClubRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String eventName = "quickJoinClubRoomPush";
        String clubCode = postData.getString("clubCode");
        String account = postData.getString("account");
        int gameId = postData.getInt("gid");
        JSONObject clubInfo = this.clubBiz.getClubByCode(clubCode);
        if (Dto.isObjNull(clubInfo)) {
            this.sendPromptToSingle(client, 0, this.GLOBAL_CLUB_NAME + "不存在", eventName);
        } else {
            JSONObject userInfo = this.clubBiz.getUserByAccountAndUuid(account, postData.getString("uuid"));
            if (!Dto.isObjNull(userInfo) && this.getUserClubList(userInfo).contains(clubInfo.getString("id"))) {
                if (postData.containsKey("base_info")) {
                    int cost = this.getRoomCostByBaseInfo(postData.getJSONObject("base_info"), clubInfo.getString("payType"));
                    if ("1".equals(clubInfo.getString("payType"))) {
                        double userBalance = clubInfo.getInt("balance_type") == 1 ? (double)userInfo.getInt("roomcard") : userInfo.getDouble("yuanbao");
                        if (cost == -1 || (double)cost > userBalance) {
                            this.sendPromptToSingle(client, 0, "余额不足，无法创建房间", eventName);
                            return;
                        }
                    } else if (cost == -1 || (double)(cost + this.getClubCost(clubCode)) > clubInfo.getDouble("balance")) {
                        this.sendPromptToSingle(client, 0, "余额不足，请联系会长充值", eventName);
                        return;
                    }

                    this.baseEventDeal.createRoomBase(client, data);
                } else {
                    List<String> roomNoList = new ArrayList();
                    Iterator var11 = RoomManage.gameRoomMap.keySet().iterator();

                    while(true) {
                        String roomNo;
                        GameRoom room;
                        do {
                            do {
                                do {
                                    do {
                                        do {
                                            do {
                                                do {
                                                    if (!var11.hasNext()) {
                                                        if (roomNoList.size() != 0) {
                                                            if (postData.containsKey("room_no") && !Dto.stringIsNULL(postData.getString("room_no"))) {
                                                                boolean canJoin = this.exitRoom(postData.getString("room_no"), account);
                                                                if (!canJoin) {
                                                                    this.sendPromptToSingle(client, 0, "已加入其他房间", eventName);
                                                                    return;
                                                                }
                                                            }

                                                            Collections.shuffle(roomNoList);
                                                            postData.put("room_no", roomNoList.get(0));
                                                            postData.put("clubId", clubInfo.getLong("id"));
                                                            this.baseEventDeal.joinRoomBase(client, postData);
                                                        } else {
                                                            JSONObject quickSetting = clubInfo.containsKey("quick_setting") && !Dto.isObjNull(clubInfo.getJSONObject("quick_setting")) ? clubInfo.getJSONObject("quick_setting").getJSONObject(String.valueOf(gameId)) : null;
                                                            if (Dto.isObjNull(quickSetting)) {
                                                                this.sendPromptToSingle(client, 0, "参数不正确,请联系会长修改", eventName);
                                                            } else {
                                                                int cost = this.getRoomCostByBaseInfo(quickSetting, clubInfo.getString("payType"));
                                                                if ("1".equals(clubInfo.getString("payType"))) {
                                                                    double userBalance = clubInfo.getInt("balance_type") == 1 ? (double)userInfo.getInt("roomcard") : userInfo.getDouble("yuanbao");
                                                                    if (cost == -1 || (double)cost > userBalance) {
                                                                        this.sendPromptToSingle(client, 0, "余额不足，请前往充值", eventName);
                                                                        return;
                                                                    }
                                                                } else if (cost == -1 || (double)(cost + this.getClubCost(clubCode)) > clubInfo.getDouble("balance")) {
                                                                    this.sendPromptToSingle(client, 0, "余额不足，请联系会长充值", eventName);
                                                                    return;
                                                                }

                                                                if (postData.containsKey("room_no") && !Dto.stringIsNULL(postData.getString("room_no"))) {
                                                                    boolean canJoin = this.exitRoom(postData.getString("room_no"), account);
                                                                    if (!canJoin) {
                                                                        this.sendPromptToSingle(client, 0, "已加入其他房间", eventName);
                                                                        return;
                                                                    }
                                                                }

                                                                postData.put("base_info", quickSetting);
                                                                this.baseEventDeal.createRoomBase(client, postData);
                                                            }
                                                        }

                                                        return;
                                                    }

                                                    roomNo = (String)var11.next();
                                                    room = (GameRoom)RoomManage.gameRoomMap.get(roomNo);
                                                } while(Dto.stringIsNULL(clubCode));
                                            } while(!clubCode.equals(room.getClubCode()));
                                        } while(room.getGid() != gameId);
                                    } while(room.getPlayerMap().containsKey(account));
                                } while(room.getPlayerMap().size() >= room.getPlayerCount());
                            } while(!room.isOpen());
                        } while(postData.containsKey("room_no") && roomNo.equals(postData.getString("room_no")));

                        roomNoList.add(roomNo);
                    }
                }
            } else {
                this.sendPromptToSingle(client, 0, "未加入该" + this.GLOBAL_CLUB_NAME, eventName);
            }
        }
    }

    private int getRoomCostByBaseInfo(JSONObject baseInfo, String payType) {
        try {
            if (baseInfo.containsKey("player") && baseInfo.containsKey("turn")) {
                int player = baseInfo.getInt("player");
                JSONObject turn = baseInfo.getJSONObject("turn");
                if (turn.containsKey("AANum")) {
                    int aaNum = turn.getInt("AANum");
                    if (turn.containsKey("increase")) {
                        aaNum += player > 4 ? player * turn.getInt("increase") : 4 * turn.getInt("increase");
                    }

                    return "1".equals(payType) ? aaNum : player * aaNum;
                }
            }
        } catch (Exception var6) {
        }

        return -1;
    }

    private int getClubCost(String clubCode) {
        int cost = 0;
        Iterator var3 = RoomManage.gameRoomMap.keySet().iterator();

        while(var3.hasNext()) {
            String roomNo = (String)var3.next();
            GameRoom room = (GameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (room.getRoomType() == 6 && clubCode.equals(room.getClubCode()) && !room.isCost()) {
                cost += room.getSinglePayNum() * room.getPlayerCount();
            }
        }

        return cost;
    }

    private void sendPromptToSingle(SocketIOClient client, int code, String msg, String eventName) {
        JSONObject result = new JSONObject();
        result.put("code", code);
        result.put("msg", msg);
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), eventName);
    }

    private List<String> getUserClubList(JSONObject userInfo) {
        String clubIds = userInfo.getString("clubIds");
        return new ArrayList(Arrays.asList(clubIds.substring(1, clubIds.length()).split("\\$")));
    }

    private String getGameNameById(int gameId) {
        JSONObject gameInfo;
        try {
            StringBuffer key = new StringBuffer();
            key.append("game_on_or_off_");
            key.append(gameId);
            Object object = this.redisService.queryValueByKey(String.valueOf(key));
            if (object != null) {
                gameInfo = JSONObject.fromObject(object);
                return gameInfo.getString("name");
            }

            gameInfo = this.roomBiz.getGameInfoByID((long)gameId);
            if (!Dto.isObjNull(gameInfo)) {
                this.redisService.insertKey(String.valueOf(key), String.valueOf(gameInfo), (Long)null);
                return gameInfo.getString("name");
            }
        } catch (Exception var5) {
            gameInfo = this.roomBiz.getGameInfoByID((long)gameId);
            if (!Dto.isObjNull(gameInfo)) {
                return gameInfo.getString("name");
            }
        }

        return null;
    }

    private boolean exitRoom(String roomNo, String account) {
        if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
            GameRoom room = (GameRoom)RoomManage.gameRoomMap.get(roomNo);
            JSONObject exitData = new JSONObject();
            exitData.put("account", account);
            exitData.put("room_no", roomNo);
            exitData.put("notSend", 1);
            exitData.put("notSendToMe", 1);
            switch(room.getGid()) {
                case 1:
                    this.nnGameEventDealNew.exitRoom((SocketIOClient)null, exitData);
                    break;
                case 2:
                    this.ddzGameEventDeal.exitRoom((SocketIOClient)null, exitData);
                    break;
                case 3:
                    this.qzmjGameEventDeal.exitRoom((SocketIOClient)null, exitData);
                    break;
                case 4:
                    this.sssGameEventDealNew.exitRoom((SocketIOClient)null, exitData);
                    break;
                case 5:
                    this.gppjGameEventDeal.exitRoom((SocketIOClient)null, exitData);
                case 6:
                    this.zjhGameEventDealNew.exitRoom((SocketIOClient)null, exitData);
            }
        }

        Iterator var5 = RoomManage.gameRoomMap.keySet().iterator();

        String roomNum;
        do {
            if (!var5.hasNext()) {
                return true;
            }

            roomNum = (String)var5.next();
        } while(!RoomManage.gameRoomMap.containsKey(roomNum) || RoomManage.gameRoomMap.get(roomNum) == null || !((GameRoom)RoomManage.gameRoomMap.get(roomNum)).getPlayerMap().containsKey(account));

        return false;
    }

    public void getClubApplyList(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String clubCode = postData.containsKey("clubCode") ? postData.getString("clubCode") : "";
        String leader = postData.getString("leader");
        String uuid = postData.getString("uuid");
        String eventName = "getClubApplyListPush";
        JSONObject leaderInfo = this.clubBiz.getUserByAccountAndUuid(leader, uuid);
        if (Dto.isObjNull(leaderInfo)) {
            this.sendPromptToSingle(client, 0, "账号已在其他地方登录", eventName);
        } else {
            JSONObject clubInfo = this.clubBiz.getClubByCode(clubCode);
            if (!Dto.isObjNull(clubInfo) && clubInfo.getLong("leaderId") == leaderInfo.getLong("id")) {
                JSONArray clubInviteRec = this.clubBiz.getClubInviteRec(1, clubInfo.getLong("id"));

                for(int i = 0; i < clubInviteRec.size(); ++i) {
                    clubInviteRec.getJSONObject(i).put("headimg", CommonConstant.getServerDomain() + clubInviteRec.getJSONObject(i).getString("headimg"));
                }

                JSONObject result = new JSONObject();
                result.put("code", 1);
                result.put("msg", "获取成功");
                result.put("applyList", clubInviteRec);
                CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), eventName);
            } else {
                this.sendPromptToSingle(client, 0, "无修改权限", eventName);
            }
        }
    }

    public void clubApplyReview(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String clubCode = postData.containsKey("clubCode") ? postData.getString("clubCode") : "";
        String leader = postData.getString("leader");
        String uuid = postData.getString("uuid");
        String account = postData.getString("account");
        int type = postData.getInt("type");
        long clubApplyRecId = (long)postData.getInt("clubApplyRecId");
        String eventName = "clubApplyReviewPush";
        JSONObject leaderInfo = this.clubBiz.getUserByAccountAndUuid(leader, uuid);
        if (Dto.isObjNull(leaderInfo)) {
            this.sendPromptToSingle(client, 0, "账号已在其他地方登录", eventName);
        } else {
            JSONObject clubInfo = this.clubBiz.getClubByCode(clubCode);
            if (!Dto.isObjNull(clubInfo) && clubInfo.getLong("leaderId") == leaderInfo.getLong("id")) {
                if (type != 2 && type != 0) {
                    this.sendPromptToSingle(client, 0, "参数不正确", eventName);
                } else {
                    JSONObject userInfo = this.clubBiz.getUserClubByAccount(account);
                    if (Dto.isObjNull(userInfo)) {
                        this.clubBiz.updateClubInviteRecStatus(0, clubApplyRecId);
                        this.sendPromptToSingle(client, 0, "玩家不存在", eventName);
                    } else if (userInfo.containsKey("clubIds") && !Dto.stringIsNULL(userInfo.getString("clubIds")) && userInfo.getString("clubIds").contains("$" + clubInfo.getLong("id") + "$")) {
                        this.clubBiz.updateClubInviteRecStatus(0, clubApplyRecId);
                        this.sendPromptToSingle(client, 0, "该玩家已在当前" + this.GLOBAL_CLUB_NAME + "中", eventName);
                    } else {
                        this.clubBiz.updateClubInviteRecStatus(type, clubApplyRecId);
                        if (type == 2) {
                            String clubIds = "$" + clubInfo.getLong("id") + "$";
                            if (userInfo.containsKey("clubIds") && !Dto.stringIsNULL(userInfo.getString("clubIds"))) {
                                clubIds = userInfo.getString("clubIds") + clubInfo.getLong("id") + "$";
                            }

                            this.clubBiz.updateUserClubIds(userInfo.getLong("id"), clubIds);
                        }

                        this.sendPromptToSingle(client, 1, "操作成功", eventName);
                    }
                }
            } else {
                this.sendPromptToSingle(client, 0, "无修改权限", eventName);
            }
        }
    }

    public void clubLeaderInvite(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String clubCode = postData.containsKey("clubCode") ? postData.getString("clubCode") : "";
        String leader = postData.getString("leader");
        String uuid = postData.getString("uuid");
        String account = postData.getString("account");
        String eventName = "clubLeaderInvitePush";
        JSONObject leaderInfo = this.clubBiz.getUserByAccountAndUuid(leader, uuid);
        if (Dto.isObjNull(leaderInfo)) {
            this.sendPromptToSingle(client, 0, "账号已在其他地方登录", eventName);
        } else {
            JSONObject clubInfo = this.clubBiz.getClubByCode(clubCode);
            if (!Dto.isObjNull(clubInfo) && clubInfo.getLong("leaderId") == leaderInfo.getLong("id")) {
                JSONObject userInfo = this.clubBiz.getUserClubByAccount(account);
                if (Dto.isObjNull(userInfo)) {
                    this.sendPromptToSingle(client, 0, "玩家不存在", eventName);
                } else if (userInfo.containsKey("clubIds") && !Dto.stringIsNULL(userInfo.getString("clubIds")) && userInfo.getString("clubIds").contains("$" + clubInfo.getLong("id") + "$")) {
                    this.sendPromptToSingle(client, 0, "该玩家已在当前" + this.GLOBAL_CLUB_NAME + "中", eventName);
                } else {
                    this.clubBiz.addClubInviteRec(userInfo.getLong("id"), clubInfo.getLong("id"), clubInfo.getLong("leaderId"), "来自会长邀请", 2);
                    String clubIds = "$" + clubInfo.getLong("id") + "$";
                    if (userInfo.containsKey("clubIds") && !Dto.stringIsNULL(userInfo.getString("clubIds"))) {
                        clubIds = userInfo.getString("clubIds") + clubInfo.getLong("id") + "$";
                    }

                    this.clubBiz.updateUserClubIds(userInfo.getLong("id"), clubIds);
                    this.sendPromptToSingle(client, 1, "邀请成功", eventName);
                }
            } else {
                this.sendPromptToSingle(client, 0, "无修改权限", eventName);
            }
        }
    }

    public void clubLeaderOut(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String clubCode = postData.containsKey("clubCode") ? postData.getString("clubCode") : "";
        String leader = postData.getString("leader");
        String uuid = postData.getString("uuid");
        String account = postData.getString("account");
        String eventName = "clubLeaderOutPush";
        JSONObject leaderInfo = this.clubBiz.getUserByAccountAndUuid(leader, uuid);
        if (Dto.isObjNull(leaderInfo)) {
            this.sendPromptToSingle(client, 0, "账号已在其他地方登录", eventName);
        } else {
            JSONObject clubInfo = this.clubBiz.getClubByCode(clubCode);
            if (!Dto.isObjNull(clubInfo) && clubInfo.getLong("leaderId") == leaderInfo.getLong("id")) {
                JSONObject userInfo = this.clubBiz.getUserClubByAccount(account);
                if (Dto.isObjNull(userInfo)) {
                    this.sendPromptToSingle(client, 0, "玩家不存在", eventName);
                } else if (userInfo.containsKey("clubIds") && !Dto.stringIsNULL(userInfo.getString("clubIds")) && userInfo.getString("clubIds").contains("$" + clubInfo.getLong("id") + "$")) {
                    String clubIds = userInfo.getString("clubIds").replace("$" + clubInfo.getLong("id") + "$", "$");
                    if (clubIds.equals("$")) {
                        clubIds = "";
                    }

                    this.clubBiz.updateUserClubIds(userInfo.getLong("id"), clubIds);
                    this.sendPromptToSingle(client, 1, "移除成功", eventName);
                } else {
                    this.sendPromptToSingle(client, 0, "该玩家已不在当前" + this.GLOBAL_CLUB_NAME + "中", eventName);
                }
            } else {
                this.sendPromptToSingle(client, 0, "无修改权限", eventName);
            }
        }
    }

    public void createClub(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String leader = postData.getString("leader");
        String uuid = postData.getString("uuid");
        String clubName = postData.getString("clubName");
        String platform = postData.getString("platform");
        String eventName = "createClubPush";
        JSONObject leaderInfo = this.clubBiz.getUserByAccountAndUuid(leader, uuid);
        if (Dto.isObjNull(leaderInfo)) {
            this.sendPromptToSingle(client, 0, "账号已在其他地方登录", eventName);
        } else {
            JSONObject sysClubSetting = this.clubBiz.getSysClubSetting(platform);
            if (!Dto.isObjNull(sysClubSetting) && sysClubSetting.containsKey("has_club") && sysClubSetting.getInt("has_club") != 0 && sysClubSetting.containsKey("club_count") && sysClubSetting.containsKey("club_mini_bal") && sysClubSetting.containsKey("club_bal_type")) {
                int maxCount = sysClubSetting.getInt("club_count");
                int clubCount = this.clubBiz.countClubByLeader(leaderInfo.getString("id"), platform);
                if (clubCount >= maxCount) {
                    this.sendPromptToSingle(client, 0, "您创建的" + this.GLOBAL_CLUB_NAME + "已达上限", eventName);
                } else {
                    double sum = sysClubSetting.getDouble("club_mini_bal");
                    int type = sysClubSetting.getInt("club_bal_type");
                    double userBalance = 0.0D;
                    if (type == 4) {
                        userBalance = leaderInfo.getDouble("yuanbao");
                    } else if (type == 1) {
                        userBalance = (double)leaderInfo.getInt("roomcard");
                    }

                    if (userBalance < sum) {
                        this.sendPromptToSingle(client, 0, "余额不足", eventName);
                    } else {
                        String clubCode = this.getClubCode();
                        this.clubBiz.updatePropAndWriteRec(-sum, leaderInfo.getLong("id"), type, 6, platform, "创建" + this.GLOBAL_CLUB_NAME + "充值", 3);
                        JSONObject club = new JSONObject();
                        club.put("clubCode", clubCode);
                        club.put("clubName", clubName);
                        club.put("leaderId", leaderInfo.getLong("id"));
                        club.put("balance", sum);
                        club.put("balance_type", type);
                        club.put("isUse", 1);
                        club.put("platform", platform);
                        club.put("createTime", TimeUtil.getNowDate());
                        club.put("modifyTime", TimeUtil.getNowDate());
                        this.clubBiz.addClub(club);
                        JSONObject clubInfo = this.clubBiz.getClubByCode(clubCode);
                        if (Dto.isObjNull(clubInfo)) {
                            this.sendPromptToSingle(client, 0, "系统繁忙", eventName);
                        } else {
                            String clubIds = "$" + clubInfo.getLong("id") + "$";
                            if (leaderInfo.containsKey("clubIds") && !Dto.stringIsNULL(leaderInfo.getString("clubIds"))) {
                                clubIds = leaderInfo.getString("clubIds") + clubInfo.getLong("id") + "$";
                            }

                            this.clubBiz.updateUserClubIds(leaderInfo.getLong("id"), clubIds);
                            this.sendPromptToSingle(client, 1, "创建成功", eventName);
                        }
                    }
                }
            } else {
                this.sendPromptToSingle(client, 0, "当前未开放", eventName);
            }
        }
    }

    private String getClubCode() {
        String clubCode = MathDelUtil.getRandomStr(6);
        JSONObject clubInfo = this.clubBiz.getClubByCode(clubCode);
        return !Dto.isObjNull(clubInfo) ? this.getClubCode() : clubCode;
    }

    public void clubRecharge(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String clubCode = postData.containsKey("clubCode") ? postData.getString("clubCode") : "";
        String leader = postData.getString("leader");
        String uuid = postData.getString("uuid");
        double sum = postData.getDouble("sum");
        String eventName = "clubRechargePush";
        if (sum <= 0.0D) {
            this.sendPromptToSingle(client, 0, "请输入正确的数值", eventName);
        } else {
            JSONObject leaderInfo = this.clubBiz.getUserByAccountAndUuid(leader, uuid);
            if (Dto.isObjNull(leaderInfo)) {
                this.sendPromptToSingle(client, 0, "账号已在其他地方登录", eventName);
            } else {
                JSONObject clubInfo = this.clubBiz.getClubByCode(clubCode);
                if (!Dto.isObjNull(clubInfo) && clubInfo.getLong("leaderId") == leaderInfo.getLong("id")) {
                    int type = clubInfo.getInt("balance_type");
                    double userBalance = 0.0D;
                    if (type == 4) {
                        userBalance = leaderInfo.getDouble("yuanbao");
                    } else if (type == 1) {
                        userBalance = (double)leaderInfo.getInt("roomcard");
                    }

                    if (userBalance < sum) {
                        this.sendPromptToSingle(client, 0, "余额不足", eventName);
                    } else {
                        boolean addResult = this.clubBiz.updatePropAndWriteRec(-sum, leaderInfo.getLong("id"), clubInfo.getInt("balance_type"), 6, clubInfo.getString("platform"), this.GLOBAL_CLUB_NAME + "充值", 3);
                        if (!addResult) {
                            this.sendPromptToSingle(client, 0, "系统繁忙，请稍后再试", eventName);
                        } else {
                            this.clubBiz.updateClubBalance(clubInfo.getLong("id"), -sum);
                            this.sendPromptToSingle(client, 1, "充值成功", eventName);
                        }
                    }
                } else {
                    this.sendPromptToSingle(client, 0, "无修改权限", eventName);
                }
            }
        }
    }
}
