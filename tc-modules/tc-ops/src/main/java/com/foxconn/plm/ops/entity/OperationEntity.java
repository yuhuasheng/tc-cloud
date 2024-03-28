package com.foxconn.plm.ops.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 運維操作記錄表
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/2 11:06
 **/
@EqualsAndHashCode
@TableName("ops_operation_record")
public class OperationEntity {
    @TableId
    private Long id;
    private Long batInfoId;
    /**
     * 是否執行成功
     */
    private String status;
    /**
     * 執行結果
     */
    @TableField(value = "RESULT")
    private String result;
    /**
     * 備註
     */
    private String remark;
    @TableField(value = "created",fill = FieldFill.INSERT)
    private LocalDateTime created;
    @TableField(value = "last_upd",fill = FieldFill.UPDATE)
    private LocalDateTime lastUpd;
    @TableField(value = "del_flag",fill = FieldFill.INSERT)
    @TableLogic
    private Integer delFlag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBatInfoId() {
        return batInfoId;
    }

    public void setBatInfoId(Long batInfoId) {
        this.batInfoId = batInfoId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getLastUpd() {
        return lastUpd;
    }

    public void setLastUpd(LocalDateTime lastUpd) {
        this.lastUpd = lastUpd;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }
}
