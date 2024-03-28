package com.foxconn.plm.tcservice.issuemanagement.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 客戶
 *
 * @Description
 * @Author MW00442
 * @Date 2023/12/1 10:48
 **/
@Data
@EqualsAndHashCode
public class SysCustomer implements Serializable {
    private Long id;
    private String name;
    private String sign;
    private Integer sort;
    private String delFlag;
}
