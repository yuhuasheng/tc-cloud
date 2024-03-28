package com.foxconn.plm.extension.avl.domain;

import com.foxconn.plm.extension.config.TCPropertes;
import lombok.Data;

@Data
public class PartModel {


    @TCPropertes(tcProperty = "item_id")
    private String hfPn;

    @TCPropertes(tcProperty = "object_name")
    private String ObjectName;

    @TCPropertes(tcProperty = "d9_StandardPN")
    private String stdPn;

    @TCPropertes(tcProperty = "d9_ManufacturerID")
    private String supplierName;

    @TCPropertes(tcProperty = "d9_ManufacturerPN")
    private String supplierPn;

    @TCPropertes(tcProperty = "d9_PartNumber")
    private String partNumber;

    @TCPropertes(tcProperty = "d9_EnglishDescription")
    private String description;

    @TCPropertes(tcProperty = "d9_CompApproStatus")
    private String compApproval;

    @TCPropertes(tcProperty = "d9_ApproStatus")
    private String approvalStatus;

    @TCPropertes(tcProperty = "d9_HalogenFreeStatus")
    private String hfStatus;

    @TCPropertes(tcProperty = "d9_Remark")
    private String remark;

    //d9_ProcurementMethods d9_MaterialType d9_MaterialGroup  d9_Un

    @TCPropertes(tcProperty = "d9_ProcurementMethods")
    private String procurementMethods = "F";

    @TCPropertes(tcProperty = "d9_MaterialType")
    private String materialType = "ZROH";

    @TCPropertes(tcProperty = "d9_MaterialGroup")
    private String materialGroup;

    @TCPropertes(tcProperty = "d9_Un")
    private String unit;


//    @TCPropertes(tcProperty = "d9_Datasheet")
//    private String currentDatasheet;

    @TCPropertes(tcProperty = "object_type")
    private String objectType;

    @TCPropertes(tcProperty = "object_desc")
    private String objectDesc = "from avl sync";

    public void setHfPn(String hfPn) {
        this.hfPn = hfPn;
        this.ObjectName = hfPn;
        this.objectType = getObjectType();
    }

    public String getObjectType() {
        if (hfPn.matches("^010[1|2|4].*")) {
            return "D9_PCB_Part";
        }
        return "EDAComPart";
    }

    public String toString() {
        return this.getHfPn() + " / " + this.objectType;
    }
}
