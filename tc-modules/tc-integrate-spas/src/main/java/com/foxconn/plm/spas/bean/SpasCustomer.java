package com.foxconn.plm.spas.bean;

import lombok.Data;

@Data
public class SpasCustomer {
    private Integer id;
    private String name;
    private Integer typeId;
    private Integer isActive;
    private String creator;
    private String createTime;
    private String updator;
    private String updateTime;
}
