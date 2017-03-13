package com.cgi.eoss.ftep.core.wpswrapper;

import com.cgi.eoss.ftep.core.requesthandler.RequestHandler;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.RemoteApiVersion;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.util.HashMap;

@Log4j2
public abstract class AbstractWrapperProc {
    private static final String DEFAULT_DOCKER_HOST = "localhost";
    private static final String DEFAULT_DOCKER_PORT = "2376";
    private static final String DEFAULT_DOCKER_CERT_PATH = System.getProperty("user.home") + File.separator + ".docker";
    private static final String DOCKER_UNIX_SOCKET = "unix:///var/run/docker.sock";

    private String dockerImageName;

    public AbstractWrapperProc(String dockerImgName) {
        if (null != dockerImgName && !dockerImgName.isEmpty()) {
            dockerImageName = dockerImgName;
        } else {
            LOG.error("Missing Docker Image name");
        }
    }

    RequestHandler getRequestHandler(HashMap<String, HashMap<String, String>> conf,
                                     HashMap<String, HashMap<String, Object>> inputs, HashMap<String, HashMap<String, String>> outputs) {
        RequestHandler requestHandler = new RequestHandler(conf, inputs, outputs);
        return requestHandler;
    }

    boolean isSufficientCoinsAvailable() {
        // TODO Auto-generated method stub
        return true;
    }

    boolean isValidUser() {
        // TODO Auto-generated method stub
        return true;
    }

//	boolean getInputParameter(String string) {
//		// TODO Auto-generated method stub
//		return false;
//	}

    public String getDockerImageName() {
        return dockerImageName;
    }

    protected static DockerClient getDockerClient(String dockerHost) {
        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withApiVersion(RemoteApiVersion.VERSION_1_19);

        String hostUrl;
        if (!dockerHost.equals(DEFAULT_DOCKER_HOST)) {
            // Use TLS-secured remote docker host
            hostUrl = "tcp://" + dockerHost + ":" + DEFAULT_DOCKER_PORT;
            configBuilder.withDockerTlsVerify(true).withDockerCertPath(DEFAULT_DOCKER_CERT_PATH);
        } else {
            // Use the default unix socket rather than TCP to localhost
            hostUrl = DOCKER_UNIX_SOCKET;
        }
        configBuilder.withDockerHost(hostUrl);
        LOG.info("Worker connecting to docker: {}", hostUrl);

        return DockerClientBuilder.getInstance(configBuilder.build()).build();
    }

}
