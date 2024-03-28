package com.foxconn.plm.utils.tc;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core._2006_03.DataManagement;
import com.teamcenter.soa.client.model.ErrorStack;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.Folder;
import com.teamcenter.soa.client.model.strong.Group;
import com.teamcenter.soa.client.model.strong.User;
import com.teamcenter.soa.client.model.strong.WorkspaceObject;
import com.teamcenter.soa.exceptions.NotLoadedException;
import com.teamcenter.services.strong.core._2008_06.DataManagement.CreateIn;
import com.teamcenter.services.strong.core._2008_06.DataManagement.CreateResponse;

import java.util.*;

public class DataManagementUtil {

    private static Log log = LogFactory.get();

    //根据UID查数据
    public static ModelObject findObjectByUid(DataManagementService dmService, String uid) {
        ServiceData sd = dmService.loadObjects(new String[]{uid});
        return sd.getPlainObject(0);
    }


    public static ModelObject createTableRow(DataManagementService dmService,String type,Map<String,String> map) throws ServiceException {
        CreateIn[] createIn = new CreateIn[1];
        createIn[0] = new CreateIn();
        createIn[0].data.boName = type;
        Set<Map.Entry<String,String>> entrySet = map.entrySet();
        for (Map.Entry<String,String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            createIn[0].data.stringProps.put(key, value);
        }
        try {
            CreateResponse createObjects = dmService.createObjects(createIn);
            int sizeOfPartialErrors = createObjects.serviceData.sizeOfPartialErrors();
            for (int i = 0; i < sizeOfPartialErrors; i++) {
                ErrorStack partialError = createObjects.serviceData.getPartialError(i);
                String[] messages = partialError.getMessages();
                if (messages != null && messages.length > 0) {
                    StringBuffer sBuffe = new StringBuffer();
                    for (String string : messages) {
                        sBuffe.append(string);
                    }
                    throw new ServiceException(sBuffe.toString());
                }

            }
            if (createObjects.output.length > 0) {
                return createObjects.output[0].objects[0];
            }
        }catch (ServiceException e){
            throw new ServiceException(e.getMessages());
        }
        return null;
    }

    public static void getProperty(DataManagementService dmService, ModelObject object, String propName) {
        ModelObject[] objects = {object};
        String[] atts = {propName};
        ServiceData data = dmService.getProperties(objects, atts);
        if (data.sizeOfPartialErrors() > 0) {
            for (int i = 0; i < data.sizeOfPartialErrors(); i++) {
                log.info("【WARN】 warn info: " + Arrays.toString(data.getPartialError(i).getMessages()));
            }
        }
    }

    public static void getProperties(DataManagementService dmService, ModelObject object, String[] propNames) {
        ModelObject[] objects = {object};
        ServiceData data = dmService.getProperties(objects, propNames);

    }

    public static void addRelation(DataManagementService dmService, ModelObject primaryObject,
                                   ModelObject secondaryObject, String relationType) {
        DataManagement.Relationship[] relationships = new DataManagement.Relationship[1];
        relationships[0] = new DataManagement.Relationship();
        relationships[0].primaryObject = primaryObject;
        relationships[0].secondaryObject = secondaryObject;
        relationships[0].relationType = relationType;
        dmService.createRelations(relationships);
    }

    public static void deleteRelation(DataManagementService dmService, ModelObject primaryObject,
                                      ModelObject secondaryObject, String relationType) {
        DataManagement.Relationship[] relationships = new DataManagement.Relationship[1];
        relationships[0] = new DataManagement.Relationship();
        relationships[0].clientId = "";
        relationships[0].primaryObject = primaryObject;
        relationships[0].secondaryObject = secondaryObject;
        relationships[0].relationType = relationType;
        dmService.deleteRelations(relationships);
    }

    public static Boolean changeOwner(DataManagementService datamanagementservice, ModelObject obj, User user,
                                       Group group) {
        try {
            com.teamcenter.services.strong.core._2006_03.DataManagement.ObjectOwner[] owners = new com.teamcenter.services.strong.core._2006_03.DataManagement.ObjectOwner[1];
            owners[0] = new com.teamcenter.services.strong.core._2006_03.DataManagement.ObjectOwner();
            owners[0].group = group;
            owners[0].owner = user;
            owners[0].object = obj;
            ServiceData data = datamanagementservice.changeOwnership(owners);
            if (data.sizeOfPartialErrors() > 0) {
                throw new ServiceException("DataManagementService changeOwner returned a partial error");
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("【ERROR】 更改所有权失败，请联系管理员！");
        }
        return false;
    }

    public static ServiceData setProperties(DataManagementService dmService, ModelObject object, String propName, String propValue) {
        Map<String, com.teamcenter.services.strong.core._2007_01.DataManagement.VecStruct> map = new HashMap<>();
        com.teamcenter.services.strong.core._2007_01.DataManagement.VecStruct vecStruce = new com.teamcenter.services.strong.core._2007_01.DataManagement.VecStruct();
        vecStruce.stringVec = new String[]{propValue};
        map.put(propName, vecStruce);
        ServiceData srd = dmService.setProperties(new ModelObject[]{object}, map);
        dmService.refreshObjects(new ModelObject[]{object});
        return srd;
    }

    public static ServiceData setProperties(DataManagementService dmService, ModelObject object, Map<String,List<String>> props) {
        Map<String, com.teamcenter.services.strong.core._2007_01.DataManagement.VecStruct> map = new HashMap<>();
        for (String propName : props.keySet()) {
            com.teamcenter.services.strong.core._2007_01.DataManagement.VecStruct vecStruce = new com.teamcenter.services.strong.core._2007_01.DataManagement.VecStruct();
            vecStruce.stringVec = props.get(propName).toArray(new String[0]);
            map.put(propName, vecStruce);
        }
        ServiceData srd = dmService.setProperties(new ModelObject[]{object}, map);
        dmService.refreshObjects(new ModelObject[]{object});
        return srd;
    }

    public static Folder getFolder(DataManagementService dmService, Folder folder, String type, String name) throws NotLoadedException {
        DataManagementUtil.getProperty(dmService,folder,"contents");
        dmService.refreshObjects(new ModelObject[]{folder});
        WorkspaceObject[] contents = folder.get_contents();
        for (int i = 0; i < contents.length; i++) {
            WorkspaceObject content = contents[i];
            if (content instanceof Folder) {
                Folder childFolder = (Folder) content;
                DataManagementUtil.getProperties(dmService, childFolder, new String[]{"object_name","object_type"});
                dmService.refreshObjects(new ModelObject[]{childFolder});
                String objectType = childFolder.get_object_type();
                String folderName = childFolder.get_object_name();
                if (folderName.equalsIgnoreCase(name) && type.equals(objectType)) {
                    return childFolder;
                }
            }
        }
        return null;
    }
}
