package com.foxconn.plm.tcreport.schedule;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.*;
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
import com.xxl.job.core.context.XxlJobHelper;

import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * @ClassName: DrawCountRunnable
 * @Description:
 * @Author DY
 * @Create 2023/1/30
 */
public class DrawCountRunnable implements Runnable {
    private static Log log = LogFactory.get();
    private TCSOAServiceFactory tcsoaServiceFactory;
    private Snowflake snowflake;
    private DrawCountService service;
    private LovBean lovBean;
    private CountDownLatch countDownLatch;
    private TcProjectMapper tcProjectMapper;
    private String date;

    public DrawCountRunnable(TCSOAServiceFactory tcsoaServiceFactory, Snowflake snowflake, DrawCountService service, LovBean lovBean,
                             CountDownLatch countDownLatch, TcProjectMapper tcProjectMapper, String date) {
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
        XxlJobHelper.log("--------系統机构&电子3D图档" + lovBean.getProjectInfo() + "專案數據同步任務開始執行-------------------");
        long start = System.currentTimeMillis();
        try {
            List<DrawCountBean> totalList = new ArrayList<>();
            String projectName = lovBean.getProjectInfo().substring(lovBean.getProjectInfo().indexOf("-") + 1);
            String projectId = lovBean.getProjectInfo().substring(0, lovBean.getProjectInfo().indexOf("-"));
            // 查询该专案是否是running状态
            ModelObject[] runningObjects = TCUtils.executequery(tcsoaServiceFactory.getSavedQueryService(), tcsoaServiceFactory.getDataManagementService(), TCSearchEnum.D9_Find_Running_Project.queryName(),
                    TCSearchEnum.D9_Find_Running_Project.queryParams(), new String[]{projectId});
            if (runningObjects == null || runningObjects.length <= 0) {
                XxlJobHelper.log("--------系統机构&电子3D图档" + lovBean.getProjectInfo() + "專案不是running状态，數據同步任務執行完成-------------------");
                countDownLatch.countDown();
                return;
            }
            String pUid = tcProjectMapper.getFolderIdBySpasId(projectId);
            if(StrUtil.isNotBlank(pUid) && TCUtils.isDiscard(tcsoaServiceFactory.getDataManagementService(),pUid)){
                XxlJobHelper.log("--------系統机构&电子3D图档" + lovBean.getProjectInfo() + "專案已经废弃，數據统计任務執行完成-------------------");
                countDownLatch.countDown();
                return;
            }
            ModelObject[] modelObjects = TCUtils.executequery(tcsoaServiceFactory.getSavedQueryService(), tcsoaServiceFactory.getDataManagementService(), TCSearchEnum.D9_Find_ProductNode.queryName(),
                    TCSearchEnum.D9_Find_ProductNode.queryParams(), new String[]{projectId});
            if (modelObjects == null || modelObjects.length <= 0) {
                XxlJobHelper.log("--------系統机构&电子3D图档" + lovBean.getProjectInfo() + "專案沒有協同結構樹，數據同步任務執行完成-------------------");
                countDownLatch.countDown();
                // 將空数据保存到数据库中
                saveDefaultDate(projectName,projectId);
                return;
            }
            TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), modelObjects);
            List<ModelObject> list = CollUtil.newArrayList(modelObjects);
            Map<ModelObject, List<LovBean>> projectMap = new HashMap<>();
            list.removeIf(obj -> {
                try {
                    if (!(obj instanceof ItemRevision)) {
                        return true;
                    }
                    boolean bom = StructureManagementUtil.isBom(tcsoaServiceFactory.getDataManagementService(), (ItemRevision) obj);
                    if (!bom) {
                        return true;
                    }

                    ModelObject item = TCUtils.getPropModelObject(tcsoaServiceFactory.getDataManagementService(), obj, TCItemConstant.REL_ITEMS_TAG);
                    TCUtils.refreshObject(tcsoaServiceFactory.getDataManagementService(), item);
                    ModelObject[] propModelObjectArray = TCUtils.getPropModelObjectArray(tcsoaServiceFactory.getDataManagementService(), item, "project_list");
                    if (propModelObjectArray == null || propModelObjectArray.length == 0) {
                        return true;
                    }
                    List<LovBean> projectList = new ArrayList<>(propModelObjectArray.length);
                    for (ModelObject modelObject : propModelObjectArray) {
                        if (ObjectUtil.isNull(modelObject)) {
                            continue;
                        }
                        String Id = TCUtils.getPropStr(tcsoaServiceFactory.getDataManagementService(), modelObject, TCProjectConstant.PROPERTY_PROJECT_ID);
                        if (StrUtil.isNotBlank(Id) && Id.equalsIgnoreCase(projectId)) {
                            projectList.add(lovBean);
                        }
                    }
                    if (CollUtil.isEmpty(projectList)) {
                        return true;
                    }
                    projectMap.put(obj, projectList);
                } catch (Exception e) {
                    XxlJobHelper.handleFail("過濾沒有bom的專案出錯，錯誤原因：" + e.getMessage());
                    log.error(e);
                }
                return false;
            });


            list.parallelStream().forEach(obj -> {
                try {
                    List<DrawCountBean> resultList = FunctionUtil.sendPSE(tcsoaServiceFactory.getPreferenceManagementService(), tcsoaServiceFactory.getStructureManagementService(), tcsoaServiceFactory.getStructureService(),
                            tcsoaServiceFactory.getDataManagementService(), obj);
                    if (resultList != null && resultList.size() > 0) {
                        List<LovBean> projectList = projectMap.get(obj);
                        for (LovBean lovBean : projectList) {
                            for (DrawCountBean item : resultList) {
                                DrawCountBean bean = ObjectUtil.clone(item);
                                bean.setBu(lovBean.getBu());
                                bean.setCustomer(lovBean.getCustomer());
                                bean.setProductLine(lovBean.getProductLine());
                                bean.setProjectSeries(lovBean.getProjectSeries());
                                bean.setProjectName(projectName);
                                bean.setProjectId(projectId);
                                bean.setChassis(lovBean.getChassis());
                                bean.setPhase(lovBean.getPhase());
                                totalList.add(bean);
                            }
                        }
                    }
                } catch (Exception e) {
                    XxlJobHelper.handleFail("講查詢出來的專案信息賦值出錯，錯誤原因：" + e.getMessage());
                    log.error(e);
                }
            });
            if (CollUtil.isEmpty(totalList)) {
                XxlJobHelper.log("--------系統机构&电子3D图档" + lovBean.getProjectInfo() + "專案沒有協同結構樹，數據同步任務執行完成-------------------");
                countDownLatch.countDown();
                // 將空数据保存到数据库中
                saveDefaultDate(projectName,projectId);
                return;
            }
            List<DrawCountEntity> entityList = FunctionUtil.transToEntity(totalList);
            for (DrawCountEntity entity : entityList) {
                entity.setId(snowflake.nextId());
                entity.setReportDate(date);
                service.save(entity);
            }
        } catch (Exception e) {
            XxlJobHelper.handleFail("--------系統机构&电子3D图档" + lovBean.getProjectInfo() + "專案數據同步任務執行異常，異常信息如下：-------------------");
            log.error(e);
        }
        XxlJobHelper.log("-------" + lovBean.getProjectInfo() + "耗時：" + (System.currentTimeMillis() - start) + "毫秒-------------------");
        XxlJobHelper.log("--------系統机构&电子3D图档" + lovBean.getProjectInfo() + "專案數據同步任務執行完成-------------------");
        countDownLatch.countDown();
    }


    private void saveDefaultDate(String projectName,String projectId){
        DrawCountEntity entity = new DrawCountEntity();
        BeanUtil.copyProperties(lovBean, entity);
        entity.setProjectName(projectName);
        entity.setProjectId(projectId);
        entity.setId(snowflake.nextId());
        entity.setReportDate(date);
        if("E3".equalsIgnoreCase(lovBean.getChassis()) || "None".equalsIgnoreCase(lovBean.getChassis())){
            entity.setItemType("/");
            entity.setItemName("/");
            entity.setItemCode("/");
        }
        service.save(entity);
    }
}
