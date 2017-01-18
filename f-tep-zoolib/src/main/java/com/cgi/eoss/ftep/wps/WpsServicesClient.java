package com.cgi.eoss.ftep.wps;

import com.cgi.eoss.ftep.rpc.FtepServiceLauncherGrpc;
import com.cgi.eoss.ftep.rpc.FtepServiceParams;
import com.cgi.eoss.ftep.rpc.FtepServiceResponse;
import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.google.common.collect.Multimap;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * <p>Client for F-TEP gRPC services. Encapsulates the usage of the RPC interface so that WPS service implementations
 * may access the F-TEP orchestration environment more easily.</p>
 */
@Slf4j
public class WpsServicesClient {

    private final ManagedChannel channel;
    private final FtepServiceLauncherGrpc.FtepServiceLauncherBlockingStub ftepServiceLauncherBlockingStub;
    private final FtepServiceLauncherGrpc.FtepServiceLauncherStub ftepServiceLauncherStub;

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
        ftepServiceLauncherBlockingStub = FtepServiceLauncherGrpc.newBlockingStub(channel);
        ftepServiceLauncherStub = FtepServiceLauncherGrpc.newStub(channel);
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
     * <p>Launch a WPS Processor service with a blocking call.</p>
     *
     * @param jobId The job ID as set by the WPS server.
     * @param userId The ID of the user launching the service.
     * @param serviceId The ID of the service being launched. Used to determine the application container to use.
     * @param inputs The WPS parameter inputs. Expected to be a keyed list of strings.
     */
    public Multimap<String, String> launchService(String jobId, String userId, String serviceId, Multimap<String, String> inputs) {
        FtepServiceParams request = FtepServiceParams.newBuilder()
                .setJobId(jobId)
                .setUserId(userId)
                .setServiceId(serviceId)
                .addAllInputs(GrpcUtil.mapToParams(inputs))
                .build();
        FtepServiceResponse ftepServiceResponse = ftepServiceLauncherBlockingStub.launchService(request);
        return GrpcUtil.paramsListToMap(ftepServiceResponse.getOutputsList());
    }

}
