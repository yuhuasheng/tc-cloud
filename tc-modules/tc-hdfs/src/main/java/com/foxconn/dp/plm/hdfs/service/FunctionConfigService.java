package com.foxconn.dp.plm.hdfs.service;


import com.foxconn.dp.plm.hdfs.domain.rp.SaveBuRp;
import com.foxconn.dp.plm.hdfs.domain.rv.FunctionConfigRv;
import com.foxconn.dp.plm.hdfs.domain.rv.LOVRv;
import com.foxconn.dp.plm.hdfs.domain.rv.ProductLineRv;
import com.foxconn.plm.entity.param.BUListRp;
import com.foxconn.plm.entity.response.BURv;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface FunctionConfigService {

    List<FunctionConfigRv> getConfigList(FunctionConfigRv functionConfigRv);

    Map<String,List<FunctionConfigRv>> getFunctionList();

    List<FunctionConfigRv> getGroupList(@Param("functionId") Integer functionId );
    List<FunctionConfigRv> getTCGroupList(@Param("functionName") String functionId );

    void modify(FunctionConfigRv functionConfigRv);

    void insert(FunctionConfigRv functionConfigRv);

    void delete(FunctionConfigRv functionConfigRv);

}
