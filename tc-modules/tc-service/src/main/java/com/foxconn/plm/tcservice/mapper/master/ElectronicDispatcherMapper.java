package com.foxconn.plm.tcservice.mapper.master;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @Author HuashengYu
 * @Date 2022/8/2 9:12
 * @Version 1.0
 */
public interface ElectronicDispatcherMapper {

    List<Map> getElectDispatcherList(@Param("serverNameList") List serverNameList, @Param("stateList") List stateList);
}
