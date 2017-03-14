package com.cgi.eoss.ftep.zoomanager;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;

public final class ExampleServiceDescriptor {
    private static final List<FtepServiceDescriptor.Parameter> INPUTS = ImmutableList.of(
            FtepServiceDescriptor.Parameter.builder()
                    .id("inputfile")
                    .title("Input File 1")
                    .description("The input data file")
                    .minOccurs(1)
                    .maxOccurs(1)
                    .data(FtepServiceDescriptor.Parameter.DataNodeType.LITERAL)
                    .defaultAttrs(ImmutableMap.<String, String>builder()
                            .put("dataType", "string")
                            .build())
                    .build()
    );
    private static final List<FtepServiceDescriptor.Parameter> OUTPUTS = ImmutableList.of(
            FtepServiceDescriptor.Parameter.builder()
                    .id("result")
                    .title("URL to service output")
                    .description("see title")
                    .data(FtepServiceDescriptor.Parameter.DataNodeType.LITERAL)
                    .defaultAttrs(ImmutableMap.<String, String>builder()
                            .put("dataType", "string").build())
                    .build()
    );

    private static final FtepServiceDescriptor EXAMPLE_SVC = FtepServiceDescriptor.builder()
            .id("TestService1")
            .title("Test Service for ZCFG Generation")
            .description("This service tests the F-TEP automatic zcfg file generation")
            .version("1.0")
            .serviceProvider("ftep_service_wrapper")
            .serviceType("python")
            .storeSupported(false)
            .statusSupported(false)
            .dataInputs(INPUTS)
            .dataOutputs(OUTPUTS)
            .build();


    public static FtepServiceDescriptor getExampleSvc() {
        return EXAMPLE_SVC;
    }
}
