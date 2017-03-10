package com.cgi.eoss.ftep.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(WorkerConfig.class)
public class FtepWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FtepWorkerApplication.class, args);
    }

}
