package com.foxconn.plm.integrate.sap.customPN.service;


import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.dp.plm.privately.Access;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.integrate.sap.customPN.domain.ApplyCustomPnResponse;
import com.foxconn.plm.integrate.sap.customPN.domain.rp.CustomPartRp;
import com.foxconn.plm.integrate.sap.customPN.mapper.CustomPNMapper;
import com.foxconn.plm.integrate.sap.customPN.utils.ConnectPoolUtils;
import com.foxconn.plm.integrate.sap.customPN.utils.SAPConstants;
import com.foxconn.plm.integrate.sap.customPN.utils.ViewPoster;
import com.foxconn.plm.integrate.sap.customPN.utils.ViewUtils;
import com.foxconn.plm.integrate.sap.utils.DestinationUtils;
import com.foxconn.plm.integrate.spas.domain.D9Constants;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.TCUtils;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.teamcenter.services.loose.core.SessionService;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core._2006_03.DataManagement;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.Folder;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.client.model.strong.WorkspaceObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@Scope("prototype")
public class CustomPNService {

    private static Log log = LogFactory.get();
    private static String[] codes = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "N", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

    @Autowired(required = false)
    private CustomPNMapper customPNMapper;


    /**
     * 申请自编料号
     *
     * @param customPartRps
     * @return
     */
    public String applyCustomPNs(List<CustomPartRp> customPartRps) throws Exception {
        JCoDestination destination = null;
        JCoDestination destination888 = null;
        JCoDestination destination868 = null;
        TCSOAServiceFactory tcSOAServiceFactory = null;

        try {
            if (SAPConstants.SAP_IP == null || "".equalsIgnoreCase(SAPConstants.SAP_IP)) {
                throw new Exception("ahost is null");
            }
            tcSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            destination = JCoDestinationManager.getDestination(ConnectPoolUtils.ABAP_AS_POOLED);
            destination888 = JCoDestinationManager.getDestination(ConnectPoolUtils.ABAP_AS_POOLED_888);
            destination868 = JCoDestinationManager.getDestination(ConnectPoolUtils.ABAP_AS_POOLED_868);

            String msg = "";
            msg = checkCustomPart(customPartRps, tcSOAServiceFactory);
            if (!"".equalsIgnoreCase(msg)) {
                return msg;
            }
            List<ApplyCustomPnResponse> mMResponses = new ArrayList<>();

            List<CustomPartRp> primart49ls = new ArrayList<>();
            List<CustomPartRp> otherls = new ArrayList<>();
            for (CustomPartRp customPartRp : customPartRps) {
                String reg = customPartRp.getRuleRegx();
                if (reg.startsWith("49") && reg.indexOf("#") < 0) {
                    primart49ls.add(customPartRp);
                } else {
                    otherls.add(customPartRp);
                }
            }

            for (CustomPartRp customPartRp : primart49ls) {
                mMResponses.add(applyCustomPN(destination, customPartRp, destination888, destination868, customPartRps, tcSOAServiceFactory));
            }

            for (CustomPartRp customPartRp : otherls) {
                mMResponses.add(applyCustomPN(destination, customPartRp, destination888, destination868, customPartRps, tcSOAServiceFactory));
            }

            for (ApplyCustomPnResponse m : mMResponses) {
                if (m.getCode() == 1) {
                    continue;
                }
                msg = msg + m.getMsg() + "\n";
            }
            if (!"".equalsIgnoreCase(msg)) {
                return msg;
            }
            List<CustomPartRp> ls79 = new ArrayList<>();
            List<CustomPartRp> lsOthers = new ArrayList<>();
            for (CustomPartRp customPartRp : customPartRps) {
                if (customPartRp.getRuleRegx().startsWith("79")) {
                    ls79.add(customPartRp);
                } else {
                    lsOthers.add(customPartRp);
                }
            }
            for (CustomPartRp customPartRp : ls79) {
                mMResponses.add(archiveFolder(tcSOAServiceFactory, customPartRp));
            }
            for (CustomPartRp customPartRp : lsOthers) {
                mMResponses.add(archiveFolder(tcSOAServiceFactory, customPartRp));
            }
            for (ApplyCustomPnResponse m : mMResponses) {
                if (m.getCode() == 1) {
                    continue;
                }
                msg = msg + m.getMsg() + "\n";
            }
            log.info(msg);
            return msg;
        } finally {
            try {
                if (tcSOAServiceFactory != null) {
                    tcSOAServiceFactory.logout();
                }
            } catch (Exception e) {
            }
        }
    }


    public String postCustomPNs(List<CustomPartRp> customPartRps) throws Exception {
        JCoDestination destination = null;
        JCoDestination destination888 = null;
        JCoDestination destination868 = null;
        TCSOAServiceFactory tcSOAServiceFactory = null;
        try {
            if (SAPConstants.SAP_IP == null || "".equalsIgnoreCase(SAPConstants.SAP_IP)) {
                throw new Exception("ahost is null");
            }
            destination = JCoDestinationManager.getDestination(ConnectPoolUtils.ABAP_AS_POOLED);
            destination888 = JCoDestinationManager.getDestination(ConnectPoolUtils.ABAP_AS_POOLED_888);
            destination868 = JCoDestinationManager.getDestination(ConnectPoolUtils.ABAP_AS_POOLED_868);
            tcSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            List<ApplyCustomPnResponse> mMResponses = new ArrayList<>();
            for (CustomPartRp customPartRp : customPartRps) {
                mMResponses.add(postCustomPN(destination, customPartRp, destination888, destination868, tcSOAServiceFactory));
            }
            String msg = "";
            for (ApplyCustomPnResponse m : mMResponses) {
                if (m.getCode() == 1) {
                    continue;
                }
                msg = msg + m.getMsg() + "\n";
            }
            log.info(msg);
            return msg;
        } finally {
            try {
                if (tcSOAServiceFactory != null) {
                    tcSOAServiceFactory.logout();
                }
            } catch (Exception e) {
            }
        }
    }


    private ApplyCustomPnResponse postCustomPN(JCoDestination destination, CustomPartRp customPartRp, JCoDestination destination888, JCoDestination destination868, TCSOAServiceFactory tcSOAServiceFactory) {
        ApplyCustomPnResponse applyCustomPnResponse = new ApplyCustomPnResponse();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        try {
            SessionService sessionService = tcSOAServiceFactory.getSessionService();
            TCUtils.byPass(sessionService, true);
            log.info("post custom pn ===========>" + customPartRp.getOldMaterialNumber() + " " + customPartRp.getRuleRegx());
            DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();

            ServiceData sdDataset = dataManagementService.loadObjects(new String[]{customPartRp.getIrUid()});
            ItemRevision irv = (ItemRevision) sdDataset.getPlainObject(0);
            TCUtils.getProperty(dataManagementService, irv, "item_id");
            TCUtils.getProperty(dataManagementService, irv, "d9_DescriptionSAP");
            String descSAP = irv.getPropertyObject("d9_DescriptionSAP").getStringValue();
            if(descSAP!=null&&!("".equalsIgnoreCase(descSAP))){
                customPartRp.setDescription(descSAP);
            }

            String itemId = irv.get_item_id();
            applyCustomPnResponse.setUid(customPartRp.getUid());
            applyCustomPnResponse.setOldItemId(customPartRp.getOldMaterialNumber());
            customPartRp.setMaterialNumber(itemId);
            String msgs = "";
            String plants = customPartRp.getPlant();
            String[] plantArr = plants.split(",");
            Map<String, String> plantMp = new HashMap<>();
            for (String plant : plantArr) {
                customPartRp.setPlant(plant.trim());
                TCUtils.getProperty(dataManagementService, irv, "d9_SAPLog");
                String v = irv.getPropertyObject("d9_SAPLog").getStringValue();

                JCoDestination jcoDest = DestinationUtils.getJCoDestination(plant.trim(), destination, destination888, destination868);
                if (jcoDest == null) {
                    TCUtils.setProperties(dataManagementService, irv, "d9_SAPLog", sdf.format(new Date()) + " " + plant + " post SAP failed! 未找到對應的SAP Site\n" + v);
                    continue;
                }
                String msg = postDataToSAP(jcoDest, customPartRp);

                if (!"".equalsIgnoreCase(msg)) {
                    msgs += msg + "\n";
                    if (msg.contains("post  basicView  plant")) {
                        TCUtils.setProperties(dataManagementService, irv, "d9_SAPLog", sdf.format(new Date()) + " " + plant + " post SAP failed! " + msg + "\n" + v);
                    } else {
                        TCUtils.setProperties(dataManagementService, irv, "d9_SAPLog", sdf.format(new Date()) + " " + plant + " post SAP success! " + msg + "\n" + v);
                        plantMp.put(plant.trim(), plant.trim());
                    }
                } else {
                    TCUtils.setProperties(dataManagementService, irv, "d9_SAPLog", sdf.format(new Date()) + " " + plant + " post SAP success!\n" + v);
                    plantMp.put(plant.trim(), plant.trim());
                }
            }
            try {
                TCUtils.getProperty(dataManagementService, irv, "d9_Plant_L6");
                String v = irv.getPropertyObject("d9_Plant_L6").getStringValue();
                if (v != null && !(v.trim().equalsIgnoreCase(""))) {
                    String[] m = v.split(",");
                    for (String s : m) {
                        plantMp.put(s.trim(), s.trim());
                    }
                }
                String str = "";
                Set<String> keys = plantMp.keySet();
                for (String key : keys) {
                    str += key + ",";
                }
                if (str.endsWith(",")) {
                    str = str.substring(0, str.length() - 1);
                }
                TCUtils.setProperties(dataManagementService, irv, "d9_Plant_L6", str);

                for (String plant : plantArr) {
                    JCoDestination jcoDest = DestinationUtils.getJCoDestination(plant.trim(), destination, destination888, destination868);
                    new PostMakerInforToSAP(dataManagementService, irv, plant, jcoDest, customPNMapper).post();
                }

            } catch (Exception e) {
            }
            //basicView 料号信息抛SAP失败
            if (msgs.contains("post  basicView  plant")) {
                applyCustomPnResponse.setCode(-1);
                applyCustomPnResponse.setMsg(msgs);
                return applyCustomPnResponse;
            }
            applyCustomPnResponse.setCode(1);
            applyCustomPnResponse.setMsg("success");
            applyCustomPnResponse.setNewItemId(customPartRp.getMaterialNumber());
            return applyCustomPnResponse;
        } catch (Exception e) {
            try {
                DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();
                ServiceData sdDataset = dataManagementService.loadObjects(new String[]{customPartRp.getIrUid()});
                ItemRevision irv = (ItemRevision) sdDataset.getPlainObject(0);
                TCUtils.getProperty(dataManagementService, irv, "d9_SAPLog");
                String v = irv.getPropertyObject("d9_SAPLog").getStringValue();
                TCUtils.setProperties(dataManagementService, irv, "d9_SAPLog", sdf.format(new Date()) + " " + e.getMessage() + "\n" + v);
            } catch (Exception e1) {
            }
            applyCustomPnResponse.setCode(-1);
            applyCustomPnResponse.setMsg(e.getMessage());
            return applyCustomPnResponse;
        }
    }


    /**
     * call rfc 生成自编料号
     *
     * @param destination
     * @param customPartRp
     * @return
     */
    private ApplyCustomPnResponse applyCustomPN(JCoDestination destination, CustomPartRp customPartRp, JCoDestination destination888, JCoDestination destination868, List<CustomPartRp> customPartRps, TCSOAServiceFactory tcSOAServiceFactory) {
        ApplyCustomPnResponse applyCustomPnResponse = new ApplyCustomPnResponse();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        try {

            SessionService sessionService = tcSOAServiceFactory.getSessionService();
            TCUtils.byPass(sessionService, true);
            log.info("apply custom pn ===========>" + customPartRp.getOldMaterialNumber() + " " + customPartRp.getRuleRegx());
            SavedQueryService savedQueryService = tcSOAServiceFactory.getSavedQueryService();
            DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();

            ServiceData sdDataset = dataManagementService.loadObjects(new String[]{customPartRp.getUid()});
            Item item = (Item) sdDataset.getPlainObject(0);
            dataManagementService.refreshObjects(new ModelObject[]{item});

            sdDataset = dataManagementService.loadObjects(new String[]{customPartRp.getIrUid()});
            ItemRevision irv = (ItemRevision) sdDataset.getPlainObject(0);
            dataManagementService.refreshObjects(new ModelObject[]{irv});
            boolean needUpdate = true;
            String ruleRegx = customPartRp.getRuleRegx().toLowerCase(Locale.ENGLISH);
            if(ruleRegx.indexOf("%")<0&&(ruleRegx.indexOf("@@@")>0)){
                throw new Exception("取號規則不正确");
            }
            TCUtils.getProperty(dataManagementService, irv, "item_id");
            String tmpid = irv.get_item_id();
            if(ruleRegx.indexOf("%")>0)  {
                String t = ruleRegx.substring(0, ruleRegx.indexOf("%"));
                if (tmpid.startsWith(t.toUpperCase(Locale.ENGLISH))) {
                    customPartRp.setMaterialNumber(tmpid);
                    needUpdate = false;
                }
            }
            applyCustomPnResponse.setUid(customPartRp.getUid());
            applyCustomPnResponse.setOldItemId(customPartRp.getOldMaterialNumber());
            if (!needUpdate) {
                applyCustomPnResponse.setCode(1);
                applyCustomPnResponse.setMsg("success");
                applyCustomPnResponse.setNewItemId(customPartRp.getMaterialNumber());
                return applyCustomPnResponse;
            }

            boolean is49Primary = false;
            boolean is49Second = false;
            int seqLen = 0;
            int seqCounter = 0;
            int needSeedView = -1;
            log.info("old uid=" + customPartRp.getUid());
            log.info("RuleRegx =" + customPartRp.getRuleRegx());
            String seqId = ruleRegx.replaceAll("\\%s[0-9]+d", "");
            log.info("seq id=" + seqId);
            Pattern p = Pattern.compile("\\%s([0-9])+d");
            Matcher m = p.matcher(ruleRegx);
            if (m.find()) {
                seqLen = Integer.parseInt(m.group(1));
            }
            //else {
            //  throw new Exception("临时料号不符合规则要求");
            // }
            log.info("seq len =================>=" + seqLen);
            String modelName = customPartRp.getModelName();
            if (seqId.indexOf("@@@") > -1 && modelName != null && !(modelName.trim().equalsIgnoreCase(""))) {
                String seqCounterStr = customPNMapper.getCustomSEQ(Access.check("79@@@-" + modelName));
                if (seqCounterStr != null) {
                    int seq = Integer.parseInt(seqCounterStr);
                    String newSeqStr = coverModelSeq(seq);
                    seqId = seqId.replaceAll("@@@", newSeqStr);
                }
            }
            if (seqId.startsWith("49")) {
                String primary49 = "";
                if (seqId.indexOf("#") > -1) {
                    String[] strs = seqId.split("#");
                    seqId = strs[0].trim();
                    primary49 = strs[1].trim().toUpperCase(Locale.ENGLISH);
                    if (primary49.startsWith("49")) {
                        int f = 0;
                        for (CustomPartRp c : customPartRps) {
                            if (c.getMaterialNumber().equalsIgnoreCase(primary49)) {
                                f = 1;
                                break;
                            }
                        }
                        if (f == 0) {
                            if (!isExistInTC(savedQueryService, primary49)) {
                                throw new Exception("主料" + primary49 + " 在TC中不存在");
                            }
                            if (!isRelease(savedQueryService, primary49, dataManagementService)) {
                                throw new Exception("主料 " + primary49 + " 未發行");
                            }
                        }
                        String matnum = ruleRegx.replaceAll("\\%s[0-9]+d", primary49.substring(4, 8));
                        matnum = matnum.substring(0, matnum.lastIndexOf("#"));
                        customPartRp.setMaterialNumber(matnum.toUpperCase(Locale.ENGLISH));
                        needSeedView = 0;
                        is49Second = true;
                    } else {
                        throw new Exception("主料 " + primary49 + " 要先出號");
                    }
                } else {
                    seqId = seqId.substring(0, seqId.length() - 2);
                    is49Primary = true;
                }
            }

            String seqCounterStr = customPNMapper.getCustomSEQ(Access.check(seqId.toUpperCase(Locale.ENGLISH)));
            if (seqCounterStr != null) {
                seqCounter = Integer.parseInt(seqCounterStr);
            }
            if(seqId.startsWith("49")){
                if(seqCounter<393039){
                    seqCounter=393039;
                }
            }
            ViewUtils viewUtils = new ViewUtils();
            //料号在SAP系统中已存在
            while (needSeedView != 0) {
                if(ruleRegx.indexOf("%")<0){
                    String pi=ruleRegx.toUpperCase(Locale.ENGLISH);
                    if(pi.indexOf("#")>-1){
                        pi=pi.substring(0,pi.indexOf("#"));
                    }
                    customPartRp.setMaterialNumber(pi);
                    customPartRp.setPlant("CHMB");
                    needSeedView=0;
                    continue;
                }
                seqCounter++;
                log.info("current  counter =================>=" + seqCounter);
                String newItemId = generalNewPN(seqCounter, ruleRegx, seqLen, customPartRp.getModelName());
                customPartRp.setMaterialNumber(newItemId.toUpperCase(Locale.ENGLISH));
                customPartRp.setPlant("CHMB");
                needSeedView = viewUtils.needSendView(destination, customPartRp);
                if (needSeedView != 0) {
                    //  customPartRp.setPlant("ACDC");
                    // needSeedView = viewUtils.needSendView(destination888, customPartRp);
                }
                if (needSeedView != 0) {
                    //customPartRp.setPlant("DCA1");
                    //needSeedView = viewUtils.needSendView(destination868, customPartRp);
                }

                if (isExistInTC(savedQueryService, newItemId)) {
                    needSeedView = -1;
                }
            }

            if (isExistInTC(savedQueryService, customPartRp.getMaterialNumber())) {
                throw new Exception(customPartRp.getMaterialNumber() + "料號重複");
            }

            if(ruleRegx.indexOf("%")>0) {
                if (seqId.indexOf("@@@") > -1) {
                    seqId = seqId.replaceAll("@@@", customPartRp.getMaterialNumber().substring(2, 5));
                }
                if (!is49Second) {
                    seqCounterStr = customPNMapper.getCustomSEQ(Access.check(seqId.toUpperCase(Locale.ENGLISH)));
                    if (seqCounterStr == null) {
                        customPNMapper.addSeqCounter(Access.check(seqId.toUpperCase(Locale.ENGLISH)), Access.check("" + seqCounter));
                    } else {
                        customPNMapper.updateSeqCounter(Access.check(seqId.toUpperCase(Locale.ENGLISH)), Access.check("" + seqCounter));
                    }
                }
            }
            if (is49Primary) {
                String om = customPartRp.getOldMaterialNumber().trim();
                for (CustomPartRp c : customPartRps) {
                    String reg = c.getRuleRegx();
                    if (reg.indexOf("#") < 0) {
                        continue;
                    }
                    if (reg.split("#")[1].trim().equalsIgnoreCase(om)) {
                        c.setRuleRegx(reg.split("#")[0] + "#" + customPartRp.getMaterialNumber());
                    }
                }

                Map<String, Object> queryResults = TCUtils.executeQuery(savedQueryService, "__D9_Find_49PCB",
                        new String[]{"d9_TempPN"}, new String[]{("49*#" + om)});
                if (queryResults.get("succeeded") != null) {
                    ModelObject[] md = (ModelObject[]) queryResults.get("succeeded");
                    if (md != null && md.length > 0) {
                        for (int k = 0; k < md.length; k++) {
                            ModelObject iv = md[k];
                            ItemRevision rev = (ItemRevision) findObjectByUid(dataManagementService, iv.getUid());
                            TCUtils.getProperty(dataManagementService, rev, "d9_TempPN");
                            String v = rev.getPropertyObject("d9_TempPN").getStringValue();
                            v = v.split("#")[0] + "#" + customPartRp.getMaterialNumber();
                            TCUtils.setProperties(dataManagementService, rev, "d9_TempPN", v);
                            dataManagementService.refreshObjects(new ModelObject[]{rev});
                        }
                    }
                }

            }

            String msgs = "";
            ServiceData srd = null;
            boolean upSuccess = true;
            TCUtils.byPass(sessionService, true);
            srd = TCUtils.setProperties(dataManagementService, item, "item_id", customPartRp.getMaterialNumber());
            if (srd.sizeOfPartialErrors() > 0) {
                upSuccess = false;
                String msg = srd.getPartialError(0).getMessages()[0];
                msgs += msg + "\n";
                System.out.println(msg);
                log.error(customPartRp.getOldMaterialNumber() + " set property item_id  error ===>" + msg);
            }
            TCUtils.setProperties(dataManagementService, item, "object_name", customPartRp.getMaterialNumber());
            TCUtils.setProperties(dataManagementService, irv, "object_name", customPartRp.getMaterialNumber());
            if (customPartRp.getMaterialNumber().startsWith("49")) {
                TCUtils.setProperties(dataManagementService, irv, "d9_ManufacturerPN", customPartRp.getMaterialNumber());
            }
            TCUtils.getProperty(dataManagementService, irv, "d9_SAPLog");
            String v = irv.getPropertyObject("d9_SAPLog").getStringValue();
            if (!"".equalsIgnoreCase(msgs)) {
                TCUtils.setProperties(dataManagementService, irv, "d9_SAPLog", sdf.format(new Date()) + " " + msgs + "\n" + v);
            } else {
                TCUtils.setProperties(dataManagementService, irv, "d9_SAPLog", sdf.format(new Date()) + " apply custom pn success\n" + v);
            }
            if (!upSuccess) {
                applyCustomPnResponse.setCode(-1);
                applyCustomPnResponse.setMsg("update item id faied !");
                return applyCustomPnResponse;
            }

            if (is79Main(dataManagementService, irv)) {
                Map<String, Object> queryResults = TCUtils.executeQuery(savedQueryService, "__D9_Find_Part_SupplementInfo",
                        new String[]{"d9_SupplementInfo"}, new String[]{(tmpid)});
                if (queryResults.get("succeeded") != null) {
                    ModelObject[] md = (ModelObject[]) queryResults.get("succeeded");
                    if (md != null && md.length > 0) {
                        for (int k = 0; k < md.length; k++) {
                            ModelObject iv = md[k];
                            ItemRevision rev = (ItemRevision) findObjectByUid(dataManagementService, iv.getUid());
                            TCUtils.setProperties(dataManagementService, rev, "d9_SupplementInfo", customPartRp.getMaterialNumber());
                        }
                    }
                }
            }
            applyCustomPnResponse.setCode(1);
            applyCustomPnResponse.setMsg("success");
            applyCustomPnResponse.setNewItemId(customPartRp.getMaterialNumber());
            return applyCustomPnResponse;
        } catch (Exception e) {
            try {
                DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();
                ServiceData sdDataset = dataManagementService.loadObjects(new String[]{customPartRp.getIrUid()});
                ItemRevision irv = (ItemRevision) sdDataset.getPlainObject(0);
                TCUtils.getProperty(dataManagementService, irv, "d9_SAPLog");
                String v = irv.getPropertyObject("d9_SAPLog").getStringValue();
                TCUtils.setProperties(dataManagementService, irv, "d9_SAPLog", sdf.format(new Date()) + " " + e.getMessage() + "\n" + v);
            } catch (Exception e1) {
            }
            applyCustomPnResponse.setCode(-1);
            applyCustomPnResponse.setMsg(e.getMessage());
            return applyCustomPnResponse;
        }

    }

    private boolean is79Main(DataManagementService dataManagementService, ItemRevision irv) {
        try {
            dataManagementService.refreshObjects(new ModelObject[]{irv});
            TCUtils.getProperty(dataManagementService, irv, "d9_TempPN");
            String tempPN = irv.getPropertyObject("d9_TempPN").getStringValue();
            if (!tempPN.startsWith("79")) {
                return false;
            }
            TCUtils.getProperty(dataManagementService, irv, "d9_ProductionType_L6");
            String productionType = irv.getPropertyObject("d9_ProductionType_L6").getStringValue();
            if ("INCLUDE (SMD/AI/OTHER/PRE-FORMING)".equalsIgnoreCase(productionType) || "IC+FW".equalsIgnoreCase(productionType)) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }


    private boolean is79Second(DataManagementService dataManagementService, ItemRevision irv) {
        try {
            dataManagementService.refreshObjects(new ModelObject[]{irv});
            TCUtils.getProperty(dataManagementService, irv, "d9_TempPN");
            String tempPN = irv.getPropertyObject("d9_TempPN").getStringValue();
            if (!tempPN.startsWith("79")) {
                return false;
            }
            TCUtils.getProperty(dataManagementService, irv, "d9_ProductionType_L6");
            String productionType = irv.getPropertyObject("d9_ProductionType_L6").getStringValue();
            if (!"INCLUDE (SMD/AI/OTHER/PRE-FORMING)".equalsIgnoreCase(productionType) && !"IC+FW".equalsIgnoreCase(productionType)) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * 检查防呆
     *
     * @param customPartRps
     * @return
     */
    private String checkCustomPart(List<CustomPartRp> customPartRps, TCSOAServiceFactory tcSOAServiceFactory) {

        try {

            DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();
            SavedQueryService savedQueryService = tcSOAServiceFactory.getSavedQueryService();
            Map<String, String> map79 = new HashMap<>();
            for (CustomPartRp customPartRp : customPartRps) {
                ServiceData sdData = dataManagementService.loadObjects(new String[]{customPartRp.getIrUid()});
                ItemRevision irv = (ItemRevision) sdData.getPlainObject(0);
                dataManagementService.refreshObjects(new ModelObject[]{irv});
                TCUtils.getProperty(dataManagementService, irv, "item_id");
                String itemId = irv.get_item_id();
                if (is79Main(dataManagementService, irv)) {
                    map79.put(itemId, itemId);
                }
            }
            for (CustomPartRp customPartRp : customPartRps) {
                ServiceData sdData = dataManagementService.loadObjects(new String[]{customPartRp.getIrUid()});
                ItemRevision irv = (ItemRevision) sdData.getPlainObject(0);
                dataManagementService.refreshObjects(new ModelObject[]{irv});
                TCUtils.getProperty(dataManagementService, irv, "d9_TempPN");
                String tempPN = irv.getPropertyObject("d9_TempPN").getStringValue();
                if (!tempPN.startsWith("49") && !tempPN.startsWith("629") && !tempPN.startsWith("7351") && !is79Second(dataManagementService, irv)) {
                    continue;
                }

                TCUtils.getProperty(dataManagementService, irv, "d9_SupplementInfo");
                String supplementInfo = irv.getPropertyObject("d9_SupplementInfo").getStringValue();
                if (map79.get(supplementInfo) != null) {
                    continue;
                }

                Map<String, Object> queryResults = TCUtils.executeQuery(savedQueryService, D9Constants.D9_ITEM_NAME_OR_ID,
                        new String[]{D9Constants.D9_ITEM_ID}, new String[]{supplementInfo});
                if (queryResults.get("succeeded") == null) {
                    throw new Exception(supplementInfo + " 未查询到对应的品名补充资讯");
                }
                ModelObject[] md = (ModelObject[]) queryResults.get("succeeded");
                if (md == null || md.length <= 0) {
                    throw new Exception(supplementInfo + " 未查询到对应的品名补充资讯");
                }
                ModelObject iv = md[0];
                Item itm = (Item) findObjectByUid(dataManagementService, iv.getUid());
                ItemRevision rev = TCUtils.getItemLatestRevision(dataManagementService, itm);
                dataManagementService.refreshObjects(new ModelObject[]{rev});
                TCUtils.getProperty(dataManagementService, rev, "d9_PCBAssyClassification_L6");
                String assyClassification = rev.getPropertyObject("d9_PCBAssyClassification_L6").getStringValue();
                TCUtils.getProperty(dataManagementService, rev, "d9_DerivativeTypeDC");
                TCUtils.getProperty(dataManagementService, rev, "d9_FoxconnModelName");
                String  modelName = "";
                String deri =   rev.getPropertyObject("d9_DerivativeTypeDC").getStringValue();
                if(StringUtils.hasLength(deri)){
                    modelName = rev.getPropertyObject("d9_FoxconnModelName").getStringValue() + "_" + deri;
                }else{
                    modelName = rev.getPropertyObject("d9_FoxconnModelName").getStringValue();
                }
                //TCUtils.getProperty(dataManagementService, rev, "d9_ModelName");
                //String modelName = rev.getPropertyObject("d9_ModelName").getStringValue();
                TCUtils.getProperty(dataManagementService, rev, "project_ids");
                String projs = rev.getPropertyObject("project_ids").getStringValue();
                if (projs == null || "".equalsIgnoreCase(projs.trim())) {
                    throw new Exception(supplementInfo + " 未签核归档");
                }
                String[] ps = projs.split(",");
                for (String projiectId : ps) {
                    Map<String, Object> queryRs = TCUtils.executeQuery(savedQueryService, D9Constants.D9_FIND_PROJECT_FOLDER,
                            new String[]{D9Constants.D9_SPAS_ID}, new String[]{(projiectId)});
                    if (queryRs.get("succeeded") == null) {
                        continue;
                    }
                    ModelObject[] pd = (ModelObject[]) queryRs.get("succeeded");
                    if (pd == null || pd.length <= 0) {
                        continue;
                    }
                    Folder archiveFolder = null;
                    Folder folder = (Folder) pd[0];
                    Folder fl = findFolder(dataManagementService, folder,"自編物料協同工作區");
                    if (fl == null) {
                        throw new Exception(supplementInfo + " 未签核归档");
                    }
                    dataManagementService.refreshObjects(new ModelObject[]{fl});
                    TCUtils.getProperty(dataManagementService, fl, "contents");
                    WorkspaceObject[] ws = fl.get_contents();
                    for (WorkspaceObject w : ws) {
                        if (!(w instanceof Folder)) {
                            continue;
                        }
                        Folder ff = (Folder) w;
                        TCUtils.getProperty(dataManagementService, ff, "object_name");
                        String s = ff.get_object_name();
                        if (s.equalsIgnoreCase(modelName)) {
                            TCUtils.getProperty(dataManagementService, ff, "contents");
                            WorkspaceObject[] wsc = ff.get_contents();
                            for (WorkspaceObject wc : wsc) {
                                if (!(wc instanceof Folder)) {
                                    continue;
                                }
                                TCUtils.getProperty(dataManagementService,  (Folder) wc, "object_name");
                                String wcn = wc.get_object_name();
                                if (wcn.equalsIgnoreCase(assyClassification)){
                                    archiveFolder = (Folder) wc;
                                    break;
                                }
                            }
                            break;
                        }
                    }
                    //List<Folder> findFolders=new ArrayList<>();
                    //findFolder(dataManagementService,fl,assyClassification,findFolders);

                    //if(findFolders.size()>0){
                      //  archiveFolder= findFolders.get(0);
                    //}
                    if (archiveFolder == null) {
                        throw new Exception(supplementInfo + " 未签核归档");
                    }
                    dataManagementService.refreshObjects(new ModelObject[]{archiveFolder});
                    TCUtils.getProperty(dataManagementService, archiveFolder, "contents");
                    WorkspaceObject[] ws2 = archiveFolder.get_contents();
                    boolean hasArchive = false;
                    for (WorkspaceObject w : ws2) {
                        if (!(w instanceof Item)) {
                            continue;
                        }
                        Item im = (Item) w;
                        TCUtils.getProperty(dataManagementService, im, "item_id");
                        String s = im.get_item_id();
                        if (supplementInfo.equalsIgnoreCase(s)) {
                            hasArchive = true;
                            break;
                        }
                    }
                    if (!hasArchive) {
                        throw new Exception(supplementInfo + " 未签核归档");
                    }
                }
            }
        } catch (Exception e) {
            return e.getMessage();
        }
        return "";
    }

    /**
     * 物料归档
     *
     * @param customPartRp
     */
    private ApplyCustomPnResponse archiveFolder(TCSOAServiceFactory tcSOAServiceFactory, CustomPartRp customPartRp) {
        ApplyCustomPnResponse applyCustomPnResponse = new ApplyCustomPnResponse();
        String itemId = "";
        try {
            log.info("begin archiveFolder item id ======" + customPartRp.getMaterialNumber());
            DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();
            SavedQueryService savedQueryService = tcSOAServiceFactory.getSavedQueryService();

            ServiceData sdDataset = dataManagementService.loadObjects(new String[]{customPartRp.getUid()});
            Item item = (Item) sdDataset.getPlainObject(0);
            dataManagementService.refreshObjects(new ModelObject[]{item});

            sdDataset = dataManagementService.loadObjects(new String[]{customPartRp.getIrUid()});
            ItemRevision irv = (ItemRevision) sdDataset.getPlainObject(0);
            dataManagementService.refreshObjects(new ModelObject[]{irv});

            String projs = "";
            String assyClassification = null;
            TCUtils.getProperty(dataManagementService, irv, "item_id");
            itemId = irv.getPropertyObject("item_id").getStringValue();
            log.info("item id ======" + itemId);
            //衍生機種挂關係
            TCUtils.getProperty(dataManagementService, irv, "d9_TempPN");
            String tempPN = irv.getPropertyObject("d9_TempPN").getStringValue();
            if(tempPN.startsWith("79")){
                TCUtils.getProperty(dataManagementService, irv, "d9_DerivativeTypeDC");
                String derivativeType = irv.getPropertyObject("d9_DerivativeTypeDC").getStringValue();
                if(derivativeType!=null&&!"".equalsIgnoreCase(derivativeType.trim())){
                    TCUtils.getProperty(dataManagementService, irv, "d9_SupplementInfo");
                    String supplementInfo = irv.getPropertyObject("d9_SupplementInfo").getStringValue();
                    if(supplementInfo!=null&&!"".equalsIgnoreCase(supplementInfo.trim())){
                        Map<String, Object> queryResults = TCUtils.executeQuery(savedQueryService, D9Constants.D9_ITEM_NAME_OR_ID,
                                new String[]{D9Constants.D9_ITEM_ID}, new String[]{(supplementInfo)});
                        if (queryResults.get("succeeded") != null) {
                            ModelObject[] md = (ModelObject[]) queryResults.get("succeeded");
                            if (md != null && md.length > 0) {
                                ModelObject iv = md[0];
                                Item itm = (Item) findObjectByUid(dataManagementService, iv.getUid());
                                ItemRevision rev = TCUtils.getItemLatestRevision(dataManagementService, itm);
                                dataManagementService.refreshObjects(new ModelObject[]{rev});
                                TCUtils.byPass(tcSOAServiceFactory.getSessionService(), true);
                                TCUtils.addRelation(dataManagementService,rev,irv,"D9_HasDerivedBOM_REL");
                            }
                        }
                    }
                }
            }

            String modelName = null;
            if (is79Main(dataManagementService, irv)) {
                TCUtils.getProperty(dataManagementService, irv, "d9_PCBAssyClassification_L6");
                assyClassification = irv.getPropertyObject("d9_PCBAssyClassification_L6").getStringValue();
                TCUtils.getProperty(dataManagementService, irv, "project_ids");
                projs = irv.getPropertyObject("project_ids").getStringValue();
                // model name d9_ModelName
                TCUtils.getProperty(dataManagementService, irv, "d9_DerivativeTypeDC");
                TCUtils.getProperty(dataManagementService, irv, "d9_FoxconnModelName");
                String deri =   irv.getPropertyObject("d9_DerivativeTypeDC").getStringValue();
                if(StringUtils.hasLength(deri)){
                    modelName = irv.getPropertyObject("d9_FoxconnModelName").getStringValue() + "_" + deri;
                }else{
                    modelName = irv.getPropertyObject("d9_FoxconnModelName").getStringValue();
                }
            } else if (itemId.startsWith("49") || itemId.startsWith("629") || itemId.startsWith("7351") || is79Second(dataManagementService, irv)) {
                TCUtils.getProperty(dataManagementService, irv, "d9_SupplementInfo");
                String supplementInfo = irv.getPropertyObject("d9_SupplementInfo").getStringValue();
                log.info("supplementInfo ======" + supplementInfo);
                Map<String, Object> queryResults = TCUtils.executeQuery(savedQueryService, D9Constants.D9_ITEM_NAME_OR_ID,
                        new String[]{D9Constants.D9_ITEM_ID}, new String[]{(supplementInfo)});
                if (queryResults.get("succeeded") != null) {
                    ModelObject[] md = (ModelObject[]) queryResults.get("succeeded");
                    if (md != null && md.length > 0) {
                        ModelObject iv = md[0];
                        Item itm = (Item) findObjectByUid(dataManagementService, iv.getUid());
                        ItemRevision rev = TCUtils.getItemLatestRevision(dataManagementService, itm);
                        TCUtils.getProperty(dataManagementService, rev, "d9_PCBAssyClassification_L6");
                        assyClassification = rev.getPropertyObject("d9_PCBAssyClassification_L6").getStringValue();
                        TCUtils.getProperty(dataManagementService, rev, "project_ids");
                        projs = rev.getPropertyObject("project_ids").getStringValue();
                        TCUtils.getProperty(dataManagementService, rev, "d9_DerivativeTypeDC");
                        TCUtils.getProperty(dataManagementService, rev, "d9_FoxconnModelName");
                        String deri =   rev.getPropertyObject("d9_DerivativeTypeDC").getStringValue();
                        if(StringUtils.hasLength(deri)){
                            modelName = rev.getPropertyObject("d9_FoxconnModelName").getStringValue() + "_" + deri;
                        }else{
                            modelName = rev.getPropertyObject("d9_FoxconnModelName").getStringValue();
                        }
                        log.info("assyClassification ======" + assyClassification + "  modelName:" + modelName);
                    }
                }
            } else {
                TCUtils.getProperty(dataManagementService, irv, "project_ids");
                projs = irv.getPropertyObject("project_ids").getStringValue();
            }
            if (projs == null || "".equalsIgnoreCase(projs.trim())) {
                applyCustomPnResponse.setCode(1);
                applyCustomPnResponse.setMsg("success");
                return applyCustomPnResponse;
            }
            log.info("projs ======" + projs);
            if ((itemId.startsWith("79") || itemId.startsWith("49") || itemId.startsWith("629") || itemId.startsWith("7351")) && assyClassification == null) {
                applyCustomPnResponse.setCode(1);
                applyCustomPnResponse.setMsg("success");
                return applyCustomPnResponse;
            }
            String[] ps = projs.split(",");
            for (String projiectId : ps) {
                Map<String, Object> queryResults = TCUtils.executeQuery(savedQueryService, D9Constants.D9_FIND_PROJECT_FOLDER,
                        new String[]{D9Constants.D9_SPAS_ID}, new String[]{(projiectId)});
                if (queryResults.get("succeeded") == null) {
                    continue;
                }
                ModelObject[] md = (ModelObject[]) queryResults.get("succeeded");
                if (md == null || md.length <= 0) {
                    continue;
                }
                Folder folder = (Folder) md[0];
                if(itemId.startsWith("8")||itemId.startsWith("713")){
                   archive8And713(tcSOAServiceFactory,folder,irv);
                  continue;
               }
                // PCA歸檔文件夾加
                Folder archiveFolder = null;
                //自編物料協同工作區 文件夾
                Folder fl = findFolder(dataManagementService, folder,"自編物料協同工作區");
                if (fl == null) {
                    log.info(itemId+" 未找到歸檔文件夾");
                    continue;
                }
                Folder modelFolder = null;
                if(StringUtils.hasLength(modelName)){
                    List<Folder> modelFolders= new ArrayList<>();
                    findFolder(dataManagementService,fl,modelName,modelFolders);
                    if(modelFolders.size()>0){
                        modelFolder=  modelFolders.get(0);
                    }else{
                        //  創建Model文件夾
                        Map<String, String> propMap = new HashMap<>();
                        propMap.put(D9Constants.OBJECT_NAME, modelName);
                        propMap.put(D9Constants.OBJECT_DESC, modelName);
                        com.teamcenter.services.strong.core._2008_06.DataManagement.CreateResponse response = TCUtils.createObjects(dataManagementService, "Folder", propMap);
                        ServiceData serviceData = response.serviceData;
                        if (serviceData.sizeOfPartialErrors() <= 0) {
                            ModelObject[] folders = response.output[0].objects;
                            modelFolder = (Folder) folders[0];
                            DataManagement.Relationship[] relationships = new DataManagement.Relationship[1];
                            relationships[0] = new DataManagement.Relationship();
                            relationships[0].primaryObject = fl;
                            relationships[0].secondaryObject = modelFolder;
                            relationships[0].relationType = "contents";
                            dataManagementService.createRelations(relationships);
                            dataManagementService.refreshObjects(new ModelObject[]{fl});
                        }
                    }
                }
                if (assyClassification != null) {
                    List<Folder> archiveFolders= new ArrayList<>();
                    findFolder(dataManagementService,modelFolder,assyClassification,archiveFolders);
                    if(archiveFolders.size()>0){
                        archiveFolder=  archiveFolders.get(0);
                    }
                    if (archiveFolder == null && itemId.startsWith("79")) {
                        Map<String, String> propMap = new HashMap<>();
                        propMap.put(D9Constants.OBJECT_NAME, assyClassification);
                        propMap.put(D9Constants.OBJECT_DESC, assyClassification);
                        com.teamcenter.services.strong.core._2008_06.DataManagement.CreateResponse response = TCUtils.createObjects(dataManagementService, "Folder", propMap);
                        ServiceData serviceData = response.serviceData;
                        if (serviceData.sizeOfPartialErrors() <= 0) {
                            ModelObject[] folders = response.output[0].objects;
                            Folder f = (Folder) folders[0];
                            DataManagement.Relationship[] relationships = new DataManagement.Relationship[1];
                            relationships[0] = new DataManagement.Relationship();
                            relationships[0].primaryObject = modelFolder;
                            relationships[0].secondaryObject = f;
                            relationships[0].relationType = "contents";
                            dataManagementService.createRelations(relationships);
                            dataManagementService.refreshObjects(new ModelObject[]{modelFolder});
                            archiveFolder = f;
                        }
                    }
                    if (archiveFolder == null) {
                        continue;
                    }
                } else {
                    archiveFolder = fl;
                }

                String primaryUid = archiveFolder.getUid();
                log.info("找到了歸檔文件夾===" + primaryUid);
                String secondaryUid = item.getUid();
                ModelObject obj1 = findObjectByUid(dataManagementService, primaryUid);
                ModelObject obj2 = findObjectByUid(dataManagementService, secondaryUid);
                DataManagement.Relationship[] relationships = new DataManagement.Relationship[1];
                relationships[0] = new DataManagement.Relationship();
                relationships[0].primaryObject = obj1;
                relationships[0].secondaryObject = obj2;
                relationships[0].relationType = "contents";
                dataManagementService.createRelations(relationships);
                dataManagementService.refreshObjects(new ModelObject[]{obj1});
            }
            applyCustomPnResponse.setCode(1);
            applyCustomPnResponse.setMsg("success");
            log.info("end archiveFolder item id ======" + customPartRp.getMaterialNumber());
            return applyCustomPnResponse;
        } catch (Exception e) {
            log.info(itemId + " 归档失败=========");
            log.error(itemId + " 归档失败=========", e);
            log.info("end archiveFolder item id ======" + customPartRp.getMaterialNumber());
            applyCustomPnResponse.setCode(-1);
            applyCustomPnResponse.setMsg(customPartRp.getMaterialNumber()+"歸檔失敗");
            return applyCustomPnResponse;
        }

    }


    private void archive8And713(TCSOAServiceFactory tcSOAServiceFactory,Folder projFolder,ItemRevision irv)throws Exception {
        DataManagementService dataManagementService= tcSOAServiceFactory.getDataManagementService();
        TCUtils.getProperty(dataManagementService, irv, "item_id");
        String  itemId = irv.getPropertyObject("item_id").getStringValue();
        Folder fl=null;
        if(itemId.startsWith("8")) {
            fl = findFolder(dataManagementService, projFolder, "成品料號協同工作區");
        }else if(itemId.startsWith("713")){
            fl = findFolder(dataManagementService, projFolder, "包裝物料協同工作區");
        }
        if(fl==null){
             log.info(itemId+" 未找到歸檔文件夾");
             return;
        }
        if(itemId.startsWith("713")) {
            String primaryUid = fl.getUid();
            String secondaryUid = irv.getUid();
            ModelObject obj1 = findObjectByUid(dataManagementService, primaryUid);
            ModelObject obj2 = findObjectByUid(dataManagementService, secondaryUid);
            DataManagement.Relationship[] relationships = new DataManagement.Relationship[1];
            relationships[0] = new DataManagement.Relationship();
            relationships[0].primaryObject = obj1;
            relationships[0].secondaryObject = obj2;
            relationships[0].relationType = "contents";
            dataManagementService.createRelations(relationships);
            dataManagementService.refreshObjects(new ModelObject[]{obj1});
            return;
        }

        String modelName=null;
        TCUtils.getProperty(dataManagementService, irv, "d9_DerivativeTypeDC");
        TCUtils.getProperty(dataManagementService, irv, "d9_FoxconnModelName");
        String deri =   irv.getPropertyObject("d9_DerivativeTypeDC").getStringValue();
        if(StringUtils.hasLength(deri)){
            modelName = irv.getPropertyObject("d9_FoxconnModelName").getStringValue() + "_" + deri;
        }else{
            modelName = irv.getPropertyObject("d9_FoxconnModelName").getStringValue();
        }
       if(modelName==null){
           log.info(itemId+" 未找到model name");
           return;
       }
        Folder  modelFolder=null;
        List<Folder> archiveFolders= new ArrayList<>();
        findFolder(dataManagementService,fl,modelName,archiveFolders);
        if(archiveFolders.size()>0){
            modelFolder=  archiveFolders.get(0);
        }else {
            Map<String, String> propMap = new HashMap<>();
            propMap.put(D9Constants.OBJECT_NAME, modelName);
            propMap.put(D9Constants.OBJECT_DESC, modelName);
            com.teamcenter.services.strong.core._2008_06.DataManagement.CreateResponse response = TCUtils.createObjects(dataManagementService, "Folder", propMap);
            ServiceData serviceData = response.serviceData;
            if (serviceData.sizeOfPartialErrors() <= 0) {
                ModelObject[] folders = response.output[0].objects;
                modelFolder = (Folder) folders[0];
                DataManagement.Relationship[] relationships = new DataManagement.Relationship[1];
                relationships[0] = new DataManagement.Relationship();
                relationships[0].primaryObject = fl;
                relationships[0].secondaryObject = modelFolder;
                relationships[0].relationType = "contents";
                dataManagementService.createRelations(relationships);
                dataManagementService.refreshObjects(new ModelObject[]{fl});
            }
        }
        if(modelFolder==null){
            log.info(itemId+" 創建model folder 失敗");
            return;
        }
        String primaryUid = modelFolder.getUid();
        String secondaryUid = irv.getUid();
        ModelObject obj1 = findObjectByUid(dataManagementService, primaryUid);
        ModelObject obj2 = findObjectByUid(dataManagementService, secondaryUid);
        DataManagement.Relationship[] relationships = new DataManagement.Relationship[1];
        relationships[0] = new DataManagement.Relationship();
        relationships[0].primaryObject = obj1;
        relationships[0].secondaryObject = obj2;
        relationships[0].relationType = "contents";
        dataManagementService.createRelations(relationships);
        dataManagementService.refreshObjects(new ModelObject[]{obj1});

    }

    private Folder findFolder(DataManagementService dataManagementService, Folder f,String fname) {
        try {
            dataManagementService.refreshObjects(new ModelObject[]{f});
            TCUtils.getProperty(dataManagementService, f, "contents");
            WorkspaceObject[] ws = f.get_contents();
            Folder f1 = null;
            for (WorkspaceObject w : ws) {
                if (!(w instanceof Folder)) {
                    continue;
                }
                Folder ff = (Folder) w;
                TCUtils.getProperty(dataManagementService, ff, "object_name");
                String s = ff.get_object_name();
                if ("產品設計協同工作區".equalsIgnoreCase(s)) {
                    f1 = ff;
                    break;
                }
            }
            if (f1 == null) {
                return null;
            }
            dataManagementService.refreshObjects(new ModelObject[]{f1});
            TCUtils.getProperty(dataManagementService, f1, "contents");
            WorkspaceObject[] ws1 = f1.get_contents();
            for (WorkspaceObject w : ws1) {
                if (!(w instanceof Folder)) {
                    continue;
                }
                Folder ff = (Folder) w;
                TCUtils.getProperty(dataManagementService, ff, "object_name");
                String s = ff.get_object_name();

                if (fname.equalsIgnoreCase(s)) {
                //if ("自編物料協同工作區".equalsIgnoreCase(s)) {
                    return ff;
                }
            }
        } catch (Exception e) {
        }
        return null;
    }


    private void findFolder(DataManagementService dataManagementService, Folder f,String folderName,List<Folder>  findFolders) {
        try {
            dataManagementService.refreshObjects(new ModelObject[]{f});
            TCUtils.getProperty(dataManagementService, f, "contents");
            WorkspaceObject[] ws = f.get_contents();
            for (WorkspaceObject w : ws) {
                if (!(w instanceof Folder)) {
                    continue;
                }
                Folder ff = (Folder) w;
                TCUtils.getProperty(dataManagementService, ff, "object_name");
                String s = ff.get_object_name();
                if (folderName.equalsIgnoreCase(s)) {
                    findFolders.add(ff);
                    break;
                }else{
                    findFolder(dataManagementService,ff,folderName,findFolders);
                }
            }
        } catch (Exception e) {
        }
    }


    /**
     * 生成自编料号
     *
     * @param seqCounter
     * @param materialNumber
     * @param seqLen
     * @return
     * @throws Exception
     */
    private String generalNewPN(int seqCounter, String materialNumber, int seqLen, String modelName) throws Exception {
        int codesLen = codes.length;
        String newStr = "";
        while (seqCounter > 0) {
            int n = seqCounter % codesLen;
            newStr = codes[n] + newStr;
            seqCounter = seqCounter / codesLen;
        }
        if (newStr.length() > seqLen) {
            throw new Exception("流水码用完了....");
        }
        while (newStr.length() < seqLen) {
            newStr = "0" + newStr;
        }
        System.out.println(newStr);
        materialNumber = materialNumber.replaceAll("\\%s[0-9]+d", newStr);
        System.out.println(materialNumber);

        if (materialNumber.indexOf("@@@") > -1 && modelName != null && !(modelName.trim().equalsIgnoreCase(""))) {
            String s = customPNMapper.getCustomSEQ(Access.check("79@@@-" + modelName));
            if (s == null) {
                String seqCounterStr = customPNMapper.getCustomSEQ(Access.check("79@@@"));
                int seq = Integer.parseInt(seqCounterStr);
                seq++;
                String newSeqStr = coverModelSeq(seq);
                materialNumber = materialNumber.replaceAll("@@@", newSeqStr);
                customPNMapper.addSeqCounter(Access.check("79@@@-" + modelName), Access.check("" + seq));
                customPNMapper.updateSeqCounter(Access.check("79@@@"), Access.check("" + seq));

            } else {
                String newSeqStr = coverModelSeq(Integer.parseInt(s));
                materialNumber = materialNumber.replaceAll("@@@", newSeqStr);
            }
        }


        return materialNumber;
    }


    private String coverModelSeq(int seqCounter) throws Exception {
        int codesLen = codes.length;
        String newStr = "";
        while (seqCounter > 0) {
            int n = seqCounter % codesLen;
            newStr = codes[n] + newStr;
            seqCounter = seqCounter / codesLen;
        }
        if (newStr.length() > 3) {
            throw new Exception("流水码用完了....");
        }
        while (newStr.length() < 3) {
            newStr = "0" + newStr;
        }
        System.out.println(newStr);
        return newStr;
    }

    /**
     * 抛转物料信息到SAP
     *
     * @param destination
     * @param part
     * @return
     */
    public String postDataToSAP(JCoDestination destination, CustomPartRp part) {
        String errorMsg = "";
        try {
            ViewUtils viewUtils = new ViewUtils();
            ViewPoster viewPoster = new ViewPoster();
            int needSendView = viewUtils.needSendView(destination, part);
            if (needSendView == 0) {
                String msg = viewPoster.postBasicView(customPNMapper, destination, part, DestinationUtils.is888(part.getPlant()));
                if (!"S".equalsIgnoreCase(msg)) {
                    errorMsg += part.getOldMaterialNumber() + " post  basicView  plant " + part.getPlant() + " failed " + msg + "\n";
                    return errorMsg;
                }

                msg = viewPoster.postBasicViewCH(customPNMapper, destination, part, DestinationUtils.is888(part.getPlant()));
                if (!"S".equalsIgnoreCase(msg)) {
                    errorMsg += part.getOldMaterialNumber() + " post  basicView  plant " + part.getPlant() + " failed " + msg + "\n";
                    return errorMsg;
                }

            }
            needSendView = viewUtils.needSendView(destination, part);
            if (needSendView == 4) {
                if (viewUtils.getNeedPost(customPNMapper, part.getPlant(), part.getMaterialType(), "sd")) {
                    String msg = viewPoster.postSDViewAndCostingView(customPNMapper, destination, part);
                    if (!"S".equalsIgnoreCase(msg)) {
                        errorMsg += part.getOldMaterialNumber() + " post  SDView  plant " + part.getPlant() + " failed " + msg + "\n";
                        return errorMsg;
                    }
                }
            }

            if ((needSendView != 3) && (needSendView != 4)) {
                if (viewUtils.getNeedPost(customPNMapper, part.getPlant(), part.getMaterialType(), "sd")) {
                    String msg = viewPoster.postSDViewAndCostingView(customPNMapper, destination, part);
                    if (!"S".equalsIgnoreCase(msg)) {
                        errorMsg += part.getOldMaterialNumber() + " post  SDView2  plant " + part.getPlant() + " failed " + msg + "\n";
                        return errorMsg;
                    }
                }

                if (viewUtils.getNeedPost(customPNMapper, part.getPlant(), part.getMaterialType(), "purchasing")) {
                    String msg = viewPoster.postPurchasingAndCostingView(customPNMapper, destination, part);
                    if (!"S".equalsIgnoreCase(msg)) {
                        errorMsg += part.getOldMaterialNumber() + " post  purchasingView  plant " + part.getPlant() + " failed " + msg + "\n";
                        return errorMsg;
                    }
                }


                if (viewUtils.getNeedPost(customPNMapper, part.getPlant(), part.getMaterialType(), "mrp")) {
                    String msg = viewPoster.postMRPView(customPNMapper, destination, part);
                    if (!"S".equalsIgnoreCase(msg)) {
                        errorMsg += part.getOldMaterialNumber() + " post  mrpView  plant " + part.getPlant() + " failed " + msg + "\n";
                        return errorMsg;
                    }
                }

                if (viewUtils.getNeedPost(customPNMapper, part.getPlant(), part.getMaterialType(), "accounting")) {
                    String msg = viewPoster.postAccountingView(customPNMapper, destination, part);
                    if (!"S".equalsIgnoreCase(msg)) {
                        errorMsg += part.getOldMaterialNumber() + " post  accountingView  plant " + part.getPlant() + " failed " + msg + "\n";
                        return errorMsg;
                    }
                }
            } else if (needSendView == 3) {
                System.out.print("the part's other views have been existed in sap!");
            }
        } catch (Exception e) {
            errorMsg = e.getMessage();
        }
        return errorMsg;
    }

    public static ModelObject findObjectByUid(DataManagementService dataManagementService, String uid) {
        ServiceData sd = dataManagementService.loadObjects(new String[]{uid});
        return sd.getPlainObject(0);
    }


    private boolean isExistInTC(SavedQueryService savedQueryService, String itemId) {

        Map<String, Object> queryResults = TCUtils.executeQuery(savedQueryService, D9Constants.D9_ITEM_NAME_OR_ID,
                new String[]{D9Constants.D9_ITEM_ID}, new String[]{itemId.toUpperCase(Locale.ENGLISH)});
        if (queryResults.get("succeeded") != null) {
            ModelObject[] md = (ModelObject[]) queryResults.get("succeeded");
            if (md != null && md.length > 0) {
                return true;
            }
        }

        return false;
    }


    private boolean isRelease(SavedQueryService savedQueryService, String itemId, DataManagementService dataManagementService) {
        try {
            Map<String, Object> queryResults = TCUtils.executeQuery(savedQueryService, D9Constants.D9_ITEM_NAME_OR_ID,
                    new String[]{D9Constants.D9_ITEM_ID}, new String[]{(itemId.toUpperCase(Locale.ENGLISH))});
            ModelObject[] md = (ModelObject[]) queryResults.get("succeeded");
            ModelObject iv = md[0];
            Item itm = (Item) findObjectByUid(dataManagementService, iv.getUid());
            ItemRevision rev = TCUtils.getItemLatestRevision(dataManagementService, itm);
            dataManagementService.refreshObjects(new ModelObject[]{rev});
            TCUtils.getProperty(dataManagementService, rev, "release_status_list");
            List list = rev.getPropertyObject("release_status_list").getModelObjectListValue();
            if (list.size() > 0) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;

    }

}
