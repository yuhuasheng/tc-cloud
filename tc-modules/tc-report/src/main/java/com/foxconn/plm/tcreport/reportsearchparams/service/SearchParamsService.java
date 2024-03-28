package com.foxconn.plm.tcreport.reportsearchparams.service;

import com.foxconn.plm.tcreport.reportsearchparams.domain.LinkLovBean;

import java.util.List;

/**
 * @Author HuashengYu
 * @Date 2023/1/3 17:26
 * @Version 1.0
 */
public interface SearchParamsService {

    List<LinkLovBean> getLovList();
}
