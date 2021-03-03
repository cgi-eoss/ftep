package com.cgi.eoss.ftep.queues.service;

import java.util.Map;

public interface FtepQueueService {

    String jobQueueName = "ftep-jobs";

    String jobUpdatesQueueName = "ftep-jobs-updates";

    String dockerImageBuildsQueueName = "docker-image-builds";

    String dockerImageBuildUpdatesQueueName = "docker-image-build-updates";

    void sendObject(String queueName, Object object);

    void sendObject(String queueName, Object object, int priority);

    void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object);

    void sendObject(String queueName, Map<String, Object> additionalHeaders, Object object, int priority);

    Object receiveObject(String queueName);

    Object receiveObject(String queueName, String messageSelector);

    Object receiveObjectWithTimeout(String queueName, long timeout);

    Object receiveObjectWithTimeout(String queueName, String messageSelector, long timeout);

    long getQueueLength(String queueName);

    long getQueueLength(String queueName, String messageSelector);
}
