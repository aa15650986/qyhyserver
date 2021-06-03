
package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.game.dao.GameDao;
import javax.annotation.Resource;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class UserBizImpl implements UserBiz {
    @Resource
    private GameDao gameDao;

    public UserBizImpl() {
    }

    public JSONObject getUserByID(long id) {
        return this.gameDao.getUserByID(id);
    }

    public JSONObject getUserByAccount(String account) {
        return this.gameDao.getUserByAccount(account);
    }

    public JSONObject checkUUID(String account, String uuid) {
        JSONObject user = this.gameDao.getUserByAccount(account);
        JSONObject jsonObject = new JSONObject();
        String msg = "";
        String code;
        if (user != null) {
            String userUuid = user.getString("uuid");
            if (uuid.equals(userUuid)) {
                code = "1";
            } else {
                msg = "该帐号已在其他地方登录";
                code = "0";
            }
        } else {
            msg = "用户不存在";
            code = "0";
        }

        jsonObject.put("msg", msg);
        jsonObject.put("data", user);
        jsonObject.put("code", code);
        return jsonObject;
    }

    public boolean updateUserBalance(JSONArray data, String types) {
        return this.gameDao.updateUserBalance(data, types);
    }

    public void insertUserdeduction(JSONObject obj) {
        this.gameDao.insertUserdeduction(obj);
    }

    public JSONArray refreshUserBalance() {
        JSONArray array = this.gameDao.getYbUpdateLog();
        return array;
    }

    public JSONObject getGongHui(JSONObject userInfo) {
        return this.gameDao.getGongHui(userInfo);
    }

    public JSONObject getSysUser(String adminCode, String adminPass, String memo) {
        return this.gameDao.getSysUser(adminCode, adminPass, memo);
    }

    public void updateUserPump(String account, String type, double sum) {
        this.gameDao.updateUserPump(account, type, sum);
    }

    public void addUserTicketRec(JSONObject ticketRec) {
        this.gameDao.addUserTicketRec(ticketRec);
    }

    public JSONObject getUserInfoByTel(String tel) {
        return this.gameDao.getUserInfoByTel(tel);
    }

    public int updateUserInfo(JSONObject userInfo) {
        return this.gameDao.updateUserInfo(userInfo);
    }

    public int deleteUserInfoById(long id) {
        return this.gameDao.deleteUserInfoById(id);
    }

    public JSONObject getUserProxyInfoById(long id) {
        return this.gameDao.getUserProxyInfoById(id);
    }

	@Override
	public int updateRoomcardByAccount(String var1, int var2) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public JSONObject getUserBgSetByUserId(long var) {
		return this.gameDao.getGameBgSetByUserId(var);
	}

	@Override
	public int addUserGameSet(JSONObject var) {
		// TODO Auto-generated method stub
		return this.gameDao.insertUserGameSet(var);
	}

	@Override
	public int updateGameSetByGameId(int userId, int gameId, int bgId) {
		// TODO Auto-generated method stub
		return this.gameDao.updateUserGameSetByGameid(userId, gameId, bgId);
	}

	@Override
	public int updateUserNameByUserId(long userId, String name) {
		// TODO Auto-generated method stub
		return this.gameDao.updateUserNameByUserId(userId, name);
	}

	@Override
	public int updateUserHeadimgByUserId(long userId, String headImg) {
		// TODO Auto-generated method stub
		return this.gameDao.updateUserHeadimgByUserId(userId, headImg);
	}

	@Override
	public JSONObject findHeadimgById(long id) {
		// TODO Auto-generated method stub
		return this.gameDao.getHeadimgByHeadId(id);
	}
}
