package com.foxconn.dp.plm.fileservice.domain.rp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;


@Data
@ApiModel
public class ReviseFileRp {

    @ApiModelProperty(value = "文件")
    private MultipartFile file;

    @ApiModelProperty(value = "修改人")
    private String modified;

    @ApiModelProperty(value = "文档id")
    private Long docId;


    @ApiModelProperty(value = "文件夹Id")
    private Long folderId;


    @ApiModelProperty(value = "文档名称")
    private String docName;


}
