package com.zhuoan.biz.event.gdy;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import javax.annotation.Resource;
import javax.jms.Destination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GDYGameEvent {
    private static final Logger log = LoggerFactory.getLogger(GDYGameEvent.class);
    @Resource
    private Destination gdyQueueDestination;
    @Resource
    private ProducerService producerService;

    public GDYGameEvent() {
    }

    private void sendMessage(SocketIOClient client, Object data, int sorts) {
        this.producerService.sendMessage(this.gdyQueueDestination, new Messages(client, data, 18, sorts));
    }

    public void listenerGDYGameEvent(SocketIOServer server) {
        server.addEventListener("gdyTestasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GDYGameEvent.this.sendMessage(client, data, 0);
            }
        });
        server.addEventListener("gameReady_GDYasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GDYGameEvent.this.sendMessage(client, data, 1);
            }
        });
        server.addEventListener("gameDouble_GDYasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GDYGameEvent.this.sendMessage(client, data, 2);
            }
        });
        server.addEventListener("gamePrompt_GDYasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GDYGameEvent.this.sendMessage(client, data, 3);
            }
        });
        server.addEventListener("gameEvent_GDYasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GDYGameEvent.this.sendMessage(client, data, 4);
            }
        });
        server.addEventListener("gameTrustee_GDYasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GDYGameEvent.this.sendMessage(client, data, 5);
            }
        });
        server.addEventListener("reconnectGame_GDYasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GDYGameEvent.this.sendMessage(client, data, 6);
            }
        });
        server.addEventListener("gameContinue_GDYasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GDYGameEvent.this.sendMessage(client, data, 7);
            }
        });
        server.addEventListener("exitRoom_GDYasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GDYGameEvent.this.sendMessage(client, data, 8);
            }
        });
        server.addEventListener("closeRoom_GDYasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GDYGameEvent.this.sendMessage(client, data, 9);
            }
        });
        server.addEventListener("earlyStartGame_GDYasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GDYGameEvent.this.sendMessage(client, data, 10);
            }
        });
    }
}
