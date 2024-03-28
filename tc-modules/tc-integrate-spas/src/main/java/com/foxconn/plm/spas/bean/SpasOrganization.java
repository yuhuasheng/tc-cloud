package com.foxconn.plm.spas.bean;

import lombok.Data;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/27/ 10:18
 * @description
 */
@Data
public class SpasOrganization {
    private Integer id;
    private Integer num;
    private Integer parentId;
    private String name;
    private String displayName;
    private Integer isActive;
    private Integer creator;
    private String createTime;
    private String updator;
    private String updateTime;
}
