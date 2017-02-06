package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.DownloaderCredentials;
import com.cgi.eoss.ftep.rpc.Credentials;
import com.cgi.eoss.ftep.rpc.CredentialsServiceGrpc;
import com.cgi.eoss.ftep.rpc.GetCredentialsParams;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

@GRpcService
public class RpcCredentialsService extends CredentialsServiceGrpc.CredentialsServiceImplBase {

    private final DownloaderCredentialsDataService dataService;

    @Autowired
    public RpcCredentialsService(DownloaderCredentialsDataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public void getCredentials(GetCredentialsParams request, StreamObserver<Credentials> responseObserver) {
        DownloaderCredentials result = dataService.getByHost(request.getHost());

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
    }
}
