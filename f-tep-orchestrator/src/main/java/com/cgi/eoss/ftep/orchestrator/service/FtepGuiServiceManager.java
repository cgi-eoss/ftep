package com.cgi.eoss.ftep.orchestrator.service;

import com.cgi.eoss.ftep.rpc.Job;
import com.cgi.eoss.ftep.rpc.worker.Binding;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import com.cgi.eoss.ftep.rpc.worker.PortBinding;
import com.google.common.base.Strings;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <p>Service for more specific interaction with graphical application-type F-TEP services.</p>
 */
@Component
public class FtepGuiServiceManager {

    static final String GUACAMOLE_PORT = "8080/tcp";

    @Value("${ftep.orchestrator.gui.defaultHost:}")
    private String guiDefaultHost;

    @Autowired
    public FtepGuiServiceManager() {
    }

    /**
     * @return The string representation of a URL suitable for accessing the GUI application represented by the given
     * Job running on the given worker.
     */
    // TODO Expose the GUI URL via a reverse proxy with F-TEP access controls
    public String getGuiUrl(FtepWorkerGrpc.FtepWorkerBlockingStub worker, Job rpcJob) {
        PortBinding portBinding = worker.getPortBindings(rpcJob).getBindingsList().stream()
                .filter(b -> b.getPortDef().equals(GUACAMOLE_PORT))
                .findFirst()
                .orElseThrow(() -> new ServiceExecutionException("Could not find GUI port on docker container for job: " + rpcJob.getId()));

        Binding binding = portBinding.getBinding();

        String host;

        if (binding.getIp().equals("0.0.0.0")) {
            // Crudely determine the GUI host address from the worker gRPC endpoint, if the config property is not set
            host = Strings.isNullOrEmpty(guiDefaultHost) ? worker.getChannel().authority().split(":")[0] : guiDefaultHost;
        } else {
            host = binding.getIp();
        }

        return new HttpUrl.Builder()
                .scheme("http")
                .host(host)
                .port(binding.getPort())
                .build()
                .toString();
    }

}
