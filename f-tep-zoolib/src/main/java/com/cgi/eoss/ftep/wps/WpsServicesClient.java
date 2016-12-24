package com.cgi.eoss.ftep.wps;

import com.cgi.eoss.ftep.rpc.ApplicationLauncherGrpc;
import com.cgi.eoss.ftep.rpc.ApplicationParams;
import com.cgi.eoss.ftep.rpc.ApplicationResponse;
import com.cgi.eoss.ftep.rpc.Param;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WpsServicesClient {

    private final ManagedChannel channel;
    private final ApplicationLauncherGrpc.ApplicationLauncherBlockingStub applicationLauncherBlockingStub;
    private final ApplicationLauncherGrpc.ApplicationLauncherStub applicationLauncherStub;

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
        applicationLauncherBlockingStub = ApplicationLauncherGrpc.newBlockingStub(channel);
        applicationLauncherStub = ApplicationLauncherGrpc.newStub(channel);
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
     *
     * @param jobId The job ID as set by the WPS server.
     * @param appName The application name. This is used to determine the docker container to launch.
     * @param userId The ID of the user launching the service.
     * @param serviceId The ID of the service being launched. Used to determine the application container to use.
     * @param inputs The WPS parameter input. Expected to be a list of URLs to the input files.
     * @param timeout The maximum running time of the application in hours. The application will be terminated after
     * this.
     */
    public String launchApplication(String jobId, String appName, String userId, String serviceId, List<String> inputs, int timeout) {
        ApplicationParams request = ApplicationParams.newBuilder()
                .setJobId(jobId)
                .setAppName(appName)
                .setUserId(userId)
                .setServiceId(serviceId)
                .addInputs(Param.newBuilder().setParamName("inputs").addAllParamValue(inputs).build())
                .setTimeout(timeout)
                .build();
        ApplicationResponse applicationResponse = applicationLauncherBlockingStub.launchApplication(request);
        return applicationResponse.getOutputUrl();
    }

}
