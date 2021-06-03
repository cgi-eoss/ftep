package com.cgi.eoss.ftep.orchestrator.service.gui;

import com.cgi.eoss.ftep.model.QGroup;
import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.model.internal.QAclClass;
import com.cgi.eoss.ftep.model.internal.QAclEntry;
import com.cgi.eoss.ftep.model.internal.QAclObjectIdentity;
import com.cgi.eoss.ftep.model.internal.QAclSid;
import com.cgi.eoss.ftep.persistence.service.JobDataService;
import com.cgi.eoss.ftep.rpc.GrpcUtil;
import com.cgi.eoss.ftep.rpc.Job;
import com.cgi.eoss.ftep.rpc.worker.PortBinding;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.querydsl.jpa.impl.JPAQuery;
import lombok.Builder;
import lombok.Singular;
import lombok.extern.log4j.Log4j2;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.cgi.eoss.ftep.model.Job.Status.RUNNING;

@Service
@ConditionalOnProperty(value = "ftep.orchestrator.gui.mode", havingValue = "TRAEFIK")
@Log4j2
public class TraefikGuiUrlService implements GuiUrlService {


    private static final long PROXY_UPDATE_PERIOD_MS = 10 * 60 * 1000L;
    private static final MediaType APPLICATION_JSON = MediaType.get(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);

    private final ObjectMapper objectMapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    private final JobPortLocatorService jobPortLocatorService;
    private final EntityManager em;
    private final JobDataService jobDataService;
    private final String guiUrlPattern;
    private final String traefikUrlString;
    private final OkHttpClient traefikRestClient;
    private final boolean enableSsoHeaders;
    private final String usernameSsoHeader;

    @Autowired
    public TraefikGuiUrlService(JobPortLocatorService jobPortLocatorService,
                                EntityManager em,
                                JobDataService jobDataService,
                                @Value("${ftep.orchestrator.gui.urlPattern:/gui/__JOB_UUID__/}") String guiUrlPattern,
                                @Value("${ftep.orchestrator.traefik.url:}") String traefikUrlString,
                                @Value("${ftep.orchestrator.traefik.user:}") String traefikUser,
                                @Value("${ftep.orchestrator.traefik.password:}") String traefikPassword,
                                @Value("${ftep.orchestrator.traefik.enableSSOHeaders:true}") boolean enableSsoHeaders,
                                @Value("${ftep.api.security.username-request-header:REMOTE_USER}") String usernameSsoHeader) {
        this.jobPortLocatorService = jobPortLocatorService;
        this.em = em;
        this.jobDataService = jobDataService;
        this.guiUrlPattern = guiUrlPattern;
        this.traefikUrlString = traefikUrlString;
        this.traefikRestClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request.Builder authenticatedRequest = chain.request().newBuilder();
                    if (!Strings.isNullOrEmpty(traefikUser)) {
                        authenticatedRequest.header("Authorization", Credentials.basic(traefikUser, traefikPassword));
                    }
                    return chain.proceed(authenticatedRequest.build());
                })
                .build();

        this.enableSsoHeaders = enableSsoHeaders;
        this.usernameSsoHeader = usernameSsoHeader;
    }

    @Override
    public String getBackendEndpoint(String workerId, Job job, String port) {
        PortBinding portBinding = jobPortLocatorService.getPortBinding(workerId, job, port);
        return "http://" + portBinding.getBinding().getIp() + ":" + portBinding.getBinding().getPort();
    }

    @Override
    @Transactional(readOnly = true)
    public String buildGuiUrl(String workerId, Job job, String port) {
        return guiUrlPattern.replaceAll("__JOB_UUID__", job.getId());
    }

    @Override
    public void update() {
        try {
            updateTraefik();
        } catch (Exception e) {
            LOG.error("Error updating traefik", e);
        }
    }

    private void updateTraefik() throws IOException {
        TraefikConfig.TraefikConfigBuilder newTraefikConfig = TraefikConfig.builder();
        jobDataService.findByStatusAndGuiUrlNotNull(RUNNING).forEach(intJob -> {
            Job job = GrpcUtil.toRpcJob(intJob);

            String frontendName = "frontend-" + job.getId();
            String backendName = "backend-" + job.getId();

            // Build a TraefikBackend
            String backendEndpoint = intJob.getGuiEndpoint();
            TraefikBackend newBackend = TraefikBackend.builder()
                    .server("server-" + job.getId(), TraefikServer.builder()
                            .weight(1)
                            .url(backendEndpoint)
                            .build())
                    .build();

            // Build a frontend
            TraefikFrontend.TraefikFrontendBuilder newFrontend = TraefikFrontend.builder()
                    .backend(backendName)
                    .passHostHeader(true);

            TraefikRoute.TraefikRouteBuilder route = TraefikRoute.builder();
            HttpUrl httpUrl = HttpUrl.parse(intJob.getGuiUrl());
            String path = Optional.ofNullable(httpUrl).map(HttpUrl::encodedPath).orElse(intJob.getGuiUrl());
            if (intJob.getConfig().getService().isStripProxyPath()) {
                route.rule("PathPrefixStrip:" + path);
            } else {
                route.rule("PathPrefix:" + path);
            }
            newFrontend.route("route-" + job.getId(), route.build());

            // an additional route-rule for auth header handling
            if (enableSsoHeaders) {
                Set<String> allowedUsernames = new HashSet<>();
                // The owner naturally has access
                allowedUsernames.add(job.getUserId());

                // All members of groups who have ADMIN permission on the Job should also have access,
                // but we catch exceptions here since it's more important that the owner can get in.
                try {
                    // Find the relevant access control entries for this job, join through to the group membership, and get the usernames
                    JPAQuery<String> usernames = new JPAQuery<String>(em)
                            .select(QUser.user.name)
                            .from(QUser.user)
                            .leftJoin(QGroup.group).on(QUser.user.in(QGroup.group.members))
                            .leftJoin(QAclSid.aclSid).on(QAclSid.aclSid.sid.startsWith("GROUP_").and(QAclSid.aclSid.sid.substring(6).castToNum(Long.class).eq(QGroup.group.id)))
                            .leftJoin(QAclEntry.aclEntry).on(QAclSid.aclSid.eq(QAclEntry.aclEntry.aclSid))
                            .leftJoin(QAclObjectIdentity.aclObjectIdentity).on(QAclObjectIdentity.aclObjectIdentity.eq(QAclEntry.aclEntry.aclObjectIdentity))
                            .leftJoin(QAclClass.aclClass).on(QAclClass.aclClass.eq(QAclObjectIdentity.aclObjectIdentity.objectIdClass))
                            .where(QAclEntry.aclEntry.granting.isTrue()
                                    .and(QAclEntry.aclEntry.mask.eq(BasePermission.ADMINISTRATION.getMask()))
                                    .and(QAclClass.aclClass.className.eq(com.cgi.eoss.ftep.model.Job.class.getName()))
                                    .and(QAclObjectIdentity.aclObjectIdentity.objectIdIdentity.eq(job.getIntJobId())));

                    allowedUsernames.addAll(usernames.fetchResults().getResults());
                } catch (Exception e) {
                    LOG.warn("Failed to find additional access usernames for job {} owned by {}", job.getIntJobId(), job.getUserId(), e);
                }

                LOG.trace("Adding allowed usernames to Job GUI access: {}", allowedUsernames);

                newFrontend.route("route-" + job.getId() + "-auth", TraefikRoute.builder()
                        .rule("HeadersRegexp: " + usernameSsoHeader + "," + Joiner.on("|").join(allowedUsernames))
                        .build());
            }

            newTraefikConfig.frontend(frontendName, newFrontend.build());
            newTraefikConfig.backend(backendName, newBackend);
        });

        try (Response response = traefikRestClient.newCall(new Request.Builder()
                .put(RequestBody.create(APPLICATION_JSON, objectMapper.writeValueAsBytes(newTraefikConfig.build())))
                .url(traefikUrlString).build()).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Unsuccessful Traefik response: " + response.message());
            }
        }
    }

    @Scheduled(fixedRate = PROXY_UPDATE_PERIOD_MS, initialDelay = 10000L)
    public void scheduledUpdate() {
        this.update();
    }

    @lombok.Value
    @Builder(toBuilder = true)
    private static class TraefikConfig {
        @Singular
        Map<String, TraefikFrontend> frontends;
        @Singular
        Map<String, TraefikBackend> backends;
    }

    @lombok.Value
    @Builder
    private static class TraefikFrontend {
        @Singular
        Map<String, TraefikRoute> routes;
        String backend;
        boolean passHostHeader;
    }

    @lombok.Value
    @Builder
    private static class TraefikRoute {
        String rule;
    }

    @lombok.Value
    @Builder
    private static class TraefikBackend {
        @Singular
        Map<String, TraefikServer> servers;
    }

    @lombok.Value
    @Builder
    private static class TraefikServer {
        int weight;
        String url;
    }

}
