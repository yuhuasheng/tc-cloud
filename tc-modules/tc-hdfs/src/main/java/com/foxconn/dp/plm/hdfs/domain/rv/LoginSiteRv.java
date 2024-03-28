package com.foxconn.dp.plm.hdfs.domain.rv;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@ApiModel
public class LoginSiteRv {
    String siteCode;
    List<DeptRv> deptList = new ArrayList<>();
    String bu;
}
