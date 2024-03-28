package com.foxconn.dp.plm.hdfs.dao.xplm;


import com.foxconn.dp.plm.hdfs.domain.entity.LOVEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MailGroupUserMapper {

    int existInGroup(String empId);

}
