
package com.zhuoan.biz.event.sss;

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
import org.springframework.stereotype.Service;

@Service
public class SSSGameEvent {
    private static final Logger logger = LoggerFactory.getLogger(SSSGameEvent.class);
    @Resource
    private Destination sssQueueDestination;
    @Resource
    private ProducerService producerService;

    public SSSGameEvent() {
    }

    public void listenerSSSGameEvent(SocketIOServer server) {
        server.addEventListener("gameReady_SSSasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SSSGameEvent.this.producerService.sendMessage(SSSGameEvent.this.sssQueueDestination, new Messages(client, data, 4, 1));
            }
        });
        server.addEventListener("gameAction_SSSasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SSSGameEvent.this.producerService.sendMessage(SSSGameEvent.this.sssQueueDestination, new Messages(client, data, 4, 2));
            }
        });
        server.addEventListener("exitRoom_SSSasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SSSGameEvent.this.producerService.sendMessage(SSSGameEvent.this.sssQueueDestination, new Messages(client, data, 4, 3));
            }
        });
        server.addEventListener("reconnectGame_SSSasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SSSGameEvent.this.producerService.sendMessage(SSSGameEvent.this.sssQueueDestination, new Messages(client, data, 4, 4));
            }
        });
        server.addEventListener("closeRoom_SSSasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SSSGameEvent.this.producerService.sendMessage(SSSGameEvent.this.sssQueueDestination, new Messages(client, data, 4, 5));
            }
        });
        server.addEventListener("gameBeBanker_SSSasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SSSGameEvent.this.producerService.sendMessage(SSSGameEvent.this.sssQueueDestination, new Messages(client, data, 4, 6));
            }
        });
        server.addEventListener("gameXiaZhu_SSSasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SSSGameEvent.this.producerService.sendMessage(SSSGameEvent.this.sssQueueDestination, new Messages(client, data, 4, 7));
            }
        });
        server.addEventListener("gameStart_SSSasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SSSGameEvent.this.producerService.sendMessage(SSSGameEvent.this.sssQueueDestination, new Messages(client, data, 4, 8));
            }
        });
        server.addEventListener("checkMypai_SSSasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SSSGameEvent.this.producerService.sendMessage(SSSGameEvent.this.sssQueueDestination, new Messages(client, data, 4, 10));
            }
        });
        server.addEventListener("sss_handleReviewEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SSSGameEvent.this.producerService.sendMessage(SSSGameEvent.this.sssQueueDestination, new Messages(client, data, 4, 11));
            }
        });
        server.addEventListener("sss_relistEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SSSGameEvent.this.producerService.sendMessage(SSSGameEvent.this.sssQueueDestination, new Messages(client, data, 4, 12));
            }
        });
        server.addEventListener("sss_cutCardEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SSSGameEvent.this.producerService.sendMessage(SSSGameEvent.this.sssQueueDestination, new Messages(client, data, 4, 13));
            }
        });
        server.addEventListener("interactive_SSSasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SSSGameEvent.this.producerService.sendMessage(SSSGameEvent.this.sssQueueDestination, new Messages(client, data, 4, 15));
            }
        });
    }
}
