
package com.zhuoan.biz.model.nn;

import com.zhuoan.biz.core.nn.NNPacker;
import com.zhuoan.biz.core.nn.NNUserPacket;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.util.Dto;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class NNGameRoomNew extends GameRoom implements Serializable {
	private int bankerType;
	public Map<Integer, Integer> ratio = this.initRatio();
	private JSONArray baseNum = new JSONArray();
	public JSONArray qzTimes = new JSONArray();
	private List<Integer> specialType = new ArrayList();
	private int qzNoBanker;
	private int sjBanker;
	private ConcurrentMap<String, NNUserPacket> userPacketMap = new ConcurrentHashMap();
	private NNPacker[] pai;
	private int tongSha = 0;
	private List<String> NNUser;
	private int tuiZhuTimes;
	private String feature;

	public String getFeature() {
		return feature;
	}

	public void setFeature(String feature) {
		this.feature = feature;
	}

	public JSONArray getQzTimes() {
		return qzTimes;
	}

	public NNGameRoomNew() {
	}

	public int getTongSha() {
		return this.tongSha;
	}

	public void setTongSha(int tongSha) {
		this.tongSha = tongSha;
	}

	public NNPacker[] getPai() {
		return this.pai;
	}

	public void setPai(NNPacker[] pai) {
		this.pai = pai;
	}

	public int getSjBanker() {
		return this.sjBanker;
	}

	public void setSjBanker(int sjBanker) {
		this.sjBanker = sjBanker;
	}

	public int getBankerType() {
		return this.bankerType;
	}

	public void setBankerType(int bankerType) {
		this.bankerType = bankerType;
	}

	public Map<Integer, Integer> getRatio() {
		return this.ratio;
	}

	public void setRatio(Map<Integer, Integer> ratio) {
		this.ratio = ratio;
	}

	public JSONArray getBaseNum() {
		return this.baseNum;
	}

	public void setBaseNum(JSONArray baseNum) {
		this.baseNum = baseNum;
	}

	public void setQzTimes(JSONArray qzTimes) {
		this.qzTimes = qzTimes;
	}

	public List<Integer> getSpecialType() {
		return this.specialType;
	}

	public void setSpecialType(List<Integer> specialType) {
		this.specialType = specialType;
	}

	public int getQzNoBanker() {
		return this.qzNoBanker;
	}

	public void setQzNoBanker(int qzNoBanker) {
		this.qzNoBanker = qzNoBanker;
	}

	public ConcurrentMap<String, NNUserPacket> getUserPacketMap() {
		return this.userPacketMap;
	}

	public void setUserPacketMap(ConcurrentMap<String, NNUserPacket> userPacketMap) {
		this.userPacketMap = userPacketMap;
	}

	public List<String> getNNUser() {
		return this.NNUser;
	}

	public void setNNUser(List<String> NNUser) {
		this.NNUser = NNUser;
	}

	public int getTuiZhuTimes() {
		return this.tuiZhuTimes;
	}

	public void setTuiZhuTimes(int tuiZhuTimes) {
		this.tuiZhuTimes = tuiZhuTimes;
	}

	private Map<Integer, Integer> initRatio() {
		Map<Integer, Integer> ratio = new HashMap();
		ratio.put(0, 1);
		ratio.put(1, 1);
		ratio.put(2, 2);
		ratio.put(3, 3);
		ratio.put(4, 4);
		ratio.put(5, 5);
		ratio.put(6, 6);
		ratio.put(7, 7);
		ratio.put(8, 8);
		ratio.put(9, 9);
		ratio.put(10, 10);
		return ratio;
	}

	public void initGame() {
		this.getGameProcess().clear();
		this.tongSha = 0;
		Iterator var1 = this.getUserPacketMap().keySet().iterator();

		String account;
		while (var1.hasNext()) {
			account = (String) var1.next();
			if (this.userPacketMap.containsKey(account) && this.userPacketMap.get(account) != null) {
				((NNUserPacket) this.userPacketMap.get(account)).initUserPacket();
				((Playerinfo) this.getPlayerMap().get(account)).addPlayTimes();
			}
		}

		this.addGameIndex();
		this.addGameNewIndex();
		this.setIsClose(0);
		if (this.bankerType == 0 && (!this.getUserPacketMap().containsKey(this.getBanker())
				|| this.getUserPacketMap().get(this.getBanker()) == null)) {
			var1 = this.getUserPacketMap().keySet().iterator();

			while (var1.hasNext()) {
				account = (String) var1.next();
				if (this.getUserPacketMap().containsKey(account) && this.getUserPacketMap().get(account) != null) {
					this.setBanker(account);
					break;
				}
			}
		}

		this.getGameProcess().put("bankerType", this.bankerType);
	}

	public JSONArray getAllPlayer() {
		JSONArray array = new JSONArray();
		Iterator var2 = this.getPlayerMap().keySet().iterator();

		while (var2.hasNext()) {
			String uuid = (String) var2.next();
			Playerinfo player = (Playerinfo) this.getPlayerMap().get(uuid);
			ConcurrentMap<String, NNUserPacket> map = getUserPacketMap();
			if (player != null) {
				NNUserPacket up = (NNUserPacket) this.userPacketMap.get(uuid);
				JSONObject obj = new JSONObject();
				obj.put("account", player.getAccount());
				obj.put("name", player.getName());
				obj.put("headimg", player.getRealHeadimg());
				obj.put("sex", player.getSex());
				obj.put("ip", player.getIp());
				obj.put("vip", player.getVip());
				obj.put("location", player.getLocation());
				obj.put("area", player.getArea());
				obj.put("score", player.getScore());
				obj.put("nowScore", map.get(player.getAccount()).getNowScore());
				obj.put("index", player.getMyIndex());
				obj.put("userOnlineStatus", player.getStatus());
				obj.put("ghName", player.getGhName());
				obj.put("introduction", player.getSignature());
				obj.put("userStatus", up.getStatus());
				obj.put("isTuiZhu", map.get(player.getAccount()).isTuiZhu());
				obj.put("isPoChan",player.isCollapse());
				array.add(obj);
			}
		}

		return array;
	}

	public boolean isAllReady() {
		Iterator var1 = this.userPacketMap.keySet().iterator();

		String account;
		do {
			if (!var1.hasNext()) {
				return true;
			}

			account = (String) var1.next();
		} while (!this.userPacketMap.containsKey(account) || this.userPacketMap.get(account) == null
				|| ((NNUserPacket) this.userPacketMap.get(account)).getStatus() == 1);

		return false;
	}

	public boolean isAllQZ() {
		Iterator var1 = this.userPacketMap.keySet().iterator();

		String account;
		do {
			if (!var1.hasNext()) {
				return true;
			}

			account = (String) var1.next();
		} while (!this.userPacketMap.containsKey(account) || this.userPacketMap.get(account) == null
				|| ((NNUserPacket) this.userPacketMap.get(account)).getStatus() == 0
				|| ((NNUserPacket) this.userPacketMap.get(account)).getStatus() == 2);

		return false;
	}

	public boolean isAllXiaZhu() {
		Iterator var1 = this.userPacketMap.keySet().iterator();

		String account;
		do {
			if (!var1.hasNext()) {
				return true;
			}

			account = (String) var1.next();
		} while (!this.userPacketMap.containsKey(account) || this.userPacketMap.get(account) == null
				|| account.equals(this.getBanker()) || ((NNUserPacket) this.userPacketMap.get(account)).getStatus() == 0
				|| ((NNUserPacket) this.userPacketMap.get(account)).getStatus() == 4);

		return false;
	}

	public boolean isAllShowPai() {
		Iterator var1 = this.userPacketMap.keySet().iterator();

		String account;
		do {
			if (!var1.hasNext()) {
				return true;
			}

			account = (String) var1.next();
		} while (!this.userPacketMap.containsKey(account) || this.userPacketMap.get(account) == null
				|| ((NNUserPacket) this.userPacketMap.get(account)).getStatus() == 0
				|| ((NNUserPacket) this.userPacketMap.get(account)).getStatus() == 5);

		return false;
	}

	public boolean isAgreeClose() {
		Iterator var1 = this.userPacketMap.keySet().iterator();

		String account;
		do {
			if (!var1.hasNext()) {
				return true;
			}

			account = (String) var1.next();
		} while (!this.userPacketMap.containsKey(account) || this.userPacketMap.get(account) == null
				|| ((Playerinfo) this.getPlayerMap().get(account)).getIsCloseRoom() == 1);

		return false;
	}

	public JSONArray getQzTimes(double yuanbao) {
		int baseNum = 3;
		int playerCount = this.getUserPacketMap().size();
		int maxVal = 0;
		JSONArray array = this.getBaseNum();

		int beiShu;
		for (beiShu = 0; beiShu < array.size(); ++beiShu) {
			int val = array.getJSONObject(beiShu).getInt("val");
			if (val > maxVal) {
				maxVal = val;
			}
		}

		beiShu = (int) (yuanbao / (double) (baseNum * this.getScore() * (playerCount - 1) * maxVal));
		JSONArray qzts = new JSONArray();

		for (int i = 0; i < this.qzTimes.size(); ++i) {
			JSONObject obj = new JSONObject();
			int val = this.qzTimes.getInt(i);
			obj.put("name", val + "倍");
			obj.put("val", val);
			if (beiShu < val && this.getRoomType() != 0 && this.getRoomType() != 2 && this.getRoomType() != 4
					&& this.getRoomType() != 6) {
				obj.put("isuse", 0);
			} else {
				obj.put("isuse", 1);
			}

			qzts.add(obj);
		}

		return qzts;
	}

	public JSONArray getBaseNumTimes(String account) {
		double yuanbao = ((Playerinfo) this.getPlayerMap().get(account)).getScore();
		int baseNum = 5;
		double di = (double) this.getScore();
		int qzTimes = 1;
		if ((this.bankerType == 2 || this.bankerType == 3) && !Dto.isNull(this.userPacketMap)
				&& this.userPacketMap.get(this.getBanker()) != null) {
			qzTimes = ((NNUserPacket) this.userPacketMap.get(this.getBanker())).getQzTimes();
		}

		int beiShu = (int) (yuanbao / ((double) baseNum * di * (double) qzTimes));
		JSONArray baseNums = new JSONArray();
		JSONArray array = JSONArray.fromObject(this.getBaseNum());
		System.out.println(account + "检测是否有推注");
		System.out.println("房间推注倍数：" + this.tuiZhuTimes);
		System.out.println("上局下注倍数：" + ((NNUserPacket) this.getUserPacketMap().get(account)).getLastXzTimes());
		if (this.tuiZhuTimes > 0 && ((NNUserPacket) this.getUserPacketMap().get(account)).getLastXzTimes() > 0) {
			JSONObject tzObj = new JSONObject();
			tzObj.put("name", "推注");/*
									 * + this.tuiZhuTimes *
									 * ((NNUserPacket)this.getUserPacketMap().get(account)).getLastXzTimes())
									 */
			tzObj.put("val", this.tuiZhuTimes);
			array.add(tzObj);
		}

		for (int i = 0; i < array.size(); ++i) {
			if (array.getJSONObject(i).containsKey("name")) {
				int val = array.getJSONObject(i).getInt("val");
				String name = array.getJSONObject(i).getString("name");
				JSONObject obj = new JSONObject();
				obj.put("name", name);
				obj.put("val", val);
				if (beiShu < val && this.getRoomType() != 0 && this.getRoomType() != 2 && this.getRoomType() != 4
						&& this.getRoomType() != 6) {
					obj.put("isuse", 0);
				} else {
					obj.put("isuse", 1);
				}

				baseNums.add(obj);
			}
		}

		return JSONArray.fromObject(baseNums);
	}

	public JSONArray getQZResult() {
		JSONArray array = new JSONArray();
		Iterator var2 = this.userPacketMap.keySet().iterator();

		while (var2.hasNext()) {
			String uuid = (String) var2.next();
			if (this.userPacketMap.containsKey(uuid) && this.userPacketMap.get(uuid) != null) {
				NNUserPacket up = (NNUserPacket) this.userPacketMap.get(uuid);
				if (up.getStatus() == 2) {
					JSONObject obj = new JSONObject();
					obj.put("index", ((Playerinfo) this.getPlayerMap().get(uuid)).getMyIndex());
					obj.put("value", up.getQzTimes());
					array.add(obj);
				}
			}
		}

		return array;
	}

	public JSONArray getXiaZhuResult() {
		JSONArray array = new JSONArray();
		Iterator var2 = this.userPacketMap.keySet().iterator();

		while (var2.hasNext()) {
			String uuid = (String) var2.next();
			if (this.userPacketMap.containsKey(uuid) && this.userPacketMap.get(uuid) != null) {
				NNUserPacket up = (NNUserPacket) this.userPacketMap.get(uuid);
				if (up.getStatus() >= 4) {
					JSONObject obj = new JSONObject();
					obj.put("index", ((Playerinfo) this.getPlayerMap().get(uuid)).getMyIndex());
					obj.put("value", up.getXzTimes());
					obj.put("name", up.getXzTimes() + "倍");
					obj.put("isTui", up.getXzTimes() == this.tuiZhuTimes ? "1" : "0");
					array.add(obj);
				}
			}
		}

		return array;
	}

	public JSONObject getGameData(String account) {
		if (this.getBankerType() == 30) {
			return this.getGameDataLP(account);
		}
		if (this.getGameStatus() != 2 && this.getGameStatus() != 3 && this.getGameStatus() != 4) {
			if (this.getGameStatus() == 5) {
				return this.getGameDataLP(account);
			} else {
				return this.getGameStatus() == 6 ? this.getGameDataJS(account) : null;
			}
		} else {
			return this.getGameDataQzOrXz(account);
		}
	}

	public JSONObject getGameData2(String account) {
		JSONObject data = new JSONObject();
		JSONObject obj = new JSONObject();
		int[] mypai = userPacketMap.get(account).getMyPai();
		obj.put("pai", mypai);
		data.put(((Playerinfo) this.getPlayerMap().get(account)).getMyIndex(), obj);
		return data;

	}

	public JSONObject getGameDataQzOrXz(String account) {
		JSONObject data = new JSONObject();
		Iterator var3 = this.userPacketMap.keySet().iterator();

		while (true) {
			String uuid;
			do {
				do {
					if (!var3.hasNext()) {
						return data;
					}

					uuid = (String) var3.next();
				} while (!this.userPacketMap.containsKey(uuid));
			} while (this.userPacketMap.get(uuid) == null);

			JSONObject obj = new JSONObject();
			int[] pai;
			if (this.bankerType != 3) {
				pai = new int[0];
			} else if (((NNUserPacket) this.userPacketMap.get(uuid)).getStatus() <= 0) {
				pai = new int[0];
			} else if (!uuid.equals(account)) {
				pai = new int[] { 0, 0, 0, 0 };
			} else {
				int[] myPai = ((NNUserPacket) this.userPacketMap.get(uuid)).getMingPai();
				pai = new int[myPai.length - 1];

				for (int i = 0; i < pai.length; ++i) {
					pai[i] = myPai[i];
				}

				obj.put("paiType", ((NNUserPacket) this.userPacketMap.get(uuid)).getType());
			}

			obj.put("pai", pai);
			data.put(((Playerinfo) this.getPlayerMap().get(uuid)).getMyIndex(), obj);
		}
	}

	public JSONObject getGameDataLP(String account) {
		JSONObject data = new JSONObject();
		Iterator var3 = this.userPacketMap.keySet().iterator();

		while (true) {
			String uuid;
			do {
				do {
					if (!var3.hasNext()) {
						return data;
					}

					uuid = (String) var3.next();
				} while (!this.userPacketMap.containsKey(uuid));
			} while (this.userPacketMap.get(uuid) == null);

			JSONObject obj = new JSONObject();
			int[] pai;
			if (((NNUserPacket) this.userPacketMap.get(uuid)).getStatus() > 0) {
				if (!uuid.equals(account) && ((NNUserPacket) this.userPacketMap.get(uuid)).getStatus() != 5) {
					pai = new int[] { 0, 0, 0, 0, 0 };
				} else {
					if (this.getBankerType() == 3) {
						pai = ((NNUserPacket) this.userPacketMap.get(uuid)).getMingPai();
					} else {
						pai = ((NNUserPacket) this.userPacketMap.get(uuid)).getMyPai();
						List<Integer> p = new ArrayList();

						int i;
						for (i = 0; i < pai.length; ++i) {
							p.add(pai[i]);
						}

						Collections.shuffle(p);

						for (i = 0; i < pai.length; ++i) {
							pai[i] = (Integer) p.get(i);
						}
					}

					if (((NNUserPacket) this.userPacketMap.get(uuid)).getStatus() == 5) {
						pai = ((NNUserPacket) this.userPacketMap.get(uuid)).getSortPai();
						obj.put("paiType", ((NNUserPacket) this.userPacketMap.get(uuid)).getType());
					}
				}
			} else {
				pai = new int[0];
			}

			obj.put("pai", pai);
			data.put(((Playerinfo) this.getPlayerMap().get(uuid)).getMyIndex(), obj);

		}
	}

	public JSONObject getReconnectLpMsg() {
		JSONObject obj = new JSONObject();
		for (String account : this.userPacketMap.keySet()) {
			JSONObject userLpMsg = new JSONObject();
			boolean isLp = this.userPacketMap.get(account).isLP();
			if (isLp) {
				int index = this.getPlayerMap().get(account).getMyIndex();
				int[] pai = ((NNUserPacket) this.userPacketMap.get(account)).getSortPai();
				int paiType = ((NNUserPacket) this.userPacketMap.get(account)).getType();
				userLpMsg.put("isLp", isLp);
				userLpMsg.put("pai", pai);
				userLpMsg.put("paiType", paiType);
				userLpMsg.put("account", account);
				obj.put(index, userLpMsg);
			}

		}
		return obj;
	}

	public JSONObject getGameDataJS(String account) {
		JSONObject data = new JSONObject();
		Iterator var3 = this.userPacketMap.keySet().iterator();

		while (var3.hasNext()) {
			String uuid = (String) var3.next();
			if (this.userPacketMap.containsKey(uuid) && this.userPacketMap.get(uuid) != null) {
				JSONObject obj = new JSONObject();
				obj.put("tongsha", this.tongSha);
				int[] pai;
				if (((NNUserPacket) this.userPacketMap.get(uuid)).getStatus() > 0) {
					pai = ((NNUserPacket) this.userPacketMap.get(uuid)).getSortPai();
					obj.put("paiType", ((NNUserPacket) this.userPacketMap.get(uuid)).getType());
					obj.put("sum", ((NNUserPacket) this.userPacketMap.get(uuid)).getScore());
					obj.put("scoreLeft", ((Playerinfo) this.getPlayerMap().get(uuid)).getScore());
				} else {
					pai = new int[0];
				}

				obj.put("pai", pai);
				data.put(((Playerinfo) this.getPlayerMap().get(uuid)).getMyIndex(), obj);
			}
		}

		return data;
	}

	public JSONArray getFinalSummary() {
		if (this.getFinalSummaryData().size() > 0) {
			return this.getFinalSummaryData();
		} else {
			JSONArray array = new JSONArray();
			ArrayList<Double> score = new ArrayList();
			double circleMax = 0.0D;
			Iterator var5 = this.userPacketMap.keySet().iterator();

			String account;
			while (var5.hasNext()) {
				account = (String) var5.next();
				score.add(((Playerinfo) this.getPlayerMap().get(account)).getScore());
				if (9 == this.getRoomType()) {
					double sub = Dto.sub(((Playerinfo) this.getPlayerMap().get(account)).getScore(),
							((Playerinfo) this.getPlayerMap().get(account)).getSourceScore());
					if (sub > circleMax) {
						circleMax = sub;
					}
				}
			}

			var5 = this.userPacketMap.keySet().iterator();

			while (var5.hasNext()) {
				account = (String) var5.next();
				if (this.userPacketMap.containsKey(account) && this.userPacketMap.get(account) != null) {
					JSONObject obj = new JSONObject();
					obj.put("index", ((Playerinfo) this.getPlayerMap().get(account)).getMyIndex());
					obj.put("name", ((Playerinfo) this.getPlayerMap().get(account)).getName());
					obj.put("account", account);
					obj.put("headimg", ((Playerinfo) this.getPlayerMap().get(account)).getRealHeadimg());
					obj.put("isFangzhu", 0);
					if (account.equals(this.getOwner())) {
						obj.put("isFangzhu", 1);
					}

					obj.put("isWinner", 0);
					if (9 == this.getRoomType()) {
						double sub = Dto.sub(((Playerinfo) this.getPlayerMap().get(account)).getScore(),
								((Playerinfo) this.getPlayerMap().get(account)).getSourceScore());
						obj.put("score", sub);
						if (circleMax == sub) {
							obj.put("isWinner", 1);
						}
					} else {
						if (((Playerinfo) this.getPlayerMap().get(account)).getScore() == this.maxScore(score)) {
							obj.put("isWinner", 1);
						}

						obj.put("score", ((Playerinfo) this.getPlayerMap().get(account)).getScore());
					}

					obj.put("tongShaTimes", ((NNUserPacket) this.userPacketMap.get(account)).getTongShaTimes());
					obj.put("tongPeiTimes", ((NNUserPacket) this.userPacketMap.get(account)).getTongPeiTimes());
					obj.put("niuNiuTimes", ((NNUserPacket) this.userPacketMap.get(account)).getNiuNiuTimes());
					obj.put("wuNiuTimes", ((NNUserPacket) this.userPacketMap.get(account)).getWuNiuTimes());
					obj.put("winTimes", ((NNUserPacket) this.userPacketMap.get(account)).getWinTimes());
					obj.put("totalScore", ((NNUserPacket) this.userPacketMap.get(account)).getScore());
					array.add(obj);
				}
			}

			this.setFinalSummaryData(array);
			return array;
		}
	}

	public JSONArray getJieSanData() {
		JSONArray array = new JSONArray();
		Iterator var2 = this.userPacketMap.keySet().iterator();

		while (var2.hasNext()) {
			String account = (String) var2.next();
			if (this.userPacketMap.containsKey(account) && this.userPacketMap.get(account) != null) {
				JSONObject obj = new JSONObject();
				obj.put("index", ((Playerinfo) this.getPlayerMap().get(account)).getMyIndex());
				obj.put("name", ((Playerinfo) this.getPlayerMap().get(account)).getName());
				obj.put("result", ((Playerinfo) this.getPlayerMap().get(account)).getIsCloseRoom());
				obj.put("jiesanTimer", this.getJieSanTime());
				array.add(obj);
			}
		}

		return array;
	}

	public int getNowReadyCount() {
		int readyCount = 0;
		Iterator var2 = this.getUserPacketMap().keySet().iterator();

		while (var2.hasNext()) {
			String uuid = (String) var2.next();
			if (this.userPacketMap.containsKey(uuid) && this.userPacketMap.get(uuid) != null
					&& ((NNUserPacket) this.getUserPacketMap().get(uuid)).getStatus() == 1) {
				++readyCount;
			}
		}

		return readyCount;
	}

	public String getNextPlayer(String account) {
		return account;
	}
}
