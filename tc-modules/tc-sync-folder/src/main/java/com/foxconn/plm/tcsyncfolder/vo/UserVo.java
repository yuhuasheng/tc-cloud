package com.foxconn.plm.tcsyncfolder.vo;

import lombok.Data;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.Serializable;

/**
 * @ClassName: UserVo
 * @Description:
 * @Author DY
 * @Create 2023/3/30
 */
@Data
@EnableScheduling
public class UserVo implements Serializable {
    private String userId;
    private String userName;
}
