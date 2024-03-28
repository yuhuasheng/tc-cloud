package com.foxconn.plm.entity.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


@Data
@ApiModel
public class MakerPNRp {

    @ApiModelProperty(value = "物料puid")
    private String puid;

    @ApiModelProperty(value = "物料料号")
    private String materialNum;

    @ApiModelProperty(value = "plant")
    private String plant;

}
