package com.foxconn.plm.tcservice.config;

import com.foxconn.plm.tcservice.benefitreport.domain.ExcelConfigBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExcelConfig {
    @Bean(name = "mntExcelConfig")
    public ExcelConfigBean getMNTBean() {
        ExcelConfigBean excelConfigBean = new ExcelConfigBean();
        excelConfigBean.setTemplateExcel("/benefitreport/MNT2023.xlsx");
        excelConfigBean.setSheetIndex(2);
        excelConfigBean.setQueryDateCellPoint(new int[]{1, 28});
        excelConfigBean.setTotalCellPoint(new int[]{13, 24});
        excelConfigBean.setStartCellPoint(new int[]{4, 10});
        excelConfigBean.setMonthCellPoint(new int[]{13, 1});
        excelConfigBean.setTotalMarkStart(new int[]{4, 17});
        return excelConfigBean;
    }

    @Bean(name = "dtExcelConfig")
    public ExcelConfigBean getDTBean() {
        ExcelConfigBean excelConfigBean = new ExcelConfigBean();
        excelConfigBean.setTemplateExcel("/benefitreport/DT2023.xlsx");
        excelConfigBean.setSheetIndex(2);
        excelConfigBean.setQueryDateCellPoint(new int[]{1, 16});
        excelConfigBean.setTotalCellPoint(new int[]{30, 12});
        excelConfigBean.setStartCellPoint(new int[]{3, 6});
        excelConfigBean.setMonthCellPoint(new int[]{30, 1});
        excelConfigBean.setTotalMarkStart(new int[]{3, 9});
        return excelConfigBean;
    }

    @Bean(name = "prtExcelConfig")
    public ExcelConfigBean getPRTBean() {
        ExcelConfigBean excelConfigBean = new ExcelConfigBean();
        excelConfigBean.setTemplateExcel("/benefitreport/PRT2023.xlsx");
        excelConfigBean.setSheetIndex(2);
        excelConfigBean.setQueryDateCellPoint(new int[]{1, 10});
        excelConfigBean.setTotalCellPoint(new int[]{12, 6});
        excelConfigBean.setStartCellPoint(new int[]{3, 4});
        excelConfigBean.setMonthCellPoint(new int[]{12, 1});
        excelConfigBean.setTotalMarkStart(new int[]{3, 5});
        return excelConfigBean;
    }
}
