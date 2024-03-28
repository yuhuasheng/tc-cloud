package com.foxconn.plm.tcservice.benefitreport.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.entity.response.SPASProject;
import com.foxconn.plm.feign.service.TcIntegrateClient;
import com.foxconn.plm.tcservice.benefitreport.constant.BU;
import com.foxconn.plm.tcservice.benefitreport.constant.ExcelConfig;
import com.foxconn.plm.tcservice.benefitreport.constant.ProjectDifficulty;
import com.foxconn.plm.tcservice.benefitreport.domain.BenefitCollectBean;
import com.foxconn.plm.tcservice.benefitreport.domain.ExcelConfigBean;
import com.foxconn.plm.tcservice.benefitreport.service.SpasProjectService;
import com.foxconn.plm.utils.excel.ExcelUtil;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.*;
import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SpasProjectServiceMVP2 implements SpasProjectService {

    @Resource
    private TcIntegrateClient tcIntegrate;

    @Resource
    private ExcelConfigBean dtExcelConfig;

    @Resource
    private ExcelConfigBean mntExcelConfig;

    @Resource
    private ExcelConfigBean prtExcelConfig;

    private static Log log = LogFactory.get();
    private static final List<String> PHASES = ListUtil.of("P0", "P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8");
    private static final List<String> MNT_TEMPLATE_LEVEL = ListUtil.of("A0", "A", "B", "C", "D", "E", "F");
    private static final List<String> DT_TEMPLATE_LEVEL = ListUtil.of("E1", "E2", "E3");
    private static final List<String> PRT_TEMPLATE_LEVEL = ListUtil.of("E1");
    private static final String[] DT_Customer = new String[]{"Dell", "HP", "Lenovo"};

    List<Integer[]> dtClassify(List<SPASProject> projectList) {
        List<Integer[]> all = new ArrayList<>();
        Map<String, List<SPASProject>> map = projectList.
                stream().filter(e -> BU.DT.toString().equalsIgnoreCase(e.getBu())).collect(Collectors.groupingBy(SPASProject::getCustomer,
                Collectors.toList()));
        for (String customer : DT_Customer) {
            List<SPASProject> customerProjectList = map.get(customer);
            if (customerProjectList == null || customerProjectList.size() == 0) {
                all.addAll(initList());
            } else {
                List<Integer[]> counts = classifyByLevel(customerProjectList, DT_TEMPLATE_LEVEL);
                all.addAll(counts);
            }
        }
        return all;
    }

    List<Integer[]> mntClassify(List<SPASProject> projectList) {
        List<Integer[]> all = new ArrayList<>();
        List<SPASProject> mntList = projectList.stream().filter(e -> BU.MNT.toString().equalsIgnoreCase(e.getBu())).collect(Collectors.toList());
        if (mntList.size() > 0) {
            List<Integer[]> counts = classifyByLevel(mntList, MNT_TEMPLATE_LEVEL);
            all.addAll(counts);
        }
        return all;
    }

    List<Integer[]> prtClassify(List<SPASProject> projectList) {
        List<Integer[]> all = new ArrayList<>();
        List<SPASProject> prtList = projectList.stream().filter(e -> BU.PRT.toString().equalsIgnoreCase(e.getBu())).collect(Collectors.toList());
        if (prtList.size() > 0) {
            List<Integer[]> counts = classifyByLevel(prtList, PRT_TEMPLATE_LEVEL);
            all.addAll(counts);
        }
        return all;
    }


    List<Integer[]> initList() {
        return new ArrayList<>(ListUtil.of(null, null, null, null, null, null, null, null, null));
    }

    // P0~P8
    List<Integer[]> classifyByLevel(List<SPASProject> projectList, List<String> templateArray) {
        List<Integer[]> list = initList();
        Map<String, List<SPASProject>> phaseMap = projectList.stream().collect(Collectors.groupingBy(SPASProject::getPhase, Collectors.toList()));
        for (Map.Entry<String, List<SPASProject>> entry : phaseMap.entrySet()) {
            String phase = entry.getKey();
            List<SPASProject> listByPhase = entry.getValue();
            Integer[] levels = new Integer[templateArray.size()];
            Map<String, Integer> mapLevel = listByPhase.stream().collect(Collectors.toMap(SPASProject::getLevels,
                    e -> 1, (v1, v2) -> ++v1));
            for (Map.Entry<String, Integer> levelEntry : mapLevel.entrySet()) {
                int levelIndex = templateArray.indexOf(levelEntry.getKey());
                if (levelIndex > -1) {
                    levels[levelIndex] = levelEntry.getValue();
                }
            }
            int index = PHASES.indexOf(phase.toUpperCase());
            list.set(index, levels);
        }
        return list;
    }


    public List<SPASProject> getProjectList(String startDate) throws ParseException {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate dateTime = LocalDate.parse(startDate, df);
        LocalDate firstDate = LocalDate.of(dateTime.getYear(), dateTime.getMonth(), 1);
        LocalDate lastDate = dateTime.with(TemporalAdjusters.lastDayOfMonth());
        String firstDateStr = df.format(firstDate);
        String lastDateStr = df.format(lastDate);
        List<SPASProject> list = tcIntegrate.getClosedProjectsByDate(firstDateStr, lastDateStr, "");
        if (list != null) {
            list = filter(list);
        }
        return list;
    }

    @Override
    public R<BenefitCollectBean> getSpasProjectByDate(String startDate, String bu, Workbook wb, Map<String, Integer> sheetIndexMap) {
        BenefitCollectBean bean = new BenefitCollectBean();
        try {
            if (judgeMVP2(startDate)) {
                List<SPASProject> projectList = getProjectList(startDate);
                //bean = get2023MVP2(projectList);
                bean = get2023MVP2(projectList, startDate);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage());
        }
        bean.setName("2023效益點");
        return R.success(bean);
    }


    public boolean judgeMVP2(String startDate) {
        return startDate.compareTo(ExcelConfig.MVP2_START_DATE) >= 0;
    }

    public Map<String, Integer> getProjectCount(List<SPASProject> list, BU bu) {
        return list.stream().filter(e -> bu.toString().equalsIgnoreCase(e.getBu())).collect(Collectors.toMap((e -> e.getPhase() + e.getLevels()),
                e -> 1, (v1, v2) -> ++v1));
    }

    @Override
    public R getSingleSpasProject(String projectId, String projectName, String bu) {
        return null;
    }

    @Override
    public void wirteExcelFor2022(Workbook wb, String bu, String startDate) throws ParseException {
//        String orignlSheetName0 = wb.getSheetName(0);
//        String orignlSheetName1 = wb.getSheetName(1);
//        String sheetName;
//        if (BU.DT.name().equals(bu)) {
//            sheetName = "DT FTE效益計算";
//        }else {
//            sheetName = "MNT FTE效益計算";
//        }
//        String sheetName = wb.cloneSheet(0).getSheetName();
//        wb.setSheetName(0,"sheetName");
//        wb.setSheetName(1,"专案清单");


    }

    @Override
    public void wirteExcel(Workbook wb, String bu, String startDate) throws ParseException {
        List<SPASProject> list = new ArrayList<>();
        if (!judgeMVP2(startDate)) {
        } else {
            list = getProjectList(startDate);
        }
        list.removeIf(e -> !bu.equalsIgnoreCase(e.getBu()));
        List<Integer[]> counts = null;
        ExcelConfigBean excelConfigBean = null;
        switch (BU.valueOf(bu)) {
            case DT:
                counts = dtClassify(list);
                excelConfigBean = dtExcelConfig;
                break;
            case MNT:
                counts = mntClassify(list);
                excelConfigBean = mntExcelConfig;
                break;
            case PRT:
                counts = prtClassify(list);
                excelConfigBean = prtExcelConfig;

        }
//        if (BU.DT.toString().equalsIgnoreCase(bu)) {
//            counts = dtClassify(list);
//            excelConfigBean = dtExcelConfig;
//        } else if (BU.MNT.toString().equalsIgnoreCase(bu)) {
//            counts = mntClassify(list);
//            excelConfigBean = mntExcelConfig;
//        }
        if (counts != null) {
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate dateTime = LocalDate.parse(startDate, df);
            LocalDate firstDate = LocalDate.of(dateTime.getYear(), dateTime.getMonth(), 1);
            writeTempExcel(wb, excelConfigBean, firstDate, counts);
            try {
                Set<String> totalMarkList = getBenefitMark(wb.getSheetAt(excelConfigBean.getSheetIndex()), bu, excelConfigBean.getTotalMarkStart());
                list.removeIf(e -> {
                    String customerName = e.getCustomer();
                    if (BU.MNT.toString().equalsIgnoreCase(bu) || BU.PRT.toString().equalsIgnoreCase(bu)) {
                        customerName = "all";
                    }
                    return !totalMarkList.contains((customerName + e.getPhase() + e.getLevels()).toUpperCase());
                });
                Sheet projectSheet = wb.getSheetAt(3);
                ExcelUtil.setCellValue(list, 1, 7, projectSheet, ExcelUtil.getCellStyle(projectSheet.getWorkbook()));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void wirteExcel_(Workbook wb, String bu, String startDate) throws ParseException {
        if (!judgeMVP2(startDate)) return;
        List<SPASProject> list = getProjectList(startDate);
        list.removeIf(e -> !bu.equalsIgnoreCase(e.getBu()));
        if (BU.DT.toString().equalsIgnoreCase(bu)) {
            list.removeIf(e -> !"Dell".equalsIgnoreCase(e.getCustomer()));
        }
        BU buEnum = BU.valueOf(bu);
        Map<String, Integer> projectCount = getProjectCount(list, buEnum);
        int offSet = 0;
        Map<String, Integer[]> configMap = null;
        int m = Integer.parseInt(startDate.split("-")[1]);
        int[] mPoint = new int[2];
        switch (buEnum) {
            case MNT:
                offSet = ExcelConfig.MNT_FILL_OFFSET;
                configMap = ExcelConfig.MNT_BENEFIT_CONFIG;
                mPoint[0] = 1;
                mPoint[1] = 44;
                break;
            case DT:
                offSet = ExcelConfig.DT_FILL_OFFSET;
                configMap = ExcelConfig.DT_BENEFIT_CONFIG;
                mPoint[0] = 1;
                mPoint[1] = 19;
                break;
            default:
                break;
        }
        Sheet sheet = wb.getSheetAt(2);
        Cell mCell = sheet.getRow(mPoint[1]).getCell(mPoint[0]);
        mCell.setCellValue(m + mCell.getStringCellValue());
        if (projectCount.size() > 0 && configMap != null) {
            for (Map.Entry<String, Integer> entry : projectCount.entrySet()) {
                Integer[] points = configMap.get(entry.getKey());
                sheet.getRow(points[1]).getCell(points[0] + offSet).setCellValue(entry.getValue());
                sheet.setForceFormulaRecalculation(true);
            }
        }
        try {
            Sheet projectSheet = wb.getSheetAt(3);
            ExcelUtil.setCellValue(list, 1, 7, projectSheet, ExcelUtil.getCellStyle(projectSheet.getWorkbook()));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    private List<SPASProject> filter(List<SPASProject> list) {
        return list.stream().filter(e -> (BU.MNT.toString().equalsIgnoreCase(e.getBu()) || BU.DT.toString().equalsIgnoreCase(e.getBu()) || BU.PRT.toString().equalsIgnoreCase(e.getBu())) && StringUtils.hasLength(e.getCustomer())).map(this::updateLevel).peek(e -> {
            if (e.getCustomer().startsWith("Lenovo")) {
                e.setCustomer("Lenovo");
            }
        }).collect(Collectors.toList());
    }

    /**
     * 更新难易程度为系统难易程度
     */
    private SPASProject updateLevel(SPASProject info) {
        String bu = info.getBu();
        String levels = info.getLevels();
        if (levels != null) {
            String[] split = levels.split(",");
            if (split.length > 1) {
                String str = split[1].toUpperCase();
                if (!BU.MNT.name().equals(bu) && str.contains(ProjectDifficulty.E1.name())) {
                    info.setLevels(ProjectDifficulty.E1.name());
                } else if (!BU.MNT.name().equals(bu) && str.contains(ProjectDifficulty.E2.name())) {
                    info.setLevels(ProjectDifficulty.E2.name());
                } else if (!BU.MNT.name().equals(bu) && str.contains(ProjectDifficulty.E3.name())) {
                    info.setLevels(ProjectDifficulty.E3.name());
                } else if (BU.MNT.name().equals(bu) && str.contains(ProjectDifficulty.A0.name())) {
                    info.setLevels(ProjectDifficulty.A0.name());
                } else if (BU.MNT.name().equals(bu) && str.contains(ProjectDifficulty.A.name())) {
                    info.setLevels(ProjectDifficulty.A.name());
                } else if (BU.MNT.name().equals(bu) && str.contains(ProjectDifficulty.B.name())) {
                    info.setLevels(ProjectDifficulty.B.name());
                } else if (BU.MNT.name().equals(bu) && str.contains(ProjectDifficulty.C.name())) {
                    info.setLevels(ProjectDifficulty.C.name());
                } else if (BU.MNT.name().equals(bu) && str.contains(ProjectDifficulty.D.name())) {
                    info.setLevels(ProjectDifficulty.D.name());
                } else if (BU.MNT.name().equals(bu) && str.contains(ProjectDifficulty.E.name())) {
                    info.setLevels(ProjectDifficulty.E.name());
                } else if (BU.MNT.name().equals(bu) && str.contains(ProjectDifficulty.F.name())) {
                    info.setLevels(ProjectDifficulty.F.name());
                }
            } else {
                log.info("spas level is error : " + info.getProjectName() + "  id: " + info.getProjectId() + "  level : " + info.getLevels());
            }
        } else {
            log.info("spas level is error : " + info.getProjectName() + "  id: " + info.getProjectId() + "  level : " + info.getLevels());
        }
        return info;
    }

    public Map<String, String> loadBenefitExcelData(String excelPath, Map<String, Integer[]> configMap) {
        Map<String, String> benefitMapping = new HashMap<>();
        Workbook wb = ExcelUtil.getWorkbookNew(excelPath);
        Sheet sheet = wb.getSheetAt(2);
        configMap.forEach((k, v) -> {
            Cell cell = sheet.getRow(v[1]).getCell(v[0]);
            benefitMapping.put(k, ExcelUtil.getCellValue(cell) + "");
        });
        try {
            wb.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return benefitMapping;
    }


    public BenefitCollectBean get2023MVP2(List<SPASProject> list) {
        BenefitCollectBean benefitCollectBean = new BenefitCollectBean();
        benefitCollectBean.setMnt(summation(list, BU.MNT));
        benefitCollectBean.setDt(summation(list, BU.DT));
        return benefitCollectBean;
    }

    public BenefitCollectBean get2023MVP2(List<SPASProject> list, String startDate) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate dateTime = LocalDate.parse(startDate, df);
        LocalDate firstDate = LocalDate.of(dateTime.getYear(), dateTime.getMonth(), 1);
        BenefitCollectBean benefitCollectBean = new BenefitCollectBean();
        List<Integer[]> dtList = dtClassify(list);
        List<Integer[]> mntList = mntClassify(list);
        List<Integer[]> prtList = prtClassify(list);
        benefitCollectBean.setDt(getBenefitByBU(BU.DT, firstDate, dtList));
        benefitCollectBean.setMnt(getBenefitByBU(BU.MNT, firstDate, mntList));
        benefitCollectBean.setPrt(getBenefitByBU(BU.PRT, firstDate, prtList));
        return benefitCollectBean;
    }

    public BigDecimal summation(List<SPASProject> list, BU bu) {
        if (BU.DT.equals(bu)) {
            list.removeIf(e -> !"Dell".equalsIgnoreCase(e.getCustomer()));
        }
        BigDecimal result = new BigDecimal(0);
        Map<String, Integer> count = getProjectCount(list, bu);
        Map<String, BigDecimal> money = getBenefitByBU(bu, count);
        if (money.size() > 0) {
            result = money.values().stream().reduce(new BigDecimal(0), BigDecimal::add);
        }
        return result;
    }

    public Map<String, BigDecimal> getBenefitByBU(BU bu, Map<String, Integer> projectCountMap) {
        Map<String, BigDecimal> benefitResult = new HashMap<>();
        String excelPath = "";
        Map<String, Integer[]> configMap = null;
        switch (bu) {
            case MNT:
                excelPath = ExcelConfig.MNT_TEMPLATE;
                configMap = ExcelConfig.MNT_BENEFIT_CONFIG;
                break;
            case DT:
                excelPath = ExcelConfig.DT_TEMPLATE;
                configMap = ExcelConfig.DT_BENEFIT_CONFIG;
                break;
            default:
                break;
        }
        if (configMap != null) {
            Map<String, String> formulaMap = loadBenefitExcelData(excelPath, configMap);
            projectCountMap.forEach((k, v) -> {
                String benefitMoney = formulaMap.get(k);
                BigDecimal benefitDecimal = new BigDecimal(benefitMoney);
                benefitResult.put(k, benefitDecimal.multiply(new BigDecimal(v)));
            });
        }
        return benefitResult;
    }

    BigDecimal getBenefitByBU(BU bu, LocalDate queryDate, List<Integer[]> datas) {
        ExcelConfigBean excelConfigBean = null;
//        if (bu.equals(BU.DT)) {
//            excelConfigBean = dtExcelConfig;
//        } else if (bu.equals(BU.MNT)) {
//            excelConfigBean = mntExcelConfig;
//        } else if (bu.equals(BU.PRT)) {
//            excelConfigBean = prtExcelConfig;
//        }
        switch (bu) {
            case DT:
                excelConfigBean = dtExcelConfig;
                break;
            case MNT:
                excelConfigBean = mntExcelConfig;
                break;
            case PRT:
                excelConfigBean = prtExcelConfig;
                break;
            default:
                break;
        }
        Assert.isTrue(excelConfigBean != null);
        File tempFile = null;
        OutputStream out = null;
        try (Workbook tempExcel = ExcelUtil.getWorkbookNew(excelConfigBean.getTemplateExcel())) {
            writeTempExcel(tempExcel, excelConfigBean, queryDate, datas);
            tempFile = File.createTempFile(bu.toString() + "_benefitReport_", ".xlsx");
            out = new FileOutputStream(tempFile);
            tempExcel.write(out);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(out!=null) {
                    out.close();
                }
            }catch (IOException e){}
        }
        Assert.isTrue(tempFile != null);
        Workbook wb = null;
        try {
            wb = WorkbookFactory.create(tempFile);
            Sheet sheet = wb.getSheetAt(excelConfigBean.getSheetIndex());
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
            Cell cell = sheet.getRow(excelConfigBean.getTotalCellPoint()[0]).getCell(excelConfigBean.getTotalCellPoint()[1]);
            double totalBeneFit = evaluator.evaluate(cell).getNumberValue();
            return new BigDecimal(totalBeneFit + "");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(wb!=null) {
                    wb.close();
                }
            }catch (IOException e){}
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
        return null;
    }

    void writeTempExcel(Workbook wb, ExcelConfigBean excelConfigBean, LocalDate queryDate, List<Integer[]> datas) {
        if (excelConfigBean != null) {
            try {
                Sheet sheet = wb.getSheetAt(excelConfigBean.getSheetIndex());
                Cell dateCell = sheet.getRow(excelConfigBean.getQueryDateCellPoint()[0]).getCell(excelConfigBean.getQueryDateCellPoint()[1]);
                dateCell.setCellValue(queryDate);
                writeExcel(sheet, excelConfigBean.getStartCellPoint(), datas);
                sheet.setForceFormulaRecalculation(true);
                int[] mouthCell = excelConfigBean.getMonthCellPoint();
                Cell cell = sheet.getRow(mouthCell[0]).getCell(mouthCell[1]);
                cell.setCellValue(queryDate.getMonth().getValue() + cell.getStringCellValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void writeExcel(Sheet sheet, int[] startCell, List<Integer[]> datas) {
        for (int i = 0; i < datas.size(); i++) {
            Integer[] rowData = datas.get(i);
            if (rowData != null) {
                for (int c = 0; c < rowData.length; c++) {
                    if (rowData[c] != null) {
                        Cell cell = sheet.getRow(startCell[0] + i).getCell(startCell[1] + c);
                        cell.setCellValue(rowData[c]);
                    }
                }
            }
        }
    }

    public Set<String> getBenefitMark(Sheet sheet, String bu, int[] totalStart) {
        Set<String> totals = new HashSet<>();
        FormulaEvaluator evaluator = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator();
        String[] customers = new String[]{"all"};
        List<String> levelList = MNT_TEMPLATE_LEVEL;
        switch (BU.valueOf(bu)) {
            case DT:
                customers = DT_Customer;
                levelList = DT_TEMPLATE_LEVEL;
                break;
            case PRT:
                levelList = PRT_TEMPLATE_LEVEL;
                break;
            default:
                break;
        }
        int readRowLen = customers.length * PHASES.size();
        for (int readRow = 0; readRow < readRowLen; readRow++) {
            int cum = readRow / PHASES.size();
            String customerName = customers[cum];
            int p = readRow % PHASES.size();
            if (cum != 0 && p == 0) {
                p = PHASES.size() - 1;
            }
            String phase = PHASES.get(p);
            for (int l = 0; l < levelList.size(); l++) {
                String level = levelList.get(l);
                Cell bitCell = sheet.getRow(readRow + totalStart[0]).getCell(l + totalStart[1]);
                double totalBit = evaluator.evaluate(bitCell).getNumberValue();
                if (totalBit > 0) {
                    totals.add((customerName + phase + level).toUpperCase());
                }
            }
        }
        return totals;
    }
}
