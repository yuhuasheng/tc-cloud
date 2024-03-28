package com.foxconn.plm.integrate.log.mapper;

import com.foxconn.plm.integrate.log.domain.ActionLog;
import com.foxconn.plm.integrate.log.domain.ActionLogRp;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


@Mapper
public interface UpLogMapper {

    public List<ActionLog> selectActionLog() throws Exception;

    public List<ActionLog> selectNonProj() throws Exception;

    public void updateProj(ActionLog actionLog) throws Exception;

    public void updateActionLog(ActionLog actionLog) throws Exception;


    public List<String> selectActiveProjInTC() throws Exception;

    public List<String> getActualUsers() throws Exception;

    public List<String> getProjsIntc() throws Exception;
}
