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
public class BenefitReportForDT {

    public List<BenefitReportBean> exportReportForJson(String fileName, String date, String projectName) throws Exception {
        // String fileName = "D:/【會議資料】TC Baseline收集匯總及效益計算20220624A01.xlsx";
        Workbook wb = ExcelUtil.getLocalWorkbook(fileName);
        wb.setForceFormulaRecalculation(true);
//			Sheet sheet_dt = excelUtils.getSheet(wb, "DT FTE效益計算");

        List<BenefitReportBean> dtBeanLst = new ArrayList<BenefitReportBean>();
        List<List<String>> rowInfoLst = ExcelUtil.read(wb, "DT FTE效益計算", 36, 5, 68, 13);
        for (List<String> cellInfoLst : rowInfoLst) {
            if (8 == cellInfoLst.size()) {
                BenefitReportBean dtBean = new BenefitReportBean();
                String custom = cellInfoLst.get(0);
                String difficultLevel = cellInfoLst.get(1);
                String phase = cellInfoLst.get(2);
                if ("效益Total (KUSD)".equals(custom)) {
                    if (StringUtil.isNotEmpty(date)) {
                        custom = difficultLevel = phase = DateUtil.getMonth(date) + "月效益Total (KUSD)";
                    } else if (StringUtil.isNotEmpty(projectName)) {
                        custom = difficultLevel = phase = projectName + " 专案效益Total (KUSD)";
                    }
                }
                dtBean.setCustom(custom);
                dtBean.setDifficultLevel(difficultLevel);
                dtBean.setPhase(phase);
                if ("DHL".equals(custom)) {
                    dtBean.setProjectDellForDT(MathUtil.formatDecimal(cellInfoLst.get(4), 0));
                    dtBean.setProjectHPForDT(MathUtil.formatDecimal(cellInfoLst.get(5), 0));
                    dtBean.setProjectLenovoForDT(MathUtil.formatDecimal(cellInfoLst.get(6), 0));
                } else {
                    if (!StringUtil.isContainChinese(cellInfoLst.get(4))) {
                        dtBean.setProjectDellForDT(MathUtil.formatDecimal(cellInfoLst.get(4), 2));
                    }

                    if (!StringUtil.isContainChinese(cellInfoLst.get(5))) {
                        dtBean.setProjectHPForDT(MathUtil.formatDecimal(cellInfoLst.get(5), 2));
                    }

                    if (!StringUtil.isContainChinese(cellInfoLst.get(6))) {
                        dtBean.setProjectLenovoForDT(MathUtil.formatDecimal(cellInfoLst.get(6), 2));
                    }
                }
                dtBean.setBenefit(MathUtil.formatDecimal(cellInfoLst.get(7), 4));

                dtBeanLst.add(dtBean);
            }
        }

//			dtBeanLst.forEach(bean -> {
//				System.out.println(bean.toString());
//			});
        wb.close(); // 关闭此对象，便于后续删除此文件
        return dtBeanLst;

//			OutputStream out = new FileOutputStream(new File(fileName));
//			wb.write(out);
//			out.flush();
//			out.close();

    }

    public ByteArrayOutputStream exportReport(String reulstFile, String date, String projectName) throws Exception {
        List<BenefitReportBean> dtBeanLst = exportReportForJson(reulstFile, date, projectName);
        String fileName = "/benefitreport/BenefitReport_template.xlsx";
        Workbook wb = ExcelUtil.getWorkbookNew(fileName);
        wb.setActiveSheet(wb.getSheetIndex("DT FTE效益計算"));
//			wb.setSheetHidden(wb.getSheetIndex("DT FTE效益計算"), true);
        wb.setSheetHidden(wb.getSheetIndex("MNT FTE效益計算"), true);
        wb.setSheetHidden(wb.getSheetIndex("PRT FTE效益計算"), true);
        Sheet sheet_dt = ExcelUtil.getSheet(wb, "DT FTE效益計算");

        // 设置月份
        String content2 = "";
        if (StringUtil.isNotEmpty(date)) {
            content2 = DateUtil.getMonth(date) + "月效益Total (KUSD)";
            ExcelUtil.setValueAtForString(sheet_dt, 1, 5, date);
        } else if (StringUtil.isNotEmpty(projectName)) {
            content2 = projectName + " 专案效益Total (KUSD)";
            ExcelUtil.setValueAtForString(sheet_dt, 1, 5, projectName);
        }
        ExcelUtil.setValueAtForString(sheet_dt, 37, 1, content2);

        for (int i = 0; i < dtBeanLst.size(); i++) {
            BenefitReportBean dtBean = dtBeanLst.get(i);
            if (dtBean != null) {
                ExcelUtil.setValueAtForString(sheet_dt, 5 + i, 5, dtBean.getProjectDellForDT());
                ExcelUtil.setValueAtForString(sheet_dt, 5 + i, 6, dtBean.getProjectHPForDT());
                ExcelUtil.setValueAtForString(sheet_dt, 5 + i, 7, dtBean.getProjectLenovoForDT());
                ExcelUtil.setValueAtForString(sheet_dt, 5 + i, 8, dtBean.getBenefit());
            }
        }

        wb.setForceFormulaRecalculation(true);
        //String newFileName = "D:/BenefitReport_export.xlsx";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close(); // 关闭此对象，便于后续删除此文件
        out.flush();
        return out;
//            OutputStream out = new FileOutputStream(new File(newFileName));
//            wb.write(out);
//            out.flush();
//            out.close();
    }

}
