package com.foxconn.plm.integrate.tcfr.service;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.dp.plm.privately.Access;
import com.foxconn.plm.entity.constants.HttpResultEnum;
import com.foxconn.plm.entity.constants.TCScheduleConstant;
import com.foxconn.plm.entity.constants.TCSearchEnum;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.integrate.config.properties.TCFRProxyConfig;
import com.foxconn.plm.integrate.tcfr.domain.*;
import com.foxconn.plm.integrate.tcfr.mapper.TCFRMapper;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.collect.CollectUtil;
import com.foxconn.plm.utils.ip.IpUtil;
import com.foxconn.plm.utils.net.HttpUtil;
import com.foxconn.plm.utils.string.StringUtil;
import com.foxconn.plm.utils.tc.TCUtils;
import com.google.gson.Gson;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.ScheduleTask;
import com.teamcenter.soa.exceptions.NotLoadedException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import static com.foxconn.plm.integrate.tcfr.domain.TCFRConstant.TEMPLATEPATH;

@Service
public class MeetService {

    @Resource
    TCFRMapper tcfrMapper;

    @Resource
    private TcMailClient tcMailClient;

    @Resource
    private TCFRProxyConfig tcfrProxyConfig;

    @Resource
    public RedisTemplate redisTemplate;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    private static Log log = LogFactory.get();

    public void sendMeetEmail(JSONObject param) {
        log.info("==>> param: " + param.toJSONString());
        String scheduleId = param.getString("scheduleUid");
        String scheduleName = param.getString("scheduleName");
        String endDate = param.getString("endDate");
        String taskName = param.getString("taskName");
        MeetBean meetBean = tcfrMapper.getTCFRDataByScheduleUid(scheduleId);
        TCUserBean tcUserInfo = tcfrMapper.getTCUserInfo(meetBean.getMeetMainOwner());
        String msg = "<html><head></head><body>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "Dear " + tcUserInfo.getWorkId() + "(" + tcUserInfo.getWorkName() + ")" +
                "用户，</div><br/>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "以下時間表任務已執行完成，请登陆下方Teamcenter賬號进行查看，谢谢！" + "</div>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>Teamcenter账号：</strong>" + tcUserInfo.getTcUserId() + "</div>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>时间表名称：</strong>" + scheduleName + "</div>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>时间表任务：</strong>" + taskName + "</div>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>Due date：</strong>" + endDate + "</div>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>任务路径:</strong>D事業群企業知識庫/專案知識庫</div>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>专案系列:</strong>" + meetBean.getSpasSeries() + "</div>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>专案名:</strong>" + meetBean.getProjectName() + "</div>"
                + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>专案阶段:</strong>" + meetBean.getSpasProjPhase() + "</div>"
                + "</body></html>";
        Map<String, String> httpmap = new HashMap<>();
        httpmap.put("sendTo", meetBean.getMeetMainOwner());
//        httpmap.put("sendCc", "hua-sheng.yu@foxconn.com");
        httpmap.put("subject", "任務執行完成");
        httpmap.put("htmlmsg", msg);
        Gson gson = new Gson();
        String data = gson.toJson(httpmap);
        tcMailClient.sendMail3Method(data);// 发送邮件

    }

    public void sendMeetCompleteTaskEmail(String scheduleId, String endDate, String taskId) {
        log.info("==>> scheduleId: " + scheduleId);
        log.info("==>> endDate: " + endDate);
        log.info("==>> taskId: " + taskId);
        TCSOAServiceFactory tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS2);
        try {
            DataManagementService dataManagementService = tcsoaServiceFactory.getDataManagementService();
            ModelObject taskObject = TCUtils.findObjectByUid(dataManagementService, taskId);
            dataManagementService.getProperties(new ModelObject[]{taskObject}, new String[]{"object_name", "object_desc"});
            String taskName = taskObject.getPropertyObject("object_name").getStringValue();
            // String taskDesc = taskObject.getPropertyObject("object_desc").getStringValue();
            MeetBean meetBean = tcfrMapper.getTCFRDataByTCUid(scheduleId);
            if (meetBean != null) {
                TCUserBean tcUserInfo = tcfrMapper.getTCUserInfo(meetBean.getMeetMainOwner());
                String msg = "<html><head></head><body>"
                        + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "Dear " + tcUserInfo.getWorkId() + "(" + tcUserInfo.getWorkName() + ")" +
                        "用户，</div><br/>"
                        + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "以下時間表任務已執行完成，请登陆下方Teamcenter賬號进行查看，谢谢！" + "</div>"
                        + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>Teamcenter账号：</strong>" + tcUserInfo.getTcUserId() +
                        "</div>"
                        + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>时间表名称：</strong>" + meetBean.getMeetingTitle() + "</div>"
                        + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>时间表任务：</strong>" + taskName + "</div>"
                        + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>Due date：</strong>" + endDate + "</div>"
                        + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>任务路径:</strong>D事業群企業知識庫/專案知識庫</div>"
                        + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>专案系列:</strong>" + meetBean.getSpasSeries() + "</div>"
                        + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>专案名:</strong>" + meetBean.getProjectName() + "</div>"
                        + "<div style=\"font-family: 宋体;  font-size:15px; \">" + "<strong>专案阶段:</strong>" + meetBean.getSpasProjPhase() + "</div>"
                        + "</body></html>";
                Map<String, String> httpmap = new HashMap<>();
                httpmap.put("sendTo", meetBean.getMeetMainOwner());
                httpmap.put("sendCc", "robert.y.peng@foxconn.com,hua-sheng.yu@foxconn.com");
                httpmap.put("subject", "任務執行完成");
                httpmap.put("htmlmsg", msg);
                Gson gson = new Gson();
                String data = gson.toJson(httpmap);
                String result = tcMailClient.sendMail3Method(data);// 发送邮件
                log.info("==>> result:" + result);
            } else {
                log.info("query meetBean is null : scheduleId=" + scheduleId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage());
        } finally {
            tcsoaServiceFactory.logout();
        }
    }

    public R sendTaskStatusToTCFR(String taskId, String actionItemId, String state) {
        JSONObject paramsObject = new JSONObject();
        try {
            log.info("==>> taskId: " + taskId);
            log.info("==>> actionItemId: " + actionItemId);
            log.info("==>> state: " + state);
            paramsObject.put("actionItemId", actionItemId);
            paramsObject.put("status", state);
            log.info("==>> params: " + paramsObject.toJSONString());
            String proxyHost = getProxyHost();
            String result = HttpUtil.httpPost(tcfrProxyConfig.getTaskStatusUrl(), null, paramsObject.toJSONString(), "Y", 15000, proxyHost, tcfrProxyConfig.getPort());
            JSONObject json = JSONObject.parseObject(result);
            if (json.get("msg").equals("failure")) {
                throw new Exception("==>> 传递任务状态到TCFR失败");
            }
            log.info("==>> json: " + json);
            return R.success(json.get("msg"));
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getLocalizedMessage());
            Object value = redisTemplate.opsForHash().get(TCFRConstant.SCHEDULETASKSTATUSREDISKEY, taskId);
            if (null == value) {
                SessionCallback<Object> callback = new SessionCallback<Object>() {
                    @Override
                    public Object execute(RedisOperations operations) throws DataAccessException {
                        try {
                            operations.multi();
                            operations.opsForHash().put(TCFRConstant.SCHEDULETASKSTATUSREDISKEY, taskId, paramsObject.toJSONString());
                            return operations.exec();
                        } catch (Exception e) {
                            operations.discard();
                        }
                        return null;
                    }
                };
                redisTemplate.execute(callback);
            }
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), e.getLocalizedMessage());
        }
    }

    /**
     * 获取代理主机ip
     * @return
     */
    private String getProxyHost() {
        List<String> ipList = IpUtil.getRealIP();
        ipList.removeIf(str -> !str.startsWith("10.203") && !str.startsWith("10.205"));
        String localIP = ipList.get(0);
        System.out.println("==>> localIP: " + localIP);
        log.info("==>> localIP: " + localIP);
        String proxyHost = null;
        if (localIP.startsWith("10.203")) {
            proxyHost = tcfrProxyConfig.getServerProxyIp();
        } else if (localIP.startsWith("10.205")) {
            proxyHost = tcfrProxyConfig.getClientProxtIp();
        }
        return proxyHost;
    }


    public R manualSendTaskStatus(String startDate, String endDate) {
        log.info("============>> 开始获取需要同步时间表任务状态到TCFR记录 manualSendTaskStatus ============>>");
        TCSOAServiceFactory tcSOAServiceFactory = null;
        try {
            log.info("==>> startDate: " + startDate);
            log.info("==>> endDate: " + endDate);

            Date stDate = sdf.parse(startDate);
            Date edDate = sdf.parse(endDate);

            endDate = (edDate.getTime() / 1000) + "_epoch_time";
            startDate = (stDate.getTime() / 1000) + "_epoch_time";

            tcSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS2);

            SavedQueryService savedQueryService = tcSOAServiceFactory.getSavedQueryService();
            DataManagementService dataManagementService = tcSOAServiceFactory.getDataManagementService();

            Map<String, Object> queryResults = TCUtils.executeQueryByEntries(savedQueryService, TCSearchEnum.D9_Find_Schedule_Task.queryName(),
                    TCSearchEnum.D9_Find_Schedule_Task.queryParams(), new String[]{startDate, endDate});

            if (queryResults.get("succeeded") == null) {
                throw new Exception("未查询到數據");
            }

            ModelObject[] md = (ModelObject[]) queryResults.get("succeeded");
            if (md == null || md.length <= 0) {
                throw new Exception("未查询到數據");
            }

            List<TaskStatusPojo> tasks = new CopyOnWriteArrayList<>();

            Stream.of(md).parallel().forEach(obj -> {
                ScheduleTask task = (ScheduleTask) obj;
                TCUtils.getProperty(dataManagementService, task, TCScheduleConstant.PROPERTY_OBJECT_DESC);
                try {
                    String actionItemId = task.get_object_desc();
                    if (StringUtil.isEmpty(actionItemId)) {
                        return;
                    }

                    dataManagementService.refreshObjects(new ModelObject[]{task});
                    TCUtils.getProperty(dataManagementService, task, TCScheduleConstant.PROPERTY_FND0STATE);

                    String state = task.get_fnd0state();
                    if (TCScheduleConstant.STATE_CLOSED.equalsIgnoreCase(state) || TCScheduleConstant.STATE_COMPLETE.equalsIgnoreCase(state)) {
                        state = "1";
                    } else {
                        state = "0";
                    }

                    TaskStatusPojo taskStatusPojo = new TaskStatusPojo();
                    taskStatusPojo.setActionItemId(actionItemId);
                    taskStatusPojo.setStatus(state);
                    tasks.add(taskStatusPojo);

                } catch (NotLoadedException e) {
                    e.printStackTrace();
                }
            });

            log.info("============>> 结束需要同步时间表任务状态到TCFR记录 manualSendTaskStatus ============>>");

            log.info("============>> 开始手工同步时间表任务状态到TCFR manualSendTaskStatus ============>>");
            String proxyHost = getProxyHost();

            for (TaskStatusPojo task : tasks) {
                String result = HttpUtil.httpPost(tcfrProxyConfig.getTaskStatusUrl(), null, JSONUtil.toJsonPrettyStr(task), "Y", 15000, proxyHost, tcfrProxyConfig.getPort());
                JSONObject json = JSONObject.parseObject(result);
                if (json.get("msg").equals("failure")) {
                    throw new Exception("==>> 传递任务状态到TCFR失败");
                }
                log.info("==>> json: " + json);
            }
            log.info("============>> 结束手工同步时间表任务状态到TCFR manualSendTaskStatus ============>>");

            return R.success("时间表任务状态手工同步TCFR完成");
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), e.getLocalizedMessage());
        } finally {
            if (tcSOAServiceFactory != null) {
                tcSOAServiceFactory.logout();
            }
        }
    }

    public R updateSpasToTC(List<SpasToTCBean> list, String operation) {
        log.info("==>> list: " + list.toString());
        log.info("==>> operation: " + operation);
        try {
            if ("add".equalsIgnoreCase(operation)) {
                tcfrMapper.insertOrUpdateSpasToTC(list);
                return R.success("SPAS用户信息更新到TC中成功");
            } else if ("delete".equalsIgnoreCase(operation)) {
                tcfrMapper.deleteBySpasUserId(list);
                return R.success("SPAS用户信息从TC中移除成功");
            }
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), "此操作不合法");
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), e.getLocalizedMessage());
        }
    }


    public String downloadTemplate() throws IOException {
        InputStream in = null;
        ByteArrayOutputStream out = null;
        try {
            in = ResourceUtil.getStream(TEMPLATEPATH);
            out = new ByteArrayOutputStream();
            IoUtil.copy(in,out);
            //先声明的流后关掉！
            out.flush();
            return Base64.encode(out.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (out != null) {
                out.close();;
            }

            if (in != null) {
                in.close();
            }
        }
    }

    public R sendTCInfoToTCFR() {
        try {
            log.info("============>> 开始获取TCFR TC账号信息 ============>>");
            String userName= Access.getPasswordAuthentication();
            List<TCUserBean> totalTCUserList = tcfrMapper.getTotalTCUserInfo(Access.check(userName));
            if (CollUtil.isEmpty(totalTCUserList)) {
                return R.error(HttpResultEnum.SERVER_ERROR.getCode(), "TC账号信息为空");
            }

            totalTCUserList.removeIf(tcUserBean -> StringUtil.isEmpty(tcUserBean.getEmail()) || StringUtil.isEmpty(tcUserBean.getTcUserId())); // 移除邮箱或者TC账号为空的记录
            log.info("============>> 获取TCFR TC账号信息结束 ============>>");

            log.info("============>> 开始传递TC账号信到TCFR ============>>");
            JSONObject paramsObject = new JSONObject();
            paramsObject.put("data", totalTCUserList);

            String proxyHost = getProxyHost(); // 获取代理主机ip

            String result = HttpUtil.httpPost(tcfrProxyConfig.getTcUserParamsUrl(), null, paramsObject.toJSONString(), "Y", 15000, proxyHost, tcfrProxyConfig.getPort());
            JSONObject json = JSONObject.parseObject(result);
            if (json.get("msg").equals("failure")) {
                throw new Exception("==>> 传递TC账号信息到TCFR失败");
            }

            log.info("============>> 传递TC账号信到TCFR结束 ============>>");
            return R.success(json);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(HttpResultEnum.SERVER_ERROR.getCode(), e.getLocalizedMessage());
        }
    }
}
