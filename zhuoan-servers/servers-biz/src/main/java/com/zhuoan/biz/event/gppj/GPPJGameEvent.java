package com.zhuoan.biz.event.gppj;

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
public class GPPJGameEvent {
    @Resource
    private Destination gppjQueueDestination;
    @Resource
    private ProducerService producerService;

    public GPPJGameEvent() {
    }

    public void listenerGPPJGameEvent(SocketIOServer server) {
        server.addEventListener("gameReady_GPPJasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GPPJGameEvent.this.producerService.sendMessage(GPPJGameEvent.this.gppjQueueDestination, new Messages(client, data, 5, 1));
            }
        });
        server.addEventListener("gameStart_GPPJasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GPPJGameEvent.this.producerService.sendMessage(GPPJGameEvent.this.gppjQueueDestination, new Messages(client, data, 5, 2));
            }
        });
        server.addEventListener("gameCut_GPPJasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GPPJGameEvent.this.producerService.sendMessage(GPPJGameEvent.this.gppjQueueDestination, new Messages(client, data, 5, 3));
            }
        });
        server.addEventListener("gameQz_GPPJasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GPPJGameEvent.this.producerService.sendMessage(GPPJGameEvent.this.gppjQueueDestination, new Messages(client, data, 5, 4));
            }
        });
        server.addEventListener("gameXz_GPPJasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GPPJGameEvent.this.producerService.sendMessage(GPPJGameEvent.this.gppjQueueDestination, new Messages(client, data, 5, 5));
            }
        });
        server.addEventListener("gameShow_GPPJasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GPPJGameEvent.this.producerService.sendMessage(GPPJGameEvent.this.gppjQueueDestination, new Messages(client, data, 5, 6));
            }
        });
        server.addEventListener("reconnectGame_GPPJasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GPPJGameEvent.this.producerService.sendMessage(GPPJGameEvent.this.gppjQueueDestination, new Messages(client, data, 5, 7));
            }
        });
        server.addEventListener("exitRoom_GPPJasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GPPJGameEvent.this.producerService.sendMessage(GPPJGameEvent.this.gppjQueueDestination, new Messages(client, data, 5, 8));
            }
        });
        server.addEventListener("closeRoom_GPPJasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GPPJGameEvent.this.producerService.sendMessage(GPPJGameEvent.this.gppjQueueDestination, new Messages(client, data, 5, 9));
            }
        });
        server.addEventListener("reShuffle_GPPJasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                GPPJGameEvent.this.producerService.sendMessage(GPPJGameEvent.this.gppjQueueDestination, new Messages(client, data, 5, 10));
            }
        });
    }
}
