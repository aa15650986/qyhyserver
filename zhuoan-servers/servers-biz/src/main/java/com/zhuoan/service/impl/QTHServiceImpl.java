package com.zhuoan.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.zhuoan.constant.SSSConstant;

public class QTHServiceImpl {

	/*
	 * 控牌测试============== 六十个方法 对应40套好牌 20套垃圾牌 通过随机数 随机抽取一套牌出来
	 */

	private List<String> paiList;

	private List<String> paiList2;

	private Map<Integer, int[]> map = new ConcurrentHashMap<>();

	private List<Integer> list = new ArrayList<>();

	public QTHServiceImpl(List<String> pai) {
		/*
		 * list中存放顺序为 0.五同 1.顺子+顺子 2.顺子+同花顺 3.顺子 4.同花顺 5.同花 6.炸弹 7.三条 8.对子 9.单
		 */
		super();
		this.paiList = pai;
		this.paiList2 = new ArrayList<>();
		paiList2.addAll(paiList);
		// 1五同 1三条 1对 3单
		map.put(1, new int[] { 1, 0, 0, 0, 0, 0, 0, 1, 1, 3 });
		// 1炸弹 3对 3单
		map.put(2, new int[] { 0, 0, 0, 0, 0, 0, 1, 0, 3, 3 });
		// 1炸弹 1三条 1对 4单
		map.put(3, new int[] { 0, 0, 0, 0, 0, 0, 1, 1, 1, 4 });
		// 1炸弹 1顺子 1对 2单
		map.put(4, new int[] { 0, 0, 0, 1, 0, 0, 1, 0, 1, 2 });
		// 1同花 1炸弹 1对 2单
		map.put(5, new int[] { 0, 0, 0, 0, 0, 1, 1, 0, 1, 2 });
		// 1炸弹 1三条 2对 2单
		map.put(6, new int[] { 0, 0, 0, 0, 0, 0, 1, 1, 2, 2 });
		// 1同花顺 2对 4单
		map.put(7, new int[] { 0, 0, 0, 0, 1, 0, 0, 0, 2, 4 });
		// 1同花顺 3对 2单
		map.put(8, new int[] { 0, 0, 0, 0, 1, 0, 0, 0, 3, 2 });
		// 1同花顺 1三条 1对 3单
		map.put(9, new int[] { 0, 0, 0, 0, 1, 0, 0, 1, 1, 3 });
		// 1顺子 1同花顺 1对 1单
		map.put(10, new int[] { 0, 0, 1, 0, 0, 0, 0, 0, 1, 1 });
		// 1同花顺 1同花 1对 1单
		map.put(11, new int[] { 0, 0, 0, 0, 1, 1, 0, 0, 1, 1 });
		// 1同花顺 1三条 2对 1单
		map.put(12, new int[] { 0, 0, 0, 0, 1, 0, 0, 1, 2, 1 });
		// 1同花顺 1三条 5单
		map.put(13, new int[] { 0, 0, 0, 0, 1, 0, 0, 1, 0, 5 });
		// 1顺子 1同花顺 3单
		map.put(14, new int[] { 0, 0, 1, 0, 0, 0, 0, 0, 0, 3 });
		// 1同花顺 1同花 3单
		map.put(15, new int[] { 0, 0, 0, 0, 1, 1, 0, 0, 0, 3 });
		// 1同花 1三条 1对 3单
		map.put(16, new int[] { 0, 0, 0, 0, 0, 1, 0, 1, 1, 3 });
		// 2炸弹 5单
		map.put(17, new int[] { 0, 0, 0, 0, 0, 0, 2, 0, 0, 5 });
		// 1炸弹 2三条 3单
		map.put(18, new int[] { 0, 0, 0, 0, 0, 0, 1, 2, 0, 3 });
		// 1顺子 1炸弹 1三条 1单
		map.put(19, new int[] { 0, 0, 0, 1, 0, 0, 1, 1, 0, 1 });
		// 1同花 1炸弹 1三条 1单
		map.put(20, new int[] { 0, 0, 0, 0, 0, 1, 1, 1, 0, 1 });
		// 1炸弹 2三条 1对 1单
		map.put(21, new int[] { 0, 0, 0, 0, 0, 0, 1, 2, 1, 1 });
		// 1顺子 1同花顺 1三条
		map.put(22, new int[] { 0, 0, 1, 0, 0, 0, 0, 1, 0, 0 });
		// 1同花顺 1同花 1三条
		map.put(23, new int[] { 0, 0, 0, 0, 1, 1, 0, 1, 0, 0 });
		// 1同花顺 2三条 1对
		map.put(24, new int[] { 0, 0, 0, 0, 1, 0, 0, 2, 1, 0 });
		// 3三条 1对 2单
		map.put(25, new int[] { 0, 0, 0, 0, 0, 0, 0, 3, 1, 2 });
		// 1顺子 1同花 1三条
		map.put(26, new int[] { 0, 0, 0, 1, 0, 1, 0, 1, 0, 0 });
		// 1五同 1同花 3单
		map.put(27, new int[] { 1, 0, 0, 0, 0, 1, 0, 0, 0, 3 });
		// 1五同 1对 6单
		map.put(28, new int[] { 1, 0, 0, 0, 0, 0, 0, 0, 1, 6 });
		// 1五同 2对 4单
		map.put(29, new int[] { 1, 0, 0, 0, 0, 0, 0, 0, 2, 4 });
		// 1五同 3对 2单
		map.put(30, new int[] { 1, 0, 0, 0, 0, 0, 0, 0, 3, 2 });
		// 1五同 1顺子 3单
		map.put(31, new int[] { 1, 0, 0, 1, 0, 0, 0, 0, 0, 3 });
		// 四对 5 单
		map.put(32, new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 4, 5 });

		// 垃圾
		/*
		 * map.put(34, new int[] { 0, 0, 0, 0, 0, 0, 0, 1, 2, 6 }); map.put(35, new
		 * int[] { 0, 0, 0, 0, 0, 0, 0, 1, 2, 6 }); map.put(36, new int[] { 0, 0, 0, 0,
		 * 0, 0, 0, 1, 2, 6 }); map.put(37, new int[] { 0, 0, 0, 0, 0, 0, 0, 1, 2, 6 });
		 * map.put(38, new int[] { 0, 0, 0, 0, 0, 0, 0, 1, 2, 6 }); map.put(39, new
		 * int[] { 0, 0, 0, 0, 0, 0, 0, 1, 2, 6 }); map.put(40, new int[] { 0, 0, 0, 0,
		 * 0, 0, 0, 1, 2, 6 }); map.put(41, new int[] { 0, 0, 0, 0, 0, 0, 0, 1, 2, 6 });
		 * map.put(42, new int[] { 0, 0, 0, 0, 0, 0, 0, 1, 2, 6 }); map.put(43, new
		 * int[] { 0, 0, 0, 0, 0, 0, 0, 1, 2, 6 }); map.put(44, new int[] { 0, 0, 0, 0,
		 * 0, 0, 0, 1, 2, 6 }); map.put(45, new int[] { 0, 0, 0, 0, 0, 0, 0, 1, 2, 6 });
		 * map.put(46, new int[] { 0, 0, 0, 0, 0, 0, 0, 1, 2, 6 }); map.put(47, new
		 * int[] { 0, 0, 0, 0, 0, 0, 0, 1, 2, 6 }); map.put(48, new int[] { 0, 0, 0, 0,
		 * 0, 0, 0, 1, 2, 6 });
		 */
	}

	// 返回给某玩家发完牌之后剩余的牌组
	public List<String> returnPai(String[] userPai) {
		for (int j = 0; j < userPai.length; j++) {
			Iterator<String> iterator = paiList2.iterator();
			while (iterator.hasNext()) {
				String x = iterator.next();
				if (userPai[j].equals(x)) {
					iterator.remove();
					break;
				}
			}
		}
		return paiList2;
	}

	// 获得对子
	public List<String> getDuizi() {
		String one = null;
		while (true) {
			one = paiList.get(0);
			if (!one.startsWith("5")) {
				break;
			} else {
				paiList.remove(0);
			}
		}

		paiList.remove(one);
		String onePai = one.substring(one.length() - 1, one.length());
		String two = null;
		if (!one.substring(one.length() - 2, one.length() - 1).equals("-")) {
			// 11 - 13 的牌
			onePai = one.substring(one.length() - 2, one.length());
			for (int i = 0; i < paiList.size(); i++) {
				if (paiList.get(i).substring(paiList.get(i).length() - 2, paiList.get(i).length()).equals(onePai)
						&& !one.startsWith("5")) {
					two = paiList.get(i);
					break;
				}
			}

			// 移除剩余所有的这个对子的数字
			Iterator<String> iterator = paiList.iterator();
			while (iterator.hasNext()) {
				String x = (String) iterator.next();
				if (x.substring(x.length() - 2, x.length()).equals(onePai) && x.length() == 4) {
					iterator.remove();
				}
			}

		} else {
			// 1- 10的牌
			for (int i = 0; i < paiList.size(); i++) {
				if (paiList.get(i).length() != 4 && (paiList.get(i)
						.substring(paiList.get(i).length() - 1, paiList.get(i).length()).equals(onePai))
						&& (!one.startsWith("5"))) {
					two = paiList.get(i);
					break;
				}
			}
			// 移除剩余所有的这个对子的数字
			Iterator<String> iterator = paiList.iterator();
			while (iterator.hasNext()) {
				String x = (String) iterator.next();
				if (x.substring(x.length() - 1, x.length()).equals(onePai) && x.length() == 3) {
					iterator.remove();
				}
			}
		}

		List<String> listDuizi = new ArrayList<String>();
		listDuizi.add(one);
		listDuizi.add(two);

		System.out.println("获取对子牌：" + listDuizi);
		return listDuizi;
	}

	// 获取单牌
	public String getDan() {
		String one = null;
		while (true) {
			one = paiList.get(0);
			if (!one.startsWith("5")) {
				break;
			} else {
				paiList.remove(0);
			}
		}
		String onePai = one.substring(one.length() - 1, one.length());

		if (!one.substring(one.length() - 2, one.length() - 1).equals("-")) {
			onePai = one.substring(one.length() - 2, one.length());
			Iterator<String> iterator = paiList.iterator();
			while (iterator.hasNext()) {
				String x = (String) iterator.next();
				if (x.substring(x.length() - 2, x.length()).equals(onePai) && x.length() == 4) {
					iterator.remove();
				}
			}
		} else {
			// 移除剩余所有的这个对子的数字
			Iterator<String> iterator = paiList.iterator();
			while (iterator.hasNext()) {
				String x = (String) iterator.next();
				if (x.substring(x.length() - 1, x.length()).equals(onePai) && x.length() == 3) {
					iterator.remove();
				}
			}
		}
		System.out.println("获取单牌：" + one);
		return one;

	}

	// 获得三条
	/*
	 * public List<String> getSantiao() { String one = null; while (true) { one =
	 * paiList.get(0); if (!one.startsWith("5")) { break; } else {
	 * paiList.remove(0); } } paiList.remove(one); String onePai =
	 * one.substring(one.length() - 1, one.length()); String two = null; String
	 * three = null; if (!one.substring(one.length() - 2, one.length() -
	 * 1).equals("-")) { // 11-13 onePai = one.substring(one.length() - 2,
	 * one.length()); int j = 0; for (int i = 0; i < paiList.size(); i++) {
	 * 
	 * if (paiList.get(i).substring(paiList.get(i).length() - 2,
	 * paiList.get(i).length()).equals(onePai) && !one.startsWith("5")) { j++; if (j
	 * == 1) { two = paiList.get(i); } else if (j == 2) { three = paiList.get(i);
	 * break; } } } // 移除剩余所有的这个三条的数字 Iterator<String> iterator =
	 * paiList.iterator(); while (iterator.hasNext()) { String x = (String)
	 * iterator.next(); if (x.substring(x.length() - 2, x.length()).equals(onePai)
	 * && x.length() == 4) { iterator.remove(); } } } else { // 1-10 int j = 0; for
	 * (int i = 0; i < paiList.size(); i++) {
	 * 
	 * if (paiList.get(i).length() != 4 && (paiList.get(i)
	 * .substring(paiList.get(i).length() - 1,
	 * paiList.get(i).length()).equals(onePai)) && (!one.startsWith("5"))) { j++; if
	 * (j == 1) { two = paiList.get(i); } else if (j == 2) { three = paiList.get(i);
	 * break; } } } // 移除剩余所有的这个三条的数字 Iterator<String> iterator =
	 * paiList.iterator(); while (iterator.hasNext()) { String x = (String)
	 * iterator.next(); if (x.substring(x.length() - 1, x.length()).equals(onePai)
	 * && x.length() == 3) { iterator.remove(); } } } List<String> listSantiao = new
	 * ArrayList<String>(); listSantiao.add(one); listSantiao.add(two);
	 * listSantiao.add(three); System.out.println("获取三条：" + listSantiao); return
	 * listSantiao; }
	 */

	// 获得炸弹
	/*
	 * public List<String> getZhadan(int n) { String one = null; while (true) { one
	 * = paiList.get(0); if (!one.startsWith("5")) { break; } else {
	 * paiList.remove(0); } } paiList.remove(one); List<String> returnList = new
	 * ArrayList<String>(); returnList.add(one); Iterator<String> iterator =
	 * paiList.iterator(); int i = 0; while (iterator.hasNext()) { String string =
	 * (String) iterator.next(); if (one.length() == 3 && string.length() == 3) { if
	 * (string.substring(string.length() - 1, string.length())
	 * .equals(one.substring(one.length() - 1, one.length())) && i < n) { i++;
	 * returnList.add(string); iterator.remove(); } else if
	 * (string.substring(string.length() - 1, string.length())
	 * .equals(one.substring(one.length() - 1, one.length())) && i == n) {
	 * iterator.remove(); } } else if (one.length() == 4 && string.length() == 4) {
	 * if (string.substring(string.length() - 2, string.length())
	 * .equals(one.substring(one.length() - 2, one.length())) && i < n) { i++;
	 * returnList.add(string); iterator.remove(); } else if
	 * (string.substring(string.length() - 2, string.length())
	 * .equals(one.substring(one.length() - 2, one.length())) && i == n) {
	 * iterator.remove(); } } }
	 * 
	 * System.out.println(paiList.size()); System.out.println("获得炸弹：" + returnList);
	 * return returnList;
	 * 
	 * }
	 */

	// 获得顺子
	public List<String> getShunzi() {
		Random r = new Random();
		String one = null;
		String onePai = null;
		while (true) {
			one = paiList.get(0);
			onePai = one.length() == 3 ? one.substring(one.length() - 1, one.length())
					: one.substring(one.length() - 2, one.length());
			if (!one.startsWith("5") && Integer.parseInt(onePai) < 11) {
				break;
			} else {
				paiList.remove(0);
			}
		}
		paiList.remove(one);
		int onepai = Integer.parseInt(onePai);
		String two = (r.nextInt(4) + 1) + "-" + (onepai + 1);
		String three = (r.nextInt(4) + 1) + "-" + (onepai + 2);
		String four = (r.nextInt(4) + 1) + "-" + (onepai + 3);
		String five = null;
		if (one.substring(one.length() - 2, one.length()).equals("10")) {
			five = (r.nextInt(4) + 1) + "-" + 1;
		} else {
			five = (r.nextInt(4) + 1) + "-" + (onepai + 4);
		}
		List<String> returnList = new ArrayList<String>();
		returnList.add(one);
		returnList.add(two);
		returnList.add(three);
		returnList.add(four);
		returnList.add(five);
		// 移除顺子中用到的数字
		Iterator<String> iterator = paiList.iterator();
		for (int i = 0; i < returnList.size(); i++) {
			while (iterator.hasNext()) {
				String string = (String) iterator.next();
				if (returnList.get(i).length() > 3
						&& returnList.get(i).substring(returnList.get(i).length() - 2, returnList.get(i).length())
								.equals(string.substring(string.length() - 2, string.length()))) {
					iterator.remove();
				} else if (returnList.get(i).length() == 3
						&& returnList.get(i).substring(returnList.get(i).length() - 1, returnList.get(i).length())
								.equals(string.substring(string.length() - 1, string.length()))) {
					iterator.remove();
				}
			}
		}
		System.out.println("获得顺子：" + returnList);
		return returnList;
	}

	// 获得同花顺
	public List<String> getTonghuaShunzi() {
		Random r = new Random();
		String one = null;
		String onePai = null;
		while (true) {
			one = paiList.get(0);
			onePai = one.length() == 3 ? one.substring(one.length() - 1, one.length())
					: one.substring(one.length() - 2, one.length());
			if (!one.startsWith("5") && Integer.parseInt(onePai) < 11) {
				break;
			} else {
				paiList.remove(0);
			}
		}
		paiList.remove(one);
		int onepai = Integer.parseInt(onePai);
		String a = one.substring(0, 1);
		String two = a + "-" + (onepai + 1);
		String three = a + "-" + (onepai + 2);
		String four = a + "-" + (onepai + 3);
		String five = null;
		if (one.substring(one.length() - 2, one.length()).equals("10")) {
			five = a + "-" + 1;
		} else {
			five = a + "-" + (onepai + 4);
		}
		List<String> returnList = new ArrayList<String>();
		returnList.add(one);
		returnList.add(two);
		returnList.add(three);
		returnList.add(four);
		returnList.add(five);
		// 移除顺子中用到的数字
		Iterator<String> iterator = paiList.iterator();
		for (int i = 0; i < returnList.size(); i++) {
			while (iterator.hasNext()) {
				String string = (String) iterator.next();
				if (returnList.get(i).length() > 3
						&& returnList.get(i).substring(returnList.get(i).length() - 2, returnList.get(i).length())
								.equals(string.substring(string.length() - 2, string.length()))) {
					iterator.remove();
				} else if (returnList.get(i).length() == 3
						&& returnList.get(i).substring(returnList.get(i).length() - 1, returnList.get(i).length())
								.equals(string.substring(string.length() - 1, string.length()))) {
					iterator.remove();
				}
			}
		}
		System.out.println("获得同花顺：" + returnList);
		return returnList;
	}

	// 获得同花
	public List<String> getTonghua() {

		String one = null;
		while (true) {
			one = paiList.get(0);
			if (!one.startsWith("5")) {
				break;
			} else {
				paiList.remove(0);
			}
		}
		String startPai = one.substring(0, 1);
		List<String> returnList = new ArrayList<String>();
		Iterator<String> iterator = paiList.iterator();
		int i = 0;
		while (iterator.hasNext()) {
			String string = (String) iterator.next();
			if (string.substring(0, 1).equals(startPai) && i < 5) {
				i++;
				returnList.add(string);
				iterator.remove();
			} else if (string.substring(0, 1).equals(startPai) && i == 5) {
				break;
			}
		}
		System.out.println(paiList.size());
		System.out.println("获得同花：" + returnList);
		return returnList;

	}

	// 获得三条 或者炸弹 或者五同
	public List<String> getRepeat(int n) {
		String one = null;
		while (true) {
			one = paiList.get(0);
			if (!one.startsWith("5")) {
				break;
			} else {
				paiList.remove(0);
			}
		}
		paiList.remove(one);
		List<String> returnList = new ArrayList<String>();
		returnList.add(one);
		Iterator<String> iterator = paiList.iterator();
		int i = 0;
		while (iterator.hasNext()) {
			String string = (String) iterator.next();
			if (one.length() == 3 && string.length() == 3) {
				if (string.substring(string.length() - 1, string.length())
						.equals(one.substring(one.length() - 1, one.length())) && i < n && !string.startsWith("5")) {
					i++;
					returnList.add(string);
					iterator.remove();
				} else if (string.substring(string.length() - 1, string.length())
						.equals(one.substring(one.length() - 1, one.length())) && i == n) {
					iterator.remove();
				}
			} else if (one.length() == 4 && string.length() == 4) {
				if (string.substring(string.length() - 2, string.length())
						.equals(one.substring(one.length() - 2, one.length())) && i < n && !string.startsWith("5")) {
					i++;
					returnList.add(string);
					iterator.remove();
				} else if (string.substring(string.length() - 2, string.length())
						.equals(one.substring(one.length() - 2, one.length())) && i == n) {
					iterator.remove();
				}
			}
		}

		System.out.println(paiList.size());
		System.out.println("获得五同：" + returnList);
		return returnList;

	}

	// 获得双顺子
	public List<String> getDoubleShunzi() {
		String one1 = null;
		String one2 = null;
		String onepai1 = null;
		String onepai2 = null;
		while (true) {

			onepai1 = paiList.get(0).length() == 3
					? paiList.get(0).substring(paiList.get(0).length() - 1, paiList.get(0).length())
					: paiList.get(0).substring(paiList.get(0).length() - 2, paiList.get(0).length());
			if (paiList.get(0).startsWith("5")) {
				paiList.remove(0);
			} else {
				if (Integer.parseInt(onepai1) < 11) {
					one1 = paiList.get(0);
					break;
				} else {
					paiList.remove(0);
				}
			}
		}
		paiList.remove(one1);
		System.out.println(paiList);
		while (true) {
			onepai2 = paiList.get(0).length() == 3
					? paiList.get(0).substring(paiList.get(0).length() - 1, paiList.get(0).length())
					: paiList.get(0).substring(paiList.get(0).length() - 2, paiList.get(0).length());
			if (paiList.get(0).startsWith("5")) {
				paiList.remove(0);
			} else {
				if (Integer.parseInt(onepai2) < 11) {
					one2 = paiList.get(0);
					break;
				} else {
					paiList.remove(0);

				}
			}
		}
		paiList.remove(one2);
		Random r = new Random();
		int onepai11 = Integer.parseInt(onepai1);
		String two11 = (r.nextInt(4) + 1) + "-" + (onepai11 + 1);
		String three11 = (r.nextInt(4) + 1) + "-" + (onepai11 + 2);
		String four11 = (r.nextInt(4) + 1) + "-" + (onepai11 + 3);
		String five11 = null;
		if (one1.substring(one1.length() - 2, one1.length()).equals("10")) {
			five11 = (r.nextInt(4) + 1) + "-" + 1;
		} else {
			five11 = (r.nextInt(4) + 1) + "-" + (onepai11 + 4);
		}

		int onepai22 = Integer.parseInt(onepai2);
		String two22 = (r.nextInt(4) + 1) + "-" + (onepai22 + 1);
		String three22 = (r.nextInt(4) + 1) + "-" + (onepai22 + 2);
		String four22 = (r.nextInt(4) + 1) + "-" + (onepai22 + 3);
		String five22 = null;
		if (one2.substring(one2.length() - 2, one2.length()).equals("10")) {
			five22 = (r.nextInt(4) + 1) + "-" + 1;
		} else {
			five22 = (r.nextInt(4) + 1) + "-" + (onepai22 + 4);
		}

		List<String> shunzi1 = new ArrayList<String>();
		List<String> shunzi2 = new ArrayList<String>();
		shunzi1.add(one1);
		shunzi1.add(two11);
		shunzi1.add(three11);
		shunzi1.add(four11);
		shunzi1.add(five11);
		shunzi2.add(one2);
		shunzi2.add(two22);
		shunzi2.add(three22);
		shunzi2.add(four22);
		shunzi2.add(five22);
		// 防止两个顺子中有重复的牌 然后花色一样
		for (int i = 0; i < shunzi1.size(); i++) {
			for (int j = 0; j < shunzi2.size(); j++) {
				if (shunzi1.get(i).equals(shunzi2.get(j))) {
					String q = shunzi2.get(j).substring(0, 1);
					int s = Integer.parseInt(q);
					if (s < 4) {
						System.out.println(s);
						String newq = (s + 1) + "-" + (shunzi2.get(j).length() == 3
								? shunzi2.get(j).substring(shunzi2.get(j).length() - 1, shunzi2.get(j).length())
								: shunzi2.get(j).substring(shunzi2.get(j).length() - 2, shunzi2.get(j).length()));
						Collections.replaceAll(shunzi2, q, newq);
					} else {
						String newq = (s - 1) + "-" + (shunzi2.get(j).length() == 3
								? shunzi2.get(j).substring(shunzi2.get(j).length() - 1, shunzi2.get(j).length())
								: shunzi2.get(j).substring(shunzi2.get(j).length() - 2, shunzi2.get(j).length()));
						Collections.replaceAll(shunzi2, q, newq);
					}
				}
			}
		}

		List<String> returnList = new ArrayList<String>();
		returnList.addAll(shunzi1);
		returnList.addAll(shunzi2);
		Iterator<String> iterator = paiList.iterator();
		for (int i = 0; i < returnList.size(); i++) {
			while (iterator.hasNext()) {
				String string = (String) iterator.next();
				if (string.equals(returnList.get(i))) {
					iterator.remove();
				}
			}
		}
		System.out.println("双顺子：" + returnList);
		return returnList;
	}

	// 获得顺子+同花顺
	public List<String> getShunziAndTonghuashun() {
		String one1 = null;
		String one2 = null;
		String onepai1 = null;
		String onepai2 = null;
		while (true) {

			onepai1 = paiList.get(0).length() == 3
					? paiList.get(0).substring(paiList.get(0).length() - 1, paiList.get(0).length())
					: paiList.get(0).substring(paiList.get(0).length() - 2, paiList.get(0).length());
			if (paiList.get(0).startsWith("5")) {
				paiList.remove(0);
			} else {
				if (Integer.parseInt(onepai1) < 11) {
					one1 = paiList.get(0);
					break;
				} else {
					paiList.remove(0);
				}
			}
		}
		paiList.remove(one1);
		System.out.println(paiList);
		while (true) {
			onepai2 = paiList.get(0).length() == 3
					? paiList.get(0).substring(paiList.get(0).length() - 1, paiList.get(0).length())
					: paiList.get(0).substring(paiList.get(0).length() - 2, paiList.get(0).length());
			if (paiList.get(0).startsWith("5")) {
				paiList.remove(0);
			} else {
				if (Integer.parseInt(onepai2) < 11) {
					one2 = paiList.get(0);
					break;
				} else {
					paiList.remove(0);

				}
			}
		}
		paiList.remove(one2);
		Random r = new Random();
		int onepai11 = Integer.parseInt(onepai1);
		String tonghua = one1.substring(0, 1);
		String two11 = tonghua + "-" + (onepai11 + 1);
		String three11 = tonghua + "-" + (onepai11 + 2);
		String four11 = tonghua + "-" + (onepai11 + 3);
		String five11 = null;
		if (one1.substring(one1.length() - 2, one1.length()).equals("10")) {
			five11 = tonghua + "-" + 1;
		} else {
			five11 = tonghua + "-" + (onepai11 + 4);
		}

		int onepai22 = Integer.parseInt(onepai2);
		String two22 = (r.nextInt(4) + 1) + "-" + (onepai22 + 1);
		String three22 = (r.nextInt(4) + 1) + "-" + (onepai22 + 2);
		String four22 = (r.nextInt(4) + 1) + "-" + (onepai22 + 3);
		String five22 = null;
		if (one2.substring(one2.length() - 2, one2.length()).equals("10")) {
			five22 = (r.nextInt(4) + 1) + "-" + 1;
		} else {
			five22 = (r.nextInt(4) + 1) + "-" + (onepai22 + 4);
		}

		List<String> shunzi1 = new ArrayList<String>();
		List<String> shunzi2 = new ArrayList<String>();
		shunzi1.add(one1);
		shunzi1.add(two11);
		shunzi1.add(three11);
		shunzi1.add(four11);
		shunzi1.add(five11);
		shunzi2.add(one2);
		shunzi2.add(two22);
		shunzi2.add(three22);
		shunzi2.add(four22);
		shunzi2.add(five22);
		// 防止两个顺子中有重复的牌 然后花色一样
		for (int i = 0; i < shunzi1.size(); i++) {
			for (int j = 0; j < shunzi2.size(); j++) {
				if (shunzi1.get(i).equals(shunzi2.get(j))) {
					String q = shunzi2.get(j).substring(0, 1);
					int s = Integer.parseInt(q);
					if (s < 4) {
						System.out.println(s);
						String newq = (s + 1) + "-" + (shunzi2.get(j).length() == 3
								? shunzi2.get(j).substring(shunzi2.get(j).length() - 1, shunzi2.get(j).length())
								: shunzi2.get(j).substring(shunzi2.get(j).length() - 2, shunzi2.get(j).length()));
						Collections.replaceAll(shunzi2, q, newq);
					} else {
						String newq = (s - 1) + "-" + (shunzi2.get(j).length() == 3
								? shunzi2.get(j).substring(shunzi2.get(j).length() - 1, shunzi2.get(j).length())
								: shunzi2.get(j).substring(shunzi2.get(j).length() - 2, shunzi2.get(j).length()));
						Collections.replaceAll(shunzi2, q, newq);
					}
				}
			}
		}
		List<String> returnList = new ArrayList<String>();
		returnList.addAll(shunzi1);
		returnList.addAll(shunzi2);
		Iterator<String> iterator = paiList.iterator();
		for (int i = 0; i < returnList.size(); i++) {
			while (iterator.hasNext()) {
				String string = (String) iterator.next();
				if (string.equals(returnList.get(i))) {
					iterator.remove();
				}
			}
		}
		System.out.println("顺子+同花顺：" + returnList);
		return returnList;

	}

	public void test1() {
		// 定义一个花色数组 1黑桃，2红桃，3梅花，4方块
		String[] colors = { "1-", "2-", "3-", "4-" };

		// 定义一个点数数组
		String[] numbers = { "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "1" };

		List<String> pai = new ArrayList<>();
		// 黑桃
		List<String> oneFlower = new ArrayList<>();
		// 红心
		List<String> twoFlower = new ArrayList<>();
		// 梅花
		List<String> threeFlower = new ArrayList<>();
		// 方块
		List<String> FourFlower = new ArrayList<>();
		for (String number : numbers) {
			for (String color : colors) {
				String poker = color.concat(number);
				pai.add(poker);
				if ("1-".equals(color)) {
					oneFlower.add(poker);
				} else if ("2-".equals(color)) {
					twoFlower.add(poker);
				} else if ("3-".equals(color)) {
					threeFlower.add(poker);
				} else if ("4-".equals(color)) {
					FourFlower.add(poker);
				}
			}
		}
		pai.addAll(oneFlower);
		pai.addAll(twoFlower);
		pai.addAll(threeFlower);
		pai.addAll(FourFlower);
		pai.addAll(oneFlower);
		pai.addAll(twoFlower);
		pai.addAll(threeFlower);
		pai.addAll(FourFlower);
		pai.addAll(oneFlower);
		pai.add("5-0");
		pai.add("5-0");
		pai.add("5-1");
		pai.add("5-1");
		Collections.shuffle(pai);
		QTHServiceImpl impl = new QTHServiceImpl(pai);
		System.out.println(impl.getPaiPai());
	}

	// win
	public String[] getPaiPai() {
		/*
		 * list中存放顺序为 0.五同 1.顺子+顺子 2.顺子+同花顺 3.顺子 4.同花顺 5.同花 6.炸弹 7.三条 8.对子 9.单
		 */
		Random r = new Random();
		int a = r.nextInt(31) + 1;
		int arr[] = map.get(a);
		List<String> list1 = new ArrayList<String>();
		List<String> list2 = new ArrayList<String>();
		List<String> list3 = new ArrayList<String>();
		List<String> list4 = new ArrayList<String>();
		List<String> list5 = new ArrayList<String>();
		List<String> list6 = new ArrayList<String>();
		List<String> list7 = new ArrayList<String>();
		List<String> list8 = new ArrayList<String>();
		List<String> list9 = new ArrayList<String>();
		List<String> list10 = new ArrayList<String>();
		List<String> returnList = new ArrayList<String>();
		for (int i = 0; i < arr[0]; i++) {
			list1.addAll(getRepeat(4));
		}
		for (int i = 0; i < arr[1]; i++) {
			/*
			 * List<String> dbsz = getDoubleShunzi(); int c = r.nextInt(100)+1; if (c<40) {
			 * Collections.replaceAll(dbsz, dbsz.get(0), r.nextInt(2)==0?"5-0":"5-1"); }else
			 * if (c>=40 && c < 65) { Collections.replaceAll(dbsz, dbsz.get(0),
			 * r.nextInt(2)==0?"5-0":"5-1"); Collections.replaceAll(dbsz, dbsz.get(1),
			 * r.nextInt(2)==0?"5-0":"5-1"); }
			 */
			list2.addAll(getDoubleShunzi());
		}
		for (int i = 0; i < arr[2]; i++) {
			/*
			 * List<String> szaths = getShunziAndTonghuashun(); int c = r.nextInt(100)+1; if
			 * (c<40) { Collections.replaceAll(szaths, szaths.get(0),
			 * r.nextInt(2)==0?"5-0":"5-1"); }else if (c>=40 && c < 65) {
			 * Collections.replaceAll(szaths, szaths.get(0), r.nextInt(2)==0?"5-0":"5-1");
			 * Collections.replaceAll(szaths, szaths.get(1), r.nextInt(2)==0?"5-0":"5-1"); }
			 */
			list3.addAll(getShunziAndTonghuashun());
		}
		for (int i = 0; i < arr[3]; i++) {
			list4.addAll(getShunzi());
		}
		for (int i = 0; i < arr[4]; i++) {
			list5.addAll(getTonghuaShunzi());
		}
		for (int i = 0; i < arr[5]; i++) {
			list6.addAll(getTonghua());
		}
		for (int i = 0; i < arr[6]; i++) {
			list7.addAll(getRepeat(3));
		}
		for (int i = 0; i < arr[7]; i++) {
			list8.addAll(getRepeat(2));
		}
		for (int i = 0; i < arr[8]; i++) {
			list9.addAll(getDuizi());
		}
		for (int i = 0; i < arr[9]; i++) {
			list10.add(getDan());
		}
		returnList.addAll(list10);
		returnList.addAll(list9);
		returnList.addAll(list8);
		returnList.addAll(list7);
		returnList.addAll(list6);
		returnList.addAll(list5);
		returnList.addAll(list4);
		returnList.addAll(list3);
		returnList.addAll(list2);
		returnList.addAll(list1);

		System.out.println("换鬼之前的牌；" + returnList);
		int c = r.nextInt(99) + 1;
		if (c < 40) {
			System.out.println("加1鬼");
			returnList.set(r.nextInt(13), r.nextInt(2) == 0 ? "5-0" : "5-1");
		} else if (c >= 40 && c < 65) {
			System.out.println("加2鬼");
			returnList.set(r.nextInt(13), r.nextInt(2) == 0 ? "5-0" : "5-1");
			returnList.set(r.nextInt(13), r.nextInt(2) == 0 ? "5-0" : "5-1");
		}
		System.out.println("发牌：" + returnList);

		String userPai[] = new String[returnList.size()];
		returnList.toArray(userPai);
		return userPai;
	}

	public String[] getPaiPai2() {
		/*
		 * list中存放顺序为 0.五同 1.顺子+顺子 2.顺子+同花顺 3.顺子 4.同花顺 5.同花 6.炸弹 7.三条 8.对子 9.单
		 */
		int a = 32;
		int arr[] = map.get(a);
		List<String> list1 = new ArrayList<String>();
		List<String> list2 = new ArrayList<String>();
		List<String> list3 = new ArrayList<String>();
		List<String> list4 = new ArrayList<String>();
		List<String> list5 = new ArrayList<String>();
		List<String> list6 = new ArrayList<String>();
		List<String> list7 = new ArrayList<String>();
		List<String> list8 = new ArrayList<String>();
		List<String> list9 = new ArrayList<String>();
		List<String> list10 = new ArrayList<String>();
		List<String> returnList = new ArrayList<String>();
		for (int i = 0; i < arr[0]; i++) {
			list1.addAll(getRepeat(4));
		}
		for (int i = 0; i < arr[1]; i++) {
			/*
			 * List<String> dbsz = getDoubleShunzi(); int c = r.nextInt(100)+1; if (c<40) {
			 * Collections.replaceAll(dbsz, dbsz.get(0), r.nextInt(2)==0?"5-0":"5-1"); }else
			 * if (c>=40 && c < 65) { Collections.replaceAll(dbsz, dbsz.get(0),
			 * r.nextInt(2)==0?"5-0":"5-1"); Collections.replaceAll(dbsz, dbsz.get(1),
			 * r.nextInt(2)==0?"5-0":"5-1"); }
			 */
			list2.addAll(getDoubleShunzi());
		}
		for (int i = 0; i < arr[2]; i++) {
			/*
			 * List<String> szaths = getShunziAndTonghuashun(); int c = r.nextInt(100)+1; if
			 * (c<40) { Collections.replaceAll(szaths, szaths.get(0),
			 * r.nextInt(2)==0?"5-0":"5-1"); }else if (c>=40 && c < 65) {
			 * Collections.replaceAll(szaths, szaths.get(0), r.nextInt(2)==0?"5-0":"5-1");
			 * Collections.replaceAll(szaths, szaths.get(1), r.nextInt(2)==0?"5-0":"5-1"); }
			 */
			list3.addAll(getShunziAndTonghuashun());
		}
		for (int i = 0; i < arr[3]; i++) {
			list4.addAll(getShunzi());
		}
		for (int i = 0; i < arr[4]; i++) {
			list5.addAll(getTonghuaShunzi());
		}
		for (int i = 0; i < arr[5]; i++) {
			list6.addAll(getTonghua());
		}
		for (int i = 0; i < arr[6]; i++) {
			list7.addAll(getRepeat(3));
		}
		for (int i = 0; i < arr[7]; i++) {
			list8.addAll(getRepeat(2));
		}
		for (int i = 0; i < arr[8]; i++) {
			list9.addAll(getDuizi());
		}
		for (int i = 0; i < arr[9]; i++) {
			list10.add(getDan());
		}
		returnList.addAll(list10);
		returnList.addAll(list9);
		returnList.addAll(list8);
		returnList.addAll(list7);
		returnList.addAll(list6);
		returnList.addAll(list5);
		returnList.addAll(list4);
		returnList.addAll(list3);
		returnList.addAll(list2);
		returnList.addAll(list1);

		System.out.println("发牌：" + returnList);
		String userPai[] = new String[returnList.size()];
		returnList.toArray(userPai);
		return userPai;
	}

	public static void main(String[] args) {
		// 定义一个花色数组 1黑桃，2红桃，3梅花，4方块
		String[] colors = { "1-", "2-", "3-", "4-" };

		// 定义一个点数数组
		String[] numbers = { "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "1" };

		List<String> pai = new ArrayList<>();
		// 黑桃
		List<String> oneFlower = new ArrayList<>();
		// 红心
		List<String> twoFlower = new ArrayList<>();
		// 梅花
		List<String> threeFlower = new ArrayList<>();
		// 方块
		List<String> FourFlower = new ArrayList<>();
		for (String number : numbers) {
			for (String color : colors) {
				String poker = color.concat(number);
				pai.add(poker);
				if ("1-".equals(color)) {
					oneFlower.add(poker);
				} else if ("2-".equals(color)) {
					twoFlower.add(poker);
				} else if ("3-".equals(color)) {
					threeFlower.add(poker);
				} else if ("4-".equals(color)) {
					FourFlower.add(poker);
				}
			}
		}
		pai.addAll(oneFlower);
		pai.addAll(twoFlower);
		pai.addAll(threeFlower);
		pai.addAll(FourFlower);

		pai.addAll(oneFlower);
		Collections.shuffle(pai);
		System.out.println(pai.size());
		System.out.println(pai);
		QTHServiceImpl impl = new QTHServiceImpl(pai);
		System.out.println("最终合成吊炸天牌型：" + impl.getPaiPai2());
		// System.out.println(impl.getRepeat(2));

	}

}
