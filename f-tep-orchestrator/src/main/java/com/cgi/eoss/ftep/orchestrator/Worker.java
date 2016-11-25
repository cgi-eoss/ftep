package com.cgi.eoss.ftep.orchestrator;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.RemoteApiVersion;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.Builder;
import lombok.Data;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>Representation of an F-TEP worker node.</p>
 * <p>This exposes access to Docker containers via conventional string parameters, thus encapsulating actual usage of
 * the docker-java API.</p>
 */
@Data
public class Worker {

    private static final String DEFAULT_DOCKER_HOST = "localhost";
    private static final String DEFAULT_DOCKER_PORT = "2376";
    private static final RemoteApiVersion DEFAULT_DOCKER_API_VERSION = RemoteApiVersion.VERSION_1_22;
    private static final String DEFAULT_DOCKER_CERT_PATH = System.getProperty("user.home") + File.separator + ".docker";

    private String dockerHost;
    private String dockerPort;
    private String dockerCertPath;
    private String dockerApiVersion;

    private DockerClient dockerClient;

    @Builder
    private Worker(String dockerHost, String dockerPort, String dockerCertPath, String dockerApiVersion) {
        this.dockerHost = dockerHost;
        this.dockerPort = dockerPort;
        this.dockerCertPath = dockerCertPath;
        this.dockerApiVersion = dockerApiVersion;

        this.dockerClient = buildDockerClient();
    }

    /**
     * <p>Launch a new docker container with the given configuration.</p>
     *
     * @param config String-based configuration of the Docker container instance. The parameters follow the docker-java
     * type parsing, which itself matches closely with the docker CLI.
     * @return The ID of the launched container.
     */
    public String launchDockerContainer(DockerLaunchConfig config) {
        CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(config.getImage());
        createContainerCmd.withBinds(config.getBinds().stream().map(Bind::parse).collect(Collectors.toList()));
        // containerCmd.withVolumes(config.getVolumes().stream().map(Volume::new).collect(Collectors.toList()));
        createContainerCmd.withExposedPorts(config.getExposedPorts().stream().map(ExposedPort::parse).collect(Collectors.toList()));

        String containerId = createContainerCmd.exec().getId();
        dockerClient.startContainerCmd(containerId).exec();

        if (config.isDefaultLogging()) {
            // Enable container logging via slf4J by default
            dockerClient.logContainerCmd(containerId).withStdErr(true).withStdOut(true).withFollowStream(true).withTailAll()
                    .exec(new LogContainerResultCallback());
        }

        return containerId;
    }

    /**
     * <p>Wait for the termination of the container with the given identifier, with a maximum timeout (in hours).</p>
     * <p>Blocks until the container exits.</p>
     *
     * @param containerId The ID of the running container.
     * @param timeoutHours The maximum time to wait before terminating the container.
     * @return The exit code of the container.
     */
    public int waitForContainerExit(String containerId, int timeoutHours) {
        return waitForContainer(containerId).awaitStatusCode(timeoutHours, TimeUnit.HOURS);
    }

    /**
     * <p>Wait for the termination of the container with the given identifier.</p>
     * <p>Blocks until the container exits.</p>
     *
     * @param containerId The ID of the running container.
     * @return The exit code of the container.
     */
    public int waitForContainerExit(String containerId) {
        return waitForContainer(containerId).awaitStatusCode();
    }

    private WaitContainerResultCallback waitForContainer(String containerId) {
        return dockerClient.waitContainerCmd(containerId).exec(new WaitContainerResultCallback());
    }

    /**
     * <p>Retrieve the active port bindings from the container with the given identifier.</p>
     *
     * @param containerId The ID of the running container.
     * @return The exposed port bindings in the multi-mapped format "portDef" : "host:ip", where port definitions match
     * those passed in DockerLaunchConfig (e.g. "8080/tcp") and the bound endpoints are simply "hostname:port".
     */
    public Multimap<String, String> getContainerPortBindings(String containerId) {
        InspectContainerResponse inspectContainer = dockerClient.inspectContainerCmd(containerId).exec();
        Map<ExposedPort, Ports.Binding[]> bindings = inspectContainer.getNetworkSettings().getPorts().getBindings();

        // Twist the Docker-Java API map of arrays into a string-string multimap
        HashMultimap<String, String> portBindings = HashMultimap.create();
        bindings.entrySet().forEach(e -> Arrays.stream(e.getValue()).forEach(
                b -> portBindings.put(e.getKey().toString(), b.toString())
        ));

        return portBindings;
    }

    /**
     * @return A configured {@link DockerClient} appropriate for this Worker, including remote docker-engine access and
     * authentication if necessary.
     */
    private DockerClient buildDockerClient() {
        DockerClientConfig.DockerClientConfigBuilder configBuilder = DockerClientConfig.createDefaultConfigBuilder()
                .withApiVersion(dockerApiVersion);

        if (!dockerHost.equals(DEFAULT_DOCKER_HOST)) {
            configBuilder.withDockerHost("tcp://" + dockerHost + ":" + dockerPort)
                    .withDockerTlsVerify(true)
                    .withDockerCertPath(dockerCertPath);
        } else {
            // Use the default unix socket rather than TCP to localhost
            configBuilder.withDockerHost("unix:///var/run/docker.sock");
        }

        return DockerClientBuilder.getInstance(configBuilder.build()).build();
    }

    // Prepare default parameters for the builder.
    public static class WorkerBuilder {
        private String dockerHost = DEFAULT_DOCKER_HOST;
        private String dockerPort = DEFAULT_DOCKER_PORT;
        private String dockerCertPath = DEFAULT_DOCKER_CERT_PATH;
        private String dockerApiVersion = DEFAULT_DOCKER_API_VERSION.getVersion();
    }

}
