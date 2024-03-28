package com.foxconn.plm.integrate.sap.rfc.domain.rp;

import lombok.Data;

@Data
public class PNSupplierInfo {
    private String partPn;

    private String mfgPn;

    private String mfg;

    private String mfgZh;

    private String rev;

    private String description; //DESCRIPTION

    private String unit;

    private String materialType; //MATERIAL_TYPE

    private String MaterialGroup; // MATERIAL_GROUP

    private String procurementType; //PROCUREMENT_TYPE

    private String plant;
}
