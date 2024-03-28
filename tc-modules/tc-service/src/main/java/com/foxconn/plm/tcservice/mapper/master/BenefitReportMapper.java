package com.foxconn.plm.tcservice.mapper.master;

import com.foxconn.plm.tcservice.benefitreport.domain.ActionLogBean;
import com.foxconn.plm.tcservice.benefitreport.domain.RowDataBean;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @Author HuashengYu
 * @Date 2022/7/13 9:06
 * @Version 1.0
 */
@Mapper
public interface BenefitReportMapper {

    List<Map> getTCProject(@Param("projectName") String projectName); // 模糊查询专案

    List<ActionLogBean> getActionLog(@Param("cusDate") String cusDate, @Param("bu") String bu);

    List<ActionLogBean> getActionLogByBUAndPhase(@Param("phase") String phase, @Param("bu") String bu, @Param("functionName") String functionName, @Param("projectId") String projectId);

    List<ActionLogBean> getActionLogForSingle(@Param("projectId") String projectId, @Param("bu") String bu);

    List<RowDataBean> getRowData(@Param("bu") String bu, @Param("startDate") String startDate, @Param("projectId") String projectId);

    List<RowDataBean> getRowDataByBUAndPhase(@Param("bu") String bu, @Param("phase") String phase, @Param("functionName") String functionName, @Param("projectId") String projectId);

    List<RowDataBean> getCisRowData(@Param("bu") String bu, @Param("startDate") String startDate);
}
