
package com.zhuoan.webapp.aspect;

import com.alibaba.fastjson.JSONObject;
import com.zhuoan.enumtype.ResCodeEnum;
import com.zhuoan.exception.BizException;
import java.util.HashMap;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("commonExceptionHandlerAspect")
@Aspect
public class ControllerExceptionHandler {
    private static Logger logger = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    public ControllerExceptionHandler() {
    }

    @Pointcut("execution(* com.zhuoan.webapp.controller..*.*(..)))")
    private void webappControllerPointcut() {
    }

    @Around("webappControllerPointcut()&&@annotation(org.springframework.web.bind.annotation.ResponseBody)")
    public String doAroundResponseBody(ProceedingJoinPoint call) throws Throwable {
        String result = null;
        Object map = new HashMap();

        try {
            result = (String)call.proceed();
        } catch (BizException var8) {
            logger.error("业务异常", var8);
            if (var8.getMessageMap() != null) {
                map = var8.getMessageMap();
            }

            ((Map)map).put(ResCodeEnum.RES_CODE.getResCode(), var8.getCode());
            ((Map)map).put(ResCodeEnum.RES_MSG.getResCode(), var8.getMessage());

            try {
                result = JSONObject.toJSONString(map);
            } catch (Exception var7) {
                logger.info("接口返回数据转为json格式错误:", var7);
            }
        } catch (Exception var9) {
            logger.error("系统异常", var9);
            ((Map)map).put(ResCodeEnum.RES_CODE.getResCode(), ResCodeEnum.SYSTEM_EXCEPTION.getResCode());
            ((Map)map).put(ResCodeEnum.RES_MSG.getResCode(), ResCodeEnum.SYSTEM_EXCEPTION.getResMessage());

            try {
                result = JSONObject.toJSONString(map);
            } catch (Exception var6) {
                logger.info("接口返回数据转为json格式错误:", var6);
            }
        }

        return result;
    }
}
