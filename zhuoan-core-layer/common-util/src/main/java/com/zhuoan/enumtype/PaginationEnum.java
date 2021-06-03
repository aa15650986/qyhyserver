

package com.zhuoan.enumtype;

public enum PaginationEnum {
    DATA("data"),
    DRAW("draw"),
    RECORDS_TOTAL("recordsTotal"),
    RECORDS_FILTERED("recordsFiltered");

    private String constant;

    private PaginationEnum(String constant) {
        this.constant = constant;
    }

    public String getConstant() {
        return this.constant;
    }

    public void setConstant(String constant) {
        this.constant = constant;
    }
}
