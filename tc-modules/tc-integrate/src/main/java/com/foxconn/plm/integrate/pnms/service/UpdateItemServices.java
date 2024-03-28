package com.foxconn.plm.integrate.pnms.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.integrate.config.properties.TCAttrConfig;
import com.foxconn.plm.integrate.log.domain.ActionLogRp;
import com.foxconn.plm.integrate.log.service.ActionLogServiceImpl;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.DatasetUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.loose.core.SessionService;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core.ReservationService;
import com.teamcenter.services.strong.workflow.WorkflowService;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.exceptions.NotLoadedException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import com.teamcenter.services.strong.core._2010_09.DataManagement.NameValueStruct1;
import com.teamcenter.services.strong.core._2010_09.DataManagement.PropInfo;
import com.teamcenter.services.strong.core._2010_09.DataManagement.SetPropertyResponse;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Scope("request")
public class UpdateItemServices {
    private static Log log = LogFactory.get();
    @Resource
    private TCAttrConfig tcAttrConfig;

    @Resource
    private TcMailClient tcMailClient;

    @Resource
    private ActionLogServiceImpl actionLogService;

    @Resource
    private AddCableInfoServiceImpl addCableInfoServiceImpl;

    public String updateItem(String itemID, Map<String, String> requestMap) throws Exception {
        TCSOAServiceFactory tcSOAServiceFactory = null;
        try {
            tcSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String logStartTime = sdf.format(new Date());
            log.info("pnms updateItem requestMap ->> " + requestMap);
            DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();
            WorkflowService workflowService = tcSOAServiceFactory.getWorkflowService();
            SessionService sessionService = tcSOAServiceFactory.getSessionService();
            TCUtils.byPass(sessionService, true);
            ReservationService rs = tcSOAServiceFactory.getReservationService();
            Item item = TCUtils.queryItemByIDOrName(tcSOAServiceFactory.getSavedQueryService(), dataManagementService, itemID, "");
            if (item != null) {
                Map<ItemRevision, List<String>> statusMap = null;
                try {
                    TCUtils.byPass(sessionService, true);
                    ModelObject[] items = {item};
                    dataManagementService.getProperties(items, new String[]{"revision_list"});
                    ModelObject[] itemRevs = item.get_revision_list();
                    ModelObject[] itemAndRevs = ArrayUtil.append(itemRevs, item);
                    TCUtils.checkin(dataManagementService, rs, itemAndRevs);
                    printServiceData(dataManagementService.refreshObjects(itemAndRevs));
                    statusMap = delStatus(dataManagementService, workflowService, itemRevs);
                    Map<String, String> tcAttrMapping = tcAttrConfig.getTcAttrMapping();
                    // 更新 rev 属性
                    List<String> propList = new ArrayList<>();
                    List<String[]> prorVaule = new ArrayList<>();
                    requestMap.forEach((k, requestValue) -> {
                        if (tcAttrMapping.containsKey(k)) {
                            log.info("update ::  " + k + " ------- " + requestValue);
                            String tcProp = tcAttrMapping.get(k);
                            propList.add(tcProp);
                            prorVaule.add(new String[]{requestValue});
                        } else {
                            log.warn(" tc attribute config not contain key :: " + k);
                        }
                    });
                    String hhpn = requestMap.get("hhpn");
                    ItemRevision itemRevision = (ItemRevision) itemRevs[itemRevs.length - 1];
                    boolean b2 = setProperties(item, new String[]{"object_name"}, new String[][]{{requestMap.get("name")}}, dataManagementService);
                    boolean b1 = true;
                    for (ModelObject modelObject : itemRevs) {
                        boolean b11 = setProperties(modelObject, propList.toArray(new String[0]), prorVaule.toArray(new String[0][0]),
                                dataManagementService);
                        if (!b11) {
                            b1 = false;
                        }
                    }
                    if (b1 & b2) {
                        boolean b3 = setProperties(item, new String[]{"item_id"}, new String[][]{{hhpn}}, dataManagementService);
                        if (b3) {
                            String logEndTime = sdf.format(new Date());
                            try {
                                writeActionLog(itemRevision, logStartTime, logEndTime);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            // 视图更新 structure_revisions bom_view_tags
                            updateBOMView(dataManagementService, items, itemID, hhpn, "bom_view_tags");
                            updateBOMView(dataManagementService, itemRevs, itemID, hhpn, "structure_revisions");
                            try {
                                dataManagementService.getProperties(itemRevs, new String[]{"owning_user", "d9_CCGroupID"});
                                String groupId = itemRevision.getPropertyObject("d9_CCGroupID").getStringValue();
                                if (NumberUtil.isNumber(groupId)) {
                                    User user = (User) itemRevision.get_owning_user();
                                    dataManagementService.getProperties(new ModelObject[]{user}, new String[]{"user_name"});
                                    String designPN = itemID.substring(0, itemID.indexOf("@"));
                                    String des = requestMap.getOrDefault("des", "");
                                    String mfg = requestMap.getOrDefault("mfg", "");
                                    addCableInfo(hhpn, designPN, des, mfg, groupId, user.get_user_name());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            try {
                                //send mail
                                dataManagementService.getProperties(itemRevs, new String[]{"fnd0StartedWorkflowTasks", "IMAN_external_object_link"});
                                List<ModelObject> wfList = itemRevision.getPropertyObject("fnd0StartedWorkflowTasks").getModelObjectListValue();
                                List<ModelObject> datasetList =
                                        itemRevision.getPropertyObject("IMAN_external_object_link").getModelObjectListValue();
                                Dataset mailTxt = null;
                                if (datasetList != null && datasetList.size() > 0) {
                                    mailTxt = (Dataset) datasetList.get(0);
                                }
                                if (wfList != null && mailTxt != null) {
                                    for (ModelObject wfObject : wfList) {
                                        if (wfObject instanceof EPMReviewTask) {
                                            dataManagementService.getProperties(new ModelObject[]{wfObject}, new String[]{"state", "current_name"});
                                            if (wfObject.getPropertyObject("state").getIntValue() == 4) {
                                                String taskName = wfObject.getPropertyObject("current_name").getStringValue();
                                                FileManagementUtility fileManagementUtility = tcSOAServiceFactory.getFileManagementUtility();
                                                File[] files = DatasetUtil.getDataSetFiles(dataManagementService, mailTxt, fileManagementUtility);
                                                assert files != null && files.length > 0;
                                                List<String> mailInfos = FileUtil.readLines(files[0], "GBK");
                                                // List<String> mailInfos = Files.readAllLines(files[0].toPath());
                                                for (String mailInfo : mailInfos) {
                                                    if (mailInfo.startsWith(taskName + "=")) {
                                                        int index = mailInfo.indexOf("%%");
                                                        if (index > 0) {
                                                            String des = requestMap.get("des");
                                                            String mails = mailInfo.substring(index + 2);
                                                            if (StringUtils.hasLength(mails)) {
                                                                sendMail(mails, hhpn, des);
                                                            } else {
                                                                log.error(itemID + "  hhpn:  " + hhpn + " read publicMail is null");
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                log.error(itemID + "  -->>itemid  :  send mail fail  !", e);
                            }
                            return "ok";
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (statusMap != null && statusMap.size() > 0) {
                        for (Map.Entry<ItemRevision, List<String>> entry : statusMap.entrySet()) {
                            ItemRevision itemRev = entry.getKey();
                            List<String> statusList = entry.getValue();
                            if (itemRev != null && statusList != null) {
                                for (String statusName : statusList) {
                                    TCUtils.addStatus(workflowService, new WorkspaceObject[]{itemRev}, statusName, "Append");
                                }
                            }
                        }
                    }
                }
            } else {
                log.error(itemID + "  -->>itemid  :  not exist TC !");
                return "pnms fail :  " + itemID + "  -->>itemid  :  not exist TC !";
            }
        } finally {
            try {
                if (tcSOAServiceFactory != null) {
                    SessionService sessionService = tcSOAServiceFactory.getSessionService();
                    TCUtils.byPass(sessionService, false);
                    tcSOAServiceFactory.logout();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "pnms fail :  " + itemID + "  -->>  update attribute fail ";
    }


    //structure_revisions bom_view_tags
    public void updateBOMView(DataManagementService dataManagementService, ModelObject[] modelObjects, String bupn, String hhpn,
                              String bomViewAtrrName) {
        try {
            dataManagementService.getProperties(modelObjects, new String[]{bomViewAtrrName});
            //structure_revisions
            Set<ModelObject> allBOMView = new HashSet<>();
            for (ModelObject modelObject : modelObjects) {
                allBOMView.addAll(modelObject.getPropertyObject(bomViewAtrrName).getModelObjectListValue());
            }
            dataManagementService.getProperties(allBOMView.toArray(new ModelObject[0]), new String[]{"object_name"});
            for (ModelObject viewObject : allBOMView) {
                String viewName = viewObject.getPropertyObject("object_name").getStringValue();
                viewName = viewName.replaceAll(bupn, hhpn);
                TCUtils.setProperties(dataManagementService, viewObject, "object_name", viewName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Map<ItemRevision, List<String>> delStatus(DataManagementService dataManagementService, WorkflowService workflowService,
                                                     ModelObject[] itemRevs) throws Exception {
        Map<ItemRevision, List<String>> map = new HashMap<>();
        dataManagementService.getProperties(itemRevs, new String[]{"release_status_list"});
        for (ModelObject itemObject : itemRevs) {
            ItemRevision itemRev = (ItemRevision) itemObject;
            List<ModelObject> statuslist = itemRev.getPropertyObject("release_status_list").getModelObjectListValue();
            dataManagementService.getProperties(statuslist.toArray(new ModelObject[0]), new String[]{"object_name"});
            List<String> statusStrList = statuslist.stream().map(e -> {
                try {
                    return e.getPropertyObject("object_name").getStringValue();
                } catch (NotLoadedException notLoadedException) {
                    notLoadedException.printStackTrace();
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            for (String statusName : statusStrList) {
                if (StringUtils.hasLength(statusName)) {
                    TCUtils.addStatus(workflowService, new WorkspaceObject[]{itemRev}, statusName, "Delete");
                    dataManagementService.refreshObjects(new ModelObject[]{itemRev});
                }
            }
            map.put(itemRev, statusStrList);
        }
        return map;
    }


    public void sendMail(String mailTo, String itemId, String desc) {
        String msg = "<html><head></head><body><br/><br/>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \"> Dear : </div><br/><br/>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \"> 已完成 <" + itemId + "|" + desc + "> 物料申请，请登录TC/PNMS系统查看！"
                + "</div ></body ></html > ";
        Map<String, String> httpmap = new HashMap<>();
        httpmap.put("sendTo", mailTo);
        httpmap.put("sendCc", "");
        httpmap.put("subject", "已完成 <" + itemId + "|" + desc + "> 物料申请");
        httpmap.put("htmlmsg", msg);
        Gson gson = new Gson();
        String data = gson.toJson(httpmap);
        tcMailClient.sendMail3Method(data);// 发送邮件
    }

    public ActionLogRp writeActionLog(ItemRevision itemRevision, String startTime, String endTime) throws NotLoadedException {
        TCSOAServiceFactory tcSOAServiceFactory = null;
        ActionLogRp actionLogRp = new ActionLogRp();
        try {
            tcSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();
            dataManagementService.getProperties(new ModelObject[]{itemRevision}, new String[]{"project_ids", "item_id", "current_id_uid",
                    "item_revision_id", "owning_user"});
            String projects = itemRevision.getPropertyObject("project_ids").getStringValue();
            User user = (User) itemRevision.get_owning_user();
            dataManagementService.getProperties(new ModelObject[]{user}, new String[]{"user_id", "user_name"});
            actionLogRp.setStartTime(startTime);
            actionLogRp.setEndTime(endTime);
            actionLogRp.setProject(projects);
            actionLogRp.setItemId(itemRevision.get_item_id());
            actionLogRp.setRevUid(itemRevision.getUid());
            actionLogRp.setRev(itemRevision.get_item_revision_id());
            actionLogRp.setFunctionName("PNMS集成同步物料信息時間");
            actionLogRp.setCreator(user.get_user_id());
            actionLogRp.setCreatorName(user.get_user_name());
            actionLogService.addLog(Lists.newArrayList(actionLogRp));
        } finally {
            try {
                if (tcSOAServiceFactory != null) {
                    tcSOAServiceFactory.logout();
                }
            } catch (Exception e) {
            }
        }
        return actionLogRp;
    }


    /**
     * 设置TC对象属性值
     *
     * @param model                 TC对象
     * @param propertyname          属性名
     * @param propertyvalue         属性值
     * @param datamanagementservice 工具类
     * @return true代表成功，false代表失败
     * @throws ServiceException
     */
    public boolean setProperties(ModelObject model, String[] propertyname, String[][]
            propertyvalue, DataManagementService datamanagementservice) {
        if (model == null) {
            return false;
        }
        if (propertyname.length != propertyvalue.length) {
            return false;
        }
        try {
            PropInfo[] apropinfo = new PropInfo[1];
            PropInfo propinfo = new PropInfo();
            propinfo.object = model;
            NameValueStruct1 anamevaluestruct1[] = new NameValueStruct1[propertyname.length];
            for (int j = 0; j < propertyname.length; j++) {
                anamevaluestruct1[j] = new NameValueStruct1();
                anamevaluestruct1[j].name = propertyname[j];
                anamevaluestruct1[j].values = propertyvalue[j];
            }
            propinfo.vecNameVal = anamevaluestruct1;
            apropinfo[0] = propinfo;
            String[] as = {"ENABLE_PSE_BULLETIN_BOARD"};
            SetPropertyResponse setpropertyresponse = datamanagementservice.setProperties(apropinfo, as);
            return printServiceData(setpropertyresponse.data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean printServiceData(ServiceData sd) {
        int errorSize = sd.sizeOfPartialErrors();
        log.info(" -->error size  :: " + errorSize);
        if (errorSize > 0) {
            for (int i = 0; i < errorSize; i++) {
                log.error(i + " -->> error info  :: " + Arrays.toString(sd.getPartialError(i).getMessages()));
            }
            return false;
        }
        return true;
    }

    public String addCableInfo(String hhpn, String designPN, String desc, String mfg, String groupId, String creator) {
        int count = addCableInfoServiceImpl.getCableCountByHHPN(hhpn);
        if (count > 0) {
            return "HHPN，已存在！";
        }
        String id = addCableInfoServiceImpl.getHHPNEmptyByGroupId(groupId);
        if (StrUtil.isNotEmpty(id)) {
            addCableInfoServiceImpl.updateCableInfo(hhpn, designPN, desc, mfg, new Date(), id);
        } else {
            addCableInfoServiceImpl.addCableInfo(hhpn, designPN, desc, mfg, groupId, creator, new Date(), new Date());
        }
        return "添加成功！";
    }

    public static void main(String[] args) {
    }
}
