package com.foxconn.plm.tcreport.drawcountreport.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模型信息
 *
 * @Description
 * @Author MW00442
 * @Date 2023/9/12 16:36
 **/
@Data
@EqualsAndHashCode
public class ModelBean implements Serializable {
    private String objId;
    private String objName;
    private LocalDateTime createTime;
}
