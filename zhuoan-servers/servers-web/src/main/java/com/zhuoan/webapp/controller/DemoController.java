package com.zhuoan.webapp.controller;

import com.github.miemiedev.mybatis.paginator.domain.PageList;
import com.zhuoan.enumtype.PaginationEnum;
import com.zhuoan.enumtype.ResCodeEnum;
import com.zhuoan.model.condition.ZaUsersCondition;
import com.zhuoan.model.vo.ZaUsersVO;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.service.cache.impl.EhCacheHelper;
import com.zhuoan.user.ZaUserBiz;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping({"/"})
@Controller
public class DemoController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);
    @Resource
    private Environment env;
    @Resource
    private ZaUserBiz zaUserBiz;
    @Resource
    private RedisService redisService;

    public DemoController() {
    }

    @RequestMapping({"123demoForRedisUse"})
    @ResponseBody
    public void demoForRedisUse() throws InterruptedException {
        this.redisService.insertKey("123", "123232", 400L);
        this.redisService.insertKey("long", "longlonglong", (Long)null);
        Object o = this.redisService.queryValueByKey("123");
        logger.info("key:123 value = " + o);
        this.redisService.expire("123", 3L);
        Thread.sleep(3001L);
        logger.info("key:123 value = " + this.redisService.queryValueByKey("123") + "  因为对指定的key设置时间3s");
        this.redisService.deleteByKey("long");
        logger.info("删除了key后的值为" + this.redisService.queryValueByKey("long"));
        this.redisService.insertKey("hello，i am here", "方便RDM查看到我", (Long)null);
    }

    @RequestMapping
    public String enter() {
        return "redirect:admin";
    }

    @RequestMapping({"123admin"})
    public void demo(HttpServletRequest request) {
        logger.info("用户 [" + getIp(request) + "] 访问了admin.html");
    }

    @RequestMapping({"123error"})
    @ResponseBody
    public String for404() {
        return "Hi,真不巧,网页走丢了！";
    }

    @RequestMapping({"123key"})
    @ResponseBody
    public void demoObtainValueByKey() {
    }

    @RequestMapping({"123log"}) 
    @ResponseBody
    public String demoLog() {
        logger.info("日常业务代码请打info级别的日志，info级别以下的日志不会给予显示");
        return "WEB-INF/pages下并没有log.html这个页面噢~";
    }

    @RequestMapping(
            value = {"123queryByCondition"},
            method = {RequestMethod.GET}
    )
    @ResponseBody
    public String queryByCondition(ZaUsersCondition zaUsersCondition, String draw) {
        Map<String, Object> resultMap = new HashMap();
        zaUsersCondition.setPageLimit(this.getPageLimit());
        logger.info("查数据开始");
        PageList<ZaUsersVO> zaUsersVOS = this.zaUserBiz.queryAllUsersByCondition(zaUsersCondition);
        resultMap.put(PaginationEnum.DATA.getConstant(), zaUsersVOS);
        resultMap.put(PaginationEnum.DRAW.getConstant(), draw);
        resultMap.put(PaginationEnum.RECORDS_TOTAL.getConstant(), zaUsersVOS.getPaginator().getTotalCount());
        resultMap.put(PaginationEnum.RECORDS_FILTERED.getConstant(), zaUsersVOS.getPaginator().getTotalCount());
        resultMap.put(ResCodeEnum.RES_CODE.getResCode(), ResCodeEnum.SUCCESS.getResCode());
        resultMap.put(ResCodeEnum.RES_MSG.getResCode(), ResCodeEnum.SUCCESS.getResMessage());
        return this.objectToJson(resultMap);
    }

    @RequestMapping({"cache"})
    public void cacheTest() {
        EhCacheHelper.put("helloworld", "1", "1");
        String a = (String)EhCacheHelper.get("helloworld", "1");
        String a2 = (String)EhCacheHelper.get("helloworld2", "1");
    }
}
