package com.foxconn.plm.tcreport.schedule;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcreport.drawcountreport.service.DrawCountService;
import com.foxconn.plm.tcreport.mapper.ReportSearchMapper;
import com.foxconn.plm.tcreport.mapper.TcProjectMapper;
import com.foxconn.plm.tcreport.reportsearchparams.domain.LovBean;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CountDownLatch;


/**
 * @ClassName: DrawCountSchedule
 * @Description:
 * @Author DY
 * @Create 2023/1/16
 */
@Component
public class DrawCountSchedule {
    private static Log log = LogFactory.get();
    @Resource
    private ReportSearchMapper reportSearchMapper;
    @Resource
    private TCSOAServiceFactory tcsoaServiceFactory;
    @Resource
    private Snowflake snowflake;
    @Resource
    private DrawCountService service;
    @Resource(name = "commonTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;
    @Resource
    private TcProjectMapper tcProjectMapper;

    @XxlJob("drawCountSchedule")
    public void drawCount() {
        log.info(DateUtil.now() + "開始執行結構樹定時任務");
        List<LovBean> paramList = reportSearchMapper.getLovByParam(null, null, null, null, null, null);
        if (CollUtil.isEmpty(paramList)) {
            log.info(DateUtil.now() + "未查询到专案信息，結構樹定時任務執行完成");
            return;
        }
        // 初始化TC，登錄dev賬號
        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS4);
        CountDownLatch countDownLatch = new CountDownLatch(paramList.size());
        //String date = DateUtil.format(DateUtil.yesterday(), "yyyy-MM-dd");
        String date = DateUtil.today();
        for (LovBean lovBean : paramList) {
            // 查询专案阶段和专案类别
            try {
                initProjectChassis(lovBean);
            } catch (Exception e) {
                log.error("查詢" + JSONUtil.toJsonStr(lovBean) + "專案的階段和類別出錯");
            }
            // 查询协同结构树
            switch (lovBean.getBu()) {
                case "DT":
                    taskExecutor.execute(new DrawCountRunnable(tcsoaServiceFactory, snowflake, service, lovBean, countDownLatch, tcProjectMapper, date));
                    break;
                case "MNT":
                    taskExecutor.execute(new MntDrawCountRunnable(tcsoaServiceFactory, snowflake, service, lovBean, countDownLatch, tcProjectMapper, date));
                    break;
                case "PRT":
                    taskExecutor.execute(new PrtDrawCountRunnable(tcsoaServiceFactory, snowflake, service, lovBean, countDownLatch, tcProjectMapper, date));
                    break;
                default:
                    countDownLatch.countDown();
                    break;
            }
        }
        try {
            // 等待計數器歸零
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 登出TC
        tcsoaServiceFactory.logout();
        log.info(DateUtil.now() + "結構樹定時任務執行完成");
    }

    private void initProjectChassis(LovBean lovBean) {
        String projectId = lovBean.getProjectInfo().substring(1, lovBean.getProjectInfo().indexOf("-"));
        // 判斷項目是新項目還是舊項目，
        String process = reportSearchMapper.getProjectInfo(projectId);
        if ("2".equals(process)) {
            initNewProjectChassis(lovBean);
        } else {
            initOldProjectChassis(lovBean);
        }
    }

    private void initOldProjectChassis(LovBean lovBean) {
        String projectId = lovBean.getProjectInfo().substring(1, lovBean.getProjectInfo().indexOf("-"));
        String chassis = null;
        switch (lovBean.getBu()) {
            case "DT":
                chassis = reportSearchMapper.getChassisByProjectId(projectId);
                break;
            case "MNT":
                chassis = reportSearchMapper.getMonitorChassisByProjectId(projectId, lovBean.getProductLine(), "Level");
                break;
            case "PRT":
                chassis = reportSearchMapper.getMonitorChassisByProjectId(projectId, lovBean.getProductLine(), "Board");
                break;
            default:
                break;
        }
        if (StrUtil.isNotBlank(chassis)) {
            lovBean.setChassis(chassis);
        }
        List<Map<String, Object>> phaseList = reportSearchMapper.getPhaseByProjectId(projectId);
        if (CollUtil.isNotEmpty(phaseList)) {
            for (int i = 0; i < phaseList.size(); i++) {
                Map<String, Object> objectMap = phaseList.get(i);
                String phaseSn = objectMap.get("PHASE_SN").toString();
                Object startObj = objectMap.get("START_TIME");
                Object endObj = objectMap.get("END_TIME");
                if (startObj == null || endObj == null) {
                    continue;
                }
                String start = startObj.toString();
                String end = endObj.toString();
                if (StrUtil.isBlank(start) || StrUtil.isBlank(end)) {
                    continue;
                }
                DateTime startTime = DateUtil.beginOfDay(DateUtil.parse(start));
                DateTime endTime = DateUtil.endOfDay(DateUtil.parse(end));
                long now = DateUtil.date().getTime();
                if (startTime.getTime() <= now && now <= endTime.getTime()) {
                    lovBean.setPhase(phaseSn + "\r\t(" + DateUtil.format(startTime, "yyyy/MM/dd") + "-" + DateUtil.format(endTime, "yyyy/MM/dd") + ")");
                    break;
                }
                if (i == phaseList.size() - 1 && StrUtil.isBlank(lovBean.getPhase())) {
                    lovBean.setPhase(phaseSn + "\r\t(" + DateUtil.format(startTime, "yyyy/MM/dd") + "-" + DateUtil.format(endTime, "yyyy/MM/dd") + ")");
                }
            }
        }
    }

    public void initNewProjectChassis(LovBean lovBean) {
        String projectId = lovBean.getProjectInfo().substring(1, lovBean.getProjectInfo().indexOf("-"));
        String attr = "";
        if ("DT".equals(lovBean.getBu())) {
            attr = "Chassis";
        } else if ("MNT".equals(lovBean.getBu())) {
            attr = "Level";
        } else if ("PRT".equals(lovBean.getBu())) {
            attr = "Board";
        }
        List<Map<String, Object>> list = reportSearchMapper.getPhaseByProjectIdAndAttribute(projectId);
        if (CollUtil.isEmpty(list)) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> objectMap = list.get(i);
            String phaseSn = objectMap.get("PHASE").toString();
            Object startObj = objectMap.get("START_TIME");
            Object endObj = objectMap.get("END_TIME");
            if (startObj == null || endObj == null) {
                continue;
            }
            String start = startObj.toString();
            String end = endObj.toString();
            if (StrUtil.isBlank(start) || StrUtil.isBlank(end)) {
                continue;
            }
            DateTime startTime = DateUtil.beginOfDay(DateUtil.parse(start));
            DateTime endTime = DateUtil.endOfDay(DateUtil.parse(end));
            long now = DateUtil.date().getTime();
            if (startTime.getTime() <= now && now <= endTime.getTime()) {
                lovBean.setPhase(phaseSn + "\r\t(" + DateUtil.format(startTime, "yyyy/MM/dd") + "-" + DateUtil.format(endTime, "yyyy/MM/dd") + ")");
            }else{
                continue;
            }
            Object attribute = objectMap.get("ATTRIBUTE");
            if(ObjectUtil.isNull(attribute)){
                continue;
            }
            Object value = objectMap.get("CATEGORY_NAME");
            if(attr.equals(String.valueOf(attribute)) && ObjectUtil.isNotNull(value)){
                lovBean.setChassis(String.valueOf(value));
                break;
            }
        }
    }

}
