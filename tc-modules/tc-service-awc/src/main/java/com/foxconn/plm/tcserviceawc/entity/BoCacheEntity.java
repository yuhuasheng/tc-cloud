package com.foxconn.plm.tcserviceawc.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 緩存數據庫對象
 *
 * @Description
 * @Author MW00442
 * @Date 2024/2/2 9:15
 **/
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("bo_cache")
public class BoCacheEntity extends Model<BoCacheEntity> {
    @TableId
    private Long id;
    private String actualUser;
    private String higherObject;
    private String type;
    private String modifyObject;
    private String mainObject;
    private String active;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private String delFlag;
}
