package com.cgi.eoss.ftep.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@Import(WorkerConfig.class)
@PropertySource(value = "file:/var/f-tep/etc/f-tep-worker.properties", ignoreResourceNotFound = true)
@PropertySource(value = "file:${user.home}/.config/f-tep/f-tep-worker.properties", ignoreResourceNotFound = true)
public class FtepWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FtepWorkerApplication.class, args);
    }

}
