package com.cgi.eoss.ftep.worker.worker;

import com.cgi.eoss.ftep.queues.service.FtepQueueService;
import com.cgi.eoss.ftep.rpc.LocalWorker;
import com.cgi.eoss.ftep.rpc.worker.DockerImageBuildEvent;
import com.cgi.eoss.ftep.rpc.worker.DockerImageBuildEventType;
import com.cgi.eoss.ftep.rpc.worker.DockerImageConfig;
import io.grpc.StatusRuntimeException;
import lombok.Data;
import org.apache.logging.log4j.CloseableThreadContext;

import java.util.HashMap;
import java.util.Map;

@Data
public class ImageBuilder implements Runnable {
    private Map<String, Object> messageHeaders = new HashMap<>();
    private final FtepQueueService queueService;
    private final DockerImageConfig dockerImageConfig;
    private final LocalWorker localWorker;

    @Override
    public void run() {
        messageHeaders.put("serviceName", dockerImageConfig.getServiceName());
        messageHeaders.put("buildFingerprint", dockerImageConfig.getBuildFingerprint());
        try (CloseableThreadContext.Instance ctc = getImageBuildLoggingContext(dockerImageConfig.getServiceName(), dockerImageConfig.getBuildFingerprint())) {
            try {
                dockerImageBuildUpdate(DockerImageBuildEventType.BUILD_IN_PROCESS);
                localWorker.prepareDockerImage(dockerImageConfig);
                dockerImageBuildUpdate(DockerImageBuildEventType.BUILD_COMPLETED);
            } catch (StatusRuntimeException e) {
                dockerImageBuildUpdate(DockerImageBuildEventType.BUILD_FAILED);
            }
        }
    }

    private void dockerImageBuildUpdate(DockerImageBuildEventType dockerImageBuildEventType) {
        queueService.sendObject(FtepQueueService.dockerImageBuildUpdatesQueueName, messageHeaders,
                DockerImageBuildEvent.newBuilder().setDockerImageBuildEventType(dockerImageBuildEventType).build());
    }

    private static CloseableThreadContext.Instance getImageBuildLoggingContext(String serviceName, String buildFingerprint) {
        return CloseableThreadContext.push("F-TEP Worker Queue Dispatcher")
                .put("serviceName", serviceName)
                .put("buildFingerprint", buildFingerprint);
    }
}
