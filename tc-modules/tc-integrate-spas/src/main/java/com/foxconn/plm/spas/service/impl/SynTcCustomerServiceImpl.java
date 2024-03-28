package com.foxconn.plm.spas.service.impl;

import com.foxconn.plm.spas.bean.SynSpasChangeData;
import com.foxconn.plm.spas.bean.SynSpasConstants;
import com.foxconn.plm.spas.config.properties.SpasPropertiesConfig;
import com.foxconn.plm.spas.service.SynTcChangeDataService;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.core._2008_06.DataManagement;
import com.teamcenter.services.strong.query._2007_06.SavedQuery;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.Folder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/12/ 9:02
 * @description
 */
@Service("synTcCustomerServiceImpl")
public class SynTcCustomerServiceImpl extends SynTcChangeDataService {

    private TCSOAServiceFactory tCSOAServiceFactory;
    private SynSpasChangeData synSpasChangeData;

    @Override
    public void synSpasDataToTc(TCSOAServiceFactory tCSOAServiceFactory, SynSpasChangeData synSpasChangeData) throws Exception {
        this.tCSOAServiceFactory = tCSOAServiceFactory;
        this.synSpasChangeData = synSpasChangeData;
        String operationType = synSpasChangeData.getCustomerOperationType();
        if ("A".equals(operationType)) {
//            String customerId = synSpasChangeData.getCustomerId();
//            SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(), SynSpasConstants.D9_FIND_PROJECT_FOLDER,
//                    new String[]{SynSpasConstants.D9_SPAS_ID}, new String[]{customerId});
//            ServiceData serviceData = savedQueryResult.serviceData;
//            if (serviceData.sizeOfPartialErrors() == 0) {
//                ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
//                if (objs.length > 0) {
//                    throw new Exception("客户文件夹【"+ customerId +"】TC系统已存在！");
//                }
//            } else {
//                throw new Exception("创建客户文件夹时查询失败：" + serviceData.getPartialError(0));
//            }
            addFolder();
        }
        if ("C".equals(operationType)) {
            modFolder();
        }
        if ("D".equals(operationType)) {
            delFolder();
        }
    }

    private void addFolder() throws Exception {
        String[] projectKnowledge = TCUtils.getTCPreferences(tCSOAServiceFactory.getPreferenceManagementService(), SynSpasConstants.D9_PROJECT_KNOWLEDGE_FOLDER_UID);
        if (projectKnowledge == null) {
            throw new Exception("TC系统中未找到【D9_Project_Knowledge_Folder_UID】首选项");
        }
        //专案知识库文件夹
        Folder projectKnowledgeFolder = (Folder) TCUtils.findObjectByUid(tCSOAServiceFactory.getDataManagementService(), projectKnowledge[0]);
        Map<String, String> propMap = new HashMap<>();
        propMap.put(SynSpasConstants.D9_SPAS_ID, synSpasChangeData.getCustomerId());
        propMap.put(SynSpasConstants.OBJECT_NAME, synSpasChangeData.getCustomerName());
        DataManagement.CreateResponse response = TCUtils.createObjects(tCSOAServiceFactory.getDataManagementService(), SynSpasConstants.D9_CUSTOMER, propMap);
        ServiceData serviceData = response.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            Folder customerFolder = (Folder) response.output[0].objects[0];
            TCUtils.addContents(tCSOAServiceFactory.getDataManagementService(), projectKnowledgeFolder, customerFolder);
            tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{projectKnowledgeFolder, customerFolder});
        } else {
            throw new Exception("创建客户文件夹失败：" + serviceData.getPartialError(0));
        }
    }

    private void modFolder() throws Exception {
        String customerId = synSpasChangeData.getCustomerId();
        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(), SynSpasConstants.D9_FIND_PROJECT_FOLDER,
                new String[]{SynSpasConstants.D9_SPAS_ID}, new String[]{customerId});
        ServiceData serviceData = savedQueryResult.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
            if (objs.length == 0) {
                throw new Exception("修改客户文件夹时未找到客户文件夹.");
            }
            Folder folder = (Folder) objs[0];
            TCUtils.setProperties(tCSOAServiceFactory.getDataManagementService(), folder, SynSpasConstants.OBJECT_NAME, synSpasChangeData.getCustomerName());
            tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{folder});
        } else {
            throw new Exception("修改客户文件夹失败：" + serviceData.getPartialError(0));
        }
    }

    private void delFolder() throws Exception {
        String customerId = synSpasChangeData.getCustomerId();
        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(), SynSpasConstants.D9_FIND_PROJECT_FOLDER,
                new String[]{SynSpasConstants.D9_SPAS_ID}, new String[]{customerId});
        ServiceData serviceData = savedQueryResult.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
            if (objs.length == 0) {
                throw new Exception("删除客户文件夹时未找到客户文件夹.");
            }
            Folder folder = (Folder) objs[0];
            TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), folder, SynSpasConstants.OBJECT_NAME);
            String folderName = folder.get_object_name();
            TCUtils.deleteFolder2(tCSOAServiceFactory.getDataManagementService(), folder, folderName);
            tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{folder});
        } else {
            throw new Exception("删除客户文件夹失败：" + serviceData.getPartialError(0));
        }
    }

}
