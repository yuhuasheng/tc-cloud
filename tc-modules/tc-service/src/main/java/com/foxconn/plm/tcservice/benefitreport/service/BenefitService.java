package com.foxconn.plm.tcservice.benefitreport.service;


import com.foxconn.plm.entity.response.R;

/**
 * @Author HuashengYu
 * @Date 2022/10/11 13:55
 * @Version 1.0
 */
public interface BenefitService {

    public R getTCProject(String bu, String projectName);

    public R getBenefitRowData(String bu, String startDate, String projectId);
}
