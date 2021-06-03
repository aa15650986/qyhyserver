

package com.zhuoan.biz.model;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class RoomManage {
    public static ConcurrentHashMap<String, GameRoom> gameRoomMap = new ConcurrentHashMap();
    public static ConcurrentHashMap<String, String> gameConfigMap = new ConcurrentHashMap();
    public static ConcurrentHashMap<String, String> channelMap = new ConcurrentHashMap();
    public static ConcurrentHashMap<String, String> sessionIdMap = new ConcurrentHashMap();

    public RoomManage() {
    }
}
