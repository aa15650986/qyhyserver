package com.zhuoan.model.condition;

import com.github.miemiedev.mybatis.paginator.domain.PageBounds;

public class BaseCondition {
    private Integer pageNo = 1;
    private Integer pageLimit;

    public BaseCondition() {
    }

    public PageBounds getPageBounds() {
        return new PageBounds(this.pageNo, this.pageLimit, true);
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageNo() {
        return this.pageNo;
    }

    public Integer getPageLimit() {
        return this.pageLimit;
    }

    public void setPageLimit(Integer pageLimit) {
        this.pageLimit = pageLimit;
    }
}
