package com.foxconn.plm.tcservice.issuemanagement.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * tc賬號返回值類
 *
 * @Description
 * @Author MW00442
 * @Date 2023/12/21 17:06
 **/
@Data
@EqualsAndHashCode
public class UserRes implements Serializable {
    private String uid;
    private String desc;
}
