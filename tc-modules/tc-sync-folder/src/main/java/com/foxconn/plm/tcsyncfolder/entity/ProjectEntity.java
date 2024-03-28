package com.foxconn.plm.tcsyncfolder.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @ClassName: ProjectEntity
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("obj_project")
public class ProjectEntity extends Model<ProjectEntity> {
    @TableId
    private Integer projSn;
    private String projName;
    private String projSpasId;
    private Integer activeFlag;
    private Integer folderId;
    private String refId;
    @TableField(value = "created",fill = FieldFill.INSERT)
    private LocalDateTime created;
    @TableField(value = "last_upd",fill = FieldFill.UPDATE)
    private LocalDateTime lastUpd;
    @TableField(value = "del_flag",fill = FieldFill.INSERT)
    @TableLogic
    private Integer delFlag;
}
