
package com.zhuoan.webapp.aspect;

import com.alibaba.fastjson.JSONObject;
import com.zhuoan.enumtype.ResCodeEnum;
import com.zhuoan.exception.EventException;
import java.util.HashMap;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("eventExceptionHandlerAspect")
@Aspect
public class EventExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(EventExceptionHandler.class);

    public EventExceptionHandler() {
    }

    @Pointcut("execution(* com.zhuoan.webapp.listener.event..*.*(..)))")
    private void gamesEventPointcut() {
    }

    @Around("gamesEventPointcut())")
    public String doAroundOnMessage(ProceedingJoinPoint call) throws Throwable {
        String result = null;
        Object map = new HashMap();

        try {
            result = (String)call.proceed();
        } catch (EventException var8) {
            logger.error("事件处理异常", var8);
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
