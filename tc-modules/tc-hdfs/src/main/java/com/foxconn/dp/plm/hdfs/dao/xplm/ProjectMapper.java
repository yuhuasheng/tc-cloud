package com.foxconn.dp.plm.hdfs.dao.xplm;

import com.foxconn.dp.plm.hdfs.domain.entity.PhaseEntity;
import com.foxconn.dp.plm.hdfs.domain.entity.TCProjectEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProjectMapper {

    List<TCProjectEntity> getProjectList(@Param("ids") List<TCProjectEntity> ids);

    List<TCProjectEntity> getVirtualProjectList();

    List<TCProjectEntity> getProjectIDsInSpas(String empId);

    TCProjectEntity getProjectById(String pid);

}
