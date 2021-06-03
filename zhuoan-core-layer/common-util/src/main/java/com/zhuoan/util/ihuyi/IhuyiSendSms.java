
package com.zhuoan.util.ihuyi;

import java.io.IOException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IhuyiSendSms {
    private static final Logger log = LoggerFactory.getLogger(IhuyiSendSms.class);

    public IhuyiSendSms() {
    }

    public static void sendMsg(int mobileCode, String mobile, String account, String password, String url) {
       
    	HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(url);
        client.getParams().setContentCharset("GBK");
        method.setRequestHeader("ContentType", "application/x-www-form-urlencoded;charset=GBK");
        String content = "您的验证码是：" + mobileCode + "。请不要把验证码泄露给其他人。";
        NameValuePair[] data = new NameValuePair[]{new NameValuePair("account", account), new NameValuePair("password", password), new NameValuePair("mobile", mobile), new NameValuePair("content", content)};
        method.setRequestBody(data);

        try {
            client.executeMethod(method);
            String SubmitResult = method.getResponseBodyAsString();
            Document doc = DocumentHelper.parseText(SubmitResult);
            Element root = doc.getRootElement();
            String code = root.elementText("code");
            if ("2".equals(code)) {
                log.info("【互亿】短信发送成功{}", mobileCode);
            } else {
                log.info("【互亿】短信发送失败{}", mobileCode);
            }
        } catch (HttpException var18) {
            log.info("【互亿】短信异常{}", var18);
        } catch (IOException var19) {
            log.info("【互亿】短信异常{}", var19);
        } catch (DocumentException var20) {
            log.info("【互亿】短信异常{}", var20);
        } finally {
            method.releaseConnection();
        }

    }

}
