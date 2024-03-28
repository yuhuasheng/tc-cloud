package com.foxconn.plm.tcservice.benefitreport.service;

import java.io.*;
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
public class BenefitReportForMNT {

    public List<BenefitReportBean> exportReportForJson(String fileName, String date, String projectName) throws Exception {
        //String fileName = "D:/【會議資料】TC Baseline收集匯總及效益計算20220624A01.xlsx";
        Workbook wb = ExcelUtil.getLocalWorkbook(fileName);
        wb.setForceFormulaRecalculation(true);
//			Sheet sheet_mnt = excelUtils.getSheet(wb, "MNT FTE效益計算");

        List<BenefitReportBean> mntBeanLst = new ArrayList<BenefitReportBean>();
        List<List<String>> rowInfoLst = ExcelUtil.read(wb, "MNT FTE效益計算", 142, 5, 208, 11);
        for (List<String> cellInfoLst : rowInfoLst) {
            if (6 == cellInfoLst.size()) {
                BenefitReportBean mntBean = new BenefitReportBean();
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
                mntBean.setCustom(custom);
                mntBean.setDifficultLevel(difficultLevel);
                mntBean.setPhase(phase);
                if ("All".equals(custom)) {
                    mntBean.setProjectAllForMNT(MathUtil.formatDecimal(cellInfoLst.get(4), 0));
                } else {
                    if (!StringUtil.isContainChinese(cellInfoLst.get(4))) {
                        mntBean.setProjectAllForMNT(MathUtil.formatDecimal(cellInfoLst.get(4), 2));
                    }
                }
                mntBean.setBenefit(MathUtil.formatDecimal(cellInfoLst.get(5), 4));

                mntBeanLst.add(mntBean);
            }
        }
        wb.close(); // 关闭此对象，便于后续删除此文件
        return mntBeanLst;
    }

    public ByteArrayOutputStream exportReport(String resultFile, String date, String projectName) throws Exception {

        // AjaxResult jsonData = exportReportForJson();
        List<BenefitReportBean> mntBeanLst = exportReportForJson(resultFile, date, projectName);
        String fileName = "/benefitreport/BenefitReport_template.xlsx";
        Workbook wb = ExcelUtil.getWorkbookNew(fileName);
        wb.setActiveSheet(wb.getSheetIndex("MNT FTE效益計算"));
        wb.setSheetHidden(wb.getSheetIndex("DT FTE效益計算"), true);
//			wb.setSheetHidden(wb.getSheetIndex("MNT FTE效益計算"), true);
        wb.setSheetHidden(wb.getSheetIndex("PRT FTE效益計算"), true);
        Sheet sheet_mnt = ExcelUtil.getSheet(wb, "MNT FTE效益計算");

        // 设置月份
        String content2 = DateUtil.getMonth(date) + "月效益Total (KUSD)";
        if (StringUtil.isNotEmpty(date)) {
            content2 = DateUtil.getMonth(date) + "月效益Total (KUSD)";
            ExcelUtil.setValueAtForString(sheet_mnt, 1, 4, date);
        } else if (StringUtil.isNotEmpty(projectName)) {
            content2 = projectName + " 专案效益Total (KUSD)";
            ExcelUtil.setValueAtForString(sheet_mnt, 1, 4, projectName);
        }

        ExcelUtil.setValueAtForString(sheet_mnt, 71, 0, content2);

        for (int i = 0; i < mntBeanLst.size(); i++) {
            BenefitReportBean mntBean = mntBeanLst.get(i);
            if (mntBean != null) {
                ExcelUtil.setValueAtForString(sheet_mnt, 5 + i, 4, mntBean.getProjectAllForMNT());
                ExcelUtil.setValueAtForString(sheet_mnt, 5 + i, 5, mntBean.getBenefit());
            }
        }

        wb.setForceFormulaRecalculation(true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close(); // 关闭此对象，便于后续删除此文件
        out.flush();
        return out;
//            String newFileName = "D:/BenefitReport_export.xlsx";
//            OutputStream out = new FileOutputStream(new File(newFileName));
//            wb.write(out);
//            out.flush();
//            out.close();
    }

}
