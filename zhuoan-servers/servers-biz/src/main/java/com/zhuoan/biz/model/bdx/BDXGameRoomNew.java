
package com.zhuoan.biz.model.bdx;

import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import java.io.Serializable;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class BDXGameRoomNew extends GameRoom implements Serializable {
    private ConcurrentHashMap<String, UserPackerBDX> userPacketMap = new ConcurrentHashMap();

    public BDXGameRoomNew() {
    }

    public ConcurrentHashMap<String, UserPackerBDX> getUserPacketMap() {
        return this.userPacketMap;
    }

    public void setUserPacketMap(ConcurrentHashMap<String, UserPackerBDX> userPacketMap) {
        this.userPacketMap = userPacketMap;
    }

    public void initGame() {
    }

    public JSONArray getAllPlayer() {
        JSONArray array = new JSONArray();
        Iterator var2 = this.getPlayerMap().keySet().iterator();

        while(var2.hasNext()) {
            String uuid = (String)var2.next();
            Playerinfo player = (Playerinfo)this.getPlayerMap().get(uuid);
            if (player != null) {
                UserPackerBDX up = (UserPackerBDX)this.userPacketMap.get(uuid);
                JSONObject obj = new JSONObject();
                obj.put("account", player.getAccount());
                obj.put("name", player.getName());
                obj.put("headimg", player.getRealHeadimg());
                obj.put("sex", player.getSex());
                obj.put("ip", player.getIp());
                obj.put("vip", player.getVip());
                obj.put("location", player.getLocation());
                obj.put("area", player.getArea());
                obj.put("score", player.getScore());
                obj.put("index", player.getMyIndex());
                obj.put("userOnlineStatus", player.getStatus());
                obj.put("ghName", player.getGhName());
                obj.put("introduction", player.getSignature());
                obj.put("userStatus", up.getStatus());
                obj.put("value", up.getValue());
                obj.put("sum", up.getScore());
                array.add(obj);
            }
        }

        return array;
    }
}
