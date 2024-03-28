package com.foxconn.plm.cis.domain;

import com.foxconn.plm.cis.enumconfig.TCPropertes;
import lombok.Data;

@Data
public class PartEntity {
    private String Id;
    @TCPropertes(tcProperty = "d9_PNCategory")
    private String Category;
    @TCPropertes(tcProperty = "d9_ManufacturerID")
    private String MFG;
    @TCPropertes(tcProperty = "d9_ManufacturerPN")
    private String MfgPartNumber;
    private String IsFrequency;
    @TCPropertes(tcProperty = "d9_PartType")
    private String PartType;
    @TCPropertes(tcProperty = "d9_Value")
    private String VALUE;
    @TCPropertes(tcProperty = "d9_Function")
    private String FunctionAlias;
    @TCPropertes(tcProperty = "d9_PackageType")
    private String PackageType;
    @TCPropertes(tcProperty = "d9_PackageSize")
    private String PackageSize;
    @TCPropertes(tcProperty = "d9_Datasheet")
    private String DataSheet;
    @TCPropertes(tcProperty = "d9_PartNumber")
    private String PartNumber;
    @TCPropertes(tcProperty = "d9_ROHSStatus")
    private String RohsStatus;
    @TCPropertes(tcProperty = "d9_FoxconnPartNumber")
    private String FoxconnPartNumber;
    @TCPropertes(tcProperty = "d9_Critical")
    private String CRITICAL;
    @TCPropertes(tcProperty = "d9_CCL")
    private String CCL;
    @TCPropertes(tcProperty = "d9_Description")
    private String DESCRIPTION;
    @TCPropertes(tcProperty = "d9_SchematicPart")
    private String SchematicPart;
    private String FPCategory;
    @TCPropertes(tcProperty = "d9_PCBFootprint")
    private String PCBFootprint;
    @TCPropertes(tcProperty = "d9_BOM")
    private String BOM;
    @TCPropertes(tcProperty = "d9_SubSystem")
    private String SubSystem;
    @TCPropertes(tcProperty = "d9_Remark")
    private String REMARK;
    @TCPropertes(tcProperty = "d9_Side")
    private String SIDE;
    @TCPropertes(tcProperty = "d9_StandardPN")
    private String StandardPN;
    @TCPropertes(tcProperty = "d9_Voltage")
    private String VOLTAGE;
    @TCPropertes(tcProperty = "d9_Tolerance")
    private String TOLERANCE;
    @TCPropertes(tcProperty = "d9_ReflowTemperature")
    private String ReflowTemperature;
    @TCPropertes(tcProperty = "d9_ContactGender")
    private String ContactGender;
    @TCPropertes(tcProperty = "d9_PowerDissipation")
    private String PowerDissipation;
    @TCPropertes(tcProperty = "d9_FoxconnPartNumberNoDT")
    private String FoxconnPartNumberNodt;
    @TCPropertes(tcProperty = "d9_FoxconnPartNumberPRT")
    private String FoxconnPartNumberPrt;
    private String OriginalDrawingFile;
    private String ModifiedDrawingFile;
    private String SyncFlag;
    private String CreatedTime;
    private String UpdatedTime;
    @TCPropertes(tcProperty = "d9_InsertionType")
    private String insertionType;
}
