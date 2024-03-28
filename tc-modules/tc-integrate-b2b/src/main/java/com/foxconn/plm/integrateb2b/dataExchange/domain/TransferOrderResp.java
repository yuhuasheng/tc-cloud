package com.foxconn.plm.integrateb2b.dataExchange.domain;

import lombok.Data;

@Data
public class TransferOrderResp {
    private long changeSn;
    private int code;
    private String msg;
    private String changNum;
    private String plantCode;
}
