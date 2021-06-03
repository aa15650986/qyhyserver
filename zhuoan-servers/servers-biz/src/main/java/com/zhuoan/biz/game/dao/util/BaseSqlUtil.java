
package com.zhuoan.biz.game.dao.util;

import com.zhuoan.dao.DBUtil;
import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseSqlUtil {
    private static final Logger log = LoggerFactory.getLogger(BaseSqlUtil.class);

    public BaseSqlUtil() {
    }

    public static JSONObject getObjectByOneConditions(String field, String tableName, String conditions, String conditionsValue) {
        JSONArray jsonArray = new JSONArray();
        String[] c1 = new String[]{conditions};
        String[] c2 = new String[]{conditionsValue};

        try {
            jsonArray = getDataList(field, tableName, c1, c2, "", "", "", 0, 0);
        } catch (Exception var8) {
            var8.printStackTrace();
        }

        return jsonArray != null && jsonArray.size() > 0 ? jsonArray.getJSONObject(0) : null;
    }

    public static JSONObject getObjectByConditions(String field, String tableName, String[] conditions, Object[] conditionsValue) {
        JSONArray jsonArray = new JSONArray();

        try {
            jsonArray = getDataList(field, tableName, conditions, conditionsValue, "", "", "", 0, 0);
        } catch (Exception var6) {
            var6.printStackTrace();
        }

        return jsonArray != null && jsonArray.size() > 0 ? jsonArray.getJSONObject(0) : null;
    }

    public static JSONObject getObjectByConditionsMore(String field, String tableName, String[] conditions, Object[] conditionsValue, String sort) {
        JSONArray jsonArray = new JSONArray();

        try {
            jsonArray = getDataList(field, tableName, conditions, conditionsValue, sort, "", "", 0, 0);
        } catch (Exception var7) {
            var7.printStackTrace();
        }

        return jsonArray != null && jsonArray.size() > 0 ? jsonArray.getJSONObject(0) : null;
    }

    public static JSONArray getDataList(String field, String tableName, String[] conditions, Object[] conditionsValue, String sort, String keyFieldName, String keyValue, int page, int limitNum) {
        StringBuilder sb = new StringBuilder();
        sb.append("select ").append(field).append(" FROM ").append(tableName).append(" where 1=1  ");
        List param = new ArrayList();
        if (conditions != null) {
            for(int i = 0; i < conditions.length; ++i) {
                sb.append(" and ").append(conditions[i]).append(" =? ");
                param.add(conditionsValue[i]);
            }
        }

        if (keyFieldName != null && keyValue != null && !keyFieldName.equals("") && !keyValue.equals("")) {
            sb.append("and CONCAT(").append(keyFieldName).append(") LIKE '%").append(keyValue).append("%' ");
        }

        sb.append(sort);
        if (page > 0 && limitNum > 0) {
            sb.append(" LIMIT ").append((page - 1) * limitNum).append(",").append(limitNum);
        }

        return DBUtil.getObjectListBySQL(sb.toString(), param.toArray());
    }

    public static JSONArray getDataList(String field, String tableName, String[] conditions, Object[] conditionsValue, String sort, String keyFieldName, String keyValue) {
        return getDataList(field, tableName, conditions, conditionsValue, sort, keyFieldName, keyValue, 0, 0);
    }

    public static JSONArray getDataListLike(String field, String tableName, String[] conditions, Object[] conditionsValue, String sort, String keyFieldName, String keyValue, int page, int limitNum) {
        StringBuilder sb = new StringBuilder();
        sb.append("select ").append(field).append(" FROM ").append(tableName).append(" where 1=1  ");
        if (conditions != null) {
            for(int i = 0; i < conditions.length; ++i) {
                sb.append("and ").append(conditions[i]).append(" LIKE '%").append(conditionsValue[i]).append("%' ");
            }
        }

        if (keyFieldName != null && keyValue != null && !keyFieldName.equals("") && !keyValue.equals("")) {
            sb.append("and CONCAT(").append(keyFieldName).append(") LIKE '%").append(keyValue).append("%' ");
        }

        sb.append(sort);
        if (page > 0 && limitNum > 0) {
            sb.append(" LIMIT ").append((page - 1) * limitNum).append(",").append(limitNum);
        }

        return DBUtil.getObjectListBySQL(sb.toString(), (Object[])null);
    }

    public static JSONArray getDataListLike(String field, String tableName, String[] conditions, Object[] conditionsValue, String sort, String keyFieldName, String keyValue) {
        return getDataListLike(field, tableName, conditions, conditionsValue, sort, keyFieldName, keyValue, 0, 0);
    }

    public static long updateDataResultId(String tableName, String[] attribute, Object[] attributeValue, String[] conditions, Object[] conditionsValue) {
        StringBuilder sb = new StringBuilder();
        sb.append("update ").append(tableName).append(" set ");
        List param = new ArrayList();
        int i;
        if (conditionsValue != null && attributeValue == null) {
            for(i = 0; i < conditions.length; ++i) {
                param.add(conditionsValue[i]);
            }
        } else if (conditionsValue == null && attributeValue != null) {
            for(i = 0; i < attributeValue.length; ++i) {
                param.add(attributeValue[i]);
            }
        } else if (conditionsValue != null && attributeValue != null) {
            for(i = 0; i < attributeValue.length; ++i) {
                param.add(attributeValue[i]);
            }

            for(i = 0; i < conditionsValue.length; ++i) {
                param.add(conditionsValue[i]);
            }
        }

        if (attribute != null) {
            for(i = 0; i < attribute.length; ++i) {
                if (i > 0) {
                    sb.append(",");
                }

                sb.append(attribute[i]).append("=? ");
            }
        }

        if (conditions != null) {
            for(i = 0; i < conditions.length; ++i) {
                if (i == 0) {
                    sb.append("where ");
                } else {
                    sb.append(" and ");
                }

                sb.append(conditions[i]).append("=? ");
            }
        }

        return DBUtil.executeUpdateGetKeyBySQL(sb.toString(), param.toArray());
    }

    public static boolean updateData(String tableName, String[] attribute, Object[] attributeValue, String[] conditions, Object[] conditionsValue) {
        StringBuilder sb = new StringBuilder();
        sb.append("update ").append(tableName).append(" set ");
        List param = new ArrayList();
        int i;
        if (conditionsValue != null && attributeValue == null) {
            for(i = 0; i < conditions.length; ++i) {
                param.add(conditionsValue[i]);
            }
        } else if (conditionsValue == null && attributeValue != null) {
            for(i = 0; i < attributeValue.length; ++i) {
                param.add(attributeValue[i]);
            }
        } else if (conditionsValue != null && attributeValue != null) {
            for(i = 0; i < attributeValue.length; ++i) {
                param.add(attributeValue[i]);
            }

            for(i = 0; i < conditionsValue.length; ++i) {
                param.add(conditionsValue[i]);
            }
        }

        if (attribute != null) {
            for(i = 0; i < attribute.length; ++i) {
                if (i > 0) {
                    sb.append(",");
                }

                sb.append(attribute[i]).append("=? ");
            }
        }

        if (conditions != null) {
            for(i = 0; i < conditions.length; ++i) {
                if (i == 0) {
                    sb.append("where ");
                } else {
                    sb.append(" and ");
                }

                sb.append(conditions[i]).append("=? ");
            }
        }

        return DBUtil.executeUpdateBySQL(sb.toString(), param.toArray()) > 0;
    }

    public static boolean updateDataByMore(String tableName, String[] attribute, Object[] attributeValue, String[] conditions, Object[] conditionsValue, String moreConditions) {
        StringBuilder sb = new StringBuilder();
        sb.append("update ").append(tableName).append(" set ");
        List param = new ArrayList();
        int i;
        if (conditionsValue != null && attributeValue == null) {
            for(i = 0; i < conditions.length; ++i) {
                param.add(conditionsValue[i]);
            }
        } else if (conditionsValue == null && attributeValue != null) {
            for(i = 0; i < attributeValue.length; ++i) {
                param.add(attributeValue[i]);
            }
        } else if (conditionsValue != null && attributeValue != null) {
            for(i = 0; i < attributeValue.length; ++i) {
                param.add(attributeValue[i]);
            }

            for(i = 0; i < conditionsValue.length; ++i) {
                param.add(conditionsValue[i]);
            }
        }

        if (attribute != null) {
            for(i = 0; i < attribute.length; ++i) {
                if (i > 0) {
                    sb.append(",");
                }

                sb.append(attribute[i]).append("=? ");
            }
        }

        if (conditions != null) {
            for(i = 0; i < conditions.length; ++i) {
                if (i == 0) {
                    sb.append("where ");
                } else {
                    sb.append(" and ");
                }

                sb.append(conditions[i]).append("=? ");
            }
        }

        if (moreConditions != null) {
            sb.append(moreConditions);
        }

        return DBUtil.executeUpdateBySQL(sb.toString(), param.toArray()) > 0;
    }

    public static boolean delUpdateData(String tableName, String[] conditions, Object[] conditionsValue) {
        StringBuilder sb = new StringBuilder();
        sb.append("delete from ").append(tableName).append(" where 1=1 ");
        List param = new ArrayList();
        if (conditions != null) {
            for(int i = 0; i < conditions.length; ++i) {
                sb.append("and ").append(conditions[i]).append("=? ");
                param.add(conditionsValue[i]);
            }
        }

        return DBUtil.executeUpdateBySQL(sb.toString(), param.toArray()) > 0;
    }

    public static boolean delUpdateDataByMore(String tableName, String[] conditions, Object[] conditionsValue, String moreConditions) {
        StringBuilder sb = new StringBuilder();
        sb.append("delete from ").append(tableName).append(" where 1=1 ");
        List param = new ArrayList();
        if (conditions != null) {
            for(int i = 0; i < conditions.length; ++i) {
                sb.append("and ").append(conditions[i]).append("=? ");
                param.add(conditionsValue[i]);
            }
        }

        if (moreConditions != null) {
            sb.append(moreConditions);
        }

        return DBUtil.executeUpdateBySQL(sb.toString(), param.toArray()) > 0;
    }

    public static long insertDataResultId(String tableName, String[] attribute, Object[] attributeValue) {
        StringBuilder sb = new StringBuilder();
        sb.append("insert ").append(tableName).append(" ");
        List param = new ArrayList();
        int i;
        if (attribute != null) {
            for(i = 0; i < attribute.length; ++i) {
                if (i == 0) {
                    sb.append("(");
                } else {
                    sb.append(",");
                }

                sb.append(attribute[i]);
            }
        }

        sb.append(") values ");
        if (attributeValue != null) {
            for(i = 0; i < attributeValue.length; ++i) {
                if (i == 0) {
                    sb.append("(?");
                } else {
                    sb.append(",?");
                }

                param.add(attributeValue[i]);
                if (i == attributeValue.length - 1) {
                    sb.append(")");
                }
            }
        }

        return DBUtil.executeUpdateGetKeyBySQL(sb.toString(), param.toArray());
    }

    public static boolean insertData(String tableName, String[] attribute, Object[] attributeValue) {
        StringBuilder sb = new StringBuilder();
        sb.append("insert ").append(tableName).append(" ");
        List list = new ArrayList();
        int i;
        if (attribute != null) {
            for(i = 0; i < attribute.length; ++i) {
                if (i == 0) {
                    sb.append("(");
                } else {
                    sb.append(",");
                }

                sb.append(attribute[i]);
            }
        }

        sb.append(") values ");
        if (attributeValue != null) {
            for(i = 0; i < attributeValue.length; ++i) {
                if (i == 0) {
                    sb.append("(?");
                } else {
                    sb.append(",?");
                }

                list.add(attributeValue[i]);
                if (i == attributeValue.length - 1) {
                    sb.append(")");
                }
            }
        }

        return DBUtil.executeUpdateGetKeyBySQL(sb.toString(), list.toArray()) > 0L;
    }

    public static boolean insertData(String tableName, String[] attribute, List<Object[]> attributeValueList) {
        if (attribute != null && attributeValueList != null && attributeValueList.size() != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("insert ").append(tableName).append(" ");

            int i;
            for(i = 0; i < attribute.length; ++i) {
                if (i == 0) {
                    sb.append("(");
                } else {
                    sb.append(",");
                }

                sb.append(attribute[i]);
            }

            sb.append(") values ");

            for(i = 0; i < attributeValueList.size(); ++i) {
                Object[] attributeValue = (Object[])attributeValueList.get(i);
                if (attributeValue != null) {
                    for(int j = 0; j < attributeValue.length; ++j) {
                        if (j == 0) {
                            sb.append("(");
                        } else {
                            sb.append(",");
                        }

                        if (attributeValue[j] instanceof String) {
                            sb.append("'");
                            sb.append(attributeValue[j]);
                            sb.append("'");
                        } else {
                            sb.append(attributeValue[j]);
                        }

                        if (j == attributeValue.length - 1) {
                            sb.append(")");
                        }
                    }
                }

                if (i != attributeValueList.size() - 1) {
                    sb.append(",");
                }
            }

            return DBUtil.executeUpdateGetKeyBySQL(sb.toString(), (Object[])null) > 0L;
        } else {
            return false;
        }
    }
}
