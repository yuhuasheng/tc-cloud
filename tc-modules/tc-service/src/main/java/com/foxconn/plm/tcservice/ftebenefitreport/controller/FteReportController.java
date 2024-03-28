package com.foxconn.plm.tcservice.ftebenefitreport.controller;

import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcservice.ftebenefitreport.service.FetReportServce;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * @author Robert
 */
@RestController
@RequestMapping("/fteBenefitReport")
public class FteReportController {

    @Resource
    private FetReportServce fetReportServce;

    @PostMapping("/uploadFile")
    public R uploadFile(@RequestParam String reportType, @RequestParam MultipartFile file) {
        try {
            Workbook wb = WorkbookFactory.create(file.getInputStream());
            Sheet formSheet = wb.getSheet(reportType);
            if (formSheet == null) {
                throw new RuntimeException("excel 里面没有对应的sheet : " + reportType);
            }
            List<String> errorList = fetReportServce.checkExcel(formSheet, reportType);
            if (errorList.size() > 0) {
                return R.error(HttpResultEnum.SERVER_ERROR.getCode(),"error:duplicate");
            }
            errorList.addAll(fetReportServce.saveData(formSheet, reportType));
            if (errorList.size() > 0) {
                return R.error(HttpResultEnum.SERVER_ERROR.getCode(),"error:data exception", errorList);
            }
            //保存用户上传的文件
            fetReportServce.saveHistoryExcel(file);

        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getLocalizedMessage());
        }
        System.out.println(file.getOriginalFilename() + "  reportType " + reportType);
        return R.success();
    }

    @GetMapping("/getFTEBenefitDataByMonth")
    @ApiOperation("获取FTE效益报表json 数据 ")
    public R getFTEBenefitData(@RequestParam("startDate") String startDate, @RequestParam("endDate") String endDate) {
        try {
            Object data = fetReportServce.getFTEBenefitData(startDate, endDate);
            if (data instanceof String) {
                return R.error(HttpResultEnum.SERVER_ERROR.getCode(), data.toString());
            } else if (data instanceof List) {
                return R.success(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getLocalizedMessage());
        }
        return null;
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> fileDownload(@RequestParam String startDate, @RequestParam String endDate, String reportType) {
        HttpHeaders headers = new HttpHeaders();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String fileName = UriUtils.encode(reportType + " " + startDate + " ~ " + endDate + ".xlsx", "UTF-8");
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            fetReportServce.downloadExcel(startDate, endDate, reportType, out);
            return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(headers, HttpStatus.SERVICE_UNAVAILABLE);
    }

}
