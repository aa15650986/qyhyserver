package com.zhuoan.biz.game.biz;

import net.sf.json.JSONObject;

public interface ZaNewSignBiz {
	
	JSONObject getZaNewSignMsgByUserId(int userId);
	
	int insertZaNewSign(JSONObject jsonObject);
	
	JSONObject getSignSetting();
	
	int updateUserRoomcardBySign(int count,int id);

}
