package com.foxconn.plm.entity;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class RequestParam {

    @ApiModelProperty("登陆人工号")
    String userId;

}
