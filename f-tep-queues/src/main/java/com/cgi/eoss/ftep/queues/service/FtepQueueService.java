package com.cgi.eoss.ftep.queues.service;

import java.util.Map;

public interface FtepQueueService {

    String jobQueueName = "ftep-jobs";

    String jobUpdatesQueueName = "ftep-jobs-updates";
    
    void sendObject(String queueName, Object object);

    void sendObject(String queueName, Object object, int priority);

    void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object);
    
    void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object, int priority);

    Object receiveObject(String queueName);
    
    Object receiveObjectWithTimeout(String queueName, long timeout);
   
    Object receiveSelectedObject(String queueName, String messageSelector);
    
    Object receiveSelectedObjectWithTimeout(String queueName, String messageSelector, long timeout);

    long getQueueLength(String queueName);

}
