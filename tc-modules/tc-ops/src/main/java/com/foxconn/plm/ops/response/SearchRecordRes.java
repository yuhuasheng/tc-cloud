package com.foxconn.plm.ops.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/2 14:00
 **/
public class SearchRecordRes implements Serializable {
    @ApiModelProperty("數據唯一id")
    private String id;
    @ApiModelProperty("主機ip")
    private String host;
    @ApiModelProperty("登錄用戶名")
    private String userName;
    @ApiModelProperty("登錄密碼")
    private String secretKey;
    @ApiModelProperty("腳本所在的絕對路徑")
    private String scriptPath;
    @ApiModelProperty("腳本名稱")
    private String script;
    @ApiModelProperty("狀態")
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("創建時間")
    private LocalDateTime created;

    public String getId() {
        return id;
    }

    public void setId(String id) {
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
}
