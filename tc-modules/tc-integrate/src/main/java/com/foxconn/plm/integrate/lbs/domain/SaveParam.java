package com.foxconn.plm.integrate.lbs.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @ClassName: SaveParam
 * @Description:
 * @Author DY
 * @Create 2022/12/15
 */
@Data
@EqualsAndHashCode
public class SaveParam implements Serializable {
    private String rev;
    private String spasId;
    private String spasPhase;
    private String changList;
    private String projName;
    private String fileName;
}
