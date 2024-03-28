package com.foxconn.plm.tcservice.issuemanagement.bean;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询一级账号和二级账号的对应数据类
 *
 * @Description
 * @Author MW00442
 * @Date 2023/11/27 10:54
 **/
@Data
@EqualsAndHashCode
public class AccountBean implements Serializable {
    private String tcUid;
    private String accountId;
    private String no;
    private String name;
    private String bu;
    private String platform;
    private String dept;
    private String secondAccountUid;
}
