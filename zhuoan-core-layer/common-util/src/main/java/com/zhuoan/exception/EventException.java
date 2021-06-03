
package com.zhuoan.exception;

import com.zhuoan.enumtype.ResCodeEnum;
import java.util.Map;

public class EventException extends RuntimeException {
    private Map<String, String> mesageMap;
    private String message;
    private String code;

    public EventException(String message) {
        this.message = message;
    }

    public EventException(String message, String code) {
        this.message = message;
        this.code = code;
    }

    public EventException(Map<String, String> messageMap, String message, String code) {
        this(message, code);
        this.mesageMap = messageMap;
    }

    public EventException(ResCodeEnum resCodeEnum) {
        this.message = resCodeEnum.getResMessage();
        this.code = resCodeEnum.getResCode();
    }

    public String getMessage() {
        return this.message;
    }

    public String getCode() {
        return this.code;
    }

    public Map<String, String> getMessageMap() {
        return this.mesageMap;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("EventException{");
        sb.append("message='").append(this.message).append('\'');
        sb.append(", code='").append(this.code).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
