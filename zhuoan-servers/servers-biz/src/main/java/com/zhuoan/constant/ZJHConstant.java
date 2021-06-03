package com.zhuoan.constant;

public class ZJHConstant {
    public static final int ZJH_GAME_EVENT_READY = 1;
    public static final int ZJH_GAME_EVENT_GAME = 2;
    public static final int ZJH_GAME_EVENT_EXIT = 3;
    public static final int ZJH_GAME_EVENT_RECONNECT = 4;
    public static final int ZJH_GAME_EVENT_CLOSE_ROOM = 5;
    public static final int ZJH_GAME_STATUS_INIT = 0;
    public static final int ZJH_GAME_STATUS_READY = 1;
    public static final int ZJH_GAME_STATUS_GAME = 2;
    public static final int ZJH_GAME_STATUS_SUMMARY = 3;
    public static final int ZJH_GAME_STATUS_FINAL_SUMMARY = 4;
    public static final int ZJH_USER_STATUS_INIT = 0;
    public static final int ZJH_USER_STATUS_READY = 1;
    public static final int ZJH_USER_STATUS_AP = 2;
    public static final int ZJH_USER_STATUS_KP = 3;
    public static final int ZJH_USER_STATUS_QP = 4;
    public static final int ZJH_USER_STATUS_LOSE = 5;
    public static final int ZJH_USER_STATUS_WIN = 6;
    public static final int ZJH_USER_STATUS_SUMMARY = 7;
    public static final int ZJH_USER_STATUS_FINAL_SUMMARY = 8;
    public static final int ZJH_GAME_TYPE_CLASSIC = 0;
    public static final int ZJH_GAME_TYPE_MEN = 1;
    public static final int ZJH_GAME_TYPE_HIGH = 2;
    public static final int ZJH_MIN_START_COUNT = 2;
    public static final int ZJH_TIMER_INIT = 0;
    public static final int ZJH_TIMER_READY = 15;
    public static final int ZJH_TIMER_READY_INNING = 5;
    public static final int ZJH_TIMER_XZ = 15;
    public static final int ZJH_TIMER_CLOSE = 60;
    public static final int GAME_ACTION_TYPE_GDD = 1;
    public static final int GAME_ACTION_TYPE_GIVE_UP = 2;
    public static final int GAME_ACTION_TYPE_COMPARE = 3;
    public static final int GAME_ACTION_TYPE_LOOK = 4;
    public static final int GAME_ACTION_TYPE_GZ = 5;
    public static final int GAME_ACTION_TYPE_JZ = 6;
    public static final int GAME_ACTION_TYPE_FINAL_SUMMARY = 8;
    public static final int START_GAME_TYPE_UNSURE = 0;
    public static final int GAME_ACTION_TYPE_COMPARE_FINISH = 9;

    public ZJHConstant() {
    }
    
    public static void main(String[] args) {
		System.out.println(System.currentTimeMillis());
	}
}
