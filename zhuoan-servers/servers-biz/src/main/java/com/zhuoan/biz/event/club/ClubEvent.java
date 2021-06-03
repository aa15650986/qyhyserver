package com.zhuoan.biz.event.club;

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
public class ClubEvent {
    @Resource
    private Destination clubQueueDestination;
    @Resource
    private ProducerService producerService;

    public ClubEvent() {
    }

    public void listenerClubEvent(SocketIOServer server) {
        server.addEventListener("joinClubasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                ClubEvent.this.producerService.sendMessage(ClubEvent.this.clubQueueDestination, new Messages(client, data, 101, 1));
            }
        });
        server.addEventListener("getMyClubListasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                ClubEvent.this.producerService.sendMessage(ClubEvent.this.clubQueueDestination, new Messages(client, data, 101, 2));
            }
        });
        server.addEventListener("getClubMembersasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                ClubEvent.this.producerService.sendMessage(ClubEvent.this.clubQueueDestination, new Messages(client, data, 101, 3));
            }
        });
        server.addEventListener("getClubSettingasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                ClubEvent.this.producerService.sendMessage(ClubEvent.this.clubQueueDestination, new Messages(client, data, 101, 4));
            }
        });
        server.addEventListener("changeClubSettingasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                ClubEvent.this.producerService.sendMessage(ClubEvent.this.clubQueueDestination, new Messages(client, data, 101, 5));
            }
        });
        server.addEventListener("exitClubasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                ClubEvent.this.producerService.sendMessage(ClubEvent.this.clubQueueDestination, new Messages(client, data, 101, 6));
            }
        });
        server.addEventListener("toTopasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                ClubEvent.this.producerService.sendMessage(ClubEvent.this.clubQueueDestination, new Messages(client, data, 101, 7));
            }
        });
        server.addEventListener("refreshClubInfoasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                ClubEvent.this.producerService.sendMessage(ClubEvent.this.clubQueueDestination, new Messages(client, data, 101, 8));
            }
        });
        server.addEventListener("quickJoinClubRoomasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                ClubEvent.this.producerService.sendMessage(ClubEvent.this.clubQueueDestination, new Messages(client, data, 101, 9));
            }
        });
        server.addEventListener("getClubApplyListasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                ClubEvent.this.producerService.sendMessage(ClubEvent.this.clubQueueDestination, new Messages(client, data, 101, 10));
            }
        });
        server.addEventListener("clubApplyReviewasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                ClubEvent.this.producerService.sendMessage(ClubEvent.this.clubQueueDestination, new Messages(client, data, 101, 11));
            }
        });
        server.addEventListener("clubLeaderInviteasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                ClubEvent.this.producerService.sendMessage(ClubEvent.this.clubQueueDestination, new Messages(client, data, 101, 12));
            }
        });
        server.addEventListener("clubLeaderOutasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                ClubEvent.this.producerService.sendMessage(ClubEvent.this.clubQueueDestination, new Messages(client, data, 101, 13));
            }
        });
        server.addEventListener("createClubasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                ClubEvent.this.producerService.sendMessage(ClubEvent.this.clubQueueDestination, new Messages(client, data, 101, 14));
            }
        });
        server.addEventListener("clubRechargeasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                ClubEvent.this.producerService.sendMessage(ClubEvent.this.clubQueueDestination, new Messages(client, data, 101, 15));
            }
        });
    }
}
