package com.foxconn.plm.tcreport.drawcountreport.domain;

import lombok.Data;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.Serializable;

/**
 * @ClassName: TaskInfoVo
 * @Description:
 * @Author DY
 * @Create 2023/4/6
 */
@Data
@EnableScheduling
public class TaskInfoVo implements Serializable {
    private Integer activeCount;
    private Integer size;
    private Long completedTaskCount;
    private Integer corePoolSize;
}
