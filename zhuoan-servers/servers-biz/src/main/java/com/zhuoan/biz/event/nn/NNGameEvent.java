package com.zhuoan.biz.event.nn;

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
public class NNGameEvent {
    @Resource
    private Destination nnQueueDestination;
    @Resource
    private ProducerService producerService;

    public NNGameEvent() {
    }

    public void listenerNNGameEvent(SocketIOServer server) {
        server.addEventListener("gameReady_NNasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                NNGameEvent.this.producerService.sendMessage(NNGameEvent.this.nnQueueDestination, new Messages(client, data, 1, 1));
            }
        });
        server.addEventListener("gameXiaZhu_NNasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                NNGameEvent.this.producerService.sendMessage(NNGameEvent.this.nnQueueDestination, new Messages(client, data, 1, 3));
            }
        });
        server.addEventListener("gameEvent_NNasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                NNGameEvent.this.producerService.sendMessage(NNGameEvent.this.nnQueueDestination, new Messages(client, data, 1, 4));
            }
        });
        server.addEventListener("qiangZhuang_NNasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                NNGameEvent.this.producerService.sendMessage(NNGameEvent.this.nnQueueDestination, new Messages(client, data, 1, 2));
            }
        });
        server.addEventListener("exitRoom_NNasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                NNGameEvent.this.producerService.sendMessage(NNGameEvent.this.nnQueueDestination, new Messages(client, data, 1, 5));
            }
        });
        server.addEventListener("reconnectGame_NNasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                NNGameEvent.this.producerService.sendMessage(NNGameEvent.this.nnQueueDestination, new Messages(client, data, 1, 6));
            }
        });
        server.addEventListener("closeRoom_NNasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                NNGameEvent.this.producerService.sendMessage(NNGameEvent.this.nnQueueDestination, new Messages(client, data, 1, 7));
            }
        });
        server.addEventListener("gameBeBanker_NNasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                NNGameEvent.this.producerService.sendMessage(NNGameEvent.this.nnQueueDestination, new Messages(client, data, 1, 8));
            }
        });
        server.addEventListener("gameStart_NNasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                NNGameEvent.this.producerService.sendMessage(NNGameEvent.this.nnQueueDestination, new Messages(client, data, 1, 9));
            }
        });
    }
}
