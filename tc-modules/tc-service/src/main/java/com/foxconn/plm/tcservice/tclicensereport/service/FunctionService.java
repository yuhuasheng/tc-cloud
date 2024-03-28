package com.foxconn.plm.tcservice.tclicensereport.service;

import com.foxconn.plm.tcservice.tclicensereport.domain.FunctionInfo;

import java.util.List;

public interface FunctionService {

    List<FunctionInfo> getFunctionInfo();

    void setFunctionInfo(List<FunctionInfo> functionInfoList);
}
