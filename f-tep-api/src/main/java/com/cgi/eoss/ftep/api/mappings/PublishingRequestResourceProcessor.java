package com.cgi.eoss.ftep.api.mappings;

import com.cgi.eoss.ftep.model.PublishingRequest;
import com.cgi.eoss.ftep.model.projections.ShortPublishingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;

/**
 * <p>HATEOAS resource processor for {@link PublishingRequest}s. Adds extra _link entries for client use, e.g. the
 * associated entity.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PublishingRequestResourceProcessor extends BaseResourceProcessor<PublishingRequest> {

    private final RepositoryEntityLinks entityLinks;

    @Override
    protected EntityLinks getEntityLinks() {
        return entityLinks;
    }

    @Override
    protected Class<PublishingRequest> getTargetClass() {
        return PublishingRequest.class;
    }

    private void addAssociatedEntityLink(Resource resource, PublishingRequest.Type type, Long entityId) {
        resource.add(entityLinks.linkToSingleResource(type.getTypeClass(), entityId).withRel("associated"));
    }

    @Component
    private final class BaseEntityProcessor implements ResourceProcessor<Resource<PublishingRequest>> {
        @Override
        public Resource<PublishingRequest> process(Resource<PublishingRequest> resource) {
            PublishingRequest entity = resource.getContent();

            addSelfLink(resource, entity);
            addAssociatedEntityLink(resource, entity.getType(), entity.getAssociatedId());

            return resource;
        }
    }

    @Component
    private final class ShortEntityProcessor implements ResourceProcessor<Resource<ShortPublishingRequest>> {
        @Override
        public Resource<ShortPublishingRequest> process(Resource<ShortPublishingRequest> resource) {
            ShortPublishingRequest entity = resource.getContent();

            addSelfLink(resource, entity);
            addAssociatedEntityLink(resource, entity.getType(), entity.getAssociatedId());

            return resource;
        }
    }

}
