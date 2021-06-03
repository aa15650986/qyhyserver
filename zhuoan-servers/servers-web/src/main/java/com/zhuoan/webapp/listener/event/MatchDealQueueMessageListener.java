
package com.zhuoan.webapp.listener.event;

import com.zhuoan.biz.event.ddz.GameTimerDdz;
import com.zhuoan.biz.event.match.MatchEventDeal;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.exception.EventException;
import java.util.UUID;
import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.activemq.command.ActiveMQObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("matchDealQueueMessageListener")
public class MatchDealQueueMessageListener implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(MatchDealQueueMessageListener.class);
    @Resource
    private MatchEventDeal matchEventDeal;
    @Resource
    private GameTimerDdz gameTimerDdz;

    public MatchDealQueueMessageListener() {
    }

    public void onMessage(Message message) {
        JSONObject object = JSONObject.fromObject(this.obtainMessageStr(message));
        switch(object.getInt("deal_type")) {
            case 1:
                CommonConstant.sendMsgEventToSingle(UUID.fromString(object.getString("uuid")), String.valueOf(object.getString("result")), object.getString("eventName"));
                break;
            case 2:
                String matchNum = object.getString("matchNum");
                JSONObject matchInfo = object.getJSONObject("matchInfo");
                int perCount = object.getInt("perCount");
                JSONArray robotList = object.getJSONArray("robotList");
                JSONArray singleMate = object.getJSONArray("singleMate");
                JSONObject rankObj = object.getJSONObject("rankObj");
                this.matchEventDeal.singleJoin(matchNum, matchInfo, perCount, robotList, singleMate, rankObj);
                break;
            case 3:
                this.gameTimerDdz.doOverTimeDeal(object.getString("roomNo"), object.getJSONObject("roomInfo"));
        }

    }

    private Object obtainMessageStr(Message message) {
        if (message != null) {
            try {
                return ((ActiveMQObjectMessage)message).getObject();
            } catch (JMSException var3) {
                throw new EventException("[" + this.getClass().getName() + "] 信息接收出现异常");
            }
        } else {
            return null;
        }
    }
}
