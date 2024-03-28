package com.foxconn.plm.spas.bean;

import lombok.Data;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/28/ 11:16
 * @description
 */
@Data
public class SpasManPower {
    private Integer id;
    private Integer projectId;
    private Integer phaseId;
    private Integer functionId;
    private String funName;
    private String factor;
    private Integer creator;
    private String createTime;
    private Integer groupId;
    private Integer isActive;
    private String updateTime;
}
