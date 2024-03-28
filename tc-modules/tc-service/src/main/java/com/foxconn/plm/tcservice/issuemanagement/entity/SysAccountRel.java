package com.foxconn.plm.tcservice.issuemanagement.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 賬號對應關係表
 *
 * @Description
 * @Author MW00442
 * @Date 2023/11/24 16:54
 **/
@Data
@EqualsAndHashCode
public class SysAccountRel implements Serializable {
    private Long id;
    private String uid;
    private Long accountId;
}
