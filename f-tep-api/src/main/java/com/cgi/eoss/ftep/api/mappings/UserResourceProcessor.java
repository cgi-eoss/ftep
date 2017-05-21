package com.cgi.eoss.ftep.api.mappings;

import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;

/**
 * <p>HATEOAS resource processor for {@link User}s.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserResourceProcessor extends BaseResourceProcessor<User> {
    private final RepositoryEntityLinks entityLinks;

    @Override
    protected EntityLinks getEntityLinks() {
        return entityLinks;
    }

    @Override
    protected Class<User> getTargetClass() {
        return User.class;
    }

    private void addWalletLink(Resource resource, Wallet wallet) {
        if (wallet != null) {
            resource.add(entityLinks.linkToSingleResource(wallet));
        }
    }

    @Component
    private final class BaseEntityProcessor implements ResourceProcessor<Resource<User>> {
        @Override
        public Resource<User> process(Resource<User> resource) {
            User entity = resource.getContent();

            addSelfLink(resource, entity);
            addWalletLink(resource, entity.getWallet());

            return resource;
        }
    }

}
