package com.foxconn.plm.integrate.sap.customPN.view;

public class AccountingView extends SapView {

    private String ACCOUNTING_VALUATION_CLASS;
    private String ACCOUNTING_PRICE_CONTROL;
    private String ACCOUNTION_STANDARD_PRICE;
    private String ACCOUNTION_PRICE_UNIT;
    private String ACCOUNTION_PROFIT_CENTER;
    private String ACCOUNTION_ORIGIN_GROUP;
    private String ACCOUNTING_MOV_AVG_PRICE;

    public String getACCOUNTING_VALUATION_CLASS() {
        return this.ACCOUNTING_VALUATION_CLASS;
    }

    public void setACCOUNTING_VALUATION_CLASS(String accounting_valuation_class) {
        this.ACCOUNTING_VALUATION_CLASS = accounting_valuation_class;
    }

    public String getACCOUNTING_PRICE_CONTROL() {
        return this.ACCOUNTING_PRICE_CONTROL;
    }

    public void setACCOUNTING_PRICE_CONTROL(String accounting_price_control) {
        this.ACCOUNTING_PRICE_CONTROL = accounting_price_control;
    }

    public String getACCOUNTION_STANDARD_PRICE() {
        return this.ACCOUNTION_STANDARD_PRICE;
    }

    public void setACCOUNTION_STANDARD_PRICE(String accountion_standard_price) {
        this.ACCOUNTION_STANDARD_PRICE = accountion_standard_price;
    }

    public String getACCOUNTION_PRICE_UNIT() {
        return this.ACCOUNTION_PRICE_UNIT;
    }

    public void setACCOUNTION_PRICE_UNIT(String accountion_price_unit) {
        this.ACCOUNTION_PRICE_UNIT = accountion_price_unit;
    }

    public String getACCOUNTION_PROFIT_CENTER() {
        return this.ACCOUNTION_PROFIT_CENTER;
    }

    public void setACCOUNTION_PROFIT_CENTER(String accountion_profit_center) {
        this.ACCOUNTION_PROFIT_CENTER = accountion_profit_center;
    }

    public String getACCOUNTION_ORIGIN_GROUP() {
        return this.ACCOUNTION_ORIGIN_GROUP;
    }

    public void setACCOUNTION_ORIGIN_GROUP(String accountion_origin_group) {
        this.ACCOUNTION_ORIGIN_GROUP = accountion_origin_group;
    }


    public String toString() {
        return "[ACCOUNTING_VALUATION_CLASS=" +
                this.ACCOUNTING_VALUATION_CLASS +
                ",ACCOUNTING_PRICE_CONTROL=" + this.ACCOUNTING_PRICE_CONTROL +
                ",ACCOUNTION_STANDARD_PRICE=" +
                this.ACCOUNTION_STANDARD_PRICE + ",ACCOUNTION_PRICE_UNIT=" +
                this.ACCOUNTION_PRICE_UNIT + ",ACCOUNTION_PROFIT_CENTER=" +
                this.ACCOUNTION_PROFIT_CENTER + ",ACCOUNTION_ORIGIN_GROUP=" +
                this.ACCOUNTION_ORIGIN_GROUP + "]";
    }


    public void setACCOUNTING_MOV_AVG_PRICE(String aCCOUNTING_MOV_AVG_PRICE) {
        this.ACCOUNTING_MOV_AVG_PRICE = aCCOUNTING_MOV_AVG_PRICE;
    }

    public String getACCOUNTING_MOV_AVG_PRICE() {
        return this.ACCOUNTING_MOV_AVG_PRICE;
    }
}