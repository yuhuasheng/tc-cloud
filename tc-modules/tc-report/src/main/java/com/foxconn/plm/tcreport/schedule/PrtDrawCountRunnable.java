package com.foxconn.plm.tcreport.schedule;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.NumberUtil;
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
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.exceptions.NotLoadedException;
import com.xxl.job.core.context.XxlJobHelper;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * @ClassName: PrtDrawCountRunnable
 * @Description:
 * @Author DY
 * @Create 2023/4/27
 */
public class PrtDrawCountRunnable implements Runnable {
    private static Log log = LogFactory.get();
    private TCSOAServiceFactory tcsoaServiceFactory;
    private Snowflake snowflake;
    private DrawCountService service;
    private LovBean lovBean;
    private CountDownLatch countDownLatch;
    private TcProjectMapper tcProjectMapper;
    private String date;

    public PrtDrawCountRunnable(TCSOAServiceFactory tcsoaServiceFactory, Snowflake snowflake, DrawCountService service,
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
            Map<String, ModelObject> map = new HashMap<>(16);
            // 查询p3到p6阶段的文件夹
            List<String> phaseFolderNames = CollUtil.newArrayList("P3/Design/Imech", "P4/EVT", "P5/DVT/LP", "P6/PVT+MVT/PP");
            List<String> phaseFolderIds = tcProjectMapper.getChildFolderIdByNames(mePuid, phaseFolderNames);
            for (String phaseFolderId : phaseFolderIds) {
                initObjectMap(phaseFolderId,map,"ME 設計資料 Part List");
                initObjectMap(phaseFolderId,map,"ME 設計資料 Module List & Product Assy");
            }
            if (CollUtil.isEmpty(map)) {
                DrawCountEntity entity = initEntity(projectName, projectId, date);
                service.save(entity);
                return;
            }
            DrawCountEntity entity = initEntity(projectName, projectId, date);
            for (ModelObject object : map.values()) {
                ItemRevision itemRevision = null;
                // 判断对象是item还是itemrevision，如果是item则获取最新版本的itemrevision
                TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), new ModelObject[]{object},
                        new String[]{TCItemConstant.REL_REVISION_LIST, TCItemConstant.PROPERTY_OBJECT_TYPE, TCItemConstant.PROPERTY_ITEM_ID,
                                TCItemConstant.PROPERTY_OBJECT_NAME, TCItemConstant.PROPERTY_OBJECT_DESC});
                TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), object);
                String objectType = object.getPropertyObject(TCItemConstant.PROPERTY_OBJECT_TYPE).getStringValue();
                if ("D9_MEDesignRevision".equalsIgnoreCase(objectType)) {
                    itemRevision = (ItemRevision) object;
                    setUser(object, entity);
                } else if ("D9_MEDesign".equalsIgnoreCase(objectType)) {
                    List<ModelObject> objList = object.getPropertyObject(TCItemConstant.REL_REVISION_LIST).getModelObjectListValue();
                    if (CollUtil.isNotEmpty(objList)) {
                        itemRevision = (ItemRevision) objList.get(objList.size() - 1);
                    }
                } else {
                    continue;
                }
                if (ObjectUtil.isNotNull(itemRevision)) {
                    entity.setUploadNum(entity.getUploadNum() + 1);
                    if (TCUtils.isReleased(tcsoaServiceFactory.getDataManagementService(), itemRevision, new String[]{
                            TCWorkflowStatusEnum.D9_FastRelease.name(), TCWorkflowStatusEnum.D9_Release.name(), TCWorkflowStatusEnum.TCMReleased.name(), TCWorkflowStatusEnum.Released.name()})) {
                        entity.setReleaseNum(entity.getReleaseNum() + 1);
                        entity.setReleaseModelNum(entity.getReleaseModelNum() + 1);
                    }
                }
            }
            if (entity.getUploadNum() == 0) {
                entity.setReleaseProgress("0%");
            } else {
                String s = NumberUtil.roundStr(NumberUtil.div(entity.getReleaseNum() * 100, entity.getUploadNum().intValue()), 1);
                entity.setReleaseProgress(s + "%");
            }
            if (entity.getReleaseNum() == 0) {
                entity.setItemCompleteness("0%");
                entity.setDrawCompleteness("0%");
            } else {
                double value = NumberUtil.div(entity.getReleaseModelNum() * 100, entity.getReleaseNum().intValue());
                String s = NumberUtil.roundStr(value, 1);
                entity.setItemCompleteness(s + "%");
                entity.setDrawCompleteness(s + "%");
            }
            service.save(entity);
        } catch (Exception e) {
            XxlJobHelper.handleFail("--------系統机构&电子3D图档" + lovBean.getProjectInfo() + "專案數據统计任務執行異常，異常信息如下：-------------------");
            log.error(e);
        } finally {
            XxlJobHelper.log("-------" + lovBean.getProjectInfo() + "耗時：" + (System.currentTimeMillis() - start) + "毫秒-------------------");
            XxlJobHelper.log("--------系統机构&电子3D图档" + lovBean.getProjectInfo() + "專案數據统计任務執行完成-------------------");
            countDownLatch.countDown();
        }
    }

    private void setUser(ModelObject object, DrawCountEntity entity) throws Exception {
        TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), new ModelObject[]{object},
                new String[]{TCItemConstant.PROPERTY_ITEM_ID, TCItemConstant.PROPERTY_OBJECT_NAME});
        TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), object);
        entity.setItemCode(object.getPropertyObject(TCItemConstant.PROPERTY_ITEM_ID).getStringValue());
        entity.setItemName(object.getPropertyObject(TCItemConstant.PROPERTY_OBJECT_NAME).getStringValue());
        String type = FunctionUtil.itemTypeMatch(tcsoaServiceFactory.getPreferenceManagementService(), entity.getItemCode());
        if (StrUtil.isNotBlank(type)) {
            entity.setItemType(type);
        }
        DrawCountBean drawCountBean = new DrawCountBean();
        FunctionUtil.setUser(drawCountBean, tcsoaServiceFactory.getDataManagementService(), object);
        entity.setOwner(drawCountBean.getOwner());
        entity.setOwnerGroup(drawCountBean.getOwnerGroup());
        entity.setActualUser(drawCountBean.getPractitioner());
    }

    private void initObjectMap(String phaseFolderId,Map<String, ModelObject> map,String childFolderName) throws NotLoadedException {
        String folderId = tcProjectMapper.getChildFolderIdByName(phaseFolderId, childFolderName);
        if (StrUtil.isNotBlank(folderId)) {
            ModelObject object = TCUtils.findObjectByUid(tcsoaServiceFactory.getDataManagementService(), folderId);
            TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), object, "contents");
            TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), object);
            List<ModelObject> contents = object.getPropertyObject("contents").getModelObjectListValue();
            if (CollUtil.isNotEmpty(contents)) {
                for (ModelObject item : contents) {
                    TCUtils.getProperty(tcsoaServiceFactory.getDataManagementService(), item, TCItemConstant.PROPERTY_ITEM_ID);
                    TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), item);
                    String itemId = item.getPropertyObject(TCItemConstant.PROPERTY_ITEM_ID).getStringValue();
                    if (StrUtil.isNotBlank(itemId) && itemId.startsWith("ME-") && ObjectUtil.isNull(map.get(itemId))) {
                        map.put(itemId, item);
                    }
                }
            }
        }
    }


    /**
     * 初始化prt的entity
     *
     * @param projectName
     * @param projectId
     * @param date
     * @return
     */
    private DrawCountEntity initEntity(String projectName, String projectId, String date) {
        DrawCountEntity entity = new DrawCountEntity();
        BeanUtil.copyProperties(lovBean, entity);
        entity.setProjectName(projectName);
        entity.setProjectId(projectId);
        entity.setId(snowflake.nextId());
        entity.setDesignTreeType("機構設計");
        entity.setDesignTreeName("ME");
        entity.setReportDate(date);
        entity.setUploadNum(0);
        entity.setReleaseNum(0);
        entity.setReleaseModelNum(0);
        if("E3".equalsIgnoreCase(lovBean.getChassis()) || "None".equalsIgnoreCase(lovBean.getChassis())){
            entity.setItemType("/");
            entity.setItemName("/");
            entity.setItemCode("/");
        }
        return entity;
    }
}
