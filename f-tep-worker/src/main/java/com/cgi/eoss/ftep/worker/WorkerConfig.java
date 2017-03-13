package com.cgi.eoss.ftep.worker;

import com.cgi.eoss.ftep.rpc.CredentialsServiceGrpc;
import com.cgi.eoss.ftep.rpc.ServiceContextFilesServiceGrpc;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.RemoteApiVersion;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@ComponentScan(
        basePackageClasses = WorkerConfig.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = FtepWorkerApplication.class)
)
public class WorkerConfig {

    @Bean
    public Integer cacheConcurrencyLevel(@Value("${ftep.worker.cache.concurrency:4}") int concurrencyLevel) {
        return concurrencyLevel;
    }

    @Bean
    public Integer cacheMaxWeight(@Value("${ftep.worker.cache.maxWeight:1024}") int maximumWeight) {
        return maximumWeight;
    }

    @Bean
    public Path cacheRoot(@Value("${ftep.worker.cache.baseDir:/data/cache/dl}") String cacheRoot) {
        return Paths.get(cacheRoot);
    }

    @Bean
    public Path jobEnvironmentRoot(@Value("${ftep.worker.jobEnv.baseDir:/data/cache/jobs}") String jobEnvRoot) {
        return Paths.get(jobEnvRoot);
    }

    @Bean
    public ManagedChannelBuilder channelBuilder(@Value("${ftep.worker.server.grpcHost:f-tep-server}") String host,
                                                @Value("${ftep.worker.server.grpcPort:6565}") Integer port) {
        return ManagedChannelBuilder.forAddress(host, port);
    }

    @Bean
    public CredentialsServiceGrpc.CredentialsServiceBlockingStub credentialsService(ManagedChannelBuilder channelBuilder) {
        return CredentialsServiceGrpc.newBlockingStub(channelBuilder.build());
    }

    @Bean
    public ServiceContextFilesServiceGrpc.ServiceContextFilesServiceBlockingStub serviceContextFilesService(ManagedChannelBuilder channelBuilder) {
        return ServiceContextFilesServiceGrpc.newBlockingStub(channelBuilder.build());
    }

    @Bean
    @Primary
    public DockerClient dockerClient(@Value("${ftep.worker.docker.hostUrl:unix:///var/run/docker.sock}") String dockerHostUrl) {
        DockerClientConfig dockerClientConfig = DockerClientConfig.createDefaultConfigBuilder()
                .withApiVersion(RemoteApiVersion.VERSION_1_19)
                .withDockerHost(dockerHostUrl)
                .build();
        return DockerClientBuilder.getInstance(dockerClientConfig).build();
    }

}
