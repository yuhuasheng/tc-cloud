package com.foxconn.plm.spas.bean;

import lombok.Data;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/28/ 11:16
 * @description
 */
@Data
public class SpasFunction {
    private Integer id;
    private String name;
    private Integer isActive;
    private Integer creator;
    private String createTime;
    private Integer updator;
    private String updateTime;

}
