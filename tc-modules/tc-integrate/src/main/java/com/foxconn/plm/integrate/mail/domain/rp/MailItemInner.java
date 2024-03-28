package com.foxconn.plm.integrate.mail.domain.rp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class MailItemInner {


    @ApiModelProperty(value = "id")
    private Long id;


    @ApiModelProperty(value = "uuid")
    private String uuid;


    @ApiModelProperty(value = "對象ID")
    private String itemId;


    @ApiModelProperty(value = "對象ID")
    private String itemName;


    @ApiModelProperty(value = "對象類型")
    private String objType;


    @ApiModelProperty(value = "itemCategory")
    private String itemCategory;


    @ApiModelProperty(value = "邮件群组ID")
    private Long groupId;

    @ApiModelProperty(value = "创建人")
    private String creator;


    @ApiModelProperty(value = "创建人姓名", hidden = true)
    private String creatorName;

}
