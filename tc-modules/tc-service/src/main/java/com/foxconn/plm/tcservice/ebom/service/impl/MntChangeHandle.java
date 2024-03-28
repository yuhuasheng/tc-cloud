package com.foxconn.plm.tcservice.ebom.service.impl;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcservice.ebom.constant.ChangeAction;
import com.foxconn.plm.tcservice.ebom.domain.*;
import com.foxconn.plm.tcservice.ebom.util.EBOMExcelUtil;
import com.foxconn.plm.utils.excel.ExcelUtil;
import com.foxconn.plm.utils.string.StringUtil;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

public class MntChangeHandle extends ChangeListHandle {
    private final static String SHEET_END_STR = "表單編號Form No.：AI3OT-0001-02(V02)                                                                   " +
            "                                                                                                                                      " +
            "                 頁數/總頁數Page/Total Pages：1/1\r\n" + "版權屬於 PCEBG所有.禁止任何未經授權的使用.\r\n" + "The copyright belongs to PCEBG. Any unauthorized" +
            " use is prohibited..\r\n" + "";
    private List<MntChangeListBean> mntChangeList;
    private List<MntDCNChangeBean> mntDCNChangeList;
    private ItemRevision targetItemRev;

    public MntChangeHandle(ItemRevision sourceItemRev, ItemRevision targetItemRev, TCSOAServiceFactory tcsoaServiceFactory) throws Exception {
        super(sourceItemRev, targetItemRev, tcsoaServiceFactory);
        this.mntChangeList = new ArrayList<>();
        this.targetItemRev = targetItemRev;
        tcsoaServiceFactory.getDataManagementService().getProperties(new ModelObject[]{targetItemRev}, new String[]{"d9_ModelName"});
        this.mntDCNChangeList = new ArrayList<MntDCNChangeBean>();
    }

    public void compareBOM() {
        compareBOM(sourceBomBean, targetBomBean);
        for (int i = 0; i < mntChangeList.size(); i++) {
            mntChangeList.get(i).setNo((i + 1) + "");
        }
    }

    public void compareBOM(EBOMLineBean sourceBomBean, EBOMLineBean targetBomBean) {
        MergedBOM(targetBomBean);
        MergedBOM(sourceBomBean);
        CompareBOM result = new CompareBOM(targetBomBean, sourceBomBean);
        addChangeMainSource(result.getAdd(), ChangeAction.Add);
        addChangeMainSource(result.getDel(), ChangeAction.Delete);
        List<EBOMUpdateBean> beans = result.getChangeQty();
        List<EBOMUpdateBean> changeBeans = beans.stream().filter(e -> e.getChangeFiledNames().size() > 0).collect(Collectors.toList());
        List<MntChangeListBean> tempChangeList = new ArrayList<MntChangeListBean>();
        for (EBOMUpdateBean eBean : changeBeans) {
            MntChangeListBean changeBean = new MntChangeListBean(eBean.getNewEBomBean(), false, ChangeAction.Change);
            changeBean.setBefore_qty(eBean.getOldEBomBean().getQty());
            tempChangeList.add(changeBean);
        }
        if (tempChangeList.size() > 1) {
            tempChangeList.get(0).setMergedRegionInfos(getMergeRegions(tempChangeList.size() - 1));
        }
        mntChangeList.addAll(tempChangeList);
        // 2nd change:
        add2ndChange(beans, ChangeAction.Add);
        add2ndChange(beans, ChangeAction.Delete);
        for (EBOMUpdateBean changeBean : beans) {
            compareBOM(changeBean.getOldEBomBean(), changeBean.getNewEBomBean());
        }
    }

    public void writeDCNExcel(OutputStream out, ItemRevision dcnItemRevision) throws Exception {
        compareDCNBOM(sourceBomBean, targetBomBean, true);
        EBOMExcelUtil util = new EBOMExcelUtil();
        Workbook wb = util.getWorkbook(MntDerivativeCompare.TEMPLATE);
        if (mntDCNChangeList.size() > 0) {
            Sheet sheet = wb.getSheetAt(0);
            sheet.getRow(3).getCell(0).setCellValue("Model:" + targetItemRev.getPropertyObject("d9_ModelName").getStringValue());
            if (dcnItemRevision != null) {
                sheet.getRow(3).getCell(9).setCellValue("DCN/ECN NO.:" + dcnItemRevision.getPropertyObject("item_id").getStringValue());
            }
            for (int i = 0; i < mntDCNChangeList.size(); i++) {
                int rowLine = MntDerivativeCompare.START_ROW + i;
                MntDCNChangeBean pl = mntDCNChangeList.get(i);
                List<MergedRegionInfo> mergedList = pl.getMergedRegionInfos();
                if (mergedList != null) {
                    for (MergedRegionInfo meInfo : mergedList) {
                        util.setMerged(meInfo, MntDCNChangeBean.class, sheet, rowLine);
                    }
                }
            }
            util.setRichCellValue(mntDCNChangeList, MntDerivativeCompare.START_ROW, sheet, ExcelUtil.getCellStyle(wb));
        }
        wb.write(out);
        wb.close();
    }

    public List<MntChangeListBean> getMntChangeList() {
        return this.mntChangeList;
    }

    public List<MntDCNChangeBean> getMntDCNChangeList() {
        return this.mntDCNChangeList;
    }

    public void compareDCNBOM(EBOMLineBean sourceBomBean, EBOMLineBean targetBomBean, boolean isIteration) throws Exception {
        if (sourceBomBean == null) {
            sourceBomBean = this.sourceBomBean;
        }
        if (targetBomBean == null) {
            targetBomBean = this.targetBomBean;
        }
        List<MntDCNChangeBean> resultList = new ArrayList<MntDCNChangeBean>();
        MergedBOM(sourceBomBean);
        MergedBOM(targetBomBean);
        CompareBOM result = new CompareBOM(targetBomBean, sourceBomBean);
        List<EBOMLineBean> add = result.getAdd();
        List<EBOMLineBean> del = result.getDel();
        if (add.size() > 0) {
            MntDerivativeCompare change = new MntDerivativeCompare(add, ChangeAction.Add);
            resultList.addAll(change.getExtremeChange());
        }
        if (del.size() > 0) {
            MntDerivativeCompare change = new MntDerivativeCompare(del, ChangeAction.Delete);
            resultList.addAll(change.getExtremeChange());
        }
        List<EBOMUpdateBean> beanList = result.getSameBom();
        for (EBOMUpdateBean bomChangeBean : beanList) {
            MntDerivativeCompare change = new MntDerivativeCompare(bomChangeBean);
            resultList.addAll(change.getAllChange());
        }
        if (resultList.size() > 1) {
            List<MergedRegionInfo> mList = resultList.get(0).getMergedRegionInfos();
            if (mList == null) {
                mList = new ArrayList<MergedRegionInfo>();
            }
            mList.add(new MergedRegionInfo("parentPn", resultList.size() - 1));
            resultList.get(0).setMergedRegionInfos(mList);
        }
        mntDCNChangeList.addAll(resultList);
        if (isIteration) {
            for (EBOMUpdateBean bomChangeBean : beanList) {
                compareDCNBOM(bomChangeBean.getOldEBomBean(), bomChangeBean.getNewEBomBean(), isIteration);
            }
        }
    }

    public EBOMLineBean generateChangeList(EBOMLineBean oldBbean, String change) {
        return oldBbean;
    }

    private void MergedBOM(EBOMLineBean bomBean) {
        List<EBOMLineBean> list = new ArrayList(bomBean.getChilds().stream().collect(Collectors.toMap(EBOMLineBean::getUid, e -> e, (e1, e2) -> {
            if (StringUtil.isNotEmpty(e1.getQty()) && StringUtil.isNotEmpty(e2.getQty())) {
                e1.setQty(new BigDecimal(e1.getQty()).add(new BigDecimal(e2.getQty())) + "");
            }
            if (StringUtil.isNotEmpty(e1.getLocation()) && StringUtil.isNotEmpty(e2.getLocation())) {
                e1.setLocation(e1.getLocation() + "," + e2.getLocation());
            }
            return e1;
        })).values());
        bomBean.setChilds(list);
    }

    public static Workbook writeChangeListToExcel(List<MntChangeListBean> mntChangeList, MntChangeSheet changeSheet) throws Exception {
        EBOMExcelUtil util = new EBOMExcelUtil();
        Workbook wb = util.getWorkbook(MntChangeSheet.TEMPLATE);
        if (mntChangeList.size() > 0) {
            Sheet sheet = wb.getSheetAt(0);
            util.setRowCellVaule(changeSheet, sheet, null);
            for (int i = 0; i < mntChangeList.size(); i++) {
                int rowLine = MntChangeSheet.START_ROW + i;
                MntChangeListBean pl = mntChangeList.get(i);
                List<MergedRegionInfo> mergedList = pl.getMergedRegionInfos();
                if (mergedList != null) {
                    for (MergedRegionInfo meInfo : mergedList) {
                        util.setMerged(meInfo, MntChangeListBean.class, sheet, rowLine);
                    }
                }
            }
            util.setCellValue(mntChangeList, MntChangeSheet.START_ROW, sheet, ExcelUtil.getCellStyle(wb));
            int endRow = sheet.getLastRowNum();
            sheet.addMergedRegion(new CellRangeAddress(endRow + 1, endRow + 1, 0, 15));
            sheet.createRow(endRow + 1).createCell(0).setCellValue("以下空白");
            sheet.addMergedRegion(new CellRangeAddress(endRow + 2, endRow + 2, 0, 15));
            Cell endCell = sheet.createRow(endRow + 2).createCell(0);
            CellStyle cellStyle = util.getCellStyleFont(wb);
            cellStyle.setVerticalAlignment(VerticalAlignment.TOP);
            cellStyle.setAlignment(HorizontalAlignment.CENTER);
            endCell.setCellStyle(cellStyle);
            endCell.setCellValue(SHEET_END_STR);
        }
        return wb;
    }

    public Workbook writeExcel(MntChangeSheet changeSheet) throws Exception {
        EBOMExcelUtil util = new EBOMExcelUtil();
        Workbook wb = util.getWorkbook(MntChangeSheet.TEMPLATE);
        if (mntChangeList.size() > 0) {
            Sheet sheet = wb.getSheetAt(0);
            util.setRowCellVaule(changeSheet, sheet, null);
            for (int i = 0; i < mntChangeList.size(); i++) {
                int rowLine = MntChangeSheet.START_ROW + i;
                MntChangeListBean pl = mntChangeList.get(i);
                List<MergedRegionInfo> mergedList = pl.getMergedRegionInfos();
                if (mergedList != null) {
                    for (MergedRegionInfo meInfo : mergedList) {
                        util.setMerged(meInfo, MntChangeListBean.class, sheet, rowLine);
                    }
                }
            }
            util.setCellValue(mntChangeList, MntChangeSheet.START_ROW, sheet, ExcelUtil.getCellStyle(wb));
            int endRow = sheet.getLastRowNum();
            sheet.addMergedRegion(new CellRangeAddress(endRow + 1, endRow + 1, 0, 15));
            sheet.createRow(endRow + 1).createCell(0).setCellValue("以下空白");
            sheet.addMergedRegion(new CellRangeAddress(endRow + 2, endRow + 2, 0, 15));
            Cell endCell = sheet.createRow(endRow + 2).createCell(0);
            CellStyle cellStyle = util.getCellStyleFont(wb);
            cellStyle.setVerticalAlignment(VerticalAlignment.TOP);
            cellStyle.setAlignment(HorizontalAlignment.CENTER);
            endCell.setCellStyle(cellStyle);
            endCell.setCellValue(SHEET_END_STR);
        }
        return wb;
    }

    private void add2ndChange(List<EBOMUpdateBean> changeBeans, ChangeAction action) {
        List<MntChangeListBean> tempList = new ArrayList<MntChangeListBean>();
        for (EBOMUpdateBean changeBean : changeBeans) {
            List<EBOMLineBean> subs = null;
            if (ChangeAction.Add.equals(action)) {
                subs = changeBean.getAdd2nd();
            } else if (ChangeAction.Delete.equals(action)) {
                subs = changeBean.getDel2nd();
            }
            if (subs != null && subs.size() > 0) {
                MntChangeListBean mainBean = new MntChangeListBean(changeBean.getNewEBomBean(), false, ChangeAction.Change);
                mainBean.setMergedRegionInfos(getMergeRegions(subs.size()));
                if (ChangeAction.Add.equals(action)) {
                    mainBean.setAfter_remark("增加替代料");
                } else if (ChangeAction.Delete.equals(action)) {
                    mainBean.setAfter_remark("删除替代料");
                }
                tempList.add(mainBean);
                subs.forEach(e -> {
                    MntChangeListBean subBean = new MntChangeListBean(e, true, action);
                    subBean.setBefore_bomItem("");
                    tempList.add(subBean);
                });
            }
        }
        // if (tempList.size() > 1)
        // {
        // tempList.get(0).setMergedRegionInfos(getMergeRegions(tempList.size() - 1));
        // }
        mntChangeList.addAll(tempList);
    }

    private void addChangeMainSource(List<EBOMLineBean> list, ChangeAction action) {
        List<MntChangeListBean> tempList = new ArrayList<MntChangeListBean>();
        for (int i = 0; i < list.size(); i++) {
            EBOMLineBean ebomBean = list.get(i);
            MntChangeListBean changeListBean = new MntChangeListBean(ebomBean, false, action);
            tempList.add(changeListBean);
            // 2nd start;
            List<EBOMLineBean> seconds = ebomBean.getSecondSource();
            if (seconds != null && seconds.size() > 0) {
                changeListBean.setMergedRegionInfos(getMergeRegions(seconds.size()));
                for (EBOMLineBean subBean : seconds) {
                    MntChangeListBean subChange = new MntChangeListBean(subBean, true, action);
                    tempList.add(subChange);
                }
            }
            // 2nd end;
        }
        // if (tempList.size() > 1)
        // {
        // tempList.get(0).setMergedRegionInfos(getMergeRegions(tempList.size() - 1));
        // }
        mntChangeList.addAll(tempList);
    }

    private List<MergedRegionInfo> getMergeRegions(int size) {
        List<MergedRegionInfo> mList = new ArrayList<MergedRegionInfo>();
        mList.add(new MergedRegionInfo("parentPn", size));
        mList.add(new MergedRegionInfo("after_remark", size));
        return mList;
    }
}
