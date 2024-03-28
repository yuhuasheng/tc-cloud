package com.foxconn.plm.tcsyncfolder.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @ClassName: DocumentRevEntity
 * @Description:
 * @Author DY
 * @Create 2023/3/27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("obj_document_rev")
public class DocumentRevEntity extends Model<DocumentRevEntity> {
    @TableId
    private Integer revSn;
    private Integer docId;
    private Integer fldId;
    private String revName;
    private String revNum;
    private Long workflowId;
    private Integer refType;
    private Integer lifecyclePhase;
    private String creator;
    private String creatorName;
    private String refId;
    @TableField(value = "created",fill = FieldFill.INSERT)
    private LocalDateTime created;
    @TableField(value = "last_upd",fill = FieldFill.UPDATE)
    private LocalDateTime lastUpd;
    @TableField(value = "del_flag",fill = FieldFill.INSERT)
    @TableLogic
    private Integer delFlag;
}
