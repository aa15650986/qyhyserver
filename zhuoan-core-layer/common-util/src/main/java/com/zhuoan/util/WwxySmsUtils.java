

package com.zhuoan.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WwxySmsUtils {
    private static final Logger logger = LoggerFactory.getLogger(SmsUtils.class);
    public static final String url = "http://sms.uninets.com.cn/Modules/Interface/http/IservicesBSJY.aspx";
    public static final String userid = "zhoan";
    public static final String pwd = "zhoan10086";

    public WwxySmsUtils() {
    }

    public static void main(String[] args) {
        System.out.println(sendMsg("18159170807", (String)null));
    }

    public static String sendMsg(String mobile, String name) {
        if (name == null) {
            name = "----";
        }

        Random rd = new Random();
        int number = rd.nextInt(899999) + 100000;

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("flag=sendsms&loginname=").append("zhoan").append("&password=").append("zhoan10086").append("&p=").append(mobile).append("&c=").append(URLEncoder.encode("【" + name + "】" + "您的验证码是" + number + "。如非本人操作，请忽略本短信", "UTF-8"));
            net(sb.toString());
        } catch (Exception var5) {
            logger.error("发送短信异常！！！", var5);
        }

        return String.valueOf(number);
    }

    public static void net(String urlParameters) throws Exception {
        URL obj = new URL("http://sms.uninets.com.cn/Modules/Interface/http/IservicesBSJY.aspx");
        HttpURLConnection con = (HttpURLConnection)obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();
        int responseCode = con.getResponseCode();
        if (responseCode == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuffer response = new StringBuffer();

            String inputLine;
            while((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();
            logger.info("发送微网信云短信:" + response.toString());
        } else {
            logger.error("发送微网信云短信异常！！！");
        }

    }
}
