package com.foxconn.plm.tcservice.mapper.master;

import com.foxconn.plm.tcservice.tcfr.BUBean;
import com.foxconn.plm.tcservice.tcfr.TCFRReportBean;
import com.foxconn.plm.tcservice.tcfr.TCFRReportDetailBean;
import com.foxconn.plm.tcservice.tcfr.TFCRReportRp;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface TCFRReportMapper {


    List<TCFRReportBean> getList(TFCRReportRp rp);
    List<BUBean> getBU();
    List<BUBean> getCustomer(String buid);
    List<BUBean> getProductLine(String custId);
    List<TCFRReportDetailBean> getDetailList(TFCRReportRp rp);


}
