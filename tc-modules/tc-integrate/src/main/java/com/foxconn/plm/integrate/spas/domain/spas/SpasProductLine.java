package com.foxconn.plm.integrate.spas.domain.spas;

import lombok.Data;

@Data
public class SpasProductLine {
    private Integer id;
    private Integer customerId;
    private String name;
    private Integer isShare;
    private Integer isActive;
    private String updator;
    private String updateTime;
    private String creator;
    private String createTime;
    ;
}
