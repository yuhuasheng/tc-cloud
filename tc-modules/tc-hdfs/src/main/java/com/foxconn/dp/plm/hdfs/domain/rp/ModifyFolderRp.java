package com.foxconn.dp.plm.hdfs.domain.rp;

import com.foxconn.plm.entity.RequestParam;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel
public class ModifyFolderRp extends RequestParam {

    @ApiModelProperty("文件夹ID")
    @NotBlank(message = "文件夹ID不能为空")
    long folderId;

    @ApiModelProperty("新文件夾名稱")
    @NotBlank(message = "新文件夾名稱不能為空")
    String folderName;

    String fldDesc;
}
