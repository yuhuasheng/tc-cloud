package com.foxconn.dp.plm.hdfs.dao.xplm;


import com.foxconn.dp.plm.hdfs.domain.entity.LOVEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ConfigMapper {

    List<LOVEntity> getLOV(String name);

    List<LOVEntity> getSubLOV(String name);

    String getBuByCustomerAndProductLine(String s);

    List<String> getAllDept();

}
