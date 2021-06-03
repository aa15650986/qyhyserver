package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.CircleBiz;
import com.zhuoan.biz.game.dao.CircleDao;
import com.zhuoan.biz.game.dao.GameDao;
import com.zhuoan.biz.game.dao.util.BaseSqlUtil;
import com.zhuoan.constant.CircleConstant;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.service.RedisInfoService;
import com.zhuoan.util.DateUtils;
import com.zhuoan.util.Dto;
import com.zhuoan.util.MathDelUtil;
import com.zhuoan.util.TimeUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Resource;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CircleBizImpl implements CircleBiz {
    private static final Logger log = LoggerFactory.getLogger(CircleBizImpl.class);
    @Resource
    private CircleDao circleDao;
    @Resource
    private GameDao gameDao;
    @Resource
    private RedisInfoService redisInfoService;

    public CircleBizImpl() {
    }

    public JSONObject addGameCircleInfo(JSONObject data) {
        JSONObject condition = this.getUserCondition(data);
        if (condition.getInt("code") == 0) {
            return condition;
        } else {
            long userId = data.getLong("uid");
            String platform = data.getString("platform");
            JSONObject user = this.gameDao.getUserByID(userId);
            Integer roomCard = user.getInt("roomcard");
            String fundCountObj = this.getSysValue(platform, "game_circle_room_card");
            String gameCircleAuditObj = this.getSysValue(platform, "game_circle_audit");
            String gameCircleHpObj = this.getSysValue(platform, "game_circle_hp");
            int fundCount = "".equals(fundCountObj) ? 0 : Integer.valueOf(fundCountObj);
            String gameCircleAudit = "".equals(gameCircleAuditObj) ? "0" : gameCircleAuditObj;
            long circleId = this.insertCircleInfo(data);
            if (circleId < 0L) {
                data.element("code", 0).element("msg", "创建俱乐部失败").element("error_msg", "新增俱乐部信息数据时出错");
                return data;
            } else {
                JSONObject updateUser = new JSONObject();
                updateUser.put("id", userId);
                updateUser.put("roomcard", roomCard - fundCount);
                this.gameDao.updateUserInfo(updateUser);
                JSONObject zaUserdeduction = new JSONObject();
                zaUserdeduction.put("userid", userId);
                zaUserdeduction.put("type", 0);
                zaUserdeduction.put("sum", -fundCount);
                zaUserdeduction.put("doType", 6);
                zaUserdeduction.put("creataTime", TimeUtil.getNowDate());
                zaUserdeduction.put("pocketNew", roomCard);
                zaUserdeduction.put("pocketOld", fundCount + roomCard);
                zaUserdeduction.put("pocketChange", -fundCount);
                zaUserdeduction.put("operatorType", 3);
                zaUserdeduction.put("operatorId", userId);
                zaUserdeduction.put("reason", "创建俱乐部扣除");
                zaUserdeduction.put("platform", platform);
                zaUserdeduction.put("memo", "创建俱乐部扣除");
                this.gameDao.addUserWelfareRec(zaUserdeduction);
                JSONObject gameCircleFundBill = new JSONObject();
                gameCircleFundBill.put("circle_id", circleId);
                gameCircleFundBill.put("user_id", userId);
                gameCircleFundBill.put("operator_type", 1);
                gameCircleFundBill.put("fund_change_count", fundCount);
                gameCircleFundBill.put("fund_old", 0);
                gameCircleFundBill.put("platform", platform);
                gameCircleFundBill.put("create_user", userId);
                gameCircleFundBill.put("modify_user", userId);
                gameCircleFundBill.put("memo", user.getString("name") + " " + user.getString("account"));
                gameCircleFundBill.put("gmt_create", TimeUtil.getNowDate());
                gameCircleFundBill.put("gmt_modified", TimeUtil.getNowDate());
                this.circleDao.saveOrUpdateFundBill(gameCircleFundBill);
                if ("0".equals(gameCircleAudit)) {
                    JSONObject circleMember = new JSONObject();
                    Long gameCircleHp = "".equals(gameCircleHpObj) ? 0L : Long.valueOf(gameCircleHpObj);
                    circleMember.put("circle_id", circleId);
                    circleMember.put("user_id", userId);
                    circleMember.put("user_code", this.getCircleCode(2, 8));
                    circleMember.put("user_hp", gameCircleHp);
                    circleMember.put("user_role", 1);
                    circleMember.put("is_admin", 1);
                    circleMember.put("platform", data.getString("platform"));
                    circleMember.put("profit_ratio", 100);
                    circleMember.put("create_user", userId);
                    circleMember.put("modify_user", userId);
                    circleMember.put("gmt_create", TimeUtil.getNowDate());
                    circleMember.put("gmt_modified", TimeUtil.getNowDate());
                    if (this.circleDao.saveOrUpdateCircleMember(circleMember) < 0L) {
                        data.element("code", 0).element("msg", "创建俱乐部用户信息失败").element("error_msg", "新增俱乐部用户数据时出错");
                    } else {
                        data.element("code", 1).element("msg", "创建俱乐部成功");
                    }
                } else {
                    data.element("code", 1).element("msg", "创建俱乐部成功,但需管理员审核");
                }

                return data;
            }
        }
    }

    public JSONObject updateGameCircleInfo(JSONObject data) {
        Long circleId = data.getLong("circleId");
        Long userId = data.getLong("uid");
        String platform = data.getString("platform");
        JSONObject circleMember = this.circleDao.getCircleMemberByUserId(userId, circleId, platform);
        JSONObject circleInfo = this.circleDao.getCircleInfoById(circleId);
        JSONObject resObj = new JSONObject();
        if (circleInfo == null) {
            resObj.element("code", 0).element("msg", "不存在该俱乐部").element("error_msg", "俱乐部被删除或者没有该俱乐部");
            return resObj;
        } else if (circleMember == null) {
            resObj.element("code", 0).element("msg", "该用户不存在").element("error_msg", "该用户不存在这个俱乐部");
            return resObj;
        } else {
            String circleName = data.get("circleName") == null ? circleInfo.getString("circle_name") : data.getString("circleName");
            String circleNotice = data.get("circleNotice") == null ? circleInfo.getString("circle_notice") : data.getString("circleNotice");
            Integer createRoomRole = data.get("createRoomRole") == null ? circleInfo.getInt("create_room_role") : data.getInt("createRoomRole");
            Integer feeType = data.get("feeType") == null ? circleInfo.getInt("fee_type") : data.getInt("feeType");
            Integer fundPrepaidRole = data.get("fundPrepaidRole") == null ? circleInfo.getInt("fund_prepaid_role") : data.getInt("fundPrepaidRole");
            Integer fundBillRole = data.get("fundBillRole") == null ? circleInfo.getInt("fund_bill_role") : data.getInt("fundBillRole");
            String quickSetting = data.get("quickSetting") == null ? (circleInfo.get("quick_setting") == null ? "" : circleInfo.getString("quick_setting")) : data.getString("quickSetting");
            boolean isUpdateRole = false;
            JSONObject createUserCondition = this.isCreateUser(data);
            if (createUserCondition.getInt("code") == 1) {
                isUpdateRole = true;
            } else if (circleMember.get("is_admin") != null && circleMember.getInt("is_admin") == 1) {
                isUpdateRole = true;
            }

            if (isUpdateRole) {
                JSONObject updateCircleInfo = new JSONObject();
                updateCircleInfo.put("id", circleId);
                updateCircleInfo.put("circle_name", circleName);
                updateCircleInfo.put("circle_notice", circleNotice);
                updateCircleInfo.put("fee_type", feeType);
                updateCircleInfo.put("create_room_role", createRoomRole);
                updateCircleInfo.put("fund_prepaid_role", fundPrepaidRole);
                updateCircleInfo.put("fund_bill_role", fundBillRole);
                updateCircleInfo.put("quick_setting", quickSetting);
                updateCircleInfo.put("gmt_modified", TimeUtil.getNowDate());
                updateCircleInfo.put("modify_user", userId);
                this.circleDao.saveOrUpdateCircleInfo(updateCircleInfo);
                data.element("code", 1).element("msg", "修改俱乐部成功");
            } else {
                data.element("code", 0).element("msg", "权限不足,无法操作");
            }

            return data;
        }
    }

    public JSONObject dismissCircle(JSONObject data) {
        Long circleInfoId = data.getLong("circleId");
        JSONObject circleObj = this.circleDao.getCircleInfoById(circleInfoId);
        if (circleObj == null) {
            return data.element("code", 0).element("msg", "解散失败").element("error_msg", "该亲友圈已不存在");
        } else {
            String circleName = circleObj.get("circle_name") == null ? "" : circleObj.getString("circle_name");
            JSONObject createUserCondition = this.isCreateUser(data);
            if (createUserCondition.getInt("code") == 0) {
                return createUserCondition;
            } else {
                if (createUserCondition.getInt("code") == 1) {
                    JSONObject user = this.gameDao.getUserByID(circleObj.getLong("create_user"));
                    JSONObject circleInfo = new JSONObject();
                    circleInfo.put("id", circleInfoId);
                    circleInfo.put("is_delete", "Y");
                    circleInfo.put("gmt_modified", TimeUtil.getNowDate());
                    if (this.circleDao.saveOrUpdateCircleInfo(circleInfo) > 0) {
                        JSONObject updateUser = new JSONObject();
                        int fundCount = circleObj.get("fund_count") == null ? 0 : circleObj.getInt("fund_count");
                        int roomCard = user.get("roomcard") == null ? 0 : user.getInt("roomcard");
                        long userId = user.getLong("id");
                        String platform = user.get("platform") == null ? "" : user.getString("platform");
                        updateUser.put("id", user.getLong("id"));
                        updateUser.put("roomcard", fundCount + user.getInt("roomcard"));
                        this.gameDao.updateUserInfo(updateUser);
                        JSONObject zaUserdeduction = new JSONObject();
                        zaUserdeduction.put("userid", userId);
                        zaUserdeduction.put("type", 0);
                        zaUserdeduction.put("sum", fundCount);
                        zaUserdeduction.put("doType", 6);
                        zaUserdeduction.put("creataTime", TimeUtil.getNowDate());
                        zaUserdeduction.put("pocketNew", roomCard + fundCount);
                        zaUserdeduction.put("pocketOld", roomCard);
                        zaUserdeduction.put("pocketChange", fundCount);
                        zaUserdeduction.put("operatorType", 3);
                        zaUserdeduction.put("operatorId", userId);
                        zaUserdeduction.put("reason", "解散亲友圈返还");
                        zaUserdeduction.put("platform", platform);
                        zaUserdeduction.put("memo", "解散亲友圈返还");
                        this.gameDao.addUserWelfareRec(zaUserdeduction);
                        int res = this.adviceCircleMember(circleInfoId, platform, userId, circleName);
                        if (res < 0) {
                            data.element("code", 0).element("msg", "通知所有成员解散信息失败").element("error_msg", "通知所有成员解散信息失败");
                            return data;
                        }

                        data.element("code", 1).element("msg", "解散俱乐部成功");
                    } else {
                        data.element("code", 0).element("msg", "解散失败").element("error_msg", "数据库操作失败");
                    }
                }

                return data;
            }
        }
    }

    private int adviceCircleMember(long circleId, String platform, long createUser, String circleName) {
        JSONArray circleMemberArr = this.circleDao.getCircleMemberByCircleId(circleId);

        for(int i = 0; i < circleMemberArr.size(); ++i) {
            JSONObject circleMemberInfo = circleMemberArr.getJSONObject(i);
            Long userId = circleMemberInfo.get("user_id") == null ? 0L : circleMemberInfo.getLong("user_id");
            String superiorUserCode = circleMemberInfo.get("superior_user_code") == null ? "" : circleMemberInfo.getString("superior_user_code");
            JSONObject circleRecord = this.getCircleDismissRecord(circleId, platform, userId, createUser);
            circleRecord.put("superior_user_code", superiorUserCode);
            long pId = this.circleDao.saveOrUpdateAuditRecord(circleRecord);
            JSONObject circleMessage = this.getCircleDismissMessage(circleId, platform, userId, createUser, pId, circleName);
            if (this.circleDao.saveOrUpdateCircleMsg(circleMessage) < 0) {
                return -1;
            }
        }

        return 1;
    }

    private JSONObject getCircleDismissMessage(long circleId, String platform, long userId, long createUserId, long msgPid, String circleName) {
        return this.getCircleMessage(circleId, platform, userId, createUserId, msgPid, "5", "俱乐部解散", "俱乐部【" + circleName + "】已解散");
    }

    private JSONObject getCircleMessage(long circleId, String platform, long userId, long createUserId, long msgPid, String messageType, String msgTitle, String msgContent) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("circle_id", circleId);
        jsonObject.put("user_id", userId);
        jsonObject.put("message_type", messageType);
        jsonObject.put("is_read", "N");
        jsonObject.put("platform", platform);
        jsonObject.put("create_user", createUserId);
        jsonObject.put("modify_user", createUserId);
        jsonObject.put("is_delete", "N");
        jsonObject.put("msg_content", msgContent);
        jsonObject.put("msg_title", msgTitle);
        jsonObject.put("msg_pid", msgPid);
        jsonObject.put("gmt_modified", TimeUtil.getNowDate());
        return jsonObject;
    }

    private JSONObject getCircleDismissRecord(long circleId, String platform, Long userId, long createUserId) {
        return this.getCircleRecord(circleId, platform, userId, createUserId, "5");
    }

    private JSONObject getCircleRecord(long circleId, String platform, Long userId, long createUserId, String auditType) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("circle_id", circleId);
        jsonObject.put("user_id", userId);
        jsonObject.put("audit_type", auditType);
        jsonObject.put("is_deal", "N");
        jsonObject.put("platform", platform);
        jsonObject.put("create_user", createUserId);
        jsonObject.put("modify_user", createUserId);
        jsonObject.put("gmt_modified", TimeUtil.getNowDate());
        jsonObject.put("is_delete", "N");
        return jsonObject;
    }

    public JSONObject transferCircle(JSONObject data) {
        Long circleId = data.getLong("circleId");
        Long userId = data.getLong("uid");
        String platform = data.getString("platform");
        String newCreateAccount = data.getString("newCreateAccount");
        JSONObject newCreateUser = this.gameDao.getUserByAccount(newCreateAccount);
        JSONObject newCreateMember = this.circleDao.getCircleMemberByUserAccount(newCreateAccount, circleId, platform);
        JSONObject userMember = this.circleDao.getCircleMemberByUserId(userId, circleId, platform);
        JSONObject createUserCondition = this.isCreateUser(data);
        if (userMember == null) {
            data.element("code", 0).element("msg", "您无权进行转让操作").element("error_msg", "俱乐部账号不存在");
            return data;
        } else if (newCreateUser == null) {
            data.element("code", 0).element("msg", "转让账号不存在").element("error_msg", "转让账号不存在");
            return data;
        } else {
            long newUserId = newCreateUser.get("id") == null ? 0L : newCreateUser.getLong("id");
            if (0 == createUserCondition.getInt("code")) {
                return createUserCondition;
            } else {
                String sysSetting = this.getSysValue(platform, "game_circle_count");
                if ("".equals(sysSetting)) {
                    data.element("code", 0).element("msg", "管理员未对俱乐部系统进行配置").element("error_msg", "亲友圈配置没有数据");
                    return data;
                } else if (this.circleDao.getCountCircleByAccount(newCreateAccount, platform) + 1 > Integer.valueOf(sysSetting)) {
                    data.element("code", 0).element("msg", "该用户俱乐部已达到限制,无法完成转让").element("error_msg", "该用户的俱乐部比后台配置的最大创建俱乐部的值大");
                    return data;
                } else if (newCreateMember != null) {
                    data.element("code", 0).element("msg", "该玩家是该俱乐部,无法完成转让").element("error_msg", "玩家属于该俱乐部,不可以转让");
                    return data;
                } else {
                    if (!Dto.isObjNull(newCreateUser) && newCreateUser.containsKey("tel") && !Dto.stringIsNULL(newCreateUser.getString("tel")) && Pattern.matches("^((17[0-9])|(14[0-9])|(13[0-9])|(15[^4,\\D])|(18[0-9]))\\d{8}$", newCreateUser.getString("tel"))) {
                        JSONObject updateCircleInfo = new JSONObject();
                        updateCircleInfo.put("id", circleId);
                        updateCircleInfo.put("create_user", newUserId);
                        updateCircleInfo.put("gmt_modified", TimeUtil.getNowDate());
                        this.circleDao.saveOrUpdateCircleInfo(updateCircleInfo);
                        JSONObject updateCircleMember = new JSONObject();
                        updateCircleMember.put("id", userMember.getLong("id"));
                        updateCircleMember.put("user_id", newUserId);
                        updateCircleMember.put("create_user", newUserId);
                        updateCircleMember.put("gmt_modified", TimeUtil.getNowDate());
                        this.circleDao.saveOrUpdateCircleMember(updateCircleMember);
                        data.element("code", 1).element("msg", "转让成功");
                    } else {
                        data.element("code", 0).element("msg", "该玩家没有绑定电话号码,无法完成转让").element("error_msg", "该用户没有电话");
                    }

                    return data;
                }
            }
        }
    }

    public JSONObject mgrSettingCircle(JSONObject data) {
        Long circleId = data.getLong("circleId");
        Long userId = data.getLong("uid");
        Long adminId = data.getLong("adminId");
        int isAdmin = data.getInt("isAdmin");
        String platform = data.getString("platform");
        JSONObject admin = this.circleDao.getCircleMemberByUserId(adminId, circleId, platform);
        JSONObject createUserCondition = this.isCreateUser(data);
        if (createUserCondition.getInt("code") == 0) {
            return createUserCondition;
        } else if (admin == null) {
            data.element("code", 0).element("msg", "该玩家不是该俱乐部的成员").element("error_msg", "该玩家不是该俱乐部的成员");
            return data;
        } else {
            if (1 == isAdmin) {
                String userRole = admin.getString("user_role");
                if (1 == admin.getInt("is_admin")) {
                    data.element("code", 0).element("msg", "该玩家已经是管理员");
                    return data;
                }

                if ("2".equals(userRole)) {
                    data.element("code", 0).element("msg", "身份冲突,请先删除合伙人");
                    return data;
                }
            }

            if (this.circleDao.updateCircleMemberCreateUser(admin.getInt("user_role"), isAdmin, userId, adminId, circleId, platform) > 0) {
                if (isAdmin == 1) {
                    data.element("code", 1).element("msg", "设置管理员成功");
                } else {
                    data.element("code", 1).element("msg", "删除管理员成功");
                }
            } else {
                data.element("code", 0).element("msg", "设置管理员失败").element("error_msg", "数据库操作错误");
            }

            return data;
        }
    }

    public JSONArray queryCircleList(Long userId, String platform) {
        JSONArray circleList = this.circleDao.queryCircleList(userId, platform);

        for(int i = 0; i < circleList.size(); ++i) {
            JSONObject circleInfo = circleList.getJSONObject(i);
            int countCircleMember = this.circleDao.getCountCircleMemberByCircle(circleInfo.getLong("circle_id"), platform);
            circleInfo.element("count_member", countCircleMember);
        }

        return circleList;
    }

    public JSONObject updateProfitRatio(JSONObject object) {
        Long userId = object.getLong("uid");
        long circleId = object.getLong("circleId");
        int profitRatio = object.getInt("profitRatio");
        String platform = object.getString("platform");
        Long adminId = object.getLong("adminId");
        JSONObject resObj = new JSONObject();
        JSONObject circleMember = this.circleDao.getCircleMemberByUserId(userId, circleId, platform);
        String adminRole = circleMember.containsKey("is_admin") ? circleMember.getString("is_admin") : "0";
        if (circleMember.containsKey("user_role")) {
            circleMember.getString("user_role");
        } else {
            String var10000 = "3";
        }

        JSONObject circlePartner = this.circleDao.getCircleMemberByUserId(adminId, circleId, platform);
        int ratio = circlePartner.containsKey("profit_ratio") ? circlePartner.getInt("profit_ratio") : 0;
        String seqMemberIds = circlePartner.containsKey("seq_member_ids") ? circlePartner.getString("seq_member_ids") : "";
        log.info(String.valueOf(circlePartner));
        if (circleMember != null && circleMember.getLong("circle_id") == circleId) {
            if (circlePartner == null) {
                resObj.element("code", 0).element("msg", "被操作的玩家未在该俱乐部!!!").element("error_msg", "被操作的用户没有加入这个俱乐部");
                return resObj;
            } else if (circlePartner.get("user_role") != null && circlePartner.getInt("user_role") != 3) {
                if ("0".equals(adminRole) && !seqMemberIds.contains("$" + circleMember.getString("id") + "$")) {
                    resObj.element("code", 0).element("msg", "您没有权限进行修改!!!").element("error_msg", "被操作的用户的上级玩家不是操作用户");
                    return resObj;
                } else if (profitRatio < ratio) {
                    resObj.element("code", 0).element("msg", "不允许直接下调，请先重置合伙比例").element("error_msg", "调整的合伙比例比原本的合伙比例低");
                    return resObj;
                } else {
                    JSONObject upCircleMember = this.circleDao.getUpCircleMemberByUserId(adminId, circleId, platform);
                    int upProfitRatio = upCircleMember.get("profit_ratio") == null ? 100 : upCircleMember.getInt("profit_ratio");
                    if (profitRatio > upProfitRatio) {
                        resObj.element("code", 0).element("msg", "调整的合伙比例不可比您的上级玩家合伙比例高!!!").element("error_msg", "调整的合伙比例比上级玩家合伙比例高");
                        return resObj;
                    } else {
                        JSONObject updateProfitRadio = new JSONObject();
                        updateProfitRadio.put("id", circlePartner.getLong("id"));
                        updateProfitRadio.put("profit_ratio", profitRatio);
                        this.circleDao.saveOrUpdateCircleMember(updateProfitRadio);
                        resObj.element("code", 1).element("msg", "调整成功");
                        return resObj;
                    }
                }
            } else {
                resObj.element("code", 0);
                resObj.element("msg", "被操作的玩家不是合伙人!!!");
                return resObj;
            }
        } else {
            resObj.element("code", 0).element("msg", "您未在该俱乐部!!!").element("error_msg", "操作的用户没有加入这个俱乐部");
            return resObj;
        }
    }

    private JSONObject isCreateUser(JSONObject data) {
        long userId = data.getLong("uid");
        long circleId = data.getLong("circleId");
        JSONObject circleInfo = this.circleDao.getCircleInfoById(circleId);
        if (this.gameDao.getUserByID(userId) == null) {
            log.info("该用户不存在");
            data.element("code", 0).element("msg", "用户不存在").element("error_msg", "该用户没有注册");
            return data;
        } else if (circleInfo == null) {
            log.info("该俱乐部不存在");
            data.element("code", 0).element("msg", "俱乐部不存在").element("error_msg", "俱乐部不存在");
            return data;
        } else if (circleInfo.getInt("circle_status") != 2) {
            log.info("该俱乐部未审核或审核不通过");
            data.element("code", 0).element("msg", "该俱乐部未进行审核").element("error_msg", "需要进行后台审核通过的俱乐部即成功创建成功的才可以进行操作");
            return data;
        } else {
            if (circleInfo.getLong("create_user") != userId) {
                data.element("code", 0).element("msg", "你无权对俱乐部进行修改").element("error_msg", "该用户不是创建者");
            } else {
                data.element("code", 1).element("msg", "修改俱乐部成功");
            }

            return data;
        }
    }

    private long insertCircleInfo(JSONObject data) {
        long userId = data.getLong("uid");
        String platform = data.getString("platform");
        String gameCircleAuditObj = this.getSysValue(platform, "game_circle_audit");
        String freeIsOpenObj = this.getSysValue(platform, "free_is_open");
        String inningIsOpenObj = this.getSysValue(platform, "inning_is_open");
        String fundTypeObj = this.getSysValue(platform, "fund_type");
        String fundCountObj = this.getSysValue(platform, "game_circle_room_card");
        String gameCircleAudit = "".equals(gameCircleAuditObj) ? "0" : gameCircleAuditObj;
        JSONObject gameCircleInfo = new JSONObject();
        String circleName = data.getString("circleName");
        String freeIsOpen = "".equals(freeIsOpenObj) ? "Y" : freeIsOpenObj;
        String inningIsOpen = "".equals(inningIsOpenObj) ? "Y" : inningIsOpenObj;
        String fundType = "".equals(fundTypeObj) ? "2" : fundTypeObj;
        Integer fundCount = "".equals(fundCountObj) ? 0 : Integer.valueOf(fundCountObj);
        String circleNotice = data.get("circleNotice") == null ? "" : data.getString("circleNotice");
        String feeType = data.get("feeType") == null ? "2" : data.getString("feeType");
        String createRoomRole = data.get("createRoomRole") == null ? "1" : data.getString("createRoomRole");
        Integer fundPrepaidRole = data.get("fundPrepaidRole") == null ? 1 : data.getInt("fundPrepaidRole");
        Integer fundBillRole = data.get("fundBillRole") == null ? 1 : data.getInt("fundBillRole");
        int hpManagerRole = data.get("hpManagerRole") == null ? 2 : data.getInt("hpManagerRole");
        String circleStatus = gameCircleAudit.equals("1") ? "1" : "2";
        gameCircleInfo.put("circle_code", this.getCircleCode(1, 8));
        gameCircleInfo.put("circle_name", circleName);
        gameCircleInfo.put("free_is_open", freeIsOpen);
        gameCircleInfo.put("inning_is_open", inningIsOpen);
        gameCircleInfo.put("fund_type", fundType);
        gameCircleInfo.put("fund_count", fundCount);
        gameCircleInfo.put("circle_notice", circleNotice);
        gameCircleInfo.put("fee_type", feeType);
        gameCircleInfo.put("create_room_role", createRoomRole);
        gameCircleInfo.put("fund_prepaid_role", fundPrepaidRole);
        gameCircleInfo.put("fund_bill_role", fundBillRole);
        gameCircleInfo.put("hp_manager_role", hpManagerRole);
        gameCircleInfo.put("create_user", userId);
        gameCircleInfo.put("modify_user", userId);
        gameCircleInfo.put("gmt_create", TimeUtil.getNowDate());
        gameCircleInfo.put("gmt_modified", TimeUtil.getNowDate());
        gameCircleInfo.put("circle_status", circleStatus);
        gameCircleInfo.put("platform", platform);
        return this.circleDao.insertCircleInfoReturnId(gameCircleInfo);
    }

    private JSONObject getUserCondition(JSONObject data) {
        long userId = data.getLong("uid");
        String platform = data.getString("platform");
        JSONObject user = this.gameDao.getUserByID(userId);
        String hasGameCircle = this.getSysValue(platform, "has_game_circle");
        String gameCircleRoomCardObj = this.getSysValue(platform, "game_circle_room_card");
        String gameCircleCountObj = this.getSysValue(platform, "game_circle_count");
        if (!Dto.isObjNull(user) && user.containsKey("tel") && !Dto.stringIsNULL(user.getString("tel")) && Pattern.matches("^((17[0-9])|(14[0-9])|(13[0-9])|(15[^4,\\D])|(18[0-9]))\\d{8}$", user.getString("tel"))) {
        	if (!"".equals(hasGameCircle) && !"".equals(gameCircleRoomCardObj) && !"".equals(gameCircleCountObj)) {
                int hasClub = Integer.parseInt(hasGameCircle);
                Integer clubMiniRoomCard = Integer.valueOf(gameCircleRoomCardObj);
                Integer clubCount = Integer.valueOf(gameCircleCountObj);
                Integer countClubByLeader = this.circleDao.getCountCircleByUser(userId, platform);
                if (hasClub == 0) {
                    data.element("code", 0).element("msg", "未开放俱乐部功能").element("error_msg", "后台把俱乐部的功能关了");
                    return data;
                } else if (clubMiniRoomCard > user.getInt("roomcard")) {
                    data.element("code", 0).element("msg", "房卡不足").element("error_msg", "该用户的房卡不够");
                    return data;
                } else {
                    if (clubCount <= countClubByLeader) {
                        data.element("code", 0).element("msg", "创建的俱乐部已达到最大").element("error_msg", "该玩家创建俱乐部的值比后台配置亲友圈创建俱乐部最大值大");
                    } else {
                        data.element("code", 1).element("msg", "可以创建俱乐部");
                    }

                    return data;
                }
            } else {
                data.element("code", 0).element("msg", "管理员未对俱乐部系统进行配置").element("error_msg", "数据库中没有对亲友圈进行配置的数据");
                return data;
            }
        } else {
            data.element("code", 0).element("msg", "未绑定手机号").element("error_msg", "该用户没有手机号");
            return data;
        }
    }

    public String getCircleCode(int type, int len) {
        String code = MathDelUtil.getRandomStr(len);
        JSONObject info = new JSONObject();
        if (type == 1) {
            info = this.circleDao.getCircleInfoByCode(code);
        } else if (type == 2) {
            info = this.circleDao.getCircleMemberByCode(code);
        }

        return !Dto.isObjNull(info) ? this.getCircleCode(type, len) : code;
    }

    public int userJoinCircle(JSONObject object) {
        long userId = object.getLong("uid");
        String yqmSql = "select circle_id from game_circle_member where user_code=? and is_use='Y' and is_delete='N' and user_id !=?";
        JSONArray yqmJson = DBUtil.getObjectListBySQL(yqmSql, new Object[]{object.get("userCode"), object.get("uid")});
        if (null != yqmJson && yqmJson.size() != 0) {
            String sql = "select id from game_circle_audit_record where circle_id=? and user_id=? and is_delete=? and platform=? and is_deal=? and audit_type=?";
            JSONArray json = DBUtil.getObjectListBySQL(sql, new Object[]{yqmJson.getJSONObject(0).getString("circle_id"), userId, "N", object.get("platform"), "N", "1"});
            if (null != json && json.size() > 0) {
                return -2;
            } else {
                String memSql = "select id from game_circle_member where circle_id=? and user_id=? and is_delete=? and platform=?";
                JSONArray memJson = DBUtil.getObjectListBySQL(memSql, new Object[]{yqmJson.getJSONObject(0).getString("circle_id"), userId, "N", object.get("platform")});
                if (null != memJson && memJson.size() > 0) {
                    return -1;
                } else {
                    JSONObject recordObj = new JSONObject();
                    recordObj.put("circle_id", yqmJson.getJSONObject(0).getString("circle_id"));
                    recordObj.put("user_id", userId);
                    recordObj.put("superior_user_code", object.get("superior_user_code"));
                    recordObj.put("audit_type", "1");
                    recordObj.put("is_deal", "N");
                    recordObj.put("platform", object.get("platform"));
                    recordObj.put("create_user", userId);
                    recordObj.put("modify_user", userId);
                    recordObj.put("gmt_create", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
                    recordObj.put("gmt_modified", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
                    recordObj.put("is_delete", "N");
                    long recordId = this.circleDao.saveOrUpdateAuditRecord(recordObj);
                    JSONObject userObj = this.gameDao.getUserByID(userId);
                    JSONObject msgObj = new JSONObject();
                    msgObj.put("circle_id", yqmJson.getJSONObject(0).getString("circle_id"));
                    msgObj.put("user_id", userId);
                    msgObj.put("message_type", "1");
                    msgObj.put("is_read", "N");
                    msgObj.put("platform", object.get("platform"));
                    msgObj.put("gmt_create", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
                    msgObj.put("gmt_modified", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
                    msgObj.put("is_delete", "N");
                    msgObj.put("create_user", userId);
                    msgObj.put("modify_user", userId);
                    msgObj.put("msg_title", userObj.get("name") + "[" + userObj.get("account") + "]");
                    msgObj.put("msg_content", "我申请加入俱乐部，请审核！");
                    msgObj.put("msg_pid", recordId);
                    return this.circleDao.saveOrUpdateCircleMsg(msgObj);
                }
            }
        } else {
            return -1;
        }
    }

    public int userExitCircle(JSONObject object) {
        long userId = object.getLong("uid");
        String sql = "select id from game_circle_audit_record where circle_id=? and user_id=? and is_delete=? and platform=? and is_deal=? and audit_type=?";
        JSONArray json = DBUtil.getObjectListBySQL(sql, new Object[]{object.get("circle_id"), userId, "N", object.get("platform"), "N", "2"});
        if (null != json && json.size() > 0) {
            return -1;
        } else {
            JSONObject memObj = this.circleDao.getCircleMemberByUserId(userId, object.getLong("circle_id"), object.getString("platform"));
            if (null == memObj || memObj.getDouble("profit_balance") <= 0.0D && memObj.getDouble("user_hp") <= 0.0D) {
                JSONObject obj = this.circleDao.getLowerMemberCount(userId + "", object.getString("circle_id"), object.getString("platform"));
                long count = obj.getLong("total");
                if (count > 0L) {
                    return -3;
                } else {
                    JSONObject recordObj = new JSONObject();
                    recordObj.put("circle_id", object.get("circle_id"));
                    recordObj.put("user_id", userId);
                    recordObj.put("superior_user_code", object.get("superior_user_code"));
                    recordObj.put("audit_type", "2");
                    recordObj.put("is_deal", "N");
                    recordObj.put("create_user", userId);
                    recordObj.put("modify_user", userId);
                    recordObj.put("platform", object.get("platform"));
                    recordObj.put("gmt_create", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
                    recordObj.put("gmt_modified", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
                    recordObj.put("is_delete", "N");
                    long recordId = this.circleDao.saveOrUpdateAuditRecord(recordObj);
                    JSONObject userObj = this.gameDao.getUserByID(userId);
                    JSONObject msgObj = new JSONObject();
                    msgObj.put("circle_id", object.get("circle_id"));
                    msgObj.put("user_id", userId);
                    msgObj.put("message_type", "2");
                    msgObj.put("is_read", "N");
                    msgObj.put("platform", object.get("platform"));
                    msgObj.put("gmt_create", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
                    msgObj.put("gmt_modified", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
                    msgObj.put("is_delete", "N");
                    msgObj.put("create_user", userId);
                    msgObj.put("modify_user", userId);
                    msgObj.put("msg_title", userObj.get("name") + "[" + userObj.get("account") + "]");
                    msgObj.put("msg_content", "我申请退出俱乐部，请审核！");
                    msgObj.put("msg_pid", recordId);
                    return this.circleDao.saveOrUpdateCircleMsg(msgObj);
                }
            } else {
                return -2;
            }
        }
    }

    public long exitCircle(String userId, String circleId, String platform) {
        String sql = "select id,is_admin from game_circle_member where circle_id=? and user_id=? and is_delete=? and platform=?";
        JSONObject json = DBUtil.getObjectBySQL(sql, new Object[]{circleId, userId, "N", platform});
        if (null != json && json.containsKey("id") && !Dto.stringIsNULL(json.getString("id"))) {
            long memberId = json.getLong("id");
            JSONObject tempObj = this.circleDao.getCircleMemberByUserId(Long.valueOf(userId), Long.valueOf(circleId), platform);
            if (null != tempObj && (tempObj.getDouble("profit_balance") > 0.0D || tempObj.getDouble("user_hp") > 0.0D)) {
                return -2L;
            } else {
                JSONObject obj = this.circleDao.getLowerMemberCount(userId, circleId, platform);
                long count = obj.getLong("total");
                if (count > 0L) {
                    return -3L;
                } else {
                    JSONObject memObj = new JSONObject();
                    memObj.put("is_delete", "Y");
                    memObj.put("modify_user", userId);
                    memObj.put("gmt_modified", DateUtils.getTodayTime());
                    memObj.put("id", memberId);
                    return this.circleDao.saveOrUpdateCircleMember(memObj);
                }
            }
        } else {
            return -1L;
        }
    }

    public long userJoinExamCircle(JSONObject object) {
        String isAgree = object.get("is_agree") + "";
        JSONObject jsonObj;
        if ("Y".equals(isAgree)) {
            jsonObj = this.circleDao.getCircleMemberByUserId(object.getLong("user_id"), object.getLong("circle_id"), object.getString("platform"));
            if (null != jsonObj) {
                return -1L;
            }

            JSONObject superObj = this.circleDao.getSeqMemberIdsByUserCode(object.getString("superior_user_code"));
            String seqMemberIds;
            if (null != superObj && superObj.containsKey("seq_member_ids")) {
                seqMemberIds = superObj.getString("seq_member_ids");
                if (Dto.stringIsNULL(seqMemberIds)) {
                    seqMemberIds = seqMemberIds + "$";
                }

                seqMemberIds = seqMemberIds + superObj.getString("id") + "$";
            } else {
                seqMemberIds = "$" + superObj.getString("id") + "$";
            }

            JSONObject memObj = new JSONObject();
            memObj.put("circle_id", object.get("circle_id"));
            memObj.put("user_id", object.get("user_id"));
            memObj.put("superior_user_code", object.get("superior_user_code"));
            memObj.put("user_role", "3");
            memObj.put("is_admin", "0");
            memObj.put("is_use", "Y");
            memObj.put("platform", object.get("platform"));
            memObj.put("create_user", object.get("uid"));
            memObj.put("modify_user", object.get("uid"));
            memObj.put("gmt_create", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
            memObj.put("gmt_modified", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
            memObj.put("is_delete", "N");
            memObj.put("seq_member_ids", seqMemberIds);
            memObj.put("user_code", this.getCircleCode(2, 8));
            this.circleDao.saveOrUpdateCircleMember(memObj);
        }

        jsonObj = new JSONObject();
        jsonObj.put("is_deal", "Y");
        jsonObj.put("is_agree", isAgree);
        jsonObj.put("modify_user", object.get("uid"));
        jsonObj.put("gmt_modified", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
        jsonObj.put("id", object.getLong("recordId"));
        JSONObject msgObj = new JSONObject();
        msgObj.put("is_read", "Y");
        msgObj.put("id", object.getLong("msgId"));
        msgObj.put("modify_user", object.get("uid"));
        msgObj.put("gmt_modified", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
        this.circleDao.saveOrUpdateCircleMsg(msgObj);
        return this.circleDao.saveOrUpdateAuditRecord(jsonObj);
    }

    public long userExitExamCircle(JSONObject object) {
        String sql = "select id from game_circle_member where circle_id=? and user_id=? and is_delete=? and platform=?";
        JSONObject json = DBUtil.getObjectBySQL(sql, new Object[]{object.get("circle_id"), object.getString("user_id"), "N", object.get("platform")});
        if (null != json && json.containsKey("id") && !Dto.stringIsNULL(json.getString("id"))) {
            long memberId = json.getLong("id");
            String isAgree = object.get("is_agree") + "";
            JSONObject tempObj;
            JSONObject obj;
            if ("Y".equals(isAgree)) {
                tempObj = this.circleDao.getCircleMemberByUserId(object.getLong("user_id"), object.getLong("circle_id"), object.getString("platform"));
                if (null != tempObj && (tempObj.getDouble("profit_balance") > 0.0D || tempObj.getDouble("user_hp") > 0.0D)) {
                    return -2L;
                }

                obj = this.circleDao.getLowerMemberCount(object.getString("user_id"), object.getString("circle_id"), object.getString("platform"));
                long count = obj.getLong("total");
                if (count > 0L) {
                    return -3L;
                }

                JSONObject memObj = new JSONObject();
                memObj.put("is_delete", "Y");
                memObj.put("modify_user", object.get("uid"));
                memObj.put("gmt_modified", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
                memObj.put("id", memberId);
                this.circleDao.saveOrUpdateCircleMember(memObj);
            }

            tempObj = new JSONObject();
            tempObj.put("is_deal", "Y");
            tempObj.put("is_agree", isAgree);
            tempObj.put("modify_user", object.get("uid"));
            tempObj.put("gmt_modified", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
            tempObj.put("id", object.getLong("recordId"));
            obj = new JSONObject();
            obj.put("is_read", "Y");
            obj.put("id", object.getLong("msgId"));
            obj.put("modify_user", object.get("uid"));
            obj.put("gmt_modified", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
            this.circleDao.saveOrUpdateCircleMsg(obj);
            return this.circleDao.saveOrUpdateAuditRecord(tempObj);
        } else {
            return -1L;
        }
    }

    public int userBlackCircle(JSONObject object) {
        String rst = "1".equals(object.getString("type")) ? "N" : "Y";
        String sql = "update game_circle_member set is_use=?,gmt_modified=? where circle_id=? and user_id=? and is_delete=? and platform=?";
        return DBUtil.executeUpdateBySQL(sql, new Object[]{rst, DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"), object.get("circle_id"), object.getString("user_id"), "N", object.get("platform")});
    }

    public JSONObject queryFundBillDetail(String circleId, String time, int type, int pageIndex, int pageSize) {
        StringBuilder sb = new StringBuilder();
        List list = new ArrayList();
        sb.append("game_circle_fund_bill a left join za_users b on a.user_id=b.id where a.`circle_id`= ? ");
        list.add(circleId);
        if (null != time) {
            sb.append(" AND DATEDIFF(a.`gmt_create`,?)=0");
            list.add(time);
        }

        if (1 == type) {
            sb.append(" AND a.`operator_type` = 3 ");
        } else if (2 == type) {
            sb.append(" AND a.`operator_type` in (1,2) ");
        }

        sb.append(" order by a.gmt_create desc");
        return DBUtil.getObjectLimitPageBySQL("a.*,b.account,b.name", sb.toString(), new Object[]{circleId, time}, pageIndex, pageSize, CircleConstant.MAX_PAGE_COUNT);
    }

    public JSONArray queryMessageList(JSONObject object) {
        String sql = "select id,msg_title,circle_id from game_circle_message where circle_id=? and message_type=? and is_read=? and platform=? and is_delete=?";
        return DBUtil.getObjectListBySQL(sql, new Object[]{object.get("circle_id"), object.get("message_type"), "N", object.get("platform"), "N"});
    }

    public int userFundSavePay(JSONObject object) {
        int chgCount = object.getInt("change_count");
        String type = object.getString("operator_type");
        int sum = chgCount;
        String sql = "select roomcard,is_admin,user_role,name,account from za_users a join game_circle_member b on a.id=b.user_id where b.is_delete='N' and a.id=? and a.platform=? and b.circle_id=?";
        JSONObject userObj = DBUtil.getObjectBySQL(sql, new Object[]{object.get("uid"), object.getString("platform"), object.getString("circle_id")});
        if (null != userObj && userObj.containsKey("roomcard") && !Dto.stringIsNULL(userObj.getString("roomcard"))) {
            String circleSql = "select fund_count,fund_prepaid_role,create_user from game_circle_info where id=?";
            JSONObject circleObj = DBUtil.getObjectBySQL(circleSql, new Object[]{object.get("circle_id")});
            if (null != circleObj && circleObj.containsKey("fund_count") && !Dto.stringIsNULL(circleObj.getString("fund_count"))) {
                String isAdmin = userObj.getString("is_admin");
                String userRole = userObj.getString("user_role");
                String createUser = circleObj.getString("create_user");
                String fundPrepaidRole = circleObj.getString("fund_prepaid_role");
                int roomCard = Integer.parseInt(userObj.getString("roomcard"));
                double fundCount = Double.parseDouble(circleObj.getString("fund_count"));
                double afCount = 0.0D;
                String udpUserSql = "update za_users set roomcard=roomcard+? where id=?";
                String udpCircleSql = "update game_circle_info set fund_count=fund_count+? where id=?";
                if ("1".equals(type)) {
                    sum = -chgCount;
                    if (!"3".equals(fundPrepaidRole) && "3".equals(userRole) && "0".equals(isAdmin)) {
                        log.info("fundPrepaidRole：" + fundPrepaidRole + "---------userRole：" + userRole);
                        return -1;
                    }

                    if (roomCard < chgCount) {
                        log.info("---------roomCard：" + roomCard + "--------chgCount：" + chgCount);
                        return -2;
                    }

                    afCount = (double)(roomCard - chgCount);
                    DBUtil.executeUpdateBySQL(udpUserSql, new Object[]{-chgCount, object.get("uid")});
                    DBUtil.executeUpdateBySQL(udpCircleSql, new Object[]{chgCount, object.get("circle_id")});
                } else if ("2".equals(type)) {
                    if (!"1".equals(isAdmin) && !object.getString("uid").equals(createUser)) {
                        return -1;
                    }

                    if (fundCount < (double)chgCount) {
                        log.info("---------roomCard：" + roomCard + "--------chgCount：" + chgCount);
                        return -2;
                    }

                    afCount = (double)(roomCard + chgCount);
                    DBUtil.executeUpdateBySQL(udpUserSql, new Object[]{chgCount, object.get("uid")});
                    DBUtil.executeUpdateBySQL(udpCircleSql, new Object[]{-chgCount, object.get("circle_id")});
                }

                this.redisInfoService.delGameCircleInfoByid(String.valueOf(object.get("circle_id")));
                JSONObject billObj = new JSONObject();
                billObj.put("fund_change_count", -sum);
                billObj.put("circle_id", object.get("circle_id"));
                billObj.put("user_id", object.get("uid"));
                billObj.put("operator_type", object.get("operator_type"));
                billObj.put("fund_old", fundCount);
                billObj.put("platform", object.get("platform"));
                billObj.put("create_user", object.get("uid"));
                billObj.put("modify_user", object.get("uid"));
                billObj.put("gmt_create", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
                billObj.put("gmt_modified", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
                billObj.put("memo", userObj.getString("name") + " " + userObj.getString("account"));
                JSONObject userdeduObj = new JSONObject();
                userdeduObj.put("userid", object.get("uid"));
                userdeduObj.put("type", "0");
                userdeduObj.put("doType", "6");
                userdeduObj.put("pocketNew", afCount);
                userdeduObj.put("pocketOld", roomCard);
                userdeduObj.put("pocketChange", sum);
                userdeduObj.put("operatorType", "40");
                userdeduObj.put("sum", sum);
                userdeduObj.put("creataTime", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
                userdeduObj.put("operatorId", object.get("uid"));
                userdeduObj.put("reason", "俱乐部基金操作");
                userdeduObj.put("platform", object.getString("platform"));
                this.circleDao.saveOrUpdateZaDeduction(userdeduObj);
                return this.circleDao.saveOrUpdateFundBill(billObj);
            } else {
                log.info("fund_count 获取失败-------------");
                return -1;
            }
        } else {
            log.info("roomcard 获取失败-------------");
            return -1;
        }
    }

    public void savePropBill(Object userId, long circlrId, double changeCount, double oldCount, String changeType, String platForm, Object superiorUserId, String memo, Object juniorUserId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.element("user_id", userId);
        jsonObject.element("circle_id", circlrId);
        jsonObject.element("change_count", changeCount);
        jsonObject.element("old_count", oldCount);
        jsonObject.element("change_type", changeType);
        jsonObject.element("platform", platForm);
        jsonObject.element("superior_user_id", superiorUserId);
        jsonObject.element("memo", memo);
        jsonObject.element("junior_user_id", juniorUserId);
        this.saveOrUpdateUserPropBill(jsonObject);
    }

    public JSONObject getZaUserInfoById(long id) {
        return this.circleDao.getZaUserInfoById(id);
    }

    public JSONObject getCircleInfoById(long id) {
        return this.circleDao.getCircleInfoById(id);
    }

    public JSONObject checkMemberExist(long userId, long circleId) {
        return this.circleDao.checkMemberExist(userId, circleId);
    }

    public JSONObject selectByCircleIdAndUsercode(long circleId, String userCode) {
        return this.circleDao.selectByCircleIdAndUsercode(circleId, userCode);
    }

    public JSONArray getPartner(long circleId, long userId, String uuid) {
        return this.circleDao.getPartner(circleId, userId);
    }

    public void addPartner(JSONObject operatorUser) {
        this.circleDao.addPartner(operatorUser);
    }

    public JSONObject getUserPropBill(long circleId, long userId, Object[] changeType, String platform, int pageIndex, int pageSize) {
        return this.circleDao.getUserPropBill(circleId, userId, changeType, platform, pageIndex, pageSize);
    }

    public JSONArray getYestodayAndTodayPropBill(long circleId, long userId, String changeType1, String changeType2, String changeType3, String platform) {
        return this.circleDao.getYestodayAndTodayPropBill(circleId, userId, changeType1, changeType2, changeType3, platform);
    }

    public JSONObject getMemberStatistics(boolean readAll, int userRole, long circleId, long userId, int pageIndex, int pageSize) {
        return readAll ? this.circleDao.getAllMemberStatistics(userRole, circleId, pageIndex, pageSize) : this.circleDao.getMemberStatistics(userRole, circleId, userId, pageIndex, pageSize);
    }

    public JSONArray getYesterdayGameLog(long userId, String clubCode, int roomType1, int roomType2) {
        return this.circleDao.getYesterdayGameLog(userId, clubCode, roomType1, roomType2);
    }

    public JSONObject getOperatorSystemsettingByPlatform(String platform) {
        return this.circleDao.getOperatorSystemsettingByPlatform(platform);
    }

    public JSONObject getTodayHpChange(long userId, long circleId, String changeType1, String changeType2) {
        return this.circleDao.getTodayHpChange(userId, circleId, changeType1, changeType2);
    }

    public JSONObject getUserTotalPlay(long userId, long circleId, long memberId) {
        return this.circleDao.getUserTotalPlay(userId, circleId, memberId);
    }

    public JSONObject getYestodayHpFee(long userId, long circleId, String changeType) {
        return this.circleDao.getYestodayHpFee(userId, circleId, changeType);
    }

    public JSONObject getYestodayPalyNum(long userId, String clubCode, int roomType1, int roomType2) {
        return this.circleDao.getYestodayPalyNum(userId, clubCode, roomType1, roomType2);
    }

    public JSONObject getMyCircleStatistics(long userId, long circleId, int userRole) {
        return this.circleDao.getMyCircleStatistics(userId, circleId, userRole);
    }

    public JSONObject getUserByAccount(String account, long circleId, int userRole, String platform) {
        return this.circleDao.getUserByAccount(account, circleId, userRole, platform);
    }

    public void transferProfitBalanceToHp(JSONObject jsonObject) {
        this.circleDao.transferProfitBalanceToHp(jsonObject);
    }

    public JSONObject getMemberStatisticsPage(boolean readAll, long circleId, String userCode, int userRole, long userAccount, int pageIndex, int pageSize) {
        if (userAccount != 0L) {
            return this.circleDao.getOneMemberStatisticsPage(circleId, userRole, userAccount, pageIndex, pageSize);
        } else {
            return readAll ? this.circleDao.getAllMemberStatisticsPage(circleId, userRole, pageIndex, pageSize) : this.circleDao.getMemberStatisticsPage(circleId, userCode, userRole, pageIndex, pageSize);
        }
    }

    public JSONObject getYestodayPaly(boolean readAll, long circleId, int userRole, String userCode) {
        return readAll ? this.circleDao.getAllYestodayPaly(circleId, circleId, userRole) : this.circleDao.getYestodayPaly(circleId, circleId, userRole, userCode);
    }

    public JSONObject getSuperUserDetailMessageByCode(String userCode) {
        return this.circleDao.getSuperUserDetailMessageByCode(userCode);
    }

    public JSONObject getMemberInfoPage(String circleId, String userRole, String userCode, String type, String circleCode, List userIds, int[] roomType, long userAccount, String condition, String time, String orderByTotalScore, int pageIndex, int pageSize) {
        return this.circleDao.getMemberInfoPage(circleId, userRole, userCode, type, circleCode, userIds, roomType, userAccount, condition, time, orderByTotalScore, pageIndex, pageSize);
    }

    public JSONObject getMemberInfo(String circleId, String circleCode, long userId, String time) {
        return this.circleDao.getMemberInfo(circleId, circleCode, userId, time);
    }

    public JSONArray queryMbrExamList(String circleId, String auditType, String platform) {
        return this.circleDao.queryMbrExamList(circleId, auditType, platform);
    }

    public JSONArray queryMsgExamList(String circleId, String userId, String platform) {
        JSONObject circleMemberInfo = this.circleDao.getCircleMemberByUserId(Long.valueOf(userId), Long.valueOf(circleId), platform);
        String userRole = circleMemberInfo.containsKey("user_role") ? circleMemberInfo.getString("user_role") : "3";
        return "2".equals(userRole) ? this.circleDao.queryMsgPartnerExamList(circleId, userId, platform) : this.circleDao.queryMsgExamList(circleId, userId, platform);
    }

    public int saveOrUpdateUserPropBill(JSONObject jsonObject) {
        return this.circleDao.saveOrUpdateUserPropBill(jsonObject);
    }

    public long addCircleMember(JSONObject object) {
        String account = object.getString("account");
        String circleId = object.getString("circle_id");
        String superiorUserCode = object.getString("superior_user_code");
        String platform = object.getString("platform");
        String userId = object.getString("uid");
        String memberSqlTemp = "select * from game_circle_member where circle_id=? and user_id=? and platform=? and is_delete=?";
        JSONObject memberObjTemp = DBUtil.getObjectBySQL(memberSqlTemp, new Object[]{circleId, userId, platform, "N"});
        String userRole = memberObjTemp.containsKey("user_role") ? memberObjTemp.getString("user_role") : null;
        String adminRole = memberObjTemp.containsKey("is_admin") ? memberObjTemp.getString("is_admin") : null;
        if ("3".equals(userRole) && "1".equals(adminRole)) {
            superiorUserCode = memberObjTemp.containsKey("superior_user_code") ? memberObjTemp.getString("superior_user_code") : superiorUserCode;
        }

        String userSql = "select id from za_users where account=? and platform=?";
        JSONObject userObj = DBUtil.getObjectBySQL(userSql, new Object[]{account, platform});
        if (null != userObj && userObj.containsKey("id") && !Dto.stringIsNULL(userObj.getString("id"))) {
            String memberSql = "select id from game_circle_member where circle_id=? and user_id=? and platform=? and is_delete=?";
            JSONObject memberObj = DBUtil.getObjectBySQL(memberSql, new Object[]{circleId, userObj.getString("id"), platform, "N"});
            if (null != memberObj) {
                return -2L;
            } else {
                JSONObject superObj = this.circleDao.getSeqMemberIdsByUserCode(superiorUserCode);
                String seqMemberIds;
                if (null != superObj && superObj.containsKey("seq_member_ids")) {
                    seqMemberIds = superObj.getString("seq_member_ids");
                    if (Dto.stringIsNULL(seqMemberIds)) {
                        seqMemberIds = seqMemberIds + "$";
                    }

                    seqMemberIds = seqMemberIds + superObj.getString("id") + "$";
                } else {
                    seqMemberIds = "$" + superObj.getString("id") + "$";
                }

                JSONObject memObj = new JSONObject();
                memObj.put("circle_id", circleId);
                memObj.put("user_id", userObj.getString("id"));
                memObj.put("superior_user_code", superiorUserCode);
                memObj.put("user_role", "3");
                memObj.put("is_admin", "0");
                memObj.put("is_use", "Y");
                memObj.put("platform", platform);
                memObj.put("create_user", object.get("uid"));
                memObj.put("modify_user", object.get("uid"));
                memObj.put("gmt_create", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
                memObj.put("gmt_modified", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
                memObj.put("is_delete", "N");
                memObj.put("seq_member_ids", seqMemberIds);
                memObj.put("user_code", this.getCircleCode(2, 8));
                return this.circleDao.saveOrUpdateCircleMember(memObj);
            }
        } else {
            return -1L;
        }
    }

    public JSONObject getCircleMemberByUserId(JSONObject object) {
        return this.circleDao.getCircleMemberByUserId(object.getLong("user_id"), object.getLong("circle_id"), object.getString("platform"));
    }

    public JSONObject getCircleMemberByUserId(long userId, long circleId, String platform) {
        return this.circleDao.getCircleMemberByUserId(userId, circleId, platform);
    }

    public JSONObject getLowerMemberCount(JSONObject object) {
        return this.circleDao.getLowerMemberCount(object.getString("user_id"), object.getString("circle_id"), object.getString("platform"));
    }

    public long circleDelMember(JSONObject object) {
        String sql = "select id from game_circle_member where circle_id=? and user_id=? and is_delete=? and platform=?";
        JSONObject json = DBUtil.getObjectBySQL(sql, new Object[]{object.get("circle_id"), object.getString("user_id"), "N", object.get("platform")});
        if (null != json && json.containsKey("id") && !Dto.stringIsNULL(json.getString("id"))) {
            JSONObject obj = this.circleDao.getLowerMemberCount(object.getString("user_id"), object.getString("circle_id"), object.getString("platform"));
            long count = obj.getLong("total");
            if (count > 0L) {
                return -2L;
            } else {
                JSONObject operatorObj = this.circleDao.checkMemberExist(object.getLong("uid"), object.getLong("circle_id"));
                JSONObject entityObj = this.circleDao.getCircleMemberByUserId(object.getLong("user_id"), object.getLong("circle_id"), object.getString("platform"));
                String superCode = entityObj.containsKey("superior_user_code") ? entityObj.getString("superior_user_code") : null;
                if (operatorObj.containsKey("user_code")) {
                    operatorObj.getString("user_code");
                } else {
                    Object var10000 = null;
                }

                String seqUserIds = entityObj.containsKey("seq_member_ids") ? entityObj.getString("seq_member_ids") : "";
                String superId = operatorObj.getString("id");
                boolean isUpRelation = superCode != null && seqUserIds.contains(superId);
                if (entityObj.getDouble("profit_balance") > 0.0D) {
                    return -3L;
                } else {
                    long memberId = json.getLong("id");
                    long rst = -1L;
                    String type = object.getString("type");
                    String userRole;
                    String adminRole;
                    if ("1".equals(type)) {
                        if (entityObj.getDouble("user_hp") != 0.0D) {
                            return -4L;
                        }

                        userRole = operatorObj.containsKey("user_role") ? operatorObj.getString("user_role") : "3";
                        adminRole = operatorObj.containsKey("is_admin") ? operatorObj.getString("is_admin") : "0";
                        if ("3".equals(userRole)) {
                            if ("0".equals(adminRole)) {
                                return -5L;
                            }
                        } else if ("2".equals(userRole) && !isUpRelation) {
                            return -5L;
                        }

                        JSONObject memObj = new JSONObject();
                        memObj.put("is_delete", "Y");
                        memObj.put("modify_user", object.get("uid"));
                        memObj.put("gmt_modified", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
                        memObj.put("id", memberId);
                        rst = this.circleDao.saveOrUpdateCircleMember(memObj);
                    } else if ("2".equals(type)) {
                        JSONObject memObj = new JSONObject();
                        memObj.put("user_role", "3");
                        memObj.put("profit_ratio", "0");
                        memObj.put("modify_user", object.get("uid"));
                        memObj.put("gmt_modified", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
                        memObj.put("id", memberId);
                        rst = this.circleDao.saveOrUpdateCircleMember(memObj);
                    }

                    userRole = "update game_circle_message set is_read='Y',modify_user=? where circle_id=? and user_id=? and platform=?";
                    DBUtil.executeUpdateBySQL(userRole, new Object[]{object.get("uid"), object.get("circle_id"), object.getString("user_id"), object.get("platform")});
                    adminRole = "update game_circle_audit_record set is_deal='Y',modify_user=? where circle_id=? and user_id=? and platform=?";
                    DBUtil.executeUpdateBySQL(adminRole, new Object[]{object.get("uid"), object.get("circle_id"), object.getString("user_id"), object.get("platform")});
                    return rst;
                }
            }
        } else {
            return -1L;
        }
    }

    public String getSysValue(String platform, String key) {
        JSONObject object = this.redisInfoService.getSysGlobalByKey(key, platform);
        return object != null && object.get("global_value") != null ? object.getString("global_value") : "";
    }

    public JSONObject getSuperUserByCode(String userCode) {
        return this.circleDao.getSuperUserByCode(userCode);
    }

    public JSONArray getSysGlobalEvent(JSONObject object) {
        return this.circleDao.getSysGlobalEvent(object);
    }

    public int getHpRole(long circleId) {
        JSONObject circleInfo = this.circleDao.getCircleInfoById(circleId);
        return circleInfo.get("hp_manager_role") == null ? 1 : circleInfo.getInt("hp_manager_role");
    }

    public JSONArray getMemberArray(JSONArray memberList, JSONObject user, long circleId) {
        JSONArray newData = new JSONArray();
        int hpRole = this.getHpRole(circleId);
        Iterator var9 = memberList.iterator();

        while(var9.hasNext()) {
            Object aMemberList = var9.next();
            JSONObject member = JSONObject.fromObject(aMemberList);
            long partnerId = member.getLong("user_id");
            JSONObject todayHpChange = this.getTodayHpChange(partnerId, circleId, "2", "3");
            boolean isOnline = this.redisInfoService.isOnline(String.valueOf(partnerId));
            boolean isUnder = false;
            member.element("isOnline", isOnline);
            member.element("todayHpChangeNum", todayHpChange.get("change_count"));
            if (hpRole == 1) {
                if (member.getString("superior_user_code").equals(user.getString("user_code"))) {
                    isUnder = true;
                }
            } else if (hpRole == 0 && "1".equals(user.getString("user_role"))) {
                isUnder = true;
            }

            member.element("isUnder", isUnder);
            newData.add(member);
        }

        return newData;
    }

    public void insertZaUserGameStatis(JSONObject object) {
        if (object.containsKey("array")) {
            JSONArray data = object.getJSONArray("array");
            if (null != data && data.size() != 0) {
                ArrayList<String> columnList = new ArrayList();
                JSONObject jsonObject = data.getJSONObject(0);
                Iterator it = jsonObject.keys();

                while(it.hasNext()) {
                    String key = (String)it.next();
                    columnList.add(key);
                }

                long sole = Long.valueOf("" + System.currentTimeMillis() + jsonObject.get("room_no"));
                columnList.add("sole");
                List<Object[]> insertList = new ArrayList();

                for(int i = 0; i < data.size(); ++i) {
                    JSONObject o = data.getJSONObject(i);
                    if (null != o) {
                        ArrayList list = new ArrayList();
                        boolean b = true;

                        for(int j = 0; j < columnList.size(); ++j) {
                            if ("sole".equals(columnList.get(j))) {
                                list.add(sole);
                            } else {
                                if (o.get((String)columnList.get(j)) == null) {
                                    b = false;
                                    break;
                                }

                                list.add(o.get((String)columnList.get(j)));
                            }
                        }

                        if (b) {
                            insertList.add(list.toArray(new Object[]{list.size()}));
                        }
                    }
                }

                if (columnList != null && columnList.size() > 0) {
                    BaseSqlUtil.insertData("za_user_game_statis", (String[])columnList.toArray(new String[columnList.size()]), insertList);
                }

                if (object.containsKey("attachInfo")) {
                    JSONObject attachObject = object.getJSONObject("attachInfo");
                    if (null != attachObject && attachObject.containsKey("gameId") && attachObject.containsKey("extraInfo")) {
                        BaseSqlUtil.insertData("za_game_log_attach", new String[]{"sole_id", "game_id", "extra_info"}, new Object[]{sole, attachObject.get("gameId"), attachObject.get("extraInfo")});
                    }
                }

            }
        }
    }

    public JSONArray getCircleMemberLowerCount(String circleId) {
        return this.circleDao.getCircleMemberByCircleId(Long.valueOf(circleId));
    }

    public JSONObject getCircleMemberSuper(Object circleId, Object userId) {
        return this.circleDao.getCircleMemberInfo(circleId, userId);
    }

    public boolean isSuperiorRelation(JSONObject upperMember, JSONObject lowerMember) {
        String upperRole = upperMember.containsKey("user_role") ? upperMember.getString("user_role") : "3";
        String lowerRole = lowerMember.containsKey("user_role") ? lowerMember.getString("user_role") : "3";
        if ("3".equals(upperRole)) {
            return false;
        } else if (!"2".equals(lowerRole)) {
            return false;
        } else {
            String upperUserId = upperMember.containsKey("id") ? upperMember.getString("id") : null;
            String upperUserIds = lowerMember.containsKey("seq_member_ids") ? lowerMember.getString("seq_member_ids") : "";
            return upperUserIds.contains("$" + upperUserId + "$");
        }
    }

    public void resetPartner(Long userMemberId, long circleId, long upperId, long lowerId, String platform) {
        JSONArray lowerList = this.circleDao.getLowerMemberList(userMemberId, circleId, platform);
        long[] lowerIdList = new long[lowerList.size() + 1];
        lowerIdList[lowerList.size()] = userMemberId;

        for(int i = 0; i < lowerList.size(); ++i) {
            JSONObject memberInfo = lowerList.getJSONObject(i);
            long id = memberInfo.containsKey("id") ? memberInfo.getLong("id") : -1L;
            long userId = memberInfo.containsKey("user_id") ? memberInfo.getLong("user_id") : -1L;
            lowerIdList[i] = id;
            JSONObject auditRecord = this.getCircleRecord(circleId, platform, userId, upperId, "6");
            long msgId = this.circleDao.saveOrUpdateAuditRecord(auditRecord);
            JSONObject circleMessage = this.getCircleMessage(circleId, platform, userId, upperId, msgId, "6", "合伙人重置", "您的合伙比例已被重置");
            this.circleDao.saveOrUpdateCircleMsg(circleMessage);
        }

        JSONObject auditRecord = this.getCircleRecord(circleId, platform, lowerId, upperId, "6");
        long msgId = this.circleDao.saveOrUpdateAuditRecord(auditRecord);
        JSONObject circleMessage = this.getCircleMessage(circleId, platform, lowerId, upperId, msgId, "6", "合伙人重置", "您的合伙比例已被重置");
        this.circleDao.saveOrUpdateCircleMsg(circleMessage);
        this.circleDao.resetPartner(lowerIdList);
    }

    public void updateMessage(long msgId) {
        this.circleDao.updateMessage(msgId);
    }

    public JSONObject getMasterMemberInfo(long circleId) {
        return this.circleDao.queryMasterMemberInfo(circleId);
    }

    public JSONObject getMemberPage(int type, Object circleId, Object userAccount, Object superUserId, String condition, String userCode, String orderByTotalScore, String circleCode, String time, int[] roomType, int pageIndex, int pageSize) {
        StringBuilder sb = new StringBuilder();
        sb.append(" (SELECT z.`account`,z.`name`,z.`headimg`,g.`is_use`,g.`user_id`,g.`user_role`,g.`is_admin`,g.`id`,g.`profit_ratio`,g.`profit_balance_all`,g.`superior_user_code`,g.`user_code`,g.`user_hp`,IF(f.`totalNumber`IS NULL,0,f.`totalNumber`)AS totalNumber,IF(f.`totalScore`IS NULL,0,f.`totalScore`)AS totalScore,IF(f.`bigWinnerCount`IS NULL,0,f.`bigWinnerCount`)AS bigWinnerCount,IF(f.`bigWinnerScore`IS NULL,0,f.`bigWinnerScore`)AS bigWinnerScore FROM game_circle_member g LEFT JOIN za_users z ON g.`user_id`=z.`id`  LEFT JOIN(SELECT COUNT(*)AS totalNumber,l.`user_id`,SUM(l.`score`)AS totalScore,SUM(IF(l.`is_big_winner`=0,0,1))AS bigWinnerCount,SUM(IF(l.`is_big_winner`=0,0,l.`score`))AS bigWinnerScore FROM za_user_game_statis l WHERE ");
        sb.append(" l.`circle_code`= ? AND (DATEDIFF(l.`create_time`,?)=0)  ");
        List list = new ArrayList();
        list.add(circleCode);
        list.add(time);
        if (null != roomType && roomType.length > 0) {
            sb.append(" AND l.`room_type` in(");

            for(int i = 0; i < roomType.length; ++i) {
                sb.append("?");
                list.add(roomType[i]);
                if (i != roomType.length - 1) {
                    sb.append(",");
                }
            }

            sb.append(") ");
        }

        sb.append(" GROUP BY l.`user_id`)f ON g.`user_id`=f.`user_id` WHERE g.`circle_id`=? AND g.`is_delete`=? ");
        list.add(circleId);
        list.add("N");
        if (1 != type && 2 == type) {
            sb.append(" AND g.`user_role`=? ");
            list.add("2");
        }
        if (null != userAccount) {
            sb.append(" AND z.`account`= ?");
            list.add(userAccount);
        }

        if (null != superUserId) {
            sb.append(" AND g.`seq_member_ids`LIKE '%$").append(superUserId).append("$%'");
        }
        sb.append(" AND z.`isShow`= ?");
        list.add(0);

        if ("1".equals(condition)) {
            sb.append(" AND  g.`superior_user_code` = ? ");
            list.add(userCode);
        } else if ("2".equals(condition)) {
            sb.append(" AND  g.`superior_user_code` != ? ");
            list.add(userCode);
        } else if (type == 1) {
            sb.append(" OR g.id = ? ");
            list.add(superUserId);
            if (userAccount != null) {
                sb.append(" AND z.account = ? ");
                list.add(userAccount);
            }
        }

        if (1 == type) {
            sb.append(" ORDER BY f.`totalScore` ").append(orderByTotalScore);
        } else if (2 == type) {
            sb.append(" ORDER BY g.`profit_balance_all` ").append(orderByTotalScore);
        }

        sb.append(" ,g.id )f");
        return DBUtil.getObjectPageBySQL("*", sb.toString(), list.toArray(), pageIndex, pageSize);
    }

    public JSONObject getMemberPropBillPage(Object circleId, Object userId, Object[] changeType, String time, int pageIndex, int pageSize) {
        StringBuilder sb = new StringBuilder();
        sb.append(" game_circle_prop_bill z WHERE 1=1");
        List list = new ArrayList();
        if (null != circleId) {
            sb.append(" AND z.`circle_id`= ? ");
            list.add(circleId);
        }

        if (null != userId) {
            sb.append(" AND z.`user_id`= ? ");
            list.add(userId);
        }

        if (null != changeType && changeType.length > 0) {
            sb.append(" AND z.`change_type` in(");

            for(int i = 0; i < changeType.length; ++i) {
                sb.append("?");
                list.add(changeType[i]);
                if (i != changeType.length - 1) {
                    sb.append(",");
                }
            }

            sb.append(") ");
        }

        sb.append(" AND TO_DAYS(NOW()) - TO_DAYS(z.`create_time`) < 4 ");
        sb.append("  ORDER BY z.`create_time` DESC");
        return DBUtil.getObjectPageBySQL("*", sb.toString(), list.toArray(), pageIndex, pageSize);
    }

    public JSONArray getMemberPropBillAll(Object circleId, Object userId, Object[] changeType) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT SUM(z.`change_count`) AS changeCount,DATE_FORMAT(z.`create_time`, '%Y-%m-%d') createTime  FROM game_circle_prop_bill z WHERE 1=1");
        List list = new ArrayList();
        if (null != circleId) {
            sb.append(" AND z.`circle_id`= ? ");
            list.add(circleId);
        }

        if (null != userId) {
            sb.append(" AND z.`user_id`= ? ");
            list.add(userId);
        }

        if (null != changeType && changeType.length > 0) {
            sb.append(" AND z.`change_type` in(");

            for(int i = 0; i < changeType.length; ++i) {
                sb.append("?");
                list.add(changeType[i]);
                if (i != changeType.length - 1) {
                    sb.append(",");
                }
            }

            sb.append(") ");
        }

        sb.append(" AND TO_DAYS(NOW()) - TO_DAYS(z.`create_time`) < 2 GROUP BY DATE_FORMAT(z.`create_time`, '%Y-%m-%d') ");
        return DBUtil.getObjectListBySQL(sb.toString(), list.toArray());
    }

    public int addBalanceReviewMessage(long circleId, long userId, String platform, double profitBalance) {
        JSONObject userInfo = DBUtil.getObjectBySQL("SELECT * FROM za_users WHERE id = ?", new Object[]{userId});
        if (userInfo != null && userInfo.containsKey("account")) {
            String messageContent = "合伙人[" + userInfo.getString("account") + "]提取" + profitBalance + "佣金";
            String messageTitle = "提取佣金审核";
            JSONObject record = this.getCircleRecord(circleId, platform, userId, userId, "7");
            record.put("balance_number", profitBalance);
            long pid = this.circleDao.saveOrUpdateAuditRecord(record);
            if (pid != 0L) {
                JSONObject circleMessage = this.getCircleMessage(circleId, platform, userId, userId, pid, "7", messageTitle, messageContent);
                return this.circleDao.saveOrUpdateCircleMsg(circleMessage);
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    public long userExtractBalance(JSONObject object) {
        String sql = "select id from game_circle_member where circle_id=? and user_id=? and is_delete=? and platform=?";
        JSONObject json = DBUtil.getObjectBySQL(sql, new Object[]{object.get("circle_id"), object.getString("user_id"), "N", object.get("platform")});
        if (null != json && json.containsKey("id") && !Dto.stringIsNULL(json.getString("id"))) {
            String isAgree = object.get("is_agree") + "";
            long recordId = object.getLong("recordId");
            long userId = object.getLong("user_id");
            long circleId = object.getLong("circle_id");
            String platform = object.getString("platform");
            JSONObject recordInfo = DBUtil.getObjectBySQL("select * from game_circle_audit_record where id = ?", new Object[]{recordId});
            if (recordInfo == null) {
                return -1L;
            } else {
                double profitBalance = recordInfo.containsKey("balance_number") ? recordInfo.getDouble("balance_number") : 0.0D;
                long operatorUserId = recordInfo.containsKey("user_id") ? recordInfo.getLong("user_id") : 0L;
                JSONObject operatorMember = DBUtil.getObjectBySQL("SELECT * FROM game_circle_member WHERE circle_id = ? AND is_delete = ? AND user_id = ? AND platform = ?", new Object[]{circleId, "N", operatorUserId, platform});
                if (operatorMember == null) {
                    return -2L;
                } else {
                    JSONObject recordObj = new JSONObject();
                    recordObj.put("is_deal", "Y");
                    recordObj.put("is_agree", isAgree);
                    recordObj.put("modify_user", object.get("uid"));
                    recordObj.put("gmt_modified", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
                    recordObj.put("id", object.getLong("recordId"));
                    JSONObject msgObj = new JSONObject();
                    msgObj.put("is_read", "Y");
                    msgObj.put("id", object.getLong("msgId"));
                    msgObj.put("modify_user", object.get("uid"));
                    msgObj.put("gmt_modified", DateUtils.getStringTime("yyyy-MM-dd HH:mm:ss"));
                    JSONObject auditRecord = this.getCircleRecord(circleId, platform, userId, userId, "8");
                    auditRecord.put("is_agree", isAgree);
                    auditRecord.put("balance_number", profitBalance);
                    long msgId = this.circleDao.saveOrUpdateAuditRecord(auditRecord);
                    String title = "佣金提取审核";
                    String content;
                    if ("Y".equals(isAgree)) {
                        content = "管理员同意了您提取佣金" + Math.abs(profitBalance) + "的申请";
                    } else {
                        content = "管理员拒绝了您提取佣金" + Math.abs(profitBalance) + "的申请";
                    }

                    JSONObject circleMessage = this.getCircleMessage(circleId, platform, userId, userId, msgId, "8", title, content);
                    this.circleDao.saveOrUpdateCircleMsg(circleMessage);
                    this.circleDao.saveOrUpdateCircleMsg(msgObj);
                    return this.circleDao.saveOrUpdateAuditRecord(recordObj);
                }
            }
        } else {
            return -1L;
        }
    }

    public int extractBalanceMessageHandle(long recordId, String platform) {
        JSONObject gameCircleRecord = DBUtil.getObjectBySQL("SELECT * FROM game_circle_audit_record WHERE id = ?", new Object[]{recordId});
        if (gameCircleRecord == null) {
            return -1;
        } else {
            long circleId = gameCircleRecord.containsKey("circle_id") ? gameCircleRecord.getLong("circle_id") : -1L;
            long userId = gameCircleRecord.containsKey("user_id") ? gameCircleRecord.getLong("user_id") : -1L;
            String isAgree = gameCircleRecord.containsKey("is_agree") ? gameCircleRecord.getString("is_agree") : null;
            double profitBalance = gameCircleRecord.containsKey("balance_number") ? gameCircleRecord.getDouble("balance_number") : 0.0D;
            JSONObject memberInfo = DBUtil.getObjectBySQL("SELECT * FROM game_circle_member a WHERE a.`circle_id` = ? AND a.`user_id` = ? AND a.`is_delete` = ?", new Object[]{circleId, userId, "N"});
            if (memberInfo == null) {
                return -1;
            } else {
                long memberId = memberInfo.getLong("id");
                double userHp = memberInfo.getDouble("user_hp");
                double balance = memberInfo.getDouble("profit_balance");
                JSONObject updateMember;
                if ("Y".equals(isAgree)) {
                    updateMember = new JSONObject();
                    updateMember.put("id", memberId);
                    updateMember.put("user_hp", Dto.add(userHp, profitBalance));
                    this.circleDao.transferProfitBalanceToHp(updateMember);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.element("user_id", userId);
                    jsonObject.element("circle_id", circleId);
                    jsonObject.element("change_count", profitBalance);
                    jsonObject.element("old_count", userHp);
                    jsonObject.element("change_type", "4");
                    jsonObject.element("platform", platform);
                    jsonObject.element("create_time", TimeUtil.getNowDate());
                    jsonObject.element("is_delete", 0);
                    jsonObject.element("fenrun_id", userId);
                    jsonObject.element("superior_user_id", userId);
                    jsonObject.element("memo", "消耗提取");
                    jsonObject.element("junior_user_id", userId);
                    this.circleDao.saveOrUpdateUserPropBill(jsonObject);
                } else {
                    if (!"N".equals(isAgree)) {
                        return -1;
                    }

                    updateMember = new JSONObject();
                    updateMember.put("id", memberId);
                    updateMember.put("profit_balance", Dto.add(balance, profitBalance));
                    this.circleDao.transferProfitBalanceToHp(updateMember);
                }

                return 1;
            }
        }
    }

	@Override
	public JSONObject getBankMsgByCircle(String password, int circleId,String userId) {
		JSONObject circleBank = DBUtil.getObjectBySQL("SELECT * FROM za_bank WHERE circle_id = ? and user_id=?", new Object[]{circleId,userId});
		return circleBank;
	}

	@Override
	public void insertZaBank(JSONObject bank) {
		circleDao.saveOrUpdateZaBank(bank);
		
	}

	@Override
	public int operationMoney(int circleId, String userId, int money) {
		return circleDao.updateBankBalance(circleId, userId, money);
	}

	@Override
	public JSONObject getCircleMemberByUserId(String userId,int circleId) {
		JSONObject Member = DBUtil.getObjectBySQL("SELECT * FROM game_circle_member WHERE user_id = ? and circle_id=?", new Object[]{userId,circleId});
		return Member;
	}

	@Override
	public JSONObject getLuckyDrawById(int id) {
		
		return circleDao.getLuckyDrawById(id);
	}

	@Override
	public JSONObject getBoos(int circleId) {
		return circleDao.getBoos(circleId);
	}

	

	
	
	
}
