package com.cgi.eoss.ftep.zoomanager.service;

import com.cgi.eoss.ftep.rpc.UpdateActiveZooServicesResult;
import com.cgi.eoss.ftep.rpc.WpsServiceDescriptors;
import com.cgi.eoss.ftep.rpc.ZooManagerServiceGrpc;
import com.cgi.eoss.ftep.zoomanager.stubs.ZooStubWriter;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.nio.file.Path;

@GRpcService
public class ZooManagerService extends ZooManagerServiceGrpc.ZooManagerServiceImplBase {

    private final ZooStubWriter zooStubWriter;
    private final FtepServiceDescriptorHandler serviceDescriptorHandler;
    private final Path zooServicesStubJar;

    @Autowired
    public ZooManagerService(ZooStubWriter zooStubWriter, FtepServiceDescriptorHandler serviceDescriptorHandler, @Qualifier("zooServicesStubJar") Path zooServicesStubJar) {
        this.zooStubWriter = zooStubWriter;
        this.serviceDescriptorHandler = serviceDescriptorHandler;
        this.zooServicesStubJar = zooServicesStubJar;
    }


    @Override
    public void updateActiveZooServices(WpsServiceDescriptors request, StreamObserver<UpdateActiveZooServicesResult> responseObserver) {
        super.updateActiveZooServices(request, responseObserver);
    }

}
