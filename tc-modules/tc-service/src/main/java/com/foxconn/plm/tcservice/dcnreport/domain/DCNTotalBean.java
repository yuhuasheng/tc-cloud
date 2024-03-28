package com.foxconn.plm.tcservice.dcnreport.domain;

import lombok.Data;

/**
 * @Author MW00333
 * @Date 2023/5/11 16:20
 * @Version 1.0
 */
@Data
public class DCNTotalBean {

    private String userId;
    private String userName;
    private float dcnFeePer = 0L;
}
