package com.foxconn.plm.tcreport.drawcountreport.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;

/**
 * @ClassName: DrawCountEntity
 * @Description:
 * @Author DY
 * @Create 2023/1/16
 */
@Data
@EqualsAndHashCode
@TableName("draw_count_report")
public class DrawCountEntity extends Model<DrawCountEntity> {
    @TableId
    private Long id;
    @TableField
    private String bu;
    @TableField
    private String customer;
    @TableField
    private String productLine;
    @TableField
    private String projectSeries;
    @TableField
    private String projectName;
    @TableField
    private String designTreeType;
    @TableField
    private String designTreeName;
    @TableField
    private String owner;
    @TableField
    private String ownerGroup;
    @TableField
    private String actualUser;
    @TableField
    private String itemCode;
    @TableField
    private String itemName;
    @TableField
    private String itemType;
    @TableField
    private Integer uploadNum;
    @TableField
    private Integer releaseNum;
    @TableField
    private String releaseProgress;
    @TableField
    private Integer releaseModelNum;
    @TableField
    private String itemCompleteness;
    @TableField
    private String drawCompleteness;
    @TableField
    private String projectId;
    @TableField
    private String reportDate;
    @TableField
    private String phase;
    @TableField
    private String chassis;
}
