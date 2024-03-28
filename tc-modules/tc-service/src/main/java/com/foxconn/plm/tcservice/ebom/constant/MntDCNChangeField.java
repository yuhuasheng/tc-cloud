package com.foxconn.plm.tcservice.ebom.constant;

public enum MntDCNChangeField {
    QTY("qty"), Rev("version"), Des("description"), UNIT("unit"), SUPPLIER("mfg"), LOCATION("");

    private final String value;

    private MntDCNChangeField(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }
}
