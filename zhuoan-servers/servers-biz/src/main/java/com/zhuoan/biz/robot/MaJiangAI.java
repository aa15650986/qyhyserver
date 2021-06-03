
package com.zhuoan.biz.robot;

import com.zhuoan.biz.core.qzmj.MaJiangCore;
import com.zhuoan.constant.QZMJConstant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class MaJiangAI {
    public MaJiangAI() {
    }

    public static void main(String[] args) {
        int[] list = new int[]{3, 19, 17, 27, 32, 1, 1, 14, 14, 14, 5, 0, 24, 4, 10, 13, 21};
        JSONObject special = new JSONObject();
        special.put("mj_count", 34);
        int jin = -1;
        int pai = getRobotChupai(list, special, jin);
        System.out.println("出牌：" + pai);
    }

    static int getRobotChupai(int[] list, JSONObject special, int jin) {
        int listSize = list.length;
        int mjCount = special.getInt("mj_count");
        int[] arr = new int[mjCount];

        int ret_needhun;
        for(ret_needhun = 0; ret_needhun < listSize; ++ret_needhun) {
            ++arr[list[ret_needhun]];
        }

        ret_needhun = 32;
        int ret_nexus = 0;
        int ret_pai = list[0];
        List<Integer> ret_tinglist = new ArrayList();
        boolean has_single = false;

        for(int k = 0; k < list.length; ++k) {
            if (list[k] != jin) {
                --arr[list[k]];
                int[] new_list = Arrays.copyOf(list, list.length);
                new_list[k] = -1;
                special.put("pai", new_list);
                List<Integer> tingList = canTingPai(arr, jin, special);
                int tingSize = tingList.size();
                if (tingSize > 0) {
                    if (((List)ret_tinglist).size() < tingSize) {
                        ret_tinglist = tingList;
                        ret_needhun = 1;
                        ret_pai = list[k];
                    }
                } else if (((List)ret_tinglist).size() == 0) {
                    int needhun = getNeedhunForHu(arr, jin, special);
                    int nexus = has_nexus(list[k], arr);
                    if (nexus == 0) {
                        if (!has_single) {
                            ret_needhun = needhun;
                            ret_pai = list[k];
                        }

                        has_single = true;
                        if (list[k] > 26) {
                            ret_needhun = needhun;
                            ret_pai = list[k];
                        }

                        if ((list[k] % 9 < 1 || list[k] % 9 > 7) && ret_pai <= 26) {
                            ret_needhun = needhun;
                            ret_pai = list[k];
                        }
                    } else if (!has_single) {
                        if (needhun < ret_needhun) {
                            ret_needhun = needhun;
                            ret_pai = list[k];
                        } else if (needhun == ret_needhun) {
                            if (nexus < ret_nexus) {
                                ret_nexus = nexus;
                                ret_pai = list[k];
                            } else if (nexus == ret_nexus) {
                                if (list[k] > 26) {
                                    ret_pai = list[k];
                                }

                                if ((list[k] % 9 < 1 || list[k] % 9 > 7) && ret_pai <= 26) {
                                    ret_pai = list[k];
                                }
                            }
                        }
                    }
                }

                ++arr[list[k]];
            }
        }

        return ret_pai;
    }

    static List<Integer> canTingPai(int[] arr, int jin, JSONObject special) {
        List<Integer> myPai = getMaJiangListByIndex(arr, special);
        List<Integer> tingList = MaJiangCore.isTingPai(myPai, jin);
        if (tingList.size() > 0) {
            System.out.println(JSONArray.fromObject(myPai));
            System.out.println(JSONArray.fromObject(tingList));
        }

        return getMaJiangIndex(tingList, special);
    }

    static List<Integer> getMaJiangListByIndex(int[] arr, JSONObject special) {
        List<Integer> myPai = new ArrayList();
        JSONArray pais = special.getJSONArray("pai");

        for(int i = 0; i < pais.size(); ++i) {
            int index = pais.getInt(i);
            if (index >= 0) {
                myPai.add(QZMJConstant.ALL_CAN_HU_PAI[index]);
            }
        }

        return myPai;
    }

    static List<Integer> getMaJiangIndex(List<Integer> pais, JSONObject special) {
        List<Integer> paiIndex = new ArrayList();
        int[] paiArray = QZMJConstant.ALL_CAN_HU_PAI;
        Iterator var4 = pais.iterator();

        while(var4.hasNext()) {
            int i = (Integer)var4.next();

            for(int j = 0; j < paiArray.length; ++j) {
                if (i == paiArray[j]) {
                    paiIndex.add(j);
                }
            }
        }

        return paiIndex;
    }

    static int has_nexus(int i, int[] arr) {
        if (i > 26) {
            return arr[i];
        } else if (i % 9 == 8) {
            return arr[i] + arr[i - 1] + arr[i - 2];
        } else if (i % 9 == 7) {
            return arr[i] + arr[i - 1] + arr[i - 2] + arr[i + 1];
        } else if (i % 9 == 0) {
            return arr[i] + arr[i + 1] + arr[i + 2];
        } else {
            return i % 9 == 1 ? arr[i] + arr[i + 1] + arr[i + 2] + arr[i - 1] : arr[i] + arr[i + 1] + arr[i + 2] + arr[i - 1] + arr[i - 2];
        }
    }

    static JSONObject fmin_data(JSONObject data1, JSONObject data2) {
        return data1.getInt("needhun") > data2.getInt("needhun") ? data2 : data1;
    }

    static JSONObject del_list(int[] old_arr, int i, int j, JSONObject data) {
        int[] arr = Arrays.copyOf(old_arr, old_arr.length);

        for(int k = 0; k < 3; ++k) {
            if (arr[i + k] > 0) {
                --arr[i + k];
            } else {
                int needhun = data.getInt("needhun");
                ++needhun;
                data.put("needhun", needhun);
            }
        }

        return dfs(arr, i, j, data);
    }

    static JSONObject del_same(int[] old_arr, int i, int j, JSONObject data) {
        int[] arr = Arrays.copyOf(old_arr, old_arr.length);
        arr[i] %= 3;
        int needhun;
        switch(arr[i]) {
            case 0:
            default:
                break;
            case 1:
                if (data.getBoolean("hasjiang")) {
                    needhun = data.getInt("needhun");
                    needhun += 2;
                    data.put("needhun", needhun);
                } else {
                    needhun = data.getInt("needhun");
                    ++needhun;
                    data.put("needhun", needhun);
                    data.put("hasjiang", true);
                }
                break;
            case 2:
                if (data.getBoolean("hasjiang")) {
                    needhun = data.getInt("needhun");
                    ++needhun;
                    data.put("needhun", needhun);
                } else {
                    data.put("hasjiang", true);
                }
        }

        arr[i] = 0;
        return dfs(arr, i + 1, j, data);
    }

    static JSONObject dfs(int[] arr, int i, int j, JSONObject data) {
        int needhun;
        if (i > j) {
            if (!data.getBoolean("hasjiang")) {
                needhun = data.getInt("needhun");
                needhun += 2;
                data.put("needhun", needhun);
            }

            data.put("arr", arr);
            return data;
        } else if (i % 9 == 6 && i < 27 && arr[i + 1] % 3 == 1 && arr[i + 2] % 3 == 1) {
            return del_list(arr, i, j, data);
        } else if (arr[i] == 0) {
            return dfs(arr, i + 1, j, data);
        } else if (i % 9 >= 7 || i >= 27 || arr[i + 1] <= 0 && arr[i + 2] <= 0) {
            return del_same(arr, i, j, data);
        } else {
            needhun = data.getInt("needhun");
            boolean hasjiang = data.getBoolean("hasjiang");
            JSONObject d = new JSONObject();
            d.put("needhun", needhun);
            d.put("hasjiang", hasjiang);
            JSONObject tmp1 = del_list(arr, i, j, d);
            JSONObject tmp2 = del_same(arr, i, j, d);
            return fmin_data(tmp1, tmp2);
        }
    }

    static int getneedhun(int[] old_arr, int type, boolean hasjiang) {
        JSONObject data = new JSONObject();
        data.put("needhun", 0);
        data.put("hasjiang", hasjiang);
        int[] arr = Arrays.copyOf(old_arr, old_arr.length);
        int i = 0;
        int j = 0;
        switch(type) {
            case 0:
                i = 0;
                j = 8;
                break;
            case 1:
                i = 9;
                j = 17;
                break;
            case 2:
                i = 18;
                j = 26;
                break;
            case 3:
                i = 27;
                j = 33;
        }

        data = dfs(arr, i, j, data);
        return data.getInt("needhun");
    }

    static int getNeedhunForHu(int[] old_arr, int Hun, JSONObject special) {
        int[] arr = Arrays.copyOf(old_arr, old_arr.length);
        int HunCount = 0;
        if (Hun >= 0) {
            HunCount = arr[Hun];
            arr[Hun] = 0;
        }

        int min_needhun = 32;

        for(int i = 0; i < 4; ++i) {
            int needhun = 0;

            for(int j = 0; j < 4; ++j) {
                needhun += getneedhun(arr, j, j != i);
            }

            if (needhun < min_needhun) {
                min_needhun = needhun;
            }
        }

        return min_needhun - HunCount;
    }
}
