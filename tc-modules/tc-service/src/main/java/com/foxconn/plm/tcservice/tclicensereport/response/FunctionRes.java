package com.foxconn.plm.tcservice.tclicensereport.response;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @ClassName: FunctionRes
 * @Description:
 * @Author DY
 * @Create 2022/12/3
 */
@Data
@EqualsAndHashCode
public class FunctionRes implements Serializable {
    private static final long serialVersionUID = 1L;
    @ApiModelProperty("department所屬的BU")
    private String bu;
    @ApiModelProperty("function所屬的department")
    private String department;
    @ApiModelProperty("Function")
    private String function;
    @ApiModelProperty("已使用數量")
    private Integer used;
    @ApiModelProperty("總數")
    private Integer total;
    @ApiModelProperty("使用率")
    private String utilizationRate;
    @ApiModelProperty("稼動率")
    private String cropRate;
}
