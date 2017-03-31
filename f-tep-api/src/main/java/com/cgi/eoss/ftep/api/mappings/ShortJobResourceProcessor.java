package com.cgi.eoss.ftep.api.mappings;

import com.cgi.eoss.ftep.api.projections.ShortJob;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;

/**
 * <p>HATEOAS resource processor for {@link ShortJob}s. Adds extra _link entries for client use, e.g. job container
 * logs.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ShortJobResourceProcessor implements ResourceProcessor<Resource<ShortJob>> {
    private final RepositoryEntityLinks entityLinks;

    @Override
    public Resource<ShortJob> process(Resource<ShortJob> resource) {
        if (!Strings.isNullOrEmpty(resource.getContent().getGuiUrl())) {
            resource.add(new Link(resource.getContent().getGuiUrl()).withRel("gui"));
        }

        // TODO Do this properly with a method reference
        resource.add(new Link(resource.getLink("self").getHref() +"/logs").withRel("logs"));

        return resource;
    }

}
