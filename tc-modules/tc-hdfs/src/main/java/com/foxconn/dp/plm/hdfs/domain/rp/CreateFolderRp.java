package com.foxconn.dp.plm.hdfs.domain.rp;

import com.foxconn.plm.entity.RequestParam;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Data
@ApiModel
public class CreateFolderRp extends RequestParam {

    @ApiModelProperty(hidden = true)
    long fid;

    @ApiModelProperty(hidden = true)
    long fsId;

    @ApiModelProperty("父文件夹ID")
    @Min(0)
    long parentFolderId;

    @ApiModelProperty("文件夾名稱")
    @NotBlank(message = "文件夾名稱不能為空")
    String folderName;

    String fldDesc;
}
