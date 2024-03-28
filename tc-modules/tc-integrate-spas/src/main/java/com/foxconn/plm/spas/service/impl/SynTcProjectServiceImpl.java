package com.foxconn.plm.spas.service.impl;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.BUConstant;
import com.foxconn.plm.spas.bean.*;
import com.foxconn.plm.spas.config.properties.SpasPropertiesConfig;
import com.foxconn.plm.spas.mapper.SynSpasChangeDataMapper;
import com.foxconn.plm.spas.service.SynTcChangeDataService;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.excel.ExcelUtil;
import com.foxconn.plm.utils.file.FileUtil;
import com.foxconn.plm.utils.tc.ProjectUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.cad.StructureManagementService;
import com.teamcenter.services.strong.cad._2013_05.StructureManagement;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core.ProjectLevelSecurityService;
import com.teamcenter.services.strong.core._2008_06.DataManagement;
import com.teamcenter.services.strong.core._2012_09.ProjectLevelSecurity;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.services.strong.query._2007_06.SavedQuery;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/12/ 9:06
 * @description
 */
@Service("synTcProjectServiceImpl")
public class SynTcProjectServiceImpl extends SynTcChangeDataService {
    private static Log log = LogFactory.get();
    private TCSOAServiceFactory tCSOAServiceFactory;
    private SynSpasChangeData synSpasChangeData;

    @Resource
    private SpasPropertiesConfig spasPropertiesConfig;

    @Resource
    private ManpowerServiceImpl manpowerServiceImpl;

    @Resource
    private SpasServiceImpl spasServiceImpl;

    @Resource
    private SynSpasChangeDataMapper synSpasChangeDataMapper;

    @Override
    public void synSpasDataToTc(TCSOAServiceFactory tCSOAServiceFactory, SynSpasChangeData synSpasChangeData) throws Exception {
        this.tCSOAServiceFactory = tCSOAServiceFactory;
        this.synSpasChangeData = synSpasChangeData;
        String operationType = synSpasChangeData.getPlatformOperationType();
        if ("A".equals(operationType)) {

            addFolder();
        }
        if ("D".equals(operationType)) {
            delFolder();
        }
        if ("C".equals(operationType)) {
            modFolder();
        }
        if ("M".equals(operationType)) {
            moveFolder();
        }

    }

    private void addFolder() throws Exception {
        Folder projectFolder = createProjectFolder();
        TC_Project tcProject = createTCProject(projectFolder);
        synProjectPersonals(tcProject);
        crateWorkspace(projectFolder, tcProject);
        if("dt".equalsIgnoreCase(synSpasChangeData.getBu())){
            String lov= null;
            String customer=synSpasChangeData.getCustomerName();
            if("dell".equalsIgnoreCase(customer)){
                lov="Dell";
            }
            if("hp".equalsIgnoreCase(customer)){
                lov="HP";
            }
            if("lenovo".equalsIgnoreCase(customer)){
                lov="Lenovo";
            }
            if(lov!=null){
                TCUtils.setProperties(tCSOAServiceFactory.getDataManagementService(), tcProject, "fnd0ProjectCategory", lov);
            }
        }
        List<ManpowerInfo> manpowerInfos=manpowerServiceImpl.getManPowers(synSpasChangeData.getPlatformFoundId());
        if (BUConstant.DT.equals(synSpasChangeData.getBu())) {
            List<String> phaseList = Arrays.asList(synSpasChangeData.getPlatformPhase().split(","));
            spasServiceImpl.createDTFolder(projectFolder,synSpasChangeData.getCustomerName(),synSpasChangeData.getProductLine(),manpowerInfos,phaseList,tCSOAServiceFactory);
        } else if (BUConstant.MNT.equals(synSpasChangeData.getBu())) {
            spasServiceImpl.createMNTFolder(projectFolder,manpowerInfos,synSpasChangeData.getPlatformLevel(),tCSOAServiceFactory);
        }  else if (BUConstant.SH.equals(synSpasChangeData.getBu())) {
            spasServiceImpl.createSHFolder(projectFolder,manpowerInfos,tCSOAServiceFactory,synSpasChangeData.getCustomerName());
        }else {
            spasServiceImpl.createPrtFolder(projectFolder,manpowerInfos,tCSOAServiceFactory);
        }
    }

    private void modFolder() throws Exception {
         String projectId = synSpasChangeData.getPlatformFoundId();
        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(), SynSpasConstants.D9_FIND_PROJECT_FOLDER,
                new String[]{SynSpasConstants.D9_SPAS_ID}, new String[]{projectId});
        ServiceData serviceData = savedQueryResult.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
            if (objs.length == 0) {
                throw new Exception("專案【" + projectId + "】更新專案名稱失敗，請確認是否需要同步專案並更新");
            }
            Folder folder = (Folder) objs[0];
            TCUtils.setProperties(tCSOAServiceFactory.getDataManagementService(), folder, SynSpasConstants.OBJECT_NAME, synSpasChangeData.getPlatformFoundName());
            tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{folder});
        } else {
            throw new Exception("專案【" + projectId + "】更新專案名稱失敗，請確認是否需要同步專案並更新：" + serviceData.getPartialError(0).getErrorValues()[0].getMessage());
        }

        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult1 = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(), SynSpasConstants.D9_FIND_PROJECT,
                new String[]{SynSpasConstants.D9_PROJECT_ID}, new String[]{projectId});
        ServiceData serviceData1 = savedQueryResult1.serviceData;
        if (serviceData1.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = savedQueryResult1.arrayOfResults[0].objects;
            if (objs.length == 0) {
                throw new Exception("修改专案项目时未找到专案项目.");
            }
            TC_Project tcProject = (TC_Project) objs[0];

            String tcProjectName = synSpasChangeData.getPlatformFoundName();
            tcProjectName = tcProjectName.replaceAll("\\*", "_").replaceAll("\\.", "_").replaceAll("%", "_").replaceAll("@", "_");
            SavedQuery.ExecuteSavedQueriesResponse res = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(), SynSpasConstants.D9_FIND_PROJECT,
                    new String[]{SynSpasConstants.D9_PROJECT_NAME}, new String[]{tcProjectName});
            ServiceData sd = res.serviceData;
            if (sd.sizeOfPartialErrors() == 0) {
                ModelObject[] objs2 = res.arrayOfResults[0].objects;
                if (objs2.length > 0) {
                    tcProjectName = synSpasChangeData.getSeriesName() + "_" + synSpasChangeData.getPlatformFoundName();
                }
            } else {
                throw new Exception("查询项目失败：" + sd.getPartialError(0).getErrorValues()[0].getMessage());
            }
            tcProjectName = tcProjectName.replaceAll("\\*", "_").replaceAll("\\.", "_").replaceAll("%", "_").replaceAll("@", "_");
            TCUtils.setProperties(tCSOAServiceFactory.getDataManagementService(), tcProject, SynSpasConstants.OBJECT_NAME, tcProjectName);
            tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{tcProject});
        } else {
            throw new Exception("修改专案项目失败：" + serviceData1.getPartialError(0).getErrorValues()[0].getMessage());
        }
    }

    private void delFolder() throws Exception {
        String projectId = synSpasChangeData.getPlatformFoundId();
        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(), SynSpasConstants.D9_FIND_PROJECT_FOLDER,
                new String[]{SynSpasConstants.D9_SPAS_ID}, new String[]{projectId});
        ServiceData serviceData = savedQueryResult.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
            if (objs.length == 0) {
                throw new Exception("删除专案文件夹时未找到专案文件夹.");
            }
            Folder folder = (Folder) objs[0];
            TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), folder, SynSpasConstants.OBJECT_NAME);
            String folderName = folder.get_object_name();
            TCUtils.deleteFolder2(tCSOAServiceFactory.getDataManagementService(), folder, folderName);
            tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{folder});
        } else {
            throw new Exception("删除专案文件夹失败：" + serviceData.getPartialError(0).getErrorValues()[0].getMessage());
        }
    }

    private void moveFolder() throws Exception {
        String platformFoundId = synSpasChangeData.getPlatformFoundId();
        String seriesId = synSpasChangeData.getSeriesId();
        Folder platformFoundFolder = null;
        Folder newSeriesFolder = null;
        SavedQueryService sqService = tCSOAServiceFactory.getSavedQueryService();
        DataManagementService dmService = tCSOAServiceFactory.getDataManagementService();
        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult1 = TCUtils.execute2Query(sqService, SynSpasConstants.D9_FIND_PROJECT_FOLDER,
                new String[]{SynSpasConstants.D9_SPAS_ID}, new String[]{platformFoundId});
        ServiceData serviceData1 = savedQueryResult1.serviceData;
        if (serviceData1.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = savedQueryResult1.arrayOfResults[0].objects;
            if (objs.length == 0) {
                throw new Exception("移动系列文件夹时未找到专案文件夹.");
            }
            platformFoundFolder = (Folder) objs[0];
        }else {
            throw new Exception("移动系列文件夹时查询专案文件夹失败：" + serviceData1.getPartialError(0).getErrorValues()[0].getMessage());
        }

        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult2 = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(), SynSpasConstants.D9_FIND_PROJECT_FOLDER,
                new String[]{SynSpasConstants.D9_SPAS_ID}, new String[]{seriesId});
        ServiceData serviceData2 = savedQueryResult2.serviceData;
        if (serviceData2.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = savedQueryResult2.arrayOfResults[0].objects;
            if (objs.length == 0) {
                throw new Exception("移动系列文件夹时未找到系列文件夹.");
            }
            newSeriesFolder = (Folder) objs[0];
        } else {
            throw new Exception("移动系列文件夹时查询系列文件夹失败：" + serviceData2.getPartialError(0).getErrorValues()[0].getMessage());
        }

        com.teamcenter.services.strong.core._2007_01.DataManagement.WhereReferencedResponse resp = dmService.whereReferenced(new WorkspaceObject[]{platformFoundFolder}, 1);
        ServiceData sd = resp.serviceData;
        if(sd.sizeOfPlainObjects() > 0){
            Folder oldSeriesFolder = (Folder) sd.getPlainObject(0);
            TCUtils.deleteRelation(dmService, oldSeriesFolder, platformFoundFolder, "contents");
        }
        TCUtils.addRelation(dmService, newSeriesFolder, platformFoundFolder, "contents");

//        ICTService ictService = tCSOAServiceFactory.getICTService();
//        ICT.Arg[] argss1 = new ICT.Arg[4];
//        ICT.Arg[] argss2 = new ICT.Arg[2];
//
//        ICT.Arg args1 = new ICT.Arg();
//        args1.val = "Folder";
//        argss1[0] = args1;
//
//        ICT.Arg args2 = new ICT.Arg();
//        args2.val = "TYPE::Fnd0HomeFolder::Folder::Folder";
//        argss1[1] = args2;
//
//        ICT.Structure[] structures = new ICT.Structure[1];
//        ICT.Arg args3 = new ICT.Arg();
//        args3.val = "true";
//        argss2[0] = args3;
//
//        User user = tCSOAServiceFactory.getUser();
//        TCUtils.getProperty(dmService, user, "home_folder");
//        Folder homeFolder = user.get_home_folder();
//        String uid = homeFolder.getUid();
//
//        ICT.Arg args4 = new ICT.Arg();
//        args4.val = uid;
//        argss2[1] = args4;
//
//        structures[0].args = argss2;
//        ICT.Arg args5 = new ICT.Arg();
//        args5.structure = structures;
//
//        ICT.Arg args6 = new ICT.Arg();
//        args6.val = "contents";
//        argss1[2] = args6;
//
//
//        ICT.Arg args7 = new ICT.Arg();
//        ICT.Array[] arrays = new ICT.Array[1];
//        ICT.Entry[] entrys = new ICT.Entry[1];
//        entrys[0].val = seriesFolder.getUid();
//        arrays[0].entries = entrys;
//        args7.array = arrays;
//
//        ICT.InvokeICTMethodResponse resp = ictService.invokeICTMethod("ICCT", "removeRelated", argss1);
    }

    private Folder createProjectFolder() throws Exception {
        String seriesId = synSpasChangeData.getSeriesId();
        Folder seriesFolder = null;
        Folder projectFolder = null;

        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(), SynSpasConstants.D9_FIND_PROJECT_FOLDER,
                new String[]{SynSpasConstants.D9_SPAS_ID}, new String[]{seriesId});
        ServiceData serviceData = savedQueryResult.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
            if (objs.length == 0) {
                throw new Exception("创建专案文件夹时未找到系列文件夹.");
            }
            seriesFolder = (Folder) objs[0];
        } else {
            throw new Exception("创建专案文件夹时未找到系列文件夹：" + serviceData.getPartialError(0).getErrorValues()[0].getMessage());
        }

        Map<String, String> propMap = new HashMap<>();
        propMap.put(SynSpasConstants.D9_SPAS_ID, synSpasChangeData.getPlatformFoundId());
        propMap.put(SynSpasConstants.OBJECT_NAME, synSpasChangeData.getPlatformFoundName());
        DataManagement.CreateResponse resp = TCUtils.createObjects(tCSOAServiceFactory.getDataManagementService(), SynSpasConstants.D9_PLATFORMFOUND, propMap);
        ServiceData service1Data = resp.serviceData;
        if (service1Data.sizeOfPartialErrors() == 0) {
            projectFolder = (Folder) resp.output[0].objects[0];
            TCUtils.addContents(tCSOAServiceFactory.getDataManagementService(), seriesFolder, projectFolder);
            tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{seriesFolder, projectFolder});
        } else {
            throw new Exception("创建专案文件夹失败：" + serviceData.getPartialError(0).getErrorValues()[0].getMessage());
        }
        return projectFolder;
    }

    private TC_Project createTCProject(Folder projectFolder) throws Exception {

        User adminUser = getAdminUser();
        tCSOAServiceFactory.getDataManagementService().getProperties(new ModelObject[]{adminUser}, new String[]{"user_id", "default_group"});
        String userId = adminUser.get_user_id();
        Group defaultGroup = (Group) adminUser.get_default_group();
        tCSOAServiceFactory.getDataManagementService().getProperties(new ModelObject[]{defaultGroup}, new String[]{"full_name"});
        String userGroup = defaultGroup.get_full_name();
        GroupMember adminGroupMember = getAdminGroupMember(userId, userGroup);

        TCUtils.byPass(tCSOAServiceFactory.getSessionService(), true);
        String tcProjectName = synSpasChangeData.getPlatformFoundName();


        tcProjectName = tcProjectName.replaceAll("\\*", "_").replaceAll("\\.", "_").replaceAll("%", "_").replaceAll("@", "_");

        SavedQuery.ExecuteSavedQueriesResponse res = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(), SynSpasConstants.D9_FIND_PROJECT,
                new String[]{SynSpasConstants.D9_PROJECT_NAME}, new String[]{tcProjectName});
        ServiceData sd = res.serviceData;
        if (sd.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = res.arrayOfResults[0].objects;
            if (objs.length > 0) {
                tcProjectName = synSpasChangeData.getSeriesName() + "_" + synSpasChangeData.getPlatformFoundName();
            }
        } else {
            throw new Exception("查询项目失败：" + sd.getPartialError(0).getErrorValues()[0].getMessage());
        }

        tcProjectName = tcProjectName.replaceAll("\\*", "_").replaceAll("\\.", "_").replaceAll("%", "_").replaceAll("@", "_");

        ProjectLevelSecurity.ProjectOpsResponse response = TCUtils.createTCProject(
                tCSOAServiceFactory.getProjectLevelSecurityService(),
                synSpasChangeData.getPlatformFoundId().toUpperCase(), tcProjectName,
                "", adminUser, adminGroupMember);
        TCUtils.byPass(tCSOAServiceFactory.getSessionService(), false);


        TC_Project tcProject = null;
        ServiceData serviceData = response.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            tcProject = (TC_Project) serviceData.getCreatedObject(0);
        } else {

            throw new Exception("創建【" + synSpasChangeData.getPlatformFoundId() +"】TC_Project失敗,"+serviceData.getPartialError(0).getErrorValues()[0].getMessage());
        }



        ProjectUtil.assignedProject(tCSOAServiceFactory.getProjectLevelSecurityService(), projectFolder, tcProject);
        return tcProject;
    }

    private User getAdminUser() throws Exception {
        User user = null;
        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(), SynSpasConstants.D9_WEB_FIND_USER,
                new String[]{SynSpasConstants.D9_USER_ID}, new String[]{spasPropertiesConfig.getAdmin()});
        ServiceData serviceData = savedQueryResult.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
            if (objs.length == 0) {
                throw new Exception("创建TC_Project时未找到管理员用户.");
            }
            user = (User) objs[0];
        } else {
            throw new Exception("创建TC_Project时未找到管理员用户：" + serviceData.getPartialError(0).getErrorValues()[0].getMessage());
        }
        return user;
    }

    private GroupMember getAdminGroupMember(String userId, String userGroup) throws Exception {
        GroupMember groupMember = null;
        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(), SynSpasConstants.D9_EINT_GROUP_MEMBERS,
                new String[]{SynSpasConstants.D9_USER_USER_ID, SynSpasConstants.D9_GROUP_GROUP_NAME}, new String[]{userId, userGroup});
        ServiceData serviceData = savedQueryResult.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
            if (objs.length == 0) {
                throw new Exception("创建TC_Project时未找到用户GroupMember.");
            }
            groupMember = (GroupMember) objs[0];
        } else {
            throw new Exception("创建TC_Project时未找到管理员GroupMember：" + serviceData.getPartialError(0).getErrorValues()[0].getMessage());
        }
        return groupMember;
    }

    private void synProjectPersonals(TC_Project tcProjcet) throws Exception {
      /*  String[] TMLTemplats = TCUtils.getTCPreferences(tCSOAServiceFactory.getPreferenceManagementService(), SynSpasConstants.D9_PROJECT_TML_TEMPLATE);//通过首选项获取模板
        Dataset dataset = (Dataset) TCUtils.findObjectByUid(tCSOAServiceFactory.getDataManagementService(), TMLTemplats[0]);
        //String dirPath = cn.hutool.core.io.FileUtil.getAbsolutePath("");
        String dirPath = System.getProperty("java.io.tmpdir");
        FileUtil.checkSecurePath(dirPath);
        log.info("Template path =====> "+dirPath);
        FileUtil.checkSecurePath(dirPath);
        File file = TCUtils.downloadDataset(tCSOAServiceFactory.getDataManagementService(),
                tCSOAServiceFactory.getFileManagementUtility(dirPath), dataset, dirPath);
        XSSFWorkbook wb = ExcelUtil.getWorkbook(file);
        XSSFSheet sheet = null;
        if ("DT".equalsIgnoreCase(synSpasChangeData.getBu())) {
            sheet = ExcelUtil.getSheet(wb, "DT");
        } else if ("PRT".equalsIgnoreCase(synSpasChangeData.getBu())) {
            sheet = ExcelUtil.getSheet(wb, "PRT");
        } else if ("MNT".equalsIgnoreCase(synSpasChangeData.getBu())) {
            sheet = ExcelUtil.getSheet(wb, "MNT");
        } else {
            throw new Exception("读取Excel模板失败");
        }
        HashMap<String, List<ProjectPersonl>> tmls = new HashMap<>();
        String tmp = "";
        //解析Excel 按客户分组
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            XSSFRow row = sheet.getRow(i);
            if(row ==null){
                break;
            }
            String dept = ExcelUtil.getCellValueToString(row.getCell(2));
            if (!dept.equalsIgnoreCase("")) {
                tmp = dept;
            } else {
                dept = tmp;
            }
            String userNumber = ExcelUtil.getCellValueToString(row.getCell(4));
            if (userNumber == null || "".equalsIgnoreCase(userNumber)) {
                break;
            }
            List<ProjectPersonl> users = tmls.get(dept);
            if (users == null) {
                users = new ArrayList<>();
                tmls.put(dept, users);
            }
            ProjectPersonl projectPersonl = new ProjectPersonl();
            projectPersonl.setUserNumber(userNumber);
            projectPersonl.setOperationType("A");
            users.add(projectPersonl);
        }
        //专案成员
       List<ProjectPersonl> projectPersonls = tmls.get(synSpasChangeData.getCustomerName());
        if (projectPersonls == null) {
            projectPersonls = new ArrayList<>();
        }
        List<ProjectPersonl> projectPersonlsAll = tmls.get("All");
        if (projectPersonlsAll != null) {
            for (ProjectPersonl p : projectPersonlsAll) {
                projectPersonls.add(p);
            }
        }
        wb.close();
        if (projectPersonls == null || projectPersonls.size() <= 0) {
            System.out.println("没有可处理数据！");
            return;
        }*/
        //同步专案成员信息
        String queryBU="";
        String queryCustomerName="";
        if ("DT".equalsIgnoreCase(synSpasChangeData.getBu())) {
            queryBU="Desktop";
        } else if ("PRT".equalsIgnoreCase(synSpasChangeData.getBu())) {
            queryBU="Printer";
        } else if ("MNT".equalsIgnoreCase(synSpasChangeData.getBu())) {
            queryBU="Monitor";
        } else if ("SH".equalsIgnoreCase(synSpasChangeData.getBu())) {
            queryBU="Desktop";
        }else {
            throw new Exception("BU 不支持");
        }

        if ("Dell".equalsIgnoreCase(synSpasChangeData.getCustomerName())) {
            queryCustomerName="D";
        } else if ("HP".equalsIgnoreCase(synSpasChangeData.getCustomerName())) {
            queryCustomerName="H";
        } else if ("Lenovo".equalsIgnoreCase(synSpasChangeData.getCustomerName())) {
            queryCustomerName="L";
        }

        if ("SH".equalsIgnoreCase(synSpasChangeData.getBu())) {
            queryCustomerName="S";
        }


       List<ProjectPersonl>  projectPersonls= synSpasChangeDataMapper.querySpasTML(queryBU,queryCustomerName);
        doProject(tcProjcet, projectPersonls);
    }

    private void doProject(TC_Project project, List<ProjectPersonl> projectPersonls) throws Exception {

        TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), project, "object_name");
        String projectName = project.get_object_name();

        ProjectLevelSecurityService projectLevelSecurityService = tCSOAServiceFactory.getProjectLevelSecurityService();
        ProjectLevelSecurity.ProjectClientId[] projectClientIds = new ProjectLevelSecurity.ProjectClientId[1];
        ProjectLevelSecurity.ProjectClientId projectClientId = new ProjectLevelSecurity.ProjectClientId();
        projectClientId.tcProject = project;
        projectClientIds[0] = projectClientId;

        ProjectLevelSecurity.ProjectTeamsResponse projectTeamsResponse = projectLevelSecurityService.getProjectTeams(projectClientIds);
        ProjectLevelSecurity.ProjectTeamData[] projectTeamDatas = projectTeamsResponse.projectTeams;
        List<ModelObject> groupMembers = new ArrayList<>();
        List<ModelObject> adminUsers = new ArrayList<>();
        Map<String, ModelObject> memberMap = new HashMap<>();
        //获取专案已有的专案成员信息
        for (ProjectLevelSecurity.ProjectTeamData projectTeamData : projectTeamDatas) {
            ModelObject[] privs = projectTeamData.privMembers;
            ModelObject[] regulars = projectTeamData.regularMembers;
            ModelObject[] admins = projectTeamData.projectTeamAdmins;

            for (ModelObject model : privs) {
                User u = null;
                if (model instanceof User) {
                    u = (User) model;
                } else if (model instanceof GroupMember) {
                    GroupMember groupMember = (GroupMember) model;
                    TCUtils.findObjectByUid(tCSOAServiceFactory.getDataManagementService(), groupMember.getUid());
                    TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), groupMember, "user");
                    u = (User) groupMember.get_user();
                    //过滤掉删除的数据
                    if (isNeedRemove(u, projectPersonls)) {
                        continue;
                    }
                    groupMembers.add(model);
                }
                if (u != null && memberMap.get(u.getUid()) == null) {
                    memberMap.put(u.getUid(), u);
                }
            }

            for (ModelObject model : regulars) {
                User u = null;
                if (model instanceof User) {
                    u = (User) model;
                } else if (model instanceof GroupMember) {
                    GroupMember groupMember = (GroupMember) model;
                    TCUtils.findObjectByUid(tCSOAServiceFactory.getDataManagementService(), groupMember.getUid());
                    TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), groupMember, "user");
                    u = (User) groupMember.get_user();
                    //过滤掉删除的数据
                    if (isNeedRemove(u, projectPersonls)) {
                        continue;
                    }
                    groupMembers.add(model);
                }
                if (u != null && memberMap.get(u.getUid()) == null) {
                    memberMap.put(u.getUid(), u);
                }
            }

            for (ModelObject model : admins) {
                User u = null;
                if (model instanceof User) {
                    u = (User) model;
                    adminUsers.add(u);
                } else if (model instanceof GroupMember) {
                    GroupMember groupMember = (GroupMember) model;
                    tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{groupMember});
                    TCUtils.findObjectByUid(tCSOAServiceFactory.getDataManagementService(), groupMember.getUid());
                    TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), groupMember, "user");
                    u = (User) groupMember.get_user();
                    adminUsers.add(u);
                    groupMembers.add(model);
                }
                if (u != null && memberMap.get(u.getUid()) == null) {
                    memberMap.put(u.getUid(), u);
                }
            }
        }
        String userStrs = "";
        for (int i = 0; i < projectPersonls.size(); i++) {
            ProjectPersonl p = projectPersonls.get(i);
            userStrs += p.getUserNumber() + ";";
        }
        userStrs = userStrs + "tcadmin" + ";";
        if (userStrs.endsWith(";")) {
            userStrs = userStrs.substring(0, userStrs.length() - 1);
        }

        ModelObject[] newUsers = null;

        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(),
                SynSpasConstants.D9_WEB_FIND_USER, new String[]{SynSpasConstants.D9_USER_ID}, new String[]{userStrs});
        ServiceData serviceData = savedQueryResult.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            newUsers = savedQueryResult.arrayOfResults[0].objects;
        } else {
            throw new Exception(projectName + " 查詢用戶失敗 user id"+userStrs+" "+serviceData.getPartialError(0).getErrorValues()[0].getMessage());
        }

        List<User> mailUsers = new ArrayList();
        for (ModelObject u : newUsers) {
            if (memberMap.get(u.getUid()) == null) {
                User user = (User) u;
                TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), user, "user_id");
                //过滤掉 删除的数据
                if (isNeedRemove(user, projectPersonls)) {
                    continue;
                }
                mailUsers.add(user);
                if ("tcadmin".equalsIgnoreCase(user.get_user_id())) {
                    adminUsers.add(user);
                }
                groupMembers.add(findGroupMember(user.get_user_id()));
            }
        }

        com.teamcenter.services.strong.core._2017_05.ProjectLevelSecurity.ModifyProjectsInfo2[] modifyProjectsInfos = new com.teamcenter.services.strong.core._2017_05.ProjectLevelSecurity.ModifyProjectsInfo2[1];
        com.teamcenter.services.strong.core._2017_05.ProjectLevelSecurity.ModifyProjectsInfo2 modifyProjectsInfo = new com.teamcenter.services.strong.core._2017_05.ProjectLevelSecurity.ModifyProjectsInfo2();
        modifyProjectsInfo.sourceProject = project;

        com.teamcenter.services.strong.core._2017_05.ProjectLevelSecurity.ProjectInformation2 projectInformation = new com.teamcenter.services.strong.core._2017_05.ProjectLevelSecurity.ProjectInformation2();
        TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), project, "project_id");
        projectInformation.projectId = project.get_project_id();
        projectInformation.projectName = projectName;
        ProjectLevelSecurity.TeamMemberInfo[] teamMemberInfos0 = new ProjectLevelSecurity.TeamMemberInfo[groupMembers.size()];

        for (int i = 0; i < groupMembers.size(); i++) {
            ModelObject modelObj = groupMembers.get(i);
            ProjectLevelSecurity.TeamMemberInfo teamMemberInfo = new ProjectLevelSecurity.TeamMemberInfo();
            teamMemberInfo.teamMember = modelObj;
            teamMemberInfo.teamMemberType = 0;
            teamMemberInfos0[i] = teamMemberInfo;
        }

        //添加特权用户
        List<ProjectLevelSecurity.TeamMemberInfo> teamMemberInfoTmps = new ArrayList<>();
        for (int i = 0; i < groupMembers.size(); i++) {
            ModelObject modelObj = groupMembers.get(i);
            GroupMember groupMember = (GroupMember) modelObj;
            TCUtils.findObjectByUid(tCSOAServiceFactory.getDataManagementService(), groupMember.getUid());
            TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), groupMember, "user");
            ModelObject u = groupMember.get_user();
            int f = 0;
            //过滤掉admin用户
            for (ModelObject a : adminUsers) {
                if (a.getUid().equalsIgnoreCase(u.getUid())) {
                    f = 1;
                    break;
                }
            }
            if (f == 1) {
                continue;
            }

            ProjectLevelSecurity.TeamMemberInfo teamMemberInfo = new ProjectLevelSecurity.TeamMemberInfo();
            teamMemberInfo.teamMember = u;
            teamMemberInfo.teamMemberType = 1;
            teamMemberInfoTmps.add(teamMemberInfo);
        }

        ProjectLevelSecurity.TeamMemberInfo[] teamMemberInfos1 = new ProjectLevelSecurity.TeamMemberInfo[teamMemberInfoTmps.size()];
        for (int i = 0; i < teamMemberInfoTmps.size(); i++) {
            teamMemberInfos1[i] = teamMemberInfoTmps.get(i);
        }
        //添加admin用户
        ProjectLevelSecurity.TeamMemberInfo[] teamMemberInfos2 = new ProjectLevelSecurity.TeamMemberInfo[adminUsers.size()];
        for (int i = 0; i < adminUsers.size(); i++) {
            ModelObject u = adminUsers.get(i);
            ProjectLevelSecurity.TeamMemberInfo teamMemberInfo = new ProjectLevelSecurity.TeamMemberInfo();
            teamMemberInfo.teamMember = u;
            teamMemberInfo.teamMemberType = 2;
            teamMemberInfos2[i] = teamMemberInfo;
        }

        ProjectLevelSecurity.TeamMemberInfo[] teamMemberInfos = ArrayUtil.addAll(teamMemberInfos0, teamMemberInfos1, teamMemberInfos2);

        projectInformation.teamMembers = teamMemberInfos;
        projectInformation.useProgramContext = false;
        projectInformation.visible = true;
        projectInformation.active = true;

        Map<String, String[]> pmap = new HashMap<>();
        pmap.put("fnd0InheritTeamFromParent", new String[]{"0"});
        pmap.put("fnd0CollaborationCategories", new String[]{""});
        pmap.put("fnd0ProjectCategory", new String[]{""});
        projectInformation.propertyMap = pmap;
        modifyProjectsInfo.projectInfo = projectInformation;

        modifyProjectsInfos[0] = modifyProjectsInfo;
        projectLevelSecurityService.modifyProjects2(modifyProjectsInfos);
        //发邮件通知相关人员
        User[] us = new User[mailUsers.size()];
        for (int i = 0; i < mailUsers.size(); i++) {
            us[i] = mailUsers.get(i);
        }
        sendMail(us, projectName);
    }

    private boolean isNeedRemove(User u, List<ProjectPersonl> projectPersonls) throws Exception {
        TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), u, "user_id");
        String user_id = u.get_user_id();
        ProjectPersonl p0 = null;
        for (ProjectPersonl p : projectPersonls) {
            if (user_id.equalsIgnoreCase(p.getUserNumber())) {
                p0 = p;
            }
        }
        if (p0 != null && "D".equalsIgnoreCase(p0.getOperationType())) {
            return true;
        }
        return false;
    }

    private GroupMember findGroupMember(String userId) throws Exception {
        User u;
        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(),
                SynSpasConstants.D9_WEB_FIND_USER, new String[]{SynSpasConstants.D9_USER_ID}, new String[]{userId});
        ServiceData serviceData = savedQueryResult.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
            if (objs.length == 0) {
                throw new Exception(userId + " is not exist");
            }
            u = (User) objs[0];
        } else {
            throw new Exception(userId + " is not exist");
        }
        TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), u, "default_group");
        Group g = (Group) u.get_default_group();
        TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), g, "full_name");
        String group = g.get_full_name();

        GroupMember groupMember;
        ImanQuery query = getSavedQuery("__EINT_group_members");
        SavedQuery.SavedQueryInput[] inputs = new SavedQuery.SavedQueryInput[1];
        inputs[0] = new SavedQuery.SavedQueryInput();
        inputs[0].query = query;
        inputs[0].entries = new String[2];
        inputs[0].values = new String[2];
        inputs[0].entries[0] = "User";
        inputs[0].values[0] = userId;
        inputs[0].entries[1] = "Group";
        inputs[0].values[1] = group;
        inputs[0].maxNumToInflate = -1;
        SavedQuery.ExecuteSavedQueriesResponse response = tCSOAServiceFactory.getSavedQueryService().executeSavedQueries(inputs);
        groupMember = (GroupMember) response.arrayOfResults[0].objects[0];
        tCSOAServiceFactory.getDataManagementService().loadObjects(new String[]{groupMember.getUid()});
        return groupMember;
    }

    private static HashMap<String, ImanQuery> queryMap = new HashMap<String, ImanQuery>();

    private ImanQuery getSavedQuery(String queryName) throws Exception {
        ImanQuery query = null;
        query = queryMap.get(queryName);
        if (query != null) {
            return query;
        }
        com.teamcenter.services.strong.query._2006_03.SavedQuery.GetSavedQueriesResponse response = tCSOAServiceFactory.getSavedQueryService().getSavedQueries();
        for (int i = 0; i < response.queries.length; i++) {
            if (response.queries[i].name.equalsIgnoreCase(queryName)) {
                query = response.queries[i].query;
                break;
            }
        }
        return query;
    }

    private void sendMail(User[] users, String projName) throws Exception {
        DataManagement.CreateIn createIn = new DataManagement.CreateIn();
        createIn.clientId = "";
        createIn.data.boName = "Envelope";
        createIn.data.stringProps.put("object_name", "您已经是专案:" + projName + "的专案成员了");
        createIn.data.stringProps.put("object_desc", "您已经是专案:" + projName + "的专案成员了");
        createIn.data.tagArrayProps.put("listOfReceivers", users);
        DataManagement.CreateResponse response = tCSOAServiceFactory.getDataManagementService().createObjects(new DataManagement.CreateIn[]{createIn});
        ModelObject[] ms = response.output[0].objects;

        Envelope[] envelopes = new Envelope[ms.length];
        for (int i = 0; i < ms.length; i++) {
            envelopes[i] = (Envelope) ms[i];
        }
        tCSOAServiceFactory.getEnvelopeService().sendAndDeleteEnvelopes(envelopes);
    }

    private void crateWorkspace(Folder projectFolder, TC_Project tcProject) throws Exception {
        String workspaceTempId = getWorkspaceTempId();
        if(workspaceTempId==null||"".equalsIgnoreCase(workspaceTempId)){
            return;
        }
        Item workspaceTempItem = getWorkspaceTempById(workspaceTempId);
        List<WorkAreaFolderInfo> workspaceNameList = getWorkspaceName(workspaceTempItem);
        Folder WorkspaceFolder = null;
        for (int i = 0; i < workspaceNameList.size(); i++) {
            WorkAreaFolderInfo waf = workspaceNameList.get(i);
            Map<String, String> propMap = new HashMap<>();
            propMap.put(SynSpasConstants.OBJECT_NAME, waf.getName());
            propMap.put(SynSpasConstants.OBJECT_DESC, waf.getDesc());
            DataManagement.CreateResponse response = TCUtils.createObjects(tCSOAServiceFactory.getDataManagementService(), SynSpasConstants.D9_WORKAREA, propMap);
            ServiceData serviceData = response.serviceData;
            if (serviceData.sizeOfPartialErrors() == 0) {
                ModelObject[] folders = response.output[0].objects;
                if (WorkspaceFolder == null) {
                    WorkspaceFolder = (Folder) folders[0];
                } else {
                    Folder folder = (Folder) folders[0];
                    ProjectUtil.assignedProject(tCSOAServiceFactory.getProjectLevelSecurityService(), folder, tcProject);
                    TCUtils.addContents(tCSOAServiceFactory.getDataManagementService(), WorkspaceFolder, folder);
                }
            } else {
                throw new Exception("创建工作区文件夹失败：" + serviceData.getPartialError(0).getErrorValues()[0].getMessage());
            }
        }
        TCUtils.addContents(tCSOAServiceFactory.getDataManagementService(), projectFolder, WorkspaceFolder);
    }

    private String getWorkspaceTempId() throws Exception {
        String tempId = "";
        String[] workAreaTemps = TCUtils.getTCPreferences(
                tCSOAServiceFactory.getPreferenceManagementService(), SynSpasConstants.D9_WORKAREA_FOLDER_TEMPLATE);
        if (workAreaTemps == null) {
            throw new Exception("TC系统中未找到【D9_WorkArea_Folder_Template】首选项");
        }
        for (int i = 0; i < workAreaTemps.length; i++) {
            String workAreaTemp = workAreaTemps[i];
            String[] temp = workAreaTemp.split(":");
            String buName = temp[0];
            if (buName.equals(synSpasChangeData.getBu())) {
                tempId = temp[1];
                break;
            }
        }
        return tempId;
    }

    private Item getWorkspaceTempById(String workspaceTempId) throws Exception {
        Item item = null;
        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(
                tCSOAServiceFactory.getSavedQueryService(), SynSpasConstants.D9_ITEM_NAME_OR_ID,
                new String[]{SynSpasConstants.D9_ITEM_ID}, new String[]{workspaceTempId});
        ServiceData serviceData = savedQueryResult.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
            if (objs.length == 0) {
                throw new Exception("创建工作区文件夹时未找模板：" + workspaceTempId);
            }
            item = (Item) objs[0];
        } else {
            throw new Exception("创建工作区文件夹时未找模板：" + serviceData.getPartialError(0).getErrorValues()[0].getMessage());
        }
        return item;
    }

    private List<WorkAreaFolderInfo> getWorkspaceName(Item item) {
        List<WorkAreaFolderInfo> bomLineNameList = null;
        try {
            TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), item, "bom_view_tags");
            ModelObject[] bom_view_tags = item.get_bom_view_tags();
            BOMView bomView = (BOMView) bom_view_tags[0];
            ItemRevision itemRev = TCUtils.getItemLatestRevision(tCSOAServiceFactory.getDataManagementService(), item);
            StructureManagement.CreateWindowsInfo2[] createWindowsInfo2s = new StructureManagement.CreateWindowsInfo2[1];
            createWindowsInfo2s[0] = new StructureManagement.CreateWindowsInfo2();
            createWindowsInfo2s[0].item = item;
            createWindowsInfo2s[0].itemRev = itemRev;
            createWindowsInfo2s[0].bomView = bomView;
            StructureManagementService smService = tCSOAServiceFactory.getStructureManagementService();
            com.teamcenter.services.strong.cad._2007_01.StructureManagement.CreateBOMWindowsResponse response = smService.createBOMWindows2(createWindowsInfo2s);
            BOMLine topBOMLine = response.output[0].bomLine;
            BOMWindow bomWindow = response.output[0].bomWindow;
            bomLineNameList = getChildren(topBOMLine);
            smService.saveBOMWindows(new BOMWindow[]{bomWindow});
            smService.closeBOMWindows(new BOMWindow[]{bomWindow});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bomLineNameList;
    }

    private List<WorkAreaFolderInfo> getChildren(BOMLine topBOMLine) {
        List<WorkAreaFolderInfo> bomLineNameList = null;
        try {
            TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), topBOMLine, "bl_item_object_name");
            String topBOMLineName = topBOMLine.get_bl_item_object_name();
            WorkAreaFolderInfo waf = new WorkAreaFolderInfo();
            waf.setName(topBOMLineName);
            waf.setDesc("");
            bomLineNameList = new ArrayList<>();
            bomLineNameList.add(waf);

            StructureManagementService smService = tCSOAServiceFactory.getStructureManagementService();
            com.teamcenter.services.strong.cad._2007_01.StructureManagement.ExpandPSAllLevelsInfo expandPSAllLevelsInfo = new com.teamcenter.services.strong.cad._2007_01.StructureManagement.ExpandPSAllLevelsInfo();
            expandPSAllLevelsInfo.parentBomLines = new BOMLine[]{topBOMLine};
            expandPSAllLevelsInfo.excludeFilter = "None";

            com.teamcenter.services.strong.cad._2007_01.StructureManagement.ExpandPSAllLevelsPref expandPSAllLevelsPref = new com.teamcenter.services.strong.cad._2007_01.StructureManagement.ExpandPSAllLevelsPref();
            expandPSAllLevelsPref.expItemRev = false;

            com.teamcenter.services.strong.cad._2007_01.StructureManagement.ExpandPSAllLevelsResponse expandPSAllLevelsResponse = smService.expandPSAllLevels(expandPSAllLevelsInfo, expandPSAllLevelsPref);
            ServiceData serviceData = expandPSAllLevelsResponse.serviceData;
            for (int i = 0; i < serviceData.sizeOfCreatedObjects(); i++) {
                BOMLine bomLine = (BOMLine) serviceData.getCreatedObject(i);
                TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), bomLine, "bl_item_object_name");
                TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), bomLine, "bl_item_object_desc");
                String itemName = bomLine.get_bl_item_object_name();
                String itemDesc = bomLine.get_bl_item_object_desc();
                WorkAreaFolderInfo waf1 = new WorkAreaFolderInfo();
                waf1.setName(itemName);
                waf1.setDesc(itemDesc);
                bomLineNameList.add(waf1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bomLineNameList;
    }








}
