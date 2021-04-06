package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.rpc.GetJobResultRequest;
import com.cgi.eoss.ftep.rpc.GetJobResultResponse;
import com.cgi.eoss.ftep.rpc.GetJobStatusRequest;
import com.cgi.eoss.ftep.rpc.GetJobStatusResponse;
import com.cgi.eoss.ftep.rpc.JobDataServiceGrpc;
import com.cgi.eoss.ftep.rpc.JobParam;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@GRpcService
@Log4j2
public class RpcJobDataService extends JobDataServiceGrpc.JobDataServiceImplBase {

    private final JobDataService jobDataService;

    @Autowired
    public RpcJobDataService(JobDataService jobDataService) {
        this.jobDataService = jobDataService;
    }

    @Override
    @Transactional(readOnly = true)
    public void getJobStatus(GetJobStatusRequest request, StreamObserver<GetJobStatusResponse> responseObserver) {
        try {
            Job job = jobDataService.getByExtId(UUID.fromString(request.getJobId()));

            GetJobStatusResponse response = GetJobStatusResponse.newBuilder()
                    .setJobStatus(getJobStatus(job))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to get job status for job {}", request.getJobId(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getJobResult(GetJobResultRequest request, StreamObserver<GetJobResultResponse> responseObserver) {
        try {
            Job job = jobDataService.getByExtId(UUID.fromString(request.getJobId()));

            GetJobResultResponse.Builder response = GetJobResultResponse.newBuilder()
                    .setJobStatus(getJobStatus(job));

            job.getOutputs().asMap().forEach((outputId, outputUrls) -> response.addOutputs(JobParam.newBuilder()
                    .setParamName(outputId)
                    .addAllParamValue(outputUrls)
                    .build()));

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to get job result for job {}", request.getJobId(), e);
            responseObserver.onError(e);
        }
    }

    private com.cgi.eoss.ftep.rpc.Job.Status getJobStatus(Job job) {
        switch (job.getStatus()) {
            case CREATED:
                // F-TEP does not distinguish between ACCEPTED and QUEUED
                return com.cgi.eoss.ftep.rpc.Job.Status.ACCEPTED;
            case RUNNING:
                return com.cgi.eoss.ftep.rpc.Job.Status.RUNNING;
            case COMPLETED:
                return com.cgi.eoss.ftep.rpc.Job.Status.COMPLETED;
            case ERROR:
                return com.cgi.eoss.ftep.rpc.Job.Status.FAILED;
            case CANCELLED:
                return com.cgi.eoss.ftep.rpc.Job.Status.CANCELLED;
        }
        throw new IllegalStateException("Failed to determine status of job: " + job);
    }

}
