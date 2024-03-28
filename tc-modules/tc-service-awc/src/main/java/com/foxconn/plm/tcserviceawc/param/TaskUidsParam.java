package com.foxconn.plm.tcserviceawc.param;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 過濾任務的uid
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/25 15:49
 **/
@Data
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskUidsParam {
    private List<String> uids;
    private String empNo;
}
