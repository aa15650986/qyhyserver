
package com.zhuoan.times;

import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Resource;
import javax.jms.Destination;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SingleTimer extends Thread {
    @Resource
    private Destination nnQueueDestination;
    @Resource
    private ProducerService producerService;
    private static final Logger logger = LoggerFactory.getLogger(SingleTimer.class);
    private Lock m_locker = new ReentrantLock();
    private Map<String, Messages> m_map = new HashMap();

    public SingleTimer() {
    }

    public Map<String, Messages> getM_map() {
        return this.m_map;
    }

    public void setM_map(Map<String, Messages> m_map) {
        this.m_map = m_map;
    }

    public boolean hasKey(String roomNo) {
        boolean flag = false;
        this.m_locker.lock();
        if (this.m_map.containsKey(roomNo)) {
            flag = true;
        }

        this.m_locker.unlock();
        return flag;
    }

    public void run() {
        while(true) {
            while(true) {
                try {
                    Thread.sleep(1000L);
                    this.m_locker.lock();
                    Iterator it = this.m_map.entrySet().iterator();

                    while(it.hasNext()) {
                        Entry<String, Messages> entry = (Entry)it.next();
                        Object data = ((Messages)entry.getValue()).getDataObject();
                        JSONObject obj = JSONObject.fromObject(data);
                        String roomNo = obj.getString("room_no");
                        if (RoomManage.gameRoomMap.get(roomNo) != null) {
                            GameRoom gameRoom = (GameRoom)RoomManage.gameRoomMap.get(roomNo);
                            gameRoom.setTimeLeft(gameRoom.getTimeLeft() - 1);
                            if (gameRoom.getTimeLeft() == 0) {
                                this.producerService.sendMessage(this.nnQueueDestination, (Messages)entry.getValue());
                                it.remove();
                            }
                        } else {
                            it.remove();
                        }
                    }
                } catch (Exception var10) {
                    logger.error("", var10);
                } finally {
                    this.m_locker.unlock();
                }
            }
        }
    }

    public void createTimer(String roomNo, Messages messages) {
        try {
            this.m_locker.lock();
            this.m_map.put(roomNo, messages);
        } finally {
            this.m_locker.unlock();
        }

    }

    public void deleteTimer(String roomid) {
        try {
            this.m_locker.lock();
            this.m_map.remove(roomid);
        } finally {
            this.m_locker.unlock();
        }

    }
}
