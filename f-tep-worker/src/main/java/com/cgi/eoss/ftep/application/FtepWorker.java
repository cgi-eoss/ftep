package com.cgi.eoss.ftep.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication(scanBasePackageClasses = FtepWorker.class)
@PropertySource(value = "file:/var/f-tep/etc/f-tep-worker.properties", ignoreResourceNotFound = true)
@PropertySource(value = "file:${user.home}/.config/f-tep/f-tep-worker.properties", ignoreResourceNotFound = true)
public class FtepWorker {
    public static void main(String[] args) {
        SpringApplication.run(FtepWorker.class, args);
    }
}
