package com.zhuoan.service.impl;

import com.zhuoan.biz.core.sss.SSSOrdinaryCards;
import com.zhuoan.biz.core.sss.SSSSpecialCards;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.sss.SSSUserPacket;
import com.zhuoan.biz.model.sss.SSSGameRoomNew;
import com.zhuoan.constant.SSSConstant;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.service.SSSService;
import com.zhuoan.util.Dto;
import com.zhuoan.util.LogUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
public class SSSServiceImpl implements SSSService {
	
	@Resource
	private RedisInfoService redisInfoService;

	// 在第三局赢的账号
	// public static Set<String> winAccountset1 = new HashSet<String>();

	// 在第四局赢的账号
	// public static Set<String> winAccountset2 = new HashSet<String>();

	// 根据房间号随机赢一局的账号
	// public static Set<String> winAccountset3 = new HashSet<String>();

	@Override
	public int isSpecialCards(String[] cards, String sameColor) {
		int specialType = SSSSpecialCards.none;// 不是特殊牌型
		// 获取房间是否是清一色类型
		boolean isSameColor = false;
		if (SSSConstant.SSS_SAME_COLOR_YES.equals(sameColor)) {
			isSameColor = true;
		}
		ArrayList<String> player = new ArrayList<>();
		for (String string : cards) {
			// 癞子牌不参加特殊牌型,有癞子直接返回
			if ("5".equals(string.split("-")[0])) {
				return specialType;
			}
			player.add(string);
		}
		// 获取十三水特殊牌倍数
		JSONArray array = redisInfoService.getGameSssSpecialSetting();
		if (null == array || array.size() == 0)
			return specialType;
		for (int i = 0; i < array.size(); i++) {
			JSONObject data = array.getJSONObject(i);
			if (null == data || !data.containsKey("card_type"))
				continue;
			switch (data.getInt("card_type")) {
			case SSSSpecialCards.sameThirteen:// 至尊清龙
				if (sameThirteen(player))
					return SSSSpecialCards.sameThirteen;
				break;
			case SSSSpecialCards.eightXian:// 八仙过海
				if (eightXian(player))
					return SSSSpecialCards.eightXian;
				break;
			case SSSSpecialCards.thirteen:// 一条龙
				if (thirteen(player))
					return SSSSpecialCards.thirteen;
				break;
			case SSSSpecialCards.fourThree:// 四套三条
				if (fourThree(player))
					return SSSSpecialCards.fourThree;
				break;
			case SSSSpecialCards.threeBomb:// 三分天下
				if (threeBomb(player))
					return SSSSpecialCards.threeBomb;
				break;
			case SSSSpecialCards.threeEmFiveSo:// 三皇五帝
				if (threeEmFiveSo(player))
					return SSSSpecialCards.threeEmFiveSo;
				break;
			case SSSSpecialCards.sevenStars:// 七星连珠
				if (sevenStars(player))
					return SSSSpecialCards.sevenStars;
				break;
			case SSSSpecialCards.twelfth:// 十二皇族
				if (twelfth(player))
					return SSSSpecialCards.twelfth;
				break;
			case SSSSpecialCards.threeFlushByFlower:// 三同花顺
				if (threeFlushByFlower(player))
					return SSSSpecialCards.threeFlushByFlower;
				break;
			case SSSSpecialCards.sixDaSun:// 六六大顺
				if (sixDaSun(player))
					return SSSSpecialCards.sixDaSun;
				break;
			case SSSSpecialCards.allBig:// 全大
				if (allBig(player))
					return SSSSpecialCards.allBig;
				break;
			case SSSSpecialCards.allSmall:// 全小
				if (allSmall(player))
					return SSSSpecialCards.allSmall;
				break;
			case SSSSpecialCards.oneColor:// 凑一色
				if (!isSameColor && oneColor(player))
					return SSSSpecialCards.oneColor;
				break;
			case SSSSpecialCards.twoGourd:// 双怪冲三
				if (twoGourd(player))
					return SSSSpecialCards.twoGourd;
				break;
			case SSSSpecialCards.fiveThree:// 五对三条
				if (fiveThree(player))
					return SSSSpecialCards.fiveThree;
				break;
			case SSSSpecialCards.sixPairs:// 六对半
				if (sixPairs(player))
					return SSSSpecialCards.sixPairs;
				break;
			case SSSSpecialCards.threeFlush:// 三同花顺
				if (threeFlush(player))
					return SSSSpecialCards.threeFlush;
				break;
			case SSSSpecialCards.threeFlower:// 三同花
				if (threeFlower(player))
					return SSSSpecialCards.threeFlower;
				break;
			default:
				break;
			}
		}

		return specialType;
	}

	@Override
	public int score(int spe) {
		// 获取十三水特殊牌倍数
		JSONArray array = redisInfoService.getGameSssSpecialSetting();
		if (null == array || array.size() == 0)
			return SSSSpecialCards.none;
		for (int i = 0; i < array.size(); i++) {
			JSONObject data = array.getJSONObject(i);
			if (null == data || !data.containsKey("card_type") || !data.containsKey("multiple"))
				continue;
			if (spe == data.getInt("card_type"))
				return data.getInt("multiple");
		}
		return SSSSpecialCards.none;
	}

	@Override
	public String getName(int spe) {
		// 获取十三水特殊牌倍数
		JSONArray array = redisInfoService.getGameSssSpecialSetting();
		if (null == array || array.size() == 0)
			return "";
		for (int i = 0; i < array.size(); i++) {
			JSONObject data = array.getJSONObject(i);
			if (null == data || !data.containsKey("card_type") || !data.containsKey("remark"))
				continue;
			if (spe == data.getInt("card_type"))
				return data.getString("remark");
		}
		return "";
	}
	public static Set<String> ssslj = new HashSet<String>();
	public static Set<String> ssssj = new HashSet<String>();
	public static Set<String> sssGood = new HashSet<String>();
	@Override
	public void faPaiNew(String roomNo) {
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		List<String> pai = room.getPai();
		List<String> list = new ArrayList<>();

		for (String account : room.getUserPacketMap().keySet()) {
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			SSSUserPacket player = room.getUserPacketMap().get(account);
			if (null == playerinfo || null == player)
				continue;
			if (playerinfo.isCollapse())
				continue; // 破产玩家不考虑进去
			if (SSSConstant.SSS_USER_STATUS_READY == player.getStatus()) {
				list.add(account);
			}
		}

		Collections.shuffle(list);// 洗人
		// 马牌处理 start
		boolean maBoolean = false;
		if (room.getMaPaiType() > 0 && !Dto.stringIsNULL(room.getMaPai())) {
			maBoolean = true;
		}
		List<String> maPaiList = new ArrayList<>();// 所有马牌
		if (maBoolean) {
			for (int i = 0; i < pai.size(); i++) {
				if (room.getMaPai().equals(pai.get(i))) {
					maPaiList.add(pai.get(i));
					pai.remove(i);
					i--;
				}
			}
		}
		// 马牌处理 end
		String realMaPai = "";
		if (null != maPaiList && maPaiList.size() > 0) {
			realMaPai = maPaiList.get(0);
			maPaiList.remove(0);
		}
		
		String sameColor = room.getRoomInfo().containsKey(SSSConstant.SSS_DATA_KET_SAME_COLOR)
				? room.getRoomInfo().getString(SSSConstant.SSS_DATA_KET_SAME_COLOR)
				: null;
		for (int i = 0; i < list.size(); i++) {
			String account = list.get(i);
			Playerinfo playerinfo = room.getPlayerMap().get(account);
			if (playerinfo.getSss().size() > 0 && playerinfo.getSss().size()==13) {
				List<String> kongpai = playerinfo.getSss();
				String userPai[] = new String[kongpai.size()];
				kongpai.toArray(userPai);
				QTHServiceImpl impl = new QTHServiceImpl(pai);
				pai = impl.returnPai(userPai);
				for (int j = 0; j < userPai.length; j++) {
					System.out.println(userPai[j]+" ");
				}
				room.getUserPacketMap().get(account).setPai(SSSGameRoomNew.sortPaiDesc(userPai));
				room.getUserPacketMap().get(account).setPaiType(isSpecialCards(userPai, sameColor)); 
				Collections.shuffle(pai);
				playerinfo.setSss(new ArrayList<>());
				room.getPlayerMap().put(account, playerinfo);
				RoomManage.gameRoomMap.put(roomNo, room);
				list.remove(i);
				break;
			}
		}
		//发垃圾牌
		for (int i = 0; i < list.size(); i++) {
			String account = list.get(i);
			String[] userPai = new String[13];
			if (ssslj.contains(account) &&(  room.getGameIndex() == 4)) {
				System.out.println("给玩家" + account + "发牌：");
				QTHServiceImpl impl = new QTHServiceImpl(pai);
				userPai = impl.getPaiPai2();
				for (int j = 0; j < userPai.length; j++) {
					if (null == userPai[j]) {
						System.out.println("发牌有误！");
						break;
					}
				}
				pai = impl.returnPai(userPai);
				room.getUserPacketMap().get(account).setPai(SSSGameRoomNew.sortPaiDesc(userPai));
				// 设置玩家牌型
				room.getUserPacketMap().get(account).setPaiType(isSpecialCards(userPai, sameColor));
				// Collections.shuffle(pai);//洗牌
				list.remove(i);
				break;
			}
		}
		for (int i = 0; i < list.size(); i++) {
			String account = list.get(i);
			String[] userPai = new String[13];
			int need = Integer.parseInt(roomNo.substring(roomNo.length()-1, roomNo.length()));
			if (need==0) {
				need=10;
			}
			if (ssssj.contains(account) && ((room.getGameIndex()== need))) {
				System.out.println("给玩家" + account + "发牌：");
				QTHServiceImpl impl = new QTHServiceImpl(pai);
				userPai = impl.getPaiPai();
				for (int j = 0; j < userPai.length; j++) {
					if (null == userPai[j]) {
						System.out.println("发牌有误！");
						break;
					}
				}
				pai = impl.returnPai(userPai);
				room.getUserPacketMap().get(account).setPai(SSSGameRoomNew.sortPaiDesc(userPai));
				// 设置玩家牌型
				room.getUserPacketMap().get(account).setPaiType(isSpecialCards(userPai, sameColor));
				// Collections.shuffle(pai);//洗牌
				list.remove(i);
				break;

			}

		}
		
		for (int i = 0; i < list.size(); i++) {
			String account = list.get(i);
			String[] userPai = new String[13];
			for (int j = 0; j < userPai.length; j++) {
				if (null != realMaPai && !"".equals(realMaPai)) {
					userPai[j] = realMaPai;
					realMaPai = "";
					room.setMaPaiAccount(account);
					room.setMaPaiIndex(room.getPlayerMap().get(account).getMyIndex());
					continue;
				}
				userPai[j] = pai.get(0);
				pai.remove(0);
			}
			/*if (account.equals("15541276")) {
				//1黑桃，2红桃，3梅花，4方块
				userPai[0]="2-6";
				userPai[1]="3-6";
				userPai[2]="4-6";
				userPai[3]="1-12";
				userPai[4]="1-9";
				userPai[5]="1-8";
				userPai[6]="1-7";
				userPai[7]="1-5";
				userPai[8]="3-5";
				userPai[9]="4-4";
				userPai[10]="3-4";
				userPai[11]="1-4";
				userPai[12]="2-3";
			}else if(account.equals("10855749")){
				userPai[0]="2-2";
				userPai[1]="4-2";
				userPai[2]="5-1";
				userPai[3]="1-1";
				userPai[4]="4-11";
				userPai[5]="4-5";
				userPai[6]="2-5";
				userPai[7]="5-1";
				userPai[8]="1-8";
				userPai[9]="3-8";
				userPai[10]="1-7";
				userPai[11]="5-0";
				userPai[12]="5-0";
			}else if (account.equals("17185517")) {
				userPai[0]="4-1";
				userPai[1]="2-12";
				userPai[2]="5-0";
				userPai[3]="1-10";
				userPai[4]="2-9";
				userPai[5]="2-8";
				userPai[6]="3-7";
				userPai[7]="1-6";
				userPai[8]="1-4";
				userPai[9]="1-3";
				userPai[10]="4-3";
				userPai[11]="5-1";
				userPai[12]="5-1";
				room.setMaPaiAccount(account);
				room.setMaPaiIndex(room.getPlayerMap().get(account).getMyIndex());
			}*/
			
			// 测试用 end
			room.getUserPacketMap().get(account).setPai(SSSGameRoomNew.sortPaiDesc(userPai));
			// 设置玩家牌型
			room.getUserPacketMap().get(account).setPaiType(isSpecialCards(userPai, sameColor));
			if (null != maPaiList && maPaiList.size() > 0) {
				pai.addAll(maPaiList);// 把剩下的假马牌分发给其他人
				maPaiList = new ArrayList<>();// 清空马牌
			}
			Collections.shuffle(pai);// 洗牌
		}
	}

	
	
	
	
		
	
	
	
	@Override
	public void faPai(String roomNo) {
		SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		String sameColor = room.getRoomInfo().containsKey(SSSConstant.SSS_DATA_KET_SAME_COLOR)
				? room.getRoomInfo().getString(SSSConstant.SSS_DATA_KET_SAME_COLOR)
				: null;
		int paiIndex = 0;
		for (String account : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
				if (room.getUserPacketMap().get(account).getStatus() > SSSConstant.SSS_USER_STATUS_INIT) {
					String[] userPai = new String[13];
					for (int i = 0; i < userPai.length; i++) {
						userPai[i] = room.getPai().get(paiIndex);
						paiIndex++;
					}
					room.getUserPacketMap().get(account).setPai(SSSGameRoomNew.sortPaiDesc(userPai));
					// 设置玩家牌型
					room.getUserPacketMap().get(account).setPaiType(isSpecialCards(userPai, sameColor));
				}
			}
		}
	}

	public static boolean sameThirteen(ArrayList<String> player) {
		String flower = player.get(0).split("-")[0];
		TreeSet<String> set = new TreeSet<String>();
		for (String string : player) {
			if (flower.equals(string.split("-")[0])) {
				set.add(string.split("-")[1]);
			}
		}
		if (set.size() == 13) {
			return true;
		}
		return false;
	}

	public static boolean thirteen(ArrayList<String> player) {
		TreeSet<String> set = new TreeSet<String>();
		for (String string : player) {
			set.add(string.split("-")[1]);
		}
		if (set.size() == 13) {
			return true;
		}
		return false;
	}

	public static boolean twelfth(ArrayList<String> player) {
		int i = 0;
		for (String string : player) {
			if (Integer.parseInt(string.split("-")[1]) > 10 || Integer.parseInt(string.split("-")[1]) == 1) {
				i++;
			}
		}
		if (i == 13) {
			return true;
		}
		return false;
	}

	public static boolean threeFlushByFlower(ArrayList<String> player) {
		ArrayList<ArrayList<Integer>> set = getListByFlower(player);

		int i = 0;
		for (ArrayList<Integer> list : set) {
			if (list.size() == 3 || list.size() == 5 || list.size() == 8 || list.size() == 10) {
				if (list.size() == 3) {
					if (list.get(0) == 1 && list.get(2) == 13) {
						if (list.get(1) + 1 == list.get(2)) {
							i++;
						}
					} else {
						if (list.get(0) + 1 == list.get(1) && list.get(1) + 1 == list.get(2)) {
							i++;
						}
					}
				} else if (list.size() == 5) {
					if (list.get(0) == 1 && list.get(4) == 13) {
						if (list.get(1) + 1 == list.get(2) && list.get(2) + 1 == list.get(3)
								&& list.get(3) + 1 == list.get(4)) {
							i += 2;
						}
					} else {
						if (list.get(0) + 1 == list.get(1) && list.get(1) + 1 == list.get(2)
								&& list.get(2) + 1 == list.get(3) && list.get(3) + 1 == list.get(4)) {
							i += 2;
						}
					}
				} else if (list.size() == 8) {
					boolean isTrue1 = false;
					boolean isTrue2 = false;
					for (int k = 0; k < list.size() - 3; k++) {
						ArrayList<Integer> temp = new ArrayList<Integer>();
						temp.addAll(list);
						if (list.contains(temp.get(k) + 1) && list.contains(temp.get(k) + 2)) {
							isTrue1 = true;
							i++;
							temp.remove(temp.indexOf(temp.get(k) + 2));
							temp.remove(temp.indexOf(temp.get(k) + 1));
							temp.remove(k);
						} else {
							isTrue1 = false;
						}
						if (isTrue1) {
							if (temp.get(0) == 1 && temp.get(temp.size() - 1) == 13) {
								if (temp.get(1) + 1 == temp.get(2) && temp.get(2) + 1 == temp.get(3)
										&& temp.get(3) + 1 == temp.get(4)) {
									i += 2;
									isTrue2 = true;
								} else {
									isTrue2 = false;
								}
							} else {
								if (temp.get(0) + 1 == temp.get(1) && temp.get(1) + 1 == temp.get(2)
										&& temp.get(2) + 1 == temp.get(3) && temp.get(3) + 1 == temp.get(4)) {
									i += 2;
									isTrue2 = true;
								} else {
									isTrue2 = false;
								}
							}
							if (isTrue2) {
								break;
							} else {
								i = 0;
							}
						} else {
							i = 0;
						}
					}

					if (!isTrue2) {
						for (int k = 0; k < list.size() - 5; k++) {
							ArrayList<Integer> temp = new ArrayList<Integer>();
							temp.addAll(list);
							if (list.contains(temp.get(k) + 1) && list.contains(temp.get(k) + 2)
									&& list.contains(temp.get(k) + 3) && list.contains(temp.get(k) + 4)) {
								isTrue1 = true;
								i += 2;
								temp.remove(temp.indexOf(temp.get(k) + 4));
								temp.remove(temp.indexOf(temp.get(k) + 3));
								temp.remove(temp.indexOf(temp.get(k) + 2));
								temp.remove(temp.indexOf(temp.get(k) + 1));
								temp.remove(k);
							} else {
								isTrue1 = false;
							}
							if (isTrue1) {
								if (temp.get(0) == 1 && temp.get(temp.size() - 1) == 13) {
									if (temp.get(1) + 1 == temp.get(2)) {
										i++;
										isTrue2 = true;
									} else {
										isTrue2 = false;
									}
								} else {
									if (temp.get(0) + 1 == temp.get(1) && temp.get(1) + 1 == temp.get(2)) {
										i++;
										isTrue2 = true;
									} else {
										isTrue2 = false;
									}
								}
								if (isTrue2) {
									break;
								} else {
									i = 0;
								}
							} else {
								i = 0;
							}
						}
					}

				} else if (list.size() == 10) {
					boolean isTrue1 = false;
					boolean isTrue2 = false;

					for (int k = 0; k < list.size() - 5; k++) {
						ArrayList<Integer> temp = new ArrayList<Integer>();
						temp.addAll(list);
						if (list.contains(temp.get(k) + 1) && list.contains(temp.get(k) + 2)
								&& list.contains(temp.get(k) + 3) && list.contains(temp.get(k) + 4)) {
							isTrue1 = true;
							i += 2;
							temp.remove(temp.indexOf(temp.get(k) + 4));
							temp.remove(temp.indexOf(temp.get(k) + 3));
							temp.remove(temp.indexOf(temp.get(k) + 2));
							temp.remove(temp.indexOf(temp.get(k) + 1));
							temp.remove(k);
						} else {
							isTrue1 = false;
						}
						if (isTrue1) {
							if (temp.get(0) == 1 && temp.get(temp.size() - 1) == 13) {
								if (temp.get(1) + 1 == temp.get(2) && temp.get(2) + 1 == temp.get(3)
										&& temp.get(3) + 1 == temp.get(4)) {
									i += 2;
									isTrue2 = true;
								} else {
									isTrue2 = false;
								}
							} else {
								if (temp.get(0) + 1 == temp.get(1) && temp.get(1) + 1 == temp.get(2)
										&& temp.get(2) + 1 == temp.get(3) && temp.get(3) + 1 == temp.get(4)) {
									i += 2;
									isTrue2 = true;
								} else {
									isTrue2 = false;
								}
							}
							if (isTrue2) {
								break;
							} else {
								i = 0;
							}
						} else {
							i = 0;
						}
					}
				}
			}
		}

		if (i == 5) {
			return true;
		}
		return false;
	}

	public static boolean threeBomb(ArrayList<String> player) {
		ArrayList<ArrayList<Integer>> set = getListByNum(player);

		int i = 0;

		for (int k = 0; k < set.size(); k++) {
			if (set.get(k).size() >= 4) {
				i++;
			}
		}

		if (i == 3) {
			return true;
		}
		return false;
	}

	public static boolean allBig(ArrayList<String> player) {
		int i = 0;
		for (String string : player) {
			if (Integer.parseInt(string.split("-")[1]) >= 8 || Integer.parseInt(string.split("-")[1]) == 1) {
				i++;
			}
		}
		if (i == 13) {
			return true;
		}
		return false;
	}

	public static boolean allSmall(ArrayList<String> player) {
		int i = 0;
		for (String string : player) {
			if (Integer.parseInt(string.split("-")[1]) <= 8 && Integer.parseInt(string.split("-")[1]) >= 2) {
				i++;
			}
		}
		if (i == 13) {
			return true;
		}
		return false;
	}

	public static boolean oneColor(ArrayList<String> player) {
		int black = 0;
		int red = 0;
		for (String string : player) {
			if ("1".equals(string.split("-")[0]) || "3".equals(string.split("-")[0])) {
				black++;
			} else if ("2".equals(string.split("-")[0]) || "4".equals(string.split("-")[0])) {
				red++;
			}
		}
		if (black == 0 || red == 0) {
			return true;
		}
		return false;
	}

	public static boolean twoGourd(ArrayList<String> player) {
		ArrayList<ArrayList<Integer>> set = getListByNum(player);

		int i = 0;// 单牌
		int j = 0;// 对子
		int k = 0;// 三张
		int l = 0;// 炸

		for (ArrayList<Integer> list : set) {
			if (list.size() == 1) {
				i++;
			} else if (list.size() == 2) {
				j++;
			} else if (list.size() == 3) {
				k++;
			} else if (list.size() == 4) {
				l++;
			}
		}
		if ((i == 1 && j == 3 && k == 2 && l == 0) || (i == 1 && j == 1 && k == 2 && l == 1)
				|| (i == 0 && j == 2 && k == 3 && l == 0) || (i == 0 && j == 0 && k == 3 && l == 1)
				|| (i == 0 && j == 3 && k == 1 && l == 1) || (i == 0 && j == 1 && k == 2 && l == 1)) {
			return true;
		}
		return false;
	}

	public static boolean fourThree(ArrayList<String> player) {
		ArrayList<ArrayList<Integer>> set = getListByNum(player);
		// 单牌
		int one = 0;
		// 对子
		int two = 0;
		// 三张
		int three = 0;
		// 炸
		int four = 0;
		// 6张
		int six = 0;
		// 7张
		int seven = 0;
		for (ArrayList<Integer> list : set) {
			if (list.size() == 1) {
				one++;
			} else if (list.size() == 2) {
				two++;
			} else if (list.size() == 3) {
				three++;
			} else if (list.size() == 4) {
				four++;
			} else if (list.size() == 6) {
				six++;
			} else if (list.size() == 7) {
				seven++;
			}
		}
		// 7+6
		if (seven == 1 && six == 1) {
			return true;
		}
		// 7+3+3
		if (seven == 1 && three == 2) {
			return true;
		}
		// 6+3+4
		if (six == 1 && four == 1 && three == 1) {
			return true;
		}
		// 6+3+3+1
		if (six == 1 && three == 2 && one == 1) {
			return true;
		}
		// 4+3+3+3
		if (four == 1 && three == 3) {
			return true;
		}
		// 3+3+3+3+1
		if (three == 4 && one == 1) {
			return true;
		}
		return false;
	}

	public static boolean fiveThree(ArrayList<String> player) {
		ArrayList<ArrayList<Integer>> set = getListByNum(player);
		// 单牌
		int one = 0;
		// 对子
		int two = 0;
		// 三张
		int three = 0;
		// 炸
		int four = 0;
		// 5张
		int five = 0;
		for (ArrayList<Integer> list : set) {
			if (list.size() == 1) {
				one++;
			} else if (list.size() == 2) {
				two++;
			} else if (list.size() == 3) {
				three++;
			} else if (list.size() == 4) {
				four++;
			} else if (list.size() == 5) {
				five++;
			}
		}
		// 2+2+2+2+2+3
		if (one == 0 && two == 5 && three == 1 && four == 0 && five == 0) {
			return true;
		}
		// 4+2+2+2+3
		if (one == 0 && two == 3 && three == 1 && four == 1 && five == 0) {
			return true;
		}
		// 4+4+2+3
		if (one == 0 && two == 1 && three == 1 && four == 2 && five == 0) {
			return true;
		}
		// 2+2+2+2+5
		if (one == 0 && two == 4 && three == 0 && four == 0 && five == 1) {
			return true;
		}
		// 2+2+4+5
		if (one == 0 && two == 2 && three == 0 && four == 1 && five == 1) {
			return true;
		}
		return false;
	}

	public static boolean sixPairs(ArrayList<String> player) {
		ArrayList<ArrayList<Integer>> set = getListByNum(player);
		if (null == set)
			return false;
		int count = 0;
		for (ArrayList<Integer> list : set) {
			if (count > 1)
				return false;
			if (1 == list.size() % 2) {
				count++;
			}
		}
		if (count <= 1)
			return true;
		return false;
	}

	public static boolean threeFlush(ArrayList<String> player) {
		ArrayList<ArrayList<String>> flu = SSSOrdinaryCards.flush(player);
		int s = 0;
		for (int i = 0; i < flu.size(); i++) {
			ArrayList<String> five1 = flu.get(i);
			for (int j = 1; j < flu.size(); j++) {
				ArrayList<String> player1 = new ArrayList<String>(player);
				ArrayList<String> five2 = flu.get(j);
				if (five1 != five2) {
					for (int k = 0; k < five1.size(); k++) {
						player1.remove(five1.get(k));
						player1.remove(five2.get(k));
					}
					if (player1.size() == 3) {
						int one = SSSGameRoomNew.getValue(player1.get(0));
						int two = SSSGameRoomNew.getValue(player1.get(1));
						int three = SSSGameRoomNew.getValue(player1.get(2));
						ArrayList<Integer> ps = new ArrayList<Integer>();
						ps.add(one);
						ps.add(two);
						ps.add(three);
						Collections.sort(ps);
						if ((ps.get(0) + 1 == ps.get(1) && ps.get(1) + 1 == ps.get(2) && ps.get(2) - 2 == ps.get(0))
								|| (ps.get(0) == 1 && ps.get(1) == 12 && ps.get(2) == 13)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public static boolean threeFlower(ArrayList<String> player) {
		ArrayList<ArrayList<Integer>> set = getListByFlower(player);

		int i = 0;

		int zero = 0;
		for (ArrayList<Integer> list : set) {
			if (list.size() == 0) {
				zero++;
			}
		}
		if (zero == 1) {
			for (ArrayList<Integer> list : set) {
				if (list.size() == 3 || list.size() == 5) {
					i++;
				}
			}
		} else if (zero == 2) {
			for (ArrayList<Integer> list : set) {
				if (list.size() == 3 || list.size() == 5) {
					i++;
				} else if (list.size() == 8 || list.size() == 10) {
					i += 2;
				}
			}
		}
		if (i == 3) {
			return true;
		}
		return false;
	}

	public static boolean eightXian(ArrayList<String> player) {
		ArrayList<ArrayList<Integer>> set = getListByNum(player);

		for (ArrayList<Integer> list : set) {
			if (list.size() == 8) {
				return true;
			}
		}
		return false;

	}

	public static boolean sevenStars(ArrayList<String> player) {
		ArrayList<ArrayList<Integer>> set = getListByNum(player);

		for (ArrayList<Integer> list : set) {
			if (list.size() == 7) {
				return true;
			}
		}

		return false;

	}

	public static boolean sixDaSun(ArrayList<String> player) {
		ArrayList<ArrayList<Integer>> set = getListByNum(player);
		for (ArrayList<Integer> list : set) {
			if (list.size() == 6) {
				return true;
			}
		}
		return false;

	}

	public static boolean threeEmFiveSo(ArrayList<String> player) {
		ArrayList<ArrayList<Integer>> set = getListByNum(player);

		int three = 0;
		int five = 0;

		for (ArrayList<Integer> list : set) {
			if (list.size() == 5) {
				five++;
			}
			if (list.size() == 3) {
				three++;
			}
		}

		if (three == 1 && five == 2) {
			return true;
		}

		return false;

	}

	public static ArrayList<ArrayList<Integer>> getListByFlower(ArrayList<String> player) {

		ArrayList<ArrayList<Integer>> set = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> set1 = new ArrayList<Integer>();
		ArrayList<Integer> set2 = new ArrayList<Integer>();
		ArrayList<Integer> set3 = new ArrayList<Integer>();
		ArrayList<Integer> set4 = new ArrayList<Integer>();

		for (String string : player) {
			if ("1".equals(string.split("-")[0])) {
				set1.add(Integer.parseInt(string.split("-")[1]));
			} else if ("2".equals(string.split("-")[0])) {
				set2.add(Integer.parseInt(string.split("-")[1]));
			} else if ("3".equals(string.split("-")[0])) {
				set3.add(Integer.parseInt(string.split("-")[1]));
			} else if ("4".equals(string.split("-")[0])) {
				set4.add(Integer.parseInt(string.split("-")[1]));
			}
		}
		Collections.sort(set1);
		Collections.sort(set2);
		Collections.sort(set3);
		Collections.sort(set4);

		set.add(set1);
		set.add(set2);
		set.add(set3);
		set.add(set4);
		return set;
	}

	public static ArrayList<ArrayList<Integer>> getListByNum(ArrayList<String> player) {

		ArrayList<ArrayList<Integer>> set = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> set1 = new ArrayList<Integer>();
		ArrayList<Integer> set2 = new ArrayList<Integer>();
		ArrayList<Integer> set3 = new ArrayList<Integer>();
		ArrayList<Integer> set4 = new ArrayList<Integer>();
		ArrayList<Integer> set5 = new ArrayList<Integer>();
		ArrayList<Integer> set6 = new ArrayList<Integer>();
		ArrayList<Integer> set7 = new ArrayList<Integer>();
		ArrayList<Integer> set8 = new ArrayList<Integer>();
		ArrayList<Integer> set9 = new ArrayList<Integer>();
		ArrayList<Integer> set10 = new ArrayList<Integer>();
		ArrayList<Integer> set11 = new ArrayList<Integer>();
		ArrayList<Integer> set12 = new ArrayList<Integer>();
		ArrayList<Integer> set13 = new ArrayList<Integer>();

		for (String string : player) {
			if ("1".equals(string.split("-")[1]) && !"5".equals(string.split("-")[0])) {
				set1.add(Integer.parseInt(string.split("-")[0]));
			} else if ("2".equals(string.split("-")[1])) {
				set2.add(Integer.parseInt(string.split("-")[0]));
			} else if ("3".equals(string.split("-")[1])) {
				set3.add(Integer.parseInt(string.split("-")[0]));
			} else if ("4".equals(string.split("-")[1])) {
				set4.add(Integer.parseInt(string.split("-")[0]));
			} else if ("5".equals(string.split("-")[1])) {
				set5.add(Integer.parseInt(string.split("-")[0]));
			} else if ("6".equals(string.split("-")[1])) {
				set6.add(Integer.parseInt(string.split("-")[0]));
			} else if ("7".equals(string.split("-")[1])) {
				set7.add(Integer.parseInt(string.split("-")[0]));
			} else if ("8".equals(string.split("-")[1])) {
				set8.add(Integer.parseInt(string.split("-")[0]));
			} else if ("9".equals(string.split("-")[1])) {
				set9.add(Integer.parseInt(string.split("-")[0]));
			} else if ("10".equals(string.split("-")[1])) {
				set10.add(Integer.parseInt(string.split("-")[0]));
			} else if ("11".equals(string.split("-")[1])) {
				set11.add(Integer.parseInt(string.split("-")[0]));
			} else if ("12".equals(string.split("-")[1])) {
				set12.add(Integer.parseInt(string.split("-")[0]));
			} else if ("13".equals(string.split("-")[1])) {
				set13.add(Integer.parseInt(string.split("-")[0]));
			}
		}
		set.add(set1);
		set.add(set2);
		set.add(set3);
		set.add(set4);
		set.add(set5);
		set.add(set6);
		set.add(set7);
		set.add(set8);
		set.add(set9);
		set.add(set10);
		set.add(set11);
		set.add(set12);
		set.add(set13);
		return set;
	}

	public static String[] CardSort(String[] car, int carType, SSSGameRoomNew room) {

		switch (carType) {
		case 0:

			return SSSOrdinaryCards.sort(car, room);
		case 1:

			return threeFlower(car, room);

		case 2:

			return threeFlush(car, room);

		case 3:

			return sixPairs(car, room);

		case 4:

			return fiveThree(car, room);

		case 5:

			return fourThree(car, room);

		case 6:

			return twoGourd(car, room);

		case 7:

			return oneColor(car);

		case 8:

			return allSmall(car);

		case 9:

			return allBig(car);

		case 10:

			return threeBomb(car, room);

		case 11:

			return threeFlushByFlower(car, room);

		case 12:

			return twelfth(car);

		case 13:

			return thirteen(car);

		case 14:

			return sameThirteen(car);
		case 15:

			return eightXian(car);
		case 16:

			return sevenStars(car);
		case 17:

			return sixDaSun(car);
		case 18:

			return threeEmFiveSo(car);

		default:
			break;
		}

		return SSSOrdinaryCards.sort(car, room);

	}

	private static String[] threeEmFiveSo(String[] car) {
		ArrayList<String> player = new ArrayList<String>();
		for (String string : car) {
			player.add(string);
		}
		ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
		String[] cars = new String[13];
		int num = 0;
		for (ArrayList<Integer> list : set) {
			num++;
			if (list.size() == 3) {
				cars[0] = list.get(0) + "-" + num;
				cars[1] = list.get(1) + "-" + num;
				cars[2] = list.get(2) + "-" + num;
			} else if (list.size() == 5) {
				if (cars[3] == null) {
					cars[3] = list.get(0) + "-" + num;
					cars[4] = list.get(1) + "-" + num;
					cars[5] = list.get(2) + "-" + num;
					cars[6] = list.get(3) + "-" + num;
					cars[7] = list.get(4) + "-" + num;
				} else {
					cars[8] = list.get(0) + "-" + num;
					cars[9] = list.get(1) + "-" + num;
					cars[10] = list.get(2) + "-" + num;
					cars[11] = list.get(3) + "-" + num;
					cars[12] = list.get(4) + "-" + num;
				}
			}
		}

		for (String pai : cars) {
			if (pai == null) {
				LogUtil.print("三皇五帝排牌出错：" + player);
				break;
			}
		}

		return cars;
	}

	private static String[] sixDaSun(String[] car) {
		ArrayList<String> player = new ArrayList<String>();
		for (String string : car) {
			player.add(string);
		}
		ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);

		String[] cars = new String[13];
		int num = 0;
		int i = 6;
		for (ArrayList<Integer> list : set) {
			num++;
			if (list.size() == 6) {
				cars[0] = list.get(0) + "-" + num;
				cars[1] = list.get(1) + "-" + num;
				cars[2] = list.get(2) + "-" + num;
				cars[3] = list.get(3) + "-" + num;
				cars[4] = list.get(4) + "-" + num;
				cars[5] = list.get(5) + "-" + num;

			} else if (list.size() != 0) {
				for (int j = 0; j < list.size(); j++) {
					cars[i] = list.get(j) + "-" + num;
					i++;
				}
			}
		}

		for (String pai : cars) {
			if (pai == null) {
				LogUtil.print("六六大顺排牌出错：" + player);
				break;
			}
		}

		return cars;

	}

	private static String[] sevenStars(String[] car) {
		ArrayList<String> player = new ArrayList<String>();
		for (String string : car) {
			player.add(string);
		}
		ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
		String[] cars = new String[13];
		int num = 0;
		int i = 7;
		for (ArrayList<Integer> list : set) {
			num++;
			if (list.size() == 7) {
				cars[0] = list.get(0) + "-" + num;
				cars[1] = list.get(1) + "-" + num;
				cars[2] = list.get(2) + "-" + num;
				cars[3] = list.get(3) + "-" + num;
				cars[4] = list.get(4) + "-" + num;
				cars[5] = list.get(5) + "-" + num;
				cars[6] = list.get(6) + "-" + num;

			} else if (list.size() != 0) {
				for (int j = 0; j < list.size(); j++) {
					cars[i] = list.get(j) + "-" + num;
					i++;
				}
			}
		}

		for (String pai : cars) {
			if (pai == null) {
				LogUtil.print("七星连珠排牌出错：" + player);
				break;
			}
		}
		return cars;
	}

	private static String[] eightXian(String[] car) {
		ArrayList<String> player = new ArrayList<String>();
		for (String string : car) {
			player.add(string);
		}

		ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
		String[] cars = new String[13];
		int num = 0;
		int i = 8;
		for (ArrayList<Integer> list : set) {
			num++;
			if (list.size() == 8) {
				cars[0] = list.get(0) + "-" + num;
				cars[1] = list.get(1) + "-" + num;
				cars[2] = list.get(2) + "-" + num;
				cars[3] = list.get(3) + "-" + num;
				cars[4] = list.get(4) + "-" + num;
				cars[5] = list.get(5) + "-" + num;
				cars[6] = list.get(6) + "-" + num;
				cars[7] = list.get(7) + "-" + num;

			} else if (list.size() != 0) {
				for (int j = 0; j < list.size(); j++) {
					cars[i] = list.get(j) + "-" + num;
					i++;
				}
			}
		}

		for (String pai : cars) {
			if (pai == null) {
				LogUtil.print("八仙过海排牌出错：" + player);
				break;
			}
		}
		return cars;
	}

	public static String[] sameThirteen(String[] car) {
		for (int i = 0; i < car.length - 1; i++) {

			for (int j = i + 1; j < car.length; j++) {

				if (SSSGameRoomNew.getValue(car[i]) > SSSGameRoomNew.getValue(car[j])) {
					String o = car[i];
					String o1 = car[j];
					car[i] = o1;
					car[j] = o;
				}
			}
		}
		return car;
	}

	public static String[] thirteen(String[] car) {
		for (int i = 0; i < car.length - 1; i++) {

			for (int j = i + 1; j < car.length; j++) {

				if (SSSGameRoomNew.getValue(car[i]) > SSSGameRoomNew.getValue(car[j])) {
					String o = car[i];
					String o1 = car[j];
					car[i] = o1;
					car[j] = o;
				}
			}
		}
		return car;
	}

	public static String[] twelfth(String[] car) {
		for (int i = 0; i < car.length - 1; i++) {
			for (int j = i + 1; j < car.length; j++) {
				if (SSSGameRoomNew.getValue(car[i]) > SSSGameRoomNew.getValue(car[j])) {
					String o = car[i];
					String o1 = car[j];
					car[i] = o1;
					car[j] = o;
				}
			}
		}
		return car;
	}

	public static String[] threeFlushByFlower(String[] car, SSSGameRoomNew room) {
		// TODO Auto-generated method stub
		return SSSOrdinaryCards.sort(car, room);
	}

	public static String[] threeBomb(String[] car, SSSGameRoomNew room) {
		// TODO Auto-generated method stub
		return SSSOrdinaryCards.sort(car, room);
	}

	public static String[] allBig(String[] car) {
		for (int i = 0; i < car.length - 1; i++) {
			for (int j = i + 1; j < car.length; j++) {
				if (SSSGameRoomNew.getValue(car[i]) > SSSGameRoomNew.getValue(car[j])) {
					String o = car[i];
					String o1 = car[j];
					car[i] = o1;
					car[j] = o;
				}
			}
		}
		return car;
	}

	public static String[] allSmall(String[] car) {

		for (int i = 0; i < car.length - 1; i++) {

			for (int j = i + 1; j < car.length; j++) {

				if (SSSGameRoomNew.getValue(car[i]) > SSSGameRoomNew.getValue(car[j])) {
					String o = car[i];
					String o1 = car[j];
					car[i] = o1;
					car[j] = o;
				}
			}
		}
		return car;
	}

	public static String[] oneColor(String[] car) {
		for (int i = 0; i < car.length - 1; i++) {

			for (int j = i + 1; j < car.length; j++) {

				if (SSSGameRoomNew.getValue(car[i]) > SSSGameRoomNew.getValue(car[j])) {
					String o = car[i];
					String o1 = car[j];
					car[i] = o1;
					car[j] = o;
				}
			}
		}
		return car;
	}

	public static String[] twoGourd(String[] car, SSSGameRoomNew roomNew) {
		// TODO Auto-generated method stub
		return SSSOrdinaryCards.sort(car, roomNew);
	}

	public static String[] fourThree(String[] car, SSSGameRoomNew roomNew) {
		// TODO Auto-generated method stub
		return SSSOrdinaryCards.sort(car, roomNew);
	}

	public static String[] fiveThree(String[] car, SSSGameRoomNew roomNew) {
		// TODO Auto-generated method stub
		return SSSOrdinaryCards.sort(car, roomNew);
	}

	public static String[] sixPairs(String[] car, SSSGameRoomNew roomNew) {

		ArrayList<String> player = new ArrayList<String>();
		for (String string : car) {
			player.add(string);
		}

		ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
		// ArrayList<ArrayList<String>> set1 =SSSOrdinaryCards.one(player);

		String[] cars = new String[13];
		int num = 0;
		int c = 0;
		for (ArrayList<Integer> list : set) {
			num++;

			if (list.size() == 1 || list.size() == 3 || list.size() == 5 || list.size() == 9) {
				for (int i = 0; i < list.size(); i++) {
					if (cars[c] == null) {
						cars[c] = list.get(i) + "-" + num;
					}
					c++;
				}

			} else if (list.size() == 2 || list.size() == 4 || list.size() == 6 || list.size() == 8) {

				for (int i = 0; i < list.size(); i++) {
					if (cars[c] == null) {
						cars[c] = list.get(i) + "-" + num;
					}
					c++;
				}
			}
		}
		/*
		 * int j=1; for (ArrayList<Integer> list : set) { if (list.size() == 1) {
		 * cars[0]= } else if (list.size() >= 2) {
		 * 
		 * for (int i = 0; i < list.size(); i++) {
		 * 
		 * } j++; } }
		 */
		Boolean is = false;
		for (String pai : cars) {
			if (pai == null) {
				is = true;
				LogUtil.print("六对半排牌出错：" + player);
				break;
			}
		}

		if (is) {
			return SSSOrdinaryCards.sort(car, roomNew);
		} else {
			return cars;
		}

		/* return SSSOrdinaryCards.sort(car); */
	}

	public static String[] threeFlush(String[] car, SSSGameRoomNew roomNew) {
		ArrayList<String> player = new ArrayList<String>();
		for (String string : car) {
			player.add(string);
		}

		String[] cars = new String[13];
		ArrayList<ArrayList<String>> flu = SSSOrdinaryCards.flush(player);

		for (int i = 0; i < flu.size(); i++) {
			/* ArrayList<String> player1=new ArrayList<String>(player); */
			ArrayList<String> five1 = flu.get(i);
			for (int j = 1; j < flu.size(); j++) {
				ArrayList<String> player1 = new ArrayList<String>(player);
				ArrayList<String> five2 = flu.get(j);
				if (five1 != five2) {
					for (int k = 0; k < five1.size(); k++) {
						player1.remove(five1.get(k));
						player1.remove(five2.get(k));
					}
					if (player1.size() == 3) {
						int one = SSSGameRoomNew.getValue(player1.get(0));
						int two = SSSGameRoomNew.getValue(player1.get(1));
						int three = SSSGameRoomNew.getValue(player1.get(2));
						ArrayList<Integer> ps = new ArrayList<Integer>();
						ps.add(one);
						ps.add(two);
						ps.add(three);
						Collections.sort(ps);
						if (ps.get(0) + 1 == ps.get(1) && ps.get(1) + 1 == ps.get(2) && ps.get(2) - 2 == ps.get(0)) {
							if (SSSGameRoomNew.getValue(five1.get(4)) > SSSGameRoomNew.getValue(five2.get(4))) {
								if ((SSSGameRoomNew.getValue(five1.get(0)) != 1
										&& SSSGameRoomNew.getValue(five2.get(0)) != 1)
										|| (SSSGameRoomNew.getValue(five1.get(0)) == 1
												&& SSSGameRoomNew.getValue(five2.get(0)) != 1)) {
									cars[12] = five1.get(4);
									cars[11] = five1.get(3);
									cars[10] = five1.get(2);
									cars[9] = five1.get(1);
									cars[8] = five1.get(0);
									cars[7] = five2.get(4);
									cars[6] = five2.get(3);
									cars[5] = five2.get(2);
									cars[4] = five2.get(1);
									cars[3] = five2.get(0);
									cars[2] = player1.get(2);
									cars[1] = player1.get(1);
									cars[0] = player1.get(0);
								} else if (SSSGameRoomNew.getValue(five1.get(0)) != 1
										&& SSSGameRoomNew.getValue(five2.get(0)) == 1) {
									cars[12] = five2.get(4);
									cars[11] = five2.get(3);
									cars[10] = five2.get(2);
									cars[9] = five2.get(1);
									cars[8] = five2.get(0);
									cars[7] = five1.get(4);
									cars[6] = five1.get(3);
									cars[5] = five1.get(2);
									cars[4] = five1.get(1);
									cars[3] = five1.get(0);
									cars[2] = player1.get(2);
									cars[1] = player1.get(1);
									cars[0] = player1.get(0);
								}
							} else {
								if ((SSSGameRoomNew.getValue(five1.get(0)) != 1
										&& SSSGameRoomNew.getValue(five2.get(0)) != 1)
										|| (SSSGameRoomNew.getValue(five1.get(0)) == 1
												&& SSSGameRoomNew.getValue(five2.get(0)) != 1)) {
									cars[12] = five2.get(4);
									cars[11] = five2.get(3);
									cars[10] = five2.get(2);
									cars[9] = five2.get(1);
									cars[8] = five2.get(0);
									cars[7] = five1.get(4);
									cars[6] = five1.get(3);
									cars[5] = five1.get(2);
									cars[4] = five1.get(1);
									cars[3] = five1.get(0);
									cars[2] = player1.get(2);
									cars[1] = player1.get(1);
									cars[0] = player1.get(0);

								} else if (SSSGameRoomNew.getValue(five1.get(0)) != 1
										&& SSSGameRoomNew.getValue(five2.get(0)) == 1) {
									cars[12] = five1.get(4);
									cars[11] = five1.get(3);
									cars[10] = five1.get(2);
									cars[9] = five1.get(1);
									cars[8] = five1.get(0);
									cars[7] = five2.get(4);
									cars[6] = five2.get(3);
									cars[5] = five2.get(2);
									cars[4] = five2.get(1);
									cars[3] = five2.get(0);
									cars[2] = player1.get(2);
									cars[1] = player1.get(1);
									cars[0] = player1.get(0);
								}
							}
						} else if (ps.get(0) == 1 && ps.get(1) == 12 && ps.get(2) == 13) {
							if ((SSSGameRoomNew.getValue(five1.get(0)) != 1
									&& SSSGameRoomNew.getValue(five2.get(0)) != 1)
									|| (SSSGameRoomNew.getValue(five1.get(0)) == 1
											&& SSSGameRoomNew.getValue(five2.get(0)) != 1)) {
								cars[12] = five1.get(4);
								cars[11] = five1.get(3);
								cars[10] = five1.get(2);
								cars[9] = five1.get(1);
								cars[8] = five1.get(0);
								cars[7] = five2.get(4);
								cars[6] = five2.get(3);
								cars[5] = five2.get(2);
								cars[4] = five2.get(1);
								cars[3] = five2.get(0);
								cars[2] = player1.get(2);
								cars[1] = player1.get(1);
								cars[0] = player1.get(0);
							} else if (SSSGameRoomNew.getValue(five1.get(0)) != 1
									&& SSSGameRoomNew.getValue(five2.get(0)) == 1) {
								cars[12] = five2.get(4);
								cars[11] = five2.get(3);
								cars[10] = five2.get(2);
								cars[9] = five2.get(1);
								cars[8] = five2.get(0);
								cars[7] = five1.get(4);
								cars[6] = five1.get(3);
								cars[5] = five1.get(2);
								cars[4] = five1.get(1);
								cars[3] = five1.get(0);
								cars[2] = player1.get(2);
								cars[1] = player1.get(1);
								cars[0] = player1.get(0);
							}
						}
					}

				}

			}
		}
		Boolean is = false;
		for (String pai : cars) {
			if (pai == null) {
				is = true;
				LogUtil.print("三顺子排牌出错：" + player);
				break;
			}
		}
		if (is) {
			return SSSOrdinaryCards.sort(car, roomNew);
		} else {
			return cars;
		}
	}

	public static String[] threeFlower(String[] car, SSSGameRoomNew roomNew) {
		ArrayList<String> player = new ArrayList<String>();
		for (String string : car) {
			player.add(string);
		}

		ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByFlower(player);

		String[] cars = new String[13];
		int flower = 0;
		for (ArrayList<Integer> list : set) {
			flower++;
			if (list.size() == 3) {
				cars[0] = flower + "-" + list.get(0);
				cars[1] = flower + "-" + list.get(1);
				cars[2] = flower + "-" + list.get(2);
			} else if (list.size() == 5) {
				if (cars[3] == null) {
					cars[3] = flower + "-" + list.get(0);
					cars[4] = flower + "-" + list.get(1);
					cars[5] = flower + "-" + list.get(2);
					cars[6] = flower + "-" + list.get(3);
					cars[7] = flower + "-" + list.get(4);
				} else {
					cars[8] = flower + "-" + list.get(0);
					cars[9] = flower + "-" + list.get(1);
					cars[10] = flower + "-" + list.get(2);
					cars[11] = flower + "-" + list.get(3);
					cars[12] = flower + "-" + list.get(4);
				}
			} else if (list.size() == 8) {
				if (cars[3] == null && cars[0] == null) {
					cars[0] = flower + "-" + list.get(0);
					cars[1] = flower + "-" + list.get(1);
					cars[2] = flower + "-" + list.get(2);
					cars[3] = flower + "-" + list.get(3);
					cars[4] = flower + "-" + list.get(4);
					cars[5] = flower + "-" + list.get(5);
					cars[6] = flower + "-" + list.get(6);
					cars[7] = flower + "-" + list.get(7);
				} else if (cars[0] == null && cars[8] == null) {
					cars[0] = flower + "-" + list.get(0);
					cars[1] = flower + "-" + list.get(1);
					cars[2] = flower + "-" + list.get(2);
					cars[8] = flower + "-" + list.get(3);
					cars[9] = flower + "-" + list.get(4);
					cars[10] = flower + "-" + list.get(5);
					cars[11] = flower + "-" + list.get(6);
					cars[12] = flower + "-" + list.get(7);
				}
			} else if (list.size() == 10) {
				cars[3] = flower + "-" + list.get(0);
				cars[4] = flower + "-" + list.get(1);
				cars[5] = flower + "-" + list.get(2);
				cars[6] = flower + "-" + list.get(3);
				cars[7] = flower + "-" + list.get(4);
				cars[8] = flower + "-" + list.get(5);
				cars[9] = flower + "-" + list.get(6);
				cars[10] = flower + "-" + list.get(7);
				cars[11] = flower + "-" + list.get(8);
				cars[12] = flower + "-" + list.get(9);
			} else if (list.size() == 13) {
				cars[0] = flower + "-" + list.get(0);
				cars[1] = flower + "-" + list.get(1);
				cars[2] = flower + "-" + list.get(2);
				cars[3] = flower + "-" + list.get(3);
				cars[4] = flower + "-" + list.get(4);
				cars[5] = flower + "-" + list.get(5);
				cars[6] = flower + "-" + list.get(6);
				cars[7] = flower + "-" + list.get(7);
				cars[8] = flower + "-" + list.get(8);
				cars[9] = flower + "-" + list.get(9);
				cars[10] = flower + "-" + list.get(10);
				cars[11] = flower + "-" + list.get(11);
				cars[12] = flower + "-" + list.get(12);
			}

		}
		Boolean is = false;
		for (String pai : cars) {
			if (pai == null) {
				is = true;
				LogUtil.print("三同花排牌出错：" + player);
				break;
			}
		}
		if (is) {
			return SSSOrdinaryCards.sort(car, roomNew);
		} else {
			return cars;
		}
	}
}
