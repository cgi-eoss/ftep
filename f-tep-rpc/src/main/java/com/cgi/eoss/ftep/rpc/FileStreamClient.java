package com.cgi.eoss.ftep.rpc;

import com.google.protobuf.Message;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/**
 * <p>A class for receiving and writing locally a FileStream, utilising flow control.</p>
 * <p>This exposes a {@link CountDownLatch} which will tick once, in the {@link StreamObserver#onCompleted()} and {@link
 * StreamObserver#onError(Throwable)} methods. To retain synchronous behaviour, use
 * <code>fileStreamClient.getLatch().await()</code> after making the gRPC call.</code></p>
 *
 * @param <T> The gRPC request parameter type. Unused in the default implementation.
 */
@Log4j2
public abstract class FileStreamClient<T extends Message> implements Closeable {

    private final CountDownLatch latch;
    private OutputStream outputStream;
    private Path outputPath;

    protected FileStreamClient() {
        this.latch = new CountDownLatch(1);
    }

    public ClientResponseObserver<T, FileStream> getFileStreamObserver() {
        return new FileStreamClientResponseObserver();
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public Path getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(Path outputPath) {
        this.outputPath = outputPath;
    }

    public void onCompleted() {
        // no-op by default
    }

    @Override
    public void close() throws IOException {
        this.outputStream.close();
    }

    protected abstract OutputStream buildOutputStream(FileStream.FileMeta fileMeta) throws IOException;

    private class FileStreamClientResponseObserver implements ClientResponseObserver<T, FileStream> {
        private ClientCallStreamObserver<T> requestStream;

        @Override
        public void beforeStart(ClientCallStreamObserver<T> requestStream) {
            this.requestStream = requestStream;
            requestStream.disableAutoInboundFlowControl();
        }

        @Override
        public void onNext(FileStream value) {
            try {
                if (value.hasMeta()) {
                    setOutputStream(buildOutputStream(value.getMeta()));
                } else {
                    LOG.trace("Receiving file chunk from position {}", value.getChunk().getPosition());
                    value.getChunk().getData().writeTo(outputStream);
                }
                requestStream.request(1);
            } catch (IOException e) {
                throw new FileStreamIOException(e);
            }
        }

        @Override
        public void onError(Throwable t) {
            latch.countDown();
            throw new FileStreamIOException(t);
        }

        @Override
        public void onCompleted() {
            latch.countDown();
            FileStreamClient.this.onCompleted();
        }

        private void setOutputStream(OutputStream outputStream) {
            FileStreamClient.this.outputStream = outputStream;
        }
    }

}
