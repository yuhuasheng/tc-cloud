package com.foxconn.dp.plm.health;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.shaded.com.google.gson.Gson;
import com.foxconn.plm.entity.response.HealthStatusRv;
import com.foxconn.plm.feign.service.TcMailClient;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.CharsetUtils;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


@Service
public class CheckHealthService {

    @Resource
    private DiscoveryClient discoveryClient;

    @Value("${tc.services}")
    private String services;

    @Value("${spring.cloud.nacos.discovery.namespace}")
    private String env;

    @Value("${spring.cloud.nacos.discovery.server-addr}")
    private String nacosUrl;

    @Value("${tc.email.staff}")
    private String emailList;

    @Resource
    private TcMailClient mailClient;

    @Resource
    HealthMapper mapper;

    public List<HealthStatusRv> queryServiceStatus(){
        return mapper.getList();
    }


    // 每10秒执行一次
    @Scheduled(cron = "0/10 * * * * ? ")
    public void check(){
        synchronized (CheckHealthService.class){
            String[] split = services.split(",");
            for (String service : split) {
                int count = mapper.count(service);
                if(count == 0){
                    mapper.insert(service);
                }
                List<ServiceInstance> serviceInstanceList = discoveryClient.getInstances(service);
                if(serviceInstanceList.isEmpty()){
                    fault(service);
                    continue;
                }
                boolean isRunning = false;
                for (ServiceInstance serviceInstance : serviceInstanceList) {
                    String host = serviceInstance.getHost();
                    int port = serviceInstance.getPort();
                    String url = "http://"+host+":"+port+"/status";
                    String s = HttpUrlUtil.get(url);
                    try {
                        JSONObject jsonObject = JSONObject.parseObject(s);
                        int code = jsonObject.getIntValue("code");
                        if(code!=0){
                            throw new Exception(jsonObject.getString("msg"));
                        }
                        JSONObject data = jsonObject.getJSONObject("data");
                        // 运行中
                        try {
                            HealthStatusRv healthRv = new HealthStatusRv(service);
                            healthRv.setJvmMaxMemory(data.getString("jvmMaxMemory"));
                            healthRv.setJvmTotalMemory(data.getString("jvmTotalMemory"));
                            healthRv.setJvmUsedMemory(data.getString("jvmUsedMemory"));
                            healthRv.setJvmThreadNum(data.getString("jvmThreadNum"));
                            mapper.success(healthRv);
                            isRunning = true;
                            break;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } catch (Exception e) {
                        // 故障 next
                    }
                }
                if (!isRunning) {
                    fault(service);
                }
            }
        }
    }

    public void fault(String service ){
        try {
            int count = mapper.faultCount(service);
            if (count == 0) {
                mapper.fault(service);
                // 报警
                String[] split = emailList.split(",");
                List<String> cclList = Convert.toList(String.class, split);
                String receiver = cclList.remove(0);
                sendEmail(service,env,nacosUrl,receiver,cclList);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendEmail(String service,String env,String nacosUrl,String receiver,List<String> ccList){
        String subject = "【系統故障通知】";
        String content = String.format("<!doctype html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\"\n" +
                "          content=\"width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0\">\n" +
                "    <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">\n" +
                "    <title>Document</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <pre>\n" +
                "    环境：%s\n"+
                "    服务：%s\n"+
                "    Nacos地址：%s/nacos/index.html\n" +
                "    </pre>\n" +
                "<h3 style=\"margin: 0\">此通知由服务监控系統發送，請勿回復。</h3>\n" +
                "</body>\n" +
                "</html>",env, service,nacosUrl);
        HashMap<String, String> parma = new HashMap<>();
        parma.put("sendTo", receiver);
        parma.put("sendCc", Convert.toStr(ccList).replace("[","").replace("]",""));
        parma.put("subject", subject);
        parma.put("htmlmsg", content);
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String data = gson.toJson(parma);
        String anotherString = mailClient.sendMail3Method(data);
        System.out.println(anotherString);
        if (!"success".equalsIgnoreCase(anotherString)) {
            System.out.println("报警邮件发送失败:"+receiver);
        }
    }

    public String testEmail(){
        String subject = "【系統故障通知】";
        String content = "Test发送邮件功能";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sendTo", "jian-jun.fan@foxconn.com");
        jsonObject.put("subject", subject);
        jsonObject.put("htmlmsg", content);
        return mailClient.sendMail3Method(jsonObject.toJSONString());
    }

    public static String httpPost(HashMap<String, String> map) {
        String content = "";
        try {
            String ruleName = map.get("ruleName").trim();
            String requestPath = map.get("requestPath").trim();
            String url = requestPath + ruleName;
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);
            Gson gson = new Gson();
            String params = gson.toJson(map);
            StringBody contentBody = new StringBody(params, CharsetUtils.get("UTF-8"));
            // 以浏览器兼容模式访问,否则就算指定编码格式,中文文件名上传也会乱码
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addPart("data", contentBody);
            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);
            HttpResponse response = httpClient.execute(httpPost);
            if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
                HttpEntity entitys = response.getEntity();
                if (entitys != null) {
                    content = EntityUtils.toString(entitys);
                }
            }
            httpClient.getConnectionManager().shutdown();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("post request commit error" + e);
            System.out.println("post request to microservice failure, please check microservice");
        }
        return content;
    }

}
