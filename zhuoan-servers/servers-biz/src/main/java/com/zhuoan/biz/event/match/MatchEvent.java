

package com.zhuoan.biz.event.match;

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
import org.springframework.stereotype.Component;

@Component
public class MatchEvent {
    private static final Logger log = LoggerFactory.getLogger(MatchEvent.class);
    @Resource
    private Destination matchQueueDestination;
    @Resource
    private ProducerService producerService;

    public MatchEvent() {
    }

    public void listenerMatchGameEvent(SocketIOServer server) {
        server.addEventListener("getMatchInfoasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                MatchEvent.log.info("事件名：getMatchInfo");
                MatchEvent.this.producerService.sendMessage(MatchEvent.this.matchQueueDestination, new Messages(client, data, 100, 1));
            }
        });
        server.addEventListener("matchSignUpasd1", Object.class,new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                MatchEvent.log.info("事件名：matchSignUp");
                MatchEvent.this.producerService.sendMessage(MatchEvent.this.matchQueueDestination, new Messages(client, data, 100, 2));
            }
        });
        server.addEventListener("updateMatchCountasd1",  Object.class,new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                MatchEvent.log.info("事件名：updateMatchCount");
                MatchEvent.this.producerService.sendMessage(MatchEvent.this.matchQueueDestination, new Messages(client, data, 100, 3));
            }
        });
        server.addEventListener("matchCancelSignasd1",  Object.class,new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                MatchEvent.log.info("事件名：matchCancelSign");
                MatchEvent.this.producerService.sendMessage(MatchEvent.this.matchQueueDestination, new Messages(client, data, 100, 4));
            }
        });
        server.addEventListener("getWinningRecordasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                MatchEvent.log.info("事件名：getWinningRecord");
                MatchEvent.this.producerService.sendMessage(MatchEvent.this.matchQueueDestination, new Messages(client, data, 100, 5));
            }
        });
        server.addEventListener("getSignUpInfoasd1", Object.class,new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                MatchEvent.log.info("事件名：getSignUpInfo");
                MatchEvent.this.producerService.sendMessage(MatchEvent.this.matchQueueDestination, new Messages(client, data, 100, 6));
            }
        });
        server.addEventListener("checkMatchStatusasd1", Object.class, new DataListener<Object>() {
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                MatchEvent.log.info("事件名：checkMatchStatus");
                MatchEvent.this.producerService.sendMessage(MatchEvent.this.matchQueueDestination, new Messages(client, data, 100, 7));
            }
        });
    }
}
