package com.foxconn.plm.integrate.cis.scheduled;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.integrate.cis.config.*;
import com.foxconn.plm.integrate.cis.domain.MaterialRequest;
import com.foxconn.plm.integrate.cis.domain.PartEntity;
import com.foxconn.plm.integrate.cis.domain.ThreeDDrawingBean;
import com.foxconn.plm.integrate.cis.service.ICISService;
import com.foxconn.plm.integrate.cis.utils.CISBatcher;
import com.foxconn.plm.integrate.cis.utils.CISUtils;
import com.foxconn.plm.integrate.cis.utils.CisPropertitesUtil;
import com.foxconn.plm.integrate.config.properties.CISAttrConfig;
import com.foxconn.plm.integrate.log.domain.ActionLogRp;
import com.foxconn.plm.integrate.log.mapper.ActionLogMapper;
import com.foxconn.plm.integrate.log.service.ActionLogServiceImpl;
import com.foxconn.plm.integrate.spas.domain.D9Constants;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.collect.CollectUtil;
import com.foxconn.plm.utils.date.DateUtil;
import com.foxconn.plm.utils.net.HttpUtil;
import com.foxconn.plm.utils.string.StringUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.loose.core.SessionService;
import com.teamcenter.services.strong.classification.ClassificationService;
import com.teamcenter.services.strong.classification._2007_01.Classification;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core._2008_06.DataManagement;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.services.strong.workflow.WorkflowService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.Folder;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.exceptions.NotLoadedException;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CISSynScheduledTask {
    private static Log log = LogFactory.get();
    private static final SimpleDateFormat _dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final int BATCHER_SIZE = 10;
    private static final String sdfm = "yyyy-MM-dd HH:mm:ss.SSS";
    private static List<CISPropertyType> _cisPropLst = new ArrayList<>();
    private static List<CISPropertyType> _cisDellPropLst = new ArrayList<>();

    static {
        // 动态属性
        _cisPropLst.add(CISPropertyType.FOXCONN_PART_NUMBER);
        _cisPropLst.add(CISPropertyType.FOXCONN_PART_NUMBER_NODT);
        _cisPropLst.add(CISPropertyType.STANDARD_PN);

        _cisDellPropLst.add(CISPropertyType.FOXCONN_PART_NUMBER);
        _cisDellPropLst.add(CISPropertyType.STANDARD_PN);
    }

    private Folder _dt_folder = null;
    private Folder _nodt_folder = null;
    private Folder _standard_pn_folder = null;
    private Folder _tc_sync_folder = null;

    private List<String> _cisSyncLst = null;
    private List<String> _cisNotSyncLst = null;
    //    private List<String> _cisExistLst = null;
    private List<String> _cisDellSyncLst = null;
    private List<String> _cisDellNotSyncLst = null;
//    private List<String> _cisDellExistLst = null;

    private List<ActionLogRp> _actionLogs = null;

    private Map<String, ItemRevision> _materialMap = null;


    @Resource(name = "CISServiceImpl")
    private ICISService cisService;

    @Resource(name = "CISDellServiceImpl")
    private ICISService cisDellService;

    @Resource
    private ActionLogMapper actionLogMapper;

    @Resource
    private CISAttrConfig cisAttrConfig;

    @Resource
    private ActionLogServiceImpl actionLogService;

    @Value("${cis.mail-addr}")
    private String mailAddrs;

    @Value("${cis.mail-url}")
    private String mailUrl;

    @Value("${cis.material-url}")
    private String materialUrl;

    @Value("${cis.globalPath}")
    private String defaultPath;
    @Value("${cis.dellPath}")
    private String dellPath;

    //  @PostConstruct
    //@Scheduled(cron = "0 */30 * * * ?") //cron="0 0 0/1 * * ?" 每小时执行一次 //cron="0 */3 * * * ?" 每三分钟执行一次
    @XxlJob("TcSyncCISSchedule")
    public void syncCIS() {
        TCSOAServiceFactory tcSOAServiceFactory = null;
        try {

            // 初始化
            _cisSyncLst = new ArrayList<>();
            _cisNotSyncLst = new ArrayList<>();
            _cisDellSyncLst = new ArrayList<>();
            _cisDellNotSyncLst = new ArrayList<>();
            _actionLogs = new ArrayList<>();
            _materialMap = new HashMap<>();

            tcSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);

            SessionService sessionService = tcSOAServiceFactory.getSessionService();
            DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();
            SavedQueryService savedQueryService = tcSOAServiceFactory.getSavedQueryService();
            writeLog("同步CIS数据库定时任务开始：");

            TCUtils.byPass(sessionService, true);

            // 查找指定文件夹对象
            _dt_folder = getFolder("PDMCIS_TC_Sync_DT", savedQueryService);
            _nodt_folder = getFolder("PDMCIS_TC_Sync_NoDT", savedQueryService);
            _standard_pn_folder = getFolder("PDMCIS_TC_Sync_STANDARD_PN", savedQueryService);
            _tc_sync_folder = getFolder("PDMCISForDell_TC_Sync", savedQueryService);

            // 获取未同步数据
            writeLog("获取CIS数据库数据开始：");
            List<PartEntity> partLstForCIS = cisService.getNotSyncPart();
            writeLog("获取CIS数据库【" + partLstForCIS.size() + "】条数据完成：");
            writeLog("获取CISDell数据库数据开始：");
            List<PartEntity> partLstForCISDell = cisDellService.getNotSyncPart();
            writeLog("获取CISDell数据库【" + partLstForCISDell.size() + "】条数据完成：");

            // CIS同步数据
            writeLog("CIS数据库同步开始：");
            processDataSyn(_cisPropLst, partLstForCIS, CISType.CIS, tcSOAServiceFactory);
            writeLog("CIS数据库同步完成：");
            // CISDell同步数据
            writeLog("CISDell数据库同步开始：");
            processDataSyn(_cisDellPropLst, partLstForCISDell, CISType.CIS_DELL, tcSOAServiceFactory);
            writeLog("CISDell数据库同步完成：");

            // 更新物料属性
            writeLog("CIS更新物料属性开始：");
            updateMaterialProp(dataManagementService);
            writeLog("CIS更新物料属性完成：");

            if (_cisSyncLst.size() > 0 || _cisNotSyncLst.size() > 0 || _cisDellSyncLst.size() > 0 || _cisDellNotSyncLst.size() > 0) {
                writeLog("自动同步PDMCIS新增料件邮件通知开始：");
                sendMail();
                writeLog("自动同步PDMCIS新增料件邮件通知完成：");
            }

            // 记录效益报表日志
            cisActionLog();

            log.info("获取CIS数据库数据开始：");
            syncECADHint(dataManagementService, savedQueryService);

            List<ActionLogRp> recordCisSyncTCList = cisService.recordCisSyncTC(dataManagementService, savedQueryService); // 获取CIS同步TC的记录
            if (recordCisSyncTCList != null) {
                recordCisSyncTCList.forEach(actionLogRp -> {
                    Integer count = actionLogMapper.getCISActionLogRecord(actionLogRp);
                    if (count == 0) {
                        actionLogMapper.insertCISPart(actionLogRp); // 将CIS共用料信息保存到TC中间表CIS_ACTION_LOG
                    }
                });
            }

            TCUtils.byPass(sessionService, false);
        } catch (Exception e) {
            e.printStackTrace();
            writeLog("同步CIS数据库定时任务出错：" + e.getMessage());
            XxlJobHelper.handleFail(e.getLocalizedMessage());
        } finally {
            try {
                tcSOAServiceFactory.logout();//退出TC
            } catch (Exception e) {
                log.error("TC 退出失败", e);
            }
            writeLog("同步CIS数据库定时任务结束：");
        }
    }

    /**
     * 同步ECAD_Hint信息
     *
     * @return
     * @throws Exception
     */
    private void syncECADHint(DataManagementService dataManagementService, SavedQueryService savedQueryService) throws Exception {
        log.info("********** ecad_hint.map製作效率匯總邏輯 开始同步 **********");
        List<ThreeDDrawingBean> threeDDrawingRecordAll = new ArrayList<>();
        threeDDrawingRecordAll.addAll(cisService.getThreeDDrawingRecord());
        threeDDrawingRecordAll.addAll(cisDellService.getThreeDDrawingRecord());
        if (CollectUtil.isEmpty(threeDDrawingRecordAll)) {
            log.info("ecad_hint.map製作效率不存在待同步的记录");
            return;
        }
        log.info("threeDDrawingRecord-->" + threeDDrawingRecordAll);
        threeDDrawingRecordAll.removeIf(bean -> StringUtil.isEmpty(bean.getMfg()) && StringUtil.isEmpty(bean.getMfgPN())); // 移除mfg和mfg PN不存在的记录
        if (CollectUtil.isEmpty(threeDDrawingRecordAll)) {
            return;
        }
        List<ActionLogRp> list = Collections.synchronizedList(new ArrayList<ActionLogRp>());
        threeDDrawingRecordAll.parallelStream().forEach(bean -> {
            String mfg = bean.getMfg();
            System.out.println("==>> mfg: " + mfg);
            String mfgPN = bean.getMfgPN();
            System.out.println("==>> mfgPN: " + mfgPN);
            String itemId = bean.getItemId();
            if (StringUtil.isNotEmpty(itemId)) {
                itemId = itemId.substring(0, itemId.lastIndexOf("_"));
            } else {
                itemId = "*";
            }
            System.out.println("==>> hhpn: " + itemId);
            List<Item> hhpnList = CISUtils.getHHPN(savedQueryService, dataManagementService, itemId, mfg, mfgPN);
            hhpnList = hhpnList.stream().filter(CollectUtil.distinctByKey(item -> item.getUid())).collect(Collectors.toList()); // 移除相同的对象
            if (CollectUtil.isNotEmpty(hhpnList)) {
                hhpnList.forEach(item -> {
                    ActionLogRp actionLogRp = new ActionLogRp();
                    actionLogRp.setCreatorName(bean.getCreator());
                    actionLogRp.setFunctionName(CISConstants.ECAD_BENEFIT_NAME);
                    try {
                        ItemRevision itemRev = TCUtils.getItemLatestRevision(dataManagementService, item);
                        TCUtils.getProperties(dataManagementService, itemRev, new String[]{"item_id", "item_revision_id"});
                        actionLogRp.setItemId(itemRev.get_item_id());
                        actionLogRp.setRev(itemRev.get_item_revision_id());
                        actionLogRp.setRevUid(itemRev.getUid());
                        actionLogRp.setStartTime(bean.getStartTime());
                        if (StringUtil.isEmpty(bean.getEndTime())) {
                            actionLogRp.setEndTime(DateUtil.addTime(new SimpleDateFormat(sdfm).parse(bean.getStartTime()), sdfm, RandomUtil.randomLong(100, 300)));
                        } else {
                            actionLogRp.setEndTime(bean.getEndTime());
                        }
                        list.add(actionLogRp);
                    } catch (NotLoadedException e) {
                        e.printStackTrace();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                });
            }
        });

        List<ActionLogRp> filterList = list.stream().filter(CollectUtil.distinctByKey(actionLogRp -> actionLogRp.getItemId() + actionLogRp.getRev() + actionLogRp.getRevUid())).collect(Collectors.toList()); // 去除重复的料号对象
        filterList.forEach(actionLogRp -> {
            Integer count = actionLogMapper.getCISActionLogRecord(actionLogRp);
            if (count == 0) {
                actionLogMapper.insertCISPart(actionLogRp); // 将CIS共用料信息保存到TC中间表CIS_ACTION_LOG
            }
        });

        log.info("********** ecad_hint.map製作效率匯總邏輯 同步完成 **********");
    }


    private Folder getFolder(String folderDesc, SavedQueryService queryService) {
        Folder folder = null;

        try {
            Map<String, Object> queryResults = TCUtils.executeQuery(queryService, CISConstants.FIND_D9_FIND_FOLDER, new String[]{CISConstants.ATTR_OBJECT_TYPE, CISConstants.ATTR_OBJECT_DESC}, new String[]{"Folder", folderDesc});
            ModelObject[] md = (ModelObject[]) queryResults.get("succeeded");
            folder = (Folder) md[0];
        } catch (Exception e) {
            e.printStackTrace();
            writeLog("查找指定文件夹【" + folderDesc + "】对象出错：" + e.getMessage());
        }

        return folder;
    }

    private void processDataSyn(List<CISPropertyType> propLst, List<PartEntity> partLst, CISType cisType, TCSOAServiceFactory tcSOAServiceFactory) {
        DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();
        SavedQueryService savedQueryService = tcSOAServiceFactory.getSavedQueryService();
        WorkflowService workflowService = tcSOAServiceFactory.getWorkflowService();
        ClassificationService classificationService = tcSOAServiceFactory.getClassificationService();
        for (PartEntity part : partLst) {
            boolean blnSyncFlag = false;

            List<String> processedPartNumLst = new ArrayList<>();
            String id = Optional.ofNullable(part.getId()).orElse("");
            String part_number = "";
            String category = Optional.ofNullable(part.getCategory()).orElse("");
            String packageType = Optional.ofNullable(part.getPackageType()).orElse("");
            try {
                writeLog("同步【" + cisType + "】数据库中ID【" + id + "】行数据开始：");

                // 获取属性
                Map<String, String> propMap = new HashMap<>();
                getTCProperty(part, cisAttrConfig.getTcAttrMapping(), propMap);

                for (CISPropertyType propType : propLst) {
                    if (propType.equals(CISPropertyType.FOXCONN_PART_NUMBER)) {
                        part_number = Optional.ofNullable(part.getFoxconnPartNumber()).orElse("");
                    } else if (propType.equals(CISPropertyType.FOXCONN_PART_NUMBER_NODT)) {
                        part_number = Optional.ofNullable(part.getFoxconnPartNumberNodt()).orElse("");
                    } else if (propType.equals(CISPropertyType.STANDARD_PN)) {
                        part_number = Optional.ofNullable(part.getStandardPN()).orElse("");
                    }

                    if (processedPartNumLst.contains(part_number))
                        continue;
                    else
                        processedPartNumLst.add(part_number);

                    // 检查物料是否存在
                    boolean blnIsFromAVL = false;
                    Item item = null;
                    ItemRevision itemRev = null;
                    if (!StringUtil.isEmpty(part_number)) {
                        item = getItem(part_number, savedQueryService);
                        if (item != null) {
                            // 更新物料属性
                            TCUtils.getProperty(dataManagementService, item, CISConstants.ATTR_OBJECT_DESC);
                            String objDesc = item.get_object_desc();
                            if (CISConstants.STR_FROM_AVL_SYNC.equals(objDesc.toUpperCase(Locale.ENGLISH))) {
                                itemRev = TCUtils.getItemLatestRevision(dataManagementService, item);
                                if (TCUtils.isReleased(dataManagementService, itemRev)) {
                                    TCUtils.setProperties(dataManagementService, itemRev, propMap);
                                    if (importCISLib(cisType, category, itemRev, classificationService)) {
                                        blnIsFromAVL = true;
                                        blnSyncFlag = true;
                                    } else
                                        blnSyncFlag = false;
                                }
                            } else {
//                                countLog(CISSyncType.EXIST, cisType, part_number);
                            }

                            if (blnSyncFlag)
                                countLog(CISSyncType.SYNC, cisType, part_number);
                        } else {
                            // 创建物料
                            String startTime = DateUtil.getNowTime("yyyy-MM-dd HH:mm:ss.SSS");
                            item = createPart(cisType, packageType, part_number, dataManagementService);
                            String endTime = DateUtil.getNowTime("yyyy-MM-dd HH:mm:ss.SSS");

                            if (item != null) {
                                itemRev = TCUtils.getItemLatestRevision(dataManagementService, item);
                                TCUtils.setProperties(dataManagementService, itemRev, propMap);

                                // 设置效益报表日志信息
                                setActionLogInfo(part_number, "A", itemRev.getUid(), startTime, endTime);

                                // 归档
                                archivePart(cisType, propType, item, dataManagementService);

                                // 导入分类库
                                if (itemRev != null) {
                                    _materialMap.put(part_number, itemRev);

                                    if (importCISLib(cisType, category, itemRev, classificationService)) {
                                        // 发布
                                        TCUtils.createNewProcess(workflowService, itemRev.get_item_id(), CISConstants.PROCESS_FXN30_PARTS_BOM_FAST_RELEASE_PROCESS, new ModelObject[]{itemRev});

                                        blnSyncFlag = true;
                                    } else
                                        blnSyncFlag = false;
                                }
                            }

                            if (blnSyncFlag)
                                countLog(CISSyncType.SYNC, cisType, part_number);
                        }
                    }

                    if (item != null && itemRev != null && blnSyncFlag) {
                        if (!blnIsFromAVL) {
                            TCUtils.setProperties(dataManagementService, item, CISConstants.ATTR_OBJECT_DESC, "Sync from " + cisType);
                            TCUtils.setProperties(dataManagementService, itemRev, CISConstants.ATTR_OBJECT_DESC, "Sync from " + cisType);
                            dataManagementService.refreshObjects(new ModelObject[]{item, itemRev});
                        }
                    }
                }

                /*// 2023/6/20 新增同步CIS的3D模型數據到TC庫
                category = transCategory(category);
                // 将3D模型挂载到standardPN下
                updateObject(tcSOAServiceFactory, part.getStandardPN(), part.getModifiedDrawingFile(), cisType, part.getCategory());
                // 将3D模型挂载到foxconnPartNodt下
                if (StrUtil.isNotBlank(part.getFoxconnPartNumberNodt()) && !part.getFoxconnPartNumberNodt().equals(part.getStandardPN())) {
                    updateObject(tcSOAServiceFactory, part.getFoxconnPartNumberNodt(), part.getModifiedDrawingFile(), CISType.CIS_DELL, category);
                }*/

                // 回写同步标志
                if (cisType.equals(CISType.CIS)) {
                    if (blnSyncFlag)
                        cisService.updateSync(Integer.parseInt(id));
                } else if (cisType.equals(CISType.CIS_DELL)) {
                    if (blnSyncFlag)
                        cisDellService.updateSync(Integer.parseInt(id));
                }

                writeLog("同步【" + cisType + "】数据库中ID【" + id + "】行数据结束：");

            } catch (Exception e) {
                e.printStackTrace();
                writeLog("【" + cisType + "】数据库数据库中ID【" + id + "】行同步出错：" + e.getMessage());
            }
        }
    }

    /**
     * 将文件夹类型转换成对应的首字母大写其他字母小写的形式
     *
     * @param category
     * @return
     */
    private String transCategory(String category) {
        if (category.contains("_")) {
            String[] list = category.split("_");
            StrBuilder sb = new StrBuilder();
            for (String s : list) {
                sb.append(StrUtil.upperFirst(s.toLowerCase())).append(" ");
            }
            return sb.subString(0, sb.length() - 1);
        } else {
            return StrUtil.upperFirst(category.toLowerCase());
        }
    }

    /**
     * 根据对象名称创建item或者item版本，根据3D数据模型判断是否上传模型数据
     *
     * @param objName         item的名称
     * @param drawingFileName 3D模型的名称
     * @param type            模型类型，CIS全局库还是DELL库
     * @throws Exception
     */
    private void updateObject(TCSOAServiceFactory tcsoaServiceFactory, String objName, String drawingFileName, CISType type, String category) throws Exception {
        try {
            if (StrUtil.isNotBlank(objName)) {
                Item item = getItem(objName, tcsoaServiceFactory.getSavedQueryService());
                if(ObjectUtil.isNull(item)){
                    item = TCUtils.createDocument(tcsoaServiceFactory.getDataManagementService(), objName, CISConstants.TYPE_EDA_COM_PART, objName, "A", new HashMap<>());
                }
                ItemRevision itemRevision = TCUtils.getItemLatestRevision(tcsoaServiceFactory.getDataManagementService(), item);
                ItemRevision drawingItemRevision = relateDesign(itemRevision,drawingFileName,tcsoaServiceFactory.getSavedQueryService(),tcsoaServiceFactory.getDataManagementService());
                if (StrUtil.isNotBlank(drawingFileName) && ObjectUtil.isNotNull(drawingItemRevision)) {
                    TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), drawingItemRevision, "IMAN_specification");
                    TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), drawingItemRevision);
                    List<ModelObject> modelObjectList = drawingItemRevision.getPropertyObject("IMAN_specification").getModelObjectListValue();
                    if (CollUtil.isNotEmpty(modelObjectList)) {
                        for (ModelObject modelObject : modelObjectList) {
                            String name = modelObject.getTypeObject().getName();
                            TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), modelObject, "object_name");
                            TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), modelObject);
                            String objectName = modelObject.getPropertyObject("object_name").getStringValue();
                            if ("ProPrt".equalsIgnoreCase(name) && objectName.equals(drawingItemRevision.get_item_id())) {
                                return;
                            }
                        }
                    }
                    // 掛載3D模型
                    File modelFile = getModel(drawingFileName, type, category);
                    if (ObjectUtil.isNotNull(modelFile)) {
                        try {
                            TCUtils.uploadDataset(tcsoaServiceFactory.getDataManagementService(), tcsoaServiceFactory.getFileManagementUtility(), drawingItemRevision,
                                    modelFile.getAbsolutePath(), "PrtFile", drawingItemRevision.get_item_id(), "ProPrt");
                            TCUtils.createNewProcess(tcsoaServiceFactory.getWorkflowService(), drawingItemRevision.get_item_id(), CISConstants.PROCESS_FXN30_PARTS_BOM_FAST_RELEASE_PROCESS, new ModelObject[]{drawingItemRevision});
                        } catch (Exception e) {
                            LogFactory.get().error("上传数据集文件失败，错误原因：" + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogFactory.get().error("同步" + objName + "的3D模型到TC出错，错误信息：" + e.getMessage());
        }
    }

    /**
     * 根据CIS类型 获取3D模型的文件
     *
     * @param name 3D模型文件名称
     * @param type CIS类型，全局库还是DELL库
     * @return
     */
    private File getModel(String name, CISType type, String category) {
        String path = "";
        if (CISType.CIS.equals(type)) {
            path = defaultPath + category;
        } else if (CISType.CIS_DELL.equals(type)) {
            path = dellPath + category;
        }
        List<File> files = FileUtil.loopFiles(path, new FileFilter() {
            @Override
            public boolean accept(File file) {
                return name.equals(file.getName());
            }
        });
        return CollUtil.isNotEmpty(files) ? files.get(0) : null;
    }

    private ItemRevision relateDesign(ModelObject itemRevision, String drawingFileName, SavedQueryService savedQueryService,
                                      DataManagementService dataManagementService) throws Exception {
        if (StrUtil.isBlank(drawingFileName)) {
            return null;
        }
        String itemName = drawingFileName.substring(0, drawingFileName.lastIndexOf("."));
        if (itemName.indexOf("_") == 0) {
            return null;
        }
        // 查詢版本下是否能查詢到關係
        TCUtils.getProperties(dataManagementService, itemRevision, new String[]{"TC_Is_Represented_By", "d9_EnglishDescription"});
        TCUtils.refreshObject(dataManagementService, itemRevision);
        List<ModelObject> objectList = itemRevision.getPropertyObject("TC_Is_Represented_By").getModelObjectListValue();
        if (CollUtil.isNotEmpty(objectList)) {
            for (ModelObject modelObject : objectList) {
                TCUtils.getProperties(dataManagementService,modelObject,new String[]{"item_id"});
                TCUtils.refreshObject(dataManagementService, modelObject);
                String itemId = modelObject.getPropertyObject("item_id").getStringValue();
                if(itemId.equals(itemName)){
                    return (ItemRevision) modelObject;
                }
            }
        }
        Item item = getItem(itemName, savedQueryService);
        if(ObjectUtil.isNull(item)) {
            // 創建對象並關聯關係
            item = TCUtils.createDocument(dataManagementService, itemName, "D9_EDADesign", itemName, "A", new HashMap<>());
        }
        ItemRevision itemLatestRevision = TCUtils.getItemLatestRevision(dataManagementService, item);
        String description = itemRevision.getPropertyObject("d9_EnglishDescription").getStringValue();
        TCUtils.setProperties(dataManagementService, itemLatestRevision, "d9_EnglishDescription", description);
        TCUtils.addRelation(dataManagementService, itemRevision, itemLatestRevision, "TC_Is_Represented_By");
        return itemLatestRevision;
    }

    /**
     * 查詢EDA商用零件是否存在，存在則獲取其未發佈的itemReview，不存在則創建一個EDA商用零件並且創建一個未發佈的itemReview
     *
     * @param itemName
     * @param savedQueryService
     * @param dataManagementService
     * @return
     * @throws Exception
     */
    private ItemRevision getItemRevision(String itemName, String drawingFileName, SavedQueryService savedQueryService,
                                         DataManagementService dataManagementService) throws Exception {
        Item item = getItem(itemName, savedQueryService);
        if (ObjectUtil.isNotNull(item)) {
            // 獲取最新的版本itemReview
            ItemRevision itemRevision = TCUtils.getItemLatestRevision(dataManagementService, item);
            // 判断最后一个版本是否已经上传了3D模型
            TCUtils.getProperty(dataManagementService, itemRevision, "IMAN_specification");
            TCUtils.refreshObject(dataManagementService, itemRevision);
            List<ModelObject> modelObjectList = itemRevision.getPropertyObject("IMAN_specification").getModelObjectListValue();
            if (CollUtil.isNotEmpty(modelObjectList)) {
                for (ModelObject modelObject : modelObjectList) {
                    String name = modelObject.getTypeObject().getName();
                    if ("ProPrt".equalsIgnoreCase(name)) {
                        return null;
                    }
                }
            }
            // 最后一个版本没有上传3D模型则升版用于上传新模型
            ModelObject newItemRevision = itemRevision;
            if (TCUtils.isReleased(dataManagementService, itemRevision) && StrUtil.isNotBlank(drawingFileName)) {
                // 如果最新版本已經發佈，升版並且講模型掛載到新版本上並發佈流程
                TCUtils.getProperty(dataManagementService, new ModelObject[]{itemRevision}, new String[]{"item_revision_id", "object_type", "object_name"});
                TCUtils.refreshObject(dataManagementService, itemRevision);
                String revisionId = itemRevision.getPropertyObject("item_revision_id").getStringValue();
                String objectType = itemRevision.getPropertyObject("object_type").getStringValue();
                String objectName = itemRevision.getPropertyObject("object_name").getStringValue();
                String newRevisionId = TCUtils.reviseVersion(dataManagementService, revisionId, objectType, itemRevision.getUid());
                newItemRevision = TCUtils.reviseItemRev(dataManagementService, itemRevision, objectName, newRevisionId);
            }
            return (ItemRevision) newItemRevision;
        } else if (StrUtil.isNotBlank(drawingFileName)) {
            // 創建一個item對象，並在itemReview中上傳零件
            item = TCUtils.createDocument(dataManagementService, itemName, CISConstants.TYPE_EDA_COM_PART, itemName, "A", new HashMap<>());
            return TCUtils.getItemLatestRevision(dataManagementService, item);
        }
        return null;
    }

    private Item getItem(String itemId, SavedQueryService savedQueryService) {
        Map<String, Object> queryResults = TCUtils.executeQuery(savedQueryService, D9Constants.D9_ITEM_NAME_OR_ID, new String[]{CISConstants.ATTR_ITEM_ID}, new String[]{itemId});

        ModelObject[] md = (ModelObject[]) queryResults.get("succeeded");
        if (md != null && md.length > 0) {
            return (Item) md[0];
        }
        return null;
    }

    private void getTCProperty(PartEntity partModel, Map<String, String> configMap, Map<String, String> propMap) {
        try {
            Field[] fields = partModel.getClass().getDeclaredFields();
            for (Field field : fields) {
                ReflectionUtils.makeAccessible(field);
                TCPropertes tcPropertes = field.getAnnotation(TCPropertes.class);
                if (tcPropertes != null) {
                    String tcProp = tcPropertes.tcProperty();
                    if (!tcProp.isEmpty()) {
                        Object o = field.get(partModel);
                        if (o != null) {
                            if (configMap.containsKey(field.getName()))
                                propMap.put(configMap.get(field.getName()), (String) o);
                            else
                                propMap.put(tcProp, (String) o);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Item createPart(CISType cisType, String packageType, String foxconn_part_number, DataManagementService dataManagementService) throws Exception {
        try {
            Map<String, String> itemPropMap = new HashMap<>();
            itemPropMap.put("item_id", foxconn_part_number);

            DataManagement.CreateResponse response = null;
            if (packageType.matches("^\\d{1,2}-LAYER"))
                response = TCUtils.createObjects(dataManagementService, CISConstants.TYPE_D9_PCB_PART, itemPropMap);
            else
                response = TCUtils.createObjects(dataManagementService, CISConstants.TYPE_EDA_COM_PART, itemPropMap);
            ServiceData serviceData = response.serviceData;
            if (serviceData.sizeOfPartialErrors() <= 0) {
                ModelObject[] items = response.output[0].objects;
                return (Item) items[0];
            }
        } catch (Exception e) {
            countLog(CISSyncType.NO_SYNC, cisType, foxconn_part_number);
            e.printStackTrace();
            writeLog("创建物料【" + foxconn_part_number + "】出错：" + e.getMessage());
            throw new Exception(e);
        }

        return null;
    }

    private void archivePart(CISType cisType, CISPropertyType cisPropType, Item item, DataManagementService dataManagementService) throws Exception {
        String tdmId = "";
        try {
            tdmId = item.get_item_id();
            Folder folder = null;
            if (cisType.equals(CISType.CIS)) {
                if (cisPropType.equals(CISPropertyType.FOXCONN_PART_NUMBER))
                    folder = _dt_folder;
                else if (cisPropType.equals(CISPropertyType.FOXCONN_PART_NUMBER_NODT))
                    folder = _nodt_folder;
                else if (cisPropType.equals(CISPropertyType.STANDARD_PN))
                    folder = _standard_pn_folder;
            } else if (cisType.equals(CISType.CIS_DELL)) {
                if (cisPropType.equals(CISPropertyType.FOXCONN_PART_NUMBER) || cisPropType.equals(CISPropertyType.STANDARD_PN))
                    folder = _tc_sync_folder;
            }

            if (folder != null && item != null) {
                TCUtils.addContents(dataManagementService, folder, item);
                dataManagementService.refreshObjects(new ModelObject[]{folder, item});
            }

        } catch (Exception e) {
            countLog(CISSyncType.NO_SYNC, cisType, tdmId);
            e.printStackTrace();
            writeLog("物料【" + tdmId + "】归档出错：" + e.getMessage());
            throw new Exception(e);
        }
    }

    private boolean importCISLib(CISType cisType, String key, ItemRevision itemRev, ClassificationService clsService) throws Exception {
        String tdmId = "";
        try {
            tdmId = itemRev.get_item_id();
            String category = CisPropertitesUtil.props.getProperty(key);


            // Create the ICO object
//            Classification.ClassificationPropertyValue[] ico_prop_values1 = new Classification.ClassificationPropertyValue[1];
//            Classification.ClassificationProperty[] ico_props = new Classification.ClassificationProperty[1];
            Classification.ClassificationObject[] icos = new Classification.ClassificationObject[1];

//            ico_prop_values1[0] = new Classification.ClassificationPropertyValue();
//            ico_prop_values1[0].dbValue = "abc";

//            ico_props[0] = new Classification.ClassificationProperty();
//            ico_props[0].attributeId = 50003;
//            ico_props[0].values = ico_prop_values1;

            icos[0] = new Classification.ClassificationObject();
            icos[0].classId = category;
            icos[0].clsObjTag = null;
            icos[0].instanceId = itemRev.get_item_id();
//            icos[0].properties = ico_props;
            icos[0].unitBase = "METRIC";
            // wsoId is null for standalone ICO.
            icos[0].wsoId = itemRev;

            Classification.CreateClassificationObjectsResponse createICOResponse = clsService.createClassificationObjects(icos);
            if (createICOResponse.data.sizeOfPartialErrors() <= 0) {
                return true;
            } else {
                countLog(CISSyncType.NO_SYNC, cisType, tdmId);
                writeLog("【" + tdmId + "】没有导入分类库【" + key + "】：");
            }

        } catch (Exception e) {
            countLog(CISSyncType.NO_SYNC, cisType, tdmId);
            e.printStackTrace();
            writeLog("【" + tdmId + "】导入分类库【" + key + "】出错：" + e.getMessage());
            throw new Exception(e);
        }

        return false;
    }

    private void writeLog(String msg) {
        log.info("==================" + msg + _dateFormat.format(new Date()) + "==================");
        System.err.println("==================" + msg + _dateFormat.format(new Date()) + "==================");
    }

    private void countLog(CISSyncType syncType, CISType cisType, String id) {
//        if (syncType.equals(CISSyncType.EXIST)) {
//            if (cisType.equals(CISType.CIS))
//                _cisExistLst.add(id);
//            else if (cisType.equals(CISType.CIS_DELL))
//                _cisDellExistLst.add(id);
//        } else
        if (syncType.equals(CISSyncType.SYNC)) {
            if (cisType.equals(CISType.CIS))
                _cisSyncLst.add(id);
            else if (cisType.equals(CISType.CIS_DELL))
                _cisDellSyncLst.add(id);
        } else if (syncType.equals(CISSyncType.NO_SYNC)) {
            if (cisType.equals(CISType.CIS))
                _cisNotSyncLst.add(id);
            else if (cisType.equals(CISType.CIS_DELL))
                _cisDellNotSyncLst.add(id);
        }
    }

    private void sendMail() {
        try {
            String[] mailAddrArr = mailAddrs.split(",");
            for (String mailAddr : mailAddrArr) {
                String subject = "【自動同步PDMCIS新增料件郵件通知】";
                String content = String.format(
                        "<!doctype html>\n" +
                                "<html lang=\"en\">\n" +
                                "<head>\n" +
                                "    <meta charset=\"UTF-8\">\n" +
                                "    <meta name=\"viewport\"\n" +
                                "          content=\"width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0\">\n" +
                                "    <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">\n" +
                                "    <title>Document</title>\n" +
                                "</head>\n" +
                                "<body>\n" +
                                "    <pre>\n" +
                                "Dear Tc管理員，\n" +
                                "\n" +
                                "    本次CIS庫同步作業已完成。PDMCIS庫，已同步數量為%s【%s】；未同步數量為%s【%s】。PDMCISForDell庫，已同步數量為%s【%s】；未同步數量為%s【%s】，請參考\n" +
                                "\n" +
                                "    </pre>\n" +
                                "<h3 style=\"margin: 0\">此通知由自動同步PDMCIS新增料件發送，請勿回復。</h3>\n" +
                                "</body>\n" +
                                "</html>",
                        _cisSyncLst.size(), _cisSyncLst.stream().collect(Collectors.joining(",")), _cisNotSyncLst.size(), _cisNotSyncLst.stream().collect(Collectors.joining(",")),
                        _cisDellSyncLst.size(), _cisDellSyncLst.stream().collect(Collectors.joining(",")), _cisDellNotSyncLst.size(), _cisDellNotSyncLst.stream().collect(Collectors.joining(",")));
                HashMap<String, String> parma = new HashMap<>();
                parma.put("requestPath", mailUrl);
                parma.put("ruleName", "/tc-mail/teamcenter/sendMail3");
                parma.put("sendTo", mailAddr);
                parma.put("subject", subject);
                parma.put("htmlmsg", content);

                HttpUtil.httpPost(parma);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateMaterialProp(DataManagementService dataManagementService) {
        try {
            CISBatcher.batcher(_materialMap, BATCHER_SIZE, m -> {
                List<MaterialRequest> materialReqLst = m.keySet().stream().map(MaterialRequest::new).collect(Collectors.toList());

                String jsonInfo = "";
                try {
                    jsonInfo = HttpUtil.post(materialUrl, JSONArray.toJSONString(materialReqLst));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (StringUtil.isNotEmpty(jsonInfo)) {
                    JSONArray jsonArray = JSONObject.parseObject(jsonInfo).getJSONArray("data");
                    for (Object obj : jsonArray) {
                        JSONObject jsonObj = (JSONObject) obj;
                        String materialNum = Optional.ofNullable(jsonObj.getString("materialNum")).orElse("");
                        String materialGroup = Optional.ofNullable(jsonObj.getString("materialGroup")).orElse("");
                        String baseUnit = Optional.ofNullable(jsonObj.getString("baseUnit")).orElse("");

                        ItemRevision itemRev = m.getOrDefault(materialNum, null);
                        if (itemRev != null) {
                            // 更新物料属性
                            Map<String, String> propMap = new HashMap<>();
                            propMap.put(CISConstants.ATTR_D9_PROCUREMENT_METHODS, "F");
                            propMap.put(CISConstants.ATTR_D9_MATERIAL_TYPE, "ZROH");
                            propMap.put(CISConstants.ATTR_D9_MATERIAL_GROUP, materialGroup);
                            propMap.put(CISConstants.ATTR_D9_UN, baseUnit);
                            TCUtils.setProperties(dataManagementService, itemRev, propMap);
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setActionLogInfo(String itemId, String rev, String revUid, String startTime, String endTime) {
        ActionLogRp actionLog = new ActionLogRp();

        actionLog.setFunctionName("在TC系統中維護CIS物料信息時間（CIS系統集成）");
        actionLog.setCreator("dev");
        actionLog.setCreatorName("dev");
        actionLog.setProject("");
        actionLog.setPhase("");
        actionLog.setItemId(itemId);
        actionLog.setRev(rev);
        actionLog.setRevUid(revUid);
        actionLog.setStartTime(startTime);
        actionLog.setEndTime(endTime);

        if (_actionLogs != null)
            _actionLogs.add(actionLog);
    }

    private void cisActionLog() {
        try {
            if (_actionLogs != null && _actionLogs.size() > 0) {
                writeLog("记录效益报表日志【" + _actionLogs.size() + "】条数据完成：");
                actionLogService.addLog(_actionLogs);
                writeLog("记录效益报表日志【" + _actionLogs.size() + "】条数据完成：");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
