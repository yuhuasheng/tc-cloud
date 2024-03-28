package com.foxconn.plm.tcreport.mapper;


import com.foxconn.plm.tcreport.reportsearchparams.domain.LovBean;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @Author HuashengYu
 * @Date 2023/1/3 16:49
 * @Version 1.0
 */
@Mapper
public interface ReportSearchMapper {

    List<LovBean> getLov();

    List<LovBean> getLovByParam(@Param("bu") String bu, @Param("customer") String customer, @Param("productLine") String productLine,
                                @Param("projectSeries") String projectSeries, @Param("projectName") String projectName, @Param("projectId") String projectId);

    String getChassisByProjectId(@Param("projectId")String projectId);

    String getMonitorChassisByProjectId(@Param("projectId")String projectId,@Param("line")String line,@Param("attribute")String attribute);

    List<Map<String, Object>> getPhaseByProjectId(@Param("projectId")String projectId);

    List<Map<String, Object>> getPhaseByProjectIdAndAttribute(@Param("projectId")String projectId);

    String getProjectInfo(@Param("projectId")String projectId);
}
