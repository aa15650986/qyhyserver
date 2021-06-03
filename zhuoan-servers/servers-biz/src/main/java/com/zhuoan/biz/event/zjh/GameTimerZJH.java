
package com.zhuoan.biz.event.zjh;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.zjh.UserPacket;
import com.zhuoan.biz.model.zjh.ZJHGameRoomNew;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.ZJHConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Resource;
import javax.jms.Destination;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GameTimerZJH {
	private static final Logger logger = LoggerFactory.getLogger(GameTimerZJH.class);
	@Resource
	private Destination zjhQueueDestination;
	@Resource
	private ProducerService producerService;

	public GameTimerZJH() {
	}

	public void readyOverTime(String roomNo, int gameStatus, int time) {
		ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo); 
		System.out.println("单局倒计时："+time);
		for (int i = time; i >= 0 && RoomManage.gameRoomMap.containsKey(roomNo)
				&& RoomManage.gameRoomMap.get(roomNo) != null; --i) {
			System.out.println("--------------------"+i);
			if (i == 0) {
				System.out.println("倒计时结束 开始判断所有未准备的玩家");
				List<String> unReadyList = new ArrayList<String>();
				for (String acc : room.getUserPacketMap().keySet()) {
					if (room.getUserPacketMap().get(acc).getStatus() != ZJHConstant.ZJH_USER_STATUS_READY) {
						unReadyList.add(acc);
					}
				}
				for (String string : unReadyList) {
					JSONObject data = new JSONObject();
					data.put("room_no", room.getRoomNo());
					data.put("account", string);
					SocketIOClient client = GameMain.server.getClient(room.getPlayerMap().get(string).getUuid());
					System.out.println(string + "自动准备");
					producerService.sendMessage(zjhQueueDestination, new Messages(client, data, 6, 1));
				}

			}
			try {
				Thread.sleep(1000L);
			} catch (Exception var9) {
				logger.error("", var9);
			}
		}
	}

	public void gameOverTime(String roomNo, int gameStatus, String account, int time) {
		System.out.println("玩家" + account + "开始定时任务 时间为：" + time);
		/*
		 * 如果为下注状态 先判断此人是第几人下注 如果为第一人 则时间到了自动判定为1注 如果不是第一人 则时间到了直接判为飞牌
		 */
		ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		System.out.println(room);
		if (null != account && ("").equals(account)) {
			room.getUserPacketMap().get(account).isDo = false;
		}
		System.out.println(RoomManage.gameRoomMap.containsKey(roomNo));
		System.out.println(RoomManage.gameRoomMap.get(roomNo) != null);
		for (int i = time; i >= 0 && RoomManage.gameRoomMap.containsKey(roomNo)
				&& RoomManage.gameRoomMap.get(roomNo) != null; --i) {
			if (null != account && !("").equals(account)) {
				if (room.getUserPacketMap().get(account).isDo) {
					System.out.println(account + "操作 跳出定时任务");
					System.out.println("此时玩家状态为：" + room.getUserPacketMap().get(account).getStatus());
					room.getUserPacketMap().get(account).isDo = false;
					break;
				}
			}
			System.out.println(account + "=自动任务倒计时=" + i);

			if (gameStatus == ZJHConstant.ZJH_GAME_STATUS_SUMMARY) {
				System.out.println("单局结束倒计时==" + i);
				// 此时需要倒计时 然后通知玩家准备
				// 当i==0时 获取所有未操作的玩家信息
				if (i == 0) {
					System.out.println("倒计时结束 开始判断所有未准备的玩家");
					List<String> unReadyList = new ArrayList<String>();
					for (String acc : room.getUserPacketMap().keySet()) {
						if (room.getUserPacketMap().get(acc).getStatus() != ZJHConstant.ZJH_USER_STATUS_READY) {
							unReadyList.add(acc);
						}
					}
					for (String string : unReadyList) {
						JSONObject data = new JSONObject();
						data.put("room_no", room.getRoomNo());
						data.put("account", string);
						SocketIOClient client = GameMain.server.getClient(room.getPlayerMap().get(string).getUuid());
						System.out.println(string + "自动准备");
						producerService.sendMessage(zjhQueueDestination, new Messages(client, data, 6, 1));
					}

				}
			}
			if (room.getGameStatus() != gameStatus || !room.getFocus().equals(account)) {
				break;
			}
			JSONObject data;
			SocketIOClient client;
			if (null != account && ("").equals(account)
					&& (i == time - 1 && ((UserPacket) room.getUserPacketMap().get(account)).isGenDaoDi)) {
				data = new JSONObject();
				data.put("roomNo", room.getRoomNo());
				data.put("uid", account);
				data.put("type", 5);
				client = GameMain.server.getClient(((Playerinfo) room.getPlayerMap().get(account)).getUuid());
				this.producerService.sendMessage(this.zjhQueueDestination, new Messages(client, data, 6, 2));
			}

			if (null != account && !("").equals(account) && (i == 0)) {
				System.out.println("............................");
				if (!room.getUserPacketMap().get(account).isDo) {
					// 倒计时结束 玩家未操作 进行弃牌
					data = new JSONObject();
					data.put("roomNo", room.getRoomNo());
					data.put("uid", account);
					data.put("type", 2);// 弃牌
					client = GameMain.server.getClient(((Playerinfo) room.getPlayerMap().get(account)).getUuid());
					System.out.println("发送弃牌请求");
					this.producerService.sendMessage(this.zjhQueueDestination, new Messages(client, data, 6, 2));
				}

			}

			try {
				Thread.sleep(1000L);
			} catch (Exception var9) {
				logger.error("", var9);
			}
		}
	}

	public void closeRoomOverTime(String roomNo, int timeLeft) {
		for (int i = timeLeft; i >= 0 && RoomManage.gameRoomMap.containsKey(roomNo)
				&& RoomManage.gameRoomMap.get(roomNo) != null; --i) {
			ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
			if (room.getJieSanTime() == 0) {
				break;
			}

			room.setJieSanTime(i);
			if (i == 0) {
				List<String> autoAccountList = new ArrayList();
				Iterator var6 = room.getUserPacketMap().keySet().iterator();

				String account;
				while (var6.hasNext()) {
					account = (String) var6.next();
					if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null
							&& ((Playerinfo) room.getPlayerMap().get(account)).getIsCloseRoom() == 0) {
						autoAccountList.add(account);
					}
				}

				var6 = autoAccountList.iterator();

				while (var6.hasNext()) {
					account = (String) var6.next();
					JSONObject data = new JSONObject();
					data.put("room_no", room.getRoomNo());
					data.put("account", account);
					data.put("type", 1);
					SocketIOClient client = GameMain.server
							.getClient(((Playerinfo) room.getPlayerMap().get(account)).getUuid());
					this.producerService.sendMessage(this.zjhQueueDestination, new Messages(client, data, 6, 5));
				}
			}

			try {
				Thread.sleep(1000L);
			} catch (Exception var10) {
				logger.error("", var10);
			}
		}

	}
}
