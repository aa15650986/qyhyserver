package com.zhuoan.biz.game.dao;

import net.sf.json.JSONObject;

public interface ZaNewSignDao {
	JSONObject getZaNewSignMsgByUserId(int userId);
	
	int saveOrUpdateSign(JSONObject zaUserSign);

	JSONObject getSignSetting();
	
	int signUpdateUserRoomCard(int count,int id);
}
