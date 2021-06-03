package com.zhuoan.constant;

public class RegexConstant {
    public static final String REGEX_USERNAME = "^[a-zA-Z]\\w{5,20}$";
    public static final String REGEX_PASSWORD = "^[a-zA-Z0-9]{6,20}$";
    public static final String REGEX_MOBILE = "^((17[0-9])|(14[0-9])|(13[0-9])|(15[^4,\\D])|(18[0-9]))\\d{8}$";
    public static final String REGEX_EMAIL = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
    public static final String REGEX_CHINESE = "^[一-龥],{0,}$";
    public static final String REGEX_ID_CARD = "(^\\d{18}$)|(^\\d{15}$)";
    public static final String REGEX_URL = "http(s)?://([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=]*)?";
    public static final String REGEX_IP_ADDRESS = "(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)";

    public RegexConstant() {
    }
}
