package com.foxconn.plm.ops.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 運維操作記錄類
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/2 10:58
 **/
@EqualsAndHashCode
@TableName("OPS_BAT_INFO")
public class OpsBatInfoEntity extends Model<OpsBatInfoEntity> {
    @TableId
    private Long id;
    /**
     * 主機ip
     */
    private String host;
    /**
     * 主機用戶名
     */
    private String userName;
    /**
     * 註解密碼
     */
    private String secretKey;
    /**
     * 腳本所在路徑（絕對路徑）
     */
    private String scriptPath;
    /**
     * 腳本名稱
     */
    private String script;
    /**
     * 是否停用
     */
    private String status;
    /**
     * 數據創建時間
     */
    @TableField(value = "created",fill = FieldFill.INSERT)
    private LocalDateTime created;
    /**
     * 數據更新時間
     */
    @TableField(value = "last_upd",fill = FieldFill.UPDATE)
    private LocalDateTime lastUpd;
    /**
     * 刪除狀態
     */
    @TableField(value = "del_flag",fill = FieldFill.INSERT)
    @TableLogic
    private Integer delFlag;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getScriptPath() {
        return scriptPath;
    }

    public void setScriptPath(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
