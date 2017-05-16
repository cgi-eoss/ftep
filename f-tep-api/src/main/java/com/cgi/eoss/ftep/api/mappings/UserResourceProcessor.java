package com.cgi.eoss.ftep.api.mappings;

import com.cgi.eoss.ftep.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;

/**
 * <p>HATEOAS resource processor for {@link User}s.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserResourceProcessor implements ResourceProcessor<Resource<User>> {
    private final RepositoryEntityLinks entityLinks;
    @Override
    public Resource<User> process(Resource<User> resource) {
        User entity = resource.getContent();

        if (resource.getLink("self") == null) {
            resource.add(entityLinks.linkToSingleResource(entity).withSelfRel().expand());
            resource.add(entityLinks.linkToSingleResource(entity));
        }

        if (entity.getWallet() != null) {
            resource.add(entityLinks.linkToSingleResource(entity.getWallet()));
        }

        return resource;
    }
}
