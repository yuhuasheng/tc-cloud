package com.foxconn.plm.spas.service.impl;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.constants.BUConstant;
import com.foxconn.plm.entity.constants.TCFolderConstant;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.param.BUListRp;
import com.foxconn.plm.entity.response.BURv;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.feign.service.HDFSClient;
import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.spas.bean.*;
import com.foxconn.plm.spas.config.properties.SpasPropertiesConfig;
import com.foxconn.plm.spas.mapper.SynSpasDBMapper;
import com.foxconn.plm.spas.service.SynSpasDBService;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.FolderUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Folder;
import com.teamcenter.soa.client.model.strong.WorkspaceObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;

@Service("synSpasDBServiceImpl")
public class SynSpasDBServiceImpl implements SynSpasDBService {
    private static Log log = LogFactory.get();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private static final SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat sdf2 = new SimpleDateFormat("HH");
    private static final SimpleDateFormat sdf3 = new SimpleDateFormat("yyyyMMddHHmm");
    private static String startDate;
    private static String endDate;
    @Autowired(required = false)
    private ManpowerServiceImpl manpowerServiceImpl;

    public static  void updateDate() {
        Integer h= Integer.parseInt(sdf2.format(new Date()));
        Calendar c1 = Calendar.getInstance();
        if(h.intValue()<4){
            c1.add(Calendar.DAY_OF_MONTH, -1);
        }else {
            c1.add(Calendar.DAY_OF_MONTH, 0);
        }
        startDate = dateFormat.format(c1.getTime());
        Calendar c2 = Calendar.getInstance();
        c2.add(Calendar.DAY_OF_MONTH, 1);
        endDate = dateFormat.format(c2.getTime());
    }

    @Value("${spring.cloud.nacos.discovery.namespace}")
    private String env;

    @Value("${mail.to}")
    private String mailTo;

    @Value("${mail.cc}")
    private String mailCC;


    @Resource
    private SpasPropertiesConfig spasPropertiesConfig;

    @Resource
    private SynSpasDBMapper synSpasDBMapper;

    @Resource
    private HDFSClient hdfsClient;



    @Resource
    TcMailClient tcMail;

    @Override
    public void addUserRoleData() throws Exception {
        String url = spasPropertiesConfig.getUrl()
                + "/user-server/api/user/tc/userRoleList?startDate="
                + startDate + "&endDate=" + endDate;
        List<SpasUserRole> userRoleData = getSpasData(url, SpasUserRole.class);
        for (SpasUserRole userRole : userRoleData) {
            synSpasDBMapper.deleteUserRole(userRole);
            synSpasDBMapper.saveUserRole(userRole);
        }
    }

    @Override
    public void addUserData() throws Exception {
        String url = "https://spas.efoxconn.com/user-server/api/user/tc/userList?startDate=2019-01-01&endDate=" + endDate;
        List<SpasUser> userData = getSpasDataprd(url, SpasUser.class);
        for (SpasUser user : userData) {
            String notes=user.getNotes();
            if(notes==null){
                notes="";
            }
            user.setNotes(notes.trim());
            synSpasDBMapper.deleteUser(user);
            synSpasDBMapper.saveUser(user);
        }
    }

    @Override
    public void addRoleData() throws Exception {
        String url = spasPropertiesConfig.getUrl()
                + "/user-server/api/user/tc/roleList?startDate="
                + startDate + "&endDate=" + endDate;
        List<SpasRole> roleData = getSpasData(url, SpasRole.class);
        for (SpasRole role : roleData) {
            synSpasDBMapper.deleteRole(role);
            synSpasDBMapper.saveRole(role);
        }
    }

    @Override
    public void addOrganizationData() throws Exception {
        String url = spasPropertiesConfig.getUrl()
                + "/user-server/api/user/tc/organizationList?startDate="
                + startDate + "&endDate=" + endDate;
        List<SpasOrganization> organizationData = getSpasData(url, SpasOrganization.class);
        for (SpasOrganization organization : organizationData) {
            synSpasDBMapper.deleteOrganization(organization);
            synSpasDBMapper.saveOrganization(organization);
        }
    }

    @Override
    public void addDeptGroupData() throws Exception {
        String url = spasPropertiesConfig.getUrl()
                + "/user-server/api/user/tc/deptGroupList?startDate="
                + startDate + "&endDate=" + endDate;
        List<SpasDeptGroup> deptGroupData = getSpasData(url, SpasDeptGroup.class);
        for (SpasDeptGroup deptGroup : deptGroupData) {
            synSpasDBMapper.deleteDeptGroup(deptGroup);
            synSpasDBMapper.saveDeptGroup(deptGroup);
        }
    }

    @Override
    public void addProjectSeriesData() throws Exception {
        String url = spasPropertiesConfig.getUrl()
                + "/project-server/api/project/tc/projectSeriesList?startDate="
                + startDate + "&endDate=" + endDate;
        List<SpasProjectSeries> projectSeriesData = getSpasData(url, SpasProjectSeries.class);
        for (SpasProjectSeries projectSeries : projectSeriesData) {
            synSpasDBMapper.deleteProjectSeries(projectSeries);
            synSpasDBMapper.saveProjectSeries(projectSeries);
        }
    }

    @Override
    public void addProjectScheduleData() throws Exception {

        String url = spasPropertiesConfig.getUrl()
                + "/project-server/api/project/tc/projectScheduleList?startDate="
                + startDate + "&endDate=" + endDate;
        List<SpasProjectSchedule> projectScheduleData = getSpasData(url, SpasProjectSchedule.class);
        for (SpasProjectSchedule projectSchedule : projectScheduleData) {
            synSpasDBMapper.deleteProjectSchedule(projectSchedule);
            synSpasDBMapper.saveProjectSchedule(projectSchedule);
        }
    }

    @Override
    public void addProjectPersonData() throws Exception {
        try {
            String url = spasPropertiesConfig.getUrl()
                    + "/project-server/api/project/tc/projectPersonList";
            List<SpasProjectPerson> projectPersonData = getSpasData(url, SpasProjectPerson.class);
            for (SpasProjectPerson projectPerson : projectPersonData) {
                    synSpasDBMapper.deleteProjectPerson(projectPerson);
                    synSpasDBMapper.saveProjectPerson(projectPerson);
            }
        }catch(Exception e){
            log.error(e.getMessage(),e);
            throw e;
        }
    }

    @Override
    public void addProjectAttributeData() throws Exception {
        String url = spasPropertiesConfig.getUrl()
                + "/project-server/api/project/tc/projectAttributeList";
        List<SpasProjectAttribute> projectAttributeData = getSpasData(url, SpasProjectAttribute.class);
        for (SpasProjectAttribute projectAttribute : projectAttributeData) {
            synSpasDBMapper.deleteProjectAttribute(projectAttribute);
            synSpasDBMapper.saveProjectAttribute(projectAttribute);
        }
    }

    @Override
    public void addProductLinePhaseData() throws Exception {
        String url = spasPropertiesConfig.getUrl()
                + "/project-server/api/project/tc/productLinePhaseList?startDate="
                + startDate + "&endDate=" + endDate;
        List<SpasProductLinePhase> productLinePhaseData = getSpasData(url, SpasProductLinePhase.class);
        for (SpasProductLinePhase productLinePhase : productLinePhaseData) {
            log.info("id:" + productLinePhase.getId()
            + "，productLineId：" + productLinePhase.getProductLineId()
            + "，phaseSn：" + productLinePhase.getPhaseSn()
            + "，name：" + productLinePhase.getName()
            + "，isActive：" + productLinePhase.getIsActive()
            + "，businessStageId：" + productLinePhase.getBusinessStageId()
            + "，updator：" + productLinePhase.getUpdator()
            + "，updateTime：" + productLinePhase.getUpdateTime()
            + "，creator：" + productLinePhase.getCreator()
            + "，createTime：" + productLinePhase.getCreateTime());
            synSpasDBMapper.deleteProductLinePhase(productLinePhase);
            synSpasDBMapper.saveProductLinePhase(productLinePhase);
        }
    }

    @Override
    public void addProductLineData() throws Exception {
        String url = spasPropertiesConfig.getUrl()
                + "/project-server/api/project/tc/productLineList?startDate="
                + startDate + "&endDate=" + endDate;
        List<SpasProductLine> productLineData = getSpasData(url, SpasProductLine.class);
        for (int i = 0; i < productLineData.size(); i++) {
            SpasProductLine spasProductLine = productLineData.get(i);
            log.info("productLineId：" + spasProductLine.getId() + "，productLineName：" + spasProductLine.getName());
        }
        for (SpasProductLine productLine : productLineData) {
            synSpasDBMapper.deleteProductLine(productLine);
            synSpasDBMapper.saveProductLine(productLine);
        }
    }

    @Override
    public void addPlatformFoundData() throws Exception {
        String url = spasPropertiesConfig.getUrl()
                + "/project-server/api/project/tc/platformFoundList?startDate="
                + startDate + "&endDate=" + endDate;
        List<SpasPlatformFound> platformFoundData = getSpasData(url, SpasPlatformFound.class);
        for (SpasPlatformFound platformFound : platformFoundData) {
            log.info("id:" + platformFound.getId()
                    + "，seriesId：" + platformFound.getSeriesId()
                    + "，name：" + platformFound.getName()
                    + "，status：" + platformFound.getStatus()
                    + "，curPhaseId：" + platformFound.getCurPhaseId()
                    + "，closeCause：" + platformFound.getCloseCause()
                    + "，type：" + platformFound.getType()
                    + "，mainContact：" + platformFound.getMainContact()
                    + "，isActive：" + platformFound.getIsActive()
                    + "，owner：" + platformFound.getOwner()
                    + "，startTime：" + platformFound.getStartTime()
                    + "，updateTime：" + platformFound.getUpdateTime()
                    + "，creator：" + platformFound.getCreator()
                    + "，createTime：" + platformFound.getCreateTime()
                    + "，process：" + platformFound.getProcess());
            synSpasDBMapper.deletePlatformFound(platformFound);
            synSpasDBMapper.savePlatformFound(platformFound);
        }
    }

    @Override
    public void addCustomerData() throws Exception {
        String url = spasPropertiesConfig.getUrl()
                + "/project-server/api/project/tc/customerList?startDate="
                + startDate + "&endDate=" + endDate;
        List<SpasCustomer> customerData = getSpasData(url, SpasCustomer.class);
        for (int i = 0; i < customerData.size(); i++) {
            SpasCustomer spasCustomer = customerData.get(i);
            log.info("customerId：" + spasCustomer.getId() + "，customerName：" + spasCustomer);
        }
        for (SpasCustomer customer : customerData) {
            synSpasDBMapper.deleteCustomer(customer);
            synSpasDBMapper.saveCustomer(customer);
        }
    }

    @Override
    public void addCusAttributeCategoryData() throws Exception {
        String url = spasPropertiesConfig.getUrl()
                + "/project-server/api/project/tc/attributeCategoryList?startDate="
                + startDate + "&endDate=" + endDate;
        List<SpasCusAttributeCategory> cusAttributeCategoryData = getSpasData(url, SpasCusAttributeCategory.class);
        for (SpasCusAttributeCategory cusAttributeCategory : cusAttributeCategoryData) {
            synSpasDBMapper.deleteCusAttributeCategory(cusAttributeCategory);
            synSpasDBMapper.saveCusAttributeCategory(cusAttributeCategory);
        }
    }

    @Override
    public void addCusAttributeData() throws Exception {
        String url = spasPropertiesConfig.getUrl()
                + "/project-server/api/project/tc/cusAttributeList?startDate="
                + startDate + "&endDate=" + endDate;
        List<SpasCusAttribute> cusAttributeData = getSpasData(url, SpasCusAttribute.class);
        for (SpasCusAttribute cusAttribute : cusAttributeData) {
            synSpasDBMapper.deleteCusAttribute(cusAttribute);
            synSpasDBMapper.saveCusAttribute(cusAttribute);
        }
    }

    @Override
    public void addStiTeamRosterData() throws Exception {
        String url = spasPropertiesConfig.getUrl()
                + "/project-server/api/project/tc/teamRosterList?startDate="
                + startDate + "&endDate=" + endDate;
        List<SpasStiTeamRoster> stiTeamRosterData = getSpasData(url, SpasStiTeamRoster.class);
        for (SpasStiTeamRoster stiTeamRoster : stiTeamRosterData) {
            synSpasDBMapper.deleteStiTeamRoster(stiTeamRoster);
            synSpasDBMapper.saveStiTeamRoster(stiTeamRoster);
        }
    }

    @Override
    public void addManpowerStandardData() throws Exception {
        String url = spasPropertiesConfig.getUrl()
                + "/project-server/api/project/tc/manPowerList?startDate="
                + startDate + "&endDate=" + endDate;
        List<SpasManpowerStandard> manpowerStandardData = getSpasData(url, SpasManpowerStandard.class);
        for (SpasManpowerStandard manpowerStandard : manpowerStandardData) {
            synSpasDBMapper.deleteManpowerStandard(manpowerStandard);
            synSpasDBMapper.saveManpowerStandard(manpowerStandard);
        }
    }

    @Override
    public void test() throws Exception {
        TCSOAServiceFactory tCSOAServiceFactory =null;
        tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
        //SpasUtil.getDTTemplates(new ArrayList<String>(),tCSOAServiceFactory,"Dell");
       // SpasUtil.getMNTTemplates(tCSOAServiceFactory,"E1(A0)");
       // SpasUtil.getPrtTemplates(tCSOAServiceFactory);
    }

    @Override
    public void updateManpowerStandardData() throws Exception {
        TCSOAServiceFactory tCSOAServiceFactory =null;
        try {
            log.info("begin updateManpowerStandardData ======");
            String url = spasPropertiesConfig.getUrl()
                    + "/project-server/api/project/tc/manPowerList?startDate="
                    + startDate + "&endDate=" + endDate;
            List<SpasManpowerStandard> manpowerStandardData = getSpasData(url, SpasManpowerStandard.class);
            for (SpasManpowerStandard manpowerStandard : manpowerStandardData) {
                synSpasDBMapper.deleteManpowerStandard(manpowerStandard);
                synSpasDBMapper.saveManpowerStandard(manpowerStandard);
            }
            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS3);
            SavedQueryService savedQueryService = tCSOAServiceFactory.getSavedQueryService();

            List<Integer> projectIds=synSpasDBMapper.getManpowerDiff(startDate);
            for(Integer projId:projectIds) {
                String snapId=null;
                try {
                    log.info("begin update project id ===>" + projId);
                    List<ManpowerActionInfo> manpowerActionInfos = new ArrayList<>();
                    List<ManpowerRawInfo> manpowerRawInfos = synSpasDBMapper.getManpowerAction(projId);
                    if(manpowerRawInfos.size()<=0){
                        log.info("end update project id ===>" + projId+" no data");
                        continue;
                    }
                    snapId = sdf3.format(new Date())+projId;
                    log.info("snapId ===> "+snapId);
                    Map<String, ManpowerActionInfo> map = new HashMap<>();
                    Date maxCreateDate = null;
                    Date maxUpdateDate = null;
                    for (ManpowerRawInfo mr : manpowerRawInfos) {
                        if(maxCreateDate==null||mr.getCreateDate().getTime()>maxCreateDate.getTime()){
                            maxCreateDate=mr.getCreateDate();
                        }
                        if(maxUpdateDate==null||mr.getUpdateDate().getTime()>maxUpdateDate.getTime()){
                            maxUpdateDate=mr.getUpdateDate();
                        }
                        mr.setSnapId(snapId);
                        synSpasDBMapper.addSnap(mr);
                        String deptName = mr.getDeptName().trim();
                        String phase = mr.getPhase().trim();
                        Integer isActive = mr.getIsActive();
                        Float factor = mr.getFactor();
                        String key = projId + "_" + deptName;
                        ManpowerActionInfo manpowerActionInfo = map.get(key);
                        if (map.get(key) == null) {
                            manpowerActionInfo = new ManpowerActionInfo();
                            manpowerActionInfo.setProjectId("" + projId);
                            manpowerActionInfo.setDeptName(deptName);
                            map.put(key, manpowerActionInfo);
                            manpowerActionInfos.add(manpowerActionInfo);
                        }
                        if (isActive.intValue() > 0 && factor.floatValue() > 0) {
                            List<String> ls = manpowerActionInfo.getAddPhaseNames();
                            if (ls == null) {
                                ls = new ArrayList<>();
                                manpowerActionInfo.setAddPhaseNames(ls);
                            }
                            int f=0;
                            for(String s:ls){
                                if(s.equalsIgnoreCase(phase)){
                                    f=1;
                                }
                            }
                            if(f==0) {
                                ls.add(phase);
                            }
                        } else {
                            List<String> ls = manpowerActionInfo.getDeletePhaseNames();
                            if (ls == null) {
                                ls = new ArrayList<>();
                                manpowerActionInfo.setDeletePhaseNames(ls);
                            }
                            int f=0;
                            for(String s:ls){
                                   if(s.equalsIgnoreCase(phase)){
                                       f=1;
                                   }
                            }
                            if(f==0) {
                                ls.add(phase);
                            }
                        }
                    }
                    if(maxCreateDate==null || maxUpdateDate==null){
                        throw  new Exception("Action date is error");
                    }
                    for (ManpowerActionInfo action : manpowerActionInfos) {
                        log.info("begin dept ====> "+action.getDeptName());
                        String projectId = "";
                        projectId = "p" + action.getProjectId();
                        Map<String, Object> queryResults = TCUtils.executeQuery(savedQueryService, "__D9_Find_Project_Folder", new String[]{"d9_SPAS_ID"}, new String[]{(projectId)});
                        if (queryResults.get("succeeded") == null) {
                            continue;
                        }
                        ModelObject[] md = (ModelObject[]) queryResults.get("succeeded");
                        if (md == null || md.length <= 0) {
                            continue;
                        }
                        Folder projectFolder = (Folder) md[0];

                        PlatformFound platformFound = synSpasDBMapper.getSpasProject(projectId);
                        if (platformFound == null) {
                            log.info("=========>get project infos2======");
                            platformFound = synSpasDBMapper.getSpasProject2(projectId.replaceAll("p",""));
                            if(platformFound==null) {
                                continue;
                            }
                            String level=platformFound.getPlatformFoundLevel();
                            log.info("=========>level:"+level);
                            level=level.split(",")[1];
                            platformFound.setPlatformFoundLevel(level.trim());
                        }
                        Integer state=synSpasDBMapper.getHandleState(projectId);
                        if(state!=null&&state.intValue()!=2){
                            continue;
                        }
                        log.info("platformFound info ==== ");
                        log.info(JSONUtil.toJsonStr(platformFound));
                        String customerName = platformFound.getCName();
                        String productLine = platformFound.getProductLineName();
                        BUListRp buListRp = new BUListRp();
                        buListRp.setCustomer(customerName);
                        buListRp.setProductLine(productLine);
                        R<List<BURv>> buRv = hdfsClient.buList(buListRp);
                        List<BURv> data = buRv.getData();
                        String bu = "";
                        if (data != null && data.size() > 0) {
                            bu = data.get(0).getBu();
                        }
                        if (bu.equalsIgnoreCase("")) {
                            continue;
                        }
                        //專案是否已同步到hdfs
                        boolean isSync = false;
                        List<FolderInfo> deptFolders = synSpasDBMapper.getDeptFolders(projectId);
                        if (deptFolders != null && deptFolders.size() > 0) {
                            isSync = true;
                        }
                        String logStr = JSONUtil.toJsonStr(action);
                        log.info(snapId + " " + logStr);
                        if (BUConstant.DT.equals(bu)) {
                            manpowerServiceImpl.updateDTFolder(projectId, isSync, projectFolder, customerName, productLine, action, bu, tCSOAServiceFactory, snapId);
                        } else if (BUConstant.MNT.equals(bu)) {
                            manpowerServiceImpl.updateMNTFolder(projectId, isSync, projectFolder, action, bu, platformFound.getPlatformFoundLevel(), tCSOAServiceFactory, snapId);
                        } else if (BUConstant.SH.equals(bu)) {
                            manpowerServiceImpl.updateSHFolder(projectId, isSync, projectFolder, customerName, action,bu, tCSOAServiceFactory, snapId);
                        }else {
                            manpowerServiceImpl.updatePrtFolder(projectId, isSync, projectFolder, action, bu, tCSOAServiceFactory, snapId);
                        }
                        log.info("end dept ====> "+action.getDeptName());
                      }
                      synSpasDBMapper.addActionDate(snapId,projId,dateFormat2.format(maxCreateDate),dateFormat2.format(maxUpdateDate));
                    }catch (Exception e) {
                        log.error(e.getMessage(), e);
                        log.info("update project failed  id ====>" + projId + " " + e.getMessage());
                        try {
                            JSONObject httpmap = new JSONObject();
                            httpmap.put("sendTo", mailTo);
                            httpmap.put("sendCc", mailCC);
                            httpmap.put("subject", "【专案人力同步异常】请及时处理！");
                            log.error(e.getMessage(), e);
                            String message = e.getMessage();
                            message = env + "環境 , 專案【" + projId + "】 同步人力失敗，" + message;
                            httpmap.put("htmlmsg", message);
                            tcMail.sendMail3Method(httpmap.toJSONString());
                            synSpasDBMapper.addSnapHis(snapId,projId,message);
                        } catch (Exception e0) {
                            log.error(e0);
                        }
                    }
                    log.info("end  update project id ===>" + projId);
                }
        }catch(Exception e){
            throw e;
        }finally {
            try {
                if (tCSOAServiceFactory != null) {
                    tCSOAServiceFactory.logout();
                }
            }catch(Exception e){}
            log.info("end updateManpowerStandardData ======");
        }

    }


    @Override
    public void addFunctionData() throws Exception {
        String url = spasPropertiesConfig.getUrl()
                + "/user-server/api/user/tc/functionList?startDate="
                + startDate + "&endDate=" + endDate;
        List<SpasFunction> functionData = getSpasData(url, SpasFunction.class);
        for (SpasFunction function : functionData) {
            synSpasDBMapper.deleteFunction(function);
            synSpasDBMapper.saveFunction(function);
        }
    }

    @Override
    public void addRoutingData() throws Exception {
        String url = spasPropertiesConfig.getUrl()
                + "/project-server/api/project/tc/getRouting";
        List<SpasProjectRouting> functionData = getSpasDataByPost(url, SpasProjectRouting.class);
        for (SpasProjectRouting function : functionData) {
             String projectId=function.getProjectId();
             projectId=projectId.replaceAll("p","");
             STIProject  projInfo= synSpasDBMapper.getProjectInfo(""+projectId);
             if(projInfo==null) {
                continue;
             }
            String cname= projInfo.getCustomerName();
             if(cname==null){
                 continue;
             }
             cname=cname.toLowerCase(Locale.ENGLISH);
             if(!(cname.startsWith("lenovo"))&&!(cname.startsWith("hp"))&&!(cname.startsWith("dell"))){
                    continue;
             }
             Integer process= projInfo.getProcess();
             if(process==null ||process.intValue()<2){
                continue;
             }

            if(cname.startsWith("lenovo")) {
                String levels = projInfo.getPlatformFoundLevel();
                if (levels.split(",")[1].equalsIgnoreCase("") || levels.split(",")[1].equalsIgnoreCase("none")) {
                    continue;
                }
            }
            String bu="";
            BUListRp rp=new BUListRp();
            rp.setCustomer(projInfo.getCustomerName());
            rp.setProductLine(projInfo.getPlatformFoundProductLine());
            R<List<BURv>> buRv = hdfsClient.buList(rp);
            List<BURv> data = buRv.getData();
            if (data != null && data.size() > 0) {
              bu = data.get(0).getBu();
            }
            if(!("dt".equalsIgnoreCase(bu))){
                continue;
            }

            synSpasDBMapper.deleteRouting(function);
            synSpasDBMapper.saveRouting(function);
        }
    }

    @Override
    public void updateRoutingData() throws Exception {

        Calendar c1 = Calendar.getInstance();
        c1.add(Calendar.DAY_OF_MONTH, -600);
        startDate = dateFormat.format(c1.getTime());
        Calendar c2 = Calendar.getInstance();
        c2.add(Calendar.DAY_OF_MONTH, 1);
        endDate = dateFormat.format(c2.getTime());

        String url = spasPropertiesConfig.getUrl()
                + "/project-server/api/project/tc/getRouting";
        List<SpasProjectRouting> functionData = getSpasDataByPost(url, SpasProjectRouting.class);
        for (SpasProjectRouting function : functionData) {
            synSpasDBMapper.upRouting(function);
        }
    }


    private String getSpasToken(SpasPropertiesConfig spasPropertiesConfig) {
        String url = spasPropertiesConfig.getUrl() + "/user-server/api/user/sysSignIn";
        JSONObject parameterMap = new JSONObject();
        parameterMap.put("sysFlag", spasPropertiesConfig.getSysFlag());
        parameterMap.put("apiKey", spasPropertiesConfig.getApiKey());
        parameterMap.put("userName", spasPropertiesConfig.getUserName1());
        parameterMap.put("password", spasPropertiesConfig.getPassword1());
        JSONObject parameterJson = new JSONObject(parameterMap);
        String rs = HttpUtil.post(url, parameterJson.toString());
        JSONObject obj = JSONObject.parseObject(rs);
        if (200 == obj.getInteger("code")) {
            log.info("token ===> "+ obj.getString("data"));
            return obj.getString("data");
        }
        return null;
    }

    private String getSpasTokenprd() {
        String url = "https://spas.efoxconn.com/user-server/api/user/sysSignIn";
        JSONObject parameterMap = new JSONObject();
        parameterMap.put("sysFlag", "D");
        parameterMap.put("apiKey", "tcToSpas");
        parameterMap.put("userName", "IKF9kc7ON8q");
        parameterMap.put("password", "lfD3ENexfU3ky62");
        JSONObject parameterJson = new JSONObject(parameterMap);
        String rs = HttpUtil.post(url, parameterJson.toString());
        JSONObject obj = JSONObject.parseObject(rs);
        if (200 == obj.getInteger("code")) {
            log.info("token ===> "+ obj.getString("data"));
            return obj.getString("data");
        }
        return null;
    }

    private <T> List<T> getSpasData(String url, Class<T> clazz) throws Exception {
        String spasToken = getSpasToken(spasPropertiesConfig);
        if (spasToken == null) {
            throw new Exception("获取 Token 失败！");
        }
        HashMap<String, String> tokenMap = new HashMap<>();
        tokenMap.put("token", spasToken);
        HttpResponse rp = HttpUtil.createGet(url).addHeaders(tokenMap).execute();
        System.out.print(rp.body());
        String result = rp.body();
        log.info("url " + url);
        log.info("result " + result);
        JSONObject obj = JSONObject.parseObject(result);
       log.info("size ====>"+obj.getJSONArray("data").size());
        return JSONArray.parseArray(obj.getJSONArray("data").toJSONString(), clazz);
    }


    private <T> List<T> getSpasDataByPost(String url, Class<T> clazz) throws Exception {
        String spasToken = getSpasToken(spasPropertiesConfig);
        if (spasToken == null) {
            throw new Exception("获取 Token 失败！");
        }
        Map<String,String> paramMap=new HashMap<>();
        paramMap.put("startDate",startDate);
        paramMap.put("endDate",endDate);

        log.info(JSONUtil.toJsonStr(paramMap));
        String result = HttpUtil.createPost(url).header("token",spasToken).body(JSONUtil.toJsonStr(paramMap), "application/json").execute().body();
        log.info("url " + url);
        log.info("result " + result);
        JSONObject obj = JSONObject.parseObject(result);
        log.info("size ====>"+obj.getJSONArray("data").size());
        return JSONArray.parseArray(obj.getJSONArray("data").toJSONString(), clazz);
    }

    private <T> List<T> getSpasDataprd(String url, Class<T> clazz) throws Exception {
        String spasToken = getSpasTokenprd();
        if (spasToken == null) {
            throw new Exception("获取 Token 失败！");
        }
        HashMap<String, String> tokenMap = new HashMap<>();
        tokenMap.put("token", spasToken);
        HttpResponse rp = HttpUtil.createGet(url).addHeaders(tokenMap).execute();
        System.out.print(rp.body());
        String result = rp.body();
        log.info("url " + url);
        log.info("result " + result);
        JSONObject obj = JSONObject.parseObject(result);
        log.info("size ====>"+obj.getJSONArray("data").size());
        return JSONArray.parseArray(obj.getJSONArray("data").toJSONString(), clazz);
    }


}
