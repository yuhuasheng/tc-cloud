package com.foxconn.plm.tcservice.ftebenefitreport.constant;

/**
 * @Author HuashengYu
 * @Date 2022/9/27 11:55
 * @Version 1.0
 */
public enum FunctionNameEnum {

    REPAI_MOLD("修模", "REPAI_MOLD"), REWORK("重工", "REWORK"), STOPLINE("停线", "STOPLINE");
    private final String functionNameCh;
    private final String functionNameEn;

    private FunctionNameEnum(String functionNameCh, String functionNameEn) {
        this.functionNameCh = functionNameCh;
        this.functionNameEn = functionNameEn;
    }

    public String functionNameCh() {
        return this.functionNameCh;
    }

    public String functionNameEn() {
        return this.functionNameEn;
    }
}
