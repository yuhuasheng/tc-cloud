package com.foxconn.plm.tcsyncfolder.schedule;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCSearchEnum;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcsyncfolder.mapper.TcProjectMapper;
import com.foxconn.plm.tcsyncfolder.vo.ProjectVo;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.administration.PreferenceManagementService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.Folder;
import com.teamcenter.soa.client.model.strong.TC_Project;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * @ClassName: SyncFolderScheduling
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
//@EnableScheduling
@Component
public class SyncFolderScheduling {
    private static Log log = LogFactory.get();

    @Resource(name = "commonTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;
    @Resource
    private TCSOAServiceFactory tcsoaServiceFactory;
    @Resource
    private TcProjectMapper tcProjectMapper;
    @Value("${spring.profiles.active}")
    private String env;

    //@Scheduled(cron = "0 38 12 * * ?")
    @XxlJob("syncFolderScheduling")
    public void syncFolder() {
        log.info("--------------------> 专案信息同步开始  <-------------------");
        // 查询 TC中的专案数量
        List<ProjectVo> projectList = tcProjectMapper.getAllProject();
        if (CollUtil.isEmpty(projectList)) {
            log.warn("未查询到TC中的专案信息");
        }
        projectList = projectList.parallelStream().sorted(Comparator.comparing(ProjectVo::getSpasId).reversed()).collect(Collectors.toList());
/*        Set<String> set = CollUtil.newHashSet("p618");
        projectList = projectList.parallelStream().filter(item -> set.contains(item.getSpasId())).collect(Collectors.toList());*/
        tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS2);
        // 查询无账号部门
        Map<String, Set<String>> noAccountDeptMap = getNoAccountDeptMap(tcsoaServiceFactory.getPreferenceManagementService());
        if (CollUtil.isEmpty(noAccountDeptMap)) {
            log.warn("未查询到TC中的无账号部门");
        }
        CountDownLatch countDownLatch = new CountDownLatch(projectList.size());
        for (ProjectVo projectVo : projectList) {
            String bu = getProjectBu(projectVo.getSpasId());
            try {
                TC_Project projectObj = null;
                // 查询项目对象
                Map<String, Object> queryResults = TCUtils.executeQuery(tcsoaServiceFactory.getSavedQueryService(), "__D9_Find_Project",
                        new String[]{"project_id"}, new String[]{projectVo.getSpasId()});
                if (queryResults.get("succeeded") == null) {
                    log.error("未查询到项目信息, -------------------->" + JSONUtil.toJsonStr(projectVo));
                }
                ModelObject[] objs = (ModelObject[]) queryResults.get("succeeded");
                if (objs.length > 0) {
                    projectObj = (TC_Project) objs[0];
                    // 23/5/9修改 查询专案活动状态, 在TC中已经关闭的专案不再同步
                    ModelObject[] runningObjects = TCUtils.executequery(tcsoaServiceFactory.getSavedQueryService(), tcsoaServiceFactory.getDataManagementService(), TCSearchEnum.D9_Find_Running_Project.queryName(),
                            TCSearchEnum.D9_Find_Running_Project.queryParams(), new String[]{projectVo.getSpasId()});
                    if (runningObjects == null || runningObjects.length <= 0) {
                        log.info("专案不是活动状态-------------------->" + JSONUtil.toJsonStr(projectVo));
                        countDownLatch.countDown();
                        continue;
                    }
                }
                taskExecutor.execute(new SyncFolderRunnable(projectVo, countDownLatch, noAccountDeptMap,
                        tcProjectMapper, bu, tcsoaServiceFactory, projectObj, env));
            } catch (Exception e) {
                log.error("查询TC专案信息失败", e);
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

        log.info("--------------------> 专案信息同步结束  <-------------------");
    }

    private String getProjectBu(String spasId) {
        try {
            Map<String, Object> queryResults = TCUtils.executeQuery(tcsoaServiceFactory.getSavedQueryService(), "__D9_Find_Series_Folder",
                    new String[]{"D9_PlatformFoundFolder:contents.d9_SPAS_ID"}, new String[]{spasId});
            if (queryResults.get("succeeded") == null) {
                throw new Exception("【" + spasId + "】专案未找到BU.");
            }
            ModelObject[] queryResult = (ModelObject[]) queryResults.get("succeeded");
            Folder folder = (Folder) queryResult[0];
            return TCUtils.getPropStr(tcsoaServiceFactory.getDataManagementService(), folder, "object_desc");
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Set<String>> getNoAccountDeptMap(PreferenceManagementService preferenceManagementService) {
        Map<String, Set<String>> map = new HashMap<>();
        try {
            String[] noTcAccDept = TCUtils.getTCPreferences(preferenceManagementService, "D9_TC_NoAccount_Department");
            for (int i = 0; i < noTcAccDept.length; i++) {
                String[] buAndDept = noTcAccDept[i].split("=");
                String bu = buAndDept[0];
                String dept = buAndDept[1];
                Set<String> deptList = CollUtil.newHashSet(Arrays.asList(dept.split(",")));
                map.put(bu, deptList);
            }
        } catch (Exception e) {
            log.error("查询无账号部门失败");
        }
        return map;
    }
}
