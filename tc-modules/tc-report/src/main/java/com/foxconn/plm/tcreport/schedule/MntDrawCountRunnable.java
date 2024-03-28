package com.foxconn.plm.tcreport.schedule;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCItemConstant;
import com.foxconn.plm.entity.constants.TCSearchEnum;
import com.foxconn.plm.entity.constants.TCWorkflowStatusEnum;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcreport.drawcountreport.domain.DrawCountBean;
import com.foxconn.plm.tcreport.drawcountreport.domain.DrawCountEntity;
import com.foxconn.plm.tcreport.drawcountreport.service.DrawCountService;
import com.foxconn.plm.tcreport.mapper.TcProjectMapper;
import com.foxconn.plm.tcreport.reportsearchparams.domain.LovBean;
import com.foxconn.plm.tcreport.utils.FunctionUtil;
import com.foxconn.plm.utils.tc.StructureManagementUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.exceptions.NotLoadedException;
import com.xxl.job.core.context.XxlJobHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * @ClassName: MNTDrawCountRunnable
 * @Description:
 * @Author DY
 * @Create 2023/4/27
 */
public class MntDrawCountRunnable implements Runnable {
    private static Log log = LogFactory.get();
    private static Set<String> set = CollUtil.newHashSet("E2(B)","E2(C)","E3(E)","E3(F)");
    private TCSOAServiceFactory tcsoaServiceFactory;
    private Snowflake snowflake;
    private DrawCountService service;
    private LovBean lovBean;
    private CountDownLatch countDownLatch;
    private TcProjectMapper tcProjectMapper;
    private String date;

    public MntDrawCountRunnable(TCSOAServiceFactory tcsoaServiceFactory, Snowflake snowflake, DrawCountService service,
                                LovBean lovBean, CountDownLatch countDownLatch, TcProjectMapper tcProjectMapper,String date) {
        this.tcsoaServiceFactory = tcsoaServiceFactory;
        this.snowflake = snowflake;
        this.service = service;
        this.lovBean = lovBean;
        this.countDownLatch = countDownLatch;
        this.tcProjectMapper = tcProjectMapper;
        this.date = date;
    }

    @Override
    public void run() {
        XxlJobHelper.log("--------系統机构&电子3D图档" + lovBean.getProjectInfo() + "專案數據统计任務開始執行-------------------");
        long start = System.currentTimeMillis();
        try {
            String projectName = lovBean.getProjectInfo().substring(lovBean.getProjectInfo().indexOf("-") + 1);
            String projectId = lovBean.getProjectInfo().substring(0, lovBean.getProjectInfo().indexOf("-"));
            // 查询该专案是否是running状态
            ModelObject[] runningObjects = TCUtils.executequery(tcsoaServiceFactory.getSavedQueryService(), tcsoaServiceFactory.getDataManagementService(), TCSearchEnum.D9_Find_Running_Project.queryName(),
                    TCSearchEnum.D9_Find_Running_Project.queryParams(), new String[]{projectId});
            if (runningObjects == null || runningObjects.length <= 0) {
                XxlJobHelper.log("--------系統机构&电子3D图档" + lovBean.getProjectInfo() + "專案不是running状态，數據统计任務執行完成-------------------");
                return;
            }
            String pUid = tcProjectMapper.getFolderIdBySpasId(projectId);
            if(StrUtil.isNotBlank(pUid) && TCUtils.isDiscard(tcsoaServiceFactory.getDataManagementService(),pUid)){
                XxlJobHelper.log("--------系統机构&电子3D图档" + lovBean.getProjectInfo() + "專案已经废弃，數據统计任務執行完成-------------------");
                return;
            }
            // 查询专案的ME文件夹uuid
            String mePuid = tcProjectMapper.getFolderIdBySpasIdAndName(projectId, "ME");
            if (StrUtil.isBlank(mePuid)) {
                XxlJobHelper.log("--------系統机构&电子3D图档" + lovBean.getProjectInfo() + "專案未查询到ME文件夹信息，數據统计任務執行完成-------------------");
                return;
            }
            // 查询p5阶段的零组件
            List<ModelObject> contentsObj = getContentsObj(mePuid, "P5(DVT)");
            if (CollUtil.isEmpty(contentsObj)) {
                contentsObj = getContentsObj(mePuid, "P3(design)");
            }
            if (CollUtil.isEmpty(contentsObj)) {
                // 没有存放任何零组件，直接保存数据
                saveEntity(projectName, projectId, date);
                XxlJobHelper.log("--------系統机构&电子3D图档" + lovBean.getProjectInfo() + "專案沒有協同結構樹，數據同步任務執行完成-------------------");
                return;
            }
            List<DrawCountBean> totalList = new ArrayList<>();
            for (ModelObject object : contentsObj) {
                ItemRevision itemRevision = null;
                // 判断对象是item还是itemrevision，如果是item则获取最新版本的itemrevision
                TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), new ModelObject[]{object},
                        new String[]{TCItemConstant.REL_REVISION_LIST, TCItemConstant.PROPERTY_OBJECT_TYPE, TCItemConstant.PROPERTY_ITEM_ID,
                                TCItemConstant.PROPERTY_OBJECT_NAME, TCItemConstant.PROPERTY_OBJECT_DESC});
                TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), object);
                String objectType = object.getPropertyObject(TCItemConstant.PROPERTY_OBJECT_TYPE).getStringValue();
                if ("D9_MEDesignRevision".equalsIgnoreCase(objectType)) {
                    itemRevision = (ItemRevision) object;
                } else if ("D9_MEDesign".equalsIgnoreCase(objectType)) {
                    List<ModelObject> objList = object.getPropertyObject(TCItemConstant.REL_REVISION_LIST).getModelObjectListValue();
                    if (CollUtil.isEmpty(objList)) {
                        DrawCountBean drawCountBean =getDrawCountBean(object,projectName,projectId);
                        totalList.add(drawCountBean);
                    } else {
                        itemRevision = (ItemRevision) objList.get(objList.size() - 1);
                    }
                } else {
                    continue;
                }
                boolean bom = StructureManagementUtil.isBom(tcsoaServiceFactory.getDataManagementService(), itemRevision);
                if (bom) {
                    DrawCountBean drawCountBean = FunctionUtil.mntCount(tcsoaServiceFactory.getPreferenceManagementService(), tcsoaServiceFactory.getStructureManagementService(), tcsoaServiceFactory.getStructureService()
                            , tcsoaServiceFactory.getDataManagementService(), itemRevision);
                    if (ObjectUtil.isNull(drawCountBean)) {
                        drawCountBean = new DrawCountBean();
                        FunctionUtil.setUser(drawCountBean, tcsoaServiceFactory.getDataManagementService(), itemRevision);
                    }
                    BeanUtil.copyProperties(lovBean, drawCountBean);
                    drawCountBean.setProjectName(projectName);
                    drawCountBean.setProjectId(projectId);
                    totalList.add(drawCountBean);
                } else {
                    DrawCountBean drawCountBean = getDrawCountBean(itemRevision,projectName,projectId);
                    if (TCUtils.isReleased(tcsoaServiceFactory.getDataManagementService(), itemRevision, new String[]{
                            TCWorkflowStatusEnum.D9_FastRelease.name(), TCWorkflowStatusEnum.D9_Release.name(), TCWorkflowStatusEnum.TCMReleased.name(), TCWorkflowStatusEnum.Released.name()})) {
                        drawCountBean.setReleaseNum(1);
                        int modelNum = FunctionUtil.getModelNum(tcsoaServiceFactory.getDataManagementService(), itemRevision);
                        drawCountBean.setReleaseModelNum(modelNum);
                    }
                    totalList.add(drawCountBean);
                }
            }
            List<DrawCountEntity> entityList = FunctionUtil.transToEntity(totalList);
            for (DrawCountEntity entity : entityList) {
                entity.setId(snowflake.nextId());
                entity.setReportDate(date);
                entity.setDesignTreeType("機構設計");
                entity.setDesignTreeName("ME");
                service.save(entity);
            }
        } catch (Exception e) {
            XxlJobHelper.handleFail("--------系統机构&电子3D图档" + lovBean.getProjectInfo() + "專案數據统计任務執行異常，異常信息如下：-------------------");
            log.error(e);
        } finally {
            XxlJobHelper.log("-------" + lovBean.getProjectInfo() + "耗時：" + (System.currentTimeMillis() - start) + "毫秒-------------------");
            XxlJobHelper.log("--------系統机构&电子3D图档" + lovBean.getProjectInfo() + "專案數據统计任務執行完成-------------------");
            countDownLatch.countDown();
        }
    }

    private DrawCountBean getDrawCountBean(ModelObject object,String projectName, String projectId) throws Exception {
        DrawCountBean drawCountBean = new DrawCountBean();
        FunctionUtil.setUser(drawCountBean, tcsoaServiceFactory.getDataManagementService(), object);
        BeanUtil.copyProperties(lovBean, drawCountBean);
        drawCountBean.setProjectName(projectName);
        drawCountBean.setProjectId(projectId);
        drawCountBean.setDesignTreeType("機構設計");
        drawCountBean.setDesignTreeName("ME");
        drawCountBean.setItemCode(object.getPropertyObject(TCItemConstant.PROPERTY_ITEM_ID).getStringValue());
        drawCountBean.setItemName(object.getPropertyObject(TCItemConstant.PROPERTY_OBJECT_NAME).getStringValue());
        String type = FunctionUtil.itemTypeMatch(tcsoaServiceFactory.getPreferenceManagementService(), drawCountBean.getItemCode());
        if (StrUtil.isNotBlank(type)) {
            drawCountBean.setItemType(type);
        }
        drawCountBean.setUploadNum(1);
        return drawCountBean;
    }

    /**
     * 将没有数据的专案信息保存到数据库中
     *
     * @param projectName
     * @param projectId
     */
    private void saveEntity(String projectName, String projectId, String date) {
        DrawCountEntity entity = new DrawCountEntity();
        BeanUtil.copyProperties(lovBean, entity);
        entity.setProjectName(projectName);
        entity.setProjectId(projectId);
        entity.setId(snowflake.nextId());
        entity.setDesignTreeType("機構設計");
        entity.setDesignTreeName("ME");
        entity.setReportDate(date);
        if(set.contains(lovBean.getChassis())){
            entity.setItemCode("/");
            entity.setItemName("/");
            entity.setItemType("/");
        }
        service.save(entity);
    }

    /**
     * 查询Me文件夹下的指定P5或者P3阶段存放的零组件
     *
     * @param mePuid
     * @param childFolderName
     * @return
     */
    private List<ModelObject> getContentsObj(String mePuid, String childFolderName) throws NotLoadedException {
        String childFolderPuid = tcProjectMapper.getChildFolderIdByName(mePuid, childFolderName);
        if (StrUtil.isNotBlank(childFolderPuid)) {
            // 查询阶段下的指定文件夹
            String folderPuid = null;
            if (childFolderName.startsWith("P5")) {
                folderPuid = tcProjectMapper.getChildFolderIdByName(childFolderPuid, "ME Sub-Ass'y drawing");
            } else {
                folderPuid = tcProjectMapper.getChildFolderIdByName(childFolderPuid, "ME 3D design drawing");
            }
            if (StrUtil.isNotBlank(folderPuid)) {
                // 查询文件夹下的零组件
                ModelObject obj = TCUtils.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), folderPuid);
                TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), obj, "contents");
                TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), obj);
                List<ModelObject> contents = obj.getPropertyObject("contents").getModelObjectListValue();
                return contents.parallelStream().filter(object -> {
                    TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), object, TCItemConstant.PROPERTY_ITEM_ID);
                    TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), object);
                    try {
                        String itemId = object.getPropertyObject(TCItemConstant.PROPERTY_ITEM_ID).getStringValue();
                        if (ObjectUtil.isNull(itemId)) {
                            return false;
                        }
                        return itemId.startsWith("ME-");
                    } catch (NotLoadedException e) {
                        return false;
                    }
                }).collect(Collectors.toList());
            }
        }
        return null;
    }

}
