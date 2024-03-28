package com.foxconn.plm.tcsyncfolder.vo;

import lombok.Data;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.Serializable;

/**
 * @ClassName: DocumentRevisionVo
 * @Description:
 * @Author DY
 * @Create 2023/3/30
 */
@Data
@EnableScheduling
public class DocumentRevisionVo implements Serializable {
    /**
     * 文档版本属性
     */
    private String documentId;
    private String documentRevisionUid;
    private String documentRevisionName;
    private String version;
    private UserVo ownUser;
    private Integer release = 1;
}
