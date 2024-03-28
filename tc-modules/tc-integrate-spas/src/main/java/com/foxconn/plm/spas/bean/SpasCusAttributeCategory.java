package com.foxconn.plm.spas.bean;

import lombok.Data;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/27/ 14:55
 * @description
 */
@Data
public class SpasCusAttributeCategory {
    private Integer id;
    private Integer attributeId;
    private String categoryName;
    private Integer sort;
    private Integer isActive;
    private Integer creator;
    private String createTime;
    private Integer updator;
    private String updateTime;
}
