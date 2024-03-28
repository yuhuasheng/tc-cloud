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
@TableName("obj_document")
public class DocumentEntity extends Model<DocumentEntity> {
    @TableId
    private Integer docSn;
    private String docNum;
    private String description;
    private Long docOrigin;
    private String productCode;
    private String productLine;
    private String customer;
    private String documentCategory;
    private String checkoutUser;
    private LocalDateTime checkoutDt;
    private LocalDateTime checkinDt;
    private String creator;
    private String docName;
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
