package com.foxconn.plm.spas.bean;

import lombok.Data;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/27/ 15:07
 * @description
 */
@Data
public class SpasCusAttribute {
    private Integer id;
    private Integer customerId;
    private String attribute;
    private Integer isActive;
    private Integer creator;
    private String createTime;
    private Integer updator;
    private String updateTime;
}
