package com.cgi.eoss.ftep.logging;

import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.CloseableThreadContext;

@UtilityClass
public class Logging {
    public static final String USER_LOG_MESSAGE_FIELD = "userMessage";

    public static CloseableThreadContext.Instance userLoggingContext() {
        return CloseableThreadContext.put(USER_LOG_MESSAGE_FIELD, "1");
    }

    public static void withUserLoggingContext(Runnable runnable) {
        try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext()) {
            runnable.run();
        }
    }

    public static CloseableThreadContext.Instance imageBuildLoggingContext(String serviceName, String buildFingerprint) {
        return CloseableThreadContext
                .put("serviceName", serviceName)
                .put("buildFingerprint", buildFingerprint);
    }

    public static CloseableThreadContext.Instance jobLoggingContext(String extId, String jobId, String userId, String serviceId) {
        return CloseableThreadContext
                .put("zooId", extId)
                .put("jobId", jobId)
                .put("userId", userId)
                .put("serviceId", serviceId);
    }

    public static void withJobLoggingContext(String extId, String jobId, String userId, String serviceId, Runnable runnable) {
        try (CloseableThreadContext.Instance ctc = jobLoggingContext(extId, jobId, userId, serviceId)) {
            runnable.run();
        }
    }

    public static void withImageBuildLoggingContext(String serviceName, String buildFingerprint, Runnable runnable) {
        try (CloseableThreadContext.Instance ctc = imageBuildLoggingContext(serviceName, buildFingerprint)) {
            runnable.run();
        }
    }

}
