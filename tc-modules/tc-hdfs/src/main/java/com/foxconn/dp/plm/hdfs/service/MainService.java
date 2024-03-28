package com.foxconn.dp.plm.hdfs.service;


import com.foxconn.dp.plm.hdfs.domain.entity.LOVEntity;

import java.util.List;
import java.util.Map;

public interface MainService {

    Map<String, List<LOVEntity>> getLOV(List<String> list);

    List<LOVEntity> getLOV(String name);

    List<String> getAllDept();

    String getLoginSite(String ip);

}
