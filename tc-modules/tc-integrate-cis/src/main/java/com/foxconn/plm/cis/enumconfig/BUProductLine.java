package com.foxconn.plm.cis.enumconfig;

/**
 * @Author HuashengYu
 * @Date 2022/9/3 14:39
 * @Version 1.0
 */
public enum BUProductLine {

    MNTPRODUCTLINE("MNT", 2), PRTPRODUCT("PRT", 3);
    private final String bu;
    private final Integer productLine;

    private BUProductLine(String bu, Integer productLine) {
        this.bu = bu;
        this.productLine = productLine;
    }

    public String bu() {
        return bu;
    }

    public Integer productLine() {
        return productLine;
    }
}
