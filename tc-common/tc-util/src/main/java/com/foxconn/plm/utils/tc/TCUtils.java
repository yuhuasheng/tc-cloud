package com.foxconn.plm.utils.tc;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCItemConstant;
import com.foxconn.plm.entity.constants.TCScheduleConstant;
import com.foxconn.plm.utils.collect.CollectUtil;
import com.foxconn.plm.utils.file.FileUtil;
import com.foxconn.plm.utils.string.StringUtil;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.loose.core.SessionService;
import com.teamcenter.services.strong.administration.PreferenceManagementService;
import com.teamcenter.services.strong.administration.UserManagementService;
import com.teamcenter.services.strong.administration._2012_09.PreferenceManagement;
import com.teamcenter.services.strong.administration._2012_09.PreferenceManagement.GetPreferencesResponse;
import com.teamcenter.services.strong.administration._2015_07.UserManagement;
import com.teamcenter.services.strong.cad.StructureManagementService;
import com.teamcenter.services.strong.cad._2007_01.StructureManagement;
import com.teamcenter.services.strong.cad._2007_01.StructureManagement.CreateBOMWindowsResponse;
import com.teamcenter.services.strong.cad._2013_05.StructureManagement.CreateWindowsInfo2;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core.ProjectLevelSecurityService;
import com.teamcenter.services.strong.core.ReservationService;
import com.teamcenter.services.strong.core._2006_03.DataManagement.CreateFolderInput;
import com.teamcenter.services.strong.core._2006_03.DataManagement.CreateFoldersResponse;
import com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship;
import com.teamcenter.services.strong.core._2006_03.Reservation;
import com.teamcenter.services.strong.core._2007_01.DataManagement.VecStruct;
import com.teamcenter.services.strong.core._2008_06.DataManagement;
import com.teamcenter.services.strong.core._2008_06.DataManagement.CreateIn;
import com.teamcenter.services.strong.core._2008_06.DataManagement.CreateInput;
import com.teamcenter.services.strong.core._2012_09.ProjectLevelSecurity;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.services.strong.query._2006_03.SavedQuery;
import com.teamcenter.services.strong.query._2006_03.SavedQuery.GetSavedQueriesResponse;
import com.teamcenter.services.strong.query._2006_03.SavedQuery.SavedQueryFieldObject;
import com.teamcenter.services.strong.query._2007_06.SavedQuery.ExecuteSavedQueriesResponse;
import com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput;
import com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryResults;
import com.teamcenter.services.strong.query._2007_09.SavedQuery.QueryResults;
import com.teamcenter.services.strong.query._2007_09.SavedQuery.SavedQueriesResponse;
import com.teamcenter.services.strong.query._2008_06.SavedQuery.QueryInput;
import com.teamcenter.services.strong.workflow.WorkflowService;
import com.teamcenter.services.strong.workflow._2008_06.Workflow;
import com.teamcenter.soa.client.Connection;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.GetFileResponse;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.Property;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.common.ObjectPropertyPolicy;
import com.teamcenter.soa.common.PolicyType;
import com.teamcenter.soa.exceptions.NotLoadedException;

import java.io.*;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Stream;

@Deprecated
public class TCUtils {

    private static Log log = LogFactory.get();

    /**
     * 开关旁路
     *
     * @param bypass
     */
    public static void byPass(SessionService sessionservice, boolean bypass) {
        com.teamcenter.services.loose.core._2007_12.Session.StateNameValue astatenamevalue[] = new com.teamcenter.services.loose.core._2007_12.Session.StateNameValue[1];
        astatenamevalue[0] = new com.teamcenter.services.loose.core._2007_12.Session.StateNameValue();
        astatenamevalue[0].name = "bypassFlag";
        astatenamevalue[0].value = Property.toBooleanString(bypass);
        sessionservice.setUserSessionState(astatenamevalue);
    }

    private static HashMap<String, ImanQuery> queryMap = new HashMap<String, ImanQuery>();

    public static ImanQuery getSavedQuery(SavedQueryService savedQueryService, String queryName) throws Exception {
        ImanQuery query = null;
        query = queryMap.get(queryName);
        if (query != null) {
            return query;
        }
        GetSavedQueriesResponse response = savedQueryService.getSavedQueries();
        for (int i = 0; i < response.queries.length; i++) {
            if (response.queries[i].name.equalsIgnoreCase(queryName)) {
                query = response.queries[i].query;
                break;
            }
        }
        return query;
    }

    //根据UID查数据
    public static ModelObject findObjectByUid(DataManagementService dmService, String uid) {
        ServiceData sd = dmService.loadObjects(new String[]{uid});
        return sd.getPlainObject(0);
    }


    /**
     * 添加关系
     *
     * @param primaryObject
     * @param secondaryObject
     */
    public static void addContents(DataManagementService dmService, ModelObject primaryObject, ModelObject secondaryObject) {
        //byPass(true);
        Relationship[] relationships = new Relationship[1];
        relationships[0] = new Relationship();
        relationships[0].primaryObject = primaryObject;
        relationships[0].secondaryObject = secondaryObject;
        relationships[0].relationType = "contents";
        dmService.createRelations(relationships);
        //byPass(false);
    }

    public static void addRelation(DataManagementService dmService, ModelObject primaryObject,
                                   ModelObject secondaryObject, String relationType) {
        Relationship[] relationships = new Relationship[1];
        relationships[0] = new Relationship();
        relationships[0].primaryObject = primaryObject;
        relationships[0].secondaryObject = secondaryObject;
        relationships[0].relationType = relationType;
        dmService.createRelations(relationships);
    }


    public static void deleteRelation(DataManagementService dmService, ModelObject primaryObject,
                                      ModelObject secondaryObject, String relationType) {
        Relationship[] relationships = new Relationship[1];
        relationships[0] = new Relationship();
        relationships[0].clientId = "";
        relationships[0].primaryObject = primaryObject;
        relationships[0].secondaryObject = secondaryObject;
        relationships[0].relationType = relationType;
        dmService.deleteRelations(relationships);
    }


    /**
     * @param dmService
     * @param primaryObject
     * @param secondaryObjects
     */
    public static void addContents(DataManagementService dmService, ModelObject primaryObject, ModelObject[] secondaryObjects) {
        Relationship[] relationships = new Relationship[secondaryObjects.length];
        for (int i = 0; i < secondaryObjects.length; i++) {
            relationships[i] = new Relationship();
            relationships[i].primaryObject = primaryObject;
            relationships[i].secondaryObject = secondaryObjects[i];
            relationships[i].relationType = "contents";
        }
        dmService.createRelations(relationships);
    }


    /**
     * 创建文件夹
     *
     * @param folderName
     * @return
     */
    public static Folder createFolder(DataManagementService dmService, String folderName) {
        CreateFolderInput[] folderInputs = new CreateFolderInput[1];
        folderInputs[0] = new CreateFolderInput();
        folderInputs[0].name = folderName;

        CreateFoldersResponse response = dmService.createFolders(folderInputs, null, "");

        if (response.serviceData.sizeOfPartialErrors() <= 0) {
            return response.output[0].folder;
        }
        return null;
    }

    public static DataManagement.CreateResponse createObjects(DataManagementService dmService, String objectType, Map<String, String> propMap) {
        DataManagement.CreateResponse response = null;
        try {
            CreateIn[] createIns = new CreateIn[1];
            createIns[0] = new CreateIn();
            CreateInput createInput = new CreateInput();
            createInput.boName = objectType;
            createInput.stringProps = propMap;
            createIns[0].data = createInput;
            response = dmService.createObjects(createIns);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }




    public static DataManagement.CreateResponse batchCreateObject(DataManagementService dmService, List<Map<String, String>> propMaps) {
        DataManagement.CreateResponse response = null;
        try {
            CreateIn[] createIns = new CreateIn[propMaps.size()];
            for (int i = 0; i < propMaps.size(); i++) {
                Map<String, String> propMap = propMaps.get(i);
                createIns[i] = new CreateIn();
                CreateInput createInput = new CreateInput();
                createInput.boName = propMap.get("object_type");
                propMap.remove("object_type");
                createInput.stringProps = propMap;
                createIns[i].data = createInput;
            }
            response = dmService.createObjects(createIns);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 删除文件夹
     */
    public static void deleteFolder(DataManagementService dmService, ModelObject[] folders) {
        dmService.deleteObjects(folders);
    }

    /**
     * 获取单个属性
     *
     * @param object
     * @param propName
     */
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


    public static void getProperties(DataManagementService dmService, ModelObject[] objects, String[] propNames) {
        ServiceData data = dmService.getProperties(objects, propNames);
        if (data.sizeOfPartialErrors() > 0) {
            for (int i = 0; i < data.sizeOfPartialErrors(); i++) {
                log.info("【WARN】 warn info: " + Arrays.toString(data.getPartialError(i).getMessages()));
            }
        }
    }

    /**
     * 设置属性值
     *
     * @param object
     * @param propName
     * @param propValue
     */
    public static ServiceData setProperties(DataManagementService dmService, ModelObject object, String propName, String propValue) {
        Map<String, VecStruct> map = new HashMap<>();
        VecStruct vecStruce = new VecStruct();
        vecStruce.stringVec = new String[]{propValue};
        map.put(propName, vecStruce);
        ServiceData srd = dmService.setProperties(new ModelObject[]{object}, map);
        dmService.refreshObjects(new ModelObject[]{object});
        return srd;
    }

    public static void setUserProperties(UserManagementService umService, String userId, String propName, String propValue) {
        UserManagement.CreateOrUpdateUserInputs[] userInputs = new UserManagement.CreateOrUpdateUserInputs[1];
        UserManagement.CreateOrUpdateUserInputs userInput = new UserManagement.CreateOrUpdateUserInputs();
        userInput.userId = userId;
        Map<String, String[]> map = new HashMap<String, String[]>();
        map.put(propName, new String[]{propValue});
        userInput.userPropertyMap = map;
        userInputs[0] = userInput;
        umService.createOrUpdateUser(userInputs);
    }


    public static ServiceData setProperties(DataManagementService dmService, ModelObject objects[], String propNames[], String propValues[]) {
        Map<String, VecStruct> map = new HashMap<>();
        for (int i = 0; i < propNames.length; i++) {
            VecStruct vecStruce = new VecStruct();
            vecStruce.stringVec = new String[]{propValues[i]};
            map.put(propNames[i], vecStruce);
        }
        return dmService.setProperties(objects, map);
    }

    //调用查询
    public static Map<String, Object> executeQuery(SavedQueryService queryService, String searchName, String[] keys, String[] values) {
        Map<String, Object> queryResults = new HashMap<>();
        try {
            ImanQuery query = null;
            GetSavedQueriesResponse savedQueries = queryService.getSavedQueries();
            for (int i = 0; i < savedQueries.queries.length; i++) {
                if (savedQueries.queries[i].name.equals(searchName)) {
                    query = savedQueries.queries[i].query;
                    break;
                }
            }
            if (query == null) {
                queryResults.put("failed", "系统中未找到【" + searchName + "】查询..");
                return queryResults;
            }
            Map<String, String> entriesMap = new HashMap<>();
            SavedQuery.DescribeSavedQueriesResponse describeSavedQueriesResponse = queryService.describeSavedQueries(new ImanQuery[]{query});
            for (SavedQuery.SavedQueryFieldObject field : describeSavedQueriesResponse.fieldLists[0].fields) {
                String attributeName = field.attributeName;
                String entryName = field.entryName;
                System.out.println(entryName);
                entriesMap.put(attributeName, entryName);
            }
            String[] entries = new String[keys.length];
            for (int i = 0; i < keys.length; i++) {
                entries[i] = entriesMap.get(keys[i]);
            }
            SavedQueryInput[] savedQueryInput = new SavedQueryInput[1];
            savedQueryInput[0] = new SavedQueryInput();
            savedQueryInput[0].query = query;
            savedQueryInput[0].entries = entries;
            savedQueryInput[0].values = values;
            ExecuteSavedQueriesResponse savedQueryResult = queryService.executeSavedQueries(savedQueryInput);
            SavedQueryResults found = savedQueryResult.arrayOfResults[0];
            queryResults.put("succeeded", found.objects);
            return queryResults;
        } catch (Exception e) {
            queryResults.put("failed", e.getMessage());
            return queryResults;
        }
    }


    public static Map<String, Object> executeQueryByEntries(SavedQueryService queryService, String searchName, String[] entries, String[] values) {
        Map<String, Object> queryResults = new HashMap<>();
        try {
            ImanQuery query = null;
            GetSavedQueriesResponse savedQueries = queryService.getSavedQueries();
            for (int i = 0; i < savedQueries.queries.length; i++) {
                if (savedQueries.queries[i].name.equals(searchName)) {
                    query = savedQueries.queries[i].query;
                    break;
                }
            }
            if (query == null) {
                queryResults.put("failed", "系统中未找到【" + searchName + "】查询..");
                return queryResults;
            }
            SavedQueryInput[] savedQueryInput = new SavedQueryInput[1];
            savedQueryInput[0] = new SavedQueryInput();
            savedQueryInput[0].query = query;
            savedQueryInput[0].entries = entries;
            savedQueryInput[0].values = values;
            ExecuteSavedQueriesResponse savedQueryResult = queryService.executeSavedQueries(savedQueryInput);
            SavedQueryResults found = savedQueryResult.arrayOfResults[0];
            queryResults.put("succeeded", found.objects);
            return queryResults;
        } catch (Exception e) {
            queryResults.put("failed", e.getMessage());
            return queryResults;
        }
    }


    //调用查询
    public static ModelObject[] executeSOAQuery(SavedQueryService queryService, String searchName, String[] keys, String[] values) throws Exception {
        ModelObject[] modelObject = null;
        ImanQuery query = null;
        GetSavedQueriesResponse savedQueries = queryService.getSavedQueries();
        if (savedQueries.queries.length == 0) {
            throw new Exception("There are no saved queries in the system.");
        }
        for (int i = 0; i < savedQueries.queries.length; i++) {
            if (savedQueries.queries[i].name.equals(searchName)) {
                query = savedQueries.queries[i].query;
                System.out.println(query);
                break;
            }
        }
        Map<String, String> entriesMap = new HashMap<>();
        SavedQuery.DescribeSavedQueriesResponse describeSavedQueriesResponse = queryService.describeSavedQueries(new ImanQuery[]{query});
        SavedQueryFieldObject[] fields = describeSavedQueriesResponse.fieldLists[0].fields;
        for (SavedQuery.SavedQueryFieldObject field : describeSavedQueriesResponse.fieldLists[0].fields) {
            String attributeName = field.attributeName;
            String entryName = field.entryName;
            entriesMap.put(attributeName, entryName);
        }
        String[] entries = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            entries[i] = entriesMap.get(keys[i]);
        }
        SavedQueryInput[] savedQueryInput = new SavedQueryInput[1];
        savedQueryInput[0] = new SavedQueryInput();
        savedQueryInput[0].query = query;
        savedQueryInput[0].maxNumToReturn = 9999;
        savedQueryInput[0].entries = entries;
        savedQueryInput[0].values = values;
        ExecuteSavedQueriesResponse savedQueryResult = queryService.executeSavedQueries(savedQueryInput);
        SavedQueryResults found = savedQueryResult.arrayOfResults[0];
        return found.objects;
    }

    //获得指定名称首选项:站点类型
    public static String[] getTCPreferences(PreferenceManagementService preferenmanagementservice, String prefername) throws Exception {
        preferenmanagementservice.refreshPreferences();
        GetPreferencesResponse getpreferencesRes = preferenmanagementservice.getPreferences(new String[]{prefername}, false);
        PreferenceManagement.CompletePreference[] completePref = getpreferencesRes.response;
        String[] temps = null;
        if (completePref.length > 0) {
            PreferenceManagement.CompletePreference onecompletePref = completePref[0];
            PreferenceManagement.PreferenceValue prefvalue = onecompletePref.values;
            temps = prefvalue.values;
        }
        return temps;
    }









    // 获取最新版本
    public static ItemRevision getItemLatestRevision(DataManagementService dmService, Item item) {
        try {
            ModelObject[] objects = {item};
            String[] atts = {"revision_list"};
            dmService.getProperties(objects, atts);
            ModelObject[] itemRevs = item.get_revision_list();
            ItemRevision itemRev = (ItemRevision) itemRevs[itemRevs.length - 1];
            return itemRev;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static ModelObject[] findUsers(DataManagementService dmService, SavedQueryService savedQueryService, String[] userIds) throws Exception {
        ModelObject[] users = new ModelObject[0];
        ImanQuery query = getSavedQuery(savedQueryService, "__find_pom_user");
        SavedQueryInput[] inputs = new SavedQueryInput[userIds.length];
        for (int i = 0; i < userIds.length; i++) {
            inputs[i] = new SavedQueryInput();
            inputs[i].query = query;
            inputs[i].entries = new String[]{"Id"};
            inputs[i].values = new String[1];
            inputs[i].values[0] = userIds[i];
        }
        ExecuteSavedQueriesResponse response = savedQueryService.executeSavedQueries(inputs);
        if (response.arrayOfResults == null || response.arrayOfResults.length <= 0) {
            return users;
        }
        for (int i = 0; i < response.arrayOfResults.length; i++) {
            ModelObject[] objs = response.arrayOfResults[i].objects;
            if (objs == null) {
                continue;
            }
            users = ArrayUtil.append(objs, users);
            for (ModelObject o : objs) {
                dmService.loadObjects(new String[]{o.getUid()});
            }
        }

        return users;
    }


    public static Item queryItemByIDOrName(SavedQueryService queryService, DataManagementService dataManagementService, String itemId, String itemName) throws Exception {
        String queryname = "Item_Name_or_ID";
        //Item_Name_or_ID
        String[] entries = new String[]{"Item ID", "Name"};
        String[] values = new String[]{itemId, itemName};
        ModelObject[] results = executequery(queryService, dataManagementService, queryname, entries, values);
        if (results != null && results.length > 0) {
            return (Item) results[0];
        }
        return null;
    }

    public static ModelObject[] executequery(SavedQueryService queryService, DataManagementService dataManagementService, String queryname, String[] entries, String[] values) throws Exception {
        ImanQuery query = null;
        try {
            GetSavedQueriesResponse savedQueries = queryService.getSavedQueries();
            if (savedQueries.queries.length == 0) {
                System.err.println("【ERROR】 There are no saved queries in the system.");
                return null;
            }
            for (int i = 0; i < savedQueries.queries.length; i++) {
                if (savedQueries.queries[i].name.equals(queryname)) {
                    query = savedQueries.queries[i].query;
                    System.out.println(query);
                    break;
                }
            }
        } catch (ServiceException e) {
            System.err.println("【ERROR】 GetSavedQueries service request failed.");
            return null;
        }
        if (query == null) {
            System.err.println("【ERROR】 There is not an 'Item Name' query.");
            return null;
        }

        SavedQuery.DescribeSavedQueriesResponse descResp = queryService.describeSavedQueries(new ImanQuery[]{query});
        SavedQueryFieldObject[] queryFields = descResp.fieldLists[0].fields;
        for (int i = 0; i < queryFields.length; i++) {
            System.out.println(queryFields[i].entryName);
        }

        try {
            QueryInput[] savedQueryInput = new QueryInput[1];
            savedQueryInput[0] = new QueryInput();
            savedQueryInput[0].query = query;
            savedQueryInput[0].maxNumToReturn = 9999;
            savedQueryInput[0].limitList = new ModelObject[0];
            savedQueryInput[0].entries = entries;
            savedQueryInput[0].values = values;

            SavedQueriesResponse savedQueryResult = queryService.executeSavedQueries(savedQueryInput);
            QueryResults found = savedQueryResult.arrayOfResults[0];

            System.out.println("Found Items:");

            String[] uids = new String[found.objectUIDS.length];
            for (int i = 0; i < found.objectUIDS.length; i++) {

                uids[i] = found.objectUIDS[i];

            }
            if (uids == null || uids.length == 0) {
                return null;
            }
            ServiceData sd = dataManagementService.loadObjects(uids);
            ModelObject[] foundObjs = new ModelObject[sd.sizeOfPlainObjects()];
            for (int k = 0; k < sd.sizeOfPlainObjects(); k++) {
                foundObjs[k] = (ModelObject) sd.getPlainObject(k);
            }
            return foundObjs;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("【ERROR】 ExecuteSavedQuery service request failed.");
            return null;
        }
    }


    public static void checkOut(ModelObject[] objects, com.teamcenter.services.strong.core._2006_03.Reservation res) {
        res.checkout(objects, "", "");
    }

    public static void checkIn(ModelObject[] objects, Reservation res) {
        res.checkin(objects);
    }


    public static void checkin(DataManagementService dataManagementService, ReservationService rs, ModelObject[]
            objects)
            throws NotLoadedException, ServiceException {
        // 判断是否已经被签出
        dataManagementService.refreshObjects(objects);
        dataManagementService.getProperties(objects, new String[]{"checked_out"});
        ArrayList<ModelObject> cArray = new ArrayList<>();
        for (ModelObject object : objects) {
            // 是否签出的标志 Y带包已经签出, ""代表已经签入
            String checkedOut = object.getPropertyObject("checked_out").getStringValue().trim();
            if (!checkedOut.isEmpty()) {
                cArray.add(object);
            }
        }
        ModelObject[] cOut = cArray.toArray(new ModelObject[0]);
        ServiceData servicedata = rs.checkin(cOut);
        if (servicedata.sizeOfPartialErrors() > 0) {
            throw new ServiceException("ReservationService checkin returned a partial error.");
        }
    }


    public static void createNewProcess(WorkflowService wfService, String workflowName, String
            processTemplate, ModelObject[] objects) throws ServiceException {
        boolean startImmediately = true;
        String observerKey = "";
        String name = workflowName;
        String subject = "";
        String description = "";

        Workflow.ContextData contextData = new Workflow.ContextData();
        contextData.attachmentCount = objects.length;
        String[] attachments = new String[objects.length];
        int[] attachmentTypes = new int[objects.length];
        for (int i = 0; i < objects.length; i++) {
            attachments[i] = objects[i].getUid();
            attachmentTypes[i] = 1;
        }
        contextData.attachments = attachments;
        contextData.attachmentTypes = attachmentTypes;
        contextData.processTemplate = processTemplate;
        contextData.subscribeToEvents = false;
        contextData.subscriptionEventCount = 0;

        Workflow.InstanceInfo instanceInfo = wfService.createInstance(startImmediately, observerKey, name, subject, description, contextData);
        ServiceData serviceData = instanceInfo.serviceData;
        if (serviceData.sizeOfPartialErrors() > 0) {
            throw new ServiceException(serviceData.getPartialError(0).getErrorValues()[0].getMessage());
        }
    }

    public static String reviseVersion(DataManagementService dataManagementService, String ruleMapping, String
            itemTypeRevName, String itemRevUid) {
        String version = null;
        try {
            com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesIn[] ins = new com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesIn[1];
            com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesIn in = new com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesIn();
            ins[0] = in;
            in.businessObjectName = itemTypeRevName;
            in.clientId = "AppX-Test";//AppX-Test
            in.operationType = 2;

            Map<String, String> map = new HashMap<>();
            map.put("item_revision_id", ruleMapping);
            in.propertyNameWithSelectedPattern = map;

            Map<String, String> map1 = new HashMap<>();
            map1.put("sourceObject", itemRevUid);
            in.additionalInputParams = map1;
            com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesResponse response = dataManagementService.generateNextValues(ins);
            com.teamcenter.services.strong.core._2013_05.DataManagement.GeneratedValuesOutput[] outputs = response.generatedValues;
            for (com.teamcenter.services.strong.core._2013_05.DataManagement.GeneratedValuesOutput result : outputs) {
                Map<String, com.teamcenter.services.strong.core._2013_05.DataManagement.GeneratedValue> resultMap = result.generatedValues;
                com.teamcenter.services.strong.core._2013_05.DataManagement.GeneratedValue generatedValue = resultMap.get("item_revision_id");
                version = generatedValue.nextValue;
                System.out.println("==>> revise Version: " + version);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return version;
    }


    public static String generateVersion(DataManagementService dataManagementService, String ruleMapping, String
            itemTypeRevName) {
        String version = null;
        com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesIn[] ins = new com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesIn[1];
        com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesIn in = new com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesIn();
        ins[0] = in;
//		in.businessObjectName = "CommercialPart Revision";
        in.businessObjectName = itemTypeRevName;
        in.clientId = "AppX-Test";
        in.operationType = 1;
        Map<String, String> map = new HashMap<String, String>();
        map.put("item_revision_id", ruleMapping);
        in.propertyNameWithSelectedPattern = map;
        com.teamcenter.services.strong.core._2013_05.DataManagement.GenerateNextValuesResponse response = dataManagementService.generateNextValues(ins);
        com.teamcenter.services.strong.core._2013_05.DataManagement.GeneratedValuesOutput[] outputs = response.generatedValues;
        for (com.teamcenter.services.strong.core._2013_05.DataManagement.GeneratedValuesOutput result : outputs) {
            Map<String, com.teamcenter.services.strong.core._2013_05.DataManagement.GeneratedValue> resultMap = result.generatedValues;
            com.teamcenter.services.strong.core._2013_05.DataManagement.GeneratedValue generatedValue = resultMap.get("item_revision_id");
            version = generatedValue.nextValue;
            System.out.println("==>> generate Version: " + version);
        }
        return version;
    }

    public static ModelObject reviseItemRev(DataManagementService dmService, ModelObject obj, String
            itemRevName, String itemRevisionId) throws Exception {
        com.teamcenter.services.strong.core._2013_05.DataManagement.ReviseIn[] reviseIns = new com.teamcenter.services.strong.core._2013_05.DataManagement.ReviseIn[1];
        //reviseIns[0].deepCopyDatas = null;
        reviseIns[0] = new com.teamcenter.services.strong.core._2013_05.DataManagement.ReviseIn();
        Map<String, String[]> map = new HashMap<>();
        map.put("object_name", new String[]{itemRevName});
        map.put("item_revision_id", new String[]{itemRevisionId});
        reviseIns[0].reviseInputs = map;
        reviseIns[0].targetObject = obj;
        com.teamcenter.services.strong.core._2013_05.DataManagement.ReviseObjectsResponse resp = dmService.reviseObjects(reviseIns);
        ServiceData serviceData = resp.serviceData;
        if (serviceData.sizeOfPartialErrors() > 0) {
            throw new Exception(serviceData.getPartialError(0).toString());
        }
        return resp.output[0].objects[0];
    }

    /**
     * 设置物件的流程状态 D9_Release
     *
     * @param workflowService
     * @param itemRevisons
     * @param statusNmae
     * @throws ServiceException
     * @author robert
     */
    public static void addStatus(WorkflowService workflowService, WorkspaceObject[] itemRevisons, String statusNmae) throws
            ServiceException {
        com.teamcenter.services.strong.workflow._2007_06.Workflow.ReleaseStatusInput[] releaseStatusInputs = new com.teamcenter.services.strong.workflow._2007_06.Workflow.ReleaseStatusInput[1];
        com.teamcenter.services.strong.workflow._2007_06.Workflow.ReleaseStatusInput releaseStatusInput = new com.teamcenter.services.strong.workflow._2007_06.Workflow.ReleaseStatusInput();
        releaseStatusInputs[0] = releaseStatusInput;
        com.teamcenter.services.strong.workflow._2007_06.Workflow.ReleaseStatusOption[] releaseStatusOptions = new com.teamcenter.services.strong.workflow._2007_06.Workflow.ReleaseStatusOption[1];
        com.teamcenter.services.strong.workflow._2007_06.Workflow.ReleaseStatusOption releaseStatusOption = new com.teamcenter.services.strong.workflow._2007_06.Workflow.ReleaseStatusOption();
        releaseStatusOptions[0] = releaseStatusOption;
        releaseStatusOption.existingreleaseStatusTypeName = "";
        releaseStatusOption.newReleaseStatusTypeName = statusNmae;
        releaseStatusOption.operation = "Append";
        releaseStatusInput.objects = itemRevisons;
        releaseStatusInput.operations = releaseStatusOptions;
        com.teamcenter.services.strong.workflow._2007_06.Workflow.SetReleaseStatusResponse reponse = workflowService.setReleaseStatus(releaseStatusInputs);
        if (reponse.serviceData.sizeOfPartialErrors() > 0) {
            throw new RuntimeException("modify status fail，cause: " + reponse.serviceData);
        }
    }

    /**
     * 更改所有权
     *
     * @param dataManagementService 工具类
     * @param obj                   对象
     * @param user                  用户
     * @param group                 组
     * @return
     */
    public static Boolean changeOwnShip(DataManagementService dataManagementService, ModelObject obj, User user,
                                        Group group) {
        Boolean check = null;
        try {
            check = changeOwner(dataManagementService, obj, user, group);
            if (!check) {
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("【ERROR】 对象/对象版本版本的所有权更改失败，请联系管理员！");
        }
        return false;
    }

    /**
     * 更改对象所有权
     *
     * @param datamanagementservice 工具类
     * @param obj                   对象
     * @param user                  用户
     * @param group                 组
     * @return
     */
    private static Boolean changeOwner(DataManagementService datamanagementservice, ModelObject obj, User user,
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

    public static boolean isReleased1(DataManagementService dmService, ItemRevision itemRev, String statusName) throws Exception {
        getProperty(dmService, itemRev, "release_status_list");
        ReleaseStatus[] statusArr = itemRev.get_release_status_list();
        if (statusArr != null && statusArr.length > 0) {
            return true;
        }
        return false;
    }

    /**
     * 版本修订
     *
     * @param item
     * @return
     * @throws Exception
     */
    public static ModelObject reviseItemRev(DataManagementService dmService, Item item, String newRevId) throws Exception {
        BOMView bomView = null;
        ItemRevision itemRev = TCUtils.getItemLatestRevision(dmService, item);
        TCUtils.getProperty(dmService, item, "bom_view_tags");
        ModelObject[] bom_view_tags = item.get_bom_view_tags();
        if (bom_view_tags.length > 0) {
            bomView = (BOMView) bom_view_tags[0];
        }
        com.teamcenter.services.strong.core._2013_05.DataManagement.ReviseIn[] reviseIns = new com.teamcenter.services.strong.core._2013_05.DataManagement.ReviseIn[1];
        com.teamcenter.services.strong.core._2013_05.DataManagement.ReviseIn reviseIn = new com.teamcenter.services.strong.core._2013_05.DataManagement.ReviseIn();

//        com.teamcenter.services.strong.core._2013_05.DataManagement.DeepCopyData deepCopyData0 = new com.teamcenter.services.strong.core._2013_05.DataManagement.DeepCopyData();
////        deepCopyData0.copyAction = "CopyAsObject";
////        deepCopyData0.copyRelations = false;
////        deepCopyData0.isRequired = true;
////        deepCopyData0.isTargetPrimary = false;
////        deepCopyData0.operationInputTypeName = "";
////        deepCopyData0.propertyName = "IMAN_master_form_rev";
////        deepCopyData0.propertyType = "Relation";
////        deepCopyDatas[0] = deepCopyData0;

        com.teamcenter.services.strong.core._2013_05.DataManagement.DeepCopyData[] deepCopyDatas = null;
        if (bomView != null) {
            deepCopyDatas = new com.teamcenter.services.strong.core._2013_05.DataManagement.DeepCopyData[1];
            com.teamcenter.services.strong.core._2013_05.DataManagement.DeepCopyData deepCopyData = new com.teamcenter.services.strong.core._2013_05.DataManagement.DeepCopyData();
            deepCopyData.attachedObject = bomView;
            deepCopyData.copyAction = "SystemCopy";
            deepCopyData.copyRelations = false;
            deepCopyData.isRequired = true;
            deepCopyData.isTargetPrimary = false;
            deepCopyData.operationInputTypeName = "";
            deepCopyData.propertyName = "structure_revisions";
            deepCopyData.propertyType = "Reference";
            deepCopyDatas[0] = deepCopyData;
        }
        Map<String, String[]> reviseInputMap = new HashMap<>();
        reviseInputMap.put("item_revision_id", new String[]{newRevId});
        if (deepCopyDatas != null) {
            reviseIn.deepCopyDatas = deepCopyDatas;
        }
        //reviseIn.reviseInputs = reviseInputMap;
        reviseIn.targetObject = itemRev;
        reviseIn.reviseInputs = reviseInputMap;
        reviseIns[0] = reviseIn;
        com.teamcenter.services.strong.core._2013_05.DataManagement.ReviseObjectsResponse reviseObjectsResponse = dmService.reviseObjects(reviseIns);
        return reviseObjectsResponse.output[0].objects[0];
    }

    /**
     * 设置属性值
     *
     * @param object
     * @param propMap
     */
    public static void setProperties(DataManagementService dmService, ModelObject object, Map<String, String> propMap) {
        Map<String, VecStruct> map = new HashMap<>();

        propMap.forEach((key, value) -> {
            VecStruct vecStruce = new VecStruct();
            vecStruce.stringVec = new String[]{value};
            map.put(key, vecStruce);
        });

        dmService.setProperties(new ModelObject[]{object}, map);
        dmService.refreshObjects(new ModelObject[]{object});
    }

    public static ItemRevision createItem(DataManagementService dmService, String itemId, String revId, String itemType, String itemName, Map<String, String> propMap) throws Exception {
        com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties[] itemProps = new com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties[1];
        com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties itemProperty = new com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties();
        itemProperty.clientId = itemId + "--" + getRandomNumber();
        itemProperty.itemId = itemId;
        itemProperty.revId = revId;
        itemProperty.name = itemName;
        itemProperty.type = itemType;
        itemProperty.description = "";
        itemProperty.uom = "";
        if (propMap != null) {
            itemProperty.extendedAttributes = new com.teamcenter.services.strong.core._2006_03.DataManagement.ExtendedAttributes[1];
            com.teamcenter.services.strong.core._2006_03.DataManagement.ExtendedAttributes theExtendedAttr = new com.teamcenter.services.strong.core._2006_03.DataManagement.ExtendedAttributes();
            theExtendedAttr.attributes = propMap;
            theExtendedAttr.objectType = itemType;
            itemProperty.extendedAttributes[0] = theExtendedAttr;
        }
        itemProps[0] = itemProperty;
        com.teamcenter.services.strong.core._2006_03.DataManagement.CreateItemsResponse response = dmService.createItems(itemProps, null, "");
        ServiceData serviceData = response.serviceData;
        int sizeOfPartialErrors = serviceData.sizeOfPartialErrors();
        if (sizeOfPartialErrors > 0) {
            String errorMessage = "";
            for (int i = 0; i < sizeOfPartialErrors; i++) {
                errorMessage = errorMessage + serviceData.getPartialError(i).toString();
            }
            throw new Exception(errorMessage);
        }
        return response.output[0].itemRev;
    }

    private static String getRandomNumber() {
        String str = "ABCDEFJHIJKLMNOPQRSTUVWXYZ0123456789";
        String uuid = new String();
        for (int i = 0; i < 4; i++) {
            char ch = str.charAt(new SecureRandom().nextInt(str.length()));
            uuid += ch;
        }
        return uuid;
    }

    /**
     * 加载属性
     *
     * @param dmService
     */
    public static void getProperty(DataManagementService dmService, ModelObject[] objects, String[] atts) {
        ServiceData data = dmService.getProperties(objects, atts);
        if (data.sizeOfPartialErrors() > 0) {
            for (int i = 0; i < data.sizeOfPartialErrors(); i++) {
                System.err.println("【WARN】 warn info: " + Arrays.toString(data.getPartialError(i).getMessages()));
            }
        }
    }

    /**
     * 返回某个对象多值属性的值(对象数组)
     *
     * @param object
     * @return
     * @throws NotLoadedException
     */
    public static ModelObject[] getPropModelObjectArray(DataManagementService dmService, ModelObject object,
                                                        String propName) throws NotLoadedException {
        getProperty(dmService, object, propName);
        return object.getPropertyObject(propName).getModelObjectArrayValue();
    }

    /**
     * 返回某个对象单值属性的值(对象)
     *
     * @param dmService
     * @param object
     * @param propName
     * @return
     * @throws NotLoadedException
     */
    public static ModelObject getPropModelObject(DataManagementService dmService, ModelObject object, String propName)
            throws NotLoadedException {
        getProperty(dmService, object, propName);
        return object.getPropertyObject(propName).getModelObjectValue();
    }

    /**
     * 设置默认加载属性方案
     *
     * @param service
     * @param objectType
     * @param properties
     */
    public static void setDefaultLoadProperty(com.teamcenter.services.strong.core.SessionService service, String objectType, String[] properties) {
        ObjectPropertyPolicy objectpropertypolicy = new ObjectPropertyPolicy();
        PolicyType policytype = new PolicyType(objectType, properties);
        objectpropertypolicy.addType(policytype);
        service.setObjectPropertyPolicy(objectpropertypolicy);
    }

    /**
     * 刷新对象
     *
     * @param dmService
     * @param
     */
    public static void refreshObject(DataManagementService dmService, ModelObject[] objects) {
        dmService.refreshObjects(objects);
    }

    /**
     * 刷新对象
     *
     * @param dmService
     * @param object
     */
    public static void refreshObject(DataManagementService dmService, ModelObject object) {
        dmService.refreshObjects(new ModelObject[]{object});
    }


    /**
     * 返回某个对象属性单值(boolean)
     *
     * @param dmService
     * @param object
     * @param propName
     * @return
     * @throws NotLoadedException
     */
    public static Boolean getPropBoolean(DataManagementService dmService, ModelObject object, String propName) throws NotLoadedException {
        getProperty(dmService, object, propName);
        return object.getPropertyObject(propName).getBoolValue();
    }


    /**
     * 返回某个对象属性单值(string)
     *
     * @param dmService
     * @param object
     * @param propName
     * @return
     * @throws NotLoadedException
     */
    public static String getPropStr(DataManagementService dmService, ModelObject object, String propName)
            throws NotLoadedException {
        getProperty(dmService, object, propName);
        return object.getPropertyObject(propName).getStringValue();
    }

    /**
     * 返回某个对象属性单值(string[])
     *
     * @param dmService
     * @param object
     * @param propName
     * @return
     * @throws NotLoadedException
     */
    public static String[] getPropStrArray(DataManagementService dmService, ModelObject object, String propName)
            throws NotLoadedException {
        getProperty(dmService, object, propName);
        return object.getPropertyObject(propName).getStringArrayValue();
    }


    public static boolean isReleased(DataManagementService dmService, ModelObject obj) throws NotLoadedException {
        if (obj == null) {
            return false;
        }
        refreshObject(dmService, obj);
        ModelObject[] relStatus = getPropModelObjectArray(dmService, obj, "release_status_list");
        if (CollectUtil.isNotEmpty(relStatus)) {
            return true;
        }
        return false;
    }

    public static boolean isReleased(DataManagementService dmService, ModelObject obj, String[] statusNameArray) throws NotLoadedException {
        if (obj == null || statusNameArray == null || statusNameArray.length < 1) {
            return false;
        }

        ModelObject[] relStatus = getPropModelObjectArray(dmService, obj, "release_status_list");
        refreshObject(dmService, relStatus);
        if (relStatus != null && relStatus.length > 0) {
            ModelObject status = relStatus[relStatus.length - 1];
            String statusName = getPropStr(dmService, status, "object_name");
            if (statusName == null || "".equalsIgnoreCase(statusName)) {
                return false;
            }
            statusName = statusName.replaceAll(" ", "");
            for (String str : statusNameArray) {
                if (StringUtil.isNotEmpty(str) && str.equalsIgnoreCase(statusName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 上传数据集
     *
     * @param itemRev
     * @param filePath
     * @param refName
     * @param datasetName
     * @param datasetType
     * @return
     * @throws ServiceException
     */
    public static Dataset uploadDataset(DataManagementService dmService, FileManagementUtility fmuService, ItemRevision itemRev, String filePath, String refName, String datasetName, String datasetType) throws ServiceException {
        Dataset dataset = null;
        DataManagement.DatasetProperties2[] datasetProps = new DataManagement.DatasetProperties2[1];
        DataManagement.DatasetProperties2 datasetProp = new DataManagement.DatasetProperties2();

        datasetProp.clientId = "datasetWriteTixTestClientId";
        datasetProp.type = datasetType;
        datasetProp.name = datasetName;
        datasetProp.description = "";
        datasetProps[0] = datasetProp;
        com.teamcenter.services.strong.core._2006_03.DataManagement.CreateDatasetsResponse dsResp = dmService.createDatasets2(datasetProps);
        dataset = dsResp.output[0].dataset;

        Relationship[] relationships = new Relationship[1];
        Relationship relationship = new Relationship();

        relationship.clientId = "";
        relationship.primaryObject = itemRev;
        relationship.secondaryObject = dataset;
        relationship.relationType = "IMAN_specification";
        relationship.userData = null;
        relationships[0] = relationship;
        com.teamcenter.services.strong.core._2006_03.DataManagement.CreateRelationsResponse crResponse = dmService.createRelations(relationships);
        ServiceData crServiceData = crResponse.serviceData;
        if (crServiceData.sizeOfPartialErrors() > 0) {
            throw new ServiceException(crServiceData.getPartialError(0).toString());
        }

        com.teamcenter.services.loose.core._2006_03.FileManagement.DatasetFileInfo[] datasetFileInfos = new com.teamcenter.services.loose.core._2006_03.FileManagement.DatasetFileInfo[1];
        com.teamcenter.services.loose.core._2006_03.FileManagement.DatasetFileInfo datasetFileInfo = new com.teamcenter.services.loose.core._2006_03.FileManagement.DatasetFileInfo();

        datasetFileInfo.fileName = filePath;
        datasetFileInfo.allowReplace = true;
        datasetFileInfo.isText = false;
        datasetFileInfo.namedReferencedName = refName;
        datasetFileInfos[0] = datasetFileInfo;

        com.teamcenter.services.loose.core._2006_03.FileManagement.GetDatasetWriteTicketsInputData[] inputDatas = new com.teamcenter.services.loose.core._2006_03.FileManagement.GetDatasetWriteTicketsInputData[1];
        com.teamcenter.services.loose.core._2006_03.FileManagement.GetDatasetWriteTicketsInputData inputData = new com.teamcenter.services.loose.core._2006_03.FileManagement.GetDatasetWriteTicketsInputData();
        inputData.dataset = dataset;
        inputData.createNewVersion = false;
        inputData.datasetFileInfos = datasetFileInfos;
        inputDatas[0] = inputData;

        ServiceData fmuResponse = fmuService.putFiles(inputDatas);
        if (fmuResponse.sizeOfPartialErrors() > 0) {
            throw new ServiceException(fmuResponse.getPartialError(0).toString());
        }
        dmService.refreshObjects(new ModelObject[]{dataset});
        return dataset;
    }


    public static ExecuteSavedQueriesResponse execute2Query(SavedQueryService savedQueryService, String searchName, String[] keys, String[] values) throws Exception {
        ImanQuery query = null;
        GetSavedQueriesResponse savedQueries = savedQueryService.getSavedQueries();
        if (savedQueries.queries.length == 0) {
            throw new Exception("【ERROR】 There are no saved queries in the system.");
        }
        for (int i = 0; i < savedQueries.queries.length; i++) {
            if (savedQueries.queries[i].name.equals(searchName)) {
                query = savedQueries.queries[i].query;
                System.out.println(query);
                break;
            }
        }
        if (query == null) {
            throw new Exception("系统中未找到 " + searchName + " 查询..");
        }
        Map<String, String> entriesMap = new HashMap<>();
        SavedQuery.DescribeSavedQueriesResponse describeSavedQueriesResponse = savedQueryService.describeSavedQueries(new ImanQuery[]{query});
        for (SavedQuery.SavedQueryFieldObject field : describeSavedQueriesResponse.fieldLists[0].fields) {
            String attributeName = field.attributeName;
            String entryName = field.entryName;
            entriesMap.put(attributeName, entryName);
        }
        String[] entries = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            entries[i] = entriesMap.get(keys[i]);
        }
        SavedQueryInput[] savedQueryInput = new SavedQueryInput[1];
        savedQueryInput[0] = new SavedQueryInput();
        savedQueryInput[0].query = query;
        savedQueryInput[0].maxNumToReturn = 9999;
        savedQueryInput[0].entries = entries;
        savedQueryInput[0].values = values;
        ExecuteSavedQueriesResponse savedQueryResult = savedQueryService.executeSavedQueries(savedQueryInput);
        return savedQueryResult;
    }

    public static void deleteFolder2(DataManagementService dataManagementService, Folder folder, String folderName) {
        folderName = folderName + "(SPAS)已删除";
        setProperties(dataManagementService, folder, "object_name", folderName);
        //dmService.deleteObjects(folders);
    }

    /**
     * 创建项目
     *
     * @param projectId
     * @param projectName
     * @param projectDescription
     * @param teamAdmin
     * @param teamUser
     */
    public static ProjectLevelSecurity.ProjectOpsResponse createTCProject(ProjectLevelSecurityService projectLevelSecurityService, String projectId, String projectName, String projectDescription,
                                                                          ModelObject teamAdmin, ModelObject teamUser) {

        com.teamcenter.services.strong.core._2017_05.ProjectLevelSecurity.ProjectInformation2[] projectInfos = new com.teamcenter.services.strong.core._2017_05.ProjectLevelSecurity.ProjectInformation2[1];
        projectInfos[0] = new com.teamcenter.services.strong.core._2017_05.ProjectLevelSecurity.ProjectInformation2();
        projectInfos[0].projectId = projectId;
        projectInfos[0].projectName = projectName;
        projectInfos[0].projectDescription = projectDescription;
        projectInfos[0].useProgramContext = false;
        projectInfos[0].visible = true;
        projectInfos[0].active = true;

        ProjectLevelSecurity.TeamMemberInfo[] TeamMemberInfos = new ProjectLevelSecurity.TeamMemberInfo[2];
        TeamMemberInfos[0] = new ProjectLevelSecurity.TeamMemberInfo();
        TeamMemberInfos[0].teamMember = teamAdmin;
        TeamMemberInfos[0].teamMemberType = 2;

        TeamMemberInfos[1] = new ProjectLevelSecurity.TeamMemberInfo();
        TeamMemberInfos[1].teamMember = teamUser;
        TeamMemberInfos[1].teamMemberType = 0;

        projectInfos[0].teamMembers = TeamMemberInfos;

        return projectLevelSecurityService.createProjects2(projectInfos);
    }

    public static File downloadDataset(DataManagementService dataManagementService,
                                       FileManagementUtility fileManagementUtility, Dataset dataset, String dirPath) throws Exception {
        File newfile = null;

        dataManagementService.refreshObjects(new ModelObject[]{dataset});
        dataManagementService.getProperties(new ModelObject[]{dataset}, new String[]{"ref_list"});
        ModelObject[] dsfiles = dataset.get_ref_list();
        if (dsfiles == null || dsfiles.length == 0) {
            return null;
        }
        ImanFile dsFile = null;
        for (int i = 0; i < dsfiles.length; i++) {
            if (!(dsfiles[i] instanceof ImanFile)) {
                continue;
            }
            dsFile = (ImanFile) dsfiles[i];
            dataManagementService.refreshObjects(new ModelObject[]{dsFile});
            getProperty(dataManagementService, dsFile, "original_file_name");
            String fileName = dsFile.get_original_file_name();
//            CommonTools.checkFileName(fileName);
//            if (!fileName.toLowerCase(Locale.ENGLISH).contains(fileExtensions)) {
//                continue;
//            }

            // 下载数据集
//            FileManagementUtility fileManagementUtility = null;
//            if (fmsUrl != null) {
//                fileManagementUtility = new FileManagementUtility(AppXSession.getConnection(), null, null, new String[]{fmsUrl}, null);
//            } else {
//                fileManagementUtility = new FileManagementUtility(AppXSession.getConnection());
//            }

            GetFileResponse responseFiles = fileManagementUtility.getFiles(new ModelObject[]{dsFile});
            File[] fileinfovec = responseFiles.getFiles();
            File file = fileinfovec[0];
            FileUtil.checkSecurePath(dirPath);
            String filePath = "";
            if (dirPath.endsWith("\\")) {
                filePath = dirPath + fileName;
            } else {
                filePath = dirPath + File.separator + fileName;
            }
            // 判断数据集是否存在
            newfile = new File(filePath);
            if (newfile.exists()) {
                newfile.delete();
            }

            File dstFile = new File(filePath);
            // 复制文件
            copyFile(file, dstFile);
        }
        return newfile;
    }

    public static void copyFile(File sourceFile, File targetFile) throws Exception {
        BufferedInputStream inBuff = null;
        BufferedOutputStream outBuff = null;
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            fileInputStream = new FileInputStream(sourceFile);
            fileOutputStream = new FileOutputStream(targetFile);
            inBuff = new BufferedInputStream(fileInputStream);
            outBuff = new BufferedOutputStream(fileOutputStream);

            byte[] b = new byte[1024 * 5];
            int len;
            while ((len = inBuff.read(b)) != -1) {
                outBuff.write(b, 0, len);
            }
            outBuff.flush();
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException e) {
            }
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
            }
            try {
                if (inBuff != null) {
                    inBuff.close();
                }
            } catch (IOException e) {
            }
            try {
                if (outBuff != null) {
                    outBuff.close();
                }
            } catch (IOException e) {
            }
        }
    }


    public static void modifyProjects(ProjectLevelSecurityService plsService,
                                      TC_Project sourceProject, String projectId, String projectName, boolean isActive) throws Exception {

        com.teamcenter.services.strong.core._2017_05.ProjectLevelSecurity.ModifyProjectsInfo2[] modifyProjectsInfos = new com.teamcenter.services.strong.core._2017_05.ProjectLevelSecurity.ModifyProjectsInfo2[1];
        modifyProjectsInfos[0] = new com.teamcenter.services.strong.core._2017_05.ProjectLevelSecurity.ModifyProjectsInfo2();
        modifyProjectsInfos[0].sourceProject = sourceProject;

        com.teamcenter.services.strong.core._2017_05.ProjectLevelSecurity.ProjectInformation2 projectInfo = new com.teamcenter.services.strong.core._2017_05.ProjectLevelSecurity.ProjectInformation2();
        projectInfo.active = isActive;
        projectInfo.projectId = projectId;
        projectInfo.projectName = projectName;
        modifyProjectsInfos[0].projectInfo = projectInfo;

        System.out.println("projectId：" + projectId + "，projectName：" + projectName);

        //byPass(true);
        com.teamcenter.services.strong.core._2012_09.ProjectLevelSecurity.ProjectOpsResponse response = plsService.modifyProjects2(modifyProjectsInfos);
        //byPass(false);
        ServiceData serviceData = response.serviceData;
        if (serviceData.sizeOfPartialErrors() > 0) {
            throw new Exception(response.serviceData.getPartialError(0).getErrorValues()[0].getMessage());
        }
    }

    /**
     * 设置状态
     *
     * @param obj
     * @param statusName
     * @param operation  "Append"、 "Delete"
     * @throws Exception
     */
    public static void addStatus(WorkflowService wfService, WorkspaceObject[] obj, String statusName, String operation) throws Exception {
        com.teamcenter.services.strong.workflow._2007_06.Workflow.ReleaseStatusInput[] releaseStatusInputs = new com.teamcenter.services.strong.workflow._2007_06.Workflow.ReleaseStatusInput[1];
        com.teamcenter.services.strong.workflow._2007_06.Workflow.ReleaseStatusInput releaseStatusInput = new com.teamcenter.services.strong.workflow._2007_06.Workflow.ReleaseStatusInput();
        releaseStatusInputs[0] = releaseStatusInput;
        com.teamcenter.services.strong.workflow._2007_06.Workflow.ReleaseStatusOption[] releaseStatusOptions = new com.teamcenter.services.strong.workflow._2007_06.Workflow.ReleaseStatusOption[1];
        com.teamcenter.services.strong.workflow._2007_06.Workflow.ReleaseStatusOption releaseStatusOption = new com.teamcenter.services.strong.workflow._2007_06.Workflow.ReleaseStatusOption();
        releaseStatusOptions[0] = releaseStatusOption;
        String appendStatus = "";
        String deleteStatus = "";
        if ("Append".equals(operation)) {
            appendStatus = statusName;
        }
        if ("Delete".equals(operation)) {
            deleteStatus = statusName;
        }
        releaseStatusOption.newReleaseStatusTypeName = appendStatus;
        releaseStatusOption.existingreleaseStatusTypeName = deleteStatus;
        releaseStatusOption.operation = operation;
        releaseStatusInput.objects = obj;
        releaseStatusInput.operations = releaseStatusOptions;
        com.teamcenter.services.strong.workflow._2007_06.Workflow.SetReleaseStatusResponse reponse = wfService.setReleaseStatus(releaseStatusInputs);
        int sizeOfPartialErrors = reponse.serviceData.sizeOfPartialErrors();
        if (sizeOfPartialErrors > 0) {
            String errorMessage = "";
            for (int i = 0; i < sizeOfPartialErrors; i++) {
                errorMessage = errorMessage + reponse.serviceData.getPartialError(i).toString();
            }
            throw new Exception(errorMessage);
        }
    }


    /**
     * 生成ID
     *
     * @param dmService 工具类
     * @param type      类型
     * @return
     */
    public static Map<String, String> generateIdAndVer(DataManagementService dmService, String type) throws Exception {
        com.teamcenter.services.strong.core._2006_03.DataManagement.GenerateItemIdsAndInitialRevisionIdsProperties[] revisionIdsProperties = new com.teamcenter.services.strong.core._2006_03.DataManagement.GenerateItemIdsAndInitialRevisionIdsProperties[1];
        revisionIdsProperties[0] = new com.teamcenter.services.strong.core._2006_03.DataManagement.GenerateItemIdsAndInitialRevisionIdsProperties();
        revisionIdsProperties[0].itemType = type;
        revisionIdsProperties[0].count = 1;
        com.teamcenter.services.strong.core._2006_03.DataManagement.GenerateItemIdsAndInitialRevisionIdsResponse response = dmService.generateItemIdsAndInitialRevisionIds(revisionIdsProperties);
        String info = getErrorMsg(response.serviceData);
        if (StrUtil.isNotEmpty(info)) {
            throw new Exception(info);
        }
        Map<String, String> resultMap = new HashMap<>();
        Map<BigInteger, com.teamcenter.services.strong.core._2006_03.DataManagement.ItemIdsAndInitialRevisionIds[]> map = response.outputItemIdsAndInitialRevisionIds;
        for (Map.Entry<BigInteger, com.teamcenter.services.strong.core._2006_03.DataManagement.ItemIdsAndInitialRevisionIds[]> entry : map.entrySet()) {
            System.out.println("key = " + entry.getKey());
            com.teamcenter.services.strong.core._2006_03.DataManagement.ItemIdsAndInitialRevisionIds[] outputs = entry.getValue();
            for (com.teamcenter.services.strong.core._2006_03.DataManagement.ItemIdsAndInitialRevisionIds result : outputs) {
                String newItemId = result.newItemId;
                System.out.println("==>> newItemId: " + newItemId);
                String newRevId = result.newRevId;
                System.out.println("==>> newRevId: " + newRevId);
                resultMap.put("id", newItemId);
                resultMap.put("version", newRevId);
            }
        }
        return resultMap;
    }


    public static String generateId(DataManagementService dmService, String type) throws Exception {
        com.teamcenter.services.strong.core._2014_10.DataManagement.GenerateIdInput generateIdInput = new com.teamcenter.services.strong.core._2014_10.DataManagement.GenerateIdInput();
        com.teamcenter.services.strong.core._2008_06.DataManagement.CreateInput createInput = new com.teamcenter.services.strong.core._2008_06.DataManagement.CreateInput();
        createInput.boName = type;
        generateIdInput.quantity = 1;
        generateIdInput.propertyName = TCItemConstant.PROPERTY_ITEM_ID;
        generateIdInput.createInput = createInput;
        System.out.print(generateIdInput);
        com.teamcenter.services.strong.core._2014_10.DataManagement.GenerateIdsResponse response = dmService.generateIdsUsingIDGenerationRules(new com.teamcenter.services.strong.core._2014_10.DataManagement.GenerateIdInput[]{generateIdInput});
        String result = TCUtils.getErrorMsg(response.serviceData);
        if (StrUtil.isNotEmpty(result)) {
            throw new Exception(result);
        }
        String id = response.generateIdsOutput[0].generatedIDs[0]; // 生成流水码
        System.out.println("==>> id: " + id);
        return id;
    }


    /**
     * 返回错误信息
     *
     * @param data
     * @return
     */
    public static String getErrorMsg(ServiceData data) {
        String errorMsg = "";
        int errorSize = data.sizeOfPartialErrors();
        if (errorSize > 0) {
            for (int i = 0; i < errorSize; i++) {
                errorMsg += errorMsg + Arrays.toString(data.getPartialError(i).getMessages());
            }
        }
        return errorMsg;
    }


    /**
     * 判断对象是否存在
     *
     * @param dmService 工具类
     * @param objs      对象集合
     * @param name      名称
     * @param type      类型
     * @return
     */
    public static ModelObject checkModelObjExist(DataManagementService dmService, ModelObject[] objs, String name, String type, String propName) {
        if (null == objs) {
            return null;
        }
        Optional<ModelObject> findAny = Stream.of(objs).filter(obj -> {
            try {
                String objectName = TCUtils.getPropStr(dmService, obj, propName);
                String objectType = obj.getTypeObject().getName();
                if (name.equals(objectName) && type.equals(objectType)) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }).findAny();
        if (findAny.isPresent()) {
            return findAny.get();
        }
        return null;
    }

    /**
     * 判断对象是否存在
     *
     * @param dmService 工具类
     * @param objs      对象集合
     * @param type      类型
     * @return
     */
    public static ModelObject checkModelObjTypeExist(DataManagementService dmService, ModelObject[] objs, String type) {
        if (null == objs) {
            return null;
        }
        Optional<ModelObject> findAny = Stream.of(objs).filter(obj -> {
            try {
                String objectType = obj.getTypeObject().getName();
                if (type.equals(objectType)) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }).findAny();
        if (findAny.isPresent()) {
            return findAny.get();
        }
        return null;
    }


    public static Folder findFolderByUid(DataManagementService dmService, String uid) throws ServiceException {
        ServiceData serviceData = dmService.loadObjects(new String[]{uid});
        if (serviceData.sizeOfPartialErrors() > 0) {
            throw new ServiceException(serviceData.getPartialError(0).toString());
        }
        return (Folder) serviceData.getPlainObject(0);
    }

    public static Folder createFolder(DataManagementService dmService, String folderType, Map<String, String> propMap) {
        Folder folder = null;
        try {
            CreateIn[] createIns = new CreateIn[1];
            createIns[0] = new CreateIn();
            CreateInput createInput = new CreateInput();
            createInput.boName = folderType;
            createInput.stringProps = propMap;
            createIns[0].data = createInput;
            DataManagement.CreateResponse response = dmService.createObjects(createIns);
            folder = (Folder) response.output[0].objects[0];
        } catch (Exception e) {
            LogFactory.get().error(e);
        }
        return folder;
    }

    public static void addContents(DataManagementService dmService, ModelObject primaryObject,
                                   ModelObject secondaryObject, String relationshipName) throws ServiceException {
        Relationship[] relationships = new Relationship[1];
        relationships[0] = new Relationship();
        relationships[0].primaryObject = primaryObject;
        relationships[0].secondaryObject = secondaryObject;
        relationships[0].relationType = relationshipName;//"contents";
        dmService.createRelations(relationships);
    }

    public static String createTCFolder(DataManagementService dmService, String parentId, String childName, String desc) {
        String puid = "";
        try {
            Folder parentFolder = findFolderByUid(dmService, parentId);
            getProperty(dmService, parentFolder, "object_type");
            String parentFolderType = parentFolder.get_object_type();
            String childFolderType = "D9_ArchiveFolder";
            if (parentFolderType.equals("D9_PlatformFoundFolder")) {
                childFolderType = "D9_FunctionFolder";
            }
            if (parentFolderType.equals("D9_FunctionFolder")) {
                childFolderType = "D9_PhaseFolder";
            }
            Map<String, String> propMap = new HashMap<>();
            propMap.put("object_name", childName);
            if (StrUtil.isNotBlank(desc)) {
                propMap.put("object_desc", desc);
            }
            Folder childFolder = createFolder(dmService, childFolderType, propMap);
            puid = childFolder.getUid();
            addContents(dmService, parentFolder, childFolder, "contents");
        } catch (Exception e) {
            LogFactory.get().error(e);
        }
        return puid;
    }


    //创建数据集
    public static void createDataset(DataManagementService dmService, ItemRevision itemRev, String datasetName, String datasetDesc) throws ServiceException {
        Dataset dataset = null;
        DataManagement.DatasetProperties2[] datasetProps = new DataManagement.DatasetProperties2[1];
        DataManagement.DatasetProperties2 datasetProp = new DataManagement.DatasetProperties2();

        datasetProp.clientId = "datasetWriteTixTestClientId";
        datasetProp.type = "HTML";
        datasetProp.name = datasetName;
        datasetProp.description = datasetDesc;
        datasetProps[0] = datasetProp;
        com.teamcenter.services.strong.core._2006_03.DataManagement.CreateDatasetsResponse dsResp = dmService.createDatasets2(datasetProps);
        dataset = dsResp.output[0].dataset;

        Relationship[] relationships = new Relationship[1];
        Relationship relationship = new Relationship();

        relationship.clientId = "";
        relationship.primaryObject = itemRev;
        relationship.secondaryObject = dataset;
        relationship.relationType = "IMAN_specification";
        relationship.userData = null;
        relationships[0] = relationship;
        com.teamcenter.services.strong.core._2006_03.DataManagement.CreateRelationsResponse crResponse = dmService.createRelations(relationships);
        ServiceData crServiceData = crResponse.serviceData;
        if (crServiceData.sizeOfPartialErrors() > 0) {
            throw new ServiceException(crServiceData.getPartialError(0).toString());
        }

        dmService.refreshObjects(new ModelObject[]{dataset});
    }

    public static Item createDocument(DataManagementService dmService, String itemId, String itemType, String itemName, Map<String, String> propMap) throws Exception {
        com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties[] itemProps = new com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties[1];
        com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties itemProperty = new com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties();
        itemProperty.clientId = itemId + "--" + getRandomNumber();
        itemProperty.itemId = itemId;
        itemProperty.revId = "01";
        itemProperty.name = itemName;
        itemProperty.type = itemType;
        itemProperty.description = "";
        itemProperty.uom = "";

        itemProperty.extendedAttributes = new com.teamcenter.services.strong.core._2006_03.DataManagement.ExtendedAttributes[1];
        com.teamcenter.services.strong.core._2006_03.DataManagement.ExtendedAttributes theExtendedAttr = new com.teamcenter.services.strong.core._2006_03.DataManagement.ExtendedAttributes();
        theExtendedAttr.attributes = propMap;
        theExtendedAttr.objectType = itemType;
        itemProperty.extendedAttributes[0] = theExtendedAttr;
        itemProps[0] = itemProperty;

        com.teamcenter.services.strong.core._2006_03.DataManagement.CreateItemsResponse response = dmService.createItems(itemProps, null, "");
        ServiceData serviceData = response.serviceData;
        if (serviceData.sizeOfPartialErrors() > 0) {
            throw new ServiceException(serviceData.getPartialError(0).toString());
        }
        return response.output[0].item;
    }

    public static Item createDocument(DataManagementService dmService, String itemId, String itemType, String itemName, String revId, Map<String, String> propMap) throws Exception {
        com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties[] itemProps = new com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties[1];
        com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties itemProperty = new com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties();
        itemProperty.clientId = itemId + "--" + getRandomNumber();
        itemProperty.itemId = itemId;
        itemProperty.revId = revId;
        itemProperty.name = itemName;
        itemProperty.type = itemType;
        itemProperty.description = "";
        itemProperty.uom = "";

        itemProperty.extendedAttributes = new com.teamcenter.services.strong.core._2006_03.DataManagement.ExtendedAttributes[1];
        com.teamcenter.services.strong.core._2006_03.DataManagement.ExtendedAttributes theExtendedAttr = new com.teamcenter.services.strong.core._2006_03.DataManagement.ExtendedAttributes();
        theExtendedAttr.attributes = propMap;
        theExtendedAttr.objectType = itemType;
        itemProperty.extendedAttributes[0] = theExtendedAttr;
        itemProps[0] = itemProperty;

        com.teamcenter.services.strong.core._2006_03.DataManagement.CreateItemsResponse response = dmService.createItems(itemProps, null, "");
        ServiceData serviceData = response.serviceData;
        if (serviceData.sizeOfPartialErrors() > 0) {
            throw new ServiceException(serviceData.getPartialError(0).toString());
        }
        return response.output[0].item;
    }


    /**
     * 判断用户所有者是否相同
     *
     * @param dmService
     * @param user
     * @param obj
     * @return
     * @throws NotLoadedException
     */
    public static boolean checkObjectOwner(DataManagementService dmService, ModelObject obj, User user) throws NotLoadedException {
        if (null == obj) {
            return false;
        }

        if (null == user) {
            return false;
        }
        TCUtils.refreshObject(dmService, new ModelObject[]{obj});
        ModelObject ownerUser = TCUtils.getPropModelObject(dmService, obj, TCScheduleConstant.REL_OWNERING_USER);
        return user.getUid().equals(ownerUser.getUid());
    }

    /**
     * 根据专案文件夹的pUid查询专案是否归档在废弃的文件夹下
     *
     * @param dmService
     * @param pUid
     * @return
     */
    public static boolean isDiscard(DataManagementService dmService, String pUid) throws NotLoadedException {
        ModelObject object = findObjectByUid(dmService, pUid);
        ModelObject[] parentObjs = getParent(dmService, (WorkspaceObject) object, "");
        if (ObjectUtil.isNull(parentObjs) || parentObjs.length == 0) {
            return false;
        }
        Set<String> set = CollUtil.newHashSet("PRT Projects-廢棄", "DT Projects-廢棄", "MNT Projects(6/1-6/13)廢棄", "MNT Projects(6/13-)廢棄");
        for (ModelObject parentObj : parentObjs) {
            getProperty(dmService, parentObj, "object_name");
            refreshObject(dmService, parentObj);
            String objectName = parentObj.getPropertyObject("object_name").getStringValue();
            if (set.contains(objectName)) {
                return true;
            }
        }
        return false;
    }

    public static ModelObject[] getParent(DataManagementService dmService, WorkspaceObject child, String refType) {
        WorkspaceObject children[] = new WorkspaceObject[1];
        children[0] = child;
        com.teamcenter.services.strong.core._2007_01.DataManagement.WhereReferencedResponse resp = dmService.whereReferenced(children, 1);
        List temp = new ArrayList();
        com.teamcenter.services.strong.core._2007_01.DataManagement.WhereReferencedInfo awherereferencedinfo[];
        int k = (awherereferencedinfo = resp.output[0].info).length;
        for (int j = 0; j < k; j++) {
            com.teamcenter.services.strong.core._2007_01.DataManagement.WhereReferencedInfo info = awherereferencedinfo[j];
            if (refType.equalsIgnoreCase("")) {
                temp.add(info.referencer);
            } else if (info.relation.equalsIgnoreCase(refType)) {
                temp.add(info.referencer);
            }
        }


        ModelObject result[] = new ModelObject[temp.size()];
        for (int i = 0; i < temp.size(); i++) {
            result[i] = (ModelObject) temp.get(i);
        }
        return result;
    }
}
