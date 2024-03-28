package com.foxconn.plm.tcservice.tclicensereport.service.impl;

import com.foxconn.dp.plm.privately.Access;
import com.foxconn.plm.tcservice.mapper.master.FunctionMapper;
import com.foxconn.plm.tcservice.tclicensereport.domain.FunctionInfo;
import com.foxconn.plm.tcservice.tclicensereport.service.FunctionService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class FunctionServiceImpl implements FunctionService {

    @Resource
    private FunctionMapper functionMapper;

    @Override
    public List<FunctionInfo> getFunctionInfo() {
        return functionMapper.getFunctionInfo();
    }

    @Override
    public void setFunctionInfo(List<FunctionInfo> functionInfoList) {
        functionMapper.setFunctionInfo(Access.check(functionInfoList));
    }

}
