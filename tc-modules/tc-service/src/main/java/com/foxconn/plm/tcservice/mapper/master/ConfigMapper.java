package com.foxconn.plm.tcservice.mapper.master;


import com.foxconn.plm.tcservice.entity.LOVEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ConfigMapper {

    List<LOVEntity> getLOV(String name);

    List<LOVEntity> getSubLOV(String name);

    String getBuByCustomerAndProductLine(String s);

    List<String> getAllDept();

}
