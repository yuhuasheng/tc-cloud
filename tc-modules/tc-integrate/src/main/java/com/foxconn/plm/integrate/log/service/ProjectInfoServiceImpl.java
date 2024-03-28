package com.foxconn.plm.integrate.log.service;

import cn.hutool.core.lang.hash.Hash;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.foxconn.dp.plm.privately.Access;
import com.foxconn.plm.entity.constants.TCFolderConstant;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.integrate.log.domain.ManpowerInfo;
import com.foxconn.plm.integrate.log.domain.ManpowerPhaseInfo;
import com.foxconn.plm.integrate.log.domain.ProjectInfo;
import com.foxconn.plm.integrate.log.mapper.ProjectInfoMapper;
import com.foxconn.plm.integrate.spas.domain.PhasePojo;
import com.foxconn.plm.integrate.spas.domain.ReportPojo;
import com.foxconn.plm.integrate.spas.service.impl.SpasServiceImpl;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.DataManagementUtil;
import com.foxconn.plm.utils.tc.FolderUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Folder;
import com.teamcenter.soa.client.model.strong.WorkspaceObject;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@Service("projectInfoServiceImpl")
public class ProjectInfoServiceImpl {
    private static Log log = LogFactory.get();
    @Autowired(required = false)
    ProjectInfoMapper projectInfoMapper;


    @Autowired(required = false)
    private SpasServiceImpl reportServiceImpl;

    /**
     * 刷新spas_info表,用於報表統計
     */
    @Transactional(rollbackFor = Exception.class)
    public void synProjectInfo() throws Exception{
            log.info("======================start synProjectInfo========================");
            deleteProjInfo();
            List<String> projs = selectActiveProjInTC();
            for (String proj : projs) {
                try {
                    if (proj == null || "".equalsIgnoreCase(proj.trim())) {
                        continue;
                    }
                    proj = proj.toLowerCase(Locale.ENGLISH).replaceAll("p", "");
                   
                    ReportPojo reportPojo = reportServiceImpl.getPhases(proj);

                    List<PhasePojo> phases = reportPojo.getPhases();
                    for (PhasePojo p : phases) {
                        ProjectInfo info = new ProjectInfo();
                        info.setBu(forrmatString(reportPojo.getBu()));
                        info.setCurPhase(forrmatString(reportPojo.getPhase()));
                        info.setCustomer(forrmatString(reportPojo.getCustomer()));
                        info.setProductLine(forrmatString(reportPojo.getProductLine()));
                        info.setSeries(forrmatString(reportPojo.getSeries()));
                        info.setProjId(forrmatString("p" + reportPojo.getProjectId()));
                        info.setProjName(forrmatString(reportPojo.getProjectName()));
                        info.setStartTime(forrmatString(p.getStartDate()));
                        info.setEndTime(forrmatString(p.getEndDate()));
                        info.setPhase(forrmatString(forrmatString(p.getName())));
                        info.setCurStartTime(forrmatString(reportPojo.getStartTime()));
                        info.setCurEndTime(forrmatString(reportPojo.getEndTime()));
                        info.setOwnerName(forrmatString(reportPojo.getOwnerName()));
                        String dat0 = "2022-06-15";
                        String bu = reportPojo.getBu();
                        if ("DT".equalsIgnoreCase(bu)) {
                            dat0 = "2022-06-01";
                        }
                        String dat1 = p.getEndDate();
                        if(dat1==null){
                            continue;
                        }
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        Date d0 = sdf.parse(dat0);
                        Date d1 = sdf.parse(dat1);
                        if (d1.getTime() < d0.getTime()) {
                            continue;
                        }

                        String dat2 = p.getStartDate();
                        if(dat2==null){
                            continue;
                        }
                        Date d2 = sdf.parse(dat2);

                        if (d2.getTime() > new Date().getTime()) {
                            continue;
                        }

                        addProjInfo(info);
                    }
                } catch (Exception e) {
                    log.error(e.getLocalizedMessage(), e);
                }
            }

        log.info("======================end synProjectInfo========================");
    }


    public List<String> selectActiveProjInTC() throws Exception {
        String userName= Access.getPasswordAuthentication();
        return projectInfoMapper.selectActiveProjInTC(userName);

    }


    public void addProjInfo(ProjectInfo projectInfo) {
        projectInfoMapper.addProjInfo(projectInfo);
    }


    public void deleteProjInfo() {
        projectInfoMapper.deleteProjInfo();
    }


    public List<String> getActualUsers() throws Exception {
        return projectInfoMapper.getActualUsers();
    }


    public String getFolderDiff() throws Exception {

        TCSOAServiceFactory tcsoaServiceFactory=null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        List<Map<String,String>>  actions= new ArrayList<>();
    try {
         tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS4);
         List<String> projs = selectActiveProjInTC();
        for (String p : projs) {

            Map<String, Object> queryResults = TCUtils.executeQuery(tcsoaServiceFactory.getSavedQueryService(), "__D9_Find_Project_Folder", new String[]{"d9_SPAS_ID"}, new String[]{(p)});
            if (queryResults.get("succeeded") == null) {
                continue;
            }
            ModelObject[] md = (ModelObject[]) queryResults.get("succeeded");
            if (md == null || md.length <= 0) {
                continue;
            }
            Folder projectFolder = (Folder) md[0];

            DataManagementUtil.getProperty(tcsoaServiceFactory.getDataManagementService(),projectFolder,"creation_date");
            Calendar calendar=projectFolder.get_creation_date();
            String dat= sdf.format(calendar.getTime());
            //prod 2275 uat 2100
            if(dat.compareTo("20230412")>0){
                continue;
            }

           String p0 = p.substring(1);
           List<ManpowerPhaseInfo> manpowerPhaseInfos = projectInfoMapper.getManPowerFunction(p0);
           Map<String, List<String>> manpowerMap = new HashMap<>();
          for (ManpowerPhaseInfo manpowerPhaseInfo : manpowerPhaseInfos) {
              String dept = manpowerPhaseInfo.getDeptName();
              List<String> ls = manpowerMap.get(dept);
              if (ls == null) {
                 ls = new ArrayList<>();
                 manpowerMap.put(dept, ls);
              }
              ls.add(manpowerPhaseInfo.getPhase());
          }
          List<ManpowerInfo> manpowerInfos = new ArrayList<>();
          Set<String> keys = manpowerMap.keySet();
          for (String key : keys) {
            ManpowerInfo manpowerInfo = new ManpowerInfo();
            manpowerInfo.setDeptName(key);
            manpowerInfo.setPhases(manpowerMap.get(key));
            manpowerInfos.add(manpowerInfo);
          }

            tcsoaServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{projectFolder});
            TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), projectFolder, "contents");
            WorkspaceObject[] contents = projectFolder.get_contents();
            for (int i = 0; i < contents.length; i++) {
                   WorkspaceObject content = contents[i];
                   if (!(content instanceof Folder)) {
                      continue;
                    }
                    Folder deptfolder = (Folder) content;
                    TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), deptfolder, "object_name");
                    String deptFolderName = deptfolder.get_object_name();
                    if(deptFolderName.equalsIgnoreCase("PM")){
                        continue;
                    }
                    if(deptFolderName.equalsIgnoreCase("SPM")){
                       continue;
                    }
                    if(deptFolderName.equalsIgnoreCase("產品設計協同工作區")){
                        continue;
                    }
                    if(deptFolderName.toLowerCase(Locale.ENGLISH).startsWith("tcfr")){
                        continue;
                    }
                   if(deptFolderName.toLowerCase(Locale.ENGLISH).startsWith("sim")){
                    continue;
                   }
                    ManpowerInfo manInfo=null;
                    for(ManpowerInfo m:manpowerInfos){
                        if(m.getDeptName().equalsIgnoreCase(deptFolderName)){
                            manInfo=m;
                           break;
                        }
                    }
                    if(manInfo==null){
                        Map<String,String> mp= new HashMap<>();
                        mp.put("proj",p);
                        mp.put("dept",deptFolderName);
                        mp.put("phase","");
                        mp.put("action","D");
                        actions.add(mp);
                    }else{
                        tcsoaServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{deptfolder});
                        TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), deptfolder, "contents");
                        WorkspaceObject[] contents2 = deptfolder.get_contents();
                        for (int k = 0; k < contents2.length; k++) {
                                WorkspaceObject content2 = contents2[k];
                                if (!(content2 instanceof Folder)) {
                                    continue;
                                }
                                Folder phasefolder = (Folder) content2;
                                TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), phasefolder, "object_name");
                                TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), phasefolder, TCFolderConstant.PROPERTY_OBJECT_TYPE);
                                String objectType=phasefolder.get_object_type();
                                if(!(TCFolderConstant.TYPE_D9_PHASEFOLDER.equalsIgnoreCase(objectType))){
                                   continue;
                                }
                                String phaseFolderName = phasefolder.get_object_name();
                                List<String> pss= manInfo.getPhases();
                                int f=0;
                                for(String ps:pss){
                                    if(phaseFolderName.toLowerCase(Locale.ENGLISH).startsWith(ps.toLowerCase(Locale.ENGLISH))){
                                        f=1;
                                    }
                                }
                                if(f==0){
                                    Map<String,String> mp= new HashMap<>();
                                    mp.put("proj",p);
                                    mp.put("dept",deptFolderName);
                                    mp.put("phase",phaseFolderName);
                                    mp.put("action","D");
                                    actions.add(mp);
                                }
                        }
                    }
            }
      }

        HSSFWorkbook wb = new HSSFWorkbook();
        HSSFSheet sheet=wb.createSheet("ALL");

        for(int i=0;i<actions.size();i++){
           Map<String,String> m= actions.get(i);
          //  html+= m.get("proj")+"\t"+m.get("dept")+"\t"+m.get("phase")+"\t"+m.get("action")+"\n";
            HSSFRow row=sheet.createRow(i);

            HSSFCell cell=row.createCell(0);
            setValue(cell,m.get("proj"));

            cell=row.createCell(1);
            setValue(cell,m.get("dept"));

            cell=row.createCell(2);
            setValue(cell,m.get("phase"));

            cell=row.createCell(3);
            setValue(cell,m.get("action"));
        }
        String dirPath = System.getProperty("java.io.tmpdir");
        String path=dirPath+File.separator+"diff_"+new Date().getTime()+".xls";
        FileOutputStream fos = new FileOutputStream(new File(path));
        wb.write(fos);
        fos.flush();
        fos.close();
      return path;

     }finally {
         if(tcsoaServiceFactory!=null){
             tcsoaServiceFactory.logout();
         }
     }

    }



public String saveFolderDiff(String path) throws Exception{
    TCSOAServiceFactory tCSOAServiceFactory=null;
    ExcelReader reader=null;
    try {
         tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS4);
         TCUtils.byPass(tCSOAServiceFactory.getSessionService(), true);
         reader = ExcelUtil.getReader(path);
         reader.setSheet(0);
         Sheet sheet = reader.getSheet();
        int lastRow = reader.getRowCount();
        for (int i = 0; i < lastRow; i++) {
              Row row = sheet.getRow(i);
              Cell cell = row.getCell(0);
              String proj = cell.getStringCellValue();
              if (proj == null || "".equalsIgnoreCase(proj.trim())) {
                break;
             }
             cell = row.getCell(1);
             String dept = cell.getStringCellValue();

             cell = row.getCell(2);
             String phase = cell.getStringCellValue();

             cell = row.getCell(3);
             String action = cell.getStringCellValue();

             System.out.println(proj + " " + dept + " " + phase + " " + action);


            Map<String, Object> queryResults = TCUtils.executeQuery(tCSOAServiceFactory.getSavedQueryService(), "__D9_Find_Project_Folder", new String[]{"d9_SPAS_ID"}, new String[]{(proj)});
            if (queryResults.get("succeeded") == null) {
                continue;
            }
            ModelObject[] md = (ModelObject[]) queryResults.get("succeeded");
            if (md == null || md.length <= 0) {
                continue;
            }
            Folder projectFolder = (Folder) md[0];
            tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{projectFolder});
            TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), projectFolder, TCFolderConstant.REL_CONTENTS);
            WorkspaceObject[] contents = projectFolder.get_contents();


            for (int j = 0; j < contents.length; j++) {
                    WorkspaceObject content = contents[j];
                    if (!(content instanceof Folder)) {
                       continue;
                    }
                    Folder deptFolder = (Folder) content;
                    TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), deptFolder, TCFolderConstant.PROPERTY_OBJECT_NAME);
                    String deptForlderName = deptFolder.get_object_name();
                    if(!(deptForlderName.equalsIgnoreCase(dept))){
                        continue;
                    }
                    if(phase ==null  || "".equalsIgnoreCase(phase)){
                        //直接删除部门文件夹
                        List<WorkspaceObject> wks = new ArrayList<>();
                        FolderUtil.isEmmptyFolder(tCSOAServiceFactory.getDataManagementService(), deptFolder, wks);
                        if (wks == null || wks.size() <= 0) {
                            com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship[] relationships =
                                    new com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship[1];
                            relationships[0] = new com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship();
                            relationships[0].clientId = "";
                            relationships[0].primaryObject = projectFolder;
                            relationships[0].secondaryObject = deptFolder;
                            relationships[0].relationType = TCFolderConstant.REL_CONTENTS;
                            TCUtils.setProperties(tCSOAServiceFactory.getDataManagementService(), deptFolder, "object_desc", "del_"+proj);
                            tCSOAServiceFactory.getDataManagementService().deleteRelations(relationships);
                            tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{projectFolder});
                        }else{
                            tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{deptFolder});
                            TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), deptFolder, TCFolderConstant.REL_CONTENTS);
                            WorkspaceObject[]  phaseContents = deptFolder.get_contents();
                            for (int k = 0; k < phaseContents.length; k++) {
                                WorkspaceObject phaseContent = phaseContents[k];
                                if (!(phaseContent instanceof Folder)) {
                                    continue;
                                }
                                Folder phaseFolder = (Folder) phaseContent;
                                TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), phaseFolder, TCFolderConstant.PROPERTY_OBJECT_TYPE);
                                String objectType=phaseFolder.get_object_type();
                                if(!(TCFolderConstant.TYPE_D9_PHASEFOLDER.equalsIgnoreCase(objectType))){
                                    continue;
                                }
                                List<WorkspaceObject> wks2 = new ArrayList<>();
                                FolderUtil.isEmmptyFolder(tCSOAServiceFactory.getDataManagementService(), phaseFolder, wks2);
                                if (wks2 == null || wks2.size() <= 0) {
                                    com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship[] relationships =
                                            new com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship[1];
                                    relationships[0] = new com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship();
                                    relationships[0].clientId = "";
                                    relationships[0].primaryObject = deptFolder;
                                    relationships[0].secondaryObject = phaseFolder;
                                    relationships[0].relationType = TCFolderConstant.REL_CONTENTS;
                                    TCUtils.setProperties(tCSOAServiceFactory.getDataManagementService(), phaseFolder, "object_desc", "del_"+proj+"_"+dept);
                                    tCSOAServiceFactory.getDataManagementService().deleteRelations(relationships);
                                    tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{deptFolder});
                                }
                            }
                        }
                    }else{
                        //删除部门文件夹下的阶段文件夹
                        tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{deptFolder});
                        TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), deptFolder, TCFolderConstant.REL_CONTENTS);
                        WorkspaceObject[]  phaseContents = deptFolder.get_contents();
                        for (int k = 0; k < phaseContents.length; k++) {
                            WorkspaceObject phaseContent = phaseContents[k];
                            if (!(phaseContent instanceof Folder)) {
                                continue;
                            }
                            Folder phaseFolder = (Folder) phaseContent;
                            TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), phaseFolder, TCFolderConstant.PROPERTY_OBJECT_TYPE);
                            String objectType=phaseFolder.get_object_type();
                            if(!(TCFolderConstant.TYPE_D9_PHASEFOLDER.equalsIgnoreCase(objectType))){
                                continue;
                            }
                            TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), phaseFolder, TCFolderConstant.PROPERTY_OBJECT_NAME);
                            String phaseForlderName = phaseFolder.get_object_name();
                            if(!(phaseForlderName.toLowerCase(Locale.ENGLISH).startsWith(phase.toLowerCase(Locale.ENGLISH)))){
                              continue;
                            }

                            List<WorkspaceObject> wks2 = new ArrayList<>();
                            FolderUtil.isEmmptyFolder(tCSOAServiceFactory.getDataManagementService(), phaseFolder, wks2);
                            if (wks2 == null || wks2.size() <= 0) {
                                com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship[] relationships =
                                        new com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship[1];
                                relationships[0] = new com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship();
                                relationships[0].clientId = "";
                                relationships[0].primaryObject = deptFolder;
                                relationships[0].secondaryObject = phaseFolder;
                                relationships[0].relationType = TCFolderConstant.REL_CONTENTS;
                                TCUtils.setProperties(tCSOAServiceFactory.getDataManagementService(), phaseFolder, "object_desc", "del_"+proj+"_"+dept);
                                tCSOAServiceFactory.getDataManagementService().deleteRelations(relationships);
                                tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{deptFolder});
                            }
                         }

                        //删除完之后，判断部门文件夹下还有没有部门文件夹
                        tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{deptFolder});
                        TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), deptFolder, TCFolderConstant.REL_CONTENTS);
                        WorkspaceObject[] phaseContents2 = deptFolder.get_contents();
                        if(phaseContents2==null||phaseContents2.length<=0){
                            com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship[] relationships =
                                    new com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship[1];
                            relationships[0] = new com.teamcenter.services.strong.core._2006_03.DataManagement.Relationship();
                            relationships[0].clientId = "";
                            relationships[0].primaryObject = projectFolder;
                            relationships[0].secondaryObject = deptFolder;
                            relationships[0].relationType = TCFolderConstant.REL_CONTENTS;
                            TCUtils.setProperties(tCSOAServiceFactory.getDataManagementService(), deptFolder, "object_desc", "del_"+proj);
                            tCSOAServiceFactory.getDataManagementService().deleteRelations(relationships);
                            tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{projectFolder});
                        }
                    }
            }
        }
       }finally {
           if(tCSOAServiceFactory!=null){
               tCSOAServiceFactory.logout();
           }
           if(reader!=null){
               reader.close();
           }
      }

        return null;
}


    private void setValue(HSSFCell cell,String v){

        cell.setCellValue(v==null?"":v);
    }



    public List<String> getProjsIntc() throws Exception {
        return projectInfoMapper.getProjsIntc();
    }

    private String forrmatString(String str) {
        if (str == null) {
            return "";
        }
        return str.trim();
    }
}
