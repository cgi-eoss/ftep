package com.cgi.eoss.ftep.rpc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.List;

/**
 * <p>Utility class providing helper methods for dealing with Protobuf/Grpc services and objects.</p>
 */
@UtilityClass
public class GrpcUtil {
    private static final int DEFAULT_FILE_STREAM_BUFFER_SIZE = 8192;

    /**
     * <p>Convert a gRPC parameters collection to a more convenient {@link Multimap}.</p>
     *
     * @param params The parameters to be converted.
     * @return The params input collection mapped to &lt;String, String&gt; entries.
     */
    public static Multimap<String, String> paramsListToMap(List<JobParam> params) {
        ImmutableMultimap.Builder<String, String> mapBuilder = ImmutableMultimap.builder();
        params.forEach(p -> mapBuilder.putAll(p.getParamName(), p.getParamValueList()));
        return mapBuilder.build();
    }

    /**
     * <p>Convert a {@link Multimap} into a collection of {@link JobParam}s for gRPC.</p>
     *
     * @param params The parameters to be converted.
     * @return The params input collection mapped to {@link JobParam}s.
     */
    public static Iterable<JobParam> mapToParams(Multimap<String, String> params) {
        ImmutableList.Builder<JobParam> paramsBuilder = ImmutableList.builder();
        params.keySet().forEach(
                k -> paramsBuilder.add(JobParam.newBuilder().setParamName(k).addAllParamValue(params.get(k)).build()));
        return paramsBuilder.build();
    }

    /**
     * <p>Convert a {@link com.cgi.eoss.ftep.model.Job} to its gRPC {@link Job} representation.</p>
     *
     * @param job The job to be converted.
     * @return The input job mapped to {@link Job}.
     */
    public static Job toRpcJob(com.cgi.eoss.ftep.model.Job job) {
        return Job.newBuilder()
                .setId(job.getExtId())
                .setIntJobId(String.valueOf(job.getId()))
                .setUserId(job.getOwner().getName())
                .setServiceId(job.getConfig().getService().getName())
                .build();
    }

    public static void streamFile(StreamObserver<FileStream> observer, String filename, long filesize, ReadableByteChannel byteChannel) throws IOException {
        streamFile(observer, filename, filesize, byteChannel, DEFAULT_FILE_STREAM_BUFFER_SIZE);
    }

    public static void streamFile(StreamObserver<FileStream> observer, String filename, long filesize, ReadableByteChannel byteChannel, int bufferSize) throws IOException {
        // First message carries file metadata
        FileStream.FileMeta fileMeta = FileStream.FileMeta.newBuilder()
                .setFilename(filename)
                .setSize(filesize)
                .build();
        observer.onNext(FileStream.newBuilder().setMeta(fileMeta).build());

        // Remaining messages carry the data in chunks of the specified buffer size
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        int position = 0;
        while (byteChannel.read(buffer) > 0) {
            int size = buffer.position();
            buffer.rewind();
            observer.onNext(FileStream.newBuilder().setChunk(FileStream.Chunk.newBuilder()
                    .setPosition(position)
                    .setData(ByteString.copyFrom(buffer, size))
                    .build()).build());
            position += buffer.position();
            buffer.flip();
        }
    }

}
