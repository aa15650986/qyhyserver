package com.zhuoan.biz.robot;

import javax.annotation.Resource;
import javax.jms.Destination;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.zhuoan.biz.event.circle.CircleBaseEvent;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;

public class RobotEvent {
	@Resource
    private Destination robotQueueDestination;
    @Resource
    private ProducerService producerService;
    
    

    public RobotEvent() {
    }

    private void sendMessage(SocketIOClient client, Object data, int sorts) {
        this.producerService.sendMessage(this.robotQueueDestination, new Messages(client, data, 200, sorts));
    }
	
	  public void listenerCircleBaseEvent(SocketIOServer server) {
	        server.addEventListener("addRobotasd1", Object.class, new DataListener<Object>() {
	            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
	            	RobotEvent.this.sendMessage(client, data, 0);
	            }
	        });
	        
	        
	    }

}
