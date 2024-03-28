package com.foxconn.plm.integrate.tcfr.scheduling;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.integrate.tcfr.domain.MeetBean;
import com.foxconn.plm.integrate.tcfr.mapper.TCFRMapper;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.string.StringUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.google.gson.Gson;
import com.teamcenter.services.loose.core.SessionService;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.services.strong.query._2006_03.SavedQuery;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.ImanQuery;
import com.teamcenter.soa.client.model.strong.User;
import com.teamcenter.soa.exceptions.NotLoadedException;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;

@Service
public class TCFRMailFollowUp {

    private static Log log = LogFactory.get();

    @Resource
    private TCFRMapper tcfrMapper;

    @Resource
    private TcMailClient tcMailClient;


    @XxlJob("tcfrTaskUrgeMail")
    public void urgeMail() {
        log.info("tcfr 邮件跟催定时任务开始");
        XxlJobHelper.log("tcfr 邮件跟催定时任务开始");
        TCSOAServiceFactory tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS2);
        SessionService sessionService = tcsoaServiceFactory.getSessionService();
        try {
            TCUtils.byPass(sessionService, true);
            List<Map<String, Object>> scheduleInfoList = getAllScheduleObject(tcsoaServiceFactory);
            XxlJobHelper.log("time schedule size : " + scheduleInfoList.size());
            if (scheduleInfoList.size() > 0) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                LocalDate nowDate = LocalDate.now();
                for (Map<String, Object> scheduleEntry : scheduleInfoList) {
                    try {
                        String scheduleTaskName = (String) scheduleEntry.get("scheduleTaskName");
                        String userFullName = (String) scheduleEntry.get("userFullName");
                        Date finishDate = (Date) scheduleEntry.get("finishDate");
                        String wfUserId = (String) scheduleEntry.get("userId");
                        String scheduleUid = (String) scheduleEntry.get("scheduleUid");
                        LocalDate dueDate = convertDate(finishDate);
                        int index = !userFullName.contains("(") ? 0 : userFullName.indexOf("(");
                        int days = isNeedUrgeMail(nowDate, dueDate, userFullName.substring(index));
                        if (days <= 0) {
                            String dueDateStr = dateFormat.format(finishDate);
                            Period between = Period.between(nowDate, dueDate);
                            int compare = between.getDays();
                            if (compare < 0) {
                                sendUrgeMail(scheduleUid, scheduleTaskName, dueDateStr, userFullName, "TCFR 時間表任務逾期提醒", "以下時間表任務已逾期", wfUserId);
                            } else {
                                sendUrgeMail(scheduleUid, scheduleTaskName, dueDateStr, userFullName, "TCFR 時間表任務即將到期提醒", "以下時間表任務即將到期", wfUserId);
                            }

                        }
                    } catch (Exception e) {
                        log.error(e);
                        XxlJobHelper.log(e);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e);
            XxlJobHelper.log(e);
            XxlJobHelper.handleFail();
            e.printStackTrace();
        } finally {
            tcsoaServiceFactory.logout();
            TCUtils.byPass(sessionService, false);
        }
    }

    //
    //
    public void sendUrgeMail(String uid, String scheduleTaskName, String dueDateStr, String userFullName, String subject,
                             String urgeContent, String tcUserId) throws NotLoadedException {
        if (StringUtil.isEmpty(userFullName)) {
            XxlJobHelper.log("userFullName is null:: " + scheduleTaskName);
            return;
        }
        String to = tcfrMapper.getUserMail(userFullName);
        if (StringUtil.isEmpty(to)) {
            XxlJobHelper.log("userFullName getUserMail is null:: " + userFullName);
            return;
        }
//        String tcUserId = tcfrMapper.getTCUserInfo(to).getTcUserId();
        MeetBean bean = tcfrMapper.getTCFRDataByTCUid(uid);
        if (bean == null) {
            XxlJobHelper.log("tc fr uid MeetBean is null:: " + scheduleTaskName);
            log.error("tc fr uid MeetBean is null:: " + scheduleTaskName);
            return;
        }
        Map<String, String> httpmap = new HashMap<>();
        httpmap.put("sendTo", to);
        httpmap.put("sendCc", "robert.y.peng@foxconn.com");
        httpmap.put("subject", subject);
        String msg =
                "<html><head></head><body>"
                        + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "Dear " + userFullName + "</div><br/>"
                        + "<div style=\"font-family: 宋体;  font-size:15px; \">" + urgeContent + "，请登陆下方Teamcenter賬號进行查看，谢谢！" + "</div>"
                        + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>Teamcenter账号：</strong>" + tcUserId + "</div>"
                        + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>时间表名称：</strong>" + bean.getMeetingTitle() + "</div>"
                        + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>时间表任务：</strong>" + scheduleTaskName + "</div>"
                        + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>Due date：</strong>" + dueDateStr + "</div>"
                        + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>任务路径:</strong>" + "D事業群企業知識庫/專案知識庫" + "</div>"
                        + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>专案系列:</strong>" + bean.getSpasSeries() + "</div>"
                        + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>专案名:</strong>" + bean.getProjectName() + "</div>"
                        + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>专案阶段:</strong>" + bean.getSpasProjPhase() + "</div>"
                        + "</body></html>";
        httpmap.put("htmlmsg", msg);
        Gson gson = new Gson();
        String data = gson.toJson(httpmap);
        tcMailClient.sendMail3Method(data);// 发送邮件

    }

    public List<Map<String, Object>> getAllScheduleObject(TCSOAServiceFactory tcsoaServiceFactory) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        SavedQueryService savedQueryService = tcsoaServiceFactory.getSavedQueryService();
        DataManagementService dataManagementService = tcsoaServiceFactory.getDataManagementService();
        ModelObject[] modelObjects = findWfScheduleTask(savedQueryService);
        if (modelObjects != null) {
            try {
                dataManagementService.getProperties(modelObjects, new String[]{"project_task_attachments", "owning_user"});
            } catch (Exception e) {
                log.error(e);
            }
            for (ModelObject taskObject : modelObjects) {
                try {
                    ModelObject[] wfTarget = taskObject.getPropertyObject("project_task_attachments").getModelObjectArrayValue();
                    ModelObject user = taskObject.getPropertyObject("owning_user").getModelObjectValue();
                    dataManagementService.getProperties(new ModelObject[]{user}, new String[]{"user_id"});
                    String userId = user.getPropertyObject("user_id").getStringValue();
                    if (wfTarget != null && wfTarget.length > 0) {
                        Map<String, Object> map = new HashMap<>();
                        ModelObject wfModelObject = wfTarget[0];
                        dataManagementService.getProperties(wfTarget, new String[]{"schedule_tag", "object_name", "d9_RealAuthor", "finish_date"});
                        ModelObject scheduleModelObject = wfModelObject.getPropertyObject("schedule_tag").getModelObjectValue();// schedule
                        String scheduleTaskName = wfModelObject.getPropertyObject("object_name").getStringValue();
                        String userFullName = wfModelObject.getPropertyObject("d9_RealAuthor").getStringValue();
                        Date finishDate = wfModelObject.getPropertyObject("finish_date").getCalendarValue().getTime();
                        String scheduleUid = scheduleModelObject.getUid();
                        map.put("userId", userId);
                        map.put("scheduleTaskName", scheduleTaskName);
                        map.put("userFullName", userFullName);
                        map.put("finishDate", finishDate);
                        map.put("scheduleUid", scheduleUid);
                        list.add(map);
                    }
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }
        return list;
    }

    public ModelObject[] findWfScheduleTask(SavedQueryService savedQueryService) throws Exception {
        ImanQuery query = null;
        SavedQuery.GetSavedQueriesResponse savedQueries = savedQueryService.getSavedQueries();
        for (int i = 0; i < savedQueries.queries.length; i++) {
            if (savedQueries.queries[i].name.equals("__D9_Find_ScheduleTask_with_State")) {
                query = savedQueries.queries[i].query;
                break;
            }
        }
        if (query == null) {
            return null;
        }
        String[] entries = new String[]{"taskStateValue", "objectType", "templateName"};
        String[] values = new String[]{"4", "EPMReviewTask;EPMConditionTask;EPMAcknowledgeTask", "FXN33*"};
        com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput[] savedQueryInput =
                new com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput[1];
        savedQueryInput[0] = new com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput();
        savedQueryInput[0].query = query;
        savedQueryInput[0].entries = entries;
        savedQueryInput[0].values = values;
        com.teamcenter.services.strong.query._2007_06.SavedQuery.ExecuteSavedQueriesResponse savedQueryResult =
                savedQueryService.executeSavedQueries(savedQueryInput);
        com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryResults found = savedQueryResult.arrayOfResults[0];
        if (found.numOfObjects <= 0) {
            return null;
        }
        return found.objects;
    }

    public int isNeedUrgeMail(LocalDate localDateNow, LocalDate localDateDue, String userId) {
        int betweenDays = 0;
        LocalDate localDateDueCopy = localDateDue;
        do {
            localDateDueCopy = localDateDueCopy.minusDays(1);
            if (!judgeNotWorking(userId, localDateDueCopy)) {
                betweenDays++;
            }
        } while (betweenDays != 3);
        Period between = Period.between(localDateNow, localDateDueCopy);
        int days = between.getDays();
        return days;
    }

    public boolean judgeNotWorking(String userId, LocalDate localDate) {
        Date date = convertDate(localDate);
        String workingStr = "";
        if (StringUtils.hasLength(userId) && Character.isDigit(userId.charAt(0))) {
            workingStr = tcfrMapper.getWrokDayTpe(date);
        } else {
            workingStr = tcfrMapper.getWrokDayMainland(date);
        }
        return "N".equalsIgnoreCase(workingStr);
    }

    Date convertDate(LocalDate localDate) {
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zdt = localDate.atStartOfDay(zoneId);
        return Date.from(zdt.toInstant());
    }

    LocalDate convertDate(Date date) {
        Instant instant = date.toInstant();
        ZoneId zoneId = ZoneId.systemDefault();
        return instant.atZone(zoneId).toLocalDate();
    }


}
