package com.foxconn.plm.tcsyncfolder.service.impl;

import com.foxconn.plm.tcsyncfolder.service.TaskService;
import com.foxconn.plm.tcsyncfolder.vo.TaskInfoVo;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @ClassName: TaskServiceImpl
 * @Description:
 * @Author DY
 * @Create 2023/4/6
 */
@Service
public class TaskServiceImpl implements TaskService {

    @Resource(name = "commonTaskExecutor")
    private ThreadPoolTaskExecutor taskExecutor;


    @Override
    public TaskInfoVo getTaskInfo() {
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
        return infoVo;
    }
}
