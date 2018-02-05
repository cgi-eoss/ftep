package com.cgi.eoss.ftep.zoomanager;

import com.google.common.jimfs.Jimfs;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 */
@Configuration
public class ZooManagerTestConfig {

    @Bean
    public FileSystem fileSystem() {
        return Jimfs.newFileSystem(com.google.common.jimfs.Configuration.unix());
    }

    @Bean
    public Path zcfgBasePath(@Value("${ftep.zoomanager.zcfg.path:/var/www/cgi-bin}") String zcfgBasePath) throws IOException {
        Path path = fileSystem().getPath(zcfgBasePath);
        Files.createDirectories(path);
        return path;
    }

    @Bean
    public Path zooServicesStubJar(@Value("${ftep.zoomanager.stub.jarFile:/var/www/cgi-bin/jars/f-tep-services.jar}") String zooServicesStubJar) throws IOException {
        Path path = fileSystem().getPath(zooServicesStubJar);
        Files.createDirectories(path.getParent());
        return path;
    }

    @Bean
    public String javacClasspath() {
        return System.getProperty("java.class.path");
    }

    @Bean
    public InProcessServerBuilder serverBuilder() {
        return InProcessServerBuilder.forName(getClass().getName()).directExecutor();
    }

    @Bean
    public ManagedChannelBuilder channelBuilder() {
        return InProcessChannelBuilder.forName(getClass().getName()).directExecutor();
    }

}
