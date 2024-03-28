package com.foxconn.dp.plm.hdfs.service.impl;

import com.foxconn.dp.plm.hdfs.dao.xplm.FunctionConfigMapper;
import com.foxconn.dp.plm.hdfs.dao.xplm.FunctionOrganizationMapper;
import com.foxconn.dp.plm.hdfs.domain.rv.FunctionConfigRv;
import com.foxconn.dp.plm.hdfs.service.FunctionConfigService;
import com.foxconn.dp.plm.hdfs.service.FunctionOrganizationService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FunctionOrganizationServiceImpl implements FunctionOrganizationService {

    @Resource
    FunctionOrganizationMapper functionOrganizationMapper;


    @Override
    public List<FunctionConfigRv> getConfigList(FunctionConfigRv functionConfigRv) {
        return functionOrganizationMapper.getConfigList(functionConfigRv);
    }

    @Override
    public Map<String,List<FunctionConfigRv>> getFunctionList() {
        List<FunctionConfigRv> functionList = functionOrganizationMapper.getFunctionList();
        List<FunctionConfigRv> tcFunctionList = functionOrganizationMapper.getTCFunctionList();
        Map<String,List<FunctionConfigRv>> map = new HashMap<>();
        map.put("functionList",functionList);
        map.put("tcFunctionList",tcFunctionList);
        return map;
    }

    @Override
    public List<FunctionConfigRv> getGroupList(Integer functionId) {
        return functionOrganizationMapper.getGroupList(functionId);
    }

    @Override
    public List<FunctionConfigRv> getTCGroupList(String functionId) {
        return functionOrganizationMapper.getTCGroupList(functionId);
    }

    @Override
    public void modify(FunctionConfigRv functionConfigRv) {
        functionOrganizationMapper.modify(functionConfigRv);
    }

    @Override
    public void insert(FunctionConfigRv functionConfigRv) {
        functionOrganizationMapper.insert(functionConfigRv);
    }

    @Override
    public void delete(FunctionConfigRv functionConfigRv) {
        functionOrganizationMapper.delete(functionConfigRv);
    }
}
