package com.foxconn.plm.spas.bean;

import lombok.Data;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/27/ 10:58
 * @description
 */
@Data
public class SpasProjectPerson {
    private Integer id;
    private Integer projectId;
    private String workId;
    private Integer groupId;
    private Integer isMainContact;
    private String creator;
    private String createTime;
}
