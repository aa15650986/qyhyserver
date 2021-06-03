
package com.zhuoan.constant;

import com.zhuoan.biz.model.GameRoom;
import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constant implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Constant.class);
    private static final long serialVersionUID = 6536626351642832853L;
    public static final int ONLINE_STATUS_YES = 1;
    public static final int ONLINE_STATUS_NO = 0;
    public static Properties cfgProperties = new Properties();
    public static String DOMAIN = "/zagame";
    public static Map<String, GameRoom> gameRoomMap = new ConcurrentHashMap();

    public Constant() {
    }
}
