package com.foxconn.plm.integrate.sap.customPN.view;

public class CostingView extends SapView {

    private String CENTER_CODE;

    public String getCENTER_CODE() {
        return this.CENTER_CODE;
    }

    public void setCENTER_CODE(String center_code) {
        this.CENTER_CODE = center_code;
    }

    public String toString() {
        return "[ CENTER_CODE = " + this.CENTER_CODE + "]";
    }


}