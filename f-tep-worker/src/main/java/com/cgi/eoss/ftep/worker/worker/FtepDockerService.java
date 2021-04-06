package com.cgi.eoss.ftep.worker.worker;

import com.cgi.eoss.ftep.logging.Logging;
import com.cgi.eoss.ftep.rpc.worker.JobSpec;
import com.cgi.eoss.ftep.worker.DockerRegistryConfig;
import com.cgi.eoss.ftep.worker.docker.DockerClientFactory;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PushImageCmd;
import com.github.dockerjava.api.exception.BadRequestException;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.CloseableThreadContext;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
public class FtepDockerService {

    public DockerClient getDockerClient(String dockerEngineUrl, DockerRegistryConfig dockerRegistryConfig) {
        if (dockerRegistryConfig != null) {
            return DockerClientFactory.buildDockerClient(dockerEngineUrl, dockerRegistryConfig);
        } else {
            return DockerClientFactory.buildDockerClient(dockerEngineUrl);
        }
    }

    public Optional<Instant> getContainerStarttime(Container container, DockerClient dockerClient) {
        String startTimeStr = dockerClient.inspectContainerCmd(container.getId()).exec().getState().getStartedAt();
        try {
            if (!Strings.isNullOrEmpty(startTimeStr)) {
                return Optional.of(Instant.parse(startTimeStr));
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void pushDockerImage(DockerClient dockerClient, String dockerImage, DockerRegistryConfig dockerRegistryConfig) throws IOException, InterruptedException {
        LOG.info("Pushing Docker image '{}' to registry {}", dockerImage, dockerRegistryConfig.getDockerRegistryUrl());
        PushImageCmd pushImageCmd = dockerClient.pushImageCmd(dockerImage);
        AuthConfig authConfig = new AuthConfig()
                .withRegistryAddress(dockerRegistryConfig.getDockerRegistryUrl())
                .withUsername(dockerRegistryConfig.getDockerRegistryUsername())
                .withPassword(dockerRegistryConfig.getDockerRegistryPassword());
        dockerClient.authCmd().withAuthConfig(authConfig).exec();
        pushImageCmd = pushImageCmd.withAuthConfig(authConfig);
        pushImageCmd.exec(new PushImageResultCallback()).awaitSuccess();
        LOG.info("Pushed Docker image '{}' to registry {}", dockerImage, dockerRegistryConfig.getDockerRegistryUrl());
    }

    public void pullDockerImage(DockerClient dockerClient, String dockerImage, DockerRegistryConfig dockerRegistryConfig) throws IOException, InterruptedException {
        LOG.info("Pulling Docker image '{}' from registry {}", dockerImage, dockerRegistryConfig.getDockerRegistryUrl());
        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(dockerImage);
        AuthConfig authConfig = new AuthConfig()
                .withRegistryAddress(dockerRegistryConfig.getDockerRegistryUrl())
                .withUsername(dockerRegistryConfig.getDockerRegistryUsername())
                .withPassword(dockerRegistryConfig.getDockerRegistryPassword());
        dockerClient.authCmd().withAuthConfig(authConfig).exec();
        pullImageCmd = pullImageCmd.withRegistry(dockerRegistryConfig.getDockerRegistryUrl()).withAuthConfig(authConfig);
        pullImageCmd.exec(new PullImageResultCallback()).awaitSuccess();
        LOG.info("Pulled Docker image '{}' from registry {}", dockerImage, dockerRegistryConfig.getDockerRegistryUrl());
    }

    public boolean isImageAvailableLocally(DockerClient dockerClient, String dockerImage) {
        List<Image> images = dockerClient.listImagesCmd().withImageNameFilter(dockerImage).exec();
        return !images.isEmpty();
    }

    public void removeContainer(DockerClient client, String containerId) {
        try {
            LOG.info("Removing container {}", containerId);
            client.removeContainerCmd(containerId).exec();
        } catch (BadRequestException e) {
            if (!e.getMessage().endsWith("is already in progress")) {
                LOG.error("Failed to delete container {}", containerId, e);
            }
        } catch (Exception e) {
            LOG.error("Failed to delete container {}", containerId, e);
        }
    }

    public CreateContainerResponse createContainer(DockerClient dockerClient, JobSpec jobSpec, String imageTag, List<String> binds) {
        try (CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(imageTag)) {
            Map<String, String> labels = new HashMap<>();
            labels.put(FtepWorker.JOB_ID, jobSpec.getJob().getId());
            labels.put(FtepWorker.INT_JOB_ID, String.valueOf(jobSpec.getJob().getIntJobId()));
            labels.put(FtepWorker.USER_ID, jobSpec.getJob().getUserId());
            labels.put(FtepWorker.SERVICE_ID, jobSpec.getJob().getServiceId());
            labels.put(FtepWorker.WORKER_ID, jobSpec.getWorker().getId());
            if (jobSpec.getHasTimeout()) {
                labels.put(FtepWorker.TIMEOUT_VALUE, String.valueOf(jobSpec.getTimeoutValue()));
            }
            createContainerCmd.withLabels(labels);
            createContainerCmd.withBinds(binds.stream().map(Bind::parse).collect(Collectors.toList()));
            createContainerCmd.withExposedPorts(jobSpec.getExposedPortsList().stream().map(ExposedPort::parse).collect(Collectors.toList()));
            createContainerCmd.withPortBindings(jobSpec.getExposedPortsList().stream()
                    .map(p -> new com.github.dockerjava.api.model.PortBinding(new Ports.Binding(null, null), ExposedPort.parse(p)))
                    .collect(Collectors.toList()));

            // Add proxy vars to the container, if they are set in the environment
            createContainerCmd.withEnv(
                    ImmutableSet.of("http_proxy", "https_proxy", "no_proxy").stream()
                            .filter(var -> System.getenv().containsKey(var))
                            .map(var -> var + "=" + System.getenv(var))
                            .collect(Collectors.toList()));

            return createContainerCmd.exec();
        }
    }

    public String buildImage(DockerClient dockerClient, String serviceName, String dockerImageTag, String fingerprint, Path serviceContext) {
        LOG.info("Starting Docker image build '{}' for service {}", dockerImageTag, serviceName);
        BuildImageCmd buildImageCmd = dockerClient.buildImageCmd()
                .withRemove(true)
                .withBaseDirectory(serviceContext.toFile())
                .withDockerfile(serviceContext.resolve("Dockerfile").toFile())
                .withTags(ImmutableSet.of(dockerImageTag));

        // Add proxy vars to the container, if they are set in the environment
        ImmutableSet.of("http_proxy", "https_proxy", "no_proxy").stream()
                .filter(var -> System.getenv().containsKey(var))
                .forEach(var -> buildImageCmd.withBuildArg(var, System.getenv(var)));

        return buildImageCmd.exec(new BuildImageResultCallback() {
            @Override
            public void onNext(BuildResponseItem item) {
                if (!Strings.isNullOrEmpty(item.getStream())) {
                    if (fingerprint != null) {
                        try (CloseableThreadContext.Instance userCtc = Logging.userLoggingContext().put("dockerBuildFingerprint", fingerprint)) {
                            LOG.info("{}", item.getStream().trim());
                        }
                    } else {
                        LOG.debug("{}:{} :: {}", serviceName, dockerImageTag, item.getStream().trim());
                    }
                }
                super.onNext(item);
            }
        }).awaitImageId();
    }
}
