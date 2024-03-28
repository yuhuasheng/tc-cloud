package com.foxconn.plm.tcservice.ebom.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCPropName;
import com.foxconn.plm.entity.pojo.ActualUserPojo;
import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcservice.ebom.constant.AlternativeConstant;
import com.foxconn.plm.tcservice.ebom.domain.EBOMLineBean;
import com.foxconn.plm.utils.string.StringUtil;
import com.foxconn.plm.utils.tc.ActualUserUtil;
import com.foxconn.plm.utils.tc.DatasetUtil;
import com.foxconn.plm.utils.tc.StructureManagementUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.google.gson.Gson;
import com.teamcenter.rac.kernel.*;
import com.teamcenter.services.strong.cad.StructureManagementService;
import com.teamcenter.services.strong.cad._2008_06.StructureManagement;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.Property;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import com.teamcenter.soa.exceptions.NotLoadedException;
import com.teamcenter.soaictstubs.ICCTBOMLine;
import com.teamcenter.soaictstubs.uidSeq_tHolder;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class EBOMServiceImpl {

    private final Log log = LogFactory.get();

    private String[] bomProps;

    private String[] itemRevProps;


    private ICCTBOMLine icctbomLine;

    @Resource
    private TcMailClient tcMailClient;

    enum WFTaskRealUser {
        taskName, realUserName, tcUserId, realMail;
    }

    public void setPropArray() {
        if (bomProps == null || itemRevProps == null) {
            List<String>[] propList = getTCPropNames(EBOMLineBean.class);
            propList[0].add("fnd0bl_is_substitute");
            propList[0].add("bl_revision");
            propList[0].add("bl_substitute_list");
            propList[0].add("bl_has_children");
            bomProps = propList[0].toArray(new String[0]);
            itemRevProps = propList[1].toArray(new String[0]);
        }
    }

    public void loadProps(DataManagementService ds, ModelObject modelObject) {
        if (modelObject instanceof BOMLine) {
            ds.getProperties(new ModelObject[]{modelObject}, bomProps);
        } else if (modelObject instanceof ItemRevision) {
            ds.getProperties(new ModelObject[]{modelObject}, itemRevProps);
        }
    }

    public EBOMLineBean getBOMStruct(TCSOAServiceFactory tcsoaServiceFactory, ItemRevision itemRevision) {
        setPropArray();
        StructureManagementService smService = tcsoaServiceFactory.getStructureManagementService();
        DataManagementService ds = tcsoaServiceFactory.getDataManagementService();
        icctbomLine = new ICCTBOMLine(tcsoaServiceFactory.getConnection(), "BOMLine", "TYPE::BOMLine::BOMLine::RuntimeBusinessObject");
        List out = StructureManagementUtil.openBOMWindow(smService, itemRevision);
        if (out != null && out.size() == 2) {
            BOMWindow bomWindow = (BOMWindow) out.get(0);
            BOMLine topLine = (BOMLine) out.get(1);
            EBOMLineBean rootBean = null;
            try {
                loadProps(ds, topLine);
                rootBean = getBOMStruct_(smService, ds, topLine);
            } catch (NotLoadedException | TCException | IllegalAccessException e) {
                e.printStackTrace();
            } finally {
                StructureManagementUtil.closeBOMWindow(smService, bomWindow);
            }
            return rootBean;
        } else {
            log.info(" get getBOMStruct null");
        }
        return null;
    }

    public EBOMLineBean newEBOMLineBean(DataManagementService ds, BOMLine bomLine) {

        try {
            EBOMLineBean bomLineBean = new EBOMLineBean();
            tcPropMapping(ds, bomLineBean, bomLine);
            ItemRevision itemRev = (ItemRevision) bomLine.get_bl_revision();
            if (itemRev == null) {
                Item item = (Item) bomLine.get_bl_item();
                TCUtils.getProperty(ds, item, "revision_list");
                ModelObject[] itemRevs = item.get_revision_list();
                itemRev = (ItemRevision) itemRevs[itemRevs.length - 1];
            }
            bomLineBean.setItemRevUid(itemRev.getUid());
            return bomLineBean;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public EBOMLineBean getBOMStruct_(StructureManagementService smService, DataManagementService ds, BOMLine topLine
    ) throws NotLoadedException, TCException, IllegalAccessException {
        EBOMLineBean ebomLineBean = newEBOMLineBean(ds, topLine);
        if (topLine.get_bl_has_children()) {
            com.teamcenter.services.strong.cad._2008_06.StructureManagement.ExpandPSOneLevelInfo info =
                    new StructureManagement.ExpandPSOneLevelInfo();
            info.parentBomLines = new BOMLine[]{topLine};
            info.excludeFilter = "None2";
            com.teamcenter.services.strong.cad._2008_06.StructureManagement.ExpandPSOneLevelPref pref =
                    new com.teamcenter.services.strong.cad._2008_06.StructureManagement.ExpandPSOneLevelPref();
            StructureManagement.ExpandPSOneLevelResponse2 response = smService.expandPSOneLevel(info, pref);
            if (response.output.length > 0) {
                StructureManagement.ExpandPSData[] children = response.output[0].children;
                for (StructureManagement.ExpandPSData expandPSData : children) {
                    BOMLine childline = expandPSData.bomLine;
                    loadProps(ds, childline);
                    System.out.println("get bom  :: " + ebomLineBean.getItem() + " child :: " + childline.get_bl_item_item_id());
                    if (!childline.get_fnd0bl_is_substitute()) {
                        EBOMLineBean childBean = getBOMStruct_(smService, ds, childline);
                        setLocationByDesignBOM(childBean, childline);
                        childBean.setParentItem(ebomLineBean.getItem());
                        childBean.setParentRevUid(ebomLineBean.getItemRevUid());
                        if (childline.get_bl_has_substitutes()) {
                            ModelObject[] modelObjectSubs = getSubsLine(childline, ds);
                            List<EBOMLineBean> subBeans = new ArrayList<>();
                            for (ModelObject subModelObject : modelObjectSubs) {
                                BOMLine subBomline = (BOMLine) subModelObject;
                                loadProps(ds, subBomline);
                                EBOMLineBean subBean = tcPropMapping(ds, new EBOMLineBean(), subBomline);
                                subBean.setParentItem(childBean.getParentItem());
                                subBean.setIsSecondSource(true);
                                subBean.setMainSource(childBean.getItem());
                                subBean.setLocation("");
                                subBean.setBomId();
                                subBean.setParentItem(childBean.getParentRevUid());
                                subBean.setAlternativeCode(AlternativeConstant.ALT);
                                subBeans.add(subBean);
                            }
                            childBean.setSecondSource(subBeans);
                        }
                        childBean.setAlternativeCode(AlternativeConstant.PRI);
                        ebomLineBean.getChilds().add(childBean);
                    }
                }
            }
        }
        return ebomLineBean;
    }


    public List<String>[] getTCPropNames(Class cls) {
        List<String> bomPropList = new ArrayList<>();
        List<String> itemPropList = new ArrayList<>();
        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            ReflectionUtils.makeAccessible(field);
            TCPropName tcPropName = field.getAnnotation(TCPropName.class);
            if (tcPropName != null) {
                String tcAttrName = tcPropName.value();
                String tcOtherAttrName = tcPropName.otherVal();
                if (StringUtil.isNotEmpty(tcAttrName)) {
                    if (tcAttrName.startsWith("bl")) {
                        bomPropList.add(tcAttrName);
                    } else {
                        itemPropList.add(tcAttrName);
                    }
                }
                if (StringUtil.isNotEmpty(tcOtherAttrName)) {
                    if (tcOtherAttrName.startsWith("bl")) {
                        bomPropList.add(tcOtherAttrName);
                    } else {
                        itemPropList.add(tcOtherAttrName);
                    }
                }

            }
        }
        List<String>[] props = new ArrayList[2];
        props[0] = bomPropList;
        props[1] = itemPropList;
        return props;
    }

    public EBOMLineBean tcPropMapping(DataManagementService ds, EBOMLineBean bean, ModelObject modelObject) throws IllegalArgumentException,
            IllegalAccessException, NotLoadedException {
        if (bean != null && modelObject != null) {
            ItemRevision itemRev = null;
            if (modelObject instanceof BOMLine) {
                itemRev = (ItemRevision) modelObject.getPropertyObject("bl_revision").getModelObjectValue();
                if (itemRev == null) {
                    Item item = (Item) modelObject.getPropertyObject("bl_item").getModelObjectValue();
                    TCUtils.getProperty(ds, item, "revision_list");
                    ModelObject[] itemRevs = item.get_revision_list();
                    itemRev = (ItemRevision) itemRevs[itemRevs.length - 1];
                }
            } else if (modelObject instanceof Item) {
                TCUtils.getProperty(ds, modelObject, "revision_list");
                ModelObject[] itemRevs = modelObject.getPropertyObject("revision_list").getModelObjectArrayValue();
                itemRev = (ItemRevision) itemRevs[itemRevs.length - 1];
            }
            loadProps(ds, itemRev);
            Field[] fields = bean.getClass().getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                ReflectionUtils.makeAccessible(fields[i]);
                TCPropName tcPropName = fields[i].getAnnotation(TCPropName.class);
                if (tcPropName != null) {
                    String tcAttrName = tcPropName.value();
                    if (StringUtil.isNotEmpty(tcAttrName)) {
                        Object value = "";
                        if (tcAttrName.startsWith("bl") && modelObject instanceof BOMLine) {
                            value = getTCPropertyString(modelObject, tcAttrName);
                            if (value == null || value.equals("")) {
                                String tcOtherAttrName = tcPropName.otherVal();
                                if (StringUtil.isNotEmpty(tcOtherAttrName)) {
                                    try {
                                        value = getTCPropertyString(modelObject, tcOtherAttrName);
                                    } catch (Exception exception) {
                                        log.info("getPropertyObject exception ::  " + tcOtherAttrName);
                                        exception.printStackTrace();
                                    }

                                }
                            }
                        } else {
                            value = getTCPropertyString(itemRev, tcAttrName);
                        }
                        if (fields[i].getType() == Integer.class) {
                            try {
                                value = Integer.parseInt((String) value);
                            } catch (Exception e) {
                                value = null;
                            }
                        }
                        fields[i].set(bean, value);
                    }
                }
            }
            bean.setUid(modelObject.getUid());
        }
        return bean;
    }

    public String getTCPropertyString(ModelObject modelObject, String propName) {
        String value = "";
        try {
            Property property = modelObject.getPropertyObject(propName);
            // boolean isArray = property.getPropertyDescription().isArray();
            if ("release_status_list".equalsIgnoreCase(propName)) {
                List<ModelObject> modelObjects = property.getModelObjectListValue();
                if (modelObjects.size() > 0) {
                    value = "released";
                }
            } else {
                value = property.getStringValue();
            }
        } catch (NotLoadedException e) {
            log.info(" getTCPropertyString exception ::  " + propName);
            e.printStackTrace();
        }
        return value;
    }

    public void setLocationByDesignBOM(EBOMLineBean rootBean, BOMLine bomLine) throws TCException, NotLoadedException {
        List<String> packedLocations = getBomAttrMergeByPacked(bomLine, "bl_occ_ref_designator");
        if (packedLocations != null) {
            packedLocations.add(rootBean.getLocation());
            packedLocations.sort(String::compareTo);
            rootBean.setLocation(String.join(",", packedLocations));
        }
    }

    public List<String> getBomAttrMergeByPacked(BOMLine bomLine, String arrtName) throws TCException, NotLoadedException {
        if (bomLine.get_bl_is_packed()) {
            ModelObject[] packedLines = bomLine.get_bl_packed_lines();
            if (packedLines.length > 0) {
                List<String> strList = new ArrayList<>();
                for (ModelObject pakcedLine : packedLines) {
                    String value = pakcedLine.getPropertyObject(arrtName).getStringValue();
                    if (value != null && value.length() > 0) {
                        strList.add(value);
                    }
                }
                if (strList.size() > 0) {
                    return strList;
                }
            }
        }
        return null;
    }

    private ModelObject[] getSubsLine(BOMLine bomLine, DataManagementService ds) {
        ModelObject[] modelObjects = new ModelObject[0];
        uidSeq_tHolder uidSeqTHolder = new uidSeq_tHolder();
        try {
            icctbomLine.listSubstitutes(bomLine.getUid(), uidSeqTHolder);
            String[] value = uidSeqTHolder.value;
            modelObjects = loadObject(ds, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return modelObjects;
    }


    public ModelObject[] loadObject(DataManagementService ds, String[] uids) throws IOException {
        ServiceData sd = ds.loadObjects(uids);
        if (sd.sizeOfPartialErrors() > 0) {
            throw new IOException(sd.getPartialError(0).toString());
        } else {
            ModelObject[] objArray = new ModelObject[sd.sizeOfPlainObjects()];
            for (int i = 0; i < sd.sizeOfPlainObjects(); i++) {
                objArray[i] = sd.getPlainObject(i);
            }
            return objArray;
        }
    }

    public void NoDifferenceNotice(TCSOAServiceFactory tcsoaServiceFactory, String taskUid) throws Exception {
        DataManagementService ds = tcsoaServiceFactory.getDataManagementService();
        ServiceData sd = ds.loadObjects(new String[]{taskUid});
        EPMTask task = (EPMTask) sd.getPlainObject(0);
        ds.getProperties(new ModelObject[]{task}, new String[]{"root_target_attachments"});
        ModelObject[] modelObjects = task.get_root_target_attachments();
        ds.getProperties(modelObjects, new String[]{"object_desc", "object_type"});
        ModelObject changeFile = null;
        ModelObject pcaBom = null;
        for (ModelObject attachmentObject : modelObjects) {
            if ("changeListExcel".equalsIgnoreCase(attachmentObject.getPropertyObject("object_desc").getStringValue())) {
                changeFile = attachmentObject;
            }
            if ("D9_PCA_PartRevision".equalsIgnoreCase(attachmentObject.getPropertyObject("object_type").getStringValue())) {
                pcaBom = attachmentObject;
                //d9_MaterialGroup  B8X80   //object_type D9_PCA_PartRevision
            }
            if (changeFile != null && pcaBom != null) {
                break;
            }
        }
        if (pcaBom != null) {
            ds.getProperties(new ModelObject[]{pcaBom}, new String[]{"D9_HasSourceBOM_REL", "item_id", "d9_ActualUserID", "projects_list",
                    "d9_DerivativeTypeDC",
                    "d9_FoxconnModelName"});
            List<ModelObject> soruceBOMList = pcaBom.getPropertyObject("D9_HasSourceBOM_REL").getModelObjectListValue();
            Map<WFTaskRealUser, String> map = getRealUser(ds, tcsoaServiceFactory.getFileManagementUtility(),
                    pcaBom, "2.1-EE/PI Leader Review");
            if (map != null) {
                File changeListFile = null;
                String fileName = "";
                String mailContent = "";
                String mailSubject = "";
                String realUserName = map.get(WFTaskRealUser.realUserName);
                String mailTo = map.get(WFTaskRealUser.realMail);
                String tcUserId = map.get(WFTaskRealUser.tcUserId);
                String deriveBOM = pcaBom.getPropertyObject("item_id").getStringValue();
                String actualUser = pcaBom.getPropertyObject("d9_ActualUserID").getStringValue();
                String projectName = pcaBom.getPropertyObject("projects_list").getStringValue();
                String modelName = pcaBom.getPropertyObject("d9_FoxconnModelName").getStringValue();
                String typeDC = pcaBom.getPropertyObject("d9_DerivativeTypeDC").getStringValue();
                if (StringUtil.isNotEmpty(typeDC)) {
                    modelName += "_" + typeDC;
                }
                if (soruceBOMList != null && soruceBOMList.size() > 0) {
                    ModelObject sourceBom = soruceBOMList.get(0);
                    ds.getProperties(new ModelObject[]{sourceBom}, new String[]{"item_id"});
                    String sourceBOM = sourceBom.getPropertyObject("item_id").getStringValue();
                    if (changeFile != null) {
                        File[] changeExcelFiles = DatasetUtil.getDataSetFiles(ds, (Dataset) changeFile,
                                tcsoaServiceFactory.getFileManagementUtility());
                        if (changeExcelFiles != null && changeExcelFiles.length > 0) {
                            changeListFile = changeExcelFiles[0];
                        }
                        ds.getProperties(new ModelObject[]{changeFile}, new String[]{"object_name"});
                        fileName = ((Dataset) changeFile).get_object_name();
                    }
                    mailContent = "<html> <body>" +
                            "Dear 『" + realUserName + "』: <br><br>" +
                            "&nbsp;&nbsp;&nbsp;請登錄『" + tcUserId + "』賬號審核專案『" + projectName + "』的機種『" + modelName + "』，" +
                            "機種編號為『" + deriveBOM + "』，該機種為源機種『" + sourceBOM + "』的衍生機種的任務." +
                            "<br><br>";
                    mailSubject = "請登錄『" + tcUserId + "』賬號審核由" +
                            "『" + actualUser + "』發起的“衍生機種『" + deriveBOM + "』審核任務”）";

                } else {
                    mailContent = "<html> <body>" +
                            "Dear 『" + realUserName + "』: <br><br>" +
                            "&nbsp;&nbsp;&nbsp;請登錄『" + tcUserId + "』賬號審核專案『" + projectName + "』的機種『" + modelName + "』，" +
                            "機種編號為『" + deriveBOM + "』，當前審核機種為源機種. <br><br>";
                    mailSubject = "請登錄『" + tcUserId + "』賬號審核由" +
                            "『" + actualUser + "』發起的（“源機種『" + deriveBOM + "』的審核任務”）";
                }
                mailContent += "<b>Teamcenter 系统自动发送,请勿回复邮件！</b><br><br></body></html>";
                String result = sendMail(mailTo, "", mailSubject, mailContent, changeListFile, fileName);
                log.info("NoDifferenceNotice :  taskUid=" + taskUid + "  邮件发送 success  : mailTo=" + mailTo + "   sendMail result=" + result);
            } else {
                log.info("NoDifferenceNotice :  " + taskUid + " 没有取到实际用户");
            }
        } else {
            log.info("NoDifferenceNotice :: " + taskUid + " 流程目标数据不对 ：   pcaBom= null");
        }
    }

    public List<String> getBOMPn(DataManagementService ds, ModelObject attachmentObject) throws NotLoadedException {
        List<String> pnList = new ArrayList<>();

        ds.getProperties(new ModelObject[]{attachmentObject}, new String[]{"object_name"});
        String fileName = attachmentObject.getPropertyObject("object_name").getStringValue();
        Pattern templatePattern = Pattern.compile("\\[(\\w+)\\]");
        Matcher matcher = templatePattern.matcher(fileName);
        while (matcher.find()) {
            String pn = matcher.group(1);
            pnList.add(pn);
        }
        return pnList;
    }

    public Set<User> getWfEPMTaskSingOffUsers(DataManagementService ds, EPMTask task, String taskName) throws NotLoadedException {
        Set<User> users = new HashSet<>();
        ds.getProperties(new ModelObject[]{task}, new String[]{"root_task"});
        ModelObject wfRoot = task.get_root_task();
        ds.getProperties(new ModelObject[]{wfRoot}, new String[]{"child_tasks"});
        ModelObject[] childTasks = wfRoot.getPropertyObject("child_tasks").getModelObjectArrayValue();
        String toMail = "";
        for (ModelObject childModel : childTasks) {
            System.out.println(" class name  -->>  " + childModel.getClass().getName());
            if (childModel instanceof EPMTask) {
                ds.getProperties(new ModelObject[]{childModel}, new String[]{"object_name"});
                if (taskName.equalsIgnoreCase(childModel.getPropertyObject("object_name").getStringValue())) {
                    ds.getProperties(new ModelObject[]{childModel}, new String[]{"awp0Reviewers"});
                    ModelObject[] signOff = childModel.getPropertyObject("awp0Reviewers").getModelObjectArrayValue();
                    ds.getProperties(signOff, new String[]{"user"});
                    for (ModelObject uModelObject : signOff) {
                        User signOffUser = (User) uModelObject.getPropertyObject("user").getModelObjectValue();
                        users.add(signOffUser);
                        //Person person = signOffUser.get_person();
                        //user.add(person);
                        //String mail = person.get_PA9();
                    }
                }
            }
        }
        return users;
    }


    public String sendMail(String mailTo, String mailCc, String subject, String content, File file, String fileName) {
        String result = "";
        Map<String, String> httpmap = new HashMap<>();
        httpmap.put("sendTo", mailTo);
        if (StringUtil.isNotEmpty(mailCc)) {
            httpmap.put("sendCc", mailCc);
        }
        httpmap.put("subject", subject);
        httpmap.put("htmlmsg", content);
        Gson gson = new Gson();
        String data = gson.toJson(httpmap);
        if (file != null) {
            MultipartFile multipartFile = new CommonsMultipartFile(createFileItem(file, fileName));
            result = tcMailClient.sendMail3Method(data, multipartFile);// 发送邮件
        } else {
            result = tcMailClient.sendMail3Method(data);// 发送邮件}
        }
        return result;
    }

    //把File转化为CommonsMultipartFile
    public FileItem createFileItem(File file, String fileName) {
        FileItemFactory factory = new DiskFileItemFactory(16, null);
        FileItem item = factory.createItem("file", "text/plain", true, fileName);
        int bytesRead = 0;
        byte[] buffer = new byte[8192];
        FileInputStream fis = null;
        OutputStream os = null;
        try {
            fis = new FileInputStream(file);
            os = item.getOutputStream();
            while ((bytesRead = fis.read(buffer, 0, 8192)) != -1) {
                os.write(buffer, 0, bytesRead);//从buffer中得到数据进行写操作
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            }catch (IOException e){}

            try {
                if (os != null) {
                   os.close();
                }
            }catch (IOException e){}

        }
        return item;
    }

    public Map<WFTaskRealUser, String> getRealUser(DataManagementService dataManagementService,
                                                   FileManagementUtility fileManagementUtility, ModelObject targetObject, String taskName) throws NotLoadedException {
        ActualUserPojo actualUserPojo = ActualUserUtil.getActualUserByProcessNode(dataManagementService, targetObject, taskName);
        if(ObjectUtil.isNotNull(actualUserPojo)){
            Map<WFTaskRealUser, String> realMap = new HashMap<>();
            realMap.put(WFTaskRealUser.tcUserId, actualUserPojo.getActualUserId());
            realMap.put(WFTaskRealUser.realMail, actualUserPojo.getActualUserMail());
            realMap.put(WFTaskRealUser.realUserName, actualUserPojo.getActualUserName());
            return realMap;
        }
        /*TCUtils.getProperty(dataManagementService, targetObject, "IMAN_external_object_link");
        ModelObject[] modelObject = targetObject.getPropertyObject("IMAN_external_object_link").getModelObjectArrayValue();
        Dataset dataset = (Dataset) modelObject[0];
        dataManagementService.refreshObjects(new ModelObject[]{dataset});
        dataManagementService.getProperties(new ModelObject[]{dataset}, new String[]{"ref_list"});
        ModelObject[] dsfiles = dataset.get_ref_list();
        for (int i = 0; i < dsfiles.length; i++) {
            if (!(dsfiles[i] instanceof ImanFile)) {
                continue;
            }
            ImanFile dsFile = (ImanFile) dsfiles[i];
            dataManagementService.refreshObjects(new ModelObject[]{dsFile});
            dataManagementService.getProperties(new ModelObject[]{dsFile},
                    new String[]{"original_file_name"});
            GetFileResponse responseFiles = fileManagementUtility.getFiles(new ModelObject[]{dsFile});
            File[] files = responseFiles.getFiles();
            File file = files[0];
            List<String> lines = FileUtil.readLines(file, "GBK");
            for (String line : lines) {
                System.out.println(line);
                String[] strs1 = line.split("=");
                if (taskName.equalsIgnoreCase(strs1[0]) && strs1[1] != null) {
                    String[] strs2 = strs1[1].split(";");
                    if (strs2.length > 0) {
                        String str = strs2[strs2.length - 1];
                        int s1 = str.indexOf("##");
                        int s2 = str.indexOf("%%");
                        if (s1 > 0 && s2 > 0) {
                            Map<WFTaskRealUser, String> realMap = new HashMap<>();
                            String tcUserId = str.substring(0, s1);
                            String realUserName = str.substring(s1 + 2, s2);
                            String realMail = str.substring(s2 + 2);
                            realMap.put(WFTaskRealUser.tcUserId, tcUserId);
                            realMap.put(WFTaskRealUser.realMail, realMail);
                            realMap.put(WFTaskRealUser.realUserName, realUserName);
                            return realMap;
                        }
                    }
                }
            }
        }*/
        return null;
    }


    public static String matcherStr(String str, String startStr, String endStr) {
        //Pattern templatePattern = Pattern.compile("\\[(\\w+)\\]");
        Pattern templatePattern = Pattern.compile(startStr + "(\\w+)" + endStr);
        Matcher matcher = templatePattern.matcher(str);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
