package com.foxconn.plm.spas.scheduled;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.spas.bean.SynSpasChangeData;
import com.foxconn.plm.spas.bean.SynSpasHandleResults;
import com.foxconn.plm.spas.config.SyncSpasQueueListener;
import com.foxconn.plm.spas.service.impl.SynSpasChangeDataServiceImpl;
import com.foxconn.plm.spas.service.impl.SynSpasDBServiceImpl;
import com.foxconn.plm.spas.service.impl.SynTcChangeDataServiceImpl;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author {chen.zhang@foxconn.com}
 * @Date: 2022/11/23/ 13:44
 * @description
 */

@Component
public class SynSpasProjectTask {
    private static Log log = LogFactory.get();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final SimpleDateFormat hourMinuteSecond = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat yearMonthDay = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat sdf2 = new SimpleDateFormat("HH");
    private static String startDate;
    private static String endDate;

    private void updateDate() {
        Integer h= Integer.parseInt(sdf2.format(new Date()));
        Calendar c1 = Calendar.getInstance();
        if(h.intValue()<4){
            c1.add(Calendar.DAY_OF_MONTH, -1);
        }else {
            c1.add(Calendar.DAY_OF_MONTH, 0);
        }
        c1.add(Calendar.DAY_OF_MONTH, 0);
        startDate = yearMonthDay.format(c1.getTime());
        Calendar c2 = Calendar.getInstance();
        c2.add(Calendar.DAY_OF_MONTH, 1);
        endDate = yearMonthDay.format(c2.getTime());
    }

    @Resource(name = "synSpasChangeDataServiceImpl")
    SynSpasChangeDataServiceImpl synSpasChangeDataServiceImpl;
    @Resource(name = "synTcChangeDataServiceImpl")
    SynTcChangeDataServiceImpl synTcChangeDataServiceImpl;

    @Resource
    private AmqpTemplate amqpTemplate;

    @Resource
    SyncSpasQueueListener syncSpasQueueListener;

    @Resource
    private SynSpasDBServiceImpl synSpasDBServiceImpl;


    // @PostConstruct
     //@Scheduled(cron = "0 30 * * * ? ")//每个半点执行一次
     @XxlJob("synSpasProject")
    public void timedTask() {

        log.info("开始同步SPAS变化数据到TC数据库中.");
        try {
            SynSpasDBServiceImpl.updateDate();
            synSpasDBServiceImpl.addManpowerStandardData();
            synSpasDBServiceImpl.addProductLinePhaseData();
            updateDate();
            List<SynSpasChangeData> synSpasChangeDataList = synSpasChangeDataServiceImpl.querySynSpasChangeData(startDate, endDate);
            for (int i = 0; i < synSpasChangeDataList.size(); i++) {
                log.info(startDate + "SPAS变化数据：" + synSpasChangeDataList.get(i));
            }
            if (synSpasChangeDataList.size() != 0) {
                List<SynSpasChangeData> addSynSpasChangeData = new ArrayList<>();
                for (int i = 0; i < synSpasChangeDataList.size(); i++) {
                    SynSpasChangeData synSpasChangeData = synSpasChangeDataList.get(i);
                    Integer id = synSpasChangeData.getId();
                    Integer count = synSpasChangeDataServiceImpl.querySynSpasChangeDataRecord(id);
                    if (count == 0) {
                        addSynSpasChangeData.add(synSpasChangeData);
                    }
                }
                for (int i = 0; i < addSynSpasChangeData.size(); i++) {
                    log.info("SPAS需同步的变化数据：" + addSynSpasChangeData.get(i));
                }
                if (addSynSpasChangeData.size() > 0) {
                    synSpasChangeDataServiceImpl.addSynSpasChangeData(addSynSpasChangeData);
                }
            } else {
                log.info("开始同步SPAS变化数据到TC数据库中.");
            }
        } catch (Exception e) {
            XxlJobHelper.handleFail(e.getLocalizedMessage());
            log.info("同步SPAS专案错误：" + e.getMessage());
        }
        log.info("结束同步SPAS变化数据到TC数据库中.");

        log.info("开始同步SPAS专案.");
        List<SynSpasChangeData> spasChangeDataList = synTcChangeDataServiceImpl.querySynSpasChangeData();
        spasChangeDataList = spasChangeDataList.stream().sorted(Comparator.comparing(SynSpasChangeData::getId)).collect(Collectors.toList());

        for (int i = 0; i < spasChangeDataList.size(); i++) {
            SynSpasChangeData synSpasChangeData = spasChangeDataList.get(i);
            SynSpasHandleResults synSpasHandleResults = new SynSpasHandleResults();
            synSpasHandleResults.setId(synSpasChangeData.getId());
            synSpasHandleResults.setState(1);
            synTcChangeDataServiceImpl.addSynSpasChangeDataHandleResults(synSpasHandleResults);
            log.info("添加任務到消息隊列 =====> "+synSpasChangeData.getId());

            syncSpasQueueListener.handleDataChange(synSpasChangeData);
        }
        log.info("结束同步SPAS专案.");
    }

}
