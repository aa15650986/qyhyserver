
package com.zhuoan.webapp.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("eventMethodTimeConsumeHandler")
@Aspect
public class EventMethodTimeConsumeHandler {
    private static final Logger logger = LoggerFactory.getLogger(EventMethodTimeConsumeHandler.class);
    private static final Long slowMethodMillis = 100L;

    public EventMethodTimeConsumeHandler() {
    }

    @Pointcut("execution(* com.zhuoan.biz.event..*.*(..)))")
    private void gamesEventDealPointcut() {
    }

    @Around("gamesEventDealPointcut())")
    public void doAroundOnMessage(ProceedingJoinPoint call) throws Throwable {
        Signature callSignature = call.getSignature();
        Long beginTime = System.currentTimeMillis();
        call.proceed();
        Long timeConsume = System.currentTimeMillis() - beginTime;
        if (timeConsume > slowMethodMillis) {
            logger.warn("慢方法 = [" + callSignature.getDeclaringTypeName() + "." + callSignature.getName() + "] 耗时 = [" + timeConsume + "] ms");
        }

    }
}
