package com.cgi.eoss.ftep.api.mappings;

import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.projections.DetailedJob;
import com.cgi.eoss.ftep.model.projections.ShortJob;
import com.cgi.eoss.ftep.persistence.service.FtepFileDataService;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;

/**
 * <p>HATEOAS resource processor for {@link Job}s. Adds extra _link entries for client use, e.g. job container logs.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JobResourceProcessor extends BaseResourceProcessor<Job> {


    private final RepositoryEntityLinks entityLinks;
    private final FtepFileDataService ftepFileDataService;

    @Override
    protected EntityLinks getEntityLinks() {
        return entityLinks;
    }

    @Override
    protected Class<Job> getTargetClass() {
        return Job.class;
    }

    private void addGuiLink(Resource resource, Job.Status status, String guiUrl) {
        if (status == Job.Status.RUNNING && !Strings.isNullOrEmpty(guiUrl)) {
            resource.add(new Link(guiUrl).withRel("gui"));
        }
    }

    private void addLogsLink(Resource resource) {
        // TODO Do this properly with a method reference
        resource.add(new Link(resource.getLink("self").getHref() + "/logs").withRel("logs"));
    }

    private void addOutputLinks(Resource resource, Multimap<String, String> outputs) {
        // Transform any "ftep://" URIs into relation links
        if (outputs != null && !outputs.isEmpty()) {
            outputs.entries().stream()
                    .filter(e -> e.getValue().startsWith("ftep://"))
                    .forEach(e -> {
                        FtepFile ftepFile = ftepFileDataService.getByUri(e.getValue());
                        resource.add(entityLinks.linkToSingleResource(ftepFile).withRel("output-" + e.getKey()).expand());
                    });
        }
    }

    @Component
    private final class BaseEntityProcessor implements ResourceProcessor<Resource<Job>> {
        @Override
        public Resource<Job> process(Resource<Job> resource) {
            Job entity = resource.getContent();

            addSelfLink(resource, entity);
            addGuiLink(resource, entity.getStatus(), entity.getGuiUrl());
            addLogsLink(resource);
            addOutputLinks(resource, entity.getOutputs());

            return resource;
        }
    }

    @Component
    private final class DetailedEntityProcessor implements ResourceProcessor<Resource<DetailedJob>> {
        @Override
        public Resource<DetailedJob> process(Resource<DetailedJob> resource) {
            DetailedJob entity = resource.getContent();

            addSelfLink(resource, entity);
            addGuiLink(resource, entity.getStatus(), entity.getGuiUrl());
            addLogsLink(resource);
            addOutputLinks(resource, entity.getOutputs());

            return resource;
        }
    }

    @Component
    private final class ShortEntityProcessor implements ResourceProcessor<Resource<ShortJob>> {
        @Override
        public Resource<ShortJob> process(Resource<ShortJob> resource) {
            ShortJob entity = resource.getContent();

            addSelfLink(resource, entity);
            addGuiLink(resource, entity.getStatus(), entity.getGuiUrl());
            addLogsLink(resource);

            return resource;
        }
    }

}
