package com.zhuoan.biz.game.biz.impl;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.zhuoan.biz.game.biz.ZaNewSignBiz;
import com.zhuoan.biz.game.dao.ZaNewSignDao;

import net.sf.json.JSONObject;

@Service
public class ZaUserSignBizImpl implements ZaNewSignBiz{
	
	@Resource
	private ZaNewSignDao zaNewSignDao;

	@Override
	public JSONObject getZaNewSignMsgByUserId(int userId) {
		return zaNewSignDao.getZaNewSignMsgByUserId(userId);
	}

	@Override
	public int insertZaNewSign(JSONObject jsonObject) {
		return zaNewSignDao.saveOrUpdateSign(jsonObject);
	}

	@Override
	public JSONObject getSignSetting() {
		return zaNewSignDao.getSignSetting();
	}

	@Override
	public int updateUserRoomcardBySign(int count,int id) {
		
		return zaNewSignDao.signUpdateUserRoomCard(count,id);
	}

}
