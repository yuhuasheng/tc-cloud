package com.foxconn.plm.integrate.mdas.domain;

import lombok.Data;

@Data
public class MdasResponse {
    private String msg;
    private int status;
    private int errorCode;
    private Object data;
}
