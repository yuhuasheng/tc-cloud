package com.foxconn.dp.plm.hdfs.controller;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.nacos.shaded.com.google.gson.Gson;
import com.foxconn.dp.plm.hdfs.DesUtils;
import com.foxconn.dp.plm.hdfs.domain.entity.LOVEntity;
import com.foxconn.dp.plm.hdfs.domain.entity.UserEntity;
import com.foxconn.dp.plm.hdfs.domain.rp.EncryptStrRp;
import com.foxconn.dp.plm.hdfs.domain.rp.SendEmailRp;
import com.foxconn.dp.plm.hdfs.domain.rv.DeptRv;
import com.foxconn.dp.plm.hdfs.domain.rv.LoginSiteRv;
import com.foxconn.dp.plm.hdfs.service.MainService;
import com.foxconn.dp.plm.hdfs.service.UserService;
import com.foxconn.dp.plm.hdfs.service.impl.TCSOAClientConfigImpl;
import com.foxconn.dp.plm.privately.Access;
import com.foxconn.plm.entity.exception.BizException;
import com.foxconn.plm.entity.response.HealthStatusRv;
import com.foxconn.plm.entity.response.R;

import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.utils.net.NetUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;

@Api(tags = "其他")
@RestController
@Validated
public class MainController {
    private static Log log = LogFactory.get();
    private static String headEmpId;
    private static String admin;

    @Resource
    TcMailClient tcMailClient;

    static {
        try {
            Properties properties = PropertiesLoaderUtils.loadAllProperties("config.properties");
            admin = properties.getProperty("admin");
            headEmpId = properties.getProperty("request.head.empId");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    @Resource
    MainService mainService;
    @Resource
    UserService userService;

    @Value("${spring.application.name}")
    private String serverName;

    @ApiOperation("检查状态")
    @GetMapping(value = "/status")
    public R<HealthStatusRv> status() {
        HealthStatusRv statusRv = new HealthStatusRv(serverName);
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        statusRv.setJvmMaxMemory(runtime.maxMemory() / 1024 / 1024 + "M");
        statusRv.setJvmTotalMemory(runtime.totalMemory() / 1024 / 1024 + "M");
        statusRv.setJvmUsedMemory((totalMemory - freeMemory) / 1024 / 1024 + "M");
        statusRv.setJvmThreadNum(threadMXBean.getThreadCount() + "");
        return R.success(statusRv);
    }

    @PostMapping(value = "/getLOV")
    public R<Map<String, List<LOVEntity>>> getLOV(@RequestBody @NotNull List<String> list) {
        return R.success(mainService.getLOV(list));
    }

    @ApiOperation(value = "获取站点代码")
    @PostMapping(value = "/getLoginSite")
    public R<LoginSiteRv> getLoginSite(HttpServletRequest request) {
        String empId = request.getHeader(Access.check(headEmpId));
        LoginSiteRv loginSiteRv = new LoginSiteRv();

        if (admin.equals(empId)) {
            loginSiteRv.setSiteCode("WH");
            loginSiteRv.setBu("DT");
            List<String> allDept = mainService.getAllDept();
            List<DeptRv> list = new ArrayList<>();
            for (String dept : allDept) {
                list.add(new DeptRv(dept, "0"));
            }
            loginSiteRv.setDeptList(list);
            return R.success(loginSiteRv);
        }

        ArrayList<String> list = new ArrayList<>();
        list.add(empId);
        List<UserEntity> userInfoInSpas = userService.getUserInfoInSpas(list);
        if (userInfoInSpas.isEmpty()) {
            throw new BizException("請確認工號是否正確！");
        }
        loginSiteRv.setBu(userInfoInSpas.get(0).getBu());

        List<DeptRv> deptList = new ArrayList<>();
        loginSiteRv.setDeptList(deptList);
        List<LOVEntity> deptSpasToTC = mainService.getLOV("DeptSpasToTC");
        for (LOVEntity lovEntity : deptSpasToTC) {
            if (lovEntity.getValue().equals(userInfoInSpas.get(0).getDept())) {
                String code = lovEntity.getCode();
                String[] split = code.split("<->");
                String dept = split[0];
                String hasTC = split[1];
                deptList.add(new DeptRv(dept, hasTC));
            }
        }

        String ipAddr = NetUtil.getIpAddr(request);
        log.info("IP==============================" + ipAddr);
//        if("10.203.64.139".equalsIgnoreCase(ipAddr)){
//            loginSiteRv.setSiteCode("TPE");
//            return R.ok(loginSiteRv);
//        }
//        if("10.203.65.85".equalsIgnoreCase(ipAddr)){
//            loginSiteRv.setSiteCode("LH");
//            return R.ok(loginSiteRv);
//        }
        String loginSite = mainService.getLoginSite(ipAddr);
        loginSiteRv.setSiteCode(loginSite);
        return R.success(loginSiteRv);
    }

    @ApiOperation(value = "获取IP地址")
    @PostMapping(value = "/getIp")
    public R<String> getIp(HttpServletRequest request) {
        return R.success("success",NetUtil.getIpAddr(request));
    }

    @ApiOperation(value = "加密字符串")
    @PostMapping(value = "/encryptStr")
    public R<String> encryptStr(@RequestBody EncryptStrRp rp) {
        return R.success("success",DesUtils.encode(rp.getText()));
    }

    @ApiOperation(value = "解密字符串")
    @PostMapping(value = "/decryptStr")
    public R<String> decryptStr(@RequestBody EncryptStrRp rp) {
        return R.success("success",DesUtils.decode(rp.getText()));
    }

    @ApiOperation(value = "发送邮件")
    @PostMapping(value = "/sendEmail")
    public R<List<SendEmailRp.Receiver>> sendEmail(@RequestBody SendEmailRp rp) {
        List<SendEmailRp.Receiver> failureList = new ArrayList<>();
        String docUrl = rp.getDocUrl();
        String projectName = rp.getProjectName();
        String docName = rp.getDocName();
        List<SendEmailRp.Receiver> toList = rp.getTo();
        for (SendEmailRp.Receiver receiver : toList) {
            String subject = String.format("【文檔管理系統通知】『%s』.『%s』已上傳，請參考", projectName, docName);
            String name = receiver.getName();
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
                    "Dear 『%s』，\n" +
                    "\n" +
                    "    『%s』.『%s』 已上傳，請參考\n" +
                    "\n" +
                    "鏈接地址：%s\n" +
                    "    </pre>\n" +
                    "<h3 style=\"margin: 0\">此通知由文檔管理系統發送，請勿回復。</h3>\n" +
                    "</body>\n" +
                    "</html>", name, projectName, docName, docUrl);
            HashMap<String, String> parma = new HashMap<>();
            parma.put("requestPath", "http://10.203.163.43:");
            parma.put("ruleName", "80/tc-mail/teamcenter/sendMail3");
            parma.put("sendTo", receiver.getEmail());
//            parma.put("sendCc","jian-jun.fan@foxconn.com");
            parma.put("subject", subject);
            parma.put("htmlmsg", content);
            if (!"success".equalsIgnoreCase(httpPost(parma))) {
                failureList.add(receiver);
            }
        }
        return R.success("success",failureList);
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

    @GetMapping(value = "/sendMail3")
    public String sendMail3() {
        return "success";
    }

    @PostMapping(value = "/test")
    public String test() {
        String body = "Dear Dev,请登陆TC完成任务签核！此通知由Teamcenter系统发送";
        String msg = "<html><head></head><body>"+body+"</body></html>";
        Map<String, String> httpmap = new HashMap<>();
        httpmap.put("sendTo", "jian-jun.fan@foxconn.com");
        httpmap.put("sendCc", "hua-sheng.yu@foxconn.com");
        httpmap.put("subject", "【Matrix签核通知】请你登陆TC完成签核流程");
        httpmap.put("htmlmsg", msg);
        Gson gson = new Gson();
        String data = gson.toJson(httpmap);
        String pathname = "C:\\Users\\Oz\\Desktop\\999.xlsx";
        FileItem file = createFileItem(new File(pathname), "file");
        FileItem f2 = createFileItem(new File("C:\\Users\\Oz\\Desktop\\1.html"), "file");
//        FileItem f2 = createFileItem(new File("D:\\37942a279c8de881c8db2674f7b33ed5e602073445ef8e578239568ab800cdb8"), "file");
        tcMailClient.sendMail3Method(data, new CommonsMultipartFile(file),new CommonsMultipartFile(f2));// 发送邮件
        return data;
    }

    //把File转化为CommonsMultipartFile
    public FileItem createFileItem(File file, String fieldName) {
        //DiskFileItemFactory()：构造一个配置好的该类的实例
        //第一个参数threshold(阈值)：以字节为单位.在该阈值之下的item会被存储在内存中，在该阈值之上的item会被当做文件存储
        //第二个参数data repository：将在其中创建文件的目录.用于配置在创建文件项目时，当文件项目大于临界值时使用的临时文件夹，默认采用系统默认的临时文件路径
        FileItemFactory factory = new DiskFileItemFactory(16, null);
        //fieldName：表单字段的名称；第二个参数 ContentType；第三个参数isFormField；第四个：文件名
        FileItem item = factory.createItem(fieldName, "text/plain", true, file.getName());
        int bytesRead = 0;
        byte[] buffer = new byte[8192];
        FileInputStream fis = null;
        OutputStream os = null;
        try {
            fis = new FileInputStream(file);
            os = item.getOutputStream();
            while((bytesRead = fis.read(buffer, 0, 8192)) != -1) {
                os.write(buffer, 0, bytesRead);//从buffer中得到数据进行写操作
            }
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(os != null) {
                    os.close();
                }
                if(fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return item;
    }


}
