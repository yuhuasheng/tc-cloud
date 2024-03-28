package com.foxconn.plm.tcservice.tclicensereport.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.dp.plm.privately.PrivaFileUtis;
import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.entity.response.MegerCellEntity;
import com.foxconn.plm.tcservice.mapper.master.TcLicenseReportMapper;
import com.foxconn.plm.tcservice.tclicensereport.domain.*;
import com.foxconn.plm.tcservice.tclicensereport.response.*;
import com.foxconn.plm.utils.excel.ExcelUtil;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TcLicenseReportService {

    @Resource
    TcLicenseReportMapper mapper;

    public List<ReportVO> queryData(QueryRp rp) {
        List<ReportVO> summary = rp.getHistoryMouth().length() > 0 ? mapper.history(rp) : mapper.summary(rp);
        for (ReportVO vo : summary) {
            vo.setMergeFlag1(vo.getBU());
            vo.setMergeFlag2(vo.getBU() + vo.getDept());
        }
        return summary;
    }

    public ByteArrayOutputStream export(QueryRp query) throws Exception {
        ByteArrayOutputStream out;
        XSSFWorkbook wb = null;
        FileInputStream is = null;
        try {
            List<ReportVO> list = queryData(query);
            File destFile = PrivaFileUtis.releaseFile("TCLicenseReportExportTemplate.xlsx");
            is = new FileInputStream(Objects.requireNonNull(destFile));
            wb = new XSSFWorkbook(is);
            XSSFSheet sheet = wb.getSheetAt(0);
            XSSFCellStyle cellStyle = wb.createCellStyle();
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            cellStyle.setAlignment(HorizontalAlignment.CENTER);
            cellStyle.setWrapText(true);
            cellStyle.setBorderBottom(BorderStyle.THIN); //下边框
            cellStyle.setBorderLeft(BorderStyle.THIN);//左边框
            cellStyle.setBorderTop(BorderStyle.THIN);//上边框
            cellStyle.setBorderRight(BorderStyle.THIN);//右边框

            for (int i = 0; i < list.size(); i++) {
                ReportVO entity = list.get(i);
                XSSFRow row = sheet.createRow(i + 2);
                ExcelUtil.setCellStyleAndValue(row.createCell(0), entity.getBU(), cellStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(1), entity.getDept(), cellStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(2), entity.getFunc(), cellStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(3), entity.getLicenseTotal(), cellStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(4), entity.getNotUsedQty(), cellStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(5), String.format("%.2f", entity.getUtilizationRate() * 100) + "%", cellStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(6), String.format("%.2f", entity.getAccumulateUsageQty()), cellStyle);
                ExcelUtil.setCellStyleAndValue(row.createCell(7), String.format("%.2f", entity.getCropRate() * 100) + "%", cellStyle);
            }

            List<MegerCellEntity> megerBu = ExcelUtil.scanMegerCells(list, "mergeFlag1", 1);
            List<MegerCellEntity> megerDept = ExcelUtil.scanMegerCells(list, "mergeFlag2", 1);
            //合并单位格
            for (MegerCellEntity megerCellEntity : megerBu) {
                sheet.addMergedRegion(new CellRangeAddress(megerCellEntity.startRow, megerCellEntity.endRow, 0, 0));
            }

            for (MegerCellEntity megerCellEntity : megerDept) {
                sheet.addMergedRegion(new CellRangeAddress(megerCellEntity.startRow, megerCellEntity.endRow, 1, 1));
            }
            XSSFFont font = wb.createFont();
            font.setBold(true);
            XSSFCellStyle cellStyleWithBold = cellStyle.clone();
            cellStyleWithBold.setFont(font);

            if (query.getHistoryMouth().isEmpty()) {
                sheet = wb.getSheetAt(1);
                List<UserInfoVO> userInfo = mapper.getUserInfo(query);
                for (int i = 0; i < userInfo.size(); i++) {
                    XSSFRow row = sheet.createRow(i + 1);
                    UserInfoVO entity = userInfo.get(i);
                    ExcelUtil.setCellStyleAndValue(row.createCell(0), entity.getBU(), cellStyle);
                    ExcelUtil.setCellStyleAndValue(row.createCell(1), entity.getDept(), cellStyle);
                    ExcelUtil.setCellStyleAndValue(row.createCell(2), entity.getFunc(), cellStyle);
                    ExcelUtil.setCellStyleAndValue(row.createCell(3), entity.getWorkNum(), cellStyle);
                    ExcelUtil.setCellStyleAndValue(row.createCell(4), entity.getName(), cellStyle);
                    ExcelUtil.setCellStyleAndValue(row.createCell(5), entity.getLoginDate(), cellStyle);
                }

                sheet = wb.getSheetAt(2);
                List<UserInfoVO> usageInfo = mapper.getUsageInfo(query);
                float accumulateDayUsageTotal = 0;
                for (int i = 0; i < usageInfo.size(); i++) {
                    XSSFRow row = sheet.createRow(i + 1);
                    UserInfoVO entity = usageInfo.get(i);
                    ExcelUtil.setCellStyleAndValue(row.createCell(0), entity.getBU(), cellStyle);
                    ExcelUtil.setCellStyleAndValue(row.createCell(1), entity.getDept(), cellStyle);
                    ExcelUtil.setCellStyleAndValue(row.createCell(2), entity.getFunc(), cellStyle);
                    ExcelUtil.setCellStyleAndValue(row.createCell(3), entity.getWorkNum(), cellStyle);
                    ExcelUtil.setCellStyleAndValue(row.createCell(4), entity.getName(), cellStyle);
                    ExcelUtil.setCellStyleAndValue(row.createCell(5), entity.getLoginDate(), cellStyle);
                    accumulateDayUsageTotal += entity.getAccumulateDayUsageQty();
                    ExcelUtil.setCellStyleAndValue(row.createCell(6), String.format("%.2f", entity.getAccumulateDayUsageQty()), cellStyle);
                }
                XSSFRow row = sheet.createRow(usageInfo.size() + 1);
                ExcelUtil.setCellStyleAndValue(row.createCell(6), "合计：" + String.format("%.2f", accumulateDayUsageTotal), cellStyleWithBold);
            } else {
                wb.removeSheetAt(1);
                wb.removeSheetAt(1);
            }

            out = new ByteArrayOutputStream();
            wb.write(out);

            out.flush();
            destFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
            throw new BizException(e.getMessage());
        } finally {
            if (wb != null) {
                wb.close(); // 关闭此对象，便于后续删除此文件
            }
            if (is != null) {
                is.close(); // 关闭此对象，便于后续删除此文件
            }

        }
        return out;
    }

    public JSONObject getLovList() {
        List<LovEntity> lov = mapper.getLov();
        TreeMap<String, Object> buMap = new TreeMap<>();
        TreeMap<String, Object> deptMap = new TreeMap<>();
        TreeMap<String, Object> funcMap = new TreeMap<>();
        for (LovEntity value : lov) {
            buMap.put(value.bu.trim(), null);
            deptMap.put(value.dept.trim(), null);
            funcMap.put(value.func.trim(), null);
        }
        JSONObject jo = new JSONObject();
        jo.put("buList", buMap.keySet());
        jo.put("deptList", deptMap.keySet());
        jo.put("funcList", funcMap.keySet());
        return jo;
    }

    public List<LicenseRes> utilizationRate(String startDay, String endDay) {
        List<Map<String, Object>> mapList = mapper.utilizationRate(startDay, endDay);
        if (CollUtil.isEmpty(mapList)) {
            return null;
        }
        // 從返回值中解析BU
        List<String> buList = mapList.parallelStream().map(item -> item.get("BU").toString()).distinct().collect(Collectors.toList());
        Map<String, StatisticsVo> buMap = new HashMap<>(mapList.size());
        Set<String> timeList = new HashSet<>();
        String year = "";
        boolean isSameYear = true;
        for (Map<String, Object> itemMap : mapList) {
            String total = itemMap.get("TOTAL").toString();
            String bu = itemMap.get("BU").toString();
            String used = itemMap.get("USED").toString();
            String day = itemMap.get("DAY").toString();
            String itemYear = day.substring(0, 4);
            if (StrUtil.isBlank(year)) {
                year = itemYear;
            }
            if (!year.equals(itemYear)) {
                isSameYear = false;
            }
            timeList.add(day);
            StatisticsVo vo = buMap.get(day + bu);
            if (ObjectUtil.isNull(vo)) {
                vo = new StatisticsVo();
                vo.setLabel(day + bu);
            }
            if ("1".equals(used)) {
                vo.setUsed(Integer.parseInt(total));
            } else {
                vo.setUnUsed(Integer.parseInt(total));
            }
            buMap.put(vo.getLabel(), vo);
        }
        // 初始化返回值
        List<LicenseRes> retList = new ArrayList<>(buMap.size());
        for (String day : timeList) {
            LicenseRes res = new LicenseRes();
            if (isSameYear) {
                res.setName(day.substring(5));
            } else {
                res.setName(day);
            }
            List<StatisticsRes> list = new ArrayList<>();
            for (String bu : buList) {
                StatisticsRes item = new StatisticsRes();
                item.setLabel(bu);
                StatisticsVo vo = buMap.get(day + bu);
                if (ObjectUtil.isNotNull(vo)) {
                    if (ObjectUtil.isNull(vo.getUsed())) {
                        vo.setUsed(0);
                    }
                    if (ObjectUtil.isNull(vo.getUnUsed())) {
                        vo.setUnUsed(0);
                    }
                    String value = NumberUtil.roundStr(NumberUtil.div(vo.getUsed() * 100, vo.getUsed() + vo.getUnUsed()), 2) + "%";
                    item.setValue(value);
                }
                list.add(item);
            }
            res.setList(list);
            retList.add(res);
        }
        return retList.parallelStream().sorted(Comparator.comparing(LicenseRes::getName)).collect(Collectors.toList());
    }

    public List<LicenseRes> cropRate(String startDay, String endDay) {
        List<Map<String, Object>> mapList = mapper.cropRate(startDay, endDay);
        if (CollUtil.isEmpty(mapList)) {
            return null;
        }
        Map<String, LicenseRes> map = new HashMap<>(mapList.size());
        String year = "";
        boolean isSameYear = true;
        for (Map<String, Object> itemMap : mapList) {
            String bu = itemMap.get("BU").toString();
            String lur = itemMap.get("LUR").toString();
            String day = itemMap.get("DAY").toString();
            String itemYear = day.substring(0, 4);
            if (StrUtil.isBlank(year)) {
                year = itemYear;
            }
            if (!year.equals(itemYear)) {
                isSameYear = false;
            }
            LicenseRes res = map.get(day);
            if (ObjectUtil.isNull(res)) {
                res = new LicenseRes();
                res.setName(day);
                res.setList(new ArrayList<>());
                map.put(day, res);
            }
            StatisticsRes item = new StatisticsRes();
            item.setLabel(bu);
            item.setValue(NumberUtil.roundStr(Double.parseDouble(lur) * 100, 2) + "%");
            res.getList().add(item);
        }
        for (LicenseRes item : map.values()) {
            if (isSameYear) {
                item.setName(item.getName().substring(5));
            }
        }
        return map.values().parallelStream().sorted(Comparator.comparing(LicenseRes::getName)).collect(Collectors.toList());
    }

    public List<LicenseRes> radarChart(String startDay, String endDay) {
        // 統計使用率
        LicenseRes utilizationRate = getTotalUtilizationRate(startDay, endDay);
        // 統計稼動率
        LicenseRes cropRate = getTotalCropRate(startDay, endDay);
        return CollUtil.newArrayList(utilizationRate, cropRate);
    }

    public List<FunctionRes> statisticsByFunction(String startDay, String endDay) {
        // 統計使用情況
        List<Map<String, Object>> mapList = mapper.utilizationRateByFunction(startDay, endDay);
        Map<String, Set<String>> funcTotalMap = new HashMap<>();
        Map<String, FunctionRes> map = new HashMap<>(mapList.size());
        for (Map<String, Object> itemMap : mapList) {
            String bu = itemMap.get("BU").toString();
            String department = itemMap.get("DEPARTMENT").toString();
            String function = itemMap.get("FUNCTION").toString();
            String used = itemMap.get("USED").toString();
            String key = bu + "_" + department + "_" + function;
            FunctionRes res = map.get(key);
            if (ObjectUtil.isNull(res)) {
                res = new FunctionRes();
                res.setBu(bu);
                res.setDepartment(department);
                res.setFunction(function);
                res.setUsed(0);
                res.setTotal(0);
                map.put(key, res);
            }
            if ("1".equals(used)) {
                res.setUsed(res.getUsed() + 1);
            }
            // 計算bu的license數量
            Set<String> set = funcTotalMap.get(key);
            if (ObjectUtil.isNull(set)) {
                set = new HashSet<>();
                funcTotalMap.put(key, set);
            }
            String userId = itemMap.get("USER_ID").toString();
            set.add(bu + "_" + department + "_" + function + "_" + userId);
        }
        // 統計稼動率
        List<Map<String, Object>> mapList1 = mapper.cropRateByFunction(startDay, endDay);
        for (Map<String, Object> itemMap : mapList1) {
            String lur = itemMap.get("LUR").toString();
            String bu = itemMap.get("BU").toString();
            String department = itemMap.get("DEPARTMENT").toString();
            String function = itemMap.get("FUNCTION").toString();
            String key = bu + "_" + department + "_" + function;
            FunctionRes res = map.get(key);
            if (ObjectUtil.isNull(res)) {
                res = new FunctionRes();
                res.setBu(bu);
                res.setDepartment(department);
                res.setFunction(function);
                res.setUsed(0);
                res.setTotal(0);
                map.put(key, res);
            }
            res.setCropRate(NumberUtil.roundStr(Double.parseDouble(lur) * 100, 2) + "%");
        }
        return map.values().parallelStream().peek(item -> {
            String key = item.getBu() + "_" + item.getDepartment() + "_" + item.getFunction();
            Set<String> set = funcTotalMap.get(key);
            if (ObjectUtil.isNotNull(set)) {
                item.setTotal(set.size());
            }
            if (item.getTotal() == 0) {
                item.setUtilizationRate("0%");
            } else {
                String value = NumberUtil.roundStr(NumberUtil.div(item.getUsed() * 100, (int) item.getTotal()), 2) + "%";
                item.setUtilizationRate(value);
            }
        }).sorted(Comparator.comparing(FunctionRes::getDepartment).thenComparing(FunctionRes::getFunction)).collect(Collectors.toList());
    }


    public HistoryRateRes historyRate() {
        List<Map<String, Object>> utilizationRateList = mapper.historyUtilizationRate();
        Map<String, LicenseRes> aurMap = new HashMap<>(utilizationRateList.size());
        for (Map<String, Object> itemMap : utilizationRateList) {
            String bu = itemMap.get("BU").toString();
            Integer total = Integer.parseInt(itemMap.get("TOTAL").toString());
            String month = itemMap.get("MONTH").toString();
            Integer unused = Integer.parseInt(itemMap.get("UNUSED").toString());
            LicenseRes res = aurMap.get(month);
            if (ObjectUtil.isNull(res)) {
                res = new LicenseRes();
                res.setName(month);
                res.setList(new ArrayList<>());
                aurMap.put(month, res);
            }
            res.getList().add(new StatisticsRes(bu, NumberUtil.roundStr(NumberUtil.div(total - unused, (int) total) * 100, 2) + "%"));
        }
        List<Map<String, Object>> cropRateList = mapper.historyCropRate();
        Map<String, LicenseRes> lurMap = new HashMap<>(cropRateList.size());
        for (Map<String, Object> itemMap : cropRateList) {
            String bu = itemMap.get("BU").toString();
            String lur = itemMap.get("LUR").toString();
            String month = itemMap.get("MONTH").toString();
            LicenseRes lurRes = lurMap.get(month);
            if (ObjectUtil.isNull(lurRes)) {
                lurRes = new LicenseRes();
                lurRes.setName(month);
                lurRes.setList(new ArrayList<>());
                lurMap.put(month, lurRes);
            }
            lurRes.getList().add(new StatisticsRes(bu, NumberUtil.roundStr(Double.parseDouble(lur) * 100, 2) + "%"));
        }
        return new HistoryRateRes(
                aurMap.values().parallelStream().sorted(Comparator.comparing(LicenseRes::getName)).collect(Collectors.toList()),
                lurMap.values().parallelStream().sorted(Comparator.comparing(LicenseRes::getName)).collect(Collectors.toList())
        );
    }


    public List<HistoryRadarRes> historyRadarChart(String month) {
        DateTime parse = DateUtil.parse(month, "yyyy-MM");
        List<Map<String, Object>> mapList = mapper.historyRadarChart(parse.monthBaseOne());
        if (ObjectUtil.isNull(mapList)) {
            return null;
        }
        Map<String, HistoryRadarRes> map = new HashMap<>();
        for (Map<String, Object> itemMap : mapList) {
            String bu = itemMap.get("BU").toString();
            String department = itemMap.get("DEPARTMENT").toString();
            String function = itemMap.get("FUNCTION").toString();
            String lur = itemMap.get("LUR").toString();
            String total = itemMap.get("TOTAL").toString();
            String unused = itemMap.get("UNUSED").toString();
            HistoryRadarRes res = map.get(bu);
            if (ObjectUtil.isNull(res)) {
                res = new HistoryRadarRes();
                res.setBu(bu);
                res.setItemList(new ArrayList<>());
                map.put(bu, res);
            }
            FunctionRes itemRes = new FunctionRes();
            itemRes.setBu(bu);
            itemRes.setDepartment(department);
            itemRes.setTotal(Integer.parseInt(total));
            itemRes.setUsed(itemRes.getTotal() - Integer.parseInt(unused));
            itemRes.setFunction(function);
            itemRes.setUtilizationRate(NumberUtil.roundStr(NumberUtil.div((int) itemRes.getUsed(), (int) itemRes.getTotal()) * 100, 2) + "%");
            itemRes.setCropRate(lur);
            res.getItemList().add(itemRes);
        }
        return map.values().stream().peek(item -> {
            List<FunctionRes> itemList = item.getItemList();
            Double totalLur = 0D;
            Integer total = 0, used = 0;
            for (FunctionRes res : itemList) {
                double lur = Double.parseDouble(res.getCropRate());
                totalLur += lur;
                res.setCropRate(NumberUtil.roundStr(lur * 100, 2) + "%");
                total += res.getTotal();
                used += res.getUsed();
            }
            // 計算使用率
            item.setTotalCropRate(NumberUtil.roundStr(NumberUtil.div(used * 100, (int) total), 2) + "%");
            // 計算稼動率
            item.setTotalUtilizationRate(NumberUtil.roundStr(NumberUtil.div(totalLur * 100, itemList.size()), 2) + "%");
            List<FunctionRes> collect = item.getItemList().parallelStream()
                    .sorted(Comparator.comparing(FunctionRes::getDepartment).thenComparing(FunctionRes::getFunction))
                    .collect(Collectors.toList());
            item.setItemList(collect);
        }).collect(Collectors.toList());
    }

    private LicenseRes getTotalCropRate(String startDay, String endDay) {
        List<Map<String, Object>> mapList = mapper.totalCropRate(startDay, endDay);
        LicenseRes res = new LicenseRes();
        res.setName("cropRate");
        res.setList(mapList.parallelStream().map(itemMap -> {
            String bu = itemMap.get("BU").toString();
            String lur = itemMap.get("LUR").toString();
            StatisticsRes item = new StatisticsRes();
            item.setLabel(bu);
            item.setValue(NumberUtil.roundStr(Double.parseDouble(lur) * 100, 2) + "%");
            return item;
        }).collect(Collectors.toList()));
        return res;
    }

    private LicenseRes getTotalUtilizationRate(String startDay, String endDay) {
        // 查詢使用過的license
        List<Map<String, Object>> mapList = mapper.utilizationRateByFunction(startDay, endDay);
        // 可以根據部門下的不同的用戶id確認license總數，根據used為1表示使用數
        Map<String, Set<String>> totalMap = new HashMap<>();
        Map<String, StatisticsVo> map = new HashMap<>();
        for (Map<String, Object> itemMap : mapList) {
            String used = itemMap.get("USED").toString();
            String bu = itemMap.get("BU").toString();
            StatisticsVo vo = map.get(bu);
            if (ObjectUtil.isNull(vo)) {
                vo = new StatisticsVo();
                vo.setLabel(bu);
                vo.setUsed(0);
                vo.setUnUsed(0);
                map.put(bu, vo);
            }
            if ("1".equals(used)) {
                vo.setUsed(vo.getUsed() + 1);
            }
            // 計算bu的license數量
            Set<String> set = totalMap.get(bu);
            if (ObjectUtil.isNull(set)) {
                set = new HashSet<>();
                totalMap.put(bu, set);
            }
            String userId = itemMap.get("USER_ID").toString();
            set.add(bu + userId);
        }
        // 獲取license總數
        LicenseRes utilizationRate = new LicenseRes();
        utilizationRate.setName("utilizationRate");
        utilizationRate.setList(map.values().parallelStream().map(item -> {
            StatisticsRes res = new StatisticsRes();
            res.setLabel(item.getLabel());
            int total = 0;
            Set<String> set = totalMap.get(item.getLabel());
            if (ObjectUtil.isNotNull(set)) {
                total = set.size();
            }
            String value = NumberUtil.roundStr(NumberUtil.div(item.getUsed() * 100, total), 2) + "%";
            res.setValue(value);
            return res;
        }).collect(Collectors.toList()));
        return utilizationRate;
    }

    public ByteArrayOutputStream exportLicense() throws Exception {
        List<TCLicenseByBean> export = mapper.exportByPhase();
        File file = PrivaFileUtis.releaseFile("TCLicenseByPhaseTemplate.xlsx");
        ExcelWriter writer = new ExcelWriter(file);
        writer.renameSheet(0,"專案Phase稼動率");
        for (int i = 1; i <= export.size(); i++) {
            TCLicenseByBean bean = export.get(i-1);
            writer.writeCellValue(0,i,bean.getBu());
            writer.writeCellValue(1,i,bean.getDepartment());
            writer.writeCellValue(2,i,bean.getFunction());
            writer.writeCellValue(3,i,bean.getLevel());
            writer.writeCellValue(4,i,bean.getCustomer());
            writer.writeCellValue(5,i,bean.getPhase());
            writer.writeCellValue(6,i,bean.getAvgUsed());
            writer.writeCellValue(7,i,String.format("%.2f%%",Float.parseFloat(bean.getAvgRate())*100));
        }
        // 合并
        Sheet sheet = writer.getSheet();
        ExcelUtil.scanMegerCells4(sheet,export,0,0,"bu","department","function");
        ExcelUtil.scanMegerCells4(sheet,export,0,1,"bu","department","function");
        ExcelUtil.scanMegerCells4(sheet,export,0,2,"bu","department","function");
        ExcelUtil.scanMegerCells4(sheet,export,0,3,"bu","department","function","level");
        ExcelUtil.scanMegerCells4(sheet,export,0,4,"bu","department","function","level","customer");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.flush(out);
        writer.close();
        return out;
    }

}
