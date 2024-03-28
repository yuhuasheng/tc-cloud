package com.foxconn.plm.tcservice.issuemanagement.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 查詢所有一級二級用戶信息
 *
 * @Description
 * @Author MW00442
 * @Date 2023/12/21 17:09
 **/
@Data
@EqualsAndHashCode
public class UserListRes implements Serializable {
    private List<UserRes> userList;

    private List<UserRes> accountList;
}
