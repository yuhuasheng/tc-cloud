package com.foxconn.plm.integrate.sap.customPN.view;

public class PurchasingView extends SapView {

    private String PURCHASING_SA_STATUS;
    private String PURCHASING_INSPECTION_TYPE;
    private String PURCHASING_MANU_CONTROL_KEY;
    private String PURCHASING_GROUP;
    private String PURCHASING_SOURCELIST;

    public String getPURCHASING_SA_STATUS() {
        return this.PURCHASING_SA_STATUS;
    }

    public void setPURCHASING_SA_STATUS(String purchasing_sa_status) {
        this.PURCHASING_SA_STATUS = purchasing_sa_status;
    }

    public String getPURCHASING_INSPECTION_TYPE() {
        return this.PURCHASING_INSPECTION_TYPE;
    }

    public void setPURCHASING_INSPECTION_TYPE(String purchasing_inspection_type) {
        this.PURCHASING_INSPECTION_TYPE = purchasing_inspection_type;
    }

    public String getPURCHASING_MANU_CONTROL_KEY() {
        return this.PURCHASING_MANU_CONTROL_KEY;
    }

    public void setPURCHASING_MANU_CONTROL_KEY(String purchasing_manu_control_key) {
        this.PURCHASING_MANU_CONTROL_KEY = purchasing_manu_control_key;
    }

    public String getPURCHASING_GROUP() {
        return this.PURCHASING_GROUP;
    }

    public void setPURCHASING_GROUP(String purchasing_group) {
        this.PURCHASING_GROUP = purchasing_group;
    }

    public String toString() {
        return "[PURCHASING_SA_STATUS=" + this.PURCHASING_SA_STATUS +
                ",PURCHASING_INSPECTION_TYPE=" + this.PURCHASING_INSPECTION_TYPE +
                ",PURCHASING_GROUP=" + this.PURCHASING_GROUP +
                ",PURCHASING_MANU_CONTROL_KEY=" + this.PURCHASING_MANU_CONTROL_KEY + "]";
    }

    public void setPURCHASING_SOURCELIST(String pURCHASING_SOURCELIST) {
        this.PURCHASING_SOURCELIST = pURCHASING_SOURCELIST;
    }

    public String getPURCHASING_SOURCELIST() {
        return this.PURCHASING_SOURCELIST;
    }
}