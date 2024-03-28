package com.foxconn.plm.utils.tc;

import cn.hutool.core.util.ObjectUtil;
import com.foxconn.plm.entity.pojo.ActualUserPojo;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.exceptions.NotLoadedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 實際工作者工具類
 *
 * @Description
 * @Author MW00442
 * @Date 2023/12/9 11:44
 **/
public class ActualUserUtil {

    /**
     * 獲取對象執行流程配置的節點信息
     * @param dataService
     * @param modelObject
     * @return
     */
    public static List<ActualUserPojo> getAllActualUser(DataManagementService dataService, ModelObject modelObject){
        try {
            DataManagementUtil.getProperty(dataService, modelObject, "IMAN_external_object_link");
            dataService.refreshObjects(new ModelObject[]{modelObject});
            List<ModelObject> list = modelObject.getPropertyObject("IMAN_external_object_link").getModelObjectListValue();
            ModelObject streamObj = null;
            for (ModelObject object : list) {
                if ("D9_TaskForm".equals(object.getTypeObject().getName())) {
                    streamObj = object;
                }
            }
            if (ObjectUtil.isNull(streamObj)) {
                return Collections.emptyList();
            }
            // 查詢table表的每一行
            DataManagementUtil.getProperty(dataService, streamObj, "d9_TaskTable");
            dataService.refreshObjects(new ModelObject[]{streamObj});
            List<ModelObject> table = streamObj.getPropertyObject("d9_TaskTable").getModelObjectListValue();
            List<ActualUserPojo> resList = new ArrayList<>();
            for (ModelObject object : table) {
                DataManagementUtil.getProperties(dataService, object,
                        new String[]{"d9_ProcessNode", "d9_ProcessNodeStatus","d9_TCUser","d9_ActualUserID",
                                "d9_ActualUserName", "d9_ActualUserMail","d9_ApprovalStatus","d9_ApprovalDate"});
                dataService.refreshObjects(new ModelObject[]{object});
                resList.add(getActualUserProperty(object));
            }
            return resList;
        }catch (Exception e){
            return Collections.emptyList();
        }
    }

    /**
     * 獲取對象流程指定節點的流程信息
     * @param dataService
     * @param modelObject
     * @param processNode
     * @return
     */
    public static ActualUserPojo getActualUserByProcessNode(DataManagementService dataService, ModelObject modelObject,String processNode){
        try {
            DataManagementUtil.getProperty(dataService, modelObject, "IMAN_external_object_link");
            dataService.refreshObjects(new ModelObject[]{modelObject});
            List<ModelObject> list = modelObject.getPropertyObject("IMAN_external_object_link").getModelObjectListValue();
            ModelObject streamObj = null;
            for (ModelObject object : list) {
                if ("D9_TaskForm".equals(object.getTypeObject().getName())) {
                    streamObj = object;
                }
            }
            if (ObjectUtil.isNull(streamObj)) {
                return null;
            }
            // 查詢table表的每一行
            DataManagementUtil.getProperty(dataService, streamObj, "d9_TaskTable");
            dataService.refreshObjects(new ModelObject[]{streamObj});
            List<ModelObject> table = streamObj.getPropertyObject("d9_TaskTable").getModelObjectListValue();
            for (ModelObject object : table) {
                DataManagementUtil.getProperties(dataService, object,
                        new String[]{"d9_ProcessNode", "d9_ProcessNodeStatus","d9_TCUser","d9_ActualUserID",
                                "d9_ActualUserName", "d9_ActualUserMail","d9_ApprovalStatus","d9_ApprovalDate"});
                dataService.refreshObjects(new ModelObject[]{object});
                if(processNode.equals(object.getPropertyDisplayableValue("d9_ProcessNode"))){
                    return getActualUserProperty(object);
                }
            }
        }catch (Exception e){

        }
        return null;
    }


    private static ActualUserPojo getActualUserProperty(ModelObject object) throws NotLoadedException {
        ActualUserPojo pojo = new ActualUserPojo();
        pojo.setProcessNode(object.getPropertyDisplayableValue("d9_ProcessNode"));
        pojo.setProcessNodeStatus(object.getPropertyDisplayableValue("d9_ProcessNodeStatus"));
        pojo.setTcUser(object.getPropertyDisplayableValue("d9_TCUser"));
        pojo.setActualUserId(object.getPropertyDisplayableValue("d9_ActualUserID"));
        pojo.setActualUserName(object.getPropertyDisplayableValue("d9_ActualUserName"));
        pojo.setActualUserMail(object.getPropertyDisplayableValue("d9_ActualUserMail"));
        pojo.setApprovalStatus(object.getPropertyDisplayableValue("d9_ApprovalStatus"));
        pojo.setApprovalDate(object.getPropertyDisplayableValue("d9_ApprovalDate"));
        return pojo;
    }
}
