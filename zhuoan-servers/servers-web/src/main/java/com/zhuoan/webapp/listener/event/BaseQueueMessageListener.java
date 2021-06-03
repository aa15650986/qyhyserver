
package com.zhuoan.webapp.listener.event;

import com.zhuoan.queue.GameEventDeal;
import javax.annotation.Resource;
import javax.jms.Message;
import javax.jms.MessageListener;
import org.springframework.stereotype.Component;

@Component("baseQueueMessageListener")
public class BaseQueueMessageListener implements MessageListener {
    @Resource
    private GameEventDeal gameEventDeal;

    public BaseQueueMessageListener() {
    }

    public void onMessage(Message message) {
        this.gameEventDeal.eventsMQ(message);
    }
}
