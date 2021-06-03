
package com.zhuoan.constant.event.enmu;

public enum TeaEventEnum {
    TEST(0, "tea_testasd1"),
    TEA_LIST(1, "tea_listEventasd1"),
    TEA_CREATOR(2, "tea_creatorEventasd1"),
    TEA_JOIN(3, "tea_joinEventasd1"),
    TEA_MODIFY_NAME(4, "tea_modifyNameEventasd1"),
    TEA_MODIFY_PLAY(5, "tea_modifyPlayEventasd1"),
    TEA_ADD_MEMBER(6, "tea_addMemberEventasd1"),
    TEA_IMPORT_MEMBER_LIST(7, "tea_importMemberListEventasd1"),
    TEA_IMPORT_MEMBER(8, "tea_importMemberEventasd1"),
    TEA_CLOSES(9, "tea_closesEventasd1"),
    TEA_REMOVE(10, "tea_removeEventasd1"),
    TEA_MEMBER_LIST(11, "tea_memberListEventasd1"),
    TEA_AUDIT_MEMBER(12, "tea_auditMemberEventasd1"),
    TEA_AUDIT_MEMBER_LIST(13, "tea_auditMemberListEventasd1"),
    TEA_EXIT_INFO_LIST(14, "tea_exitInfoListEventasd1"),
    TEA_ADMIN_LIST(15, "tea_adminListEventasd1"),
    TEA_MODIFY_ADMIN(16, "tea_modifyAdminEventasd1"),
    TEA_MY_RECORD_LIST(17, "tea_myRecordListEventasd1"),
    TEA_RECORD_LIST(18, "tea_recordListEventasd1"),
    TEA_RECORD_STATIS_LIST(19, "tea_recordStatisListEventasd1"),
    TEA_BIG_WINNER_LIST(20, "tea_bigWinnerListEventasd1"),
    TEA_QUICK_JOIN(21, "tea_quickJoinEventasd1"),
    TEA_INFO(22, "tea_infoEventasd1"),
    TEA_EXIT_MEMBER(23, "tea_exitMemberEventasd1"),
    TEA_MODIFY_INFO_RECORD(24, "tea_modifyInfoRecordEventasd1"),
    TEA_ROOM_LIST(25, "tea_roomListEventasd1"),
    TEA_REMOVE_ROOM(26, "tea_removeRoomEventasd1"),
    TEA_DEL_MEMBER(27, "tea_delMemberEventasd1"),
    TEA_JOIN_ROOM(28, "tea_joinRoomEventasd1"),
    TEA_QUIT_ROOM(29, "tea_quitRoomEventasd1"),
    TEA_RECORD_INFO(30, "tea_recordInfoEventasd1"),
    TEA_MODIFY_PROXY_MONEY(31, "tea_modifyProxyMoneyEventasd1"),
    TEA_MODIFY_TEA_MONEY(32, "tea_modifyTeaMoneyEventasd1"),
    TEA_GET_MEMBER_INFO(33, "tea_getMemberInfoEventasd1"),
    TEA_MEMBER_MSG_READ(34, "tea_memberMsgReadEventasd1"),
    TEA_MONEY_RECORD_LIST(35, "tea_moneyRecordListEventasd1"),
    TEA_RECORD_TICK(36, "tea_recordTickEventasd1"),
    TEA_MODIFY_MEMBER_SCORE_LOCK(37, "tea_modifyMemberScoreLockEventasd1"),
    TEA_MODIFY_MEMBER_USE_STATUS(38, "tea_modifyMemberUseStatusEventasd1");

    private int type;
    private String event;

    private TeaEventEnum(int type, String event) {
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
        return this.event.split("asd1")[0] + "_push";
    }
    
    
}
