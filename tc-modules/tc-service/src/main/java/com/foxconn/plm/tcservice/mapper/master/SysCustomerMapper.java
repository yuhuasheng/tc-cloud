package com.foxconn.plm.tcservice.mapper.master;

import com.foxconn.plm.tcservice.issuemanagement.entity.SysCustomer;
import org.apache.ibatis.annotations.Mapper;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2023/12/1 10:54
 **/
@Mapper
public interface SysCustomerMapper {
    Long getId();
    Boolean insertEntity(SysCustomer bean);
    Boolean updateById(SysCustomer bean);
}
