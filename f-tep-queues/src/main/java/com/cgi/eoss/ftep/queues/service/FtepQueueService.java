package com.cgi.eoss.ftep.queues.service;

import java.io.Serializable;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public interface FtepQueueService {

    final static String jobQueueName = "ftep-jobs";
    final static String jobUpdatesQueueName = "ftep-jobs-updates";

    void sendObject(String queueName, Object object);
    void sendObject(String queueName, Object object, int priority);
    void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object);
    void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object, int priority);

    public Object sendAndReceiveObject(String queueName, Serializable message);

    public Object receiveObject(String queueName);
    public Object receiveObjectWithTimeout(String queueName, long timeout);
    public Object receiveSelectedObject(String queueName, String messageSelector);
    public Object receiveSelectedObjectWithTimeout(String queueName, String messageSelector, long timeout);

    public long getQueueLength(String queueName);
}
