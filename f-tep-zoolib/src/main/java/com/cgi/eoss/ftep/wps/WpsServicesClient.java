package com.cgi.eoss.ftep.wps;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WpsServicesClient {

    private final ManagedChannel channel;
    private final ApplicationLauncherGrpc.ApplicationLauncherBlockingStub blockingStub;
    private final ApplicationLauncherGrpc.ApplicationLauncherStub asyncStub;

    /**
     * <p>Construct gRPC client connecting to server at ${host}:${port}.</p>
     */
    public WpsServicesClient(String host, int port) {
        // TLS is unused since this should only be active in the F-TEP infrastructure, i.e. not public
        // TODO Investigate feasibility of certificate security
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true));
    }

    /**
     * <p>Construct gRPC client using an existing channel builder.</p>
     */
    public WpsServicesClient(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        blockingStub = ApplicationLauncherGrpc.newBlockingStub(channel);
        asyncStub = ApplicationLauncherGrpc.newStub(channel);
    }

    /**
     * <p>Tear down gRPC channel safely.</p>
     *
     * @throws InterruptedException
     */
    @PreDestroy
    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * <p>Launch a WPS Application with a blocking call, and stream the response messages (i.e. lines from the
     * application stdout/stderr) as they arrive.</p>
     * @param jobId
     * @param appName The application name. This is used to determine the docker container to launch.
     * @param userId
     * @param serviceId
     * @param input The WPS parameter input. Expected to be the URL to an input file.
     * @param output The WPS parameter output. Expected to be the URL to an output file.
     * @param timeout
     */
    public String launchApplication(String jobId, String appName, String userId, String serviceId, String input, String output, String timeout) {
        ApplicationParams request = ApplicationParams.newBuilder()
                .setJobId(jobId)
                .setAppName(appName)
                .setUserId(userId)
                .setServiceId(serviceId)
                .setInput(input)
                .setOutput(output)
                .setTimeout(Integer.parseInt(timeout))
                .build();
        ApplicationResponse applicationResponse = blockingStub.launchApplication(request);
        return applicationResponse.getOutputUrl();
    }

}
