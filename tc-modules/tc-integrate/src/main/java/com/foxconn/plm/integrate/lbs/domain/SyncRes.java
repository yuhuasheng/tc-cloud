package com.foxconn.plm.integrate.lbs.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @ClassName: SyncRes
 * @Description:
 * @Author DY
 * @Create 2022/12/20
 */
@Data
@EqualsAndHashCode
public class SyncRes implements Serializable {
    private String id;
    private String rev;
    private String spasId;
    private String spasPhase;
    private String changList;
    private String projName;
    private String fileName;
}
