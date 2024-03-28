package com.foxconn.dp.plm.hdfs.domain.rp;

import com.foxconn.plm.entity.param.PageParam;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel
public class ItemRevisionPageRp extends PageParam {

    @ApiModelProperty("文档Id")
    @NotBlank(message = "文档Id不能為空")
    long docId;
}
