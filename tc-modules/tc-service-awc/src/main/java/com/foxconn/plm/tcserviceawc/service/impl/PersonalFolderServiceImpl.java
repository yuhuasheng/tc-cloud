package com.foxconn.plm.tcserviceawc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.pojo.ActualUserPojo;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcserviceawc.param.PersonalFolderParam;
import com.foxconn.plm.tcserviceawc.param.TaskUidsParam;
import com.foxconn.plm.tcserviceawc.service.PersonalFolderService;
import com.foxconn.plm.utils.tc.*;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.exceptions.NotLoadedException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 個人工作區業務邏輯接口實現類
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/23 17:08
 **/
@Service
public class PersonalFolderServiceImpl implements PersonalFolderService {
    @Resource
    private TCSOAServiceFactory tcsoaServiceFactory;


    @Override
    public String getPersonalFolderUid(PersonalFolderParam param) {
        // 查詢個人工作區
        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS4);
        try{
            String folderName = null;
            Map<String, Object> actualUsers = QueryUtil.executeQuery(tcsoaServiceFactory.getSavedQueryService(), "__D9_Find_Actual_User",
                    new String[]{"item_id"}, new String[]{param.getEmpNo()});
            if(ObjectUtil.isNotNull(actualUsers.get("succeeded"))){
                ModelObject[] md = (ModelObject[]) actualUsers.get("succeeded");
                if(md != null && md.length > 0){
                    ModelObject object =  md[0];
                    DataManagementUtil.getProperties(tcsoaServiceFactory.getDataManagementService(), object, new String[]{"item_id","object_name"});
                    tcsoaServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{object});
                    String itemId = object.getPropertyObject("item_id").getStringValue();
                    String objectName = object.getPropertyObject("object_name").getStringValue();
                    folderName = objectName + "(" + itemId + ")";
                }
            }
            if(StrUtil.isNotBlank(folderName)){
                Map<String, Object> personalFolderMap = QueryUtil.executeQuery(tcsoaServiceFactory.getSavedQueryService(), "General...",
                        new String[]{"object_type","object_name"}, new String[]{"D9_UserFolder" , folderName});
                Folder homeFolder = null;
                if(ObjectUtil.isNotNull(personalFolderMap.get("succeeded"))){
                    ModelObject[] md = (ModelObject[]) personalFolderMap.get("succeeded");
                    if(md != null && md.length > 0){
                        homeFolder =  (Folder) md[0];
                    }else{
                        homeFolder = FolderUtil.createFolder(tcsoaServiceFactory.getDataManagementService(),"D9_UserFolder",folderName,null);
                    }
                    // 處理部門文件夾
                    String[] list = PreferencesUtil.getTCPreferences(tcsoaServiceFactory.getPreferenceManagementService(), "D9_SU_Function_Folder_Mapping");
                    Map<String,String> map = new HashMap<String, String>();
                    for (String item : list) {
                        List<String> split = StrSplitter.split(item, ":",true,true);
                        if(split.size() == 2) {
                            map.put(split.get(0).substring(1,split.get(0).length()-1), split.get(1).substring(1,split.get(1).length()-1));
                        }
                    }
                    if(StrUtil.isNotBlank(map.get(param.getDept()))){
                        Set<String> set= new HashSet<>();
                        DataManagementUtil.getProperty(tcsoaServiceFactory.getDataManagementService(),homeFolder,"contents");
                        tcsoaServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{homeFolder});
                        WorkspaceObject[] contents = homeFolder.get_contents();
                        if(contents.length > 0) {
                            for (int i = 0; i < contents.length; i++) {
                                set.add(contents[i].getUid());
                            }
                        }
                        if("DT+RD+ME".equals(param.getDept()) || "DT+RD+ME PM".equals(param.getDept())) {
                            if(!set.contains(map.get("DT+RD+ME"))) {
                                ModelObject meFolder = DataManagementUtil.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), map.get("DT+RD+ME"));
                                DataManagementUtil.addRelation(tcsoaServiceFactory.getDataManagementService(),homeFolder,meFolder,"contents");
                            }
                            if(!set.contains(map.get("DT+RD+ME PM"))) {
                                ModelObject mePmFolder = DataManagementUtil.findObjectByUid(tcsoaServiceFactory.getDataManagementService(),map.get("DT+RD+ME PM"));
                                DataManagementUtil.addRelation(tcsoaServiceFactory.getDataManagementService(),homeFolder,mePmFolder,"contents");
                            }
                        } else {
                            if(!set.contains(map.get(param.getDept()))) {
                                ModelObject deptFolder =  DataManagementUtil.findObjectByUid(tcsoaServiceFactory.getDataManagementService(),map.get(param.getDept()));
                                DataManagementUtil.addRelation(tcsoaServiceFactory.getDataManagementService(),homeFolder,deptFolder,"contents");
                            }
                        }
                    }
                    // 修改個人文件夾的所屬關係
                    DataManagementUtil.getProperty(tcsoaServiceFactory.getDataManagementService(),homeFolder,"owning_user");
                    tcsoaServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{homeFolder});
                    String userUid = homeFolder.get_owning_user().getUid();
                    if(!userUid.equals(param.getUserUid())){
                        User user = (User) DataManagementUtil.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), param.getUserUid());
                        Group group = (Group) DataManagementUtil.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), param.getGroupUid());
                        DataManagementUtil.changeOwner(tcsoaServiceFactory.getDataManagementService(),homeFolder,user,group);
                    }
                    return homeFolder.getUid();
                }
            }
        }catch (Exception e){
            LogFactory.get().error("查詢TC數據出錯");
        }finally {
            if(ObjectUtil.isNotNull(tcsoaServiceFactory)){
                tcsoaServiceFactory.logout();
            }
        }
        return null;
    }

    @Override
    public String getTaskFolderUid(PersonalFolderParam param) {
        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS4);
        try {
            User user = (User) DataManagementUtil.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), param.getUserUid());
            Group group = (Group) DataManagementUtil.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), param.getGroupUid());
            String userName = null;
            String folderName = null;
            List<ModelObject> tasksToPerforms = null;
            List<ModelObject> tasksToTracks = null;
            Map<String, Object> actualUsers = QueryUtil.executeQuery(tcsoaServiceFactory.getSavedQueryService(), "__D9_Find_Actual_User",
                    new String[]{"item_id"}, new String[]{param.getEmpNo()});
            if(ObjectUtil.isNotNull(actualUsers.get("succeeded"))){
                ModelObject[] md = (ModelObject[]) actualUsers.get("succeeded");
                if(md != null && md.length > 0){
                    ModelObject object =  md[0];
                    DataManagementUtil.getProperties(tcsoaServiceFactory.getDataManagementService(), object, new String[]{"item_id","object_name"});
                    tcsoaServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{object});
                    String itemId = object.getPropertyObject("item_id").getStringValue();
                    userName = object.getPropertyObject("object_name").getStringValue();
                    folderName = userName + "(" + itemId + ")任務箱";
                }
            }
            if(StrUtil.isNotBlank(folderName)){
                DataManagementUtil.getProperty(tcsoaServiceFactory.getDataManagementService(),user,"userinbox");
                tcsoaServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{user});
                ModelObject userinbox = user.get_userinbox();
                DataManagementUtil.getProperty(tcsoaServiceFactory.getDataManagementService(),userinbox,"contents");
                tcsoaServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{userinbox});
                List<ModelObject> contents = userinbox.getPropertyObject("contents").getModelObjectListValue();
                if(CollUtil.isNotEmpty(contents)){
                    // 要執行的任務
                    ModelObject tasksToPerform = null;
                    // 要跟蹤的任務
                    ModelObject tasksToTrack = null;
                    ModelObject taskInbox = contents.get(0);
                    DataManagementUtil.getProperty(tcsoaServiceFactory.getDataManagementService(),taskInbox,"contents");
                    tcsoaServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{taskInbox});
                    List<ModelObject> list = taskInbox.getPropertyObject("contents").getModelObjectListValue();
                    for (ModelObject object : list) {
                        String name = object.getTypeObject().getName();
                        if("TasksToPerform".equals(name)){
                            tasksToPerform = object;
                        }else if("TasksToTrack".equals(name)){
                            tasksToTrack = object;
                        }
                    }
                    // 過濾要執行的任務
                    tasksToPerforms = getTask(tasksToPerform, userName);
                    // 過濾要跟蹤的任務
                    tasksToTracks = getTask(tasksToTrack, userName);
                }
            }
            // 任務列表
            DataManagementUtil.getProperty(tcsoaServiceFactory.getDataManagementService(),user,"userid");
            tcsoaServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{user});
            Folder InBoxFolder = null;
            Map<String, Object> myInBoxMap = QueryUtil.executeQuery(tcsoaServiceFactory.getSavedQueryService(), "General...",
                    new String[]{"object_name", "object_type", "owning_user.user_id" }, new String[]{folderName,"Folder", user.get_userid()});
            if(ObjectUtil.isNotNull(myInBoxMap.get("succeeded"))){
                ModelObject[] md = (ModelObject[]) myInBoxMap.get("succeeded");
                if(md != null && md.length > 0){
                    InBoxFolder = (Folder) md[0];
                }else{
                    InBoxFolder = FolderUtil.createFolder(tcsoaServiceFactory.getDataManagementService(),"Folder",folderName,null);
                    DataManagementUtil.changeOwner(tcsoaServiceFactory.getDataManagementService(),InBoxFolder,user,group);
                }
                Map<String,ModelObject> folderMap = new HashMap<>();
                DataManagementUtil.getProperty(tcsoaServiceFactory.getDataManagementService(),InBoxFolder,"contents");
                tcsoaServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{InBoxFolder});
                List<ModelObject> childrenFolder = InBoxFolder.getPropertyObject("contents").getModelObjectListValue();
                for (ModelObject object : childrenFolder) {
                    DataManagementUtil.getProperty(tcsoaServiceFactory.getDataManagementService(),object,"object_name");
                    tcsoaServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{object});
                    folderMap.put(object.getPropertyObject("object_name").getStringValue(),object);
                }
                ModelObject tasksToPerformFolder = null;
                if(folderMap.containsKey("要執行的任務")){
                    // 清空對象重新複製
                    tasksToPerformFolder = folderMap.get("要執行的任務");
                    DataManagementUtil.deleteRelation(tcsoaServiceFactory.getDataManagementService(),tasksToPerformFolder,null,"contents");
                }else{
                    tasksToPerformFolder = FolderUtil.createFolder(tcsoaServiceFactory.getDataManagementService(),"Folder","要執行的任務",null);
                    DataManagementUtil.changeOwner(tcsoaServiceFactory.getDataManagementService(),tasksToPerformFolder,user,group);
                    DataManagementUtil.addRelation(tcsoaServiceFactory.getDataManagementService(),InBoxFolder,tasksToPerformFolder,"contents");
                }
                if(CollUtil.isNotEmpty(tasksToPerforms)){
                    for (ModelObject tasksToPerform : tasksToPerforms) {
                        DataManagementUtil.addRelation(tcsoaServiceFactory.getDataManagementService(),tasksToPerformFolder,tasksToPerform,"contents");
                    }
                }
                ModelObject tasksToTrackFolder = null;
                if(folderMap.containsKey("要跟蹤的任務")){
                    // 清空對象重新複製
                    tasksToTrackFolder = folderMap.get("要跟蹤的任務");
                    DataManagementUtil.deleteRelation(tcsoaServiceFactory.getDataManagementService(),tasksToTrackFolder,null,"contents");
                }else{
                    tasksToTrackFolder = FolderUtil.createFolder(tcsoaServiceFactory.getDataManagementService(),"Folder","要跟蹤的任務",null);
                    DataManagementUtil.changeOwner(tcsoaServiceFactory.getDataManagementService(),tasksToTrackFolder,user,group);
                    DataManagementUtil.addRelation(tcsoaServiceFactory.getDataManagementService(),InBoxFolder,tasksToTrackFolder,"contents");
                }
                if(CollUtil.isNotEmpty(tasksToTracks)){
                    for (ModelObject tasksToTrack : tasksToTracks) {
                        DataManagementUtil.addRelation(tcsoaServiceFactory.getDataManagementService(),tasksToTrackFolder,tasksToTrack,"contents");
                    }
                }
                return  InBoxFolder.getUid();
            }
        }catch (Exception e){
            LogFactory.get().error("查詢TC數據出錯");
        }finally {
            if(ObjectUtil.isNotNull(tcsoaServiceFactory)){
                tcsoaServiceFactory.logout();
            }
        }
        return null;
    }


    @Override
    public List<String> getTaskUids(TaskUidsParam param) {
        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS4);
        try {
            String userName = "apadmin";
            Map<String, Object> actualUsers = QueryUtil.executeQuery(tcsoaServiceFactory.getSavedQueryService(), "__D9_Find_Actual_User",
                    new String[]{"item_id"}, new String[]{param.getEmpNo()});
            if(ObjectUtil.isNotNull(actualUsers.get("succeeded"))){
                ModelObject[] md = (ModelObject[]) actualUsers.get("succeeded");
                if(md != null && md.length > 0){
                    ModelObject object =  md[0];
                    DataManagementUtil.getProperties(tcsoaServiceFactory.getDataManagementService(), object, new String[]{"item_id","object_name"});
                    tcsoaServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{object});
                    userName = object.getPropertyObject("object_name").getStringValue();
                }
            }
            List<String> resList = new ArrayList<>();
            if(StrUtil.isNotBlank(userName)){
                ServiceData serviceData = tcsoaServiceFactory.getDataManagementService().loadObjects(param.getUids().toArray(String[]::new));
                ModelObject[] objList = new ModelObject[serviceData.sizeOfPlainObjects()];
                Map<String,String> map = new HashMap<>();
                for (int i = 0; i < serviceData.sizeOfPlainObjects(); i++) {
                    objList[i] = serviceData.getPlainObject(i);
                }
                tcsoaServiceFactory.getDataManagementService().getProperties(objList,new String[]{"fnd0ParentTask"});
                tcsoaServiceFactory.getDataManagementService().refreshObjects(objList);
                List<ModelObject> taskList = new ArrayList<>();
                for (int i = 0; i < objList.length; i++) {
                    try{
                        ModelObject task = objList[i].getPropertyObject("fnd0ParentTask").getModelObjectValue();
                        taskList.add(task);
                        map.put(task.getUid(),objList[i].getUid());
                    }catch (Exception e){

                    }
                }
                if(CollUtil.isNotEmpty(taskList)){
                    ModelObject[] modelObjects = taskList.toArray(ModelObject[]::new);
                    tcsoaServiceFactory.getDataManagementService().getProperties(modelObjects,new String[]{"root_target_attachments", "parent_name"});
                    tcsoaServiceFactory.getDataManagementService().refreshObjects(modelObjects);
                    for (int i = 0; i < modelObjects.length; i++) {
                        ModelObject task = modelObjects[i];
                        String taskName = task.getPropertyObject("parent_name").getStringValue();
                        List<ModelObject> list = task.getPropertyObject("root_target_attachments").getModelObjectListValue();
                        for (ModelObject object : list) {
                            if (object instanceof ItemRevision) {
                                ActualUserPojo pojo = ActualUserUtil.getActualUserByProcessNode(tcsoaServiceFactory.getDataManagementService(), object, taskName);
                                if (ObjectUtil.isNotNull(pojo) && userName.equals(pojo.getActualUserName())) {
                                    resList.add(map.get(task.getUid()));
                                }
                            }
                        }
                    }
                }
            }
            return resList;
        }catch (Exception e){
            LogFactory.get().error("查詢TC數據出錯");
        }finally {
            if(ObjectUtil.isNotNull(tcsoaServiceFactory)){
                tcsoaServiceFactory.logout();
            }
        }
        return null;
    }

    private List<ModelObject> getTask (ModelObject folder, String actualUserName) throws NotLoadedException {
        List<ModelObject> resList = new ArrayList<>();
        if(ObjectUtil.isNotNull(folder)){
            // 過濾要執行的任務
            DataManagementUtil.getProperty(tcsoaServiceFactory.getDataManagementService(),folder,"contents");
            tcsoaServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{folder});
            List<ModelObject> tasks = folder.getPropertyObject("contents").getModelObjectListValue();
            for (ModelObject task : tasks) {
                DataManagementUtil.getProperties(tcsoaServiceFactory.getDataManagementService(),task,new String[]{"root_target_attachments","parent_name"});
                tcsoaServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{task});
                String taskName = task.getPropertyObject("parent_name").getStringValue();
                List<ModelObject> list = task.getPropertyObject("root_target_attachments").getModelObjectListValue();
                for (ModelObject object : list) {
                    if(object instanceof ItemRevision){
                        ActualUserPojo pojo = ActualUserUtil.getActualUserByProcessNode(tcsoaServiceFactory.getDataManagementService(), object, taskName);
                        if(ObjectUtil.isNotNull(pojo) && actualUserName.equals(pojo.getActualUserName())){
                            resList.add(object);
                        }
                    }
                }
            }
        }
        return resList;
    }
}
