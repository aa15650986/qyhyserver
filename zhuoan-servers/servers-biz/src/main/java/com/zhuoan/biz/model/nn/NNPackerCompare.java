
package com.zhuoan.biz.model.nn;

import com.zhuoan.biz.core.nn.NNPacker;
import com.zhuoan.biz.core.nn.NNUserPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NNPackerCompare {
    private static final Logger logger = LoggerFactory.getLogger(NNPackerCompare.class);

    public NNPackerCompare() {
    }

    public static NNUserPacket getWin(NNUserPacket up1, NNUserPacket up2) {
        if (!up1.isBanker() ^ up2.isBanker()) {
            try {
                throw new Exception("两个用户,必须一个是庄家、一个是用户");
            } catch (Exception var3) {
                logger.error("", var3);
                return null;
            }
        } else {
            return CompareType(up1, up2);
        }
    }

    private static NNUserPacket CompareType(NNUserPacket up1, NNUserPacket up2) {
        if (up1.getType() == up2.getType()) {
            compareNum(up1, up2);
            return up1.isWin() ? up1 : up2;
        } else {
            if (up1.getType() > up2.getType()) {
                up1.setWin(true);
                up2.setWin(false);
            } else {
                up1.setWin(false);
                up2.setWin(true);
            }

            return up1.isWin() ? up1 : up2;
        }
    }

    private static void compareNum(NNUserPacket up1, NNUserPacket up2) {
        NNPacker[] newP1 = NNPacker.sort(up1.getPs());
        NNPacker[] newP2 = NNPacker.sort(up2.getPs());
        int result = newP1[4].compare(newP2[4]);
        if (result == 0) {
            try {
                throw new Exception("服务器异常，一副牌中出现大小花色完全一样的牌");
            } catch (Exception var6) {
                logger.error("", var6);
            }
        } else {
            if (result > 0) {
                up1.setWin(true);
                up2.setWin(false);
            } else {
                up1.setWin(false);
                up2.setWin(true);
            }

        }
    }
}
