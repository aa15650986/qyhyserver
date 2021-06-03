package com.zhuoan.enumtype;

public enum ResCodeEnum {
    RES_MSG("resMsg"),
    RES_CODE("resCode"),
    SUCCESS("000000", "成功"),
    SYSTEM_EXCEPTION("900001", "系统异常"),
    OTHER("111111", "我是返回信息");

    private String resCode;
    private String resMessage;

    private ResCodeEnum(String resCode) {
        this.resCode = resCode;
    }

    private ResCodeEnum(String resCode, String resMessage) {
        this.resCode = resCode;
        this.resMessage = resMessage;
    }

    public String getResCode() {
        return this.resCode;
    }

    public void setResCode(String resCode) {
        this.resCode = resCode;
    }

    public String getResMessage() {
        return this.resMessage;
    }

    public void setResMessage(String resMessage) {
        this.resMessage = resMessage;
    }
}
