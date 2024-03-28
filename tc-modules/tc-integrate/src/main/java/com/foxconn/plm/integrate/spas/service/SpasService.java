package com.foxconn.plm.integrate.spas.service;

import com.foxconn.plm.integrate.spas.domain.PhasePojo;
import com.foxconn.plm.integrate.spas.domain.ReportPojo;
import com.foxconn.plm.integrate.spas.domain.SPASUser;
import com.foxconn.plm.integrate.spas.domain.STIProject;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface SpasService {

    public List<ReportPojo> searchPojects(String startDate, String endDate, String buName) throws Exception;


    public ReportPojo getPhases(String projectId) throws Exception;
    public List<ReportPojo> getAllPhases(String projectId) throws Exception;

    public List<Map> getCurBUTCProject(List<Map> list, String BUName);

    public STIProject getProjectInfo(@Param("projId") String projId);

    List<SPASUser> queryTeamRoster(String[] platformFoundIds);

    List<SPASUser> queryTeamRosterByEmpId(String empId);

    List<SPASUser> selectSPASUser();
}
