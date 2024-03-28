package com.foxconn.plm.tcservice.mapper.master;

import com.foxconn.plm.tcservice.issuemanagement.entity.SysAccountLib;
import org.apache.ibatis.annotations.Mapper;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2023/12/1 10:57
 **/
@Mapper
public interface SysAccountLibMapper {
    Long getId();

    Boolean insertEntity(SysAccountLib bean);

    Boolean updateById(SysAccountLib bean);
}
