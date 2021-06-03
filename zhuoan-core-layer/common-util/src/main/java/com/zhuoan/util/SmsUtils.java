
package com.zhuoan.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmsUtils {
    private static final Logger logger = LoggerFactory.getLogger(SmsUtils.class);
    public static final String DEF_CHARSET = "UTF-8";
    public static final int DEF_CONN_TIMEOUT = 30000;
    public static final int DEF_READ_TIMEOUT = 30000;
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.1547.66 Safari/537.36";
    public static final String TEMPLATE_ID = "113597";
    public static final String APP_KEY = "59f49def764d83248b2dd9418dd0a0a9";

    public SmsUtils() {
    }

    public static String sendMsg(String mobile) {
        Random rd = new Random();
        int number = rd.nextInt(899999) + 100000;
        String result = null;
        String url = "http://v.juhe.cn/sms/send";
        Map<String, Object> params = new HashMap();
        params.put("mobile", mobile);
        params.put("tpl_id", "113597");
        String tpl_value = "#code#=" + String.valueOf(number);
        params.put("tpl_value", tpl_value);
        params.put("key", "59f49def764d83248b2dd9418dd0a0a9");
        params.put("dtype", "json");

        try {
            result = net(url, params, "GET");
            JSONObject object = JSONObject.fromObject(result);
            if (object.getInt("error_code") == 0) {
                logger.info(object.get("result").toString());
            } else {
                logger.info(object.get("error_code") + ":" + object.get("reason"));
            }
        } catch (Exception var8) {
            logger.error("发送短信异常！！！", var8);
        }

        return String.valueOf(number);
    }

    public static String net(String strUrl, Map<String, Object> params, String method) throws Exception {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        String rs = null;

        try {
            StringBuffer sb = new StringBuffer();
            if (method == null || method.equals("GET")) {
                strUrl = strUrl + "?" + urlEncode(params);
            }

            URL url = new URL(strUrl);
            conn = (HttpURLConnection)url.openConnection();
            if (method != null && !method.equals("GET")) {
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
            } else {
                conn.setRequestMethod("GET");
            }

            conn.setRequestProperty("User-agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.1547.66 Safari/537.36");
            conn.setUseCaches(false);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(false);
            conn.connect();
            if (params != null && method.equals("POST")) {
                try {
                    DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                    out.writeBytes(urlEncode(params));
                } catch (Exception var14) {
                    logger.error("发送短信异常！！！", var14);
                }
            }

            InputStream is = conn.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            String strRead;
            while((strRead = reader.readLine()) != null) {
                sb.append(strRead);
            }

            rs = sb.toString();
        } catch (IOException var15) {
            logger.error("发送短信异常！！！", var15);
        } finally {
            if (reader != null) {
                reader.close();
            }

            if (conn != null) {
                conn.disconnect();
            }

        }

        return rs;
    }

    public static String urlEncode(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        Iterator var2 = data.entrySet().iterator();

        while(var2.hasNext()) {
            Entry i = (Entry)var2.next();

            try {
                sb.append(i.getKey()).append("=").append(URLEncoder.encode(i.getValue() + "", "UTF-8")).append("&");
            } catch (UnsupportedEncodingException var5) {
                logger.error("发送短信异常！！！", var5);
            }
        }

        return sb.toString();
    }
}
