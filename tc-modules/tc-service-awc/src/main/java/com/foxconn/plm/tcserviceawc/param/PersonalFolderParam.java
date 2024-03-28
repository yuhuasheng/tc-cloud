package com.foxconn.plm.tcserviceawc.param;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 獲取個人工作區的參數類
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/23 17:14
 **/
@Data
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonalFolderParam {
    private String empNo;
    private String dept;
    private String userUid;
    private String groupUid;
}
