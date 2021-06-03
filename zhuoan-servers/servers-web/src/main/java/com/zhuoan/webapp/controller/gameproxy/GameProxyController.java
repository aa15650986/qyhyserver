

package com.zhuoan.webapp.controller.gameproxy;

import com.zhuoan.biz.event.BaseEventDeal;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.webapp.controller.BaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class GameProxyController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(GameProxyController.class);
    @Resource
    private RedisService redisService;
    @Resource
    private BaseEventDeal baseEventDeal;
    @Resource
    private UserBiz userBiz;

    public GameProxyController() {
    }

    @RequestMapping(
            value = {"123updateRedisCache.json"},
            method = {RequestMethod.POST}
    )
    public void updateRedisCache(HttpServletRequest request, HttpServletResponse response) throws Exception {
    }
}
