package com.foxconn.plm.spas.scheduled;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.spas.bean.SpasWorkItem;
import com.foxconn.plm.spas.config.properties.SpasPropertiesConfig;
import com.foxconn.plm.spas.config.properties.SpasWorkItemPropertiesConfig;
import com.foxconn.plm.spas.mapper.SynSpasWorkItemMapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;

/**
 * @Author {jian-jun.fan@foxconn.com}
 * @Date: 2023/7/31/ 9:45
 * @description 同步SPAS系统中的工时
 */
@Component
public class SynSpasWorkItemTask {

    private static final int pageSize = 5000;
    private static final Log log = LogFactory.get();

    @Resource
    private SpasWorkItemPropertiesConfig spasWorkItemPropertiesConfig;

    @Resource
    SynSpasWorkItemMapper workItemMapper;

    // @PostConstruct
    @XxlJob("synSpasWorkItem")
    public void sync() {
        log.info("========= SynSpasWorkItemTask 开始 =========");
        String token = loginToSpas();
        if(token==null){
            return;
        }
        String url = spasWorkItemPropertiesConfig.getHost()+"/project-server/api/project/tc/getWorkHours";
        log.info("同步地址是：{}",url);
        String start = DateUtil.offsetDay(new Date(), -5).toDateStr();
        String end = DateUtil.yesterday().toDateStr();
//        start = "2023-07-17";
//        end = "2023-07-21";
        log.info("时间段：{} 至 {}",start,end);
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("startDate",start);
        paramMap.put("endDate",end);
        JSONArray data = new JSONArray();
        for (int i = 1; i <= 1000; i++) {
            paramMap.put("currentPage",i);
            paramMap.put("pageSize",pageSize);
            String post = HttpUtil.createPost(url).header("token",token).body(JSONUtil.toJsonStr(paramMap), "application/json").execute().body();
            JSONObject jsonObject = JSONUtil.parseObj(post);
            JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("items");
            if (jsonArray.size() < pageSize) {
                break;
            }
            data.addAll(jsonArray);
        }
        // 删除时间段内的数据
        log.info("已删除{}笔数据",workItemMapper.delete(start,end));
        List<String> tcProjectList = workItemMapper.tcProjectList();
        // 插入数据库
        for (int i = 0; i < data.size(); i++) {
            JSONObject jsonObject = data.getJSONObject(i);
            String workItem = jsonObject.getStr("workItem");
            String curPhase = jsonObject.getStr("curPhase");
            String projectId = jsonObject.getStr("projectId");
            float workedHours = jsonObject.getFloat("workedHours",0F);
            if(workItem==null){
                workItem = "";
            }
            if(curPhase==null){
                curPhase = "";
            }
            if(projectId==null){
                projectId = "";
            }
            //meeting/Meeting/study/Study/会议/学习/會議/學習
            // SELECT * FROM SPAS_WORK_ITEM swi WHERE SWI .WORK_ITEM IN ('meeting','Meeting','study','Study','会议','学习','會議','學習')
            if (workItem.contains("meeting") || workItem.contains("Meeting") || workItem.contains("study") || workItem.contains("Study") || workItem.contains("会议") || workItem.contains("学习") || workItem.contains("會議") || workItem.contains("學習")) {
                log.info("【学习会议】过滤："+jsonObject);
                continue;
            }
            if (curPhase.contains("P8") || curPhase.equals("p8")) {
                log.info("【P8阶段】过滤："+jsonObject);
                continue;
            }
            if (!tcProjectList.contains(projectId)) {
                log.info("【TC中不存在专案】过滤："+jsonObject);
                continue;
            }
            if (workedHours == 0) {
                log.info("【工時為0】过滤："+jsonObject);
                continue;
            }
            log.info("【有效记录】："+jsonObject);
            SpasWorkItem spasWorkItem = jsonObject.toBean(SpasWorkItem.class);
            try {
                workItemMapper.saveWorkItem(spasWorkItem);
            } catch (Exception e) {
                log.info("失败：{}",e.getMessage());
            }
        }
        log.info("========= SynSpasWorkItemTask 结束 =========");
    }

    private String loginToSpas(){
        try {
            String loginUrl = spasWorkItemPropertiesConfig.getHost() + "/user-server/api/user/sysSignIn";
            log.info("登录到Spas：{}", loginUrl);
            Map<String,Object> loginParam = new HashMap<>();
            loginParam.put("apiKey",spasWorkItemPropertiesConfig.getApiKey());
            loginParam.put("password",spasWorkItemPropertiesConfig.getPassword());
            loginParam.put("sysFlag",spasWorkItemPropertiesConfig.getSysFlag());
            loginParam.put("userName",spasWorkItemPropertiesConfig.getUsername());
            String body = HttpUtil.createPost(loginUrl).body(JSONUtil.toJsonStr(loginParam), "application/json").execute().body();
            String data = JSONUtil.parseObj(body).getStr("data");
            if(data ==null){
                log.info("登录失败："+body);
            }
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }



}
