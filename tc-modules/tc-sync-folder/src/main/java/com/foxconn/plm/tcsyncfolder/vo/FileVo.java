package com.foxconn.plm.tcsyncfolder.vo;

import lombok.Data;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.Serializable;

/**
 * @ClassName: FileVo
 * @Description:
 * @Author DY
 * @Create 2023/3/30
 */
@Data
@EnableScheduling
public class FileVo implements Serializable {

    private String fileVersionSn;

    private String fileId;

    private String fileName;

    private String fileType;
}
