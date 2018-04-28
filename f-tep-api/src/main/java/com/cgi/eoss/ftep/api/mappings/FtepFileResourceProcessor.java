package com.cgi.eoss.ftep.api.mappings;

import com.cgi.eoss.ftep.catalogue.CatalogueService;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.projections.DetailedFtepFile;
import com.cgi.eoss.ftep.model.projections.ShortFtepFile;
import lombok.RequiredArgsConstructor;
import okhttp3.HttpUrl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * <p>HATEOAS resource processor for {@link FtepFile}s. Adds extra _link entries for client use, e.g. file download.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FtepFileResourceProcessor extends BaseResourceProcessor<FtepFile> {

    private final RepositoryEntityLinks entityLinks;
    private final CatalogueService catalogueService;

    @Override
    protected EntityLinks getEntityLinks() {
        return entityLinks;
    }

    @Override
    protected Class<FtepFile> getTargetClass() {
        return FtepFile.class;
    }

    private void addDownloadLink(Resource resource, FtepFile.Type type) {
        // TODO Check file datasource?
        if (type != FtepFile.Type.EXTERNAL_PRODUCT) {
            // TODO Do this properly with a method reference
            resource.add(new Link(resource.getLink("self").getHref() + "/dl").withRel("download"));
        }
    }

    private void addWmsLink(Resource resource, FtepFile.Type type, URI uri) {
        HttpUrl wmsLink = catalogueService.getWmsUrl(type, uri);
        if (wmsLink != null) {
            resource.add(new Link(wmsLink.toString()).withRel("wms"));
        }
    }

    private void addFtepLink(Resource resource, URI ftepFileUri) {
        resource.add(new Link(ftepFileUri.toASCIIString()).withRel("ftep"));
    }

    @Component
    private final class BaseEntityProcessor implements ResourceProcessor<Resource<FtepFile>> {
        @Override
        public Resource<FtepFile> process(Resource<FtepFile> resource) {
            FtepFile entity = resource.getContent();

            addSelfLink(resource, entity);
            addDownloadLink(resource, entity.getType());
            addWmsLink(resource, entity.getType(), entity.getUri());
            addFtepLink(resource, entity.getUri());

            return resource;
        }
    }

    @Component
    private final class DetailedEntityProcessor implements ResourceProcessor<Resource<DetailedFtepFile>> {
        @Override
        public Resource<DetailedFtepFile> process(Resource<DetailedFtepFile> resource) {
            DetailedFtepFile entity = resource.getContent();

            addSelfLink(resource, entity);
            addDownloadLink(resource, entity.getType());
            addWmsLink(resource, entity.getType(), entity.getUri());
            addFtepLink(resource, entity.getUri());

            return resource;
        }
    }

    @Component
    private final class ShortEntityProcessor implements ResourceProcessor<Resource<ShortFtepFile>> {
        @Override
        public Resource<ShortFtepFile> process(Resource<ShortFtepFile> resource) {
            ShortFtepFile entity = resource.getContent();

            addSelfLink(resource, entity);
            addDownloadLink(resource, entity.getType());
            addFtepLink(resource, entity.getUri());

            return resource;
        }
    }

}
