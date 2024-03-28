package com.foxconn.plm.tcservice.projectReport;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.response.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Api(tags = "专案报表查询")
@RestController
@RequestMapping("/project")
public class ProjectReportController {
    private static Log log = LogFactory.get();
    @Autowired
    ProjectReportService service;

    @Resource
    EmailNoticeTask emailNoticeTask;


    @ApiOperation("查询报表")
    @PostMapping("/search")
    public R<List<ReportEntity>> report(@RequestBody QueryEntity p) {
        List<ReportEntity> reportEntities = service.queryData(p);
        return R.success(reportEntities);
    }

    @ApiOperation("导出")
    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestBody QueryEntity p) {
        HttpHeaders headers = new HttpHeaders();
        try {
            ByteArrayOutputStream export = service.export(p);
            headers.setContentDispositionFormData("attachment", "ProjectReport.xlsx");
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            return new ResponseEntity<>(export.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(headers, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @ApiOperation("下拉列表")
    @GetMapping("/getLovList")
    public R<JSONObject> getLovList() {
        return R.success(service.getLovList());
    }

    @ApiOperation("刷新專案執行報表配置")
    @GetMapping("/refreshConfig")
    public R<String> refreshConfig() {
        InputStream resourceAsStream = null;
        ExcelReader reader=null;
        try {
            resourceAsStream = getClass().getResourceAsStream("/TCProjectReportEmailConfig_prod.xlsx");
            reader = ExcelUtil.getReader(resourceAsStream);
            reader.setSheet("當前時間配置");
            ProjectReportService.emailTrackCurrentDate = Integer.parseInt(com.foxconn.plm.utils.excel.ExcelUtil.getCellValueToString(reader.getCell(0, 0)));
            reader.close();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            }catch (Exception e){}
            try {
                if(resourceAsStream!=null){
                    resourceAsStream.close();
                }
            }catch (IOException e){}
        }
        return R.success("OK");
    }

    @ApiOperation("發送郵件提醒")
    @GetMapping(value = "/sendEmailNotice")
    public R<String> sendEmailNotice(HttpServletRequest request) throws InterruptedException {
        String remoteHost = request.getRemoteHost();
        new Thread(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                emailNoticeTask.sendTracEmail();
            }
        }).start();
        log.info("Call sendEmailNotice from: "+remoteHost);
        Thread.sleep(5000);
        return R.success(remoteHost);
    }

}
