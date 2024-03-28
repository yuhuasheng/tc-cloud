package com.foxconn.plm.entity.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2023/12/9 11:46
 **/
@Data
@EqualsAndHashCode
public class ActualUserPojo implements Serializable {
    private String processNode;
    private String processNodeStatus;
    private String tcUser;
    private String actualUserId;
    private String actualUserName;
    private String actualUserMail;
    private String approvalStatus;
    private String approvalDate;
}
