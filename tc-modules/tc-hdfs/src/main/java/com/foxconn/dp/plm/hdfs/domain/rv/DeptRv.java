package com.foxconn.dp.plm.hdfs.domain.rv;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@ApiModel
@AllArgsConstructor
public class DeptRv {
    String dept;
    String hasTCAccount;
}
