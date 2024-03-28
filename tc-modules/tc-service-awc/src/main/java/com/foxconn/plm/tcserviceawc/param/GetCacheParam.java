package com.foxconn.plm.tcserviceawc.param;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2024/2/2 10:43
 **/
@Data
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetCacheParam implements Serializable {
    @NotBlank(message = "actualUser不能為空")
    private String actualUser;
    @NotBlank(message = "higherObject不能為空")
    private String higherObject;
}
