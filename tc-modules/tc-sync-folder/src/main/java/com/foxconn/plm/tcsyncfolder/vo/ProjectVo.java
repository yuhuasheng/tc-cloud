package com.foxconn.plm.tcsyncfolder.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @ClassName: ProjectVo
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
@Data
@EqualsAndHashCode
public class ProjectVo implements Serializable {
    private String puid;
    private String projectName;
    private String spasId;
}
