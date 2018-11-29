package com.cgi.eoss.ftep.queues.service;

import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Map;

@Component
public interface FtepQueueService {

    public final static String jobQueueName = "ftep-jobs";
    public final static String jobUpdatesQueueName = "ftep-jobs-updates";

    public void sendObject(String queueName, Object object);
    public void sendObject(String queueName, Object object, int priority);
    public void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object);
    public void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object, int priority);

    public Object sendAndReceiveObject(String queueName, Serializable message);

    public Object receiveObject(String queueName);
    public Object receiveObjectWithTimeout(String queueName, long timeout);
    public Object receiveSelectedObject(String queueName, String messageSelector);
    public Object receiveSelectedObjectWithTimeout(String queueName, String messageSelector, long timeout);

    public long getQueueLength(String queueName);
}
