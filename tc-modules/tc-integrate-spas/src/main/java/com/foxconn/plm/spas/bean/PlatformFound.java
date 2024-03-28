package com.foxconn.plm.spas.bean;

import lombok.Data;

import java.io.Serializable;

@Data
public class PlatformFound extends Project implements Serializable {
    private String cId;
    private String cName;
    private String sId;
    private String sName;
    private String platformFoundLevel;//专案等级
    private String platformFoundPhase;//专案阶段
    private String productLineId;//产品线ID
    private String productLineName;//产品线名称
    private String creator;
}
