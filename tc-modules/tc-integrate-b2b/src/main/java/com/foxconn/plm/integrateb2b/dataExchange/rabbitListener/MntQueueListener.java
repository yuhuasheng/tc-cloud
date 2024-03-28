package com.foxconn.plm.integrateb2b.dataExchange.rabbitListener;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.entity.constants.TCUserEnum;
import com.foxconn.plm.integrateb2b.dataExchange.core.DataExchangeListener;
import com.foxconn.plm.integrateb2b.dataExchange.core.ext.MNTL6DataExchangeListener;
import com.foxconn.plm.integrateb2b.dataExchange.core.ext.MNTL10DataExchangeListener;
import com.foxconn.plm.integrateb2b.dataExchange.domain.PostB2BResp;
import com.foxconn.plm.integrateb2b.dataExchange.domain.TransferOrder;
import com.foxconn.plm.integrateb2b.dataExchange.domain.TransferOrderResp;
import com.foxconn.plm.integrateb2b.dataExchange.factory.MNTDataExchangeFactory;
import com.foxconn.plm.tcapi.service.TCSOAServiceFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MntQueueListener {

    private static Log log = LogFactory.get();

    @Autowired
    MNTDataExchangeFactory mNTDataExchangeFactory;

    @RabbitListener(queues = "tcintegratemntb2b.queue14",concurrency = "1")
    public void handleDataChange(TransferOrder transferOrder) throws Exception {
        log.info("********Begin Consumer Message From RabbitMQ ********");
        TCSOAServiceFactory tCSOAServiceFactory=null;
        DataExchangeListener dataExchangeListener=null;
        try {
            log.info("==========================begin post MNTDCN  queue =========================");
                tCSOAServiceFactory = new TCSOAServiceFactory(TCUserEnum.DEV);
                TransferOrderResp transferOrderResp = new TransferOrderResp();
                String changeNum=transferOrder.getChangNum();
                String plantCode=transferOrder.getPlantCode();
                try {
                    log.info("begin dealwith ecn:" + changeNum + " plant:" + plantCode);
                    transferOrderResp.setChangeSn(transferOrder.getChangeSn());
                    transferOrderResp.setChangNum(changeNum);
                    transferOrderResp.setPlantCode(plantCode);
                    if(changeNum==null||"".equalsIgnoreCase(changeNum)){
                        throw new Exception ("ECN NO is null");
                    }
                    if(plantCode==null||"".equalsIgnoreCase(plantCode)){
                        throw new Exception ("Plant is null");
                    }
                    dataExchangeListener=mNTDataExchangeFactory.getDataExchangeListener(plantCode);
                    String jsonStr = dataExchangeListener.buildTransferJson(tCSOAServiceFactory,transferOrder);
                    log.info("json data:" + jsonStr);
                    PostB2BResp postRp=dataExchangeListener.postJsonData(jsonStr);
                    if(postRp.getCode()==1){
                        transferOrderResp.setCode(2);
                        transferOrderResp.setMsg(postRp.getMsg());
                    }else{
                        transferOrderResp.setCode(-1);
                        transferOrderResp.setMsg(postRp.getMsg());
                    }
                     dataExchangeListener.updateTransferOrder(transferOrderResp);
                    log.info("end dealwith ecn:" + changeNum + " plant:" + plantCode);
                }catch(Exception e){
                    transferOrderResp.setCode(-1);
                    transferOrderResp.setMsg(e.getMessage());
                    dataExchangeListener.updateTransferOrder(transferOrderResp);
                    log.info("failed dealwith ecn:" + changeNum + " plant:" + plantCode+e.getMessage());
                    log.error(e.getMessage(),e);
                }
                log.info("end dealwith ecn:" + changeNum + " plant:" + plantCode);

        }catch(Exception e){
            log.info(e.getMessage());
            log.error(e.getMessage(),e);
        }finally {
            if(tCSOAServiceFactory!=null){
                try {
                    tCSOAServiceFactory.logout();
                }catch (Exception e){}
            }
        }
        log.info("==========================end post MNTDCN queue =========================");
        log.info("********End Consumer Message From RabbitMQ ********");

    }


}
