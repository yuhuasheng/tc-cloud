package com.foxconn.plm.integrate.mail.domain.rp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel
public class MailUserRp {


    @ApiModelProperty(value = "邮件群组ID")
    private Long groupId;


    @ApiModelProperty(value = "数据集")
    private List<MailUserInner> list;

}
