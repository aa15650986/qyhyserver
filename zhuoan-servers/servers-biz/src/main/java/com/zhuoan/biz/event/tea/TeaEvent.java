

package com.zhuoan.biz.event.tea;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.zhuoan.constant.event.enmu.TeaEventEnum;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import javax.annotation.Resource;
import javax.jms.Destination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TeaEvent {
    private static final Logger log = LoggerFactory.getLogger(TeaEvent.class);
    @Resource
    private Destination teaQueueDestination;
    @Resource
    private ProducerService producerService;

    public TeaEvent() {
    }

    private void sendMessage(SocketIOClient client, Object data, int sorts) {
        this.producerService.sendMessage(this.teaQueueDestination, new Messages(client, data, 210, sorts));
    }

    public void listenerTeaGameEvent(SocketIOServer server) {
        TeaEventEnum[] var2 = TeaEventEnum.values();
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            final TeaEventEnum m = var2[var4];
            server.addEventListener(m.getEvent(), Object.class, new DataListener<Object>() {
                public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                    TeaEvent.this.sendMessage(client, data, m.getType());
                }
            });
        }

    }
}
