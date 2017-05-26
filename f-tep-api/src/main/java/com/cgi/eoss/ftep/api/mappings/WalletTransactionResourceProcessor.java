package com.cgi.eoss.ftep.api.mappings;

import com.cgi.eoss.ftep.model.WalletTransaction;
import com.cgi.eoss.ftep.model.projections.ShortWalletTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;

/**
 * <p>HATEOAS resource processor for {@link WalletTransaction}s. Adds extra _link entries for client use, e.g. the
 * associated entity.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class WalletTransactionResourceProcessor extends BaseResourceProcessor<WalletTransaction> {


    private final RepositoryEntityLinks entityLinks;

    @Override
    protected EntityLinks getEntityLinks() {
        return entityLinks;
    }

    @Override
    protected Class<WalletTransaction> getTargetClass() {
        return WalletTransaction.class;
    }

    private void addAssociatedEntityLink(Resource resource, WalletTransaction.Type type, Long entityId) {
        if (type != WalletTransaction.Type.CREDIT) {
            resource.add(entityLinks.linkToSingleResource(type.getTypeClass(), entityId).withRel("associated"));
        }
    }

    @Component
    private final class BaseEntityProcessor implements ResourceProcessor<Resource<WalletTransaction>> {
        @Override
        public Resource<WalletTransaction> process(Resource<WalletTransaction> resource) {
            WalletTransaction entity = resource.getContent();

            addSelfLink(resource, entity);
            addAssociatedEntityLink(resource, entity.getType(), entity.getAssociatedId());

            return resource;
        }
    }

    @Component
    private final class ShortEntityProcessor implements ResourceProcessor<Resource<ShortWalletTransaction>> {
        @Override
        public Resource<ShortWalletTransaction> process(Resource<ShortWalletTransaction> resource) {
            ShortWalletTransaction entity = resource.getContent();

            addSelfLink(resource, entity);
            addAssociatedEntityLink(resource, entity.getType(), entity.getAssociatedId());

            return resource;
        }
    }

}
