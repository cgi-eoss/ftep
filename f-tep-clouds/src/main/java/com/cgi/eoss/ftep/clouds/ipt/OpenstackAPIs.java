package com.cgi.eoss.ftep.clouds.ipt;

import lombok.Builder;
import lombok.Value;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.nova.v2_0.NovaApi;

@Value
@Builder
public class OpenstackAPIs {
    private final NeutronApi neutronApi;
    private final NovaApi novaApi;
}
