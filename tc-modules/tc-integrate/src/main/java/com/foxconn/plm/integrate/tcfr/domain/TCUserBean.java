package com.foxconn.plm.integrate.tcfr.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author HuashengYu
 * @Date 2023/3/9 14:04
 * @Version 1.0
 */
@Data
@ApiModel
public class TCUserBean {

    @ApiModelProperty(value = "工号")
    private String workId;

    @ApiModelProperty(value = "名称")
    private String workName;

    @ApiModelProperty(value = "TC用户ID")
    private String tcUserId;

    @ApiModelProperty(value = "邮箱")
    private String email;
}
