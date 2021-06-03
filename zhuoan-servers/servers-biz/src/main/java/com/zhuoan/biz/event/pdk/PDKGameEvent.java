package com.zhuoan.biz.event.pdk;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.zhuoan.constant.event.enmu.PDKEventEnum;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import javax.annotation.Resource;
import javax.jms.Destination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PDKGameEvent {
    private static final Logger log = LoggerFactory.getLogger(PDKGameEvent.class);
    @Resource
    private Destination pdkQueueDestination;
    @Resource
    private ProducerService producerService;

    public PDKGameEvent() {
    }

    private void sendMessage(SocketIOClient client, Object data, int sorts) {
        this.producerService.sendMessage(this.pdkQueueDestination, new Messages(client, data, 21, sorts));
    }

    public void listenerPDKGameEvent(SocketIOServer server) {
        PDKEventEnum[] var2 = PDKEventEnum.values();
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            final PDKEventEnum m = var2[var4];
            server.addEventListener(m.getEvent(), Object.class, new DataListener<Object>() {
                public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                    PDKGameEvent.this.sendMessage(client, data, m.getType());
                }
            });
        }

    }
}
