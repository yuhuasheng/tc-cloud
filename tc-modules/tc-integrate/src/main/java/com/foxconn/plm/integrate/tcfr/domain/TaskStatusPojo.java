package com.foxconn.plm.integrate.tcfr.domain;

import lombok.Data;

@Data
public class TaskStatusPojo {
    private String  actionItemId;
    private String  status;//0：open    1：close
}
