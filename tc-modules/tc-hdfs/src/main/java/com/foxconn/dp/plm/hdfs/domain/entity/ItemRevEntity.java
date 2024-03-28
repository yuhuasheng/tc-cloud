package com.foxconn.dp.plm.hdfs.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.foxconn.plm.entity.Entity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ItemRevEntity extends Entity implements Serializable {


    String id;

    @ApiModelProperty(value = "文档ID")
    String docId;

    @ApiModelProperty(value = "父文件夹ID")
    String folderId;

    @ApiModelProperty(value = "文档编号ID")
    String num;

    @ApiModelProperty(value = "文档名称")
    String docName;

    @ApiModelProperty(value = "文档出库人ID")
    String checkOutUserId;

    @ApiModelProperty(value = "来源")
    int origin;

    @ApiModelProperty(value = "文档出库人名称")
    String checkOutUserName;

    @ApiModelProperty(value = "文档出库时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    Date checkOutDate;

    @ApiModelProperty(value = "文档出库时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    Date checkInDate;

    @ApiModelProperty(value = "文件名称")
    String fileName;

    @ApiModelProperty(value = "版本Id")
    String verId;

    @ApiModelProperty(value = "版本号")
    String verNum;

    @ApiModelProperty(value = "路径")
    String fullPath;

    @ApiModelProperty(value = "状态")
    int status;

    FolderEntity parentFolder;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    Date created;

    @ApiModelProperty(value = "最后更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    Date lastUpdateDate;

}
