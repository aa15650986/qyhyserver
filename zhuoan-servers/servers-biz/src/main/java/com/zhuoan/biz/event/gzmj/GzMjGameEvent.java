package com.zhuoan.biz.event.gzmj;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import javax.annotation.Resource;
import javax.jms.Destination;
import org.springframework.stereotype.Service;

@Service
public class GzMjGameEvent {
    @Resource
    private Destination gzmjQueueDestination;
    @Resource
    private ProducerService producerService;

    public GzMjGameEvent() {
    }

    public void listenerGZMJGameEvent(SocketIOServer server) {
        server.addEventListener("sitDown_GZMJasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GzMjGameEvent.this.producerService.sendMessage(GzMjGameEvent.this.gzmjQueueDestination, new Messages(client, data, 20, 1));
            }
        });
        server.addEventListener("gameReady_GZMJasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GzMjGameEvent.this.producerService.sendMessage(GzMjGameEvent.this.gzmjQueueDestination, new Messages(client, data, 20, 2));
            }
        });
        server.addEventListener("gameBet_GZMJasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GzMjGameEvent.this.producerService.sendMessage(GzMjGameEvent.this.gzmjQueueDestination, new Messages(client, data, 20, 3));
            }
        });
        server.addEventListener("gameChooseLack_GZMJasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GzMjGameEvent.this.producerService.sendMessage(GzMjGameEvent.this.gzmjQueueDestination, new Messages(client, data, 20, 4));
            }
        });
        server.addEventListener("gameReconnect_GZMJasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GzMjGameEvent.this.producerService.sendMessage(GzMjGameEvent.this.gzmjQueueDestination, new Messages(client, data, 20, 5));
            }
        });
        server.addEventListener("gameOutCard_GZMJasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GzMjGameEvent.this.producerService.sendMessage(GzMjGameEvent.this.gzmjQueueDestination, new Messages(client, data, 20, 6));
            }
        });
        server.addEventListener("gameEvent_GZMJasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GzMjGameEvent.this.producerService.sendMessage(GzMjGameEvent.this.gzmjQueueDestination, new Messages(client, data, 20, 7));
            }
        });
        server.addEventListener("closeRoom_GZMJasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GzMjGameEvent.this.producerService.sendMessage(GzMjGameEvent.this.gzmjQueueDestination, new Messages(client, data, 20, 8));
            }
        });
        server.addEventListener("exitRoom_GZMJasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GzMjGameEvent.this.producerService.sendMessage(GzMjGameEvent.this.gzmjQueueDestination, new Messages(client, data, 20, 9));
            }
        });
    }
}
