package com.foxconn.plm.tcservice.tcfr;

import com.foxconn.plm.entity.response.MegerCellEntity;
import com.foxconn.plm.tcservice.mapper.master.TCFRReportMapper;
import com.foxconn.plm.tcservice.projectReport.ReportEntity;
import com.foxconn.plm.utils.excel.ExcelUtil;
import com.foxconn.plm.utils.file.FileUtil;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Service
public class TCFRReportService {


    @Resource
    TCFRReportMapper mapper;

    public List<BUBean> getBU() {

        return mapper.getBU();

    }

    public List<BUBean> getCustomer(String buid) {

        return mapper.getCustomer(buid);

    }

    public List<BUBean> getProductLine(String custId) {

        return mapper.getProductLine(custId);

    }

    public List<TCFRReportBean> getList(TFCRReportRp rp) {

        List<TCFRReportBean> list = mapper.getList(rp);
        for (TCFRReportBean tcfrReportBean : list) {
            int numOfAccumulatedNotHeld = tcfrReportBean.getNumOfAccumulatedNotHeld();
            int numOfShouldHeld = tcfrReportBean.getNumOfShouldHeld();
            if(numOfShouldHeld==0){
                tcfrReportBean.setRateOfHeld("-");
            }else{
                tcfrReportBean.setRateOfHeld(String.format("%.2f%%",1.0*numOfAccumulatedNotHeld/numOfShouldHeld*100));
            }
        }
        return list;

    }

    public ByteArrayOutputStream export(TFCRReportRp rp) throws Exception {
        FileInputStream fis = null;
        XSSFWorkbook wb = null;
        try {
            List<TCFRReportBean> list = getList(rp);
            List<TCFRReportDetailBean> detailList = mapper.getDetailList(rp);
            // 標記重複項
            String lastFlag = null;
            for (TCFRReportDetailBean detailBean : detailList) {
                String flag = detailBean.getPid() + detailBean.getPhase() + detailBean.getDesignReviewMeeting() + detailBean.getStartDate() + detailBean.getEndDate();
                detailBean.setReduplicate(flag.equals(lastFlag));
                lastFlag = flag;
            }
            File destFile = FileUtil.releaseFile("TCFRReportTemplate.xlsx");
            fis = new FileInputStream(Objects.requireNonNull(destFile));
            wb = new XSSFWorkbook(fis);
            XSSFSheet sheet = wb.getSheetAt(0);
            writeSheet(sheet, list, wb, 2,rp.getWeeks());
            XSSFSheet sheet2 = wb.getSheetAt(1);
            writeDetail(sheet2,detailList,wb,1);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            wb.close(); // 关闭此对象，便于后续删除此文件
            out.flush();
            destFile.delete();
            return out;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if (wb != null) {
                    wb.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeSheet(XSSFSheet sheet, List<TCFRReportBean> list, XSSFWorkbook wb, int start,String weeks) throws IllegalAccessException,
            NoSuchFieldException, ClassNotFoundException {
        XSSFCellStyle cellStyle = wb.createCellStyle();
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setWrapText(true);
        cellStyle.setBorderBottom(BorderStyle.THIN); //下边框
        cellStyle.setBorderLeft(BorderStyle.THIN);//左边框
        cellStyle.setBorderTop(BorderStyle.THIN);//上边框
        cellStyle.setBorderRight(BorderStyle.THIN);//右边框
        // 設置頭上的周
        String[] split = weeks.split(",");
        XSSFRow weekRow = sheet.getRow(1);
        weekRow.getCell(4).setCellValue(split[0]);
        weekRow.getCell(5).setCellValue(split[1]);
        weekRow.getCell(6).setCellValue(split[2]);
        weekRow.getCell(7).setCellValue(split[3]);
        int sumProjectNum = 0;
        int sumW0 = 0;
        int sumW1 = 0;
        int sumW2 = 0;
        int sumW3 = 0;
        int sumNumOfAccumulatedNotHeld = 0;
        int sumNumOfShouldHeld = 0;
        String sumRate = "0.00%";
        for (int i = 0; i < list.size(); i++) {
            TCFRReportBean entity = list.get(i);
            XSSFRow row = sheet.createRow(i + start);
            ExcelUtil.setCellStyleAndValue(row.createCell(0), entity.getBu(), cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(1), entity.getCustomer(), cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(2), entity.getProductLine(), cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(3), entity.getNumOfProject(), cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(4), entity.getW0(), cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(5), entity.getW1(), cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(6), entity.getW2(), cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(7), entity.getW3(), cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(8), entity.getNumOfAccumulatedNotHeld(), cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(9), entity.getNumOfShouldHeld(), cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(10), entity.getRateOfHeld(), cellStyle);
            sumProjectNum+=entity.getNumOfProject();
            sumW0+=entity.getW0();
            sumW1+=entity.getW1();
            sumW2+=entity.getW2();
            sumW3+=entity.getW3();
            sumNumOfAccumulatedNotHeld+=entity.getNumOfAccumulatedNotHeld();
            sumNumOfShouldHeld+=entity.getNumOfShouldHeld();
        }
        if(sumNumOfShouldHeld!=0){
            sumRate = String.format("%.2f%%",1.0*sumNumOfAccumulatedNotHeld / sumNumOfShouldHeld * 100);
        }
        int i = start + list.size();
        XSSFRow sumRon = sheet.createRow(i);
        ExcelUtil.setCellStyleAndValue(sumRon.createCell(0),"Total",cellStyle);
        ExcelUtil.setCellStyleAndValue(sumRon.createCell(1),"Total",cellStyle);
        ExcelUtil.setCellStyleAndValue(sumRon.createCell(2),"Total",cellStyle);
        sheet.addMergedRegion(new CellRangeAddress(i,i,0,2));
        ExcelUtil.setCellStyleAndValue(sumRon.createCell(3),sumProjectNum,cellStyle);
        ExcelUtil.setCellStyleAndValue(sumRon.createCell(4),sumW0,cellStyle);
        ExcelUtil.setCellStyleAndValue(sumRon.createCell(5),sumW1,cellStyle);
        ExcelUtil.setCellStyleAndValue(sumRon.createCell(6),sumW2,cellStyle);
        ExcelUtil.setCellStyleAndValue(sumRon.createCell(7),sumW3,cellStyle);
        ExcelUtil.setCellStyleAndValue(sumRon.createCell(8),sumNumOfAccumulatedNotHeld,cellStyle);
        ExcelUtil.setCellStyleAndValue(sumRon.createCell(9),sumNumOfShouldHeld,cellStyle);
        ExcelUtil.setCellStyleAndValue(sumRon.createCell(10),sumRate,cellStyle);
        ExcelUtil.scanMegerCells3(sheet,list, "bu",start-1,0);
        ExcelUtil.scanMegerCells4(sheet,list, start-1,1,"bu","customer");
    }

    public void writeDetail(XSSFSheet sheet, List<TCFRReportDetailBean> list, XSSFWorkbook wb, int start){
        XSSFCellStyle cellStyle = wb.createCellStyle();
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setWrapText(true);
        cellStyle.setBorderBottom(BorderStyle.THIN); //下边框
        cellStyle.setBorderLeft(BorderStyle.THIN);//左边框
        cellStyle.setBorderTop(BorderStyle.THIN);//上边框
        cellStyle.setBorderRight(BorderStyle.THIN);//右边框
        XSSFCellStyle clone = cellStyle.clone();
        clone.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFColor xssfColor = new XSSFColor(new Color(252, 157, 154), new DefaultIndexedColorMap());
        clone.setFillForegroundColor(xssfColor);
        for (int i = 0; i < list.size(); i++) {
            TCFRReportDetailBean entity = list.get(i);
            XSSFRow row = sheet.createRow(i + start);
            XSSFCellStyle style = entity.isReduplicate() ? clone : cellStyle;
            ExcelUtil.setCellStyleAndValue(row.createCell(0), entity.getBu(), style);
            ExcelUtil.setCellStyleAndValue(row.createCell(1), entity.getCustomer(), style);
            ExcelUtil.setCellStyleAndValue(row.createCell(2), entity.getProductLine(), style);
            ExcelUtil.setCellStyleAndValue(row.createCell(3), entity.getSeries(), style);
            ExcelUtil.setCellStyleAndValue(row.createCell(4), entity.getProjectName(), style);
            ExcelUtil.setCellStyleAndValue(row.createCell(5), entity.getCurrentPhase(), style);
            ExcelUtil.setCellStyleAndValue(row.createCell(6), entity.getPhase(), style);
            ExcelUtil.setCellStyleAndValue(row.createCell(7), entity.getOverallNotHeldRate(), style);
            ExcelUtil.setCellStyleAndValue(row.createCell(8), entity.getDesignReviewMeeting(), style);
            ExcelUtil.setCellStyleAndValue(row.createCell(9), entity.getStartDate(), style);
            ExcelUtil.setCellStyleAndValue(row.createCell(10), entity.getEndDate(), style);
            ExcelUtil.setCellStyleAndValue(row.createCell(11), entity.getNumOfNotHeld(), style);
            ExcelUtil.setCellStyleAndValue(row.createCell(12), entity.getNumOfShouldHeld(), style);
            ExcelUtil.setCellStyleAndValue(row.createCell(13), entity.getNumOfNotHeldRate(), style);
            ExcelUtil.setCellStyleAndValue(row.createCell(14), entity.getSpm(), style);
            ExcelUtil.setCellStyleAndValue(row.createCell(15), entity.getPid(), style);
            ExcelUtil.setCellStyleAndValue(row.createCell(16), entity.getNeedNotifyWeek(), style);
            ExcelUtil.setCellStyleAndValue(row.createCell(17), entity.getYear(), style);
        }
    }


}
