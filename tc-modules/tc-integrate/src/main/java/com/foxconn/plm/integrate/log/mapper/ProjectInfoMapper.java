package com.foxconn.plm.integrate.log.mapper;

import com.foxconn.plm.integrate.log.domain.ManpowerPhaseInfo;
import com.foxconn.plm.integrate.log.domain.ProjectInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface ProjectInfoMapper {


    void addProjInfo(ProjectInfo projectLogInfo);

    void deleteProjInfo();

    public List<String> selectActiveProjInTC(@Param("userName") String userName ) throws Exception;

    public List<String> getActualUsers() throws Exception;

    public List<String> getProjsIntc() throws Exception;

    List<ManpowerPhaseInfo> getManPowerFunction(String projectId)throws Exception ;
}
