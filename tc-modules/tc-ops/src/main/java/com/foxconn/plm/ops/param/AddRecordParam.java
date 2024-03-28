package com.foxconn.plm.ops.param;

import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 *
 *
 * @Description
 * @Author MW00442
 * @Date 2024/1/2 11:31
 **/
@EqualsAndHashCode
public class AddRecordParam implements Serializable {
    @NotBlank(message = "主機ip不能為空")
    @ApiModelProperty("主機ip")
    private String host;
    @NotBlank(message = "用戶名不能為空")
    @ApiModelProperty("登錄用戶名")
    private String userName;
    @NotBlank(message = "密碼不能為空")
    @ApiModelProperty("登錄密碼")
    private String secretKey;
    @NotBlank(message = "腳本所在的絕對路徑不能為空")
    @ApiModelProperty("腳本所在的絕對路徑")
    private String scriptPath;
    @NotBlank(message = "腳本名稱不能為空")
    @ApiModelProperty("腳本名稱")
    private String script;

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
}
