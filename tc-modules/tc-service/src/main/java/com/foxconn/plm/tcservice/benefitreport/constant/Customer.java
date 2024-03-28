package com.foxconn.plm.tcservice.benefitreport.constant;

/**
 * @Author HuashengYu
 * @Date 2022/7/13 10:25
 * @Version 1.0
 */
public enum Customer {

    Dell("DELL", "D"), HP("HP", "H"), LENOVO("LENOVO", "L"), ALL("", "All"), PRINTER("", "Printer");
    private final String customerName;
    private final String customerCode;

    private Customer(String customerName, String customerCode) {
        this.customerName = customerName;
        this.customerCode = customerCode;
    }

    public String customerName() {
        return customerName;
    }

    public String customerCode() {
        return customerCode;
    }
}
