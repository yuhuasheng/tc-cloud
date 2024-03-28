package com.foxconn.plm.ops.param;

import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/2 11:40
 **/
@EqualsAndHashCode
public class EditRecordParam extends AddRecordParam{
    @NotBlank(message = "數據唯一id不能為空")
    @ApiModelProperty("數據唯一id")
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
