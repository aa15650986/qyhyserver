package com.zhuoan.constant;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.Dto;
import com.zhuoan.util.SpringTool;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.sf.json.JSONObject;

public class CommonConstant {
    public static String RESOURCE_DOMAIN;
    public static Map<String, String> socketMap;
    public static final int ROOM_VISIT_YES = 1;
    public static final int ROOM_VISIT_NO = 0;
    public static final int ROOM_VISIT_INDEX = -99;
    public static final String CLOSING_DOWN_MSG = "服务器即将维护,无法开始游戏。";
    public static final int BASE_GAME_GET_USER_INFO = 1;
    public static final int BASE_GAME_EVENT_CHECK_USER = 2;
    public static final int BASE_GAME_EVENT_GET_GAME_SETTING = 3;
    public static final int BASE_GAME_EVENT_GET_ALL_ROOM_LIST = 4;
    public static final int BASE_GAME_EVENT_CREATE_ROOM = 5;
    public static final int BASE_GAME_EVENT_JOIN_ROOM = 6;
    public static final int BASE_GAME_EVENT_GET_SHUFFLE_INFO = 7;
    public static final int BASE_GAME_EVENT_DO_SHUFFLE = 8;
    public static final int BASE_GAME_EVENT_SEND_MESSAGE = 9;
    public static final int BASE_GAME_EVENT_SEND_VOICE = 10;
    public static final int BASE_GAME_EVENT_GET_USER_GAME_LOGS = 11;
    public static final int BASE_GAME_EVENT_DISSOLVE_ROOM = 12;
    public static final int BASE_GAME_EVENT_SEND_NOTICE = 14;
    public static final int BASE_GAME_EVENT_GET_NOTICE = 15;
    public static final int BASE_GAME_EVENT_GET_ROOM_AND_PLAYER_COUNT = 16;
    public static final int BASE_GAME_EVENT_SON_GAME = 18;
    public static final int BASE_GAME_EVENT_JOIN_COIN_ROOM = 19;
    public static final int BASE_GAME_EVENT_GET_ROOM_CARD_PAY_INFO = 20;
    public static final int BASE_GAME_EVENT_GET_COIN_SETTING = 21;
    public static final int BASE_GAME_EVENT_GET_USER_SIGN_INFO = 22;
    public static final int BASE_GAME_EVENT_DO_USER_SIGN = 23;
    public static final int BASE_GAME_EVENT_GET_COMPETITIVE_INFO = 24;
    public static final int BASE_GAME_EVENT_JOIN_COMPETITIVE_ROOM = 25;
    public static final int BASE_GAME_EVENT_CHECK_IP = 26;
    public static final int BASE_GAME_EVENT_GET_PROXY_ROOM_LIST = 27;
    public static final int BASE_GAME_EVENT_DISSOLVE_PROXY_ROOM = 28;
    public static final int BASE_GAME_EVENT_GET_USER_ACHIEVEMENT_INFO = 29;
    public static final int BASE_GAME_EVENT_GET_PROPS_INFO = 30;
    public static final int BASE_GAME_EVENT_USER_PURCHASE = 31;
    public static final int BASE_GAME_EVENT_GET_ACHIEVEMENT_RANK = 32;
    public static final int BASE_GAME_EVENT_GET_DRAW_INFO = 33;
    public static final int BASE_GAME_EVENT_GAME_DRAW = 34;
    public static final int BASE_GAME_EVENT_GET_ACHIEVEMENT_DETAIL = 35;
    public static final int BASE_GAME_EVENT_DRAW_ACHIEVEMENT_REWARD = 36;
    public static final int BASE_GAME_EVENT_CHANGE_ROOM = 37;
    public static final int BASE_GAME_EVENT_GET_ROOM_CARD_GAME_LOG = 38;
    public static final int BASE_GAME_EVENT_GET_ROOM_CARD_GAME_LOG_DETAIL = 39;
    public static final int BASE_GAME_EVENT_GET_CLUB_GAME_LOG = 40;
    public static final int BASE_GAME_EVENT_GET_BACKPACK_INFO = 41;
    public static final int BASE_GAME_EVENT_CHECK_BIND_STATUS = 42;
    public static final int BASE_GAME_EVENT_USER_BIND = 43;
    public static final int BASE_GAME_EVENT_REFRESH_LOCATION = 44;
    public static final int BASE_GAME_EVENT_GET_LOCATION = 45;
    public static final int GAME_BASE = 0;
    public static final int GAME_MATCH = 100;
    public static final int GAME_CLUB = 101;
    public static final int GAME_CIRCLE_BASE = 200;
    public static final int GAME_TEA = 210;
    public static final int GAME_ID_NN = 1;
    public static final int GAME_ID_DDZ = 2;
    public static final int GAME_ID_QZMJ = 3;
    public static final int GAME_ID_SSS = 4;
    public static final int GAME_ID_GP_PJ = 5;
    public static final int GAME_ID_ZJH = 6;
    public static final int GAME_ID_BDX = 10;
    public static final int GAME_ID_NAMJ = 12;
    public static final int GAME_ID_ZZC = 13;
    public static final int GAME_ID_SW = 14;
    public static final int GAME_ID_MJ_PJ = 17;
    public static final int GAME_ID_GDY = 18;
    public static final int GAME_ID_SG = 19;
    public static final int GAME_ID_GZMJ = 20;
    public static final int GAME_ID_PDK = 21;
    public static final int ROOM_TYPE_FK = 0;
    public static final int ROOM_TYPE_JB = 1;
    public static final int ROOM_TYPE_DK = 2;
    public static final int ROOM_TYPE_YB = 3;
    public static final int ROOM_TYPE_COMPETITIVE = 4;
    public static final int ROOM_TYPE_MATCH = 5;
    public static final int ROOM_TYPE_CLUB = 6;
    public static final int ROOM_TYPE_FRIEND = 7;
    public static final int ROOM_TYPE_FREE = 8;
    public static final int ROOM_TYPE_INNING = 9;
    public static final int ROOM_TYPE_TEA = 10;
    public static final String START_STATUS = "is_start_game";
    public static final int ROOM_STATUS_YES = 1;
    public static final int ROOM_STATUS_NO = 0;
    public static final int GLOBAL_YES = 1;
    public static final int GLOBAL_NO = 0;
    public static final int READY_OVERTIME_NOTHING = 0;
    public static final int READY_OVERTIME_AUTO = 1;
    public static final int READY_OVERTIME_OUT = 2;
    public static final int CLOSE_ROOM_AGREE = 1;
    public static final int CLOSE_ROOM_UNSURE = 0;
    public static final int CLOSE_ROOM_DISAGREE = -1;
    public static final int DISSMISS_ROLE_WJ = 1;
    public static final int DISSMISS_ROLE_MSG = 0;
    public static final int CHECK_GAME_STATUS_NO = -1;
    public static final int NOTICE_TYPE_MALL = 1;
    public static final int NOTICE_TYPE_GAME = 2;
    public static final int NOTICE_TYPE_ALL = 3;
    public static final int NOTICE_TYPE_ALERT = 4;
    public static final int SHOW_MSG_TYPE_NORMAL = 0;
    public static final int SHOW_MSG_TYPE_SMALL = 1;
    public static final int SHOW_MSG_TYPE_BIG = 2;
    public static final int SCORE_CHANGE_TYPE_OTHER = 3;
    public static final int SCORE_CHANGE_TYPE_PUMP = 10;
    public static final int SCORE_CHANGE_TYPE_GAME = 20;
    public static final int SCORE_CHANGE_TYPE_SHUFFLE = 30;
    public static final int SCORE_CHANGE_DO_TYPE_WELFARE = 4;
    public static final int NO_BANKER_INDEX = -1;
    public static final int COINS_SIGN_MIN = 100;
    public static final int COINS_SIGN_MAX = 1000;
    public static final int COINS_SIGN_BASE = 0;
    public static final int CLOSE_ROOM_TYPE_DISSOLVE = -1;
    public static final int CLOSE_ROOM_TYPE_FINISH = -2;
    public static final int CLOSE_ROOM_TYPE_INIT = 0;
    public static final int CLOSE_ROOM_TYPE_GAME = 1;
    public static final int CLOSE_ROOM_TYPE_OVER = 2;
    public static final int GAME_LOG_SIZE_PER_PAGE = 20;
    public static final int PROPS_TYPE_JPQ = 1;
    public static final int PROPS_TYPE_DOUBLE_CARD = 2;
    public static final int TICKET_TYPE_MONEY = 3;
    public static final int TICKET_TYPE_THING = 2;
    public static final int TICKET_TYPE_ROOM_CARD = 0;
    public static final int SIGN_INFO_EVENT_TYPE_HALL = 0;
    public static final int SIGN_INFO_EVENT_TYPE_CLICK = 1;
    public static final String ROOM_PAY_TYPE_FANGZHU = "0";
    public static final String ROOM_PAY_TYPE_AA = "1";
    public static final String ROOM_PAY_TYPE_FUND = "2";
    public static final String ROOM_PAY_TYPE_TEA = "3";
    public static final String ROOM_PAY_TYPE_TEA_AA = "4";
    public static final int NOTICE_DATABASE_TYPE_ROLL = 2;
    public static final int NOTICE_DATABASE_TYPE_ALERT = 4;
    public static final int CURRENCY_TYPE_ROOM_CARD = 1;
    public static final int CURRENCY_TYPE_COINS = 2;
    public static final int CURRENCY_TYPE_SCORE = 3;
    public static final int CURRENCY_TYPE_YB = 4;
    public static final String IS_ALL_START_Y = "1";
    public static final String IS_ALL_START_N = "0";
    public static final String DATA_KEY_ROOM_NO = "room_no";
    public static final String DATA_KEY_ACCOUNT = "account";
    public static final String DATA_KEY_PLATFORM = "platform";
    public static final String USERS_ID = "uid";
    public static final String ROOM_NO = "roomNo";
    public static final String USERS_TOKEN = "token";
    public static final String RESULT_KEY_CODE = "code";
    public static final String RESULT_KEY_MSG = "msg";
    public static final String RESULT_KEY_ERROR = "error_msg";
    public static String SECRET_KEY_ZOB = "zhoan";
    public static List<String> fundPlatformList = Arrays.asList("ZOBQP");
    public static List<String> weekSignPlatformList = Arrays.asList("YQWDDZ");
    public static List<String> jwdList = Arrays.asList("24.8939108980,118.6029052734", "24.9711199276,118.8391113281", "24.6969342264,118.7155151367", "25.5176574300,118.4655761719", "24.4721504372,118.1195068359", "24.5021449012,117.6333618164", "25.4011039369,118.7237548828", "25.7998911821,119.4241333008", "24.5171394505,117.5811767578", "25.4135086080,118.9791870117");

    public CommonConstant() {
    }

    public static String getServerDomain() {
        if (null == socketMap || !socketMap.containsKey("headimgUrl")) {
            RedisInfoService redisInfoService = (RedisInfoService)SpringTool.getBean(RedisInfoService.class);
            socketMap = redisInfoService.getSokcetInfo();
        }

        return (String)socketMap.get("headimgUrl");
    }

    public static String getResourceDomain() {
        if (null == RESOURCE_DOMAIN || "".equals(RESOURCE_DOMAIN)) {
            RESOURCE_DOMAIN = Constant.cfgProperties.getProperty("resource_domain");
        }

        return RESOURCE_DOMAIN;
    }

    public static boolean checkEvent(JSONObject postData, int gameStatus, SocketIOClient client) {
        if (postData.containsKey("room_no") && postData.containsKey("account")) {
            String roomNo = postData.getString("room_no");
            String account = postData.getString("account");
            if (!Dto.stringIsNULL(roomNo) && RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
                GameRoom gameRoom = (GameRoom)RoomManage.gameRoomMap.get(roomNo);
                if (!Dto.stringIsNULL(account) && gameRoom.getPlayerMap().containsKey(account) && gameRoom.getPlayerMap().get(account) != null) {
                    if (null != client && !client.getSessionId().equals(((Playerinfo)gameRoom.getPlayerMap().get(account)).getUuid())) {
                        return false;
                    } else {
                        return gameStatus == -1 || gameRoom.getGameStatus() == gameStatus;
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

    public static void sendMsgEventToAll(List<UUID> uuidList, String data, String eventName) {
        Iterator var3 = uuidList.iterator();

        while(var3.hasNext()) {
            UUID uuid = (UUID)var3.next();
            if (uuid != null) {
                SocketIOClient client = GameMain.server.getClient(uuid);
                if (null != client) {
                    client.sendEvent(eventName, new Object[]{JSONObject.fromObject(data)});
                }
            }
        }

    }

    public static void sendMsgEventAll(List<UUID> uuidList, Object data, String eventName) {
        if (uuidList != null && uuidList.size() != 0) {
            Iterator var3 = uuidList.iterator();

            while(var3.hasNext()) {
                UUID uuid = (UUID)var3.next();
                if (uuid != null) {
                    SocketIOClient client = GameMain.server.getClient(uuid);
                    if (null != client) {
                        client.sendEvent(eventName, new Object[]{com.alibaba.fastjson.JSONObject.parse(data.toString())});
                    }
                }
            }

        }
    }

    public static void sendMsgEventToSingle(UUID uuid, String data, String eventName) {
        SocketIOClient client = GameMain.server.getClient(uuid);
        if (null != client) {
            client.sendEvent(eventName, new Object[]{JSONObject.fromObject(data)});
        }

    }

    public static void sendMsgEventToSingle(UUID uuid, Object data, String eventName) {
        SocketIOClient client = GameMain.server.getClient(uuid);
        if (null != client) {
            client.sendEvent(eventName, new Object[]{com.alibaba.fastjson.JSONObject.parse(data.toString())});
        }

    }

    public static void sendMsgEventToSingle(SocketIOClient client, String data, String eventName) {
        if (null != client) {
            client.sendEvent(eventName, new Object[]{JSONObject.fromObject(data)});
        }

    }

    public static void sendMsgEvent(SocketIOClient client, Object data, String eventName) {
        if (null != client) {
            client.sendEvent(eventName, new Object[]{com.alibaba.fastjson.JSONObject.parse(data.toString())});
        }

    }

    public static void sendMsgEventNo(SocketIOClient client, String msg, String errorMsg, String eventName) {
        JSONObject object = new JSONObject();
        object.element("code", 0);
        if (null != msg) {
            object.element("msg", msg);
        }

        if (null != errorMsg) {
            object.element("error_msg", errorMsg);
        }

        if (null != client) {
        	System.out.println("向客户端返回加入失败信息");
            client.sendEvent(eventName, new Object[]{object});
        }

    }

    public static void sendMsgEventYes(SocketIOClient client, String msg, String eventName) {
        JSONObject object = new JSONObject();
        if (null != msg) {
            object.element("code", 1);
        }

        object.element("msg", msg);
        if (null != client) {
            client.sendEvent(eventName, new Object[]{object});
        }

    }
}
