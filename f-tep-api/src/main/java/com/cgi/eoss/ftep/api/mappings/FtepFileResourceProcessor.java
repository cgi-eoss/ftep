package com.cgi.eoss.ftep.api.mappings;

import com.cgi.eoss.ftep.catalogue.CatalogueService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.google.common.base.Strings;
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
    private final CatalogueService catalogueService;

    @Override
    public Resource<FtepFile> process(Resource<FtepFile> resource) {
        FtepFile entity = resource.getContent();

        if (resource.getLink("self") == null) {
            resource.add(entityLinks.linkToSingleResource(entity).withSelfRel().expand());
            resource.add(entityLinks.linkToSingleResource(entity));
        }

        // TODO Do this properly with a method reference
        if (entity.getType() != FtepFile.Type.EXTERNAL_PRODUCT) {
            resource.add(new Link(resource.getLink("self").getHref() + "/dl").withRel("download"));
        }

        String wmsLink = catalogueService.getWmsUrl(entity);
        if (!Strings.isNullOrEmpty(wmsLink)) {
            resource.add(new Link(wmsLink).withRel("wms"));
        }

        return resource;
    }

}
