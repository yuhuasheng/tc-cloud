package com.foxconn.plm.integrate.dgkpi.domain.resp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


@Data
@ApiModel
public class UrlResp {


    @ApiModelProperty(value = "SPAS专案id")
    private String spasProjId;

    @ApiModelProperty(value = "對象ID")
    private String itemId;

    @ApiModelProperty(value = "对象名称")
    private String objectName;

    @ApiModelProperty(value = "工号(姓名)")
    private String owningUser;


    @ApiModelProperty(value = "文档下载url")
    private String url;


    @ApiModelProperty(value = "对象版本")
    private String rev;

}
