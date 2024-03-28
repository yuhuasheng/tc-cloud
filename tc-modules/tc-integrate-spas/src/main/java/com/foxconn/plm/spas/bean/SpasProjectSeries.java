package com.foxconn.plm.spas.bean;

import lombok.Data;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/27/ 10:40
 * @description
 */
@Data
public class SpasProjectSeries {
    private Integer id;
    private Integer customerId;
    private String name;
    private Integer owner;
    private Integer creator;
    private String createTime;
    private String updator;
    private String updateTime;
}
