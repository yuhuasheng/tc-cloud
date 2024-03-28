package com.foxconn.plm.tcservice.tclicensereport.response;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @ClassName: StatisticsRes
 * @Description:
 * @Author DY
 * @Create 2022/12/3
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsRes implements Serializable {
    private static final long serialVersionUID = 1L;
    @ApiModelProperty("統計的標籤")
    private String label;
    @ApiModelProperty("統計的值")
    private Object value;
}
