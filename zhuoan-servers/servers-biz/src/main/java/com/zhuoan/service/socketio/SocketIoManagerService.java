
package com.zhuoan.service.socketio;

import com.corundumstudio.socketio.SocketIOServer;

public interface SocketIoManagerService {
    void startServer(boolean var1);

    void stopServer();

    SocketIOServer getServer();

    void sendMessageToAllClient(String var1, String var2);

    void sendMessageToOneClient(String var1, String var2, String var3);
}
