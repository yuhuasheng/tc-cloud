package com.foxconn.plm.spas.utils;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.spas.bean.SynSpasConstants;
import com.foxconn.plm.spas.bean.TempInfo;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.cad.StructureManagementService;
import com.teamcenter.services.strong.cad._2013_05.StructureManagement;
import com.teamcenter.services.strong.query._2007_06.SavedQuery;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateUtil {

    private static Log log = LogFactory.get();
    private static Map<String,List<TempInfo>> dtTemplateInfosCachMap=null;
    private static Map<String,List<TempInfo>> shTemplateInfosCachMap=null;
    private static Map<String,List<TempInfo>> mntTemplateInfosCachMap=null;
    private static List<TempInfo> prtTemplateInfosCach=null;

    public static synchronized  List<TempInfo> getDTTemplates(TCSOAServiceFactory tCSOAServiceFactory,String customerName) throws Exception {
        List<TempInfo> dtTemplateInfos= null;
        if(dtTemplateInfosCachMap!=null){
            List<TempInfo> dtTemplateInfosCach= dtTemplateInfosCachMap.get(customerName);
            if(dtTemplateInfosCach!=null) {
                dtTemplateInfos = new ArrayList<>();
                for (TempInfo t : dtTemplateInfosCach) {
                    dtTemplateInfos.add(BeanUtils.cloneBean(t));
                }
                return dtTemplateInfos;
            }
        }
        List<TempInfo> dtTemplateInfosCachTmp=null;
        log.info("begin load DT Templates");
        String[] projectTemplates = TCUtils.getTCPreferences(tCSOAServiceFactory.getPreferenceManagementService(), SynSpasConstants.D9_PROJECT_FOLDER_TEMPLATE);
        List<String> projectTemplateList = Arrays.asList(projectTemplates);
        for(String pref:projectTemplateList){
           String[] m= pref.split("=");
           String customer=m[0];
           if(!(customer.equalsIgnoreCase("DT_"+customerName))){
               continue;
           }

           String itemId=m[1];
            SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(
                    tCSOAServiceFactory.getSavedQueryService(),
                    SynSpasConstants.D9_ITEM_NAME_OR_ID, new String[]{SynSpasConstants.D9_ITEM_ID},  new String[]{itemId});

            ServiceData serviceData = savedQueryResult.serviceData;
            if (serviceData.sizeOfPartialErrors() == 0) {
                ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
                if (objs.length == 0) {
                    log.info("未找到模板");
                    throw new Exception("未找到模板");
                }
                Item root = (Item) objs[0];
                dtTemplateInfosCachTmp= sendPSE1(root,tCSOAServiceFactory);
            } else {
                throw new Exception("创建阶段文件夹未找到模板：" + serviceData.getPartialError(0).getErrorValues()[0].getMessage());
            }
        }

        dtTemplateInfos= new ArrayList<>();
        for(TempInfo t:dtTemplateInfosCachTmp){
            dtTemplateInfos.add(BeanUtils.cloneBean(t));
        }
        if(dtTemplateInfosCachMap==null){
            dtTemplateInfosCachMap=new HashMap<>();
        }
        dtTemplateInfosCachMap.put(customerName,dtTemplateInfosCachTmp);
        log.info("end load DT Templates");
        return dtTemplateInfos;
    }


      public static synchronized List<TempInfo> getMNTTemplates(TCSOAServiceFactory tCSOAServiceFactory,String platformLevel) throws Exception {
          String monitorTemplate = "";
          String[] levelTempConfig = TCUtils.getTCPreferences(tCSOAServiceFactory.getPreferenceManagementService(), SynSpasConstants.D9_MNT_PROJECT_FOLDER_TEMPLATE);
          String platformFoundLevel = platformLevel;
          if (platformFoundLevel == null) {
              platformFoundLevel = "";
          }
          String level = "";
          Matcher mat = Pattern.compile("\\(.*\\)").matcher(platformFoundLevel);
          while (mat.find()) {
              level = mat.group().replace("(", "").replace(")", "");
          }
          for (String levelTemp : levelTempConfig) {
              String[] split = levelTemp.split(":");
              if (level.equals(split[0])) {
                  monitorTemplate = split[1];
              }
          }

          List<TempInfo> tempInfosTmps= null;
          if(mntTemplateInfosCachMap!=null){
              List<TempInfo> mntTemplateInfosCach= mntTemplateInfosCachMap.get(level);
              if(mntTemplateInfosCach!=null) {
                  tempInfosTmps = new ArrayList<>();
                  for (TempInfo t : mntTemplateInfosCach) {
                      tempInfosTmps.add(BeanUtils.cloneBean(t));
                  }
                  return tempInfosTmps;
              }
          }

        log.info("begin load MNT Templates");
        Item levelItem = null;
        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(
                tCSOAServiceFactory.getSavedQueryService(), SynSpasConstants.D9_ITEM_NAME_OR_ID,
                new String[]{SynSpasConstants.D9_ITEM_ID}, new String[]{monitorTemplate});
        ServiceData serviceData = savedQueryResult.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
            if (objs.length == 0) {
                throw new Exception("創建部門文件夾時未找到對應模板，請聯係管理員確認文件夾模板是否存在!");
            }
            levelItem = (Item) objs[0];
        } else {
            throw new Exception("創建部門文件夾時未找到對應模板，請聯係管理員確認文件夾模板是否存在：" + serviceData.getPartialError(0));
        }
        List<TempInfo>  mntTemplateInfosCachTmp= sendPSE1(levelItem,tCSOAServiceFactory);

        tempInfosTmps = new ArrayList<>();
        for (TempInfo t : mntTemplateInfosCachTmp) {
              tempInfosTmps.add(BeanUtils.cloneBean(t));
        }
        if(mntTemplateInfosCachMap==null){
            mntTemplateInfosCachMap=new HashMap<>();
        }
        mntTemplateInfosCachMap.put(level,mntTemplateInfosCachTmp);
        log.info("end load MNT Templates");
        return tempInfosTmps;
    }

    public static synchronized   List<TempInfo>  getPrtTemplates(TCSOAServiceFactory tCSOAServiceFactory) throws Exception {
        List<TempInfo> prtTemplateInfos= null;
        if(prtTemplateInfosCach!=null){
            prtTemplateInfos= new ArrayList<>();
            for(TempInfo t:prtTemplateInfosCach){
                prtTemplateInfos.add(BeanUtils.cloneBean(t));
            }
            return prtTemplateInfos;
        }

        log.info("begin load PRT Templates");
        String[] projectTemplates = TCUtils.getTCPreferences(tCSOAServiceFactory.getPreferenceManagementService(), SynSpasConstants.D9_PROJECT_FOLDER_TEMPLATE);
        List<String> projectTemplateList = Arrays.asList(projectTemplates);
        for(String pref:projectTemplateList){
            String[] m= pref.split("=");
            String customer=m[0];
            if(!(customer.equalsIgnoreCase("PRT"))){
                continue;
            }
            String itemId=m[1];
            SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(
                    tCSOAServiceFactory.getSavedQueryService(),
                    SynSpasConstants.D9_ITEM_NAME_OR_ID, new String[]{SynSpasConstants.D9_ITEM_ID},  new String[]{itemId});
            ServiceData serviceData = savedQueryResult.serviceData;
            if (serviceData.sizeOfPartialErrors() == 0) {
                ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
                if (objs.length == 0) {
                    log.info("未找到模板");
                    throw new Exception("未找到模板");
                }
                Item root = (Item) objs[0];
                prtTemplateInfosCach= sendPSE1(root,tCSOAServiceFactory);
            } else {
                throw new Exception("创建阶段文件夹未找到模板：" + serviceData.getPartialError(0).getErrorValues()[0].getMessage());
            }
        }
        prtTemplateInfos= new ArrayList<>();
        if(prtTemplateInfosCach==null){
            prtTemplateInfosCach=new ArrayList<>();
        }
        for(TempInfo t:prtTemplateInfosCach){
            prtTemplateInfos.add(BeanUtils.cloneBean(t));
        }
        log.info("end reload PRT Templates");
        return prtTemplateInfos;
    }


    public static synchronized  List<TempInfo> getSHTemplates(TCSOAServiceFactory tCSOAServiceFactory,String customerName) throws Exception {
        List<TempInfo> shTemplateInfos= null;
        if(shTemplateInfosCachMap!=null){
            List<TempInfo> shTemplateInfosCach= shTemplateInfosCachMap.get(customerName);
            if(shTemplateInfosCach!=null) {
                shTemplateInfos = new ArrayList<>();
                for (TempInfo t : shTemplateInfosCach) {
                    shTemplateInfos.add(BeanUtils.cloneBean(t));
                }
                return shTemplateInfos;
            }
        }
        List<TempInfo> shTemplateInfosCachTmp=null;
        log.info("begin load DT Templates");
        String[] projectTemplates = TCUtils.getTCPreferences(tCSOAServiceFactory.getPreferenceManagementService(), SynSpasConstants.D9_PROJECT_FOLDER_TEMPLATE);
        List<String> projectTemplateList = Arrays.asList(projectTemplates);
        for(String pref:projectTemplateList){
            String[] m= pref.split("=");
            String customer=m[0];
            if(!(customer.equalsIgnoreCase("SH_"+customerName))){
                continue;
            }

            String itemId=m[1];
            SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(
                    tCSOAServiceFactory.getSavedQueryService(),
                    SynSpasConstants.D9_ITEM_NAME_OR_ID, new String[]{SynSpasConstants.D9_ITEM_ID},  new String[]{itemId});

            ServiceData serviceData = savedQueryResult.serviceData;
            if (serviceData.sizeOfPartialErrors() == 0) {
                ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
                if (objs.length == 0) {
                    log.info("未找到模板");
                    throw new Exception("未找到模板");
                }
                Item root = (Item) objs[0];
                shTemplateInfosCachTmp= sendPSE1(root,tCSOAServiceFactory);
            } else {
                throw new Exception("创建阶段文件夹未找到模板：" + serviceData.getPartialError(0).getErrorValues()[0].getMessage());
            }
        }

        shTemplateInfos= new ArrayList<>();
        for(TempInfo t:shTemplateInfosCachTmp){
            shTemplateInfos.add(BeanUtils.cloneBean(t));
        }
        if(shTemplateInfosCachMap==null){
            shTemplateInfosCachMap=new HashMap<>();
        }
        shTemplateInfosCachMap.put(customerName,shTemplateInfosCachTmp);
        log.info("end load SH Templates");
        return shTemplateInfos;
    }


    private static List<TempInfo> sendPSE1(Item root, TCSOAServiceFactory tCSOAServiceFactory) throws Exception {
        List<TempInfo> tempInfos=new ArrayList<>();
        tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{root});
        TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), root, "bom_view_tags");
        ModelObject[] bom_view_tags = root.get_bom_view_tags();
        if (bom_view_tags.length == 0) {
            throw new Exception("解析模板bom失敗");
        }
        BOMView bomView = (BOMView) bom_view_tags[0];
        ItemRevision itemRev = TCUtils.getItemLatestRevision(tCSOAServiceFactory.getDataManagementService(), root);
        StructureManagement.CreateWindowsInfo2[] createWindowsInfo2s = new StructureManagement.CreateWindowsInfo2[1];
        createWindowsInfo2s[0] = new StructureManagement.CreateWindowsInfo2();
        createWindowsInfo2s[0].item = root;
        createWindowsInfo2s[0].itemRev = itemRev;
        createWindowsInfo2s[0].bomView = bomView;
        StructureManagementService smService = tCSOAServiceFactory.getStructureManagementService();
        com.teamcenter.services.strong.cad._2007_01.StructureManagement.CreateBOMWindowsResponse response = smService.createBOMWindows2(createWindowsInfo2s);
        BOMLine customerBOMLine = response.output[0].bomLine;
        BOMWindow bomWindow = response.output[0].bomWindow;

        tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{customerBOMLine});
        com.teamcenter.services.strong.cad._2007_01.StructureManagement.ExpandPSOneLevelInfo expandPSAllLevelsInfo = new com.teamcenter.services.strong.cad._2007_01.StructureManagement.ExpandPSOneLevelInfo();
        expandPSAllLevelsInfo.parentBomLines = new BOMLine[]{customerBOMLine};
        expandPSAllLevelsInfo.excludeFilter = "None";
        com.teamcenter.services.strong.cad._2007_01.StructureManagement.ExpandPSOneLevelPref expandPSAllLevelsPref = new com.teamcenter.services.strong.cad._2007_01.StructureManagement.ExpandPSOneLevelPref();
        expandPSAllLevelsPref.expItemRev = false;
        com.teamcenter.services.strong.cad._2007_01.StructureManagement.ExpandPSOneLevelResponse expandPSAllLevelsResponse = smService.expandPSOneLevel(expandPSAllLevelsInfo, expandPSAllLevelsPref);
        ServiceData serviceData = expandPSAllLevelsResponse.serviceData;

        for (int i = 0; i < serviceData.sizeOfCreatedObjects(); i++) {
            BOMLine deptBomLine = (BOMLine) serviceData.getCreatedObject(i);

            TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), deptBomLine, "bl_item_object_name");
            TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), deptBomLine, "bl_item_item_id");
            String deptBomLineName = deptBomLine.get_bl_item_object_name();
            String tmpItemId = deptBomLine.get_bl_item_item_id();
            TempInfo deptTempInfo=new TempInfo();
            deptTempInfo.setName(deptBomLineName.trim());
            deptTempInfo.setDescr(getDescr(tmpItemId,tCSOAServiceFactory));
            tempInfos.add(deptTempInfo);

            tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{deptBomLine});
            TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), deptBomLine, "bl_all_child_lines");
            ModelObject[] allPhaseChild = deptBomLine.get_bl_all_child_lines();
            for (int k = 0; k < allPhaseChild.length; k++) {
                BOMLine phaseBomLine = (BOMLine) allPhaseChild[k];
                TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), phaseBomLine, "bl_item_object_name");
                TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), phaseBomLine, "bl_item_item_id");
                tmpItemId = phaseBomLine.get_bl_item_item_id();
                String phaseBomLineName = phaseBomLine.get_bl_item_object_name();
                TempInfo pahseTempInfo=new TempInfo();
                pahseTempInfo.setName(phaseBomLineName.trim());
                pahseTempInfo.setDescr(getDescr(tmpItemId,tCSOAServiceFactory));
                List<TempInfo>  phaseList=deptTempInfo.getChildren();
                if(phaseList==null){
                    phaseList= new ArrayList<>();
                    deptTempInfo.setChildren(phaseList);
                }
                phaseList.add(pahseTempInfo);

                tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{phaseBomLine});
                TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), phaseBomLine, "bl_all_child_lines");
                ModelObject[] allArchiveChild = phaseBomLine.get_bl_all_child_lines();

                if(allArchiveChild.length>0) {
                    for (int h = 0; h < allArchiveChild.length; h++) {
                        BOMLine archiveBomLine = (BOMLine) allArchiveChild[h];
                        TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), archiveBomLine, "bl_item_object_name");
                        TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), archiveBomLine, "bl_item_item_id");
                        String archiveBomLineName = archiveBomLine.get_bl_item_object_name();
                        tmpItemId = archiveBomLine.get_bl_item_item_id();
                        TempInfo archiveTempInfo = new TempInfo();
                        archiveTempInfo.setName(archiveBomLineName.trim());
                        archiveTempInfo.setDescr(getDescr(tmpItemId,tCSOAServiceFactory));
                        List<TempInfo> archiveList = pahseTempInfo.getChildren();
                        if (archiveList == null) {
                            archiveList = new ArrayList<>();
                            pahseTempInfo.setChildren(archiveList);
                        }
                        archiveList.add(archiveTempInfo);
                    }
                }else{
                    TempInfo archiveTempInfo = new TempInfo();
                    archiveTempInfo.setName(SynSpasConstants.NO_PHASE);
                    archiveTempInfo.setDescr(SynSpasConstants.NO_PHASE);
                    List<TempInfo> archiveList = pahseTempInfo.getChildren();
                    if (archiveList == null) {
                        archiveList = new ArrayList<>();
                        pahseTempInfo.setChildren(archiveList);
                    }
                    archiveList.add(archiveTempInfo);
                }
            }

        }
        smService.saveBOMWindows(new BOMWindow[]{bomWindow});
        smService.closeBOMWindows(new BOMWindow[]{bomWindow});
        return tempInfos;
    }


    private static String getDescr(String itemId,TCSOAServiceFactory tCSOAServiceFactory) throws Exception{
        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(
                tCSOAServiceFactory.getSavedQueryService(),
                SynSpasConstants.D9_ITEM_NAME_OR_ID, new String[]{SynSpasConstants.D9_ITEM_ID},  new String[]{itemId});
        ServiceData serviceData = savedQueryResult.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
            if (objs.length == 0) {
                throw new Exception("未找到對象"+itemId);
            }
            Item root = (Item) objs[0];
            ItemRevision itemRev = TCUtils.getItemLatestRevision(tCSOAServiceFactory.getDataManagementService(), root);
            tCSOAServiceFactory.getDataManagementService().refreshObjects(new ModelObject[]{itemRev});
            TCUtils.getProperty(tCSOAServiceFactory.getDataManagementService(), itemRev, "object_desc");
            String  descr =itemRev.get_object_desc();
            return descr;
        } else {
            throw new Exception("创建阶段文件夹未找到模板：" + serviceData.getPartialError(0).getErrorValues()[0].getMessage());
        }
    }




}
