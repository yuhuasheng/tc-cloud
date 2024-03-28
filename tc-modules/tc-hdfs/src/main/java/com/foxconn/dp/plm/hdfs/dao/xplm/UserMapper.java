package com.foxconn.dp.plm.hdfs.dao.xplm;

import com.foxconn.dp.plm.hdfs.domain.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {

    List<UserEntity> getUserInfoInSpas(List<String> empId);


}
