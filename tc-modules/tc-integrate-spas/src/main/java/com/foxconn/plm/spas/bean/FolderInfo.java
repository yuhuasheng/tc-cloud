package com.foxconn.plm.spas.bean;

import io.swagger.models.auth.In;
import lombok.Data;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/12/28/ 11:16
 * @description
 */
@Data
public class FolderInfo {

    private Integer parentFolderId;
    private Integer id;
    private String name;
    private Integer isActive;
    private Integer struId;
    private String uid;
    private String descr;

}
