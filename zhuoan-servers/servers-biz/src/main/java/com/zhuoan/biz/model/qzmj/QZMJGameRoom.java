package com.zhuoan.biz.model.qzmj;

import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.QZMJConstant;
import com.zhuoan.constant.SysGlobalConstant;
import com.zhuoan.util.BaseInfoUtil;
import com.zhuoan.util.DateUtils;
import com.zhuoan.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.RandomUtils;

import static org.junit.Assert.fail;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QZMJGameRoom extends GameRoom implements Serializable {

	private int youJinScore = 1;

	private int paiCount;

	private int bankerIndex;

	private int jin;

	private int bankerTimes = 1;

	private int bankerScore = 1;

	private int yjType;

	private String yjAccount;

	private int pai[];

	private int index;

	private int lastMoPai;

	private String lastMoAccount;

	private int lastPai;

	private String lastAccount;

	private String thisAccount;

	private int thisType;

	private String nextAskAccount;

	private int nextAskType;

	private int askNum;

	private String winner;

	private ConcurrentHashMap<String, QZMJUserPacket> userPacketMap = new ConcurrentHashMap<>();

	private List<KaiJuModel> kaiJuList = new ArrayList<KaiJuModel>();

	private int[] dice;

	private int huType;

	public boolean isGuangYou;

	public int hasJinNoPingHu;

	public boolean isNotChiHu;

	public boolean isGameOver = false;

	private int startStatus;

	private String compensateAccount = null;

	private Map<String, String> compensateMap = new HashMap<>();

	private JSONObject tooNextAskAccount;

	private Object[] chuPaiJieGuo;

	private int keScoreVal = 100;

	private int zhuangScore = 1;

	private int xianScore = 1;

	private int fanMultiple = 1;

	private int playTime = 20;

	private String startTime;

	private int yjRewardScore = 0;

	private int shyRewardScore = 0;

	private int syRewardScore = 0;

	private int ssRewardScore = 0;

	public int getSsRewardScore() {
		return ssRewardScore;
	}

	public void setSsRewardScore(int ssRewardScore) {
		this.ssRewardScore = ssRewardScore;
	}

	public int getYjRewardScore() {
		return yjRewardScore;
	}

	public void setYjRewardScore(int yjRewardScore) {
		this.yjRewardScore = yjRewardScore;
	}

	public int getShyRewardScore() {
		return shyRewardScore;
	}

	public void setShyRewardScore(int shyRewardScore) {
		this.shyRewardScore = shyRewardScore;
	}

	public int getSyRewardScore() {
		return syRewardScore;
	}

	public void setSyRewardScore(int syRewardScore) {
		this.syRewardScore = syRewardScore;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public int getPlayTime() {
		return playTime;
	}

	public void setPlayTime(int playTime) {
		this.playTime = playTime;
	}

	public JSONObject getTooNextAskAccount() {
		return tooNextAskAccount;
	}

	public void setTooNextAskAccount(JSONObject tooNextAskAccount) {
		this.tooNextAskAccount = tooNextAskAccount;
	}

	public String getCompensateAccount() {
		return compensateAccount;
	}

	public void setCompensateAccount(String compensateAccount) {
		this.compensateAccount = compensateAccount;
	}

	public int getStartStatus() {
		return startStatus;
	}

	public void setStartStatus(int startStatus) {
		this.startStatus = startStatus;
	}

	public int getYouJinScore() {
		return youJinScore;
	}

	public int getPaiCount() {
		return paiCount;
	}

	public int getBankerIndex() {
		return bankerIndex;
	}

	public int getJin() {
		return jin;
	}

	public int getBankerTimes() {
		return bankerTimes;
	}

	public int getYjType() {
		return yjType;
	}

	public String getYjAccount() {
		return yjAccount;
	}

	public int[] getPai() {
		return pai;
	}

	public int getIndex() {
		return index;
	}

	public int getLastMoPai() {
		return lastMoPai;
	}

	public String getLastMoAccount() {
		return lastMoAccount;
	}

	public int getLastPai() {
		return lastPai;
	}

	public String getLastAccount() {
		return lastAccount;
	}

	public String getThisAccount() {
		return thisAccount;
	}

	public int getAskNum() {
		return askNum;
	}

	public void setAskNum(int askNum) {
		this.askNum = askNum;
	}

	public int getThisType() {
		return thisType;
	}

	public String getNextAskAccount() {
		return nextAskAccount;
	}

	public int getNextAskType() {
		return nextAskType;
	}

	public String getWinner() {
		return winner;
	}

	public ConcurrentHashMap<String, QZMJUserPacket> getUserPacketMap() {
		return userPacketMap;
	}

	public List<KaiJuModel> getKaiJuList() {
		return kaiJuList;
	}

	public int[] getDice() {
		return dice;
	}

	public void setYouJinScore(int youJinScore) {
		this.youJinScore = youJinScore;
	}

	public void setPaiCount(int paiCount) {
		this.paiCount = paiCount;
	}

	public void setBankerIndex(int bankerIndex) {
		this.bankerIndex = bankerIndex;
	}

	public void setJin(int jin) {
		this.jin = jin;
	}

	public void setBankerTimes(int bankerTimes) {
		this.bankerTimes = bankerTimes;
	}

	public void setYjType(int yjType) {
		this.yjType = yjType;
	}

	public void setYjAccount(String yjAccount) {
		this.yjAccount = yjAccount;
	}

	public void setPai(int[] pai) {
		this.pai = pai;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public void setLastMoPai(int lastMoPai) {
		this.lastMoPai = lastMoPai;
	}

	public void setLastMoAccount(String lastMoAccount) {
		this.lastMoAccount = lastMoAccount;
	}

	public void setLastPai(int lastPai) {
		this.lastPai = lastPai;
	}

	public void setLastAccount(String lastAccount) {
		this.lastAccount = lastAccount;
	}

	public void setThisAccount(String thisAccount) {
		this.thisAccount = thisAccount;
	}

	public void setThisType(int thisType) {
		this.thisType = thisType;
	}

	public void setNextAskAccount(String nextAskAccount) {
		this.nextAskAccount = nextAskAccount;
	}

	public void setNextAskType(int nextAskType) {
		this.nextAskType = nextAskType;
	}

	public void setWinner(String winner) {
		this.winner = winner;
	}

	public void setUserPacketMap(ConcurrentHashMap<String, QZMJUserPacket> userPackerMap) {
		this.userPacketMap = userPackerMap;
	}

	public boolean isGuangYou() {
		return isGuangYou;
	}

	public void setGuangYou(boolean guangYou) {
		isGuangYou = guangYou;
	}

	public int getHasJinNoPingHu() {
		return hasJinNoPingHu;
	}

	public void setHasJinNoPingHu(int hasJinNoPingHu) {
		this.hasJinNoPingHu = hasJinNoPingHu;
	}

	public int getBankerScore() {
		return bankerScore;
	}

	public void setBankerScore(int bankerScore) {
		this.bankerScore = bankerScore;
	}

	public boolean isNotChiHu() {
		return isNotChiHu;
	}

	public void setNotChiHu(boolean notChiHu) {
		isNotChiHu = notChiHu;
	}

	public boolean isGameOver() {
		return isGameOver;
	}

	public int getZhuangScore() {
		return zhuangScore;
	}

	public void setZhuangScore(int zhuangScore) {
		this.zhuangScore = zhuangScore;
	}

	public int getXianScore() {
		return xianScore;
	}

	public void setXianScore(int xianScore) {
		this.xianScore = xianScore;
	}

	public void setGameOver(boolean gameOver) {
		isGameOver = gameOver;
	}

	public void setKaiJuList(List<KaiJuModel> kaiJuList) {
		this.kaiJuList = kaiJuList;
	}

	public void setDice(int[] dice) {
		this.dice = dice;
	}

	public int getHuType() {
		return huType;
	}

	public void setHuType(int huType) {
		this.huType = huType;
	}

	public Map<String, String> getCompensateMap() {
		return compensateMap;
	}

	public void setCompensateMap(Map<String, String> compensateMap) {
		this.compensateMap = compensateMap;
	}

	public Object[] getChuPaiJieGuo() {
		return chuPaiJieGuo;
	}

	public void setChuPaiJieGuo(Object[] chuPaiJieGuo) {
		this.chuPaiJieGuo = chuPaiJieGuo;
	}

	public int getKeScoreVal() {
		return keScoreVal;
	}

	public void setKeScoreVal(int keScoreVal) {
		this.keScoreVal = keScoreVal;
	}

	public int getFanMultiple() {
		return fanMultiple;
	}

	public void setFanMultiple(int fanMultiple) {
		this.fanMultiple = fanMultiple;
	}

	public void initGame() {
		System.out.println("执行方法：initGame");

		// 游金类型 1：单游 2：双游 3：三游
		this.yjType = 0;
		// 游金玩家
		this.yjAccount = null;
		// 上个操作玩家
		this.lastAccount = null;
		this.compensateAccount = null;
		this.compensateMap.clear();
		// 胡牌类型
		this.huType = 0;
		this.startStatus = 0;
		this.kaiJuList = new ArrayList<>();
		this.lastMoPai = 0;
		this.chuPaiJieGuo = null;
		// 游戏开始时间
		this.startTime = DateUtils.getTodayTime();
		getSummaryData().clear();
		addGameIndex();
		addGameNewIndex();
		setIsClose(CommonConstant.CLOSE_ROOM_TYPE_INIT);
		for (String uuid : getUserPacketMap().keySet()) {
			if (getUserPacketMap().containsKey(uuid) && getUserPacketMap().get(uuid) != null) {
				getUserPacketMap().get(uuid).initUserPacket();
				getPlayerMap().get(uuid).addPlayTimes();// 添加游戏局数
			}
		}
		this.askNum = 0;// 询问次数
	}

	public void andAskNum() {
		int askNum = this.askNum;
		if (askNum < 0)
			askNum = 0;
		askNum++;
		this.askNum = askNum;
	}

	public boolean choiceBanker() {
		/*
		 * 1.庄家在房间内取庄家 2.房主在房间内取房主 3.庄家房主均不在随机庄家 4.设置开局询问
		 */
		String bankerAccount = null;
		if (checkAccount(getBanker())) {
			bankerAccount = getBanker();
		} else if (checkAccount(getOwner())) {
			bankerAccount = getOwner();
		} else if (getPlayerMap().size() > 0) {
			for (String account : getPlayerMap().keySet()) {
				if (getPlayerMap().containsKey(account) && getPlayerMap().get(account) != null) {
					bankerAccount = account;
					break;
				}
			}
		}
		if (!Dto.stringIsNULL(bankerAccount)) {
			setBanker(bankerAccount);
			thisType = 1;
			thisAccount = bankerAccount;
			return true;
		}
		return false;
	}

	public void obtainDice() {
		int[] dices = new int[2];
		dices[0] = RandomUtils.nextInt(6) + 1;
		dices[1] = RandomUtils.nextInt(6) + 1;
		setDice(dices);
	}

	public void shufflePai() {
		/*
		 * 1.打乱牌下标,设置牌组 2.设置下一张牌的下标
		 */
		int[] indexs = randomPai(QZMJConstant.ALL_PAI.length);
		int[] newPais = new int[QZMJConstant.ALL_PAI.length];

		for (int i = 0; i < indexs.length; i++) {

			newPais[i] = QZMJConstant.ALL_PAI[indexs[i]];
		}
		setPai(newPais);
		this.index = 0;
	}

	public void choiceJin() {
		/*
		 * 1.从牌堆随机一张金牌 2.设置金牌
		 */
		Random rd = new Random();
		while (true) {
			// 开金选择在不可动的牌堆里开（最后16张）
			int index = QZMJConstant.ALL_PAI.length - rd.nextInt(QZMJConstant.LEFT_PAI_COUNT) - 1;
			// 如果为花牌重新随机
			if (!QZMJConstant.isHuaPai(this.pai[index])) {
				// 设置金牌
				setJin(this.pai[index]);
				break;
			}
		}
	}

	public static Set<String> jinAccount = new HashSet<>();
	public static Set<String> jinjinAccount = new HashSet<>();
	public static void main(String[] args) {
		ConcurrentHashMap<String, QZMJUserPacket> m = new ConcurrentHashMap<>();
		
		m.put("191297", new QZMJUserPacket());
		m.put("12345", new QZMJUserPacket());
		QZMJGameRoom gameRoom = new QZMJGameRoom();
		gameRoom.userPacketMap = m;
		gameRoom.pai = QZMJConstant.ALL_PAI;
		gameRoom.setJin(12);
		jinAccount.add("1191297");
		gameRoom.paiCount = 16;
		gameRoom.faPai();

	}

	public void faPai() {
		int lastIndex = 0;
		/*
		 * 1.设置玩家手牌
		 * 
		 * 
		 * int jinpaiIndex = 0; for (String account : getUserPacketMap().keySet()) { if
		 * (jinAccount.contains(account)) { System.out.println("============="); for
		 * (int i = 0; i < 16; i++) { if (pai[i]==jin ) { if
		 * (getUserPacketMap().containsKey(account) && getUserPacketMap().get(account)
		 * != null) { lastIndex = index + paiCount; System.out.println(pai[0]);
		 * System.out.println(jin); int[] myPai = Arrays.copyOfRange(pai, index,
		 * lastIndex); index = lastIndex;
		 * getUserPacketMap().get(account).setMyPai(myPai); for (int j = 0; j <
		 * myPai.length; j++) { if (myPai[j]==jin) { jinpaiIndex++; } } } break; }else
		 * if(i==15 && pai[i]!=jin) { for (int j = pai.length-1; j >= 0; j--) { if
		 * (pai[j] == jin) { Random r = new Random(); int a = r.nextInt(15)+1; pai[j] =
		 * pai[a]; pai[a] = jin; break; } } if (getUserPacketMap().containsKey(account)
		 * && getUserPacketMap().get(account) != null) { lastIndex = index + paiCount;
		 * int[] myPai = Arrays.copyOfRange(pai, index, lastIndex); index = lastIndex;
		 * getUserPacketMap().get(account).setMyPai(myPai); for (int j = 0; j <
		 * myPai.length; j++) { if (myPai[j]==jin) { jinpaiIndex++; } }
		 * 
		 * } break; } }
		 * 
		 * }
		 * 
		 * }
		 */

		for (String account : getUserPacketMap().keySet()) {
			if (getUserPacketMap().containsKey(account) && getUserPacketMap().get(account) != null) {
				lastIndex = index + paiCount;
				int[] myPai = Arrays.copyOfRange(pai, index, lastIndex);
				if (jinAccount.contains(account) || jinjinAccount.contains(account)) {
					System.out.println("检测到指定玩家！");
					if (!ArrayUtils.contains(myPai, jin)) {
						System.out.println("指定玩家不包含金牌  开始换金");
						int aaa = 0;
						for (int j = pai.length - 1; j >= 0; j--) {
							if (pai[j] == jin && aaa == 0) {
								aaa++;
								continue;
							}
							if (pai[j] == jin && aaa == 1 && j > 32) {
								System.out.println("替换的牌的下标为："+j);
								System.out.println("替换之前的牌组为：");
								for (int i = 0; i < pai.length; i++) {
									System.out.print(pai[i]+ " ");
								}
								int qq = myPai[0];
								myPai[0] = pai[j];
								pai[j] = qq;
								System.out.println("替换之后的牌组为：");
								for (int i = 0; i < pai.length; i++) {
									System.out.print(pai[i]+ " ");
								}
								break;
							}
						}
						aaa = 0;
					}
				}
				index = lastIndex;
				/*
				 * for (int i = 0; i < myPai.length; i++) { if (myPai[i]==jin) { jinpaiIndex++;
				 * } } if (jinpaiIndex==4) { for (int i = 0; i < myPai.length; i++) { if
				 * (myPai[i]==jin) { Random rd = new Random(); while (true) { //
				 * 从最后16张抽一张替换多出来的一张金牌 int index = QZMJConstant.ALL_PAI.length -
				 * rd.nextInt(QZMJConstant.LEFT_PAI_COUNT) - 1; // 如果为花牌重新随机 if
				 * (!QZMJConstant.isHuaPai(this.pai[index])) { // 替换金牌 myPai[i] =
				 * this.pai[index]; break; } } break; } } }
				 */
				getUserPacketMap().get(account).setMyPai(myPai);

			}
		}
	}

	private int[] randomPai(int paiCount) {
		int[] nums = new int[paiCount];
		Random rd = new Random();
		// 随机牌次数
		int randomCardCount = QZMJConstant.RANDOM_CARD_COUNT;
		// 连续牌次数
		int continuedCardCount = QZMJConstant.CONTINUED_CARD_COUNT;
		// 获取随机牌次数系统参数信息
		String randCardCountObj = BaseInfoUtil.getSystemInfo(SysGlobalConstant.QZMJ_RANDOM_CARD_COUNT);
		// 获取连续牌次数系统参数信息
		String continuedCardCountObj = BaseInfoUtil.getSystemInfo(SysGlobalConstant.QZMJ_CONTINUED_CARD_COUNT);
		if (randCardCountObj != null && Dto.isNumber(randCardCountObj)) {
			randomCardCount = Integer.valueOf(randCardCountObj);
		}
		if (continuedCardCountObj != null && Dto.isNumber(continuedCardCountObj)) {
			continuedCardCount = Integer.valueOf(continuedCardCountObj);
		}
		for (int i = 0; i < getUserPacketMap().keySet().size(); i++) {
			int shunCount = rd.nextInt(randomCardCount) + continuedCardCount;
			for (int j = 0; j < shunCount; j++) {
				while (true) {
					int cardType = rd.nextInt(3);
					List<Integer> list = QZMJConstant.TIAO_INDEX_LIST;
					if (cardType % 3 == 0) {
						list = QZMJConstant.WANG_INDEX_LIST;
					} else if (cardType % 3 == 1) {
						list = QZMJConstant.TONG_INDEX_LIST;
					}
					int index = rd.nextInt(list.size());
					int index1 = index % 9 == 8 ? index - 1 : index + 1;
					int index2 = index % 9 == 8 ? index - 2 : index % 9 == 7 ? index - 1 : index + 2;
					int num = list.get(index);
					int next1 = list.get(index1);
					int next2 = list.get(index2);
					if (!ArrayUtils.contains(nums, next1) && !ArrayUtils.contains(nums, next2)) {
						if (!ArrayUtils.contains(nums, num)) {
							nums[j * 3 + i * 16] = num;
							nums[j * 3 + i * 16 + 1] = next1;
							nums[j * 3 + i * 16 + 2] = next2;
							break;
						} else if (num == 0) { // 若是0，判断之前是否已存在
							if (ArrayUtils.indexOf(nums, num) == j * 3) {
								break;
							}
						}
					}
				}
			}
		}

		for (int i = 0; i < nums.length; i++) {
			while (true) {
				int num = rd.nextInt(paiCount);
				if (nums[i] != 0) {
					break;
				}
				if (!ArrayUtils.contains(nums, num)) {
					nums[i] = num;
					break;
				} else if (num == 0) { // 若是0，判断之前是否已存在
					if (ArrayUtils.indexOf(nums, num) == i) {
						break;
					}
				}
			}
		}
		for (int i = 0; i < getUserPacketMap().keySet().size(); i++) {
			List<Integer> list = new ArrayList<>();
			int begin = i * 16;
			for (int j = 0; j < 16; j++) {
				list.add(nums[begin + j]);
			}
			Collections.shuffle(list);
			for (int j = 0; j < list.size(); j++) {
				nums[begin + j] = list.get(j);
			}
		}
		return nums;
	}

	public boolean checkAccount(String account) {
		if (Dto.stringIsNULL(account)) {
			return false;
		}
		if (!getPlayerMap().containsKey(account)) {
			return false;
		}
		if (getPlayerMap().get(account) == null) {
			return false;
		}
		return true;
	}

	public KaiJuModel getLastZhuoPaiValue() {

		if (kaiJuList != null && kaiJuList.size() > 0) {
			for (int i = kaiJuList.size() - 1; i >= 0; i--) {
				KaiJuModel jl = kaiJuList.get(i);
				if (jl.getType() == 0 && jl.getShowType() == 0) {
					return jl;
				}
			}
		}
		return null;
	}

	public JSONArray getChuPaiJiLu(int index) {
		List<Integer> jlList = new ArrayList<Integer>();
		for (KaiJuModel jl : this.kaiJuList) {
			if (jl.getType() == 0 && jl.getIndex() == index && jl.getShowType() == 0) {
				jlList.add(jl.getValues()[0]);
			}
		}
		return JSONArray.fromObject(jlList);
	}

	public int getPlayerIndex(String account) {
		if (!Dto.stringIsNULL(account) && getPlayerMap().get(account) != null) {
			return getPlayerMap().get(account).getMyIndex();
		} else {
			return -1;
		}
	}

	public void setNextThisUUID() {
		this.thisAccount = getNextPlayer(this.thisAccount);
	}

	public boolean hasYouJinType(int youJinType) {
		for (String uuid : getUserPacketMap().keySet()) {
			if (getUserPacketMap().containsKey(uuid) && getUserPacketMap().get(uuid) != null) {
				QZMJUserPacket up = getUserPacketMap().get(uuid);
				if (up != null && up.getYouJinIng() >= youJinType) {
					return true;
				}
			}
		}
		return false;
	}

	public void addKaijuList(int index, int type, int[] values) {
		// 常量定义-todo
		// 本次操作是吃杠碰
		if (type == 4 || type == 5 || type == 6) {
			if (this.kaiJuList.size() > 0) {
				KaiJuModel jilu = this.kaiJuList.get(this.kaiJuList.size() - 1);
				// 上次操作是出牌事件
				if (jilu.getType() == 0) {
					// 隐藏吃碰杠后的牌
					jilu.setShowType(1);
				}
			}
		}
		KaiJuModel kj = new KaiJuModel(index, type, values);
		this.kaiJuList.add(kj);
	}

	public KaiJuModel getLastValue() {
		if (kaiJuList != null && kaiJuList.size() > 0) {
			for (int i = kaiJuList.size() - 1; i >= 0; i--) {
				KaiJuModel jl = kaiJuList.get(i);
				if (jl.getType() == 2) {
					// 暗杠
					return jl;
				} else if (jl.getType() == 9) {
					// 抓杠
					return jl;
				} else if (jl.getType() == 6) {
					// 明杠
					return jl;
				} else if (jl.getType() == 0) {
					// 出牌
					return new KaiJuModel(jl.getIndex(), 1, jl.getValues());
				}
			}
		}
		return null;
	}

	public int getLastFocus() {
		if (kaiJuList != null && kaiJuList.size() - 1 > 0) {
			for (int i = kaiJuList.size() - 1; i >= 0; i--) {
				KaiJuModel jl = kaiJuList.get(i);
				if (jl.getType() != 1) {
					// 暗杠
					return jl.getIndex();
				}
			}
		}
		return getPlayerIndex(getBanker());
	}

	public int getFocusIndex() {
		// 获取最后一次操作记录
		KaiJuModel kaijujl = getLastKaiJuValue(-1);
		if (kaijujl != null) {
			int jltype = kaijujl.getType();
			// 需要出牌-todo 常量定义
			if (jltype == 0 || jltype == 1 || jltype == 2 || jltype == 4 || jltype == 5 || jltype == 6 || jltype == 9) {
				return kaijujl.getIndex();
			} else if (jltype == 11) {
				// 摸牌次数
				int moPaiConut = getActionTimes(kaijujl.getIndex(), 1);
				if (moPaiConut > 0) {
					return kaijujl.getIndex();
				}
			}
		}
		return getPlayerIndex(getBanker());
	}

	public int getActionTimes(int userIndex, int actType) {
		// 操作次数
		int actionTimes = 0;
		for (KaiJuModel kaiJu : getKaiJuList()) {

			if (userIndex != -1 && actType != -1) {

				if (kaiJu.getType() == actType && kaiJu.getIndex() == userIndex) {
					actionTimes++;
				}
			} else if (userIndex != -1) {

				if (kaiJu.getIndex() == userIndex) {
					actionTimes++;
				}
			} else if (actType != -1) {

				if (kaiJu.getType() == actType) {
					actionTimes++;
				}
			}
		}
		return actionTimes;
	}

	public KaiJuModel getLastKaiJuValue(int type) {
		if (kaiJuList != null && kaiJuList.size() > 0) {
			for (int i = kaiJuList.size() - 1; i >= 0; i--) {
				KaiJuModel jl = kaiJuList.get(i);
				if (type == -1) {
					return jl;
				} else if (jl.getType() == type) {
					return jl;
				}
			}
		}
		return null;
	}

	public int getNowPoint() {
		for (KaiJuModel kaijujl : getKaiJuList()) {
			if (kaijujl != null) {
				int jltype = kaijujl.getType();
				// 出牌记录
				if (jltype == 0) {
					if (kaijujl.getShowType() != 1) {
						return kaijujl.getIndex();
					} else {
						return -1;
					}
				}
			}
		}
		return -1;
	}

	public String getNextPlayer(String account) {
		int index = 0;
		if (getPlayerMap().get(account) != null) {
			index = getPlayerMap().get(account).getMyIndex();
		}
		int next = 0;
		// 两人局，坐对面
		if (getPlayerCount() == 2) {
			next = index + 2;
			if (next >= getPlayerMap().size() * 2) {
				next = next - getPlayerMap().size() * 2;
			}
		} else {
			next = index + 1;
			if (next >= getPlayerMap().size()) {
				next = next - getPlayerMap().size();
			}
		}
		for (String uuid : getPlayerMap().keySet()) {
			if (getPlayerMap().containsKey(account) && getPlayerMap().get(account) != null) {
				if (next == getPlayerMap().get(uuid).getMyIndex()) {
					return uuid;
				}
			}
		}
		return getBanker();
	}

	public JSONArray getAllPlayer() {
		JSONArray array = new JSONArray();
		for (String account : getPlayerMap().keySet()) {
			Playerinfo playerinfo = getPlayerMap().get(account);
			QZMJUserPacket up = getUserPacketMap().get(account);
			if (playerinfo == null || up == null)
				continue;

			JSONObject obj = new JSONObject();
			obj.put("account", playerinfo.getAccount());
			obj.put("name", playerinfo.getName());
			obj.put("headimg", playerinfo.getRealHeadimg());
			obj.put("sex", playerinfo.getSex());
			obj.put("ip", playerinfo.getIp());
			obj.put("vip", playerinfo.getVip());
			obj.put("location", playerinfo.getLocation());
			obj.put("area", playerinfo.getArea());

			obj.put("score", playerinfo.getScore() + playerinfo.getSourceScore());
			obj.put("index", playerinfo.getMyIndex());
			obj.put("userOnlineStatus", playerinfo.getStatus());
			obj.put("ghName", playerinfo.getGhName());
			obj.put("introduction", playerinfo.getSignature());
			obj.put("userStatus", up.getStatus());
			obj.put("isTrustee", up.getIsTrustee());
			obj.put("time", up.getTimeLeft());

			array.add(obj);

		}
		return array;
	}

	public JSONArray getCloseRoomData() {
		JSONArray array = new JSONArray();
		for (String account : userPacketMap.keySet()) {
			if (userPacketMap.containsKey(account) && userPacketMap.get(account) != null) {
				JSONObject obj = new JSONObject();
				obj.put("index", getPlayerMap().get(account).getMyIndex());
				obj.put("result", getPlayerMap().get(account).getIsCloseRoom());
				obj.put("name", getPlayerMap().get(account).getName());
				obj.put("jiesanTimer", getJieSanTime());
				array.add(obj);
			}
		}
		return array;
	}

	public static int jiSuanScore(int huType, int difen, int fan, int youjin) {
		int score = 0;

		if (huType == QZMJConstant.HU_TYPE_PH) {
			score = (difen + fan) * QZMJConstant.SCORE_TYPE_PH;
		} else if (huType == QZMJConstant.HU_TYPE_ZM || huType == QZMJConstant.HU_TYPE_QGH
				|| huType == QZMJConstant.HU_TYPE_TH) {
			score = (difen + fan) * QZMJConstant.SCORE_TYPE_ZM;
		} else if (huType == QZMJConstant.HU_TYPE_YJ) {
			if (youjin == 3) {
				score = (difen + fan) * QZMJConstant.SCORE_TYPE_YJ_THREE;
			} else {
				score = (difen + fan) * QZMJConstant.SCORE_TYPE_YJ_FOUR;
			}
		} else if (huType == QZMJConstant.HU_TYPE_SJD) {
			score = (difen + fan) * QZMJConstant.SCORE_TYPE_SJD;
		} else if (huType == QZMJConstant.HU_TYPE_SHY) {
			if (youjin == 3) {
				score = (difen + fan) * QZMJConstant.SCORE_TYPE_SHY_THREE;
			} else {
				score = (difen + fan) * QZMJConstant.SCORE_TYPE_SHY_FOUR;
			}
		} else if (huType == QZMJConstant.HU_TYPE_SY) {
			if (youjin == 3) {
				score = (difen + fan) * QZMJConstant.SCORE_TYPE_SY_THREE;
			} else {
				score = (difen + fan) * QZMJConstant.SCORE_TYPE_SY_FOUR;
			}
		}
		return score;
	}
	public void addBankTimes() {
		bankerTimes++;
	}
	public boolean isAllReady() {
		for (String account : getUserPacketMap().keySet()) {
			if (getUserPacketMap().containsKey(account) && getUserPacketMap().get(account) != null) {
				if (getUserPacketMap().get(account).getStatus() != QZMJConstant.QZ_USER_STATUS_READY) {
					return false;
				}
			}
		}
		return true;
	}
	public boolean isAgreeClose() {
		for (String account : userPacketMap.keySet()) {
			if (getUserPacketMap().containsKey(account) && getUserPacketMap().get(account) != null) {
				if (getPlayerMap().get(account).getIsCloseRoom() != CommonConstant.CLOSE_ROOM_AGREE) {
					return false;
				}
			}
		}
		return true;
	}
	public int getUserScore(String account) {
		if (account.equals(getBanker())) { // 庄家底分翻倍
			int bankerTimes = getBankerTimes();// 连庄次数
			if (bankerTimes > 1) {
				return getZhuangScore() + getBankerScore() * (bankerTimes - 1);// 连庄分数
			}
			return getZhuangScore();
		}
		return getXianScore();
	}
	public int getNowReadyCount() {
		int readyCount = 0;
		for (String account : getUserPacketMap().keySet()) {
			if (getUserPacketMap().containsKey(account) && getUserPacketMap().get(account) != null) {
				if (getUserPacketMap().get(account).getStatus() == QZMJConstant.QZ_USER_STATUS_READY) {
					readyCount++;
				}
			}
		}
		return readyCount;
	}

	public boolean allNoClose() {
		for (String account : getPlayerMap().keySet()) {
			if (getUserPacketMap().containsKey(account) && getUserPacketMap().get(account) != null) {
				if (getPlayerMap().get(account).getIsCloseRoom() != CommonConstant.CLOSE_ROOM_UNSURE) {
					return false;
				}
			}
		}
		return true;
	}
}
