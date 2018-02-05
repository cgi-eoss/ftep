package com.cgi.eoss.ftep.rpc;

import com.google.protobuf.ByteString;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
public abstract class FileStreamServer implements Closeable {

    private final Path inputPath;
    private final StreamObserver<FileStream> responseObserver;
    private final ServerCallStreamObserver<FileStream> serverCallStreamObserver;

    @Getter(lazy = true)
    private final FileStream.FileMeta fileMeta = buildFileMeta();
    @Getter(lazy = true)
    private final ReadableByteChannel byteChannel = buildByteChannel();

    protected FileStreamServer(Path inputPath, StreamObserver<FileStream> responseObserver) {
        this.inputPath = inputPath;
        this.responseObserver = responseObserver;
        this.serverCallStreamObserver = (ServerCallStreamObserver<FileStream>) responseObserver;

        serverCallStreamObserver.disableAutoInboundFlowControl();
        final AtomicBoolean wasReady = new AtomicBoolean(false);
        serverCallStreamObserver.setOnReadyHandler(() -> {
            if (serverCallStreamObserver.isReady() && wasReady.compareAndSet(false, true)) {
                LOG.debug("serverCallStreamObserver onReady");
                serverCallStreamObserver.request(1);
            }
        });
    }

    public void streamFile() throws IOException, InterruptedException {
        // First message carries file metadata
        responseObserver.onNext(FileStream.newBuilder().setMeta(getFileMeta()).build());

        // Remaining messages carry the data in chunks of the specified buffer size
        ReadableByteChannel channel = getByteChannel();
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        int position = 0;
        while (channel.read(buffer) > 0) {
            // Block until the server says another message can be buffered
            while (!serverCallStreamObserver.isReady()) {
                Thread.sleep(1);
            }

            LOG.trace("Sending file chunk from position {}", position);
            int size = buffer.position();
            buffer.rewind();
            responseObserver.onNext(FileStream.newBuilder().setChunk(FileStream.Chunk.newBuilder()
                    .setPosition(position)
                    .setData(ByteString.copyFrom(buffer, size))
                    .build()).build());
            position += buffer.position();
            buffer.flip();
        }

        responseObserver.onCompleted();
    }

    public Path getInputPath() {
        return inputPath;
    }

    @Override
    public void close() throws IOException {
        getByteChannel().close();
    }

    protected abstract FileStream.FileMeta buildFileMeta();

    protected abstract ReadableByteChannel buildByteChannel();

}
