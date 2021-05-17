package com.cgi.eoss.ftep.orchestrator.service.gui;

import com.cgi.eoss.ftep.rpc.Job;
import com.cgi.eoss.ftep.rpc.worker.PortBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "ftep.orchestrator.gui.mode", havingValue = "SIMPLE", matchIfMissing = true)
public class PatternBasedGuiUrlService implements GuiUrlService {

    private final JobPortLocatorService jobPortLocatorService;
    private final String guiUrlPattern;

    @Autowired
    public PatternBasedGuiUrlService(JobPortLocatorService jobPortLocatorService,
                                     @Value("${ftep.orchestrator.gui.urlPattern:/gui/:__PORT__/}") String guiUrlPattern) {
        this.jobPortLocatorService = jobPortLocatorService;
        this.guiUrlPattern = guiUrlPattern;
    }

    @Override
    public String getBackendEndpoint(String workerId, Job job, String port) {
        PortBinding portBinding = jobPortLocatorService.getPortBinding(workerId, job, port);
        return "http://" + portBinding.getBinding().getIp() + ":" + portBinding.getBinding().getPort();
    }

    @Override
    public String buildGuiUrl(String workerId, com.cgi.eoss.ftep.rpc.Job job, String port) {
        PortBinding portBinding = jobPortLocatorService.getPortBinding(workerId, job, port);
        return guiUrlPattern.replaceAll("__PORT__", String.valueOf(portBinding.getBinding().getPort()));
    }

}
