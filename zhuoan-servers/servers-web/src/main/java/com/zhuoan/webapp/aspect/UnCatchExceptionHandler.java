

package com.zhuoan.webapp.aspect;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

public class UnCatchExceptionHandler implements HandlerExceptionResolver {
    private static Logger logger = LoggerFactory.getLogger(UnCatchExceptionHandler.class);

    public UnCatchExceptionHandler() {
    }

    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse httpServletResponse, Object o, Exception e) {
        logger.error("ZHUO_AN:异常URL【" + request.getRequestURL() + "】捕捉到非业务异常：", e);
        return new ModelAndView("/error");
    }
}
