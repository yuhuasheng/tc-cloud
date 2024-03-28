package com.foxconn.plm.spas.service.impl;

import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCFolderConstant;
import com.foxconn.plm.entity.constants.TCPreferenceConstant;
import com.foxconn.plm.spas.bean.ManpowerInfo;
import com.foxconn.plm.spas.bean.SynSpasConstants;
import com.foxconn.plm.spas.bean.TempInfo;
import com.foxconn.plm.spas.service.SpasService;
import com.foxconn.plm.spas.utils.SpasTool;
import com.foxconn.plm.spas.utils.TemplateUtil;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.FolderUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.soa.client.model.strong.Folder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service("spasServiceImpl")
public class SpasServiceImpl  implements SpasService {
    private static Log log = LogFactory.get();

    /**
     * 創建DT 部門文件夾
     * @param projectFolder  專案文件夾
     * @param customerName   客戶名稱
     * @param productLine     產品綫
     * @param manpowerInfos   人力信息
     * @param phaseList       專案階段
     * @param tCSOAServiceFactory
     * @throws Exception
     */
    @Override
    public  void createDTFolder( Folder projectFolder, String customerName, String productLine, List<ManpowerInfo> manpowerInfos, List<String> phaseList, TCSOAServiceFactory tCSOAServiceFactory) throws Exception {
        log.info("begin create DT Folder");
        List<TempInfo> dtTemplateInfos = TemplateUtil.getDTTemplates(tCSOAServiceFactory, customerName);

        if (dtTemplateInfos == null) {
            log.info("创建DT部门文件夹未找到模板");
            throw new Exception("创建DT部门文件夹未找到模板.");
        }
        log.info("before filter data DT");
        String logStr= JSONUtil.toJsonStr(dtTemplateInfos);
        log.info(logStr);
        log.info("length ====>"+logStr.length());

        for(TempInfo t:dtTemplateInfos){
            List<TempInfo> phaseinfos= t.getChildren();
            List<TempInfo> removes= new ArrayList<>();
            if(phaseinfos==null){
                System.out.println("");
            }
            for(TempInfo p:phaseinfos){
                String phase=p.getName();
                int f=0;
                for(String s:phaseList){
                    if(phase.toLowerCase(Locale.ENGLISH).startsWith(s.toLowerCase(Locale.ENGLISH))){
                        f=1;
                        break;
                    }
                }
                if(!(p.getChildren().get(0).getName().equalsIgnoreCase(SynSpasConstants.NO_PHASE))&&f==0){
                    removes.add(p);
                }
            }
            t.getChildren().removeAll(removes);
        }

        log.info("before filter data");
        log.info(JSONUtil.toJsonStr(dtTemplateInfos));
        log.info("manpowerInfos ====>");
        log.info(JSONUtil.toJsonStr(manpowerInfos));
        filterDeptAndPhase(dtTemplateInfos,manpowerInfos);
        log.info("after filter data ");
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

        createPhaseArchiveFolderInTC(dtTemplateInfos,projectFolder,tCSOAServiceFactory);

        log.info("end create DT Folder");
    }


    /**
     * 創建MNT部門文件夾
     * @param projectFolder
     * @param manpowerInfos
     * @param platformLevel
     * @param tCSOAServiceFactory
     * @throws Exception
     */
    @Override
    public  void createMNTFolder(Folder projectFolder, List<ManpowerInfo> manpowerInfos, String platformLevel, TCSOAServiceFactory tCSOAServiceFactory) throws Exception {
        log.info("begin  createMNTFolder");
        List<TempInfo> mntTemplateInfos = TemplateUtil.getMNTTemplates(tCSOAServiceFactory,platformLevel);
        if (mntTemplateInfos == null) {
            log.info("创建DT部门文件夹未找到模板");
            throw new Exception("创建DT部门文件夹未找到模板.");
        }
        log.info("before filter data");
        String logStr=JSONUtil.toJsonStr(mntTemplateInfos);
        log.info(logStr);
        log.info("length======> "+logStr.length());
        filterDeptAndPhase(mntTemplateInfos,manpowerInfos);
        log.info("after filter data");
        log.info(JSONUtil.toJsonStr(mntTemplateInfos));
        createPhaseArchiveFolderInTC(mntTemplateInfos,projectFolder,tCSOAServiceFactory);
        log.info("end createMNTFolder");
    }


    /**
     * 創建PRT 部門文件夾
     * @param projectFolder
     * @param manpowerInfos
     * @param tCSOAServiceFactory
     * @throws Exception
     */
    @Override
    public  void createPrtFolder(Folder projectFolder, List<ManpowerInfo> manpowerInfos, TCSOAServiceFactory tCSOAServiceFactory) throws Exception {
        log.info("begin  createPrtFolder");
        List<TempInfo> templateInfos = TemplateUtil.getPrtTemplates(tCSOAServiceFactory);
        if (templateInfos == null) {
            log.info("创建PRT部门文件夹时未找到模板.");
            throw new Exception("创建PRT部门文件夹时未找到模板.");
        }
        log.info("before filter data");
        String logStr=JSONUtil.toJsonStr(templateInfos);
        log.info(logStr);
        log.info("length =======> "+logStr.length());
        filterDeptAndPhase(templateInfos,manpowerInfos);
        log.info("after filter data");
        log.info(JSONUtil.toJsonStr(templateInfos));
        createPhaseArchiveFolderInTC(templateInfos, projectFolder, tCSOAServiceFactory);

        log.info("end  createPrtFolder");

    }

    @Override
    public void createSHFolder(Folder projectFolder, List<ManpowerInfo> manpowerInfos, TCSOAServiceFactory tCSOAServiceFactory,String customerName) throws Exception {
        log.info("begin  createSHFolder");
        List<TempInfo> templateInfos = TemplateUtil.getSHTemplates(tCSOAServiceFactory,customerName);
        if (templateInfos == null) {
            log.info("创建SH部门文件夹时未找到模板.");
            throw new Exception("创建SH部门文件夹时未找到模板.");
        }
        log.info("before filter data");
        String logStr=JSONUtil.toJsonStr(templateInfos);
        log.info(logStr);
        log.info("length =======> "+logStr.length());
        filterDeptAndPhase(templateInfos,manpowerInfos);
        log.info("after filter data");
        log.info(JSONUtil.toJsonStr(templateInfos));
        createPhaseArchiveFolderInTC(templateInfos, projectFolder, tCSOAServiceFactory);

        log.info("end  createSHFolder");

    }


    /**
     * 過濾掉沒有人力配置的部門
     * @param deptInfos
     * @param manpowerInfos
     */
    private static  void filterDeptAndPhase(List<TempInfo> deptInfos, List<ManpowerInfo> manpowerInfos){
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


    private static  void createPhaseArchiveFolderInTC(List<TempInfo> templateInfos, Folder projectFolder, TCSOAServiceFactory tCSOAServiceFactory) throws Exception {

        for (TempInfo deptInfo : templateInfos) {
            List<TempInfo> phaseInfos = deptInfo.getChildren();
            if(phaseInfos==null||phaseInfos.size()<=0){
                continue;
            }
            String depart = deptInfo.getName();
            Folder departFolder = FolderUtil.createFolder(tCSOAServiceFactory.getDataManagementService(),projectFolder, TCFolderConstant.TYPE_D9_FUNCTIONFOLDER, depart,deptInfo.getDescr());

            for (TempInfo phaseInfo :phaseInfos) {
                if (FolderUtil.isExistChildFolder(departFolder, phaseInfo.getName(), tCSOAServiceFactory.getDataManagementService())) {
                    continue;
                }
                if(phaseInfo.getChildren().get(0).getName().equalsIgnoreCase(SynSpasConstants.NO_PHASE)){
                    FolderUtil.createFolder(tCSOAServiceFactory.getDataManagementService(), departFolder, TCFolderConstant.TYPE_D9_ARCHIVE, phaseInfo.getName(),phaseInfo.getDescr());
                }else {
                    if (FolderUtil.isExistPhaseFolder(departFolder, phaseInfo.getName(), tCSOAServiceFactory.getDataManagementService())) {
                        continue;
                    }
                    Folder phaseFolder = FolderUtil.createFolder(tCSOAServiceFactory.getDataManagementService(), departFolder, TCFolderConstant.TYPE_D9_PHASEFOLDER, phaseInfo.getName(),phaseInfo.getDescr());
                    List<TempInfo> archiveInfos =phaseInfo.getChildren();
                    for (TempInfo archiveInfo : archiveInfos) {
                        FolderUtil.createFolder(tCSOAServiceFactory.getDataManagementService(), phaseFolder, TCFolderConstant.TYPE_D9_ARCHIVE, archiveInfo.getName(),archiveInfo.getDescr());
                    }
                }
            }
        }
    }





}
