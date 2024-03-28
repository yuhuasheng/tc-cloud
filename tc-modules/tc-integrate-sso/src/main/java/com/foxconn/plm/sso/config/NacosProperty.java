package com.foxconn.plm.sso.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * @author DY
 * @CLassName: NacosProperty
 * @Description:
 * @create 2022/10/17
 */
@EqualsAndHashCode
@Configuration
@RefreshScope
public class NacosProperty {
    @Value("${sso.clientId}")
    private String clientId;
    @Value("${sso.clientSecret}")
    private String clientSecret;
    @Value("${sso.redirectUri}")
    private String redirectUri;
    @Value("${sso.scope}")
    private String scope;
    @Value("${sso.grantType}")
    private String grantType;
    @Value("${sso.baseUrl}")
    private String baseUrl;
    @Value("${sso.tokenMethod}")
    private String tokenMethod;
    @Value("${sso.userMethod}")
    private String userMethod;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getTokenMethod() {
        return tokenMethod;
    }

    public void setTokenMethod(String tokenMethod) {
        this.tokenMethod = tokenMethod;
    }

    public String getUserMethod() {
        return userMethod;
    }

    public void setUserMethod(String userMethod) {
        this.userMethod = userMethod;
    }
}
