package com.foxconn.plm.integrate.sap.customPN.domain.rp;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel
public class CustomPartRp {
    @ApiModelProperty(value = "ecUid")
    private String ecUid;

    @ApiModelProperty(value = "uid")
    private String uid;

    @ApiModelProperty(value = "irUid")
    private String irUid;


    @ApiModelProperty(value = "临时料号")
    private String oldMaterialNumber;

    @ApiModelProperty(value = "料号")
    private String materialNumber;

    @ApiModelProperty(value = "rule ")
    private String ruleRegx;

    @ApiModelProperty(value = "plant")
    private String plant;

    @ApiModelProperty(value = "物料类型")
    private String materialType;

    @ApiModelProperty(value = "描述")
    private String description;

    @ApiModelProperty(value = "中文描述")
    private String descriptionZH;


    @ApiModelProperty(value = "material Group")
    private String materialGroup;

    @ApiModelProperty(value = "用量单位")
    private String baseUnit;

    @ApiModelProperty(value = "净重")
    private String netWeight;

    @ApiModelProperty(value = "毛重")
    private String grossWeight;

    @ApiModelProperty(value = "重量单位")
    private String weightUnit;

    @ApiModelProperty(value = "mrp Group")
    private String mrpGroup;

    @ApiModelProperty(value = "mrp Type")
    private String mrpType;

    @ApiModelProperty(value = "part Source")
    private String partSource;

    @ApiModelProperty(value = "model name")
    private String modelName;

    @ApiModelProperty(value = "制造商编码")
    private String makerCode;

    @ApiModelProperty(value = "制造商料号")
    private String makerPN;
}
