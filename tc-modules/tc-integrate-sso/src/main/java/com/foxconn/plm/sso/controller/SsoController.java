package com.foxconn.plm.sso.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.redis.service.RedisService;
import com.foxconn.plm.sso.config.NacosProperty;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 相信SSo掃碼前端控制類
 *
 * @Description
 * @Author MW00442
 * @Date 2023/12/27 9:50
 **/
@CrossOrigin
@RestController
@RequestMapping
public class SsoController {
    @Resource
    private RedisService redisService;
    @Resource
    private NacosProperty property;

    @RequestMapping("/sso/getSsoState")
    public String getSsoState(String tcKey, HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        String userInfo = redisService.getCacheObject("tc:" + tcKey);
        LogFactory.get().info(tcKey + "<---------->" + userInfo);
        if (StringUtils.hasLength(userInfo)) {
            return userInfo;
        }
        return "";
    }

    @RequestMapping("/tcSSO")
    public void tcSso(HttpServletRequest request, HttpServletResponse response) {
        String code = request.getParameter("code");
        String tckey = request.getParameter("state");
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("client_id", property.getClientId());
        queryParams.put("client_secret", property.getClientSecret());
        queryParams.put("redirect_uri", property.getRedirectUri());
        queryParams.put("scope", property.getScope());
        queryParams.put("grant_type", property.getGrantType());
        queryParams.put("code", code);
        String result = HttpRequest.post(property.getBaseUrl() + property.getTokenMethod()).form(queryParams).addHeaders(headers).execute().body();
        LogFactory.get().info("oss code ::" + result);
        JSONObject resultJosn = JSONUtil.parseObj(result);
        String token = (String) resultJosn.get("access_token");
        LogFactory.get().info("idToken ::" + token);
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + token);
        String userInfo = HttpRequest.get(property.getBaseUrl() + property.getUserMethod()).addHeaders(headers).execute().body();
        LogFactory.get().info("userInfo :: " + userInfo);
        JSONObject userJson = JSONUtil.parseObj(userInfo);
        LogFactory.get().info("userJson ::" + userJson);
        redisService.setCacheObject("tc:" + tckey, userInfo, 10L, TimeUnit.MINUTES);
        response.setHeader("Access-Control-Allow-Origin", "*");
    }
}
