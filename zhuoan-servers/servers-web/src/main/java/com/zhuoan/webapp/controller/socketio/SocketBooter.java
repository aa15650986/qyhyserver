package com.zhuoan.webapp.controller.socketio;

import com.zhuoan.service.socketio.SocketIoManagerService;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class SocketBooter implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger log = LoggerFactory.getLogger(SocketIoController.class);
    @Resource
    private SocketIoManagerService service;

    public SocketBooter() {
    }

    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() == null) {
            log.info("自动启动 SocketIO服务");

            try {
                if (this.service.getServer() == null) {
                    this.service.startServer(true);
                }
            } catch (Exception var3) {
                log.info("SocketIO服务启动失败...[{}]", var3);
            }
        }

    }
}
