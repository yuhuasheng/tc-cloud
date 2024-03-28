package com.foxconn.plm.integrate.agile.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.constants.TCItemConstant;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.response.SPASUser;
import com.foxconn.plm.feign.service.TcIntegrateClient;
import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.integrate.agile.domain.PrtBomAction;
import com.foxconn.plm.integrate.agile.domain.PrtBomLineInfo;
import com.foxconn.plm.integrate.agile.domain.PrtPartInfo;
import com.foxconn.plm.integrate.mail.service.MailGroupSettingService;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.collect.CollectUtil;
import com.foxconn.plm.utils.tc.DataManagementUtil;
import com.foxconn.plm.utils.tc.DatasetUtil;
import com.foxconn.plm.utils.tc.PreferencesUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.google.gson.Gson;
import com.teamcenter.services.strong.cad.StructureManagementService;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.*;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;

@Service
public class PRTEBOMServiceImpl {

    @Value("${agile.prt.url:}")
    private String agileUrl;

    @Value("${agile.prt.user:}")
    private String agileUser;

    @Value("${agile.prt.pword:}")
    private String agilePw;

    private static Log log = LogFactory.get();
    @Autowired(required = false)
    MailGroupSettingService mailGroupSettingImpl;

    @Resource
    private TcIntegrateClient tcIntegrateClient;

    @Resource
    TcMailClient tcMail;


    public void buildAgileBOM(String uid)   {
        TCSOAServiceFactory tcSOAServiceFactory=null;
        String ecnNO=null;
        String mail=null;
        String[] mailNotice=null;
        String[] mailTitle=null;
        try {
            tcSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            DataManagementService dataManagementService=tcSOAServiceFactory.getDataManagementService();

            ServiceData sdDataset = dataManagementService.loadObjects(new String[]{uid});
            Map<String,PrtPartInfo> partsMap=new HashMap<>();
            ItemRevision dcnItemRev = (ItemRevision) sdDataset.getPlainObject(0);

            String email=null;
            String[] customerName=PreferencesUtil.getTCPreferences(tcSOAServiceFactory.getPreferenceManagementService(),"D9_PRT_CustomerName");
            mailNotice=PreferencesUtil.getTCPreferences(tcSOAServiceFactory.getPreferenceManagementService(),"D9_PRT_EBOMToAgile_MailNotice");
            mailTitle=PreferencesUtil.getTCPreferences(tcSOAServiceFactory.getPreferenceManagementService(),"D9_PRT_EBOMToAgile_MailTitle");
            TCUtils.getProperty(dataManagementService, new ModelObject[]{dcnItemRev}, new String[]{"d9_ActualUserID"});
            String actualUserID= dcnItemRev.getPropertyObject("d9_ActualUserID").getStringValue();
            if(actualUserID!=null&&(!("".equalsIgnoreCase(actualUserID.trim())))) {
                Map<String, String> dataMap = new HashMap<>();
                dataMap.put("empId", actualUserID.substring(actualUserID.indexOf("(") + 1, actualUserID.indexOf(")")));
                Gson gson = new Gson();
                List<SPASUser> spasUsers = tcIntegrateClient.getTeamRosterByEmpId(gson.toJson(dataMap));
                if (CollectUtil.isNotEmpty(spasUsers)) {
                    email = spasUsers.get(0).getNotes();
                }
            }
            if(email==null||"".equalsIgnoreCase(email.trim())){
                email= StringUtil.join(mailNotice,";");
            }else{
                mail=  email;
            }

            TCUtils.getProperty(dataManagementService, new ModelObject[]{dcnItemRev}, new String[]{"item_id"});
            ecnNO= dcnItemRev.get_item_id();

            ModelObject[] solutionItemObjs = TCUtils.getPropModelObjectArray(dataManagementService, dcnItemRev, TCItemConstant.REL_SOLUTIONITEM);
            Map<String,List<PrtBomAction>>  bomActionsMap=  new HashMap<>();
            for(ModelObject m :solutionItemObjs){
                  if(!(m instanceof  ItemRevision)){
                      continue;
                  }
                ItemRevision itemRev=  (ItemRevision)m;
                TCUtils.getProperties(dataManagementService, itemRev, new String[]{"items_tag","item_id"});
                Item item=itemRev.get_items_tag();
                String itemId=itemRev.get_item_id();
                ModelObject[] objects = {item};
                String[] atts = {"revision_list"};
                dataManagementService.getProperties(objects, atts);
                ModelObject[] itemRevs = item.get_revision_list();
                ItemRevision preItemRev =null;
                if(itemRevs.length>1){
                    preItemRev=(ItemRevision)(itemRevs[itemRevs.length-2]);
                }
                List<PrtBomAction> actions=compareBOM(item,itemRev,tcSOAServiceFactory,preItemRev,partsMap);
                bomActionsMap.put(itemId,actions);
            }
            Map   mp=  new HashMap<>();
            mp.put("parts",partsMap);
            mp.put("boms",bomActionsMap);

            File f=null;
            ModelObject[] modelObjects = TCUtils.getPropModelObjectArray(dataManagementService, dcnItemRev, "IMAN_specification");
            if(modelObjects.length>=0) {
                for(ModelObject m:modelObjects){
                    if(m instanceof  Dataset){
                        Dataset  dataset= (Dataset)m;
                        FileManagementUtility fileManagementUtility = tcSOAServiceFactory.getFileManagementUtility();
                        File[] files = DatasetUtil.getDataSetFiles(dataManagementService, dataset, fileManagementUtility);
                        if(files.length>=0){
                            ModelObject[] refList = dataset.get_ref_list();
                            ImanFile dsFile = (ImanFile) refList[0];
                            TCUtils.getProperty(dataManagementService, dsFile, "original_file_name");
                            String original_file_name = dsFile.get_original_file_name();
                            File temp = File.createTempFile("tcDataSet-", ".temp");
                            String filePath = temp.getParent() + "\\" + System.currentTimeMillis()+ "\\" + original_file_name;
                            FileUtil.rename(files[0], filePath, true);
                            System.out.println(filePath);
                            f=  new File(filePath);
                        }
                        break;
                    }
                }
            }
             new SyncThread(JSONUtil.toJsonStr(mp),f,StringUtil.join(customerName,";"),StringUtil.join(mailNotice,";"),mailTitle[0],email,ecnNO).start();
        } catch(Exception e){
            log.error(e.getMessage(),e);
            try {
                JSONObject httpmap = new JSONObject();
                if(mail==null||"".equalsIgnoreCase(mail.trim())){
                    httpmap.put("sendTo", StringUtil.join(mailNotice,","));
                }else{
                    httpmap.put("sendTo", mail);
                }
                httpmap.put("sendCc", StringUtil.join(mailNotice,","));
                httpmap.put("subject", mailTitle[0]);
                String html="TC ECN NO. :"+ecnNO+" 同步Agile失败, "+(e.getMessage()==null?"failed":e.getMessage());
                httpmap.put("htmlmsg", html);
                tcMail.sendMail3Method(httpmap.toJSONString());
            }catch (Exception e0) {
                log.error(e0);
            }

        }finally {
            if(tcSOAServiceFactory!=null){
                tcSOAServiceFactory.logout();
            }
        }

    }

    class SyncThread extends  Thread {
        private String json;
        private File file;
        private String customerName;
        private String mailNotice;
        private String mailTitle;
        private String mailTo;
        private String ecNo;

        public  SyncThread(String json,File f,String customerName, String mailNotice,String mailTitle,String mailTo,String ecNo){
                 this.json=json;
                 this.file=f;
                 this.customerName=customerName;
                 this.mailNotice=mailNotice;
                 this.mailTitle=mailTitle;
                 this.mailTo=mailTo;
                 this.ecNo=ecNo;
        }

        @Override
        public void run() {
          //  String rs = HttpUtil.post(agileUrl, this.json);
            HashMap mp=  new HashMap<String,Object>();
            mp.put("json", this.json);
            mp.put("multipartFile", file);
            mp.put("customerName", this.customerName);
            mp.put("mailNotice", this.mailNotice);
            mp.put("mailTitle", this.mailTitle);
            mp.put("mailTo", this.mailTo);
            mp.put("ecNo", this.ecNo);

            HttpUtil.post(agileUrl, mp);
        }
    }






     private  List<PrtBomAction>  compareBOM(Item item,ItemRevision curItemRev,TCSOAServiceFactory tCSOAServiceFactory,ItemRevision prerItemRev,Map<String,PrtPartInfo> parts) throws Exception {
         List<PrtBomLineInfo> curBomlines=getPrtBomLines(item,curItemRev,tCSOAServiceFactory,parts);
         List<PrtBomLineInfo> preBomlines=getPrtBomLines(item,prerItemRev,tCSOAServiceFactory,parts);
         System.out.println("");
         List<PrtBomAction> actions=new ArrayList<>();
         if(preBomlines==null||preBomlines.size()<=0){
             for(PrtBomLineInfo p:curBomlines){
                 PrtBomAction action=new PrtBomAction();
                 action.setParentNum(p.getParentNum());
                 action.setChildNum(p.getChildNum());
                 action.setFindNum(p.getFindNum());
                 action.setQty(p.getQty());
                 action.setAltCode(p.getAltCode());
                 action.setAltGroup(p.getAltGroup());
                 action.setAction("A");
                 actions.add(action);
             }
             return actions;
         }

         for(PrtBomLineInfo p:curBomlines){
             String childNum= p.getChildNum();
             String findNum= p.getFindNum();
             String qty= p.getQty();
             int f=0;
             for(PrtBomLineInfo p2:preBomlines){
                 String childNum2= p2.getChildNum();
                 String findNum2= p2.getFindNum();
                 String qty2     =p2.getQty();
                 if(childNum.equalsIgnoreCase(childNum2)&&findNum.equalsIgnoreCase(findNum2)){
                     f=1;
                     if(!(qty.equalsIgnoreCase(qty2))){
                         PrtBomAction action=new PrtBomAction();
                         action.setParentNum(p.getParentNum());
                         action.setChildNum(p.getChildNum());
                         action.setFindNum(p.getFindNum());
                         action.setQty(p.getQty());
                         action.setAltCode(p.getAltCode());
                         action.setAltGroup(p.getAltGroup());
                         action.setAction("C");
                         actions.add(action);
                     }
                 }
             }
             if(f==0){
                 PrtBomAction action=new PrtBomAction();
                 action.setParentNum(p.getParentNum());
                 action.setChildNum(p.getChildNum());
                 action.setFindNum(p.getFindNum());
                 action.setQty(p.getQty());
                 action.setAltCode(p.getAltCode());
                 action.setAltGroup(p.getAltGroup());
                 action.setAction("A");
                 actions.add(action);
             }
         }

         for(PrtBomLineInfo p:preBomlines){
             String childNum= p.getChildNum();
             String findNum= p.getFindNum();
             int f=0;
             for(PrtBomLineInfo p2:curBomlines){
                 String childNum2= p2.getChildNum();
                 String findNum2= p2.getFindNum();
                 if(childNum.equalsIgnoreCase(childNum2)&&findNum.equalsIgnoreCase(findNum2)){
                     f=1;
                 }
             }
             if(f==0){
                 PrtBomAction action=new PrtBomAction();
                 action.setParentNum(p.getParentNum());
                 action.setChildNum(p.getChildNum());
                 action.setFindNum(p.getFindNum());
                 action.setQty(p.getQty());
                 action.setAltCode(p.getAltCode());
                 action.setAltGroup(p.getAltGroup());
                 action.setAction("D");
                 actions.add(action);
             }
         }
         return actions;

     }

    List<PrtBomLineInfo> getPrtBomLines(Item item,ItemRevision itemRev,TCSOAServiceFactory tCSOAServiceFactory,Map<String,PrtPartInfo> parts) throws Exception {
        BOMWindow bomWindow =null;
        StructureManagementService structureManagementService=null;
        try {
            List<PrtBomLineInfo> curBomlines = new ArrayList<>();
            Map<String, PrtBomLineInfo> curBomlineMps = new HashMap<>();
            Map<String, Integer> fnMps = new HashMap<>();
            DataManagementService dataManagementService = tCSOAServiceFactory.getDataManagementService();
            structureManagementService = tCSOAServiceFactory.getStructureManagementService();
            DataManagementUtil.getProperty(dataManagementService, item, TCItemConstant.REL_BOM_VIEW_TAGS);
            ModelObject[] bom_view_tags = item.get_bom_view_tags();
            if(bom_view_tags.length<=0||itemRev==null){
                if(itemRev!=null) {
                    DataManagementUtil.getProperty(dataManagementService, itemRev, "item_id");
                    String parentNum = itemRev.get_item_id();
                    if (parts.get(parentNum) == null) {
                        parts.put(parentNum, getPartInfo(itemRev, tCSOAServiceFactory));
                    }
                }
                return curBomlines;
            }
            BOMView bomView = (BOMView) bom_view_tags[0];
            dataManagementService.refreshObjects(new ModelObject[]{bomView});
            com.teamcenter.services.strong.cad._2013_05.StructureManagement.CreateWindowsInfo2[] createWindowsInfo2s = new com.teamcenter.services.strong.cad._2013_05.StructureManagement.CreateWindowsInfo2[1];
            createWindowsInfo2s[0] = new com.teamcenter.services.strong.cad._2013_05.StructureManagement.CreateWindowsInfo2();
            createWindowsInfo2s[0].item = item;
            createWindowsInfo2s[0].itemRev = itemRev;
            createWindowsInfo2s[0].bomView = bomView;
            com.teamcenter.services.strong.cad._2007_01.StructureManagement.CreateBOMWindowsResponse response = structureManagementService.createBOMWindows2(createWindowsInfo2s);
            BOMLine topBOMLine = response.output[0].bomLine;
            bomWindow = response.output[0].bomWindow;
            DataManagementUtil.getProperty(dataManagementService, topBOMLine, "bl_all_child_lines");
            ModelObject[] childModels = topBOMLine.get_bl_all_child_lines();
            DataManagementUtil.getProperty(dataManagementService, topBOMLine, "bl_revision");
            ItemRevision parentItemRev = (ItemRevision) topBOMLine.get_bl_revision();
            dataManagementService.refreshObjects(new ModelObject[]{parentItemRev});
            DataManagementUtil.getProperty(dataManagementService, parentItemRev, "item_id");
            String parentNum = parentItemRev.get_item_id();
            if(parts.get(parentNum)==null){
                parts.put(parentNum,getPartInfo(parentItemRev,tCSOAServiceFactory));
            }
            String priQty=null;
            String priFindNum=null;
            for (ModelObject m : childModels) {
                if (!(m instanceof BOMLine)) {
                    continue;
                }
                String altCode = "PRI";
                BOMLine bomLine = (BOMLine) m;
                dataManagementService.refreshObjects(new ModelObject[]{bomLine});
                DataManagementUtil.getProperty(dataManagementService, bomLine, "fnd0bl_is_substitute");
                boolean isStitutes = bomLine.get_fnd0bl_is_substitute();
                if (isStitutes) {
                    altCode = "ALT";
                }
                DataManagementUtil.getProperty(dataManagementService, bomLine, "bl_revision");
                ItemRevision childItemRev = (ItemRevision) bomLine.get_bl_revision();
                dataManagementService.refreshObjects(new ModelObject[]{childItemRev});
                DataManagementUtil.getProperty(dataManagementService, childItemRev, "item_id");
                String childNum = childItemRev.get_item_id();
                if(parts.get(childNum)==null){
                    parts.put(childNum,getPartInfo(childItemRev,tCSOAServiceFactory));
                }
                DataManagementUtil.getProperty(dataManagementService, bomLine, "bl_quantity");
                String qty = bomLine.get_bl_quantity();
                if(altCode.equalsIgnoreCase("PRI")){
                    if (qty == null || "".equalsIgnoreCase(qty.trim())) {
                        throw new Exception("qty is error");
                    }
                    priQty=qty;
                }else{
                    qty=priQty;
                }

                DataManagementUtil.getProperty(dataManagementService, bomLine, "bl_sequence_no");
                String findNum = bomLine.get_bl_sequence_no();
                if(altCode.equalsIgnoreCase("PRI")) {
                    if (findNum == null || "".equalsIgnoreCase(findNum.trim())) {
                        throw new Exception("find num  is errror");
                    }
                    priFindNum=findNum;
                }else{
                    findNum= priFindNum;
                }
                String key = parentNum + childNum + altCode + findNum;
                PrtBomLineInfo bomLineInfo = curBomlineMps.get(key);
                if (bomLineInfo == null) {
                    bomLineInfo = new PrtBomLineInfo();
                    bomLineInfo.setParentNum(parentNum);
                    bomLineInfo.setChildNum(childNum);
                    bomLineInfo.setQty(qty);
                    bomLineInfo.setAltCode(altCode);
                    bomLineInfo.setFindNum(findNum);
                    bomLineInfo.setAltGroup(getAltGroup(findNum));
                    curBomlineMps.put(key, bomLineInfo);
                    curBomlines.add(bomLineInfo);
                    Integer n=fnMps.get(findNum);
                    if(n==null){
                        n=0;
                    }
                    n++;
                    fnMps.put(findNum,n);
                } else {
                    String qty1 = bomLineInfo.getQty();
                    Double qty2 = Double.parseDouble(qty1) + Double.parseDouble(qty);
                    bomLineInfo.setQty(qty2.toString());
                }
                System.out.println("");
            }

            for(PrtBomLineInfo  p:curBomlines){
                String fm= p.getFindNum();
                if(fnMps.get(fm).intValue()<=1){
                    p.setAltGroup("");
                    p.setAltCode("");
                }

            }
            return curBomlines;
        }finally {
            try {
                   if (bomWindow != null) {
                       structureManagementService.closeBOMWindows(new BOMWindow[]{bomWindow});
                  }
            }catch(Exception e){}
        }
    }


  private   PrtPartInfo getPartInfo(ItemRevision itemRev,TCSOAServiceFactory tCSOAServiceFactory) throws Exception {
            DataManagementService dataManagementService = tCSOAServiceFactory.getDataManagementService();
            dataManagementService.refreshObjects(new ModelObject[]{itemRev});
            DataManagementUtil.getProperties(dataManagementService, itemRev, new String []{"item_id", "d9_EnglishDescription","d9_ChineseDescription","d9_2DRev","d9_3DRev","d9_ProcurementMethods",
                                                                              "d9_SourcingType","d9_Un","d9_Module","d9_CommodityType","d9_Material","d9_OuterPart","d9_Customer","d9_CustomerPN","d9_FamilyPartNumber","object_type"});
            String itemNum=itemRev.get_item_id();
            String descrEn=itemRev.getPropertyObject("d9_EnglishDescription").getStringValue();
            String descrZh=itemRev.getPropertyObject("d9_ChineseDescription").getStringValue();
            String rev2d=itemRev.getPropertyObject("d9_2DRev").getStringValue();
            String rev3d=itemRev.getPropertyObject("d9_3DRev").getStringValue();
            String procurementMethods=itemRev.getPropertyObject("d9_ProcurementMethods").getStringValue();
            String sourcingType=itemRev.getPropertyObject("d9_SourcingType").getStringValue();
            String un=itemRev.getPropertyObject("d9_Un").getStringValue();
            String module=itemRev.getPropertyObject("d9_Module").getStringValue();
            String commodityType=itemRev.getPropertyObject("d9_CommodityType").getStringValue();
            String material=itemRev.getPropertyObject("d9_Material").getStringValue();
            String outerPart=itemRev.getPropertyObject("d9_OuterPart").getStringValue();
            String customer=itemRev.getPropertyObject("d9_Customer").getStringValue();
            String customerPN=itemRev.getPropertyObject("d9_CustomerPN").getStringValue();
            String familyPartNumber=itemRev.getPropertyObject("d9_FamilyPartNumber").getStringValue();
            String objectType=itemRev.getPropertyObject("object_type").getStringValue();

            PrtPartInfo  partInfo=new PrtPartInfo();
            partInfo.setDescrEn(descrEn);
            partInfo.setItemNum(itemNum);
            partInfo.setDescrZh(descrZh);
            partInfo.setRev2d(rev2d);
            partInfo.setRev3d(rev3d);

           partInfo.setProcurementMethods(procurementMethods);
           partInfo.setSourcingType(sourcingType);
           partInfo.setUn(un);
           partInfo.setModule(module);
           partInfo.setCommodityType(commodityType);
           partInfo.setMaterial(material);
           partInfo.setOuterPart(outerPart);
           partInfo.setCustomer(customer);
           partInfo.setCustomerPN(customerPN);
           partInfo.setFamilyPartNumber(familyPartNumber);
           partInfo.setObjectType(objectType);

           return partInfo;
     }

    private static String[] codes = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "N", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};


    private static String  getAltGroup(String f) throws Exception  {
        int	fl=Integer.parseInt(f.trim());
        if(fl<10) {
            throw new Exception("alt failed");
        }
        fl=fl/10;

        int codesLen = codes.length;
        String newStr = "";
        while (fl > 0) {
            int n = fl % codesLen;
            newStr = codes[n] + newStr;
            fl = fl / codesLen;
        }
        if (newStr.length() > 2) {
            throw new Exception("流水码用完了....");
        }
        while (newStr.length() < 2) {
            newStr = "0" + newStr;
        }
        return newStr;
    }





}
