package com.zhuoan.constant;

public class SwConstant {
    public static final int TREASURE_BLACK_ROOK = 1;
    public static final int TREASURE_BLACK_KNIGHT = 2;
    public static final int TREASURE_BLACK_CANNON = 3;
    public static final int TREASURE_BLACK_ELEPHANT = 4;
    public static final int TREASURE_BLACK_MANDARIN = 5;
    public static final int TREASURE_BLACK_KING = 6;
    public static final int TREASURE_RED_ROOK = 7;
    public static final int TREASURE_RED_KNIGHT = 8;
    public static final int TREASURE_RED_CANNON = 9;
    public static final int TREASURE_RED_ELEPHANT = 10;
    public static final int TREASURE_RED_MANDARIN = 11;
    public static final int TREASURE_RED_KING = 12;
    public static final int SW_GAME_STATUS_INIT = 0;
    public static final int SW_GAME_STATUS_READY = 1;
    public static final int SW_GAME_STATUS_BET = 2;
    public static final int SW_GAME_STATUS_SHOW = 3;
    public static final int SW_GAME_STATUS_SUMMARY = 4;
    public static final int SW_GAME_STATUS_CHOICE_BANKER = 5;
    public static final int SW_GAME_STATUS_HIDE_TREASURE = 6;
    public static final int SUMMARY_RESULT_WIN = 1;
    public static final int SUMMARY_RESULT_NO_IN = 0;
    public static final int SUMMARY_RESULT_LOSE = -1;
    public static final int SW_GAME_EVENT_START_GAME = 1;
    public static final int SW_GAME_EVENT_BET = 2;
    public static final int SW_GAME_EVENT_BE_BANKER = 3;
    public static final int SW_GAME_EVENT_UNDO = 4;
    public static final int SW_GAME_EVENT_EXIT_ROOM = 5;
    public static final int SW_GAME_EVENT_CHANGE_SEAT = 6;
    public static final int SW_GAME_EVENT_RECONNECT = 7;
    public static final int SW_GAME_EVENT_GET_HISTORY = 8;
    public static final int SW_GAME_EVENT_GET_ALL_USER = 9;
    public static final int SW_GAME_EVENT_HIDE_TREASURE = 10;
    public static final int SW_GAME_EVENT_GET_UNDO_INFO = 11;
    public static final int SW_MIN_SEAT_NUM = 0;
    public static final int SW_MAX_SEAT_NUM = 18;
    public static final int SW_HISTORY_TREASURE_SIZE = 30;
    public static final int SW_TYPE_TEN = 0;
    public static final int SW_TYPE_TEN_POINT_FIVE = 1;
    public static final int SW_TYPE_TEN_ELEVEN = 2;
    public static final int SW_TYPE_TEN_ELEVEN_POINT_FIVE = 3;
    public static final int SW_TYPE_TEN_TWELVE = 4;
    public static final int SW_TIME_HIDE_TREASURE = 60;
    public static final int SW_TIME_BET = 70;
    public static final int SW_TIME_SHOW = 10;
    public static final int SW_TIME_START = 90;
    public static final int SW_TIME_SUMMARY_ANIMATION = 4;
    public static final int SW_USER_SIZE_PER_PAGE = 30;
    public static final int UN_BET_TIME = 3;
    public static final String SW_DATA_KEY_TREASURE = "treasure";
    public static final String SW_DATA_KEY_PLACE = "place";
    public static final String SW_DATA_KEY_VALUE = "value";
    public static final String SW_DATA_KEY_INDEX = "index";

    public SwConstant() {
    }
}
