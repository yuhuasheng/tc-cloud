package com.foxconn.plm.integrate.spas.domain.spas;

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
