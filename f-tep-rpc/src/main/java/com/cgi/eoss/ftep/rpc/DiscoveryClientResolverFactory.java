package com.cgi.eoss.ftep.rpc;

import io.grpc.Attributes;
import io.grpc.NameResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;

/**
 * <p>Factory to produce a {@link NameResolver} for a given service, with resolution handled by our
 * {@link DiscoveryClient}.</p>
 */
@Service
public class DiscoveryClientResolverFactory extends NameResolver.Factory {

    private static final String SCHEME = "spring-cloud";

    private final DiscoveryClient discoveryClient;

    @Autowired
    public DiscoveryClientResolverFactory(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, Attributes params) {
        // We're not quite doing gRPC URIs conventionally, so de-mangle the service ID
        try {
            String serviceId = URLDecoder.decode(targetUri.getPath().substring(1), "UTF-8");
            return new DiscoveryClientNameResolver(discoveryClient, serviceId, params);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDefaultScheme() {
        return SCHEME;
    }

}
