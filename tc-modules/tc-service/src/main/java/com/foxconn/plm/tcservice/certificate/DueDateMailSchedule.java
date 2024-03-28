package com.foxconn.plm.tcservice.certificate;

import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcapi.soa.client.AppXSession;
import com.foxconn.plm.tcservice.mapper.master.ProjectPMMapper;
import com.google.gson.Gson;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.services.strong.query._2006_03.SavedQuery;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.ImanQuery;
import com.teamcenter.soa.exceptions.NotLoadedException;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.SystemOutLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DueDateMailSchedule {

    private final String MNT = "MNT";
    private final String DT = "DT";
    private final String SOUTH_AFRICA = "South Africa";
    private final List<String> DT_CERTIFICATE_ITEMS = List.of("NOM-208/IFT Certificate", "NRCS", "SDPPI");

    @Resource
    private ProjectPMMapper projectPMMapper;

    @Value("${tc.certificate.mnt.leader.name}")
    private String mntLeaderName;

    @Value("${tc.certificate.mnt.leader.email}")
    private String mntLeaderEmail;

    @Value("${spring.profiles.active}")
    private String profile;

    @Resource
    private TcMailClient tcMailClient;


    @XxlJob("CertificateDueDateMailSchedule")
    public void DueDateMailNotice() {
        TCSOAServiceFactory tcsoaServiceFactory = null;
        try {
            XxlJobHelper.log(" *** certificate DueDateMailSchedule start  *** ");
            tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            DataManagementService dataManagementService = tcsoaServiceFactory.getDataManagementService();
            ModelObject[] allCer = queryCertificateItemRev(tcsoaServiceFactory.getSavedQueryService());
            loadAllProp(allCer, dataManagementService);
            Map<ProjectPojo, List<ModelObject>> projectModelMapping = getCerProjectInfo(allCer, dataManagementService);
            Date nowDate = getYMDDate(Calendar.getInstance());
            projectModelMapping.forEach((k, v) -> processor(k, v, nowDate, dataManagementService));
        } catch (Exception e) {
            e.printStackTrace();
            log.error("fail", e);
            XxlJobHelper.log(e);
        } finally {
            if (tcsoaServiceFactory != null) {
                tcsoaServiceFactory.logout();
            }
        }
    }

    public void processor(ProjectPojo projectPojo, List<ModelObject> list, Date nowDate, DataManagementService dataManagementService) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
        String bu = projectPojo.getBu();
        List<ModelObject> needMail = new ArrayList<>();
        boolean isSix = false;
        for (ModelObject modelObject : list) {
            try {
                Calendar validate = modelObject.getPropertyObject("d9_ValidDate").getCalendarValue();
                if (validate == null) {
                    continue;
                }
                Calendar validateCope = Calendar.getInstance();
                validateCope.setTime(validate.getTime());
                int month = 3;
                if (MNT.equalsIgnoreCase(bu)) {
                    if (SOUTH_AFRICA.equalsIgnoreCase(modelObject.getPropertyObject("d9_CertificateCountry").getStringValue())) {
                        month = 6;
                    }
                } else if (DT.equalsIgnoreCase(bu)) {
                    if (DT_CERTIFICATE_ITEMS.contains(modelObject.getPropertyObject("d9_CertificateItems").getStringValue())) {
                        month = 6;
                    }
                }
                if (month == 6) {
                    isSix = true;
                }
                validateCope.add(Calendar.MONTH, -month);
                if (nowDate.compareTo(validateCope.getTime()) == 0) {
                    needMail.add(modelObject);
                }
            } catch (Exception e) {
                log.error("DueDateMailSchedule processor get due date error", e);
                XxlJobHelper.log(e);
                e.printStackTrace();
            }
        }
        if (needMail.size() > 0) {

            Set<String> toMail = new HashSet<>();
            toMail.add(projectPojo.getPmName());
            String tcUser = "";
            String tcProjectName = projectPojo.getProjectName();
            try {
                ModelObject owningUser = needMail.get(0).getPropertyObject("owning_user").getModelObjectValue();
                dataManagementService.getProperties(new ModelObject[]{owningUser}, new String[]{"user_id"});
                tcUser = owningUser.getPropertyObject("user_id").getStringValue();
                //project_list
                List<ModelObject> projectList = needMail.get(0).getPropertyObject("project_list").getModelObjectListValue();
                if (projectList.size() > 0) {
                    ModelObject project = projectList.get(0);
                    dataManagementService.getProperties(new ModelObject[]{project}, new String[]{"project_name"});
                    tcProjectName = project.getPropertyObject("project_name").getStringValue();
                }
            } catch (NotLoadedException e) {
                e.printStackTrace();
            }
            String mailFrom = "";
            if (!"prod".equalsIgnoreCase(profile)) {
                mailFrom = "TC Test";
            }
            String six = isSix ? "或6月" : "";
            String subject = mailFrom + "【Teamcenter】中『" + tcProjectName + "』證書在3月" + six + "后將到期，請及時處理";
            String header = "Dear 『證書負責人&PM&主管』,";
            if (DT.equalsIgnoreCase(bu)) {
                header = "Dear 『證書負責人&PM』,";
            }
            if (MNT.equalsIgnoreCase(bu)) {
                toMail.add(mntLeaderEmail);
            }
            String body = "請登錄TC共用帳號『" + tcUser + "』完成『" + tcProjectName + "』. 相關證書的更新或者廢棄。證書列表如下：";
            StringBuilder cerTable = new StringBuilder();
            cerTable.append("<table border=1 cellspacing=0><tr><th>TC_Item</th><th>型號</th><th>認證國家</th><th>認證類別</th><th>認證項目</th><th>發證日期</th><th" +
                    ">有效日期</th" +
                    "></tr>");
            dataManagementService.getProperties(needMail.toArray(new ModelObject[0]), new String[]{"d9_Model"});
            for (ModelObject cerObject : needMail) {
                try {
                    String cerUser = cerObject.getPropertyObject("d9_ActualUserID").getStringValue();
                    toMail.add(projectPMMapper.getUserMail(cerUser));
                    Date cerDate = cerObject.getPropertyObject("d9_CertificationDate").getCalendarValue().getTime();
                    Date validDate = cerObject.getPropertyObject("d9_ValidDate").getCalendarValue().getTime();
                    cerTable.append("<tr><td>")
                            .append(cerObject.getPropertyObject("item_id").getStringValue()).append("</td><td>")
                            .append(cerObject.getPropertyObject("d9_Model").getStringValue()).append("</td><td>")
                            .append(cerObject.getPropertyObject("d9_CertificateCountry").getStringValue()).append("</td><td>")
                            .append(cerObject.getPropertyObject("d9_CertificateType").getStringValue()).append("</td><td>")
                            .append(cerObject.getPropertyObject("d9_CertificateItems").getStringValue()).append("</td><td>")
                            .append(simpleDateFormat.format(cerDate)).append("</td><td>")
                            .append(simpleDateFormat.format(validDate)).append("</td><tr>");
                } catch (NotLoadedException e) {
                    log.error("DueDateMailSchedule processor get mail date error", e);
                    XxlJobHelper.log(e);
                    e.printStackTrace();
                }
            }
            cerTable.append("</table>");
            String mailBody = "<html><br>" + header + "<br><br>" + body + cerTable.toString() + "<br> 此通知由 Teamcenter 系統 发送。" + mailFrom + "</html>";
            Map<String, String> httpmap = new HashMap<>();
            String toMailStr = String.join(",", toMail);
            httpmap.put("sendTo", toMailStr);
            httpmap.put("sendCc", "robert.y.peng@foxconn.com");
            httpmap.put("subject", subject);
            httpmap.put("htmlmsg", mailBody);
            Gson gson = new Gson();
            String data = gson.toJson(httpmap);
            tcMailClient.sendMail3Method(data);// 发送邮件
        }
    }


    public void loadAllProp(ModelObject[] allCer, DataManagementService dataManagementService) {
        dataManagementService.getProperties(allCer, new String[]{"item_id", "project_list", "project_ids", "d9_CertificateType",
                "d9_CertificateCountry", "d9_CertificateItems", "d9_CertificationDate", "d9_ValidDate", "d9_ActualUserID", "owning_user"});
    }

    public Map<ProjectPojo, List<ModelObject>> getCerProjectInfo(ModelObject[] allCer, DataManagementService dataManagementService) throws NotLoadedException {
        Map<ProjectPojo, List<ModelObject>> map = new HashMap<>();
        for (ModelObject cerObjert : allCer) {
            String projectId = cerObjert.getPropertyObject("project_ids").getStringValue();
            projectId = projectId.substring(1);
            if (StringUtils.hasLength(projectId)) {
                try {
                    ProjectPojo pojo = projectPMMapper.getProjectPMInfo(projectId);
                    if (pojo != null) {
                        putData(map, pojo, cerObjert);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error(projectId + " query spas project fail!", e);
                }
            } else {
                String itemId = cerObjert.getPropertyObject("item_id").getStringValue();
                System.out.println("project_ids is null ::: " + itemId);
                XxlJobHelper.log("project_ids is null ::: " + itemId);
            }
        }
        return map;
    }

    public void putData(Map<ProjectPojo, List<ModelObject>> map, ProjectPojo pojo, ModelObject cerObjert) {
        List<ModelObject> list = null;
        Optional<ProjectPojo> key = map.keySet().stream().filter(e -> e.getProjectId().equalsIgnoreCase(pojo.getProjectId())).findFirst();
        if (key.isPresent()) {
            list = map.get(key.get());
        } else {
            list = new ArrayList<>();
            map.put(pojo, list);
        }
        list.add(cerObjert);
    }

    public ModelObject[] queryCertificateItemRev(SavedQueryService savedQueryService) throws ServiceException {
        ImanQuery query = null;
        SavedQuery.GetSavedQueriesResponse savedQueries = savedQueryService.getSavedQueries();
        for (int i = 0; i < savedQueries.queries.length; i++) {
            if (savedQueries.queries[i].name.equals("__D9_Find_Certificate")) {
                query = savedQueries.queries[i].query;
                break;
            }
        }
        if (query == null) {
            return null;
        }
        String[] entries = new String[]{"Brand"};
        String[] values = new String[]{"*"};
        return getModelObjects(savedQueryService, query, entries, values);

    }


    public ModelObject[] getModelObjects(SavedQueryService savedQueryService, ImanQuery query, String[] entries, String[] values) {
        com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput[] savedQueryInput =
                new com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput[1];
        savedQueryInput[0] = new com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput();
        savedQueryInput[0].query = query;
        savedQueryInput[0].entries = entries;
        savedQueryInput[0].values = values;
        com.teamcenter.services.strong.query._2007_06.SavedQuery.ExecuteSavedQueriesResponse savedQueryResult =
                savedQueryService.executeSavedQueries(savedQueryInput);
        com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryResults found = savedQueryResult.arrayOfResults[0];
        return found.objects;
    }


    public Date getYMDDate(Calendar calendar) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        DecimalFormat dt = new DecimalFormat("00");
        int year = calendar.get(Calendar.YEAR);
        int mouth = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String mStr = dt.format(mouth);
        String dStr = dt.format(day);
        return simpleDateFormat.parse(year + mStr + dStr);
    }

}
