package com.foxconn.plm.rabbitmq.dynamic;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RabbitModuleInitializer implements SmartInitializingSingleton {
    private static Log log = LogFactory.get();
    private AmqpAdmin amqpAdmin;

    private RabbitModuleProperties rabbitModuleProperties;

    public RabbitModuleInitializer(AmqpAdmin amqpAdmin, RabbitModuleProperties rabbitModuleProperties) {
        this.amqpAdmin = amqpAdmin;
        this.rabbitModuleProperties = rabbitModuleProperties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        declareRabbitModule();
    }

    private void declareRabbitModule() {
        List<RabbitModuleInfo> rabbitModuleInfos = rabbitModuleProperties.getModules();
        if (CollectionUtil.isEmpty(rabbitModuleInfos)) {
            return;
        }
        for (RabbitModuleInfo rabbitModuleInfo : rabbitModuleInfos) {
            configParamValidate(rabbitModuleInfo);

            Queue queue = convertQueue(rabbitModuleInfo.getQueue());
            Exchange exchange = convertExchange(rabbitModuleInfo.getExchange());
            String routingKey = rabbitModuleInfo.getRoutingKey();
            String queueName = rabbitModuleInfo.getQueue().getName();
            String exchangeName = rabbitModuleInfo.getExchange().getName();
            Binding binding = new Binding(queueName, Binding.DestinationType.QUEUE, exchangeName, routingKey, null);

            amqpAdmin.declareQueue(queue);
            amqpAdmin.declareExchange(exchange);
            amqpAdmin.declareBinding(binding);
        }
    }

    public void configParamValidate(RabbitModuleInfo rabbitModuleInfo) {

        String routingKey = rabbitModuleInfo.getRoutingKey();

        Assert.isTrue(StrUtil.isNotBlank(routingKey), "RoutingKey 未配置");

        Assert.isTrue(rabbitModuleInfo.getExchange() != null, "routingKey:{}未配置exchange", routingKey);
        Assert.isTrue(StrUtil.isNotBlank(rabbitModuleInfo.getExchange().getName()), "routingKey:{}未配置exchange的name属性", routingKey);

        Assert.isTrue(rabbitModuleInfo.getQueue() != null, "routingKey:{}未配置queue", routingKey);
        Assert.isTrue(StrUtil.isNotBlank(rabbitModuleInfo.getQueue().getName()), "routingKey:{}未配置queue的name属性", routingKey);

    }

    public Queue convertQueue(RabbitModuleInfo.Queue queue) {
        Map<String, Object> arguments = queue.getArguments();

        if (arguments != null && arguments.containsKey("x-message-ttl")) {
            arguments.put("x-message-ttl", Convert.toLong(arguments.get("x-message-ttl")));
        }

        String deadLetterExchange = queue.getDeadLetterExchange();
        String deadLetterRoutingKey = queue.getDeadLetterRoutingKey();
        if (StrUtil.isNotBlank(deadLetterExchange) && StrUtil.isNotBlank(deadLetterRoutingKey)) {

            if (arguments == null) {
                arguments = new HashMap<>(4);
            }
            arguments.put("x-dead-letter-exchange", deadLetterExchange);
            arguments.put("x-dead-letter-routing-key", deadLetterRoutingKey);

        }

        return new Queue(queue.getName(), queue.isDurable(), queue.isExclusive(), queue.isAutoDelete(), arguments);
    }


    public Exchange convertExchange(RabbitModuleInfo.Exchange exchangeInfo) {

        AbstractExchange exchange = null;

        RabbitExchangeTypeEnum exchangeType = exchangeInfo.getType();

        String exchangeName = exchangeInfo.getName();
        boolean isDurable = exchangeInfo.isDurable();
        boolean isAutoDelete = exchangeInfo.isAutoDelete();

        Map<String, Object> arguments = exchangeInfo.getArguments();

        switch (exchangeType) {
            case DIRECT:// 直连交换机
                exchange = new DirectExchange(exchangeName, isDurable, isAutoDelete, arguments);
                break;
            case TOPIC: // 主题交换机
                exchange = new TopicExchange(exchangeName, isDurable, isAutoDelete, arguments);
                break;
            case FANOUT: //扇形交换机
                exchange = new FanoutExchange(exchangeName, isDurable, isAutoDelete, arguments);
                break;
            case HEADERS: // 头交换机
                exchange = new HeadersExchange(exchangeName, isDurable, isAutoDelete, arguments);
                break;
        }

        return exchange;

    }


}
