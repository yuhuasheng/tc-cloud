package com.foxconn.plm.integrate.cis.controller;

import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.integrate.cis.domain.EE3DProjectBean;
import com.foxconn.plm.integrate.cis.domain.EE3DReportBean;
import com.foxconn.plm.integrate.cis.service.Impl.EE3DReportService;
import com.foxconn.plm.utils.excel.ExcelUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sun.security.action.GetPropertyAction;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/ee3DReport")
public class EE3DReportController {

    @Resource
    private EE3DReportService ee3DReportService;

    @PostMapping("/getData")
    public R<List<EE3DReportBean>> getEE3Report(@RequestBody List<EE3DProjectBean> data) {
        return R.success(ee3DReportService.getAllReport(data));
    }


    @GetMapping("/getLov")
    public R<List<EE3DProjectBean>> getEE3ReportProjects() {
        return R.success(ee3DReportService.getProjectInfoTree());
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestBody List<EE3DProjectBean> data) {
        List<EE3DReportBean> list = ee3DReportService.getAllReport(data);
        list.forEach(EE3DReportBean::setPercent);
        HttpHeaders headers = new HttpHeaders();
        Workbook workbook = ExcelUtil.getWorkbookNew("/templates/EE3DReportTemplates.xlsx");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ExcelUtil.setCellValue(list, 3, 11, workbook.getSheetAt(0), ExcelUtil.getCellStyle(workbook));
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
