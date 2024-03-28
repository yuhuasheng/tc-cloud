package com.foxconn.plm.tcservice.mapper.master;

import com.foxconn.plm.tcservice.ftebenefitreport.domain.FTERecordInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * @Author HuashengYu
 * @Date 2022/9/22 9:19
 * @Version 1.0
 */
@Mapper
public interface FTEBenefitReportMapper {

    void insertOrUpdateFTERecord(@Param("list") List<FTERecordInfo> fteRecordInfoList);

    List<FTERecordInfo> getFTEBenefitRecordn(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

}
