package com.cgi.eoss.ftep.rpc;

import io.grpc.stub.StreamObserver;

public class NoopStreamObserver<T> implements StreamObserver<T> {
    @Override
    public void onNext(Object value) {
    }

    @Override
    public void onError(Throwable t) {
    }

    @Override
    public void onCompleted() {
    }
}
