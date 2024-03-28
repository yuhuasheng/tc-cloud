package com.foxconn.plm.tcservice.ebom.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcservice.ebom.constant.ChangeAction;
import com.foxconn.plm.tcservice.ebom.constant.MntDCNChangeField;
import com.foxconn.plm.tcservice.ebom.domain.EBOMLineBean;
import com.foxconn.plm.tcservice.ebom.domain.EBOMUpdateBean;
import com.foxconn.plm.tcservice.ebom.domain.MergedRegionInfo;
import com.foxconn.plm.tcservice.ebom.domain.MntDCNChangeBean;
import com.foxconn.plm.tcservice.ebom.util.EBOMExcelUtil;
import com.foxconn.plm.utils.excel.ExcelUtil;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import org.apache.commons.collections4.ListUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.util.ReflectionUtils;


/**
 * @author Robert
 */
public class MntDerivativeCompare {
    public final static int START_ROW = 6;
    public final static String START_FW_PN = "629";
    public final static String TEMPLATE = "templates/MNT_DCN_Change_List.xlsx";
    private static final String[] MERGED_FIELD = new String[]{"bomItem", "code", "location", "rev", "before_qty", "after_qty", "unit", "remark"};
    private EBOMLineBean dynamicBOMBean;
    private List<String> newLocation;
    private List<String> oldLocation;
    private EBOMUpdateBean changeBean;
    private List<MntDCNChangeBean> extremeChange;


    public static void exportMntDerivativeChangeList(File changeListFile, ItemRevision previousRevision, ItemRevision solutionItemRev,
                                                     TCSOAServiceFactory tcsoaServiceFactory) throws Exception {

        MntChangeHandle change = new MntChangeHandle(previousRevision, solutionItemRev, tcsoaServiceFactory);
        change.compareDCNBOM(null, null, false);
        List<MntDCNChangeBean> allChangeList = change.getMntDCNChangeList();
        EBOMExcelUtil util = new EBOMExcelUtil();
        Workbook wb = util.getWorkbook(MntDerivativeCompare.TEMPLATE);
        if (allChangeList.size() > 0) {
            Sheet sheet = wb.getSheetAt(0);
            sheet.getRow(3).getCell(0).setCellValue("Model:");
            sheet.getRow(3).getCell(9).setCellValue("DCN/ECN NO.:");
            for (int i = 0; i < allChangeList.size(); i++) {
                int rowLine = MntDerivativeCompare.START_ROW + i;
                MntDCNChangeBean pl = allChangeList.get(i);
                pl.setNo((i + 1) + "");
                List<MergedRegionInfo> mergedList = pl.getMergedRegionInfos();
                if (mergedList != null) {
                    for (MergedRegionInfo meInfo : mergedList) {
                        util.setMerged(meInfo, MntDCNChangeBean.class, sheet, rowLine);
                    }
                }
            }
            util.setRichCellValue(allChangeList, MntDerivativeCompare.START_ROW, sheet, ExcelUtil.getCellStyle(wb));
        }
        OutputStream out = new FileOutputStream(changeListFile);
        wb.write(out);
        wb.close();
        out.close();
    }


    public MntDerivativeCompare(EBOMUpdateBean changeBean) throws Exception {
        this.changeBean = changeBean;
        this.dynamicBOMBean = changeBean.getOldEBomBean().clone();
        this.newLocation = asList(changeBean.getNewEBomBean().getLocation(), ",");
        this.oldLocation = asList(changeBean.getOldEBomBean().getLocation(), ",");
    }

    public MntDerivativeCompare(List<EBOMLineBean> ebomLines, ChangeAction action) throws Exception {
        this.extremeChange = new ArrayList<MntDCNChangeBean>();
        for (EBOMLineBean bomBean : ebomLines) {
            extremeChange.addAll(extremeChange(bomBean, action));
        }
    }

    public List<MntDCNChangeBean> getExtremeChange() {
        return extremeChange;
    }

    private List<MntDCNChangeBean> extremeChange(EBOMLineBean bomLineBean, ChangeAction action) {
        List<MntDCNChangeBean> extremeChangeList = new ArrayList<MntDCNChangeBean>();
        MntDCNChangeBean mainSourceChangeBean = new MntDCNChangeBean(bomLineBean);
        mainSourceChangeBean.setCode(action.value());
        extremeChangeList.add(mainSourceChangeBean);
        String remark = "";
        List<String> mfgList = new ArrayList<String>();
        mfgList.add(bomLineBean.getMfg());
        List<EBOMLineBean> subs = bomLineBean.getSecondSource();
        if (subs == null) {
            subs = new ArrayList<EBOMLineBean>();
        }
        List<MntDCNChangeBean> change2nd = new ArrayList<MntDCNChangeBean>();
        for (EBOMLineBean sub : subs) {
            MntDCNChangeBean subChangeBean = new MntDCNChangeBean(sub);
            change2nd.add(subChangeBean);
        }
        switch (action) {
            case Add:
                remark = "增加 " + bomLineBean.getLocation();
                mainSourceChangeBean.setBefore_qty("0");
                if (change2nd.size() > 0) {
                    try {
                        MntDCNChangeBean cloneMainSourceChangeBean = mainSourceChangeBean.clone();
                        // Stream<String> mfgStream = Stream.of(bomLineBean.getMfg());
                        // mfgStream = Stream.concat(mfgStream, subs.stream().map(EBOMLineBean::getMfg));
                        String changMfgs = subs.stream().map(EBOMLineBean::getMfg).collect(Collectors.joining(","));
                        cloneMainSourceChangeBean.setRemark("增加 " + changMfgs);
                        extremeChangeList.add(cloneMainSourceChangeBean);
                        for (MntDCNChangeBean add2ndBean : change2nd) {
                            add2ndBean.setSupplier(insertAddLabel(add2ndBean.getSupplier()));
                        }
                        extremeChangeList.addAll(change2nd);
                        setMergedInfos(bomLineBean, cloneMainSourceChangeBean, 0);
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                }
                mainSourceChangeBean.setLocation(insertAddLabel(mainSourceChangeBean.getLocation()));
                break;
            case Delete:
                remark = "删除 " + bomLineBean.getLocation();
                extremeChangeList.addAll(change2nd);
                for (MntDCNChangeBean bean : extremeChangeList) {
                    bean.setDel();
                }
                setMergedInfos(bomLineBean, mainSourceChangeBean, 0);
                break;
            default:
                break;
        }
        mainSourceChangeBean.setRemark(remark);
        return extremeChangeList;
    }

    public List<MntDCNChangeBean> getAllChange() throws Exception {
        List<MntDCNChangeBean> allChange = new ArrayList<MntDCNChangeBean>();
        allChange.addAll(getLocationChange(ChangeAction.Add));
        allChange.addAll(getLocationChange(ChangeAction.Delete));
        allChange.addAll(getDel2ndSourceChange());
        allChange.addAll(getAdd2ndSourceChange());
        allChange.addAll(getCommonChange(MntDCNChangeField.Des));
        allChange.addAll(getCommonChange(MntDCNChangeField.SUPPLIER));
        allChange.addAll(getPriChange(MntDCNChangeField.QTY));
        allChange.addAll(getPriChange(MntDCNChangeField.Rev));
        allChange.addAll(getPriChange(MntDCNChangeField.UNIT));
        return allChange;
    }

    public List<MntDCNChangeBean> getDel2ndSourceChange() {
        List<MntDCNChangeBean> sourceChangeList = new ArrayList<MntDCNChangeBean>();
        List<EBOMLineBean> delSubs = changeBean.getDel2nd();
        if (delSubs.size() > 0) {
            List<EBOMLineBean> subs = dynamicBOMBean.getSecondSource();
            MntDCNChangeBean mainSourceChangeBean = new MntDCNChangeBean(dynamicBOMBean);
            setMergedInfos(mainSourceChangeBean, 0);
            String addmfg = delSubs.stream().map(EBOMLineBean::getMfg).collect(Collectors.joining(","));
            mainSourceChangeBean.setCode(ChangeAction.Delete.value());
            mainSourceChangeBean.setRemark("删除 " + addmfg);
            sourceChangeList.add(mainSourceChangeBean);
            // 替代料处理
            for (EBOMLineBean sub : subs) {
                MntDCNChangeBean subChangeBean = new MntDCNChangeBean(sub);
                if (delSubs.contains(sub)) {
                    subChangeBean.setPartPn(insertDelLabel(subChangeBean.getPartPn()));
                    subChangeBean.setDes(insertDelLabel(subChangeBean.getDes()));
                    subChangeBean.setSupplier(insertDelLabel(subChangeBean.getSupplier()));
                }
                sourceChangeList.add(subChangeBean);
            }
            subs.removeAll(delSubs);
        }
        return sourceChangeList;
    }

    public List<MntDCNChangeBean> getAdd2ndSourceChange() {
        List<MntDCNChangeBean> sourceChangeList = new ArrayList<MntDCNChangeBean>();
        List<EBOMLineBean> addSubs = changeBean.getAdd2nd();
        if (addSubs.size() > 0) {
            List<EBOMLineBean> subs = dynamicBOMBean.getSecondSource();
            if (subs == null) {
                subs = new ArrayList<EBOMLineBean>();
                dynamicBOMBean.setSecondSource(subs);
            }
            subs.addAll(addSubs);
            MntDCNChangeBean mainSourceChangeBean = new MntDCNChangeBean(dynamicBOMBean);
            setMergedInfos(mainSourceChangeBean, 0);
            List<String> addmfgList = addSubs.stream().map(EBOMLineBean::getMfg).collect(Collectors.toList());
            String addmfg = addmfgList.stream().collect(Collectors.joining(","));
            mainSourceChangeBean.setRemark("增加 " + addmfg);
            mainSourceChangeBean.setCode(ChangeAction.Add.value());
            sourceChangeList.add(mainSourceChangeBean);
            fill2nd(sourceChangeList);
            for (MntDCNChangeBean bean : sourceChangeList) {
                if (addmfgList.contains(bean.getSupplier())) {
                    bean.setSupplier(insertAddLabel(bean.getSupplier()));
                }
            }
        }
        return sourceChangeList;
    }

    /**
     * Robert 2022年10月19日
     *
     * @param fieldEunm
     * @return
     * @throws Exception
     */
    public List<MntDCNChangeBean> getCommonChange(MntDCNChangeField fieldEunm) throws Exception {
        List<MntDCNChangeBean> commonChangeList = new ArrayList<MntDCNChangeBean>();
        String qtyField = fieldEunm.value();
        List<String> changeContent = new ArrayList<>();
        if (isChange(changeBean.getNewEBomBean(), changeBean.getOldEBomBean(), qtyField)) {
            updateBOMBean(changeBean.getNewEBomBean(), qtyField, false);
            changeContent.add(dynamicBOMBean.getMfg());
        }
        // 替代料处理
        List<EBOMLineBean> subs = dynamicBOMBean.getSecondSource();
        if (subs != null && subs.size() > 0) {
            for (EBOMLineBean sub : subs) {
                EBOMLineBean newSubBean = getEBOMLineBean(sub, changeBean.getNewEBomBean().getSecondSource());
                EBOMLineBean oldSubBean = getEBOMLineBean(sub, changeBean.getOldEBomBean().getSecondSource());
                if (oldSubBean != null && newSubBean != null && isChange(newSubBean, oldSubBean, qtyField)) {
                    updateBOMBean(sub, qtyField, true);
                    changeContent.add(sub.getMfg());
                }
            }
        }
        if (changeContent.size() > 0) {
            MntDCNChangeBean mianBean = new MntDCNChangeBean(dynamicBOMBean);
            mianBean.setCode(ChangeAction.Change.value());
            String changeStr = String.join(",", changeContent);
            if (fieldEunm.equals(MntDCNChangeField.Des)) {
                mianBean.setRemark("描述变更 " + changeStr);
            } else if (fieldEunm.equals(MntDCNChangeField.SUPPLIER)) {
                mianBean.setRemark("厂商变更 " + changeStr);
            }
            commonChangeList.add(mianBean);
            setMergedInfos(mianBean, 0);
            if (subs != null) {
                subs.stream().map(MntDCNChangeBean::new).forEach(commonChangeList::add);
            }
        }
        return commonChangeList;
    }

    /**
     * Robert 2022年10月18日
     *
     * @param fieldEunm
     * @return
     * @throws Exception
     */
    public List<MntDCNChangeBean> getPriChange(MntDCNChangeField fieldEunm) throws Exception {
        List<MntDCNChangeBean> commonChangeList = new ArrayList<MntDCNChangeBean>();
        String qtyField = fieldEunm.value();
        if (fieldEunm.equals(MntDCNChangeField.QTY) && !ListUtils.isEqualList(oldLocation, newLocation)) {
            return commonChangeList;
        }
        if (isChange(changeBean.getNewEBomBean(), changeBean.getOldEBomBean(), qtyField)) {
            updateBOMBean(changeBean.getNewEBomBean(), qtyField, false);
            MntDCNChangeBean mainSourceChangeBean = new MntDCNChangeBean(dynamicBOMBean);
            mainSourceChangeBean.setCode(ChangeAction.Change.value());
            switch (fieldEunm) {
                case QTY:
                    mainSourceChangeBean.setBefore_qty(changeBean.getOldEBomBean().getQty());
                    mainSourceChangeBean.setRemark("用量变更 ");
                    break;
                case Rev:
                    mainSourceChangeBean.setRemark("版本变更，变更前：" + changeBean.getOldEBomBean().getVersion());
                    break;
                case UNIT:
                    mainSourceChangeBean.setRemark("单位变更，变更前：" + changeBean.getOldEBomBean().getUnit());
                    break;
                default:
                    break;
            }
            setMergedInfos(mainSourceChangeBean, 0);
            commonChangeList.add(mainSourceChangeBean);
            fill2nd(commonChangeList);
        }
        return commonChangeList;
    }

    public List<MntDCNChangeBean> getLocationChange(ChangeAction action) throws Exception {
        List<MntDCNChangeBean> locationChangeList = new ArrayList<MntDCNChangeBean>();
        List<String> changeStrs = null;
        String remark = "";
        String str = "";
        switch (action) {
            case Add:
                changeStrs = getAddLocation();
                if (changeStrs.size() > 0) {
                    str = String.join(",", changeStrs);
                    remark = "增加 " + str;
                }
                break;
            case Delete:
                changeStrs = getDelLocation();
                if (changeStrs.size() > 0) {
                    str = String.join(",", changeStrs);
                    remark = "删除 " + str;
                    List<String> currentLocation = asList(dynamicBOMBean.getLocation(), ",");
                    currentLocation.removeAll(changeStrs);
                    dynamicBOMBean.setLocation(String.join(",", currentLocation));
                }
                break;
            default:
                break;
        }
        if (changeStrs != null && changeStrs.size() > 0) {
            dynamicBOMBean.setQty(changeBean.getNewEBomBean().getQty());
            MntDCNChangeBean mainSourceChangeBean = new MntDCNChangeBean(dynamicBOMBean);
            mainSourceChangeBean.setBefore_qty(changeBean.getOldEBomBean().getQty());
            mainSourceChangeBean.setRemark(remark);
            mainSourceChangeBean.setCode(action.value());
            if (ChangeAction.Delete.equals(action)) {
                mainSourceChangeBean.setLocation(mainSourceChangeBean.getLocation() + "," + insertDelLabel(str));
            } else {
                mainSourceChangeBean.setLocation(mainSourceChangeBean.getLocation() + "," + insertAddLabel(str));
                dynamicBOMBean.setLocation(dynamicBOMBean.getLocation() + "," + str);
            }
            setMergedInfos(mainSourceChangeBean, 0);
            locationChangeList.add(mainSourceChangeBean);
            fill2nd(locationChangeList);
        }
        return locationChangeList;
    }

    private List<String> getAddLocation() {
        List<String> addList = new ArrayList<String>(newLocation);
        addList.removeAll(oldLocation);
        return addList;
    }

    private List<String> getDelLocation() {
        List<String> delList = new ArrayList<String>(oldLocation);
        delList.removeAll(newLocation);
        return delList;
    }

    private void fill2nd(List<MntDCNChangeBean> commonChangeList) {
        List<EBOMLineBean> subs = dynamicBOMBean.getSecondSource();
        if (subs != null) {
            for (EBOMLineBean sub : subs) {
                MntDCNChangeBean subChangeBean = new MntDCNChangeBean(sub);
                commonChangeList.add(subChangeBean);
            }
        }
    }

    private List<String> asList(String str, String separator) {
        return new ArrayList<String>(Arrays.asList(str.split(separator)));
    }

    private void updateBOMBean(EBOMLineBean fromBomBean, String fieldName, boolean isSub) throws Exception {
        Field field = EBOMLineBean.class.getDeclaredField(fieldName);
        ReflectionUtils.makeAccessible(field);
        Object nOb = field.get(fromBomBean);
        if (isSub) {
            EBOMLineBean oldBean = getEBOMLineBean(fromBomBean, dynamicBOMBean.getSecondSource());
            field.set(oldBean, nOb);
        } else {
            field.set(dynamicBOMBean, nOb);
        }
    }

    private void setMergedInfos(EBOMLineBean bomLineBean, MntDCNChangeBean mainSourceChangeBean, int offset) {
        List<EBOMLineBean> subs = bomLineBean.getSecondSource();
        if (subs != null && subs.size() > 0) {
            int mergedLenth = subs.size() + offset;
            List<MergedRegionInfo> mergedRegionInfos = new ArrayList<>();
            for (String mergedField : MERGED_FIELD) {
                mergedRegionInfos.add(new MergedRegionInfo(mergedField, mergedLenth));
            }
            mainSourceChangeBean.setMergedRegionInfos(mergedRegionInfos);
        }
    }

    private void setMergedInfos(MntDCNChangeBean mainSourceChangeBean, int offset) {
        List<EBOMLineBean> subs = dynamicBOMBean.getSecondSource();
        if (subs != null) {
            int mergedLenth = subs.size() + offset;
            if (mergedLenth > 0) {
                List<MergedRegionInfo> mergedRegionInfos = new ArrayList<>();
                for (String mergedField : MERGED_FIELD) {
                    mergedRegionInfos.add(new MergedRegionInfo(mergedField, mergedLenth));
                }
                mainSourceChangeBean.setMergedRegionInfos(mergedRegionInfos);
            }
        }
    }

    private EBOMLineBean getEBOMLineBean(EBOMLineBean bomBean, List<EBOMLineBean> otherList) {
        if (otherList != null && otherList.size() > 0) {
            int index = otherList.indexOf(bomBean);
            if (index > 0) {
                return otherList.get(index);
            }
        }
        return null;
    }

    public static String insertDelLabel(String str) {
        return "<del>" + str + "</del>";
    }

    static String insertAddLabel(String str) {
        return "<add>" + str + "</add>";
    }

    private boolean isChange(EBOMLineBean newBomBean, EBOMLineBean oldBomBean, String changeField) throws Exception {
        Field field = EBOMLineBean.class.getDeclaredField(changeField);
        ReflectionUtils.makeAccessible(field);
        String nOb = (String) field.get(newBomBean);
        String oOb = (String) field.get(oldBomBean);
        if (MntDCNChangeField.QTY.value().equalsIgnoreCase(changeField)) {
            if (nOb == null || nOb.length() == 0) {
                nOb = "0";
            }
            if (oOb == null || oOb.length() == 0) {
                oOb = "0";
            }
            return new BigDecimal((String) nOb).compareTo(new BigDecimal((String) oOb)) != 0;
        }
        if (MntDCNChangeField.Rev.value().equalsIgnoreCase(changeField)) {
            if (!newBomBean.getItem().startsWith(START_FW_PN)) {
                return false;
            }
        }
        return !nOb.equals(oOb);
    }
}
