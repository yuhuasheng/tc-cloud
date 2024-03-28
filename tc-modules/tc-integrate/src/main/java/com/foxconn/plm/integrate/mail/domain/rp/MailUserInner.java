package com.foxconn.plm.integrate.mail.domain.rp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class MailUserInner {


    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "工号")
    private String empId;


    @ApiModelProperty(value = "用户名")
    private String userName;


    @ApiModelProperty(value = "邮箱")
    private String mail;


    @ApiModelProperty(value = "邮件群组ID")
    private Long groupId;

    @ApiModelProperty(value = "创建人")
    private String creator;


    @ApiModelProperty(value = "创建人姓名", hidden = true)
    private String creatorName;


}
