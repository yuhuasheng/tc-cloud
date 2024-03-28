package com.foxconn.plm.tcservice.issuemanagement.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.foxconn.dp.plm.privately.PrivaFileUtis;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcservice.issuemanagement.param.AddDellIssueParam;
import com.foxconn.plm.tcservice.issuemanagement.param.AddHpIssueParam;
import com.foxconn.plm.tcservice.issuemanagement.param.AddIssueUpdatesParam;
import com.foxconn.plm.tcservice.issuemanagement.param.AddLenovoIssueParam;
import com.foxconn.plm.tcservice.issuemanagement.service.CreateIssueService;
import com.foxconn.plm.utils.tc.*;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core._2006_03.DataManagement;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.Property;
import com.teamcenter.soa.client.model.strong.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 創建Issue接口實現類
 *
 * @Description
 * @Author MW00442
 * @Date 2024/2/19 16:39
 **/
@Service
public class CreateIssueServiceImpl implements CreateIssueService {
    @Resource
    private TCSOAServiceFactory tcsoaServiceFactory;

    @Override
    public R createDellIssue(AddDellIssueParam param, List<MultipartFile> files) {
        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS4);
        try{
            Map<String,List<String>> props = new HashMap<>();
            if(StrUtil.isNotBlank(param.getActualUser())){
                props.put("d9_ActualUserID",CollUtil.newArrayList(param.getActualUser()));
            }
            if(StrUtil.isNotBlank(param.getProject())){
                props.put("d9_IRProject",CollUtil.newArrayList(param.getProject()));
            }
            if(StrUtil.isNotBlank(param.getIssueType())){
                props.put("d9_IRIssueType",CollUtil.newArrayList(param.getIssueType()));
            }
            if(StrUtil.isNotBlank(param.getOriginVendor())){
                props.put("d9_IROriginatingVendor",CollUtil.newArrayList(param.getOriginVendor()));
            }
            if(StrUtil.isNotBlank(param.getOriginGroup())){
                props.put("d9_IROriginatingGroup",CollUtil.newArrayList(param.getOriginGroup()));
            }
            if(StrUtil.isNotBlank(param.getLobFound())){
                List<String> split = StrSplitter.split(param.getLobFound(), ",",true,true);
                props.put("d9_IRLOBFound",split);
            }
            if(StrUtil.isNotBlank(param.getPlatformFound())){
                List<String> split = StrSplitter.split(param.getPlatformFound(), ",",true,true);
                props.put("d9_IRPlatformFoundDell",split);
            }
            if(StrUtil.isNotBlank(param.getComponent())){
                props.put("d9_IRCategory",CollUtil.newArrayList(param.getComponent()));
            }
            if(StrUtil.isNotBlank(param.getGroupActivity())){
                props.put("d9_IRGroupActivity",CollUtil.newArrayList(param.getGroupActivity()));
            }
            if(StrUtil.isNotBlank(param.getGroupLocation())){
                props.put("d9_IRGroupLocation",CollUtil.newArrayList(param.getGroupLocation()));
            }
            if(StrUtil.isNotBlank(param.getPhaseFound())){
                props.put("d9_IRPhaseFoundDell",CollUtil.newArrayList(param.getPhaseFound()));
            }
            if(StrUtil.isNotBlank(param.getHardwareBuildVersion())){
                props.put("d9_IRHardwareBuildVersion",CollUtil.newArrayList(param.getHardwareBuildVersion()));
            }
            if(StrUtil.isNotBlank(param.getDiscoveryMethod())){
                props.put("d9_IRDiscoveryMethod",CollUtil.newArrayList(param.getDiscoveryMethod()));
            }
            if(StrUtil.isNotBlank(param.getTestCaseNumber())){
                props.put("d9_IRTestCaseNumberRequired",CollUtil.newArrayList(param.getTestCaseNumber()));
            }
            if(StrUtil.isNotBlank(param.getPlatformIndependent())){
                props.put("d9_IRPlatformIndependent",CollUtil.newArrayList(param.getPlatformIndependent()));
            }
            if(StrUtil.isNotBlank(param.getDiscretionaryLabels())){
                props.put("d9_IRDiscretionaryLabels",CollUtil.newArrayList(param.getDiscretionaryLabels()));
            }
            if(StrUtil.isNotBlank(param.getClassify())){
                props.put("d9_IRCommodity",CollUtil.newArrayList(param.getClassify()));
            }
            if(StrUtil.isNotBlank(param.getSubClassify())){
                props.put("d9_IRComponent",CollUtil.newArrayList(param.getSubClassify()));
            }
            if(StrUtil.isNotBlank(param.getProductImpact())){
                props.put("d9_IRProductImpact",CollUtil.newArrayList(param.getProductImpact()));
            }
            if(StrUtil.isNotBlank(param.getCustomerImpact())){
                props.put("d9_IRCustomerImpactDell",CollUtil.newArrayList(param.getCustomerImpact()));
            }
            if(StrUtil.isNotBlank(param.getLikelihood())){
                props.put("d9_IRLikelihood",CollUtil.newArrayList(param.getLikelihood()));
            }
            if(StrUtil.isNotBlank(param.getRpn())){
                props.put("d9_IRRPN",CollUtil.newArrayList(param.getRpn()));
            }
            if(StrUtil.isNotBlank(param.getIssueSeverity())){
                props.put("d9_IRSeverity",CollUtil.newArrayList(param.getIssueSeverity()));
            }
            if(StrUtil.isNotBlank(param.getAffectedOs())){
                List<String> split = StrSplitter.split(param.getAffectedOs(), ",",true,true);
                props.put("d9_IRAffectedOS",split);
            }
            if(StrUtil.isNotBlank(param.getAffectedLanguages())){
                List<String> split = StrSplitter.split(param.getAffectedLanguages(), ",",true,true);
                props.put("d9_IRAffectedLanguages",split);
            }
            if(StrUtil.isNotBlank(param.getAffectedItems())){
                List<String> split = StrSplitter.split(param.getAffectedItems(), ",",true,true);
                props.put("d9_IRAffectedItemsDell",split);
            }
            if(StrUtil.isNotBlank(param.getPartsForProjectAffect())){
                props.put("d9_IRPartsForProjectAffect",CollUtil.newArrayList(param.getPartsForProjectAffect()));
            }
            if(StrUtil.isNotBlank(param.getSummary())){
                props.put("d9_IRName",CollUtil.newArrayList(param.getSummary()));
            }
            if(StrUtil.isNotBlank(param.getDescription())){
                props.put("d9_IRLongDescription",CollUtil.newArrayList(param.getDescription()));
            }
            if(StrUtil.isNotBlank(param.getStepsToReproduce())){
                props.put("d9_IRStepsToReproduce",CollUtil.newArrayList(param.getStepsToReproduce()));
            }
            String itemUid = createIssue(tcsoaServiceFactory.getDataManagementService(), "D9_IR_DELL", props,
                    param.getUserUid(), param.getGroupUid(), files, param.getTcProject());
            return R.success(HttpResultEnum.SUCCESS.getMsg(),itemUid);
        }catch (Exception e){
            return R.error(HttpResultEnum.NO_RESULT.getCode(),"創建issue失敗");
        }finally {
            tcsoaServiceFactory.logout();
        }
    }

    @Override
    public R createHpIssue(AddHpIssueParam param, List<MultipartFile> files) {
        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS4);
        try{
            Map<String,List<String>> props = new HashMap<>();
            if(StrUtil.isNotBlank(param.getActualUser())){
                props.put("d9_ActualUserID",CollUtil.newArrayList(param.getActualUser()));
            }
            if(StrUtil.isNotBlank(param.getState())){
                props.put("d9_IRState",CollUtil.newArrayList(param.getState()));
            }
            if(StrUtil.isNotBlank(param.getStatus())){
                props.put("d9_IRStatus",CollUtil.newArrayList(param.getStatus()));
            }
            if(StrUtil.isNotBlank(param.getPriority())){
                props.put("d9_IRPriority",CollUtil.newArrayList(param.getPriority()));
            }
            if(StrUtil.isNotBlank(param.getDivision())){
                props.put("d9_IRDivision",CollUtil.newArrayList(param.getDivision()));
            }
            if(StrUtil.isNotBlank(param.getOriginatorWorkgroup())){
                props.put("d9_IROriginatorWorkgroup",CollUtil.newArrayList(param.getOriginatorWorkgroup()));
            }
            if(StrUtil.isNotBlank(param.getPrimaryProduct())){
                props.put("d9_IRPlatformFound",CollUtil.newArrayList(param.getPrimaryProduct()));
            }
            if(StrUtil.isNotBlank(param.getProductVersion())){
                props.put("d9_IRProductVersion",CollUtil.newArrayList(param.getProductVersion()));
            }
            if(StrUtil.isNotBlank(param.getProductLine())){
                props.put("d9_IRProductionLine",CollUtil.newArrayList(param.getProductLine()));
            }
            if(StrUtil.isNotBlank(param.getComponentType())){
                props.put("d9_IRIssueType",CollUtil.newArrayList(param.getComponentType()));
            }
            if(StrUtil.isNotBlank(param.getComponentSubSystem())){
                props.put("d9_IRCategory",CollUtil.newArrayList(param.getComponentSubSystem()));
            }
            if(StrUtil.isNotBlank(param.getComponent())){
                props.put("d9_IRCommodity",CollUtil.newArrayList(param.getComponent()));
            }
            if(StrUtil.isNotBlank(param.getComponentVersion())){
                props.put("d9_IRComponentVersion",CollUtil.newArrayList(param.getComponentVersion()));
            }
            if(StrUtil.isNotBlank(param.getComponentLocalization())){
                props.put("d9_IRComponentLocalization",CollUtil.newArrayList(param.getComponentLocalization()));
            }
            if(StrUtil.isNotBlank(param.getComponentPartNumber())){
                props.put("d9_IRComponentPartNumber",CollUtil.newArrayList(param.getComponentPartNumber()));
            }
            if(StrUtil.isNotBlank(param.getFrequency())){
                props.put("d9_IRFrequency",CollUtil.newArrayList(param.getFrequency()));
            }
            if(StrUtil.isNotBlank(param.getGatingMilestone())){
                props.put("d9_IRGatingMilestone",CollUtil.newArrayList(param.getGatingMilestone()));
            }
            if(StrUtil.isNotBlank(param.getTestEscape())){
                props.put("d9_IRTestEscape",CollUtil.newArrayList(param.getTestEscape()));
            }
            if(StrUtil.isNotBlank(param.getSeverity())){
                props.put("d9_IRSeverity",CollUtil.newArrayList(param.getSeverity()));
            }
            if(StrUtil.isNotBlank(param.getImpacts())){
                props.put("d9_IRImpacts",CollUtil.newArrayList(param.getImpacts()));
            }
            if(StrUtil.isNotBlank(param.getShortDesc())){
                props.put("d9_IRName",CollUtil.newArrayList(param.getShortDesc()));
            }
            if(StrUtil.isNotBlank(param.getLongDesc())){
                props.put("d9_IRLongDescription",CollUtil.newArrayList(param.getLongDesc()));
            }
            if(StrUtil.isNotBlank(param.getStepsToReproduce())){
                props.put("d9_IRStepsToReproduce",CollUtil.newArrayList(param.getStepsToReproduce()));
            }
            if(StrUtil.isNotBlank(param.getCustomerImpact())){
                props.put("d9_IRCustomerImpact",CollUtil.newArrayList(param.getCustomerImpact()));
            }
            String itemUid = createIssue(tcsoaServiceFactory.getDataManagementService(), "D9_IR_HP", props,
                    param.getUserUid(), param.getGroupUid(), files, param.getTcProject());
            return R.success(HttpResultEnum.SUCCESS.getMsg(),itemUid);
        }catch (Exception e){
            e.printStackTrace();
            return R.error(HttpResultEnum.NO_RESULT.getCode(),"創建issue失敗");
        }finally {
            tcsoaServiceFactory.logout();
        }
    }

    @Override
    public R createLenovoIssue(AddLenovoIssueParam param, List<MultipartFile> files) {
        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS4);
        try{
            Map<String,List<String>> props = new HashMap<>();
            if(StrUtil.isNotBlank(param.getActualUser())){
                props.put("d9_ActualUserID",CollUtil.newArrayList(param.getActualUser()));
            }
            if(StrUtil.isNotBlank(param.getName())){
                props.put("d9_IRName",CollUtil.newArrayList(param.getName()));
            }
            if(StrUtil.isNotBlank(param.getRequestPriority())){
                props.put("d9_IRSeverity",CollUtil.newArrayList(param.getRequestPriority()));
            }
            if(StrUtil.isNotBlank(param.getRelease())){
                props.put("d9_IRPlatformFound",CollUtil.newArrayList(param.getRelease()));
            }
            if(StrUtil.isNotBlank(param.getProductionLine())){
                props.put("d9_IRProductionLine",CollUtil.newArrayList(param.getProductionLine()));
            }
            if(StrUtil.isNotBlank(param.getComponent())){
                props.put("d9_IRCommodity",CollUtil.newArrayList(param.getComponent()));
            }
            if(StrUtil.isNotBlank(param.getDescription())){
                props.put("d9_IRLongDescription",CollUtil.newArrayList(param.getDescription()));
            }
            if(StrUtil.isNotBlank(param.getReleaseOther())){
                props.put("d9_IRSimilarIssue",CollUtil.newArrayList(param.getReleaseOther()));
            }
            if(StrUtil.isNotBlank(param.getOperationSys())){
                props.put("d9_IROperatingSystem",CollUtil.newArrayList(param.getOperationSys()));
            }
            if(StrUtil.isNotBlank(param.getOperationSysOther())){
                List<String> split = StrSplitter.split(param.getOperationSysOther(), ",",true,true);
                props.put("d9_IROperatingSystemOther",split);
            }
            if(StrUtil.isNotBlank(param.getPhaseFound())){
                props.put("d9_IRPhaseFound",CollUtil.newArrayList(param.getPhaseFound()));
            }
            if(StrUtil.isNotBlank(param.getReproduceSteps())){
                props.put("d9_IRStepsToReproduce",CollUtil.newArrayList(param.getReproduceSteps()));
            }
            if(StrUtil.isNotBlank(param.getDefectConsistency())){
                props.put("d9_IRFrequency",CollUtil.newArrayList(param.getDefectConsistency()));
            }
            if(StrUtil.isNotBlank(param.getAffectedSystem())){
                props.put("d9_IRAffectedSystem",CollUtil.newArrayList(param.getAffectedSystem()));
            }
            if(StrUtil.isNotBlank(param.getLimitation())){
                props.put("d9_IRLimitation",CollUtil.newArrayList(param.getLimitation()));
            }
            if(StrUtil.isNotBlank(param.getBrand())){
                props.put("d9_IRBrand",CollUtil.newArrayList(param.getBrand()));
            }
            if(StrUtil.isNotBlank(param.getCloseDate())){
                props.put("d9_IRCloseDate",CollUtil.newArrayList(param.getCloseDate()));
            }
            if(StrUtil.isNotBlank(param.getAnswerCode())){
                props.put("d9_IRIssueType",CollUtil.newArrayList(param.getAnswerCode()));
            }
            if(StrUtil.isNotBlank(param.getRemark())){
                props.put("d9_IRComments",CollUtil.newArrayList(param.getRemark()));
            }
            if(StrUtil.isNotBlank(param.getConfiguration())){
                props.put("d9_IRConfiguration",CollUtil.newArrayList(param.getConfiguration()));
            }
            String itemUid = createIssue(tcsoaServiceFactory.getDataManagementService(), "D9_IR_LENOVO", props,
                    param.getUserUid(), param.getGroupUid(), files, param.getTcProject());
            return R.success(HttpResultEnum.SUCCESS.getMsg(),itemUid);
        }catch (Exception e){
            return R.error(HttpResultEnum.NO_RESULT.getCode(),"創建issue失敗");
        }finally {
            tcsoaServiceFactory.logout();
        }
    }

    @Override
    public R addIssueUpdates(AddIssueUpdatesParam param, List<MultipartFile> files) {
        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS4);
        try{
            DataManagementService dataManagementService = tcsoaServiceFactory.getDataManagementService();
            ModelObject itemRev = DataManagementUtil.findObjectByUid(dataManagementService, param.getItemRevUid());
            Map<String,List<String>> props = new HashMap<>();
            if(StrUtil.isNotBlank(param.getState())){
                props.put("d9_IRState",CollUtil.newArrayList(param.getState()));
            }
            if(StrUtil.isNotBlank(param.getTaskActualUser())){
                props.put("d9_IRCurrentTaskActualUser",CollUtil.newArrayList(param.getTaskActualUser()));
            }
            if(StrUtil.isNotBlank(param.getTaskOwner())){
                props.put("d9_IRCurrentTaskOwner",CollUtil.newArrayList(param.getTaskOwner()));
            }
            DataManagementUtil.setProperties(dataManagementService,itemRev,props);
            Map<String,String> map = new HashMap<>();
            map.put("d9_AssignedTo",param.getTaskActualUser());
            map.put("d9_CreatedBy",param.getActualUser());
            map.put("d9_Date", DateUtil.format(DateUtil.date(),"yyyy-MM-dd HH:mm:ss"));
            map.put("d9_State",param.getState());
            map.put("d9_Response",param.getResponse());
            ModelObject rowModel = DataManagementUtil.createTableRow(dataManagementService, "D9_IRUpdateHistoryTableRow", map);
            DataManagementUtil.addRelation(dataManagementService,itemRev,rowModel,"d9_IRUpdateHistoryTable");
            if(CollUtil.isNotEmpty(files)) {
                // 修改所屬權
                User user = (User) DataManagementUtil.findObjectByUid(dataManagementService,param.getUserUid());
                Group group = (Group) DataManagementUtil.findObjectByUid(dataManagementService, param.getGroupUid());
                String tmpdir = PrivaFileUtis.getTmpdir();
                for (MultipartFile file : files) {
                    String fileName = file.getOriginalFilename();
                    String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
                    List<String> typeList = DatasetUtil.getFileType(extension);
                    String path = tmpdir + fileName;
                    File newFile = FileUtil.newFile(path);
                    file.transferTo(newFile);
                    Dataset dataset = DatasetUtil.uploadDataset(dataManagementService, tcsoaServiceFactory.getFileManagementUtility(),(ItemRevision) itemRev, path, typeList.get(1),
                            fileName.substring(0, fileName.lastIndexOf(".")), typeList.get(0), "CMReferences");
                    DataManagementUtil.setProperties(dataManagementService,dataset,"d9_RealAuthor",param.getActualUser());
                    DataManagementUtil.changeOwner(dataManagementService, dataset, user, group);
                    FileUtil.del(newFile);
                }
            }
            if(ObjectUtil.isNotNull(param.getMail()) && param.getMail()){
                // 發送郵件，查詢該issue所有指定的數據並發送郵件
            }
            return R.success(Boolean.TRUE);
        }catch (Exception e){
            e.printStackTrace();
            return R.error(HttpResultEnum.NO_RESULT.getCode(),"新增Updates失敗");
        }finally {
            tcsoaServiceFactory.logout();
        }
    }

    private String createIssue(DataManagementService dataManagementService, String issueType, Map<String,List<String>> props,
                               String userUid, String groupUid, List<MultipartFile> files, String projectId) throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("object_type",issueType);
        map.put("item_revision_id","A");
        DataManagement.CreateItemsResponse response = ItemUtil.createItems(dataManagementService, CollUtil.newArrayList(map), null);
        DataManagement.CreateItemsOutput[] outputs = response.output;
        if(ObjectUtil.isNull(outputs) || outputs.length == 0){
            return "";
        }
        Item item = outputs[0].item;
        ItemRevision itemRev = outputs[0].itemRev;

        DataManagementUtil.setProperties(dataManagementService,itemRev,props);
        // 修改所屬權
        User user = (User) DataManagementUtil.findObjectByUid(dataManagementService, userUid);
        Group group = (Group) DataManagementUtil.findObjectByUid(dataManagementService, groupUid);
        DataManagementUtil.changeOwner(dataManagementService,item,user,group);
        DataManagementUtil.changeOwner(dataManagementService,itemRev,user,group);
        // 上傳附件
        String tmpdir = PrivaFileUtis.getTmpdir();
        if(CollUtil.isNotEmpty(files)) {
            for (MultipartFile file : files) {
                String fileName = file.getOriginalFilename();
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
                List<String> typeList = DatasetUtil.getFileType(extension);
                String path = tmpdir + fileName;
                File newFile = FileUtil.newFile(path);
                file.transferTo(newFile);
                Dataset dataset = DatasetUtil.uploadDataset(dataManagementService, tcsoaServiceFactory.getFileManagementUtility(), itemRev, path, typeList.get(1),
                        fileName.substring(0, fileName.lastIndexOf(".")), typeList.get(0), "CMReferences");
                DataManagementUtil.setProperties(dataManagementService,dataset,"d9_RealAuthor",props.get("d9_ActualUserID").get(0));
                DataManagementUtil.changeOwner(dataManagementService, dataset, user, group);
                FileUtil.del(newFile);
            }
        }
        // 掛載
        contentToProject(projectId,item);
        return item.getUid();
    }

    private void contentToProject(String projectId, Item item) throws Exception {
        // 查詢專案文件夾
        Map<String, Object> resultMap = QueryUtil.executeQuery(tcsoaServiceFactory.getSavedQueryService(), "__D9_Find_Project_Folder", new String[]{"d9_SPAS_ID"},
                new String[]{projectId});
        if (ObjectUtil.isNotNull(resultMap.get("succeeded"))) {
            ModelObject[] md = (ModelObject[]) resultMap.get("succeeded");
            if (md != null && md.length > 0) {
                // 獲取專案文件夾
                Folder folder = (Folder) md[0];
                Folder folder1 = DataManagementUtil.getFolder(tcsoaServiceFactory.getDataManagementService(), folder, "D9_WorkAreaFolder", "產品設計協同工作區");
                if(ObjectUtil.isNotNull(folder1)){
                    Folder folder2 = DataManagementUtil.getFolder(tcsoaServiceFactory.getDataManagementService(), folder1, "D9_WorkAreaFolder", "Issue協同工作區");
                    if(ObjectUtil.isNull(folder2)){
                        folder2 = FolderUtil.createFolder(tcsoaServiceFactory.getDataManagementService(),folder1,"D9_WorkAreaFolder", "Issue協同工作區",null);
                    }
                    TCUtils.addContents(tcsoaServiceFactory.getDataManagementService(), folder2, item);
                }
            }
        }
    }
}
