package com.foxconn.plm.tcservice.mailtrack.scheduled;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.feign.service.TcIntegrateClient;
import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.tcservice.mailtrack.domain.PoInfo;
import com.foxconn.plm.tcservice.mailtrack.domain.TrackResponse;
import com.foxconn.plm.tcservice.mailtrack.domain.UserPojo;
import com.foxconn.plm.tcservice.mailtrack.utils.MailTrackUtils;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.EPMTask;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class MailTrackSchedule {
    private static Log log = LogFactory.get();
    @Resource
    private TcIntegrateClient tcIntegrate;

    @Resource
    TcMailClient tcMail;
   // @PostConstruct
    @XxlJob("TcMailTrackSchedule")
    public void mailTrack() {
        log.info("********** start mailTrack  ********** ");
        TCSOAServiceFactory tcsoaServiceFactory = null;
        try {

            tcsoaServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
            SimpleDateFormat sdf = new SimpleDateFormat("HH");
            int h = Integer.parseInt(sdf.format(new Date()));
            if (h < 8 || h > 17) {
                log.info("************ 非跟催時段 ***********");
                return;
            }
            FileManagementUtility fileManagementUtility = tcsoaServiceFactory.getFileManagementUtility();

            DataManagementService dataManagementService = tcsoaServiceFactory.getDataManagementService();
            tcsoaServiceFactory.getSessionService().refreshPOMCachePerRequest(true);
            SavedQueryService savedQueryService = tcsoaServiceFactory.getSavedQueryService();
            ModelObject[] tasks = MailTrackUtils.taskQuery(savedQueryService, "4", "EPMReviewTask;EPMConditionTask;EPMAcknowledgeTask", "FXN41*");
            if (tasks == null || tasks.length <= 0) {
                log.info("********* 未查詢到要跟催的數據 *********");
                return;
            }

            for (ModelObject m : tasks) {
                log.info("begin track task " + m.getUid());
                try {
                    List<TrackResponse> trackResponses = MailTrackUtils.getTrackResponse(savedQueryService, dataManagementService, (EPMTask) m, fileManagementUtility, tcIntegrate);
                    HashMap<String, List<TrackResponse>> mp = new HashMap<>();
                    for (TrackResponse r : trackResponses) {
                        List<TrackResponse> ls = mp.get(r.getUid());
                        if (ls == null) {
                            ls = new ArrayList<>();
                            mp.put(r.getUid(), ls);
                        }
                        ls.add(r);
                    }
                    Set<String> keys = mp.keySet();
                    for (String key : keys) {
                        List<TrackResponse> ls = mp.get(key);
                        HashMap<String, String> hasMp = new HashMap<>();
                        for (TrackResponse r : ls) {
                            List<UserPojo> trackers = r.getTrackers();
                            if (trackers == null || trackers.size() <= 0) {
                                continue;
                            }
                            if (!r.getPoInfo().isNeedTrack()) {
                                continue;
                            }
                            for (UserPojo u : trackers) {
                                if (hasMp.get(u.getUserId()) != null) {
                                    continue;
                                }
                                hasMp.put(u.getUserId(), u.getUserId());
                                if (u.getMail() == null || "".equalsIgnoreCase(u.getMail())) {
                                    continue;
                                }
                                String html = MailTrackUtils.genTrackMailHtml(r, u.getUserName());
                                JSONObject httpmap = new JSONObject();
                                httpmap.put("sendTo", u.getMail());
                                PoInfo poInfo = r.getPoInfo();
                                String pmMail = poInfo.getPmMail();
                                if (pmMail != null && !("".equalsIgnoreCase(pmMail))) {
                                    httpmap.put("sendCc", pmMail + ",leky.p.li@foxconn.com,dong.d.li@foxconn.com");
                                } else {
                                    httpmap.put("sendCc", "leky.p.li@foxconn.com,dong.d.li@foxconn.com");
                                }
                                if (poInfo.getRealDelayHours() > 0) {
                                    httpmap.put("subject", "【邮件跟催】 PCBA BOM製作申請[" + poInfo.getItemId() + "/" + poInfo.getItemRev() + "],已逾期" + (poInfo.getRealDelayHours() + 48) + "小時,請及時處理");
                                } else {
                                    if (Math.abs(poInfo.getRealDelayHours()) < 48) {
                                        httpmap.put("subject", "【邮件跟催】 PCBA BOM製作申請[" + poInfo.getItemId() + "/" + poInfo.getItemRev() + "],已逾期" + (48 - Math.abs(poInfo.getRealDelayHours())) + "小時,請及時處理");
                                    } else {
                                        httpmap.put("subject", "【邮件跟催】 PCBA BOM製作申請[" + poInfo.getItemId() + "/" + poInfo.getItemRev() + "],距離逾期還剩" + (Math.abs(poInfo.getRealDelayHours()) - 48) + "小時請及時處理");
                                    }
                                }
                                log.info("mail to =====" + u.getMail());
                                log.info("mail title =====" + httpmap.getString("subject"));
                                log.info("mail content =====" + html);
                                httpmap.put("htmlmsg", html);
                                tcMail.sendMail3Method(httpmap.toJSONString());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.info(e.getMessage());
                    log.error(e.getMessage(), e);
                }
                log.info("end track task " + m.getUid());
            }

        } catch (Exception e) {
            System.out.println(e);
            XxlJobHelper.handleFail(e.getLocalizedMessage());
        } finally {
            try {
                if (tcsoaServiceFactory != null) {
                    tcsoaServiceFactory.logout();
                }
            } catch (Exception e) {
            }
            log.info("end mailTrack   ******** ");
        }

    }


}
