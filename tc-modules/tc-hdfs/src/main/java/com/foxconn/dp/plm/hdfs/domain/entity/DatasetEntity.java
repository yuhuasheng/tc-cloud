package com.foxconn.dp.plm.hdfs.domain.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Date;

@Data
@ApiModel
public class DatasetEntity {

    @ApiModelProperty(value = "puid")
    private String puid;


    @ApiModelProperty(value = "文件名")
    private String fileName;

    @ApiModelProperty(value = "文件来源 0-文件系统  1-TC")
    private String origin;

}
