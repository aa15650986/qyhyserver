
package com.zhuoan.biz.event;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.util.Dto;
import com.zhuoan.util.serializing.SerializingUtil;
import javax.annotation.Resource;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SuperEventDeal {
    private static final Logger logger = LoggerFactory.getLogger(SuperEventDeal.class);
    protected static final String ROOM_KEY = "all_room_obj";
    @Resource
    private RedisService redisService;

    public SuperEventDeal() {
    }

    protected void roomSerializable(GameRoom room) {
        try {
            if (room != null) {
                String roomNo = room.getRoomNo();
                String serStr = SerializingUtil.objectSerializable(room);
                this.redisService.hset("all_room_obj", roomNo, serStr);
            }
        } catch (Exception var4) {
            logger.error("", var4);
        }

    }

    protected GameRoom roomDeserializable(String roomNo) {
        try {
            Object serObj = this.redisService.hget("all_room_obj", roomNo);
            if (serObj != null) {
                return (GameRoom)SerializingUtil.objectDeserialization(String.valueOf(serObj));
            }
        } catch (Exception var3) {
            logger.error("", var3);
        }

        return null;
    }

    protected void removeRoom(String roomNo) {
        this.redisService.hdel("all_room_obj", new Object[]{roomNo});
    }

    protected boolean checkGameEvent(JSONObject postData, int gameStatus, SocketIOClient client) {
        if (postData.containsKey("room_no") && postData.containsKey("account")) {
            String roomNo = postData.getString("room_no");
            String account = postData.getString("account");
            if (Dto.stringIsNULL(roomNo)) {
                return false;
            } else {
                GameRoom gameRoom = this.roomDeserializable(roomNo);
                if (gameRoom == null) {
                    return false;
                } else if (!Dto.stringIsNULL(account) && gameRoom.getPlayerMap().containsKey(account) && gameRoom.getPlayerMap().get(account) != null) {
                    if (client != null && !client.getSessionId().equals(((Playerinfo)gameRoom.getPlayerMap().get(account)).getUuid())) {
                        return false;
                    } else {
                        return gameStatus == -1 || gameRoom.getGameStatus() == gameStatus;
                    }
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public void sendPromptToSinglePlayer(SocketIOClient client, int code, String msg, String eventName) {
        JSONObject result = new JSONObject();
        result.put("code", code);
        result.put("msg", msg);
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), eventName);
    }
}
