package com.foxconn.plm.tcreport.drawcountreport.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.foxconn.plm.tcreport.drawcountreport.domain.*;
import com.foxconn.plm.tcreport.drawcountreport.service.DrawCountService;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.hutool.poi.excel.cell.CellUtil;
import com.foxconn.plm.tcreport.drawcountreport.service.DrawCountReportService;
import com.foxconn.plm.tcreport.mapper.ReportSearchMapper;
import com.foxconn.plm.tcreport.reportsearchparams.domain.LovBean;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author HuashengYu
 * @Date 2023/1/3 14:24
 * @Version 1.0
 */
@Service
public class DrawCountReportServiceImpl implements DrawCountReportService {
    private static Log log = LogFactory.get();
    @Resource
    private DrawCountService service;
    @Resource
    private ReportSearchMapper reportSearchMapper;

    @Override
    public List<DrawCountRes> getDrawCountRecordList(QueryBean queryBean) {
        if (StrUtil.isBlank(queryBean.getReportDate())) {
            // 默認為昨天的時間
            String yesterday = DateUtil.format(DateUtil.yesterday(), "yyyy-MM-dd");
            queryBean.setReportDate(yesterday);
        }
        // 查詢匹配到的專案
        List<LovBean> paramList = reportSearchMapper.getLovByParam(queryBean.getBu(), queryBean.getCustomer(), queryBean.getProductLine(),
                queryBean.getProjectSeries(), queryBean.getProjectName(), queryBean.getProjectId());
        if (CollUtil.isEmpty(paramList)) {
            return Collections.emptyList();
        }
        List<String> projectIds = paramList.parallelStream()
                .filter(item -> !"Lenovo L5".equals(item.getCustomer()))
                .map(item -> item.getProjectInfo().substring(0, item.getProjectInfo().indexOf("-")))
                .collect(Collectors.toList());

        List<DrawCountEntity> list = service.list(new QueryWrapper<DrawCountEntity>().lambda()
                .eq(DrawCountEntity::getReportDate, queryBean.getReportDate())
                .in(DrawCountEntity::getProjectId, projectIds)
        );
        return list.stream().filter(item -> {
            if (("DT".equals(item.getBu()) || "PRT".equals(item.getBu())) && ("None".equals(item.getChassis()) || "E3".equals(item.getChassis()))) {
                return false;
            }else if ("MNT".equals(item.getBu()) && CollUtil.newHashSet("E2(B)","E2(C)","E3(E)","E3(F)").contains(item.getChassis())) {
                return false;
            }
            return true;
        }).map(item -> {
            DrawCountRes res = new DrawCountRes();
            BeanUtil.copyProperties(item, res);
            if (StrUtil.isBlank(item.getDesignTreeName())) {
                res.setDesignTreeName("");
            }
            if (StrUtil.isBlank(item.getDesignTreeType())) {
                res.setDesignTreeType("");
            }
            if (StrUtil.isBlank(item.getOwner())) {
                res.setOwner("");
            }
            if (StrUtil.isBlank(item.getItemCode())) {
                res.setItemCode("");
            }
            if (StrUtil.isBlank(item.getItemName())) {
                res.setItemName("");
            }
            if (StrUtil.isBlank(item.getItemType())) {
                res.setItemType("");
            }
            return res;
        }).sorted(Comparator.comparing(DrawCountRes::getBu).thenComparing(DrawCountRes::getCustomer)
                .thenComparing(DrawCountRes::getProductLine).thenComparing(DrawCountRes::getProjectSeries)
                .thenComparing(DrawCountRes::getProjectName).thenComparing(DrawCountRes::getDesignTreeType)
                .thenComparing(DrawCountRes::getDesignTreeName).thenComparing(DrawCountRes::getOwner)
                .thenComparing(DrawCountRes::getItemCode).thenComparing(DrawCountRes::getItemName).thenComparing(DrawCountRes::getItemType)
        ).collect(Collectors.toList());
    }

    @Override
    public void exportDrawCountRecordList(HttpServletResponse response, QueryBean queryBean) {
        List<DrawCountRes> list = getDrawCountRecordList(queryBean);
        // 寫出到excel
        InputStream is = null;
        try {
            ClassPathResource cpr = new ClassPathResource("/templates/DrawCountTemplates.xlsx");
            is = cpr.getInputStream();
            XSSFWorkbook workbook = new XSSFWorkbook(is);
            if (CollectionUtil.isNotEmpty(list)) {
                XSSFSheet sheet = workbook.getSheetAt(0);
                sheet.setAutobreaks(true);
                XSSFCellStyle cellStyle = workbook.getCellStyleAt(0);
                cellStyle.setAlignment(HorizontalAlignment.CENTER);
                cellStyle.setWrapText(true);
                cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                cellStyle.setBorderBottom(BorderStyle.THIN);
                cellStyle.setBorderLeft(BorderStyle.THIN);
                cellStyle.setBorderTop(BorderStyle.THIN);
                cellStyle.setBorderRight(BorderStyle.THIN);
                int index = 3;
                String prefProjectKey = "", prefDesignTreeNameKey = "", prefDesignTreeTypeKey = "",
                        prefItemCodeKey = "", prefItemNameKey = "", prefItemTypeKey = "", prefOwnKey = "";
                Map<String, Integer> margeMap = new HashMap<>();
                for (DrawCountRes entity : list) {
                    XSSFRow row = sheet.createRow(index);
                    row.createCell(0).setCellValue(entity.getBu());
                    row.createCell(1).setCellValue(entity.getCustomer());
                    row.createCell(2).setCellValue(entity.getProductLine());
                    row.createCell(3).setCellValue(entity.getProjectSeries());
                    row.createCell(4).setCellValue(entity.getProjectName());
                    row.createCell(5).setCellValue(entity.getPhase());
                    row.createCell(6).setCellValue(entity.getChassis());
                    row.createCell(7).setCellValue(entity.getProjectId());
                    row.createCell(8).setCellValue(entity.getDesignTreeType());
                    row.createCell(9).setCellValue(entity.getDesignTreeName());
                    row.createCell(10).setCellValue(entity.getOwner());
                    row.createCell(11).setCellValue(entity.getOwnerGroup());
                    row.createCell(12).setCellValue(entity.getActualUser());
                    row.createCell(13).setCellValue(entity.getItemCode());
                    row.createCell(14).setCellValue(entity.getItemName());
                    row.createCell(15).setCellValue(entity.getItemType());
                    row.createCell(16).setCellValue(ObjectUtil.isNull(entity.getUploadNum()) ? "" : entity.getUploadNum() + "");
                    row.createCell(17).setCellValue(ObjectUtil.isNull(entity.getReleaseNum()) ? "" : entity.getReleaseNum() + "");
                    row.createCell(18).setCellValue(entity.getReleaseProgress());
                    row.createCell(19).setCellValue(ObjectUtil.isNull(entity.getReleaseModelNum()) ? "" : entity.getReleaseModelNum() + "");
                    row.createCell(20).setCellValue(entity.getItemCompleteness());
                    row.createCell(21).setCellValue(entity.getDrawCompleteness());
                    // 判斷是否需要合併
                    String projectKey = entity.getBu() + entity.getCustomer() + entity.getProductLine() + entity.getProjectSeries() + entity.getProjectName();
                    if (ObjectUtil.isNull(margeMap.get(projectKey))) {
                        margeMap.put(projectKey, index);
                        if (StrUtil.isBlank(prefProjectKey)) {
                            prefProjectKey = projectKey;
                        }
                        if (index != 3 && StrUtil.isNotBlank(prefProjectKey)) {
                            // 說明項目換了 需要合併
                            Integer startRow = margeMap.get(prefProjectKey);
                            if (startRow + 1 < index) {
                                CellUtil.mergingCells(sheet, startRow, index - 1, 0, 0, cellStyle);
                                CellUtil.mergingCells(sheet, startRow, index - 1, 1, 1, cellStyle);
                                CellUtil.mergingCells(sheet, startRow, index - 1, 2, 2, cellStyle);
                                CellUtil.mergingCells(sheet, startRow, index - 1, 3, 3, cellStyle);
                                CellUtil.mergingCells(sheet, startRow, index - 1, 4, 4, cellStyle);
                                CellUtil.mergingCells(sheet, startRow, index - 1, 5, 5, cellStyle);
                                CellUtil.mergingCells(sheet, startRow, index - 1, 6, 6, cellStyle);
                                CellUtil.mergingCells(sheet, startRow, index - 1, 7, 7, cellStyle);
                                CellUtil.mergingCells(sheet, startRow, index - 1, 21, 21, cellStyle);
                            }
                            prefProjectKey = projectKey;
                        }
                    }
                    String designTreeTypeKey = projectKey + (StrUtil.isNotBlank(entity.getDesignTreeType()) ? entity.getDesignTreeType().trim() : "null");
                    if (ObjectUtil.isNull(margeMap.get(designTreeTypeKey))) {
                        margeMap.put(designTreeTypeKey, index);
                        if (StrUtil.isBlank(prefDesignTreeTypeKey)) {
                            prefDesignTreeTypeKey = designTreeTypeKey;
                        }
                        if (index != 3 && StrUtil.isNotBlank(prefDesignTreeTypeKey)) {
                            // 說明协同结构树类别換了 需要合併
                            Integer startRow = margeMap.get(prefDesignTreeTypeKey);
                            if (startRow + 1 < index) {
                                CellUtil.mergingCells(sheet, startRow, index - 1, 8, 8, cellStyle);
                            }
                            prefDesignTreeTypeKey = designTreeTypeKey;
                        }
                    }
                    String designTreeNameKey = designTreeTypeKey + (StrUtil.isNotBlank(entity.getDesignTreeName()) ? entity.getDesignTreeName().trim() : "null");
                    if (ObjectUtil.isNull(margeMap.get(designTreeNameKey))) {
                        margeMap.put(designTreeNameKey, index);
                        if (StrUtil.isBlank(prefDesignTreeNameKey)) {
                            prefDesignTreeNameKey = designTreeNameKey;
                        }
                        if (index != 3 && StrUtil.isNotBlank(prefDesignTreeNameKey)) {
                            // 說明协同结构树名称換了 需要合併
                            Integer startRow = margeMap.get(prefDesignTreeNameKey);
                            if (startRow + 1 < index) {
                                CellUtil.mergingCells(sheet, startRow, index - 1, 9, 9, cellStyle);
                            }
                            prefDesignTreeNameKey = designTreeNameKey;
                        }
                    }
                    String ownKey = designTreeNameKey + (StrUtil.isNotBlank(entity.getOwner()) ? entity.getOwner().trim() : "null");
                    if (ObjectUtil.isNull(margeMap.get(ownKey))) {
                        margeMap.put(ownKey, index);
                        if (StrUtil.isBlank(prefOwnKey)) {
                            prefOwnKey = ownKey;
                        }
                        if (index != 3 && StrUtil.isNotBlank(prefOwnKey)) {
                            // 說明协同结构树名称換了 需要合併
                            Integer startRow = margeMap.get(prefOwnKey);
                            if (startRow + 1 < index) {
                                CellUtil.mergingCells(sheet, startRow, index - 1, 10, 10, cellStyle);
                                CellUtil.mergingCells(sheet, startRow, index - 1, 11, 11, cellStyle);
                                CellUtil.mergingCells(sheet, startRow, index - 1, 12, 12, cellStyle);
                            }
                            prefOwnKey = ownKey;
                        }
                    }

                    String itemCodeKey = ownKey + (StrUtil.isNotBlank(entity.getItemCode()) ? entity.getItemCode().trim() : "null");
                    if (ObjectUtil.isNull(margeMap.get(itemCodeKey))) {
                        margeMap.put(itemCodeKey, index);
                        if (StrUtil.isBlank(prefItemCodeKey)) {
                            prefItemCodeKey = itemCodeKey;
                        }
                        if (index != 3 && StrUtil.isNotBlank(prefItemCodeKey)) {
                            // 說明零件编码換了 需要合併
                            Integer startRow = margeMap.get(prefItemCodeKey);
                            if (startRow + 1 < index) {
                                CellUtil.mergingCells(sheet, startRow, index - 1, 13, 13, cellStyle);
                            }
                            prefItemCodeKey = itemCodeKey;
                        }
                    }
                    String itemNameKey = itemCodeKey + (StrUtil.isNotBlank(entity.getItemName()) ? entity.getItemName().trim() : "null");
                    if (ObjectUtil.isNull(margeMap.get(itemNameKey))) {
                        margeMap.put(itemNameKey, index);
                        if (StrUtil.isBlank(prefItemNameKey)) {
                            prefItemNameKey = itemNameKey;
                        }
                        if (index != 3 && StrUtil.isNotBlank(prefItemNameKey)) {
                            // 說明零件名称換了 需要合併
                            Integer startRow = margeMap.get(prefItemNameKey);
                            if (startRow + 1 < index) {
                                CellUtil.mergingCells(sheet, startRow, index - 1, 14, 14, cellStyle);
                            }
                            prefItemNameKey = itemNameKey;
                        }
                    }
                    String itemTypeKey = itemNameKey + (StrUtil.isNotBlank(entity.getItemType()) ? entity.getItemType().trim() : "null");
                    if (ObjectUtil.isNull(margeMap.get(itemTypeKey))) {
                        margeMap.put(itemTypeKey, index);
                        if (StrUtil.isBlank(prefItemTypeKey)) {
                            prefItemTypeKey = itemTypeKey;
                        }
                        if (index != 3 && StrUtil.isNotBlank(prefItemTypeKey)) {
                            // 說明零件名称換了 需要合併
                            Integer startRow = margeMap.get(prefItemTypeKey);
                            if (startRow + 1 < index) {
                                CellUtil.mergingCells(sheet, startRow, index - 1, 15, 15, cellStyle);
                            }
                            prefItemTypeKey = itemTypeKey;
                        }
                    }

                    index++;
                }
                // 合併最後的數據
                if (margeMap.get(prefProjectKey) != index - 1) {
                    CellUtil.mergingCells(sheet, margeMap.get(prefProjectKey), index - 1, 0, 0, cellStyle);
                    CellUtil.mergingCells(sheet, margeMap.get(prefProjectKey), index - 1, 1, 1, cellStyle);
                    CellUtil.mergingCells(sheet, margeMap.get(prefProjectKey), index - 1, 2, 2, cellStyle);
                    CellUtil.mergingCells(sheet, margeMap.get(prefProjectKey), index - 1, 3, 3, cellStyle);
                    CellUtil.mergingCells(sheet, margeMap.get(prefProjectKey), index - 1, 4, 4, cellStyle);
                    CellUtil.mergingCells(sheet, margeMap.get(prefProjectKey), index - 1, 5, 5, cellStyle);
                    CellUtil.mergingCells(sheet, margeMap.get(prefProjectKey), index - 1, 6, 6, cellStyle);
                    CellUtil.mergingCells(sheet, margeMap.get(prefProjectKey), index - 1, 7, 7, cellStyle);
                    CellUtil.mergingCells(sheet, margeMap.get(prefProjectKey), index - 1, 21, 21, cellStyle);
                }
                if (margeMap.get(prefDesignTreeTypeKey) != index - 1) {
                    CellUtil.mergingCells(sheet, margeMap.get(prefDesignTreeTypeKey), index - 1, 8, 8, cellStyle);
                }
                if (margeMap.get(prefDesignTreeNameKey) != index - 1) {
                    CellUtil.mergingCells(sheet, margeMap.get(prefDesignTreeNameKey), index - 1, 9, 9, cellStyle);
                }
                if (margeMap.get(prefOwnKey) != index - 1) {
                    CellUtil.mergingCells(sheet, margeMap.get(prefDesignTreeNameKey), index - 1, 10, 10, cellStyle);
                    CellUtil.mergingCells(sheet, margeMap.get(prefDesignTreeNameKey), index - 1, 11, 11, cellStyle);
                    CellUtil.mergingCells(sheet, margeMap.get(prefDesignTreeNameKey), index - 1, 12, 12, cellStyle);
                }
                if (margeMap.get(prefItemCodeKey) != index - 1) {
                    CellUtil.mergingCells(sheet, margeMap.get(prefItemCodeKey), index - 1, 13, 13, cellStyle);
                }
                if (margeMap.get(prefItemNameKey) != index - 1) {
                    CellUtil.mergingCells(sheet, margeMap.get(prefItemNameKey), index - 1, 14, 14, cellStyle);
                }
                if (margeMap.get(prefItemTypeKey) != index - 1) {
                    CellUtil.mergingCells(sheet, margeMap.get(prefItemTypeKey), index - 1, 15, 15, cellStyle);
                }
            }
            response.setCharacterEncoding("UTF-8");
            response.setHeader("content-type", "application/vnd.ms-excel;charset=utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=\"" + URLEncoder.encode("進行中專案3D模型建制進度報表(機構&系統).xlsx", "UTF-8") + "\"");
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();

        } catch (Exception e) {
            log.error("導出進行中專案3D模型建制進度報表(機構&系統)錯誤，錯誤原因", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    LogFactory.get().warn("关闭流错误,错误原因：{}", e.getMessage());
                }
            }
        }
    }
}
