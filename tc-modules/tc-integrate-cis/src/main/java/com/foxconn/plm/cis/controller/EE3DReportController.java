package com.foxconn.plm.cis.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.cis.domain.EE3DCISModelInfo;
import com.foxconn.plm.cis.domain.EE3DCISModelInfoExcel;
import com.foxconn.plm.cis.service.Impl.TCService;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.cis.domain.EE3DProjectBean;
import com.foxconn.plm.cis.domain.EE3DReportBean;
import com.foxconn.plm.cis.service.Impl.EE3DReportService;
import com.foxconn.plm.redis.service.RedisService;
import com.foxconn.plm.utils.excel.ExcelUtil;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ee3DReport")
public class EE3DReportController {

    private final Log log = LogFactory.get();

    @Resource
    private EE3DReportService ee3DReportService;


    @Resource
    private TCService tcService;


    @PostMapping("/getNoCisModel")
    public R<List<EE3DCISModelInfo>> getNoCisModel(@RequestBody Set<String> pnList, String bu, String customer) {
        System.out.println("bu : " + bu);
        System.out.println("customer : " + customer);
        return R.success(ee3DReportService.getNoCISModelInfos(pnList, bu, customer));

    }


    @PostMapping("/getData")
    public R<List<EE3DReportBean>> getEE3Report(@RequestBody EE3DProjectBean data) {
        return R.success(tcService.selectEE3DReportList(data));
        //return R.success(ee3DReportService.getAllReport(data));
    }


    @GetMapping("/getLov")
    public R<List<EE3DProjectBean>> getEE3ReportProjects(EE3DProjectBean projectBean) {
        return R.success(ee3DReportService.getProjectInfoTree());
    }


    @GetMapping("/queryInfo")
    public R<Set<String>> getEE3ReportCondition(EE3DProjectBean projectBean) {
        Set<String> result = tcService.selectConditionProject(projectBean);
        return R.success(result);
    }


    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestBody EE3DProjectBean data) {
        List<EE3DReportBean> list = tcService.selectEE3DReportList(data);
        list.forEach(EE3DReportBean::setPercent);
        HttpHeaders headers = new HttpHeaders();
        Workbook workbook = ExcelUtil.getWorkbookNew("/templates/EE3DReportTemplates.xlsx");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Sheet projectSheet = workbook.getSheetAt(0);
            ExcelUtil.setCellValue(list, 2, 11, projectSheet, ExcelUtil.getCellStyle(workbook));
            List<CellRangeAddress> projectCellRangeAddressList = ExcelUtil.generateCellRangeAddress(list, 2, EE3DReportBean.class);
            projectCellRangeAddressList.forEach(projectSheet::addMergedRegion);
            List<EE3DCISModelInfoExcel> noCisList = list.stream().map(e -> {
                        Set<String> noCisSet = e.getNoCisModel();
                        List<EE3DCISModelInfoExcel> noCisModelList = new ArrayList<>();
                        if (noCisSet != null && noCisSet.size() > 0) {
                            for (String mfgPn : noCisSet) {
                                EE3DCISModelInfoExcel ee3DCISModelInfoExcel = new EE3DCISModelInfoExcel();
                                BeanUtils.copyProperties(e, ee3DCISModelInfoExcel);
                                String[] mfgs = mfgPn.split("\\$");
                                ee3DCISModelInfoExcel.setMfgPn(mfgs[0]);
                                ee3DCISModelInfoExcel.setHhPn(mfgs[1]);
                                noCisModelList.add(ee3DCISModelInfoExcel);
                            }
                        }
                        return noCisModelList;
                    }
            ).flatMap(Collection::stream).collect(Collectors.toList());

            Map<String, List<EE3DCISModelInfoExcel>> cisMap =
                    noCisList.stream().collect(Collectors.groupingBy(k -> k.getBu() + "#" + k.getCustomer()));
            List<EE3DCISModelInfo> noCisDBList = new ArrayList<>();
            cisMap.forEach((k, v) -> {
                String[] buCustomer = k.split("#");
                Set<String> noCisStrs = v.stream().map(e -> e.getMfgPn() + "$" + e.getHhPn()).collect(Collectors.toSet());
                List<EE3DCISModelInfo> tempList = ee3DReportService.getNoCISModelInfos(noCisStrs, buCustomer[0], buCustomer[1]);
                noCisDBList.addAll(tempList);
            });
            List<EE3DCISModelInfoExcel> noCisExcelList = new ArrayList<>();
            Map<String, EE3DCISModelInfo> noCisMap2 = noCisDBList.stream().collect(Collectors.toMap(k -> k.getCisLibrary() + k.getMfgPn(), e -> e,
                    (v1,
                     v2) -> v1));
            for (EE3DCISModelInfoExcel excelBean : noCisList) {
                String library = ee3DReportService.getCisLibrary(excelBean.getBu(), excelBean.getCustomer());
                EE3DCISModelInfo noCisInfo = noCisMap2.get(library + excelBean.getMfgPn());
                if (noCisInfo != null) {
                    BeanUtils.copyProperties(noCisInfo, excelBean);
                }
                noCisExcelList.add(excelBean);
            }
//            Collection<EE3DCISModelInfoExcel> newNoCisExcelList =
//                    noCisExcelList.stream().collect(Collectors.toMap(e -> e.getBu() + e.getCustomer() + e.getProjectSeries() + e.getProcessName()
//                    + e.getPhase() + e.getVersion() + e.getCisLibrary() + e.getPartType() + e.getHhPn() + e.getStandardPn() + e.getMfg() + e
//                    .getMfgPn(), e -> e, (o1, o2) -> o1)).values();
//            noCisExcelList = new ArrayList<>(newNoCisExcelList);
            noCisExcelList.sort(Comparator.comparing(e -> e.getBu() + e.getCustomer() + e.getProjectSeries() + e.getProcessName() + e.getPhase() + e.getVersion() + e.getCisLibrary() + e.getPartType()));
            for (int i = 0; i < noCisExcelList.size(); i++) {
                noCisExcelList.get(i).setItem(i + 1 + "");
            }
            Sheet noCisSheet = workbook.getSheetAt(1);
            ExcelUtil.setCellValue(noCisExcelList, 1, 19, noCisSheet, ExcelUtil.getCellStyle(workbook));
            List<CellRangeAddress> cellRangeAddressList = ExcelUtil.generateCellRangeAddress(noCisExcelList, 1, EE3DCISModelInfoExcel.class);
            cellRangeAddressList.forEach(noCisSheet::addMergedRegion);
            workbook.setActiveSheet(0);
            workbook.write(out);
        } catch (Exception e) {
            e.printStackTrace();
        }
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timeStr = df.format(LocalDateTime.now());
        headers.setContentDispositionFormData("attachment", timeStr + "_EE3EReport.xlsx");
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);
    }

}
