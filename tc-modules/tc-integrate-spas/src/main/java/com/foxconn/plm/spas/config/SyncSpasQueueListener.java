package com.foxconn.plm.spas.config;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.alibaba.fastjson.JSONObject;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.entity.param.BUListRp;
import com.foxconn.plm.entity.response.BURv;
import com.foxconn.plm.entity.response.R;
import com.foxconn.plm.feign.service.HDFSClient;
import com.foxconn.plm.feign.service.TcMailClient;
import com.foxconn.plm.spas.bean.SynSpasChangeData;
import com.foxconn.plm.spas.bean.SynSpasConstants;
import com.foxconn.plm.spas.bean.SynSpasHandleResults;
import com.foxconn.plm.spas.mapper.SynSpasChangeDataMapper;
import com.foxconn.plm.spas.mapper.SynTcChangeDataMapper;
import com.foxconn.plm.spas.service.impl.SynTcChangeDataServiceImpl;
import com.foxconn.plm.spas.service.impl.SynTcCustomerServiceImpl;
import com.foxconn.plm.spas.service.impl.SynTcProjectServiceImpl;
import com.foxconn.plm.spas.service.impl.SynTcSeriesServiceImpl;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import com.foxconn.plm.utils.tc.TCUtils;
import com.teamcenter.services.strong.query._2007_06.SavedQuery;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author Robert
 */
@Component
public class SyncSpasQueueListener {
    private static Log log = LogFactory.get();
    private static JSONObject httpmap = null;
    static {
        httpmap = new JSONObject();
        httpmap.put("sendTo", "cheryl.l.wang@foxconn.com,leky.p.li@foxconn.com,mindy.m.wu@foxconn.com,dane.d.wu@foxconn.com");
        httpmap.put("sendCc", "thomas.l.yang@foxconn.com");
        httpmap.put("subject", "【专案同步异常】请及时处理！");
    }

    @Value("${spring.cloud.nacos.discovery.namespace}")
    private String env;

    @Resource
    private AmqpTemplate amqpTemplate;
    @Resource
    private HDFSClient hdfsClient;
    @Resource
    private SynTcChangeDataServiceImpl synTcChangeDataServiceImpl;
    @Resource
    private SynTcCustomerServiceImpl synTcCustomerServiceImpl;
    @Resource
    private SynTcSeriesServiceImpl synTcSeriesServiceImpl;
    @Resource
    private SynTcProjectServiceImpl synTcProjectServiceImpl;

    @Resource
    private SynTcChangeDataMapper synTcChangeDataMapper;



    @Resource
    TcMailClient tcMail;

    public void handleDataChange(SynSpasChangeData synSpasChangeData) {
        TCSOAServiceFactory tCSOAServiceFactory = null;
        SynSpasHandleResults synSpasHandleResults = new SynSpasHandleResults();
        try {
            log.info("消息隊列開始處理任務 ====> "+synSpasChangeData.getId());
           Integer handleCnt= synTcChangeDataMapper.getHandleStatusCnt(synSpasChangeData.getId());
           if(handleCnt!=null&&handleCnt.intValue()>0){
               log.info("消息已經消費過了 ====> "+synSpasChangeData.getId());
               return;
           }
           //设置处理状态 (0 未处理 1 处理中 2 成功 3 处理失败)
            synSpasHandleResults.setId(synSpasChangeData.getId());
            synSpasHandleResults.setState(1);
            synTcChangeDataServiceImpl.addSynSpasChangeDataHandleResults(synSpasHandleResults);

            tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.SPAS1);
            if (synSpasChangeData.getCustomerName() != null && synSpasChangeData.getProductLine() != null) {
                String bu = getBU(synSpasChangeData);
                synSpasChangeData.setBu(bu);
            }
            operationTypeHandler(tCSOAServiceFactory, synSpasChangeData);
            synTcCustomerServiceImpl.synSpasDataToTc(tCSOAServiceFactory, synSpasChangeData);
            synTcSeriesServiceImpl.synSpasDataToTc(tCSOAServiceFactory, synSpasChangeData);
            synTcProjectServiceImpl.synSpasDataToTc(tCSOAServiceFactory, synSpasChangeData);
            synSpasHandleResults.setState(2);
            synSpasHandleResults.setCompleteTime(new Date());
            synTcChangeDataServiceImpl.addSynSpasChangeDataHandleResults(synSpasHandleResults);
        } catch (Exception e) {
            try {
                log.error(e.getMessage(),e);
                synSpasHandleResults.setState(3);
                synSpasHandleResults.setExceptionMessage(e.getMessage());
                synSpasHandleResults.setCompleteTime(new Date());
                synTcChangeDataServiceImpl.addSynSpasChangeDataHandleResults(synSpasHandleResults);
                String message = e.getMessage();

                message = env+"環境 , 專案【" + synSpasChangeData.getPlatformFoundId() + "】" +  message;

                httpmap.put("htmlmsg", message);
                tcMail.sendMail3Method(httpmap.toJSONString());
            }catch (Exception e0) {
                System.out.print(e0);
            }
        } finally {
            try {
                log.info("消息隊列結束處理任務 ====> "+synSpasChangeData.getId());
                if (tCSOAServiceFactory != null) {
                    tCSOAServiceFactory.logout();
                }
                Thread.sleep(5000);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    private void operationTypeHandler(TCSOAServiceFactory tCSOAServiceFactory, SynSpasChangeData synSpasChangeData) throws Exception {
        SavedQuery.ExecuteSavedQueriesResponse savedQueryResult = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(), SynSpasConstants.D9_FIND_PROJECT_FOLDER,
                new String[]{SynSpasConstants.D9_SPAS_ID}, new String[]{synSpasChangeData.getCustomerId()});
        ServiceData serviceData = savedQueryResult.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
            if (objs.length > 0) {
                if(!("C".equalsIgnoreCase(synSpasChangeData.getCustomerOperationType()))){
                    synSpasChangeData.setCustomerOperationType("U");
                }
            }else{
                if(synSpasChangeData.getPlatformFoundId()!=null){
                    synSpasChangeData.setCustomerOperationType("A");
                }else{
                    synSpasChangeData.setCustomerOperationType("U");
                }
            }
        }

        savedQueryResult = TCUtils.execute2Query(tCSOAServiceFactory.getSavedQueryService(), SynSpasConstants.D9_FIND_PROJECT_FOLDER,
                new String[]{SynSpasConstants.D9_SPAS_ID}, new String[]{synSpasChangeData.getSeriesId()});
        serviceData = savedQueryResult.serviceData;
        if (serviceData.sizeOfPartialErrors() == 0) {
            ModelObject[] objs = savedQueryResult.arrayOfResults[0].objects;
            if (objs.length > 0) {
                if(!("C".equalsIgnoreCase(synSpasChangeData.getSeriesOperationType()))){
                    synSpasChangeData.setSeriesOperationType("U");
                }
            }else{
                if(synSpasChangeData.getPlatformFoundId()!=null){
                    synSpasChangeData.setSeriesOperationType("A");
                }else{
                    synSpasChangeData.setSeriesOperationType("U");
                }
            }
        }
    }

    private String getBU(SynSpasChangeData synSpasChangeData) throws Exception {
        String bu = "";
        BUListRp buListRp = new BUListRp();
        String customerName = synSpasChangeData.getCustomerName();
        String productLine = synSpasChangeData.getProductLine();
        buListRp.setCustomer(customerName);
        buListRp.setProductLine(productLine);
        R<List<BURv>> buRv = hdfsClient.buList(buListRp);
        List<BURv> data = buRv.getData();
        if (data != null && data.size() > 0) {
            bu = data.get(0).getBu();
        }
        if ("".equals(bu)) {
            throw new Exception("專案【" + synSpasChangeData.getPlatformFoundId()
                    + "】同步失敗：" + "【" + customerName + "】"
                    + "【" + productLine + "】" + "未查询到BU配置，請進行BU配置更新！");
        }
        return bu;
    }
}
