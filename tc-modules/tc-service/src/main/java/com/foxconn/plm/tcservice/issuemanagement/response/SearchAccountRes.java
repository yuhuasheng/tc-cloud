package com.foxconn.plm.tcservice.issuemanagement.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 搜索賬號返回值類
 *
 * @Description
 * @Author MW00442
 * @Date 2023/12/21 11:13
 **/
@Data
@EqualsAndHashCode
public class SearchAccountRes implements Serializable {
    private String tcUid;
    private String accountId;
    private String no;
    private String name;
    private String bu;
    private String platform;
    private String dept;
    private String secondAccountUid;
    private String accountDesc;
    private String tcUserDesc;
}
