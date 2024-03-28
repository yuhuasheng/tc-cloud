package com.foxconn.plm.tcserviceawc.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 緩存屬性類
 *
 * @Description
 * @Author MW00442
 * @Date 2024/2/2 9:21
 **/
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("bo_cache_prop")
public class BoCachePropEntity extends Model<BoCachePropEntity> {
    @TableId
    private Long id;
    private Long cacheId;
    private String propKey;
    private String propValue;
}
