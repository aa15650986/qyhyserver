
package com.zhuoan.webapp.listener.socketio;

import com.zhuoan.service.socketio.SocketIoManagerService;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PreDestroyListener {
    private static final Logger logger = LoggerFactory.getLogger(PreDestroyListener.class);
    @Resource
    private SocketIoManagerService service;

    public PreDestroyListener() {
    }

    @PreDestroy
    public void closeSocketPort() {
        if (this.service.getServer() != null) {
        }

    }
}
