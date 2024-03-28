package com.foxconn.plm.tcsyncfolder.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @ClassName: FolderRefEntity
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("folder_structure")
public class FolderRefEntity extends Model<FolderRefEntity> {
    @TableId
    private Integer fldStruSn;
    private Integer fldId;
    private Integer fldChildId;
    private String creator;
    private String modified;
    @TableField(value = "created",fill = FieldFill.INSERT)
    private LocalDateTime created;
    @TableField(value = "last_upd",fill = FieldFill.UPDATE)
    private LocalDateTime lastUpd;
}
