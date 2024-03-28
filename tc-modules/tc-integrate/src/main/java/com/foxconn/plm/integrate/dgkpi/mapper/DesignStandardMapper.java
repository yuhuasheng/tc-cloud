package com.foxconn.plm.integrate.dgkpi.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DesignStandardMapper {


    public abstract String getSubType(@Param("uid") String uid);


}
