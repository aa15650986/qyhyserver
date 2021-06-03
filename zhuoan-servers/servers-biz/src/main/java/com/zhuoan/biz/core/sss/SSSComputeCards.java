package com.zhuoan.biz.core.sss;

import com.zhuoan.biz.model.sss.SSSGameRoomNew;
import com.zhuoan.constant.SSSConstant;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.*;

public class SSSComputeCards {

	private static final int none = -1;

	private static final int zero = 0;

	private static final int one = 1;

	private static final int two = 2;

	private static final int three = 3;

	private static final int flush = 4;

	private static final int sameFlower = 5;

	private static final int gourd = 6;

	// "5-1","4-13","1-13","1-4","4-4"]
	private static final int bomb = 7;

	private static final int flushByFlower = 8;

	private static final int both = 9;

	private static final int dashThree = 11;

	private static final int mideGourd = 10;

	private static final int six = 12;

	public static JSONObject compare(String[] card1, String[] card2, SSSGameRoomNew room) {
		// 是否是清一色房间
		boolean isSameColor = false;
		if (room.getRoomInfo().containsKey(SSSConstant.SSS_DATA_KET_SAME_COLOR)) {
			isSameColor = SSSConstant.SSS_SAME_COLOR_YES
					.equals(room.getRoomInfo().getString(SSSConstant.SSS_DATA_KET_SAME_COLOR));
		}
		if (isSameColor) {
			return compareForSameColor(card1, card2, room.getRuanguiCount());
		} else {
			return compare(card1, card2, room.getRuanguiCount());
		}
	}

	private static JSONObject compareForSameColor(String[] card1, String[] card2, int ruanguiCount) {
		JSONArray p1 = new JSONArray();
		JSONArray p2 = new JSONArray();
		JSONArray threeP1 = new JSONArray();
		JSONArray threeP2 = new JSONArray();
		JSONArray five1P1 = new JSONArray();
		JSONArray five1P2 = new JSONArray();
		JSONArray five2P1 = new JSONArray();
		JSONArray five2P2 = new JSONArray();

		for (int i = 0; i < card1.length; i++) {
			if (i < 3) {
				threeP1.add(card1[i]);
				threeP2.add(card2[i]);
			} else if (i < 8) {
				five1P1.add(card1[i]);
				five1P2.add(card2[i]);
			} else {
				five2P1.add(card1[i]);
				five2P2.add(card2[i]);
			}
		}
		p1.add(threeP1);
		p1.add(five1P1);
		p1.add(five2P1);
		p2.add(threeP2);
		p2.add(five1P2);
		p2.add(five2P2);

		JSONObject result = new JSONObject();
		JSONArray resultSum = new JSONArray();
		JSONArray resultA = new JSONArray();
		JSONArray resultB = new JSONArray();

		int sumA = 0;
		int sumB = 0;

		for (int k = 0; k < p1.size(); k++) {
			JSONArray player1 = (JSONArray) p1.get(k);
			JSONArray player2 = (JSONArray) p2.get(k);

			int type1 = none;
			int type2 = none;
			int sixPlayer1 = isLiutong(player1, ruanguiCount);
			int bothPlayer1 = none;
			int flushByflowerPlayer1 = none;
			int bombPlayer1 = none;
			int gourdPlayer1 = none;
			int sameFlowerPlayer1 = none;
			int flushPlayer1 = none;
			int threePlayer1 = none;
			int twoPlayer1 = none;
			int onePlayer1 = none;
			if (sixPlayer1 == none) {
				bothPlayer1 = isBoth(player1, ruanguiCount);
				if (bothPlayer1 == none) {
					bombPlayer1 = isBomb(player1, ruanguiCount);
					if (bombPlayer1 == none) {
						flushByflowerPlayer1 = isFlushByFlower(player1);
						if (flushByflowerPlayer1 == none) {
							gourdPlayer1 = isGourd(player1);
							if (gourdPlayer1 == none) {
								sameFlowerPlayer1 = isSameFlower(player1);
								if (sameFlowerPlayer1 == none) {
									flushPlayer1 = isFlush(player1);
									if (flushPlayer1 == none) {
										threePlayer1 = isThree(player1,ruanguiCount);
										if (threePlayer1 == none) {
											twoPlayer1 = isTwo(player1);
											if (twoPlayer1 == none) {
												onePlayer1 = isOne(player1);
												if (onePlayer1 == zero) {
													type1 = zero;
												} else {
													type1 = onePlayer1;
												}
											} else {
												type1 = twoPlayer1;
											}
										} else {
											type1 = threePlayer1;
											if (k == 0) {
												type1 = dashThree;
											}
										}
									} else {
										type1 = flushPlayer1;
									}
								} else {
									type1 = sameFlowerPlayer1;
								}
							} else {
								type1 = gourdPlayer1;
								if (k == 1) {
									type1 = mideGourd;
								}
							}
						} else {
							type1 = flushByflowerPlayer1;
						}
					} else {
						type1 = bombPlayer1;
					}
				} else {
					type1 = bothPlayer1;
				}
			} else {
				type1 = sixPlayer1;
			}

			int sixPlayer2 = isLiutong(player2, ruanguiCount);
			int bothPlayer2 = none;
			int flushByflowerPlayer2 = none;
			int bombPlayer2 = none;
			int gourdPlayer2 = none;
			int sameFlowerPlayer2 = none;
			int flushPlayer2 = none;
			int threePlayer2 = none;
			int twoPlayer2 = none;
			int onePlayer2 = none;
			if (sixPlayer2 == none) {
				bothPlayer2 = isBoth(player2, ruanguiCount);

				if (bothPlayer2 == none) {
					bombPlayer2 = isBomb(player2, ruanguiCount);
					if (bombPlayer2 == none) {
						flushByflowerPlayer2 = isFlushByFlower(player2);
						if (flushByflowerPlayer2 == none) {
							gourdPlayer2 = isGourd(player2);
							if (gourdPlayer2 == none) {
								sameFlowerPlayer2 = isSameFlower(player2);
								if (sameFlowerPlayer2 == none) {
									flushPlayer2 = isFlush(player2);
									if (flushPlayer2 == none) {
										threePlayer2 = isThree(player2,ruanguiCount);
										if (threePlayer2 == none) {
											twoPlayer2 = isTwo(player2);
											if (twoPlayer2 == none) {
												onePlayer2 = isOne(player2);
												if (onePlayer2 == zero) {
													type2 = zero;
												} else {
													type2 = onePlayer2;
												}
											} else {
												type2 = twoPlayer2;
											}
										} else {
											type2 = threePlayer2;
											if (k == 0) {
												type2 = dashThree;
											}
										}
									} else {
										type2 = flushPlayer2;
									}
								} else {
									type2 = sameFlowerPlayer2;
								}
							} else {
								type2 = gourdPlayer2;
								if (k == 1) {
									type2 = mideGourd;
								}
							}
						} else {
							type2 = flushByflowerPlayer2;
						}
					} else {
						type2 = bombPlayer2;
					}
				} else {
					type2 = bothPlayer2;
				}
			} else {
				type2 = sixPlayer2;
			}
			JSONObject temp = new JSONObject();
			int score = 0;

			if (sixPlayer1 > sixPlayer2) {
				score = 1;
				if (k == 1) {
					score += 39;
				} else if (k == 2) {
					score += 19;
				}
			} else if (sixPlayer1 == sixPlayer2) {
				if (sixPlayer1 > 0) {
					int i = compareBoth(player1, player2, ruanguiCount);
					if (i == 1) {
						score += 1;
						if (k == 1) {
							score += 39;
						} else if (k == 2) {
							score += 19;
						}
					} else if (i == -1) {
						score += -1;
						if (k == 1) {
							score += -39;
						} else if (k == 2) {
							score += -19;
						}
					}
				} else {
					// 五同
					if (bothPlayer1 > bothPlayer2) {
						score = 1;
						if (k == 1) {
							score += 19;
						} else if (k == 2) {
							score += 9;
						}
					} else if (bothPlayer1 == bothPlayer2) {
						if (bothPlayer1 > 0) {
							int i = compareBoth(player1, player2, ruanguiCount);
							if (i == 1) {
								score += 1;
								if (k == 1) {
									score += 19;
								} else if (k == 2) {
									score += 9;
								}
							} else if (i == -1) {
								score += -1;
								if (k == 1) {
									score += -19;
								} else if (k == 2) {
									score += -9;
								}
							}
						} else {
							// 铁支
							if (bombPlayer1 > bombPlayer2) {
								score = 1;
								if (k == 1) {
									score += 9;
								} else if (k == 2) {
									score += 4;
								}
							} else if (bombPlayer1 == bombPlayer2) {
								if (bombPlayer1 > 0) {
									int i = compareBomb(player1, player2, ruanguiCount);
									if (i == 1) {
										score += 1;
										if (k == 1) {
											score += 9;
										} else if (k == 2) {
											score += 4;
										}
									} else if (i == -1) {
										score += -1;
										if (k == 1) {
											score += -9;
										} else if (k == 2) {
											score += -4;
										}
									}
								} else {
									// 同花顺
									if (flushByflowerPlayer1 > flushByflowerPlayer2) {
										score += 1;
										if (k == 1) {
											score += 7;
										} else if (k == 2) {
											score += 3;
										}
									} else if (flushByflowerPlayer1 == flushByflowerPlayer2) {
										if (flushByflowerPlayer1 > 0) {
											int i = compareFlushByflower(player1, player2);
											if (i == 1) {
												score += 1;
												if (k == 1) {
													score += 7;
												} else if (k == 2) {
													score += 3;
												}
											} else if (i == -1) {
												score += -1;
												if (k == 1) {
													score += -7;
												} else if (k == 2) {
													score += -3;
												}
											}
										} else {
											// 葫芦
											if (gourdPlayer1 > gourdPlayer2) {
												score += 1;
												if (k == 1) {
													score += 1;
												}
											} else if (gourdPlayer1 == gourdPlayer2) {
												if (gourdPlayer1 > 0) {
													int i = compareGourd(player1, player2);
													if (i == 1) {
														score += 1;
														if (k == 1) {
															score += 1;
														}
													} else if (i == -1) {
														score += -1;
														if (k == 1) {
															score += -1;
														}
													}
												} else {
													// 同花
													if (sameFlowerPlayer1 > sameFlowerPlayer2) {
														score += 1;
													} else if (sameFlowerPlayer1 == sameFlowerPlayer2) {
														if (sameFlowerPlayer1 > 0) {
															int i = compareSameFlower(player1, player2);
															if (i == 1) {
																score += 1;
															} else if (i == -1) {
																score += -1;
															}
														} else {
															// 顺子
															if (flushPlayer1 > flushPlayer2) {
																score += 1;
															} else if (flushPlayer1 == flushPlayer2) {
																if (flushPlayer1 > 0) {
																	int i = compareFlush(player1, player2);
																	if (i == 1) {
																		score += 1;
																	} else if (i == -1) {
																		score += -1;
																	}
																} else {
																	// 三条
																	if (threePlayer1 > threePlayer2) {
																		score += 1;
																		if (k == 0) {
																			score += 2;
																		}
																	} else if (threePlayer1 == threePlayer2) {
																		if (threePlayer1 > 0) {
																			int i = compareThree(player1, player2);
																			if (i == 1) {
																				score += 1;
																				if (k == 0) {
																					score += 2;
																				}
																			} else if (i == -1) {
																				score += -1;
																				if (k == 0) {
																					score += -2;
																				}
																			}
																		} else {
																			// 两对
																			if (twoPlayer1 > twoPlayer2) {
																				score += 1;
																			} else if (twoPlayer1 == twoPlayer2) {
																				if (twoPlayer1 > 0) {
																					int i = compareTwo(player1,
																							player2);
																					if (i == 1) {
																						score += 1;
																					} else if (i == -1) {
																						score += -1;
																					}
																				} else {
																					// 一对
																					if (onePlayer1 > onePlayer2) {
																						score += 1;
																					} else if (onePlayer1 == onePlayer2) {
																						if (onePlayer1 > 0) {
																							int i = compareOne(player1,
																									player2);
																							if (i == 1) {
																								score += 1;
																							} else if (i == -1) {
																								score += -1;
																							}
																						} else {
																							// 乌龙
																							int i = compareZero(player1,
																									player2);
																							if (i == 1) {
																								score += 1;
																							} else if (i == -1) {
																								score += -1;
																							}
																						}
																					} else if (onePlayer1 < onePlayer2) {
																						score += -1;
																					}
																				}
																			} else if (twoPlayer1 < twoPlayer2) {
																				score += -1;
																			}
																		}
																	} else if (threePlayer1 < threePlayer2) {
																		score += -1;
																		if (k == 0) {
																			score += -2;
																		}
																	}
																}
															} else if (flushPlayer1 < flushPlayer2) {
																score += -1;
															}
														}
													} else if (sameFlowerPlayer1 < sameFlowerPlayer2) {
														score += -1;
													}
												}
											} else if (gourdPlayer1 < gourdPlayer2) {
												score += -1;
												if (k == 1) {
													score += -1;
												}
											}
										}
									} else if (flushByflowerPlayer1 < flushByflowerPlayer2) {
										score += -1;
										if (k == 1) {
											score += -7;
										} else if (k == 2) {
											score += -3;
										}
									}
								}
							} else if (bombPlayer1 < bombPlayer2) {
								score += -1;
								if (k == 1) {
									score += -9;
								} else if (k == 2) {
									score += -4;
								}
							}
						}
					} else if (bothPlayer1 < bothPlayer2) {
						score += -1;
						if (k == 1) {
							score += -19;
						} else if (k == 2) {
							score += -9;
						}
					}
				}
			} else if (sixPlayer1 < sixPlayer2) {
				score += -1;
				if (k == 1) {
					score += -39;
				} else if (k == 2) {
					score += -19;
				}
			}

			JSONObject tempResult1 = new JSONObject();
			JSONObject tempResult2 = new JSONObject();

			tempResult1.put("score", score);
			tempResult1.put("type", type1);
			tempResult2.put("score", -score);
			tempResult2.put("type", type2);

			resultA.add(tempResult1);
			resultB.add(tempResult2);
			sumA += score;
			sumB += -score;
		}

		resultSum.add(resultA);
		resultSum.add(resultB);

		result.put("result", resultSum);
		result.put("A", sumA);
		result.put("B", sumB);

		return result;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		JSONArray js1 = new JSONArray();
		js1.add("1-3");
		js1.add("1-3");
		js1.add("3-3");
		
		js1.add("4-3");
		js1.add("5-3");
		//js1.add("1-3");
		//js1add("1-8");
		//js1.add("1-9");
		JSONArray js2 = new JSONArray();
		//js1.add("5-1");
		///js1.add("5-1");
		//js1.add("1-1");
		//js1.add("1-5");
		//js1.add("3-12");
		
	//	System.out.println(judge(js1, 4));
		//System.out.println(compareBomb(js1, js2, 4));
		
		
		System.out.println(isLiutong(js1, 4));
		//System.out.println(compareBomb(js1, js2, 4));
		//int i =1;
		//System.out.println(i+= -15);
	}

	private static JSONObject compare(String[] card1, String[] card2, int ruanguiCount) {
		JSONArray p1 = new JSONArray();
		JSONArray p2 = new JSONArray();
		JSONArray threeP1 = new JSONArray();
		JSONArray threeP2 = new JSONArray();
		JSONArray five1P1 = new JSONArray();
		JSONArray five1P2 = new JSONArray();
		JSONArray five2P1 = new JSONArray();
		JSONArray five2P2 = new JSONArray();

		for (int i = 0; i < card1.length; i++) {
			if (i < 3) {
				threeP1.add(card1[i]);
				threeP2.add(card2[i]);
			} else if (i < 8) {
				five1P1.add(card1[i]);
				five1P2.add(card2[i]);
			} else {
				five2P1.add(card1[i]);
				five2P2.add(card2[i]);
			}
		}
		p1.add(threeP1);
		p1.add(five1P1);
		p1.add(five2P1);
		p2.add(threeP2);
		p2.add(five1P2);
		p2.add(five2P2);

		JSONObject result = new JSONObject();
		JSONArray resultSum = new JSONArray();
		JSONArray resultA = new JSONArray();
		JSONArray resultB = new JSONArray();

		int sumA = 0;
		int sumB = 0;

		for (int k = 0; k < p1.size(); k++) {
			JSONArray player1 = (JSONArray) p1.get(k);
			JSONArray player2 = (JSONArray) p2.get(k);

			int type1 = none;
			int type2 = none;

			int sixPlayer1 = isLiutong(player1, ruanguiCount);
			int bothPlayer1 = none;
			int flushByflowerPlayer1 = none;
			int bombPlayer1 = none;
			int gourdPlayer1 = none;
			int sameFlowerPlayer1 = none;
			int flushPlayer1 = none;
			int threePlayer1 = none;
			int twoPlayer1 = none;
			int onePlayer1 = none;
			if (sixPlayer1 == none) {
				bothPlayer1 = isBoth(player1, ruanguiCount);
				if (bothPlayer1 == none) {
					flushByflowerPlayer1 = isFlushByFlower(player1);
					if (flushByflowerPlayer1 == none) {
						bombPlayer1 = isBomb(player1, ruanguiCount);
						if (bombPlayer1 == none) {
							gourdPlayer1 = isGourd(player1);
							if (gourdPlayer1 == none) {
								sameFlowerPlayer1 = isSameFlower(player1);
								if (sameFlowerPlayer1 == none) {
									flushPlayer1 = isFlush(player1);
									if (flushPlayer1 == none) {
										threePlayer1 = isThree(player1,ruanguiCount);
										if (threePlayer1 == none) {
											twoPlayer1 = isTwo(player1);
											if (twoPlayer1 == none) {
												onePlayer1 = isOne(player1);
												if (onePlayer1 == zero) {
													type1 = zero;
												} else {
													type1 = onePlayer1;
												}
											} else {
												type1 = twoPlayer1;
											}
										} else {
											type1 = threePlayer1;
											if (k == 0 && type1!=bomb) {
												type1 = dashThree;
											}
										}
									} else {
										type1 = flushPlayer1;
									}
								} else {
									type1 = sameFlowerPlayer1;
								}
							} else {
								type1 = gourdPlayer1;
								if (k == 1) {
									type1 = mideGourd;
								}
							}
						} else {
							type1 = bombPlayer1;
						}
					} else {
						type1 = flushByflowerPlayer1;
					}
				} else {
					type1 = bothPlayer1;
				}
			} else {
				type1 = sixPlayer1;
			}

			int sixPlayer2 = isLiutong(player2, ruanguiCount);
			int bothPlayer2 = none;
			int flushByflowerPlayer2 = none;
			int bombPlayer2 = none;
			int gourdPlayer2 = none;
			int sameFlowerPlayer2 = none;
			int flushPlayer2 = none;
			int threePlayer2 = none;
			int twoPlayer2 = none;
			int onePlayer2 = none;
			if (sixPlayer2 == none) {
				bothPlayer2 = isBoth(player2, ruanguiCount);
				if (bothPlayer2 == none) {
					flushByflowerPlayer2 = isFlushByFlower(player2);
					if (flushByflowerPlayer2 == none) {
						bombPlayer2 = isBomb(player2, ruanguiCount);
						if (bombPlayer2 == none) {
							gourdPlayer2 = isGourd(player2);
							if (gourdPlayer2 == none) {
								sameFlowerPlayer2 = isSameFlower(player2);
								if (sameFlowerPlayer2 == none) {
									flushPlayer2 = isFlush(player2);
									if (flushPlayer2 == none) {
										threePlayer2 = isThree(player2,ruanguiCount);
										if (threePlayer2 == none) {
											twoPlayer2 = isTwo(player2);
											if (twoPlayer2 == none) {
												onePlayer2 = isOne(player2);
												if (onePlayer2 == zero) {
													type2 = zero;
												} else {
													type2 = onePlayer2;
												}
											} else {
												type2 = twoPlayer2;
											}
										} else {
											type2 = threePlayer2;
											if (k == 0 && type2!=bomb) {
												type2 = dashThree;
											}
										}
									} else {
										type2 = flushPlayer2;
									}
								} else {
									type2 = sameFlowerPlayer2;
								}
							} else {
								type2 = gourdPlayer2;
								if (k == 1) {
									type2 = mideGourd;
								}
							}
						} else {
							type2 = bombPlayer2;
						}
					} else {
						type2 = flushByflowerPlayer2;
					}
				} else {
					type2 = bothPlayer2;
				}
			} else {
				type2 = sixPlayer2;
			}

			JSONObject temp = new JSONObject();
			int score = 0;

			// 六同
			if (sixPlayer1 > sixPlayer2) {
				score = 1;
				if (k == 1) {
					score += 39;
				} else if (k == 2) {
					score += 19;
				}
			} else if (sixPlayer1 == sixPlayer2) {
				if (sixPlayer1 > 0) {
					int i = compareLiuTong(player1, player2, ruanguiCount);
					if (i == 1) {
						score += 1;
						if (k == 1) {
							score += 39;
						} else if (k == 2) {
							score += 19;
						}
					} else if (i == -1) {
						score += -1;
						if (k == 1) {
							score += -39;
						} else if (k == 2) {
							score += -19;
						}
					}
				} else {
					// 五同
					if (bothPlayer1 > bothPlayer2) {
						score = 1;
						if (k == 1) {
							score += 19;
						} else if (k == 2) {
							score += 9;
						}
					} else if (bothPlayer1 == bothPlayer2) {
						if (bothPlayer1 > 0) {
							int i = compareBoth(player1, player2, ruanguiCount);
							if (i == 1) {
								score += 1;
								if (k == 1) {
									score += 19;
								} else if (k == 2) {
									score += 9;
								}
							} else if (i == -1) {
								score += -1;
								if (k == 1) {
									score += -19;
								} else if (k == 2) {
									score += -9;
								}
							}
						} else {
							// 同花顺
							if (flushByflowerPlayer1 > flushByflowerPlayer2) {
								score = 1;
								if (k == 1) {
									score += 9;
								} else if (k == 2) {
									score += 4;
								}
							} else if (flushByflowerPlayer1 == flushByflowerPlayer2) {
								if (flushByflowerPlayer1 > 0) {
									int i = compareFlushByflower(player1, player2);
									if (i == 1) {
										score += 1;
										if (k == 1) {
											score += 9;
										} else if (k == 2) {
											score += 4;
										}
									} else if (i == -1) {
										score += -1;
										if (k == 1) {
											score += -9;
										} else if (k == 2) {
											score += -4;
										}
									}
								} else {
									// 铁支
									if (bombPlayer1 > bombPlayer2) {
										score += 1;
										if (k == 1) {
											score += 7;
										} else if (k == 2) {
											score += 3;
										}
									} else if (bombPlayer1 == bombPlayer2) {
										if (bombPlayer1 > 0) {
											int i = compareBomb(player1, player2, ruanguiCount);
											if (i == 1) {
												score += 1;
												if (k == 1) {
													score += 7;
												} else if (k == 2) {
													score += 3;
												}
											} else if (i == -1) {
												score += -1;
												if (k == 1) {
													score += -7;
												} else if (k == 2) {
													score += -3;
												}
											}
										} else {
											// 葫芦
											if (gourdPlayer1 > gourdPlayer2) {
												score += 1;
												if (k == 1) {
													score += 1;
												}
											} else if (gourdPlayer1 == gourdPlayer2) {
												if (gourdPlayer1 > 0) {
													int i = compareGourd(player1, player2);
													if (i == 1) {
														score += 1;
														if (k == 1) {
															score += 1;
														}
													} else if (i == -1) {
														score += -1;
														if (k == 1) {
															score += -1;
														}
													}
												} else {
													// 同花
													if (sameFlowerPlayer1 > sameFlowerPlayer2) {
														score += 1;
													} else if (sameFlowerPlayer1 == sameFlowerPlayer2) {
														if (sameFlowerPlayer1 > 0) {
															int i = compareSameFlower(player1, player2);
															if (i == 1) {
																score += 1;
															} else if (i == -1) {
																score += -1;
															}
														} else {
															// 顺子
															if (flushPlayer1 > flushPlayer2) {
																score += 1;
															} else if (flushPlayer1 == flushPlayer2) {
																if (flushPlayer1 > 0) {
																	int i = compareFlush(player1, player2);
																	if (i == 1) {
																		score += 1;
																	} else if (i == -1) {
																		score += -1;
																	}
																} else {
																	// 三条
																	if (threePlayer1 > threePlayer2) {
																		score += 1;
																		if (threePlayer1==bomb && k==0) {
																			score+=15;
																		}else if (k == 0 && threePlayer1!=bomb) {
																			score += 2;
																		}
																	} else if (threePlayer1 == threePlayer2) {
																		if (threePlayer1 > 0) {
																			int i = compareThree(player1, player2);
																			if (i == 1) {
																				score += 1;
																				if (k == 0) {
																					if (threePlayer1==bomb) {
																						score+=15;
																					}else {
																					score += 2;
																					}
																				}
																			} else if (i == -1) {
																				score += -1;
																				if (k == 0) {
																					if (threePlayer1==bomb) {
																						score+=-15;
																					}else {
																					score += -2;
																					}
																				}
																			}
																		} else {
																			// 两对
																			if (twoPlayer1 > twoPlayer2) {
																				score += 1;
																			} else if (twoPlayer1 == twoPlayer2) {
																				if (twoPlayer1 > 0) {
																					int i = compareTwo(player1,
																							player2);
																					if (i == 1) {
																						score += 1;
																					} else if (i == -1) {
																						score += -1;
																					}
																				} else {
																					// 一对
																					if (onePlayer1 > onePlayer2) {
																						score += 1;
																					} else if (onePlayer1 == onePlayer2) {
																						if (onePlayer1 > 0) {
																							int i = compareOne(player1,
																									player2);
																							if (i == 1) {
																								score += 1;
																							} else if (i == -1) {
																								score += -1;
																							}
																						} else {
																							// 乌龙
																							int i = compareZero(player1,
																									player2);
																							if (i == 1) {
																								score += 1;
																							} else if (i == -1) {
																								score += -1;
																							}
																						}
																					} else if (onePlayer1 < onePlayer2) {
																						score += -1;
																					}
																				}
																			} else if (twoPlayer1 < twoPlayer2) {
																				score += -1;
																			}
																		}
																	} else if (threePlayer1 < threePlayer2) {
																		score += -1;
																		if (k == 0 && threePlayer1==bomb) {
																			score += 16;
																		}else if(k==0 && threePlayer2 ==bomb){
																			score += -15;
																		}else if(k==0){
																			score+= -2;
																		}
																	}
																}
															} else if (flushPlayer1 < flushPlayer2) {
																score += -1;
															}
														}
													} else if (sameFlowerPlayer1 < sameFlowerPlayer2) {
														score += -1;
													}
												}
											} else if (gourdPlayer1 < gourdPlayer2) {
												score += -1;
												if (k == 1) {
													score += -1;
												}
											}
										}
									} else if (bombPlayer1 < bombPlayer2) {
										score += -1;
										if (k == 1) {
											score += -7;
										} else if (k == 2) {
											score += -3;
										}
									}
								}
							} else if (flushByflowerPlayer1 < flushByflowerPlayer2) {
								score += -1;
								if (k == 1) {
									score += -9;
								} else if (k == 2) {
									score += -4;
								}
							}
						}
					} else if (bothPlayer1 < bothPlayer2) {
						score += -1;
						if (k == 1) {
							score += -19;
						} else if (k == 2) {
							score += -9;
						}
					}
				}
			} else if (sixPlayer1 < sixPlayer2) {
				score += -1;
				if (k == 1) {
					score += -39;
				} else if (k == 2) {
					score += -19;
				}
			}

			JSONObject tempResult1 = new JSONObject();
			JSONObject tempResult2 = new JSONObject();

			tempResult1.put("score", score);
			tempResult1.put("type", type1);
			tempResult2.put("score", -score);
			tempResult2.put("type", type2);

			resultA.add(tempResult1);
			resultB.add(tempResult2);
			sumA += score;
			sumB += -score;
		}

		resultSum.add(resultA);
		resultSum.add(resultB);

		result.put("result", resultSum);
		result.put("A", sumA);
		result.put("B", sumB);

		return result;
	}

	public static JSONArray judge(JSONArray player, SSSGameRoomNew room) {
		// 如果是清一色,则铁支>同花顺
		if (room.getRoomInfo().containsKey(SSSConstant.SSS_DATA_KET_SAME_COLOR)) {
			if (SSSConstant.SSS_SAME_COLOR_YES
					.equals(room.getRoomInfo().getString(SSSConstant.SSS_DATA_KET_SAME_COLOR))) {
				return judgeBySameColor(player, room.getRuanguiCount());
			}
		}
		return judge(player, room.getRuanguiCount());
	}

	private static JSONArray judgeBySameColor(JSONArray player, int ruanguiCount) {
		JSONArray result = new JSONArray();
		JSONArray num = new JSONArray();

		ListIterator<Object> it = (ListIterator<Object>) player.iterator();
		JSONArray temp = new JSONArray();
		JSONArray temp1 = new JSONArray();
		JSONArray temp2 = new JSONArray();
		JSONArray temp3 = new JSONArray();
		while (it.hasNext()) {
			String string = (String) it.next();
			if (temp1.size() < 3) {
				temp1.add(string);
			} else if (temp2.size() < 5) {
				temp2.add(string);
			} else if (temp3.size() < 5) {
				temp3.add(string);
			}
		}
		temp.add(temp1);
		temp.add(temp2);
		temp.add(temp3);
		ListIterator<Object> itTemp = (ListIterator<Object>) temp.iterator();
		while (itTemp.hasNext()) {
			JSONArray playerJsonArray = (JSONArray) itTemp.next();
			int sixPlayer = isLiutong(playerJsonArray, ruanguiCount);

			int bothPlayer = none;
			int flushByflowerPlayer = none;
			int bombPlayer = none;
			int gourdPlayer = none;
			int sameFlowerPlayer = none;
			int flushPlayer = none;
			int threePlayer = none;
			int twoPlayer = none;
			int onePlayer = none;
			if (sixPlayer == none) {
				bothPlayer = isBoth(playerJsonArray, ruanguiCount);

				if (bothPlayer == none) {
					bombPlayer = isBomb(playerJsonArray, ruanguiCount);
					if (bombPlayer == none) {
						flushByflowerPlayer = isFlushByFlower(playerJsonArray);
						if (flushByflowerPlayer == none) {
							gourdPlayer = isGourd(playerJsonArray);
							if (gourdPlayer == none) {
								sameFlowerPlayer = isSameFlower(playerJsonArray);
								if (sameFlowerPlayer == none) {
									flushPlayer = isFlush(playerJsonArray);
									if (flushPlayer == none) {
										threePlayer = isThree(playerJsonArray,ruanguiCount);
										if (threePlayer == none) {
											twoPlayer = isTwo(playerJsonArray);
											if (twoPlayer == none) {
												onePlayer = isOne(playerJsonArray);
												if (onePlayer == zero) {
													result.add("乌龙");
													num.add(zero);
												} else {
													result.add("一对");
													num.add(onePlayer);
												}
											} else {
												result.add("两对");
												num.add(twoPlayer);
											}
										} else {
											if (playerJsonArray.size() == 3) {
												if (threePlayer==bomb) {
													result.add("铁支");
												}else {
													result.add("冲三");
												}
											} else {
												result.add("三条");
											}
											num.add(threePlayer);
										}
									} else {
										result.add("顺子");
										num.add(flushPlayer);
									}
								} else {
									result.add("同花");
									num.add(sameFlowerPlayer);
								}
							} else {
								if (it.hasPrevious() && it.hasNext()) {
									result.add("中墩葫芦");
								} else {
									result.add("葫芦");
								}
								num.add(gourdPlayer);
							}
						} else {
							result.add("同花顺");
							num.add(flushByflowerPlayer);
						}
					} else {
						result.add("铁支");
						num.add(bombPlayer);
					}
				} else {
					result.add("五同");
					num.add(bothPlayer);
				}
			} else {
				result.add("六同");
				num.add(sixPlayer);
			}
		}
		if (num.getInt(0) > num.getInt(1) || num.getInt(1) > num.getInt(2)) {
			result.clear();
			result.add("倒水");
		} else {
			if (num.getInt(0) == num.getInt(1)) {
				if (num.getInt(0) == zero) {
					int i = compareZero(temp.getJSONArray(0), temp.getJSONArray(1));
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(0) == one) {
					int i = compareOne(temp.getJSONArray(0), temp.getJSONArray(1));
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				}
			}
			if (num.getInt(1) == num.getInt(2)) {
				if (num.getInt(1) == zero) {
					int i = compareZero(temp.getJSONArray(1), temp.getJSONArray(2));
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(1) == one) {
					int i = compareOne(temp.getJSONArray(1), temp.getJSONArray(2));
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(1) == two) {
					int i = compareTwo(temp.getJSONArray(1), temp.getJSONArray(2));
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(1) == three) {
					int i = compareThree(temp.getJSONArray(1), temp.getJSONArray(2));
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(1) == flush) {
					int i = compareFlush(temp.getJSONArray(1), temp.getJSONArray(2));
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(1) == sameFlower) {
					int i = compareSameFlower(temp.getJSONArray(1), temp.getJSONArray(2));
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(1) == gourd) {
					int i = compareGourd(temp.getJSONArray(1), temp.getJSONArray(2));
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(1) == bomb) {
					if (compareBomb(temp.getJSONArray(1), temp.getJSONArray(2), ruanguiCount) == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(1) == flushByFlower) {
					int i = compareFlushByflower(temp.getJSONArray(1), temp.getJSONArray(2));
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(1) == both) {
					int i = compareBoth(temp.getJSONArray(1), temp.getJSONArray(2), ruanguiCount);
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(1) == six) {
					int i = compareBoth(temp.getJSONArray(1), temp.getJSONArray(2), ruanguiCount);
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				}
			}
		}
		return result;
	}

	public static JSONArray judge(JSONArray player, int ruanguiCount) {
		JSONArray result = new JSONArray();
		JSONArray num = new JSONArray();

		ListIterator<Object> it = (ListIterator<Object>) player.iterator();
		JSONArray temp = new JSONArray();
		JSONArray temp1 = new JSONArray();
		JSONArray temp2 = new JSONArray();
		JSONArray temp3 = new JSONArray();
		while (it.hasNext()) {
			String string = (String) it.next();
			if (temp1.size() < 3) {
				temp1.add(string);
			} else if (temp2.size() < 5) {
				temp2.add(string);
			} else if (temp3.size() < 5) {
				temp3.add(string);
			}
		}
		temp.add(temp1);
		temp.add(temp2);
		temp.add(temp3);
		ListIterator<Object> itTemp = (ListIterator<Object>) temp.iterator();
		while (itTemp.hasNext()) {
			JSONArray playerJsonArray = (JSONArray) itTemp.next();
			int sixPlayer = isLiutong(playerJsonArray, ruanguiCount);
			int bothPlayer = none;
			int flushByflowerPlayer = none;
			int bombPlayer = none;
			int gourdPlayer = none;
			int sameFlowerPlayer = none;
			int flushPlayer = none;
			int threePlayer = none;
			int twoPlayer = none;
			int onePlayer = none;
			if (sixPlayer == none) {
				bothPlayer = isBoth(playerJsonArray, ruanguiCount);
				if (bothPlayer == none) {
					flushByflowerPlayer = isFlushByFlower(playerJsonArray);
					if (flushByflowerPlayer == none) {
						bombPlayer = isBomb(playerJsonArray, ruanguiCount);
						if (bombPlayer == none) {
							gourdPlayer = isGourd(playerJsonArray);
							if (gourdPlayer == none) {
								sameFlowerPlayer = isSameFlower(playerJsonArray);
								if (sameFlowerPlayer == none) {
									flushPlayer = isFlush(playerJsonArray);
									if (flushPlayer == none) {
										threePlayer = isThree(playerJsonArray,ruanguiCount);
										if (threePlayer == none) {
											twoPlayer = isTwo(playerJsonArray);
											if (twoPlayer == none) {
												onePlayer = isOne(playerJsonArray);
												if (onePlayer == zero) {
													result.add("乌龙");
													num.add(zero);
												} else {
													result.add("一对");
													num.add(onePlayer);
												}
											} else {
												result.add("两对");
												num.add(twoPlayer);
											}
										} else {
											if (playerJsonArray.size() == 3) {
												if (threePlayer==bomb) {
													result.add("铁支");
												}else {
													result.add("冲三");
												}
												
											} else {
												result.add("三条");
											}
											num.add(threePlayer);
										}
									} else {
										result.add("顺子");
										num.add(flushPlayer);
									}
								} else {
									result.add("同花");
									num.add(sameFlowerPlayer);
								}
							} else {
								if (it.hasPrevious() && it.hasNext()) {
									result.add("中墩葫芦");
								} else {
									result.add("葫芦");
								}
								num.add(gourdPlayer);
							}
						} else {
							result.add("铁支");
							num.add(bombPlayer);
						}
					} else {
						result.add("同花顺");
						num.add(flushByflowerPlayer);
					}
				} else {
					result.add("五同");
					num.add(bothPlayer);
				}
			} else {
				result.add("六同");
				num.add(sixPlayer);
			}
		}
		System.out.println(num);
		if (num.getInt(0) > num.getInt(1) || num.getInt(1) > num.getInt(2)) {
			result.clear();
			result.add("倒水");
		} else {
			if (num.getInt(0) == num.getInt(1)) {
				if (num.getInt(0) == zero) {
					int i = compareZero(temp.getJSONArray(0), temp.getJSONArray(1));
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(0) == one) {
					int i = compareOne(temp.getJSONArray(0), temp.getJSONArray(1));
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				}
			}
			if (num.getInt(1) == num.getInt(2)) {
				if (num.getInt(1) == zero) {
					int i = compareZero(temp.getJSONArray(1), temp.getJSONArray(2));
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(1) == one) {
					int i = compareOne(temp.getJSONArray(1), temp.getJSONArray(2));
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(1) == two) {
					int i = compareTwo(temp.getJSONArray(1), temp.getJSONArray(2));
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(1) == three) {
					int i = compareThree(temp.getJSONArray(1), temp.getJSONArray(2));
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(1) == flush) {
					int i = compareFlush(temp.getJSONArray(1), temp.getJSONArray(2));
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(1) == sameFlower) {
					int i = compareSameFlower(temp.getJSONArray(1), temp.getJSONArray(2));
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(1) == gourd) {
					int i = compareGourd(temp.getJSONArray(1), temp.getJSONArray(2));
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(1) == bomb) {
					System.out.println(temp.getJSONArray(1));
					System.out.println(temp.getJSONArray(2));
					if (compareBomb(temp.getJSONArray(1), temp.getJSONArray(2), ruanguiCount) == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(1) == flushByFlower) {
					int i = compareFlushByflower(temp.getJSONArray(1), temp.getJSONArray(2));
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(1) == both) {
					int i = compareBoth(temp.getJSONArray(1), temp.getJSONArray(2), ruanguiCount);
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				} else if (num.getInt(1) == six) {
					int i = compareBoth(temp.getJSONArray(1), temp.getJSONArray(2), ruanguiCount);
					if (i == 1) {
						result.clear();
						result.add("倒水");
					}
				}
			}
		}
		return result;
	}

	public static ArrayList<ArrayList<Integer>> getListByFlower(JSONArray player) {

		ArrayList<ArrayList<Integer>> set = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> set1 = new ArrayList<Integer>();
		ArrayList<Integer> set2 = new ArrayList<Integer>();
		ArrayList<Integer> set3 = new ArrayList<Integer>();
		ArrayList<Integer> set4 = new ArrayList<Integer>();

		if (player != null) {
			for (Object temp : player) {
				if (temp != null) {
					String string = String.valueOf(temp);
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

	public static Integer getKingPaiCount(JSONArray player) {
		int count = 0;
		if (player == null || player.size() == 0) {
			return count;
		}
		for (Object temp : player) {
			if (null != temp) {
				String s = String.valueOf(temp);
				if ("5".equals(s.split("-")[0])) {
					count++;
				}
			}
		}
		return count;
	}

	public static ArrayList<ArrayList<Integer>> getListByNum(JSONArray player) {
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

		Iterator<Object> it = player.iterator();

		while (it.hasNext()) {
			String string = (String) it.next();
			// 如果是癞子则直接跳过
			if ("5".equals(string.split("-")[0]))
				continue;
			if ("1".equals(string.split("-")[1])) {
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

	public static TreeSet<Integer> sortByNum(JSONArray player) {
		TreeSet<Integer> p = new TreeSet<Integer>();
		Iterator<Object> it = player.iterator();
		while (it.hasNext()) {
			String string = (String) it.next();
			// 如果是癞子则跳过
			if (Integer.parseInt(string.split("-")[0]) == 5)
				continue;
			p.add(Integer.parseInt(string.split("-")[1]));
		}
		return p;
	}

	public static List<Integer> sortByNumList(JSONArray player) {
		List<Integer> p = new JSONArray();
		Iterator<Object> it = player.iterator();
		while (it.hasNext()) {
			String string = (String) it.next();
			// 如果是癞子则 当A 14
			if (Integer.parseInt(string.split("-")[0]) == 5) {
				p.add(14);
			} else {
				int pai = Integer.parseInt(string.split("-")[1]);
				if (1 == pai) {
					p.add(14);
				} else {
					p.add(pai);
				}

			}
		}
		Collections.sort(p);
		return p;
	}

	public static int isOne(JSONArray player) {
		// 有癞子牌必有对子
		if (getKingPaiCount(player) > 0)
			return one;
		// 无癞子牌处理
		ArrayList<ArrayList<Integer>> set = getListByNum(player);
		int i = 0;
		for (ArrayList<Integer> temp : set) {
			if (temp.size() == 2) {
				i++;
			}
		}
		if (i == 1) {
			return one;
		} else {
			return zero;
		}
	}

	public static int isTwo(JSONArray player) {
		if (player.size() == 3) {
			return none;
		}
		ArrayList<ArrayList<Integer>> set = getListByNum(player);
		// 获取鬼牌数量
		int kingCount = getKingPaiCount(player);
		int i = 0;
		for (ArrayList<Integer> temp : set) {
			if (temp.size() + kingCount == 2) {
				i++;
				kingCount = 2 - temp.size();
			}
		}
		if (i == 2 && kingCount >= 0) {
			return two;
		} else {
			return none;
		}
	}

	public static int isThreeHasKing(JSONArray player,int ruanguiCount) {
		// 获取鬼牌
		int kingCount = getKingPaiCount(player);
		ArrayList<ArrayList<Integer>> set = getListByNum(player);
		int i = 0;
		
		int k =0 ;
		for (int j = 0; j < set.size(); j++) {
			if (set.get(j).size()+kingCount ==3 ) {
				k=j+1;
				i++;
			}
		}
		if (i >= 1) {
			if (k>=2 && k<=ruanguiCount+1) {
				return bomb;
			}
			return three;
		} else {
			return none;
		}
	}

	public static int isThree(JSONArray player,int ruanguiCount) {
		// 如果含有癞子牌处理
		if (getKingPaiCount(player) > 0)
			return isThreeHasKing(player,ruanguiCount);
		// 无癞子牌处理
		ArrayList<ArrayList<Integer>> set = getListByNum(player);
		int i = 0;
		int k = 0;
		for (int j = 0; j < set.size(); j++) {
			if (set.get(j).size() == 3) {
				k=j+1;
				i++;
			}
		}
		if (i == 1) {
			if (k>=2&&k<=ruanguiCount+1) {
				return bomb;
			}
			return three;
		} else {
			return none;
		}
	}

	public static int isFlush(JSONArray player) {
		if (player.size() == 3) {
			return none;
		}
		// 获得鬼牌数量
		int kingCount = getKingPaiCount(player);
		// 含有癞子判断
		if (kingCount > 0)
			return isFlushHasKing(player);

		// 无癞子判断
		TreeSet<Integer> treeSet = new TreeSet<>();
		Iterator<Object> it = player.iterator();
		while (it.hasNext()) {
			String string = (String) it.next();
			if ("5".equals(string.split("-")[0]))
				continue;
			treeSet.add(Integer.parseInt(string.split("-")[1]));
		}
		if (treeSet.size() == 5) {
			if (treeSet.contains(1) && treeSet.contains(13)) {
				if (treeSet.contains(10) && treeSet.contains(11) && treeSet.contains(12)) {
					return flush;
				} else {
					return none;
				}
			} else {
				if (treeSet.first() + 4 == treeSet.last()) {
					return flush;
				} else {
					return none;
				}
			}
		}
		return none;
	}

	private static int isFlushHasKing(JSONArray player) {
		int kingCount = getKingPaiCount(player);
		TreeSet<Integer> treeSet = sortByNum(player);
		if (treeSet.size() + kingCount == 5) {
			ArrayList<Integer> temp = new ArrayList<>(treeSet);
			// 从哪里开始遍历
			int j = 0;
			// 若牌中有1时,则从第二个开始遍历 特殊:1 10 11 12 13
			if (temp.get(0).equals(1))
				j = 1;
			for (int i = j; i < temp.size() - 1; i++) {
				// 若牌中有牌相同直接返回
				if (temp.get(i + 1) - temp.get(i) == 0) {
					return none;
				}
				// 若牌里有A但其他牌不在指定范围内也直接返回(无法组成顺子)
				if (temp.get(0).equals(1)) {
					if (temp.get(i) < 10 && temp.get(i) > 5) {
						return none;
					}
					if (temp.get(i + 1) < 10 && temp.get(i + 1) > 5) {
						return none;
					}
				}
				// 所需要的鬼牌
				int t;
				// 若相差过多则使用鬼牌
				if ((t = temp.get(i + 1) - temp.get(i) - 1) != 0) {
					kingCount = kingCount - t;
				}
			}
			// 若使用鬼牌的数量比持有鬼牌数量多的话,则kingCount为负数
			if (kingCount >= 0) {
				return flush;
			}
		}
		return none;
	}

	public static int isSameFlower(JSONArray player) {
		if (player.size() == 3) {
			return none;
		}
		// 含有癞子牌判断
		if (getKingPaiCount(player) > 0)
			return isSameFlowerHasKing(player);
		// 无癞子牌判断
		ArrayList<ArrayList<Integer>> set = getListByFlower(player);
		int i = 0;
		for (ArrayList<Integer> temp : set) {
			if (temp.size() == 5) {
				i++;
			}
		}
		if (i == 1) {
			return sameFlower;
		} else {
			return none;
		}
	}

	public static int isSameFlowerHasKing(JSONArray player) {
		// 获取鬼牌的数量
		int kingCount = getKingPaiCount(player);
		ArrayList<ArrayList<Integer>> set = getListByFlower(player);
		int i = 0;
		for (ArrayList<Integer> temp : set) {
			if (temp.size() + kingCount == 5) {
				i++;
			}
		}
		if (i == 1) {
			return sameFlower;
		} else {
			return none;
		}
	}

	public static int isGourd(JSONArray player) {
		if (player.size() == 3) {
			return none;
		}
		// 如果牌含有癞子判断
		if (getKingPaiCount(player) > 0)
			return isGourdHasKing(player);
		// 如果牌不含癞子判断
		ArrayList<ArrayList<Integer>> set = getListByNum(player);
		int i = 0;
		for (ArrayList<Integer> temp : set) {
			if (temp.size() == 2) {
				i++;
			}
			if (temp.size() == 3) {
				i += 2;
			}
		}
		if (i == 3) {
			return gourd;
		} else {
			return none;
		}
	}

	private static int isGourdHasKing(JSONArray player) {
		// 获取鬼牌的数量
		int kingCount = getKingPaiCount(player);
		ArrayList<ArrayList<Integer>> set = getListByNum(player);
		int i = 0;
		for (ArrayList<Integer> temp : set) {
			if (temp.size() + kingCount == 2) {
				i++;
				kingCount = kingCount - (2 - temp.size());
			}
			if (temp.size() + kingCount == 3) {
				i += 2;
				kingCount = kingCount - (3 - temp.size());
			}
		}
		if (i == 3 && kingCount >= 0) {
			return gourd;
		} else {
			return none;
		}
	}

	public static int isBomb(JSONArray player, int ruanguiCount) {
		if (player.size() == 3) {
			return none;
		}
		// 牌含有癞子时判断
		if (getKingPaiCount(player) > 0)
			return isBombHasKing(player, ruanguiCount);
		// 没有癞子时判断
		ArrayList<ArrayList<Integer>> set = getListByNum(player);
		int i = 0;
		for (int j = 0; j < set.size(); j++) {
			if (set.get(j).size() >= 4 || (set.get(j).size() >= 3 && j >= 1 && j <= ruanguiCount)) {
				i++;
				break;
			}
		}
		if (i == 1) {
			return bomb;
		} else {
			return none;
		}
	}

	private static int isBombHasKing(JSONArray player, int ruanguiCount) {
		ArrayList<ArrayList<Integer>> set = getListByNum(player);
		int i = 0;
		// 获取鬼牌的数量
		int kingCount = getKingPaiCount(player);
		for (int j = 0; j < set.size(); j++) {
			if (set.get(j).size() + kingCount >= 4
					|| (set.get(j).size() + kingCount >= 3 && j >= 1 && j <= ruanguiCount)) {
				i++;
				break;
			}
		}

		if (i >= 1) {
			return bomb;
		} else {
			return none;
		}
	}

	public static int isFlushByFlower(JSONArray player) {
		if (player.size() == 3) {
			return none;
		}
		// 有癞子牌判断
		if (getKingPaiCount(player) > 0)
			return isFlushByFlowerHasKing(player);
		// 无癞子牌判断
		// 根据花色进行分类
		ArrayList<ArrayList<Integer>> set = getListByFlower(player);
		for (ArrayList<Integer> temp : set) {
			if (temp.size() == 5) {
				if (temp.get(0) == 1 && temp.get(4) == 13) {
					if (temp.get(1) + 1 == temp.get(2) && temp.get(2) + 1 == temp.get(3)
							&& temp.get(3) + 1 == temp.get(4)) {
						return flushByFlower;
					}
				} else {
					if (temp.get(0) + 1 == temp.get(1) && temp.get(1) + 1 == temp.get(2)
							&& temp.get(2) + 1 == temp.get(3) && temp.get(3) + 1 == temp.get(4)) {
						return flushByFlower;
					}
				}
			}
		}
		return none;
	}

	private static int isFlushByFlowerHasKing(JSONArray player) {
		// 根据花色进行分类
		ArrayList<ArrayList<Integer>> set = getListByFlower(player);
		int t = 0;
		for (ArrayList<Integer> integers : set) {
			if (integers.size() > 0) {
				t++;
			}
		}
		// 如果牌不是同一花色则直接return
		if (t != 1) {
			return none;
		}
		// 判断牌是否是顺子
		if (isFlushHasKing(player) > 0) {
			return flushByFlower;
		}
		return none;
//        for (ArrayList<Integer> temp : set) {
//            //癞子玩法
//            if (temp.size() + kingCount == 5) {
//                if (kingCount == 4) {
//                    return flushByFlower;
//                } else if (temp.size() > 0) {
//                    //从哪里开始遍历
//                    int j = 0;
//                    //若牌中有1时,则从第二个开始遍历 特殊:1 10 11 12 13
//                    if (temp.get(0).equals(1)) j = 1;
//                    for (int i = j; i < temp.size() - 1; i++) {
//                        //若牌中有牌相同直接返回 或者有1 但其他牌不在指定范围内也直接返回(无法组成顺子)
//                        if (temp.get(i + 1) - temp.get(i) == 0 || (temp.get(0).equals(1) && temp.get(i + 1) < 10 && temp.get(i + 1) > 5))
//                            return none;
//                        //需要的鬼牌
//                        int t;
//                        //若相差过多则使用鬼牌
//                        if ((t = temp.get(i + 1) - temp.get(i) - 1) != 0) {
//                            kingCount = kingCount - t;
//                        }
//                    }
//                    //若使用鬼牌的数量比持有鬼牌数量多的话,则kingCount为负数
//                    if (kingCount >= 0) {
//                        return flushByFlower;
//                    }
//                }
//            }
//        }
//        return none;
	}

	public static int isBoth(JSONArray player, int ruanguiCount) {
		if (player.size() == 3) {
			return none;
		}
		// 有癞子牌判断
		if (getKingPaiCount(player) > 0)
			return isBothHasKing(player, ruanguiCount);
		// 无癞子牌判断
		// 根据牌数字排序
		ArrayList<ArrayList<Integer>> set = getListByNum(player);
		int i = 0;
		for (int j = 0; j < set.size(); j++) {
			if (set.get(j).size() >= 5 || (set.get(j).size() >= 4 && j >= 1 && j <= ruanguiCount)) {
				i++;
				break;
			}
		}
		if (i == 1) {
			return both;
		} else {
			return none;
		}
	}

	public static int isLiutong(JSONArray player, int ruanguiCount) {
		if (player.size() == 3) {
			return none;
		}
		// 有癞子牌判断
		if (getKingPaiCount(player) > 0)
			return isLiuTongHasKing(player, ruanguiCount);
		// 无癞子牌判断
		// 根据牌数字排序
		ArrayList<ArrayList<Integer>> set = getListByNum(player);
		int i = 0;
		for (int j = 0; j < set.size(); j++) {
			if (set.get(j).size()==5 && j >= 1 && j <= ruanguiCount) {
				i++;
			}
		}

		
		if (i == 1) {
			return six;
		} else {
			return none;
		}
	}

	// ["5-1","5-1","5-0","3-5","2-5"]
	private static int isBothHasKing(JSONArray player, int ruanguiCount) {
		// 根据牌数字排序
		ArrayList<ArrayList<Integer>> set = getListByNum(player);
		// 获取鬼牌数量
		int kingCount = getKingPaiCount(player);
		// 如果鬼牌数量是五个直接是五同
		if (kingCount == 5) {
			return both;
		}
		int i = 0;
		for (int j = 0; j < set.size(); j++) {
			if (set.get(j).size() + kingCount >= 5
					|| (set.get(j).size() + kingCount >= 4 && j >= 1 && j <= ruanguiCount)) {
				i++;
				break;
			}
		}

		if (i == 1) {
			return both;
		} else {
			return none;
		}
	}

	private static int isLiuTongHasKing(JSONArray player, int ruanguiCount) {
		// 根据牌数字排序
		ArrayList<ArrayList<Integer>> set = getListByNum(player);
		// 获取鬼牌数量
		int kingCount = getKingPaiCount(player);
		// 如果鬼牌数量是5个直接是6同
		if (kingCount ==5) {
			return six;
		}
		int i = 0;
		for (int j = 0; j < set.size(); j++) {
			if (set.get(j).size() + kingCount == 5 && j >= 1 && j <= ruanguiCount) {
				i++;
			}
		}

		if (i == 1) {
			return six;
		} else {
			return none;
		}
	}

	public static int compareLiuTong(JSONArray player1, JSONArray player2, int ruanguiCount) {
		// 有癞子时对比五同
		if (getKingPaiCount(player1) > 0 || getKingPaiCount(player2) > 0) {
			return compareLiuTongHasKing(player1, player2, ruanguiCount);
		}
		ArrayList<ArrayList<Integer>> p1 = getListByNum(player1);
		ArrayList<ArrayList<Integer>> p2 = getListByNum(player2);
		int t1 = 0;
		int t2 = 0;
		for (int i = 0; i < p1.size(); i++) {
			if (p1.get(i).size() == 5) {
				t1 = i + 1;
				break;
			}
		}
		for (int i = 0; i < p2.size(); i++) {
			if (p2.get(i).size() == 5) {
				t2 = i + 1;
				break;
			}
		}
		if (t1 > t2) {
			if (t2 == 1) {
				return -1;
			}
			return 1;
		} else if (t1 < t2) {
			if (t1 == 1) {
				return 1;
			}
			return -1;
		} else {
			return 0;
		}
	}

	public static int compareBoth(JSONArray player1, JSONArray player2, int ruanguiCount) {
		// 有癞子时对比五同
		if (getKingPaiCount(player1) > 0 || getKingPaiCount(player2) > 0) {
			return compareBothHasKing(player1, player2, ruanguiCount);
		}
		ArrayList<ArrayList<Integer>> p1 = getListByNum(player1);
		ArrayList<ArrayList<Integer>> p2 = getListByNum(player2);
		int t1 = 0;
		int t2 = 0;
		for (int i = 0; i < p1.size(); i++) {
			if (p1.get(i).size() == 5 || (p1.get(i).size() == 4) && i >= 1 && i <= ruanguiCount) {
				t1 = i + 1;
				break;
			}
		}
		for (int i = 0; i < p2.size(); i++) {
			if (p2.get(i).size() == 5 || (p2.get(i).size() == 4) && i >= 1 && i <= ruanguiCount) {
				t2 = i + 1;
				break;
			}
		}
		if (t1 > t2) {
			if (t2 == 1) {
				return -1;
			}
			return 1;
		} else if (t1 < t2) {
			if (t1 == 1) {
				return 1;
			}
			return -1;
		} else {
			// t1 == t2 需判断是不是软鬼5同 如果是 需要比较剩下的单张
			if (t1 >= 2 && t1 <= ruanguiCount + 1) {
				int t1one = 0;
				int t2one = 0;
				for (int i = 0; i < p1.size(); i++) {
					if (p1.get(i).size() == 1) {
						t1one = i + 1;
						break;
					}
				}
				for (int i = 0; i < p2.size(); i++) {
					if (p2.get(i).size() == 1) {
						t2one = i + 1;
						break;
					}
				}
				if (t1one > t2one) {
					if (t2one == 1) {
						return -1;
					}
					return 1;
				} else if (t1one < t2one) {
					if (t1one == 1) {
						return 1;
					}
					return -1;
				} else {
					return 0;
				}
			} else {
				return 0;
			}
		}
	}

	private static int compareLiuTongHasKing(JSONArray player1, JSONArray player2, int ruanguiCount) {
		ArrayList<ArrayList<Integer>> p1 = getListByNum(player1);
		ArrayList<ArrayList<Integer>> p2 = getListByNum(player2);
		int t1 = 0;
		int t2 = 0;
		// 获取鬼牌数量
		int kingCount1 = getKingPaiCount(player1);
		int kingCount2 = getKingPaiCount(player2);
		for (int i = 0; i < p1.size(); i++) {
			if (p1.get(i).size() + kingCount1 == 5) {
				t1 = i + 1;
				break;
			}
		}
		for (int i = 0; i < p2.size(); i++) {
			if (p2.get(i).size() + kingCount2 == 5) {
				t2 = i + 1;
				break;
			}
		}
		if (t1 > t2) {
			if (t2 == 1) {
				return -1;
			}
			return 1;
		} else if (t1 < t2) {
			if (t1 == 1) {
				return 1;
			}
			return -1;
		} else {
			return 0;
		}
	}

	private static int compareBothHasKing(JSONArray player1, JSONArray player2, int ruanguiCount) {
		ArrayList<ArrayList<Integer>> p1 = getListByNum(player1);
		ArrayList<ArrayList<Integer>> p2 = getListByNum(player2);
		int t1 = 0;
		int t2 = 0;
		// 获取鬼牌数量
		int kingCount1 = getKingPaiCount(player1);
		int kingCount2 = getKingPaiCount(player2);
		for (int i = 0; i < p1.size(); i++) {
			if (p1.get(i).size() + kingCount1 == 5
					|| (p1.get(i).size() + kingCount1 == 4 && i >= 1 && i <= ruanguiCount)) {
				t1 = i + 1;
				break;
			}
		}
		for (int i = 0; i < p2.size(); i++) {
			if (p2.get(i).size() + kingCount2 == 5
					|| (p2.get(i).size() + kingCount2 == 4 && i >= 1 && i <= ruanguiCount)) {
				t2 = i + 1;
				break;
			}
		}
		if (t1 > t2) {
			if (t2 == 1) {
				return -1;
			}
			return 1;
		} else if (t1 < t2) {
			if (t1 == 1) {
				return 1;
			}
			return -1;
		} else {
			// t1 == t2 需判断是不是软鬼5同 如果是 需要比较剩下的单张
			if (t1 >= 2 && t1 <= ruanguiCount + 1) {
				int t1one = 0;
				int t2one = 0;
				for (int i = 0; i < p1.size(); i++) {
					if (p1.get(i).size() == 1) {
						t1one = i + 1;
						break;
					}
				}
				for (int i = 0; i < p2.size(); i++) {
					if (p2.get(i).size() == 1) {
						t2one = i + 1;
						break;
					}
				}
				if (t1one > t2one) {
					if (t2one == 1) {
						return -1;
					}
					return 1;
				} else if (t1one < t2one) {
					if (t1one == 1) {
						return 1;
					}
					return -1;
				} else {
					return 0;
				}
			} else {
				return 0;
			}

		}
	}

	public static int compareFlushByflower(JSONArray player1, JSONArray player2) {
		// 有癞子时对比同花顺 直接比顺子
		if (getKingPaiCount(player1) > 0 || getKingPaiCount(player2) > 0) {
			return compareFlushHasKing(player1, player2);
		}

		TreeSet<Integer> p1 = sortByNum(player1);
		TreeSet<Integer> p2 = sortByNum(player2);
		if (p1.contains(1)) {
			if (p2.contains(1)) {
				if (p1.last() > p2.last()) {
					return 1;
				} else if (p1.last() < p2.last()) {
					return -1;
				}
			} else {
				return 1;
			}
		} else {
			if (p2.contains(1)) {
				return -1;
			} else {
				if (p1.last() > p2.last()) {
					return 1;
				} else if (p1.last() < p2.last()) {
					return -1;
				}
			}
		}
		return 0;
	}

	public static int compareBomb(JSONArray player1, JSONArray player2, int ruanguiCount) {
		// 有癞子时对比铁支
		if (getKingPaiCount(player1) > 0 || getKingPaiCount(player2) > 0)
			return compareBombHasKing(player1, player2, ruanguiCount);

		ArrayList<ArrayList<Integer>> p1 = getListByNum(player1);
		ArrayList<ArrayList<Integer>> p2 = getListByNum(player2);
		int t1 = 0;
		int t2 = 0;
		int s1 = 0;
		int s2 = 0;
		int ss1 =0;
		int ss2 =0;
		for (int i = 0; i < p1.size(); i++) {
			if (p1.get(i).size() == 4 || p1.get(i).size() == 3 && i >= 1 && i <= ruanguiCount) {
				t1 = i + 1;
			}
			
		}
		int k=0;
		for (int i = 0; i < p1.size(); i++) {
			if (i == t1 - 1 || p1.get(i).size()==0) {
				continue;
			}
			if (p1.get(i).size()==2) {
				s1 = i+1;
				ss1 = i+1;
				break;
			}
			if (p1.get(i).size()==1) {
				
				if (k==0) {
					ss1 = i+1;
				}else {
					s1 = i+1;
				}
				k++;
			}

		}

		for (int i = 0; i < p2.size(); i++) {
			if (p2.get(i).size() == 4 || p2.get(i).size() == 3 && i >= 1 && i <= ruanguiCount) {
				t2 = i + 1;
			}
			
		}
		int kk=0;
		for (int i = 0; i < p2.size(); i++) {
			if (i == t2 - 1 || p2.get(i).size()==0) {
				continue;
			}
			if (p2.get(i).size()==2) {
				s2 = i+1;
				ss2 = i+1;
				break;
			}
			if (p2.get(i).size()==1) {
				
				if (kk==0) {
					ss2 = i+1;
				}else {
					s2 = i+1;
				}
				kk++;
			}

		}
		if (t1 > t2) {
			if (t2 == 1) {
				return -1;
			} else {
				return 1;
			}

		} else if (t1 < t2) {
			if (t1 == 1) {
				return 1;
			} else {
				return -1;
			}
		} else {
			if (ss1==1 && ss2!=1) {
				return 1;
			}
			if (ss1!=1 && ss2==1) {
				return -1;
			}
			
			if (s1 > s2) {
				if (s2 == 1) {
					return -1;
				}
				return 1;
			} else if (s1 == s2) {
				//比较ss1 和 ss2
				if (ss1>ss2) {
					return 1;
				}else if (ss1<ss2) {
					return -1;
				}else {
					return 0;
				}
			} else {
				if (s1 == 1) {
					return 1;
				}
				return -1;
			}
		}
	}

	private static int compareBombHasKing(JSONArray player1, JSONArray player2, int ruanguiCount) {
		ArrayList<ArrayList<Integer>> p1 = getListByNum(player1);
		ArrayList<ArrayList<Integer>> p2 = getListByNum(player2);

		int t1 = 0;
		int t2 = 0;
		int s1 = 0;
		int s2 = 0;
		int ss1 = 0;
		int ss2 = 0;
		// 获取鬼牌数量
		int kingCount1 = getKingPaiCount(player1);
		int kingCount2 = getKingPaiCount(player2);
		if (kingCount1 == 3) {
			t1 = getMaxCardNum(player1);
			s1 = getMinCardNum(player1);
		} else {
			for (int i = 0; i < p1.size(); i++) {
				// 取能组成最大的铁支
				if (p1.get(i).size() + kingCount1 == 4
						|| (p1.get(i).size() + kingCount1 == 3 && i >= 1 && i <= ruanguiCount) && t1 != 1) {
					t1 = i + 1;
					continue;
				}
			}
			int k=0;
			for (int i = 0; i < p1.size(); i++) {
				if (i == t1 - 1 || p1.get(i).size()==0) {
					continue;
				}
				if (p1.get(i).size()==2) {
					s1 = i+1;
					ss1 = i+1;
					break;
				}
				if (p1.get(i).size()==1) {
					
					if (k==0) {
						ss1 = i+1;
					}else {
						s1 = i+1;
					}
					k++;
				}

			}

		}
		if (kingCount2 == 3) {
			t2 = getMaxCardNum(player2);
			s2 = getMinCardNum(player2);
		} else {
			for (int i = 0; i < p2.size(); i++) {
				// 取最大三条,若以取到1则不需要在取了
				if (p2.get(i).size() + kingCount2 == 4
						|| (p2.get(i).size() + kingCount2 == 3 && i >= 1 && i <= ruanguiCount) && t2 != 1) {
					t2 = i + 1;
					continue;
				}
				
			}
			int k=0;
			for (int i = 0; i < p2.size(); i++) {
				if (i == t2 - 1 || p2.get(i).size()==0) {
					continue;
				}
				if (p2.get(i).size()==2) {
					s2 = i+1;
					ss2 = i+1;
					break;
				}
				if (p2.get(i).size()==1) {
					
					if (k==0) {
						ss2 = i+1;
					}else {
						s2 = i+1;
					}
					k++;
				}

			}
			
		}
		if (t1 > t2) {
			if (t2 == 1) {
				return -1;
			} else {
				return 1;
			}

		} else if (t1 < t2) {
			if (t1 == 1) {
				return 1;
			} else {
				return -1;
			}
		} else {
			
			if (ss1==1 && ss2!=1) {
				return 1;
			}
			if (ss1!=1 && ss2==1) {
				return -1;
			}
			
			if (s1 > s2) {
				if (s2 == 1) {
					return -1;
				}
				return 1;
			} else if (s1 == s2) {
				//比较ss1 和 ss2
				if (ss1>ss2) {
					return 1;
				}else if (ss1<ss2) {
					return -1;
				}else {
					return 0;
				}
			} else {
				if (s1 == 1) {
					return 1;
				}
				return -1;
			}
		}
	}

	public static int compareGourd(JSONArray player1, JSONArray player2) {
		// 有癞子时对比葫芦
		if (getKingPaiCount(player1) > 0 || getKingPaiCount(player2) > 0)
			return compareGourdHasKing(player1, player2);
		ArrayList<ArrayList<Integer>> p1 = getListByNum(player1);
		ArrayList<ArrayList<Integer>> p2 = getListByNum(player2);
		int t1 = 0;
		int t2 = 0;
		int s1 = 0;
		int s2 = 0;
		for (int i = 0; i < p1.size(); i++) {
			if (p1.get(i).size() == 3) {
				t1 = i + 1;
			}
			if (p1.get(i).size() == 2) {
				s1 = i + 1;
			}
			if (t1 > 0 && s1 > 0) {
				break;
			}
		}
		for (int i = 0; i < p2.size(); i++) {
			if (p2.get(i).size() == 3) {
				t2 = i + 1;
			}
			if (p2.get(i).size() == 2) {
				s2 = i + 1;
			}
			if (t2 > 0 && s2 > 0) {
				break;
			}
		}
		if (t1 > t2) {
			if (t2 == 1) {
				return -1;
			}
			return 1;
		} else if (t1 < t2) {
			if (t1 == 1) {
				return 1;
			}
			return -1;
		} else {
			if (s1 > s2) {
				if (s2 == 1) {
					return -1;
				}
				return 1;
			} else if (s1 < s2) {
				if (s1 == 1) {
					return 1;
				}
				return -1;
			} else {
				return 0;
			}
		}
	}

	private static int compareGourdHasKing(JSONArray player1, JSONArray player2) {
		ArrayList<ArrayList<Integer>> p1 = getListByNum(player1);
		ArrayList<ArrayList<Integer>> p2 = getListByNum(player2);
		// 获得鬼牌
		int kingCount1 = getKingPaiCount(player1);
		int kingCount2 = getKingPaiCount(player2);
		int t1 = 0;
		int t2 = 0;
		int s1 = 0;
		int s2 = 0;
		if (kingCount1 > 0) {
			t1 = getMaxCardNum(player1);
			s1 = getMinCardNum(player1);
		} else {
			for (int i = 0; i < p1.size(); i++) {
				if (p1.get(i).size() == 3) {
					t1 = i + 1;
				}
				if (p1.get(i).size() == 2) {
					s1 = i + 1;
				}
			}
		}
		if (kingCount2 > 0) {
			t2 = getMaxCardNum(player2);
			s2 = getMinCardNum(player2);
		} else {
			for (int i = 0; i < p2.size(); i++) {
				if (p2.get(i).size() == 3) {
					t2 = i + 1;
				}
				if (p2.get(i).size() == 2) {
					s2 = i + 1;
				}
			}
		}
		if (t1 > t2) {
			if (t2 == 1) {
				return -1;
			}
			return 1;
		} else if (t1 < t2) {
			if (t1 == 1) {
				return 1;
			}
			return -1;
		} else {
			if (s1 > s2) {
				if (s2 == 1) {
					return -1;
				}
				return 1;
			} else if (s1 < s2) {
				if (s1 == 1) {
					return 1;
				}
				return -1;
			} else {
				return 0;
			}
		}
	}

	private static int getMaxCardNum(JSONArray player1) {
		boolean hasA = isHasA(player1);
		if (hasA) {
			return 1;
		}
		int maxNum = 0;
		for (int i = 0; i < player1.size(); i++) {
			if ("5".equals(player1.getString(i).split("-")[0])) {
				continue;
			}
			int cardNum = Integer.valueOf(player1.getString(i).split("-")[1]);
			if (maxNum < cardNum) {
				maxNum = cardNum;
			}
		}
		return maxNum;
	}

	private static int getMinCardNum(JSONArray player1) {
		int minNum = 100;
		for (int i = 0; i < player1.size(); i++) {
			if ("5".equals(player1.getString(i).split("-")[0]) || "1".equals(player1.getString(i).split("-")[1])) {
				continue;
			}
			int cardNum = Integer.valueOf(player1.getString(i).split("-")[1]);
			if (minNum > cardNum) {
				minNum = cardNum;
			}
		}
		return minNum;
	}

	private static boolean isHasA(JSONArray cards) {
		boolean isHasA = false;
		// 是否牌中都是鬼牌,则表示为包含A
		boolean isHasAllKing = true;
		for (int i = 0; i < cards.size(); i++) {
			String temp = cards.getString(i);
			if (!"5".equals(temp.split("-")[0])) {
				isHasAllKing = false;
				if ("1".equals(temp.split("-")[1])) {
					isHasA = true;
					break;
				}
			}
		}
		return isHasAllKing || isHasA;
	}

	public static int compareSameFlower(JSONArray player1, JSONArray player2) {
//        return compareSameFlowerSet(player1, player2);   //有对子以上进行对比
		return compareSameFlowerList(player1, player2);
	}

	public static int compareSameFlowerList(JSONArray player1, JSONArray player2) {

		List<Integer> p1 = sortByNumList(player1);
		List<Integer> p2 = sortByNumList(player2);

		for (int i = p1.size() - 1; i > -1; i--) {
			if (p1.get(i) > p2.get(i)) {
				return 1;
			} else if (p1.get(i) < p2.get(i)) {
				return -1;
			}
		}
		return 0;
	}

	public static int compareSameFlowerSet(JSONArray player1, JSONArray player2) {
		// 有癞子牌对比同花
		if (getKingPaiCount(player1) > 0 || getKingPaiCount(player2) > 0)
			return compareSameFlowerHasKing(player1, player2);
		// 无癞子牌对比同花
		TreeSet<Integer> p1 = sortByNum(player1);
		TreeSet<Integer> p2 = sortByNum(player2);
		if (p1.size() < p2.size()) {
			return 1;
		} else if (p1.size() > p2.size()) {
			return -1;
		} else {
			if (p1.size() == 3 && p2.size() == 3) {
				// ============加入同花三条判断 wqm 20181025 start==================
				ArrayList<ArrayList<Integer>> p11 = getListByNum(player1);
				ArrayList<ArrayList<Integer>> p22 = getListByNum(player2);
				boolean containsThree1 = false;
				boolean containsThree2 = false;
				for (int i = 0; i < p11.size(); i++) {
					if (p11.get(i).size() == 3) {
						containsThree1 = true;
					}
				}
				for (int i = 0; i < p11.size(); i++) {
					if (p22.get(i).size() == 3) {
						containsThree2 = true;
					}
				}
				if (containsThree1 && containsThree2) {
					return compareThree(player1, player2);
				} else if (containsThree1 && !containsThree2) {
					return 1;
				} else if (!containsThree1 && containsThree2) {
					return -1;
				}
				// ============================end=============================
				return compareTwo(player1, player2);
			} else if (p1.size() == 4 && p2.size() == 4) {
				return compareOne(player1, player2);
			} else {
				if (p1.contains(1)) {
					if (p2.contains(1)) {
						/*
						 * for(int i=0;i<p1.size();i++){ if(p1.last()>p2.last()){ return 1; }else
						 * if(p1.last()<p2.last()){ return -1; }else { p1.remove(p1.last());
						 * p2.remove(p2.last()); } }
						 */
						for (int i = p1.size() - 1; i > -1; i--) {
							if (p1.last() > p2.last()) {
								return 1;
							} else if (p1.last() < p2.last()) {
								return -1;
							} else {
								p1.remove(p1.last());
								p2.remove(p2.last());
							}
						}
					} else {
						return 1;
					}
				} else {
					if (p2.contains(1)) {
						return -1;
					} else {
						for (int i = 0; i < p1.size(); i++) {
							if (p1.last() > p2.last()) {
								return 1;
							} else if (p1.last() < p2.last()) {
								return -1;
							} else {
								p1.remove(p1.last());
								p2.remove(p2.last());
								i--;
							}
						}
					}
				}
			}

		}
		return 0;
	}

	private static int compareSameFlowerHasKing(JSONArray player1, JSONArray player2) {
		TreeSet<Integer> p1 = sortByNum(player1);
		TreeSet<Integer> p2 = sortByNum(player2);
		// 获得鬼牌
		int kingCount1 = getKingPaiCount(player1);
		int kingCount2 = getKingPaiCount(player2);
		if (p1.size() + kingCount1 < p2.size() + kingCount2) {
			return 1;
		} else if (p1.size() + kingCount1 > p2.size() + kingCount2) {
			return -1;
		} else {
			if (p1.size() == 3 && p2.size() == 3) {
				// ============加入同花三条判断 wqm 20181025 start==================
				ArrayList<ArrayList<Integer>> p11 = getListByNum(player1);
				ArrayList<ArrayList<Integer>> p22 = getListByNum(player2);
				boolean containsThree1 = false;
				boolean containsThree2 = false;
				for (int i = 0; i < p11.size(); i++) {
					if (p11.get(i).size() + kingCount1 == 3) {
						containsThree1 = true;
					}
				}
				for (int i = 0; i < p11.size(); i++) {
					if (p22.get(i).size() + kingCount2 == 3) {
						containsThree2 = true;
					}
				}
				if (containsThree1 && containsThree2) {
					return compareThree(player1, player2);
				} else if (containsThree1 && !containsThree2) {
					return 1;
				} else if (!containsThree1 && containsThree2) {
					return -1;
				}
				// ============================end=============================
				return compareTwo(player1, player2);
			} else if (p1.size() + kingCount1 == 4 && p2.size() + kingCount2 == 4) {
				return compareOne(player1, player2);
			}
			// =========有癞子必有对子同花,因此无需把每个牌进行对比================
		}
		return 0;
	}

	public static int compareFlush(JSONArray player1, JSONArray player2) {
		if (getKingPaiCount(player1) > 0 || getKingPaiCount(player2) > 0)
			return compareFlushHasKing(player1, player2);
		TreeSet<Integer> p1 = sortByNum(player1);
		TreeSet<Integer> p2 = sortByNum(player2);

		if (p1.contains(1)) {
			if (p2.contains(1)) {
				if (p1.last() > p2.last()) {
					return 1;
				} else if (p1.last() < p2.last()) {
					return -1;
				}
			} else {
				return 1;
			}
		} else {
			if (p2.contains(1)) {
				return -1;
			} else {
				if (p1.last() > p2.last()) {
					return 1;
				} else if (p1.last() < p2.last()) {
					return -1;
				}
			}
		}
		return 0;
	}

	private static int compareFlushHasKing(JSONArray player1, JSONArray player2) {
		TreeSet<Integer> p1 = sortByNum(player1);
		TreeSet<Integer> p2 = sortByNum(player2);
		// 获取鬼牌数量
		int kingCount1 = getKingPaiCount(player1);
		int kingCount2 = getKingPaiCount(player2);
		List<Integer> list1 = new ArrayList<>(p1);
		List<Integer> list2 = new ArrayList<>(p2);
		TreeSet<Integer> s1 = new TreeSet<>();
		TreeSet<Integer> s2 = new TreeSet<>();
		// 表示所需要的鬼牌
		int temp;
		if (kingCount1 > 0) {
			s1.add(list1.get(0));
			// 将有癞子的牌转换成实际牌
			for (int i = 0; i < list1.size() - 1; i++) {
				if ((temp = list1.get(i + 1) - list1.get(i) - 1) != 0 && temp <= kingCount1) {
					kingCount1 = kingCount1 - temp;
					for (int j = 0; j < temp; j++) {
						s1.add(list1.get(i) + 1 + j);
					}
				}
				s1.add(list1.get(i + 1));
			}
			// 多余鬼牌处理
			if (kingCount1 > 0) {
				for (int i = 0; i < kingCount1; i++) {
					// 取出牌最后的元素
					int last = s1.last();
					if (last == 13 || last == 5) {
						// 如果牌已有1 如 1,3,4,5 鬼
						if (s1.contains(1)) {
							s1.add((Integer) s1.toArray()[1] - 1);
						} else {
							s1.add(1);
						}
						continue;
					}
					s1.add(last + 1);
				}
			}
			p1 = s1;
		}
		if (kingCount2 > 0) {
			s2.add(list2.get(0));
			// 将有癞子的牌转换成实际牌
			for (int i = 0; i < list2.size() - 1; i++) {
				if ((temp = list2.get(i + 1) - list2.get(i) - 1) != 0 && temp <= kingCount2) {
					kingCount2 = kingCount2 - temp;
					for (int j = 0; j < temp; j++) {
						s2.add(list2.get(i) + 1 + j);
					}
				}
				s2.add(list2.get(i + 1));
			}
			// 多余鬼牌处理
			if (kingCount2 > 0) {
				for (int i = 0; i < kingCount2; i++) {
					// 取出牌最后的元素
					int last = s2.last();
					if (last == 13 || last == 5) {
						if (s2.contains(1)) {
							s2.add((Integer) s2.toArray()[1] - 1);
						} else {
							s2.add(1);
						}
						continue;
					}
					s2.add(last + 1);
				}
			}
			p2 = s2;
		}
		if (p1.contains(1)) {
			if (p2.contains(1)) {
				if (p1.last() > p2.last()) {
					return 1;
				} else if (p1.last() < p2.last()) {
					return -1;
				} else {
					return 0;
				}
			} else {
				return 1;
			}
		} else {
			if (p2.contains(1)) {
				return -1;
			} else {
				if (p1.last() > p2.last()) {
					return 1;
				} else if (p1.last() < p2.last()) {
					return -1;
				} else {
					return 0;
				}
			}
		}
	}

	public static int compareThree(JSONArray player1, JSONArray player2) {
		// 有癞子时三条之间的对比
		if (getKingPaiCount(player1) > 0 || getKingPaiCount(player2) > 0)
			return compareThreeHasKing(player1, player2);
		// 无癞子之间对比
		ArrayList<ArrayList<Integer>> p1 = getListByNum(player1);
		ArrayList<ArrayList<Integer>> p2 = getListByNum(player2);
		int t1 = 0;
		int t2 = 0;
		ArrayList<Integer> s1 = new ArrayList<>();
		ArrayList<Integer> s2 = new ArrayList<>();
		for (int i = 0; i < p1.size(); i++) {
			if (p1.get(i).size() == 3) {
				t1 = i + 1;
				if (player1.size() == 3) {
					break;
				}
			}
			if (p1.get(i).size() == 1) {
				s1.add(i + 1);
			}
			if (t1 > 0 && s1.size() == 2) {
				break;
			}
		}
		for (int i = 0; i < p2.size(); i++) {
			if (p2.get(i).size() == 3) {
				t2 = i + 1;
				if (player2.size() == 3) {
					break;
				}
			}
			if (p2.get(i).size() == 1) {
				s2.add(i + 1);
			}
			if (t2 > 0 && s2.size() == 2) {
				break;
			}
		}
		if (t1 > t2) {
			if (t2 == 1) {
				return -1;
			}
			return 1;
		} else if (t1 == t2) {
			if (s1.size() == 0 && s2.size() == 0) {
				return 0;
			}
			if (s1.contains(1) && !s2.contains(1)) {
				return 1;
			} else if (!s1.contains(1) && s2.contains(1)) {
				return -1;
			} else {
				if (s1.get(1) > s2.get(1)) {
					return 1;
				} else if (s1.get(1) < s2.get(1)) {
					return -1;
				} else {
					if (s1.get(0) > s2.get(0)) {
						return 1;
					} else if (s1.get(0) < s2.get(0)) {
						return -1;
					} else {
						return 0;
					}
				}
			}
		} else {
			if (t1 == 1) {
				return 1;
			}
			return -1;
		}
	}

	private static int compareThreeHasKing(JSONArray player1, JSONArray player2) {
		// 获取鬼牌数量
		int kingCount1 = getKingPaiCount(player1);
		int kingCount2 = getKingPaiCount(player2);

		ArrayList<ArrayList<Integer>> p1 = getListByNum(player1);
		ArrayList<ArrayList<Integer>> p2 = getListByNum(player2);
		int t1 = 0;
		int t2 = 0;
		ArrayList<Integer> s1 = new ArrayList<>();
		ArrayList<Integer> s2 = new ArrayList<>();
		// 癞子玩法
		TreeSet<Integer> treeSet1 = sortByNum(player1);
		TreeSet<Integer> treeSet2 = sortByNum(player2);
		// 头道三张鬼牌时当作是A
		if (player1.size() == 3 && treeSet1.isEmpty()) {
			t1 = 1;
		} else if (kingCount1 == 2) {
			t1 = treeSet1.last();
			if (treeSet1.size() + kingCount1 == 5) {
				Iterator<Integer> iterator1 = treeSet1.iterator();
				s1.add(iterator1.next());
				s1.add(iterator1.next());
			}
		} else {
			for (int i = 0; i < p1.size(); i++) {
				if (p1.get(i).size() + kingCount1 == 3) {
					t1 = i + 1;
					if (player1.size() == 3) {
						break;
					}
				}
				if (p1.get(i).size() == 1) {
					s1.add(i + 1);
				}
				if (t1 > 0 && s1.size() == 2) {
					break;
				}
			}
		}

		// 三张鬼牌
		if (player2.size() == 3 && treeSet2.isEmpty()) {
			t2 = 1;
		} else if (kingCount2 == 2) {
			t2 = treeSet2.last();
			if (treeSet2.size() + kingCount2 == 5) {
				Iterator<Integer> iterator1 = treeSet2.iterator();
				s2.add(iterator1.next());
				s2.add(iterator1.next());
			}
		} else {
			for (int i = 0; i < p2.size(); i++) {
				if (p2.get(i).size() + kingCount2 == 3) {
					t2 = i + 1;
					if (player2.size() == 3) {
						break;
					}
				}
				if (p2.get(i).size() == 1) {
					s2.add(i + 1);
				}
				if (t2 > 0 && s2.size() == 2) {
					break;
				}
			}
		}
		if (t1 > t2) {
			if (t2 == 1) {
				return -1;
			}
			return 1;
		} else if (t1 == t2) {
			if (s1.size() == 0 && s2.size() == 0) {
				return 0;
			}
			if (s1.contains(1) && !s2.contains(1)) {
				return 1;
			} else if (!s1.contains(1) && s2.contains(1)) {
				return -1;
			} else {
				if (s1.get(1) > s2.get(1)) {
					return 1;
				} else if (s1.get(1) < s2.get(1)) {
					return -1;
				} else {
					if (s1.get(0) > s2.get(0)) {
						return 1;
					} else if (s1.get(0) < s2.get(0)) {
						return -1;
					} else {
						return 0;
					}
				}
			}
		} else {
			if (t1 == 1) {
				return 1;
			}
			return -1;
		}
	}

	public static int compareTwo(JSONArray player1, JSONArray player2) {
		ArrayList<ArrayList<Integer>> p1 = getListByNum(player1);
		ArrayList<ArrayList<Integer>> p2 = getListByNum(player2);

		ArrayList<Integer> t1 = new ArrayList<Integer>();
		ArrayList<Integer> t2 = new ArrayList<Integer>();
		int s1 = 0;
		int s2 = 0;
		for (int i = 0; i < p1.size(); i++) {
			if (p1.get(i).size() == 2) {
				t1.add(i + 1);
			}
			if (p1.get(i).size() == 1) {
				s1 = i + 1;
			}
		}
		for (int i = 0; i < p1.size(); i++) {
			if (p2.get(i).size() == 2) {
				t2.add(i + 1);
			}
			if (p2.get(i).size() == 1) {
				s2 = i + 1;
			}
		}

		if (t1.get(0) == 1) {
			if (t2.get(0) == 1) {
				if (t1.get(1) > t2.get(1)) {
					return 1;
				} else if (t1.get(1) < t2.get(1)) {
					return -1;
				} else if (t1.get(1) == t2.get(1)) {
					if (s1 == s2) {
						return 0;
					} else {
						if (s1 == 1) {
							return 1;
						} else if (s2 == 1) {
							return -1;
						} else {
							if (s1 > s2) {
								return 1;
							} else if (s1 < s2) {
								return -1;
							}
						}
					}
				}
			} else {
				return 1;
			}
		} else {
			if (t2.get(0) == 1) {
				return -1;
			} else {
				if (t1.get(1) > t2.get(1)) {
					return 1;
				} else if (t1.get(1) < t2.get(1)) {
					return -1;
				} else if (t1.get(1) == t2.get(1)) {
					if (t1.get(0) > t2.get(0)) {
						return 1;
					} else if (t1.get(0) < t2.get(0)) {
						return -1;
					} else {
						if (s1 == s2) {
							return 0;
						} else {
							if (s1 == 1) {
								return 1;
							} else if (s2 == 1) {
								return -1;
							} else {
								if (s1 > s2) {
									return 1;
								} else if (s1 < s2) {
									return -1;
								}
							}
						}
					}
				}
			}
		}
		return 0;
	}

	public static int compareOne(JSONArray player1, JSONArray player2) {
		ArrayList<ArrayList<Integer>> p1 = getListByNum(player1);
		ArrayList<ArrayList<Integer>> p2 = getListByNum(player2);

		ArrayList<Integer> t1 = new ArrayList<Integer>();
		ArrayList<Integer> t2 = new ArrayList<Integer>();
		int s1 = 0;
		int s2 = 0;
		// 含有癞子牌比较
		if (getKingPaiCount(player1) > 0 || getKingPaiCount(player2) > 0)
			return compareOneHasKing(player1, player2);
		// 无癞子牌比较
		for (int i = 0; i < p1.size(); i++) {
			if (p1.get(i).size() == 2) {
				s1 = i + 1;
			}
			if (p1.get(i).size() == 1) {
				t1.add(i + 1);
			}
		}
		for (int i = 0; i < p1.size(); i++) {
			if (p2.get(i).size() == 2) {
				s2 = i + 1;
			}
			if (p2.get(i).size() == 1) {
				t2.add(i + 1);
			}
		}
		if (s1 == s2) {
			if (t1.get(0) == 1 && t2.get(0) != 1) {
				return 1;
			} else if (t1.get(0) != 1 && t2.get(0) == 1) {
				return -1;
			} else {
				for (int i = 0; i < t1.size(); i++) {
					if (t1.get(t1.size() - 1) > t2.get(t2.size() - 1)) {
						return 1;
					} else if (t1.get(t1.size() - 1) < t2.get(t2.size() - 1)) {
						return -1;
					} else if (t1.get(t1.size() - 1) == t2.get(t2.size() - 1)) {
						t1.remove(t1.size() - 1);
						t2.remove(t2.size() - 1);
						i--;
					}
				}
			}

		} else {
			if (s1 == 1) {
				return 1;
			} else if (s2 == 1) {
				return -1;
			} else {
				if (s1 > s2) {
					return 1;
				} else {
					return -1;
				}
			}
		}
		return 0;
	}

	private static int compareOneHasKing(JSONArray player1, JSONArray player2) {
		ArrayList<ArrayList<Integer>> p1 = getListByNum(player1);
		ArrayList<ArrayList<Integer>> p2 = getListByNum(player2);
		ArrayList<Integer> t1 = new ArrayList<>();
		ArrayList<Integer> t2 = new ArrayList<>();
		int s1 = 0;
		int s2 = 0;
		// 获取鬼牌数量
		int kingCount1 = getKingPaiCount(player1);
		int kingCount2 = getKingPaiCount(player2);
		if (kingCount1 > 0) {
			TreeSet<Integer> treeSet = sortByNum(player1);
			if (treeSet.contains(1)) {
				s1 = 1;
			} else {
				s1 = treeSet.last();
			}
			Iterator<Integer> iterator = treeSet.iterator();
			while (iterator.hasNext()) {
				t1.add(iterator.next());
			}
			// 把最大的牌去除
			if (t1.contains(1)) {
				t1.remove(0);
			} else {
				t1.remove(t1.size() - 1);
			}
		} else {
			for (int i = 0; i < p1.size(); i++) {
				if (p1.get(i).size() == 2) {
					s1 = i + 1;
				}
				if (p1.get(i).size() == 1) {
					t1.add(i + 1);
				}
			}
		}
		if (kingCount2 > 0) {
			TreeSet<Integer> treeSet = sortByNum(player2);
			if (treeSet.contains(1)) {
				s2 = 1;
			} else {
				s2 = treeSet.last();
			}
			Iterator<Integer> iterator = treeSet.iterator();
			while (iterator.hasNext()) {
				t2.add(iterator.next());
			}
			// 把最大的牌去除
			if (t2.contains(1)) {
				t2.remove(0);
			} else {
				t2.remove(t2.size() - 1);
			}
		} else {
			for (int i = 0; i < p1.size(); i++) {
				if (p2.get(i).size() == 2) {
					s2 = i + 1;
				}
				if (p2.get(i).size() == 1) {
					t2.add(i + 1);
				}
			}
		}
		if (s1 == s2) {
			if (t1.get(0) == 1 && t2.get(0) != 1) {
				return 1;
			} else if (t1.get(0) != 1 && t2.get(0) == 1) {
				return -1;
			} else {
				for (int i = 0; i < t1.size(); i++) {
					if (t1.get(t1.size() - 1) > t2.get(t2.size() - 1)) {
						return 1;
					} else if (t1.get(t1.size() - 1) < t2.get(t2.size() - 1)) {
						return -1;
					} else if (t1.get(t1.size() - 1) == t2.get(t2.size() - 1)) {
						t1.remove(t1.size() - 1);
						t2.remove(t2.size() - 1);
						i--;
					}
				}
			}
		} else {
			if (s1 == 1) {
				return 1;
			} else if (s2 == 1) {
				return -1;
			} else {
				if (s1 > s2) {
					return 1;
				} else {
					return -1;
				}
			}
		}
		return 0;
	}

	public static int compareZero(JSONArray player1, JSONArray player2) {
		TreeSet<Integer> p1 = sortByNum(player1);
		TreeSet<Integer> p2 = sortByNum(player2);

		if (p1.first() == 1 && p2.first() != 1) {
			return 1;
		} else if (p1.first() != 1 && p2.first() == 1) {
			return -1;
		} else {
			for (int i = 0; i < p1.size(); i++) {
				if (p1.last() > p2.last()) {
					return 1;
				} else if (p1.last() < p2.last()) {
					return -1;
				} else if (p1.last() == p2.last()) {
					p1.remove(p1.last());
					p2.remove(p2.last());
					i--;
				}
			}
		}
		return 0;
	}

	private JSONArray transformKingPai(JSONArray player) {
		// 获得鬼牌
		int kingCount = getKingPaiCount(player);
		ArrayList<ArrayList<Integer>> listByNum = getListByNum(player);
		ArrayList<ArrayList<Integer>> listByFlower = getListByFlower(player);
		// 转换成的牌型
		JSONArray newPlayer = new JSONArray();
		// 转换为五同牌
		for (int i = 0; i < listByNum.size(); i++) {
			if (listByNum.get(i).size() + kingCount == 5) {
				for (int j = 0; j < 5; j++) {
					newPlayer.add("1-" + (i + 1));
				}
				return newPlayer;
			}
		}
		return newPlayer;
	}

}
