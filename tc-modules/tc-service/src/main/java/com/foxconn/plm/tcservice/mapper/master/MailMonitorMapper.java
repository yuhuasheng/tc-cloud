package com.foxconn.plm.tcservice.mapper.master;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * @Author HuashengYu
 * @Date 2022/7/21 11:00
 * @Version 1.0
 */
@Mapper
public interface MailMonitorMapper {

    List<Map> getActiveEmail();
}
