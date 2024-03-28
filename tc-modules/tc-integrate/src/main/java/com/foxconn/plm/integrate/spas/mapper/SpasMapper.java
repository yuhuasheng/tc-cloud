package com.foxconn.plm.integrate.spas.mapper;

import com.foxconn.plm.integrate.mail.domain.MailUser;
import com.foxconn.plm.integrate.spas.domain.PhasePojo;
import com.foxconn.plm.integrate.spas.domain.ReportPojo;
import com.foxconn.plm.integrate.spas.domain.SPASUser;
import com.foxconn.plm.integrate.spas.domain.STIProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface SpasMapper {


    public List<ReportPojo> queryProjectPhases(@Param("projectId") String projectId );

    public List<ReportPojo> queryProjectById(@Param("projectId") String projectId );

    public List<ReportPojo> queryProjects(@Param("sdt") String std, @Param("edt") String edt) throws Exception;

    public List<PhasePojo> getPhases(@Param("projectId") String projectId) throws Exception;

    public STIProject getProjectInfo(@Param("projId") String projId);


    List<SPASUser> queryTeamRoster(String[] platformFoundIds);

    List<SPASUser> queryTeamRosterByEmpId(String empId);

    List<SPASUser> selectSPASUser();


    public List<MailUser> findMailUsers(@Param("keyWords") String keyWords);

    List<SPASUser> getSpasUserInfoByDate(@Param("startDate") Date startDate, @Param("endDate") Date endDate,@Param("prefix")String prefix);
}
