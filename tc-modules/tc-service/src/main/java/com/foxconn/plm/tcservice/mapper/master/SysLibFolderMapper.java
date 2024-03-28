package com.foxconn.plm.tcservice.mapper.master;

import com.foxconn.plm.tcservice.issuemanagement.entity.SysLibFolder;
import org.apache.ibatis.annotations.Mapper;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2023/12/1 10:54
 **/
@Mapper
public interface SysLibFolderMapper {
    Long getId();
    Boolean insertEntity(SysLibFolder bean);
    Boolean updateById(SysLibFolder bean);
}
