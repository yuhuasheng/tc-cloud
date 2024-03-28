package com.foxconn.plm.tcservice.benefitreport.controller;

import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcservice.benefitreport.constant.BU;
import com.foxconn.plm.tcservice.benefitreport.constant.ExcelConfig;
import com.foxconn.plm.tcservice.benefitreport.domain.BenefitCollectBean;
import com.foxconn.plm.tcservice.benefitreport.domain.BenefitReportBean;
import com.foxconn.plm.tcservice.benefitreport.domain.ExcelConfigBean;
import com.foxconn.plm.tcservice.benefitreport.domain.RowDataBean;
import com.foxconn.plm.tcservice.benefitreport.service.*;
import com.foxconn.plm.tcservice.benefitreport.service.impl.SpasProjectServiceImpl;
import com.foxconn.plm.utils.collect.CollectUtil;
import com.foxconn.plm.utils.date.DateUtil;
import com.foxconn.plm.utils.excel.ExcelUtil;
import com.foxconn.plm.utils.string.StringUtil;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/benefit/report")
public class ReportController {

    @Resource
    private BenefitReportForDT benefitReportForDT;

    @Resource
    private BenefitReportForMNT benefitReportForMNT;

    @Resource
    private BenefitReportForPRT benefitReportForPRT;

    @Resource(type = SpasProjectServiceImpl.class)
    private SpasProjectService spasProjectService;

    @Resource
    private SpasProjectService spasProjectServiceMVP2;

    @Resource
    private BenefitService benefitService;

    @Resource
    private ExcelConfigBean dtExcelConfig;

    @Resource
    private ExcelConfigBean mntExcelConfig;

    @Resource
    private ExcelConfigBean prtExcelConfig;


    @GetMapping("/exportReport/{bu}")
    public ResponseEntity<byte[]> exportReportDataByDate(@PathVariable String bu,
                                                         @RequestParam(value = "startDate") String startDate) {
        HttpHeaders headers = new HttpHeaders();
        Workbook workbook = null;
        try {
            if (BU.MNT.toString().equals(bu)) {
                workbook = ExcelUtil.getWorkbookNew(mntExcelConfig.getTemplateExcel());
            } else if (BU.DT.toString().equals(bu)) {
                workbook = ExcelUtil.getWorkbookNew(dtExcelConfig.getTemplateExcel());
            } else if (BU.PRT.toString().equals(bu)) {
                // prt 还是使用旧的模板
                workbook = ExcelUtil.getWorkbookNew(prtExcelConfig.getTemplateExcel());
                // return exportReportDataByDate(bu, startDate, null, null, null, null);
            }
            assert workbook != null;
            //2022
            Map<String, Integer> sheetIndexMap = new HashMap<>();
            sheetIndexMap.put("DT FTE效益計算", 0);
            sheetIndexMap.put("MNT FTE效益計算", 0);
            sheetIndexMap.put("PRT FTE效益計算", 0);
            sheetIndexMap.put("专案清单", 1);
            exportReportDataByDate(bu, startDate, null, null, workbook, sheetIndexMap);
            //2023
            spasProjectServiceMVP2.wirteExcel(workbook, bu, startDate);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            headers.setContentDispositionFormData("attachment", bu + "_" + startDate + "_benefitReport.xlsx");
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            return new ResponseEntity<byte[]>(out.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<byte[]>(headers, HttpStatus.SERVICE_UNAVAILABLE);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @GetMapping("/getReport")
    public R<List<BenefitCollectBean>> getReportDataByDate(@RequestParam(value = "startDate", required = false) String startDate) {
        List<BenefitCollectBean> list = new ArrayList<>();
        //2022
        List<BenefitReportBean> mntData = getReportDataByDate(BU.MNT.name(), startDate, null, null).getData();
        List<BenefitReportBean> dtData = getReportDataByDate(BU.DT.name(), startDate, null, null).getData();
        List<BenefitReportBean> prtData = getReportDataByDate(BU.PRT.name(), startDate, null, null).getData();
        BenefitCollectBean r2022 = new BenefitCollectBean();
        BigDecimal mntTotal = new BigDecimal(0);
        BigDecimal dtTotal = new BigDecimal(0);
        BigDecimal prtTotal = new BigDecimal(0);
        if (!CollectUtil.isEmpty(mntData)) {
            mntTotal = new BigDecimal(mntData.get(mntData.size() - 1).getBenefit());
        }
        if (!CollectUtil.isEmpty(dtData)) {
            dtTotal = new BigDecimal(dtData.get(dtData.size() - 1).getBenefit());
        }
        if (!CollectUtil.isEmpty(prtData)) {
            prtTotal = new BigDecimal(prtData.get(prtData.size() - 1).getBenefit());
        }
        r2022.setMnt(mntTotal);
        r2022.setDt(dtTotal);
        r2022.setPrt(prtTotal);
        r2022.setName("2022效益點");
        list.add(r2022);
        //2023
        R<BenefitCollectBean> r = spasProjectServiceMVP2.getSpasProjectByDate(startDate, "", null, null);
        BenefitCollectBean r2023 = r.getData();
        list.add(r2023);
        // 汇总
        BenefitCollectBean all = new BenefitCollectBean();
        all.setName("Total");
        all.setDt(r2022.getDt().add(r2023.getDt()));
        all.setMnt(r2022.getMnt().add(r2023.getMnt()));
        all.setPrt(r2022.getPrt().add(r2023.getPrt()));
        list.add(all);
        return R.success(list);
    }


    @GetMapping("/getProject/{bu}")
    @ApiOperation("获取TC专案")
    public R getTCProject(@PathVariable String bu, @RequestParam(value = "projectName") String projectName) {
        return benefitService.getTCProject(bu, projectName);
    }


    @GetMapping("/getReportByDate/{bu}")
    @ApiOperation("获取效益报表json 数据 ")
    public R<List<BenefitReportBean>> getReportDataByDate(@PathVariable String bu,
                                                          @RequestParam(value = "startDate", required = false) String startDate,
                                                          @RequestParam(value = "projectId", required = false) String projectId,
                                                          @RequestParam(value = "projectName",
                                                                  required = false) String projectName) {
        BU buEnum = BU.valueOf(bu);
        String resultFile = null;
        try {
            if (StringUtil.isEmpty(startDate) && StringUtil.isEmpty(projectId)) {
                return R.error(HttpResultEnum.SERVER_ERROR.getCode(), "开始时间和专案ID必填一个");
            }
            List<BenefitReportBean> resultData = new ArrayList<>();
            R result = null;
            if (StringUtil.isNotEmpty(startDate)) {
                Map<String, Integer> sheetIndexMap = new HashMap<>();
                sheetIndexMap.put("DT FTE效益計算", 0);
                sheetIndexMap.put("MNT FTE效益計算", 1);
                sheetIndexMap.put("PRT FTE效益計算", 2);
                sheetIndexMap.put("专案清单", 3);
                result = spasProjectService.getSpasProjectByDate(startDate, bu, null, sheetIndexMap);
                if (result.getCode().equalsIgnoreCase(HttpResultEnum.SUCCESS.getCode())) {
                    resultFile = (String) result.getData();
                } else {
                    return result;
                }
            } else if (StringUtil.isNotEmpty(projectId)) {
                result = spasProjectService.getSingleSpasProject(projectId, projectName, bu);
                if (result.getCode().equalsIgnoreCase(HttpResultEnum.SUCCESS.getCode())) {
                    resultFile = (String) result.getData();
                } else {
                    return result;
                }
            }

            switch (buEnum) {
                case DT:
                    resultData = benefitReportForDT.exportReportForJson(resultFile, startDate, projectName);
                    break;
                case MNT:
                    resultData = benefitReportForMNT.exportReportForJson(resultFile, startDate, projectName);
                    break;
                case PRT:
                    resultData = benefitReportForPRT.exportReportForJson(resultFile, startDate, projectName);
                    break;
                default:
                    break;
            }
            if (StringUtil.isNotEmpty(resultFile)) {
                File file = new File(resultFile);
                if (file.exists()) {
                    file.delete(); // 对于执行成功的代码，需要将生成的resultFile路径的文件删除
                }
            }
            return R.success(resultData);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), e.getLocalizedMessage());
        }
    }

    @GetMapping("/exportReportByDate/{bu}")
    @ApiOperation("导出效益报表Excel ")
    public ResponseEntity<byte[]> exportReportDataByDate(@PathVariable String bu,
                                                         @RequestParam(value = "startDate", required = false) String startDate,
                                                         @RequestParam(value = "projectId", required = false) String projectId,
                                                         @RequestParam(value = "projectName", required = false) String projectName, Workbook wb,
                                                         Map<String, Integer> sheetIndexMap) {
        if (sheetIndexMap == null) {
            // 设置默认参数
            sheetIndexMap = new HashMap<>();
            sheetIndexMap.put("DT FTE效益計算", 0);
            sheetIndexMap.put("MNT FTE效益計算", 1);
            sheetIndexMap.put("PRT FTE效益計算", 2);
            sheetIndexMap.put("专案清单", 3);
        }
        HttpHeaders headers = new HttpHeaders();
        BU buEnum = BU.valueOf(bu);
        String resultFile = null;
        try {
            if (StringUtil.isEmpty(startDate) && StringUtil.isEmpty(projectId)) {
                return new ResponseEntity<byte[]>(headers, HttpStatus.SERVICE_UNAVAILABLE);
            }
//            ByteArrayOutputStream out = null;
            R result = null;
            if (StringUtil.isNotEmpty(startDate)) {
                result = spasProjectService.getSpasProjectByDate(startDate, bu, wb, sheetIndexMap);
                if (result.getCode().equalsIgnoreCase(HttpResultEnum.SUCCESS.getCode())) {
                    resultFile = result.getData().toString();
                } else {
                    byte[] bytes = result.getMsg().getBytes();
//                    return new ResponseEntity<byte[]>(bytes, null, HttpStatus.INTERNAL_SERVER_ERROR);
                    return new ResponseEntity<byte[]>(headers, HttpStatus.SERVICE_UNAVAILABLE);
                }

            } else if (StringUtil.isNotEmpty(projectId)) {
                result = spasProjectService.getSingleSpasProject(projectId, projectName, bu);
                if (result.getCode().equalsIgnoreCase(HttpResultEnum.SUCCESS.getCode())) {
                    resultFile = result.getData().toString();
                } else {
                    byte[] bytes = result.getMsg().getBytes();
//                    return new ResponseEntity<byte[]>(bytes, null, HttpStatus.INTERNAL_SERVER_ERROR);
                    return new ResponseEntity<byte[]>(headers, HttpStatus.SERVICE_UNAVAILABLE);
                }
            }
//            switch (buEnum) {
//                case DT:
//                    out = benefitReportForDT.exportReport(resultFile,startDate, projectName);
//                    break;
//                case MNT:
//                    out = benefitReportForMNT.exportReport(resultFile,startDate, projectName);
//                    break;
//                case PRT:
//                    out = benefitReportForPRT.exportReport(resultFile,startDate, projectName);
//                    break;
//                default:
//                    break;
//            }

            boolean externWorkbook = (wb != null);
            if (wb == null) {
                wb = ExcelUtil.getLocalWorkbook(resultFile);
            }
            String content = "";
            if (StringUtil.isNotEmpty(startDate)) {
                content = DateUtil.getMonth(startDate) + "月效益Total (KUSD)";
            } else if (StringUtil.isNotEmpty(projectName)) {
                content = projectName + " 专案效益Total (KUSD)";
            }

            switch (buEnum) {
                case DT:
                    wb.setActiveSheet(sheetIndexMap.get("DT FTE效益計算")); // 设置激活
                    hiddenSheet(wb, sheetIndexMap, "MNT FTE效益計算"); // 设置隐藏
                    hiddenSheet(wb, sheetIndexMap, "PRT FTE效益計算"); // 设置隐藏
                    if (StringUtil.isNotEmpty(projectId)) {
                        hiddenSheet(wb, sheetIndexMap, "专案清单"); // 设置隐藏
                    }

                    Sheet sheet_dt = wb.getSheetAt(sheetIndexMap.get("DT FTE效益計算"));
                    if (StringUtil.isNotEmpty(startDate)) {
                        ExcelUtil.setValueAtForString(sheet_dt, 32, 9, startDate);
                    } else if (StringUtil.isNotEmpty(projectName)) {
                        ExcelUtil.setValueAtForString(sheet_dt, 32, 9, projectName);
                    }
                    ExcelUtil.setValueAtForString(sheet_dt, 68, 5, content);

                    break;
                case MNT:
                    wb.setActiveSheet(sheetIndexMap.get("MNT FTE效益計算"));
                    hiddenSheet(wb, sheetIndexMap, "DT FTE效益計算"); // 设置隐藏
                    hiddenSheet(wb, sheetIndexMap, "PRT FTE效益計算"); // 设置隐藏
                    if (StringUtil.isNotEmpty(projectId)) {
                        hiddenSheet(wb, sheetIndexMap, "专案清单"); // 设置隐藏
                    }

                    Sheet sheet_mnt = wb.getSheetAt(sheetIndexMap.get("MNT FTE效益計算"));
                    if (StringUtil.isNotEmpty(startDate)) {
                        ExcelUtil.setValueAtForString(sheet_mnt, 138, 9, startDate);
                    } else if (StringUtil.isNotEmpty(projectName)) {
                        ExcelUtil.setValueAtForString(sheet_mnt, 138, 9, projectName);
                    }
                    ExcelUtil.setValueAtForString(sheet_mnt, 208, 5, content);

                    break;
                case PRT:
                    wb.setActiveSheet(sheetIndexMap.get("PRT FTE效益計算"));
                    hiddenSheet(wb, sheetIndexMap, "DT FTE效益計算"); // 设置隐藏
                    hiddenSheet(wb, sheetIndexMap, "MNT FTE效益計算"); // 设置隐藏
                    if (StringUtil.isNotEmpty(projectId)) {
                        hiddenSheet(wb, sheetIndexMap, "专案清单"); // 设置隐藏
                    }

                    Sheet sheet_prt = wb.getSheetAt(sheetIndexMap.get("PRT FTE效益計算"));
                    if (StringUtil.isNotEmpty(startDate)) {
                        ExcelUtil.setValueAtForString(sheet_prt, 19, 9, startDate);
                    } else if (StringUtil.isNotEmpty(projectName)) {
                        ExcelUtil.setValueAtForString(sheet_prt, 19, 9, projectName);
                    }
                    ExcelUtil.setValueAtForString(sheet_prt, 35, 5, content);
                    break;
            }
            if (externWorkbook) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            wb.close(); // 关闭此对象，便于后续删除此文件
            out.flush();
            headers.setContentDispositionFormData("attachment", bu + "_" + startDate + "_benefitReport.xlsx");
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            if (StringUtil.isNotEmpty(resultFile)) {
                File file = new File(resultFile);
                if (file.exists()) {
                    file.delete(); // 对于执行成功的代码，需要将生成的resultFile路径的文件删除
                }
            }
            return new ResponseEntity<byte[]>(out.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<byte[]>(headers, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @GetMapping("/getRowData/{bu}")
    @ApiOperation("获取效益的RowData")
    public R getBenefitRowData(@PathVariable String bu, @RequestParam(value = "startDate", required = false) String startDate,
                               @RequestParam(value = "projectId", required = false) String projectId) {
        return benefitService.getBenefitRowData(bu, startDate, projectId);
    }

    private void hiddenSheet(Workbook wb, Map<String, Integer> sheetIndexMap, String sheetName) {
        Integer index = sheetIndexMap.get(sheetName);
        if (index == null) {
            return;
        }
        wb.setSheetHidden(index, true);
    }

    @GetMapping("/getDetailData/{bu}")
    public ResponseEntity<byte[]> getDetailData(@PathVariable String bu, @RequestParam(value = "startDate") String startDate) {
        R<List<RowDataBean>> r = benefitService.getBenefitRowData(bu, startDate, "");
        List<RowDataBean> list = r.getData();
        if (list == null) {
            list = new ArrayList<>();
        }
        list.forEach(e -> e.setCreateName(e.getCreator() + "/" + e.getCreateName()));
        HttpHeaders headers = new HttpHeaders();
        Workbook workbook = null;
        try {
            workbook = ExcelUtil.getWorkbookNew(ExcelConfig.FTE2022_DETAIL);
            ExcelUtil.setCellValue(list, 1, 9, workbook.getSheetAt(0), ExcelUtil.getCellStyle(workbook));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            headers.setContentDispositionFormData("attachment", bu + "_" + startDate + "_DetailData.xlsx");
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            return new ResponseEntity<byte[]>(out.toByteArray(), headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<byte[]>(headers, HttpStatus.SERVICE_UNAVAILABLE);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
