package com.foxconn.dp.plm.hdfs.domain.rv;

import io.swagger.models.auth.In;
import lombok.Data;

@Data
public class FunctionConfigRv {
    private Integer id;
    private  Integer functionId;
    private String functionName;
    private Integer groupId;
    private String groupName;
    private String tcFunctionName;
    private String tcGroupName;
    private String userName;
}
