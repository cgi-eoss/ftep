package com.cgi.eoss.ftep.orchestrator.worker;

import com.cgi.eoss.ftep.orchestrator.io.ServiceInputOutputManager;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.RemoteApiVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * <p>Provides control and access of worker nodes, i.e. machines where F-TEP services may be executed.</p>
 * <p>Injects required local and remote services to the workers.</p>
 */
@Service("manualWorkerService")
@Slf4j
public class ManualWorkerService implements WorkerService {

    private static final String DEFAULT_DOCKER_HOST = "localhost";
    private static final String DEFAULT_DOCKER_PORT = "2376";
    private static final RemoteApiVersion DEFAULT_DOCKER_API_VERSION = RemoteApiVersion.VERSION_1_19;
    private static final String DEFAULT_DOCKER_CERT_PATH = System.getProperty("user.home") + File.separator + ".docker";
    private static final String DOCKER_UNIX_SOCKET = "unix:///var/run/docker.sock";

    private final JobEnvironmentService jobEnvironmentService;
    private final ServiceInputOutputManager inputOutputManager;

    public ManualWorkerService() {
        // TODO Remove this placeholder constructor for legacy WPS services
        this(null, null);
    }

    @Autowired
    public ManualWorkerService(JobEnvironmentService jobEnvironmentService, ServiceInputOutputManager inputOutputManager) {
        this.jobEnvironmentService = jobEnvironmentService;
        this.inputOutputManager = inputOutputManager;
    }

    @Override
    public Worker getWorker() {
        return getWorkerBuilder()
                .dockerClient(buildDockerClient(DEFAULT_DOCKER_HOST, DEFAULT_DOCKER_PORT, DEFAULT_DOCKER_CERT_PATH, DEFAULT_DOCKER_API_VERSION))
                .build();
    }

    @Override
    public Worker getWorker(String host) {
        return getWorkerBuilder()
                .dockerClient(buildDockerClient(host, DEFAULT_DOCKER_PORT, DEFAULT_DOCKER_CERT_PATH, DEFAULT_DOCKER_API_VERSION))
                .build();
    }

    private Worker.WorkerBuilder getWorkerBuilder() {
        return Worker.builder().jobEnvironmentService(jobEnvironmentService).inputOutputManager(inputOutputManager);
    }

    /**
     * @param dockerHost
     * @param dockerPort
     * @param dockerCertPath
     * @param dockerApiVersion
     * @return A configured {@link DockerClient} for a worker, including remote docker-engine access and authentication
     * if necessary.
     */
    private DockerClient buildDockerClient(String dockerHost, String dockerPort, String dockerCertPath, RemoteApiVersion dockerApiVersion) {
        DockerClientConfig.DockerClientConfigBuilder configBuilder = DockerClientConfig.createDefaultConfigBuilder()
                .withApiVersion(dockerApiVersion);

        String hostUrl;
        if (!dockerHost.equals(DEFAULT_DOCKER_HOST)) {
            // Use TLS-secured remote docker host
            hostUrl = "tcp://" + dockerHost + ":" + dockerPort;
            configBuilder.withDockerTlsVerify(true).withDockerCertPath(dockerCertPath);
        } else {
            // Use the default unix socket rather than TCP to localhost
            hostUrl = DOCKER_UNIX_SOCKET;
            configBuilder.withDockerHost(hostUrl);
        }
        configBuilder.withDockerHost(hostUrl);
        LOG.info("Worker connecting to docker: {}", hostUrl);

        return DockerClientBuilder.getInstance(configBuilder.build()).build();
    }

}
