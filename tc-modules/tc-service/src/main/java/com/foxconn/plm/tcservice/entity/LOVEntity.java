package com.foxconn.plm.tcservice.entity;

import com.foxconn.plm.entity.Entity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class LOVEntity extends Entity {

    int id;
    int parentId;

    @ApiModelProperty(value = "名称")
    String name;

    @ApiModelProperty(value = "代号")
    String code;


    @ApiModelProperty(value = "值")
    String value;

    List<LOVEntity> subList;
}
