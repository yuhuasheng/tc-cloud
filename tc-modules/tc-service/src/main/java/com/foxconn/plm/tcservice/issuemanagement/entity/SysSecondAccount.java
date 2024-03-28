package com.foxconn.plm.tcservice.issuemanagement.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 二級賬號表
 *
 * @Description
 * @Author MW00442
 * @Date 2023/11/24 16:51
 **/
@Data
@EqualsAndHashCode
public class SysSecondAccount implements Serializable {
    private Long id;
    private String no;
    private String name;
    private String bu;
    private String platform;
    private String dept;
    private String tcUid;
    private String delFlag;
}
