package com.cgi.eoss.ftep.orchestrator.service;

import com.cgi.eoss.ftep.rpc.Job;
import com.cgi.eoss.ftep.rpc.worker.Binding;
import com.cgi.eoss.ftep.rpc.worker.FtepWorkerGrpc;
import com.cgi.eoss.ftep.rpc.worker.PortBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <p>Service for more specific interaction with graphical application-type F-TEP services.</p>
 */
@Component
public class FtepGuiServiceManager {

    static final String GUACAMOLE_PORT = "8080/tcp";

    @Value("${ftep.orchestrator.gui.urlPattern:/gui/:__PORT__/}")
    private String guiUrlPattern;

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

        return guiUrlPattern.replaceAll("__PORT__", String.valueOf(binding.getPort()));
    }

}
