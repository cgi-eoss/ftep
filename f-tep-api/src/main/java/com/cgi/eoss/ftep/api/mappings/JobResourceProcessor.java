package com.cgi.eoss.ftep.api.mappings;

import com.cgi.eoss.ftep.model.Job;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

/**
 * <p>HATEOAS resource processor for {@link Job}s. Adds extra _link entries for client use, e.g. job container logs.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JobResourceProcessor implements ResourceProcessor<Resource<Job>> {
    private final RepositoryEntityLinks entityLinks;

    @Override
    public Resource<Job> process(Resource<Job> resource) {
        if (!Strings.isNullOrEmpty(resource.getContent().getGuiUrl())) {
            resource.add(linkTo(resource.getContent().getGuiUrl()).withRel("gui"));
        }

        // TODO Do this properly with a method reference
        resource.add(new Link(resource.getLink("self").getHref() +"/logs").withRel("logs"));

        return resource;
    }

}
