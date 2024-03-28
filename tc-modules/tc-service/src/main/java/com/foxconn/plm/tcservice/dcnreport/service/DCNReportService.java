package com.foxconn.plm.tcservice.dcnreport.service;

import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.tcservice.dcnreport.domain.*;
import org.apache.ibatis.annotations.Param;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * @Author HuashengYu
 * @Date 2022/10/21 14:22
 * @Version 1.0
 */
public interface DCNReportService {

    void saveDCNReportData(List<DCNReportBean> list);

    JSONObject getLovList();

    List<LinkLovEntity> getLinkageLovList();

    List<LinkFeeLovEntity> getFeeLinkageLovList();

    JSONObject getFeeLovList();

    List<DCNReportBean> getDCNRecordList(QueryEntity queryEntity) throws Exception;

    List<DCNFeeBean> getDCNFeeList(String projectId, String owner);

    List<DCNTotalBean> getDCNFeePerByProject(String projectId, String owner);

    ByteArrayOutputStream export(QueryEntity queryEntity);
}
