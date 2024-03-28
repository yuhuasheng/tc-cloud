package com.foxconn.plm.integrate.sap.customPN.domain;

import lombok.Data;

@Data
public class ApplyCustomPnResponse {
    private int code;//-1 error   1- 成功
    private String msg;
    private String uid;
    private String oldItemId;
    private String newItemId;


}
