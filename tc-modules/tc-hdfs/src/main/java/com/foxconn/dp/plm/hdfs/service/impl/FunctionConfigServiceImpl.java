package com.foxconn.dp.plm.hdfs.service.impl;

import com.foxconn.dp.plm.hdfs.dao.xplm.BUMapper;
import com.foxconn.dp.plm.hdfs.dao.xplm.FunctionConfigMapper;
import com.foxconn.dp.plm.hdfs.domain.rv.FunctionConfigRv;
import com.foxconn.dp.plm.hdfs.service.FunctionConfigService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FunctionConfigServiceImpl implements FunctionConfigService {

    @Resource
    FunctionConfigMapper functionConfigMapper;


    @Override
    public List<FunctionConfigRv> getConfigList(FunctionConfigRv functionConfigRv) {
        return functionConfigMapper.getConfigList(functionConfigRv);
    }

    @Override
    public Map<String,List<FunctionConfigRv>> getFunctionList() {
        List<FunctionConfigRv> functionList = functionConfigMapper.getFunctionList();
        List<FunctionConfigRv> tcFunctionList = functionConfigMapper.getTCFunctionList();
        Map<String,List<FunctionConfigRv>> map = new HashMap<>();
        map.put("functionList",functionList);
        map.put("tcFunctionList",tcFunctionList);
        return map;
    }

    @Override
    public List<FunctionConfigRv> getGroupList(Integer functionId) {
        return functionConfigMapper.getGroupList(functionId);
    }

    @Override
    public List<FunctionConfigRv> getTCGroupList(String functionId) {
        return functionConfigMapper.getTCGroupList(functionId);
    }

    @Override
    public void modify(FunctionConfigRv functionConfigRv) {
         functionConfigMapper.modify(functionConfigRv);
    }

    @Override
    public void insert(FunctionConfigRv functionConfigRv) {
         functionConfigMapper.insert(functionConfigRv);
    }

    @Override
    public void delete(FunctionConfigRv functionConfigRv) {
         functionConfigMapper.delete(functionConfigRv);
    }
}
