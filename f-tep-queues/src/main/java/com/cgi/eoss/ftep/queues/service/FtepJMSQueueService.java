package com.cgi.eoss.ftep.queues.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Log4j2
public class FtepJMSQueueService implements FtepQueueService {

    private JmsTemplate jmsTemplate;

    public FtepJMSQueueService(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    public void sendObject(String queueName, Object object) {
        jmsTemplate.convertAndSend(queueName, object);
    }

    @Override
    public void sendObject(String queueName, Object object, int priority) {
        jmsTemplate.convertAndSend(queueName, object, message -> {
            message.setJMSPriority(priority);
            return message;
        });
    }

    @Override
    public void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object) {
        jmsTemplate.convertAndSend(queueName, object, message -> {
            additionalHeaders.forEach((k, v) -> {
                try {
                    message.setObjectProperty(k, v);
                } catch (JMSException e) {
                    LOG.error("Error sending message to JMS Queue " + queueName, e);
                }
            });
            return message;
        });
    }

    @Override
    public void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object, int priority) {
        jmsTemplate.convertAndSend(queueName, object, message -> {
            additionalHeaders.forEach((k, v) -> {
                try {
                    message.setObjectProperty(k, v);
                } catch (JMSException e) {
                    LOG.error("Error sending message to JMS Queue " + queueName, e);
                }
            });
            message.setJMSPriority(priority);
            return message;
        });
    }

    @Override
    public Object receiveObject(String queueName) {
        jmsTemplate.setReceiveTimeout(JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
        return jmsTemplate.receiveAndConvert(queueName);
    }

    @Override
    public Object receiveObjectWithTimeout(String queueName, long timeout) {
        jmsTemplate.setReceiveTimeout(timeout);
        return jmsTemplate.receiveAndConvert(queueName);
    }

    @Override
    public Object receiveSelectedObject(String queueName, String messageSelector) {
        jmsTemplate.setReceiveTimeout(JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
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
            QueueBrowser queueBrowser = session.createBrowser(session.createQueue(queueName));
            return Long.valueOf(Collections.list(queueBrowser.getEnumeration()).size());
        }, true);
    }
}
