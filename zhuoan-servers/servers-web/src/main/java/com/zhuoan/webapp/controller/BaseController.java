
package com.zhuoan.webapp.controller;

import com.alibaba.fastjson.JSONObject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseController {
    private static final Logger logger = LoggerFactory.getLogger(BaseController.class);
    protected static final Integer PAGE_DEFAULT = 1;
    protected static final Integer LIMIT_DEFAULT = 10;
    private static final String IPV4_LOCAL = "127.0.0.1";
    private static final String IPV6_LOCAL = "0:0:0:0:0:0:0:1";
    private static final String IP_UNKNOWN = "unknown";

    public BaseController() {
    }

    public int getPageLimit() {
        return LIMIT_DEFAULT;
    }

    public static String getIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) {
                InetAddress inet = null;

                try {
                    inet = InetAddress.getLocalHost();
                } catch (UnknownHostException var4) {
                    logger.error("未知主机异常:", var4);
                }

                if (inet != null) {
                    ip = inet.getHostAddress();
                }
            }
        }

        if (StringUtils.isNotBlank(ip) && ip.contains(",")) {
            ip = ip.substring(0, ip.indexOf(44));
        }

        return ip;
    }

    protected <T> String objectToJson(T t) {
        try {
            String response = JSONObject.toJSONString(t);
            logger.info("接口响应的数据response=[" + response + "]");
            return response;
        } catch (Exception var3) {
            logger.info("接口返回数据转为json格式错误:", var3);
            return null;
        }
    }
}
