package com.foxconn.plm.integrate.mail.domain;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class MailUser {

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "工号")
    private String empId;


    @ApiModelProperty(value = "用户名")
    private String userName;


    @ApiModelProperty(value = "邮件群组ID")
    private Long groupId;

    @ApiModelProperty(value = "邮箱")
    private String mail;

    @ApiModelProperty(value = "部门")
    private String dept;

    @ApiModelProperty(value = "bu")
    private String bu;


}
