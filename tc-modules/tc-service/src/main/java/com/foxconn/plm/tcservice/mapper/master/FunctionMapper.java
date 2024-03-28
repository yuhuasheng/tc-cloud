package com.foxconn.plm.tcservice.mapper.master;

import com.foxconn.plm.tcservice.tclicensereport.domain.DateRecordInfo;
import com.foxconn.plm.tcservice.tclicensereport.domain.FunctionInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface FunctionMapper {

    List<FunctionInfo> getFunctionInfo();

    void setFunctionInfo(List<FunctionInfo> functionInfoList);

    void insertOrUpdateDateRecord(@Param("list") List<DateRecordInfo> dateRecordInfoList);
}
