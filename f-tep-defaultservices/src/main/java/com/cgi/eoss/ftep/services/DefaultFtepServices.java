package com.cgi.eoss.ftep.services;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.FtepServiceContextFile;
import com.cgi.eoss.ftep.model.FtepServiceDescriptor;
import com.cgi.eoss.ftep.model.ServiceLicence;
import com.cgi.eoss.ftep.model.ServiceStatus;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.internal.CompleteFtepService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>Access to the default F-TEP service collection as Java objects.</p>
 * <p>The services are read from classpath resources baked in at compile-time, and may be used to install or restore the
 * default service set during runtime.</p>
 */
@UtilityClass
@Log4j2
public class DefaultFtepServices {

    private static final Set<String> DEFAULT_SERVICES = ImmutableSet.of(
            // Processors
            "LandCoverS1",
            "LandCoverS2",
            "S1Biomass",
            "VegetationIndices",
            // Graphical applications
            "Monteverdi",
            "QGIS",
            "SNAP"
    );

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    public static Set<CompleteFtepService> getDefaultServices() {
        return DEFAULT_SERVICES.stream().map(DefaultFtepServices::importDefaultService).collect(Collectors.toSet());
    }

    private static CompleteFtepService importDefaultService(String serviceId) {
        try {
            FtepService service = new FtepService(serviceId, User.DEFAULT, "ftep/" + serviceId.toLowerCase());
            service.setLicence(ServiceLicence.OPEN);
            service.setStatus(ServiceStatus.AVAILABLE);
            service.setServiceDescriptor(getServiceDescriptor(service));
            service.setDescription(service.getServiceDescriptor().getDescription());

            Set<FtepServiceContextFile> files = getServiceContextFiles(service);

            return CompleteFtepService.builder()
                    .service(service)
                    .files(files)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Could not load default F-TEP Service " + serviceId, e);
        }
    }

    private static FtepServiceDescriptor getServiceDescriptor(FtepService service) throws IOException {
        try (Reader reader = new InputStreamReader(DefaultFtepServices.class.getResourceAsStream("/" + service.getName() + ".yaml"))) {
            return YAML_MAPPER.readValue(reader, FtepServiceDescriptor.class);
        }
    }

    private static Set<FtepServiceContextFile> getServiceContextFiles(FtepService service) throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(DefaultFtepServices.class.getClassLoader());
        Resource baseDir = resolver.getResource("classpath:/" + service.getName());
        Resource[] resources = resolver.getResources("classpath:/" + service.getName() + "/**/*");

        Path basePath = Paths.get(baseDir.getURI());

        Set<FtepServiceContextFile> files = new HashSet<>();
        for (Resource resource : resources) {
            if (!resource.getFile().isDirectory()) {
                String relativeFilename = basePath.relativize(Paths.get(resource.getURI())).toString();
                FtepServiceContextFile file = new FtepServiceContextFile(service, relativeFilename);
                file.setContent(new String(ByteStreams.toByteArray(resource.getInputStream())));
                files.add(file);
            }
        }

        return files;
    }

}
