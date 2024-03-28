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
@TableName("obj_folder")
public class FolderEntity extends Model<FolderEntity> {
    @TableId
    private Integer fldSn;
    private String fldName;
    private String creator;
    private String modified;
    private String fldDesc;
    private Integer refType;
    private String refId;
    @TableField(value = "created",fill = FieldFill.INSERT)
    private LocalDateTime created;
    @TableField(value = "last_upd",fill = FieldFill.UPDATE)
    private LocalDateTime lastUpd;
    @TableField(value = "del_flag",fill = FieldFill.INSERT)
    @TableLogic
    private Integer delFlag;
}
