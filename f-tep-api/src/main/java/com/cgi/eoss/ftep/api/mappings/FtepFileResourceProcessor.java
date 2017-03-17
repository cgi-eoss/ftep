package com.cgi.eoss.ftep.api.mappings;

import com.cgi.eoss.ftep.api.controllers.FtepFilesApiImpl;
import com.cgi.eoss.ftep.model.FtepFile;
import com.cgi.eoss.ftep.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

/**
 * <p>HATEOAS resource processor for {@link FtepFile}s. Adds extra _link entries for client use, e.g. file download.</p>
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FtepFileResourceProcessor implements ResourceProcessor<Resource<FtepFile>> {
    private final RepositoryEntityLinks entityLinks;

    @Override
    public Resource<FtepFile> process(Resource<FtepFile> resource) {
        // Reset the links, as @RepositoryRestController does not apply "self", which leads to this method duplicating it on the @RepositoryRestResource
        resource.removeLinks();

        resource.add(entityLinks.linkToSingleResource(FtepFile.class, resource.getContent().getId()).withSelfRel());

        // Add owner link
        if (resource.getContent().getOwner() != null) {
            resource.add(entityLinks.linkToSingleResource(User.class, resource.getContent().getOwner().getId()).withRel("owner"));
        }

        // Add download link
        Method downloadMethod = ReflectionUtils.findMethod(FtepFilesApiImpl.class, "downloadFile", FtepFile.class, HttpServletResponse.class);
        resource.add(linkTo(downloadMethod, resource.getContent().getId()).withRel("download"));

        return resource;
    }

}
