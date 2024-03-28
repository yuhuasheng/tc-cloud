package com.foxconn.dp.plm.hdfs.domain.rp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel
public class SubFolderListRp {

    @ApiModelProperty("父文件夹Id")
    @NotBlank(message = "父文件夾ID不能為空")
    long folderId;

    @ApiModelProperty("专案ID")
    @NotBlank(message = "专案ID不能為空")
    String projectId;

    @ApiModelProperty("部门短名称")
    @NotBlank(message = "部门短名称不能為空")
    String dept;

    String empId;

    int showAll;

    // 是否是虚拟专案
    public boolean isVirtual() {
        return projectId != null && projectId.startsWith("v");
    }

}
