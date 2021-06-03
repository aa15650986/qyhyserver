package com.zhuoan.biz.event.sg;

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
public class SGGameEvent {
    private static final Logger log = LoggerFactory.getLogger(SGGameEvent.class);
    @Resource
    private Destination sgQueueDestination;
    @Resource
    private ProducerService producerService;

    public SGGameEvent() {
    }

    private void sendMessage(SocketIOClient client, Object data, int sorts) {
        this.producerService.sendMessage(this.sgQueueDestination, new Messages(client, data, 19, sorts));
    }

    public void listenerSGGameEvent(SocketIOServer server) {
        server.addEventListener("test_SGasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SGGameEvent.this.sendMessage(client, data, 0);
            }
        });
        server.addEventListener("ready_SGasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SGGameEvent.this.sendMessage(client, data, 1);
            }
        });
        server.addEventListener("robZhuang_SGasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SGGameEvent.this.sendMessage(client, data, 2);
            }
        });
        server.addEventListener("bet_SGasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SGGameEvent.this.sendMessage(client, data, 3);
            }
        });
        server.addEventListener("showCard_SGasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SGGameEvent.this.sendMessage(client, data, 4);
            }
        });
        server.addEventListener("exitRoom_SGasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SGGameEvent.this.sendMessage(client, data, 5);
            }
        });
        server.addEventListener("closeRoom_SGasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SGGameEvent.this.sendMessage(client, data, 6);
            }
        });
        server.addEventListener("earlyStart_SGasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SGGameEvent.this.sendMessage(client, data, 7);
            }
        });
        server.addEventListener("reconnect_SGasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SGGameEvent.this.sendMessage(client, data, 8);
            }
        });
    }
}
