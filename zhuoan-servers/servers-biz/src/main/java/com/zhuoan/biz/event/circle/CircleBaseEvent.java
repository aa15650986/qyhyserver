package com.zhuoan.biz.event.circle;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.zhuoan.biz.event.BaseGameEvent;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jms.Destination;

@Service
public class CircleBaseEvent {
    private static final Logger log = LoggerFactory.getLogger(CircleBaseEvent.class);
    @Resource
    private Destination circleBaseQueueDestination;
    @Resource
    private ProducerService producerService;

    public CircleBaseEvent() {
    }

    private void sendMessage(SocketIOClient client, Object data, int sorts) {
        this.producerService.sendMessage(this.circleBaseQueueDestination, new Messages(client, data, 200, sorts));
    }

    public void listenerCircleBaseEvent(SocketIOServer server) {
        server.addEventListener("circleTestasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 0);
            }
        });
        server.addEventListener("createModifyCircleEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 1);
            }
        });
        server.addEventListener("dismissCircleEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 3);
            }
        });
        server.addEventListener("mgrSettingCircleEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 4);
            }
        });
        server.addEventListener("transferCircleEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 5);
            }
        });
        server.addEventListener("circleMbrAddExitExamEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 6);
            }
        });
        server.addEventListener("circleBlacklistEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 7);
            }
        });
        server.addEventListener("circleFundSavePayEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 8);
            }
        });
        server.addEventListener("circleFundBillDetailEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 9);
            }
        });
        server.addEventListener("circleMessageInfoEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 10);
            }
        });
        server.addEventListener("circleMessageBlanceReminderEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 11);
            }
        });
        server.addEventListener("circlePartnerListEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 12);
            }
        });
        server.addEventListener("circlePartnerAddEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 13);
            }
        });
        server.addEventListener("circleMemberListEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 14);
            }
        });
        server.addEventListener("circleModifyStrenthEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 15);
            }
        });
        server.addEventListener("circleStrenthLogEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 16);
            }
        });
        server.addEventListener("circlePartnerShareEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 17);
            }
        });
        server.addEventListener("circleGeneralJoinExitEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 18);
            }
        });
        server.addEventListener("circleListEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 19);
            }
        });
        server.addEventListener("circleMbrExamListEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 20);
            }
        });
        server.addEventListener("circleRecordEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 21);
            }
        });
        server.addEventListener("circleExamMsgInfoEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 22);
            }
        });
        server.addEventListener("addCircleMemberEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 23);
            }
        });
        server.addEventListener("historyHpChangeByOperateEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 24);
            }
        });
        server.addEventListener("memberDetailInfoEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 25);
            }
        });
        server.addEventListener("circleRoomListEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 26);
            }
        });
        server.addEventListener("circleDelMemberEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 27);
            }
        });
        server.addEventListener("circleSysGlobalEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 28);
            }
        });
        server.addEventListener("circleGetProfitBalanceToHpEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 29);
            }
        });
        server.addEventListener("getGameCircleInfoEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 30);
            }
        });
        server.addEventListener("circleRecordInfoEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 31);
            }
        });
        server.addEventListener("quickJoinRoomCircleEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 32);
            }
        });
        server.addEventListener("removeRoomCircleEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 33);
            }
        });
        server.addEventListener("circleMemberInfoListEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 34);
            }
        });
        server.addEventListener("offlineCircleEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 35);
            }
        });
        server.addEventListener("quickEntryRoomCircleEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 36);
            }
        });
        server.addEventListener("resetPartnerEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 37);
            }
        });
        server.addEventListener("readMessageEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 38);
            }
        });
        server.addEventListener("circleVisitJoinRoomEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 39);
            }
        });
        server.addEventListener("circleVisitJoinGameEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 40);
            }
        });
        server.addEventListener("circleVisitListEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 41);
            }
        });
        server.addEventListener("subscribeCircleListEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 100);
            }
        });
        server.addEventListener("unSubscribeCircleListEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 101);
            }
        });
        server.addEventListener("goBankEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 201);
            }
        });
        
        server.addEventListener("operationMoneyEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                CircleBaseEvent.this.sendMessage(client, data, 202);
            }
        });
        server.addEventListener("luckyDrawEventasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
            	   CircleBaseEvent.this.sendMessage(client, data, 203);
            }
        });
        
    }
}
