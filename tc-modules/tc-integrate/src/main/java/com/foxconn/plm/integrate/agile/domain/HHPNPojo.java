package com.foxconn.plm.integrate.agile.domain;

import lombok.Data;

@Data
public class HHPNPojo {

    private int index;
    private int isExistInTC=0;//0- 不存在 1-存在
    private String itemId;
    private String mfg;
    private String mfgPN;
    private String unit;
    private String descr;
    private String enDescr;
    private String cnDescr;
    private String materialType;
    private String materialGroup;
    private String procurementType;
    private String dataFrom;
    private String plant;
    private int rohos; //0---G  1- -H
    private int pnms; //0---prd  1-- uat
    private String rev;
    private String customer;
    private String customerPn;
    private String customerPnRev;
}
