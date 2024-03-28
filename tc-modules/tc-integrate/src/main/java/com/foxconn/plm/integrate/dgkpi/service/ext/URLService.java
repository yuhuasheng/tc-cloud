package com.foxconn.plm.integrate.dgkpi.service.ext;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.integrate.dgkpi.domain.resp.UrlResp;
import com.foxconn.plm.integrate.dgkpi.domain.rp.UrlRp;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.DataManagementUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("uRLService")
public class URLService {

    @Value("${kpi.hdfs}")
    private String hdfs;
    @Value("${kpi.fsurl}")
    private String fsurl;


    private static Log log = LogFactory.get();
   public R getURLByQuery(JSONObject paramJSONObject){
       try{
           UrlRp urlRp= JSONObject.toJavaObject(paramJSONObject, UrlRp.class);
           String objType=urlRp.getDocType();
           //兼容老接口
           if((!("STP-DIAG-L6".equalsIgnoreCase(objType)))
               &&!("DRM-ME".equalsIgnoreCase(objType))
               &&!("DRM-SE".equalsIgnoreCase(objType))
               &&!("DRM-PSU".equalsIgnoreCase(objType))
           ){
              return getOldURLByQuery(paramJSONObject);
           }
       }catch(Exception e){
           log.error(e.getLocalizedMessage(),e);
           return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getMessage());
       }

       TCSOAServiceFactory tcSOAServiceFactory = null;
       try {
           UrlRp urlRp= JSONObject.toJavaObject(paramJSONObject, UrlRp.class);
           R r=checkParams(urlRp);
           if(r !=null){
               return r;
           }
           tcSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
           tcSOAServiceFactory.getSessionService().refreshPOMCachePerRequest(true);
           DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();
           SavedQueryService savedQueryService=tcSOAServiceFactory.getSavedQueryService();
           String sisProject=urlRp.getSpasProjId();
           if(sisProject!=null&&sisProject.indexOf("-")>-1){
               sisProject=sisProject.substring(0,sisProject.indexOf("-")).trim();
           }
           if(sisProject.toLowerCase(Locale.ENGLISH).startsWith("p")){
               sisProject=sisProject.substring(1);
           }
           sisProject="P"+sisProject;
           Map<String, Object> queryResults = TCUtils.executeQuery(savedQueryService, "__D9_Find_KPIDocument",
                   new String[]{"project_list.project_id","Document:items_tag.d9_DocumentType"}, new String[]{"*"+sisProject+"*",urlRp.getDocType()+"*"});
           if (queryResults.get("succeeded") == null) {
               return R.error(HttpResultEnum.NO_RESULT.getCode(),"未查询到數據");
           }
           ModelObject[] mds = (ModelObject[]) queryResults.get("succeeded");
           if (mds == null || mds.length <= 0) {
               return R.error(HttpResultEnum.NO_RESULT.getCode(),"未查询到數據");
           }

           List<Document> documents =new ArrayList<>();
           for(ModelObject m:mds){
               DocumentRevision documentRevision=(DocumentRevision)m;
               DataManagementUtil.getProperty(dataManagementService,documentRevision,"items_tag");
               Document doc=(Document)documentRevision.get_items_tag();
               documents.add(doc);
           }


           List<Document> removes= new ArrayList<>();
           if(urlRp.getItemId()!=null){
               for(Document doc:documents){
                   DataManagementUtil.getProperty(dataManagementService,doc,"item_id");
                   String itemId = doc.get_item_id();
                   if(!itemId.equalsIgnoreCase(urlRp.getItemId())){
                       removes.add(doc);
                   }
               }
           }
           if(removes.size()>0){
               documents.removeAll(removes);
           }

           Date date=null;
           if("First Created".equalsIgnoreCase(urlRp.getObjectCondition())){
               Document firstCreated=null;
               for(Document doc:documents){
                   if("First Released".equalsIgnoreCase(urlRp.getRevsionRule())||"Last Released".equalsIgnoreCase(urlRp.getRevsionRule())||"Last Working".equalsIgnoreCase(urlRp.getRevsionRule())){
                       String statusName=getStatusName(dataManagementService, doc);
                       if(statusName !=null  && statusName.indexOf("Obsolete")>-1){
                           continue;
                       }
                   }else if("EOL".equalsIgnoreCase(urlRp.getRevsionRule())){
                       String statusName=getStatusName(dataManagementService, doc);
                       if(statusName ==null || statusName.indexOf("Obsolete")<0){
                           continue;
                       }
                   }
                   DataManagementUtil.getProperty(dataManagementService,doc,"creation_date");
                   Date dat = doc.getPropertyObject("creation_date").getCalendarValue().getTime();
                   if(date==null){
                       date=dat;
                       firstCreated=doc;
                   }else{
                       if(dat.getTime()<date.getTime()){
                           date=dat;
                           firstCreated=doc;
                       }
                   }
               }
               documents.clear();
               documents.add(firstCreated);
           }
           date=null;
           if("Last Created".equalsIgnoreCase(urlRp.getObjectCondition())){
               Document lastCreated=null;
               for(Document doc:documents){
                   if("First Released".equalsIgnoreCase(urlRp.getRevsionRule())||"Last Released".equalsIgnoreCase(urlRp.getRevsionRule())||"Last Working".equalsIgnoreCase(urlRp.getRevsionRule())){
                       String statusName=getStatusName(dataManagementService, doc);
                       if(statusName !=null  && statusName.indexOf("Obsolete")>-1){
                           continue;
                       }
                   }else if("EOL".equalsIgnoreCase(urlRp.getRevsionRule())){
                       String statusName=getStatusName(dataManagementService, doc);
                       if(statusName ==null || statusName.indexOf("Obsolete")<0){
                           continue;
                       }
                   }
                   DataManagementUtil.getProperty(dataManagementService,doc,"creation_date");
                   Date dat = doc.getPropertyObject("creation_date").getCalendarValue().getTime();
                   if(date==null){
                       date=dat;
                       lastCreated=doc;
                   }else{
                       if(dat.getTime()>date.getTime()){
                           date=dat;
                           lastCreated=doc;
                       }
                   }
               }
               documents.clear();
               documents.add(lastCreated);
           }

           List<DocumentRevision> documentRevisons =new ArrayList<>();
           for(Document doc :documents){
               DataManagementUtil.getProperty(dataManagementService,doc,"revision_list");
               ModelObject[] itemRevs = doc.get_revision_list();
               DocumentRevision firstDocRev=null;
               DocumentRevision lastDocRev=null;
               DocumentRevision eolDocRev=null;
               for(ModelObject itmRev:itemRevs){
                   DocumentRevision docRev=(DocumentRevision)itmRev;
                   if(urlRp.getRevsionRule()==null){
                       documentRevisons.add(docRev);
                       continue;
                   }
                   DataManagementUtil.getProperty(dataManagementService,docRev,"release_status_list");
                   ModelObject[] statusArr = docRev.get_release_status_list();
                   if(statusArr==null||statusArr.length<=0){
                       if("Last Working".equalsIgnoreCase(urlRp.getRevsionRule())){
                           lastDocRev=docRev;
                       }
                       continue;
                   }
                   String statusName=getStatusName(dataManagementService, docRev);
                   if("First Released".equalsIgnoreCase(urlRp.getRevsionRule())){
                        if(firstDocRev==null){
                            if(statusName !=null && statusName.indexOf("Obsolete")<0){
                                firstDocRev=docRev;
                                documentRevisons.add(firstDocRev);
                            }
                        }
                   }
                   if(statusName !=null && statusName.indexOf("Obsolete")<0){
                       lastDocRev=docRev;
                   }
                   if(statusName !=null && statusName.indexOf("Obsolete")>-1){
                       eolDocRev=docRev;
                   }
               }

               if("Last Released".equalsIgnoreCase(urlRp.getRevsionRule())&&lastDocRev!=null){
                       documentRevisons.add(lastDocRev);
               }

               if("EOL".equalsIgnoreCase(urlRp.getRevsionRule())&&eolDocRev!=null){
                   documentRevisons.add(eolDocRev);
               }
           }


           List<UrlResp> data=new ArrayList<>();
          for(DocumentRevision documentRevision:documentRevisons) {
              DataManagementUtil.getProperties(dataManagementService, documentRevision, new String[]{"IMAN_specification","item_revision_id"});
              ModelObject[] datasets = null;
              if (documentRevision != null) {
                  datasets = documentRevision.get_IMAN_specification();
              }
              String revId=documentRevision.get_item_revision_id();
              if (datasets == null || datasets.length <= 0) {
                  return R.error(HttpResultEnum.NO_RESULT.getCode(), "未查询到數據");
              }
              Dataset dataset = (Dataset) datasets[0];
              DataManagementUtil.getProperty(dataManagementService, dataset, "object_type");
              String objType = dataset.get_object_type();
              String url = "";
              if ("html".equalsIgnoreCase(objType)) {
                  DataManagementUtil.getProperty(dataManagementService, dataset, "object_desc");
                  String fid = dataset.get_object_desc();
                  url = hdfs + "/downloadFileFromHdfs?fileVersionId=" + fid;
              } else {
                  String uid = dataset.getUid();
                  log.info("drm uid " + uid);
                  url = hdfs + "/downloadFile?site=WH&refId=" + uid;
              }
              DataManagementUtil.getProperties(dataManagementService, documentRevision, new String[]{"object_name","item_id","owning_user"});
              String objectName=documentRevision.getPropertyObject("object_name").getStringValue();
              String itemId=documentRevision.get_item_id();
              User owneringUser=(User)documentRevision.get_owning_user();
              DataManagementUtil.getProperties(dataManagementService, owneringUser, new String[]{"user_name"});
              String userName=owneringUser.get_user_name();

              UrlResp  rp=new UrlResp();
              rp.setItemId(itemId);
              rp.setObjectName(objectName);
              rp.setOwningUser(userName);
              rp.setUrl(url);
              rp.setSpasProjId(urlRp.getSpasProjId());
              rp.setRev(revId);
              data.add(rp);
          }

           if(data==null||data.size()<=0) {
               return R.error(HttpResultEnum.NO_RESULT.getCode(),"未查询到數據");
           }
           return R.success(data);
       } catch(Exception e){
           log.error(e.getLocalizedMessage(),e);
           return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getMessage());
       }finally {
           if (tcSOAServiceFactory != null) {
               tcSOAServiceFactory.logout();
           }
       }
    }


    private R getOldURLByQuery(JSONObject paramJSONObject){

        TCSOAServiceFactory tcSOAServiceFactory = null;
        try {
            tcSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            tcSOAServiceFactory.getSessionService().refreshPOMCachePerRequest(true);
            DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();
            UrlRp urlRp= JSONObject.toJavaObject(paramJSONObject, UrlRp.class);
            SavedQueryService savedQueryService=tcSOAServiceFactory.getSavedQueryService();
            String sisProject=urlRp.getSpasProjId();
            if(sisProject!=null&&sisProject.indexOf("-")>-1){
                sisProject=sisProject.substring(0,sisProject.indexOf("-")).trim();
            }
            if(sisProject.toLowerCase(Locale.ENGLISH).startsWith("p")){
                sisProject=sisProject.substring(1);
            }
            sisProject="P"+sisProject;
            Map<String, Object> queryResults = TCUtils.executeQuery(savedQueryService, "__D9_Find_KPIDocument",
                    new String[]{"project_list.project_id","Document:items_tag.d9_DocumentType"}, new String[]{"*"+sisProject+"*",urlRp.getDocType()+"*"});
            if (queryResults.get("succeeded") == null) {
                return R.error(HttpResultEnum.NO_RESULT.getCode(),"未查询到數據");
            }
            ModelObject[] mds = (ModelObject[]) queryResults.get("succeeded");
            if (mds == null || mds.length <= 0) {
                return R.error(HttpResultEnum.NO_RESULT.getCode(),"未查询到數據");
            }
            DocumentRevision documentRevision=null;
            Date lastDate=null;
            //如果有多個取最新
            for(ModelObject m:mds){
                DataManagementUtil.getProperty(dataManagementService,m,"creation_date");
                Date dat = m.getPropertyObject("creation_date").getCalendarValue().getTime();
                if(lastDate==null){
                    documentRevision=(DocumentRevision)m;
                    lastDate=dat;
                }else{
                    if(dat.getTime()>lastDate.getTime()){
                        documentRevision=(DocumentRevision)m;
                        lastDate=dat;
                    }
                }
            }
            DataManagementUtil.getProperty(dataManagementService,documentRevision,"IMAN_specification");
            ModelObject[]  datasets =null;
            if(documentRevision!=null){
                datasets  = documentRevision.get_IMAN_specification();
            }

            if (datasets == null || datasets.length <= 0) {
                return R.error(HttpResultEnum.NO_RESULT.getCode(),"未查询到數據");
            }
            Dataset dataset= (Dataset)datasets[0];
            DataManagementUtil.getProperty(dataManagementService,dataset,"object_type");
            String objType= dataset.get_object_type();
            String url="";
            if("html".equalsIgnoreCase(objType)){
                DataManagementUtil.getProperty(dataManagementService,dataset,"object_desc");
                String fid=dataset.get_object_desc();
                url = hdfs + "/downloadFileFromHdfs?fileVersionId="+fid;
            }else {
                String uid = dataset.getUid();
                log.info("drm uid " + uid);
                url = hdfs + "/downloadFile?site=WH&refId=" + uid;
            }
            List<String> data=new ArrayList<>();
            data.add(url);
            if(data==null||data.size()<=0) {
                return R.error(HttpResultEnum.NO_RESULT.getCode(),"未查询到數據");
            }
            return R.success(data);
        } catch(Exception e){
            log.error(e.getLocalizedMessage(),e);
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getMessage());
        }finally {
            if (tcSOAServiceFactory != null) {
                tcSOAServiceFactory.logout();
            }
        }
    }

    private R checkParams(UrlRp urlRp) throws  Exception{

             if(urlRp.getObjectCondition()!=null){
                  if(!("First Created".equalsIgnoreCase(urlRp.getObjectCondition()))&&!("Last Created".equalsIgnoreCase(urlRp.getObjectCondition()))){
                    return R.error(HttpResultEnum.PARAM_ERROR.getCode(),"objectCondition 參數錯誤");
                  }
             }

        if(urlRp.getRevsionRule()!=null){
              if(!("First Released".equalsIgnoreCase(urlRp.getRevsionRule()))&&!("Last Released".equalsIgnoreCase(urlRp.getRevsionRule()))){
                  return R.error(HttpResultEnum.PARAM_ERROR.getCode(),"revsionRule 參數錯誤");
              }
        }
        return null;

    }


    public static String getStatusName(DataManagementService dmService, Document doc) throws Exception {
        DataManagementUtil.getProperty(dmService,doc,"release_status_list");
        ModelObject[] statusArr = doc.get_release_status_list();
        if(statusArr==null||statusArr.length<=0){
            return null;
        }
        ModelObject status = statusArr[statusArr.length - 1];
        DataManagementUtil.getProperty(dmService,status,"object_name");
        String statusName= status.getPropertyObject("object_name").getStringValue();
        if (statusName == null || "".equalsIgnoreCase(statusName)) {
            return null;
        }
        return statusName;
    }


    public static String getStatusName(DataManagementService dmService, DocumentRevision docRev) throws Exception {
        DataManagementUtil.getProperty(dmService,docRev,"release_status_list");
        ModelObject[] statusArr = docRev.get_release_status_list();
        if(statusArr==null||statusArr.length<=0){
            return null;
        }
        ModelObject status = statusArr[statusArr.length - 1];
        DataManagementUtil.getProperty(dmService,status,"object_name");
        String statusName= status.getPropertyObject("object_name").getStringValue();
        if (statusName == null || "".equalsIgnoreCase(statusName)) {
            return null;
        }
        return statusName;
    }



}
