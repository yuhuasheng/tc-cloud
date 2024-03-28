package com.foxconn.plm.tcservice.benefitreport.service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.foxconn.plm.tcservice.benefitreport.domain.BenefitReportBean;
import com.foxconn.plm.utils.date.DateUtil;
import com.foxconn.plm.utils.excel.ExcelUtil;
import com.foxconn.plm.utils.math.MathUtil;
import com.foxconn.plm.utils.string.StringUtil;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;

@Service
public class BenefitReportForPRT {

    public List<BenefitReportBean> exportReportForJson(String fileName, String date, String projectName) throws Exception {
        //String fileName = "D:/【會議資料】TC Baseline收集匯總及效益計算20220624A01.xlsx";
        Workbook wb = ExcelUtil.getLocalWorkbook(fileName);
        wb.setForceFormulaRecalculation(true);
//			Sheet sheet_prt = excelUtils.getSheet(wb, "PRT FTE效益計算");

        List<BenefitReportBean> prtBeanLst = new ArrayList<BenefitReportBean>();
        List<List<String>> rowInfoLst = ExcelUtil.read(wb, "PRT FTE效益計算", 23, 5, 35, 12);
        for (List<String> cellInfoLst : rowInfoLst) {
            if (7 == cellInfoLst.size()) {
                BenefitReportBean prtBean = new BenefitReportBean();
                String custom = cellInfoLst.get(0);
                String difficultLevel = cellInfoLst.get(1);
                String phase = cellInfoLst.get(2);
                if ("效益Total (KUSD)".equals(custom)) {
                    if (StringUtil.isNotEmpty(date)) {
                        custom = difficultLevel = phase = DateUtil.getMonth(date) + "月效益Total (KUSD)";
                    } else if (StringUtil.isNotEmpty(projectName)) {
                        custom = difficultLevel = phase = projectName + "专案效益Total (KUSD)";
                    }
                }
                prtBean.setCustom(custom);
                prtBean.setDifficultLevel(difficultLevel);
                prtBean.setPhase(phase);
                if ("Printer/ID".equals(custom)) {
                    prtBean.setProjectPrinterForPRT(MathUtil.formatDecimal(cellInfoLst.get(4), 0));
                    prtBean.setProjectIIDForPRT(MathUtil.formatDecimal(cellInfoLst.get(5), 0));
                } else {
                    if (!StringUtil.isContainChinese(cellInfoLst.get(4))) {
                        prtBean.setProjectPrinterForPRT(MathUtil.formatDecimal(cellInfoLst.get(4), 2));
                    }

                    if (!StringUtil.isContainChinese(cellInfoLst.get(5))) {
                        prtBean.setProjectIIDForPRT(MathUtil.formatDecimal(cellInfoLst.get(5), 2));
                    }
                }
                prtBean.setBenefit(MathUtil.formatDecimal(cellInfoLst.get(6), 4));

                prtBeanLst.add(prtBean);
            }
        }
        wb.close(); // 关闭此对象，便于后续删除此文件
        return prtBeanLst;
    }

    public ByteArrayOutputStream exportReport(String resultFile, String date, String projectName) throws Exception {
        List<BenefitReportBean> prtBeanLst = exportReportForJson(resultFile, date, projectName);
        String fileName = "/benefitreport/BenefitReport_template.xlsx";
        Workbook wb = ExcelUtil.getWorkbookNew(fileName);
        wb.setActiveSheet(wb.getSheetIndex("PRT FTE效益計算"));
        wb.setSheetHidden(wb.getSheetIndex("DT FTE效益計算"), true);
        wb.setSheetHidden(wb.getSheetIndex("MNT FTE效益計算"), true);
//			wb.setSheetHidden(wb.getSheetIndex("PRT FTE效益計算"), true);
        Sheet sheet_prt = ExcelUtil.getSheet(wb, "PRT FTE效益計算");

        // 设置月份
        String content2 = DateUtil.getMonth(date) + "月效益Total (KUSD)";
        if (StringUtil.isNotEmpty(date)) {
            content2 = DateUtil.getMonth(date) + "月效益Total (KUSD)";
            ExcelUtil.setValueAtForString(sheet_prt, 1, 4, date);
        } else if (StringUtil.isNotEmpty(projectName)) {
            content2 = projectName + " 专案效益Total (KUSD)";
            ExcelUtil.setValueAtForString(sheet_prt, 1, 4, projectName);
        }
        ExcelUtil.setValueAtForString(sheet_prt, 17, 0, content2);

        for (int i = 0; i < prtBeanLst.size(); i++) {
            BenefitReportBean prtBean = prtBeanLst.get(i);
            if (prtBean != null) {
                ExcelUtil.setValueAtForString(sheet_prt, 5 + i, 4, prtBean.getProjectPrinterForPRT());
                ExcelUtil.setValueAtForString(sheet_prt, 5 + i, 5, prtBean.getProjectIIDForPRT());
                ExcelUtil.setValueAtForString(sheet_prt, 5 + i, 6, prtBean.getBenefit());
            }
        }
        wb.setForceFormulaRecalculation(true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close(); // 关闭此对象，便于后续删除此文件
        out.flush();
        return out;

    }

}
