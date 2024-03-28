package com.foxconn.plm.integrate.log.service;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.integrate.cis.config.CISConstants;
import com.foxconn.plm.integrate.log.domain.ActionLog;
import com.foxconn.plm.integrate.spas.domain.CustQueryResults;
import com.foxconn.plm.integrate.spas.domain.D9Constants;
import com.foxconn.plm.integrate.spas.domain.PhasePojo;
import com.foxconn.plm.integrate.spas.domain.ReportPojo;
import com.foxconn.plm.integrate.spas.service.impl.SpasServiceImpl;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Item;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


@Component
public class SynLogInfoTask {
    private static Log log = LogFactory.get();
    @Autowired(required = false)
    private ActionLogServiceImpl actionLogServiceImpl;


    @Autowired(required = false)
    private ProjectInfoServiceImpl projectInfoServiceImpl;

    @Autowired(required = false)
    private SpasServiceImpl reportServiceImpl;

    /**
     * 刷新action_log表相关信息，用於報表統計
     */
    @XxlJob("TcActionLogSchedule")
    public void timedTask() {
        try {
            log.info("======================start fresh action log info========================");
            List<ActionLog> actionLogs = actionLogServiceImpl.selectActionLog();
            for (ActionLog actionLog : actionLogs) {
                try {
                    String projectId = actionLog.getProject();
                    if (projectId != null && !"".equalsIgnoreCase(projectId.trim())) {
                        projectId = projectId.toLowerCase(Locale.ENGLISH).replaceAll("p", "");
                        ReportPojo reportPojo = reportServiceImpl.getPhases(projectId);
                        String bu = reportPojo.getBu();
                        actionLog.setBu(bu);
                        actionLog.setCustomer(reportPojo.getCustomer());
                        String level = reportPojo.getLevels().split(",")[1];
                        actionLog.setProjLevel(level);
                        setPhase(actionLog, reportPojo);
                        System.out.print("");
                    }
                    actionLog.setHandleResult("success");
                    actionLog.setMsg("success");
                } catch (Exception e) {
                    actionLog.setHandleResult("failed");
                    actionLog.setMsg(e.getLocalizedMessage());
                    log.error(e.getLocalizedMessage(), e);
                }
            }
            for (ActionLog actionLog : actionLogs) {
                try {
                    actionLogServiceImpl.updateActionLog(actionLog);
                } catch (Exception e) {
                    log.error(e.getLocalizedMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            XxlJobHelper.handleFail(e.getLocalizedMessage());
        }
        log.info("======================end fresh action log info========================");
    }

    /**
     * 刷新spas_info表,用於報表統計
     */
   // @PostConstruct
   @XxlJob("TcSynProjectInfoSchedule")
    public void synProjectInfo() {
        try {
            projectInfoServiceImpl.synProjectInfo();
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
            XxlJobHelper.handleFail(e.getLocalizedMessage());
        }
    }


    private void setPhase(ActionLog actionLog, ReportPojo reportPojo) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String startTime = actionLog.getStartTime();
        Date d = sdf.parse(startTime);
        List<PhasePojo> phasePojos = reportPojo.getPhases();
        for (PhasePojo p : phasePojos) {
            String std = p.getStartDate();
            String edt = p.getEndDate();
            Date d1 = sdf.parse(std);
            Date d2 = sdf.parse(edt);
            if (d.getTime() > d1.getTime() && d.getTime() < d2.getTime()) {
                actionLog.setPhase(p.getName());
                actionLog.setPhaseEndDate(edt);
                break;
            }
        }
    }


}
