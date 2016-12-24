package com.cgi.eoss.ftep.rpc;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import lombok.experimental.UtilityClass;

/**
 * <p>Utility class providing helper methods for dealing with Protobuf/Grpc services and objects.</p>
 */
@UtilityClass
public class GrpcUtil {
    /**
     * <p>Convert a service request's input parameters to a more convenient {@link Multimap}.</p>
     *
     * @param params The ApplicationParams from which the input params are converted.
     * @return The ApplicationParams input object(s) mapped to &lt;String, String&gt; entries.
     */
    public static Multimap<String, String> getInputsAsMap(ApplicationParams params) {
        ImmutableMultimap.Builder<String, String> mapBuilder = ImmutableMultimap.builder();
        params.getInputsList().forEach(p -> mapBuilder.putAll(p.getParamName(), p.getParamValueList()));
        return mapBuilder.build();
    }

    /**
     * <p>Convert a service request's input parameters to a more convenient {@link Multimap}.</p>
     *
     * @param params The ApplicationParams from which the input params are converted.
     * @return The ApplicationParams input object(s) mapped to &lt;String, String&gt; entries.
     */
    public static Multimap<String, String> getInputsAsMap(ProcessorParams params) {
        ImmutableMultimap.Builder<String, String> mapBuilder = ImmutableMultimap.builder();
        params.getInputsList().forEach(p -> mapBuilder.putAll(p.getParamName(), p.getParamValueList()));
        return mapBuilder.build();
    }
}
