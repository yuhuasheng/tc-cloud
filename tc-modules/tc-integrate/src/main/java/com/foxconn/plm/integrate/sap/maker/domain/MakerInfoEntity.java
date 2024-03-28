package com.foxconn.plm.integrate.sap.maker.domain;

import lombok.Data;

@Data
public class MakerInfoEntity {

    private Long id;
    private String address;
    private String makerCode;
    private String makerName;
    private String makerPN;
    private String tel;
    private String contactMan;
    private String faxNumber;
    private String manufacturerID;
}
