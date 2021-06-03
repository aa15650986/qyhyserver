
package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.FundBiz;
import com.zhuoan.biz.game.dao.FundDao;
import javax.annotation.Resource;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class FundBizImpl implements FundBiz {
    @Resource
    private FundDao fundDao;

    public FundBizImpl() {
    }

    public void updateUserScoreByOpenId(String openId, double score) {
        this.fundDao.updateUserScoreByOpenId(openId, score);
    }

    public JSONObject getSysUsers() {
        return this.fundDao.getSysUsers();
    }

    public void updateSysUserStatusByAccount(String account, int status) {
        this.fundDao.updateSysUserStatusByAccount(account, status);
    }

    public JSONObject getVersionInfo() {
        return this.fundDao.getVersionInfo();
    }

    public JSONObject getUserInfoByOpenId(String openId) {
        return this.fundDao.getUserInfoByOpenId(openId);
    }

    public JSONObject getUserInfoByAccount(String account) {
        return this.fundDao.getUserInfoByAccount(account);
    }

    public void insertUserInfo(String account, JSONObject obj, String platform) {
        this.fundDao.insertUserInfo(account, obj, platform);
    }
}
