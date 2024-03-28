package com.foxconn.plm.integrateb2b.dataExchange.domain;


import lombok.Data;

@Data
public class MaterialInfo {


    private Long materialSn;
    private String materialChangeNum;
    private String materialNum;
    private String materialRev;
    private String materialPuid;
    private String materialDescriptionEn;
    private String materialDescriptionZf;
    private String materialBaseUnit;
    private String materialGroup;
    private String materialType;
    private String materialMfgPn;
    private String materialMfgId;
    private String materialProcurementType;
    private String materialGrossWeight;
    private String materialNetWeight;
    private String materialWeightUnit;
    private String sapRev;
}
