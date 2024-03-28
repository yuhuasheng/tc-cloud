package com.foxconn.plm.spas.service.impl;

import com.foxconn.plm.spas.bean.SynSpasConstants;
import com.foxconn.plm.spas.bean.SynSpasChangeData;
import com.foxconn.plm.spas.service.SynTcChangeDataService;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.internal.strong.core.ICTService;
import com.teamcenter.services.internal.strong.core._2011_06.ICT;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core._2008_06.DataManagement;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.services.strong.query._2007_06.SavedQuery;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.Folder;
import com.teamcenter.soa.client.model.strong.User;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/12/ 9:06
 * @description
 */
@Service("synTcSeriesServiceImpl")
public class SynTcSeriesServiceImpl extends SynTcChangeDataService {

    private TCSOAServiceFactory tCSOAServiceFactory;
    private SynSpasChangeData synSpasChangeData;

    @Override
    public void synSpasDataToTc(TCSOAServiceFactory tCSOAServiceFactory, SynSpasChangeData synSpasChangeData) throws Exception {
        this.tCSOAServiceFactory = tCSOAServiceFactory;
        this.synSpasChangeData = synSpasChangeData;
        String operationType = synSpasChangeData.getSeriesOperationType();
        if ("A".equals(operationType)) {
//            String seriesId = synSpasChangeData.getSeriesId();
//            SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(), SynSpasConstants.D9_FIND_PROJECT_FOLDER,
//                    new String[]{SynSpasConstants.D9_SPAS_ID}, new String[]{seriesId});
//            ServiceData serviceData = savedQueryResult.serviceData;
//            if (serviceData.sizeOfPartialErrors() == 0) {
//                ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
//                if (objs.length > 0) {
//                    throw new Exception("系列文件夹【"+ seriesId +"】TC系统已存在！");
//                }
//            } else {
//                throw new Exception("创建系列文件夹时查询失败：" + serviceData.getPartialError(0));
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
        String customerId = synSpasChangeData.getCustomerId();
        Folder customerFolder;

        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(), SynSpasConstants.D9_FIND_PROJECT_FOLDER,
                new String[]{SynSpasConstants.D9_SPAS_ID}, new String[]{customerId});
        ServiceData serviceData = savedQueryResult.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
            if (objs.length == 0) {
                throw new Exception("创建系列文件夹时未找到客户文件夹.");
            }
            customerFolder = (Folder) objs[0];
        } else {
            throw new Exception("创建系列文件夹时未找到客户文件夹：" + serviceData.getPartialError(0));
        }

        Map<String, String> propMap = new HashMap<>();
        propMap.put(SynSpasConstants.D9_SPAS_ID, synSpasChangeData.getSeriesId());
        propMap.put(SynSpasConstants.OBJECT_NAME, synSpasChangeData.getSeriesName());
        propMap.put(SynSpasConstants.OBJECT_DESC, synSpasChangeData.getBu());
        DataManagement.CreateResponse resp = TCUtils.createObjects(tCSOAServiceFactory.getDataManagementService(), SynSpasConstants.D9_SERIES, propMap);
        ServiceData service1Data = resp.serviceData;
        if (service1Data.sizeOfPartialErrors() == 0) {
            Folder seriesFolder = (Folder) resp.output[0].objects[0];
            TCUtils.addContents(tCSOAServiceFactory.getDataManagementService(), customerFolder, seriesFolder);
            tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{customerFolder, seriesFolder});
        } else {
            throw new Exception("创建系列文件夹失败：" + service1Data.getPartialError(0));
        }
    }

    private void modFolder() throws Exception {
        String seriesId = synSpasChangeData.getSeriesId();
        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(), SynSpasConstants.D9_FIND_PROJECT_FOLDER,
                new String[]{SynSpasConstants.D9_SPAS_ID}, new String[]{seriesId});
        ServiceData serviceData = savedQueryResult.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
            if (objs.length == 0) {
                throw new Exception("修改系列文件夹时未找到系列文件夹.");
            }
            Folder folder = (Folder) objs[0];
            TCUtils.setProperties(tCSOAServiceFactory.getDataManagementService(), folder, SynSpasConstants.OBJECT_NAME, synSpasChangeData.getSeriesName());
            tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{folder});
        } else {
            throw new Exception("修改系列文件夹失败：" + serviceData.getPartialError(0));
        }
    }

    private void delFolder() throws Exception {
        String seriesId = synSpasChangeData.getSeriesId();
        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(), SynSpasConstants.D9_FIND_PROJECT_FOLDER,
                new String[]{SynSpasConstants.D9_SPAS_ID}, new String[]{seriesId});
        ServiceData serviceData = savedQueryResult.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
            if (objs.length == 0) {
                throw new Exception("删除系列文件夹时未找到系列文件夹.");
            }
            Folder folder = (Folder) objs[0];
            TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), folder, SynSpasConstants.OBJECT_NAME);
            String folderName = folder.get_object_name();
            TCUtils.deleteFolder2(tCSOAServiceFactory.getDataManagementService(), folder, folderName);
            tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{folder});
        } else {
            throw new Exception("删除系列文件夹失败：" + serviceData.getPartialError(0));
        }
    }


}
