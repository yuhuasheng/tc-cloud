package com.foxconn.plm.tcreport.reportsearchparams.service.impl;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.tcreport.mapper.ReportSearchMapper;
import com.foxconn.plm.tcreport.reportsearchparams.domain.LinkLovBean;
import com.foxconn.plm.tcreport.reportsearchparams.domain.LovBean;
import com.foxconn.plm.tcreport.reportsearchparams.service.SearchParamsService;
import com.foxconn.plm.utils.collect.CollectUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author HuashengYu
 * @Date 2023/1/3 17:27
 * @Version 1.0
 */
@Service
public class SearchParamsServiceImpl implements SearchParamsService {
    private static Log log = LogFactory.get();
    @Resource
    private ReportSearchMapper reportSearchMapper;

    @Override
    public List<LinkLovBean> getLovList() {
        List<LovBean> lov = reportSearchMapper.getLov();
        Collections.sort(lov);
        if (CollectUtil.isEmpty(lov)) {
            return new ArrayList<>();
        }

        lov.removeIf(bean -> "N/A".equals(bean.getBu()));
        return groupByLov(lov);
    }

    private List<LinkLovBean> groupByLov(List<LovBean> list) {
        List<LinkLovBean> resultList = new ArrayList<>();
        Map<String, List<LovBean>> buGroup = list.stream().collect(Collectors.groupingBy(bean -> bean.getBu()));
        buGroup.forEach((k1, v1) -> {
            LinkLovBean rootBean = new LinkLovBean();
            rootBean.setValue(k1);
            Map<String, List<LovBean>> customerGroup = v1.stream().collect(Collectors.groupingBy(bean -> bean.getCustomer()));
            customerGroup.forEach((k2, v2) -> {
                if("Lenovo L5".equals(k2)){
                    return;
                }
                LinkLovBean customerBean = new LinkLovBean();
                customerBean.setValue(k2);
                rootBean.addChild(customerBean);
                Map<String, List<LovBean>> productLineGroup = v2.stream().collect(Collectors.groupingBy(bean -> bean.getProductLine()));
                productLineGroup.forEach((k3, v3) -> {
                    LinkLovBean productLineBean = new LinkLovBean();
                    productLineBean.setValue(k3);
                    customerBean.addChild(productLineBean);
                    Map<String, List<LovBean>> projectSeriesGroup = v3.stream().collect(Collectors.groupingBy(bean -> bean.getProjectSeries()));
                    projectSeriesGroup.forEach((k4, v4) -> {
                        LinkLovBean projectSeriesBean = new LinkLovBean();
                        projectSeriesBean.setValue(k4);
                        productLineBean.addChild(projectSeriesBean);
                        Map<String, List<LovBean>> projectInfoGroup = v4.stream().collect(Collectors.groupingBy(bean -> bean.getProjectInfo()));
                        projectInfoGroup.forEach((k5, v5) -> {
                            LinkLovBean projectInfoBean = new LinkLovBean();
                            projectInfoBean.setValue(k5);
                            projectSeriesBean.addChild(projectInfoBean);
                        });
                    });
                });
            });
            resultList.add(rootBean);
        });
        return resultList;
    }
}
