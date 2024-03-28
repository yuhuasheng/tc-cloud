package com.foxconn.plm.tcservice.ebom.service.impl;

import cn.hutool.json.JSONUtil;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcapi.soa.client.AppXSession;
import com.foxconn.plm.tcservice.ebom.domain.EBOMLineBean;
import com.foxconn.plm.tcservice.ebom.domain.EBOMUpdateBean;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.core.SessionService;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.ItemRevision;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class CompareBOM {
    private EBOMLineBean newRootBOMBean;
    private EBOMLineBean oldRootBOMBean;


    public CompareBOM(EBOMLineBean newRootBOMBean, EBOMLineBean oldRootBOMBean) {
        this.newRootBOMBean = newRootBOMBean;
        this.oldRootBOMBean = oldRootBOMBean;
    }

    public List<EBOMLineBean> getAdd() {
        List<EBOMLineBean> newChilds = newRootBOMBean.getChilds();
        List<EBOMLineBean> oldChilds = oldRootBOMBean.getChilds();
        List<EBOMLineBean> addList = new ArrayList<EBOMLineBean>(newChilds);
        addList.removeAll(oldChilds);
        return addList;
    }

    public List<EBOMLineBean> getDel() {
        List<EBOMLineBean> newChilds = newRootBOMBean.getChilds();
        List<EBOMLineBean> oldChilds = oldRootBOMBean.getChilds();
        List<EBOMLineBean> delList = new ArrayList<EBOMLineBean>(oldChilds);
        delList.removeAll(newChilds);
        return delList;
    }

    public List<EBOMUpdateBean> getLocationChange() {
        List<EBOMLineBean> newChilds = newRootBOMBean.getChilds();
        List<EBOMLineBean> oldChilds = oldRootBOMBean.getChilds();
        return newChilds.parallelStream()
                .map(e -> getChange(e, oldChilds, new String[]{"item"}))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<EBOMUpdateBean> getChangeQty() {
        List<EBOMLineBean> newChilds = newRootBOMBean.getChilds();
        List<EBOMLineBean> oldChilds = oldRootBOMBean.getChilds();
        return newChilds.parallelStream()
                .map(e -> getChange(e, oldChilds, new String[]{"qty"}))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<EBOMUpdateBean> getSameBom() {
        List<EBOMLineBean> newChilds = newRootBOMBean.getChilds();
        List<EBOMLineBean> oldChilds = oldRootBOMBean.getChilds();
        return newChilds.parallelStream().filter(oldChilds::contains).map(e -> {
            EBOMLineBean oldBean = oldChilds.get(oldChilds.indexOf(e));
            return new EBOMUpdateBean(e, oldBean);
        }).collect(Collectors.toList());
    }


    public List<EBOMUpdateBean> getChange(String[] changeField) {
        List<EBOMLineBean> newChilds = newRootBOMBean.getChilds();
        List<EBOMLineBean> oldChilds = oldRootBOMBean.getChilds();
        return newChilds.parallelStream()
                .map(e -> getChange(e, oldChilds, changeField))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<EBOMUpdateBean> getChange2nd() {
        List<EBOMLineBean> newChilds = newRootBOMBean.getChilds();
        List<EBOMLineBean> oldChilds = oldRootBOMBean.getChilds();
        return newChilds.parallelStream().map(e -> getChange(e, oldChilds, null)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private EBOMUpdateBean getChange(EBOMLineBean nBOMBean, List<EBOMLineBean> bomBeans, String[] changeField) {
        for (EBOMLineBean oBOMBean : bomBeans) {
            if (oBOMBean.equals(nBOMBean)) {
                try {
                    EBOMUpdateBean changeBean = new EBOMUpdateBean(nBOMBean, oBOMBean);
                    if (changeField != null) {
                        changeBean.change(changeField);
                    }
                    return changeBean;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return null;
    }


}
