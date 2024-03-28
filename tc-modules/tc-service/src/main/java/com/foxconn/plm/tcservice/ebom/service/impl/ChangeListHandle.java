package com.foxconn.plm.tcservice.ebom.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcservice.ebom.domain.EBOMLineBean;
import com.foxconn.plm.tcservice.ebom.domain.EBOMUpdateBean;
import com.foxconn.plm.utils.string.StringUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.rac.kernel.TCComponentBOMLine;
import com.teamcenter.rac.kernel.TCComponentBOMWindow;
import com.teamcenter.rac.kernel.TCComponentItemRevision;
import com.teamcenter.rac.kernel.TCException;
import com.teamcenter.services.strong.cad.StructureManagementService;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core.SessionService;
import com.teamcenter.soa.client.model.strong.ItemRevision;

public class ChangeListHandle {
    protected EBOMLineBean sourceBomBean;
    protected EBOMLineBean targetBomBean;
    private List<EBOMLineBean> adds = new ArrayList<EBOMLineBean>();
    private List<EBOMLineBean> dels = new ArrayList<EBOMLineBean>();
    private List<EBOMUpdateBean> changes = new ArrayList<EBOMUpdateBean>();

    public ChangeListHandle(ItemRevision sourceItemRev, ItemRevision targetItemRev, TCSOAServiceFactory tcsoaServiceFactory) throws Exception {

        sourceBomBean = getTopBOMLine(tcsoaServiceFactory, sourceItemRev);
        targetBomBean = getTopBOMLine(tcsoaServiceFactory, targetItemRev);
        if (targetBomBean == null || sourceBomBean == null) {
            throw new RuntimeException("error data !!");
        }
        convertUid(sourceBomBean.getChilds());
        convertUid(targetBomBean.getChilds());
    }

    public List<EBOMLineBean> getAdd() {
        return adds;
    }

    public List<EBOMLineBean> getDel() {
        return dels;
    }

    public List<EBOMUpdateBean> getChange() {
        return changes;
    }

    public void compareBOM() {
        compareBOM(sourceBomBean, targetBomBean);
    }

    private void compareBOM(EBOMLineBean sourceBomBean, EBOMLineBean targetBomBean) {
        CompareBOM result = new CompareBOM(targetBomBean, sourceBomBean);
        adds.addAll(result.getAdd());
        dels.addAll(result.getDel());
        List<EBOMUpdateBean> beans = result.getChangeQty();
        List<EBOMUpdateBean> changeBeans = beans.stream().filter(e -> e.getChangeFiledNames().size() > 0).collect(Collectors.toList());
        changes.addAll(changeBeans);
        for (EBOMUpdateBean changeBean : beans) {
            compareBOM(changeBean.getOldEBomBean(), changeBean.getNewEBomBean());
        }
    }

    public static EBOMLineBean getTopBOMLine(TCSOAServiceFactory tcsoaServiceFactory, ItemRevision itemRevision
    ) throws TCException {
        return new EBOMServiceImpl().getBOMStruct(tcsoaServiceFactory, itemRevision);
    }


    public String getExcelFileName() throws TCException {
        LocalDateTime localTime = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String fileName = sourceBomBean.getItem() + "_changeList_" + dtf.format(localTime) + "_.xlsx";
        return fileName;
    }

    private void convertUid(List<EBOMLineBean> beans) {
        if (beans != null) {
            beans.forEach(bean -> {
                bean.setUid(bean.getBomId());
                if (StringUtil.isNotEmpty(bean.getReferenceDimension())) {
                    bean.setUid(bean.getBomId() + bean.getLocation());
                }
                convertUid(bean.getSecondSource());
                convertUid(bean.getChilds());
            });
        }
    }
}
