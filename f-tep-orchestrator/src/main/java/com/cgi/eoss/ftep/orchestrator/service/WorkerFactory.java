package com.cgi.eoss.ftep.orchestrator.service;

import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * <p>Service providing access to F-TEP Worker nodes based on environment requests.</p>
 */
@Service
public class WorkerFactory {

    private final ManagedChannelBuilder localChannelBuilder;

    @Autowired
    public WorkerFactory(@Qualifier("localWorkerChannelBuilder") ManagedChannelBuilder localChannelBuilder) {
        // TODO Use service discovery instead of manual definition + injection
        this.localChannelBuilder = localChannelBuilder;
    }

    /**
     * @return A Worker appropriate for the requested environment.
     */
    public FtepWorkerGrpc.FtepWorkerBlockingStub getWorker(WorkerEnvironment env) {
        switch (env) {
            case LOCAL:
                return FtepWorkerGrpc.newBlockingStub(localChannelBuilder.build());
            default:
                throw new UnsupportedOperationException("Unable to launch worker for environment: " + env);
        }
    }

}
