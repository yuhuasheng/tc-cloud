package com.foxconn.plm.spas.bean;

import lombok.Data;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/27/ 10:50
 * @description
 */
@Data
public class SpasRouting {
    private Integer id;
    private Integer projectId;
    private Integer phaseId;
    private String startTime;
    private String endTime;
    private Integer status;
    private Integer creator;
    private String createTime;
    private String updator;
    private String updateTime;
}
