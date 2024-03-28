package com.foxconn.plm.tcservice.issuemanagement.param;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 新增賬號參數類
 *
 * @Description
 * @Author MW00442
 * @Date 2023/12/22 15:30
 **/
@Data
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddAccountParam implements Serializable {
    @NotBlank(message = "工號不能為空")
    @ApiParam(value = "工號")
    private String no;
    @NotBlank(message = "名稱不能為空")
    @ApiParam(value = "名稱")
    private String name;
    @NotBlank(message = "Bu不能為空")
    @ApiParam(value = "bu")
    private String bu;
    @NotBlank(message = "Platform不能為空")
    @ApiParam(value = "platform")
    private String platform;
    @NotBlank(message = "部門不能為空")
    @ApiParam(value = "部門")
    private String dept;
    @ApiParam(value = "一級賬號uid")
    private String tcUid;
}
