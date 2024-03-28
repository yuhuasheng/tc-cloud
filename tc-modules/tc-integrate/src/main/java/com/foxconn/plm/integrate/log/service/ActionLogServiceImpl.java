package com.foxconn.plm.integrate.log.service;


import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.dp.plm.privately.Access;
import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.integrate.log.domain.ActionLog;
import com.foxconn.plm.integrate.log.domain.ActionLogRp;
import com.foxconn.plm.integrate.log.domain.ItemRev2Info;
import com.foxconn.plm.integrate.log.mapper.ActionLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Service("actionLogServiceImpl")
public class ActionLogServiceImpl {
    private static Log log = LogFactory.get();
    @Autowired(required = false)
    private ActionLogMapper actionLogMapper;


    /**
     *
     */
    public void addLog(List<ActionLogRp> actionLogRps) {
        for (ActionLogRp l : actionLogRps) {
            log.info(" creator:" + l.getCreator() + " function name:" + l.getCreatorName() + " start time:" + l.getStartTime() + " end time:" + l.getEndTime() + " project:" + l.getProject() + " phase:" + l.getPhase() + " item_id:" + l.getItemId() + " rev:" + l.getRev());
            if (l.getFunctionName() == null || "".equalsIgnoreCase(l.getFunctionName())) {
                throw new BizException("功能名稱不能爲空");
            }
            if (!checkDate(l.getEndTime())) {
                throw new BizException("時間格式不正確");
            }

            String projs = l.getProject();
            if (projs != null && !"".equalsIgnoreCase(projs)) {
                String[] m = projs.split(",");
                for (String p : m) {
                    l.setProject(p);
                    Integer count = actionLogMapper.getActionLogRecord(l);
                    if (count > 0) {
                        continue;
                    }
                    actionLogMapper.addLog(l);
                }
            } else {
                actionLogMapper.addLog(l);
            }

        }
    }


    public List<ActionLog> selectNonProj() throws Exception {
        return actionLogMapper.selectNonProj();

    }

    ;

    public void updateProj(ActionLog actionLog) throws Exception {
        actionLogMapper.updateProj(actionLog);
    }

    ;


    boolean checkDate(String datStr) {
        if (datStr == null || "".equalsIgnoreCase(datStr)) {
            return false;
        }
        Date date = null;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            date = sdf.parse(datStr);
        } catch (ParseException ex) {
            ex.printStackTrace();

        }

        return date != null;

    }


    public List<ActionLog> selectActionLog() throws Exception {
        String userName= Access.getPasswordAuthentication();
        return actionLogMapper.selectActionLog(Access.check(userName));

    }

    public void updateActionLog(ActionLog actionLog) throws Exception {
        actionLogMapper.updateActionLog(Access.check(actionLog));
    }


    public void add2log(List<ItemRev2Info> ItemRev2Infos) {
        List<ActionLogRp> cisActionLogs = new ArrayList<>();
        for (int i = 0; i < ItemRev2Infos.size(); i++) {
            ItemRev2Info itemRev2Info = ItemRev2Infos.get(i);

            List<ActionLogRp> cisActionLogList = actionLogMapper.getCISActionLog(Access.check(itemRev2Info));
            if (cisActionLogList == null && cisActionLogs.size() == 0) {
                continue;
            }

            for (int j = 0; j < cisActionLogList.size(); j++) {
                ActionLogRp actionLogRp = cisActionLogList.get(j);
                String uid = itemRev2Info.getUid();
                String project = itemRev2Info.getProject();
                actionLogRp.setRevUid(uid);
                actionLogRp.setProject(project);
                cisActionLogs.add(Access.check(actionLogRp));
            }
        }

        if (cisActionLogs.size() > 0) {
            actionLogMapper.setActionLog(cisActionLogs);
        }
    }

    public Integer getCISActionLogRecord(ActionLogRp actionLogRp) {
        return actionLogMapper.getCISActionLogRecord(actionLogRp);
    }

    public void insertCISPart(ActionLogRp actionLogRp) {
        actionLogMapper.insertCISPart(actionLogRp);
    }
}
