package com.foxconn.plm.tcservice.ebom.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcservice.ebom.domain.QuotationBOMBean;
import com.foxconn.plm.tcservice.ebom.service.impl.EBOMServiceImpl;
import com.foxconn.plm.tcservice.ebom.service.impl.SecondSourceServiceImpl;
import com.foxconn.plm.utils.excel.ExcelUtil;
import com.foxconn.plm.utils.tc.ItemUtil;
import com.foxconn.plm.utils.tc.QueryUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/mntebom")
public class MNTEBOMController {

    private final Log log = LogFactory.get();
    @Resource
    EBOMServiceImpl ebomService;

    @Resource
    private SecondSourceServiceImpl secondSourceService;

    @RequestMapping("/noDifferenceNotice")
    public String NoDifferenceNotice(String taskUid) {
        TCSOAServiceFactory tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
        try {
            ebomService.NoDifferenceNotice(tcsoaServiceFactory, taskUid);
            return "S";
        } catch (Exception e) {
            log.error(e);
            e.printStackTrace();
        } finally {
            tcsoaServiceFactory.logout();
        }
        return "F";
    }

    @PostMapping("/convertQuotationBOM")
    public R<List<QuotationBOMBean>> convertQuotationBOM(MultipartFile file) {
        List<QuotationBOMBean> list = new ArrayList<>();
        try {
            byte[] fileBytes = file.getBytes();
            String fileContent = new String(fileBytes, StandardCharsets.UTF_8);
            String[] lines = fileContent.split("\r\n");
            QuotationBOMBean temp = null;
            int index = 0;
            if (lines.length > 14) {
                for (int i = 14; i < lines.length; i++) {
                    String[] beanStrs = lines[i].split("\t");
                    if (beanStrs.length >= 10) {
                        temp = QuotationBOMBean.newQuotationBOMBean(beanStrs);
                        temp.setIndex(index);
                        list.add(temp);
                        index++;
                    } else if (beanStrs.length == 6 && temp != null) {
                        temp.setLocation(temp.getLocation() + beanStrs[5]);
                    }
                }
            } else {
                R.error(HttpResultEnum.PARAM_ERROR.getCode(), "upload bom file format is error!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        if (list.size() > 0) {
            TCSOAServiceFactory tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            BigDecimal thousand = new BigDecimal("1000");
            list.stream().forEach(bomBean -> {
                try {
                    String stdPn = bomBean.getStdPn();
                    if (StrUtil.isNotEmpty(stdPn)) {
                        Item item = TCUtils.queryItemByIDOrName(tcsoaServiceFactory.getSavedQueryService(),
                                tcsoaServiceFactory.getDataManagementService(),
                                stdPn,
                                "");
                        if (item != null) {
                            ItemRevision itemRev = ItemUtil.getItemLatestRevision(tcsoaServiceFactory.getDataManagementService(), item);
                            tcsoaServiceFactory.getDataManagementService().getProperties(new ModelObject[]{itemRev}, new String[]{"d9_Un"});
                            String unit = itemRev.getPropertyDisplayableValue("d9_Un");
                            bomBean.setUnit(unit);
                            if ("KEA".equalsIgnoreCase(unit)) {
                                if (StrUtil.isNotEmpty(bomBean.getQty())) {
                                    BigDecimal newQty = new BigDecimal(bomBean.getQty()).divide(thousand);
                                    bomBean.setQty(newQty.toPlainString());
                                }
                            }
                        } else {
                            bomBean.setNotes("Don′t exist in the Teamcenter!");
                        }
                    } else {
                        bomBean.setNotes("Error , STD PN is empty!");
                    }

                } catch (Exception e) {
                    log.info(bomBean.getStdPn() + " error :: " + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            });
            tcsoaServiceFactory.logout();
        }
        // list.sort(Comparator.comparing(QuotationBOMBean::getIndex));
        return R.success(list);
    }

    @PostMapping("/downloadQuotationBOMExcel")
    public ResponseEntity<byte[]> downloadQuotationBOMExcel(@RequestBody List<QuotationBOMBean> dataList) {
        HttpHeaders headers = new HttpHeaders();
        if (dataList != null && dataList.size() > 0) {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                String fileName = "BOMFile.xlsx";
                headers.setContentDispositionFormData("attachment", fileName);
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                Workbook workbook = ExcelUtil.getWorkbookNew("/templates/BOMFile.xlsx");
                ExcelUtil.setCellValue(dataList, 1, 13, workbook.getSheetAt(0), ExcelUtil.getCellStyle(workbook));
                workbook.write(out);
                return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ResponseEntity<>(headers, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @PostMapping("/sync2ndSourceQuotationBOM")
    public R<List<QuotationBOMBean>> sync2ndSourceQuotationBOM(@RequestBody List<QuotationBOMBean> dataList) {
        log.info("==>> sync2ndSourceQuotationBOM dataList: " + JSONUtil.toJsonStr(dataList));
        return secondSourceService.sync2ndSourceQuotationBOM(dataList);
    }
}
