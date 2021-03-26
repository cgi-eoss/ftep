package com.cgi.eoss.ftep.worker;

import com.cgi.eoss.ftep.worker.worker.FtepWorkerDispatcher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(WorkerConfig.class)
public class FtepWorkerApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(FtepWorkerApplication.class, args);
        FtepWorkerDispatcher ftepWorkerDispatcher = (FtepWorkerDispatcher) context.getBean("ftepWorkerDispatcher");
        ftepWorkerDispatcher.recoverJobs();
    }

}
