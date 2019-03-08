package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.DownloaderCredentials;
import com.cgi.eoss.ftep.rpc.Credentials;
import com.cgi.eoss.ftep.rpc.CredentialsServiceGrpc;
import com.cgi.eoss.ftep.rpc.GetCredentialsParams;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityNotFoundException;
import java.util.Optional;

@GRpcService
@Log4j2
public class RpcCredentialsService extends CredentialsServiceGrpc.CredentialsServiceImplBase {

    private final DownloaderCredentialsDataService dataService;

    @Autowired
    public RpcCredentialsService(DownloaderCredentialsDataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public void getCredentials(GetCredentialsParams request, StreamObserver<Credentials> responseObserver) {
        try {
            DownloaderCredentials result = dataService.getByHost(request.getHost())
                    .orElseThrow(() -> new EntityNotFoundException("No credentials found for host " + request.getHost()));

            // TODO Use mapstruct for type conversion
            Credentials.Builder credentialsBuilder = Credentials.newBuilder()
                    .setType(Credentials.Type.valueOf(result.getType().name()));
            if (result.getHost() != null) {
                credentialsBuilder.setHost(result.getHost());
            }
            if (result.getUsername() != null) {
                credentialsBuilder.setUsername(result.getUsername());
            }
            if (result.getPassword() != null) {
                credentialsBuilder.setPassword(result.getPassword());
            }
            if (result.getCertificatePath() != null) {
                credentialsBuilder.setCertificatePath(result.getCertificatePath());
            }

            responseObserver.onNext(credentialsBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Failed to retrieve downloader credentials for host '{}'", request.getHost(), e);
            responseObserver.onError(e);
        }
    }

}
