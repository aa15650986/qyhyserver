package com.zhuoan.biz.event.bdx;

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
public class BDXGameEvent {
    @Resource
    private Destination bdxQueueDestination;
    @Resource
    private ProducerService producerService;

    public BDXGameEvent() {
    }

    public void listenerBDXGameEvent(SocketIOServer server) {
        server.addEventListener("gameXiazhu_BDXasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                BDXGameEvent.this.producerService.sendMessage(BDXGameEvent.this.bdxQueueDestination, new Messages(client, data, 10, 1));
            }
        });
        server.addEventListener("gameEvent_BDXasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                BDXGameEvent.this.producerService.sendMessage(BDXGameEvent.this.bdxQueueDestination, new Messages(client, data, 10, 2));
            }
        });
        server.addEventListener("exitRoom_BDXasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                BDXGameEvent.this.producerService.sendMessage(BDXGameEvent.this.bdxQueueDestination, new Messages(client, data, 10, 3));
            }
        });
        server.addEventListener("reconnectGame_BDXasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                BDXGameEvent.this.producerService.sendMessage(BDXGameEvent.this.bdxQueueDestination, new Messages(client, data, 10, 4));
            }
        });
    }
}
