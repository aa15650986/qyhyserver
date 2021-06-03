

package com.zhuoan.biz.event.sw;

import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.sw.SwGameRoom;
import com.zhuoan.util.Dto;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GameTimerSw {
    private static final Logger logger = LoggerFactory.getLogger(GameTimerSw.class);
    @Resource
    private SwGameEventDeal swGameEventDeal;

    public GameTimerSw() {
    }

    public void gameOverTime(String roomNo, int timeLeft, int gameStatus) {
        for(int i = timeLeft; i >= 0 && RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null; --i) {
            SwGameRoom room = (SwGameRoom)RoomManage.gameRoomMap.get(roomNo);
            room.setTimeLeft(i);
            if (room.getGameStatus() != gameStatus) {
                break;
            }

            if (i == 0) {
                if (gameStatus == 2) {
                    this.swGameEventDeal.betFinish(roomNo);
                } else if (gameStatus == 3) {
                    this.swGameEventDeal.summary(roomNo);
                } else if (gameStatus == 4) {
                    this.swGameEventDeal.choiceBanker(roomNo);
                } else if (gameStatus == 6) {
                    this.swGameEventDeal.hideOverTime(roomNo);
                }
            }

            try {
                Thread.sleep(1000L);
            } catch (Exception var7) {
                logger.error("", var7);
            }
        }

    }

    public void startOverTime(String roomNo, String account, int timeLeft) {
        for(int i = timeLeft; i >= 0 && RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null; --i) {
            SwGameRoom room = (SwGameRoom)RoomManage.gameRoomMap.get(roomNo);
            room.setTimeLeft(i);
            if (room.getGameStatus() == 2) {
                room.setTimeLeft(0);
                break;
            }

            if (Dto.stringIsNULL(room.getBanker()) || !account.equals(room.getBanker())) {
                room.setTimeLeft(0);
                break;
            }

            if (i == 0) {
                this.swGameEventDeal.choiceBanker(roomNo);
            }

            try {
                Thread.sleep(1000L);
            } catch (Exception var7) {
                logger.error("", var7);
            }
        }

    }
}
