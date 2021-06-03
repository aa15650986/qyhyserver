
package com.zhuoan.constant;

public class GPPJConstant {
    public static final int BANKER_TYPE_OWNER = 0;
    public static final int BANKER_TYPE_LOOK = 1;
    public static final int BANKER_TYPE_COMPARE = 2;
    public static final int BANKER_TYPE_TURN = 3;
    public static final int BANKER_TYPE_ROB = 4;
    public static final int GP_PJ_GAME_STATUS_INIT = 0;
    public static final int GP_PJ_GAME_STATUS_READY = 1;
    public static final int GP_PJ_GAME_STATUS_CUT = 2;
    public static final int GP_PJ_GAME_STATUS_QZ = 3;
    public static final int GP_PJ_GAME_STATUS_XZ = 4;
    public static final int GP_PJ_GAME_STATUS_SHOW = 5;
    public static final int GP_PJ_GAME_STATUS_SUMMARY = 6;
    public static final int GP_PJ_GAME_STATUS_FINAL_SUMMARY = 7;
    public static final int GP_PJ_GAME_STATUS_RESHUFFLE = 8;
    public static final int GP_PJ_USER_STATUS_INIT = 0;
    public static final int GP_PJ_USER_STATUS_READY = 1;
    public static final int GP_PJ_USER_STATUS_CUT = 2;
    public static final int GP_PJ_USER_STATUS_QZ = 3;
    public static final int GP_PJ_USER_STATUS_XZ = 4;
    public static final int GP_PJ_USER_STATUS_SHOW = 5;
    public static final int GP_PJ_TIMER_INIT = 0;
    public static final int GP_PJ_BTN_TYPE_NONE = 0;
    public static final int GP_PJ_BTN_TYPE_READY = 1;
    public static final int GP_PJ_BTN_TYPE_ROB = 2;
    public static final int GP_PJ_BTN_TYPE_RESHUFFLE = 3;
    public static final int GP_PJ_MIN_START_COUNT = 2;
    public static final int GP_PJ_GAME_EVENT_READY = 1;
    public static final int GP_PJ_GAME_EVENT_START = 2;
    public static final int GP_PJ_GAME_EVENT_CUT = 3;
    public static final int GP_PJ_GAME_EVENT_QZ = 4;
    public static final int GP_PJ_GAME_EVENT_XZ = 5;
    public static final int GP_PJ_GAME_EVENT_SHOW = 6;
    public static final int GP_PJ_GAME_EVENT_RECONNECT = 7;
    public static final int GP_PJ_GAME_EVENT_EXIT = 8;
    public static final int GP_PJ_GAME_EVENT_CLOSE_ROOM = 9;
    public static final int GP_PJ_GAME_EVENT_RESHUFFLE = 10;
    public static final int COMPARE_RESULT_WIN = 1;
    public static final int COMPARE_RESULT_EQUALS = 0;
    public static final int COMPARE_RESULT_LOSE = -1;
    public static final int MULTIPLE_TYPE_ZZ_DOUBLE = 0;
    public static final int START_GAME_TYPE_UNSURE = 1;
    public static final int START_GAME_TYPE_SURE = 2;
    public static final int SLEEP_TYPE_NONE = 0;
    public static final int SLEEP_TYPE_START_GAME = 1;
    public static final int GP_PJ_TIME_CUT = 10;
    public static final int GP_PJ_TIME_QZ = 10;
    public static final int GP_PJ_TIME_XZ = 10;
    public static final int GP_PJ_TIME_SHOW = 60;
    public static final int GP_PJ_TIME_RESHUFFLE = 60;
    public static final int SLEEP_TIME_START_GAME = 2500;
    public static final int CARD_SIZE = 2;
    public static final String DATA_KEY_QZ_TIMES = "qzTimes";
    public static final String DATA_KEY_XZ_TIMES = "xzTimes";
    public static final String DATA_KEY_CUT_PLACE = "cutPlace";
    public static final String DATA_KEY_TYPE = "type";
    public static final String ROOM_SETTING_KEY_CUT_TIME = "cutTime";
    public static final String ROOM_SETTING_KEY_ROB_TIME = "robTime";
    public static final String ROOM_SETTING_KEY_BET_TIME = "betTime";
    public static final String ROOM_SETTING_KEY_SHOW_TIME = "showTime";
    public static final String ROOM_SETTING_KEY_RESHUFFLE_TIME = "reshuffleTime";

    public GPPJConstant() {
    }
}
