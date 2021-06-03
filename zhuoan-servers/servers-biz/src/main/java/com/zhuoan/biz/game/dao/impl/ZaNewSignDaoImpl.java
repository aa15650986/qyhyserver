package com.zhuoan.biz.game.dao.impl;

import org.springframework.stereotype.Component;

import com.zhuoan.biz.game.dao.ZaNewSignDao;
import com.zhuoan.dao.DBJsonUtil;
import com.zhuoan.dao.DBUtil;

import net.sf.json.JSONObject;
@Component
public class ZaNewSignDaoImpl implements ZaNewSignDao{

	@Override
	public int saveOrUpdateSign(JSONObject zaUserSign) {
		// 有id为update  无id为save
		return DBJsonUtil.saveOrUpdate(zaUserSign, "za_new_sign");
	}

	@Override
	public JSONObject getZaNewSignMsgByUserId(int userId) {
		String sql = "select * from za_new_sign where user_id = ?";
		JSONObject userSign = DBUtil.getObjectBySQL(sql, new Object[]{userId});
		return userSign;
	}

	@Override
	public JSONObject getSignSetting() {
		String sql = "select * from za_new_sign_setting where id = ?";
		JSONObject signSetting = DBUtil.getObjectBySQL(sql, new Object[]{1});
		return signSetting;
	}

	@Override
	public int signUpdateUserRoomCard(int count,int id) {
		String sql = "update za_users set roomcard = roomcard+? where id = ?";
		return DBUtil.executeUpdateBySQL(sql, new Object[]{count,id});
	}

}
