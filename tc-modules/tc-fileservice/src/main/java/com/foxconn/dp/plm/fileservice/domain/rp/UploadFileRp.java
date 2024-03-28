package com.foxconn.dp.plm.fileservice.domain.rp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;


@Data
@ApiModel
public class UploadFileRp {

    @ApiModelProperty(value = "文件")
    private MultipartFile[] file;

    @ApiModelProperty(value = "修改人")
    private String modified;

    @ApiModelProperty(value = "文件夹Id")
    private Long folderId;

    @ApiModelProperty(value = "文档来源 0：文件系統；1：TC")
    private Integer docOrigin;

    @ApiModelProperty(value = "文档类型")
    private String docCategory;

    @ApiModelProperty(value = "產品代碼")
    private String productCode;

    @ApiModelProperty(value = "產品線")
    private String productLine;

    @ApiModelProperty(value = "客戶")
    private String customer;

    @ApiModelProperty(value = "文档名称")
    private String docName;

    @ApiModelProperty(value = "文档描述")
    private String docDescription;
}
