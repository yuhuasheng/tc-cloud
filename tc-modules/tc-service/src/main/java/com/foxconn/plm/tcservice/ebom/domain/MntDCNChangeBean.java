package com.foxconn.plm.tcservice.ebom.domain;

import java.util.List;

import com.foxconn.plm.entity.constants.TCPropName;
import com.foxconn.plm.tcservice.ebom.service.impl.MntDerivativeCompare;
import com.foxconn.plm.utils.string.StringUtil;

public class MntDCNChangeBean implements Cloneable {
    @TCPropName(cell = 0)
    private String no;
    @TCPropName(cell = 1)
    private String bomItem;
    @TCPropName(cell = 2)
    private String code;
    @TCPropName(cell = 3)
    private String parentPn;
    @TCPropName(cell = 4)
    private String partPn;
    @TCPropName(cell = 5)
    private String des;
    @TCPropName(cell = 6)
    private String location;
    @TCPropName(cell = 7)
    private String supplier;
    @TCPropName(cell = 8)
    private String rev;
    @TCPropName(cell = 9)
    private String before_qty;
    @TCPropName(cell = 10)
    private String after_qty;
    @TCPropName(cell = 11)
    private String unit;
    @TCPropName(cell = 12)
    private String remark;
    private List<MergedRegionInfo> mergedRegionInfos;

    public MntDCNChangeBean(EBOMLineBean ebomBean) {
        this.bomItem = ebomBean.getFindNum() == null ? "" : ebomBean.getFindNum() + "";
        this.partPn = ebomBean.getItem();
        this.parentPn = ebomBean.getParentItem();
        this.des = ebomBean.getDescription();
        this.location = ebomBean.getLocation();
        this.supplier = ebomBean.getMfg();
        this.rev = ebomBean.getVersion();
        this.before_qty = StringUtil.isEmpty(ebomBean.getQty()) ? "1" : ebomBean.getQty();
        this.after_qty = StringUtil.isEmpty(ebomBean.getQty()) ? "1" : ebomBean.getQty();
        this.unit = ebomBean.getUnit();
    }

    public MntDCNChangeBean setDel() {
        this.partPn = MntDerivativeCompare.insertDelLabel(this.partPn);
        this.des = MntDerivativeCompare.insertDelLabel(this.des);
        this.location = MntDerivativeCompare.insertDelLabel(this.location);
        this.supplier = MntDerivativeCompare.insertDelLabel(this.supplier);
        this.after_qty = "0";
        return this;
    }

    public String getNo() {
        return no;
    }

    public void setNo(String no) {
        this.no = no;
    }

    public String getBomItem() {
        return bomItem;
    }

    public void setBomItem(String bomItem) {
        this.bomItem = bomItem;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getParentPn() {
        return parentPn;
    }

    public void setParentPn(String parentPn) {
        this.parentPn = parentPn;
    }

    public String getPartPn() {
        return partPn;
    }

    public void setPartPn(String partPn) {
        this.partPn = partPn;
    }

    public String getDes() {
        return des;
    }

    public void setDes(String des) {
        this.des = des;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getRev() {
        return rev;
    }

    public void setRev(String rev) {
        this.rev = rev;
    }

    public String getBefore_qty() {
        return before_qty;
    }

    public void setBefore_qty(String before_qty) {
        this.before_qty = before_qty;
    }

    public String getAfter_qty() {
        return after_qty;
    }

    public void setAfter_qty(String after_qty) {
        this.after_qty = after_qty;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<MergedRegionInfo> getMergedRegionInfos() {
        return mergedRegionInfos;
    }

    public void setMergedRegionInfos(List<MergedRegionInfo> mergedRegionInfos) {
        this.mergedRegionInfos = mergedRegionInfos;
    }

    @Override
    public MntDCNChangeBean clone() throws CloneNotSupportedException {
        return (MntDCNChangeBean) super.clone();
    }
}
