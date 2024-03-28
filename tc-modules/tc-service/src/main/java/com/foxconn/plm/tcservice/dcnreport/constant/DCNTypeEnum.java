package com.foxconn.plm.tcservice.dcnreport.constant;

/**
 * @Author HuashengYu
 * @Date 2022/11/7 14:41
 * @Version 1.0
 */
public enum DCNTypeEnum {

    DT_DCN_REV("Desktop DCN Revision", "D9_DT_DCNRevision"), MNT_DCN_REV("Monitor DCN Revision", "D9_MNT_DCNRevision"), PRT_DCN_REV("Printer DCN Revision", "D9_PRT_DCNRevision");

    private final String displayType;
    private final String actualType;

    private DCNTypeEnum(String displayType, String actualType) {
        this.displayType = displayType;
        this.actualType = actualType;
    }

    public String displayType() {
        return this.displayType;
    }

    public String actualType() {
        return this.actualType;
    }
}
