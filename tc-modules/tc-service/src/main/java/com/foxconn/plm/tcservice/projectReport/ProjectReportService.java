package com.foxconn.plm.tcservice.projectReport;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.response.MegerCellEntity;
import com.foxconn.plm.tcservice.entity.LOVEntity;
import com.foxconn.plm.tcservice.mapper.infodba.ProjectReportMapper;
import com.foxconn.plm.tcservice.mapper.master.ConfigMapper;
import com.foxconn.plm.utils.excel.ExcelUtil;
import com.foxconn.plm.utils.file.FileUtil;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectReportService {

    @Autowired
    ProjectReportMapper mapper;

    @Autowired
    ConfigMapper configMapper;

    // 催跟郵件當周需完成邏輯配置，可以配置當周或者當月 1表示周，2表示月
    public static int emailTrackCurrentDate = 1;


    public ByteArrayOutputStream export(QueryEntity query) throws Exception {
        FileInputStream fis = null;
        XSSFWorkbook wb = null;
        try {
            List<ReportEntity> list = queryData(query);
            File destFile = FileUtil.releaseFile("ProjectReportExportTemplate.xlsx");
            fis = new FileInputStream(Objects.requireNonNull(destFile));
            wb = new XSSFWorkbook(fis);
            XSSFSheet sheet = wb.getSheetAt(0);
            writeSheet(sheet, list, wb, 2, false);
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

    public void writeSheet(XSSFSheet sheet, List<ReportEntity> list, XSSFWorkbook wb, int start, boolean addRunning) throws IllegalAccessException,
            NoSuchFieldException, ClassNotFoundException {
        XSSFCellStyle cellStyle = wb.createCellStyle();
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setWrapText(true);
        cellStyle.setBorderBottom(BorderStyle.THIN); //下边框
        cellStyle.setBorderLeft(BorderStyle.THIN);//左边框
        cellStyle.setBorderTop(BorderStyle.THIN);//上边框
        cellStyle.setBorderRight(BorderStyle.THIN);//右边框
        for (int i = 0; i < list.size(); i++) {
            ReportEntity entity = list.get(i);
            XSSFRow row = sheet.createRow(i + start);
            ExcelUtil.setCellStyleAndValue(row.createCell(0), entity.bu, cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(1), entity.customer, cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(2), entity.productLine, cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(3), entity.series, cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(4), entity.projectName, cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(5), entity.phase, cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(6), entity.historicalPhase, cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(7), entity.overallOutputProgress + "%", cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(8), entity.dept, cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(9), entity.workflowDiagramDocumentQty, cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(10), entity.archivedQty, cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(11), entity.outputDeliverableQty, cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(12), entity.shouldOutputDeliverableQty, cellStyle);
            ExcelUtil.setCellStyleAndValue(row.createCell(13), entity.outputProgress + "%", cellStyle);


            ExcelUtil.setCellStyleAndValue(row.createCell(14), entity.spm, cellStyle);
            if (addRunning) {
                ExcelUtil.setCellStyleAndValue(row.createCell(15), "Running", cellStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(16), entity.pid, cellStyle);
            } else {
                ExcelUtil.setCellStyleAndValue(row.createCell(15), entity.pid, cellStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(16), entity.currentPhase, cellStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(17), entity.needComplete, cellStyle);
            }
        }
        List<MegerCellEntity> megerCellList0_4 = ExcelUtil.scanMegerCells(list, "pid", start - 1);
        List<MegerCellEntity> megerCellList5_7 = ExcelUtil.scanMegerCells(list, "megerFlag", start - 1);
        //合并单位格
        for (MegerCellEntity megerCellEntity : megerCellList0_4) {
            sheet.addMergedRegion(new CellRangeAddress(megerCellEntity.startRow, megerCellEntity.endRow, 0, 0));
            sheet.addMergedRegion(new CellRangeAddress(megerCellEntity.startRow, megerCellEntity.endRow, 1, 1));
            sheet.addMergedRegion(new CellRangeAddress(megerCellEntity.startRow, megerCellEntity.endRow, 2, 2));
            sheet.addMergedRegion(new CellRangeAddress(megerCellEntity.startRow, megerCellEntity.endRow, 3, 3));
            sheet.addMergedRegion(new CellRangeAddress(megerCellEntity.startRow, megerCellEntity.endRow, 4, 4));
            sheet.addMergedRegion(new CellRangeAddress(megerCellEntity.startRow, megerCellEntity.endRow, 5, 5));
            sheet.addMergedRegion(new CellRangeAddress(megerCellEntity.startRow, megerCellEntity.endRow, 14, 14));
        }
        for (MegerCellEntity megerCellEntity : megerCellList5_7) {
            sheet.addMergedRegion(new CellRangeAddress(megerCellEntity.startRow, megerCellEntity.endRow, 6, 6));
            sheet.addMergedRegion(new CellRangeAddress(megerCellEntity.startRow, megerCellEntity.endRow, 7, 7));
        }
    }

    public List<ReportEntity> queryData(QueryEntity query) {

        // 查询BU
        System.out.println("开始访问数据库");
        long start = System.currentTimeMillis();
        List<ReportEntity> summary = mapper.summary(query);
        List<WorkingDataEntity> workingData = mapper.workingData();
        List<ArchiveDataEntity> archiveData = mapper.archiveData();
        long end = System.currentTimeMillis();
        System.out.println("数据库访问结束，耗时： " + (end - start));

        Map<String, Integer> workingDataGroupMap = new HashMap<>();
        for (WorkingDataEntity entity : workingData) {
            String group = entity.functionName + entity.pid + entity.phase;
            Integer count = workingDataGroupMap.get(group);
            if (count == null) {
                count = 0;
            }
            count++;
            workingDataGroupMap.put(group, count);
        }
        Map<String, Integer> archiveDataGroupMap = new HashMap<>();
        for (ArchiveDataEntity entity : archiveData) {
            String group;
            if (entity.functionName.startsWith("SIM")) {
                group = entity.functionName + entity.pid;
            } else {
                group = entity.functionName + entity.pid + entity.phase;
            }
            Integer count = archiveDataGroupMap.get(group);
            if (count == null) {
                count = 0;
            }
            count++;
            archiveDataGroupMap.put(group, count);
        }
        // 过滤BU
        if (!query.getBu().isEmpty()) {
            summary.removeIf(reportEntity -> !query.getBu().equals(reportEntity.bu));
        }
        for (ReportEntity entity : summary) {
            entity.phaseShort = entity.phase == null || entity.phase.isEmpty() ? "" : entity.phase.substring(0, 2);
            entity.historicalPhaseShort = entity.historicalPhase == null || entity.historicalPhase.isEmpty() ? "" :
                    entity.historicalPhase.substring(0, 2);
            entity.megerFlag = entity.pid + entity.historicalPhaseShort;
            if (entity.outputDeliverableQty == entity.shouldOutputDeliverableQty) {
                entity.outputProgress = 100f;
            } else {
                entity.outputProgress = Float.parseFloat(String.format("%.2f",
                        entity.outputDeliverableQty * 1.0f / (entity.shouldOutputDeliverableQty == 0 ? 1 : entity.shouldOutputDeliverableQty) * 100));
            }
            entity.overallOutputProgressFlag = md5Flag((entity.pid + entity.historicalPhaseShort));
            entity.isCurrentPhase = entity.phaseShort.equals(entity.historicalPhaseShort);
            long phaseEndDate = DateUtil.parseDate(entity.phaseEndDate).getTime();
            long now;
            if (emailTrackCurrentDate == 1) {
                now = DateUtil.endOfWeek(new Date(),true).getTime();
            }else{
                now = DateUtil.endOfMonth(new Date()).getTime();
            }
            entity.isNeedComplete = phaseEndDate <= now;
            entity.currentPhase = entity.isCurrentPhase ?"當前階段":"歷史階段";
            entity.needComplete = entity.isNeedComplete ?"當週需完成":"當週無需完成";
        }

        long curOverallOutputProgressFlag = 0;
        for (int i = 0; i < summary.size(); i++) {
            ReportEntity entity = summary.get(i);
            long overallOutputProgressFlag = entity.overallOutputProgressFlag;
            if (overallOutputProgressFlag == curOverallOutputProgressFlag) {
                entity.overallOutputProgress = summary.get(i - 1).overallOutputProgress;
            } else {
                entity.overallOutputProgress = calcOverallOutputProgress(summary, entity, i);
                curOverallOutputProgressFlag = overallOutputProgressFlag;
            }
            String group;
            if (entity.dept.startsWith("SIM")) {
                group = entity.dept + entity.pid;
            } else {
                group = entity.dept + entity.pid + entity.historicalPhaseShort;
            }
            Integer qty = workingDataGroupMap.get(group);
            if (qty == null) {
                qty = 0;
            }
            entity.workflowDiagramDocumentQty = qty;
            qty = archiveDataGroupMap.get(group);
            if (qty == null) {
                qty = 0;
            }
            entity.archivedQty = qty;
        }
        for (ReportEntity entity : summary) {
            if (entity.outputProgress == 100) {
                entity.status = "完成";
            } else if (entity.outputProgress > entity.overallOutputProgress) {
                entity.status = "ahead";
            } else if (entity.outputProgress <= entity.overallOutputProgress) {
                entity.status = "behind";
            } else if (entity.outputProgress == 0) {
                entity.status = "未完成";
            }
        }
        System.out.println("汇总完毕，耗时：" + (System.currentTimeMillis() - end));
        filter(summary);
        return summary;
    }

    //SIM-SYS/SIM-EI
    public void filter(List<ReportEntity> summary) {
        List<ReportEntity> contrary = new ArrayList<>();
        List<ReportEntity> sysList = new ArrayList<>();
        List<ReportEntity> eiList = new ArrayList<>();
        for (ReportEntity entity : summary) {
            if ("SIM-SYS".equalsIgnoreCase(entity.dept)) {
                sysList.add(entity);
            } else if ("SIM-EI".equalsIgnoreCase(entity.dept)) {
                eiList.add(entity);
            } else {
                contrary.add(entity);
            }
        }
        removeData(sysList, "P1");
        removeData(eiList, "P4");
        summary.clear();
        summary.addAll(contrary);
        summary.addAll(sysList);
        summary.addAll(eiList);
        summary.sort(Comparator.comparing(e -> e.bu + e.customer + e.productLine + e.series + e.projectName + e.phaseShort + e.historicalPhaseShort));
    }

    public void removeData(List<ReportEntity> list, String startPhase) {
        Map<String, String> cacheMap = new HashMap<>();
        if (list != null && list.size() > 0) {
            Map<String, List<ReportEntity>> map = list.stream().collect(Collectors.groupingBy(e -> e.pid));
            list.removeIf(e -> {
                String currentPhase = e.phaseShort;
                String historicalPhase = e.historicalPhaseShort;
                if (startPhase.compareTo(historicalPhase) > 0) {
                    return true;
                } else if (startPhase.equalsIgnoreCase(currentPhase) && startPhase.equalsIgnoreCase(historicalPhase)) {
                    return false;
                } else {
                    List<ReportEntity> projectPhaseList = map.get(e.pid);
                    if (projectPhaseList != null && projectPhaseList.size() > 1) {
                        if (!cacheMap.containsKey(e.pid)) {
                            Map<String, List<ReportEntity>> mapHistroyPhase =
                                    projectPhaseList.stream().collect(Collectors.groupingBy(v -> v.historicalPhaseShort));
                            List<String> histroyPhaseStrs = new ArrayList<>(mapHistroyPhase.keySet());
                            histroyPhaseStrs.sort(Comparator.comparing(String::valueOf).reversed());
                            if (histroyPhaseStrs.size() > 0) {
                                cacheMap.put(e.pid, histroyPhaseStrs.get(1));
                            } else {
                                cacheMap.put(e.pid, histroyPhaseStrs.get(0));
                            }
                        }
                        String cachePhase = cacheMap.get(e.pid);
                        return !cachePhase.equalsIgnoreCase(historicalPhase);
                    } else {
                        return false;
                    }
                }
            });
        }
    }

    public static float calcOverallOutputProgress(List<ReportEntity> list, ReportEntity item, int start) {
        int count = 0;
        float sum = 0;
        for (int i = start; i < list.size(); i++) {
            ReportEntity entity = list.get(i);
            if (item.overallOutputProgressFlag == entity.overallOutputProgressFlag) {
                count++;
                sum += entity.outputProgress;
            } else {
                break;
            }
        }
        if (count == 0) {
            return 0;
        }
        return Float.parseFloat(String.format("%.1f", sum / count));
    }


    public static void calcBu(ReportEntity reportEntity, List<LOVEntity> buList) {
        String s = reportEntity.customer + "_" + reportEntity.productLine;
        for (LOVEntity lovEntity : buList) {
            String value = lovEntity.getValue();
            if (value.equals(s)) {
                reportEntity.bu = lovEntity.getCode();
                return;
            }
        }
    }


    public JSONObject getLovList() {
        List<LovEntity> lov = mapper.getLov();
        List<String> functionList = mapper.getFunction();
        TreeMap<String, Object> customerMap = new TreeMap<>();
        TreeMap<String, Object> productLineMap = new TreeMap<>();
        TreeMap<String, Object> seriesMap = new TreeMap<>();
        for (LovEntity value : lov) {
            customerMap.put(value.customer.trim(), null);
            productLineMap.put(value.productLine.trim(), null);
            seriesMap.put(value.seriesName.trim(), null);
        }
        JSONObject jo = new JSONObject();
        jo.put("customerList", customerMap.keySet());
        jo.put("productLineList", productLineMap.keySet());
        jo.put("seriesList", seriesMap.keySet());
        jo.put("functionList", functionList);
        return jo;
    }

    public static long md5Flag(String str) {
        try {
            // 创建一个MessageDigest实例:
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 反复调用update输入数据:
            md.update(str.getBytes(StandardCharsets.UTF_8));
            byte[] result = md.digest(); // 16 bytes: 68e109f0f40ca72a15e05cc22786f8e6
            return new BigInteger(1, result).longValue();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return str.hashCode();
        }
    }

}
