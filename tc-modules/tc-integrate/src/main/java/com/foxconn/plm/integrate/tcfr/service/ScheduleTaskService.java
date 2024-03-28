package com.foxconn.plm.integrate.tcfr.service;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.constants.*;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.integrate.tcfr.domain.*;
import com.foxconn.plm.integrate.tcfr.mapper.TCFRMapper;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.collect.CollectUtil;
import com.foxconn.plm.utils.date.DateUtil;
import com.foxconn.plm.utils.string.StringUtil;
import com.foxconn.plm.utils.tc.*;
import com.google.gson.Gson;
import com.teamcenter.services.loose.core.SessionService;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core.LOVService;
import com.teamcenter.services.strong.core.ProjectLevelSecurityService;
import com.teamcenter.services.strong.core._2006_03.DataManagement;
import com.teamcenter.services.strong.projectmanagement.ScheduleManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.services.strong.workflow.WorkflowService;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.exceptions.NotLoadedException;
import com.teamcenter.soa.internal.client.model.ModelObjectImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service("ScheduleTaskService")
public class ScheduleTaskService   {

    private static Log log = LogFactory.get();

    public static final String sdf = "yyyy-MM-dd";

    public static final String sdf1 = "yyyy-MM-dd'T'HH:mm:ssXXX";

    @Resource
    private TCFRMapper tcfrMapper;

    @Resource
    private TcMailClient tcMailClient;

    private String projectName; // 专案名称

    private Map<EPMTaskTemplate, String> WORKFLOWTEMPLATEMAP;

    private Cache<String, String> scheduleTaskCache = CacheUtil.newFIFOCache(20);


    public R saveTCData(JSONObject paramJSONObject) throws Exception {
        log.info("==>> 会议记录传递到TC开始执行");
        MeetInfo meetInfo = JSONObject.toJavaObject(paramJSONObject, MeetInfo.class);
        log.info("==>> meetInfo: " + meetInfo);
        String key= SecureUtil.md5(meetInfo.getSpasProjId() + meetInfo.getSpasProjPhase() + meetInfo.getSpasSeries() + meetInfo.getMeetingStartDate() + meetInfo.getMeetingTitle() + meetInfo.getMeetingMinutesPath()
                + meetInfo.getCustomerName() + meetInfo.getMeetMainOwner());
        if (scheduleTaskCache.get(key) != null) {
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),"頻繁訪問");
        }

        TCSOAServiceFactory tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS2);
        List<String> userErrorList = null;
        try {
            scheduleTaskCache.put(key,key, DateUnit.MINUTE.getMillis()*5); // 设置失效时间为5分钟
            MeetBean rootBean = saveTCFRDataToDB(meetInfo); // 保存数据至DB表中
            ScheduleManagementService scheduleService = tcsoaServiceFactory.getScheduleManagementService();
            DataManagementService dmService = tcsoaServiceFactory.getDataManagementService();
            SavedQueryService savedQueryService = tcsoaServiceFactory.getSavedQueryService();
            LOVService lovService = tcsoaServiceFactory.getLovService();
            WorkflowService workflowService = tcsoaServiceFactory.getWorkflowService();
            FileManagementUtility fmUtility = tcsoaServiceFactory.getFileManagementUtility();
            SessionService sessionService = tcsoaServiceFactory.getSessionService();
            ProjectLevelSecurityService projectLevelSecurityService = tcsoaServiceFactory.getProjectLevelSecurityService();
            TCUtils.byPass(sessionService, true);

            String msg = "";

            Folder TCFRPhaseFolder = createTCFRAndPhaseFolder(savedQueryService, dmService, meetInfo); // 创建TCFR文件夹和阶段文件夹
            TCUtils.refreshObject(dmService, new ModelObject[]{TCFRPhaseFolder});
            ModelObject[] objs = TCUtils.getPropModelObjectArray(dmService, TCFRPhaseFolder, TCFolderConstant.REL_CONTENTS);

            ModelObject schedule = null;
            Map<String, String> dateMap = null;
            boolean createScheduleFlag = false;
            List<MeetDataInfo> dataInfos = meetInfo.getData();
            if (CollUtil.isNotEmpty(dataInfos)) {
                log.info("==>> : dataInfos: " + JSONUtil.toJsonPrettyStr(dataInfos));
                updateDateFormat(meetInfo); // 更新日期字段格式
                removeInvalidChar(meetInfo); // 移除无效的字符
                List<String> dateList = getDateList(dataInfos);
                if (CollUtil.isEmpty(dateList)) {
                    throw new Exception("计划开始时间和计划结束时间未填写或者不符合规范yyyy/MM/dd样式");
                }

                dateMap = DateUtil.getMinAndMaxDate(dateList, sdf);
                if (CollUtil.isEmpty(dateMap)) {
                    throw new Exception("获取会议开始时间和会议结束时间失败");
                }

                WORKFLOWTEMPLATEMAP = WorkFlowUtil.getAllWorkflowTemplates(workflowService, dmService); // 获取所有的流程模板
                schedule = TCUtils.checkModelObjExist(dmService, objs, meetInfo.getMeetingTitle(), TCScheduleConstant.PROPERTY_OBJECT_TYPE_SCHEDULETYPE, TCFolderConstant.PROPERTY_OBJECT_NAME);
                if (null == schedule) {
                    try {
                        schedule = generateSchedule(scheduleService, dmService, dateMap, meetInfo, TCFRPhaseFolder); // 生成时间表对象
                        createScheduleFlag = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error(e.getLocalizedMessage());
                        msg += "会议名称为:" + meetInfo.getMeetingTitle() + " 创建TC时间表失败，错误信息为: " + e.getLocalizedMessage();
                    }
                }

                String[] preferences = TCUtils.getTCPreferences(tcsoaServiceFactory.getPreferenceManagementService(), TCPreferenceConstant.ScheduleDeliverableWSOTypes);
                if (ArrayUtil.isEmpty(preferences)) {
                    throw new Exception("首选项: " + TCPreferenceConstant.ScheduleDeliverableWSOTypes + ", 不存在，请联系TC系统管理员进行处理");
                }

                if (schedule != null) {
                    rootBean.setScheduleUid(schedule.getUid());
                    tcfrMapper.updateScheduleUid(rootBean); // 将时间表uid更新到数据库表
                    Map<String, List<String>> map = getDeliverableList(savedQueryService, dmService, dataInfos);// 获取交付件名称集合
                    userErrorList = map.get("userErrorList");
                    Map<String, ModelObject> scheduleDeliverMap = null;
                    if (CollUtil.isNotEmpty(map.get("deliverableList"))) {
                        scheduleDeliverMap = ScheduleTaskUtil.createScheduleDeliverableList(scheduleService, dmService, (Schedule) schedule, map.get("deliverableList"), preferences[0]);// 创建时间表交付件
                    }
                    msg += generateScheduleTask(scheduleService, savedQueryService, dmService, (Schedule) schedule, dataInfos, meetInfo, scheduleDeliverMap, 0); // 创建时间表任务
                }
            }



            ItemRevision documentItemRev = generateDocument(meetInfo, dmService, lovService, fmUtility, TCFRPhaseFolder, objs); // 创建存放会议纪要的文档对象
            if (schedule != null) {
                msg += updateScheduleStatus(savedQueryService, dmService, (Schedule) schedule, meetInfo, dateMap.get("maxDate"), createScheduleFlag);// 更新时间表任务对象并且发送邮件
            }

            msg += updateDocumentStatus(projectLevelSecurityService, savedQueryService, dmService, documentItemRev, meetInfo);


            if (ObjUtil.isNotEmpty(documentItemRev)) {
                String documentId = TCUtils.getPropStr(dmService, documentItemRev, TCItemConstant.PROPERTY_ITEM_ID);
                String documentVer = TCUtils.getPropStr(dmService, documentItemRev, TCItemConstant.PROPETY_ITEM_REVISION_ID);
                String documentUid = documentItemRev.getUid();

                rootBean.setDocumentId(documentId);
                rootBean.setDocumentVer(documentVer);
                rootBean.setDocumentUid(documentUid);

                tcfrMapper.updateDocumentInfo(rootBean); // 更新文档对象ID，版本号，文档对象版本UID
            }

            rootBean.setMsg("N");
            tcfrMapper.updateFlag(rootBean); // 将信息标识设置为N，代表需要重新同步会议附件

            if (CollUtil.isNotEmpty(userErrorList)) {
                msg += String.join(";", userErrorList);
                throw new Exception(msg);
            }

            log.info("==>> 会议记录传递到TC执行结束");
            return R.success("传递TC成功");
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage());
            scheduleTaskCache.remove(key); // 从缓存中移除此消息
            throw new Exception(e);
        } finally {
            tcsoaServiceFactory.logout();
        }
    }


    /**
     * 保存数据至DB表中
     *
     * @throws Exception
     */
    private MeetBean saveTCFRDataToDB(MeetInfo meetInfo) throws Exception {
        MeetBean rootBean = new MeetBean(meetInfo);
        if (tcfrMapper.getTCFRDataCount(rootBean) <= 0) {
            tcfrMapper.insertTCFRData(rootBean);
        } else {
            tcfrMapper.updateTCFRFilePath(rootBean);
        }
        return rootBean;
    }


    /**
     * 添加会议记忆文档和将会议附件添加到会议文档对象中
     *
     * @param meetInfo
     * @param dmService
     * @param lovService
     * @param fmUtility
     * @param TCFRPhaseFolder
     * @param objs
     * @throws Exception
     */
    private ItemRevision generateDocument(MeetInfo meetInfo, DataManagementService dmService, LOVService lovService, FileManagementUtility fmUtility, Folder TCFRPhaseFolder, ModelObject[] objs) throws Exception {
        String meetingPath = meetInfo.getMeetingMinutesPath();
        if (StringUtil.isEmpty(meetingPath)) {
            return null;
        }

        ItemRevision documentItemRev = null;
        if (StringUtil.isNotEmpty(meetingPath)) {
            ModelObject document = TCUtils.checkModelObjExist(dmService, objs, meetInfo.getMeetingTitle(), TCItemTypeConstant.DOCUMENT_ITEM_TYPE, TCItemConstant.PROPERTY_OBJECT_NAME);
            if (null == document) {
                documentItemRev = createDocument(lovService, dmService, TCFRPhaseFolder, meetInfo);
                if (null == documentItemRev) {
                    throw new Exception("创建会议记录文档对象失败");
                }
//                if (StringUtil.isNotEmpty(mainOwner)) {
//                    TCUtils.setProperties(dmService, documentItemRev, TCItemConstant.PROPERTY_D9_ACTUALUSERID, mainOwner); // 保存会议文档实际用户属性值
//                }
            } else {
                documentItemRev = TCUtils.getItemLatestRevision(dmService, (Item) document);
            }
        }
        return documentItemRev;
    }

    /**
     * 获取日期集合
     *
     * @param list
     * @return
     */
    private List<String> getDateList(List<MeetDataInfo> list) {
        List<String> retList = new ArrayList<>();
        for (MeetDataInfo info : list) {
            retList.add(info.getPlanStartDate());
            retList.add(info.getPlanEndDate());
        }

        retList.removeIf(str -> str == null || "".equals(str) || "N/A".equals(str));
        retList = retList.stream().filter(CollectUtil.distinctByKey(str -> str)).collect(Collectors.toList());
        return retList;
    }

    /**
     * 创建时间表对象
     *
     * @param scheduleService 时间表服务类
     * @param dmService       工具类
     * @param map
     * @param meetInfo
     * @param folder          挂载到某个文件夹
     * @return
     * @throws Exception
     */
    private Schedule generateSchedule(ScheduleManagementService scheduleService, DataManagementService dmService, Map<String, String> map, MeetInfo meetInfo, Folder folder) throws Exception {
        Calendar start = DateUtil.dealDateFormat(map.get("minDate") + "T08:00:00+08:00", sdf1);
        Calendar finish = DateUtil.dealDateFormat(map.get("maxDate") + "T17:00:00+08:00", sdf1);
        String objectName = meetInfo.getMeetingTitle();
        Schedule[] schedules = ScheduleTaskUtil.createSchedule(scheduleService, dmService, TCScheduleConstant.PROPERTY_OBJECT_TYPE_SCHEDULETYPE,
                objectName, null, start, finish, folder);
        if (null == schedules || schedules.length <= 0) {
            throw new Exception("SPAS ID为: " + meetInfo.getSpasProjId() + ", 创建时间表对象失败");
        }
        return schedules[0];

    }


    /**
     * 创建TCFR文件夹和阶段文件夹
     *
     * @param savedQueryService 查询服务类
     * @param dmService         工具类
     * @param meetInfo
     * @return
     * @throws Exception
     */
    private Folder createTCFRAndPhaseFolder(SavedQueryService savedQueryService, DataManagementService dmService, MeetInfo meetInfo) throws Exception {
        String projectId = "p" + meetInfo.getSpasProjId();
        String phaseName = meetInfo.getSpasProjPhase();
        ModelObject[] objects = TCUtils.executequery(savedQueryService, dmService, TCSearchEnum.D9_FIND_PROJECT_FOLDER.queryName(),
                TCSearchEnum.D9_FIND_PROJECT_FOLDER.queryParams(),
                new String[]{projectId, "*"});
        if (null == objects || objects.length <= 0) {
            throw new Exception("SPAS ID为: " + meetInfo.getSpasProjId() + ", 在TC中未查询到专案文件夹");
        }

        Folder projectFolder = (Folder) objects[0];
        projectName = TCUtils.getPropStr(dmService, projectFolder, TCFolderConstant.PROPERTY_OBJECT_NAME);
        log.info("==>> projectName: " + projectName);

        ModelObject TCFRFolder = null;
        TCUtils.refreshObject(dmService, projectFolder);
        ModelObject[] functionFolders = TCUtils.getPropModelObjectArray(dmService, projectFolder, TCFolderConstant.REL_CONTENTS);
        if (functionFolders != null && functionFolders.length > 0) {
            TCFRFolder = TCUtils.checkModelObjExist(dmService, functionFolders, TCFRConstant.TCFRFOLDER, TCFolderConstant.TYPE_D9_FUNCTIONFOLDER,
                    TCFolderConstant.PROPERTY_OBJECT_NAME);
        }
        if (null == TCFRFolder) {
            TCFRFolder = FolderUtil.createReferenceFolder(dmService, projectFolder, TCFRConstant.TCFRFOLDER, TCFolderConstant.TYPE_D9_FUNCTIONFOLDER);
        }

        if (null == TCFRFolder) {
            throw new Exception("专案名称为: " + projectName + ", 创建TCFR文件夹失败");
        }

        ModelObject TCFRPhaseFolder = null;
        TCUtils.refreshObject(dmService, TCFRFolder);
        ModelObject[] phaseFolders = TCUtils.getPropModelObjectArray(dmService, TCFRFolder, TCFolderConstant.REL_CONTENTS);
        if (phaseFolders != null && phaseFolders.length > 0) {
            TCFRPhaseFolder = TCUtils.checkModelObjExist(dmService, phaseFolders, phaseName, TCFolderConstant.TYPE_D9_PHASEFOLDER,
                    TCFolderConstant.PROPERTY_OBJECT_NAME);
        }

        if (null == TCFRPhaseFolder) {
            TCFRPhaseFolder = FolderUtil.createReferenceFolder(dmService, (Folder) TCFRFolder, phaseName, TCFolderConstant.TYPE_D9_PHASEFOLDER);
        }

        if (null == TCFRPhaseFolder) {
            throw new Exception("专案名称为: " + projectName + ", TCFR文件夹, 创建阶段文件夹: " + phaseName + " 失败");
        }

        return (Folder) TCFRPhaseFolder;
    }


    /**
     * 生成时间表任务
     *
     * @param scheduleService
     * @param savedQueryService
     * @param dmService
     * @param schedule
     * @param dataInfos
     * @param meetInfo
     * @param scheduleDeliverMap
     * @param submitType
     * @throws Exception
     */
    private String generateScheduleTask(ScheduleManagementService scheduleService, SavedQueryService savedQueryService, DataManagementService dmService, Schedule schedule, List<MeetDataInfo> dataInfos, MeetInfo meetInfo,
                                        Map<String, ModelObject> scheduleDeliverMap, int submitType) throws NotLoadedException {
        ScheduleTask rootTask = (ScheduleTask) TCUtils.getPropModelObject(dmService, schedule, TCScheduleConstant.REL_FND0SUMMARYTASK);
        Calendar start = null;
        Calendar finish = null;
        String errorMsg = "";
        for (MeetDataInfo dataInfo : dataInfos) {
            try {
                String[] propNames = {TCScheduleConstant.PROPERTY_OBJECT_DESC};
                String[] propValues = {dataInfo.getActionItemId()};
                String taskName = dataInfo.getActionItem();
                String owner = dataInfo.getOwners();
                Map<String, List<?>> listMap = getTCUser(savedQueryService, dmService, owner);
                List<User> userList = null;
                List<TCUserBean> userInfoList = null;
                List<String> userErrorList = null;
                if (listMap != null) {
                    userList = (List<User>) listMap.get("userList");
                    userInfoList = (List<TCUserBean>) listMap.get("userInfoList");
                    userErrorList = (List<String>) listMap.get("userErrorList");
                }

                if (CollUtil.isNotEmpty(userErrorList)) {
                    continue;
                }

                long compareTime = DateUtil.compareTime(dataInfo.getPlanStartDate(), dataInfo.getPlanEndDate(), sdf);
                if (compareTime == -1) {
                    errorMsg += "actionItem为: " + dataInfo.getActionItem() + ", 开始日期/结束日期未填写;";
                    continue;
                }
                if (compareTime < 0) {
                    errorMsg += "actionItem为: " + dataInfo.getActionItem() + ", 开始日期大于结束日期;";
                    continue;
                }

                start = DateUtil.dealDateFormat(dataInfo.getPlanStartDate() + "T08:00:00+08:00", sdf1);
                finish = DateUtil.dealDateFormat(dataInfo.getPlanEndDate() + "T17:00:00+08:00", sdf1);
                long day = DateUtil.getDaySub(dataInfo.getPlanStartDate(), dataInfo.getPlanEndDate(), sdf);
                int workEstimate = getWorkEstimate(day);
                EPMTaskTemplate workFlowTemplate = getWorkFlowTemplate(TCFRConstant.FXN33_Schedule_Collaboration_Process);
                List<String> scheduleTaskDeliverList = null;
                List<String> spasUserInfo = null;
                if (userInfoList != null && userInfoList.size() > 0) {
                    scheduleTaskDeliverList = userInfoList.stream().map(e -> getDeliverName(e.getWorkName() + "_" + dataInfo.getActionItem())).collect(Collectors.toList());
                    spasUserInfo = userInfoList.stream().map(e -> e.getWorkName() + "(" + e.getWorkId() + ")").collect(Collectors.toList());
//                scheduleTaskDeliverList = userInfoList.stream().map(getDeliverName(TCUserBean::getWorkName + "_" + dataInfo.getActionItem()).collect(Collectors.toList());
                }
                Map<ScheduleTask, Boolean> retMap = ScheduleTaskUtil.generateScheduleTask(scheduleService, dmService, schedule, rootTask, rootTask.getUid(), null, taskName, start,
                        finish, workEstimate, TCScheduleConstant.PROPERTY_OBJECT_TYPE_SCHEDULETASKTYPE, propNames, propValues, userList, workFlowTemplate,
                        scheduleDeliverMap, scheduleTaskDeliverList, spasUserInfo, submitType, dataInfo.getPlanStartDate() + "T08:00:00+08:00", dataInfo.getPlanEndDate() + "T17:0000+08:00", null);
                if (CollUtil.isNotEmpty(retMap)) {
                    for (Map.Entry<ScheduleTask, Boolean> entry : retMap.entrySet()) {
                        if (TCUtils.getPropStr(dmService, entry.getKey(), TCScheduleConstant.PROPERTY_OBJECT_NAME).equals(taskName) && entry.getValue()) {
                            sendNotifyMail(meetInfo.getMeetingTitle(), taskName, owner, meetInfo.getSpasSeries(), meetInfo.getSpasProjPhase(), dataInfo.getPlanEndDate(), userInfoList, true); // 发送通知邮件
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                errorMsg += "actionItem为: " + dataInfo.getActionItem() + e.getLocalizedMessage();
            }
        }
        return errorMsg;
    }

    /**
     * 获取TC用户
     *
     * @param savedQueryService
     * @param dmService
     * @param owner
     * @return
     * @throws Exception
     */
    private Map<String, List<? extends Object>> getTCUser(SavedQueryService savedQueryService, DataManagementService dmService, String owner) throws Exception {
        Map<String, List<? extends Object>> map = new HashMap<>();
        List<User> userList = new ArrayList<>();
        List<TCUserBean> userInfoList = new ArrayList<>();
        List<String> userErrorList = new ArrayList<>();
        if (StringUtil.isEmpty(owner)) {
            return null;
        }

        List<String> convertOwners = ScheduleTaskUtil.convertOwners(owner);
        if (CollUtil.isEmpty(convertOwners)) {
            return null;
        }

        for (String str : convertOwners) {
            TCUserBean tcUserBean = tcfrMapper.getTCUserInfo(str);
            if (null == tcUserBean) {
                userErrorList.add("邮箱为: " + str + ", 不存在TC账号");
                continue;
            }
            userInfoList.add(tcUserBean);
            String userId = tcUserBean.getTcUserId();
//            String userId = "dev";
            log.info("==>> userId: " + userId);

            ModelObject[] objects = TCUtils.executequery(savedQueryService, dmService, TCSearchEnum.WEB_FIND_USER.queryName(),
                    TCSearchEnum.WEB_FIND_USER.queryParams(), new String[]{userId});
            if (null == objects || objects.length <= 0) {
                userErrorList.add("工号为: " + userId + ", 查询TC账号失败");
                continue;
            }
            userList.add((User) objects[0]);
        }

        userList = userList.stream().filter(CollectUtil.distinctByKey(ModelObjectImpl::getUid)).collect(Collectors.toList());
        userInfoList = userInfoList.stream().filter(CollectUtil.distinctByKey(TCUserBean::getWorkId)).collect(Collectors.toList());
        map.put("userList", userList);
        map.put("userInfoList", userInfoList);
        map.put("userErrorList", userErrorList);
        return map;
    }


    /**
     * 时间表交付件名称集合
     *
     * @param dataInfos
     * @return
     */
    private Map<String, List<String>> getDeliverableList(SavedQueryService savedQueryService, DataManagementService dmService, List<MeetDataInfo> dataInfos) throws Exception {
        Map<String, List<String>> retMap = new HashMap<>();
        List<String> deliverableList = new ArrayList<>();
        List<String> userErrorList = new ArrayList<>();
        String errMsg = "";
        for (MeetDataInfo dataInfo : dataInfos) {
            String owner = dataInfo.getOwners();
            String actionItem = dataInfo.getActionItem();
            if (StringUtil.isEmpty(owner)) {
                errMsg = "actionItem为: " + actionItem + " owner不存在";
                log.error(errMsg);
                userErrorList.add(errMsg);
                continue;
            }

            List<String> convertOwners = ScheduleTaskUtil.convertOwners(owner);
            if (CollUtil.isEmpty(convertOwners)) {
                errMsg = "actionItem为: " + actionItem + " owner填写不符合规范";
                log.error(errMsg);
                userErrorList.add(errMsg);
                continue;
            }

            for (String str : convertOwners) {
                TCUserBean tcUserBean = tcfrMapper.getTCUserInfo(str);
                if (null == tcUserBean) {
                    errMsg = "actionItem为: " + actionItem + " owner为: " + str + ", 不存在TC账号";
                    log.error(errMsg);
                    userErrorList.add(errMsg);
                    continue;
                }

                String userId = tcUserBean.getTcUserId();
//            String userId = "dev";
                log.info("==>> userId: " + userId);

                ModelObject[] objects = TCUtils.executequery(savedQueryService, dmService, TCSearchEnum.WEB_FIND_USER.queryName(),
                        TCSearchEnum.WEB_FIND_USER.queryParams(), new String[]{userId});
                if (null == objects || objects.length <= 0) {
                    errMsg = "==>> actionItem为: " + actionItem + "工号为: " + userId + ", 查询TC账号失败";
                    log.error(errMsg);
                    userErrorList.add(errMsg);
                    continue;
                }

                deliverableList.add(getDeliverName(tcUserBean.getWorkName() + "_" + actionItem));
            }
        }
        retMap.put("deliverableList",  deliverableList.stream().filter(CollectUtil.distinctByKey(str -> str)).collect(Collectors.toList()));
        retMap.put("userErrorList", userErrorList);
        return retMap;
    }


    /**
     * 更改时间表任务属性值和发送邮件
     *
     * @param savedQueryService
     * @param dmService
     * @param schedule
     * @param meetInfo
     * @param endDate
     * @throws Exception
     */
    private String updateScheduleStatus(SavedQueryService savedQueryService, DataManagementService dmService, Schedule schedule, MeetInfo meetInfo, String endDate, boolean flag) throws Exception {
        ModelObject scheduleTask = TCUtils.getPropModelObject(dmService, schedule, TCScheduleConstant.REL_FND0SUMMARYTASK);
        String meetMainOwner = meetInfo.getMeetMainOwner();
        String msg = "";
        if (StringUtil.isNotEmpty(meetMainOwner)) {
            TCUserBean tcUserBean = tcfrMapper.getTCUserInfo(meetMainOwner);
            if (null == tcUserBean) {
                msg = "会议主题为: " + meetInfo.getMeetingTitle() + ", 邮箱为: " + meetMainOwner + ", 不存在TC账号信息";
                return msg;
            }

            String mainOwner = tcUserBean.getWorkName() + "(" + tcUserBean.getWorkId() + ")";
            TCUtils.setProperties(dmService, schedule, TCScheduleConstant.PROPERTY_D9_REALAUTHOR, mainOwner); // 保存时间表对象实际用户属性值
            TCUtils.setProperties(dmService, scheduleTask, TCScheduleConstant.PROPERTY_D9_REALAUTHOR, mainOwner); // 保存时间表任务对象实际用户属性值

            String userId = tcUserBean.getTcUserId();
            ModelObject[] objects = TCUtils.executequery(savedQueryService, dmService, TCSearchEnum.WEB_FIND_USER.queryName(), TCSearchEnum.WEB_FIND_USER.queryParams(), new String[]{userId});
            User user = null;
            if (objects != null && objects.length > 0) {
                user = (User) objects[0];
                if (!TCUtils.checkObjectOwner(dmService, schedule, user)) {
                    TCUtils.changeOwnShip(dmService, schedule, user, (Group) user.get_default_group()); // 更改时间表所有权
                }

                if (!TCUtils.checkObjectOwner(dmService, scheduleTask, user)) {
                    TCUtils.changeOwnShip(dmService, scheduleTask, user, (Group) user.get_default_group()); // 更改时间表任务所有权
                }
            }

            if (flag) { // 判断是否为新创建的时间表对象，若是则需要发邮件通知给会议主责人
                sendNotifyMail(meetInfo.getMeetingTitle(), null, meetMainOwner, meetInfo.getSpasSeries(), meetInfo.getSpasProjPhase(), endDate,
                        new ArrayList<TCUserBean>() {{
                            add(tcUserBean);
                        }}, false); // 发送邮件给主责人
            }
        }
        return msg;
    }


    /**
     * 更新文档对象状态
     * @param savedQueryService
     * @param dmService
     * @param documentItemRev
     * @param meetInfo
     * @return
     * @throws Exception
     */
    private String updateDocumentStatus(ProjectLevelSecurityService projectLevelSecurityService, SavedQueryService savedQueryService, DataManagementService dmService, ItemRevision documentItemRev, MeetInfo meetInfo) throws Exception {
        String meetMainOwner = meetInfo.getMeetMainOwner();
        String meetingType = meetInfo.getMeetingType();
        String msg = "";

        if (StringUtil.isNotEmpty(meetMainOwner)) {
            if (documentItemRev != null) {

                Item document = (Item) TCUtils.getPropModelObject(dmService, documentItemRev, TCItemConstant.REL_ITEMS_TAG);

                TCUtils.setProperties(dmService, document, TCScheduleConstant.PROPERTY_OBJECT_DESC, meetingType); // 保存至文档对象描述属性值
                TCUtils.setProperties(dmService, documentItemRev, TCScheduleConstant.PROPERTY_OBJECT_DESC, meetingType); // 保存至文档对象版本描述属性值

                String projectId = "p" + meetInfo.getSpasProjId();
                Map<String, Object> queryResults = TCUtils.executeQuery(savedQueryService, TCSearchEnum.D9_FIND_PROJECT.queryName(), TCSearchEnum.D9_FIND_PROJECT.queryParams(), new String[] {projectId}); // 查询专案对象
                if (queryResults.get("succeeded") == null) {
                    msg = meetInfo.getSpasProjId() + "未查询到TC项目信息";
                }

                TC_Project projectObj = null;

                ModelObject[] objs = (ModelObject[]) queryResults.get("succeeded");
                if (objs.length > 0) {
                    projectObj = (TC_Project) objs[0];
                }

                if (ObjectUtil.isNotNull(projectObj)) {
                    ProjectUtil.assignedProject(projectLevelSecurityService, documentItemRev, projectObj); // 对象指派专案
                }
            }


            TCUserBean tcUserBean = tcfrMapper.getTCUserInfo(meetMainOwner);
            if (null == tcUserBean) {
                msg = "会议主题为: " + meetInfo.getMeetingTitle() + ", 邮箱为: " + meetMainOwner + ", 不存在TC账号信息";
                return msg;
            }

            String mainOwner = tcUserBean.getWorkName() + "(" + tcUserBean.getWorkId() + ")";
            String userId = tcUserBean.getTcUserId();
            ModelObject[] objects = TCUtils.executequery(savedQueryService, dmService, TCSearchEnum.WEB_FIND_USER.queryName(), TCSearchEnum.WEB_FIND_USER.queryParams(), new String[]{userId});
            User user = null;

            if (objects != null && objects.length > 0) {
                user = (User) objects[0];
            }

            if (documentItemRev != null) { // 执行保存实际用户和更改所有权的操作
                TCUtils.setProperties(dmService, documentItemRev, TCItemConstant.PROPERTY_D9_ACTUALUSERID, mainOwner); // 保存会议文档实际用户属性值

                Item document = (Item) TCUtils.getPropModelObject(dmService, documentItemRev, TCItemConstant.REL_ITEMS_TAG);

                if (!TCUtils.checkObjectOwner(dmService, document, user)) {
                    if(user!=null) {
                        TCUtils.changeOwnShip(dmService, document, user, (Group) user.get_default_group()); // 转移对象所有权
                    }
                }

                if (!TCUtils.checkObjectOwner(dmService, documentItemRev, user)) {
                    if(user!=null) {
                        TCUtils.changeOwnShip(dmService, documentItemRev, user, (Group) user.get_default_group()); // 转移版本所有权
                    }
                }
            }
        }

        return msg;
    }


    /**
     * 发送通知邮件
     *
     * @param scheduleName
     * @param taskName
     * @param owner
     * @param series
     * @param phase
     * @param actionItemMailFlag true代表和action item owner发送邮件， false代表和主责人发送邮件
     */
    private void sendNotifyMail(String scheduleName, String taskName, String owner, String series, String phase, String endDate,
                                List<TCUserBean> userInfoList, boolean actionItemMailFlag) {
        Map<String, String> httpmap = new HashMap<>();
        if (StringUtil.isEmpty(owner)) {
            return;
        }

        List<String> convertOwners = ScheduleTaskUtil.convertOwners(owner);
        if (CollUtil.isEmpty(convertOwners)) {
            return;
        }

        String to = "";
        String userName = "";
        String tcUserId = "";
        for (String str : convertOwners) {
            TCUserBean tcUserInfo = getTCUserInfo(userInfoList, str);
            if (null == tcUserInfo) {
                continue;
            }
            userName += tcUserInfo.getWorkId() + "(" + tcUserInfo.getWorkName() + ")" + ",";
            tcUserId += tcUserInfo.getTcUserId() + ",";
            to += str + ",";
        }

        if ("".equals(userName)) {
            return;
        }

        if ("".equals(to)) {
            return;
        }
        userName = userName.substring(0, userName.length() - 1);
        tcUserId = tcUserId.substring(0, tcUserId.length() - 1);
        to = to.substring(0, to.length() - 1);

        httpmap.put("sendTo", to);
//        httpmap.put("sendCc", "hua-sheng.yu@foxconn.com");
        httpmap.put("subject", "TCFR 时间表任务通知");
        String msg = "";
        if (actionItemMailFlag) {
            msg = "<html><head></head><body>"
                    + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "Dear " + userName + "</div><br/>"
                    + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "您有代办事项需要处理，请登陆下方Teamcenter賬號进行查看，谢谢！" + "</div>"
                    + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>Teamcenter账号：</strong>" + tcUserId + "</div>"
                    + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>时间表名称：</strong>" + scheduleName + "</div>"
                    + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>时间表任务：</strong>" + taskName + "</div>"
                    + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>Due date：</strong>" + endDate + "</div>"
                    + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>任务路径:</strong>" + "D事業群企業知識庫/專案知識庫" + "</div>"
                    + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>专案系列:</strong>" + series + "</div>"
                    + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>专案名:</strong>" + projectName + "</div>"
                    + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>专案阶段:</strong>" + phase + "</div>"
                    + "</body></html>";
        } else {
            msg = "<html><head></head><body>"
                    + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "Dear " + userName + "</div><br/>"
                    + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "本次會議時間表任務已創建完成，请登陆下方Teamcenter賬號进行查看，谢谢！" + "</div>"
                    + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>Teamcenter账号：</strong>" + tcUserId + "</div>"
                    + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>时间表名称：</strong>" + scheduleName + "</div>"
                    + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>Due date：</strong>" + endDate + "</div>"
                    + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>任务路径:</strong>" + "D事業群企業知識庫/專案知識庫" + "</div>"
                    + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>专案系列:</strong>" + series + "</div>"
                    + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>专案名:</strong>" + projectName + "</div>"
                    + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>专案阶段:</strong>" + phase + "</div>"
                    + "</body></html>";
        }
        httpmap.put("htmlmsg", msg);
        Gson gson = new Gson();
        String data = gson.toJson(httpmap);
        String result = tcMailClient.sendMail3Method(data);// 发送邮件
        log.info("==>> result: " + result);
    }


    /**
     * 根据邮箱获取TC账号
     *
     * @param userInfoList
     * @param email
     * @return
     */
    private TCUserBean getTCUserInfo(List<TCUserBean> userInfoList, String email) {
        if (CollUtil.isEmpty(userInfoList)) {
            return null;
        }

        Optional<TCUserBean> findAny = userInfoList.stream().filter(bean -> {
            if (bean.getEmail().equals(email)) {
                return true;
            }
            return false;
        }).findAny();

        if (findAny.isPresent()) {
            return findAny.get();
        }
        return null;
    }


    /**
     * 创建document文档
     *
     * @param lovService
     * @param dmService
     * @param meetInfo
     * @param folder
     * @return
     * @throws Exception
     */
    private ItemRevision createDocument(LOVService lovService, DataManagementService dmService, Folder folder, MeetInfo meetInfo) throws Exception {
        List<Map<String, String[]>> systemNameLovValues = LOVUtil.getLovValue(lovService, TCLovConstant.BONAME_D9_DOCUMENT_ID,
                TCItemConstant.PROPERTY_D9_SYSTEMNAME);
        if (CollUtil.isEmpty(systemNameLovValues)) {
            return null;
        }
        String systemName = systemNameLovValues.get(0).get("lov_values")[0];
        List<Map<String, String[]>> systemLevelLovValues = LOVUtil.getLovValue(lovService, TCLovConstant.BONAME_D9_DOCUMENT_ID,
                TCItemConstant.PROPERTY_D9_SYSTEMLEVEL);
        if (CollUtil.isEmpty(systemLevelLovValues)) {
            return null;
        }
        String systemLevel = systemLevelLovValues.get(2).get("lov_values")[0];
        List<Map<String, String[]>> productCodeLovValues = LOVUtil.getLovValue(lovService, TCLovConstant.BONAME_D9_DOCUMENT_ID,
                TCItemConstant.PROPERTY_D9_PRODUCTCODE);
        if (CollUtil.isEmpty(productCodeLovValues)) {
            return null;
        }
        String productCode = productCodeLovValues.get(7).get("object_name")[0];
        List<Map<String, String[]>> productLineLovValues = LOVUtil.getLovValue(lovService, TCLovConstant.BONAME_D9_DOCUMENT_ID,
                TCItemConstant.PROPERTY_D9_PRODUCTLINE);
        if (CollUtil.isEmpty(productLineLovValues)) {
            return null;
        }
        String productLine = productLineLovValues.get(1).get("object_name")[0];
        List<Map<String, String[]>> customerLovValues = LOVUtil.getLovValue(lovService, TCLovConstant.BONAME_D9_DOCUMENT_ID,
                TCItemConstant.PROPERTY_D9_CUSTOMER);
        if (CollUtil.isEmpty(customerLovValues)) {
            return null;
        }
        String customerName = getCustomerCode(customerLovValues, meetInfo.getCustomerName());
        List<Map<String, String[]>> documentTypeLovValues = LOVUtil.getLovValue(lovService, TCLovConstant.BONAME_DOCUMENT,
                TCItemConstant.PROPERTY_D9_DOCUMENTTYPE);
        if (CollUtil.isEmpty(documentTypeLovValues)) {
            return null;
        }

//        String documentType = documentTypeLovValues.get(documentTypeLovValues.size() - 1).get("lov_values")[0];
        String documentType = TCFRConstant.documentType;
        log.info("==>> documentType: " + documentType);

        String id = TCUtils.generateId(dmService, TCItemTypeConstant.DOCUMENT_ITEM_TYPE);
        id = id.replace(TCFRConstant.systemType + systemName + systemLevel, "");
        String docId = TCFRConstant.systemType + systemName + systemLevel + productCode + productLine + customerName + id;
        log.info("==>> docId: " + docId);

        List<Map<String, String>> list = new ArrayList<>();
        Map<String, String> propMap = new HashMap<>();
        propMap.put(TCItemConstant.PROPERTY_ITEM_ID, docId);
        propMap.put(TCItemConstant.PROPETY_ITEM_REVISION_ID, "01");
        propMap.put(TCItemConstant.PROPERTY_OBJECT_NAME, meetInfo.getMeetingTitle());
        propMap.put(TCItemConstant.PROPERTY_OBJECT_TYPE, TCItemTypeConstant.DOCUMENT_ITEM_TYPE);
        propMap.put(TCItemConstant.PROPERTY_D9_DOCUMENTTYPE, documentType);
        list.add(propMap);
        DataManagement.CreateItemsResponse response = ItemUtil.createItems(dmService, list, folder, TCFolderConstant.REL_CONTENTS);
        String result = TCUtils.getErrorMsg(response.serviceData);
        if (StrUtil.isNotEmpty(result)) {
            throw new Exception(result);
        }

        DataManagement.CreateItemsOutput[] outputs = response.output;
        for (DataManagement.CreateItemsOutput output : outputs) {
            return output.itemRev;
        }
        return null;
    }


    /**
     * 获取客户代号
     *
     * @param list
     * @param customer
     * @return
     */
    private String getCustomerCode(List<Map<String, String[]>> list, String customer) {
        Optional<Map<String, String[]>> findAny = list.stream().filter(e -> {
            if (customer.equals(e.get("object_desc")[0])) {
                return true;
            }
            return false;
        }).findAny();

        if (findAny.isPresent()) {
            return findAny.get().get("object_name")[0];
        }
        return null;
    }

    /**
     * 获取流程模板对象
     *
     * @return
     */
    private EPMTaskTemplate getWorkFlowTemplate(String str) {
        if (CollUtil.isEmpty(WORKFLOWTEMPLATEMAP)) {
            return null;
        }
        Optional<Map.Entry<EPMTaskTemplate, String>> findAny =
                WORKFLOWTEMPLATEMAP.entrySet().stream().filter(m -> m.getValue().equals(str)).findAny();
        return findAny.map(Map.Entry::getKey).orElse(null);
    }


    /**
     * 更新日期字段样式
     *
     * @param meetInfo
     */
    private void updateDateFormat(MeetInfo meetInfo) {
        String meetingStartDate = meetInfo.getMeetingStartDate();
        if (StrUtil.isNotEmpty(meetingStartDate)) {
            meetInfo.setMeetingStartDate(meetingStartDate.replaceAll("/", "-"));
        }

        List<MeetDataInfo> dataInfos = meetInfo.getData();
        for (MeetDataInfo info : dataInfos) {
            String planStartDate = info.getPlanStartDate();
            String planEndDate = info.getPlanEndDate();
            if (StrUtil.isNotEmpty(planStartDate)) {
                planStartDate = planStartDate.replaceAll("/", "-");
                info.setPlanStartDate(planStartDate);
            }

            if (StrUtil.isNotEmpty(planEndDate)) {
                planEndDate = planEndDate.replaceAll("/", "-");
                info.setPlanEndDate(planEndDate);
            }
        }
    }


    /**
     * 移除无效的字符
     * @param meetInfo
     */
    private void removeInvalidChar(MeetInfo meetInfo) {
        meetInfo.setMeetingTitle(StringUtil.replaceBlank(meetInfo.getMeetingTitle()));
        List<MeetDataInfo> dataInfos = meetInfo.getData();
        for (MeetDataInfo info : dataInfos) {
            info.setActionItem(StringUtil.replaceBlank(info.getActionItem()));
            info.setActionItemId(StringUtil.replaceBlank(info.getActionItemId()));
            info.setOwners(StringUtil.replaceBlank(info.getOwners()));
            info.setPlanStartDate(StringUtil.replaceBlank(info.getPlanStartDate()));
            info.setPlanEndDate(StringUtil.replaceBlank(info.getPlanEndDate()));
        }
    }


    /**
     * 获取交付件名称
     * @param str
     * @return
     */
    public static String getDeliverName(String str) {
        if (StringUtil.isEmpty(str)) {
            return null;
        }

        StringBuffer buffer = new StringBuffer();
        int valueLength = 0;
        String chinese = "[\u0391-\uFFE5]";
        for (int i = 0; i < str.length(); i++) {
            String temp = str.substring(i,i + 1);
            buffer.append(temp);
            if (temp.matches(chinese)) {
                valueLength += 3;
            } else {
                valueLength += 1;
            }

            if (valueLength > TCFRConstant.DELIVERABLENAME) {
                break;
            }
        }

        return buffer.toString();
    }


    /**
     * 获取工作评估时长
     * @param day
     * @return
     */
    public int getWorkEstimate(long day) {
        if (day == 0) {
            return (int) (8 * 60);
        } else {
            return (int) ((day + 1) * 8 *60);
        }
    }

}
