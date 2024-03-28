package com.foxconn.plm.spas.service.impl;

import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.BUConstant;
import com.foxconn.plm.entity.constants.TCFolderConstant;
import com.foxconn.plm.entity.constants.TCPreferenceConstant;
import com.foxconn.plm.spas.bean.*;
import com.foxconn.plm.spas.mapper.SynSpasDBMapper;
import com.foxconn.plm.spas.mapper.SynTcChangeDataMapper;
import com.foxconn.plm.spas.service.ManPowerService;
import com.foxconn.plm.spas.utils.SpasTool;
import com.foxconn.plm.spas.utils.TemplateUtil;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.FolderUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Folder;
import com.teamcenter.soa.client.model.strong.WorkspaceObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 人力配置服務
 */
@Service("manpowerServiceImpl")
public class ManpowerServiceImpl implements ManPowerService {
    private static Log log = LogFactory.get();
    @Resource
    private SynTcChangeDataMapper synTcChangeDataMapper;

    @Resource
    private SynSpasDBMapper synSpasDBMapper;

    /**
     * 根據專案id 獲取有人力配置的部門
     * @param projectId
     * @return
     * @throws Exception
     */
    @Override
    public List<ManpowerInfo> getManPowers(String projectId) throws Exception {
        log.info("begin getManPowers =====");
        log.info("project id ====>"+projectId);
        projectId = projectId.toLowerCase(Locale.ENGLISH);
        projectId=projectId.replaceAll("p","");
        List<ManpowerPhaseInfo> manpowerPhaseInfos=synTcChangeDataMapper.getManPowerFunction(projectId);
        Map<String,List<String>> manpowerMap= new HashMap<>();
         for(ManpowerPhaseInfo manpowerPhaseInfo: manpowerPhaseInfos){
             String dept=manpowerPhaseInfo.getDeptName();
             List<String>  ls=manpowerMap.get(dept);
             if(ls==null){
                 ls=new ArrayList<>();
                 manpowerMap.put(dept,ls);
             }
             ls.add(manpowerPhaseInfo.getPhase());
         }
        List<ManpowerInfo>  manpowerInfos=  new ArrayList<>();
        Set<String> keys=manpowerMap.keySet();
         for(String key:keys){
             ManpowerInfo manpowerInfo= new ManpowerInfo();
             manpowerInfo.setDeptName(key);
             manpowerInfo.setPhases(manpowerMap.get(key));
             manpowerInfos.add(manpowerInfo);
         }
         log.info(JSONUtil.toJsonStr(manpowerInfos));
         log.info("end  getManPowers===");
        return manpowerInfos;
    }



    @Override
    public   List<TempInfo> updateDTFolder(String spasProjectId, boolean isSyned, Folder projectFolder, String customerName, String productLine, ManpowerActionInfo manpowerActionInfo, String bu, TCSOAServiceFactory tCSOAServiceFactory,String snapId) throws Exception {
        log.info("begin update DT Folder");
        List<TempInfo> dtTemplateInfos = TemplateUtil.getDTTemplates(tCSOAServiceFactory, customerName);

        if (dtTemplateInfos == null) {
            log.info("更新DT部门文件夹未找到模板");
            throw new Exception("更新DT部门文件夹未找到模板.");
        }
        log.info("before filter data DT");
        String logStr=JSONUtil.toJsonStr(dtTemplateInfos);
        log.info(logStr);
        log.info("length ====>"+logStr.length());

        log.info("before filter data");
        log.info(JSONUtil.toJsonStr(dtTemplateInfos));

        String[] filterFolder = TCUtils.getTCPreferences(tCSOAServiceFactory.getPreferenceManagementService(),
                TCPreferenceConstant.D9_DT_PSU_DELETE_FOLDER);
        for (int i = 0; i < filterFolder.length; i++) {
            String cusAndProStr = filterFolder[i];
            String[] cusAndProArr = cusAndProStr.split("=");
            String cus = cusAndProArr[0];
            List<String> proList = Arrays.asList(cusAndProArr[1].split(","));
            if (cus.equals(customerName) && proList.contains(productLine)){
                List<TempInfo> deptRemoves= new ArrayList<>();
                for(TempInfo dept:dtTemplateInfos){
                    if(dept.getName().equalsIgnoreCase("PSU")){
                        deptRemoves.add(dept);
                    }
                }
                dtTemplateInfos.removeAll(deptRemoves);
            }
        }
        log.info("after filter PSU data");
        log.info(JSONUtil.toJsonStr(dtTemplateInfos));

        updateFolder(spasProjectId, isSyned, bu, projectFolder, manpowerActionInfo, tCSOAServiceFactory,dtTemplateInfos,snapId);

        log.info("end update DT Folder");
        return dtTemplateInfos;
    }

    @Override
    public List<TempInfo> updateSHFolder(String spasProjectId, boolean isSyned, Folder projectFolder, String customerName, ManpowerActionInfo manpowerActionInfo, String bu, TCSOAServiceFactory tCSOAServiceFactory, String snapId) throws Exception {
        log.info("begin update SH Folder");
        List<TempInfo> dtTemplateInfos = TemplateUtil.getSHTemplates(tCSOAServiceFactory, customerName);

        if (dtTemplateInfos == null) {
            log.info("更新SH部门文件夹未找到模板");
            throw new Exception("更新SH部门文件夹未找到模板.");
        }
        log.info("before filter data SH");
        String logStr=JSONUtil.toJsonStr(dtTemplateInfos);
        log.info(logStr);
        log.info("length ====>"+logStr.length());

        log.info("before filter data");
        log.info(JSONUtil.toJsonStr(dtTemplateInfos));

        updateFolder(spasProjectId, isSyned, bu, projectFolder, manpowerActionInfo, tCSOAServiceFactory,dtTemplateInfos,snapId);

        log.info("end update SH Folder");
        return dtTemplateInfos;
    }


    @Override
    public  List<TempInfo>  updateMNTFolder(String spasProjectId,boolean isSyned, Folder projectFolder, ManpowerActionInfo manpowerActionInfo,String bu, String platformLevel, TCSOAServiceFactory tCSOAServiceFactory,String snapId) throws Exception {
        log.info("begin update MNT Folder");
        List<TempInfo> mntTemplateInfos = TemplateUtil.getMNTTemplates(tCSOAServiceFactory,platformLevel);
        if (mntTemplateInfos == null) {
            log.info("更新mnt部门文件夹未找到模板");
            throw new Exception("更新mnt部门文件夹未找到模板.");
        }
        List<String> depts=new ArrayList<>();
        for (TempInfo t:mntTemplateInfos) {
            depts.add(t.getName());
        }
        log.info("before filter data");
        String logStr=JSONUtil.toJsonStr(mntTemplateInfos);
        log.info(logStr);
        log.info("length======> "+logStr.length());

        updateFolder(spasProjectId, isSyned, bu, projectFolder, manpowerActionInfo, tCSOAServiceFactory,mntTemplateInfos,snapId);

        log.info("end upfate MNT Folder");
        return mntTemplateInfos;
    }

    @Override
    public  List<TempInfo> updatePrtFolder(String spasProjectId,boolean isSyned,Folder projectFolder, ManpowerActionInfo manpowerActionInfo, String bu ,TCSOAServiceFactory tCSOAServiceFactory,String snapId) throws Exception {
        log.info("begin update prt Folder");
        List<TempInfo> templateInfos = TemplateUtil.getPrtTemplates(tCSOAServiceFactory);
        if (templateInfos == null) {
            log.info("更新PRT部门文件夹时未找到模板.");
            throw new Exception("更新PRT部门文件夹时未找到模板.");
        }

        log.info("before filter data");
        String logStr=JSONUtil.toJsonStr(templateInfos);
        log.info(logStr);
        log.info("length =======> "+logStr.length());
        updateFolder(spasProjectId, isSyned, bu, projectFolder,  manpowerActionInfo, tCSOAServiceFactory,templateInfos,snapId);
        log.info("end update prt Folder");
        return templateInfos;
    }


    private  void  updateFolder(String spasPojectId,boolean isSyned,String bu, Folder projectFolder, ManpowerActionInfo manpowerActionInfo, TCSOAServiceFactory tCSOAServiceFactory,List<TempInfo> dtTemplateInfos,String snapId)throws Exception{
            addFolder(spasPojectId,manpowerActionInfo,tCSOAServiceFactory,dtTemplateInfos,snapId,projectFolder,isSyned,bu);
            deleteFolder(spasPojectId,manpowerActionInfo,tCSOAServiceFactory,dtTemplateInfos,snapId,projectFolder,isSyned,bu);
    }


    private  void addFolder(String spasProjectId, ManpowerActionInfo manpowerActionInfo, TCSOAServiceFactory tCSOAServiceFactory, List<TempInfo> dtTemplateInfos,String snapId, Folder projectFolder, boolean isSyned, String bu ) throws Exception {
        List<String> adds= manpowerActionInfo.getAddPhaseNames();
        String manDeptName= manpowerActionInfo.getDeptName();
        if(SpasTool.isNoManPowerDept(manDeptName)){
            return;
        }
        if(adds==null||adds.size()<=0){
            return;
        }
        TempInfo deptInfo =null;
        for(TempInfo t:dtTemplateInfos){
            if(t.getName().equalsIgnoreCase(manDeptName)){
                deptInfo=t;
                break;
            }
        }
        if(deptInfo==null){
            log.info("模板中没有此部门,无需处理======》"+manDeptName);
            return;
        }
        boolean isNewDeptFolder=false;
        Folder deptFolder = FolderUtil.findChildFolder(projectFolder, deptInfo.getName(),tCSOAServiceFactory.getDataManagementService());
        if(deptFolder==null) {
            deptFolder =FolderUtil.createFolder(tCSOAServiceFactory.getDataManagementService(),projectFolder, TCFolderConstant.TYPE_D9_FUNCTIONFOLDER, manDeptName,deptInfo.getDescr());
            SpasActionHis his=new SpasActionHis();
            his.setAction("A");
            his.setProjectId(spasProjectId);
            his.setDept(deptInfo.getName());
            his.setSnapId(snapId);
            his.setResource("tc");
            synSpasDBMapper.addSpasActionHis(his);
            isNewDeptFolder=true;
        }
        List<TempInfo> phaseInfos = deptInfo.getChildren();
        for(String manPhase:adds){
            TempInfo phaseInfo=null;
            for(TempInfo t:phaseInfos){
                if(t.getName().toLowerCase(Locale.ENGLISH).startsWith(manPhase.toLowerCase(Locale.ENGLISH))){
                    phaseInfo=t;
                }
            }
            if(phaseInfo==null){
                continue;
            }
            if (FolderUtil.isExistChildFolder(deptFolder, phaseInfo.getName(), tCSOAServiceFactory.getDataManagementService())) {
                continue;
            }
            if (FolderUtil.isExistPhaseFolder(deptFolder, phaseInfo.getName(), tCSOAServiceFactory.getDataManagementService())) {
                continue;
            }
            if(!SpasTool.isNoAcountDept(tCSOAServiceFactory,bu,deptInfo.getName())||!isSyned) {
                Folder phaseFolder = FolderUtil.createFolder(tCSOAServiceFactory.getDataManagementService(), deptFolder, TCFolderConstant.TYPE_D9_PHASEFOLDER, phaseInfo.getName(),phaseInfo.getDescr());
                SpasActionHis his=new SpasActionHis();
                his.setAction("A");
                his.setProjectId(spasProjectId);
                his.setDept(deptInfo.getName());
                his.setPhase(phaseInfo.getName());
                his.setResource("tc");
                his.setSnapId(snapId);
                synSpasDBMapper.addSpasActionHis(his);
                List<TempInfo> archiveInfos = phaseInfo.getChildren();
                for (TempInfo archiveInfo : archiveInfos) {
                    FolderUtil.createFolder(tCSOAServiceFactory.getDataManagementService(), phaseFolder, TCFolderConstant.TYPE_D9_ARCHIVE, archiveInfo.getName(), archiveInfo.getDescr());
                    SpasActionHis his2=new SpasActionHis();
                    his2.setAction("A");
                    his2.setProjectId(spasProjectId);
                    his2.setDept(deptInfo.getName());
                    his2.setPhase(phaseInfo.getName());
                    his2.setArchive(archiveInfo.getName());
                    his2.setResource("tc");
                    his2.setSnapId(snapId);
                    synSpasDBMapper.addSpasActionHis(his2);
                }
                addExtendsFolder(tCSOAServiceFactory,deptInfo.getName(),phaseFolder, bu,projectFolder);

            }else{
                List<FolderInfo> dbDeptFolders=synSpasDBMapper.getDeptFolders(spasProjectId);
                if(dbDeptFolders==null||dbDeptFolders.size()<=0){
                    return;
                }
                int fl=0;
                for (FolderInfo dbDeptFolderInfo:dbDeptFolders) {
                    if (deptInfo.getName().equalsIgnoreCase(dbDeptFolderInfo.getName())) {
                        fl=1;
                        int f=0;
                        List<FolderInfo> dbPhaseFolders = synSpasDBMapper.getChildFolders(dbDeptFolderInfo.getId());
                        for(FolderInfo dbPhaseFolder:dbPhaseFolders ){
                            if(dbPhaseFolder.getName().toLowerCase(Locale.ENGLISH).startsWith(manPhase.toLowerCase(Locale.ENGLISH).substring(0,2))){
                                f=1;
                            }
                        }
                        if(f==0){
                            addPhaseAndAchive(snapId,spasProjectId,dbDeptFolderInfo.getId(),phaseInfo,deptInfo.getName());
                        }
                    }
                }
                if(fl==0){
                    deptFolder=FolderUtil.findChildFolder(projectFolder,deptInfo.getName(),tCSOAServiceFactory.getDataManagementService());
                    if(deptFolder==null){
                        log.info("hdfs創建部門文件夾失敗，在TC中找不到部門文件夾 "+spasProjectId+" "+deptInfo.getName());
                    }else {
                        Integer deptFolderId= addDept(snapId,spasProjectId, dbDeptFolders.get(0).getParentFolderId(), deptInfo,deptFolder.getUid());
                        addPhaseAndAchive(snapId,spasProjectId,deptFolderId,phaseInfo,deptInfo.getName());
                    }
                }
            }
        }

        if(isNewDeptFolder) {
            if(!SpasTool.isNoAcountDept(tCSOAServiceFactory,bu,deptInfo.getName()) || !isSyned){
                for (TempInfo phaseInfo : phaseInfos) {
                    if (phaseInfo.getChildren().get(0).getName().equalsIgnoreCase(SynSpasConstants.NO_PHASE)) {
                        FolderUtil.createFolder(tCSOAServiceFactory.getDataManagementService(), deptFolder, TCFolderConstant.TYPE_D9_ARCHIVE, phaseInfo.getName(),phaseInfo.getDescr());
                        SpasActionHis his=new SpasActionHis();
                        his.setAction("A");
                        his.setProjectId(spasProjectId);
                        his.setDept(deptInfo.getName());
                        his.setPhase(SynSpasConstants.NO_PHASE);
                        his.setArchive(phaseInfo.getName());
                        his.setResource("tc");
                        his.setSnapId(snapId);
                        synSpasDBMapper.addSpasActionHis(his);
                    }
                }
            }else {
                Integer deptFolderId =null;
                for (TempInfo phaseInfo : phaseInfos) {
                    if (phaseInfo.getChildren().get(0).getName().equalsIgnoreCase(SynSpasConstants.NO_PHASE)) {
                        if(deptFolderId==null) {
                            List<FolderInfo> dbDeptFolders = synSpasDBMapper.getDeptFolders(spasProjectId);
                            deptFolderId = addDept(snapId, spasProjectId, dbDeptFolders.get(0).getParentFolderId(), deptInfo, deptFolder.getUid());
                        }
                        addPhaseAndAchive(snapId, spasProjectId, deptFolderId, phaseInfo,deptInfo.getName());
                    }
                }
            }
        }
    }

    private void deleteFolder(String spasProjectId,ManpowerActionInfo manpowerActionInfo,TCSOAServiceFactory tCSOAServiceFactory,List<TempInfo> dtTemplateInfos,String snapId,Folder projectFolder,boolean isSyned,String bu) throws Exception {
        List<String> removes= manpowerActionInfo.getDeletePhaseNames();
        String manDeptName= manpowerActionInfo.getDeptName();
        if(SpasTool.isNoManPowerDept(manDeptName)){
            return;
        }
        if(removes==null||removes.size()<=0){
            removes=new ArrayList<>();
        }

        TempInfo deptInfo =null;
        for(TempInfo t:dtTemplateInfos){
            if(t.getName().equalsIgnoreCase(manDeptName)){
                deptInfo=t;
                break;
            }
        }
        if(deptInfo==null){
            log.info("模板中没有此部门,无需处理======》"+manDeptName);
            return;
        }
        Folder  deptFolder = FolderUtil.findChildFolder(projectFolder, deptInfo.getName(),tCSOAServiceFactory.getDataManagementService());
        if(deptFolder==null) {
            return;
        }

        if(!SpasTool.isNoAcountDept(tCSOAServiceFactory,bu,deptInfo.getName())||!isSyned) {
            for (String manPhase : removes) {
                tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{deptFolder});
                TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), deptFolder, TCFolderConstant.REL_CONTENTS);
                WorkspaceObject[] phaseContents = deptFolder.get_contents();
                for (int k = 0; k < phaseContents.length; k++) {
                    WorkspaceObject phaseContent = phaseContents[k];
                    if (!(phaseContent instanceof Folder)) {
                        continue;
                    }
                    Folder phaseFolder = (Folder) phaseContent;
                    TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), phaseFolder, TCFolderConstant.PROPERTY_OBJECT_NAME);
                    TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), phaseFolder, TCFolderConstant.PROPERTY_OBJECT_TYPE);
                    String phaseForlderName = phaseFolder.get_object_name();
                    String objectType = phaseFolder.get_object_type();
                    if (!(TCFolderConstant.TYPE_D9_PHASEFOLDER.equalsIgnoreCase(objectType))) {
                        continue;
                    }
                    if (!(phaseForlderName.toLowerCase(Locale.ENGLISH).startsWith(manPhase.toLowerCase(Locale.ENGLISH)))) {
                        continue;
                    }
                    List<WorkspaceObject> wks = new ArrayList<>();
                    FolderUtil.isEmmptyFolder(tCSOAServiceFactory.getDataManagementService(), phaseFolder, wks);
                    if (wks == null || wks.size() <= 0) {
                        TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), phaseFolder, TCFolderConstant.PROPERTY_OBJECT_TYPE);
                        String folderType = phaseFolder.get_object_type();
                        if (TCFolderConstant.TYPE_D9_PHASEFOLDER.equalsIgnoreCase(folderType)) {
                            //刪除防呆
                            Integer mp=synSpasDBMapper.getManpower(spasProjectId.replaceAll("p",""),deptInfo.getName(),manPhase.toUpperCase(Locale.ENGLISH));
                            if(mp!=null&&mp.intValue()>0){
                                continue;
                            }
                            log.info("从TC中删除阶段文件夹====>" + deptInfo.getName() + " project " + projectFolder.getUid() + " phase " + phaseFolder.getUid());
                            String desc="Del_"+spasProjectId+"_"+deptInfo.getName()+"_"+phaseForlderName+"_"+snapId;
                            FolderUtil.deleteFolderSoft(tCSOAServiceFactory.getDataManagementService(), tCSOAServiceFactory.getSessionService(), deptFolder, phaseFolder,desc);

                            SpasActionHis his=new SpasActionHis();
                            his.setAction("D");
                            his.setProjectId(spasProjectId);
                            his.setDept(deptInfo.getName());
                            his.setPhase(phaseForlderName);
                            his.setSnapId(snapId);
                            his.setResource("tc");
                            synSpasDBMapper.addSpasActionHis(his);
                        }
                    }
                }
            }

            tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{deptFolder});
            TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), deptFolder, TCFolderConstant.REL_CONTENTS);
            WorkspaceObject[] phaseContents = deptFolder.get_contents();
            if (phaseContents == null || phaseContents.length <= 0) {
                TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), deptFolder, TCFolderConstant.PROPERTY_OBJECT_TYPE);
                String folderType = deptFolder.get_object_type();
                TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), deptFolder, TCFolderConstant.PROPERTY_OBJECT_NAME);
                String deptForlderName = deptFolder.get_object_name();
                if (TCFolderConstant.TYPE_D9_FUNCTIONFOLDER.equalsIgnoreCase(folderType)) {
                    log.info("从TC中删除部门文件夹====>" + deptForlderName + " project " + projectFolder.getUid());
                    String desc="Del_"+spasProjectId+"_"+deptForlderName+"_"+snapId;
                    FolderUtil.deleteFolderSoft(tCSOAServiceFactory.getDataManagementService(), tCSOAServiceFactory.getSessionService(), projectFolder, deptFolder,desc);
                    SpasActionHis his=new SpasActionHis();
                    his.setAction("D");
                    his.setProjectId(spasProjectId);
                    his.setDept(deptInfo.getName());
                    his.setSnapId(snapId);
                    his.setResource("tc");
                    synSpasDBMapper.addSpasActionHis(his);
                }
            }
        }else{
            List<FolderInfo> dbDeptFolders=synSpasDBMapper.getDeptFolders(spasProjectId);
            if(dbDeptFolders==null||dbDeptFolders.size()<=0){
                return;
            }
            FolderInfo dbDeptFolderInfo=null;
            for (FolderInfo dbDeptFolder:dbDeptFolders) {
                if(dbDeptFolder.getName().equalsIgnoreCase(deptInfo.getName())){
                    dbDeptFolderInfo =dbDeptFolder;
                    break;
                }
            }
            if(dbDeptFolderInfo==null){
                tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{deptFolder});
                TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), deptFolder, TCFolderConstant.REL_CONTENTS);
                WorkspaceObject[] phaseContents = deptFolder.get_contents();
                if (phaseContents == null || phaseContents.length <= 0) {
                    TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), deptFolder, TCFolderConstant.PROPERTY_OBJECT_TYPE);
                    String folderType = deptFolder.get_object_type();
                    TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), deptFolder, TCFolderConstant.PROPERTY_OBJECT_NAME);
                    String deptForlderName = deptFolder.get_object_name();
                    if (TCFolderConstant.TYPE_D9_FUNCTIONFOLDER.equalsIgnoreCase(folderType)) {
                        log.info("从TC中删除部门文件夹====>" + deptForlderName + " project " + projectFolder.getUid());
                        String desc="Del_"+spasProjectId+"_"+deptForlderName+"_"+snapId;
                        FolderUtil.deleteFolderSoft(tCSOAServiceFactory.getDataManagementService(), tCSOAServiceFactory.getSessionService(), projectFolder, deptFolder,desc);
                        SpasActionHis his=new SpasActionHis();
                        his.setAction("D");
                        his.setProjectId(spasProjectId);
                        his.setDept(deptInfo.getName());
                        his.setSnapId(snapId);
                        his.setResource("tc");
                        synSpasDBMapper.addSpasActionHis(his);
                    }
                }
                return;
            }

            Integer deptForldId = dbDeptFolderInfo.getId();
            List<FolderInfo> dbPhaseFolders = synSpasDBMapper.getChildFolders(deptForldId);
            String deptForlderName = dbDeptFolderInfo.getName();
            //這幾個部門沒有階段文件夾
            if(SpasTool.isNoPhaseDept(deptForlderName)){
                return;
            }
            for (FolderInfo dbPhaseFolderInfo : dbPhaseFolders) {
                int fl = 0;
                for (String  manPhase : removes) {
                    if (dbPhaseFolderInfo.getName().toLowerCase(Locale.ENGLISH).startsWith(manPhase.toLowerCase(Locale.ENGLISH))) {
                        fl = 1;
                    }
                }
                if (fl == 1) {
                    //刪除防呆
                    log.info("刪除防呆 ===> "+spasProjectId.replaceAll("p","")+" "+deptForlderName+" "+ dbPhaseFolderInfo.getName().substring(0,2));
                    Integer mp=synSpasDBMapper.getManpower(spasProjectId.replaceAll("p",""),deptForlderName,dbPhaseFolderInfo.getName().substring(0,2));
                    if(mp!=null&&mp.intValue()>0){
                        continue;
                    }
                    deleteForlderFromHdfs(spasProjectId,dbPhaseFolderInfo.getParentFolderId(),dbPhaseFolderInfo.getId(), dbPhaseFolderInfo.getStruId());
                    SpasActionHis his=new SpasActionHis();
                    his.setAction("D");
                    his.setProjectId(spasProjectId);
                    his.setDept(deptInfo.getName());
                    his.setPhase(dbPhaseFolderInfo.getName());
                    his.setSnapId(snapId);
                    his.setResource("hdfs");
                    synSpasDBMapper.addSpasActionHis(his);
                }
            }

            List<FolderInfo> phaseFolders= synSpasDBMapper.getChildFolders(deptForldId);
            if(phaseFolders==null||phaseFolders.size()<=0){
                // 從TC中刪除部門文件夾，不要從外挂系統刪

                deleteFolderFromTC(spasProjectId,projectFolder,deptForlderName,tCSOAServiceFactory,snapId);
                SpasActionHis his=new SpasActionHis();
                his.setAction("D");
                his.setProjectId(spasProjectId);
                his.setDept(deptInfo.getName());
                his.setSnapId(snapId);
                his.setResource("tc");
                synSpasDBMapper.addSpasActionHis(his);
            }
        }

    }

    private  Integer addDept(String snapId,String spasProjectId,Integer projectFolderId,TempInfo deptInfo,String deptUid)throws Exception{
        FolderInfo ff=new FolderInfo();
        ff.setName(deptInfo.getName());
        ff.setDescr(deptInfo.getDescr());
        ff.setUid(deptUid);
        synSpasDBMapper.insertFolder(ff);
        Integer  deptFolderId= ff.getId();
        ff.setParentFolderId(projectFolderId);
        synSpasDBMapper.insertFolderStruct(ff);
        log.info("从hdfs创建部门文件夹====>"+deptInfo.getName()+" project "+spasProjectId+" depet"+deptFolderId);

        SpasActionHis his=new SpasActionHis();
        his.setAction("A");
        his.setProjectId(spasProjectId);
        his.setDept(deptInfo.getName());
        his.setResource("hdfs");
        his.setSnapId(snapId);
        synSpasDBMapper.addSpasActionHis(his);
        return deptFolderId;
    }

    private  void addPhaseAndAchive(String snapId ,String projectId, Integer deptFolderId,TempInfo phaseInfo,String deptName) throws Exception {
        FolderInfo f=new FolderInfo();
        f.setName(phaseInfo.getName());
        f.setDescr(phaseInfo.getDescr());
        synSpasDBMapper.insertFolder(f);
        Integer  phaseFolderId= f.getId();
        f.setParentFolderId(deptFolderId);
        synSpasDBMapper.insertFolderStruct(f);

        SpasActionHis his=new SpasActionHis();
        his.setAction("A");
        his.setProjectId(projectId);
        his.setDept(deptName);
        his.setPhase(phaseInfo.getName());
        his.setResource("hdfs");
        his.setSnapId(snapId);
        synSpasDBMapper.addSpasActionHis(his);

        log.info("从hdfs创建部门阶段文件夹====>"+phaseInfo.getDescr()+" project "+projectId+" depet"+deptFolderId);
        List<TempInfo> archiveInfos=  phaseInfo.getChildren();
        for(TempInfo archive:archiveInfos){
            FolderInfo f0=new FolderInfo();
            f0.setName(archive.getName());
            f0.setDescr(archive.getDescr());
            if(archive.getName().equalsIgnoreCase(SynSpasConstants.NO_PHASE)){
                continue;
            }
            synSpasDBMapper.insertFolder(f0);
            f0.setParentFolderId(phaseFolderId);
            synSpasDBMapper.insertFolderStruct(f0);

            SpasActionHis his2=new SpasActionHis();
            his2.setAction("A");
            his2.setProjectId(projectId);
            his2.setDept(deptName);
            his2.setPhase(phaseInfo.getName());
            his2.setArchive(archive.getName());
            his2.setResource("hdfs");
            his2.setSnapId(snapId);
            synSpasDBMapper.addSpasActionHis(his2);

            log.info("从hdfs创建阶段归档文件夹=====>"+archive.getName()+" project "+projectId+" depet"+deptFolderId+" phase "+archive.getName());
        }
    }


    /**
     * 從外挂系統中刪除階段文件夾
     * @param folderId
     * @param struId
     * @throws Exception
     */
    private void deleteForlderFromHdfs(String projectId,Integer parentId,Integer folderId, Integer struId) throws Exception {
        List<Integer>   docs=new ArrayList<>();
        findDocuments(folderId,docs);
        if(docs.size()>0){
            return;
        }
        synSpasDBMapper.deleteFolder(folderId);
        synSpasDBMapper.deleteStru(struId);
        log.info("从hdfs中删除了阶段文件夹=======>phase Id "+folderId+"  depet id =====>"+parentId+" 专案"+projectId);
    }


    private  void findDocuments(Integer folderId,List<Integer> docs)throws Exception {
        Integer cnt= synSpasDBMapper.getDocumentCnt(folderId);
        if(cnt.intValue()>0){
            docs.add(cnt);
        }
        List<FolderInfo> childs=synSpasDBMapper.getChildFolders(folderId);
        for(FolderInfo f:childs){
            findDocuments(f.getId(),docs);
        }
    }

    private void deleteFolderFromTC(String spasProjectId,Folder projectFolder,String folderName,TCSOAServiceFactory tCSOAServiceFactory,String snapId) throws Exception {
        tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{projectFolder});
        TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), projectFolder, TCFolderConstant.REL_CONTENTS);
        WorkspaceObject[] contents = projectFolder.get_contents();
        //刪除部門文件夾
        for (int i = 0; i < contents.length; i++) {
            WorkspaceObject content = contents[i];
            if (content instanceof Folder) {
                Folder deptFolder = (Folder) content;
                TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), deptFolder, TCFolderConstant.PROPERTY_OBJECT_NAME);
                String deptForlderName = deptFolder.get_object_name();
                if (folderName.equalsIgnoreCase(deptForlderName)) {
                    TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), deptFolder, TCFolderConstant.PROPERTY_OBJECT_TYPE);
                    String folderType = deptFolder.get_object_type();
                    if (TCFolderConstant.TYPE_D9_FUNCTIONFOLDER.equalsIgnoreCase(folderType)) {
                        List<WorkspaceObject> wks = new ArrayList<>();
                        FolderUtil.isEmmptyFolder(tCSOAServiceFactory.getDataManagementService(), deptFolder, wks);
                        if (wks == null || wks.size() <= 0) {
                            String desc="Del_"+spasProjectId+"_"+folderName+"_"+snapId;
                            FolderUtil.deleteFolderSoft(tCSOAServiceFactory.getDataManagementService(),tCSOAServiceFactory.getSessionService(),projectFolder,deptFolder,desc);
                            log.info("从TC中删除了没TC账号的部门=======>"+folderName+"  project =====>"+spasProjectId);
                        }
                    }
                }
            }
        }
    }


    /**
     * 過濾掉沒有人力配置的部門
     * @param deptInfos
     * @param manpowerInfos
     */
    private   void filterDeptAndPhase(List<TempInfo> deptInfos, List<ManpowerInfo> manpowerInfos){
        List<TempInfo> removeDepts=new ArrayList<>();
        for(TempInfo dept:deptInfos){
            if(SpasTool.isNoManPowerDept(dept.getName())){
                continue;
            }
            int f=0;
            for(ManpowerInfo manpowerInfo:manpowerInfos){
                if(dept.getName().equalsIgnoreCase(manpowerInfo.getDeptName())){
                    f=1;
                    break;
                }
            }
            if(f==0){
                removeDepts.add(dept);
            }
        }
        deptInfos.removeAll(removeDepts);

        removeDepts=new ArrayList<>();
        for(TempInfo deptInfo:deptInfos){
            if(SpasTool.isNoManPowerDept(deptInfo.getName())){
                continue;
            }
            List<TempInfo> newPhaseInfos=new ArrayList<>();
            List<TempInfo> phaseInfos= deptInfo.getChildren();

            for(TempInfo phaseInfo:phaseInfos){
                if(phaseInfo.getChildren().get(0).getName().equalsIgnoreCase(SynSpasConstants.NO_PHASE)){
                    newPhaseInfos.add(phaseInfo);
                    continue;
                }
                for(ManpowerInfo manpowerInfo:manpowerInfos){
                    if(deptInfo.getName().equalsIgnoreCase(manpowerInfo.getDeptName())){
                        List<String> phs=manpowerInfo.getPhases();
                        for(String p:phs){
                            if(phaseInfo.getName().toLowerCase(Locale.ENGLISH).startsWith(p.toLowerCase(Locale.ENGLISH))){
                                newPhaseInfos.add(phaseInfo);
                            }
                        }
                    }
                }
            }
            if(newPhaseInfos.size()>0) {
                deptInfo.setChildren(newPhaseInfos);
            }else{
                removeDepts.add(deptInfo);
            }
        }
        deptInfos.removeAll(removeDepts);
    }



    public void addExtendsFolder(TCSOAServiceFactory tCSOAServiceFactory,String deptName,Folder phaseFolder, String bu,Folder projectFolder) throws Exception {
        log.info("begin update extends folder");
        log.info(deptName+" "+ bu);
        if(!(BUConstant.MNT.equals(bu))){
            log.info("end update extends folder");
            return;
        }
        if((!(deptName.equalsIgnoreCase("psu")))&&(!(deptName.equalsIgnoreCase("layout")))&&(!(deptName.equalsIgnoreCase("ee")))){
            log.info("end update extends folder");
            return;
        }
        Map<String,List<String>>  flsMap= findFolder(tCSOAServiceFactory.getDataManagementService(),projectFolder,deptName);
        if(flsMap==null||flsMap.size()<=0){
            log.info("未找到机种文件夹");
            log.info("end update extends folder");
            return;
        }
        Set<String> keys=flsMap.keySet();
        for(String key:keys){
            log.info("create folder "+key);
             Folder f=findExistFolder(tCSOAServiceFactory.getDataManagementService(),phaseFolder,key);
             if(f==null) {
                 f = FolderUtil.createFolder(tCSOAServiceFactory.getDataManagementService(), phaseFolder, "Folder", key, key);
             }
            List<String> ls= flsMap.get(key);
           for(String s:ls){
               log.info("create subfolder "+s);
               Folder f2=findExistFolder(tCSOAServiceFactory.getDataManagementService(),f,s);
               if(f2==null) {
                   FolderUtil.createFolder(tCSOAServiceFactory.getDataManagementService(), f, "Folder", s, s);
               }
           }
        }
        log.info("end update extends folder");
    }



    private Folder  findExistFolder(DataManagementService dataManagementService,Folder parentFolder,String childFolderName) throws Exception {
            dataManagementService.refreshObjects(new ModelObject[]{parentFolder});
            TCUtils.getProperty(dataManagementService, parentFolder, "contents");
            WorkspaceObject[] ws22 = parentFolder.get_contents();
            for (WorkspaceObject w2 : ws22) {
                if (!(w2 instanceof Folder)) {
                    continue;
                }
                Folder ff2 = (Folder) w2;
                TCUtils.getProperty(dataManagementService, ff2, TCFolderConstant.PROPERTY_OBJECT_TYPE);
                String folderType2 = ff2.get_object_type();
                TCUtils.getProperty(dataManagementService, ff2, "object_name");
                String s2 = ff2.get_object_name();
                if (folderType2.equalsIgnoreCase("Folder")) {
                    if(s2.equalsIgnoreCase(childFolderName)){
                        return  ff2;
                    }
                }
            }
            return  null;
    }

    private  Map<String,List<String>>  findFolder(DataManagementService dataManagementService, Folder f,String deptName) {
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
            Folder slefPnFolder = null;
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

                if ("自編物料協同工作區".equalsIgnoreCase(s)) {
                    slefPnFolder= ff;
                    break;
                }

            }
           if(slefPnFolder==null){
               return null;
           }
         //p2042
            Map<String,List<String>> fsMap=new HashMap<String,List<String>>();
            dataManagementService.refreshObjects(new ModelObject[]{slefPnFolder});
            TCUtils.getProperty(dataManagementService, slefPnFolder, "contents");
            WorkspaceObject[] ws2 = slefPnFolder.get_contents();
            for (WorkspaceObject w : ws2) {
                if (!(w instanceof Folder)) {
                    continue;
                }
                Folder ff = (Folder) w;
                TCUtils.getProperty(dataManagementService, ff, TCFolderConstant.PROPERTY_OBJECT_TYPE);
                String folderType = ff.get_object_type();
                TCUtils.getProperty(dataManagementService, ff, "object_name");
                String s = ff.get_object_name();
                if (folderType.equalsIgnoreCase("Folder")) {
                    dataManagementService.refreshObjects(new ModelObject[]{ff});
                    TCUtils.getProperty(dataManagementService, ff, "contents");
                    WorkspaceObject[] ws22 = ff.get_contents();
                    for (WorkspaceObject w2 : ws22) {
                        if (!(w2 instanceof Folder)) {
                            continue;
                        }
                        Folder ff2 = (Folder) w2;
                        TCUtils.getProperty(dataManagementService, ff2, TCFolderConstant.PROPERTY_OBJECT_TYPE);
                        String folderType2 = ff2.get_object_type();
                        TCUtils.getProperty(dataManagementService, ff2, "object_name");
                        String s2 = ff2.get_object_name();
                        if (folderType2.equalsIgnoreCase("Folder")) {
                            int flag=0;
                            if("Power&PI&INV&LED CONV BD".equalsIgnoreCase(s2)
                               ||"LED driver BD".equalsIgnoreCase(s2)
                               ||"LED Lighting BD".equalsIgnoreCase(s2)
                               ||"LED Lighting  Driver BD".equalsIgnoreCase(s2)
                               ||"DC JACK".equalsIgnoreCase(s2)
                               ||"Safety POWER BD".equalsIgnoreCase(s2)

                            ){
                                flag=1;
                            }
                            if(deptName.equalsIgnoreCase("ee")){
                              if(flag==0){
                                  List<String> fs = fsMap.get(s);
                                  if (fs == null) {
                                      fs = new ArrayList<>();
                                      fsMap.put(s, fs);
                                  }
                                  fs.add(s2);
                              }
                            }else if(deptName.equalsIgnoreCase("psu")){
                                if(flag==1){
                                    List<String> fs = fsMap.get(s);
                                    if (fs == null) {
                                        fs = new ArrayList<>();
                                        fsMap.put(s, fs);
                                    }
                                    fs.add(s2);
                                }
                            }else {
                                List<String> fs = fsMap.get(s);
                                if (fs == null) {
                                    fs = new ArrayList<>();
                                    fsMap.put(s, fs);
                                }
                                fs.add(s2);
                            }
                            log.info(s +"=====>"+s2);
                        }
                    }
                }
            }
           return fsMap;

        } catch (Exception e) {
        }
        return null;
    }


}
