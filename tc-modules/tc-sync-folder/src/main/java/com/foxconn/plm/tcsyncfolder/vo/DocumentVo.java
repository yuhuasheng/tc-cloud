package com.foxconn.plm.tcsyncfolder.vo;

import lombok.Data;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.Serializable;
import java.util.List;

/**
 * @ClassName: DocumentVersionVo
 * @Description:
 * @Author DY
 * @Create 2023/3/29
 */
@Data
@EnableScheduling
public class DocumentVo implements Serializable {
    private String documentUid;
    private String documentNum;
    private String documentName;
    private String documentType;
    private UserVo ownUser;
    private List<DocumentRevisionVo> documentRevisionVoList;
}
