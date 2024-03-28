package com.foxconn.plm.tcservice.dcnreport.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcservice.dcnreport.domain.DCNReportBean;
import com.foxconn.plm.tcservice.dcnreport.domain.QueryEntity;
import com.foxconn.plm.tcservice.dcnreport.service.DCNReportService;
import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @Author HuashengYu
 * @Date 2022/10/21 11:13
 * @Version 1.0
 */
@RestController
@RequestMapping("/DCNReport")
@Scope("request")
public class DCNReportController {
    private static Log log = LogFactory.get();
    @Resource
    private DCNReportService dcnReportService;


    @PostMapping("/saveDCNData")
    @ApiOperation("保存DCN数据")
    public R saveDCNData(@RequestBody List<DCNReportBean> list) {
        log.info("==>> 开始执行 saveDCNData");
        try {
            dcnReportService.saveDCNReportData(list);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getLocalizedMessage());
        }
        log.info("==>> 结束执行 saveDCNData");
        return R.success();
    }

    @GetMapping("/getLovList")
    @ApiOperation("下拉列表")
    public R getLovList() {
        log.info("==>> 开始执行 getLovList");
        try {
            return R.success(dcnReportService.getLinkageLovList());
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getLocalizedMessage());
        }
    }

    @GetMapping("/getFeeLovList")
    @ApiOperation("DCN费用下拉列表")
    public R getFeeLovList() {
        log.info("==>> 开始执行 getFeeLovList");
        try {
            return R.success(dcnReportService.getFeeLovList());
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getLocalizedMessage());
        }
    }


    @GetMapping("/searchDCNRecord")
    @ApiOperation("查询DCN列表")
    public R getDCNRecordList(QueryEntity queryEntity) {
        log.info("==>> 开始执行 getDCNRecordList");
        try {
            return R.success(dcnReportService.getDCNRecordList(queryEntity));
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getLocalizedMessage());
        }
    }


    @GetMapping("/getDCNFeeList")
    @ApiOperation("查询DCN费用占比列表")
    public R getDCNFeeList(@RequestParam("projectId") String projectId, @RequestParam("owner") String owner) {
        log.info("==>> 开始执行 getDCNFeeList");
        try {
            return R.success(dcnReportService.getDCNFeeList(projectId, owner));
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getLocalizedMessage());
        }
    }

    @GetMapping("/getDCNFeePerByProject")
    @ApiOperation("通过专案ID查询DCN费用占比")
    public R getDCNFeePerByProject(@RequestParam("projectId") String projectId, @RequestParam(value = "owner", required = false) String owner) {
        log.info("==>> 开始执行 getDCNFeePercent");
        try {
            return R.success(dcnReportService.getDCNFeePerByProject(projectId, owner));
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getLocalizedMessage());
        }
    }

    @GetMapping("/export")
    @ApiOperation("导出DCN报表数据")
    public ResponseEntity<byte[]> export(QueryEntity queryEntity) {
        HttpHeaders headers = new HttpHeaders();
        try {
            ByteArrayOutputStream export = dcnReportService.export(queryEntity);
            if (export==null) {
                throw new Exception("获取数据失败");
            }
            headers.setContentDispositionFormData("attachment", new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + "_DCN_Report.xlsx");
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            return new ResponseEntity<>(export.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(headers, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

}
