package com.foxconn.plm.integrate.mail.domain.rp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel
@Data
public class MailGroupRp {

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "群组名")
    private String groupName;

    @ApiModelProperty(value = "描述")
    private String description;

    @ApiModelProperty(value = "创建人")
    private String creator;

    @ApiModelProperty(value = "创建人姓名", hidden = true)
    private String creatorName;

    @ApiModelProperty(value = "修改人")
    private String updateBy;

    @ApiModelProperty(value = "修改人姓名", hidden = true)
    private String updateByName;


    @ApiModelProperty(value = "bu")
    private String bu;


}
