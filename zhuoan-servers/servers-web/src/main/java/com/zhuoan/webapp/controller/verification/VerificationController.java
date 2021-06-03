

package com.zhuoan.webapp.controller.verification;

import com.alibaba.fastjson.JSON;
import com.zhuoan.service.SmsService;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.webapp.controller.BaseController;
import java.io.IOException;
import java.util.regex.Pattern;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class VerificationController extends BaseController {
    @Resource
    private RedisService redisService;
    @Resource
    private SmsService smsService;

    public VerificationController() {
    }

    @RequestMapping(
            value = {"123getVerificationCode"},
            method = {RequestMethod.GET}
    )
    @ResponseBody
    public void getVerificationCode(HttpServletResponse response, String tel, String platform) throws IOException {
        System.out.println("===============发送短信请求================");
    	JSONObject result = new JSONObject();
        if (platform != null && !"".equals(platform)) {
            boolean matches = false;
            if (tel != null) {
                matches = Pattern.matches("^((17[0-9])|(14[0-9])|(13[0-9])|(15[^4,\\D])|(18[0-9]))\\d{8}$", tel);
            }

            if (matches) {
                if (!this.redisService.sHasKey("verification_set", tel)) {
                    String verificationCode = this.smsService.sendMsgIhuyi(tel);
                    this.redisService.hset("verification_map", tel, verificationCode, 1800L);
                    this.redisService.sSetAndTime("verification_set", 60L, new Object[]{tel});
                    result.put("code", 1);
                    result.put("msg", "获取验证码成功");
                } else {
                    result.put("code", 0);
                    result.put("msg", "请勿重复获取验证码");
                }
            } else {
                result.put("code", 0);
                result.put("msg", "请输入正确的手机号");
            }
        } else {
            result.put("code", 0);
            result.put("msg", "请输入平台号");
        }

        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/plain");
        response.getWriter().write(JSON.toJSONString(result));
    }
}
