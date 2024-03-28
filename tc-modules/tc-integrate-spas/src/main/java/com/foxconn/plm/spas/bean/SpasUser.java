package com.foxconn.plm.spas.bean;

import lombok.Data;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/14/ 13:48
 * @description
 */
@Data
public class SpasUser {
    private Integer id;
    private String workId;
    private String userName;
    private Integer groupId;
    private String notes;
    private String deptName;
    private Integer isActive;
    private String createTime;
    private String updateTime;
}
