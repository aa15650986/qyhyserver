
package com.zhuoan.biz.event.qzmj;

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
public class QZMJGameEvent {
    @Resource
    private Destination qzmjQueueDestination;
    @Resource
    private ProducerService producerService;

    public QZMJGameEvent() {
    }

    public void listenerQZMJGameEvent(SocketIOServer server) {
        server.addEventListener("gameReadyasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                QZMJGameEvent.this.producerService.sendMessage(QZMJGameEvent.this.qzmjQueueDestination, new Messages(client, data, 3, 1));
            }
        });
        
        server.addEventListener("qzmj_gameReadyEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                QZMJGameEvent.this.producerService.sendMessage(QZMJGameEvent.this.qzmjQueueDestination, new Messages(client, data, 3, 16));
            }
        });
        server.addEventListener("gameChupaiasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                QZMJGameEvent.this.producerService.sendMessage(QZMJGameEvent.this.qzmjQueueDestination, new Messages(client, data, 3, 2));
            }
        });
        server.addEventListener("gameEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                QZMJGameEvent.this.producerService.sendMessage(QZMJGameEvent.this.qzmjQueueDestination, new Messages(client, data, 3, 3));
            }
        });
        server.addEventListener("gangChupaiEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                QZMJGameEvent.this.producerService.sendMessage(QZMJGameEvent.this.qzmjQueueDestination, new Messages(client, data, 3, 4));
            }
        });
        server.addEventListener("closeRoomasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                QZMJGameEvent.this.producerService.sendMessage(QZMJGameEvent.this.qzmjQueueDestination, new Messages(client, data, 3, 5));
            }
        });
        server.addEventListener("exitRoomasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                QZMJGameEvent.this.producerService.sendMessage(QZMJGameEvent.this.qzmjQueueDestination, new Messages(client, data, 3, 6));
            }
        });
        server.addEventListener("reconnectGameasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                QZMJGameEvent.this.producerService.sendMessage(QZMJGameEvent.this.qzmjQueueDestination, new Messages(client, data, 3, 7));
            }
        });
        server.addEventListener("gameTrusteeasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                QZMJGameEvent.this.producerService.sendMessage(QZMJGameEvent.this.qzmjQueueDestination, new Messages(client, data, 3, 14));
            }
        });
        server.addEventListener("asfrsgsdzasd1",  Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                QZMJGameEvent.this.producerService.sendMessage(QZMJGameEvent.this.qzmjQueueDestination, new Messages(client, data, 3, 12));
            }
        });
    }
}
