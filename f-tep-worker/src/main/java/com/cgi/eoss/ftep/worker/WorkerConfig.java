package com.cgi.eoss.ftep.worker;

import com.cgi.eoss.ftep.clouds.CloudsConfig;
import com.cgi.eoss.ftep.io.IoConfig;
import com.cgi.eoss.ftep.queues.QueuesConfig;
import com.cgi.eoss.ftep.rpc.FtepServerClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@ComponentScan(
        basePackageClasses = {WorkerConfig.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = FtepWorkerApplication.class)
)
@Import({
        CloudsConfig.class,
        IoConfig.class,
        QueuesConfig.class
})
@EnableEurekaClient
@EnableScheduling
public class WorkerConfig {

    @Bean
    public Path cacheRoot(@Value("${ftep.worker.cache.baseDir:/data/dl}") String cacheRoot) {
        return Paths.get(cacheRoot);
    }

    @Bean
    public Path jobEnvironmentRoot(@Value("${ftep.worker.jobEnv.baseDir:/data/jobs}") String jobEnvRoot) {
        return Paths.get(jobEnvRoot);
    }

    @Bean
    public String workerId(@Value("${eureka.instance.metadataMap.workerId:workerId}") String workerId) {
        return workerId;
    }

    // MIN Number of workers available
    @Bean
    public Integer minWorkerNodes(@Value("${ftep.worker.minWorkerNodes:1}") int minWorkerNodes) {
        return minWorkerNodes;
    }

    // MAX Number of workers available
    @Bean
    public Integer maxWorkerNodes(@Value("${ftep.worker.maxWorkerNodes:1}") int maxWorkerNodes) {
        return maxWorkerNodes;
    }

    // Number of concurrent jobs on each worker node
    @Bean
    public Integer maxJobsPerNode(@Value("${ftep.worker.maxJobsPerNode:2}") int maxJobsPerNode) {
        return maxJobsPerNode;
    }

    @Bean
    public Integer cacheConcurrencyLevel(@Value("${ftep.worker.cache.concurrency:4}") int concurrencyLevel) {
        return concurrencyLevel;
    }

    @Bean
    public FtepServerClient ftepServerClient(DiscoveryClient discoveryClient, @Value("${ftep.worker.server.eurekaServiceId:f-tep server}") String ftepServerServiceId) {
        return new FtepServerClient(discoveryClient, ftepServerServiceId);
    }
}
