package com.foxconn.dp.plm.hdfs.dao.xplm;


import com.foxconn.dp.plm.hdfs.domain.rv.FunctionConfigRv;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FunctionOrganizationMapper {

    List<FunctionConfigRv> getConfigList(FunctionConfigRv functionConfigRv);

    List<FunctionConfigRv> getFunctionList();
    List<FunctionConfigRv> getTCFunctionList();

    List<FunctionConfigRv> getGroupList(@Param("functionId") Integer functionId );
    List<FunctionConfigRv> getTCGroupList(@Param("functionName") String functionName );

    int modify(FunctionConfigRv functionConfigRv);

    int insert(FunctionConfigRv functionConfigRv);

    int delete(FunctionConfigRv functionConfigRv);



}
