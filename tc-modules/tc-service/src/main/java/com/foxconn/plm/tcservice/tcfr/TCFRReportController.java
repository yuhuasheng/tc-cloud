package com.foxconn.plm.tcservice.tcfr;

import cn.hutool.core.date.DateUtil;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.entity.response.RList;
import com.foxconn.plm.tcservice.projectReport.QueryEntity;
import com.foxconn.plm.tcservice.tclicensereport.domain.DateRecordInfo;
import com.foxconn.plm.utils.collect.CollectUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import oracle.jdbc.proxy.annotation.Post;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @Author HuashengYu
 * @Date 2022/9/20 11:02
 * @Version 1.0
 */
@Api(tags = "TCFR 報表")
@RestController(value = "TCFRReportController")
@RequestMapping("tcfr/report")
public class TCFRReportController {

    @Resource
    TCFRReportService service;

    @ApiOperation("BU列表")
    @GetMapping("/getBU")
    public R<List<BUBean>> getBU() {
        List<BUBean> list = service.getBU();
        return R.success(list);
    }

    @ApiOperation("客戶列表")
    @GetMapping("/getCustomer")
    public R<List<BUBean>> getCustomer(String buid) {
        List<BUBean> list = service.getCustomer(buid);
        return R.success(list);
    }

    @ApiOperation("產品線列表")
    @GetMapping("/getProductLine")
    public R<List<BUBean>> getProductLine(String custId) {
        List<BUBean> list = service.getProductLine(custId);
        return R.success(list);
    }

    @ApiOperation("查看報表")
    @PostMapping("/getList")
    public R<List<TCFRReportBean>> getList(@RequestBody TFCRReportRp rp) {
        List<TCFRReportBean> list = service.getList(rp);
        return R.success(list);
    }


    @ApiOperation("导出")
    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestBody TFCRReportRp p) {
        HttpHeaders headers = new HttpHeaders();
        try {
            ByteArrayOutputStream export = service.export(p);
            String now = DateUtil.now().replace(" ","-");
            headers.setContentDispositionFormData("attachment", "TCFRReport."+now+".xlsx");
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            return new ResponseEntity<>(export.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(headers, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

}
