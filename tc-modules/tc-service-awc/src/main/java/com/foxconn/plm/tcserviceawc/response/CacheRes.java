package com.foxconn.plm.tcserviceawc.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2024/2/2 10:40
 **/
@Data
@EqualsAndHashCode
public class CacheRes implements Serializable {
    private String actualUser;
    private String higherObject;
    private String type;
    private String modifyObject;
    private String mainObject;
    private Map<String,String> props;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cacheTime;
}
