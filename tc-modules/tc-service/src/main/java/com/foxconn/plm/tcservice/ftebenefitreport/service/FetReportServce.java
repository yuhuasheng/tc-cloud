package com.foxconn.plm.tcservice.ftebenefitreport.service;

import com.foxconn.plm.tcservice.ftebenefitreport.constant.BUEnum;
import com.foxconn.plm.tcservice.ftebenefitreport.constant.FTEConstant;
import com.foxconn.plm.tcservice.ftebenefitreport.constant.FunctionNameEnum;
import com.foxconn.plm.tcservice.ftebenefitreport.domain.FTEBenefitBean;
import com.foxconn.plm.tcservice.ftebenefitreport.domain.FTERecordInfo;
import com.foxconn.plm.tcservice.mapper.master.FTEBenefitReportMapper;
import com.foxconn.plm.utils.collect.CollectUtil;
import com.foxconn.plm.utils.date.DateUtil;
import com.foxconn.plm.utils.excel.ExcelUtil;
import com.foxconn.plm.utils.math.MathUtil;
import com.foxconn.plm.utils.string.StringUtil;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FetReportServce {

    @Resource
    private FTEBenefitReportMapper fteBenefitReportMapper;

    private final static String BACKUP_PATH = "C:" + File.separator + "tc-fte-report" + File.separator + "backup" + File.separator;
    private final static String TEMPLATE_PATH = "C:" + File.separator + "tc-fte-report" + File.separator + "template" + File.separator;
    private final static String HISTORY_PATH = "C:" + File.separator + "tc-fte-report" + File.separator + "history" + File.separator;
    private final static String TOTAL_PATH = "C:" + File.separator + "tc-fte-report" + File.separator + "total" + File.separator;
    private final static String sdf = "yyyy/MM/dd";

    public List<String> checkExcel(Sheet fromSheet, String reportType) throws Exception {
        String toExcelPath = TOTAL_PATH + reportType + ".xlsx";
        Workbook toWb = WorkbookFactory.create(new File(toExcelPath));
        List<String> errorList = new ArrayList<>();
        int startRow = FTEConstant.startRowMap.get(reportType);
        List<List<String>> dataList = ExcelUtil.read(toWb.getSheet(reportType), startRow, -1);
        String[] keyMap = FTEConstant.excelKeyMap.get(reportType);
        Function<List<String>, String> f = e -> {
            StringBuilder keyStr = new StringBuilder();
            for (String key : keyMap) {
                int offset = 0;
                if ("DT_L5".equals(reportType)) {
                    offset = 1;
                }
                keyStr.append(e.get(ExcelUtil.getColumIntByString(key) - offset)).append("=");
            }
            return keyStr.toString();
        };
        List<String> keyTotalList = dataList.stream().filter(Objects::nonNull).map(f).collect(Collectors.toList());
        List<List<String>> fromList = ExcelUtil.read(fromSheet, startRow, -1);
        List<String> keyFromList = fromList.stream().filter(Objects::nonNull).map(f).collect(Collectors.toList());
        String keyJoining = String.join(",", keyMap);
        for (int i = 0; i < keyFromList.size(); i++) {
            if (keyTotalList.contains(keyFromList.get(i))) {
                errorList.add("第 " + (i + startRow) + "行与服务器数据比对后有重复, 重复的列： " + keyJoining);
            }
        }
        return errorList;
    }

    public File backupFile(File excelFile, String reportType) throws IOException {
        File bakFile = new File(TOTAL_PATH + reportType + "-bak.xlsx");
        if (excelFile.exists()) {
            if (bakFile.exists()) {
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd HHmmss");
                String timeStr = inputFormatter.format(LocalDateTime.now());
                if (bakFile.renameTo(new File(BACKUP_PATH + reportType + "-" + timeStr + "-bak.xlsx"))) {
                    bakFile.delete();
                }
            }
            excelFile.renameTo(bakFile);
            excelFile.createNewFile();
        }
        return bakFile;
    }

    /**
     * @param srcSheet
     */
    public void superimposedData(Sheet srcSheet, String reportType) throws IOException {
        String toExcelPath = TOTAL_PATH + reportType + ".xlsx";
        File excelFile = new File(toExcelPath);
        File bakFile = backupFile(excelFile, reportType);
        ZipSecureFile.setMinInflateRatio(-1.0d);
        Workbook toWb = WorkbookFactory.create(bakFile);
        Sheet toSheet = toWb.getSheet(reportType);
        int startRow = FTEConstant.startRowMap.get(reportType);
        int startRdrow = toSheet.getLastRowNum() + 1;
        ExcelUtil.copySheetData(srcSheet, startRow, srcSheet.getLastRowNum(), toSheet);
        revomeRowNotRd(startRdrow, toSheet, reportType);
        if (FTEConstant.DT_L5.equalsIgnoreCase(reportType)) {
            Sheet srcSheet1 = srcSheet.getWorkbook().getSheetAt(1);
            Sheet toSheet1 = toWb.getSheetAt(1);
            ExcelUtil.copySheetData(srcSheet1, 2, srcSheet1.getLastRowNum(), toSheet1);
        }

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(excelFile);
            toWb.write(fileOutputStream);
            toWb.close();
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (toWb != null) {
                toWb.close();
            }

            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    public void revomeRowNotRd(int startRdrow, Sheet toSheet, String reportType) {
        int reduce = 0;
        int lastRowNum = toSheet.getLastRowNum();
        for (; startRdrow < lastRowNum + 1; startRdrow++) {
            boolean isNotRd = false;
            int rowNum = startRdrow - reduce;
            Row row = toSheet.getRow(rowNum);
            if (row == null) {
                continue;
            }
            String isRdStr;
            switch (reportType) {
                case FTEConstant.DT_L5:
                    break;
                case FTEConstant.DT_L6:
                    isRdStr = row.getCell(ExcelUtil.getColumIntByString("N")).getStringCellValue();
                    if (!"是".equalsIgnoreCase(isRdStr)) {
                        isNotRd = true;
                    }
                    break;
                case FTEConstant.DT_L10:
                    isRdStr = row.getCell(ExcelUtil.getColumIntByString("J")).getStringCellValue();
                    if (!"是".equalsIgnoreCase(isRdStr)) {
                        isNotRd = true;
                    }
                    break;
                case FTEConstant.MNT_L5:
                    isRdStr = row.getCell(ExcelUtil.getColumIntByString("J")).getStringCellValue();
                    if (!"RD".equalsIgnoreCase(isRdStr)) {
                        isNotRd = true;
                    }
                    break;
                case FTEConstant.MNT_L6:
                    isRdStr = row.getCell(ExcelUtil.getColumIntByString("I")).getStringCellValue();
                    if (!"RD".equalsIgnoreCase(isRdStr)) {
                        isNotRd = true;
                    }
                    break;
                case FTEConstant.MNT_L10:
                    isRdStr = row.getCell(ExcelUtil.getColumIntByString("M")).getStringCellValue();
                    if (!"RD".equalsIgnoreCase(isRdStr)) {
                        isNotRd = true;
                    }
                    break;
                default:
                    break;
            }
            if (isNotRd) {
                List<Integer> regionIndexs = ExcelUtil.isMergedRowRegion(toSheet, rowNum);
                if (regionIndexs.size() > 0) {
                    toSheet.removeMergedRegions(regionIndexs);
                }
                //toSheet.removeRow(row);
                toSheet.shiftRows(rowNum + 1, lastRowNum + 1, -1);
                reduce++;
            }
        }
    }

    @Transactional
    public List<String> saveData(Sheet fromSheet, String reportType) throws Exception {
        List<String> errorList = new ArrayList<>();
        int startRow = FTEConstant.startRowMap.get(reportType);
        List<List<String>> list = readSheet(fromSheet, startRow, -1, reportType);
        List<FTERecordInfo> beanList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            List<String> data = list.get(i);
            try {
                FTERecordInfo bean = FTERecordInfo.newFTERecordInfo(data, reportType, i + startRow);
                if (bean != null) {
                    beanList.add(bean);
                }
            } catch (Exception e) {
                e.printStackTrace();
                errorList.add(e.getMessage());
            }
        }
        if (errorList.size() > 0) {
            return errorList;
        }

        if (CollectUtil.isNotEmpty(beanList)) {
            fteBenefitReportMapper.insertOrUpdateFTERecord(beanList);
        }
        superimposedData(fromSheet, reportType);
        return errorList;
    }


    public void saveHistoryExcel(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd HHmmss");
        String timeStr = inputFormatter.format(LocalDateTime.now());
        int index = fileName.lastIndexOf(".");
        fileName = fileName.substring(0, index) + "-" + timeStr + fileName.substring(index, fileName.length());
        Path newFile = Paths.get(HISTORY_PATH + fileName);
        Files.createFile(newFile);
        Files.copy(file.getInputStream(), newFile, StandardCopyOption.REPLACE_EXISTING);
    }

    public List<List<String>> readSheet(Sheet sheet, int start, int end, String reportType) throws Exception {
        List<List<String>> list = new ArrayList<List<String>>();
        if (end > sheet.getLastRowNum() || end == -1) {
            end = sheet.getLastRowNum();
        }
        String[] notSplits = FTEConstant.cellSplitMap.get(reportType);
        List<Integer> notSplitList = Stream.of(notSplits).map(ExcelUtil::getColumIntByString).collect(Collectors.toList());
        for (int i = start; i <= end; i++) {
            List<String> rowList = new ArrayList<String>();
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            for (int j = 0; j < row.getLastCellNum(); j++) {
                Cell cell = row.getCell(j);
                if (cell != null) {
                    if (notSplitList.contains(j)) {
                        Object value = ExcelUtil.getCellValue(cell);
                        if (value != null) {
                            rowList.add(value + "");
                        } else {
                            rowList.add(null);

                        }
                    } else {
                        rowList.add(ExcelUtil.getCellValue(sheet, cell, "yyyy/MM/dd"));
                    }
                } else {
                    rowList.add(null);
                }
            }
            list.add(rowList);
        }
        return list;
    }

    public synchronized void downloadExcel(String startDate, String endDate, String reportType, OutputStream out) throws IOException {
        String separator = "/";
        String startYearStr = startDate.split(separator)[0];
        String endYear = endDate.split(separator)[0];
        if (!startYearStr.equalsIgnoreCase(endYear)) {
            throw new RuntimeException("导出明细不可以跨年!");
        }
        int startMonth = Integer.parseInt(startDate.split(separator)[1]);
        int endMonth = Integer.parseInt(endDate.split(separator)[1]);
        int startYear = Integer.parseInt(startYearStr);
        Workbook srcWb = WorkbookFactory.create(new File(TOTAL_PATH + reportType + ".xlsx"));
        Workbook toWb = WorkbookFactory.create(new File(TEMPLATE_PATH + reportType + ".xlsx"));
        Sheet srcSheet = srcWb.getSheetAt(0);
        copySheetData(srcSheet, toWb.getSheetAt(0), FTEConstant.startRowMap.get(reportType), e -> {
            try {
                Cell cell = e.getCell(ExcelUtil.getColumIntByString(FTEConstant.dateCellMap.get(reportType)));
                String dateStr = ExcelUtil.getCellValue(srcSheet, cell, "yyyy/MM/dd");
                String yearStr = dateStr.split("/")[0];
                int year = Integer.parseInt(yearStr);
                int m = Integer.parseInt(dateStr.split("/")[1]);
                return (year + 1 == startYear || year == startYear) && m >= startMonth && m <= endMonth;
            } catch (Exception e1) {
                e1.printStackTrace();
                return false;
            }
        });
        if (FTEConstant.DT_L5.equalsIgnoreCase(reportType)) {
            Sheet srcSheet1 = srcWb.getSheetAt(1);
            copySheetData(srcSheet1, toWb.getSheetAt(1), 2, e -> {
                try {
                    Cell cell = e.getCell(ExcelUtil.getColumIntByString("F"));
                    String dateStr = ExcelUtil.getCellValue(srcSheet1, cell, "yyyy/MM/dd");
                    String startRangeStr = dateStr.split("-")[0].trim();
                    String endRangeStr = dateStr.split("-")[1].trim();
                    String startYStr = startRangeStr.split("/")[0];
                    String endYStr = endRangeStr.split("/")[0];
                    int startY = Integer.parseInt(startYStr);
                    int endY = Integer.parseInt(endYStr);
                    String startMStr = startRangeStr.split("/")[1];
                    String endMStr = endRangeStr.split("/")[1];
                    int startM = Integer.parseInt(startMStr);
                    int endM = Integer.parseInt(endMStr);
                    return ((startYear == startY && startYear == endY) || (startYear - 1 == startY && startYear - 1 == endY)) && ((startMonth >= startM && startMonth <= endM) || (endMonth >= startM && endMonth <= endM));
                } catch (Exception e2) {
                    e2.printStackTrace();
                    return false;
                }
            });
        }
        toWb.write(out);
        srcWb.close();
    }

    public void copySheetData(Sheet srcSheet, Sheet toSheet, int startRow, Predicate<Row> predicate) {
        CellRangeAddress cellAddresses;
        for (int r = startRow; r <= srcSheet.getLastRowNum(); r++) {
            int toStartNum = toSheet.getLastRowNum() + 1;
            Row srcRow = srcSheet.getRow(r);
            if (predicate.test(srcRow)) {
                for (int c = 0; c < srcRow.getLastCellNum(); c++) {
                    cellAddresses = ExcelUtil.getMergedRegion(srcSheet, r, c);
                    if (cellAddresses != null) {
                        int mergedLen = cellAddresses.getLastRow() - cellAddresses.getFirstRow();
                        int tempLen = mergedLen;
                        do {
                            Row tempRow = srcSheet.getRow(r + mergedLen);
                            if (!predicate.test(tempRow)) {
                                mergedLen--;
                            }
                        } while (tempLen-- > 1);
                        if (mergedLen > 0) {
                            cellAddresses.setFirstRow(toStartNum);
                            cellAddresses.setLastRow(toStartNum + mergedLen);
                            toSheet.addMergedRegion(cellAddresses);
                        }
                    }
                    ExcelUtil.copyRow(srcRow, toSheet.createRow(toStartNum));
                }
            }
        }
    }

    public Object getFTEBenefitData(String startTime, String endTime) throws Exception {
        Date startDate = new SimpleDateFormat(sdf).parse(startTime);
        Date endDate = new SimpleDateFormat(sdf).parse(endTime);
        String currentYear = new SimpleDateFormat("yyyy").format(startDate);
        List<FTERecordInfo> list = new ArrayList<>();
        list.addAll(fteBenefitReportMapper.getFTEBenefitRecordn(startDate, endDate));
        Date lastStartDate = DateUtil.getLastYear(startDate);
        Date lastEndDate = DateUtil.getLastYear(endDate);
        list.addAll(fteBenefitReportMapper.getFTEBenefitRecordn(lastStartDate, lastEndDate));
        if (CollectUtil.isEmpty(list)) {
            return "开始月份为: " + startTime + ", 结束月份为: " + endTime + ", 不存在数据";
        }
        removeInvalidRecord(list); // 移除无效记录
        resetFunctionName(list); // 重新设置功能名称
        Map<String, Map<String, Map<String, List<FTERecordInfo>>>> FTEBenefitMap = groupFTEBenefitData(list);
        if (CollectUtil.isEmpty(FTEBenefitMap)) {
            throw new Exception("对FTE效益点信息分组失败");
        }
        return handleGroupData(FTEBenefitMap, currentYear);
    }

    private void resetFunctionName(List<FTERecordInfo> list) {
        list.forEach(bean -> {
            if (FunctionNameEnum.REPAI_MOLD.functionNameCh().equals(bean.getFunctionName())) {
                bean.setFunctionName(FunctionNameEnum.REPAI_MOLD.functionNameEn());
            } else if (FunctionNameEnum.REWORK.functionNameCh().equals(bean.getFunctionName())) {
                bean.setFunctionName(FunctionNameEnum.REWORK.functionNameEn());
            } else if (FunctionNameEnum.STOPLINE.functionNameCh().equals(bean.getFunctionName())) {
                bean.setFunctionName(FunctionNameEnum.STOPLINE.functionNameEn());
            }
        });
    }


    private Map<String, Map<String, Map<String, List<FTERecordInfo>>>> groupFTEBenefitData(List<FTERecordInfo> list) {
        Map<String, Map<String, Map<String, List<FTERecordInfo>>>> retMap = new HashMap<>();
        Map<String, List<FTERecordInfo>> buGroup = list.stream().collect(Collectors.groupingBy(bean -> bean.getBU()));
        buGroup.forEach((k1, v1) -> {
            Map<String, Map<String, List<FTERecordInfo>>> functionNameMap = new HashMap<>();
            Map<String, List<FTERecordInfo>> functionNameGroup = v1.stream().collect(Collectors.groupingBy(bean -> bean.getFunctionName()));
            functionNameGroup.forEach((k2, v2) -> {
                Map<String, List<FTERecordInfo>> yearsGroup = v2.stream().collect(Collectors.groupingBy(bean -> bean.getYears()));
                functionNameMap.put(k2, yearsGroup);
            });
            retMap.put(k1, functionNameMap);
        });
        return retMap;
    }


    /**
     * 处理分组之后的数据
     *
     * @param map
     * @param year
     */
    private List<FTEBenefitBean> handleGroupData(Map<String, Map<String, Map<String, List<FTERecordInfo>>>> map, String year) {
        List<FTEBenefitBean> benefitBeanList = new ArrayList<>();
        map.forEach((buKey, buValue) -> {
            buValue.forEach((functionNameKey, functionNameValue) -> {
                functionNameValue.forEach((yearsKey, yearsValue) -> {
                    FTEBenefitBean bean = new FTEBenefitBean();
                    bean.setBu(buKey);
                    bean.setFunctionName(functionNameKey);
                    bean.setYears(yearsKey);
                    if (BUEnum.DT.name().equals(buKey) && FunctionNameEnum.REPAI_MOLD.functionNameEn().equals(functionNameKey) && year.equals(yearsKey)) {
                        bean.setPredictBenefit(getReworkCostTotal(yearsValue, true));
                    } else {
                        bean.setBenefit(getReworkCostTotal(yearsValue, false));
                    }

                    if (BUEnum.DT.name().equals(buKey) && FunctionNameEnum.REPAI_MOLD.functionNameEn().equals(functionNameKey) && !year.equals(yearsKey)) {
                        return;
                    }
                    benefitBeanList.add(bean);
                });
            });
        });
        return benefitBeanList;
    }


    /**
     * 获取重工金额
     *
     * @param list
     * @return
     */
    private String getReworkCostTotal(List<FTERecordInfo> list, boolean predictFlag) {
        double totalReworkCost = 0.0;
        for (FTERecordInfo bean : list) {
            totalReworkCost += bean.getReworkCost();
        }

        String value = null;
        if (predictFlag) {
            value = MathUtil.formatDecimal(String.valueOf(totalReworkCost), 4);
        } else {
            value = MathUtil.formatDecimal(String.valueOf(totalReworkCost / 1000), 4);
        }
        return value;
    }

    private void removeInvalidRecord(List<FTERecordInfo> list) {
        list.removeIf(bean -> StringUtil.isEmpty(bean.getYears()));
        list.removeIf(bean -> StringUtil.isEmpty(bean.getFunctionName()));
        list.removeIf(bean -> bean.getReworkCost() == null);
    }

}
