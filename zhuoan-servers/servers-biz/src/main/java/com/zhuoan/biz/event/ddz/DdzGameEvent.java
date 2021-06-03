package com.zhuoan.biz.event.ddz;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import javax.annotation.Resource;
import javax.jms.Destination;
import org.springframework.stereotype.Component;

@Component
public class DdzGameEvent {
    @Resource
    private Destination ddzQueueDestination;
    @Resource
    private ProducerService producerService;

    public DdzGameEvent() {
    }

    public void listenerDDZGameEvent(SocketIOServer server) {
        server.addEventListener("gameReady_DDZasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                DdzGameEvent.this.producerService.sendMessage(DdzGameEvent.this.ddzQueueDestination, new Messages(client, data, 2, 1));
            }
        });
        server.addEventListener("gameLandlord_DDZasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                DdzGameEvent.this.producerService.sendMessage(DdzGameEvent.this.ddzQueueDestination, new Messages(client, data, 2, 2));
            }
        });
        server.addEventListener("gameEvent_DDZasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                DdzGameEvent.this.producerService.sendMessage(DdzGameEvent.this.ddzQueueDestination, new Messages(client, data, 2, 3));
            }
        });
        server.addEventListener("reconnectGame_DDZasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                DdzGameEvent.this.producerService.sendMessage(DdzGameEvent.this.ddzQueueDestination, new Messages(client, data, 2, 4));
            }
        });
        server.addEventListener("gamePrompt_DDZasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                DdzGameEvent.this.producerService.sendMessage(DdzGameEvent.this.ddzQueueDestination, new Messages(client, data, 2, 5));
            }
        });
        server.addEventListener("gameContinue_DDZasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                DdzGameEvent.this.producerService.sendMessage(DdzGameEvent.this.ddzQueueDestination, new Messages(client, data, 2, 6));
            }
        });
        server.addEventListener("exitRoom_DDZasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                DdzGameEvent.this.producerService.sendMessage(DdzGameEvent.this.ddzQueueDestination, new Messages(client, data, 2, 7));
            }
        });
        server.addEventListener("closeRoom_DDZasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                DdzGameEvent.this.producerService.sendMessage(DdzGameEvent.this.ddzQueueDestination, new Messages(client, data, 2, 8));
            }
        });
        server.addEventListener("gameTrustee_DDZasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                DdzGameEvent.this.producerService.sendMessage(DdzGameEvent.this.ddzQueueDestination, new Messages(client, data, 2, 9));
            }
        });
        server.addEventListener("getOutInfo_DDZasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                DdzGameEvent.this.producerService.sendMessage(DdzGameEvent.this.ddzQueueDestination, new Messages(client, data, 2, 13));
            }
        });
        server.addEventListener("gameDouble_DDZasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                DdzGameEvent.this.producerService.sendMessage(DdzGameEvent.this.ddzQueueDestination, new Messages(client, data, 2, 14));
            }
        });
    }
}
