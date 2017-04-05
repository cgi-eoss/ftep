package com.cgi.eoss.ftep.api.mappings;

import com.cgi.eoss.ftep.model.FtepFile;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;

/**
 * <p>HATEOAS resource processor for {@link FtepFile}s. Adds extra _link entries for client use, e.g. file download.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FtepFileResourceProcessor implements ResourceProcessor<Resource<FtepFile>> {
    private final RepositoryEntityLinks entityLinks;

    @Override
    public Resource<FtepFile> process(Resource<FtepFile> resource) {
        if (resource.getLink("self") == null) {
            resource.add(entityLinks.linkToSingleResource(resource.getContent()).withSelfRel().expand());
            resource.add(entityLinks.linkToSingleResource(resource.getContent()));
        }

        // TODO Do this properly with a method reference
        resource.add(new Link(resource.getLink("self").getHref() + "/dl").withRel("download"));

        return resource;
    }

}
