package com.zhuoan.biz.core.sss;

import com.zhuoan.biz.model.sss.SSSGameRoomNew;
import com.zhuoan.constant.SSSConstant;
import com.zhuoan.service.impl.SSSServiceImpl;
import net.sf.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import static com.zhuoan.biz.core.sss.SSSKingCards.*;

public class SSSOrdinaryCards {

	public static void main(String[] args) {
		String[] pais = { "5-1", "3-13", "1-13", "2-13", "1-12", "2-2", "1-9", "3-9", "3-8", "1-5", "2-5", "3-5",
				"4-4" };
		SSSGameRoomNew room = new SSSGameRoomNew();
		room.setRuanguiCount(4);
		//ArrayList<ArrayList<String>> santiao = three(new ArrayList<>(Arrays.asList(pais)));
		//System.out.println("所有三条："+santiao);
		//matchWuTong(new ArrayList<>(Arrays.asList(pais)), 4);
		System.out.println(new ArrayList<>(Arrays.asList(sort(pais, room))));
		
	}

	public static String[] sort(String[] player, SSSGameRoomNew room) {
		String[] result = new String[13];
		ArrayList<String> five2 = new ArrayList<>();
		ArrayList<String> five1 = new ArrayList<>();
		ArrayList<String> three = new ArrayList<>();
		ArrayList<String> playerTemp = new ArrayList<>(Arrays.asList(player));
		// 六同
		for (int i = 0; i < 2; i++) {
			if (five2.size() == 0) {
				five2 = matchLiuTong(playerTemp, room.getRuanguiCount());
				playerTemp = removeByList(playerTemp, five2);
			} else if (five1.size() == 0) {
				five1 = matchLiuTong(playerTemp, room.getRuanguiCount());
				playerTemp = removeByList(playerTemp, five1);
			}
		}
		// 五同
		for (int i = 0; i < 2; i++) {
			if (five2.size() == 0) {
				five2 = matchWuTong(playerTemp,room.getRuanguiCount());
				playerTemp = removeByList(playerTemp, five2);
				// 如果没有检测到5同牌 就去检测铁枝看看是否有软鬼的炸弹升级为5同
			} else if (five1.size() == 0) {
				five1 = matchWuTong(playerTemp,room.getRuanguiCount());
				playerTemp = removeByList(playerTemp, five1);
			}
		}

		String sameColor = SSSConstant.SSS_SAME_COLOR_NO;//// 没有清一色
		/*
		 * if (room.getRoomInfo().containsKey(SSSConstant.SSS_DATA_KET_SAME_COLOR)) {
		 * sameColor =
		 * room.getRoomInfo().getString(SSSConstant.SSS_DATA_KET_SAME_COLOR); }
		 */
		if (SSSConstant.SSS_SAME_COLOR_YES.equals(sameColor)) {
			// 铁支
			for (int i = 0; i < 2; i++) {
				if (five2.size() == 0) {
					five2 = matchBomb(playerTemp,room.getRuanguiCount());
					playerTemp = removeByList(playerTemp, five2);

				} else if (five1.size() == 0) {
					five1 = matchBomb(playerTemp,room.getRuanguiCount());
					playerTemp = removeByList(playerTemp, five1);
				}
			}
			// 同花顺
			for (int i = 0; i < 2; i++) {
				if (five2.size() == 0) {
					five2 = matchFlushByFlower(playerTemp);
					playerTemp = removeByList(playerTemp, five2);
				} else if (five1.size() == 0) {
					five1 = matchFlushByFlower(playerTemp);
					playerTemp = removeByList(playerTemp, five1);
				}
			}
		} else {
			// 同花顺
			for (int i = 0; i < 2; i++) {
				if (five2.size() == 0) {
					five2 = matchFlushByFlower(playerTemp);
					playerTemp = removeByList(playerTemp, five2);
				} else if (five1.size() == 0) {
					five1 = matchFlushByFlower(playerTemp);
					playerTemp = removeByList(playerTemp, five1);
				}
			}
			// 铁支
			for (int i = 0; i < 2; i++) {
				if (five2.size() == 0) {
					five2 = matchBomb(playerTemp,room.getRuanguiCount());
					playerTemp = removeByList(playerTemp, five2);
				} else if (five1.size() == 0) {
					five1 = matchBomb(playerTemp,room.getRuanguiCount());
					playerTemp = removeByList(playerTemp, five1);
				}
			}
		}

		// 葫芦
		for (int i = 0; i < 2; i++) {
			if (five2.size() == 0) {
				five2 = matchGourd(playerTemp);
				playerTemp = removeByList(playerTemp, five2);
			} else if (five1.size() == 0) {
				five1 = matchGourd(playerTemp);
				playerTemp = removeByList(playerTemp, five1);
			}
		}
		// 同花
		for (int i = 0; i < 2; i++) {
			ArrayList<ArrayList<String>> tonghua = null;
			if (five1.size() == 0 || five2.size() == 0) {
				tonghua = sameFlower(playerTemp);
			}
			if (tonghua != null && tonghua.size() > 0) {
				if (SSSComputeCards.getKingPaiCount(JSONArray.fromObject(playerTemp)) > 0) {
					ArrayList<String> strings = SSSLaiZIForMula.sameFlowerByLaiZi(playerTemp);
					if (strings != null) {
						if (five2.size() == 0) {
							five2.addAll(strings);
							for (String s : five2) {
								playerTemp.remove(s);
							}
						} else if (five1.size() == 0) {
							five1.addAll(strings);
							for (String s : five1) {
								playerTemp.remove(s);
							}
						}
					}
				} else {
					if (five2.size() == 0) {
						ArrayList<String> tt = new ArrayList<String>();
						tt.addAll(playerTemp);
						ArrayList<ArrayList<String>> oneArrayList = one(tt);
						if (oneArrayList.size() > 0) {
							if ("1".equals(oneArrayList.get(0).get(0).split("-")[1])) {
								for (String temp : oneArrayList.get(0)) {
									tt.remove(temp);
								}
								ArrayList<ArrayList<String>> fList = sameFlower(tt);
								if (fList.size() > 0) {
									ArrayList<ArrayList<String>> oneArrayList2 = one(tt);
									if (oneArrayList2.size() > 0) {
										for (String temp : oneArrayList2.get(oneArrayList2.size() - 1)) {
											tt.remove(temp);
										}
										ArrayList<ArrayList<String>> fList2 = sameFlower(tt);
										if (fList2.size() > 0) {
											ArrayList<ArrayList<String>> oneArrayList3 = one(tt);
											if (oneArrayList3.size() > 0) {
												for (String temp : oneArrayList3.get(oneArrayList3.size() - 1)) {
													tt.remove(temp);
												}
												ArrayList<ArrayList<String>> fList3 = sameFlower(tt);
												if (fList3.size() > 0) {
													five2.addAll(fList3.get(fList3.size() - 1));
												}
											}
											if (five2.size() == 0) {
												five2.addAll(fList2.get(fList2.size() - 1));
											}
										}
									}
									if (five2.size() == 0) {
										five2.addAll(fList.get(fList.size() - 1));
									}
								}
							} else {
								for (String temp : oneArrayList.get(oneArrayList.size() - 1)) {
									tt.remove(temp);
								}
								ArrayList<ArrayList<String>> fList = sameFlower(tt);
								if (fList.size() > 0) {
									ArrayList<ArrayList<String>> oneArrayList2 = one(tt);
									if (oneArrayList2.size() > 0) {
										for (String temp : oneArrayList2.get(oneArrayList2.size() - 1)) {
											tt.remove(temp);
										}
										ArrayList<ArrayList<String>> fList2 = sameFlower(tt);
										if (fList2.size() > 0) {
											ArrayList<ArrayList<String>> oneArrayList3 = one(tt);
											if (oneArrayList3.size() > 0) {
												for (String temp : oneArrayList3.get(oneArrayList3.size() - 1)) {
													tt.remove(temp);
												}
												ArrayList<ArrayList<String>> fList3 = sameFlower(tt);
												if (fList3.size() > 0) {
													five2.addAll(fList3.get(fList3.size() - 1));
												}
											}
											if (five2.size() == 0) {
												five2.addAll(fList2.get(fList2.size() - 1));
											}
										}
									}
									if (five2.size() == 0) {
										five2.addAll(fList.get(fList.size() - 1));
									}
								}
							}
						}
						if (five2.size() == 0) {
							five2.addAll(tonghua.get(tonghua.size() - 1));
						}
						for (String temp : five2) {
							playerTemp.remove(temp);
						}
					} else if (five1.size() == 0) {
						ArrayList<String> tt = new ArrayList<String>();
						tt.addAll(playerTemp);
						ArrayList<ArrayList<String>> oneArrayList = one(tt);
						if (oneArrayList.size() > 0) {
							if ("1".equals(oneArrayList.get(0).get(0).split("-")[1])) {
								for (String temp : oneArrayList.get(0)) {
									tt.remove(temp);
								}
								ArrayList<ArrayList<String>> fList = sameFlower(tt);
								if (fList.size() > 0) {
									five1.addAll(fList.get(fList.size() - 1));
								}
							} else {
								for (String temp : oneArrayList.get(oneArrayList.size() - 1)) {
									tt.remove(temp);
								}
								ArrayList<ArrayList<String>> fList = sameFlower(tt);
								if (fList.size() > 0) {
									five1.addAll(fList.get(fList.size() - 1));
								}
							}
						}
						if (five1.size() == 0) {
							five1.addAll(tonghua.get(tonghua.size() - 1));
						}
						for (String temp : five1) {
							playerTemp.remove(temp);
						}
					}
					if (five1.size() > 0 && five2.size() > 0) {
						if (i == 1) {
							boolean isFloewr = false;
							if (five1.get(0).split("-")[0].equals(five1.get(1).split("-")[0])
									&& five1.get(1).split("-")[0].equals(five1.get(2).split("-")[0])
									&& five1.get(2).split("-")[0].equals(five1.get(3).split("-")[0])
									&& five1.get(3).split("-")[0].equals(five1.get(4).split("-")[0])) {
								isFloewr = true;
							}
							if (isFloewr) {
								ArrayList<String> temp = new ArrayList<String>();
								if ("1".equals(five1.get(0).split("-")[1])) {
									if ("1".equals(five2.get(0).split("-")[1])) {
										if (Integer.parseInt(five1.get(4).split("-")[1]) > Integer
												.parseInt(five2.get(4).split("-")[1])) {
											temp.addAll(five1);
											five1.clear();
											five1.addAll(five2);
											five2.clear();
											five2.addAll(temp);
										} else if (Integer.parseInt(five1.get(4).split("-")[1]) == Integer
												.parseInt(five2.get(4).split("-")[1])) {
											if (Integer.parseInt(five1.get(3).split("-")[1]) > Integer
													.parseInt(five2.get(3).split("-")[1])) {
												temp.addAll(five1);
												five1.clear();
												five1.addAll(five2);
												five2.clear();
												five2.addAll(temp);
											} else if (Integer.parseInt(five1.get(3).split("-")[1]) == Integer
													.parseInt(five2.get(3).split("-")[1])) {
												if (Integer.parseInt(five1.get(2).split("-")[1]) > Integer
														.parseInt(five2.get(2).split("-")[1])) {
													temp.addAll(five1);
													five1.clear();
													five1.addAll(five2);
													five2.clear();
													five2.addAll(temp);
												} else if (Integer.parseInt(five1.get(2).split("-")[1]) == Integer
														.parseInt(five2.get(2).split("-")[1])) {
													if (Integer.parseInt(five1.get(1).split("-")[1]) > Integer
															.parseInt(five2.get(1).split("-")[1])) {
														temp.addAll(five1);
														five1.clear();
														five1.addAll(five2);
														five2.clear();
														five2.addAll(temp);
													}
												}
											}
										}
									} else {
										temp.addAll(five1);
										five1.clear();
										five1.addAll(five2);
										five2.clear();
										five2.addAll(temp);
									}
								} else {
									if (Integer.parseInt(five1.get(4).split("-")[1]) > Integer
											.parseInt(five2.get(4).split("-")[1])) {
										temp.addAll(five1);
										five1.clear();
										five1.addAll(five2);
										five2.clear();
										five2.addAll(temp);
									} else if (Integer.parseInt(five1.get(4).split("-")[1]) == Integer
											.parseInt(five2.get(4).split("-")[1])) {
										if (Integer.parseInt(five1.get(3).split("-")[1]) > Integer
												.parseInt(five2.get(3).split("-")[1])) {
											temp.addAll(five1);
											five1.clear();
											five1.addAll(five2);
											five2.clear();
											five2.addAll(temp);
										} else if (Integer.parseInt(five1.get(3).split("-")[1]) == Integer
												.parseInt(five2.get(3).split("-")[1])) {
											if (Integer.parseInt(five1.get(2).split("-")[1]) > Integer
													.parseInt(five2.get(2).split("-")[1])) {
												temp.addAll(five1);
												five1.clear();
												five1.addAll(five2);
												five2.clear();
												five2.addAll(temp);
											} else if (Integer.parseInt(five1.get(2).split("-")[1]) == Integer
													.parseInt(five2.get(2).split("-")[1])) {
												if (Integer.parseInt(five1.get(1).split("-")[1]) > Integer
														.parseInt(five2.get(1).split("-")[1])) {
													temp.addAll(five1);
													five1.clear();
													five1.addAll(five2);
													five2.clear();
													five2.addAll(temp);
												} else if (Integer.parseInt(five1.get(1).split("-")[1]) == Integer
														.parseInt(five2.get(1).split("-")[1])) {
													if (Integer.parseInt(five1.get(0).split("-")[1]) > Integer
															.parseInt(five2.get(0).split("-")[1])) {
														temp.addAll(five1);
														five1.clear();
														five1.addAll(five2);
														five2.clear();
														five2.addAll(temp);
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		// 顺子
		for (int i = 0; i < 2; i++) {
			ArrayList<ArrayList<String>> shunzi = flush(playerTemp);

			if (shunzi.size() > 0) {
				if (SSSComputeCards.getKingPaiCount(JSONArray.fromObject(playerTemp)) > 0) {
					ArrayList<String> strings = SSSLaiZIForMula.flushBrandByLaiZi(playerTemp);
					if (strings != null) {
						if (five2.size() == 0) {
							five2.addAll(strings);
							for (String s : five2) {
								playerTemp.remove(s);
							}
						} else if (five1.size() == 0) {
							five1.addAll(strings);
							for (String s : five1) {
								playerTemp.remove(s);
							}
						}
					}
				} else {
					if (five2.size() == 0) {
						if ("1".equals(shunzi.get(0).get(0).split("-")[1])
								&& !"1".equals(shunzi.get(shunzi.size() - 1).get(4).split("-")[1])) {
							five2.addAll(shunzi.get(0));
						} else {
							five2.addAll(shunzi.get(shunzi.size() - 1));
						}
						for (String temp : five2) {
							playerTemp.remove(temp);
						}
					} else if (five1.size() == 0) {
						ArrayList<String> tt = new ArrayList<String>();
						tt.addAll(playerTemp);
						ArrayList<ArrayList<String>> oneArrayList = one(tt);
						if (oneArrayList.size() > 0) {
							int kk = 0;
							if (!"1".equals(oneArrayList.get(0).get(0).split("-")[1])) {
								kk = oneArrayList.size() - 1;
							}
							for (String temp : oneArrayList.get(kk)) {
								tt.remove(temp);
							}
							ArrayList<ArrayList<String>> fList = flush(tt);
							if (fList.size() > 0) {
								if ("1".equals(shunzi.get(0).get(0).split("-")[1])
										&& !"1".equals(shunzi.get(shunzi.size() - 1).get(4).split("-")[1])) {
									five1.addAll(shunzi.get(0));
								} else {
									five1.addAll(fList.get(fList.size() - 1));
								}
							}
						}
						if (five1.size() == 0) {
							if ("1".equals(shunzi.get(0).get(0).split("-")[1])
									&& !"1".equals(shunzi.get(shunzi.size() - 1).get(4).split("-")[1])) {
								five1.addAll(shunzi.get(0));
							} else {
								five1.addAll(shunzi.get(shunzi.size() - 1));
							}
						}
						for (String temp : five1) {
							playerTemp.remove(temp);
						}
					}
				}
			}
		}
		// 三条
		for (int i = 0; i < 3; i++) {
			ArrayList<ArrayList<String>> santiao = three(playerTemp);
			if (santiao.size() > 0) {
				if (SSSComputeCards.getKingPaiCount(JSONArray.fromObject(playerTemp)) > 0) {
					boolean isHasA = isHasA(santiao.get(0));
					if (five2.size() == 0) {
						if (isHasA) {
							five2.addAll(santiao.get(0));
						} else {
							five2.addAll(santiao.get(santiao.size() - 1));
						}
						for (String s : five2) {
							playerTemp.remove(s);
						}
					} else if (five1.size() == 0) {
						if (isHasA) {
							five1.addAll(santiao.get(0));
						} else {
							five1.addAll(santiao.get(santiao.size() - 1));
						}
						for (String s : five1) {
							playerTemp.remove(s);
						}
					} else if (three.size() == 0) {
						if (isHasA) {
							three.addAll(santiao.get(0));
						} else {
							three.addAll(santiao.get(santiao.size() - 1));
						}
						for (String s : three) {
							playerTemp.remove(s);
						}
					}
				} else {
					if (five2.size() == 0) {
						if ("1".equals(santiao.get(0).get(0).split("-")[1])) {
							five2.addAll(santiao.get(0));
						} else {
							five2.addAll(santiao.get(santiao.size() - 1));
						}
						for (String temp : five2) {
							playerTemp.remove(temp);
						}
					} else if (five1.size() == 0) {
						if ("1".equals(santiao.get(0).get(0).split("-")[1])) {
							five1.addAll(santiao.get(0));
						} else {
							five1.addAll(santiao.get(santiao.size() - 1));
						}
						for (String temp : five1) {
							playerTemp.remove(temp);
						}
					} else if (three.size() == 0) {
						three.addAll(santiao.get(santiao.size() - 1));
						for (String temp : three) {
							playerTemp.remove(temp);
						}
					}

				}
			}
		}
		// 两对
		for (int i = 0; i < 3; i++) {
			ArrayList<ArrayList<String>> liangdui = two(playerTemp);

			if (liangdui.size() > 0) {
				int loop = 0;
				for (int k = 0; k < liangdui.size(); k++) {
					if (!"1".equals(liangdui.get(k).get(0).split("-")[1])) {
						loop = k;
						break;
					}
				}
				if (five2.size() == 0) {
					if (loop != 0) {
						five2.addAll(liangdui.get(loop - 1));
					} else {
						five2.addAll(liangdui.get(liangdui.size() - 1));
					}
					for (String temp : five2) {
						playerTemp.remove(temp);
					}
				} else if (five1.size() == 0) {
					if (loop != 0) {
						five1.addAll(liangdui.get(loop));
					} else {
						five1.addAll(liangdui.get(0));
					}
					for (String temp : five1) {
						playerTemp.remove(temp);
					}
				}

			}
		}

		// 一对
		for (int i = 0; i < 3; i++) {
			ArrayList<ArrayList<String>> yidui = one(playerTemp);

			if (yidui.size() > 0) {
				if (SSSComputeCards.getKingPaiCount(JSONArray.fromObject(playerTemp)) > 0) {
					boolean isHasA = isHasA(playerTemp);
					if (five2.size() == 0) {
						if (isHasA) {
							five2.addAll(yidui.get(0));
						} else {
							five2.addAll(yidui.get(yidui.size() - 1));
						}
						for (String temp : five2) {
							playerTemp.remove(temp);
						}
					} else if (five1.size() == 0) {
						if (isHasA) {
							five1.addAll(yidui.get(0));
						} else {
							five1.addAll(yidui.get(yidui.size() - 1));
						}
						for (String temp : five1) {
							playerTemp.remove(temp);
						}
					} else if (three.size() == 0) {
						if (isHasA) {
							three.addAll(yidui.get(0));
						} else {
							three.addAll(yidui.get(yidui.size() - 1));
						}
						for (String temp : three) {
							playerTemp.remove(temp);
						}
					}
				} else {
					if (five2.size() == 0) {
						if ("1".equals(yidui.get(0).get(0).split("-")[1])) {
							five2.addAll(yidui.get(0));
						} else {
							five2.addAll(yidui.get(yidui.size() - 1));
						}
						for (String temp : five2) {
							playerTemp.remove(temp);
						}
					} else if (five1.size() == 0) {
						if ("1".equals(yidui.get(0).get(0).split("-")[1])) {
							five1.addAll(yidui.get(0));
						} else {
							five1.addAll(yidui.get(yidui.size() - 1));
						}
						for (String temp : five1) {
							playerTemp.remove(temp);
						}
					} else if (three.size() == 0) {
						if ("1".equals(yidui.get(0).get(0).split("-")[1])) {
							three.addAll(yidui.get(0));
						} else {
							three.addAll(yidui.get(yidui.size() - 1));
						}
						for (String temp : three) {
							playerTemp.remove(temp);
						}
					}
				}
			}
		}
		if (three.size() < 3) {
			for (int k = three.size(); k < 3; k++) {
				ArrayList<ArrayList<Integer>> num = SSSServiceImpl.getListByNum(playerTemp);
				if (num.get(0).size() > 0 && five1.size() != 0 && five2.size() != 0) {
					three.add(num.get(0).get(0) + "-" + 1);
					playerTemp.remove(num.get(0).get(0) + "-" + 1);
				} else {
					if (num.get(0).size() > 0) {
						if (five2.size() == 0) {
							five2.add(num.get(0).get(0) + "-" + 1);
							playerTemp.remove(num.get(0).get(0) + "-" + 1);
						} else if (five1.size() == 0) {
							five1.add(num.get(0).get(0) + "-" + 1);
							playerTemp.remove(num.get(0).get(0) + "-" + 1);
						}
					}
					int numTemp = 0;
					for (int l = num.size() - 1; l >= 0; l--) {
						if (num.get(l).size() > 0) {
							numTemp = l;
							break;
						}
					}
					if (num.get(numTemp).size() > 0) {
						if (five2.size() == 0) {
							five2.add(num.get(numTemp).get(0) + "-" + (numTemp + 1));
							k--;
						} else if (five1.size() == 0) {
							five1.add(num.get(numTemp).get(0) + "-" + (numTemp + 1));
							k--;
						} else {
							three.add(num.get(numTemp).get(0) + "-" + (numTemp + 1));
						}
						playerTemp.remove(num.get(numTemp).get(0) + "-" + (numTemp + 1));
					}
				}
			}
		}
		if (five1.size() < 5) {
			for (int k = five1.size(); k < 5; k++) {
				ArrayList<ArrayList<Integer>> num = SSSServiceImpl.getListByNum(playerTemp);
				if (num.get(0).size() > 0) {
					five1.add(num.get(0).get(0) + "-" + 1);
					playerTemp.remove(num.get(0).get(0) + "-" + 1);
				} else {
					int numTemp = 0;
					for (int l = num.size() - 1; l >= 0; l--) {
						if (num.get(l).size() > 0) {
							numTemp = l;
							break;
						}
					}
					if (num.get(numTemp).size() > 0) {
						five1.add(num.get(numTemp).get(0) + "-" + (numTemp + 1));
						playerTemp.remove(num.get(numTemp).get(0) + "-" + (numTemp + 1));
					}
				}
			}
		}
		if (five2.size() < 5) {
			for (int k = five2.size(); k < 5; k++) {
				ArrayList<ArrayList<Integer>> num = SSSServiceImpl.getListByNum(playerTemp);
				if (num.get(0).size() > 0) {
					five2.add(num.get(0).get(0) + "-" + 1);
					playerTemp.remove(num.get(0).get(0) + "-" + 1);
				} else {
					int numTemp = 0;
					for (int l = num.size() - 1; l >= 0; l--) {
						if (num.get(l).size() > 0) {
							numTemp = l;
							break;
						}
					}
					if (num.get(numTemp).size() > 0) {
						five2.add(num.get(numTemp).get(0) + "-" + (numTemp + 1));
						playerTemp.remove(num.get(numTemp).get(0) + "-" + (numTemp + 1));
					}
				}
			}
		}

		if (three.size() == 3 && five1.size() == 5 && five2.size() == 5) {
			for (int i = 0; i < result.length; i++) {
				for (String string : three) {
					result[i] = string;
					i++;
				}
				for (String string : five1) {
					result[i] = string;
					i++;
				}
				for (String string : five2) {
					result[i] = string;
					i++;
				}
			}
		}
		return result;
	}

	public static ArrayList<ArrayList<String>> both(ArrayList<String> player) {
		// 获取牌中的癞子
		Integer kingPaiCount = SSSComputeCards.getKingPaiCount(JSONArray.fromObject(player));
		if (kingPaiCount > 0) {
			return bothHasKing(player);
		}
		ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
		ArrayList<ArrayList<String>> tempList = new ArrayList<ArrayList<String>>();
		for (int index = 0; index < set.size(); index++) {
			ArrayList<Integer> list = set.get(index);
			if (list.size() == 5) {
				ArrayList<String> temp = new ArrayList<String>();
				temp.add(list.get(0) + "-" + (index + 1));
				temp.add(list.get(1) + "-" + (index + 1));
				temp.add(list.get(2) + "-" + (index + 1));
				temp.add(list.get(3) + "-" + (index + 1));
				temp.add(list.get(4) + "-" + (index + 1));
				tempList.add(temp);
			}
		}
		return tempList;
	}

	public static ArrayList<ArrayList<String>> flushByFlower(ArrayList<String> player) {
		// 获取牌中的癞子
		Integer kingPaiCount = SSSComputeCards.getKingPaiCount(JSONArray.fromObject(player));
		if (kingPaiCount > 0) {
			return flushByFlowerHasKing(player);
		}
		ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByFlower(player);
		ArrayList<ArrayList<String>> tempList = new ArrayList<ArrayList<String>>();
		for (int index = 0; index < set.size(); index++) {
			ArrayList<Integer> list = set.get(index);
			if (list.size() >= 5) {
				if (list.get(0) != 1 && list.get(list.size() - 1) != 13) {
					for (int i = 0; i <= list.size() - 5; i++) {
						ArrayList<String> temp = new ArrayList<String>();
						if (list.get(i) + 1 == list.get(i + 1) && list.get(i + 1) + 1 == list.get(i + 2)
								&& list.get(i + 2) + 1 == list.get(i + 3) && list.get(i + 3) + 1 == list.get(i + 4)) {
							temp.add(index + 1 + "-" + list.get(i));
							temp.add(index + 1 + "-" + list.get(i + 1));
							temp.add(index + 1 + "-" + list.get(i + 2));
							temp.add(index + 1 + "-" + list.get(i + 3));
							temp.add(index + 1 + "-" + list.get(i + 4));
							tempList.add(temp);
						} else {
							continue;
						}
					}
				} else {
					for (int i = 0; i <= list.size() - 5; i++) {
						ArrayList<String> temp = new ArrayList<String>();
						if (list.get(i) + 1 == list.get(i + 1) && list.get(i + 1) + 1 == list.get(i + 2)
								&& list.get(i + 2) + 1 == list.get(i + 3) && list.get(i + 3) + 1 == list.get(i + 4)) {
							temp.add(index + 1 + "-" + list.get(i));
							temp.add(index + 1 + "-" + list.get(i + 1));
							temp.add(index + 1 + "-" + list.get(i + 2));
							temp.add(index + 1 + "-" + list.get(i + 3));
							temp.add(index + 1 + "-" + list.get(i + 4));
							tempList.add(temp);
						} else {
							continue;
						}
					}
					if (list.get(0) == 1) {
						list.add(list.get(0));
						list.remove(0);
						for (int i = list.size() - 5; i <= list.size() - 5; i++) {
							ArrayList<String> temp = new ArrayList<String>();
							if (i == list.size() - 5) {
								if (list.get(i) + 1 == list.get(i + 1) && list.get(i + 1) + 1 == list.get(i + 2)
										&& list.get(i + 2) + 1 == list.get(i + 3) && list.get(i + 3) == 13) {
									temp.add(index + 1 + "-" + list.get(i));
									temp.add(index + 1 + "-" + list.get(i + 1));
									temp.add(index + 1 + "-" + list.get(i + 2));
									temp.add(index + 1 + "-" + list.get(i + 3));
									temp.add(index + 1 + "-" + list.get(i + 4));
									tempList.add(temp);
								} else {
									continue;
								}
							}
						}
					}
				}
			}
		}
		return tempList;
	}

	public static ArrayList<ArrayList<String>> bomb(ArrayList<String> player) {
		// 获取牌中的癞子
		Integer kingPaiCount = SSSComputeCards.getKingPaiCount(JSONArray.fromObject(player));
		if (kingPaiCount > 0) {
			return bombHasKing(player);
		}
		ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
		ArrayList<ArrayList<String>> tempList = new ArrayList<ArrayList<String>>();
		for (int index = 0; index < set.size(); index++) {
			ArrayList<Integer> list = set.get(index);
			if (list.size() == 4) {
				ArrayList<String> temp = new ArrayList<String>();
				temp.add(list.get(0) + "-" + (index + 1));
				temp.add(list.get(1) + "-" + (index + 1));
				temp.add(list.get(2) + "-" + (index + 1));
				temp.add(list.get(3) + "-" + (index + 1));
				tempList.add(temp);
			}
		}
		return tempList;
	}

	public static ArrayList<ArrayList<String>> gourd(ArrayList<String> player) {
		// 获取牌中的癞子
		Integer kingPaiCount = SSSComputeCards.getKingPaiCount(JSONArray.fromObject(player));
		if (kingPaiCount > 0) {
			return gourdHasKing(player);
		}
		ArrayList<ArrayList<String>> tempList = new ArrayList<ArrayList<String>>();
		for (int index = 0; index < 13; index++) {
			ArrayList<ArrayList<Integer>> tempSet = SSSServiceImpl.getListByNum(player);
			ArrayList<Integer> list = tempSet.get(index);
			if (list.size() >= 3) {// 三条
				ArrayList<String> temp = new ArrayList<String>();
				temp.add(list.get(0) + "-" + (index + 1));
				temp.add(list.get(1) + "-" + (index + 1));
				temp.add(list.get(2) + "-" + (index + 1));
				tempSet.get(index).remove(0);
				tempSet.get(index).remove(0);
				tempSet.get(index).remove(0);
				for (int j = 0; j < tempSet.size(); j++) {
					ArrayList<Integer> listj = tempSet.get(j);
					if (listj.size() >= 2) {// 俩对
						ArrayList<String> tempj = new ArrayList<String>();
						tempj.add(temp.get(0));
						tempj.add(temp.get(1));
						tempj.add(temp.get(2));
						tempj.add(listj.get(0) + "-" + (j + 1));
						tempj.add(listj.get(1) + "-" + (j + 1));
						tempList.add(tempj);
					}
				}
			}
		}
		return tempList;
	}

	public static ArrayList<ArrayList<String>> sameFlower(ArrayList<String> player) {
		// 获取牌中的癞子
		Integer kingPaiCount = SSSComputeCards.getKingPaiCount(JSONArray.fromObject(player));
		if (kingPaiCount > 0) {
			ArrayList<ArrayList<String>> temp = new ArrayList<>();
			temp.add(SSSLaiZIForMula.sameFlowerByLaiZi(player));
			return temp;
		}
		ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByFlower(player);
		ArrayList<ArrayList<String>> tempList = new ArrayList<ArrayList<String>>();
		for (int index = 0; index < set.size(); index++) {
			ArrayList<Integer> list = set.get(index);
			if (list.size() == 5) {
				ArrayList<String> temp = new ArrayList<String>();
				for (int i = 0; i < list.size(); i++) {
					temp.add(index + 1 + "-" + list.get(i));
				}
				tempList.add(temp);
			} else if (list.size() > 5) {
				for (int i = 0; i < list.size(); i++) {
					if (list.size() == 6) {
						ArrayList<String> temp = new ArrayList<String>();
						for (int k = 0; k < list.size(); k++) {
							if (i != k) {
								temp.add(index + 1 + "-" + list.get(k));
							}
						}
						if (temp.size() == 5) {
							tempList.add(temp);
						}
					} else if (list.size() == 7) {
						for (int j = i + 1; j < list.size(); j++) {
							ArrayList<String> temp = new ArrayList<String>();
							for (int k = 0; k < list.size(); k++) {
								if (i != k && j != k) {
									temp.add(index + 1 + "-" + list.get(k));
								}
							}
							if (temp.size() == 5) {
								tempList.add(temp);
							}
						}
					} else if (list.size() == 8) {
						for (int j = i + 1; j < list.size(); j++) {
							for (int l = j + 1; l < list.size(); l++) {
								ArrayList<String> temp = new ArrayList<String>();
								for (int k = 0; k < list.size(); k++) {
									if (i != k && j != k && l != k) {
										temp.add(index + 1 + "-" + list.get(k));
									}
								}
								if (temp.size() == 5) {
									tempList.add(temp);
								}
							}
						}
					} else if (list.size() == 9) {
						for (int j = i + 1; j < list.size(); j++) {
							for (int l = j + 1; l < list.size(); l++) {
								for (int m = l + 1; m < list.size(); m++) {
									ArrayList<String> temp = new ArrayList<String>();
									for (int k = 0; k < list.size(); k++) {
										if (i != k && j != k && l != k && m != k) {
											temp.add(index + 1 + "-" + list.get(k));
										}
									}
									if (temp.size() == 5) {
										tempList.add(temp);
									}
								}
							}
						}
					} else if (list.size() >= 10) {
						for (int j = i + 1; j < list.size(); j++) {
							for (int l = j + 1; l < list.size(); l++) {
								for (int m = l + 1; m < list.size(); m++) {
									for (int n = m + 1; n < list.size(); n++) {
										ArrayList<String> temp = new ArrayList<String>();
										int count = 0;
										for (int k = 0; k < list.size(); k++) {
											if (i != k && j != k && l != k && m != k && n != k && count < 5) {
												temp.add(index + 1 + "-" + list.get(k));
												count++;
											}
										}
										if (temp.size() == 5) {
											tempList.add(temp);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		for (int i = 0; i < tempList.size(); i++) {
			for (int j = 0; j < tempList.size() - 1; j++) {
				ArrayList<String> temp = new ArrayList<String>();
				ArrayList<String> now = tempList.get(j);
				ArrayList<String> next = tempList.get(j + 1);
				if ("1".equals(now.get(0).split("-")[1])) {
					if ("1".equals(next.get(0).split("-")[1])) {
						if (Integer.parseInt(now.get(4).split("-")[1]) > Integer.parseInt(next.get(4).split("-")[1])) {
							temp.addAll(now);
							tempList.set(j, next);
							tempList.set(j + 1, temp);
						} else if (Integer.parseInt(now.get(4).split("-")[1]) == Integer
								.parseInt(next.get(4).split("-")[1])) {
							if (Integer.parseInt(now.get(3).split("-")[1]) > Integer
									.parseInt(next.get(3).split("-")[1])) {
								temp.addAll(now);
								tempList.set(j, next);
								tempList.set(j + 1, temp);
							} else if (Integer.parseInt(now.get(3).split("-")[1]) == Integer
									.parseInt(next.get(3).split("-")[1])) {
								if (Integer.parseInt(now.get(2).split("-")[1]) > Integer
										.parseInt(next.get(2).split("-")[1])) {
									temp.addAll(now);
									tempList.set(j, next);
									tempList.set(j + 1, temp);
								} else if (Integer.parseInt(now.get(2).split("-")[1]) == Integer
										.parseInt(next.get(2).split("-")[1])) {
									if (Integer.parseInt(now.get(1).split("-")[1]) > Integer
											.parseInt(next.get(1).split("-")[1])) {
										temp.addAll(now);
										tempList.set(j, next);
										tempList.set(j + 1, temp);
									}
								}
							}
						}
					} else {
						temp.addAll(now);
						tempList.set(j, next);
						tempList.set(j + 1, temp);
					}
				} else {
					if (Integer.parseInt(now.get(4).split("-")[1]) > Integer.parseInt(next.get(4).split("-")[1])) {
						temp.addAll(now);
						tempList.set(j, next);
						tempList.set(j + 1, temp);
					} else if (Integer.parseInt(now.get(4).split("-")[1]) == Integer
							.parseInt(next.get(4).split("-")[1])) {
						if (Integer.parseInt(now.get(3).split("-")[1]) > Integer.parseInt(next.get(3).split("-")[1])) {
							temp.addAll(now);
							tempList.set(j, next);
							tempList.set(j + 1, temp);
						} else if (Integer.parseInt(now.get(3).split("-")[1]) == Integer
								.parseInt(next.get(3).split("-")[1])) {
							if (Integer.parseInt(now.get(2).split("-")[1]) > Integer
									.parseInt(next.get(2).split("-")[1])) {
								temp.addAll(now);
								tempList.set(j, next);
								tempList.set(j + 1, temp);
							} else if (Integer.parseInt(now.get(2).split("-")[1]) == Integer
									.parseInt(next.get(2).split("-")[1])) {
								if (Integer.parseInt(now.get(1).split("-")[1]) > Integer
										.parseInt(next.get(1).split("-")[1])) {
									temp.addAll(now);
									tempList.set(j, next);
									tempList.set(j + 1, temp);
								} else if (Integer.parseInt(now.get(1).split("-")[1]) == Integer
										.parseInt(next.get(1).split("-")[1])) {
									if (Integer.parseInt(now.get(0).split("-")[1]) > Integer
											.parseInt(next.get(0).split("-")[1])) {
										temp.addAll(now);
										tempList.set(j, next);
										tempList.set(j + 1, temp);
									}
								}
							}
						}
					}
				}
			}
		}
		return tempList;
	}

	public static ArrayList<ArrayList<String>> flush(ArrayList<String> player) {
		// 获取牌中的癞子
		Integer kingPaiCount = SSSComputeCards.getKingPaiCount(JSONArray.fromObject(player));
		if (kingPaiCount > 0) {
			ArrayList<ArrayList<String>> temp = new ArrayList<>();
			temp.add(SSSLaiZIForMula.flushBrandByLaiZi(player));
			return temp;
		}
		ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
		ArrayList<ArrayList<String>> tempList = new ArrayList<ArrayList<String>>();
		TreeSet<Integer> allSet = new TreeSet<Integer>();
		ArrayList<Integer> allList = new ArrayList<Integer>();
		for (String list : player) {
			allSet.add(Integer.parseInt(list.split("-")[1]));
		}
		allList.addAll(allSet);
		if (allList.size() > 4) {
			for (int i = 0; i < allList.size() - 4; i++) {
				if (allList.get(i) + 4 == allList.get(i + 4)) {
					ArrayList<ArrayList<String>> listTemp = new ArrayList<ArrayList<String>>();
					ArrayList<String> list = new ArrayList<String>();
					for (int j = 0; j < 5; j++) {
						if (set.get(allList.get(i + j) - 1).size() == 1) {
							if (listTemp.size() == 0) {
								list.add(set.get(allList.get(i + j) - 1).get(0) + "-" + allList.get(i + j));
								listTemp.add(list);
							} else {
								for (int k = 0; k < listTemp.size(); k++) {
									ArrayList<String> li = listTemp.get(k);
									li.add(set.get(allList.get(i + j) - 1).get(0) + "-" + allList.get(i + j));
								}
							}
						} else {
							if (listTemp.size() == 0) {
								for (int k = 0; k < set.get(allList.get(i + j) - 1).size(); k++) {
									ArrayList<String> litemp = new ArrayList<String>();
									litemp.add(set.get(allList.get(i + j) - 1).get(k) + "-" + allList.get(i + j));
									listTemp.add(litemp);
								}
							} else {
								int si = listTemp.size();
								for (int k = 0; k < si; k++) {
									for (int l = 0; l < set.get(allList.get(i + j) - 1).size(); l++) {
										if (l == 0) {
											ArrayList<String> li = listTemp.get(k);
											li.add(set.get(allList.get(i + j) - 1).get(l) + "-" + allList.get(i + j));
										} else {
											ArrayList<String> li = listTemp.get(k);
											ArrayList<String> litemp = new ArrayList<String>();
											for (int m = 0; m < li.size() - 1; m++) {
												litemp.add(li.get(m));
											}
											litemp.add(
													set.get(allList.get(i + j) - 1).get(l) + "-" + allList.get(i + j));
											listTemp.add(litemp);
										}
									}
								}
							}
						}
					}
					tempList.addAll(listTemp);
				}
				if (allList.get(0) == 1 && allList.get(allList.size() - 1) == 13 && i == allList.size() - 5) {
					ArrayList<ArrayList<String>> listTemp = new ArrayList<ArrayList<String>>();
					if (allList.get(i + 1) + 3 == allList.get(i + 4)) {
						ArrayList<String> list = new ArrayList<String>();
						for (int j = 0; j < 4; j++) {
							if (set.get(allList.get(i + j + 1) - 1).size() == 1) {
								if (listTemp.size() == 0) {
									list.add(set.get(allList.get(i + j + 1) - 1).get(0) + "-" + allList.get(i + j + 1));
									listTemp.add(list);
								} else {
									for (int k = 0; k < listTemp.size(); k++) {
										ArrayList<String> li = listTemp.get(k);
										li.add(set.get(allList.get(i + j + 1) - 1).get(0) + "-"
												+ allList.get(i + j + 1));
									}
								}
							} else {
								if (listTemp.size() == 0) {
									for (int k = 0; k < set.get(allList.get(i + j + 1) - 1).size(); k++) {
										ArrayList<String> litemp = new ArrayList<String>();
										litemp.add(set.get(allList.get(i + j + 1) - 1).get(k) + "-"
												+ allList.get(i + j + 1));
										listTemp.add(litemp);
									}
								} else {
									int si = listTemp.size();
									for (int k = 0; k < si; k++) {
										for (int l = 0; l < set.get(allList.get(i + j + 1) - 1).size(); l++) {
											if (l == 0) {
												ArrayList<String> li = listTemp.get(k);
												li.add(set.get(allList.get(i + j + 1) - 1).get(l) + "-"
														+ allList.get(i + j + 1));
											} else {
												ArrayList<String> li = listTemp.get(k);
												ArrayList<String> litemp = new ArrayList<String>();
												for (int m = 0; m < li.size() - 1; m++) {
													litemp.add(li.get(m));
												}
												litemp.add(set.get(allList.get(i + j + 1) - 1).get(l) + "-"
														+ allList.get(i + j + 1));
												listTemp.add(litemp);
											}
										}
									}
								}
							}
						}
						if (set.get(0).size() == 1) {
							for (int k = 0; k < listTemp.size(); k++) {
								ArrayList<String> li = listTemp.get(k);
								li.add(set.get(0).get(0) + "-" + 1);
							}
						} else if (set.get(0).size() > 1) {
							int si = listTemp.size();
							for (int k = 0; k < si; k++) {
								for (int l = 0; l < set.get(0).size(); l++) {
									if (l == 0) {
										ArrayList<String> li = listTemp.get(k);
										li.add(set.get(0).get(l) + "-" + 1);
									} else {
										ArrayList<String> li = listTemp.get(k);
										ArrayList<String> litemp = new ArrayList<String>();
										for (int m = 0; m < li.size() - 1; m++) {
											litemp.add(li.get(m));
										}
										litemp.add(set.get(0).get(l) + "-" + 1);
										listTemp.add(litemp);
									}
								}
							}
						}
						tempList.addAll(listTemp);
					}
				}
			}
		}
		return tempList;
	}

	public static ArrayList<ArrayList<String>> three(ArrayList<String> player) {
		// 获取牌中的癞子
		Integer kingPaiCount = SSSComputeCards.getKingPaiCount(JSONArray.fromObject(player));
		if (kingPaiCount > 0) {
			return threeHasKing(player);
		}
		ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
		ArrayList<ArrayList<String>> tempList = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < set.size(); i++) {
			if (set.get(i).size() == 3) {
				ArrayList<String> temp = new ArrayList<String>();
				for (int j = 0; j < 3; j++) {
					temp.add(set.get(i).get(j) + "-" + (i + 1));
				}
				tempList.add(temp);
			} else if (set.get(i).size() == 4) {
				for (int j = 0; j < 4; j++) {
					ArrayList<String> temp = new ArrayList<String>();
					for (int k = 0; k < 4; k++) {
						if (k != j) {
							temp.add(set.get(i).get(k) + "-" + (i + 1));
						}
					}
					tempList.add(temp);
				}
			}
		}
		return tempList;
	}

	public static ArrayList<ArrayList<String>> two(ArrayList<String> player) {
		ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
		ArrayList<ArrayList<String>> tempList = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < set.size(); i++) {
			if (set.get(i).size() == 2) {
				ArrayList<String> temp = new ArrayList<String>();
				for (int j = 0; j < 2; j++) {
					temp.add(set.get(i).get(j) + "-" + (i + 1));
				}
				for (int j = i + 1; j < set.size(); j++) {
					if (set.get(j).size() == 2) {
						ArrayList<String> ttemp = new ArrayList<String>();
						ttemp.add(temp.get(0));
						ttemp.add(temp.get(1));
						for (int k = 0; k < 2; k++) {
							ttemp.add(set.get(j).get(k) + "-" + (j + 1));
						}
						tempList.add(ttemp);
					}
				}

			}
		}
		return tempList;
	}

	public static ArrayList<ArrayList<String>> one(ArrayList<String> player) {
		// 获取牌中的癞子
		Integer kingPaiCount = SSSComputeCards.getKingPaiCount(JSONArray.fromObject(player));
		if (kingPaiCount > 0) {
			return oneHasKing(player);
		}
		ArrayList<ArrayList<Integer>> set = SSSServiceImpl.getListByNum(player);
		ArrayList<ArrayList<String>> tempList = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < set.size(); i++) {
			if (set.get(i).size() == 2) {
				ArrayList<String> temp = new ArrayList<String>();
				for (int j = 0; j < 2; j++) {
					temp.add(set.get(i).get(j) + "-" + (i + 1));
				}
				tempList.add(temp);
			}
		}
		return tempList;
	}

	private static boolean isHasA(List<String> strings) {
		boolean isHasA = false;
		// 是否牌中都是鬼牌,则表示为包含A
		boolean isHasAllKing = true;
		for (String string : strings) {
			if (!"5".equals(string.split("-")[0])) {
				isHasAllKing = false;
				if ("1".equals(string.split("-")[1])) {
					isHasA = true;
					break;
				}
			}
		}
		return isHasAllKing || isHasA;
	}

	private static boolean isHasMaxFlush(List<String> strings) {
		for (String string : strings) {
			int t = Integer.parseInt(string.split("-")[1]);
			if ("5".equals(string.split("-")[0]))
				continue;
			if (t != 1 && t < 10) {
				return false;
			}
		}
		return true;
	}

	private static boolean isHasSecondFlush(List<String> strings) {
		for (String string : strings) {
			int t = Integer.parseInt(string.split("-")[1]);
			if ("5".equals(string.split("-")[0]))
				continue;
			if (t > 5) {
				return false;
			}
		}
		return true;
	}

	private static ArrayList<String> matchWuTong(ArrayList<String> playerTemp,int ruanguiCount) {
		List<String> list = new ArrayList<String>();
		if (ruanguiCount>0) {
			list.add("2");
			list.add("3");
			if (ruanguiCount == 4) {
				list.add("4");
				list.add("5");
			}
		}
		
		
		// 检测出所有5同的牌型
		ArrayList<ArrayList<String>> wuTong = both(new ArrayList<>(playerTemp));
		ArrayList<ArrayList<String>> tiezhi = bomb(playerTemp);
		int k = 0;
		for (int i = 0; i < tiezhi.size(); i++) {
			for (String str: tiezhi.get(i)) {
				String sss = str.substring(2);
				if (str.length()==3 && list.contains(sss)) {
					k++;
				}
			}
			if (k>1) {
				wuTong.add(tiezhi.get(i));
				k=0;
			}
		}
		ArrayList<String> pai = new ArrayList<>();
		if (wuTong.size() > 0) {
			int kingCount = SSSComputeCards.getKingPaiCount(JSONArray.fromObject(playerTemp));
			// 癞子五同配牌
			if (kingCount > 0) {
				boolean isHasA = isHasA(wuTong.get(0));
				if (isHasA) {
					pai.addAll(wuTong.get(0));
				} else {
					pai.addAll(wuTong.get(wuTong.size() - 1));
				}
			} else {
				if ("1".equals(wuTong.get(0).get(0).split("-")[1])) {
					pai.addAll(wuTong.get(0));
				} else {
					pai.addAll(wuTong.get(wuTong.size() - 1));
				}
			}
		}
		return pai;
	}

	private static ArrayList<String> matchLiuTong(ArrayList<String> playerTemp, int ruanguiCount) {
		List<String> list = new ArrayList<String>();

		if (ruanguiCount>0) {
			list.add("2");
			list.add("3");
			if (ruanguiCount == 4) {
				list.add("4");
				list.add("5");
			}
		}
		ArrayList<ArrayList<String>> wuTong = both(new ArrayList<>(playerTemp));

		ArrayList<String> pai = new ArrayList<>();
		if (wuTong.size() > 0) {
			int kingCount = SSSComputeCards.getKingPaiCount(JSONArray.fromObject(playerTemp));
			for (int i = 0; i < wuTong.size(); i++) {
				for (String string : wuTong.get(i)) {
					if (string.length() == 3 && list.contains(string.substring(2))) {
						pai.addAll(wuTong.get(i));
						return pai;
					}
				}
			}
			// 癞子五同配牌
			if (kingCount > 0) {
				boolean isHasA = isHasA(wuTong.get(0));
				if (isHasA) {
					pai.addAll(wuTong.get(0));
				} else {
					pai.addAll(wuTong.get(wuTong.size() - 1));
				}
			} else {
				if ("1".equals(wuTong.get(0).get(0).split("-")[1])) {
					pai.addAll(wuTong.get(0));
				} else {
					pai.addAll(wuTong.get(wuTong.size() - 1));
				}
			}
		}
		return pai;
	}

	private static ArrayList<String> matchFlushByFlower(ArrayList<String> playerTemp) {
		ArrayList<ArrayList<String>> tonghuashun = flushByFlower(playerTemp);
		ArrayList<String> pai = new ArrayList<>();
		if (tonghuashun.size() > 0) {
			if (SSSComputeCards.getKingPaiCount(JSONArray.fromObject(playerTemp)) > 0) {
				boolean isHasMaxFlush = isHasMaxFlush(tonghuashun.get(tonghuashun.size() - 1));
				boolean isHasSecondFlush = isHasSecondFlush(tonghuashun.get(tonghuashun.size() - 1));
				if (isHasSecondFlush && !isHasMaxFlush) {
					pai.addAll(tonghuashun.get(0));
				} else {
					pai.addAll(tonghuashun.get(tonghuashun.size() - 1));
				}
			} else {
				if ("1".equals(tonghuashun.get(0).get(0).split("-")[1])
						&& !"1".equals(tonghuashun.get(tonghuashun.size() - 1).get(4).split("-")[1])) {
					pai.addAll(tonghuashun.get(0));
				} else {
					pai.addAll(tonghuashun.get(tonghuashun.size() - 1));
				}
			}
		}
		return pai;
	}

	private static ArrayList<String> matchBomb(ArrayList<String> playerTemp,int ruanguiCount) {
		List<String> list = new ArrayList<String>();
		if (ruanguiCount>0) {
			list.add("2");
			list.add("3");
			if (ruanguiCount == 4) {
				list.add("4");
				list.add("5");
			}
		}
		ArrayList<String> pai = new ArrayList<>();
		ArrayList<ArrayList<String>> tiezhi = bomb(playerTemp);
		ArrayList<ArrayList<String>> santiao = three(playerTemp);
		for (int i = 0; i < santiao.size(); i++) {
			for (String arrayList : santiao.get(i)) {
				if (arrayList.length()==3 && list.contains(arrayList.substring(2))) {
					tiezhi.add(santiao.get(i));
					break;
				}
			}
		}
		if (tiezhi.size() > 0) {
			if (SSSComputeCards.getKingPaiCount(JSONArray.fromObject(playerTemp)) > 0) {
				if (isHasA(tiezhi.get(0))) {
					pai.addAll(tiezhi.get(0));
				} else {
					pai.addAll(tiezhi.get(tiezhi.size() - 1));
				}
			} else {
				if ("1".equals(tiezhi.get(0).get(0).split("-")[1])) {
					pai.addAll(tiezhi.get(0));
				} else {
					pai.addAll(tiezhi.get(tiezhi.size() - 1));
				}
			}
		}
		return pai;
	}

	private static ArrayList<String> matchGourd(ArrayList<String> playerTemp) {
		ArrayList<String> pai = new ArrayList<>();
		ArrayList<ArrayList<String>> hulu = gourd(playerTemp);
		if (hulu.size() > 0) {
			if (SSSComputeCards.getKingPaiCount(JSONArray.fromObject(playerTemp)) > 0) {
				if ("1".equals(hulu.get(0).get(2).split("-")[1])) {
					pai.addAll(hulu.get(0));
				} else {
					pai.addAll(hulu.get(hulu.size() - 1));
				}
			} else {
				if ("1".equals(hulu.get(0).get(2).split("-")[1])) {
					pai.addAll(hulu.get(0));
				} else {
					pai.addAll(hulu.get(hulu.size() - 1));
				}
			}
		}
		return pai;
	}

	private static ArrayList<String> removeByList(ArrayList<String> playTemp, ArrayList<String> temp) {
		for (String s : temp) {
			playTemp.remove(s);
		}
		return playTemp;
	}

	private static boolean isUp(ArrayList<String> list, SSSGameRoomNew room) {

		boolean result = false;
		if (list.size() == 0) {
			return result;
		}
		int ruanguiCount = room.getRuanguiCount();
		if (ruanguiCount == 4) {
			for (String string : list) {
				if (string.equals("1-2") || string.equals("1-3") || string.equals("1-4") || string.equals("1-5")
						|| string.equals("2-2") || string.equals("2-3") || string.equals("2-4") || string.equals("2-5")
						|| string.equals("3-2") || string.equals("3-3") || string.equals("3-4") || string.equals("3-5")
						|| string.equals("4-2") || string.equals("4-3") || string.equals("4-4")
						|| string.equals("4-5")) {
					result = true;
					break;
				}
			}
		} else if (ruanguiCount == 2) {
			for (String string : list) {
				if (string.equals("1-2") || string.equals("1-3") || string.equals("2-2") || string.equals("2-3")
						|| string.equals("3-2") || string.equals("3-3") || string.equals("4-2")
						|| string.equals("4-3")) {
					result = true;
					break;
				}
			}
		}

		return result;
	}

	private static ArrayList<String> getRuanGuiFive(ArrayList<String> list, SSSGameRoomNew room) {

		ArrayList<String> five = new ArrayList<String>();
		int ruanguiCount = room.getRuanguiCount();

		// 2 3 4 5的数量
		int count2 = 0;
		int count3 = 0;
		int count4 = 0;
		int count5 = 0;
		int kingcount = 0;
		for (String string : list) {
			if (string.length() == 3 && string.endsWith("2")) {
				count2++;
			}
			if (string.length() == 3 && string.endsWith("3")) {
				count3++;
			}
			if (string.length() == 3 && string.endsWith("4")) {
				count4++;
			}
			if (string.length() == 3 && string.endsWith("5")) {
				count5++;
			}
			if (string.startsWith("5")) {
				kingcount++;
			}
		}
		if (count2 + kingcount == 4) {
			for (String string : list) {

			}
		}

		return five;
	}
}
