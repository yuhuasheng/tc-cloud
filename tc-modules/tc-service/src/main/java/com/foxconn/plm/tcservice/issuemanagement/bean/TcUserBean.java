package com.foxconn.plm.tcservice.issuemanagement.bean;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * TC一級賬號用戶信息
 *
 * @Description
 * @Author MW00442
 * @Date 2024/2/19 8:22
 **/
@Data
@EqualsAndHashCode
public class TcUserBean {
    private String puid;
    private String userId;
    private String userName;
}
