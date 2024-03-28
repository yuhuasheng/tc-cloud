package com.foxconn.plm.tcservice.mapper.master;

import com.foxconn.plm.tcservice.dcnreport.domain.*;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Author HuashengYu
 * @Date 2022/10/21 14:27
 * @Version 1.0
 */
public interface DCNReportMapper {

    void insertDCNData(@Param("list") List<DCNReportBean> list);

    List<LovEntity> getLov();

    List<FeeLovEntity> getFeeLov();

    List<DCNReportBean> getDCNRecord(@Param("queryEntity") QueryEntity queryEntity);

    List<DCNReportBean> getDCNRecordByType(@Param("queryEntity") QueryEntity queryEntity, @Param("modelNoPrefix") List<String> modelNoPrefix);

    List<DCNCreateBean> getDCNCreateRecord(@Param("objectType") String objectType, @Param("startDate") Date startDate, @Param("endDate") Date endDate);

    List<DCNFeeBean> getDCNFeeRecord(@Param("projectId") String projectId, @Param("owner") String owner);

    List<DCNFeeBean> getNewMoldFeeRecord(@Param("owner") String owner);

    List<DCNFeeBean> getNewMoldFee(@Param("itemId") String itemId);
}
