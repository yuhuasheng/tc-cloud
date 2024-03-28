package com.foxconn.plm.integrate.spas.domain;

import lombok.Data;

import java.util.Date;

@Data
public class STIProject {

    private int id;
    private String customerId;//客户ID
    private String customerName;//客户名称
    private String customerOperationType;//客户操作类型
    private String seriesId;//系列ID
    private String seriesName;//系列名称
    private String seriesOperationType;//系列操作类型
    private String platformFoundId;//专案ID
    private String platformFoundName;//专案名称
    private String platformFoundCreatorEmpId;//专案创建人(工号)
    private String platformFoundCreatorName;//专案创建人(姓名)
    private String platformFoundLevel;//专案等级
    private String platformFoundPhase;//专案阶段
    private String platformFoundProductLineId;//产品线ID
    private String platformFoundProductLine;//产品线名称
    private String platformFoundOperationType;//专案操作类型
    private Date lastUpdateTime;//修改时间

}
