package com.foxconn.plm.tcreport.drawcountreport.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * TODO
 *
 * @Description
 * @Author MW00442
 * @Date 2023/9/12 17:11
 **/
@Data
@EqualsAndHashCode
@TableName("DRAW_COUNT_REL")
public class DrawCountRelEntity extends Model<DrawCountRelEntity> {
    @TableId
    private Long id;
    private String timeType;
    private Integer total;
    private String projectId;
    private String objType;

    public DrawCountRelEntity() {
    }

    public DrawCountRelEntity(Long id, String timeType, String type,String projectId) {
        this.id = id;
        this.timeType = timeType;
        this.projectId = projectId;
        this.objType = type;
        this.total = 0;
    }
}
