package com.foxconn.plm.integrate.dgkpi.service.ext;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCBOMLineConstant;
import com.foxconn.plm.integrate.dgkpi.domain.DesignStandardPojo;
import com.foxconn.plm.integrate.dgkpi.domain.rp.DesignStandardRp;
import com.foxconn.plm.integrate.dgkpi.mapper.DesignStandardMapper;
import com.foxconn.plm.integrate.dgkpi.utils.KPIConstants;
import com.foxconn.plm.utils.tc.DataManagementUtil;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.*;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @ClassName: SyncFolderRunnable
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
public class MEBomRunnable implements Runnable {
    private static Log log = LogFactory.get();

    private ThreadPoolExecutor taskExecutor;
    private DataManagementService  dmService;
    private BOMLine  topBOMLine;
    private List<DesignStandardPojo> kPIPojos;
    private DesignStandardRp kPIPojoRp;
    private  DesignStandardMapper designStandardMapper;
    public MEBomRunnable(DataManagementService dmService , BOMLine topBOMLine, List<DesignStandardPojo> kPIPojos, DesignStandardRp kPIPojoRp, DesignStandardMapper designStandardMapper, ThreadPoolExecutor taskExecutor) {
           this.dmService=dmService;
           this.topBOMLine=topBOMLine;
           this.kPIPojos =kPIPojos;
           this.kPIPojoRp=kPIPojoRp;
           this.designStandardMapper=designStandardMapper;
           this.taskExecutor=taskExecutor;
    }

    @Override
    public void run() {
        log.info(" -----> 开始处理bom line ");
        try {
            DataManagementUtil.getProperty(dmService, topBOMLine, TCBOMLineConstant.REL_BL_REVISION);
            ItemRevision topItemRev= (ItemRevision)topBOMLine.get_bl_revision();
            DataManagementUtil.getProperty(dmService, topItemRev, "item_id");
            String parentId = topItemRev.get_item_id();
            log.info(" -----> 开始处理 item_id  " +parentId);
            dmService.refreshObjects(new ModelObject[]{topBOMLine});

            DataManagementUtil.getProperty(dmService, topBOMLine, "bl_all_child_lines");
            ModelObject[]  childModels=topBOMLine.get_bl_all_child_lines();
            HashMap childMP=new HashMap();
            for(ModelObject m:childModels){
                if(!(m instanceof  BOMLine)){
                    continue;
                }
                BOMLine bomLine = (BOMLine)m;
                dmService.refreshObjects(new ModelObject[]{bomLine});

                DataManagementUtil.getProperty(dmService, bomLine, "fnd0bl_is_substitute");
                boolean isStitutes = bomLine.get_fnd0bl_is_substitute();
                if(isStitutes){
                    continue;
                }

                DataManagementUtil.getProperty(dmService, bomLine, "bl_revision");
                ItemRevision itemRev= (ItemRevision)bomLine.get_bl_revision();

                dmService.refreshObjects(new ModelObject[]{itemRev});
                DataManagementUtil.getProperty(dmService, itemRev, "item_id");
                String childId = itemRev.get_item_id();
                DataManagementUtil.getProperty(dmService, bomLine, "bl_quantity");
                String  qty=bomLine.get_bl_quantity();
                if(qty==null||"".equalsIgnoreCase(qty.trim())){
                    qty="1";
                }
                if(childMP.get(childId)!=null){
                    DesignStandardPojo kp=(DesignStandardPojo)childMP.get(childId);
                    String qtyStr=kp.getQty();

                    int nqty= Integer.parseInt(qtyStr)+Integer.parseInt(qty);
                    kp.setQty(""+nqty);
                    continue;
                }

                DataManagementUtil.getProperty(dmService, bomLine, "bl_rev_release_status_list");
                String status= bomLine.get_bl_rev_release_status_list();
                DesignStandardPojo kPIPojo= new DesignStandardPojo();
                kPIPojo.setQty(qty);

                if(status!=null&&!("".equalsIgnoreCase(status.trim()))){
                    kPIPojo.setStatus("發佈");
                }else{
                    kPIPojo.setStatus("未發佈");
                }

                DataManagementUtil.getProperty(dmService, bomLine, "bl_has_children");
                boolean hasChild=bomLine.get_bl_has_children();
                if(hasChild){
                    kPIPojo.addMeType(KPIConstants.KPI_TYPE_ASSEMBLY);
                }

                DataManagementUtil.getProperty(dmService, itemRev, "project_ids");
                String projects=itemRev.get_project_ids();
                kPIPojo.setSpasProjIds(projects.replaceAll("P",""));

                DataManagementUtil.getProperty(dmService, itemRev, "d9_InitialProject");
                String initialProjId = itemRev.getPropertyObject("d9_InitialProject").getStringValue();
                if(initialProjId!=null&&initialProjId.startsWith("P")&&initialProjId.indexOf("-")>-1){
                    initialProjId=initialProjId.substring(0,initialProjId.indexOf("-")).trim();
                }
                initialProjId=initialProjId.replaceAll("P","");
                kPIPojo.setInitialProjId(initialProjId);

                if(kPIPojoRp.getSpasProjId()!=null&&kPIPojoRp.getSpasProjId().equalsIgnoreCase(initialProjId)){
                    kPIPojo.addMeType(KPIConstants.KPI_TYPE_NEW);
                    DataManagementUtil.getProperty(dmService, itemRev, "IMAN_specification");
                    ModelObject[]  specifications= itemRev.get_IMAN_specification();
                    if(specifications!=null&&specifications.length>0){
                        String featureIds="";
                        for(ModelObject spec:specifications){
                            String objType= spec.getTypeObject().getName();
                            if(!("ProPrt".equalsIgnoreCase(objType))){
                                continue;
                            }
                            if(!(spec instanceof Dataset)){
                                continue;
                            }
                            Dataset  specDataSet= (Dataset )spec;
                            DataManagementUtil.getProperty(dmService, specDataSet, "Pro2_merge");
                            ModelObject[] merges= specDataSet.getPropertyObject("Pro2_merge").getModelObjectArrayValue();
                            if(merges!=null&&merges.length>0){
                                for(ModelObject merge:merges){
                                    if(!(merge instanceof  ItemRevision)){
                                        continue;
                                    }
                                    ItemRevision  mergeRev=(ItemRevision)merge;
                                    DataManagementUtil.getProperty(dmService, mergeRev, "item_id");
                                    String featureId=mergeRev.get_item_id();
                                    if(featureId.startsWith("ME-PTFM")){
                                        DataManagementUtil.getProperty(dmService, mergeRev, "IMAN_classification");
                                        ModelObject[]  classifys=mergeRev.get_IMAN_classification();
                                        if(classifys==null||classifys.length<=0){
                                            continue;
                                        }
                                        String subType= designStandardMapper.getSubType(classifys[0].getUid());
                                        if("emi".equalsIgnoreCase(subType)||"io".equalsIgnoreCase(subType)||"hook".equalsIgnoreCase(subType)){
                                            featureIds+=featureId+"#"+subType+",";
                                            kPIPojo.setEmcType("Y");
                                        }else{
                                            featureIds += featureId + ",";
                                        }
                                    }
                                }
                            }
                        }
                        kPIPojo.setFeature(featureIds);
                        if("".equalsIgnoreCase(kPIPojo.getEmcType())&&featureIds.length()>2){
                            kPIPojo.setEmcType("N");
                        }

                    }
                }else{
                    kPIPojo.addMeType(KPIConstants.KPI_TYPE_BORROW);
                }

                DataManagementUtil.getProperty(dmService, itemRev, "d9_ActualUserID");
                String actualUserId = itemRev.getPropertyObject("d9_ActualUserID").getStringValue();
                kPIPojo.setActualUser(actualUserId);

                DataManagementUtil.getProperty(dmService, itemRev, "IMAN_classification");
                ModelObject[]  classifys=itemRev.get_IMAN_classification();
                if(classifys!=null&&classifys.length>0){
                    kPIPojo.addMeType(KPIConstants.KPI_TYPE_STANDARD);
                    String subType= designStandardMapper.getSubType(classifys[0].getUid());
                    if("psc".equalsIgnoreCase(subType)){
                        kPIPojo.setEmcType("PCI Slot Cover");
                    }
                }

                if(childId.startsWith("ME-SHLD")){
                    kPIPojo.setEmcType("SHLD");
                }

                String userRole="";
                DataManagementUtil.getProperty(dmService, itemRev, "owning_user");
                User u= (User)itemRev.get_owning_user();
                DataManagementUtil.getProperty(dmService, u, "default_group");
                Group gp=(Group)u.get_default_group();
                DataManagementUtil.getProperty(dmService, gp, "roles");
                ModelObject[] roles=gp.get_roles();
                if(roles!=null&&roles.length>0){
                    Role r= (Role)roles[0];
                    DataManagementUtil.getProperty(dmService, r, "object_name");
                    userRole=r.get_object_name();
                }
                kPIPojo.setUserRole(userRole);

                kPIPojo.setParentId(parentId);
                kPIPojo.setChildId(childId);
                kPIPojos.add(kPIPojo);
                childMP.put(childId,kPIPojo);
                if(hasChild) {
                    taskExecutor.execute(new MEBomRunnable(dmService, bomLine, kPIPojos,
                            kPIPojoRp, designStandardMapper,taskExecutor));
                }
            }

        } catch (Exception e) {
            log.error("", e);

        } finally {

        }
    }





}
