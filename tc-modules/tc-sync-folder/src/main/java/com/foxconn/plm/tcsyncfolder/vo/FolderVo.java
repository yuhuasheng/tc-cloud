package com.foxconn.plm.tcsyncfolder.vo;

import lombok.Data;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.Serializable;

/**
 * @ClassName: FolderVo
 * @Description:
 * @Author DY
 * @Create 2023/3/28
 */
@Data
@EnableScheduling
public class FolderVo implements Serializable {
    private String puid;
    private String folderName;
    private String folderType;
    private String folderDesc;
}
