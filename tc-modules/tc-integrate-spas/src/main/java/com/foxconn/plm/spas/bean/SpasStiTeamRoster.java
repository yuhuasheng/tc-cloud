package com.foxconn.plm.spas.bean;

import lombok.Data;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/27/ 15:13
 * @description
 */
@Data
public class SpasStiTeamRoster {
    private Integer id;
    private String platformFoundId;
    private String platformFoundName;
    private String teammemberEmpId;
    private String teammemberName;
    private String operationType;
    private String department;
    private String lastUpdateTime;
}
