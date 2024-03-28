package com.foxconn.plm.tcreport.drawcountreport.controller;

import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.tcreport.drawcountreport.domain.TaskInfoVo;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @ClassName: TaskController
 * @Description:
 * @Author DY
 * @Create 2023/4/6
 */
@CrossOrigin
@RestController
@RequestMapping("/drawCountReport")
public class TaskController {

    @Resource(name = "commonTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;

    @GetMapping("taskInfo")
    public R<TaskInfoVo> getTaskInfo() {
        TaskInfoVo infoVo = new TaskInfoVo();
        // 活动的线程数
        int activeCount = taskExecutor.getActiveCount();
        infoVo.setActiveCount(activeCount);
        // 获取排队的任务数
        int size = taskExecutor.getThreadPoolExecutor().getQueue().size();
        infoVo.setSize(size);
        // 执行完成的任务数
        long completedTaskCount = taskExecutor.getThreadPoolExecutor().getCompletedTaskCount();
        infoVo.setCompletedTaskCount(completedTaskCount);
        // 核心线程数
        int corePoolSize = taskExecutor.getThreadPoolExecutor().getCorePoolSize();
        infoVo.setCorePoolSize(corePoolSize);
        return R.success(infoVo);
    }

}
