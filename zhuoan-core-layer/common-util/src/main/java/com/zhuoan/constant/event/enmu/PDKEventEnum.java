package com.zhuoan.constant.event.enmu;

public enum PDKEventEnum {
    TEST(0, "pdk_testasd1"),
    PDK_GAME_READY(1, "pdk_gameReadyEventasd1"),
    PDK_GAME(2, "pdk_gameEventasd1"),
    PDK_RECONNECT_GAME(3, "pdk_reconnectGameEventasd1"),
    PDK_EXIT_ROOM(4, "pdk_exitRoomeEventasd1"),
    PDK_CLOSE_ROOM(5, "pdk_closeRoomEventasd1"),
    PDK_GAME_TRUSTEE(6, "pdk_gameTrusteeEventasd1"),
    PDK_GAME_TIP(7, "pdk_gameTipEventasd1"),
    PDK_GAME_FORCED_ROOM_EVENT(8, "pdk_forcedRoomEvent");

    private int type;
    private String event;
    public static final String PDK_ENTER_ROOM = "pdk_enterRoom_push";
    public static final String PDK_PLAYER_ENTER = "pdk_playerEnter_push";
    public static final String PDK_READY_TIMER = "pdk_readyTimer_push";
    public static final String PDK_SETTLE_ACCOUNTS = "pdk_settleAccounts_push";

    private PDKEventEnum(int type, String event) {
        this.type = type;
        this.event = event;
    }

    public int getType() {
        return this.type;
    }

    public String getEvent() {
        return this.event;
    }

    public String getEventPush() {
    	
    	String retuenEvent = this.event.substring(0,event.length()-4);
        return retuenEvent + "_push";
    }
}
