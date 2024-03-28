package com.foxconn.plm.integrate.sap.rfc.domain.rp;

import lombok.Data;

@Data
public class MaterialRp {

    private int index;
    private int isExistInTC;//0- 不存在 1-存在
    private String pItemId;
    private String itemId;
    private String mfg;
    private String mfgPN;
    private String unit;
    private String descr;
    private String materialType;
    private String materialGroup;
    private String procurementType;
    private String dataFrom;
    private String plant;
    private int rohos; //
}
