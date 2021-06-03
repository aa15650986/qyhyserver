

package com.zhuoan.biz.event;

import com.zhuoan.biz.game.biz.FundBiz;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.util.MathDelUtil;
import java.util.Map;
import javax.annotation.Resource;
import javax.jms.Destination;
import net.sf.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FundEventDeal {
    private static final Logger logger = LoggerFactory.getLogger(FundEventDeal.class);
    @Resource
    private RedisService redisService;
    @Resource
    private Destination baseQueueDestination;
    @Resource
    private ProducerService producerService;
    @Resource
    private FundBiz fundBiz;

    public FundEventDeal() {
    }

    public void getAndUpdateUserMoney(String chainAdd) {
    }

    private double getUserMoneyFromZob(String chainAdd) {
        return -1.0D;
    }

    public void changeUserBalStatus(String chainAdd, int status, long assetsId, String roomNo, String account) {
    }

    public void addBalChangeRecord(Map<String, Double> userScoreMap, String des) {
    }

    public void addGameOrder(JSONArray array) {
    }

    public void joinSysUser(String roomNo) {
    }

    public void sysUserExit(String account) {
    }

    private void initSysUser() {
    }

    private String getUsefulAccount() {
        String account = MathDelUtil.getRandomStr(8);
        return account;
    }
}
