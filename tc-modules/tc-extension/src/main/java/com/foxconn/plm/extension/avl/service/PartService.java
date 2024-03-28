package com.foxconn.plm.extension.avl.service;

import com.foxconn.plm.extension.avl.domain.PartModel;
import com.foxconn.plm.extension.avl.mapper.PartMapper;
import com.foxconn.plm.extension.client.TcService;
import com.foxconn.plm.extension.config.TCPropertes;
import com.foxconn.plm.tcapi.config.TCUserEnum;
import com.foxconn.plm.tcapi.domain.dto.AjaxResult;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core._2006_03.DataManagement;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.exceptions.NotLoadedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PartService {
    private static Logger log = LogManager.getLogger(PartService.class);
    @Resource
    private PartMapper partMapper;


    private final static String[] VARIABLE_ATTR = {"d9_CompApproStatus", "d9_ApproStatus", "d9_HalogenFreeStatus"};

    @Value("${tc.avl.folder-uid}")
    private String folderUid;


    @Resource
    private TcService tcService;


    private final static String ATTR_PREFIX = "d9_";

    //@Scheduled(cron = "0 0 1 * * ?") // 每天上午1点执行
    //@Scheduled(cron = "0 */5 * * * ?") // 每5分钟执行一次 测试用
    public void handleSync() {
        log.info("start avl Scheduled  ********** ");
        List<PartModel> parts = findParts(getFromData());
        log.info("search avl data size :: " + parts.size());
        syncTc(parts);
        log.info("end avl Scheduled  ******** ");
    }


    @PostConstruct
    public void SyncAll() {
        log.info("start avl SyncAll  ********** ");
        List<PartModel> parts = partMapper.findAllParts();
        log.info("search avl data size :: " + parts.size());
        syncTc(parts);
        log.info("end avl SyncAll  ******** ");
    }

    public void syncTc(List<PartModel> parts) {
        Map<PartModel, ItemRevision> resultMap = new HashMap<>();
        parts.parallelStream().forEach(part -> {
            resultMap.put(part, this.queryItem(part));
        });
        List<PartModel> newItems = new ArrayList<>();
        resultMap.forEach((k, v) -> {
            if (v == null && !"R".equalsIgnoreCase(k.getApprovalStatus()) && Pattern.matches("^\\S{9}-\\S{3}-\\S{1}$", k.getHfPn())) {
                newItems.add(k);
            }
        });
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        if (newItems.size() > 0) {
            //测试
            log.info("start avl createItems  -->>>  -->>> " + newItems.size());
            int tempSize = 0;
            for (int i = 0; i < newItems.size(); i++) {
                if (i != 0 && i % 100 == 0) {
                    List<PartModel> newList = newItems.subList(tempSize, i);
                    executorService.execute(() -> {
                        log.info("running create items  -->>>" + Thread.currentThread().getName());
                        createItems(newList);
                        log.info("end create items  -->>>");
                    });
                    tempSize = i;
                }
                if (i == newItems.size() - 1) {
                    List<PartModel> newList = newItems.subList(tempSize, newItems.size());
                    executorService.execute(() -> {
                        log.info("running create items  -->>>" + Thread.currentThread().getName());
                        createItems(newList);
                        log.info("end create items  -->>>");
                    });
                }
            }
            log.info("end avl createItems @@@@@@@@@@@@");
        }
        resultMap.keySet().removeIf(newItems::contains);
        if (resultMap.size() > 0) {
            log.info("start modify Item ------------- >>>> ");
            modifyItem(resultMap, executorService);
            log.info("end avl modify Item @@@@@@@@@@@@");
        }
    }

    public LocalDateTime getFromData() {
//        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        LocalDateTime date = LocalDateTime.parse("2019-08-17 00:00:00", dtf);
        LocalDateTime nowDate = LocalDateTime.now();
        nowDate = nowDate.minusDays(1);
        return nowDate;
    }

    public void modifyItem(Map<PartModel, ItemRevision> items, ExecutorService executorService) {
        TCSOAServiceFactory tcsoaServiceFactory = null;
        try {
            tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            DataManagementService dataManagementService = tcsoaServiceFactory.getDataManagementService();
            for (Map.Entry<PartModel, ItemRevision> entry : items.entrySet()) {
                if (entry.getValue() != null) {
                    Map<String, String> changeMap = getChangeData(dataManagementService, getTCPropMap(entry.getKey(), f -> f.startsWith(ATTR_PREFIX)), entry.getValue());
                    if (!changeMap.isEmpty()) {
                        executorService.execute(() -> {
                            try {
                                ItemRevision itemRevision = entry.getValue();
                                //去掉升版逻辑
//                        if (isReleased(dataManagementService, itemRevision)) {
//                            itemRevision = (ItemRevision) doRevise(dataManagementService, itemRevision);
//                        }
                                modifyPropertyForItem(dataManagementService, changeMap, itemRevision);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });

                    }
                }
            }
        } finally {
            try {
                if (tcsoaServiceFactory != null) {
                    tcsoaServiceFactory.logout();
                }

            } catch (Exception e) {
            }
//        items.entrySet().parallelStream().filter(e -> "dba".equalsIgnoreCase(getItemGroupName(dataManagementService, e.getValue()).toLowerCase())).forEach(e -> {
//            Map<String, String> changeMap = getChangeData(dataManagementService, getTCPropMap(e.getKey(), f -> f.startsWith(ATTR_PREFIX)), e.getValue());
//            if (!changeMap.isEmpty()) {
//                log.info("avl modifyItem :: " + e.getKey().getHfPn() + " changeMap:: " + changeMap);
//                modifyPropertyForItem(dataManagementService, changeMap, e.getValue());
//            }
//        });
        }
    }

    public Map<String, String> getChangeData(DataManagementService dataManagementService, Map<String, String> propMap, ItemRevision item) {
        dataManagementService.getProperties(new ModelObject[]{item}, VARIABLE_ATTR);
        Map<String, String> changMap = new HashMap<>();
        for (String key : VARIABLE_ATTR) {
            String newValue = propMap.getOrDefault(key, "");
            try {
                String oldValue = item.getPropertyObject(key).getStringValue();
                if (!newValue.equalsIgnoreCase(oldValue)) {
                    changMap.put(key, newValue);
                }
            } catch (NotLoadedException e) {
                e.printStackTrace();
            }
        }
        return changMap;
    }

    public void modifyPropertyForItem(DataManagementService dataManagementService, Map<String, String> tcPropMap, ItemRevision item) {
        HashMap<String, com.teamcenter.services.strong.core._2007_01.DataManagement.VecStruct> propMap = new HashMap<>();
        tcPropMap.forEach((k, v) -> {
            com.teamcenter.services.strong.core._2007_01.DataManagement.VecStruct vecStruct = new com.teamcenter.services.strong.core._2007_01.DataManagement.VecStruct();
            vecStruct.stringVec = new String[]{v};
            propMap.put(k, vecStruct);
        });
        ServiceData response = dataManagementService.setProperties(new ModelObject[]{item}, propMap);
        int errorSize = response.sizeOfPartialErrors();
        if (errorSize > 0) {
            log.error("avl modify Item error size :: " + errorSize);
            for (int i = 0; i < errorSize; i++) {
                log.error("avl modify Item error info  :: " + Arrays.toString(response.getPartialError(i).getMessages()));
            }
        }
    }

    public String getItemGroupName(DataManagementService dataManagementService, ItemRevision item) {
        String groupName = "";
        TCUtils.getProperty(dataManagementService, item, "owning_group");
        try {
            Group group = (Group) item.get_owning_group();
            TCUtils.getProperty(dataManagementService, group, "name");
            groupName = group.get_name();
        } catch (NotLoadedException e) {
            e.printStackTrace();
        }
        return groupName;
    }

    public void createItems(List<PartModel> newItems) {
        TCSOAServiceFactory tcsoaServiceFactory = null;
        try {
            tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            if (newItems.size() > 0) {
                log.info("running create items  -->>>");
                DataManagementService dataManagementService = tcsoaServiceFactory.getDataManagementService();
                List<Map<String, String>> newMapList = newItems.stream().map(e ->
                {
                    setExtraAttr(e);
                    Map<String, String> propMap = getTCPropMap(e, f -> !f.startsWith(ATTR_PREFIX));
                    //propMap.put("item_revision_id", "A");
                    //TCUtils.generateVersion(dataManagementService, "@", e.getObjectType());
                    return propMap;
                }).collect(Collectors.toList());
                DataManagement.CreateItemsResponse response = TCUtils.createItems(dataManagementService, newMapList, getFolder());
                int errorSize = response.serviceData.sizeOfPartialErrors();
                if (errorSize > 0) {
                    log.error("create item  response error size : " + errorSize);
                    for (int i = 0; i < errorSize; i++) {
                        log.error("create item response error info : " + Arrays.toString(response.serviceData.getPartialError(i).getMessages()));
                    }
                }
                if (response.output.length > 0) {
                    DataManagement.CreateItemsOutput[] output = response.output;
                    List<ItemRevision> revList = new ArrayList<>();
                    for (DataManagement.CreateItemsOutput object : output) {
                        ItemRevision itemRev = object.itemRev;
                        revList.add(itemRev);
                    }
                    Map<String, Map<String, String>> itemMap = newItems.stream().collect(Collectors.toMap(PartModel::getHfPn, e -> getTCPropMap(e, f -> f.startsWith(ATTR_PREFIX))));
                    ItemRevision[] itemRevs = revList.toArray(new ItemRevision[0]);
                    tcsoaServiceFactory.getDataManagementService().getProperties(itemRevs, new String[]{"item_id"});
                    for (ItemRevision itemRev : itemRevs) {
                        try {
                            String itemId = itemRev.get_item_id();
                            Map<String, String> propMap = itemMap.get(itemId);
                            modifyPropertyForItem(tcsoaServiceFactory.getDataManagementService(), propMap, itemRev);
                        } catch (NotLoadedException e) {
                            e.printStackTrace();
                        }
                    }
                    tcsoaServiceFactory.getDataManagementService().refreshObjects(itemRevs);
                    //发行
                    try {
                        TCUtils.addStatus(tcsoaServiceFactory.getWorkflowService(), itemRevs, "D9_Release");
                    } catch (ServiceException e) {
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            try {
                if (tcsoaServiceFactory != null) {
                    tcsoaServiceFactory.logout();
                }
            } catch (Exception e) {
            }
        }
    }


    public ModelObject getFolder() {
        TCSOAServiceFactory tcsoaServiceFactory = null;
        ModelObject avlFolder = null;
        try {
            tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            avlFolder = TCUtils.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), folderUid);
            if (avlFolder == null) {
                try {
                    avlFolder = tcsoaServiceFactory.getUser().get_newstuff_folder();
                } catch (NotLoadedException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            try {
                if (tcsoaServiceFactory != null) {
                    tcsoaServiceFactory.logout();
                }
            } catch (Exception e) {
            }
        }
        return avlFolder;
    }

    public List<PartModel> findParts(LocalDateTime date) {
        return partMapper.findPartsByDate(date);
    }


    private ItemRevision queryItem(PartModel part) {
        TCSOAServiceFactory tcsoaServiceFactory = null;
        ItemRevision itemRevision = null;
        try {
            tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            String itemID = part.getHfPn();
            System.out.println("queryItem itemID ::: " + itemID);
            Item item = TCUtils.queryItemByIDOrName(tcsoaServiceFactory.getSavedQueryService(), tcsoaServiceFactory.getDataManagementService(), itemID, null);
            if (item != null) {
                itemRevision = TCUtils.getItemLatestRevision(tcsoaServiceFactory.getDataManagementService(), item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (tcsoaServiceFactory != null) {
                    tcsoaServiceFactory.logout();
                }
            } catch (Exception e) {
            }
        }
        return itemRevision;
    }

    private Map<String, String> getTCPropMap(PartModel partModel, Predicate<String> predicate) {
        System.out.println("start -->>  getTCPropMap");
        Map<String, String> tcPropMap = new HashMap<>();
        Field[] fields = partModel.getClass().getDeclaredFields();
        for (Field field : fields) {
            ReflectionUtils.makeAccessible(field);
            TCPropertes tcPropertes = field.getAnnotation(TCPropertes.class);
            if (tcPropertes != null) {
                String tcProp = tcPropertes.tcProperty();
                if (!tcProp.isEmpty()) {
                    try {
                        Object o = field.get(partModel);
                        if (o != null) {
                            if (predicate.test(tcProp)) {
                                tcPropMap.put(tcProp, (String) o);
                            }
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        System.out.println("tcPropMap :: " + tcPropMap);
        return tcPropMap;
    }

    public boolean isReleased(DataManagementService dataManagementService, ModelObject object) throws NotLoadedException {
        TCUtils.getProperty(dataManagementService, object, "release_status_list");
        List list = object.getPropertyObject("release_status_list").getModelObjectListValue();
        if (list.size() > 0) {
            return true;
        }
        return false;
    }

    public ModelObject doRevise(DataManagementService dataManagementService, ModelObject object) throws Exception {
        String versionRule = "";
        dataManagementService.getProperties(new ModelObject[]{object}, new String[]{"item_revision_id", "object_type", "object_name"});
        String version = object.getPropertyObject("item_revision_id").getStringValue();
        if (version.matches("[0-9]+")) {
            versionRule = "NN";
        } else if (version.matches("[a-zA-Z]+")) {
            versionRule = "@";
        }
        String objectType = object.getPropertyObject("object_type").getStringValue();
        String objectName = object.getPropertyObject("object_name").getStringValue();
        String newRevsionId = TCUtils.reviseVersion(dataManagementService, versionRule, objectType, object.getUid());
        return TCUtils.reviseItemRev(dataManagementService, object, objectName, newRevsionId);
    }


    public void setExtraAttr(PartModel partModel) {
        AjaxResult ajaxRsult = tcService.getMaterialGroupAndBaseUnit(partModel.getHfPn());
        List list = (List) ajaxRsult.get(AjaxResult.DATA_TAG);
        if (list != null && list.size() > 0) {
            Map<String, String> map = (Map) list.get(0);
            partModel.setMaterialGroup(map.getOrDefault("materialGroup", ""));
            partModel.setUnit(map.getOrDefault("baseUnit", ""));
        }
    }

    public ItemRevision[] batchCreateItem(DataManagementService dataManagementService, List<Map<String, String>> propList, ModelObject folder) throws ServiceException {

        if (propList != null) {
            com.teamcenter.services.strong.core._2008_06.DataManagement.CreateIn[] createObjects = new com.teamcenter.services.strong.core._2008_06.DataManagement.CreateIn[propList.size()];
            for (int i = 0; i < propList.size(); i++) {
                Map<String, String> propMap = propList.get(i);
                com.teamcenter.services.strong.core._2008_06.DataManagement.CreateIn create = new com.teamcenter.services.strong.core._2008_06.DataManagement.CreateIn();
                create.clientId = "";
                com.teamcenter.services.strong.core._2008_06.DataManagement.CreateInput input = new com.teamcenter.services.strong.core._2008_06.DataManagement.CreateInput();
                input.boName = propMap.get("object_type");
                HashMap<String, String> map = new HashMap<>();
                map.put("object_name", propMap.get("object_name"));
                map.put("item_id", propMap.get("item_id"));
                map.put("object_desc", propMap.getOrDefault("object_desc", ""));
                input.stringProps = map;
                create.data = input;
                createObjects[i] = create;
            }
            com.teamcenter.services.strong.core._2008_06.DataManagement.CreateResponse responseObjects = dataManagementService.createObjects(createObjects);
            int errorSize = responseObjects.serviceData.sizeOfPartialErrors();
            if (errorSize > 0) {
                log.error("create item  response error size : " + errorSize);
                for (int i = 0; i < errorSize; i++) {
                    log.error("create item response error info : " + Arrays.toString(responseObjects.serviceData.getPartialError(i).getMessages()));
                }
            }
            com.teamcenter.services.strong.core._2008_06.DataManagement.CreateOut[] output = responseObjects.output;
            Item[] items = new Item[output.length];
            ItemRevision[] itemRevs = new ItemRevision[output.length];
            for (int i = 0; i < output.length; i++) {
                ModelObject[] objects = output[i].objects;
                for (ModelObject object : objects) {
                    if (object instanceof Item) {
                        items[i] = (Item) object;
                    } else if (object instanceof ItemRevision) {
                        itemRevs[i] = (ItemRevision) object;
                    }
                }
            }
            TCUtils.addContents(dataManagementService, folder, items);
            return itemRevs;
        }
        return null;
    }

    public static void main(String[] args) {
    }
}
