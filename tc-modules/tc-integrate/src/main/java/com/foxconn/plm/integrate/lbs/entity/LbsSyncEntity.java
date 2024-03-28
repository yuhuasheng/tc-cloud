package com.foxconn.plm.integrate.lbs.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @ClassName: LbsSyncEntity
 * @Description:
 * @Author DY
 * @Create 2022/12/15
 */
@Data
@EqualsAndHashCode
public class LbsSyncEntity {
    private Long id;
    private String rev;
    private String spasId;
    private String spasPhase;
    private String changList;
    private String projName;
    private String fileName;
    private String delFlag;
    private Date createTime;
}
