

package com.zhuoan.service.jms.impl;

import com.alibaba.fastjson.JSONObject;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.queue.Messages;
import com.zhuoan.queue.SqlModel;
import com.zhuoan.service.jms.ProducerService;
import java.io.Serializable;
import javax.annotation.Resource;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

@Service
public class ProducerServiceImpl implements ProducerService {
    private static final Logger logger = LoggerFactory.getLogger(ProducerServiceImpl.class);
    @Resource
    private JmsTemplate jmsTemplate;

    public ProducerServiceImpl() {
    }

    public void sendMessage(Destination destination, final String msg) {
        logger.info("向队列" + String.valueOf(destination) + "发送------------" + msg);
        this.jmsTemplate.send(destination, new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                return session.createTextMessage(msg);
            }
        });
    }

    public void sendMessage(final String msg) {
        Destination destination = this.jmsTemplate.getDefaultDestination();
        logger.info("向队列" + String.valueOf(destination) + "发送------------" + msg);
        this.jmsTemplate.send(new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                return session.createTextMessage(msg);
            }
        });
    }

    public void sendMessage(Destination destination, final Object msg) {
        if (msg instanceof PumpDao) {
            PumpDao pumpDao = (PumpDao)msg;
            logger.info("[" + String.valueOf(destination) + "]发送:[" + pumpDao + "]");
        } else if (msg instanceof SqlModel) {
            SqlModel sqlModel = (SqlModel)msg;
            logger.info("[" + String.valueOf(destination) + "]发送:[" + sqlModel + "]");
        } else if (msg instanceof Object) {
            logger.info("[" + String.valueOf(destination) + "]发送:[" + msg + "]");
        }

        this.jmsTemplate.send(destination, new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                return session.createObjectMessage((Serializable)msg);
            }
        });
    }

    public void sendMessage(Destination destination, Messages msg) {
        final String msgText = JSONObject.toJSONString(msg);
        this.jmsTemplate.send(destination, new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                return session.createTextMessage(msgText);
            }
        });
    }
}
