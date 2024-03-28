package com.foxconn.plm.tcservice.ftebenefitreport.domain;

import lombok.Data;

/**
 * @Author HuashengYu
 * @Date 2022/9/26 11:49
 * @Version 1.0
 */
@Data
public class FTEBenefitBean {
    private String bu;
    private String functionName;
    private String years;
    private String benefit;
    private String predictBenefit;
}
