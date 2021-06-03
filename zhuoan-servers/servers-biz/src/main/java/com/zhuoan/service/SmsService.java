
package com.zhuoan.service;

import net.sf.json.JSONObject;

public interface SmsService {
    JSONObject getSysSmsByType(int var1);

    String sendMsgIhuyi(String var1);
}
