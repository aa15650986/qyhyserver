package com.zhuoan.webapp.controller.socketio;

import com.corundumstudio.socketio.Configuration;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.service.socketio.SocketIoManagerService;
import com.zhuoan.webapp.controller.BaseController;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class SocketIoController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(SocketIoController.class);
    @Resource
    private SocketIoManagerService service;

    public SocketIoController() {
    }

    @RequestMapping(
            value = {"123startServer"},
            method = {RequestMethod.GET}
    )
    @ResponseBody
    public String startServer(HttpServletRequest request) {
        logger.info("[" + getIp(request) + "] 手动启动 SocketIO服务");
        if (this.service.getServer() == null) {
            try {
                this.service.startServer(false);
            } catch (Exception var5) {
                return "<br><br><br><br><br><h1><div style=\"text-align: center;color: #F44336;\">服务启动失败：RMI异常" + var5 + "</div></h1><br><p><div><br>" + Arrays.toString(var5.getStackTrace()) + "</div></p>";
            }
        }

        Configuration configuration = this.service.getServer().getConfiguration();
        int port = configuration.getPort();
        String host = configuration.getHostname();
        return "<br><br><br><br><br><h1><div style=\"text-align: center;color: #4CAF50;\">恭喜你，成功启用服务</h1></div><br><p><div style=\"text-align: center;\"> [" + host + ":" + port + "]</div></p>";
    }

    @RequestMapping(
            value = {"123setting"},
            method = {RequestMethod.GET}
    )
    @ResponseBody
    public String setting(HttpServletRequest request) {
        try {
            String k = request.getParameter("k");
            String v = request.getParameter("v");
            if (null == v) {
                if ("all".equals(k)) {
                    RoomManage.gameConfigMap = new ConcurrentHashMap();
                } else {
                    RoomManage.gameConfigMap.remove(k);
                }

                return "成功";
            } else {
                JSONObject o1 = new JSONObject();
                String[] v1 = v.split("----------");
                String[] v2 = v1[1].split(",,,,,,,");
                JSONObject o3 = new JSONObject();
                String[] var8 = v2;
                int var9 = v2.length;

                for (int var10 = 0; var10 < var9; ++var10) {
                    String s = var8[var10];
                    String[] v3 = s.split(",,,--,,,,");
                    o3.element(v3[0], v3[1]);
                }

                o1.element(v1[0], o3.toString());
                RoomManage.gameConfigMap.put(k, o1.toString());
                return "成功";
            }
        } catch (Exception var13) {
            return "失败";
        }
    }

    @RequestMapping(
            value = {"123queryStatus"},
            method = {RequestMethod.GET}
    )
    @ResponseBody
    public String queryStatus(HttpServletRequest request) {
        logger.info(" [" + getIp(request) + "] 查看 SocketIO服务状态");
        if (this.service.getServer() != null) {
            Configuration configuration = this.service.getServer().getConfiguration();
            int port = configuration.getPort();
            String host = configuration.getHostname();
            return "<br><br><br><br><br><h1><div style=\"text-align: center;color: #4CAF50;\">SocketIO服务状态：进行中</h1></div><br><p><div style=\"text-align: center;\"> [" + host + ":" + port + "]</p>";
        } else {
            return "<br><br><br><br><br><h1><div style=\"text-align: center;color: #F44336;\">SocketIO服务状态：未启动</div></h1>";
        }
    }

    @RequestMapping(
            value = {"123stopServer95111"},
            method = {RequestMethod.GET}
    )
    @ResponseBody
    public String stopServer(HttpServletRequest request) {
        logger.info(" [" + getIp(request) + "] 手动停止 SocketIO服务");
        if (this.service.getServer() != null) {
            this.service.stopServer();
        }

        return "服务状态：关闭";
    }
}
