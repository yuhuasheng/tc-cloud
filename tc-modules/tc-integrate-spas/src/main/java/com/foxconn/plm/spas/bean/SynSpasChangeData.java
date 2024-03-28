package com.foxconn.plm.spas.bean;

import lombok.Data;

import java.util.Date;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/12/ 14:16
 * @description
 */
@Data
public class SynSpasChangeData {
    private Integer id;
    private String customerId;//客户ID
    private String customerName;//客户名称
    private String customerOperationType;//客户操作类型
    private String seriesId;//系列ID
    private String seriesName;//系列名称
    private String seriesOperationType;//系列操作类型
    private String platformFoundId;//专案ID
    private String platformFoundName;//专案名称
    private String creatorEmpId;//专案创建人(工号)
    private String creatorEmpName;//专案创建人(姓名)
    private String platformLevel;//专案等级
    private String platformPhase;//专案阶段
    private String productLineId;//产品线ID
    private String productLine;//产品线名称
    private String platformOperationType;//专案操作类型
    private Date lastUpdateTime;//修改时间
    private Date creationTime = new Date();
    private Integer handleState = 0;
    private String bu;
}
