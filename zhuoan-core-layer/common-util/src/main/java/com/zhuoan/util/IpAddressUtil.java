
package com.zhuoan.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpAddressUtil {
    private static final Logger logger = LoggerFactory.getLogger(IpAddressUtil.class);

    public IpAddressUtil() {
    }

    public static String getInnerNetIp() {
        String ip = null;

        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception var2) {
            logger.error("获得内网IP异常", var2);
        }

        return ip;
    }

    public static String getOuterIp() {
        String ip = "";
        String chinaz = "http://ip.chinaz.com";
        StringBuilder inputLine = new StringBuilder();
        BufferedReader in = null;

        try {
            URL url = new URL(chinaz);
            HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
            in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));

            String read;
            while((read = in.readLine()) != null) {
                inputLine.append(read).append("\r\n");
            }
        } catch (IOException var15) {
            logger.error("获得外网IP异常", var15);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException var14) {
                    logger.error("获取外网ip#####关闭连接异常#####", var14);
                }
            }

        }

        return parse(ip, inputLine);
    }

    private static String parse(String ip, StringBuilder inputLine) {
        Pattern p = Pattern.compile("\\<dd class\\=\"fz24\">(.*?)\\<\\/dd>");
        Matcher m = p.matcher(inputLine.toString());
        if (m.find()) {
            ip = m.group(1);
        }

        return ip;
    }

    public static String getLocalOuterNetIp() {
        InputStream in = null;
        StringBuffer buffer = new StringBuffer();

        try {
            URL url = new URL("http://ip.chinaz.com/getip.aspx");
            in = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));

            String inputLine;
            while((inputLine = reader.readLine()) != null) {
                buffer.append(inputLine);
            }
        } catch (Exception var13) {
            logger.error("获取外网ip和所在地异常", var13);
        } finally {
            try {
                ((InputStream)Objects.requireNonNull(in)).close();
            } catch (Exception var12) {
                logger.error("获取外网ip和所在地#####关闭连接异常#####", var12);
            }

        }

        JSONObject o = JSONObject.fromObject(String.valueOf(buffer));
        return String.valueOf(o.get("ip"));
    }
}
