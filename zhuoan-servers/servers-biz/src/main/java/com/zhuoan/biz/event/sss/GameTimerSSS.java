package com.zhuoan.biz.event.sss;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.sss.SSSGameRoomNew;
import com.zhuoan.biz.model.sss.SSSUserPacket;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.SSSConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.ArrayList;
import java.util.List;

@Component
public class GameTimerSSS {

    private final static Logger logger = LoggerFactory.getLogger(GameTimerSSS.class);

    @Resource
    private Destination sssQueueDestination;
    @Resource
    private RedisInfoService redisInfoService;
    @Resource
    private ProducerService producerService;


    public void gameReadyOverTime(String roomNo, String account, int timeLeft) {

        for (int i = timeLeft; i >= 0; i--) {
            SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
            //房间不存在
            if (room == null) return;
            // 游戏不处理准备或结算阶段
            int gameStatus = room.getGameStatus();
            if (SSSConstant.SSS_GAME_STATUS_READY != gameStatus)
                return;
            SSSUserPacket up = room.getUserPacketMap().get(account);
            Playerinfo playerinfo = room.getPlayerMap().get(account);
            if (null == up || null == playerinfo || CommonConstant.ROOM_VISIT_INDEX == playerinfo.getMyIndex()) return;
            if (SSSConstant.SSS_USER_STATUS_READY == up.getStatus()) {
                up.setTimeLeft(0);//存入倒计时变化
                return;
            }
            up.setTimeLeft(i);//存入倒计时变化

            if (i == 0) {//时间到
                JSONObject data = new JSONObject();
                data.put(CommonConstant.DATA_KEY_ROOM_NO, roomNo);
                data.put(CommonConstant.DATA_KEY_ACCOUNT, account);
                int sorts = SSSConstant.SSS_GAME_EVENT_EXIT;//退出房间
                int roomType = room.getRoomType();
                // 房卡和局数场 有参与的玩家 强制准备
                if (CommonConstant.ROOM_TYPE_FK == roomType || CommonConstant.ROOM_TYPE_INNING == roomType) {
                    if (playerinfo.getPlayTimes() > 0) {
                        sorts = SSSConstant.SSS_GAME_EVENT_READY;
                    }
                }
                SocketIOClient client = GameMain.server.getClient(playerinfo.getUuid());
                producerService.sendMessage(sssQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_SSS, sorts));
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                System.out.println("十三水准备倒计时" + e);
            }
        }
    }


    public void gameOverTime(String roomNo, int gameStatus, int timeLeft) {
        // 玩家状态
        int userStatus = SSSConstant.SSS_USER_STATUS_INIT;
        switch (gameStatus) {
            case SSSConstant.SSS_GAME_STATUS_READY:
                userStatus = SSSConstant.SSS_USER_STATUS_READY;
                break;
            case SSSConstant.SSS_GAME_STATUS_GAME_EVENT:
                userStatus = SSSConstant.SSS_USER_STATUS_GAME_EVENT;
                break;
            case SSSConstant.SSS_GAME_STATUS_XZ:
                userStatus = SSSConstant.SSS_USER_STATUS_XZ;
                break;
            case SSSConstant.SSS_GAME_STATUS_SUMMARY:
                userStatus = SSSConstant.SSS_USER_STATUS_READY;
                break;
            default:
                break;
        }
        for (int i = timeLeft; i >= 0; i--) {
            // 房间存在
            if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
                SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
                if (null == room) return;
                // 非当前游戏状态停止定时器
                if (room.getGameStatus() != gameStatus && room.getGameStatus() != SSSConstant.SSS_GAME_STATUS_READY) {
                    return;
                }
                if (room.isAgreeClose()) {//解散房间不处理
                    return;
                }

                // 准备状态需要检查当前准备人数是否大于最低开始人数
                if (gameStatus == SSSConstant.SSS_GAME_STATUS_READY && room.getNowReadyCount() < room.getMinPlayer()) {
                    return;
                }

                //是否重置倒计时
                if (room.isResetTime()) {
                    int cutTime = i;
                    //判断是否是切牌模式
                    if (SSSConstant.SSS_CUT_CARD_MODE_YES.equals(room.getCutCardMode())) {
                        cutTime = redisInfoService.getAppTimeSetting(room.getPlatform(), room.getGid(), "SSSConstant.SSS_CUT_CARD_TIMER");
                        if (cutTime <= 0) {
                            cutTime = SSSConstant.SSS_CUT_CARD_TIME;
                        }
                    }
                    i = cutTime;
                    room.setResetTime(false);
                }

                // 设置倒计时
                room.setTimeLeft(i);
                if (i == 0) {
                    // 当前阶段所有未完成操作的玩家
                    List<String> autoAccountList = new ArrayList<>();
                    for (String account : room.getPlayerMap().keySet()) {
                        SSSUserPacket up = room.getUserPacketMap().get(account);
                        Playerinfo playerinfo = room.getPlayerMap().get(account);
                        if (null == up || null == playerinfo || playerinfo.isCollapse()) continue; //破产玩家不考虑进去

                        if (userStatus == up.getStatus()) continue;
                        // 除准备阶段以外不需要判断中途加入的玩家
                        if (SSSConstant.SSS_USER_STATUS_INIT == up.getStatus() && SSSConstant.SSS_GAME_STATUS_READY != gameStatus
                                && SSSConstant.SSS_GAME_STATUS_SUMMARY != gameStatus) {
                            continue;
                        }

                        if (SSSConstant.SSS_BANKER_TYPE_ZZ == room.getBankerType() && SSSConstant.SSS_GAME_STATUS_READY == gameStatus) {
                            if (autoAccountList.contains(account) && account.equals(room.getBanker())) {
                                continue;
                            }
                        }
                        autoAccountList.add(account);
                    }
                    // 投递消息类型
                    int messageSort = 0;
                    for (String account : autoAccountList) {
                        // 组织数据
                        JSONObject data = new JSONObject();
                        // 房间号
                        data.put(CommonConstant.DATA_KEY_ROOM_NO, room.getRoomNo());
                        // 账号
                        data.put(CommonConstant.DATA_KEY_ACCOUNT, account);
                        if (gameStatus == SSSConstant.SSS_GAME_STATUS_READY) {
                            // 准备阶段超时踢出
                            if (room.getReadyOvertime() == CommonConstant.READY_OVERTIME_OUT) {
                                messageSort = SSSConstant.SSS_GAME_EVENT_EXIT;
                            }
                        }
                        if (gameStatus == SSSConstant.SSS_USER_STATUS_GAME_EVENT) {
                            messageSort = SSSConstant.SSS_GAME_EVENT_EVENT;
                            // 自动配牌
                            data.put("type", 1);
                        }
                        if (gameStatus == SSSConstant.SSS_GAME_STATUS_XZ) {
                            messageSort = SSSConstant.SSS_GAME_EVENT_XZ;
                            // 自动下注
                            data.put("money", 1);
                        }
                        if (gameStatus == SSSConstant.SSS_GAME_STATUS_SUMMARY) {
                            // 准备阶段超时踢出
                            if (room.getRoomType() == CommonConstant.ROOM_TYPE_FREE || room.getRoomType() == CommonConstant.ROOM_TYPE_YB) {
                                messageSort = SSSConstant.SSS_GAME_EVENT_EXIT;
                            } else if (room.getRoomType() == CommonConstant.ROOM_TYPE_INNING || room.getRoomType() == CommonConstant.ROOM_TYPE_TEA || room.getRoomType() == CommonConstant.ROOM_TYPE_FK) {
                                if (room.getPlayerMap().get(account).getPlayTimes() > 0) {
                                    messageSort = SSSConstant.SSS_GAME_EVENT_READY;
                                } else {
                                    int time = redisInfoService.getAppTimeSetting(room.getPlatform(), room.getGid(), "SSSConstant.SSS_TIMER_READY_INNING");
                                    if (time > 0 && time < 5) {//准备时间太短直接让玩家准备
                                        messageSort = SSSConstant.SSS_GAME_EVENT_READY;
                                    } else {
                                        messageSort = SSSConstant.SSS_GAME_EVENT_EXIT;
                                    }

                                }

                            }
                        }
                        if (messageSort > 0) {
                            SocketIOClient client = GameMain.server.getClient(room.getPlayerMap().get(account).getUuid());
                            producerService.sendMessage(sssQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_SSS, messageSort));
                        }
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    logger.error("", e);
                }
            } else {
                break;
            }
        }
    }


    public void closeRoomOverTime(String roomNo, int timeLeft) {
        for (int i = timeLeft; i >= 0; i--) {
            // 房间存在
            if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
                SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
                if (room.getJieSanTime() == 0) {
                    break;
                }
                // 设置倒计时
                room.setJieSanTime(i);
                if (i == 0) {
                    // 当前阶段所有未完成操作的玩家
                    List<String> autoAccountList = new ArrayList<String>();
                    for (String account : room.getPlayerMap().keySet()) {
                        Playerinfo playerinfo = room.getPlayerMap().get(account);
                        if (null == playerinfo) continue;
                        if (CommonConstant.CLOSE_ROOM_UNSURE == playerinfo.getIsCloseRoom()) {
                            autoAccountList.add(account);
                        }
                    }
                    for (String account : autoAccountList) {
                        // 组织数据
                        JSONObject data = new JSONObject();
                        // 房间号
                        data.put(CommonConstant.DATA_KEY_ROOM_NO, room.getRoomNo());
                        // 账号
                        data.put(CommonConstant.DATA_KEY_ACCOUNT, account);
                        // 同意解散
                        data.put("type", CommonConstant.CLOSE_ROOM_AGREE);
                        SocketIOClient client = GameMain.server.getClient(room.getPlayerMap().get(account).getUuid());
                        producerService.sendMessage(sssQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_SSS, SSSConstant.SSS_GAME_EVENT_CLOSE_ROOM));
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    logger.error("", e);
                }
            } else {
                break;
            }
        }
    }


}
