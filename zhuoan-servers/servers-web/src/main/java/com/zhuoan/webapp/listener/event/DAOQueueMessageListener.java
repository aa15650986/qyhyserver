
package com.zhuoan.webapp.listener.event;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.event.circle.CircleBaseEventDeal;
import com.zhuoan.biz.event.tea.TeaEventDeal;
import com.zhuoan.biz.game.biz.CircleBiz;
import com.zhuoan.biz.game.biz.GameCircleService;
import com.zhuoan.biz.game.biz.GameLogBiz;
import com.zhuoan.biz.game.biz.PublicBiz;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.exception.EventException;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.TeaService;
import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import net.sf.json.JSONObject;
import org.apache.activemq.command.ActiveMQObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("daoQueueMessageListener")
public class DAOQueueMessageListener implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(DAOQueueMessageListener.class);
    @Resource
    private RoomBiz roomBiz;
    @Resource
    private UserBiz userBiz;
    @Resource
    private PublicBiz publicBiz;
    @Resource
    private GameLogBiz gameLogBiz;
    @Resource
    private GameCircleService gameCircleService;
    @Resource
    private CircleBaseEventDeal circleBaseEventDeal;
    @Resource
    private CircleBiz circleBiz;
    @Resource
    private TeaService teaService;
    @Resource
    private TeaEventDeal teaEventDeal;
    @Resource
    private RedisInfoService redisInfoService;

    public DAOQueueMessageListener() {
    }

    public void onMessage(Message message) {
        PumpDao pumpDao = (PumpDao)this.obtainMessageStr(message);
        JSONObject o = pumpDao.getObjectDao();
        String key;
        if (null != pumpDao.getIdempotentUUID() && !"".equals(pumpDao.getIdempotentUUID())) {
            key = "idempotentUUID" + ":" + pumpDao.getDaoType() + ":" + pumpDao.getIdempotentUUID();
            if (!this.redisInfoService.idempotent(key)) {
                return;
            }
        }

        key = pumpDao.getDaoType();
        byte var5 = -1;
        switch(key.hashCode()) {
            case -1564582088:
                if (key.equals("applyJoinTea")) {
                    var5 = 13;
                }
                break;
            case -1538819684:
                if (key.equals("update_room_info")) {
                    var5 = 3;
                }
                break;
            case -1514840553:
                if (key.equals("add_or_update_user_coins_rec")) {
                    var5 = 10;
                }
                break;
            case -1319943107:
                if (key.equals("insert_game_log")) {
                    var5 = 6;
                }
                break;
            case -1153858319:
                if (key.equals("user_deduction")) {
                    var5 = 2;
                }
                break;
            case -1061075872:
                if (key.equals("insertTeaMemberMoneyRecord")) {
                    var5 = 18;
                }
                break;
            case -598057060:
                if (key.equals("update_score")) {
                    var5 = 1;
                }
                break;
            case -566287083:
                if (key.equals("update_quit_user_room")) {
                    var5 = 19;
                }
                break;
            case -458772060:
                if (key.equals("update_room_index")) {
                    var5 = 4;
                }
                break;
            case -430668636:
                if (key.equals("insert_app_obj_rec")) {
                    var5 = 8;
                }
                break;
            case -236094327:
                if (key.equals("insert_Za_User_Game_Statis")) {
                    var5 = 12;
                }
                break;
            case -131898196:
                if (key.equals("update_user_info")) {
                    var5 = 9;
                }
                break;
            case 3452520:
                if (key.equals("pump")) {
                    var5 = 0;
                }
                break;
            case 375917669:
                if (key.equals("insert_user_game_log")) {
                    var5 = 7;
                }
                break;
            case 468157738:
                if (key.equals("addTeaRoom")) {
                    var5 = 16;
                }
                break;
            case 875958592:
                if (key.equals("circle_game_romm_recover")) {
                    var5 = 11;
                }
                break;
            case 1227160292:
                if (key.equals("update_za_gamerooms")) {
                    var5 = 20;
                }
                break;
            case 1423483312:
                if (key.equals("insertTeaMemberMsg")) {
                    var5 = 15;
                }
                break;
            case 1670575547:
                if (key.equals("teaGameRommCreate")) {
                    var5 = 17;
                }
                break;
            case 2031615746:
                if (key.equals("insert_game_room")) {
                    var5 = 5;
                }
                break;
            case 2086171889:
                if (key.equals("importMemberTea")) {
                    var5 = 14;
                }
        }

        switch(var5) {
            case 0:
                logger.info("抽水ING");
                this.roomBiz.pump(o.getJSONArray("array"), o.getString("roomNo"), o.getInt("gId"), o.getDouble("fee"), o.getString("updateType"));
                break;
            case 1:
                logger.info("更新元宝ING");
                this.userBiz.updateUserBalance(o.getJSONArray("array"), o.getString("updateType"));
                break;
            case 2:
                logger.info("更新元宝记录ING");
                this.userBiz.insertUserdeduction(o);
                break;
            case 3:
                logger.info("更新房间信息ING");
                this.roomBiz.updateGameRoom(o);
                break;
            case 4:
                if (o.containsKey("roomId")) {
                    logger.info("添加房间局数ING");
                    this.roomBiz.increaseRoomIndexByRoomNo(o.getString("roomId"));
                }
                break;
            case 5:
                logger.info("插入房间信息ING");
                this.roomBiz.insertGameRoom(o);
                break;
            case 6:
                logger.info("插入战绩信息ING");
                this.gameLogBiz.addOrUpdateGameLog(o);
                break;
            case 7:
                if (o.containsKey("array") && o.containsKey("object")) {
                    logger.info("插入玩家战绩信息ING");
                    this.gameLogBiz.addUserGameLog(o.getJSONArray("array"), o.getJSONObject("object"));
                }
                break;
            case 8:
                logger.info("插入玩家洗牌记录ING");
                this.publicBiz.addAppObjRec(o);
                break;
            case 9:
                logger.info("更新玩家信息ING");
                this.userBiz.updateUserPump(o.getString("account"), o.getString("updateType"), o.getDouble("sum"));
                break;
            case 10:
                logger.info("更新玩家积分ING");
                this.publicBiz.addOrUpdateUserCoinsRec(o.getLong("userId"), o.getInt("score"), o.getInt("type"));
                break;
            case 11:
                if (o.containsKey("roomCount")) {
                    logger.info("重启服务 恢复俱乐部创建的房间数量:" + o.get("roomCount"));
                }

                this.circleBaseEventDeal.quickJoinRoomCircleEvent((SocketIOClient)null, o);
                break;
            case 12:
                logger.info("批量插入大赢家 游戏记录统计");
                this.circleBiz.insertZaUserGameStatis(o);
                break;
            case 13:
                logger.info("申请加入茶楼");
                this.teaService.applyJoinTea(o);
                break;
            case 14:
                logger.info("批量导入茶楼成员");
                this.teaService.importMemberTea(o);
                break;
            case 15:
                logger.info("插入茶楼消息通知");
                if (o.containsKey("memberId") && o.containsKey("type")) {
                    this.teaService.insertTeaMemberMsg(o);
                }
                break;
            case 16:
                if (o.containsKey("teaInfoCode") && o.containsKey("used") && o.containsKey("unused")) {
                    logger.info("自动生成茶楼房间teaInfoCode：[{}]", o.get("teaInfoCode"));
                    this.teaService.generationTeaRoom(o);
                }
                break;
            case 17:
                logger.info("茶楼房间创建");
                this.teaEventDeal.createRoomTeaEvent((SocketIOClient)null, o);
                break;
            case 18:
                logger.info("插入茶楼房间消耗记录");
                this.teaService.insertTeaMemberMoneyRecord(o);
                break;
            case 19:
                logger.info("玩家退出房间 更新房间数据");
                this.roomBiz.updateRoomQuitUser(o);
                break;
            case 20:
                logger.info("同步更新房间信息");
                this.roomBiz.updateZaGamerooms(o);
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
