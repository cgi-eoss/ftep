package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.api.security.FtepSecurityService;
import com.cgi.eoss.ftep.api.service.InProcessRpc;
import com.cgi.eoss.ftep.model.JobConfig;
import com.cgi.eoss.ftep.rpc.FtepServiceLauncherGrpc;
import com.cgi.eoss.ftep.rpc.FtepServiceParams;
import com.cgi.eoss.ftep.rpc.GrpcUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * <p>A {@link RepositoryRestController} for interacting with {@link JobConfig}s. Offers additional functionality over
 * the standard CRUD-style {@link JobConfigsApi}.</p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/jobConfigs")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class JobConfigsApiExtension {

    private final FtepSecurityService ftepSecurityService;
    private final InProcessRpc inProcessRpc;

    /**
     * <p>Provides a direct interface to the service orchestrator, allowing users to launch job configurations without
     * going via WPS.</p>
     * <p>Service are launched asynchronously; the gRPC response is discarded.</p>
     */
    @PostMapping("/{jobConfigId}/launch")
    @PreAuthorize("hasAnyRole('ROLE_CONTENT_AUTHORITY', 'ROLE_ADMIN') or hasPermission(#jobConfig, 'read')")
    @ResponseBody
    public void launch(@ModelAttribute("jobConfigId") JobConfig jobConfig) {
        FtepServiceLauncherGrpc.FtepServiceLauncherFutureStub serviceLauncher = inProcessRpc.futureFtepServiceLauncher();

        FtepServiceParams serviceParams = FtepServiceParams.newBuilder()
                .setJobId(UUID.randomUUID().toString())
                .setUserId(ftepSecurityService.getCurrentUser().getName())
                .setServiceId(jobConfig.getService().getName())
                .addAllInputs(GrpcUtil.mapToParams(jobConfig.getInputs()))
                .build();

        LOG.info("Launching service via REST API: {}", serviceParams);
        serviceLauncher.launchService(serviceParams);
    }

}
