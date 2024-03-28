package com.foxconn.plm.tcservice.issuemanagement.bean;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * TC二級賬號ActualUser信息
 *
 * @Description
 * @Author MW00442
 * @Date 2024/2/19 8:33
 **/
@Data
@EqualsAndHashCode
public class TcAccountUserBean {
    private String puid;
    private String itemId;
    private String objectName;
    private String userInfo;
    private String enName;
}
