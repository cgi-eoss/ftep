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

}
