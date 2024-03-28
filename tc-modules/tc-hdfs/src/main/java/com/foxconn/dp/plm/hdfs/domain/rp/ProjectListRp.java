package com.foxconn.dp.plm.hdfs.domain.rp;

import com.foxconn.plm.entity.RequestParam;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel
public class ProjectListRp extends RequestParam {

    @ApiModelProperty("工号Id")
    @NotBlank(message = "工号Id不能为空")
    String empId;


    @ApiModelProperty("PDM文件库")
    String pdm;

}
