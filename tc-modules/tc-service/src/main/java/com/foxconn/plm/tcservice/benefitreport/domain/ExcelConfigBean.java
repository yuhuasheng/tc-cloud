package com.foxconn.plm.tcservice.benefitreport.domain;

import lombok.Data;

@Data
public class ExcelConfigBean {
    private String templateExcel;
    private int sheetIndex;
    private int[] startCellPoint;
    private int[] totalCellPoint;
    private int[] queryDateCellPoint;
    private int[] monthCellPoint;
    private int[] totalMarkStart;
}
