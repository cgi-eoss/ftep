package com.cgi.eoss.ftep.queues.service;

import java.util.Map;

public interface FtepQueueService {

    final static String jobQueueName = "fstep-jobs";

    final static String jobUpdatesQueueName = "fstep-jobs-updates";
    
    void sendObject(String queueName, Object object);

    void sendObject(String queueName, Object object, int priority);

    void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object);
    
    void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object, int priority);

    public Object receiveObject(String queueName);
    
    public Object receiveObjectWithTimeout(String queueName, long timeout);
   
    public Object receiveSelectedObject(String queueName, String messageSelector);
    
    public Object receiveSelectedObjectWithTimeout(String queueName, String messageSelector, long timeout);

    public long getQueueLength(String queueName);

}
