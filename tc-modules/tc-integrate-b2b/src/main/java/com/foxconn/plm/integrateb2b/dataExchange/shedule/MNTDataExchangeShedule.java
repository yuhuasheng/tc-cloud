package com.foxconn.plm.integrateb2b.dataExchange.shedule;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.integrateb2b.dataExchange.constants.BUConstants;
import com.foxconn.plm.integrateb2b.dataExchange.constants.PlantConstants;
import com.foxconn.plm.integrateb2b.dataExchange.core.ext.MNTL6DataExchangeListener;
import com.foxconn.plm.integrateb2b.dataExchange.domain.TransferOrder;
import com.foxconn.plm.integrateb2b.dataExchange.mapper.DataExchangeMapper;
import com.foxconn.plm.rabbitmq.constant.RabbitMQEnum;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class MNTDataExchangeShedule {
    private static Log log = LogFactory.get();


    @Autowired(required = false)
    public DataExchangeMapper dataExchangeMapper;

    @Resource
    private AmqpTemplate amqpTemplate;

     //@PostConstruct
     @XxlJob("TcIntegratemntb2bSchedule")
    public void postMNTDCN() {
        try {
            log.info("==========================begin post MNTDCN =========================");
            List<TransferOrder> transferOrders = getTransferOrders();
            for (TransferOrder transferOrder : transferOrders) {
                    amqpTemplate.convertAndSend(RabbitMQEnum.TCIntegrateMNTB2B_RabbitMQ.exchangeName(), RabbitMQEnum.TCIntegrateMNTB2B_RabbitMQ.routingkey(), transferOrder);
            }
        } catch (Exception e) {
            log.info(e.getMessage());
            log.error(e.getMessage(), e);
            XxlJobHelper.handleFail(e.getLocalizedMessage());
        }
        log.info("==========================end post MNTDCN =========================");
    }

    private List<TransferOrder> getTransferOrders() throws Exception {
        List<String> plants= new ArrayList<>();
        String[] m=PlantConstants.MNT_L6.split(",");
        for(String str:m){
            plants.add(str);
        }
        m=PlantConstants.MNT_L10.split(",");
        for(String str:m){
            plants.add(str);
        }

        m=PlantConstants.MNT_L5.split(",");
        for(String str:m){
            plants.add(str);
        }

        List<TransferOrder> transferOrders = dataExchangeMapper.getTransferOrders(plants);
        for (TransferOrder transferOrder : transferOrders) {
            transferOrder.setSynFlag(1);
            dataExchangeMapper.updateSynFlag(transferOrder);
        }
        return transferOrders;
    }


}
