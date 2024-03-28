package com.foxconn.dp.plm.fileservice.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Date;

@Data
@ApiModel
public class FileEntity {

    @ApiModelProperty(value = "文件夹Id")
    private Long folderId;

    @ApiModelProperty(value = "文件id")
    private Long fileId;

    @ApiModelProperty(value = "文件版本Id")
    private Long fileVersionId;

    @ApiModelProperty(value = "文件名")
    private String fileName;

    @ApiModelProperty(value = "文件类型")
    private String fileType;

    @ApiModelProperty(value = "文件大小")
    private Long fileSize;

    @ApiModelProperty(value = "源服务器Id，1：武漢；2：重慶；3：龍華；4：台北；5：新竹")
    private Integer serverId;

    @ApiModelProperty(value = "文件路径")
    private String filePath;

    @ApiModelProperty(value = "0：未删除；1：已删除")
    private Integer delFlag;

    @ApiModelProperty(value = "创建日期")
    private Date createDate;

    @ApiModelProperty(value = "更新日期")
    private Date lastUpdateDate;

    @ApiModelProperty(value = "文件")
    private MultipartFile file;

    @ApiModelProperty(value = "文件")
    private File orgFile;
}
