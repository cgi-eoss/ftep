package com.cgi.eoss.ftep.zoomanager.service;

import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import com.cgi.eoss.ftep.rpc.WpsServiceDescriptor;
import com.cgi.eoss.ftep.rpc.WpsServiceDescriptors;
import com.cgi.eoss.ftep.rpc.ZooManagerServiceGrpc;
import com.cgi.eoss.ftep.zoomanager.ExampleServiceDescriptor;
import com.cgi.eoss.ftep.zoomanager.ZooManagerConfig;
import com.cgi.eoss.ftep.zoomanager.ZooManagerTestConfig;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {ZooManagerConfig.class, ZooManagerTestConfig.class})
@TestPropertySource("classpath:test-zoomanager.properties")
public class ZooManagerServiceIT {

    @Autowired
    private ZooManagerService zooManagerService;

    @Autowired
    private InProcessServerBuilder serverBuilder;

    @Autowired
    private ManagedChannelBuilder channelBuilder;

    private Server server;

    private ZooManagerServiceGrpc.ZooManagerServiceBlockingStub client;

    @Autowired
    @Qualifier("zcfgBasePath")
    private Path zcfgBasePath;

    @Autowired
    @Qualifier("zooServicesStubJar")
    private Path zooServicesStubJar;

    @Before
    public void setUp() throws IOException {
        serverBuilder.addService(zooManagerService);
        server = serverBuilder.build().start();

        client = ZooManagerServiceGrpc.newBlockingStub(channelBuilder.build());
    }

    @After
    public void tearDown() {
        server.shutdownNow();
    }

    @Test
    public void testUpdateActiveZooServices() throws Exception {
        // Preconditions
        Set<String> zcfgFiles = Files.list(zcfgBasePath).map(Path::toString).collect(Collectors.toSet());
        assertThat(zcfgFiles, is(ImmutableSet.of("/var/www/cgi-bin/jars")));
        assertThat(Files.exists(zooServicesStubJar), is(false));

        FtepServiceDescriptor exampleSvc = ExampleServiceDescriptor.getExampleSvc();

        WpsServiceDescriptors rpcArg = WpsServiceDescriptors.newBuilder().addServices(
                WpsServiceDescriptor.newBuilder().setName(exampleSvc.getId()).setContent(ByteString.copyFromUtf8(exampleSvc.toYaml())).build()
        ).build();

        client.updateActiveZooServices(rpcArg);

        zcfgFiles = Files.list(zcfgBasePath).map(Path::toString).collect(Collectors.toSet());
        assertThat(zcfgFiles, is(ImmutableSet.of("/var/www/cgi-bin/jars", "/var/www/cgi-bin/TestService1.zcfg")));
        assertThat(Files.exists(zooServicesStubJar), is(true));
    }

}