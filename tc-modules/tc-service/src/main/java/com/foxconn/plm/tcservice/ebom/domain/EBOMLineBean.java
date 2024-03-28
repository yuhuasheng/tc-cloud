package com.foxconn.plm.tcservice.ebom.domain;

import java.util.ArrayList;
import java.util.List;

import com.foxconn.plm.entity.constants.TCPropName;
import com.foxconn.plm.utils.string.StringUtil;

public class EBOMLineBean implements Cloneable {
    // @Expose(serialize = false, deserialize = false)
    private String parentItem;
    private transient String parentRevUid;
    private transient String parentUid;
    private String itemRevUid;
    private List<EBOMLineBean> childs = new ArrayList<>();
    private List<EBOMLineBean> secondSource;

    //
    @TCPropName("bl_sequence_no")
    private Integer findNum;
    //
    @TCPropName("bl_item_item_id")
    private String item;
    //
    @TCPropName("current_revision_id")
    private String version;
    //
    @TCPropName(value = "release_status_list")
    private String status;
    //
    @TCPropName("d9_EnglishDescription")
    private String description;
    @TCPropName("d9_DescriptionSAP")
    private String sapDescription;
    //
    @TCPropName("d9_ManufacturerID")
    private String mfg;
    //
    @TCPropName("d9_ManufacturerPN")
    private String mfgPn;
    //
    @TCPropName(value = "bl_occ_d9_Location", otherVal = "bl_occ_ref_designator")
    private String location;
    //
    @TCPropName(value = "bl_quantity")
    private String qty;
    //
    @TCPropName(value = "bl_occ_d9_PackageType")
    private String packageType;
    @TCPropName(value = "bl_occ_d9_Side")
    private String side;
    //
    @TCPropName("bl_occ_d9_AltGroup")
    private String alternativeGroup;
    //
    @TCPropName("d9_MaterialGroup")
    private String materialGroup;
    //
    @TCPropName("d9_MaterialType")
    private String materialType;
    //
    @TCPropName(value = "d9_Un")
    private String unit;
    private Boolean isModifyTree;
    // @TCPropName("bl_occ_d9_AltCode")
    private String alternativeCode;
    @TCPropName(value = "d9_ProcurementMethods")
    private String procurementType;
    @TCPropName(value = "bl_occ_d9_ReferenceDimension")
    private String referenceDimension;
    @TCPropName(value = "d9_TempPN")
    private String tempPN;
    @TCPropName(value = "d9_SAPRev")
    private String sapRev;
    @TCPropName(value = "d9_SupplierZF")
    private String supplierZF;

    //
    private String uid;
    private Boolean isSecondSource;
    private transient String mainSource;
    private String bomId;
    private String sourceSystem;
    private Boolean isNewVersion;
    private Boolean isModifyItem;
    private String userBu;
    private Boolean isBOMViewWFTask;
    private Boolean isJumpLine;
    private Boolean hasChild;
    private Boolean isCanDcn = true;
    private Boolean canEditPIEE = false;
    private String owner;

    @TCPropName(value = "d9_AcknowledgementRev")
    private String acknowledgementRev;

    @TCPropName(value = "d9_ChineseDescription")
    private String chineseDescription;

    public EBOMLineBean() {
    }

    public String getAcknowledgementRev() {
        return acknowledgementRev;
    }

    public void setAcknowledgementRev(String acknowledgementRev) {
        this.acknowledgementRev = acknowledgementRev;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }


    public String getChineseDescription() {
        return chineseDescription;
    }

    public void setChineseDescription(String chineseDescription) {
        this.chineseDescription = chineseDescription;
    }


    public String getParentItem() {
        return parentItem;
    }

    public void setParentItem(String parentItem) {
        this.parentItem = parentItem;
    }

    public String getBomId() {
        return this.bomId;
    }

    public void setBomId() {
        this.bomId = this.parentItem + "$" + this.getItem();
        if (StringUtil.isNotEmpty(this.mainSource)) {
            this.bomId += this.mainSource;
        }
    }

    public List<EBOMLineBean> getChilds() {
        return childs;
    }


    public void addChild(EBOMLineBean child) {
        this.childs.add(child);
    }

    public Integer getFindNum() {
        return findNum;
    }

    public void setFindNum(Integer findNum) {
        this.findNum = findNum;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMfg() {
        return mfg;
    }

    public void setMfg(String mfg) {
        this.mfg = mfg;
    }

    public String getMfgPn() {
        return mfgPn;
    }

    public void setMfgPn(String mfgPn) {
        this.mfgPn = mfgPn;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<EBOMLineBean> getSecondSource() {
        return secondSource;
    }

    public void setSecondSource(List<EBOMLineBean> secondSource) {
        this.secondSource = secondSource;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Boolean getIsSecondSource() {
        return isSecondSource;
    }

    public void setIsSecondSource(Boolean isSecondSource) {
        this.isSecondSource = isSecondSource;
    }

    public String getMainSource() {
        return mainSource;
    }

    public void setMainSource(String mainSource) {
        this.mainSource = mainSource;
    }

    @Override
    public boolean equals(Object var1) {
        if (var1 instanceof EBOMLineBean) {
            EBOMLineBean other = (EBOMLineBean) var1;
            if (StringUtil.isNotEmpty(this.uid) && StringUtil.isNotEmpty(other.getUid())) {
                return this.uid.equals(other.getUid());
            }
        }
        return super.equals(var1);
    }

    public String toString() {
        return this.uid + " " + this.getFindNum() + "   " + this.getItem() + "  " + this.getLocation();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getQty() {
        return qty;
    }

    public void setQty(String qty) {
        this.qty = qty;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getPackageType() {
        return packageType;
    }

    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }

    @Override
    public EBOMLineBean clone() throws CloneNotSupportedException {
        return (EBOMLineBean) super.clone();
    }

    public void setChilds(List<EBOMLineBean> childs) {
        this.childs = childs;
    }

    public String getParentRevUid() {
        return parentRevUid;
    }

    public void setParentRevUid(String parentRevUid) {
        this.parentRevUid = parentRevUid;
    }

    public String getItemRevUid() {
        return itemRevUid;
    }

    public void setItemRevUid(String itemRevUid) {
        this.itemRevUid = itemRevUid;
    }

    public String getAlternativeGroup() {
        return alternativeGroup;
    }

    public void setAlternativeGroup(String alternativeGroup) {
        this.alternativeGroup = alternativeGroup;
    }

    public String getAlternativeCode() {
        return alternativeCode;
    }

    public void setAlternativeCode(String alternativeCode) {
        this.alternativeCode = alternativeCode;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getMaterialGroup() {
        return materialGroup;
    }

    public void setMaterialGroup(String materialGroup) {
        this.materialGroup = materialGroup;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public Boolean getIsModifyTree() {
        return isModifyTree;
    }

    public void setIsModifyTree(Boolean isModifyTree) {
        this.isModifyTree = isModifyTree;
    }

    public Boolean getIsNewVersion() {
        return isNewVersion;
    }

    public void setIsNewVersion(Boolean isNewVersion) {
        this.isNewVersion = isNewVersion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getProcurementType() {
        return procurementType;
    }

    public void setProcurementType(String procurementType) {
        this.procurementType = procurementType;
    }

    public Boolean getIsModifyItem() {
        return isModifyItem;
    }

    public void setIsModifyItem(Boolean isModifyItem) {
        this.isModifyItem = isModifyItem;
    }

    public String getUserBu() {
        return userBu;
    }

    public void setUserBu(String userBu) {
        this.userBu = userBu;
    }

    public Boolean getIsBOMViewWFTask() {
        return isBOMViewWFTask;
    }

    public void setIsBOMViewWFTask(Boolean isBOMViewWFTask) {
        this.isBOMViewWFTask = isBOMViewWFTask;
    }

    public Boolean getIsJumpLine() {
        return isJumpLine;
    }

    public void setIsJumpLine(Boolean isJumpLine) {
        this.isJumpLine = isJumpLine;
    }

    public Boolean getHasChild() {
        return hasChild;
    }

    public void setHasChild(Boolean hasChild) {
        this.hasChild = hasChild;
    }

    public String getReferenceDimension() {
        return referenceDimension;
    }

    public void setReferenceDimension(String referenceDimension) {
        this.referenceDimension = referenceDimension;
    }

    public String getSapDescription() {
        return sapDescription;
    }

    public void setSapDescription(String sapDescription) {
        this.sapDescription = sapDescription;
    }

    public String getParentUid() {
        return parentUid;
    }

    public void setParentUid(String parentUid) {
        this.parentUid = parentUid;
    }

    public String getSapRev() {
        return sapRev;
    }

    public void setSapRev(String sapRev) {
        this.sapRev = sapRev;
    }

    public Boolean getIsCanDcn() {
        return isCanDcn;
    }

    public void setIsCanDcn(Boolean isCanDcn) {
        this.isCanDcn = isCanDcn;
    }

    public String getSupplierZF() {
        return supplierZF;
    }

    public void setSupplierZF(String supplierZF) {
        this.supplierZF = supplierZF;
    }

    public Boolean getCanEditPIEE() {
        return canEditPIEE;
    }

    public void setCanEditPIEE(Boolean canEditPIEE) {
        this.canEditPIEE = canEditPIEE;
    }
}
