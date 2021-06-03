package com.zhuoan.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.zhuoan.biz.model.pdk.PDKPacker;

/*
 * 仓促版本
 * 先用着
 * 日后优化
 * 先上线再说
 * 
 */
public class PDKKServiceImpl {

	private List<String> paiList = new ArrayList<String>();

	private Map<Integer, int[]> map = new ConcurrentHashMap<>();

	private List<String> paiList2;

	PDKKServiceImpl() {

		// 定义一个花色数组 4黑桃，3红桃，2梅花，1方块
		String[] colors = { "1-", "2-", "3-", "4-" };
		// 16张玩法：去掉红桃2，方块2，梅花2和方块A共48张牌。
		// 定义一个点数数组
		String[] numbers = { "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15" };
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

		pai.remove("4-15");
		pai.remove("2-15");
		pai.remove("3-15");
		pai.remove("4-14");
		Collections.shuffle(pai);

		this.paiList2 = new ArrayList<>();
		this.paiList = pai;

		// 1炸弹 2三条 1对 3单 1二
		// 7顺子 2连对 1 个 2 三条 1个单
		// 3飞机带6单 1个2
		// 7顺子 8顺子 1个A
		// 3个A 1 个2 3连对子 6张顺子
		// 1炸弹 7顺子 5顺子
		// 2炸弹 6单 1 A 1 2

		// 1炸弹 2三条 1对 3单 1二
		map.put(2, new int[] { 0, 0, 0, 0, 0, 0, 1, 1, 1, 3, 1 });
		// 1炸弹 1顺子 2对 2单 1二
		map.put(3, new int[] { 0, 0, 0, 1, 0, 0, 1, 0, 2, 2, 1 });
		// 2炸弹 3单 2对 1二
		map.put(4, new int[] { 0, 0, 0, 0, 0, 0, 2, 0, 2, 3, 1 });
		// 1炸弹 2三条 1对 3单 1二
		map.put(5, new int[] { 0, 0, 0, 0, 0, 0, 1, 2, 1, 3, 1 });
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

	public String getTwo() {
		paiList.remove("1-15");
		System.out.println("获取2");
		return "1-15";
	}

	// 1炸弹 2三条 1对 3单 1二

	public List<String> one() {
		List<String> list = new ArrayList<String>();
		// 添加1个2
		list.add("1-15");
		paiList.remove("1-15");
		String zhadanOne = "";
		String santiao1 = "";
		String santiao2 = "";
		// 添加 1 个炸弹
		for (int i = 0; i < paiList.size(); i++) {
			if (paiList.get(i).endsWith("14") || paiList.get(i).endsWith("15")) {
				continue;
			}
			zhadanOne = paiList.get(i);
			break;
		}
		list.add("1-" + zhadanOne.substring(2, zhadanOne.length()));
		list.add("2-" + zhadanOne.substring(2, zhadanOne.length()));
		list.add("3-" + zhadanOne.substring(2, zhadanOne.length()));
		list.add("4-" + zhadanOne.substring(2, zhadanOne.length()));
		paiList.remove("1-" + zhadanOne.substring(2, zhadanOne.length()));
		paiList.remove("2-" + zhadanOne.substring(2, zhadanOne.length()));
		paiList.remove("3-" + zhadanOne.substring(2, zhadanOne.length()));
		paiList.remove("4-" + zhadanOne.substring(2, zhadanOne.length()));

		// 添加第一个三条
		for (int i = 0; i < paiList.size(); i++) {
			if (paiList.get(i).endsWith("14")) {
				continue;
			}
			santiao1 = paiList.get(i);
			break;
		}
		list.add("1-" + santiao1.substring(2, santiao1.length()));
		list.add("2-" + santiao1.substring(2, santiao1.length()));
		list.add("4-" + santiao1.substring(2, santiao1.length()));
		paiList.remove("1-" + santiao1.substring(2, santiao1.length()));
		paiList.remove("2-" + santiao1.substring(2, santiao1.length()));
		paiList.remove("3-" + santiao1.substring(2, santiao1.length()));
		paiList.remove("4-" + santiao1.substring(2, santiao1.length()));

		// 添加第二个三条
		for (int i = 0; i < paiList.size(); i++) {
			if (paiList.get(i).endsWith("14") && !paiList.get(i).substring(2, paiList.get(i).length())
					.equals(santiao1.substring(2, santiao1.length()))) {
				continue;
			}
			santiao2 = paiList.get(i);
			break;
		}
		list.add("2-" + santiao2.substring(2, santiao2.length()));
		list.add("3-" + santiao2.substring(2, santiao2.length()));
		list.add("4-" + santiao2.substring(2, santiao2.length()));
		paiList.remove("1-" + santiao2.substring(2, santiao2.length()));
		paiList.remove("2-" + santiao2.substring(2, santiao2.length()));
		paiList.remove("3-" + santiao2.substring(2, santiao2.length()));
		paiList.remove("4-" + santiao2.substring(2, santiao2.length()));

		// 获取对子
		String duizi = paiList.get(0);
		list.add("1-" + duizi.substring(2, duizi.length()));
		list.add("3-" + duizi.substring(2, duizi.length()));
		paiList.remove("1-" + duizi.substring(2, duizi.length()));
		paiList.remove("2-" + duizi.substring(2, duizi.length()));
		paiList.remove("3-" + duizi.substring(2, duizi.length()));
		paiList.remove("4-" + duizi.substring(2, duizi.length()));

		// 三张单 先用13水的方法

		list.add(getDan());
		list.add(getDan());
		list.add(getDan());

		return list;

	}

	// 3飞机带6单 1个2
	public List<String> two() {
		List<String> list = new ArrayList<String>();
		list.add("1-15");
		paiList.remove("1-15");
		String oneFeiji = "";
		// 确认起始飞机的数字
		for (int i = 0; i < paiList.size(); i++) {
			if (!paiList.get(i).endsWith("12") && !paiList.get(i).endsWith("13") && !paiList.get(i).endsWith("14")) {
				oneFeiji = paiList.get(i);
			}
		}
		// 添加第一个三条
		list.add("2-" + oneFeiji.substring(2, oneFeiji.length()));
		list.add("3-" + oneFeiji.substring(2, oneFeiji.length()));
		list.add("4-" + oneFeiji.substring(2, oneFeiji.length()));
		paiList.remove("1-" + oneFeiji.substring(2, oneFeiji.length()));
		paiList.remove("2-" + oneFeiji.substring(2, oneFeiji.length()));
		paiList.remove("3-" + oneFeiji.substring(2, oneFeiji.length()));
		paiList.remove("4-" + oneFeiji.substring(2, oneFeiji.length()));
		// 添加第二个三条
		String a = oneFeiji.substring(2, oneFeiji.length());
		int towSt = Integer.parseInt(a);
		towSt += 1;
		list.add("1-" + towSt);
		list.add("2-" + towSt);
		list.add("3-" + towSt);
		paiList.remove("1-" + towSt);
		paiList.remove("2-" + towSt);
		paiList.remove("3-" + towSt);
		paiList.remove("4-" + towSt);
		towSt += 1;
		// 添加第三个三条
		list.add("2-" + towSt);
		list.add("3-" + towSt);
		list.add("4-" + towSt);
		paiList.remove("1-" + towSt);
		paiList.remove("2-" + towSt);
		paiList.remove("3-" + towSt);
		paiList.remove("4-" + towSt);
		for (int i = 0; i < 6; i++) {
			list.add(getDan());
		}
		return list;

	}

	// 7顺子 8顺子 1个A

	// 2炸弹 6单 1 A 1 2
	public void remove(int onePai) {
		paiList.remove("1-" + onePai);
		paiList.remove("2-" + onePai);
		paiList.remove("3-" + onePai);
		paiList.remove("4-" + onePai);
	}

	// 写不出来 先搁置
	/*
	 * public List<String> three(){ List<String> list = new ArrayList<String>(); //1
	 * 个 2 //list.add("1-15"); paiList.remove("1-15"); //顺子 Random r = new Random();
	 * int onePai = r.nextInt(6) + 3; list.add((r.nextInt(4) + 1)+"-"+ onePai);
	 * remove(onePai); list.add((r.nextInt(4) + 1)+"-"+ ++onePai); remove(onePai);
	 * list.add((r.nextInt(4) + 1)+"-"+ ++onePai); remove(onePai);
	 * list.add((r.nextInt(4) + 1)+"-"+ ++onePai); remove(onePai); list.add(1+"-"+
	 * ++onePai); list.add(2+"-"+ onePai); remove(onePai); list.add((r.nextInt(4) +
	 * 1)+"-"+ ++onePai); remove(onePai); list.add((r.nextInt(4) + 1)+"-"+
	 * ++onePai); remove(onePai);
	 * 
	 * list.add("1-15");
	 * 
	 * 
	 * for (int i = 0; i < paiList.size(); i++) { int one =
	 * Integer.parseInt(paiList.get(i).substring(2, paiList.get(i).length())) ;
	 * one++; for (int j = 1; j < paiList.size(); j++) {
	 * 
	 * } }
	 * 
	 * //3 4 5 6 7 8 9 10 11 12 13 A
	 * 
	 * return list; }
	 */

	// 3个A 1 个2 3连对子 6张顺子
	public List<String> three() {
		List<String> list = new ArrayList<String>();
		list.add("2-14");
		list.add("3-14");
		list.add("1-14");
		list.add("1-15");
		Random r = new Random();
		int start = r.nextInt(9) + 3;
		list.add("1-" + start);
		list.add("3-" + start);
		list.add("2-" + ++start);
		list.add("4-" + start);
		list.add("1-" + ++start);
		list.add("4-" + start);

		int start2 = r.nextInt(6) + 3;
		String s1 = (r.nextInt(4) + 1) + "-" + start2;
		while (true) {

			if (!list.contains(s1)) {
				break;
			} else {
				System.out.println("花色保护");
				s1 = (r.nextInt(4) + 1) + "-" + start2;
			}

		}
		list.add(s1);
		String s2 = (r.nextInt(4) + 1) + "-" + ++start2;
		while (true) {

			if (!list.contains(s2)) {
				break;
			} else {
				System.out.println("花色保护");
				s2 = (r.nextInt(4) + 1) + "-" + start2;
			}

		}
		list.add(s2);
		String s3 = (r.nextInt(4) + 1) + "-" + ++start2;
		while (true) {
			if (!list.contains(s3)) {
				break;
			} else {
				System.out.println("花色保护");
				s3 = (r.nextInt(4) + 1) + "-" + start2;
			}

		}
		list.add(s3);
		String s4 = (r.nextInt(4) + 1) + "-" + ++start2;
		while (true) {

			if (!list.contains(s4)) {
				break;
			} else {
				System.out.println("花色保护");
				s4 = (r.nextInt(4) + 1) + "-" + start2;
			}

		}
		list.add(s4);
		String s5 = (r.nextInt(4) + 1) + "-" + ++start2;
		while (true) {

			if (!list.contains(s5)) {
				break;
			} else {
				System.out.println("花色保护");
				s5 = (r.nextInt(4) + 1) + "-" + start2;
			}

		}
		list.add(s5);
		String s6 = (r.nextInt(4) + 1) + "-" + ++start2;
		while (true) {

			if (!list.contains(s6)) {
				break;
			} else {
				System.out.println("花色保护");
				s6 = (r.nextInt(4) + 1) + "-" + start2;
			}

		}
		list.add(s6);
		return list;

	}

	// 1炸弹 7顺子 5顺子
	public List<String> four() {
		List<String> list = new ArrayList<String>();
		Random r = new Random();
		int onePai = r.nextInt(5) + 3;
		list.add((r.nextInt(4) + 1) + "-" + onePai);
		remove(onePai);
		list.add((r.nextInt(4) + 1) + "-" + ++onePai);
		remove(onePai);
		list.add((r.nextInt(4) + 1) + "-" + ++onePai);
		remove(onePai);
		list.add((r.nextInt(4) + 1) + "-" + ++onePai);
		remove(onePai);
		list.add((r.nextInt(4) + 1) + "-" + ++onePai);
		remove(onePai);
		list.add((r.nextInt(4) + 1) + "-" + ++onePai);
		remove(onePai);
		list.add((r.nextInt(4) + 1) + "-" + ++onePai);
		remove(onePai);

		int start2 = r.nextInt(6) + 3;
		String s1 = (r.nextInt(4) + 1) + "-" + start2;
		while (true) {
			if (!list.contains(s1)) {
				break;
			} else {
				s1 = (r.nextInt(4) + 1) + "-" + start2;
			}

		}
		remove(start2);
		list.add(s1);
		String s2 = (r.nextInt(4) + 1) + "-" + ++start2;
		while (true) {
			if (!list.contains(s2)) {
				break;
			} else {
				s2 = (r.nextInt(4) + 1) + "-" + start2;
			}

		}
		remove(start2);
		list.add(s2);
		String s3 = (r.nextInt(4) + 1) + "-" + ++start2;
		while (true) {

			if (!list.contains(s3)) {
				break;
			} else {
				s3 = (r.nextInt(4) + 1) + "-" + start2;
			}

		}
		remove(start2);
		list.add(s3);
		String s4 = (r.nextInt(4) + 1) + "-" + ++start2;
		while (true) {

			if (!list.contains(s4)) {
				break;
			} else {
				s4 = (r.nextInt(4) + 1) + "-" + start2;
			}

		}
		remove(start2);
		list.add(s4);
		String s5 = (r.nextInt(4) + 1) + "-" + ++start2;
		while (true) {
			if (!list.contains(s5)) {
				break;
			} else {
				s5 = (r.nextInt(4) + 1) + "-" + start2;
			}

		}
		remove(start2);
		list.add(s5);
		paiList.remove("1-14");
		paiList.remove("2-14");
		paiList.remove("3-14");
		paiList.remove("1-15");
		System.out.println("==="+this.paiList);
		if (this.paiList.size() == 0) {
			list.add("1-14");
			list.add("2-14");
			list.add("3-14");
			list.add("1-15");
		} else {
			String zd="";
			int i = 0;
			while (true) {
					try {
						if (i>=this.paiList.size()) {
							list.add("1-14");
							list.add("2-14");
							list.add("3-14");
							list.add("1-15");
							break;
						}
						zd = this.paiList.get(i).substring(2, this.paiList.get(i).length());
						if (!zd.equals("14") && !zd.equals("15")) {
							list.add("1-" + zd);
							list.add("2-" + zd);
							list.add("3-" + zd);
							list.add("4-" + zd);
							i=0;
							break;
						}
						i++;
					} catch (Exception e) {
						e.printStackTrace();
					}
				
			}
			
			
		}

		return list;

	}

	// 获得对子
	public List<String> getDuizi() {
		String one = paiList.get(0);
		if (one.equals("1-15")) {
			one = paiList.get(1);
		}

		paiList.remove(one);
		String two = "";
		int length = one.length();
		for (int i = 0; i < paiList.size(); i++) {
			if (paiList.get(i).endsWith(one.substring(2, length)) && paiList.get(i).length() == one.length()) {
				two = paiList.get(i);
			}
		}
		paiList.remove(two);
		List<String> list = new ArrayList<String>();
		list.add(one);
		list.add(two);
		return list;
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
			if (!one.endsWith("15")) {
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
		System.out.println("获得" + (n + 1) + "张一样的：" + returnList);
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
		String[] numbers = { "14", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "15" };

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
		Collections.shuffle(pai);
		QTHServiceImpl impl = new QTHServiceImpl(pai);
		System.out.println(impl.getPaiPai());
	}

	// win
	public List<PDKPacker> getPaiPai() {
		/*
		 * list中存放顺序为 0.五同 1.顺子+顺子 2.顺子+同花顺 3.顺子 4.同花顺 5.同花 6.炸弹 7.三条 8.对子 9.单
		 */
		Random r = new Random();
		int a = r.nextInt(5) + 1;
		int arr[] = map.get(a);
		System.out.println("a=" + a);
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
		for (int i = 0; i < arr[10]; i++) {
			list10.add(getTwo());
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
		PDKServiceImpl impl2 = new PDKServiceImpl();
		System.out.println(impl2.getPdkPacker(returnList));
		System.out.println("发牌：" + returnList);
		String userPai[] = new String[returnList.size()];
		returnList.toArray(userPai);
		return impl2.getPdkPacker(returnList);
	}

	public List<PDKPacker> getGoodPai() {
		Random r = new Random();
		int a = r.nextInt(4);
		List<String> goodList = new ArrayList<String>();
		switch (a) {
		case 0:
			goodList = one();
			break;
		case 1:
			goodList = two();
			break;
		case 2:
			goodList = three();
			break;
		case 3:
			goodList = four();
			break;
		default:
			goodList = one();
			break;
		}
		return PDKServiceImpl.getPdkPacker(goodList);
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
		for (int i = 0; i < 50; i++) {
		// 定义一个花色数组 4黑桃，3红桃，2梅花，1方块
		String[] colors = { "1-", "2-", "3-", "4-" };
		// 16张玩法：去掉红桃2，方块2，梅花2和方块A共48张牌。
		// 定义一个点数数组
		String[] numbers = { "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15" };
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

		pai.remove("2-15");  // 红
		pai.remove("3-15");  // 梅花
		pai.remove("4-15");   //方
		pai.remove("4-14");    //方
		Collections.shuffle(pai);
		
			PDKKServiceImpl impl = new PDKKServiceImpl();
			impl.paiList = pai;
			List<String> list = impl.four();
			List<PDKPacker> list2 = PDKServiceImpl.getPdkPacker(list);
			System.out.println(list2);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// System.out.println("最终合成吊炸天牌型："+impl.getPaiPai());
		// System.out.println(impl.getRepeat(2));
		// for (int i = 0; i < 30; i++) {
		// System.out.println(impl.getDuizi());
		// }
		// System.out.println(impl.paiList.contains(aaa.get(0)));
	}
	
	public List<PDKPacker> getLaji1(){
		List<String> list = new ArrayList<String>();
		Random r = new Random();
		//  33  4  55         77 8 99 11  12 A 
		int start = r.nextInt(2) + 3;  // 3 4
		list.add((r.nextInt(2)+1) +"-" + start);     
		list.add((r.nextInt(2)+3) +"-" + start);    
		list.add("2-" + ++start);
		list.add((r.nextInt(2)+1) +"-" + ++start);
		list.add((r.nextInt(2)+3) +"-"  + start);
		start++;
		list.add((r.nextInt(2)+1) +"-"+ ++start);
		list.add((r.nextInt(2)+3) +"-" + start);
		list.add("2-" + ++start);
		list.add((r.nextInt(2)+1) +"-"+  ++start);
		list.add((r.nextInt(2)+3) +"-" + start);
		start++;
		list.add((r.nextInt(4)+1)+"-"+ ++start);
		list.add((r.nextInt(4)+1)+"-"+ ++start);
		list.add((r.nextInt(3)+1)+"-"+ "14");
		
		int one =  r.nextInt(4)+3;
		String s1 = (r.nextInt(4) + 1) + "-" + one;
		while (true) {
			if (!list.contains(s1)) {
				break;
			} else {
				System.out.println("花色保护");
				s1 = (r.nextInt(4) + 1) + "-" + one;
			}
		}
		
		
		int two =  r.nextInt(4)+3;
		String s2 = (r.nextInt(4) + 1) + "-" + two;
		while (true) {
			if (!list.contains(s2)) {
				break;
			} else {
				System.out.println("花色保护");
				s2 = (r.nextInt(4) + 1) + "-" + two;
			}
		}
		list.add(s2);
		
		
		int three =  r.nextInt(3)+9;
		String s3 = (r.nextInt(4) + 1) + "-" + three;
		while (true) {
			if (!list.contains(s3)) {
				break;
			} else {
				System.out.println("花色保护");
				s3 = (r.nextInt(4) + 1) + "-" + three;
			}
		}
		list.add(s3);
		return PDKServiceImpl.getPdkPacker(list);
	}
	
	
	
	public List<PDKPacker> getLaji2(){
		List<String> list = new ArrayList<String>();
		Random r = new Random();
		//  33  4  55         77 8 99 11  12 A 
		int start = r.nextInt(2) + 3;  // 3 4
		list.add((r.nextInt(2)+1) +"-" + start);     
		list.add((r.nextInt(2)+3) +"-" + start);    
		list.add("2-" + ++start);
		list.add((r.nextInt(2)+1) +"-" + ++start);
		list.add((r.nextInt(2)+3) +"-"  + start);
		start++;
		list.add((r.nextInt(2)+1) +"-"+ ++start);
		list.add((r.nextInt(2)+3) +"-" + start);
		list.add("2-" + ++start);
		list.add((r.nextInt(2)+1) +"-"+  ++start);
		list.add((r.nextInt(2)+3) +"-" + start);
		start++;
		list.add((r.nextInt(4)+1)+"-"+ ++start);
		list.add((r.nextInt(4)+1)+"-"+ ++start);
		String k = (r.nextInt(4)+1)+"-"+ "13";
		
		while (true) {
			if (!list.contains(k)) {
				break;
			} else {
				System.out.println("花色保护");
				k = (r.nextInt(4) + 1) + "-" + "13";
			}
		}
		list.add(k);
		
		int one =  r.nextInt(4)+3;
		String s1 = (r.nextInt(4) + 1) + "-" + one;
		while (true) {
			if (!list.contains(s1)) {
				break;
			} else {
				System.out.println("花色保护");
				s1 = (r.nextInt(4) + 1) + "-" + one;
			}
		}
		
		
		int two =  r.nextInt(4)+3;
		String s2 = (r.nextInt(4) + 1) + "-" + two;
		while (true) {
			if (!list.contains(s2)) {
				break;
			} else {
				System.out.println("花色保护");
				s2 = (r.nextInt(4) + 1) + "-" + two;
			}
		}
		list.add(s2);
		
		
		int three =  r.nextInt(3)+9;
		String s3 = (r.nextInt(4) + 1) + "-" + three;
		while (true) {
			if (!list.contains(s3)) {
				break;
			} else {
				System.out.println("花色保护");
				s3 = (r.nextInt(4) + 1) + "-" + three;
			}
		}
		list.add(s3);
		return PDKServiceImpl.getPdkPacker(list);
	}
	
	
	public List<PDKPacker> getBad(){
		Random r = new Random();
		
		List<PDKPacker> list = new ArrayList<PDKPacker>();
		int a = r.nextInt(2);
		if (a==0) {
			list = getLaji1();
		}else {
			list = getLaji2();
		}
		
		
		
		return list;
	}
	

}
