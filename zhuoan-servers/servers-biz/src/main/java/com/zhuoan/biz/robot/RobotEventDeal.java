
package com.zhuoan.biz.robot;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.ddz.DdzCore;
import com.zhuoan.biz.core.nn.NNUserPacket;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.ddz.DdzGameRoom;
import com.zhuoan.biz.model.ddz.UserPacketDdz;
import com.zhuoan.biz.model.nn.NNGameRoomNew;
import com.zhuoan.biz.model.qzmj.QZMJGameRoom;
import com.zhuoan.biz.model.qzmj.QZMJUserPacket;
import com.zhuoan.constant.QZMJConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.util.Dto;
import com.zhuoan.util.thread.ThreadPoolHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Resource;
import javax.jms.Destination;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RobotEventDeal {
	private static final Logger logger = LoggerFactory.getLogger(RobotEventDeal.class);
	public static ConcurrentHashMap<String, RobotInfo> robots = new ConcurrentHashMap();
	public static List<String> roomNoList = new ArrayList<String>();
	@Resource
	private Destination baseQueueDestination;
	@Resource
	private Destination sssQueueDestination;
	@Resource
	private Destination pdkQueueDestination;
	@Resource
	private Destination ddzQueueDestination;
	@Resource
	private Destination nnQueueDestination;
	@Resource
	private Destination qzmjQueueDestination;
	@Resource
	private ProducerService producerService;
	@Resource
	private RoomBiz roomBiz;
	private static final int ACTION_TYPE_DO_ROBOT_JOIN = 9999;

	public RobotEventDeal() {
		System.out.println("开启机器人功能");
		startRobot();
		ThreadPoolHelper.executorService.submit(new Runnable() {
			public void run() {
				while (true) {
					if (roomNoList.size() > 0) {
						String roomNo = roomNoList.get(0);
						robotJoin(roomNo);
						roomNoList.remove(0);
					}
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				/*
				 * try { Thread.sleep(5000); System.out.println("添加机器人"); Set<String> set =
				 * RoomManage.gameRoomMap.keySet(); for (String roomNo : set) {
				 * System.out.println("===" + roomNo); robotJoin(roomNo); } } catch
				 * (InterruptedException e) { // TODO Auto-generated catch block
				 * e.printStackTrace(); }
				 */

			}
		});

	}

	public void startRobot() {
		ThreadPoolHelper.executorService.submit(new Runnable() {
			public void run() {
				while (true) {
					try {
						if (RobotEventDeal.robots.size() > 0) {
							Iterator var1 = RobotEventDeal.robots.keySet().iterator();

							while (var1.hasNext()) {
								String robotAccount = (String) var1.next();
								((RobotInfo) RobotEventDeal.robots.get(robotAccount)).subDelayTime();
								if (((RobotInfo) RobotEventDeal.robots.get(robotAccount)).getDelayTime() == 0) {
									switch (((RobotInfo) RobotEventDeal.robots.get(robotAccount)).getPlayGameId()) {
									case 1:
										RobotEventDeal.this.playNN(robotAccount);
										break;
									case 2:
										RobotEventDeal.this.playDdz(robotAccount);
										break;
									case 3:
										RobotEventDeal.this.playQzMj(robotAccount);
										break;
									case 4:
										RobotEventDeal.this.playSSS(robotAccount);
										break;
									case 21:
										RobotEventDeal.this.playPdk(robotAccount);
										break;
									}
								}
							}
						}

						Thread.sleep(1000L);
					} catch (Exception var3) {
					}
				}
			}
		});
	}

	public void robotJoin(String roomNo) {
		System.out.println("执行robotJoin");
		RoomManage.gameRoomMap.get(roomNo).setRobot(true);
		if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
			int totalCount = ((GameRoom) RoomManage.gameRoomMap.get(roomNo)).getPlayerCount();
			int playerCount = ((GameRoom) RoomManage.gameRoomMap.get(roomNo)).getPlayerMap().size();
			int robotCount = totalCount - playerCount;
			System.out.println("totalcount:" + totalCount);
			System.out.println("playercount:" + playerCount);
			if (robotCount < 0) {
				return;
			}
			double minScore = ((GameRoom) RoomManage.gameRoomMap.get(roomNo)).getEnterScore();
			double maxScore = ((GameRoom) RoomManage.gameRoomMap.get(roomNo)).getLeaveScore();
			System.out.println(minScore + "  " + maxScore);
			JSONArray robotArray = this.roomBiz.getRobotArray(robotCount, minScore, maxScore);
			System.out.println("人机：" + robotArray);
			for (int i = 0; i < robotArray.size(); ++i) {
				String robotAccount = robotArray.getJSONObject(i).getString("account");
				RobotInfo robotInfo = new RobotInfo();
				robotInfo.setRobotAccount(robotAccount);
				robotInfo.setPlayRoomNo(roomNo);
				robotInfo.setPlayGameId(((GameRoom) RoomManage.gameRoomMap.get(roomNo)).getGid());
				robotInfo.setRobotUUID(robotArray.getJSONObject(i).getString("uuid"));
				robotInfo.setDelayTime(RandomUtils.nextInt(3) + 3 * (i + 1));
				robotInfo.setOutTimes(RandomUtils.nextInt(60) + 15);
				robotInfo.setActionType(9999);
				robots.put(robotAccount, robotInfo);
			}

		}
	}

	public void robotJoinRoom(SocketIOClient client, JSONObject object) {

		String roomNo = object.getString("roomNo");
		RoomManage.gameRoomMap.get(roomNo).setRobot(true);
		System.out.println("机器人数组添加机器人");
		if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
			int totalCount = ((GameRoom) RoomManage.gameRoomMap.get(roomNo)).getPlayerCount();
			int playerCount = ((GameRoom) RoomManage.gameRoomMap.get(roomNo)).getPlayerMap().size();
			int robotCount = totalCount - playerCount - 3;
			if (robotCount < 0) {
				return;
			}
			double minScore = ((GameRoom) RoomManage.gameRoomMap.get(roomNo)).getEnterScore();
			double maxScore = ((GameRoom) RoomManage.gameRoomMap.get(roomNo)).getLeaveScore();
			System.out.println(minScore + "  " + maxScore);
			JSONArray robotArray = this.roomBiz.getRobotArray(robotCount, minScore, maxScore);

			for (int i = 0; i < robotArray.size(); ++i) {
				System.out.println("==============");
				String robotAccount = robotArray.getJSONObject(i).getString("account");
				RobotInfo robotInfo = new RobotInfo();
				robotInfo.setRobotAccount(robotAccount);
				robotInfo.setPlayRoomNo(roomNo);
				robotInfo.setPlayGameId(((GameRoom) RoomManage.gameRoomMap.get(roomNo)).getGid());
				robotInfo.setRobotUUID(robotArray.getJSONObject(i).getString("uuid"));
				robotInfo.setDelayTime(RandomUtils.nextInt(3) + 3 * (i + 1));
				robotInfo.setOutTimes(RandomUtils.nextInt(60) + 15);
				robotInfo.setActionType(9999);
				robots.put(robotAccount, robotInfo);
			}

		}
	}

	public void robotExit(String robotAccount) {
		if (this.checkRobotAccount(robotAccount)) {
			this.robotJoin(((RobotInfo) robots.get(robotAccount)).getPlayRoomNo());
			this.roomBiz.updateRobotStatus(robotAccount, 0);
			robots.remove(robotAccount);
		}

	}

	public void changeRobotActionDetail(String robotAccount, int nextActionType, int delayTime) {
		if (this.checkRobotAccount(robotAccount)) {
			((RobotInfo) robots.get(robotAccount)).setActionType(nextActionType);
			((RobotInfo) robots.get(robotAccount)).setDelayTime(delayTime);
			if (((RobotInfo) robots.get(robotAccount)).getPlayGameId() == 1 && nextActionType == 1) {
				((RobotInfo) robots.get(robotAccount)).subOutTimes();
			}

			if (((RobotInfo) robots.get(robotAccount)).getPlayGameId() == 4 && nextActionType == 1) {
				((RobotInfo) robots.get(robotAccount)).subOutTimes();
			}

			if (((RobotInfo) robots.get(robotAccount)).getPlayGameId() == 2 && nextActionType == 1) {
				((RobotInfo) robots.get(robotAccount)).subOutTimes();
			}
		}

	}

	private void doRobotJoin(String robotAccount) {
		if (this.checkRobotAccount(robotAccount)) {
			String roomNo = ((RobotInfo) robots.get(robotAccount)).getPlayRoomNo();
			if (!RoomManage.gameRoomMap.containsKey(roomNo) || RoomManage.gameRoomMap.get(roomNo) == null) {
				this.roomBiz.updateRobotStatus(robotAccount, 0);
				return;
			}

			if (((GameRoom) RoomManage.gameRoomMap.get(roomNo))
					.getPlayerCount() <= ((GameRoom) RoomManage.gameRoomMap.get(roomNo)).getPlayerMap().size()) {
				this.roomBiz.updateRobotStatus(robotAccount, 0);
				return;
			}

			JSONObject obj = new JSONObject();
			obj.put("account", robotAccount);
			obj.put("room_no", roomNo);
			obj.put("uuid", ((RobotInfo) robots.get(robotAccount)).getRobotUUID());
			obj.put("platform", "HYQP");
			this.producerService.sendMessage(this.baseQueueDestination, new Messages((SocketIOClient) null, obj, 0, 6));
			((GameRoom) RoomManage.gameRoomMap.get(roomNo)).getRobotList().add(robotAccount);
			((RobotInfo) robots.get(robotAccount)).setDelayTime(RandomUtils.nextInt(3) + 2);
			switch (((GameRoom) RoomManage.gameRoomMap.get(roomNo)).getGid()) {
			case 1:
				((RobotInfo) robots.get(robotAccount)).setActionType(1);
				break;
			case 2:
				((RobotInfo) robots.get(robotAccount)).setActionType(1);
				break;
			case 3:
				((RobotInfo) robots.get(robotAccount)).setActionType(1);
				break;
			case 4:
				((RobotInfo) robots.get(robotAccount)).setActionType(1);
				break;
			case 21:
				((RobotInfo) robots.get(robotAccount)).setActionType(1);
				break;
			}
		}

	}

	public void playNN(String robotAccount) {
		if (this.checkRobotAccount(robotAccount)) {
			RobotInfo robotInfo = (RobotInfo) robots.get(robotAccount);
			if (robotInfo.getActionType() == 9999) {
				this.doRobotJoin(robotAccount);
				return;
			}

			JSONObject obj = new JSONObject();
			obj.put("room_no", robotInfo.getPlayRoomNo());
			obj.put("account", robotAccount);
			if (robotInfo.getActionType() == 2) {
				obj.put("value", this.getRobotQZTimes(robotInfo.getPlayRoomNo(), robotAccount));
			}

			if (robotInfo.getActionType() == 3) {
				obj.put("money", this.getRobotXZTimes(robotInfo.getPlayRoomNo(), robotAccount));
			}

			if (robotInfo.getOutTimes() < 0 && robotInfo.getActionType() == 1) {
				robotInfo.setActionType(5);
			}

			this.producerService.sendMessage(this.nnQueueDestination,
					new Messages((SocketIOClient) null, obj, 1, robotInfo.getActionType()));
		}

	}

	public int getRobotXZTimes(String roomNo, String robotAccount) {
		int xzTimes = 1;
		if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
			int maxTimes = this.getMaxTimes(roomNo, robotAccount, 2);
			NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
			NNUserPacket robot = new NNUserPacket(((NNUserPacket) room.getUserPacketMap().get(robotAccount)).getPs(),
					room.getSpecialType());
			if (robot.getType() > 6) {
				xzTimes = maxTimes;
			} else if (robot.getType() > 0) {
				xzTimes += RandomUtils.nextInt(maxTimes);
			}
		}

		return xzTimes;
	}

	public int getRobotQZTimes(String roomNo, String robotAccount) {
		int qzTimes = 0;
		if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
			int maxTimes = this.getMaxTimes(roomNo, robotAccount, 1);
			NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
			NNUserPacket robot = new NNUserPacket(((NNUserPacket) room.getUserPacketMap().get(robotAccount)).getPs(),
					room.getSpecialType());
			if (robot.getType() > 0) {
				qzTimes = maxTimes;
			}
		}

		return qzTimes;
	}

	public int getMaxTimes(String roomNo, String robotAccount, int type) {
		int maxTimes = 0;
		if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
			NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
			JSONArray array = room.getQzTimes(((Playerinfo) room.getPlayerMap().get(robotAccount)).getScore());
			if (type == 2) {
				array = room.getBaseNumTimes(robotAccount);
			}

			if (array.size() > 0) {
				for (int i = 0; i < array.size(); ++i) {
					JSONObject baseNum = array.getJSONObject(i);
					if (baseNum.getInt("isuse") == 1 && baseNum.getInt("val") > maxTimes) {
						maxTimes = baseNum.getInt("val");
					}
				}
			}
		}

		return maxTimes;
	}

	public void playPdk(String robotAccount) {
		if(this.checkRobotAccount(robotAccount)) {
			RobotInfo robotInfo = (RobotInfo) robots.get(robotAccount);
			JSONObject obj = new JSONObject();
			obj.put("room_no", robotInfo.getPlayRoomNo());
			obj.put("account", robotAccount);
			if (robotInfo.getActionType() == 9999) {
				this.doRobotJoin(robotAccount);
				return;
			}
			if (robotInfo.getActionType() == 2) {
				obj.put("type", 1);
			}

			if (robotInfo.getOutTimes() < 0 && robotInfo.getActionType() == 1) {
				robotInfo.setActionType(4);
			}

			this.producerService.sendMessage(this.pdkQueueDestination,
					new Messages((SocketIOClient) null, obj, 21, robotInfo.getActionType()));
		}
	}

	public void playSSS(String robotAccount) {
		if (this.checkRobotAccount(robotAccount)) {
			RobotInfo robotInfo = (RobotInfo) robots.get(robotAccount);
			JSONObject obj = new JSONObject();
			obj.put("room_no", robotInfo.getPlayRoomNo());
			obj.put("account", robotAccount);
			if (robotInfo.getActionType() == 9999) {
				this.doRobotJoin(robotAccount);
				return;
			}
			if (robotInfo.getActionType() == 2) {
				obj.put("type", 1);
			}

			if (robotInfo.getOutTimes() < 0 && robotInfo.getActionType() == 1) {
				robotInfo.setActionType(3);
			}

			this.producerService.sendMessage(this.sssQueueDestination,
					new Messages((SocketIOClient) null, obj, 4, robotInfo.getActionType()));
		}

	}

	public void playDdz(String robotAccount) {
		if (this.checkRobotAccount(robotAccount)) {
			RobotInfo robotInfo = (RobotInfo) robots.get(robotAccount);
			JSONObject obj = new JSONObject();
			obj.put("room_no", robotInfo.getPlayRoomNo());
			obj.put("account", robotAccount);
			if (robotInfo.getActionType() == 11 || robotInfo.getActionType() == 12) {
				if (robotInfo.getActionType() == 11) {
					obj.put("type", 1);
				} else {
					obj.put("type", 2);
				}

				obj.put("isChoice", this.obtainRobOrNot(robotInfo.getPlayRoomNo(), robotAccount));
				robotInfo.setActionType(2);
			}

			if (robotInfo.getActionType() == 14) {
				obj.put("type", 0);
			}

			if (robotInfo.getActionType() == 3) {
				List<String> allCard = this.obtainRobotCardDdz(robotInfo.getPlayRoomNo(), robotAccount);
				obj.put("paiList", allCard);
				if (allCard.size() > 0) {
					obj.put("type", 1);
				} else {
					obj.put("type", 2);
				}
			}

			if (robotInfo.getOutTimes() < 0 && robotInfo.getActionType() == 1) {
				robotInfo.setActionType(7);
			}

			this.producerService.sendMessage(this.ddzQueueDestination,
					new Messages((SocketIOClient) null, obj, 2, robotInfo.getActionType()));
		}

	}

	private int obtainRobOrNot(String roomNo, String robotAccount) {
		DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
		List<String> robotPai = ((UserPacketDdz) room.getUserPacketMap().get(robotAccount)).getMyPai();
		DdzCore.sortCard(robotPai);
		if (DdzCore.obtainCardValue((String) robotPai.get(robotPai.size() - 4)) > 13) {
			return 1;
		} else {
			List<List<String>> bombList = DdzCore.obtainRepeatList(robotPai, 4, false);
			return bombList.size() > 0 ? 1 : 0;
		}
	}

	private List<String> obtainRobotCardDdz(String roomNo, String robotAccount) {
		DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
		List<String> lastCard = room.getLastCard();
		if (room.getLastCard().size() == 0 || robotAccount.equals(room.getLastOperateAccount())) {
			lastCard.clear();
		}

		List<String> robotPai = ((UserPacketDdz) room.getUserPacketMap().get(robotAccount)).getMyPai();
		if (DdzCore.checkCard(lastCard, robotPai) && DdzCore.obtainCardType(robotPai) != 7
				&& DdzCore.obtainCardType(robotPai) != 8) {
			return robotPai;
		} else {
			List<List<String>> allCard = DdzCore.obtainRobotCard(lastCard, robotPai);
			if (!this.isTeammateWithNext(roomNo, robotAccount)) {
				String nextAccount = this.getNextAccount(roomNo, robotAccount);
				if (allCard.size() > 0
						&& ((UserPacketDdz) room.getUserPacketMap().get(nextAccount)).getMyPai().size() == 1) {
					List<List<String>> notSingleList = this.getNotSingleList(allCard);
					return (List) (notSingleList.size() > 0 ? (List) notSingleList.get(0) : new ArrayList());
				}
			}

			if (this.isTeammateWithLast(roomNo, room.getLastOperateAccount(), robotAccount)) {
				if (DdzCore.obtainCardType(lastCard) != 1 && DdzCore.obtainCardType(lastCard) != 2) {
					return new ArrayList();
				}

				if (lastCard.size() > 0 && DdzCore.obtainCardValue((String) lastCard.get(0)) > 12) {
					return new ArrayList();
				}

				if (DdzCore.checkCard(lastCard,
						((UserPacketDdz) room.getUserPacketMap().get(room.getLastOperateAccount())).getMyPai())) {
					return new ArrayList();
				}
			}

			if (lastCard.size() > 0 && allCard.size() > 0 && DdzCore.obtainCardType((List) allCard.get(0)) == 6
					&& ((UserPacketDdz) room.getUserPacketMap().get(room.getLastOperateAccount())).getMyPai()
							.size() > 10) {
				return new ArrayList();
			} else if (allCard.size() > 0) {
				return DdzCore.obtainCardType((List) allCard.get(0)) == 9 ? (List) allCard.get(0)
						: this.obtainMinRobotCardDdz(allCard, lastCard);
			} else {
				return new ArrayList();
			}
		}
	}

	private List<List<String>> getNotSingleList(List<List<String>> allCard) {
		List<List<String>> notSingleList = new ArrayList();
		List<List<String>> singleList = new ArrayList();
		Iterator var4 = allCard.iterator();

		while (var4.hasNext()) {
			List<String> cardList = (List) var4.next();
			if (DdzCore.obtainCardType(cardList) != 1) {
				notSingleList.add(cardList);
			} else {
				singleList.add(cardList);
			}
		}

		Collections.sort(singleList, new Comparator<List<String>>() {
			public int compare(List<String> o1, List<String> o2) {
				return RobotEventDeal.this.getMaxValue(o2) - RobotEventDeal.this.getMaxValue(o1);
			}
		});
		notSingleList.addAll(singleList);
		return notSingleList;
	}

	private List<String> obtainMinRobotCardDdz(List<List<String>> allCard, List<String> lastCard) {
		List<String> bestList = new ArrayList();

		for (int i = 0; i < allCard.size(); ++i) {
			if (this.getMinValue((List) allCard.get(i)) < this.getMinValue((List) bestList) && (lastCard.size() == 0
					|| DdzCore.obtainCardType(lastCard) == DdzCore.obtainCardType((List) allCard.get(i)))) {
				bestList = (List) allCard.get(i);
			}
		}

		return (List) bestList;
	}

	private int getMinValue(List<String> cardList) {
		int minValue = 18;
		int cardType = DdzCore.obtainCardType(cardList);
		if (cardType == 4 || cardType == 5) {
			cardList = (List) DdzCore.obtainRepeatList(cardList, 3, false).get(0);
		}

		Iterator var4 = cardList.iterator();

		while (var4.hasNext()) {
			String card = (String) var4.next();
			int cardValue = DdzCore.obtainCardValue(card);
			if (cardValue < minValue) {
				minValue = cardValue;
			}
		}

		return minValue;
	}

	private int getMaxValue(List<String> cardList) {
		int maxValue = 0;
		int cardType = DdzCore.obtainCardType(cardList);
		if (cardType == 4 || cardType == 5) {
			cardList = (List) DdzCore.obtainRepeatList(cardList, 3, false).get(0);
		}

		Iterator var4 = cardList.iterator();

		while (var4.hasNext()) {
			String card = (String) var4.next();
			int cardValue = DdzCore.obtainCardValue(card);
			if (cardValue > maxValue) {
				maxValue = cardValue;
			}
		}

		return maxValue;
	}

	private boolean isTeammateWithLast(String roomNo, String lastAccount, String account) {
		DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
		return !Dto.stringIsNULL(lastAccount) && !Dto.stringIsNULL(account) && !lastAccount.equals(account)
				&& !lastAccount.equals(room.getLandlordAccount()) && !account.equals(room.getLandlordAccount());
	}

	private boolean isTeammateWithNext(String roomNo, String account) {
		DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (account.equals(room.getLandlordAccount())) {
			return false;
		} else {
			return !room.getLandlordAccount().equals(this.getNextAccount(roomNo, account));
		}
	}

	private String getNextAccount(String roomNo, String account) {
		DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
		int playerIndex = ((Playerinfo) room.getPlayerMap().get(account)).getMyIndex();
		++playerIndex;
		Iterator var5 = room.getPlayerMap().keySet().iterator();

		String next;
		do {
			if (!var5.hasNext()) {
				return null;
			}

			next = (String) var5.next();
		} while (((Playerinfo) room.getPlayerMap().get(next)).getMyIndex() != playerIndex % 3);

		return next;
	}

	public void playQzMj(String robotAccount) {
		if (this.checkRobotAccount(robotAccount)) {
			RobotInfo robotInfo = (RobotInfo) robots.get(robotAccount);
			JSONObject obj = new JSONObject();
			obj.put("room_no", robotInfo.getPlayRoomNo());
			obj.put("account", robotAccount);
			if (robotInfo.getActionType() == 9999) {
				this.doRobotJoin(robotAccount);
				return;
			}
			if (robotInfo.getActionType() == 1) {
				if (robotInfo.getOutTimes() < 0) {
					robotInfo.setActionType(6);
				} else {
					obj.put("type", 2);
				}
			}

			if (robotInfo.getActionType() == 9) {
				obj.put("type", 1);
				robotInfo.setActionType(3);
			} else if (robotInfo.getActionType() == 10) {
				obj.put("type", 11);
				robotInfo.setActionType(3);
			} else if (robotInfo.getActionType() == 8) {
				obj.put("type", 7);
				robotInfo.setActionType(3);
			}

			if (robotInfo.getActionType() == 2) {
				obj.put("pai", this.obtainRobotChuQzMj(robotInfo.getPlayRoomNo(), robotAccount));
			}

			this.producerService.sendMessage(this.qzmjQueueDestination,
					new Messages((SocketIOClient) null, obj, 3, robotInfo.getActionType()));
		}

	}

	private int obtainRobotChuQzMj(String roomNo, String robotAccount) {
		QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);

		try {
			JSONObject special = new JSONObject();
			special.put("mj_count", 34);
			List<Integer> indexList = MaJiangAI
					.getMaJiangIndex(((QZMJUserPacket) room.getUserPacketMap().get(robotAccount)).getMyPai(), special);
			int[] paiIndex = new int[indexList.size()];

			int jin;
			for (jin = 0; jin < indexList.size(); ++jin) {
				paiIndex[jin] = (Integer) indexList.get(jin);
			}

			jin = -1;

			int i;
			for (i = 0; i < QZMJConstant.ALL_CAN_HU_PAI.length; ++i) {
				if (room.getJin() == QZMJConstant.ALL_CAN_HU_PAI[i]) {
					jin = i;
					break;
				}
			}

			i = MaJiangAI.getRobotChupai(paiIndex, special, jin);
			return QZMJConstant.ALL_CAN_HU_PAI[i];
		} catch (Exception var9) {
			logger.error("", var9);
			return (Integer) ((QZMJUserPacket) room.getUserPacketMap().get(robotAccount)).getMyPai().get(0);
		}
	}

	public boolean checkRobotAccount(String robotAccount) {
		return !Dto.stringIsNULL(robotAccount) && robots.containsKey(robotAccount) && robots.get(robotAccount) != null;
	}

	public void addRobotInfo(String account, String roomNo, int gameId) {
		RobotInfo robotInfo = new RobotInfo();
		robotInfo.setRobotAccount(account);
		robotInfo.setPlayRoomNo(roomNo);
		robotInfo.setPlayGameId(gameId);
		robotInfo.setOutTimes(10);
		robots.put(account, robotInfo);
	}
	public boolean isRobot(String account) {
		if(Dto.isNull(robots.get(account))) {
			return false;
		}else {
			return true;
		}
	}
}
