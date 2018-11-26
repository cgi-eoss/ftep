package com.cgi.eoss.ftep.queues.service;

import java.io.Serializable;
import java.util.Map;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;

@Log4j2
public class FtepJMSQueueService implements FtepQueueService {

    private final JmsTemplate jmsTemplate;
    private final int RECEIVE_TIMEOUT_DEFINITE_WAIT = 5000;

    @Autowired
    public FtepJMSQueueService(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    public void sendObject(String queueName, Object object) {
        jmsTemplate.convertAndSend(queueName, object);
    }

    @Override
    public void sendObject(String queueName, Object object, int priority) {
        jmsTemplate.convertAndSend(queueName, object, (Message message) -> {
            message.setJMSPriority(priority);
            return message;
        });
    }

    @Override
    public void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object) {
        jmsTemplate.convertAndSend(queueName, object, (javax.jms.Message message) -> {
            additionalHeaders.forEach((k, v) -> {
                try {
                    message.setObjectProperty(k, v);
                } catch (JMSException e) {
                    LOG.warn(e.getMessage());
                }
            });
            return message;
        });
    }

    @Override
    public void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object, int priority) {
        jmsTemplate.convertAndSend(queueName, object, (javax.jms.Message message) -> {
            additionalHeaders.forEach((k, v) -> {
                try {
                    message.setObjectProperty(k, v);
                } catch (JMSException e) {
                    LOG.warn(e.getMessage());
                }
            });
            message.setJMSPriority(priority);
            return message;
        });
    }

    @Override
    public Object sendAndReceiveObject(String queueName, Serializable message) {
        jmsTemplate.setReceiveTimeout(10000);
        return jmsTemplate.sendAndReceive(queueName, (Session session) -> session.createObjectMessage(message));
    }

    @Override
    public Object receiveObject(String queueName) {
        jmsTemplate.setReceiveTimeout(RECEIVE_TIMEOUT_DEFINITE_WAIT);
        return jmsTemplate.receiveAndConvert(queueName);
    }

    @Override
    public Object receiveObjectWithTimeout(String queueName, long timeout) {
        jmsTemplate.setReceiveTimeout(timeout);
        return jmsTemplate.receiveAndConvert(queueName);
    }

    @Override
    public Object receiveSelectedObject(String queueName, String messageSelector) {
        jmsTemplate.setReceiveTimeout(RECEIVE_TIMEOUT_DEFINITE_WAIT);
        return jmsTemplate.receiveSelectedAndConvert(queueName, messageSelector);
    }

    @Override
    public Object receiveSelectedObjectWithTimeout(String queueName, String messageSelector, long timeout) {
        jmsTemplate.setReceiveTimeout(timeout);
        return jmsTemplate.receiveSelectedAndConvert(queueName, messageSelector);
    }

    @Override
    public long getQueueLength(String queueName) {
        return jmsTemplate.execute(session -> {
            String statisticsQueueName = "ActiveMQ.Statistics.Destination." + queueName;
            Queue statisticsQueue = session.createQueue(statisticsQueueName);
            Queue replyTo = session.createTemporaryQueue();
            MessageConsumer consumer = session.createConsumer(replyTo);
            MessageProducer producer = session.createProducer(statisticsQueue);
            Message msg = session.createMessage();
            msg.setJMSReplyTo(replyTo);
            producer.send(msg);
            MapMessage reply = (MapMessage) consumer.receive();
            long queueSize = reply.getLong("size");
            producer.close();
            consumer.close();
            return queueSize;
        }, true);
    }
}
