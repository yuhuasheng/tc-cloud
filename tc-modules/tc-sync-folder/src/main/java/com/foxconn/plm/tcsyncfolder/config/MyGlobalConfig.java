package com.foxconn.plm.tcsyncfolder.config;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @ClassName: MyGlobalConfig
 * @Description:
 * @Author DY
 * @Create 2023/1/16
 */
@Configuration
public class MyGlobalConfig {

    @Bean("commonTaskExecutor")
    public ThreadPoolTaskExecutor commonThreadPool() {
        int core = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        // 设置核心线程数
        pool.setCorePoolSize(2 * core + 1);
        // 设置最大线程数
        pool.setMaxPoolSize(20);
        // 设置工作队列大小
        pool.setQueueCapacity(2000);
        // 设置线程空闲时间
        pool.setKeepAliveSeconds(60);
        // 设置线程名称前缀
        pool.setThreadNamePrefix("threadPoolTaskExecutor -->");
        // 设置拒绝策略,线程数达到最大线程时使用调用线程执行该任务
        pool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        pool.setWaitForTasksToCompleteOnShutdown(true);
        pool.setAwaitTerminationSeconds(60);
        // 初始化线程池
        pool.initialize();
        return pool;
    }
}
