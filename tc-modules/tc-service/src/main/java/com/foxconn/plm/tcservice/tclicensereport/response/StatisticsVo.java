package com.foxconn.plm.tcservice.tclicensereport.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @ClassName: StatisticsVo
 * @Description:
 * @Author DY
 * @Create 2022/12/3
 */
@Data
@EqualsAndHashCode
public class StatisticsVo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String label;
    private Integer used;
    private Integer unUsed;

}
