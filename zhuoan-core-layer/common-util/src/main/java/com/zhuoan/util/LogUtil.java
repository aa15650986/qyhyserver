package com.zhuoan.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtil {
    private static final Logger logger = LoggerFactory.getLogger(LogUtil.class);

    public LogUtil() {
    }

    public static void print(String msg) {
        logger.info(msg);
    }




}
