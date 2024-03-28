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
 * @Date 2024/1/2 13:59
 **/
@EqualsAndHashCode
public class SearchRecordParam implements Serializable {
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
    private Integer pageNum = 1;
    private Integer pageSize  = 10;

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

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
