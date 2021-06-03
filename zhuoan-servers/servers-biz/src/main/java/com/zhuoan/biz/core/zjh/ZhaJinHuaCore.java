
package com.zhuoan.biz.core.zjh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.RandomUtils;

public class ZhaJinHuaCore {
	public static int[] PAIS = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73};
	public static int[] PAIS_JQ = new int[]{1, 9, 10, 11, 12, 13, 21, 29, 30, 31, 32, 33, 41, 49, 50, 51, 52, 53, 61, 69, 70, 71, 72, 73};
	public static int TYPE_BAOZI = 106;
	public static int TYPE_TONGHUASHUN = 105;
	public static int TYPE_JINHUA = 104;
	public static int TYPE_SHUNZI = 103;
	public static int TYPE_DUIZI = 102;
	public static int TYPE_SANPAI = 101;
	public static int paiIndex = 0;

	public ZhaJinHuaCore() {
	}

	public static int getColor(int pai) {
		return pai / 20;
	}

	public static int getNumber(int pai) {
		return pai % 20;
	}

	public static boolean isBaoZi(List<Integer> paiList) {
		if (paiList.size() > 0 && paiList.size() == 3) {
			int p = getNumber((Integer)paiList.get(0));
			int p1 = getNumber((Integer)paiList.get(1));
			int p2 = getNumber((Integer)paiList.get(2));
			if (p == p1 && p == p2) {
				return true;
			}
		}

		return false;
	}

	public static boolean isTongHuaSun(List<Integer> paiList) {
		return isJinHua(paiList) && isSunZi(paiList);
	}

	public static boolean isJinHua(List<Integer> paiList) {
		if (paiList.size() > 0 && paiList.size() == 3) {
			Collections.sort(paiList);
			int p = getColor((Integer)paiList.get(0));
			int p1 = getColor((Integer)paiList.get(1));
			int p2 = getColor((Integer)paiList.get(2));
			if (p == p1 && p == p2) {
				return true;
			}
		}

		return false;
	}

	public static boolean isSunZi(List<Integer> paiList) {
		if (paiList.size() > 0 && paiList.size() == 3) {
			List<Integer> pais = new ArrayList();
			Iterator var2 = paiList.iterator();

			while(var2.hasNext()) {
				Integer pai = (Integer)var2.next();
				pais.add(getNumber(pai));
			}

			Collections.sort(pais);
			int p = (Integer)pais.get(0);
			int p1 = (Integer)pais.get(1) - 1;
			int p2 = (Integer)pais.get(2) - 2;
			if (p == p1 && p == p2) {
				return true;
			}

			if (p == 1 && p == p1 - 10 && p == p2 - 10) {
				return true;
			}
		}

		return false;
	}

	public static boolean isDuiZi(List<Integer> paiList) {
		if (paiList.size() > 0 && paiList.size() == 3) {
			List<Integer> pais = new ArrayList();
			Iterator var2 = paiList.iterator();

			while(true) {
				if (!var2.hasNext()) {
					Collections.sort(pais);
					int p = (Integer)pais.get(0);
					int p1 = (Integer)pais.get(1);
					int p2 = (Integer)pais.get(2);
					if (p == p1 && p != p2 || p1 == p2 && p != p2) {
						return true;
					}
					break;
				}

				Integer pai = (Integer)var2.next();
				pais.add(getNumber(pai));
			}
		}

		return false;
	}

	public static int getMaxNum(List<Integer> paiList) {
		if (paiList.size() > 0 && paiList.size() == 3) {
			List<Integer> pais = new ArrayList();
			Iterator var2 = paiList.iterator();

			while(var2.hasNext()) {
				Integer pai = (Integer)var2.next();
				pais.add(getNumber(pai));
			}

			Collections.sort(pais);
			return (Integer)pais.get(0) == 1 ? (Integer)pais.get(0) : (Integer)pais.get(2);
		} else {
			return 0;
		}
	}

	public static int getMaxPai(List<Integer> paiList) {
		int maxNum = getMaxNum(paiList);
		int maxPai = 0;

		for(int i = 0; i < paiList.size(); ++i) {
			int num = getNumber((Integer)paiList.get(i));
			if (num == maxNum && (Integer)paiList.get(i) > maxPai) {
				maxPai = (Integer)paiList.get(i);
			}
		}

		return maxPai;
	}

	public static int getPaiType(List<Integer> paiList) {
		if (isBaoZi(paiList)) {
			return TYPE_BAOZI;
		} else if (isTongHuaSun(paiList)) {
			return TYPE_TONGHUASHUN;
		} else if (isJinHua(paiList)) {
			return TYPE_JINHUA;
		} else if (isSunZi(paiList)) {
			return TYPE_SHUNZI;
		} else {
			return isDuiZi(paiList) ? TYPE_DUIZI : TYPE_SANPAI;
		}
	}

	public static int compare(List<Integer> paiA, List<Integer> paiB) {
		int aType = getPaiType(paiA);
		int bType = getPaiType(paiB);
		if (aType > bType) {
			return 1;
		} else if (aType < bType) {
			return -1;
		} else if (aType != bType) {
			return 0;
		} else if (aType != TYPE_DUIZI) {
			return compareDaXiao(paiA, paiB);
		} else {
			int duiziA = 0;
			int duiziB = 0;

			int i;
			int duizi;
			int j;
			for(i = 0; i < paiA.size() - 1; ++i) {
				duizi = 0;

				for(j = i + 1; j < paiA.size(); ++j) {
					if (getNumber((Integer)paiA.get(i)) == getNumber((Integer)paiA.get(j))) {
						++duizi;
					}
				}

				if (duizi != 0) {
					duiziA = (Integer)paiA.get(i);
					break;
				}
			}

			for(i = 0; i < paiB.size() - 1; ++i) {
				duizi = 0;

				for(j = i + 1; j < paiB.size(); ++j) {
					if (getNumber((Integer)paiB.get(i)) == getNumber((Integer)paiB.get(j))) {
						++duizi;
					}
				}

				if (duizi != 0) {
					duiziB = (Integer)paiB.get(i);
					break;
				}
			}

			if (compareNum(getNumber(duiziA), getNumber(duiziB)) > 0) {
				return 1;
			} else {
				return compareNum(getNumber(duiziA), getNumber(duiziB)) < 0 ? -1 : compareDaXiao(paiA, paiB);
			}
		}
	}

	public static int compareDaXiao(List<Integer> paiA, List<Integer> paiB) {
		List<Integer> list1 = new ArrayList();
		List<Integer> list2 = new ArrayList();
		Iterator var4 = paiA.iterator();

		Integer integer;
		while(var4.hasNext()) {
			integer = (Integer)var4.next();
			if (getNumber(integer) == 1) {
				list1.add(14);
			} else {
				list1.add(getNumber(integer));
			}
		}

		var4 = paiB.iterator();

		while(var4.hasNext()) {
			integer = (Integer)var4.next();
			if (getNumber(integer) == 1) {
				list2.add(14);
			} else {
				list2.add(getNumber(integer));
			}
		}

		Collections.sort(list1);
		Collections.sort(list2);
		if ((Integer)list1.get(2) > (Integer)list2.get(2)) {
			return 1;
		} else if ((Integer)list1.get(2) < (Integer)list2.get(2)) {
			return -1;
		} else if ((Integer)list1.get(1) > (Integer)list2.get(1)) {
			return 1;
		} else if ((Integer)list1.get(1) < (Integer)list2.get(1)) {
			return -1;
		} else if ((Integer)list1.get(0) > (Integer)list2.get(0)) {
			return 1;
		} else if ((Integer)list1.get(0) < (Integer)list2.get(0)) {
			return -1;
		} else {
			return 0;
		}
	}

	public static int compareNum(int paiA, int paiB) {
		if (paiA == paiB) {
			return 0;
		} else if (paiA != 1 && paiB != 1) {
			return paiA > paiB ? 1 : -1;
		} else {
			return paiA == 1 ? 1 : -1;
		}
	}

	public static List<Integer> xiPai() {
		paiIndex = 0;
		int[] indexs = randomPai();
		List<Integer> pais = new ArrayList();

		for(int i = 0; i < indexs.length; ++i) {
			pais.add(PAIS[indexs[i]]);
		}

		return pais;
	}

	private static int[] randomPai() {
		int[] nums = new int[52];

		int num;
		for(int i = 0; i < nums.length; ++i) {
			do {
				num = RandomUtils.nextInt(52);
				if (!ArrayUtils.contains(nums, num)) {
					nums[i] = num;
					break;
				}
			} while(num != 0 || ArrayUtils.indexOf(nums, num) != i);
		}

		return nums;
	}

	public static List<Integer> faPai(List<Integer> pais) {
		List<Integer> paiList = new ArrayList();

		for(int j = 0; j < 3; ++j) {
			paiList.add(pais.get(paiIndex));
			++paiIndex;
		}

		return paiList;
	}

	public static void main(String[] args) {
		List<Integer> a = Arrays.asList(6, 21, 62);
		List<Integer> b = Arrays.asList(41, 70, 72);
		System.out.println(getPaiType(a));
		System.out.println(getPaiType(b));
		System.out.println(compare(a, b));
	}
}
