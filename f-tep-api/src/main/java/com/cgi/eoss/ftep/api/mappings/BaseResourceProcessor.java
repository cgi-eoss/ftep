package com.cgi.eoss.ftep.api.mappings;

import com.cgi.eoss.ftep.model.FtepEntity;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Identifiable;
import org.springframework.hateoas.Resource;

abstract class BaseResourceProcessor<T extends FtepEntity<T>> {
    void addSelfLink(Resource resource, Identifiable<Long> entity) {
        if (resource.getLink("self") == null) {
            resource.add(getEntityLinks().linkToSingleResource(getTargetClass(), entity.getId()).withSelfRel().expand());
            resource.add(getEntityLinks().linkToSingleResource(getTargetClass(), entity.getId()));
        }
    }

    protected abstract EntityLinks getEntityLinks();

    protected abstract Class<T> getTargetClass();
}
