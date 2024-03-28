package com.foxconn.dp.plm.hdfs.domain.rp;

import com.foxconn.plm.entity.param.PageParam;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel
public class ItemListRp extends PageParam {

    @ApiModelProperty("父文件夹Id")
    List<Long> folderIds;

    @ApiModelProperty("文档编号")
    String docNum;

    @ApiModelProperty("文档名称")
    String docName;


}
