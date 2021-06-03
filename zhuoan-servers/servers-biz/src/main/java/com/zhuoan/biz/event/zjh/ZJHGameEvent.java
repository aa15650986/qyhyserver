
package com.zhuoan.biz.event.zjh;

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
public class ZJHGameEvent {
    @Resource
    private Destination zjhQueueDestination;
    @Resource
    private ProducerService producerService;

    public ZJHGameEvent() {
    }

    public void listenerZJHGameEvent(SocketIOServer server) {
        server.addEventListener("gameReady_ZJHasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                ZJHGameEvent.this.producerService.sendMessage(ZJHGameEvent.this.zjhQueueDestination, new Messages(client, data, 6, 1));
            }
        });
        server.addEventListener("gameEvent_ZJHasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                ZJHGameEvent.this.producerService.sendMessage(ZJHGameEvent.this.zjhQueueDestination, new Messages(client, data, 6, 2));
            }
        });
        server.addEventListener("exitRoom_ZJHasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                ZJHGameEvent.this.producerService.sendMessage(ZJHGameEvent.this.zjhQueueDestination, new Messages(client, data, 6, 3));
            }
        });
        server.addEventListener("reconnectGame_ZJHasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                ZJHGameEvent.this.producerService.sendMessage(ZJHGameEvent.this.zjhQueueDestination, new Messages(client, data, 6, 4));
            }
        });
        server.addEventListener("closeRoom_ZJHasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                ZJHGameEvent.this.producerService.sendMessage(ZJHGameEvent.this.zjhQueueDestination, new Messages(client, data, 6, 5));
            }
        });
    }
}
