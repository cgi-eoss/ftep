package com.cgi.eoss.ftep.rpc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import lombok.experimental.UtilityClass;

import java.util.List;

/**
 * <p>Utility class providing helper methods for dealing with Protobuf/Grpc services and objects.</p>
 */
@UtilityClass
public class GrpcUtil {
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
}
