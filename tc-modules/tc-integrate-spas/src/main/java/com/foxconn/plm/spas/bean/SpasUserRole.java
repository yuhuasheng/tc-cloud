package com.foxconn.plm.spas.bean;

import lombok.Data;

@Data
public class SpasUserRole {
    private Integer id;
    private Integer userId;
    private Integer roleId;
    private String creator;
    private String updateTime;
    private String updator;
    private String createTime;
    private String workId;
}
