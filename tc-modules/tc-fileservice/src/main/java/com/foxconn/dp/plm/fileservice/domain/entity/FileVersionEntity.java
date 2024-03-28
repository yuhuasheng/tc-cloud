package com.foxconn.dp.plm.fileservice.domain.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

@Data
@ApiModel
public class FileVersionEntity {

    @ApiModelProperty(value = "文件版本Id")
    private Long fileVersionId;

    @ApiModelProperty(value = "文档版本id")
    private Long docRevId;

    @ApiModelProperty(value = "文件id")
    private Long fileId;

    @ApiModelProperty(value = "版本号")
    private String versionNum;

    @ApiModelProperty(value = "创建日期")
    private Date createDate;

    @ApiModelProperty(value = "更新日期")
    private Date lastUpdateDate;

}
