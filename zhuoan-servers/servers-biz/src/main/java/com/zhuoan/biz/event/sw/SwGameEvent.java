
package com.zhuoan.biz.event.sw;

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
public class SwGameEvent {
    @Resource
    private Destination swQueueDestination;
    @Resource
    private ProducerService producerService;

    public SwGameEvent() {
    }

    public void listenerSwGameEvent(SocketIOServer server) {
        server.addEventListener("gameStart_SWasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SwGameEvent.this.producerService.sendMessage(SwGameEvent.this.swQueueDestination, new Messages(client, data, 14, 1));
            }
        });
        server.addEventListener("gameBet_SWasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SwGameEvent.this.producerService.sendMessage(SwGameEvent.this.swQueueDestination, new Messages(client, data, 14, 2));
            }
        });
        server.addEventListener("gameBeBanker_SWasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SwGameEvent.this.producerService.sendMessage(SwGameEvent.this.swQueueDestination, new Messages(client, data, 14, 3));
            }
        });
        server.addEventListener("gameUndo_SWasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SwGameEvent.this.producerService.sendMessage(SwGameEvent.this.swQueueDestination, new Messages(client, data, 14, 4));
            }
        });
        server.addEventListener("exitRoom_SWasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SwGameEvent.this.producerService.sendMessage(SwGameEvent.this.swQueueDestination, new Messages(client, data, 14, 5));
            }
        });
        server.addEventListener("gameChangeSeat_SWasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SwGameEvent.this.producerService.sendMessage(SwGameEvent.this.swQueueDestination, new Messages(client, data, 14, 6));
            }
        });
        server.addEventListener("reconnectGame_SWasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SwGameEvent.this.producerService.sendMessage(SwGameEvent.this.swQueueDestination, new Messages(client, data, 14, 7));
            }
        });
        server.addEventListener("getHistory_SWasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SwGameEvent.this.producerService.sendMessage(SwGameEvent.this.swQueueDestination, new Messages(client, data, 14, 8));
            }
        });
        server.addEventListener("getAllUsers_SWasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SwGameEvent.this.producerService.sendMessage(SwGameEvent.this.swQueueDestination, new Messages(client, data, 14, 9));
            }
        });
        server.addEventListener("gameHide_SWasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SwGameEvent.this.producerService.sendMessage(SwGameEvent.this.swQueueDestination, new Messages(client, data, 14, 10));
            }
        });
        server.addEventListener("getUndoInfo_SWasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                SwGameEvent.this.producerService.sendMessage(SwGameEvent.this.swQueueDestination, new Messages(client, data, 14, 11));
            }
        });
    }
}
