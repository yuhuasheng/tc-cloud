package com.foxconn.plm.utils.tc;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.foxconn.plm.entity.constants.*;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.projectmanagement.ScheduleManagementService;
import com.teamcenter.services.strong.projectmanagement._2015_07.ScheduleManagement;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.exceptions.NotLoadedException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author HuashengYu
 * @Date 2023/3/7 16:48
 * @Version 1.0
 */
public class ScheduleTaskUtil {

    /**
     * 创建时间表对象
     *
     * @param scheduleManagementService 时间表服务类
     * @param dmService                 工具类
     * @param objectType                类型
     * @param objectName                名称
     * @param id                        itemId
     * @param startDate                 开始时间
     * @param finishDate                结束时间
     * @param folder                    创建好挂载在那个文件夹下
     * @return
     * @throws Exception
     */
    public static Schedule[] createSchedule(ScheduleManagementService scheduleManagementService, DataManagementService dmService, String objectType, String objectName, String id, Calendar startDate, Calendar finishDate, ModelObject folder) throws Exception {
        com.teamcenter.services.strong.projectmanagement._2008_06.ScheduleManagement.NewScheduleContainer[] newScheduleContainers = new com.teamcenter.services.strong.projectmanagement._2008_06.ScheduleManagement.NewScheduleContainer[1];
        newScheduleContainers[0] = new com.teamcenter.services.strong.projectmanagement._2008_06.ScheduleManagement.NewScheduleContainer();
        newScheduleContainers[0].name = objectName;
        newScheduleContainers[0].type = objectType;
        newScheduleContainers[0].taskFixedType = 0;
        newScheduleContainers[0].status = 0;
        newScheduleContainers[0].published = true;
        newScheduleContainers[0].priority = 3;
        newScheduleContainers[0].percentLinked = false;
        newScheduleContainers[0].notificationsEnabled = true;
        newScheduleContainers[0].isTemplate = false;
        newScheduleContainers[0].isPublic = false;
        newScheduleContainers[0].id = id;
        newScheduleContainers[0].startDate = startDate;
        newScheduleContainers[0].finishDate = finishDate;

        com.teamcenter.services.strong.projectmanagement._2008_06.ScheduleManagement.StringValContainer[] stringValueContainers = new com.teamcenter.services.strong.projectmanagement._2008_06.ScheduleManagement.StringValContainer[6];

        stringValueContainers[0] = new com.teamcenter.services.strong.projectmanagement._2008_06.ScheduleManagement.StringValContainer();
        stringValueContainers[0].key = "dates_linked";
        stringValueContainers[0].type = 5;
        stringValueContainers[0].value = "false";

        stringValueContainers[1] = new com.teamcenter.services.strong.projectmanagement._2008_06.ScheduleManagement.StringValContainer();
        stringValueContainers[1].key = "end_date_scheduling";
        stringValueContainers[1].type = 5;
        stringValueContainers[1].value = "false";

        stringValueContainers[2] = new com.teamcenter.services.strong.projectmanagement._2008_06.ScheduleManagement.StringValContainer();
        stringValueContainers[2].key = "fnd0allowExecUpdates";
        stringValueContainers[2].type = 5;
        stringValueContainers[2].value = "false";

        stringValueContainers[3] = new com.teamcenter.services.strong.projectmanagement._2008_06.ScheduleManagement.StringValContainer();
        stringValueContainers[3].key = "wbsformat";
        stringValueContainers[3].type = 7;
        stringValueContainers[3].value = "N.N";

        stringValueContainers[4] = new com.teamcenter.services.strong.projectmanagement._2008_06.ScheduleManagement.StringValContainer();
        stringValueContainers[4].key = "wbsvalue";
        stringValueContainers[4].type = 7;
        stringValueContainers[4].value = "1";

        stringValueContainers[5] = new com.teamcenter.services.strong.projectmanagement._2008_06.ScheduleManagement.StringValContainer();
        stringValueContainers[5].key = "fnd0TimeZone";
        stringValueContainers[5].type = 7;
        stringValueContainers[5].value = "Asia/Shanghai";

        newScheduleContainers[0].stringValueContainer = stringValueContainers;
        com.teamcenter.services.strong.projectmanagement._2008_06.ScheduleManagement.CreateScheduleResponse response = scheduleManagementService.createSchedule(newScheduleContainers);
        String result = TCUtils.getErrorMsg(response.serviceData);
        if (StrUtil.isNotEmpty(result)) {
            throw new Exception(result);
        }
        Schedule[] schedules = response.schedules;
        if (ObjUtil.isNotEmpty(folder)) {
            TCUtils.addContents(dmService, folder, schedules); // 将创建出来的时间表对象添加到主对象folder的content关系下
        }
        return schedules;
    }

    /**
     * 递归遍历时间表任务对象
     *
     * @param dmService         工具类
     * @param rootTask          根时间表任务对象
     * @param parentTaskUid     父时间表任务uid
     * @param prevSibingTaskUid 上一个时间表任务uid
     * @throws Exception
     */
    public static Map<ScheduleTask, Boolean> generateScheduleTask(ScheduleManagementService scheduleService, DataManagementService dmService, Schedule schedule, ScheduleTask rootTask,String parentTaskUid, String prevSibingTaskUid, String taskName,
                                            Calendar start, Calendar finish, Integer workEstimate, String objectType, String[] propNames, String[] propValues, List<User> userList, EPMTaskTemplate epmTaskTemplate,
                                            Map<String, ModelObject> scheduleDeliverMap, List<String> scheduleTaskDeliverList, List<String> spasUserInfo, int submitType, String startDate, String finishDate, Map<ScheduleTask, Boolean> map) throws Exception {
        if (CollUtil.isEmpty(map)) {
            map = new LinkedHashMap<>();
        }
        String rootTaskName = TCUtils.getPropStr(dmService, rootTask, TCScheduleConstant.PROPERTY_OBJECT_NAME);
        System.out.println("==>> rootTaskName: " + rootTaskName);
        TCUtils.refreshObject(dmService, rootTask);
        ModelObject[] children = TCUtils.getPropModelObjectArray(dmService, rootTask, TCScheduleConstant.REL_CHILD_TASK_TAGLIST);
        ScheduleTask scheduleTask = null;
        if (parentTaskUid.equals(rootTask.getUid())) {
            if (ArrayUtil.isEmpty(children) || !checkScheduleTaskExist(scheduleService, dmService, schedule, rootTask, taskName, objectType, userList, epmTaskTemplate, scheduleDeliverMap, scheduleTaskDeliverList, spasUserInfo, submitType, startDate, finishDate)) { // 子任务为空或者子任务对象没有添加过
                scheduleTask = createScheduleTask(scheduleService, dmService, schedule, rootTask, null, taskName, start, finish, workEstimate, objectType, propNames, propValues, userList, epmTaskTemplate, scheduleDeliverMap, scheduleTaskDeliverList, spasUserInfo, submitType, startDate, finishDate);
                map.put(scheduleTask, true);
            } else {
                if (StrUtil.isNotEmpty(prevSibingTaskUid)) {
                    List<ModelObject> list = Stream.of(children).collect(Collectors.toList());
                    Optional<ModelObject> findAny = list.stream().filter(obj -> obj.getUid().equals(prevSibingTaskUid)).findAny();
                    if (findAny.isPresent()) {
                        if (!checkScheduleTaskExist(scheduleService, dmService, schedule, rootTask, taskName, objectType, userList, epmTaskTemplate, scheduleDeliverMap, scheduleTaskDeliverList, spasUserInfo, submitType, startDate, finishDate)) {
                            scheduleTask = createScheduleTask(scheduleService, dmService, schedule, rootTask, (ScheduleTask) findAny.get(), taskName, start, finish, workEstimate, objectType, propNames, propValues, userList, epmTaskTemplate, scheduleDeliverMap, scheduleTaskDeliverList, spasUserInfo, submitType, startDate, finishDate);
                            map.put(scheduleTask, true);
                        }
                    }
                }
            }
        } else {
            if (ArrayUtil.isNotEmpty(children)) {
                for (ModelObject child : children) {
                    generateScheduleTask(scheduleService, dmService, schedule, (ScheduleTask) child, parentTaskUid, prevSibingTaskUid, taskName, start, finish, workEstimate, objectType, propNames, propValues, userList, epmTaskTemplate, scheduleDeliverMap, scheduleTaskDeliverList, spasUserInfo, submitType, startDate, finishDate, map);
                }
            }
        }
        return map;
    }


    /**
     * 创建时间表任务
     *
     * @param scheduleService 时间表服务类
     * @param schedule        时间表对象
     * @param parentTask      父时间表任务对象
     * @param prevSibingTask  上一个结点时间表任务对象
     * @param taskName        节点名
     * @param start           开始时间
     * @param finish          结束时间
     * @param workEstimate    工作评估时间
     * @param objectType      类型
     * @return
     * @throws Exception
     */
    public static ScheduleTask createScheduleTask(ScheduleManagementService scheduleService, DataManagementService dmService, Schedule schedule, ScheduleTask parentTask, ScheduleTask prevSibingTask, String taskName,
                                                  Calendar start, Calendar finish, Integer workEstimate, String objectType, String[] propNames, String[] propValues, List<User> userList, EPMTaskTemplate epmTaskTemplate,
                                                  Map<String, ModelObject> scheduleDeliverMap, List<String> scheduleTaskDeliverList, List<String> spasUserInfo, int submitType,  String startDate, String finishDate) throws Exception {
        com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.TaskCreateContainer[] taskCreateContainers = new com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.
                TaskCreateContainer[1];
        taskCreateContainers[0] = new com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.TaskCreateContainer();
        taskCreateContainers[0].parent = parentTask;
        taskCreateContainers[0].prevSibling = prevSibingTask;
        taskCreateContainers[0].name = taskName;
        taskCreateContainers[0].objectType = objectType;
        taskCreateContainers[0].start = start;
        taskCreateContainers[0].finish = finish;
        if (workEstimate == null) {
            taskCreateContainers[0].workEstimate = 0;
        } else {
            taskCreateContainers[0].workEstimate = workEstimate;
        }

        com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.AttributeUpdateContainer[] otherAttributes = new com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.AttributeUpdateContainer[5];
        otherAttributes[0] = new com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.AttributeUpdateContainer();
        otherAttributes[0].attrName = "fixed_type";
        otherAttributes[0].attrType = 2;
        otherAttributes[0].attrValue = "1";

        otherAttributes[1] = new com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.AttributeUpdateContainer();
        otherAttributes[1].attrName = "item_id";
        otherAttributes[1].attrType = 1;
        otherAttributes[1].attrValue = "";

        otherAttributes[2] = new com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.AttributeUpdateContainer();
        otherAttributes[2].attrName = "work_complete";
        otherAttributes[2].attrType = 2;
        otherAttributes[2].attrValue = "0";

        otherAttributes[3] = new com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.AttributeUpdateContainer();
        otherAttributes[3].attrName = "duration";
        otherAttributes[3].attrType = 2;
        otherAttributes[3].attrValue = "-1";

        otherAttributes[4] = new com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.AttributeUpdateContainer();
        otherAttributes[4].attrName = "priority";
        otherAttributes[4].attrType = 2;
        otherAttributes[4].attrValue = "3";

        taskCreateContainers[0].otherAttributes = otherAttributes;

        com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.CreatedObjectsContainer response = scheduleService.createTasks(schedule, taskCreateContainers);
        String result = TCUtils.getErrorMsg(response.serviceData);
        if (StrUtil.isNotEmpty(result)) {
            throw new Exception(result);
        }

        POM_object[] createdObjects = response.createdObjects;
        ScheduleTask task = (ScheduleTask) createdObjects[0];
        if (CollUtil.isNotEmpty(userList)) {
            checkAssignScheduleTask(scheduleService, dmService, schedule, task, userList, epmTaskTemplate, scheduleDeliverMap, scheduleTaskDeliverList, spasUserInfo, submitType, startDate, finishDate);
//            for (User user : userList) {
//                if (!TCUtils.checkObjectOwner(dmService, task, user)) {
//                    TCUtils.changeOwnShip(dmService, task, user, (Group) user.get_default_group()); // 转移所有权
//                }
//                createScheduleTaskDeliverableList(scheduleService, task, dmService, scheduleDeliverMap, scheduleTaskDeliverList, submitType); // 指派时间表任务交付件
//                assignWorkFlowTemplate(scheduleService, schedule, task, epmTaskTemplate);
//                updateScheduleDate(scheduleService, schedule, task, startDate, finishDate);
//                assignScheduleTask(scheduleService, schedule, task, user); // 时间表任务指派用户
//                startScheduleTaskWorkFlow(scheduleService, task); // 启动时间表任务流程
//            }
        }
        if (propValues != null && propValues.length > 0) {
            TCUtils.setProperties(dmService, new ModelObject[]{task}, propNames, propValues);

        }
        return task;
    }


    /**
     * 更新时间表任务开始和结束日期
     * @param scheduleService
     * @param schedule
     * @param task
     * @param start
     * @param finish
     * @throws Exception
     */
    private static void updateScheduleDate(ScheduleManagementService scheduleService,  Schedule schedule, ScheduleTask task, String start, String finish) throws Exception {
        com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.ObjectUpdateContainer[] objectUpdateContainers = new com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.ObjectUpdateContainer[1];
        objectUpdateContainers[0] = new com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.ObjectUpdateContainer();
        objectUpdateContainers[0].object = task;

        com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.AttributeUpdateContainer[] attributeUpdateContainers = new com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.AttributeUpdateContainer[2];
        attributeUpdateContainers[0] = new com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.AttributeUpdateContainer();
        attributeUpdateContainers[0].attrName = "start_date";
        attributeUpdateContainers[0].attrType = 8;
        attributeUpdateContainers[0].attrValue = start;

        attributeUpdateContainers[1] = new com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.AttributeUpdateContainer();
        attributeUpdateContainers[1].attrName = "finish_date";
        attributeUpdateContainers[1].attrType = 8;
        attributeUpdateContainers[1].attrValue = finish;

        objectUpdateContainers[0].updates = attributeUpdateContainers;

        ServiceData data = scheduleService.updateTasks(schedule, objectUpdateContainers);

        String result = TCUtils.getErrorMsg(data);
        if (StrUtil.isNotEmpty(result)) {
            throw new Exception(result);
        }
    }


    /**
     * 判断时间表任务对象是否存在
     *
     * @param dmService  工具类
     * @param parentTask 父任务对象
     * @param taskName   子任务对象名称
     * @param objectType
     * @return
     * @throws NotLoadedException
     */
    private static boolean checkScheduleTaskExist(ScheduleManagementService scheduleService, DataManagementService dmService, Schedule schedule, ScheduleTask parentTask, String taskName, String objectType, List<User> userList,
                                                  EPMTaskTemplate epmTaskTemplate, Map<String, ModelObject> scheduleDeliverMap, List<String> scheduleTaskDeliverList, List<String> spasUserInfo, int submitType, String startDate, String finishDate) throws Exception {
        ModelObject[] children = TCUtils.getPropModelObjectArray(dmService, parentTask, TCScheduleConstant.REL_CHILD_TASK_TAGLIST);
        ModelObject object = TCUtils.checkModelObjExist(dmService, children, taskName, objectType, TCFolderConstant.PROPERTY_OBJECT_NAME);
        if (null == object) {
            return false;
        }

        checkAssignScheduleTask(scheduleService, dmService, schedule, (ScheduleTask) object, userList, epmTaskTemplate, scheduleDeliverMap, scheduleTaskDeliverList, spasUserInfo, submitType, startDate, finishDate); // 核对任务是否指派所有者
        return true;
    }

    /**
     * 核对任务是否指派所有者
     *
     * @param scheduleService
     * @param dmService
     * @param schedule
     * @param scheduleTask
     * @param list
     * @throws Exception
     */
    private static void checkAssignScheduleTask(ScheduleManagementService scheduleService, DataManagementService dmService, Schedule schedule, ScheduleTask scheduleTask, List<User> list, EPMTaskTemplate epmTaskTemplate, Map<String, ModelObject> scheduleDeliverMap, List<String> scheduleTaskDeliverList, List<String> spasUserInfo, int submitType, String startDate, String finishDate) throws Exception {
        if (CollUtil.isEmpty(list)) {
            return;
        }

        TCUtils.refreshObject(dmService, scheduleTask);
        ModelObject[] assignUsers = TCUtils.getPropModelObjectArray(dmService, scheduleTask, TCScheduleConstant.REL_RESOURCEASSIGNMENT);
        for (User user : list) {
            checkAssignWorkFlowTemplate(scheduleService, dmService, schedule, scheduleTask, epmTaskTemplate); // 核对是否已经指派流程模板
            if (CollUtil.isNotEmpty(spasUserInfo)) {
                String actuserId = spasUserInfo.stream().collect(Collectors.joining(";"));
                TCUtils.setProperties(dmService, scheduleTask, TCScheduleConstant.PROPERTY_D9_REALAUTHOR, actuserId); // 设置时间表任务实际用户
            }

            if (!checkAssignUser(user, assignUsers)) { // 判断用户是否已经指派
                if (!TCUtils.checkObjectOwner(dmService, scheduleTask, user)) {
                    TCUtils.changeOwnShip(dmService, scheduleTask, user, (Group) user.get_default_group()); // 转移所有权
                }
                assignScheduleTask(scheduleService, schedule, scheduleTask, user); // 指派任务所有者
            }
//            updateScheduleDate(scheduleService, schedule, scheduleTask, startDate, finishDate); // 更新开始和结束日期
            boolean flag = createScheduleTaskDeliverableList(scheduleService, scheduleTask, dmService, scheduleDeliverMap, scheduleTaskDeliverList, submitType);// 指定时间表任务交付件
            if (!flag) { // 假如交付件不存在话，则无需启动流程
                return;
            }
            checkScheduleTaskWorkFlow(scheduleService, dmService, scheduleTask); // 校验时间表任务是否已经起草流程
        }
    }

    /**
     * 核对是否已经指派流程模板
     *
     * @param scheduleService
     * @param dmService
     * @param schedule
     * @param scheduleTask
     * @param epmTaskTemplate
     * @throws Exception
     */
    private static void checkAssignWorkFlowTemplate(ScheduleManagementService scheduleService, DataManagementService dmService, Schedule schedule, ScheduleTask scheduleTask, EPMTaskTemplate epmTaskTemplate) throws Exception {
        TCUtils.refreshObject(dmService, scheduleTask);
        ModelObject workFlowTemplate = TCUtils.getPropModelObject(dmService, scheduleTask, TCEPMTaskConstant.REL_WORKFLOW_TEMPLATE); // 判断时间表任务是否已经指派流程模板
        if (workFlowTemplate != null) {
            return;
        }

        assignWorkFlowTemplate(scheduleService, schedule, scheduleTask, epmTaskTemplate); // 给时间表任务指派流程模板
    }


    /**
     * 创建时间表交付件
     *
     * @param scheduleService
     * @param dmService
     * @param schedule
     * @param deliverableNameList
     * @param deliverableType
     * @throws Exception
     */
    public static Map<String, ModelObject> createScheduleDeliverableList(ScheduleManagementService scheduleService, DataManagementService dmService, Schedule schedule, List<String> deliverableNameList, String deliverableType) throws Exception {
        if (CollUtil.isEmpty(deliverableNameList)) {
            return null;
        }
        TCUtils.refreshObject(dmService, new ModelObject[]{schedule});
        ModelObject[] deliverableObjs = TCUtils.getPropModelObjectArray(dmService, schedule, TCScheduleConstant.REL_SCHEDULE_DELIVERABLE_LIST);
        TCUtils.refreshObject(dmService, deliverableObjs);
        for (Iterator it = deliverableNameList.iterator(); it.hasNext(); ) {
            String deliverableName = (String) it.next();
            if (ArrayUtil.isNotEmpty(deliverableObjs)) {
                boolean anyMatch = Stream.of(deliverableObjs).anyMatch(obj -> {
                    try {
                        if (obj instanceof SchDeliverable) {
                            SchDeliverable schDeliverable = (SchDeliverable) obj;
                            String str1 = TCUtils.getPropStr(dmService, schDeliverable, TCSchDeliverableConstant.PROPERTY_DELIVERABLE_NAME);
                            String str2 = TCUtils.getPropStr(dmService, schDeliverable, TCSchDeliverableConstant.PROPERTY_DELIVERABLE_TYPE);
                            if (deliverableName.equals(str1) && deliverableType.equals(str2)) {
                                return true;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return false;
                });
                if (anyMatch) {
                    it.remove();
                }
            }
        }

        Map<String, ModelObject> retMap = null;
        if (CollUtil.isNotEmpty(deliverableNameList)) {
            retMap = assignScheduleDeliverable(scheduleService, schedule, dmService, deliverableNameList, deliverableType);
        }
        return retMap;
    }


    /**
     * 创建时间表任务交付件
     * @param scheduleService
     * @param scheduleTask
     * @param dmService
     * @param scheduleDeliverMap
     * @param scheduleTaskDeliverList
     * @param submitType
     * return 代表是不存在交付件，true代表设置交付件成功，或者说已经设置过交付件
     * @throws Exception
     */
    public static boolean createScheduleTaskDeliverableList(ScheduleManagementService scheduleService, ScheduleTask scheduleTask, DataManagementService dmService, Map<String, ModelObject> scheduleDeliverMap, List<String> scheduleTaskDeliverList, int submitType) throws Exception {
        if (CollUtil.isEmpty(scheduleTaskDeliverList)) {
            return false;
        }

        if (CollUtil.isEmpty(scheduleDeliverMap)) {
            return false;
        }

        TCUtils.refreshObject(dmService, new ModelObject[] {scheduleTask});
        ModelObject[] taskDeliverableObjs = TCUtils.getPropModelObjectArray(dmService, scheduleTask, TCScheduleConstant.REL_SCH_TASK_DELIVERABLE_LIST);
        TCUtils.refreshObject(dmService, taskDeliverableObjs);
        List<ModelObject> resultList = new ArrayList<>();
        for (String str : scheduleTaskDeliverList) {
            Optional<Map.Entry<String, ModelObject>> findAny = scheduleDeliverMap.entrySet().stream().filter(m -> m.getKey().equals(str)).findAny(); // 判断是否在时间表交付件池中
            if (findAny.isPresent()) {
                if (ArrayUtil.isEmpty(taskDeliverableObjs)) {
                    resultList.add(findAny.get().getValue());
                } else {
                    boolean anyMatch = Stream.of(taskDeliverableObjs).anyMatch(obj -> { // 判断此时间表任务交付件是否已经添加
                        try {
                            if (obj instanceof SchTaskDeliverable) {
                                SchTaskDeliverable schTaskDeliverable = (SchTaskDeliverable) obj;
                                String objectName = TCUtils.getPropStr(dmService, schTaskDeliverable, TCSchTaskDeliverableConstant.PROPERTY_OBJECT_NAME);
                                if (str.equals(objectName)) {
                                    return true;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return false;
                    });
                    if (!anyMatch) {
                        resultList.add(findAny.get().getValue());
                    }
                }
            }
        }

        if (CollUtil.isNotEmpty(resultList)) {
            assignScheduleTaskDeliverable(scheduleService, scheduleTask, resultList, submitType); // 指派时间表任务交付件
        }

        return true;
    }


    /**
     * 校验时间表任务是否已经起草流程
     *
     * @param scheduleService
     * @param dmService
     * @param scheduleTask
     * @throws Exception
     */
    private static void checkScheduleTaskWorkFlow(ScheduleManagementService scheduleService, DataManagementService dmService, ScheduleTask scheduleTask) throws Exception {
        TCUtils.refreshObject(dmService, scheduleTask);
        ModelObject workflowProcess = TCUtils.getPropModelObject(dmService, scheduleTask, TCEPMTaskConstant.REL_WORKFLOW_PROCESS);// 判断时间表任务是否已经起草流程任务
        if (workflowProcess != null) {
            return;
        }
        startScheduleTaskWorkFlow(scheduleService, scheduleTask); // 启动时间表任务流程
    }


    /**
     * 判断指派的用户是否已经存在
     *
     * @param user
     * @param assignUsers
     * @return
     */
    private static boolean checkAssignUser(User user, ModelObject[] assignUsers) {
        if (assignUsers == null || assignUsers.length <= 0) {
            return false;
        }
        boolean anyMatch = Stream.of(assignUsers).anyMatch(obj -> {
            return obj.getUid().equals(user.getUid());
        });
        return anyMatch;
    }

    /**
     * 时间表任务对象指派人员
     *
     * @param scheduleService 时间表服务类
     * @param schedule        时间表对象
     * @param scheduleTask    时间表任务对象
     * @param user            用户
     * @return true代表指派成功，false代表指派失败
     */
    public static Boolean assignScheduleTask(ScheduleManagementService scheduleService, Schedule schedule, ScheduleTask scheduleTask, User user) throws Exception {
        ScheduleManagement.AssignmentCreateContainer[] assignmentCreateContainers = new ScheduleManagement.AssignmentCreateContainer[1];
        assignmentCreateContainers[0] = new ScheduleManagement.AssignmentCreateContainer();
        assignmentCreateContainers[0].task = scheduleTask;
        assignmentCreateContainers[0].resource = user;
        assignmentCreateContainers[0].isPlaceHolder = false;
        assignmentCreateContainers[0].assignedPercent = 100.0;
        com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.CreatedObjectsContainer response = scheduleService.assignResources(schedule, assignmentCreateContainers);
        String result = TCUtils.getErrorMsg(response.serviceData);
        if (StrUtil.isNotEmpty(result)) {
            throw new Exception(result);
        }
        return true;
    }


    /**
     * 给时间表任务指派流程模板
     *
     * @param scheduleService
     * @param schedule
     * @param scheduleTask
     * @param workFlowTemplate
     * @return
     * @throws Exception
     */
    public static Boolean assignWorkFlowTemplate(ScheduleManagementService scheduleService, Schedule schedule, ScheduleTask scheduleTask, EPMTaskTemplate workFlowTemplate) throws Exception {
        if (null == workFlowTemplate) {
            return false;
        }
        com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.ObjectUpdateContainer[] objectUpdateContainers = new com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.ObjectUpdateContainer[1];
        objectUpdateContainers[0] = new com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.ObjectUpdateContainer();
        objectUpdateContainers[0].object = scheduleTask;

        com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.AttributeUpdateContainer[] attributeUpdateContainers = new com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.AttributeUpdateContainer[3];
        attributeUpdateContainers[0] = new com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.AttributeUpdateContainer();
        attributeUpdateContainers[0].attrName = TCEPMTaskConstant.REL_WORKFLOW_TEMPLATE;
        attributeUpdateContainers[0].attrType = 1;
        attributeUpdateContainers[0].attrValue = workFlowTemplate.getUid();

        attributeUpdateContainers[1] = new com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.AttributeUpdateContainer();
        attributeUpdateContainers[1].attrName = TCEPMTaskConstant.PROPERTY_WORKFLOW_TRIGGER_TYPE;
        attributeUpdateContainers[1].attrType = 2;
        attributeUpdateContainers[1].attrValue = "0";

        attributeUpdateContainers[2] = new com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.AttributeUpdateContainer();
        attributeUpdateContainers[2].attrName = TCEPMTaskConstant.REL_PRIVILEGED_USER;
        attributeUpdateContainers[2].attrType = 0;
        attributeUpdateContainers[2].attrValue = "";

        objectUpdateContainers[0].updates = attributeUpdateContainers;
        ServiceData serviceData = scheduleService.updateTasks(schedule, objectUpdateContainers);
        String result = TCUtils.getErrorMsg(serviceData);
        if (StrUtil.isNotEmpty(result)) {
            throw new Exception(result);
        }
        return true;
    }

    /**
     * 启动时间表任务流程
     *
     * @param scheduleService
     * @param scheduleTask
     * @return
     * @throws Exception
     */
    private static Boolean startScheduleTaskWorkFlow(ScheduleManagementService scheduleService, ScheduleTask scheduleTask) throws Exception {
        com.teamcenter.services.strong.projectmanagement._2012_02.ScheduleManagement.LaunchedWorkflowContainer response = scheduleService.launchScheduledWorkflow(new ScheduleTask[]{scheduleTask});
        ServiceData serviceData = response.serviceData;
        String result = TCUtils.getErrorMsg(serviceData);
        if (StrUtil.isNotEmpty(result)) {
            throw new Exception(result);
        }
        return true;
    }

    /**
     * 指派时间表交付件
     *
     * @param scheduleService         时间表服务类
     * @param schedule                时间表对象
     * @param scheduleDeliverableList 交付件名称集合
     * @param deliverableType         交付件类型
     * @return
     * @throws Exception
     */
    private static Map<String, ModelObject> assignScheduleDeliverable(ScheduleManagementService scheduleService, Schedule schedule, DataManagementService dmService, List<String> scheduleDeliverableList, String deliverableType) throws Exception {
        Map<String, ModelObject> map = new LinkedHashMap<>();
        com.teamcenter.services.strong.projectmanagement._2008_06.ScheduleManagement.ScheduleDeliverableData[] scheduleDeliverableDatas = new com.teamcenter.services.strong.projectmanagement._2008_06.ScheduleManagement.ScheduleDeliverableData[scheduleDeliverableList.size()];
        for (int i = 0; i < scheduleDeliverableList.size(); i++) {
            scheduleDeliverableDatas[i] = new com.teamcenter.services.strong.projectmanagement._2008_06.ScheduleManagement.ScheduleDeliverableData();
            scheduleDeliverableDatas[i].schedule = schedule;
            scheduleDeliverableDatas[i].deliverableName = scheduleDeliverableList.get(i);
            scheduleDeliverableDatas[i].deliverableType = deliverableType;
        }
        ServiceData serviceData = scheduleService.createScheduleDeliverableTemplates(scheduleDeliverableDatas);
        String result = TCUtils.getErrorMsg(serviceData);
        if (StrUtil.isNotEmpty(result)) {
            throw new Exception(result);
        }

        int size = serviceData.sizeOfCreatedObjects();
        for (int j = 0; j < size; j++) {
            ModelObject object = serviceData.getCreatedObject(j);
            String deliverableName = TCUtils.getPropStr(dmService, object, TCSchDeliverableConstant.PROPERTY_OBJECT_NAME);
            map.put(deliverableName, object);
        }
        return map;
    }


    /**
     * 创建时间表任务交付件
     *
     * @param scheduleService
     * @param scheduleTask
     * @param scheduleTaskDeliverableList
     * @param submitType 0代表目标，1代表引用，3代表不提交
     * @return
     * @throws Exception
     */
    private static Boolean assignScheduleTaskDeliverable(ScheduleManagementService scheduleService, ScheduleTask scheduleTask, List<ModelObject> scheduleTaskDeliverableList, int submitType) throws Exception {
        com.teamcenter.services.strong.projectmanagement._2007_06.ScheduleManagement.TaskDeliverableContainer[] taskDeliverableContainers = new com.teamcenter.services.strong.projectmanagement._2007_06.ScheduleManagement.TaskDeliverableContainer[scheduleTaskDeliverableList.size()];
        for (int i = 0; i < scheduleTaskDeliverableList.size(); i++) {
            taskDeliverableContainers[i] = new com.teamcenter.services.strong.projectmanagement._2007_06.ScheduleManagement.TaskDeliverableContainer();
            taskDeliverableContainers[i].scheduleTask = scheduleTask;
            taskDeliverableContainers[i].scheduleDeliverable = scheduleTaskDeliverableList.get(i);
            taskDeliverableContainers[i].submitType = submitType;
        }
        ServiceData serviceData = scheduleService.createTaskDeliverableTemplates(taskDeliverableContainers);
        String result = TCUtils.getErrorMsg(serviceData);
        if (StrUtil.isNotEmpty(result)) {
            throw new Exception(result);
        }
        return true;
    }


    public static List<String> convertOwners(String owner) {
        String[] split = owner.split(";");
        List<String> list = Convert.convert(new TypeReference<List<String>>() {
        }, split);
        list.removeIf(str -> str == null || "".equals(str) || "N/A".equals(str));
        if (CollUtil.isEmpty(list)) {
            return null;
        }
        return list;
    }
}
