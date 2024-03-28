package com.foxconn.plm.tcserviceawc.param;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.Map;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2024/2/2 10:53
 **/
@Data
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddCacheParam implements Serializable {
    @NotBlank(message = "actualUser不能為空")
    private String actualUser;
    @NotBlank(message = "higherObject不能為空")
    private String higherObject;
    @NotBlank(message = "type不能為空")
    private String type;
    @NotBlank(message = "modifyObject不能為空")
    private String modifyObject;
    @NotBlank(message = "mainObject不能為空")
    private String mainObject;
    @NotEmpty(message = "props不能為空")
    private Map<String,String> props;
}
