package com.foxconn.plm.spas.bean;

import lombok.Data;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/27/ 15:28
 * @description
 */
@Data
public class SpasManpowerStandard {
    private Integer id;
    private Integer projectId;
    private Integer phaseId;
    private Integer functionId;
    private String funName;
    private Integer groupId;
    private Integer isActive = 1;
    private String createdTime;
    private String factor;
    private Integer creator;
    private String updateTime;
}
