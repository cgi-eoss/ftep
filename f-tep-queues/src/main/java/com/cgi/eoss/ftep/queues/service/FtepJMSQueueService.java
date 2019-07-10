package com.cgi.eoss.ftep.queues.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.jms.core.BrowserCallback;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.JMSException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Log4j2
public class FtepJMSQueueService implements FtepQueueService {

    private JmsTemplate jmsTemplate;
    private static final BrowserCallback<Long> QUEUE_COUNT_CALLBACK = (session, browser) -> (long) Collections.list(browser.getEnumeration()).size();

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
    public Object receiveObject(String queueName, String messageSelector) {
        jmsTemplate.setReceiveTimeout(JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT);
        return jmsTemplate.receiveSelectedAndConvert(queueName, messageSelector);
    }

    @Override
    public Object receiveObjectWithTimeout(String queueName, String messageSelector, long timeout) {
        jmsTemplate.setReceiveTimeout(timeout);
        return jmsTemplate.receiveSelectedAndConvert(queueName, messageSelector);
    }

    @Override
    public long getQueueLength(String queueName) {
        return Optional.ofNullable(jmsTemplate.browse(queueName, QUEUE_COUNT_CALLBACK))
                .orElse(0L);
    }

    @Override
    public long getQueueLength(String queueName, String messageSelector) {
        return Optional.ofNullable(jmsTemplate.browseSelected(queueName, messageSelector, QUEUE_COUNT_CALLBACK))
                .orElse(0L);
    }
}
