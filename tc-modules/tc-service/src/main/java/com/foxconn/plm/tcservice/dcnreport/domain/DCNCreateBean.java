package com.foxconn.plm.tcservice.dcnreport.domain;

import lombok.Data;

import java.util.Date;

/**
 * @Author HuashengYu
 * @Date 2022/11/8 17:25
 * @Version 1.0
 */
@Data
public class DCNCreateBean {

    private String itemId;
    private String version;
    private String itemRevUid;
    private Date createDate;
}
