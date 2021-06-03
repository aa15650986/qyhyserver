
package com.zhuoan.service.socketio.impl;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.zhuoan.biz.event.BaseEventDeal;
import com.zhuoan.biz.event.BaseGameEvent;
import com.zhuoan.biz.event.bdx.BDXGameEvent;
import com.zhuoan.biz.event.circle.CircleBaseEvent;
import com.zhuoan.biz.event.club.ClubEvent;
import com.zhuoan.biz.event.ddz.DdzGameEvent;
import com.zhuoan.biz.event.gdy.GDYGameEvent;
import com.zhuoan.biz.event.gppj.GPPJGameEvent;
import com.zhuoan.biz.event.gzmj.GzMjGameEvent;
import com.zhuoan.biz.event.match.MatchEvent;
import com.zhuoan.biz.event.nn.NNGameEvent;
import com.zhuoan.biz.event.pdk.PDKGameEvent;
import com.zhuoan.biz.event.qzmj.QZMJGameEvent;
import com.zhuoan.biz.event.sg.SGGameEvent;
import com.zhuoan.biz.event.sss.SSSGameEvent;
import com.zhuoan.biz.event.sw.SwGameEvent;
import com.zhuoan.biz.event.tea.TeaEvent;
import com.zhuoan.biz.event.zjh.ZJHGameEvent;
import com.zhuoan.biz.game.dao.util.BaseSqlUtil;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.service.impl.SSSServiceImpl;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.SocketIoManagerService;
import com.zhuoan.util.DateUtils;
import com.zhuoan.util.LogUtil;
import com.zhuoan.util.PropertiesUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Resource;
import javax.jms.Destination;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GameMain implements SocketIoManagerService {
    private static final Logger log = LoggerFactory.getLogger(GameMain.class);
    public static SocketIOServer server;
    @Resource
    private BaseGameEvent baseGameEvent;
    @Resource
    private SSSGameEvent sssGameEvent;
    @Resource
    private ZJHGameEvent zjhGameEvent;
    @Resource
    private NNGameEvent nnGameEvent;
    @Resource
    private BDXGameEvent bdxGameEvent;
    @Resource
    private QZMJGameEvent qzmjGameEvent;
    @Resource
    private GPPJGameEvent gppjGameEvent;
    @Resource
    private SwGameEvent swGameEvent;
    @Resource
    private DdzGameEvent ddzGameEvent;
    @Resource
    private MatchEvent matchEvent;
    @Resource
    private ClubEvent clubEvent;
    @Resource
    private CircleBaseEvent circleBaseEvent;
    @Resource
    private TeaEvent teaEvent;
    @Resource
    private RedisInfoService redisInfoService;
    @Resource
    private RedisService redisService;
    @Resource
    private GDYGameEvent gdyGameEvent;
    @Resource
    private SGGameEvent sgGameEvent;
    @Resource
    private GzMjGameEvent gzMjGameEvent;
    @Resource
    private ProducerService producerService;
    @Resource
    private Destination daoQueueDestination;
    @Resource
    private PDKGameEvent pdkGameEvent;
    @Resource
    private BaseEventDeal baseEventDeal;
    public static boolean scheduledProcessing = false;
    public static boolean isRoomTask = false;
    public static PropertiesUtil propertiesUtil = new PropertiesUtil("ip.properties");
    public GameMain() {
    }

    public void startServer(boolean isGameTask) {
    	 long time1 = System.currentTimeMillis();
        Map<String, String> map = this.redisInfoService.getSokcetInfo();
        System.out.println(map);
        System.out.println("============GameMain==================");
        System.out.println("socket:"+map);
        System.out.println("============GameMain==================");
        int localPort = Integer.valueOf((String)map.get("socketPort"));
        this.stopServer();
        server = new SocketIOServer(this.serverConfig(localPort));
        log.info("SocketIOServer 端口号{}", localPort);
        this.addEventListener(server);
        server.start();
        GetMsg.sssBad();
        GetMsg.sssSJ();
        try {
			BadPai.creatServer();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        if (isGameTask) {
            log.info("============================== 紧接,SocketIO服务完成了监听事件的添加  ==============================");
            log.info("==============================[ SOCKET-IO服务启用成功 ]==============================");
            if (!scheduledProcessing) {
                scheduledProcessing = true;
                this.scheduleDeal();
                ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(2, (new Builder()).namingPattern("heartbeat-schedule-pool-%d").daemon(true).build());
                executor.scheduleWithFixedDelay(new GameMain.RoomTask(), 0L, 5L, TimeUnit.MINUTES);
                executor.scheduleWithFixedDelay(new GameMain.SocketTask(), 0L, 50L, TimeUnit.SECONDS);
            }
        }

        
        
    }

    public void stopServer() {
        try {
            server.stop();
            Configuration configuration = server.getConfiguration();
            int port = configuration.getPort();
          
            String host = configuration.getHostname();
            System.out.println("host==========="+host);
            //host = "192.168.124.3";
            server = null;
            log.info("==============================[ SOCKET-IO服务[" + host + ":" + port + "]已关闭 ]==============================");
        } catch (Exception var4) {
        }

    }

    public void sendMessageToAllClient(String eventType, String message) {
        Collection<SocketIOClient> clients = server.getAllClients();
        Iterator var4 = clients.iterator();

        while(var4.hasNext()) {
            SocketIOClient client = (SocketIOClient)var4.next();
            client.sendEvent(eventType, new Object[]{message});
        }

    }

    public void sendMessageToOneClient(String uuid, String eventType, String message) {
        if (StringUtils.isNotBlank(uuid)) {
            Map<String, SocketIOClient> clientsMap = new HashMap();
            SocketIOClient client = (SocketIOClient)clientsMap.get(uuid);
            if (client != null) {
                client.sendEvent(eventType, new Object[]{message});
            }
        }

    }

    public SocketIOServer getServer() {
        return server;
    }

    private Configuration serverConfig(int localPort) {
        Configuration config = new Configuration();
        config.setPort(localPort);
        config.setWorkerThreads(2);
        config.setMaxFramePayloadLength(1048576);
        config.setMaxHttpContentLength(1048576);
        log.info("============================== 其次,SocketIO 启用本地服务 [(当前主机内网IP):" + localPort + "(必须开放此端口)] ==============================");
        return config;
    }

    private void scheduleDeal() {
        try {
            long startTime = DateUtils.getLongTime();
			
            
            log.info("----------------------------------------  启动服务器删除redis和创建房间信息 开始---------------------------------------- ");
            this.delRedisSystem();
            this.preSelectRoomSetting();
            long endTime = DateUtils.getLongTime();
            int robotCount = DBUtil.executeUpdateBySQL("UPDATE robot_info SET status = 0 WHERE status != 0 ", (Object[])null);
            log.info("----------------------------------------  robot 更新状态数量 [{}]---------------------------------------- ", robotCount);
            log.info("----------------------------------------  启动服务器删除redis、创建房间信息、游戏战绩 结束---------------------------------------- ");
            log.info("----------------------------------------  用时：" + (endTime - startTime) + "毫秒---------------------------------------- ");
        } catch (Exception var6) {
            log.info("scheduleDeal [{}]", var6);
        }

    }

    private void preSelectRoomSetting() {
        String sql = "SELECT `base_info` FROM za_gamerooms  WHERE `status` IN(0,1) GROUP BY `room_no`";
        JSONArray array = DBUtil.getObjectListBySQL(sql, (Object[])null);
        LogUtil.print("查询上次服务器断开所有游戏未结束的房间数量：" + array.size());
        BaseSqlUtil.updateDataByMore("za_gamerooms", new String[]{"status"}, new String[]{"-1"}, (String[])null, (Object[])null, " where `status` in(0,1)");
        if (array != null && array.size() != 0) {
            AtomicInteger count = new AtomicInteger();

            for(int i = 0; i < array.size(); ++i) {
                if (array.getJSONObject(i) != null && array.getJSONObject(i).containsKey("base_info")) {
                    JSONObject baseInfo = array.getJSONObject(i).getJSONObject("base_info");
                    if (baseInfo != null && baseInfo.containsKey("gameRoomOther")) {
                        JSONObject gameRoomOther = baseInfo.getJSONObject("gameRoomOther");
                        if (gameRoomOther != null) {
                            JSONObject object = new JSONObject();
                            Iterator iterator = gameRoomOther.keys();

                            while(iterator.hasNext()) {
                                String key = (String)iterator.next();
                                object.put(key, gameRoomOther.get(key));
                            }

                            object.put("base_info", baseInfo);
                            count.addAndGet(1);
                            object.put("roomCount", count);
                            this.producerService.sendMessage(this.daoQueueDestination, new PumpDao("circle_game_romm_recover", object));
                        }
                    }
                }
            }

        }
    }

    private void delRedisSystem() {
        this.redisInfoService.delGameSettingAll();
        this.redisInfoService.delGameInfoAll();
        this.redisInfoService.delSummaryAll();
        this.redisService.deleteByLikeKey("startTimes_*");
        this.redisInfoService.delAppTimeSettingAll();
        this.redisInfoService.delTeaInfoAllCode();
        this.redisInfoService.delTeaRoomOpen((String)null);
        this.redisInfoService.delExistCreateRoom((String)null);
        this.redisInfoService.delTeaSys();
        this.redisInfoService.delGameSssSpecialSetting();
        this.redisInfoService.delAllCreateCircle();
        this.redisInfoService.delAllReadMessageRepeatCheck();
        this.redisInfoService.delHasControl();
        this.redisInfoService.delSysGlobal();
        this.redisInfoService.delSokcetInfo();
    }

    private void addEventListener(SocketIOServer server) {
        this.baseGameEvent.listenerBaseGameEvent(server);
        this.bdxGameEvent.listenerBDXGameEvent(server);
        this.nnGameEvent.listenerNNGameEvent(server);
        this.sssGameEvent.listenerSSSGameEvent(server);
        this.zjhGameEvent.listenerZJHGameEvent(server);
        this.qzmjGameEvent.listenerQZMJGameEvent(server);
        this.gppjGameEvent.listenerGPPJGameEvent(server);
        this.swGameEvent.listenerSwGameEvent(server);
        this.ddzGameEvent.listenerDDZGameEvent(server);
        this.matchEvent.listenerMatchGameEvent(server);
        this.clubEvent.listenerClubEvent(server);
        this.circleBaseEvent.listenerCircleBaseEvent(server);
        this.gdyGameEvent.listenerGDYGameEvent(server);
        this.sgGameEvent.listenerSGGameEvent(server);
        this.gzMjGameEvent.listenerGZMJGameEvent(server);
        this.teaEvent.listenerTeaGameEvent(server);
        this.pdkGameEvent.listenerPDKGameEvent(server);
    }

    class RoomTask extends TimerTask {
        RoomTask() {
        }

        public void run() {
            try {
                CommonConstant.socketMap = null;
                if (!GameMain.isRoomTask) {
                    GameMain.isRoomTask = true;
                    return;
                }

                GameMain.log.info("...游戏中的房间检查是否存在...");
                JSONArray roomArray = DBUtil.getObjectListBySQL("SELECT z.`id`,z.`room_no`,z.`player_number`,z.`game_index` FROM za_gamerooms z WHERE z.`status`IN(1) ORDER BY createtime LIMIT 0,10", (Object[])null);
                if (null == roomArray || roomArray.size() == 0) {
                    return;
                }

                for(int i = 0; i < roomArray.size(); ++i) {
                    JSONObject roomObject = roomArray.getJSONObject(i);
                    if (null == roomObject || !roomObject.containsKey("id") || !roomObject.containsKey("room_no") || !roomObject.containsKey("player_number") || !roomObject.containsKey("game_index")) {
                        return;
                    }

                    GameRoom room = (GameRoom)RoomManage.gameRoomMap.get(roomObject.getString("room_no"));
                    if (null == room) {
                        GameMain.log.info("房间号[{}]已不存在,删除该房间", roomObject.getString("room_no"));
                        DBUtil.executeUpdateBySQL("UPDATE `za_gamerooms` SET `status`= ? WHERE `id` = ? ", new Object[]{2, roomObject.get("id")});
                    } else if (room.getPlayerMap().size() != roomObject.getInt("player_number") || 0 == room.getGameStatus() || room.getGameNewIndex() != roomObject.getInt("game_index") || room.getId() != roomObject.getLong("id")) {
                        DBUtil.executeUpdateBySQL("UPDATE `za_gamerooms` SET `status`= ? WHERE `id` = ? ", new Object[]{2, roomObject.get("id")});
                        GameMain.this.baseEventDeal.updateZaGamerooms(roomObject.getString("room_no"));
                    }
                }
            } catch (Exception var5) {
                GameMain.log.info("游戏中的房间[{}]", var5);
            }

        }
    }

    class SocketTask extends TimerTask {
        SocketTask() {
        	
        }

        public void run() {
            try {
                if (null == GameMain.server) {
                    GameMain.log.info("...socket检测连接.....................【异常】");
                    GameMain.this.startServer(false);
                } else {
                    GameMain.log.info("...socket检测连接.....................【正常】");
                }
            } catch (Exception var2) {
                GameMain.log.info("定时检测...SocketIOServer...连接情况[{}]", var2);
            }

        }
    }
}
