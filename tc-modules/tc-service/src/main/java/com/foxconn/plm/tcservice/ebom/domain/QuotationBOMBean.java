package com.foxconn.plm.tcservice.ebom.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.foxconn.plm.entity.constants.TCPropName;
import lombok.Data;

@Data
public class QuotationBOMBean implements Comparable<QuotationBOMBean> {

    @JsonIgnore
    private Integer index = null;
    private String type = "PRI";
    @TCPropName(cell = 0)
    private String item;
    @TCPropName(cell = 1, tcProperty = "item_id")
    private String stdPn;
    @TCPropName(cell = 2, tcProperty = "d9_SupplierZF")
    private String supplier;
    @TCPropName(cell = 3)
    private String supplierPn;
    @TCPropName(cell = 4, tcProperty = "bl_quantity")
    private String qty;
    @TCPropName(cell = 5)
    private String unit;
    @TCPropName(cell = 6, tcProperty = "bl_occ_d9_Location")
    private String location;
    @TCPropName(cell = 7)
    private String bom;
    @TCPropName(cell = 8, tcProperty = "bl_occ_d9_PackageType")
    private String packageType;
    @TCPropName(cell = 9, tcProperty = "bl_occ_d9_Side")
    private String side;
    @TCPropName(cell = 10)
    private String ccL;
    @TCPropName(cell = 11)
    private String reMark;
    @TCPropName(cell = 12)
    private String notes;
    private boolean isSub = false;
    private boolean check = true;

    public static QuotationBOMBean newQuotationBOMBean(String[] beanStrs) {
        QuotationBOMBean bean = new QuotationBOMBean();
        bean.setItem(beanStrs[0]);
        bean.setStdPn(beanStrs[1]);
        bean.setSupplier(beanStrs[2]);
        bean.setSupplierPn(beanStrs[3]);
        bean.setQty(beanStrs[4]);
        bean.setLocation(beanStrs[5]);
        bean.setBom(beanStrs[6]);
        bean.setPackageType(beanStrs[7]);
        bean.setSide(beanStrs[8]);
        bean.setCcL(beanStrs[9]);
        if (beanStrs.length < 11) {
            bean.setReMark("");
        } else {
            bean.setReMark(beanStrs[10]);
        }

        return bean;
    }

    @Override
    public int compareTo(QuotationBOMBean o) {
        int i = this.index.compareTo(o.getIndex());
        if (i == 0) {
            return o.getType().compareTo(this.getType());
        } else {
            return i;
        }
    }
}
