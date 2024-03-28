package com.foxconn.plm.integrate.spas.domain;

import lombok.Data;

import java.util.Date;

@Data
public class STITeamRoster {
    private String sn;
    private Long id;
    private String platformFoundId;//专案ID
    private String platformFoundName;//专案名称
    private String customerName;//客户名称
    private String platformFoundPhase;//专案阶段
    private String productLineName;//产品线名称
    private String teamMemberEmpId;//专案成员(工号)
    private String teamMemberName;//专案成员(姓名)
    private String department;//部门
    private String operationType;//操作类型
    private Date lastUpdateTime;//修改时间
}
