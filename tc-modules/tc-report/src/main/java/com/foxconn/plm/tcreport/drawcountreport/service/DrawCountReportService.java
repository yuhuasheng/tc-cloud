package com.foxconn.plm.tcreport.drawcountreport.service;

import com.foxconn.plm.tcreport.drawcountreport.domain.DrawCountRes;
import com.foxconn.plm.tcreport.drawcountreport.domain.QueryBean;
import com.foxconn.plm.tcreport.reportsearchparams.domain.LovBean;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * @Author HuashengYu
 * @Date 2023/1/3 14:23
 * @Version 1.0
 */
public interface DrawCountReportService {

    List<DrawCountRes> getDrawCountRecordList(QueryBean queryBean);

    void exportDrawCountRecordList(HttpServletResponse response, QueryBean queryBean);

}
