package com.foxconn.plm.tcservice.issuemanagement.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 庫文件夾
 *
 * @Description
 * @Author MW00442
 * @Date 2023/12/1 10:48
 **/
@Data
@EqualsAndHashCode
public class SysLibFolder implements Serializable {
    private Long id;
    private String name;
    private String sign;
    private Integer sort;
    private String delFlag;
}
