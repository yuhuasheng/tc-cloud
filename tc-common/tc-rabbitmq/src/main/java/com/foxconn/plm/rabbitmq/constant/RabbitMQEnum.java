package com.foxconn.plm.rabbitmq.constant;

public enum RabbitMQEnum {

    TCIntegrateMNTB2B_RabbitMQ("tcintegratemntb2b.key14", "tcintegratemntb2b.queue14", "tcintegratemntb2b.exchange14"),
    TCMail_RabbitMQ("tcmail.routing.key14", "tcmail.queue14", "tcmail.exchange14"),
    TCMailERROR_RabbitMQ("tcmailerror.routing.key14", "tcmailerror.queue14", "tcmailerror.exchange14");
    private final String routingkey;
    private final String queueName;
    private final String exchangeName;

    private RabbitMQEnum(String routingkey, String queueName, String exchangeName) {
        this.routingkey = routingkey;
        this.queueName = queueName;
        this.exchangeName = exchangeName;
    }

    public String routingkey() {
        return routingkey;
    }

    public String queueName() {
        return queueName;
    }

    public String exchangeName() {
        return exchangeName;
    }
}
