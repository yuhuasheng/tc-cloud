package com.foxconn.plm.integrate.spas.domain.spas;

import lombok.Data;

@Data
public class SpasUserRole {
    private Integer id;
    private Integer userId;
    private Integer roleId;
    private String creator;
    private String createdTime;
    private String updator;
    private String updatedTime;
    private String workId;
}
