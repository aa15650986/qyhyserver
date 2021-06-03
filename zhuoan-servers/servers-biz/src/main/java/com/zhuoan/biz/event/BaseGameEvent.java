

package com.zhuoan.biz.event;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import javax.annotation.Resource;
import javax.jms.Destination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BaseGameEvent {
    private static final Logger logger = LoggerFactory.getLogger(BaseGameEvent.class);
    @Resource
    private Destination baseQueueDestination;
    @Resource
    private ProducerService producerService;

    public BaseGameEvent() {
    }

    public void listenerBaseGameEvent(SocketIOServer server) {
        server.addConnectListener(new ConnectListener() {
            public void onConnect(SocketIOClient client) {
                BaseGameEvent.logger.info("用户 IP = [{}] with sessionId = [{}] 上线了！！！", BaseGameEvent.this.obtainClientIp(client), client.getSessionId());
            }
        });
        server.addDisconnectListener(new DisconnectListener() {
            public void onDisconnect(SocketIOClient client) {
                BaseGameEvent.logger.info("用户 IP = [{}] with sessionId = [{}] 离线了！！！", BaseGameEvent.this.obtainClientIp(client), client.getSessionId());
                String sessionId = client.getSessionId().toString();
                if (null != sessionId && !"".equals(sessionId)) {
                    String userId = (String)RoomManage.sessionIdMap.get(sessionId);
                    RoomManage.sessionIdMap.remove(sessionId);
                    if (null != userId && !"".equals(userId)) {
                        RoomManage.channelMap.remove(userId);
                    }
                }

            }
        });
        server.addEventListener("game_pingasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object obj, AckRequest request) {
                client.sendEvent("game_pong", new Object[]{obj});
            }
        });
        server.addEventListener("getUserInfoasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 1));
            }
        });
        server.addEventListener("checkUserasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 2));
            }
        });
        server.addEventListener("getGameSettingasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 3));
            }
        });
        server.addEventListener("getAllRoomListasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 4));
            }
        });
        server.addEventListener("createRoomasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.logger.info("事件名：createRoom");
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 5));
            }
        });
        server.addEventListener("joinRoomasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, data, 0, 6));
            }
        });
        server.addEventListener("xipaiMessaasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 7));
            }
        });
        server.addEventListener("xipaiFunasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 8));
            }
        });
        server.addEventListener("sendMsgEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 9));
            }
        });
        server.addEventListener("voiceCallGameasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 10));
            }
        });
        server.addEventListener("getGameLogsListasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 11));
            }
        });
        server.addEventListener("dissolveRoomasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 12));
            }
        });
        server.addEventListener("sendNoticeasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 14));
            }
        });
        server.addEventListener("getMessageasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 15));
            }
        });
        server.addEventListener("getRoomAndPlayerCountasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 16));
            }
        });
        server.addEventListener("getRoomGidasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 18));
            }
        });
        server.addEventListener("quickJoinasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 19));
            }
        });
        server.addEventListener("getRoomCardPayInfoasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 20));
            }
        });
        server.addEventListener("getGameGoldSettingasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 21));
            }
        });
        server.addEventListener("checkSignInasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 22));
            }
        });
        server.addEventListener("userSignInasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 23));
            }
        });
        server.addEventListener("getArenaasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 24));
            }
        });
        server.addEventListener("arenaJoinasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 25));
            }
        });
        server.addEventListener("gameCheckIpasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 26));
            }
        });
        server.addEventListener("getProxyRoomListasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 27));
            }
        });
        server.addEventListener("dissolveProxyRoomasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 28));
            }
        });
        server.addEventListener("getUserAchievementInfoasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 29));
            }
        });
        server.addEventListener("getPropsInfoasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 30));
            }
        });
        server.addEventListener("userPurchaseasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 31));
            }
        });
        server.addEventListener("getAchievementRankasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 32));
            }
        });
        server.addEventListener("getDrawInfoasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 33));
            }
        });
        server.addEventListener("gameDrawasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 34));
            }
        });
        server.addEventListener("getAchievementDetailasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 35));
            }
        });
        server.addEventListener("drawAchievementRewardasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, object, 0, 36));
            }
        });
        server.addEventListener("changeRoomasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, data, 0, 37));
            }
        });
        server.addEventListener("getRoomCardGameLogListasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, data, 0, 38));
            }
        });
        server.addEventListener("getRoomCardGameLogDetailasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, data, 0, 39));
            }
        });
        server.addEventListener("getClubGameLogListasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, data, 0, 40));
            }
        });
        server.addEventListener("getBackpackInfoasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, data, 0, 41));
            }
        });
        server.addEventListener("checkBindStatusasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, data, 0, 42));
            }
        });
        server.addEventListener("userBindasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, data, 0, 43));
            }
        });
        server.addEventListener("refreshLocationasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, data, 0, 44));
            }
        });
        server.addEventListener("getUserLocationasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, data, 0, 45));
            }
        });
        server.addEventListener("getSignMessageEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, data, 0, 46));
            }
        });
        server.addEventListener("UserSignEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, data, 0, 47));
            }
        });
        server.addEventListener("changeBgasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, data, 0, 48));
            }
        });
        server.addEventListener("updateUserMsgasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                BaseGameEvent.this.producerService.sendMessage(BaseGameEvent.this.baseQueueDestination, new Messages(client, data, 0, 49));
            }
        });
        
       
    }

    private String obtainClientIp(SocketIOClient client) {
        String sa = String.valueOf(client.getRemoteAddress());
        return sa.substring(1, sa.indexOf(":"));
    }
}
