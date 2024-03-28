package com.foxconn.plm.tcservice.connandcable.domain;

import lombok.Data;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2023/01/31/ 16:46
 * @description
 */

@Data
public class CoCaInfo {
    private Integer id;
    private String hhPN;
    private String designPN = "";
    private String description;
    private String supplier;
    private Integer groupId;
}
