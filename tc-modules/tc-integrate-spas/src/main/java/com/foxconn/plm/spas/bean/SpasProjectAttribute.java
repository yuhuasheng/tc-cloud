package com.foxconn.plm.spas.bean;

import lombok.Data;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/27/ 11:16
 * @description
 */
@Data
public class SpasProjectAttribute {
    private Integer id;
    private Integer projectId;
    private Integer attributeCategoryId;
    private Integer creator;
    private String createTime;
    private Integer businessStageId;
}
