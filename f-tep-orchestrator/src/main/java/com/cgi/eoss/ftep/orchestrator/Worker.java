package com.cgi.eoss.ftep.orchestrator;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
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
@Builder
@Slf4j
public class Worker {

    private final DockerClient dockerClient;
    private final JobEnvironmentService jobEnvironmentService;
    private final ServiceInputOutputManager inputOutputManager;

    /**
     * <p>Ensure the map of input files is prepared in the given location. This may involve downloading products from
     * external hosts, or copying/symlinking existing files.</p>
     *
     * @param inputs Collection of inputs to be prepared. The multi-map key specifies the subdir name within
     * <code>inputDir</code> while the multi-map values specify the URLs to be prepared in the subdir.
     * @param inputDir
     */
    public void prepareInputs(Multimap<String, String> inputs, Path inputDir) {
        inputs.keys().forEach(subdir -> {
            Path subdirPath = inputDir.resolve(subdir);
            inputOutputManager.prepareInput(subdirPath, inputs.get(subdir));
        });
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
     * <p>Create a new job environment with job parameters file.</p>
     *
     * @param jobId The jobId for the environment.
     * @param jobConfig The job configuration parameters.
     * @return The created environment.
     * @throws IOException If any problem occurred in creating the job workspace or config file.
     */
    public JobEnvironment createJobEnvironment(String jobId, Multimap<String, String> jobConfig) throws IOException {
        return jobEnvironmentService.createEnvironment(jobId, jobConfig);
    }
}
