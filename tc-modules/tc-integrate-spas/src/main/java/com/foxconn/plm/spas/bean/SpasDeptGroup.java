package com.foxconn.plm.spas.bean;

import lombok.Data;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/27/ 10:28
 * @description
 */
@Data
public class SpasDeptGroup {
    private Integer id;
    private Integer businessUnitId;
    private Integer divisionId;
    private Integer departmentId;
    private Integer sectionId;
    private Integer functionId;
    private Integer isActive;
    private Integer creator;
    private String createTime;
    private String updator;
    private String updateTime;
    private Integer isYellow;
    private String deptCode;
    private String prodId;
}
