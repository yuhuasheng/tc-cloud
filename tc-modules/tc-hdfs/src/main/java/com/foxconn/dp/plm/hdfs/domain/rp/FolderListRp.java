package com.foxconn.dp.plm.hdfs.domain.rp;

import com.foxconn.plm.entity.param.PageParam;
import com.sun.istack.NotNull;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel
public class FolderListRp extends PageParam {

    @ApiModelProperty("专案Id")
    @NotBlank(message = "专案不能为空")
    String projectId;

}
