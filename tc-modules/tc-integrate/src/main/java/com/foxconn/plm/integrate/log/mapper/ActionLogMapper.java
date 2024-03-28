package com.foxconn.plm.integrate.log.mapper;

import com.foxconn.plm.integrate.log.domain.ActionLog;
import com.foxconn.plm.integrate.log.domain.ActionLogRp;
import com.foxconn.plm.integrate.log.domain.ItemRev2Info;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface ActionLogMapper {


    public List<ActionLog> selectActionLog(@Param("userName") String userName ) throws Exception;

    public List<ActionLog> selectNonProj() throws Exception;

    public void updateProj(ActionLog actionLog) throws Exception;

    public void updateActionLog(ActionLog actionLog) throws Exception;


    void addLog(ActionLogRp pctionLogRp);


    void insertCISPart(ActionLogRp pctionLogRp);

    List<ActionLogRp> getCISActionLogData();

    Integer getActionLogRecord(ActionLogRp pctionLogRp);

    Integer getCISActionLogRecord(ActionLogRp pctionLogRp);

    List<ActionLogRp> getCISActionLog(ItemRev2Info itemRev2Info);

    void setActionLog(List<ActionLogRp> cisActionLogs);
}
