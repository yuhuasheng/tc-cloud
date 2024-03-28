package com.foxconn.dp.plm.hdfs.domain.rp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel
public class ItemRevisionListRp {

    @ApiModelProperty("文档Id")
    @NotBlank(message = "文档Id不能為空")
    long docId;
}
