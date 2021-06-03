

package com.zhuoan.exception;

import com.zhuoan.enumtype.ResCodeEnum;
import java.util.Map;

public class BizException extends RuntimeException {
    private Map<String, String> mesageMap;
    private String message;
    private String code;

    public BizException(String message) {
        this.message = message;
    }

    public BizException(String message, String code) {
        this.message = message;
        this.code = code;
    }

    public BizException(Map<String, String> messageMap, String message, String code) {
        this(message, code);
        this.mesageMap = messageMap;
    }

    public BizException(ResCodeEnum resCodeEnum) {
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
        StringBuilder sb = new StringBuilder("BizException{");
        sb.append("message='").append(this.message).append('\'');
        sb.append(", code='").append(this.code).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
