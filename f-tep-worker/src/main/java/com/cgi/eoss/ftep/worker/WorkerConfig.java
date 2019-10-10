package com.cgi.eoss.ftep.worker;

import com.cgi.eoss.ftep.clouds.CloudsConfig;
import com.cgi.eoss.ftep.clouds.service.NodeFactory;
import com.cgi.eoss.ftep.io.IoConfig;
import com.cgi.eoss.ftep.queues.QueuesConfig;
import com.cgi.eoss.ftep.rpc.InProcessRpcConfig;
import com.cgi.eoss.ftep.worker.worker.FtepWorkerNodeManager;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@ComponentScan(
        basePackageClasses = {WorkerConfig.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = FtepWorkerApplication.class)
)
@Import({
        CloudsConfig.class,
        InProcessRpcConfig.class,
        IoConfig.class,
        QueuesConfig.class
})
@EnableEurekaClient
@EnableScheduling
@Log4j2
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
    public Boolean restrictedWorker(@Value("${ftep.worker.restricted:false}") boolean restrictedWorker) {
        return restrictedWorker;
    }

    @Bean
    public String workerId(@Value("${eureka.instance.metadataMap.workerId:workerId}") String workerId) {
        return workerId;
    }

    // Minimum Number of workers available
    @Bean
    public Integer minWorkerNodes(@Value("${ftep.worker.minWorkerNodes:1}") int minWorkerNodes) {
        return minWorkerNodes;
    }

    @Bean
    public Boolean keepProcDir(@Value("${ftep.worker.jobEnv.keepProcDir:false}") boolean keepProcDir) {
        return keepProcDir;
    }

    // Maximum Number of workers available
    @Bean
    public Integer maxWorkerNodes(@Value("${ftep.worker.maxWorkerNodes:1}") int maxWorkerNodes) {
        return maxWorkerNodes;
    }

    // Actual Number of concurrent jobs on each worker node
    @Bean
    public Integer maxJobsPerNode(@Value("${ftep.worker.maxJobsPerNode:2}") int maxJobsPerNode) {
        return maxJobsPerNode;
    }

    @Bean
    public Long minSecondsBetweenScalingActions(@Value("${ftep.worker.minSecondsBetweenScalingActions:600}") long minSecondsBetweenScalingActions) {
        return minSecondsBetweenScalingActions;
    }

    @Bean
    public Long minimumHourFractionUptimeSeconds(@Value("${ftep.worker.minimumHourFractionUptimeSeconds:3000}") long minimumHourFractionUptimeSeconds) {
        return minimumHourFractionUptimeSeconds;
    }

    // The cloud node manager that replaces the use of the NodeFactory
    @Bean
    public FtepWorkerNodeManager ftepWorkerNodeManager(NodeFactory nodeFactory, @Qualifier("cacheRoot") Path dataBaseDir, @Qualifier("maxJobsPerNode") Integer maxJobsPerNode) {
        return new FtepWorkerNodeManager(nodeFactory, dataBaseDir, maxJobsPerNode);
    }

    @Bean
    public TaskScheduler taskScheduler() {
        final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        return scheduler;
    }

    @Bean
    @ConditionalOnProperty("ftep.worker.dockerRegistryUrl")
    public DockerRegistryConfig dockerRegistryConfig(
            @Value("${ftep.worker.dockerRegistryUrl}") String dockerRegistryUrl,
            @Value("${ftep.worker.dockerRegistryUsername}") String dockerRegistryUsername,
            @Value("${ftep.worker.dockerRegistryPassword}") String dockerRegistryPassword) {
        return new DockerRegistryConfig(dockerRegistryUrl, dockerRegistryUsername, dockerRegistryPassword);
    }
}
