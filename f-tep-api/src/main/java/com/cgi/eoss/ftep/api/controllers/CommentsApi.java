package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Comment;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasRole('ADMIN')")
@RepositoryRestResource(
        path = "comments",
        itemResourceRel = "comment",
        collectionResourceRel = "comments"
)
public interface CommentsApi extends CommentsApiCustom, PagingAndSortingRepository<Comment, Long> {

}
