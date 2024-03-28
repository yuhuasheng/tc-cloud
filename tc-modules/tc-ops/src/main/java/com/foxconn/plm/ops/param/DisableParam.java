package com.foxconn.plm.ops.param;

import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/2 14:15
 **/
@EqualsAndHashCode
public class DisableParam implements Serializable {
    @NotBlank(message = "數據唯一id不能為空")
    @ApiModelProperty("數據唯一id")
    private String id;
    @NotNull(message = "凍結狀態不能為空")
    @ApiModelProperty("是否凍結")
    private Boolean disable;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getDisable() {
        return disable;
    }

    public void setDisable(Boolean disable) {
        this.disable = disable;
    }
}
