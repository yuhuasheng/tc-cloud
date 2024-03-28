package com.foxconn.plm.spas.scheduled;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCFolderConstant;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.param.BUListRp;
import com.foxconn.plm.entity.response.BURv;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.feign.service.HDFSClient;
import com.foxconn.plm.spas.bean.PlatformFound;
import com.foxconn.plm.spas.bean.ProjectInfo;
import com.foxconn.plm.spas.mapper.SynSpasDBMapper;
import com.foxconn.plm.spas.service.impl.SynSpasProjectStatusServiceImpl;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.FolderUtil;
import com.foxconn.plm.utils.tc.ProjectUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core.ProjectLevelSecurityService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.services.strong.query._2007_06.SavedQuery;
import com.teamcenter.services.strong.workflow.WorkflowService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.Folder;
import com.teamcenter.soa.client.model.strong.TC_Project;
import com.teamcenter.soa.client.model.strong.WorkspaceObject;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2023/02/14/ 11:00
 * @description
 */
@Component
public class SynSpasProjectStatusTask {

    private static Log log = LogFactory.get();

    @Resource
    private SynSpasDBMapper synSpasDBMapper;

    @Resource
    private HDFSClient hdfsClient;

    @Resource
    private SynSpasProjectStatusServiceImpl synSpasProjectStatusServiceImpl;



     // @PostConstruct
    //@Scheduled(cron = "0 30 * * * ? ")//每个半点执行一次
   @XxlJob("synSpasProjectStatus")
    public void timedTask() {
        log.info("==================同步专案状态开始==================");
        TCSOAServiceFactory tCSOAServiceFactory = null;
        try {
            List<String> projectSPASIds = synSpasProjectStatusServiceImpl.getProjectSPASIdAll();
            List<String> projectIds = new ArrayList<>();
            for (int i = 0; i < projectSPASIds.size(); i++) {
                String projectId = projectSPASIds.get(i);
                if (projectId == null){
                    continue;
                }
                String id = projectId.substring(1, projectId.length());
                projectIds.add(id);
            }
            List<ProjectInfo> projectInfos = synSpasProjectStatusServiceImpl.getProjectInfoById(projectIds);

            //spas进行中的项目
            List<ProjectInfo> spasOngoingProject = projectInfos.stream().filter(e -> e.getProjectStatus() == 2)
                    .collect(Collectors.toList());

            //spas关闭、暂停的项目
            List<ProjectInfo> spasCloseSuspendProject = projectInfos.stream().filter(e -> e.getProjectStatus() == 3 || e.getProjectStatus() == 4)
                    .collect(Collectors.toList());

            spasCloseSuspendProject = spasCloseSuspendProject.stream().filter(e -> e.getPhaseEndTime() != null).collect(Collectors.toList());

            //七天后需要关闭的项目
            List<ProjectInfo> finalCloseProjectInfos = new ArrayList<>();
            for (int i = 0; i < spasCloseSuspendProject.size(); i++) {
                ProjectInfo projectInfo = spasCloseSuspendProject.get(i);
                Date projectEndTime = projectInfo.getProjectEndTime();
                long startTime = projectEndTime.getTime();
                long endTime = new Date().getTime();
                long num = (endTime - startTime)/24/60/60/1000;
                if (num >= 7) {
                    finalCloseProjectInfos.add(projectInfo);
                }
            }
            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS1);
            setProjectActive(tCSOAServiceFactory, spasOngoingProject, true);
            setProjectActive(tCSOAServiceFactory, finalCloseProjectInfos, false);
        } catch (Exception e) {
            XxlJobHelper.handleFail(e.getLocalizedMessage());
            log.info("同步SPAS专案状态错误：" + e.getMessage());
            log.error(e.getMessage(),e);
        }finally {
            if (tCSOAServiceFactory != null){
                tCSOAServiceFactory.logout();
            }
        }
        log.info("==================同步专案状态结束==================");
    }

    private void setProjectActive(TCSOAServiceFactory tCSOAServiceFactory, List<ProjectInfo> projectInfos, Boolean isActive) throws Exception {
        DataManagementService dmService = tCSOAServiceFactory.getDataManagementService();
        SavedQueryService sqService = tCSOAServiceFactory.getSavedQueryService();
        ProjectLevelSecurityService plsService = tCSOAServiceFactory.getProjectLevelSecurityService();
        WorkflowService wfService = tCSOAServiceFactory.getWorkflowService();
        for(int i=0; i < projectInfos.size(); i++){
            ProjectInfo projectInfo = projectInfos.get(i);
            String projectId = "P" + projectInfo.getProjectId();

            TC_Project project = null;
            SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(sqService,
                    "__D9_Find_Project", new String[]{"project_id"}, new String[]{projectId});
            ServiceData serviceData = savedQueryResult.serviceData;
            if (serviceData.sizeOfPartialErrors() == 0) {
                ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
                if (objs.length == 0) {
                    continue;
                }
                project = (TC_Project) objs[0];
            }
            TCUtils.getProperty(dmService, project,"is_active");
            boolean projectIsActive = project.get_is_active();
            if(projectIsActive == isActive){
                continue;
            }
            log.info("close project ======>"+projectId);
            TCUtils.getProperty(dmService, project,"project_name");
            String projectName = project.get_project_name();
            TCUtils.modifyProjects(plsService, project, projectId, projectName, isActive);

            //关闭文件夹(加状态)
            Folder projectFolder = null;
            SavedQuery.ExecuteSavedQueriesResponse savedQueryResult2 = TCUtils.execute2Query(sqService, "__D9_Find_Project_Folder",
                    new String[]{"d9_SPAS_ID"},new String []{projectId});
            ServiceData serviceData2 = savedQueryResult2.serviceData;
            if (serviceData2.sizeOfPartialErrors() == 0) {
                ModelObject[] objs = savedQueryResult2.arrayOfResults[0].objects;
                if (objs.length == 0) {
                    continue;
                }
                projectFolder = (Folder) objs[0];
            }

            if(!isActive){
                log.info("move project ======>"+projectId);
                moveFolderContent(projectId,tCSOAServiceFactory,dmService,projectFolder);
                log.info("move project end ======>"+projectId);
            }
            String operation = "Append";
            if(isActive){
                operation = "Delete";
            }
            TCUtils.addStatus(wfService, new WorkspaceObject[] {projectFolder}, "D9_Release", operation);
            openOrColseProjectFolder(dmService, wfService, projectFolder, operation);
        }

    }

   private void moveFolderContent(String projectId,TCSOAServiceFactory tCSOAServiceFactory, DataManagementService dmService, Folder projectFolder) throws Exception {
       PlatformFound platformFound = synSpasDBMapper.getSpasProject(projectId.toLowerCase(Locale.ENGLISH));
       if (platformFound == null) {
           return;
       }
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
        if(!("dt".equalsIgnoreCase(bu))){
              return;
        }
        String assignedProjectId="";
        if("dell".equalsIgnoreCase(customerName)){
            assignedProjectId="Common Dell";
        }else if("Lenovo".equalsIgnoreCase(customerName)){
            assignedProjectId="Common Lenovo";
       }else if("hp".equalsIgnoreCase(customerName)){
            assignedProjectId="Common HP";
       }else{
            return;
       }

        TC_Project tcProject = null;
        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(),
               "__D9_Find_Project", new String[]{"project_id"}, new String[]{assignedProjectId});
       ServiceData serviceData = savedQueryResult.serviceData;
       if (serviceData.sizeOfPartialErrors() == 0) {
           ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
           if (objs.length > 0) {
              tcProject = (TC_Project) objs[0];
           }
       }
       if(tcProject==null){
           return;
       }
       ProjectUtil.assignedProject(tCSOAServiceFactory.getProjectLevelSecurityService(), projectFolder, tcProject);
       dmService.refreshObjects(new ModelObject[]{projectFolder});
       TCUtils.getProperty(dmService, projectFolder, TCFolderConstant.REL_CONTENTS);
       WorkspaceObject[] contents = projectFolder.get_contents();
       for (int i = 0; i < contents.length; i++) {
           WorkspaceObject content = contents[i];
           if (content instanceof Folder) {
               Folder childFolder = (Folder) content;
               assignedProj(tCSOAServiceFactory,dmService,tcProject,childFolder);
           }
       }

     savedQueryResult = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(),
               "__D9_Find_IR_ByProject", new String[]{"project_list.project_id"}, new String[]{projectId});
        serviceData = savedQueryResult.serviceData;
       if (serviceData.sizeOfPartialErrors() == 0) {
           ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
           if (objs.length > 0) {
              for(int i=0;i<objs.length;i++){
                  ProjectUtil.assignedProject(tCSOAServiceFactory.getProjectLevelSecurityService(), objs[i], tcProject);
              }
           }
       }
   }

     private void assignedProj(TCSOAServiceFactory tCSOAServiceFactory,DataManagementService dmService,TC_Project tcProject,Folder folder)throws Exception{
         dmService.refreshObjects(new ModelObject[]{folder});
         TCUtils.getProperty(dmService, folder, TCFolderConstant.REL_CONTENTS);
         WorkspaceObject[] contents = folder.get_contents();
         for (int i = 0; i < contents.length; i++) {
             WorkspaceObject content = contents[i];
             if (content instanceof Folder) {
                 Folder childFolder = (Folder) content;
                 assignedProj(tCSOAServiceFactory,dmService,tcProject,childFolder);
             }
         }

     }
    private  void openOrColseProjectFolder(DataManagementService dmService, WorkflowService wfService,
                                                 Folder projectFolder, String operation) throws Exception{
        TCUtils.getProperty(dmService, projectFolder, "contents");
        WorkspaceObject[] children = projectFolder.get_contents();
        if(children.length > 0) {
            for (int i = 0; i < children.length; i++) {
                WorkspaceObject obj = children[i];
                if (obj instanceof Folder) {
                    Folder folder = (Folder) obj;
                    TCUtils.addStatus(wfService, new WorkspaceObject[] {obj}, "D9_Release", operation);
                    openOrColseProjectFolder(dmService, wfService, folder, operation);
                }
            }
        }
    }
}
