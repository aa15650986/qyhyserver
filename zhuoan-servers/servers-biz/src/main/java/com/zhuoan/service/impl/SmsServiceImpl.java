
package com.zhuoan.service.impl;

import com.zhuoan.dao.DBUtil;
import com.zhuoan.service.SmsService;
import com.zhuoan.util.ihuyi.IhuyiSendSms;
import java.util.Random;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class SmsServiceImpl implements SmsService {
    public SmsServiceImpl() {
    }

    public JSONObject getSysSmsByType(int type) {
        return DBUtil.getObjectBySQL("SELECT * FROM sys_sms s WHERE s.`sms_type`= ? AND s.`is_deleted` = ? ", new Object[]{type, 0});
    }

    public String sendMsgIhuyi(String mobile) {
    	
        JSONObject object = this.getSysSmsByType(1);
        System.out.println("api_id:"+String.valueOf(object.get("api_id")));
    	System.out.println("api_key:"+String.valueOf(object.get("api_key")));
    	System.out.println("sms_url"+String.valueOf(object.get("sms_url")));
        Random rd = new Random();
        int mobileCode = rd.nextInt(899999) + 100000;
        IhuyiSendSms.sendMsg(mobileCode, mobile, String.valueOf(object.get("api_id")), String.valueOf(object.get("api_key")), String.valueOf(object.get("sms_url")));
        return String.valueOf(mobileCode);
    }
}
