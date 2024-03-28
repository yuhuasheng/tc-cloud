package com.foxconn.plm.mail.config;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.foxconn.plm.mail.service.TCMailService;
import com.foxconn.plm.rabbitmq.constant.RabbitMQEnum;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.foxconn.plm.rabbitmq.constant.RabbitMQEnum.TCMail_RabbitMQ;

@Component
public class TCMailQueueListener {

    private static Log log = LogFactory.get();

    @Resource
    private TCMailService tcMailService;

    @Resource
    private AmqpTemplate amqpTemplate;

    @RabbitListener(queues = "tcmail.queue14")
    public void handleDataChange(Map map) throws Exception {
        log.info("******** Begin Consumer Message From RabbitMQ ********");
        Boolean flag = tcMailService.sendTCMail4Service(map);
        if (!flag) {
            log.error("==>> 邮件发送失败");
            amqpTemplate.convertAndSend(RabbitMQEnum.TCMailERROR_RabbitMQ.exchangeName(), RabbitMQEnum.TCMailERROR_RabbitMQ.routingkey(), map); // 将此消息移除到存在错误消息的队列中
        } else {
            log.info("==>> 邮件发送成功");
        }
    }
}
