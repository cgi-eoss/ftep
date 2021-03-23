package com.cgi.eoss.ftep.wps;

import com.cgi.eoss.ftep.rpc.FtepJobLauncherGrpc;
import com.cgi.eoss.ftep.rpc.FtepServiceParams;
import com.cgi.eoss.ftep.rpc.GetJobResultRequest;
import com.cgi.eoss.ftep.rpc.GetJobResultResponse;
import com.cgi.eoss.ftep.rpc.GetJobStatusRequest;
import com.cgi.eoss.ftep.rpc.GetJobStatusResponse;
import com.cgi.eoss.ftep.rpc.Job;
import com.cgi.eoss.ftep.rpc.JobDataServiceGrpc;
import com.cgi.eoss.ftep.rpc.JobParam;
import com.cgi.eoss.ftep.rpc.SubmitJobResponse;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.SocketUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * <p>Integration test for launching WPS services.</p>
 * <p><strong>This uses a real Docker engine to build and run a container!</strong></p>
 */
public class FtepServicesClientTest {
    private static final String RPC_SERVER_NAME = FtepServicesClientTest.class.getName();
    private static final String SERVICE_NAME = "service1";

    @Rule
    public GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Mock
    FtepJobLauncherGrpc.FtepJobLauncherImplBase ftepJobLauncher;

    @Mock
    JobDataServiceGrpc.JobDataServiceImplBase jobDataService;

    private int grpcPort;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        grpcPort = SocketUtils.findAvailableTcpPort();
        grpcCleanup.register(NettyServerBuilder.forPort(grpcPort).directExecutor()
                .addService(ftepJobLauncher)
                .addService(jobDataService)
                .build().start());
    }

    @Test
    public void launchApplication() {
        String jobId = UUID.randomUUID().toString();
        String userId = "userId";

        doAnswer(invocation -> {
            FtepServiceParams params = invocation.getArgument(0);
            StreamObserver<SubmitJobResponse> responseObserver = invocation.getArgument(1);

            assertThat(params.getUserId(), is(userId));
            assertThat(params.getServiceId(), is(SERVICE_NAME));
            assertThat(params.getJobId(), is(jobId));
            assertThat(params.getInputsList().get(0), is(JobParam.newBuilder().setParamName("inputKey1").addParamValue("inputVal1").build()));
            assertThat(params.getInputsList().get(1), is(JobParam.newBuilder().setParamName("inputKey2").addParamValue("inputVal2-1").addParamValue("inputVal2-2").build()));

            responseObserver.onNext(SubmitJobResponse.newBuilder().setJob(Job.newBuilder()
                    .setId(jobId)
                    .build()).build());
            responseObserver.onCompleted();
            return null;
        }).when(ftepJobLauncher).submitJob(any(), any());

        AtomicInteger statusRequestCount = new AtomicInteger(0);
        doAnswer(invocation -> {
            GetJobStatusRequest params = invocation.getArgument(0);
            StreamObserver<GetJobStatusResponse> responseObserver = invocation.getArgument(1);

            assertThat(params.getJobId(), is(jobId));

            Job.Status status;
            if (statusRequestCount.getAndIncrement() == 0) {
                status = Job.Status.ACCEPTED;
            } else if (statusRequestCount.getAndIncrement() <= 5) {
                status = Job.Status.RUNNING;
            } else {
                status = Job.Status.COMPLETED;
            }

            responseObserver.onNext(GetJobStatusResponse.newBuilder().setJobStatus(status).build());
            responseObserver.onCompleted();
            return null;
        }).when(jobDataService).getJobStatus(any(), any());

        doAnswer(invocation -> {
            GetJobResultRequest params = invocation.getArgument(0);
            StreamObserver<GetJobResultResponse> responseObserver = invocation.getArgument(1);

            assertThat(params.getJobId(), is(jobId));

            responseObserver.onNext(GetJobResultResponse.newBuilder()
                    .setJobStatus(Job.Status.COMPLETED)
                    .addOutputs(JobParam.newBuilder()
                            .setParamName("output")
                            .addParamValue("ftep://output/output_file_1")
                            .addParamValue("ftep://output/A_plot.zip")
                            .addParamValue("ftep://output/A_plot.qpj")
                            .addParamValue("ftep://output/A_point.qpj")
                            .build())
                    .build());
            responseObserver.onCompleted();
            return null;
        }).when(jobDataService).getJobResult(any(), any());

        Map<String, Object> conf = ImmutableMap.<String, Object>builder()
                .put("ftep", new HashMap<>(ImmutableMap.<String, Object>builder()
                        .put("ftepServerGrpcHost", "localhost")
                        .put("ftepServerGrpcPort", String.valueOf(grpcPort))
                        .build()))
                .put("lenv", new HashMap<>(ImmutableMap.<String, Object>builder()
                        .put("uusid", jobId)
                        .build()))
                .put("renv", new HashMap<>(ImmutableMap.<String, Object>builder()
                        .put("HTTP_REMOTE_USER", userId)
                        .build()))
                .build();

        HashMap<String, Object> inputs = new HashMap<>();
        inputs.put("inputKey1", new HashMap<>(ImmutableMap.of("value", "inputVal1")));
        inputs.put("inputKey2", new HashMap<>(ImmutableMap.of("isArray", "true", "value", new ArrayList<>(ImmutableList.of("inputVal2-1", "inputVal2-2")))));
        HashMap<String, String> outputs = new HashMap<>();

        try {
            FtepServicesClient.launch(SERVICE_NAME, new HashMap<>(conf), inputs, outputs);
        } catch (UnsatisfiedLinkError e) {
            // Swallow this, it's not worth linking just to get the return value
            assertThat(e.getMessage(), is("no ZOO in java.library.path"));
        }
        assertThat(outputs.isEmpty(), is(false));
        assertThat(outputs.get("output"), is(
                "ftep://output/output_file_1," +
                        "ftep://output/A_plot.zip," +
                        "ftep://output/A_plot.qpj," +
                        "ftep://output/A_point.qpj"
        ));
    }

}
