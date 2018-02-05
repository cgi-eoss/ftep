package com.cgi.eoss.ftep.zoomanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(ZooManagerConfig.class)
public class ZooManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZooManagerApplication.class, args);
    }

}
