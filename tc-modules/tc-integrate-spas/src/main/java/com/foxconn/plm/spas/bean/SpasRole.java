package com.foxconn.plm.spas.bean;

import lombok.Data;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/27/ 10:06
 * @description
 */
@Data
public class SpasRole {
    private Integer id;
    private String name;
    private Integer isActive;
    private Integer creator;
    private String createTime;
    private String updator;
    private String updateTime;
}
