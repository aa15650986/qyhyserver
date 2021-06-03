
package com.zhuoan.biz.model.gzmj;

import net.sf.json.JSONObject;

public class GzMjSummaryDetail {
    private String detailName;
    private JSONObject detailCardList = new JSONObject();
    private String detailScore;

    public GzMjSummaryDetail() {
    }

    public String getDetailName() {
        return this.detailName;
    }

    public void setDetailName(String detailName) {
        this.detailName = detailName;
    }

    public JSONObject getDetailCardList() {
        return this.detailCardList;
    }

    public void setDetailCardList(JSONObject detailCardList) {
        this.detailCardList = detailCardList;
    }

    public String getDetailScore() {
        return this.detailScore;
    }

    public void setDetailScore(String detailScore) {
        this.detailScore = detailScore;
    }
}
