package com.foxconn.plm.integrate.agile.service;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.agile.api.*;
import com.foxconn.plm.entity.constants.TCPropName;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.integrate.agile.domain.BOMInfo;
import com.foxconn.plm.integrate.mail.domain.MailUser;
import com.foxconn.plm.integrate.mail.service.MailGroupSettingService;
import com.foxconn.plm.integrate.mail.utils.MailSupport;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.string.StringUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.rac.kernel.TCException;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.loose.core.SessionService;
import com.teamcenter.services.strong.administration.PreferenceManagementService;
import com.teamcenter.services.strong.cad.StructureManagementService;
import com.teamcenter.services.strong.cad._2007_01.StructureManagement;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core.ReservationService;
import com.teamcenter.services.strong.core._2013_05.DataManagement;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.services.strong.structuremanagement.StructureService;
import com.teamcenter.services.strong.structuremanagement._2012_09.Structure;
import com.teamcenter.services.strong.workflow.WorkflowService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DTEBOMServiceImpl {

    @Value("${agile.l6.url:}")
    private String agileUrl;

    @Value("${agile.l6.user:}")
    private String agileUser;

    @Value("${agile.l6.pword:}")
    private String agilePw;

    private static Log log = LogFactory.get();
    @Autowired(required = false)
    MailGroupSettingService mailGroupSettingImpl;

    public IAgileSession connectAgile() {
        IAgileSession agileSession = null;
        Map<Integer, String> params = new HashMap<>();
        try {
            AgileSessionFactory factory = AgileSessionFactory.getInstance(agileUrl);
            params.put(AgileSessionFactory.PASSWORD, agilePw);
            params.put(AgileSessionFactory.USERNAME, agileUser);
            agileSession = factory.createSession(params);
            log.info(agileUrl + " connect agile success !");
        } catch (APIException e) {
            e.printStackTrace();
        }
        return agileSession;
    }

    public File getAgileBOMFile(IChange change) throws APIException, IOException {
        ITable attachments = change.getTable(ItemConstants.TABLE_ATTACHMENTS);
        for (Object object : attachments) {
            IRow row = (IRow) object;
            String fileName = row.getCell(CommonConstants.ATT_ATTACHMENTS_FILE_NAME).toString();
            if (fileName.matches("^.*(_EZBOM_).*(_BOM_).*$")) {
                IAttachmentFile attachmentFile = (IAttachmentFile) object;
                if (attachmentFile.isSecure()) {
                    InputStream inputStream = attachmentFile.getFile();
                    String temPath = System.getProperty("java.io.tmpdir");
                    Path file = Paths.get(temPath + fileName);
                    Files.copy(inputStream, file, StandardCopyOption.REPLACE_EXISTING);
                    return file.toFile();
                }
            }
        }
        return null;
    }

    public String buildAgileBOM(String ecnNO) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        log.info("==================构建【DT L6 EBOM】开始：" + dateTimeFormatter.format(LocalDateTime.now()) + "==================");
        IAgileSession agileSession = connectAgile();
        String result = "";
        try {
            IChange change = (IChange) agileSession.getObject(IChange.OBJECT_TYPE, ecnNO);
            BOMInfo bomInfo = getAgileBOM(agileSession, change);
            File bomFile = getAgileBOMFile(change);
            uploadEBOM(bomFile, bomInfo);
            result = "S";
        } catch (Exception e) {
            result = "BOM Sync TC Fail : " + e.getLocalizedMessage();
            e.printStackTrace();
        } finally {
            agileSession.close();
        }
        log.info("==================构建【DT L6 EBOM】完成：" + dateTimeFormatter.format(LocalDateTime.now()) + "==================");
        return result;
    }

    public BOMInfo getAgileBOM(IAgileSession agileSession, IChange change) throws APIException {
        String pcaNumber = change.getValue(1575).toString();
        IItem pca = (IItem) agileSession.getObject(IItem.OBJECT_TYPE, pcaNumber);
        BOMInfo bomInfo = getAgileBOM_(pca, null);
        bomInfo.setPnRev(change.getValue(1332).toString());
        bomInfo.setBomRev(change.getValue(1334).toString());
        bomInfo.setCustomer(change.getValue(1003).toString());
        bomInfo.setProject(change.getValue(1301).toString());
        bomInfo.setPhase(change.getValue(1303).toString());
        return bomInfo;
    }


    public BOMInfo getAgileBOM_(IItem agileItem, BOMInfo bomInfo) {
        boolean isTop = false;
        if (bomInfo == null) {
            isTop = true;
            bomInfo = new BOMInfo();
        }
        List<BOMInfo> childList = new ArrayList<>();
        try {
            setItemAttribute(agileItem, bomInfo);
            ITable productBOMTable = agileItem.getTable(ItemConstants.TABLE_BOM);
            for (Object o : productBOMTable) {
                BOMInfo childBOMInfo = new BOMInfo();
                childBOMInfo.setVirtualPart(isTop);
                IRow bomRow = (IRow) o;
                setBOMInfoByAgile(bomRow, childBOMInfo);
                IItem childItem = (IItem) bomRow.getReferent();
                getAgileBOM_(childItem, childBOMInfo);
                childList.add(childBOMInfo);
            }
        } catch (APIException e) {
            e.printStackTrace();
        }
        bomInfo.setChild(childList);
        return bomInfo;
    }


    void setBOMInfoByAgile(IRow bomRow, BOMInfo bomInfo) {
        try {
            // bom attribute :
            mappingBOMAttribute(bomRow, bomInfo);
            IItem item = (IItem) bomRow.getReferent();
            //setItemAttribute(item, bomInfo);
            //mfg:
            ITable mfgTable = item.getTable(ItemConstants.TABLE_MANUFACTURERS);
            if (mfgTable.size() > 0) {
                IRow mfgRow = (IRow) mfgTable.getTableIterator().next();
                bomInfo.setSupplier(mfgRow.getCell(1902).toString());
                bomInfo.setSupplierPN(mfgRow.getCell(1946).toString());
            }
        } catch (APIException e) {
            e.printStackTrace();
        }
    }

    void setItemAttribute(IItem item, BOMInfo bomInfo) throws APIException {
        bomInfo.setMaterialGroup(item.getValue(2007).toString());
        bomInfo.setMaterialType(item.getValue(2020).toString());
        bomInfo.setPartNum(item.getValue(1001).toString());
        bomInfo.setDescription(item.getValue(1002).toString());
    }


    public void mappingBOMAttribute(IRow bomRow, BOMInfo bomInfo) {
        Field[] fields = BOMInfo.class.getDeclaredFields();
        for (Field field : fields) {
            ReflectionUtils.makeAccessible(field);
            TCPropName tcPropName = field.getAnnotation(TCPropName.class);
            if (tcPropName != null) {
                String val = tcPropName.value();
                if (StringUtil.isNotEmpty(val)) {
                    try {
                        field.set(bomInfo, bomRow.getCell(Integer.parseInt(val)).toString());
                    } catch (IllegalAccessException | APIException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    public void uploadEBOM(File file, BOMInfo bomInfo) throws Exception {
        TCSOAServiceFactory tCSOAServiceFactory = null;
        BOMWindow[] bomWindows = new BOMWindow[1];
        StructureManagementService smService = null;
        try {
            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            smService = tCSOAServiceFactory.getStructureManagementService();
            SessionService sessionService = tCSOAServiceFactory.getSessionService();
            PreferenceManagementService preferenceManagementService = tCSOAServiceFactory.getPreferenceManagementService();
            TCUtils.byPass(sessionService, true);
            ItemRevision pcaPartRev = getPartRev(bomInfo, tCSOAServiceFactory);
            bomInfo.setItemRev(pcaPartRev);
            setPartRev(bomInfo, tCSOAServiceFactory);
            log.info("==========================开始构建EBOM =======================================");
            ArrayList<Object> newBOMWindows = openBOMWindow(smService, pcaPartRev);
            if (null == newBOMWindows) {
                throw new Exception("创建BOMWindows失败！");
            }
            bomWindows[0] = (BOMWindow) newBOMWindows.get(0);
            BOMLine parentBOMLine = (BOMLine) newBOMWindows.get(1);
            bomInfo.setBomLine(parentBOMLine);
            buildEBOM(bomInfo, tCSOAServiceFactory);
            for (BOMInfo childBom : bomInfo.getChild()) {
                buildVirtualEBOM(childBom, tCSOAServiceFactory);
            }
            smService.saveBOMWindows(bomWindows);
            createFolder(bomInfo, pcaPartRev, tCSOAServiceFactory);
            log.info("==========================构建EBOM结束=======================================");
            if (file != null) {
                log.info("==========================开始上传文件=======================================");
                TCUtils.uploadDataset(tCSOAServiceFactory.getDataManagementService(), tCSOAServiceFactory.getFileManagementUtility(), pcaPartRev,
                        file.getAbsolutePath(),
                        "excel", file.getName(), "MSExcel");
                log.info("==========================上传文件结束=======================================");
            }
            log.info("==========================开始发邮件 =======================================");
            //sendMail(preferenceManagementService, bomInfo);
            TCUtils.byPass(sessionService, false);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (bomWindows[0] != null) {
                smService.closeBOMWindows(bomWindows);
            }
            if (tCSOAServiceFactory != null) {
                tCSOAServiceFactory.logout();
            }
        }
    }

    public void createFolder(BOMInfo bomInfo, ItemRevision pcaPartRev, TCSOAServiceFactory tCSOAServiceFactory) throws Exception {
        String project = bomInfo.getProject();
        String partNum = bomInfo.getPartNum();
        String phase = bomInfo.getPhase();
        String bomRev = bomInfo.getBomRev();
        filing(tCSOAServiceFactory.getDataManagementService(), tCSOAServiceFactory.getPreferenceManagementService(), project, partNum, phase,
                bomRev, pcaPartRev);
    }

    public void buildVirtualEBOM(BOMInfo bomInfo, TCSOAServiceFactory tCSOAServiceFactory) throws Exception {
        List<BOMInfo> edaPartList = bomInfo.getChild();
        //根据altGroup分组
        Map<String, List<BOMInfo>> edaComPartMap = edaPartList.stream().collect(Collectors.groupingBy(BOMInfo::getAltGroup));
        //处理主料/替代料
        List<BOMInfo> edaParts = handlePriAlt(edaComPartMap);
        bomInfo.setChild(edaParts);
        setPartRev(bomInfo, tCSOAServiceFactory);
        buildEBOM(bomInfo, tCSOAServiceFactory);
    }

    private List<BOMInfo> handlePriAlt(Map<String, List<BOMInfo>> edaComPartMap) {
        List<BOMInfo> edaComPartList = new ArrayList<>();
        for (Map.Entry<String, List<BOMInfo>> item : edaComPartMap.entrySet()) {
            List<BOMInfo> bomInfos = item.getValue();
            BOMInfo bomInfoPri = bomInfos.stream().filter(i -> i.getAltCode().equals("PRI")).collect(Collectors.toList()).get(0);
            List<BOMInfo> bomInfoAlt = bomInfos.stream().filter(i -> i.getAltCode().equals("ALT")).collect(Collectors.toList());
            bomInfoPri.setSubstitute(bomInfoAlt);
            edaComPartList.add(bomInfoPri);
        }
        return edaComPartList;
    }

    private void setPartRev(BOMInfo bomInfo, TCSOAServiceFactory tCSOAServiceFactory) throws Exception {
        List<BOMInfo> child = bomInfo.getChild();
        if (child != null && child.size() > 0) {
            for (int i = 0; i < child.size(); i++) {
                BOMInfo childBOMInfo = child.get(i);
                ItemRevision edaPartRev = getPartRev(childBOMInfo, tCSOAServiceFactory);
                childBOMInfo.setItemRev(edaPartRev);
                List<BOMInfo> substitute = childBOMInfo.getSubstitute();
                if (substitute != null && substitute.size() > 0) {
                    for (int j = 0; j < substitute.size(); j++) {
                        BOMInfo substituteBOMInfo = substitute.get(j);
                        ItemRevision substitutePartRev = getPartRev(substituteBOMInfo, tCSOAServiceFactory);
                        substituteBOMInfo.setItemRev(substitutePartRev);
                    }
                }
            }
        }
    }

    private ItemRevision getPartRev(BOMInfo bomInfo, TCSOAServiceFactory tCSOAServiceFactory) throws Exception {

        DataManagementService dmService = tCSOAServiceFactory.getDataManagementService();
        WorkflowService wfService = tCSOAServiceFactory.getWorkflowService();
        SavedQueryService queryService = tCSOAServiceFactory.getSavedQueryService();
        ItemRevision partRev = null;
        String partNum = bomInfo.getPartNum();
        String project = bomInfo.getProject();
        ModelObject[] modelObject = TCUtils.executeSOAQuery(queryService, "Item_Name_or_ID", new String[]{"item_id"}, new String[]{partNum});
        if (modelObject != null && modelObject.length > 0) {
            Item partItem = (Item) modelObject[0];
            partRev = TCUtils.getItemLatestRevision(dmService, partItem);

            if ("ZROH".equalsIgnoreCase(bomInfo.getMaterialType())) {
                return partRev;
            }

            if (project == null) {
                TCUtils.getProperty(dmService, partRev, "d9_EnglishDescription");
                String desc = partRev.getPropertyObject("d9_EnglishDescription").getStringValue();
                String description = bomInfo.getDescription();
                if (!desc.equals(description)) {
                    boolean isReleased = TCUtils.isReleased1(dmService, partRev, "Release");
                    if (!isReleased) {
//                        TCUtils.createNewProcess(wfService, "TCM Release Process :" + partRev.toString(),
//                                "FXN53_PCA EBOM Quick Release Process", new ModelObject[]{partRev});
                        TCUtils.addStatus(wfService, new WorkspaceObject[]{partRev}, "D9_Release");
                    }
                    String versionRule = getVersionRule(dmService, partRev);
                    String objType = partRev.get_object_type();
                    String newRevId = generateVersion(dmService, versionRule, objType, partRev.getUid());
                    partRev = (ItemRevision) TCUtils.reviseItemRev(dmService, partItem, newRevId);
                    Map<String, String> props = new HashMap<>();
                    props.put("d9_EnglishDescription", bomInfo.getDescription());
                    props.put("d9_ManufacturerID", bomInfo.getSupplier());
                    props.put("d9_ManufacturerPN", bomInfo.getSupplierPN());
                    props.put("d9_MaterialType", bomInfo.getMaterialType());
                    props.put("d9_MaterialGroup", bomInfo.getMaterialGroup());
                    props.put("d9_Un", bomInfo.getUnit());
                    TCUtils.setProperties(dmService, partRev, props);
                }
            } else {
                boolean isReleased = TCUtils.isReleased1(dmService, partRev, "Release");
                if (!isReleased) {
                    TCUtils.addStatus(wfService, new WorkspaceObject[]{partRev}, "D9_Release");
//                    TCUtils.createNewProcess(wfService, "TCM Release Process :" + partRev.toString(),
//                            "FXN53_PCA EBOM Quick Release Process", new ModelObject[]{partRev});

                }
                String versionRule = getVersionRule(dmService, partRev);
                String objType = partRev.get_object_type();
                String newRevId = generateVersion(dmService, versionRule, objType, partRev.getUid());
                partRev = (ItemRevision) TCUtils.reviseItemRev(dmService, partItem, newRevId);
            }
        } else {
            String itemType = "";
            if (bomInfo.isVirtualPart()) {
                itemType = "D9_VirtualPart";
            } else if (project != null) {
                itemType = "D9_PCA_Part";
            } else {
                itemType = "EDAComPart";
            }
            partRev = TCUtils.createItem(dmService, partNum, "A", itemType, partNum, null);
            Map<String, String> props = new HashMap<>();
            if (project == null) {
                props.put("d9_EnglishDescription", bomInfo.getDescription());
                props.put("d9_ManufacturerID", bomInfo.getSupplier());
                props.put("d9_ManufacturerPN", bomInfo.getSupplierPN());
                props.put("d9_MaterialType", bomInfo.getMaterialType());
                props.put("d9_MaterialGroup", bomInfo.getMaterialGroup());
                props.put("d9_Un", bomInfo.getUnit());
            } else {
                props.put("d9_EnglishDescription", bomInfo.getDescription());
                props.put("d9_Un", bomInfo.getUnit());
            }
            TCUtils.setProperties(dmService, partRev, props);
            if ("EDAComPart".equalsIgnoreCase(itemType)) {
                boolean isReleased = TCUtils.isReleased1(dmService, partRev, "Release");
                if (!isReleased) {
                    TCUtils.addStatus(wfService, new WorkspaceObject[]{partRev}, "D9_Release");
//                    TCUtils.createNewProcess(wfService, "TCM Release Process :" + partRev.toString(),
//                            "FXN53_PCA EBOM Quick Release Process", new ModelObject[]{partRev});
                }
            }
        }
        return partRev;
    }

    /**
     * 返回升版规则
     *
     * @param itemRev
     * @return
     * @throws TCException
     */
    private String getVersionRule(DataManagementService dmService, ItemRevision itemRev) throws Exception {
        TCUtils.getProperty(dmService, itemRev, "item_revision_id");
        String version = itemRev.get_item_revision_id();
        String versionRule = "";
        if (version.matches("[0-9]+")) { // 判断对象版本是否为数字版
            versionRule = "NN";
        } else if (version.matches("[a-zA-Z]+")) { // 判断对象版本是否为字母版
            versionRule = "@";
        }
        return versionRule;
    }

    public static String generateVersion(DataManagementService dmService, String ruleMapping, String itemTypeRevName, String itemRevUid) {
        String version = null;
        try {
            DataManagement.GenerateNextValuesIn[] ins = new DataManagement.GenerateNextValuesIn[1];
            DataManagement.GenerateNextValuesIn in = new DataManagement.GenerateNextValuesIn();
            ins[0] = in;
            in.businessObjectName = itemTypeRevName;
            in.clientId = "AutoAssignRAC";
            in.operationType = 2;

            Map<String, String> map = new HashMap<String, String>();
            map.put("item_revision_id", ruleMapping);
            in.propertyNameWithSelectedPattern = map;

            Map<String, String> map1 = new HashMap<String, String>();
            map1.put("sourceObject", itemRevUid);
            in.additionalInputParams = map1;

            DataManagement.GenerateNextValuesResponse response = dmService.generateNextValues(ins);
            DataManagement.GeneratedValuesOutput[] outputs = response.generatedValues;
            for (DataManagement.GeneratedValuesOutput result : outputs) {
                Map<String, DataManagement.GeneratedValue> resultMap = result.generatedValues;
                DataManagement.GeneratedValue generatedValue = resultMap.get("item_revision_id");
                version = generatedValue.nextValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return version;
    }

    private void removeChildren(DataManagementService dmService, StructureManagementService smService, ReservationService rvService,
                                BOMLine topBOMLine, String[] uids) throws Exception {
        ModelObject outobj = null;

        TCUtils.getProperty(dmService, topBOMLine, "bl_item");
        Item item = (Item) topBOMLine.get_bl_item();
        TCUtils.getProperty(dmService, item, "item_revision");
        ItemRevision itemRev = TCUtils.getItemLatestRevision(dmService, item);

        com.teamcenter.services.strong.cad._2008_06.StructureManagement.DeleteRelativeStructureInfo3[] deleteRelativeStructureInfo3s =
                new com.teamcenter.services.strong.cad._2008_06.StructureManagement.DeleteRelativeStructureInfo3[1];
        com.teamcenter.services.strong.cad._2008_06.StructureManagement.DeleteRelativeStructureInfo3 deleteRelativeStructureInfo3 =
                new com.teamcenter.services.strong.cad._2008_06.StructureManagement.DeleteRelativeStructureInfo3();
        deleteRelativeStructureInfo3.childInfo = uids;
        deleteRelativeStructureInfo3.parent = itemRev;
        deleteRelativeStructureInfo3s[0] = deleteRelativeStructureInfo3;

        com.teamcenter.services.strong.cad._2007_12.StructureManagement.DeleteRelativeStructurePref2 deleteRelativeStructurePref2 =
                new com.teamcenter.services.strong.cad._2007_12.StructureManagement.DeleteRelativeStructurePref2();
        deleteRelativeStructurePref2.cadOccIdAttrName = "bl_occurrence_uid";
        deleteRelativeStructurePref2.overwriteForLastModDate = false;

        TCUtils.getProperty(dmService, itemRev, "structure_revisions");
        PSBOMViewRevision[] boms = itemRev.get_structure_revisions();
        if (boms != null) {
            PSBOMViewRevision bom = boms[0];
            outobj = checkout(dmService, rvService, bom);
        }

        StructureManagement.DeleteRelativeStructureResponse delResponse = smService.deleteRelativeStructure(deleteRelativeStructureInfo3s, "",
                deleteRelativeStructurePref2);
        ServiceData serviceData = delResponse.serviceData;
        if (serviceData.sizeOfPartialErrors() > 0) {
            String message = serviceData.getPartialError(0).getMessages()[0];
            throw new Exception(message);
        }

        if (outobj != null) {
            checkin(dmService, rvService, outobj);
        }
    }

    private void buildEBOM(BOMInfo bomInfo, TCSOAServiceFactory tCSOAServiceFactory) throws Exception {
        DataManagementService dmService = tCSOAServiceFactory.getDataManagementService();
        PreferenceManagementService pmService = tCSOAServiceFactory.getPreferenceManagementService();
        StructureService structureService = tCSOAServiceFactory.getStructureService();
        ReservationService rvService = tCSOAServiceFactory.getReservationService();
        StructureManagementService smService = tCSOAServiceFactory.getStructureManagementService();
        WorkflowService wfService = tCSOAServiceFactory.getWorkflowService();
        ItemRevision parentItemRev = bomInfo.getItemRev();
        List<BOMInfo> child = bomInfo.getChild();
        try {
            BOMLine parentBOMLine = bomInfo.getBomLine();
            TCUtils.getProperty(dmService, parentBOMLine, "bl_all_child_lines");
            ModelObject[] children = parentBOMLine.get_bl_all_child_lines();
            List<BOMLine> BOMLineList = new ArrayList<>();
            for (ModelObject modelObject : children) {
                BOMLine childBOMLine = (BOMLine) modelObject;
                TCUtils.getProperty(dmService, childBOMLine, "fnd0bl_is_substitute");
                boolean fnd0bl_is_substitute = childBOMLine.get_fnd0bl_is_substitute();
                if (!fnd0bl_is_substitute) {
                    BOMLineList.add(childBOMLine);
                }
            }
            for (BOMLine childBOMLine : BOMLineList) {
                TCUtils.getProperty(dmService, childBOMLine, "bl_occurrence_uid");
                String uid = childBOMLine.get_bl_occurrence_uid();
                removeChildren(dmService, smService, rvService, parentBOMLine, new String[]{uid});
            }
            for (BOMInfo childBOMInfo : child) {
                ItemRevision childItemRev = childBOMInfo.getItemRev();
                Map<String, String> props = new HashMap<>();
                if ("KEA".equalsIgnoreCase(childBOMInfo.getUnit())) {
                    props.put("bl_uom", "0nmJ5YD5ppJG1D");
                } else {
                    props.put("bl_quantity", childBOMInfo.getQty());
                }
                props.put("bl_sequence_no", childBOMInfo.getFindNum());
                props.put("bl_occ_d9_Location", childBOMInfo.getLocation());
                props.put("bl_occ_d9_AltGroup", childBOMInfo.getAltGroup());
                props.put("bl_occ_d9_CCL", childBOMInfo.getCcl());
                BOMLine newBOMline = addBOMLine(dmService, structureService, parentBOMLine, childItemRev, 4, props);
                if ("KEA".equalsIgnoreCase(childBOMInfo.getUnit())) {
                    dmService.refreshObjects(new ModelObject[]{newBOMline});
                    TCUtils.setProperties(dmService, newBOMline, "bl_quantity", childBOMInfo.getQty());
                }
                List<BOMInfo> substituteBOMInfos = childBOMInfo.getSubstitute();
                if (substituteBOMInfos != null) {
                    for (BOMInfo substituteBOMInfo : substituteBOMInfos) {
                        ItemRevision substituteItemRev = substituteBOMInfo.getItemRev();
                        addBOMLine(dmService, structureService, newBOMline, substituteItemRev, 1, null);
                    }
                }
                childBOMInfo.setBomLine(newBOMline);
            }
            boolean isReleased = TCUtils.isReleased1(dmService, parentItemRev, "Release");
            if (!isReleased) {
                TCUtils.addStatus(wfService, new WorkspaceObject[]{parentItemRev}, "D9_Release");
            }
//                TCUtils.createNewProcess(wfService, "TCM Release Process :" + parentItemRev.toString(),
//                        "FXN53_PCA EBOM Quick Release Process", new ModelObject[]{parentItemRev});
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    private BOMLine addBOMLine(DataManagementService dmService, StructureService structureService, BOMLine parentBOMLine, ItemRevision childItemRev
            , int flags, Map<String, String> propsMap) throws Exception {

        TCUtils.getProperty(dmService, childItemRev, "items_tag");
        Item childItem = childItemRev.get_items_tag();

        StructureService.AddParam[] addParams = new StructureService.AddParam[1];
        StructureService.AddParam addParam = new StructureService.AddParam();
        addParams[0] = addParam;
        addParam.flags = flags;
        addParam.parent = parentBOMLine;
        StructureService.AddInformation[] addInformations = new Structure.AddInformation[1];
        StructureService.AddInformation addInformation = new StructureService.AddInformation();
        addInformations[0] = addInformation;
        addInformation.item = childItem;
        addInformation.itemRev = childItemRev;
        if (propsMap != null) {
            addInformation.initialValues = propsMap;
        }
        addParam.toBeAdded = addInformations;
        Structure.AddResponse addResp = structureService.add(addParams);
        ServiceData serviceData = addResp.serviceData;
        if (serviceData.sizeOfPartialErrors() > 0) {
            return null;
        }
        return addResp.addedLines[0];
    }

    public ArrayList<Object> openBOMWindow(com.teamcenter.services.strong.cad.StructureManagementService cadSMService, ItemRevision parent) {
        ArrayList<Object> bomWindowandParentLine = new ArrayList(2);
        try {
            com.teamcenter.services.strong.cad._2007_01.StructureManagement.CreateBOMWindowsInfo[] createBOMWindowsInfo =
                    new com.teamcenter.services.strong.cad._2007_01.StructureManagement.CreateBOMWindowsInfo[1];
            createBOMWindowsInfo[0] = new com.teamcenter.services.strong.cad._2007_01.StructureManagement.CreateBOMWindowsInfo();
            createBOMWindowsInfo[0].itemRev = parent;
            createBOMWindowsInfo[0].clientId = "BOMUtils";
            createBOMWindowsInfo[0].item = parent.get_items_tag();
            com.teamcenter.services.strong.cad._2007_01.StructureManagement.CreateBOMWindowsResponse createBOMWindowsResponse =
                    cadSMService.createBOMWindows(createBOMWindowsInfo);
            if (createBOMWindowsResponse.serviceData.sizeOfPartialErrors() > 0) {
                for (int i = 0; i < createBOMWindowsResponse.serviceData.sizeOfPartialErrors(); i++) {
                    log.error("Partial Error in Open BOMWindow = " + createBOMWindowsResponse.serviceData
                            .getPartialError(i).getMessages()[0]);
                }
                return null;
            }
            com.teamcenter.services.strong.cad._2007_01.StructureManagement.CreateBOMWindowsOutput[] output = createBOMWindowsResponse.output;
            if (output == null || output.length == 0) {
                return null;
            }
            bomWindowandParentLine.add(output[0].bomWindow);
            bomWindowandParentLine.add(output[0].bomLine);
            return bomWindowandParentLine;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void checkin(DataManagementService dmService, ReservationService rs, ModelObject object) throws Exception {
        PSBOMViewRevision psbomViewRevision = (PSBOMViewRevision) object;
        //判断是否已经被签出
        dmService.refreshObjects(new ModelObject[]{psbomViewRevision});
        dmService.getProperties(new ModelObject[]{psbomViewRevision}, new String[]{"checked_out"});
        //是否签出的标志 Y带包已经签出, ""代表已经签入
        String checkedOut = psbomViewRevision.get_checked_out();
        //无需重复签入
        if ("".equals(checkedOut)) {
            return;
        }
        ModelObject[] objects = new ModelObject[1];
        objects[0] = object;
        ServiceData servicedata = rs.checkin(objects);
        if (servicedata.sizeOfPartialErrors() > 0) {
            throw new ServiceException("ReservationService checkin returned a partial error.");
        }
        return;
    }

    public static ModelObject checkout(DataManagementService dmService, ReservationService rvService, ModelObject object) throws Exception {
        ModelObject checkoutobject = null;
        PSBOMViewRevision psbomViewRevision = (PSBOMViewRevision) object;
        //判断是否已经被签出
        dmService.refreshObjects(new ModelObject[]{psbomViewRevision});
        dmService.getProperties(new ModelObject[]{psbomViewRevision}, new String[]{"checked_out"});
        //是否签出的标志 Y带包已经签出, ""代表已经签入
        String checkedOut = psbomViewRevision.get_checked_out();
        //如果已经签出, 如果已经签出, 则先签入, 然后再进行签出
        if ("Y".equals(checkedOut)) {
            checkin(dmService, rvService, psbomViewRevision);
        }
        ModelObject[] objects = new ModelObject[1];
        objects[0] = object;
        ServiceData servicedata = rvService.checkout(objects, "ImportData", "");
        if (servicedata.sizeOfPartialErrors() > 0) {
            throw new ServiceException("ReservationService checkout returned a partial error.");
        }
        checkoutobject = servicedata.getUpdatedObject(0);
        return checkoutobject;
    }

    private void filing(DataManagementService dmService, PreferenceManagementService pmService,
                        String project, String partNum, String phase, String bomRev, ItemRevision itemRev) throws Exception {
        String[] tcPreferences = TCUtils.getTCPreferences(pmService, "D9_DT_L6_EBOM_Folder");
        if (tcPreferences.length == 0) {
            throw new Exception("EBOM构建成功，归档时未找到首选项！");
        }

        Folder EBOMFolder = (Folder) TCUtils.findObjectByUid(dmService, tcPreferences[0]);
        Folder projectFolder = null;
        Folder partNumFolder = null;
        Folder phaseFolder = null;
        Folder bomRevFolder = null;

        projectFolder = getFolderByName(dmService, EBOMFolder, project);
        if (projectFolder == null) {
            projectFolder = TCUtils.createFolder(dmService, project);
            TCUtils.addContents(dmService, EBOMFolder, projectFolder);
        }

        partNumFolder = getFolderByName(dmService, projectFolder, partNum);
        if (partNumFolder == null) {
            partNumFolder = TCUtils.createFolder(dmService, partNum);
            TCUtils.addContents(dmService, projectFolder, partNumFolder);
        }

        phaseFolder = getFolderByName(dmService, partNumFolder, phase);
        if (phaseFolder == null) {
            phaseFolder = TCUtils.createFolder(dmService, phase);
            TCUtils.addContents(dmService, partNumFolder, phaseFolder);
        }

        bomRevFolder = getFolderByName(dmService, phaseFolder, bomRev);
        if (bomRevFolder == null) {
            bomRevFolder = TCUtils.createFolder(dmService, bomRev);
            TCUtils.addContents(dmService, phaseFolder, bomRevFolder);
        }

        TCUtils.addContents(dmService, (ModelObject) bomRevFolder, (ModelObject) itemRev);
    }

    private Folder getFolderByName(DataManagementService dmService, Folder primaryFolder, String chilName) throws Exception {

        Folder chilFolder = null;

        TCUtils.getProperty(dmService, primaryFolder, "contents");
        WorkspaceObject[] chilObjs = primaryFolder.get_contents();

        for (int i = 0; i < chilObjs.length; i++) {
            WorkspaceObject chilObj = chilObjs[i];
            if (chilObj instanceof Folder) {
                TCUtils.getProperty(dmService, chilObj, "object_name");
                String folderName = chilObj.get_object_name();
                if (folderName.equals(chilName)) {
                    chilFolder = (Folder) chilObj;
                }
            }
        }

        return chilFolder;
    }


    public void sendMail(PreferenceManagementService preferenceManagementService, BOMInfo bomInfo) {
        MailSupport mailSupport = new MailSupport();
        String customer = bomInfo.getCustomer();
        String mailGroup = "";
        if (customer == null || "".equalsIgnoreCase(customer)) {
            return;
        }
        customer = customer.toLowerCase(Locale.ENGLISH);
        if (customer.contains("hp")) {
            mailGroup = "HP PCA EBOM Notice Group";
        } else if (customer.contains("dell")) {
            mailGroup = "Dell PCA EBOM Notice Group";
        } else if (customer.contains("lenovo")) {
            mailGroup = "Lenovo PCA EBOM Notice Group";
        }

        if ("".equalsIgnoreCase(mailGroup)) {
            return;
        }

        List<MailUser> mailUsers = mailGroupSettingImpl.getGroupUsersByName(mailGroup);

        if (mailUsers == null || mailUsers.size() <= 0) {
            return;
        }
        for (MailUser u : mailUsers) {
            String html = mailSupport.genL6EBOMMailBody(u.getUserName(), bomInfo);
            HashMap<String, String> httpmap = new HashMap<String, String>();
            httpmap.put("ruleName", "/tc-mail/teamcenter/sendMail3");
            httpmap.put("sendTo", u.getMail());
            String subTitle = "【TC Sync Notice】【" + bomInfo.getProject() + "】.【" + bomInfo.getPartNum() + "】.【" + bomInfo.getPnRev() + "】is already" +
                    " sync to Teamcenter";
            if (bomInfo.getPnRev() == null || "".equalsIgnoreCase(bomInfo.getPnRev().trim())) {
                subTitle = "【TC Sync Notice】【" + bomInfo.getProject() + "】.【" + bomInfo.getPartNum() + "】is already sync to Teamcenter";
            }
            httpmap.put("subject", subTitle);
            httpmap.put("htmlmsg", html);
            mailSupport.sendMai(preferenceManagementService, httpmap);
        }


    }

    public static void main(String[] args) throws Exception {
    }
}
