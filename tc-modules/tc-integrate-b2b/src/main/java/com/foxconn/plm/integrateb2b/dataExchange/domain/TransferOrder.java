package com.foxconn.plm.integrateb2b.dataExchange.domain;

import lombok.Data;

@Data
public class TransferOrder {

    private Long changeSn;
    private String changNum;
    private String plantCode;
    private String changeStatus;
    private String effectDate;
    private String changeDescr;
    private String owner;
    private String msg;
    private Integer synFlag;

}
