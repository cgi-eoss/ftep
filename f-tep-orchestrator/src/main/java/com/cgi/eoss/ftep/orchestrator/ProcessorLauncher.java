package com.cgi.eoss.ftep.orchestrator;

import com.cgi.eoss.ftep.rpc.ProcessorLauncherGrpc;
import com.cgi.eoss.ftep.rpc.ProcessorParams;
import com.cgi.eoss.ftep.rpc.ProcessorResponse;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Server endpoint for the ProcessorLauncher RPC service.</p>
 */
@Slf4j
public class ProcessorLauncher extends ProcessorLauncherGrpc.ProcessorLauncherImplBase {

    private final WorkerService workerService;
    private final JobStatusService jobStatusService;
    private final JobEnvironmentService jobEnvironmentService;

    public ProcessorLauncher(WorkerService workerService,
                             JobStatusService jobStatusService,
                             JobEnvironmentService jobEnvironmentService) {
        // TODO Distribute by configuring and connecting to these services via RPC
        this.workerService = workerService;
        this.jobStatusService = jobStatusService;
        this.jobEnvironmentService = jobEnvironmentService;
    }

    @Override
    public void launchProcessor(ProcessorParams request, StreamObserver<ProcessorResponse> responseObserver) {
        // TODO Implement the processor launching
        super.launchProcessor(request, responseObserver);
    }
}
