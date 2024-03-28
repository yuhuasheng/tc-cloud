package com.foxconn.plm.tcservice.issuemanagement.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * TODO
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/31 13:36
 **/
@Data
@EqualsAndHashCode
public class AccountRes {
    private String uid;
    private String puid;
    private String item_id;
    private String object_name;
    private String user_info;
    private String user_uid;
    private boolean disabled =false;

    private List<AccountRes> parent;

}
