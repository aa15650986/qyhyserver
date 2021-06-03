package com.zhuoan.biz.event.nn;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.nn.NNGameRoomNew;
import com.zhuoan.biz.robot.RobotEventDeal;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.NNConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.AppBeanUtil;
import com.zhuoan.util.thread.ThreadPoolHelper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Component
public class GameTimerNN {

    private final static Logger logger = LoggerFactory.getLogger(GameTimerNN.class);
    @Resource
    private Destination nnQueueDestination;
    @Resource
    private ProducerService producerService;
    @Resource
    private RedisInfoService redisInfoService;

    public void gameOverTime(String roomNo, int gameStatus, int sleepTime) {
        if (sleepTime > 0) {
        	System.out.println("sleepTime="+sleepTime);
            try {
                Thread.sleep(sleepTime);
            } catch (Exception e) {
                logger.error("", e);
            }
        }
        if (!RoomManage.gameRoomMap.containsKey(roomNo) || RoomManage.gameRoomMap.get(roomNo) == null) {
            return;
        }
        NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);

        if (gameStatus == NNConstant.NN_GAME_STATUS_DZ) {
            // 设置游戏状态
            room.setGameStatus(NNConstant.NN_GAME_STATUS_XZ);
            room.setTimeLeft(NNConstant.NN_TIMER_XZ);
            gameStatus = NNConstant.NN_GAME_STATUS_XZ;
            // 通知玩家
            informPlayer(room);
        }
        // 倒计时
        int timeLeft = 0;
        // 玩家状态
        int userStatus = NNConstant.NN_USER_STATUS_INIT;
        switch (gameStatus) {
            case NNConstant.NN_GAME_STATUS_READY:
                timeLeft = NNConstant.NN_TIMER_READY;
                userStatus = NNConstant.NN_USER_STATUS_READY;
                break;
            case NNConstant.NN_GAME_STATUS_QZ:
                timeLeft = NNConstant.NN_TIMER_QZ;
                userStatus = NNConstant.NN_USER_STATUS_QZ;
                break;
            case NNConstant.NN_GAME_STATUS_XZ:
                timeLeft = NNConstant.NN_TIMER_XZ;
                userStatus = NNConstant.NN_USER_STATUS_XZ;
                break;
            case NNConstant.NN_GAME_STATUS_LP:
                timeLeft = NNConstant.NN_TIMER_SHOW;
                userStatus = NNConstant.NN_USER_STATUS_LP;
                break;
            case NNConstant.NN_GAME_STATUS_JS:
                if (CommonConstant.ROOM_TYPE_INNING == room.getRoomType()) {

                    timeLeft = redisInfoService.getAppTimeSetting(room.getPlatform(), room.getGid(), "NNConstant.NN_TIMER_READY_INNING");
                    System.out.println("从数据库读取的单局结束倒计时"+timeLeft);
                    if (-99 == timeLeft) {
                        timeLeft = NNConstant.NN_TIMER_READY_INNING;
                    }
                    timeLeft = NNConstant.NN_TIMER_READY_INNING;
                } else {
                    timeLeft = 15;
                }
                userStatus = NNConstant.NN_USER_STATUS_READY;
                break;
            default:
                break;
        }
        for (int i = timeLeft; i >= 0; i--) {
            // 房间存在
            if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
                room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
                // 非当前游戏状态停止定时器
                if (room.getGameStatus() != gameStatus && room.getGameStatus() != NNConstant.NN_GAME_STATUS_READY) {
                    break;
                }
                // 准备状态需要检查当前准备人数是否大于最低开始人数
                if (gameStatus == NNConstant.NN_GAME_STATUS_READY && room.getNowReadyCount() < NNConstant.NN_MIN_START_COUNT) {
                    break;
                }
                // 设置倒计时
                room.setTimeLeft(i);
                if (i == 0) {
                    // 当前阶段所有未完成操作的玩家
                    List<String> autoAccountList = new ArrayList<String>();
                    for (String account : room.getUserPacketMap().keySet()) {
                        if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                            // 除准备阶段以外不需要判断中途加入的玩家
                            if (gameStatus == NNConstant.NN_GAME_STATUS_READY || room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_INIT) {
                                if (room.getUserPacketMap().get(account).getStatus() != userStatus) {
                                    autoAccountList.add(account);
                                }
                            }
                            if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_ZZ && gameStatus == NNConstant.NN_GAME_STATUS_READY) {
                                if (autoAccountList.contains(account) && account.equals(room.getBanker())) {
                                    autoAccountList.remove(account);
                                }
                            }
                        }
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
                        if (gameStatus == NNConstant.NN_GAME_STATUS_READY) {
                            // 准备阶段超时踢出
                            if (room.getReadyOvertime() == CommonConstant.READY_OVERTIME_OUT) {
                                messageSort = NNConstant.NN_GAME_EVENT_EXIT;
                            }
                            // 准备阶段超时自动准备
                            if (room.getReadyOvertime() == CommonConstant.READY_OVERTIME_AUTO) {
                                messageSort = NNConstant.NN_GAME_EVENT_READY;
                            }
                        } else if (gameStatus == NNConstant.NN_GAME_STATUS_QZ) {
                            messageSort = NNConstant.NN_GAME_EVENT_QZ;
                            // 抢庄阶段超时不抢
                            data.put(NNConstant.DATA_KEY_VALUE, 0);
                        } else if (gameStatus == NNConstant.NN_GAME_STATUS_XZ) {
                            messageSort = NNConstant.NN_GAME_EVENT_XZ;
                            // 下注阶段默认下最小倍数
                            JSONArray baseNum = JSONArray.fromObject(room.getBaseNum());
                            data.put(NNConstant.DATA_KEY_MONEY, baseNum.getJSONObject(0).getInt("val"));
                        } else if (gameStatus == NNConstant.NN_GAME_STATUS_LP) {
                            messageSort = NNConstant.NN_GAME_EVENT_LP;
                        } else if (gameStatus == NNConstant.NN_GAME_STATUS_JS) {
                            if (CommonConstant.ROOM_TYPE_INNING == room.getRoomType()) {
                                messageSort = NNConstant.NN_GAME_EVENT_EXIT;
                                if (room.getPlayerMap().get(account).getPlayTimes() > 0) {
                                    messageSort = NNConstant.NN_GAME_EVENT_READY;
                                }
                            } else {
                                // 准备阶段超时踢出
                                if (room.getReadyOvertime() == CommonConstant.READY_OVERTIME_OUT) {
                                    messageSort = NNConstant.NN_GAME_EVENT_EXIT;
                                }
                                // 准备阶段超时自动准备
                                if (room.getReadyOvertime() == CommonConstant.READY_OVERTIME_AUTO) {
                                    messageSort = NNConstant.NN_GAME_EVENT_READY;
                                }
                            }
                        }
                        if (gameStatus == NNConstant.NN_GAME_STATUS_JS) {
							System.out.println("结算结束   下一阶段消息：========"+messageSort);
						}
                        if (messageSort > 0) {
                            SocketIOClient client = GameMain.server.getClient(room.getPlayerMap().get(account).getUuid());
                            System.out.println("-----------"+sleepTime+"------------------服务器自动进行下一阶段事件  事件ID："+messageSort);
                            producerService.sendMessage(nnQueueDestination, new Messages(client, data, 1, messageSort));
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
                NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
                if (room.getJieSanTime() == 0) {
                    break;
                }
                // 设置倒计时
                room.setJieSanTime(i);
                if (i == 0) {
                    // 当前阶段所有未完成操作的玩家
                    List<String> autoAccountList = new ArrayList<String>();
                    for (String account : room.getUserPacketMap().keySet()) {
                        if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                            if (room.getPlayerMap().get(account).getIsCloseRoom() == CommonConstant.CLOSE_ROOM_UNSURE) {
                                autoAccountList.add(account);
                            }
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
                        producerService.sendMessage(nnQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_NN, NNConstant.NN_GAME_EVENT_CLOSE_ROOM));
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


    public void chooseZhuangNnTime(final String roomNo) {
        NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        // 设置房间状态(定庄)
        room.setGameStatus(NNConstant.NN_GAME_STATUS_DZ);
        List<String> qzList = new ArrayList<>();
        if (room.getGameIndex() == 1) {//首局 随机庄
            for (String account : room.getUserPacketMap().keySet()) {
                if (room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_READY) continue;
                qzList.add(account);
            }
        } else {
            List<String> NNUser = room.getNNUser();
            if (NNUser == null || NNUser.size() == 0) {//中途没有人拿到牛牛的牌，则庄家继续由上一局庄家当
                qzList.add(room.getBanker());
            } else {
                qzList = NNUser;
            }
        }
        if (qzList == null || qzList.size() == 0) {
            return;
        } else if (qzList.size() == 1) {
            room.setBanker(qzList.get(0));
        } else {
            int bankerIndex = RandomUtils.nextInt(qzList.size());
            room.setBanker(qzList.get(bankerIndex));
        }
        room.setNNUser(null);
        // 通知前端状态改变
        informPlayer(room);
        if (room.getGameIndex() == 1) {//首局定庄3秒特效
            try {
                Thread.sleep(NNConstant.NN_TIMER_DZ * 1000);
            } catch (Exception e) {
                logger.error("", e);
            }
        }

        // 设置游戏状态(亮牌)
        room.setGameStatus(NNConstant.NN_GAME_STATUS_XZ);
        // 设置倒计时
        room.setTimeLeft(NNConstant.NN_TIMER_XZ);
        // 开启亮牌定时器
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                gameOverTime(roomNo, NNConstant.NN_GAME_STATUS_XZ, 0);
            }
        });
        // 通知前端状态改变
        informPlayer(room);
    }

    private void informPlayer(NNGameRoomNew room) {
        // 通知玩家
        for (String account : room.getPlayerMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                JSONObject obj = new JSONObject();
                obj.put("gameStatus", room.getGameStatus());
                obj.put("gameBankerType", room.getBankerType());
                if (room.getGameStatus() > NNConstant.NN_GAME_STATUS_DZ && room.getBankerType() != NNConstant.NN_BANKER_TYPE_TB) {
                    obj.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
                    obj.put("qzScore", room.getUserPacketMap().get(room.getBanker()).getQzTimes());
                } else {
                    obj.put("zhuang", -1);
                    obj.put("qzScore", 0);
                }
                obj.put("game_index", room.getGameIndex());
                obj.put("showTimer", CommonConstant.GLOBAL_YES);
                if (room.getTimeLeft() == NNConstant.NN_TIMER_INIT) {
                    obj.put("showTimer", CommonConstant.GLOBAL_NO);
                }
                obj.put("timer", room.getTimeLeft());
                obj.put("qzTimes", room.getQzTimes(room.getPlayerMap().get(account).getScore()));
                obj.put("baseNum", room.getBaseNumTimes(account));
                obj.put("users", room.getAllPlayer());
                obj.put("gameData", room.getGameData(account));
                UUID uuid = room.getPlayerMap().get(account).getUuid();
                if (uuid != null) {
                    CommonConstant.sendMsgEventToSingle(uuid, obj.toString(), "changeGameStatusPush_NN");
                }
            }
        }
        if (room.isRobot()) {
            for (String robotAccount : room.getRobotList()) {
                int delayTime = RandomUtils.nextInt(3) + 2;
                if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_XZ) {
                    AppBeanUtil.getBean(RobotEventDeal.class).changeRobotActionDetail(robotAccount, NNConstant.NN_GAME_EVENT_XZ, delayTime);
                }
            }
        }
    }
}
