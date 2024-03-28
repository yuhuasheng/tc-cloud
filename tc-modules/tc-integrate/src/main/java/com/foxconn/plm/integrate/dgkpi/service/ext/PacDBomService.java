package com.foxconn.plm.integrate.dgkpi.service.ext;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.constants.TCItemConstant;
import com.foxconn.plm.entity.constants.TCSearchEnum;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.integrate.dgkpi.domain.resp.PacDesignStandardResp;
import com.foxconn.plm.integrate.dgkpi.domain.rp.PacDesignStandardRp;
import com.foxconn.plm.integrate.dgkpi.mapper.DesignStandardMapper;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.DataManagementUtil;
import com.foxconn.plm.utils.tc.ItemUtil;
import com.foxconn.plm.utils.tc.SessionUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.loose.core.SessionService;
import com.teamcenter.services.strong.cad.StructureManagementService;
import com.teamcenter.services.strong.cad._2013_05.StructureManagement;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("pacDBomService")
public   class PacDBomService {
    private static Log log = LogFactory.get();
    @Autowired(required = false)
    DesignStandardMapper designStandardMapper;

    public R getMEDBOM(JSONObject paramJSONObject){
        TCSOAServiceFactory tcSOAServiceFactory=null;
        List<PacDesignStandardResp>  kpiPojos= new ArrayList<>();
        try {
            PacDesignStandardRp kPIPojoRp= JSONObject.toJavaObject(paramJSONObject, PacDesignStandardRp.class);
            tcSOAServiceFactory=new TCSOAServiceFactory(TCUserEnum.DEV);
            String spasProjId=kPIPojoRp.getSpasProjId();
            log.info("Begin get kpi data ==========  spasId:"+spasProjId);
            SessionService sessionService = tcSOAServiceFactory.getSessionService();
            SessionUtil.byPass(sessionService, true);
            DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();
            SavedQueryService savedQueryService=tcSOAServiceFactory.getSavedQueryService();
            StructureManagementService structureManagementService=tcSOAServiceFactory.getStructureManagementService();
            Map<String, Object> queryResults = TCUtils.executeQuery(savedQueryService, TCSearchEnum.D9_FIND_PACPARTREV.queryName(),
                    TCSearchEnum.D9_FIND_PACPARTREV.queryParams(), new String[]{"p"+spasProjId});
            if (queryResults.get("succeeded") == null) {
                return R.error(HttpResultEnum.NO_RESULT.getCode(),"未查询到KPI数据");
            }
            ModelObject[] mds = (ModelObject[]) queryResults.get("succeeded");
            if (mds == null || mds.length <= 0) {
                return R.error(HttpResultEnum.NO_RESULT.getCode(),"未查询图到KPI数据");
            }
            for(ModelObject modelIv:mds) {
                String level="";
                String actualUser="";
                String bomName="";
                ItemRevision modelRev = (ItemRevision)modelIv;

                DataManagementUtil.getProperties(dataManagementService, modelRev, new String[]{"d9_Level","d9_ActualUserID","object_name","items_tag"});
                level=modelRev.getPropertyObject("d9_Level").getStringValue();
                actualUser=modelRev.getPropertyObject("d9_ActualUserID").getStringValue();
                bomName=modelRev.getPropertyObject("object_name").getStringValue();
                Item modelItem= modelRev.get_items_tag();
                log.info(" -----> 开始处理 item_id  " +bomName);
                DataManagementUtil.getProperty(dataManagementService, modelItem, TCItemConstant.REL_BOM_VIEW_TAGS);
                ModelObject[] bom_view_tags = modelItem.get_bom_view_tags();
                BOMView bomView = (BOMView) bom_view_tags[0];
                StructureManagement.CreateWindowsInfo2[] createWindowsInfo2s = new StructureManagement.CreateWindowsInfo2[1];
                createWindowsInfo2s[0] = new StructureManagement.CreateWindowsInfo2();
                createWindowsInfo2s[0].item = modelItem;
                createWindowsInfo2s[0].itemRev = modelRev;
                createWindowsInfo2s[0].bomView = bomView;
                com.teamcenter.services.strong.cad._2007_01.StructureManagement.CreateBOMWindowsResponse response = structureManagementService.createBOMWindows2(createWindowsInfo2s);
                BOMLine parentBOMLine = response.output[0].bomLine;
                BOMWindow bomWindow = response.output[0].bomWindow;
                getPacResps(kpiPojos,parentBOMLine,dataManagementService,spasProjId,level,bomName,actualUser);
                structureManagementService.saveBOMWindows(new BOMWindow[]{bomWindow});
                structureManagementService.closeBOMWindows(new BOMWindow[]{bomWindow});
            }

            SessionUtil.byPass(sessionService, false);
            log.info("End get kpi data ==========  modeId:"+paramJSONObject.getString("itemId"));
            if(  kpiPojos==null||kpiPojos.size()<=0){
                return R.error(HttpResultEnum.NO_RESULT.getCode(),"未查询到KPI数据");
            }
            return R.success(kpiPojos);
        }catch(Exception e){

            log.error(e.getLocalizedMessage(),e);
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(),e.getMessage());
        }finally {
            try {
                if(tcSOAServiceFactory!=null) {
                    tcSOAServiceFactory.logout();
                }
            }catch (Exception e){}
        }

    }




     private void getPacResps(List<PacDesignStandardResp> pacResps,BOMLine parentLine,DataManagementService dataManagementService,String spasProjId,String level,String bomName,String actualUser) throws Exception {
         dataManagementService.refreshObjects(new ModelObject[]{parentLine});

         DataManagementUtil.getProperty(dataManagementService, parentLine, "bl_all_child_lines");
         ModelObject[]  childModels=parentLine.get_bl_all_child_lines();
         HashMap childMP=new HashMap();
         for(ModelObject m:childModels){
             if(!(m instanceof  BOMLine)){
                 continue;
             }
             BOMLine bomLine = (BOMLine)m;
             dataManagementService.refreshObjects(new ModelObject[]{bomLine});

             DataManagementUtil.getProperty(dataManagementService, bomLine, "fnd0bl_is_substitute");
             boolean isStitutes = bomLine.get_fnd0bl_is_substitute();
             if(isStitutes){
                 continue;
             }

             DataManagementUtil.getProperty(dataManagementService, bomLine, "bl_revision");
             ItemRevision itemRev= (ItemRevision)bomLine.get_bl_revision();

             dataManagementService.refreshObjects(new ModelObject[]{itemRev});
             DataManagementUtil.getProperties(dataManagementService, itemRev, new String[]{"item_id","d9_InitialProject"});
             String childId = itemRev.get_item_id();
             String initialProject = itemRev.getPropertyObject("d9_InitialProject").getStringValue();
             String flag="Y";
             if(initialProject.indexOf("P"+spasProjId)>-1){
                 flag="N";
             }

             PacDesignStandardResp r=new PacDesignStandardResp();
             r.setFeature(flag);
             r.setChildId(childId);
             r.setSpasProjId(spasProjId);
             r.setLevel(level);
             r.setBomName(bomName);
             r.setActualUser(actualUser);
             pacResps.add(r);
             getPacResps(pacResps,bomLine,dataManagementService,spasProjId,level,bomName,actualUser);
         }

     }


}
