package com.cgi.eoss.ftep.application.worker;

import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import org.lognet.springboot.grpc.GRpcService;

@GRpcService
public class FtepWorker extends FtepWorkerGrpc.FtepWorkerImplBase {
}
