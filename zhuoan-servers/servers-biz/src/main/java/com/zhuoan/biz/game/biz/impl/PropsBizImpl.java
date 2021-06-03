

package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.PropsBiz;
import com.zhuoan.biz.game.dao.PropsDao;
import javax.annotation.Resource;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class PropsBizImpl implements PropsBiz {
    @Resource
    private PropsDao propsDao;

    public PropsBizImpl() {
    }

    public JSONArray getPropsInfoByPlatform(String platform) {
        return this.propsDao.getPropsInfoByPlatform(platform);
    }

    public JSONObject getPropsInfoById(long propsId) {
        return this.propsDao.getPropsInfoById(propsId);
    }

    public JSONObject getUserPropsByType(String account, int propsType) {
        return this.propsDao.getUserPropsByType(account, propsType);
    }

    public void addOrUpdateUserProps(JSONObject userProps) {
        this.propsDao.addOrUpdateUserProps(userProps);
    }

    public void updateUserPropsCount(String account, int propsType, int sum) {
        this.propsDao.updateUserPropsCount(account, propsType, sum);
    }

    public JSONArray getUserPropsByAccount(String account) {
        return this.propsDao.getUserPropsByAccount(account);
    }
}
