package com.foxconn.plm.tcservice.mapper.infodba;

import com.foxconn.plm.tcservice.projectReport.*;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProjectReportMapper {

    List<ReportEntity> summary(QueryEntity query);

    List<WorkingDataEntity> workingData();

    List<ArchiveDataEntity> archiveData();

    int testsql();

    List<LovEntity> getLov();

    List<String> getFunction();


}
