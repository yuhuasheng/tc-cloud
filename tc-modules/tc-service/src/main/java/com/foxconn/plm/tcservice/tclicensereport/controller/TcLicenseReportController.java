package com.foxconn.plm.tcservice.tclicensereport.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcservice.tclicensereport.domain.QueryRp;
import com.foxconn.plm.tcservice.tclicensereport.domain.ReportVO;
import com.foxconn.plm.tcservice.tclicensereport.response.*;
import com.foxconn.plm.tcservice.tclicensereport.service.TcLicenseReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;

@Api(tags = "TC License 查询")
@RestController
@RequestMapping("/license")
public class TcLicenseReportController {
    private static Log log = LogFactory.get();
    @Autowired
    TcLicenseReportService service;

    @ApiOperation("查询报表")
    @PostMapping("/search")
    public R<List<ReportVO>> search(@RequestBody QueryRp p) {
        List<ReportVO> reportEntities = service.queryData(p);
        return R.success(reportEntities);
    }

    @ApiOperation("导出")
    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestBody QueryRp p) {
        HttpHeaders headers = new HttpHeaders();
        try {
            ByteArrayOutputStream export = service.export(p);
            headers.setContentDispositionFormData("attachment", "TcLicenseReport.xlsx");
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


    @GetMapping("/utilizationRate")
    @ApiOperation("根據Bu統計每天的使用率")
    public R<List<LicenseRes>> utilizationRate(@RequestParam("startDay") String startDay, @RequestParam("endDay") String endDay) {
        List<LicenseRes> list = service.utilizationRate(startDay, endDay);
        return CollUtil.isNotEmpty(list) ? R.success(list) : R.success(Collections.emptyList());
    }

    @GetMapping("/cropRate")
    @ApiOperation("根據Bu統計每天的稼動率")
    public R<List<LicenseRes>> cropRate(@RequestParam("startDay") String startDay, @RequestParam("endDay") String endDay) {
        List<LicenseRes> list = service.cropRate(startDay, endDay);
        return CollUtil.isNotEmpty(list) ? R.success(list) : R.success(Collections.emptyList());
    }

    @GetMapping("/radarChart")
    @ApiOperation("根據Bu統計每天的使用率和稼動率")
    public R<List<LicenseRes>> radarChart(@RequestParam("startDay") String startDay, @RequestParam("endDay") String endDay) {
        List<LicenseRes> list = service.radarChart(startDay, endDay);
        return CollUtil.isNotEmpty(list) ? R.success(list) : R.success(Collections.emptyList());
    }

    @GetMapping("/statisticsByFunction")
    @ApiOperation("根據Function統計License")
    public R<List<FunctionRes>> statisticsByFunction(@RequestParam("startDay") String startDay, @RequestParam("endDay") String endDay) {
        List<FunctionRes> list = service.statisticsByFunction(startDay, endDay);
        return CollUtil.isNotEmpty(list) ? R.success(list) : R.success(Collections.emptyList());
    }


    @GetMapping("/history/rate")
    @ApiOperation("根據Bu統計歷史數據每月的使用率和稼動率")
    public R<HistoryRateRes> historyRate() {
        HistoryRateRes result = service.historyRate();
        return ObjectUtil.isNotNull(result) ? R.success(result) : R.success(new HistoryRateRes(Collections.emptyList(), Collections.emptyList()));
    }

    @GetMapping("/history/radarChart")
    @ApiOperation("根據Bu統計指定月份的使用率和稼動率")
    public R<List<HistoryRadarRes>> historyRadarChart(@RequestParam("month") String month) {
        List<HistoryRadarRes> list = service.historyRadarChart(month);
        return CollUtil.isNotEmpty(list) ? R.success(list) : R.success(Collections.emptyList());
    }

    @GetMapping(value = "/exportByPhase")
    public ResponseEntity<byte[]> exportByPhase() {
            HttpHeaders headers = new HttpHeaders();
            try {
                ByteArrayOutputStream export = service.exportLicense();
                headers.setContentDispositionFormData("attachment", "TcLicenseByPhaseReport.xlsx");
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                return new ResponseEntity<>(export.toByteArray(), headers, HttpStatus.OK);
            } catch (Exception e) {
                e.printStackTrace();
                return new ResponseEntity<>(headers, HttpStatus.SERVICE_UNAVAILABLE);
            }
    }

    public static void main(String[] args) {
        long l = HttpUtil.downloadFile("http://127.0.0.1:8888/license/exportByPhase", "D:/2.xlsx");
        System.out.println(l);
    }
}
