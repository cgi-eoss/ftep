package com.cgi.eoss.ftep.api.mappings;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.persistence.service.FtepFileDataService;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;

/**
 * <p>HATEOAS resource processor for {@link Job}s. Adds extra _link entries for client use, e.g. job container logs.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JobResourceProcessor implements ResourceProcessor<Resource<Job>> {

    private final FtepFileDataService ftepFileDataService;

    private final RepositoryEntityLinks entityLinks;

    @Override
    public Resource<Job> process(Resource<Job> resource) {
        Job entity = resource.getContent();

        if (resource.getLink("self") == null) {
            resource.add(entityLinks.linkToSingleResource(entity).withSelfRel().expand());
            resource.add(entityLinks.linkToSingleResource(entity));
        }

        if (!Strings.isNullOrEmpty(entity.getGuiUrl())) {
            resource.add(new Link(entity.getGuiUrl()).withRel("gui"));
        }

        // TODO Do this properly with a method reference
        resource.add(new Link(resource.getLink("self").getHref() + "/logs").withRel("logs"));

        // Transform any "ftep://" URIs into relation links
        Multimap<String, String> outputs = entity.getOutputs();
        if (outputs != null && !outputs.isEmpty()) {
            outputs.entries().stream()
                    .filter(e -> e.getValue().startsWith("ftep://"))
                    .forEach(e -> {
                        FtepFile ftepFile = ftepFileDataService.getByUri(e.getValue());
                        resource.add(entityLinks.linkToSingleResource(ftepFile).withRel("output-" + e.getKey()).expand());
                    });
        }

        return resource;
    }

}
