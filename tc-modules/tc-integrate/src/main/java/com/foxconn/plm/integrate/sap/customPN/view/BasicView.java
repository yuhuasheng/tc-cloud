package com.foxconn.plm.integrate.sap.customPN.view;


public class BasicView extends SapView {
    private String materialNumber;
    private String industrySector;
    private String materialType;
    private String descriptionEng;
    private String descriptionChi;
    private String materialGroup;
    private String line1;
    private String line2;
    private String oldPartNum;
    private String baseUnit;
    private String perOrderUnit;
    private String orderUnit;
    private String inch;
    private String WEIGHT_UNIT;
    private String NET_WEIGHT;
    private String GROSS_WEIGHT;

    public String getMaterialNumber() {
        return this.materialNumber;
    }

    public void setMaterialNumber(String materialNumber) {
        this.materialNumber = materialNumber;
    }

    public String getIndustrySector() {
        return this.industrySector;
    }

    public void setIndustrySector(String industrySector) {
        //limo
//		this.industrySector = "L";
        this.industrySector = "Z";
    }

    public String getMaterialType() {
        return this.materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public String getDescriptionEng() {
        return this.descriptionEng;
    }

    public void setDescriptionEng(String descriptionEng) {
        this.descriptionEng = descriptionEng;
    }

    public String getDescriptionChi() {
        return this.descriptionChi;
    }

    public void setDescriptionChi(String descriptionChi) {
        this.descriptionChi = descriptionChi;
    }

    public String getMaterialGroup() {
        return this.materialGroup;
    }

    public void setMaterialGroup(String materialGroup) {
        this.materialGroup = materialGroup;
    }

    public String getLine1() {
        return this.line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return this.line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public String getOldPartNum() {
        return this.oldPartNum;
    }

    public void setOldPartNum(String oldPartNum) {
        this.oldPartNum = oldPartNum;
    }

    public String getBaseUnit() {
        return this.baseUnit;
    }

    public void setBaseUnit(String baseUnit) {
        this.baseUnit = baseUnit;
    }

    public String getPerOrderUnit() {
        return this.perOrderUnit;
    }

    public void setPerOrderUnit(String perOrderUnit) {
        this.perOrderUnit = perOrderUnit;
    }

    public String getOrderUnit() {
        return this.orderUnit;
    }

    public void setOrderUnit(String orderUnit) {
        this.orderUnit = orderUnit;
    }

    public String getInch() {
        return this.inch;
    }

    public void setInch(String inch) {
        this.inch = inch;
    }

    public String getWEIGHT_UNIT() {
        return this.WEIGHT_UNIT;
    }

    public void setWEIGHT_UNIT(String weight_unit) {
        this.WEIGHT_UNIT = weight_unit;
    }

    public String getNET_WEIGHT() {
        return this.NET_WEIGHT;
    }

    public void setNET_WEIGHT(String net_weight) {
        this.NET_WEIGHT = net_weight;
    }

    public String getGROSS_WEIGHT() {
        return this.GROSS_WEIGHT;
    }

    public void setGROSS_WEIGHT(String gross_weight) {
        this.GROSS_WEIGHT = gross_weight;
    }


}